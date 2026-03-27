(ns com.pringwa.service.handler.filter-indicators
  (:require [com.pringwa.persistence.interface :as store]))

(def ^:private valid-criteria-keys
  #{:adversary :tlp :author_name :description :tags
    :industries :targeted_countries :name :id})

(defn- unknown-keys [criteria]
  (remove valid-criteria-keys (keys criteria)))

(defn response [{:keys [db access-policy param]}]
  (let [bad-keys (seq (unknown-keys param))]
    (cond
      (empty? param)
      {:status 400
       :body   {:error "Please provide search criteria"}}

      bad-keys
      {:status 400
       :body   {:error (str "Unknown search criteria: " (vec bad-keys))}}

      :else
      {:status 200
       :body   {:results (-> (store/search-documents db param access-policy)
                             store/transform-keys)}})))

(defn handler
  [{:keys [body-params db access-policy]}]
  (response {:db            db
             :access-policy access-policy
             :param         body-params}))
