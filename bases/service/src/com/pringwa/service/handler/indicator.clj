(ns com.pringwa.service.handler.indicator
  (:require [datomic.client.api :as d]
            [com.pringwa.persistence.interface :as store]))

(defn result [conn id]
  (->
    (store/find-document (d/db conn) id)
    store/transform-keys))

(defn handler
  [{:keys [conn reitit.core/match]}]
  (let [id (-> match :path-params :id)]
    {:status 200
     :body   {:result (result conn id)}}))
