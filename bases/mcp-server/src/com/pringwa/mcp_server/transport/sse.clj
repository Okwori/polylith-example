(ns com.pringwa.mcp-server.transport.sse
  (:require [cheshire.core :as json]
            [com.pringwa.mcp-server.protocol :as protocol]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :as params])
  (:import [java.io PipedInputStream PipedOutputStream PrintWriter]
           [java.util UUID]))

(def ^:private sessions (atom {}))

(defn- send-event! [^PrintWriter writer event data]
  (.println writer (str "event: " event))
  (.println writer (str "data: " data))
  (.println writer "")
  (.flush writer))

(defn- send-to-session! [session-id msg]
  (when-let [writer (get @sessions session-id)]
    (try
      (send-event! writer "message" (json/generate-string msg))
      (catch Exception _
        (swap! sessions dissoc session-id)))))

(defn- keepalive-thread [session-id ^PrintWriter writer]
  (doto (Thread. (fn []
                   (try
                     (loop []
                       (Thread/sleep 15000)
                       (when (contains? @sessions session-id)
                         (.println writer ": keepalive")
                         (.flush writer)
                         (recur)))
                     (catch Exception _
                       (swap! sessions dissoc session-id)))))
    (.setDaemon true)
    (.start)))

(defn- sse-handler [_req]
  (let [session-id (str (UUID/randomUUID))
        pipe-out   (PipedOutputStream.)
        pipe-in    (PipedInputStream. pipe-out 65536)
        writer     (PrintWriter. pipe-out true)]
    (swap! sessions assoc session-id writer)
    (send-event! writer "endpoint" (str "/message?sessionId=" session-id))
    (keepalive-thread session-id writer)
    {:status  200
     :headers {"Content-Type"  "text/event-stream"
               "Cache-Control" "no-cache"
               "Connection"    "keep-alive"}
     :body    pipe-in}))

(defn- message-handler [req]
  (let [session-id (get-in req [:params "sessionId"])
        body       (json/parse-string (slurp (:body req)) true)
        response   (protocol/handle body)]
    (when response
      (send-to-session! session-id response))
    {:status 202 :body ""}))

(defn- router [req]
  (cond
    (and (= :get  (:request-method req)) (= "/sse"     (:uri req))) (sse-handler req)
    (and (= :post (:request-method req)) (= "/message" (:uri req))) (message-handler req)
    :else {:status 404 :body "Not found"}))

(defn start! [port]
  (jetty/run-jetty (params/wrap-params router) {:port port :join? false}))
