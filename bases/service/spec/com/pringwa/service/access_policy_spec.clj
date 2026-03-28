(ns com.pringwa.service.access-policy-spec
  (:require [speclj.core :refer [describe it should-be-nil should-not-be-nil should=]]
            [com.pringwa.service.access-policy :as policy]
            [com.pringwa.persistence.interface :as store]))

(describe "from-identity"
  (describe "nil / empty identity"
    (it "returns most-restrictive policy when scopes are nil"
      (let [p (policy/from-identity {:sub "anon" :scopes nil})]
        (should-not-be-nil (:entity-rules p))
        (should-not-be-nil (:pull-pattern p))))

    (it "returns most-restrictive policy when scopes are empty"
      (let [p (policy/from-identity {:sub "anon" :scopes #{}})]
        (should-not-be-nil (:entity-rules p))
        (should-not-be-nil (:pull-pattern p))))

    (it "returns most-restrictive policy for nil identity"
      (let [p (policy/from-identity nil)]
        (should-not-be-nil (:entity-rules p))
        (should-not-be-nil (:pull-pattern p)))))

  (describe "indicators:admin"
    (it "returns nil entity-rules (unrestricted)"
      (let [p (policy/from-identity {:scopes #{"indicators:admin"}})]
        (should-be-nil (:entity-rules p))))

    (it "returns nil pull-pattern (full attribute set)"
      (let [p (policy/from-identity {:scopes #{"indicators:admin"}})]
        (should-be-nil (:pull-pattern p))))

    (it "admin wins even when other scopes are also present"
      (let [p (policy/from-identity {:scopes #{"indicators:admin" "indicators:read"}})]
        (should-be-nil (:entity-rules p)))))

  (describe "indicators:read"
    (it "returns non-nil entity-rules (white-only visibility)"
      (let [p (policy/from-identity {:scopes #{"indicators:read"}})]
        (should-not-be-nil (:entity-rules p))))

    (it "returns nil pull-pattern (full attributes)"
      (let [p (policy/from-identity {:scopes #{"indicators:read"}})]
        (should-be-nil (:pull-pattern p))))

    (it "white-rules contain exactly one clause"
      (let [p (policy/from-identity {:scopes #{"indicators:read"}})]
        (should= 1 (count (:entity-rules p)))))

    (it "white-rules reference :document/tlp white"
      (let [rules (-> (policy/from-identity {:scopes #{"indicators:read"}})
                      :entity-rules)]
        (should= '[[(visible? ?doc) [?doc :document/tlp "white"]]] rules))))

  (describe "indicators:read:green"
    (it "returns non-nil entity-rules"
      (let [p (policy/from-identity {:scopes #{"indicators:read:green"}})]
        (should-not-be-nil (:entity-rules p))))

    (it "green-rules cover white and green TLP (2 clauses)"
      (let [rules (-> (policy/from-identity {:scopes #{"indicators:read:green"}})
                      :entity-rules)]
        (should= 2 (count rules))))

    (it "green-rules include white TLP"
      (let [rules (-> (policy/from-identity {:scopes #{"indicators:read:green"}})
                      :entity-rules)]
        (should-not-be-nil (some #(= '[?doc :document/tlp "white"] (second %)) rules))))

    (it "green-rules include green TLP"
      (let [rules (-> (policy/from-identity {:scopes #{"indicators:read:green"}})
                      :entity-rules)]
        (should-not-be-nil (some #(= '[?doc :document/tlp "green"] (second %)) rules)))))

  (describe "indicators:read:amber"
    (it "amber-rules cover white, green, and amber TLP (3 clauses)"
      (let [rules (-> (policy/from-identity {:scopes #{"indicators:read:amber"}})
                      :entity-rules)]
        (should= 3 (count rules))))

    (it "amber-rules include amber TLP"
      (let [rules (-> (policy/from-identity {:scopes #{"indicators:read:amber"}})
                      :entity-rules)]
        (should-not-be-nil (some #(= '[?doc :document/tlp "amber"] (second %)) rules)))))

  (describe "scope precedence (first match wins)"
    (it "admin beats read:amber"
      (let [p (policy/from-identity {:scopes #{"indicators:read:amber" "indicators:admin"}})]
        (should-be-nil (:entity-rules p))))

    (it "read:amber beats read:green"
      (let [amber-rules (-> (policy/from-identity {:scopes #{"indicators:read:amber"}}) :entity-rules)
            p           (policy/from-identity {:scopes #{"indicators:read:amber" "indicators:read:green"}})]
        (should= 3 (count (:entity-rules p)))))

    (it "read:green beats read"
      (let [p (policy/from-identity {:scopes #{"indicators:read:green" "indicators:read"}})]
        (should= 2 (count (:entity-rules p))))))

  (describe "most-restrictive (default)"
    (it "redacted pull-pattern omits :document/description"
      (let [p (policy/from-identity {:scopes #{}})]
        (should= false (contains? (set (:pull-pattern p)) :document/description))))

    (it "redacted pull-pattern omits :document/adversary"
      (let [p (policy/from-identity {:scopes #{}})]
        (should= false (contains? (set (:pull-pattern p)) :document/adversary))))

    (it "redacted pull-pattern retains :document/id"
      (let [p (policy/from-identity {:scopes #{}})]
        (should= true (contains? (set (:pull-pattern p)) :document/id))))

    (it "redacted pull-pattern is a strict subset of the full document-pattern"
      (let [p       (policy/from-identity {:scopes #{}})
            full    (set store/document-pattern)
            partial (set (:pull-pattern p))]
        (should= true (every? full partial))
        (should= false (= full partial))))))
