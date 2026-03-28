(ns com.pringwa.server.core
  (:require [clojure.tools.logging :as log]
            [com.pringwa.server.auth :refer [wrap-authentication]]
            [com.pringwa.server.circuit-breaker :refer [wrap-circuit-breaker]]
            [com.pringwa.server.compression :refer [wrap-gzip]]
            [com.pringwa.server.correlation :refer [wrap-correlation-id]]
            [com.pringwa.server.cors :refer [wrap-cors]]
            [com.pringwa.server.metrics-state :refer [wrap-metrics]]
            [com.pringwa.server.middleware :refer [wrap-cache-control wrap-connection wrap-exception]]
            [com.pringwa.server.rate-limit :refer [wrap-rate-limit]]
            [com.pringwa.server.request-log :refer [wrap-request-log]]
            [com.pringwa.server.timeout :refer [wrap-timeout]]
            [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]])
  (:import (org.eclipse.jetty.server Server)))

;; Middleware stack (outermost → innermost on the request path):
;;
;;   rate-limit        fast reject before doing any work
;;   correlation-id    generate / propagate X-Request-ID, populate MDC
;;   cors              preflight + response headers
;;   gzip              compress serialised response body
;;   request-log       structured access log after response is known
;;   metrics           record status + latency counters
;;   cache-control     Cache-Control: no-store on every response
;;   exception         catch unhandled exceptions → 500
;;   circuit-breaker   protect dependencies, return 503 when open
;;   timeout           enforce per-request deadline, return 504
;;   authentication    validate Bearer JWT → :identity
;;   connection        inject :conn
;;   ── Reitit router ──────────────────────────────────────────────────────
;;   authorization     scope check → 401/403
;;   scoped-db         derive :db snapshot + :access-policy
;;   handler

(defrecord WebServer [handler-fn database config server]
  component/Lifecycle

  (start [this]
    (if server
      this
      (let [{:keys [host port] :or {host "localhost" port 8080}} (:server config)
            auth-opts    (get config :auth {})
            cors-opts    (get config :cors {})
            rate-opts    (get config :rate-limit {})
            timeout-opts (get config :timeout {})
            cb-opts      (get config :circuit-breaker {})
            conn         (:conn database)
            base-handler (handler-fn)
            handler      (-> base-handler
                             (wrap-connection conn)
                             (wrap-authentication auth-opts)
                             (wrap-timeout timeout-opts)
                             (wrap-circuit-breaker cb-opts)
                             wrap-exception
                             wrap-cache-control
                             wrap-metrics
                             wrap-request-log
                             wrap-gzip
                             (wrap-cors cors-opts)
                             wrap-correlation-id
                             (wrap-rate-limit rate-opts))
            stop-timeout (get-in config [:server :stop-timeout-ms] 10000)
            http-srv     (run-jetty handler
                           {:host        host
                            :port        port
                            :join?       false
                            :configurator (fn [^Server s]
                                            (.setStopTimeout s stop-timeout))})]
        (log/infof "Web server running at %s:%s" host port)
        (assoc this :server http-srv))))

  (stop [this]
    (when server
      (.stop server))        ; honours stop-timeout before force-closing connections
    (assoc this :server nil)))

(defn create [router config]
  (component/using (map->WebServer {:handler-fn router
                                    :config     config})
                   [:database]))
