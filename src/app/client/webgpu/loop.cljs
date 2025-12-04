(ns app.client.webgpu.loop
  (:require [app.client.webgpu.editor :as editor]))

(defn make-input-state []
  (atom {:scroll-y 0 :width 100 :height 100 :dpr 1 
         :auto-scroll? false}))

;; --- HUD & CONTROLS ---

(defn ensure-hud-element [!state toggle-auto-fn]
  (let [id "webgpu-hud"]
    (or (.getElementById js/document id)
        (let [div (.createElement js/document "div")
              btn (.createElement js/document "button")]
          
          (set! (.-id div) id)
          (set! (.-style div) "position:fixed;top:10px;left:10px;background:rgba(0,0,0,0.85);color:#0f0;padding:10px;font-family:monospace;font-weight:bold;z-index:99999;border:1px solid #333;display:flex;flex-direction:column;gap:10px;")
          
          (let [stats (.createElement js/document "div")]
             (set! (.-id stats) "webgpu-stats")
             (set! (.-innerText stats) "HUD READY")
             (.appendChild div stats))

          (set! (.-innerText btn) "🔥 Toggle Stress Test")
          (set! (.-style btn) "background:#333;color:white;border:1px solid #555;padding:5px;cursor:pointer;")
          (set! (.-onclick btn) toggle-auto-fn)
          (.appendChild div btn)

          (.appendChild js/document.body div)
          div))))

(defn update-stats! [cpu-ms mode]
  (when-let [el (.getElementById js/document "webgpu-stats")]
    (set! (.-innerText el) 
          (str "MODE:    " mode "\n"
               "CPU:     " (.toFixed cpu-ms 3) " ms\n"
               "(Core Render Logic Only)"))))

;; --- LOOP LOGIC ---

(defn configure-reactive-loop [node !state device ctx geometry]
  (let [!pending-frame (atom nil)
        !dragging?     (atom false)]
    
    ;; USE LETFN FOR RECURSION
    (letfn [(draw! []
              (reset! !pending-frame nil)
              
              (let [t0 (js/performance.now)
                    state-val @!state
                    {:keys [scroll-y width height auto-scroll?]} state-val
                    {:keys [camera-floats pass-descriptor]} (:pipelines geometry)
                    
                    ;; Auto-scroll logic
                    final-scroll-y (if auto-scroll? 
                                     (let [next-y (+ scroll-y 100)]
                                       (swap! !state assoc :scroll-y next-y)
                                       next-y) 
                                     scroll-y)]
                
                ;; 1. EXECUTE GPU COMMANDS
                (editor/draw-frame! device ctx 
                                    (:text geometry) (:rect geometry) 
                                    camera-floats pass-descriptor
                                    0 (- final-scroll-y) width height)
                
                ;; 2. METRICS
                (let [t1 (js/performance.now)
                      mode (if auto-scroll? "AUTO (Max FPS)" "REACTIVE (Input)")]
                  (update-stats! (- t1 t0) mode))

                ;; 3. RECUR (Only if auto-scrolling)
                ;; Now valid because letfn supports recursion
                (when (:auto-scroll? @!state)
                  (reset! !pending-frame (js/requestAnimationFrame draw!)))))

            (request-draw! []
              (when-not @!pending-frame
                (reset! !pending-frame (js/requestAnimationFrame draw!))))

            (toggle-stress-test []
              (let [on? (:auto-scroll? @!state)]
                (swap! !state assoc :auto-scroll? (not on?))
                (if (not on?)
                  (request-draw!)
                  (reset! !pending-frame nil))))]

      ;; --- SETUP LISTENERS ---
      
      (ensure-hud-element !state toggle-stress-test)
      
      (let [wheel-handler (fn [e]
                            (.preventDefault e)
                            (swap! !state update :scroll-y + (.-deltaY e))
                            (request-draw!))

            mousedown-handler (fn [e] (reset! !dragging? true))
            mouseup-handler   (fn [e] (reset! !dragging? false))
            
            mousemove-handler (fn [e]
                                (when @!dragging?
                                  (let [dy (.-movementY e)]
                                    (swap! !state update :scroll-y - dy)
                                    (request-draw!))))

            resize-handler (fn [_]
                             (let [win js/window
                                   dpr (or (.-devicePixelRatio win) 1)
                                   w   (max 1 (* (.-innerWidth win) dpr))
                                   h   (max 1 (* (.-innerHeight win) dpr))]
                               (swap! !state merge {:width w :height h :dpr dpr})
                               (set! (.-width node) w)
                               (set! (.-height node) h)
                               (request-draw!)))]

        ;; Init
        (resize-handler nil)
        
        (.addEventListener node "wheel" wheel-handler #js {:passive false})
        (.addEventListener node "mousedown" mousedown-handler)
        (.addEventListener js/window "mousemove" mousemove-handler)
        (.addEventListener js/window "mouseup" mouseup-handler)
        (.addEventListener js/window "resize" resize-handler #js {:passive true})

        ;; Cleanup
        (fn []
          (when @!pending-frame (js/cancelAnimationFrame @!pending-frame))
          (.removeEventListener node "wheel" wheel-handler)
          (.removeEventListener node "mousedown" mousedown-handler)
          (.removeEventListener js/window "mousemove" mousemove-handler)
          (.removeEventListener js/window "mouseup" mouseup-handler)
          (.removeEventListener js/window "resize" resize-handler)
          (some-> (.getElementById js/document "webgpu-hud") .remove))))))
