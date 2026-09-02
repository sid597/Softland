(ns app.client.image.renderer
  "The image renderer: quads sampling an atlas or a dedicated texture, with mip
   levels for zooming out.
   Takes: a device, a format, and the shared camera and groups buffers to
   build the system; image sources to register; the frame's image draw-items to
   prepare; a render pass to draw into.
   Gives: an image system with atlas, pipelines, and mip generator; packed
   instances in a pool; draw calls.
   Holds: per-system atoms for prepared state, image sources, atlas placement,
   and resources."
  (:require [app.client.engine.buffer-pool :as buffer-pool]
            [app.client.engine.color :as scene-color]
            [app.client.engine.device :as device]
            [app.client.engine.transform :as transform]
            [app.client.image.frame :as frame]
            [app.client.image.component :as image-component]))

(def image-vertex-shader "
  struct Camera { pan: vec2<f32>, zoom: f32, padding: f32, screen_dimensions: vec2<f32>, };
  @group(0) @binding(2) var<uniform> camera: Camera;
  struct GroupTransform {
    axis_x: vec2<f32>, axis_y: vec2<f32>, translation: vec2<f32>,
    flags: u32, padding: u32,
  };
  @group(0) @binding(3) var<storage, read> groups: array<GroupTransform>;
  struct InstanceInput {
    @location(0) rect: vec4<f32>,
    @location(1) uv_bounds: vec4<f32>,
    @location(2) tint: vec4<f32>,
    @location(3) group_buffer_index: u32,
  };
  struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) uv: vec2<f32>,
    @location(1) tint: vec4<f32>,
    @location(2) edge_pos: vec2<f32>,
    @location(3) edge_size: vec2<f32>,
  };
  @vertex
  fn main(@builtin(vertex_index) v_index: u32, instance: InstanceInput) -> VertexOutput {
    var output: VertexOutput;
    var pos = vec2<f32>(0.0, 0.0);
    switch(v_index) {
      case 0u: { pos = vec2<f32>(0.0, 0.0); } case 1u: { pos = vec2<f32>(1.0, 0.0); }
      case 2u: { pos = vec2<f32>(0.0, 1.0); } case 3u: { pos = vec2<f32>(1.0, 0.0); }
      case 4u: { pos = vec2<f32>(1.0, 1.0); } default: { pos = vec2<f32>(0.0, 1.0); }
    }
    let c = groups[instance.group_buffer_index];
    let is_screen = (c.flags & 1u) != 0u;
    let zm = select(camera.zoom, 1.0, is_screen);
    let pn = select(camera.pan, vec2<f32>(0.0, 0.0), is_screen);
    let axis_scale = max(vec2<f32>(length(c.axis_x), length(c.axis_y)) * zm,
                         vec2<f32>(0.0001, 0.0001));
    // Ramped Cg support: expand the raster hull by half a screen pixel while
    // keeping the mathematical quad at edge_pos 0/edge_size (IMAGE-STEP G4).
    let sign = pos * 2.0 - vec2<f32>(1.0, 1.0);
    let local_delta = sign * vec2<f32>(0.5, 0.5) / axis_scale;
    let local_pos = instance.rect.xy + pos * instance.rect.zw + local_delta;
    let world_pos = c.translation + c.axis_x * local_pos.x + c.axis_y * local_pos.y;
    let panned = world_pos * zm + pn;
    let ndc = (panned / camera.screen_dimensions * 2.0) - vec2<f32>(1.0, 1.0);
    output.position = vec4<f32>(ndc.x, -ndc.y, 0.0, 1.0);
    output.uv = mix(instance.uv_bounds.xy, instance.uv_bounds.zw, pos);
    output.tint = instance.tint;
    output.edge_pos = (pos * instance.rect.zw + local_delta) * axis_scale;
    output.edge_size = instance.rect.zw * axis_scale;
    return output;
  }")

(def ^:private image-fragment-main
  (str "@group(0) @binding(0) var image_sampler: sampler;\n"
       "@group(0) @binding(1) var image_texture: texture_2d<f32>;\n"
       "@fragment\n"
       "fn main(@location(0) uv: vec2<f32>,\n"
       "        @location(1) tint: vec4<f32>,\n"
       "        @location(2) edge_pos: vec2<f32>,\n"
       "        @location(3) edge_size: vec2<f32>) -> @location(0) vec4<f32> {\n"
       "  let edge_distance = min(min(edge_pos.x, edge_size.x - edge_pos.x),\n"
       "                          min(edge_pos.y, edge_size.y - edge_pos.y));\n"
       "  let cg = clamp(edge_distance + 0.5, 0.0, 1.0);\n"
       "  let sampled = textureSample(image_texture, image_sampler, uv);\n"
       "  let t = scene_color(vec4<f32>(tint.rgb, sampled.a * tint.a), cg);\n"
       "  return vec4<f32>(sampled.rgb * t.rgb, t.a);\n"
       "}\n"))

(def image-fragment-shader
  (str device/scene-color-wgsl image-fragment-main))

(def ^:private image-mip-vertex-shader "
  struct Output { @builtin(position) position: vec4<f32>, @location(0) uv: vec2<f32>, };
  @vertex fn main(@builtin(vertex_index) index: u32) -> Output {
    var positions = array<vec2<f32>, 3>(vec2<f32>(-1.0, -1.0),
                                        vec2<f32>(3.0, -1.0),
                                        vec2<f32>(-1.0, 3.0));
    var out: Output;
    out.position = vec4<f32>(positions[index], 0.0, 1.0);
    out.uv = positions[index] * vec2<f32>(0.5, -0.5) + vec2<f32>(0.5, 0.5);
    return out;
  }")

(def ^:private image-mip-fragment-shader "
  @group(0) @binding(0) var mip_sampler: sampler;
  @group(0) @binding(1) var mip_source: texture_2d<f32>;
  @fragment fn main(@location(0) uv: vec2<f32>) -> @location(0) vec4<f32> {
    return textureSample(mip_source, mip_sampler, uv);
  }")

;; --- 2. INITIALIZATION ---

(def image-instance-stride image-component/image-instance-stride)

;; --- Image step resource system --------------------------------------------

(defn- pack-image-instance [image-draw-item]
  (let [component (:image/component image-draw-item)
        paint (:image/paint component)
        words (image-component/instance-words
               {:rect (:image/rect component)
                :uv (:image/resolved-uv image-draw-item)
                :tint (:tint paint)
                :opacity (:opacity paint)
                :buffer-index (:buffer-index image-draw-item)})
        data (js/Float32Array. image-component/image-instance-words)
        uints (js/Uint32Array. (.-buffer data))]
    (dotimes [index 12]
      (aset data index (nth words index)))
    (aset uints 12 (nth words 12))
    data))

(defn- create-image-bind-group
  [^js device layout sampler texture-view camera-buffer groups-buffer]
  (.createBindGroup
   device
   (clj->js {:layout layout
             :entries [{:binding 0 :resource sampler}
                       {:binding 1 :resource texture-view}
                       {:binding 2 :resource {:buffer camera-buffer}}
                       {:binding 3 :resource {:buffer groups-buffer}}]})))

(defn- create-image-pipeline
  [^js device fformat bind-layout scene-color]
  (let [vertex-module (.createShaderModule device
                                          (clj->js {:code image-vertex-shader}))
        fragment-module (.createShaderModule
                         device
                         (clj->js {:code (device/configure-scene-color-shader
                                         image-fragment-shader scene-color)}))
        layout (.createPipelineLayout
                device (clj->js {:bindGroupLayouts [bind-layout]}))]
    (.createRenderPipeline
     device
     (clj->js
      {:layout layout
       :vertex {:module vertex-module
                :entryPoint "main"
                :buffers [{:arrayStride image-instance-stride
                           :stepMode "instance"
                           :attributes [{:shaderLocation 0 :offset 0
                                         :format "float32x4"}
                                        {:shaderLocation 1 :offset 16
                                         :format "float32x4"}
                                        {:shaderLocation 2 :offset 32
                                         :format "float32x4"}
                                        {:shaderLocation 3 :offset 48
                                         :format "uint32"}]}]}
       :fragment {:module fragment-module
                  :entryPoint "main"
                  :targets [{:format fformat
                             :blend (device/scene-color-blend scene-color)}]}
       :primitive {:topology "triangle-list"}}))))

(defn- create-image-mip-system [^js device scene-color]
  (let [mip-format (if (:enabled? scene-color)
                     "rgba8unorm-srgb"
                     "rgba8unorm")
        bind-layout (.createBindGroupLayout
                     device
                     (clj->js
                      {:entries [{:binding 0
                                  :visibility js/GPUShaderStage.FRAGMENT
                                  :sampler {:type "filtering"}}
                                 {:binding 1
                                  :visibility js/GPUShaderStage.FRAGMENT
                                  :texture {:sampleType "float"}}]}))
        layout (.createPipelineLayout
                device (clj->js {:bindGroupLayouts [bind-layout]}))
        vertex-module (.createShaderModule
                       device (clj->js {:code image-mip-vertex-shader}))
        fragment-module (.createShaderModule
                         device (clj->js {:code image-mip-fragment-shader}))
        ;; IMAGE-STEP T4: candidate mip views are sRGB on both sides, so
        ;; filtering occurs between hardware decode and encode.  Seam-off uses
        ;; unorm on both sides so its declared zero-transfer leg cannot
        ;; accidentally half-convert while generating mips.
        pipeline (.createRenderPipeline
                  device
                  (clj->js
                   {:layout layout
                    :vertex {:module vertex-module :entryPoint "main"}
                    :fragment {:module fragment-module :entryPoint "main"
                               :targets [{:format mip-format}]}
                    :primitive {:topology "triangle-list"}}))
        sampler (.createSampler device
                                (clj->js {:minFilter "linear"
                                          :magFilter "linear"
                                          :mipmapFilter "linear"
                                          :addressModeU "clamp-to-edge"
                                          :addressModeV "clamp-to-edge"}))]
    {:bind-layout bind-layout :pipeline pipeline :sampler sampler
     :format mip-format}))

(defn- generate-image-mips!
  [^js device mip-system ^js texture mip-level-count]
  (when (> mip-level-count 1)
    (let [encoder (.createCommandEncoder device)]
      (doseq [level (range 1 mip-level-count)]
        (let [source-view (.createView
                           texture
                           (clj->js {:format (:format mip-system)
                                     :baseMipLevel (dec level)
                                     :mipLevelCount 1}))
              target-view (.createView
                           texture
                           (clj->js {:format (:format mip-system)
                                     :baseMipLevel level
                                     :mipLevelCount 1}))
              bind-group (.createBindGroup
                          device
                          (clj->js
                           {:layout (:bind-layout mip-system)
                            :entries [{:binding 0
                                       :resource (:sampler mip-system)}
                                      {:binding 1 :resource source-view}]}))
              pass (.beginRenderPass
                    encoder
                    (clj->js {:colorAttachments
                              [{:view target-view
                                :clearValue {:r 0 :g 0 :b 0 :a 0}
                                :loadOp "clear" :storeOp "store"}]}))]
          (.setPipeline pass (:pipeline mip-system))
          (.setBindGroup pass 0 bind-group)
          (.draw pass 3 1 0 0)
          (.end pass)))
      (.submit (.-queue device) #js [(.finish encoder)]))))

(defn- image-texture
  [^js device width height mip-level-count]
  (.createTexture
   device
   (clj->js {:size {:width width :height height :depthOrArrayLayers 1}
             :mipLevelCount mip-level-count
             :format "rgba8unorm"
             :viewFormats ["rgba8unorm-srgb"]
             :usage (bit-or js/GPUTextureUsage.TEXTURE_BINDING
                            js/GPUTextureUsage.COPY_DST
                            js/GPUTextureUsage.RENDER_ATTACHMENT)})))

(defn- image-view [^js texture scene-color]
  (.createView texture
               (clj->js {:format (if (:enabled? scene-color)
                                   "rgba8unorm-srgb"
                                   "rgba8unorm")})))

(defn- padded-image-canvas [^js bitmap padding]
  (let [width (.-width bitmap)
        height (.-height bitmap)
        canvas (js/OffscreenCanvas. (+ width (* 2 padding))
                                    (+ height (* 2 padding)))
        context (.getContext canvas "2d")]
    (.clearRect context 0 0 (.-width canvas) (.-height canvas))
    (.drawImage context bitmap padding padding)
    ;; IMAGE-STEP T5: extrude edge texels through the declared gutter before
    ;; the atlas mip chain is generated; UVs still address only the interior.
    (.drawImage context bitmap 0 0 1 height 0 padding padding height)
    (.drawImage context bitmap (dec width) 0 1 height (+ padding width) padding
                padding height)
    (.drawImage context bitmap 0 0 width 1 padding 0 width padding)
    (.drawImage context bitmap 0 (dec height) width 1 padding (+ padding height)
                width padding)
    (.drawImage context bitmap 0 0 1 1 0 0 padding padding)
    (.drawImage context bitmap (dec width) 0 1 1 (+ padding width) 0
                padding padding)
    (.drawImage context bitmap 0 (dec height) 1 1 0 (+ padding height)
                padding padding)
    (.drawImage context bitmap (dec width) (dec height) 1 1
                (+ padding width) (+ padding height) padding padding)
    canvas))

(defn- bytes->sha256 [bytes]
  (-> (.digest (.-subtle js/crypto) "SHA-256" bytes)
      (.then (fn [digest]
               (apply str
                      (map (fn [byte]
                             (.padStart (.toString byte 16) 2 "0"))
                           (array-seq (js/Uint8Array. digest))))))))

(defn- normalize-image-alpha!
  "Return a bitmap whose RGB is straight.  PNG decode already yields straight
   bytes for :straight/:opaque sources.  A source explicitly tagged
   :premultiplied is unassociated exactly once before texture upload (T3/T9)."
  [source ^js bitmap]
  (if-not (= :premultiplied (:image/alpha-association source))
    (js/Promise.resolve bitmap)
    (let [width (.-width bitmap)
          height (.-height bitmap)
          canvas (js/OffscreenCanvas. width height)
          context (.getContext canvas "2d"
                               #js {:willReadFrequently true})
          _ (.drawImage context bitmap 0 0)
          image-data (.getImageData context 0 0 width height)
          pixels (.-data image-data)]
      (loop [index 0]
        (when (< index (.-length pixels))
          (let [alpha (aget pixels (+ index 3))]
            (when (pos? alpha)
              (aset pixels index
                    (min 255 (Math/round (/ (* (aget pixels index) 255.0)
                                              alpha))))
              (aset pixels (+ index 1)
                    (min 255 (Math/round (/ (* (aget pixels (+ index 1)) 255.0)
                                              alpha))))
              (aset pixels (+ index 2)
                    (min 255 (Math/round (/ (* (aget pixels (+ index 2)) 255.0)
                                              alpha))))))
          (recur (+ index 4))))
      (.putImageData context image-data 0 0)
      (.close bitmap)
      (js/createImageBitmap canvas #js {:colorSpaceConversion "none"
                                        :premultiplyAlpha "none"}))))

(defn init-image-system
  "Own the image pipeline, digest registry, atlas/dedicated resources, and the
   one shared 13-word instance pool.  Product activation remains staged; the
  harness creates this system directly."
  [^js device fformat camera-buffer groups-buffer
   & {:keys [initial-capacity scene-color]
      :or {initial-capacity 256
           scene-color scene-color/legacy-direct-color}}]
  (assert groups-buffer "init-image-system requires :groups-buffer")
  (let [bind-layout (.createBindGroupLayout
                     device
                     (clj->js
                      {:entries [{:binding 0
                                  :visibility js/GPUShaderStage.FRAGMENT
                                  :sampler {:type "filtering"}}
                                 {:binding 1
                                  :visibility js/GPUShaderStage.FRAGMENT
                                  :texture {:sampleType "float"}}
                                 {:binding 2
                                  :visibility js/GPUShaderStage.VERTEX
                                  :buffer {:type "uniform"}}
                                 {:binding 3
                                  :visibility js/GPUShaderStage.VERTEX
                                  :buffer {:type "read-only-storage"}}]}))
        pipeline (create-image-pipeline device fformat bind-layout scene-color)
        sampler (.createSampler device
                                (clj->js {:minFilter "linear"
                                          :magFilter "linear"
                                          :mipmapFilter "linear"
                                          :addressModeU "clamp-to-edge"
                                          :addressModeV "clamp-to-edge"}))
        mip-system (create-image-mip-system device scene-color)
        placeholder-texture (image-texture device 2 2 1)
        placeholder-bytes (js/Uint8Array.
                           #js [255 0 255 255, 24 24 24 255,
                                24 24 24 255, 255 0 255 255])
        _ (.writeTexture (.-queue device)
                         (clj->js {:texture placeholder-texture})
                         placeholder-bytes
                         (clj->js {:bytesPerRow 8 :rowsPerImage 2})
                         (clj->js {:width 2 :height 2}))
        placeholder-view (image-view placeholder-texture scene-color)
        placeholder-bind-group (create-image-bind-group
                                device bind-layout sampler placeholder-view
                                camera-buffer groups-buffer)
        {:keys [width height mip-level-count]} image-component/atlas-config
        atlas-texture (image-texture device width height mip-level-count)
        atlas-view (image-view atlas-texture scene-color)
        atlas-bind-group (create-image-bind-group
                          device bind-layout sampler atlas-view
                          camera-buffer groups-buffer)
        pool (buffer-pool/create-pool
              device initial-capacity
              :floats-per-item image-component/image-instance-words
              :pack-fn pack-image-instance)
        image-system
        {:device device :pipeline pipeline :bind-layout bind-layout
         :sampler sampler :mip-system mip-system :pool pool
         :camera-buffer camera-buffer :groups-buffer groups-buffer
         :scene-color scene-color
         :placeholder {:texture placeholder-texture
                       :bind-group placeholder-bind-group
                       :binding-key :image/placeholder
                       :uv [0.0 0.0 1.0 1.0]}
         :!atlas-resource (atom {:texture atlas-texture
                                 :bind-group atlas-bind-group})
         :!atlas (atom (image-component/empty-atlas))
         :!resources (atom {})
         :!source-registry (atom (image-component/empty-source-registry))
         :!source-bytes (atom {})
         :!prepared (atom [])
         :!last-frame-key (atom ::never)
         :!residency-rev (atom 0)}]
    image-system))

(defn- placeholder-residency [image-system status reason]
  (let [placeholder (:placeholder image-system)]
    {:status status
     :reason reason
     :binding {:key (:binding-key placeholder)
               :group (:bind-group placeholder)}
     :uv (:uv placeholder)}))

(defn- resource-residency [resource]
  (-> resource
      (assoc :status :ok
             :reason nil
             :binding {:key (:binding-key resource)
                       :group (:bind-group resource)})
      (dissoc :binding-key :bind-group)))

(defn- set-residency! [image-system digest residency]
  (swap! (:!resources image-system) assoc digest residency)
  (swap! (:!residency-rev image-system) inc)
  residency)

(defn- ensure-residency! [image-system digest]
  (or (get @(:!resources image-system) digest)
      (set-residency! image-system digest
                      (placeholder-residency image-system :unavailable
                                             :unresolvable-digest))))

(defn- copy-image-to-atlas!
  [image-system ^js bitmap {:keys [x y padding width height]}]
  (let [^js device (:device image-system)
        ^js texture (:texture @(:!atlas-resource image-system))
        padded (padded-image-canvas bitmap padding)]
    (.copyExternalImageToTexture
     (.-queue device)
     (clj->js {:source padded})
     (clj->js {:texture texture
               :origin {:x (- x padding) :y (- y padding)}})
     (clj->js {:width (+ width (* 2 padding))
               :height (+ height (* 2 padding))}))
    (generate-image-mips! device (:mip-system image-system) texture
                          (:mip-level-count image-component/atlas-config))))

(defn- create-dedicated-image-resource!
  [image-system digest ^js bitmap]
  (let [^js device (:device image-system)
        width (.-width bitmap)
        height (.-height bitmap)
        mip-level-count (image-component/mip-level-count width height)
        texture (image-texture device width height mip-level-count)
        _ (.copyExternalImageToTexture
           (.-queue device)
           (clj->js {:source bitmap})
           (clj->js {:texture texture})
           (clj->js {:width width :height height}))
        _ (generate-image-mips! device (:mip-system image-system) texture
                                mip-level-count)
        view (image-view texture (:scene-color image-system))
        bind-group (create-image-bind-group
                    device (:bind-layout image-system) (:sampler image-system)
                    view (:camera-buffer image-system)
                    (:groups-buffer image-system))]
    {:texture texture :bind-group bind-group
     :binding-key [:image/dedicated digest]
     :tier :dedicated :uv [0.0 0.0 1.0 1.0]
     :width width :height height :mip-level-count mip-level-count}))

(defn register-image-source!
  "Verify, decode, upload, and register one digest-addressed image source.
   The returned Promise resolves to an explicit status value.  No decode or
   Promise work is reachable from the frame producer."
  [image-system source bytes]
  (let [digest (:image/digest source)]
    (-> (js/Promise.resolve nil)
        (.then
         (fn [_]
           (image-component/validate-source! source)
           (bytes->sha256 bytes)))
        (.then
         (fn [computed-digest]
           (when-not (= digest computed-digest)
             (throw (ex-info "Image source digest mismatch"
                             {:reason :digest-mismatch
                              :declared digest :computed computed-digest})))
           (let [registry (image-component/register-verified-source
                           @(:!source-registry image-system)
                           source computed-digest)]
             (reset! (:!source-registry image-system) registry)
             (swap! (:!source-bytes image-system) assoc digest
                    {:source source :bytes bytes})
             (let [blob (js/Blob. #js [bytes] #js {:type "image/png"})]
               (js/createImageBitmap
                blob
                #js {:colorSpaceConversion
                     (if (get-in image-system [:scene-color :enabled?])
                       "default"
                       "none")
                     :premultiplyAlpha "none"})))))
        (.then (fn [bitmap]
                 (if bitmap
                   (normalize-image-alpha! source bitmap)
                   nil)))
        (.then
         (fn [bitmap]
           (when bitmap
             (let [width (.-width bitmap)
                   height (.-height bitmap)
                   declared [(:image/width source) (:image/height source)]]
               (when-not (= declared [width height])
                 (.close bitmap)
                 (throw (ex-info "Decoded image dimensions differ from source"
                                 {:reason :dimension-mismatch
                                  :declared declared :decoded [width height]})))
               (let [plan (image-component/placement-plan
                           @(:!atlas image-system) digest
                           {:width width :height height})
                     resource
                     (if (= :atlas (:tier plan))
                       (let [placement (:placement plan)
                             _ (reset! (:!atlas image-system) (:atlas plan))
                             _ (copy-image-to-atlas! image-system bitmap placement)
                             atlas-resource @(:!atlas-resource image-system)]
                         {:bind-group (:bind-group atlas-resource)
                          :binding-key :image/atlas
                          :tier :atlas :uv (:uv placement)
                          :placement placement
                          :width width :height height
                          :mip-level-count
                          (:mip-level-count image-component/atlas-config)})
                       (create-dedicated-image-resource!
                        image-system digest bitmap))
                     residency (resource-residency resource)]
                 (.close bitmap)
                 (set-residency! image-system digest residency)
                 {:status :ok :digest digest :reason nil})))))
        (.catch
         (fn [error]
           (let [data (ex-data error)
                 reason (or (:reason data) (:error-type data) :invalid-source)
                 residency-digest (or digest :image/unknown)
                 prior (get @(:!resources image-system) residency-digest)]
             (if (= :ok (:status prior))
               (swap! (:!residency-rev image-system) inc)
               (set-residency!
                image-system residency-digest
                (placeholder-residency image-system :rejected reason)))
             {:status :rejected :digest digest :reason reason}))))))

(defn- destroy-dedicated-resources! [image-system]
  (doseq [[_ resource] @(:!resources image-system)
          :when (= :dedicated (:tier resource))]
    (when-let [texture (:texture resource)]
      (.destroy ^js texture))))

(defn rebuild-image-resources!
  "Reconstruct every device-owned image resource on a freshly initialized
   replacement system.  No pipeline, atlas, placeholder, pool buffer, or
   bind-group from the lost device is reused."
  [lost-system replacement-system]
  (let [sources (sort-by #(get-in % [:source :image/digest])
                         (vals @(:!source-bytes lost-system)))
        source-digests (set (map #(get-in % [:source :image/digest]) sources))
        carried (remove (fn [[digest residency]]
                          (or (= :ok (:status residency))
                              (contains? source-digests digest)))
                        @(:!resources lost-system))
        resources-fresh?
        (and (not (identical? (:device lost-system)
                              (:device replacement-system)))
             (not (identical? (:pipeline lost-system)
                              (:pipeline replacement-system)))
             (not (identical? (get-in lost-system [:placeholder :texture])
                              (get-in replacement-system
                                      [:placeholder :texture])))
             (not (identical? (:texture @(:!atlas-resource lost-system))
                              (:texture @(:!atlas-resource
                                          replacement-system))))
             (not (identical? (:buffer @(:pool lost-system))
                              (:buffer @(:pool replacement-system)))))]
    (doseq [[digest {:keys [status reason]}] carried]
      (set-residency! replacement-system digest
                      (placeholder-residency replacement-system status reason)))
    (-> (js/Promise.all
         (clj->js (mapv (fn [{:keys [source bytes]}]
                          (register-image-source! replacement-system
                                                  source bytes))
                        sources)))
        (.then
         (fn [rebuilt]
           (swap! (:!residency-rev replacement-system) inc)
           {:image-system replacement-system
            :rebuilt (count (filter #(= :ok (:status %))
                                    (array-seq rebuilt)))
            :resources-fresh? resources-fresh?})))))

(defn destroy-image-system! [image-system]
  (destroy-dedicated-resources! image-system)
  (doseq [texture [(get-in image-system [:placeholder :texture])
                   (:texture @(:!atlas-resource image-system))]]
    (when texture
      (.destroy ^js texture)))
  (when-let [pool (:pool image-system)]
    (let [buffer (:buffer @pool)]
      (.destroy ^js buffer)))
  (when (seq @(:!resources image-system))
    (swap! (:!residency-rev image-system) inc))
  (reset! (:!resources image-system) {})
  (reset! (:!prepared image-system) [])
  (reset! (:!last-frame-key image-system) ::never)
  true)

(defn- inset-resource-uv [resource-uv crop-uv]
  (let [[resource-u0 resource-v0 resource-u1 resource-v1] resource-uv
        [crop-u0 crop-v0 crop-u1 crop-v1] crop-uv
        du (- resource-u1 resource-u0)
        dv (- resource-v1 resource-v0)]
    [(+ resource-u0 (* crop-u0 du))
     (+ resource-v0 (* crop-v0 dv))
     (+ resource-u0 (* crop-u1 du))
     (+ resource-v0 (* crop-v1 dv))]))

(defn- resolve-image-draw-item [image-system world-transforms image-draw-item]
  (let [component (:image/component image-draw-item)
        digest (:image/source-digest component)
        residency (ensure-residency! image-system digest)
        resource? (= :ok (:status residency))
        crop (image-component/normalize-crop
              (:image/intrinsic-size component) (:image/crop component))
        crop-uv (image-component/crop->uv (:image/intrinsic-size component) crop)
        binding (:binding residency)]
    (assoc image-draw-item
           :buffer-index (transform/buffer-index world-transforms (:container image-draw-item))
           :image/status (:status residency)
           :image/reason (:reason residency)
           :image/binding-key (:key binding)
           :image/bind-group (:group binding)
           :image/resolved-uv
           (if resource?
             (inset-resource-uv (:uv residency) crop-uv)
             (:uv residency)))))

(defn prepare-image-frame!
  "Write the image pool only when a component/group key or residency
   revision changes."
  [image-system draw-items world-transforms]
  (let [draw-items (or draw-items [])]
    (doseq [draw-item draw-items]
      (ensure-residency! image-system
                         (get-in draw-item [:image/component :image/source-digest])))
    (let [frame-key (frame/frame-key draw-items @(:!residency-rev image-system))]
      (if (= frame-key @(:!last-frame-key image-system))
        {:changed? false :writes 0
         :instances (count @(:!prepared image-system))}
        (let [prepared (mapv #(resolve-image-draw-item image-system world-transforms %) draw-items)
              item-writes (buffer-pool/batch-update-pool!
                           (:pool image-system) prepared)]
          (reset! (:!last-frame-key image-system) frame-key)
          (reset! (:!prepared image-system) prepared)
          {:changed? true
           :writes item-writes
           :instances (count prepared)})))))

(defn image-draw-runs
  "Binding runs over one contiguous slice of the prepared image draw-items, in draw-item
   order; no texture grouping may reorder the stamped draw-item stream."
  [image-system offset instance-count]
  (let [prepared @(:!prepared image-system)
        buffer-index-items (subvec prepared offset (+ offset instance-count))
        buffer (:buffer @(:pool image-system))]
    (mapv (fn [{:keys [first-instance instance-count draw-items]}]
            {:bind-group (:image/bind-group (first draw-items))
             :buffer buffer
             :instance-count instance-count
             :first-instance (+ offset first-instance)})
          (image-component/contiguous-binding-runs buffer-index-items))))

(defn draw-image-runs!
  "Family-owned sub-draw walker over one prepared image slice."
  [^js pass image-system offset instance-count]
  (.setPipeline pass (:pipeline image-system))
  (doseq [{:keys [bind-group buffer instance-count first-instance]}
          (image-draw-runs image-system offset instance-count)]
    (.setBindGroup pass 0 bind-group)
    (.setVertexBuffer pass 0 buffer)
    (.draw pass 6 instance-count 0 first-instance)))
