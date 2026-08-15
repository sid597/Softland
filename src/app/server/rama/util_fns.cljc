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

;; ── Transitional local mirrors — QUARANTINED OUT OF THE KERNEL CONTRACT
;; (retro K-07 / prior 01-F2). A mirror atom here is session-local UI state,
;; NOT kernel truth: it resets to its literal initial value on JVM restart,
;; has no rebuild path from PStates, and the back-arrow rule (C12) does not
;; apply because it is declared outside the contract. !ingest-epoch-atom is
;; the one current mirror. When a Rama subscription path exists (foreign-proxy-async on
;; global PStates crashes Rama 1.6.0 — S40 quirk), a mirror should become a
;; kernel projection and leave this list.

(def transitional-mirror-quarantine
  "Machine-readable quarantine declaration. Tests assert this list covers
   every mirror atom in this namespace, so adding a mirror without declaring
   it (or declaring it kernel-backed without a rebuild path) fails loudly."
  {:kernel-contract? false
   :durable? false
   :reset-on-restart? true
   :rebuild-path :none
   :durable-audit :compat-record-events
   :mirrors '[!ingest-epoch-atom]})

;; view-mvp WP-B2 OP-35: monotonic ingest-epoch counter. A counter, not
;; truth - carries no data, orders nothing semantically, resets on restart
;; (INV-14); bumped by ingest-watchers on import completion, pushed to the
;; client by WatchIngestEpoch (the S1a carve-out).
(defonce !ingest-epoch-atom (atom 0))

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
