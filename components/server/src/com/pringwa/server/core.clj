(ns com.pringwa.server.core
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]))

(defrecord WebServer [handler-fn
                      app-state
                      server]
  component/Lifecycle
  (start [this]
    (if server
      this
      (let [{:keys [host port context-path]} (-> app-state :config :server)]
        (log/infof "web server running at %s:%s" host port)
        (assoc this
          :server (run-jetty (handler-fn app-state)
                             {:port port :join? false})))))
  (stop [this]
    (if server
      (do
        (.stop server)
        (assoc this :server nil))
      this)))

(defn create
  [handler-fn port]
  (component/using (map->WebServer {:handler-fn handler-fn :port port})
                   [:app-state]))

