(ns com.pringwa.service.access-policy
  "Maps identity scopes to an access-policy consumed by the persistence layer.

  An access-policy is a plain map with two keys:

    :entity-rules   Datomic Datalog rules implementing a `(visible? ?doc)`
                    predicate.  When non-nil every query adds these rules and
                    the predicate to its :where clause so visibility is
                    evaluated inside Datomic's query engine, not in memory.
                    nil means unrestricted — no visibility predicate is added.

    :pull-pattern   The Datomic pull pattern used to fetch document attributes.
                    When non-nil it replaces the full document-pattern, so
                    restricted attributes are never fetched from the database.
                    nil means use the full pattern.

  Rule sets and pull patterns are plain data defined below.  Add new sets here
  to support new business rules (ownership, classification, tenancy, etc.)
  without touching the persistence or server components.

  The default scope->policy mapping is:
    indicators:admin       — unrestricted (nil rules, full pattern)
    indicators:read:amber  — amber TLP entity rules, full pattern
    indicators:read:green  — green TLP entity rules, full pattern
    indicators:read        — white TLP entity rules, full pattern
    (no matching scope)    — most restrictive: white-only, no sensitive attrs"
  (:require [com.pringwa.persistence.interface :as store]))

;; ---------------------------------------------------------------------------
;; Entity-level rule sets
;; Rules implement (visible? ?doc) — used verbatim by model query functions.
;; ---------------------------------------------------------------------------

(def ^:private white-rules
  '[[(visible? ?doc)
     [?doc :document/tlp "white"]]])

(def ^:private green-rules
  '[[(visible? ?doc)
     [?doc :document/tlp "white"]]
    [(visible? ?doc)
     [?doc :document/tlp "green"]]])

(def ^:private amber-rules
  '[[(visible? ?doc)
     [?doc :document/tlp "white"]]
    [(visible? ?doc)
     [?doc :document/tlp "green"]]
    [(visible? ?doc)
     [?doc :document/tlp "amber"]]])

;; ---------------------------------------------------------------------------
;; Attribute-level pull patterns
;; ---------------------------------------------------------------------------

(def ^:private sensitive-attrs
  #{:document/description :document/adversary})

(def ^:private redacted-pattern
  (filterv #(not (contains? sensitive-attrs %)) store/document-pattern))

;; ---------------------------------------------------------------------------
;; Policy registry
;; ---------------------------------------------------------------------------

(def ^:private scope->policy
  "Ordered: first matching scope wins."
  [["indicators:admin"
    {:entity-rules nil
     :pull-pattern nil}]

   ["indicators:read:amber"
    {:entity-rules amber-rules
     :pull-pattern nil}]

   ["indicators:read:green"
    {:entity-rules green-rules
     :pull-pattern nil}]

   ["indicators:read"
    {:entity-rules white-rules
     :pull-pattern nil}]])

(def ^:private most-restrictive
  {:entity-rules white-rules
   :pull-pattern redacted-pattern})

(defn from-identity
  "Returns an access-policy map for the given identity, or the most
  restrictive policy when no matching scope is found."
  [{:keys [scopes]}]
  (or (some (fn [[scope policy]]
              (when (contains? scopes scope) policy))
            scope->policy)
      most-restrictive))
