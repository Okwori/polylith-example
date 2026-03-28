(ns com.pringwa.service.handler.indicators-spec
  (:require [speclj.core :refer [describe it should should-contain should= with-stubs]]
            [com.pringwa.service.handler.indicators :as indicators]
            [com.pringwa.persistence.interface :as store]))

(def ^:private mock-documents
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

(def ^:private transformed-documents
  [{:id "doc-1"
    :name "Test Document 1"
    :adversary "Plead"
    :tlp "white"
    :indicators [{:indicator "192.168.1.1" :type "IPv4" :id 1}]}
   {:id "doc-2"
    :name "Test Document 2"
    :adversary "APT28"
    :tlp "green"
    :indicators [{:indicator "malware.com" :type "hostname" :id 2}]}])

(describe "GET /indicators"
  (with-stubs)

  (describe "without type filter"
    (it "returns HTTP 200"
      (with-redefs [store/find-all-documents (constantly mock-documents)
                    store/transform-keys (constantly transformed-documents)]
        (let [response (indicators/handler {:db :mock-db :access-policy nil :query-params {}})]
          (should= 200 (:status response)))))

    (it "returns body with :results key"
      (with-redefs [store/find-all-documents (constantly mock-documents)
                    store/transform-keys (constantly transformed-documents)]
        (let [response (indicators/handler {:db :mock-db :access-policy nil :query-params {}})]
          (should-contain :results (:body response)))))

    (it "returns empty result when no documents exist"
      (with-redefs [store/find-all-documents (constantly [])
                    store/transform-keys (constantly [])]
        (let [response (indicators/handler {:db :mock-db :access-policy nil :query-params {}})]
          (should= [] (-> response :body :results)))))

    (it "calls find-all-documents when type param is absent"
      (let [find-all-called? (atom false)]
        (with-redefs [store/find-all-documents (fn [_db _policy]
                                                 (reset! find-all-called? true)
                                                 mock-documents)
                      store/transform-keys (constantly transformed-documents)]
          (indicators/handler {:db :mock-db :access-policy nil :query-params {}})
          (should @find-all-called?))))

    (it "calls find-all-documents when type param is an empty string"
      (let [find-all-called? (atom false)]
        (with-redefs [store/find-all-documents (fn [_db _policy]
                                                 (reset! find-all-called? true)
                                                 mock-documents)
                      store/transform-keys (constantly transformed-documents)]
          (indicators/handler {:db :mock-db :access-policy nil :query-params {"type" ""}})
          (should @find-all-called?))))

    (it "calls find-all-documents when type param is whitespace only"
      (let [find-all-called? (atom false)]
        (with-redefs [store/find-all-documents (fn [_db _policy]
                                                 (reset! find-all-called? true)
                                                 mock-documents)
                      store/transform-keys (constantly transformed-documents)]
          (indicators/handler {:db :mock-db :access-policy nil :query-params {"type" "   "}})
          (should @find-all-called?)))))

  (describe "with type filter"
    (it "calls find-document-by-type when type param is provided"
      (let [captured-type (atom nil)]
        (with-redefs [store/find-document-by-type (fn [_db t _policy]
                                                    (reset! captured-type t)
                                                    [(first mock-documents)])
                      store/transform-keys (constantly [(first transformed-documents)])]
          (indicators/handler {:db :mock-db :access-policy nil :query-params {"type" "IPv4"}})
          (should= "IPv4" @captured-type))))

    (it "returns HTTP 200 when filtering by type"
      (with-redefs [store/find-document-by-type (constantly [(first mock-documents)])
                    store/transform-keys (constantly [(first transformed-documents)])]
        (let [response (indicators/handler {:db :mock-db :access-policy nil :query-params {"type" "IPv4"}})]
          (should= 200 (:status response)))))

    (it "returns empty result when no documents match the type"
      (with-redefs [store/find-document-by-type (constantly [])
                    store/transform-keys (constantly [])]
        (let [response (indicators/handler {:db :mock-db :access-policy nil :query-params {"type" "URL"}})]
          (should= [] (-> response :body :results)))))))
