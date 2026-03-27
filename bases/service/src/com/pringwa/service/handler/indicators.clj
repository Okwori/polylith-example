(ns com.pringwa.service.handler.indicators
  (:require [clojure.string :as str]
            [com.pringwa.persistence.interface :as store]))

(defn result [db access-policy type]
  (-> (if (str/blank? type)
        (store/find-all-documents db access-policy)
        (store/find-document-by-type db type access-policy))
      store/transform-keys))

(defn handler
  [{:keys [db access-policy query-params]}]
  (let [type (some-> (get query-params "type") str/trim)]
    {:status 200
     :body   {:results (result db access-policy type)}}))
