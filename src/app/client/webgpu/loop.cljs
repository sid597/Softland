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

                          ;; Escape - Close command panel / clear focus
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

(defn start-loop! [node device ctx geometry line-lengths lines tokenize-fn layout-fn find-bracket-fn detect-folds-fn find-form-fn eval-form-fn atlas]
  (let [;; Layout Configuration (Must match Editor defaults)
        font-size 16
        gutter-w  40           ;; Width of gutter for fold indicators
        layout-x  (+ 50 gutter-w)  ;; Shift text right to make room for gutter
        layout-y  100
        line-h    (* font-size 1.2)

        ;; Command panel configuration
        cmd-panel-h   40           ;; Height of command panel when visible

        initial-state {:scroll-y      0
                       :width         (.-innerWidth js/window)
                       :height        (.-innerHeight js/window)
                       :dpr           (or (.-devicePixelRatio js/window) 1)
                       :dragging?     false
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
                       :focus         :editor}     ;; :editor or :command-panel
        
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
        wheel-deltas   (->> (>wheel-deltas node) (m/relieve +))
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
             keyboard-events)

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
                                screen-h (:height state)
                                cmd-visible? (:cmd-visible state)
                                ;; Command panel occupies bottom cmd-panel-h pixels when visible
                                cmd-panel-top (if cmd-visible? (- screen-h cmd-panel-h) screen-h)
                                clicked-in-cmd-panel? (and cmd-visible? (>= y cmd-panel-top))]
                            (if clicked-in-cmd-panel?
                              ;; Clicked in command panel - focus it and position cursor
                              (let [char-w (* font-size 0.6)
                                    text (:cmd-text state)
                                    text-len (count text)
                                    ;; Command panel text starts at x=60 (with some padding)
                                    cmd-text-x 60
                                    col (-> (/ (- x cmd-text-x) char-w)
                                            (Math/round)
                                            (max 0)
                                            (min text-len))]
                                (assoc state :focus :command-panel :cmd-cursor col :caret-visible true))
                              ;; Clicked in editor area
                              (let [adj-y (+ y (:scroll-y state))]
                                ;; Check if click is in gutter area (for fold toggle)
                                ;; Gutter spans from x=50 to x=50+gutter-w
                                (if (and (>= x 50) (< x (+ 50 gutter-w)))
                                  ;; Gutter click - toggle fold
                                  ;; Use visual line to find which fold indicator was clicked
                                  (let [visual-line (max 0 (Math/floor (/ (- adj-y layout-y) line-h)))
                                        ;; Map visual line to logical line
                                        logical-line (get @!line-mapping visual-line visual-line)
                                        ;; Find fold region starting at this logical line
                                        fold-region (first (filter #(= (:start-line %) logical-line) @!fold-regions))]
                                    (if fold-region
                                      (update state :folded-lines
                                              (fn [folded]
                                                (if (contains? folded logical-line)
                                                  (disj folded logical-line)
                                                  (conj folded logical-line))))
                                      ;; Not on a fold indicator - just focus editor
                                      (assoc state :focus :editor)))
                                  ;; Normal click - cursor placement with visual->logical mapping
                                  (let [visual-line (max 0 (Math/floor (/ (- adj-y layout-y) line-h)))
                                        logical-line (get @!line-mapping visual-line (min visual-line (dec (count @!line-lengths))))
                                        line-len (get @!line-lengths logical-line 0)
                                        char-w (* font-size 0.6)
                                        col (-> (/ (- x layout-x) char-w)
                                                (Math/round)
                                                (max 0)
                                                (min line-len))
                                        pos {:line logical-line :col col}]
                                    (assoc state :dragging? true :sel-start pos :sel-end pos
                                           :desired-col col :caret-visible true :focus :editor))))))

                          :mousemove
                          (if (:dragging? state)
                            (let [{:keys [x y]} value
                                  adj-y (+ y (:scroll-y state))
                                  ;; Use visual->logical mapping for selection
                                  visual-line (max 0 (Math/floor (/ (- adj-y layout-y) line-h)))
                                  logical-line (get @!line-mapping visual-line (min visual-line (dec (count @!line-lengths))))
                                  line-len (get @!line-lengths logical-line 0)
                                  char-w (* font-size 0.6)
                                  col (-> (/ (- x layout-x) char-w)
                                          (Math/round)
                                          (max 0)
                                          (min line-len))
                                  pos {:line logical-line :col col}]
                              (assoc state :sel-end pos))
                            state)

                          :mouseup
                          (assoc state :dragging? false)

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
                            ;; Command panel Enter - submit command
                            (let [cmd-text (:cmd-text state)]
                              (when (seq cmd-text)
                                (js/console.log "Command submitted:" cmd-text)
                                ;; TODO: Process the command here
                                )
                              ;; Clear and close panel
                              (assoc state
                                     :cmd-text ""
                                     :cmd-cursor 0
                                     :cmd-visible false
                                     :focus :editor))
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

                          :escape
                          (cond
                            ;; If command panel is open, close it
                            (:cmd-visible state)
                            (assoc state :cmd-visible false :focus :editor)
                            ;; Otherwise, clear selection
                            :else
                            (assoc state :sel-start nil :sel-end nil))

                          state))))
             nil))

      ;; 2a. EDITOR TEXT UPDATER (re-tokenize when lines or fold state changes)
      (->> (m/latest vector
                     (m/watch !lines)
                     (m/eduction (map :folded-lines) (m/watch !state)))
           (m/eduction (dedupe))
           (m/reduce
             (fn [_ [new-lines folded-lines]]
               (let [new-line-lengths (mapv count new-lines)
                     ;; Detect fold regions from parse tree
                     fold-regions (or (detect-folds-fn new-lines new-line-lengths) [])
                     _ (reset! !fold-regions fold-regions)
                     _ (reset! !line-lengths new-line-lengths)
                     tokenized-lines (mapv tokenize-fn new-lines)
                     ;; Layout with folding - returns {:render-ops :line-mapping}
                     layout-result (layout-fn tokenized-lines layout-x layout-y font-size
                                              fold-regions folded-lines)
                     render-ops (:render-ops layout-result)]
                 (reset! !line-mapping (:line-mapping layout-result))
                 ;; Cache editor render ops
                 (reset! !editor-render-ops render-ops))
               nil)
             nil))

      ;; 2b. COMBINED TEXT UPDATER (combines editor + command panel text)
      ;; Updates GPU when editor ops change OR when command panel state changes
      (->> (m/latest vector
                     (m/watch !editor-render-ops)
                     (m/eduction (map (fn [s] {:cmd-visible (:cmd-visible s)
                                                :cmd-text (:cmd-text s)
                                                :scroll-y (:scroll-y s)
                                                :height (:height s)}))
                                 (m/watch !state)))
           (m/eduction (dedupe))
           (m/reduce
             (fn [_ [editor-ops {:keys [cmd-visible cmd-text scroll-y height]}]]
               (let [;; Add command panel text if visible
                     ;; Position it at fixed screen position (accounting for scroll)
                     cmd-panel-y (+ scroll-y (- height cmd-panel-h))
                     cmd-text-y (+ cmd-panel-y 12 font-size)  ;; Center vertically

                     ;; Show placeholder or actual text
                     cmd-text-token (when cmd-visible
                                      (if (seq cmd-text)
                                        ;; Show actual text
                                        [{:text cmd-text
                                          :type :text
                                          :from 0
                                          :to (count cmd-text)
                                          :x 60
                                          :y cmd-text-y
                                          :size font-size
                                          :r 0.9 :g 0.9 :b 0.9 :a 1.0}]
                                        ;; Show placeholder
                                        [{:text "Type a task..."
                                          :type :comment  ;; Gray color
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

                     ;; Combine editor ops with command panel ops
                     render-ops (if cmd-visible
                                  (vec (concat editor-ops
                                               (when cmd-prompt-token [cmd-prompt-token])
                                               (when cmd-text-token [cmd-text-token])))
                                  editor-ops)]
                 (reset! !text-geo (editor/update-text-data device (:text geometry) render-ops atlas font-size)))
               nil)
             nil))

      ;; 3. SELECTION & CARET GEOMETRY UPDATER (with bracket matching + fold indicators + eval results + command panel)
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
                           :height (:height s)}))
             (dedupe))
           (m/reduce
             (fn [_ {:keys [sel-start sel-end caret-visible folded-lines eval-result
                            cmd-visible cmd-text cmd-cursor focus width height]}]
               (let [line-mapping @!line-mapping
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
                                                x (+ 50 (/ (- gutter-w indicator-size) 2))
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
                                                 {:x (+ layout-x (* col char-w))
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
                                             {:x (+ layout-x (* (:col sel-start) char-w))
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
                                                       {:x (+ layout-x (* col-start char-w))
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
                                             result-x (+ layout-x (* (+ line-len 2) char-w))
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

                     ;; Combine all rects
                     rects (vec (concat fold-rects
                                        (or bracket-rects [])
                                        (or selection-rects [])
                                        (if editor-caret-rect [editor-caret-rect] [])
                                        (if eval-rect [eval-rect] [])
                                        (if cmd-bg-rect [cmd-bg-rect] [])
                                        (if cmd-caret-rect [cmd-caret-rect] [])))]
                 (reset! !rect-sys (editor/update-rects device (:rect geometry) rects)))
               nil)
             nil))

      ;; 4. RENDER LOOP (just draws, no rect calculation)
      (m/reduce
        (fn [_ state]
          (editor/draw-frame! device ctx
                              @!text-geo
                              @!rect-sys
                              (:camera-floats (:pipelines geometry))
                              (:pass-descriptor (:pipelines geometry))
                              0 (- (:scroll-y state)) (:width state) (:height state))
          nil)
        nil
        (m/sample (fn [s _t] s) (m/watch !state) >raf)))))
