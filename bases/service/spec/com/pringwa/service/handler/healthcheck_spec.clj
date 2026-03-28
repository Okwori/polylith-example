(ns com.pringwa.service.handler.healthcheck-spec
  (:require [speclj.core :refer [describe it should-contain should=]]
            [com.pringwa.service.handler.healthcheck :as healthcheck]
            [datomic.client.api :as d]))

(describe "healthcheck handler"
  (describe "no connection (nil conn)"
    (it "returns HTTP 503 when conn is nil"
      (should= 503 (:status (healthcheck/handler {}))))

    (it "returns :status degraded when conn is nil"
      (should= "degraded" (-> (healthcheck/handler {}) :body :status)))

    (it "returns :checks map in body"
      (should-contain :checks (:body (healthcheck/handler {})))))

  (describe "healthy connection"
    (it "returns HTTP 200 when Datomic probe succeeds"
      (with-redefs [d/db (constantly :mock-db)]
        (should= 200 (:status (healthcheck/handler {:conn :mock-conn})))))

    (it "returns :status ok when healthy"
      (with-redefs [d/db (constantly :mock-db)]
        (should= "ok" (-> (healthcheck/handler {:conn :mock-conn}) :body :status))))

    (it "returns :checks with database=connected when healthy"
      (with-redefs [d/db (constantly :mock-db)]
        (should= "connected" (-> (healthcheck/handler {:conn :mock-conn}) :body :checks :database)))))

  (describe "degraded connection"
    (it "returns HTTP 503 when Datomic probe throws"
      (with-redefs [d/db (fn [_] (throw (ex-info "connection refused" {})))]
        (should= 503 (:status (healthcheck/handler {:conn :mock-conn})))))

    (it "returns :status degraded when probe throws"
      (with-redefs [d/db (fn [_] (throw (ex-info "connection refused" {})))]
        (should= "degraded" (-> (healthcheck/handler {:conn :mock-conn}) :body :status))))))
