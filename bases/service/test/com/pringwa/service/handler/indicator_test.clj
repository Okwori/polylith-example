(ns com.pringwa.service.handler.indicator-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.pringwa.service.handler.indicator :as indicator]
            [com.pringwa.persistence.interface :as store]
            [datomic.client.api :as d]))

(def mock-document
  {:document/id "5b433d8fe822e72e3c57d26c"
   :document/name "Test Document"
   :document/adversary "Plead"
   :document/tlp "white"
   :document/author_name "AlienVault"
   :document/description "Test description"
   :document/tags ["china" "apt"]
   :document/indicators [{:indicator/indicator "192.168.1.1"
                          :indicator/type "IPv4"
                          :indicator/id 1}]})

(def transformed-document
  {:id "5b433d8fe822e72e3c57d26c"
   :name "Test Document"
   :adversary "Plead"
   :tlp "white"
   :author_name "AlienVault"
   :description "Test description"
   :tags ["china" "apt"]
   :indicators [{:indicator "192.168.1.1"
                 :type "IPv4"
                 :id 1}]})

(deftest get-indicator-by-id-test
  (testing "returns document with status 200"
    (with-redefs [d/db (constantly :mock-db)
                  store/find-document (constantly mock-document)
                  store/transform-keys (constantly transformed-document)]
      (let [request  {:conn :mock-conn :path-params {:id "5b433d8fe822e72e3c57d26c"}}
            response (indicator/handler request)]
        (is (= 200 (:status response))))))

  (testing "returns correct document structure"
    (with-redefs [d/db (constantly :mock-db)
                  store/find-document (constantly mock-document)
                  store/transform-keys (constantly transformed-document)]
      (let [request  {:conn :mock-conn :path-params {:id "5b433d8fe822e72e3c57d26c"}}
            response (indicator/handler request)
            result   (-> response :body :result)]
        (is (= "5b433d8fe822e72e3c57d26c" (:id result)))
        (is (= "Test Document" (:name result)))
        (is (= "Plead" (:adversary result)))
        (is (= "white" (:tlp result))))))

  (testing "includes indicators in response"
    (with-redefs [d/db (constantly :mock-db)
                  store/find-document (constantly mock-document)
                  store/transform-keys (constantly transformed-document)]
      (let [request    {:conn :mock-conn :path-params {:id "5b433d8fe822e72e3c57d26c"}}
            response   (indicator/handler request)
            indicators (-> response :body :result :indicators)]
        (is (= 1 (count indicators)))
        (is (= "192.168.1.1" (-> indicators first :indicator)))
        (is (= "IPv4" (-> indicators first :type)))))))

(deftest get-nonexistent-document-test
  (testing "returns 404 when document not found"
    (with-redefs [d/db (constantly :mock-db)
                  store/find-document (constantly nil)
                  store/transform-keys (constantly nil)]
      (let [request  {:conn :mock-conn :path-params {:id "nonexistent-id"}}
            response (indicator/handler request)]
        (is (= 404 (:status response))))))

  (testing "returns error message when document not found"
    (with-redefs [d/db (constantly :mock-db)
                  store/find-document (constantly nil)
                  store/transform-keys (constantly nil)]
      (let [request  {:conn :mock-conn :path-params {:id "nonexistent-id"}}
            response (indicator/handler request)]
        (is (contains? (:body response) :error))))))

(deftest id-extraction-test
  (testing "extracts ID from path-params"
    (let [passed-id (atom nil)]
      (with-redefs [d/db (constantly :mock-db)
                    store/find-document (fn [_ id]
                                          (reset! passed-id id)
                                          mock-document)
                    store/transform-keys (constantly transformed-document)]
        (indicator/handler {:conn :mock-conn :path-params {:id "abc123"}})
        (is (= "abc123" @passed-id)))))

  (testing "handles IDs with hex characters"
    (let [passed-id (atom nil)]
      (with-redefs [d/db (constantly :mock-db)
                    store/find-document (fn [_ id]
                                          (reset! passed-id id)
                                          mock-document)
                    store/transform-keys (constantly transformed-document)]
        (indicator/handler {:conn :mock-conn :path-params {:id "5b433d8fe822e72e3c57d26c"}})
        (is (= "5b433d8fe822e72e3c57d26c" @passed-id))))))
