(ns app.client.webgpu.core
  (:require [app.client.webgpu.shader :refer [shader-descriptor add-new-rects-shader-descriptor]]))



(defn upload-vertices [from data device fformat context config]
  (println "upload vertices ::" from "::" config)
  (let [varray                (js/Float32Array. (clj->js data))
        settings-array         (js/Float32Array. (clj->js config))
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
        binding-group-layout (.createBindGroupLayout
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
                                                            :buffer {:type "uniform"}}])}))

        bind-group            (.createBindGroup
                                device
                                (clj->js {:layout binding-group-layout
                                          :entries (clj->js [{:binding 0
                                                              :resource {:buffer input-buffer}}
                                                             {:binding 1
                                                              :resource {:buffer output-buffer}}
                                                             {:binding 2
                                                              :resource {:buffer settings-uniform-buffer}}])}))

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
                                                                :clearValue (clj->js {:r 0.0 :g 0.0 :b 0.0 :a 1})
                                                                :loadOp "clear"
                                                                :storeOp "store"}])
                                   :label "render parss"}))]


      (.setPipeline  render-pass render-pipeline)
      (.setBindGroup render-pass 0 render-bind-group)
      (.draw         render-pass (* num-rectangles 2))
      (.end          render-pass)
      (.submit       (.-queue device) [(.finish encoder)]))))