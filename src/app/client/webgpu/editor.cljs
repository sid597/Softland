(ns app.client.webgpu.editor)

;; --- 1. SHADERS (Unchanged) ---
(def rect-vertex-shader "
  struct Camera { pan: vec2<f32>, zoom: f32, padding: f32, screen_dimensions: vec2<f32>, };
  @group(0) @binding(0) var<uniform> camera: Camera;
  struct InstanceInput { @location(0) rect_geometry: vec4<f32>, @location(1) color: vec4<f32>, };
  struct VertexOutput { @builtin(position) position: vec4<f32>, @location(0) color: vec4<f32>, };

  @vertex
  fn main(@builtin(vertex_index) v_index: u32, instance: InstanceInput) -> VertexOutput {
      var output: VertexOutput;
      var pos = vec2<f32>(0.0, 0.0);
      switch(v_index) {
          case 0u: { pos = vec2<f32>(0.0, 0.0); } case 1u: { pos = vec2<f32>(1.0, 0.0); }
          case 2u: { pos = vec2<f32>(0.0, 1.0); } case 3u: { pos = vec2<f32>(1.0, 0.0); }
          case 4u: { pos = vec2<f32>(1.0, 1.0); } default: { pos = vec2<f32>(0.0, 1.0); }
      }
      let world_pos = vec2<f32>(instance.rect_geometry.x + (pos.x * instance.rect_geometry.z),
                                instance.rect_geometry.y + (pos.y * instance.rect_geometry.w));
      let panned = (world_pos * camera.zoom) + camera.pan;
      let ndc = (panned / camera.screen_dimensions * 2.0) - vec2<f32>(1.0, 1.0);
      output.position = vec4<f32>(ndc.x, -ndc.y, 0.0, 1.0);
      output.color = instance.color;
      return output;
  }")

(def rect-fragment-shader "
  @fragment fn main(@location(0) color: vec4<f32>) -> @location(0) vec4<f32> { return color; }")

(def text-vertex-shader "
  struct Camera { pan: vec2<f32>, zoom: f32, padding: f32, screen_dimensions: vec2<f32>, };
  @group(0) @binding(2) var<uniform> camera: Camera;
  struct InstanceInput { @location(0) rect: vec4<f32>, @location(1) uv_bounds: vec4<f32>, };
  struct VertexOutput { @builtin(position) position: vec4<f32>, @location(0) uv: vec2<f32>, @location(1) v_visual_size: f32, };

  @vertex
  fn main(@builtin(vertex_index) v_index: u32, instance: InstanceInput) -> VertexOutput {
      var output: VertexOutput;
      var pos = vec2<f32>(0.0, 0.0);
      switch(v_index) {
          case 0u: { pos = vec2<f32>(0.0, 0.0); } case 1u: { pos = vec2<f32>(1.0, 0.0); }
          case 2u: { pos = vec2<f32>(0.0, 1.0); } case 3u: { pos = vec2<f32>(1.0, 0.0); }
          case 4u: { pos = vec2<f32>(1.0, 1.0); } default: { pos = vec2<f32>(0.0, 1.0); }
      }
      let world_pos = vec2<f32>(instance.rect.x + (pos.x * instance.rect.z),
                                instance.rect.y + (pos.y * instance.rect.w));
      let u = mix(instance.uv_bounds.x, instance.uv_bounds.z, pos.x);
      let v = mix(instance.uv_bounds.y, instance.uv_bounds.w, pos.y);
      let panned = (world_pos * camera.zoom) + camera.pan;
      let ndc = (panned / camera.screen_dimensions * 2.0) - vec2<f32>(1.0, 1.0);
      output.position = vec4<f32>(ndc.x, -ndc.y, 0.0, 1.0);
      output.uv = vec2<f32>(u, v);
      output.v_visual_size = max(instance.rect.z, instance.rect.w) * camera.zoom;
      return output;
  }")

(def text-fragment-shader "
  @group(0) @binding(0) var sampler0: sampler;
  @group(0) @binding(1) var texture0: texture_2d<f32>;
  struct Sizing { pxRange: f32, atlasEmSize: f32, color_r: f32, color_g: f32, color_b: f32, padding: f32, };
  @group(0) @binding(3) var<uniform> params: Sizing;
  fn median(a: f32, b: f32, c: f32) -> f32 { return max(min(a, b), min(max(a, b), c)); }

  @fragment
  fn main(@location(0) uv: vec2<f32>, @location(1) visual_size: f32) -> @location(0) vec4<f32> {
       let msd = textureSample(texture0, sampler0, uv).rgb;
       let sd = median(msd.r, msd.g, msd.b);
       let screenPxRange = params.pxRange * (visual_size / params.atlasEmSize);
       let size_factor = clamp(1.0 - (visual_size / 24.0), 0.0, 1.0);
       let dist = sd - 0.5 + (size_factor * 0.2); 
       let opacity = clamp(dist * screenPxRange + 0.5, 0.0, 1.0);
       return vec4<f32>(params.color_r, params.color_g, params.color_b, opacity);
  }")

;; --- 2. INITIALIZATION ---

(defn init-rect-system [^js/GPUDevice device fformat camera-buffer & {:keys [initial-capacity] :or {initial-capacity 1000}}]
  (let [v-module (.createShaderModule device (clj->js {:code rect-vertex-shader}))
        f-module (.createShaderModule device (clj->js {:code rect-fragment-shader}))
        instance-buffer (.createBuffer device (clj->js {:size (* initial-capacity 32) :usage (bit-or js/GPUBufferUsage.VERTEX js/GPUBufferUsage.COPY_DST)}))
        bg-layout (.createBindGroupLayout device (clj->js {:entries [{:binding 0 :visibility js/GPUShaderStage.VERTEX :buffer {:type "uniform"}}]}))
        pipeline-layout (.createPipelineLayout device (clj->js {:bindGroupLayouts [bg-layout]}))
        pipeline (.createRenderPipeline device (clj->js {:layout pipeline-layout
                                                         :vertex {:module v-module :entryPoint "main"
                                                                  :buffers [{:arrayStride 32 :stepMode "instance"
                                                                             :attributes [{:shaderLocation 0 :offset 0 :format "float32x4"}
                                                                                          {:shaderLocation 1 :offset 16 :format "float32x4"}]}]}
                                                         :fragment {:module f-module :entryPoint "main"
                                                                    :targets [{:format fformat :blend {:color {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}
                                                                                                       :alpha {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}}}]}
                                                         :primitive {:topology "triangle-list"}}))
        bind-group (.createBindGroup device (clj->js {:layout bg-layout :entries [{:binding 0 :resource {:buffer camera-buffer}}]}))]
    {:pipeline pipeline :bind-group bind-group :instance-buffer instance-buffer :num-instances 0}))

(defn init-text-system [^js/GPUDevice device fformat atlas font-bitmap & {:keys [initial-capacity] :or {initial-capacity 10000}}]
  (println "INITIALIZING INSTANCED TEXT SYSTEM")
  (let [vertex-module (.createShaderModule device (clj->js {:code text-vertex-shader}))
        fragment-module (.createShaderModule device (clj->js {:code text-fragment-shader}))
        texture (.createTexture device (clj->js {:size {:width (.-width font-bitmap) :height (.-height font-bitmap) :depthOrArrayLayers 1}
                                                 :format "rgba8unorm" :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT js/GPUTextureUsage.TEXTURE_BINDING js/GPUTextureUsage.COPY_DST)}))
        _ (.copyExternalImageToTexture (.-queue device) (clj->js {:source font-bitmap}) (clj->js {:texture texture}) (clj->js {:width (.-width font-bitmap) :height (.-height font-bitmap)}))
        sampler (.createSampler device (clj->js {:minFilter "linear" :magFilter "linear" :mipmapFilter "linear"}))
        instance-buffer (.createBuffer device (clj->js {:size (* initial-capacity 32) :usage (bit-or js/GPUBufferUsage.VERTEX js/GPUBufferUsage.COPY_DST)}))
        camera-buffer (.createBuffer device (clj->js {:size 24 :usage (bit-or js/GPUBufferUsage.UNIFORM js/GPUBufferUsage.COPY_DST)}))
        sizes-buffer (.createBuffer device (clj->js {:size 24 :usage (bit-or js/GPUBufferUsage.UNIFORM js/GPUBufferUsage.COPY_DST)}))
        bg-layout (.createBindGroupLayout device (clj->js {:entries [{:binding 0 :visibility js/GPUShaderStage.FRAGMENT :sampler {:type "filtering"}}
                                                                     {:binding 1 :visibility js/GPUShaderStage.FRAGMENT :texture {:sampleType "float"}}
                                                                     {:binding 2 :visibility js/GPUShaderStage.VERTEX :buffer {:type "uniform"}}
                                                                     {:binding 3 :visibility js/GPUShaderStage.FRAGMENT :buffer {:type "uniform"}}]}))
        pipeline-layout (.createPipelineLayout device (clj->js {:bindGroupLayouts [bg-layout]}))
        pipeline (.createRenderPipeline device (clj->js {:layout pipeline-layout
                                                         :vertex {:module vertex-module :entryPoint "main"
                                                                  :buffers [{:arrayStride 32 :stepMode "instance"
                                                                             :attributes [{:shaderLocation 0 :offset 0 :format "float32x4"}
                                                                                          {:shaderLocation 1 :offset 16 :format "float32x4"}]}]}
                                                         :fragment {:module fragment-module :entryPoint "main"
                                                                    :targets [{:format fformat :blend {:color {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}
                                                                                                       :alpha {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}}}]}
                                                         :primitive {:topology "triangle-list"}}))
        bind-group (.createBindGroup device (clj->js {:layout bg-layout
                                                      :entries [{:binding 0 :resource sampler} {:binding 1 :resource (.createView texture)}
                                                                {:binding 2 :resource {:buffer camera-buffer}} {:binding 3 :resource {:buffer sizes-buffer}}]}))]
    {:pipeline pipeline :bind-group bind-group :camera-uniform-buffer camera-buffer :sizes-uniform-buffer sizes-buffer :instance-buffer instance-buffer :num-instances 0}))

(defn create-editor-state [{:keys [device format atlas bitmap]}]
  (let [text-sys (init-text-system device format atlas bitmap :initial-capacity 1000000)
        rect-sys (init-rect-system device format (:camera-uniform-buffer text-sys) :initial-capacity 50000)
        
        ;; OPTIMIZATION 1: Pre-allocate the Camera Array
        camera-floats (js/Float32Array. 6)
        
        ;; OPTIMIZATION 2: Pre-allocate the Render Pass Descriptor object
        ;; We create the JS object ONCE. We will just swap the .view property later.
        pass-descriptor (clj->js {:colorAttachments [{:view nil ;; Placeholder
                                                      :clearValue {:r 0.0 :g 0.0 :b 0.0 :a 0.0}
                                                      :loadOp "clear"
                                                      :storeOp "store"}]})]
    
    {:text-sys text-sys 
     :rect-sys rect-sys
     ;; Store reusable objects in the state
     :camera-floats camera-floats
     :pass-descriptor pass-descriptor}))

;; --- 3. UPDATES (CPU -> GPU) ---

(defn shape-text [texts global-fsize msdf-atlas]
  (let [atlas (:atlas msdf-atlas) metrics (:metrics msdf-atlas)
        glyphs (reduce (fn [acc glyph] (assoc acc (:unicode glyph) glyph)) {} (:glyphs msdf-atlas))
        atlas-w (or (:width atlas) 1) atlas-h (or (:height atlas) 1) line-h (or (:lineHeight metrics) 1.2)
        res (atom [])]
    (doseq [txt texts]
      (let [{:keys [text x y]} txt fsize (max (or (:size txt) global-fsize) 17.0)
            start-x x !x (atom x) !y (atom y)]
        (doseq [ch (seq text)]
          (let [code (.charCodeAt ch 0)]
            (cond
              (= ch \newline) (do (reset! !x start-x) (reset! !y (+ @!y (* fsize line-h))))
              (= ch \space) (swap! !x + (* fsize 0.25))
              :else (when-let [g (get glyphs code)]
                      (let [pb (:planeBounds g) ab (:atlasBounds g)
                            advance (* fsize (or (:advance g) 0))
                            sl (+ @!x (* fsize (or (:left pb) 0))) sr (+ @!x (* fsize (or (:right pb) 0)))
                            st (- @!y (* fsize (or (:top pb) 0))) sb (- @!y (* fsize (or (:bottom pb) 0)))
                            ul (/ (:left ab) atlas-w) ur (/ (:right ab) atlas-w)
                            vt (- 1.0 (/ (:top ab) atlas-h)) vb (- 1.0 (/ (:bottom ab) atlas-h))]
                        (swap! !x + advance)
                        (swap! res conj {:vertices [[sl sb ul vb fsize] [sr sb ur vb fsize] [sr st ur vt fsize] [sl st ul vt fsize]]}))))))))
    @res))

(defn update-text-data [^js/GPUDevice device renderer-state texts atlas font-size]
  (println "---- update text data")
  (let [shaped (shape-text texts font-size atlas) count (count shaped)
        data (js/Float32Array. (* count 8))]
    (println "CPU Geometry:" count "instances.")
    (loop [i 0 quads shaped]
      (when (seq quads)
        (let [verts (:vertices (first quads))
              v-tl (nth verts 3) v-br (nth verts 1)
              [tl_x tl_y u_min v_min _] v-tl [br_x br_y u_max v_max _] v-br
              base (* i 8)]
          (aset data (+ base 0) tl_x) (aset data (+ base 1) tl_y)
          (aset data (+ base 2) (- br_x tl_x)) (aset data (+ base 3) (- br_y tl_y))
          (aset data (+ base 4) u_min) (aset data (+ base 5) v_min)
          (aset data (+ base 6) u_max) (aset data (+ base 7) v_max)
          (recur (inc i) (next quads)))))
    (.writeBuffer (.-queue device) (:instance-buffer renderer-state) 0 data)
    (let [sizes (js/Float32Array. #js [8.0 64.0 1.0 1.0 1.0 0.0])]
      (.writeBuffer (.-queue device) (:sizes-uniform-buffer renderer-state) 0 sizes))
    (assoc renderer-state :num-instances count)))

(defn update-rects [^js device rect-system rects]
  (let [count (count rects) data (js/Float32Array. (* count 8))]
    (loop [i 0 rs rects]
      (when (seq rs)
        (let [{:keys [x y w h r g b a]} (first rs) base (* i 8)]
          (aset data (+ base 0) x) (aset data (+ base 1) y)
          (aset data (+ base 2) w) (aset data (+ base 3) h)
          (aset data (+ base 4) r) (aset data (+ base 5) g)
          (aset data (+ base 6) b) (aset data (+ base 7) a)
          (recur (inc i) (next rs)))))
    (.writeBuffer (.-queue device) (:instance-buffer rect-system) 0 data)
    (assoc rect-system :num-instances count)))

;; --- 4. RENDER LOOP OPTIMIZED ---

(defn update-camera [^js device camera-buffer ^js floats pan-x pan-y zoom w h]
  (aset floats 0 pan-x)
  (aset floats 1 pan-y)
  (aset floats 2 zoom)
  (aset floats 3 0.0)
  (aset floats 4 w)
  (aset floats 5 h)
  (.writeBuffer (.-queue device) camera-buffer 0 floats))

(defn draw-frame! [^js device ^js context text-sys rect-sys camera-floats pass-descriptor pan-x pan-y w h]
  ;; REMOVED PRINTLN HERE - IT WAS KILLING PERFORMANCE
  
  ;; 1. Update Camera Buffer
  (update-camera device (:camera-uniform-buffer text-sys) camera-floats pan-x pan-y 1.0 w h)
  
  (let [encoder (.createCommandEncoder device)
        texture (.getCurrentTexture context)
        view    (.createView texture)
        
        ;; 2. Reuse the Descriptor Object
        color-attachments (aget pass-descriptor "colorAttachments")
        attachment-0      (aget color-attachments 0)]
    
    (aset attachment-0 "view" view)
    
    (let [pass (.beginRenderPass encoder pass-descriptor)]
      
      (when (and rect-sys (> (:num-instances rect-sys) 0))
        (.setPipeline pass (:pipeline rect-sys))
        (.setBindGroup pass 0 (:bind-group rect-sys))
        (.setVertexBuffer pass 0 (:instance-buffer rect-sys))
        (.draw pass 6 (:num-instances rect-sys)))

      (when (and text-sys (> (:num-instances text-sys) 0))
        (.setPipeline pass (:pipeline text-sys))
        (.setBindGroup pass 0 (:bind-group text-sys))
        (.setVertexBuffer pass 0 (:instance-buffer text-sys))
        (.draw pass 6 (:num-instances text-sys)))

      (.end pass)
      (.submit (.-queue device) #js [(.finish encoder)]))))

