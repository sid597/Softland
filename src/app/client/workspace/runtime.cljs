(ns app.client.workspace.runtime
  "Thin shell: build runtime context, wire modules, join the reactive loop.
   All business logic lives in workspace/runtime/* modules."
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [missionary.core :as m]
            [app.client.workspace.events :as events]
            [app.client.workspace.sidebar :refer [cmd-panel-h status-bar-h]]
            [app.client.workspace.runtime.state :as state]
            [app.client.workspace.runtime.fonts :as fonts]
            [app.client.workspace.runtime.sidebar-io :as sidebar-io :refer [emit-settings-update!]]
            [app.client.workspace.runtime.workspace-actions :as ws]
            [app.client.workspace.runtime.interop :as interop]
            [app.client.workspace.runtime.agent-flow :as agent-flow]
            [app.client.workspace.runtime.scroll :as scroll]
            [app.client.workspace.runtime.mouse :as mouse]
            [app.client.workspace.runtime.keyboard :as kbd]
            [app.client.workspace.runtime.render :as render]))

(defn start-loop!
  "Start the reactive editor loop.
   Public API — signature unchanged. Builds the rt context, wires modules,
   returns a Missionary task that runs the render loop."
  [node device ctx geometry initial-line-lengths initial-lines
   tokenize-fn layout-fn find-bracket-fn detect-folds-fn
   find-form-fn eval-form-fn font-assets & {:keys [font-manifest gpu-budget !sidebar-visible !file-load-request !preview-el !remote-sidebar-truth !remote-settings-truth !remote-agent-trail !remote-flow-session !remote-workspace-truth initial-file]}]

  (let [;; Phase 2: Build the rt context map
        rt (state/make-runtime-state
             {:node node :device device :ctx ctx :geometry geometry :font-assets font-assets
              :initial-lines initial-lines :font-manifest font-manifest
              :gpu-budget gpu-budget
              :!sidebar-visible !sidebar-visible
              :!file-load-request !file-load-request
              :!preview-el !preview-el})

        atoms  (:atoms rt)
        layout (:layout rt)
        gpu    (:gpu rt)

        deps {:tokenize-fn tokenize-fn :layout-fn layout-fn
              :find-bracket-fn find-bracket-fn :detect-folds-fn detect-folds-fn
              :find-form-fn find-form-fn :eval-form-fn eval-form-fn}

        ;; ── Install watches & side effects ──────────────────────────
        _ (fonts/install-font-watch! atoms)
        _ (interop/install-extract-preview-watch! atoms)

        ;; ── Sidebar I/O ─────────────────────────────────────────────
        io (sidebar-io/make-sidebar-io atoms)
        _ (sidebar-io/install-sidebar-watch! atoms (:fetch-home-dirs! io))
        _ (sidebar-io/seed-initial-file! atoms io initial-file)

        ;; ── Rama truth sync (Electric → local sidebar state) ──────
        ;; Live reconciliation: Rama truth -> !sidebar-truth.
        ;; When truth catches up, clear matching pending optimistic entries.
        apply-sidebar-truth!
        (fn [truth]
          (when (map? truth)
            (let [!st  (:!sidebar-truth atoms)
                  !so  (:!sidebar-overlay atoms)
                  !ui  (:!sidebar-ui atoms)
                  !cf  (:!current-file atoms)
                  old-file @!cf]
              ;; 1. Update committed truth
              (swap! !st (fn [s]
                           (cond-> s
                             (contains? truth :project)
                             (assoc :project (:project truth))
                             (contains? truth :expanded-dirs)
                             (assoc :expanded-dirs (:expanded-dirs truth))
                             (contains? truth :selected-file)
                             (assoc :selected-file (:selected-file truth)))))
                             
              ;; Sync !selected-artifact + !current-file from truth
              (when (contains? truth :selected-file)
                (let [sf (:selected-file truth)]
                  (reset! !cf sf)
                  (reset! (:!selected-artifact atoms)
                          (when (:path sf)
                            {:kind :file :path (:path sf) :name (:name sf)}))))
                
              ;; 2. Clear matched optimistic overlay state
              (swap! !so (fn [overlay]
                           (let [proj-matched? (if (contains? overlay :pending-project)
                                                 (= (:path (:project truth)) (:path (:pending-project overlay)))
                                                 false)
                                 ;; clear dirs from pending if they are now in truth
                                 new-pending-exp (set/difference (:pending-expanded-dirs overlay) (or (:expanded-dirs truth) #{}))
                                 ;; collpased means it's NOT in truth
                                 new-pending-col (set/intersection (:pending-collapsed-dirs overlay) (or (:expanded-dirs truth) #{}))
                                 file-matched? (if (contains? overlay :pending-selected-file)
                                                 (= (:path (:selected-file truth)) (:path (:pending-selected-file overlay)))
                                                 false)]
                             (cond-> overlay
                               proj-matched? (dissoc :pending-project)
                               true (assoc :pending-expanded-dirs new-pending-exp)
                               true (assoc :pending-collapsed-dirs new-pending-col)
                               file-matched? (dissoc :pending-selected-file)))))
                               
              ;; 3. Rehydrate dir-cache based on new truth
              (when-let [proj (:project truth)]
                (let [proj-path (:path proj)
                      dirs      (or (:expanded-dirs truth) #{})]
                  ;; Fetch project root if not cached
                  (when (and proj-path
                             (not (contains? (:dir-cache @!ui) proj-path)))
                    ((:fetch-dir! io) proj-path))
                  ;; Fetch each expanded dir if not cached
                  (doseq [d dirs]
                    (when-not (contains? (:dir-cache @!ui) d)
                      ((:fetch-dir! io) d)))))
                      
              ;; 4. Rehydrate file content if selected file changed
              (when-let [sf (:selected-file truth)]
                (let [proj-path (some-> (:project truth) :path)]
                  (when (and (:path sf) proj-path
                             (not= (:path sf) (:path old-file)))
                    ((:fetch-file! io) (:path sf) proj-path)))))))

        ;; Watch remote truth continuously and reconcile against optimistic overlay
        _ (when !remote-sidebar-truth
            (add-watch !remote-sidebar-truth :truth-sync
              (fn [_ _ _ new-truth]
                (apply-sidebar-truth! new-truth)))
            ;; Apply current truth initially
            (apply-sidebar-truth! @!remote-sidebar-truth))

        ;; ── Settings persistence (Rama round-trip) ─────────────────
        ;; 1. On load: apply persisted settings from Rama → !settings
        ;; 2. After load: watch local !settings changes → fire-and-forget to Rama
        ;; Suppression flag prevents the initial load from triggering a pointless POST.
        !settings-loaded (atom false)

        _ (when !remote-settings-truth
            (let [persistent-keys #{:font-size :line-height :px-range :sharpness
                                    :snap-to-pixel? :show-diagnostics? :font-id :theme-id}
                  truth @!remote-settings-truth]
              (when (and (map? truth) (seq truth))
                (let [persistent-fields (select-keys truth persistent-keys)]
                  (when (seq persistent-fields)
                    (swap! (:!settings atoms) merge persistent-fields)
                    (js/console.log "[SETTINGS-TRUTH] Initial load applied:" (pr-str (keys persistent-fields)))
                    ;; Sync !active-font if font-id was restored from truth.
                    ;; Normally !active-font → !settings (via font watch), but on
                    ;; load we need the reverse direction to apply the saved font.
                    (when-let [saved-font-id (:font-id persistent-fields)]
                      (let [manifest @(:!font-manifest atoms)
                            fonts (or (:fonts manifest) [])
                            available-fonts (filterv #(not (false? (:available %))) fonts)
                            font-config (first (filter #(= (:id %) saved-font-id) available-fonts))
                            font-idx (when font-config
                                       (first (keep-indexed
                                                (fn [i f] (when (= (:id f) saved-font-id) i))
                                                available-fonts)))]
                        (when font-config
                          ;; Reset !active-font — this triggers install-font-watch! which
                          ;; synchronously writes font defaults into !settings.
                          (reset! (:!active-font atoms)
                                  {:id (:id font-config)
                                   :char-width (or (:charWidth font-config) 0.56)
                                   :name (:name font-config)})
                          ;; Re-apply persisted settings to undo the font-watch default
                          ;; overwrite. The watch fires synchronously above, so this merge
                          ;; restores the user's saved slider values over the font's defaults.
                          (swap! (:!settings atoms) merge persistent-fields)
                          ;; Reconcile the settings panel's local cursor state so the
                          ;; highlighted row matches the restored font, not the boot default.
                          (when font-idx
                            (swap! (:!settings atoms) assoc :selected-index font-idx))))))))))

        ;; Persist local settings changes to Rama via fire-and-forget HTTP.
        ;; Only fires after initial truth has loaded (suppresses the no-op echo).
        _ (let [persistent-keys #{:font-size :line-height :px-range :sharpness
                                  :snap-to-pixel? :show-diagnostics? :font-id :theme-id}]
            (add-watch (:!settings atoms) :settings-persist
              (fn [_ _ old-val new-val]
                (when @!settings-loaded
                  (let [old-p (select-keys old-val persistent-keys)
                        new-p (select-keys new-val persistent-keys)]
                    (when (not= old-p new-p)
                      (let [changed (into {} (filter (fn [[k v]] (not= v (get old-p k))) new-p))]
                        (when (seq changed)
                          (emit-settings-update! changed))))))))
            ;; Enable persistence after the initial truth has been applied
            (reset! !settings-loaded true))

        ;; ── Agent trail restore (Rama → local) ───────────────────────
        ;; On load: if Rama has a saved trail and !agent-output is empty,
        ;; restore it so the last run's trail survives page reload.
        _ (when !remote-agent-trail
            (let [saved @!remote-agent-trail]
              (when (and (map? saved) (:trail-data saved) (nil? @(:!agent-output atoms)))
                (let [{:keys [run-id trail-data]} saved]
                  (reset! (:!agent-output atoms)
                          {:status   (or (:status trail-data) :complete)
                           :provider (or (:provider trail-data) :claude)
                           :prompt   (or (:prompt trail-data) "")
                           :output   ""
                           :run-id   run-id
                           :trail    (or (:trail trail-data) [])
                           :tool-buf {}
                           :structured-result (:structured-result trail-data)})
                  (js/console.log "[TRAIL-TRUTH] Restored trail for run:" run-id)))))

        ;; ── Flow session persistence (Rama round-trip) ───────────────
        ;; On load: restore flow state from Rama if !flow-state is at :idle.
        ;; After load: watch !flow-state for FSM node transitions → persist.
        _ (when !remote-flow-session
            (let [saved @!remote-flow-session
                  persistent-keys #{:node :tickets :batch :active-lane-idx
                                    :runs :decisions :session-id :history}]
              ;; Restore on load (only if idle — don't overwrite an active session)
              (when (and (map? saved) (seq saved)
                         (= :idle (:node @(:!flow-state atoms))))
                (let [restored (select-keys saved persistent-keys)
                      ;; Reconstruct :selected from persisted :batch :lanes.
                      ;; set-selection keeps these in sync, so on restore we
                      ;; reverse it — without this, the UI shows "no selection"
                      ;; even though the batch has lanes.
                      lanes (get-in restored [:batch :lanes])
                      restored (if (seq lanes)
                                 (assoc restored :selected (vec lanes))
                                 restored)]
                  (when (and (seq restored) (not= :idle (:node restored)))
                    (swap! (:!flow-state atoms) merge restored)
                    (js/console.log "[FLOW-TRUTH] Restored flow state:" (pr-str (:node restored))))))
              ;; Persist when any persistent field changes (not just :node).
              ;; Covers batch/lane updates from set-selection, session-id
              ;; from agent events, etc.
              (add-watch (:!flow-state atoms) :flow-persist
                (fn [_ _ old-val new-val]
                  (let [old-p (select-keys old-val persistent-keys)
                        new-p (select-keys new-val persistent-keys)]
                    (when (not= old-p new-p)
                      (sidebar-io/save-flow-state! new-p)))))))

        ;; Reset detail scroll when ticket selection changes — centralized
        ;; so all paths (mouse, keyboard, /flow-select command) are covered.
        _ (add-watch (:!flow-state atoms) :selection-scroll-reset
            (fn [_ _ old-val new-val]
              (when (not= (:selected old-val) (:selected new-val))
                (reset! (:!detail-scroll-y atoms) 0))))

        ;; ── Workspace truth persistence (Phase 7) ────────────────────
        ;; Restore on load: selected-artifact, active-pane, sidebar-visible.
        ;; Persist on change: debounced to avoid intermediate states.
        ;; Do NOT persist derived state (!effective-local-world, :split, :panes).
        !workspace-loaded (atom false)
        !workspace-persist-timer (atom nil)

        _ (when !remote-workspace-truth
            (let [saved @!remote-workspace-truth]
              (when (and (map? saved) (seq saved))
                ;; Restore selected-artifact + sync !current-file
                (let [art (:selected-artifact saved)]
                  (reset! (:!selected-artifact atoms) art)
                  (if (and art (= :file (:kind art)))
                    (do (reset! (:!current-file atoms) {:path (:path art) :name (:name art)})
                        (when-let [project (or (:path (:project @(:!sidebar-truth atoms)))
                                               (some-> (:path art) (str/split #"/") butlast seq (#(str/join "/" %))))]
                          ((:fetch-file! io) (:path art) project)))
                    ;; Not a file or nil — clear !current-file to prevent stale identity
                    (reset! (:!current-file atoms) nil)))
                ;; Restore active-pane via semantic action (syncs !focus + !caret-visible)
                (when-let [pane (:active-pane saved)]
                  (ws/set-active-pane! atoms pane))
                ;; Restore sidebar-visible
                (when (contains? saved :sidebar-visible)
                  (when (:!sidebar-visible atoms)
                    (reset! (:!sidebar-visible atoms) (:sidebar-visible saved))))
                (js/console.log "[WORKSPACE-TRUTH] Restored:" (pr-str (keys saved))))))

        ;; Debounced persist: wait 16ms after last atom change so sequential
        ;; mutations from one action (e.g. clear-artifact! sets both
        ;; !selected-artifact and !active-pane) settle before saving.
        _ (let [persist-workspace!
                (fn []
                  (when-let [timer @!workspace-persist-timer]
                    (js/clearTimeout timer))
                  (reset! !workspace-persist-timer
                    (js/setTimeout
                      (fn []
                        (reset! !workspace-persist-timer nil)
                        (when @!workspace-loaded
                          (let [truth {:selected-artifact @(:!selected-artifact atoms)
                                       :active-pane @(:!active-pane atoms)
                                       :sidebar-visible (boolean (and (:!sidebar-visible atoms)
                                                                      @(:!sidebar-visible atoms)))}]
                            (sidebar-io/save-workspace-truth! truth))))
                      16)))]
            (add-watch (:!selected-artifact atoms) :workspace-persist (fn [_ _ _ _] (persist-workspace!)))
            (add-watch (:!active-pane atoms) :workspace-persist (fn [_ _ _ _] (persist-workspace!)))
            (when (:!sidebar-visible atoms)
              (add-watch (:!sidebar-visible atoms) :workspace-persist (fn [_ _ _ _] (persist-workspace!))))
            (reset! !workspace-loaded true))

        ;; ── Effective local world (reactive derivation) ──────────────
        ;; One derived object that answers "what world is the user in?"
        ;; Recomputed when any of its inputs change.
        recompute-local-world!
        (fn []
          (let [sidebar-truth @(:!sidebar-truth atoms)
                sidebar-overlay @(:!sidebar-overlay atoms)]
            (reset! (:!effective-local-world atoms)
                    (ws/derive-effective-local-world
                      {:selected-artifact @(:!selected-artifact atoms)
                       :active-pane       @(:!active-pane atoms)
                       :sidebar-visible   (and (:!sidebar-visible atoms)
                                               @(:!sidebar-visible atoms))
                       :flow-state        @(:!flow-state atoms)
                       :agent-output      @(:!agent-output atoms)
                       :project           (or (:pending-project sidebar-overlay)
                                              (:project sidebar-truth))}))))

        _ (recompute-local-world!)
        _ (doseq [a [:!selected-artifact :!active-pane :!flow-state :!agent-output]]
            (add-watch (get atoms a) :local-world (fn [_ _ _ _] (recompute-local-world!))))
        _ (when (:!sidebar-visible atoms)
            (add-watch (:!sidebar-visible atoms) :local-world (fn [_ _ _ _] (recompute-local-world!))))
        _ (add-watch (:!sidebar-truth atoms) :local-world (fn [_ _ _ _] (recompute-local-world!)))
        _ (add-watch (:!sidebar-overlay atoms) :local-world (fn [_ _ _ _] (recompute-local-world!)))

        ;; ── Agent API (needs io for trigger-dev-replay!) ────────────
        trigger-replay! (fn [] (interop/trigger-dev-replay! atoms))
        agent-api (agent-flow/make-agent-api atoms trigger-replay!)
        _ (interop/install-window-globals! atoms (:show-flow-info! agent-api))

        ;; ── Event flows (fresh per instance) ────────────────────────
        >raf            (events/make-raf-flow)
        >blink-timer    (events/make-blink-timer)
        >shimmer-timer  (events/make-blink-timer)
        >resize         (events/>canvas-resize node)
        >wheel-events   (events/>wheel node)
        >mouse-events   (events/>mouse node)
        >keyboard-events (events/>keyboard js/window)

        ;; ── Focus-based routing ─────────────────────────────────────
        <global-keys      (events/<global-events >keyboard-events)
        <editor-keyboard  (events/<editor-keys >keyboard-events (:!focus atoms))
        <cmd-keyboard     (events/<cmd-panel-keys >keyboard-events (:!focus atoms))
        <chat-keyboard    (events/<chat-input-keys >keyboard-events (:!focus atoms))
        <settings-keyboard (events/<settings-panel-keys >keyboard-events (:!focus atoms))

        ;; ── DOM listeners (raw, not Missionary) ─────────────────────
        _ (mouse/install-drag-select! atoms layout node)
        _ (mouse/install-paste-handler! atoms)]

    ;; ═══════════════════════════════════════════════════════════════
    ;; JOIN: all consumers run concurrently
    ;; ═══════════════════════════════════════════════════════════════
    (m/join vector
      ;; Timers
      (->> >blink-timer
           (m/reduce (fn [_ v] (reset! (:!caret-visible atoms) v) nil) nil))
      (->> >shimmer-timer
           (m/reduce (fn [_ v] (reset! (:!shimmer-phase atoms) v) nil) nil))

      ;; Viewport resize — update atom + sync canvas pixel dimensions immediately.
      ;; Setting canvas.width/height clears the swap chain, but unconditional RAF
      ;; redraws within 16ms. Without immediate sync, the browser stretches the
      ;; old pixel buffer to fit the new CSS box, causing visible flickering.
      (->> >resize
           (m/reduce
             (fn [_ {:keys [width height dpr]}]
               (let [safe-dpr (or dpr 1)]
                 (reset! (:!viewport atoms) {:width width :height height :dpr safe-dpr})
                 (set! (.-width node) (Math/floor (* width safe-dpr)))
                 (set! (.-height node) (Math/floor (* height safe-dpr))))
               nil)
             nil))

      ;; Consumers from modules
      (scroll/scroll-consumer atoms >wheel-events)
      (mouse/mouse-consumer atoms layout deps io >mouse-events)
      (kbd/global-keys-consumer atoms <global-keys)
      (kbd/file-load-consumer atoms)
      (kbd/editor-keys-consumer atoms layout deps <editor-keyboard)
      (kbd/command-keys-consumer atoms (:submit-agent-run! agent-api) <cmd-keyboard)
      (kbd/chat-keys-consumer atoms (:submit-agent-run! agent-api) <chat-keyboard)
      (kbd/settings-keys-consumer atoms <settings-keyboard)

      ;; Render loop (the terminal consumer)
      (render/render-consumer atoms layout gpu deps >raf))))
