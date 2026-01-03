(ns app.client.webgpu.loop
  (:require [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [missionary.core :as m]
            [contrib.missionary-contrib :as mx]
            [app.client.webgpu.editor :as editor]))

(defn <window-metrics []
  (->> (m/observe
         (fn [!]
           (let [handler (fn []
                           (! {:width  js/window.innerWidth
                               :height js/window.innerHeight
                               :dpr    (or js/window.devicePixelRatio 1)}))]
             (js/window.addEventListener "resize" handler)
             (handler) 
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

(defn >mouse-events [node]
  (m/observe
    (fn [!]
      (let [get-coords (fn [e] 
                         (let [rect (.getBoundingClientRect node)]
                           {:x (- (.-clientX e) (.-left rect))
                            :y (- (.-clientY e) (.-top rect))}))
            
            down-h (fn [e] (! [:mousedown (get-coords e)]))
            up-h   (fn [e] (! [:mouseup (get-coords e)]))
            move-h (fn [e] (! [:mousemove (get-coords e)]))]

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

(def >blink-timer
  "Emits true/false every 530ms for caret blinking"
  (m/ap
    (loop []
      (m/amb true
             (do (m/? (m/sleep 530))
                 (m/amb false
                        (do (m/? (m/sleep 530))
                            (recur))))))))

(defn start-loop! [node device ctx geometry line-lengths]
  (let [;; Layout Configuration (Must match Editor defaults)
        font-size 16
        layout-x  50
        layout-y  100
        line-h    (* font-size 1.2)

        initial-state {:scroll-y      0
                       :width         (.-innerWidth js/window)
                       :height        (.-innerHeight js/window)
                       :dpr           (or (.-devicePixelRatio js/window) 1)
                       :dragging?     false
                       :sel-start     nil
                       :sel-end       nil
                       :caret-visible true}
        
        !state        (atom initial-state)
        !rect-sys     (atom (:rect geometry))
        
        ;; Event Streams
        wheel-deltas  (->> (>wheel-deltas node) (m/relieve +))
        mouse-events  (->> (>mouse-events node) (m/relieve (fn [_ x] x))) 
        window-metrics (<window-metrics)]

    (m/join {}
      
      ;; 1. STATE REDUCER
      (->> (mx/mix
             (m/eduction (map (fn [m] [:resize m])) window-metrics)
             (m/eduction (map (fn [x] [:wheel x])) wheel-deltas)
             (m/eduction (map (fn [v] [:blink v])) >blink-timer)
             mouse-events)

           (m/reduce
             (fn [_ [type value]]
               (swap! !state
                      (fn [state]
                        (case type
                          :resize (let [{:keys [width height dpr]} value]
                                    (set! (.-width node)  (Math/floor (* width dpr)))
                                    (set! (.-height node) (Math/floor (* height dpr)))
                                    (merge state value))

                          :wheel  (update state :scroll-y + value)

                          :blink  (assoc state :caret-visible value)

                          :mousedown
                          (let [{:keys [x y]} value
                                adj-y (+ y (:scroll-y state))
                                pos   (editor/hit-test x adj-y font-size layout-x layout-y line-h line-lengths)]
                            (assoc state :dragging? true :sel-start pos :sel-end pos :caret-visible true))

                          :mousemove
                          (if (:dragging? state)
                            (let [{:keys [x y]} value
                                  adj-y (+ y (:scroll-y state))
                                  pos   (editor/hit-test x adj-y font-size layout-x layout-y line-h line-lengths)]
                              (assoc state :sel-end pos))
                            state)

                          :mouseup
                          (assoc state :dragging? false)

                          state))))
             nil))

      ;; 2. SELECTION & CARET GEOMETRY UPDATER
      (->> (m/watch !state)
           (m/eduction
             (map (fn [s] {:sel-start (:sel-start s)
                           :sel-end (:sel-end s)
                           :caret-visible (:caret-visible s)}))
             (dedupe))
           (m/reduce
             (fn [_ {:keys [sel-start sel-end caret-visible]}]
               (let [has-selection? (and sel-start sel-end
                                         (not (and (= (:line sel-start) (:line sel-end))
                                                   (= (:col sel-start) (:col sel-end)))))
                     rects (if has-selection?
                             ;; Selection mode: show selection rects
                             (editor/calculate-selection-rects
                               sel-start sel-end
                               font-size layout-x layout-y line-h
                               line-lengths)
                             ;; Caret mode: show blinking caret
                             (if-let [caret-rect (editor/calculate-caret-rect
                                                   sel-start font-size layout-x layout-y line-h
                                                   caret-visible)]
                               [caret-rect]
                               []))]
                 (reset! !rect-sys (editor/update-rects device (:rect geometry) rects)))
               nil)
             nil))

      ;; 3. RENDER LOOP (just draws, no rect calculation)
      (m/reduce
        (fn [_ state]
          (editor/draw-frame! device ctx 
                              (:text geometry) 
                              @!rect-sys
                              (:camera-floats (:pipelines geometry)) 
                              (:pass-descriptor (:pipelines geometry)) 
                              0 (- (:scroll-y state)) (:width state) (:height state))
          nil)
        nil
        (m/sample (fn [s _t] s) (m/watch !state) >raf)))))
