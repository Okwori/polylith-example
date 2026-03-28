(ns com.pringwa.server.compression
  "Response compression middleware (gzip).

  Compresses JSON and text responses when the client advertises
  Accept-Encoding: gzip.  Skips nil bodies (204/304) and non-compressible
  content types so binary assets and already-compressed streams pass through
  untouched.

  Place this just inside wrap-cors so CORS headers are added after compression
  and the Vary: Accept-Encoding header is already present."
  (:require [clojure.string :as str])
  (:import (java.io ByteArrayOutputStream)
           (java.util.zip GZIPOutputStream)))

(defn- accepts-gzip? [request]
  (some-> (get-in request [:headers "accept-encoding"])
          (str/includes? "gzip")))

(defn- compressible? [response]
  (when-let [ct (get-in response [:headers "Content-Type"])]
    (or (str/includes? ct "json")
        (str/includes? ct "text")
        (str/includes? ct "xml"))))

(defn- body->bytes [body]
  (cond
    (nil? body)    nil
    (bytes? body)  body
    (string? body) (.getBytes ^String body "UTF-8")
    :else          nil))

(defn- gzip [^bytes data]
  (let [baos (ByteArrayOutputStream.)]
    (with-open [gz (GZIPOutputStream. baos)]
      (.write gz data))
    (.toByteArray baos)))

(defn wrap-gzip
  "Gzip-compresses the response body when the client accepts it."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if (and (accepts-gzip? request)
               (compressible? response))
        (if-let [data (body->bytes (:body response))]
          (-> response
              (assoc  :body data)
              (assoc  :body (gzip data))
              (update :headers assoc
                      "Content-Encoding" "gzip"
                      "Vary"             "Accept-Encoding"))
          response)
        response))))
