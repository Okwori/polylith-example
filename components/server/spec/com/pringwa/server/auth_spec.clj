(ns com.pringwa.server.auth-spec
  (:require [speclj.core :refer [describe it should should-contain should=]]
            [clojure.tools.logging :as log]
            [com.pringwa.server.auth :as auth]
            [reitit.core :as reitit]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- with-route-data [request data]
  (assoc request ::reitit/match {:data data}))

(defn- bearer [token]
  {"authorization" (str "Bearer " token)})

(defn- capturing-handler []
  (let [received (atom nil)]
    [(fn [req] (reset! received req) {:status 200})
     received]))

;; ---------------------------------------------------------------------------
;; wrap-authentication
;; ---------------------------------------------------------------------------

(describe "wrap-authentication"
  (describe "public routes"
    (it "calls handler without checking auth"
      (let [[inner received] (capturing-handler)
            handler (auth/wrap-authentication inner
                      {:validator-fn (fn [_] (throw (ex-info "must not call" {})))})]
        (handler (with-route-data {} {:auth/public? true}))
        (should= {} (dissoc @received ::reitit/match))))

    (it "does not attach :identity on public routes"
      (let [[inner received] (capturing-handler)
            handler (auth/wrap-authentication inner {})]
        (handler (with-route-data {} {:auth/public? true}))
        (should= nil (:identity @received)))))

  (describe "dev mode"
    (it "injects identity with sub=dev-user"
      (let [[inner received] (capturing-handler)
            handler (auth/wrap-authentication inner {:dev? true})]
        (handler {})
        (should= "dev-user" (:sub (:identity @received)))))

    (it "injects identity with indicators:admin scope"
      (let [[inner received] (capturing-handler)
            handler (auth/wrap-authentication inner {:dev? true})]
        (handler {})
        (should-contain "indicators:admin" (:scopes (:identity @received)))))

    (it "calls handler without requiring a token"
      (let [called? (atom false)
            handler (auth/wrap-authentication
                      (fn [_] (reset! called? true) {:status 200})
                      {:dev? true})]
        (handler {:headers {}})
        (should @called?))))

  (describe "no validator-fn"
    (it "calls handler without authentication"
      (let [called? (atom false)
            handler (auth/wrap-authentication
                      (fn [_] (reset! called? true) {:status 200})
                      {})]
        (handler {:headers {}})
        (should @called?)))

    (it "does not attach :identity"
      (let [[inner received] (capturing-handler)
            handler (auth/wrap-authentication inner {})]
        (handler {:headers {}})
        (should= nil (:identity @received)))))

  (describe "token validation"
    (it "returns 401 when Authorization header is absent"
      (let [handler (auth/wrap-authentication
                      (constantly {:status 200})
                      {:validator-fn (constantly {:sub "u" :scopes #{}})})]
        (should= 401 (:status (handler {:headers {}})))))

    (it "returns :error in body when header is absent"
      (let [handler (auth/wrap-authentication
                      (constantly {:status 200})
                      {:validator-fn (constantly {:sub "u" :scopes #{}})})]
        (should-contain :error (:body (handler {:headers {}})))))

    (it "returns 401 when token fails validation"
      (with-redefs [log/log* (fn [& _])]
        (let [handler (auth/wrap-authentication
                        (constantly {:status 200})
                        {:validator-fn (fn [_] (throw (ex-info "bad" {})))})]
          (should= 401 (:status (handler {:headers (bearer "bad")}))))))

    (it "returns :error in body when token is invalid"
      (with-redefs [log/log* (fn [& _])]
        (let [handler (auth/wrap-authentication
                        (constantly {:status 200})
                        {:validator-fn (fn [_] (throw (ex-info "bad" {})))})]
          (should-contain :error (:body (handler {:headers (bearer "bad")}))))))

    (it "passes handler the :identity from valid claims"
      (let [claims  {:sub "alice" :scopes #{"indicators:read"}}
            [inner received] (capturing-handler)
            handler (auth/wrap-authentication inner {:validator-fn (constantly claims)})]
        (handler {:headers (bearer "good-token")})
        (should= claims (:identity @received))))

    (it "strips the Bearer prefix before passing to validator-fn"
      (let [captured (atom nil)
            handler  (auth/wrap-authentication
                       (constantly {:status 200})
                       {:validator-fn (fn [t] (reset! captured t) {:sub "u" :scopes #{}})})]
        (handler {:headers (bearer "my-secret-token")})
        (should= "my-secret-token" @captured)))

    (it "returns 200 on a valid token"
      (let [handler (auth/wrap-authentication
                      (constantly {:status 200})
                      {:validator-fn (constantly {:sub "u" :scopes #{}})})]
        (should= 200 (:status (handler {:headers (bearer "ok")})))))))

;; ---------------------------------------------------------------------------
;; wrap-authorization
;; ---------------------------------------------------------------------------

(describe "wrap-authorization"
  (describe "public routes"
    (it "passes through with no identity"
      (let [handler (auth/wrap-authorization (constantly {:status 200}))]
        (should= 200 (:status (handler (with-route-data {} {:auth/public? true}))))))

    (it "passes through even when scopes are also declared"
      (let [handler (auth/wrap-authorization (constantly {:status 200}))]
        (should= 200 (:status (handler (with-route-data {}
                                         {:auth/public? true
                                          :auth/scopes  #{:indicators/read}})))))))

  (describe "no required scopes"
    (it "passes through regardless of identity"
      (let [handler (auth/wrap-authorization (constantly {:status 200}))]
        (should= 200 (:status (handler (with-route-data {} {}))))))

    (it "passes through even when identity is nil"
      (let [handler (auth/wrap-authorization (constantly {:status 200}))]
        (should= 200 (:status (handler (with-route-data {:identity nil} {})))))))

  (describe "scope enforcement"
    (it "returns 401 when scopes required but identity is absent"
      (let [handler (auth/wrap-authorization (constantly {:status 200}))]
        (should= 401 (:status (handler (with-route-data {}
                                         {:auth/scopes #{:indicators/read}}))))))

    (it "returns :error in body on 401"
      (let [handler (auth/wrap-authorization (constantly {:status 200}))]
        (should-contain :error
                        (:body (handler (with-route-data {}
                                          {:auth/scopes #{:indicators/read}}))))))

    (it "returns 403 when identity lacks the required scope"
      (let [handler (auth/wrap-authorization (constantly {:status 200}))
            request (-> {:identity {:sub "u" :scopes #{"indicators:search"}}}
                        (with-route-data {:auth/scopes #{:indicators/read}}))]
        (should= 403 (:status (handler request)))))

    (it "returns :error in body on 403"
      (let [handler (auth/wrap-authorization (constantly {:status 200}))
            request (-> {:identity {:sub "u" :scopes #{"indicators:search"}}}
                        (with-route-data {:auth/scopes #{:indicators/read}}))]
        (should-contain :error (:body (handler request)))))

    (it "passes through when identity has the required scope"
      (let [handler (auth/wrap-authorization (constantly {:status 200}))
            request (-> {:identity {:sub "u" :scopes #{"indicators:read"}}}
                        (with-route-data {:auth/scopes #{:indicators/read}}))]
        (should= 200 (:status (handler request)))))

    (it "converts keyword scopes (:indicators/read) to string form (indicators:read) for matching"
      ;; Routes use keyword scopes; identity carries strings from JWT claims.
      ;; :indicators/read → \"indicators:read\"
      (let [handler (auth/wrap-authorization (constantly {:status 200}))
            request (-> {:identity {:sub "u" :scopes #{"indicators:read"}}}
                        (with-route-data {:auth/scopes #{:indicators/read}}))]
        (should= 200 (:status (handler request)))))

    (it "requires ALL declared scopes (AND semantics)"
      (let [handler (auth/wrap-authorization (constantly {:status 200}))
            request (-> {:identity {:sub "u" :scopes #{"indicators:read"}}}
                        (with-route-data {:auth/scopes #{:indicators/read :indicators/search}}))]
        (should= 403 (:status (handler request)))))

    (it "passes through when identity has all required scopes"
      (let [handler (auth/wrap-authorization (constantly {:status 200}))
            request (-> {:identity {:sub "u" :scopes #{"indicators:read" "indicators:search"}}}
                        (with-route-data {:auth/scopes #{:indicators/read :indicators/search}}))]
        (should= 200 (:status (handler request)))))))
