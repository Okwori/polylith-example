(ns com.pringwa.server.circuit-breaker
  "Circuit-breaker middleware for the Ring handler stack.

  Protects the application from cascading failures when an underlying
  dependency (e.g. Datomic) becomes unavailable.  Any 5xx response or
  unhandled exception counts as a failure.

  State machine
    :closed    — normal operation; failures increment a counter
    :open      — failure threshold exceeded; all requests get 503 immediately
    :half-open — recovery probe after the recovery window has elapsed;
                 one request is let through — success closes the breaker,
                 failure re-opens it

  Configuration
    :failure-threshold    consecutive failures before opening  (default 5)
    :recovery-timeout-ms  time in :open before trying :half-open (default 30000)
    :success-threshold    consecutive successes in :half-open to close  (default 2)"
  (:require [clojure.tools.logging :as log]))

;; ---------------------------------------------------------------------------
;; State helpers
;; ---------------------------------------------------------------------------

(defn- initial-state []
  {:state           :closed
   :failures        0
   :successes       0
   :last-failure-at nil})

(defn- record-success [state success-threshold]
  (case (:state state)
    :half-open
    (let [successes (inc (:successes state))]
      (if (>= successes success-threshold)
        (do (log/info "Circuit breaker CLOSED after successful probe")
            (initial-state))
        (assoc state :successes successes :failures 0)))

    :closed
    (assoc state :failures 0 :successes 0)

    state))

(defn- record-failure [state failure-threshold]
  (let [failures (inc (:failures state))
        now      (System/currentTimeMillis)]
    (if (>= failures failure-threshold)
      (do (log/warnf "Circuit breaker OPEN after %d failures" failures)
          {:state           :open
           :failures        failures
           :successes       0
           :last-failure-at now})
      (assoc state :failures failures :successes 0 :last-failure-at now))))

(defn- try-recover? [state recovery-timeout-ms]
  (and (= :open (:state state))
       (> (- (System/currentTimeMillis) (:last-failure-at state 0))
          recovery-timeout-ms)))

;; ---------------------------------------------------------------------------
;; Middleware
;; ---------------------------------------------------------------------------

(defn wrap-circuit-breaker
  [handler {:keys [failure-threshold recovery-timeout-ms success-threshold]
            :or   {failure-threshold   5
                   recovery-timeout-ms 30000
                   success-threshold   2}}]
  (let [!state (atom (initial-state))]
    (fn [request]
      (let [current @!state]
        (cond
          ;; Open but recovery window has elapsed → probe with half-open
          (try-recover? current recovery-timeout-ms)
          (do
            (swap! !state assoc :state :half-open :successes 0)
            (let [response (try (handler request)
                                (catch Exception e
                                  (log/errorf e "Circuit breaker probe failed")
                                  {:status 500 :body {:error "Internal error"}}))]
              (if (>= (:status response) 500)
                (do (swap! !state record-failure failure-threshold)
                    response)
                (do (swap! !state record-success success-threshold)
                    response))))

          ;; Open — reject immediately
          (= :open (:state current))
          (do
            (log/debugf "Circuit breaker OPEN — rejecting request %s" (:uri request))
            {:status  503
             :headers {"Retry-After" (str (quot recovery-timeout-ms 1000))}
             :body    {:error "Service temporarily unavailable — please retry shortly"}})

          ;; Closed or half-open — pass through and track outcome
          :else
          (let [response (try (handler request)
                              (catch Exception e
                                (log/errorf e "Unhandled exception in circuit breaker")
                                {:status 500 :body {:error "Internal error"}}))]
            (if (>= (:status response) 500)
              (swap! !state record-failure failure-threshold)
              (swap! !state record-success success-threshold))
            response))))))
