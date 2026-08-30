(ns app.client.engine.limits
  "Static WebGPU limits and texture-allocation pricing.
   Takes: a WebGPU adapter; a texture format; texture dimensions, pixel size,
   mip count, and sample count.
   Gives: an adapter-limit snapshot, bytes per texel for a format, and the
   bytes reserved by a texture.
   Holds: nothing.")

(defn snapshot-adapter-limits [^js adapter]
  (when adapter
    (let [limits (.-limits adapter)]
      {:max-buffer-size (some-> limits .-maxBufferSize)
       :max-storage-buffer-binding-size (some-> limits .-maxStorageBufferBindingSize)
       :max-texture-dimension-2d (some-> limits .-maxTextureDimension2D)
       :max-texture-array-layers (some-> limits .-maxTextureArrayLayers)})))

(defn texture-bytes-per-pixel
  "Bytes per texel for a WebGPU texture format; unknown formats price as 4."
  [format]
  (case format
    ("rgba8unorm" "bgra8unorm" "rgba8unorm-srgb" "bgra8unorm-srgb") 4
    "rgba16float" 8
    "rg16uint" 4
    4))

(defn texture-reserved-bytes
  "Price every allocated mip level and MSAA sample, not only level zero."
  ([width height depth-or-array-layers bytes-per-pixel mip-level-count]
   (texture-reserved-bytes width height depth-or-array-layers bytes-per-pixel
                           mip-level-count 1))
  ([width height depth-or-array-layers bytes-per-pixel mip-level-count
    sample-count]
   (let [depth (max 1 (or depth-or-array-layers 1))
         levels (max 1 (or mip-level-count 1))
         samples (max 1 (or sample-count 1))]
     (reduce + 0
             (map (fn [level]
                    (* (max 1 (quot (max 1 (or width 1))
                                    (bit-shift-left 1 level)))
                       (max 1 (quot (max 1 (or height 1))
                                    (bit-shift-left 1 level)))
                       depth bytes-per-pixel samples))
                  (range levels))))))
