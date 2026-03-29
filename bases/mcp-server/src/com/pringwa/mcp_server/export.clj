(ns com.pringwa.mcp-server.export
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.java.io :as io]))

(def ^:private service-url
  (or (System/getenv "MCP_SERVICE_URL") "http://localhost:8080"))

(def ^:private service-token
  (System/getenv "MCP_SERVICE_TOKEN"))

(defn- auth-headers []
  (when service-token {"Authorization" (str "Bearer " service-token)}))

(defn- fetch-page [offset]
  (-> (http/get (str service-url "/v1/indicators")
                {:query-params {:limit 100 :offset offset}
                 :headers      (auth-headers)
                 :as           :json})
      :body))

(defn- fetch-all []
  (loop [offset 0 acc []]
    (let [{:keys [results total]} (fetch-page offset)
          acc (into acc results)]
      (if (>= (count acc) total)
        acc
        (recur (+ offset 100) acc)))))

(defn- prompt [indicator]
  (str "What is the threat intelligence indicator with type \""
       (get indicator :type "unknown")
       "\" and id \""
       (get indicator :id "unknown") "\"?"))

(defn- completion [indicator]
  (json/generate-string indicator {:pretty true}))

(defn- anthropic-line [indicator]
  (json/generate-string
    {:messages [{:role "user"      :content (prompt indicator)}
                {:role "assistant" :content (completion indicator)}]}))

(defn- openai-line [indicator]
  (json/generate-string
    {:messages [{:role "user"      :content (prompt indicator)}
                {:role "assistant" :content (completion indicator)}]}))

(defn- llama-line [indicator]
  (json/generate-string
    {:instruction (prompt indicator)
     :input       ""
     :output      (completion indicator)}))

(defn- write-jsonl! [path lines]
  (io/make-parents path)
  (with-open [w (io/writer path)]
    (doseq [line lines]
      (.write w line)
      (.newLine w))))

(defn export! [output-dir]
  (let [indicators (fetch-all)]
    (write-jsonl! (str output-dir "/anthropic.jsonl") (map anthropic-line indicators))
    (write-jsonl! (str output-dir "/openai.jsonl")    (map openai-line    indicators))
    (write-jsonl! (str output-dir "/llama.jsonl")     (map llama-line     indicators))
    {:count (count indicators) :output-dir output-dir}))
