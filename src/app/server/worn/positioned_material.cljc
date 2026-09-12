(ns app.server.worn.positioned-material
  "Pure specification for placement policy and block drag bindings.
   v0 declares reply gap, fallback coordinates, ordered anchor rules and the
   persist-derived-reply-birth flag; v1 adds drag rows and v2 validates site
   arguments. Bootstrap uses v0; code-floor fallback uses v2.

   Forms, EDN and served active maps produce compiler results or resolved
   material through facet-engine, with optional contribution stamps. Owns only
   immutable specification data. It neither calculates live placement nor writes
   positions, maintains a drag, or persists reply births."
  (:require [app.server.worn.binding-material :as binding-material]
            [app.server.worn.facet-engine :as facet-engine]))

(def master-id "fm:positioned")
(def grammar-version 0)
(def code-floor-revision-id "code-floor:fm:positioned:v0")
(def bindings-grammar-version 1)
(def bindings-code-floor-revision-id "code-floor:fm:positioned:v1")
;; P6 · T10 — v2 adds ONE refusal to the v1 grammar (a row whose site cannot
;; feed its verb's required args). v1 keeps its own declaration below and is
;; never re-read through v2's validator.
(def strict-bindings-grammar-version 2)
(def strict-bindings-code-floor-revision-id "code-floor:fm:positioned:v2")

(def default-form
  {:facet-master/id master-id
   :facet-master/grammar grammar-version
   :facet-master/facet :positioned
   :positioned/reply-gap 34.0
   :positioned/fallback-position {:x 60.0 :y 60.0}
   :positioned/anchor-order
   {:machine [:same-source-tail :source :previous]
    :ordinary [:previous]}
   :positioned/persist-derived-reply-birth? true})

(def default-source (pr-str default-form))

(def ^:private drag-row
  {:binding/gesture :pointer/press
   :binding/phase :threshold
   :binding/modifiers #{}
   :binding/verb {:verb/name :placement/drag-group :verb/version 0}
   :binding/priority 10})

(def bindings-form
  "v1 = v0 plus the drag row, extracted verbatim from pointer-move! :pending's
   `:else` branch (`:dragging` on a block target) and pointer-up! :dragging's
   per-member `arm-settle! :cell`."
  (assoc default-form
         :facet-master/grammar bindings-grammar-version
         :facet-master/bindings
         {:block/user-hit-area [drag-row]
          :block/machine-hit-area [drag-row]}))

(def bindings-source (pr-str bindings-form))

(def strict-bindings-form
  "v2 = v1's rows VERBATIM under the stricter grammar. The bytes differ from v1
   only in the grammar version, so this migration adds a refusal and changes no
   behavior: every row that shipped still validates."
  (assoc bindings-form
         :facet-master/grammar strict-bindings-grammar-version))

(def strict-bindings-source (pr-str strict-bindings-form))

(def ^:private anchor-rules
  #{:same-source-tail :source :previous})

(defn- valid-position?
  "Require exactly finite numeric x and y coordinates."
  [x]
  (and (map? x)
       (= #{:x :y} (set (keys x)))
       (every? facet-engine/finite-number? (vals x))))

(defn- valid-anchor-order?
  "Require nonempty, duplicate-free machine/ordinary vectors of known anchor rules."
  [x]
  (and (map? x)
       (= #{:machine :ordinary} (set (keys x)))
       (every?
        (fn [order]
          (and (vector? order)
               (seq order)
               (= (count order) (count (distinct order)))
               (every? anchor-rules order)))
        (vals x))))

(def ^:private v0-grammar
  {:material-keys
   #{:positioned/reply-gap
     :positioned/fallback-position
     :positioned/anchor-order
     :positioned/persist-derived-reply-birth?}
   :validators
   {:positioned/reply-gap
    {:valid? facet-engine/non-negative-number?
     :error-type :positioned/reply-gap-invalid}
    :positioned/fallback-position
    {:valid? valid-position?
     :error-type :positioned/fallback-position-invalid}
    :positioned/anchor-order
    {:valid? valid-anchor-order?
     :error-type :positioned/anchor-order-invalid}
    :positioned/persist-derived-reply-birth?
    {:valid? boolean?
     :error-type :positioned/persist-derived-reply-birth-invalid}}})

(def spec
  {:facet-master/id master-id
   :facet-master/facet :positioned
   :facet-master/source-ref "softland://facet-master/positioned"
   :facet-master/default-form default-form
   :facet-master/floor-form strict-bindings-form
   :facet-master/code-floor-revision-id
   strict-bindings-code-floor-revision-id
   :facet-master/grammars
   {grammar-version v0-grammar
    bindings-grammar-version
    {:material-keys
     (conj (:material-keys v0-grammar) :facet-master/bindings)
     :validators
     (assoc (:validators v0-grammar)
            :facet-master/bindings binding-material/bindings-validator)}
    ;; v1 above stays EXACTLY as it shipped. A durable v1 revision is compiled
    ;; under v1's declaration forever; v2 is an additional entry, never a
    ;; rewrite of an existing one (P3's per-version grammar law).
    strict-bindings-grammar-version
    {:material-keys
     (conj (:material-keys v0-grammar) :facet-master/bindings)
     :validators
     (assoc (:validators v0-grammar)
            :facet-master/bindings
            binding-material/strict-bindings-validator)}}})

(defn compile-form
  "Validate a form under this spec; return validity, errors, grammar and material."
  [form]
  (facet-engine/compile-form spec form))

(defn compile-source
  "Read EDN and compile under this spec; return parse/validation errors as data."
  [source]
  (facet-engine/compile-source spec source))

(def code-floor
  (facet-engine/code-floor spec))

(defn resolved-wear
  "Resolve a complete served active map, falling back to this spec's code floor."
  [served]
  (facet-engine/resolved-wear spec served))

(defn contribution-stamp
  "Return subject/facet/revision and site/role/slot provenance for supplied wear."
  [wear subject site role slot]
  (facet-engine/contribution-stamp wear subject site role slot))
