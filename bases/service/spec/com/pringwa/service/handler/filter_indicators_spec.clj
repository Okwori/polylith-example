(ns com.pringwa.service.handler.filter-indicators-spec
  (:require [speclj.core :refer :all]
            [com.pringwa.service.handler.filter-indicators :as filter-indicators]
            [com.pringwa.persistence.interface :as store]
            [datomic.client.api :as d]))

(def ^:private mock-documents
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

(def ^:private transformed-documents
  [{:id "doc-1" :name "Test Document 1" :adversary "Plead" :tlp "white"
    :author_name "AlienVault" :tags ["china" "apt"]}
   {:id "doc-2" :name "Test Document 2" :adversary "APT28" :tlp "green"
    :author_name "Mandiant" :tags ["russia" "apt"]}])

(defn- make-request [body-params]
  {:conn :mock-conn :body-params body-params})

(describe "POST /indicators/search"
  (describe "validation"
    (it "returns HTTP 400 when body is empty"
      (with-redefs [d/db (constantly :mock-db)]
        (should= 400 (:status (filter-indicators/handler (make-request {}))))))

    (it "returns HTTP 400 when body is nil"
      (with-redefs [d/db (constantly :mock-db)]
        (should= 400 (:status (filter-indicators/handler (make-request nil))))))

    (it "returns an :error key in the body on 400"
      (with-redefs [d/db (constantly :mock-db)]
        (should-contain :error (:body (filter-indicators/handler (make-request {}))))))

    (it "returns HTTP 400 for unknown criteria keys"
      (with-redefs [d/db (constantly :mock-db)]
        (should= 400 (:status (filter-indicators/handler (make-request {:adversarry "typo"}))))))

    (it "returns :error for unknown criteria keys"
      (with-redefs [d/db (constantly :mock-db)]
        (should-contain :error (:body (filter-indicators/handler (make-request {:unknown-field "value"})))))))

  (describe "successful search"
    (it "returns HTTP 200 when criteria are provided"
      (with-redefs [d/db (constantly :mock-db)
                    store/find-all-documents (constantly mock-documents)
                    store/matches? (constantly true)
                    store/transform-keys (constantly transformed-documents)]
        (should= 200 (:status (filter-indicators/handler (make-request {:adversary "Plead"}))))))

    (it "returns body with :results key"
      (with-redefs [d/db (constantly :mock-db)
                    store/find-all-documents (constantly mock-documents)
                    store/matches? (constantly true)
                    store/transform-keys (constantly transformed-documents)]
        (should-contain :results (:body (filter-indicators/handler (make-request {:adversary "Plead"}))))))

    (it "returns empty results when no documents match"
      (with-redefs [d/db (constantly :mock-db)
                    store/find-all-documents (constantly mock-documents)
                    store/matches? (constantly false)
                    store/transform-keys (constantly [])]
        (let [response (filter-indicators/handler (make-request {:adversary "Unknown"}))]
          (should= [] (-> response :body :results)))))

    (it "filters by adversary"
      (with-redefs [d/db (constantly :mock-db)
                    store/find-all-documents (constantly mock-documents)
                    store/matches? (fn [doc criteria]
                                     (= (:document/adversary doc) (:adversary criteria)))
                    store/transform-keys (constantly [(first transformed-documents)])]
        (let [response (filter-indicators/handler (make-request {:adversary "Plead"}))]
          (should= 1 (count (-> response :body :results))))))

    (it "filters by tlp"
      (with-redefs [d/db (constantly :mock-db)
                    store/find-all-documents (constantly mock-documents)
                    store/matches? (fn [doc criteria]
                                     (= (:document/tlp doc) (:tlp criteria)))
                    store/transform-keys (constantly [(first transformed-documents)])]
        (let [response (filter-indicators/handler (make-request {:tlp "white"}))]
          (should= 1 (count (-> response :body :results))))))

    (it "filters by author"
      (with-redefs [d/db (constantly :mock-db)
                    store/find-all-documents (constantly mock-documents)
                    store/matches? (fn [doc criteria]
                                     (= (:document/author_name doc) (:author_name criteria)))
                    store/transform-keys (constantly [(first transformed-documents)])]
        (let [response (filter-indicators/handler (make-request {:author_name "AlienVault"}))]
          (should= 1 (count (-> response :body :results))))))

    (it "applies multiple criteria (AND logic)"
      (with-redefs [d/db (constantly :mock-db)
                    store/find-all-documents (constantly mock-documents)
                    store/matches? (fn [doc criteria]
                                     (and (= (:document/adversary doc) (:adversary criteria))
                                          (= (:document/tlp doc) (:tlp criteria))))
                    store/transform-keys (constantly [(first transformed-documents)])]
        (let [response (filter-indicators/handler (make-request {:adversary "Plead" :tlp "white"}))]
          (should= 1 (count (-> response :body :results))))))

    (it "returns empty when combined criteria match no documents"
      (with-redefs [d/db (constantly :mock-db)
                    store/find-all-documents (constantly mock-documents)
                    store/matches? (constantly false)
                    store/transform-keys (constantly [])]
        (let [response (filter-indicators/handler (make-request {:adversary "Plead" :tlp "green"}))]
          (should= [] (-> response :body :results)))))))
