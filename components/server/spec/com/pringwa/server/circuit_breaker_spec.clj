(ns com.pringwa.server.circuit-breaker-spec
  (:require [speclj.core :refer [before describe it should should-contain should=]]
            [com.pringwa.server.circuit-breaker :as cb]))

(defn- ok-handler    [_] {:status 200 :body "ok"})
(defn- error-handler [_] {:status 500 :body {:error "boom"}})

(defn- make-breaker
  ([] (make-breaker {}))
  ([opts]
   (cb/wrap-circuit-breaker ok-handler
                             (merge {:failure-threshold   3
                                     :recovery-timeout-ms 60000
                                     :success-threshold   2}
                                    opts))))

(defn- make-breaker-with [handler opts]
  (cb/wrap-circuit-breaker handler
                            (merge {:failure-threshold   3
                                    :recovery-timeout-ms 60000
                                    :success-threshold   2}
                                   opts)))

(describe "wrap-circuit-breaker"
  (describe "closed state — normal operation"
    (it "allows requests when healthy"
      (let [handler (make-breaker)]
        (should= 200 (:status (handler {})))))

    (it "allows requests after a single failure"
      (let [handler (make-breaker-with
                      (let [calls (atom 0)]
                        (fn [_]
                          (swap! calls inc)
                          (if (= @calls 1)
                            {:status 500 :body {}}
                            {:status 200 :body "ok"})))
                      {})]
        (handler {}) ; failure
        (should= 200 (:status (handler {}))))) ; second call succeeds

    (it "does not open below the failure threshold"
      (let [calls (atom 0)
            handler (make-breaker-with
                      (fn [_]
                        (swap! calls inc)
                        (if (< @calls 3) {:status 500 :body {}} {:status 200 :body "ok"}))
                      {:failure-threshold 5})]
        ;; 2 failures, threshold is 5
        (handler {})
        (handler {})
        (should= 200 (:status (handler {}))))))

  (describe "opening the circuit"
    (it "opens after reaching the failure threshold"
      (let [handler (make-breaker-with error-handler {:failure-threshold 3})]
        (dotimes [_ 3] (handler {}))
        (should= 503 (:status (handler {})))))

    (it "returns :error in body when open"
      (let [handler (make-breaker-with error-handler {:failure-threshold 2})]
        (dotimes [_ 2] (handler {}))
        (should-contain :error (:body (handler {})))))

    (it "includes Retry-After header when open"
      (let [handler (make-breaker-with error-handler
                      {:failure-threshold   2
                       :recovery-timeout-ms 30000})]
        (dotimes [_ 2] (handler {}))
        (should= "30" (get-in (handler {}) [:headers "Retry-After"]))))

    (it "rejects subsequent requests immediately when open"
      (let [call-count (atom 0)
            inner      (fn [_] (swap! call-count inc) {:status 500 :body {}})
            handler    (make-breaker-with inner {:failure-threshold 2})]
        (dotimes [_ 2] (handler {})) ; open the circuit
        (let [calls-before @call-count]
          (handler {}) ; should be rejected without calling inner
          (should= calls-before @call-count)))))

  (describe "half-open recovery"
    (it "transitions to half-open and allows one probe after recovery window"
      ;; Use a very short recovery timeout so we can test recovery
      (let [calls (atom 0)
            inner (fn [_]
                    (swap! calls inc)
                    (if (<= @calls 3)
                      {:status 500 :body {}}
                      {:status 200 :body "ok"}))
            handler (make-breaker-with inner
                      {:failure-threshold   3
                       :recovery-timeout-ms 1})] ; 1ms recovery
        ;; Open the circuit
        (dotimes [_ 3] (handler {}))
        ;; Wait for recovery window
        (Thread/sleep 5)
        ;; Next request should be let through (probe)
        (should= 200 (:status (handler {})))))

    (it "re-opens if the probe fails"
      (let [inner   (fn [_] {:status 500 :body {}})
            handler (make-breaker-with inner
                      {:failure-threshold   2
                       :recovery-timeout-ms 1})]
        (dotimes [_ 2] (handler {}))
        (Thread/sleep 5)
        (handler {}) ; probe — fails
        ;; Should be open again
        (should= 503 (:status (handler {}))))))

  (describe "success tracking"
    (it "resets failure count after a success so circuit stays closed"
      ;; Alternating 500/200 calls: failures reset on each success so the
      ;; threshold of 4 is never reached.  Call 5 returns 500 from the inner
      ;; handler — NOT 503 from an open circuit.
      (let [calls (atom 0)
            inner (fn [_]
                    (let [n (swap! calls inc)]
                      (if (odd? n) {:status 500 :body {}} {:status 200 :body "ok"})))
            handler (make-breaker-with inner {:failure-threshold 4})]
        (dotimes [_ 4] (handler {}))
        ;; 5th call (odd) → inner returns 500; circuit is still closed, not 503
        (should= 500 (:status (handler {})))))))
