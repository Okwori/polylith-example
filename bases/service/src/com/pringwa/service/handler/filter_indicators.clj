(ns com.pringwa.service.handler.filter-indicators
  (:require [com.pringwa.persistence.interface :as store]
            [datomic.client.api :as d]))

(defn handler
  [{:keys [body-params] :as req}]
  (let [conn     (:conn (store/init-db))
        criteria body-params]
    (if (empty? criteria)
      {:status 400
       :body   {:error "Please provide search criteria"}}
      {:status 200
       :body   {:results (->> (store/find-all-documents (d/db conn))
                              (filter #(store/matches? % criteria))
                              store/transform-keys)}})))