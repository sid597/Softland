(ns app.client.harness.region
     "Drive Region3D preparation, passes and lease pressure.

      Input: device/font assets, synthetic 3D scenes, resolved ink
      placements and compositor budgets. Output: four image cases and checks
      for picking/lighting, update classification, camera wake, lease
      lifetime, resolution reduction/recovery and rejection. It owns
      harness-local camera/groups, path and region systems, compositors and
      accumulated diagnostic state.

      The frame sequence is prepare desired regions, reconcile leases,
      encode shadow/interior passes, composite into linear scene color,
      present, submit and read back.

      Folder map: README.md."
     (:require [app.client.engine.color :as scene-color]
               [app.client.engine.compositor :as compositor-gpu]
               [app.client.engine.device :as device]
               [app.client.engine.leases :as region-bindings]
               [app.client.engine.transform :as transform]
               [app.client.path.component :as path-component]
            [app.client.path.construction :as path-construction]
               [app.client.path.renderer :as path-renderer]
               [app.client.region3d.on-plane :as on-plane]
               [app.client.region3d.component :as region3d-component]
               [app.client.harness.region-oracle :as region3d-oracle]
               [app.client.region3d.renderer :as region3d-renderer]
               [app.client.region3d.scene :as region3d-scene]
               [app.client.harness.path :refer [path-draw-item]]
               [app.client.harness.shared
:refer [canvas-size color-format glyph-screen-x glyph-screen-baseline
        glyph-screen-size zoom-cases image-fixtures promise-mapv
        bytes->hex sha256-bytes sha256-string opaque-png-data-url
        q8-world-transforms run-q8-transport! boundary-pixels byte-delta pixel-rgba
        srgb->linear linear->srgb-byte adapter-information
        shader-digests w4-read-texture!]]))

(def ^:private region3d-id :region3d/harness)

(defn- region3d-tagged
  "RGB or RGBA numbers → tagged color.

   Fixture constructor with default alpha."
  ([r g b] (region3d-tagged r g b 1.0))
  ([r g b a]
   {:rgba [r g b a] :color-space :srgb :alpha-association :straight}))

(defn- region3d-transform
  "Translation, scale and optional rotation → object transform map.

   Explicit TRS defaults."
  ([translation scale]
   (region3d-transform translation scale [0.0 0.0 0.0 1.0]))
  ([translation scale rotation]
   {:translation translation :rotation rotation :scale scale}))

(defn- region3d-mesh
  "ID, primitive, TRS, color and metallic/roughness → mesh object.

   Builds a canonical primitive/component fixture with provenance. Intended
   for controlled scene input."
  [id primitive translation scale color metallic roughness]
  {:object/id id :object/kind :mesh :parent nil
   :transform (region3d-transform translation scale)
   :provenance {:asserted-by :sid :act :render-harness}
   :mesh {:kind primitive
          :params (get region3d-component/primitive-defaults primitive)}
   :component {:base-color color :metallic metallic :roughness roughness
              :emissive (region3d-tagged 0.0 0.0 0.0)}})

(defn- region3d-light
  "ID, kind, transform/color/intensity and overrides → light object.

   Merges kind-independent defaults with fixture-specific settings.
   Overrides can deliberately change defaults."
  [id kind translation color intensity more]
  {:object/id id :object/kind :light :parent nil
   :transform (region3d-transform translation [1.0 1.0 1.0])
   :provenance {:asserted-by :sid :act :render-harness}
   :light (merge {:kind kind :color color :intensity intensity
                 :cast-shadow false}
                 more)})

(defn- region3d-remint
  "Region → validated region with fresh content-derived revision.

   Removes old identity fields before recomputing. Intended for fixture
   mutation tracking."
  [region]
  (let [content (dissoc region :region/revision)]
    (region3d-component/validate-region!
     (assoc content :region/revision (hash content)))))

(defn- region3d-fixture-region
  "Optional opaque/transparent background mode → revisioned scene region.

   Near/far boxes, sphere, glass, floor and three light kinds;
   migration/default path retained. Serves as a broad scene fixture, not a
   scene complexity sweep."
  ([] (region3d-fixture-region :transparent))
  ([background-kind]
   (region3d-remint
    {:region/id region3d-id
     :region3d/version 1
     :extent {:width 640.0 :height 360.0 :depth 100.0}
     :background {:kind background-kind
                  :color (region3d-tagged 0.055 0.07 0.10
                                         (if (= :transparent background-kind)
                                           0.0 1.0))}
     :ambient {:color (region3d-tagged 0.72 0.80 1.0) :intensity 0.11}
     :view {:pivot [0.0 0.0 0.0] :distance 8.0
                    :yaw 0.0 :pitch 0.0
                    :lens region3d-component/default-perspective-lens}
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
                             :cone {:inner-deg 18.0 :outer-deg 32.0}})}
     :region/rect {:x 24.0 :y 20.0 :w 80.0 :h 88.0}})))

(defn- region3d-draw-item
  "Region and optional extent/group → reminted region draw item.

   Updates rect before wrapping. Changed extent receives changed identity."
  [region & {:keys [width height group]
                              :or {width 80.0 height 88.0 group 0}}]
  {:region/material
   (region3d-remint
    (assoc region :region/rect {:x 24.0 :y 20.0
                                :w width :h height}))
   :container group})

(defn- region3d-draw-item-size
  "Draw item → width/height pair."
  [draw-item]
  (let [{:keys [w h]} (get-in draw-item [:region/material :region/rect])]
    [w h]))

(defn- ink-record
  "ID, samples [x y pressure] and colour → a validated pen record with the
   harness's width rule, 16 × pressure, as a polyline."
  [id samples color]
  (let [record {:path/material-id id :path/revision ::pending
                :path/tool {:size 16 :fit :polyline :streamline 0}
                :path/source {:kind :pen :samples samples}
                :path/paint {:stroke {:width [:* [:get :size] [:get :p]] :cap :round :join :round :color color}}}
        validated (path-construction/construct record)]
    (assoc validated :path/revision (path-component/component-content-key validated))))

(defn- polygon-record
  "ID, points and colour → a validated filled polygon record."
  [id points color]
  (let [record {:path/material-id id :path/revision ::pending
                :path/source {:kind :anchors :contours [{:closed? true :anchors (mapv (fn [p] {:p p}) points)}]}
                :path/paint {:fill {:rule :nonzero :color color}}}
        validated (path-construction/construct record)]
    (assoc validated :path/revision (path-component/component-content-key validated))))

(defn- region3d-boundary-fixture
  "No arguments → region/draw item with a resolved transformed ink placement
   in child group 17.

   Constructs scene plane and path fixture explicitly. Intended for the
   implemented ink-on-plane boundary; does not exercise text placement
   rendering."
  []
  (let [ink-object
        {:object/id :boundary/ink :object/kind :ink :parent nil
         :transform (region3d-transform
                     [-2.7 -0.95 0.4] [0.018 0.018 0.018]
                     [0.0 -0.21644 0.0 0.976296])
         :provenance {:asserted-by :sid :act :render-harness}
         :ink {:ref {:address :boundary/ink-component}}}
        region (-> (region3d-fixture-region :opaque)
                   (assoc :region3d/version 2)
                   (assoc-in [:scene :boundary/ink] ink-object)
                   region3d-remint)
        ink-component (ink-record
                       :boundary/ink-component
                       [[0.0 8.0 0.45] [54.0 2.0 0.9]
                        [108.0 26.0 0.62] [164.0 8.0 1.0]
                        [222.0 34.0 0.55]]
                       [0.16 0.82 1.0 0.92])
        ink-placement
        {:object-id :boundary/ink :object ink-object :kind :ink
         :address :boundary/ink-component :status :resolved
         :content-revision (path-component/component-content-key ink-component)
         :component ink-component
         :owner {:vi :boundary/ink-owner :draw-item-id :boundary/ink-component}}
        draw-item (assoc (region3d-draw-item region :group 17)
                  :region3d/resolved-placements
                  [ink-placement])]
    {:region (:region/material draw-item) :draw-item draw-item
     :placements [ink-placement]}))

(defn- empty-region3d-system-result
  "No arguments → initial diagnostic accumulator.

   Explicit counters/maps. These counters are harness-owned."
  []
  {:version 1 :prepare-calls 0 :scene-derives 0
   :scene-transform-updates 0 :region-encodes 0 :held-passes 0
   :object-instance-uploads 0 :mesh-vertex-uploads 0
   :light-uploads 0 :uniform-uploads 0 :composite-uploads 0
   :regions {}
   :placements {:version 1 :packs 0 :uploads 0 :draws 0
                :ink-vertices 0 :over-limit 0 :last-coverage-check {}}
   :prepared {}})

(defn- placement-ink-vertices
  "Resolved placements → total curve count of the ink's packed regions.

   Rederives the regions for reporting through the same on-plane route the
   placement renderer packs with; proves derivable geometry size, not the
   rows uploaded."
  [placements]
  (reduce
   + 0
   (keep (fn [placed]
           (when (and (= :resolved (:status placed))
                      (= :ink (:kind placed)))
             (reduce + 0 (map (comp :count :pack) (:regions (on-plane/placed-ink-regions placed))))))
         placements)))

(defn- prepared-summary
  "Draw item → selected mesh/light/shadow/placement summary.

   Counts source fixture facts. Serves as an input summary; not an
   inspection of all GPU prepared state."
  [draw-item]
  (let [region (:region/material draw-item)
        placements (:region3d/resolved-placements draw-item)]
    {:shadow? (boolean
               (some #(and (= :light (:object/kind %))
                           (get-in % [:light :cast-shadow]))
                     (vals (:scene region))))
     :placement-layouts
     (into {}
           (map (fn [placed]
                  [(:object-id placed)
                   {:address (:address placed)
                    :status (:status placed)
                    :layout-id (get-in placed [:layout :layout/id])}]))
           placements)
     :objects (count (:scene region))}))

(defn- record-prepare-return!
  "Harness, input items and renderer prepare result → same result; updates
   diagnostic atom.

   Combines actual returned classifications with inferred upload/placement
   counts. Derived quantities must retain their stated meaning rather than
   be treated as raw call counters."
  [{:keys [!region-result]} draw-items call-return]
  (let [region-returns (:regions call-return)
        changed (filter (comp :changed? val) region-returns)
        placements (vec (mapcat #(or (:region3d/resolved-placements %) []) draw-items))
        statuses (frequencies (map :status placements))
        ink-vertices (placement-ink-vertices placements)
        full-ids (set (keep (fn [[id result]]
                              (when (pos? (:full-rebuilds result)) id))
                            region-returns))
        light-uploads
        (reduce + 0
                (for [draw-item draw-items
                      :let [region (:region/material draw-item)]
                      :when (contains? full-ids (:region/id region))]
                  (count (filter #(= :light (:object/kind %))
                                 (vals (:scene region))))))
        prepared
        (into {}
              (map (fn [draw-item]
                     [(get-in draw-item [:region/material :region/id])
                      (prepared-summary draw-item)]))
              draw-items)]
    (swap! !region-result
           (fn [result]
             (-> result
                 (update :prepare-calls +
                         (if (or (seq changed)
                                 (pos? (:composite-uploads call-return))) 1 0))
                 (update :scene-derives +
                         (reduce + 0 (map (comp :full-rebuilds val) changed)))
                 (update :scene-transform-updates +
                         (count (filter (fn [[_ value]]
                                          (and (zero? (:full-rebuilds value))
                                               (pos? (:instance-uploads value))))
                                        changed)))
                 (update :object-instance-uploads +
                         (reduce + 0 (map (comp :instance-uploads val) changed)))
                 (update :mesh-vertex-uploads +
                         (reduce + 0 (map (comp :full-rebuilds val) changed)))
                 (update :light-uploads + light-uploads)
                 (update :uniform-uploads + (count changed))
                 (update :composite-uploads + (:composite-uploads call-return))
                 (assoc :regions region-returns :prepared prepared)
                 (update :placements
                         (fn [placement-result]
                           (-> placement-result
                               (update :packs +
                                       (if (seq changed) (count placements) 0))
                               (update :uploads +
                                       (if (and (seq changed)
                                                (pos? ink-vertices)) 1 0))
                               (assoc :ink-vertices ink-vertices
                                      :over-limit 0
                                      :last-coverage-check statuses)))))))
    call-return))

(defn- record-pass-returns!
  "Harness and pass results → updates diagnostic pass/draw counters.

   Counts encoded/held roles and encoded interior passes. Placement-draw
   count is an interior-pass proxy."
  [{:keys [!region-result]} passes]
  (swap! !region-result
         (fn [result]
           (-> result
               (update :region-encodes + (count (filter :encoded? passes)))
               (update :held-passes + (count (filter :held? passes)))
               (update-in [:placements :draws] +
                          (count (filter #(and (:encoded? %)
                                              (= :interior (:role %)))
                                         passes))))))
  passes)

(defn- region3d-system-result
  "Harness → accumulated report plus binding statistics.

   Merges harness observations and renderer binding owner state."
  [{:keys [!region-result region-system]}]
  (assoc @!region-result
         :bindings (region-bindings/stats
                    (region3d-renderer/binding-owner region-system))))

(defn- region3d-renderers
  "Harness and sandwich/region/empty mode → ordered pass-painting closures.

   Selects below-path, region composite and above-path order. Intended for
   explicit overlap evidence."
  [{:keys [region-system surround-path-system]} sides]
  (let [surround (fn [placement-id]
                   (let [[first-instance instance-count]
                         (path-renderer/item-range surround-path-system placement-id)]
                     (fn [pass]
                       (path-renderer/draw-path-instances! pass surround-path-system
                                                           first-instance instance-count))))
        region (fn [pass]
                 (region3d-renderer/composite-region! pass region-system
                                                 region3d-id))]
    (case sides
      :sandwich [(surround :region3d/below) region (surround :region3d/above)]
      :region [region]
      :empty [(surround :region3d/below)])))

(defn- region3d-direct-frame!
  "Harness and paint closures → promise of bytes and pass returns; performs
   a GPU frame.

   Reconciles leases, encodes prepared regions, draws linear scene,
   presents/submits, schedules lease/target release and reads back. Serves
   as the real integration driver; a large function owns multiple
   cleanup-sensitive stages."
  [{:keys [device compositor region-system !region-result] :as harness}
   renderers]
  (let [^js device device
        ^js texture (.createTexture
                     device
                     (clj->js {:size {:width canvas-size :height canvas-size
                                      :depthOrArrayLayers 1}
                               :format color-format
                               :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                              js/GPUTextureUsage.COPY_SRC)}))
        owner (region3d-renderer/binding-owner region-system)
        {:keys [active stale]}
        (compositor-gpu/active-region-leases! compositor owner)
        encoder (.createCommandEncoder device)
        prepared (set (keys (:prepared @!region-result)))
        passes (vec (for [{region-id :region/id shadow? :shadow?}
                          (region-bindings/desired-rows owner)
                          :when (contains? prepared region-id)
                          role (if shadow? [:shadow :interior] [:interior])]
                      (region3d-renderer/encode-region-pass!
                       region-system encoder region-id role
                       (get active region-id))))
        _ (record-pass-returns! harness passes)
        scene (compositor-gpu/acquire-target!
               (:target-pool compositor) "rgba16float"
               canvas-size canvas-size "frame/scene-color")
        pass (compositor-gpu/begin-target-pass! encoder scene "clear")]
    (doseq [paint! renderers] (paint! pass))
    (.end pass)
    (compositor-gpu/draw-present! compositor encoder scene
                                  (.createView texture) color-format)
    (.submit (.-queue device) #js [(.finish encoder)])
    (compositor-gpu/release-after-submit! compositor [scene] [] stale)
    (-> (w4-read-texture! device texture canvas-size canvas-size)
        (.then (fn [bytes]
                 (.destroy texture)
                 {:bytes bytes :passes passes})))))

(defn- region3d-prepare-options
  "Harness and session → renderer options including session snapshot
   identity.

   Supplies shared transforms/resources and hashes the session revision.
   Intended for controlled harness invalidation; session hash is a value
   hash, not collision-proof identity."
  [harness session]
  {:zoom 1.0 :dpr 1.0
   :max-lease-size (get-in harness [:compositor :max-lease-size])
   :world-transforms (:world-transforms harness)
   :font-assets (:font-assets harness)
   :session-layout-snapshot
   {:address :region3d/harness-session :revision (hash session)}})

(defn- prepare-region3d!
  "Harness, draw items, session and overrides → prepare result plus recorded
   diagnostics.

   Attaches compositor, merges options, invokes production preparation.
   Serves as a single integration boundary."
  [harness draw-items session prepare-overrides]
  (let [{:keys [compositor region-system]} harness]
    (region3d-renderer/attach-compositor! region-system compositor)
    (record-prepare-return!
     harness draw-items
     (region3d-renderer/prepare-region3d-frame!
      region-system {:regions draw-items} session
      (merge (region3d-prepare-options harness session) prepare-overrides)))))

(defn- region3d-capture!
  "Harness, item, session, sides and optional overrides → promise of one
   frame.

   Prepare then drive."
  ([harness draw-item session sides]
   (region3d-capture! harness draw-item session sides {}))
  ([{:keys [compositor region-system] :as harness}
    draw-item session sides prepare-overrides]
   (prepare-region3d! harness [draw-item] session prepare-overrides)
   (region3d-direct-frame! harness (region3d-renderers harness sides))))

(defn- region3d-capture-pair!
  "Harness, draw item, session, sides and optional preparation overrides →
   promise of first bytes, prepare result, two hashes and equality.

   Prepares once then drives twice. Intended for cached-pass repeatability
   as well as visible determinism."
  ([harness draw-item session sides]
   (region3d-capture-pair! harness draw-item session sides {}))
  ([{:keys [compositor region-system] :as harness}
    draw-item session sides prepare-overrides]
   (let [prepare-return
         (prepare-region3d! harness [draw-item] session prepare-overrides)
         renderers (region3d-renderers harness sides)]
     (.then
      (region3d-direct-frame! harness renderers)
      (fn [{first-bytes :bytes}]
        (.then
         (region3d-direct-frame! harness renderers)
         (fn [{second-bytes :bytes}]
           (.then
            (js/Promise.all
             #js [(sha256-bytes first-bytes)
                  (sha256-bytes second-bytes)])
            (fn [hashes]
              {:bytes first-bytes :prepare prepare-return
               :first-sha256 (aget hashes 0)
               :second-sha256 (aget hashes 1)
               :byte-identical? (= (aget hashes 0)
                                   (aget hashes 1))})))))))))

(defn- region-result
  "Frame → this fixture region's preparation row.

   Fixed-ID projection. Intended for the one-primary-region checks."
  [frame]
  (get-in frame [:prepare :regions region3d-id]))

(defn- region3d-r3!
  "Harness and region → promise of five-frame update classification
   evidence.

   Drives first frame, identical hold, revision remint, object transform and
   zoom; checks rebuild/upload/refit/encode returns. Intended for the
   intended incremental branches; counters are those reported by
   preparation/passes."
  [harness region]
  (let [base-region (-> region
                        (assoc-in [:scene :sun :light :cast-shadow] false)
                        region3d-remint)
        base-draw-item (region3d-draw-item base-region)
        reminted-draw-item
        (assoc-in base-draw-item [:region/material :region/revision]
                  [:reminted (get-in base-draw-item
                                     [:region/material :region/revision])])
        moved-region
        (-> (:region/material reminted-draw-item)
            (assoc-in [:scene :near :transform :translation]
                      [0.125 0.0 0.0])
            region3d-remint)
        moved-draw-item (assoc reminted-draw-item :region/material moved-region)
        drive
        (fn [draw-item zoom]
          (let [prepare (prepare-region3d! harness [draw-item] {} {:zoom zoom})]
            (-> (region3d-direct-frame!
                 harness (region3d-renderers harness :region))
                (.then (fn [{:keys [passes]}]
                         {:prepare prepare :passes passes})))))
        steps
        [(fn [_] (drive base-draw-item 1.0))
         (fn [frame1]
           (.then (drive base-draw-item 1.0)
                  #(hash-map :frame1 frame1 :frame2 %)))
         (fn [state]
           (.then (drive reminted-draw-item 1.0)
                  #(assoc state :frame3 %)))
         (fn [state]
           (.then (drive moved-draw-item 1.0)
                  #(assoc state :frame4 %)))
         (fn [state]
           (.then (drive moved-draw-item 4.0)
                  #(assoc state :frame5 %)))
         (fn [{:keys [frame1 frame2 frame3 frame4 frame5] :as frames}]
           (let [r1 (region-result frame1)
                 r2 (region-result frame2)
                 r3 (region-result frame3)
                 r4 (region-result frame4)
                 r5 (assoc (region-result frame5)
                           :region-encodes
                           (count (filter :encoded? (:passes frame5))))
                 zero-counts {:full-rebuilds 0 :instance-uploads 0
                              :bvh-refits 0 :region-encodes 0}
                 pass? (and (:changed? r1)
                            (= 1 (:full-rebuilds r1))
                            (false? (:changed? r2))
                            (= zero-counts (select-keys r2 (keys zero-counts)))
                            (:changed? r3)
                            (zero? (:full-rebuilds r3))
                            (zero? (:instance-uploads r3))
                            (:changed? r4)
                            (= 1 (:instance-uploads r4))
                            (= 1 (:bvh-refits r4))
                            (zero? (:full-rebuilds r4))
                            (:changed? r5)
                            (= 1 (:region-encodes r5)))]
             (prepare-region3d! harness [] {} {})
             {:frames [(assoc r1 :frame 1)
                       (assoc r2 :frame 2)
                       (assoc r3 :frame 3)
                       (assoc r4 :frame 4)
                       (assoc r5 :frame 5)]
              :pass? pass?}))]]
    (prepare-region3d! harness [] {} {})
    (reduce (fn [promise step] (.then promise step))
            (js/Promise.resolve nil)
            steps)))

(defn- region3d-image-record
  "Case ID and render pair → PNG/hash/determinism record."
  [case-id pair]
  {:mode case-id :file (str "gpu-region3d-floor-" case-id ".png")
   :raw-sha256 (:first-sha256 pair)
   :png-data-url (opaque-png-data-url (:bytes pair))
                 :determinism {:first-raw-sha256 (:first-sha256 pair)
                 :second-raw-sha256 (:second-sha256 pair)
                 :byte-identical? (:byte-identical? pair)}})

(defn- region3d-lights
  "Maintained scene → transformed light records.

   Collects all scene lights and transforms position/direction. Does not
   apply the GPU's sorted eight-light cap."
  [maintained]
  (->> (get-in maintained [:region :scene])
       (keep (fn [[id object]]
               (when (= :light (:object/kind object))
                 (let [matrix (get-in maintained [:world-transforms id])]
                   {:light (:light object)
                    :position (region3d-scene/transform-point matrix
                                                              [0.0 0.0 0.0])
                    :direction (region3d-scene/transform-direction
                                matrix [0.0 0.0 -1.0])}))))
       vec))

(defn- region3d-lit-oracle
  "Region and captured pixels → sampled lighting/depth/transparency/picking
   evidence.

   CPU ray pick/reference shade at the center, plus glass/outside/boundary
   samples and two-byte color tolerance. Intended for a few explicit points;
   not image-wide equivalence."
  [region bytes]
  (let [maintained (assoc (region3d-scene/derive-scene region)
                          :region-id region3d-id)
        camera (region3d-scene/camera-matrices (:view region)
                                                [80.0 88.0])
        hit (region3d-scene/pick-region
             {:maintained maintained :camera camera :region-point [40.0 44.0]})
        object (get-in region [:scene (:object-id hit)])
        linear (region3d-oracle/shade-reference
                {:component (:component object) :normal (:normal hit)
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
  "Harness, region and draw item → promise of
   camera/resize/shadow/close/budget/recreate evidence.

   Sequential captures with queue-completion waits; tests held shadow versus
   waking interior, quantized size, released bytes, rejection visibility and
   compositor recreation. Intended for explicit lifecycle transitions;
   upload deltas use the harness's derived counters."
  [{:keys [device compositor region-system] :as harness} region draw-item]
  (let [base-view (:view region)
        changed-view (assoc base-view :yaw 0.045 :pitch -0.02)
        before-view (region3d-system-result harness)
        wait-for-queue
        (fn [value-fn]
          (.then (.onSubmittedWorkDone (.-queue ^js device))
                 value-fn))
        steps
        [(fn [_]
           (.then
            (region3d-capture!
             harness draw-item {:regions {region3d-id {:view changed-view}}} :region)
            (fn [{view-passes :passes}]
              (let [after-view (region3d-system-result harness)]
                (.then
                 (region3d-capture!
                  harness draw-item {:regions {region3d-id {:view changed-view}}}
                  :region)
                 (fn [{clean-passes :passes}]
                   {:before-view before-view :after-view after-view
                    :view-passes view-passes
                    :clean-passes clean-passes}))))))
         (fn [state]
           (let [resized (region3d-draw-item (:region/material draw-item)
                                      :width 300.0
                                      :height (get-in draw-item [:region/material
                                                          :region/rect :h])
                                      :group (:container draw-item))]
             (.then
              (region3d-capture!
               harness resized {:regions {region3d-id {:view changed-view}}}
               :region)
              (fn [_]
                (wait-for-queue
                 (fn []
                   (assoc state :resize
                          (compositor-gpu/region-leases-stats compositor)
                          :resized-draw-item resized)))))))
         (fn [{:keys [resized-draw-item] :as state}]
           (let [shadow-off
                 (-> (:region/material resized-draw-item)
                     (assoc-in [:scene :sun :light :cast-shadow] false)
                     region3d-remint)
                 shadow-off-draw-item (assoc resized-draw-item :region/material shadow-off)]
             (.then
              (region3d-capture!
               harness shadow-off-draw-item
               {:regions {region3d-id {:view changed-view}}} :region)
              (fn [_]
                (wait-for-queue
                 (fn []
                   (assoc state :shadow-off
                          (compositor-gpu/region-leases-stats compositor))))))))
         (fn [state]
           (prepare-region3d! harness [] {} {})
           (let [frame (region3d-direct-frame!
                        harness (region3d-renderers harness :empty))]
             (.then
              frame
              (fn [_]
                (wait-for-queue
                 (fn []
                   (assoc state :after-close
                          (compositor-gpu/region-leases-stats compositor))))))))
         (fn [state]
           (let [rejection-compositor
                 (compositor-gpu/create-compositor!
                  device color-format
                  :budget-cap-bytes (* 5 1024 1024))
                 rejection-harness (assoc harness :compositor rejection-compositor)]
             (.then
              (region3d-capture! rejection-harness draw-item {} :region)
              (fn [{:keys [bytes]}]
                (wait-for-queue
                 (fn []
                   (let [stats
                         (compositor-gpu/compositor-stats rejection-compositor)
                         sample (pixel-rgba bytes 64 64)
                         next-state
                         (assoc state :rejection
                                {:stats stats :sample sample
                                 :pass? (and (some? (:last-region-rejection stats))
                                             (pos? (apply max sample)))})]
                     (compositor-gpu/destroy-compositor! rejection-compositor)
                     (region3d-renderer/attach-compositor! region-system compositor)
                     (prepare-region3d! harness [] {} {})
                     next-state)))))))
         (fn [state]
           (let [first-compositor
                 (compositor-gpu/create-compositor!
                  device color-format)
                 first-harness (assoc harness :compositor first-compositor)]
             (.then
              (region3d-capture! first-harness draw-item {} :region)
              (fn [_]
                (wait-for-queue
                 (fn []
                   (let [before-destroy
                         (compositor-gpu/region-leases-stats
                          first-compositor)]
                     (compositor-gpu/destroy-compositor! first-compositor)
                     (let [after-destroy
                           (compositor-gpu/region-leases-stats
                            first-compositor)
                           recreated
                           (compositor-gpu/create-compositor!
                            device color-format)
                           recreated-harness (assoc harness
                                                      :compositor recreated)]
                       (.then
                        (region3d-capture! recreated-harness draw-item {} :region)
                        (fn [{:keys [passes]}]
                          (wait-for-queue
                           (fn []
                             (let [after-recreate
                                   (compositor-gpu/region-leases-stats
                                    recreated)
                                   evidence
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
                               (region3d-renderer/attach-compositor!
                                region-system compositor)
                               (prepare-region3d! harness [] {} {})
                               (assoc state :destroy-recreate evidence))))))))))))))
         (fn [{:keys [before-view after-view view-passes clean-passes
                      resize shadow-off after-close rejection destroy-recreate]
               :as evidence}]
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
                            (:pass? rejection)
                            (:pass? destroy-recreate))]
             (-> evidence
                 (dissoc :resized-draw-item)
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
  "Target pool and target → whether that identity remains in a free bucket.

   Inspects private pool state. Precise reserve-preservation evidence
   coupled to internal representation."
  [pool target]
  (let [target-id (:target/id target)]
    (boolean
     (some #(= target-id (:target/id %))
           (mapcat val (:free @(:!state pool)))))))

(defn- region3d-lower-step!
  "Harness and render closures → promise of bytes, pass returns and
   compositor stats.

   Frame plus state snapshot."
  [harness renderers]
  (-> (region3d-direct-frame! harness renderers)
      (.then (fn [{:keys [bytes passes]}]
               {:bytes bytes :passes passes
                :stats (compositor-gpu/compositor-stats
                          (:compositor harness))}))))

(defn- region3d-rejection-leg!
  "Harness, fixture and budget → promise of visible rejection result.

   Creates constrained compositor, captures zoom 8, checks rejection
   metadata/nonzero center sample, destroys compositor. Intended for this
   rejection surface; does not by itself prove the entire rejection image."
  [{:keys [device region-system] :as harness}
   region draw-item budget-cap-bytes]
  (let [compositor (compositor-gpu/create-compositor!
                    device color-format
                    :budget-cap-bytes budget-cap-bytes)
        rejection-harness (assoc harness :compositor compositor)]
    (-> (region3d-capture! rejection-harness draw-item {} :region {:zoom 8.0})
        (.then
         (fn [{:keys [bytes]}]
           (let [stats (compositor-gpu/compositor-stats compositor)
                 result {:bytes bytes
                         :stats stats
                         :sample (pixel-rgba bytes 64 64)
                         :pass? (and (some? (:last-region-rejection stats))
                                     (pos? (apply max (pixel-rgba bytes 64 64))))}]
             (compositor-gpu/destroy-compositor! compositor)
             (prepare-region3d! harness [] {} {})
             result))))))

(defn- region3d-lower-resolution!
  "Harness, region and draw item → promise of pressure, worn-content, hold,
   recovery and floor evidence.

   Reserves a free target, installs a competing lease, drives zoom/content
   changes, removes pressure, compares hashes and checks physical activity,
   picking and wear marker. Intended for a carefully constructed
   resource-policy scenario. Directly mutates desired binding rows and
   depends on detailed pool/accounting representation; extensive sequential
   state makes maintenance harder."
  [{:keys [device region-system compositor] :as harness} region draw-item]
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
                                  :buffer-index 0}}
        prepare-frame
        (fn [current-draw-item zoom]
          (prepare-region3d! lower-harness [current-draw-item] {} {:zoom zoom})
          (region3d-renderers lower-harness :region))
        install-pressure!
        (fn []
          (let [owner (region3d-renderer/binding-owner region-system)
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
            (assoc-in [:scene :near :component :base-color]
                      (region3d-tagged 0.12 0.92 0.28))
            region3d-remint)
        mutated-draw-item (assoc draw-item :region/material mutated-region)
        steps
        [(fn [_]
           (let [frame (prepare-frame draw-item 2.0)]
             (install-pressure!)
             (region3d-lower-step! lower-harness frame)))
         (fn [state]
           (let [frame (prepare-frame draw-item 8.0)]
             (install-pressure!)
             (.then (region3d-lower-step! lower-harness frame)
                    #(assoc state :worn %))))
         (fn [state]
           (let [frame (prepare-frame mutated-draw-item 8.0)]
             (install-pressure!)
             (.then (region3d-lower-step! lower-harness frame)
                    #(assoc state :mutated % :mutated-frame frame))))
         (fn [{:keys [mutated-frame] :as state}]
           (.then (region3d-lower-step! lower-harness mutated-frame)
                  #(assoc state :held %)))
         (fn [state]
           (let [frame (prepare-frame mutated-draw-item 10.0)]
             (install-pressure!)
             (.then (region3d-lower-step! lower-harness frame)
                    #(assoc state :honest-counter %))))
         (fn [state]
           (let [frame (prepare-frame mutated-draw-item 8.0)
                 owner (region3d-renderer/binding-owner region-system)
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
                     region3d-remint)
                 no-shadow-draw-item (assoc draw-item :region/material no-shadow-region)]
             (.then
              (region3d-rejection-leg! harness no-shadow-region no-shadow-draw-item
                                     (* 3 1024 1024))
              #(assoc state :no-shadow-floor %))))
         (fn [state]
           (.then
            (region3d-rejection-leg! harness region draw-item (* 5 1024 1024))
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
                                                 [:stats :region-leases
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
                                     (:view mutated-region)
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
                        (and (= 1 (get-in worn [:stats :lease-activity
                                               :region-binding-updates]))
                             (= 1 (get-in worn [:stats :lease-activity :leases-acquired]))
                             (= 1 (get-in worn [:stats :lease-activity :leases-retired]))
                             (= 1 (get-in worn [:stats :lease-activity :region-rungs-worn])))
                        current-content?
                        (and (> (byte-delta bytes (:bytes mutated)) 2)
                             (every? :encoded? mutated-passes))
                        held-stable?
                        (and (zero? (get-in held [:stats :lease-activity
                                                 :region-binding-updates]))
                             (zero? (get-in held [:stats :lease-activity :leases-acquired]))
                             (zero? (get-in held [:stats :lease-activity :leases-retired]))
                             (every? #(and (:held? %) (not (:encoded? %)))
                                     held-passes))
                        honest-counter?
                        (and (= [512 512]
                                (get-in honest-counter
                                        [:stats :region-leases :leases
                                         [:region3d/harness 512 512] :size]))
                             (zero? (get-in honest-counter
                                            [:stats :lease-activity :region-binding-updates]))
                             (zero? (get-in honest-counter
                                            [:stats :lease-activity :leases-acquired]))
                             (zero? (get-in honest-counter
                                            [:stats :lease-activity :leases-retired])))
                        recovery?
                        (and (= 1 (:rung-divisor recovered-lease))
                             (= [768 768] (:size recovered-lease))
                             (= 1 (get-in recovered
                                          [:stats :lease-activity :region-binding-updates]))
                             (= 1 (get-in recovered [:stats :lease-activity :leases-acquired]))
                             (= 1 (get-in recovered [:stats :lease-activity :leases-retired]))
                             (= 1 (get-in recovered
                                          [:stats :lease-activity :region-rung-recoveries])))
                        floor-identical? (= (aget hashes 2) (aget hashes 3))
                        pick? (and (= :near (:object-id object-pick))
                                   (= :region-background
                                      (:route background-pick)))
                        deterministic? (= (aget hashes 0) (aget hashes 1))
                        worn? (and (= 2 (:rung-divisor worn-lease))
                                   (= [512 512] (:size worn-lease))
                                   (empty? (get-in worn [:stats :pool
                                                         :rejections]))
                                   (nil? (get-in worn [:stats
                                                       :last-region-rejection])))
                        glyph? (and (> (nth wear-sample 1) (nth wear-sample 0))
                                    (> (nth wear-sample 2) (nth wear-sample 0)))
                        pass? (and (not (:rejected? pressure-lease))
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
                                        :lease-activity (get-in sharp [:stats :lease-activity])}
                                :worn {:lease worn-lease :lease-activity (get-in worn [:stats :lease-activity])
                                       :rung-stats
                                       (get-in worn [:stats
                                                     :region-rung-stats])}
                                :current-content? current-content?
                                :wear-sample wear-sample :glyph? glyph?
                                :held-stable? held-stable?
                                :honest-counter? honest-counter?
                                :recovery {:lease recovered-lease
                                           :lease-activity (get-in recovered [:stats :lease-activity])
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
                    (region3d-renderer/attach-compositor! region-system compositor)
                    (prepare-region3d! harness [] {} {})
                    result)))))]]
    (reduce (fn [promise step] (.then promise step))
            (js/Promise.resolve nil)
            steps)))

(defn run-region3d-floor!
  "Device and font assets → promise of four cases plus
   R1/R3/S2/S4/S5/lower-resolution evidence.

   Builds systems and fixtures, runs captures then lifecycle/pressure
   stages, aggregates explicit checks and destroys compositors/render
   systems normally. Intended for the declared rendering floor; S2 and S4
   are aliases of one oracle result, and the externally supplied device plus
   created camera/group buffers are not destroyed here."
  [device font-assets]
  (let [camera (device/create-camera-buffer device)
        groups-buffer (device/create-groups-buffer device)
        registry (transform/add-group
                  (transform/empty-registry) 17
                  {:parent 0 :affine [0.5 0.0 0.0 0.5 40.0 20.0]})
        world-transforms (transform/world-transforms registry)
        _ (device/update-camera device camera (js/Float32Array. 6)
                                  0.0 0.0 1.0 canvas-size canvas-size)
        _ (device/write-groups!
           device groups-buffer world-transforms)
        surround-path-system
        (path-renderer/init-path-system
         device "rgba16float" camera groups-buffer {:zoom 1.0 :pan [0.0 0.0]}
         :initial-capacity 16
         :scene-color (scene-color/scene-color true))
        surround-draw-items
        [(path-draw-item
          :region3d/below
          (polygon-record
           :region3d/below
           [[8.0 8.0] [120.0 8.0] [120.0 120.0] [8.0 120.0]]
           [0.04 0.07 0.15 1.0])
          0)
         (path-draw-item
          :region3d/above
          (polygon-record
           :region3d/above
           [[10.0 58.0] [118.0 58.0] [118.0 70.0] [10.0 70.0]]
           [0.98 0.72 0.12 0.88])
          0)]
        _ (path-renderer/push!
           surround-path-system {:upsert (into {} (map (juxt :id #(dissoc % :id))) surround-draw-items)
                                 :order (mapv :id surround-draw-items) :groups world-transforms})
        region-system (region3d-renderer/ensure-region3d-system!
                       device camera groups-buffer)
        compositor (compositor-gpu/create-compositor!
                    device color-format)
        harness {:device device :camera camera
                 :groups-buffer groups-buffer
                 :world-transforms world-transforms
                 :surround-path-system surround-path-system
                 :region-system region-system
                 :!region-result (atom (empty-region3d-system-result))
                 :font-assets font-assets :compositor compositor}
        opaque-region (region3d-fixture-region :opaque)
        transparent-region (region3d-fixture-region :transparent)
        opaque-draw-item (region3d-draw-item opaque-region)
        transparent-draw-item (region3d-draw-item transparent-region)
        boundary (region3d-boundary-fixture)
        boundary-region (:region boundary)
        boundary-draw-item (:draw-item boundary)
        specs [{:case-id "sandwich" :region opaque-region :draw-item opaque-draw-item
                :session {} :sides :sandwich}
               {:case-id "lit-depth-shadow" :region transparent-region
                :draw-item transparent-draw-item :session {} :sides :region}
               {:case-id "tree" :region boundary-region
                :draw-item boundary-draw-item :session {} :sides :sandwich
                :boundary? true}]
        unknown-group-rejection
        (try
          (prepare-region3d! harness [(assoc opaque-draw-item :container 999)] {} {})
          nil
          (catch :default error
            (ex-data error)))]
    (-> (promise-mapv
         (fn [{:keys [case-id region draw-item session sides boundary?]}]
           (-> (region3d-capture-pair! harness draw-item session sides)
               (.then
                (fn [pair]
                  {:case-id case-id :zoom 1.0
                     :lod :region3d-floor-default
                     :normalization :region-local-3d-inside-world-2d
                     :shape-extent-world (region3d-draw-item-size draw-item)
                     :oracle (when (= case-id "lit-depth-shadow")
                               (region3d-lit-oracle region (:bytes pair)))
                     :boundary-evidence
                     (when boundary?
                       {:resolved (count (filter #(= :resolved (:status %))
                                                 (:region3d/resolved-placements draw-item)))
                        :ink-vertices
                        (placement-ink-vertices
                         (:region3d/resolved-placements draw-item))})
                     :images [(region3d-image-record case-id pair)]}))))
         specs)
        (.then
         (fn [cases]
           (-> (region3d-r3! harness transparent-region)
               (.then
                (fn [r3]
                  (-> (region3d-capture! harness transparent-draw-item {} :region)
                      (.then
                       (fn [_]
                         (-> (region3d-s5-lifecycle!
                              harness transparent-region transparent-draw-item)
                             (.then
                              (fn [s5]
                                (-> (region3d-lower-resolution!
                                     harness transparent-region transparent-draw-item)
                                    (.then
                                     (fn [lower]
                                       {:cases cases :r3 r3
                                        :s5 s5 :lower lower}))))))))))))))
        (.then
         (fn [{:keys [cases r3 s5 lower]}]
           (let [base-cases cases
                 s2 (get-in base-cases [1 :oracle])
                 s4 s2
                 cases (conj base-cases
                             {:case-id "worn" :zoom 8.0
                              :lod :region3d-floor-worn
                              :normalization :region-local-3d-inside-world-2d
                              :shape-extent-world
                              (region3d-draw-item-size transparent-draw-item)
                              :images [(:image lower)]})
                 determinism (mapcat #(map :determinism (:images %)) cases)
                 system-stats (region3d-system-result harness)
                 boundary-evidence (:boundary-evidence (last base-cases))
                 compositor-stats
                 (compositor-gpu/compositor-stats compositor)
                 boundary-pass? (and (= 1 (:resolved boundary-evidence))
                                 (pos? (or (:ink-vertices boundary-evidence) 0)))
                 r1-pass? (= :transform/unknown-group
                             (:error-type unknown-group-rejection))
                 pass? (and (= 4 (count cases))
                            (every? :byte-identical? determinism)
                            r1-pass? (:pass? r3)
                            (:pass? s2) (:pass? s4) (:pass? s5) boundary-pass?
                            (:pass? lower))
                 result {:cases cases
                         :r1 {:unknown-group unknown-group-rejection
                              :pass? r1-pass?}
                         :r3 r3 :s2 s2 :s4 s4 :s5 s5
                         :lower-resolution (dissoc lower :image)
                         :boundary (assoc boundary-evidence :pass? boundary-pass?)
                         :system system-stats
                         :compositor compositor-stats
                         :fixture-query "?region3d=1"
                         :pass? pass?}]
             (compositor-gpu/destroy-compositor! compositor)
             (path-renderer/destroy-path-system! surround-path-system)
             (region3d-renderer/destroy-region3d-system! region-system)
             result))))))
