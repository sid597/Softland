(ns app.shared.threaded-material
  "Facet 5: revisioned policy for how far below a conversation column a fresh
   block may stand and still adopt that thread. Thread identity and turn truth
   remain durable instance data."
  (:require [app.shared.facet-material :as facet-material]))

(def master-id "fm:threaded")
(def grammar-version 0)
(def code-floor-revision-id "code-floor:fm:threaded:v0")

(def default-form
  {:facet-master/id master-id
   :facet-master/grammar grammar-version
   :facet-master/facet :threaded
   :threaded/column-adoption-reach-lines 3.0})

(def default-source (pr-str default-form))

(def spec
  {:facet-master/id master-id
   :facet-master/facet :threaded
   :facet-master/source-ref "softland://facet-master/threaded"
   :facet-master/default-form default-form
   :facet-master/floor-form default-form
   :facet-master/code-floor-revision-id code-floor-revision-id
   :facet-master/grammars
   {grammar-version
    {:material-keys
     #{:threaded/column-adoption-reach-lines}
     :validators
     {:threaded/column-adoption-reach-lines
      {:valid? facet-material/non-negative-number?
       :error-type :threaded/column-adoption-reach-lines-invalid}}}}})

(defn compile-form
  [form]
  (facet-material/compile-form spec form))

(defn compile-source
  [source]
  (facet-material/compile-source spec source))

(def code-floor
  (facet-material/code-floor spec))

(defn resolved-wear
  [served]
  (facet-material/resolved-wear spec served))

(defn contribution-stamp
  [wear subject site role slot]
  (facet-material/contribution-stamp wear subject site role slot))
