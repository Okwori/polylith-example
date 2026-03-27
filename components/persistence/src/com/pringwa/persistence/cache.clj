(ns com.pringwa.persistence.cache
  "LRU query-result cache keyed by Datomic basis-t.

  Cache key structure: [(:t db) query-fingerprint]

  Because Datomic db values carry a monotonically increasing :t (basis-t),
  results are automatically correct per snapshot:
    - same :t  → cache hit (data has not changed)
    - new :t   → cache miss (new transaction committed, fresh query runs)
    - old :t   → naturally displaced by LRU eviction

  This gives cache-correctness without TTL tuning or manual invalidation."
  (:require [clojure.core.cache :as c]
            [clojure.core.cache.wrapped :as cw]))

;; ---------------------------------------------------------------------------
;; Cache store
;; ---------------------------------------------------------------------------

(def ^:private threshold
  "Maximum number of entries before LRU eviction kicks in.
  256 covers all realistic query combinations with minimal memory footprint."
  256)

(defn- empty-cache []
  (c/lru-cache-factory {} :threshold threshold))

;; store is an atom wrapping a plain cache map — managed via cw/* functions.
(def ^:private store
  (atom (empty-cache)))

;; ---------------------------------------------------------------------------
;; Public API
;; ---------------------------------------------------------------------------

(defn basis-t
  "Returns the basis-t of a Datomic db value — used as the time component
  of every cache key."
  [db]
  (:t db))

(defn lookup-or-miss
  "Returns the cached value for k, or computes and caches (f) if absent.
  f is a zero-argument thunk."
  [k f]
  (cw/lookup-or-miss store k (fn [_] (f))))

(defn clear!
  "Resets the cache to an empty state.
  Intended for use in tests and application restart scenarios."
  []
  (reset! store (empty-cache)))
