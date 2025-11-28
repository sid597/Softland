(ns app.client.webgpu.editor)

;; --- 1. SHADERS (Fixed Derivative Logic) ---

(def vertex-shader-code "
  struct Camera {
                 pan: vec2<f32>,
                 zoom: f32,
                 padding: f32,
                 screen_dimensions: vec2<f32>,
                 };

  struct VertexInput {
                      @location(0) position: vec2<f32>,
                      @location(1) uv: vec2<f32>,
                      @location(2) msdf_scale: f32, 
                      };

  struct VertexOutput {
                       @builtin(position) position: vec4<f32>,
                       @location(0) uv: vec2<f32>,
                       @location(1) v_visual_size: f32,
                       };

  @group(0) @binding(2) var<uniform> camera: Camera;

  @vertex
  fn main(input: VertexInput) -> VertexOutput {
       var output: VertexOutput;

       let zoomed_position = input.position * camera.zoom;
       let panned_position = zoomed_position + camera.pan;
       let zero_to_two = panned_position / camera.screen_dimensions * 2.0;
       let shifted = zero_to_two - vec2<f32>(1.0, 1.0);

       output.position = vec4<f32>(shifted.x, -shifted.y, 0.0, 1.0);
       output.uv = input.uv;
       output.v_visual_size = input.msdf_scale * camera.zoom;

       return output;
  }")

(def msdf-fragment-shader-code "
  @group(0) @binding(0) var sampler0: sampler;
  @group(0) @binding(1) var texture0: texture_2d<f32>;

  struct Sizing {
     pxRange: f32, // Ensure this is set to 8.0 in your buffer update!
     atlasEmSize: f32,
     color_r: f32,
     color_g: f32,
     color_b: f32,
     padding: f32,
  };
  @group(0) @binding(3) var<uniform> params: Sizing;

  fn median(a: f32, b: f32, c: f32) -> f32 {
       return max(min(a, b), min(max(a, b), c));
  }

  @fragment
  fn main(
          @location(0) uv: vec2<f32>,
          @location(1) visual_size: f32
          ) -> @location(0) vec4<f32> {
       
       let msd = textureSample(texture0, sampler0, uv).rgb;
       let sd = median(msd.r, msd.g, msd.b);

       // 1. Calculate Screen Pixel Range
       // This tells us how many screen pixels the soft edge covers.
       let screenPxRange = params.pxRange * (visual_size / params.atlasEmSize);

       // 2. The 'M' Saver: Adaptive Thickening
       // If visual_size is small (< 20px), we add weight.
       // The 0.2 factor is aggressive. It forces the 'm' legs to stay distinct.
       let size_factor = clamp(1.0 - (visual_size / 24.0), 0.0, 1.0);
       let weight_bias = size_factor * 0.2; 

       // 3. Distance Calculation
       // We center around 0.5, add our bias.
       let dist = sd - 0.5 + weight_bias;
       
       // 4. Render
       // using fwidth ensures the edge is sharp regardless of zoom
       let opacity = clamp(dist * screenPxRange + 0.5, 0.0, 1.0);

       return vec4<f32>(params.color_r, params.color_g, params.color_b, opacity);
  }")
;; --- 2. SYSTEM SETUP ---

(defn init-text-system
  [^js/GPUDevice device fformat atlas font-bitmap & {:keys [initial-capacity] :or {initial-capacity 10000}}]
  (println "INITIALIZING CRISP TEXT SYSTEM (FWIDTH FIX)")

  (let [vertex-module    (.createShaderModule device (clj->js {:code vertex-shader-code}))
        fragment-module (.createShaderModule device (clj->js {:code msdf-fragment-shader-code}))

        texture (.createTexture device
                                (clj->js {:size {:width (.-width font-bitmap) 
                                                 :height (.-height font-bitmap) 
                                                 :depthOrArrayLayers 1}
                                          :format "rgba8unorm"
                                          :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                                         js/GPUTextureUsage.TEXTURE_BINDING 
                                                         js/GPUTextureUsage.COPY_DST)}))

        _ (.copyExternalImageToTexture (.-queue device)
                                       (clj->js {:source font-bitmap})
                                       (clj->js {:texture texture :origin {:x 0 :y 0 :z 0}})
                                       (clj->js {:width (.-width font-bitmap) 
                                                 :height (.-height font-bitmap) 
                                                 :depthOrArrayLayers 1}))

        ;; SAMPLER: Must be Linear for fwidth to work correctly
        sampler (.createSampler device (clj->js {:minFilter "linear" 
                                                 :magFilter "linear" 
                                                 :mipmapFilter "linear"}))

        vertex-buffer (.createBuffer device
                                     (clj->js {:size (* initial-capacity 4 20)
                                               :usage (bit-or js/GPUBufferUsage.VERTEX js/GPUBufferUsage.COPY_DST)}))
        index-buffer (.createBuffer device
                                    (clj->js {:size (* initial-capacity 6 2)
                                              :usage (bit-or js/GPUBufferUsage.INDEX js/GPUBufferUsage.COPY_DST)}))
        camera-buffer (.createBuffer device
                                     (clj->js {:size 24 
                                               :usage (bit-or js/GPUBufferUsage.UNIFORM js/GPUBufferUsage.COPY_DST)}))
        
        sizes-buffer (.createBuffer device
                                    (clj->js {:size 24 
                                              :usage (bit-or js/GPUBufferUsage.UNIFORM js/GPUBufferUsage.COPY_DST)}))

        bind-group-layout (.createBindGroupLayout device
                                                  (clj->js {:entries [{:binding 0 :visibility js/GPUShaderStage.FRAGMENT :sampler {:type "filtering"}}
                                                                      {:binding 1 :visibility js/GPUShaderStage.FRAGMENT :texture {:sampleType "float"}}
                                                                      {:binding 2 :visibility js/GPUShaderStage.VERTEX   :buffer {:type "uniform"}}
                                                                      {:binding 3 :visibility js/GPUShaderStage.FRAGMENT :buffer {:type "uniform"}}]}))

        pipeline-layout (.createPipelineLayout device (clj->js {:bindGroupLayouts [bind-group-layout]}))

        pipeline (.createRenderPipeline device
                                        (clj->js {:layout pipeline-layout
                                                  :vertex {:module vertex-module
                                                           :entryPoint "main"
                                                           :buffers [{:arrayStride 20 
                                                                      :attributes [{:shaderLocation 0 :offset 0 :format "float32x2"}   
                                                                                   {:shaderLocation 1 :offset 8 :format "float32x2"}   
                                                                                   {:shaderLocation 2 :offset 16 :format "float32"}]}]} 
                                                  :primitive {:topology "triangle-list" :cullMode "none"} 
                                                  :fragment {:module fragment-module
                                                             :entryPoint "main"
                                                             :targets [{:format fformat
                                                                        :blend {:color {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}
                                                                                :alpha {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}}}]}}))

        bind-group (.createBindGroup device
                                     (clj->js {:layout bind-group-layout
                                               :entries [{:binding 0 :resource sampler}
                                                         {:binding 1 :resource (.createView texture)}
                                                         {:binding 2 :resource {:buffer camera-buffer}}
                                                         {:binding 3 :resource {:buffer sizes-buffer}}]}))]

    {:pipeline pipeline
     :bind-group bind-group
     :camera-uniform-buffer camera-buffer
     :sizes-uniform-buffer sizes-buffer
     :vertex-buffer vertex-buffer
     :index-buffer index-buffer
     :num-indices 0}))

;; --- 3. GEOMETRY GENERATION ---

(defn shape-text [texts global-fsize msdf-atlas]
  (let [atlas        (:atlas msdf-atlas)
        metrics      (:metrics msdf-atlas)
        glyphs       (reduce (fn [acc glyph] (assoc acc (:unicode glyph) glyph)) {} (:glyphs msdf-atlas))
        atlas-width  (or (:width atlas) 1)
        atlas-height (or (:height atlas) 1)
        line-height  (or (:lineHeight metrics) 1.2)
        res          (atom [])]

    (doseq [txt texts]
      (let [{:keys [text x y]} txt
            raw-size   (or (:size txt) global-fsize)
            fsize (max raw-size 17.0)
            start-x x
            !x      (atom x)
            !y      (atom y)]
        (doseq [ch (seq text)]
          (let [codepoint (.charCodeAt ch 0)]
            (cond
              (= ch \newline) 
              (do
                (reset! !x start-x)
                (reset! !y (+ @!y (* fsize line-height))))
              
              (= ch \space)
              ;; Try to get space advance from atlas, fallback to 0.25 em
              (let [space-glyph (get glyphs 32)
                    space-advance (if space-glyph 
                                    (* fsize (or (:advance space-glyph) 0.25))
                                    (* fsize 0.25))]
                (reset! !x (+ @!x space-advance)))
              
              :else
              (let [glyph (get glyphs codepoint)]
                (when glyph
                  (let [plane-bounds (:planeBounds glyph)
                        atlas-bounds (:atlasBounds glyph)

                        pb-left   (or (:left plane-bounds) 0)
                        pb-right  (or (:right plane-bounds) 0)
                        pb-top    (or (:top plane-bounds) 0)
                        pb-bottom (or (:bottom plane-bounds) 0)

                        advance (* fsize (or (:advance glyph) 0))

                        ;; Screen positions - cursor is at @!x, @!y (baseline)
                        ;; pb-left/right are relative to cursor in em units
                        screen-left   (+ @!x (* fsize pb-left))
                        screen-right  (+ @!x (* fsize pb-right))  ;; FIX: directly use pb-right
                        screen-top    (- @!y (* fsize pb-top))
                        screen-bottom (- @!y (* fsize pb-bottom))

                        ;; Atlas UVs (flip V for WebGPU top-left origin)
                        u-left   (/ (:left atlas-bounds) atlas-width)
                        u-right  (/ (:right atlas-bounds) atlas-width)
                        v-top    (- 1.0 (/ (:top atlas-bounds) atlas-height))
                        v-bottom (- 1.0 (/ (:bottom atlas-bounds) atlas-height))

                        ;; Vertices: [x, y, u, v, size]
                        v-bl [screen-left  screen-bottom u-left  v-bottom fsize]
                        v-br [screen-right screen-bottom u-right v-bottom fsize]
                        v-tr [screen-right screen-top    u-right v-top    fsize]
                        v-tl [screen-left  screen-top    u-left  v-top    fsize]]

                    (reset! !x (+ @!x advance))
                    (swap! res conj {:vertices [v-bl v-br v-tr v-tl]})))))))))
    @res))

(defn update-text-data [^js/GPUDevice device renderer-state texts atlas font-size]
  (let [shaped-texts (shape-text texts font-size atlas)
        total-quads  (count shaped-texts)
        total-verts  (* total-quads 4)
        total-indices (* total-quads 6)
        
        v-data (js/Float32Array. (* total-verts 5)) 
        i-data (js/Uint16Array. total-indices)]

    (println "CPU Geometry:" total-quads "quads," total-indices "indices.")

    (loop [i 0
           quads shaped-texts]
      (when (seq quads)
        (let [quad (first quads)
              verts (:vertices quad)
              v-base (* i 20) 
              i-base (* i 6)
              vert-idx-base (* i 4)]

          (dotimes [v 4]
            (let [[x y u v_coord scale] (nth verts v)
                  offset (+ v-base (* v 5))]
              (aset v-data offset x)
              (aset v-data (+ offset 1) y)
              (aset v-data (+ offset 2) u)
              (aset v-data (+ offset 3) v_coord)
              (aset v-data (+ offset 4) scale)))

          (aset i-data (+ i-base 0) (+ vert-idx-base 0))
          (aset i-data (+ i-base 1) (+ vert-idx-base 1))
          (aset i-data (+ i-base 2) (+ vert-idx-base 2))
          (aset i-data (+ i-base 3) (+ vert-idx-base 0))
          (aset i-data (+ i-base 4) (+ vert-idx-base 2))
          (aset i-data (+ i-base 5) (+ vert-idx-base 3))

          (recur (inc i) (next quads)))))

    (.writeBuffer (.-queue device) (:vertex-buffer renderer-state) 0 v-data)
    (.writeBuffer (.-queue device) (:index-buffer renderer-state) 0 i-data)

    ;; UPDATE SIZES:
    ;; We defaults to the NEW recommended settings if you regenerate (42.0/4.0).
    ;; If you are still using the old atlas, these values effectively don't matter 
    ;; for the fwidth calculation, so it is safe to set them to the "goal" values.
    (let [sizes-data (js/Float32Array. (clj->js [8.0 64.0 1.0 1.0 1.0 0.0]))]
      (.writeBuffer (.-queue device) (:sizes-uniform-buffer renderer-state) 0 sizes-data))

    (assoc renderer-state :num-indices total-indices)))

(defn draw-text [^js/GPUDevice device ^js/GPUCanvasContext context renderer-state camera-state]
  (when (> (:num-indices renderer-state) 0)
    (let [camera-array (js/Float32Array. (clj->js [(:pan-x camera-state) 
                                                   (:pan-y camera-state)
                                                   (:zoom camera-state) 
                                                   0.0
                                                   (:width camera-state) 
                                                   (:height camera-state)]))

          encoder (.createCommandEncoder device)
          view    (.createView (.getCurrentTexture context))

          render-pass (.beginRenderPass encoder
                                        (clj->js {:colorAttachments [{:view view
                                                                      :clearValue {:r 0.0 :g 0.0 :b 0.0 :a 1.0} 
                                                                      :loadOp "clear"
                                                                      :storeOp "store"}]}))]

      (.writeBuffer (.-queue device) (:camera-uniform-buffer renderer-state) 0 camera-array)

      (.setPipeline render-pass (:pipeline renderer-state))
      (.setBindGroup render-pass 0 (:bind-group renderer-state))
      (.setVertexBuffer render-pass 0 (:vertex-buffer renderer-state))
      (.setIndexBuffer render-pass (:index-buffer renderer-state) "uint16")
      (.drawIndexed render-pass (:num-indices renderer-state))
      (.end render-pass)

      (.submit (.-queue device) [(.finish encoder)]))))

