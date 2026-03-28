(ns com.pringwa.service.handler.filter-indicators
  (:require [clojure.string :as str]
            [com.pringwa.persistence.interface :as store]))

(def ^:private valid-criteria-keys
  #{:adversary :tlp :author_name :description :tags
    :industries :targeted_countries :name :id})

(defn- unknown-keys [criteria]
  (remove valid-criteria-keys (keys criteria)))

(defn- parse-pagination [query-params]
  (letfn [(safe-long [k default]
            (let [v (some-> (get query-params k) str/trim)]
              (if (and v (re-matches #"\d+" v))
                (Long/parseLong v)
                default)))]
    {:limit  (safe-long "limit" 20)
     :offset (safe-long "offset" 0)}))

(defn response [{:keys [db access-policy param pagination]}]
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
       :body   (-> (store/search-documents db param access-policy)
                   store/transform-keys
                   (store/paginate pagination))})))

(defn handler
  [{:keys [body-params query-params db access-policy]}]
  (response {:db            db
             :access-policy access-policy
             :param         body-params
             :pagination    (parse-pagination query-params)}))
