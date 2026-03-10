(ns app.client.workspace.editor-compute
  "Editor state computation: event handling, fold/bracket, rect building."
  (:require [clojure.string :as str]
            [missionary.core :as m]
            [app.client.workspace.events :refer [maybe-snap]]
            [app.client.workspace.rect-tree :refer [rt-node resolve-layout tree->rects tree->shadows]]
            [app.client.workspace.themes :as themes]
            [app.client.workspace.text-input :as text-input]
            [app.client.workspace.ui-primitives :refer [dt]]
            [app.client.workspace.sidebar :as sidebar :refer [sidebar-w cmd-panel-h status-bar-h build-sidebar-tree]]
            [app.client.workspace.shell :refer [build-file-layout]]))

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
   layout-x layout-y line-h gutter-w char-advance viewport-w
   & {:keys [gutter-lx]}]
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
        ;; Gutter uses unscrolled x so fold indicators stay fixed
        gutter-x (- (or gutter-lx layout-x) gutter-w)

        ;; Fold indicator rects
        fold-rects (keep (fn [{:keys [start-line]}]
                          (when-let [visual-y (logical->visual-y start-line)]
                            (let [is-folded? (contains? folded start-line)
                                  indicator-size 8
                                  x (+ gutter-x 2)
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
                                            width-chars (- col-end col-start)
                                            x (+ layout-x (* col-start char-w))
                                            raw-w (* width-chars char-w)
                                            ;; Clamp to editor pane boundary
                                            clamped-w (min raw-w (max 0 (- viewport-w x)))]
                                        (when (> clamped-w 0)
                                          {:x x :y visual-y
                                           :w clamped-w :h line-h
                                           :r 0.2 :g 0.4 :b 0.9 :a 0.5}))))
                                  (range (:line s) (inc (:line e))))))

        ;; Current-line highlight (subtle background on cursor's line)
        current-line-rect (when (and cursor (= focus :editor) (not selection))
                            (when-let [visual-y (logical->visual-y (:line cursor))]
                              {:x 0 :y visual-y :w viewport-w :h line-h
                               :r 1.0 :g 1.0 :b 1.0 :a 0.04}))

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

    (vec (concat (if current-line-rect [current-line-rect] [])
                 fold-rects
                 (or bracket-rects [])
                 (or selection-rects [])
                 (if caret-rect [caret-rect] [])
                 (if eval-rect [eval-rect] [])))))

(defn <editor-rects
  "Derived flow: all editor rectangles (selection, caret, brackets, folds, eval)
   Uses m/latest instead of m/ap to avoid cancellation propagation issues.
   REACTIVE: font-size, line-h, char-advance come from !settings and !active-font.
   OPTIMIZED: fold-state and bracket-match are pre-computed in cached flows
   that only recompute when the document changes — NOT on every blink tick.
   MODE-SWITCH: when flow canvas is active, returns ticket card rects instead.
   SIDEBAR: when sidebar visible, sidebar rects prepended, content offset right."
  [!editor-doc !eval-result !caret-visible !focus !settings !active-font !viewport
   <fold-data <bracket-data
   !flow-state !scroll-y !collapsed-groups !hovered-row-idx !drag-state
   !sidebar-state !sidebar-visible !current-file !extract-preview !agent-output
   !shimmer-phase !trail-collapsed !active-pane !scroll-x !chat-scroll-y !chat-input !run-scroll-y !detail-scroll-y
   flow-canvas-active?* compute-ticket-list-rects* compute-run-rects* offset-rects* offset-shadows*
   layout-x layout-y gutter-w]
  (m/latest
    (fn [doc fold-state bracket-match eval-result caret-visible focus settings active-font viewport
         flow-state scroll-y collapsed-groups hovered-row-idx drag-state
         sidebar-state sidebar-visible? current-file extract-preview agent-output
         shimmer-phase trail-collapsed active-pane scroll-x chat-scroll-y chat-input run-scroll-y detail-scroll-y]
      (let [sb-vis? (boolean sidebar-visible?)
            sb-w (if sb-vis? sidebar-w 0)
            dpr (:dpr viewport)
            snap? (:snap-to-pixel? settings)
            font-size (:font-size settings)
            char-advance (maybe-snap (* font-size (:char-width active-font)) dpr snap?)
            ;; Build sidebar rects when visible
            sidebar-result
            (when sb-vis?
              (let [tree (resolve-layout
                           (build-sidebar-tree sidebar-state current-file true
                                               (:height viewport) scroll-y font-size char-advance))]
                (when tree
                  {:rects (tree->rects tree)
                   :shadows (tree->shadows tree)})))
            ;; Build content rects (offset by sb-w)
            content-w (- (:width viewport) sb-w)
            file-open? (some? current-file)
            content-result
            (if (:rt-node extract-preview)
              ;; Extract preview on right half
              (let [half-w (/ (- (:width viewport) sb-w) 2)
                    preview-tree (when-let [rt (:rt-node extract-preview)]
                                   (resolve-layout
                                     (rt-node :extract-preview-root :rect
                                              {:x half-w :y 0 :w half-w :h (:height viewport)}
                                              :style {:bg (:bg (:colors dt))}
                                              :layout {:direction :column :padding [16 16 16 16] :gap 8}
                                              :children [(rt-node :extract-label :text
                                                                  {:x 0 :y 0 :w (- half-w 32) :h 24}
                                                                  :text [{:text "Compiled Preview" :type :keyword
                                                                          :from 0 :to 16 :x 0 :y 16
                                                                          :size 14 :r 0.55 :g 0.55 :b 0.60 :a 1.0}])
                                                         (assoc-in rt [:bounds :w] (- half-w 32))])))]
                {:rects (if preview-tree (tree->rects preview-tree) [])
                 :shadows (if preview-tree (tree->shadows preview-tree) [])})
              (if (flow-canvas-active?* flow-state)
                (if (= :intake (:node flow-state))
                  (compute-ticket-list-rects* flow-state content-w (:height viewport)
                                              scroll-y detail-scroll-y hovered-row-idx collapsed-groups drag-state
                                              font-size char-advance)
                  (compute-run-rects* flow-state content-w (:height viewport)
                                      scroll-y agent-output font-size char-advance
                                      shimmer-phase trail-collapsed run-scroll-y))
                ;; File open -> 3-pane layout; no file -> plain editor
                (if file-open?
                  (let [code-w (int (* content-w 0.4))
                        content-h (- (:height viewport) cmd-panel-h status-bar-h)
                        line-h (maybe-snap (* font-size (:line-height settings)) dpr snap?)
                        lx (maybe-snap (- layout-x (or scroll-x 0)) dpr snap?)
                        ly (maybe-snap layout-y dpr snap?)
                        ulx (maybe-snap layout-x dpr snap?) ;; unscrolled for gutter
                        editor-rects (compute-editor-rects doc fold-state bracket-match eval-result
                                                           caret-visible focus lx ly line-h gutter-w
                                                           char-advance code-w
                                                           :gutter-lx ulx)
                        shimmer-alpha (if shimmer-phase 0.9 0.4)
                        right-tree (resolve-layout
                                     (build-file-layout content-w content-h current-file agent-output font-size
                                                        shimmer-alpha trail-collapsed
                                                        :active-pane active-pane :char-advance char-advance
                                                        :chat-scroll-y (or chat-scroll-y 0)
                                                        :chat-input chat-input :focus focus))
                        right-rects (mapv #(update % :y + scroll-y) (tree->rects right-tree))
                        right-shadows (mapv #(update % :y + scroll-y) (tree->shadows right-tree))]
                    {:rects (into (vec right-rects) editor-rects)
                     :shadows (vec right-shadows)})
                  (let [line-h (maybe-snap (* font-size (:line-height settings)) dpr snap?)
                        lx (maybe-snap (- layout-x (or scroll-x 0)) dpr snap?)
                        ulx (maybe-snap layout-x dpr snap?)
                        ly (maybe-snap layout-y dpr snap?)]
                    {:rects (compute-editor-rects doc fold-state bracket-match eval-result caret-visible focus
                                                  lx ly line-h gutter-w char-advance content-w
                                                  :gutter-lx ulx)
                     :shadows []}))))]
        {:rects (into (or (:rects sidebar-result) [])
                      (offset-rects* (:rects content-result) sb-w))
         :shadows (into (or (:shadows sidebar-result) [])
                        (offset-shadows* (:shadows content-result) sb-w))}))
    (m/watch !editor-doc)
    <fold-data
    <bracket-data
    (m/watch !eval-result)
    (m/watch !caret-visible)
    (m/watch !focus)
    (m/watch !settings)
    (m/watch !active-font)
    (m/watch !viewport)
    (m/watch !flow-state)
    (m/watch !scroll-y)
    (m/watch !collapsed-groups)
    (m/watch !hovered-row-idx)
    (m/watch !drag-state)
    (m/watch !sidebar-state)
    (m/watch !sidebar-visible)
    (m/watch !current-file)
    (m/watch !extract-preview)
    (m/watch !agent-output)
    (m/watch !shimmer-phase)
    (m/watch !trail-collapsed)
    (m/watch !active-pane)
    (m/watch !scroll-x)
    (m/watch !chat-scroll-y)
    (m/watch !chat-input)
    (m/watch !run-scroll-y)
    (m/watch !detail-scroll-y)))

;; --- Markdown rendering helpers for chat pane trail --------------------------
