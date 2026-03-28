(ns com.pringwa.server.cors
  "CORS middleware.

  For Ion/API-Gateway deployments the gateway can handle CORS, but shipping
  it here gives identical behaviour in the standalone server and keeps
  the policy version-controlled alongside the application code.

  Preflight (OPTIONS) requests are answered immediately with 204 — they
  never reach authentication or business-logic handlers.

  Configuration
    :allowed-origins  vector of strings, or [\"*\"] for dev
    :allowed-methods  vector of method strings  (default: GET POST OPTIONS)
    :allowed-headers  vector of header strings  (default: Authorization Content-Type Accept)
    :max-age          preflight cache TTL in seconds (default: 3600)"
  (:require [clojure.string :as str]))

(defn- request-origin [request]
  (get-in request [:headers "origin"]))

(defn- allowed-origin [request allowed-origins]
  (let [origin (request-origin request)]
    (when origin
      (if (= allowed-origins ["*"])
        "*"
        (some #{origin} allowed-origins)))))

(defn- cors-response-headers [origin allowed-methods allowed-headers max-age]
  {"Access-Control-Allow-Origin"  origin
   "Access-Control-Allow-Methods" (str/join ", " allowed-methods)
   "Access-Control-Allow-Headers" (str/join ", " allowed-headers)
   "Access-Control-Max-Age"       (str max-age)
   "Vary"                         "Origin"})

(defn wrap-cors
  "Adds CORS response headers and handles OPTIONS preflight requests.

  When the request Origin is not in the allowed list no CORS headers are
  added — the browser will block the cross-origin request naturally."
  [handler {:keys [allowed-origins allowed-methods allowed-headers max-age]
            :or   {allowed-origins ["*"]
                   allowed-methods ["GET" "POST" "OPTIONS"]
                   allowed-headers ["Authorization" "Content-Type" "Accept"]
                   max-age         3600}}]
  (fn [request]
    (if-let [origin (allowed-origin request allowed-origins)]
      (let [headers (cors-response-headers origin allowed-methods allowed-headers max-age)]
        (if (= :options (:request-method request))
          {:status 204 :headers headers :body nil}
          (update (handler request) :headers merge headers)))
      (handler request))))
