(ns app.client.region3d.placement
  "Pure Region3D placement derivations.

   This namespace owns flat-plane math, text and ink packing, and region-object
   anchor projection.
   Store values and explicit cache values enter as data; there is no ambient
   state and no execution clock."
  (:require [app.client.path.material :as path-material]
            [app.client.path.tessellation :as path-tessellation]
            [app.client.region3d.scene :as region3d-scene]
            [app.client.engine.placement :as containers]
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

(defn object->material-local [[x y _z]] [(double x) (- (double y))])

(defn ray->placement-plane
  "Intersect a region-space ray with an object's local XY plane. Returns the
   material-local point plus the positive region-ray parameter."
  [{:keys [origin direction]} effective-matrix]
  (let [inverse (region3d-scene/inverse-mat4 effective-matrix)
        local-origin (region3d-scene/transform-point inverse origin)
        local-direction (region3d-scene/transform-direction inverse direction)
        dz (nth local-direction 2)]
    (when (> (Math/abs (double dz)) plane-epsilon)
      (let [local-t (/ (- (nth local-origin 2)) dz)]
        (when (pos? local-t)
          (let [local-point (region3d-scene/v+
                             local-origin
                             (region3d-scene/v* local-direction local-t))
                region-point (region3d-scene/transform-point effective-matrix
                                                               local-point)
                denominator (region3d-scene/dot direction direction)
                t (/ (region3d-scene/dot
                      (region3d-scene/v- region-point origin) direction)
                     denominator)]
            (when (pos? t)
              {:t t
               :point3 region-point
               :object-local local-point
               :material-local (object->material-local local-point)})))))))

(defn pack-placed-ink
  "Derive one placed ink mesh through the caller-owned path cache value."
  [cache placement]
  (let [{next-cache :cache [mesh] :meshes derived-keys :derived-keys}
        (path-tessellation/derive-mesh-set cache [(:material placement)]
                                           placement-zoom)]
    {:cache next-cache
     :derived-keys derived-keys
     :pack {:object-id (:object-id placement)
            :address (:address placement)
            :cache-key (:cache-key mesh)
            :vertices (:vertices mesh)
            :triangles (:triangles mesh)
            :color (adapt-legacy-color
                    (path-material/paint-color (:material placement)))}}))

(defn srgb-channel->linear [value]
  (let [value (double value)]
    (if (<= value 0.04045)
      (/ value 12.92)
      (Math/pow (/ (+ value 0.055) 1.055) 2.4))))

(defn linear-premultiplied [{[r g b a] :rgba} coverage opacity]
  (let [alpha (max 0.0 (min 1.0 (* (double a) (double coverage)
                                    (double opacity))))]
    [(* (srgb-channel->linear r) alpha)
     (* (srgb-channel->linear g) alpha)
     (* (srgb-channel->linear b) alpha)
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
  [{:keys [binding region-op maintained camera effective-transforms
           anchor-container]}]
  (let [object-id (:object binding)
        matrix (get-in maintained [:effective-transforms object-id])
        region-effective (get effective-transforms (:container region-op))
        anchor-effective (get effective-transforms anchor-container)]
    (when (and matrix region-effective anchor-effective)
      (let [point3 (region3d-scene/transform-point matrix (:local binding))
            projected (region3d-scene/project-point camera point3)
            depth (:depth projected)]
        (when (and projected (<= 0.0 depth 1.0))
          (let [width (double (:w region-op))
                height (double (:h region-op))
                {:keys [point clamped?]} (clamp-projection (:screen projected)
                                                           width height)
                region-container-point [(+ (double (:x region-op)) (first point))
                                        (+ (double (:y region-op)) (second point))]
                anchor-point (containers/inverse-point
                              anchor-effective
                              (containers/forward-point region-effective
                                                        region-container-point))]
            {:status :resolved
             :kind :point
             :center anchor-point
             :camera (:flags region-effective)
             :clip :none
             :region (:region binding)
             :object object-id
             :point3 point3
             :anchor-clamped clamped?}))))))
