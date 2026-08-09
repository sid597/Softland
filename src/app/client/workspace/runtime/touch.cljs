(ns app.client.workspace.runtime.touch
  "Touch → the existing mouse/wheel event grammar.

   Dispatches native synthetic MouseEvent/WheelEvent on the canvas so the
   >mouse/>wheel flows and every consumer behind them stay untouched:
   - one finger: press/drag/tap — the ground pointer machine decides the
     verb exactly as on desktop (pan on empty ground, block drag on a
     block, text select on text, tap acts on release)
   - two fingers: pinch zoom at the centroid, synthesized as wheel deltas
     matching :camera/zoom-at-pointer's 1.0015^-dy factor")

;; ln 1.0015 — inverse of handle-wheel!'s zoom factor, so a pinch scale of
;; s maps to the dy whose 1.0015^-dy equals s.
(def ^:private wheel-zoom-ln 0.0014989)

(defn- dispatch-mouse! [node type x y]
  (.dispatchEvent node
    (js/MouseEvent. type
      #js {:bubbles true :cancelable true :view js/window
           :clientX x :clientY y :button 0})))

(defn- dispatch-wheel! [node x y dy]
  (.dispatchEvent node
    (js/WheelEvent. "wheel"
      #js {:bubbles true :cancelable true :view js/window
           :clientX x :clientY y :deltaX 0 :deltaY dy})))

(defn- txy [t] [(.-clientX t) (.-clientY t)])

(defn- pinch-geom [ts]
  (let [[x0 y0] (txy (aget ts 0))
        [x1 y1] (txy (aget ts 1))]
    {:cx (/ (+ x0 x1) 2)
     :cy (/ (+ y0 y1) 2)
     :dist (js/Math.hypot (- x1 x0) (- y1 y0))}))

(defn install-touch-adapter!
  "Idempotent per node — safe under hot reload."
  [node]
  (when-not (.-__softlandTouchAdapter node)
    (set! (.-__softlandTouchAdapter node) true)
    ;; stop Safari from claiming the gestures (scroll, double-tap zoom)
    (set! (.. node -style -touchAction) "none")
    (let [!state (atom {:mode :idle})
          begin-drag! (fn [t]
                        (let [[x y] (txy t)]
                          (reset! !state {:mode :drag :x x :y y})
                          (dispatch-mouse! node "mousedown" x y)))
          end-drag! (fn []
                      (let [{:keys [x y]} @!state]
                        (reset! !state {:mode :idle})
                        (dispatch-mouse! node "mouseup" x y)))
          on-start
          (fn [e]
            ;; also suppresses WebKit's emulated mouse events, so a touch
            ;; never reaches the flows twice
            (.preventDefault e)
            (let [ts (.-touches e)]
              (case (.-length ts)
                1 (begin-drag! (aget ts 0))
                2 (do (when (= :drag (:mode @!state))
                        (end-drag!))
                      (reset! !state (assoc (pinch-geom ts) :mode :pinch)))
                ;; 3+ fingers: abandon the gesture cleanly
                (do (when (= :drag (:mode @!state))
                      (end-drag!))
                    (reset! !state {:mode :idle})))))
          on-move
          (fn [e]
            (.preventDefault e)
            (let [ts (.-touches e)
                  st @!state]
              (cond
                (and (= :drag (:mode st)) (= 1 (.-length ts)))
                (let [[x y] (txy (aget ts 0))]
                  (swap! !state assoc :x x :y y)
                  (dispatch-mouse! node "mousemove" x y))

                (and (= :pinch (:mode st)) (= 2 (.-length ts)))
                (let [{:keys [cx cy dist]} (pinch-geom ts)
                      prev (:dist st)]
                  (when (and prev (pos? prev) (pos? dist))
                    (let [dy (/ (- (js/Math.log (/ dist prev))) wheel-zoom-ln)]
                      (dispatch-wheel! node cx cy dy)))
                  (swap! !state assoc :dist dist :cx cx :cy cy)))))
          on-end
          (fn [e]
            (.preventDefault e)
            (let [remaining (.-touches e)
                  st @!state]
              (case (:mode st)
                :drag (when (zero? (.-length remaining))
                        (let [[x y] (txy (aget (.-changedTouches e) 0))]
                          (reset! !state {:mode :idle})
                          (dispatch-mouse! node "mouseup" x y)))
                :pinch (cond
                         (>= (.-length remaining) 2) nil
                         ;; pinch → one finger: hand off into a fresh drag
                         (= 1 (.-length remaining))
                         (begin-drag! (aget remaining 0))
                         :else (reset! !state {:mode :idle}))
                nil)))]
      (.addEventListener node "touchstart" on-start #js {:passive false})
      (.addEventListener node "touchmove" on-move #js {:passive false})
      (.addEventListener node "touchend" on-end #js {:passive false})
      (.addEventListener node "touchcancel" on-end #js {:passive false})
      (js/console.log "[TOUCH] adapter installed"))))
