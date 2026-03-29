(ns com.pringwa.server.publisher
  "mulog publisher lifecycle.

  Starts a publisher on server boot and returns a stop function.
  The publisher type is driven by config:

    {:type :console :pretty? true}            ; dev — pretty JSON to stdout
    {:type        :mulog/cloudwatch           ; staging/prod
     :cloudwatch/log-group-name \"/pringwa/service\"
     :cloudwatch/region         \"us-east-1\"}"
  (:require [com.brunobonacci.mulog :as u]))

(defn start!
  "Starts a mulog publisher from config. Returns a zero-arg stop function."
  [config]
  (u/start-publisher! config))
