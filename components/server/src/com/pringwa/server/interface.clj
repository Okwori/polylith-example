(ns com.pringwa.server.interface
  (:require [com.pringwa.server.auth :as auth]
            [com.pringwa.server.core :as server]
            [com.pringwa.server.middleware :as middleware]))

(defn create [router config]
  (server/create router config))

(defn wrap-exception [handler]
  (middleware/wrap-exception handler))

(defn wrap-authentication [handler opts]
  (auth/wrap-authentication handler opts))

(defn wrap-authorization [handler]
  (auth/wrap-authorization handler))
