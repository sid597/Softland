(ns app.client.workspace.runtime
  "Thin shell: build runtime context, wire modules, join the reactive loop.
   All business logic lives in workspace/runtime/* modules."
  (:require [clojure.set :as set]
            [missionary.core :as m]
            [app.client.workspace.events :as events]
            [app.client.workspace.sidebar :refer [cmd-panel-h status-bar-h]]
            [app.client.workspace.runtime.state :as state]
            [app.client.workspace.runtime.fonts :as fonts]
            [app.client.workspace.runtime.sidebar-io :as sidebar-io]
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
   find-form-fn eval-form-fn atlas & {:keys [font-manifest !sidebar-visible !file-load-request !preview-el !remote-sidebar-truth initial-file]}]

  (let [;; Phase 2: Build the rt context map
        rt (state/make-runtime-state
             {:node node :device device :ctx ctx :geometry geometry :atlas atlas
              :initial-lines initial-lines :font-manifest font-manifest
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
                             
              ;; Also update !current-file for now (to not break the rest of the app)
              (when (contains? truth :selected-file)
                (reset! !cf (:selected-file truth)))
                
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

      ;; Viewport resize
      (->> >resize
           (m/reduce
             (fn [_ {:keys [width height dpr]}]
               (reset! (:!viewport atoms) {:width width :height height :dpr dpr})
               (set! (.-width node) (Math/floor (* width dpr)))
               (set! (.-height node) (Math/floor (* height dpr)))
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
