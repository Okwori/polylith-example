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

(defn router []
  (ring/ring-handler
   (ring/router
     [["/swagger.json"
       {:get {:no-doc  true
              :auth/public? true
              :swagger {:info {:title       "Centripetal Network"
                               :description "Indicators API v1"}}
              :handler (swagger/create-swagger-handler)}}]
      ["/healthcheck" {:get {:handler      healthcheck/handler
                             :name         ::healthcheck
                             :auth/public? true}}]
      ["/metrics"     {:get {:handler      metrics/handler
                             :name         ::metrics
                             :auth/public? true}}]
      ["/v1"
       ["/indicators"
        ["" {:get {:handler    indicators/handler
                   :name       ::indicators
                   :auth/scopes #{:indicators/read}
                   :parameters {:query {:type string?}}}}]
        ["/search" {:post {:handler     filter-indicators/handler
                           :name        ::filter-indicator
                           :auth/scopes #{:indicators/search}}}]
        ["/:id" {:get {:handler     indicator/handler
                       :name        ::indicator
                       :auth/scopes #{:indicators/read}
                       :parameters  {:path {:id string?}}}}]]]]

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
