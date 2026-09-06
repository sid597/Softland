(ns app.client.path.stroke
  "Sweep a tip along a path and give back regions.

   Input: a path value, a stroke declaration and options (tip, tolerance,
   widths). Output: regions as closed outline paths of lines and arc
   quadratics, consistently oriented so the filler's nonzero rule turns the
   folded skin into one region; or the ordered round dabs of an accumulating
   brush. No retained state; the camera enters only through the flattening
   tolerance and a device-unit width the caller has already converted.

   The centerline is flattened at the tolerance, then one closed outline is
   traced per open piece (side A forward, end cap, side B backward, start
   cap), or two loops per closed piece (alignment picks which two). The two
   tips differ in one line, the direction of the tangent point from the
   knot: the ribbon's is the piece's normal; the swept round nib's is the
   external tangent of the two end discs, which leans with the taper. Inner
   joins fold through the pivot and keep the body's orientation. Dash walks
   the flattened centerline by arc length and makes open pieces with caps.

   Folder map: README.md."
  (:require [app.client.path.value :as v]))

;; ---- an outline under construction, threaded through the tracer ----

(def empty-outline {:subpaths [] :current nil :arcs 0})

(defn- finish
  "Outline → the same with any current subpath closed off."
  [out]
  (if-let [cur (:current out)]
    (-> out (update :subpaths conj cur) (assoc :current nil))
    out))

(defn move-to [out [x y]]
  (assoc (finish out) :current {:closed? true :start [x y] :segments []}))

(defn line-to [out [x y]]
  (update-in out [:current :segments] conj (v/line [x y])))

(defn quad-to [out c p]
  (update-in out [:current :segments] conj (v/quad c p)))

(defn add-subpath
  "Outline and a subpath → the outline with that subpath added as a closed
   loop of its own (alignment rings reuse the exact centerline)."
  [out sp]
  (update (finish out) :subpaths conj
          {:closed? true :start (:start sp) :segments (vec (:segments sp))}))

(defn arc
  "Outline, centre, radius, start and end angle, optional via angle → the
   outline with the arc as ≤45° quadratics. The short way round, or through
   `via` when given (caps go around the end)."
  [out center r a0 a1 via]
  (if (< r 1.0e-9)
    (line-to out (v/add center (v/scale (v/dir a1) r)))
    (let [d (v/norm-angle (- a1 a0))
          d (if (some? via)
              (let [dv (v/norm-angle (- via a0))
                    inside? (and (<= (Math/abs dv) (+ (Math/abs d) 1.0e-9))
                                 (or (< (Math/abs dv) 1.0e-9) (= (v/sign dv) (v/sign d))))]
                (if inside? d (- d (* (if (zero? d) 1.0 (v/sign d)) v/tau))))
              d)]
      (if (< (Math/abs d) 1.0e-9)
        (line-to out (v/add center (v/scale (v/dir a1) r)))
        (let [k (max 1 (int (Math/ceil (/ (Math/abs d) (/ Math/PI 4.0)))))]
          (reduce (fn [out i]
                    (let [t0 (+ a0 (/ (* d (dec i)) k))
                          t1 (+ a0 (/ (* d i) k))
                          h (/ (- t1 t0) 2.0)
                          c (v/add center (v/scale (v/dir (/ (+ t0 t1) 2.0)) (/ r (Math/cos h))))]
                      (-> out
                          (quad-to c (v/add center (v/scale (v/dir t1) r)))
                          (update :arcs inc))))
                  out (range 1 (inc k))))))))

(defn outline-path
  "Outline → {:subpaths [...]} of what was traced."
  [out]
  {:subpaths (:subpaths (finish out))})

;; ---- the tracer ----

(defn dedupe-points
  "Points and closed? → the points with near-coincident neighbours dropped;
   a closed run drops a repeated end."
  [points closed?]
  (let [xy (fn [p] [(:x p) (:y p)])
        out (reduce (fn [acc p]
                      (if (or (empty? acc) (> (v/dist (xy p) (xy (peek acc))) 1.0e-6))
                        (conj acc p)
                        acc))
                    [] points)]
    (if (and closed? (> (count out) 1) (<= (v/dist (xy (first out)) (xy (peek out))) 1.0e-6))
      (pop out)
      out)))

(defn with-radius
  "Points and a factor → the points with :r = width × factor."
  [points f]
  (mapv (fn [p] (assoc p :r (max 0.0 (* (or (:w p) 0.0) f)))) points))

(defn signed-area
  "Polygon points → signed area (positive when wound with Y down clockwise
   on screen, i.e. positive cross products)."
  [points]
  (/ (reduce + 0.0
             (map (fn [a b] (v/cross [(:x a) (:y a)] [(:x b) (:y b)]))
                  points (concat (rest points) [(first points)])))
     2.0))

(defn trace-pieces
  "Points with radii, closed?, tip → one piece per segment: {:a :b :u :d
   :m+ :m-}, where :m+ and :m- are the directions from the knots to the
   tangent points on each side. :nib leans them by the external tangent of
   the two end discs; :ribbon uses the normal."
  [points closed? tip]
  (let [n (count points)
        m (if closed? n (dec n))]
    (vec (for [i (range m)]
           (let [a (nth points i) b (nth points (mod (inc i) n))
                 pa [(:x a) (:y a)] pb [(:x b) (:y b)]
                 d (let [d (v/dist pa pb)] (if (zero? d) 1.0e-9 d))
                 u (v/unit (v/sub pb pa))
                 nrm (v/perp u)
                 [c sn] (if (= :nib tip)
                          (let [c (v/clamp (/ (- (:r a) (:r b)) d) -1.0 1.0)]
                            [c (Math/sqrt (max 0.0 (- 1.0 (* c c))))])
                          [0.0 1.0])
                 m-side (fn [s] [(+ (* c (u 0)) (* s sn (nrm 0))) (+ (* c (u 1)) (* s sn (nrm 1)))])]
             {:a a :b b :u u :d d :m+ (m-side 1.0) :m- (m-side -1.0)})))))

(defn tangent-point
  "Piece, end (:a or :b), side (1 or -1) → the tangent point on the hull."
  [piece end s]
  (let [m (if (pos? s) (:m+ piece) (:m- piece))
        k (if (= :a end) (:a piece) (:b piece))]
    [(+ (:x k) (* (:r k) (m 0))) (+ (:y k) (* (:r k) (m 1)))]))

(defn- knot-xy [k] [(:x k) (:y k)])

(defn join
  "Outline, pieces, indexes i (in) and j (out), side, forward?, stroke → the
   outline with the join at the knot they share. An inner join folds through
   the pivot; an outer join is round, miter (within the limit) or bevel."
  [out pieces i j s fwd? stroke]
  (let [pi (nth pieces i) pj (nth pieces j)
        k (:b pi) kxy (knot-xy k)
        from (if fwd? (tangent-point pi :b s) (tangent-point pj :a s))
        to (if fwd? (tangent-point pj :a s) (tangent-point pi :b s))
        turn (v/cross (:u pi) (:u pj))
        straight? (and (< (Math/abs turn) 1.0e-7) (pos? (v/dot (:u pi) (:u pj))))]
    (cond
      (and straight? (< (v/dist from to) 1.0e-7)) out
      ;; inner side: the fold through the pivot; nonzero absorbs it
      (and (not (neg? (* s turn))) (not straight?)) (-> out (line-to kxy) (line-to to))
      (or (= :round (:join stroke)) straight? (< (:r k) 1.0e-9))
      (arc out kxy (:r k) (v/angle (v/sub from kxy)) (v/angle (v/sub to kxy)) nil)
      (= :miter (:join stroke))
      (let [A (tangent-point pi :b s) dA (v/unit (v/sub (tangent-point pi :b s) (tangent-point pi :a s)))
            B (tangent-point pj :a s) dB (v/unit (v/sub (tangent-point pj :b s) (tangent-point pj :a s)))
            den (v/cross dA dB)]
        (if (> (Math/abs den) 1.0e-9)
          (let [t (/ (v/cross (v/sub B A) dB) den)
                M (v/add A (v/scale dA t))]
            (if (<= (v/dist M kxy) (* (or (:miter-limit stroke) 4.0) (:r k)))
              (-> out (line-to M) (line-to to))
              (line-to out to)))
          (line-to out to)))
      :else (line-to out to))))

(defn cap
  "Outline, piece, end, side from, side to, stroke → the outline with the
   cap around that end: round through the end direction, square, or butt
   (the chord between the tangent points; on a taper it is not perpendicular
   to the centerline)."
  [out piece end s-from s-to stroke]
  (let [k (if (= :a end) (:a piece) (:b piece)) kxy (knot-xy k)
        from (tangent-point piece end s-from)
        to (tangent-point piece end s-to)
        via (if (= :a end) (v/angle (v/scale (:u piece) -1.0)) (v/angle (:u piece)))]
    (cond
      (< (:r k) 1.0e-9) (line-to out to)
      (= :round (:cap stroke)) (arc out kxy (:r k) (v/angle (v/sub from kxy)) (v/angle (v/sub to kxy)) via)
      (= :square (:cap stroke))
      (let [e (v/scale (:u piece) (if (= :a end) (- (:r k)) (:r k)))]
        (-> out (line-to (v/add from e)) (line-to (v/add to e)) (line-to to)))
      :else (line-to out to))))

(defn trace-open
  "Outline, points with radii, stroke, tip → [outline pieces]: one closed
   outline around an open centerline."
  [out points stroke tip]
  (let [pieces (trace-pieces points false tip)
        m (count pieces)
        out (move-to out (tangent-point (first pieces) :a 1))
        out (reduce (fn [out i]
                      (let [out (line-to out (tangent-point (nth pieces i) :b 1))]
                        (if (< i (dec m)) (join out pieces i (inc i) 1 true stroke) out)))
                    out (range m))
        out (cap out (nth pieces (dec m)) :b 1 -1 stroke)
        out (reduce (fn [out i]
                      (let [out (line-to out (tangent-point (nth pieces i) :a -1))]
                        (if (pos? i) (join out pieces (dec i) i -1 false stroke) out)))
                    out (range (dec m) -1 -1))
        out (cap out (first pieces) :a -1 1 stroke)]
    [out pieces]))

(defn trace-loop
  "Outline, pieces of a closed centerline, side, forward?, stroke → the
   outline with one loop on that side."
  [out pieces s fwd? stroke]
  (let [m (count pieces)]
    (if fwd?
      (reduce (fn [out i]
                (-> out
                    (line-to (tangent-point (nth pieces i) :b s))
                    (join pieces i (mod (inc i) m) s true stroke)))
              (move-to out (tangent-point (first pieces) :a s))
              (range m))
      (reduce (fn [out i]
                (-> out
                    (line-to (tangent-point (nth pieces i) :a s))
                    (join pieces (mod (+ (dec i) m) m) i s false stroke)))
              (move-to out (tangent-point (nth pieces (dec m)) :b s))
              (range (dec m) -1 -1)))))

(defn stroke-closed
  "Outline, points, stroke, tip, the exact subpath → [outline pieces]: the
   skin of a closed centerline as a ring between two loops. :center uses
   both half-width offsets; :inside the path itself and its inward offset;
   :outside the outward offset and the path reversed, wound opposite so
   nonzero leaves the middle empty."
  [out points stroke tip exact-subpath]
  (let [area (signed-area points)
        s-in (if (pos? area) 1 -1)]
    (case (:align stroke)
      :inside (let [pieces (trace-pieces (with-radius points 1.0) true tip)
                    out (-> out
                            (add-subpath (v/explicit-close exact-subpath))
                            (trace-loop pieces s-in false stroke))]
                [out pieces])
      :outside (let [pieces (trace-pieces (with-radius points 1.0) true tip)
                     out (-> out
                             (trace-loop pieces (- s-in) true stroke)
                             (add-subpath (v/reverse-subpath exact-subpath)))]
                 [out pieces])
      (let [pieces (trace-pieces (with-radius points 0.5) true tip)
            out (-> out (trace-loop pieces 1 true stroke) (trace-loop pieces -1 false stroke))]
        [out pieces]))))

(defn dash-polyline
  "Flattened polyline {:closed? :points}, on, off, phase → open pieces by
   arc length; an invalid dash returns the polyline itself."
  [{:keys [closed? points]} on off phase]
  (let [points (if closed? (conj (vec points) (first points)) (vec points))
        period (+ on off)]
    (if-not (and (pos? on) (pos? period))
      [{:closed? closed? :points points}]
      (let [ph (mod (mod (or phase 0.0) period) period)
            state0 (identity (< ph on))
            remain0 (if state0 (- on ph) (- off (- ph on)))]
        (loop [i 1 state (identity state0) remain remain0
               cur (when state0 {:closed? false :points [(first points)]})
               pieces []]
          (if (= i (count points))
            (if (and state cur (> (count (:points cur)) 1)) (conj pieces cur) pieces)
            (let [a (nth points (dec i)) b (nth points i)
                  seg-len (v/dist [(:x a) (:y a)] [(:x b) (:y b)])
                  [state remain cur pieces t0]
                  (loop [state (identity state) remain remain cur cur pieces pieces t0 0.0]
                    (if (> (- seg-len t0) remain)
                      (let [t (/ (+ t0 remain) seg-len)
                            p {:x (+ (:x a) (* (- (:x b) (:x a)) t))
                               :y (+ (:y a) (* (- (:y b) (:y a)) t))
                               :w (+ (or (:w a) 0.0) (* (- (or (:w b) 0.0) (or (:w a) 0.0)) t))
                               :p (+ (or (:p a) 0.5) (* (- (or (:p b) 0.5) (or (:p a) 0.5)) t))}
                            [cur pieces] (if state
                                           [nil (conj pieces (update cur :points conj p))]
                                           [{:closed? false :points [p]} pieces])
                            state (not state)]
                        (recur state (if state on off) cur pieces (+ t0 remain)))
                      [state remain cur pieces t0]))
                  remain (- remain (- seg-len t0))
                  cur (if state (update cur :points conj b) cur)]
              (recur (inc i) state remain cur pieces))))))))

(defn- width-of-fn
  "Options → (subpath, knot index → width)."
  [{:keys [width-local knot-scale fallback-width]}]
  (fn [sp i]
    (if (number? width-local)
      width-local
      (* (v/knot-width sp i (or fallback-width 4.0)) (or knot-scale 1.0)))))

(defn- envelope-result
  "The tracer's accumulator → the public result with :path and :arcs."
  [acc]
  (let [out (:out acc)]
    (-> acc
        (dissoc :out)
        (assoc :path (outline-path out) :arcs (:arcs out)))))

(defn envelope
  "Path, stroke declaration ({:cap :join :miter-limit :align}), options →
   {:path :arcs :open :closed :pieces :polylines}.

   Options: :tip (:nib | :ribbon), :tolerance (flattening, local units),
   :width-local (a constant width) or :knot-scale over the knot widths with
   :fallback-width, :width-fn (pressure, s → width), :dash [on off],
   :dash-phase. The returned path is the skin, one closed outline per open
   piece and a ring per closed piece, to be filled nonzero. :polylines are
   the flattened centerlines with radii, for distance answers."
  [path stroke opts]
  (let [width-of (width-of-fn opts)
        tip (or (:tip opts) :nib)
        tol (or (:tolerance opts) 0.1)]
    (envelope-result
     (reduce
     (fn [acc sp]
       (let [flat (v/flatten-subpath (v/explicit-close sp) tol width-of (:width-fn opts))
             points (dedupe-points (:points flat) (:closed? flat))]
         (cond
           (= 1 (count points))
           (let [p (first points) r (/ (or (:w p) 0.0) 2.0)]
             (if (pos? r)
               (update acc :out (fn [out]
                                  (-> out
                                      (move-to [(+ (:x p) r) (:y p)])
                                      (arc [(:x p) (:y p)] r 0.0 Math/PI nil)
                                      (arc [(:x p) (:y p)] r Math/PI v/tau nil))))
               acc))
           (< (count points) 2) acc
           :else
           (let [runs (if (:dash opts)
                        (dash-polyline {:closed? (:closed? flat) :points points}
                                       (first (:dash opts)) (second (:dash opts)) (:dash-phase opts))
                        [{:closed? (:closed? flat) :points points}])]
             (reduce
              (fn [acc run]
                (cond
                  (< (count (:points run)) 2) acc
                  (and (:closed? run) (> (count (:points run)) 2))
                  (let [[out pieces] (stroke-closed (:out acc) (:points run) stroke tip sp)]
                    (-> acc
                        (assoc :out out)
                        (update :pieces + (count pieces))
                        (update :polylines conj (with-radius (:points run) (if (= :center (:align stroke)) 0.5 1.0)))
                        (update :closed inc)))
                  :else
                  (let [pts (with-radius (:points run) 0.5)
                        [out pieces] (trace-open (:out acc) pts stroke tip)]
                    (-> acc
                        (assoc :out out)
                        (update :pieces + (count pieces))
                        (update :polylines conj pts)
                        (update :open inc)))))
              acc runs)))))
     {:out empty-outline :open 0 :closed 0 :pieces 0 :polylines []}
     (:subpaths path)))))

(defn disc
  "Centre and radius → a closed outline of one disc as four arc quads
   (two half arcs, each two quads)."
  [[x y] r]
  (outline-path (-> empty-outline
                    (move-to [(+ x r) y])
                    (arc [x y] r 0.0 Math/PI nil)
                    (arc [x y] r Math/PI v/tau nil))))

(defn dabs
  "Path and options (as envelope's, plus :spacing) → {:dabs :polylines
   :length}: round dabs at arc lengths 0, spacing, 2·spacing … along the
   flattened centerline, each {:x :y :r :at :s :p :seg :u :path}, in
   drawing order. The width is evaluated at the dab's own interpolated
   pressure when a width-fn is given, never interpolated between evaluated
   widths (attack 2)."
  [path opts]
  (let [width-of (width-of-fn opts)
        tol (or (:tolerance opts) 0.1)
        spacing (or (:spacing opts) 12.0)]
    (reduce
     (fn [acc sp]
       (let [flat (v/flatten-subpath (v/explicit-close sp) tol width-of (:width-fn opts))
             points (dedupe-points (:points flat) (:closed? flat))]
         (if (empty? points)
           acc
           (let [points (if (and (:closed? flat) (> (count points) 2)) (conj points (first points)) points)
                 total (v/polyline-length points)
                 n (count points)
                 [dabs travelled]
                 (loop [i 0 next 0.0 s 0.0 dabs (:dabs acc)]
                   (if-not (or (< (inc i) n) (and (zero? i) (= n 1)))
                     [dabs s]
                     (let [a (nth points i) b (if (< (inc i) n) (nth points (inc i)) a)
                           L (v/dist [(:x a) (:y a)] [(:x b) (:y b)])
                           [dabs next]
                           (loop [next next dabs dabs]
                             (if (and (<= next (+ s L 1.0e-9)) (or (pos? L) (zero? next)))
                               (let [t (if (pos? L) (/ (- next s) L) 0.0)
                                     x (+ (:x a) (* (- (:x b) (:x a)) t))
                                     y (+ (:y a) (* (- (:y b) (:y a)) t))
                                     pa (or (:p a) 0.5) pb (or (:p b) 0.5)
                                     p (+ pa (* (- pb pa) t))
                                     s-frac (if (pos? total) (/ next total) 0.0)
                                     tt (+ (or (:t a) 0.0) (* (- (or (:t b) 0.0) (or (:t a) 0.0)) t))
                                     w (if (:width-fn opts) ((:width-fn opts) p s-frac) (+ (:w a) (* (- (:w b) (:w a)) t)))
                                     r (/ w 2.0)
                                     dab {:x x :y y :r r :at next :s s-frac :p p
                                          :seg (int (Math/floor tt)) :u (- tt (Math/floor tt))
                                          :path (disc [x y] r)}
                                     dabs (conj dabs dab)]
                                 (if (zero? L) [dabs (+ next spacing)] (recur (+ next spacing) dabs)))
                               [dabs next]))]
                       (recur (inc i) next (+ s L) dabs))))]
             (-> acc
                 (assoc :dabs dabs)
                 (update :polylines conj (with-radius points 0.5))
                 (update :length + travelled))))))
     {:dabs [] :polylines [] :length 0.0}
     (:subpaths path))))
