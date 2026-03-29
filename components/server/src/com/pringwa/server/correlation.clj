(ns com.pringwa.server.correlation
  "Correlation ID middleware.

  Reads X-Request-ID from the incoming request (useful when a load-balancer
  or API Gateway sets it) or generates a UUID when absent.  The ID is:

    1. Bound into mulog's thread-local context so every u/log call for this
       request automatically includes it as :request-id.
    2. Attached to the Ring request as :request-id.
    3. Echoed back in the X-Request-ID response header so clients and
       load-balancers can correlate request/response pairs."
  (:require [com.brunobonacci.mulog :as u])
  (:import (java.util UUID)))

(defn wrap-correlation-id [handler]
  (fn [request]
    (let [id (or (get-in request [:headers "x-request-id"])
                 (str (UUID/randomUUID)))]
      (u/with-context {:request-id id}
        (let [response (handler (assoc request :request-id id))]
          (update response :headers assoc "X-Request-ID" id))))))
