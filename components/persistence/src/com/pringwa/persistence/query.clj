(ns com.pringwa.persistence.query
  "DSL that compiles a search-criteria map into a Datomic Datalog query.

  Each key in the criteria map is a document attribute keyword (without the
  :document/ namespace).  Values are matched according to three dispatch rules:

    - fulltext-attrs  → (fulltext $ :document/K \"v\") [[?doc]]   tokenised, stemmed, case-insensitive
    - many-attrs      → [?doc :document/K v] or (or ...)          set membership
    - everything else → [?doc :document/K v]                      exact equality

  Multiple keys are AND-ed (multiple :where clauses).

  An anchor clause [?doc :document/id] is always prepended so the query is
  scoped to document entities and benefits from the :document/id index.

  Example:
    (compile {:adversary \"Plead\" :tags [\"china\" \"apt\"]})
    ;;=> {:query {:find  [(pull ?doc pattern)]
    ;;            :in    [$ pattern]
    ;;            :where [[?doc :document/id]
    ;;                    [(fulltext $ :document/adversary \"Plead\") [[?doc]]]
    ;;                    (or [?doc :document/tags \"china\"]
    ;;                        [?doc :document/tags \"apt\"])]}}")

;; ---------------------------------------------------------------------------
;; Schema knowledge
;; ---------------------------------------------------------------------------

(def ^:private fulltext-attrs
  "Attributes declared with :db/fulltext true in the schema.
  Searched using Datomic's fulltext function: tokenised, stemmed, case-insensitive."
  #{:name :description :adversary :author_name})

(def ^:private many-attrs
  "Attributes with db.cardinality/many — matched by set-membership logic."
  #{:tags :industries :targeted_countries :extract_source :references})

;; ---------------------------------------------------------------------------
;; Clause builders
;; ---------------------------------------------------------------------------

(defn- doc-attr
  "Qualifies a bare keyword into the :document namespace."
  [k]
  (keyword "document" (name k)))

(defn- scalar-clause [k v]
  [['?doc (doc-attr k) v]])

(defn- fulltext-clause
  "Produces a Datomic fulltext function expression clause.
  Binds the matched entity to ?doc, unifying with the rest of the query."
  [k v]
  [[(list 'fulltext '$ (doc-attr k) v) [['?doc]]]])

(defn- membership-clauses
  "Produces a single equality clause for one value, or an `or` clause for many."
  [k values]
  (let [attr   (doc-attr k)
        values (if (sequential? values) values [values])]
    (if (= 1 (count values))
      [['?doc attr (first values)]]
      [(apply list 'or (map (fn [v] ['?doc attr v]) values))])))

(defn- entry->clauses [[k v]]
  (cond
    (contains? fulltext-attrs k) (fulltext-clause k v)
    (contains? many-attrs k)     (membership-clauses k v)
    :else                        (scalar-clause k v)))

;; ---------------------------------------------------------------------------
;; Public API
;; ---------------------------------------------------------------------------

(defn build-query
  "Compiles a criteria map into a Datomic pull-query.

  Returns:
    {:query {:find [...] :in [...] :where [...]}}

  The caller supplies :args [db pattern] at query time:
    (d/q {:query (:query (build-query criteria)) :args [db pattern]})"
  [criteria]
  {:query {:find  '[(pull ?doc pattern)]
           :in    '[$ pattern]
           :where (into [['?doc :document/id]]
                        (mapcat entry->clauses criteria))}})
