(ns com.pringwa.app-state.interface
  (:require [com.pringwa.app-state.core :as core]))

(defn create
  [config]
  (core/create-appstate config))
