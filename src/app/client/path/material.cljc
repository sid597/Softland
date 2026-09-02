(ns app.client.path.material
  "What a path is, and the four things everyone needs from one. A path is ink
   (a stroke of width-tagged points, round caps) or a shape (filled outer
   contours with holes), straight segments only, with a paint.
   Takes: a validated path map; a point; optional local-unit hit slop.
   Gives: the declared grammar; inside, boundary, or outside; a content key;
   paint.
   Holds nothing."
  (:require [app.client.engine.schema :as schema]))

(def schema-version 2)
(def legal-kinds #{:ink :shape})
(def legal-contour-roles #{:outer :hole})
(def legal-cap-join #{:round})
(def boundary-epsilon 1.0e-9)

(defn- named-validator [error-type predicate]
  (fn [value]
    (when-not (predicate value)
      (throw (ex-info "Path grammar refused value" {:error-type error-type})))
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

(def knot
  {:keys #{:knot/id :position :width}
   :optional #{:pressure :gesture-time}
   :validators
   {:knot/id (named-validator :path/knot-id some?)
    :position (named-validator :path/knot-position schema/point?)
    :width (named-validator :path/knot-width schema/positive-number?)
    :pressure (named-validator :path/knot-pressure schema/finite-number?)
    :gesture-time
    (named-validator :path/knot-gesture-time schema/finite-number?)}})

(def ink-geometry
  {:keys #{:knots :cap :join}
   :validators
   {:knots [:vector-of knot {:min 2 :unique-by :knot/id}]
    :cap (named-validator :path/cap legal-cap-join)
    :join (named-validator :path/join legal-cap-join)}})

(def contour
  {:keys #{:contour/id :role :points}
   :validators
   {:contour/id (named-validator :path/contour-id some?)
    :role (named-validator :path/contour-role legal-contour-roles)
    :points [:vector-of schema/point? {:min 3}]}})

(defn- holes-have-an-outer? [geometry]
  (let [contours (:contours geometry)]
    (or (not-any? #(= :hole (:role %)) contours)
        (some #(= :outer (:role %)) contours))))

(def shape-geometry
  {:keys #{:contours}
   :validators
   {:contours [:vector-of contour {:min 1 :unique-by :contour/id}]}
   :form-validators
   [{:valid? holes-have-an-outer? :error-type :path/hole-without-outer}]})

(defn- geometry-matches-kind? [material]
  (case (:path/kind material)
    :ink (do (schema/check ink-geometry (:path/geometry material)) true)
    :shape (do (schema/check shape-geometry (:path/geometry material)) true)
    false))

(def grammar
  {:keys #{:path/material-id :path/revision :path/kind :path/geometry
           :path/paint}
   :validators
   {:path/material-id (named-validator :path/material-id some?)
    :path/revision (named-validator :path/revision some?)
    :path/kind (named-validator :path/kind legal-kinds)
    :path/paint paint}
   :form-validators
   [{:valid? geometry-matches-kind? :error-type :path/geometry-kind}]})

(defn validate-material!
  "Check the declared path grammar and return the unchanged EDN map."
  [material]
  (schema/check grammar material))

(defn canonical-material [material]
  (letfn [(canonical [value]
            (cond
              (map? value) (into (sorted-map)
                                 (map (fn [[key child]] [key (canonical child)]))
                                 value)
              (vector? value) (mapv canonical value)
              (set? value) (into (sorted-set) (map canonical) value)
              :else value))]
    (canonical material)))

(defn material-content-key [material]
  [:path/content-v2
   (pr-str (dissoc (canonical-material material)
                   :path/material-id :path/revision))])

(defn- sq [value] (* value value))

(defn- distance-squared [[ax ay] [bx by]]
  (+ (sq (- ax bx)) (sq (- ay by))))

(defn- segment-projection [[ax ay] [bx by] [px py]]
  (let [dx (- bx ax)
        dy (- by ay)
        denominator (+ (* dx dx) (* dy dy))
        t (if (zero? denominator)
            0.0
            (min 1.0
                 (max 0.0 (/ (+ (* (- px ax) dx) (* (- py ay) dy))
                             denominator))))]
    {:t t :point [(+ ax (* t dx)) (+ ay (* t dy))]}))

(defn- point-segment-distance [point a b]
  (let [{closest :point} (segment-projection a b point)]
    (Math/sqrt (distance-squared point closest))))

(defn- segment-delta [a width-a b width-b point]
  (let [{:keys [t] closest :point} (segment-projection a b point)
        distance (Math/sqrt (distance-squared point closest))
        half-width (/ (+ width-a (* t (- width-b width-a))) 2.0)]
    {:t t
     :distance distance
     :half-width half-width
     :delta (- distance half-width)}))

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

(defn- ink-deltas [geometry query-point]
  (mapv (fn [[left right]]
          (:delta (segment-delta (:position left) (:width left)
                                 (:position right) (:width right)
                                 query-point)))
        (partition 2 1 (:knots geometry))))

(defn- ink-classify [geometry query-point slop-local]
  (let [delta (- (apply min (ink-deltas geometry query-point)) slop-local)]
    (cond
      (< delta (- boundary-epsilon)) :inside
      (<= (Math/abs delta) boundary-epsilon) :boundary
      :else :outside)))

(defn classify
  "Tri-state authority classification in path-local f64 coordinates."
  ([material point] (classify material point 0.0))
  ([material point slop-local]
   (when-not (schema/non-negative-number? slop-local)
     (throw (ex-info "Path hit slop must be finite local units"
                     {:error-type :path/hit-slop
                      :path [:slop-local]
                      :value slop-local})))
   (case (:path/kind material)
     :ink (ink-classify (:path/geometry material) point slop-local)
     :shape
     (let [contours (get-in material [:path/geometry :contours])
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

(defn hit? [material point]
  (not= :outside (classify material point)))

(defn boundary-distance [material point]
  (case (:path/kind material)
    :ink (apply min (map #(Math/abs %) (ink-deltas (:path/geometry material)
                                                   point)))
    :shape
    (apply min
           (for [contour (get-in material [:path/geometry :contours])
                 [a b] (map vector (:points contour)
                            (concat (rest (:points contour))
                                    [(first (:points contour))]))]
             (point-segment-distance point a b)))))

(defn paint-color [material]
  (let [{:keys [color opacity]} (:path/paint material)
        [red green blue alpha] color]
    [red green blue (* alpha opacity)]))
