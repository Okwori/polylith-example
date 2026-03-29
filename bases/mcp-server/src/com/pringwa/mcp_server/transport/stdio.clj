(ns com.pringwa.mcp-server.transport.stdio
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [com.pringwa.mcp-server.protocol :as protocol])
  (:import [java.io BufferedReader InputStreamReader]))

(defn start! []
  (let [rdr (BufferedReader. (InputStreamReader. System/in))]
    (loop []
      (when-let [line (.readLine rdr)]
        (when-not (str/blank? line)
          (try
            (let [msg      (json/parse-string line true)
                  response (protocol/handle msg)]
              (when response
                (println (json/generate-string response))
                (flush)))
            (catch Exception e
              (println (json/generate-string
                         {:jsonrpc "2.0"
                          :id      nil
                          :error   {:code -32700 :message (.getMessage e)}}))
              (flush))))
        (recur)))))
