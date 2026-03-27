(ns com.pringwa.persistence.query
  "DSL that compiles a search-criteria map into a Datomic Datalog query.

  Each key in the criteria map is a document attribute keyword (without the
  :document/ namespace).  Values are matched according to two dispatch rules:

    - many-attrs      → [?doc :document/K v] or (or ...)          set membership
    - everything else → [?doc :document/K v]                      exact equality

  Multiple keys are AND-ed (multiple :where clauses).

  An anchor clause [?doc :document/id] is always prepended so the query is
  scoped to document entities and benefits from the :document/id index.

  Attributes declared :db/fulltext true in the schema (name, description,
  adversary, author_name) benefit from Datomic's fulltext index when running
  against Datomic Cloud or On-Prem; on Datomic Local :mem they fall back to
  exact attribute equality.

  Example:
    (build-query {:adversary \"Plead\" :tags [\"china\" \"apt\"]})
    ;;=> {:query {:find  [(pull ?doc pattern)]
    ;;            :in    [$ pattern]
    ;;            :where [[?doc :document/id]
    ;;                    [?doc :document/adversary \"Plead\"]
    ;;                    (or [?doc :document/tags \"china\"]
    ;;                        [?doc :document/tags \"apt\"])]}}")

;; ---------------------------------------------------------------------------
;; Schema knowledge
;; ---------------------------------------------------------------------------

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

(defn- membership-clauses
  "Produces a single equality clause for one value, or an `or` clause for many.
  Returns an empty vector for an empty collection (no constraint added)."
  [k values]
  (let [attr   (doc-attr k)
        values (if (sequential? values) values [values])]
    (cond
      (empty? values)    []
      (= 1 (count values)) [['?doc attr (first values)]]
      :else [(apply list 'or (map (fn [v] ['?doc attr v]) values))])))

(defn- entry->clauses [[k v]]
  (if (contains? many-attrs k)
    (membership-clauses k v)
    (scalar-clause k v)))

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
