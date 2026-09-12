(ns app.server.worn.threaded-material
  "Pure specification for conversation-thread placement and edge appearance.
   v0 declares column adoption reach in lines; v1 adds edge rail width, color
   and indent. Bootstrap uses v0; the code floor uses v1.

   Forms, EDN and served active maps yield compiler results or resolved values
   through facet-engine; contribution stamps label caller-supplied contributions.
   Owns immutable specification data only. It does not infer thread links,
   position blocks, draw rails or write episode structure."
  (:require [app.server.worn.facet-engine :as facet-engine]))

(def master-id "fm:threaded")
(def grammar-version 0)
(def code-floor-revision-id "code-floor:fm:threaded:v0")
(def thread-edge-grammar-version 1)
(def thread-edge-code-floor-revision-id "code-floor:fm:threaded:v1")

(def default-form
  {:facet-master/id master-id
   :facet-master/grammar grammar-version
   :facet-master/facet :threaded
   :threaded/column-adoption-reach-lines 3.0})

(def default-source (pr-str default-form))

(def thread-edge-form
  (assoc default-form
         :facet-master/grammar thread-edge-grammar-version
         :threaded/edge-rail-width 2.0
         :threaded/edge-rail-color [0.45 0.62 0.85 0.7]
         :threaded/edge-indent 10.0))

(def thread-edge-source (pr-str thread-edge-form))

(def ^:private v0-grammar
  {:material-keys
   #{:threaded/column-adoption-reach-lines}
   :validators
   {:threaded/column-adoption-reach-lines
    {:valid? facet-engine/non-negative-number?
     :error-type :threaded/column-adoption-reach-lines-invalid}}})

(def spec
  {:facet-master/id master-id
   :facet-master/facet :threaded
   :facet-master/source-ref "softland://facet-master/threaded"
   :facet-master/default-form default-form
   ;; v0's declaration and default bytes remain frozen. The structural keys
   ;; arrive as an explicit second grammar and become the total code floor.
   :facet-master/floor-form thread-edge-form
   :facet-master/code-floor-revision-id thread-edge-code-floor-revision-id
   :facet-master/grammars
   {grammar-version
    v0-grammar
    thread-edge-grammar-version
    {:material-keys
     (into (:material-keys v0-grammar)
           #{:threaded/edge-rail-width
             :threaded/edge-rail-color
             :threaded/edge-indent})
     :validators
     (merge
      (:validators v0-grammar)
      {:threaded/edge-rail-width
       {:valid? facet-engine/non-negative-number?
        :error-type :threaded/edge-rail-width-invalid}
       :threaded/edge-rail-color
       {:valid? facet-engine/valid-rgba?
        :error-type :threaded/edge-rail-color-invalid}
       :threaded/edge-indent
       {:valid? facet-engine/non-negative-number?
        :error-type :threaded/edge-indent-invalid}})}}})

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
