(ns com.pringwa.service.handler.indicator)

(defn handler
  [{:keys [reitit.core/match]}]
  (let [id (-> match :path-params :id Long/parseLong)]
   {:status 200
    :body   {:result id}}))
