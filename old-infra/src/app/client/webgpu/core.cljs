(ns app.client.webgpu.core
  (:require
    [global-flow :refer [!visible-rects !old-visible-rects !font-bitmap !atlas-data]]
    [app.client.webgpu.shader :refer [text-vertex-shader text-fragment-shader add-new-rects-shader-descriptor]]
    [hyperfiddle.incseq :as i]))


(defn string->ints [s]
  (map #(.charCodeAt % 0) s))


;; In ns app.client.webgpu.core

(defn shape-text [texts fsize msdf-atlas]
  (let [atlas          (:atlas msdf-atlas)
        metrics        (:metrics msdf-atlas)

        glyphs         (reduce (fn [acc glyph] (assoc acc (:unicode glyph) glyph)) {} (:glyphs msdf-atlas))
        atlas-width    (or (:width atlas) 1)
        atlas-height   (or (:height atlas) 1)
        line-height    (or (:lineHeight metrics) 1.2)
        font-size      fsize
        res            (atom [])]

    (doseq [txt texts]
      (let [{:keys [text x y]} txt
            !x (atom x)
            !y (atom y)]
        (doseq [ch (seq text)]
          (let [codepoint (.charCodeAt ch 0)]
            (cond
              (= ch \newline)   (reset! !y (- @!y (* font-size line-height)))
              (= ch \space)     (reset! !x (+ @!x (* font-size 0.5)))
              :else             (let [glyph (get glyphs codepoint)]
                                  (when glyph
                                    (let [plane-bounds   (:planeBounds glyph)
                                          atlas-bounds   (:atlasBounds glyph)
                                          pb-left        (or (get plane-bounds :left) 0)
                                          pb-right       (or (get plane-bounds :right) 0)
                                          pb-top         (or (get plane-bounds :top) 0)
                                          pb-bottom      (or (get plane-bounds :bottom) 0)
                                          advance        (* font-size (or (:advance glyph) 0))
                                          fw             (* font-size (- pb-right pb-left))
                                          fh             (* font-size (- pb-top pb-bottom))
                                          pl             (+ @!x (* font-size pb-left))
                                          pr             (+ pl fw)
                                          pt             (+ @!y (* font-size pb-top))
                                          pb             (- pt fh)
                                          positions      [[pl pb] [pr pb] [pr pt] [pl pt]]
                                          al             (/ (:left atlas-bounds) atlas-width)
                                          ab             (/ (:bottom atlas-bounds) atlas-height)
                                          ar             (/ (:right atlas-bounds) atlas-width)
                                          at             (/ (:top atlas-bounds) atlas-height)
                                          uvs [[ar (- 1.0 ab)] [al (- 1.0 ab)] [al (- 1.0 at)] [ar (- 1.0 at)]]]
                                      ;uvs [[al (- 1.0 ab)] [ar (- 1.0 ab)] [ar (- 1.0 at)] [al (- 1.0 at)]]

                                      (do
                                        (reset! !x (+ @!x advance))
                                        (swap! res conj {:codepoint codepoint
                                                         :positions positions
                                                          :uvs uvs}))))))))))
    @res))


(defn prepare-vertex-data [shaped-text]
  (let [vertices (atom [])
        indices  (atom [])
        index    (atom 0)]
    ;(doseq [shaped-text shaped-texts])
    (doseq [glyph shaped-text]
      ;(println 'shaped-text-1 glyph)
      (let [[[x0 y0] [x1 y1] [x2 y2] [x3 y3]] (:positions glyph)
            [[u0 v0] [u1 v1] [u2 v2] [u3 v3]] (:uvs glyph)
            idx @index]
        ;(println 'positions (:positions glyph))
        ;; Add vertices (position and UVs)
        (swap! vertices conj
               x0 y0 u0 v0
               x1 y1 u1 v1
               x2 y2 u2 v2
               x3 y3 u3 v3)
        ;; Add indices for two triangles (assuming CCW order)
        (swap! indices conj
               idx
               (+ 1 idx)
               (+ 2 idx)
               idx
               (+ 2 idx)
               (+ 3 idx))
        ;; Increment index
        (swap! index + 4)))
    {:vertex-data (js/Float32Array. (clj->js @vertices))
     :index-data  (js/Uint16Array. (clj->js @indices))}))


;; --- The new, efficient setup function ---
(defn setup-text-renderer [^js/GPUDevice device fformat texts atlas font-bitmap]
  (println "PERFORMING EXPENSIVE SETUP. This should only run once!")

  ;; STEP 1: PREPARE CPU DATA (The "Stencil Cutting")
  ;; This is the only time we will call these expensive functions.
  (let [shaped-texts (shape-text texts 16.0 atlas) ; Using a base font size
        {:keys [vertex-data index-data]} (prepare-vertex-data shaped-texts)
        num-indices (.-length index-data)
        ;; ======================== DEBUGGING LOG ========================
        _ (js/console.log "--- Text Renderer Setup Debug ---")
        _ (js/console.log "Number of shaped glyphs:" (count shaped-texts))
        _ (js/console.log "Number of indices to draw:" num-indices)
        _ (when (< num-indices 100)
            (js/console.log "Vertex Data (first 100 bytes):" (.slice vertex-data 0 100))
            (js/console.log "Index Data (first 20 indices):" (.slice index-data 0 20)))
        _ (js/console.log "------------------------------------")
        ;; =============================================================

        ;; STEP 2: CREATE GPU BUFFERS AND UPLOAD STATIC DATA
        ;; These buffers will live on the GPU for the lifetime of the app.
        vertex-buffer (.createBuffer device
                       (clj->js {:label "Static Text Vertex Buffer"
                                 :size (.-byteLength vertex-data)
                                 :usage (bit-or js/GPUBufferUsage.VERTEX js/GPUBufferUsage.COPY_DST)}))

        index-buffer (.createBuffer device
                      (clj->js {:label "Static Text Index Buffer"
                                :size (.-byteLength index-data)
                                :usage (bit-or js/GPUBufferUsage.INDEX js/GPUBufferUsage.COPY_DST)}))
        camera-uniform-buffer (.createBuffer device
                               (clj->js {:label "Camera Uniform Buffer"
                                         :size 24
                                         :usage (bit-or js/GPUBufferUsage.UNIFORM js/GPUBufferUsage.COPY_DST)}))

        ;; NEW: Uniform buffer for the FRAGMENT shader (sizes)
        sizes-data (js/Float32Array. (clj->js [16.0 ; px-range
                                               (:size (:atlas atlas))
                                               16.0])) ; font-size
        sizes-uniform-buffer (.createBuffer device
                              (clj->js {:label "Sizes Uniform Buffer"
                                        :size (.-byteLength sizes-data)
                                        :usage (bit-or js/GPUBufferUsage.UNIFORM js/GPUBufferUsage.COPY_DST)}))


        ;; Upload the vertex and index data now. This is a one-time operation.
        _ (.writeBuffer (.-queue device) vertex-buffer 0 vertex-data)
        _ (.writeBuffer (.-queue device) index-buffer 0 index-data)
        _ (.writeBuffer (.-queue device) sizes-uniform-buffer 0 sizes-data)



        ;; STEP 3: CREATE TEXTURE AND SAMPLER (One time)

        bitmap-height            (.-height font-bitmap)
        bitmap-width             (.-width font-bitmap)
        texture (.createTexture device
                                  (clj->js {:size {:width bitmap-width
                                                   :height bitmap-height
                                                   :depthOrArrayLayers 1}
                                            :format "rgba8unorm"
                                            :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                                           js/GPUTextureUsage.TEXTURE_BINDING
                                                           js/GPUTextureUsage.COPY_DST)}))
        sampler (.createSampler device (clj->js {:minFilter "linear"
                                                                  :magFilter "linear"
                                                                  :mipmapFilter "linear"}))
        _  (.copyExternalImageToTexture
             ( .-queue device)
             ( clj->js {:source font-bitmap})
             ( clj->js {:texture texture
                        :origin {:x 0 :y 0 :z 0}})
             ( clj->js {:width bitmap-width
                        :height bitmap-height
                        :depthOrArrayLayers 1}))


        ;; STEP 4: COMPILE SHADERS (One time)
        ;; Your fragment shader can stay the same. We update the vertex shader.
        vertex-shader-with-camera (clj->js {:label "text vertex shader with camera"
                                             :code "
                                               struct Camera {
                                                 pan: vec2<f32>,
                                                 zoom: f32,
                                                 // padding
                                                 screen_dimensions: vec2<f32>,
                                               };

                                               struct VertexInput {
                                                 @location(0) position: vec2<f32>,
                                                 @location(1) uv: vec2<f32>,
                                               };

                                               struct VertexOutput {
                                                 @builtin(position) position: vec4<f32>,
                                                 @location(0) uv: vec2<f32>,
                                               };

                                               @group(0) @binding(2) var<uniform> camera: Camera;

                                               @vertex
                                               fn main(input: VertexInput) -> VertexOutput {
                                                   var output: VertexOutput;

                                                   let zoomed_position = input.position * camera.zoom;
                                                   let panned_position = zoomed_position + camera.pan;

                                                   // Convert from world pixel coordinates to GPU clip space (-1.0 to 1.0)
                                                   let zero_to_two = panned_position / camera.screen_dimensions * 2.0;
                                                   let shifted = zero_to_two - vec2<f32>(1.0, 1.0);

                                                   output.position = vec4<f32>(shifted.x, -shifted.y, 0.0, 1.0); // Flip Y
                                                   output.uv = input.uv;
                                                   return output;
                                               }
                                             "})
        
        shader-module-vertex (.createShaderModule device vertex-shader-with-camera)
        shader-module-fragment (.createShaderModule device text-fragment-shader) ; Your existing fragment shader


        ;; STEP 5: CREATE THE ENTIRE RENDER PIPELINE (The most expensive "one-time" step)
        bind-group-layout (.createBindGroupLayout
                                   device
                                   (clj->js {:label "bind group layout"
                                             :entries [{:binding 0
                                                        :visibility js/GPUShaderStage.FRAGMENT
                                                        :sampler {:type "filtering"}}
                                                       {:binding 1
                                                        :visibility js/GPUShaderStage.FRAGMENT
                                                        :texture {:sampleType "float"}}
                                                       {:binding 2
                                                        :visibility js/GPUShaderStage.VERTEX
                                                        :buffer {:type "uniform"}}
                                                       {:binding 3 ; Sizes Uniform
                                                        :visibility js/GPUShaderStage.FRAGMENT
                                                        :buffer {:type "uniform"}}]}))
        
        pipeline-layout (.createPipelineLayout device (clj->js {:bindGroupLayouts [bind-group-layout]}))
        
        pipeline (.createRenderPipeline device
                                  (clj->js {:layout pipeline-layout
                                            :vertex {:module shader-module-vertex
                                                     :entryPoint "main"
                                                     :buffers (clj->js [{:arrayStride (* 4 4)
                                                                         :attributes (clj->js
                                                                                      [{:shaderLocation 0 :offset 0 :format "float32x2"}
                                                                                       {:shaderLocation 1 :offset 8 :format "float32x2"}])}])}
                                            :fragment (clj->js
                                                        {:module shader-module-fragment
                                                         :entryPoint "main"
                                                         :targets (clj->js
                                                                    [{:format fformat
                                                                      :blend (clj->js {:color (clj->js {:srcFactor "src-alpha"
                                                                                                        :dstFactor "one-minus-src-alpha"})
                                                                                       :alpha (clj->js {:srcFactor "src-alpha"
                                                                                                        :dstFactor "one-minus-src-alpha"})})}])})}))
        
        ;; STEP 6: CREATE THE BIND GROUP (Connects our resources together)
        bind-group (.createBindGroup device
                    (clj->js {:layout bind-group-layout
                              :entries [{:binding 0, :resource sampler}
                                        {:binding 1, :resource (.createView texture)}
                                        {:binding 2, :resource {:buffer camera-uniform-buffer}}
                                        {:binding 3, :resource {:buffer sizes-uniform-buffer}}]}))]

    ;; STEP 7: RETURN ALL THE REUSABLE GPU OBJECTS IN A MAP
    (println "DONE WITH SETUP")
    {:pipeline pipeline
     :bind-group bind-group
     :camera-uniform-buffer camera-uniform-buffer
     :num-indices num-indices
     :vertex-buffer vertex-buffer
     :index-buffer index-buffer}))


;; --- The new, fast drawing function ---
(defn draw-text [^js/GPUDevice device ^js/GPUCanvasContext context renderer camera-state]
  ;; `renderer` is the map we got from `setup-text-renderer`
  ;; `camera-state` is a map like {:pan-x 10, :pan-y 20, :zoom 1.5, :width 800, :height 600}

  (println "DRAW TEXT: ")
  (cljs.pprint/pprint renderer)
  (cljs.pprint/pprint camera-state)

  ;; STEP 1: UPDATE THE SMALL UNIFORM BUFFER (Very fast)
  (let [camera-array (js/Float32Array.
                      (clj->js [(:pan-x camera-state)
                                (:pan-y camera-state)
                                (:zoom camera-state)
                                0.0 ; Padding
                                (:width camera-state)
                                (:height camera-state)]))]
    ;; We only write 24 bytes of data each frame, not megabytes!
    (.writeBuffer (.-queue device) (:camera-uniform-buffer renderer) 0 camera-array)
    (println "wrote buffer"))
  ;; STEP 2: CREATE ENCODER AND RENDER PASS
  (let [encoder (.createCommandEncoder device)
        ;; NOTE: loadOp is "load" so we draw ON TOP of whatever was there before (like your rectangles)
        render-pass (.beginRenderPass encoder
                     (clj->js {:colorAttachments [{:view (.createView (.getCurrentTexture context))
                                                   :loadOp "load"
                                                   :storeOp "store"}]}))]

    ;; STEP 3: ISSUE DRAW COMMANDS USING THE PRE-BUILT OBJECTS
    (.setPipeline render-pass (:pipeline renderer))
    (.setBindGroup render-pass 0 (:bind-group renderer))
    (println "pipeline and bindgroup set")
    
    ;; We don't need to set the vertex/index buffers because they are part of the pipeline state in this setup
    ;; (This can vary based on exact pipeline setup, but often they are set with the pipeline)
    ;; If you need to set them explicitly:
    (.setVertexBuffer render-pass 0 (:vertex-buffer renderer))
    (.setIndexBuffer render-pass (:index-buffer renderer) "uint16")

    (.drawIndexed render-pass (:num-indices renderer))

    (.end render-pass)
    (println "done render pass")
    (.submit (.-queue device) [(.finish encoder)])))

(defn render-text [^js/GPUDevice device format  ^js/GPUCanvasContext context px-range font-size atlas font-bitmap texts]
  (println "webgpu render text ")
  (let [sizes                    (js/Float32Array. (clj->js [px-range (:size (:atlas atlas)) font-size]))
        shaped-texts              (shape-text texts font-size atlas)
       ; _ (println "shaped texts" shaped-texts)
        {:keys [vertex-data
                index-data]}     (prepare-vertex-data shaped-texts)
        ;_ (println 'vertex-data vertex-data)
        ;_ (println 'index-data index-data)
        num-indices              (.-length index-data)
        shader-module-vertex     (.createShaderModule
                                   device
                                   text-vertex-shader)
        shader-module-fragment   (.createShaderModule
                                   device
                                   text-fragment-shader)
        vertex-buffer            (.createBuffer device
                                  (clj->js {:size (.-byteLength vertex-data)
                                            :usage (bit-or js/GPUBufferUsage.VERTEX
                                                           js/GPUBufferUsage.COPY_DST)}))
        index-buffer             (.createBuffer device
                                   (clj->js {:size (.-byteLength index-data)
                                             :usage (bit-or js/GPUBufferUsage.INDEX
                                                            js/GPUBufferUsage.COPY_DST)}))
        size-buffer              (.createBuffer
                                   device
                                   (clj->js {:size (.-byteLength sizes)
                                             :usage (bit-or js/GPUBufferUsage.UNIFORM
                                                             js/GPUBufferUsage.COPY_DST)}))
        bitmap-height            (.-height font-bitmap)
        bitmap-width             (.-width font-bitmap)
        texture                  (.createTexture device
                                  (clj->js {:size {:width bitmap-width
                                                   :height bitmap-height
                                                   :depthOrArrayLayers 1}
                                            :format "rgba8unorm"
                                            :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                                           js/GPUTextureUsage.TEXTURE_BINDING
                                                           js/GPUTextureUsage.COPY_DST)}))
        sampler                  (.createSampler device (clj->js {:minFilter "linear"
                                                                  :magFilter "linear"
                                                                  :mipmapFilter "linear"}))
        texture-view             (.createView texture)
        ;_                        (println "texture view")
        bind-group-layout        (.createBindGroupLayout
                                   device
                                   (clj->js {:label "bind group layout"
                                             :entries [{:binding 0
                                                        :visibility js/GPUShaderStage.FRAGMENT
                                                        :sampler {:type "filtering"}}
                                                       {:binding 1
                                                        :visibility js/GPUShaderStage.FRAGMENT
                                                        :texture {:sampleType "float"}}
                                                       {:binding 2
                                                        :visibility js/GPUShaderStage.FRAGMENT
                                                        :buffer {:type "uniform"}}]}))

        bind-group               (.createBindGroup device
                                  (clj->js {:layout bind-group-layout
                                            :entries (clj->js
                                                       [{:binding 0
                                                         :resource sampler}
                                                        {:binding 1
                                                         :resource texture-view}
                                                        {:binding 2
                                                         :resource {:buffer size-buffer}}])}))
        ;_ (println "bind group done")
        pipeline-layout          (.createPipelineLayout
                                   device
                                   (clj->js {:label "pipeline layout"
                                             :bindGroupLayouts [bind-group-layout]}))
        pipeline                 (.createRenderPipeline device
                                  (clj->js {:layout pipeline-layout
                                            :vertex {:module shader-module-vertex
                                                     :entryPoint "main"
                                                     :buffers (clj->js [{:arrayStride (* 4 4)
                                                                         :attributes (clj->js
                                                                                      [{:shaderLocation 0 :offset 0 :format "float32x2"}
                                                                                       {:shaderLocation 1 :offset 8 :format "float32x2"}])}])}
                                            :fragment (clj->js
                                                        {:module shader-module-fragment
                                                         :entryPoint "main"
                                                         :targets (clj->js
                                                                    [{:format format
                                                                      :blend (clj->js {:color (clj->js {:srcFactor "src-alpha"
                                                                                                        :dstFactor "one-minus-src-alpha"})
                                                                                       :alpha (clj->js {:srcFactor "src-alpha"
                                                                                                        :dstFactor "one-minus-src-alpha"})})}])})}))

        encoder                  (.createCommandEncoder device)

        bbg {:r 0.0 :g 0.0 :b 0.0 :a 1.0}
        wbg {:r 1.0 :g 1.0 :b 1.0 :a 1.0}
        render-pass              (.beginRenderPass
                                   encoder
                                   (clj->js {:colorAttachments
                                             (clj->js [{:view (.createView (.getCurrentTexture context))
                                                        :loadOp "load"
                                                        :storeOp "store"}])
                                             :label "render parss"}))]
        ;_ (println "num indexes" num-indices)]
    ;(println "COMPILING")
    (-> (.getCompilationInfo shader-module-vertex)
        (.then (fn [info] (js/console.log "compute shader info:" info))))
    (-> (.getCompilationInfo shader-module-fragment)
        (.then (fn [info] (js/console.log "compute shader info:" info))))
    (.copyExternalImageToTexture
      (.-queue device)
      (clj->js {:source font-bitmap})
      (clj->js {:texture texture
                :origin {:x 0 :y 0 :z 0}})
      (clj->js {:width bitmap-width
                :height bitmap-height
                :depthOrArrayLayers 1}))
    ;(println "COPIED")
    (.writeBuffer (.-queue device) vertex-buffer 0 vertex-data)
    (.writeBuffer (.-queue device) index-buffer 0 index-data)
    (.writeBuffer (.-queue device) size-buffer 0 sizes)
    (.setPipeline render-pass pipeline)
    (.setBindGroup render-pass 0 bind-group)
    (.setVertexBuffer render-pass 0 vertex-buffer)
    (.setIndexBuffer render-pass index-buffer "uint16")
    ;(println "pipeline and buffers set")
    (.drawIndexed render-pass num-indices)
    (.end render-pass)
    (.submit (.-queue device) [(.finish encoder)])))




(defn render-rect [from data ^js/GPUDevice device fformat ^js/GPUCanvasContext context config ids]
  ;(println 'uplaod-vertices data ":::::::" ids)
  (let [varray                (js/Float32Array. (clj->js data))
        ids-array             (js/Uint32Array.  (clj->js ids))
        ;_ (println "IDS ARRAY" ids-array)
        ids-array-length      (.-byteLength ids-array)
        settings-array        (js/Float32Array. (clj->js config))
        num-rectangles        (count data)
        output-size           (* num-rectangles 12)
        shader-module         (.createShaderModule device add-new-rects-shader-descriptor)
        input-buffer          (.createBuffer
                                device
                                (clj->js {:label "input buffer"
                                          :size (.-byteLength varray)
                                          :usage (bit-or js/GPUBufferUsage.STORAGE
                                                   js/GPUBufferUsage.COPY_DST)}))
        output-buffer         (.createBuffer
                                device
                                (clj->js {:label "output buffer"
                                          :size output-size
                                          :usage (bit-or js/GPUBufferUsage.STORAGE
                                                    js/GPUBufferUsage.VERTEX
                                                    js/GPUBufferUsage.COPY_DST
                                                    js/GPUBufferUsage.COPY_SRC)}))
        settings-uniform-buffer (.createBuffer
                                  device
                                  (clj->js {:label "settings buffer"
                                            :size (.-byteLength settings-array)
                                            :usage (bit-or js/GPUBufferUsage.UNIFORM
                                                     js/GPUBufferUsage.COPY_DST)}))
        id-buffer             (.createBuffer
                                device
                                (clj->js {:label "id buffer"
                                          :size ids-array-length
                                          :usage (bit-or js/GPUBufferUsage.STORAGE
                                                   js/GPUBufferUsage.COPY_DST)}))
        rendered-ids-buffer   (.createBuffer
                                device
                                (clj->js {:label "rendered ids buffer"
                                          :size ids-array-length
                                          :usage (bit-or js/GPUBufferUsage.STORAGE
                                                   js/GPUBufferUsage.COPY_SRC)}))
        binding-group-layout  (.createBindGroupLayout
                               device
                               (clj->js {:label "compute bind group layout"
                                         :entries (clj->js [{:binding 0
                                                             :visibility js/GPUShaderStage.COMPUTE
                                                             :buffer {:type "read-only-storage"}}
                                                            {:binding 1
                                                             :visibility js/GPUShaderStage.COMPUTE
                                                             :buffer {:type "storage"}}
                                                            {:binding 2
                                                             :visibility js/GPUShaderStage.COMPUTE
                                                             :buffer {:type "uniform"}}
                                                            {:binding 3
                                                             :visibility js/GPUShaderStage.COMPUTE
                                                             :buffer {:type "read-only-storage"}}
                                                            {:binding 4
                                                             :visibility js/GPUShaderStage.COMPUTE
                                                             :buffer {:type "storage"}}])}))
        bind-group            (.createBindGroup
                                device
                                (clj->js {:layout binding-group-layout
                                          :entries (clj->js [{:binding 0
                                                              :resource {:buffer input-buffer}}
                                                             {:binding 1
                                                              :resource {:buffer output-buffer}}
                                                             {:binding 2
                                                              :resource {:buffer settings-uniform-buffer}}
                                                             {:binding 3
                                                              :resource {:buffer id-buffer}}
                                                             {:binding 4
                                                              :resource {:buffer rendered-ids-buffer}}])}))
        pipeline-layout       (.createPipelineLayout
                                device
                                (clj->js {:label "compute pipeline layout"
                                          :bindGroupLayouts [binding-group-layout]}))
        compute-pipeline      (.createComputePipeline
                                device
                                (clj->js {:layout pipeline-layout
                                          :label "compute pipeline"
                                          :compute (clj->js {:module shader-module
                                                             :entryPoint "main"})}))
        render-binding-group-layout  (.createBindGroupLayout
                                       device
                                       (clj->js {:label "render bind group layout"
                                                 :entries (clj->js [{:binding 1
                                                                      :visibility js/GPUShaderStage.VERTEX
                                                                      :buffer {:type "read-only-storage"}}
                                                                    {:binding 2
                                                                     :visibility js/GPUShaderStage.VERTEX
                                                                     :buffer {:type "uniform"}}])}))

        render-bind-group            (.createBindGroup
                                       device
                                       (clj->js {:layout render-binding-group-layout
                                                 :entries (clj->js [{:binding 1
                                                                     :resource {:buffer output-buffer}}
                                                                    {:binding 2
                                                                     :resource {:buffer settings-uniform-buffer}}])}))

        render-pipeline-layout (.createPipelineLayout
                                 device
                                 (clj->js {:label "compute pipeline layout"
                                           :bindGroupLayouts [render-binding-group-layout]}))

        render-pipeline       (.createRenderPipeline
                                device
                                (clj->js {:label "vertices render pipeline"
                                          :layout render-pipeline-layout
                                          :vertex (clj->js
                                                    {:module shader-module
                                                     :entryPoint "renderVertices"
                                                     :layout "auto"
                                                     :buffers (clj->js [])})
                                          :fragment (clj->js
                                                      {:module shader-module
                                                       :entryPoint "renderVerticesFragment"
                                                       :targets (clj->js 
                                                                    [{:format fformat
                                                                      #_#_:blend (clj->js {:color (clj->js {:srcFactor "src-alpha"
                                                                                                            :dstFactor "one-minus-src-alpha"})
                                                                                           :alpha (clj->js {:srcFactor "src-alpha"
                                                                                                            :dstFactor "one-minus-src-alpha"})})}])})}))]

    #_(-> (.getCompilationInfo shader-module)
        (.then (fn [info] (js/console.log "compute shader info:" info))))
    (.writeBuffer (.-queue device) input-buffer 0 varray)
    (.writeBuffer (.-queue device) settings-uniform-buffer 0 settings-array)
    (.writeBuffer (.-queue device) id-buffer 0 ids-array)


    (let [encoder (.createCommandEncoder device)
          compute-pass  (.beginComputePass encoder)]
       ;; Compute pipeline
       (.setPipeline        compute-pass compute-pipeline)
       (.setBindGroup       compute-pass 0 bind-group)
       (.dispatchWorkgroups compute-pass (max 1 (/ num-rectangles  64)))
       (.end                compute-pass)
       ;(.submit (.-queue device) [(.finish encoder)])
       (let [staging-buffer (.createBuffer
                              device
                              (clj->js {:label "staging buffer"
                                        :size  ids-array-length
                                        :usage (bit-or js/GPUBufferUsage.MAP_READ
                                                 js/GPUBufferUsage.COPY_DST)}))]
         (.copyBufferToBuffer encoder rendered-ids-buffer 0 staging-buffer 0 ids-array-length)

         (.submit (.-queue device) [(.finish encoder)])

         ; Read the staging buffer
         (-> (.mapAsync staging-buffer js/GPUMapMode.READ)
            (.then (fn []
                     (let [mapped-range (.getMappedRange staging-buffer)
                           num-rendered (js/Uint32Array. mapped-range)
                           rendered-ids (sort (into-array num-rendered))
                           new-rects    (js->clj (into-array (filter (complement zero?) rendered-ids)))]
                       ;(println 'rendered-ids num-rendered new-rects)
                       (if (= "initial" from)
                         (swap! !visible-rects (constantly new-rects))
                         (do 
                           (when-not (= new-rects @!visible-rects)
                             (swap! !old-visible-rects (constantly @!visible-rects)))
                           (swap! !visible-rects (constantly new-rects))))
                       (.unmap staging-buffer)))))))


    ;; Render pipeline
    (let [encoder (.createCommandEncoder device)
          render-pass  (.beginRenderPass
                         encoder
                         (clj->js {:colorAttachments (clj->js [{:view (.createView (.getCurrentTexture context))
                                                                :clearValue (clj->js {:r 0.0 :g 0.0 :b 0.0 :a 1})
                                                                :loadOp "clear"
                                                                :storeOp "store"}])
                                   :label "render parss"}))]


      (.setPipeline  render-pass render-pipeline)
      (.setBindGroup render-pass 0 render-bind-group)
      (.draw         render-pass (* num-rectangles 2))
      (.end          render-pass)
      (.submit (.-queue device) [(.finish encoder)]))))

