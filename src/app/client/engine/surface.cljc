(ns app.client.engine.surface
  "The compositor's pure CPU runner: surface values, paint and sample.
   Takes explicit surfaces, coverage, paint and domains; gives new values.
   Holds nothing; paint copies its input. compositor.cljs owns the physical
   targets. Evidence: surface_test.clj and path/pickup_test.clj."
  (:refer-clojure :exclude [new])
  (:require [app.client.engine.value-bytes :as vb]
            [app.client.engine.schema :as schema]
            [app.client.engine.color :as color]
            [app.client.engine.surface-png :as png]))

(defn- fail! [reason message] (throw (ex-info message {:reason reason})))

(defn validate!
  "Surface declaration/value → itself; only the landed domain/filter/color
   vocabulary is admitted, and a value's array must match its dimensions."
  [{:keys [width height domain filter color data initial surface/id] :as s}]
  (when-not (and (string? id) (integer? width) (pos? width) (integer? height) (pos? height))
    (fail! :dimensions "A surface needs an id and positive integer dimensions"))
  (when-not (= color :linear-premultiplied-rgba) (fail! :color "Unsupported surface color"))
  (when-not (= filter :nearest) (fail! :filter "Unsupported surface filter"))
  (case (:kind domain)
    :plane (let [[a b c d :as m] (:map domain)]
             (when-not (and (= 6 (count m)) (every? schema/finite-number? m)
                            (not (zero? (- (* a d) (* b c)))))
               (fail! :domain "A plane surface needs an invertible finite affine map")))
    :chart (let [[_ _ w h :as rect] (:rect domain)]
             (when-not (and (keyword? (:chart domain)) (= 4 (count rect))
                            (every? schema/finite-number? rect) (pos? w) (pos? h))
               (fail! :domain "A chart surface needs a named chart and positive finite rectangle")))
    (fail! :domain "Unsupported surface domain"))
  (when (and (contains? s :data) (not (and (vb/floats? data) (= (* width height 4) (alength ^floats data)))))
    (fail! :array-length "Surface payload differs from width × height × 4"))
  (when (and (contains? s :initial) (not (and (= 4 (count initial)) (every? schema/finite-number? initial))))
    (fail! :initial "A surface needs four finite initial channels"))
  s)

(defn new
  "Declaration → fresh RGBA32F surface, filled with :initial."
  [declaration]
  (validate! declaration)
  (let [{:keys [width height initial surface/id]} declaration
        _ (when-not initial (fail! :initial "Missing surface initial color"))
        ^floats data (vb/floats (* width height 4))
        fill (mapv float initial)]
    (dotimes [i (alength data)] (aset data i (float (nth fill (mod i 4)))))
    (assoc (dissoc declaration :initial :subject :changed) :revision 0 :data data :key (str id "@initial") :parent nil)))

(defn to-texel
  "Surface and local/support point → continuous texel coordinates. A chart
   domain is supplied by the kind; there is no engine dependency on a kind."
  ([surface point] (to-texel surface point {}))
  ([surface [x y :as point] ctx]
   (if (= :plane (get-in surface [:domain :kind]))
     (let [[a b c d e f] (get-in surface [:domain :map])]
       [(+ (* a x) (* c y) e) (+ (* b x) (* d y) f)])
     (if-let [f (get-in ctx [:domains (get-in surface [:domain :kind]) :to-texel])]
       (f surface point)
       (fail! :domain "The kind must supply this domain's to-texel operation")))))

(defn to-point
  "Surface and texel index → local position of that texel's centre."
  ([surface x y] (to-point surface x y {}))
  ([surface x y ctx]
   (if (= :plane (get-in surface [:domain :kind]))
     (let [[a b c d e f] (get-in surface [:domain :map]) det (- (* a d) (* b c))
           tx (- (+ x 0.5) e) ty (- (+ y 0.5) f)]
       [(/ (- (* d tx) (* c ty)) det) (/ (- (* a ty) (* b tx)) det)])
     (if-let [f (get-in ctx [:domains (get-in surface [:domain :kind]) :to-point])]
       (f surface x y)
       (fail! :domain "The kind must supply this domain's to-point operation")))))

(defn mix [a b amount] (mapv #(+ (* %1 (- 1.0 amount)) (* %2 amount)) a b))
(defn over [src dst] (mapv #(+ %1 (* %2 (- 1.0 (nth src 3)))) src dst))
(def blends {:source-over over})

(defn paint
  "Surface, region, color paint and {:coverage :clips :bounds :key} → new
   surface. Coverage is at texel indexes. Region semantics belong to the
   kind's supplied coverage. Bounds are half-open and clipped to the grid."
  [surface _region {:keys [kind rgba opacity blend] :or {opacity 1.0 blend :source-over}} opts]
  (validate! surface)
  (when-not (= :color kind) (fail! :paint-kind "Only color paint is landed"))
  (when-not (and (= 4 (count rgba)) (every? schema/finite-number? rgba)
                 (schema/finite-number? opacity) (<= 0 opacity 1))
    (fail! :paint "Paint needs finite RGBA and opacity in [0,1]"))
  (when-not (:key opts) (fail! :key "Paint needs its result key"))
  (let [blend-fn (or (get blends blend)
                     (throw (ex-info "Unsupported blend" {:reason :unsupported-blend :supported (vec (keys blends))})))
        {:keys [width height]} surface
        ^floats data (:data surface)
        result #?(:clj (aclone data) :cljs (.slice data))
        [x0 y0 x1 y1] (or (:bounds opts) [0 0 width height])
        coverage (:coverage opts)
        changed (reduce (fn [changed [x y]]
                          (let [c (reduce * (coverage x y) (map #(% x y) (:clips opts)))]
                            (when-not (and (schema/finite-number? c) (<= 0 c 1))
                              (fail! :coverage "Coverage must be in [0,1]"))
                            (if (zero? c) changed
                                (let [i (* 4 (+ x (* y width)))
                                      src (mapv #(* % opacity c) rgba)
                                      dst (mapv #(aget data (+ i %)) (range 4))
                                      out (blend-fn src dst)]
                                  (dotimes [k 4] (aset result (+ i k) (float (nth out k))))
                                  (inc changed)))))
                        0 (for [y (range (max 0 (int (Math/floor y0))) (min height (int (Math/ceil y1))))
                                x (range (max 0 (int (Math/floor x0))) (min width (int (Math/ceil x1))))] [x y]))]
    (assoc (dissoc surface :subject) :data result :key (:key opts) :parent (:key surface)
           :revision (inc (:revision surface)) :changed changed)))

(defn snapshot
  "Stack → whole layer inputs, point and filter without sampling. Callable
   adapters never enter the returned value or a continuation."
  [stack point filter ctx]
  {:layers (mapv (fn [layer]
                   (if (:layer/kind layer)
                     (if-let [f (:snapshot layer)] (f point filter ctx)
                         (fail! :snapshot "A non-surface layer must declare its snapshot"))
                     layer)) stack)
   :point point :filter filter})

(defn- sample-layer [layer point filter ctx]
  (if (:layer/kind layer)
    ((:sample layer) point filter ctx)
    (do
      (validate! layer)
      (let [[tx ty] (to-texel layer point ctx) {:keys [width height]} layer
            ^floats data (:data layer)
            x (Math/floor tx) y (Math/floor ty)]
        (if (and (<= 0 x) (< x width) (<= 0 y) (< y height))
          (let [i (* 4 (+ (int x) (* (int y) width)))]
            {:status :resolved :color (mapv #(aget data (+ i %)) (range 4)) :covered? true
             :contributors [(str (:surface/id layer) "@" (:revision layer))]})
          {:status :resolved :color [0 0 0 0] :covered? false :contributors []})))))

(defn sample
  "Layers bottom to top, point, filter and domain context → read. Pending
   layers contribute their known partial color, and layers above still
   compose. Missing policy or unsupported reads stop composition. Outside a
   surface is transparent.
   Evidence: surface_test.clj and region3d/brush_test.clj."
  [stack point filter ctx]
  (when-not (= :nearest filter) (fail! :filter "Only nearest sampling is landed"))
  (let [result
        (reduce (fn [acc layer]
                  (let [r (sample-layer layer point filter ctx)]
                    (case (:status r)
                      (:needs-policy :unsupported) (reduced r)
                      (:resolved :pending)
                      (let [pending? (or (= :pending (:status acc)) (= :pending (:status r)))
                            color (over (if (= :pending (:status r)) (:partial r) (:color r)) (:color acc))
                            names (into (:contributors acc) (if (= :pending (:status r)) (:known r) (:contributors r)))]
                        {:status (if pending? :pending :resolved) :color color :contributors names
                         :covered? (or (:covered? acc) (:covered? r) (seq names))
                         :missing (into (:missing acc) (:missing r))})
                      (fail! :layer "A layer must return a declared read status"))))
                {:status :resolved :color [0.0 0.0 0.0 0.0] :contributors [] :covered? false :missing []} stack)
        result (if (= :pending (:status result))
                 (-> result (assoc :partial (:color result) :known (:contributors result))
                     (dissoc :color :contributors))
                 (dissoc result :missing))]
    (assoc result :covered? (boolean (:covered? result)) :snapshot (snapshot stack point filter ctx))))

(defn validate-value! [value]
  (cond
    (map? value) (do (when (contains? value :surface/id) (validate! value))
                    (doseq [v (vals value)] (validate-value! v)))
    (coll? value) (doseq [v value] (validate-value! v))) value)

(defn encode [value] (vb/encode (validate-value! value)))
(defn decode [bytes] (validate-value! (vb/decode bytes)))

(defn png-bytes
  "Surface → deterministic straight-sRGB RGBA8 PNG bytes. Float data stays
   linear and unchanged; this conversion is only the exported picture."
  [{:keys [width height] :as surface}]
  (validate! surface)
  (let [^floats data (:data surface)]
    (png/encode width height
                (vec (mapcat (fn [i]
                               (let [a (double (aget data (+ i 3)))
                                     channel (fn [v] (int (Math/floor (+ 0.5 (* 255 (max 0.0 (min 1.0 v)))))))]
                                 (conj (mapv #(channel (color/linear->srgb-channel
                                                       (if (pos? a) (/ (aget data (+ i %)) a) 0.0))) (range 3))
                                       (channel a))))
                             (range 0 (alength data) 4))))))
