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

(defn >keyboard-events [node]
  (m/observe
    (fn [!]
      (let [handler (fn [e]
                      (let [key (.-key e)]
                        (cond
                          ;; Navigation keys
                          (contains? #{"ArrowLeft" "ArrowRight" "ArrowUp" "ArrowDown"
                                       "Home" "End"} key)
                          (do (.preventDefault e)
                              (! [:keydown key]))

                          ;; Printable characters (length 1, not control keys)
                          (and (= 1 (.-length key))
                               (not (.-ctrlKey e))
                               (not (.-metaKey e))
                               (not (.-altKey e)))
                          (do (.preventDefault e)
                              (! [:char-input key]))

                          ;; Enter key
                          (= key "Enter")
                          (do (.preventDefault e)
                              (! [:enter nil]))

                          ;; Backspace
                          (= key "Backspace")
                          (do (.preventDefault e)
                              (! [:backspace nil])))))]
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

(defn start-loop! [node device ctx geometry line-lengths lines tokenize-fn layout-fn find-bracket-fn detect-folds-fn atlas]
  (let [;; Layout Configuration (Must match Editor defaults)
        font-size 16
        gutter-w  40           ;; Width of gutter for fold indicators
        layout-x  (+ 50 gutter-w)  ;; Shift text right to make room for gutter
        layout-y  100
        line-h    (* font-size 1.2)

        initial-state {:scroll-y      0
                       :width         (.-innerWidth js/window)
                       :height        (.-innerHeight js/window)
                       :dpr           (or (.-devicePixelRatio js/window) 1)
                       :dragging?     false
                       :sel-start     nil
                       :sel-end       nil
                       :desired-col   0
                       :caret-visible true
                       :folded-lines  #{}}
        
        !state        (atom initial-state)
        !rect-sys     (atom (:rect geometry))
        !lines        (atom lines)
        !line-lengths (atom line-lengths)
        !text-geo     (atom (:text geometry))
        !fold-regions (atom [])
        !line-mapping (atom [])  ;; Maps visual line index -> logical line index
        
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
                                adj-y (+ y (:scroll-y state))]
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
                                  state))
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
                                       :desired-col col :caret-visible true))))

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
                          (if-let [pos (:sel-start state)]
                            (let [line-idx (:line pos)
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
                            state)

                          :backspace
                          (if-let [pos (:sel-start state)]
                            (let [line-idx (:line pos)
                                  col      (:col pos)]
                              (cond
                                ;; At start of line - join with previous line
                                (and (= col 0) (> line-idx 0))
                                (let [current-line (get @!lines line-idx "")
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
                                (let [current-line (get @!lines line-idx "")
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
                            state)

                          :enter
                          (if-let [pos (:sel-start state)]
                            (let [line-idx (:line pos)
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
                            state)

                          :keydown
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
                            state)

                          state))))
             nil))

      ;; 2. TEXT CONTENT UPDATER (re-tokenize and update GPU when lines OR fold state changes)
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
                 (reset! !text-geo (editor/update-text-data device (:text geometry) render-ops atlas font-size)))
               nil)
             nil))

      ;; 3. SELECTION & CARET GEOMETRY UPDATER (with bracket matching + fold indicators)
      (->> (m/watch !state)
           (m/eduction
             (map (fn [s] {:sel-start (:sel-start s)
                           :sel-end (:sel-end s)
                           :caret-visible (:caret-visible s)
                           :folded-lines (:folded-lines s)}))
             (dedupe))
           (m/reduce
             (fn [_ {:keys [sel-start sel-end caret-visible folded-lines]}]
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
                     ;; Calculate caret rect at visual position
                     caret-rect (when (and sel-start caret-visible (not has-selection?))
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
                     ;; Combine all rects
                     rects (vec (concat fold-rects
                                        (or bracket-rects [])
                                        (or selection-rects [])
                                        (if caret-rect [caret-rect] [])))]
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
