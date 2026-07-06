(ns app.client.workspace.editor-compute
  "Editor state computation: event handling, fold/bracket, rect building."
  (:require [clojure.string :as str]
            [missionary.core :as m]
            [app.client.workspace.events :refer [maybe-snap]]
            [app.client.workspace.rect-tree :refer [rt-node resolve-layout tree->rects tree->shadows]]
            [app.client.workspace.runtime.workspace-actions :as ws]
            [app.client.workspace.themes :as themes]
            [app.client.workspace.text-input :as text-input]
            [app.client.workspace.ui-primitives :refer [dt]]
            [app.client.workspace.sidebar :as sidebar :refer [sidebar-w cmd-panel-h status-bar-h build-sidebar-tree derive-effective-sidebar]]
            [app.client.workspace.trail-face.scene :as trail-scene]
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
  [lines folded detect-folds-fn]
  (let [lengths (mapv count lines)
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
  "Derived flow: fold regions + line mapping.
   Dedupes on (:lines doc) so cursor-only moves don't trigger recomputation."
  [!editor-doc !folded-lines detect-folds-fn]
  (m/latest
    (fn [lines folded]
      (compute-fold-state lines folded detect-folds-fn))
    (m/eduction (map :lines) (dedupe) (m/watch !editor-doc))
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
                              {:id [:fold start-line] :z 1
                               :x x :y y :w indicator-size :h indicator-size
                               :r (if is-folded? 0.3 0.7)
                               :g (if is-folded? 0.5 0.6)
                               :b (if is-folded? 0.9 0.3)
                               :a 0.8})))
                        regions)

        ;; Bracket match rects (pre-computed, cached in <bracket-match flow)
        bracket-rects (when bracket-match
                        (keep (fn [[btype {:keys [line col]}]]
                                (when-let [visual-y (logical->visual-y line)]
                                  {:id [:bracket btype] :z 2
                                   :x (+ layout-x (* col char-w))
                                   :y visual-y
                                   :w char-w
                                   :h line-h
                                   :r 0.8 :g 0.6 :b 0.2 :a 0.4}))
                              [[:open (:open bracket-match)] [:close (:close bracket-match)]]))

        ;; Caret rect (only when editor is focused and no selection)
        caret-rect (when (and cursor caret-visible (= focus :editor) (not selection))
                     (when-let [visual-y (logical->visual-y (:line cursor))]
                       {:id :caret :z 4
                        :x (+ layout-x (* (:col cursor) char-w))
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
                                          {:id [:selection logical-line] :z 3
                                           :x x :y visual-y
                                           :w clamped-w :h line-h
                                           :r 0.2 :g 0.4 :b 0.9 :a 0.5}))))
                                  (range (:line s) (inc (:line e))))))

        ;; Current-line highlight (subtle background on cursor's line)
        current-line-rect (when (and cursor (= focus :editor) (not selection))
                            (when-let [visual-y (logical->visual-y (:line cursor))]
                              {:id :current-line :z 0
                               :x 0 :y visual-y :w viewport-w :h line-h
                               :r 1.0 :g 1.0 :b 1.0 :a 0.04}))

        ;; Eval result rect
        eval-rect (when eval-result
                    (let [now (js/Date.now)]
                      (when (< now (:expires-at eval-result))
                        (when-let [visual-y (logical->visual-y (:line eval-result))]
                          (let [line-len (get lengths (:line eval-result) 0)
                                result-x (+ layout-x (* (+ line-len 2) char-w))
                                result-w (* (count (:text eval-result)) char-w)]
                            {:id :eval-result :z 5
                             :x result-x
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

(defn <editor-rects+sidebar
  "Derived flows: editor rectangles + sidebar rectangles (separate).
   Returns {:<editor-rects <flow> :<sidebar <flow>} so sidebar rects
   can be routed to a differential buffer pool instead of the editor rect system.
   Split into scoped sub-flows so each mode only watches its own atoms.
   Caret blink no longer recomputes flow-canvas rects and vice versa."
  [!editor-doc !eval-result !caret-visible !focus !settings !active-font !viewport
   <fold-data <bracket-data
   !flow-state !scroll-y !collapsed-groups !hovered-row-idx !drag-state
   !sidebar-truth !sidebar-overlay !sidebar-ui !sidebar-visible !current-file !effective-local-world !sidebar-scene !extract-preview !agent-output
   !shimmer-phase !trail-collapsed !active-pane !scroll-x !chat-scroll-y !chat-input !run-scroll-y !detail-scroll-y
   !trail-face-state !trail-face-scene !trail-text !trail-feed !trail-bundles !trail-coverage
   compute-ticket-list-rects* compute-run-rects* offset-rects* offset-shadows*
   layout-x layout-y gutter-w]
  (let [;; ── Shared layout context (changes on: resize, settings, font, sidebar toggle) ──
        <layout
        (m/latest
          (fn [viewport settings active-font sidebar-visible? local-world]
            (let [dpr (:dpr viewport)
                  snap? (:snap-to-pixel? settings)
                  font-size (:font-size settings)
                  char-advance (maybe-snap (* font-size (:char-width active-font)) dpr snap?)
                  ;; trail-room R-1 item 7 / W-1 (fixed 2026-07-05 at first
                  ;; light): in a trail face NOTHING else is ambient — the
                  ;; layout consumes the derived judgment, not the raw sidebar
                  ;; atom (which stays untouched, so leaving the trail face
                  ;; restores the sidebar exactly as it was).
                  trail-face? (ws/local-world-trail-face? local-world)
                  sb-vis? (boolean (and sidebar-visible? (not trail-face?)))]
              {:viewport viewport :settings settings :dpr dpr :snap? snap?
               :font-size font-size :char-advance char-advance
               :sb-vis? sb-vis? :sb-w (if sb-vis? sidebar-w 0)}))
          (m/watch !viewport) (m/watch !settings) (m/watch !active-font) (m/watch !sidebar-visible)
          (m/watch !effective-local-world))
        ;; 5 fn args, 5 flows

        ;; ── Mode determination ──
        <mode
        (m/latest
          (fn [local-world extract-preview]
            (cond
              (:rt-node extract-preview) :extract-preview
              :else (ws/local-world-mode local-world)))
          (m/watch !effective-local-world) (m/watch !extract-preview))
        ;; 2 fn args, 2 flows

        ;; ── Sidebar rects (independent of content mode) ──
        ;; Builds + resolves the tree once, caches in !sidebar-scene for hit-testing,
        ;; then extracts rects+shadows for GPU. One tree, two consumers.
        !last-sidebar-struct-hash (atom nil)
        <sidebar
        (m/latest
          (fn [layout sidebar-truth sidebar-overlay sidebar-ui scroll-y]
            (if-not (:sb-vis? layout)
              (do (reset! !sidebar-scene nil) nil)
              (let [t0 (js/performance.now)
                    {:keys [viewport font-size char-advance]} layout
                    sidebar-state (derive-effective-sidebar sidebar-truth sidebar-overlay sidebar-ui)
                    struct-hash (hash [layout sidebar-truth sidebar-overlay (select-keys sidebar-ui [:scroll-y :dir-cache :home-dirs]) scroll-y])
                    structure-changed? (not= struct-hash @!last-sidebar-struct-hash)
                    _ (when structure-changed? (reset! !last-sidebar-struct-hash struct-hash))
                    
                    raw-tree (build-sidebar-tree sidebar-state true
                                                (:height viewport) scroll-y font-size char-advance (:hover-id sidebar-ui))
                    t1 (js/performance.now)
                    tree (resolve-layout raw-tree)
                    t2 (js/performance.now)]
                (when structure-changed?
                  (reset! !sidebar-scene tree))
                (when tree
                  (let [rects (tree->rects tree)
                        t3 (js/performance.now)
                        shadows (tree->shadows tree)
                        t4 (js/performance.now)]
                    (js/console.log "[SIDEBAR-FLOW] build:" (.toFixed (- t1 t0) 1) "ms | resolve:" (.toFixed (- t2 t1) 1) "ms | rects:" (.toFixed (- t3 t2) 1) "ms | shadows:" (.toFixed (- t4 t3) 1) "ms | TOTAL:" (.toFixed (- t4 t0) 1) "ms | rows:" (count (:children (first (:children (last (:children raw-tree)))))))
                    {:rects rects :shadows shadows})))))
          <layout (m/watch !sidebar-truth) (m/watch !sidebar-overlay) (m/watch !sidebar-ui) (m/watch !scroll-y))
        ;; 5 fn args, 5 flows

        ;; ── Trail face scene (view-mvp WP-B2) ──
        ;; Build ONCE per data/viewport change, cache in !trail-face-scene;
        ;; combined_text flattens text ops from it and mouse hit-tests the
        ;; SAME object (gate 13 / trap 1 - the sidebar pattern, NOT the
        ;; chat/flow rebuild-at-click anti-pattern). Scroll rides the
        ;; camera (pan-y = -scroll-y), so the scene is scroll-independent
        ;; and this flow does NOT watch !scroll-y (advisory A4).
        ;; falsification-pass fix: compare the actual input VALUE, not its
        ;; hash — a hash collision would freeze a stale scene forever (the
        ;; "state stuck masking future truth" lifecycle failure).
        !last-trail-struct (atom ::none)
        ;; trail-room R-2 s2.4: the prev-assignment carry lives in a
        ;; SEPARATE post-build cache atom (the !last-trail-struct pattern
        ;; this line sits next to) - NEVER inside !trail-face-state, which
        ;; is a WATCHED input (trap 13: writing per-build output into a
        ;; watched input is a rebuild/re-pull feedback loop). Contract s5
        ;; placed this atom in state.cljs; it lives HERE because threading
        ;; a state.cljs atom to this flow would touch render.cljs (off the
        ;; allowlist) - deviation recorded in BRANCH_REPORT_R2.
        !trail-prev-carry (atom nil)
        <trail-face
        (m/latest
          (fn [layout trail-state trail-text trail-feed trail-bundles coverage]
            (if-not (:face trail-state)
              (do (reset! !trail-face-scene nil)
                  ;; lifecycle clear: leaving the face drops the carry so a
                  ;; later re-entry starts with arrivals, not phantom moves
                  (reset! !trail-prev-carry nil)
                  nil)
              (let [{:keys [viewport font-size char-advance]} layout
                    geom {:viewport-w (:width viewport)
                          :viewport-h (:height viewport)
                          :line-height (js/Math.round (* font-size 1.4))
                          :font-size font-size
                          :char-advance char-advance
                          :card-w 320 :pad 12
                          ;; honest server stamp for staleness, never the wall clock
                          :now-ms (or (:feed/rendered-at-ms trail-feed) 0)}
                    struct [layout trail-state trail-text trail-feed
                            trail-bundles coverage]
                    changed? (not= struct @!last-trail-struct)
                    scene (if changed?
                            (let [s (if (= :text (:face trail-state))
                                      (trail-scene/build-text-face-scene
                                        {:text (or trail-text "")
                                         :address (:address trail-state)
                                         :coverage coverage :geom geom})
                                      (trail-scene/build-timeline-scene
                                        {:feed trail-feed
                                         :bundles trail-bundles
                                         :view-state {:expanded (:expanded trail-state #{})
                                                      :order (:order trail-state :arrival)
                                                      ;; R-2 s2.2: band rides view-state
                                                      :band (:band trail-state 2)}
                                         ;; R-2 s2.4: read the carry at build
                                         :prev @!trail-prev-carry
                                         :coverage coverage :geom geom}))]
                              (reset! !last-trail-struct struct)
                              (reset! !trail-face-scene s)
                              ;; R-2 s2.4: write the carry AFTER the build -
                              ;; this atom is watched by NOTHING (trap 13)
                              (when-let [carry (get-in s [:data :trail-face/carry])]
                                (reset! !trail-prev-carry carry))
                              s)
                            @!trail-face-scene)]
                (when scene
                  {:rects (tree->rects scene)
                   :shadows (tree->shadows scene)}))))
          <layout (m/watch !trail-face-state) (m/watch !trail-text)
          (m/watch !trail-feed) (m/watch !trail-bundles) (m/watch !trail-coverage))
        ;; 6 fn args, 6 flows

        ;; ── Flow canvas rects (intake + run) ──
        ;; ── Intake rects (ticket list) ──
        ;; NOT watching: !shimmer-phase, !agent-output, !trail-collapsed, !run-scroll-y
        <intake-content
        (m/latest
          (fn [layout local-world flow-state scroll-y detail-scroll-y
               hovered-row-idx collapsed-groups drag-state]
            (if-not (ws/local-world-intake? local-world)
              {:rects [] :shadows []}
              (let [{:keys [viewport font-size char-advance sb-w]} layout
                    content-w (- (:width viewport) sb-w)]
                (compute-ticket-list-rects* flow-state content-w (:height viewport)
                                            scroll-y detail-scroll-y hovered-row-idx
                                            collapsed-groups drag-state
                                            font-size char-advance))))
          <layout
          (m/watch !effective-local-world) (m/watch !flow-state) (m/watch !scroll-y) (m/watch !detail-scroll-y)
          (m/watch !hovered-row-idx) (m/watch !collapsed-groups) (m/watch !drag-state))
        ;; 8 fn args, 8 flows

        ;; ── Run rects (agent execution view) ──
        ;; NOT watching: !hovered-row-idx, !collapsed-groups, !drag-state, !detail-scroll-y
        <run-content
        (m/latest
          (fn [layout local-world flow-state scroll-y agent-output shimmer-phase
               trail-collapsed run-scroll-y]
            (if-not (ws/local-world-run? local-world)
              {:rects [] :shadows []}
              (let [{:keys [viewport font-size char-advance sb-w]} layout
                    content-w (- (:width viewport) sb-w)]
                (compute-run-rects* flow-state content-w (:height viewport)
                                    scroll-y agent-output font-size char-advance
                                    shimmer-phase trail-collapsed run-scroll-y))))
          <layout
          (m/watch !effective-local-world) (m/watch !flow-state) (m/watch !scroll-y)
          (m/watch !agent-output) (m/watch !shimmer-phase)
          (m/watch !trail-collapsed) (m/watch !run-scroll-y))
        ;; 8 fn args, 8 flows

        ;; ── Editor/file/extract rects ──
        ;; NOT watching: !hovered-row-idx, !collapsed-groups, !drag-state,
        ;;               !detail-scroll-y, !run-scroll-y
        <editor-content
        (m/latest
          (fn [layout local-world current-file extract-preview
               doc fold-state bracket-match eval-result caret-visible focus
               scroll-y scroll-x
               agent-output shimmer-phase trail-collapsed
               active-pane chat-scroll-y chat-input]
            (let [{:keys [viewport settings dpr snap? font-size char-advance sb-w]} layout
                  content-w (- (:width viewport) sb-w)]
              (cond
                ;; Extract preview
                (:rt-node extract-preview)
                (let [half-w (/ content-w 2)
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

                ;; File open (3-pane)
                (ws/local-world-file-workspace? local-world)
                (let [code-w (int (* content-w (ws/pane-width-pct local-world :main 0.4)))
                      content-h (- (:height viewport) cmd-panel-h status-bar-h)
                      line-h (maybe-snap (* font-size (:line-height settings)) dpr snap?)
                      lx (maybe-snap (- layout-x (or scroll-x 0)) dpr snap?)
                      ly (maybe-snap layout-y dpr snap?)
                      ulx (maybe-snap layout-x dpr snap?)
                      editor-rects (compute-editor-rects doc fold-state bracket-match eval-result
                                                         caret-visible focus lx ly line-h gutter-w
                                                         char-advance code-w
                                                         :gutter-lx ulx)
                      shimmer-alpha (if shimmer-phase 0.9 0.4)
                      right-tree (resolve-layout
                                   (build-file-layout content-w content-h current-file agent-output font-size
                                                      shimmer-alpha trail-collapsed
                                                      :local-world local-world
                                                      :active-pane active-pane :char-advance char-advance
                                                      :chat-scroll-y (or chat-scroll-y 0)
                                                      :chat-input chat-input :focus focus))
                      right-rects (mapv #(update % :y + scroll-y) (tree->rects right-tree))
                      right-shadows (mapv #(update % :y + scroll-y) (tree->shadows right-tree))]
                  {:rects (into (vec right-rects) editor-rects)
                   :shadows (vec right-shadows)})

                ;; Plain editor
                :else
                (let [line-h (maybe-snap (* font-size (:line-height settings)) dpr snap?)
                      lx (maybe-snap (- layout-x (or scroll-x 0)) dpr snap?)
                      ulx (maybe-snap layout-x dpr snap?)
                      ly (maybe-snap layout-y dpr snap?)]
                  {:rects (compute-editor-rects doc fold-state bracket-match eval-result caret-visible focus
                                                lx ly line-h gutter-w char-advance content-w
                                                :gutter-lx ulx)
                   :shadows []}))))
          <layout (m/watch !effective-local-world) (m/watch !current-file) (m/watch !extract-preview)
          (m/watch !editor-doc) <fold-data <bracket-data (m/watch !eval-result)
          (m/watch !caret-visible) (m/watch !focus)
          (m/watch !scroll-y) (m/watch !scroll-x)
          (m/watch !agent-output) (m/watch !shimmer-phase) (m/watch !trail-collapsed)
          (m/watch !active-pane) (m/watch !chat-scroll-y) (m/watch !chat-input))]
        ;; 18 fn args, 18 flows

    ;; ── Return both flows separately ──
    ;; Editor rects (content only, offset by sidebar width) go to the editor pool.
    ;; Sidebar rects go to the sidebar pool.
    {:<editor-rects
     (m/latest
       (fn [mode intake run editor-content trail layout]
         (let [content (case mode
                         :flow-intake intake
                         :flow-run run
                         :trail-text trail
                         :trail-timeline trail
                         editor-content)]
           {:rects (vec (offset-rects* (:rects content) (:sb-w layout)))
            :shadows (vec (offset-shadows* (:shadows content) (:sb-w layout)))}))
       <mode <intake-content <run-content <editor-content <trail-face <layout)
     :<sidebar <sidebar}))

;; --- Markdown rendering helpers for chat pane trail --------------------------
