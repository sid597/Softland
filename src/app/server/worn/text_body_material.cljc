(ns app.server.worn.text-body-material
  "Facet 6: the narrow revisioned text-body policy slice used to choose a
   machine reply's wrap column. The raw text and source relationship remain
   durable entity truth."
  (:require [app.server.worn.facet-material :as facet-material]))

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
  [x]
  (and (facet-material/integer-number? x) (pos? x)))

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
