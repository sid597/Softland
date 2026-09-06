(ns app.client.path.nib
  "Construct the round nib's union in the bench's path vocabulary.

   Takes a subpath, width rule and local error request. Gives flattened
   points with source correspondence, and closed capsule paths. Holds no
   state. Radius subdivision samples quarter points: an estimate for a
   general expression, not a bound on an arbitrary oscillating function.
   See nib-test for nonlinear width and containing-disc counterexamples."
  (:require [app.client.engine.schema :as schema]
            [app.client.path.value :as v]))

(defn- controls [from segment]
  (case (:kind segment)
    :line [from (:p segment)]
    :quad [from (:c segment) (:p segment)]
    :cubic [from (:c1 segment) (:c2 segment) (:p segment)]))

(defn- evaluate [points t]
  (loop [points points]
    (if (= 1 (count points)) (first points)
        (recur (mapv #(v/lerp %1 %2 t) points (rest points))))))

(defn- split [points]
  (loop [row points left [] right ()]
    (let [left (conj left (first row)) right (conj right (peek row))]
      (if (= 1 (count row)) [left (vec right)]
          (recur (mapv #(v/lerp %1 %2 0.5) row (rest row)) left right)))))

(defn flatten-stroke
  "Subpath, positive local tolerance, knot-width reader and optional
   (pressure, progress → width) → {:closed? :points}. Bounds the centerline
   control-hull error and samples radius error before accepting a piece.
   Progress follows the existing segment-plus-parameter convention."
  [sp tolerance width-of width-fn]
  (when-not (schema/positive-number? tolerance)
    (throw (ex-info "Positive stroke tolerance required" {:error-type :path/tolerance})))
  (let [sp (v/explicit-close sp)
        segments (:segments sp) n (count segments)
        point (fn [cs i t]
                (let [p (+ (v/knot-pressure sp i)
                           (* t (- (v/knot-pressure sp (inc i)) (v/knot-pressure sp i))))
                      w (if width-fn (width-fn p (/ (+ i t) (max 1 n)))
                            (+ (width-of sp i) (* t (- (width-of sp (inc i)) (width-of sp i)))))
                      [x y] (evaluate cs t)]
                  (when-not (schema/non-negative-number? w)
                    (throw (ex-info "Stroke width must be finite and nonnegative"
                                    {:error-type :path/width :width w})))
                  {:x x :y y :w w :p p :t (+ i t) :seg i :u t}))
        points
        (reduce
         (fn [out [i from segment]]
           (let [cs (controls from segment)]
             (loop [pending [[cs 0.0 1.0]] out out]
               (if-let [[part t0 t1] (peek pending)]
                 (let [a (point cs i t0) b (point cs i t1)
                       chord (apply max (map-indexed
                                         (fn [j p] (v/dist p (v/lerp (first part) (peek part)
                                                                   (/ j (dec (count part)))))) part))
                       radius (apply max
                                     (for [u [0.25 0.5 0.75]
                                           :let [p (point cs i (+ t0 (* u (- t1 t0))))]]
                                       (* 0.5 (Math/abs (- (:w p) (+ (:w a) (* u (- (:w b) (:w a)))))))))]
                   (if (<= (+ chord radius) tolerance)
                     (recur (pop pending) (conj out b))
                     (let [tm (* 0.5 (+ t0 t1)) [left right] (split part)]
                       (when (or (= tm t0) (= tm t1))
                         (throw (ex-info "Stroke tolerance exceeds numeric resolution"
                                         {:error-type :path/tolerance-resolution})))
                       (recur (conj (pop pending) [right tm t1] [left t0 tm]) out))))
                 out))))
         [(point (if (seq segments) (controls (:start sp) (first segments)) [(:start sp)]) 0 0.0)]
         (map vector (range) (cons (:start sp) (map :p segments)) segments))]
    {:closed? (boolean (:closed? sp)) :points points}))

(defn arc
  "Center, radius, increasing angles and local tolerance → arc quads.
   Ported circle bound: r*(1-cos(span/2))²/(2*cos(span/2)); spans ≤ pi/8."
  [center radius a0 a1 tolerance]
  (let [point (fn [angle r] (v/add center (v/scale (v/dir angle) r)))
        n (loop [n (max 1 (long (Math/ceil (/ (Math/abs (- a1 a0)) (/ Math/PI 8.0)))))]
            (let [c (Math/cos (/ (- a1 a0) (* 2.0 n)))
                  error (/ (* radius (- 1 c) (- 1 c)) (* 2 c))]
              (if (<= error tolerance) n (recur (* 2 n)))))]
    (mapv (fn [i]
            (let [a (+ a0 (* (- a1 a0) (/ i n))) b (+ a0 (* (- a1 a0) (/ (inc i) n)))]
              (v/quad (point (* 0.5 (+ a b)) (/ radius (Math/cos (* 0.5 (- b a)))))
                      (point b radius)))) (range n))))

(defn disc
  "Center, nonnegative radius and local tolerance → positively wound disc."
  [[x y :as center] radius tolerance]
  {:subpaths (if (pos? radius)
               [{:start [(+ x radius) y] :closed? true
                 :segments (arc center radius 0.0 v/tau tolerance)}] [])})

(defn capsule
  "Two points with :x :y :r and tolerance → the hull of their discs.
   If one disc contains the other, the result is that entire disc."
  [a b tolerance]
  (let [ap [(:x a) (:y a)] bp [(:x b) (:y b)] ar (:r a) br (:r b)
        d (v/dist ap bp)]
    (if (<= d (Math/abs (- ar br)))
      (if (>= ar br) (disc ap ar tolerance) (disc bp br tolerance))
      (let [direction (v/angle (v/sub bp ap)) angle (Math/acos (/ (- ar br) d))
            upper (+ direction angle) lower (- direction angle)
            radial (fn [p r angle] (v/add p (v/scale (v/dir angle) r)))]
        {:subpaths [{:start (radial ap ar upper) :closed? true
                     :segments (vec (concat (arc ap ar upper (+ lower v/tau) tolerance)
                                             [(v/line (radial bp br lower))]
                                             (arc bp br lower upper tolerance)
                                             [(v/line (radial ap ar upper))]))}]}))))

(defn swept-nib
  "Points with radii, closed flag and tolerance → one nonzero union path.
   All capsule loops share winding; overlap is painted once."
  [points closed? tolerance]
  (let [points (cond-> points (and closed? (> (count points) 1)) (conj (first points)))]
    (if (= 1 (count points))
      (disc [(:x (first points)) (:y (first points))] (:r (first points)) tolerance)
      {:subpaths (vec (mapcat (fn [a b] (:subpaths (capsule a b tolerance))) points (rest points)))})))
