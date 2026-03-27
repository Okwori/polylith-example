(ns com.pringwa.service.handler.filter-indicators-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.pringwa.service.handler.filter-indicators :as filter-indicators]
            [com.pringwa.persistence.interface :as store]
            [datomic.client.api :as d]))

(def mock-documents
  [{:document/id "doc-1"
    :document/name "Test Document 1"
    :document/adversary "Plead"
    :document/tlp "white"
    :document/author_name "AlienVault"
    :document/tags ["china" "apt"]}
   {:document/id "doc-2"
    :document/name "Test Document 2"
    :document/adversary "APT28"
    :document/tlp "green"
    :document/author_name "Mandiant"
    :document/tags ["russia" "apt"]}])

(def transformed-documents
  [{:id "doc-1"
    :name "Test Document 1"
    :adversary "Plead"
    :tlp "white"
    :author_name "AlienVault"
    :tags ["china" "apt"]}
   {:id "doc-2"
    :name "Test Document 2"
    :adversary "APT28"
    :tlp "green"
    :author_name "Mandiant"
    :tags ["russia" "apt"]}])

;; Empty body cases
(deftest empty-criteria-test
  (testing "returns 400 error when body is empty"
    (with-redefs [d/db (constantly :mock-db)]
      (let [request {:conn :mock-conn
                     :body-params {}}
            response (filter-indicators/handler request)]
        (is (= 400 (:status response)))
        (is (contains? (:body response) :error)))))

  (testing "returns 400 error when body is nil"
    (with-redefs [d/db (constantly :mock-db)]
      (let [request {:conn :mock-conn
                     :body-params nil}
            response (filter-indicators/handler request)]
        (is (= 400 (:status response)))))))

(deftest unknown-criteria-test
  (testing "returns 400 for unknown criteria key"
    (with-redefs [d/db (constantly :mock-db)]
      (let [request {:conn :mock-conn
                     :body-params {:adversarry "typo"}}
            response (filter-indicators/handler request)]
        (is (= 400 (:status response))))))

  (testing "returns :error for unknown criteria key"
    (with-redefs [d/db (constantly :mock-db)]
      (let [request {:conn :mock-conn
                     :body-params {:unknown-field "value"}}
            response (filter-indicators/handler request)]
        (is (contains? (:body response) :error))))))

;; Search tests
(deftest search-by-adversary-test
  (testing "returns matching documents"
    (with-redefs [d/db (constantly :mock-db)
                  store/find-all-documents (constantly mock-documents)
                  store/transform-keys (constantly [(first transformed-documents)])]
      (let [request {:conn :mock-conn
                     :body-params {:adversary "Plead"}}
            response (filter-indicators/handler request)]
        (is (= 200 (:status response)))
        (is (contains? (:body response) :results)))))

  (testing "returns empty results when no match"
    (with-redefs [d/db (constantly :mock-db)
                  store/find-all-documents (constantly mock-documents)
                  store/transform-keys (constantly [])]
      (let [request {:conn :mock-conn
                     :body-params {:adversary "NonExistent"}}
            response (filter-indicators/handler request)]
        (is (= 200 (:status response)))
        (is (= [] (-> response :body :results)))))))

(deftest search-by-tlp-test
  (testing "returns documents with matching tlp"
    (with-redefs [d/db (constantly :mock-db)
                  store/find-all-documents (constantly mock-documents)
                  store/transform-keys (constantly [(first transformed-documents)])]
      (let [request {:conn :mock-conn
                     :body-params {:tlp "white"}}
            response (filter-indicators/handler request)]
        (is (= 200 (:status response)))
        (is (= 1 (count (-> response :body :results))))))))

(deftest search-by-author-test
  (testing "returns documents with matching author"
    (with-redefs [d/db (constantly :mock-db)
                  store/find-all-documents (constantly mock-documents)
                  store/transform-keys (constantly [(first transformed-documents)])]
      (let [request {:conn :mock-conn
                     :body-params {:author_name "AlienVault"}}
            response (filter-indicators/handler request)]
        (is (= 200 (:status response)))))))

(deftest search-by-tags-test
  (testing "returns documents containing tag"
    (with-redefs [d/db (constantly :mock-db)
                  store/find-all-documents (constantly mock-documents)
                  store/transform-keys (constantly transformed-documents)]
      (let [request {:conn :mock-conn
                     :body-params {:tags "apt"}}
            response (filter-indicators/handler request)]
        (is (= 200 (:status response)))))))

(deftest search-by-multiple-criteria-test
  (testing "returns documents matching all criteria"
    (with-redefs [d/db (constantly :mock-db)
                  store/find-all-documents (constantly mock-documents)
                  store/transform-keys (constantly [(first transformed-documents)])]
      (let [request {:conn :mock-conn
                     :body-params {:adversary "Plead"
                                   :tlp "white"}}
            response (filter-indicators/handler request)]
        (is (= 200 (:status response))))))

  (testing "returns empty when criteria don't all match"
    (with-redefs [d/db (constantly :mock-db)
                  store/find-all-documents (constantly mock-documents)
                  store/transform-keys (constantly [])]
      (let [request {:conn :mock-conn
                     :body-params {:adversary "Plead"
                                   :tlp "green"}}
            response (filter-indicators/handler request)]
        (is (= 200 (:status response)))
        (is (= [] (-> response :body :results)))))))

(deftest response-structure-test
  (testing "returns body with :results key"
    (with-redefs [d/db (constantly :mock-db)
                  store/find-all-documents (constantly mock-documents)
                  store/transform-keys (constantly transformed-documents)]
      (let [request {:conn :mock-conn
                     :body-params {:adversary "Plead"}}
            response (filter-indicators/handler request)]
        (is (contains? (:body response) :results))))))
