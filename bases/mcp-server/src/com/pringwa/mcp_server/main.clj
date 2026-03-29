(ns com.pringwa.mcp-server.main
  (:require [com.pringwa.mcp-server.transport.sse   :as sse]
            [com.pringwa.mcp-server.transport.stdio :as stdio])
  (:gen-class))

(defn -main [& args]
  (let [transport (or (System/getenv "MCP_TRANSPORT") (first args) "stdio")
        port      (Integer/parseInt (or (System/getenv "MCP_PORT") "3001"))]
    (case transport
      "stdio"
      (stdio/start!)

      "sse"
      (do (println (str "MCP SSE server listening on port " port))
          (sse/start! port)
          @(promise))

      "both"
      (do (println (str "MCP SSE server listening on port " port))
          (sse/start! port)
          (stdio/start!))

      (throw (ex-info (str "Unknown transport: " transport
                           " — expected stdio, sse, or both") {})))))
