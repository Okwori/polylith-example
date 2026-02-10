(ns com.pringwa.service.routes
  (:require [clojure.java.io :as io]
            [com.pringwa.service.handler :as handler]
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
      ["/" {:get index/handler, :name ::index}]
      ["/healthcheck" {:get healthcheck/handler, :name ::healthcheck}]
      ["/api"
       ["/account/:id/re-send-invite" {:put  re-send-invite/handler
                                       :name ::re-send-invite}]
       ["/account/:id/re-send-new-email-invite" {:put  re-send-new-email-invite/handler
                                                 :name ::re-send-new-email-invite}]]])
   (ring/routes
     (swagger-ui/create-swagger-ui-handler
       {:path   "/api-docs"
        :url    "/swagger.json"
        :config {:validatorUrl     nil
                 :operationsSorter "alpha"}})
     (ring/redirect-trailing-slash-handler {:method :strip})
     (ring/create-default-handler))))
