(ns app.client.workspace.editor-compute
  "Editor state computation: event handling, fold/bracket, rect building."
  (:require [clojure.string :as str]
            [missionary.core :as m]
            [app.client.workspace.events :refer [maybe-snap]]
            [app.client.workspace.rect-tree :refer [rt-node resolve-layout tree->rects tree->shadows]]
            [app.client.workspace.runtime.workspace-actions :as ws]
            [app.client.workspace.themes :as themes]
            [app.client.workspace.text-layout :as tl]
            [app.client.workspace.text-input :as text-input]
            [app.client.workspace.ui-primitives :refer [dt]]
            [app.client.workspace.sidebar :as sidebar :refer [sidebar-w cmd-panel-h status-bar-h build-sidebar-tree derive-effective-sidebar]]
            [app.client.workspace.trail-face.scene :as trail-scene]
            [app.client.workspace.scene-store :as ss]
            [app.client.workspace.scene-runtime :as scene-rt]
            [app.client.workspace.block-edit :as block-edit]
            [app.client.workspace.block-edit-wiring :as block-edit-wiring]
            [app.client.workspace.shell :refer [build-file-layout]]))

;; ============================================================================
;; LAYER 5: COMPONENT UPDATE FLOWS
;; ============================================================================

(defn doc-with-lengths
  "Attach cached per-line lengths to a doc. This is the ONE place :lengths is
   derived: every writer that changes (:lines doc) routes the new doc through
   here, so the keystroke hot path (keyboard) and <bracket-match can read
   (:lengths doc) instead of recomputing (mapv count lines) on every event.
   Cursor-only writers leave :lines (and thus :lengths) untouched; the initial
   doc (state.cljs, out of fence) carries no :lengths and readers fall back
   with (or (:lengths doc) (mapv count …)) — absent-safe, and stale is
   impossible because every :lines change comes back through here."
  [doc]
  (assoc doc :lengths (mapv count (:lines doc))))

(defn editor-apply-event
  "Pure function: apply event to editor doc, returns new doc.
   Line-changing branches refresh the cached :lengths via doc-with-lengths so
   downstream cursor math never recomputes it; movement branches leave :lines
   unchanged and carry :lengths through the merge."
  [doc event line-lengths clipboard]
  (let [input {:lines (:lines doc)
               :cursor (:cursor doc)
               :selection (:selection doc)
               :desired-col (:desired-col doc)}]
    (case (:type event)
      :char
      (let [new-input (text-input/insert-char input (:char event) true)]
        (doc-with-lengths (merge doc new-input {:selection nil})))

      :backspace
      (let [new-input (text-input/delete-backward input true)]
        (doc-with-lengths (merge doc new-input)))

      :delete
      (let [new-input (text-input/delete-forward input true)]
        (doc-with-lengths (merge doc new-input)))

      :enter
      (let [new-input (text-input/insert-char input "\n" true)]
        (doc-with-lengths (merge doc new-input {:selection nil})))

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
          (doc-with-lengths (merge doc new-input)))
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

(def ^:private fold-recompute-throttle-ms
  "Fold detection scans every line, so cap its recompute rate during a typing
   burst instead of rescanning on every char. Fold indicators lag at most this
   long behind a burst — imperceptible, and text is unaffected (line-mapping is
   identity whenever nothing is folded, the common case while typing)."
  150)

(defn- throttle
  "Leading, rate-limited sampling of a continuous flow: emits the first value
   immediately, then at most one value per `dur` ms, always settling on the
   latest within `dur`. Leading (initially ready) is REQUIRED here — <fold-data
   feeds two m/latest text-rect flows, so a trailing debounce would blank the
   editor until its first emit (JVM-probed: this shape yields an m/latest value
   in ~3ms, well inside the window). Defined locally rather than pulled from the
   contrib.missionary-contrib staging namespace (ambiguous resolution) — the
   same local-operator practice global_flow.cljs uses."
  [dur >in]
  (m/ap
    (let [x (m/?> (m/relieve {} >in))]
      (m/amb x (do (m/? (m/sleep dur)) (m/amb))))))

(defn <fold-state
  "Derived flow: fold regions + line mapping.
   Dedupes on (:lines doc) so cursor-only moves don't trigger recomputation,
   and throttles line-changes so a typing burst doesn't rescan folds per char."
  [!editor-doc !folded-lines detect-folds-fn]
  (m/latest
    (fn [lines folded]
      (compute-fold-state lines folded detect-folds-fn))
    (throttle fold-recompute-throttle-ms
              (m/eduction (map :lines) (dedupe) (m/watch !editor-doc)))
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
          ;; reuse the doc's cached line lengths (doc-with-lengths); recomputes
          ;; only for the initial doc, which carries no :lengths yet
          (let [lengths (or (:lengths doc) (mapv count lines))]
            (find-bracket-fn cursor lines lengths)))))
    (m/watch !editor-doc)))

(defn compute-editor-rects
  "Pure function: compute all editor rectangles from PRE-COMPUTED fold state and bracket match.
   No longer calls detect-folds-fn or find-bracket-fn directly — those are cached in separate flows."
  [doc fold-state bracket-match eval-result caret-visible focus
   layout-x layout-y line-h gutter-w char-advance viewport-w
   & {:keys [gutter-lx font-size text-provider]}]
  (let [cursor (:cursor doc)
        selection (:selection doc)

        ;; Use pre-computed fold state (cached, only changes on doc/fold change)
        {:keys [lengths regions folded line-mapping logical->visual]} fold-state

        visual-source-lines (mapv #(get (:lines doc) % "") line-mapping)
        editor-layout (tl/layout {:text (str/join "\n" visual-source-lines)
                                  :source-lines visual-source-lines
                                  :provider text-provider
                                  :font-size (or font-size 14)
                                  :char-advance char-advance
                                  :line-height line-h
                                  :origin [layout-x layout-y]
                                  :line-map line-mapping
                                  :source-id :editor/document
                                  :source-revision (hash [(:lines doc) folded])})

        ;; Helper to get visual y for logical line
        logical->visual-y (fn [logical-line]
                            (when-let [visual-idx (get logical->visual logical-line)]
                              (+ layout-y (* visual-idx line-h))))

        ;; Gutter uses unscrolled x so fold indicators stay fixed
        gutter-x (- (or gutter-lx layout-x) gutter-w)

        ;; Fold indicator rects
        fold-rects (keep (fn [{:keys [start-line]}]
                          (when-let [visual-y (logical->visual-y start-line)]
                            (let [is-folded? (contains? folded start-line)
                                  indicator-size 8
                                  x (+ gutter-x 2)
                                  y (+ visual-y (* 0.5 (- line-h indicator-size)))]
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
                                (when-let [visual-idx (get logical->visual line)]
                                  (merge (:rect (tl/selection-result
                                                  editor-layout visual-idx col (inc col)))
                                         {:id [:bracket btype] :z 2
                                          :r 0.8 :g 0.6 :b 0.2 :a 0.4})))
                              [[:open (:open bracket-match)] [:close (:close bracket-match)]]))

        ;; Caret rect (only when editor is focused and no selection)
        caret-rect (when (and cursor caret-visible (= focus :editor) (not selection))
                     (when-let [visual-idx (get logical->visual (:line cursor))]
                       (merge (:rect (tl/caret-result editor-layout visual-idx (:col cursor)))
                              {:id :caret :z 4
                               :r 0.9 :g 0.9 :b 0.9 :a 1.0})))

        ;; Selection rects
        selection-rects (when selection
                          (let [{:keys [start end]} selection
                                [s e] (if (or (> (:line start) (:line end))
                                              (and (= (:line start) (:line end))
                                                   (> (:col start) (:col end))))
                                        [end start]
                                        [start end])]
                            (mapcat
                              (fn [logical-line]
                                (when-let [visual-idx (get logical->visual logical-line)]
                                  (let [line-len (get lengths logical-line 0)
                                        col-start (if (= logical-line (:line s)) (:col s) 0)
                                        col-end (if (= logical-line (:line e)) (:col e) line-len)
                                        regions (:rects (tl/selection-result
                                                          editor-layout visual-idx
                                                          col-start col-end))]
                                    (keep-indexed
                                      (fn [region-idx {:keys [x w] :as rect}]
                                        (let [clamped-w (min w (max 0 (- viewport-w x)))]
                                          (when (pos? clamped-w)
                                            (merge rect
                                                   {:id [:selection logical-line region-idx]
                                                    :z 3 :w clamped-w
                                                    :r 0.2 :g 0.4 :b 0.9 :a 0.5}))))
                                      regions))))
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
                                visual-idx (get logical->visual (:line eval-result))
                                line-end-x (first (:position (tl/caret-result
                                                              editor-layout visual-idx line-len)))
                                gap-layout (tl/layout {:text "  "
                                                      :provider text-provider
                                                      :font-size (or font-size 14)
                                                      :char-advance char-advance
                                                      :line-height line-h
                                                      :origin [line-end-x visual-y]})
                                result-x (+ line-end-x
                                            (first (get-in (tl/measure-result gap-layout)
                                                           [:metrics :advance])))
                                result-layout (tl/layout {:text (:text eval-result)
                                                         :provider text-provider
                                                         :font-size (or font-size 14)
                                                         :char-advance char-advance
                                                         :line-height line-h
                                                         :origin [result-x visual-y]})
                                result-w (first (get-in (tl/measure-result result-layout)
                                                        [:metrics :advance]))]
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

(def ^:private empty-face-content
  "first-light P1 (trap T6): the editor pool's face-mode contribution — a
   SHARED constant so identical? holds frame-over-frame while the :face-main
   store slot carries the actual face ops."
  {:rects [] :shadows []})

(defn- content-variants [dim bright]
  {:dim dim :bright bright})

(defn- select-shimmer-paint [variants shimmer-phase]
  (get variants (if shimmer-phase :bright :dim)))

(defn- toggle-editor-overlays [variants caret-visible shimmer-phase]
  (let [content (select-shimmer-paint variants shimmer-phase)]
    (if caret-visible
      content
      (update content :rects
              (fn [rects]
                (into [] (remove #(= :caret (:id %))) rects))))))

(defn- note-stable-editor-recompute! []
  ;; G5L probe: set globalThis.__softland_seam_probe_stable=true and reset
  ;; __softland_seam_stable_recomputes to 0 after the editor settles. Blink
  ;; ticks must leave the readable counter at zero for the wearing receipt.
  (when (true? (aget js/globalThis "__softland_seam_probe_stable"))
    (aset js/globalThis "__softland_seam_stable_recomputes"
          (inc (or (aget js/globalThis "__softland_seam_stable_recomputes")
                   0)))))

;; ── first-light P1: the main-face build, DEFERRED off the transport path ──
;; The <trail-face caching shape: build ONCE per input change, compare the
;; input VALUE never a hash (trap T13); !face-scene + these caches are
;; per-build OUTPUT watched by NOTHING (trap T9). Called ONLY from the render
;; consumer's microtask (T4: edge-only mutation; microtasks drain before the
;; browser paints, so the slot lands the same frame as the change).

(defonce ^:private !last-face-struct (atom ::none))
(defonce ^:private !last-slotted (atom nil))

(defn build-main-face!
  "Consume one <face-main bundle: overlay the edit UI onto the served context
   (trap T5), build the RESOLVED address-stamped tree (ss/build-face-tree —
   both Δ1 stamps, trap T7), and upsert/close the :face-main store slot.
   Value-compared build cache + identity-compared slot write, so an unchanged
   bundle is a no-op. Idempotent; last-write-wins under coalescing."
  [{:keys [layout face-state face-context compiled edit-st truth-overlay
           !face-scene]}]
  (if-not (and (:face face-state) compiled)
    (do (reset! !face-scene nil)
        (reset! !last-face-struct ::none)
        (when @!last-slotted
          (reset! !last-slotted nil)
          (scene-rt/close-main-face!)))
    (let [{:keys [viewport font-size char-advance text-provider face-mode?]} layout
          geom {:viewport-w   (:width viewport)
                :viewport-h   (:height viewport)
                ;; text-runs wrap at content-w; the root assembly's padding
                ;; offsets x, so inset the wrap width to keep prose off the
                ;; right edge (lane A note: geom threads down unchanged, §5)
                :content-w    (- (:width viewport) 32)
                :line-height  (js/Math.round (* font-size 1.4))
                :font-size    font-size
                :char-advance char-advance
                :text-provider text-provider
                ;; honest server stamp, never the wall clock (§5)
                :now-ms       (or (:face/rendered-at-ms face-context) 0)}
          ;; block-write INT: the focused block's pending-input (buffer
          ;; text+caret, ONE value — BW-T6/L8), refusal notices (G5), and the
          ;; §5 single-unit truth overlay enter the projection BEFORE
          ;; interpretation — trap T5: the overlay survives the flip because
          ;; it enters the tree the slot is built from.
          face-context (block-edit/overlay-face-context
                         face-context edit-st truth-overlay)
          ;; compiled compares by identity inside the value compare
          ;; (it holds closures; it only changes by /face reset!)
          struct [layout face-state face-context compiled]
          changed? (not= struct @!last-face-struct)
          scene (if changed?
                  (let [uids (ss/block-unit-ids face-context)
                        s (ss/build-face-tree
                            compiled face-context
                            {:view-instance :face-main
                             :address (or (:conversation/address face-context)
                                          (:address face-state))
                             :geom geom}
                            uids)]
                    (reset! !last-face-struct struct)
                    (reset! !face-scene s)
                    s)
                  @!face-scene)
          slotted (when face-mode? scene)]
      (when-not (identical? slotted @!last-slotted)
        (reset! !last-slotted slotted)
        (if slotted
          (scene-rt/upsert-main-face! slotted)
          (scene-rt/close-main-face!))))))

(defn <editor-rects+sidebar
  "Derived flows: editor rectangles + sidebar rectangles (separate), plus the
   main-face tree flow (first-light P1). Returns {:<editor-rects <flow>
   :<sidebar <flow> :<face-main <flow>} — sidebar rects route to a differential
   buffer pool; the main-face tree routes to its dedicated store-slot consumer
   edge in render.cljs. Split into scoped sub-flows so each mode only watches
   its own atoms. Caret blink no longer recomputes flow-canvas rects and vice
   versa."
  [!editor-doc !eval-result !caret-visible !focus !settings !active-font !viewport
   <fold-data <bracket-data
   !flow-state !scroll-y !collapsed-groups !hovered-row-idx !drag-state
   !sidebar-truth !sidebar-overlay !sidebar-ui !sidebar-visible !current-file !effective-local-world !sidebar-scene !extract-preview !agent-output !face-list
   !shimmer-phase !trail-collapsed !active-pane !scroll-x !chat-scroll-y !chat-input !run-scroll-y !detail-scroll-y
   !trail-face-state !trail-face-scene !trail-text !trail-feed !trail-bundles !trail-coverage
   !face-state !face-context !face-compiled !face-scene
   compute-ticket-list-rects* compute-run-rects* offset-rects* offset-shadows*
   layout-x layout-y gutter-w]
  (let [;; ── Shared layout context (changes on: resize, settings, font, sidebar toggle) ──
        <layout
        ;; SEAM-STEP1 T5: G4 proved b.46 shares one process, replays latest,
        ;; stops at zero subscribers, and restarts on resubscribe. This pure
        ;; derivation is the real sharing point; side-effecting siblings stay raw.
        (m/signal
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
                  ;; the assembly-hosted face inherits the trail-face ground
                  ;; rule: nothing else is ambient (framework W1-INT)
                  face-mode? (ws/local-world-face-assembly? local-world)
                  trail-face? (or (ws/local-world-trail-face? local-world)
                                  face-mode?)
                  sb-vis? (boolean (and sidebar-visible? (not trail-face?)))]
              {:viewport viewport :settings settings :dpr dpr :snap? snap?
               :font-size font-size :char-advance char-advance
               :text-provider (:layout-provider active-font)
               ;; first-light P1: the face flow gates its emission on the mode
               ;; through THIS derived map — one source, no second
               ;; local-world watch beside <layout (L8: no diamond).
               :face-mode? face-mode?
               :sb-vis? sb-vis? :sb-w (if sb-vis? sidebar-w 0)}))
          (m/watch !viewport) (m/watch !settings) (m/watch !active-font) (m/watch !sidebar-visible)
          (m/watch !effective-local-world)))
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
        ;; G26 fix (render LOW): compare the input VALUE, not its hash — the
        ;; sibling trail/face flows already do (trap T13: a hash collision
        ;; freezes a stale HIT-TEST scene while the drawn rects move on)
        !last-sidebar-struct (atom ::none)
        <sidebar
        (m/latest
          (fn [layout sidebar-truth sidebar-overlay sidebar-ui scroll-y face-list]
            (if-not (:sb-vis? layout)
              (do (reset! !sidebar-scene nil) nil)
              (let [t0 (js/performance.now)
                    {:keys [viewport font-size char-advance]} layout
                    ;; W2 (CONTRACT §16): the FACES roster joins the sidebar
                    ;; state — from Rama via :!face-list (trap T14)
                    sidebar-state (derive-effective-sidebar sidebar-truth sidebar-overlay sidebar-ui face-list)
                    struct [layout sidebar-truth sidebar-overlay (select-keys sidebar-ui [:scroll-y :dir-cache :home-dirs]) scroll-y face-list]
                    structure-changed? (not= struct @!last-sidebar-struct)
                    _ (when structure-changed? (reset! !last-sidebar-struct struct))
                    
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
          <layout (m/watch !sidebar-truth) (m/watch !sidebar-overlay) (m/watch !sidebar-ui) (m/watch !scroll-y) (m/watch !face-list))
        ;; 6 fn args, 6 flows

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

        ;; ── Assembly-hosted face → the MAIN-FACE STORE SLOT (first-light P1,
        ;; the P3c minimum flip) ──
        ;; The <trail-face shape for caching: build ONCE per input change,
        ;; cache in !face-scene; compare the input VALUE, never a hash
        ;; (trap T13 — a collision would freeze a stale scene forever).
        ;; !face-scene is per-build OUTPUT cached in an atom watched by
        ;; NOTHING (trap T9). Interpretation cost lives on the data-change
        ;; path, never the frame path (trap T3). Scroll rides the camera
        ;; (§10 SLOT-C): the scene is scroll-independent, this flow does NOT
        ;; watch !scroll-y.
        ;;
        ;; FLIPPED: this flow no longer emits GPU ops for the editor pool —
        ;; it emits the RESOLVED, ADDRESS-STAMPED tree (ss/build-face-tree:
        ;; root Δ1 stamp + per-block [:data :address], trap T7) for the
        ;; dedicated consumer edge in render.cljs, which upserts it as the
        ;; :face-main store slot in the SAME propagation wave (T4 edge-only
        ;; mutation without a +1-frame paint lag). The edit overlay
        ;; (pending-input/caret/refusal + truth overlay) is threaded onto the
        ;; projection BEFORE interpretation, exactly as before — trap T5: the
        ;; overlay survives the flip because it enters the tree the slot is
        ;; built from. Emission is nil outside face mode (layout :face-mode?)
        ;; so mode exits close the slot and re-entries re-land it.
        <face-main
        (m/latest
          (fn [layout face-state face-context compiled edit-st truth-overlay]
            ;; CHEAP bundle only — the build is DEFERRED to a microtask at the
            ;; consumer edge (build-main-face!). Rationale (G1 drill, measured):
            ;; building synchronously inside this combine put ~7-15ms of work
            ;; into EVERY keystroke/decision/truth propagation turn, delaying
            ;; the edit envelope's dispatch and the echo's processing — narrow
            ;; echo p50 went 20→52ms. A microtask still completes before the
            ;; browser paints (no +1 frame), but off the transport path.
            {:layout layout :face-state face-state :face-context face-context
             :compiled compiled :edit-st edit-st :truth-overlay truth-overlay
             :!face-scene !face-scene})
          <layout (m/watch !face-state) (m/watch !face-context)
          (m/watch !face-compiled)
          (m/watch block-edit-wiring/!edit-state)
          (m/watch block-edit-wiring/!truth-overlay))
        ;; 6 fn args, 6 flows (block-write INT: +edit-state +truth-overlay)

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
        <run-content-stable
        (m/latest
          (fn [layout local-world flow-state scroll-y agent-output
               trail-collapsed run-scroll-y]
            (if-not (ws/local-world-run? local-world)
              (content-variants empty-face-content empty-face-content)
              (let [{:keys [viewport font-size char-advance sb-w]} layout
                    content-w (- (:width viewport) sb-w)]
                ;; SEAM-STEP1 T4: geometry is derived once per stable input
                ;; generation; the downstream overlay only selects paint alpha.
                (content-variants
                 (compute-run-rects* flow-state content-w (:height viewport)
                                     scroll-y agent-output font-size char-advance
                                     false trail-collapsed run-scroll-y)
                 (compute-run-rects* flow-state content-w (:height viewport)
                                     scroll-y agent-output font-size char-advance
                                     true trail-collapsed run-scroll-y)))))
          <layout
          (m/watch !effective-local-world) (m/watch !flow-state) (m/watch !scroll-y)
          (m/watch !agent-output)
          (m/watch !trail-collapsed) (m/watch !run-scroll-y))
        ;; 7 fn args, 7 flows

        <run-content
        (m/latest
         (fn [stable shimmer-phase]
           (select-shimmer-paint stable shimmer-phase))
         <run-content-stable (m/watch !shimmer-phase))
        ;; 2 fn args, 2 flows

        ;; ── Editor/file/extract rects ──
        ;; NOT watching: !hovered-row-idx, !collapsed-groups, !drag-state,
        ;;               !detail-scroll-y, !run-scroll-y
        <editor-content-stable
        (m/latest
          (fn [layout local-world current-file extract-preview
               doc fold-state bracket-match eval-result focus
               scroll-y scroll-x
               agent-output trail-collapsed
               active-pane chat-scroll-y chat-input]
            (note-stable-editor-recompute!)
            (let [{:keys [viewport settings dpr snap? font-size char-advance
                          text-provider sb-w]} layout
                  content-w (- (:width viewport) sb-w)
                  mode (ws/local-world-mode local-world)]
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
                                                           (assoc-in rt [:bounds :w] (- half-w 32))])))
                      content {:rects (if preview-tree (tree->rects preview-tree) [])
                               :shadows (if preview-tree (tree->shadows preview-tree) [])}]
                  (content-variants content content))

                ;; This eager branch remains subscribed in every workspace mode.
                ;; Do not run the hidden editor's proportional layout on caret/
                ;; shimmer ticks while a flow, trail, or face owns the surface;
                ;; the selected mode-specific branch below supplies those rects.
                (not (contains? #{:editor :file-workspace} mode))
                (content-variants empty-face-content empty-face-content)

                ;; File open (3-pane)
                (ws/local-world-file-workspace? local-world)
                (let [code-w (int (* content-w (ws/pane-width-pct local-world :main 0.4)))
                      content-h (- (:height viewport) cmd-panel-h status-bar-h)
                      line-h (maybe-snap (* font-size (:line-height settings)) dpr snap?)
                      lx (maybe-snap (- layout-x (or scroll-x 0)) dpr snap?)
                      ly (maybe-snap layout-y dpr snap?)
                      ulx (maybe-snap layout-x dpr snap?)
                      editor-rects (compute-editor-rects doc fold-state bracket-match eval-result
                                                         true focus lx ly line-h gutter-w
                                                         char-advance code-w
                                                         :gutter-lx ulx
                                                         :font-size font-size
                                                         :text-provider text-provider)
                      build-paint
                      (fn [shimmer-alpha]
                        (let [right-tree
                              (resolve-layout
                               (build-file-layout content-w content-h current-file agent-output font-size
                                                  shimmer-alpha trail-collapsed
                                                  :local-world local-world
                                                  :active-pane active-pane :char-advance char-advance
                                                  :chat-scroll-y (or chat-scroll-y 0)
                                                  :chat-input chat-input :focus focus))
                              right-rects (mapv #(update % :y + scroll-y)
                                                (tree->rects right-tree))
                              right-shadows (mapv #(update % :y + scroll-y)
                                                  (tree->shadows right-tree))]
                          {:rects (into (vec right-rects) editor-rects)
                           :shadows (vec right-shadows)}))]
                  (content-variants (build-paint 0.4) (build-paint 0.9)))

                ;; Plain editor
                :else
                (let [line-h (maybe-snap (* font-size (:line-height settings)) dpr snap?)
                      lx (maybe-snap (- layout-x (or scroll-x 0)) dpr snap?)
                      ulx (maybe-snap layout-x dpr snap?)
                      ly (maybe-snap layout-y dpr snap?)
                      content
                      {:rects (compute-editor-rects doc fold-state bracket-match eval-result true focus
                                                   lx ly line-h gutter-w char-advance content-w
                                                   :gutter-lx ulx
                                                   :font-size font-size
                                                   :text-provider text-provider)
                       :shadows []}]
                  (content-variants content content)))))
          <layout (m/watch !effective-local-world) (m/watch !current-file) (m/watch !extract-preview)
          (m/watch !editor-doc) <fold-data <bracket-data (m/watch !eval-result)
          (m/watch !focus)
          (m/watch !scroll-y) (m/watch !scroll-x)
          (m/watch !agent-output) (m/watch !trail-collapsed)
          (m/watch !active-pane) (m/watch !chat-scroll-y) (m/watch !chat-input))
        ;; 16 fn args, 16 flows

        <editor-content
        ;; SEAM-STEP1 T4: a chain, not a raw-m/latest diamond. Ticker inputs
        ;; consume the stable stage's one output and only toggle paint/presence.
        (m/latest
         (fn [stable caret-visible shimmer-phase]
           (toggle-editor-overlays stable caret-visible shimmer-phase))
         <editor-content-stable
         (m/watch !caret-visible)
         (m/watch !shimmer-phase))]
        ;; 3 fn args, 3 flows

    ;; ── Return the flows separately ──
    ;; Editor rects (content only, offset by sidebar width) go to the editor pool.
    ;; Sidebar rects go to the sidebar pool. The main-face tree flow goes to its
    ;; dedicated consumer edge (render.cljs) — the store slot is its only sink.
    {:<editor-rects
     (m/latest
       (fn [mode intake run editor-content trail layout]
         (if (= mode :face-assembly)
           ;; first-light P1 (trap T6): the worn face's rects/shadows now ride
           ;; its :face-main store slot — the editor pool contributes NOTHING
           ;; in face mode. A shared constant, so editor-rects stays identical?
           ;; frame-over-frame and the pool never churns on face edits.
           empty-face-content
           (let [content (case mode
                           :flow-intake intake
                           :flow-run run
                           :trail-text trail
                           :trail-timeline trail
                           editor-content)]
             {:rects (vec (offset-rects* (:rects content) (:sb-w layout)))
              :shadows (vec (offset-shadows* (:shadows content) (:sb-w layout)))})))
       <mode <intake-content <run-content <editor-content <trail-face <layout)
     :<sidebar <sidebar
     :<face-main <face-main}))

;; --- Markdown rendering helpers for chat pane trail --------------------------
