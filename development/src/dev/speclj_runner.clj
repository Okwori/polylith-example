(ns dev.speclj-runner
  (:require [clojure.string :as str]
            [polylith.clj.core.test-runner-contract.interface :as runner]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- clj-file? [{:keys [file-path]}]
  (or (str/ends-with? file-path ".clj")
      (str/ends-with? file-path ".cljc")))

(defn- spec-namespace? [{:keys [namespace]}]
  (str/ends-with? namespace "-spec"))

(defn- brick-spec-namespaces [bricks brick-names]
  (let [brick->test-ns (->> bricks
                             (map (juxt :name #(-> % :namespaces :test)))
                             (into {}))]
    (->> brick-names
         (mapcat brick->test-ns)
         (filter clj-file?)
         (filter spec-namespace?)
         (map :namespace)
         vec)))

;; ---------------------------------------------------------------------------
;; Constructor — called by the poly CLI at startup
;; ---------------------------------------------------------------------------

(defn create [{:keys [workspace project]}]
  (let [{:keys [bases components]} workspace
        {:keys [name bricks-to-test paths]} project
        test-sources-present* (delay (-> paths :test seq))
        ns-names* (delay (brick-spec-namespaces (into components bases) bricks-to-test))]

    (reify runner/TestRunner

      (test-runner-name [_] "speclj")

      (test-sources-present? [_] @test-sources-present*)

      (tests-present? [this _opts]
        (and (runner/test-sources-present? this) (seq @ns-names*)))

      (run-tests [this {:keys [eval-in-project is-verbose] :as opts}]
        (when (runner/tests-present? this opts)
          (println (str "Running Speclj specs for the " name " project..."))
          ;; Suppress SLF4J noise before any namespace (and therefore any logger)
          ;; is loaded. System properties are JVM-wide and are read by SLF4J
          ;; simple on its first initialisation inside the isolated classloader.
          (eval-in-project
           '(do (System/setProperty "org.slf4j.simpleLogger.defaultLogLevel" "error")
                (System/setProperty "slf4j.provider"
                                    "org.slf4j.simple.SimpleServiceProvider")))
          ;; Load every spec namespace — this registers all describe/it blocks
          ;; with speclj's global world atom.
          (doseq [ns-name @ns-names*]
            (when is-verbose (println "  Requiring:" ns-name))
            (eval-in-project `(require '~(symbol ns-name))))
          ;; speclj.run.standard/run-specs runs every registered spec and
          ;; returns the number of failures (0 = green).
          (let [failures (eval-in-project
                          '(do (require '[speclj.run.standard :as std])
                               (std/run-specs)))]
            (when (and (number? failures) (pos? failures))
              (throw (Exception. (str failures " Speclj spec(s) failed"))))))))))
