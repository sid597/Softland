(ns app.client.image.painter
  "The image painter: quads sampling an atlas or a dedicated texture, with mip
   levels for zooming out.
   Takes: a device, a format, and the shared camera and containers buffers to
   build the system; image sources to register; the frame's image ops to
   prepare; a render pass to draw into.
   Gives: an image system with atlas, pipelines, and mip generator; packed
   instances in a pool; draw calls.
   Holds: per-system atoms for the buffer, capacity, prepared state, last
   images, and resources."
  (:require [clojure.string :as str]
            [app.client.engine.buffer-pool :as buffer-pool]
            [app.client.engine.budget :as gpu-budget]
            [app.client.engine.color :as scene-color]
            [app.client.engine.device :as device]
            [app.client.image.material :as image-material]))

;; IMAGE-ATOM T3: the candidate samples through an sRGB texture view (the one
;; ingress decode) and writes a linear-premultiplied value to an *-srgb target
;; (the one presentation encode).  The legacy seam samples an unorm view and
;; returns encoded straight RGB, so it never half-converts.
(def ^:private image-color-mode-declaration
  "const kImageLinearPremultiplied: bool = false;")

(defn- configure-image-color-shader [shader color]
  (if (:enabled? color)
    (str/replace shader image-color-mode-declaration
                 "const kImageLinearPremultiplied: bool = true;")
    shader))

(def image-vertex-shader "
  struct Camera { pan: vec2<f32>, zoom: f32, padding: f32, screen_dimensions: vec2<f32>, };
  @group(0) @binding(2) var<uniform> camera: Camera;
  struct ContainerTransform {
    axis_x: vec2<f32>, axis_y: vec2<f32>, translation: vec2<f32>,
    flags: u32, padding: u32,
  };
  @group(0) @binding(3) var<storage, read> containers: array<ContainerTransform>;
  struct InstanceInput {
    @location(0) rect: vec4<f32>,
    @location(1) uv_bounds: vec4<f32>,
    @location(2) tint: vec4<f32>,
    @location(3) container_idx: u32,
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
    let c = containers[instance.container_idx];
    let is_screen = (c.flags & 1u) != 0u;
    let zm = select(camera.zoom, 1.0, is_screen);
    let pn = select(camera.pan, vec2<f32>(0.0, 0.0), is_screen);
    let axis_scale = max(vec2<f32>(length(c.axis_x), length(c.axis_y)) * zm,
                         vec2<f32>(0.0001, 0.0001));
    // Ramped Cg support: expand the raster hull by half a screen pixel while
    // keeping the mathematical quad at edge_pos 0/edge_size (IMAGE-ATOM G4).
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

(def image-fragment-shader
  (str image-color-mode-declaration "\n"
       "fn srgb_channel_to_linear(v: f32) -> f32 {\n"
       "  if (v <= 0.04045) { return v / 12.92; }\n"
       "  return pow((v + 0.055) / 1.055, 2.4);\n"
       "}\n"
       "@group(0) @binding(0) var image_sampler: sampler;\n"
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
       "  if (!kImageLinearPremultiplied) {\n"
       "    return vec4<f32>(sampled.rgb * tint.rgb, sampled.a * tint.a * cg);\n"
       "  }\n"
       "  let tint_linear = vec3<f32>(srgb_channel_to_linear(tint.r),\n"
       "                              srgb_channel_to_linear(tint.g),\n"
       "                              srgb_channel_to_linear(tint.b));\n"
       "  let alpha = clamp(sampled.a * tint.a * cg, 0.0, 1.0);\n"
       "  return vec4<f32>(sampled.rgb * tint_linear * alpha, alpha);\n"
       "}\n"))

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

(def image-instance-stride image-material/image-instance-stride)

;; --- Image atom resource system --------------------------------------------

(defn- pack-image-instance [image-op]
  (let [words (image-material/instance-words
               {:x (:x image-op) :y (:y image-op)
                :w (:w image-op) :h (:h image-op)
                :uv (:image/resolved-uv image-op)
                :tint (:image/tint image-op)
                :opacity (:image/opacity image-op)
                :container-idx (:container-idx image-op)})
        data (js/Float32Array. image-material/image-instance-words)
        uints (js/Uint32Array. (.-buffer data))]
    (dotimes [index 12]
      (aset data index (nth words index)))
    (aset uints 12 (nth words 12))
    data))

(defn- create-image-bind-group
  [^js device layout sampler texture-view camera-buffer containers-buffer]
  (.createBindGroup
   device
   (clj->js {:layout layout
             :entries [{:binding 0 :resource sampler}
                       {:binding 1 :resource texture-view}
                       {:binding 2 :resource {:buffer camera-buffer}}
                       {:binding 3 :resource {:buffer containers-buffer}}]})))

(defn- create-image-pipeline
  [^js device fformat bind-layout scene-color]
  (let [vertex-module (.createShaderModule device
                                          (clj->js {:code image-vertex-shader}))
        fragment-module (.createShaderModule
                         device
                         (clj->js {:code (configure-image-color-shader
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
        ;; IMAGE-ATOM T4: candidate mip views are sRGB on both sides, so
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

(defn- receipt-row-counts [rows]
  (reduce (fn [counts [_ {:keys [status]}]]
            (update counts status (fnil inc 0)))
          {:ok 0 :refused 0 :unavailable 0 :device-lost 0}
          rows))

(defn- publish-image-receipt! [image-system]
  (let [!receipt (:!receipt image-system)
        receipt (assoc @!receipt :counts (receipt-row-counts (:rows @!receipt)))]
    (reset! !receipt receipt)
    (aset js/globalThis "__softland_image_ingress_receipt" (clj->js receipt))
    receipt))

(defn- record-image-receipt!
  [image-system digest status reason details]
  (swap! (:!receipt image-system)
         (fn [receipt]
           (let [event (merge {:status status :reason reason} details)
                 prior (get-in receipt [:rows digest])
                 history (conj (vec (:history prior)) event)]
             (-> receipt
                 (assoc-in [:rows digest] (assoc event :history history))
                 (assoc :updated-at-ms (.now js/performance))))))
  (publish-image-receipt! image-system))

(defn- padded-image-canvas [^js bitmap padding]
  (let [width (.-width bitmap)
        height (.-height bitmap)
        canvas (js/OffscreenCanvas. (+ width (* 2 padding))
                                    (+ height (* 2 padding)))
        context (.getContext canvas "2d")]
    (.clearRect context 0 0 (.-width canvas) (.-height canvas))
    (.drawImage context bitmap padding padding)
    ;; IMAGE-ATOM T5: extrude edge texels through the declared gutter before
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
   verifier creates this system directly."
  [^js device fformat camera-buffer containers-buffer
   & {:keys [initial-capacity tracker scene-color budget-cap-bytes]
      :or {initial-capacity 256
           scene-color scene-color/legacy-direct-color}}]
  (assert containers-buffer "init-image-system requires :containers-buffer")
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
        _ (gpu-budget/register-texture! tracker placeholder-texture
                                        "image/placeholder"
                                        :format "rgba8unorm" :width 2 :height 2
                                        :mip-level-count 1)
        placeholder-view (image-view placeholder-texture scene-color)
        placeholder-bind-group (create-image-bind-group
                                device bind-layout sampler placeholder-view
                                camera-buffer containers-buffer)
        {:keys [width height mip-level-count]} image-material/atlas-config
        atlas-texture (image-texture device width height mip-level-count)
        _ (gpu-budget/register-texture! tracker atlas-texture "image/atlas"
                                        :format "rgba8unorm"
                                        :width width :height height
                                        :mip-level-count mip-level-count)
        atlas-view (image-view atlas-texture scene-color)
        atlas-bind-group (create-image-bind-group
                          device bind-layout sampler atlas-view
                          camera-buffer containers-buffer)
        pool (buffer-pool/create-pool
              device initial-capacity pipeline nil
              :floats-per-item image-material/image-instance-words
              :pack-fn pack-image-instance
              :tracker tracker :label "image/instances")
        image-system
        {:device device :pipeline pipeline :bind-layout bind-layout
         :sampler sampler :mip-system mip-system :pool pool
         :camera-buffer camera-buffer :containers-buffer containers-buffer
         :scene-color scene-color :gpu-tracker tracker
         :budget-cap-bytes budget-cap-bytes
         :placeholder {:texture placeholder-texture
                       :bind-group placeholder-bind-group
                       :binding-key :image/placeholder
                       :uv [0.0 0.0 1.0 1.0]}
         :!atlas-resource (atom {:texture atlas-texture
                                 :bind-group atlas-bind-group})
         :!atlas (atom (image-material/empty-atlas))
         :!resources (atom {})
         :!source-registry (atom (image-material/empty-source-registry))
         :!source-bytes (atom {})
         :frame-input/identity (js-obj) :!shape-rev (atom 0)
         :!last-images (atom ::never) :!prepared-images (atom [])
         :!last-prepare-key (atom ::never)
         :!receipt (atom {:version 1 :rows {}
                          :ingress image-material/ingress-receipt})}]
    (publish-image-receipt! image-system)
    image-system))

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
                          (:mip-level-count image-material/atlas-config))))

(defn- create-dedicated-image-resource!
  [image-system digest ^js bitmap]
  (let [^js device (:device image-system)
        width (.-width bitmap)
        height (.-height bitmap)
        mip-level-count (image-material/mip-level-count width height)
        texture (image-texture device width height mip-level-count)
        _ (.copyExternalImageToTexture
           (.-queue device)
           (clj->js {:source bitmap})
           (clj->js {:texture texture})
           (clj->js {:width width :height height}))
        _ (generate-image-mips! device (:mip-system image-system) texture
                                mip-level-count)
        _ (gpu-budget/register-texture! (:gpu-tracker image-system) texture
                                        (str "image/dedicated/" digest)
                                        :format "rgba8unorm"
                                        :width width :height height
                                        :mip-level-count mip-level-count)
        view (image-view texture (:scene-color image-system))
        bind-group (create-image-bind-group
                    device (:bind-layout image-system) (:sampler image-system)
                    view (:camera-buffer image-system)
                    (:containers-buffer image-system))]
    {:texture texture :bind-group bind-group
     :binding-key [:image/dedicated digest]
     :tier :dedicated :uv [0.0 0.0 1.0 1.0]
     :width width :height height :mip-level-count mip-level-count}))

(defn register-image-source!
  "Verify, decode, upload, and register one digest-addressed image source.
   The returned Promise resolves to a GPU resource or nil on a receipted
   refusal.  No decode or Promise work is reachable from the frame producer
   (IMAGE-ATOM T2/T9/T10)."
  [image-system source bytes]
  (let [digest (:image/digest source)
        started-at (.now js/performance)]
    (-> (js/Promise.resolve nil)
        (.then
         (fn [_]
           (image-material/validate-source! source)
           (bytes->sha256 bytes)))
        (.then
         (fn [computed-digest]
           (when-not (= digest computed-digest)
             (throw (ex-info "Image source digest mismatch"
                             {:reason :digest-mismatch
                              :declared digest :computed computed-digest})))
           (let [registry (image-material/register-verified-source
                           @(:!source-registry image-system)
                           source computed-digest)
                 width (:image/width source)
                 height (:image/height source)
                 planned-bytes (image-material/texture-bytes width height)
                 cap (:budget-cap-bytes image-system)]
             (reset! (:!source-registry image-system) registry)
             (swap! (:!source-bytes image-system) assoc digest
                    {:source source :bytes bytes})
             (if (and cap (> planned-bytes cap))
               (do
                 ;; IMAGE-ATOM T11: refusal uses full-chain bytes; level zero
                 ;; can never sneak through an injected budget cap.
                 (record-image-receipt!
                  image-system digest :refused :over-budget
                  {:planned-bytes planned-bytes :budget-cap-bytes cap})
                 nil)
               (let [blob (js/Blob. #js [bytes] #js {:type "image/png"})]
                 (js/createImageBitmap
                  blob
                  #js {:colorSpaceConversion
                       (if (get-in image-system [:scene-color :enabled?])
                         "default"
                         "none")
                       :premultiplyAlpha "none"}))))))
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
               (let [decoded-at (.now js/performance)
                     plan (image-material/placement-plan
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
                          (:mip-level-count image-material/atlas-config)})
                       (create-dedicated-image-resource!
                        image-system digest bitmap))
                     completed-at (.now js/performance)]
                 (.close bitmap)
                 (swap! (:!resources image-system) assoc digest resource)
                 (record-image-receipt!
                  image-system digest :ok (:tier resource)
                  {:tier (:tier resource)
                   :mip-level-count (:mip-level-count resource)
                   :ingress-transfers (if (get-in image-system
                                                  [:scene-color :enabled?])
                                        1 0)
                   :presentation-encodes (if (get-in image-system
                                                     [:scene-color :enabled?])
                                           1 0)
                   :decode-ms (- decoded-at started-at)
                   :upload-enqueue-ms (- completed-at decoded-at)
                   :total-enqueue-ms (- completed-at started-at)})
                 resource)))))
        (.catch
         (fn [error]
           (let [data (ex-data error)
                 reason (or (:reason data) :invalid-source)]
             (record-image-receipt!
              image-system (or digest :image/unknown) :refused reason
              {:message (or (.-message error) (str error))})
             nil))))))

(defn image-ingress-receipt [image-system]
  (publish-image-receipt! image-system))

(defn- destroy-dedicated-resources! [image-system]
  (doseq [[_ resource] @(:!resources image-system)
          :when (= :dedicated (:tier resource))]
    (when-let [texture (:texture resource)]
      (gpu-budget/destroy-resource! (:gpu-tracker image-system) texture
                                    :reason :image-resource-destroy)
      (.destroy ^js texture))))

(defn rebuild-image-resources!
  "Reconstruct every device-owned image resource on a freshly initialized
   replacement system.  No pipeline, atlas, placeholder, pool buffer, or
   bind-group from the lost device is reused.  The digest registry is the only
   bridge across devices (IMAGE-ATOM T2/T11)."
  [lost-system replacement-system]
  (let [sources (sort-by #(get-in % [:source :image/digest])
                         (vals @(:!source-bytes lost-system)))
        digests (mapv #(get-in % [:source :image/digest]) sources)
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
    (doseq [digest digests]
      (record-image-receipt! lost-system digest :device-lost :device-lost
                             {:rebuild-count 0})
      (record-image-receipt! replacement-system digest
                             :device-lost :replacement-rebuild-start
                             {:rebuild-count 0}))
    (-> (js/Promise.all
         (clj->js (mapv (fn [{:keys [source bytes]}]
                          (register-image-source! replacement-system
                                                  source bytes))
                        sources)))
        (.then
         (fn [rebuilt]
           (swap! (:!receipt replacement-system) assoc
                  :device-loss {:status :device-lost
                                :rebuilt-count
                                (count (filter some? (array-seq rebuilt)))
                                :replacement-device? true
                                :resource-identities-fresh?
                                resources-fresh?})
           {:image-system replacement-system
            :receipt (publish-image-receipt! replacement-system)})))))

(defn destroy-image-system! [image-system]
  (destroy-dedicated-resources! image-system)
  (doseq [texture [(get-in image-system [:placeholder :texture])
                   (:texture @(:!atlas-resource image-system))]]
    (when texture
      (gpu-budget/destroy-resource! (:gpu-tracker image-system) texture
                                    :reason :image-system-destroy)
      (.destroy ^js texture)))
  (when-let [pool (:pool image-system)]
    (let [buffer (:buffer @pool)]
      (gpu-budget/destroy-resource! (:gpu-tracker image-system) buffer
                                    :reason :image-system-destroy)
      (.destroy ^js buffer)))
  (reset! (:!resources image-system) {})
  (reset! (:!prepared-images image-system) [])
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

(defn- resolve-image-op [image-system image-op]
  (let [digest (:image/digest image-op)
        resource (get @(:!resources image-system) digest)
        prior-row (get-in @(:!receipt image-system) [:rows digest])
        resolved (or resource (:placeholder image-system))]
    (when-not resource
      ;; Missing resources are a deterministic material outcome.  The frame
      ;; never starts decode work and never silently drops an instance (T10).
      ;; A prior refusal or device-loss event keeps its causal status/history;
      ;; painting its placeholder must not relabel it as an unknown digest.
      (if prior-row
        (do
          (swap! (:!receipt image-system) assoc-in
                 [:rows digest :placeholder-rendered] true)
          (publish-image-receipt! image-system))
        (record-image-receipt! image-system digest :unavailable
                               :unresolvable-digest
                               {:placeholder true
                                :placeholder-rendered true})))
    (assoc image-op
           :image/binding-key (:binding-key resolved)
           :image/bind-group (:bind-group resolved)
           :image/resolved-uv
           (if resource
             (inset-resource-uv (:uv resolved) (:image/uv image-op))
             (:uv resolved)))))

(defn prepare-image-frame!
  "Identity-gated image pool write: the prepared vector is rewritten only when
   the image ops or the resolved resources changed (IMAGE-ATOM T10)."
  [image-system images]
  (let [images (or images [])
        !last-images (:!last-images image-system)
        prepare-key {:images images :resources @(:!resources image-system)}]
    (if (= prepare-key @(:!last-prepare-key image-system))
      {:identity-changed? false :writes 0
       :instances (count @(:!prepared-images image-system))}
      (let [prepared (mapv #(resolve-image-op image-system %) images)
            writes (buffer-pool/batch-update-pool! (:pool image-system)
                                                   prepared)]
        (reset! !last-images images)
        (reset! (:!last-prepare-key image-system) prepare-key)
        (reset! (:!prepared-images image-system) prepared)
        (swap! (:!receipt image-system) assoc
               :frame-write {:identity-changed? true
                             :writes writes :instances (count prepared)})
        (publish-image-receipt! image-system)
        {:identity-changed? true :writes writes
         :instances (count prepared)}))))

(defn image-draw-runs
  "Binding runs over one contiguous slice of the prepared image ops, in op
   order; no texture grouping may reorder the stamped op stream."
  [image-system offset instance-count]
  (let [prepared @(:!prepared-images image-system)
        slot-ops (subvec prepared offset (+ offset instance-count))
        buffer (:buffer @(:pool image-system))]
    (mapv (fn [{:keys [first-instance instance-count ops]}]
            {:bind-group (:image/bind-group (first ops))
             :buffer buffer
             :instance-count instance-count
             :first-instance (+ offset first-instance)})
          (image-material/contiguous-binding-runs slot-ops))))

(defn draw-image-runs!
  "Family-owned sub-draw walker over one prepared image slice."
  [^js pass image-system offset instance-count]
  (.setPipeline pass (:pipeline image-system))
  (doseq [{:keys [bind-group buffer instance-count first-instance]}
          (image-draw-runs image-system offset instance-count)]
    (.setBindGroup pass 0 bind-group)
    (.setVertexBuffer pass 0 buffer)
    (.draw pass 6 instance-count 0 first-instance)))

(defn- create-linear-image-variant [^js device image-system]
  (when image-system
    (let [bind-layout
          (.createBindGroupLayout
           device
           (clj->js
            {:entries [{:binding 0 :visibility js/GPUShaderStage.FRAGMENT
                        :sampler {:type "filtering"}}
                       {:binding 1 :visibility js/GPUShaderStage.FRAGMENT
                        :texture {:sampleType "float"}}
                       {:binding 2 :visibility js/GPUShaderStage.VERTEX
                        :buffer {:type "uniform"}}
                       {:binding 3 :visibility js/GPUShaderStage.VERTEX
                        :buffer {:type "read-only-storage"}}]}))
          pipeline (create-image-pipeline device "rgba16float" bind-layout
                                          scene-color/linear-premultiplied-color)
          binding-map (js/WeakMap.)
          install!
          (fn [{:keys [texture bind-group]}]
            (when (and texture bind-group (not (.has binding-map bind-group)))
              (let [view (.createView ^js texture
                                      (clj->js {:format "rgba8unorm-srgb"}))
                    linear-bind-group
                    (create-image-bind-group
                     device bind-layout (:sampler image-system) view
                     (:camera-buffer image-system)
                     (:containers-buffer image-system))]
                (.set binding-map bind-group linear-bind-group))))
          sync!
          (fn []
            (install! (:placeholder image-system))
            (install! @(:!atlas-resource image-system))
            (doseq [resource (vals @(:!resources image-system))]
              (install! resource))
            true)]
      (sync!)
      {:pipeline pipeline :binding-map binding-map :sync! sync!})))
