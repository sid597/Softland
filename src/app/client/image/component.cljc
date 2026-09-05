(ns app.client.image.component
  "Pure image contracts and placement calculations.

   This file accepts source/component maps, rectangles, atlas values and
   stamped image items. It returns validated maps, source registries,
   crop/UV geometry, atlas plans, 13-word instance values and adjacent
   binding runs. It owns no mutable state or GPU handles.

   Folder map: README.md."
  (:require [app.client.engine.color :as color]
            [app.client.engine.schema :as schema]))

;; Color ingress --------------------------------------------------------------

(def legal-source-tags #{:srgb :embedded-profile})
(def legal-alpha-associations #{:straight :premultiplied :opaque})

(defn sha256-digest?
  "Value → lowercase 64-hex-character string?

   Length plus regex. Intended for digest syntax; does not verify bytes."
  [value]
  (and (string? value)
       (= 64 (count value))
       (boolean (re-matches #"[0-9a-f]{64}" value))))

(defn- positive-int?
  "Value → positive integer?

   Type/sign check."
  [value]
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
  "Source record → unchanged valid record or exception.

   Shared schema. Actual digest proof is a separate input."
  [source-row]
  (schema/check source source-row))

(defn empty-source-registry
  "No input → versioned empty registry.

   Pure constructor."
  []
  {:image-registry/version 1 :sources {}})

(defn register-verified-source
  "Registry, source, independently computed digest → updated registry;
   mismatches/conflicting records throw.

   Checks schema and digest equality; identical registration is idempotent.
   Content identity has a single binding."
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
  "Registry and digest → source record or nil."
  [registry digest]
  (get-in registry [:sources digest]))

;; Component schema -----------------------------------------------------------

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

(defn- intrinsic-size?
  "Value → pair of positive integers?

   Fixed-size vector check."
  [value]
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
  "Component → unchanged checked map or throws.

   Shared schema. Does not cross-check intrinsic size/color tag against a
   resolved source record."
  [component]
  (schema/check schema component))

(defn canonical-component
  "Nested value → recursively sorted maps/sets, ordered vectors.

   Local recursive canonical helper. Requires comparable sorted
   keys/elements."
  [component]
  (letfn [(canonical [value]
            (cond
              (map? value) (into (sorted-map)
                                 (map (fn [[key child]] [key (canonical child)]))
                                 value)
              (vector? value) (mapv canonical value)
              (set? value) (into (sorted-set) (map canonical) value)
              :else value))]
    (canonical component)))

;; Geometry -------------------------------------------------------------------

(defn normalize-crop
  "Intrinsic dimensions and optional crop → intersection rectangle; empty
   interior throws.

   Endpoint intersection with full-image default. Cropping cannot silently
   request an empty image."
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
  "Local rectangle and point → outside/boundary/inside.

   Inclusive outer bounds with exact equality on edges. Intended for a
   diagnostic tri-state contract."
  [{:keys [x y w h]} [px py]]
  (let [x1 (+ x w) y1 (+ y h)]
    (cond
      (or (< px x) (> px x1) (< py y) (> py y1)) :outside
      (or (= px x) (= px x1) (= py y) (= py y1)) :boundary
      :else :inside)))

(defn half-open-hit?
  "Rectangle and point → min-inclusive/max-exclusive hit boolean.

   Direct inequalities, zero slop. Adjacent rectangles have a clear equality
   rule."
  [{:keys [x y w h]} [px py]]
  (and (>= px x) (< px (+ x w))
       (>= py y) (< py (+ y h))))

(defn clip-placement
  "Placement, source crop, optional clip → proportionally clipped
   placement/crop, or nil.

   Transfers geometric clipping fractions into crop coordinates. Avoids
   stretching after clipping; assumes positive placement size."
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
  "Intrinsic size and crop → normalized [u0 v0 u1 v1].

   Direct division."
  [[intrinsic-width intrinsic-height] {:keys [x y w h]}]
  [(/ x (double intrinsic-width))
   (/ y (double intrinsic-height))
   (/ (+ x w) (double intrinsic-width))
   (/ (+ y h) (double intrinsic-height))])

;; Mip and allocation truth --------------------------------------------------

(defn mip-level-count
  "Dimensions → number of levels down to 1.

   Repeated integer halving of largest side. Intended for positive pixel
   dimensions."
  [width height]
  (loop [levels 1 side (max 1 width height)]
    (if (<= side 1) levels (recur (inc levels) (quot side 2)))))

(defn mip-sizes
  "Dimensions → every mip's clamped dimensions.

   Computes dimensions for the declared level count."
  [width height]
  (mapv (fn [level]
          [(max 1 (quot width (bit-shift-left 1 level)))
           (max 1 (quot height (bit-shift-left 1 level)))])
        (range (mip-level-count width height))))

;; Atlas/dedicated placement --------------------------------------------------

;; Atlas policy uses fixed dimensions, gutters, mip count and candidate-size
;; threshold. Oversize or unplaceable images receive dedicated textures.
;; These values are policy, not hardware-derived limits.
(def atlas-config
  {:atlas/version 1
   :width 512
   :height 512
   :padding 2
   :max-side 128
   :format :rgba8unorm
   ;; Two levels are the declared atlas route; level 1 is the deepest sampled
   ;; mip and the 2px gutter therefore still supplies a full texel.
   :mip-level-count 2
   :lod-max 1.0
   :overflow :dedicated})

(defn placement-tier
  "Width/height map → atlas or dedicated.

   Positive-size and configured-side threshold. Invalid dimensions also
   route to dedicated; this function is a selector, not validation."
  [{:keys [width height]}]
  (if (and (positive-int? width) (positive-int? height)
           (<= width (:max-side atlas-config))
           (<= height (:max-side atlas-config)))
    :atlas
    :dedicated))

(defn empty-atlas
  "No input → empty shelves/placements plus config.

   Pure constructor."
  []
  {:config atlas-config :shelves [] :used-height 0 :placements {}})

(defn- atlas-uv
  "Atlas dimensions and placed rectangle → normalized UV endpoints.

   Direct division."
  [{:keys [width height]} x y w h]
  [(/ x (double width)) (/ y (double height))
   (/ (+ x w) (double width)) (/ (+ y h) (double height))])

(defn atlas-place
  "Atlas, source key, dimensions → updated atlas/placement or rejection
   data.

   First fitting shelf, else new shelf; padded dimensions and duplicate
   guard. Deterministic/simple packing without compaction or freeing."
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
  "Atlas, key, dimensions → atlas/dedicated plan and reason.

   Threshold selection then shelf attempt with dedicated fallback. Intended
   for declared overflow policy; duplicate atlas keys also fall back to
   dedicated."
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
  "Rect, UV, tagged tint, opacity, compact group index → 13 numeric words.

   Rect 4 + UV 4 + tint 4 + index 1. Alpha multiplies opacity once, integer
   packing happens later."
  [{:keys [rect uv tint opacity buffer-index]}]
  (let [{:keys [x y w h]} rect
        {:keys [rgba]} tint
        [u0 v0 u1 v1] uv
        [r g b a] rgba]
    [x y w h u0 v0 u1 v1 r g b (* a (or opacity 1.0))
     buffer-index]))

(defn contiguous-binding-runs
  "Ordered resolved items → runs with binding, offset/count and items;
   missing binding throws.

   Merges adjacent equal bindings only. Reduces state changes without
   changing draw order."
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
