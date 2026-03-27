(ns com.pringwa.server.specs-test
  "Bridges Speclj specs into clojure.test so `poly test` runs them."
  (:require [clojure.test :refer [deftest is]]
            [speclj.run.standard :as runner]
            com.pringwa.server.middleware-spec))

(deftest server-specs
  (runner/run-specs "-f" "clojure-test"))
