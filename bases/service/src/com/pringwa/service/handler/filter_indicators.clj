(ns com.pringwa.service.handler.filter-indicators
  (:require [com.pringwa.persistence.interface :as store]
            [datomic.client.api :as d]))

(def ^:private valid-criteria-keys
  #{:adversary :tlp :author_name :description :tags
    :industries :targeted_countries :name :id})

(defn- unknown-keys [criteria]
  (remove valid-criteria-keys (keys criteria)))

(defn response [{:keys [conn param]}]
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
       :body   {:results (->> (store/find-all-documents (d/db conn))
                              (filter #(store/matches? % param))
                              store/transform-keys)}})))

(defn handler
  [{:keys [body-params conn]}]
  (response {:conn conn
             :param body-params}))
