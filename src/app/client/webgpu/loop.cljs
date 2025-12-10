(ns app.client.webgpu.loop
  (:require [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [missionary.core :as m]
            [contrib.missionary-contrib :as mx]
            [app.client.webgpu.editor :as editor]))

;; --- 1. UTILS & HUD ---

(defn ensure-hud! [toggle-fn]
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
          (set! (.-onclick btn) toggle-fn)
          (.appendChild div btn)
          
          (.appendChild js/document.body div)
          div))))

(defn update-stats! [cpu-ms mode]
  (when-let [el (.getElementById js/document "webgpu-stats")]
    (set! (.-innerText el) 
          (str "MODE:    " mode "\n"
               "CPU:     " (.toFixed cpu-ms 3) " ms\n"
               "(Core Render Logic Only)"))))

;; --- 2. INPUT SIGNALS ---

(defn <canvas-size [node]
  (->> (m/observe
         (fn [!]
           (let [update-size (fn []
                               (let [win js/window
                                     dpr (or (.-devicePixelRatio win) 1)
                                     rect (.getBoundingClientRect node)
                                     w (max 1 (Math/floor (* (.-width rect) dpr)))
                                     h (max 1 (Math/floor (* (.-height rect) dpr)))]
                                 (set! (.-width node) w)
                                 (set! (.-height node) h)
                                 (! {:width w :height h :dpr dpr})))
                 
                 obs (new js/ResizeObserver update-size)]
             
             (update-size)
             (.observe obs node)
             #(.disconnect obs))))
       (m/relieve (fn [_old new] new))))

(defn >wheel-deltas [node]
  (m/observe
    (fn [!]
      (let [handler (fn [e]
                      (.preventDefault e)
                      (! (.-deltaY e)))]
        (.addEventListener node "wheel" handler #js {:passive false})
        #(.removeEventListener node "wheel" handler)))))

(defn >user-input-deltas [node]
  (m/observe
    (fn [!]
      (let [dragging? (volatile! false)
            
            down-h  (fn [e] (vreset! dragging? true))
            up-h    (fn [e] (vreset! dragging? false))
            
            move-h  (fn [e]
                      (when @dragging?
                        (! (- (.-movementY e)))))]

        (.addEventListener node "mousedown" down-h)
        (.addEventListener js/window "mouseup" up-h)
        (.addEventListener js/window "mousemove" move-h)

        (fn []
          (.removeEventListener node "mousedown" down-h)
          (.removeEventListener js/window "mouseup" up-h)
          (.removeEventListener js/window "mousemove" move-h))))))

(def >raf
  (m/observe 
    (fn [!]
      (let [active? (volatile! true)
            callback (fn loop [t]
                       (when @active?
                         (! t)
                         (js/requestAnimationFrame loop)))]
        (js/requestAnimationFrame callback)
        #(vreset! active? false)))))


(defn start-loop! [node device ctx geometry]
  
  (let [;; 1. STATE CONTAINER
        initial-state {:scroll-y 0 :width 100 :height 100 :dpr 1}
        !state        (atom initial-state)

        !auto-scroll? (atom false)
        toggle-fn     #(swap! !auto-scroll? not)
        _             (ensure-hud! toggle-fn)

        wheel-deltas (->> (>wheel-deltas node) (m/relieve +))
        input-deltas (->> (>user-input-deltas node) (m/relieve +))
        canvas-size  (<canvas-size node)]

    (m/join {}
      
      (->> (mx/mix
             (m/eduction (map (fn [x] [:resize x])) canvas-size)
             (m/eduction (map (fn [x] [:input x])) wheel-deltas)
             (m/eduction (map (fn [x] [:input x])) input-deltas))
           
           (m/reduce
             (fn [_ [type value]]
               (swap! !state
                      (fn [state]
                        (case type
                          :resize (merge state value)
                          :input  (update state :scroll-y + value)))))
             nil))

      (m/reduce
        (fn [_ state]
          (editor/draw-frame! device ctx 
                                (:text geometry) (:rect geometry) 
                                (:camera-floats (:pipelines geometry)) 
                                (:pass-descriptor (:pipelines geometry)) 
                                0 (- (:scroll-y state)) (:width state) (:height state))
            
          nil)
        nil
        (->> (m/sample identity (m/watch !state) >raf)
             (m/eduction (dedupe))))
      )))

(defn configure-reactive-loop [node _unused-state device ctx geometry]
  (start-loop! node device ctx geometry))
