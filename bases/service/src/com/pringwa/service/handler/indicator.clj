(ns com.pringwa.service.handler.indicator
  (:require [datomic.client.api :as d]
            [com.pringwa.persistence.interface :as store]))

(defn result [conn id]
  (-> (store/find-document (d/db conn) id)
      store/transform-keys))

(defn handler
  [{:keys [conn path-params]}]
  (let [id  (:id path-params)
        doc (result conn id)]
    (if doc
      {:status 200 :body {:result doc}}
      {:status 404 :body {:error (str "No indicator found with id: " id)}})))
