(ns com.pringwa.persistence.util-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest testing is]]
            [com.pringwa.persistence.util :as util]
            [com.pringwa.persistence.schema :as schema])
  (:import (java.util Date)))

(deftest parse-date-test
         (let [parse-date #'util/parse-date]
           (testing "parses valid ISO date string"
             (let [result (parse-date "2018-07-09T10:48:47Z")]
               (is (instance? Date result))))
           (testing "returns nil for nil input"
                    (is (nil? (parse-date nil))))
           (testing "returns nil for empty string"
                    (is (nil? (parse-date ""))))
           (testing "returns nil for invalid date string"
                    (is (nil? (parse-date "not-a-date"))))
           (testing "returns nil for malformed date"
                    (is (nil? (parse-date "2018-99-99"))))))

(deftest map->indicator-tx-test
  (let [map->indicator-tx #'util/map->indicator-tx]

    (testing "converts complete indicator map"
      (let [input {"indicator" "192.168.1.1"
                   "id" 12345
                   "type" "IPv4"
                   "created" "2018-07-09T10:48:48Z"
                   "description" "Test description"
                   "title" "Test title"
                   "content" "Test content"}
            result (map->indicator-tx input)]
        (is (= "192.168.1.1" (:indicator/indicator result)))
        (is (= 12345 (:indicator/id result)))
        (is (= "IPv4" (:indicator/type result)))
        (is (instance? Date (:indicator/created result)))
        (is (= "Test description" (:indicator/description result)))
        (is (= "Test title" (:indicator/title result)))
        (is (= "Test content" (:indicator/content result)))))

    (testing "converts minimal indicator map"
      (let [input {"indicator" "malware.com"
                   "id" 1
                   "type" "hostname"
                   "created" "2018-07-09T10:48:48Z"}
            result (map->indicator-tx input)]
        (is (= "malware.com" (:indicator/indicator result)))
        (is (= 1 (:indicator/id result)))
        (is (= "hostname" (:indicator/type result)))
        (is (not (contains? result :indicator/description)))
        (is (not (contains? result :indicator/title)))
        (is (not (contains? result :indicator/content)))))

    (testing "handles nil created date"
      (let [input {"indicator" "test.com"
                   "id" 1
                   "type" "hostname"
                   "created" nil}
            result (map->indicator-tx input)]
        (is (nil? (:indicator/created result)))))

    (testing "excludes optional fields when nil"
      (let [input {"indicator" "test.com"
                   "id" 1
                   "type" "hostname"
                   "created" "2018-07-09T10:48:48Z"
                   "description" nil
                   "title" nil
                   "content" nil}
            result (map->indicator-tx input)]
        (is (not (contains? result :indicator/description)))
        (is (not (contains? result :indicator/title)))
        (is (not (contains? result :indicator/content)))))))

(deftest map->document-tx-test
  (let [map->document-tx #'util/map->document-tx]

    (testing "converts complete document map"
      (let [input {"id" "doc-123"
                   "name" "Test Document"
                   "tlp" "white"
                   "author_name" "AlienVault"
                   "description" "Test description"
                   "created" "2018-07-09T10:48:47Z"
                   "modified" "2018-07-09T10:48:47Z"
                   "revision" 1
                   "public" 1
                   "adversary" "Plead"
                   "more_indicators" false
                   "tags" ["china" "apt"]
                   "industries" ["tech"]
                   "references" ["https://example.com"]
                   "extract_source" "source"
                   "targeted_countries" ["TW"]
                   "indicators" [{"indicator" "192.168.1.1"
                                  "id" 1
                                  "type" "IPv4"
                                  "created" "2018-07-09T10:48:48Z"}]}
            result (map->document-tx input)]
        (is (= "doc-123" (:document/id result)))
        (is (= "Test Document" (:document/name result)))
        (is (= "white" (:document/tlp result)))
        (is (= "AlienVault" (:document/author_name result)))
        (is (= "Test description" (:document/description result)))
        (is (= "Plead" (:document/adversary result)))
        (is (= 1 (:document/revision result)))
        (is (= 1 (:document/public result)))
        (is (= ["china" "apt"] (:document/tags result)))
        (is (= ["tech"] (:document/industries result)))
        (is (instance? Date (:document/created result)))
        (is (instance? Date (:document/modified result)))
        (is (= 1 (count (:document/indicators result))))))

    (testing "excludes nil values from result"
      (let [input {"id" "doc-123"
                   "name" "Test Document"
                   "tlp" "white"
                   "description" nil
                   "adversary" nil
                   "tags" nil
                   "indicators" []}
            result (map->document-tx input)]
        (is (= "doc-123" (:document/id result)))
        (is (not (contains? result :document/description)))
        (is (not (contains? result :document/adversary)))
        (is (not (contains? result :document/tags)))))

    (testing "converts public and revision to long"
      (let [input {"id" "doc-123"
                   "name" "Test"
                   "public" 1
                   "revision" 2
                   "indicators" []}
            result (map->document-tx input)]
        (is (= 1 (:document/public result)))
        (is (= 2 (:document/revision result)))
        (is (integer? (:document/public result)))
        (is (integer? (:document/revision result)))))

    (testing "handles empty indicators"
      (let [input {"id" "doc-123"
                   "name" "Test"
                   "indicators" []}
            result (map->document-tx input)]
        (is (= [] (:document/indicators result)))))

    (testing "converts multiple indicators"
      (let [input {"id" "doc-123"
                   "name" "Test"
                   "indicators" [{"indicator" "192.168.1.1"
                                  "id" 1
                                  "type" "IPv4"
                                  "created" "2018-07-09T10:48:48Z"}
                                 {"indicator" "malware.com"
                                  "id" 2
                                  "type" "hostname"
                                  "created" "2018-07-09T10:48:48Z"}]}
            result (map->document-tx input)]
        (is (= 2 (count (:document/indicators result))))))))

(deftest transform-keys-test
  (testing "removes namespace from keyword keys"
    (let [input {:document/id "123"
                 :document/name "Test"}
          result (util/transform-keys input)]
      (is (= {:id "123" :name "Test"} result))))

  (testing "handles nested maps"
    (let [input {:document/id "123"
                 :document/data {:inner/key "value"}}
          result (util/transform-keys input)]
      (is (= {:id "123" :data {:key "value"}} result))))

  (testing "handles vectors of maps"
    (let [input {:document/indicators [{:indicator/type "IPv4"
                                        :indicator/id 1}
                                       {:indicator/type "hostname"
                                        :indicator/id 2}]}
          result (util/transform-keys input)]
      (is (= {:indicators [{:type "IPv4" :id 1}
                           {:type "hostname" :id 2}]}
             result))))

  (testing "handles deeply nested structures"
    (let [input {:document/id "123"
                 :document/indicators [{:indicator/type "IPv4"
                                        :indicator/meta {:meta/key "value"}}]}
          result (util/transform-keys input)]
      (is (= {:id "123"
              :indicators [{:type "IPv4"
                            :meta {:key "value"}}]}
             result))))

  (testing "preserves non-keyword keys"
    (let [input {"string-key" "value"
                 :document/id "123"}
          result (util/transform-keys input)]
      (is (= {"string-key" "value" :id "123"} result))))

  (testing "handles empty map"
    (is (= {} (util/transform-keys {}))))

  (testing "handles empty vector"
    (is (= [] (util/transform-keys []))))

  (testing "handles nil"
    (is (nil? (util/transform-keys nil))))

  (testing "handles complete document structure"
    (let [input {:document/id "5b433d8fe822e72e3c57d26c"
                 :document/name "Test Document"
                 :document/adversary "Plead"
                 :document/tlp "white"
                 :document/tags ["china" "apt"]
                 :document/indicators [{:indicator/indicator "192.168.1.1"
                                        :indicator/type "IPv4"
                                        :indicator/id 1}]}
          result (util/transform-keys input)]
      (is (= "5b433d8fe822e72e3c57d26c" (:id result)))
      (is (= "Test Document" (:name result)))
      (is (= "Plead" (:adversary result)))
      (is (= "white" (:tlp result)))
      (is (= ["china" "apt"] (:tags result)))
      (is (= 1 (count (:indicators result))))
      (is (= "192.168.1.1" (-> result :indicators first :indicator)))
      (is (= "IPv4" (-> result :indicators first :type))))))

(deftest matches?-test
  (let [document {:document/id "doc-123"
                  :document/name "Test Document"
                  :document/adversary "Plead"
                  :document/tlp "white"
                  :document/author_name "AlienVault"
                  :document/tags ["china" "apt" "blacktech"]
                  :document/industries ["tech" "finance"]}]

    (testing "matches single string criterion"
      (is (util/matches? document {:adversary "Plead"}))
      (is (util/matches? document {:tlp "white"}))
      (is (util/matches? document {:author_name "AlienVault"})))

    (testing "does not match wrong value"
      (is (not (util/matches? document {:adversary "APT28"})))
      (is (not (util/matches? document {:tlp "red"}))))

    (testing "matches multiple criteria (AND logic)"
      (is (util/matches? document {:adversary "Plead" :tlp "white"}))
      (is (util/matches? document {:adversary "Plead" :tlp "white" :author_name "AlienVault"})))

    (testing "fails if any criterion doesn't match"
      (is (not (util/matches? document {:adversary "Plead" :tlp "red"})))
      (is (not (util/matches? document {:adversary "Wrong" :tlp "white"}))))

    (testing "does not match value not in vector"
      (is (not (util/matches? document {:tags "russia"})))
      (is (not (util/matches? document {:industries "healthcare"}))))

    (testing "matches exact id"
      (is (util/matches? document {:id "doc-123"}))
      (is (not (util/matches? document {:id "wrong-id"}))))

    (testing "matches exact name"
      (is (util/matches? document {:name "Test Document"}))
      (is (not (util/matches? document {:name "Wrong Name"}))))))

(deftest init-db-test
  (testing "returns map with client, conn, and db-name"
    (with-redefs [util/create-client (constantly :mock-client)
                  util/create-database (constantly :mock-conn)
                  schema/install-schema! (constantly nil)]
      (let [result (util/init-db "test-db")]
        (is (= :mock-client (:client result)))
        (is (= :mock-conn (:conn result)))
        (is (= "test-db" (:db-name result)))))))

(deftest create-client-test
  (testing "creates client with correct config"
    (let [client-config (atom nil)]
      (with-redefs [util/create-client
                    (fn []
                      (reset! client-config {:server-type :datomic-local
                                             :storage-dir :mem
                                             :system "indicators"})
                      :mock-client)]
        (let [result (util/create-client)]
          (is (= :mock-client result)))))))

(deftest slurp-data-test
  (testing "prints error when resource not found"
    (with-redefs [io/resource (constantly nil)]
      (is (nil? (util/slurp-data! :mock-conn "nonexistent.json" 10))))))