(ns com.pringwa.persistence.interface
  (:require [com.pringwa.persistence.core :as db]
            [com.pringwa.persistence.model :as model]
            [com.pringwa.persistence.util :as util]))

(defn init-db [] (util/init-db util/db-name))

(defn slurp-data! [conn filename batch-size]
  (util/slurp-data! conn filename batch-size))

(defn matches? [document criteria]
  (util/matches? document criteria))

(defn transform-keys [m]
  (util/transform-keys m))

(defn find-document [db id]
  (model/find-document db id))

(defn find-all-documents [db]
  (model/find-all-documents db))

(defn find-document-by-type [db type]
  (model/find-document-by-type db type))

(defn conn
  [config]
  (db/create-conn config))
