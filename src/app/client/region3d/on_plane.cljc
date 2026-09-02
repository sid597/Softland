(ns app.client.region3d.on-plane
  "Puts 2D marks on 3D planes: text laid out with the text preparer, ink
   tessellated with the path preparer, then packed and anchored on a region's
   plane. The one place two kinds meet.
   Takes: a placed text or ink row and font assets; a path-mesh cache; a
   placement's anchor to project.
   Gives: a layout; a packed ink mesh; a projected anchor.
   Holds nothing."
  (:require [app.client.path.component :as path-component]
            [app.client.path.tessellation :as path-tessellation]
            [app.client.region3d.scene :as region3d-scene]
            [app.client.engine.color :as color]
            [app.client.engine.transform :as transform]
            [app.client.text.layout :as text-layout]))

(def placed-color-adapter-version :region3d/placed-color-v1)
(def placed-ink-version :region3d/placed-ink-v1)
(def placement-pack-version :region3d/placement-pack-v1)
(def placement-zoom 1.0)
(def plane-epsilon 1.0e-9)

(defn adapt-legacy-color
  "Adapt the existing flat [r g b a] text/path paint into Contract-C ingress.
   Already-tagged colors pass through unchanged."
  [color]
  (cond
    (nil? color) nil
    (and (map? color) (:rgba color)) color
    (and (vector? color) (= 4 (count color)))
    {:rgba color
     :color-space :srgb
     :alpha-association :straight}
    :else
    (throw (ex-info "Placed color must be tagged or legacy flat RGBA"
                    {:color color :adapter placed-color-adapter-version}))))

(defn provider-identity
  "Return the shaping identity which joins a settled placement packing key."
  [font-assets]
  (select-keys (:layout-provider font-assets)
               [:face-id :face-revision :shaper-id :shaper-version
                :features :variations :axes :fallback-chain :upem :metrics]))

(defn session-layout-key [session-snapshot]
  [:session (:address session-snapshot) (:revision session-snapshot)])

(defn layout-placed-text
  "Create the settled Contract-T layout for one resolved placed text. The
   renderer-side packing cache is responsible for calling this only on a key
   transition; live editing supplies its already-carried result instead."
  [placement font-assets]
  (let [font-size (double (get-in placement [:style :font-size] 14.0))
        constraints (get-in placement [:layout :constraints])
        inline-size (get-in placement [:style :max-inline-size])
        inline-size (when (number? inline-size) inline-size)
        line-height (get constraints :line-height)
        line-height (if (number? line-height) line-height (* font-size 1.2))
        carried-advance (or (get-in placement [:layout :reference-advance])
                            (some-> (get-in placement [:layout :lines 0])
                                    text-layout/first-glyph-advance-x)
                            font-size)]
    (text-layout/layout
     {:text (:text placement)
      :provider (:layout-provider font-assets)
      :font-size font-size
      :char-advance carried-advance
      :line-height line-height
      :origin [0.0 0.0]
      :inline-size inline-size
      :wrap-policy (or (:wrap constraints) :none)
      :tab-stops (:tab-stops constraints)
      :clip (:clip constraints)
      :source-id (:address placement)
      :source-revision (:content-revision placement)
      :zoom placement-zoom})))

(defn object->component-local [[x y _z]] [(double x) (- (double y))])

(defn ray->placement-plane
  "Intersect a region-space ray with an object's local XY plane. Returns the
   component-local point plus the positive region-ray parameter."
  [{:keys [origin direction]} world-transform-matrix]
  (let [inverse (region3d-scene/inverse-mat4 world-transform-matrix)
        local-origin (region3d-scene/transform-point inverse origin)
        local-direction (region3d-scene/transform-direction inverse direction)
        dz (nth local-direction 2)]
    (when (> (Math/abs (double dz)) plane-epsilon)
      (let [local-t (/ (- (nth local-origin 2)) dz)]
        (when (pos? local-t)
          (let [local-point (region3d-scene/v+
                             local-origin
                             (region3d-scene/v* local-direction local-t))
                region-point (region3d-scene/transform-point world-transform-matrix
                                                               local-point)
                denominator (region3d-scene/dot direction direction)
                t (/ (region3d-scene/dot
                      (region3d-scene/v- region-point origin) direction)
                     denominator)]
            (when (pos? t)
              {:t t
               :point3 region-point
               :object-local local-point
               :component-local (object->component-local local-point)})))))))

(defn pack-placed-ink
  "Derive one placed ink mesh through the caller-owned path cache value."
  [cache placement]
  (let [{next-cache :cache [mesh] :meshes derived-keys :derived-keys}
        (path-tessellation/derive-mesh-set cache [(:component placement)]
                                           placement-zoom)]
    {:cache next-cache
     :derived-keys derived-keys
     :pack {:object-id (:object-id placement)
            :address (:address placement)
            :cache-key (:cache-key mesh)
            :vertices (:vertices mesh)
            :color (adapt-legacy-color
                    (path-component/paint-color (:component placement)))}}))

(defn linear-premultiplied [{[r g b a] :rgba} coverage opacity]
  (let [alpha (max 0.0 (min 1.0 (* (double a) (double coverage)
                                    (double opacity))))]
    [(* (color/srgb-channel->linear r) alpha)
     (* (color/srgb-channel->linear g) alpha)
     (* (color/srgb-channel->linear b) alpha)
     alpha]))

(defn- clamp-projection [[x y] width height]
  (let [cx (/ (double width) 2.0)
        cy (/ (double height) 2.0)]
    (if (and (<= 0.0 x width) (<= 0.0 y height))
      {:point [x y] :clamped? false}
      (let [dx (- x cx)
            dy (- y cy)
            tx (if (zero? dx) ##Inf (/ cx (Math/abs (double dx))))
            ty (if (zero? dy) ##Inf (/ cy (Math/abs (double dy))))
            t (min tx ty)]
        {:point [(max 0.0 (min width (+ cx (* t dx))))
                 (max 0.0 (min height (+ cy (* t dy))))]
         :clamped? true}))))

(defn project-region-anchor
  "Project a region-object binding into the connector anchor container."
  [{:keys [binding region-draw-item maintained camera world-transforms
           anchor-container]}]
  (let [object-id (:object binding)
        matrix (get-in maintained [:world-transforms object-id])
        region-world-transform (get world-transforms (:container region-draw-item))
        anchor-world-transform (get world-transforms anchor-container)]
    (when (and matrix region-world-transform anchor-world-transform)
      (let [point3 (region3d-scene/transform-point matrix (:local binding))
            projected (region3d-scene/project-point camera point3)
            depth (:depth projected)]
        (when (and projected (<= 0.0 depth 1.0))
          (let [width (double (:w region-draw-item))
                height (double (:h region-draw-item))
                {:keys [point clamped?]} (clamp-projection (:screen projected)
                                                           width height)
                region-container-point [(+ (double (:x region-draw-item)) (first point))
                                        (+ (double (:y region-draw-item)) (second point))]
                anchor-point (transform/inverse-point
                              anchor-world-transform
                              (transform/forward-point region-world-transform
                                                        region-container-point))]
            {:status :resolved
             :kind :point
             :center anchor-point
             :camera (:flags region-world-transform)
             :clip :none
             :region (:region binding)
             :object object-id
             :point3 point3
             :anchor-clamped clamped?}))))))
