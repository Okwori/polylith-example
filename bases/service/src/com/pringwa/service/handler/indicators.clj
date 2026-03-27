(ns com.pringwa.service.handler.indicators
  (:require [clojure.string :as str]
            [com.pringwa.persistence.interface :as store]
            [datomic.client.api :as d]))

(defn result [db type]
  (-> (if (str/blank? type)
        (store/find-all-documents db)
        (store/find-document-by-type db type))
      store/transform-keys))

(defn handler
  [{:keys [conn query-params]}]
  (let [type (some-> (get query-params "type") str/trim)]
    {:status 200
     :body   {:results (result (d/db conn) type)}}))
