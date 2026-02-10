(ns com.pringwa.server.interface
  (:require [com.pringwa.server.core :as server]))

(defn create
  [handler-fn port]
  (server/create handler-fn port))

