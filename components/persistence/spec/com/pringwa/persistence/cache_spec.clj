(ns com.pringwa.persistence.cache-spec
  (:require [speclj.core :refer :all]
            [com.pringwa.persistence.cache :as cache]))

(defn- mock-db [t]
  {:t t})

(describe "cache"
  (before (cache/clear!))

  (describe "basis-t"
    (it "extracts :t from a db value"
      (should= 42 (cache/basis-t {:t 42})))

    (it "returns nil when db has no :t"
      (should= nil (cache/basis-t {}))))

  (describe "lookup-or-miss"
    (it "computes and returns the value on a cache miss"
      (should= :computed (cache/lookup-or-miss :k (constantly :computed))))

    (it "returns the cached value on a subsequent call — f is not called again"
      (let [call-count (atom 0)]
        (cache/lookup-or-miss :k (fn [] (swap! call-count inc) :result))
        (cache/lookup-or-miss :k (fn [] (swap! call-count inc) :result))
        (should= 1 @call-count)))

    (it "different keys are stored independently"
      (cache/lookup-or-miss :k1 (constantly :v1))
      (cache/lookup-or-miss :k2 (constantly :v2))
      (should= :v1 (cache/lookup-or-miss :k1 (constantly :ignored)))
      (should= :v2 (cache/lookup-or-miss :k2 (constantly :ignored))))

    (it "accepts composite keys"
      (let [k [42 :find-all]]
        (cache/lookup-or-miss k (constantly [:doc1 :doc2]))
        (should= [:doc1 :doc2]
                 (cache/lookup-or-miss k (constantly :stale)))))

    (it "different basis-t values produce independent cache entries"
      (let [call-count (atom 0)
            compute    (fn [] (swap! call-count inc) [:result])]
        (cache/lookup-or-miss [1 :find-all] compute)
        (cache/lookup-or-miss [2 :find-all] compute)
        (should= 2 @call-count)))

    (it "same basis-t and same query key hits the cache"
      (let [call-count (atom 0)
            compute    (fn [] (swap! call-count inc) [:result])]
        (cache/lookup-or-miss [1 :find-all] compute)
        (cache/lookup-or-miss [1 :find-all] compute)
        (should= 1 @call-count)))

    (it "same basis-t with different query fingerprints are independent"
      (let [call-count (atom 0)
            compute    (fn [] (swap! call-count inc) [:result])]
        (cache/lookup-or-miss [1 :find-all]              compute)
        (cache/lookup-or-miss [1 :find-by-type "IPv4"]   compute)
        (cache/lookup-or-miss [1 {:adversary "Plead"}]   compute)
        (should= 3 @call-count)))

  (describe "clear!"
    (it "removes all cached entries"
      (cache/lookup-or-miss :a (constantly 1))
      (cache/lookup-or-miss :b (constantly 2))
      (cache/clear!)
      (let [call-count (atom 0)]
        (cache/lookup-or-miss :a (fn [] (swap! call-count inc) 1))
        (cache/lookup-or-miss :b (fn [] (swap! call-count inc) 2))
        (should= 2 @call-count)))

    (it "cache works normally after clear!"
      (cache/clear!)
      (should= :fresh (cache/lookup-or-miss :k (constantly :fresh)))))))
