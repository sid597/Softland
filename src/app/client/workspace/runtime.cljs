(ns app.client.workspace.runtime
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
            [app.client.substrate.webgpu.renderer :as editor]
            [app.client.workspace.text-input :as text-input]
            [app.client.workspace.themes :as themes]
            [app.client.workspace.events :as events :refer [maybe-snap snap-to-dpr]]
            [app.client.workspace.rect-tree :as rt :refer [wrap-line resolve-layout tree->rects tree->text-ops tree->shadows hit-test dispatch-event]]
            [app.client.workspace.ui-primitives :as ui :refer [list-left-pane-pct]]
            [app.client.workspace.sidebar :as sidebar :refer [sidebar-w sidebar-tab-h cmd-panel-h status-bar-h compute-sidebar-content-height build-sidebar-tree]]
            [app.client.workspace.trail :refer [compute-agent-panel-h agent-wrapped-line-count]]
            [app.client.workspace.shell :refer [build-file-layout]]
            [app.client.workspace.settings-view :as settings-view :refer [manifest-defaults->settings font-defaults->settings slider-specs <settings-panel-rects <settings-panel-text]]
            [app.client.workspace.cmd-panel :as cmd-panel :refer [cmd-panel-apply-event cmd-text-start-x parse-agent-command <cmd-panel-rects]]
            [app.client.workspace.agent :as agent :refer [stream-agent-run! parse-structured-result]]
            [app.client.workspace.editor-compute :as editor-compute :refer [editor-apply-event <fold-state <bracket-match <editor-rects]]
            [app.client.workspace.combined-text :refer [<combined-text-ops]]
            [app.client.workflows.dg-flow :as dg :refer [initial-flow-state flow-canvas-active? flow-prompt group-tickets-by-status list-content-height drag-distance drag-threshold-px build-intake-tree compute-run-text-ops compute-run-rects]]
            [app.client.workflows.jit :as jit]))

;; ============================================================================
;; LAYER 1: PRIMARY SOURCES (The Only Atoms)
;; ============================================================================
;; These are created per-instance in start-loop! to support multiple editors


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
   find-form-fn eval-form-fn atlas & {:keys [font-manifest !sidebar-visible !file-load-request !preview-el initial-file]}]

  (let [;; Layout Configuration
        font-size 19
        gutter-w  40
        layout-x  (+ 50 gutter-w)
        layout-y  100
        line-h    (* font-size 1.2)

        ;; =====================================================================
        ;; LAYER 1: PRIMARY SOURCE ATOMS
        ;; =====================================================================

        !editor-doc (atom {:lines initial-lines
                          :cursor {:line 0 :col 0}
                          :selection nil
                          :desired-col 0})

        !cmd-panel (atom {:text "" :cursor 0 :visible true})

        !focus (atom :command-panel)

        !scroll-y (atom 0)
        !scroll-x (atom 0)
        !run-scroll-y (atom 0)
        !detail-scroll-y (atom 0)

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

        ;; Focus hierarchy — which pane is active
        !active-pane (atom :editor)

        ;; Font configuration - use passed manifest or default
        default-manifest {:fonts [{:name "DejaVu Sans Mono"
                                   :id "dejavu-sans-mono"
                                   :charWidth 0.56
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
                         {:id "dejavu-sans-mono" :name "DejaVu Sans Mono" :charWidth 0.56})
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
                                                    (clj->js {:size (* capacity editor/rect-stride)
                                                              :usage (bit-or js/GPUBufferUsage.VERTEX
                                                                             js/GPUBufferUsage.COPY_DST)}))]
                              {:pipeline (:pipeline (:rect geometry))
                               :bind-group (:bind-group (:rect geometry))
                               :instance-buffer instance-buffer
                               :num-instances 0}))
        !settings-rect-sys (atom (let [capacity 32
                                        instance-buffer (.createBuffer device
                                                          (clj->js {:size (* capacity editor/rect-stride)
                                                                    :usage (bit-or js/GPUBufferUsage.VERTEX
                                                                                   js/GPUBufferUsage.COPY_DST)}))]
                                    {:pipeline (:pipeline (:rect geometry))
                                     :bind-group (:bind-group (:rect geometry))
                                     :instance-buffer instance-buffer
                                     :num-instances 0}))
        !shadow-sys (atom (:shadow geometry))

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
        ;; V0: default to discourse-graph entry point
        !current-file (atom {:path "/home/sid/projects/discourse-graph/apps/roam/src/index.ts"
                             :name "index.ts"})
        ;; Consolidated sidebar state (replaces 9 individual atoms)
        !sidebar-state (atom {:project nil           ;; {:name :path} or nil
                              :expanded-dirs #{}
                              :dir-cache {}
                              :home-dirs nil
                              :scroll-y 0
                              :hovered-id nil
                              :loading? false})
        !ai-provider (atom :claude)     ;; :claude | :codex | :gemini
        !agent-output (atom nil)        ;; {:status :provider :prompt :output :run-id :trail :tool-buf}
        !agent-scroll-y (atom 0)        ;; scroll offset within agent output panel
        !chat-scroll-y (atom 0)         ;; scroll offset within chat pane (3-pane mode)
        !chat-input (atom {:text "" :cursor 0})  ;; dedicated text buffer for chat pane input
        !mouse-x (atom 0)               ;; last known mouse X (viewport-relative)
        !mouse-y (atom 0)               ;; last known mouse Y (viewport-relative)
        !flow-state (atom (initial-flow-state))  ;; V0 flow state machine
        !collapsed-groups (atom #{})           ;; set of collapsed group status strings
        !hovered-row-idx (atom nil)            ;; 0-based flat ticket index under mouse cursor
        !drag-state (atom {:phase :idle})      ;; drag state machine: :idle/:pending/:dragging
        !extract-preview (atom nil)           ;; {:rt-node :ir :source-url :selector :html-styles}
        !trail-collapsed (atom #{})            ;; set of tool-ids whose card body is collapsed
        !shimmer-phase (atom false)            ;; toggled by blink timer — maps to shimmer-alpha

        ;; Extract preview overlay watcher — show/hide DOM overlay + populate HTML
        _ (add-watch !extract-preview :overlay
            (fn [_ _ _ new-val]
              (when-let [el (and !preview-el @!preview-el)]
                (if new-val
                  (do (set! (.-display (.-style el)) "block")
                      ;; Populate with reconstructed HTML if available
                      (if-let [html (:html new-val)]
                        (set! (.-innerHTML el)
                              (str "<div style='color:#888;font-size:12px;margin-bottom:8px;'>Source: "
                                   (or (:source-url new-val) "extracted") "</div>"
                                   "<div style='padding:16px;border:1px solid #333;border-radius:8px;background:#0d0d0f;'>"
                                   html "</div>"))
                        (set! (.-innerHTML el)
                              "<div style='color:#666;padding:40px;text-align:center;'>No HTML preview available.<br>Run extractor to capture source HTML.</div>")))
                  (do (set! (.-display (.-style el)) "none")
                      (set! (.-innerHTML el) ""))))))

        ;; sidebar-el removed — sidebar now rendered via WebGPU rect tree

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
        (fn []
          (when (nil? (:home-dirs @!sidebar-state))
            (fetch-edn! "/api/home-dirs"
                        (fn [dirs]
                          (swap! !sidebar-state assoc :home-dirs dirs)))))

        fetch-dir!
        (fn [path]
          (when-not (contains? (:dir-cache @!sidebar-state) path)
            (fetch-edn! (str "/api/list-dir?path=" (js/encodeURIComponent path))
                        (fn [entries]
                          (swap! !sidebar-state assoc-in [:dir-cache path] entries)))))

        fetch-file!
        (fn [path root-path & {:keys [target-line]}]
          (fetch-edn! (str "/api/read-file?path=" (js/encodeURIComponent path)
                           "&root=" (js/encodeURIComponent root-path))
                      (fn [result]
                        (if (:error result)
                          (js/console.error "[SIDEBAR] File read error:" (:error result))
                          (let [lines (str/split-lines (:content result))]
                            (reset! !current-file {:path path :name (last (str/split path #"/"))})
                            (reset! !scroll-x 0)
                            (reset! !file-load-request
                                    (cond-> {:lines lines}
                                      target-line (assoc :target-line target-line))))))))

        trigger-dev-replay!
        (fn []
          (js/console.log "[DEV] Triggering replay fixture...")
          (reset! !agent-scroll-y 0)
          (reset! !agent-output {:status :running
                                 :provider :claude
                                 :prompt "Replay Fixture"
                                 :output ""
                                 :run-id "replay-dev"
                                 :trail []
                                 :tool-buf {}})
          (-> (js/fetch "/api/dev/replay-fixture")
              (.then (fn [resp] (.json resp)))
              (.then (fn [json]
                       (let [data (js->clj json :keywordize-keys true)]
                         (if (:ok data)
                           (let [events (:events data)]
                             (doseq [[i evt] (map-indexed vector events)]
                               (js/setTimeout
                                 (fn []
                                   (let [kind (:event evt)]
                                     (case kind
                                       :text-delta
                                       (do
                                         (swap! !agent-output
                                           (fn [ao]
                                             (-> ao
                                               (update :output str (:text evt))
                                               (update :trail conj {:kind :reasoning :text (:text evt)}))))
                                         (let [ao @!agent-output
                                               viewport @!viewport
                                               settings @!settings
                                               font-size (:font-size settings)
                                               char-advance (* font-size (:char-width @!active-font))
                                               agent-h (compute-agent-panel-h ao font-size (:height viewport) (:width viewport) char-advance)
                                               line-step (* font-size 1.2)
                                               max-chars (if (pos? char-advance)
                                                           (max 1 (int (/ (- (:width viewport) 48) char-advance)))
                                                           80)
                                               line-count (agent-wrapped-line-count ao max-chars)
                                               total-h (* line-count line-step)
                                               max-scroll (max 0 (- total-h (- agent-h 16)))]
                                           (reset! !agent-scroll-y max-scroll)))

                                       :tool-use-start
                                       (swap! !agent-output
                                         (fn [ao]
                                           (-> ao
                                             (assoc-in [:tool-buf (:tool-id evt)]
                                               {:tool-name (:tool-name evt) :json "" :block-idx (:block-idx evt)})
                                             (update :trail conj {:kind :tool-call-start
                                                                  :tool-name (:tool-name evt)
                                                                  :tool-id (:tool-id evt)
                                                                  :block-idx (:block-idx evt)}))))

                                       :tool-input-delta
                                       (if-let [tid (:tool-id evt)]
                                         (swap! !agent-output
                                           (fn [ao]
                                             (update-in ao [:tool-buf tid :json] str (:json-chunk evt))))
                                         (js/console.warn "[DEV][REPLAY] :tool-input-delta missing :tool-id" (clj->js evt)))

                                       :tool-result
                                       (swap! !agent-output
                                         (fn [ao]
                                           (update ao :trail conj {:kind :tool-result
                                                                   :tool-id (:tool-id evt)
                                                                   :content (:content evt)})))

                                       :block-stop
                                       (let [ao @!agent-output
                                             matching-tool (some (fn [[tid buf]]
                                                                   (when (= (:block-idx buf) (:block-idx evt))
                                                                     [tid buf]))
                                                                 (:tool-buf ao))]
                                         (when matching-tool
                                           (let [[tid buf] matching-tool
                                                 parsed-input (try (js/JSON.parse (:json buf))
                                                                   (catch :default _ nil))]
                                             (swap! !agent-output
                                               (fn [ao]
                                                 (-> ao
                                                   (update :trail conj {:kind :tool-call
                                                                        :tool-name (:tool-name buf)
                                                                        :tool-id tid
                                                                        :input (js->clj parsed-input :keywordize-keys true)
                                                                        :block-idx (:block-idx evt)})
                                                   (update :tool-buf dissoc tid)))))))

                                       (:result :run-done)
                                       (swap! !agent-output assoc :status :complete)

                                       :run-error
                                       (swap! !agent-output assoc :status :failed)

                                       ;; Unknown — surface
                                       (js/console.warn "[DEV][UNKNOWN-EVENT]" (clj->js evt)))))
                                 (* i 50))))
                           (js/console.error "[DEV] Replay failed:" (:error data))))))
              (.catch (fn [err] (js/console.error "[DEV] Replay fetch error:" err)))))

        ;; =====================================================================
        ;; EXTRACTED EVENT HANDLER (DRY — used by replay, submit, and flow runs)
        ;; =====================================================================

        auto-scroll-agent!
        (fn []
          (let [ao @!agent-output
                viewport @!viewport
                settings @!settings
                font-size (:font-size settings)
                char-advance (* font-size (:char-width @!active-font))
                agent-h (compute-agent-panel-h ao font-size (:height viewport)
                                               (:width viewport) char-advance)
                line-step (* font-size 1.2)
                max-chars (if (pos? char-advance)
                            (max 1 (int (/ (- (:width viewport) 48) char-advance)))
                            80)
                line-count (agent-wrapped-line-count ao max-chars)
                total-h (* line-count line-step)
                max-scroll (max 0 (- total-h (- agent-h 16)))]
            (reset! !agent-scroll-y max-scroll)))

        make-event-handler
        (fn [run-id on-done-fn]
          (fn [evt]
            (let [kind (:event evt)]
              (case kind
                :text-delta
                (do (swap! !agent-output
                      (fn [ao]
                        (-> ao
                          (update :output str (:text evt))
                          (update :trail conj {:kind :reasoning :text (:text evt)}))))
                    (auto-scroll-agent!))

                :tool-use-start
                (swap! !agent-output
                  (fn [ao]
                    (-> ao
                      (assoc-in [:tool-buf (:tool-id evt)]
                        {:tool-name (:tool-name evt) :json "" :block-idx (:block-idx evt)})
                      (update :trail conj {:kind :tool-call-start
                                           :tool-name (:tool-name evt)
                                           :tool-id (:tool-id evt)
                                           :block-idx (:block-idx evt)}))))

                :thinking-delta
                (do (swap! !agent-output
                      (fn [ao]
                        (-> ao
                          (update :output str (:text evt))
                          (update :trail conj {:kind :thinking :text (:text evt)}))))
                    (auto-scroll-agent!))

                :thinking-start
                nil  ;; marker only, content comes via :thinking-delta

                :tool-input-delta
                (if-let [tid (:tool-id evt)]
                  (swap! !agent-output
                    (fn [ao]
                      (update-in ao [:tool-buf tid :json] str (:json-chunk evt))))
                  (js/console.warn "[AGENT] :tool-input-delta missing :tool-id" (clj->js evt)))

                :tool-result
                (swap! !agent-output
                  (fn [ao]
                    (update ao :trail conj {:kind :tool-result
                                            :tool-id (:tool-id evt)
                                            :content (:content evt)})))

                :block-stop
                (let [ao @!agent-output
                      matching-tool (some (fn [[tid buf]]
                                            (when (= (:block-idx buf) (:block-idx evt))
                                              [tid buf]))
                                          (:tool-buf ao))]
                  (when matching-tool
                    (let [[tid buf] matching-tool
                          parsed-input (try (js/JSON.parse (:json buf))
                                            (catch :default _ nil))]
                      (swap! !agent-output
                        (fn [ao]
                          (-> ao
                            (update :trail conj {:kind :tool-call
                                                 :tool-name (:tool-name buf)
                                                 :tool-id tid
                                                 :input (js->clj parsed-input :keywordize-keys true)
                                                 :block-idx (:block-idx evt)})
                            (update :tool-buf dissoc tid)))))))

                (:done :run-done)
                (do (swap! !agent-output (fn [ao]
                      (cond-> (assoc ao :status (or (:status evt) :complete))
                        (:result evt) (assoc :structured-result (:result evt)))))
                    (js/console.log "[AGENT][DONE]" (clj->js {:run-id run-id
                                                               :status (:status evt)
                                                               :has-result (some? (:result evt))}))
                    (when on-done-fn (on-done-fn)))

                (:start :run-start)
                (do (js/console.log "[AGENT][STREAM-START]" (clj->js evt))
                    ;; Capture session-id into flow state for --resume continuity
                    (when-let [sid (:session-id evt)]
                      (swap! !flow-state assoc :session-id sid)))

                :init
                (do (js/console.log "[AGENT][INIT]" (clj->js evt))
                    (when-let [sid (:session-id evt)]
                      (swap! !flow-state assoc :session-id sid)))

                :result
                (do (js/console.log "[AGENT][RESULT] session-id:" (:session-id evt))
                    (when-let [sid (:session-id evt)]
                      (swap! !flow-state assoc :session-id sid)))

                :run-error
                (do (swap! !agent-output assoc :status :failed)
                    (js/console.error "[AGENT][RUN-ERROR]" (clj->js evt))
                    (when on-done-fn (on-done-fn)))

                ;; Unknown — surface, don't silently drop
                (js/console.warn "[AGENT][UNKNOWN-EVENT]" (clj->js evt))))))

        ;; =====================================================================
        ;; FLOW RUN HELPER (compose prompt → stream)
        ;; =====================================================================

        ;; V0: hardcoded to discourse-graph (per commission-consensus.md scope)
        flow-cwd "/home/sid/projects/discourse-graph"
        ;; Read-only Linear MCP tools pre-approved for non-interactive (-p) mode
        flow-allowed-tools ["mcp__linear-server__list_issues"
                            "mcp__linear-server__get_issue"
                            "mcp__linear-server__search_issues"
                            "mcp__linear-server__list_projects"
                            "mcp__linear-server__get_project"
                            "mcp__linear-server__list_teams"]

        fire-flow-run!
        (fn [prompt-action & {:keys [on-done json-schema max-budget-usd model append-system-prompt]}]
          (let [flow @!flow-state
                {:keys [prompt]} (flow-prompt prompt-action flow)
                provider @!ai-provider
                cwd flow-cwd
                run-id (str (random-uuid))
                session-id (:session-id flow)
                request-body (cond-> {:run-id run-id
                                      :provider provider
                                      :prompt prompt
                                      :cwd cwd
                                      :allowed-tools flow-allowed-tools
                                      :context {:timestamp (js/Date.now)}}
                               session-id          (assoc :session-id session-id)
                               json-schema         (assoc :json-schema json-schema)
                               max-budget-usd      (assoc :max-budget-usd max-budget-usd)
                               model               (assoc :model model)
                               append-system-prompt (assoc :append-system-prompt append-system-prompt))]
            (js/console.log "[FLOW][FIRE]" (clj->js {:action prompt-action
                                                      :node (:node flow)
                                                      :run-id run-id
                                                      :session-id session-id}))
            (reset! !agent-scroll-y 0)
            (reset! !agent-output {:status :running
                                   :provider provider
                                   :prompt (str "[" (name prompt-action) "]")
                                   :output ""
                                   :run-id run-id
                                   :trail []
                                   :tool-buf {}})
            (stream-agent-run!
              "/api/agent/stream"
              request-body
              (make-event-handler run-id on-done)
              (fn [err]
                (js/console.error "[FLOW][STREAM-ERROR]" err)
                (reset! !agent-output {:status :failed
                                       :provider provider
                                       :prompt (str "[" (name prompt-action) "]")
                                       :output (str "Stream error: " (.-message err))
                                       :run-id run-id
                                       :trail []
                                       :tool-buf {}})
                (when on-done (on-done))))))

        show-flow-info!
        (fn [msg]
          (reset! !agent-scroll-y 0)
          (reset! !agent-output {:status :complete
                                 :provider @!ai-provider
                                 :prompt "flow"
                                 :output msg
                                 :run-id nil}))

        ;; Register global injection fns at startup (always available for Claude-in-Chrome)
        _ (set! (.-__softland_inject_preview js/window)
                (fn [data-json]
                  (let [data (js->clj (.parse js/JSON data-json) :keywordize-keys true)]
                    (-> (js/fetch "/api/extract/compile"
                          (clj->js {:method "POST"
                                    :headers {"Content-Type" "application/edn"}
                                    :body (pr-str {:tree data :source-url (.-href js/location)})}))
                        (.then #(.text %))
                        (.then (fn [text]
                                 (let [result (reader/read-string text)]
                                   (if (:ok result)
                                     (do (reset! !extract-preview {:rt-node (:rt-node result)
                                                                    :ir (:ir result)
                                                                    :source-url (:source-url result)})
                                         (show-flow-info! (str "Component compiled.\nSource: " (:source-url result))))
                                     (show-flow-info! (str "Compile failed: " (:error result)))))))
                        (.catch (fn [err]
                                  (show-flow-info! (str "Compile error: " (.-message err)))))))))
        _ (set! (.-__softland_inject_rt_node js/window)
                (fn [rt-node-json]
                  (let [rt-node (js->clj (.parse js/JSON rt-node-json) :keywordize-keys true)]
                    (reset! !extract-preview {:rt-node rt-node})
                    (show-flow-info! "rt-node injected directly."))))

        submit-agent-run!
        (fn [cmd-text]
          (let [shell-parsed (parse-agent-command cmd-text @!ai-provider)
                parsed (if (= :workflow-command (:kind shell-parsed))
                         (or (dg/parse-dg-command (:command shell-parsed))
                             (jit/parse-jit-command (:command shell-parsed))
                             {:kind :error
                              :message (str "Unknown command: " (:command shell-parsed))})
                         shell-parsed)
                doc @!editor-doc
                file-path (:path @!current-file)
                scroll-y @!scroll-y
                viewport @!viewport
                cwd (or (:path (:project @!sidebar-state))
                        (some-> file-path (str/split #"/") butlast seq (str/join "/"))
                        ".")
                context {:cursor (:cursor doc)
                         :selection (:selection doc)
                         :visible-range [scroll-y (+ scroll-y (:height viewport))]
                         :file-path file-path
                         :timestamp (js/Date.now)}]
            (case (:kind parsed)
              :noop
              nil

              :replay
              (trigger-dev-replay!)

              :set-provider
              (do
                (reset! !ai-provider (:provider parsed))
                (reset! !agent-scroll-y 0)
                (reset! !agent-output {:status :complete
                                       :provider (:provider parsed)
                                       :prompt "provider"
                                       :output (str "Provider set to " (-> (:provider parsed) name str/upper-case))
                                       :run-id nil}))

              :error
              (do (reset! !agent-scroll-y 0)
                  (reset! !agent-output {:status :failed
                                          :provider @!ai-provider
                                          :prompt cmd-text
                                          :output (:message parsed)
                                          :run-id nil}))

              ;; === Regular prompt run (refactored to use make-event-handler) ===
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
                                   (seq argv) (assoc :argv argv)
                                   (:session-id @!flow-state) (assoc :session-id (:session-id @!flow-state)))]
                (js/console.log "[AGENT][CLIENT][SUBMIT]"
                                (clj->js {:run-id run-id
                                          :provider provider
                                          :prompt prompt}))
                (reset! !agent-scroll-y 0)
                (reset! !agent-output {:status :running
                                       :provider provider
                                       :prompt prompt
                                       :output ""
                                       :run-id run-id
                                       :trail []
                                       :tool-buf {}})
                (stream-agent-run!
                  "/api/agent/stream"
                  request-body
                  (make-event-handler run-id nil)
                  (fn [err]
                    (js/console.error "[AGENT][CLIENT][STREAM-ERROR]" err)
                    (reset! !agent-output {:status :failed
                                           :provider provider
                                           :prompt prompt
                                           :output (str "Stream error: " (.-message err))
                                           :run-id run-id
                                           :trail []
                                           :tool-buf {}}))))

              ;; === Flow commands (V0 state machine) ===

              :extract-component
              (jit/handle-jit-command! parsed
                                       {:!extract-preview !extract-preview
                                        :show-flow-info! show-flow-info!})

              :hardcode
              (jit/handle-jit-command! parsed
                                       {:!extract-preview !extract-preview
                                        :show-flow-info! show-flow-info!})

              (:flow-bootstrap :flow-mock-bootstrap :flow-select :flow-arrange :flow-run
               :flow-review :flow-rework :flow-finalize :flow-status :flow-reset)
              (dg/handle-dg-command! parsed
                                     {:!flow-state !flow-state
                                      :!scroll-y !scroll-y
                                      :show-flow-info! show-flow-info!
                                      :fire-flow-run! fire-flow-run!}))))



        ;; Initial sidebar data fetch
        _ (when (and !sidebar-visible @!sidebar-visible)
            (fetch-home-dirs!))
        ;; Fetch data when sidebar becomes visible
        _ (when !sidebar-visible
            (add-watch !sidebar-visible :sidebar-fetch
                       (fn [_ _ old-vis new-vis]
                         (when (and new-vis (not old-vis))
                           (fetch-home-dirs!)))))

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
                  (swap! !sidebar-state assoc
                         :project {:name (last (str/split project-path #"/"))
                                   :path project-path}
                         :expanded-dirs (set dir-paths))
                  ;; Fetch root dir + expanded dirs so tree renders with content
                  (fetch-dir! project-path)
                  (doseq [dp dir-paths]
                    (fetch-dir! dp))))))

        ;; =====================================================================
        ;; LAYER 2: EVENT FLOWS
        ;; =====================================================================
        ;; IMPORTANT: These must be fresh flows, not shared top-level defs!

        >raf (events/make-raf-flow)              ;; Fresh RAF flow for this instance
        >blink-timer (events/make-blink-timer)   ;; Fresh blink timer for this instance
        >shimmer-timer (events/make-blink-timer) ;; Separate blink for tool-card shimmer
        >resize (events/>canvas-resize node)
        >wheel-events (events/>wheel node)
        >mouse-events (events/>mouse node)
        >keyboard-events (events/>keyboard js/window)

        ;; RAW DOM drag-select: bypasses Missionary's async scheduling so every
        ;; mousemove is captured synchronously while the mouse button is held.
        ;; Uses !drag-start atom to avoid race with Missionary's async mousedown.
        !drag-start (atom nil)
        _ (let [get-coords (fn [e]
                             (let [rect (.getBoundingClientRect node)]
                               {:x (- (.-clientX e) (.-left rect))
                                :y (- (.-clientY e) (.-top rect))}))
               mouse->pos (fn [{:keys [x y]}]
                            (let [scroll-y @!scroll-y
                                  sb-w (if (and !sidebar-visible @!sidebar-visible) sidebar-w 0)
                                  local-x (- x sb-w)
                                  dpr (:dpr @!viewport)
                                  snap? (:snap-to-pixel? @!settings)
                                  font-size (:font-size @!settings)
                                  char-width (:char-width @!active-font)
                                  line-h (maybe-snap (* font-size (:line-height @!settings)) dpr snap?)
                                  char-w (maybe-snap (* font-size char-width) dpr snap?)
                                  adj-y (+ y scroll-y)
                                  lx (maybe-snap (- layout-x (or @!scroll-x 0)) dpr snap?)
                                  ly (maybe-snap layout-y dpr snap?)
                                  text-result @!text-geo
                                  line-mapping (or (:line-mapping text-result) [])
                                  lengths (mapv count (:lines @!editor-doc))
                                  visual-line (max 0 (Math/floor (/ (- adj-y ly) line-h)))
                                  logical-line (get line-mapping visual-line
                                                    (min visual-line (dec (count lengths))))
                                  line-len (get lengths logical-line 0)
                                  col (-> (/ (- local-x lx) char-w)
                                          (Math/round)
                                          (max 0)
                                          (min line-len))]
                              {:line logical-line :col col}))]
            (.addEventListener node "mousedown"
              (fn [e]
                (let [coords (get-coords e)
                      {:keys [x]} coords
                      sb-w (if (and !sidebar-visible @!sidebar-visible) sidebar-w 0)
                      local-x (- x sb-w)
                      content-w (- (:width @!viewport) sb-w)
                      code-w (int (* content-w 0.4))
                      in-editor? (and (some? (:path @!current-file)) (< local-x code-w))
                      in-normal-editor? (and (nil? (:path @!current-file))
                                             (not (flow-canvas-active? @!flow-state)))]
                  (when (or in-editor? in-normal-editor?)
                    (let [pos (mouse->pos coords)]
                      (reset! !drag-start pos)
                      (reset! !dragging? true)
                      (swap! !editor-doc assoc
                             :cursor pos :selection nil :desired-col (:col pos))
                      (reset! !focus :editor)
                      (reset! !caret-visible true))))))
            (.addEventListener js/window "mousemove"
              (fn [e]
                (when @!dragging?
                  (let [pos (mouse->pos (get-coords e))
                        start @!drag-start]
                    (when (and start (not= pos start))
                      (swap! !editor-doc assoc
                             :selection {:start start :end pos}))))))
            (.addEventListener js/window "mouseup"
              (fn [_]
                (reset! !dragging? false)
                (reset! !drag-start nil))))

        ;; =====================================================================
        ;; LAYER 4: FOCUS-BASED EVENT ROUTING
        ;; =====================================================================

        <global-keys (events/<global-events >keyboard-events)
        <editor-keyboard (events/<editor-keys >keyboard-events !focus)
        <cmd-keyboard (events/<cmd-panel-keys >keyboard-events !focus)
        <chat-keyboard (events/<chat-input-keys >keyboard-events !focus)
        <settings-keyboard (events/<settings-panel-keys >keyboard-events !focus)

        ;; System clipboard paste: listen for native paste event (fired by Ctrl+V)
        _ (let [paste-handler
                (fn [e]
                  (let [text (.getData (.-clipboardData e) "text/plain")]
                    (when (seq text)
                      (.preventDefault e)
                      (reset! !clipboard text)
                      (case @!focus
                        :editor
                        (let [doc @!editor-doc
                              lengths (mapv count (:lines doc))]
                          (save-undo! (:lines doc) (:cursor doc))
                          (let [new-doc (editor-apply-event doc {:type :paste} lengths text)]
                            (reset! !editor-doc new-doc)
                            (reset! !caret-visible true)))
                        :command-panel
                        (let [panel @!cmd-panel
                              new-panel (cmd-panel-apply-event panel {:type :paste} text)]
                          (reset! !cmd-panel new-panel)
                          (reset! !caret-visible true))
                        :chat
                        (let [ci @!chat-input
                              ci-text (:text ci)
                              ci-cursor (:cursor ci)
                              new-text (str (subs ci-text 0 ci-cursor) text (subs ci-text ci-cursor))]
                          (reset! !chat-input {:text new-text :cursor (+ ci-cursor (count text))})
                          (reset! !caret-visible true))
                        nil))))]
            (.addEventListener js/window "paste" paste-handler))]

    ;; =====================================================================
    ;; AUTO-BOOTSTRAP (V0 — fire once on load if idle)
    ;; =====================================================================
    ;; Semantics (per Codex review):
    ;;   Fresh load:                auto-bootstrap
    ;;   Resume with cached state:  skip bootstrap (atom persists on hot-reload)
    ;;   Resume without cached:     bootstrap (atom reset to idle)
    ;; Guard: only fires if flow state is :idle AND no session-id cached.
    ;; Auto-bootstrap disabled — user triggers /bootstrap manually from cmd panel
    ;; (was: fire once on fresh load if idle + no session-id)

    (m/join vector

      ;; =====================================================================
      ;; BLINK TIMER CONSUMER
      ;; =====================================================================
      (->> >blink-timer
           (m/reduce (fn [_ v] (reset! !caret-visible v) nil) nil))

      ;; =====================================================================
      ;; SHIMMER TIMER CONSUMER (pulses pending tool cards)
      ;; =====================================================================
      (->> >shimmer-timer
           (m/reduce (fn [_ v] (reset! !shimmer-phase v) nil) nil))

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
           (m/reduce (fn [_ wheel-evt]
                       (let [delta (:dy wheel-evt 0)
                             dx (:dx wheel-evt 0)
                             shift? (:shift? wheel-evt)
                             viewport @!viewport
                             settings @!settings
                             dpr (:dpr viewport)
                             snap? (:snap-to-pixel? settings)
                             font-size (:font-size settings)
                             char-advance (* font-size (:char-width @!active-font))
                             sb-vis? (and !sidebar-visible @!sidebar-visible)
                             mouse-x @!mouse-x
                             in-sidebar? (and sb-vis? (< mouse-x sidebar-w))
                             agent-output @!agent-output
                             agent-h (if (some? (:path @!current-file))
                                       0 ;; Suppress agent panel when 3-pane layout active
                                       (compute-agent-panel-h agent-output font-size
                                                              (:height viewport) (:width viewport)
                                                              char-advance))
                             ;; Agent panel Y bounds (viewport-relative, no scroll offset)
                             agent-y0 (- (:height viewport) cmd-panel-h status-bar-h agent-h 12)
                             agent-y1 (- (:height viewport) cmd-panel-h status-bar-h)
                             mouse-y @!mouse-y
                             in-agent? (and (not in-sidebar?)
                                            (pos? agent-h)
                                            (>= mouse-y agent-y0)
                                            (< mouse-y agent-y1))]
                         (cond
                           ;; Scroll sidebar file tree
                           in-sidebar?
                           (let [ss @!sidebar-state
                                 content-h (compute-sidebar-content-height ss @!current-file)
                                 visible-h (- (:height viewport) sidebar-tab-h)
                                 max-scroll (max 0 (- content-h visible-h))]
                             (swap! !sidebar-state update :scroll-y
                                    #(-> (+ (or % 0) delta) (max 0) (min max-scroll))))
                           ;; Scroll agent panel
                           in-agent?
                           (let [line-step (* font-size 1.2)
                                 max-chars (if (pos? char-advance)
                                             (max 1 (int (/ (- (:width viewport) 48) char-advance)))
                                             80)
                                 line-count (agent-wrapped-line-count agent-output max-chars)
                                 total-h (* line-count line-step)
                                 max-scroll (max 0 (- total-h (- agent-h 16)))]
                             (swap! !agent-scroll-y
                                    #(-> (+ % delta) (max 0) (min max-scroll))))
                           ;; Scroll chat pane when mouse is over it (3-pane mode)
                           (let [file-open? (some? (:path @!current-file))
                                 sb-off (if sb-vis? sidebar-w 0)
                                 cw (- (:width viewport) sb-off)
                                 code-w (int (* cw 0.4))
                                 chat-w (int (* cw 0.55))
                                 rel-mx (- mouse-x sb-off)
                                 in-chat? (and file-open? (>= rel-mx code-w) (< rel-mx (+ code-w chat-w)))]
                             (and file-open? in-chat?))
                           (swap! !chat-scroll-y #(max 0 (+ % delta)))

                           ;; Scroll editor / flow canvas
                           :else
                           (if (flow-canvas-active? @!flow-state)
                             ;; List view: clamp scroll to grouped list content height
                             (let [flow @!flow-state
                                   grouped (group-tickets-by-status (:tickets flow))
                                   content-h (list-content-height grouped @!collapsed-groups)
                                   visible-h (- (:height viewport) cmd-panel-h status-bar-h agent-h 12)
                                   max-scroll (max 0 (- content-h visible-h))]
                               (swap! !scroll-y #(-> (+ % delta) (max 0) (min max-scroll))))
                             ;; Normal editor scroll: vertical + horizontal (skip if mouse over preview pane)
                             (when (or (not (some? (:path @!current-file)))
                                       (< (- mouse-x (if sb-vis? sidebar-w 0))
                                          (int (* (- (:width viewport) (if sb-vis? sidebar-w 0)) 0.4))))
                             (do (swap! !scroll-y #(maybe-snap (+ % delta) dpr snap?))
                                 ;; Horizontal scroll: only within editor pane
                                 (let [h-delta (if shift? delta dx)
                                       sb-off (if sb-vis? sidebar-w 0)
                                       cw (- (:width viewport) sb-off)
                                       editor-right (if (some? @!current-file)
                                                      (+ sb-off (int (* cw 0.4)))
                                                      (+ sb-off cw))]
                                   (when (and (not (zero? h-delta)) (< mouse-x editor-right))
                                     (swap! !scroll-x #(max 0 (+ (or % 0) h-delta))))))))))
                       nil) nil))

      ;; =====================================================================
      ;; MOUSE EVENTS CONSUMER
      ;; =====================================================================
      (->> >mouse-events
           (m/reduce
             (fn [_ [type coords]]
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
                           slider-item-h 58
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

                     ;; Not in settings - check sidebar, status bar, command panel, or editor
                     (let [sb-vis? (and !sidebar-visible @!sidebar-visible)
                           clicked-in-sidebar? (and sb-vis? (< x sidebar-w))]
                       (if clicked-in-sidebar?
                         ;; Click in sidebar — hit-test the sidebar tree
                         (let [ss @!sidebar-state
                               font-size (:font-size @!settings)
                               char-advance (* font-size (:char-width @!active-font))
                               tree (resolve-layout
                                      (build-sidebar-tree ss @!current-file true
                                                          (:height viewport) scroll-y font-size char-advance))
                               path (when tree (hit-test tree x (+ y scroll-y)))]
                           (when path
                             (some (fn [node]
                                     (case (:type node)
                                       ;; File/dir entry click
                                       :sidebar-entry
                                       (let [d (:data node)
                                             et (:entry-type d)]
                                         (case et
                                           :back-btn
                                           (do (swap! !sidebar-state assoc
                                                      :project nil
                                                      :expanded-dirs #{}
                                                      :dir-cache {}
                                                      :scroll-y 0)
                                               (reset! !current-file nil)
                                               true)
                                           :home-dir
                                           (let [entry (:entry d)]
                                             (swap! !sidebar-state assoc
                                                    :project {:name (:name entry) :path (:path entry)}
                                                    :expanded-dirs #{}
                                                    :dir-cache {}
                                                    :scroll-y 0)
                                             (fetch-dir! (:path entry))
                                             true)
                                           :dir
                                           (let [entry (:entry d)
                                                 path (:path entry)]
                                             (swap! !sidebar-state update :expanded-dirs
                                                    (fn [dirs]
                                                      (if (contains? dirs path)
                                                        (disj dirs path)
                                                        (conj dirs path))))
                                             (fetch-dir! path)
                                             true)
                                           :file
                                           (let [entry (:entry d)
                                                 project (:project @!sidebar-state)]
                                             (fetch-file! (:path entry) (:path project))
                                             true)
                                           ;; Unknown entry type
                                           nil))
                                       ;; Other node types — skip
                                       nil))
                                   (rseq path))))
                     (let [status-bar-top (- (:height viewport) status-bar-h)
                           clicked-in-status? (>= y status-bar-top)]
                       (if clicked-in-status?
                         nil ;; Clicks in status bar are no-ops
                     (let [cmd-panel @!cmd-panel
                           cmd-visible? (or (:visible cmd-panel)
                                            (some? @!current-file)
                                            (dg/flow-canvas-active? @!flow-state))
                           cmd-panel-top (if cmd-visible?
                                           (- (:height viewport) cmd-panel-h status-bar-h)
                                           (:height viewport))
                           clicked-in-cmd? (and cmd-visible? (>= y cmd-panel-top) (< y status-bar-top))]

                       (if clicked-in-cmd?
                         ;; Click in command panel - use reactive font values (offset by sidebar)
                         (let [font-size (:font-size @!settings)
                              dpr (:dpr @!viewport)
                              snap? (:snap-to-pixel? @!settings)
                              char-width (:char-width @!active-font)
                              char-w (maybe-snap (* font-size char-width) dpr snap?)
                              sb-w (if sb-vis? sidebar-w 0)
                              text-x (+ (cmd-text-start-x @!ai-provider font-size char-width dpr snap?) sb-w)
                              text (:text cmd-panel)
                              col (-> (/ (- x text-x) char-w)
                                       (Math/round)
                                       (max 0)
                                       (min (count text)))]
                           (reset! !focus :command-panel)
                           (swap! !cmd-panel assoc :cursor col)
                           (reset! !caret-visible true))
                        ;; Click in editor area — check flow canvas, then editor
                        (do
                        (when (:visible @!cmd-panel)
                          (swap! !cmd-panel assoc :visible false))
                        (cond
                          ;; Flow canvas mode: rect tree hit-test
                          (flow-canvas-active? @!flow-state)
                          (let [flow @!flow-state
                                tree (resolve-layout
                                       (build-intake-tree flow (:width viewport) (:height viewport)
                                                          scroll-y nil @!hovered-row-idx @!collapsed-groups 0 0 nil))
                                path (hit-test tree x (+ y scroll-y))]
                            ;; Walk path innermost→outermost, handle first recognized type
                            (when path
                              (some (fn [node]
                                      (case (:type node)
                                        :group-header
                                        ;; Immediate — not draggable
                                        (let [status (:status (:data node))]
                                          (swap! !collapsed-groups
                                                 (fn [cg] (if (contains? cg status)
                                                            (disj cg status)
                                                            (conj cg status))))
                                          true)
                                        :ticket-row
                                        ;; Enter PENDING — defer click vs drag to mouseup/mousemove
                                        (do (reset! !drag-state
                                                    {:phase :pending
                                                     :origin {:x x :y y}
                                                     :node node})
                                            true)
                                        ;; Other node types — skip, let it bubble
                                        nil))
                                    (rseq path))))

                          ;; File layout mode: set active pane + click in chat → toggle tool card collapse
                          (some? @!current-file)
                          (let [content-w (- (:width viewport) (if sb-vis? sidebar-w 0))
                                code-w (int (* content-w 0.4))
                                chat-w (int (* content-w 0.55))
                                ;; Adjust x relative to content area (subtract sidebar)
                                rel-x (- x (if sb-vis? sidebar-w 0))
                                in-chat? (and (>= rel-x code-w) (< rel-x (+ code-w chat-w)))
                                in-editor? (< rel-x code-w)
                                ;; Focus hierarchy: set active pane based on click position
                                clicked-pane (cond
                                               in-editor? :editor
                                               in-chat? :chat
                                               :else :preview)]
                            (reset! !active-pane clicked-pane)
                            ;; Editor clicks: place cursor + set focus (same as normal mode)
                            (when in-editor?
                              (let [adj-y (+ y scroll-y)
                                    local-x (- x (if sb-vis? sidebar-w 0))
                                    dpr (:dpr @!viewport)
                                    snap? (:snap-to-pixel? @!settings)
                                    font-size (:font-size @!settings)
                                    char-width (:char-width @!active-font)
                                    line-h (maybe-snap (* font-size (:line-height @!settings)) dpr snap?)
                                    char-w (maybe-snap (* font-size char-width) dpr snap?)
                                    elx (maybe-snap (- layout-x (or @!scroll-x 0)) dpr snap?)
                                    ely (maybe-snap layout-y dpr snap?)
                                    gutter-x (- elx gutter-w)
                                    gutter-right (+ gutter-x gutter-w)]
                                (if (and (>= local-x gutter-x) (< local-x gutter-right))
                                  ;; Gutter click - toggle fold
                                  (let [text-result @!text-geo
                                        line-mapping (or (:line-mapping text-result) [])
                                        visual-line (max 0 (Math/floor (/ (- adj-y ely) line-h)))
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
                                                 (conj folded logical-line))))))
                                  ;; Normal click - place cursor
                                  (let [text-result @!text-geo
                                        line-mapping (or (:line-mapping text-result) [])
                                        lengths (mapv count (:lines @!editor-doc))
                                        visual-line (max 0 (Math/floor (/ (- adj-y ely) line-h)))
                                        logical-line (get line-mapping visual-line
                                                          (min visual-line (dec (count lengths))))
                                        line-len (get lengths logical-line 0)
                                        col (-> (/ (- local-x elx) char-w)
                                                (Math/round)
                                                (max 0)
                                                (min line-len))
                                        pos {:line logical-line :col col}]
                                    ;; Skip if raw DOM handler already processed this click
                                    (when-not @!drag-start
                                      (swap! !editor-doc assoc
                                             :cursor pos
                                             :selection nil
                                             :desired-col col)
                                      (reset! !caret-visible true)
                                      (reset! !focus :editor))))))
                            (when in-chat?
                              ;; Always focus chat pane on click (prevents editor caret bleed-through)
                              (reset! !focus :chat)
                              (reset! !caret-visible true)
                              ;; Hit-test upper chat area for tool-header + nav clicks
                              (let [file-layout-h (- (:height viewport) cmd-panel-h status-bar-h)]
                                (when (< y (- file-layout-h 36))
                                  (let [font-size (:font-size @!settings)
                                      char-advance (* font-size (:char-width @!active-font))
                                      shimmer-alpha (if @!shimmer-phase 0.9 0.4)
                                      tree (resolve-layout
                                             (build-file-layout content-w file-layout-h
                                                                @!current-file @!agent-output font-size
                                                                shimmer-alpha @!trail-collapsed
                                                                :active-pane @!active-pane :char-advance char-advance
                                                                :chat-scroll-y (or @!chat-scroll-y 0)
                                                                :chat-input @!chat-input :focus @!focus))
                                    path (hit-test tree rel-x y)]
                                (when path
                                  ;; Priority 1: navigate (tool cards with :nav file path)
                                  (let [handled-nav?
                                        (some (fn [node]
                                                (when-let [nav (:nav (:data node))]
                                                  (let [file-path (:file-path nav)
                                                        target-line (or (:line nav) 0)
                                                        current-path (:path @!current-file)
                                                        project (or (:path (:project @!sidebar-state)) flow-cwd)
                                                        same-file? (or (= file-path current-path)
                                                                       (and current-path
                                                                            (str/ends-with? current-path file-path)))]
                                                    (if same-file?
                                                      ;; Same file: reposition cursor + scroll
                                                      (let [safe-line (min target-line
                                                                          (max 0 (dec (count (:lines @!editor-doc)))))]
                                                        (swap! !editor-doc assoc
                                                               :cursor {:line safe-line :col 0}
                                                               :selection nil :desired-col 0)
                                                        (let [line-h (* font-size (:line-height @!settings))]
                                                          (reset! !scroll-y (max 0 (- (* safe-line line-h) 100)))))
                                                      ;; Different file: load with target-line
                                                      (fetch-file! file-path project :target-line target-line))
                                                    (reset! !active-pane :editor)
                                                    true)))
                                              (rseq path))]
                                    ;; Priority 2: collapse toggle (thinking/grep cards without nav)
                                    (when-not handled-nav?
                                      (some (fn [node]
                                              (when (= :tool-header (:type node))
                                                (when-let [cid (:collapse-id (:data node))]
                                                  (swap! !trail-collapsed
                                                         (fn [s] (if (contains? s cid)
                                                                    (disj s cid)
                                                                    (conj s cid))))
                                                  true)))
                                            (rseq path))))))))))

                          ;; Normal editor mode: cursor placement / fold toggle
                          :else
                          (let [adj-y (+ y scroll-y)
                                local-x (- x (if sb-vis? sidebar-w 0))
                                dpr (:dpr @!viewport)
                                snap? (:snap-to-pixel? @!settings)
                                font-size (:font-size @!settings)
                                char-width (:char-width @!active-font)
                                line-h (maybe-snap (* font-size (:line-height @!settings)) dpr snap?)
                                char-w (maybe-snap (* font-size char-width) dpr snap?)
                                layout-x (maybe-snap (- layout-x (or @!scroll-x 0)) dpr snap?)
                                layout-y (maybe-snap layout-y dpr snap?)
                                gutter-x (- layout-x gutter-w)
                                gutter-right (+ gutter-x gutter-w)]
                            (if (and (>= local-x gutter-x) (< local-x gutter-right))
                              ;; Gutter click - toggle fold
                              (let [text-result @!text-geo
                                    line-mapping (or (:line-mapping text-result) [])
                                    visual-line (max 0 (Math/floor (/ (- adj-y layout-y) line-h)))
                                    logical-line (get line-mapping visual-line visual-line)
                                    regions (detect-folds-fn (:lines @!editor-doc)
                                                             (mapv count (:lines @!editor-doc)))
                                    fold-region (first (filter #(= (:start-line %) logical-line)
                                                               (or regions [])))]
                                (js/console.log "[FOLD] visual:" visual-line "logical:" logical-line
                                                "region:" (clj->js fold-region)
                                                "folded-before:" (clj->js @!folded-lines))
                                (when fold-region
                                  (swap! !folded-lines
                                         (fn [folded]
                                           (if (contains? folded logical-line)
                                             (disj folded logical-line)
                                             (conj folded logical-line))))
                                  (js/console.log "[FOLD] folded-after:" (clj->js @!folded-lines)))
                                (reset! !focus :editor))
                              ;; Normal click - place cursor
                              (let [text-result @!text-geo
                                    line-mapping (or (:line-mapping text-result) [])
                                    lengths (mapv count (:lines @!editor-doc))
                                    visual-line (max 0 (Math/floor (/ (- adj-y layout-y) line-h)))
                                    logical-line (get line-mapping visual-line
                                                      (min visual-line (dec (count lengths))))
                                    line-len (get lengths logical-line 0)
                                    col (-> (/ (- local-x layout-x) char-w)
                                            (Math/round)
                                            (max 0)
                                            (min line-len))
                                    pos {:line logical-line :col col}]
                                ;; Skip if raw DOM handler already processed this click
                                (when-not @!drag-start
                                  (swap! !editor-doc assoc
                                         :cursor pos
                                         :selection nil
                                         :desired-col col)
                                  (reset! !caret-visible true)
                                  (reset! !focus :editor))))))))))))))

                 :mousemove
                 (do (reset! !mouse-x (:x coords))
                     (reset! !mouse-y (:y coords))
                 ;; --- Sidebar hover tracking ---
                 (let [sb-vis? (and !sidebar-visible @!sidebar-visible)
                       in-sidebar? (and sb-vis? (< (:x coords) sidebar-w))]
                   (if in-sidebar?
                     ;; Hit-test sidebar for hover
                     (let [ss @!sidebar-state
                           font-size (:font-size @!settings)
                           char-advance (* font-size (:char-width @!active-font))
                           tree (resolve-layout
                                  (build-sidebar-tree ss @!current-file true
                                                      (:height @!viewport) @!scroll-y font-size char-advance))
                           path (when tree (hit-test tree (:x coords) (+ (:y coords) @!scroll-y)))
                           target (peek path)
                           new-id (when (and target (#{:sidebar-entry :ticket-row} (:type target)))
                                    (:id target))]
                       (when (not= new-id (:hovered-id @!sidebar-state))
                         (swap! !sidebar-state assoc :hovered-id new-id)))
                     ;; Clear sidebar hover when outside
                     (when (:hovered-id @!sidebar-state)
                       (swap! !sidebar-state assoc :hovered-id nil))))
                 ;; --- Drag state machine transitions ---
                 (let [ds @!drag-state
                       mx (:x coords) my (:y coords)]
                   (case (:phase ds)
                     :pending
                     (when (> (drag-distance ds mx my) drag-threshold-px)
                       (reset! !drag-state
                               {:phase :dragging
                                :origin (:origin ds)
                                :node (:node ds)
                                :current {:x mx :y my}}))
                     :dragging
                     (swap! !drag-state assoc :current {:x mx :y my})
                     nil))
                 ;; Hover tracking — suppress during drag
                 (when (and (flow-canvas-active? @!flow-state)
                            (= :idle (:phase @!drag-state)))
                   (let [flow @!flow-state
                         tree (resolve-layout
                                (build-intake-tree flow (:width @!viewport) (:height @!viewport)
                                                   @!scroll-y nil @!hovered-row-idx @!collapsed-groups 0 0 nil))
                         path (hit-test tree (:x coords) (+ (:y coords) @!scroll-y))
                         target (peek path)]
                     (reset! !hovered-row-idx
                             (when (and target (= (:type target) :ticket-row))
                               (:idx (:data target))))))
                 ;; Drag-select handled by raw DOM listeners (see above) --
                 ;; Missionary's async scheduling loses mousemove events.
                 nil)

                 :mouseup
                 (let [ds @!drag-state]
                   (case (:phase ds)
                     :pending
                     ;; Under threshold — treat as click (toggle selection)
                     (do (let [node (:node ds)]
                           (when (= :ticket-row (:type node))
                             (let [idx (:idx (:data node))
                                   flow @!flow-state
                                   selected (:selected flow)
                                   already? (some #{idx} selected)
                                   new-sel (if already?
                                             (vec (remove #{idx} selected))
                                             (conj (vec selected) idx))]
                               (swap! !flow-state assoc :selected new-sel))))
                         (reset! !drag-state {:phase :idle}))
                     :dragging
                     ;; Past threshold — check drop zone
                     (let [node (:node ds)
                           cur (:current ds)
                           left-w (int (* (:width @!viewport) list-left-pane-pct))]
                       (when (and node cur (= :ticket-row (:type node)))
                         (if (>= (:x cur) left-w)
                           ;; Dropped on right pane → select ticket (cross-panel DnD)
                           (let [idx (:idx (:data node))
                                 flow @!flow-state
                                 selected (:selected flow)
                                 already? (some #{idx} selected)]
                             (when-not already?
                               (swap! !flow-state assoc :selected
                                      (conj (vec selected) idx))))
                           ;; Dropped on left pane → reorder (future)
                           nil))
                       (reset! !drag-state {:phase :idle}))
                     ;; :idle — drag-select managed by raw DOM listeners (see above)
                     nil)))
               nil)
             nil)))

      ;; =====================================================================
      ;; GLOBAL KEYBOARD EVENTS CONSUMER
      ;; =====================================================================
      (->> <global-keys
           (m/reduce
             (fn [_ event]
               (when event
                 (case (:type event)
                   :toggle-command-panel
                   (let [visible? (:visible @!cmd-panel)
                         file-open? (some? (:path @!current-file))]
                     (if (and visible? (not file-open?))
                       (do (swap! !cmd-panel assoc :visible false)
                           (reset! !focus :editor))
                       (do (swap! !cmd-panel assoc :visible true)
                           (swap! !settings assoc :visible false)  ;; Close settings if open
                           (reset! !focus :command-panel)
                           (reset! !caret-visible true))))

                   :toggle-settings-panel
                   (let [visible? (:visible @!settings)]
                     (if visible?
                       (do (swap! !settings assoc :visible false)
                           (reset! !focus :editor))
                       (do (swap! !settings assoc :visible true)
                           (swap! !cmd-panel assoc :visible false)
                           (reset! !focus :settings-panel))))

                   :escape
                   (cond
                     (:visible @!settings)
                     (do (swap! !settings assoc :visible false)
                         (reset! !focus :editor))

                     (= @!focus :chat)
                     (do (reset! !chat-input {:text "" :cursor 0})
                         (reset! !focus :editor))

                     (or (:visible @!cmd-panel) (some? (:status @!agent-output)))
                     (if (some? (:path @!current-file))
                       ;; File open: just unfocus (panel stays visible)
                       (reset! !focus :editor)
                       ;; No file: hide panel + clear agent output
                       (do (swap! !cmd-panel assoc :visible false)
                           (reset! !agent-output nil)
                           (reset! !focus :editor)))

                     (and !sidebar-visible @!sidebar-visible)
                     (reset! !sidebar-visible false)

                     :else
                     (swap! !editor-doc assoc :selection nil))

                   :toggle-file-viewer
                   (when !sidebar-visible
                     (swap! !sidebar-visible not))

                   :focus-pane
                   (when (some? @!current-file)
                     (let [pane (:pane event)]
                       (reset! !active-pane pane)
                       (when (= pane :chat)
                         (reset! !focus :chat)
                         (reset! !caret-visible true))
                       (when (= pane :editor)
                         (reset! !focus :editor))))

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
      (cond
        !file-load-request
        (->> (m/watch !file-load-request)
             (m/eduction (filter some?))
             (m/reduce
               (fn [_ request]
                 (let [{:keys [lines target-line]} request
                       target-line (or target-line 0)]
                   (when (seq lines)
                     (let [safe-line (min target-line (max 0 (dec (count lines))))]
                       (js/console.log "[FILE-LOAD] Loading file with" (count lines) "lines, target:" safe-line)
                       (reset! !editor-doc {:lines (vec lines)
                                            :cursor {:line safe-line :col 0}
                                            :selection nil
                                            :desired-col 0})
                       ;; Scroll to target line with ~100px top margin
                       (let [font-size (:font-size @!settings)
                             line-h (* font-size (:line-height @!settings))
                             target-y (* safe-line line-h)]
                         (reset! !scroll-y (max 0 (- target-y 100))))
                       (reset! !scroll-x 0)
                       (reset! !undo-stack [])
                       (reset! !redo-stack [])
                       (reset! !folded-lines #{})
                       (reset! !caret-visible true)
                       ;; Only steal focus if command panel is not open
                       (when-not (:visible @!cmd-panel)
                         (reset! !focus :editor))
                       ;; Clear the request so same file can be re-opened
                       (reset! !file-load-request nil))))
                 nil)
               nil))
        ;; No-op task when !file-load-request not provided
        :else
        (m/reduce (fn [_ _] nil) nil (m/seed [nil])))

      ;; =====================================================================
      ;; EDITOR KEYBOARD EVENTS CONSUMER
      ;; =====================================================================
      (->> <editor-keyboard
           (m/reduce
             (fn [_ event]
               (when event
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
      (m/reduce
        (fn [_ event]
          (when event
            (let [tickets (:tickets @!flow-state)
                  has-tickets? (seq tickets)
                  ;; Visual order: ticket indices in grouped display order
                  visual-order (when has-tickets?
                                 (vec (mapcat (fn [g] (mapv :idx (:tickets g)))
                                              (group-tickets-by-status tickets))))
                  n-visual (count visual-order)]
              (case (:type event)
                :enter
                (let [cmd-text (:text @!cmd-panel)]
                  (if (seq cmd-text)
                    ;; Submit command, clear text, keep panel open for follow-up
                    (do (submit-agent-run! cmd-text)
                        (swap! !cmd-panel assoc :text "" :cursor 0))
                    ;; Empty Enter with hovered ticket = toggle selection
                    (if (and has-tickets? @!hovered-row-idx)
                      (let [idx @!hovered-row-idx]
                        (swap! !flow-state update :selected
                               (fn [sel]
                                 (if (some #{idx} sel)
                                   (vec (remove #{idx} sel))
                                   (conj (vec sel) idx)))))
                      ;; No tickets or no hover = unfocus (close if no file open)
                      (do (when-not (some? (:path @!current-file))
                            (swap! !cmd-panel assoc :text "" :cursor 0 :visible false))
                          (reset! !focus :editor)))))

                ;; Up/Down: navigate ticket list in visual (grouped) order
                :up
                (when has-tickets?
                  (let [cur-idx @!hovered-row-idx
                        vis-pos (when cur-idx
                                  (some (fn [[i v]] (when (= v cur-idx) i))
                                        (map-indexed vector visual-order)))
                        new-pos (if (nil? vis-pos)
                                  (dec n-visual)
                                  (mod (dec vis-pos) n-visual))]
                    (reset! !hovered-row-idx (nth visual-order new-pos))))

                :down
                (when has-tickets?
                  (let [cur-idx @!hovered-row-idx
                        vis-pos (when cur-idx
                                  (some (fn [[i v]] (when (= v cur-idx) i))
                                        (map-indexed vector visual-order)))
                        new-pos (if (nil? vis-pos)
                                  0
                                  (mod (inc vis-pos) n-visual))]
                    (reset! !hovered-row-idx (nth visual-order new-pos))))

                ;; Edit/navigation operations (char, backspace, left, right, etc.)
                (let [panel @!cmd-panel
                      new-panel (cmd-panel-apply-event panel event @!clipboard)]
                  (reset! !cmd-panel new-panel)
                  (reset! !caret-visible true)))))
          nil)
        nil
        <cmd-keyboard)

      ;; =====================================================================
      ;; CHAT INPUT KEYBOARD EVENTS CONSUMER
      ;; =====================================================================
      (m/reduce
        (fn [_ event]
          (when event
            (case (:type event)
              :enter
              (let [cmd-text (:text @!chat-input)]
                (when (seq cmd-text)
                  (submit-agent-run! cmd-text)
                  (reset! !chat-input {:text "" :cursor 0})))

              :escape
              (do (reset! !chat-input {:text "" :cursor 0})
                  (reset! !focus :editor)
                  (reset! !active-pane :editor))

              (:up :down)
              nil ;; no-op for now (could scroll chat later)

              ;; Edit/navigation: reuse cmd-panel-apply-event pure fn
              (let [panel @!chat-input
                    new-panel (cmd-panel-apply-event panel event @!clipboard)]
                (reset! !chat-input new-panel)
                (reset! !caret-visible true))))
          nil)
        nil
        <chat-keyboard)

      ;; =====================================================================
      ;; SETTINGS PANEL KEYBOARD EVENTS CONSUMER
      ;; =====================================================================
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
                      :px-range    (swap! !settings update :px-range #(min 32 (inc %)))
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
        nil
        <settings-keyboard)

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

            <text-data (<combined-text-ops !editor-doc !cmd-panel !ai-provider !agent-output !agent-scroll-y !scroll-y !viewport !settings !active-font
                                           !current-file
                                           tokenize-fn layout-fn
                                           <fold-data
                                           !flow-state !collapsed-groups !hovered-row-idx !drag-state
                                           !sidebar-state !sidebar-visible !extract-preview
                                           !shimmer-phase !trail-collapsed !active-pane !scroll-x !chat-scroll-y !chat-input !focus !run-scroll-y !detail-scroll-y
                                           dg/flow-canvas-active? dg/compute-ticket-list-text-ops dg/compute-run-text-ops dg/offset-text-ops
                                           layout-x layout-y cmd-panel-h status-bar-h)
            <editor-rect-data (<editor-rects !editor-doc !eval-result !caret-visible !focus
                                             !settings !active-font !viewport
                                             <fold-data <bracket-data
                                             !flow-state !scroll-y !collapsed-groups !hovered-row-idx !drag-state
                                             !sidebar-state !sidebar-visible !current-file !extract-preview !agent-output
                                             !shimmer-phase !trail-collapsed !active-pane !scroll-x !chat-scroll-y !chat-input !run-scroll-y !detail-scroll-y
                                             dg/flow-canvas-active? dg/compute-ticket-list-rects dg/compute-run-rects dg/offset-rects dg/offset-shadows
                                             layout-x layout-y gutter-w)
            <cmd-rect-data (<cmd-panel-rects !cmd-panel !focus !caret-visible !scroll-y !viewport
                                             !settings !active-font
                                             !ai-provider !agent-output !sidebar-visible !sidebar-state
                                             !current-file !flow-state dg/flow-canvas-active?
                                             cmd-panel-h status-bar-h)
            ;; Settings panel flows (reactive: derive font-size from !settings internally)
            <settings-rect-data (<settings-panel-rects !settings !focus !viewport !scroll-y !font-manifest)
            <settings-text-data (<settings-panel-text !settings !viewport !scroll-y !font-manifest)

            ;; Combined world state - sampled on RAF
            ;; m/latest combines flows, m/sample synchronizes with frame clock
            ;; All derived flows now use m/latest internally, so they're continuous
            <world-snapshot (m/latest
                              (fn [text-data editor-rect-data cmd-rects settings-rects settings-text
                                   viewport scroll-y cmd-panel settings active-font agent-output
                                   current-file flow-state]
                                {:text-data text-data
                                 :editor-rect-data editor-rect-data
                                 :cmd-rects cmd-rects
                                 :settings-rects settings-rects
                                 :settings-text settings-text
                                 :viewport viewport
                                 :scroll-y scroll-y
                                 :cmd-visible (or (:visible cmd-panel)
                                                  (some? current-file)
                                                  (dg/flow-canvas-active? flow-state))
                                 :agent-visible (some? (:status agent-output))
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
                              (m/watch !active-font)
                              (m/watch !agent-output)
                              (m/watch !current-file)
                              (m/watch !flow-state))]

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
                (let [{:keys [text-data editor-rect-data cmd-rects settings-rects settings-text
                              viewport scroll-y cmd-visible agent-visible settings-visible
                              font-size px-range line-height sharpness char-width
                              snap-to-pixel? show-diagnostics?]} world
                      editor-rects   (:rects editor-rect-data)
                      editor-shadows (:shadows editor-rect-data)

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
                      render-line-count (count (:render-ops text-data))
                      editor-line-count (:editor-line-count text-data)
                      cmd-line-count (:cmd-line-count text-data)
                      settings-line-count (count (or settings-lines []))
                      diagnostics-line-index (when diagnostics-line
                                               (+ render-line-count settings-line-count))

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

                      ;; Upload shadows (only if changed)
                      new-shadow-sys (if (not (identical? editor-shadows (:prev-editor-shadows prev-state)))
                                       (editor/update-shadows device
                                                              (or (:shadow-sys prev-state) @!shadow-sys)
                                                              (or editor-shadows []))
                                       (:shadow-sys prev-state))

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
                                      :pre-settings-line-count render-line-count
                                      :settings-line-count settings-line-count
                                      :settings-visible settings-visible
                                      :settings-rect-sys new-settings-sys
                                      :diagnostics-visible show-diagnostics?
                                      :diagnostics-line-index diagnostics-line-index
                                      :agent-visible agent-visible
                                      :shadow-sys new-shadow-sys)

                  ;; Return state for next frame comparison
                  {:text-geo new-text-geo
                       :editor-rect-sys new-editor-sys
                       :cmd-rect-sys new-cmd-sys
                       :settings-rect-sys new-settings-sys
                       :shadow-sys new-shadow-sys
                       :prev-world world
                       :prev-text-data text-data
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

          ;; Initial state
          {:text-geo (:text geometry)
           :editor-rect-sys (:rect geometry)
           :cmd-rect-sys @!cmd-rect-sys
           :settings-rect-sys @!settings-rect-sys
           :shadow-sys @!shadow-sys
           :prev-world nil
           :prev-text-data nil
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

          ;; Sample world state on each RAF tick
          (m/sample vector <world-snapshot >raf))))))
