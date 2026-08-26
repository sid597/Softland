(ns app.client.substrate.region3d-material
  "Pure, fail-closed material grammars for Region3D.

   Region rows are ordinary EDN render payloads. This namespace owns
   versioned defaults, canonical validation, and semantic material values. GPU
   resources and session state never enter this grammar. Unknown fields are
   preserved so later sculpting and node-authoring extensions can enter without
   silently losing meaning."
  )

(def schema-version 2)
(def shadow-algorithm-version :region3d/shadow-v1)
(def quaternion-tolerance 1.0e-3)
(def extent-max 1.0e4)
(def mesh-vertex-max 65536)
(def mesh-triangle-max 131072)
(def legal-object-kinds #{:mesh :light :empty :text :ink})
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
