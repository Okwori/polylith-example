(ns com.pringwa.service.handler.healthcheck-spec
  (:require [speclj.core :refer :all]
            [com.pringwa.service.handler.healthcheck :as healthcheck]))

(describe "healthcheck handler"
  (it "returns HTTP 204"
    (should= 204 (:status (healthcheck/handler {}))))

  (it "returns HTTP 204 for nil request"
    (should= 204 (:status (healthcheck/handler nil))))

  (it "returns no body"
    (should-be-nil (:body (healthcheck/handler {}))))

  (it "returns no headers"
    (should-be-nil (:headers (healthcheck/handler {})))))
