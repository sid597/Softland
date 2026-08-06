(ns app.client.substrate.path-material
  "Pure grammar, Contract-G truth, cache identity, normalization, and packing
   laws for the path atom. GPU meshes are disposable projections of this
   namespace's centerline/contour authority; no renderer state lives here.")

(def schema-version 1)
(def algorithm-version :path-tessellation-v1)
(def legal-kinds #{:ink :shape})
(def legal-contour-roles #{:open :outer :hole})
(def legal-cap-join #{:round})
(def boundary-epsilon 1.0e-9)
(def hit-slop-screen-px 0.0)

(def legal-zoom-regimes
  [{:regime/id :legal-min
    :zoom {:min 0.01 :max 0.1}
    :fan-resolution 4
    :extent :normalized-arbitrary
    :normalization :shape-local-origin-scale
    :coordinate-precision :f32
    :coverage-precision :rgba8unorm
    :lifecycle :live
    :backend :webgpu-triangle-list
    :verdict :measured-path-parity}
   {:regime/id :floor-default
    :zoom {:min 0.1 :max 8.0}
    :fan-resolution 8
    :extent :normalized-arbitrary
    :normalization :shape-local-origin-scale
    :coordinate-precision :f32
    :coverage-precision :rgba8unorm
    :lifecycle :live
    :backend :webgpu-triangle-list
    :verdict :measured-path-parity}
   {:regime/id :legal-max
    :zoom {:min 8.0 :max 1000.0}
    :fan-resolution 16
    :extent :normalized-arbitrary
    :normalization :shape-local-origin-scale
    :coordinate-precision :f32
    :coverage-precision :rgba8unorm
    :lifecycle :live
    :backend :webgpu-triangle-list
    :verdict :measured-path-parity}])

(def geometry-declaration
  {:geometry/version 1
   :authority {:kind :centerline-pressure-or-explicit-contours
               :source-id :scene-entry/material-id
               :source-revision :scene-entry/material-revision
               :algorithm-version algorithm-version
               :local-space :container-local-f64-authority}
   :classify {:result #{:inside :boundary :outside}
              :fill-rule :explicit-outer-minus-holes
              :boundary-rule :boundary-is-hit}
   :coverage {:geometry-operator :direct-tessellation
              :operator-version algorithm-version
              :boundary-relation :aliased-triangle-edge-v1
              :reference-isocontour :not-applicable
              :visual-factors [:solid-color :material-alpha :effective-opacity]
              :tie-token :mathematical-boundary
              :quantization :measured-f32-screen-error-per-regime}
   :pick {:policy :interior-or-centerline-width
          :boundary :hit
          :hit-slop {:metric :screen-px :radius hit-slop-screen-px}
          :owner :path-node/address}
   :bounds {:math :derived-from-authority
            :paint :tessellated-mesh-bounds
            :pick :authority-plus-declared-slop}
   :derivations [{:kind :outline-and-triangle-mesh
                  :source-revision :scene-entry/material-revision
                  :algorithm-version algorithm-version
                  :tolerance-lod :zoom-regime-fan-resolution
                  :normalization :shape-local-origin-scale
                  :precision :container-local-f32
                  :backend :webgpu-triangle-list
                  :regime :path-zoom-regime}]
   :regimes (mapv #(dissoc % :regime/id :fan-resolution)
                  legal-zoom-regimes)})

(def material-required-keys
  #{:path/material-id :path/revision :path/kind :path/geometry
    :path/paint :path/provenance})
(def material-optional-keys #{:path/extensions})
(def paint-required-keys #{:color :opacity :color-space :alpha-association})

(defn finite-number? [x]
  (and (number? x)
       #?(:clj (Double/isFinite (double x))
          :cljs (js/Number.isFinite x))))

(defn- point? [point]
  (and (vector? point)
       (= 2 (count point))
       (every? finite-number? point)))

(defn clamp-pressure [pressure]
  (min 1.0 (max 0.0 (double pressure))))

(defn pressure-width [base-width pressure]
  (* (double base-width) (clamp-pressure pressure)))

(defn zoom-regime [zoom]
  (when-not (and (finite-number? zoom) (<= 0.01 zoom 1000.0))
    (throw (ex-info "Path zoom is outside the legal envelope"
                    {:zoom zoom :legal [0.01 1000.0]})))
  (cond
    (< zoom 0.1) (first legal-zoom-regimes)
    (<= zoom 8.0) (second legal-zoom-regimes)
    :else (nth legal-zoom-regimes 2)))

(defn- validate-paint! [paint]
  (let [keys* (set (keys paint))
        missing (seq (sort (remove keys* paint-required-keys)))
        unknown (seq (sort (remove paint-required-keys keys*)))
        color (:color paint)]
    (when missing
      (throw (ex-info "Path paint is missing required fields"
                      {:missing (vec missing)})))
    (when unknown
      (throw (ex-info "Path paint has unknown fields"
                      {:unknown (vec unknown) :policy :reject})))
    (when-not (and (vector? color) (= 4 (count color))
                   (every? #(and (finite-number? %) (<= 0.0 % 1.0)) color))
      (throw (ex-info "Path paint color must be straight RGBA in [0,1]"
                      {:color color})))
    (when-not (and (finite-number? (:opacity paint))
                   (<= 0.0 (:opacity paint) 1.0))
      (throw (ex-info "Path paint opacity must be in [0,1]"
                      {:opacity (:opacity paint)})))
    (when-not (= :srgb (:color-space paint))
      (throw (ex-info "Path paint requires tagged sRGB material color"
                      {:color-space (:color-space paint)})))
    (when-not (= :straight (:alpha-association paint))
      (throw (ex-info "Path paint must be straight alpha at material ingress"
                      {:alpha-association (:alpha-association paint)})))
    paint))

(defn- validate-knot! [knot]
  (let [required #{:knot/id :position :pressure}
        optional #{:gesture-time :source-event-ids}
        keys* (set (keys knot))]
    (when-let [missing (seq (sort (remove keys* required)))]
      (throw (ex-info "Ink knot is missing required fields"
                      {:missing (vec missing) :knot knot})))
    (when-let [unknown (seq (sort (remove (into required optional) keys*)))]
      (throw (ex-info "Ink knot has unknown fields"
                      {:unknown (vec unknown) :knot knot})))
    (when-not (point? (:position knot))
      (throw (ex-info "Ink knot position must be a finite [x y]"
                      {:knot knot})))
    (when-not (finite-number? (:pressure knot))
      (throw (ex-info "Ink knot pressure must be finite" {:knot knot})))
    knot))

(defn- validate-ink-geometry! [geometry]
  (let [required #{:knots :base-width :cap :join}
        keys* (set (keys geometry))]
    (when-let [missing (seq (sort (remove keys* required)))]
      (throw (ex-info "Ink geometry is missing required fields"
                      {:missing (vec missing)})))
    (when-let [unknown (seq (sort (remove required keys*)))]
      (throw (ex-info "Ink geometry has unknown fields"
                      {:unknown (vec unknown) :policy :reject})))
    (when-not (and (vector? (:knots geometry))
                   (<= 2 (count (:knots geometry))))
      (throw (ex-info "Ink requires at least two ordered knots"
                      {:knots (:knots geometry)})))
    (doseq [knot (:knots geometry)] (validate-knot! knot))
    (when-not (= (count (:knots geometry))
                 (count (set (map :knot/id (:knots geometry)))))
      (throw (ex-info "Ink knot identities must be unique"
                      {:ids (mapv :knot/id (:knots geometry))})))
    (when-not (and (finite-number? (:base-width geometry))
                   (pos? (:base-width geometry)))
      (throw (ex-info "Ink base width must be positive"
                      {:base-width (:base-width geometry)})))
    (when-not (contains? legal-cap-join (:cap geometry))
      (throw (ex-info "Path v1 supports round caps only"
                      {:cap (:cap geometry)})))
    (when-not (contains? legal-cap-join (:join geometry))
      (throw (ex-info "Path v1 supports round joins only"
                      {:join (:join geometry)})))
    geometry))

(defn- validate-contour! [contour]
  (let [required #{:contour/id :role :points}
        keys* (set (keys contour))
        role (:role contour)
        points (:points contour)]
    (when-let [missing (seq (sort (remove keys* required)))]
      (throw (ex-info "Shape contour is missing required fields"
                      {:missing (vec missing) :contour contour})))
    (when-let [unknown (seq (sort (remove required keys*)))]
      (throw (ex-info "Shape contour has unknown fields"
                      {:unknown (vec unknown) :contour contour})))
    (when-not (contains? legal-contour-roles role)
      (throw (ex-info "Shape contour role is invalid"
                      {:role role :legal legal-contour-roles})))
    (when-not (and (vector? points)
                   (<= (if (= :open role) 2 3) (count points))
                   (every? point? points))
      (throw (ex-info "Shape contour has invalid line points"
                      {:role role :points points})))
    contour))

(defn- validate-shape-geometry! [geometry]
  (let [required #{:contours :open-width}
        keys* (set (keys geometry))
        contours (:contours geometry)]
    (when-let [missing (seq (sort (remove keys* required)))]
      (throw (ex-info "Shape geometry is missing required fields"
                      {:missing (vec missing)})))
    (when-let [unknown (seq (sort (remove required keys*)))]
      (throw (ex-info "Shape geometry has unknown fields"
                      {:unknown (vec unknown) :policy :reject})))
    (when-not (and (vector? contours) (seq contours))
      (throw (ex-info "Shape requires at least one contour" {})))
    (doseq [contour contours] (validate-contour! contour))
    (when-not (= (count contours) (count (set (map :contour/id contours))))
      (throw (ex-info "Shape contour identities must be unique"
                      {:ids (mapv :contour/id contours)})))
    (when (and (some #(= :hole (:role %)) contours)
               (not-any? #(= :outer (:role %)) contours))
      (throw (ex-info "Explicit holes require an outer contour" {})))
    (when-not (and (finite-number? (:open-width geometry))
                   (pos? (:open-width geometry)))
      (throw (ex-info "Shape open-polyline width must be positive"
                      {:open-width (:open-width geometry)})))
    geometry))

(defn validate-material!
  "Fail-closed path grammar. Unknown fields, curves, implicit holes, untagged
   color, and unsupported cap/join semantics are rejected by name."
  [material]
  (let [keys* (set (keys material))
        missing (seq (sort (remove keys* material-required-keys)))
        unknown (seq (sort (remove (into material-required-keys
                                         material-optional-keys)
                                   keys*)))
        kind (:path/kind material)]
    (when missing
      (throw (ex-info "Path material is missing required fields"
                      {:missing (vec missing)})))
    (when unknown
      (throw (ex-info "Path material has unknown fields"
                      {:unknown (vec unknown) :policy :reject})))
    (when-not (contains? legal-kinds kind)
      (throw (ex-info "Path material kind is invalid"
                      {:kind kind :legal legal-kinds})))
    (when (nil? (:path/material-id material))
      (throw (ex-info "Path material identity cannot be nil" {})))
    (when (nil? (:path/revision material))
      (throw (ex-info "Path material revision cannot be nil" {})))
    (validate-paint! (:path/paint material))
    (case kind
      :ink (validate-ink-geometry! (:path/geometry material))
      :shape (validate-shape-geometry! (:path/geometry material)))
    material))

(defn canonical-material [material]
  (letfn [(canonical [value]
            (cond
              (map? value) (into (sorted-map)
                                 (map (fn [[k v]] [k (canonical v)]))
                                 value)
              (vector? value) (mapv canonical value)
              (set? value) (into (sorted-set) (map canonical) value)
              :else value))]
    (canonical (validate-material! material))))

(defn- revisioned-edit [material next-revision edit-id edit]
  (let [material (validate-material! material)]
    (when (= (:path/revision material) next-revision)
      (throw (ex-info "Path edit requires a new material revision"
                      {:revision next-revision})))
    (validate-material!
     (-> (edit material)
         (assoc :path/revision next-revision)
         (update :path/provenance
                 (fn [provenance]
                   (-> (or provenance {})
                       (assoc :act edit-id)
                       (update :parents (fnil conj [])
                               [(:path/material-id material)
                                (:path/revision material)]))))))))

(defn move-knot
  "Semantic ink edit over stable knot identity. The caller supplies the next
   durable revision; derived meshes are invalidated only through that key."
  [material knot-id position next-revision]
  (when-not (point? position)
    (throw (ex-info "Path knot edit requires a finite [x y]"
                    {:position position})))
  (revisioned-edit
   material next-revision :path/move-knot
   (fn [material]
     (when-not (= :ink (:path/kind material))
       (throw (ex-info "move-knot applies only to ink materials"
                       {:kind (:path/kind material)})))
     (let [knots (get-in material [:path/geometry :knots])
           matches (count (filter #(= knot-id (:knot/id %)) knots))]
       (when-not (= 1 matches)
         (throw (ex-info "Path knot edit requires exactly one stable identity"
                         {:knot/id knot-id :matches matches})))
       (update-in material [:path/geometry :knots]
                  (fn [rows]
                    (mapv #(if (= knot-id (:knot/id %))
                             (assoc % :position position)
                             %)
                          rows)))))))

(defn set-knot-pressure
  [material knot-id pressure next-revision]
  (when-not (finite-number? pressure)
    (throw (ex-info "Path pressure edit requires a finite value"
                    {:pressure pressure})))
  (revisioned-edit
   material next-revision :path/set-knot-pressure
   (fn [material]
     (when-not (= :ink (:path/kind material))
       (throw (ex-info "set-knot-pressure applies only to ink materials"
                       {:kind (:path/kind material)})))
     (let [knots (get-in material [:path/geometry :knots])
           matches (count (filter #(= knot-id (:knot/id %)) knots))]
       (when-not (= 1 matches)
         (throw (ex-info "Path pressure edit requires exactly one stable identity"
                         {:knot/id knot-id :matches matches})))
       (update-in material [:path/geometry :knots]
                  (fn [rows]
                    (mapv #(if (= knot-id (:knot/id %))
                             (assoc % :pressure pressure)
                             %)
                          rows)))))))

(defn move-contour-point
  "Day-one shape edit addressed by stable contour identity plus canonical
   point index. A later vector-network grammar may replace the index route
   without changing contour identity."
  [material contour-id point-index position next-revision]
  (when-not (point? position)
    (throw (ex-info "Shape point edit requires a finite [x y]"
                    {:position position})))
  (when-not (and (integer? point-index) (<= 0 point-index))
    (throw (ex-info "Shape point edit requires a non-negative integer index"
                    {:point-index point-index})))
  (revisioned-edit
   material next-revision :path/move-contour-point
   (fn [material]
     (when-not (= :shape (:path/kind material))
       (throw (ex-info "move-contour-point applies only to shape materials"
                       {:kind (:path/kind material)})))
     (let [contours (get-in material [:path/geometry :contours])
           index (first (keep-indexed
                         (fn [index contour]
                           (when (= contour-id (:contour/id contour)) index))
                         contours))]
       (when (nil? index)
         (throw (ex-info "Shape edit names an unknown contour"
                         {:contour/id contour-id})))
       (when-not (< -1 point-index
                    (count (get-in contours [index :points])))
         (throw (ex-info "Shape edit point index is outside the contour"
                         {:contour/id contour-id :point-index point-index})))
       (assoc-in material
                 [:path/geometry :contours index :points point-index]
                 position)))))

(defn set-paint [material paint next-revision]
  (validate-paint! paint)
  (revisioned-edit material next-revision :path/set-paint
                   #(assoc % :path/paint paint)))

(defn replace-contours [material contours next-revision]
  (revisioned-edit
   material next-revision :path/replace-contours
   (fn [material]
     (when-not (= :shape (:path/kind material))
       (throw (ex-info "replace-contours applies only to shape materials"
                       {:kind (:path/kind material)})))
     (assoc-in material [:path/geometry :contours] (vec contours)))))

(defn material-content-key [material]
  [:path/content-v1 (pr-str (dissoc (canonical-material material)
                                    :path/revision))])

(defn material-cache-key
  ([material zoom]
   (material-cache-key material algorithm-version zoom))
  ([material algorithm zoom]
   (let [material (validate-material! material)
         regime (:regime/id (zoom-regime zoom))]
     [(material-content-key material)
      (:path/revision material)
      algorithm
      regime])))

(defn material-points [material]
  (let [material (validate-material! material)]
    (case (:path/kind material)
      :ink (mapv :position (get-in material [:path/geometry :knots]))
      :shape (into [] (mapcat :points)
                   (get-in material [:path/geometry :contours])))))

(defn shape-normalization [material]
  (let [points (material-points material)
        xs (map first points)
        ys (map second points)
        min-x (apply min xs) min-y (apply min ys)
        max-x (apply max xs) max-y (apply max ys)
        scale (max 1.0 (- max-x min-x) (- max-y min-y))]
    {:origin [min-x min-y]
     :scale scale
     :authority-precision :f64
     :derived-precision :f32}))

(defn normalize-point [{:keys [origin scale]} [x y]]
  [(/ (- x (first origin)) scale)
   (/ (- y (second origin)) scale)])

(defn denormalize-point [{:keys [origin scale]} [x y]]
  [(+ (first origin) (* x scale))
   (+ (second origin) (* y scale))])

(defn- sq [x] (* x x))
(defn- distance-squared [[ax ay] [bx by]]
  (+ (sq (- ax bx)) (sq (- ay by))))

(defn- segment-projection [[ax ay] [bx by] [px py]]
  (let [dx (- bx ax) dy (- by ay)
        denom (+ (* dx dx) (* dy dy))
        t (if (zero? denom)
            0.0
            (min 1.0 (max 0.0 (/ (+ (* (- px ax) dx)
                                      (* (- py ay) dy))
                                   denom))))]
    {:t t :point [(+ ax (* t dx)) (+ ay (* t dy))]}))

(defn point-segment-distance [point a b]
  (let [{closest :point} (segment-projection a b point)]
    (Math/sqrt (distance-squared point closest))))

(defn- point-on-segment? [point a b]
  (<= (point-segment-distance point a b) boundary-epsilon))

(defn contour-classify [points point]
  (if (some (fn [[a b]] (point-on-segment? point a b))
            (map vector points (concat (rest points) [(first points)])))
    :boundary
    (let [[px py] point
          inside?
          (reduce
           (fn [inside? [[ax ay] [bx by]]]
             (if (and (not= (> ay py) (> by py))
                      (< px (+ ax (* (/ (- py ay) (- by ay)) (- bx ax)))))
               (not inside?)
               inside?))
           false
           (map vector points (concat (rest points) [(first points)])))]
      (if inside? :inside :outside))))

(defn- ink-classify [geometry query-point slop-local]
  (let [knots (:knots geometry)
        base-width (:base-width geometry)
        rows
        (mapv
         (fn [[left right]]
           (let [a (:position left) b (:position right)
                 {t :t closest :point} (segment-projection a b query-point)
                 pressure (+ (:pressure left)
                             (* t (- (:pressure right) (:pressure left))))
                 half-width (/ (pressure-width base-width pressure) 2.0)
                 distance (Math/sqrt (distance-squared query-point closest))
                 delta (- distance (+ half-width slop-local))]
             delta))
         (partition 2 1 knots))
        delta (apply min rows)]
    (cond
      (< delta (- boundary-epsilon)) :inside
      (<= (Math/abs delta) boundary-epsilon) :boundary
      :else :outside)))

(defn classify
  "Tri-state authority classification in path-local f64 coordinates."
  ([material point] (classify material point 1.0))
  ([material point zoom]
   (let [material (validate-material! material)
         slop-local (/ hit-slop-screen-px zoom)]
     (case (:path/kind material)
       :ink (ink-classify (:path/geometry material) point slop-local)
       :shape
       (let [contours (get-in material [:path/geometry :contours])
             closed (remove #(= :open (:role %)) contours)
             open (filter #(= :open (:role %)) contours)
             closed-classes (mapv #(assoc % :class (contour-classify (:points %) point))
                                  closed)
             open-delta
             (when (seq open)
               (apply min
                      (for [contour open
                            [a b] (partition 2 1 (:points contour))]
                        (- (point-segment-distance point a b)
                           (+ (/ (get-in material [:path/geometry :open-width]) 2.0)
                              slop-local)))))]
         (cond
           (some #(= :boundary (:class %)) closed-classes) :boundary
           (and open-delta (<= (Math/abs open-delta) boundary-epsilon)) :boundary
           (and open-delta (neg? open-delta)) :inside
           (and (some #(and (= :outer (:role %)) (= :inside (:class %)))
                      closed-classes)
                (not-any? #(and (= :hole (:role %)) (= :inside (:class %)))
                          closed-classes)) :inside
           :else :outside))))))

(defn hit? [material point]
  (not= :outside (classify material point)))

(defn boundary-distance [material point]
  (let [material (validate-material! material)]
    (case (:path/kind material)
      :ink
      (let [geometry (:path/geometry material)]
        (apply min
               (for [[left right] (partition 2 1 (:knots geometry))]
                 (Math/abs
                  (- (point-segment-distance point (:position left) (:position right))
                     (/ (pressure-width (:base-width geometry)
                                        (/ (+ (:pressure left) (:pressure right)) 2.0))
                        2.0))))))
      :shape
      (apply min
             (for [contour (get-in material [:path/geometry :contours])
                   [a b] (if (= :open (:role contour))
                           (partition 2 1 (:points contour))
                           (map vector (:points contour)
                                (concat (rest (:points contour))
                                        [(first (:points contour))])))]
               (point-segment-distance point a b))))))

(def vertex-words 7)
(def vertex-stride (* vertex-words 4))

(defn paint-color [material]
  (let [{:keys [color opacity]} (:path/paint (validate-material! material))
        [r g b a] color]
    [r g b (* a opacity)]))

(defn vertex-values [material [x y] container-idx]
  (into [x y] (conj (paint-color material) (or container-idx 0))))

(def claimed-corpus-pressures
  #{:pressure-width :round-caps-joins :open-polyline :concave-outer
    :explicit-hole :translucent-self-crossing :legal-zoom-extremes})

(defn assert-corpus-coverage! [fixture-pressure->gates]
  (let [actual (set (keys fixture-pressure->gates))
        missing (seq (sort (remove actual claimed-corpus-pressures)))
        unconsumed (seq (sort (for [[pressure gates] fixture-pressure->gates
                                   :when (empty? gates)]
                               pressure)))]
    (when (or missing unconsumed)
      (throw (ex-info "Path corpus is incomplete or unconsumed"
                      {:missing (vec missing) :unconsumed (vec unconsumed)})))
    true))
