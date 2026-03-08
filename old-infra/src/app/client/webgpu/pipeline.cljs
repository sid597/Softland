(ns app.client.webgpu.pipeline)

;; Create a render pipeline
;; A shader module can't be used for rendering on its own. Instead, you have to use it as part of a GPURenderPipeline,
;; created by calling device.createRenderPipeline(). The render pipeline controls how geometry is drawn, including things
;; like which shaders are used, how to interpret data in vertex buffers, which kind of geometry should be rendered
;; (lines, points, triangles...), and more!


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

(defn base-square-pipeline-descriptor [shader-module cformat]
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

