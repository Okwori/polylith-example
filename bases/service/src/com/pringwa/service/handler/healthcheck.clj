(ns com.pringwa.service.handler.healthcheck
  (:require [datomic.client.api :as d]))

(defn handler [{:keys [conn]}]
  (if-not conn
    {:status 503
     :body   {:status "degraded" :checks {:database "no connection"}}}
    (try
      (d/db conn)
      {:status 200
       :body   {:status "ok" :checks {:database "connected"}}}
      (catch Exception e
        {:status 503
         :body   {:status "degraded"
                  :checks {:database (.getMessage e)}}}))))
