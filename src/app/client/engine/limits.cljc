(ns app.client.engine.limits
  "What a WebGPU device allows and what an allocated texture costs.
   Takes: a WebGPU adapter or device; a texture format and dimensions.
   Gives: the selected limits and total bytes across mips and samples.
   Holds: nothing.")

(defn adapter-limits [^js adapter-or-device]
  #?(:clj nil
     :cljs
     (when adapter-or-device
       (let [^js limits (.-limits adapter-or-device)]
         {:max-buffer-size (some-> limits .-maxBufferSize)
          :max-uniform-buffer-binding-size
          (some-> limits .-maxUniformBufferBindingSize)
          :max-storage-buffer-binding-size
          (some-> limits .-maxStorageBufferBindingSize)
          :max-texture-dimension-2d (some-> limits .-maxTextureDimension2D)
          :max-bind-groups (some-> limits .-maxBindGroups)
          :max-vertex-buffers (some-> limits .-maxVertexBuffers)}))))

(def ^:private format-bytes
  {"rgba8unorm" 4
   "bgra8unorm" 4
   "rgba8unorm-srgb" 4
   "bgra8unorm-srgb" 4
   "rgba16float" 8
   "rg16uint" 4
   "depth24plus" 4
   "depth32float" 4})

(defn texture-bytes
  "Price every allocated mip level and sample. Unknown formats fail closed."
  [format width height mip-level-count sample-count]
  (let [bytes-per-texel
        (or (get format-bytes format)
            (throw (ex-info "Texture format lacks byte pricing"
                            {:format format})))
        levels (max 1 (or mip-level-count 1))
        samples (max 1 (or sample-count 1))]
    (reduce + 0
            (map (fn [level]
                   (* (max 1 (quot (max 1 (or width 1))
                                   (bit-shift-left 1 level)))
                      (max 1 (quot (max 1 (or height 1))
                                   (bit-shift-left 1 level)))
                      bytes-per-texel
                      samples))
                 (range levels)))))
