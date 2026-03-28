(ns com.pringwa.server.core
  (:require [clojure.tools.logging :as log]
            [com.pringwa.server.auth :refer [wrap-authentication]]
            [com.pringwa.server.cors :refer [wrap-cors]]
            [com.pringwa.server.metrics-state :refer [wrap-metrics]]
            [com.pringwa.server.middleware :refer [wrap-cache-control wrap-connection wrap-exception]]
            [com.pringwa.server.rate-limit :refer [wrap-rate-limit]]
            [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]))

(defrecord WebServer [handler-fn database config server]
  component/Lifecycle

  (start [this]
    (if server
      this
      (let [{:keys [host port] :or {host "localhost" port 8080}} (:server config)
            auth-opts   (get config :auth {})
            cors-opts   (get config :cors {})
            rate-opts   (get config :rate-limit {})
            conn        (:conn database)
            base-handler (handler-fn)
            handler     (-> base-handler
                            (wrap-connection conn)
                            (wrap-authentication auth-opts)
                            wrap-exception
                            wrap-cache-control
                            wrap-metrics
                            (wrap-cors cors-opts)
                            (wrap-rate-limit rate-opts))
            http-srv    (run-jetty handler {:host host :port port :join? false})]
        (log/infof "Web server running at %s:%s" host port)
        (assoc this :server http-srv))))

  (stop [this]
    (when server
      (.stop server))
    (assoc this :server nil)))

(defn create [router config]
  (component/using (map->WebServer {:handler-fn router
                                    :config     config})
                   [:database]))
