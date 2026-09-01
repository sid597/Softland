(ns app.client.path.material
  "What a path is, and the four things everyone needs from one. A path is ink
   (a stroke of pressure-tagged points, round caps) or a shape (filled outer
   contours with holes), straight segments only, with a paint.
   Takes: a path map; a point; optional local-unit hit slop.
   Gives: a validated path; inside, boundary, or outside; a content key; paint.
   Holds nothing.")

(def schema-version 2)
(def legal-kinds #{:ink :shape})
(def legal-contour-roles #{:outer :hole})
(def legal-cap-join #{:round})
(def boundary-epsilon 1.0e-9)

(def example-ink-material
  {:path/material-id :path/ink-fixture
   :path/revision :ink/rev-1
   :path/kind :ink
   :path/geometry
   {:knots [{:knot/id [:knot 0] :position [0.0 0.0] :pressure 0.2}
            {:knot/id [:knot 1] :position [20.0 0.0] :pressure 0.6}
            {:knot/id [:knot 2] :position [30.0 10.0] :pressure 1.0}]
    :base-width 10.0 :cap :round :join :round}
   :path/paint {:color [0.2 0.6 0.9 0.8]
                :opacity 0.75
                :color-space :srgb
                :alpha-association :straight}})

(def example-shape-material
  {:path/material-id :path/holed-concave
   :path/revision :shape/rev-1
   :path/kind :shape
   :path/geometry
   {:contours
    [{:contour/id :outer :role :outer
      :points [[0.0 0.0] [40.0 0.0] [40.0 40.0]
               [24.0 40.0] [24.0 16.0] [16.0 16.0]
               [16.0 40.0] [0.0 40.0]]}
     {:contour/id :hole :role :hole
      :points [[4.0 4.0] [13.0 4.0] [13.0 13.0] [4.0 13.0]]}]}
   :path/paint {:color [0.9 0.3 0.2 1.0]
                :opacity 1.0
                :color-space :srgb
                :alpha-association :straight}})

(def material-required-keys
  #{:path/material-id :path/revision :path/kind :path/geometry
    :path/paint})
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
        optional #{:gesture-time}
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
    (when (and (contains? knot :gesture-time)
               (not (finite-number? (:gesture-time knot))))
      (throw (ex-info "Ink knot gesture time must be finite" {:knot knot})))
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
                   (<= 3 (count points))
                   (every? point? points))
      (throw (ex-info "Shape contour has invalid line points"
                      {:role role :points points})))
    contour))

(defn- validate-shape-geometry! [geometry]
  (let [required #{:contours}
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
    geometry))

(defn validate-material!
  "Fail-closed path grammar. Unknown fields, curves, implicit holes, untagged
   color, and unsupported cap/join semantics are rejected by name."
  [material]
  (let [keys* (set (keys material))
        missing (seq (sort (remove keys* material-required-keys)))
        unknown (seq (sort (remove material-required-keys keys*)))
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

(defn material-content-key [material]
  [:path/content-v2 (pr-str (dissoc (canonical-material material)
                                    :path/material-id :path/revision))])

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
     :scale scale}))

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

(defn- contour-boundary-distance [points point]
  (apply min
         (for [[a b] (map vector points
                          (concat (rest points) [(first points)]))]
           (point-segment-distance point a b))))

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
  ([material point] (classify material point 0.0))
  ([material point slop-local]
   (let [material (validate-material! material)]
     (when-not (and (finite-number? slop-local) (not (neg? slop-local)))
       (throw (ex-info "Path hit slop must be finite local units"
                       {:slop-local slop-local})))
     (case (:path/kind material)
       :ink (ink-classify (:path/geometry material) point slop-local)
       :shape
       (let [contours (get-in material [:path/geometry :contours])
             closed-classes (mapv #(assoc % :class (contour-classify (:points %) point))
                                  contours)
             base-class
             (cond
               (some #(= :boundary (:class %)) closed-classes) :boundary
               (and (some #(and (= :outer (:role %)) (= :inside (:class %)))
                                closed-classes)
                     (not-any? #(and (= :hole (:role %)) (= :inside (:class %)))
                               closed-classes)) :inside
               :else :outside)
             slop-delta
             (when (and (= :outside base-class) (pos? slop-local))
               (- (apply min
                         (map #(contour-boundary-distance (:points %) point)
                              contours))
                  slop-local))]
         (cond
           (not= :outside base-class) base-class
           (and slop-delta (<= (Math/abs slop-delta) boundary-epsilon)) :boundary
           (and slop-delta (neg? slop-delta)) :inside
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
                   [a b] (map vector (:points contour)
                              (concat (rest (:points contour))
                                      [(first (:points contour))]))]
               (point-segment-distance point a b))))))

(defn paint-color [material]
  (let [{:keys [color opacity]} (:path/paint (validate-material! material))
        [r g b a] color]
    [r g b (* a opacity)]))
