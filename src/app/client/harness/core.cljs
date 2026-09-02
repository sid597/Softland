(ns app.client.harness.core
     "The compiled browser-harness entry and result assembler.
      Takes: nothing; it acquires WebGPU and font assets itself.
      Gives: window.__renderVerifierResult and completion state.
      Holds nothing."
     (:require [app.client.engine.device :as device]
               [app.client.text.fonts :as fonts]
               [app.client.harness.image :as image]
               [app.client.harness.path :as path]
               [app.client.harness.region :as region]
               [app.client.harness.text :as text]
               [app.client.harness.shared
:refer [canvas-size color-format glyph-screen-x glyph-screen-baseline
        glyph-screen-size zoom-cases image-fixtures promise-mapv
        bytes->hex sha256-bytes sha256-string opaque-png-data-url
        q8-world-transforms run-q8-transport! boundary-pixels byte-delta pixel-rgba
        srgb->linear linear->srgb-byte selected-limits adapter-information
        shader-digests w4-read-texture!]]))

(defn ^:export run-harness! []
  (js/console.log "[W0-A] init-start")
  (when-not (and (.-isSecureContext js/window)
                 (exists? js/navigator.gpu))
    (throw (js/Error. "W0-A requires a secure origin with WebGPU")))
  (-> (.requestAdapter js/navigator.gpu)
      (.then
       (fn [^js adapter]
         (when-not adapter
           (throw (js/Error. "W0-A could not acquire a WebGPU adapter")))
         (js/console.log "[W0-A] init-adapter")
         (-> (.requestDevice adapter)
             (.then
              (fn [^js device]
                (js/console.log "[W0-A] init-device")
                (-> (fonts/load-font-manifest-async)
                    (.then
                     (fn [manifest]
                       (js/console.log "[W0-A] init-font-manifest")
                       (let [font-config (first (filter #(= "dejavu-sans-mono" (:id %))
                                                       (:fonts manifest)))
                             t1-font-config (first (filter #(= "ubuntu-sans-variable" (:id %))
                                                          (:fonts manifest)))]
                         (when-not (and font-config t1-font-config)
                           (throw (js/Error. "A harness font is absent from manifest")))
                         (-> (js/Promise.all
                               #js [(fonts/load-font-assets font-config)
                                    (fonts/load-font-assets t1-font-config)])
                             (.then
                              (fn [font-values]
                                (let [slug-assets (aget font-values 0)
                                      t1-assets (aget font-values 1)
                                      t1-evidence
                                      (try
                                        (text/t1-layout-evidence
                                         (:layout-provider t1-assets))
                                        (catch :default error
                                          (assoc (or (ex-data error) {})
                                                 :pass false
                                                 :foreign-failure
                                                 "T1 browser layout evidence failed.")))]
                                (js/console.log "[W0-A] init-font-assets")
                                (let [camera-buffer (device/create-camera-buffer device)
                                      groups-buffer (device/create-groups-buffer device)
                                      q8-transport (run-q8-transport! device groups-buffer)
                                      _ (js/console.log "[W0-A] init-shared-buffers")
                                      text-slug
                                      (text/run-text-slug!
                                       device camera-buffer groups-buffer
                                       slug-assets t1-assets)]
                                  (-> (js/Promise.all
#js [text-slug
     (shader-digests)
     (image/run-image-step! device)
     (path/run-path-step! device)
     (region/run-region3d-floor! device t1-assets)
     (js/Promise.resolve
      (text/run-text-flat-route! slug-assets t1-assets))])
                                      (.then
                                       (fn [values]
                                         {:schema-version 2
                                          :harness "softland-render-engine-w0-a"
                                          :production-renderer? true
                                          :product-server-used? false
                                          :secure-context? (.-isSecureContext js/window)
                                          :user-agent (.-userAgent js/navigator)
                                          :adapter (adapter-information adapter)
                                          :device-limits (selected-limits (.-limits device))
                                          :canvas {:width canvas-size
                                                   :height canvas-size
                                                   :device-pixel-ratio (.-devicePixelRatio js/window)
                                                   :color-format color-format}
                                          :font {:id (:id font-config)
                                                 :slug (:slug font-config)}
                                          :decoded-slug-curve-count (:decoded-slug-curve-count (aget values 0))
                                          :shader-digests (aget values 1)
                                          :q8-transport q8-transport
                                          :t1-layout t1-evidence
                                          :ubuntu-slug (:ubuntu-slug (aget values 0))
                                          :image-step (aget values 2)
                                          :path-step (aget values 3)
                                          :region3d-floor (aget values 4)
                                          :text-flat-route (aget values 5)
                                          :cases (:cases (aget values 0))})))))))))))))))))))

(defn ^:export run-region3d-floor-harness! []
  (when-not (and (.-isSecureContext js/window)
                 (exists? js/navigator.gpu))
    (throw (js/Error. "Region3D floor harness requires WebGPU")))
  (-> (.requestAdapter js/navigator.gpu)
      (.then
       (fn [^js adapter]
         (when-not adapter
           (throw (js/Error. "Region3D floor harness has no adapter")))
         (-> (.requestDevice adapter)
             (.then
              (fn [^js device]
                (-> (fonts/load-font-manifest-async)
                    (.then
                     (fn [manifest]
                       (let [font-config
                             (first (filter #(= "ubuntu-sans-variable" (:id %))
                                            (:fonts manifest)))]
                         (when-not font-config
                           (throw (js/Error. "Region3D harness font is absent")))
                         (-> (fonts/load-font-assets font-config)
                             (.then
                              (fn [font-assets]
                                (-> (js/Promise.all
                                     #js [(shader-digests)
                                          (region/run-region3d-floor! device font-assets)])
                                    (.then
                                     (fn [values]
                                       {:schema-version 2
                                        :harness "softland-region3d-floor"
                                        :secure-context? (.-isSecureContext js/window)
                                        :user-agent (.-userAgent js/navigator)
                                        :adapter (adapter-information adapter)
                                        :device-limits
                                        (selected-limits (.-limits device))
                                        :canvas {:width canvas-size
                                                 :height canvas-size
                                                 :device-pixel-ratio
                                                 (.-devicePixelRatio js/window)
                                                 :color-format color-format}
                                        :shader-digests (aget values 0)
                                        :region3d-floor (aget values 1)})))))))))))))))))

(defn ^:export start! []
  (js/console.log "[W0-A] start")
  (set! (.-__renderVerifierDone js/window) false)
  ;; Yield once so CDP can publish the boot marker before any browser/driver
  ;; implementation performs synchronous pipeline compilation.
  (js/setTimeout
   (fn []
     (js/console.log "[W0-A] scheduled-callback")
     (try
       (let [params (js/URLSearchParams. (.-search js/location))
             runner (if (.has params "region3d-floor-only")
                      run-region3d-floor-harness!
                      run-harness!)]
       (-> (runner)
           (.then
            (fn [result]
              (set! (.-__renderVerifierResult js/window) (clj->js result))
              (set! (.-__renderVerifierDone js/window) true)))
           (.catch
            (fn [error]
              (set! (.-__renderVerifierResult js/window)
                    #js {:fatal (str error)
                         :stack (.-stack error)})
              (set! (.-__renderVerifierDone js/window) true)))))
       (catch :default error
         (set! (.-__renderVerifierResult js/window)
               #js {:fatal (str error)
                    :stack (.-stack error)})
         (set! (.-__renderVerifierDone js/window) true))))
   0))
