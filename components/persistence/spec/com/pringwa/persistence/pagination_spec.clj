(ns com.pringwa.persistence.pagination-spec
  (:require [speclj.core :refer [describe it should= should-contain]]
            [com.pringwa.persistence.interface :as store]))

(def ^:private items (vec (range 50)))

(describe "paginate"
  (describe "defaults"
    (it "defaults to limit=20 and offset=0"
      (let [p (store/paginate items)]
        (should= 20 (:limit p))
        (should= 0  (:offset p))))

    (it "returns first 20 items by default"
      (should= (vec (range 20)) (-> (store/paginate items) :results)))

    (it "includes :total equal to input count"
      (should= 50 (:total (store/paginate items)))))

  (describe "offset"
    (it "skips the first N items"
      (should= [20 21 22] (-> (store/paginate items {:limit 3 :offset 20}) :results)))

    (it "offset beyond total returns empty results"
      (should= [] (-> (store/paginate items {:limit 10 :offset 100}) :results)))

    (it "offset equal to total returns empty results"
      (should= [] (-> (store/paginate items {:limit 10 :offset 50}) :results)))

    (it "clamps negative offset to 0"
      (should= 0 (:offset (store/paginate items {:limit 5 :offset -1})))))

  (describe "limit"
    (it "returns at most limit items"
      (should= 5 (count (:results (store/paginate items {:limit 5 :offset 0})))))

    (it "clamps limit to 100"
      (should= 100 (:limit (store/paginate items {:limit 999 :offset 0}))))

    (it "clamps limit minimum to 1"
      (should= 1 (:limit (store/paginate items {:limit 0 :offset 0}))))

    (it "returns fewer items when near the end"
      (should= (vec (range 48 50))
               (-> (store/paginate items {:limit 10 :offset 48}) :results))))

  (describe "metadata"
    (it "response contains :results :total :limit :offset"
      (let [p (store/paginate items {:limit 5 :offset 10})]
        (should-contain :results p)
        (should-contain :total p)
        (should-contain :limit p)
        (should-contain :offset p)))

    (it "total reflects original collection size regardless of pagination"
      (should= 50 (:total (store/paginate items {:limit 5 :offset 45}))))

    (it "works on empty collections"
      (let [p (store/paginate [])]
        (should= []  (:results p))
        (should= 0   (:total p))
        (should= 20  (:limit p))
        (should= 0   (:offset p))))))
