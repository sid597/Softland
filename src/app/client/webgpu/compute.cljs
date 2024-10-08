(ns app.client.webgpu.compute)


(defn compute-pipeline-descriptor [shader-module]
  {:label "square compute descriptor"
   :layout "GPUPipelineLayout"
   :compute (clj->js {:module shader-module
                      :entryPoint "modifySquare"})})



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
