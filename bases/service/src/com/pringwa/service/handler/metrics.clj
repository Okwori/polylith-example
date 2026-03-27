(ns com.pringwa.service.handler.metrics
  (:import (java.lang.management ManagementFactory)))

(defn- heap-stats []
  (let [heap (.getHeapMemoryUsage (ManagementFactory/getMemoryMXBean))]
    {:used-mb      (quot (.getUsed heap) 1048576)
     :committed-mb (quot (.getCommitted heap) 1048576)
     :max-mb       (quot (.getMax heap) 1048576)}))

(defn- thread-stats []
  (let [bean (ManagementFactory/getThreadMXBean)]
    {:count      (.getThreadCount bean)
     :peak-count (.getPeakThreadCount bean)}))

(defn handler [_]
  {:status 200
   :body   {:jvm    {:heap    (heap-stats)
                     :threads (thread-stats)}
            :uptime {:ms (.getUptime (ManagementFactory/getRuntimeMXBean))}}})
