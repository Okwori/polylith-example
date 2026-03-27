(ns com.pringwa.server.middleware
  (:require [clojure.tools.logging :as log]))

(defn wrap-connection [handler conn]
  (fn [request]
    (handler (assoc request :conn conn))))

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
