(ns app.client.webgpu.loop
  "Reactive-first editor loop following Electric/Missionary patterns.

   Architecture:
   - Layer 1: Primary Sources (6 atoms)
   - Layer 2: Event Flows (keyboard, mouse, wheel, resize, blink)
   - Layer 3: Derived Flows (line-lengths, fold-regions, tokenized, render-ops)
   - Layer 4: Focus-based Event Routing
   - Layer 5: Component Update Flows
   - Layer 6: GPU State Derived Flows
   - Layer 7: Terminal Render Consumer"
  (:require [clojure.string :as str]
            [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [missionary.core :as m]
            [contrib.missionary-contrib :as mx]
            [app.client.webgpu.editor :as editor]
            [app.client.webgpu.text-input :as text-input]))

;; ============================================================================
;; LAYER 1: PRIMARY SOURCES (The Only Atoms)
;; ============================================================================
;; These are created per-instance in start-loop! to support multiple editors

;; ============================================================================
;; LAYER 2: EVENT FLOWS (Produce Values, Don't Store)
;; ============================================================================

(defn >window-resize []
  "Flow that emits viewport dimensions on resize"
  (->> (m/observe
         (fn [!]
           (let [handler (fn []
                           (! {:width  js/window.innerWidth
                               :height js/window.innerHeight
                               :dpr    (or js/window.devicePixelRatio 1)}))]
             (js/window.addEventListener "resize" handler)
             (handler)  ;; Emit initial value
             #(js/window.removeEventListener "resize" handler))))
       (m/relieve (fn [_old new] new))))

(defn >wheel [node]
  "Flow that emits wheel delta values"
  (->> (m/observe
         (fn [!]
           (let [handler (fn [e]
                           (.preventDefault e)
                           (! (.-deltaY e)))]
             (.addEventListener node "wheel" handler #js {:passive false})
             #(.removeEventListener node "wheel" handler))))
       (m/relieve +)))

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

(defn parse-key-event
  "Parse DOM keyboard event into semantic event map"
  [e]
  (let [key (.-key e)
        ctrl? (or (.-ctrlKey e) (.-metaKey e))
        shift? (.-shiftKey e)]
    (cond
      ;; Global shortcuts (not affected by focus)
      (and ctrl? (= key "k")) {:type :toggle-command-panel :global? true}
      (and ctrl? (= key "s")) {:type :save :global? true}

      ;; Escape - context dependent but handled globally
      (= key "Escape") {:type :escape :global? true}

      ;; Editor-specific shortcuts
      (and ctrl? (= key "Enter")) {:type :eval}
      (and ctrl? (= key "z") (not shift?)) {:type :undo}
      (and ctrl? (= key "z") shift?) {:type :redo}
      (and ctrl? (= key "y")) {:type :redo}
      (and ctrl? (= key "c")) {:type :copy}
      (and ctrl? (= key "x")) {:type :cut}
      (and ctrl? (= key "v")) {:type :paste}

      ;; Word navigation
      (and ctrl? (= key "ArrowLeft")) {:type :word-left}
      (and ctrl? (= key "ArrowRight")) {:type :word-right}

      ;; Navigation keys
      (= key "ArrowLeft") {:type :left}
      (= key "ArrowRight") {:type :right}
      (= key "ArrowUp") {:type :up}
      (= key "ArrowDown") {:type :down}
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
;; LAYER 3: DERIVED FLOWS (Pure Transformations)
;; ============================================================================

(defn <line-lengths
  "Derived flow: line lengths from editor doc"
  [!editor-doc]
  (m/ap
    (let [doc (m/?< (m/watch !editor-doc))]
      (mapv count (:lines doc)))))

(defn <fold-regions
  "Derived flow: fold regions from lines"
  [!editor-doc detect-folds-fn]
  (m/ap
    (let [doc (m/?< (m/watch !editor-doc))
          lines (:lines doc)
          lengths (mapv count lines)]
      (or (detect-folds-fn lines lengths) []))))

(defn <line-mapping
  "Derived flow: visual→logical line mapping based on fold state"
  [!editor-doc !folded-lines detect-folds-fn]
  (m/ap
    (let [doc (m/?< (m/watch !editor-doc))
          regions (m/?< (<fold-regions !editor-doc detect-folds-fn))
          folded (m/?< (m/watch !folded-lines))
          lines (:lines doc)
          num-lines (count lines)]
      ;; Build mapping: visual line index -> logical line index
      (loop [logical-idx 0
             mapping []]
        (if (>= logical-idx num-lines)
          mapping
          (let [;; Check if this line should be visible
                visible? (not (some (fn [{:keys [start-line end-line]}]
                                      (and (contains? folded start-line)
                                           (> logical-idx start-line)
                                           (<= logical-idx end-line)))
                                    regions))]
            (if visible?
              (recur (inc logical-idx) (conj mapping logical-idx))
              (recur (inc logical-idx) mapping))))))))

(defn <tokenized-lines
  "Derived flow: tokenized lines for syntax highlighting"
  [!editor-doc tokenize-fn]
  (m/ap
    (let [doc (m/?< (m/watch !editor-doc))]
      (mapv tokenize-fn (:lines doc)))))

(defn <editor-render-ops
  "Derived flow: render operations for editor text"
  [!editor-doc !folded-lines tokenize-fn layout-fn detect-folds-fn layout-x layout-y font-size]
  (m/ap
    (let [doc (m/?< (m/watch !editor-doc))
          folded (m/?< (m/watch !folded-lines))
          lines (:lines doc)
          tokenized (mapv tokenize-fn lines)
          regions (or (detect-folds-fn lines (mapv count lines)) [])
          layout-result (layout-fn tokenized layout-x layout-y font-size regions folded)]
      layout-result)))

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

;; ============================================================================
;; LAYER 5: COMPONENT UPDATE FLOWS
;; ============================================================================

(defn editor-apply-event
  "Pure function: apply event to editor doc, returns new doc"
  [doc event line-lengths clipboard]
  (let [input {:lines (:lines doc)
               :cursor (:cursor doc)
               :selection (:selection doc)
               :desired-col (:desired-col doc)}]
    (case (:type event)
      :char
      (let [new-input (text-input/insert-char input (:char event) true)]
        (merge doc new-input {:selection nil}))

      :backspace
      (let [new-input (text-input/delete-backward input true)]
        (merge doc new-input))

      :delete
      (let [new-input (text-input/delete-forward input true)]
        (merge doc new-input))

      :enter
      (let [new-input (text-input/insert-char input "\n" true)]
        (merge doc new-input {:selection nil}))

      :left
      (let [new-input (text-input/move-cursor input :left true line-lengths)]
        (merge doc new-input))

      :right
      (let [new-input (text-input/move-cursor input :right true line-lengths)]
        (merge doc new-input))

      :up
      (let [new-input (text-input/move-cursor input :up true line-lengths)]
        (merge doc new-input))

      :down
      (let [new-input (text-input/move-cursor input :down true line-lengths)]
        (merge doc new-input))

      :home
      (let [new-input (text-input/move-cursor input :home true line-lengths)]
        (merge doc new-input))

      :end
      (let [new-input (text-input/move-cursor input :end true line-lengths)]
        (merge doc new-input))

      :word-left
      (let [new-input (text-input/move-word input :left true line-lengths)]
        (merge doc new-input))

      :word-right
      (let [new-input (text-input/move-word input :right true line-lengths)]
        (merge doc new-input))

      :paste
      (if clipboard
        (let [new-input (text-input/paste input clipboard true)]
          (merge doc new-input))
        doc)

      ;; Default: no change
      doc)))

(defn cmd-panel-apply-event
  "Pure function: apply event to command panel, returns new panel state"
  [panel event clipboard]
  (let [input {:text (:text panel) :cursor (:cursor panel)}]
    (case (:type event)
      :char
      (let [new-input (text-input/insert-char input (:char event) false)]
        (assoc panel :text (:text new-input) :cursor (:cursor new-input)))

      :backspace
      (let [new-input (text-input/delete-backward input false)]
        (assoc panel :text (:text new-input) :cursor (:cursor new-input)))

      :delete
      (let [new-input (text-input/delete-forward input false)]
        (assoc panel :text (:text new-input) :cursor (:cursor new-input)))

      :enter
      ;; Submit command - will be handled by caller
      panel

      :left
      (let [new-input (text-input/move-cursor input :left false nil)]
        (assoc panel :cursor (:cursor new-input)))

      :right
      (let [new-input (text-input/move-cursor input :right false nil)]
        (assoc panel :cursor (:cursor new-input)))

      :home
      (let [new-input (text-input/move-cursor input :home false nil)]
        (assoc panel :cursor (:cursor new-input)))

      :end
      (let [new-input (text-input/move-cursor input :end false nil)]
        (assoc panel :cursor (:cursor new-input)))

      :word-left
      (let [new-input (text-input/move-word input :left false nil)]
        (assoc panel :cursor (:cursor new-input)))

      :word-right
      (let [new-input (text-input/move-word input :right false nil)]
        (assoc panel :cursor (:cursor new-input)))

      :paste
      (if clipboard
        (let [new-input (text-input/paste input clipboard false)]
          (assoc panel :text (:text new-input) :cursor (:cursor new-input)))
        panel)

      ;; Default: no change
      panel)))

;; ============================================================================
;; LAYER 6: GPU STATE DERIVED FLOWS
;; ============================================================================

(defn calculate-logical->visual
  "Create reverse mapping from logical line to visual line"
  [line-mapping]
  (reduce-kv (fn [m visual-idx logical-idx]
               (assoc m logical-idx visual-idx))
             {}
             (vec line-mapping)))

(defn compute-editor-rects
  "Pure function: compute all editor rectangles from inputs"
  [doc folded eval-result caret-visible
   detect-folds-fn find-bracket-fn
   font-size layout-x layout-y line-h gutter-w]
  (let [lines (:lines doc)
        cursor (:cursor doc)
        selection (:selection doc)

        lengths (mapv count lines)
        regions (or (detect-folds-fn lines lengths) [])

        ;; Build line mapping
        line-mapping (loop [logical-idx 0 mapping []]
                       (if (>= logical-idx (count lines))
                         mapping
                         (let [visible? (not (some (fn [{:keys [start-line end-line]}]
                                                     (and (contains? folded start-line)
                                                          (> logical-idx start-line)
                                                          (<= logical-idx end-line)))
                                                   regions))]
                           (if visible?
                             (recur (inc logical-idx) (conj mapping logical-idx))
                             (recur (inc logical-idx) mapping)))))

        logical->visual (calculate-logical->visual line-mapping)

        ;; Helper to get visual y for logical line
        logical->visual-y (fn [logical-line]
                            (when-let [visual-idx (get logical->visual logical-line)]
                              (+ layout-y (* visual-idx line-h))))

        char-w (* font-size 0.6)

        ;; Fold indicator rects
        fold-rects (keep (fn [{:keys [start-line]}]
                          (when-let [visual-y (logical->visual-y start-line)]
                            (let [is-folded? (contains? folded start-line)
                                  indicator-size 10
                                  x (+ 50 (/ (- gutter-w indicator-size) 2))
                                  y (+ visual-y (/ (- line-h indicator-size) 2))]
                              {:x x :y y :w indicator-size :h indicator-size
                               :r (if is-folded? 0.3 0.7)
                               :g (if is-folded? 0.5 0.6)
                               :b (if is-folded? 0.9 0.3)
                               :a 0.8})))
                        regions)

        ;; Bracket match rects
        bracket-match (when (and cursor (not selection))
                        (find-bracket-fn cursor lines lengths))
        bracket-rects (when bracket-match
                        (keep (fn [{:keys [line col]}]
                                (when-let [visual-y (logical->visual-y line)]
                                  {:x (+ layout-x (* col char-w))
                                   :y visual-y
                                   :w char-w
                                   :h line-h
                                   :r 0.8 :g 0.6 :b 0.2 :a 0.4}))
                              [(:open bracket-match) (:close bracket-match)]))

        ;; Caret rect (when no selection)
        caret-rect (when (and cursor caret-visible (not selection))
                     (when-let [visual-y (logical->visual-y (:line cursor))]
                       {:x (+ layout-x (* (:col cursor) char-w))
                        :y visual-y
                        :w 2
                        :h line-h
                        :r 0.9 :g 0.9 :b 0.9 :a 1.0}))

        ;; Selection rects
        selection-rects (when selection
                          (let [{:keys [start end]} selection
                                [s e] (if (or (> (:line start) (:line end))
                                              (and (= (:line start) (:line end))
                                                   (> (:col start) (:col end))))
                                        [end start]
                                        [start end])]
                            (keep (fn [logical-line]
                                    (when-let [visual-y (logical->visual-y logical-line)]
                                      (let [line-len (get lengths logical-line 0)
                                            col-start (if (= logical-line (:line s)) (:col s) 0)
                                            col-end (if (= logical-line (:line e)) (:col e) line-len)
                                            width-chars (- col-end col-start)]
                                        (when (> width-chars 0)
                                          {:x (+ layout-x (* col-start char-w))
                                           :y visual-y
                                           :w (* width-chars char-w)
                                           :h line-h
                                           :r 0.2 :g 0.4 :b 0.9 :a 0.5}))))
                                  (range (:line s) (inc (:line e))))))

        ;; Eval result rect
        eval-rect (when eval-result
                    (let [now (js/Date.now)]
                      (when (< now (:expires-at eval-result))
                        (when-let [visual-y (logical->visual-y (:line eval-result))]
                          (let [line-len (get lengths (:line eval-result) 0)
                                result-x (+ layout-x (* (+ line-len 2) char-w))
                                result-w (* (count (:text eval-result)) char-w)]
                            {:x result-x
                             :y visual-y
                             :w (+ result-w 16)
                             :h line-h
                             :r (if (str/starts-with? (:text eval-result) "=>") 0.1 0.4)
                             :g (if (str/starts-with? (:text eval-result) "=>") 0.3 0.1)
                             :b 0.1
                             :a 0.8})))))]

    (vec (concat fold-rects
                 (or bracket-rects [])
                 (or selection-rects [])
                 (if caret-rect [caret-rect] [])
                 (if eval-rect [eval-rect] [])))))

(defn <editor-rects
  "Derived flow: all editor rectangles (selection, caret, brackets, folds, eval)
   Uses m/latest instead of m/ap to avoid cancellation propagation issues."
  [!editor-doc !folded-lines !eval-result !caret-visible
   detect-folds-fn find-bracket-fn
   font-size layout-x layout-y line-h gutter-w]
  (m/latest
    (fn [doc folded eval-result caret-visible]
      (compute-editor-rects doc folded eval-result caret-visible
                            detect-folds-fn find-bracket-fn
                            font-size layout-x layout-y line-h gutter-w))
    (m/watch !editor-doc)
    (m/watch !folded-lines)
    (m/watch !eval-result)
    (m/watch !caret-visible)))

(defn <cmd-panel-rects
  "Derived flow: command panel rectangles (background + caret)
   Uses m/latest instead of m/ap to avoid cancellation propagation."
  [!cmd-panel !focus !caret-visible !scroll-y !viewport font-size cmd-panel-h]
  (m/latest
    (fn [panel focus caret-visible scroll-y viewport]
      (when (:visible panel)
        (let [panel-y (+ scroll-y (- (:height viewport) cmd-panel-h))
              char-w (* font-size 0.6)

              ;; Background rect
              bg-rect {:x 0 :y panel-y :w (:width viewport) :h cmd-panel-h
                       :r 0.15 :g 0.15 :b 0.2 :a 1.0}

              ;; Caret rect (only when focused and visible)
              caret-rect (when (and caret-visible (= focus :command-panel))
                           {:x (+ 60 (* (:cursor panel) char-w))
                            :y (+ panel-y 8)
                            :w 2
                            :h (- cmd-panel-h 16)
                            :r 0.9 :g 0.9 :b 0.9 :a 1.0})]

          (if caret-rect
            [bg-rect caret-rect]
            [bg-rect]))))
    (m/watch !cmd-panel)
    (m/watch !focus)
    (m/watch !caret-visible)
    (m/watch !scroll-y)
    (m/watch !viewport)))

(defn <combined-text-ops
  "Derived flow: combined text render ops (editor + command panel)
   Uses m/latest instead of m/ap to avoid cancellation propagation."
  [!editor-doc !cmd-panel !scroll-y !viewport !folded-lines
   tokenize-fn layout-fn detect-folds-fn
   layout-x layout-y font-size cmd-panel-h]
  (m/latest
    (fn [doc panel scroll-y viewport folded]
      (let [;; Editor render ops
            lines (:lines doc)
            tokenized (mapv tokenize-fn lines)
            regions (or (detect-folds-fn lines (mapv count lines)) [])
            layout-result (layout-fn tokenized layout-x layout-y font-size regions folded)
            editor-ops (:render-ops layout-result)

            ;; Command panel ops (if visible)
            cmd-ops (when (:visible panel)
                      (let [cmd-panel-y (+ scroll-y (- (:height viewport) cmd-panel-h))
                            cmd-text-y (+ cmd-panel-y 12 font-size)]
                        [(when (seq (:text panel))
                           [{:text (:text panel)
                             :type :text
                             :from 0 :to (count (:text panel))
                             :x 60 :y cmd-text-y
                             :size font-size
                             :r 0.9 :g 0.9 :b 0.9 :a 1.0}])
                         [{:text "> "
                           :type :macro
                           :from 0 :to 2
                           :x 40 :y cmd-text-y
                           :size font-size
                           :r 0.3 :g 0.6 :b 1.0 :a 1.0}]
                         (when (empty? (:text panel))
                           [{:text "Type a task..."
                             :type :comment
                             :from 0 :to 14
                             :x 60 :y cmd-text-y
                             :size font-size
                             :r 0.5 :g 0.5 :b 0.5 :a 0.7}])]))]

        {:render-ops (if (:visible panel)
                       (vec (concat editor-ops (filter some? cmd-ops)))
                       editor-ops)
         :line-mapping (:line-mapping layout-result)
         :editor-line-count (count editor-ops)}))
    (m/watch !editor-doc)
    (m/watch !cmd-panel)
    (m/watch !scroll-y)
    (m/watch !viewport)
    (m/watch !folded-lines)))

;; ============================================================================
;; LAYER 7: TERMINAL RENDER CONSUMER
;; ============================================================================

(defn start-loop!
  "Start the reactive editor loop.

   This is the main entry point. It creates the reactive flow graph and
   returns a Missionary task that runs the render loop."
  [node device ctx geometry initial-line-lengths initial-lines
   tokenize-fn layout-fn find-bracket-fn detect-folds-fn
   find-form-fn eval-form-fn atlas]
  
  (let [;; Layout Configuration
        font-size 16
        gutter-w  40
        layout-x  (+ 50 gutter-w)
        layout-y  100
        line-h    (* font-size 1.2)
        cmd-panel-h 40

        ;; =====================================================================
        ;; LAYER 1: PRIMARY SOURCE ATOMS
        ;; =====================================================================

        !editor-doc (atom {:lines initial-lines
                          :cursor {:line 0 :col 0}
                          :selection nil
                          :desired-col 0})

        !cmd-panel (atom {:text "" :cursor 0 :visible false})

        !focus (atom :editor)

        !scroll-y (atom 0)

        !viewport (atom {:width  (.-innerWidth js/window)
                         :height (.-innerHeight js/window)
                         :dpr    (or (.-devicePixelRatio js/window) 1)})

        !folded-lines (atom #{})

        ;; Additional state atoms (not primary sources, but needed for features)
        !caret-visible (atom true)
        !clipboard (atom nil)
        !undo-stack (atom [])
        !redo-stack (atom [])
        !eval-result (atom nil)
        !dragging? (atom false)

        ;; GPU state atoms (terminals update these)
        !text-geo (atom (:text geometry))
        !editor-rect-sys (atom (:rect geometry))
        !cmd-rect-sys (atom (let [capacity 16
                                  instance-buffer (.createBuffer device
                                                    (clj->js {:size (* capacity 32)
                                                              :usage (bit-or js/GPUBufferUsage.VERTEX
                                                                             js/GPUBufferUsage.COPY_DST)}))]
                              {:pipeline (:pipeline (:rect geometry))
                               :bind-group (:bind-group (:rect geometry))
                               :instance-buffer instance-buffer
                               :num-instances 0}))

        ;; Helper functions
        save-undo! (fn [lines cursor]
                     (swap! !undo-stack conj {:lines lines :cursor cursor})
                     (when (> (count @!undo-stack) 100)
                       (swap! !undo-stack #(vec (drop 1 %))))
                     (reset! !redo-stack []))

        ;; =====================================================================
        ;; LAYER 2: EVENT FLOWS
        ;; =====================================================================
        ;; IMPORTANT: These must be fresh flows, not shared top-level defs!

        >raf (make-raf-flow)              ;; Fresh RAF flow for this instance
        >blink-timer (make-blink-timer)   ;; Fresh blink timer for this instance
        >resize (>window-resize)
        >wheel-events (>wheel node)
        >mouse-events (->> (>mouse node) (m/relieve (fn [_ x] x)))
        >keyboard-events (>keyboard js/window)

        ;; =====================================================================
        ;; LAYER 4: FOCUS-BASED EVENT ROUTING
        ;; =====================================================================

        <global-keys (<global-events >keyboard-events)
        <editor-keyboard (<editor-keys >keyboard-events !focus)
        <cmd-keyboard (<cmd-panel-keys >keyboard-events !focus)]

    (m/join vector

      ;; =====================================================================
      ;; BLINK TIMER CONSUMER
      ;; =====================================================================
      (->> >blink-timer
           (m/reduce (fn [_ v] (reset! !caret-visible v) nil) nil))

      ;; =====================================================================
      ;; VIEWPORT RESIZE CONSUMER
      ;; =====================================================================
      (->> >resize
           (m/reduce
             (fn [_ {:keys [width height dpr]}]
               (reset! !viewport {:width width :height height :dpr dpr})
               (set! (.-width node) (Math/floor (* width dpr)))
               (set! (.-height node) (Math/floor (* height dpr)))
               nil)
             nil))

      ;; =====================================================================
      ;; SCROLL CONSUMER
      ;; =====================================================================
      (->> >wheel-events
           (m/reduce (fn [_ delta] 
                       #_(js/console.log "Scroll:" delta)
                       (swap! !scroll-y + delta) nil) nil))

      ;; =====================================================================
      ;; MOUSE EVENTS CONSUMER
      ;; =====================================================================
      (->> >mouse-events
           (m/reduce
             (fn [_ [type coords]]
               #_(js/console.log "Mouse:" type coords)
               (case type
                 :mousedown
                 (let [{:keys [x y]} coords
                       viewport @!viewport
                       scroll-y @!scroll-y
                       cmd-panel @!cmd-panel
                       cmd-visible? (:visible cmd-panel)
                       cmd-panel-top (if cmd-visible?
                                       (- (:height viewport) cmd-panel-h)
                                       (:height viewport))
                       clicked-in-cmd? (and cmd-visible? (>= y cmd-panel-top))]
                   (if clicked-in-cmd?
                     ;; Click in command panel
                     (let [char-w (* font-size 0.6)
                           text (:text cmd-panel)
                           col (-> (/ (- x 60) char-w)
                                   (Math/round)
                                   (max 0)
                                   (min (count text)))]
                       (reset! !focus :command-panel)
                       (swap! !cmd-panel assoc :cursor col)
                       (reset! !caret-visible true))
                     ;; Click in editor
                     (let [adj-y (+ y scroll-y)]
                       (if (and (>= x 50) (< x (+ 50 gutter-w)))
                         ;; Gutter click - toggle fold
                         (let [text-result @!text-geo
                               line-mapping (or (:line-mapping text-result) [])
                               visual-line (max 0 (Math/floor (/ (- adj-y layout-y) line-h)))
                               logical-line (get line-mapping visual-line visual-line)
                               regions (detect-folds-fn (:lines @!editor-doc)
                                                        (mapv count (:lines @!editor-doc)))
                               fold-region (first (filter #(= (:start-line %) logical-line)
                                                          (or regions [])))]
                           (when fold-region
                             (swap! !folded-lines
                                    (fn [folded]
                                      (if (contains? folded logical-line)
                                        (disj folded logical-line)
                                        (conj folded logical-line)))))
                           (reset! !focus :editor))
                         ;; Normal click - place cursor
                         (let [text-result @!text-geo
                               line-mapping (or (:line-mapping text-result) [])
                               lengths (mapv count (:lines @!editor-doc))
                               visual-line (max 0 (Math/floor (/ (- adj-y layout-y) line-h)))
                               logical-line (get line-mapping visual-line
                                                (min visual-line (dec (count lengths))))
                               line-len (get lengths logical-line 0)
                               char-w (* font-size 0.6)
                               col (-> (/ (- x layout-x) char-w)
                                       (Math/round)
                                       (max 0)
                                       (min line-len))
                               pos {:line logical-line :col col}]
                           (reset! !dragging? true)
                           (swap! !editor-doc assoc
                                  :cursor pos
                                  :selection nil
                                  :desired-col col)
                           (reset! !caret-visible true)
                           (reset! !focus :editor))))))

                 :mousemove
                 (when @!dragging?
                   (let [{:keys [x y]} coords
                         scroll-y @!scroll-y
                         adj-y (+ y scroll-y)
                         text-result @!text-geo
                         line-mapping (or (:line-mapping text-result) [])
                         lengths (mapv count (:lines @!editor-doc))
                         visual-line (max 0 (Math/floor (/ (- adj-y layout-y) line-h)))
                         logical-line (get line-mapping visual-line
                                          (min visual-line (dec (count lengths))))
                         line-len (get lengths logical-line 0)
                         char-w (* font-size 0.6)
                         col (-> (/ (- x layout-x) char-w)
                                 (Math/round)
                                 (max 0)
                                 (min line-len))
                         pos {:line logical-line :col col}
                         doc @!editor-doc
                         start-pos (:cursor doc)]
                     (when (not= pos start-pos)
                       (swap! !editor-doc assoc
                              :selection {:start start-pos :end pos}))))

                 :mouseup
                 (reset! !dragging? false))
               nil)
             nil))

      ;; =====================================================================
      ;; GLOBAL KEYBOARD EVENTS CONSUMER
      ;; =====================================================================
      (->> <global-keys
           (m/reduce
             (fn [_ event]
               (when event
                 (js/console.log "Global Key:" (:type event))
                 (case (:type event)
                   :toggle-command-panel
                   (let [visible? (:visible @!cmd-panel)]
                     (if visible?
                       (do (swap! !cmd-panel assoc :visible false)
                           (reset! !focus :editor))
                       (do (swap! !cmd-panel assoc :visible true)
                           (reset! !focus :command-panel)
                           (reset! !caret-visible true))))

                   :escape
                   (cond
                     (:visible @!cmd-panel)
                     (do (swap! !cmd-panel assoc :visible false)
                         (reset! !focus :editor))

                     :else
                     (swap! !editor-doc assoc :selection nil))

                   :save
                   (let [content (str/join "\n" (:lines @!editor-doc))
                         blob (js/Blob. #js [content] #js {:type "text/plain"})
                         url (js/URL.createObjectURL blob)
                         a (js/document.createElement "a")]
                     (set! (.-href a) url)
                     (set! (.-download a) "code.clj")
                     (.click a)
                     (js/URL.revokeObjectURL url)
                     (js/console.log "Saved file: code.clj" (count content) "bytes"))

                   nil))
               nil)
             nil))

      ;; =====================================================================
      ;; EDITOR KEYBOARD EVENTS CONSUMER
      ;; =====================================================================
      (->> <editor-keyboard
           (m/reduce
             (fn [_ event]
               (when event
                 (js/console.log "Editor Key:" (:type event))
                 (let [doc @!editor-doc
                       lengths (mapv count (:lines doc))]
                   (case (:type event)
                     ;; Edit operations (save undo first)
                     (:char :backspace :delete :enter :paste)
                     (let [_ (save-undo! (:lines doc) (:cursor doc))
                           new-doc (editor-apply-event doc event lengths @!clipboard)]
                       (reset! !editor-doc new-doc)
                       (reset! !caret-visible true))

                     ;; Navigation (no undo needed)
                     (:left :right :up :down :home :end :word-left :word-right)
                     (let [new-doc (editor-apply-event doc event lengths nil)
                           ;; Auto-scroll to keep caret visible
                           caret-y (+ layout-y (* (:line (:cursor new-doc)) line-h))
                           scroll-y @!scroll-y
                           viewport @!viewport
                           viewport-bottom (+ scroll-y (:height viewport))
                           padding line-h
                           new-scroll (cond
                                        (< caret-y (+ scroll-y padding))
                                        (max 0 (- caret-y padding))

                                        (> (+ caret-y line-h) (- viewport-bottom padding))
                                        (+ (- caret-y (:height viewport)) line-h padding)

                                        :else scroll-y)]
                       (reset! !editor-doc new-doc)
                       (reset! !scroll-y new-scroll)
                       (reset! !caret-visible true))

                     :copy
                     (let [input {:lines (:lines doc)
                                  :cursor (:cursor doc)
                                  :selection (:selection doc)}
                           text (text-input/copy input true)]
                       (when text
                         (reset! !clipboard text)
                         (js/console.log "Copied:" text)))

                     :cut
                     (let [input {:lines (:lines doc)
                                  :cursor (:cursor doc)
                                  :selection (:selection doc)}
                           result (text-input/cut input true)]
                       (when (:text result)
                         (save-undo! (:lines doc) (:cursor doc))
                         (reset! !clipboard (:text result))
                         (reset! !editor-doc (merge doc (:state result)))
                         (js/console.log "Cut:" (:text result))))

                     :undo
                     (when-let [prev (peek @!undo-stack)]
                       (swap! !redo-stack conj {:lines (:lines doc) :cursor (:cursor doc)})
                       (swap! !undo-stack pop)
                       (reset! !editor-doc (merge doc {:lines (:lines prev)
                                                       :cursor (:cursor prev)
                                                       :selection nil
                                                       :desired-col (:col (:cursor prev))}))
                       (reset! !caret-visible true))

                     :redo
                     (when-let [next-state (peek @!redo-stack)]
                       (swap! !undo-stack conj {:lines (:lines doc) :cursor (:cursor doc)})
                       (swap! !redo-stack pop)
                       (reset! !editor-doc (merge doc {:lines (:lines next-state)
                                                       :cursor (:cursor next-state)
                                                       :selection nil
                                                       :desired-col (:col (:cursor next-state))}))
                       (reset! !caret-visible true))

                     :eval
                     (when-let [pos (:cursor doc)]
                       (if-let [form-info (find-form-fn pos (:lines doc) lengths)]
                         (let [result-text (eval-form-fn (:form-str form-info))]
                           (js/console.log "SCI Eval:" (:form-str form-info) "=>" result-text)
                           (reset! !eval-result {:text result-text
                                                 :line (:end-line form-info)
                                                 :expires-at (+ (js/Date.now) 5000)}))
                         (do (js/console.log "SCI: No form at cursor")
                             (reset! !eval-result {:text "No form at cursor"
                                                   :line (:line pos)
                                                   :expires-at (+ (js/Date.now) 2000)}))))

                     nil)))
               nil)
             nil))

      ;; =====================================================================
      ;; COMMAND PANEL KEYBOARD EVENTS CONSUMER
      ;; =====================================================================
      (->> <cmd-keyboard
           (m/reduce
             (fn [_ event]
               (when event
                 (js/console.log "Cmd Key:" (:type event))
                 (case (:type event)
                   :enter
                   (let [cmd-text (:text @!cmd-panel)]
                     (when (seq cmd-text)
                       (js/console.log "Command submitted:" cmd-text))
                     (swap! !cmd-panel assoc :text "" :cursor 0 :visible false)
                     (reset! !focus :editor))

                   ;; Edit/navigation operations
                   (let [panel @!cmd-panel
                         new-panel (cmd-panel-apply-event panel event @!clipboard)]
                     (reset! !cmd-panel new-panel)
                     (reset! !caret-visible true))))
               nil)
             nil))

      ;; =====================================================================
      ;; RENDER LOOP (SINGLE TERMINAL - "PULL" MODEL)
      ;; =====================================================================
      ;;
      ;; This is the ONLY place GPU operations happen. We "pull" the latest
      ;; computed state from all derived flows when RAF fires, then upload
      ;; and draw in a single atomic operation per frame.
      ;;
      ;; This ensures:
      ;; 1. Consistent snapshot - all data is from the same logical moment
      ;; 2. No wasted GPU uploads - only upload when we're about to draw
      ;; 3. Frame-synchronized updates - GPU state changes aligned with vsync
      ;;
      (let [;; Derived flows (pure computation, no GPU side effects)
            <text-data (<combined-text-ops !editor-doc !cmd-panel !scroll-y !viewport !folded-lines
                                           tokenize-fn layout-fn detect-folds-fn
                                           layout-x layout-y font-size cmd-panel-h)
            <editor-rect-data (<editor-rects !editor-doc !folded-lines !eval-result !caret-visible
                                             detect-folds-fn find-bracket-fn
                                             font-size layout-x layout-y line-h gutter-w)
            <cmd-rect-data (<cmd-panel-rects !cmd-panel !focus !caret-visible !scroll-y !viewport
                                             font-size cmd-panel-h)

            ;; Combined world state - sampled on RAF
            ;; m/latest combines flows, m/sample synchronizes with frame clock
            ;; All derived flows now use m/latest internally, so they're continuous
            <world-snapshot (m/latest
                              (fn [text-data editor-rects cmd-rects viewport scroll-y cmd-panel]
                                {:text-data text-data
                                 :editor-rects editor-rects
                                 :cmd-rects cmd-rects
                                 :viewport viewport
                                 :scroll-y scroll-y
                                 :cmd-visible (:visible cmd-panel)})
                              <text-data
                              <editor-rect-data
                              <cmd-rect-data
                              (m/watch !viewport)
                              (m/watch !scroll-y)
                              (m/watch !cmd-panel))]

        ;; The render pulse: sample world state on each animation frame
        (m/reduce
          (fn [prev-state [world _frame-time]]
            ;; Only upload to GPU if data actually changed (via structural comparison)
            (let [{:keys [text-data editor-rects cmd-rects viewport scroll-y cmd-visible]} world

                  ;; Upload text geometry (only if changed)
                  new-text-geo (if (= (:render-ops text-data) (:prev-text-ops prev-state))
                                 (:text-geo prev-state)
                                 (let [geo (editor/update-text-data device (:text geometry)
                                                                    (:render-ops text-data) atlas font-size)]
                                   (assoc geo
                                          :line-mapping (:line-mapping text-data)
                                          :editor-line-count (:editor-line-count text-data))))

                  ;; Upload editor rects (only if changed)
                  new-editor-sys (if (= editor-rects (:prev-editor-rects prev-state))
                                   (:editor-rect-sys prev-state)
                                   (editor/update-rects device
                                                        (or (:editor-rect-sys prev-state) (:rect geometry))
                                                        editor-rects))

                  ;; Upload cmd panel rects (only if changed)
                  new-cmd-sys (if (= cmd-rects (:prev-cmd-rects prev-state))
                                (:cmd-rect-sys prev-state)
                                (editor/update-rects device
                                                     (or (:cmd-rect-sys prev-state) @!cmd-rect-sys)
                                                     (or cmd-rects [])))]

              ;; Store line-mapping for mouse hit testing
              (reset! !text-geo new-text-geo)

              ;; Draw the frame
              (editor/draw-frame! device ctx
                                  new-text-geo
                                  new-editor-sys
                                  new-cmd-sys
                                  (:camera-floats (:pipelines geometry))
                                  (:pass-descriptor (:pipelines geometry))
                                  0 (- scroll-y)
                                  (:width viewport) (:height viewport)
                                  :cmd-panel-visible cmd-visible
                                  :cmd-panel-h cmd-panel-h
                                  :editor-line-count (:editor-line-count text-data))

              ;; Return state for next frame comparison
              {:text-geo new-text-geo
               :editor-rect-sys new-editor-sys
               :cmd-rect-sys new-cmd-sys
               :prev-text-ops (:render-ops text-data)
               :prev-editor-rects editor-rects
               :prev-cmd-rects cmd-rects}))

          ;; Initial state
          {:text-geo (:text geometry)
           :editor-rect-sys (:rect geometry)
           :cmd-rect-sys @!cmd-rect-sys
           :prev-text-ops nil
           :prev-editor-rects nil
           :prev-cmd-rects nil}

          ;; Sample world state on each RAF tick
          (m/sample vector <world-snapshot >raf))))))
