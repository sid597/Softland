(ns app.shared.positioned-material
  "Facet 4: revisioned shared policy for derived placement and reply birth.
   Settled geometry cells remain per-instance durable truth; this master owns
   only the defaults used before a cell exists."
  (:require [app.shared.facet-material :as facet-material]))

(def master-id "fm:positioned")
(def grammar-version 0)
(def code-floor-revision-id "code-floor:fm:positioned:v0")

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

(def ^:private anchor-rules
  #{:same-source-tail :source :previous})

(defn- valid-position?
  [x]
  (and (map? x)
       (= #{:x :y} (set (keys x)))
       (every? facet-material/finite-number? (vals x))))

(defn- valid-anchor-order?
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

(def spec
  {:facet-master/id master-id
   :facet-master/facet :positioned
   :facet-master/source-ref "softland://facet-master/positioned"
   :facet-master/default-form default-form
   :facet-master/floor-form default-form
   :facet-master/code-floor-revision-id code-floor-revision-id
   :facet-master/grammars
   {grammar-version
    {:material-keys
     #{:positioned/reply-gap
       :positioned/fallback-position
       :positioned/anchor-order
       :positioned/persist-derived-reply-birth?}
     :validators
     {:positioned/reply-gap
      {:valid? facet-material/non-negative-number?
       :error-type :positioned/reply-gap-invalid}
      :positioned/fallback-position
      {:valid? valid-position?
       :error-type :positioned/fallback-position-invalid}
      :positioned/anchor-order
      {:valid? valid-anchor-order?
       :error-type :positioned/anchor-order-invalid}
      :positioned/persist-derived-reply-birth?
      {:valid? boolean?
       :error-type :positioned/persist-derived-reply-birth-invalid}}}}})

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
