(ns com.pringwa.persistence.schema
  (:require [datomic.client.api :as d]))

(defn- attr
  ([ident type cardinality] (attr ident type cardinality nil))
  ([ident type cardinality doc] (attr ident type cardinality doc nil))
  ([ident type cardinality doc more]
   (let [type (keyword "db.type" (name type))
         cardinality (keyword "db.cardinality" (name cardinality))
         more (if doc (assoc more :db/doc doc) more)]
     (merge {:db/ident ident
             :db/valueType type
             :db/cardinality cardinality}
            more))))

(defn- component
  "A component attribute"
  ([ident cardinality]
   (dissoc (attr ident :ref cardinality "" {:db/isComponent true}) :db/doc))
  ([ident cardinality doc]
   (attr ident :ref cardinality doc {:db/isComponent true})))

(def document-schema
  [(attr :document/industries :string :many "The industries ...")
   (attr :document/tlp :string :one "TLP ...")
   (attr :document/description :string :one "The description of the document" {:db/fulltext true})
   (attr :document/created :instant :one "When document was created")
   (attr :document/tags :string :many "The document tags")
   (attr :document/modified :instant :one "modified ...")
   (attr :document/author_name :string :one "author_name ..." {:db/fulltext true})
   (attr :document/public :long :one "public ...")
   (attr :document/extract_source :string :many "extract source ...")
   (attr :document/references :string :many "references ...")
   (attr :document/targeted_countries :string :many "targeted countries ...")
   (attr :document/more_indicators :boolean :one "more indicators ...")
   (attr :document/revision :long :one "revision ...")
   (attr :document/adversary :string :one "adversary ..." {:db/fulltext true})
   (attr :document/id :string :one "id ...")
   (attr :document/name :string :one "name ..." {:db/fulltext true})
   (component :document/indicators :many "Indicators")])

(def indicator-schema
  [(attr :indicator/indicator :string :one "The indicator")
   (attr :indicator/description :string :one "The desc of the indicator")
   (attr :indicator/created :instant :one "When it was created")
   (attr :indicator/title :string :one "The title ...")
   (attr :indicator/type :string :one "The type ...")
   (attr :indicator/content :string :one "The content ...")
   (attr :indicator/id :long :one "The ID ...")])

(def schema
  (vec
    (concat document-schema
            indicator-schema
            [(attr :transaction/namespace :string :one "Which namespace initiated the transaction")])))

(defn install-schema! [conn]
  (d/transact conn {:tx-data schema}))
