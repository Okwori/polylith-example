(ns com.pringwa.server.request-log
  "Structured per-request access logging.

  Emits one INFO log line per request after the response is produced.
  Fields are pushed into SLF4J MDC for the duration of the log call so
  logstash-logback-encoder includes them as top-level JSON keys alongside
  the standard timestamp / level / logger fields.

  Example JSON output:
    {\"timestamp\":\"2026-03-28T12:00:00.000Z\",
     \"level\":\"INFO\",
     \"message\":\"GET /v1/indicators → 200 (14ms)\",
     \"requestId\":\"550e8400-...\",
     \"http.method\":\"GET\",
     \"http.uri\":\"/v1/indicators\",
     \"http.status\":\"200\",
     \"http.duration_ms\":\"14\"}"
  (:require [clojure.tools.logging :as log])
  (:import (org.slf4j MDC)))

(defn wrap-request-log [handler]
  (fn [request]
    (let [start    (System/currentTimeMillis)
          method   (some-> request :request-method name .toUpperCase)
          uri      (:uri request)
          response (handler request)
          elapsed  (- (System/currentTimeMillis) start)
          status   (:status response)]
      (MDC/put "http.method"      (or method ""))
      (MDC/put "http.uri"         (or uri ""))
      (MDC/put "http.status"      (str status))
      (MDC/put "http.duration_ms" (str elapsed))
      (try
        (if (>= status 500)
          (log/errorf "%s %s → %d (%dms)" method uri status elapsed)
          (log/infof  "%s %s → %d (%dms)" method uri status elapsed))
        (finally
          (MDC/remove "http.method")
          (MDC/remove "http.uri")
          (MDC/remove "http.status")
          (MDC/remove "http.duration_ms")))
      response)))
