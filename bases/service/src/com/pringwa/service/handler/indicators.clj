(ns com.pringwa.service.handler.indicators
  (:require [com.pringwa.persistence.interface :as store]
            [datomic.client.api :as d]))

(defn handler
  [{:keys [query-params] :as req}]
  (let [conn (:conn (store/init-db))
        type (not-empty (get query-params "type"))]
    {:status 200
     :body   (if type
               {:result (-> (store/find-document-by-type (d/db conn) type)
                            store/transform-keys)}
               {:result (-> (store/find-all-documents (d/db conn))
                            store/transform-keys)})}))
