(ns app.client.region3d.surface-region
  "Retained spherical caps as data, shared by reach and brush footprints.
   Takes a sphere at a revision, seed or point and radius. Gives membership,
   distance, cap area, conservative bounds and chart pieces. Holds no closures
   or caller-owned mutable state. Evidence: surface_region_test.clj."
  (:require [app.client.region3d.support :as support]
            [app.client.engine.schema :as schema]))

(defn- pieces [point rho]
  (let [lat (support/lat point) lon (support/lon point)
        pole? (>= rho (- (/ Math/PI 2) (Math/abs lat) 1.0e-12))
        half (if pole? Math/PI (Math/asin (min 1.0 (/ (Math/sin rho) (Math/cos lat)))))
        lo (- lon half) hi (+ lon half)
        spans? (fn [a b] (or pole? (some (fn [k]
                                         (let [shift (* k 2 Math/PI)]
                                           (<= (max (+ lo shift) a) (min (+ hi shift) b)))) (range -1 2))))]
    (cond-> []
      (> (+ lat rho) support/north-lat) (conj :north)
      (and (<= (- lat rho) support/north-lat) (spans? (- Math/PI) 0)) (conj :west)
      (and (<= (- lat rho) support/north-lat) (spans? 0 Math/PI)) (conj :east))))

(defn construct
  "Sphere, support selector and {:seed|:point :radius :distance} → retained
   cap. Radius >= pi R is the whole host; zero is the seed; negative refuses.
   The complete geometric support is retained, not a live reference."
  [host selector {:keys [seed point radius distance] :or {distance :surface}}]
  (support/validate! host)
  (when-not (= (:id host) (:id selector)) (support/refuse! :support selector))
  (support/revision-chart host (get selector :revision 0))
  (when-not (= :surface distance) (support/refuse! :distance distance))
  (when-not (and (schema/finite-number? radius) (<= 0 radius)) (support/refuse! :radius radius))
  (when-not (or point (and (= 2 (count seed)) (every? schema/finite-number? seed)
                           (<= (- (/ Math/PI 2)) (second seed) (/ Math/PI 2))))
    (support/refuse! :seed seed))
  (let [n (support/point! (or point (support/unit seed))) R (:R host)
        saturated? (>= radius (* Math/PI R)) rho (if saturated? Math/PI (/ radius R))
        cr (Math/cos rho) sr (Math/sin rho)
        bounds (mapv (fn [ni] (let [s (Math/sqrt (max 0.0 (- 1.0 (* ni ni))))]
                               [(if (or saturated? (>= (- ni) cr)) (- R) (* R (- (* ni cr) (* s sr))))
                                (if (or saturated? (>= ni cr)) R (* R (+ (* ni cr) (* s sr))))])) n)]
    {:region/kind :surface :support (assoc (select-keys host [:kind :id :R :charts]) :revision (get selector :revision 0))
     :centre [(support/lon n) (support/lat n)] :point n :radius radius :distance :surface
     :area (if saturated? (* 4 Math/PI R R) (* 2 Math/PI R R (- 1.0 cr)))
     :bounds bounds :pieces (pieces n rho) :saturated? saturated?}))

(defn distance "Retained region and point → the same intrinsic distance used by membership."
  [region point]
  (when-not (= :surface (:region/kind region)) (support/refuse! :region region))
  (support/distance (:support region) (:point region) (support/point! point)))

(defn member [region point] (<= (distance region point) (:radius region)))
