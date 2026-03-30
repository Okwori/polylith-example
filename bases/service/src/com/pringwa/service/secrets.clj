(ns com.pringwa.service.secrets
  "Fetches application secrets from AWS Secrets Manager at Ion cold-start.
   The secret is expected to be a JSON object stored as a SecretString.

   Expected keys (all optional — service falls back to config.edn defaults):
     :cors-origin     — allowed origin for CORS (e.g. \"https://app.example.com\")
     :jwt-public-key  — PEM public key for in-app JWT validation (when added)"
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [cognitect.aws.client.api :as aws]))

(defn fetch
  "Fetch a Secrets Manager secret by name and parse its JSON value.
   Returns a keyword-keyed map on success, or {} if the secret is not found
   or AWS credentials are unavailable (graceful degradation in local dev)."
  [secret-name]
  (try
    (let [client (aws/client {:api :secretsmanager})
          result (aws/invoke client {:op      :GetSecretValue
                                     :request {:SecretId secret-name}})]
      (if (:cognitect.anomalies/category result)
        (do (log/warnf "Secrets Manager: could not fetch '%s' — %s"
                       secret-name
                       (or (:cognitect.anomalies/message result) result))
            {})
        (json/parse-string (:SecretString result) true)))
    (catch Exception e
      (log/warnf "Secrets Manager: skipping '%s' — %s" secret-name (.getMessage e))
      {})))
