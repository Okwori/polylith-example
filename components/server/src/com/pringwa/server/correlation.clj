(ns com.pringwa.server.correlation
  "Correlation ID middleware.

  Reads X-Request-ID from the incoming request (useful when a load-balancer
  or API Gateway sets it) or generates a UUID when absent.  The ID is:

    1. Stored in SLF4J MDC as \"requestId\" so every log line for this
       request automatically includes it in the JSON output.
    2. Attached to the Ring request as :request-id.
    3. Echoed back in the X-Request-ID response header so clients and
       load-balancers can correlate request/response pairs."
  (:import (java.util UUID)
           (org.slf4j MDC)))

(defn wrap-correlation-id [handler]
  (fn [request]
    (let [id (or (get-in request [:headers "x-request-id"])
                 (str (UUID/randomUUID)))]
      (MDC/put "requestId" id)
      (try
        (let [response (handler (assoc request :request-id id))]
          (update response :headers assoc "X-Request-ID" id))
        (finally
          (MDC/remove "requestId"))))))
