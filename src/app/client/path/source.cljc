(ns app.client.path.source
  "Turn what a tool saw into a path value.

   Input: a source record (a pen's samples, a designer's anchors, a
   rectangle's numbers) and the tool's numbers. Output: {:path :meta}: the
   path value and its source metadata. No retained
   state; no camera.

   These are sources, not kinds: a pen, a formula and a designer all compile
   to the one value. The pen is the most work: streamline (a low-pass over
   positions), speed, simulated pressure when the device gave none, the
   width expression per sample (data, run by path/width through engine/executor), tapers, a
   fit at tolerance τ (decimation), then the interpolating spline through the
   knots, or a polyline when asked. Samples are the event truth; the path is
   derived from them at a declared tolerance.

   Folder map: README.md."
  (:require [app.client.path.width :as width]
            [app.client.engine.schema :as schema]
            [app.client.path.value :as v]))

(def kappa 0.5522847498)

(defn arc-cubic
  "Centre, radius, start and end angle (≤ 90° apart) → one cubic segment
   approximating the arc, the standard κ bridge."
  [[cx cy] r a0 a1]
  (let [d (- a1 a0)
        k (* (/ 4.0 3.0) (Math/tan (/ d 4.0)) r)
        p0 [(+ cx (* r (Math/cos a0))) (+ cy (* r (Math/sin a0)))]
        p3 [(+ cx (* r (Math/cos a1))) (+ cy (* r (Math/sin a1)))]]
    (v/cubic [(- (p0 0) (* k (Math/sin a0))) (+ (p0 1) (* k (Math/cos a0)))]
             [(+ (p3 0) (* k (Math/sin a1))) (- (p3 1) (* k (Math/cos a1)))]
             p3)))

(defn build-rect
  "Rect source {:x :y :w :h :r?} → {:path :meta}: four lines and, with a
   radius, four arcs as cubics. A formula shape; the generator is code."
  [{:keys [x y w h] :as source}]
  (let [r (v/clamp (or (:r source) 0.0) 0.0 (/ (min w h) 2.0))
        knots (fn [n] (vec (for [i (range n)] {:id (str "k" i)})))]
    (if (pos? r)
      (let [half Math/PI]
        {:path {:subpaths
                [{:closed? true
                  :start [(+ x r) y]
                  :segments [(v/line [(- (+ x w) r) y])
                             (arc-cubic [(- (+ x w) r) (+ y r)] r (- (/ half 2.0)) 0.0)
                             (v/line [(+ x w) (- (+ y h) r)])
                             (arc-cubic [(- (+ x w) r) (- (+ y h) r)] r 0.0 (/ half 2.0))
                             (v/line [(+ x r) (+ y h)])
                             (arc-cubic [(+ x r) (- (+ y h) r)] r (/ half 2.0) half)
                             (v/line [x (+ y r)])
                             (arc-cubic [(+ x r) (+ y r)] r half (* 1.5 half))]
                  :knots (knots 9)}]}
         :meta {:kind :rect :lines 4 :arcs 4}})
      {:path {:subpaths
              [{:closed? true
                :start [x y]
                :segments [(v/line [(+ x w) y]) (v/line [(+ x w) (+ y h)])
                           (v/line [x (+ y h)]) (v/line [x y])]
                :knots (knots 5)}]}
       :meta {:kind :rect :lines 4 :arcs 0}})))

(defn build-anchors
  "Anchors source {:contours [{:closed? :anchors [{:id :p :in :out}]}]} →
   {:path :meta}: cubics between anchors, a line where both handles are
   missing. Pure copying; a designer's outline already is a path."
  [source]
  (let [subpaths
        (for [contour (:contours source)
              :let [anchors (vec (:anchors contour))]
              :when (>= (count anchors) 2)]
          (let [n (if (:closed? contour) (count anchors) (dec (count anchors)))
                segments (vec (for [i (range n)]
                                (let [a (nth anchors i)
                                      b (nth anchors (mod (inc i) (count anchors)))]
                                  (if (and (nil? (:out a)) (nil? (:in b)))
                                    (v/line (:p b))
                                    (v/cubic (or (:out a) (:p a)) (or (:in b) (:p b)) (:p b))))))]
            {:closed? (boolean (:closed? contour))
             :start (:p (first anchors))
             :segments segments
             :knots (vec (for [i (range (inc n))]
                           (let [a (nth anchors (mod i (count anchors)))]
                             {:id (or (:id a) (str "a" (mod i (count anchors))))})))}))
        path {:subpaths (vec subpaths)}
        st (v/stats path)]
    {:path path :meta {:kind :anchors :cubics (:cubics st) :lines (:lines st)}}))

(defn decimate
  "Points with :x :y and a tolerance → the subset that keeps the polyline
   within the tolerance (Ramer–Douglas–Peucker), sample maps preserved."
  [points eps]
  (let [n (count points)]
    (if (< n 3)
      (vec points)
      (let [xy (fn [p] [(:x p) (:y p)])
            keep (loop [stack [[0 (dec n)]] keep #{0 (dec n)}]
                   (if-let [[a b] (peek stack)]
                     (let [stack (pop stack)
                           A (xy (nth points a)) B (xy (nth points b))
                           d (v/sub B A) L (v/len d)
                           [best bi] (reduce (fn [[best bi] i]
                                               (let [P (xy (nth points i))
                                                     dd (if (> L 1.0e-9)
                                                          (/ (Math/abs (double (v/cross d (v/sub P A)))) L)
                                                          (v/dist P A))]
                                                 (if (> dd best) [dd i] [best bi])))
                                             [-1.0 -1] (range (inc a) b))]
                       (if (and (> best eps) (pos? bi))
                         (recur (conj stack [a bi] [bi b]) (conj keep bi))
                         (recur stack keep)))
                     keep))]
        (vec (keep-indexed (fn [i p] (when (keep i) p)) points))))))

(defn build-pen
  "Pen source {:samples [[x y pressure? time?]]} and tool → {:path :meta}.

   Streamline k pulls each position toward the previous one by k; speed is
   units per millisecond; simulated pressure follows tldraw's rule from
   speed against the tool size; the width is the expression per sample, then
   the tapers; fit τ > 0 decimates, :polyline or fewer than three knots gives
   lines, otherwise Catmull-Rom cubics through the knots. Knots keep the
   sample index as id and carry width, pressure and time."
  [source tool]
  (let [raw (vec (map-indexed (fn [i [x y p t]]
                                {:x x :y y :p (if (number? p) p 0.5) :t (if (number? t) t (* i 8)) :id (str "s" i)})
                              (:samples source)))
        {:keys [width-fn names error]} (width/width-function tool)
        meta {:kind :pen :samples (count raw) :knots 0
              :streamline (or (:streamline tool) 0) :fit (:fit tool)
              :width-names names :taper-start (or (:taper-start tool) 0)
              :taper-end (or (:taper-end tool) 0)
              :simulated? (boolean (:simulate-pressure? tool)) :error error}]
    (if (empty? raw)
      {:path v/empty-path :meta meta}
      (let [k (v/clamp (or (:streamline tool) 0) 0.0 0.95)
            smoothed (reduce (fn [acc s]
                               (if (empty? acc)
                                 (conj acc s)
                                 (let [q (peek acc)]
                                   (conj acc (assoc s :x (+ (:x q) (* (- (:x s) (:x q)) (- 1.0 k)))
                                                    :y (+ (:y q) (* (- (:y s) (:y q)) (- 1.0 k))))))))
                             [] raw)
            with-arc (loop [i 1 acc [(assoc (first smoothed) :s 0.0 :v 0.0)] L 0.0]
                       (if (= i (count smoothed))
                         [acc L]
                         (let [q (nth smoothed i) prev (nth smoothed (dec i))
                               d (v/dist [(:x q) (:y q)] [(:x prev) (:y prev)])
                               L (+ L d)]
                           (recur (inc i)
                                  (conj acc (assoc q :s L :v (/ d (max 1.0e-3 (- (:t q) (:t prev))))))
                                  L))))
            [points L] with-arc
            points (if (:simulate-pressure? tool)
                     (let [size (or (:size tool) 8)]
                       (loop [i 0 prev 0.5 acc []]
                         (if (= i (count points))
                           acc
                           (let [q (nth points i)]
                             (if (zero? i)
                               (recur (inc i) prev (conj acc (assoc q :p prev)))
                               (let [pr (nth points (dec i))
                                     sp (min 1.0 (/ (v/dist [(:x q) (:y q)] [(:x pr) (:y pr)]) size))
                                     rp (min 1.0 (- 1.0 sp))
                                     next (min 1.0 (+ prev (* (- rp prev) (* sp 0.275))))]
                                 (recur (inc i) next (conj acc (assoc q :p next)))))))))
                     points)
            taper-start (:taper-start meta) taper-end (:taper-end meta)
            points (mapv (fn [q]
                           (let [w (width-fn (:p q) (if (pos? L) (/ (:s q) L) 0.0) (:v q))
                                 w (cond-> w
                                     (pos? taper-start) (* (v/clamp (/ (:s q) taper-start) 0.0 1.0))
                                     (pos? taper-end) (* (v/clamp (/ (- L (:s q)) taper-end) 0.0 1.0)))]
                             (assoc q :w (max 0.0 w))))
                         points)
            fit (:fit tool)
            knots (if (and (number? fit) (pos? fit)) (decimate points fit) points)
            polyline? (or (= :polyline fit) (< (count knots) 3))
            xy (fn [q] [(:x q) (:y q)])
            segments (if polyline?
                       (mapv (fn [q] (v/line (xy q))) (rest knots))
                       (vec (for [i (range (dec (count knots)))]
                              (let [p0 (xy (nth knots i)) p3 (xy (nth knots (inc i)))
                                    pm (xy (nth knots (max 0 (dec i))))
                                    pp (xy (nth knots (min (dec (count knots)) (+ i 2))))]
                                (v/cubic (v/add p0 (v/scale (v/sub p3 pm) (/ 1.0 6.0)))
                                         (v/sub p3 (v/scale (v/sub pp p0) (/ 1.0 6.0)))
                                         p3)))))]
        {:path {:subpaths [{:closed? false
                            :start (xy (first knots))
                            :segments segments
                            :knots (mapv (fn [q] {:id (:id q) :width (:w q) :pressure (:p q) :time (:t q)}) knots)}]}
         :meta (assoc meta :knots (count knots) :length L
                      :spline (if polyline? :polyline :catmull-rom))}))))

(defn build
  "Source and tool → {:path :meta} by the source's kind; an unknown kind
   throws a named error. The kind names the capability;
   nothing here reads a tool's name."
  [source tool]
  (case (:kind source)
    :pen (build-pen source (or tool {}))
    :rect (build-rect source)
    :anchors (build-anchors source)
    (throw (ex-info "Unknown source capability" {:error-type :path/source-kind :kind (:kind source)}))))
