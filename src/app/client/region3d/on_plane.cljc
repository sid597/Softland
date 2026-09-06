(ns app.client.region3d.on-plane
  "Adapt 2D geometry and coordinates to object-local planes.

   Input: resolved text/ink placement, font provider, rays/matrices, and
   anchor bindings. Output: text layout, the ink's regions and packs with
   their colours, plane intersections and projected anchors. No retained
   state. It calls the existing text/path computations; it does not resolve
   external content addresses or allocate GPU resources. placement-zoom is
   fixed at 1: placed ink is packed for one local unit per device pixel and
   the projection reads it at whatever scale it lands.

   Folder map: README.md."
  (:require [app.client.path.component :as path-component]
            [app.client.path.pack :as path-pack]
            [app.client.region3d.scene :as region3d-scene]
            [app.client.engine.color :as color]
            [app.client.engine.transform :as transform]
            [app.client.text.layout :as text-layout]))

(def placed-color-adapter-version :region3d/placed-color-v1)
(def placement-zoom 1.0)
(def plane-epsilon 1.0e-9)

(defn adapt-legacy-color
  "Nil, tagged color, or flat RGBA → nil/tagged color; other forms throw.

   Narrow legacy adapter. An existing map with :rgba passes through without
   full tagged-color validation."
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

(defn layout-placed-text
  "Placement and font assets → flat text layout at local origin.

   Adapts style/constraints to text.layout/layout. Serves as reusable
   calculation; this does not prove a text GPU placement path exists."
  [placement font-assets]
  (let [font-size (double (get-in placement [:style :font-size] 14.0))
        constraints (get-in placement [:layout :constraints])
        inline-size (get-in placement [:style :max-inline-size])
        inline-size (when (number? inline-size) inline-size)
        line-height (get constraints :line-height)
        line-height (if (number? line-height) line-height (* font-size 1.2))]
    (text-layout/layout
     {:text (:text placement)
      :provider (:layout-provider font-assets)
      :font-size font-size
      :line-height line-height
      :origin [0.0 0.0]
      :inline-size inline-size
      :wrap-policy (or (:wrap constraints) :none)
      :tab-stops (:tab-stops constraints)
      :clip (:clip constraints)
      :source-id (:address placement)
      :source-revision (:content-revision placement)
      :zoom placement-zoom})))

(defn object->component-local
  "Object-local XYZ → 2D [x -y].

   Explicit coordinate convention."
  [[x y _z]] [(double x) (- (double y))])

(defn ray->placement-plane
  "Region ray and object matrix → positive intersection with
   component/object/region coordinates, or nil.

   Inverts matrix, intersects local z=0, converts t back. Singular inverse
   throws and zero-direction ray is not independently checked."
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

(def placement-bucket
  "The scale bucket placed ink is packed for: bucket 0 is one local unit per
   device pixel, placement-zoom."
  (path-pack/scale-bucket placement-zoom))

(defn placed-ink-regions
  "Placement → {:object-id :address :regions [{:region :pack :cover
   :color :clip}] :ok? :missing}: the path value evaluated at the unit view,
   each painted region lowered for the placement bucket with a box cover,
   its colour tagged. The same regions and packs the 2D lane draws, so a
   stroke means one thing on a plane and on the canvas."
  [placement]
  (let [record (:component placement)
        result (path-component/regions record {:scale placement-zoom :pan [0.0 0.0]})
        tolerance (path-pack/bucket-tolerance placement-bucket)
        margin (path-pack/bucket-margin placement-bucket)
        clip (when-let [clip (:clip result)]
               (assoc clip :pack (:pack (path-pack/pack-region (:path clip) tolerance {}))))
        regions (vec (for [region (:regions result)
                           :let [{:keys [pack]} (path-pack/pack-region (:path region) tolerance {})]
                           :when (and pack (or (nil? clip) (:pack clip)))]
                       {:region region
                        :pack pack :clip clip
                        :cover (path-pack/cover pack {:mode :box :margin margin})
                        :color (adapt-legacy-color (path-component/region-color record region))}))]
    {:object-id (:object-id placement)
     :address (:address placement)
     :regions regions
     :ok? true
     :missing []}))

(defn linear-premultiplied
  "Tagged color, coverage, opacity → linear premultiplied RGBA.

   Transfer conversion and bounded combined alpha. Assumes straight-sRGB
   input."
  [{[r g b a] :rgba} coverage opacity]
  (let [alpha (max 0.0 (min 1.0 (* (double a) (double coverage)
                                    (double opacity))))]
    [(* (color/srgb-channel->linear r) alpha)
     (* (color/srgb-channel->linear g) alpha)
     (* (color/srgb-channel->linear b) alpha)
     alpha]))

(defn- clamp-projection
  "2D point and region size → point/clamped flag.

   Intersects center-to-point direction with rectangle edge. Intended for
   directional edge anchoring, not independent-axis clamping."
  [[x y] width height]
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
  "Binding, prepared scene/camera, region item, transforms, anchor group →
   resolved point anchor or nil.

   Projects object point, checks depth, clamps region edge, converts groups.
   Serves as geometry utility; its connector wording does not establish a
   current connector system."
  [{:keys [binding region-draw-item maintained camera world-transforms
           anchor-group]}]
  (let [object-id (:object binding)
        matrix (get-in maintained [:world-transforms object-id])
        region-world-transform (get world-transforms (:container region-draw-item))
        anchor-world-transform (get world-transforms anchor-group)]
    (when (and matrix region-world-transform anchor-world-transform)
      (let [point3 (region3d-scene/transform-point matrix (:local binding))
            projected (region3d-scene/project-point camera point3)
            depth (:depth projected)]
        (when (and projected (<= 0.0 depth 1.0))
          (let [width (double (:w region-draw-item))
                height (double (:h region-draw-item))
                {:keys [point clamped?]} (clamp-projection (:screen projected)
                                                           width height)
                region-group-point [(+ (double (:x region-draw-item)) (first point))
                                        (+ (double (:y region-draw-item)) (second point))]
                anchor-point (transform/inverse-point
                              anchor-world-transform
                              (transform/forward-point region-world-transform
                                                        region-group-point))]
            {:status :resolved
             :kind :point
             :center anchor-point
             :camera (:flags region-world-transform)
             :clip :none
             :region (:region binding)
             :object object-id
             :point3 point3
             :anchor-clamped clamped?}))))))
