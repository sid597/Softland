(ns app.client.webgpu.core
  (:require
    [global-flow :refer [!visible-rects !old-visible-rects !font-bitmap !atlas-data]]
    [app.client.webgpu.shader :refer [text-vertex-shader text-fragment-shader add-new-rects-shader-descriptor]]
    [hyperfiddle.incseq :as i]))


(defn string->ints [s]
  (map #(.charCodeAt % 0) s))

(defn shape-text [text fsize msdf-atlas]
  (let [
        _ (println "MSDF ATLAS" msdf-atlas)
        atlas          (:atlas msdf-atlas)
        atlas-width    (:width atlas)
        atlas-height   (:height atlas)
        metrics        (:metrics msdf-atlas)
        line-height    (:lineHeight metrics)
        glyphs         (reduce (fn [acc glyph]
                                 (assoc acc (:unicode glyph)
                                            glyph))
                         {}
                         (:glyphs msdf-atlas))
        font-size      (* (/ 1 (:size atlas)) fsize)]
    (println "--" font-size (:size atlas) (first (seq "c")) "=============" (get glyphs 72))

    (loop [chars (seq text)
           x     0
           y     0
           acc   []]
      (if (empty? chars)
        acc
        (let [ch        (first chars)
              codepoint (.charCodeAt ch 0)]
          (if (= ch \newline)
            ;; Handle newlines by resetting x and adjusting y
            (recur (rest chars)
              0
              (- y (* font-size line-height))
              acc)
            (let [glyph (get glyphs codepoint)]
              (println 'glyph ch codepoint glyph)
              (if glyph
                (let [advance        (* font-size (:advance glyph))
                      plane-bounds   (:planeBounds glyph)
                      atlas-bounds   (:atlasBounds glyph)
                      _ (println "advance" advance plane-bounds atlas-bounds)
                      ;; Scale plane bounds by font size
                      pl             (+ x (* font-size (get plane-bounds :left)))
                      pb             (+ y (* font-size (get plane-bounds :bottom)))
                      pr             (+ x (* font-size (get plane-bounds :right)))
                      pt             (+ y (* font-size (get plane-bounds :top)))
                      positions      [[pl pb] [pr pb] [pr pt] [pl pt]]
                      _ (println "--" positions)
                      ;; Calculate texture coordinates
                      al             (/ (get atlas-bounds :left) atlas-width)
                      ab             (/ (get atlas-bounds :bottom) atlas-height)
                      ar             (/ (get atlas-bounds :right) atlas-width)
                      at             (/ (get atlas-bounds :top) atlas-height)
                      uvs [[al (- 1.0 ab)] [ar (- 1.0 ab)] [ar (- 1.0 at)] [al (- 1.0 at)]]]

                  (recur (rest chars)
                    (+ x advance)
                    y
                    (conj acc {:codepoint codepoint
                               :positions positions
                               :uvs uvs})))
                ;; Skip character if glyph not found
                (recur (rest chars)
                  x
                  y
                  acc)))))))))


(defn to-clip-space [x y canvas-width canvas-height]
  (let [x-ndc (- (* (/ x canvas-width) 2.0) 1.0)
        y-ndc (- 1.0 (* (/ y canvas-height) 2.0))] ; Flip y-axis if necessary
    [x-ndc y-ndc]))


(defn prepare-vertex-data1 [shaped-text canvas-width canvas-height]
  (println "prepare vertext data" canvas-height canvas-width)
  (let [vertices (atom [])
        indices  (atom [])
        index    (atom 0)]
    (doseq [glyph shaped-text]
      (let [[[x0 y0] [x1 y1] [x2 y2] [x3 y3]] (:positions glyph)
            [[u0 v0] [u1 v1] [u2 v2] [u3 v3]] (:uvs glyph)
            idx @index
            ;; Map positions to clip space coordinates
            [x0' y0'] (to-clip-space x0 y0 canvas-width canvas-height)
            [x1' y1'] (to-clip-space x1 y1 canvas-width canvas-height)
            [x2' y2'] (to-clip-space x2 y2 canvas-width canvas-height)
            [x3' y3'] (to-clip-space x3 y3 canvas-width canvas-height)]
        ;; Add transformed vertices (position and UVs)
        (swap! vertices conj
               x0' y0' u0 v0
               x1' y1' u1 v1
               x2' y2' u2 v2
               x3' y3' u3 v3)
        ;; Add indices for two triangles
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



(defn prepare-vertex-data [shaped-text]
  (let [vertices (atom [])
        indices  (atom [])
        index    (atom 0)]
    (doseq [glyph shaped-text]
      (let [[[x0 y0] [x1 y1] [x2 y2] [x3 y3]] (:positions glyph)
            [[u0 v0] [u1 v1] [u2 v2] [u3 v3]] (:uvs glyph)
            idx @index]
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


(defn render-text [device format context text font-size atlas font-bitmap]
  (println "render text ")
  (let [canvas-width             (.-width (.-canvas context))
        canvas-height            (.-height (.-canvas context))
        shaped-text              (shape-text text font-size atlas)
        {:keys [vertex-data
                index-data]}     (prepare-vertex-data shaped-text )
        #_#_{:keys [vertex-data
                index-data]}     (prepare-vertex-data shaped-text canvas-width canvas-height)
        _ (println 'shaped-text 'vertex-data vertex-data 'index-data index-data)
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
                                                                  :mipmapFilter "nearest"}))
        texture-view             (.createView texture)
        _                        (println "texture view")
        bind-group-layout        (.createBindGroupLayout
                                   device
                                   (clj->js {:label "bind group layout"
                                             :entries [{:binding 0
                                                        :visibility js/GPUShaderStage.FRAGMENT
                                                        :sampler {:type "filtering"}}
                                                       {:binding 1
                                                        :visibility js/GPUShaderStage.FRAGMENT
                                                        :texture {:sampleType "float"}}]})) 
        bind-group               (.createBindGroup device
                                  (clj->js {:layout bind-group-layout
                                            :entries (clj->js
                                                       [{:binding 0
                                                         :resource sampler}
                                                        {:binding 1
                                                         :resource texture-view}])}))
        _ (println "bind group done")
        pipeline-layout          (.createPipelineLayout
                                   device 
                                   (clj->js {:label "pipeline layout"
                                             :bindGroupLayouts [bind-group-layout]}))
        pipeline                 (.createRenderPipeline device
                                  (clj->js {:layout pipeline-layout
                                            :vertex {:module shader-module-vertex
                                                     :entryPoint "main"
                                                     :buffers [{:arrayStride (* 4 4)
                                                                :attributes [{:shaderLocation 0 :offset 0 :format "float32x2"}
                                                                             {:shaderLocation 1 :offset 8 :format "float32x2"}]}]}
                                            :fragment {:module shader-module-fragment
                                                       :entryPoint "main"
                                                       :targets [{:format format}]}}))
        encoder                  (.createCommandEncoder device)
        render-pass              (.beginRenderPass
                                   encoder
                                   (clj->js {:colorAttachments 
                                             (clj->js [{:view (.createView (.getCurrentTexture context))
                                                        :clearValue (clj->js {:r 1.0 :g 1.0 :b 1.0 :a 1.0})
                                                        :loadOp "clear"
                                                        :storeOp "store"}])
                                             :label "render parss"}))
        _ (println "num indexes" num-indices)]
    (println "COMPILING")
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
    (println "COPIED")
    (.writeBuffer (.-queue device) vertex-buffer 0 vertex-data)
    (.writeBuffer (.-queue device) index-buffer 0 index-data)
    (.setPipeline render-pass pipeline)
    (.setBindGroup render-pass 0 bind-group)
    (.setVertexBuffer render-pass 0 vertex-buffer)
    (.setIndexBuffer render-pass index-buffer "uint16")
    (println "pipeline and buffers set")
    (.drawIndexed render-pass num-indices)
    (.end render-pass)
    (.submit (.-queue device) [(.finish encoder)])))
  

 

(defn upload-vertices [from data device fformat context config ids]
  ;(println "upload vertices ::" from "::" config "")
  (let [varray                (js/Float32Array. (clj->js data))
        ids-array             (js/Float32Array.  (clj->js ids))
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
                                                       :targets (clj->js [{:format fformat}])})}))]

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
       (.dispatchWorkgroups compute-pass (/ num-rectangles  64))
       (.end                compute-pass)
       ;(.submit (.-queue device) [(.finish encoder)])
       (let [staging-buffer (.createBuffer
                              device
                              (clj->js {:label "staging buffer"
                                        :size ids-array-length
                                        :usage (bit-or js/GPUBufferUsage.MAP_READ
                                                 js/GPUBufferUsage.COPY_DST)}))]
         (.copyBufferToBuffer encoder rendered-ids-buffer 0 staging-buffer 0 ids-array-length)

         (.submit (.-queue device) [(.finish encoder)])

         ; Read the staging buffer
         (-> (.mapAsync staging-buffer js/GPUMapMode.READ)
            (.then (fn []
                     (let [mapped-range (.getMappedRange staging-buffer)
                           num-rendered (js/Float32Array. mapped-range)
                           rendered-ids (sort (into-array num-rendered))
                           new-rects    (js->clj (into-array (filter (complement zero?) rendered-ids)))]
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

