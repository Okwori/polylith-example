(ns com.pringwa.persistence.specs-test
  "Bridges Speclj specs into clojure.test so `poly test` runs them.
  Requiring the spec namespaces registers their descriptions; runner/run-specs
  executes them and returns the failure count."
  (:require [clojure.test :refer [deftest is]]
            [speclj.run.standard :as runner]
            com.pringwa.persistence.cache-spec
            com.pringwa.persistence.query-spec))

(deftest persistence-specs
  (runner/run-specs "-f" "clojure-test"))
