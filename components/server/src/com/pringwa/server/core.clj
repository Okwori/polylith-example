(ns com.pringwa.server.core
  (:require [clojure.tools.logging :as log]
            [com.pringwa.server.middleware :refer [wrap-connection]]
            [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]))

(defrecord WebServer [handler-fn database config server]
  component/Lifecycle

  (start [this]
    (if server
      this
      (let [{:keys [host port] :or {host "localhost" port 8080}} (:server config)
            conn (:conn database)
            base-handler (handler-fn)
            handler (wrap-connection base-handler conn)
            http-srv (run-jetty handler {:host host :port port :join? false})]
        (log/infof "Web server running at %s:%s" host port)
        (assoc this :server http-srv))))

  (stop [this]
    (when server
      (.stop server))
    (assoc this :server nil)))

(defn create
  [router config]
  (component/using (map->WebServer {:handler-fn router
                                    :config config})
                   [:database]))
