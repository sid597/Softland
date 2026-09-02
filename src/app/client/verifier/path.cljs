(ns app.client.verifier.path
     "Browser receipts for path component, tessellation, painting, and frame gates.
      Takes: a WebGPU device.
      Gives: the path verifier result map and Region3D fixture builders.
      Holds nothing."
     (:require [app.client.engine.color :as scene-color]
               [app.client.engine.device :as device]
               [app.client.engine.transform :as transform]
               [app.client.path.component :as path-component]
               [app.client.path.renderer :as path-renderer]
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
  (let [validated (path-component/validate-component! path)]
    (assoc validated :path/revision
           (path-component/component-content-hash validated))))

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
    {:lod (:lod/id (path-tessellation/zoom-lod zoom))
     :zoom zoom
     :coordinate-precision :f32
     :max-local-error max-local
     :max-screen-px-error (* zoom max-local)}))

(defn path-ink-component [id zoom samples color opacity]
  (revisioned-path
   {:path/material-id id
    :path/revision ::pending
    :path/kind :ink
    :path/geometry
    {:stroke-points (mapv (fn [index [x y pressure]]
                    {:stroke-point/id [id index]
                     :position (screen-point zoom [x y])
                     :width (* (/ 16.0 zoom) pressure)
                     :pressure pressure})
                  (range) samples)
     :cap :round :join :round}
    :path/paint (path-paint color opacity)}))

(defn- path-shape-component [id zoom color opacity]
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

(defn path-polygon-component [id points color opacity]
  (revisioned-path
   {:path/material-id id
    :path/revision ::pending
    :path/kind :shape
    :path/geometry
    {:contours [{:contour/id [id :outer] :role :outer
                 :points points}]}
    :path/paint (path-paint color opacity)}))

(defn- path-quad-component [id zoom color opacity]
  (path-polygon-component
   id
   (mapv (partial screen-point zoom)
         [[24.0 24.0] [104.0 24.0] [104.0 104.0] [24.0 104.0]])
   color opacity))

(defn path-draw-item [id component group]
  {:id id :path/material component :container group})

(defn- render-path-bytes!
  [^js device path-system draw-items zoom world-transforms
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
        (path-renderer/prepare-path-frame! path-system draw-items zoom world-transforms)
        encoder (.createCommandEncoder device)
        pass (.beginRenderPass
              encoder
              (clj->js {:colorAttachments
                        [{:view (.createView target
                                            (clj->js {:format view-format}))
                          :clearValue clear-value
                          :loadOp "clear" :storeOp "store"}]}))]
    (path-renderer/draw-path-range! pass path-system 0 vertices)
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

(defn- render-path-pair! [device path-system draw-items zoom world-transforms clear-value]
  (-> (render-path-bytes! device path-system draw-items zoom world-transforms
                          :clear-value clear-value)
      (.then
       (fn [first-bytes]
         (-> (render-path-bytes! device path-system draw-items zoom world-transforms
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
     :component (path-ink-component
                :path-golden/pressure 0.1
                [[26.0 72.0 0.2] [48.0 36.0 0.45]
                 [78.0 84.0 0.72] [102.0 42.0 1.0]]
                [0.16 0.68 0.96 0.94] 1.0)}

    :holed-concave
    {:case-id "holed-concave-default-unit-z1"
     :zoom 1.0 :mode "holed-concave"
     :component (path-shape-component :path-golden/shape 1.0
                                    [0.94 0.32 0.18 0.96] 1.0)}

    :translucent-self-crossing
    {:case-id "translucent-self-crossing-legal-z10"
     :zoom 10.0 :mode "translucent-self-crossing"
     :component (path-ink-component
                :path-golden/self-cross 10.0
                [[26.0 28.0 0.65] [102.0 100.0 0.9]
                 [28.0 100.0 1.0] [102.0 28.0 0.7]]
                [0.84 0.36 0.94 0.62] 1.0)}))

(defn- run-path-golden! [device path-system world-transforms mode]
  (let [{:keys [case-id zoom component] :as spec} (path-golden-spec mode)
        clear {:r 0.025 :g 0.06 :b 0.11 :a 1.0}
        draw-item (path-draw-item [:golden mode] component 0)]
    (-> (render-path-pair! device path-system [draw-item] zoom world-transforms clear)
        (.then
         (fn [pair]
           (let [mesh (path-tessellation/tessellate component zoom)]
             {:case-id case-id
              :zoom zoom
              :lod (name (:lod/id (path-tessellation/zoom-lod zoom)))
              :normalization "screen-constant-shape-local"
              :shape-extent-world (/ 80.0 zoom)
              :mesh {:triangles (:triangle-count mesh)
                     :coverage (:coverage mesh)
                     :quantization
                     (quantization-receipt mesh zoom)}
              :images [(path-image-record (:mode spec) case-id pair)]}))))))

(defn- run-path-tree-golden! [device path-system world-transforms]
  (let [case-id "tree-containers-cid17-slot1"
        mode "container-tree"
        zoom 1.0
        clear {:r 0.025 :g 0.06 :b 0.11 :a 1.0}
        component (path-polygon-component
                  :path-golden/container-tree
                  [[8.0 8.0] [40.0 8.0] [40.0 40.0] [8.0 40.0]]
                  [0.18 0.82 0.58 0.96] 1.0)
        draw-items [(path-draw-item :path-tree/root component 0)
             (path-draw-item :path-tree/child component 17)]
        unknown-error
        (try
          (path-renderer/prepare-path-frame!
           path-system [(path-draw-item :path-tree/missing component 99)] zoom world-transforms)
          nil
          (catch :default error (ex-data error)))]
    (-> (render-path-pair! device path-system draw-items zoom world-transforms clear)
        (.then
         (fn [pair]
           {:case-id case-id
            :zoom zoom
            :lod (name (:lod/id (path-tessellation/zoom-lod zoom)))
            :normalization "path-local-with-world-transforms-container-tree"
            :shape-extent-world 32.0
            :buffer-indexes [(get-in world-transforms [0 :buffer-index])
                              (get-in world-transforms [17 :buffer-index])]
            :unknown-container unknown-error
            :images [(path-image-record mode case-id pair)]})))))

(defn- path-parity-row!
  [device path-system world-transforms {:keys [case-id zoom lod]}]
  (let [component (path-shape-component [:path-parity case-id] zoom
                                      [1.0 1.0 1.0 1.0] 1.0)
        mesh (path-tessellation/tessellate component zoom)
        draw-item (path-draw-item [:parity case-id] component 0)]
    (-> (render-path-bytes! device path-system [draw-item] zoom world-transforms)
        (.then
         (fn [bytes]
           (let [rows
                 (for [y (range canvas-size)
                       x (range canvas-size)
                       :let [point [(/ (+ x 0.5) zoom)
                                    (/ (+ y 0.5) zoom)]
                             distance-px (* zoom
                                            (path-component/boundary-distance
                                             component point))]
                       :when (> distance-px 1.25)]
                   (let [cpu (path-component/classify component point)
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
                                           (path-component/boundary-distance
                                            component point))
                                        1.25)]
                          [x y]))]
             {:case-id case-id :zoom zoom :lod lod
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

(defn- path-color-row! [device path-system world-transforms linear?]
  (let [clear {:r 0.04 :g 0.18 :b 0.35 :a 1.0}
        color [(/ 200.0 255.0) (/ 80.0 255.0) (/ 40.0 255.0) 0.5]
        component (path-quad-component :path-color/source-over 1.0 color 1.0)]
    (-> (render-path-bytes! device path-system
                            [(path-draw-item :path-color component 0)] 1.0 world-transforms
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
  [device path-system camera groups-buffer world-transforms]
  (let [legacy-system
        (path-renderer/init-path-system
         device "rgba8unorm" camera groups-buffer
         :scene-color (scene-color/scene-color false))]
    (-> (js/Promise.all
         #js [(path-color-row! device legacy-system world-transforms false)
              (path-color-row! device path-system world-transforms true)])
        (.then
         (fn [rows]
           (let [legacy (aget rows 0)
                 linear (aget rows 1)]
             (path-renderer/destroy-path-system! legacy-system)
             {:legacy legacy
              :linear-premultiplied linear
              :pass? (and (:pass? legacy) (:pass? linear))}))))))

(defn- run-path-upload-gate! [path-system world-transforms]
  (let [left (path-polygon-component
              :path-upload/left
              [[3.0 5.0] [19.0 5.0] [19.0 22.0] [3.0 22.0]]
              [0.3 0.7 0.4 1.0] 1.0)
        right (path-polygon-component
               :path-upload/right
               [[52.0 74.0] [91.0 61.0] [104.0 103.0]]
               [0.8 0.2 0.5 1.0] 1.0)
        first-draw-items [(path-draw-item :path-upload/left left 0)
                   (path-draw-item :path-upload/right right 17)]
        frame-1 (path-renderer/prepare-path-frame!
                 path-system first-draw-items 1.0 world-transforms)
        frame-2 (path-renderer/prepare-path-frame!
                 path-system (mapv identity first-draw-items) 1.0 world-transforms)
        reminted-left (assoc left :path/revision
                             [:path/revision (:path/revision left)])
        reminted-draw-items [(path-draw-item :path-upload/left reminted-left 0)
                      (second first-draw-items)]
        frame-3 (path-renderer/prepare-path-frame!
                 path-system reminted-draw-items 1.0 world-transforms)
        frame-4 (path-renderer/prepare-path-frame!
                 path-system reminted-draw-items 10.0 world-transforms)]
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
        groups-buffer (device/create-groups-buffer device)
        registry (-> (transform/empty-registry)
                     (transform/add-group
                      17 {:parent 0
                          :affine [0.5 0.0 0.0 0.5 40.0 20.0]}))
        world-transforms (transform/world-transforms registry)
        _ (device/write-groups! device groups-buffer world-transforms)
        system (path-renderer/init-path-system
                device "rgba8unorm-srgb" camera groups-buffer
                :scene-color (scene-color/scene-color true))]
    (-> (promise-mapv (partial run-path-golden! device system world-transforms)
                      [:holed-concave :translucent-self-crossing])
        (.then
         (fn [cases]
           (-> (run-path-tree-golden! device system world-transforms)
               (.then #(conj cases %)))))
        (.then (fn [cases] {:cases cases}))
        (.then
         (fn [state]
           (-> (promise-mapv (partial path-parity-row!
                                      device system world-transforms)
                             zoom-cases)
               (.then #(assoc state :parity %)))))
        (.then
         (fn [state]
           (-> (run-path-color! device system camera groups-buffer world-transforms)
               (.then #(assoc state :color %)))))
        (.then
         (fn [state]
           (assoc state :upload-gate
                  (run-path-upload-gate! system world-transforms))))
        (.then
         (fn [{:keys [cases parity color upload-gate] :as state}]
           (let [determinism
                 (mapcat (fn [case]
                           (map :determinism (:images case)))
                         cases)
                 pass? (and (= 3 (count cases))
                            (every? :byte-identical? determinism)
                            (= [0 1] (:buffer-indexes (last cases)))
                            (= :transform/unknown-group
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
             (path-renderer/destroy-path-system! system)
             result))))))
