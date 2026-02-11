(ns com.pringwa.persistence.core
  (:require [com.pringwa.persistence.util :as util]
            [com.stuartsierra.component :as component]
            [datomic.client.api :as d]))

(defrecord Database [conn]
  component/Lifecycle

  (start [this]
    (if conn
      this
      (let [db-map (util/init-db util/db-name)
            ;{:keys [conn]} db-map
            _ (util/slurp-data! (:conn db-map) "persistence/indicators.json" 3)]
        (assoc this :conn (:conn db-map)))))

  (stop [this]
    (assoc this :conn nil)))

(defn create-conn
  [{:keys [conn]}]
  (map->Database {:conn conn}))
