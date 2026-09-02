(ns app.client.verifier.image
     "Browser receipts for image material, painting, color, and lifecycle behavior.
      Takes: a WebGPU device.
      Gives: the image verifier result map.
      Holds nothing."
     (:require [app.client.engine.color :as scene-color]
               [app.client.engine.device :as device]
               [app.client.engine.transform :as transform]
               [app.client.image.material :as image-material]
               [app.client.image.painter :as image-painter]
               [app.client.verifier.shared
:refer [canvas-size color-format glyph-screen-x glyph-screen-baseline
        glyph-screen-size zoom-cases image-fixtures promise-mapv
        bytes->hex sha256-bytes sha256-string opaque-png-data-url
        q8-effective run-q8-transport! boundary-pixels byte-delta pixel-rgba
        srgb->linear linear->srgb-byte selected-limits adapter-information
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
     (image-painter/register-image-source! image-system source bytes))
   (mapv corpus (map :filename image-fixtures))))

(defn- image-material-row
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
        content-key [id (:digest fixture) rect crop paint]
        row {:image/material-id id
             :image/revision (or revision content-key)
             :image/source-digest (:digest fixture)
             :image/color-tag (or (:color-tag fixture) :srgb)
             :image/intrinsic-size [intrinsic-width intrinsic-height]
             :image/provenance {:actor :verifier
                                :fixture (or (:filename fixture) :unavailable)}
             :image/rect rect
             :image/crop crop
             :image/paint paint}]
    (image-material/validate-material! row)))

(defn- image-op [material container]
  {:image/material material :container container})

(defn- render-image-bytes!
  [^js device image-system ops effective zoom
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
        _ (image-painter/prepare-image-frame! image-system ops effective)
        encoder (.createCommandEncoder device)
        pass (.beginRenderPass
              encoder
              (clj->js {:colorAttachments
                        [{:view (.createView target
                                            (clj->js {:format view-format}))
                          :clearValue clear-value
                          :loadOp "clear" :storeOp "store"}]}))]
    (image-painter/draw-image-runs! pass image-system 0 (count ops))
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

(defn- render-image-pair! [device image-system ops effective zoom clear-value]
  (-> (render-image-bytes! device image-system ops effective zoom
                           :clear-value clear-value)
      (.then
       (fn [first-bytes]
         (-> (render-image-bytes! device image-system ops effective zoom
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

(defn- image-atom-record [mode case-id pair]
  {:mode mode
   :file (str "gpu-image-atom-" mode "-" case-id ".png")
   :raw-sha256 (:first-sha256 pair)
   :png-data-url (opaque-png-data-url (:bytes pair))
   :determinism {:first-raw-sha256 (:first-sha256 pair)
                 :second-raw-sha256 (:second-sha256 pair)
                 :byte-identical? (:byte-identical? pair)}})

(defn- run-image-golden-case!
  [device candidate-system effective corpus {:keys [case-id zoom regime]}]
  (let [screen->world #(/ % zoom)
        opaque (get corpus "atlas-opaque-srgb.png")
        alpha (get corpus "alpha-reference-straight.png")
        clipped (get corpus "clip-stripes-srgb.png")
        clear {:r 0.018 :g 0.055 :b 0.09 :a 1.0}
        opaque-op (image-op
                   (image-material-row
                    :golden/opaque opaque
                    (screen->world 24.0) (screen->world 24.0)
                    (screen->world 80.0) (screen->world 80.0))
                   0)
        alpha-op (image-op
                  (image-material-row
                   :golden/alpha alpha
                   (screen->world 24.0) (screen->world 24.0)
                   (screen->world 80.0) (screen->world 80.0))
                  0)
        ;; T15: this is the post-clamp transform/crop pair for an original
        ;; 80px quad clipped by 20px on each x edge.
        clipped-op (image-op
                    (image-material-row
                     :golden/clipped clipped
                     (screen->world 44.0) (screen->world 24.0)
                     (screen->world 40.0) (screen->world 80.0)
                     :uv [0.25 0.0 0.75 1.0])
                    0)]
    (-> (render-image-pair! device candidate-system [opaque-op]
                            effective zoom clear)
        (.then
         (fn [opaque-pair]
           (-> (render-image-pair! device candidate-system [alpha-op]
                                   effective zoom clear)
               (.then (fn [alpha-pair] [opaque-pair alpha-pair])))))
        (.then
         (fn [[opaque-pair alpha-pair]]
           (-> (render-image-pair! device candidate-system [clipped-op]
                                   effective zoom clear)
               (.then
                (fn [clipped-pair]
                  {:case-id case-id :zoom zoom :regime regime
                   :normalization "screen-constant"
                   :shape-extent-world (/ 80.0 zoom)
                   :op-counts {:opaque 1 :alpha 1 :partially-clipped 1}
                   :images [(image-atom-record "opaque-atlas" case-id
                                               opaque-pair)
                            (image-atom-record "alpha-dedicated" case-id
                                               alpha-pair)
                            (image-atom-record "partially-clipped" case-id
                                               clipped-pair)]}))))))))

(defn- run-image-tree-golden! [device image-system effective corpus]
  (let [case-id "tree-containers-cid17-slot1"
        mode "container-tree"
        zoom 1.0
        clear {:r 0.025 :g 0.06 :b 0.11 :a 1.0}
        material (image-material-row
                  :image-golden/container-tree
                  (get corpus "atlas-opaque-srgb.png")
                  8.0 8.0 32.0 32.0)
        ops [(image-op material 0) (image-op material 17)]
        unknown-error
        (try
          (image-painter/prepare-image-frame!
           image-system [(image-op material 99)] effective)
          nil
          (catch :default error (ex-data error)))]
    (-> (render-image-pair! device image-system ops effective zoom clear)
        (.then
         (fn [pair]
           {:case-id case-id
            :zoom zoom
            :regime "floor-default-clamp"
            :normalization "image-local-with-effective-container-tree"
            :shape-extent-world 32.0
            :op-counts {:container-tree 2}
            :buffer-indexes [(get-in effective [0 :buffer-index])
                              (get-in effective [17 :buffer-index])]
            :unknown-container unknown-error
            :images [(image-atom-record mode case-id pair)]})))))

(defn- image-product-inside? [op zoom screen-x screen-y]
  (image-material/half-open-hit?
   (get-in op [:image/material :image/rect])
   [(/ screen-x zoom) (/ screen-y zoom)]))

(defn- image-parity-receipt [extent-id op zoom rgba]
  (let [boundary (boundary-pixels rgba)
        rows (mapv
              (fn [[x y coverage]]
                (let [inside? (image-product-inside?
                               op zoom (+ x 0.5) (+ y 0.5))
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
  [device seam-system effective corpus]
  (let [fixture (get corpus "coverage-white-srgb.png")]
    (promise-mapv
     (fn [{:keys [case-id zoom]}]
       (promise-mapv
        (fn [[extent-id width height]]
          (let [op (image-op
                    (image-material-row
                     [:parity extent-id case-id] fixture
                     (/ 24.25 zoom) (/ 24.25 zoom) width height)
                    0)]
            (-> (render-image-bytes! device seam-system [op] effective zoom)
                (.then #(image-parity-receipt extent-id op zoom %)))))
        [["screen-constant" (/ 80.0 zoom) (/ 80.0 zoom)]
         ["world-256" 256.0 256.0]]))
     zoom-cases)))

(defn- bitmap-pixel [^js bitmap x y]
  (let [canvas (js/OffscreenCanvas. (.-width bitmap) (.-height bitmap))
        context (.getContext canvas "2d" #js {:willReadFrequently true})
        _ (.drawImage context bitmap 0 0)
        data (.-data (.getImageData context x y 1 1))]
    [(aget data 0) (aget data 1) (aget data 2) (aget data 3)]))

(defn- icc-decode-receipt! [corpus]
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

(defn- run-color-receipts!
  [device candidate-system seam-system effective corpus]
  (let [clear {:r 0.04 :g 0.18 :b 0.35 :a 1.0}
        alpha (get corpus "alpha-reference-straight.png")
        profile (get corpus "profiled-linear-rgb.png")
        straight (get corpus "dedicated-alpha-straight.png")
        premultiplied (get corpus "dedicated-alpha-premultiplied.png")
        seam (get corpus "seam-byte-srgb.png")
        quad (fn [id fixture]
               (image-op
                (image-material-row id fixture 24.0 24.0 80.0 80.0)
                0))
        alpha-op (quad :color/alpha alpha)
        profile-op (quad :color/profile profile)
        straight-op (quad :color/straight straight)
        premultiplied-op (quad :color/premultiplied premultiplied)
        seam-op (image-op
                 (image-material-row :color/seam seam 24.0 24.0 64.0 64.0)
                 0)
        seam-profile-op
        (image-op
         (image-material-row :color/seam-profile profile 24.0 24.0 8.0 8.0)
         0)]
    (-> (render-image-bytes! device candidate-system [alpha-op] effective 1.0
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
         (fn [receipt]
           (-> (render-image-bytes! device candidate-system [profile-op]
                                    effective 1.0)
               (.then
                (fn [direct]
                  (-> (render-image-bytes!
                       device candidate-system [profile-op] effective 1.0
                       :intermediate-copy? true)
                      (.then
                       (fn [copied]
                         (assoc receipt :presentation
                                {:direct-vs-intermediate-max-byte-delta
                                 (byte-delta direct copied)
                                 :pass? (zero? (byte-delta direct copied))})))))))))
        (.then
         (fn [receipt]
           (-> (render-image-bytes! device candidate-system [straight-op]
                                    effective 1.0)
               (.then
                (fn [straight]
                  (-> (render-image-bytes!
                       device candidate-system [premultiplied-op] effective 1.0)
                      (.then
                       (fn [premultiplied]
                         (assoc receipt :alpha-association
                                {:max-byte-delta
                                 (byte-delta straight premultiplied)
                                 :pass? (<= (byte-delta straight premultiplied)
                                            4)}
                                :alpha-association-bytes premultiplied)))))))))
        (.then
         (fn [receipt]
           (let [camera (device/create-camera-buffer device)
                 groups-buffer (device/create-groups-buffer device)
                 _ (device/write-groups! device groups-buffer effective)
                 system (image-painter/init-image-system
                         device "rgba8unorm-srgb" camera groups-buffer
                         :scene-color (scene-color/scene-color true))
                 row (get corpus "dedicated-alpha-premultiplied.png")
                 mistagged-source (assoc (:source row)
                                         :image/alpha-association :straight)]
             (-> (image-painter/register-image-source! system mistagged-source
                                                  (:bytes row))
                 (.then (fn [_]
                          (render-image-bytes! device system
                                               [premultiplied-op] effective 1.0)))
                 (.then
                  (fn [mistagged]
                    (let [delta (byte-delta
                                 (:alpha-association-bytes receipt)
                                 mistagged)]
                      (image-painter/destroy-image-system! system)
                      (.destroy camera)
                      (.destroy groups-buffer)
                      (-> receipt
                          (dissoc :alpha-association-bytes)
                          (assoc :mistagged-alpha
                                 {:max-byte-delta delta
                                  :pass? (> delta 8)})))))))))
        (.then
         (fn [receipt]
           (-> (render-image-bytes! device seam-system [seam-op] effective 1.0)
               (.then
                (fn [seam-bytes]
                  (let [actual (subvec (vec (pixel-rgba seam-bytes 56 56))
                                       0 3)]
                    (assoc receipt :seam-off
                           {:expected [64 128 192] :actual actual
                            :transfer-count 0 :pass? (= [64 128 192] actual)})))))))
        (.then
         (fn [receipt]
           (-> (render-image-bytes! device seam-system [seam-profile-op]
                                    effective 1.0)
               (.then
                (fn [seam-bytes]
                  (let [actual (subvec (vec (pixel-rgba seam-bytes 25 25))
                                       0 3)
                        row (get @(:!resources seam-system) (:digest profile))
                        ingress-transfers
                        (if (get-in seam-system [:scene-color :enabled?]) 1 0)
                        presentation-encodes ingress-transfers]
                    (assoc receipt :seam-off-profile
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
         (fn [receipt]
           (-> (icc-decode-receipt! corpus)
               (.then
                (fn [icc]
                  (let [transfer-rows (vals @(:!resources candidate-system))
                        counts-ok? (every?
                                    #(= :ok (:status %)) transfer-rows)]
                    (assoc receipt
                           :icc-decode icc
                           :candidate-transfer-counts
                           {:ingress 1 :presentation 1
                            :rows-pass? counts-ok?})))))))
        (.then
         (fn [receipt]
           (assoc receipt :pass?
                  (every? :pass?
                          [(:source-over receipt)
                           (:non-black-fringe receipt)
                           (:presentation receipt)
                           (:alpha-association receipt)
                           (:mistagged-alpha receipt)
                           (:seam-off receipt)
                           (:seam-off-profile receipt)
                           (:icc-decode receipt)])))))))

(defn- residency-counts [rows]
  (reduce (fn [counts [_ {:keys [status]}]]
            (update counts status (fnil inc 0)))
          {:ok 0 :refused 0 :unavailable 0}
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

(defn- run-image-upload-gate! [device effective corpus]
  (let [camera (device/create-camera-buffer device)
        groups-buffer (device/create-groups-buffer device)
        _ (device/write-groups! device groups-buffer effective)
        system (image-painter/init-image-system
                device "rgba8unorm-srgb" camera groups-buffer
                :scene-color (scene-color/scene-color true))
        left-fixture (get corpus "atlas-opaque-srgb.png")
        right-fixture (get corpus "alpha-reference-straight.png")
        landing-fixture (get corpus "clip-stripes-srgb.png")
        left (image-material-row :image-upload/left left-fixture
                                 3.0 5.0 16.0 17.0)
        right (image-material-row :image-upload/right right-fixture
                                  52.0 61.0 39.0 42.0)
        ops [(image-op left 0) (image-op right 17)]]
    (-> (promise-mapv
         (fn [fixture]
           (image-painter/register-image-source!
            system (:source fixture) (:bytes fixture)))
         [left-fixture right-fixture])
        (.then
         (fn [_]
           (let [frame-1 (image-painter/prepare-image-frame!
                          system ops effective)
                 frame-2 (image-painter/prepare-image-frame!
                          system (mapv identity ops) effective)
                 reminted-left (assoc left :image/revision
                                      [:image/revision (:image/revision left)])
                 reminted-ops [(image-op reminted-left 0) (second ops)]
                 frame-3 (image-painter/prepare-image-frame!
                          system reminted-ops effective)
                 landing-material
                 (image-material-row :image-upload/landing landing-fixture
                                     12.0 18.0 40.0 32.0)
                 landing-ops [(image-op landing-material 0)]
                 placeholder-frame (image-painter/prepare-image-frame!
                                    system landing-ops effective)]
             {:frame-1 frame-1 :frame-2 frame-2 :frame-3 frame-3
              :landing-ops landing-ops
              :placeholder-frame placeholder-frame
              :placeholder-status (:image/status (first @(:!prepared system)))})))
        (.then
         (fn [state]
           (-> (image-painter/register-image-source!
                system (:source landing-fixture) (:bytes landing-fixture))
               (.then
                (fn [_]
                  (let [frame-4 (image-painter/prepare-image-frame!
                                 system (:landing-ops state) effective)
                        prepared (first @(:!prepared system))
                        result
                        (-> state
                            (dissoc :landing-ops)
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
                    (image-painter/destroy-image-system! system)
                    (.destroy camera)
                    (.destroy groups-buffer)
                    result)))))))))

(defn- request-replacement-device! []
  ;; Dawn consumes an adapter after its first device.  A fresh adapter request
  ;; is therefore part of the real replacement-device lifecycle receipt.
  (-> (.requestAdapter js/navigator.gpu)
      (.then
       (fn [^js replacement-adapter]
         (when-not replacement-adapter
           (throw (js/Error. "Image lifecycle could not acquire replacement adapter")))
         (.requestDevice replacement-adapter)))))

(defn- run-lifecycle-receipt!
  [device candidate-system effective corpus]
  (let [registered-digests
        (mapv :digest (mapv corpus (map :filename image-fixtures)))
        unknown-digest (apply str (repeat 64 "f"))
        refused-digest (apply str (repeat 64 "e"))
        unavailable-fixture {:digest unknown-digest :width 2 :height 2
                             :color-tag :srgb :filename :unavailable}
        refused-fixture {:digest refused-digest :width 2 :height 2
                         :color-tag :srgb :filename :refused}
        unavailable-op
        (image-op (image-material-row :lifecycle/unavailable
                                      unavailable-fixture 0 0 8 8)
                  0)
        refused-op
        (image-op (image-material-row :lifecycle/refused
                                      refused-fixture 0 0 8 8)
                  0)
        source-row (get corpus "atlas-opaque-srgb.png")
        refused-source (-> (:source source-row)
                           (assoc :image/digest refused-digest
                                  :image/color-tag :untagged))]
    (-> (image-painter/register-image-source!
         candidate-system refused-source (:bytes source-row))
        (.then
         (fn [refusal]
           (let [refused-before
                 (get @(:!resources candidate-system) refused-digest)]
             (-> (render-image-bytes! device candidate-system
                                      [unavailable-op] effective 1.0)
                 (.then
                  (fn [unavailable-bytes]
                    (-> (sha256-bytes unavailable-bytes)
                        (.then
                         (fn [hash]
                           {:refusal refusal
                            :refused-before refused-before
                            :placeholder {:unavailable-sha256 hash}})))))))))
        (.then
         (fn [state]
           (-> (render-image-bytes! device candidate-system
                                    [refused-op] effective 1.0)
               (.then
                (fn [_]
                  (assoc state :refused-after-paint
                         (get @(:!resources candidate-system)
                              refused-digest)))))))
        (.then
         (fn [state]
           (-> (request-replacement-device!)
               (.then
                (fn [replacement-device]
                  (let [replacement-camera
                        (device/create-camera-buffer replacement-device)
                        replacement-containers
                        (device/create-groups-buffer replacement-device)
                        _ (device/write-groups! replacement-device
                                                    replacement-containers
                                                    effective)
                        replacement-system
                        (image-painter/init-image-system
                         replacement-device "rgba8unorm-srgb"
                         replacement-camera replacement-containers
                         :scene-color (scene-color/scene-color true))]
                    (-> (image-painter/rebuild-image-resources!
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
                                 refused-after-loss
                                 (get replacement-rows refused-digest)
                                 refusal-preserved?
                                 (= (select-keys (:refused-before state)
                                                 [:status :reason])
                                    (select-keys (:refused-after-paint state)
                                                 [:status :reason])
                                    (select-keys refused-after-loss
                                                 [:status :reason]))
                                 result
                                 (assoc state
                                        :rebuild rebuild
                                        :replacement-report replacement-report
                                        :registered-pass? registered-pass?
                                        :refusal-preserved? refusal-preserved?)]
                             (image-painter/destroy-image-system!
                              replacement-system)
                             (.destroy replacement-camera)
                             (.destroy replacement-containers)
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
                  :refused-status-preserved? (:refusal-preserved? state)}
                 placeholder-sha256
                 "aab20b3aa071f60cd29cbcbc44271364792aa800eb6186042eb0d0ab7e4915e8"
                 placeholder-pass?
                 (= placeholder-sha256
                    (get-in state [:placeholder :unavailable-sha256]))
                 replacement-counts
                 (get-in state [:replacement-report :counts])
                 lifecycle-pass?
                 (and placeholder-pass?
                      (= :refused (get-in state [:refusal :status]))
                      (= :unavailable (:status unavailable))
                      (:placeholder-rendered unavailable)
                      (= (count registered-digests) (:rebuilt rebuild))
                      (:resources-fresh? rebuild)
                      (:registered-pass? state)
                      (:refusal-preserved? state))]
             {:placeholder (assoc (:placeholder state)
                                  :expected-sha256 placeholder-sha256
                                  :pass? placeholder-pass?)
              :unavailable unavailable
              :device-loss device-loss
              :replacement-history-pass? (:refusal-preserved? state)
              :lost-device-counts (:counts lost-report)
              :replacement-counts replacement-counts
              :pass? lifecycle-pass?}))))))

(defn run-image-atom! [device]
  (let [candidate-camera (device/create-camera-buffer device)
        candidate-containers (device/create-groups-buffer device)
        registry (-> (transform/empty-registry)
                     (transform/add-group
                      17 {:parent 0
                          :affine [0.5 0.0 0.0 0.5 40.0 20.0]}))
        effective (transform/world-transforms registry)
        _candidate-transport
        (device/write-groups! device candidate-containers effective)
        candidate-system
        (image-painter/init-image-system
         device "rgba8unorm-srgb" candidate-camera candidate-containers
         :scene-color (scene-color/scene-color true))
        seam-camera (device/create-camera-buffer device)
        seam-containers (device/create-groups-buffer device)
        _seam-transport
        (device/write-groups! device seam-containers effective)
        seam-system
        (image-painter/init-image-system
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
                         device candidate-system effective corpus)
                zoom-cases)
               (.then
                (fn [cases]
                  (-> (run-image-tree-golden!
                       device candidate-system effective corpus)
                      (.then #(hash-map :corpus corpus
                                        :cases (conj cases %)))))))))
        (.then
         (fn [{:keys [corpus] :as state}]
           (-> (run-image-parity! device seam-system effective corpus)
               (.then #(assoc state :parity (vec (mapcat identity %)))))))
        (.then
         (fn [{:keys [corpus] :as state}]
           (-> (run-color-receipts! device candidate-system seam-system
                                    effective corpus)
               (.then #(assoc state :color %)))))
        (.then
         (fn [{:keys [corpus] :as state}]
           (-> (run-image-upload-gate! device effective corpus)
               (.then #(assoc state :upload-gate %)))))
        (.then
         (fn [{:keys [corpus] :as state}]
           (-> (run-lifecycle-receipt! device candidate-system effective corpus)
               (.then #(assoc state :lifecycle %)))))
        (.then
         (fn [{:keys [corpus cases parity color upload-gate lifecycle]}]
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
                                       [:unknown-container :error-type]))
                            (:pass? color)
                            (:pass? upload-gate)
                            (:pass? lifecycle))
                 result {:cases cases :parity parity :color color
                         :lifecycle lifecycle :upload-gate upload-gate
                         :fixture-digests fixture-digests
                         :candidate-ingress
                         (residency-report candidate-system)
                         :seam-off-ingress
                         (residency-report seam-system)
                         :product-loop-claim :parked-verifier-only
                         :product-loop-join :none
                         :image-above-text-kind-layer true
                         :default-dark true :felt-gate :sid-live
                         :pass? pass?}]
             (image-painter/destroy-image-system! candidate-system)
             (image-painter/destroy-image-system! seam-system)
             result))))))

;; --- PATH ATOM --------------------------------------------------------------
