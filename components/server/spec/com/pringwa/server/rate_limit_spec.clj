(ns com.pringwa.server.rate-limit-spec
  (:require [speclj.core :refer [before describe it should should-contain should=]]
            [com.pringwa.server.rate-limit :as rate-limit]))

(defn- make-request
  ([]     (make-request "1.2.3.4"))
  ([ip]   {:remote-addr ip :request-method :get :uri "/test"}))

(defn- ok-handler [_] {:status 200 :body "ok"})

(defn- fixed-ip-key [ip]
  (fn [_] ip))

(describe "wrap-rate-limit"
  (describe "requests within limit"
    (it "allows a request below the limit"
      (let [handler (rate-limit/wrap-rate-limit ok-handler
                      {:requests-per-second 10
                       :key-fn (fixed-ip-key "1.2.3.4")})]
        (should= 200 (:status (handler (make-request))))))

    (it "allows requests up to the limit"
      (let [handler (rate-limit/wrap-rate-limit ok-handler
                      {:requests-per-second 5
                       :key-fn (fixed-ip-key "1.2.3.4")})]
        (dotimes [_ 5]
          (should= 200 (:status (handler (make-request))))))))

  (describe "requests exceeding limit"
    (it "returns 429 on the request that exceeds the limit"
      (let [handler (rate-limit/wrap-rate-limit ok-handler
                      {:requests-per-second 3
                       :key-fn (fixed-ip-key "2.2.2.2")})]
        (dotimes [_ 3] (handler (make-request)))
        (should= 429 (:status (handler (make-request))))))

    (it "returns :error in the 429 body"
      (let [handler (rate-limit/wrap-rate-limit ok-handler
                      {:requests-per-second 2
                       :key-fn (fixed-ip-key "3.3.3.3")})]
        (dotimes [_ 2] (handler (make-request)))
        (should-contain :error (:body (handler (make-request))))))

    (it "includes Retry-After: 1 header on 429"
      (let [handler (rate-limit/wrap-rate-limit ok-handler
                      {:requests-per-second 1
                       :key-fn (fixed-ip-key "4.4.4.4")})]
        (handler (make-request))
        (should= "1" (get-in (handler (make-request)) [:headers "Retry-After"])))))

  (describe "per-IP isolation"
    (it "counts are independent per IP"
      (let [handler (rate-limit/wrap-rate-limit ok-handler
                      {:requests-per-second 2
                       :key-fn              (fn [req] (:remote-addr req))})]
        ;; exhaust IP A
        (dotimes [_ 2] (handler (make-request "10.0.0.1")))
        ;; IP B should still be allowed
        (should= 200 (:status (handler (make-request "10.0.0.2"))))))

    (it "rate-limited IP does not affect other IPs"
      (let [handler (rate-limit/wrap-rate-limit ok-handler
                      {:requests-per-second 1
                       :key-fn              (fn [req] (:remote-addr req))})]
        (handler (make-request "10.0.0.3"))
        (handler (make-request "10.0.0.3")) ; this one is rate-limited
        (should= 200 (:status (handler (make-request "10.0.0.4")))))))

  (describe "X-Forwarded-For support"
    (it "uses the first IP from X-Forwarded-For"
      (let [captured (atom nil)
            handler  (rate-limit/wrap-rate-limit
                       ok-handler
                       {:requests-per-second 100
                        :key-fn (fn [req]
                                  (let [ip (or (some-> (get-in req [:headers "x-forwarded-for"])
                                                       (clojure.string/split #",")
                                                       first
                                                       clojure.string/trim)
                                               (:remote-addr req))]
                                    (reset! captured ip)
                                    ip))})]
        (handler {:remote-addr     "10.0.0.5"
                  :headers         {"x-forwarded-for" "203.0.113.1, 10.0.0.5"}
                  :request-method  :get
                  :uri             "/test"})
        (should= "203.0.113.1" @captured)))))
