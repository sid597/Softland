(ns app.client.workspace.runtime.render
  "Render consumer: derived flow assembly, world snapshot, GPU upload diffing, draw."
  (:require [missionary.core :as m]
            [app.client.substrate.webgpu.renderer :as editor]
            [app.client.substrate.webgpu.buffer-pool :as pool]
            [app.client.workspace.events :refer [maybe-snap]]
            [app.client.workspace.runtime.workspace-actions :as ws]
            [app.client.workspace.sidebar :refer [cmd-panel-h status-bar-h]]
            [app.client.workspace.editor-compute :refer [<fold-state <bracket-match <editor-rects+sidebar]]
            [app.client.workspace.combined-text :refer [<combined-text-ops]]
            [app.client.workspace.cmd-panel :refer [<cmd-panel-rects]]
            [app.client.workspace.settings-view :refer [<settings-panel-rects <settings-panel-text]]
            [app.client.workflows.dg-flow :as dg]))

(defn render-consumer
  "Missionary consumer: assemble derived flows, build world snapshot, diff-upload to GPU, draw on RAF."
  [{:keys [!editor-doc !cmd-panel !ai-provider !agent-output !agent-scroll-y !scroll-y
           !viewport !settings !active-font !current-file !effective-local-world !flow-state !collapsed-groups
           !hovered-row-idx !drag-state !sidebar-truth !sidebar-overlay !sidebar-ui !sidebar-visible !sidebar-scene !extract-preview
           !shimmer-phase !trail-collapsed !active-pane !scroll-x !chat-scroll-y !chat-input
           !focus !run-scroll-y !detail-scroll-y !eval-result !caret-visible !folded-lines
           !font-manifest !font-assets !text-geo !cmd-rect-sys !settings-rect-sys
           !sidebar-pool !editor-pool !editor-shadow-pool !sidebar-shadow-pool]
    :as atoms}
   {:keys [layout-x layout-y gutter-w]}
   {:keys [device ctx geometry atlas]}
   {:keys [tokenize-fn layout-fn detect-folds-fn find-bracket-fn]}]
  (let [;; Dirty-present RAF: only fires when world-snapshot changes (Phase 6E)
        !request-frame (volatile! nil)
        >dirty-raf
        (m/observe
          (fn [!]
            (let [pending? (volatile! false)
                  raf-id (volatile! nil)
                  request! (fn []
                             (when-not @pending?
                               (vreset! pending? true)
                               (vreset! raf-id
                                 (js/requestAnimationFrame
                                   (fn [t]
                                     (vreset! pending? false)
                                     (vreset! raf-id nil)
                                     (! t))))))]
              (vreset! !request-frame request!)
              (request!)  ;; ensure first frame renders
              #(do (vreset! !request-frame nil)
                   (when-let [id @raf-id]
                     (js/cancelAnimationFrame id))))))

        ;; Derived flows
        <fold-data    (<fold-state !editor-doc !folded-lines detect-folds-fn)
        <bracket-data (<bracket-match !editor-doc find-bracket-fn)

        <text-data (<combined-text-ops
                      !editor-doc !cmd-panel !ai-provider !agent-output !agent-scroll-y !scroll-y !viewport !settings !active-font
                      !current-file !effective-local-world
                      tokenize-fn layout-fn
                      <fold-data
                      !flow-state !collapsed-groups !hovered-row-idx !drag-state
                      !sidebar-visible !sidebar-scene !extract-preview
                      !shimmer-phase !trail-collapsed !active-pane !scroll-x !chat-scroll-y !chat-input !focus !run-scroll-y !detail-scroll-y
                      dg/compute-ticket-list-text-ops dg/compute-run-text-ops dg/offset-text-ops
                      layout-x layout-y cmd-panel-h status-bar-h)

        ;; Split: editor rects (content only) + sidebar rects (for pool)
        {<editor-rect-flow :<editor-rects
         <sidebar-flow     :<sidebar}
        (<editor-rects+sidebar
          !editor-doc !eval-result !caret-visible !focus
          !settings !active-font !viewport
          <fold-data <bracket-data
          !flow-state !scroll-y !collapsed-groups !hovered-row-idx !drag-state
          !sidebar-truth !sidebar-overlay !sidebar-ui !sidebar-visible !current-file !effective-local-world !sidebar-scene !extract-preview !agent-output
          !shimmer-phase !trail-collapsed !active-pane !scroll-x !chat-scroll-y !chat-input !run-scroll-y !detail-scroll-y
          dg/compute-ticket-list-rects dg/compute-run-rects dg/offset-rects dg/offset-shadows
          layout-x layout-y gutter-w)

        <editor-rect-data <editor-rect-flow

        <cmd-rect-data (<cmd-panel-rects
                          !cmd-panel !focus !caret-visible !scroll-y !viewport
                          !settings !active-font
                          !ai-provider !agent-output !sidebar-visible
                          !effective-local-world
                          cmd-panel-h status-bar-h)

        <settings-rect-data (<settings-panel-rects !settings !focus !viewport !scroll-y !font-manifest)
        <settings-text-data (<settings-panel-text !settings !viewport !scroll-y !font-manifest)

        ;; World snapshot (now includes sidebar data for pool updates)
        <world-snapshot (m/latest
                          (fn [text-data editor-rect-data sidebar-data
                               cmd-rects settings-rects settings-text
                               viewport scroll-y cmd-panel settings active-font agent-output
                               local-world]
                            (when-let [req @!request-frame] (req))
                            {:text-data text-data
                             :editor-rect-data editor-rect-data
                             :sidebar-data sidebar-data
                             :cmd-rects cmd-rects
                             :settings-rects settings-rects
                             :settings-text settings-text
                             :viewport viewport
                             :scroll-y scroll-y
                             :cmd-visible (or (:visible cmd-panel)
                                              (ws/local-world-file-workspace? local-world)
                                              (ws/local-world-flow? local-world))
                             :agent-visible (some? (:status agent-output))
                             :settings-visible (:visible settings)
                             :font-size (:font-size settings)
                             :px-range (:px-range settings)
                             :line-height (:line-height settings)
                             :sharpness (:sharpness settings)
                             :snap-to-pixel? (:snap-to-pixel? settings)
                             :show-diagnostics? (:show-diagnostics? settings)
                             :char-width (:char-width active-font)})
                          <text-data
                          <editor-rect-data
                          <sidebar-flow
                          <cmd-rect-data
                          <settings-rect-data
                          <settings-text-data
                          (m/watch !viewport)
                          (m/watch !scroll-y)
                          (m/watch !cmd-panel)
                          (m/watch !settings)
                          (m/watch !active-font)
                          (m/watch !agent-output)
                          (m/watch !effective-local-world))]

    ;; Render pulse: sample world on each RAF tick
    (m/reduce
      (fn [prev-state [world _frame-time]]
        (if (identical? world (:prev-world prev-state))
          prev-state

          (let [raf-t0 (js/performance.now)
                {:keys [text-data editor-rect-data sidebar-data cmd-rects settings-rects settings-text
                        viewport scroll-y cmd-visible agent-visible settings-visible
                        font-size px-range line-height sharpness char-width
                        snap-to-pixel? show-diagnostics?]} world
                editor-rects   (:rects editor-rect-data)
                editor-shadows (:shadows editor-rect-data)

                ;; Differential sidebar pool update — keyed by identity (Phase 5)
                sidebar-rects (or (:rects sidebar-data) [])
                _sidebar-diff (when-not (identical? sidebar-data (:prev-sidebar-data prev-state))
                                (let [has-ids? (some :id sidebar-rects)
                                      t0 (js/performance.now)
                                      result (if has-ids?
                                               (pool/keyed-diff-update-pool! !sidebar-pool sidebar-rects)
                                               (do (pool/batch-update-pool! !sidebar-pool sidebar-rects) nil))
                                      t1 (js/performance.now)]
                                  (when (and result (pos? (+ (:added result) (:updated result) (:freed result))))
                                    (js/console.log "[SIDEBAR-POOL] keyed-diff:"
                                                    (.toFixed (- t1 t0) 2) "ms |"
                                                    "added:" (:added result)
                                                    "updated:" (:updated result)
                                                    "freed:" (:freed result)
                                                    "writes:" (:total-writes result)
                                                    "rects:" (count sidebar-rects)))))
                dpr (:dpr viewport)
                snap? (not (false? snap-to-pixel?))
                line-h (maybe-snap (* font-size line-height) dpr snap?)
                snap-step (when snap? (/ 1 (or dpr 1)))

                font-assets @!font-assets
                prev-font-id (:prev-font-id prev-state)
                font-changed? (not= (:id font-assets) prev-font-id)

                ;; Font texture update — both text systems share pipeline but have separate bind-groups
                [updated-content-geo updated-chrome-geo]
                (if (and font-changed? (:bitmap font-assets))
                  (do (js/console.log "[RENDER] Updating font texture for:" (:id font-assets))
                      [(editor/update-font-texture device (:content-text-geo prev-state) (:bitmap font-assets))
                       (editor/update-font-texture device (:chrome-text-geo prev-state) (:bitmap font-assets))])
                  [(:content-text-geo prev-state) (:chrome-text-geo prev-state)])

                active-atlas (or (:atlas font-assets) atlas)

                ;; Resize render target if viewport changed (Phase 6E)
                ;; Render target is always at physical pixels (CSS * dpr)
                prev-rt (:render-target prev-state)
                phys-w (Math/floor (* (:width viewport) dpr))
                phys-h (Math/floor (* (:height viewport) dpr))
                rt-resized? (or (not= phys-w (:width prev-rt))
                                (not= phys-h (:height prev-rt)))
                render-target (if rt-resized?
                                (do (editor/destroy-render-target! prev-rt)
                                    (editor/create-render-target device
                                      phys-w phys-h
                                      (:format (:pipelines geometry))))
                                prev-rt)

                ;; Common rendering settings check
                settings-same? (and (= font-size (:prev-font-size prev-state))
                                    (= px-range (:prev-px-range prev-state))
                                    (= line-h (:prev-line-height prev-state))
                                    (= sharpness (:prev-sharpness prev-state))
                                    (= char-width (:prev-char-width prev-state))
                                    (= snap-step (:prev-snap-step prev-state))
                                    (not font-changed?))

                ;; ── Content text (editor + sidebar — the bulk) ──
                content-ops (:content-ops text-data)
                content-same? (and (identical? content-ops (:prev-content-ops prev-state))
                                   settings-same?)

                raf-t1 (js/performance.now)
                new-content-geo (if content-same?
                                  updated-content-geo
                                  (editor/update-text-data device updated-content-geo
                                                           content-ops active-atlas font-size
                                                           :px-range px-range
                                                           :line-height line-h
                                                           :char-width char-width
                                                           :snap-step snap-step
                                                           :sharpness sharpness))

                ;; ── Chrome text (cmd + agent + status + settings + diagnostics) ──
                chrome-ops (:chrome-ops text-data)
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
                                     [{:text diag-text :type :comment
                                       :from 0 :to (count diag-text)
                                       :x diag-x :y diag-y :size diag-size
                                       :r 0.7 :g 0.7 :b 0.7 :a 1.0}]))
                chrome-base-count (count (or chrome-ops []))
                full-chrome-ops (vec (concat (or chrome-ops [])
                                            (or settings-lines [])
                                            diagnostics-line))
                chrome-same? (and (identical? chrome-ops (:prev-chrome-ops prev-state))
                                  (identical? settings-text (:prev-settings-text prev-state))
                                  (= settings-visible (:prev-settings-visible prev-state))
                                  (= show-diagnostics? (:prev-show-diagnostics prev-state))
                                  (= scroll-y (:prev-scroll-y prev-state))
                                  settings-same?)

                new-chrome-geo (if chrome-same?
                                 updated-chrome-geo
                                 (editor/update-text-data device updated-chrome-geo
                                                          full-chrome-ops active-atlas font-size
                                                          :px-range px-range
                                                          :line-height line-h
                                                          :char-width char-width
                                                          :snap-step snap-step
                                                          :sharpness sharpness))

                raf-t2 (js/performance.now)
                editor-line-count (:editor-line-count text-data)
                settings-line-count (count (or settings-lines []))
                diagnostics-line-index (when diagnostics-line
                                         (+ chrome-base-count settings-line-count))

                ;; Content geo for hit-testing (mouse uses line-mapping + editor-line-count)
                new-text-geo (assoc new-content-geo
                                    :line-mapping (:line-mapping text-data)
                                    :editor-line-count editor-line-count)

                ;; Differential editor pool update — ordered keyed diff (Phase 6A)
                _editor-pool-diff (when-not (identical? editor-rects (:prev-editor-rects prev-state))
                                    (let [t0 (js/performance.now)
                                          result (pool/ordered-diff-update-pool! !editor-pool editor-rects)
                                          t1 (js/performance.now)]
                                      (when (pos? (:total-writes result))
                                        (js/console.log "[EDITOR-POOL] ordered-diff:"
                                                        (.toFixed (- t1 t0) 2) "ms |"
                                                        "added:" (:added result)
                                                        "updated:" (:updated result)
                                                        "freed:" (:freed result)
                                                        "writes:" (:total-writes result)
                                                        "rects:" (count editor-rects)))))

                ;; Per-source shadow pools (Phase 6C) — independent diffs, no cross-source shifts
                _editor-shadow-diff
                (when-not (identical? editor-shadows (:prev-editor-shadows prev-state))
                  (let [shadows (or editor-shadows [])
                        t0 (js/performance.now)
                        writes (pool/batch-update-pool! !editor-shadow-pool shadows)
                        t1 (js/performance.now)]
                    (when (pos? writes)
                      (js/console.log "[EDITOR-SHADOW-POOL] batch-diff:"
                                      (.toFixed (- t1 t0) 2) "ms |"
                                      "writes:" writes
                                      "shadows:" (count shadows)))))
                _sidebar-shadow-diff
                (when-not (identical? sidebar-data (:prev-sidebar-data prev-state))
                  (let [shadows (or (:shadows sidebar-data) [])
                        t0 (js/performance.now)
                        writes (pool/batch-update-pool! !sidebar-shadow-pool shadows)
                        t1 (js/performance.now)]
                    (when (pos? writes)
                      (js/console.log "[SIDEBAR-SHADOW-POOL] batch-diff:"
                                      (.toFixed (- t1 t0) 2) "ms |"
                                      "writes:" writes
                                      "shadows:" (count shadows)))))

                new-cmd-sys (if (not (identical? cmd-rects (:prev-cmd-rects prev-state)))
                              (editor/update-rects device
                                                   (or (:cmd-rect-sys prev-state) @!cmd-rect-sys)
                                                   (or cmd-rects []))
                              (:cmd-rect-sys prev-state))

                new-settings-sys (if (not (identical? settings-rects (:prev-settings-rects prev-state)))
                                   (editor/update-rects device
                                                        (or (:settings-rect-sys prev-state) @!settings-rect-sys)
                                                        (or settings-rects []))
                                   (:settings-rect-sys prev-state))]

            (reset! !text-geo new-text-geo)

            ;; ── Dirty-present metadata (Phase 6E) ─────────────────────
            ;; Collect which subsystems changed to compute dirty rect for scissored redraw
            ;; All rects in PHYSICAL pixels (CSS * dpr) for setScissorRect
            (let [sidebar-dirty? (not (identical? sidebar-data (:prev-sidebar-data prev-state)))
                  editor-rects-dirty? (not (identical? editor-rects (:prev-editor-rects prev-state)))
                  editor-shadows-dirty? (not (identical? editor-shadows (:prev-editor-shadows prev-state)))
                  cmd-dirty? (not (identical? cmd-rects (:prev-cmd-rects prev-state)))
                  settings-dirty? (not (identical? settings-rects (:prev-settings-rects prev-state)))
                  first-frame? (nil? (:prev-world prev-state))
                  ;; Physical pixel dimensions
                  pw phys-w
                  ph phys-h
                  sb-w (* dpr (or (:sb-w (:line-mapping text-data)) 0))
                  chrome-h (* dpr (+ cmd-panel-h status-bar-h))
                  ;; Per-subsystem screen regions (physical pixels)
                  ;; Sidebar: left strip
                  ;; Editor: right of sidebar, above chrome
                  ;; Chrome: bottom strip (cmd panel + status bar)
                  ;; Settings: full viewport (overlay, conservative)
                  dirty-rect
                  (cond
                    ;; First frame, resize, or settings/font changed → full clear, no scissor
                    (or first-frame? rt-resized? (not settings-same?) font-changed?)
                    nil

                    ;; Content or chrome text changed → full viewport (text spans everything)
                    (or (not content-same?) (not chrome-same?))
                    nil

                    ;; Only rects/shadows changed — union per-subsystem regions
                    (or sidebar-dirty? editor-rects-dirty? editor-shadows-dirty?
                        cmd-dirty? settings-dirty?)
                    (let [regions (cond-> []
                                   sidebar-dirty?
                                   (conj {:x 0 :y 0 :w (max 1 sb-w) :h ph})
                                   (or editor-rects-dirty? editor-shadows-dirty?)
                                   (conj {:x sb-w :y 0 :w (- pw sb-w) :h (- ph chrome-h)})
                                   cmd-dirty?
                                   (conj {:x 0 :y (- ph chrome-h) :w pw :h chrome-h})
                                   settings-dirty?
                                   (conj {:x 0 :y 0 :w pw :h ph}))]
                      (when (seq regions)
                        (let [x1 (apply min (map :x regions))
                              y1 (apply min (map :y regions))
                              x2 (apply max (map #(+ (:x %) (:w %)) regions))
                              y2 (apply max (map #(+ (:y %) (:h %)) regions))]
                          {:x x1 :y y1 :w (- x2 x1) :h (- y2 y1)})))

                    ;; Nothing changed (shouldn't happen — world identity check should have caught this)
                    :else nil)

                  raf-t3 (js/performance.now)]  ;; before draw
            (editor/draw-frame! device ctx
                                new-content-geo (pool/pool-draw-info !editor-pool) new-cmd-sys
                                (:camera-floats (:pipelines geometry))
                                (:pass-descriptor (:pipelines geometry))
                                0 (- scroll-y)
                                (:width viewport) (:height viewport)
                                :cmd-panel-visible cmd-visible
                                :cmd-panel-h cmd-panel-h
                                :chrome-text-sys new-chrome-geo
                                :chrome-base-line-count chrome-base-count
                                :settings-line-count settings-line-count
                                :settings-visible settings-visible
                                :settings-rect-sys new-settings-sys
                                :diagnostics-visible show-diagnostics?
                                :diagnostics-line-index diagnostics-line-index
                                :agent-visible agent-visible
                                :editor-shadow-pool-info (pool/pool-draw-info !editor-shadow-pool)
                                :sidebar-shadow-pool-info (pool/pool-draw-info !sidebar-shadow-pool)
                                :sidebar-pool-info (pool/pool-draw-info !sidebar-pool)
                                :dirty-rect dirty-rect
                                :render-target render-target
                                :clear-quad (:clear-quad (:pipelines geometry)))
              (let [raf-t4 (js/performance.now)]
                (when (> (- raf-t4 raf-t0) 5)
                  (js/console.log "[RAF] prep:" (.toFixed (- raf-t1 raf-t0) 1) "ms | text-gpu:" (.toFixed (- raf-t2 raf-t1) 1) "ms | rects-gpu:" (.toFixed (- raf-t3 raf-t2) 1) "ms | draw:" (.toFixed (- raf-t4 raf-t3) 1) "ms | TOTAL:" (.toFixed (- raf-t4 raf-t0) 1) "ms | content-same?:" content-same? "chrome-same?:" chrome-same?
                                  "dirty-rect:" (if dirty-rect "partial" "full")))))

            {:content-text-geo new-content-geo
             :chrome-text-geo new-chrome-geo
             :cmd-rect-sys new-cmd-sys
             :settings-rect-sys new-settings-sys
             :render-target render-target
             :prev-world world
             :prev-content-ops content-ops
             :prev-chrome-ops chrome-ops
             :prev-sidebar-data sidebar-data
             :prev-settings-text settings-text
             :prev-settings-visible settings-visible
             :prev-show-diagnostics show-diagnostics?
             :prev-scroll-y scroll-y
             :prev-editor-rects editor-rects
             :prev-editor-shadows editor-shadows
             :prev-cmd-rects cmd-rects
             :prev-settings-rects settings-rects
             :prev-font-size font-size
             :prev-px-range px-range
             :prev-line-height line-h
             :prev-sharpness sharpness
             :prev-char-width char-width
             :prev-snap-step snap-step
             :prev-font-id (:id font-assets)})))

      {:content-text-geo (:text geometry)
       :chrome-text-geo (editor/clone-text-system device (:text geometry) 2000)
       :cmd-rect-sys @!cmd-rect-sys
       :settings-rect-sys @!settings-rect-sys
       :render-target (let [vp @!viewport
                            d (or (:dpr vp) 1)]
                        (editor/create-render-target device
                          (Math/floor (* (:width vp) d))
                          (Math/floor (* (:height vp) d))
                          (:format (:pipelines geometry))))
       :prev-world nil
       :prev-content-ops nil
       :prev-chrome-ops nil
       :prev-sidebar-data nil
       :prev-settings-text nil
       :prev-settings-visible nil
       :prev-show-diagnostics nil
       :prev-scroll-y nil
       :prev-editor-rects nil
       :prev-editor-shadows nil
       :prev-cmd-rects nil
       :prev-settings-rects nil
       :prev-font-size nil
       :prev-px-range nil
       :prev-line-height nil
       :prev-sharpness nil
       :prev-char-width nil
       :prev-snap-step nil
       :prev-font-id "dejavu-sans-mono"}

      (m/sample vector <world-snapshot >dirty-raf))))
