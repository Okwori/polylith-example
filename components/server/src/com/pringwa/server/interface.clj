(ns com.pringwa.server.interface
  (:require [com.pringwa.server.core :as server]))

(defn create
  [router config]
  (server/create router config))
