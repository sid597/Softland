(ns app.client.region3d.component
  "Canonicalize and validate region data.

   Input: region/object/view values. Output: canonical values, schema
   acceptance or named errors. No retained state. Defaults and schemas
   define version 2; object kinds mesh/light/empty/text/ink; six primitive
   kinds; perspective/orthographic lenses; directional/point/spot lights;
   source references and provenance. :empty is a legal transform-bearing
   object kind in this source.

   Folder map: README.md."
  (:require [app.client.engine.color :as color]
            [app.client.engine.schema :as schema]))

(def schema-version 2)
(def shadow-algorithm-version :region3d/shadow-v1)
(def quaternion-tolerance 1.0e-3)
(def extent-max 1.0e4)
;; Indexed-mesh cardinality limits apply to supplied indexed geometry.
;; Primitive segment limits are per parameter: their products can generate
;; more vertices/triangles than these indexed-mesh limits.
(def mesh-vertex-max 65536)
(def mesh-triangle-max 131072)
(def legal-object-kinds #{:mesh :light :empty :text :ink})
(def legal-primitive-kinds #{:box :sphere :cylinder :plane :cone :torus})
(def legal-light-kinds #{:directional :point :spot})

(def default-background
  {:kind :opaque
   :color {:rgba [0.12 0.13 0.15 1.0]
           :color-space :srgb
           :alpha-association :straight}})

(def default-ambient
  {:color {:rgba [1.0 1.0 1.0 1.0]
           :color-space :srgb
           :alpha-association :straight}
   :intensity 0.1})

(def default-perspective-lens
  {:kind :perspective :fov-y-deg 50.0 :near 0.1 :far 1.0e4})

(def default-ortho-lens
  {:kind :ortho :ortho-scale 10.0 :near 0.1 :far 1.0e4})

(def default-view
  {:pivot [0.0 0.0 0.0]
   :distance 10.0
   :yaw 0.0
   :pitch 0.0
   :lens default-perspective-lens})

(def primitive-defaults
  {:box {:size [1.0 1.0 1.0]}
   :sphere {:radius 0.5 :width-segments 32 :height-segments 16}
   :cylinder {:radius 0.5 :height 1.0 :radial-segments 32}
   :plane {:size [1.0 1.0]}
   :cone {:radius 0.5 :height 1.0 :radial-segments 32}
   :torus {:radius 0.5 :tube 0.2
           :radial-segments 32 :tubular-segments 16}})

(def default-component
  {:base-color {:rgba [0.62 0.66 0.72 1.0]
                :color-space :srgb
                :alpha-association :straight}
   :metallic 0.0
   :roughness 0.6
   :emissive {:rgba [0.0 0.0 0.0 1.0]
              :color-space :srgb
              :alpha-association :straight}})

(def default-transform
  {:translation [0.0 0.0 0.0]
   :rotation [0.0 0.0 0.0 1.0]
   :scale [1.0 1.0 1.0]})

(def shadow-constants
  {:algorithm-version shadow-algorithm-version
   :size 2048
   :bounds-padding 0.05
   :pcf-kernel [3 3]
   :pipeline-depth-bias 2
   :pipeline-depth-bias-slope-scale 2.0
   :shader-comparison-offset 0.0015})

(defn- named-validator
  "Error type/predicate → checker returning true or throwing.

   Family-specific error vocabulary."
  [error-type predicate]
  (fn [value]
    (when-not (predicate value)
      (throw (ex-info "Region schema rejected value" {:error-type error-type})))
    true))

(defn- finite-vector?
  "Required length and value → finite numeric vector of that length?

   Shape and element checks."
  [n value]
  (and (vector? value)
       (= n (count value))
       (every? schema/finite-number? value)))

(defn- in-range?
  "Low/value/high → finite inclusive membership?

   Shared numeric validation."
  [lo value hi]
  (and (schema/finite-number? value) (<= lo value hi)))

(defn- positive-int-at-most?
  "Minimum, maximum, value → integer within bounds?

   Direct bounds; positivity comes from minimum."
  [minimum maximum value]
  (and (integer? value) (<= minimum value maximum)))

(defn- exact-keys?
  "Required/optional sets and value → exact allowed map shape?

   Checks presence and rejects extras."
  [required optional value]
  (and (map? value)
       (every? #(contains? value %) required)
       (every? (into required optional) (keys value))))

(defn- vec3?
  "Value → three finite coordinates?

   Specializes finite-vector check."
  [value]
  (finite-vector? 3 value))

(defn- positive-vec?
  "Length/value → all-positive finite vector?

   Shape plus sign check."
  [n value]
  (and (finite-vector? n value) (every? pos? value)))

(defn- quaternion-length
  "Quaternion → magnitude.

   Sum of squares/square root. Intended for validated numeric values."
  [quaternion]
  (Math/sqrt (reduce + (map #(* % %) quaternion))))

(defn- unit-quaternion?
  "Value → unit four-vector within 1e-9?

   Tight post-normalization check."
  [value]
  (and (finite-vector? 4 value)
       (<= (Math/abs (- (quaternion-length value) 1.0)) 1.0e-9)))

(defn normalize-quaternion
  "Value → normalized quaternion only if within 1e-3 of unit; otherwise
   unchanged.

   Tolerant migration before strict validation. Does not silently repair
   arbitrary rotations."
  [quaternion]
  (if (finite-vector? 4 quaternion)
    (let [length (quaternion-length quaternion)]
      (if (and (pos? length)
               (<= (Math/abs (- length 1.0)) quaternion-tolerance))
        (mapv #(/ (double %) length) quaternion)
        quaternion))
    quaternion))

(def transform
  {:keys #{:translation :rotation :scale}
   :validators
   {:translation (named-validator :region/transform-translation vec3?)
    :rotation (named-validator :region/quaternion-unit unit-quaternion?)
    :scale (named-validator :region/transform-scale vec3?)}})

(defn- lens-matches-kind?
  "Lens → exact perspective/ortho field set?

   Kind-specific key equality."
  [lens]
  (case (:kind lens)
    :perspective (= #{:kind :fov-y-deg :near :far} (set (keys lens)))
    :ortho (= #{:kind :ortho-scale :near :far} (set (keys lens)))
    false))

(defn- near-before-far?
  "Lens → positive near < far?

   Cross-field invariant."
  [{:keys [near far]}]
  (and (schema/positive-number? near)
       (schema/positive-number? far)
       (< near far)))

(def lens
  {:keys #{:kind :near :far}
   :optional #{:fov-y-deg :ortho-scale}
   :validators
   {:kind (named-validator :region/lens-kind #{:perspective :ortho})
    :near (named-validator :region/lens-near schema/positive-number?)
    :far (named-validator :region/lens-far schema/positive-number?)
    :fov-y-deg (named-validator
                :region/lens-fov
                #(and (schema/finite-number? %) (< 0.0 % 170.0)))
    :ortho-scale (named-validator :region/lens-scale schema/positive-number?)}
   :form-validators
   [{:valid? lens-matches-kind? :error-type :region/lens-kind}
    {:valid? near-before-far? :error-type :region/lens-near-far}]})

(def view
  {:keys #{:pivot :distance :yaw :pitch :lens}
   :validators
   {:pivot (named-validator :region/view-pivot vec3?)
    :distance (named-validator :region/view-distance schema/positive-number?)
    :yaw (named-validator :region/view-yaw schema/finite-number?)
    :pitch (named-validator :region/view-pitch schema/finite-number?)
    :lens lens}})

(def extent
  {:keys #{:width :height :depth}
   :validators
   {:width (named-validator :region/extent
                            #(and (schema/positive-number? %)
                                  (<= % extent-max)))
    :height (named-validator :region/extent
                             #(and (schema/positive-number? %)
                                   (<= % extent-max)))
    :depth (named-validator :region/extent
                            #(and (schema/positive-number? %)
                                  (<= % extent-max)))}})

(def background
  {:keys #{:kind :color}
   :validators {:kind #{:opaque :transparent}
                :color color/tagged}})

(def ambient
  {:keys #{:color :intensity}
   :validators {:color color/tagged
                :intensity (named-validator
                            :region/ambient-intensity
                            schema/non-negative-number?)}})

(def component-spec
  {:keys #{:base-color :metallic :roughness :emissive}
   :validators
   {:base-color color/tagged
    :metallic (named-validator :region/component-metallic
                               #(in-range? 0.0 % 1.0))
    :roughness (named-validator :region/component-roughness
                                #(in-range? 0.0 % 1.0))
    :emissive color/tagged}})

(def ^:private box-params
  {:keys #{:size}
   :validators {:size (named-validator :region/primitive-kind
                                       #(positive-vec? 3 %))}})

(def ^:private plane-params
  {:keys #{:size}
   :validators {:size (named-validator :region/primitive-kind
                                       #(positive-vec? 2 %))}})

(def ^:private sphere-params
  {:keys #{:radius :width-segments :height-segments}
   :validators
   {:radius (named-validator :region/primitive-kind schema/positive-number?)
    :width-segments (named-validator
                     :region/primitive-kind
                     #(positive-int-at-most? 3 4096 %))
    :height-segments (named-validator
                      :region/primitive-kind
                      #(positive-int-at-most? 2 4096 %))}})

(def ^:private radial-params
  {:keys #{:radius :height :radial-segments}
   :validators
   {:radius (named-validator :region/primitive-kind schema/positive-number?)
    :height (named-validator :region/primitive-kind schema/positive-number?)
    :radial-segments (named-validator
                      :region/primitive-kind
                      #(positive-int-at-most? 3 4096 %))}})

(defn- torus-params-valid?
  "Radius/tube map → tube < radius?

   Direct invariant."
  [{:keys [radius tube]}]
  (< tube radius))

(def ^:private torus-params
  {:keys #{:radius :tube :radial-segments :tubular-segments}
   :validators
   {:radius (named-validator :region/primitive-kind schema/positive-number?)
    :tube (named-validator :region/primitive-kind schema/positive-number?)
    :radial-segments (named-validator
                      :region/primitive-kind
                      #(positive-int-at-most? 3 4096 %))
    :tubular-segments (named-validator
                       :region/primitive-kind
                       #(positive-int-at-most? 3 4096 %))}
   :form-validators
   [{:valid? torus-params-valid? :error-type :region/primitive-kind}]})

(defn- primitive-params-spec
  "Primitive kind → parameter schema or nil.

   Closed dispatch."
  [kind]
  (case kind
    :box box-params
    :plane plane-params
    :sphere sphere-params
    (:cylinder :cone) radial-params
    :torus torus-params
    nil))

(defn- params-match-kind?
  "Primitive → true after matching schema, nil if unknown; may throw.

   Delegated validation."
  [{:keys [kind params]}]
  (when-let [spec (primitive-params-spec kind)]
    (schema/check spec params)
    true))

(def primitive
  {:keys #{:kind :params}
   :validators {:kind (named-validator :region/primitive-kind
                                       legal-primitive-kinds)
                :params map?}
   :form-validators
   [{:valid? params-match-kind? :error-type :region/primitive-kind}]})

(defn- flat-vec3-data?
  "Maximum count and value → bounded finite flattened triples?

   Shape/cardinality checks. Empty vector is accepted."
  [maximum value]
  (and (vector? value)
       (zero? (mod (count value) 3))
       (<= (/ (count value) 3) maximum)
       (every? schema/finite-number? value)))

(defn- triangle-index-data?
  "Value → bounded vector of nonnegative integer triples?

   Count and element checks. Empty mesh is permitted."
  [value]
  (and (vector? value)
       (zero? (mod (count value) 3))
       (<= (/ (count value) 3) mesh-triangle-max)
       (every? #(and (integer? %) (<= 0 %)) value)))

(defn- normals-match-positions?
  "Mesh → equal flattened lengths?

   Direct cardinality check. Does not normalize normals."
  [{:keys [positions normals]}]
  (= (count positions) (count normals)))

(defn- indices-in-range?
  "Mesh → all indexes below vertex count?

   Linear scan."
  [{:keys [positions indices]}]
  (let [vertex-count (/ (count positions) 3)]
    (every? #(< % vertex-count) indices)))

(def indexed-triangles
  {:keys #{:kind :positions :normals :indices}
   :validators
   {:kind #{:indexed-triangles}
    :positions (named-validator :region/mesh-positions
                                #(flat-vec3-data? mesh-vertex-max %))
    :normals (named-validator :region/mesh-normals
                              #(flat-vec3-data? mesh-vertex-max %))
    :indices (named-validator :region/mesh-index triangle-index-data?)}
   :form-validators
   [{:valid? normals-match-positions? :error-type :region/mesh-normals}
    {:valid? indices-in-range? :error-type :region/mesh-index}]})

(defn- geometry-matches-kind?
  "Mesh value → true after indexed/primitive validation, false otherwise.

   Closed kind dispatch."
  [mesh-value]
  (case (:kind mesh-value)
    :indexed-triangles (do (schema/check indexed-triangles mesh-value) true)
    (:box :sphere :cylinder :plane :cone :torus)
    (do (schema/check primitive mesh-value) true)
    false))

(def mesh
  {:keys #{:kind}
   :optional #{:params :positions :normals :indices}
   :validators
   {:kind (named-validator :region/mesh-kind
                           (conj legal-primitive-kinds :indexed-triangles))
    :params map?
    :positions vector?
    :normals vector?
    :indices vector?}
   :form-validators
   [{:valid? geometry-matches-kind? :error-type :region/mesh-kind}]})

(defn- light-matches-kind?
  "Light → exact kind-specific keys?

   Spot adds cone; other kinds do not."
  [light-value]
  (case (:kind light-value)
    :spot (= #{:kind :color :intensity :range :cast-shadow :cone}
             (set (keys light-value)))
    (:directional :point)
    (= #{:kind :color :intensity :range :cast-shadow}
       (set (keys light-value)))
    false))

(defn- spot-cone-ordered?
  "Light → legal ordered cone angles, or true for nonspot.

   Exact cone shape and bounded angles."
  [{:keys [kind cone]}]
  (if (= :spot kind)
    (let [{:keys [inner-deg outer-deg]} cone]
      (and (exact-keys? #{:inner-deg :outer-deg} #{} cone)
           (in-range? 0.0 inner-deg 89.0)
           (in-range? 0.0 outer-deg 89.0)
           (<= inner-deg outer-deg)))
    true))

(defn- shadow-only-directional?
  "Light → shadow disabled or directional?

   Simple implication. Does not limit how many directional lights request
   shadows."
  [{:keys [kind cast-shadow]}]
  (or (not cast-shadow) (= :directional kind)))

(def light
  {:keys #{:kind :color :intensity :range :cast-shadow}
   :optional #{:cone}
   :validators
   {:kind (named-validator :region/light-kind legal-light-kinds)
    :color color/tagged
    :intensity (named-validator :region/light-intensity
                                schema/non-negative-number?)
    :range (named-validator :region/light-range schema/positive-number?)
    :cast-shadow boolean?
    :cone map?}
   :form-validators
   [{:valid? light-matches-kind? :error-type :region/light-kind}
    {:valid? spot-cone-ordered? :error-type :region/light-cone}
    {:valid? shadow-only-directional? :error-type :region/light-shadow}]})

(def placed-ref
  {:keys #{:address}
   :validators {:address (named-validator :region/placed-ref some?)}})

(def ^:private placed-text-params
  {:keys #{}
   :optional #{:color :max-inline-size}
   :validators
   {:color color/tagged
    :max-inline-size (named-validator :region/text-inline-size
                                      schema/positive-number?)}})

(def placed-text
  {:keys #{:ref :params}
   :validators {:ref placed-ref :params placed-text-params}})

(def placed-ink
  {:keys #{:ref}
   :validators {:ref placed-ref}})

(def ^:private provenance
  {:keys #{:asserted-by}
   :optional #{:act}
   :validators {:asserted-by (named-validator :region/provenance some?)
                :act (named-validator :region/provenance some?)}})

(defn- body-matches-kind?
  "Object → correct exclusive body fields?

   Compares present mesh/component/light/text/ink keys."
  [object-value]
  (let [present (set (filter #(contains? object-value %)
                             [:mesh :component :light :text :ink]))]
    (case (:object/kind object-value)
      :mesh (= #{:mesh :component} present)
      :light (= #{:light} present)
      :text (= #{:text} present)
      :ink (= #{:ink} present)
      :empty (empty? present)
      false)))

(def object
  {:keys #{:object/id :object/kind :transform :provenance}
   :optional #{:parent :mesh :component :light :text :ink}
   :validators
   {:object/id (named-validator :region/object-id some?)
    :object/kind (named-validator :region/object-kind legal-object-kinds)
    :parent (constantly true)
    :transform transform
    :provenance provenance
    :mesh mesh
    :component component-spec
    :light light
    :text placed-text
    :ink placed-ink}
   :form-validators
   [{:valid? body-matches-kind? :error-type :region/object-kind}]})

(def ^:private rect
  {:keys #{:x :y :w :h}
   :validators
   {:x schema/finite-number?
    :y schema/finite-number?
    :w schema/positive-number?
    :h schema/positive-number?}})

(defn ids-match-keys?
  "Region → all scene keys equal object IDs?

   Full scene check."
  [{:keys [scene]}]
  (every? (fn [[object-id object-value]]
            (= object-id (:object/id object-value)))
          scene))

(defn- id-mismatch
  "Region → first deterministic mismatch record or nil.

   Sorted diagnostic search."
  [{:keys [scene]}]
  (when-let [[scene-key object-value]
             (first (filter (fn [[object-id value]]
                              (not= object-id (:object/id value)))
                            (sort-by (comp pr-str first) scene)))]
    {:scene-key scene-key :object/id (:object/id object-value)}))

(defn parents-exist?
  "Region → every nonnil parent exists?

   Membership scan."
  [{:keys [scene]}]
  (every? (fn [[_ object-value]]
            (let [parent (:parent object-value)]
              (or (nil? parent) (contains? scene parent))))
          scene))

(defn- missing-parent
  "Region → first deterministic missing-parent record or nil.

   Sorted diagnostic search."
  [{:keys [scene]}]
  (when-let [[object-id object-value]
             (first (filter (fn [[_ value]]
                              (let [parent (:parent value)]
                                (and (some? parent)
                                     (not (contains? scene parent)))))
                            (sort-by (comp pr-str first) scene)))]
    {:object/id object-id :parent (:parent object-value)}))

(defn- cycle-info
  "Region → first cycle description or nil.

   Walks parent ancestry from every sorted ID. Can revisit shared ancestry
   repeatedly; no cross-start memoization."
  [{:keys [scene]}]
  (some (fn [object-id]
          (loop [cursor object-id
                 seen #{}]
            (cond
              (nil? cursor) nil
              (contains? seen cursor)
              {:object/id object-id :cycle-at cursor}
              :else
              (recur (:parent (get scene cursor)) (conj seen cursor)))))
        (sort-by pr-str (keys scene))))

(defn acyclic?
  "Region → no cycle?

   Projects cycle-info. Failed validation may compute the diagnostic again."
  [region]
  (nil? (cycle-info region)))

(def schema
  {:keys #{:region/id :region/revision :region3d/version :extent :scene :view
           :background :ambient :region/rect}
   :validators
   {:region/id (named-validator :region/id some?)
    :region/revision (named-validator :region/revision some?)
    :region3d/version #{schema-version}
    :extent extent
    :scene [:map-of some? object]
    :view view
    :background background
    :ambient ambient
    :region/rect rect}
   :form-validators
   [{:valid? ids-match-keys?
     :error-type :region/object-id-mismatch
     :explain id-mismatch}
    {:valid? parents-exist?
     :error-type :region/parent-missing
     :explain missing-parent}
    {:valid? acyclic?
     :error-type :region/parent-cycle
     :explain cycle-info}]})

(defn canonical-transform
  "Nil/map/other → defaults merged and near-unit rotation normalized, or
   unchanged invalid value.

   Pure normalization. Zero scale is allowed by schema although
   inverse/normal computations may be singular."
  [transform-value]
  (if (or (nil? transform-value) (map? transform-value))
    (update (merge default-transform (or transform-value {}))
            :rotation normalize-quaternion)
    transform-value))

(defn- canonical-lens
  "Lens map/other → kind defaults merged or unchanged.

   Preserves unknown kinds for rejection."
  [lens-value]
  (if (map? lens-value)
    (merge (case (:kind lens-value)
             :perspective default-perspective-lens
             :ortho default-ortho-lens
             {})
           lens-value)
    lens-value))

(defn canonical-view
  "Nil/map/other → defaulted view with canonical lens, or unchanged invalid
   value.

   Pure nested normalization."
  [view-value]
  (if (or (nil? view-value) (map? view-value))
    (update (merge default-view (or view-value {})) :lens canonical-lens)
    view-value))

(defn- canonical-component
  "Nil/map/other → defaulted shading parameters or unchanged.

   Shallow merge. Nested tagged colors must be complete."
  [component-value]
  (if (or (nil? component-value) (map? component-value))
    (merge default-component (or component-value {}))
    component-value))

(defn- canonical-mesh
  "Mesh → primitive parameter defaults filled where applicable.

   Kind-specific merge."
  [mesh-value]
  (if (map? mesh-value)
    (if (contains? legal-primitive-kinds (:kind mesh-value))
      (update mesh-value :params
              #(if (or (nil? %) (map? %))
                 (merge (get primitive-defaults (:kind mesh-value)) (or % {}))
                 %))
      mesh-value)
    mesh-value))

(defn- canonical-light
  "Nil/map/other → common light defaults merged or unchanged.

   Does not invent a kind or spot cone."
  [light-value]
  (if (or (nil? light-value) (map? light-value))
    (merge {:color (:color default-ambient)
            :intensity 1.0
            :range 100.0
            :cast-shadow false}
           (or light-value {}))
    light-value))

(defn- canonical-placed-text
  "Text value → map with nil optional params removed, or unchanged.

   Filters params. Does not resolve content references."
  [text-value]
  (if (map? text-value)
    (update text-value :params
            #(if (map? %)
               (into (empty %) (remove (comp nil? val)) %)
               %))
    text-value))

(defn canonical-object
  "Object → canonical transform and kind-specific body.

   Dispatches normalization."
  [object-value]
  (if (map? object-value)
    (let [object-value (update object-value :transform canonical-transform)]
      (case (:object/kind object-value)
        :mesh (-> object-value
                  (update :mesh canonical-mesh)
                  (update :component canonical-component))
        :light (update object-value :light canonical-light)
        :text (update object-value :text canonical-placed-text)
        object-value))
    object-value))

(defn- canonical-scene
  "Scene map → canonicalized values under same IDs; other values unchanged.

   Pure map traversal."
  [scene]
  (if (map? scene)
    (into (empty scene)
          (map (fn [[object-id object-value]]
                 [object-id (canonical-object object-value)]))
          scene)
    scene))

(defn- migrate-region
  "Region → version/view-key migration.

   Version 1 becomes 2; explicit :view wins over :view-default. Intended for
   the implemented migration."
  [region]
  (if (map? region)
    (cond-> region
      (= 1 (:region3d/version region))
      (assoc :region3d/version schema-version)

      (and (contains? region :view-default)
           (not (contains? region :view)))
      (assoc :view (:view-default region))

      (contains? region :view-default)
      (dissoc :view-default))
    region))

(defn canonical-region
  "Region → migrated/defaulted region without deliberate rejection.

   Composes normalizers. Normalization and acceptance are distinct."
  [region]
  (let [region (migrate-region region)]
    (if (map? region)
      (-> region
          (update :scene canonical-scene)
          (update :view canonical-view)
          (update :background
                  #(if (or (nil? %) (map? %))
                     (merge default-background (or % {}))
                     %))
          (update :ambient
                  #(if (or (nil? %) (map? %))
                     (merge default-ambient (or % {}))
                     %)))
      region)))

(defn validate-region!
  "Region → canonical accepted region or schema error.

   One shared-schema call after canonicalization. Downstream APIs do not all
   call this automatically."
  [region]
  (schema/check schema (canonical-region region)))
