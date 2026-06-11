(ns app.server.rama.util-fns
  (:require [clojure.string :as string]
            [app.server.rama.core :as core]
            [app.server.rama.text-kernel :as text-kernel]
            [missionary.core :as m]))

;; This namespace is now a thin adapter over the canonical text kernel.
;; The old node-events module and its app-state PStates have been removed from
;; Rama. Remaining legacy fn names are compatibility shims for server routes
;; that have not yet been re-expressed as projection/action/kernel flows.

(defonce !kernel-runtime
  (delay
    (println "--R--: Start canonical text kernel")
    (let [runtime (text-kernel/start-text-runtime!)]
      (println "--R--: Text kernel ready" {:module (:module-name runtime)})
      runtime)))

(defn runtime
  []
  @!kernel-runtime)

(defn append-action-request!
  [request]
  (text-kernel/append-action-request! (runtime) request))

(defn ingest-text!
  ([content] (text-kernel/ingest-text! (runtime) content))
  ([content opts] (text-kernel/ingest-text! (runtime) content opts)))

(defn unitize-lines!
  ([artifact-event] (text-kernel/unitize-lines! (runtime) artifact-event))
  ([artifact-event opts] (text-kernel/unitize-lines! (runtime) artifact-event opts)))

(defn set-unit-status!
  ([unit-id status] (text-kernel/set-unit-status! (runtime) unit-id status))
  ([unit-id status opts] (text-kernel/set-unit-status! (runtime) unit-id status opts)))

(defn get-artifact
  [artifact-id]
  (text-kernel/read-artifact (runtime) artifact-id))

(defn get-text-head
  [artifact-id]
  (text-kernel/read-text-head (runtime) artifact-id))

(defn get-canonical-view
  ([artifact-id] (get-canonical-view core/default-branch-id artifact-id))
  ([branch-id artifact-id]
   (text-kernel/read-canonical-view (runtime) branch-id artifact-id)))

(defn get-discarded-view
  ([artifact-id] (get-discarded-view core/default-branch-id artifact-id))
  ([branch-id artifact-id]
   (text-kernel/read-discarded-view (runtime) branch-id artifact-id)))

(defn run-v0-text-proof!
  [content]
  (text-kernel/run-v0-text-proof! (runtime) content))

(defn proxy-callback
  [emit]
  (fn [new-val _diff _old-val]
    (emit new-val)
    nil))

(defn !subscribe
  "Compatibility helper for older Electric call sites. New code should read
   through kernel projections instead of subscribing to ad hoc PStates."
  [path pstate]
  (->> (m/observe
         (fn [!]
           (! {:deprecated true
               :path path
               :pstate (some-> pstate str)})
           #()))
       (m/relieve {})))

;; ── Transitional local mirrors for routes not yet moved to kernel projections
;;
;; QUARANTINED OUT OF THE KERNEL CONTRACT (retro K-07 / prior 01-F2).
;;
;; These atoms are session-local UI mirrors, NOT kernel truth. The kernel
;; contract's back-arrow rule (C12: Rama is truth, projections read PStates)
;; does not apply to them because they are declared outside the contract:
;;
;;   - They reset to their literal initial values on every JVM restart; no
;;     rebuild path from PStates exists or is promised.
;;   - Their writes also append :compat/record requests, so a durable,
;;     decided, auditable trail of every mutation lives in the kernel — but
;;     nothing folds those events back into the atoms.
;;   - The atom updates after each append are fire-and-forget: if the kernel
;;     rejects the compat request (e.g. an event type that fell off the
;;     allow-list in core/compat-allowed-event-types), the atom still updates.
;;
;; Why they survive this fix session instead of becoming PState reads: the
;; Electric UI subscribes via (e/watch !atom), and per-key Rama subscription
;; (foreign-proxy-async on global PStates) crashes Rama 1.6.0 (S40 quirk in
;; memory/implementation-quirks.md). Replacing the atoms with foreign selects
;; would silently break UI reactivity. When a subscription path exists, each
;; mirror should become a kernel projection and leave this list.

(def transitional-mirror-quarantine
  "Machine-readable quarantine declaration. Tests assert this list covers
   every mirror atom in this namespace, so adding a mirror without declaring
   it (or declaring it kernel-backed without a rebuild path) fails loudly."
  {:kernel-contract? false
   :durable? false
   :reset-on-restart? true
   :rebuild-path :none
   :durable-audit :compat-record-events
   :mirrors '[!cli-sessions
              !agent-runs
              !sidebar-truth-atom
              !settings-truth-atom
              !agent-trail-atom
              !workspace-truth-atom
              !editor-doc-atom
              !flow-session-atom]})

(defonce !cli-sessions (atom {}))
(defonce !agent-runs (atom {}))
(defonce !sidebar-truth-atom (atom {:project nil :expanded-dirs #{} :selected-file nil}))
(defonce !settings-truth-atom (atom {}))
(defonce !agent-trail-atom (atom nil))
(defonce !workspace-truth-atom (atom {}))
(defonce !editor-doc-atom (atom nil))
(defonce !flow-session-atom (atom {}))

(defn- relation-id
  [& parts]
  (string/join "::" (map #(or % "") parts)))

(defn- append-compat-event!
  [{:keys [event-type target-kind target-id action-type capability payload]}]
  (append-action-request!
    (core/compat-record-request
      {:event-type event-type
       :target-kind target-kind
       :target-id target-id
       :action-type action-type
       :capability capability
       :payload payload})))

(defn update-event-id
  []
  (append-compat-event!
    {:event-type :compat/event-id-tick
     :target-kind :relation
     :target-id "compat/event-id"
     :action-type :compat/event-id-tick
     :capability :action/append
     :payload {}}))

(defn register-user
  ([username event-data]
   (register-user username event-data false false))
  ([username event-data _save? _update?]
   (let [user-id (str "user/" username)]
     (append-compat-event!
       {:event-type :identity/user-registered
        :target-kind :relation
        :target-id user-id
        :action-type :identity/register-user
        :capability :action/append
        :payload {:username username
                  :event-data event-data}})
     {:user-id user-id
      :username username})))

(defn get-user-id
  [username]
  (str "user/" username))

(defn get-user-graph-settings
  [_user-id _graph-name]
  nil)

(defn update-user-setting
  [settings-data event-data _save? _update?]
  (append-compat-event!
    {:event-type :settings/user-setting-updated
     :target-kind :relation
     :target-id (relation-id "user-setting" (:username event-data) (:graph-name event-data))
     :action-type :settings/update-user-setting
     :capability :action/append
     :payload {:settings-data settings-data
               :event-data event-data}})
  true)

(defn get-cli-session
  [file-path provider]
  (get @!cli-sessions [file-path provider]))

(defn update-cli-session
  [file-path provider session-id]
  (when (and (seq file-path) provider (seq session-id))
    (swap! !cli-sessions assoc [file-path provider]
           {:session-id session-id
            :last-active (System/currentTimeMillis)})
    (append-compat-event!
      {:event-type :cli/session-updated
       :target-kind :relation
       :target-id (relation-id "cli-session" file-path (name provider))
       :action-type :cli/update-session
       :capability :action/append
       :payload {:file-path file-path
                 :provider provider
                 :session-id session-id}})
    true))

(defn submit-agent-run
  [request-data]
  (let [run-id (or (:run-id request-data) (str (java.util.UUID/randomUUID)))
        file-path (:file request-data)
        provider (:provider request-data)
        session (when (and file-path provider)
                  (get-cli-session file-path provider))
        request-data (cond-> (assoc request-data :run-id run-id)
                       (and session (not (:session-id request-data)))
                       (assoc :session-id (:session-id session)))
        run {:run-id run-id
             :status :running
             :provider provider
             :prompt (:prompt request-data)
             :request-data request-data
             :started-at (System/currentTimeMillis)}]
    (swap! !agent-runs assoc run-id run)
    (append-compat-event!
      {:event-type :agent/run-submitted
       :target-kind :relation
       :target-id run-id
       :action-type :agent/submit-run
       :capability :action/append
       :payload run})
    run-id))

(defn get-agent-run
  [run-id]
  (get @!agent-runs run-id))

(defn get-sidebar-state
  []
  @!sidebar-truth-atom)

(defn- apply-sidebar-action
  [state action-type data]
  (case action-type
    :sidebar/dir-toggle
    (update state :expanded-dirs
            (fn [dirs]
              (let [dirs (or dirs #{})
                    path (:path data)]
                (if (contains? dirs path)
                  (disj dirs path)
                  (conj dirs path)))))

    :sidebar/file-select
    (assoc state :selected-file {:path (:path data)
                                 :name (:name data)})

    :sidebar/project-select
    (assoc state
           :project {:path (:path data)
                     :name (:name data)}
           :expanded-dirs #{}
           :selected-file nil)

    :sidebar/project-back
    (assoc state
           :project nil
           :expanded-dirs #{}
           :selected-file nil)

    state))

(defn emit-sidebar-event!
  [action-type data]
  (append-compat-event!
    {:event-type action-type
     :target-kind :projection
     :target-id "sidebar"
     :action-type action-type
     :capability :action/append
     :payload data})
  (swap! !sidebar-truth-atom apply-sidebar-action action-type data))

(defn get-settings-state
  []
  @!settings-truth-atom)

(defn emit-settings-event!
  [settings-data]
  (append-compat-event!
    {:event-type :settings/update
     :target-kind :projection
     :target-id "settings"
     :action-type :settings/update
     :capability :action/append
     :payload settings-data})
  (swap! !settings-truth-atom merge settings-data))

(defn get-agent-trail
  [run-id]
  (when (= run-id (:run-id @!agent-trail-atom))
    (:trail-data @!agent-trail-atom)))

(defn get-latest-trail-run-id
  []
  (:run-id @!agent-trail-atom))

(defn save-agent-trail!
  [run-id trail-data]
  (append-compat-event!
    {:event-type :agent-trail/saved
     :target-kind :relation
     :target-id run-id
     :action-type :agent-trail/save
     :capability :action/append
     :payload {:run-id run-id
               :trail-data trail-data}})
  (let [result {:run-id run-id :trail-data trail-data}]
    (reset! !agent-trail-atom result)
    result))

(defn get-workspace-truth
  []
  @!workspace-truth-atom)

(defn emit-workspace-truth-event!
  [truth-data]
  (append-compat-event!
    {:event-type :workspace/save-truth
     :target-kind :projection
     :target-id "workspace"
     :action-type :workspace/save-truth
     :capability :action/append
     :payload truth-data})
  (swap! !workspace-truth-atom merge truth-data))

(defn get-editor-doc
  [file-path]
  (get @!editor-doc-atom file-path))

(defn save-editor-doc!
  [file-path doc-state]
  (let [t0 (System/currentTimeMillis)]
    (append-compat-event!
      {:event-type :editor/save-doc
       :target-kind :artifact
       :target-id file-path
       :action-type :editor/save-doc
       :capability :action/append
       :payload {:file-path file-path
                 :doc-state doc-state}})
    (swap! !editor-doc-atom assoc file-path doc-state)
    {:ok true
     :latency-ms (- (System/currentTimeMillis) t0)}))

(defn get-flow-session-state
  []
  @!flow-session-atom)

(defn emit-flow-session-event!
  [flow-data]
  (append-compat-event!
    {:event-type :flow/save-state
     :target-kind :projection
     :target-id "flow"
     :action-type :flow/save-state
     :capability :action/append
     :payload flow-data})
  (swap! !flow-session-atom merge flow-data))
