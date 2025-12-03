(ns app.client.webgpu.editor)

;; --- 1. SHADERS (Instanced) ---

(def rect-vertex-shader "
  struct Camera {
      pan: vec2<f32>,
      zoom: f32,
      padding: f32,
      screen_dimensions: vec2<f32>,
  };
  @group(0) @binding(0) var<uniform> camera: Camera;

  struct InstanceInput {
      // Float32x4: x, y, width, height
      @location(0) rect_geometry: vec4<f32>, 
      // Float32x4: r, g, b, a
      @location(1) color: vec4<f32>,         
  };

  struct VertexOutput {
      @builtin(position) position: vec4<f32>,
      @location(0) color: vec4<f32>,
  };

  @vertex
  fn main(@builtin(vertex_index) v_index: u32, instance: InstanceInput) -> VertexOutput {
      var output: VertexOutput;

      // 1. Generate Unit Quad (0.0 to 1.0) based on vertex index (0-5)
      // Triangle List Topology: 
      // Tri 1: (0,0), (1,0), (0,1) -> TL, TR, BL
      // Tri 2: (1,0), (1,1), (0,1) -> TR, BR, BL
      
      var pos = vec2<f32>(0.0, 0.0);
      
      switch(v_index) {
          case 0u: { pos = vec2<f32>(0.0, 0.0); } // TL
          case 1u: { pos = vec2<f32>(1.0, 0.0); } // TR
          case 2u: { pos = vec2<f32>(0.0, 1.0); } // BL
          case 3u: { pos = vec2<f32>(1.0, 0.0); } // TR
          case 4u: { pos = vec2<f32>(1.0, 1.0); } // BR
          default: { pos = vec2<f32>(0.0, 1.0); } // BL (case 5)
      }

      // 2. Scale and Translate based on Instance Data
      let world_x = instance.rect_geometry.x + (pos.x * instance.rect_geometry.z);
      let world_y = instance.rect_geometry.y + (pos.y * instance.rect_geometry.w);
      let world_pos = vec2<f32>(world_x, world_y);

      // 3. Apply Camera
      let zoomed_position = world_pos * camera.zoom;
      let panned_position = zoomed_position + camera.pan;
      let zero_to_two = panned_position / camera.screen_dimensions * 2.0;
      let shifted = zero_to_two - vec2<f32>(1.0, 1.0);

      output.position = vec4<f32>(shifted.x, -shifted.y, 0.0, 1.0);
      output.color = instance.color;

      return output;
  }
")

(def rect-fragment-shader "
  @fragment
  fn main(@location(0) color: vec4<f32>) -> @location(0) vec4<f32> {
      return color;
  }
")


;; --- 2. RECTANGLE SYSTEM (Instanced) ---

(defn init-rect-system [^js/GPUDevice device fformat camera-buffer & {:keys [capacity] :or {capacity 1000}}]
  (let [v-module (.createShaderModule device (clj->js {:code rect-vertex-shader}))
        f-module (.createShaderModule device (clj->js {:code rect-fragment-shader}))

        ;; INSTANCE BUFFER
        ;; Layout: [x, y, w, h] [r, g, b, a] = 8 floats per instance
        instance-buffer (.createBuffer device (clj->js {:size (* capacity 8 4) 
                                                        :usage (bit-or js/GPUBufferUsage.VERTEX js/GPUBufferUsage.COPY_DST)}))

        bg-layout (.createBindGroupLayout device (clj->js {:entries [{:binding 0 
                                                                      :visibility js/GPUShaderStage.VERTEX 
                                                                      :buffer {:type "uniform"}}]}))
        
        pipeline-layout (.createPipelineLayout device (clj->js {:bindGroupLayouts [bg-layout]}))

        pipeline (.createRenderPipeline device
                   (clj->js {:layout pipeline-layout
                             :vertex {:module v-module :entryPoint "main"
                                      :buffers [{:arrayStride 32 ;; 8 floats * 4 bytes
                                                 :stepMode "instance" ;; <--- CRITICAL FOR INSTANCING
                                                 :attributes [{:shaderLocation 0 :offset 0 :format "float32x4"}  ;; rect_geometry (x,y,w,h)
                                                              {:shaderLocation 1 :offset 16 :format "float32x4"}]}]} ;; color
                             :fragment {:module f-module :entryPoint "main"
                                        :targets [{:format fformat
                                                   :blend {:color {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}
                                                           :alpha {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}}}]}
                             :primitive {:topology "triangle-list"}}))

        bind-group (.createBindGroup device (clj->js {:layout bg-layout
                                                      :entries [{:binding 0 :resource {:buffer camera-buffer}}]}))]

    {:pipeline pipeline
     :bind-group bind-group
     :instance-buffer instance-buffer
     :num-instances 0})) ;; Renamed from num-verts


(defn update-rects [^js device rect-system rects]
  (let [count (count rects)
        ;; 8 floats per instance (x, y, w, h, r, g, b, a)
        total-floats (* count 8) 
        data (js/Float32Array. total-floats)]
    
    (loop [i 0, rs rects]
      (when (seq rs)
        (let [{:keys [x y w h r g b a]} (first rs)
              base (* i 8)]
          
          ;; Pack Geometry (x, y, w, h)
          (aset data (+ base 0) x)
          (aset data (+ base 1) y)
          (aset data (+ base 2) w)
          (aset data (+ base 3) h)

          ;; Pack Color (r, g, b, a)
          (aset data (+ base 4) r)
          (aset data (+ base 5) g)
          (aset data (+ base 6) b)
          (aset data (+ base 7) a)

          (recur (inc i) (next rs)))))
    
    (.writeBuffer (.-queue device) (:instance-buffer rect-system) 0 data)
    (assoc rect-system :num-instances count)))


;; --- 3. TEXT SYSTEM (Unchanged - Keeping strict crispness settings) ---

(def vertex-shader-code "
  struct Camera {
      pan: vec2<f32>,
      zoom: f32,
      padding: f32,
      screen_dimensions: vec2<f32>,
  };
  @group(0) @binding(2) var<uniform> camera: Camera;

  struct InstanceInput {
      // x, y, width, height  <-- CHANGED
      @location(0) rect: vec4<f32>, 
      // u_min, v_min, u_max, v_max
      @location(1) uv_bounds: vec4<f32>, 
  };

  struct VertexOutput {
      @builtin(position) position: vec4<f32>,
      @location(0) uv: vec2<f32>,
      @location(1) v_visual_size: f32,
  };

  @vertex
  fn main(@builtin(vertex_index) v_index: u32, instance: InstanceInput) -> VertexOutput {
      var output: VertexOutput;

      // 1. Generate Unit Quad (0.0 -> 1.0)
      var pos = vec2<f32>(0.0, 0.0);
      switch(v_index) {
          case 0u: { pos = vec2<f32>(0.0, 0.0); } // TL
          case 1u: { pos = vec2<f32>(1.0, 0.0); } // TR
          case 2u: { pos = vec2<f32>(0.0, 1.0); } // BL
          case 3u: { pos = vec2<f32>(1.0, 0.0); } // TR
          case 4u: { pos = vec2<f32>(1.0, 1.0); } // BR
          default: { pos = vec2<f32>(0.0, 1.0); } // BL
      }

      // 2. Map Unit Quad to World Position using Width/Height
      let world_x = instance.rect.x + (pos.x * instance.rect.z); // x + width
      let world_y = instance.rect.y + (pos.y * instance.rect.w); // y + height
      let world_pos = vec2<f32>(world_x, world_y);

      // 3. UV Mapping
      let u = mix(instance.uv_bounds.x, instance.uv_bounds.z, pos.x);
      let v = mix(instance.uv_bounds.y, instance.uv_bounds.w, pos.y);
      output.uv = vec2<f32>(u, v);

      // 4. Camera Transform
      let zoomed_position = world_pos * camera.zoom;
      let panned_position = zoomed_position + camera.pan;
      let zero_to_two = panned_position / camera.screen_dimensions * 2.0;
      let shifted = zero_to_two - vec2<f32>(1.0, 1.0);

      output.position = vec4<f32>(shifted.x, -shifted.y, 0.0, 1.0);
      
      // We pass the larger dimension as visual size to ensure enough pxRange
      output.v_visual_size = max(instance.rect.z, instance.rect.w) * camera.zoom;

      return output;
  }
")

(def msdf-fragment-shader-code "
  @group(0) @binding(0) var sampler0: sampler;
  @group(0) @binding(1) var texture0: texture_2d<f32>;

  struct Sizing {
     pxRange: f32, 
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

(defn init-text-system
  [^js/GPUDevice device fformat atlas font-bitmap & {:keys [initial-capacity] :or {initial-capacity 10000}}]
  (println "INITIALIZING INSTANCED TEXT SYSTEM")

  (let [vertex-module    (.createShaderModule device (clj->js {:code vertex-shader-code}))
        fragment-module  (.createShaderModule device (clj->js {:code msdf-fragment-shader-code}))

        texture (.createTexture device
                  (clj->js {:size {:width (.-width font-bitmap) 
                                   :height (.-height font-bitmap) :depthOrArrayLayers 1}
                            :format "rgba8unorm"
                            :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                           js/GPUTextureUsage.TEXTURE_BINDING 
                                           js/GPUTextureUsage.COPY_DST)}))

        _ (.copyExternalImageToTexture (.-queue device)
                                       (clj->js {:source font-bitmap})
                                       (clj->js {:texture texture :origin {:x 0 :y 0 :z 0}})
                                       (clj->js {:width (.-width font-bitmap) 
                                                 :height (.-height font-bitmap) :depthOrArrayLayers 1}))

        sampler (.createSampler device (clj->js {:minFilter "linear" :magFilter "linear" :mipmapFilter "linear"}))

        ;; INSTANCE BUFFER: 8 floats per char (32 bytes)
        instance-buffer (.createBuffer device
                          (clj->js {:size (* initial-capacity 32)
                                    :usage (bit-or js/GPUBufferUsage.VERTEX js/GPUBufferUsage.COPY_DST)}))
        
        camera-buffer (.createBuffer device (clj->js {:size 24 :usage (bit-or js/GPUBufferUsage.UNIFORM js/GPUBufferUsage.COPY_DST)}))
        sizes-buffer  (.createBuffer device (clj->js {:size 24 :usage (bit-or js/GPUBufferUsage.UNIFORM js/GPUBufferUsage.COPY_DST)}))

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
                                      :buffers [{:arrayStride 32 ;; 8 floats
                                                 :stepMode "instance"
                                                 :attributes [{:shaderLocation 0 :offset 0  :format "float32x4"}   ;; pos_scale (x,y,size,pad)
                                                              {:shaderLocation 1 :offset 16 :format "float32x4"}]}]} ;; uv_bounds (u1,v1,u2,v2)
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
     :instance-buffer instance-buffer ;; CHANGED: No vertex/index buffer
     :num-instances 0}))

(defn shape-text [texts global-fsize msdf-atlas]
  (let [atlas       (:atlas msdf-atlas)
        metrics     (:metrics msdf-atlas)
        glyphs      (reduce (fn [acc glyph] (assoc acc (:unicode glyph) glyph)) {} (:glyphs msdf-atlas))
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

                        screen-left   (+ @!x (* fsize pb-left))
                        screen-right  (+ @!x (* fsize pb-right)) 
                        screen-top    (- @!y (* fsize pb-top))
                        screen-bottom (- @!y (* fsize pb-bottom))

                        u-left   (/ (:left atlas-bounds) atlas-width)
                        u-right  (/ (:right atlas-bounds) atlas-width)
                        v-top    (- 1.0 (/ (:top atlas-bounds) atlas-height))
                        v-bottom (- 1.0 (/ (:bottom atlas-bounds) atlas-height))

                        v-bl [screen-left  screen-bottom u-left  v-bottom fsize]
                        v-br [screen-right screen-bottom u-right v-bottom fsize]
                        v-tr [screen-right screen-top    u-right v-top    fsize]
                        v-tl [screen-left  screen-top    u-left  v-top    fsize]]

                    (reset! !x (+ @!x advance))
                    (swap! res conj {:vertices [v-bl v-br v-tr v-tl]})))))))))
    @res))



(defn update-text-data [^js/GPUDevice device renderer-state texts atlas font-size]
  (println "---- update text data")
  (let [shaped-texts (shape-text texts font-size atlas)
        total-chars  (count shaped-texts)
        total-floats (* total-chars 8)
        data         (js/Float32Array. total-floats)]

    (println "CPU Geometry:" total-chars "instances.")
    (println "⚡ GPU Upload | Chars:" total-chars " | First Quad:" (first shaped-texts))

    (loop [i 0
           quads shaped-texts]
      (when (seq quads)
        (let [quad (first quads)
              verts (:vertices quad)
              
              ;; Shape-text order: BL, BR, TR, TL
              ;; We need TL (Top-Left) and BR (Bottom-Right) to calculate dimensions
              v-tl  (nth verts 3)
              v-br  (nth verts 1)
              
              [tl_x tl_y u_min v_min _] v-tl
              [br_x br_y u_max v_max _] v-br

              base (* i 8)
              
              ;; Calculate actual dimensions from the shaped corners
              ;; NOTE: Check your Y coordinate direction. 
              ;; In shape-text, top is usually "higher" visually but might be lower value?
              ;; Let's assume standard: width = right - left, height = bottom - top
              w (- br_x tl_x)
              h (- br_y tl_y)] 

          ;; 1. Instance Geometry (x, y, width, height)
          (aset data (+ base 0) tl_x)
          (aset data (+ base 1) tl_y)
          (aset data (+ base 2) w)
          (aset data (+ base 3) h)

          ;; 2. UV Bounds (u_min, v_min, u_max, v_max)
          (aset data (+ base 4) u_min)
          (aset data (+ base 5) v_min)
          (aset data (+ base 6) u_max)
          (aset data (+ base 7) v_max)

          (recur (inc i) (next quads)))))

    (.writeBuffer (.-queue device) (:instance-buffer renderer-state) 0 data)

    ;; Update Sizes (Unchanged)
    (let [sizes-data (js/Float32Array. (clj->js [8.0 64.0 1.0 1.0 1.0 0.0]))]
      (.writeBuffer (.-queue device) (:sizes-uniform-buffer renderer-state) 0 sizes-data))

    (assoc renderer-state :num-instances total-chars)))


(defn update-camera [^js device camera-buffer camera-state]
  ;; --- ADD LOG START ---
  (println "🎥 Camera Update | Pan:" (:pan-x camera-state) (:pan-y camera-state) 
           "| Zoom:" (:zoom camera-state) 
           "| Screen:" (:width camera-state) (:height camera-state))
  ;; --- ADD LOG END ---

  (let [camera-array (js/Float32Array. (clj->js [(:pan-x camera-state) 
                                                 (:pan-y camera-state)
                                                 (:zoom camera-state) 
                                                 0.0
                                                 (:width camera-state) 
                                                 (:height camera-state)]))]
    (.writeBuffer (.-queue device) camera-buffer 0 camera-array)))


(defn draw-rects [^js pass ^js rect-system]
  (when (> (:num-instances rect-system) 0)
    (.setPipeline pass (:pipeline rect-system))
    (.setBindGroup pass 0 (:bind-group rect-system))
    (.setVertexBuffer pass 0 (:instance-buffer rect-system))
    (.draw pass 6 (:num-instances rect-system))))


(defn draw-text-pass [^js pass ^js text-sys] ;; REMOVED device/camera args
  (when (> (:num-instances text-sys) 0)
    (.setPipeline pass (:pipeline text-sys))
    (.setBindGroup pass 0 (:bind-group text-sys))
    (.setVertexBuffer pass 0 (:instance-buffer text-sys))
    (.draw pass 6 (:num-instances text-sys))))



(defn draw-frame! [^js device ^js context text-sys rect-sys camera-state]
  (update-camera device (:camera-uniform-buffer text-sys) camera-state)
  (let [encoder (.createCommandEncoder device)
        texture (.getCurrentTexture context)
        view    (.createView texture)
        pass-desc (clj->js {:colorAttachments [{:view view
                                                :clearValue {:r 0.2 :g 0 :b 0 :a 1}
                                                :loadOp "clear"
                                                :storeOp "store"}]})
        
        pass (.beginRenderPass encoder pass-desc)]

    (draw-rects pass rect-sys)
    (draw-text-pass pass text-sys )

    (.end pass)
    (.submit (.-queue device) (clj->js [(.finish encoder)]))))

(defn create-editor-state [{:keys [device format atlas bitmap]}]
     (let [text-sys (init-text-system device format atlas bitmap :capacity 6e6)
           rect-sys (init-rect-system device format (:camera-uniform-buffer text-sys))]
       {:text-sys text-sys 
        :rect-sys rect-sys}))
