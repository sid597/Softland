(ns app.client.image.material
  "Pure material, geometry, allocation, and packing laws for the image atom.

   This namespace owns no GPU objects and emits no scene-tape entries.  Source
   bytes resolve only through the digest-keyed registry below; renderer code is
   a consumer of these values, never a second material authority."
  )

;; Contract C ingress ---------------------------------------------------------

(def source-refusal-policy :reject-untagged)
(def legal-source-tags #{:srgb :embedded-profile})
(def legal-alpha-associations #{:straight :premultiplied :opaque})

(def ingress-receipt
  {:image-ingress/version 1
   :decode :create-image-bitmap
   :embedded-profile :convert-to-srgb-at-decode
   :srgb :preserve-encoded-srgb-at-decode
   :candidate-transfer :srgb-to-linear-once-at-texture-ingress
   :candidate-premultiply :after-coverage-and-opacity
   :candidate-presentation :linear-to-output-once-at-srgb-attachment
   :seam-off :encoded-byte-passthrough})

(defn sha256-digest?
  [value]
  (and (string? value)
       (= 64 (count value))
       (boolean (re-matches #"[0-9a-f]{64}" value))))

(defn- positive-int? [value]
  (and (integer? value) (pos? value)))

(defn validate-source!
  "Validate a resolved source record.  The caller still has to prove its byte
   digest through `register-verified-source`; a declared digest is never
   caller authority (T2/T9)."
  [{:keys [image/digest image/color-tag image/width image/height
           image/bytes-route image/alpha-association]
    :as source}]
  (let [declared-ingress-receipt (:image/ingress-receipt source)]
  (when-not (sha256-digest? digest)
    (throw (ex-info "Image source requires a lowercase sha256 digest"
                    {:source source :image/digest digest})))
  (when-not (contains? legal-source-tags color-tag)
    (throw (ex-info "Untagged image color is refused"
                    {:policy source-refusal-policy
                     :image/color-tag color-tag
                     :legal legal-source-tags})))
  (when-not (contains? legal-alpha-associations alpha-association)
    (throw (ex-info "Image source requires a declared alpha association"
                    {:image/alpha-association alpha-association
                     :legal legal-alpha-associations})))
  (when-not (and (positive-int? width) (positive-int? height))
    (throw (ex-info "Image source dimensions must be positive integers"
                    {:image/width width :image/height height})))
  (when-not (some? bytes-route)
    (throw (ex-info "Image source requires a digest-keyed bytes route"
                    {:image/digest digest})))
  (when-not (= ingress-receipt declared-ingress-receipt)
    (throw (ex-info "Image source ingress receipt is not the admitted chain"
                    {:image/digest digest
                     :expected ingress-receipt
                     :actual declared-ingress-receipt})))
  source))

(defn empty-source-registry []
  {:image-registry/version 1 :sources {}})

(defn register-verified-source
  "Register `source` only after the byte reader has independently computed
   `computed-digest`.  Duplicate identical rows are idempotent; a conflicting
   row for one digest is refused."
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
  [source algorithm-version regime]
  [(:image/digest (validate-source! source)) algorithm-version regime])

;; Contract M grammar ---------------------------------------------------------

(def material-schema-version 1)
(def material-required-keys
  #{:image/material-id :image/revision :image/source-digest
    :image/color-tag :image/intrinsic-size :image/provenance})
(def material-optional-keys #{:image/extensions})

(defn validate-material!
  "Fail-closed material grammar.  Unknown top-level fields and moving-media
   time are refused in this dark wave."
  [material]
  (let [keys* (set (keys material))
        missing (seq (sort (remove keys* material-required-keys)))
        unknown (seq (sort (remove (into material-required-keys
                                         material-optional-keys)
                                   keys*)))]
    (when missing
      (throw (ex-info "Image material is missing required fields"
                      {:missing (vec missing)})))
    (when unknown
      (throw (ex-info "Image material has unknown fields"
                      {:unknown (vec unknown) :policy :reject})))
    (when (contains? (or (:image/extensions material) {}) :image/time)
      (throw (ex-info "Moving sampled media is refused by this wave"
                      {:field :image/time})))
    (when-not (sha256-digest? (:image/source-digest material))
      (throw (ex-info "Image material source identity must be a sha256 digest"
                      {:image/source-digest (:image/source-digest material)})))
    (when-not (contains? legal-source-tags (:image/color-tag material))
      (throw (ex-info "Image material has an inadmissible color tag"
                      {:image/color-tag (:image/color-tag material)})))
    (let [[width height] (:image/intrinsic-size material)]
      (when-not (and (positive-int? width) (positive-int? height))
        (throw (ex-info "Image material intrinsic size is invalid"
                        {:image/intrinsic-size (:image/intrinsic-size material)}))))
    material))

(defn canonical-material
  [material]
  (into (sorted-map) (validate-material! material)))

(defn material-cache-key
  [material algorithm-version regime]
  (let [material (validate-material! material)]
    [(:image/source-digest material)
     (:image/revision material)
     algorithm-version
     regime]))

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

;; Mip and budget truth -------------------------------------------------------

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

(defn texture-bytes
  ([width height] (texture-bytes width height 4))
  ([width height bytes-per-pixel]
   (reduce + 0 (map (fn [[w h]] (* w h bytes-per-pixel))
                    (mip-sizes width height)))))

;; Atlas/dedicated placement --------------------------------------------------

(def atlas-config
  {:atlas/version 1
   :width 512
   :height 512
   :padding 2
   :max-side 128
   :format :rgba8unorm
   ;; Two levels are the declared atlas road; level 1 is the deepest sampled
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
  "rect[4] + uv[4] + tint/opacity[4] + container u32[1].  There is no
   per-node transform representation (T8/T14)."
  [{:keys [x y w h uv tint opacity container-idx]}]
  (let [[u0 v0 u1 v1] uv
        [r g b a] (or tint [1.0 1.0 1.0 1.0])]
    [x y w h u0 v0 u1 v1 r g b (* a (or opacity 1.0))
     (or container-idx 0)]))

(defn contiguous-binding-runs
  "Walk stamped image ops in order and merge adjacent equal bindings only.
   Returned offsets reproduce op order as explicit sub-draw indirection; no
   map or registration order participates (T1/T13)."
  [ops]
  (reduce-kv
   (fn [runs offset op]
     (let [binding (:image/binding-key op)
           last-run (peek runs)]
       (when-not binding
         (throw (ex-info "Image op is missing a resolved texture binding"
                         {:offset offset :op op})))
       (if (= binding (:binding-key last-run))
         (conj (pop runs)
               (-> last-run
                   (update :instance-count inc)
                   (update :ops conj op)))
         (conj runs {:binding-key binding
                     :first-instance offset
                     :instance-count 1
                     :ops [op]}))))
   []
   (vec ops)))

(def claimed-corpus-pressures
  #{:two-extents :atlas-overflow :alpha-association-pair
    :embedded-icc :untagged-refusal :digest-mismatch :unresolvable-digest
    :partially-clipped})

(defn assert-corpus-coverage!
  [fixture-pressure->gates]
  (let [actual (set (keys fixture-pressure->gates))
        missing (seq (sort (remove actual claimed-corpus-pressures)))
        unconsumed (seq (sort (for [[pressure gates] fixture-pressure->gates
                                   :when (empty? gates)] pressure)))]
    (when (or missing unconsumed)
      (throw (ex-info "Image corpus is incomplete or unconsumed"
                      {:missing (vec missing) :unconsumed (vec unconsumed)})))
    true))
