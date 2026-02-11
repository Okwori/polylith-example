(ns com.pringwa.service.handler.indicator
  (:require [datomic.client.api :as d]
            [com.pringwa.persistence.interface :as store]))

(def document-pattern
  [:document/id
   :document/name
   :document/tlp
   :document/author_name
   :document/description
   :document/created
   :document/modified
   :document/revision
   :document/public
   :document/adversary
   :document/more_indicators
   :document/tags
   :document/industries
   :document/references
   :document/extract_source
   :document/targeted_countries
   {:document/indicators
    [:indicator/indicator
     :indicator/type
     :indicator/id
     :indicator/created
     :indicator/title
     :indicator/description
     :indicator/content]}])

(defn findDocument [db id]
  (let [query '{:find [(pull ?document pattern)]
                :in [$ ?document-id pattern]
                :where [[?document :document/id ?document-id]]}]
    (ffirst (d/q {:query query, :args [db id document-pattern]}))))

(defn handler
  [{:keys [reitit.core/match conn] :as req}]
  (let [id (-> match :path-params :id)
        conn (:conn (store/init-db))]
    {:status 200
    :body   {:result id
             :db-value-from-conn (->
                                   (findDocument (d/db conn) id)
                                   (store/transform-keys))}}))
