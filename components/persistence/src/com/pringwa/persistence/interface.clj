(ns com.pringwa.persistence.interface
  (:require [com.pringwa.persistence.core :as db]))

(defn conn
  [config]
  (db/create-conn config))

