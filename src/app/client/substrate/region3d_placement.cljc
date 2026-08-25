(ns app.client.substrate.region3d-placement
  "Pure Region3D placement derivations.

   This namespace owns material-reference resolution, flat-plane math, text
   and ink packing, placed pick readers, and region-object anchor projection.
   Store values and explicit cache values enter as data; there is no ambient
   state and no execution clock."
  (:require [app.client.substrate.path-material :as path-material]
            [app.client.substrate.path-tessellation :as path-tessellation]
            [app.client.substrate.region3d-scene :as region3d-scene]
            [app.client.workspace.containers :as containers]
            [app.client.workspace.text-layout :as text-layout]))

(def placed-color-adapter-version :region3d/placed-color-v1)
(def placed-msdf-version :region3d/placed-msdf-v1)
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

(defn- text-resolution [object owner]
  (let [layout (:layout owner)
        params (get-in object [:text :params])
        legacy-style (:style owner)
        material-color (adapt-legacy-color
                        [(:r legacy-style 1.0) (:g legacy-style 1.0)
                         (:b legacy-style 1.0) (:a legacy-style 1.0)])
        color (or (:color params) material-color)
        max-inline-size (or (:max-inline-size params)
                            (get-in layout [:constraints :inline-size]))
        text (get-in layout [:source :text] "")
        style {:font-size (double (or (:size legacy-style)
                                     (get-in layout [:font :size]) 14.0))
               :color color
               :max-inline-size max-inline-size
               :color-adapter placed-color-adapter-version}
        content-revision
        [:region3d/text-content-v1 (:address owner) text style
         (select-keys (:constraints layout)
                      [:wrap :line-height :alignment :tab-stops :clip])]]
    {:status :resolved
     :kind :text
     :address (:address owner)
     :owner (select-keys owner [:vi :path :node-id])
     :content-revision content-revision
     :text text
     :style style
     :layout layout}))

(defn- ink-resolution [owner]
  (let [material (path-material/validate-material! (:material owner))]
    {:status :resolved
     :kind :ink
     :address (:address owner)
     :owner (select-keys owner [:vi :op-id])
     :content-revision (path-material/material-content-key material)
     :cache-key (path-material/material-cache-key material placement-zoom)
     :material material}))

(defn resolve-placed-object
  "Resolve one placed object against deterministic occurrence-owner indexes.
   Zero/many owners are declared non-paint states."
  [object text-index ink-index]
  (let [kind (:object/kind object)
        address (get-in object [kind :ref :address])
        owners (case kind
                 :text (get text-index address [])
                 :ink (get ink-index address [])
                 [])
        base {:object-id (:object/id object)
              :object object
              :kind kind
              :address address}]
    (cond
      (empty? owners) (assoc base :status :ref-absent)
      (< 1 (count owners)) (assoc base :status :ref-ambiguous
                                  :owner-count (count owners))
      :else (merge base
                   (case kind
                     :text (text-resolution object (first owners))
                     :ink (ink-resolution (first owners)))))))

(defn placement-census [placements]
  (let [statuses (frequencies (map :status placements))]
    {:placements (count placements)
     :resolved (get statuses :resolved 0)
     :ref-absent (get statuses :ref-absent 0)
     :ref-ambiguous (get statuses :ref-ambiguous 0)}))

(defn provider-identity
  "Return the shaping identity which joins a settled placement packing key."
  [font-assets]
  (select-keys (:layout-provider font-assets)
               [:face-id :face-revision :shaper-id :shaper-version
                :features :variations :axes :fallback-chain :upem :metrics]))

(defn settled-layout-key [placement font-assets]
  [(:address placement) (:content-revision placement)
   (provider-identity font-assets)])

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

(defn material->object-local [[x y]] [(double x) (- (double y)) 0.0])
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

(defn- contains-point? [{:keys [x y w h]} [px py]]
  (and (<= x px (+ x w)) (<= y py (+ y h))))

(defn pick-placed-text [placement ray]
  (when (and (= :resolved (:status placement)) (:layout placement))
    (when-let [plane (ray->placement-plane ray (:matrix placement))]
      (when (contains-point? (get-in placement [:layout :metrics :logical-bounds])
                             (:material-local plane))
        (merge {:route :placed-text
                :object-id (:object-id placement)
                :address (:address placement)
                :layout/id (get-in placement [:layout :layout/id])}
               (select-keys plane [:t :point3 :material-local])
               {:text-hit (text-layout/hit-test-result
                           (:layout placement) (:material-local plane))})))))

(defn pick-placed-ink [placement ray]
  (when (and (= :resolved (:status placement)) (:material placement))
    (when-let [plane (ray->placement-plane ray (:matrix placement))]
      (when (path-material/hit? (:material placement)
                                (:material-local plane))
        (merge {:route :placed-ink
                :object-id (:object-id placement)
                :address (:address placement)}
               (select-keys plane [:t :point3 :material-local]))))))

(defn pick-placement [placement ray]
  (case (:kind placement)
    :text (pick-placed-text placement ray)
    :ink (pick-placed-ink placement ray)
    nil))

(defn- glyph-map [glyphs]
  (reduce (fn [index glyph]
            (cond-> index
              (some? (:unicode glyph))
              (assoc [:unicode (:unicode glyph)] glyph)
              (some? (:index glyph))
              (assoc [:index (:index glyph)] glyph)
              (and (:fontId glyph) (some? (:unicode glyph)))
              (assoc [(:fontId glyph) :unicode (:unicode glyph)] glyph)
              (and (:fontId glyph) (some? (:index glyph)))
              (assoc [(:fontId glyph) :index (:index glyph)] glyph)))
          {}
          glyphs))

(defn atlas-glyph-map [font-assets]
  (let [atlas (:atlas font-assets)]
    (if-let [variants (:variants atlas)]
      (reduce (fn [result [font-id variant]]
                (reduce-kv (fn [index [kind glyph-id] glyph]
                             (assoc index [font-id kind glyph-id] glyph))
                           result
                           (glyph-map (:glyphs variant))))
              {}
              (map vector (:atlas-faces font-assets) variants))
      (glyph-map (:glyphs atlas)))))

(defn- painted-glyph [paint-map {:keys [glyph-id glyph-id-kind font-id]}]
  (let [kind (if (= glyph-id-kind :font-glyph-index) :index :unicode)]
    (or (get paint-map [font-id kind glyph-id])
        (get paint-map [kind glyph-id])
        (get paint-map [font-id :unicode 0xFFFD])
        (get paint-map [:unicode 0xFFFD])
        (get paint-map [font-id :index 0])
        (get paint-map [:index 0]))))

(defn pack-glyph-quads
  "Pack Contract-T positioned glyphs with atlas coverage metadata only."
  [placement layout font-assets]
  (let [atlas-width (double (or (get-in font-assets [:atlas :atlas :width]) 1))
        atlas-height (double (or (get-in font-assets [:atlas :atlas :height]) 1))
        paint-map (atlas-glyph-map font-assets)
        font-size (double (get-in placement [:style :font-size] 14.0))
        color (get-in placement [:style :color])]
    (into []
          (keep
           (fn [{:keys [character position glyph-id-kind] :as glyph}]
             (when-not (or (= character " ") (= glyph-id-kind :virtual/tab))
               (when-let [atlas-glyph (painted-glyph paint-map glyph)]
                 (let [[x baseline-y] position
                       plane (:planeBounds atlas-glyph)
                       atlas (:atlasBounds atlas-glyph)
                       left (+ x (* font-size (or (:left plane) 0.0)))
                       right (+ x (* font-size (or (:right plane) 0.0)))
                       top (- baseline-y (* font-size (or (:top plane) 0.0)))
                       bottom (- baseline-y (* font-size (or (:bottom plane) 0.0)))]
                   {:object-id (:object-id placement)
                    :address (:address placement)
                    :layout/id (:layout/id layout)
                    :rect [left top (- right left) (- bottom top)]
                    :uv [(/ (:left atlas) atlas-width)
                         (- 1.0 (/ (:top atlas) atlas-height))
                         (/ (:right atlas) atlas-width)
                         (- 1.0 (/ (:bottom atlas) atlas-height))]
                    :color color}))))
           (mapcat :glyphs (:lines (text-layout/paint-result layout)))))))

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

(defn region-anchor-resolver
  "Build the injected connector resolver from same-frame prepared region data.
   `prepared-by-region` maps region-id to a maintained scene + camera."
  [{:keys [regions prepared-by-region effective-transforms]}]
  (let [regions-by-address (into {} (map (juxt :address identity)) regions)]
    (fn [binding anchor-container & [effective]]
      (when-let [region-op (get regions-by-address (:region binding))]
        (let [{:keys [maintained camera]}
              (get prepared-by-region (:region-id region-op))]
          (project-region-anchor
           {:binding binding
            :region-op region-op
            :maintained maintained
            :camera camera
            :effective-transforms (or effective effective-transforms)
            :anchor-container anchor-container}))))))
