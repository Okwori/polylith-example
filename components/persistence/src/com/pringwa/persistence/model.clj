(ns com.pringwa.persistence.model
  (:require [com.pringwa.persistence.cache :as cache]
            [com.pringwa.persistence.query :as query]
            [datomic.client.api :as d]))

(def document-pattern
  "Full pull pattern for a document entity.  Exposed so callers can derive
  restricted variants for attribute-level access policies."
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

;; ---------------------------------------------------------------------------
;; Access-policy helpers
;; ---------------------------------------------------------------------------

(defn- effective-pattern [access-policy]
  (or (:pull-pattern access-policy) document-pattern))

(defn- apply-visibility
  "Injects entity-level visibility rules into a query map when the access-policy
  carries rules.  Rules must implement the (visible? ?doc) predicate.
  The rules vector is added to :in as % and (visible? ?doc) to :where."
  [q access-policy]
  (if-let [rules (:entity-rules access-policy)]
    (-> q
        (update :in conj '%)
        (update :where conj '(visible? ?doc)))
    q))

(defn- entity-args
  "Base args vector: [db] plus rules when present.
  Each query function appends its own positional bindings after this."
  [db access-policy]
  (cond-> [db]
    (:entity-rules access-policy) (conj (:entity-rules access-policy))))

;; ---------------------------------------------------------------------------
;; Query functions
;; ---------------------------------------------------------------------------

(defn find-document
  ([db id]            (find-document db id nil))
  ([db id access-policy]
   (cache/lookup-or-miss
     [(cache/basis-t db) :find-doc id access-policy]
     (fn []
       (let [pattern (effective-pattern access-policy)
             q       (apply-visibility
                       {:find  [(list 'pull '?doc pattern)]
                        :in    '[$ ?doc-id]
                        :where '[[?doc :document/id ?doc-id]]}
                       access-policy)]
         (ffirst (d/q {:query q
                       :args  (conj (entity-args db access-policy) id)})))))))

(defn find-all-documents
  ([db]              (find-all-documents db nil))
  ([db access-policy]
   (cache/lookup-or-miss
     [(cache/basis-t db) :find-all access-policy]
     (fn []
       (let [pattern (effective-pattern access-policy)
             q       (apply-visibility
                       {:find  [(list 'pull '?doc 'pattern)]
                        :in    '[$ pattern]
                        :where '[[?doc :document/id]]}
                       access-policy)]
         (mapv first (d/q {:query q
                           :args  (conj (entity-args db access-policy) pattern)})))))))

(defn find-document-by-type
  ([db type]              (find-document-by-type db type nil))
  ([db type access-policy]
   (cache/lookup-or-miss
     [(cache/basis-t db) :find-by-type type access-policy]
     (fn []
       (let [pattern (effective-pattern access-policy)
             q       (apply-visibility
                       {:find  [(list 'pull '?doc 'pattern)]
                        :in    '[$ ?type-str pattern]
                        :where '[[?doc :document/indicators ?ind]
                                 [?ind :indicator/type ?type-str]]}
                       access-policy)]
         (mapv first (d/q {:query q
                           :args  (into (entity-args db access-policy) [type pattern])})))))))

(defn search-documents
  "Executes a compiled Datomic query derived from a criteria map.
  Pushes all filtering into the database — no in-memory scan."
  ([db criteria]              (search-documents db criteria nil))
  ([db criteria access-policy]
   (cache/lookup-or-miss
     [(cache/basis-t db) :search criteria access-policy]
     (fn []
       (let [pattern (effective-pattern access-policy)
             {:keys [query]} (query/build-query criteria)
             q       (apply-visibility query access-policy)]
         (mapv first (d/q {:query q
                           :args  (conj (entity-args db access-policy) pattern)})))))))
