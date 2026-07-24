(ns app.shared.attention-material
  "Facet 2: revisioned policy for the block's hit box and attention-only
   interaction border. Hover/focus remain ephemeral mechanism; these values
   are the shared material policy they reveal."
  (:require [app.shared.facet-material :as facet-material]))

(def master-id "fm:attention")
(def grammar-version 0)
(def code-floor-revision-id "code-floor:fm:attention:v0")

(def default-form
  {:facet-master/id master-id
   :facet-master/grammar grammar-version
   :facet-master/facet :attention
   :facet-master/merge :append
   :facet-master/priority 20
   :attention/hit-padding 8.0
   :attention/border-width 1.0
   :attention/border-color [0.45 0.52 0.66 0.55]
   :attention/background [0.0 0.0 0.0 0.0]})

(def default-source (pr-str default-form))

(def spec
  {:facet-master/id master-id
   :facet-master/facet :attention
   :facet-master/source-ref "softland://facet-master/attention"
   :facet-master/default-form default-form
   :facet-master/floor-form default-form
   :facet-master/code-floor-revision-id code-floor-revision-id
   :facet-master/grammars
   {grammar-version
    {:material-keys
     #{:facet-master/merge
       :facet-master/priority
       :attention/hit-padding
       :attention/border-width
       :attention/border-color
       :attention/background}
     :validators
     {:facet-master/merge
      {:valid? #{:append}
       :error-type :facet-master/merge-invalid}
      :facet-master/priority
      {:valid? facet-material/integer-number?
       :error-type :facet-master/priority-invalid}
      :attention/hit-padding
      {:valid? facet-material/non-negative-number?
       :error-type :attention/hit-padding-invalid}
      :attention/border-width
      {:valid? facet-material/non-negative-number?
       :error-type :attention/border-width-invalid}
      :attention/border-color
      {:valid? facet-material/valid-rgba?
       :error-type :attention/border-color-invalid}
      :attention/background
      {:valid? facet-material/valid-rgba?
       :error-type :attention/background-invalid}}}}})

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
