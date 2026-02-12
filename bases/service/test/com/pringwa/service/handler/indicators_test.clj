(ns com.pringwa.service.handler.indicators-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.pringwa.service.handler.indicators :as indicators]
            [com.pringwa.persistence.interface :as store]
            [datomic.client.api :as d]))

(def mock-documents
  [{:document/id "doc-1"
    :document/name "Test Document 1"
    :document/adversary "Plead"
    :document/tlp "white"
    :document/indicators [{:indicator/indicator "192.168.1.1"
                           :indicator/type "IPv4"
                           :indicator/id 1}]}
   {:document/id "doc-2"
    :document/name "Test Document 2"
    :document/adversary "APT28"
    :document/tlp "green"
    :document/indicators [{:indicator/indicator "malware.com"
                           :indicator/type "hostname"
                           :indicator/id 2}]}])

(def transformed-documents
  [{:id "doc-1"
    :name "Test Document 1"
    :adversary "Plead"
    :tlp "white"
    :indicators [{:indicator "192.168.1.1"
                  :type "IPv4"
                  :id 1}]}
   {:id "doc-2"
    :name "Test Document 2"
    :adversary "APT28"
    :tlp "green"
    :indicators [{:indicator "malware.com"
                  :type "hostname"
                  :id 2}]}])

(deftest get-all-indicators-test
         (testing "returns all documents with status 200"
                  (with-redefs [store/init-db (constantly {:conn :mock-conn})
                                d/db (constantly :mock-db)
                                store/find-all-documents (constantly mock-documents)
                                store/transform-keys (constantly transformed-documents)]
                    (let [request {:query-params {}}
                          response (indicators/handler request)]
                      (is (= 200 (:status response)))
                      (is (contains? (:body response) :result)))))

         (testing "returns empty result when no documents exist"
                  (with-redefs [store/init-db (constantly {:conn :mock-conn})
                                d/db (constantly :mock-db)
                                store/find-all-documents (constantly [])
                                store/transform-keys (constantly [])]
                    (let [request {:query-params {}}
                          response (indicators/handler request)]
                      (is (= 200 (:status response)))
                      (is (= {:result []} (:body response))))))

         (testing "calls find-all-documents when type is empty string"
                  (let [find-all-called? (atom false)]
                    (with-redefs [store/init-db (constantly {:conn :mock-conn})
                                  d/db (constantly :mock-db)
                                  store/find-all-documents (fn [_]
                                                             (reset! find-all-called? true)
                                                             mock-documents)
                                  store/transform-keys (constantly transformed-documents)]
                      (let [request {:query-params {"type" ""}}
                            response (indicators/handler request)]
                        (is (= 200 (:status response)))
                        (is @find-all-called?))))))