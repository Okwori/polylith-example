(ns com.pringwa.service.handler.indicator-spec
  (:require [speclj.core :refer :all]
            [com.pringwa.service.handler.indicator :as indicator]
            [com.pringwa.persistence.interface :as store]
            [datomic.client.api :as d]))

(def ^:private mock-document
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

(def ^:private transformed-document
  {:id "5b433d8fe822e72e3c57d26c"
   :name "Test Document"
   :adversary "Plead"
   :tlp "white"
   :author_name "AlienVault"
   :description "Test description"
   :tags ["china" "apt"]
   :indicators [{:indicator "192.168.1.1" :type "IPv4" :id 1}]})

(defn- make-request [id]
  {:conn :mock-conn
   :reitit.core/match {:path-params {:id id}}})

(describe "GET /indicators/:id"
  (describe "when document is found"
    (it "returns HTTP 200"
      (with-redefs [d/db (constantly :mock-db)
                    store/find-document (constantly mock-document)
                    store/transform-keys (constantly transformed-document)]
        (should= 200 (:status (indicator/handler (make-request "5b433d8fe822e72e3c57d26c"))))))

    (it "returns body with :result key"
      (with-redefs [d/db (constantly :mock-db)
                    store/find-document (constantly mock-document)
                    store/transform-keys (constantly transformed-document)]
        (should-contain :result (:body (indicator/handler (make-request "5b433d8fe822e72e3c57d26c"))))))

    (it "returns the document id"
      (with-redefs [d/db (constantly :mock-db)
                    store/find-document (constantly mock-document)
                    store/transform-keys (constantly transformed-document)]
        (let [result (-> (indicator/handler (make-request "5b433d8fe822e72e3c57d26c"))
                         :body :result)]
          (should= "5b433d8fe822e72e3c57d26c" (:id result)))))

    (it "returns document with indicators"
      (with-redefs [d/db (constantly :mock-db)
                    store/find-document (constantly mock-document)
                    store/transform-keys (constantly transformed-document)]
        (let [indicators (-> (indicator/handler (make-request "5b433d8fe822e72e3c57d26c"))
                             :body :result :indicators)]
          (should= 1 (count indicators))
          (should= "192.168.1.1" (-> indicators first :indicator))
          (should= "IPv4" (-> indicators first :type)))))

    (it "passes the path id to find-document"
      (let [captured-id (atom nil)]
        (with-redefs [d/db (constantly :mock-db)
                      store/find-document (fn [_ id]
                                            (reset! captured-id id)
                                            mock-document)
                      store/transform-keys (constantly transformed-document)]
          (indicator/handler (make-request "abc123"))
          (should= "abc123" @captured-id)))))

  (describe "when document is not found"
    (it "returns HTTP 404"
      (with-redefs [d/db (constantly :mock-db)
                    store/find-document (constantly nil)
                    store/transform-keys (constantly nil)]
        (should= 404 (:status (indicator/handler (make-request "missing-id"))))))

    (it "returns an :error key in the body"
      (with-redefs [d/db (constantly :mock-db)
                    store/find-document (constantly nil)
                    store/transform-keys (constantly nil)]
        (should-contain :error (:body (indicator/handler (make-request "missing-id"))))))))
