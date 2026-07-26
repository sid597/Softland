(ns app.shared.space-material
  "The outer space's first served facet-master.

   Zoom bounds are shared, revisioned form. Tap and Shift-drag meanings are
   bindable master material; camera gestures remain only in
   `binding-material/space-floor-bindings`, structurally uncapturable by any
   material tier."
  (:require [app.shared.binding-material :as binding-material]
            [app.shared.facet-material :as facet-material]))

(def master-id "fm:space")
(def grammar-version 0)
(def code-floor-revision-id binding-material/space-floor-master-id)
(def bindings-grammar-version 1)

(def zoom-form
  {:facet-master/id master-id
   :facet-master/grammar grammar-version
   :facet-master/facet :space
   :space/zoom-min 0.1
   :space/zoom-max 8.0})

(def master-bindings
  "T2 — only the bindable subset of the four space floor rows. The camera rows
   stay floor-only; derive rather than duplicate the two surviving rows."
  (into {}
        (map (fn [[site rows]]
               [site
                (filterv
                 #(contains?
                   #{:anchor/place :selection/marquee-begin}
                   (get-in % [:binding/verb :verb/name]))
                 rows)]))
        binding-material/space-floor-bindings))

(def default-form
  "The form a new `fm:space` master activates: zoom policy plus the two meanings
   material is allowed to own."
  (assoc zoom-form
         :facet-master/grammar bindings-grammar-version
         :facet-master/bindings master-bindings))

(def default-source (pr-str default-form))

(defn- valid-zoom-bound?
  [x]
  (and (facet-material/finite-number? x)
       (<= 0.01 x 1000.0)))

(defn- valid-zoom-clamp?
  "T9 — total over nil, arbitrary maps, and instance-material projections with
   three extra keys. Only the two space keys participate in the invariant."
  [material]
  (let [zoom-min (get material :space/zoom-min)
        zoom-max (get material :space/zoom-max)]
    (and (map? material)
         (valid-zoom-bound? zoom-min)
         (valid-zoom-bound? zoom-max)
         (< zoom-min zoom-max))))

(defn- valid-master-bindings?
  [bindings]
  (and (binding-material/valid-bindings-strict? bindings)
       (every?
        (fn [[site rows]]
          (not-any? #(binding-material/camera-gesture-reserved? site %) rows))
        bindings)))

(def ^:private zoom-grammar
  {:material-keys #{:space/zoom-min :space/zoom-max}
   :validators
   {:space/zoom-min
    {:valid? valid-zoom-bound?
     :error-type :space/zoom-min-invalid}
    :space/zoom-max
    {:valid? valid-zoom-bound?
     :error-type :space/zoom-max-invalid}}
   ;; RULING R1 · T9 — the first consumer of the optional whole-form seam.
   :form-validators
   [{:valid? valid-zoom-clamp?
     :error-type :space/zoom-clamp-invalid}]})

(def spec
  {:facet-master/id master-id
   :facet-master/facet :space
   :facet-master/source-ref "softland://facet-master/space"
   ;; Bootstrap activates the v1 bindable meaning. The v0 code floor contains
   ;; only zoom form; the four camera-inclusive rows stay in the binding kernel.
   :facet-master/default-form default-form
   :facet-master/floor-form zoom-form
   :facet-master/code-floor-revision-id code-floor-revision-id
   :facet-master/grammars
   {grammar-version zoom-grammar
    bindings-grammar-version
    {:material-keys
     (conj (:material-keys zoom-grammar) :facet-master/bindings)
     :validators
     (assoc (:validators zoom-grammar)
            :facet-master/bindings
            {:valid? valid-master-bindings?
             :error-type :facet-master/bindings-invalid})
     :form-validators (:form-validators zoom-grammar)}}})

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
