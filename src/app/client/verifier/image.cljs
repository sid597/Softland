(ns app.client.verifier.image
     "Browser receipts for image material, painting, color, and lifecycle behavior.
      Takes: a WebGPU device.
      Gives: the image verifier result map.
      Holds nothing."
     (:require [app.client.engine.color :as scene-color]
               [app.client.engine.device :as device]
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
                              :image/ingress-receipt image-material/ingress-receipt
                              :image/alpha-association
                              (:alpha-association fixture)})))))
       image-fixtures)
      (.then (fn [rows] (into {} (map (juxt :filename identity)) rows)))))

(defn- ingress-corpus! [image-system corpus]
  (promise-mapv
   (fn [{:keys [source bytes]}]
     (image-painter/register-image-source! image-system source bytes))
   (mapv corpus (map :filename image-fixtures))))

(defn- image-op
  [id digest x y width height & {:keys [uv opacity tint]
                                 :or {uv [0.0 0.0 1.0 1.0]
                                      opacity 1.0
                                      tint [1.0 1.0 1.0 1.0]}}]
  {:id id :x x :y y :w width :h height
   :image/digest digest :image/uv uv
   :image/opacity opacity :image/tint tint
   :container-idx 0})

(defn- render-image-bytes!
  [^js device image-system ops zoom
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
        _ (image-painter/prepare-image-frame! image-system ops)
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

(defn- render-image-pair! [device image-system ops zoom clear-value]
  (-> (render-image-bytes! device image-system ops zoom
                           :clear-value clear-value)
      (.then
       (fn [first-bytes]
         (-> (render-image-bytes! device image-system ops zoom
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
  [device candidate-system corpus {:keys [case-id zoom regime]}]
  (let [screen->world #(/ % zoom)
        opaque (:digest (get corpus "atlas-opaque-srgb.png"))
        alpha (:digest (get corpus "alpha-reference-straight.png"))
        clipped (:digest (get corpus "clip-stripes-srgb.png"))
        clear {:r 0.018 :g 0.055 :b 0.09 :a 1.0}
        opaque-op (image-op :golden/opaque opaque
                            (screen->world 24.0) (screen->world 24.0)
                            (screen->world 80.0) (screen->world 80.0))
        alpha-op (image-op :golden/alpha alpha
                           (screen->world 24.0) (screen->world 24.0)
                           (screen->world 80.0) (screen->world 80.0))
        ;; T15: this is the post-clamp placement/crop pair for an original
        ;; 80px quad clipped by 20px on each x edge.
        clipped-op (image-op :golden/clipped clipped
                             (screen->world 44.0) (screen->world 24.0)
                             (screen->world 40.0) (screen->world 80.0)
                             :uv [0.25 0.0 0.75 1.0])]
    (-> (render-image-pair! device candidate-system [opaque-op] zoom clear)
        (.then
         (fn [opaque-pair]
           (-> (render-image-pair! device candidate-system [alpha-op] zoom clear)
               (.then (fn [alpha-pair] [opaque-pair alpha-pair])))))
        (.then
         (fn [[opaque-pair alpha-pair]]
           (-> (render-image-pair! device candidate-system [clipped-op] zoom clear)
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

(defn- image-product-inside? [op zoom screen-x screen-y]
  (image-material/half-open-hit?
   (select-keys op [:x :y :w :h])
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
  [device seam-system corpus]
  (let [digest (:digest (get corpus "coverage-white-srgb.png"))]
    (promise-mapv
     (fn [{:keys [case-id zoom]}]
       (promise-mapv
        (fn [[extent-id width height]]
          (let [op (image-op [:parity extent-id case-id] digest
                             (/ 24.25 zoom) (/ 24.25 zoom) width height)]
            (-> (render-image-bytes! device seam-system [op] zoom)
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
  [device candidate-system seam-system corpus]
  (let [clear {:r 0.04 :g 0.18 :b 0.35 :a 1.0}
        alpha-digest (:digest (get corpus "alpha-reference-straight.png"))
        profile-digest (:digest (get corpus "profiled-linear-rgb.png"))
        straight-digest (:digest (get corpus "dedicated-alpha-straight.png"))
        premultiplied-digest
        (:digest (get corpus "dedicated-alpha-premultiplied.png"))
        seam-digest (:digest (get corpus "seam-byte-srgb.png"))
        quad (fn [id digest]
               (image-op id digest 24.0 24.0 80.0 80.0))
        alpha-op (quad :color/alpha alpha-digest)
        profile-op (quad :color/profile profile-digest)
        straight-op (quad :color/straight straight-digest)
        premultiplied-op (quad :color/premultiplied premultiplied-digest)
        seam-op (image-op :color/seam seam-digest 24.0 24.0 64.0 64.0)
        seam-profile-op (image-op :color/seam-profile profile-digest
                                  24.0 24.0 8.0 8.0)]
    (-> (render-image-bytes! device candidate-system [alpha-op] 1.0
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
           (-> (render-image-bytes! device candidate-system [profile-op] 1.0)
               (.then
                (fn [direct]
                  (-> (render-image-bytes!
                       device candidate-system [profile-op] 1.0
                       :intermediate-copy? true)
                      (.then
                       (fn [copied]
                         (assoc receipt :presentation
                                {:direct-vs-intermediate-max-byte-delta
                                 (byte-delta direct copied)
                                 :pass? (zero? (byte-delta direct copied))})))))))))
        (.then
         (fn [receipt]
           (-> (render-image-bytes! device candidate-system [straight-op] 1.0)
               (.then
                (fn [straight]
                  (-> (render-image-bytes!
                       device candidate-system [premultiplied-op] 1.0)
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
                 containers-buffer (device/create-containers-buffer device)
                 system (image-painter/init-image-system
                         device "rgba8unorm-srgb" camera containers-buffer
                         :scene-color (scene-color/scene-color true))
                 row (get corpus "dedicated-alpha-premultiplied.png")
                 mistagged-source (assoc (:source row)
                                         :image/alpha-association :straight)]
             (-> (image-painter/register-image-source! system mistagged-source
                                                  (:bytes row))
                 (.then (fn [_]
                          (render-image-bytes! device system
                                               [premultiplied-op] 1.0)))
                 (.then
                  (fn [mistagged]
                    (let [delta (byte-delta
                                 (:alpha-association-bytes receipt)
                                 mistagged)]
                      (image-painter/destroy-image-system! system)
                      (-> receipt
                          (dissoc :alpha-association-bytes)
                          (assoc :mistagged-alpha
                                 {:max-byte-delta delta
                                  :pass? (> delta 8)})))))))))
        (.then
         (fn [receipt]
           (-> (render-image-bytes! device seam-system [seam-op] 1.0)
               (.then
                (fn [seam-bytes]
                  (let [actual (subvec (vec (pixel-rgba seam-bytes 56 56))
                                       0 3)]
                    (assoc receipt :seam-off
                           {:expected [64 128 192] :actual actual
                            :transfer-count 0 :pass? (= [64 128 192] actual)})))))))
        (.then
         (fn [receipt]
           (-> (render-image-bytes! device seam-system [seam-profile-op] 1.0)
               (.then
                (fn [seam-bytes]
                  (let [actual (subvec (vec (pixel-rgba seam-bytes 25 25))
                                       0 3)
                        row (get-in (image-painter/image-ingress-receipt seam-system)
                                    [:rows profile-digest])]
                    (assoc receipt :seam-off-profile
                           {:fixture "profiled-linear-rgb.png"
                            :pixel [1 1]
                            ;; The 0.5px ramped hull makes this a pinned
                            ;; encoded-space bilinear sample of the raw
                            ;; no-conversion texels, not the decode probe's
                            ;; exact texel-center value.
                            :expected [51 54 57]
                            :actual actual
                            :ingress-transfers (:ingress-transfers row)
                            :presentation-encodes (:presentation-encodes row)
                            :pass? (and (= [51 54 57] actual)
                                        (zero? (:ingress-transfers row))
                                        (zero? (:presentation-encodes row)))})))))))
        (.then
         (fn [receipt]
           (-> (icc-decode-receipt! corpus)
               (.then
                (fn [icc]
                  (let [rows (:rows (image-painter/image-ingress-receipt
                                     candidate-system))
                        transfer-rows (vals rows)
                        counts-ok? (every?
                                    #(and (= 1 (:ingress-transfers %))
                                          (= 1 (:presentation-encodes %)))
                                    (filter #(= :ok (:status %)) transfer-rows))]
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
  [device candidate-system corpus]
  (let [registered-digests
        (mapv :digest (mapv corpus (map :filename image-fixtures)))
        unknown-digest (apply str (repeat 64 "f"))
        unavailable-op (image-op :lifecycle/unavailable unknown-digest
                                 0 0 8 8)]
    (-> (render-image-bytes! device candidate-system [unavailable-op] 1.0)
        (.then
         (fn [unavailable-bytes]
           (-> (sha256-bytes unavailable-bytes)
               (.then (fn [hash]
                        {:placeholder {:unavailable-sha256 hash}})))))
        (.then
         (fn [receipt]
           (-> (request-replacement-device!)
               (.then
                (fn [replacement-device]
                  (let [replacement-camera
                        (device/create-camera-buffer replacement-device)
                        replacement-containers
                        (device/create-containers-buffer replacement-device)
                        replacement-system
                        (image-painter/init-image-system
                         replacement-device "rgba8unorm-srgb"
                         replacement-camera replacement-containers
                         :scene-color (scene-color/scene-color true))]
                    (-> (image-painter/rebuild-image-resources! candidate-system
                                                          replacement-system)
                        (.then
                         (fn [rebuild]
                           (let [replacement-receipt (:receipt rebuild)
                                 replacement-rows (:rows replacement-receipt)
                                 history-pass?
                                 (every?
                                  (fn [digest]
                                    (let [statuses
                                          (mapv :status
                                                (:history
                                                 (get replacement-rows digest)))]
                                      (and (some #{:device-lost} statuses)
                                           (= :ok (last statuses)))))
                                  registered-digests)
                                 lost-receipt
                                 (image-painter/image-ingress-receipt
                                  candidate-system)
                                 rebuild-receipt
                                 {:replacement-receipt replacement-receipt
                                  :lost-receipt lost-receipt
                                  :history-pass? history-pass?}]
                             (image-painter/destroy-image-system!
                              replacement-system)
                             (doseq [buffer [replacement-camera
                                             replacement-containers]]
                               (.destroy buffer))
                             (.destroy replacement-device)
                             (assoc receipt :rebuild rebuild-receipt)))))))))))
        (.then
         (fn [receipt]
           (let [unavailable (get-in
                              (image-painter/image-ingress-receipt candidate-system)
                              [:rows unknown-digest])
                 replacement-receipt
                 (get-in receipt [:rebuild :replacement-receipt])
                 lost-receipt (get-in receipt [:rebuild :lost-receipt])
                 device-loss (:device-loss replacement-receipt)
                 placeholder-sha256
                 "aab20b3aa071f60cd29cbcbc44271364792aa800eb6186042eb0d0ab7e4915e8"
                 placeholder-pass?
                 (= placeholder-sha256
                    (get-in receipt [:placeholder :unavailable-sha256]))
                 device-loss-pass?
                 (and (= (count registered-digests)
                         (:rebuilt-count device-loss))
                      (:replacement-device? device-loss)
                      (:resource-identities-fresh? device-loss)
                      (= (count registered-digests)
                         (get-in lost-receipt [:counts :device-lost]))
                      (= (count registered-digests)
                         (get-in replacement-receipt [:counts :ok]))
                      (get-in receipt [:rebuild :history-pass?]))]
             {:placeholder (assoc (:placeholder receipt)
                                  :expected-sha256 placeholder-sha256
                                  :pass? placeholder-pass?)
              :unavailable unavailable
              :device-loss device-loss
              :replacement-history-pass?
              (get-in receipt [:rebuild :history-pass?])
              :lost-device-counts (:counts lost-receipt)
              :replacement-counts (:counts replacement-receipt)
              :pass? (and placeholder-pass?
                          (= :unavailable (:status unavailable))
                          (:placeholder-rendered unavailable)
                          device-loss-pass?)}))))))

(defn run-image-atom! [device]
  (let [candidate-camera (device/create-camera-buffer device)
        candidate-containers (device/create-containers-buffer device)
        candidate-system
        (image-painter/init-image-system
         device "rgba8unorm-srgb" candidate-camera candidate-containers
         :scene-color (scene-color/scene-color true))
        seam-camera (device/create-camera-buffer device)
        seam-containers (device/create-containers-buffer device)
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
                         device candidate-system corpus)
                zoom-cases)
               (.then (fn [cases] {:corpus corpus :cases cases})))))
        (.then
         (fn [{:keys [corpus] :as state}]
           (-> (run-image-parity! device seam-system corpus)
               (.then #(assoc state :parity (vec (mapcat identity %)))))))
        (.then
         (fn [{:keys [corpus] :as state}]
           (-> (run-color-receipts! device candidate-system seam-system corpus)
               (.then #(assoc state :color %)))))
        (.then
         (fn [{:keys [corpus] :as state}]
           (-> (run-lifecycle-receipt! device candidate-system corpus)
               (.then #(assoc state :lifecycle %)))))
        (.then
         (fn [{:keys [corpus cases parity color lifecycle]}]
           (let [fixture-digests
                 (into {} (map (fn [[filename row]]
                                 [filename (:digest row)])) corpus)
                 pass? (and (= 21 (reduce + (map #(count (:images %)) cases)))
                            (= 14 (count parity))
                            (every? :pass? parity)
                            (:pass? color)
                            (:pass? lifecycle))
                 result {:cases cases :parity parity :color color
                         :lifecycle lifecycle
                         :fixture-digests fixture-digests
                         :candidate-ingress
                         (image-painter/image-ingress-receipt candidate-system)
                         :seam-off-ingress
                         (image-painter/image-ingress-receipt seam-system)
                         :product-loop-claim :parked-verifier-only
                         :product-loop-join :none
                         :image-above-text-kind-layer true
                         :default-dark true :felt-gate :sid-live
                         :pass? pass?}]
             (image-painter/destroy-image-system! candidate-system)
             (image-painter/destroy-image-system! seam-system)
             result))))))

;; --- PATH ATOM --------------------------------------------------------------
