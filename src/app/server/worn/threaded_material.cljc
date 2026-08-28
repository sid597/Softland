(ns app.server.worn.threaded-material
  "Facet 5: revisioned policy for how far below a conversation column a fresh
   block may stand and still adopt that thread. Thread identity and turn truth
   remain durable instance data."
  (:require [app.server.worn.facet-material :as facet-material]))

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
    {:valid? facet-material/non-negative-number?
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
       {:valid? facet-material/non-negative-number?
        :error-type :threaded/edge-rail-width-invalid}
       :threaded/edge-rail-color
       {:valid? facet-material/valid-rgba?
        :error-type :threaded/edge-rail-color-invalid}
       :threaded/edge-indent
       {:valid? facet-material/non-negative-number?
        :error-type :threaded/edge-indent-invalid}})}}})

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
