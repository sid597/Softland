(ns app.client.webgpu.sync)



(defonce vertices (js/Float32Array. (clj->js [ -0.8, -0.8,
                                              0.8, -0.8,
                                              0.8,  0.8,
                                              -0.8, -0.8,
                                              0.8,  0.8,
                                              -0.8,  0.8,])))

;; First create the array of all the vertices to be rendered in a buffer
(defn buffer-data [device]
  (clj->js {:label "Cell vertices"
            :size (.-byteLength vertices)
            :usage (bit-or js/GPUBufferUsage.VERTEX
                     js/GPUBufferUsage.COPY_DST)}))

;; Then define the structure of each vertexs data
(def vertex-buffer-layout
  (clj->js {;;This is the number of bytes the GPU needs to skip forward in the buffer
            ;; when it's looking for the next vertex. Each vertex of your square is made
            ;; up of two 32-bit floating point numbers. As mentioned earlier, a 32-bit
            ;; float is 4 bytes, so two floats is 8 bytes
            :arrayStride 8

            ;;Attributes are the individual pieces of information encoded into each vertex.
            ;; Your vertices only contain one attribute (the vertex position), but more advanced use
            ;; cases frequently have vertices with multiple attributes in them like the color of a
            ;; vertex or the direction the geometry surface is pointing.
            :attributes (clj->js
                          [{:format "float32x2"
                            :offset 0
                            :shaderLocation 0}])}))

(def shader-data
  (clj->js
    {:label "cell shader"
     :code "@vertex
             fn vertexMain(@location(0) pos: vec2f) ->
               @builtin(position) vec4f {
               return vec4f(pos, 0, 1);
             }

             @fragment
             fn fragmentMain() -> @location(0) vec4f {
               return vec4f(1, 0, 0, 1);
             }"}))



;; Create a render pipeline
;; A shader module can't be used for rendering on its own. Instead, you have to use it as part of a GPURenderPipeline,
;; created by calling device.createRenderPipeline(). The render pipeline controls how geometry is drawn, including things
;; like which shaders are used, how to interpret data in vertex buffers, which kind of geometry should be rendered
;; (lines, points, triangles...), and more!


(defn render-pipeline [shader-module cformat]
  (clj->js
    {:label "cell pipeline"
     :layout "auto"
     :vertex (clj->js
               {:module shader-module
                :entryPoint "vertexMain"
                :buffers (clj->js [vertex-buffer-layout])})
     :fragment (clj->js
                 {:module shader-module
                  :entryPoint "fragmentMain"
                  :targets (clj->js [{:format cformat}])})}))



(defn run-webgpu [context device canvas cformat]
  (let [bdata               (buffer-data device)
        vertex-buffer       (.createBuffer device bdata)
        cell-shader-module (.createShaderModule device shader-data)
        pipeline-data      (render-pipeline cell-shader-module cformat)
        cell-pipeline      (.createRenderPipeline device pipeline-data)
        encoder            (.createCommandEncoder device)
        pass               (.beginRenderPass
                             encoder
                             (clj->js {:colorAttachments
                                       (clj->js [{:view (.createView (.getCurrentTexture context))
                                                  :clearValue (clj->js {:r 0 :g 0 :b 0.4 :a 1})
                                                  :loadOp "clear"
                                                  :storeOp "store"}])}))]

    (.writeBuffer (.-queue device) vertex-buffer 0 vertices)
    (.setPipeline pass cell-pipeline)
    (.setVertexBuffer pass 0 vertex-buffer)
    (.draw pass 6)
    (.end pass)
    (.submit (.-queue device) [(.finish encoder)])))

