(ns com.pringwa.persistence.interface
  (:require [com.pringwa.persistence.core :as db]
            [com.pringwa.persistence.model :as model]
            [com.pringwa.persistence.util :as util]))

(def document-pattern
  "Full pull pattern for a document entity.  Callers may derive restricted
  variants from this for attribute-level access policies."
  model/document-pattern)

(defn transform-keys [m]
  (util/transform-keys m))

(defn find-document
  ([db id]              (model/find-document db id))
  ([db id access-policy] (model/find-document db id access-policy)))

(defn find-all-documents
  ([db]              (model/find-all-documents db))
  ([db access-policy] (model/find-all-documents db access-policy)))

(defn find-document-by-type
  ([db type]              (model/find-document-by-type db type))
  ([db type access-policy] (model/find-document-by-type db type access-policy)))

(defn search-documents
  ([db criteria]              (model/search-documents db criteria))
  ([db criteria access-policy] (model/search-documents db criteria access-policy)))

(defn paginate
  "Slices a result vector and returns pagination metadata.
  Defaults: limit=20, offset=0. Clamps limit to [1, 100].

  Returns {:results [...] :total N :limit N :offset N}"
  ([results] (paginate results {}))
  ([results {:keys [limit offset] :or {limit 20 offset 0}}]
   (let [total  (count results)
         offset (max 0 (long offset))
         limit  (min 100 (max 1 (long limit)))
         start  (min offset total)
         end    (min (+ start limit) total)]
     {:results (subvec (vec results) start end)
      :total   total
      :limit   limit
      :offset  offset})))

(defn create [config]
  (db/new-database config))
