(ns com.pringwa.service.routes
  (:require [clojure.java.io :as io]
            [com.pringwa.service.handler.healthcheck :as healthcheck]
            [com.pringwa.service.handler.indicator :as indicator]
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
      ["/indicators"
       ["/search" {:post {:handler (fn [req] {:status 200
                                              :body   [{:x 1 :y 2} {:a 2} [1 2 3]]}) #_(handlers/search-indicators-handler conn)}}]
       ["/:id" {:get {:handler indicator/handler,  :name ::indicator}}]
       ;["/types" {:get {:handler #() #_(handlers/get-types-handler conn)}}]
       ;["/authors" {:get {:handler #() #_ (handlers/get-authors-handler conn)}}]
       ;["/:id" {:get {:handler #() #_ (handlers/get-indicator-by-id-handler conn)}}]
       ]]
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
