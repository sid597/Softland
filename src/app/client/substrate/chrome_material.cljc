(ns app.client.substrate.chrome-material
  "Pure grammar and hybrid anchor/metric geometry for render-family chrome.

   Vertex anchors are container-local. Pixel offsets are deliberately separate
   and are applied after the container affine and world camera by chrome-gpu."
  (:require [app.client.workspace.containers :as containers]))

(def schema-version 1)
(def algorithm-version :chrome-derived-v1)
(def snap-threshold-screen-px 8.0)
(def handle-size-screen-px 10.0)
(def handle-slop-screen-px 6.0)
(def chrome-width-screen-px 1.0)
(def gap-tick-length-screen-px 8.0)

(def legal-forms
  #{:selection-outline :handle :marquee :guide-line :gap-tick})
(def legal-corners #{:nw :ne :sw :se})

(def colors
  "Straight sRGB RGBA. The scene-color seam performs transfer/premultiplication."
  {:selection-outline [0.20 0.64 1.00 1.00]
   :handle [0.96 0.98 1.00 1.00]
   :handle-border [0.12 0.48 0.96 1.00]
   :marquee-fill [0.18 0.58 1.00 0.16]
   :marquee-border [0.24 0.68 1.00 0.92]
   :guide-line [1.00 0.30 0.62 0.96]
   :gap-tick [1.00 0.48 0.20 0.96]})

(def legal-zoom-regimes
  [{:zoom {:min 0.01 :max 0.1}
    :extent :normalized-arbitrary :normalization :container-local-anchor
    :coordinate-precision :f32 :coverage-precision :rgba8unorm
    :lifecycle :gesture-or-session :backend :webgpu-triangle-list
    :verdict :hybrid-anchor-metric-twin}
   {:zoom {:min 0.1 :max 8.0}
    :extent :normalized-arbitrary :normalization :container-local-anchor
    :coordinate-precision :f32 :coverage-precision :rgba8unorm
    :lifecycle :gesture-or-session :backend :webgpu-triangle-list
    :verdict :hybrid-anchor-metric-twin}
   {:zoom {:min 8.0 :max 1000.0}
    :extent :normalized-arbitrary :normalization :container-local-anchor
    :coordinate-precision :f32 :coverage-precision :rgba8unorm
    :lifecycle :gesture-or-session :backend :webgpu-triangle-list
    :verdict :hybrid-anchor-metric-twin}])

(defn- geometry-declaration-for [form]
  (let [handle? (= :handle form)]
    {:geometry/version 1
     :authority {:kind :derived-chrome
                 :source-id :chrome/derived-from
                 :source-revision :chrome/selection-rev
                 :algorithm-version algorithm-version
                 :local-space :hybrid-container-anchor+screen-px-metric}
     :classify {:result #{:inside :boundary :outside}
                :fill-rule :not-applicable
                :boundary-rule :explicit}
     :coverage {:geometry-operator :aliased-v1
                :operator-version algorithm-version
                :boundary-relation :aliased-v1
                :reference-isocontour :not-applicable
                :visual-factors [:straight-srgb-color :effective-opacity]
                :tie-token :mathematical-boundary
                :quantization :rgba8unorm-per-regime}
     :pick {:policy (if handle? :interior :none)
            :boundary (if handle? :hit :not-applicable)
            :hit-slop {:metric :screen-px
                       :radius (if handle? handle-slop-screen-px 0.0)}
            :owner (if handle? :chrome-handle/identity :none)}
     :bounds {:math :form-quad
              :paint :form-quad
              :pick (if handle? :form-quad+declared-slop :none)}
     :derivations [{:kind :screen-offset-triangle-quads
                    :source-revision :chrome/selection-rev
                    :algorithm-version algorithm-version
                    :tolerance-lod :constant-screen-px
                    :normalization :container-local-anchor
                    :precision :f32
                    :backend :webgpu-triangle-list
                    :regime :chrome-legal-zoom}]
     :regimes legal-zoom-regimes}))

(def form-declarations
  (into {} (map (fn [form] [form (geometry-declaration-for form)])) legal-forms))

(def geometry-declaration
  "Family admission declaration. Per-form pick/metric differences remain in
   form-declarations and are validated on every material."
  (assoc (geometry-declaration-for :handle)
         :authority (assoc (:authority (geometry-declaration-for :handle))
                           :kind :derived-chrome-form-quad)
         :pick {:policy :declared-per-form
                :boundary :declared-per-form
                :hit-slop {:metric :screen-px :radius handle-slop-screen-px}
                :owner :chrome-form/identity}))

(def required-material-keys
  #{:chrome/form :chrome/anchor-bounds :chrome/derived-from
    :chrome/selection-rev :chrome/pick})
(def optional-material-keys
  #{:chrome/corner :chrome/gesture-id :chrome/from :chrome/to
    :chrome/alignment :chrome/container :chrome/extensions})

(defn finite-number? [x]
  (and (number? x)
       #?(:clj (Double/isFinite (double x))
          :cljs (js/Number.isFinite x))))

(defn point? [point]
  (and (vector? point) (= 2 (count point)) (every? finite-number? point)))

(defn bounds? [bounds]
  (and (map? bounds)
       (every? #(finite-number? (get bounds %)) [:x :y :w :h])
       (not (neg? (:w bounds)))
       (not (neg? (:h bounds)))))

(defn validate-material! [material]
  (let [keys* (set (keys material))
        missing (seq (sort (remove keys* required-material-keys)))
        unknown (seq (sort (remove (into required-material-keys
                                           optional-material-keys)
                                   keys*)))
        form (:chrome/form material)
        declaration (get form-declarations form)]
    (when missing
      (throw (ex-info "Chrome material is missing required fields"
                      {:missing (vec missing) :material material})))
    (when unknown
      (throw (ex-info "Chrome material has unknown fields"
                      {:unknown (vec unknown) :material material})))
    (when-not declaration
      (throw (ex-info "Chrome material has an unknown form"
                      {:form form :legal legal-forms})))
    (when-not (bounds? (:chrome/anchor-bounds material))
      (throw (ex-info "Chrome :chrome/anchor-bounds must be a finite rect"
                      {:bounds (:chrome/anchor-bounds material)})))
    (when-not (= (:chrome/pick material)
                 (get-in declaration [:pick :policy]))
      (throw (ex-info "Chrome material pick policy disagrees with its form"
                      {:form form :pick (:chrome/pick material)
                       :required (get-in declaration [:pick :policy])})))
    (if (= :handle form)
      (when-not (contains? legal-corners (:chrome/corner material))
        (throw (ex-info "Chrome handle requires a legal :chrome/corner"
                        {:corner (:chrome/corner material)})))
      (when (some? (:chrome/corner material))
        (throw (ex-info "Only chrome handles may carry :chrome/corner"
                        {:form form :corner (:chrome/corner material)}))))
    (when (contains? #{:guide-line :gap-tick} form)
      (when-not (and (point? (:chrome/from material))
                     (point? (:chrome/to material)))
        (throw (ex-info "Chrome line/tick forms require finite :chrome/from and :chrome/to"
                        {:form form :from (:chrome/from material)
                         :to (:chrome/to material)}))))
    material))

(defn material-id [{:keys [chrome/form chrome/corner chrome/gesture-id]
                    :as material}]
  (let [material (validate-material! material)]
    [form (:chrome/derived-from material) corner gesture-id]))

(defn- camera-project [effective camera point]
  (let [[wx wy] (containers/forward-point effective point)
        screen? (= 1 (:flags effective))
        zoom (if screen? 1.0 (double (or (:zoom camera) (:scale camera) 1.0)))
        pan-x (if screen? 0.0 (double (or (:x camera) 0.0)))
        pan-y (if screen? 0.0 (double (or (:y camera) 0.0)))]
    [(+ (* wx zoom) pan-x) (+ (* wy zoom) pan-y)]))

(defn chrome-screen-rect
  "CPU twin of `(anchor · container-affine · camera) + offset_px`.

   `offset` is {:x :y :w :h} in screen pixels around the projected anchor
   rectangle. It can turn a zero-sized corner anchor into a constant-pixel
   handle or expand a scaled target anchor by a constant border metric."
  [anchor-bounds effective camera offset]
  (let [{:keys [x y w h]} anchor-bounds
        points [(camera-project effective camera [x y])
                (camera-project effective camera [(+ x w) y])
                (camera-project effective camera [x (+ y h)])
                (camera-project effective camera [(+ x w) (+ y h)])]
        xs (map first points)
        ys (map second points)
        x0 (apply min xs)
        y0 (apply min ys)
        x1 (apply max xs)
        y1 (apply max ys)]
    {:x (+ x0 (double (or (:x offset) 0.0)))
     :y (+ y0 (double (or (:y offset) 0.0)))
     :w (+ (- x1 x0) (double (or (:w offset) 0.0)))
     :h (+ (- y1 y0) (double (or (:h offset) 0.0)))}))

(defn- vertex [anchor offset color]
  {:anchor anchor :offset-px offset :color color})

(defn- quad [anchors offsets color]
  (mapv #(vertex %1 %2 color) anchors offsets))

(defn- solid-rect-quad [{:keys [x y w h]} color]
  (quad [[x y] [(+ x w) y] [(+ x w) (+ y h)] [x (+ y h)]]
        [[0.0 0.0] [0.0 0.0] [0.0 0.0] [0.0 0.0]] color))

(defn- outline-quads [{:keys [x y w h]} color]
  (let [half (/ chrome-width-screen-px 2.0)
        x1 (+ x w) y1 (+ y h)]
    [(quad [[x y] [x1 y] [x1 y] [x y]]
           [[0.0 (- half)] [0.0 (- half)] [0.0 half] [0.0 half]] color)
     (quad [[x y1] [x1 y1] [x1 y1] [x y1]]
           [[0.0 (- half)] [0.0 (- half)] [0.0 half] [0.0 half]] color)
     (quad [[x y] [x y] [x y1] [x y1]]
           [[(- half) 0.0] [half 0.0] [half 0.0] [(- half) 0.0]] color)
     (quad [[x1 y] [x1 y] [x1 y1] [x1 y1]]
           [[(- half) 0.0] [half 0.0] [half 0.0] [(- half) 0.0]] color)]))

(defn- line-quad [[x0 y0 :as from] [x1 y1 :as to] width color]
  (let [half (/ width 2.0)]
    (if (>= (abs (- x1 x0)) (abs (- y1 y0)))
      (quad [from to to from]
            [[0.0 (- half)] [0.0 (- half)] [0.0 half] [0.0 half]] color)
      (quad [from from to to]
            [[(- half) 0.0] [half 0.0] [half 0.0] [(- half) 0.0]] color))))

(defn- alignment-axis [alignment]
  (if (map? alignment) (:axis alignment) (first alignment)))

(defn- gap-tick-quad [anchor alignment]
  (let [half-length (/ gap-tick-length-screen-px 2.0)
        half-width (/ chrome-width-screen-px 2.0)
        offsets (if (= :x (alignment-axis alignment))
                  [[(- half-width) (- half-length)]
                   [half-width (- half-length)]
                   [half-width half-length]
                   [(- half-width) half-length]]
                  [[(- half-length) (- half-width)]
                   [half-length (- half-width)]
                   [half-length half-width]
                   [(- half-length) half-width]])]
    (quad [anchor anchor anchor anchor] offsets (:gap-tick colors))))

(defn material-quads [material]
  (let [{:chrome/keys [form anchor-bounds from to alignment]}
        (validate-material! material)]
    (case form
      :selection-outline (outline-quads anchor-bounds (:selection-outline colors))
      :handle (let [half (/ handle-size-screen-px 2.0)
                    anchor [(:x anchor-bounds) (:y anchor-bounds)]]
                [(quad [anchor anchor anchor anchor]
                       [[(- half) (- half)] [half (- half)]
                        [half half] [(- half) half]]
                       (:handle colors))])
      :marquee (into [(solid-rect-quad anchor-bounds (:marquee-fill colors))]
                     (outline-quads anchor-bounds (:marquee-border colors)))
      :guide-line [(line-quad from to chrome-width-screen-px
                              (:guide-line colors))]
      :gap-tick [(gap-tick-quad from alignment)])))

(defn material-vertices
  "Expand each quad to two clockwise-independent triangles."
  [material]
  (into []
        (mapcat (fn [[a b c d]] [a b c a c d]))
        (material-quads material)))

(def vertex-words 9)
(def vertex-stride (* vertex-words 4))

(defn vertex-values [{:keys [anchor offset-px color]} container-idx]
  (into (into (vec anchor) offset-px)
        (conj (vec color) (or container-idx 0))))

(def claimed-corpus-forms legal-forms)

(defn assert-corpus-coverage! [form->gates]
  (let [actual (set (keys form->gates))
        missing (seq (sort (remove actual claimed-corpus-forms)))
        unconsumed (seq (sort (for [[form gates] form->gates :when (empty? gates)]
                               form)))]
    (when (or missing unconsumed)
      (throw (ex-info "Chrome corpus is incomplete or unconsumed"
                      {:missing (vec missing) :unconsumed (vec unconsumed)})))
    true))
