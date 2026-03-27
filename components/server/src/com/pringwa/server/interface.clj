(ns com.pringwa.server.interface
  (:require [com.pringwa.server.core :as server]
            [com.pringwa.server.middleware :as middleware]))

(defn create
  [router config]
  (server/create router config))

(defn wrap-exception
  [handler]
  (middleware/wrap-exception handler))
