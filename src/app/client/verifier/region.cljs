(ns app.client.verifier.region
     "Browser receipts for the Region3D floor and its on-plane path seam.
      Takes: a WebGPU device and loaded font assets.
      Gives: the Region3D verifier result map.
      Holds nothing."
     (:require [app.client.engine.color :as scene-color]
               [app.client.engine.compositor :as compositor-gpu]
               [app.client.engine.device :as device]
               [app.client.engine.leases :as region-bindings]
               [app.client.engine.placement :as containers]
               [app.client.path.material :as path-material]
               [app.client.path.painter :as path-painter]
               [app.client.path.tessellation :as path-tessellation]
               [app.client.region3d.material :as region3d-material]
               [app.client.region3d.oracle :as region3d-oracle]
               [app.client.region3d.painter :as region3d-painter]
               [app.client.region3d.scene :as region3d-scene]
               [app.client.verifier.path
                :refer [path-op path-ink-material path-polygon-material]]
               [app.client.verifier.shared
:refer [canvas-size color-format glyph-screen-x glyph-screen-baseline
        glyph-screen-size zoom-cases image-fixtures promise-mapv
        bytes->hex sha256-bytes sha256-string opaque-png-data-url
        q8-effective run-q8-transport! boundary-pixels byte-delta pixel-rgba
        srgb->linear linear->srgb-byte selected-limits adapter-information
        shader-digests w4-read-texture!]]))

(def ^:private region3d-owner-vi [:region3d-floor :verifier])
(def ^:private region3d-id :region3d/verifier)

(defn- region3d-tagged
  ([r g b] (region3d-tagged r g b 1.0))
  ([r g b a]
   {:rgba [r g b a] :color-space :srgb :alpha-association :straight}))

(defn- region3d-transform
  ([translation scale]
   (region3d-transform translation scale [0.0 0.0 0.0 1.0]))
  ([translation scale rotation]
   {:translation translation :rotation rotation :scale scale}))

(defn- region3d-mesh
  [id primitive translation scale color metallic roughness]
  {:object/id id :object/kind :mesh :parent nil
   :transform (region3d-transform translation scale)
   :provenance {:asserted-by :sid :act :render-verifier}
   :mesh {:kind primitive
          :params (get region3d-material/primitive-defaults primitive)}
   :material {:base-color color :metallic metallic :roughness roughness
              :emissive (region3d-tagged 0.0 0.0 0.0)}})

(defn- region3d-light [id kind translation color intensity more]
  {:object/id id :object/kind :light :parent nil
   :transform (region3d-transform translation [1.0 1.0 1.0])
   :provenance {:asserted-by :sid :act :render-verifier}
   :light (merge {:kind kind :color color :intensity intensity
                  :cast-shadow false}
                 more)})

(defn- region3d-fixture-region
  ([] (region3d-fixture-region :transparent))
  ([background-kind]
   (region3d-material/validate-region!
    {:region3d/version 1
     :extent {:width 640.0 :height 360.0 :depth 100.0}
     :background {:kind background-kind
                  :color (region3d-tagged 0.055 0.07 0.10
                                         (if (= :transparent background-kind)
                                           0.0 1.0))}
     :ambient {:color (region3d-tagged 0.72 0.80 1.0) :intensity 0.11}
     :view-default {:pivot [0.0 0.0 0.0] :distance 8.0
                    :yaw 0.0 :pitch 0.0
                    :lens region3d-material/default-perspective-lens}
     :scene
     {:near (region3d-mesh :near :box [0.0 0.0 0.0] [2.0 2.0 2.0]
                           (region3d-tagged 0.58 0.64 0.73) 0.18 0.42)
      :far (region3d-mesh :far :box [0.35 0.05 -0.75] [2.5 1.35 2.0]
                          (region3d-tagged 0.24 0.48 0.82) 0.42 0.30)
      :sphere (region3d-mesh :sphere :sphere [-2.0 -0.05 0.0]
                             [1.55 1.55 1.55]
                             (region3d-tagged 0.82 0.34 0.16) 0.68 0.23)
      :glass (region3d-mesh :glass :box [2.25 0.65 0.15] [1.35 1.35 1.35]
                            (region3d-tagged 0.14 0.82 0.70 0.5) 0.08 0.24)
      :floor (region3d-mesh :floor :plane [0.0 -1.20 0.0] [8.0 1.0 7.0]
                            (region3d-tagged 0.30 0.33 0.38) 0.0 0.86)
      :sun (region3d-light :sun :directional [4.0 8.0 6.0]
                           (region3d-tagged 1.0 0.93 0.82) 2.4
                           {:cast-shadow true})
      :point (region3d-light :point :point [-3.0 2.5 4.0]
                             (region3d-tagged 0.38 0.58 1.0) 34.0
                             {:range 18.0})
      :spot (region3d-light :spot :spot [3.0 4.0 4.0]
                            (region3d-tagged 1.0 0.34 0.20) 24.0
                            {:range 20.0
                             :cone {:inner-deg 18.0 :outer-deg 32.0}})}})))

(defn- region3d-op [region & {:keys [width height]
                              :or {width 80.0 height 88.0}}]
  {:id :region3d/verifier-node :address :region3d/verifier-address
   :region-id region3d-id :owner-vi region3d-owner-vi
   :container 0 :container-idx 0
   :x 24.0 :y 20.0 :w width :h height
   :region3d/scene region})

(defn- region3d-seam-fixture []
  (let [ink-object
        {:object/id :seam/ink :object/kind :ink :parent nil
         :transform (region3d-transform
                     [-2.7 -0.95 0.4] [0.018 0.018 0.018]
                     [0.0 -0.21644 0.0 0.976296])
         :provenance {:asserted-by :sid :act :render-verifier}
         :ink {:ref {:address :seam/ink-material}}}
        region (-> (region3d-fixture-region :opaque)
                   (assoc :region3d/version 2)
                   (assoc-in [:scene :seam/ink] ink-object)
                   region3d-material/validate-region!)
        ink-material (path-ink-material
                      :seam/ink-material 1.0
                      [[0.0 8.0 0.45] [54.0 2.0 0.9]
                       [108.0 26.0 0.62] [164.0 8.0 1.0]
                       [222.0 34.0 0.55]]
                      [0.16 0.82 1.0 0.92] 1.0)
        ink-placement
        {:object-id :seam/ink :object ink-object :kind :ink
         :address :seam/ink-material :status :resolved
         :content-revision (path-material/material-content-key ink-material)
         :cache-key (path-tessellation/material-cache-key ink-material 1.0)
         :material ink-material
         :owner {:vi :seam/ink-owner :op-id :seam/ink-material}}
        op (assoc (region3d-op region)
                  :region3d/resolved-placements
                  [ink-placement])]
    {:region region :op op
     :placements [ink-placement]}))

(defn- region3d-regions [op]
  {:regions [op]})

(defn- region3d-painters
  "The painters of one floor frame, back to front: the prepared surround
   paths (op 0 below, op 1 above) bracket the region composite."
  [{:keys [region-system surround-path-system]} sides]
  (let [surround (fn [op-index]
                   (let [{:keys [first-vertex vertex-count]}
                         (nth @(:!prepared surround-path-system) op-index)]
                     (fn [pass]
                       (path-painter/draw-path-range! pass surround-path-system
                                                  first-vertex vertex-count))))
        region (fn [pass]
                 (region3d-painter/composite-region! pass region-system
                                                 region3d-id))]
    (case sides
      :sandwich [(surround 0) region (surround 1)]
      :region [region]
      :empty [(surround 0)])))

(defn- region3d-direct-frame!
  "Direct driver, no order model: lease every desired region, encode the
   shadow and interior of each one the region painter has prepared, paint the
   painters back to front into one linear scene target, present it, and read
   the pixels back."
  [{:keys [device compositor region-system]} painters]
  (let [^js device device
        ^js texture (.createTexture
                     device
                     (clj->js {:size {:width canvas-size :height canvas-size
                                      :depthOrArrayLayers 1}
                               :format color-format
                               :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                              js/GPUTextureUsage.COPY_SRC)}))
        owner (region3d-painter/binding-owner region-system)
        {:keys [active stale]}
        (compositor-gpu/active-region-leases! compositor owner)
        encoder (.createCommandEncoder device)
        prepared (:prepared (region3d-painter/region3d-receipt region-system))
        passes (vec (for [{region-id :region/id shadow? :shadow?}
                          (region-bindings/desired-rows owner)
                          :when (contains? prepared region-id)
                          role (if shadow? [:shadow :interior] [:interior])]
                      (region3d-painter/encode-region-pass!
                       region-system encoder region-id role
                       (get active region-id))))
        scene (compositor-gpu/acquire-target!
               (:target-pool compositor) "rgba16float"
               canvas-size canvas-size "frame/scene-color")
        pass (compositor-gpu/begin-target-pass! encoder scene "clear")]
    (doseq [paint! painters] (paint! pass))
    (.end pass)
    (compositor-gpu/draw-present! compositor encoder scene
                                  (.createView texture) color-format)
    (.submit (.-queue device) #js [(.finish encoder)])
    (compositor-gpu/release-after-submit! compositor [scene] [] stale)
    (-> (w4-read-texture! device texture canvas-size canvas-size)
        (.then (fn [bytes]
                 (.destroy texture)
                 {:bytes bytes :passes passes})))))

(defn- region3d-prepare-options [harness]
  {:zoom 1.0 :dpr 1.0
   :font-assets (:font-assets harness)
   :path-system (:path-system harness)})

(defn- region3d-capture!
  ([harness op session sides]
   (region3d-capture! harness op session sides {}))
  ([{:keys [compositor region-system] :as harness}
    op session sides prepare-overrides]
   (region3d-painter/attach-compositor! region-system compositor)
   (region3d-painter/prepare-region3d-frame!
    region-system (region3d-regions op) session
    (merge (region3d-prepare-options harness) prepare-overrides))
   (region3d-direct-frame! harness (region3d-painters harness sides))))

(defn- region3d-capture-pair!
  ([harness op session sides]
   (region3d-capture-pair! harness op session sides {}))
  ([{:keys [compositor region-system] :as harness}
    op session sides prepare-overrides]
   (region3d-painter/attach-compositor! region-system compositor)
   (region3d-painter/prepare-region3d-frame!
    region-system (region3d-regions op) session
    (merge (region3d-prepare-options harness) prepare-overrides))
   (let [painters (region3d-painters harness sides)]
     (-> (region3d-direct-frame! harness painters)
         (.then
          (fn [{first-bytes :bytes}]
            (-> (region3d-direct-frame! harness painters)
                (.then
                 (fn [{second-bytes :bytes}]
                   (-> (js/Promise.all
                        #js [(sha256-bytes first-bytes)
                             (sha256-bytes second-bytes)])
                       (.then
                        (fn [hashes]
                          {:bytes first-bytes
                           :first-sha256 (aget hashes 0)
                           :second-sha256 (aget hashes 1)
                           :byte-identical? (= (aget hashes 0)
                                               (aget hashes 1))}))))))))))))

(defn- region3d-image-record [case-id pair]
  {:mode case-id :file (str "gpu-region3d-floor-" case-id ".png")
   :raw-sha256 (:first-sha256 pair)
   :png-data-url (opaque-png-data-url (:bytes pair))
                 :determinism {:first-raw-sha256 (:first-sha256 pair)
                 :second-raw-sha256 (:second-sha256 pair)
                 :byte-identical? (:byte-identical? pair)}})

(defn- region3d-lights [maintained]
  (->> (get-in maintained [:region :scene])
       (keep (fn [[id object]]
               (when (= :light (:object/kind object))
                 (let [matrix (get-in maintained [:effective-transforms id])]
                   {:light (:light object)
                    :position (region3d-scene/transform-point matrix
                                                              [0.0 0.0 0.0])
                    :direction (region3d-scene/transform-direction
                                matrix [0.0 0.0 -1.0])}))))
       vec))

(defn- region3d-lit-oracle [region bytes]
  (let [maintained (assoc (region3d-scene/derive-scene region)
                          :region-id region3d-id)
        camera (region3d-scene/camera-matrices (:view-default region)
                                                [80.0 88.0])
        hit (region3d-scene/pick-region
             {:maintained maintained :camera camera :region-point [40.0 44.0]})
        object (get-in region [:scene (:object-id hit)])
        linear (region3d-oracle/shade-reference
                {:material (:material object) :normal (:normal hit)
                 :point (:point3 hit) :eye (:eye camera)
                 :lights (region3d-lights maintained)
                 :ambient (:ambient region) :bvh (:bvh maintained)
                 :object-id (:object-id hit)})
        expected (conj (mapv #(linear->srgb-byte %) (take 3 linear))
                       (js/Math.round (* 255.0 (last linear))))
        actual (pixel-rgba bytes 64 64)
        delta (apply max (map #(js/Math.abs (- %1 %2)) expected actual))
        glass-screen (:screen (region3d-scene/project-point camera
                                                             [2.25 0.65 0.825]))
        [glass-x glass-y] (mapv js/Math.floor glass-screen)
        glass-sample (pixel-rgba bytes (+ 24 glass-x) (+ 20 glass-y))
        outside-point [1.0 1.0]
        outside-pick (region3d-scene/pick-region
                      {:maintained maintained :camera camera
                       :region-point outside-point})
        outside-sample (pixel-rgba bytes 25 21)
        boundary-screen (:screen (region3d-scene/project-point
                                  camera [1.0 1.0 1.0]))
        boundary-pick (region3d-scene/pick-region
                       {:maintained maintained :camera camera
                        :region-point boundary-screen})
        [boundary-x boundary-y] (mapv js/Math.floor boundary-screen)
        boundary-sample (pixel-rgba bytes (+ 24 boundary-x)
                                    (+ 20 boundary-y))
        depth-classes
        {:interior {:cpu-route (:route hit) :cpu-object (:object-id hit)
                    :gpu-rgba actual}
         :outside {:cpu-route (:route outside-pick) :gpu-rgba outside-sample}
         :boundary {:cpu-route (:route boundary-pick)
                    :cpu-object (:object-id boundary-pick)
                    :cpu-boundary? (:boundary? boundary-pick)
                    :gpu-rgba boundary-sample
                    :screen boundary-screen}}
        depth-classes-pass?
        (and (= :object (get-in depth-classes [:interior :cpu-route]))
             (= :near (get-in depth-classes [:interior :cpu-object]))
             (pos? (last (get-in depth-classes [:interior :gpu-rgba])))
             (= :region-background
                (get-in depth-classes [:outside :cpu-route]))
             (zero? (last (get-in depth-classes [:outside :gpu-rgba])))
             (= :object (get-in depth-classes [:boundary :cpu-route]))
             (= :near (get-in depth-classes [:boundary :cpu-object]))
             (true? (get-in depth-classes [:boundary :cpu-boundary?]))
             (pos? (last (get-in depth-classes [:boundary :gpu-rgba]))))]
    {:sample [64 64] :expected-object :near
     :actual-object (:object-id hit) :expected-t 6.9 :actual-t (:t hit)
     :expected-rgba expected :actual-rgba actual :max-byte-delta delta
     :epsilon-bytes 2
     :transparency-sample [(+ 24 glass-x) (+ 20 glass-y)]
     :transparency-rgba glass-sample
     :depth-classes depth-classes
     :depth-classes-pass? depth-classes-pass?
     :pass? (and (= :near (:object-id hit))
                 (<= (js/Math.abs (- 6.9 (:t hit))) 1.0e-6)
                 (<= delta 2)
                 (< 0 (last glass-sample) 255)
                 depth-classes-pass?)}))

(defn- region3d-s5-lifecycle!
  [{:keys [device compositor region-system] :as harness} region op]
  (let [base-view (:view-default region)
        changed-view (assoc base-view :yaw 0.045 :pitch -0.02)
        before-view (region3d-painter/region3d-receipt region-system)
        wait-for-queue
        (fn [value-fn]
          (.then (.onSubmittedWorkDone (.-queue ^js device))
                 value-fn))
        steps
        [(fn [_]
           (.then
            (region3d-capture!
             harness op {:regions {region3d-id {:view changed-view}}} :region)
            (fn [{view-passes :passes}]
              (let [after-view (region3d-painter/region3d-receipt region-system)]
                (.then
                 (region3d-capture!
                  harness op {:regions {region3d-id {:view changed-view}}}
                  :region)
                 (fn [{clean-passes :passes}]
                   {:before-view before-view :after-view after-view
                    :view-passes view-passes
                    :clean-passes clean-passes}))))))
         (fn [state]
           (let [resized (assoc op :w 300.0)]
             (.then
              (region3d-capture!
               harness resized {:regions {region3d-id {:view changed-view}}}
               :region)
              (fn [_]
                (wait-for-queue
                 (fn []
                   (assoc state :resize
                          (compositor-gpu/region-leases-receipt compositor)
                          :resized-op resized)))))))
         (fn [{:keys [resized-op] :as state}]
           (let [shadow-off (assoc-in region
                                      [:scene :sun :light :cast-shadow] false)
                 shadow-off-op (assoc resized-op :region3d/scene shadow-off)]
             (.then
              (region3d-capture!
               harness shadow-off-op
               {:regions {region3d-id {:view changed-view}}} :region)
              (fn [_]
                (wait-for-queue
                 (fn []
                   (assoc state :shadow-off
                          (compositor-gpu/region-leases-receipt compositor))))))))
         (fn [state]
           (region3d-painter/prepare-region3d-frame!
            region-system {:regions []} {} {:zoom 1.0 :dpr 1.0})
           (let [frame (region3d-direct-frame!
                        harness (region3d-painters harness :empty))]
             (.then
              frame
              (fn [_]
                (wait-for-queue
                 (fn []
                   (assoc state :after-close
                          (compositor-gpu/region-leases-receipt compositor))))))))
         (fn [state]
           (let [refusal-compositor
                 (compositor-gpu/create-compositor!
                  device color-format
                  :budget-cap-bytes (* 5 1024 1024))
                 refusal-harness (assoc harness :compositor refusal-compositor)]
             (.then
              (region3d-capture! refusal-harness op {} :region)
              (fn [{:keys [bytes]}]
                (wait-for-queue
                 (fn []
                   (let [receipt
                         (compositor-gpu/compositor-receipt refusal-compositor)
                         sample (pixel-rgba bytes 64 64)
                         next-state
                         (assoc state :refusal
                                {:receipt receipt :sample sample
                                 :pass? (and (some? (:last-region-refusal receipt))
                                             (pos? (apply max sample)))})]
                     (compositor-gpu/destroy-compositor! refusal-compositor)
                     (region3d-painter/attach-compositor! region-system compositor)
                     (region3d-painter/prepare-region3d-frame!
                      region-system {:regions []} {} {:zoom 1.0 :dpr 1.0})
                     next-state)))))))
         (fn [state]
           (let [first-compositor
                 (compositor-gpu/create-compositor!
                  device color-format)
                 first-harness (assoc harness :compositor first-compositor)]
             (.then
              (region3d-capture! first-harness op {} :region)
              (fn [_]
                (wait-for-queue
                 (fn []
                   (let [before-destroy
                         (compositor-gpu/region-leases-receipt
                          first-compositor)]
                     (compositor-gpu/destroy-compositor! first-compositor)
                     (let [after-destroy
                           (compositor-gpu/region-leases-receipt
                            first-compositor)
                           recreated
                           (compositor-gpu/create-compositor!
                            device color-format)
                           recreated-harness (assoc harness
                                                      :compositor recreated)]
                       (.then
                        (region3d-capture! recreated-harness op {} :region)
                        (fn [{:keys [passes]}]
                          (wait-for-queue
                           (fn []
                             (let [after-recreate
                                   (compositor-gpu/region-leases-receipt
                                    recreated)
                                   receipt
                                   {:before-destroy before-destroy
                                    :after-destroy after-destroy
                                    :after-recreate after-recreate
                                    :passes passes
                                    :pass?
                                    (and (pos? (:bytes before-destroy))
                                         (zero? (:bytes after-destroy))
                                         (pos? (:bytes after-recreate))
                                         (every? :encoded? passes))}]
                               (compositor-gpu/destroy-compositor! recreated)
                               (region3d-painter/attach-compositor!
                                region-system compositor)
                               (region3d-painter/prepare-region3d-frame!
                                region-system {:regions []} {}
                                {:zoom 1.0 :dpr 1.0})
                               (assoc state :destroy-recreate receipt))))))))))))))
         (fn [{:keys [before-view after-view view-passes clean-passes
                      resize shadow-off after-close refusal destroy-recreate]
               :as receipt}]
           (let [object-upload-delta
                 (- (:object-instance-uploads after-view)
                    (:object-instance-uploads before-view))
                 mesh-upload-delta
                 (- (:mesh-vertex-uploads after-view)
                    (:mesh-vertex-uploads before-view))
                 uniform-upload-delta
                 (- (:uniform-uploads after-view)
                    (:uniform-uploads before-view))
                 view-by-role (into {} (map (juxt :role identity)) view-passes)
                 clean? (every? #(and (:held? %) (not (:encoded? %)))
                                clean-passes)
                 resize-leases (vals (:leases resize))
                 shadow-off-leases (vals (:leases shadow-off))
                 pass? (and (zero? object-upload-delta)
                            (zero? mesh-upload-delta)
                            (= 1 uniform-upload-delta)
                            (get-in view-by-role [:interior :encoded?])
                            (get-in view-by-role [:shadow :held?])
                            clean?
                            (= 1 (count resize-leases))
                            (= [512 256] (:size (first resize-leases)))
                            (= 1 (count shadow-off-leases))
                            (< (:bytes shadow-off) (:bytes resize))
                            (zero? (:bytes after-close))
                            (:pass? refusal)
                            (:pass? destroy-recreate))]
             (-> receipt
                 (dissoc :resized-op)
                 (assoc :camera-wake
                        {:object-instance-upload-delta object-upload-delta
                         :mesh-vertex-upload-delta mesh-upload-delta
                         :uniform-upload-delta uniform-upload-delta
                         :passes view-passes}
                        :clean-held? clean?
                        :pass? pass?))))]]
    (reduce (fn [promise step] (.then promise step))
            (js/Promise.resolve nil)
            steps)))

(def ^:private lower-resolution-pressure-id :region3d/lower-resolution-pressure)

(defn- pool-holds-free-target?
  [pool target]
  (let [target-id (:target/id target)]
    (boolean
     (some #(= target-id (:target/id %))
           (mapcat val (:free @(:!state pool)))))))

(defn- region3d-lower-step!
  [harness painters]
  (-> (region3d-direct-frame! harness painters)
      (.then (fn [{:keys [bytes passes]}]
               {:bytes bytes :passes passes
                :receipt (compositor-gpu/compositor-receipt
                          (:compositor harness))}))))

(defn- region3d-refusal-leg!
  [{:keys [device region-system] :as harness}
   region op budget-cap-bytes]
  (let [compositor (compositor-gpu/create-compositor!
                    device color-format
                    :budget-cap-bytes budget-cap-bytes)
        refusal-harness (assoc harness :compositor compositor)]
    (-> (region3d-capture! refusal-harness op {} :region {:zoom 8.0})
        (.then
         (fn [{:keys [bytes]}]
           (let [receipt (compositor-gpu/compositor-receipt compositor)
                 result {:bytes bytes
                         :receipt receipt
                         :sample (pixel-rgba bytes 64 64)
                         :pass? (and (some? (:last-region-refusal receipt))
                                     (pos? (apply max (pixel-rgba bytes 64 64))))}]
             (compositor-gpu/destroy-compositor! compositor)
             (region3d-painter/prepare-region3d-frame!
              region-system {:regions []} {} {:zoom 1.0 :dpr 1.0})
             result))))))

(defn- region3d-lower-resolution!
  [{:keys [device region-system compositor] :as harness} region op]
  (let [lower-compositor
        (compositor-gpu/create-compositor!
         device color-format :budget-cap-bytes (* 64 1024 1024))
        lower-harness (assoc harness :compositor lower-compositor)
        pool (:target-pool lower-compositor)
        reserve-target
        (compositor-gpu/acquire-target!
         pool "rgba16float" 512 256 "frame/group-output/lower-resolution"
         :usage js/GPUTextureUsage.RENDER_ATTACHMENT)
        _ (compositor-gpu/release-target! pool reserve-target)
        pressure-lease
        (compositor-gpu/acquire-region-lease!
         lower-compositor lower-resolution-pressure-id 768 512 false)
        pressure-row {:region/id lower-resolution-pressure-id
                      :lease-size [768 512] :shadow? false
                      :background nil :encode-rung 1
                      :composite {:x 0.0 :y 0.0 :w 1.0 :h 1.0
                                  :container-idx 0}}
        prepare-frame
        (fn [current-op zoom]
          (region3d-painter/attach-compositor! region-system lower-compositor)
          (region3d-painter/prepare-region3d-frame!
           region-system (region3d-regions current-op) {}
           (merge (region3d-prepare-options lower-harness) {:zoom zoom}))
          (region3d-painters lower-harness :region))
        install-pressure!
        (fn []
          (let [owner (region3d-painter/binding-owner region-system)
                primary (first (filter #(= region3d-id (:region/id %))
                                       (region-bindings/desired-rows owner)))
                physical (compositor-gpu/region-lease
                          lower-compositor lower-resolution-pressure-id)]
            (region-bindings/reconcile-desired!
             owner [primary pressure-row])
            (region-bindings/record-lease!
             owner lower-resolution-pressure-id (or physical pressure-lease))))
        mutated-region
        (-> region
            (assoc-in [:scene :near :material :base-color]
                      (region3d-tagged 0.12 0.92 0.28))
            region3d-material/validate-region!)
        mutated-op (assoc op :region3d/scene mutated-region)
        steps
        [(fn [_]
           (let [frame (prepare-frame op 2.0)]
             (install-pressure!)
             (region3d-lower-step! lower-harness frame)))
         (fn [state]
           (let [frame (prepare-frame op 8.0)]
             (install-pressure!)
             (.then (region3d-lower-step! lower-harness frame)
                    #(assoc state :worn %))))
         (fn [state]
           (let [frame (prepare-frame mutated-op 8.0)]
             (install-pressure!)
             (.then (region3d-lower-step! lower-harness frame)
                    #(assoc state :mutated % :mutated-frame frame))))
         (fn [{:keys [mutated-frame] :as state}]
           (.then (region3d-lower-step! lower-harness mutated-frame)
                  #(assoc state :held %)))
         (fn [state]
           (let [frame (prepare-frame mutated-op 10.0)]
             (install-pressure!)
             (.then (region3d-lower-step! lower-harness frame)
                    #(assoc state :honest-counter %))))
         (fn [state]
           (let [frame (prepare-frame mutated-op 8.0)
                 owner (region3d-painter/binding-owner region-system)
                 primary (first (region-bindings/desired-rows owner))]
             (region-bindings/reconcile-desired! owner [primary])
             (compositor-gpu/release-region-lease!
              lower-compositor lower-resolution-pressure-id)
             (.then (region3d-lower-step! lower-harness frame)
                    #(assoc state :recovered %))))
         (fn [state]
           (let [no-shadow-region
                 (-> region
                     (assoc-in [:scene :sun :light :cast-shadow] false)
                     region3d-material/validate-region!)
                 no-shadow-op (assoc op :region3d/scene no-shadow-region)]
             (.then
              (region3d-refusal-leg! harness no-shadow-region no-shadow-op
                                     (* 3 1024 1024))
              #(assoc state :no-shadow-floor %))))
         (fn [state]
           (.then
            (region3d-refusal-leg! harness region op (* 5 1024 1024))
            #(assoc state :shadowed-floor %)))
         (fn [{:keys [bytes worn mutated held honest-counter recovered
                      no-shadow-floor shadowed-floor]
               :as state}]
           (-> (js/Promise.all
                #js [(sha256-bytes (get-in state [:mutated :bytes]))
                     (sha256-bytes (get-in state [:held :bytes]))
                     (sha256-bytes (:bytes no-shadow-floor))
                     (sha256-bytes (:bytes shadowed-floor))])
               (.then
                (fn [hashes]
                  (let [sharp state
                        primary-lease
                        (fn [step]
                          (first
                           (filter #(= region3d-id (:region-id %))
                                   (vals (get-in step
                                                 [:receipt :region-leases
                                                  :leases])))))
                        worn-lease (primary-lease worn)
                        mutated-passes (:passes mutated)
                        held-passes (:passes held)
                        recovered-lease (primary-lease recovered)
                        wear-sample (pixel-rgba (:bytes mutated) 100 24)
                        maintained (assoc (region3d-scene/derive-scene
                                           mutated-region)
                                          :region-id region3d-id)
                        pick-camera (region3d-scene/camera-matrices
                                     (:view-default mutated-region)
                                     [640.0 704.0])
                        object-pick (region3d-scene/pick-region
                                     {:maintained maintained
                                      :camera pick-camera
                                      :region-point [320.0 352.0]})
                        background-pick (region3d-scene/pick-region
                                         {:maintained maintained
                                          :camera pick-camera
                                          :region-point [5.0 5.0]})
                        reserve-preserved?
                        (pool-holds-free-target? pool reserve-target)
                        physical-crossing?
                        (and (= 1 (get-in worn [:receipt :lease-activity
                                               :region-binding-updates]))
                             (= 1 (get-in worn [:receipt :lease-activity :leases-acquired]))
                             (= 1 (get-in worn [:receipt :lease-activity :leases-retired]))
                             (= 1 (get-in worn [:receipt :lease-activity :region-rungs-worn])))
                        current-content?
                        (and (> (byte-delta bytes (:bytes mutated)) 2)
                             (every? :encoded? mutated-passes))
                        held-stable?
                        (and (zero? (get-in held [:receipt :lease-activity
                                                 :region-binding-updates]))
                             (zero? (get-in held [:receipt :lease-activity :leases-acquired]))
                             (zero? (get-in held [:receipt :lease-activity :leases-retired]))
                             (every? #(and (:held? %) (not (:encoded? %)))
                                     held-passes))
                        honest-counter?
                        (and (= [512 512]
                                (get-in honest-counter
                                        [:receipt :region-leases :leases
                                         [:region3d/verifier 512 512] :size]))
                             (zero? (get-in honest-counter
                                            [:receipt :lease-activity :region-binding-updates]))
                             (zero? (get-in honest-counter
                                            [:receipt :lease-activity :leases-acquired]))
                             (zero? (get-in honest-counter
                                            [:receipt :lease-activity :leases-retired])))
                        recovery?
                        (and (= 1 (:rung-divisor recovered-lease))
                             (= [768 768] (:size recovered-lease))
                             (= 1 (get-in recovered
                                          [:receipt :lease-activity :region-binding-updates]))
                             (= 1 (get-in recovered [:receipt :lease-activity :leases-acquired]))
                             (= 1 (get-in recovered [:receipt :lease-activity :leases-retired]))
                             (= 1 (get-in recovered
                                          [:receipt :lease-activity :region-rung-recoveries])))
                        floor-identical? (= (aget hashes 2) (aget hashes 3))
                        pick? (and (= :near (:object-id object-pick))
                                   (= :region-background
                                      (:route background-pick)))
                        deterministic? (= (aget hashes 0) (aget hashes 1))
                        worn? (and (= 2 (:rung-divisor worn-lease))
                                   (= [512 512] (:size worn-lease))
                                   (empty? (get-in worn [:receipt :pool
                                                         :refusals]))
                                   (nil? (get-in worn [:receipt
                                                       :last-region-refusal])))
                        glyph? (and (> (nth wear-sample 1) (nth wear-sample 0))
                                    (> (nth wear-sample 2) (nth wear-sample 0)))
                        pass? (and (not (:refused? pressure-lease))
                                   worn? physical-crossing? current-content?
                                   held-stable? honest-counter? recovery?
                                   reserve-preserved? deterministic? floor-identical?
                                   (:pass? no-shadow-floor)
                                   (:pass? shadowed-floor) pick? glyph?)
                        pair {:bytes (:bytes mutated)
                              :first-sha256 (aget hashes 0)
                              :second-sha256 (aget hashes 1)
                              :byte-identical? deterministic?}
                        result {:image (region3d-image-record "worn" pair)
                                :sharp {:lease (primary-lease sharp)
                                        :lease-activity (get-in sharp [:receipt :lease-activity])}
                                :worn {:lease worn-lease :lease-activity (get-in worn [:receipt :lease-activity])
                                       :rung-receipts
                                       (get-in worn [:receipt
                                                     :region-rung-receipts])}
                                :current-content? current-content?
                                :wear-sample wear-sample :glyph? glyph?
                                :held-stable? held-stable?
                                :honest-counter? honest-counter?
                                :recovery {:lease recovered-lease
                                           :lease-activity (get-in recovered [:receipt :lease-activity])
                                           :pass? recovery?}
                                :reserve-preserved? reserve-preserved?
                                :pick {:object (:object-id object-pick)
                                       :background (:route background-pick)
                                       :pass? pick?}
                                :floor {:no-shadow
                                        (dissoc no-shadow-floor :bytes)
                                        :shadowed
                                        (dissoc shadowed-floor :bytes)
                                        :byte-identical? floor-identical?}
                                :deterministic? deterministic?
                                :pass? pass?}]
                    (compositor-gpu/destroy-compositor! lower-compositor)
                    (region3d-painter/attach-compositor! region-system compositor)
                    (region3d-painter/prepare-region3d-frame!
                     region-system {:regions []} {} {:zoom 1.0 :dpr 1.0})
                    result)))))]]
    (reduce (fn [promise step] (.then promise step))
            (js/Promise.resolve nil)
            steps)))

(defn run-region3d-floor! [device font-assets]
  (let [camera (device/create-camera-buffer device)
        containers-buffer (device/create-containers-buffer device)
        surround-effective
        {0 {:affine containers/identity-affine :flags 0 :layer 0
            :stack-path [[0 0]] :transport-slot 0}}
        _ (device/update-camera device camera (js/Float32Array. 6)
                                  0.0 0.0 1.0 canvas-size canvas-size)
        _ (device/write-containers!
           device containers-buffer surround-effective)
        surround-path-system
        (path-painter/init-path-system
         device "rgba16float" camera containers-buffer
         :initial-capacity 16
         :scene-color (scene-color/scene-color true))
        surround-ops
        [(path-op
          :region3d/below
          (path-polygon-material
           :region3d/below
           [[8.0 8.0] [120.0 8.0] [120.0 120.0] [8.0 120.0]]
           [0.04 0.07 0.15 1.0] 1.0)
          0)
         (path-op
          :region3d/above
          (path-polygon-material
           :region3d/above
           [[10.0 58.0] [118.0 58.0] [118.0 70.0] [10.0 70.0]]
           [0.98 0.72 0.12 0.88] 1.0)
          0)]
        _ (path-painter/prepare-path-frame!
           surround-path-system surround-ops 1.0 surround-effective)
        region-system (region3d-painter/ensure-region3d-system!
                       device camera containers-buffer)
        path-system
        (path-painter/init-path-system
         device "rgba16float" camera containers-buffer
         :scene-color (scene-color/scene-color true))
        compositor (compositor-gpu/create-compositor!
                    device color-format)
        harness {:device device :camera camera
                 :containers-buffer containers-buffer
                 :surround-path-system surround-path-system
                 :region-system region-system
                 :path-system path-system
                 :font-assets font-assets :compositor compositor}
        opaque-region (region3d-fixture-region :opaque)
        transparent-region (region3d-fixture-region :transparent)
        opaque-op (region3d-op opaque-region)
        transparent-op (region3d-op transparent-region)
        seam (region3d-seam-fixture)
        seam-region (:region seam)
        seam-op (:op seam)
        specs [{:case-id "sandwich" :region opaque-region :op opaque-op
                :session {} :sides :sandwich}
               {:case-id "lit-depth-shadow" :region transparent-region
                :op transparent-op :session {} :sides :region}
               {:case-id "placed-depth-interleave" :region seam-region
                :op seam-op :session {} :sides :sandwich
                :seam? true}]]
    (-> (promise-mapv
         (fn [{:keys [case-id region op session sides seam?]}]
           (-> (region3d-capture-pair! harness op session sides)
               (.then
                (fn [pair]
                  (let [placement-receipt
                        (:placements
                         (region3d-painter/region3d-receipt region-system))]
                    {:case-id case-id :zoom 1.0
                     :regime :region3d-floor-default
                     :normalization :region-local-3d-inside-world-2d
                     :shape-extent-world [(:w op) (:h op)]
                     :oracle (when (= case-id "lit-depth-shadow")
                               (region3d-lit-oracle region (:bytes pair)))
                     :seam-receipt
                     (when seam?
                       {:resolved (count (filter #(= :resolved (:status %))
                                                 (:region3d/resolved-placements op)))
                        :ink-vertices (:ink-vertices placement-receipt)})
                     :images [(region3d-image-record case-id pair)]})))))
         specs)
        (.then
         (fn [cases]
           (-> (region3d-capture! harness transparent-op {} :region)
               (.then
                (fn [_]
                  (-> (region3d-s5-lifecycle!
                       harness transparent-region transparent-op)
                      (.then
                       (fn [s5]
                         (-> (region3d-lower-resolution!
                              harness transparent-region transparent-op)
                             (.then (fn [lower]
                                      {:cases cases :s5 s5 :lower lower})))))))))))
        (.then
         (fn [{:keys [cases s5 lower]}]
           (let [base-cases cases
                 s2 (get-in base-cases [1 :oracle])
                 s4 s2
                 cases (conj base-cases
                             {:case-id "worn" :zoom 8.0
                              :regime :region3d-floor-worn
                              :normalization :region-local-3d-inside-world-2d
                              :shape-extent-world [(:w transparent-op)
                                                   (:h transparent-op)]
                              :images [(:image lower)]})
                 determinism (mapcat #(map :determinism (:images %)) cases)
                 system-receipt (region3d-painter/region3d-receipt region-system)
                 seam-receipt (:seam-receipt (last base-cases))
                 compositor-receipt
                 (compositor-gpu/compositor-receipt compositor)
                 seam-pass? (and (= 1 (:resolved seam-receipt))
                                 (pos? (or (:ink-vertices seam-receipt) 0)))
                 pass? (and (= 4 (count cases))
                            (every? :byte-identical? determinism)
                            (:pass? s2) (:pass? s4) (:pass? s5) seam-pass?
                            (:pass? lower))
                 result {:cases cases
                         :s2 s2 :s4 s4 :s5 s5
                         :lower-resolution (dissoc lower :image)
                         :seam (assoc seam-receipt :pass? seam-pass?)
                         :system system-receipt
                         :compositor compositor-receipt
                         :fixture-query "?region3d=1"
                         :pass? pass?}]
             (compositor-gpu/destroy-compositor! compositor)
             (path-painter/destroy-path-system! path-system)
             (path-painter/destroy-path-system! surround-path-system)
             (region3d-painter/destroy-region3d-system! region-system)
             result))))))
