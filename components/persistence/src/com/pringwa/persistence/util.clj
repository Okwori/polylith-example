(ns com.pringwa.persistence.util
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.instant :as instant]
            [com.pringwa.persistence.schema :as schema]
            [datomic.client.api :as d]))

(def db-name "indicators")

(defn- parse-date [inst-str]
  (try
    (if inst-str (instant/read-instant-date inst-str) nil)
    (catch Exception _ nil)))

(defn- map->indicator-tx [ind]
  (cond-> {:indicator/indicator (get ind "indicator")
           :indicator/id        (get ind "id")
           :indicator/type      (get ind "type")
           :indicator/created   (parse-date (get ind "created"))}
          (get ind "description") (assoc :indicator/description (get ind "description"))
          (get ind "title")       (assoc :indicator/title (get ind "title"))
          (get ind "content")     (assoc :indicator/content (get ind "content"))))

(defn- map->document-tx [doc]
  (let [m {:document/industries         (get doc "industries")
           :document/tlp                (get doc "tlp")
           :document/description        (get doc "description")
           :document/created            (parse-date (get doc "created"))
           :document/tags               (get doc "tags")
           :document/modified           (parse-date (get doc "modified"))
           :document/author_name        (get doc "author_name")
           :document/public             (some-> (get doc "public") long)
           :document/extract_source     (get doc "extract_source")
           :document/references         (get doc "references")
           :document/targeted_countries (get doc "targeted_countries")
           :document/more_indicators    (get doc "more_indicators")
           :document/revision           (some-> (get doc "revision") long)
           :document/adversary          (get doc "adversary")
           :document/id                 (get doc "id")
           :document/name               (get doc "name")
           :document/indicators         (mapv map->indicator-tx (get doc "indicators"))}]
    (into {} (filter (comp some? val) m))))

(defn slurp-data!
[conn filename batch-size]
(if-let [res (io/resource filename)]
  (with-open [reader (io/reader res)]
    (let [data (json/read reader)]
      (doseq [batch (partition-all batch-size data)]
        (let [tx-data (mapv map->document-tx batch)]
          (d/transact conn {:tx-data tx-data})
          (println "Transacted batch of" (count batch))))))
  (println "Resource not found:" filename)))

(defn create-client []
  (d/client {:server-type :datomic-local
             :storage-dir :mem
             :system "indicators"}))

(defn create-database [client db-name]
  (d/create-database client {:db-name db-name})
  (d/connect client {:db-name db-name}))

(defn init-db [db-name]
  (let [client (create-client)
        conn (create-database client db-name)]
    (schema/install-schema! conn)
    {:client client :conn conn :db-name db-name}))
