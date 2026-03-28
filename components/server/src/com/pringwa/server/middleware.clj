(ns com.pringwa.server.middleware
  (:require [clojure.tools.logging :as log]))

(defn wrap-connection [handler conn]
  (fn [request]
    (handler (assoc request :conn conn))))

(defn wrap-cache-control
  "Adds Cache-Control: no-store to every response.
  Appropriate for security-sensitive APIs — prevents proxies and browsers
  from storing responses that may contain access-controlled data."
  [handler]
  (fn [request]
    (update (handler request) :headers assoc "Cache-Control" "no-store")))

(defn wrap-exception [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/errorf e "Unhandled exception for %s %s"
                    (name (:request-method request))
                    (:uri request))
        {:status 500
         :body   {:error "An unexpected error occurred"}}))))
