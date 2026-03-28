(ns com.pringwa.server.rate-limit
  "In-memory per-IP rate limiting using a fixed 1-second window.

  This is a defence-in-depth layer for single-instance deployments (dev /
  staging standalone).  In production Ion deployments API Gateway enforces
  rate limits before requests reach the application.

  Each IP gets an independent counter that resets every second.  Requests
  that exceed the limit receive a 429 with a Retry-After: 1 header.

  IP extraction honours the X-Forwarded-For header (first IP in the chain),
  falling back to :remote-addr, so it works correctly behind a load balancer.

  Configuration
    :requests-per-second  integer, default 100
    :key-fn               (fn [request]) -> string IP key (overridable for tests)"
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]))

(defn- client-ip [request]
  (or (some-> (get-in request [:headers "x-forwarded-for"])
              (str/split #",")
              first
              str/trim)
      (:remote-addr request)
      "unknown"))

(defn- current-window [] (quot (System/currentTimeMillis) 1000))

(defn- check-and-increment!
  "Increments the counter for this IP in the current window.
  Returns the new count so the caller can decide whether to allow."
  [!state ip]
  (let [window (current-window)]
    (get-in
      (swap! !state
        (fn [state]
          (let [{:keys [win cnt] :or {win -1 cnt 0}} (get state ip)]
            (if (= win window)
              (update-in state [ip :cnt] inc)
              (assoc state ip {:win window :cnt 1})))))
      [ip :cnt])))

(defn wrap-rate-limit
  "Rejects requests that exceed :requests-per-second for a given IP."
  [handler {:keys [requests-per-second key-fn]
            :or   {requests-per-second 100
                   key-fn              client-ip}}]
  (let [!state (atom {})]
    (fn [request]
      (let [ip  (key-fn request)
            cnt (check-and-increment! !state ip)]
        (if (<= cnt requests-per-second)
          (handler request)
          (do
            (log/debugf "Rate limit exceeded for %s (%d req/s)" ip cnt)
            {:status  429
             :headers {"Retry-After" "1"}
             :body    {:error "Rate limit exceeded — please slow down"}}))))))
