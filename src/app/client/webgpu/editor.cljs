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

;; Updated hit-test: clamps column to actual line length
(defn hit-test [x y font-size start-x start-y line-h line-lengths]
  (let [char-w     (* font-size 0.6)
        rel-x      (- x start-x)
        rel-y      (- y start-y)
        line-idx   (max 0 (Math/floor (/ rel-y line-h)))
        ;; Clamp line index to valid range
        line-idx   (min line-idx (max 0 (dec (count line-lengths))))
        ;; Get actual line length, default to 0 for empty lines
        line-len   (get line-lengths line-idx 0)
        ;; Clamp column to [0, line-length]
        col-idx    (-> (/ rel-x char-w)
                       (Math/round)
                       (max 0)
                       (min line-len))]
    {:line line-idx :col col-idx}))

;; Updated calculate-selection-rects: uses actual line lengths
(defn calculate-selection-rects [sel-start sel-end font-size start-x start-y line-h line-lengths]
  (if (and sel-start sel-end)
    (let [;; Normalize selection direction
          [start end] (if (or (> (:line sel-start) (:line sel-end))
                              (and (= (:line sel-start) (:line sel-end))
                                   (> (:col sel-start) (:col sel-end))))
                        [sel-end sel-start]
                        [sel-start sel-end])
          char-w (* font-size 0.6)
          ;; Selection highlight color
          r 0.2 g 0.4 b 0.9 a 0.5]

      (loop [curr-line (:line start)
             rects     []]
        (if (> curr-line (:line end))
          rects
          (let [;; Get actual length of this line
                line-len   (get line-lengths curr-line 0)
                is-first?  (= curr-line (:line start))
                is-last?   (= curr-line (:line end))
                
                ;; Column range for this line
                col-start  (if is-first? (:col start) 0)
                col-end    (if is-last? 
                             (:col end) 
                             line-len)  ;; Use actual line length, not hardcoded!
                
                ;; Only create rect if there's content to highlight
                width-chars (- col-end col-start)]
            
            (if (and (> width-chars 0) (> line-len 0))
              ;; Create rect for this line's selection
              (let [px-x (+ start-x (* col-start char-w))
                    px-y (+ start-y (* curr-line line-h))
                    px-w (* width-chars char-w)
                    px-h line-h]
                (recur (inc curr-line)
                       (conj rects {:x px-x :y px-y :w px-w :h px-h
                                    :r r :g g :b b :a a})))
              ;; Skip empty lines or zero-width selections
              (recur (inc curr-line) rects))))))
    []))

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
        
        camera-floats (js/Float32Array. 6)
        
        pass-descriptor (clj->js {:colorAttachments [{:view nil
                                                      :clearValue {:r 0.0 :g 0.0 :b 0.0 :a 0.0}
                                                      :loadOp "clear"
                                                      :storeOp "store"}]})]
    
    {:text-sys text-sys 
     :rect-sys rect-sys
     :camera-floats camera-floats
     :pass-descriptor pass-descriptor}))

;; --- 3. UPDATES (CPU -> GPU) ---
(defn shape-text [texts global-fsize msdf-atlas]
  (let [atlas (:atlas msdf-atlas) metrics (:metrics msdf-atlas)
        glyphs (reduce (fn [acc glyph] (assoc acc (:unicode glyph) glyph)) {} (:glyphs msdf-atlas))
        atlas-w (or (:width atlas) 1) atlas-h (or (:height atlas) 1) line-h (or (:lineHeight metrics) 1.2)
        res (atom [])]
    (doseq [txt texts]
      (let [{:keys [text x y]} txt 
            fsize (or (:size txt) global-fsize)
            start-x x !x (atom x) !y (atom y)]
        (doseq [ch (seq text)]
          (let [code (.charCodeAt ch 0)]
            (cond
              (= ch \newline) (do (reset! !x start-x) (reset! !y (+ @!y (* fsize line-h))))
              (= ch \space) (swap! !x + (* fsize 0.25))
              :else (when-let [g (get glyphs code)]
                      (let [pb (:planeBounds g)
                            ab (:atlasBounds g)
                            advance (* fsize (or (:advance g) 0))
                            sl (+ @!x (* fsize (or (:left pb) 0)))
                            sr (+ @!x (* fsize (or (:right pb) 0)))
                            st (- @!y (* fsize (or (:top pb) 0)))
                            sb (- @!y (* fsize (or (:bottom pb) 0)))
                            ul (/ (:left ab) atlas-w)
                            ur (/ (:right ab) atlas-w)
                            vt (- 1.0 (/ (:top ab) atlas-h))
                            vb (- 1.0 (/ (:bottom ab) atlas-h))]
                        (swap! !x + advance)
                        (swap! res conj {:vertices [[sl sb ul vb fsize] [sr sb ur vb fsize] [sr st ur vt fsize] [sl st ul vt fsize]]}))))))))
    @res))


(defn update-text-data [^js/GPUDevice device renderer-state texts atlas font-size]
  (let [
        shaped-lines (mapv (fn [tokens-in-line]
                             (let [quads (shape-text tokens-in-line font-size atlas)]
                               {:quads quads :count (count quads)}))
                           texts)
        
        total-instances (reduce + (map :count shaped-lines))
        total-instances (max total-instances 1) 
        data (js/Float32Array. (* total-instances 8))
        
        line-offsets (loop [lines shaped-lines
                            current-idx 0
                            offsets []]
                       (if (seq lines)
                         (let [cnt (:count (first lines))]
                           (recur (next lines) (+ current-idx cnt) (conj offsets current-idx)))
                         (vec offsets)))

        current-buffer (:instance-buffer renderer-state)
        required-size (.-byteLength data)
        current-size (.-size ^js current-buffer) 
        
        needs-resize? (> required-size current-size)

        new-buffer (if needs-resize?
                     (do
                       (.destroy ^js current-buffer) 
                       (.createBuffer device (clj->js {:size required-size
                                                       :usage (bit-or js/GPUBufferUsage.STORAGE 
                                                                      js/GPUBufferUsage.COPY_DST)})))
                     current-buffer)

        new-bind-group (if needs-resize?
                         (.createBindGroup device
                           (clj->js {:layout (:bind-group-layout renderer-state)
                                     :entries [{:binding 0
                                                :resource {:buffer new-buffer}}
                                               {:binding 1
                                                :resource {:buffer (:sizes-uniform-buffer renderer-state)}}]}))
                         (:bind-group renderer-state))]

    (loop [lines shaped-lines global-i 0]
      (when (seq lines)
        (let [quads (:quads (first lines))]
          (loop [q quads sub-i 0]
            (when (seq q)
              (let [verts (:vertices (first q))
                    v-tl (nth verts 3) v-br (nth verts 1)
                    [tl_x tl_y u_min v_min _] v-tl [br_x br_y u_max v_max _] v-br
                    base (* (+ global-i sub-i) 8)]
                (aset data (+ base 0) tl_x) (aset data (+ base 1) tl_y)
                (aset data (+ base 2) (- br_x tl_x)) (aset data (+ base 3) (- br_y tl_y))
                (aset data (+ base 4) u_min) (aset data (+ base 5) v_min)
                (aset data (+ base 6) u_max) (aset data (+ base 7) v_max)
                (recur (next q) (inc sub-i)))))
          (recur (next lines) (+ global-i (:count (first lines)))))))

    (.writeBuffer (.-queue device) new-buffer 0 data)
    
    (let [sizes (js/Float32Array. #js [8.0 64.0 1.0 1.0 1.0 0.0])]
      (.writeBuffer (.-queue device) (:sizes-uniform-buffer renderer-state) 0 sizes))

    (assoc renderer-state 
           :instance-buffer new-buffer
           :bind-group new-bind-group 
           :num-instances total-instances
           :line-offsets line-offsets
           :line-height (* font-size 1.2))))


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


(defn update-camera [^js device camera-buffer ^js floats pan-x pan-y zoom w h]
  (aset floats 0 pan-x)
  (aset floats 1 pan-y)
  (aset floats 2 zoom)
  (aset floats 3 0.0)
  (aset floats 4 w)
  (aset floats 5 h)
  (.writeBuffer (.-queue device) camera-buffer 0 floats))


(defn draw-frame! [^js device ^js context text-sys rect-sys camera-floats _ignored_pass_descriptor pan-x pan-y w h]
  (update-camera device (:camera-uniform-buffer text-sys) camera-floats pan-x pan-y 1.0 w h)
  
  (let [encoder (.createCommandEncoder device)
        texture (.getCurrentTexture context)
        view    (.createView texture)
        
        pass-descriptor (clj->js 
                          {:colorAttachments [{:view view
                                               :clearValue {:r 0.0 :g 0.0 :b 0.0 :a 1.0}
                                               :loadOp "clear"
                                               :storeOp "store"}]})
        
        pass (.beginRenderPass encoder pass-descriptor)]

    (when (and rect-sys (> (:num-instances rect-sys) 0))
      (.setPipeline pass (:pipeline rect-sys))
      (.setBindGroup pass 0 (:bind-group rect-sys))
      (.setVertexBuffer pass 0 (:instance-buffer rect-sys))
      (.draw pass 6 (:num-instances rect-sys)))

    (when (and text-sys (> (:num-instances text-sys) 0))
      (.setPipeline pass (:pipeline text-sys))
      (.setBindGroup pass 0 (:bind-group text-sys))
      (.setVertexBuffer pass 0 (:instance-buffer text-sys))
      
      (let [line-offsets (:line-offsets text-sys)
            line-h       (:line-height text-sys)
            total-lines  (count line-offsets)
            
            scroll-y     (- pan-y) 
            start-line   (max 0 (Math/floor (/ scroll-y line-h)))
            end-line     (min total-lines (+ (Math/ceil (/ (+ scroll-y h) line-h)) 2))]
        
        (when (< start-line end-line)
          (let [start-inst (nth line-offsets start-line)
                end-inst   (if (< end-line total-lines)
                             (nth line-offsets end-line)
                             (:num-instances text-sys))
                draw-count (- end-inst start-inst)]
            
            (.draw pass 6 draw-count 0 start-inst)))))

    (.end pass)
    (.submit (.-queue device) #js [(.finish encoder)])))
