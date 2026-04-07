(ns app.client.workspace.events
  "Event flows: DOM events -> semantic values.
   Layer 2 (event sources) and Layer 4 (focus-based routing)."
  (:require [missionary.core :as m]))

;; ============================================================================
;; LAYER 2: EVENT FLOWS (Produce Values, Don't Store)
;; ============================================================================

(defn >canvas-resize
  "Flow that emits viewport dimensions when the canvas element resizes.
   Uses ResizeObserver to detect size changes from flex layout, sidebar toggle, etc."
  [canvas-node]
  (->> (m/observe
         (fn [!]
           (let [emit! (fn []
                         (let [raw-width (max 1 (.-clientWidth canvas-node))
                               raw-height (max 1 (.-clientHeight canvas-node))
                               win-width (max 1 (or (.-innerWidth js/window) raw-width))
                               win-height (max 1 (or (.-innerHeight js/window) raw-height))]
                           (! {:width  (max raw-width win-width)
                               :height (max raw-height win-height)
                               :dpr    (or js/window.devicePixelRatio 1)})))]
             (if (exists? js/ResizeObserver)
               (let [obs (js/ResizeObserver. (fn [_entries] (emit!)))]
                 (.observe obs canvas-node)
                 (emit!)  ;; Emit initial value
                 #(.disconnect obs))
               ;; Fallback: window resize (won't catch sidebar toggle)
               (do (js/window.addEventListener "resize" emit!)
                   (emit!)
                   #(js/window.removeEventListener "resize" emit!))))))
       (m/relieve (fn [_old new] new))))

(defn >wheel [node]
  "Flow that emits wheel delta values as {:dy N :dx N :x N :y N}."
  (->> (m/observe
         (fn [!]
           (let [get-coords (fn [e]
                              (let [rect (.getBoundingClientRect node)]
                                {:x (- (.-clientX e) (.-left rect))
                                 :y (- (.-clientY e) (.-top rect))}))
                 handler (fn [e]
                           (let [{:keys [x y]} (get-coords e)]
                             (.preventDefault e)
                             (! {:dy (.-deltaY e)
                                 :dx (.-deltaX e)
                                 :shift? (.-shiftKey e)
                                 :x x
                                 :y y})))]
             (.addEventListener node "wheel" handler #js {:passive false})
             #(.removeEventListener node "wheel" handler))))
       (m/relieve (fn [a b] {:dy (+ (:dy a) (:dy b))
                             :dx (+ (:dx a) (:dx b))
                             :shift? (:shift? b)
                             :x (:x b)
                             :y (:y b)}))))

(defn >mouse [node]
  "Flow that emits mouse events [:mousedown/:mouseup/:mousemove coords]"
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

(defn snap-to-dpr [v dpr]
  (let [scale (or dpr 1)]
    (/ (Math/round (* v scale)) scale)))

(defn maybe-snap [v dpr snap?]
  (if snap? (snap-to-dpr v dpr) v))

(defn parse-key-event
  "Parse DOM keyboard event into semantic event map"
  [e]
  (let [key (.-key e)
        ctrl? (or (.-ctrlKey e) (.-metaKey e))
        shift? (.-shiftKey e)]
    (cond
      ;; Global shortcuts (not affected by focus)
      (and ctrl? (= key "k")) {:type :toggle-command-panel :global? true}
      (and ctrl? (= key "g")) {:type :toggle-settings-panel :global? true}
      (and ctrl? (= key "b")) {:type :toggle-file-viewer :global? true}
      (and ctrl? (= key "s")) {:type :save :global? true}
      ;; Pane focus shortcuts
      (and ctrl? (= key "1")) {:type :focus-pane :pane :editor :global? true}
      (and ctrl? (= key "2")) {:type :focus-pane :pane :chat :global? true}
      (and ctrl? (= key "3")) {:type :focus-pane :pane :preview :global? true}

      ;; Escape - context dependent but handled globally
      (= key "Escape") {:type :escape :global? true}

      ;; Editor-specific shortcuts
      (and ctrl? (= key "Enter")) {:type :eval}
      (and ctrl? (= key "z") (not shift?)) {:type :undo}
      (and ctrl? (= key "z") shift?) {:type :redo}
      (and ctrl? (= key "y")) {:type :redo}
      (and ctrl? (= key "c")) {:type :copy}
      (and ctrl? (= key "x")) {:type :cut}
      ;; Ctrl+V: return nil so .preventDefault is NOT called — lets browser fire native paste event

      ;; Word navigation
      (and ctrl? (= key "ArrowLeft")) {:type :word-left}
      (and ctrl? (= key "ArrowRight")) {:type :word-right}

      ;; Navigation keys (shift? included for stack reorder)
      (= key "ArrowLeft") {:type :left :shift? shift?}
      (= key "ArrowRight") {:type :right :shift? shift?}
      (= key "ArrowUp") {:type :up :shift? shift?}
      (= key "ArrowDown") {:type :down :shift? shift?}
      (= key "Home") {:type :home}
      (= key "End") {:type :end}

      ;; Editing keys
      (= key "Backspace") {:type :backspace}
      (= key "Delete") {:type :delete}
      (= key "Enter") {:type :enter}

      ;; Character input
      (and (= 1 (count key)) (not ctrl?) (not (.-altKey e)))
      {:type :char :char key}

      :else nil)))

(defn >keyboard [node]
  "Flow that emits parsed keyboard events"
  (->> (m/observe
         (fn [!]
           (let [handler (fn [e]
                           (when-let [event (parse-key-event e)]
                             (.preventDefault e)
                             (! event)))]
             (.addEventListener node "keydown" handler)
             #(.removeEventListener node "keydown" handler))))
       (m/relieve (fn [_ x] x))))

(defn make-raf-flow
  "Creates a fresh RAF flow - emits timestamps on each animation frame.
   IMPORTANT: Must be called fresh for each subscription, not shared!"
  []
  (m/observe
    (fn [!]
      (let [active? (volatile! true)
            callback (fn loop [t]
                       (when @active?
                         (! t)
                         (js/requestAnimationFrame loop)))]
        (js/requestAnimationFrame callback)
        #(vreset! active? false)))))

(defn make-blink-timer
  "Creates a fresh blink timer flow - emits true/false every 530ms.
   IMPORTANT: Must be called fresh for each subscription, not shared!"
  []
  (m/ap
    (loop []
      (m/amb true
             (do (m/? (m/sleep 530))
                 (m/amb false
                        (do (m/? (m/sleep 530))
                            (recur))))))))

;; ============================================================================
;; LAYER 4: FOCUS-BASED EVENT ROUTING
;; ============================================================================
;;
;; IMPORTANT: Do NOT use m/ap with m/?< on m/watch here!
;; These flows filter discrete events - use m/eduction with deref instead.
;; This avoids cancellation when focus changes.

(defn <global-events
  "Flow of global events (not affected by focus)"
  [>keyboard]
  (->> >keyboard
       (m/eduction (filter :global?))))

(defn <editor-keys
  "Flow of keyboard events routed to editor (when focused).
   Uses deref instead of m/watch to avoid cancellation on focus change."
  [>keyboard !focus]
  (->> >keyboard
       (m/eduction (filter (fn [event]
                             (and (= @!focus :editor)
                                  (not (:global? event))))))))

(defn <cmd-panel-keys
  "Flow of keyboard events routed to command panel (when focused).
   Uses deref instead of m/watch to avoid cancellation on focus change."
  [>keyboard !focus]
  (->> >keyboard
       (m/eduction (filter (fn [event]
                             (and (= @!focus :command-panel)
                                  (not (:global? event))))))))

(defn <chat-input-keys
  "Flow of keyboard events routed to chat input (when focused).
   Uses deref instead of m/watch to avoid cancellation on focus change."
  [>keyboard !focus]
  (->> >keyboard
       (m/eduction (filter (fn [event]
                             (and (= @!focus :chat)
                                  (not (:global? event))))))))

(defn <settings-panel-keys
  "Flow of keyboard events routed to settings panel (when focused).
   Uses deref instead of m/watch to avoid cancellation on focus change."
  [>keyboard !focus]
  (->> >keyboard
       (m/eduction (filter (fn [event]
                             (and (= @!focus :settings-panel)
                                  (not (:global? event))))))))
