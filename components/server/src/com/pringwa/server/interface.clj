(ns com.pringwa.server.interface
  (:require [com.pringwa.server.auth :as auth]
            [com.pringwa.server.circuit-breaker :as cb]
            [com.pringwa.server.compression :as compression]
            [com.pringwa.server.core :as server]
            [com.pringwa.server.cors :as cors]
            [com.pringwa.server.correlation :as correlation]
            [com.pringwa.server.metrics-state :as metrics]
            [com.pringwa.server.middleware :as middleware]
            [com.pringwa.server.rate-limit :as rate-limit]
            [com.pringwa.server.request-log :as request-log]
            [com.pringwa.server.timeout :as timeout]))

(defn create [router config]
  (server/create router config))

(defn wrap-exception [handler]
  (middleware/wrap-exception handler))

(defn wrap-cache-control [handler]
  (middleware/wrap-cache-control handler))

(defn wrap-authentication [handler opts]
  (auth/wrap-authentication handler opts))

(defn wrap-authorization [handler]
  (auth/wrap-authorization handler))

(defn wrap-cors [handler opts]
  (cors/wrap-cors handler opts))

(defn wrap-rate-limit [handler opts]
  (rate-limit/wrap-rate-limit handler opts))

(defn wrap-metrics [handler]
  (metrics/wrap-metrics handler))

(defn metrics-snapshot []
  (metrics/snapshot))

(defn wrap-timeout [handler opts]
  (timeout/wrap-timeout handler opts))

(defn wrap-circuit-breaker [handler opts]
  (cb/wrap-circuit-breaker handler opts))

(defn wrap-gzip [handler]
  (compression/wrap-gzip handler))

(defn wrap-correlation-id [handler]
  (correlation/wrap-correlation-id handler))

(defn wrap-request-log [handler]
  (request-log/wrap-request-log handler))
