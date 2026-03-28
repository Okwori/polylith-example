(ns com.pringwa.service.routes
  (:require [com.pringwa.server.interface :as server]
            [com.pringwa.service.handler.filter-indicators :as filter-indicators]
            [com.pringwa.service.handler.healthcheck :as healthcheck]
            [com.pringwa.service.handler.indicator :as indicator]
            [com.pringwa.service.handler.indicators :as indicators]
            [com.pringwa.service.handler.metrics :as metrics]
            [com.pringwa.service.scoped-db :as scoped-db]
            [muuntaja.core :as m]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]))

(def ^:private pagination-query-params
  {:limit  {:type "integer" :minimum 1 :maximum 100 :default 20}
   :offset {:type "integer" :minimum 0 :default 0}})

(def ^:private error-response
  {:description "Error"
   :content     {"application/json"
                 {:schema {:type "object"
                           :properties {"error" {:type "string"}}}}}})

(def ^:private paginated-response
  {:description "Paginated list of indicator documents"
   :content     {"application/json"
                 {:schema {:type "object"
                           :required ["results" "total" "limit" "offset"]
                           :properties {"results" {:type "array"}
                                        "total"   {:type "integer"}
                                        "limit"   {:type "integer"}
                                        "offset"  {:type "integer"}}}}}})

(defn router []
  (ring/ring-handler
   (ring/router
     [["/swagger.json"
       {:get {:no-doc       true
              :auth/public? true
              :swagger      {:info {:title       "Centripetal Network"
                                    :description "Threat Intelligence Indicators API v1"
                                    :version     "1.0.0"}}
              :handler      (swagger/create-swagger-handler)}}]

      ["/healthcheck"
       {:get {:handler      healthcheck/handler
              :name         ::healthcheck
              :auth/public? true
              :swagger      {:summary   "Liveness / readiness probe"
                             :responses {200 {:description "Service is healthy"}
                                         503 {:description "Service is degraded"}}}}}]

      ["/metrics"
       {:get {:handler      metrics/handler
              :name         ::metrics
              :auth/public? true
              :swagger      {:summary   "Application and JVM metrics"
                             :responses {200 {:description "Metrics snapshot"}}}}}]

      ["/v1"
       ["/indicators"
        ["" {:get {:handler     indicators/handler
                   :name        ::indicators
                   :auth/scopes #{:indicators/read}
                   :parameters  {:query (merge {:type string?}
                                               pagination-query-params)}
                   :swagger     {:summary     "List indicator documents"
                                 :description "Returns a paginated list of all indicator documents visible to the caller."
                                 :responses   {200 paginated-response
                                               401 error-response
                                               403 error-response}}}}]

        ["/search"
         {:post {:handler     filter-indicators/handler
                 :name        ::filter-indicator
                 :auth/scopes #{:indicators/search}
                 :swagger     {:summary     "Search indicator documents"
                               :description "Filters documents by criteria (AND logic). Pagination via ?limit= and ?offset= query params."
                               :responses   {200 paginated-response
                                             400 error-response
                                             401 error-response
                                             403 error-response}}}}]

        ["/:id"
         {:get {:handler     indicator/handler
                :name        ::indicator
                :auth/scopes #{:indicators/read}
                :parameters  {:path {:id string?}}
                :swagger     {:summary     "Get a single indicator document"
                              :responses   {200 {:description "The indicator document"}
                                            401 error-response
                                            403 error-response
                                            404 error-response}}}}]]]]

     {:conflicts nil
      :data      {:muuntaja   m/instance
                  :middleware [swagger/swagger-feature
                               parameters/parameters-middleware
                               muuntaja/format-negotiate-middleware
                               muuntaja/format-response-middleware
                               muuntaja/format-request-middleware
                               coercion/coerce-response-middleware
                               coercion/coerce-request-middleware
                               server/wrap-authorization
                               scoped-db/wrap-scoped-db]}})
   (ring/routes
     (swagger-ui/create-swagger-ui-handler
       {:path   "/api-docs"
        :url    "/swagger.json"
        :config {:validatorUrl     nil
                 :operationsSorter "alpha"}})
     (ring/redirect-trailing-slash-handler {:method :strip})
     (ring/create-default-handler))))
