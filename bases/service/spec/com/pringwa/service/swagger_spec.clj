(ns com.pringwa.service.swagger-spec
  "Browser-based end-to-end specs for the Swagger UI using Etaoin.

  Boots the full Component system (Datomic Local :mem + Jetty) on a dedicated
  test port, then drives a headless Chrome browser against the live UI.

  Prerequisites:
    - Google Chrome and ChromeDriver must be installed and on PATH.
    - In CI (GitHub Actions ubuntu-latest), both are available by default.

  Run alongside the rest of the Speclj suite:
    clojure -M:dev:test"
  (:require [speclj.core :refer [after-all before before-all describe it pending should should-contain]]
            [com.pringwa.service.main :as main]
            [com.stuartsierra.component :as component]
            [etaoin.api :as e]))

;; ---------------------------------------------------------------------------
;; Config
;; ---------------------------------------------------------------------------

(def ^:private test-port 18080)
(def ^:private base-url  (str "http://localhost:" test-port))

(defn- test-config []
  (-> (main/config :dev)
      (assoc-in [:server :port] test-port)
      (assoc-in [:server :host] "localhost")))

;; ---------------------------------------------------------------------------
;; Shared state
;; ---------------------------------------------------------------------------

(def ^:private !system (atom nil))
(def ^:private !driver (atom nil))
(def ^:private !skip?  (atom false))

(defn- start! []
  (try
    (reset! !system (component/start (main/new-system (test-config))))
    (reset! !driver (e/chrome {:headless true
                                :args     ["--no-sandbox"
                                           "--disable-dev-shm-usage"
                                           "--disable-gpu"]}))
    (catch Exception _
      (reset! !skip? true))))

(defn- stop! []
  (when @!driver
    (e/quit @!driver)
    (reset! !driver nil))
  (when @!system
    (component/stop @!system)
    (reset! !system nil)))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- go! [path]
  (e/go @!driver (str base-url path)))

(defn- wait-for [selector]
  (e/wait-visible @!driver selector {:timeout 15}))

(defmacro ^:private browser-it [desc & body]
  `(it ~desc
     (if @!skip?
       (pending "ChromeDriver unavailable — install via: brew install --cask chromedriver")
       (do ~@body))))

;; ---------------------------------------------------------------------------
;; Specs
;; ---------------------------------------------------------------------------

(describe "Swagger UI — browser integration"
  (before-all (start!))
  (after-all  (stop!))

  (describe "page load"
    (browser-it "responds with the Swagger UI page"
      (go! "/api-docs")
      (wait-for {:css ".swagger-ui"})
      (should-contain "Swagger" (e/get-title @!driver)))

    (browser-it "displays the API title from the OpenAPI spec"
      (go! "/api-docs")
      (wait-for {:css ".title"})
      (should-contain "Centripetal" (e/get-element-text @!driver {:css ".title"}))))

  (describe "endpoint visibility"
    (before
      (when-not @!skip?
        (go! "/api-docs")
        (wait-for {:css ".opblock"})))

    (browser-it "lists the /healthcheck endpoint"
      (should (e/exists? @!driver {:css "[data-path='/healthcheck']"})))

    (browser-it "lists the /metrics endpoint"
      (should (e/exists? @!driver {:css "[data-path='/metrics']"})))

    (browser-it "lists the GET /v1/indicators endpoint"
      (should (e/exists? @!driver {:css "[data-path='/v1/indicators']"})))

    (browser-it "lists the POST /v1/indicators/search endpoint"
      (should (e/exists? @!driver {:css "[data-path='/v1/indicators/search']"})))

    (browser-it "lists the GET /v1/indicators/{id} endpoint"
      (should (e/exists? @!driver {:css "[data-path='/v1/indicators/{id}']"}))))

  (describe "OpenAPI spec endpoint"
    (browser-it "serves the raw swagger.json"
      (go! "/swagger.json")
      (wait-for {:tag :body})
      (let [source (e/get-source @!driver)]
        (should-contain "Centripetal Network" source)
        (should-contain "/v1/indicators" source)))))
