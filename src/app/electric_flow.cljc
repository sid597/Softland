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


(defonce !editor-state (atom nil)) ;; Holds the compiled GPU pipelines
(defonce !gpu-format (atom nil))

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
(e/declare editor-state) 
(e/declare gpu-format)


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

(e/defn Init-Editor-State []
  (e/client
    (when (nil? editor-state)
      (println "⏳ Starting Async Init...")
      (load-resources)
      (when-not (some nil? [atlas-data font-bitmap])
        (let [gpu js/navigator.gpu
              adapter (e/Task (await-promise (.requestAdapter gpu)))
              device  (e/Task (await-promise (.requestDevice adapter)))
              ]

          ;; 3. The "Bingoing": Compile Shaders & Save State
          (when device
            (println "⚡ GPU & Files Ready. Compiling Shaders...")
            (js/console.log "ff" gpu "--" adapter "--" device "--" atlas-data "--" font-bitmap)
            (let [format (.getPreferredCanvasFormat gpu)
                  ;; This is the HEAVY function from your editor ns
                  final-state (editor/init-text-system 
                                device 
                                format 
                                atlas-data
                                font-bitmap
                                )]
              (js/console.log "GOT final state" final-state)

              ;; Save global refs needed for rendering later
              (reset! !device device)

              ;; Save the compiled pipeline state!
              (reset! !editor-state final-state)
              (println "✅ Init Complete. State saved."))))))))

(defn layout-lines [start-y line-gap lines]
  (let [calc-next (fn [{:keys [y current-h]} line]
                    (let [size    (:size line)
                          ;; Crude metric: Cap height is roughly 70% of font size
                          ;; Line height usually 1.2x font size
                          height  (* size 1.2)
                          new-y   (if y 
                                    (- y current-h line-gap) ;; Move UP (since 0,0 is bottom-left in WebGPU usually, but check your camera)
                                    ;; If your Y=0 is TOP-LEFT (standard UI), use (+ y current-h line-gap)
                                    ;; Based on your previous code, let's assume Y grows downwards or we adjust manually.
                                    ;; Let's assume Standard UI: Y increases going DOWN.
                                    (+ y height line-gap)
                                    )]
                      
                      {:y new-y
                       :current-h height
                       :lines (conj (:lines line) (assoc line :y start-y))}))]
    
    ;; Simple reducer to stack them
    (first (reduce (fn [[acc-y final-lines] line]
                     (let [size   (:size line)
                           height (* size 1.2) ;; Standard Line Height
                           this-y acc-y
                           next-y (+ acc-y height line-gap)]
                       [next-y (conj final-lines (assoc line :y this-y))]))
                   [start-y []]
                   lines))))


(e/defn WebGPU-Render-Logic []
  (e/client

    ;; Only render if we have the canvas AND the compiled state
    (js/console.log "WebGPU-Render-Logic" canvas device editor-state)
    (when-not (some nil? [canvas device editor-state]) 
      (js/console.log "WebGPU-Render-Logic")

      (let [context (.getContext canvas "webgpu" (clj->js {:alpha true}))
            gpu     js/navigator.gpu
            format  (.getPreferredCanvasFormat gpu)
            w       (js/Math.ceil width)
            h       (js/Math.ceil height)
            configured? (do 
                          (.configure context (clj->js {:device device
                                                        :format format
                                                        :width  w
                                                        :height h}))
                          true)]

        ;; 2. Update Geometry (CPU Math - Fast)
        ;; We pass the *existing* editor-state to reuse buffers

        (when configured?
          (println "configured" w h)
          (let [raw-content [{:text "Paragraph text is smaller (64px)" :size 64}
                             {:text "Paragraph text is smaller (32px)" :size 32}
                             {:text "Paragraph text is smaller (19px)" :size 19}
                             {:text "Paragraph text is smaller (17px)" :size 14}]
                
                ;; Calculate Y positions automatically starting at Y=100 with 10px gap
                [_ stacked-lines] (reduce (fn [[current-y lines] line]
                                            (let [fsize (:size line)
                                                  ;; Move Y down by line-height (e.g. 1.2x font size)
                                                  next-y (+ current-y (* fsize 1.2))] 
                                              [next-y (conj lines (assoc line :x 50 :y current-y))]))
                                          [100.0 []] ;; Start Y
                                          raw-content)

                updated-state (editor/update-text-data 
                                device 
                                editor-state 
                                stacked-lines
                                atlas-data 
                                19)

                ;; 3. Camera
                camera {:pan-x 0.0 :pan-y 0.0 :zoom 1.0 
                        :width w :height h}]
            (println "stacked-lines" stacked-lines)

            ;; 4. Draw
            ;; WHY REQUEST_ANIMATION_FRAME?
            ;; WebGPU renders to a texture, but the Browser controls the presentation.
            ;; When the window resizes, the browser clears the <canvas> bitmap.
            ;; If we draw synchronously, the browser might clear the canvas AFTER our draw
            ;; but BEFORE the screen refresh, resulting in a black screen (race condition).
            ;; requestAnimationFrame aligns our draw call to the start of the next VSync,
            ;; guaranteeing it executes after the browser's layout/clear pass is finished.
            (let [draw-cmd (fn [] 
                             (editor/draw-text device context updated-state camera))]
              
              (js/requestAnimationFrame draw-cmd))
            ))))))


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
              editor-state (e/watch !editor-state)
              gpu-format (e/watch !gpu-format)
              dpr (e/watch !dpr)]
      (println "moin")

      (dom/style {:margin "0" 
                  :padding "0" 
                  :width "100vw" 
                  :height "100vh" 
                  :overflow "hidden" 
                  :background "black"})

      (Init-Editor-State)
      (dom/On js/window "resize" 
              (fn [_]
                (let [dpr (.-devicePixelRatio js/window)
                      w   (.-innerWidth js/window)
                      h   (.-innerHeight js/window)]
                  (reset! !dpr dpr)
                  (reset! !width (* dpr w))
                  (reset! !height (* dpr h))))
              nil  ;; Initial value (ignored for side effects)
              {})
      ;; 2. CALCULATE DIMENSIONS from Window (More robust than measuring DOM)
      (let [dpr   (.-devicePixelRatio js/window)
            win-w (.-innerWidth js/window)
            win-h (.-innerHeight js/window)]

        (reset! !width (* dpr win-w))
        (reset! !height (* dpr win-h))
        (println "dpr" dpr win-h win-w "--" height width) 

        (dom/canvas
          (dom/props {:id "webgpu-canvas"
                      :width (e/watch !width)
                      :height (e/watch !height)
                      ;; Canvas fills the window
                      :style {:width "100vw" :height "100vh" :display "block"}})

          (reset! !canvas dom/node)
          (WebGPU-Render-Logic))))))
