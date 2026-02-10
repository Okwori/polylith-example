(ns com.pringwa.app-state.core
  (:require [com.stuartsierra.component :as component]))

(defrecord AppState [config conn]
  component/Lifecycle
  (start [this]
    this)
  (stop [this]
    this))

(defn create-appstate
  [config]
  (component/using (map->AppState {:config config})
                   [:conn]))
