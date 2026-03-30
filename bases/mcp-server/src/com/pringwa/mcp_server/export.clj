(ns com.pringwa.mcp-server.export
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [cognitect.aws.client.api :as aws]))

(def ^:private service-url
  (or (System/getenv "MCP_SERVICE_URL") "http://localhost:8080"))

(defn- resolve-service-token
  "Returns the service bearer token. Prefers AWS Secrets Manager when
   MCP_SECRET_NAME is set; falls back to MCP_SERVICE_TOKEN env var."
  []
  (if-let [secret-name (System/getenv "MCP_SECRET_NAME")]
    (try
      (let [client (aws/client {:api :secretsmanager})
            result (aws/invoke client {:op      :GetSecretValue
                                       :request {:SecretId secret-name}})]
        (if (:cognitect.anomalies/category result)
          (do (log/warnf "MCP: could not fetch secret '%s' — falling back to env var" secret-name)
              (System/getenv "MCP_SERVICE_TOKEN"))
          (get (json/parse-string (:SecretString result) true) :mcp-service-token)))
      (catch Exception e
        (log/warnf "MCP: secrets fetch failed (%s) — falling back to env var" (.getMessage e))
        (System/getenv "MCP_SERVICE_TOKEN")))
    (System/getenv "MCP_SERVICE_TOKEN")))

(def ^:private service-token
  (delay (resolve-service-token)))

(def ^:private export-bucket
  (System/getenv "MCP_EXPORT_BUCKET"))

(defn- auth-headers []
  (when-let [token @service-token]
    {"Authorization" (str "Bearer " token)}))

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

(defn- lines->bytes [lines]
  (.getBytes (str (str/join "\n" lines) "\n") "UTF-8"))

(defn- write-local! [path lines]
  (io/make-parents path)
  (with-open [w (io/writer path)]
    (doseq [line lines]
      (.write w line)
      (.newLine w))))

(defn- upload-s3! [bucket key lines]
  (let [client  (aws/client {:api :s3})
        payload (lines->bytes lines)
        result  (aws/invoke client
                            {:op      :PutObject
                             :request {:Bucket      bucket
                                       :Key         key
                                       :Body        (java.io.ByteArrayInputStream. payload)
                                       :ContentType "application/x-ndjson"}})]
    (when (:cognitect.anomalies/category result)
      (throw (ex-info "S3 upload failed" result)))))

(defn- persist! [output-dir filename lines]
  (if export-bucket
    (upload-s3! export-bucket (str output-dir "/" filename) lines)
    (write-local! (str output-dir "/" filename) lines)))

(defn export! [output-dir]
  (let [indicators (fetch-all)]
    (persist! output-dir "anthropic.jsonl" (map anthropic-line indicators))
    (persist! output-dir "openai.jsonl"    (map openai-line    indicators))
    (persist! output-dir "llama.jsonl"     (map llama-line     indicators))
    {:count      (count indicators)
     :output-dir output-dir
     :bucket     (or export-bucket :local)}))
