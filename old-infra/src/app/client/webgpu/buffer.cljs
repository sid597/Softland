(ns app.client.webgpu.buffer
  (:require [app.client.webgpu.data :refer [vertices uniform-array]]))


(def base-square-buffer-descriptor
  (clj->js {:label "Cell vertices"
            :size (.-byteLength vertices)
            :usage (bit-or js/GPUBufferUsage.VERTEX
                     js/GPUBufferUsage.COPY_DST)}))

(def storage-buffer-descriptor
  (clj->js {:label "Squares Buffer"
            :size 1000
            :usage (bit-or js/GPUBufferUsage.STORAGE
                     js/GPUBufferUsage.COPY_DST)}))

(def uniform-buffer-descriptor
  (clj->js {:label "grid uniform"
            :size (.-byteLength uniform-array)
            :usage (bit-or js/GPUBufferUsage.UNIFORM
                     js/GPUBufferUsage.COPY_DST)}))

