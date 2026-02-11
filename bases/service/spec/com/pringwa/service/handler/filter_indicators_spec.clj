(ns com.pringwa.service.handler.filter-indicators-spec
  (:require [speclj.core :refer :all]
            [com.pringwa.service.handler.indicators :as indicators]
            [com.pringwa.service.store :as store]
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

(def mock-ipv4-documents
  [{:document/id "doc-1"
    :document/name "Test Document 1"
    :document/adversary "Plead"
    :document/tlp "white"
    :document/indicators [{:indicator/indicator "192.168.1.1"
                           :indicator/type "IPv4"
                           :indicator/id 1}]}])

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

(describe "GET /indicators handler"

          (with-stubs)

          (before
            (with-redefs [store/init-db (fn [] {:conn :mock-conn})
                          d/db (fn [_] :mock-db)]))

          (context "without type parameter"

                   (it "returns all documents with status 200"
                       (with-redefs [store/find-all-documents (stub :find-all {:return mock-documents})
                                     store/transform-keys (stub :transform {:return transformed-documents})]
                         (let [request {:query-params {}}
                               response (indicators/handler request)]
                           (should= 200 (:status response))
                           (should-have-invoked :find-all)
                           (should-have-invoked :transform))))

                   (it "returns empty result when no documents exist"
                       (with-redefs [store/find-all-documents (stub :find-all {:return []})
                                     store/transform-keys (stub :transform {:return []})]
                         (let [request {:query-params {}}
                               response (indicators/handler request)]
                           (should= 200 (:status response))
                           (should= {:result []} (:body response)))))

                   (it "calls find-all-documents when type is empty string"
                       (with-redefs [store/find-all-documents (stub :find-all {:return mock-documents})
                                     store/transform-keys (stub :transform {:return transformed-documents})]
                         (let [request {:query-params {"type" ""}}
                               response (indicators/handler request)]
                           (should= 200 (:status response))
                           (should-have-invoked :find-all)
                           (should-not-have-invoked :find-by-type)))))

          (context "with type parameter"

                   (it "returns filtered documents by type IPv4"
                       (with-redefs [store/find-document-by-type (stub :find-by-type {:return mock-ipv4-documents})
                                     store/transform-keys (fn [docs] [{:id "doc-1"
                                                                       :name "Test Document 1"
                                                                       :adversary "Plead"
                                                                       :tlp "white"
                                                                       :indicators [{:indicator "192.168.1.1"
                                                                                     :type "IPv4"
                                                                                     :id 1}]}])]
                         (let [request {:query-params {"type" "IPv4"}}
                               response (indicators/handler request)]
                           (should= 200 (:status response))
                           (should-have-invoked :find-by-type {:with [:mock-db "IPv4"]}))))

                   (it "returns filtered documents by type hostname"
                       (with-redefs [store/find-document-by-type (stub :find-by-type {:return []})
                                     store/transform-keys (stub :transform {:return []})]
                         (let [request {:query-params {"type" "hostname"}}
                               response (indicators/handler request)]
                           (should= 200 (:status response))
                           (should-have-invoked :find-by-type {:with [:mock-db "hostname"]}))))

                   (it "returns empty result when no documents match type"
                       (with-redefs [store/find-document-by-type (stub :find-by-type {:return []})
                                     store/transform-keys (stub :transform {:return []})]
                         (let [request {:query-params {"type" "NonExistentType"}}
                               response (indicators/handler request)]
                           (should= 200 (:status response))
                           (should= {:result []} (:body response))))))

          (context "response structure"

                   (it "returns body with :result key"
                       (with-redefs [store/find-all-documents (constantly mock-documents)
                                     store/transform-keys (constantly transformed-documents)]
                         (let [request {:query-params {}}
                               response (indicators/handler request)]
                           (should-contain :result (:body response)))))

                   (it "result contains transformed documents"
                       (with-redefs [store/find-all-documents (constantly mock-documents)
                                     store/transform-keys (constantly transformed-documents)]
                         (let [request {:query-params {}}
                               response (indicators/handler request)
                               result (-> response :body :result)]
                           (should= 2 (count result))
                           (should= "doc-1" (-> result first :id)))))))

(run-specs)
