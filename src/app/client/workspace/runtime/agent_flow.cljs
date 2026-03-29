(ns app.client.workspace.runtime.agent-flow
  "Agent API: auto-scroll, event handler, prompt submission, flow runs, command dispatch."
  (:require [clojure.string :as str]
            [app.client.workspace.agent :refer [stream-agent-run!]]
            [app.client.workspace.cmd-panel :refer [parse-agent-command]]
            [app.client.workspace.trail :refer [compute-agent-panel-h agent-wrapped-line-count]]
            [app.client.workspace.runtime.sidebar-io :refer [save-agent-trail!]]
            [app.client.workflows.dg-flow :as dg :refer [flow-prompt]]
            [app.client.workflows.jit :as jit]))

;; V0: hardcoded to discourse-graph (per commission-consensus.md scope)
(def ^:private flow-cwd "/home/sid/projects/discourse-graph")
(def ^:private flow-allowed-tools
  ["mcp__linear-server__list_issues"
   "mcp__linear-server__get_issue"
   "mcp__linear-server__search_issues"
   "mcp__linear-server__list_projects"
   "mcp__linear-server__get_project"
   "mcp__linear-server__list_teams"])

(defn make-agent-api
  "Build agent API closures from atoms. Returns map of:
   {:auto-scroll-agent! :make-event-handler :fire-flow-run! :show-flow-info! :submit-agent-run!}
   trigger-dev-replay! is passed in from interop module."
  [{:keys [!agent-output !agent-scroll-y !viewport !settings !active-font
           !flow-state !ai-provider !editor-doc !current-file !scroll-y
           !sidebar-truth !sidebar-overlay !extract-preview]}
   trigger-dev-replay!]
  (let [auto-scroll-agent!
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
                nil

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
                    ;; Persist completed trail to Rama (fire-and-forget)
                    (let [ao @!agent-output]
                      (when (and run-id (:trail ao))
                        (save-agent-trail! run-id
                          {:status   (or (:status evt) :complete)
                           :provider (:provider ao)
                           :prompt   (:prompt ao)
                           :trail    (:trail ao)
                           :structured-result (:structured-result ao)})))
                    (when on-done-fn (on-done-fn)))

                (:start :run-start)
                (do (js/console.log "[AGENT][STREAM-START]" (clj->js evt))
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

                (js/console.warn "[AGENT][UNKNOWN-EVENT]" (clj->js evt))))))

        show-flow-info!
        (fn [msg]
          (reset! !agent-scroll-y 0)
          (reset! !agent-output {:status :complete
                                 :provider @!ai-provider
                                 :prompt "flow"
                                 :output msg
                                 :run-id nil}))

        fire-flow-run!
        (fn [prompt-action & {:keys [on-done json-schema max-budget-usd model append-system-prompt]}]
          (let [flow @!flow-state
                {:keys [prompt]} (flow-prompt prompt-action flow)
                provider @!ai-provider
                run-id (str (random-uuid))
                session-id (:session-id flow)
                request-body (cond-> {:run-id run-id
                                      :provider provider
                                      :prompt prompt
                                      :cwd flow-cwd
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
                cwd (or (:path (:pending-project @!sidebar-overlay))
                        (:path (:project @!sidebar-truth))
                        (some-> file-path (str/split #"/") butlast seq (str/join "/"))
                        ".")
                context {:cursor (:cursor doc)
                         :selection (:selection doc)
                         :visible-range [scroll-y (+ scroll-y (:height viewport))]
                         :file-path file-path
                         :timestamp (js/Date.now)}]
            (case (:kind parsed)
              :noop nil

              :replay (trigger-dev-replay!)

              :set-provider
              (do (reset! !ai-provider (:provider parsed))
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
                                      :fire-flow-run! fire-flow-run!}))))]

    {:auto-scroll-agent! auto-scroll-agent!
     :make-event-handler make-event-handler
     :fire-flow-run! fire-flow-run!
     :show-flow-info! show-flow-info!
     :submit-agent-run! submit-agent-run!}))
