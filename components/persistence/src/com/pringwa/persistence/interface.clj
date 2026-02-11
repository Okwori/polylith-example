(ns com.pringwa.persistence.interface
  (:require [com.pringwa.persistence.core :as db]
            [com.pringwa.persistence.util :as util]))

(defn init-db [] (util/init-db util/db-name))

(defn slurp-data! [conn filename batch-size]
  (util/slurp-data! conn filename batch-size))

(defn conn
  [config]
  (db/create-conn config))
