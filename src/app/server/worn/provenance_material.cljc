(ns app.server.worn.provenance-material
  "The provenance facet specification.
   Takes: served provenance and composition forms.
   Gives: compiled provenance channels, composition values, and contribution rows.
   Holds: spec."
  (:require [app.server.worn.facet-engine :as facet-engine]))

(def master-id "fm:provenance")
(def grammar-version 0)
(def code-floor-revision-id "code-floor:fm:provenance:v0")
(def composition-grammar-version 1)
(def composition-code-floor-revision-id "code-floor:fm:provenance:v1")

(def default-form
  {:facet-master/id master-id
   :facet-master/grammar grammar-version
   :facet-master/facet :provenance
   :provenance/tint [0.62 0.66 0.76 0.6]})

(def default-source (pr-str default-form))

(def composition-form
  (assoc default-form
         :facet-master/grammar composition-grammar-version
         :facet-master/merge :append
         :facet-master/priority 10))

(def composition-source (pr-str composition-form))

(defn valid-tint?
  [x]
  (facet-engine/valid-rgba? x))

(def spec
  {:facet-master/id master-id
   :facet-master/facet :provenance
   :facet-master/source-ref "softland://facet-master/provenance"
   :facet-master/default-form default-form
   ;; P3's explicit composition revision is the new code floor. Durable v0
   ;; remains accepted by its original grammar entry below; it is never
   ;; backfilled with v1 meaning.
   :facet-master/floor-form composition-form
   :facet-master/code-floor-revision-id composition-code-floor-revision-id
   :facet-master/grammars
   {grammar-version
    {:material-keys #{:provenance/tint}
     :validators
     {:provenance/tint
      {:valid? valid-tint?
       :error-type :provenance/tint-invalid}}}
    composition-grammar-version
    {:material-keys
     #{:facet-master/merge
       :facet-master/priority
       :provenance/tint}
     :validators
     {:facet-master/merge
      {:valid? #{:append}
       :error-type :facet-master/merge-invalid}
      :facet-master/priority
      {:valid? facet-engine/integer-number?
       :error-type :facet-master/priority-invalid}
      :provenance/tint
      {:valid? valid-tint?
       :error-type :provenance/tint-invalid}}}}})

(defn compile-form
  [form]
  (facet-engine/compile-form spec form))

(defn compile-source
  [source]
  (facet-engine/compile-source spec source))

(def code-floor
  (facet-engine/code-floor spec))

(defn resolved-wear
  [served]
  (facet-engine/resolved-wear spec served))

(defn contribution-stamp
  [wear subject site role slot]
  (facet-engine/contribution-stamp wear subject site role slot))
