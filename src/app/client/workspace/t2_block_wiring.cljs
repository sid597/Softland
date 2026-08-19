(ns app.client.workspace.t2-block-wiring
  "Effectful owner of the one real served-block T2 occurrence.

   Owns exactly two watches and the editing runtime's one semantic commit sink.
   Durable writes and narrow reads still travel only through block-edit-wiring."
  (:require [app.client.workspace.block-edit-wiring :as block-edit-wiring]
            [app.client.workspace.editing-runtime :as editing-runtime]
            [app.client.workspace.t2-block-join :as join]))

(defonce ^:private !join-state (atom (join/init)))
(defonce ^:private !installation (atom nil))
(defonce ^:private browser-session-id (str (random-uuid)))
(defonce ^:private !client-generation (atom 0))
(defonce ^:private !pending-requests (atom {}))
(defonce ^:private !receipt (atom {:installed? false :events []}))

(defn- bounded-conj [entries entry]
  (let [entries (vec (or entries []))]
    (conj (if (>= (count entries) 32) (subvec entries 1) entries) entry)))

(defn- publish! []
  (aset js/globalThis "__softlandT2BlockJoinReceipt" (clj->js @!receipt))
  @!receipt)

(defn- record! [event]
  (swap! !receipt update :events bounded-conj
         (assoc event :at-ms (js/Date.now)))
  (publish!))

(defn- fresh-client-id [unit-id]
  (str "t2-real:" browser-session-id ":"
       (swap! !client-generation inc) ":" unit-id))

(defn- cancel-target-requests! [unit-id]
  (doseq [[request-id target-id] @!pending-requests
          :when (= unit-id target-id)]
    (block-edit-wiring/cancel-continuation! request-id)
    (swap! !pending-requests dissoc request-id)))

(defn- view-signature [view]
  (select-keys view [:unit-id :vi :text :caret :refusal]))

(defn- sync-view! [before after]
  (let [before-view (join/view before)
        after-view (join/view after)]
    (when (and before-view
               (not= (:vi before-view) (:vi after-view)))
      (editing-runtime/remove-document! (:vi before-view)))
    (when (and after-view
               (not= (view-signature before-view)
                     (view-signature after-view)))
      (editing-runtime/sync-document!
       (:spec after-view)
       {:text (:text after-view)
        :caret (:caret after-view)
        :status (:refusal after-view)}))
    (swap! !receipt assoc
           :target (some-> after-view
                           (select-keys [:unit-id :vi :text :caret
                                         :awaiting-keyed-truth :refusal])))
    (publish!)))

(defn- install-state! [before after]
  (reset! !join-state after)
  (sync-view! before after)
  after)

(defn- apply-full! [context]
  (let [before @!join-state
        current-id (get-in before [:target :block :id])
        selected (join/select-block context current-id)
        fresh-id (when (and selected (not= current-id (:id selected)))
                   (fresh-client-id (:id selected)))
        before-view (join/view before)
        after (join/reconcile-full before context fresh-id)
        after-view (join/view after)
        blocked? (and (:awaiting-keyed-truth before)
                      selected
                      (= current-id (:id selected))
                      (not= (:text selected) (:text before-view))
                      (= (:text before-view) (:text after-view)))]
    (when (and current-id (not= current-id (:id selected)))
      (cancel-target-requests! current-id))
    (install-state! before after)
    (record! {:kind :full-context
              :unit-id (:id selected)
              :blocked-stale-text? (boolean blocked?)
              :same-occurrence? (= (:vi before-view) (:vi after-view))})
    after))

(defn- apply-keyed! [data]
  (let [before @!join-state
        after (reduce (fn [state [unit-id truth]]
                        (join/on-keyed-truth state unit-id truth))
                      before
                      (:block-truth/units data))]
    (install-state! before after)
    (doseq [[unit-id truth] (:block-truth/units data)]
      (record! {:kind :keyed-truth
                :unit-id unit-id
                :request-nonce (:request-nonce truth)
                :admitted? (and (not= before after)
                                (= unit-id (get-in after [:target :block :id])))}))
    after))

(defn- apply-decision! [request-id decision]
  (let [before @!join-state
        after (join/on-decision before request-id decision)]
    (install-state! before after)
    (record! {:kind :decision
              :request-id request-id
              :status (:status decision)
              :reason (:reason decision)
              :request-nonce (:request-nonce decision)
              :correlated? (not= before after)})
    after))

(defn- handle-commit! [fact]
  (if-let [submit! (block-edit-wiring/edit-submit!)]
    (let [before @!join-state
          {:keys [state envelope]} (join/commit before fact)]
      (install-state! before state)
      (when envelope
        (swap! !pending-requests assoc
               (:request-id envelope)
               (get-in envelope [:target :target/id]))
        (record! {:kind :proposal
                  :request-id (:request-id envelope)
                  :unit-id (get-in envelope [:target :target/id])
                  :object-key (get-in envelope [:payload :object-key])
                  :document-container-id
                  (get-in envelope [:payload :document-container-id])})
        (submit! envelope
                 (fn [decision]
                   (swap! !pending-requests dissoc (:request-id envelope))
                   (apply-decision! (:request-id envelope) decision))))
      envelope)
    (let [before @!join-state
          after (assoc before :refusal
                       {:unit-id (get-in before [:target :block :id])
                        :reason :edit-transport-unavailable})]
      (install-state! before after)
      (record! {:kind :proposal-refused :reason :edit-transport-unavailable})
      nil)))

(defn state-snapshot [] @!join-state)
(defn receipt [] (publish!))

(defn uninstall! []
  (when-let [{:keys [!face-context !block-truth-data]} @!installation]
    (when !face-context (remove-watch !face-context ::face-context))
    (when !block-truth-data (remove-watch !block-truth-data ::block-truth)))
  (editing-runtime/install-commit-sink! nil)
  (doseq [request-id (keys @!pending-requests)]
    (block-edit-wiring/cancel-continuation! request-id))
  (reset! !pending-requests {})
  (when-let [vi (:vi (join/view @!join-state))]
    (editing-runtime/remove-document! vi))
  (reset! !join-state (join/init))
  (reset! !installation nil)
  (reset! !receipt {:installed? false :events []})
  (js-delete js/globalThis "__softlandT2BlockJoinReceipt")
  (js-delete js/globalThis "__softlandT2BlockJoinControl")
  true)

(defn install!
  "Install idempotently. Replacing the runtime atoms deliberately disposes
   the old target/session and its callbacks as one ownership unit."
  [atoms {:keys [!block-truth-data]}]
  (let [refs {:!face-context (:!face-context atoms)
              :!block-truth-data !block-truth-data}]
    (when (and @!installation (not= refs @!installation))
      (uninstall!))
    (when-not @!installation
      (reset! !installation refs)
      (reset! !receipt {:installed? true :events []}))
    ;; Re-adding the fixed keys replaces stale hot-reload closures while the
    ;; target identity, queue, and monotonic sequence remain untouched.
    (when-let [!face-context (:!face-context refs)]
      (add-watch !face-context ::face-context
                 (fn [_ _ _ context] (apply-full! context))))
    (when !block-truth-data
      (add-watch !block-truth-data ::block-truth
                 (fn [_ _ _ data] (apply-keyed! data))))
    (editing-runtime/install-commit-sink! handle-commit!)
    (when-let [!face-context (:!face-context refs)]
      (apply-full! @!face-context))
    ;; Dev receipt controls call the same pure transitions and retained sync;
    ;; they do not bypass the product submit path for an actual edit.
    (aset js/globalThis "__softlandT2BlockJoinControl"
          #js {:snapshot (fn [] (clj->js @!join-state))
               :injectFull (fn [value]
                             (clj->js (apply-full!
                                       (js->clj value :keywordize-keys true))))
               :injectDecision (fn [request-id value]
                                 (clj->js
                                  (apply-decision!
                                   request-id
                                   (js->clj value :keywordize-keys true))))
               :injectTruth (fn [unit-id value]
                              (clj->js
                               (apply-keyed!
                                {:block-truth/units
                                 {unit-id (js->clj value
                                                  :keywordize-keys true)}})))})
    (publish!)))
