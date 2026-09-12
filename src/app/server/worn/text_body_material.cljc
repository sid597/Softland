(ns app.server.worn.text-body-material
  "Pure specification for text-body wrapping policy.
   The single v0 grammar requires positive integer floor and fallback column
   counts. Its default and code-floor forms are the same.

   Forms, EDN and served active maps yield compiler results or resolved values
   through facet-engine; contribution stamps label caller-supplied contributions.
   Owns immutable specification data only. It does not measure fonts, perform
   text layout, edit text or persist an active revision."
  (:require [app.server.worn.facet-engine :as facet-engine]))

(def master-id "fm:text-body")
(def grammar-version 0)
(def code-floor-revision-id "code-floor:fm:text-body:v0")

(def default-form
  {:facet-master/id master-id
   :facet-master/grammar grammar-version
   :facet-master/facet :text-body
   :text-body/wrap-floor-columns 32
   :text-body/wrap-fallback-columns 80})

(def default-source (pr-str default-form))

(defn- positive-integer?
  "Accept a positive value satisfying the common integer predicate."
  [x]
  (and (facet-engine/integer-number? x) (pos? x)))

(def spec
  {:facet-master/id master-id
   :facet-master/facet :text-body
   :facet-master/source-ref "softland://facet-master/text-body"
   :facet-master/default-form default-form
   :facet-master/floor-form default-form
   :facet-master/code-floor-revision-id code-floor-revision-id
   :facet-master/grammars
   {grammar-version
    {:material-keys
     #{:text-body/wrap-floor-columns
       :text-body/wrap-fallback-columns}
     :validators
     {:text-body/wrap-floor-columns
      {:valid? positive-integer?
       :error-type :text-body/wrap-floor-columns-invalid}
      :text-body/wrap-fallback-columns
      {:valid? positive-integer?
       :error-type :text-body/wrap-fallback-columns-invalid}}}}})

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
