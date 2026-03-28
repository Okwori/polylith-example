(ns com.pringwa.server.cors-spec
  (:require [speclj.core :refer [describe it should should-contain should= should-not-contain]]
            [com.pringwa.server.cors :as cors]))

(defn- make-request
  ([method] (make-request method "https://app.example.com"))
  ([method origin]
   {:request-method method
    :uri            "/v1/indicators"
    :headers        (cond-> {} origin (assoc "origin" origin))}))

(defn- ok-handler [_] {:status 200 :headers {} :body "ok"})

(describe "wrap-cors"
  (describe "allowed origins — wildcard"
    (it "adds Access-Control-Allow-Origin: * for any origin"
      (let [handler (cors/wrap-cors ok-handler {:allowed-origins ["*"]})]
        (should= "*" (get-in (handler (make-request :get)) [:headers "Access-Control-Allow-Origin"]))))

    (it "passes through requests with no origin header (not a cross-origin request)"
      (let [handler  (cors/wrap-cors ok-handler {:allowed-origins ["*"]})
            response (handler (make-request :get nil))]
        (should= 200 (:status response)))))

  (describe "allowed origins — specific list"
    (it "adds CORS headers for an allowed origin"
      (let [handler (cors/wrap-cors ok-handler
                      {:allowed-origins ["https://app.example.com"]})]
        (should= "https://app.example.com"
                 (get-in (handler (make-request :get "https://app.example.com"))
                         [:headers "Access-Control-Allow-Origin"]))))

    (it "does not add CORS headers for a disallowed origin"
      (let [handler (cors/wrap-cors ok-handler
                      {:allowed-origins ["https://app.example.com"]})]
        (should-not-contain "Access-Control-Allow-Origin"
                            (:headers (handler (make-request :get "https://evil.example.com"))))))

    (it "still calls the handler for a disallowed origin"
      (let [called? (atom false)
            handler (cors/wrap-cors
                      (fn [_] (reset! called? true) {:status 200 :headers {} :body nil})
                      {:allowed-origins ["https://app.example.com"]})]
        (handler (make-request :get "https://evil.example.com"))
        (should @called?))))

  (describe "preflight OPTIONS"
    (it "returns 204 for OPTIONS requests from allowed origin"
      (let [handler (cors/wrap-cors ok-handler {:allowed-origins ["*"]})]
        (should= 204 (:status (handler (make-request :options))))))

    (it "does not call inner handler on preflight"
      (let [called? (atom false)
            handler (cors/wrap-cors
                      (fn [_] (reset! called? true) {:status 200 :headers {} :body nil})
                      {:allowed-origins ["*"]})]
        (handler (make-request :options))
        (should (not @called?))))

    (it "OPTIONS response includes Allow-Methods header"
      (let [handler (cors/wrap-cors ok-handler
                      {:allowed-origins ["*"]
                       :allowed-methods ["GET" "POST" "OPTIONS"]})]
        (should-contain "GET"
                        (get-in (handler (make-request :options))
                                [:headers "Access-Control-Allow-Methods"]))))

    (it "OPTIONS response includes Allow-Headers header"
      (let [handler (cors/wrap-cors ok-handler
                      {:allowed-origins ["*"]
                       :allowed-headers ["Authorization" "Content-Type"]})]
        (should-contain "Authorization"
                        (get-in (handler (make-request :options))
                                [:headers "Access-Control-Allow-Headers"])))))

  (describe "non-OPTIONS requests"
    (it "passes GET requests through to the handler"
      (let [called? (atom false)
            handler (cors/wrap-cors
                      (fn [_] (reset! called? true) {:status 200 :headers {} :body "ok"})
                      {:allowed-origins ["*"]})]
        (handler (make-request :get))
        (should @called?)))

    (it "merges CORS headers into the handler response"
      (let [handler (cors/wrap-cors
                      (fn [_] {:status 200 :headers {"Content-Type" "application/json"} :body nil})
                      {:allowed-origins ["*"]})]
        (let [headers (:headers (handler (make-request :get)))]
          (should-contain "Access-Control-Allow-Origin" headers)
          (should-contain "Content-Type" headers))))

    (it "adds Vary: Origin header"
      (let [handler (cors/wrap-cors ok-handler
                      {:allowed-origins ["https://app.example.com"]})]
        (should= "Origin"
                 (get-in (handler (make-request :get "https://app.example.com"))
                         [:headers "Vary"]))))))
