(ns app.client.verifier.path
     "Browser receipts for path material, tessellation, painting, and frame gates.
      Takes: a WebGPU device.
      Gives: the path verifier result map and Region3D fixture builders.
      Holds nothing."
     (:require [app.client.engine.color :as scene-color]
               [app.client.engine.device :as device]
               [app.client.engine.placement :as containers]
               [app.client.path.material :as path-material]
               [app.client.path.painter :as path-painter]
               [app.client.path.tessellation :as path-tessellation]
               [app.client.verifier.shared
:refer [canvas-size color-format glyph-screen-x glyph-screen-baseline
        glyph-screen-size zoom-cases image-fixtures promise-mapv
        bytes->hex sha256-bytes sha256-string opaque-png-data-url
        q8-effective run-q8-transport! boundary-pixels byte-delta pixel-rgba
        srgb->linear linear->srgb-byte selected-limits adapter-information
        shader-digests w4-read-texture!]]))

(defn- path-paint [color opacity]
  {:color color :opacity opacity
   :color-space :srgb :alpha-association :straight})

(defn- screen-point [zoom [x y]] [(/ x zoom) (/ y zoom)])

(defn- revisioned-path [path]
  (let [validated (path-material/validate-material! path)]
    (assoc validated :path/revision
           (path-material/material-content-key validated))))

(defn- f32-roundtrip [value]
  (let [values (js/Float32Array. 1)]
    (aset values 0 value)
    (aget values 0)))

(defn- quantization-receipt [mesh zoom]
  (let [errors (mapcat (fn [[x y]]
                         [(js/Math.abs (- x (f32-roundtrip x)))
                          (js/Math.abs (- y (f32-roundtrip y)))])
                       (:vertices mesh))
        max-local (if (seq errors) (apply max errors) 0.0)]
    {:regime (:regime/id (path-tessellation/zoom-regime zoom))
     :zoom zoom
     :coordinate-precision :f32
     :max-local-error max-local
     :max-screen-px-error (* zoom max-local)}))

(defn path-ink-material [id zoom samples color opacity]
  (revisioned-path
   {:path/material-id id
    :path/revision ::pending
    :path/kind :ink
    :path/geometry
    {:knots (mapv (fn [index [x y pressure]]
                    {:knot/id [id index]
                     :position (screen-point zoom [x y])
                     :width (* (/ 16.0 zoom) pressure)
                     :pressure pressure})
                  (range) samples)
     :cap :round :join :round}
    :path/paint (path-paint color opacity)}))

(defn- path-shape-material [id zoom color opacity]
  (revisioned-path
   {:path/material-id id
    :path/revision ::pending
    :path/kind :shape
    :path/geometry
    {:contours
     [{:contour/id [id :outer] :role :outer
       :points (mapv (partial screen-point zoom)
                     [[24.0 24.0] [104.0 24.0] [104.0 104.0]
                      [72.0 104.0] [72.0 64.0] [56.0 64.0]
                      [56.0 104.0] [24.0 104.0]])}
      {:contour/id [id :hole] :role :hole
       :points (mapv (partial screen-point zoom)
                     [[34.0 34.0] [50.0 34.0]
                      [50.0 50.0] [34.0 50.0]])}]}
    :path/paint (path-paint color opacity)}))

(defn path-polygon-material [id points color opacity]
  (revisioned-path
   {:path/material-id id
    :path/revision ::pending
    :path/kind :shape
    :path/geometry
    {:contours [{:contour/id [id :outer] :role :outer
                 :points points}]}
    :path/paint (path-paint color opacity)}))

(defn- path-quad-material [id zoom color opacity]
  (path-polygon-material
   id
   (mapv (partial screen-point zoom)
         [[24.0 24.0] [104.0 24.0] [104.0 104.0] [24.0 104.0]])
   color opacity))

(defn path-op [id material container]
  {:id id :path/material material :container container})

(defn- render-path-bytes!
  [^js device path-system ops zoom effective
   & {:keys [clear-value]
      :or {clear-value {:r 0.0 :g 0.0 :b 0.0 :a 0.0}}}]
  (let [row-bytes (* canvas-size 4)
        candidate? (get-in path-system [:scene-color :enabled?])
        view-format (if candidate? "rgba8unorm-srgb" "rgba8unorm")
        target (.createTexture
                device
                (clj->js {:size {:width canvas-size :height canvas-size
                                 :depthOrArrayLayers 1}
                          :format "rgba8unorm"
                          :viewFormats ["rgba8unorm-srgb"]
                          :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                         js/GPUTextureUsage.COPY_SRC)}))
        read-buffer (.createBuffer
                     device
                     (clj->js {:size (* row-bytes canvas-size)
                               :usage (bit-or js/GPUBufferUsage.COPY_DST
                                              js/GPUBufferUsage.MAP_READ)}))
        camera (js/Float32Array. 6)
        _ (device/update-camera device (:camera-buffer path-system)
                                  camera 0.0 0.0 zoom
                                  canvas-size canvas-size)
        {:keys [vertices]}
        (path-painter/prepare-path-frame! path-system ops zoom effective)
        encoder (.createCommandEncoder device)
        pass (.beginRenderPass
              encoder
              (clj->js {:colorAttachments
                        [{:view (.createView target
                                            (clj->js {:format view-format}))
                          :clearValue clear-value
                          :loadOp "clear" :storeOp "store"}]}))]
    (path-painter/draw-path-range! pass path-system 0 vertices)
    (.end pass)
    (.copyTextureToBuffer
     encoder
     (clj->js {:texture target})
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
             copy))))))

(defn- render-path-pair! [device path-system ops zoom effective clear-value]
  (-> (render-path-bytes! device path-system ops zoom effective
                          :clear-value clear-value)
      (.then
       (fn [first-bytes]
         (-> (render-path-bytes! device path-system ops zoom effective
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

(defn- path-image-record [mode case-id pair]
  {:mode mode
   :file (str "gpu-path-" mode "-" case-id ".png")
   :raw-sha256 (:first-sha256 pair)
   :png-data-url (opaque-png-data-url (:bytes pair))
   :determinism {:first-raw-sha256 (:first-sha256 pair)
                 :second-raw-sha256 (:second-sha256 pair)
                 :byte-identical? (:byte-identical? pair)}})

(defn- path-golden-spec [mode]
  (case mode
    :pressure-ink
    {:case-id "pressure-ink-default-min-z0p1"
     :zoom 0.1 :mode "pressure-ink"
     :material (path-ink-material
                :path-golden/pressure 0.1
                [[26.0 72.0 0.2] [48.0 36.0 0.45]
                 [78.0 84.0 0.72] [102.0 42.0 1.0]]
                [0.16 0.68 0.96 0.94] 1.0)}

    :holed-concave
    {:case-id "holed-concave-default-unit-z1"
     :zoom 1.0 :mode "holed-concave"
     :material (path-shape-material :path-golden/shape 1.0
                                    [0.94 0.32 0.18 0.96] 1.0)}

    :translucent-self-crossing
    {:case-id "translucent-self-crossing-legal-z10"
     :zoom 10.0 :mode "translucent-self-crossing"
     :material (path-ink-material
                :path-golden/self-cross 10.0
                [[26.0 28.0 0.65] [102.0 100.0 0.9]
                 [28.0 100.0 1.0] [102.0 28.0 0.7]]
                [0.84 0.36 0.94 0.62] 1.0)}))

(defn- run-path-golden! [device path-system effective mode]
  (let [{:keys [case-id zoom material] :as spec} (path-golden-spec mode)
        clear {:r 0.025 :g 0.06 :b 0.11 :a 1.0}
        op (path-op [:golden mode] material 0)]
    (-> (render-path-pair! device path-system [op] zoom effective clear)
        (.then
         (fn [pair]
           (let [mesh (path-tessellation/tessellate material zoom)]
             {:case-id case-id
              :zoom zoom
              :regime (name (:regime/id (path-tessellation/zoom-regime zoom)))
              :normalization "screen-constant-shape-local"
              :shape-extent-world (/ 80.0 zoom)
              :mesh {:triangles (:triangle-count mesh)
                     :coverage (:coverage mesh)
                     :quantization
                     (quantization-receipt mesh zoom)}
              :images [(path-image-record (:mode spec) case-id pair)]}))))))

(defn- run-path-tree-golden! [device path-system effective]
  (let [case-id "tree-containers-cid17-slot1"
        mode "container-tree"
        zoom 1.0
        clear {:r 0.025 :g 0.06 :b 0.11 :a 1.0}
        material (path-polygon-material
                  :path-golden/container-tree
                  [[8.0 8.0] [40.0 8.0] [40.0 40.0] [8.0 40.0]]
                  [0.18 0.82 0.58 0.96] 1.0)
        ops [(path-op :path-tree/root material 0)
             (path-op :path-tree/child material 17)]
        unknown-error
        (try
          (path-painter/prepare-path-frame!
           path-system [(path-op :path-tree/missing material 99)] zoom effective)
          nil
          (catch :default error (ex-data error)))]
    (-> (render-path-pair! device path-system ops zoom effective clear)
        (.then
         (fn [pair]
           {:case-id case-id
            :zoom zoom
            :regime (name (:regime/id (path-tessellation/zoom-regime zoom)))
            :normalization "path-local-with-effective-container-tree"
            :shape-extent-world 32.0
            :transport-slots [(get-in effective [0 :transport-slot])
                              (get-in effective [17 :transport-slot])]
            :unknown-container unknown-error
            :images [(path-image-record mode case-id pair)]})))))

(defn- path-parity-row!
  [device path-system effective {:keys [case-id zoom regime]}]
  (let [material (path-shape-material [:path-parity case-id] zoom
                                      [1.0 1.0 1.0 1.0] 1.0)
        mesh (path-tessellation/tessellate material zoom)
        op (path-op [:parity case-id] material 0)]
    (-> (render-path-bytes! device path-system [op] zoom effective)
        (.then
         (fn [bytes]
           (let [rows
                 (for [y (range canvas-size)
                       x (range canvas-size)
                       :let [point [(/ (+ x 0.5) zoom)
                                    (/ (+ y 0.5) zoom)]
                             distance-px (* zoom
                                            (path-material/boundary-distance
                                             material point))]
                       :when (> distance-px 1.25)]
                   (let [cpu (path-material/classify material point)
                         alpha (nth (pixel-rgba bytes x y) 3)
                         gpu (cond (> alpha 128) :inside
                                   (< alpha 128) :outside
                                   :else :half)
                         decisive? (and (not= :boundary cpu)
                                        (not= :half gpu))]
                     {:pixel [x y] :cpu cpu :gpu gpu
                      :alpha alpha :decisive? decisive?
                      :match? (when decisive? (= cpu gpu))}))
                 decisive (filter :decisive? rows)
                 mismatches (filter #(false? (:match? %)) decisive)
                 boundary-count
                 (count (for [y (range canvas-size)
                              x (range canvas-size)
                              :let [point [(/ (+ x 0.5) zoom)
                                           (/ (+ y 0.5) zoom)]]
                              :when (<= (* zoom
                                           (path-material/boundary-distance
                                            material point))
                                        1.25)]
                          [x y]))]
             {:case-id case-id :zoom zoom :regime regime
              :quantization
              (quantization-receipt mesh zoom)
              :boundary-band-screen-px 1.25
              :boundary-pixel-count boundary-count
              :decisive-count (count decisive)
              :inside-count (count (filter #(= :inside (:cpu %)) decisive))
              :outside-count (count (filter #(= :outside (:cpu %)) decisive))
              :mismatch-count (count mismatches)
              :mismatch-sample (vec (take 12 mismatches))
              :pass? (and (pos? boundary-count)
                          (pos? (count decisive))
                          (some #(= :inside (:cpu %)) decisive)
                          (some #(= :outside (:cpu %)) decisive)
                          (empty? mismatches))}))))))

(defn- path-color-row! [device path-system effective linear?]
  (let [clear {:r 0.04 :g 0.18 :b 0.35 :a 1.0}
        color [(/ 200.0 255.0) (/ 80.0 255.0) (/ 40.0 255.0) 0.5]
        material (path-quad-material :path-color/source-over 1.0 color 1.0)]
    (-> (render-path-bytes! device path-system
                            [(path-op :path-color material 0)] 1.0 effective
                            :clear-value clear)
        (.then
         (fn [bytes]
           (let [expected
                 (mapv (fn [source background]
                         (if linear?
                           (linear->srgb-byte
                            (+ (* (srgb->linear source) 0.5)
                               (* background 0.5)))
                           (js/Math.round
                            (+ (* source 0.5) (* 255.0 background 0.5)))))
                       [200 80 40] [0.04 0.18 0.35])
                 actual (subvec (vec (pixel-rgba bytes 64 64)) 0 3)
                 delta (apply max (map #(js/Math.abs (- %1 %2))
                                       expected actual))]
             {:mode (if linear? :linear-premultiplied :legacy-direct)
              :expected expected :actual actual
              :max-byte-delta delta
              :non-black-background true
              :pass? (<= delta 3)}))))))

(defn- run-path-color!
  [device path-system camera containers-buffer effective]
  (let [legacy-system
        (path-painter/init-path-system
         device "rgba8unorm" camera containers-buffer
         :scene-color (scene-color/scene-color false))]
    (-> (js/Promise.all
         #js [(path-color-row! device legacy-system effective false)
              (path-color-row! device path-system effective true)])
        (.then
         (fn [rows]
           (let [legacy (aget rows 0)
                 linear (aget rows 1)]
             (path-painter/destroy-path-system! legacy-system)
             {:legacy legacy
              :linear-premultiplied linear
              :pass? (and (:pass? legacy) (:pass? linear))}))))))

(defn- run-path-upload-gate! [path-system effective]
  (let [left (path-polygon-material
              :path-upload/left
              [[3.0 5.0] [19.0 5.0] [19.0 22.0] [3.0 22.0]]
              [0.3 0.7 0.4 1.0] 1.0)
        right (path-polygon-material
               :path-upload/right
               [[52.0 74.0] [91.0 61.0] [104.0 103.0]]
               [0.8 0.2 0.5 1.0] 1.0)
        first-ops [(path-op :path-upload/left left 0)
                   (path-op :path-upload/right right 17)]
        frame-1 (path-painter/prepare-path-frame!
                 path-system first-ops 1.0 effective)
        frame-2 (path-painter/prepare-path-frame!
                 path-system (mapv identity first-ops) 1.0 effective)
        reminted-left (assoc left :path/revision
                             [:path/revision (:path/revision left)])
        reminted-ops [(path-op :path-upload/left reminted-left 0)
                      (second first-ops)]
        frame-3 (path-painter/prepare-path-frame!
                 path-system reminted-ops 1.0 effective)
        frame-4 (path-painter/prepare-path-frame!
                 path-system reminted-ops 10.0 effective)]
    {:frame-1 frame-1
     :frame-2 frame-2
     :frame-3 frame-3
     :frame-4 frame-4
     :last-return frame-4
     :pass? (and (:changed? frame-1)
                 (= 1 (:writes frame-1))
                 (= 2 (:derived frame-1))
                 (not (:changed? frame-2))
                 (zero? (:writes frame-2))
                 (zero? (:derived frame-2))
                 (:changed? frame-3)
                 (= 1 (:writes frame-3))
                 (zero? (:derived frame-3))
                 (:changed? frame-4)
                 (= 1 (:writes frame-4))
                 (= 2 (:derived frame-4)))}))

(defn run-path-atom! [device]
  (let [camera (device/create-camera-buffer device)
        containers-buffer (device/create-containers-buffer device)
        registry (-> (containers/empty-registry)
                     (containers/add-container
                      17 {:parent 0
                          :affine [0.5 0.0 0.0 0.5 40.0 20.0]}))
        effective (containers/effective registry)
        _ (device/write-containers! device containers-buffer effective)
        system (path-painter/init-path-system
                device "rgba8unorm-srgb" camera containers-buffer
                :scene-color (scene-color/scene-color true))]
    (-> (promise-mapv (partial run-path-golden! device system effective)
                      [:holed-concave :translucent-self-crossing])
        (.then
         (fn [cases]
           (-> (run-path-tree-golden! device system effective)
               (.then #(conj cases %)))))
        (.then (fn [cases] {:cases cases}))
        (.then
         (fn [state]
           (-> (promise-mapv (partial path-parity-row!
                                      device system effective)
                             zoom-cases)
               (.then #(assoc state :parity %)))))
        (.then
         (fn [state]
           (-> (run-path-color! device system camera containers-buffer effective)
               (.then #(assoc state :color %)))))
        (.then
         (fn [state]
           (assoc state :upload-gate
                  (run-path-upload-gate! system effective))))
        (.then
         (fn [{:keys [cases parity color upload-gate] :as state}]
           (let [determinism
                 (mapcat (fn [case]
                           (map :determinism (:images case)))
                         cases)
                 pass? (and (= 3 (count cases))
                            (every? :byte-identical? determinism)
                            (= [0 1] (:transport-slots (last cases)))
                            (= :path/unknown-container
                               (get-in (last cases)
                                       [:unknown-container :error-type]))
                            (= 7 (count parity))
                            (every? :pass? parity)
                            (:pass? color)
                            (:pass? upload-gate))
                 result (assoc state
                               :system (:last-return upload-gate)
                               :coverage :aliased-v1
                               :product-pick :cpu-path-authority
                               :self-overlap-alpha
                               :direct-triangle-double-blend-declared
                               :pass? pass?)]
             (path-painter/destroy-path-system! system)
             result))))))
