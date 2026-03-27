(ns com.pringwa.service.handler.indicator
  (:require [com.pringwa.persistence.interface :as store]))

(defn result [db access-policy id]
  (-> (store/find-document db id access-policy)
      store/transform-keys))

(defn handler
  [{:keys [db access-policy path-params]}]
  (let [id  (:id path-params)
        doc (result db access-policy id)]
    (if doc
      {:status 200 :body {:result doc}}
      {:status 404 :body {:error (str "No indicator found with id: " id)}})))
