(ns com.pringwa.service.handler.filter-indicators
  (:require [com.pringwa.persistence.interface :as db]
            [datomic.client.api :as d]))

(defn handler
  [{:keys [reitit.core/match conn]}]
  {:status 200
   :body   {:result conn
            :db-result (let [conn (d/connect (d/client {:server-type :datomic-local
                                                   :storage-dir :mem
                                                   :system      "indicators"})
                                        {:db-name "indicators"})]
                         (d/q '[:find ?v :where [?e :db/ident ?v]] (d/db conn)))}})
