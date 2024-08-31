(ns app.client.webgpu.sync
  (:require [app.client.webgpu.bind :refer [bind-group-descriptor
                                            storage-bind-group-descriptor]]
            [app.client.webgpu.buffer :refer [base-square-buffer-descriptor
                                              uniform-buffer-descriptor
                                              storage-buffer-descriptor]]
            [app.client.webgpu.pipeline :refer [base-square-pipeline-descriptor]]
            [app.client.webgpu.data :refer [uniform-array vertices grid-size]]
            [app.client.webgpu.shader :refer [shader-descriptor]]))

(defn render-new-node [rect-dim device encoder]
  (let [storage-buffer (.createBuffer device storage-buffer-descriptor)
        storage-pipeline ()
        bind-group     (.createBingGroup device storage-bind-group-descriptor)]))


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

