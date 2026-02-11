(ns com.pringwa.persistence.core
  (:require [com.stuartsierra.component :as component]
            [datomic.client.api :as d]))

(defrecord Database [conn]
  component/Lifecycle

  (start [this]
    (if conn
      this
      (let [client (d/client {:server-type :datomic-local
                              :storage-dir :mem
                              :system "indicators"})
            db-name "indicators"
            _ (d/create-database client {:db-name db-name})
            conn (d/connect client {:db-name db-name})]
        (assoc this :conn conn))))

  (stop [this]
    (assoc this :conn nil)))

(defn create-conn
  [{:keys [conn]}]
  (map->Database {:conn conn}))

