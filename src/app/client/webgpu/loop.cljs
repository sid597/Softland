(ns app.client.webgpu.loop
  (:require [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [missionary.core :as m]
            [contrib.missionary-contrib :as mx]
            [app.client.webgpu.editor :as editor]))

(defn <canvas-size [node]
  (->> (m/observe
         (fn [!]
           (let [update-size (fn []
                               (let [win js/window
                                     dpr (or (.-devicePixelRatio win) 1)
                                     rect (.getBoundingClientRect node)
                                     w (max 1 (Math/floor (* (.-width rect) dpr)))
                                     h (max 1 (Math/floor (* (.-height rect) dpr)))]
                                 (when (or (not= (.-width node) w)
                                           (not= (.-height node) h))
                                   (set! (.-width node) w)
                                   (set! (.-height node) h))
                                 (! {:width w :height h :dpr dpr})))
                 
                 obs (new js/ResizeObserver update-size)]
             
             (update-size)
             (.observe obs node)
             #(.disconnect obs))))
       (m/relieve (fn [_old new] new))))

(defn <window-metrics []
  (->> (m/observe
         (fn [!]
           (let [handler (fn []
                           (! {:width  js/window.innerWidth
                               :height js/window.innerHeight
                               :dpr    (or js/window.devicePixelRatio 1)}))]
             (js/window.addEventListener "resize" handler)
             (handler) ;; Fire immediately
             #(js/window.removeEventListener "resize" handler))))
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
  
  (let [initial-state {:scroll-y 0 
                       :width (.-innerWidth js/window) 
                       :height (.-innerHeight js/window) 
                       :dpr (or (.-devicePixelRatio js/window) 1)}
        !state        (atom initial-state)
        wheel-deltas (->> (>wheel-deltas node) (m/relieve +))
        input-deltas (->> (>user-input-deltas node) (m/relieve +))
        window-metrics (<window-metrics)
        ]

    (m/join {}
      
      (->> (mx/mix
             (m/eduction 
               (map (fn [{:keys [width height dpr] :as m}]
                      ;; Explicitly size the canvas buffer to match screen pixels
                      (set! (.-width node)  (Math/floor (* width dpr)))
                      (set! (.-height node) (Math/floor (* height dpr)))
                      [:resize m])) 
               window-metrics)
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
        (->> (m/sample (fn [s _t] s) (m/watch !state) >raf)
             (m/eduction (dedupe))))
      )))
