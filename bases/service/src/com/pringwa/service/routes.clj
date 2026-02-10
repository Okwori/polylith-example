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
       ;["" {:get {:handler (handlers/get-all-indicators-handler conn)}}]
       ["/search" {:post {:handler (fn [] {:x 1 :y 2}) #_(handlers/search-indicators-handler conn)}}]
       ;["/types" {:get {:handler #() #_(handlers/get-types-handler conn)}}]
       ;["/authors" {:get {:handler #() #_ (handlers/get-authors-handler conn)}}]
       ;["/:id" {:get {:handler #() #_ (handlers/get-indicator-by-id-handler conn)}}]
       ]])

   (ring/routes
     (swagger-ui/create-swagger-ui-handler
       {:path   "/api-docs"
        :url    "/swagger.json"
        :config {:validatorUrl     nil
                 :operationsSorter "alpha"}})
     (ring/redirect-trailing-slash-handler {:method :strip})
     (ring/create-default-handler))))
