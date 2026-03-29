(ns com.pringwa.mcp-server.tools
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [com.pringwa.mcp-server.export :as export]))

(def ^:private service-url
  (or (System/getenv "MCP_SERVICE_URL") "http://localhost:8080"))

(def ^:private service-token
  (System/getenv "MCP_SERVICE_TOKEN"))

(defn- auth-headers []
  (when service-token {"Authorization" (str "Bearer " service-token)}))

(defn- get! [path params]
  (-> (http/get (str service-url path)
                {:query-params     (into {} (remove (comp nil? val) params))
                 :headers          (auth-headers)
                 :as               :json
                 :throw-exceptions false})
      :body))

(defn- post! [path body]
  (-> (http/post (str service-url path)
                 {:body             (json/generate-string body)
                  :content-type     :json
                  :headers          (auth-headers)
                  :as               :json
                  :throw-exceptions false})
      :body))

(defn- text [content]
  {:content [{:type "text" :text (json/generate-string content {:pretty true})}]})

(def definitions
  [{:name        "list-indicators"
    :description "List threat intelligence indicators, optionally filtered by type"
    :inputSchema {:type       "object"
                  :properties {:type   {:type "string" :description "Filter by indicator type"}
                               :limit  {:type "integer" :default 20}
                               :offset {:type "integer" :default 0}}}}

   {:name        "get-indicator"
    :description "Get a single threat intelligence indicator by ID"
    :inputSchema {:type       "object"
                  :required   ["id"]
                  :properties {:id {:type "string"}}}}

   {:name        "search-indicators"
    :description "Search indicators by criteria (AND logic)"
    :inputSchema {:type       "object"
                  :properties {:criteria {:type        "object"
                                          :description "Search criteria map"}}}}

   {:name        "export-dataset"
    :description "Export all indicators as JSONL fine-tuning datasets (Anthropic, OpenAI, Llama formats)"
    :inputSchema {:type       "object"
                  :properties {:output-dir {:type    "string"
                                            :default "export"
                                            :description "Directory to write .jsonl files into"}}}}])

(defmulti ^:private dispatch (fn [name _] name))

(defmethod dispatch "list-indicators" [_ {:strs [type limit offset]}]
  (text (get! "/v1/indicators" {:type type :limit limit :offset offset})))

(defmethod dispatch "get-indicator" [_ {:strs [id]}]
  (text (get! (str "/v1/indicators/" id) {})))

(defmethod dispatch "search-indicators" [_ {:strs [criteria]}]
  (text (post! "/v1/indicators/search" (or criteria {}))))

(defmethod dispatch "export-dataset" [_ {:strs [output-dir]}]
  (text (export/export! (or output-dir "export"))))

(defmethod dispatch :default [name _]
  {:isError true
   :content [{:type "text" :text (str "Unknown tool: " name)}]})

(defn call [name arguments]
  (dispatch name (or arguments {})))
