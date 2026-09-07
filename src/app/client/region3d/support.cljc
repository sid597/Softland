(ns app.client.region3d.support
  "Sphere support geometry and authored chart coordinates.
   Takes a sphere value, chart/curve data and unit-vector locations. Gives
   intrinsic distances, chart locations, metrics and texel domain adapters.
   Holds nothing. No smooth/trimmed-host solver is implied.
   Evidence: surface_region_test.clj and coating_test.clj."
  (:require [app.client.engine.schema :as schema]))

(defn refuse! [reason detail]
  (throw (ex-info (name reason) {:status :refused :reason reason :detail detail})))

(defn validate! [{:keys [kind R] :as host}]
  (when-not (and (= :sphere kind) (schema/finite-number? R) (pos? R))
    (refuse! :support "A finite positive sphere radius is required")) host)

(defn dot [a b] (reduce + (map * a b)))
(defn cross [[ax ay az] [bx by bz]] [(- (* ay bz) (* az by)) (- (* az bx) (* ax bz)) (- (* ax by) (* ay bx))])
(defn length [v] (Math/sqrt (dot v v)))
(defn scale [v n] (mapv #(* % n) v))
(defn add [a b] (mapv + a b))
(defn unit [[lon lat]] [(* (Math/cos lat) (Math/cos lon)) (Math/sin lat) (* (Math/cos lat) (Math/sin lon))])
(defn lon [[x _ z]] (Math/atan2 z x))
(defn lat [[_ y _]] (Math/asin (max -1.0 (min 1.0 y))))

(defn point! [point]
  (when-not (and (= 3 (count point)) (every? schema/finite-number? point)
                 (< (Math/abs (- 1.0 (length point))) 1.0e-9))
    (refuse! :point "A point on a sphere is a unit vector")) point)

(defn angle [a b] (Math/atan2 (length (cross a b)) (dot a b)))
(defn distance "Sphere and two unit vectors → shorter great-circle distance, mm."
  [host a b] (* (:R host) (angle a b)))

(defn affine [[a b c d e f] [x y]] [(+ (* a x) (* c y) e) (+ (* b x) (* d y) f)])
(defn inverse [[a b c d e f] [x y]]
  (let [det (- (* a d) (* b c)) x (- x e) y (- y f)]
    (when (zero? det) (refuse! :chart "A chart relation must be invertible"))
    [(/ (- (* d x) (* c y)) det) (/ (- (* a y) (* b x)) det)]))

(defn chart [host id]
  (or (some #(when (= id (:id %)) %) (:charts host)) (refuse! :chart id)))

(defn revision-chart [host revision]
  (or (some #(when (= revision (:revision %)) %) (:charts host)) (refuse! :revision revision)))

(defn chart->point "Chart coordinates → unit-vector support location."
  [host chart-id uv]
  (let [[u v] (affine (:to-root (chart host chart-id)) uv)] (unit [(/ u (:R host)) (/ v (:R host))])))

(defn point->chart "Support point → authored branch around longitude pi, then chart."
  [host chart-id point]
  (let [longitude (lon point) longitude (if (neg? longitude) (+ longitude (* 2 Math/PI)) longitude)]
    (inverse (:to-root (chart host chart-id)) [(* (:R host) longitude) (* (:R host) (lat point))])))

(def north-lat (* 70 (/ Math/PI 180)))
(defn piece [point] (if (> (lat point) north-lat) :north (if (neg? (lon point)) :west :east)))

(defn metric "Chart and latitude → local metric J^T diag(cos² latitude,1) J."
  [host chart-id latitude]
  (let [[a b c d] (:to-root (chart host chart-id)) c2 (Math/pow (Math/cos latitude) 2)]
    [[(+ (* c2 a a) (* b b)) (+ (* c2 a c) (* b d))]
     [(+ (* c2 a c) (* b d)) (+ (* c2 c c) (* d d))]]))

(defn curve-point "Host and retained curve name/parameter → point; the arc is authored data."
  [host curve t]
  (let [record (some #(when (= curve (:curve %)) %) (map host (:record-keys host)))]
    (when-not record (refuse! :curve curve))
    (when-not (and (schema/finite-number? t) (<= 0 t 1)) (refuse! :parameter t))
    (let [theta (/ (- (* (:length record) t) (/ (:length record) 2)) (:R host))]
      (add (scale (unit (:seed record)) (Math/cos theta)) (scale (:tangent record) (Math/sin theta))))))

(defn arc-distance "Constant-width minor great-circle arc → closest centre, parameter and distance."
  [host record point]
  (let [n (unit (:seed record)) e (:tangent record)
        half (/ (:length record) (* 2 (:R host)))
        theta (max (- half) (min half (Math/atan2 (dot point e) (dot point n))))
        centre (add (scale n (Math/cos theta)) (scale e (Math/sin theta)))]
    {:distance (distance host point centre) :t (/ (+ theta half) (* 2 half)) :centre centre}))

(defn domains
  "Bind a sphere value into the compositor's chart domain. These functions
   are execution adapters; only the host and surface values cross the wire."
  [host]
  {:chart
   {:to-texel (fn [surface point]
                (let [[u v] (point->chart host (get-in surface [:domain :chart]) point)
                      [u0 v0 w h] (get-in surface [:domain :rect])]
                  [(* (/ (- u u0) w) (:width surface)) (* (/ (- v v0) h) (:height surface))]))
    :to-point (fn [surface x y]
                (let [[u0 v0 w h] (get-in surface [:domain :rect])]
                  (chart->point host (get-in surface [:domain :chart])
                                [(+ u0 (/ (* (+ x 0.5) w) (:width surface)))
                                 (+ v0 (/ (* (+ y 0.5) h) (:height surface)))])))}})
