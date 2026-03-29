(ns com.pringwa.server.request-log
  "Structured per-request access logging via mulog.

  Emits one event per request after the response is produced.
  The correlation ID flows in automatically via mulog's thread-local
  context set by wrap-correlation-id.

  Example event (as JSON in CloudWatch Logs):
    {\"mulog/event-name\": \"com.pringwa.server.request-log/http-request\",
     \"mulog/timestamp\": \"2026-03-28T12:00:00.000Z\",
     \"request-id\": \"550e8400-...\",
     \"http/method\": \"GET\",
     \"http/uri\": \"/v1/indicators\",
     \"http/status\": 200,
     \"http/duration-ms\": 14}"
  (:require [com.brunobonacci.mulog :as u]))

(defn wrap-request-log [handler]
  (fn [request]
    (let [start    (System/currentTimeMillis)
          method   (some-> request :request-method name .toUpperCase)
          uri      (:uri request)
          response (handler request)
          elapsed  (- (System/currentTimeMillis) start)
          status   (:status response)]
      (u/log ::http-request
             :mulog/level      (if (>= status 500) :error :info)
             :http/method      method
             :http/uri         uri
             :http/status      status
             :http/duration-ms elapsed)
      response)))
