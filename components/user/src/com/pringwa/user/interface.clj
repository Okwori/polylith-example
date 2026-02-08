(ns com.pringwa.user.interface
  (:require [com.pringwa.user.core :as core]))

(defn hello [name]
  (core/hello name))