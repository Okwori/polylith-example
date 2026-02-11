(ns com.pringwa.service.main
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [com.pringwa.app-state.interface :as app-state]
            [com.pringwa.persistence.interface :as db]
            [com.pringwa.service.routes :as routes]
            [com.pringwa.server.interface :as server]
            [com.stuartsierra.component :as component]))

(defn new-system
  ([config] (new-system config true))
  ([config port]
   (component/system-map
     :conn (db/conn config)
     :app-state (app-state/create config)
     :server (server/create #'routes/router))))

(defn config
  []
  (->> (io/resource "service/config.edn")
       (aero/read-config)))

(defn -main [& [port]]
  (let [port (or port (get (System/getenv) "PORT" 8080))
        port (cond-> port (string? port) Integer/parseInt)
        _ (println "Starting up on port" port)]
    (->> (config)
         (new-system)
         (component/start))))
