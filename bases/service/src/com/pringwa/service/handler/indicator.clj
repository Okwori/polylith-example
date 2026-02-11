(ns com.pringwa.service.handler.indicator
  (:require [datomic.client.api :as d]
            [com.pringwa.persistence.interface :as store]))

(defn handler
  [{:keys [reitit.core/match] :as req}]
  (let [id (-> match :path-params :id)
        conn (:conn (store/init-db))]
    {:status 200
     :body   {:result (->
                        (store/find-document (d/db conn) id)
                        (store/transform-keys))}}))
