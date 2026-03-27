(ns com.pringwa.persistence.util-spec
  (:require [speclj.core :refer :all]
            [clojure.java.io :as io]
            [com.pringwa.persistence.schema :as schema]
            [com.pringwa.persistence.util :as util]
            [datomic.client.api :as d])
  (:import (java.util Date)))

;; Private helpers under test
(def ^:private parse-date      #'util/parse-date)
(def ^:private map->indicator  #'util/map->indicator-tx)
(def ^:private map->document   #'util/map->document-tx)

;; ---------------------------------------------------------------------------
;; parse-date
;; ---------------------------------------------------------------------------

(describe "parse-date"
  (it "parses a valid ISO date string"
    (should (instance? Date (parse-date "2018-07-09T10:48:47Z"))))

  (it "returns nil for nil"
    (should-be-nil (parse-date nil)))

  (it "returns nil for an empty string"
    (should-be-nil (parse-date "")))

  (it "returns nil for a non-date string"
    (should-be-nil (parse-date "not-a-date")))

  (it "returns nil for a malformed date"
    (should-be-nil (parse-date "2018-99-99"))))

;; ---------------------------------------------------------------------------
;; map->indicator-tx
;; ---------------------------------------------------------------------------

(describe "map->indicator-tx"
  (it "converts a complete indicator map"
    (let [result (map->indicator {"indicator"   "192.168.1.1"
                                  "id"          12345
                                  "type"        "IPv4"
                                  "created"     "2018-07-09T10:48:48Z"
                                  "description" "Test description"
                                  "title"       "Test title"
                                  "content"     "Test content"})]
      (should= "192.168.1.1" (:indicator/indicator result))
      (should= 12345         (:indicator/id result))
      (should= "IPv4"        (:indicator/type result))
      (should (instance? Date (:indicator/created result)))
      (should= "Test description" (:indicator/description result))
      (should= "Test title"       (:indicator/title result))
      (should= "Test content"     (:indicator/content result))))

  (it "converts a minimal indicator map"
    (let [result (map->indicator {"indicator" "malware.com"
                                  "id"        1
                                  "type"      "hostname"
                                  "created"   "2018-07-09T10:48:48Z"})]
      (should= "malware.com" (:indicator/indicator result))
      (should= 1             (:indicator/id result))
      (should= "hostname"    (:indicator/type result))
      (should-not-contain :indicator/description result)
      (should-not-contain :indicator/title result)
      (should-not-contain :indicator/content result)))

  (it "handles nil created date"
    (let [result (map->indicator {"indicator" "test.com"
                                  "id"        1
                                  "type"      "hostname"
                                  "created"   nil})]
      (should-be-nil (:indicator/created result))))

  (it "excludes optional fields when nil"
    (let [result (map->indicator {"indicator"   "test.com"
                                  "id"          1
                                  "type"        "hostname"
                                  "created"     "2018-07-09T10:48:48Z"
                                  "description" nil
                                  "title"       nil
                                  "content"     nil})]
      (should-not-contain :indicator/description result)
      (should-not-contain :indicator/title result)
      (should-not-contain :indicator/content result))))

;; ---------------------------------------------------------------------------
;; map->document-tx
;; ---------------------------------------------------------------------------

(describe "map->document-tx"
  (it "converts a complete document map"
    (let [result (map->document {"id"                 "doc-123"
                                 "name"               "Test Document"
                                 "tlp"                "white"
                                 "author_name"        "AlienVault"
                                 "description"        "Test description"
                                 "created"            "2018-07-09T10:48:47Z"
                                 "modified"           "2018-07-09T10:48:47Z"
                                 "revision"           1
                                 "public"             1
                                 "adversary"          "Plead"
                                 "more_indicators"    false
                                 "tags"               ["china" "apt"]
                                 "industries"         ["tech"]
                                 "references"         ["https://example.com"]
                                 "extract_source"     "source"
                                 "targeted_countries" ["TW"]
                                 "indicators"         [{"indicator" "192.168.1.1"
                                                        "id"        1
                                                        "type"      "IPv4"
                                                        "created"   "2018-07-09T10:48:48Z"}]})]
      (should= "doc-123"          (:document/id result))
      (should= "Test Document"    (:document/name result))
      (should= "white"            (:document/tlp result))
      (should= "AlienVault"       (:document/author_name result))
      (should= "Test description" (:document/description result))
      (should= "Plead"            (:document/adversary result))
      (should= 1                  (:document/revision result))
      (should= 1                  (:document/public result))
      (should= ["china" "apt"]    (:document/tags result))
      (should= ["tech"]           (:document/industries result))
      (should (instance? Date (:document/created result)))
      (should (instance? Date (:document/modified result)))
      (should= 1 (count (:document/indicators result)))))

  (it "excludes nil values from result"
    (let [result (map->document {"id"          "doc-123"
                                 "name"        "Test Document"
                                 "tlp"         "white"
                                 "description" nil
                                 "adversary"   nil
                                 "tags"        nil
                                 "indicators"  []})]
      (should= "doc-123" (:document/id result))
      (should-not-contain :document/description result)
      (should-not-contain :document/adversary result)
      (should-not-contain :document/tags result)))

  (it "converts public and revision to integers"
    (let [result (map->document {"id"         "doc-123"
                                 "name"       "Test"
                                 "public"     1
                                 "revision"   2
                                 "indicators" []})]
      (should= 1 (:document/public result))
      (should= 2 (:document/revision result))
      (should (integer? (:document/public result)))
      (should (integer? (:document/revision result)))))

  (it "handles empty indicators list"
    (let [result (map->document {"id"         "doc-123"
                                 "name"       "Test"
                                 "indicators" []})]
      (should= [] (:document/indicators result))))

  (it "converts multiple indicators"
    (let [result (map->document {"id"         "doc-123"
                                 "name"       "Test"
                                 "indicators" [{"indicator" "192.168.1.1"
                                                "id"        1
                                                "type"      "IPv4"
                                                "created"   "2018-07-09T10:48:48Z"}
                                               {"indicator" "malware.com"
                                                "id"        2
                                                "type"      "hostname"
                                                "created"   "2018-07-09T10:48:48Z"}]})]
      (should= 2 (count (:document/indicators result))))))

;; ---------------------------------------------------------------------------
;; transform-keys
;; ---------------------------------------------------------------------------

(describe "transform-keys"
  (it "removes namespace prefix from keyword keys"
    (should= {:id "123" :name "Test"}
             (util/transform-keys {:document/id "123" :document/name "Test"})))

  (it "handles nested maps"
    (should= {:id "123" :data {:key "value"}}
             (util/transform-keys {:document/id   "123"
                                   :document/data {:inner/key "value"}})))

  (it "handles vectors of maps"
    (should= {:indicators [{:type "IPv4" :id 1} {:type "hostname" :id 2}]}
             (util/transform-keys {:document/indicators [{:indicator/type "IPv4"  :indicator/id 1}
                                                         {:indicator/type "hostname" :indicator/id 2}]})))

  (it "handles deeply nested structures"
    (should= {:id "123" :indicators [{:type "IPv4" :meta {:key "value"}}]}
             (util/transform-keys {:document/id         "123"
                                   :document/indicators [{:indicator/type "IPv4"
                                                          :indicator/meta {:meta/key "value"}}]})))

  (it "preserves non-namespaced string keys"
    (let [result (util/transform-keys {"string-key" "value" :document/id "123"})]
      (should= "value"   (get result "string-key"))
      (should= "123"     (:id result))))

  (it "handles an empty map"
    (should= {} (util/transform-keys {})))

  (it "handles an empty vector"
    (should= [] (util/transform-keys [])))

  (it "handles nil"
    (should-be-nil (util/transform-keys nil)))

  (it "handles a complete document structure"
    (let [result (util/transform-keys {:document/id         "5b433d8fe822e72e3c57d26c"
                                       :document/name       "Test Document"
                                       :document/adversary  "Plead"
                                       :document/tlp        "white"
                                       :document/tags       ["china" "apt"]
                                       :document/indicators [{:indicator/indicator "192.168.1.1"
                                                              :indicator/type      "IPv4"
                                                              :indicator/id        1}]})]
      (should= "5b433d8fe822e72e3c57d26c" (:id result))
      (should= "Test Document"            (:name result))
      (should= "Plead"                    (:adversary result))
      (should= "white"                    (:tlp result))
      (should= ["china" "apt"]            (:tags result))
      (should= 1                          (count (:indicators result)))
      (should= "192.168.1.1"              (-> result :indicators first :indicator))
      (should= "IPv4"                     (-> result :indicators first :type)))))

;; ---------------------------------------------------------------------------
;; matches?
;; ---------------------------------------------------------------------------

(describe "matches?"
  (let [doc {:document/id          "doc-123"
             :document/name        "Test Document"
             :document/adversary   "Plead"
             :document/tlp         "white"
             :document/author_name "AlienVault"
             :document/tags        ["china" "apt" "blacktech"]
             :document/industries  ["tech" "finance"]}]

    (it "matches a single scalar criterion"
      (should (util/matches? doc {:adversary "Plead"}))
      (should (util/matches? doc {:tlp "white"}))
      (should (util/matches? doc {:author_name "AlienVault"})))

    (it "does not match a wrong value"
      (should-not (util/matches? doc {:adversary "APT28"}))
      (should-not (util/matches? doc {:tlp "red"})))

    (it "ANDs multiple criteria"
      (should (util/matches? doc {:adversary "Plead" :tlp "white"}))
      (should (util/matches? doc {:adversary "Plead" :tlp "white" :author_name "AlienVault"})))

    (it "fails when any criterion does not match"
      (should-not (util/matches? doc {:adversary "Plead" :tlp "red"}))
      (should-not (util/matches? doc {:adversary "Wrong" :tlp "white"})))

    (it "does not match a value absent from a vector attribute"
      (should-not (util/matches? doc {:tags "russia"}))
      (should-not (util/matches? doc {:industries "healthcare"})))

    (it "matches by exact id"
      (should     (util/matches? doc {:id "doc-123"}))
      (should-not (util/matches? doc {:id "wrong-id"})))

    (it "matches by exact name"
      (should     (util/matches? doc {:name "Test Document"}))
      (should-not (util/matches? doc {:name "Wrong Name"})))))

;; ---------------------------------------------------------------------------
;; init-db
;; ---------------------------------------------------------------------------

(describe "init-db"
  (it "creates client, connects, installs schema, and returns result map"
    (let [captured-client (atom nil)
          captured-db     (atom nil)
          schema-called   (atom false)]
      (with-redefs [util/create-client   (fn [cfg] (reset! captured-client cfg) :mock-client)
                    util/create-database (fn [c n]  (reset! captured-db [c n]) :mock-conn)
                    schema/install-schema! (fn [conn] (reset! schema-called conn))]
        (let [result (util/init-db {:client   {:server-type :datomic-local
                                               :storage-dir :mem
                                               :system      "indicators"}
                                    :database {:db-name "test-db"}})]
          (should= {:server-type :datomic-local :storage-dir :mem :system "indicators"}
                   @captured-client)
          (should= [:mock-client "test-db"] @captured-db)
          (should= :mock-conn @schema-called)
          (should= {:client :mock-client :conn :mock-conn :db-name "test-db"} result))))))

;; ---------------------------------------------------------------------------
;; create-client
;; ---------------------------------------------------------------------------

(describe "create-client"
  (it "delegates to datomic client with the supplied config"
    (let [captured (atom nil)]
      (with-redefs [d/client (fn [cfg] (reset! captured cfg) :mock-client)]
        (let [config {:server-type :datomic-local :storage-dir :mem :system "indicators"}]
          (should= :mock-client (util/create-client config))
          (should= config @captured))))))

;; ---------------------------------------------------------------------------
;; create-database
;; ---------------------------------------------------------------------------

(describe "create-database"
  (it "creates and connects to the named database"
    (let [created   (atom nil)
          connected (atom nil)]
      (with-redefs [d/create-database (fn [c opts] (reset! created   [c opts]))
                    d/connect         (fn [c opts] (reset! connected [c opts]) :mock-conn)]
        (should= :mock-conn (util/create-database :mock-client "test-db"))
        (should= [:mock-client {:db-name "test-db"}] @created)
        (should= [:mock-client {:db-name "test-db"}] @connected)))))

;; ---------------------------------------------------------------------------
;; slurp-data!
;; ---------------------------------------------------------------------------

(describe "slurp-data!"
  (it "returns nil and prints an error when the resource is not found"
    (with-redefs [io/resource (constantly nil)]
      (should-be-nil (util/slurp-data! :mock-conn "nonexistent.json" 10)))))
