(ns app.client.substrate.image-material
  "Package 2 image atom — the pure material/resource laws.

   Everything here is data and pure functions shared by the GPU layer, the JVM
   receipts, and the browser receipt: Contract-C ingress tags and the untagged
   refusal policy, mip-chain math, the dedicated-vs-atlas placement strategy,
   the shelf atlas allocator, the instance byte layout, and per-instance scene
   entries. No GPU objects, no atoms, no renderer imports. The family's
   citizenship declarations live in `app.client.substrate.scene-tape`
   (the contract authority); this namespace derives from that registration
   and never re-declares it.")

;; --- Contract C — source tags and the refusal policy -----------------------

(def source-refusal-policy
  "Untagged image color is invalid at family admission (Contract C §8.1.1)."
  :reject-untagged)

(def legal-source-tags
  "Baseline authoring space, or an embedded profile that the browser decode
   converts to sRGB exactly once. `:unknown` is an explicit refusal, never a
   silent assumption."
  #{:srgb :embedded-profile})

(defn validate-image-source!
  "Fail closed on undeclared or unsupported color. Returns the tag map."
  [{:keys [color-space] :as source-tag}]
  (when (nil? color-space)
    (throw (ex-info "Untagged image color is invalid at family admission"
                    {:policy source-refusal-policy :source source-tag})))
  (when-not (contains? legal-source-tags color-space)
    (throw (ex-info "Undeclared image color space is refused, not assumed"
                    {:policy source-refusal-policy
                     :color-space color-space
                     :legal legal-source-tags})))
  source-tag)

(def ingress-receipt
  "The declared exactly-once ingress chain for every decoded image:
   browser decode (embedded profile → sRGB) → one premultiply at upload.
   The scene transfer stays in the shared shader seam: the legacy route is
   `:legacy-none`; the linear-premultiplied candidate decodes at sample."
  {:image-ingress/version 1
   :decode :browser-image-decode-once
   :profile-conversion :embedded-profile-to-srgb-at-decode
   :premultiply :copy-external-image-premultiplied
   :premultiply-space :encoded-srgb
   :scene-transfer :shader-scene-color-seam})

;; --- Mip-chain math --------------------------------------------------------

(defn mip-level-count
  "Full chain: floor(log2(max(w,h))) + 1."
  [width height]
  (let [side (max 1 (max width height))]
    (loop [n 1 s side]
      (if (<= s 1) n (recur (inc n) (quot s 2))))))

(defn mip-sizes
  "Level sizes [w h] from level 0 down to [1 1] (each side halved, min 1)."
  [width height]
  (let [levels (mip-level-count width height)]
    (mapv (fn [level]
            [(max 1 (quot width (bit-shift-left 1 level)))
             (max 1 (quot height (bit-shift-left 1 level)))])
          (range levels))))

(defn texture-bytes-with-mips
  "Honest rgba8 budget accounting for a full mip chain (the gpu-budget tracker
   only prices level 0 on its own)."
  [width height]
  (reduce + (map (fn [[w h]] (* w h 4)) (mip-sizes width height))))

;; --- Placement strategy: dedicated tier vs atlas tier ----------------------

(def atlas-config
  "The declared bind-group/atlas strategy for the image atom's opening:
   small images shelf-pack into one shared rgba8unorm atlas (one bind group,
   level 0 only, 2px transparent padding — bilinear reach is a half texel, so
   nothing bleeds across neighbors); everything larger owns a dedicated
   texture with a full linear-light mip chain and its own cached bind group.
   Atlas overflow falls back to the dedicated tier, machine-driven (M10)."
  {:atlas/version 1
   :width 2048
   :height 2048
   :padding 2
   :max-side 256
   :format :rgba8unorm
   :mip-level-count 1
   :edge-semantics :blend-to-transparent-padding
   :minification :level-0-bilinear-only
   :overflow :fall-back-to-dedicated})

(defn placement-tier
  "Choose the declared resource tier for a decoded image."
  [{:keys [width height]}]
  (if (and (<= width (:max-side atlas-config))
           (<= height (:max-side atlas-config)))
    :atlas
    :dedicated))

;; --- Shelf atlas allocator (pure, deterministic) ---------------------------

(defn empty-atlas []
  {:config atlas-config
   :shelves []          ;; [{:y :height :used-width}]
   :used-height 0
   :placements {}})     ;; source-key -> {:x :y :width :height :uv [...]}

(defn- placement-uv [{:keys [width height]} x y w h]
  [(/ x (double width)) (/ y (double height))
   (/ (+ x w) (double width)) (/ (+ y h) (double height))])

(defn atlas-place
  "Place one image into the shelf atlas. Returns
   {:atlas atlas' :placement {:x :y :width :height :uv [u0 v0 u1 v1]}} or
   {:rejected {...}} when no shelf can hold it — the caller routes rejections
   to the dedicated tier (the declared overflow behavior), never drops them."
  [atlas source-key {:keys [width height]}]
  (let [{:keys [padding] :as config} (:config atlas)
        padded-w (+ width (* 2 padding))
        padded-h (+ height (* 2 padding))]
    (cond
      (contains? (:placements atlas) source-key)
      {:rejected {:reason :duplicate-source :source-key source-key}}

      (or (> padded-w (:width config)) (> padded-h (:height config)))
      {:rejected {:reason :exceeds-atlas :width width :height height}}

      :else
      (let [shelf-idx (first (keep-indexed
                              (fn [idx {:keys [height used-width]}]
                                (when (and (<= padded-h height)
                                           (<= (+ used-width padded-w)
                                               (:width config)))
                                  idx))
                              (:shelves atlas)))]
        (cond
          (some? shelf-idx)
          (let [shelf (nth (:shelves atlas) shelf-idx)
                x (+ (:used-width shelf) padding)
                y (+ (:y shelf) padding)
                placement {:x x :y y :width width :height height
                           :uv (placement-uv config x y width height)}]
            {:atlas (-> atlas
                        (update-in [:shelves shelf-idx :used-width] + padded-w)
                        (assoc-in [:placements source-key] placement))
             :placement placement})

          (<= (+ (:used-height atlas) padded-h) (:height config))
          (let [y-base (:used-height atlas)
                x padding
                y (+ y-base padding)
                placement {:x x :y y :width width :height height
                           :uv (placement-uv config x y width height)}]
            {:atlas (-> atlas
                        (update :shelves conj {:y y-base
                                               :height padded-h
                                               :used-width padded-w})
                        (assoc :used-height (+ y-base padded-h))
                        (assoc-in [:placements source-key] placement))
             :placement placement})

          :else
          {:rejected {:reason :atlas-full
                      :requested [width height]
                      :used-height (:used-height atlas)}})))))

;; --- Instance byte layout --------------------------------------------------

(def image-instance-words
  "rect[4] + uv_bounds[4] + corner_radii[4] + tint[4] + container_idx[1]."
  17)

(def image-instance-stride (* 4 image-instance-words))

(defn instance-words
  "One instance's 17 words in buffer order. The last word is the container
   index and must be written through a u32 view; everything else is f32.
   `uv` comes from the source's placement ([0 0 1 1] for the dedicated tier).
   `:opacity` folds into tint alpha — effective opacity is a Cv factor."
  [{:keys [x y w h corner-radii tint opacity container-idx]} uv]
  (let [[u0 v0 u1 v1] uv
        [cr0 cr1 cr2 cr3] (or corner-radii [0.0 0.0 0.0 0.0])
        [tr tg tb ta] (or tint [1.0 1.0 1.0 1.0])
        ta (* (or ta 1.0) (or opacity 1.0))]
    [x y w h
     u0 v0 u1 v1
     cr0 cr1 cr2 cr3
     tr tg tb ta
     (or container-idx 0)]))

;; --- Scene entries (Contract O) --------------------------------------------

(defn image-order
  "Convenience Contract-O order token mirroring the frame producers' shape."
  ([stratum rank stable-tie]
   (image-order stratum rank stable-tie nil 0))
  ([stratum rank stable-tie nested-stack-path part-rank]
   {:stratum stratum
    :pass-class :direct
    :stack-path (into [[:frame/root rank rank]] (or nested-stack-path []))
    :part-rank part-rank
    :stable-tie stable-tie}))

(defn image-scene-entries
  "Per-instance tape entries for ordered image instances. One instance is one
   entry (instance-count 1 at ascending first-instance) — Contract O owns all
   ordering; nothing here re-sorts. Each instance supplies its own :order and
   :pick; :paint carries the source key and buffer range for the GPU layer to
   finalize with pipeline/bind-group/buffer identities."
  [instances]
  (vec
   (map-indexed
    (fn [index {:keys [image/id source-key order pick material-revision]}]
      (let [entry-id (or id [:image/instance index])]
        {:entry/id entry-id
         :material/id (or source-key entry-id)
         :material/revision (or material-revision 0)
         :instance/id entry-id
         :family/id :render.family/image
         :order order
         :paint {:source-key source-key
                 :vertex-count 6
                 :instance-count 1
                 :first-vertex 0
                 :first-instance index}
         :pick (or pick :none)
         :visibility {:visible? true :clip :frame-shared}}))
    instances)))

(defn contiguous-source-runs
  "The lawful batch-merge plan: walk entries in tape order and merge only
   adjacent runs sharing one paint source (Contract O — batching may merge
   order-contiguous compatible entries only). This is the planning seam for
   scene-scale image counts; the opening executor draws per entry."
  [entries]
  (reduce
   (fn [runs entry]
     (let [source-key (get-in entry [:paint :source-key])
           last-run (peek runs)]
       (if (and last-run (= (:source-key last-run) source-key))
         (conj (pop runs) (update last-run :entries conj (:entry/id entry)))
         (conj runs {:source-key source-key
                     :entries [(:entry/id entry)]}))))
   []
   entries))
