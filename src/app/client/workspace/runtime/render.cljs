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
           !font-manifest !font-assets !text-geo !shadow-sys !cmd-rect-sys !settings-rect-sys
           !sidebar-pool]
    :as atoms}
   {:keys [layout-x layout-y gutter-w]}
   {:keys [device ctx geometry atlas]}
   {:keys [tokenize-fn layout-fn detect-folds-fn find-bracket-fn]}
   >raf]
  (let [;; Derived flows
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
                current-text-geo (:text-geo prev-state)

                updated-text-geo (if (and font-changed? (:bitmap font-assets))
                                   (do (js/console.log "[RENDER] Updating font texture for:" (:id font-assets))
                                       (editor/update-font-texture device current-text-geo (:bitmap font-assets)))
                                   current-text-geo)

                active-atlas (or (:atlas font-assets) atlas)

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

                text-same? (and (identical? text-data (:prev-text-data prev-state))
                                (identical? settings-text (:prev-settings-text prev-state))
                                (= show-diagnostics? (:prev-show-diagnostics prev-state))
                                (= scroll-y (:prev-scroll-y prev-state))
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
                render-line-count (count (:render-ops text-data))
                editor-line-count (:editor-line-count text-data)
                cmd-line-count (:cmd-line-count text-data)
                settings-line-count (count (or settings-lines []))
                diagnostics-line-index (when diagnostics-line
                                         (+ render-line-count settings-line-count))

                raf-t1 (js/performance.now)  ;; after text-same? check
                base-text-geo (if (not text-same?)
                                (editor/update-text-data device updated-text-geo
                                                         all-text-ops active-atlas font-size
                                                         :px-range px-range
                                                         :line-height line-h
                                                         :char-width char-width
                                                         :snap-step snap-step
                                                         :sharpness sharpness)
                                updated-text-geo)
                raf-t2 (js/performance.now)  ;; after text GPU upload
                new-text-geo (assoc base-text-geo
                                    :line-mapping (:line-mapping text-data)
                                    :editor-line-count editor-line-count
                                    :cmd-line-count cmd-line-count
                                    :diagnostics-line-index diagnostics-line-index)

                new-editor-sys (if (not (identical? editor-rects (:prev-editor-rects prev-state)))
                                 (editor/update-rects device
                                                      (or (:editor-rect-sys prev-state) (:rect geometry))
                                                      editor-rects)
                                 (:editor-rect-sys prev-state))

                ;; Shadows: combine editor + sidebar (shadow pipeline is separate from rect pool)
                sidebar-shadows (or (:shadows sidebar-data) [])
                shadows-changed? (or (not (identical? editor-shadows (:prev-editor-shadows prev-state)))
                                     (not (identical? sidebar-data (:prev-sidebar-data prev-state))))
                combined-shadows (if shadows-changed?
                                   (into (vec (or editor-shadows [])) sidebar-shadows)
                                   nil)
                new-shadow-sys (if shadows-changed?
                                 (editor/update-shadows device
                                                        (or (:shadow-sys prev-state) @!shadow-sys)
                                                        combined-shadows)
                                 (:shadow-sys prev-state))

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

            (let [raf-t3 (js/performance.now)]  ;; before draw
            (editor/draw-frame! device ctx
                                new-text-geo new-editor-sys new-cmd-sys
                                (:camera-floats (:pipelines geometry))
                                (:pass-descriptor (:pipelines geometry))
                                0 (- scroll-y)
                                (:width viewport) (:height viewport)
                                :cmd-panel-visible cmd-visible
                                :cmd-panel-h cmd-panel-h
                                :editor-line-count (:editor-line-count text-data)
                                :pre-settings-line-count render-line-count
                                :settings-line-count settings-line-count
                                :settings-visible settings-visible
                                :settings-rect-sys new-settings-sys
                                :diagnostics-visible show-diagnostics?
                                :diagnostics-line-index diagnostics-line-index
                                :agent-visible agent-visible
                                :shadow-sys new-shadow-sys
                                :sidebar-pool-info (pool/pool-draw-info !sidebar-pool))
              (let [raf-t4 (js/performance.now)]
                (when (> (- raf-t4 raf-t0) 5)
                  (js/console.log "[RAF] prep:" (.toFixed (- raf-t1 raf-t0) 1) "ms | text-gpu:" (.toFixed (- raf-t2 raf-t1) 1) "ms | rects-gpu:" (.toFixed (- raf-t3 raf-t2) 1) "ms | draw:" (.toFixed (- raf-t4 raf-t3) 1) "ms | TOTAL:" (.toFixed (- raf-t4 raf-t0) 1) "ms | text-same?:" text-same?))))

            {:text-geo new-text-geo
             :editor-rect-sys new-editor-sys
             :cmd-rect-sys new-cmd-sys
             :settings-rect-sys new-settings-sys
             :shadow-sys new-shadow-sys
             :prev-world world
             :prev-text-data text-data
             :prev-sidebar-data sidebar-data
             :prev-settings-text settings-text
             :prev-show-diagnostics show-diagnostics?
             :prev-scroll-y scroll-y
             :prev-text-ops all-text-ops
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

      {:text-geo (:text geometry)
       :editor-rect-sys (:rect geometry)
       :cmd-rect-sys @!cmd-rect-sys
       :settings-rect-sys @!settings-rect-sys
       :shadow-sys @!shadow-sys
       :prev-world nil
       :prev-text-data nil
       :prev-sidebar-data nil
       :prev-settings-text nil
       :prev-show-diagnostics nil
       :prev-scroll-y nil
       :prev-text-ops nil
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

      (m/sample vector <world-snapshot >raf))))
