(ns com.pringwa.server.metrics-state
  "In-process request metrics: counts and latency tracking.

  State is held in a single atom so reads are always consistent snapshots.
  All writes use swap! so there are no lock contention issues.

  Two public entry points:
    wrap-metrics  — Ring middleware that records each request
    snapshot      — Returns the current metrics map (for the /metrics handler)")

(def ^:private !metrics
  (atom {:requests {:total    0
                    :by-status {}}
         :latency  {:total-ms 0
                    :min-ms   Long/MAX_VALUE
                    :max-ms   0}
         :errors   0}))

(defn- record! [status duration-ms]
  (swap! !metrics
    (fn [m]
      (-> m
          (update-in [:requests :total] inc)
          (update-in [:requests :by-status status] (fnil inc 0))
          (update-in [:latency :total-ms] + duration-ms)
          (update-in [:latency :min-ms]   min duration-ms)
          (update-in [:latency :max-ms]   max duration-ms)
          (cond-> (>= status 500) (update :errors inc))))))

(defn snapshot
  "Returns an immutable snapshot of the current metrics."
  []
  (let [{:keys [requests latency errors]} @!metrics
        total (:total requests)]
    {:requests requests
     :errors   errors
     :latency  (if (zero? total)
                 {:avg-ms 0 :min-ms 0 :max-ms 0}
                 {:avg-ms (quot (:total-ms latency) total)
                  :min-ms (if (= (:min-ms latency) Long/MAX_VALUE) 0 (:min-ms latency))
                  :max-ms (:max-ms latency)})}))

(defn reset-metrics!
  "Resets all counters — intended for test isolation only."
  []
  (reset! !metrics {:requests {:total 0 :by-status {}}
                    :latency  {:total-ms 0 :min-ms Long/MAX_VALUE :max-ms 0}
                    :errors   0}))

(defn wrap-metrics
  "Records the HTTP status code and elapsed time for every response."
  [handler]
  (fn [request]
    (let [start    (System/currentTimeMillis)
          response (handler request)
          elapsed  (- (System/currentTimeMillis) start)]
      (record! (:status response) elapsed)
      response)))
