(ns com.pringwa.server.middleware-spec
  (:require [speclj.core :refer :all]
            [clojure.tools.logging :as log]
            [com.pringwa.server.middleware :as middleware]))

(describe "wrap-connection"
  (it "injects :conn into the request"
    (let [received (atom nil)
          handler  (middleware/wrap-connection
                     (fn [req] (reset! received req) {:status 200})
                     :mock-conn)]
      (handler {:uri "/test"})
      (should= :mock-conn (:conn @received))))

  (it "passes the response through unchanged"
    (let [handler (middleware/wrap-connection
                    (constantly {:status 200 :body "ok"})
                    :mock-conn)]
      (should= {:status 200 :body "ok"} (handler {})))))

(describe "wrap-exception"
  (it "passes through a normal response"
    (let [handler (middleware/wrap-exception (constantly {:status 200 :body "ok"}))]
      (should= {:status 200 :body "ok"}
               (handler {:request-method :get :uri "/test"}))))

  (it "returns 500 when the handler throws"
    (with-redefs [log/log* (fn [& _])]
      (let [handler (middleware/wrap-exception
                      (fn [_] (throw (RuntimeException. "boom"))))]
        (should= 500
                 (:status (handler {:request-method :get :uri "/test"}))))))

  (it "returns an :error key in the body on exception"
    (with-redefs [log/log* (fn [& _])]
      (let [handler (middleware/wrap-exception
                      (fn [_] (throw (RuntimeException. "boom"))))]
        (should-contain :error
                        (:body (handler {:request-method :get :uri "/test"})))))))
