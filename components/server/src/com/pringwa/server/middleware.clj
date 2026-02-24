(ns com.pringwa.server.middleware)

(defn wrap-connection [handler conn]
  (fn [request]
    (handler (assoc request :conn conn))))
