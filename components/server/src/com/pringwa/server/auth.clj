(ns com.pringwa.server.auth
  "Authentication and authorization Ring middleware.

  Authentication — wrap-authentication
    Reads the Authorization: Bearer <token> header, delegates validation to a
    caller-supplied :validator-fn, and attaches the resulting claims map to the
    request as :identity {:sub \"...\" :scopes #{\"indicators:read\" ...}}.

    In dev mode (:dev? true) validation is skipped and a dev identity with
    unrestricted access is injected, so you can work locally without an IdP.

    For Ion/prod deployments API Gateway JWT Authorizer validates the token
    before the request reaches the app — :validator-fn can be omitted there.

    Public routes (marked :auth/public? true in Reitit route data) bypass
    authentication entirely.

  Authorization — wrap-authorization
    Must be placed in the Reitit router :middleware vector so it has access to
    the matched route data via ::reitit/match.

    Routes declare required scopes:
      {:auth/scopes  #{:indicators/read}}   checked against :identity scopes
      {:auth/public? true}                  bypasses auth entirely

    Returns 401 when no identity is present, 403 when scopes are insufficient."
  (:require [clojure.tools.logging :as log]
            [reitit.core :as reitit]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- bearer-token [request]
  (when-let [auth (get-in request [:headers "authorization"])]
    (when (.startsWith ^String auth "Bearer ")
      (subs auth 7))))

(defn- public-route? [request]
  (boolean (get-in request [::reitit/match :data :auth/public?])))

(defn- dev-identity []
  {:sub    "dev-user"
   :scopes #{"indicators:admin" "indicators:read" "indicators:search"}})

;; ---------------------------------------------------------------------------
;; Authentication
;; ---------------------------------------------------------------------------

(defn wrap-authentication
  "Validates the Bearer JWT and attaches :identity to the request.

  opts
    :dev?          when true, skip validation and inject a full-access dev identity
    :validator-fn  (fn [token-string]) -> claims-map or throws on invalid token
                   claims-map must contain :sub (string) and :scopes (set of strings)"
  [handler {:keys [dev? validator-fn]}]
  (fn [request]
    (cond
      (public-route? request)
      (handler request)

      dev?
      (handler (assoc request :identity (dev-identity)))

      (nil? validator-fn)
      (handler request)

      :else
      (let [token (bearer-token request)]
        (if-not token
          {:status 401
           :body   {:error "Missing Authorization header"}}
          (let [claims (try
                         (validator-fn token)
                         (catch Exception e
                           (log/warnf "Token validation failed: %s" (.getMessage e))
                           nil))]
            (if claims
              (handler (assoc request :identity claims))
              {:status 401
               :body   {:error "Invalid or expired token"}})))))))

;; ---------------------------------------------------------------------------
;; Authorization
;; ---------------------------------------------------------------------------

(defn wrap-authorization
  "Checks :auth/scopes declared in Reitit route data against :identity scopes.
  Place this inside the Reitit router :middleware vector, not as an outer wrapper."
  [handler]
  (fn [{:keys [identity] :as request}]
    (let [match           (::reitit/match request)
          public?         (get-in match [:data :auth/public?] false)
          required-scopes (get-in match [:data :auth/scopes])]
      (cond
        public?
        (handler request)

        (nil? required-scopes)
        (handler request)

        (nil? identity)
        {:status 401
         :body   {:error "Authentication required"}}

        (every? (:scopes identity) (map name required-scopes))
        (handler request)

        :else
        {:status 403
         :body   {:error "Insufficient permissions"}}))))
