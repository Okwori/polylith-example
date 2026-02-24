(ns com.pringwa.service.handler.filter-indicators
  (:require [com.pringwa.persistence.interface :as store]
            [datomic.client.api :as d]))

(defn response [{:keys [conn param]}]
  (if (empty? param)
    {:status 400
     :body   {:error "Please provide search criteria"}}
    {:status 200
     :body   {:results (->> (store/find-all-documents (d/db conn))
                            (filter #(store/matches? % param))
                            store/transform-keys)}}))

(defn handler
  [{:keys [body-params conn]}]
  (response {:conn conn
             :param body-params}))