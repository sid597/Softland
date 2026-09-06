(ns app.client.harness.path
  "Drive the path kind through its records and collect evidence.

   Input: device/shared buffers and the definer's records. Output:
   deterministic image records, CPU/GPU agreements, the dependency rates a
   frame reports, colour checks and completion. The five scenarios of the
   first client change (docs/below-the-waist/path-kind/from-12-to-client.md):
   records through constructions; the crossing as a union and as dabs with
   the nonlinear width; one region meaning across readers and a returned
   path as a clip; edits and presentation at their own rates, including a
   fractional placement; the placed-ink caller is region.cljs's.

   Folder map: README.md."
  (:require [app.client.engine.color :as scene-color]
            [app.client.engine.device :as device]
            [app.client.engine.transform :as transform]
            [app.client.path.component :as path-component]
            [app.client.path.pack :as path-pack]
            [app.client.path.records :as records]
            [app.client.path.renderer :as path-renderer]
            [app.client.path.value :as v]
            [app.client.harness.shared
             :refer [canvas-size zoom-cases promise-mapv sha256-bytes
                     opaque-png-data-url pixel-rgba srgb->linear linear->srgb-byte]]))

(defn- revisioned
  "Record → validated record whose revision is its content."
  [record]
  (let [validated (path-component/validate-component! record)]
    (assoc validated :path/revision (path-component/component-content-hash validated))))

(defn- scaled
  "Record and zoom → the same picture at that zoom: samples and rectangle
   divided by the zoom, a local width scaled with them."
  [record zoom]
  (let [k (/ 1.0 zoom)
        source (:path/source record)
        source (case (:kind source)
                 :pen (update source :samples (fn [samples] (mapv (fn [[x y & rest]] (into [(* x k) (* y k)] rest)) samples)))
                 :rect (-> source (update :x * k) (update :y * k) (update :w * k) (update :h * k) (update :r #(* (or % 0.0) k)))
                 :anchors (update source :contours
                                  (fn [contours]
                                    (mapv (fn [c] (update c :anchors
                                                          (fn [as] (mapv (fn [a] (reduce (fn [a key] (if (a key) (update a key (fn [[x y]] [(* x k) (* y k)])) a)) a [:p :in :out])) as))))
                                          contours))))
        tool (cond-> (:path/tool record) (number? (get-in record [:path/tool :size])) (update :size * k))
        stroke (get-in record [:path/paint :stroke])
        stroke (cond-> stroke (number? (:width stroke)) (update :width * k))]
    (cond-> (assoc record :path/source source)
      tool (assoc :path/tool tool)
      stroke (assoc-in [:path/paint :stroke] stroke))))

(defn path-draw-item
  "ID, record and group → renderer draw item."
  [id record group]
  {:id id :path/material record :container group})

(defn- render-path-bytes!
  "Device/system/items/view/transforms and clear options → promise of
   {:bytes :frame}: updates the camera, prepares, draws the frame and reads
   the target back."
  [^js device path-system draw-items view world-transforms
   & {:keys [clear-value] :or {clear-value {:r 0.0 :g 0.0 :b 0.0 :a 0.0}}}]
  (let [row-bytes (* canvas-size 4)
        candidate? (get-in path-system [:scene-color :enabled?])
        view-format (if candidate? "rgba8unorm-srgb" "rgba8unorm")
        [pan-x pan-y] (or (:pan view) [0.0 0.0])
        target (.createTexture device (clj->js {:size {:width canvas-size :height canvas-size :depthOrArrayLayers 1}
                                                :format "rgba8unorm" :viewFormats ["rgba8unorm-srgb"]
                                                :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT js/GPUTextureUsage.COPY_SRC)}))
        read-buffer (.createBuffer device (clj->js {:size (* row-bytes canvas-size)
                                                    :usage (bit-or js/GPUBufferUsage.COPY_DST js/GPUBufferUsage.MAP_READ)}))
        camera (js/Float32Array. 6)
        _ (device/update-camera device (:camera-buffer path-system) camera pan-x pan-y (:zoom view 1.0) canvas-size canvas-size)
        frame (path-renderer/prepare-path-frame! path-system draw-items view world-transforms)
        encoder (.createCommandEncoder device)
        pass (.beginRenderPass encoder (clj->js {:colorAttachments [{:view (.createView target (clj->js {:format view-format}))
                                                                     :clearValue clear-value :loadOp "clear" :storeOp "store"}]}))]
    (path-renderer/draw-path-frame! pass path-system)
    (.end pass)
    (.copyTextureToBuffer encoder (clj->js {:texture target})
                          (clj->js {:buffer read-buffer :bytesPerRow row-bytes :rowsPerImage canvas-size})
                          (clj->js {:width canvas-size :height canvas-size :depthOrArrayLayers 1}))
    (.submit (.-queue device) #js [(.finish encoder)])
    (-> (.mapAsync read-buffer js/GPUMapMode.READ)
        (.then (fn [_]
                 (let [copy (js/Uint8Array. (js/Uint8Array. (.getMappedRange read-buffer)))]
                   (.unmap read-buffer)
                   (.destroy read-buffer)
                   (.destroy target)
                   {:bytes copy :frame frame}))))))

(defn- render-path-pair!
  "Two identical captures → first bytes, hashes and equality."
  [device path-system draw-items view world-transforms clear-value]
  (-> (render-path-bytes! device path-system draw-items view world-transforms :clear-value clear-value)
      (.then (fn [{first-bytes :bytes}]
               (-> (render-path-bytes! device path-system draw-items view world-transforms :clear-value clear-value)
                   (.then (fn [{second-bytes :bytes}]
                            (-> (js/Promise.all #js [(sha256-bytes first-bytes) (sha256-bytes second-bytes)])
                                (.then (fn [hashes]
                                         {:bytes first-bytes
                                          :first-sha256 (aget hashes 0)
                                          :second-sha256 (aget hashes 1)
                                          :byte-identical? (= (aget hashes 0) (aget hashes 1))}))))))))))

(defn- path-image-record
  [mode case-id pair]
  {:mode mode
   :file (str "gpu-path-" mode "-" case-id ".png")
   :raw-sha256 (:first-sha256 pair)
   :png-data-url (opaque-png-data-url (:bytes pair))
   :determinism {:first-raw-sha256 (:first-sha256 pair)
                 :second-raw-sha256 (:second-sha256 pair)
                 :byte-identical? (:byte-identical? pair)}})

(defn- alpha-at
  "Bytes and pixel → alpha in [0, 1]."
  [bytes x y]
  (/ (nth (pixel-rgba bytes x y) 3) 255.0))

;; ---- goldens ----

(defn- path-golden-spec
  [mode]
  (case mode
    :holed-concave
    {:case-id "holed-concave-default-unit-z1" :zoom 1.0 :mode "holed-concave"
     :record (revisioned records/holed-concave)}
    :translucent-self-crossing
    {:case-id "translucent-self-crossing-legal-z10" :zoom 10.0 :mode "translucent-self-crossing"
     :record (revisioned (scaled records/harness-z 10.0))}))

(defn- run-path-golden!
  [device path-system world-transforms mode]
  (let [{:keys [case-id zoom record]} (path-golden-spec mode)
        view {:zoom zoom :pan [0.0 0.0]}
        run (path-component/run record {:scale zoom})]
    (-> (render-path-pair! device path-system [(path-draw-item [:path-golden mode] record 0)] view world-transforms
                           {:r 0.0 :g 0.0 :b 0.0 :a 0.0})
        (.then (fn [pair]
                 {:case-id case-id :zoom zoom :lod (str "bucket-" (path-pack/scale-bucket zoom))
                  :normalization "screen-constant"
                  :shape-extent-world (/ 80.0 zoom)
                  :construction {:ops (mapv :op (:log run)) :reads (vec (sort (keys (:reads run))))
                                 :regions (mapv :kind (:regions run))}
                  :images [(path-image-record (:mode (path-golden-spec mode)) case-id pair)]})))))

(defn- run-path-tree-golden!
  "Root and child group of the same record, plus the unknown-group
   rejection."
  [device path-system world-transforms]
  (let [case-id "tree-containers-cid17-slot1"
        mode "container-tree"
        view {:zoom 1.0 :pan [0.0 0.0]}
        clear {:r 0.025 :g 0.06 :b 0.11 :a 1.0}
        record (revisioned {:path/material-id :path-golden/group-tree :path/revision ::pending
                            :path/source {:kind :rect :x 8.0 :y 8.0 :w 32.0 :h 32.0}
                            :path/paint {:fill {:rule :nonzero :color [0.18 0.82 0.58 0.96]}}})
        draw-items [(path-draw-item :path-tree/root record 0) (path-draw-item :path-tree/child record 17)]
        unknown-error (try (path-renderer/prepare-path-frame! path-system [(path-draw-item :path-tree/missing record 99)] view world-transforms)
                           nil
                           (catch :default error (ex-data error)))]
    (-> (render-path-pair! device path-system draw-items view world-transforms clear)
        (.then (fn [pair]
                 {:case-id case-id :zoom 1.0 :lod "bucket-0"
                  :normalization "path-local-with-world-transforms-group-tree"
                  :shape-extent-world 32.0
                  :buffer-indexes [(get-in world-transforms [0 :buffer-index]) (get-in world-transforms [17 :buffer-index])]
                  :unknown-group unknown-error
                  :images [(path-image-record mode case-id pair)]})))))

;; ---- scenario 1: three records, one route ----

(defn- run-records-check!
  "The draw, pen and border records through their constructions: each
   renders; a geometry edit and a behaviour edit each change the picture;
   renaming the tool changes nothing and reruns nothing."
  [device path-system world-transforms]
  (let [view {:zoom 1.0 :pan [0.0 0.0]}
        cases [{:name :draw :record records/draw-tool
                :geometry #(assoc-in % [:path/source :samples 10 0] 20.0)
                :behaviour #(assoc-in % [:path/tool :width] "size * p^2")}
               {:name :pen :record records/pen-tool
                :geometry #(assoc-in % [:path/source :contours 0 :anchors 1 :p] [110.0 60.0])
                :behaviour #(assoc-in % [:path/paint :stroke :align] :outside)}
               {:name :border :record records/border
                :geometry #(assoc-in % [:path/source :w] 60.0)
                :behaviour #(assoc-in % [:path/paint :stroke :width] 3.0)}]
        capture (fn [record]
                  (-> (render-path-bytes! device path-system
                                          [(path-draw-item [:records (:path/material-id record)] (revisioned record) 0)]
                                          view world-transforms)
                      (.then (fn [{:keys [bytes frame]}]
                               (.then (sha256-bytes bytes) (fn [h] {:sha256 h :frame frame}))))))
        one (fn [{:keys [name record geometry behaviour]}]
              (let [run (path-component/run (revisioned record) {:scale 1.0})]
                (-> (promise-mapv capture [record (assoc-in record [:path/tool :name] "renamed")
                                           (geometry record) (behaviour record)])
                    (.then (fn [[base renamed edited behaved]]
                             {:name name
                              :ok? (:ok? run)
                              :ops (mapv :op (:log run))
                              :reads (vec (sort (keys (:reads run))))
                              :regions (mapv :kind (:regions run))
                              :base (:sha256 base)
                              :geometry-edit-changes? (not= (:sha256 base) (:sha256 edited))
                              :behaviour-edit-changes? (not= (:sha256 base) (:sha256 behaved))
                              :rename-changes? (not= (:sha256 base) (:sha256 renamed))
                              :rename-runs (:runs (:frame renamed))
                              :pass? (and (:ok? run)
                                          (not-any? #(re-find #"name" %) (keys (:reads run)))
                                          (not= (:sha256 base) (:sha256 edited))
                                          (not= (:sha256 base) (:sha256 behaved))
                                          (= (:sha256 base) (:sha256 renamed))
                                          (zero? (:runs (:frame renamed))))})))))]
    (-> (promise-mapv one cases)
        (.then (fn [rows] {:rows rows :pass? (every? :pass? rows)})))))

;; ---- scenario 2: pressure and crossing ----

(defn- crossing-pixel
  "The pixel whose centre is nearest the Z's crossing point."
  []
  (let [a [26.0 28.0] b [102.0 100.0] c [28.0 100.0] d [102.0 28.0]
        r (v/sub b a) s (v/sub d c)
        t (/ (v/cross (v/sub c a) s) (v/cross r s))
        [x y] (v/add a (v/scale r t))]
    [(int (Math/floor x)) (int (Math/floor y))]))

(defn- run-crossing-check!
  "The Z as a union reads 0.62 at the crossing; as 24 dabs it reads
   0.62 + 0.38 × 0.62 = 0.8556; with the width 16 p² a dab's footprint
   follows its own pressure; and along the union's edge the GPU agrees with
   the CPU twin."
  [device path-system world-transforms]
  (let [view {:zoom 1.0 :pan [0.0 0.0]}
        [cx cy] (crossing-pixel)
        union (revisioned records/harness-z)
        dabs (revisioned records/z-as-dabs)
        nonlinear (revisioned (assoc-in records/z-nonlinear [:path/paint :stroke :overlap] :accumulate))
        run-dabs (path-component/run dabs {:scale 1.0})
        run-nonlinear (path-component/run nonlinear {:scale 1.0})
        skin (first (path-component/painted-regions union {:scale 1.0}))
        ;; 16 p² is convex, so on the first segment it lies below the
        ;; interpolation of the knot widths; on the Z the gap is a fraction of
        ;; a pixel, so the GPU receipt is the dab painted at its own radius
        ;; and the rule is checked on every dab's radius
        probe (let [rows (for [region (:regions run-nonlinear)
                               :let [{:keys [x y r p seg]} (:dab region)
                                     t (/ (- p 0.65) (- 0.9 0.65))
                                     alt-r (/ (+ (* 16.0 0.65 0.65) (* (- (* 16.0 0.9 0.9) (* 16.0 0.65 0.65)) t)) 2.0)]]
                           {:seg seg :x x :y y :r r :p p :alt-r alt-r
                            :rule-holds? (< (Math/abs (- r (* 8.0 p p))) 1.0e-6)})
                    first-segment (filter #(zero? (:seg %)) rows)
                    widest-gap (apply max-key (fn [d] (- (:alt-r d) (:r d))) first-segment)]
                {:rule-holds? (every? :rule-holds? rows)
                 :gap-local (- (:alt-r widest-gap) (:r widest-gap))
                 :sub-pixel-gap? (< (- (:alt-r widest-gap) (:r widest-gap)) 1.0)
                 :dab (select-keys widest-gap [:x :y :r :p :alt-r])
                 :centre [(int (Math/floor (:x widest-gap))) (int (Math/floor (:y widest-gap)))]})
        edge-pixels (for [x (range 20 110) y (range 20 110)
                          :let [c (path-pack/coverage-at (:pack skin) (+ x 0.5) (+ y 0.5) 1.0 1.0 :nonzero)]
                          :when (< 0.15 c 0.85)]
                      [x y c])]
    (-> (render-path-bytes! device path-system [(path-draw-item :crossing/union union 0)] view world-transforms)
        (.then (fn [{u :bytes}]
                 (-> (render-path-bytes! device path-system [(path-draw-item :crossing/dabs dabs 0)] view world-transforms)
                     (.then (fn [{d :bytes frame :frame}]
                              (-> (render-path-bytes! device path-system [(path-draw-item :crossing/nonlinear nonlinear 0)] view world-transforms)
                                  (.then (fn [{n :bytes}]
                                           (let [union-alpha (alpha-at u cx cy)
                                                 dabs-alpha (alpha-at d cx cy)
                                                 edge-rows (mapv (fn [[x y c]] {:pixel [x y] :cpu (* 0.62 c) :gpu (alpha-at u x y)}) (take 200 edge-pixels))
                                                 edge-max (reduce max 0.0 (map (fn [{:keys [cpu gpu]}] (Math/abs (- cpu gpu))) edge-rows))
                                                 centre-alpha (alpha-at n (first (:centre probe)) (second (:centre probe)))]
                                             {:crossing-pixel [cx cy]
                                              :union-alpha union-alpha :dabs-alpha dabs-alpha
                                              :dab-count (count (:regions run-dabs))
                                              :dab-instances (:instances frame)
                                              :tip (get-in dabs [:path/paint :stroke :tip])
                                              :nonlinear (assoc probe :centre-alpha centre-alpha)
                                              :edge-rows (count edge-rows) :edge-max-delta edge-max
                                              :pass? (and (< (Math/abs (- union-alpha 0.62)) 0.02)
                                                          (< (Math/abs (- dabs-alpha 0.8556)) 0.02)
                                                          (= 24 (count (:regions run-dabs)))
                                                          (= :nib (get-in dabs [:path/paint :stroke :tip]))
                                                          (:rule-holds? probe)
                                                          (> centre-alpha 0.3)
                                                          (pos? (count edge-rows))
                                                          (< edge-max 0.05))}))))))))))))

;; ---- scenario 3: one region meaning across readers ----

(defn- run-region-meaning-check!
  "The holed shape under even-odd: interior, hole and notch as declared,
   an edge probe against the CPU twin; the same shape clipped by the Z's
   returned skin; nonzero fills the hole."
  [device path-system world-transforms]
  (let [view {:zoom 1.0 :pan [0.0 0.0]}
        shape (revisioned records/holed-concave)
        fill-alpha 0.96
        z-run (path-component/run (revisioned records/harness-z) {:scale 1.0})
        skin (first (:regions z-run))
        clipped (revisioned (assoc-in records/holed-concave [:path/paint :clip] {:path (:path skin) :rule :nonzero}))
        nonzero (revisioned (assoc-in records/holed-concave [:path/paint :fill :rule] :nonzero))
        ;; the shape's edges sit on integer coordinates, where no pixel centre
        ;; is half covered; a half-pixel shift puts the left edge through
        ;; pixel centres
        shifted (revisioned (update-in records/holed-concave [:path/source :contours]
                                       (fn [cs] (mapv (fn [c] (update c :anchors (fn [as] (mapv (fn [a] (update a :p (fn [[x y]] [(+ x 0.5) y]))) as)))) cs))))
        shifted-region (first (path-component/painted-regions shifted {:scale 1.0}))
        edge (some (fn [x] (let [c (path-pack/coverage-at (:pack shifted-region) (+ x 0.5) 60.5 1.0 1.0 :even-odd)]
                             (when (< 0.2 c 0.8) [x 60 c])))
                   (range 20 30))
        skin-pack (:pack (path-pack/pack-region (:path skin) 0.05 {}))
        render (fn [id record] (.then (render-path-bytes! device path-system [(path-draw-item id record 0)] view world-transforms) (fn [r] (:bytes r))))]
    (-> (promise-mapv (fn [[id record]] (render id record))
                      [[:meaning/shape shape] [:meaning/shifted shifted] [:meaning/clipped clipped] [:meaning/nonzero nonzero]])
        (.then (fn [[s e c n]]
                 (let [[ex ey ec] edge
                       inside-z? (fn [x y] (path-pack/inside? skin-pack :nonzero (+ x 0.5) (+ y 0.5)))
                       probes (vec (for [[x y] [[90 90] [30 60] [60 40] [100 70]]]
                                     {:pixel [x y] :in-z? (inside-z? x y)
                                      :alpha (alpha-at c x y)
                                      :shape-alpha (alpha-at s x y)}))
                       edge-gpu (when edge (alpha-at e ex ey))]
                   {:interior (alpha-at s 90 90) :hole (alpha-at s 42 42) :notch (alpha-at s 64 90)
                    :edge {:pixel [ex ey] :cpu (when edge (* fill-alpha ec)) :gpu edge-gpu}
                    :nonzero-hole (alpha-at n 42 42)
                    :clip-probes probes
                    :clip-path-from :z-skin
                    :text-filler :engine/coverage
                    :pass? (and (< (Math/abs (- (alpha-at s 90 90) fill-alpha)) 0.02)
                                (zero? (alpha-at s 42 42))
                                (zero? (alpha-at s 64 90))
                                (some? edge)
                                (< (Math/abs (- (* fill-alpha ec) edge-gpu)) 0.05)
                                (< (Math/abs (- (alpha-at n 42 42) fill-alpha)) 0.02)
                                (every? (fn [{:keys [in-z? alpha shape-alpha]}]
                                          (if in-z? (< (Math/abs (- alpha shape-alpha)) 0.05) (zero? alpha)))
                                        probes))}))))))

;; ---- scenario 4: edits and presentation at their own rates ----

(defn- run-rates-check!
  "What a frame rebuilds: nothing on repeat; rows only on a colour edit;
   a run on a geometry edit; nothing on a pan or a zoom inside the bucket;
   a repack across a bucket; a rerun for a snapped record on a pan; and a
   fractional placement whose edge pixels agree with the CPU twin."
  [device path-system world-transforms fractional-transforms]
  (let [z (revisioned records/harness-z)
        border (revisioned records/border)
        view {:zoom 1.0 :pan [0.0 0.0]}
        prepare (fn [items view wt] (path-renderer/prepare-path-frame! path-system items view wt))
        f1 (prepare [(path-draw-item :rates/z z 0)] view world-transforms)
        f2 (prepare [(path-draw-item :rates/z z 0)] view world-transforms)
        f3 (prepare [(path-draw-item :rates/z (revisioned (assoc-in records/harness-z [:path/paint :stroke :color] [0.1 0.9 0.2 0.62])) 0)] view world-transforms)
        f4 (prepare [(path-draw-item :rates/z (revisioned (assoc-in records/harness-z [:path/source :samples 1 0] 96.0)) 0)] view world-transforms)
        f5 (prepare [(path-draw-item :rates/z z 0)] view world-transforms)
        f6 (prepare [(path-draw-item :rates/z z 0)] {:zoom 1.0 :pan [5.0 -3.0]} world-transforms)
        f7 (prepare [(path-draw-item :rates/z z 0)] {:zoom 1.9 :pan [5.0 -3.0]} world-transforms)
        f8 (prepare [(path-draw-item :rates/z z 0)] {:zoom 2.5 :pan [5.0 -3.0]} world-transforms)
        b1 (prepare [(path-draw-item :rates/border border 0)] view world-transforms)
        b2 (prepare [(path-draw-item :rates/border border 0)] {:zoom 1.0 :pan [0.5 0.0]} world-transforms)
        b3 (prepare [(path-draw-item :rates/border border 0)] {:zoom 1.5 :pan [0.5 0.0]} world-transforms)
        g1 (prepare [(path-draw-item :rates/z z 17)] view world-transforms)
        moved (assoc-in world-transforms [17 :affine 4] 44.0)
        g2 (prepare [(path-draw-item :rates/z z 17)] view moved)
        skin (first (path-component/painted-regions z {:scale 1.0}))
        [ox oy] [0.5 0.25]
        edge-pixels (for [x (range 20 110) y (range 20 110)
                          :let [c (path-pack/coverage-at (:pack skin) (- (+ x 0.5) ox) (- (+ y 0.5) oy) 1.0 1.0 :nonzero)]
                          :when (< 0.15 c 0.85)]
                      [x y c])]
    (-> (render-path-bytes! device path-system [(path-draw-item :rates/fraction z 18)] view fractional-transforms)
        (.then (fn [{bytes :bytes}]
                 (let [rows (mapv (fn [[x y c]] {:pixel [x y] :cpu (* 0.62 c) :gpu (alpha-at bytes x y)}) (take 300 edge-pixels))
                       max-delta (reduce max 0.0 (map (fn [{:keys [cpu gpu]}] (Math/abs (- cpu gpu))) rows))
                       counts (fn [f] (select-keys f [:changed? :runs :packs :instance-writes :instances]))]
                   {:first (counts f1) :repeat (counts f2) :colour-edit (counts f3) :geometry-edit (counts f4)
                    :restore (counts f5) :pan (counts f6) :zoom-inside-bucket (counts f7) :zoom-across-bucket (counts f8)
                    :border-first (counts b1) :border-pan (counts b2) :border-zoom (counts b3)
                    :group-first (counts g1) :group-moved (counts g2)
                    :fractional {:offset [ox oy] :edge-rows (count rows) :max-delta max-delta}
                    :pass? (and (:changed? f1) (= 1 (:runs f1)) (pos? (:packs f1)) (pos? (:instance-writes f1))
                                (not (:changed? f2))
                                (:changed? f3) (zero? (:runs f3)) (zero? (:packs f3)) (pos? (:instance-writes f3))
                                (:changed? f4) (= 1 (:runs f4)) (pos? (:packs f4))
                                (not (:changed? f6))
                                (not (:changed? f7))
                                (:changed? f8) (zero? (:runs f8)) (pos? (:packs f8))
                                (:changed? b2) (= 1 (:runs b2))
                                (:changed? b3) (= 1 (:runs b3))
                                (not (:changed? g2))
                                (pos? (count rows)) (< max-delta 0.05))}))))))

;; ---- colour ----

(defn- path-color-row!
  [device path-system world-transforms linear?]
  (let [clear {:r 0.04 :g 0.18 :b 0.35 :a 1.0}
        color [(/ 200.0 255.0) (/ 80.0 255.0) (/ 40.0 255.0) 0.5]
        record (revisioned {:path/material-id :path-color/source-over :path/revision ::pending
                            :path/source {:kind :rect :x 24.0 :y 24.0 :w 80.0 :h 80.0}
                            :path/paint {:fill {:rule :nonzero :color color}}})]
    (-> (render-path-bytes! device path-system [(path-draw-item :path-color record 0)] {:zoom 1.0 :pan [0.0 0.0]} world-transforms
                            :clear-value clear)
        (.then (fn [{bytes :bytes}]
                 (let [expected (mapv (fn [source background]
                                        (if linear?
                                          (linear->srgb-byte (+ (* (srgb->linear source) 0.5) (* background 0.5)))
                                          (js/Math.round (+ (* source 0.5) (* 255.0 background 0.5)))))
                                      [200 80 40] [0.04 0.18 0.35])
                       actual (subvec (vec (pixel-rgba bytes 64 64)) 0 3)
                       delta (apply max (map #(js/Math.abs (- %1 %2)) expected actual))]
                   {:mode (if linear? :linear-premultiplied :legacy-direct)
                    :expected expected :actual actual :max-byte-delta delta
                    :pass? (<= delta 3)}))))))

(defn- run-path-color!
  [device path-system camera groups-buffer world-transforms]
  (let [legacy-system (path-renderer/init-path-system device "rgba8unorm" camera groups-buffer
                                                      :scene-color (scene-color/scene-color false))]
    (-> (js/Promise.all #js [(path-color-row! device legacy-system world-transforms false)
                             (path-color-row! device path-system world-transforms true)])
        (.then (fn [rows]
                 (let [legacy (aget rows 0) linear (aget rows 1)]
                   (path-renderer/destroy-path-system! legacy-system)
                   {:legacy legacy :linear-premultiplied linear
                    :pass? (and (:pass? legacy) (:pass? linear))}))))))

;; ---- parity across zooms ----

(defn- path-parity-row!
  "The holed shape at each legal zoom: CPU classification against the GPU
   pixel away from a 1.25-pixel boundary band, both classes present, no
   mismatch."
  [device path-system world-transforms {:keys [case-id zoom lod]}]
  (let [record (revisioned (assoc (scaled records/holed-concave zoom) :path/material-id [:path-parity case-id]))
        regions (path-component/painted-regions record {:scale zoom})]
    (-> (render-path-bytes! device path-system [(path-draw-item [:parity case-id] record 0)] {:zoom zoom :pan [0.0 0.0]} world-transforms)
        (.then (fn [{bytes :bytes}]
                 (let [rows (for [y (range canvas-size) x (range canvas-size)
                                  :let [point [(/ (+ x 0.5) zoom) (/ (+ y 0.5) zoom)]
                                        distance-px (* zoom (reduce min ##Inf (map (fn [r] (path-pack/outline-distance (:pack r) point)) regions)))]
                                  :when (> distance-px 1.25)]
                              (let [cpu (path-component/classify-regions regions point 0.0)
                                    alpha (nth (pixel-rgba bytes x y) 3)
                                    gpu (cond (> alpha 128) :inside (< alpha 128) :outside :else :half)
                                    decisive? (and (not= :boundary cpu) (not= :half gpu))]
                                {:pixel [x y] :cpu cpu :gpu gpu :alpha alpha :decisive? decisive?
                                 :match? (when decisive? (= cpu gpu))}))
                       decisive (filter :decisive? rows)
                       mismatches (filter #(false? (:match? %)) decisive)]
                   {:case-id case-id :zoom zoom :lod lod
                    :boundary-band-screen-px 1.25
                    :decisive-count (count decisive)
                    :inside-count (count (filter #(= :inside (:cpu %)) decisive))
                    :outside-count (count (filter #(= :outside (:cpu %)) decisive))
                    :mismatch-count (count mismatches)
                    :mismatch-sample (vec (take 12 mismatches))
                    :pass? (and (pos? (count decisive))
                                (some #(= :inside (:cpu %)) decisive)
                                (some #(= :outside (:cpu %)) decisive)
                                (empty? mismatches))}))))))

;; ---- the driver ----

(defn run-path-step!
  "Device → promise of the path evidence: goldens, the five scenarios'
   checks, colour and parity; destroys its path system."
  [device]
  (let [camera (device/create-camera-buffer device)
        groups-buffer (device/create-groups-buffer device)
        registry (-> (transform/empty-registry)
                     (transform/add-group 17 {:parent 0 :affine [0.5 0.0 0.0 0.5 40.0 20.0]})
                     (transform/add-group 18 {:parent 0 :affine [1.0 0.0 0.0 1.0 0.5 0.25]}))
        world-transforms (transform/world-transforms registry)
        _ (device/write-groups! device groups-buffer world-transforms)
        system (path-renderer/init-path-system device "rgba8unorm-srgb" camera groups-buffer
                                               :scene-color (scene-color/scene-color true))]
    (-> (promise-mapv (partial run-path-golden! device system world-transforms) [:holed-concave :translucent-self-crossing])
        (.then (fn [cases] (-> (run-path-tree-golden! device system world-transforms) (.then #(conj cases %)))))
        (.then (fn [cases] {:cases cases}))
        (.then (fn [state] (-> (run-records-check! device system world-transforms) (.then #(assoc state :records %)))))
        (.then (fn [state] (-> (run-crossing-check! device system world-transforms) (.then #(assoc state :crossing %)))))
        (.then (fn [state] (-> (run-region-meaning-check! device system world-transforms) (.then #(assoc state :region-meaning %)))))
        (.then (fn [state] (-> (run-rates-check! device system world-transforms world-transforms) (.then #(assoc state :rates %)))))
        (.then (fn [state] (-> (promise-mapv (partial path-parity-row! device system world-transforms) zoom-cases) (.then #(assoc state :parity %)))))
        (.then (fn [state] (-> (run-path-color! device system camera groups-buffer world-transforms) (.then #(assoc state :color %)))))
        (.then (fn [{:keys [cases records crossing region-meaning rates parity color] :as state}]
                 (let [determinism (mapcat (fn [case] (map :determinism (:images case))) cases)
                       pass? (and (= 3 (count cases))
                                  (every? :byte-identical? determinism)
                                  (= [0 1] (:buffer-indexes (last cases)))
                                  (= :transform/unknown-group (get-in (last cases) [:unknown-group :error-type]))
                                  (:pass? records) (:pass? crossing) (:pass? region-meaning) (:pass? rates)
                                  (= 7 (count parity)) (every? :pass? parity)
                                  (:pass? color))
                       result (assoc state
                                     :coverage :analytic-shared-filler
                                     :product-pick :membership-by-winding
                                     :self-overlap-alpha :one-region-one-resolve
                                     :pass? pass?)]
                   (path-renderer/destroy-path-system! system)
                   result))))))
