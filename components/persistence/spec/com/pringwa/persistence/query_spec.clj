(ns com.pringwa.persistence.query-spec
  (:require [speclj.core :refer :all]
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
      (should= [] (or-clauses {:tlp "white" :id "abc123"}))))

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
                      (where {:targeted_countries "Kuwait"}))))

  ;; -------------------------------------------------------------------------
  ;; Fulltext attributes
  ;; -------------------------------------------------------------------------

  (describe "fulltext criteria"
    (it "adversary produces a fulltext clause, not an equality clause"
      (let [clauses (where {:adversary "Plead"})
            ft-clause (second clauses)]
        (should-be vector? ft-clause)
        (should-be list? (first ft-clause))
        (should= 'fulltext (first (first ft-clause)))))

    (it "fulltext clause targets the correct attribute"
      (let [ft-clause (second (where {:adversary "Plead"}))
            fn-call (first ft-clause)]
        (should= :document/adversary (nth fn-call 2))))

    (it "fulltext clause carries the search term"
      (let [ft-clause (second (where {:adversary "Plead"}))
            fn-call (first ft-clause)]
        (should= "Plead" (nth fn-call 3))))

    (it "fulltext clause binds ?doc"
      (let [ft-clause (second (where {:adversary "Plead"}))
            binding (second ft-clause)]
        (should= [['?doc]] binding)))

    (it ":name uses fulltext"
      (let [ft-clause (second (where {:name "Downloader"}))
            fn-call (first ft-clause)]
        (should= :document/name (nth fn-call 2))))

    (it ":description uses fulltext"
      (let [ft-clause (second (where {:description "APT"}))
            fn-call (first ft-clause)]
        (should= :document/description (nth fn-call 2))))

    (it ":author_name uses fulltext"
      (let [ft-clause (second (where {:author_name "AlienVault"}))
            fn-call (first ft-clause)]
        (should= :document/author_name (nth fn-call 2))))

    (it "fulltext does not produce an equality clause"
      (let [clauses (where {:adversary "Plead"})]
        (should-not-contain ['?doc :document/adversary "Plead"] clauses)))

    (it "fulltext does not produce an equality clause for :name"
      (let [clauses (where {:name "Downloader"})]
        (should-not-contain ['?doc :document/name "Downloader"] clauses))))

  ;; -------------------------------------------------------------------------
  ;; Mixed criteria
  ;; -------------------------------------------------------------------------

  (describe "mixed fulltext + many criteria"
    (it "produces anchor + fulltext clause + or clause"
      (let [clauses (where {:adversary "Plead" :tags ["china" "apt"]})]
        (should= 3 (count clauses))))

    (it "includes the fulltext clause for adversary"
      (let [clauses (where {:adversary "Plead" :tags ["china" "apt"]})]
        (should-not= nil (some #(and (vector? %) (list? (first %))) clauses))))

    (it "includes the or clause for the :many attr"
      (should-not= [] (or-clauses {:adversary "Plead" :tags ["china" "apt"]})))

    (it ":tlp (scalar) and :name (fulltext) each produce their own clause type"
      (let [clauses (where {:tlp "white" :name "Plead"})
            ft-clauses (filter #(and (vector? %) (list? (first %))) clauses)
            eq-clauses (filter #(= (first %) '?doc) clauses)]
        (should= 1 (count ft-clauses))
        (should= 2 (count eq-clauses))))))
