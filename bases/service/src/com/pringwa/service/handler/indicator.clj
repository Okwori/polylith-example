(ns com.pringwa.service.handler.indicator
  (:require [datomic.client.api :as d]))

(defn handler
  [{:keys [reitit.core/match app-state] :as req}]
  (let [id (-> match :path-params :id Long/parseLong)]
    {:status 200
    :body   {:result id
             :db-value-from-conn (keys req)
             ;(d/q '[:find ?e :where [?e :db/ident]] (d/db conn))
             }}))
