(ns app.client.webgpu.core
  (:require [app.client.webgpu.bind :refer [bind-group-descriptor
                                            storage-bind-group-descriptor]]
            [app.client.webgpu.buffer :refer [base-square-buffer-descriptor
                                              uniform-buffer-descriptor
                                              storage-buffer-descriptor]]
            [app.client.webgpu.pipeline :refer [base-square-pipeline-descriptor]]
            [app.client.webgpu.data :refer [uniform-array vertices grid-size]]
            [app.client.webgpu.shader :refer [shader-descriptor add-new-rects-shader-descriptor]]))




(defn run-webgpu [context device canvas cformat]
  (let [vertex-buffer      (.createBuffer device base-square-buffer-descriptor)
        uniform-buffer     (.createBuffer device uniform-buffer-descriptor)
        cell-shader-module (.createShaderModule device shader-descriptor)
        pipeline-data      (base-square-pipeline-descriptor cell-shader-module cformat)
        cell-pipeline      (.createRenderPipeline device pipeline-data)
        bind-group         (.createBindGroup device
                             (bind-group-descriptor cell-pipeline uniform-buffer))
        encoder            (.createCommandEncoder device)
        pass               (.beginRenderPass
                             encoder
                             (clj->js {:colorAttachments
                                       (clj->js [{:view (.createView (.getCurrentTexture context))
                                                  :clearValue (clj->js {:r 0.2 :g 0.2 :b 0.2 :a 1})
                                                  :loadOp "clear"
                                                  :storeOp "store"}])}))]
    ;(reset! !vertex-buffer vertex-buffer)
    (.writeBuffer (.-queue device) uniform-buffer 0 uniform-array)
    (.writeBuffer (.-queue device) vertex-buffer 0 vertices)
    (.setPipeline pass cell-pipeline)
    (.setVertexBuffer pass 0 vertex-buffer)
    (.setBindGroup pass 0 bind-group)
    (.draw pass (/ (.-length vertices) 2) (* grid-size grid-size))
    (.end pass)
    (.submit (.-queue device) [(.finish encoder)])))


(def vertices-render-shader
  (clj->js {:label "vertices render shader descriptor"
            :code "
            @vertex
            fn renderVertices(@location(0) pos: vec2f) -> @builtin(position) vec4<f32> {
             return vec4f(pos, 0.0, 1.0);
            }

            @fragment
            fn renderVerticesFragment() -> @location(0) vec4f {
            return vec4f(0.9, 0.9, 0.9, 1);
            }
            "}))

(defn render-new-vertices [context new-vertices device fformat num-rectangles output-buffer]
  (js/console.log "RENDER NEW VERTICES"(js/Float32Array. new-vertices) fformat)
  (let [encoder              (.createCommandEncoder device)
        varray               (js/Float32Array. new-vertices)
        shader-module        (.createShaderModule device vertices-render-shader)
        vertex-buffer-layout (clj->js {:arrayStride 8
                                       :attributes (clj->js [{:format "float32x2"
                                                              :offset 0
                                                              ;; numeric location associated with this attribute which will corres
                                                              ;; -pond with a "@location" attribute
                                                              :shaderLocation 0}])})
         render-pipeline     (.createRenderPipeline
                               device
                               (clj->js {:label "vertices render pipeline"
                                         :layout "auto"
                                         :vertex (clj->js
                                                   {:module shader-module
                                                    :entryPoint "renderVertices"
                                                    :layout "auto"
                                                    :buffers (clj->js [vertex-buffer-layout])})
                                         :fragment (clj->js
                                                     {:module shader-module
                                                      :entryPoint "renderVerticesFragment"
                                                      :targets (clj->js [{:format fformat}])})}))
         render-pass         (.beginRenderPass
                               encoder
                               (clj->js {:colorAttachments (clj->js [{:view (.createView (.getCurrentTexture context))
                                                                      :clearValue (clj->js {:r 0.2 :g 0.2 :b 0.2 :a 1})
                                                                      :loadOp "clear"
                                                                      :storeOp "store"}])
                                         :label "render parss"}))]


   (-> (.getCompilationInfo shader-module)
     (.then (fn [info] (js/console.log " render shader info:" info))))
   (.writeBuffer (.-queue device) output-buffer 0 varray)
   (.setPipeline render-pass render-pipeline)
   (.setVertexBuffer render-pass 0 output-buffer)
   (.draw render-pass (* num-rectangles 6)) ; 6 vertices per rectangle
   (.end render-pass)
   (.submit (.-queue device) [(.finish encoder)])))


(defn upload-vertices [data device fformat context]
  (let [varray                (js/Float32Array. (clj->js data))
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
        binding-group-layout (.createBindGroupLayout
                              device
                              (clj->js {:label "compute bind group layout"
                                        :entries (clj->js [{:binding 0
                                                            :visibility js/GPUShaderStage.COMPUTE
                                                            :buffer {:type "read-only-storage"}}
                                                           {:binding 1
                                                            :visibility js/GPUShaderStage.COMPUTE
                                                            :buffer {:type "storage"}}])}))
        bind-group            (.createBindGroup
                                device
                                (clj->js {:layout binding-group-layout
                                          :entries (clj->js [{:binding 0
                                                              :resource {:buffer input-buffer}}
                                                             {:binding 1
                                                              :resource {:buffer output-buffer}}])}))

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
                                                                      :buffer {:type "read-only-storage"}}])}))


        render-bind-group            (.createBindGroup
                                       device
                                       (clj->js {:layout render-binding-group-layout
                                                 :entries (clj->js [{:binding 1
                                                                      :resource {:buffer output-buffer}}])}))
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

    (-> (.getCompilationInfo shader-module)
      (.then (fn [info] (js/console.log "compute shader info:" info))))
    (.writeBuffer        (.-queue device) input-buffer 0 varray)
    (let [encoder (.createCommandEncoder device)
          compute-pass  (.beginComputePass encoder)]
       ;; Compute pipeline
       (.setPipeline        compute-pass compute-pipeline)
       (.setBindGroup       compute-pass 0 bind-group)
       (.dispatchWorkgroups compute-pass (/ num-rectangles  64))
       (.end                compute-pass)
       (.submit       (.-queue device) [(.finish encoder)]))

    ;; Render pipeline
    (let [encoder (.createCommandEncoder device)
          render-pass  (.beginRenderPass
                         encoder
                         (clj->js {:colorAttachments (clj->js [{:view (.createView (.getCurrentTexture context))
                                                                :clearValue (clj->js {:r 1.0 :g 1.0 :b 1.0 :a 1})
                                                                :loadOp "clear"
                                                                :storeOp "store"}])
                                   :label "render parss"}))]
       (.setPipeline  render-pass render-pipeline)
       (.setBindGroup render-pass 0 render-bind-group)
       (.draw         render-pass (* num-rectangles 2))
       (.end          render-pass)
       (.submit       (.-queue device) [(.finish encoder)]))))
