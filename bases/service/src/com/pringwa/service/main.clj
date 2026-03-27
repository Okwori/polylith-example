(ns com.pringwa.service.main
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.pringwa.persistence.interface :as db]
            [com.pringwa.server.interface :as server]
            [com.pringwa.service.routes :as routes]
            [com.stuartsierra.component :as component])
  (:gen-class))

(defn config
  ([]
   (config (keyword (or (System/getenv "APP_PROFILE") "dev"))))
  ([profile]
   (aero/read-config (io/resource "service/config.edn") {:profile profile})))

(defn new-system
  [config]
  (component/system-map
    :database (db/create config)
    :server   (server/create #'routes/router config)))

(defn -main [& [host port]]
  (let [host (or host "localhost")
        port (or (cond-> port (string? port) Integer/parseInt) 8080)
        _    (log/infof "Starting up on port %s:%s" host port)]
    (->> (config)
         (new-system)
         (component/start))))
