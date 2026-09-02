(ns app.client.harness.image
     "Browser evidence for image component, rendering, color, and lifecycle behavior.
      Takes: a WebGPU device.
      Gives: the image harness result map.
      Holds nothing."
     (:require [app.client.engine.color :as scene-color]
               [app.client.engine.device :as device]
               [app.client.engine.transform :as transform]
               [app.client.image.component :as image-component]
               [app.client.image.renderer :as image-renderer]
               [app.client.harness.shared
:refer [canvas-size color-format glyph-screen-x glyph-screen-baseline
        glyph-screen-size zoom-cases image-fixtures promise-mapv
        bytes->hex sha256-bytes sha256-string opaque-png-data-url
        q8-world-transforms run-q8-transport! boundary-pixels byte-delta pixel-rgba
        srgb->linear linear->srgb-byte adapter-information
        shader-digests w4-read-texture!]]))

(defn- fetch-image-corpus! []
  (-> (promise-mapv
       (fn [{:keys [filename] :as fixture}]
         (-> (js/fetch (str "/images/" filename))
             (.then (fn [response]
                      (when-not (.-ok response)
                        (throw (js/Error.
                                (str "Image fixture fetch failed: " filename))))
                      (.arrayBuffer response)))
             (.then (fn [bytes]
                      (assoc fixture :bytes bytes
                             :source
                             {:image/digest (:digest fixture)
                              :image/color-tag (:color-tag fixture)
                              :image/width (:width fixture)
                              :image/height (:height fixture)
                              :image/bytes-route
                              {:kind :fixture
                               :path (str "images/" filename)}
                              :image/alpha-association
                              (:alpha-association fixture)})))))
       image-fixtures)
      (.then (fn [rows] (into {} (map (juxt :filename identity)) rows)))))

(defn- ingress-corpus! [image-system corpus]
  (promise-mapv
   (fn [{:keys [source bytes]}]
     (image-renderer/register-image-source! image-system source bytes))
   (mapv corpus (map :filename image-fixtures))))

(defn- image-component-row
  [id fixture x y width height & {:keys [uv opacity tint revision]
                                  :or {uv [0.0 0.0 1.0 1.0]
                                       opacity 1.0
                                       tint [1.0 1.0 1.0 1.0]}}]
  (let [[u0 v0 u1 v1] uv
        intrinsic-width (:width fixture)
        intrinsic-height (:height fixture)
        rect {:x x :y y :w width :h height}
        crop {:x (* u0 intrinsic-width)
              :y (* v0 intrinsic-height)
              :w (* (- u1 u0) intrinsic-width)
              :h (* (- v1 v0) intrinsic-height)}
        paint {:tint {:rgba tint
                      :color-space :srgb
                      :alpha-association :straight}
               :opacity opacity}
        content-hash [id (:digest fixture) rect crop paint]
        row {:image/component-id id
             :image/revision (or revision content-hash)
             :image/source-digest (:digest fixture)
             :image/color-tag (or (:color-tag fixture) :srgb)
             :image/intrinsic-size [intrinsic-width intrinsic-height]
             :image/provenance {:actor :harness
                                :fixture (or (:filename fixture) :unavailable)}
             :image/rect rect
             :image/crop crop
             :image/paint paint}]
    (image-component/validate-component! row)))

(defn- image-draw-item [component group]
  {:image/component component :container group})

(defn- render-image-bytes!
  [^js device image-system draw-items world-transforms zoom
   & {:keys [clear-value intermediate-copy?]
      :or {clear-value {:r 0.0 :g 0.0 :b 0.0 :a 0.0}
           intermediate-copy? false}}]
  (let [row-bytes (* canvas-size 4)
        candidate? (get-in image-system [:scene-color :enabled?])
        view-format (if candidate? "rgba8unorm-srgb" "rgba8unorm")
        texture-options {:size {:width canvas-size :height canvas-size
                                :depthOrArrayLayers 1}
                         :format "rgba8unorm"
                         :viewFormats ["rgba8unorm-srgb"]
                         :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                        js/GPUTextureUsage.COPY_SRC
                                        js/GPUTextureUsage.COPY_DST)}
        target (.createTexture device (clj->js texture-options))
        presentation (when intermediate-copy?
                       (.createTexture device (clj->js texture-options)))
        read-buffer (.createBuffer
                     device
                     (clj->js {:size (* row-bytes canvas-size)
                               :usage (bit-or js/GPUBufferUsage.COPY_DST
                                              js/GPUBufferUsage.MAP_READ)}))
        camera (js/Float32Array. 6)
        _ (device/update-camera device (:camera-buffer image-system)
                                  camera 0.0 0.0 zoom
                                  canvas-size canvas-size)
        _ (image-renderer/prepare-image-frame! image-system draw-items world-transforms)
        encoder (.createCommandEncoder device)
        pass (.beginRenderPass
              encoder
              (clj->js {:colorAttachments
                        [{:view (.createView target
                                            (clj->js {:format view-format}))
                          :clearValue clear-value
                          :loadOp "clear" :storeOp "store"}]}))]
    (image-renderer/draw-image-runs! pass image-system 0 (count draw-items))
    (.end pass)
    (when intermediate-copy?
      (.copyTextureToTexture encoder
                             (clj->js {:texture target})
                             (clj->js {:texture presentation})
                             (clj->js {:width canvas-size
                                       :height canvas-size})))
    (.copyTextureToBuffer
     encoder
     (clj->js {:texture (or presentation target)})
     (clj->js {:buffer read-buffer :bytesPerRow row-bytes
               :rowsPerImage canvas-size})
     (clj->js {:width canvas-size :height canvas-size
               :depthOrArrayLayers 1}))
    (.submit (.-queue device) #js [(.finish encoder)])
    (-> (.mapAsync read-buffer js/GPUMapMode.READ)
        (.then
         (fn [_]
           (let [copy (js/Uint8Array.
                       (js/Uint8Array. (.getMappedRange read-buffer)))]
             (.unmap read-buffer)
             (.destroy read-buffer)
             (.destroy target)
             (when presentation (.destroy presentation))
             copy))))))

(defn- render-image-pair! [device image-system draw-items world-transforms zoom clear-value]
  (-> (render-image-bytes! device image-system draw-items world-transforms zoom
                           :clear-value clear-value)
      (.then
       (fn [first-bytes]
         (-> (render-image-bytes! device image-system draw-items world-transforms zoom
                                  :clear-value clear-value)
             (.then
              (fn [second-bytes]
                (-> (js/Promise.all
                     #js [(sha256-bytes first-bytes)
                          (sha256-bytes second-bytes)])
                    (.then
                     (fn [hashes]
                       {:bytes first-bytes
                        :first-sha256 (aget hashes 0)
                        :second-sha256 (aget hashes 1)
                        :byte-identical? (= (aget hashes 0)
                                            (aget hashes 1))}))))))))))

(defn- image-step-record [mode case-id pair]
  {:mode mode
   :file (str "gpu-image-atom-" mode "-" case-id ".png")
   :raw-sha256 (:first-sha256 pair)
   :png-data-url (opaque-png-data-url (:bytes pair))
   :determinism {:first-raw-sha256 (:first-sha256 pair)
                 :second-raw-sha256 (:second-sha256 pair)
                 :byte-identical? (:byte-identical? pair)}})

(defn- run-image-golden-case!
  [device candidate-system world-transforms corpus {:keys [case-id zoom lod]}]
  (let [screen->world #(/ % zoom)
        opaque (get corpus "atlas-opaque-srgb.png")
        alpha (get corpus "alpha-reference-straight.png")
        clipped (get corpus "clip-stripes-srgb.png")
        clear {:r 0.018 :g 0.055 :b 0.09 :a 1.0}
        opaque-draw-item (image-draw-item
                   (image-component-row
                    :golden/opaque opaque
                    (screen->world 24.0) (screen->world 24.0)
                    (screen->world 80.0) (screen->world 80.0))
                   0)
        alpha-draw-item (image-draw-item
                  (image-component-row
                   :golden/alpha alpha
                   (screen->world 24.0) (screen->world 24.0)
                   (screen->world 80.0) (screen->world 80.0))
                  0)
        ;; T15: this is the post-clamp transform/crop pair for an original
        ;; 80px quad clipped by 20px on each x edge.
        clipped-draw-item (image-draw-item
                    (image-component-row
                     :golden/clipped clipped
                     (screen->world 44.0) (screen->world 24.0)
                     (screen->world 40.0) (screen->world 80.0)
                     :uv [0.25 0.0 0.75 1.0])
                    0)]
    (-> (render-image-pair! device candidate-system [opaque-draw-item]
                            world-transforms zoom clear)
        (.then
         (fn [opaque-pair]
           (-> (render-image-pair! device candidate-system [alpha-draw-item]
                                   world-transforms zoom clear)
               (.then (fn [alpha-pair] [opaque-pair alpha-pair])))))
        (.then
         (fn [[opaque-pair alpha-pair]]
           (-> (render-image-pair! device candidate-system [clipped-draw-item]
                                   world-transforms zoom clear)
               (.then
                (fn [clipped-pair]
                  {:case-id case-id :zoom zoom :lod lod
                   :normalization "screen-constant"
                   :shape-extent-world (/ 80.0 zoom)
                   :draw-item-counts {:opaque 1 :alpha 1 :partially-clipped 1}
                   :images [(image-step-record "opaque-atlas" case-id
                                               opaque-pair)
                            (image-step-record "alpha-dedicated" case-id
                                               alpha-pair)
                            (image-step-record "partially-clipped" case-id
                                               clipped-pair)]}))))))))

(defn- run-image-tree-golden! [device image-system world-transforms corpus]
  (let [case-id "tree-containers-cid17-slot1"
        mode "container-tree"
        zoom 1.0
        clear {:r 0.025 :g 0.06 :b 0.11 :a 1.0}
        component (image-component-row
                  :image-golden/group-tree
                  (get corpus "atlas-opaque-srgb.png")
                  8.0 8.0 32.0 32.0)
        draw-items [(image-draw-item component 0) (image-draw-item component 17)]
        unknown-error
        (try
          (image-renderer/prepare-image-frame!
           image-system [(image-draw-item component 99)] world-transforms)
          nil
          (catch :default error (ex-data error)))]
    (-> (render-image-pair! device image-system draw-items world-transforms zoom clear)
        (.then
         (fn [pair]
           {:case-id case-id
            :zoom zoom
            :lod "engine-default-clamp"
            :normalization "image-local-with-world-transforms-group-tree"
            :shape-extent-world 32.0
            :draw-item-counts {:group-tree 2}
            :buffer-indexes [(get-in world-transforms [0 :buffer-index])
                              (get-in world-transforms [17 :buffer-index])]
            :unknown-group unknown-error
            :images [(image-step-record mode case-id pair)]})))))

(defn- image-product-inside? [draw-item zoom screen-x screen-y]
  (image-component/half-open-hit?
   (get-in draw-item [:image/component :image/rect])
   [(/ screen-x zoom) (/ screen-y zoom)]))

(defn- image-parity-evidence [extent-id draw-item zoom rgba]
  (let [boundary (boundary-pixels rgba)
        rows (mapv
              (fn [[x y coverage]]
                (let [inside? (image-product-inside?
                               draw-item zoom (+ x 0.5) (+ y 0.5))
                      gpu-class (cond (< coverage 128) :outside
                                      (> coverage 128) :inside
                                      :else :half)
                      decisive? (not= :half gpu-class)
                      match? (when decisive?
                               (= inside? (= :inside gpu-class)))]
                  {:pixel [x y] :gpu-coverage-byte coverage
                   :product-inside? inside? :gpu-class gpu-class
                   :decisive? decisive? :match? match?}))
              boundary)
        decisive (filterv :decisive? rows)
        mismatches (filterv #(and (:decisive? %) (not (:match? %))) rows)
        pass? (and (pos? (count rows)) (pos? (count decisive))
                   (empty? mismatches))]
    {:extent extent-id :zoom zoom
     :boundary-pixel-count (count rows)
     :decisive-count (count decisive)
     :half-count (- (count rows) (count decisive))
     :mismatch-count (count mismatches)
     :hit-slop 0.0 :slop-path-ran? false
     :product-route "image half-open quad"
     :candidate-route "production image ramped quad"
     :agreement? pass? :pass? pass?
     :first-mismatches (subvec mismatches 0 (min 16 (count mismatches)))}))

(defn- run-image-parity!
  [device seam-system world-transforms corpus]
  (let [fixture (get corpus "coverage-white-srgb.png")]
    (promise-mapv
     (fn [{:keys [case-id zoom]}]
       (promise-mapv
        (fn [[extent-id width height]]
          (let [draw-item (image-draw-item
                    (image-component-row
                     [:parity extent-id case-id] fixture
                     (/ 24.25 zoom) (/ 24.25 zoom) width height)
                    0)]
            (-> (render-image-bytes! device seam-system [draw-item] world-transforms zoom)
                (.then #(image-parity-evidence extent-id draw-item zoom %)))))
        [["screen-constant" (/ 80.0 zoom) (/ 80.0 zoom)]
         ["world-256" 256.0 256.0]]))
     zoom-cases)))

(defn- bitmap-pixel [^js bitmap x y]
  (let [canvas (js/OffscreenCanvas. (.-width bitmap) (.-height bitmap))
        context (.getContext canvas "2d" #js {:willReadFrequently true})
        _ (.drawImage context bitmap 0 0)
        data (.-data (.getImageData context x y 1 1))]
    [(aget data 0) (aget data 1) (aget data 2) (aget data 3)]))

(defn- icc-decode-evidence! [corpus]
  (let [bytes (:bytes (get corpus "profiled-linear-rgb.png"))
        blob (js/Blob. #js [bytes] #js {:type "image/png"})]
    (-> (js/Promise.all
         #js [(js/createImageBitmap
               blob #js {:colorSpaceConversion "none"
                          :premultiplyAlpha "none"})
              (js/createImageBitmap
               blob #js {:colorSpaceConversion "default"
                          :premultiplyAlpha "none"})])
        (.then
         (fn [bitmaps]
           (let [none (aget bitmaps 0)
                 converted (aget bitmaps 1)
                 rows (mapv
                       (fn [[pixel expected-none expected-converted]]
                         (let [[x y] pixel
                               none-rgba (bitmap-pixel none x y)
                               converted-rgba (bitmap-pixel converted x y)
                               delta (apply max
                                            (map #(js/Math.abs (- %1 %2))
                                                 (take 3 converted-rgba)
                                                 expected-converted))]
                           {:pixel pixel :none none-rgba
                            :default converted-rgba
                            :expected-none expected-none
                            :expected-default expected-converted
                            :pass? (and (= expected-none (take 3 none-rgba))
                                        (<= delta 2))}))
                       [[[1 1] [40 43 46] [110 114 118]]
                        [[4 2] [144 147 150] [198 200 202]]])]
             (.close none)
             (.close converted)
             {:fixture "profiled-linear-rgb.png" :rows rows
              :pass? (every? :pass? rows)}))))))

(defn- run-color-evidence!
  [device candidate-system seam-system world-transforms corpus]
  (let [clear {:r 0.04 :g 0.18 :b 0.35 :a 1.0}
        alpha (get corpus "alpha-reference-straight.png")
        profile (get corpus "profiled-linear-rgb.png")
        straight (get corpus "dedicated-alpha-straight.png")
        premultiplied (get corpus "dedicated-alpha-premultiplied.png")
        seam (get corpus "seam-byte-srgb.png")
        quad (fn [id fixture]
               (image-draw-item
                (image-component-row id fixture 24.0 24.0 80.0 80.0)
                0))
        alpha-draw-item (quad :color/alpha alpha)
        profile-draw-item (quad :color/profile profile)
        straight-draw-item (quad :color/straight straight)
        premultiplied-draw-item (quad :color/premultiplied premultiplied)
        seam-draw-item (image-draw-item
                 (image-component-row :color/seam seam 24.0 24.0 64.0 64.0)
                 0)
        seam-profile-draw-item
        (image-draw-item
         (image-component-row :color/seam-profile profile 24.0 24.0 8.0 8.0)
         0)]
    (-> (render-image-bytes! device candidate-system [alpha-draw-item] world-transforms 1.0
                             :clear-value clear)
        (.then
         (fn [alpha-bytes]
           (let [alpha (/ 128.0 255.0)
                 expected (mapv
                           (fn [source background]
                             (linear->srgb-byte
                              (+ (* (srgb->linear source) alpha)
                                 (* background (- 1.0 alpha)))))
                           [200 80 40] [0.04 0.18 0.35])
                 actual (subvec (vec (pixel-rgba alpha-bytes 64 64)) 0 3)
                 reference-delta (apply max (map #(js/Math.abs (- %1 %2))
                                                 expected actual))
                 edge (pixel-rgba alpha-bytes 23 64)]
             {:source-over {:expected expected :actual actual
                            :max-byte-delta reference-delta
                            :pass? (<= reference-delta 3)}
              :non-black-fringe {:edge-pixel edge
                                 :pass? (every? pos? (take 3 edge))}})))
        (.then
         (fn [evidence]
           (-> (render-image-bytes! device candidate-system [profile-draw-item]
                                    world-transforms 1.0)
               (.then
                (fn [direct]
                  (-> (render-image-bytes!
                       device candidate-system [profile-draw-item] world-transforms 1.0
                       :intermediate-copy? true)
                      (.then
                       (fn [copied]
                         (assoc evidence :presentation
                                {:direct-vs-intermediate-max-byte-delta
                                 (byte-delta direct copied)
                                 :pass? (zero? (byte-delta direct copied))})))))))))
        (.then
         (fn [evidence]
           (-> (render-image-bytes! device candidate-system [straight-draw-item]
                                    world-transforms 1.0)
               (.then
                (fn [straight]
                  (-> (render-image-bytes!
                       device candidate-system [premultiplied-draw-item] world-transforms 1.0)
                      (.then
                       (fn [premultiplied]
                         (assoc evidence :alpha-association
                                {:max-byte-delta
                                 (byte-delta straight premultiplied)
                                 :pass? (<= (byte-delta straight premultiplied)
                                            4)}
                                :alpha-association-bytes premultiplied)))))))))
        (.then
         (fn [evidence]
           (let [camera (device/create-camera-buffer device)
                 groups-buffer (device/create-groups-buffer device)
                 _ (device/write-groups! device groups-buffer world-transforms)
                 system (image-renderer/init-image-system
                         device "rgba8unorm-srgb" camera groups-buffer
                         :scene-color (scene-color/scene-color true))
                 row (get corpus "dedicated-alpha-premultiplied.png")
                 mistagged-source (assoc (:source row)
                                         :image/alpha-association :straight)]
             (-> (image-renderer/register-image-source! system mistagged-source
                                                  (:bytes row))
                 (.then (fn [_]
                          (render-image-bytes! device system
                                               [premultiplied-draw-item] world-transforms 1.0)))
                 (.then
                  (fn [mistagged]
                    (let [delta (byte-delta
                                 (:alpha-association-bytes evidence)
                                 mistagged)]
                      (image-renderer/destroy-image-system! system)
                      (.destroy camera)
                      (.destroy groups-buffer)
                      (-> evidence
                          (dissoc :alpha-association-bytes)
                          (assoc :mistagged-alpha
                                 {:max-byte-delta delta
                                  :pass? (> delta 8)})))))))))
        (.then
         (fn [evidence]
           (-> (render-image-bytes! device seam-system [seam-draw-item] world-transforms 1.0)
               (.then
                (fn [seam-bytes]
                  (let [actual (subvec (vec (pixel-rgba seam-bytes 56 56))
                                       0 3)]
                    (assoc evidence :seam-off
                           {:expected [64 128 192] :actual actual
                            :transfer-count 0 :pass? (= [64 128 192] actual)})))))))
        (.then
         (fn [evidence]
           (-> (render-image-bytes! device seam-system [seam-profile-draw-item]
                                    world-transforms 1.0)
               (.then
                (fn [seam-bytes]
                  (let [actual (subvec (vec (pixel-rgba seam-bytes 25 25))
                                       0 3)
                        row (get @(:!resources seam-system) (:digest profile))
                        ingress-transfers
                        (if (get-in seam-system [:scene-color :enabled?]) 1 0)
                        presentation-encodes ingress-transfers]
                    (assoc evidence :seam-off-profile
                           {:fixture "profiled-linear-rgb.png"
                            :pixel [1 1]
                            ;; The 0.5px ramped hull makes this a pinned
                            ;; encoded-space bilinear sample of the raw
                            ;; no-conversion texels, not the decode probe's
                            ;; exact texel-center value.
                            :expected [51 54 57]
                            :actual actual
                            :ingress-transfers ingress-transfers
                            :presentation-encodes presentation-encodes
                            :pass? (and (= [51 54 57] actual)
                                        (= :ok (:status row))
                                        (zero? ingress-transfers)
                                        (zero? presentation-encodes))})))))))
        (.then
         (fn [evidence]
           (-> (icc-decode-evidence! corpus)
               (.then
                (fn [icc]
                  (let [transfer-rows (vals @(:!resources candidate-system))
                        counts-ok? (every?
                                    #(= :ok (:status %)) transfer-rows)]
                    (assoc evidence
                           :icc-decode icc
                           :candidate-transfer-counts
                           {:ingress 1 :presentation 1
                            :rows-pass? counts-ok?})))))))
        (.then
         (fn [evidence]
           (assoc evidence :pass?
                  (every? :pass?
                          [(:source-over evidence)
                           (:non-black-fringe evidence)
                           (:presentation evidence)
                           (:alpha-association evidence)
                           (:mistagged-alpha evidence)
                           (:seam-off evidence)
                           (:seam-off-profile evidence)
                           (:icc-decode evidence)])))))))

(defn- residency-counts [rows]
  (reduce (fn [counts [_ {:keys [status]}]]
            (update counts status (fnil inc 0)))
          {:ok 0 :rejected 0 :unavailable 0}
          rows))

(defn- residency-report [image-system]
  (let [rows (into {}
                   (map (fn [[digest residency]]
                          [digest
                           (assoc (select-keys residency
                                               [:status :reason :tier :uv
                                                :mip-level-count])
                                  :binding-key
                                  (get-in residency [:binding :key]))]))
                   @(:!resources image-system))]
    {:rows rows
     :counts (residency-counts rows)
     :residency-rev @(:!residency-rev image-system)}))

(defn- run-image-upload-dirty-check! [device world-transforms corpus]
  (let [camera (device/create-camera-buffer device)
        groups-buffer (device/create-groups-buffer device)
        _ (device/write-groups! device groups-buffer world-transforms)
        system (image-renderer/init-image-system
                device "rgba8unorm-srgb" camera groups-buffer
                :scene-color (scene-color/scene-color true))
        left-fixture (get corpus "atlas-opaque-srgb.png")
        right-fixture (get corpus "alpha-reference-straight.png")
        landing-fixture (get corpus "clip-stripes-srgb.png")
        left (image-component-row :image-upload/left left-fixture
                                 3.0 5.0 16.0 17.0)
        right (image-component-row :image-upload/right right-fixture
                                  52.0 61.0 39.0 42.0)
        draw-items [(image-draw-item left 0) (image-draw-item right 17)]]
    (-> (promise-mapv
         (fn [fixture]
           (image-renderer/register-image-source!
            system (:source fixture) (:bytes fixture)))
         [left-fixture right-fixture])
        (.then
         (fn [_]
           (let [frame-1 (image-renderer/prepare-image-frame!
                          system draw-items world-transforms)
                 frame-2 (image-renderer/prepare-image-frame!
                          system (mapv identity draw-items) world-transforms)
                 reminted-left (assoc left :image/revision
                                      [:image/revision (:image/revision left)])
                 reminted-draw-items [(image-draw-item reminted-left 0) (second draw-items)]
                 frame-3 (image-renderer/prepare-image-frame!
                          system reminted-draw-items world-transforms)
                 landing-component
                 (image-component-row :image-upload/landing landing-fixture
                                     12.0 18.0 40.0 32.0)
                 landing-draw-items [(image-draw-item landing-component 0)]
                 placeholder-frame (image-renderer/prepare-image-frame!
                                    system landing-draw-items world-transforms)]
             {:frame-1 frame-1 :frame-2 frame-2 :frame-3 frame-3
              :landing-draw-items landing-draw-items
              :placeholder-frame placeholder-frame
              :placeholder-status (:image/status (first @(:!prepared system)))})))
        (.then
         (fn [state]
           (-> (image-renderer/register-image-source!
                system (:source landing-fixture) (:bytes landing-fixture))
               (.then
                (fn [_]
                  (let [frame-4 (image-renderer/prepare-image-frame!
                                 system (:landing-draw-items state) world-transforms)
                        prepared (first @(:!prepared system))
                        result
                        (-> state
                            (dissoc :landing-draw-items)
                            (assoc :frame-4 frame-4
                                   :pass?
                                   (and (:changed? (:frame-1 state))
                                        (= 2 (:writes (:frame-1 state)))
                                        (not (:changed? (:frame-2 state)))
                                        (zero? (:writes (:frame-2 state)))
                                        (:changed? (:frame-3 state))
                                        ;; A reminted revision changes one
                                        ;; positional pool item even when its
                                        ;; packed image words are unchanged.
                                        (= 1 (:writes (:frame-3 state)))
                                        (:changed? (:placeholder-frame state))
                                        (= :unavailable (:placeholder-status state))
                                        (:changed? frame-4)
                                        ;; The landed texture changes the one
                                        ;; prepared placeholder-backed item.
                                        (= 1 (:writes frame-4))
                                        (= :ok (:image/status prepared))
                                        (not= :image/placeholder
                                              (:image/binding-key prepared)))))]
                    (image-renderer/destroy-image-system! system)
                    (.destroy camera)
                    (.destroy groups-buffer)
                    result)))))))))

(defn- request-replacement-device! []
  ;; Dawn consumes an adapter after its first device.  A fresh adapter request
  ;; is therefore part of the real replacement-device lifecycle evidence.
  (-> (.requestAdapter js/navigator.gpu)
      (.then
       (fn [^js replacement-adapter]
         (when-not replacement-adapter
           (throw (js/Error. "Image lifecycle could not acquire replacement adapter")))
         (.requestDevice replacement-adapter)))))

(defn- run-lifecycle-evidence!
  [device candidate-system world-transforms corpus]
  (let [registered-digests
        (mapv :digest (mapv corpus (map :filename image-fixtures)))
        unknown-digest (apply str (repeat 64 "f"))
        rejected-digest (apply str (repeat 64 "e"))
        unavailable-fixture {:digest unknown-digest :width 2 :height 2
                             :color-tag :srgb :filename :unavailable}
        rejected-fixture {:digest rejected-digest :width 2 :height 2
                         :color-tag :srgb :filename :rejected}
        unavailable-draw-item
        (image-draw-item (image-component-row :lifecycle/unavailable
                                      unavailable-fixture 0 0 8 8)
                  0)
        rejected-draw-item
        (image-draw-item (image-component-row :lifecycle/rejected
                                      rejected-fixture 0 0 8 8)
                  0)
        source-row (get corpus "atlas-opaque-srgb.png")
        rejected-source (-> (:source source-row)
                           (assoc :image/digest rejected-digest
                                  :image/color-tag :untagged))]
    (-> (image-renderer/register-image-source!
         candidate-system rejected-source (:bytes source-row))
        (.then
         (fn [rejection]
           (let [rejected-before
                 (get @(:!resources candidate-system) rejected-digest)]
             (-> (render-image-bytes! device candidate-system
                                      [unavailable-draw-item] world-transforms 1.0)
                 (.then
                  (fn [unavailable-bytes]
                    (-> (sha256-bytes unavailable-bytes)
                        (.then
                         (fn [hash]
                           {:rejection rejection
                            :rejected-before rejected-before
                            :placeholder {:unavailable-sha256 hash}})))))))))
        (.then
         (fn [state]
           (-> (render-image-bytes! device candidate-system
                                    [rejected-draw-item] world-transforms 1.0)
               (.then
                (fn [_]
                  (assoc state :rejected-after-paint
                         (get @(:!resources candidate-system)
                              rejected-digest)))))))
        (.then
         (fn [state]
           (-> (request-replacement-device!)
               (.then
                (fn [replacement-device]
                  (let [replacement-camera
                        (device/create-camera-buffer replacement-device)
                        replacement-groups
                        (device/create-groups-buffer replacement-device)
                        _ (device/write-groups! replacement-device
                                                    replacement-groups
                                                    world-transforms)
                        replacement-system
                        (image-renderer/init-image-system
                         replacement-device "rgba8unorm-srgb"
                         replacement-camera replacement-groups
                         :scene-color (scene-color/scene-color true))]
                    (-> (image-renderer/rebuild-image-resources!
                         candidate-system replacement-system)
                        (.then
                         (fn [rebuild]
                           (let [replacement-report
                                 (residency-report replacement-system)
                                 replacement-rows (:rows replacement-report)
                                 registered-pass?
                                 (every? #(= :ok
                                             (:status (get replacement-rows %)))
                                         registered-digests)
                                 rejected-after-loss
                                 (get replacement-rows rejected-digest)
                                 rejection-preserved?
                                 (= (select-keys (:rejected-before state)
                                                 [:status :reason])
                                    (select-keys (:rejected-after-paint state)
                                                 [:status :reason])
                                    (select-keys rejected-after-loss
                                                 [:status :reason]))
                                 result
                                 (assoc state
                                        :rebuild rebuild
                                        :replacement-report replacement-report
                                        :registered-pass? registered-pass?
                                        :rejection-preserved? rejection-preserved?)]
                             (image-renderer/destroy-image-system!
                              replacement-system)
                             (.destroy replacement-camera)
                             (.destroy replacement-groups)
                             (.destroy replacement-device)
                             result))))))))))
        (.then
         (fn [state]
           (let [lost-report (residency-report candidate-system)
                 unavailable
                 (assoc (get-in lost-report [:rows unknown-digest])
                        :placeholder-rendered true)
                 rebuild (:rebuild state)
                 device-loss
                 {:status :device-lost
                  :rebuilt-count (:rebuilt rebuild)
                  :replacement-device? true
                  :resource-identities-fresh? (:resources-fresh? rebuild)
                  :rejected-status-preserved? (:rejection-preserved? state)}
                 placeholder-sha256
                 "aab20b3aa071f60cd29cbcbc44271364792aa800eb6186042eb0d0ab7e4915e8"
                 placeholder-pass?
                 (= placeholder-sha256
                    (get-in state [:placeholder :unavailable-sha256]))
                 replacement-counts
                 (get-in state [:replacement-report :counts])
                 lifecycle-pass?
                 (and placeholder-pass?
                      (= :rejected (get-in state [:rejection :status]))
                      (= :unavailable (:status unavailable))
                      (:placeholder-rendered unavailable)
                      (= (count registered-digests) (:rebuilt rebuild))
                      (:resources-fresh? rebuild)
                      (:registered-pass? state)
                      (:rejection-preserved? state))]
             {:placeholder (assoc (:placeholder state)
                                  :expected-sha256 placeholder-sha256
                                  :pass? placeholder-pass?)
              :unavailable unavailable
              :device-loss device-loss
              :replacement-history-pass? (:rejection-preserved? state)
              :lost-device-counts (:counts lost-report)
              :replacement-counts replacement-counts
              :pass? lifecycle-pass?}))))))

(defn run-image-step! [device]
  (let [candidate-camera (device/create-camera-buffer device)
        candidate-groups (device/create-groups-buffer device)
        registry (-> (transform/empty-registry)
                     (transform/add-group
                      17 {:parent 0
                          :affine [0.5 0.0 0.0 0.5 40.0 20.0]}))
        world-transforms (transform/world-transforms registry)
        _candidate-transport
        (device/write-groups! device candidate-groups world-transforms)
        candidate-system
        (image-renderer/init-image-system
         device "rgba8unorm-srgb" candidate-camera candidate-groups
         :scene-color (scene-color/scene-color true))
        seam-camera (device/create-camera-buffer device)
        seam-containers (device/create-groups-buffer device)
        _seam-transport
        (device/write-groups! device seam-containers world-transforms)
        seam-system
        (image-renderer/init-image-system
         device "rgba8unorm" seam-camera seam-containers
         :scene-color (scene-color/scene-color false))]
    (-> (fetch-image-corpus!)
        (.then
         (fn [corpus]
           (-> (ingress-corpus! candidate-system corpus)
               (.then (fn [_] (ingress-corpus! seam-system corpus)))
               (.then (fn [_] corpus)))))
        (.then
         (fn [corpus]
           (-> (promise-mapv
                (partial run-image-golden-case!
                         device candidate-system world-transforms corpus)
                zoom-cases)
               (.then
                (fn [cases]
                  (-> (run-image-tree-golden!
                       device candidate-system world-transforms corpus)
                      (.then #(hash-map :corpus corpus
                                        :cases (conj cases %)))))))))
        (.then
         (fn [{:keys [corpus] :as state}]
           (-> (run-image-parity! device seam-system world-transforms corpus)
               (.then #(assoc state :parity (vec (mapcat identity %)))))))
        (.then
         (fn [{:keys [corpus] :as state}]
           (-> (run-color-evidence! device candidate-system seam-system
                                    world-transforms corpus)
               (.then #(assoc state :color %)))))
        (.then
         (fn [{:keys [corpus] :as state}]
           (-> (run-image-upload-dirty-check! device world-transforms corpus)
               (.then #(assoc state :upload-dirty-check %)))))
        (.then
         (fn [{:keys [corpus] :as state}]
           (-> (run-lifecycle-evidence! device candidate-system world-transforms corpus)
               (.then #(assoc state :lifecycle %)))))
        (.then
         (fn [{:keys [corpus cases parity color upload-dirty-check lifecycle]}]
           (let [fixture-digests
                 (into {} (map (fn [[filename row]]
                                 [filename (:digest row)])) corpus)
                 tree-case (last cases)
                 pass? (and (= 22 (reduce + (map #(count (:images %)) cases)))
                            (= 14 (count parity))
                            (every? :pass? parity)
                            (= [0 1] (:buffer-indexes tree-case))
                            (= :transform/unknown-group
                               (get-in tree-case
                                       [:unknown-group :error-type]))
                            (:pass? color)
                            (:pass? upload-dirty-check)
                            (:pass? lifecycle))
                 result {:cases cases :parity parity :color color
                         :lifecycle lifecycle :upload-dirty-check upload-dirty-check
                         :fixture-digests fixture-digests
                         :candidate-ingress
                         (residency-report candidate-system)
                         :seam-off-ingress
                         (residency-report seam-system)
                         :product-loop-claim :parked-harness-only
                         :product-loop-join :none
                         :image-above-text-kind-layer true
                         :default-dark true :felt-gate :sid-live
                         :pass? pass?}]
             (image-renderer/destroy-image-system! candidate-system)
             (image-renderer/destroy-image-system! seam-system)
             result))))))

;; --- PATH STEP --------------------------------------------------------------
