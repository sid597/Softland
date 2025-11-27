(ns app.electric-flow
  (:require [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [hyperfiddle.electric-svg3]
            #?@(:cljs [[app.client.webgpu.editor :as editor]
                       [global-flow :refer [await-promise
                                            mouse-down?>
                                            !canvas
                                            !font-bitmap
                                            !text-renderer
                                            !global-atom
                                            !device
                                            !context
                                            !atlas-data
                                            !width
                                            !height
                                            !canvas-y
                                            !dpr
                                            !canvas-x
                                            ]]])))


(hyperfiddle.rcf/enable!)


(e/declare canvas)
(e/declare adapter)
(e/declare device)
(e/declare context)
(e/declare width)
(e/declare height)
(e/declare canvas-y)
(e/declare canvas-x)
(e/declare global-atom)
(e/declare font-bitmap)
(e/declare atlas-data)
(e/declare dpr)
(e/declare text-renderer)


(e/defn Mouse-down-cords [node] (e/input (mouse-down?> node)))
    
(e/defn Tap-diffs
  ([f! x] 
   (f! (e/input (e/pure x)))
   x)
  ([x] (Tap-diffs prn x)))


#?(:cljs (defn load-bitmap-file []
           (println "Load bitmap file")
           (-> (js/fetch   "/font_atlas.png")
               (.then #(.blob %))
               (.then #(js/createImageBitmap %))
               (.then (fn [img]
                         (reset! !font-bitmap img))))))
                            
#?(:cljs
   (defn read-json-file []
     (-> (js/fetch "/font_atlas.json")
         (.then (fn [response]
                  (.json response)))
         (.then (fn [data]
                  (reset! !atlas-data (js->clj data :keywordize-keys true)))))))


;; --- Resource Loaders ---
#?(:cljs (defn load-resources []
           (println "Loading resources...")
           (-> (js/fetch "/font_atlas.png")
               (.then #(.blob %))
               (.then #(js/createImageBitmap %))
               (.then #(reset! !font-bitmap %)))
           (-> (js/fetch "/font_atlas.json")
               (.then #(.json %))
               (.then #(reset! !atlas-data (js->clj % :keywordize-keys true))))))


(e/defn WebGPU-Static-Render []
  (e/client
    (let [canvas      (e/watch !canvas)
          font-bitmap (e/watch !font-bitmap)
          atlas-data  (e/watch !atlas-data)
          width       (e/watch !width)
          height      (e/watch !height)]

      (when (and canvas font-bitmap atlas-data (> width 0) (> height 0))
        (let [gpu js/navigator.gpu
              adapter (e/Task (await-promise (.requestAdapter gpu)))
              device  (e/Task (await-promise (.requestDevice adapter)))]
          
          (when (and adapter device)
            (let [context (.getContext canvas "webgpu" (clj->js {:alpha true}))
                  format  (.getPreferredCanvasFormat gpu)
                  
                  ;; Use the atoms (which are now full-screen)
                  w (js/Math.ceil width)
                  h (js/Math.ceil height)
                  
                  configured? (do 
                                (.configure ^js context (clj->js {:device device 
                                                                  :format format
                                                                  :width w
                                                                  :height h}))
                                true)]

              (when configured?
                ;; Snapshot values to render
                (let [dv   (e/snapshot device)
                      ctx  (e/snapshot context)
                      fmat (e/snapshot format)
                      atl  (e/snapshot atlas-data)
                      bmp  (e/snapshot font-bitmap)
                      snap-w (e/snapshot w)
                      snap-h (e/snapshot h)]

                  (let [init-state (editor/init-text-system dv fmat atl bmp)
                        
                        ;; Text at 100, 100 should now be clearly visible
                        render-state (editor/update-text-data dv init-state 
                                       [{:text "HELLO FULL SCREEN!" :x 100 :y 100}] atl 64)
                        
                        camera {:pan-x 0.0 :pan-y 0.0 :zoom 1.0 
                                :width snap-w :height snap-h}]
                    
                    (println "Final Draw. Full Screen Size:" snap-w "x" snap-h)
                    (editor/draw-text dv ctx render-state camera)))))))))))

(e/defn main [ring-request]
  (e/client
    (binding [dom/node js/document.body
              canvas (e/watch !canvas)
              canvas-x (e/watch !canvas-x)
              canvas-y (e/watch !canvas-y)
              height (e/watch !height)
              width (e/watch !width)
              device (e/watch !device)
              context (e/watch !context)
              global-atom (e/watch !global-atom)
              font-bitmap (e/watch !font-bitmap)
              atlas-data (e/watch !atlas-data)
              text-renderer (e/watch !text-renderer)
              dpr (e/watch !dpr)]

(dom/style {:margin "0" 
                  :padding "0" 
                  :width "100vw" 
                  :height "100vh" 
                  :overflow "hidden" 
                  :background "black"})

      ;; 2. CALCULATE DIMENSIONS from Window (More robust than measuring DOM)
      (let [dpr   (.-devicePixelRatio js/window)
            win-w (.-innerWidth js/window)
            win-h (.-innerHeight js/window)]
        
        (reset! !width (* dpr win-w))
        (reset! !height (* dpr win-h))

        (dom/canvas
          (dom/props {:id "webgpu-canvas"
                      :width (e/watch !width)
                      :height (e/watch !height)
                      ;; Canvas fills the window
                      :style {:width "100vw" :height "100vh" :display "block"}})
          
          (reset! !canvas dom/node)
          (load-resources)
          (WebGPU-Static-Render))))))
