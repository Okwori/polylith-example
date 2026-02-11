(ns com.pringwa.persistence.interface
  (:require [com.pringwa.persistence.core :as db]
            [com.pringwa.persistence.model :as model]
            [com.pringwa.persistence.util :as util]))

(defn init-db [] (util/init-db util/db-name))

(defn slurp-data! [conn filename batch-size]
  (util/slurp-data! conn filename batch-size))

(defn transform-keys [m]
  (util/transform-keys m))

(defn findDocument [db id]
  (model/findDocument db id))

(defn findAllDocuments [db]
  (model/findAllDocuments db))

(defn conn
  [config]
  (db/create-conn config))
