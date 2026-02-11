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
            [cljs.reader :as reader]
            [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [missionary.core :as m]
            [contrib.missionary-contrib :as mx]
            [app.client.webgpu.editor :as editor]
            [app.client.webgpu.text-input :as text-input]
            [app.client.webgpu.themes :as themes]))

;; ============================================================================
;; LAYER 1: PRIMARY SOURCES (The Only Atoms)
;; ============================================================================
;; These are created per-instance in start-loop! to support multiple editors

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
                         (! {:width  (.-clientWidth canvas-node)
                             :height (.-clientHeight canvas-node)
                             :dpr    (or js/window.devicePixelRatio 1)}))]
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

(defn snap-to-dpr [v dpr]
  (let [scale (or dpr 1)]
    (/ (Math/round (* v scale)) scale)))

(defn maybe-snap [v dpr snap?]
  (if snap? (snap-to-dpr v dpr) v))

(defn manifest-defaults->settings [manifest-settings]
  (let [get-default (fn [k fallback]
                      (or (get-in manifest-settings [k :default]) fallback))]
    {:font-size (get-default :fontSize 19)
     :line-height (get-default :lineHeight 1.2)
     :px-range (get-default :pxRange 8)
     :sharpness (get-default :sharpness 0.0)
     :snap-to-pixel? (get-default :snapToPixel true)
     :show-diagnostics? (get-default :showDiagnostics false)
     :theme-id (get-default :theme :gruvbox-dark)}))

(defn compact-map [m]
  (into {} (filter (comp some? val) m)))

(defn font-defaults->settings [font]
  (let [defaults (:defaults font)]
    (when defaults
      (compact-map
        {:font-size (or (:fontSize defaults) (:font-size defaults))
         :line-height (or (:lineHeight defaults) (:line-height defaults))
         :px-range (or (:pxRange defaults) (:px-range defaults))
         :sharpness (or (:sharpness defaults) (:sharpness defaults))
         :snap-to-pixel? (or (:snapToPixel defaults) (:snap-to-pixel? defaults))
         :show-diagnostics? (or (:showDiagnostics defaults) (:show-diagnostics? defaults))}))))

(defn slider-specs [settings]
  [{:id :theme-id :label "Theme" :val (themes/theme-index (:theme-id settings)) :min 0 :max (dec (count themes/theme-list)) :discrete true}
   {:id :font-size :label "Font Size" :val (:font-size settings) :min 8 :max 40}
   {:id :line-height :label "Line Height" :val (:line-height settings) :min 1.0 :max 2.0}
   {:id :px-range :label "pxRange" :val (:px-range settings) :min 4 :max 12}
   {:id :sharpness :label "Sharpness" :val (:sharpness settings) :min -0.2 :max 0.2}
   {:id :snap-to-pixel? :label "Snap" :val (if (:snap-to-pixel? settings) 1 0) :min 0 :max 1}
   {:id :show-diagnostics? :label "Diagnostics" :val (if (:show-diagnostics? settings) 1 0) :min 0 :max 1}])

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

;; NOTE: <line-lengths, <fold-regions, <line-mapping were removed (dead code using banned m/ap+m/?< pattern).
;; Their functionality is now consolidated in <fold-state (line 633) which uses m/latest.

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

(defn <settings-panel-keys
  "Flow of keyboard events routed to settings panel (when focused).
   Uses deref instead of m/watch to avoid cancellation on focus change."
  [>keyboard !focus]
  (->> >keyboard
       (m/eduction (filter (fn [event]
                             (and (= @!focus :settings-panel)
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

(defn parse-agent-command
  "Parse command-panel text into an agent action.
   Supported forms:
   - /provider claude|codex|gemini
   - /run <raw argv...>
   - plain text prompt"
  [cmd-text current-provider]
  (let [trimmed (str/trim (or cmd-text ""))]
    (cond
      (str/blank? trimmed)
      {:kind :noop}

      (str/starts-with? trimmed "/provider ")
      (let [arg (-> trimmed
                    (subs (count "/provider "))
                    str/trim
                    str/lower-case
                    keyword)]
        (if (contains? #{:claude :codex :gemini} arg)
          {:kind :set-provider :provider arg}
          {:kind :error :message (str "Unknown provider: " arg)}))

      (str/starts-with? trimmed "/run ")
      (let [argv (-> trimmed
                     (subs (count "/run "))
                     str/trim
                     (str/split #"\s+")
                     vec)
            first-bin (some-> (first argv) str/lower-case)
            provider (case first-bin
                       "claude" :claude
                       "codex" :codex
                       "gemini" :gemini
                       current-provider)]
        (if (seq argv)
          {:kind :run :provider provider :argv argv :prompt (str/join " " argv)}
          {:kind :error :message "Missing argv for /run"}))

      :else
      {:kind :run :provider current-provider :prompt trimmed})))

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

(defn build-line-mapping
  "Build visual->logical mapping based on fold regions."
  [lines regions folded]
  (let [num-lines (count lines)]
    (loop [logical-idx 0 mapping []]
      (if (>= logical-idx num-lines)
        mapping
        (let [visible? (not (some (fn [{:keys [start-line end-line]}]
                                    (and (contains? folded start-line)
                                         (> logical-idx start-line)
                                         (<= logical-idx end-line)))
                                  regions))]
          (if visible?
            (recur (inc logical-idx) (conj mapping logical-idx))
            (recur (inc logical-idx) mapping)))))))

(defn compute-fold-state
  "Compute fold regions + line mapping once per doc/fold change.
   Safe for large files because this only runs on document changes (cached in <fold-state),
   NOT on every blink tick."
  [doc folded detect-folds-fn]
  (let [lines (:lines doc)
        lengths (mapv count lines)
        regions (or (detect-folds-fn lines lengths) [])
        line-mapping (build-line-mapping lines regions folded)
        logical->visual (calculate-logical->visual line-mapping)]
    {:lines lines
     :lengths lengths
     :regions regions
     :folded folded
     :line-mapping line-mapping
     :logical->visual logical->visual}))

(defn <fold-state
  "Derived flow: fold regions + line mapping (cached between blinks)."
  [!editor-doc !folded-lines detect-folds-fn]
  (m/latest
    (fn [doc folded]
      (compute-fold-state doc folded detect-folds-fn))
    (m/watch !editor-doc)
    (m/watch !folded-lines)))

(defn <bracket-match
  "Derived flow: cached bracket matching (recomputes on doc change, NOT on blink).
   Safe for all file sizes because this only runs on document changes."
  [!editor-doc find-bracket-fn]
  (m/latest
    (fn [doc]
      (let [lines (:lines doc)
            cursor (:cursor doc)
            selection (:selection doc)]
        (when (and cursor (not selection))
          (let [lengths (mapv count lines)]
            (find-bracket-fn cursor lines lengths)))))
    (m/watch !editor-doc)))

(defn compute-editor-rects
  "Pure function: compute all editor rectangles from PRE-COMPUTED fold state and bracket match.
   No longer calls detect-folds-fn or find-bracket-fn directly — those are cached in separate flows."
  [doc fold-state bracket-match eval-result caret-visible focus
   layout-x layout-y line-h gutter-w char-advance]
  (let [cursor (:cursor doc)
        selection (:selection doc)

        ;; Use pre-computed fold state (cached, only changes on doc/fold change)
        {:keys [lengths regions folded line-mapping logical->visual]} fold-state

        ;; Helper to get visual y for logical line
        logical->visual-y (fn [logical-line]
                            (when-let [visual-idx (get logical->visual logical-line)]
                              (+ layout-y (* visual-idx line-h))))

        ;; Use the passed char advance (reactive based on active font)
        char-w char-advance
        gutter-x (- layout-x gutter-w)

        ;; Fold indicator rects
        fold-rects (keep (fn [{:keys [start-line]}]
                          (when-let [visual-y (logical->visual-y start-line)]
                            (let [is-folded? (contains? folded start-line)
                                  indicator-size 10
                                  x (+ gutter-x (/ (- gutter-w indicator-size) 2))
                                  y (+ visual-y (/ (- line-h indicator-size) 2))]
                              {:x x :y y :w indicator-size :h indicator-size
                               :r (if is-folded? 0.3 0.7)
                               :g (if is-folded? 0.5 0.6)
                               :b (if is-folded? 0.9 0.3)
                               :a 0.8})))
                        regions)

        ;; Bracket match rects (pre-computed, cached in <bracket-match flow)
        bracket-rects (when bracket-match
                        (keep (fn [{:keys [line col]}]
                                (when-let [visual-y (logical->visual-y line)]
                                  {:x (+ layout-x (* col char-w))
                                   :y visual-y
                                   :w char-w
                                   :h line-h
                                   :r 0.8 :g 0.6 :b 0.2 :a 0.4}))
                              [(:open bracket-match) (:close bracket-match)]))

        ;; Caret rect (only when editor is focused and no selection)
        caret-rect (when (and cursor caret-visible (= focus :editor) (not selection))
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
   Uses m/latest instead of m/ap to avoid cancellation propagation issues.
   REACTIVE: font-size, line-h, char-advance come from !settings and !active-font.
   OPTIMIZED: fold-state and bracket-match are pre-computed in cached flows
   that only recompute when the document changes — NOT on every blink tick."
  [!editor-doc !eval-result !caret-visible !focus !settings !active-font !viewport
   <fold-data <bracket-data
   layout-x layout-y gutter-w]
  (m/latest
    (fn [doc fold-state bracket-match eval-result caret-visible focus settings active-font viewport]
      (let [dpr (:dpr viewport)
            snap? (:snap-to-pixel? settings)
            font-size (:font-size settings)
            line-h (maybe-snap (* font-size (:line-height settings)) dpr snap?)
            char-advance (maybe-snap (* font-size (:char-width active-font)) dpr snap?)
            layout-x (maybe-snap layout-x dpr snap?)
            layout-y (maybe-snap layout-y dpr snap?)]
        (compute-editor-rects doc fold-state bracket-match eval-result caret-visible focus
                              layout-x layout-y line-h gutter-w char-advance)))
    (m/watch !editor-doc)
    <fold-data
    <bracket-data
    (m/watch !eval-result)
    (m/watch !caret-visible)
    (m/watch !focus)
    (m/watch !settings)
    (m/watch !active-font)
    (m/watch !viewport)))

(defn <cmd-panel-rects
  "Derived flow: command panel rectangles (background + caret)
   Uses m/latest instead of m/ap to avoid cancellation propagation.
   REACTIVE: font-size and char-advance come from !settings and !active-font."
  [!cmd-panel !focus !caret-visible !scroll-y !viewport !settings !active-font cmd-panel-h]
  (m/latest
    (fn [panel focus caret-visible scroll-y viewport settings active-font]
      (when (:visible panel)
        (let [dpr (:dpr viewport)
              snap? (:snap-to-pixel? settings)
              font-size (:font-size settings)
              char-advance (maybe-snap (* font-size (:char-width active-font)) dpr snap?)
              panel-y (maybe-snap (+ scroll-y (- (:height viewport) cmd-panel-h)) dpr snap?)
              text-x (maybe-snap 60 dpr snap?)
              char-w char-advance

              ;; Background rect
              bg-rect {:x 0 :y panel-y :w (:width viewport) :h cmd-panel-h
                       :r 0.15 :g 0.15 :b 0.2 :a 1.0}

              ;; Caret rect (only when focused and visible)
              caret-rect (when (and caret-visible (= focus :command-panel))
                           {:x (+ text-x (* (:cursor panel) char-w))
                            :y (maybe-snap (+ panel-y 8) dpr snap?)
                            :w 2
                            :h (maybe-snap (- cmd-panel-h 16) dpr snap?)
                            :r 0.9 :g 0.9 :b 0.9 :a 1.0})]

          (if caret-rect
            [bg-rect caret-rect]
            [bg-rect]))))
    (m/watch !cmd-panel)
    (m/watch !focus)
    (m/watch !caret-visible)
    (m/watch !scroll-y)
    (m/watch !viewport)
    (m/watch !settings)
    (m/watch !active-font)))

(defn compute-settings-panel-rects
  "Pure function: compute settings panel rectangles (background + font list + sliders)"
  [settings focus viewport scroll-y font-manifest font-size]
  (when (:visible settings)
    (let [;; Panel dimensions - centered modal
          panel-w 600
          panel-h 480
          panel-x (/ (- (:width viewport) panel-w) 2)
          panel-y (+ scroll-y (/ (- (:height viewport) panel-h) 2))

          ;; Colors (Modern Dark Theme)
          bg-color       {:r 0.12 :g 0.12 :b 0.14 :a 0.98} ;; Deep dark grey
          border-color   {:r 0.25 :g 0.25 :b 0.28 :a 1.0}
          separator-color {:r 0.20 :g 0.20 :b 0.23 :a 1.0}
          
          item-hover     {:r 0.18 :g 0.18 :b 0.22 :a 1.0}
          item-selected  {:r 0.22 :g 0.22 :b 0.26 :a 1.0}
          item-active    {:r 0.15 :g 0.25 :b 0.40 :a 0.8}  ;; Blue-ish highlight for active focus

          slider-track   {:r 0.20 :g 0.20 :b 0.24 :a 1.0}
          slider-fill    {:r 0.40 :g 0.60 :b 0.85 :a 1.0}  ;; Accent Blue
          slider-thumb   {:r 0.90 :g 0.90 :b 0.95 :a 1.0}

          ;; State
          current-focus (or (:focus-section settings) :fonts) ;; :fonts or :sliders
          font-idx      (or (:selected-index settings) 0)
          slider-idx    (or (:slider-index settings) 0)

          ;; Layout
          left-w 220
          right-w (- panel-w left-w)
          
          ;; Left Pane (Fonts)
          left-pane-x panel-x
          left-pane-y panel-y
          
          ;; Right Pane (Sliders)
          right-pane-x (+ panel-x left-w)
          right-pane-y panel-y
          
          ;; Header
          header-h 40
          content-y (+ panel-y header-h)

          ;; Font List
          fonts (or (:fonts font-manifest)
                    [{:name "DejaVu Sans Mono" :id "dejavu-sans-mono"}])
          available-fonts (filter #(not (false? (:available %))) fonts)
          font-item-h 32
          
          font-rects (map-indexed
                       (fn [idx font]
                         (let [selected? (= idx font-idx)
                               focused?  (= current-focus :fonts)
                               item-y (+ content-y 10 (* idx font-item-h))
                               
                               bg (cond
                                    (and selected? focused?) item-active
                                    selected?                item-selected
                                    :else                    nil)]
                           (when bg
                             {:x (+ left-pane-x 8)
                              :y item-y
                              :w (- left-w 16)
                              :h (- font-item-h 4)
                              :r (:r bg) :g (:g bg) :b (:b bg) :a (:a bg)})))
                       available-fonts)

          ;; Slider List
          sliders (slider-specs settings)
          
          slider-item-h 70
          slider-rects (map-indexed
                         (fn [idx slider]
                           (let [selected? (= idx slider-idx)
                                 focused?  (= current-focus :sliders)
                                 base-y (+ content-y 10 (* idx slider-item-h))
                                 
                                 ;; Calculate ratio
                                 range (- (:max slider) (:min slider))
                                 ratio (/ (- (:val slider) (:min slider)) range)
                                 
                                 ;; Background highlight
                                 bg (cond
                                      (and selected? focused?) item-active
                                      selected?                item-selected
                                      :else                    nil)
                                      
                                 ;; Track geometry
                                 track-x (+ right-pane-x 20)
                                 track-y (+ base-y 40)
                                 track-w (- right-w 40)
                                 track-h 4
                                 fill-w (* track-w ratio)]
                             
                             (concat
                               ;; Item Background
                               (when bg
                                 [{:x (+ right-pane-x 8) :y base-y :w (- right-w 16) :h (- slider-item-h 8)
                                   :r (:r bg) :g (:g bg) :b (:b bg) :a (:a bg)}])
                               
                               ;; Track Background
                               [{:x track-x :y track-y :w track-w :h track-h
                                 :r (:r slider-track) :g (:g slider-track) :b (:b slider-track) :a (:a slider-track)}]
                               
                               ;; Filled Track
                               [{:x track-x :y track-y :w fill-w :h track-h
                                 :r (:r slider-fill) :g (:g slider-fill) :b (:b slider-fill) :a (:a slider-fill)}]
                               
                               ;; Thumb/Knob
                               [{:x (+ track-x fill-w -3) :y (- track-y 5) :w 6 :h 14
                                 :r (:r slider-thumb) :g (:g slider-thumb) :b (:b slider-thumb) :a (:a slider-thumb)}])))
                         sliders)]

      (vec
        (concat
          ;; Main Background
          [{:x panel-x :y panel-y :w panel-w :h panel-h
            :r (:r bg-color) :g (:g bg-color) :b (:b bg-color) :a (:a bg-color)}]
            
          ;; Header Separator
          [{:x panel-x :y (+ panel-y header-h) :w panel-w :h 1
            :r (:r separator-color) :g (:g separator-color) :b (:b separator-color) :a (:a separator-color)}]
            
          ;; Vertical Separator
          [{:x (+ panel-x left-w) :y (+ panel-y header-h) :w 1 :h (- panel-h header-h)
            :r (:r separator-color) :g (:g separator-color) :b (:b separator-color) :a (:a separator-color)}]

          ;; Content
          (filter some? font-rects)
          (mapcat identity slider-rects))))))

(defn <settings-panel-rects
  "Derived flow: settings panel rectangles.
   REACTIVE: font-size comes from !settings."
  [!settings !focus !viewport !scroll-y !font-manifest]
  (m/latest
    (fn [settings focus viewport scroll-y font-manifest]
      (let [font-size (:font-size settings)]
        (compute-settings-panel-rects settings focus viewport scroll-y font-manifest font-size)))
    (m/watch !settings)
    (m/watch !focus)
    (m/watch !viewport)
    (m/watch !scroll-y)
    (m/watch !font-manifest)))

(defn compute-settings-panel-text
  "Pure function: compute settings panel text elements"
  [settings viewport scroll-y font-manifest font-size]
  (when (:visible settings)
    (let [panel-w 600
          panel-h 480
          panel-x (/ (- (:width viewport) panel-w) 2)
          panel-y (+ scroll-y (/ (- (:height viewport) panel-h) 2))

          ;; State
          current-focus (or (:focus-section settings) :fonts)
          font-idx      (or (:selected-index settings) 0)
          slider-idx    (or (:slider-index settings) 0)

          ;; Layout
          left-w 220
          right-w (- panel-w left-w)
          right-pane-x (+ panel-x left-w)
          
          header-h 40
          content-y (+ panel-y header-h)

          ;; Font List
          fonts (or (:fonts font-manifest)
                    [{:name "DejaVu Sans Mono" :id "dejavu-sans-mono"}])
          available-fonts (filter #(not (false? (:available %))) fonts)
          font-item-h 32
          
          font-texts (map-indexed
                       (fn [idx font]
                         (let [selected? (= idx font-idx)
                               focused?  (= current-focus :fonts)
                               item-y (+ content-y 10 (* idx font-item-h))
                               text-y (+ item-y (/ font-item-h 2) (/ font-size 3)) ;; Approx center
                               
                               color (cond
                                       (and selected? focused?) {:r 1.0 :g 1.0 :b 1.0 :a 1.0}
                                       selected?                {:r 0.9 :g 0.9 :b 0.9 :a 1.0}
                                       :else                    {:r 0.6 :g 0.6 :b 0.6 :a 1.0})]
                           {:text (:name font)
                            :type :text
                            :from 0 :to (count (:name font))
                            :x (+ panel-x 20)
                            :y text-y
                            :size font-size
                            :r (:r color) :g (:g color) :b (:b color) :a (:a color)}))
                       available-fonts)

          ;; Slider Labels
          sliders (slider-specs settings)
          slider-item-h 70
          
          slider-texts (mapcat
                         (fn [[idx slider]]
                           (let [selected? (= idx slider-idx)
                                 focused?  (= current-focus :sliders)
                                 base-y (+ content-y 10 (* idx slider-item-h))
                                 
                                 label-color (if (and selected? focused?)
                                               {:r 1.0 :g 1.0 :b 1.0 :a 1.0}
                                               {:r 0.8 :g 0.8 :b 0.8 :a 1.0})
                                 val-color   (if (and selected? focused?)
                                               {:r 0.4 :g 0.7 :b 1.0 :a 1.0}
                                               {:r 0.5 :g 0.5 :b 0.5 :a 1.0})
                                 
                                 val-str (case (:id slider)
                                           :theme-id (let [tid (or (:theme-id settings) :gruvbox-dark)
                                                           theme (themes/get-theme tid)]
                                                       (or (:name theme) (name tid)))
                                           :line-height (.toFixed (:val slider) 1)
                                           :sharpness (.toFixed (:val slider) 2)
                                           :snap-to-pixel? (if (pos? (:val slider)) "On" "Off")
                                           :show-diagnostics? (if (pos? (:val slider)) "On" "Off")
                                           (str (:val slider)))]
                             
                             [{:text (:label slider)
                               :type :text
                               :from 0 :to (count (:label slider))
                               :x (+ right-pane-x 20)
                               :y (+ base-y 25)
                               :size font-size
                               :r (:r label-color) :g (:g label-color) :b (:b label-color) :a (:a label-color)}
                              
                              {:text val-str
                               :type :number
                               :from 0 :to (count val-str)
                               :x (+ right-pane-x right-w -20 -10) ;; Right align approx
                               :y (+ base-y 25)
                               :size font-size
                               :r (:r val-color) :g (:g val-color) :b (:b val-color) :a (:a val-color)}]))
                         (map-indexed vector sliders))
          
          ;; Headers
          title-text {:text "Settings"
                      :type :macro
                      :from 0 :to 8
                      :x (+ panel-x 20)
                      :y (+ panel-y 26)
                      :size (+ font-size 2)
                      :r 0.9 :g 0.9 :b 0.9 :a 1.0}
                      
          hint-text {:text "Tab: Switch Pane   Arrows: Navigate/Adjust"
                     :type :comment
                     :from 0 :to 38
                     :x (+ panel-x 20)
                     :y (+ panel-y panel-h -12)
                     :size (- font-size 2)
                     :r 0.5 :g 0.5 :b 0.5 :a 0.8}]

      (vec
        (concat
          [title-text]
          font-texts
          slider-texts
          [hint-text])))))

(defn <settings-panel-text
  "Derived flow: settings panel text elements.
   REACTIVE: font-size comes from !settings."
  [!settings !viewport !scroll-y !font-manifest]
  (m/latest
    (fn [settings viewport scroll-y font-manifest]
      (let [font-size (:font-size settings)]
        (compute-settings-panel-text settings viewport scroll-y font-manifest font-size)))
    (m/watch !settings)
    (m/watch !viewport)
    (m/watch !scroll-y)
    (m/watch !font-manifest)))

(defn <combined-text-ops
  "Derived flow: combined text render ops (editor + command panel)
   Uses m/latest instead of m/ap to avoid cancellation propagation.
   REACTIVE: font-size comes from !settings, updates live."
  [!editor-doc !cmd-panel !ai-provider !agent-output !scroll-y !viewport !folded-lines !settings !active-font
   tokenize-fn layout-fn detect-folds-fn
   layout-x layout-y cmd-panel-h]
  (m/latest
    (fn [doc panel provider agent-output scroll-y viewport folded settings active-font]
      (let [dpr (:dpr viewport)
              snap? (:snap-to-pixel? settings)
              ;; Reactive font settings
              font-size (:font-size settings)
              char-width (:char-width active-font)
              char-advance (maybe-snap (* font-size char-width) dpr snap?)
              line-h (maybe-snap (* font-size (:line-height settings)) dpr snap?)
              layout-x (maybe-snap layout-x dpr snap?)
              layout-y (maybe-snap layout-y dpr snap?)
              ;; Theme
              theme-id (or (:theme-id settings) :gruvbox-dark)

              ;; Editor render ops with VIEWPORT-SCOPED processing
              ;; For large files (>500 lines): only tokenize, detect folds, and layout
              ;; the visible ~50 lines. Avoids 1.4s main-thread block.
              ;; For small files: process everything (original behavior with fold support).
              lines (:lines doc)
              total-line-count (count lines)
              large-file? (> total-line-count 500)

              ;; Visible line range (with buffer above/below)
              visible-start (max 0 (- (int (/ scroll-y line-h)) 5))
              visible-end (min total-line-count (+ (int (/ (+ scroll-y (:height viewport)) line-h)) 5))

              ;; Tokenize only visible lines
              visible-lines (subvec lines visible-start visible-end)
              tokenized-visible (mapv tokenize-fn visible-lines)

              ;; Fold detection + Layout: fast path for large files
              [editor-ops final-line-mapping]
              (if large-file?
                ;; FAST PATH: skip fold detection entirely (full Lezer re-parse too expensive)
                ;; Layout only visible lines with adjusted Y offset
                (let [adjusted-y (+ layout-y (* visible-start line-h))
                      result (layout-fn tokenized-visible layout-x adjusted-y font-size
                                        [] #{} char-advance line-h theme-id)
                      ;; Identity line-mapping: visual line N = logical line N (no folds)
                      full-mapping (vec (range total-line-count))]
                  [(:render-ops result) full-mapping])

                ;; NORMAL PATH (<500 lines): full fold support
                (let [regions (or (detect-folds-fn lines (mapv count lines)) [])
                      ;; Build sparse tokenized vector for full layout
                      tokenized-all (into []
                                      (map-indexed
                                        (fn [idx _]
                                          (if (and (>= idx visible-start) (< idx visible-end))
                                            (nth tokenized-visible (- idx visible-start))
                                            [])))
                                      lines)
                      result (layout-fn tokenized-all layout-x layout-y font-size
                                        regions folded char-advance line-h theme-id)]
                  [(filterv seq (:render-ops result))
                   (:line-mapping result)]))

              ;; Command panel ops (if visible)
              cmd-ops (when (:visible panel)
                        (let [cmd-panel-y (maybe-snap (+ scroll-y (- (:height viewport) cmd-panel-h)) dpr snap?)
                              cmd-text-y (maybe-snap (+ cmd-panel-y 12 font-size) dpr snap?)
                              prompt-text (str "[" (-> (or provider :claude) name str/upper-case) "]> ")
                              prompt-x (maybe-snap 24 dpr snap?)
                              text-x (maybe-snap (+ prompt-x (* (count prompt-text) char-advance)) dpr snap?)]
                          [(when (seq (:text panel))
                             [{:text (:text panel)
                               :type :text
                               :from 0 :to (count (:text panel))
                               :x text-x :y cmd-text-y
                               :size font-size
                               :r 0.9 :g 0.9 :b 0.9 :a 1.0}])
                           [{:text prompt-text
                             :type :macro
                             :from 0 :to (count prompt-text)
                             :x prompt-x :y cmd-text-y
                             :size font-size
                             :r 0.3 :g 0.6 :b 1.0 :a 1.0}]
                           (when (empty? (:text panel))
                             [{:text "Type a task... (/provider codex | /run codex exec ...)"
                               :type :comment
                               :from 0 :to 56
                               :x text-x :y cmd-text-y
                               :size font-size
                               :r 0.5 :g 0.5 :b 0.5 :a 0.7}])]))
              cmd-lines (if (:visible panel) (vec (filter some? cmd-ops)) [])]

          (let [status (:status agent-output)
                provider-name (some-> (:provider agent-output) name str/upper-case)
                prompt (:prompt agent-output)
                result-output (or (:output agent-output) "")
                status-color (case status
                               :complete {:r 0.55 :g 0.9 :b 0.55 :a 1.0}
                               :failed {:r 0.95 :g 0.45 :b 0.45 :a 1.0}
                               :timeout {:r 0.95 :g 0.75 :b 0.35 :a 1.0}
                               :running {:r 0.6 :g 0.8 :b 1.0 :a 1.0}
                               :submitting {:r 0.6 :g 0.8 :b 1.0 :a 1.0}
                               {:r 0.75 :g 0.75 :b 0.75 :a 1.0})
                header-text (cond
                              (nil? status) nil
                              (= status :running) (str "[" provider-name "] running: " prompt)
                              (= status :submitting) (str "[" provider-name "] submitting: " prompt)
                              (= status :failed) (str "[" provider-name "] failed: " prompt)
                              (= status :timeout) (str "[" provider-name "] timeout: " prompt)
                              (= status :complete) (str "[" provider-name "] complete: " prompt)
                              :else (str "[" provider-name "] " (name status) ": " prompt))
                max-output-lines 100
                output-lines (->> (str/split-lines result-output)
                                  (take max-output-lines))
                lines (cond-> []
                        header-text (conj header-text)
                        (and (= status :running) (empty? output-lines)) (conj "...")
                        (seq output-lines) (into output-lines))
                agent-panel-h 180
                agent-x (maybe-snap 60 dpr snap?)
                agent-y0 (maybe-snap (+ scroll-y (- (:height viewport) cmd-panel-h agent-panel-h 12)) dpr snap?)
                line-step (maybe-snap (* font-size 1.2) dpr snap?)
                agent-lines (mapv (fn [idx line]
                                    [{:text line
                                      :type :comment
                                      :from 0 :to (count line)
                                      :x agent-x
                                      :y (+ agent-y0 8 font-size (* idx line-step))
                                      :size font-size
                                      :r (:r status-color)
                                      :g (:g status-color)
                                      :b (:b status-color)
                                      :a (:a status-color)}])
                                  (range (count lines))
                                  lines)]

            {:render-ops (vec (concat editor-ops cmd-lines agent-lines))
             :line-mapping final-line-mapping
             :editor-line-count (count editor-ops)
             :cmd-line-count (count cmd-lines)})))
    (m/watch !editor-doc)
    (m/watch !cmd-panel)
    (m/watch !ai-provider)
    (m/watch !agent-output)
    (m/watch !scroll-y)
    (m/watch !viewport)
    (m/watch !folded-lines)
    (m/watch !settings)
    (m/watch !active-font)))

;; ============================================================================
;; LAYER 7: TERMINAL RENDER CONSUMER
;; ============================================================================

(defn task-from-promise
  "Convert a JavaScript Promise to a Missionary task.
   Missionary tasks are functions that take success/failure callbacks."
  [p]
  (fn [success failure]
    (-> p
        (.then success)
        (.catch failure))
    ;; Return cancellation function (no-op for promises - they can't be cancelled)
    #()))

(defn load-font-assets [font-config]
  (let [base-path "/fonts/"
        atlas-url (str base-path (:atlas font-config))
        metrics-url (str base-path (:metrics font-config))]
    (-> (js/Promise.all
          #js [(-> (js/fetch atlas-url) (.then #(.blob %)) (.then #(js/createImageBitmap %)))
               (-> (js/fetch metrics-url) (.then #(.json %)) (.then #(js->clj % :keywordize-keys true)))])
        (.then (fn [assets]
                 {:bitmap (aget assets 0)
                  :atlas (aget assets 1)
                  :id (:id font-config)})))))

(defn start-loop!
  "Start the reactive editor loop.

   This is the main entry point. It creates the reactive flow graph and
   returns a Missionary task that runs the render loop."
  [node device ctx geometry initial-line-lengths initial-lines
   tokenize-fn layout-fn find-bracket-fn detect-folds-fn
   find-form-fn eval-form-fn atlas & {:keys [font-manifest !sidebar-visible !file-load-request initial-file]}]

  (let [;; Layout Configuration
        font-size 19
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

        !viewport (atom {:width  (.-clientWidth node)
                         :height (.-clientHeight node)
                         :dpr    (or (.-devicePixelRatio js/window) 1)})

        !folded-lines (atom #{})

        ;; Additional state atoms (not primary sources, but needed for features)
        !caret-visible (atom true)
        !clipboard (atom nil)
        !undo-stack (atom [])
        !redo-stack (atom [])
        !eval-result (atom nil)
        !dragging? (atom false)

        ;; Font configuration - use passed manifest or default
        default-manifest {:fonts [{:name "DejaVu Sans Mono"
                                   :id "dejavu-sans-mono"
                                   :charWidth 0.60
                                   :default true
                                   :defaults {:fontSize 19
                                              :lineHeight 1.2
                                              :pxRange 8
                                              :sharpness 0.0
                                              :snapToPixel true
                                              :showDiagnostics false}}]
                          :settings {:fontSize {:default 19}
                                     :lineHeight {:default 1.2}
                                     :pxRange {:default 8}
                                     :sharpness {:default 0.0}
                                     :snapToPixel {:default true}
                                     :showDiagnostics {:default false}}}
        manifest (or font-manifest default-manifest)
        fonts (or (:fonts manifest) [])
        available-fonts (filterv #(not (false? (:available %))) fonts)
        default-font (or (first (filter :default available-fonts))
                         (first available-fonts)
                         {:id "dejavu-sans-mono" :name "DejaVu Sans Mono" :charWidth 0.60})
        default-font-idx (or (first (keep-indexed (fn [idx font]
                                                    (when (= (:id font) (:id default-font)) idx))
                                                  available-fonts))
                             0)
        manifest-settings (manifest-defaults->settings (:settings manifest))
        base-settings {:visible false
                       :font-id (:id default-font)
                       :selected-index default-font-idx
                       :slider-index 0
                       :focus-section :fonts}
        initial-settings (merge base-settings
                                manifest-settings
                                (font-defaults->settings default-font))

        ;; Settings panel state
        !settings (atom initial-settings) ;; :fonts or :sliders

        !font-manifest (atom manifest)
        !active-font (atom {:id (:id default-font)
                            :char-width (or (:charWidth default-font) 0.56)
                            :name (:name default-font)})

        ;; Font assets atom - stores loaded atlas/bitmap for current font
        ;; Initial value uses the atlas passed to start-loop!
        !font-assets (atom {:atlas atlas :bitmap nil :id "dejavu-sans-mono"})

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
        !settings-rect-sys (atom (let [capacity 32
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

        apply-font-defaults! (fn [font]
                               (swap! !settings assoc :font-id (:id font))
                               (when-let [defaults (font-defaults->settings font)]
                                 (swap! !settings merge defaults)))

        ;; Watch for font changes - triggers async loading
        ;; Uses atom + watch pattern instead of m/ap to avoid cancellation issues
        _ (add-watch !active-font :font-loader
            (fn [_ _ old-val new-val]
              (when (not= (:id old-val) (:id new-val))
                (let [manifest @!font-manifest
                      font-config (first (filter #(= (:id %) (:id new-val)) (:fonts manifest)))]
                  (js/console.log "[FONT] Loading font:" (:id new-val) font-config)
                  (when font-config
                    (apply-font-defaults! font-config))
                  (when (and font-config (:atlas font-config))
                    ;; Async load - updates !font-assets when complete
                    (-> (load-font-assets font-config)
                        (.then (fn [assets]
                                 (js/console.log "[FONT] Loaded assets for:" (:id new-val))
                                 (reset! !font-assets assets)))
                        (.catch (fn [err]
                                  (js/console.error "[FONT] Failed to load:" err)))))))))

        ;; =====================================================================
        ;; SIDEBAR STATE & DOM RENDERING (imperative, avoids Electric DAG)
        ;; =====================================================================
        !selected-project (atom nil)    ;; {:name :path} or nil
        !expanded-dirs (atom #{})       ;; set of expanded dir paths
        !dir-cache (atom {})            ;; {path -> [entries]}
        !home-dirs (atom nil)           ;; cached home dirs list
        !current-file (atom nil)        ;; {:path "..." :name "..."} or nil
        !ai-provider (atom :claude)     ;; :claude | :codex | :gemini
        !agent-output (atom nil)        ;; {:status :provider :prompt :output :run-id}

        sidebar-el (js/document.getElementById "file-sidebar")

        ;; --- Fetch helpers (call server HTTP API, parse EDN response) ---

        fetch-edn!
        (fn
          ([url callback]
           (-> (js/fetch url)
               (.then (fn [resp] (.text resp)))
               (.then (fn [text] (callback (reader/read-string text))))
               (.catch (fn [err] (js/console.error "[SIDEBAR] Fetch error:" err)))))
          ([url callback err-callback]
           (-> (js/fetch url)
               (.then (fn [resp] (.text resp)))
               (.then (fn [text] (callback (reader/read-string text))))
               (.catch (fn [err] (err-callback err))))))

        post-edn!
        (fn [url body callback]
          (-> (js/fetch url
                        (clj->js {:method "POST"
                                  :headers {"Content-Type" "application/edn"}
                                  :body (pr-str body)}))
              (.then (fn [resp] (.text resp)))
              (.then (fn [text] (callback (reader/read-string text))))
              (.catch (fn [err]
                        (js/console.error "[AGENT][HTTP][POST-ERROR]"
                                          (clj->js {:url url
                                                    :message (.-message err)})
                                          err))))

          nil)

        fetch-home-dirs!
        (fn [render-fn]
          (if @!home-dirs
            (render-fn)
            (fetch-edn! "/api/home-dirs"
                        (fn [dirs]
                          (reset! !home-dirs dirs)
                          (render-fn)))))

        fetch-dir!
        (fn [path render-fn]
          (if (contains? @!dir-cache path)
            (render-fn)
            (fetch-edn! (str "/api/list-dir?path=" (js/encodeURIComponent path))
                        (fn [entries]
                          (swap! !dir-cache assoc path entries)
                          (render-fn)))))

        fetch-file!
        (fn [path root-path]
          (fetch-edn! (str "/api/read-file?path=" (js/encodeURIComponent path)
                           "&root=" (js/encodeURIComponent root-path))
                      (fn [result]
                        (if (:error result)
                          (js/console.error "[SIDEBAR] File read error:" (:error result))
                          (let [lines (str/split-lines (:content result))]
                            (reset! !current-file {:path path :name (last (str/split path #"/"))})
                            (reset! !file-load-request {:lines lines}))))))

        submit-agent-run!
        (fn [cmd-text]
          (let [doc @!editor-doc
                file-path (:path @!current-file)
                scroll-y @!scroll-y
                viewport @!viewport
                parsed (parse-agent-command cmd-text @!ai-provider)
                cwd (or (some-> file-path (str/split #"/") butlast seq (str/join "/"))
                        ".")
                context {:cursor (:cursor doc)
                         :selection (:selection doc)
                         :visible-range [scroll-y (+ scroll-y (:height viewport))]
                         :file-path file-path
                         :timestamp (js/Date.now)}]
            (case (:kind parsed)
              :noop
              nil

              :set-provider
              (do
                (reset! !ai-provider (:provider parsed))
                (reset! !agent-output {:status :complete
                                       :provider (:provider parsed)
                                       :prompt "provider"
                                       :output (str "Provider set to " (-> (:provider parsed) name str/upper-case))
                                       :run-id nil}))

              :error
              (reset! !agent-output {:status :failed
                                     :provider @!ai-provider
                                     :prompt cmd-text
                                     :output (:message parsed)
                                     :run-id nil})

              :run
              (let [provider (:provider parsed)
                    prompt (:prompt parsed)
                    argv (:argv parsed)
                    run-id (str (random-uuid))
                    request-body (cond-> {:run-id run-id
                                          :provider provider
                                          :prompt prompt
                                          :cwd cwd
                                          :file file-path
                                          :context context}
                                   (seq argv) (assoc :argv argv))]
                (js/console.log "[AGENT][CLIENT][SUBMIT]"
                                (clj->js {:run-id run-id
                                          :provider provider
                                          :file file-path
                                          :cwd cwd
                                          :prompt prompt
                                          :argv argv}))
                ;; Show running state immediately
                (reset! !agent-output {:status :running
                                       :provider provider
                                       :prompt prompt
                                       :output ""
                                       :run-id run-id})
                ;; POST blocks on server until complete, .then() fires with result
                (post-edn! "/api/agent/run"
                           request-body
                           (fn [resp]
                             (js/console.log "[AGENT][CLIENT][RESPONSE]"
                                             (clj->js {:run-id run-id
                                                       :status (:status resp)
                                                       :provider (:provider resp)
                                                       :prompt (:prompt resp)
                                                       :result-summary (some-> (:result resp)
                                                                               (select-keys [:exit-code :timed-out? :timeout-ms :duration-ms]))}))
                             (if-let [err (:error resp)]
                               (reset! !agent-output {:status :failed
                                                      :provider provider
                                                      :prompt prompt
                                                      :output err
                                                      :run-id run-id})
                               (let [result (:result resp)]
                                 (reset! !agent-output {:status (:status resp)
                                                        :provider (:provider resp)
                                                        :prompt (:prompt resp)
                                                        :output (or (:output result) "")
                                                        :run-id (:run-id resp)})))))))))

        render-sidebar!
        (fn render-sidebar! []
          (when sidebar-el
            (let [visible? (and !sidebar-visible @!sidebar-visible)
                  project @!selected-project
                  expanded @!expanded-dirs
                  cache @!dir-cache]
              ;; Toggle visibility
              (set! (.. sidebar-el -style -display) (if visible? "block" "none"))
              (when visible?
                ;; Clear content
                (set! (.-innerHTML sidebar-el) "")
                (if (nil? project)
                  ;; PROJECT PICKER — fetch home dirs then render
                  (do
                    ;; Header
                    (let [header (js/document.createElement "div")]
                      (set! (.-textContent header) "EXPLORER")
                      (set! (.-cssText (.-style header))
                            "padding:10px 12px;font-size:11px;text-transform:uppercase;letter-spacing:0.05em;color:#7878a0;border-bottom:1px solid #2a2a4a;")
                      (.appendChild sidebar-el header))
                    ;; Render dirs (or loading)
                    (if-let [dirs @!home-dirs]
                      (doseq [d dirs]
                        (let [el (js/document.createElement "div")]
                          (set! (.-textContent el) (str "📁 " (:name d)))
                          (set! (.-cssText (.-style el))
                                "padding:6px 12px;cursor:pointer;font-size:13px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;")
                          (set! (.-onmouseenter el) #(set! (.. el -style -background) "#252547"))
                          (set! (.-onmouseleave el) #(set! (.. el -style -background) "transparent"))
                          (set! (.-onclick el)
                                (fn [_]
                                  (reset! !selected-project {:name (:name d) :path (:path d)})
                                  (reset! !expanded-dirs #{})
                                  (reset! !dir-cache {})
                                  ;; Fetch root dir contents, then re-render
                                  (fetch-dir! (:path d) render-sidebar!)))
                          (.appendChild sidebar-el el)))
                      ;; Show loading while fetching
                      (let [loading (js/document.createElement "div")]
                        (set! (.-textContent loading) "Loading...")
                        (set! (.-cssText (.-style loading))
                              "padding:10px 12px;color:#7878a0;font-style:italic;font-size:12px;")
                        (.appendChild sidebar-el loading)
                        ;; Trigger fetch
                        (fetch-home-dirs! render-sidebar!))))

                  ;; FILE TREE VIEW
                  (let [;; Back button
                        back-btn (js/document.createElement "div")
                        _ (do (set! (.-textContent back-btn) (str "← " (:name project)))
                              (set! (.-cssText (.-style back-btn))
                                    "padding:8px 12px;cursor:pointer;font-size:12px;color:#7878a0;border-bottom:1px solid #2a2a4a;")
                              (set! (.-onmouseenter back-btn) #(set! (.. back-btn -style -background) "#252547"))
                              (set! (.-onmouseleave back-btn) #(set! (.. back-btn -style -background) "transparent"))
                              (set! (.-onclick back-btn)
                                    (fn [_]
                                      (reset! !selected-project nil)
                                      (reset! !expanded-dirs #{})
                                      (reset! !dir-cache {})
                                      (reset! !current-file nil)
                                      (render-sidebar!)))
                              (.appendChild sidebar-el back-btn))
                        ;; Render tree entries recursively
                        current-file @!current-file
                        ;; Open file breadcrumb
                        _ (when current-file
                            (let [breadcrumb (js/document.createElement "div")]
                              (set! (.-textContent breadcrumb) (:name current-file))
                              (set! (.-cssText (.-style breadcrumb))
                                    "padding:4px 12px 4px 14px;font-size:11px;color:#9898b8;border-bottom:1px solid #2a2a4a;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;")
                              (.appendChild sidebar-el breadcrumb)))
                        render-entries
                        (fn render-entries [entries depth]
                          (doseq [entry entries]
                            (let [el (js/document.createElement "div")
                                  is-dir? (= (:type entry) :dir)
                                  is-exp? (contains? expanded (:path entry))
                                  is-active? (and (not is-dir?) current-file
                                                  (= (:path entry) (:path current-file)))
                                  pad-left (+ 12 (* depth 16))]
                              (set! (.-textContent el)
                                    (if is-dir?
                                      (str (if is-exp? "▾ " "▸ ") (:name entry) "/")
                                      (str "  " (:name entry))))
                              (set! (.-cssText (.-style el))
                                    (str "padding:4px 12px;padding-left:" pad-left "px;cursor:pointer;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-size:13px;"
                                         (if is-active?
                                           "background:#37375a;border-left:3px solid #5588ff;color:#e0e0ff;"
                                           "border-left:3px solid transparent;")))
                              (set! (.-onmouseenter el) #(set! (.. el -style -background) "#252547"))
                              (set! (.-onmouseleave el) #(set! (.. el -style -background)
                                                               (if is-active? "#37375a" "transparent")))
                              (set! (.-onclick el)
                                    (fn [_]
                                      (if is-dir?
                                        (do (swap! !expanded-dirs
                                                   (fn [dirs]
                                                     (if (contains? dirs (:path entry))
                                                       (disj dirs (:path entry))
                                                       (conj dirs (:path entry)))))
                                            ;; Fetch children if not cached, then re-render
                                            (fetch-dir! (:path entry) render-sidebar!))
                                        ;; File click — fetch content from server
                                        (fetch-file! (:path entry) (:path project)))))
                              (.appendChild sidebar-el el)
                              ;; Render children if expanded and cached
                              (when (and is-dir? is-exp?)
                                (if-let [children (get cache (:path entry))]
                                  (render-entries children (inc depth))
                                  ;; Not cached yet — show loading placeholder
                                  (let [loading (js/document.createElement "div")]
                                    (set! (.-textContent loading) "  loading...")
                                    (set! (.-cssText (.-style loading))
                                          (str "padding:4px 12px;padding-left:" (+ pad-left 16) "px;color:#7878a0;font-size:12px;font-style:italic;"))
                                    (.appendChild sidebar-el loading)))))))
                        root-entries (get cache (:path project) [])]
                    (render-entries root-entries 0)))))))

        ;; Watch sidebar-related atoms to re-render
        _ (when !sidebar-visible
            (add-watch !sidebar-visible :sidebar-render
                       (fn [_ _ old-vis new-vis]
                         (render-sidebar!)
                         ;; When becoming visible with no project, fetch home dirs
                         (when (and new-vis (not old-vis) (nil? @!selected-project))
                           (fetch-home-dirs! render-sidebar!)))))
        _ (add-watch !selected-project :sidebar-render (fn [_ _ _ _] (render-sidebar!)))
        _ (add-watch !expanded-dirs :sidebar-render (fn [_ _ _ _] (render-sidebar!)))
        _ (add-watch !dir-cache :sidebar-render (fn [_ _ _ _] (render-sidebar!)))
        _ (add-watch !current-file :sidebar-render (fn [_ _ _ _] (render-sidebar!)))

        ;; Initial sidebar render (watches only fire on change, not initial state)
        _ (when (and !sidebar-visible @!sidebar-visible)
            (fetch-home-dirs! render-sidebar!))

        ;; Seed sidebar with initial file if provided
        _ (when initial-file
            (let [file-path (:path initial-file)
                  file-name (last (str/split file-path #"/"))
                  project-path (:project initial-file)]
              (reset! !current-file {:path file-path :name file-name})
              (when project-path
                ;; Expand dirs along the file path so the file is visible in the tree
                (let [rel (subs file-path (count project-path))
                      rel (if (str/starts-with? rel "/") (subs rel 1) rel)
                      parts (str/split rel #"/")
                      dir-parts (butlast parts)
                      dir-paths (loop [acc [] prefix project-path dirs dir-parts]
                                  (if (empty? dirs)
                                    acc
                                    (let [next-path (str prefix "/" (first dirs))]
                                      (recur (conj acc next-path) next-path (rest dirs)))))]
                  (reset! !selected-project {:name (last (str/split project-path #"/"))
                                             :path project-path})
                  (reset! !expanded-dirs (set dir-paths))
                  ;; Fetch root dir + expanded dirs so tree renders with content
                  (fetch-dir! project-path
                              (fn []
                                (doseq [dp dir-paths]
                                  (fetch-dir! dp render-sidebar!))))))))

        ;; =====================================================================
        ;; LAYER 2: EVENT FLOWS
        ;; =====================================================================
        ;; IMPORTANT: These must be fresh flows, not shared top-level defs!

        >raf (make-raf-flow)              ;; Fresh RAF flow for this instance
        >blink-timer (make-blink-timer)   ;; Fresh blink timer for this instance
        >resize (>canvas-resize node)
        >wheel-events (>wheel node)
        >mouse-events (->> (>mouse node) (m/relieve (fn [_ x] x)))
        >keyboard-events (>keyboard js/window)

        ;; =====================================================================
        ;; LAYER 4: FOCUS-BASED EVENT ROUTING
        ;; =====================================================================

        <global-keys (<global-events >keyboard-events)
        <editor-keyboard (<editor-keys >keyboard-events !focus)
        <cmd-keyboard (<cmd-panel-keys >keyboard-events !focus)
        <settings-keyboard (<settings-panel-keys >keyboard-events !focus)]

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
                       (let [dpr (:dpr @!viewport)
                             snap? (:snap-to-pixel? @!settings)]
                         (swap! !scroll-y #(maybe-snap (+ % delta) dpr snap?)))
                       nil) nil))

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
                       settings @!settings
                       settings-visible? (:visible settings)

                       ;; Settings panel geometry (must match compute-settings-panel-rects)
                       panel-w 600
                       panel-h 480
                       panel-x (/ (- (:width viewport) panel-w) 2)
                       panel-y (+ scroll-y (/ (- (:height viewport) panel-h) 2))

                       ;; Check if click is inside settings panel
                       clicked-in-settings? (and settings-visible?
                                                  (>= x panel-x) (< x (+ panel-x panel-w))
                                                  (>= y panel-y) (< y (+ panel-y panel-h)))]

                   (if clicked-in-settings?
                     ;; Click in settings panel
                     (let [left-w 220
                           header-h 40
                           content-y (+ panel-y header-h)
                           
                           ;; Font List
                           font-item-h 32
                           
                           ;; Sliders
                           slider-item-h 70
                           slider-count (count (slider-specs settings))
                           
                           rel-x (- x panel-x)
                           rel-y (- y content-y)]

                       (cond
                         ;; Click in Header (ignore)
                         (< rel-y 0) nil
                         
                         ;; Click in Left Pane (Fonts)
                         (< rel-x left-w)
                         (let [font-idx (int (/ rel-y font-item-h))
                               fonts (or (:fonts @!font-manifest)
                                         [{:name "DejaVu Sans Mono" :id "dejavu-sans-mono"}])
                               available-fonts (filterv #(not (false? (:available %))) fonts)]
                           (when (< font-idx (count available-fonts))
                             (swap! !settings assoc 
                                    :selected-index font-idx
                                    :focus-section :fonts)
                             ;; Immediately update active font
                             (let [selected-font (nth available-fonts font-idx)]
                               (reset! !active-font {:id (:id selected-font)
                                                     :char-width (or (:charWidth selected-font) 0.56)
                                                     :name (:name selected-font)}))
                             (js/console.log "[SETTINGS] Clicked font:" font-idx)))

                         ;; Click in Right Pane (Sliders)
                         :else
                         (let [slider-idx (int (/ rel-y slider-item-h))]
                           (when (< slider-idx slider-count)
                             (swap! !settings assoc 
                                    :slider-index slider-idx
                                    :focus-section :sliders)
                            (js/console.log "[SETTINGS] Clicked slider:" slider-idx)))))

                     ;; Not in settings - check command panel or editor
                     (let [cmd-panel @!cmd-panel
                           cmd-visible? (:visible cmd-panel)
                           cmd-panel-top (if cmd-visible?
                                           (- (:height viewport) cmd-panel-h)
                                           (:height viewport))
                           clicked-in-cmd? (and cmd-visible? (>= y cmd-panel-top))]

                       (if clicked-in-cmd?
                         ;; Click in command panel - use reactive font values
                         (let [font-size (:font-size @!settings)
                              dpr (:dpr @!viewport)
                              snap? (:snap-to-pixel? @!settings)
                              char-width (:char-width @!active-font)
                              char-w (maybe-snap (* font-size char-width) dpr snap?)
                              text-x (maybe-snap 60 dpr snap?)
                              text (:text cmd-panel)
                              col (-> (/ (- x text-x) char-w)
                                       (Math/round)
                                       (max 0)
                                       (min (count text)))]
                           (reset! !focus :command-panel)
                           (swap! !cmd-panel assoc :cursor col)
                           (reset! !caret-visible true))
                        ;; Click in editor - use reactive font values
                        (let [adj-y (+ y scroll-y)
                              dpr (:dpr @!viewport)
                              snap? (:snap-to-pixel? @!settings)
                              font-size (:font-size @!settings)
                              char-width (:char-width @!active-font)
                              line-h (maybe-snap (* font-size (:line-height @!settings)) dpr snap?)
                              char-w (maybe-snap (* font-size char-width) dpr snap?)
                              layout-x (maybe-snap layout-x dpr snap?)
                              layout-y (maybe-snap layout-y dpr snap?)
                              gutter-x (- layout-x gutter-w)
                              gutter-right (+ gutter-x gutter-w)]
                          (if (and (>= x gutter-x) (< x gutter-right))
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
                              (reset! !focus :editor))))))))

                 :mousemove
                 (when @!dragging?
                   ;; Use reactive font values for mouse drag selection
                   (let [{:keys [x y]} coords
                         scroll-y @!scroll-y
                         dpr (:dpr @!viewport)
                         snap? (:snap-to-pixel? @!settings)
                         font-size (:font-size @!settings)
                         char-width (:char-width @!active-font)
                         line-h (maybe-snap (* font-size (:line-height @!settings)) dpr snap?)
                         adj-y (+ y scroll-y)
                         layout-x (maybe-snap layout-x dpr snap?)
                         layout-y (maybe-snap layout-y dpr snap?)
                         text-result @!text-geo
                         line-mapping (or (:line-mapping text-result) [])
                         lengths (mapv count (:lines @!editor-doc))
                         visual-line (max 0 (Math/floor (/ (- adj-y layout-y) line-h)))
                         logical-line (get line-mapping visual-line
                                          (min visual-line (dec (count lengths))))
                         line-len (get lengths logical-line 0)
                         char-w (maybe-snap (* font-size char-width) dpr snap?)
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
                           (swap! !settings assoc :visible false)  ;; Close settings if open
                           (reset! !focus :command-panel)
                           (reset! !caret-visible true))))

                   :toggle-settings-panel
                   (let [visible? (:visible @!settings)]
                     (if visible?
                       (do (js/console.log "[SETTINGS] Closing, focus -> :editor")
                           (swap! !settings assoc :visible false)
                           (reset! !focus :editor))
                       (do (js/console.log "[SETTINGS] Opening, focus -> :settings-panel")
                           (swap! !settings assoc :visible true)
                           (swap! !cmd-panel assoc :visible false)  ;; Close command panel if open
                           (reset! !focus :settings-panel)
                           (js/console.log "[SETTINGS] Focus is now:" @!focus))))

                   :escape
                   (cond
                     (:visible @!settings)
                     (do (swap! !settings assoc :visible false)
                         (reset! !focus :editor))

                     (:visible @!cmd-panel)
                     (do (swap! !cmd-panel assoc :visible false)
                         (reset! !focus :editor))

                     (and !sidebar-visible @!sidebar-visible)
                     (reset! !sidebar-visible false)

                     :else
                     (swap! !editor-doc assoc :selection nil))

                   :toggle-file-viewer
                   (when !sidebar-visible
                     (swap! !sidebar-visible not))

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
      ;; FILE LOAD CONSUMER
      ;; =====================================================================
      ;; Watches !file-load-request atom. When set to a map with :lines,
      ;; resets the editor state to show the new file content.
      (if !file-load-request
        (->> (m/watch !file-load-request)
             (m/eduction (filter some?))
             (m/reduce
               (fn [_ request]
                 (let [{:keys [lines]} request]
                   (when (seq lines)
                     (js/console.log "[FILE-LOAD] Loading file with" (count lines) "lines")
                     (reset! !editor-doc {:lines (vec lines)
                                          :cursor {:line 0 :col 0}
                                          :selection nil
                                          :desired-col 0})
                     (reset! !scroll-y 0)
                     (reset! !undo-stack [])
                     (reset! !redo-stack [])
                     (reset! !folded-lines #{})
                     (reset! !caret-visible true)
                     (reset! !focus :editor)
                     ;; Clear the request so same file can be re-opened
                     (reset! !file-load-request nil)))
                 nil)
               nil))
        ;; No-op task when !file-load-request not provided
        (m/reduce (fn [_ _] nil) nil (m/seed [nil])))

      ;; =====================================================================
      ;; EDITOR KEYBOARD EVENTS CONSUMER
      ;; =====================================================================
      (->> <editor-keyboard
           (m/reduce
             (fn [_ event]
               (when event
                 (js/console.log "[EDITOR KEY]" (:type event) "focus=" @!focus)
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
                           ;; Auto-scroll to keep caret visible (use reactive font values)
                           font-size (:font-size @!settings)
                           dpr (:dpr @!viewport)
                           snap? (:snap-to-pixel? @!settings)
                           line-h (maybe-snap (* font-size (:line-height @!settings)) dpr snap?)
                           layout-y (maybe-snap layout-y dpr snap?)
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

                                        :else scroll-y)
                           new-scroll (maybe-snap new-scroll dpr snap?)]
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
                       (submit-agent-run! cmd-text))
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
      ;; SETTINGS PANEL KEYBOARD EVENTS CONSUMER
      ;; =====================================================================
      (->> <settings-keyboard
           (m/reduce
             (fn [_ event]
               (when event
                 (js/console.log "[SETTINGS KEY]" (:type event) "focus=" @!focus)
                 (let [settings @!settings
                       fonts (or (:fonts @!font-manifest)
                                 [{:name "DejaVu Sans Mono" :id "dejavu-sans-mono" :charWidth 0.60}])
                       available-fonts (filterv #(not (false? (:available %))) fonts)
                       font-count (count available-fonts)
                       
                       ;; Current state
                       focus-section (:focus-section settings)
                       slider-index (:slider-index settings)
                       sliders (slider-specs settings)
                       slider-count (count sliders)]

                   (case (:type event)
                     ;; Tab: Switch Pane
                     :char
                     (if (= (:char event) "Tab")
                       (swap! !settings update :focus-section
                              (fn [s] (if (= s :fonts) :sliders :fonts)))
                       nil)

                     ;; Navigation
                     :up
                     (if (= focus-section :fonts)
                       ;; Fonts: Change selection
                       (let [new-idx (max 0 (dec (:selected-index settings)))
                             selected-font (nth available-fonts new-idx nil)]
                         (swap! !settings assoc :selected-index new-idx)
                         (when selected-font
                           (reset! !active-font {:id (:id selected-font)
                                                 :char-width (or (:charWidth selected-font) 0.56)
                                                 :name (:name selected-font)})))
                       ;; Sliders: Change selection
                       (swap! !settings update :slider-index #(max 0 (dec %))))

                     :down
                     (if (= focus-section :fonts)
                       ;; Fonts: Change selection
                       (let [new-idx (min (dec font-count) (inc (:selected-index settings)))
                             selected-font (nth available-fonts new-idx nil)]
                         (swap! !settings assoc :selected-index new-idx)
                         (when selected-font
                           (reset! !active-font {:id (:id selected-font)
                                                 :char-width (or (:charWidth selected-font) 0.56)
                                                 :name (:name selected-font)})))
                       ;; Sliders: Change selection
                       (swap! !settings update :slider-index #(min (dec slider-count) (inc %))))

                     ;; Value Adjustment (Sliders only)
                     :left
                     (when (= focus-section :sliders)
                       (let [slider (nth sliders slider-index)
                             slider-id (:id slider)]
                         (case slider-id
                           :theme-id    (let [cur-idx (themes/theme-index (:theme-id settings))
                                              new-idx (mod (dec cur-idx) (count themes/theme-list))]
                                          (swap! !settings assoc :theme-id (nth themes/theme-list new-idx)))
                           :font-size   (swap! !settings update :font-size #(max 8 (dec %)))
                           :line-height (swap! !settings update :line-height #(max 1.0 (- % 0.1)))
                           :px-range    (swap! !settings update :px-range #(max 4 (dec %)))
                           :sharpness   (swap! !settings update :sharpness #(max -0.2 (- % 0.02)))
                           :snap-to-pixel? (swap! !settings assoc :snap-to-pixel? false)
                           :show-diagnostics? (swap! !settings assoc :show-diagnostics? false))))

                     :right
                     (when (= focus-section :sliders)
                       (let [slider (nth sliders slider-index)
                             slider-id (:id slider)]
                         (case slider-id
                           :theme-id    (let [cur-idx (themes/theme-index (:theme-id settings))
                                              new-idx (mod (inc cur-idx) (count themes/theme-list))]
                                          (swap! !settings assoc :theme-id (nth themes/theme-list new-idx)))
                           :font-size   (swap! !settings update :font-size #(min 40 (inc %)))
                           :line-height (swap! !settings update :line-height #(min 2.0 (+ % 0.1)))
                           :px-range    (swap! !settings update :px-range #(min 12 (inc %)))
                           :sharpness   (swap! !settings update :sharpness #(min 0.2 (+ % 0.02)))
                           :snap-to-pixel? (swap! !settings assoc :snap-to-pixel? true)
                           :show-diagnostics? (swap! !settings assoc :show-diagnostics? true))))

                     ;; Close
                     :enter
                     (do
                       (swap! !settings assoc :visible false)
                       (reset! !focus :editor)
                       (js/console.log "[SETTINGS] Closed panel"))
                       
                     :escape
                     (do
                       (swap! !settings assoc :visible false)
                       (reset! !focus :editor))

                     nil)))
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
            ;; CACHED: fold state only recomputes when doc/folds change (not on blink)
            <fold-data (<fold-state !editor-doc !folded-lines detect-folds-fn)
            ;; CACHED: bracket match only recomputes when doc changes (not on blink)
            <bracket-data (<bracket-match !editor-doc find-bracket-fn)

            <text-data (<combined-text-ops !editor-doc !cmd-panel !ai-provider !agent-output !scroll-y !viewport !folded-lines !settings !active-font
                                           tokenize-fn layout-fn detect-folds-fn
                                           layout-x layout-y cmd-panel-h)
            <editor-rect-data (<editor-rects !editor-doc !eval-result !caret-visible !focus
                                             !settings !active-font !viewport
                                             <fold-data <bracket-data
                                             layout-x layout-y gutter-w)
            <cmd-rect-data (<cmd-panel-rects !cmd-panel !focus !caret-visible !scroll-y !viewport
                                             !settings !active-font cmd-panel-h)
            ;; Settings panel flows (reactive: derive font-size from !settings internally)
            <settings-rect-data (<settings-panel-rects !settings !focus !viewport !scroll-y !font-manifest)
            <settings-text-data (<settings-panel-text !settings !viewport !scroll-y !font-manifest)

            ;; Combined world state - sampled on RAF
            ;; m/latest combines flows, m/sample synchronizes with frame clock
            ;; All derived flows now use m/latest internally, so they're continuous
            <world-snapshot (m/latest
                              (fn [text-data editor-rects cmd-rects settings-rects settings-text
                                   viewport scroll-y cmd-panel settings active-font]
                                {:text-data text-data
                                 :editor-rects editor-rects
                                 :cmd-rects cmd-rects
                                 :settings-rects settings-rects
                                 :settings-text settings-text
                                 :viewport viewport
                                 :scroll-y scroll-y
                                 :cmd-visible (:visible cmd-panel)
                                 :settings-visible (:visible settings)
                                 ;; Include font settings for reactive text rendering
                                 :font-size (:font-size settings)
                                 :px-range (:px-range settings)
                                 :line-height (:line-height settings)
                                 :sharpness (:sharpness settings)
                                 :snap-to-pixel? (:snap-to-pixel? settings)
                                 :show-diagnostics? (:show-diagnostics? settings)
                                 :char-width (:char-width active-font)})
                              <text-data
                              <editor-rect-data
                              <cmd-rect-data
                              <settings-rect-data
                              <settings-text-data
                              (m/watch !viewport)
                              (m/watch !scroll-y)
                              (m/watch !cmd-panel)
                              (m/watch !settings)
                              (m/watch !active-font))]

        ;; The render pulse: sample world state on each animation frame
        ;; OPTIMIZATION: Use identical? on flow objects (cheap pointer compare)
        ;; instead of = on reconstructed data (expensive deep structural compare).
        ;; Skip draw-frame! entirely when nothing changed.
        (m/reduce
          (fn [prev-state [world _frame-time]]
              ;; --- Fast dirty check using identical? on flow objects ---
              ;; m/latest caches its result, so when no input changed,
              ;; m/sample returns the exact same object. Quick pointer compare:
              (if (identical? world (:prev-world prev-state))
                ;; FAST PATH: nothing changed, skip everything (no draw-frame!)
                prev-state

                ;; SLOW PATH: something changed, figure out what
                (let [{:keys [text-data editor-rects cmd-rects settings-rects settings-text
                              viewport scroll-y cmd-visible settings-visible
                              font-size px-range line-height sharpness char-width
                              snap-to-pixel? show-diagnostics?]} world

                      dpr (:dpr viewport)
                      snap? (not (false? snap-to-pixel?))
                      line-h (maybe-snap (* font-size line-height) dpr snap?)
                      snap-step (when snap? (/ 1 (or dpr 1)))

                      ;; Get current font assets from atom (updated by watch)
                      font-assets @!font-assets

                      ;; Check if font changed
                      prev-font-id (:prev-font-id prev-state)
                      font-changed? (not= (:id font-assets) prev-font-id)

                      ;; Current renderer state
                      current-text-geo (:text-geo prev-state)

                      ;; Update font texture if needed (when we have a new bitmap)
                      updated-text-geo (if (and font-changed? (:bitmap font-assets))
                                         (do
                                           (js/console.log "[RENDER] Updating font texture for:" (:id font-assets))
                                           (editor/update-font-texture device current-text-geo (:bitmap font-assets)))
                                         current-text-geo)

                      ;; Determine atlas to use for shaping
                      active-atlas (or (:atlas font-assets) atlas)

                      ;; Upload text geometry (only if changed)
                      ;; Use identical? on the flow object (cheap) instead of = on vec (expensive)
                      settings-lines (when settings-visible (when settings-text [settings-text]))
                      diagnostics-line (when show-diagnostics?
                                         (let [diag-x (maybe-snap 16 dpr snap?)
                                               diag-y (maybe-snap (+ scroll-y 20) dpr snap?)
                                               diag-size (max 10 (- font-size 2))
                                               atlas-size (get-in active-atlas [:atlas :size])
                                               font-name (:name @!active-font)
                                               diag-text (str "font: " (or font-name (:id font-assets)) "\n"
                                                              "dpr: " dpr "  snap: " (if snap? "on" "off") "\n"
                                                              "pxRange: " px-range "  sharp: " sharpness "\n"
                                                              "atlas: " atlas-size "  charW: " char-width)]
                                           [{:text diag-text
                                             :type :comment
                                             :from 0 :to (count diag-text)
                                             :x diag-x
                                             :y diag-y
                                             :size diag-size
                                             :r 0.7 :g 0.7 :b 0.7 :a 1.0}]))

                      ;; Check text inputs by identity (flow objects are cached by m/latest)
                      text-same? (and (identical? text-data (:prev-text-data prev-state))
                                      (identical? settings-text (:prev-settings-text prev-state))
                                      (= show-diagnostics? (:prev-show-diagnostics prev-state))
                                      (= scroll-y (:prev-scroll-y prev-state))  ;; diagnostics HUD uses scroll-y
                                      (= font-size (:prev-font-size prev-state))
                                      (= px-range (:prev-px-range prev-state))
                                      (= line-h (:prev-line-height prev-state))
                                      (= sharpness (:prev-sharpness prev-state))
                                      (= char-width (:prev-char-width prev-state))
                                      (= snap-step (:prev-snap-step prev-state))
                                      (not font-changed?))

                      all-text-ops (if text-same?
                                     (:prev-text-ops prev-state)
                                     (vec (concat (:render-ops text-data)
                                                  settings-lines
                                                  diagnostics-line)))
                      editor-line-count (:editor-line-count text-data)
                      cmd-line-count (:cmd-line-count text-data)
                      settings-line-count (count (or settings-lines []))
                      diagnostics-line-index (when diagnostics-line
                                               (+ editor-line-count cmd-line-count settings-line-count))

                      base-text-geo (if (not text-same?)
                                      (editor/update-text-data device updated-text-geo
                                                               all-text-ops active-atlas font-size
                                                               :px-range px-range
                                                               :line-height line-h
                                                               :char-width char-width
                                                               :snap-step snap-step
                                                               :sharpness sharpness)
                                      updated-text-geo)
                      new-text-geo (assoc base-text-geo
                                          :line-mapping (:line-mapping text-data)
                                          :editor-line-count editor-line-count
                                          :cmd-line-count cmd-line-count
                                          :diagnostics-line-index diagnostics-line-index)

                      ;; Upload editor rects (only if changed — use identical? for flow objects)
                      new-editor-sys (if (not (identical? editor-rects (:prev-editor-rects prev-state)))
                                       (editor/update-rects device
                                                            (or (:editor-rect-sys prev-state) (:rect geometry))
                                                            editor-rects)
                                       (:editor-rect-sys prev-state))

                      ;; Upload cmd panel rects (only if changed)
                      new-cmd-sys (if (not (identical? cmd-rects (:prev-cmd-rects prev-state)))
                                    (editor/update-rects device
                                                         (or (:cmd-rect-sys prev-state) @!cmd-rect-sys)
                                                         (or cmd-rects []))
                                    (:cmd-rect-sys prev-state))

                      ;; Upload settings panel rects (only if changed)
                      new-settings-sys (if (not (identical? settings-rects (:prev-settings-rects prev-state)))
                                         (editor/update-rects device
                                                              (or (:settings-rect-sys prev-state) @!settings-rect-sys)
                                                              (or settings-rects []))
                                         (:settings-rect-sys prev-state))]

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
                                      :editor-line-count (:editor-line-count text-data)
                                      :settings-visible settings-visible
                                      :settings-rect-sys new-settings-sys
                                      :diagnostics-visible show-diagnostics?
                                      :diagnostics-line-index diagnostics-line-index)

                  ;; Return state for next frame comparison
                  {:text-geo new-text-geo
                       :editor-rect-sys new-editor-sys
                       :cmd-rect-sys new-cmd-sys
                       :settings-rect-sys new-settings-sys
                       :prev-world world
                       :prev-text-data text-data
                       :prev-settings-text settings-text
                       :prev-show-diagnostics show-diagnostics?
                       :prev-scroll-y scroll-y
                       :prev-text-ops all-text-ops
                       :prev-editor-rects editor-rects
                       :prev-cmd-rects cmd-rects
                       :prev-settings-rects settings-rects
                       :prev-font-size font-size
                       :prev-px-range px-range
                       :prev-line-height line-h
                       :prev-sharpness sharpness
                       :prev-char-width char-width
                       :prev-snap-step snap-step
                       :prev-font-id (:id font-assets)})))

          ;; Initial state
          {:text-geo (:text geometry)
           :editor-rect-sys (:rect geometry)
           :cmd-rect-sys @!cmd-rect-sys
           :settings-rect-sys @!settings-rect-sys
           :prev-world nil
           :prev-text-data nil
           :prev-settings-text nil
           :prev-show-diagnostics nil
           :prev-scroll-y nil
           :prev-text-ops nil
           :prev-editor-rects nil
           :prev-cmd-rects nil
           :prev-settings-rects nil
           :prev-font-size nil
           :prev-px-range nil
           :prev-line-height nil
           :prev-sharpness nil
           :prev-char-width nil
           :prev-snap-step nil
           :prev-font-id "dejavu-sans-mono"}

          ;; Sample world state on each RAF tick
          (m/sample vector <world-snapshot >raf))))))
