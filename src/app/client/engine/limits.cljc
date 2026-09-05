(ns app.client.engine.limits
  "Expose selected limits and price texture storage.

   Input: adapter/device or format/dimensions. Output: selected limits or
   nominal byte cost. There is no state. format-bytes is a deliberately
   finite pricing table; unknown formats throw rather than being assigned an
   invented cost.

   Folder map: README.md.")

#?(:clj
   (defn adapter-limits
     "Adapter/device → nil on JVM; selected WebGPU limits on CLJS, or nil
      for absent input.

      Explicit platform boundary. JVM code must inject limits rather than
      assume hardware access."
     [_adapter-or-device] nil)
   :cljs
   (defn adapter-limits
     "Adapter/device → nil on JVM; selected WebGPU limits on CLJS, or nil
      for absent input.

      Explicit platform boundary. JVM code must inject limits rather than
      assume hardware access."
     [^js adapter-or-device]
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
  "Format, width, height, mip count, sample count → nominal total bytes;
   unknown format throws.

   Sums downscaled mip dimensions times format width and samples. Serves as
   pool accounting; it does not measure driver allocation overhead."
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
