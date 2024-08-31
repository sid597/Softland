(ns app.client.webgpu.bind
  (:require [app.client.webgpu.buffer :refer [storage-buffer-descriptor]]))

(defn bind-group-descriptor [pipeline uniform-buffer]
  (clj->js {:label "cell renderer bind group"
            :layout (.getBindGroupLayout pipeline 0)
            :entries (clj->js [{:binding 0
                                :resource (clj->js
                                            {:buffer uniform-buffer})}])}))



(defn storage-bind-group-layout []
  (clj->js {:label "storage bind group layout"
            :entries (clj->js [{:binding 0
                                :visibility js/GPUShaderStage.COMPUTE
                                :buffer  {:type  "storage"}}])}))

(defn storage-bind-group-descriptor []
  (clj->js {:layout storage-bind-group-layout
            :label "storage bind group"
            :entries [{:binding 0
                       :resource (clj->js {:buffer storage-buffer-descriptor})}]}))

;; Then in render pass bind the group to a number
;; (.setBindGroup render-pass 0 storage-bind-group)
;; (.setBindGroup render-pass 1 compute-bind-group)


;; Then refer this in your shader and use the data.

;; @group(0) @binding(0)