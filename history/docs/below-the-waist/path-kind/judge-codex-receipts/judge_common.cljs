(ns judge-common
  (:require [app.client.engine.device :as device]
            [app.client.engine.transform :as transform]
            [app.client.harness.shared :as shared]))

(defn transforms []
  (transform/world-transforms
   (transform/add-group (transform/empty-registry) 17 {:camera :screen})))

(defn capture! [gpu system item zoom worlds prepare draw label points]
  (device/write-groups! gpu (:groups-buffer system) worlds)
  (device/update-camera gpu (:camera-buffer system) (js/Float32Array. 6) 0 0 zoom 128 128)
  (let [stats (prepare system [item] zoom worlds)
        target (.createTexture gpu #js {:size #js [128 128 1] :format "rgba8unorm" :viewFormats #js ["rgba8unorm-srgb"]
                                        :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT js/GPUTextureUsage.COPY_SRC)})
        read (.createBuffer gpu #js {:size 65536 :usage (bit-or js/GPUBufferUsage.MAP_READ js/GPUBufferUsage.COPY_DST)})
        encoder (.createCommandEncoder gpu)
        pass (.beginRenderPass encoder #js {:colorAttachments #js [#js {:view (.createView target #js {:format "rgba8unorm-srgb"})
                                                                       :clearValue #js {:r 0 :g 0 :b 0 :a 0}
                                                                       :loadOp "clear" :storeOp "store"}]})]
    (draw pass system stats)
    (.end pass)
    (.copyTextureToBuffer encoder #js {:texture target} #js {:buffer read :bytesPerRow 512 :rowsPerImage 128} #js [128 128 1])
    (.submit (.-queue gpu) #js [(.finish encoder)])
    (-> (.mapAsync read js/GPUMapMode.READ)
        (.then (fn [_]
                 (let [bytes (js/Uint8Array. (js/Uint8Array. (.getMappedRange read)))
                       result {:label label :stats (select-keys stats [:runs :packs :instances :instance-writes :derived :packed :writes :changed?])
                               :samples (mapv (fn [[x y]] {:pixel [x y] :rgba (shared/pixel-rgba bytes x y)
                                                         :alpha (/ (aget bytes (+ 3 (* 4 (+ x (* y 128))))) 255.0)}) points)
                               :file (str label ".png") :png-data-url (shared/opaque-png-data-url bytes)}]
                   (.unmap read) (.destroy read) (.destroy target)
                   result))))))

(defn start! [run]
  (-> (.requestAdapter js/navigator.gpu)
      (.then (fn [adapter]
               (-> (.requestDevice adapter)
                   (.then (fn [gpu]
                            (-> (run gpu)
                                (.then (fn [result]
                                         (.destroy gpu)
                                         (assoc result :adapter (shared/adapter-information adapter))))))))))
      (.then (fn [result]
               (set! (.-__renderVerifierResult js/window) (clj->js result))
               (set! (.-__renderVerifierDone js/window) true)))
      (.catch (fn [error]
                (set! (.-__renderVerifierResult js/window) #js {:fatal (str error) :stack (.-stack error)})
                (set! (.-__renderVerifierDone js/window) true)))))
