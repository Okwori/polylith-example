(ns com.pringwa.service.handler.healthcheck-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.pringwa.service.handler.healthcheck :as healthcheck]))

(deftest healthcheck-handler-test
         (testing "returns status 204"
                  (let [response (healthcheck/handler {})]
                    (is (= 204 (:status response)))))
         (testing "returns status 204 with nil request"
                  (let [response (healthcheck/handler nil)]
                    (is (= 204 (:status response)))))
         (testing "response has no body"
                  (let [response (healthcheck/handler {})]
                    (is (nil? (:body response))))))
