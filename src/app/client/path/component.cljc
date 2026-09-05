(ns app.client.path.component
  "Define the path's data and CPU geometric meaning.

   Input: component maps and local query points. Output: schema acceptance,
   canonical content identity, tri-state classification, distance and paint.
   No retained state. Schema definitions cover paint, stroke points, ink
   geometry, contours and shape geometry; version is 2. :path/material-id
   and :path/revision remain field names even though the file is named
   component.cljc.

   Folder map: README.md."
  (:require [app.client.engine.schema :as schema]))

(def schema-version 2)
(def legal-kinds #{:ink :shape})
(def legal-contour-roles #{:outer :hole})
(def legal-cap-join #{:round})
(def boundary-epsilon 1.0e-9)

(defn- named-validator
  "Error type and predicate → checking function returning true or throwing.

   Gives shared-schema failures family-specific names."
  [error-type predicate]
  (fn [value]
    (when-not (predicate value)
      (throw (ex-info "Path schema rejected value" {:error-type error-type})))
    true))

(def paint
  {:keys #{:color :opacity :color-space :alpha-association}
   :validators
   {:color (named-validator :path/paint-color schema/valid-rgba?)
    :opacity (named-validator
              :path/paint-opacity
              #(and (schema/finite-number? %) (<= 0.0 % 1.0)))
    :color-space (named-validator :path/paint-color-space #{:srgb})
    :alpha-association
    (named-validator :path/paint-alpha-association #{:straight})}})

(def stroke-point
  {:keys #{:stroke-point/id :position :width}
   :optional #{:pressure :gesture-time}
   :validators
   {:stroke-point/id (named-validator :path/stroke-point-id some?)
    :position (named-validator :path/stroke-point-position schema/point?)
    :width (named-validator :path/stroke-point-width schema/positive-number?)
    :pressure (named-validator :path/stroke-point-pressure schema/finite-number?)
    :gesture-time
    (named-validator :path/stroke-point-gesture-time schema/finite-number?)}})

(def ink-geometry
  {:keys #{:stroke-points :cap :join}
   :validators
   {:stroke-points [:vector-of stroke-point {:min 2 :unique-by :stroke-point/id}]
    :cap (named-validator :path/cap legal-cap-join)
    :join (named-validator :path/join legal-cap-join)}})

(def contour
  {:keys #{:contour/id :role :points}
   :validators
   {:contour/id (named-validator :path/contour-id some?)
    :role (named-validator :path/contour-role legal-contour-roles)
    :points [:vector-of schema/point? {:min 3}]}})

(defn- holes-have-an-outer?
  "Shape geometry → truthy if no holes or some outer exists.

   Presence check. Intended for this narrow invariant; does not prove
   containment."
  [geometry]
  (let [contours (:contours geometry)]
    (or (not-any? #(= :hole (:role %)) contours)
        (some #(= :outer (:role %)) contours))))

(def shape-geometry
  {:keys #{:contours}
   :validators
   {:contours [:vector-of contour {:min 1 :unique-by :contour/id}]}
   :form-validators
   [{:valid? holes-have-an-outer? :error-type :path/hole-without-outer}]})

(defn- geometry-matches-kind?
  "Component → true after matching geometry validation, false for unknown
   kind.

   Dispatches to ink/shape schema."
  [component]
  (case (:path/kind component)
    :ink (do (schema/check ink-geometry (:path/geometry component)) true)
    :shape (do (schema/check shape-geometry (:path/geometry component)) true)
    false))

(def schema
  {:keys #{:path/material-id :path/revision :path/kind :path/geometry
           :path/paint}
   :validators
   {:path/material-id (named-validator :path/material-id some?)
    :path/revision (named-validator :path/revision some?)
    :path/kind (named-validator :path/kind legal-kinds)
    :path/paint paint}
   :form-validators
   [{:valid? geometry-matches-kind? :error-type :path/geometry-kind}]})

(defn validate-component!
  "Component → same map or named exception.

   Shared schema entry. No admission metadata is stamped here. Structural
   acceptance does not establish simple polygons, contained holes or nonzero
   segments; tessellation can reject a structurally valid component."
  [component]
  (schema/check schema component))

(defn canonical-component
  "Nested component value → recursively sorted maps/sets and preserved
   vector order.

   Local recursive canonical helper performs structural normalization.
   Sorted collections assume mutually comparable keys/elements."
  [component]
  (letfn [(canonical [value]
            (cond
              (map? value) (into (sorted-map)
                                 (map (fn [[key child]] [key (canonical child)]))
                                 value)
              (vector? value) (mapv canonical value)
              (set? value) (into (sorted-set) (map canonical) value)
              :else value))]
    (canonical component)))

(defn component-content-hash
  "Component → versioned canonical printed content excluding ID/revision.

   Stable value key, not a compact cryptographic hash. Paint remains
   included here, unlike the mesh cache key."
  [component]
  [:path/content-v2
   (pr-str (dissoc (canonical-component component)
                   :path/material-id :path/revision))])

(defn- sq
  "Number → square."
  [value] (* value value))

(defn- distance-squared
  "Two points → squared distance.

   Avoids square root when unnecessary."
  [[ax ay] [bx by]]
  (+ (sq (- ax bx)) (sq (- ay by))))

(defn- segment-projection
  "Segment endpoints and point → clamped parameter and closest centerline
   point.

   Dot-product projection; zero segment gives its start."
  [[ax ay] [bx by] [px py]]
  (let [dx (- bx ax)
        dy (- by ay)
        denominator (+ (* dx dx) (* dy dy))
        t (if (zero? denominator)
            0.0
            (min 1.0
                 (max 0.0 (/ (+ (* (- px ax) dx) (* (- py ay) dy))
                             denominator))))]
    {:t t :point [(+ ax (* t dx)) (+ ay (* t dy))]}))

(defn- point-segment-distance
  "Point and segment → Euclidean distance.

   Uses projection."
  [point a b]
  (let [{closest :point} (segment-projection a b point)]
    (Math/sqrt (distance-squared point closest))))

(defn- segment-delta
  "Endpoints, endpoint widths, query → parameter/distance/interpolated
   half-width/signed delta.

   Width sampled at centerline projection. Defines the implemented
   varying-width hit rule; it is not an independent exact-distance solver
   for every tapered outline."
  [a width-a b width-b point]
  (let [{:keys [t] closest :point} (segment-projection a b point)
        distance (Math/sqrt (distance-squared point closest))
        half-width (/ (+ width-a (* t (- width-b width-a))) 2.0)]
    {:t t
     :distance distance
     :half-width half-width
     :delta (- distance half-width)}))

(defn- point-on-segment?
  "Point and segment → within boundary epsilon?

   Distance tolerance. Epsilon is in local units."
  [point a b]
  (<= (point-segment-distance point a b) boundary-epsilon))

(defn contour-classify
  "Polygon points and query → :boundary, :inside, or :outside.

   Boundary scan then odd/even ray crossing. Intended for this polygon
   contract; O(edges)."
  [points point]
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

(defn- contour-boundary-distance
  "Polygon and point → minimum edge distance.

   Full edge scan."
  [points point]
  (apply min
         (for [[a b] (map vector points
                          (concat (rest points) [(first points)]))]
           (point-segment-distance point a b))))

(defn- ink-deltas
  "Ink geometry and query → signed deltas for adjacent segments.

   Pairwise projection with interpolated widths. Allocates a vector per
   query."
  [geometry query-point]
  (mapv (fn [[left right]]
          (:delta (segment-delta (:position left) (:width left)
                                 (:position right) (:width right)
                                 query-point)))
        (partition 2 1 (:stroke-points geometry))))

(defn- ink-classify
  "Geometry, query, slop → tri-state classification.

   Minimum segment delta minus slop. Intended for the declared local hit
   rule."
  [geometry query-point slop-local]
  (let [delta (- (apply min (ink-deltas geometry query-point)) slop-local)]
    (cond
      (< delta (- boundary-epsilon)) :inside
      (<= (Math/abs delta) boundary-epsilon) :boundary
      :else :outside)))

(defn classify
  "Component, point, optional nonnegative slop → tri-state; invalid slop
   throws.

   Ink union or outer-minus-hole classification, followed by boundary slop
   expansion. Trusts component validation and treats any hole interior as
   excluded from any outer."
  ([component point] (classify component point 0.0))
  ([component point slop-local]
   (when-not (schema/non-negative-number? slop-local)
     (throw (ex-info "Path hit slop must be finite local units"
                     {:error-type :path/hit-slop
                      :path [:slop-local]
                      :value slop-local})))
   (case (:path/kind component)
     :ink (ink-classify (:path/geometry component) point slop-local)
     :shape
     (let [contours (get-in component [:path/geometry :contours])
           closed-classes
           (mapv #(assoc % :class (contour-classify (:points %) point)) contours)
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
         :else :outside)))))

(defn hit?
  "Component and point → true for inside or boundary."
  [component point]
  (not= :outside (classify component point)))

(defn boundary-distance
  "Component and point → minimum absolute ink-segment delta or contour-edge
   distance.

   Scans component geometry. For overlapping ink segments, nearest
   individual segment boundary need not be the boundary of their union."
  [component point]
  (case (:path/kind component)
    :ink (apply min (map #(Math/abs %) (ink-deltas (:path/geometry component)
                                                   point)))
    :shape
    (apply min
           (for [contour (get-in component [:path/geometry :contours])
                 [a b] (map vector (:points contour)
                            (concat (rest (:points contour))
                                    [(first (:points contour))]))]
             (point-segment-distance point a b)))))

(defn paint-color
  "Component → straight RGBA with opacity multiplied into alpha.

   Pure paint extraction. Shader applies scene-color conversion later."
  [component]
  (let [{:keys [color opacity]} (:path/paint component)
        [red green blue alpha] color]
    [red green blue (* alpha opacity)]))
