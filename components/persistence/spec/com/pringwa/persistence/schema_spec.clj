(ns com.pringwa.persistence.schema-spec
  (:require [speclj.core :refer [describe it should= before]]
            [com.pringwa.persistence.schema :as schema]
            [datomic.client.api :as d]))

(defn- fresh-conn []
  (let [client (d/client {:server-type :datomic-local
                          :storage-dir :mem
                          :system      (str (gensym "test-"))})]
    (d/create-database client {:db-name "test"})
    (let [conn (d/connect client {:db-name "test"})]
      (schema/install-schema! conn)
      conn)))

;; ---------------------------------------------------------------------------
;; document/id uniqueness
;; ---------------------------------------------------------------------------

(describe "document/id unique identity constraint"
  (it "upserts on re-transact — same document/id does not create a second entity"
    (let [conn (fresh-conn)
          doc  {:document/id   "sentinel-doc"
                :document/name "Original"}]
      (d/transact conn {:tx-data [doc]})
      (d/transact conn {:tx-data [doc]})
      (should= 1 (count (d/q '[:find ?e :in $ ?id :where [?e :document/id ?id]]
                              (d/db conn) "sentinel-doc")))))

  (it "updates existing document attributes on re-transact rather than duplicating"
    (let [conn (fresh-conn)]
      (d/transact conn {:tx-data [{:document/id "update-doc" :document/name "Before"}]})
      (d/transact conn {:tx-data [{:document/id "update-doc" :document/name "After"}]})
      (let [result (d/q '[:find ?name :in $ ?id :where [?e :document/id ?id] [?e :document/name ?name]]
                         (d/db conn) "update-doc")]
        (should= [["After"]] result)))))
