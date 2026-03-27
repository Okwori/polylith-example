(ns com.pringwa.service.routes
  (:require [com.pringwa.service.handler.healthcheck :as healthcheck]
            [com.pringwa.service.handler.filter-indicators :as filter-indicators]
            [com.pringwa.service.handler.indicator :as indicator]
            [com.pringwa.service.handler.indicators :as indicators]
            [com.pringwa.service.handler.metrics :as metrics]
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
              :swagger {:info {:title       "Centripetal Network"
                               :description "Indicators API"}}
              :handler (swagger/create-swagger-handler)}}]
      ["/healthcheck" {:get healthcheck/handler, :name ::healthcheck}]
      ["/metrics"     {:get metrics/handler,     :name ::metrics}]
      ["/indicators"
       ["" {:get {:handler    indicators/handler :name ::indicators
                  :parameters {:query {:type string?}}}}]
       ["/search" {:post {:handler filter-indicators/handler :name ::filter-indicator}}]
       ["/:id" {:get {:handler    indicator/handler, :name ::indicator
                      :parameters {:path {:id string?}}}}]]]

     {:conflicts nil
      :data      {:muuntaja     m/instance
                  :middleware   [swagger/swagger-feature
                                 parameters/parameters-middleware
                                 muuntaja/format-negotiate-middleware
                                 muuntaja/format-response-middleware
                                 muuntaja/format-request-middleware
                                 coercion/coerce-response-middleware
                                 coercion/coerce-request-middleware]}})
   (ring/routes
     (swagger-ui/create-swagger-ui-handler
       {:path   "/api-docs"
        :url    "/swagger.json"
        :config {:validatorUrl     nil
                 :operationsSorter "alpha"}})
     (ring/redirect-trailing-slash-handler {:method :strip})
     (ring/create-default-handler))))
