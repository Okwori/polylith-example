(ns com.pringwa.service.main
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [com.pringwa.persistence.interface :as db]
            [com.pringwa.service.routes :as routes]
            [com.pringwa.server.interface :as server]
            [com.stuartsierra.component :as component]))

(defn new-system
  ([config] (new-system config true))
  ([config port]
   (component/system-map
     ;:datomic/client ()
     ;:datomic/conn (db/create (-> config :database))
     :app-state (app-state/create config)
     :server (server/create #'routes/router port))))

(defn config
  []
  (->> (io/resource "service/config.edn")
       (aero/read-config)))

(defn -main [& [port]]
  (->> (config)
       (new-system)
       (component/start)))
