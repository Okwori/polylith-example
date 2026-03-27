(ns com.pringwa.service.ion
  "Ion web entry point. Only loaded in staging/prod Ion deployments —
   not required by local dev or the Component system."
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.pringwa.server.interface :as server]
            [com.pringwa.service.routes :as routes]
            [datomic.client.api :as d]
            [datomic.ion :as ion]))

(defn- config []
  (let [profile (keyword (or (System/getenv "APP_PROFILE") "staging"))]
    (aero/read-config (io/resource "service/config.edn") {:profile profile})))

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
(def ^:private app
  (-> (routes/router)
      (server/wrap-connection @!conn)
      (server/wrap-authentication {})
      server/wrap-exception))

(defn handler
  "Ion web handler — registered as the entry point in ion-config.edn.
   Receives a Ring request from API Gateway, returns a Ring response."
  [request]
  (app request))
