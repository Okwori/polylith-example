(ns com.pringwa.cli.core
  (:require [com.pringwa.user.interface :as user])
  (:gen-class))


(defn -main [& args]
  (println (user/hello (first args)))
  (System/exit 0))
