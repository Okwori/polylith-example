(ns com.pringwa.service.handler.indicators
  (:require [com.pringwa.persistence.interface :as store]
            [datomic.client.api :as d]))

(defn handler
  [{:keys [conn]}]
  (let [conn (:conn (store/init-db))]
    {:status 200
     :body   {:result (-> (store/findAllDocuments (d/db conn))
                          store/transform-keys)}}))
