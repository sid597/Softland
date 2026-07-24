(ns app.shared.foldable-material
  "Facet 3: revisioned policy for the two run-section headers and their
   initial fold state. Per-appearance toggles remain ephemeral client state;
   this master supplies only the shared defaults and visible header copy."
  (:require [app.shared.facet-material :as facet-material]))

(def master-id "fm:foldable")
(def grammar-version 0)
(def code-floor-revision-id "code-floor:fm:foldable:v0")

(def default-form
  {:facet-master/id master-id
   :facet-master/grammar grammar-version
   :facet-master/facet :foldable
   :foldable/defaults {:noise? false :prose? false}
   :foldable/header-copy
   {:noise-label "thinking+tools"
    :prose-label "reply"
    :collapsed-marker "▸ "
    :expanded-marker "▾ "
    :show-suffix " — click to show"
    :hide-suffix " — click to hide"
    :line-count-prefix " (+"
    :line-count-suffix " lines)"}})

(def default-source (pr-str default-form))

(defn- exact-map?
  [x ks value-valid?]
  (and (map? x)
       (= ks (set (keys x)))
       (every? value-valid? (vals x))))

(defn- valid-defaults?
  [x]
  (exact-map? x #{:noise? :prose?} boolean?))

(defn- valid-header-copy?
  [x]
  (exact-map?
   x
   #{:noise-label
     :prose-label
     :collapsed-marker
     :expanded-marker
     :show-suffix
     :hide-suffix
     :line-count-prefix
     :line-count-suffix}
   string?))

(def spec
  {:facet-master/id master-id
   :facet-master/facet :foldable
   :facet-master/source-ref "softland://facet-master/foldable"
   :facet-master/default-form default-form
   :facet-master/floor-form default-form
   :facet-master/code-floor-revision-id code-floor-revision-id
   :facet-master/grammars
   {grammar-version
    {:material-keys
     #{:foldable/defaults
       :foldable/header-copy}
     :validators
     {:foldable/defaults
      {:valid? valid-defaults?
       :error-type :foldable/defaults-invalid}
      :foldable/header-copy
      {:valid? valid-header-copy?
       :error-type :foldable/header-copy-invalid}}}}})

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
