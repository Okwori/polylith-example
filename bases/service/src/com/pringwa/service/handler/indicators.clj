(ns com.pringwa.service.handler.indicators
  (:require [clojure.string :as str]
            [com.pringwa.persistence.interface :as store]))

(defn- parse-pagination [query-params]
  (letfn [(safe-long [k default]
            (let [v (some-> (get query-params k) str/trim)]
              (if (and v (re-matches #"\d+" v))
                (Long/parseLong v)
                default)))]
    {:limit  (safe-long "limit" 20)
     :offset (safe-long "offset" 0)}))

(defn result [db access-policy type]
  (-> (if (str/blank? type)
        (store/find-all-documents db access-policy)
        (store/find-document-by-type db type access-policy))
      store/transform-keys))

(defn handler
  [{:keys [db access-policy query-params]}]
  (let [type   (some-> (get query-params "type") str/trim)
        page   (parse-pagination query-params)
        docs   (result db access-policy type)]
    {:status 200
     :body   (store/paginate docs page)}))
