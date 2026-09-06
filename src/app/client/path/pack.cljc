(ns app.client.path.pack
  "Lower a region for the camera, and answer coverage on the CPU the way
   the filler does on the GPU.

   Input: a region's outline path, a tolerance derived from the device scale,
   cover options; a pack and a query point. Output: a pack (quadratics in
   local units, row and column bands sorted for the shader's early exit, the
   band transform, the bounds), cover rectangles, and the CPU twin's winding
   and coverage at a point. No retained state; the pack is a derived
   rendering resource its caller caches per scale bucket.

   The camera enters here and nowhere above: a cubic becomes quadratics at a
   tolerance in local units chosen for the bucket of device pixels per local
   unit, so a zoom inside the bucket is a camera move and a zoom across it
   is a repack. Membership (winding under the rule) is the one definition
   every reader shares; pixel coverage is the filler's estimate of it, and
   the twin here computes that estimate with the same root code, the same
   solver and the same combine, so the harness can compare a GPU pixel to
   it.

   Folder map: README.md."
  (:require [app.client.path.value :as v]))

;; ---- tolerance from the device scale ----

(def device-tolerance-px 0.25)

(defn scale-bucket
  "Device pixels per local unit → the power-of-two bucket it falls in."
  [scale]
  (int (Math/floor (/ (Math/log (max scale 1.0e-9)) (Math/log 2.0)))))

(defn bucket-scale
  "Bucket → the scale at its top, where the pack's quality is set."
  [bucket]
  (Math/pow 2.0 (inc bucket)))

(defn bucket-tolerance
  "Bucket → the lowering tolerance in local units: a quarter device pixel
   at the bucket's top scale, so quality never drops inside the bucket."
  [bucket]
  (/ device-tolerance-px (bucket-scale bucket)))

(defn bucket-margin
  "Bucket → cover margin in local units: one device pixel at the bucket's
   coarsest scale, so every edge pixel is evaluated throughout the bucket."
  [bucket]
  (/ 1.0 (Math/pow 2.0 bucket)))

;; ---- lowering: cubic → quadratics ----

(defn split-cubic
  "Cubic control points and t → the two halves."
  [a c1 c2 b t]
  (let [ab (v/lerp a c1 t) bc (v/lerp c1 c2 t) cd (v/lerp c2 b t)
        abc (v/lerp ab bc t) bcd (v/lerp bc cd t) m (v/lerp abc bcd t)]
    [[a ab abc m] [m bcd cd b]]))

(defn cubic->quads
  "Cubic control points and tolerance → quads as [x1 y1 cx cy x3 y3].

   The midpoint quad of a cubic errs by at most (√3/36)·|b − 3c2 + 3c1 − a|,
   shrinking by n³ over n equal-parameter pieces."
  [a c1 c2 b tol]
  (let [d (v/len [(- (+ (b 0) (* 3.0 (c1 0))) (* 3.0 (c2 0)) (a 0))
                  (- (+ (b 1) (* 3.0 (c1 1))) (* 3.0 (c2 1)) (a 1))])
        n (int (v/clamp (Math/ceil (Math/cbrt (/ (* (/ (Math/sqrt 3.0) 36.0) d) (max tol 1.0e-6)))) 1 24))
        pieces (loop [k 1 pieces [[a c1 c2 b]]]
                 (if (>= k n)
                   pieces
                   (let [t (/ 1.0 (+ (- n k) 1))
                         [p0 p1 p2 p3] (peek pieces)
                         [p q] (split-cubic p0 p1 p2 p3 t)]
                     (recur (inc k) (-> pieces pop (conj p) (conj q))))))]
    (mapv (fn [[p0 p1 p2 p3]]
            (let [c [(/ (- (* 3.0 (+ (p1 0) (p2 0))) (p0 0) (p3 0)) 4.0)
                     (/ (- (* 3.0 (+ (p1 1) (p2 1))) (p0 1) (p3 1)) 4.0)]]
              [(p0 0) (p0 1) (c 0) (c 1) (p3 0) (p3 1)]))
          pieces)))

(defn lower
  "Path and tolerance → {:quads :cubics :from-cubics}: every segment as
   quadratics, lines as quads with the control at the midpoint."
  [path tol]
  (reduce
   (fn [acc sp0]
     (let [sp (v/explicit-close sp0)]
       (first
        (reduce (fn [[acc cur] s]
                  (let [p (:p s)]
                    [(case (:kind s)
                       :line (if (> (v/dist cur p) 1.0e-12)
                               (update acc :quads conj [(cur 0) (cur 1) (/ (+ (cur 0) (p 0)) 2.0) (/ (+ (cur 1) (p 1)) 2.0) (p 0) (p 1)])
                               acc)
                       :quad (update acc :quads conj [(cur 0) (cur 1) ((:c s) 0) ((:c s) 1) (p 0) (p 1)])
                       :cubic (let [qs (cubic->quads cur (:c1 s) (:c2 s) p tol)]
                                (-> acc
                                    (update :quads into qs)
                                    (update :cubics inc)
                                    (update :from-cubics + (count qs)))))
                     p]))
                [acc (:start sp)]
                (:segments sp)))))
   {:quads [] :cubics 0 :from-cubics 0}
   (:subpaths path)))

;; ---- bands ----

(defn- quad-box [[x1 y1 cx cy x3 y3]]
  [(min x1 cx x3) (min y1 cy y3) (max x1 cx x3) (max y1 cy y3)])

(defn pack-quads
  "Quads and options → the pack, or nil for no quads.

   Bands: ceil(sqrt(n/2)) rows and as many columns (clamped 1..64; :bands
   false gives one of each); each curve is listed in every band its box
   touches; row lists sorted by the curve's max x descending and column
   lists by max y descending, which is what lets the shader stop early.
   :band-xf maps a local point to a band index."
  [quads {:keys [bands] :or {bands true}}]
  (let [n (count quads)]
    (when (pos? n)
      (let [boxes (mapv quad-box quads)
            [x0 y0 x1 y1] (reduce (fn [[x0 y0 x1 y1] [bx0 by0 bx1 by1]]
                                    [(min x0 bx0) (min y0 by0) (max x1 bx1) (max y1 by1)])
                                  [##Inf ##Inf ##-Inf ##-Inf] boxes)
            hb (if bands (int (v/clamp (Math/ceil (Math/sqrt (/ n 2.0))) 1 64)) 1)
            vb hb
            hy (let [d (- y1 y0)] (if (zero? d) 1.0 d))
            vx (let [d (- x1 x0)] (if (zero? d) 1.0 d))
            h-lists (reduce (fn [lists i]
                              (let [b (nth boxes i)
                                    j0 (int (v/clamp (Math/floor (* (/ (- (b 1) y0) hy) hb)) 0 (dec hb)))
                                    j1 (int (v/clamp (Math/floor (* (/ (- (b 3) y0) hy) hb)) 0 (dec hb)))]
                                (reduce (fn [lists j] (update lists j conj i)) lists (range j0 (inc j1)))))
                            (vec (repeat hb [])) (range n))
            v-lists (reduce (fn [lists i]
                              (let [b (nth boxes i)
                                    k0 (int (v/clamp (Math/floor (* (/ (- (b 0) x0) vx) vb)) 0 (dec vb)))
                                    k1 (int (v/clamp (Math/floor (* (/ (- (b 2) x0) vx) vb)) 0 (dec vb)))]
                                (reduce (fn [lists k] (update lists k conj i)) lists (range k0 (inc k1)))))
                            (vec (repeat vb [])) (range n))
            h-lists (mapv (fn [l] (vec (sort-by (fn [i] (- (nth (nth boxes i) 2))) l))) h-lists)
            v-lists (mapv (fn [l] (vec (sort-by (fn [i] (- (nth (nth boxes i) 3))) l))) v-lists)]
        {:count n
         :quads quads
         :boxes boxes
         :bbox [x0 y0 x1 y1]
         :h-bands hb :v-bands vb
         :h-lists h-lists :v-lists v-lists
         :band-xf [(/ vb vx) (/ hb hy) (/ (* (- x0) vb) vx) (/ (* (- y0) hb) hy)]
         :band-texels (+ hb vb (reduce + (map count h-lists)) (reduce + (map count v-lists)))}))))

(defn pack-region
  "Region outline path, tolerance, options → {:pack :cubics :from-cubics},
   :pack nil for an empty region."
  [path tol opts]
  (let [{:keys [quads cubics from-cubics]} (lower path tol)]
    {:pack (pack-quads quads opts) :cubics cubics :from-cubics from-cubics}))

;; ---- the CPU twin ----

(defn- sign-bit
  "Number → 1 when its sign bit is set (negative or -0.0), else 0."
  [y]
  (if (or (neg? y) (and (zero? y) (neg? (/ 1.0 y)))) 1 0))

(defn root-code
  "Three y coordinates relative to the query → the crossing classes, the
   Slug lookup: bit 0 for the first root, bit 8 for the second."
  [y1 y2 y3]
  (let [shift (bit-or (sign-bit y1) (bit-shift-left (sign-bit y2) 1) (bit-shift-left (sign-bit y3) 2))]
    (bit-and (bit-shift-right 0x2E74 shift) 0x0101)))

(defn solve-axis
  "Relative control points → the two x values where the quadratic crosses
   y = 0, [x(t1) x(t2)]."
  [p1x p1y p2x p2y p3x p3y]
  (let [ay (+ (- p1y (* 2.0 p2y)) p3y)
        by (- p1y p2y)
        ax (+ (- p1x (* 2.0 p2x)) p3x)
        bx (- p1x p2x)
        d (Math/sqrt (max (- (* by by) (* ay p1y)) 0.0))
        [t1 t2] (if (< (Math/abs ay) (/ 1.0 65536.0))
                  (let [t (/ p1y (* 2.0 by))] [t t])
                  [(/ (- by d) ay) (/ (+ by d) ay)])]
    [(+ (* (- (* ax t1) (* 2.0 bx)) t1) p1x)
     (+ (* (- (* ax t2) (* 2.0 bx)) t2) p1x)]))

(defn- band-index [pack x y]
  (let [[sx sy ox oy] (:band-xf pack)]
    [(int (v/clamp (Math/floor (+ (* x sx) ox)) 0 (dec (:v-bands pack))))
     (int (v/clamp (Math/floor (+ (* y sy) oy)) 0 (dec (:h-bands pack))))]))

(defn winding-at
  "Pack and local point → the integer winding number there, counted along
   the row band the way the shader does."
  [pack x y]
  (let [[_ bi] (band-index pack x y)
        quads (:quads pack)]
    (reduce (fn [w i]
              (let [[x1 y1 x2 y2 x3 y3] (nth quads i)
                    p1x (- x1 x) p1y (- y1 y) p2x (- x2 x) p2y (- y2 y) p3x (- x3 x) p3y (- y3 y)]
                (if (neg? (max p1x p2x p3x))
                  (reduced w)
                  (let [code (root-code p1y p2y p3y)]
                    (if (zero? code)
                      w
                      (let [[r1 r2] (solve-axis p1x p1y p2x p2y p3x p3y)]
                        (cond-> w
                          (and (pos? (bit-and code 1)) (pos? r1)) inc
                          (and (> code 1) (pos? r2)) dec)))))))
            0 (nth (:h-lists pack) bi))))

(defn inside?
  "Pack, rule, local point → membership under the rule."
  [pack rule x y]
  (let [w (winding-at pack x y)]
    (if (= :even-odd rule) (odd? w) (not (zero? w)))))

(defn- sat [x] (v/clamp x 0.0 1.0))

(defn coverage-at
  "Pack, local point, device pixels per local unit on each axis, rule →
   the filler's coverage estimate at that point, in [0, 1].

   Signed root crossings on both axes, a weight per axis from how close the
   nearest crossing is, the weighted mix or the smaller magnitude,
   saturated; even-odd folds the absolute crossings through a triangle
   wave."
  [pack x y ppu-x ppu-y rule]
  (let [[bj bi] (band-index pack x y)
        quads (:quads pack)
        h (reduce (fn [[xcov xwgt xabs :as acc] i]
                    (let [[x1 y1 x2 y2 x3 y3] (nth quads i)
                          p1x (- x1 x) p1y (- y1 y) p2x (- x2 x) p2y (- y2 y) p3x (- x3 x) p3y (- y3 y)]
                      (if (< (* (max p1x p2x p3x) ppu-x) -0.5)
                        (reduced acc)
                        (let [code (root-code p1y p2y p3y)]
                          (if (zero? code)
                            acc
                            (let [[r1 r2] (solve-axis p1x p1y p2x p2y p3x p3y)
                                  r1 (* r1 ppu-x) r2 (* r2 ppu-x)
                                  [xcov xwgt xabs] (if (pos? (bit-and code 1))
                                                     (let [c (sat (+ r1 0.5))]
                                                       [(+ xcov c) (max xwgt (sat (- 1.0 (* 2.0 (Math/abs r1))))) (+ xabs c)])
                                                     [xcov xwgt xabs])]
                              (if (> code 1)
                                (let [c (sat (+ r2 0.5))]
                                  [(- xcov c) (max xwgt (sat (- 1.0 (* 2.0 (Math/abs r2))))) (+ xabs c)])
                                [xcov xwgt xabs])))))))
                  [0.0 0.0 0.0] (nth (:h-lists pack) bi))
        vv (reduce (fn [[ycov ywgt yabs :as acc] i]
                     (let [[x1 y1 x2 y2 x3 y3] (nth quads i)
                           p1x (- x1 x) p1y (- y1 y) p2x (- x2 x) p2y (- y2 y) p3x (- x3 x) p3y (- y3 y)]
                       (if (< (* (max p1y p2y p3y) ppu-y) -0.5)
                         (reduced acc)
                         (let [code (root-code p1x p2x p3x)]
                           (if (zero? code)
                             acc
                             (let [[r1 r2] (solve-axis p1y p1x p2y p2x p3y p3x)
                                   r1 (* r1 ppu-y) r2 (* r2 ppu-y)
                                   [ycov ywgt yabs] (if (pos? (bit-and code 1))
                                                      (let [c (sat (+ r1 0.5))]
                                                        [(- ycov c) (max ywgt (sat (- 1.0 (* 2.0 (Math/abs r1))))) (+ yabs c)])
                                                      [ycov ywgt yabs])]
                               (if (> code 1)
                                 (let [c (sat (+ r2 0.5))]
                                   [(+ ycov c) (max ywgt (sat (- 1.0 (* 2.0 (Math/abs r2))))) (+ yabs c)])
                                 [ycov ywgt yabs])))))))
                   [0.0 0.0 0.0] (nth (:v-lists pack) bj))
        [xcov xwgt xabs] h
        [ycov ywgt yabs] vv
        combine (fn [xc yc]
                  (let [weighted (/ (Math/abs (+ (* xc xwgt) (* yc ywgt))) (max (+ xwgt ywgt) (/ 1.0 65536.0)))]
                    (sat (max weighted (min (Math/abs xc) (Math/abs yc))))))]
    (if (= :even-odd rule)
      (let [tri (fn [a] (- 1.0 (Math/abs (- (mod a 2.0) 1.0))))]
        (combine (tri xabs) (tri yabs)))
      (combine xcov ycov))))

;; ---- cover ----

(defn- touched-cells
  "Pack, cell origin, cell size, counts, margin → the set of [i j] cells a
   curve passes through: every quadratic is flattened, each line cut into
   pieces no longer than a cell, and each piece's box, grown by the margin,
   marks the cells it overlaps. Cost follows the outline's length in cells,
   not the box's area."
  [pack X0 Y0 cw ch nx ny margin]
  (let [cell-range (fn [x0 y0 x1 y1]
                     (let [i0 (int (v/clamp (Math/floor (/ (- x0 X0) cw)) 0 (dec nx)))
                           i1 (int (v/clamp (Math/floor (/ (- x1 X0) cw)) 0 (dec nx)))
                           j0 (int (v/clamp (Math/floor (/ (- y0 Y0) ch)) 0 (dec ny)))
                           j1 (int (v/clamp (Math/floor (/ (- y1 Y0) ch)) 0 (dec ny)))]
                       (for [j (range j0 (inc j1)) i (range i0 (inc i1))] [i j])))
        cell (min cw ch)]
    (reduce (fn [cells [x1 y1 cx cy x3 y3]]
              (let [a [x1 y1] c [cx cy] b [x3 y3]
                    m (v/quad-steps a c b (/ cell 4.0))
                    pts (mapv (fn [j] (v/quad-at a c b (/ j m))) (range (inc m)))]
                (reduce (fn [cells [p q]]
                          (let [n (max 1 (int (Math/ceil (/ (v/dist p q) cell))))]
                            (reduce (fn [cells k]
                                      (let [s (v/lerp p q (/ k n)) e (v/lerp p q (/ (inc k) n))]
                                        (into cells (cell-range (- (min (s 0) (e 0)) margin) (- (min (s 1) (e 1)) margin)
                                                                (+ (max (s 0) (e 0)) margin) (+ (max (s 1) (e 1)) margin)))))
                                    cells (range n))))
                        cells (map vector pts (rest pts)))))
            #{} (:quads pack))))

(defn cover
  "Pack and options → {:rects [[x0 y0 x1 y1]] :tested :area}: the cover
   the filler draws. :mode :box gives the bounds plus :margin; :cells
   splits the bounds into cells of :cell local units and keeps the cells a
   curve passes through plus interior cells by winding at the centre (under
   :rule), so a thin diagonal stroke does not pay for its whole box."
  [pack {:keys [mode margin cell rule] :or {mode :box margin 0.0}}]
  (let [[bx0 by0 bx1 by1] (:bbox pack)
        X0 (- bx0 margin) Y0 (- by0 margin) X1 (+ bx1 margin) Y1 (+ by1 margin)
        rects (if (= :cells mode)
                (let [cell (or cell 32.0)
                      nx (int (v/clamp (Math/ceil (/ (- X1 X0) cell)) 1 96))
                      ny (int (v/clamp (Math/ceil (/ (- Y1 Y0) cell)) 1 96))
                      cw (/ (- X1 X0) nx) ch (/ (- Y1 Y0) ny)
                      touched (touched-cells pack X0 Y0 cw ch nx ny margin)]
                  (vec (for [j (range ny) i (range nx)
                             :let [cx0 (+ X0 (* i cw)) cy0 (+ Y0 (* j ch)) cx1 (+ cx0 cw) cy1 (+ cy0 ch)
                                   hit? (or (contains? touched [i j])
                                            (inside? pack rule (/ (+ cx0 cx1) 2.0) (/ (+ cy0 cy1) 2.0)))]
                             :when hit?]
                         [cx0 cy0 cx1 cy1])))
                [[X0 Y0 X1 Y1]])]
    {:rects rects
     :tested (if (= :cells mode) (count rects) 1)
     :area (reduce + 0.0 (map (fn [[x0 y0 x1 y1]] (* (- x1 x0) (- y1 y0))) rects))}))

;; ---- snapping, a policy at frame rate ----

(defn snap-path
  "Path, local→device and device→local maps → the path with every knot on
   the device grid and its handles carried along."
  [path to-device from-device]
  (let [snap (fn [p] (let [[dx dy] (to-device p)] (from-device [(Math/round (double dx)) (Math/round (double dy))])))]
    (update path :subpaths
            (fn [subpaths]
              (mapv (fn [sp]
                      (let [start (snap (:start sp))
                            [segments _ _]
                            (reduce (fn [[segments prev prev-s] s]
                                      (let [p (snap (:p s))
                                            d-prev (v/sub prev-s prev)
                                            d-end (v/sub p (:p s))
                                            ns (case (:kind s)
                                                 :line (v/line p)
                                                 :quad (v/quad (v/add (:c s) (v/scale (v/add d-prev d-end) 0.5)) p)
                                                 :cubic (v/cubic (v/add (:c1 s) d-prev) (v/add (:c2 s) d-end) p))]
                                        [(conj segments ns) (:p s) p]))
                                    [[] (:start sp) start]
                                    (:segments sp))]
                        (assoc sp :start start :segments segments)))
                    subpaths)))))

;; ---- distance to an outline, for boundary answers ----

(defn outline-distance
  "Pack (or any quads) and local point → the distance to the nearest curve,
   by flattening each quadratic finely. For boundary slop, not per pixel."
  [pack [px py]]
  (reduce (fn [best [x1 y1 cx cy x3 y3]]
            (let [a [x1 y1] c [cx cy] b [x3 y3]
                  m (v/quad-steps a c b 0.01)
                  pts (mapv (fn [j] (v/quad-at a c b (/ j m))) (range (inc m)))]
              (reduce (fn [best [p q]]
                        (let [d (v/sub q p)
                              den (v/dot d d)
                              t (if (zero? den) 0.0 (v/clamp (/ (v/dot (v/sub [px py] p) d) den) 0.0 1.0))]
                          (min best (v/dist [px py] (v/add p (v/scale d t))))))
                      best (map vector pts (rest pts)))))
          ##Inf (:quads pack)))
