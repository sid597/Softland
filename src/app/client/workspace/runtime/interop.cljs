(ns app.client.workspace.runtime.interop
  "Dev interop: replay fixture, window globals for Claude-in-Chrome, extract preview overlay."
  (:require [cljs.reader :as reader]
            [app.client.workspace.trail :refer [compute-agent-panel-h agent-wrapped-line-count]]))

(defn install-extract-preview-watch!
  "Watch !extract-preview; show/hide DOM overlay + populate HTML."
  [{:keys [!extract-preview !preview-el]}]
  (add-watch !extract-preview :overlay
    (fn [_ _ _ new-val]
      (when-let [el (and !preview-el @!preview-el)]
        (if new-val
          (do (set! (.-display (.-style el)) "block")
              (if-let [html (:html new-val)]
                (set! (.-innerHTML el)
                      (str "<div style='color:#888;font-size:12px;margin-bottom:8px;'>Source: "
                           (or (:source-url new-val) "extracted") "</div>"
                           "<div style='padding:16px;border:1px solid #333;border-radius:8px;background:#0d0d0f;'>"
                           html "</div>"))
                (set! (.-innerHTML el)
                      "<div style='color:#666;padding:40px;text-align:center;'>No HTML preview available.<br>Run extractor to capture source HTML.</div>")))
          (do (set! (.-display (.-style el)) "none")
              (set! (.-innerHTML el) "")))))))

(defn- auto-scroll-agent-impl!
  "Scroll agent output to bottom. Used by replay."
  [{:keys [!agent-output !viewport !settings !active-font !agent-scroll-y]}]
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

(defn trigger-dev-replay!
  "Fire the dev replay fixture — replays SSE events with staggered timeouts."
  [atoms]
  (let [{:keys [!agent-output !agent-scroll-y]} atoms]
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
                                 (do (swap! !agent-output
                                       (fn [ao]
                                         (-> ao
                                           (update :output str (:text evt))
                                           (update :trail conj {:kind :reasoning :text (:text evt)}))))
                                     (auto-scroll-agent-impl! atoms))

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

                                 (js/console.warn "[DEV][UNKNOWN-EVENT]" (clj->js evt)))))
                           (* i 50))))
                     (js/console.error "[DEV] Replay failed:" (:error data))))))
        (.catch (fn [err] (js/console.error "[DEV] Replay fetch error:" err))))))

(defn install-window-globals!
  "Register __softland_inject_preview and __softland_inject_rt_node on window."
  [{:keys [!extract-preview]} show-flow-info!]
  (set! (.-__softland_inject_preview js/window)
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
  (set! (.-__softland_inject_rt_node js/window)
        (fn [rt-node-json]
          (let [rt-node (js->clj (.parse js/JSON rt-node-json) :keywordize-keys true)]
            (reset! !extract-preview {:rt-node rt-node})
            (show-flow-info! "rt-node injected directly.")))))
