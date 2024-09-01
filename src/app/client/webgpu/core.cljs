(ns app.client.webgpu.core
  (:require [app.client.webgpu.bind :refer [bind-group-descriptor
                                            storage-bind-group-descriptor]]
            [app.client.webgpu.buffer :refer [base-square-buffer-descriptor
                                              uniform-buffer-descriptor
                                              storage-buffer-descriptor]]
            [app.client.webgpu.pipeline :refer [base-square-pipeline-descriptor]]
            [app.client.webgpu.data :refer [uniform-array vertices grid-size]]
            [app.client.webgpu.shader :refer [shader-descriptor]]))




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
            fn renderVertices(@location(0) pos: vec2f) -> @builtin(position) vec4f {
             return vec4f(pos, 0.0, 1.0);
            }

            @fragment
            fn renderVerticesFragment(@location(0) frag_color: vec4<f32>) -> @location(0) vec4<f32> {
             return vec4f(0.1, 1, 1, 1);
            }
            "}))

(defn render-new-vertices [context new-vertices device encoder fformat]
  (let [varray               (js/Float32Array. new-vertices)
        shader-module        (.createShaderModule device vertices-shader)]

    (js/console.log "array size" (.-byteLength varray) (count new-vertices))






    (let [vertex-buffer-layout (clj->js {:arrayStride 8 ;; each vertex is of length 4 xyhw type f32 (4 bytes)
                                         :attributes (clj->js [{:format "float32x2"
                                                                :offset 0
                                                                ;; numeric location associated with this attribute which will corres
                                                                ;; -pond with a "@location" attribute
                                                                :shaderLocation 0}])})
          render-pipeline (.createRenderPipeline
                            device
                            (clj->js {:label "vertices render pipeline"
                                      :layout "auto"
                                      :vertex (clj->js
                                                {:module shader-module
                                                 :entryPoint "renderVertices"
                                                 :layout "auto"
                                                 :buffers [vertex-buffer-layout]})
                                      :fragment (clj->js
                                                  {:module shader-module
                                                   :entryPoint "renderVerticesFragment "
                                                   :targets (clj->js [{:format fformat}])})}))
          render-pass (.beginRenderPass
                        encoder
                        (clj->js {:colorAttachments
                                  (clj->js [{:view (.createView (.getCurrentTexture context))
                                             :clearValue {:r 0.2 :g 0.9 :b 0.2 :a 1.0}
                                             :loadOp "clear"
                                             :storeOp "store"}])}))]
      (.setPipeline render-pass render-pipeline)
      (.setVertexBuffer render-pass 0 output-buffer)
      (.draw render-pass (* num-rectangles 6)) ; 6 vertices per rectangle
      (.end render-pass)
      (.submit (.-queue device) [(.finish encoder)]))))


