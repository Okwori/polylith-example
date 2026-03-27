(ns com.pringwa.service.handler.filter-indicators-spec
  (:require [speclj.core :refer [describe it should-contain should=]]
            [com.pringwa.service.handler.filter-indicators :as filter-indicators]
            [com.pringwa.persistence.interface :as store]))

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
  {:db :mock-db :access-policy nil :body-params body-params})

(describe "POST /v1/indicators/search"
  (describe "validation"
    (it "returns HTTP 400 when body is empty"
      (should= 400 (:status (filter-indicators/handler (make-request {})))))

    (it "returns HTTP 400 when body is nil"
      (should= 400 (:status (filter-indicators/handler (make-request nil)))))

    (it "returns an :error key in the body on 400"
      (should-contain :error (:body (filter-indicators/handler (make-request {})))))

    (it "returns HTTP 400 for unknown criteria keys"
      (should= 400 (:status (filter-indicators/handler (make-request {:adversarry "typo"})))))

    (it "returns :error body for unknown criteria keys"
      (should-contain :error (:body (filter-indicators/handler (make-request {:unknown-field "value"}))))))

  (describe "successful search — delegates to search-documents"
    (it "returns HTTP 200 when criteria are valid"
      (with-redefs [store/search-documents (constantly mock-documents)
                    store/transform-keys identity]
        (should= 200 (:status (filter-indicators/handler (make-request {:adversary "Plead"}))))))

    (it "returns body with :results key"
      (with-redefs [store/search-documents (constantly mock-documents)
                    store/transform-keys identity]
        (should-contain :results (:body (filter-indicators/handler (make-request {:adversary "Plead"}))))))

    (it "passes the criteria map directly to search-documents"
      (let [received-criteria (atom nil)]
        (with-redefs [store/search-documents (fn [_db criteria _policy]
                                               (reset! received-criteria criteria)
                                               mock-documents)
                      store/transform-keys identity]
          (filter-indicators/handler (make-request {:adversary "Plead"}))
          (should= {:adversary "Plead"} @received-criteria))))

    (it "passes the db value to search-documents"
      (let [received-db (atom nil)]
        (with-redefs [store/search-documents (fn [db _criteria _policy]
                                               (reset! received-db db)
                                               mock-documents)
                      store/transform-keys identity]
          (filter-indicators/handler (make-request {:adversary "Plead"}))
          (should= :mock-db @received-db))))

    (it "returns results via transform-keys"
      (with-redefs [store/search-documents (constantly mock-documents)
                    store/transform-keys (constantly transformed-documents)]
        (should= transformed-documents
                 (-> (filter-indicators/handler (make-request {:adversary "Plead"}))
                     :body :results))))

    (it "returns empty results when search-documents returns nothing"
      (with-redefs [store/search-documents (constantly [])
                    store/transform-keys (constantly [])]
        (should= [] (-> (filter-indicators/handler (make-request {:adversary "Unknown"}))
                        :body :results))))

    (it "searches by tags array (OR membership)"
      (with-redefs [store/search-documents (fn [_db criteria _policy]
                                             (filter #(some (set (:document/tags %))
                                                            (:tags criteria))
                                                     mock-documents))
                    store/transform-keys identity]
        (let [response (filter-indicators/handler (make-request {:tags ["china" "russia"]}))]
          (should= 2 (count (-> response :body :results))))))

    (it "combines multiple criteria in a single search-documents call"
      (let [call-count (atom 0)]
        (with-redefs [store/search-documents (fn [_db _criteria _policy]
                                               (swap! call-count inc)
                                               mock-documents)
                      store/transform-keys identity]
          (filter-indicators/handler (make-request {:adversary "Plead" :tlp "white"}))
          (should= 1 @call-count))))))
