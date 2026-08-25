(ns app.client.substrate.region3d-material
  "Pure, fail-closed material grammars for Region3D.

   Region rows are ordinary EDN render payloads. This namespace owns
   versioned defaults, canonical validation, semantic edit values, and the
   Contract-M citizenship descriptors. GPU resources and session state never
   enter this grammar. Unknown fields are preserved so later sculpting and
   node-authoring extensions can enter without silently losing meaning."
  )

(def schema-version 2)
(def primitive-algorithm-version :region3d/primitives-v1)
(def shadow-algorithm-version :region3d/shadow-v1)
(def quaternion-tolerance 1.0e-3)
(def extent-max 1.0e4)
(def mesh-vertex-max 65536)
(def mesh-triangle-max 131072)
(def legal-object-kinds #{:mesh :light :camera :empty :text :ink})
(def legal-primitive-kinds #{:box :sphere :cylinder :plane :cone :torus})
(def legal-light-kinds #{:directional :point :spot})
(def legal-camera-kinds #{:perspective :ortho})
(def legal-display-modes #{:flat :normal :lit})

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

(def default-material
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

(def edit-operation-ids
  #{:region3d/add-object :region3d/remove-object
    :region3d/set-transform :region3d/set-parent
    :region3d/set-material :region3d/set-light
    :region3d/set-camera :region3d/set-region :region3d/set-placed})

(defn finite-number? [value]
  (and (number? value)
       #?(:clj (Double/isFinite (double value))
          :cljs (js/Number.isFinite value))))

(defn- finite-vector? [n value]
  (and (vector? value)
       (= n (count value))
       (every? finite-number? value)))

(defn- in-range? [lo value hi]
  (and (finite-number? value) (<= lo value hi)))

(defn- positive-finite? [value]
  (and (finite-number? value) (pos? value)))

(defn- throw-field! [message data]
  (throw (ex-info message data)))

(defn validate-tagged-color!
  "Validate a straight, tagged sRGB material color and return it unchanged.
   Alpha is carried in :rgba; data textures never use this function."
  [label color]
  (let [rgba (:rgba color)]
    (when-not (map? color)
      (throw-field! "Region3D color must be a tagged map"
                    {:field label :value color}))
    (when-not (and (finite-vector? 4 rgba)
                   (every? #(<= 0.0 % 1.0) rgba))
      (throw-field! "Region3D color requires finite straight RGBA in [0,1]"
                    {:field label :rgba rgba}))
    (when-not (= :srgb (:color-space color))
      (throw-field! "Region3D material color must be tagged sRGB"
                    {:field label :color-space (:color-space color)}))
    (when-not (= :straight (:alpha-association color))
      (throw-field! "Region3D material color must enter with straight alpha"
                    {:field label
                     :alpha-association (:alpha-association color)}))
    color))

(defn normalize-quaternion
  "Canonical [x y z w] quaternion ingress. Zero and values outside the
   contract's 1e-3 unit-length tolerance refuse; accepted values are exactly
   normalized before they can enter a canonical region value."
  [quaternion]
  (when-not (finite-vector? 4 quaternion)
    (throw-field! "Region3D rotation must be finite [x y z w]"
                  {:rotation quaternion :component-order [:x :y :z :w]}))
  (let [length (Math/sqrt (reduce + (map #(* % %) quaternion)))]
    (when (zero? length)
      (throw-field! "Region3D rotation quaternion cannot have zero length"
                    {:rotation quaternion}))
    (when (> (Math/abs (- length 1.0)) quaternion-tolerance)
      (throw-field! "Region3D rotation quaternion is outside unit tolerance"
                    {:rotation quaternion :length length
                     :tolerance quaternion-tolerance}))
    (mapv #(/ (double %) length) quaternion)))

(defn canonical-transform [transform]
  (let [transform (merge default-transform (or transform {}))
        translation (:translation transform)
        scale (:scale transform)]
    (when-not (finite-vector? 3 translation)
      (throw-field! "Region3D translation must be a finite vec3"
                    {:translation translation}))
    (when-not (finite-vector? 3 scale)
      (throw-field! "Region3D scale must be a finite vec3" {:scale scale}))
    (assoc transform :rotation (normalize-quaternion (:rotation transform)))))

(defn- canonical-lens [lens]
  (let [kind (:kind lens)
        lens (merge (case kind
                      :perspective default-perspective-lens
                      :ortho default-ortho-lens
                      (throw-field! "Region3D camera lens kind is invalid"
                                    {:kind kind :legal legal-camera-kinds}))
                    lens)
        near (:near lens)
        far (:far lens)]
    (when-not (and (positive-finite? near)
                   (positive-finite? far)
                   (> far near))
      (throw-field! "Region3D camera requires 0 < near < far"
                    {:near near :far far}))
    (case kind
      :perspective
      (when-not (and (finite-number? (:fov-y-deg lens))
                     (< 0.0 (:fov-y-deg lens) 170.0))
        (throw-field! "Region3D perspective fov-y-deg must be in (0,170)"
                      {:fov-y-deg (:fov-y-deg lens)}))
      :ortho
      (when-not (positive-finite? (:ortho-scale lens))
        (throw-field! "Region3D ortho-scale must be positive"
                      {:ortho-scale (:ortho-scale lens)})))
    lens))

(defn canonical-view [view]
  (let [view (merge default-view (or view {}))]
    (when-not (finite-vector? 3 (:pivot view))
      (throw-field! "Region3D view pivot must be a finite vec3"
                    {:pivot (:pivot view)}))
    (when-not (positive-finite? (:distance view))
      (throw-field! "Region3D view distance must be positive"
                    {:distance (:distance view)}))
    (when-not (and (finite-number? (:yaw view))
                   (finite-number? (:pitch view)))
      (throw-field! "Region3D view yaw and pitch must be finite"
                    {:yaw (:yaw view) :pitch (:pitch view)}))
    (assoc view :lens (canonical-lens (:lens view)))))

(defn- canonical-material [material]
  (let [material (merge default-material (or material {}))]
    (validate-tagged-color! :material/base-color (:base-color material))
    (validate-tagged-color! :material/emissive (:emissive material))
    (when-not (in-range? 0.0 (:metallic material) 1.0)
      (throw-field! "Region3D metallic must be in [0,1]"
                    {:metallic (:metallic material)}))
    (when-not (in-range? 0.0 (:roughness material) 1.0)
      (throw-field! "Region3D roughness must be in [0,1]"
                    {:roughness (:roughness material)}))
    material))

(defn- positive-vec! [label n value]
  (when-not (and (finite-vector? n value) (every? pos? value))
    (throw-field! "Region3D primitive dimensions must be positive"
                  {:field label :value value})))

(defn- segment-count! [label value minimum]
  (when-not (and (integer? value) (<= minimum value 4096))
    (throw-field! "Region3D primitive segment count is outside v1"
                  {:field label :value value :min minimum :max 4096})))

(defn- canonical-primitive [mesh]
  (let [kind (:kind mesh)
        params (merge (get primitive-defaults kind) (:params mesh))]
    (when-not (contains? legal-primitive-kinds kind)
      (throw-field! "Region3D primitive kind is invalid"
                    {:kind kind :legal legal-primitive-kinds}))
    (case kind
      :box (positive-vec! :box/size 3 (:size params))
      :plane (positive-vec! :plane/size 2 (:size params))
      :sphere
      (do (when-not (positive-finite? (:radius params))
            (throw-field! "Region3D sphere radius must be positive" params))
          (segment-count! :sphere/width-segments (:width-segments params) 3)
          (segment-count! :sphere/height-segments (:height-segments params) 2))
      (:cylinder :cone)
      (do (when-not (positive-finite? (:radius params))
            (throw-field! "Region3D radial primitive radius must be positive"
                          params))
          (when-not (positive-finite? (:height params))
            (throw-field! "Region3D radial primitive height must be positive"
                          params))
          (segment-count! :radial/segments (:radial-segments params) 3))
      :torus
      (do (when-not (positive-finite? (:radius params))
            (throw-field! "Region3D torus radius must be positive" params))
          (when-not (and (positive-finite? (:tube params))
                         (< (:tube params) (:radius params)))
            (throw-field! "Region3D torus tube must be in (0,radius)" params))
          (segment-count! :torus/radial-segments (:radial-segments params) 3)
          (segment-count! :torus/tubular-segments (:tubular-segments params) 3)))
    (assoc mesh :params params)))

(defn- canonical-indexed-triangles [mesh]
  (let [positions (:positions mesh)
        normals (:normals mesh)
        indices (:indices mesh)
        vertex-count (when (sequential? positions) (/ (count positions) 3))
        triangle-count (when (sequential? indices) (/ (count indices) 3))]
    (when-not (and (vector? positions)
                   (zero? (mod (count positions) 3))
                   (every? finite-number? positions))
      (throw-field! "Region3D indexed positions must be finite flat vec3 data"
                    {:positions-count (count positions)}))
    (when-not (and (vector? normals)
                   (= (count positions) (count normals))
                   (every? finite-number? normals))
      (throw-field! "Region3D indexed normals must match positions"
                    {:positions-count (count positions)
                     :normals-count (count normals)}))
    (when-not (and (vector? indices)
                   (zero? (mod (count indices) 3))
                   (every? #(and (integer? %) (<= 0 %)) indices))
      (throw-field! "Region3D indices must be non-negative u32 triangle data"
                    {:indices-count (count indices)}))
    (when (> vertex-count mesh-vertex-max)
      (throw-field! "Region3D mesh exceeds the v1 vertex cap"
                    {:vertices vertex-count :max mesh-vertex-max}))
    (when (> triangle-count mesh-triangle-max)
      (throw-field! "Region3D mesh exceeds the v1 triangle cap"
                    {:triangles triangle-count :max mesh-triangle-max}))
    (when (some #(>= % vertex-count) indices)
      (throw-field! "Region3D mesh index exceeds the vertex population"
                    {:vertices vertex-count}))
    mesh))

(defn- canonical-mesh [mesh]
  (when-not (map? mesh)
    (throw-field! "Region3D mesh row must be a map" {:mesh mesh}))
  (if (= :indexed-triangles (:kind mesh))
    (canonical-indexed-triangles mesh)
    (canonical-primitive mesh)))

(defn- canonical-light [light]
  (let [kind (:kind light)
        light (merge {:color (:color default-ambient)
                      :intensity 1.0
                      :range 100.0
                      :cast-shadow false}
                     light)]
    (when-not (contains? legal-light-kinds kind)
      (throw-field! "Region3D light kind is invalid"
                    {:kind kind :legal legal-light-kinds}))
    (validate-tagged-color! :light/color (:color light))
    (when-not (and (finite-number? (:intensity light))
                   (<= 0.0 (:intensity light)))
      (throw-field! "Region3D light intensity must be non-negative" light))
    (when (contains? #{:point :spot} kind)
      (when-not (positive-finite? (:range light))
        (throw-field! "Region3D point/spot range must be positive" light)))
    (when (= :spot kind)
      (let [{:keys [inner-deg outer-deg]} (:cone light)]
        (when-not (and (in-range? 0.0 inner-deg 89.0)
                       (in-range? 0.0 outer-deg 89.0)
                       (<= inner-deg outer-deg))
          (throw-field! "Region3D spot cone requires 0 <= inner <= outer <= 89"
                        {:cone (:cone light)}))))
    (when (and (:cast-shadow light) (not= :directional kind))
      (throw-field! "Region3D v1 accepts shadows on directional lights only"
                    {:kind kind}))
    light))

(defn- canonical-provenance [provenance]
  (when-not (and (map? provenance)
                 (contains? provenance :asserted-by)
                 (some? (:asserted-by provenance)))
    (throw-field! "Region3D object provenance requires :asserted-by"
                  {:provenance provenance}))
  provenance)

(defn- canonical-placed-ref [label ref]
  (when-not (and (map? ref) (= #{:address} (set (keys ref))))
    (throw-field! "Region3D placed ref requires exactly :address"
                  {:field label :ref ref}))
  (when (nil? (:address ref))
    (throw-field! "Region3D placed ref address cannot be nil"
                  {:field label :ref ref}))
  ref)

(defn- canonical-placed-text [text]
  (when-not (and (map? text) (= #{:ref :params} (set (keys text))))
    (throw-field! "Region3D text placement requires :ref and :params"
                  {:text text}))
  (when-not (map? (:params text))
    (throw-field! "Region3D text placement params must be a map"
                  {:params (:params text)}))
  (when-let [color (:color (:params text))]
    (validate-tagged-color! :text/color color))
  (when-let [max-inline-size (:max-inline-size (:params text))]
    (when-not (positive-finite? max-inline-size)
      (throw-field! "Region3D text max-inline-size must be positive"
                    {:max-inline-size max-inline-size})))
  (update text :ref #(canonical-placed-ref :text/ref %)))

(defn- canonical-placed-ink [ink]
  (when-not (and (map? ink) (= #{:ref} (set (keys ink))))
    (throw-field! "Region3D ink placement requires exactly :ref"
                  {:ink ink}))
  (update ink :ref #(canonical-placed-ref :ink/ref %)))

(defn canonical-object [object]
  (let [kind (:object/kind object)
        object (-> object
                   (update :transform canonical-transform)
                   (update :provenance canonical-provenance))]
    (when (nil? (:object/id object))
      (throw-field! "Region3D object identity cannot be nil" {:object object}))
    (when-not (contains? legal-object-kinds kind)
      (throw-field! "Region3D object kind is invalid"
                    {:kind kind :legal legal-object-kinds}))
    (case kind
      :mesh (-> object
                (update :mesh canonical-mesh)
                (update :material canonical-material))
      :light (update object :light canonical-light)
      :camera (update object :camera canonical-lens)
      :text (update object :text canonical-placed-text)
      :ink (update object :ink canonical-placed-ink)
      :empty object)))

(defn- validate-parent-graph! [scene]
  (doseq [[object-id object] scene]
    (when-not (= object-id (:object/id object))
      (throw-field! "Region3D scene key must equal :object/id"
                    {:scene-key object-id :object/id (:object/id object)}))
    (when-let [parent (:parent object)]
      (when-not (contains? scene parent)
        (throw-field! "Region3D object parent is missing"
                      {:object-id object-id :parent parent}))
      (loop [cursor parent seen #{object-id}]
        (when cursor
          (when (contains? seen cursor)
            (throw-field! "Region3D hierarchy contains a cycle"
                          {:object-id object-id :cycle-at cursor}))
          (recur (:parent (get scene cursor)) (conj seen cursor))))))
  scene)

(defn- canonical-extent [extent]
  (doseq [axis [:width :height :depth]]
    (when-not (and (positive-finite? (get extent axis))
                   (<= (get extent axis) extent-max))
      (throw-field! "Region3D extent component must be in (0,10000]"
                    {:axis axis :value (get extent axis) :max extent-max})))
  extent)

(defn- canonical-background [background]
  (let [background (merge default-background (or background {}))]
    (when-not (contains? #{:opaque :transparent} (:kind background))
      (throw-field! "Region3D background kind is invalid"
                    {:kind (:kind background)}))
    (validate-tagged-color! :background/color (:color background))
    background))

(defn- canonical-ambient [ambient]
  (let [ambient (merge default-ambient (or ambient {}))]
    (validate-tagged-color! :ambient/color (:color ambient))
    (when-not (and (finite-number? (:intensity ambient))
                   (<= 0.0 (:intensity ambient)))
      (throw-field! "Region3D ambient intensity must be non-negative"
                    {:intensity (:intensity ambient)}))
    ambient))

(defn migrate-region
  "Migrate the v1 base grammar into v2 placements. Every v1 value is already
   semantically valid under v2, so migration changes only the version tag."
  [region]
  (case (:region3d/version region)
    1 (assoc region :region3d/version 2)
    2 region
    (throw-field! "Region3D schema version is unsupported"
                  {:version (:region3d/version region)
                   :supported [1 2]})))

(defn validate-region!
  "Migrate then return the canonical Region3D v2 value. Validation is fail-closed for all
   pinned meanings and preserves unknown extension fields verbatim."
  [region]
  (when-not (map? region)
    (throw-field! "Region3D region must be an EDN map" {:region region}))
  (let [region (migrate-region region)]
   (when-not (= schema-version (:region3d/version region))
    (throw-field! "Region3D schema version is unsupported"
                  {:version (:region3d/version region)
                   :supported schema-version}))
  (doseq [field [:extent :scene]]
    (when-not (contains? region field)
      (throw-field! "Region3D region is missing a required field"
                    {:field field})))
  (when-not (map? (:scene region))
    (throw-field! "Region3D scene must be an object-id map"
                  {:scene (:scene region)}))
  (let [canonical-scene (into (empty (:scene region))
                              (map (fn [[object-id object]]
                                     [object-id (canonical-object object)]))
                              (:scene region))
        canonical (-> region
                      (assoc :extent (canonical-extent (:extent region)))
                      (assoc :background
                             (canonical-background (:background region)))
                      (assoc :ambient (canonical-ambient (:ambient region)))
                      (assoc :view-default
                             (canonical-view (:view-default region)))
                      (assoc :scene canonical-scene))]
    (validate-parent-graph! canonical-scene)
    canonical)))

(defn canonical-region [region]
  (letfn [(canonical [value]
            (cond
              (map? value) (into (sorted-map)
                                 (map (fn [[key nested]]
                                        [key (canonical nested)]))
                                 value)
              (vector? value) (mapv canonical value)
              (set? value) (into (sorted-set) (map canonical) value)
              :else value))]
    (canonical (validate-region! region))))

(defn primitive-cache-key [object revision]
  (let [object (canonical-object object)]
    [primitive-algorithm-version
     (:object/id object)
     revision
     (get-in object [:mesh :kind])
     (get-in object [:mesh :params])]))

(defn edit-diff
  "Mint one invertible per-object/region edit value. Before/after contain only
   the named sub-map; a scene replacement is deliberately unrepresentable."
  [{:keys [op region-id object-id before after asserted-by]
    :or {asserted-by :sid}}]
  (when-not (contains? edit-operation-ids op)
    (throw-field! "Region3D edit operation is not declared"
                  {:op op :legal edit-operation-ids}))
  (when (nil? region-id)
    (throw-field! "Region3D edit requires :region-id" {:op op}))
  (when (and (not= op :region3d/set-region) (nil? object-id))
    (throw-field! "Region3D object edit requires :object-id" {:op op}))
  (when (or (and (map? before) (contains? before :scene))
            (and (map? after) (contains? after :scene)))
    (throw-field! "Region3D edit payload cannot replace the scene"
                  {:op op}))
  {:diff/version 1
   :op/id op
   :key [region-id object-id]
   :payload {:region-id region-id
             :object-id object-id
             :before before
             :after after}
   :provenance {:asserted-by asserted-by
                :draft-settle :settled}})

(defn apply-edit
  "Pure semantic application used by replay and the maintained-view oracle."
  [region {:keys [op/id payload] :as diff}]
  (let [region (validate-region! region)
        {:keys [object-id after]} payload]
    (when-not (= 1 (:diff/version diff))
      (throw-field! "Region3D diff version is unsupported" {:diff diff}))
    (case id
      :region3d/add-object
      (validate-region! (assoc-in region [:scene object-id]
                                  (canonical-object after)))
      :region3d/remove-object
      (validate-region! (update region :scene dissoc object-id))
      :region3d/set-transform
      (validate-region! (assoc-in region [:scene object-id :transform]
                                  (canonical-transform after)))
      :region3d/set-parent
      (validate-region! (assoc-in region [:scene object-id :parent] after))
      :region3d/set-material
      (validate-region! (assoc-in region [:scene object-id :material]
                                  (canonical-material after)))
      :region3d/set-light
      (validate-region! (assoc-in region [:scene object-id :light]
                                  (canonical-light after)))
      :region3d/set-camera
      (validate-region! (assoc-in region [:scene object-id :camera]
                                  (canonical-lens after)))
      :region3d/set-placed
      (let [kind (get-in region [:scene object-id :object/kind])]
        (when-not (contains? #{:text :ink} kind)
          (throw-field! "Region3D placed edit requires a text or ink object"
                        {:object-id object-id :kind kind}))
        (validate-region!
         (assoc-in region [:scene object-id kind]
                   (case kind
                     :text (canonical-placed-text after)
                     :ink (canonical-placed-ink after)))))
      :region3d/set-region
      (validate-region! (merge region after))
      (throw-field! "Region3D edit operation is not declared" {:op id}))))

(def legal-zoom-regimes
  [{:zoom {:min 0.01 :max 0.1}
    :extent {:max extent-max}
    :normalization :region-local-origin
    :coordinate-precision :region-local-f32
    :coverage-precision :rgba16float
    :backend :webgpu-indexed-triangles
    :lifecycle :held-region-lease
    :verdict :declared-region3d-v1}
   {:zoom {:min 0.1 :max 8.0}
    :extent {:max extent-max}
    :normalization :region-local-origin
    :coordinate-precision :region-local-f32
    :coverage-precision :rgba16float
    :backend :webgpu-indexed-triangles
    :lifecycle :held-region-lease
    :verdict :declared-region3d-v1}
   {:zoom {:min 8.0 :max 1000.0}
    :extent {:max extent-max}
    :normalization :region-local-origin
    :coordinate-precision :region-local-f32
    :coverage-precision :rgba16float
    :backend :webgpu-indexed-triangles
    :lifecycle :held-region-lease
    :verdict :declared-region3d-v1}])

(def region-geometry-declaration
  {:geometry/version 1
   :authority {:kind :region-composite-quad
               :source-id :region3d/region-id
               :source-revision :scene-entry/material-revision
               :algorithm-version :region3d/composite-v1
               :local-space :container-local-f64-authority}
   :classify {:result #{:inside :boundary :outside}
              :fill-rule :not-applicable
              :boundary-rule :boundary-is-hit}
   :coverage {:geometry-operator :region-resolve-sample
              :operator-version :region3d/composite-v1
              :boundary-relation :derived-effect
              :reference-isocontour :not-applicable
              :visual-factors [:resolved-alpha :effective-opacity]
              :tie-token :mathematical-boundary
              :quantization :rgba16float-to-present}
   :pick {:policy :region-router
          :boundary :hit
          :hit-slop {:metric :screen-px :radius 0.0}
          :owner :region3d/region-id}
   :bounds {:math :resolved-region-rect
            :paint :resolved-region-rect
            :pick :resolved-region-rect}
   :derivations [{:kind :offscreen-region-resolve
                  :source-revision :region3d/scene-revision
                  :algorithm-version :region3d/pass-v1
                  :tolerance-lod :lease-quantized-pixels
                  :normalization :region-local
                  :precision :f32
                  :backend :webgpu
                  :regime :region3d/legal-zoom}]
   :regimes legal-zoom-regimes})

(def object-geometry-declaration
  {:geometry/version 1
   :authority {:kind :indexed-triangle-mesh
               :source-id :object/id
               :source-revision :region3d/scene-revision
               :algorithm-version primitive-algorithm-version
               :local-space :object-local-right-handed-y-up}
   :classify {:result #{:inside :boundary :outside}
              :fill-rule :not-applicable
              :boundary-rule :ray-triangle-boundary-is-hit}
   :coverage {:geometry-operator :depth
              :operator-version :webgpu-depth24plus-v1
              :boundary-relation :not-applicable
              :reference-isocontour :not-applicable
              :visual-factors [:pbr :shadow :material-alpha]
              :tie-token :object-id-order
              :quantization :f32-depth24plus}
   :pick {:policy :ray-nearest-positive-t
          :boundary :hit
          :hit-slop {:metric :screen-px :radius 6.0}
          :owner :object/id}
   :bounds {:math :transformed-mesh-aabb
            :paint :depth-projected-mesh
            :pick :bvh-aabb-plus-screen-slop}
   :derivations [{:kind :mesh-and-bvh
                  :source-revision :region3d/scene-revision
                  :algorithm-version primitive-algorithm-version
                  :tolerance-lod :versioned-primitive-segments
                  :normalization :region-local
                  :precision :f32
                  :backend :cpu-bvh+webgpu-indexed-triangles
                  :regime :region3d/legal-zoom}]
   :regimes legal-zoom-regimes})

(defn- citizenship
  [family-id geometry material-fields instance-fields pick-result order-road]
  {:family/id family-id
   :family/version schema-version
   :grammar {:schema/version schema-version
             :material-fields material-fields
             :instance-fields instance-fields
             :validation :region3d-material/fail-closed-v2
             :defaults :region3d-material/explicit-v2
             :edit-operations (vec (sort edit-operation-ids))
             :serialization :canonical-edn-v1
             :export-projections :none-promised
             :entry-paint-required-keys [:region-id :resolve-view]}
   :pick {:geometry :render/geometry
          :scene-order order-road
          :visibility :region-depth+outer-scene-visibility
          :modalities [:pointer :pen :touch :accessibility]
          :result pick-result}
   :provenance {:material-id :region3d/material-id
                :revision :region3d/scene-revision
                :parents :region3d/provenance-parents
                :author/actor :provenance/asserted-by
                :act :region3d/edit-operation
                :source-assets :region3d/source-assets
                :derivations :render/geometry
                :draft-settle :region3d/draft-settle}
   :versioning {:schema-version schema-version
                :algorithm-versions
                {:primitives primitive-algorithm-version
                 :shadow shadow-algorithm-version}
                :migration [:region3d/v1-base :region3d/v2-placements]
                :unknown-field-policy :preserve
                :cache-invalidation :source+algorithm+regime
                :compatibility {:reader-min 1 :reader-max 2}}
   :render {:order order-road
            :geometry geometry
            :color-alpha
            {:material {:color-space :srgb :alpha-association :straight}
             :scene :scene-color/linear-premultiplied-srgb
             :seam :region3d/always-linear}
            :resources :region3d/compositor-owned-held-leases-v1
            :regimes (:regimes geometry)}
   :receipts [:region3d/s1-order :region3d/s2-depth
              :region3d/s3-edit :region3d/s4-color
              :region3d/s5-resources]})

(def region-family-registration
  (citizenship :render.family/region-3d
               region-geometry-declaration
               [:region3d/version :extent :background :ambient
                :view-default :scene]
               [:instance/id :container-slot :region3d/scene]
               :region-id+resolved-inner-identity
               :scene-order/v1))

(def object-family-citizenship
  (citizenship :material.family/object-3d
               object-geometry-declaration
               [:object/id :object/kind :parent :transform :provenance
                :mesh :material :light :camera :text :ink]
               [:region-id :object/id :effective-transform]
               :object-id+point3+normal+t+placed-material-route
               :via-router))
