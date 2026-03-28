(ns com.pringwa.server.timeout
  "Request timeout middleware.

  Runs the inner handler on the calling thread's future and dereferences
  it with a deadline.  If the handler does not complete within :timeout-ms
  milliseconds a 504 Gateway Timeout is returned and the in-flight future
  is cancelled (best-effort — the JVM cannot forcibly kill a running thread,
  but the cancellation flag is set so cooperative code will stop early).

  Configuration
    :timeout-ms  integer milliseconds, default 30000 (30 s)"
  (:require [clojure.tools.logging :as log]))

(def ^:private timed-out ::timed-out)

(defn wrap-timeout
  [handler {:keys [timeout-ms] :or {timeout-ms 30000}}]
  (fn [request]
    (let [f      (future (handler request))
          result (deref f timeout-ms timed-out)]
      (if (= result timed-out)
        (do
          (future-cancel f)
          (log/warnf "Request timed out after %dms: %s %s"
                     timeout-ms
                     (some-> request :request-method name)
                     (:uri request))
          {:status  504
           :headers {"Retry-After" (str (quot timeout-ms 1000))}
           :body    {:error "Request timed out"}})
        result))))
