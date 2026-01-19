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
  struct Sizing { pxRange: f32, atlasEmSize: f32, color_r: f32, color_g: f32, color_b: f32, sharpness: f32, };
  @group(0) @binding(3) var<uniform> params: Sizing;
  fn median(a: f32, b: f32, c: f32) -> f32 { return max(min(a, b), min(max(a, b), c)); }

  @fragment
  fn main(@location(0) uv: vec2<f32>, @location(1) visual_size: f32) -> @location(0) vec4<f32> {
       let msd = textureSample(texture0, sampler0, uv).rgb;
       let sd = median(msd.r, msd.g, msd.b);
       let screenPxRange = max(params.pxRange * (visual_size / params.atlasEmSize), 1.0);
       // sharpness: negative = sharper edges, positive = softer edges, 0 = standard MSDF
       let dist = sd - 0.5 + params.sharpness;
       let opacity = clamp(dist * screenPxRange + 0.5, 0.0, 1.0);
       return vec4<f32>(params.color_r, params.color_g, params.color_b, opacity);
  }")

;; Calculate bracket highlight rectangles
(defn calculate-bracket-rects [bracket-match font-size start-x start-y line-h]
  (when bracket-match
    (let [char-w (* font-size 0.56)
          {:keys [open close]} bracket-match
          make-rect (fn [{:keys [line col]}]
                      {:x (+ start-x (* col char-w))
                       :y (+ start-y (* line line-h))
                       :w char-w
                       :h line-h
                       ;; Golden/yellow highlight for matching brackets
                       :r 0.8 :g 0.6 :b 0.2 :a 0.4})]
      [(make-rect open) (make-rect close)])))

;; Updated hit-test: clamps column to actual line length
(defn hit-test [x y font-size start-x start-y line-h line-lengths]
  (let [char-w     (* font-size 0.56)
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
    {:pipeline pipeline :bind-group bind-group :bind-group-layout bg-layout :camera-uniform-buffer camera-buffer :sizes-uniform-buffer sizes-buffer :instance-buffer instance-buffer :num-instances 0}))

(defn update-font-texture [^js/GPUDevice device renderer-state font-bitmap]
  (let [texture (.createTexture device (clj->js {:size {:width (.-width font-bitmap) :height (.-height font-bitmap) :depthOrArrayLayers 1}
                                                 :format "rgba8unorm" :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT js/GPUTextureUsage.TEXTURE_BINDING js/GPUTextureUsage.COPY_DST)}))
        _ (.copyExternalImageToTexture (.-queue device) (clj->js {:source font-bitmap}) (clj->js {:texture texture}) (clj->js {:width (.-width font-bitmap) :height (.-height font-bitmap)}))
        sampler (.createSampler device (clj->js {:minFilter "linear" :magFilter "linear" :mipmapFilter "linear"}))
        
        new-bind-group (.createBindGroup device (clj->js {:layout (:bind-group-layout renderer-state)
                                                          :entries [{:binding 0 :resource sampler} 
                                                                    {:binding 1 :resource (.createView texture)}
                                                                    {:binding 2 :resource {:buffer (:camera-uniform-buffer renderer-state)}} 
                                                                    {:binding 3 :resource {:buffer (:sizes-uniform-buffer renderer-state)}}]}))]
    (assoc renderer-state :bind-group new-bind-group)))

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
(defn shape-text [texts global-fsize msdf-atlas & {:keys [char-width snap-step] :or {char-width 0.56}}]
  (let [atlas (:atlas msdf-atlas) metrics (:metrics msdf-atlas)
        glyphs (reduce (fn [acc glyph] (assoc acc (:unicode glyph) glyph)) {} (:glyphs msdf-atlas))
        atlas-w (or (:width atlas) 1) atlas-h (or (:height atlas) 1) line-h (or (:lineHeight metrics) 1.2)
        res (atom [])]
    (doseq [txt texts]
      (let [{:keys [text x y]} txt 
            fsize (or (:size txt) global-fsize)
            snap (when (and snap-step (pos? snap-step))
                   (fn [v] (* (Math/round (/ v snap-step)) snap-step)))
            start-x (if snap (snap x) x)
            start-y (if snap (snap y) y)
            advance (let [v (* fsize char-width)] (if snap (snap v) v))
            !x (atom start-x)
            !y (atom start-y)]
        (doseq [ch (seq text)]
          (let [code (.charCodeAt ch 0)]
            (cond
              (= ch \newline) (do (reset! !x start-x) (reset! !y (+ @!y (* fsize line-h))))
              (= ch \space) (swap! !x + advance)
              :else (when-let [g (get glyphs code)]
                      (let [pb (:planeBounds g)
                            ab (:atlasBounds g)
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


(defn update-text-data [^js/GPUDevice device renderer-state texts atlas font-size & {:keys [px-range line-height-factor line-height sharpness char-width snap-step] :or {px-range 8.0 line-height-factor 1.0 sharpness 0.0 char-width 0.56}}]
  (let [
        line-h (or line-height (* font-size line-height-factor))
        atlas-em (or (get-in atlas [:atlas :size]) 64.0)
        shaped-lines (mapv (fn [tokens-in-line]
                             (let [quads (shape-text tokens-in-line font-size atlas
                                                     :char-width char-width
                                                     :snap-step snap-step)]
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
    
    (let [sizes (js/Float32Array. #js [(float px-range) (float atlas-em) 1.0 1.0 1.0 (float sharpness)])]
      (.writeBuffer (.-queue device) (:sizes-uniform-buffer renderer-state) 0 sizes))

    (assoc renderer-state 
           :instance-buffer new-buffer
           :bind-group new-bind-group 
           :num-instances total-instances
           :line-offsets line-offsets
           :line-height line-h)))


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


(defn draw-frame! [^js device ^js context text-sys editor-rect-sys cmd-rect-sys camera-floats _ignored_pass_descriptor pan-x pan-y w h
                   & {:keys [cmd-panel-visible cmd-panel-h editor-line-count settings-visible settings-rect-sys
                             diagnostics-visible diagnostics-line-index]
                      :or {cmd-panel-visible false cmd-panel-h 40 editor-line-count nil settings-visible false settings-rect-sys nil}}]
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

    ;; Draw editor rects first (selection, brackets, fold indicators, caret, eval)
    (when (and editor-rect-sys (> (:num-instances editor-rect-sys) 0))
      (.setPipeline pass (:pipeline editor-rect-sys))
      (.setBindGroup pass 0 (:bind-group editor-rect-sys))
      (.setVertexBuffer pass 0 (:instance-buffer editor-rect-sys))
      (.draw pass 6 (:num-instances editor-rect-sys)))

    ;; Draw editor text (with viewport culling)
    (when (and text-sys (> (:num-instances text-sys) 0))
      (.setPipeline pass (:pipeline text-sys))
      (.setBindGroup pass 0 (:bind-group text-sys))
      (.setVertexBuffer pass 0 (:instance-buffer text-sys))

      (let [line-offsets (:line-offsets text-sys)
            line-h       (:line-height text-sys)
            total-lines  (count line-offsets)
            ;; If we know how many editor lines there are, only cull those
            ;; Command panel lines are at the end and need different handling
            editor-lines (or editor-line-count total-lines)

            ;; Effective viewport height (exclude command panel area)
            effective-h  (if cmd-panel-visible (- h cmd-panel-h) h)

            scroll-y     (- pan-y)
            start-line   (max 0 (Math/floor (/ scroll-y line-h)))
            end-line     (min editor-lines (+ (Math/ceil (/ (+ scroll-y effective-h) line-h)) 2))]

        ;; Draw visible editor lines
        (when (and (< start-line end-line) (< start-line (count line-offsets)))
          (let [start-inst (nth line-offsets start-line)
                end-inst   (if (< end-line (count line-offsets))
                             (nth line-offsets end-line)
                             (if (< editor-lines total-lines)
                               (nth line-offsets editor-lines)
                               (:num-instances text-sys)))
                draw-count (- end-inst start-inst)]
            (when (> draw-count 0)
              (.draw pass 6 draw-count 0 start-inst))))

        ;; Command panel: draw background, then text, then caret
        (when cmd-panel-visible
          ;; Draw command panel BACKGROUND rect (covers editor text bleeding into panel area)
          (when (and cmd-rect-sys (>= (:num-instances cmd-rect-sys) 1))
            (.setPipeline pass (:pipeline cmd-rect-sys))
            (.setBindGroup pass 0 (:bind-group cmd-rect-sys))
            (.setVertexBuffer pass 0 (:instance-buffer cmd-rect-sys))
            (.draw pass 6 1 0 0))

          ;; Draw command panel TEXT (on top of background)
          (when (< editor-lines total-lines)
            (let [cmd-start-inst (nth line-offsets editor-lines)
                  cmd-end-inst   (:num-instances text-sys)
                  cmd-draw-count (- cmd-end-inst cmd-start-inst)]
              (when (> cmd-draw-count 0)
                ;; Need to set up text pipeline again after drawing rect
                (.setPipeline pass (:pipeline text-sys))
                (.setBindGroup pass 0 (:bind-group text-sys))
                (.setVertexBuffer pass 0 (:instance-buffer text-sys))
                (.draw pass 6 cmd-draw-count 0 cmd-start-inst))))

          ;; Draw command panel CARET rect (on top of text)
          (when (and cmd-rect-sys (>= (:num-instances cmd-rect-sys) 2))
            (.setPipeline pass (:pipeline cmd-rect-sys))
            (.setBindGroup pass 0 (:bind-group cmd-rect-sys))
            (.setVertexBuffer pass 0 (:instance-buffer cmd-rect-sys))
            (.draw pass 6 1 0 1)))

        ;; Settings panel: draw on top of everything when visible
        (when settings-visible
          ;; Draw settings panel BACKGROUND + UI rects
          (when (and settings-rect-sys (> (:num-instances settings-rect-sys) 0))
            (.setPipeline pass (:pipeline settings-rect-sys))
            (.setBindGroup pass 0 (:bind-group settings-rect-sys))
            (.setVertexBuffer pass 0 (:instance-buffer settings-rect-sys))
            (.draw pass 6 (:num-instances settings-rect-sys)))

          ;; Draw settings panel TEXT (font names, labels, values)
          ;; Settings text is appended after editor+cmd text in the instance buffer
          ;; We draw all remaining instances after editor-line-count
          (when (> total-lines editor-lines)
            (let [settings-start-inst (if (and cmd-panel-visible (< editor-lines total-lines))
                                        ;; After command panel text
                                        (:num-instances text-sys)
                                        ;; After editor text
                                        (if (< editor-lines (count line-offsets))
                                          (nth line-offsets editor-lines)
                                          (:num-instances text-sys)))]
              ;; Actually, settings text is the last "line" in line-offsets
              ;; We need to draw from the settings start to end
              (when (< settings-start-inst (:num-instances text-sys))
                (.setPipeline pass (:pipeline text-sys))
                (.setBindGroup pass 0 (:bind-group text-sys))
                (.setVertexBuffer pass 0 (:instance-buffer text-sys))
                (.draw pass 6 (- (:num-instances text-sys) settings-start-inst) 0 settings-start-inst)))))

        ;; Diagnostics overlay: draw when enabled and not covered by panels
        (when (and diagnostics-visible (not cmd-panel-visible) (not settings-visible) diagnostics-line-index)
          (when (< diagnostics-line-index (count line-offsets))
            (let [start-inst (nth line-offsets diagnostics-line-index)
                  next-line (inc diagnostics-line-index)
                  end-inst (if (< next-line (count line-offsets))
                             (nth line-offsets next-line)
                             (:num-instances text-sys))
                  draw-count (- end-inst start-inst)]
              (when (> draw-count 0)
                (.setPipeline pass (:pipeline text-sys))
                (.setBindGroup pass 0 (:bind-group text-sys))
                (.setVertexBuffer pass 0 (:instance-buffer text-sys))
                (.draw pass 6 draw-count 0 start-inst)))))))

    (.end pass)
    (.submit (.-queue device) #js [(.finish encoder)])))
