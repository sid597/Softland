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
  (let [[a b c d :as m] (:map domain)]
    (when-not (and (= :plane (:kind domain)) (= 6 (count m))
                   (every? schema/finite-number? m) (not (zero? (- (* a d) (* b c)))))
      (fail! :domain "A plane surface needs an invertible finite affine map")))
  (when (and (contains? s :data) (not (and (vb/floats? data) (= (* width height 4) (alength data)))))
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
        data (vb/floats (* width height 4))]
    (dotimes [i (alength data)] (aset data i (float (nth initial (mod i 4)))))
    (assoc (dissoc declaration :initial :subject :changed) :revision 0 :data data :key (str id "@initial") :parent nil)))

(defn to-texel [surface [x y]]
  (let [[a b c d e f] (get-in surface [:domain :map])]
    [(+ (* a x) (* c y) e) (+ (* b x) (* d y) f)]))

(defn to-point
  "Surface and texel index → local position of that texel's centre."
  [surface x y]
  (let [[a b c d e f] (get-in surface [:domain :map]) det (- (* a d) (* b c))
        tx (- (+ x 0.5) e) ty (- (+ y 0.5) f)]
    [(/ (- (* d tx) (* c ty)) det) (/ (- (* a ty) (* b tx)) det)]))

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
        {:keys [width height data]} surface
        result #?(:clj (aclone ^floats data) :cljs (.slice data))
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

(defn snapshot [stack point filter _ctx] {:layers (vec stack) :point point :filter filter})

(defn sample
  "Surface layers bottom to top, local point, filter and domain context →
   resolved read. Outside a layer is transparent and is not a contributor.
   Pending/non-surface layers and chart domains belong to slice B."
  [stack point filter ctx]
  (when-not (= :nearest filter) (fail! :filter "Only nearest sampling is landed"))
  (assoc
   (reduce (fn [read surface]
             (validate! surface)
             (let [[tx ty] (to-texel surface point) {:keys [width height data]} surface
                   x (int (Math/floor tx)) y (int (Math/floor ty))]
               (if (and (<= 0 x) (< x width) (<= 0 y) (< y height))
                 (let [i (* 4 (+ x (* y width))) rgba (mapv #(aget data (+ i %)) (range 4))]
                   (-> read (assoc :color (over rgba (:color read)) :covered? true)
                       (update :contributors conj (str (:surface/id surface) "@" (:revision surface)))))
                 read)))
           {:status :resolved :color [0.0 0.0 0.0 0.0] :contributors [] :covered? false} stack)
   :snapshot (snapshot stack point filter ctx)))

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
  [{:keys [width height data] :as surface}]
  (validate! surface)
  (png/encode width height
              (vec (mapcat (fn [i]
                             (let [a (double (aget data (+ i 3)))
                                   channel (fn [v] (int (Math/floor (+ 0.5 (* 255 (max 0.0 (min 1.0 v)))))))]
                               (conj (mapv #(channel (color/linear->srgb-channel
                                                     (if (pos? a) (/ (aget data (+ i %)) a) 0.0))) (range 3))
                                     (channel a))))
                           (range 0 (alength data) 4)))))
