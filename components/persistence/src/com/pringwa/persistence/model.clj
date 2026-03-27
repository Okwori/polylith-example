(ns com.pringwa.persistence.model
  (:require [com.pringwa.persistence.cache :as cache]
            [com.pringwa.persistence.query :as query]
            [datomic.client.api :as d]))

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
  (cache/lookup-or-miss
    [(cache/basis-t db) :find-doc id]
    (fn []
      (let [q '{:find [(pull ?document pattern)]
                :in [$ ?document-id pattern]
                :where [[?document :document/id ?document-id]]}]
        (ffirst (d/q {:query q :args [db id document-pattern]}))))))

(defn find-all-documents [db]
  (cache/lookup-or-miss
    [(cache/basis-t db) :find-all]
    (fn []
      (let [q '{:find [(pull ?document pattern)]
                :in [$ pattern]
                :where [[?document :document/id]]}]
        (mapv first (d/q {:query q :args [db document-pattern]}))))))

(defn find-document-by-type [db type]
  (cache/lookup-or-miss
    [(cache/basis-t db) :find-by-type type]
    (fn []
      (let [q '{:find [(pull ?document pattern)]
                :in [$ ?type-str pattern]
                :where [[?document :document/indicators ?indicators]
                        [?indicators :indicator/type ?type-str]]}]
        (mapv first (d/q {:query q :args [db type document-pattern]}))))))

(defn search-documents
  "Executes a compiled Datomic query derived from a criteria map.
  Pushes all filtering into the database — no in-memory scan."
  [db criteria]
  (cache/lookup-or-miss
    [(cache/basis-t db) :search criteria]
    (fn []
      (let [{:keys [query]} (query/compile criteria)]
        (mapv first (d/q {:query query :args [db document-pattern]}))))))
