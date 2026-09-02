(ns app.client.image.component
  "What an image is: a verified source (digest, color tag, size, alpha
   association), its place in the atlas, and the 13 floats one image quad packs
   to.
   Takes: an image's provenance record; a registry and a computed digest; an
   atlas, a content hash, and dimensions; one quad's rect, uv, tint, and
   group buffer-index.
   Gives: a validated source; an atlas placement plan; 13 floats per instance;
   contiguous draw runs.
   Holds nothing; owns no GPU objects."
  (:require [app.client.engine.color :as color]
            [app.client.engine.schema :as schema]))

;; Contract C ingress ---------------------------------------------------------

(def legal-source-tags #{:srgb :embedded-profile})
(def legal-alpha-associations #{:straight :premultiplied :opaque})

(defn sha256-digest?
  [value]
  (and (string? value)
       (= 64 (count value))
       (boolean (re-matches #"[0-9a-f]{64}" value))))

(defn- positive-int? [value]
  (and (integer? value) (pos? value)))

(def source
  {:keys #{:image/digest :image/color-tag :image/width :image/height
           :image/bytes-route :image/alpha-association}
   :validators {:image/digest sha256-digest?
                :image/color-tag legal-source-tags
                :image/width positive-int?
                :image/height positive-int?
                :image/bytes-route some?
                :image/alpha-association legal-alpha-associations}})

(defn validate-source!
  "Validate a resolved source record.  The caller still has to prove its byte
   digest through `register-verified-source`; a declared digest is never
   caller authority (T2/T9)."
  [source-row]
  (schema/check source source-row))

(defn empty-source-registry []
  {:image-registry/version 1 :sources {}})

(defn register-verified-source
  "Register `source` only after the byte reader has independently computed
   `computed-digest`.  Duplicate identical rows are idempotent; a conflicting
   row for one digest is rejected."
  [registry source computed-digest]
  (let [source (validate-source! source)
        digest (:image/digest source)
        prior (get-in registry [:sources digest])]
    (when-not (= digest computed-digest)
      (throw (ex-info "Image bytes do not match the caller-declared digest"
                      {:declared digest :computed computed-digest})))
    (when (and prior (not= prior source))
      (throw (ex-info "Image digest is already bound to different source data"
                      {:image/digest digest :existing prior :incoming source})))
    (assoc-in registry [:sources digest] source)))

(defn resolve-source
  "The one data-resolution API: digest -> source record or nil."
  [registry digest]
  (get-in registry [:sources digest]))

(defn source-cache-key
  [source algorithm-version lod]
  [(:image/digest (validate-source! source)) algorithm-version lod])

;; Contract M schema ---------------------------------------------------------

(def rect
  {:keys #{:x :y :w :h}
   :validators {:x schema/finite-number?
                :y schema/finite-number?
                :w schema/positive-number?
                :h schema/positive-number?}})

(def paint
  {:keys #{:tint :opacity}
   :validators {:tint color/tagged
                :opacity #(and (schema/finite-number? %)
                               (<= 0.0 % 1.0))}})

(defn- intrinsic-size? [value]
  (and (vector? value)
       (= 2 (count value))
       (every? positive-int? value)))

(def schema
  {:keys #{:image/component-id :image/revision :image/source-digest
           :image/color-tag :image/intrinsic-size :image/provenance
           :image/rect :image/paint}
   :optional #{:image/crop}
   :validators {:image/component-id some?
                :image/revision some?
                :image/source-digest sha256-digest?
                :image/color-tag legal-source-tags
                :image/intrinsic-size intrinsic-size?
                :image/provenance some?
                :image/rect rect
                :image/crop rect
                :image/paint paint}})

(defn validate-component!
  "Check the declared image schema and return the unchanged EDN map."
  [component]
  (schema/check schema component))

(defn canonical-component
  [component]
  (into (sorted-map) component))

(defn component-cache-key
  [component algorithm-version lod]
  [(:image/source-digest component)
   (:image/revision component)
   algorithm-version
   lod])

;; Contract G geometry --------------------------------------------------------

(defn normalize-crop
  "Intersect a requested image-local pixel crop with the intrinsic rect."
  [[intrinsic-width intrinsic-height] crop]
  (let [{:keys [x y w h]} (or crop {:x 0 :y 0
                                    :w intrinsic-width :h intrinsic-height})
        x0 (max 0 x)
        y0 (max 0 y)
        x1 (min intrinsic-width (+ x w))
        y1 (min intrinsic-height (+ y h))]
    (when-not (and (< x0 x1) (< y0 y1))
      (throw (ex-info "Image crop has no interior after intrinsic clamp"
                      {:intrinsic [intrinsic-width intrinsic-height]
                       :crop crop})))
    {:x x0 :y y0 :w (- x1 x0) :h (- y1 y0)}))

(defn classify-quad
  "Tri-state classification against a local axis-aligned quad."
  [{:keys [x y w h]} [px py]]
  (let [x1 (+ x w) y1 (+ y h)]
    (cond
      (or (< px x) (> px x1) (< py y) (> py y1)) :outside
      (or (= px x) (= px x1) (= py y) (= py y1)) :boundary
      :else :inside)))

(defn half-open-hit?
  "Product-pick equality law: min edges inclusive, max edges exclusive;
   hit-slop is exactly 0.0 (T6/T14)."
  [{:keys [x y w h]} [px py]]
  (and (>= px x) (< px (+ x w))
       (>= py y) (< py (+ y h))))

(defn clip-placement
  "Clamp a placed quad to `clip`, carrying the same fractions into image-local
   crop coordinates.  Geometry shrinks and UVs inset together, so clipping
   crops rather than stretches (T15)."
  [{:keys [x y w h] :as placement} crop clip]
  (if-not clip
    {:placement placement :crop crop}
    (let [x0 (max x (:x clip))
          y0 (max y (:y clip))
          x1 (min (+ x w) (+ (:x clip) (:w clip)))
          y1 (min (+ y h) (+ (:y clip) (:h clip)))]
      (when (and (< x0 x1) (< y0 y1))
        (let [fx0 (/ (- x0 x) (double w))
              fy0 (/ (- y0 y) (double h))
              fx1 (/ (- x1 x) (double w))
              fy1 (/ (- y1 y) (double h))]
          {:placement {:x x0 :y y0 :w (- x1 x0) :h (- y1 y0)}
           :crop {:x (+ (:x crop) (* fx0 (:w crop)))
                  :y (+ (:y crop) (* fy0 (:h crop)))
                  :w (* (- fx1 fx0) (:w crop))
                  :h (* (- fy1 fy0) (:h crop))}})))))

(defn crop->uv
  [[intrinsic-width intrinsic-height] {:keys [x y w h]}]
  [(/ x (double intrinsic-width))
   (/ y (double intrinsic-height))
   (/ (+ x w) (double intrinsic-width))
   (/ (+ y h) (double intrinsic-height))])

;; Mip and allocation truth --------------------------------------------------

(defn mip-level-count
  [width height]
  (loop [levels 1 side (max 1 width height)]
    (if (<= side 1) levels (recur (inc levels) (quot side 2)))))

(defn mip-sizes
  [width height]
  (mapv (fn [level]
          [(max 1 (quot width (bit-shift-left 1 level)))
           (max 1 (quot height (bit-shift-left 1 level)))])
        (range (mip-level-count width height))))

;; Atlas/dedicated placement --------------------------------------------------

(def atlas-config
  {:atlas/version 1
   :width 512
   :height 512
   :padding 2
   :max-side 128
   :format :rgba8unorm
   ;; Two levels are the declared atlas route; level 1 is the deepest sampled
   ;; mip and the 2px gutter therefore still supplies a full texel (T5).
   :mip-level-count 2
   :lod-max 1.0
   :overflow :dedicated})

(defn placement-tier
  [{:keys [width height]}]
  (if (and (positive-int? width) (positive-int? height)
           (<= width (:max-side atlas-config))
           (<= height (:max-side atlas-config)))
    :atlas
    :dedicated))

(defn empty-atlas []
  {:config atlas-config :shelves [] :used-height 0 :placements {}})

(defn- atlas-uv [{:keys [width height]} x y w h]
  [(/ x (double width)) (/ y (double height))
   (/ (+ x w) (double width)) (/ (+ y h) (double height))])

(defn atlas-place
  "Deterministic shelf placement with a padded gutter.  Rejection is a value;
   the caller routes every rejection to the dedicated tier."
  [atlas source-key {:keys [width height]}]
  (let [{:keys [padding] :as config} (:config atlas)
        padded-width (+ width (* 2 padding))
        padded-height (+ height (* 2 padding))]
    (cond
      (contains? (:placements atlas) source-key)
      {:rejected {:reason :duplicate-source :source-key source-key}}

      (or (> padded-width (:width config))
          (> padded-height (:height config)))
      {:rejected {:reason :exceeds-atlas :source-key source-key}}

      :else
      (if-let [shelf-index
               (first
                (keep-indexed
                 (fn [index shelf]
                   (when (and (<= padded-height (:height shelf))
                              (<= (+ (:used-width shelf) padded-width)
                                  (:width config)))
                     index))
                 (:shelves atlas)))]
        (let [shelf (nth (:shelves atlas) shelf-index)
              x (+ (:used-width shelf) padding)
              y (+ (:y shelf) padding)
              placement {:x x :y y :width width :height height
                         :padding padding
                         :uv (atlas-uv config x y width height)}]
          {:atlas (-> atlas
                      (update-in [:shelves shelf-index :used-width] + padded-width)
                      (assoc-in [:placements source-key] placement))
           :placement placement})
        (if (<= (+ (:used-height atlas) padded-height) (:height config))
          (let [base-y (:used-height atlas)
                x padding
                y (+ base-y padding)
                placement {:x x :y y :width width :height height
                           :padding padding
                           :uv (atlas-uv config x y width height)}]
            {:atlas (-> atlas
                        (update :shelves conj {:y base-y
                                               :height padded-height
                                               :used-width padded-width})
                        (assoc :used-height (+ base-y padded-height))
                        (assoc-in [:placements source-key] placement))
             :placement placement})
          {:rejected {:reason :atlas-full
                      :source-key source-key
                      :requested [width height]}})))))

(defn placement-plan
  [atlas source-key dimensions]
  (if (= :dedicated (placement-tier dimensions))
    {:atlas atlas :tier :dedicated :reason :tier-bound}
    (let [{next-atlas :atlas placement :placement rejected :rejected}
          (atlas-place atlas source-key dimensions)]
      (if placement
        {:atlas next-atlas :tier :atlas :placement placement}
        {:atlas atlas :tier :dedicated :reason (:reason rejected)}))))

;; Instance buffer and ordered sub-draw law ----------------------------------

(def image-instance-words 13)
(def image-instance-stride (* image-instance-words 4))

(defn instance-words
  "rect[4] + uv[4] + tint/opacity[4] + group u32[1].  There is no
   per-node transform representation (T8/T14)."
  [{:keys [rect uv tint opacity buffer-index]}]
  (let [{:keys [x y w h]} rect
        {:keys [rgba]} tint
        [u0 v0 u1 v1] uv
        [r g b a] rgba]
    [x y w h u0 v0 u1 v1 r g b (* a (or opacity 1.0))
     buffer-index]))

(defn contiguous-binding-runs
  "Walk stamped image draw-items in order and merge adjacent equal bindings only.
   Returned offsets reproduce draw-item order as explicit sub-draw indirection; no
   map or registration order participates (T1/T13)."
  [draw-items]
  (reduce-kv
   (fn [runs offset draw-item]
     (let [binding (:image/binding-key draw-item)
           last-run (peek runs)]
       (when-not binding
         (throw (ex-info "Image draw-item is missing a resolved texture binding"
                         {:offset offset :draw-item draw-item})))
       (if (= binding (:binding-key last-run))
         (conj (pop runs)
               (-> last-run
                   (update :instance-count inc)
                   (update :draw-items conj draw-item)))
         (conj runs {:binding-key binding
                     :first-instance offset
                     :instance-count 1
                     :draw-items [draw-item]}))))
   []
   (vec draw-items)))
