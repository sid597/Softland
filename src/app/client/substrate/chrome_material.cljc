(ns app.client.substrate.chrome-material
  "Neutral world-anchor/screen-metric quad geometry for the chrome render road."
  (:require [app.client.workspace.containers :as containers]))

(def schema-version 1)
(def algorithm-version :chrome-neutral-quads-v1)

(def legal-zoom-regimes
  [{:zoom {:min 0.01 :max 0.1}
    :extent :normalized-arbitrary :normalization :container-local-anchor
    :coordinate-precision :f32 :coverage-precision :rgba8unorm
    :lifecycle :session :backend :webgpu-triangle-list
    :verdict :hybrid-anchor-metric-twin}
   {:zoom {:min 0.1 :max 8.0}
    :extent :normalized-arbitrary :normalization :container-local-anchor
    :coordinate-precision :f32 :coverage-precision :rgba8unorm
    :lifecycle :session :backend :webgpu-triangle-list
    :verdict :hybrid-anchor-metric-twin}
   {:zoom {:min 8.0 :max 1000.0}
    :extent :normalized-arbitrary :normalization :container-local-anchor
    :coordinate-precision :f32 :coverage-precision :rgba8unorm
    :lifecycle :session :backend :webgpu-triangle-list
    :verdict :hybrid-anchor-metric-twin}])

(def geometry-declaration
  {:geometry/version 1
   :authority {:kind :neutral-screen-metric-quads
               :source-id :scene-entry/material-id
               :source-revision :scene-entry/material-revision
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
   :pick {:policy :none :boundary :not-applicable
          :hit-slop {:metric :screen-px :radius 0.0} :owner :none}
   :bounds {:math :declared-quad :paint :declared-quad :pick :none}
   :derivations [{:kind :screen-offset-triangle-quads
                  :source-revision :scene-entry/material-revision
                  :algorithm-version algorithm-version
                  :tolerance-lod :constant-screen-px
                  :normalization :container-local-anchor
                  :precision :f32
                  :backend :webgpu-triangle-list
                  :regime :chrome-legal-zoom}]
   :regimes legal-zoom-regimes})

(defn finite-number? [x]
  (and (number? x)
       #?(:clj (Double/isFinite (double x))
          :cljs (js/Number.isFinite x))))

(defn- point? [point]
  (and (vector? point) (= 2 (count point)) (every? finite-number? point)))

(defn- camera-project [effective camera point]
  (let [[wx wy] (containers/forward-point effective point)
        screen? (= 1 (:flags effective))
        zoom (if screen? 1.0 (double (or (:zoom camera) (:scale camera) 1.0)))
        pan-x (if screen? 0.0 (double (or (:x camera) 0.0)))
        pan-y (if screen? 0.0 (double (or (:y camera) 0.0)))]
    [(+ (* wx zoom) pan-x) (+ (* wy zoom) pan-y)]))

(defn chrome-screen-rect
  "Project local bounds through a container and camera, then add px offsets."
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

(defn- validate-quad! [{:keys [anchors offsets-px color] :as value}]
  (when-not (= #{:anchors :offsets-px :color} (set (keys value)))
    (throw (ex-info "Neutral quad has unknown or missing fields" {:quad value})))
  (when-not (and (vector? anchors) (= 4 (count anchors))
                 (every? point? anchors))
    (throw (ex-info "Neutral quad requires four finite world anchors"
                    {:anchors anchors})))
  (when-not (and (vector? offsets-px) (= 4 (count offsets-px))
                 (every? point? offsets-px))
    (throw (ex-info "Neutral quad requires four finite px offsets"
                    {:offsets-px offsets-px})))
  (when-not (and (vector? color) (= 4 (count color))
                 (every? finite-number? color))
    (throw (ex-info "Neutral quad requires finite RGBA" {:color color})))
  value)

(defn material-vertices
  "Expand a vector of neutral quads to two triangles per quad."
  [quads]
  (when-not (vector? quads)
    (throw (ex-info "Neutral material must be a vector of quads"
                    {:material quads})))
  (into []
        (mapcat (fn [quad-row]
                  (let [{:keys [anchors offsets-px color]}
                        (validate-quad! quad-row)
                        [a b c d] (quad anchors offsets-px color)]
                    [a b c a c d])))
        quads))

(def vertex-words 9)
(def vertex-stride (* vertex-words 4))

(defn vertex-values [{:keys [anchor offset-px color]} container-idx]
  (into (into (vec anchor) offset-px)
        (conj (vec color) (or container-idx 0))))
