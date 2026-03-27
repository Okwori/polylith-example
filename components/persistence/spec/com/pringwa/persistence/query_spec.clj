(ns com.pringwa.persistence.query-spec
  (:require [speclj.core :refer [describe it should-contain should-not should-not-be-nil should-not= should=]]
            [com.pringwa.persistence.query :as query]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- where [criteria]
  (-> (query/build-query criteria) :query :where))

(defn- or-clauses [criteria]
  (filter list? (where criteria)))

;; ---------------------------------------------------------------------------
;; Query structure
;; ---------------------------------------------------------------------------

(describe "query/build-query"
  (describe "query structure"
    (it "returns a map with a :query key"
      (should-contain :query (query/build-query {})))

    (it "sets :find to a pull on ?doc with pattern"
      (should= '[(pull ?doc pattern)]
               (-> (query/build-query {:adversary "X"}) :query :find)))

    (it "sets :in to bind $ and pattern"
      (should= '[$ pattern]
               (-> (query/build-query {:adversary "X"}) :query :in))))

  ;; -------------------------------------------------------------------------
  ;; Anchor clause
  ;; -------------------------------------------------------------------------

  (describe "anchor clause"
    (it "is always the first :where clause"
      (should= ['?doc :document/id] (first (where {}))))

    (it "is present even when criteria are empty"
      (should= [['?doc :document/id]] (where {})))

    (it "remains first when criteria are provided"
      (should= ['?doc :document/id] (first (where {:tlp "white"})))))

  ;; -------------------------------------------------------------------------
  ;; Scalar (db.cardinality/one) attributes
  ;; -------------------------------------------------------------------------

  (describe "scalar criteria"
    (it "compiles a scalar value to an equality clause"
      (should-contain ['?doc :document/tlp "white"]
                      (where {:tlp "white"})))

    (it "qualifies the attribute into the :document namespace"
      (should-contain ['?doc :document/id "abc123"]
                      (where {:id "abc123"})))

    (it "ANDs multiple scalar criteria (one clause each)"
      (let [clauses (where {:tlp "white" :id "abc123"})]
        (should= 3 (count clauses))
        (should-contain ['?doc :document/tlp "white"] clauses)
        (should-contain ['?doc :document/id "abc123"] clauses)))

    (it "produces no or-clauses for scalar criteria"
      (should= [] (or-clauses {:tlp "white" :id "abc123"})))

    (it "nil value produces a nil equality clause"
      (should-contain ['?doc :document/tlp nil]
                      (where {:tlp nil}))))

  ;; -------------------------------------------------------------------------
  ;; Many-cardinality attributes
  ;; -------------------------------------------------------------------------

  (describe ":many attribute criteria"
    (it "bare scalar on a :many attr produces a simple equality clause"
      (should-contain ['?doc :document/tags "china"]
                      (where {:tags "china"})))

    (it "single-element vector on a :many attr uses a simple clause (no or)"
      (let [clauses (where {:tags ["china"]})]
        (should-contain ['?doc :document/tags "china"] clauses)
        (should= [] (or-clauses {:tags ["china"]}))))

    (it "multi-element vector produces an or clause"
      (let [or-clause (first (or-clauses {:tags ["china" "apt"]}))]
        (should-not-be-nil or-clause)
        (should= 'or (first or-clause))
        (should-contain ['?doc :document/tags "china"] (rest or-clause))
        (should-contain ['?doc :document/tags "apt"] (rest or-clause))))

    (it "or clause covers all values in the vector"
      (let [or-clause (first (or-clauses {:tags ["china" "apt" "malware"]}))]
        (should= 3 (count (rest or-clause)))))

    (it ":industries is treated as a :many attr"
      (let [or-clause (first (or-clauses {:industries ["tech" "finance"]}))]
        (should= 'or (first or-clause))
        (should-contain ['?doc :document/industries "tech"] (rest or-clause))
        (should-contain ['?doc :document/industries "finance"] (rest or-clause))))

    (it ":targeted_countries is treated as a :many attr"
      (should-contain ['?doc :document/targeted_countries "Kuwait"]
                      (where {:targeted_countries "Kuwait"})))

    (it "empty vector on a :many attr adds no clause (no constraint)"
      (let [clauses (where {:tags []})]
        (should= [['?doc :document/id]] clauses))))

  ;; -------------------------------------------------------------------------
  ;; Fulltext-indexed attributes — scalar equality (Datomic Local compatible)
  ;; Note: schema declares :db/fulltext true on these attrs for Cloud indexing;
  ;; the DSL uses attribute equality which works across all Datomic flavours.
  ;; -------------------------------------------------------------------------

  (describe "fulltext-indexed attr criteria"
    (it "adversary produces a scalar equality clause"
      (should-contain ['?doc :document/adversary "Plead"]
                      (where {:adversary "Plead"})))

    (it "name produces a scalar equality clause"
      (should-contain ['?doc :document/name "Downloader"]
                      (where {:name "Downloader"})))

    (it "description produces a scalar equality clause"
      (should-contain ['?doc :document/description "APT"]
                      (where {:description "APT"})))

    (it "author_name produces a scalar equality clause"
      (should-contain ['?doc :document/author_name "AlienVault"]
                      (where {:author_name "AlienVault"})))

    (it "adversary clause has no function-call wrapper"
      (let [clauses (where {:adversary "Plead"})]
        (should-not (some #(and (vector? %) (list? (first %))) clauses)))))

  ;; -------------------------------------------------------------------------
  ;; Mixed criteria
  ;; -------------------------------------------------------------------------

  (describe "mixed scalar + many criteria"
    (it "produces anchor + equality clause + or clause"
      (let [clauses (where {:adversary "Plead" :tags ["china" "apt"]})]
        (should= 3 (count clauses))))

    (it "includes the equality clause for adversary"
      (should-contain ['?doc :document/adversary "Plead"]
                      (where {:adversary "Plead" :tags ["china" "apt"]})))

    (it "includes the or clause for the :many attr"
      (should-not= [] (or-clauses {:adversary "Plead" :tags ["china" "apt"]})))

    (it ":tlp and :name both produce scalar equality clauses"
      (let [clauses    (where {:tlp "white" :name "Plead"})
            eq-clauses (filter #(= (first %) '?doc) clauses)]
        (should= 3 (count eq-clauses))
        (should-contain ['?doc :document/tlp "white"] clauses)
        (should-contain ['?doc :document/name "Plead"] clauses)))))
