(ns com.pringwa.service.integration-spec
  "End-to-end integration specs that exercise the full request stack against a
  real Datomic Local :mem database — no mocks, no stubs.

  These complement the unit specs by verifying that handlers, the query DSL,
  fulltext indexing, and the cache all work together correctly with real data.

  Note: persistence internal namespaces (schema, cache) are imported here for
  test-setup purposes only — an accepted Polylith exception for test infrastructure."
  (:require [speclj.core :refer [after-all before before-all describe it should-contain should-not-contain should-not= should=]]
            [com.pringwa.persistence.cache :as cache]
            [com.pringwa.persistence.schema :as schema]
            [com.pringwa.service.handler.filter-indicators :as filter-indicators]
            [com.pringwa.service.handler.indicator :as indicator]
            [com.pringwa.service.handler.indicators :as indicators]
            [datomic.client.api :as d]))

;; ---------------------------------------------------------------------------
;; Fixtures
;; ---------------------------------------------------------------------------

(def ^:private fixture-docs
  [{:document/id                 "doc-plead"
    :document/name               "Plead Downloader Campaign"
    :document/adversary          "Plead"
    :document/tlp                "white"
    :document/author_name        "AlienVault"
    :document/description        "APT targeting Japan using Plead malware"
    :document/tags               ["china" "apt" "japan"]
    :document/industries         ["government" "defense"]
    :document/targeted_countries ["Japan" "Taiwan"]
    :document/indicators         [{:indicator/indicator "1.2.3.4"
                                   :indicator/type      "IPv4"
                                   :indicator/id        1}
                                  {:indicator/indicator "malware.example.com"
                                   :indicator/type      "domain"
                                   :indicator/id        2}]}
   {:document/id                 "doc-apt28"
    :document/name               "APT28 Credential Phishing"
    :document/adversary          "APT28"
    :document/tlp                "green"
    :document/author_name        "Mandiant"
    :document/description        "Russian APT credential harvesting operation"
    :document/tags               ["russia" "apt" "phishing"]
    :document/industries         ["finance" "energy"]
    :document/targeted_countries ["Ukraine" "Germany"]
    :document/indicators         [{:indicator/indicator "5.6.7.8"
                                   :indicator/type      "IPv4"
                                   :indicator/id        3}]}
   {:document/id                 "doc-lazarus"
    :document/name               "Lazarus Crypto Theft"
    :document/adversary          "Lazarus"
    :document/tlp                "white"
    :document/author_name        "CrowdStrike"
    :document/description        "North Korean APT targeting crypto exchanges"
    :document/tags               ["north-korea" "crypto"]
    :document/industries         ["finance" "crypto"]
    :document/targeted_countries ["South Korea" "Japan"]
    :document/indicators         [{:indicator/indicator "https://steal.example.com"
                                   :indicator/type      "URL"
                                   :indicator/id        4}]}])

;; ---------------------------------------------------------------------------
;; Setup
;; ---------------------------------------------------------------------------

(defn- new-conn! []
  (let [client  (d/client {:server-type :datomic-local
                           :storage-dir :mem
                           :system      "integration-test"})
        db-name "integration-indicators"]
    (d/create-database client {:db-name db-name})
    (let [conn (d/connect client {:db-name db-name})]
      (schema/install-schema! conn)
      (d/transact conn {:tx-data fixture-docs})
      conn)))

(defn- req [conn & {:as opts}]
  (merge {:conn conn} opts))

;; ---------------------------------------------------------------------------
;; Shared state
;; ---------------------------------------------------------------------------

(def ^:private !conn (atom nil))

;; ---------------------------------------------------------------------------
;; Specs
;; ---------------------------------------------------------------------------

(describe "integration — handler layer"
  (before-all (reset! !conn (new-conn!)))
  (before     (cache/clear!))
  (after-all  (reset! !conn nil))

  ;; -------------------------------------------------------------------------
  ;; GET /v1/indicators
  ;; -------------------------------------------------------------------------

  (describe "GET /v1/indicators — all documents"
    (it "returns HTTP 200"
      (should= 200 (:status (indicators/handler (req @!conn :query-params {})))))

    (it "returns all 3 fixture documents"
      (should= 3 (-> (indicators/handler (req @!conn :query-params {}))
                     :body :results count)))

    (it "strips the :document/ namespace from result keys"
      (let [doc (-> (indicators/handler (req @!conn :query-params {}))
                    :body :results first)]
        (should-contain :id doc)
        (should-not-contain :document/id doc))))

  ;; -------------------------------------------------------------------------
  ;; GET /v1/indicators?type=IPv4
  ;; -------------------------------------------------------------------------

  (describe "GET /v1/indicators?type= — filter by indicator type"
    (it "returns only documents with IPv4 indicators"
      (should= 2 (-> (indicators/handler (req @!conn :query-params {"type" "IPv4"}))
                     :body :results count)))

    (it "returns only documents with URL indicators"
      (should= 1 (-> (indicators/handler (req @!conn :query-params {"type" "URL"}))
                     :body :results count)))

    (it "returns empty for an absent indicator type"
      (should= [] (-> (indicators/handler (req @!conn :query-params {"type" "FileHash-MD5"}))
                      :body :results))))

  ;; -------------------------------------------------------------------------
  ;; GET /v1/indicators/:id
  ;; -------------------------------------------------------------------------

  (describe "GET /v1/indicators/:id — single document"
    (it "returns HTTP 200 for an existing id"
      (should= 200 (:status (indicator/handler (req @!conn :path-params {:id "doc-plead"})))))

    (it "returns the correct document"
      (let [result (-> (indicator/handler (req @!conn :path-params {:id "doc-plead"}))
                       :body :result)]
        (should= "doc-plead" (:id result))
        (should= "Plead"     (:adversary result))))

    (it "returns HTTP 404 for a missing id"
      (should= 404 (:status (indicator/handler (req @!conn :path-params {:id "does-not-exist"})))))

    (it "returns :error in the body for a missing id"
      (should-contain :error (:body (indicator/handler (req @!conn :path-params {:id "does-not-exist"}))))))

  ;; -------------------------------------------------------------------------
  ;; POST /v1/indicators/search — query DSL end-to-end
  ;; -------------------------------------------------------------------------

  (describe "POST /v1/indicators/search — validation"
    (it "returns 400 for empty criteria"
      (should= 400 (:status (filter-indicators/handler (req @!conn :body-params {})))))

    (it "returns 400 for unknown criteria keys"
      (should= 400 (:status (filter-indicators/handler (req @!conn :body-params {:adversarry "typo"}))))))

  (describe "POST /v1/indicators/search — text attribute search"
    (it "finds a document by adversary (exact match)"
      (let [results (-> (filter-indicators/handler (req @!conn :body-params {:adversary "Plead"}))
                        :body :results)]
        (should= 1 (count results))
        (should= "doc-plead" (:id (first results)))))

    (it "finds a document by name (exact match)"
      (let [results (-> (filter-indicators/handler (req @!conn :body-params {:name "Lazarus Crypto Theft"}))
                        :body :results)]
        (should= 1 (count results))
        (should= "doc-lazarus" (:id (first results)))))

    (it "finds a document by author_name (exact match)"
      (let [results (-> (filter-indicators/handler (req @!conn :body-params {:author_name "Mandiant"}))
                        :body :results)]
        (should= 1 (count results))
        (should= "doc-apt28" (:id (first results))))))

  (describe "POST /v1/indicators/search — scalar equality"
    (it "returns all documents with tlp=white"
      (should= 2 (-> (filter-indicators/handler (req @!conn :body-params {:tlp "white"}))
                     :body :results count)))

    (it "returns the single document with tlp=green"
      (let [results (-> (filter-indicators/handler (req @!conn :body-params {:tlp "green"}))
                        :body :results)]
        (should= 1 (count results))
        (should= "doc-apt28" (:id (first results))))))

  (describe "POST /v1/indicators/search — many-cardinality (set membership)"
    (it "matches documents that contain a single tag"
      (should= 2 (-> (filter-indicators/handler (req @!conn :body-params {:tags "apt"}))
                     :body :results count)))

    (it "matches documents via OR across a tag array"
      (should= 3 (-> (filter-indicators/handler (req @!conn :body-params {:tags ["apt" "crypto"]}))
                     :body :results count)))

    (it "matches documents by targeted_countries"
      (should= 2 (-> (filter-indicators/handler (req @!conn :body-params {:targeted_countries "Japan"}))
                     :body :results count)))

    (it "matches documents by industries array (OR)"
      (should= 2 (-> (filter-indicators/handler (req @!conn :body-params {:industries ["government" "crypto"]}))
                     :body :results count))))

  (describe "POST /v1/indicators/search — AND logic across multiple criteria"
    (it "intersects tlp=white with tag=china → one doc"
      (let [results (-> (filter-indicators/handler (req @!conn :body-params {:tlp "white" :tags "china"}))
                        :body :results)]
        (should= 1 (count results))
        (should= "doc-plead" (:id (first results)))))

    (it "returns empty when AND criteria match no document"
      (should= [] (-> (filter-indicators/handler (req @!conn :body-params {:tlp "green" :tags "china"}))
                      :body :results))))

  (describe "cache — end-to-end consistency"
    (it "repeated search returns identical results"
      (let [search! #(-> (filter-indicators/handler (req @!conn :body-params {:tlp "white"}))
                         :body :results)]
        (should= (search!) (search!))))

    (it "different criteria return different result sets"
      (let [white (-> (filter-indicators/handler (req @!conn :body-params {:tlp "white"}))
                      :body :results count)
            green (-> (filter-indicators/handler (req @!conn :body-params {:tlp "green"}))
                      :body :results count)]
        (should-not= white green)))))
