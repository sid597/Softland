(ns app.client.webgpu.loop
  (:require [clojure.string :as str]
            [hyperfiddle.electric3 :as e]
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
                      (let [rect (.getBoundingClientRect node)]
                        (! {:delta-y (.-deltaY e)
                            :cursor-x (- (.-clientX e) (.-left rect))
                            :cursor-y (- (.-clientY e) (.-top rect))
                            :ctrl? (or (.-ctrlKey e) (.-metaKey e))})))]
        (.addEventListener node "wheel" handler #js {:passive false})
        #(.removeEventListener node "wheel" handler)))))

(defn >mouse-events [node]
  (m/observe
    (fn [!]
      (let [get-coords (fn [e]
                         (let [rect (.getBoundingClientRect node)]
                           {:x (- (.-clientX e) (.-left rect))
                            :y (- (.-clientY e) (.-top rect))
                            :button (.-button e)}))

            down-h (fn [e]
                     ;; Prevent context menu on middle click
                     (when (= 1 (.-button e))
                       (.preventDefault e))
                     (! [:mousedown (get-coords e)]))
            up-h   (fn [e] (! [:mouseup (get-coords e)]))
            move-h (fn [e] (! [:mousemove (get-coords e)]))
            context-h (fn [e]
                        ;; Prevent context menu (right click)
                        nil)]

        (.addEventListener node "mousedown" down-h)
        (.addEventListener js/window "mouseup" up-h)
        (.addEventListener js/window "mousemove" move-h)
        (.addEventListener node "contextmenu" context-h)

        (fn []
          (.removeEventListener node "mousedown" down-h)
          (.removeEventListener js/window "mouseup" up-h)
          (.removeEventListener js/window "mousemove" move-h)
          (.removeEventListener node "contextmenu" context-h))))))

(defn >keyboard-events [node]
  (m/observe
    (fn [!]
      (let [handler (fn [e]
                      (let [key (.-key e)
                            ctrl? (or (.-ctrlKey e) (.-metaKey e))]
                        (cond
                          ;; Ctrl+Enter / Cmd+Enter - Evaluate form
                          (and ctrl? (= key "Enter"))
                          (do (.preventDefault e)
                              (! [:eval nil]))

                          ;; Ctrl+C - Copy
                          (and ctrl? (= key "c"))
                          (do (.preventDefault e)
                              (! [:copy nil]))

                          ;; Ctrl+X - Cut
                          (and ctrl? (= key "x"))
                          (do (.preventDefault e)
                              (! [:cut nil]))

                          ;; Ctrl+V - Paste
                          (and ctrl? (= key "v"))
                          (do (.preventDefault e)
                              (! [:paste nil]))

                          ;; Ctrl+Z - Undo
                          (and ctrl? (not (.-shiftKey e)) (= key "z"))
                          (do (.preventDefault e)
                              (! [:undo nil]))

                          ;; Ctrl+Shift+Z or Ctrl+Y - Redo
                          (or (and ctrl? (.-shiftKey e) (= key "z"))
                              (and ctrl? (= key "y")))
                          (do (.preventDefault e)
                              (! [:redo nil]))

                          ;; Ctrl+S - Save
                          (and ctrl? (= key "s"))
                          (do (.preventDefault e)
                              (! [:save nil]))

                          ;; Ctrl+K - Toggle command panel
                          (and ctrl? (= key "k"))
                          (do (.preventDefault e)
                              (! [:toggle-command-panel nil]))

                          ;; Ctrl+J - Toggle AI response panel
                          (and ctrl? (= key "j"))
                          (do (.preventDefault e)
                              (! [:toggle-ai-panel nil]))

                          ;; Ctrl+B - Toggle file tree sidebar
                          (and ctrl? (= key "b"))
                          (do (.preventDefault e)
                              (! [:toggle-file-tree nil]))

                          ;; Ctrl+Shift+C - Copy AI response (when AI panel visible)
                          (and ctrl? (.-shiftKey e) (= key "C"))
                          (do (.preventDefault e)
                              (! [:copy-ai-response nil]))

                          ;; Ctrl+0 - Reset zoom to 100%
                          (and ctrl? (= key "0"))
                          (do (.preventDefault e)
                              (! [:reset-zoom nil]))

                          ;; Ctrl+Plus - Zoom in
                          (and ctrl? (or (= key "=") (= key "+")))
                          (do (.preventDefault e)
                              (! [:zoom-in nil]))

                          ;; Ctrl+Minus - Zoom out
                          (and ctrl? (= key "-"))
                          (do (.preventDefault e)
                              (! [:zoom-out nil]))

                          ;; Escape - Close panels / clear focus
                          (= key "Escape")
                          (do (.preventDefault e)
                              (! [:escape nil]))

                          ;; Ctrl+Arrow word navigation
                          (and ctrl? (contains? #{"ArrowLeft" "ArrowRight"} key))
                          (do (.preventDefault e)
                              (! [:word-nav key]))

                          ;; Navigation keys
                          (contains? #{"ArrowLeft" "ArrowRight" "ArrowUp" "ArrowDown"
                                       "Home" "End"} key)
                          (do (.preventDefault e)
                              (! [:keydown key]))

                          ;; Printable characters (length 1, not control keys)
                          (and (= 1 (.-length key))
                               (not ctrl?)
                               (not (.-altKey e)))
                          (do (.preventDefault e)
                              (! [:char-input key]))

                          ;; Enter key (plain, no modifier)
                          (and (= key "Enter") (not ctrl?))
                          (do (.preventDefault e)
                              (! [:enter nil]))

                          ;; Backspace
                          (= key "Backspace")
                          (do (.preventDefault e)
                              (! [:backspace nil]))

                          ;; Delete (forward delete)
                          (= key "Delete")
                          (do (.preventDefault e)
                              (! [:delete nil])))))]
        (.addEventListener node "keydown" handler)
        (fn [] (.removeEventListener node "keydown" handler))))))

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

;; Atom for external events (AI responses, file loading, etc.)
(defonce !external-events (atom nil))

(defn dispatch-external-event!
  "Dispatch an event from outside the main event loop.
   Used for async operations like AI responses."
  [event-type value]
  (reset! !external-events [event-type value]))

(defn start-loop! [node device ctx geometry line-lengths lines tokenize-fn layout-fn find-bracket-fn detect-folds-fn find-form-fn eval-form-fn atlas & [{:keys [ai-request-fn]}]]
  (let [;; Layout Configuration (Must match Editor defaults)
        font-size 16
        gutter-w  40           ;; Width of gutter for fold indicators
        layout-x  (+ 50 gutter-w)  ;; Shift text right to make room for gutter
        layout-y  100
        line-h    (* font-size 1.2)

        ;; Command panel configuration
        cmd-panel-h   40           ;; Height of command panel when visible

        ;; AI panel configuration
        ai-panel-w    400          ;; Width of AI response panel
        status-bar-h  24           ;; Height of status bar
        file-tree-w   200          ;; Width of file tree sidebar

        ;; External events flow (for AI responses, etc.)
        external-events (->> (m/watch !external-events)
                             (m/eduction (filter some?)))

        initial-state {:scroll-y      0
                       :scroll-x      0            ;; Horizontal scroll/pan
                       :zoom          1.0          ;; Zoom factor (1.0 = 100%)
                       :width         (.-innerWidth js/window)
                       :height        (.-innerHeight js/window)
                       :dpr           (or (.-devicePixelRatio js/window) 1)
                       :dragging?     false
                       :panning?      false        ;; Middle-mouse panning
                       :pan-start     nil          ;; {x y} of pan start
                       :sel-start     nil
                       :sel-end       nil
                       :desired-col   0
                       :caret-visible true
                       :folded-lines  #{}
                       :eval-result   nil
                       ;; Command panel state
                       :cmd-visible   false        ;; Is command panel shown?
                       :cmd-text      ""           ;; Text in command panel
                       :cmd-cursor    0            ;; Cursor position in command text
                       :focus         :editor      ;; :editor, :command-panel, or :ai-panel
                       ;; AI panel state
                       :ai-visible    false        ;; Is AI response panel shown?
                       :ai-loading    false        ;; Is AI request in progress?
                       :ai-response   ""           ;; AI response text
                       :ai-error      nil          ;; Error message if any
                       :ai-scroll-y   0            ;; Scroll position in AI panel
                       ;; Status bar state
                       :status-visible true        ;; Status bar always visible by default
                       ;; File tree state
                       :tree-visible  false        ;; File tree sidebar
                       :tree-files    [{:name "electric_flow.cljc" :path "src/app/electric_flow.cljc" :type :file}
                                       {:name "loop.cljs" :path "src/app/client/webgpu/loop.cljs" :type :file}
                                       {:name "editor.cljs" :path "src/app/client/webgpu/editor.cljs" :type :file}]
                       :tree-selected nil}         ;; Currently selected file path

        ;; Helper to calculate effective editor X offset based on panels
        calc-editor-x (fn [tree-visible?]
                        (if tree-visible?
                          (+ layout-x file-tree-w)  ;; Shift right when tree is open
                          layout-x))
        
        !state        (atom initial-state)
        !rect-sys     (atom (:rect geometry))
        !lines        (atom lines)
        !line-lengths (atom line-lengths)
        !text-geo     (atom (:text geometry))
        !fold-regions (atom [])
        !line-mapping (atom [])  ;; Maps visual line index -> logical line index
        !clipboard    (atom nil) ;; Internal clipboard for copy/cut/paste
        !undo-stack   (atom [])  ;; Stack of previous states for undo
        !redo-stack   (atom [])  ;; Stack of undone states for redo
        !editor-render-ops (atom [])  ;; Cached editor render ops for combining with command panel

        ;; Helper to save state before edit
        save-undo! (fn [lines-val cursor]
                     (swap! !undo-stack conj {:lines lines-val :cursor cursor})
                     ;; Limit stack size to 100
                     (when (> (count @!undo-stack) 100)
                       (swap! !undo-stack #(vec (drop 1 %))))
                     ;; Clear redo stack on new edit
                     (reset! !redo-stack []))
        
        ;; Event Streams
        wheel-deltas   (->> (>wheel-deltas node) (m/relieve (fn [_ x] x)))
        mouse-events   (->> (>mouse-events node) (m/relieve (fn [_ x] x)))
        keyboard-events (->> (>keyboard-events js/window) (m/relieve (fn [_ x] x)))
        window-metrics (<window-metrics)]

    (m/join {}
      
      ;; 1. STATE REDUCER
      (->> (mx/mix
             (m/eduction (map (fn [m] [:resize m])) window-metrics)
             (m/eduction (map (fn [x] [:wheel x])) wheel-deltas)
             (m/eduction (map (fn [v] [:blink v])) >blink-timer)
             mouse-events
             keyboard-events
             external-events)  ;; AI responses, file loads, etc.

           (m/reduce
             (fn [_ [type value]]
               (swap! !state
                      (fn [state]
                        (case type
                          :resize (let [{:keys [width height dpr]} value]
                                    (set! (.-width node)  (Math/floor (* width dpr)))
                                    (set! (.-height node) (Math/floor (* height dpr)))
                                    (merge state value))

                          :wheel  (let [{:keys [delta-y cursor-x cursor-y ctrl?]} value]
                                    (if ctrl?
                                      ;; Ctrl+Wheel = Zoom around cursor
                                      (let [current-zoom (:zoom state)
                                            scale (if (< delta-y 0) 1.05 0.95)
                                            new-zoom (-> (* current-zoom scale)
                                                         (max 0.1)   ;; Min 10%
                                                         (min 5.0))  ;; Max 500%
                                            ;; Adjust scroll to zoom around cursor
                                            scroll-x (:scroll-x state)
                                            scroll-y (:scroll-y state)
                                            ;; Calculate new scroll to keep cursor position stable
                                            pan-adjust (- 1 (/ new-zoom current-zoom))
                                            new-scroll-x (+ scroll-x (* (- cursor-x scroll-x) pan-adjust))
                                            new-scroll-y (+ scroll-y (* (- cursor-y scroll-y) pan-adjust))]
                                        (assoc state
                                               :zoom new-zoom
                                               :scroll-x new-scroll-x
                                               :scroll-y new-scroll-y))
                                      ;; Normal wheel = vertical scroll
                                      (update state :scroll-y + delta-y)))

                          :blink  (assoc state :caret-visible value)

                          :mousedown
                          (let [{:keys [x y button]} value
                                screen-w (:width state)
                                screen-h (:height state)
                                cmd-visible? (:cmd-visible state)
                                ai-visible? (:ai-visible state)
                                tree-visible? (:tree-visible state)
                                ;; Calculate panel regions
                                cmd-panel-top (if cmd-visible? (- screen-h cmd-panel-h) screen-h)
                                ai-panel-left (- screen-w ai-panel-w)
                                ;; Determine which region was clicked
                                clicked-in-cmd-panel? (and cmd-visible? (>= y cmd-panel-top))
                                clicked-in-ai-panel? (and ai-visible? (>= x ai-panel-left))
                                clicked-in-file-tree? (and tree-visible? (< x file-tree-w))
                                ;; Dynamic editor X offset
                                editor-x (calc-editor-x tree-visible?)]
                            (cond
                              ;; Middle button = start panning
                              (= button 1)
                              (assoc state :panning? true :pan-start {:x x :y y})

                              ;; Clicked in command panel
                              clicked-in-cmd-panel?
                              (let [char-w (* font-size 0.6)
                                    text (:cmd-text state)
                                    text-len (count text)
                                    cmd-text-x 60
                                    col (-> (/ (- x cmd-text-x) char-w)
                                            (Math/round)
                                            (max 0)
                                            (min text-len))]
                                (assoc state :focus :command-panel :cmd-cursor col :caret-visible true))

                              ;; Clicked in AI panel - focus AI panel
                              clicked-in-ai-panel?
                              (assoc state :focus :ai-panel)

                              ;; Clicked in file tree - handle file selection
                              clicked-in-file-tree?
                              (let [tree-files (:tree-files state)
                                    ;; Calculate which file was clicked (y position)
                                    file-y-start 40  ;; Below "FILES" title
                                    file-idx (Math/floor (/ (- y file-y-start) line-h))]
                                (if (and (>= file-idx 0) (< file-idx (count tree-files)))
                                  (let [selected-file (nth tree-files file-idx)]
                                    (js/console.log "Selected file:" (:name selected-file))
                                    (assoc state :tree-selected (:path selected-file)))
                                  state))

                              ;; Clicked in editor area
                              :else
                              (let [adj-y (+ y (:scroll-y state))
                                    ;; Adjust gutter position for file tree
                                    gutter-start (if tree-visible? (+ 50 file-tree-w) 50)]
                                ;; Check if click is in gutter area (for fold toggle)
                                (if (and (>= x gutter-start) (< x (+ gutter-start gutter-w)))
                                  ;; Gutter click - toggle fold
                                  (let [visual-line (max 0 (Math/floor (/ (- adj-y layout-y) line-h)))
                                        logical-line (get @!line-mapping visual-line visual-line)
                                        fold-region (first (filter #(= (:start-line %) logical-line) @!fold-regions))]
                                    (if fold-region
                                      (update state :folded-lines
                                              (fn [folded]
                                                (if (contains? folded logical-line)
                                                  (disj folded logical-line)
                                                  (conj folded logical-line))))
                                      (assoc state :focus :editor)))
                                  ;; Normal click - cursor placement
                                  (let [visual-line (max 0 (Math/floor (/ (- adj-y layout-y) line-h)))
                                        logical-line (get @!line-mapping visual-line (min visual-line (dec (count @!line-lengths))))
                                        line-len (get @!line-lengths logical-line 0)
                                        char-w (* font-size 0.6)
                                        col (-> (/ (- x editor-x) char-w)
                                                (Math/round)
                                                (max 0)
                                                (min line-len))
                                        pos {:line logical-line :col col}]
                                    (assoc state :dragging? true :sel-start pos :sel-end pos
                                           :desired-col col :caret-visible true :focus :editor))))))

                          :mousemove
                          (cond
                            ;; Panning with middle mouse
                            (:panning? state)
                            (let [{:keys [x y]} value
                                  pan-start (:pan-start state)
                                  dx (- x (:x pan-start))
                                  dy (- y (:y pan-start))]
                              (-> state
                                  (update :scroll-x - dx)
                                  (update :scroll-y - dy)
                                  (assoc :pan-start {:x x :y y})))

                            ;; Selection drag
                            (:dragging? state)
                            (let [{:keys [x y]} value
                                  adj-y (+ y (:scroll-y state))
                                  tree-visible? (:tree-visible state)
                                  editor-x (calc-editor-x tree-visible?)
                                  ;; Use visual->logical mapping for selection
                                  visual-line (max 0 (Math/floor (/ (- adj-y layout-y) line-h)))
                                  logical-line (get @!line-mapping visual-line (min visual-line (dec (count @!line-lengths))))
                                  line-len (get @!line-lengths logical-line 0)
                                  char-w (* font-size 0.6)
                                  col (-> (/ (- x editor-x) char-w)
                                          (Math/round)
                                          (max 0)
                                          (min line-len))
                                  pos {:line logical-line :col col}]
                              (assoc state :sel-end pos))

                            :else state)

                          :mouseup
                          (assoc state :dragging? false :panning? false :pan-start nil)

                          :char-input
                          (if (= (:focus state) :command-panel)
                            ;; Command panel input
                            (let [text (:cmd-text state)
                                  cursor (:cmd-cursor state)
                                  before (subs text 0 cursor)
                                  after (subs text cursor)
                                  new-text (str before value after)]
                              (assoc state
                                     :cmd-text new-text
                                     :cmd-cursor (inc cursor)
                                     :caret-visible true))
                            ;; Editor input
                            (if-let [pos (:sel-start state)]
                              (let [_ (save-undo! @!lines pos)
                                    line-idx (:line pos)
                                    col      (:col pos)
                                    current-line (get @!lines line-idx "")
                                    before (subs current-line 0 col)
                                    after  (subs current-line col)
                                    new-line (str before value after)
                                    new-lines (assoc @!lines line-idx new-line)
                                    new-line-lengths (mapv count new-lines)]
                                (reset! !lines new-lines)
                                (reset! !line-lengths new-line-lengths)
                                (assoc state
                                       :sel-start {:line line-idx :col (inc col)}
                                       :sel-end {:line line-idx :col (inc col)}
                                       :desired-col (inc col)
                                       :caret-visible true))
                              state))

                          :backspace
                          (if (= (:focus state) :command-panel)
                            ;; Command panel backspace
                            (let [text (:cmd-text state)
                                  cursor (:cmd-cursor state)]
                              (if (> cursor 0)
                                (let [before (subs text 0 (dec cursor))
                                      after (subs text cursor)]
                                  (assoc state
                                         :cmd-text (str before after)
                                         :cmd-cursor (dec cursor)
                                         :caret-visible true))
                                state))
                            ;; Editor backspace
                            (if-let [pos (:sel-start state)]
                              (let [line-idx (:line pos)
                                    col      (:col pos)]
                                (cond
                                  ;; At start of line - join with previous line
                                  (and (= col 0) (> line-idx 0))
                                  (let [_ (save-undo! @!lines pos)
                                        current-line (get @!lines line-idx "")
                                        prev-line (get @!lines (dec line-idx) "")
                                        prev-len (count prev-line)
                                        merged-line (str prev-line current-line)
                                        new-lines (vec (concat (subvec @!lines 0 (dec line-idx))
                                                               [merged-line]
                                                               (subvec @!lines (inc line-idx))))
                                        new-line-lengths (mapv count new-lines)]
                                    (reset! !lines new-lines)
                                    (reset! !line-lengths new-line-lengths)
                                    (assoc state
                                           :sel-start {:line (dec line-idx) :col prev-len}
                                           :sel-end {:line (dec line-idx) :col prev-len}
                                           :desired-col prev-len
                                           :caret-visible true))

                                  ;; Delete character before cursor
                                  (> col 0)
                                  (let [_ (save-undo! @!lines pos)
                                        current-line (get @!lines line-idx "")
                                        before (subs current-line 0 (dec col))
                                        after  (subs current-line col)
                                        new-line (str before after)
                                        new-lines (assoc @!lines line-idx new-line)
                                        new-line-lengths (mapv count new-lines)]
                                    (reset! !lines new-lines)
                                    (reset! !line-lengths new-line-lengths)
                                    (assoc state
                                           :sel-start {:line line-idx :col (dec col)}
                                           :sel-end {:line line-idx :col (dec col)}
                                           :desired-col (dec col)
                                           :caret-visible true))

                                  ;; At start of first line - do nothing
                                  :else state))
                              state))

                          :delete
                          (if-let [pos (:sel-start state)]
                            (let [line-idx (:line pos)
                                  col      (:col pos)
                                  current-line (get @!lines line-idx "")
                                  line-len (count current-line)
                                  max-line (dec (count @!lines))]
                              (cond
                                ;; At end of line - join with next line
                                (and (= col line-len) (< line-idx max-line))
                                (let [_ (save-undo! @!lines pos)
                                      next-line (get @!lines (inc line-idx) "")
                                      merged-line (str current-line next-line)
                                      new-lines (vec (concat (subvec @!lines 0 line-idx)
                                                             [merged-line]
                                                             (subvec @!lines (+ line-idx 2))))
                                      new-line-lengths (mapv count new-lines)]
                                  (reset! !lines new-lines)
                                  (reset! !line-lengths new-line-lengths)
                                  (assoc state :caret-visible true))

                                ;; Delete character at cursor
                                (< col line-len)
                                (let [_ (save-undo! @!lines pos)
                                      before (subs current-line 0 col)
                                      after  (subs current-line (inc col))
                                      new-line (str before after)
                                      new-lines (assoc @!lines line-idx new-line)
                                      new-line-lengths (mapv count new-lines)]
                                  (reset! !lines new-lines)
                                  (reset! !line-lengths new-line-lengths)
                                  (assoc state :caret-visible true))

                                ;; At end of last line - do nothing
                                :else state))
                            state)

                          :copy
                          (let [sel-start (:sel-start state)
                                sel-end (:sel-end state)]
                            (if (and sel-start sel-end
                                     (not (and (= (:line sel-start) (:line sel-end))
                                               (= (:col sel-start) (:col sel-end)))))
                              ;; Has selection - copy it
                              (let [[start end] (if (or (> (:line sel-start) (:line sel-end))
                                                        (and (= (:line sel-start) (:line sel-end))
                                                             (> (:col sel-start) (:col sel-end))))
                                                  [sel-end sel-start]
                                                  [sel-start sel-end])
                                    lines-val @!lines
                                    text (if (= (:line start) (:line end))
                                           ;; Single line selection
                                           (subs (get lines-val (:line start) "")
                                                 (:col start) (:col end))
                                           ;; Multi-line selection
                                           (let [first-line (subs (get lines-val (:line start) "") (:col start))
                                                 middle-lines (for [i (range (inc (:line start)) (:line end))]
                                                                (get lines-val i ""))
                                                 last-line (subs (get lines-val (:line end) "") 0 (:col end))]
                                             (str/join "\n" (concat [first-line] middle-lines [last-line]))))]
                                (reset! !clipboard text)
                                (js/console.log "Copied:" text)
                                state)
                              ;; No selection - copy current line
                              (when-let [pos sel-start]
                                (let [line-text (get @!lines (:line pos) "")]
                                  (reset! !clipboard (str line-text "\n"))
                                  (js/console.log "Copied line:" line-text)
                                  state))))

                          :cut
                          (let [sel-start (:sel-start state)
                                sel-end (:sel-end state)]
                            (if (and sel-start sel-end
                                     (not (and (= (:line sel-start) (:line sel-end))
                                               (= (:col sel-start) (:col sel-end)))))
                              ;; Has selection - copy then delete
                              (let [_ (save-undo! @!lines sel-start)
                                    [start end] (if (or (> (:line sel-start) (:line sel-end))
                                                        (and (= (:line sel-start) (:line sel-end))
                                                             (> (:col sel-start) (:col sel-end))))
                                                  [sel-end sel-start]
                                                  [sel-start sel-end])
                                    lines-val @!lines
                                    text (if (= (:line start) (:line end))
                                           (subs (get lines-val (:line start) "")
                                                 (:col start) (:col end))
                                           (let [first-line (subs (get lines-val (:line start) "") (:col start))
                                                 middle-lines (for [i (range (inc (:line start)) (:line end))]
                                                                (get lines-val i ""))
                                                 last-line (subs (get lines-val (:line end) "") 0 (:col end))]
                                             (str/join "\n" (concat [first-line] middle-lines [last-line]))))
                                    ;; Delete selection
                                    before (subs (get lines-val (:line start) "") 0 (:col start))
                                    after (subs (get lines-val (:line end) "") (:col end))
                                    merged-line (str before after)
                                    new-lines (vec (concat (subvec lines-val 0 (:line start))
                                                           [merged-line]
                                                           (subvec lines-val (inc (:line end)))))
                                    new-line-lengths (mapv count new-lines)]
                                (reset! !clipboard text)
                                (reset! !lines new-lines)
                                (reset! !line-lengths new-line-lengths)
                                (js/console.log "Cut:" text)
                                (assoc state :sel-start start :sel-end start
                                       :desired-col (:col start) :caret-visible true))
                              ;; No selection - cut current line
                              (when-let [pos sel-start]
                                (let [_ (save-undo! @!lines pos)
                                      line-idx (:line pos)
                                      line-text (get @!lines line-idx "")
                                      lines-val @!lines]
                                  (reset! !clipboard (str line-text "\n"))
                                  (if (= 1 (count lines-val))
                                    ;; Only one line - clear it
                                    (do (reset! !lines [""])
                                        (reset! !line-lengths [0])
                                        (assoc state :sel-start {:line 0 :col 0}
                                               :sel-end {:line 0 :col 0}
                                               :desired-col 0 :caret-visible true))
                                    ;; Multiple lines - remove this one
                                    (let [new-lines (vec (concat (subvec lines-val 0 line-idx)
                                                                 (subvec lines-val (inc line-idx))))
                                          new-line-lengths (mapv count new-lines)
                                          new-line-idx (min line-idx (dec (count new-lines)))]
                                      (reset! !lines new-lines)
                                      (reset! !line-lengths new-line-lengths)
                                      (assoc state :sel-start {:line new-line-idx :col 0}
                                             :sel-end {:line new-line-idx :col 0}
                                             :desired-col 0 :caret-visible true)))))))

                          :paste
                          (if-let [text @!clipboard]
                            (if-let [pos (:sel-start state)]
                              (let [_ (save-undo! @!lines pos)
                                    line-idx (:line pos)
                                    col (:col pos)
                                    current-line (get @!lines line-idx "")
                                    before (subs current-line 0 col)
                                    after (subs current-line col)
                                    paste-lines (str/split-lines text)
                                    new-lines
                                    (if (= 1 (count paste-lines))
                                      ;; Single line paste
                                      (assoc @!lines line-idx (str before (first paste-lines) after))
                                      ;; Multi-line paste
                                      (let [first-new-line (str before (first paste-lines))
                                            middle-lines (subvec paste-lines 1 (dec (count paste-lines)))
                                            last-new-line (str (last paste-lines) after)]
                                        (vec (concat (subvec @!lines 0 line-idx)
                                                     [first-new-line]
                                                     middle-lines
                                                     [last-new-line]
                                                     (subvec @!lines (inc line-idx))))))
                                    new-line-lengths (mapv count new-lines)
                                    ;; Calculate new cursor position
                                    new-pos (if (= 1 (count paste-lines))
                                              {:line line-idx :col (+ col (count (first paste-lines)))}
                                              {:line (+ line-idx (dec (count paste-lines)))
                                               :col (count (last paste-lines))})]
                                (reset! !lines new-lines)
                                (reset! !line-lengths new-line-lengths)
                                (assoc state :sel-start new-pos :sel-end new-pos
                                       :desired-col (:col new-pos) :caret-visible true))
                              state)
                            state)

                          :undo
                          (if-let [prev (peek @!undo-stack)]
                            (let [current-lines @!lines
                                  current-cursor (:sel-start state)]
                              ;; Push current state to redo stack
                              (swap! !redo-stack conj {:lines current-lines :cursor current-cursor})
                              ;; Pop from undo stack
                              (swap! !undo-stack pop)
                              ;; Restore previous state
                              (reset! !lines (:lines prev))
                              (reset! !line-lengths (mapv count (:lines prev)))
                              (let [cursor (:cursor prev)]
                                (assoc state :sel-start cursor :sel-end cursor
                                       :desired-col (:col cursor) :caret-visible true)))
                            state)

                          :redo
                          (if-let [next-state (peek @!redo-stack)]
                            (let [current-lines @!lines
                                  current-cursor (:sel-start state)]
                              ;; Push current state to undo stack
                              (swap! !undo-stack conj {:lines current-lines :cursor current-cursor})
                              ;; Pop from redo stack
                              (swap! !redo-stack pop)
                              ;; Restore next state
                              (reset! !lines (:lines next-state))
                              (reset! !line-lengths (mapv count (:lines next-state)))
                              (let [cursor (:cursor next-state)]
                                (assoc state :sel-start cursor :sel-end cursor
                                       :desired-col (:col cursor) :caret-visible true)))
                            state)

                          :save
                          (let [content (str/join "\n" @!lines)
                                blob (js/Blob. #js [content] #js {:type "text/plain"})
                                url (js/URL.createObjectURL blob)
                                a (js/document.createElement "a")]
                            (set! (.-href a) url)
                            (set! (.-download a) "code.clj")
                            (.click a)
                            (js/URL.revokeObjectURL url)
                            (js/console.log "Saved file: code.clj" (count content) "bytes")
                            state)

                          :enter
                          (if (= (:focus state) :command-panel)
                            ;; Command panel Enter - submit AI request
                            (let [cmd-text (:cmd-text state)]
                              (if (seq cmd-text)
                                (do
                                  (js/console.log "AI Request submitted:" cmd-text)
                                  ;; Trigger AI request with callback
                                  (when ai-request-fn
                                    (let [code-context (str/join "\n" @!lines)]
                                      (ai-request-fn cmd-text code-context)))
                                  ;; Show AI panel with loading state, clear command
                                  (assoc state
                                         :cmd-text ""
                                         :cmd-cursor 0
                                         :cmd-visible false
                                         :focus :editor
                                         :ai-visible true
                                         :ai-loading true
                                         :ai-error nil))
                                ;; No text - just close panel
                                (assoc state
                                       :cmd-text ""
                                       :cmd-cursor 0
                                       :cmd-visible false
                                       :focus :editor)))
                            ;; Editor Enter - new line
                            (if-let [pos (:sel-start state)]
                              (let [_ (save-undo! @!lines pos)
                                    line-idx (:line pos)
                                    col      (:col pos)
                                    current-line (get @!lines line-idx "")
                                    before (subs current-line 0 col)
                                    after  (subs current-line col)
                                    new-lines (vec (concat (subvec @!lines 0 line-idx)
                                                           [before after]
                                                           (subvec @!lines (inc line-idx))))
                                    new-line-lengths (mapv count new-lines)]
                                (reset! !lines new-lines)
                                (reset! !line-lengths new-line-lengths)
                                (assoc state
                                       :sel-start {:line (inc line-idx) :col 0}
                                       :sel-end {:line (inc line-idx) :col 0}
                                       :desired-col 0
                                       :caret-visible true))
                              state))

                          :keydown
                          (if (= (:focus state) :command-panel)
                            ;; Command panel navigation
                            (let [text (:cmd-text state)
                                  cursor (:cmd-cursor state)
                                  text-len (count text)
                                  new-cursor
                                  (case value
                                    "ArrowLeft" (max 0 (dec cursor))
                                    "ArrowRight" (min text-len (inc cursor))
                                    "Home" 0
                                    "End" text-len
                                    cursor)]
                              (assoc state :cmd-cursor new-cursor :caret-visible true))
                            ;; Editor navigation
                            (if-let [pos (:sel-start state)]
                              (let [line (:line pos)
                                    col  (:col pos)
                                    desired (:desired-col state)
                                    line-lengths-val @!line-lengths
                                    max-line (dec (count line-lengths-val))
                                    line-len (get line-lengths-val line 0)
                                    [new-pos new-desired]
                                    (case value
                                      "ArrowLeft"
                                      (let [np (if (> col 0)
                                                 {:line line :col (dec col)}
                                                 (if (> line 0)
                                                   (let [prev-len (get line-lengths-val (dec line) 0)]
                                                     {:line (dec line) :col prev-len})
                                                   pos))]
                                        [np (:col np)])

                                      "ArrowRight"
                                      (let [np (if (< col line-len)
                                                 {:line line :col (inc col)}
                                                 (if (< line max-line)
                                                   {:line (inc line) :col 0}
                                                   pos))]
                                        [np (:col np)])

                                      "ArrowUp"
                                      (if (> line 0)
                                        (let [prev-len (get line-lengths-val (dec line) 0)]
                                          [{:line (dec line) :col (min desired prev-len)} desired])
                                        [pos desired])

                                      "ArrowDown"
                                      (if (< line max-line)
                                        (let [next-len (get line-lengths-val (inc line) 0)]
                                          [{:line (inc line) :col (min desired next-len)} desired])
                                        [pos desired])

                                      "Home"
                                      [{:line line :col 0} 0]

                                      "End"
                                      [{:line line :col line-len} line-len]

                                      [pos desired])

                                    ;; Auto-scroll to keep caret visible
                                    caret-y (+ layout-y (* (:line new-pos) line-h))
                                    viewport-top (:scroll-y state)
                                    viewport-bottom (+ viewport-top (:height state))

                                    ;; Add padding (one line worth)
                                    padding line-h

                                    new-scroll (cond
                                                 ;; Caret above viewport - scroll up
                                                 (< caret-y (+ viewport-top padding))
                                                 (max 0 (- caret-y padding))

                                                 ;; Caret below viewport - scroll down
                                                 (> (+ caret-y line-h) (- viewport-bottom padding))
                                                 (+ (- caret-y (:height state)) line-h padding)

                                                 ;; Caret in view - don't scroll
                                                 :else
                                                 (:scroll-y state))]

                                (assoc state :sel-start new-pos :sel-end new-pos
                                       :desired-col new-desired :caret-visible true
                                       :scroll-y new-scroll))
                              state))

                          :word-nav
                          (if-let [pos (:sel-start state)]
                            (let [line-idx (:line pos)
                                  col (:col pos)
                                  current-line (get @!lines line-idx "")
                                  line-lengths-val @!line-lengths
                                  max-line (dec (count line-lengths-val))
                                  ;; Word boundary detection helper
                                  word-char? (fn [c] (re-matches #"[\w]" (str c)))
                                  new-pos
                                  (case value
                                    "ArrowLeft"
                                    (if (= col 0)
                                      ;; At start of line, go to end of previous line
                                      (if (> line-idx 0)
                                        {:line (dec line-idx) :col (get line-lengths-val (dec line-idx) 0)}
                                        pos)
                                      ;; Find previous word boundary
                                      (let [before (subs current-line 0 col)
                                            ;; Skip whitespace, then skip word chars
                                            skip-ws (loop [i (dec (count before))]
                                                      (if (and (>= i 0)
                                                               (= \space (nth before i)))
                                                        (recur (dec i))
                                                        i))
                                            ;; Now skip word chars
                                            new-col (loop [i skip-ws]
                                                      (if (and (>= i 0)
                                                               (word-char? (nth before i)))
                                                        (recur (dec i))
                                                        (inc i)))]
                                        {:line line-idx :col (max 0 new-col)}))

                                    "ArrowRight"
                                    (let [line-len (count current-line)]
                                      (if (= col line-len)
                                        ;; At end of line, go to start of next line
                                        (if (< line-idx max-line)
                                          {:line (inc line-idx) :col 0}
                                          pos)
                                        ;; Find next word boundary
                                        (let [after (subs current-line col)
                                              ;; Skip current word chars, then skip whitespace
                                              skip-word (loop [i 0]
                                                          (if (and (< i (count after))
                                                                   (word-char? (nth after i)))
                                                            (recur (inc i))
                                                            i))
                                              ;; Now skip whitespace
                                              new-col (loop [i skip-word]
                                                        (if (and (< i (count after))
                                                                 (= \space (nth after i)))
                                                          (recur (inc i))
                                                          i))]
                                          {:line line-idx :col (+ col new-col)})))

                                    pos)]
                              (assoc state :sel-start new-pos :sel-end new-pos
                                     :desired-col (:col new-pos) :caret-visible true))
                            state)

                          :eval
                          (if-let [pos (:sel-start state)]
                            (if-let [form-info (find-form-fn pos @!lines @!line-lengths)]
                              (let [result-text (eval-form-fn (:form-str form-info))
                                    ;; Show result after the form's end line
                                    result-line (:end-line form-info)]
                                (js/console.log "SCI Eval:" (:form-str form-info) "=>" result-text)
                                (assoc state :eval-result {:text result-text
                                                           :line result-line
                                                           :expires-at (+ (js/Date.now) 5000)}))
                              ;; No form found at cursor
                              (do (js/console.log "SCI: No form at cursor")
                                  (assoc state :eval-result {:text "No form at cursor"
                                                             :line (:line pos)
                                                             :expires-at (+ (js/Date.now) 2000)})))
                            state)

                          ;; === COMMAND PANEL EVENTS ===

                          :toggle-command-panel
                          (if (:cmd-visible state)
                            ;; Close panel, return focus to editor
                            (assoc state :cmd-visible false :focus :editor)
                            ;; Open panel, focus it
                            (assoc state :cmd-visible true :focus :command-panel :caret-visible true))

                          ;; === AI PANEL EVENTS ===

                          :toggle-ai-panel
                          (if (:ai-visible state)
                            ;; Close AI panel
                            (assoc state :ai-visible false)
                            ;; Open AI panel
                            (assoc state :ai-visible true))

                          :ai-request-start
                          (assoc state :ai-loading true :ai-error nil)

                          :ai-request-success
                          (assoc state :ai-loading false :ai-response value :ai-visible true)

                          :ai-request-error
                          (assoc state :ai-loading false :ai-error value)

                          :copy-ai-response
                          (let [response (:ai-response state)]
                            (when (seq response)
                              ;; Copy to browser clipboard
                              (-> (js/navigator.clipboard.writeText response)
                                  (.then #(js/console.log "AI response copied to clipboard"))
                                  (.catch #(js/console.error "Failed to copy:" %)))
                              ;; Also store internally
                              (reset! !clipboard response)
                              (js/console.log "AI Response copied:" (count response) "chars"))
                            state)

                          ;; === FILE TREE EVENTS ===

                          :toggle-file-tree
                          (update state :tree-visible not)

                          :select-file
                          (assoc state :tree-selected value)

                          ;; === ZOOM EVENTS ===
                          :reset-zoom
                          (assoc state :zoom 1.0 :scroll-x 0 :scroll-y 0)

                          :zoom-in
                          (update state :zoom #(min 5.0 (* % 1.1)))

                          :zoom-out
                          (update state :zoom #(max 0.1 (* % 0.9)))

                          ;; === ESCAPE - Close all panels ===

                          :escape
                          (cond
                            ;; If command panel is open, close it first
                            (:cmd-visible state)
                            (assoc state :cmd-visible false :focus :editor)
                            ;; If AI panel is open, close it
                            (:ai-visible state)
                            (assoc state :ai-visible false)
                            ;; Otherwise, clear selection
                            :else
                            (assoc state :sel-start nil :sel-end nil))

                          state))))
             nil))

      ;; 2a. EDITOR TEXT UPDATER (re-tokenize when lines, fold state, or tree visibility changes)
      (->> (m/latest vector
                     (m/watch !lines)
                     (m/eduction (map (fn [s] {:folded-lines (:folded-lines s)
                                                :tree-visible (:tree-visible s)}))
                                 (m/watch !state)))
           (m/eduction (dedupe))
           (m/reduce
             (fn [_ [new-lines {:keys [folded-lines tree-visible]}]]
               (let [new-line-lengths (mapv count new-lines)
                     ;; Detect fold regions from parse tree
                     fold-regions (or (detect-folds-fn new-lines new-line-lengths) [])
                     _ (reset! !fold-regions fold-regions)
                     _ (reset! !line-lengths new-line-lengths)
                     tokenized-lines (mapv tokenize-fn new-lines)
                     ;; Calculate dynamic layout-x based on tree visibility
                     effective-layout-x (calc-editor-x tree-visible)
                     ;; Layout with folding - returns {:render-ops :line-mapping}
                     layout-result (layout-fn tokenized-lines effective-layout-x layout-y font-size
                                              fold-regions folded-lines)
                     render-ops (:render-ops layout-result)]
                 (reset! !line-mapping (:line-mapping layout-result))
                 ;; Cache editor render ops
                 (reset! !editor-render-ops render-ops))
               nil)
             nil))

      ;; 2b. COMBINED TEXT UPDATER (combines editor + all panel text)
      ;; Updates GPU when editor ops change OR when any panel state changes
      (->> (m/latest vector
                     (m/watch !editor-render-ops)
                     (m/eduction (map (fn [s] {:cmd-visible (:cmd-visible s)
                                                :cmd-text (:cmd-text s)
                                                :scroll-y (:scroll-y s)
                                                :height (:height s)
                                                :width (:width s)
                                                :ai-visible (:ai-visible s)
                                                :ai-loading (:ai-loading s)
                                                :ai-response (:ai-response s)
                                                :status-visible (:status-visible s)
                                                :sel-start (:sel-start s)
                                                :focus (:focus s)
                                                :tree-visible (:tree-visible s)
                                                :tree-files (:tree-files s)
                                                :tree-selected (:tree-selected s)
                                                :zoom (:zoom s)}))
                                 (m/watch !state)))
           (m/eduction (dedupe))
           (m/reduce
             (fn [_ [editor-ops {:keys [cmd-visible cmd-text scroll-y height width
                                         ai-visible ai-loading ai-response
                                         status-visible sel-start focus tree-visible
                                         tree-files tree-selected zoom]}]]
               (let [;; === COMMAND PANEL TEXT ===
                     cmd-panel-y (+ scroll-y (- height cmd-panel-h))
                     cmd-text-y (+ cmd-panel-y 12 font-size)

                     cmd-text-token (when cmd-visible
                                      (if (seq cmd-text)
                                        [{:text cmd-text
                                          :type :text
                                          :from 0
                                          :to (count cmd-text)
                                          :x 60
                                          :y cmd-text-y
                                          :size font-size
                                          :r 0.9 :g 0.9 :b 0.9 :a 1.0}]
                                        [{:text "Type a task..."
                                          :type :comment
                                          :from 0
                                          :to 14
                                          :x 60
                                          :y cmd-text-y
                                          :size font-size
                                          :r 0.5 :g 0.5 :b 0.5 :a 0.7}]))

                     cmd-prompt-token (when cmd-visible
                                        [{:text "> "
                                          :type :macro
                                          :from 0
                                          :to 2
                                          :x 40
                                          :y cmd-text-y
                                          :size font-size
                                          :r 0.3 :g 0.6 :b 1.0 :a 1.0}])

                     ;; === AI PANEL TEXT ===
                     ai-panel-x (- width ai-panel-w)
                     ai-title-token (when ai-visible
                                      [{:text "AI Response"
                                        :type :macro
                                        :from 0
                                        :to 11
                                        :x (+ ai-panel-x 16)
                                        :y (+ scroll-y 24)
                                        :size font-size
                                        :r 0.6 :g 0.7 :b 1.0 :a 1.0}])

                     ;; Split AI response into lines and render
                     ai-response-lines (when (and ai-visible (seq ai-response))
                                         (let [lines (str/split-lines ai-response)
                                               max-chars 45  ;; Approximate chars per line
                                               wrapped-lines (mapcat (fn [line]
                                                                       (if (<= (count line) max-chars)
                                                                         [line]
                                                                         ;; Simple word wrap
                                                                         (loop [remaining line
                                                                                result []]
                                                                           (if (<= (count remaining) max-chars)
                                                                             (conj result remaining)
                                                                             (let [break-at (or (str/last-index-of (subs remaining 0 max-chars) " ")
                                                                                               max-chars)]
                                                                               (recur (subs remaining (inc break-at))
                                                                                      (conj result (subs remaining 0 break-at))))))))
                                                                     lines)]
                                           (map-indexed
                                             (fn [idx line]
                                               [{:text line
                                                 :type :text
                                                 :from 0
                                                 :to (count line)
                                                 :x (+ ai-panel-x 16)
                                                 :y (+ scroll-y 50 (* idx line-h))
                                                 :size (- font-size 2)  ;; Slightly smaller
                                                 :r 0.85 :g 0.85 :b 0.85 :a 1.0}])
                                             (take 30 wrapped-lines))))  ;; Limit to 30 visible lines

                     ai-loading-text (when (and ai-visible ai-loading)
                                       [{:text "Thinking..."
                                         :type :comment
                                         :from 0
                                         :to 11
                                         :x (+ ai-panel-x 16)
                                         :y (+ scroll-y 50)
                                         :size font-size
                                         :r 0.5 :g 0.6 :b 0.9 :a 0.8}])

                     ;; === STATUS BAR TEXT ===
                     status-y (+ scroll-y (- height status-bar-h
                                              (if cmd-visible cmd-panel-h 0)))
                     line-col-text (if sel-start
                                     (str "Ln " (inc (:line sel-start)) ", Col " (inc (:col sel-start)))
                                     "Ln 1, Col 1")
                     focus-text (case focus
                                  :editor "EDIT"
                                  :command-panel "CMD"
                                  :ai-panel "AI"
                                  "")

                     status-line-col-token (when status-visible
                                             [{:text line-col-text
                                               :type :text
                                               :from 0
                                               :to (count line-col-text)
                                               :x 12
                                               :y (+ status-y 16)
                                               :size (- font-size 2)
                                               :r 0.7 :g 0.7 :b 0.7 :a 1.0}])

                     status-focus-token (when status-visible
                                          [{:text focus-text
                                            :type :keyword
                                            :from 0
                                            :to (count focus-text)
                                            :x (- width 60)
                                            :y (+ status-y 16)
                                            :size (- font-size 2)
                                            :r 0.5 :g 0.8 :b 0.5 :a 1.0}])

                     ;; Zoom indicator in status bar
                     zoom-text (str (Math/round (* (or zoom 1.0) 100)) "%")
                     status-zoom-token (when status-visible
                                         [{:text zoom-text
                                           :type :number
                                           :from 0
                                           :to (count zoom-text)
                                           :x (- width 130)
                                           :y (+ status-y 16)
                                           :size (- font-size 2)
                                           :r 0.8 :g 0.7 :b 0.4 :a 1.0}])

                     ;; === FILE TREE TEXT ===
                     tree-title-token (when tree-visible
                                        [{:text "FILES"
                                          :type :macro
                                          :from 0
                                          :to 5
                                          :x 12
                                          :y (+ scroll-y 24)
                                          :size (- font-size 2)
                                          :r 0.6 :g 0.7 :b 1.0 :a 1.0}])

                     ;; File tree items
                     tree-file-tokens (when (and tree-visible (seq tree-files))
                                        (map-indexed
                                          (fn [idx file]
                                            (let [is-selected? (= (:path file) tree-selected)
                                                  file-y (+ scroll-y 44 (* idx line-h))]
                                              [{:text (:name file)
                                                :type (if is-selected? :keyword :text)
                                                :from 0
                                                :to (count (:name file))
                                                :x 16
                                                :y file-y
                                                :size (- font-size 2)
                                                :r (if is-selected? 0.4 0.8)
                                                :g (if is-selected? 0.8 0.8)
                                                :b (if is-selected? 0.9 0.8)
                                                :a 1.0}]))
                                          tree-files))

                     ;; Combine all text ops
                     render-ops (vec (concat editor-ops
                                             (when cmd-prompt-token [cmd-prompt-token])
                                             (when cmd-text-token [cmd-text-token])
                                             (when ai-title-token [ai-title-token])
                                             (when ai-loading-text [ai-loading-text])
                                             (or ai-response-lines [])
                                             (when status-line-col-token [status-line-col-token])
                                             (when status-zoom-token [status-zoom-token])
                                             (when status-focus-token [status-focus-token])
                                             (when tree-title-token [tree-title-token])
                                             (or tree-file-tokens [])))]
                 (reset! !text-geo (editor/update-text-data device (:text geometry) render-ops atlas font-size)))
               nil)
             nil))

      ;; 3. SELECTION & CARET GEOMETRY UPDATER (with bracket matching + fold indicators + eval results + all panels)
      (->> (m/watch !state)
           (m/eduction
             (map (fn [s] {:sel-start (:sel-start s)
                           :sel-end (:sel-end s)
                           :caret-visible (:caret-visible s)
                           :folded-lines (:folded-lines s)
                           :eval-result (:eval-result s)
                           :cmd-visible (:cmd-visible s)
                           :cmd-text (:cmd-text s)
                           :cmd-cursor (:cmd-cursor s)
                           :focus (:focus s)
                           :width (:width s)
                           :height (:height s)
                           ;; AI panel
                           :ai-visible (:ai-visible s)
                           :ai-loading (:ai-loading s)
                           :ai-response (:ai-response s)
                           ;; Status bar
                           :status-visible (:status-visible s)
                           ;; File tree
                           :tree-visible (:tree-visible s)}))
             (dedupe))
           (m/reduce
             (fn [_ {:keys [sel-start sel-end caret-visible folded-lines eval-result
                            cmd-visible cmd-text cmd-cursor focus width height
                            ai-visible ai-loading ai-response
                            status-visible tree-visible]}]
               (let [line-mapping @!line-mapping
                     ;; Dynamic layout-x based on tree visibility
                     effective-layout-x (calc-editor-x tree-visible)
                     ;; Gutter position shifts with tree
                     gutter-base-x (if tree-visible (+ 50 file-tree-w) 50)
                     ;; Create logical->visual line mapping (inverse)
                     logical->visual (reduce-kv (fn [m visual-idx logical-idx]
                                                  (assoc m logical-idx visual-idx))
                                                {}
                                                (vec line-mapping))
                     ;; Helper to get visual y for a logical line
                     logical-line->visual-y (fn [logical-line]
                                              (if-let [visual-idx (get logical->visual logical-line)]
                                                (+ layout-y (* visual-idx line-h))
                                                nil))  ;; Line is hidden (folded)

                     has-selection? (and sel-start sel-end
                                         (not (and (= (:line sel-start) (:line sel-end))
                                                   (= (:col sel-start) (:col sel-end)))))
                     ;; Calculate fold indicator rects in gutter (at visual positions)
                     fold-rects (keep (fn [{:keys [start-line]}]
                                        (when-let [visual-y (logical-line->visual-y start-line)]
                                          (let [is-folded? (contains? folded-lines start-line)
                                                indicator-size 10
                                                x (+ gutter-base-x (/ (- gutter-w indicator-size) 2))
                                                y (+ visual-y (/ (- line-h indicator-size) 2))]
                                            {:x x :y y :w indicator-size :h indicator-size
                                             ;; Gold for expanded, blue for folded
                                             :r (if is-folded? 0.3 0.7)
                                             :g (if is-folded? 0.5 0.6)
                                             :b (if is-folded? 0.9 0.3)
                                             :a 0.8})))
                                      @!fold-regions)
                     ;; Calculate bracket match highlights (at visual positions)
                     bracket-match (when (and sel-start (not has-selection?))
                                     (find-bracket-fn sel-start @!lines @!line-lengths))
                     bracket-rects (when bracket-match
                                     (let [{:keys [open close]} bracket-match
                                           char-w (* font-size 0.6)]
                                       (keep (fn [{:keys [line col]}]
                                               (when-let [visual-y (logical-line->visual-y line)]
                                                 {:x (+ effective-layout-x (* col char-w))
                                                  :y visual-y
                                                  :w char-w
                                                  :h line-h
                                                  :r 0.8 :g 0.6 :b 0.2 :a 0.4}))
                                             [open close])))
                     ;; Calculate caret rect at visual position (only for editor focus)
                     editor-caret-rect (when (and sel-start caret-visible (not has-selection?)
                                                  (= focus :editor))
                                         (when-let [visual-y (logical-line->visual-y (:line sel-start))]
                                           (let [char-w (* font-size 0.6)]
                                             {:x (+ effective-layout-x (* (:col sel-start) char-w))
                                              :y visual-y
                                              :w 2
                                              :h line-h
                                              :r 0.9 :g 0.9 :b 0.9 :a 1.0})))
                     ;; Calculate selection rects at visual positions
                     selection-rects (when has-selection?
                                       (let [[start end] (if (or (> (:line sel-start) (:line sel-end))
                                                                 (and (= (:line sel-start) (:line sel-end))
                                                                      (> (:col sel-start) (:col sel-end))))
                                                           [sel-end sel-start]
                                                           [sel-start sel-end])
                                             char-w (* font-size 0.6)]
                                         (keep (fn [logical-line]
                                                 (when-let [visual-y (logical-line->visual-y logical-line)]
                                                   (let [line-len (get @!line-lengths logical-line 0)
                                                         col-start (if (= logical-line (:line start)) (:col start) 0)
                                                         col-end (if (= logical-line (:line end)) (:col end) line-len)
                                                         width-chars (- col-end col-start)]
                                                     (when (> width-chars 0)
                                                       {:x (+ effective-layout-x (* col-start char-w))
                                                        :y visual-y
                                                        :w (* width-chars char-w)
                                                        :h line-h
                                                        :r 0.2 :g 0.4 :b 0.9 :a 0.5}))))
                                               (range (:line start) (inc (:line end))))))
                     ;; Eval result indicator (shows for 5 seconds)
                     eval-rect (when eval-result
                                 (let [now (js/Date.now)]
                                   (when (< now (:expires-at eval-result))
                                     ;; Render background rect for result
                                     (when-let [visual-y (logical-line->visual-y (:line eval-result))]
                                       ;; Position result indicator to the right of the line
                                       (let [line-len (get @!line-lengths (:line eval-result) 0)
                                             char-w (* font-size 0.6)
                                             result-x (+ effective-layout-x (* (+ line-len 2) char-w))
                                             result-w (* (count (:text eval-result)) char-w)]
                                         {:x result-x
                                          :y visual-y
                                          :w (+ result-w 16)  ;; padding
                                          :h line-h
                                          ;; Green background for success, red for error
                                          :r (if (str/starts-with? (:text eval-result) "=>") 0.1 0.4)
                                          :g (if (str/starts-with? (:text eval-result) "=>") 0.3 0.1)
                                          :b 0.1
                                          :a 0.8})))))

                     ;; === COMMAND PANEL RECTS ===
                     ;; Command panel is rendered at fixed screen position (not scrolled)
                     ;; We need to account for scroll-y to position it correctly
                     scroll-y (:scroll-y @!state)
                     cmd-panel-y (+ scroll-y (- height cmd-panel-h))  ;; Fixed to bottom of viewport

                     ;; Command panel background
                     cmd-bg-rect (when cmd-visible
                                   {:x 0
                                    :y cmd-panel-y
                                    :w width
                                    :h cmd-panel-h
                                    :r 0.15 :g 0.15 :b 0.2 :a 1.0})

                     ;; Command panel caret (only when focused)
                     cmd-caret-rect (when (and cmd-visible caret-visible (= focus :command-panel))
                                      (let [char-w (* font-size 0.6)
                                            cmd-text-x 60
                                            caret-x (+ cmd-text-x (* cmd-cursor char-w))
                                            caret-y (+ cmd-panel-y 8)]  ;; Vertically center in panel
                                        {:x caret-x
                                         :y caret-y
                                         :w 2
                                         :h (- cmd-panel-h 16)
                                         :r 0.9 :g 0.9 :b 0.9 :a 1.0}))

                     ;; === AI PANEL RECTS ===
                     ;; AI panel on right side of screen
                     ai-panel-x (- width ai-panel-w)
                     ai-panel-top-y scroll-y  ;; Start from top of viewport

                     ;; AI panel background
                     ai-bg-rect (when ai-visible
                                  {:x (+ scroll-y ai-panel-x)  ;; Adjust for any horizontal scroll
                                   :y scroll-y
                                   :w ai-panel-w
                                   :h height
                                   :r 0.1 :g 0.1 :b 0.15 :a 0.95})

                     ;; AI loading indicator (pulsing rectangle)
                     ai-loading-rect (when (and ai-visible ai-loading)
                                       {:x (+ ai-panel-x 20)
                                        :y (+ scroll-y 20)
                                        :w (- ai-panel-w 40)
                                        :h 4
                                        :r 0.3 :g 0.5 :b 1.0 :a 0.8})

                     ;; === STATUS BAR RECTS ===
                     ;; Status bar at very bottom (below command panel if visible)
                     status-y (+ scroll-y (- height status-bar-h
                                              (if cmd-visible cmd-panel-h 0)))

                     status-bg-rect (when status-visible
                                      {:x 0
                                       :y status-y
                                       :w width
                                       :h status-bar-h
                                       :r 0.12 :g 0.12 :b 0.18 :a 1.0})

                     ;; === FILE TREE RECTS ===
                     ;; File tree on left side
                     tree-bg-rect (when tree-visible
                                    {:x 0
                                     :y scroll-y
                                     :w file-tree-w
                                     :h height
                                     :r 0.08 :g 0.08 :b 0.12 :a 0.98})

                     ;; Combine all rects
                     rects (vec (concat fold-rects
                                        (or bracket-rects [])
                                        (or selection-rects [])
                                        (if editor-caret-rect [editor-caret-rect] [])
                                        (if eval-rect [eval-rect] [])
                                        ;; Panel backgrounds (drawn first, behind content)
                                        (if tree-bg-rect [tree-bg-rect] [])
                                        (if ai-bg-rect [ai-bg-rect] [])
                                        (if ai-loading-rect [ai-loading-rect] [])
                                        (if status-bg-rect [status-bg-rect] [])
                                        (if cmd-bg-rect [cmd-bg-rect] [])
                                        (if cmd-caret-rect [cmd-caret-rect] [])))]
                 (reset! !rect-sys (editor/update-rects device (:rect geometry) rects)))
               nil)
             nil))

      ;; 4. RENDER LOOP (just draws, no rect calculation)
      (m/reduce
        (fn [_ state]
          (let [scroll-x (:scroll-x state 0)
                scroll-y (:scroll-y state 0)
                zoom (:zoom state 1.0)]
            (editor/draw-frame! device ctx
                                @!text-geo
                                @!rect-sys
                                (:camera-floats (:pipelines geometry))
                                (:pass-descriptor (:pipelines geometry))
                                (- scroll-x) (- scroll-y) zoom
                                (:width state) (:height state)))
          nil)
        nil
        (m/sample (fn [s _t] s) (m/watch !state) >raf)))))
