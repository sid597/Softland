(ns app.electric-flow
  (:import (hyperfiddle.electric Failure Pending FailureInfo))
  (:require [hyperfiddle.electric-de :as e :refer [$]]
            [missionary.core :as m]
            [hyperfiddle.electric-dom3 :as dom]))

(hyperfiddle.rcf/enable!)

(defn await-promise
  "Returns a task completing with the result of given promise"
  [p]
  (let [v (m/dfv)]
    (.then p
      #(v (fn [] %))
      #(v (fn [] (throw %))))
    (m/absolve v)))


#?(:cljs (defonce !canvas (atom nil)))
#?(:cljs (defonce !device (atom nil)))
#?(:cljs (defonce !context (atom nil)))
#?(:cljs (defonce !gpu (atom nil)))
#?(:cljs (defonce !config (atom nil)))

(e/defn init-webgpu []
  (e/client
    (when (some? (e/watch !canvas))
      (let [gpu      js/navigator.gpu
            adapter  ($ e/Task (await-promise (.requestAdapter gpu)))
            device   ($ e/Task (await-promise (.requestDevice adapter)))]
        (reset! !device device)
        (reset! !config {:format (.getPreferredCanvasFormat gpu)
                         :device device
                         :alphaMode "opaque"
                         :usage js/GPUTextureUsage.RENDER_ATTACHMENT})))))

#?(:cljs
   (defn run-webgpu [context device config canvas]
     (.configure context (clj->js config))
     (let [color-texture (.getCurrentTexture context)
           color-texture-view (.createView color-texture)
           color-attachment (clj->js {:view color-texture-view
                                      :clearValue (clj->js {:r 1 :g 0 :b 0 :a 1})
                                      :loadOp "clear"
                                      :storeOp "store"})
           render-pass-desc (clj->js {:colorAttachments (clj->js [color-attachment])})
           command-encoder (.createCommandEncoder device)
           pass-encoder (.beginRenderPass command-encoder render-pass-desc)]
       (println "color texture" color-texture)
       (.setViewport pass-encoder 0 0 (.-width canvas) (.-height canvas) 0 1)
       (.end pass-encoder)
       (.submit (.-queue device) [(.finish command-encoder)]))))

(e/defn setup-webgpu []
  (let [canvas (e/watch !canvas)
        context (e/watch !context)
        device (e/watch  !device)
        config (e/watch !config)]
    (when canvas
       (js/console.log canvas)
       (reset! !context (.getContext canvas "webgpu")))
    (when context
      (js/console.log context)
      ($ init-webgpu))
    (when (and config device context)
      (run-webgpu context device config canvas))))

(e/defn canvas-view []
  (dom/canvas
    (dom/props {:id "top-canvas"
                :width 700
                :height 800
                :style {:width "700px"  ; Change this
                        :height "800px" ; And this
                        :border "1px solid black"}})
    (reset! !canvas dom/node)))

(e/defn main [ring-request]
  (e/client
    (binding [dom/node js/document.body]
         ($ canvas-view)
         ($ setup-webgpu))))








