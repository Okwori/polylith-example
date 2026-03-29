(ns com.pringwa.mcp-server.protocol
  (:require [com.pringwa.mcp-server.tools :as tools]))

(def ^:private server-info {:name "pringwa-mcp" :version "1.0.0"})
(def ^:private capabilities {:tools {}})

(defn- response [id result]
  {:jsonrpc "2.0" :id id :result result})

(defn- error-response [id code message]
  {:jsonrpc "2.0" :id id :error {:code code :message message}})

(defn handle [{:keys [id method params]}]
  (case method
    "initialize"
    (response id {:protocolVersion "2024-11-05"
                  :capabilities    capabilities
                  :serverInfo      server-info})

    "initialized" nil

    "ping"
    (response id {})

    "tools/list"
    (response id {:tools tools/definitions})

    "tools/call"
    (let [{:keys [name arguments]} params]
      (response id (tools/call name arguments)))

    (when id
      (error-response id -32601 (str "Method not found: " method)))))
