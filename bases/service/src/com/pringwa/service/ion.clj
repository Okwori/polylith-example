(ns com.pringwa.service.ion
  "Ion web entry point. Only loaded in staging/prod Ion deployments —
   not required by local dev or the Component system."
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.pringwa.server.interface :as server]
            [com.pringwa.service.routes :as routes]
            [com.pringwa.service.secrets :as secrets]
            [datomic.client.api :as d]
            [datomic.ion :as ion]))

(defn- config []
  (let [profile (keyword (or (System/getenv "APP_PROFILE") "staging"))]
    (aero/read-config (io/resource "service/config.edn") {:profile profile})))

(defonce ^:private _publisher
  (delay (server/start-publisher! (get (config) :mulog {:type :console}))))

(defonce ^:private !secrets
  (delay
    (if-let [secret-name (:secret-name (config))]
      (do (log/infof "Fetching secrets from Secrets Manager: %s" secret-name)
          (secrets/fetch secret-name))
      {})))

(defonce ^:private !conn
  (delay
    (let [{:keys [system region endpoint]} (ion/get-env)
          db-name                          (get-in (config) [:database :db-name])
          client (d/client {:server-type :ion
                            :region      region
                            :system      system
                            :endpoint    endpoint
                            :proxy-port  8182})]
      (log/infof "Ion connecting to system=%s db=%s" system db-name)
      (d/connect client {:db-name db-name}))))

;; API Gateway JWT Authorizer validates the token before Ion is invoked.
;; wrap-authentication is still included (with no validator-fn) so the
;; middleware chain is identical to the standalone deployment — identity
;; is extracted from the request context forwarded by API Gateway.
;;
;; wrap-cors opts are empty for Ion: CORS is handled at the API Gateway level.
;; wrap-rate-limit is also at API Gateway level, but we include a conservative
;; in-app limit as defence-in-depth.
(def ^:private app
  (let [cfg     (config)
        sec     @!secrets
        _       @_publisher
        cors-opts (cond-> (get cfg :cors {})
                    (:cors-origin sec)
                    (assoc :allowed-origins [(:cors-origin sec)]))]
    (-> (routes/router)
        (server/wrap-connection @!conn)
        (server/wrap-authentication (cond-> {}
                                      (:jwt-public-key sec)
                                      (assoc :public-key (:jwt-public-key sec))))
        (server/wrap-timeout (get cfg :timeout {}))
        (server/wrap-circuit-breaker (get cfg :circuit-breaker {}))
        server/wrap-exception
        server/wrap-cache-control
        server/wrap-metrics
        server/wrap-request-log
        server/wrap-gzip
        (server/wrap-cors cors-opts)
        server/wrap-correlation-id
        (server/wrap-rate-limit (get cfg :rate-limit {})))))

(defn handler
  "Ion web handler — registered as the entry point in ion-config.edn.
   Receives a Ring request from API Gateway, returns a Ring response."
  [request]
  (app request))
