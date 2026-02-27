(ns com.pringwa.persistence.core
  (:require [com.pringwa.persistence.util :as util]
            [com.stuartsierra.component :as component]))

(defrecord Database [config conn]
  component/Lifecycle

  (start [this]
    (if conn
      this
      (let [{{:keys [filename batch-size]} :data
             {:keys [client] :as database} :database} config
            {:keys [conn]} (util/init-db {:client client :database database})
            _ (util/slurp-data! conn filename batch-size)]
        (assoc this :conn conn))))

  (stop [this]
    (assoc this :conn nil)))

(defn new-database
  [config]
  (map->Database {:config config}))
