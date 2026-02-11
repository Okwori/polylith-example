(ns com.pringwa.persistence.model
  (:require [datomic.client.api :as d]))

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

(defn find-document [db id]
  (let [query '{:find [(pull ?document pattern)]
                :in [$ ?document-id pattern]
                :where [[?document :document/id ?document-id]]}]
    (ffirst (d/q {:query query, :args [db id document-pattern]}))))

(defn find-all-documents [db]
  (let [query '{:find [(pull ?document pattern)]
                :in [$ pattern]
                :where [[?document :document/id]]}
        result (d/q {:query query, :args [db document-pattern]})]
    (mapv first result)))

(defn find-document-by-type [db type]
  (let [query '{:find [(pull ?document pattern)]
                :in [$ ?type-str pattern]
                :where [[?document :document/indicators ?indicators]
                        [?indicators :indicator/type ?type-str]]}
        result (d/q {:query query, :args [db type document-pattern]})]
    (mapv first result)))

