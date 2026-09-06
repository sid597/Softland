(ns app.client.path.value
  "The path value the kind takes, and the answers that read it.

   Input: a path value and query parameters. Output: derived values: closed
   forms, reversals, bounds, points on curves, flattened polylines with
   source correspondence, stats. No retained state; no camera anywhere.

   The value:
     {:subpaths [{:closed? bool
                  :start [x y]
                  :segments [{:kind :line :p [x y]}
                             {:kind :quad :c [x y] :p [x y]}
                             {:kind :cubic :c1 [x y] :c2 [x y] :p [x y]}]
                  :knots [{:id any :width n? :pressure n? :time n?}]}]}
   Knot i sits at the end of segment i-1; knot 0 at the start; a subpath may
   omit knots (an outline the geometry made). Local units: one unit is one
   CSS pixel at zoom 1, Y down. Cubics are stored; the packer lowers them.

   Folder map: README.md."
  (:require [app.client.engine.schema :as schema]))

;; ---- 2D vectors, shared by the path family ----

(defn add [[ax ay] [bx by]] [(+ ax bx) (+ ay by)])
(defn sub [[ax ay] [bx by]] [(- ax bx) (- ay by)])
(defn scale [[x y] k] [(* x k) (* y k)])
(defn dot [[ax ay] [bx by]] (+ (* ax bx) (* ay by)))
(defn cross [[ax ay] [bx by]] (- (* ax by) (* ay bx)))
(defn len [[x y]] (Math/sqrt (+ (* x x) (* y y))))
(defn dist [a b] (len (sub a b)))
(defn perp "Direction → its left-hand perpendicular." [[x y]] [(- y) x])
(defn lerp [[ax ay] [bx by] t] [(+ ax (* (- bx ax) t)) (+ ay (* (- by ay) t))])
(defn angle [[x y]] (Math/atan2 y x))
(defn dir [theta] [(Math/cos theta) (Math/sin theta)])
(defn unit
  "Vector → unit vector; a zero vector stays zero."
  [v]
  (let [l (len v)] (if (< l 1.0e-12) [0.0 0.0] (scale v (/ 1.0 l)))))
(defn clamp [x a b] (min b (max a x)))
(defn sign [x] (cond (pos? x) 1.0 (neg? x) -1.0 :else 0.0))

(def tau (* 2.0 Math/PI))

(defn norm-angle
  "Angle → the same angle in (-π, π]."
  [a]
  (- (mod (+ a Math/PI) tau) Math/PI))

;; ---- schema ----

(def legal-segment-kinds #{:line :quad :cubic})

(defn- named-validator
  [error-type predicate]
  (fn [value]
    (when-not (predicate value)
      (throw (ex-info "Path value rejected" {:error-type error-type})))
    true))

(defn segment?
  "Value → a well-formed segment of a legal kind?"
  [segment]
  (and (map? segment)
       (case (:kind segment)
         :line (schema/point? (:p segment))
         :quad (and (schema/point? (:c segment)) (schema/point? (:p segment)))
         :cubic (and (schema/point? (:c1 segment)) (schema/point? (:c2 segment))
                     (schema/point? (:p segment)))
         false)))

(def knot
  {:keys #{:id}
   :optional #{:width :pressure :time}
   :validators
   {:id (named-validator :path/knot-id some?)
    :width (named-validator :path/knot-width schema/non-negative-number?)
    :pressure (named-validator :path/knot-pressure schema/finite-number?)
    :time (named-validator :path/knot-time schema/finite-number?)}})

(defn- knots-match-segments?
  [subpath]
  (or (nil? (:knots subpath))
      (= (count (:knots subpath)) (inc (count (:segments subpath))))))

(def subpath
  {:keys #{:closed? :start :segments}
   :optional #{:knots}
   :validators
   {:closed? (named-validator :path/closed boolean?)
    :start (named-validator :path/start schema/point?)
    :segments [:vector-of segment? {}]
    :knots [:vector-of knot {}]}
   :form-validators
   [{:valid? knots-match-segments? :error-type :path/knots-count}]})

(def schema
  {:keys #{:subpaths}
   :optional #{:meta}
   :validators {:subpaths [:vector-of subpath {}]}})

(defn validate!
  "Path value → the same value, or a named exception."
  [path]
  (schema/check schema path))

;; ---- constructors ----

(defn line [p] {:kind :line :p p})
(defn quad [c p] {:kind :quad :c c :p p})
(defn cubic [c1 c2 p] {:kind :cubic :c1 c1 :c2 c2 :p p})

(def empty-path {:subpaths []})

;; ---- reading a subpath ----

(defn segment-end
  "Subpath and segment index → the point where segment i ends; -1 gives the
   start."
  [sp i]
  (if (neg? i) (:start sp) (:p (nth (:segments sp) i))))

(defn knot-at
  "Subpath and knot index → the knot, clamped to the last one, or nil."
  [sp i]
  (let [knots (:knots sp)]
    (when (seq knots) (nth knots (min i (dec (count knots)))))))

(defn knot-width
  "Subpath, knot index, fallback → the knot's width or the fallback."
  [sp i fallback]
  (let [w (:width (knot-at sp i))]
    (if (number? w) w fallback)))

(defn knot-pressure
  "Subpath and knot index → the knot's pressure, 0.5 when absent."
  [sp i]
  (let [p (:pressure (knot-at sp i))]
    (if (number? p) p 0.5)))

(defn explicit-close
  "Subpath → the same subpath with its closing line spelled out when closed,
   so reversal and joins see it."
  [sp]
  (if-not (:closed? sp)
    sp
    (let [last-point (segment-end sp (dec (count (:segments sp))))]
      (if (< (dist last-point (:start sp)) 1.0e-9)
        sp
        (cond-> (update sp :segments conj (line (:start sp)))
          (:knots sp) (update :knots conj (first (:knots sp))))))))

(defn reverse-subpath
  "Subpath → the same curve walked backwards, knots reversed."
  [sp0]
  (let [sp (explicit-close sp0)
        n (count (:segments sp))
        segments (vec (for [i (range (dec n) -1 -1)]
                        (let [s (nth (:segments sp) i)
                              p (segment-end sp (dec i))]
                          (case (:kind s)
                            :line (line p)
                            :quad (quad (:c s) p)
                            :cubic (cubic (:c2 s) (:c1 s) p)))))]
    (cond-> {:closed? (:closed? sp)
             :start (segment-end sp (dec n))
             :segments segments}
      (:knots sp) (assoc :knots (vec (rseq (:knots sp)))))))

(defn bbox
  "Path → [x0 y0 x1 y1] over every control point; an empty path gives
   zeros."
  [path]
  (let [points (for [sp (:subpaths path)
                     p (cons (:start sp)
                             (mapcat (fn [s] (remove nil? [(:c s) (:c1 s) (:c2 s) (:p s)]))
                                     (:segments sp)))]
                 p)]
    (if (seq points)
      (reduce (fn [[x0 y0 x1 y1] [x y]]
                [(min x0 x) (min y0 y) (max x1 x) (max y1 y)])
              [##Inf ##Inf ##-Inf ##-Inf]
              points)
      [0.0 0.0 0.0 0.0])))

(defn stats
  "Path → counts of subpaths, knots and segment kinds, and whether any knot
   width varies."
  [path]
  (reduce (fn [acc sp]
            (let [widths (keep :width (:knots sp))]
              (-> acc
                  (update :subpaths inc)
                  (update :knots + (count (:knots sp)))
                  (update :lines + (count (filter #(= :line (:kind %)) (:segments sp))))
                  (update :quads + (count (filter #(= :quad (:kind %)) (:segments sp))))
                  (update :cubics + (count (filter #(= :cubic (:kind %)) (:segments sp))))
                  (cond-> (and (seq widths) (apply not= widths)) (assoc :varying? true)))))
          {:subpaths 0 :knots 0 :lines 0 :quads 0 :cubics 0 :varying? false}
          (:subpaths path)))

;; ---- points on curves ----

(defn quad-at [a c b t]
  (let [u (- 1.0 t)]
    [(+ (* u u (a 0)) (* 2.0 u t (c 0)) (* t t (b 0)))
     (+ (* u u (a 1)) (* 2.0 u t (c 1)) (* t t (b 1)))]))

(defn cubic-at [a c1 c2 b t]
  (let [u (- 1.0 t)]
    [(+ (* u u u (a 0)) (* 3.0 u u t (c1 0)) (* 3.0 u t t (c2 0)) (* t t t (b 0)))
     (+ (* u u u (a 1)) (* 3.0 u u t (c1 1)) (* 3.0 u t t (c2 1)) (* t t t (b 1)))]))

(defn evaluate
  "Subpath, segment index, parameter → the point on that segment."
  [sp i t]
  (let [a (segment-end sp (dec i))
        s (nth (:segments sp) i)]
    (case (:kind s)
      :line (lerp a (:p s) t)
      :quad (quad-at a (:c s) (:p s) t)
      :cubic (cubic-at a (:c1 s) (:c2 s) (:p s) t))))

(defn quad-steps
  "Quadratic control points and tolerance → line count (Wang's bound)."
  [a c b tol]
  (let [d (len [(+ (a 0) (* -2.0 (c 0)) (b 0)) (+ (a 1) (* -2.0 (c 1)) (b 1))])]
    (int (clamp (Math/ceil (Math/sqrt (/ d (* 4.0 tol)))) 1 96))))

(defn cubic-steps
  "Cubic control points and tolerance → line count (Wang's bound)."
  [a c1 c2 b tol]
  (let [d (max (len [(+ (a 0) (* -2.0 (c1 0)) (c2 0)) (+ (a 1) (* -2.0 (c1 1)) (c2 1))])
               (len [(+ (c1 0) (* -2.0 (c2 0)) (b 0)) (+ (c1 1) (* -2.0 (c2 1)) (b 1))]))]
    (int (clamp (Math/ceil (Math/sqrt (/ (* 0.75 d) tol))) 1 128))))

(defn flatten-subpath
  "Subpath, tolerance, width-of (subpath, knot index → width) and optional
   width-fn (pressure, arc fraction → width) → {:closed? :points}.

   Each point carries :x :y :w (width there), :t (segment + parameter), :p
   (the interpolated pressure), :seg and :u. Width between knots is either
   the interpolation of the knot widths, or width-fn applied to the
   interpolated pressure (the definer's fix: interpolate the source
   attribute, then apply the response). A closed subpath drops a repeated
   final point."
  [sp tol width-of width-fn]
  (let [segments (:segments sp)
        n (count segments)
        pressure (fn [i t] (+ (knot-pressure sp i) (* (- (knot-pressure sp (inc i)) (knot-pressure sp i)) t)))
        width (fn [i t wa wb]
                (if width-fn
                  (width-fn (pressure i t) (/ (+ i t) (max 1 n)))
                  (+ wa (* (- wb wa) t))))
        point (fn [[x y] i t wa wb]
                {:x x :y y :w (width i t wa wb) :t (+ i t) :p (pressure i t) :seg i :u t})
        w0 (width-of sp 0)
        points
        (loop [i 0 cur (:start sp) acc [(point (:start sp) 0 0.0 w0 w0)]]
          (if (= i n)
            acc
            (let [s (nth segments i)
                  wa (width-of sp i)
                  wb (width-of sp (inc i))
                  acc (case (:kind s)
                        :line (conj acc (point (:p s) i 1.0 wa wb))
                        :quad (let [m (quad-steps cur (:c s) (:p s) tol)]
                                (reduce (fn [acc j]
                                          (let [t (/ j m)]
                                            (conj acc (point (quad-at cur (:c s) (:p s) t) i t wa wb))))
                                        acc (range 1 (inc m))))
                        :cubic (let [m (cubic-steps cur (:c1 s) (:c2 s) (:p s) tol)]
                                 (reduce (fn [acc j]
                                           (let [t (/ j m)]
                                             (conj acc (point (cubic-at cur (:c1 s) (:c2 s) (:p s) t) i t wa wb))))
                                         acc (range 1 (inc m)))))]
              (recur (inc i) (:p s) acc))))
        points (if (and (:closed? sp) (> (count points) 1)
                        (< (dist [(:x (first points)) (:y (first points))]
                                 [(:x (peek points)) (:y (peek points))]) 1.0e-9))
                 (pop points)
                 points)]
    {:closed? (boolean (:closed? sp)) :points points}))

(defn flatten-path
  "Path and tolerance → one polyline per subpath as [x y] points, with
   :closed?. The answer flatten(τ) for consumers that need lines."
  [path tol]
  (mapv (fn [sp]
          (let [{:keys [closed? points]} (flatten-subpath (explicit-close sp) tol (constantly 0.0) nil)]
            {:closed? closed? :points (mapv (fn [p] [(:x p) (:y p)]) points)}))
        (:subpaths path)))

(defn polyline-length
  "Points ([x y] or {:x :y}) → arc length along them."
  [points]
  (let [xy (fn [p] (if (map? p) [(:x p) (:y p)] p))]
    (reduce + 0.0 (map (fn [a b] (dist (xy a) (xy b))) points (rest points)))))

(defn map-points
  "Path and f ([x y] → [x y]) → the path with every point mapped, knots
   kept. For snapping and projection."
  [path f]
  (update path :subpaths
          (fn [subpaths]
            (mapv (fn [sp]
                    (-> sp
                        (update :start f)
                        (update :segments
                                (fn [segments]
                                  (mapv (fn [s]
                                          (reduce (fn [s k] (if (contains? s k) (update s k f) s))
                                                  s [:c :c1 :c2 :p]))
                                        segments)))))
                  subpaths))))
