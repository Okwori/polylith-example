(ns com.pringwa.service.scoped-db
  "Middleware that derives an immutable Datomic db snapshot and an access-policy
  from the request's :conn and :identity, then passes both to the handler.

  The access-policy (built by access-policy/from-identity) carries:
    :entity-rules  — Datalog rules for (visible? ?doc), evaluated inside Datomic
    :pull-pattern  — attribute whitelist replacing the default document-pattern

  Placing this middleware inside the Reitit router stack ensures it runs after
  :identity has been attached by wrap-authentication."
  (:require [com.pringwa.service.access-policy :as policy]
            [datomic.client.api :as d]))

(defn wrap-scoped-db
  [handler]
  (fn [{:keys [conn identity] :as request}]
    (handler (assoc request
                    :db            (d/db conn)
                    :access-policy (policy/from-identity identity)))))
