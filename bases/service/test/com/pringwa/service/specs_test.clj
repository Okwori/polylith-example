(ns com.pringwa.service.specs-test
  "Bridges Speclj specs into clojure.test so `poly test` runs them."
  (:require [clojure.test :refer [deftest is]]
            [speclj.run.standard :as runner]
            com.pringwa.service.handler.filter-indicators-spec
            com.pringwa.service.handler.healthcheck-spec
            com.pringwa.service.handler.indicator-spec
            com.pringwa.service.handler.indicators-spec
            com.pringwa.service.integration-spec
            com.pringwa.service.swagger-spec))

(deftest service-specs
  (runner/run-specs "-f" "clojure-test"))
