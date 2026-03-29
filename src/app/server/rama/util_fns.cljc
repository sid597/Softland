(ns app.server.rama.util-fns
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest :refer [create-ipc launch-module! gen-hashing-index-keys]]
            [app.server.file :refer [save-event deserialize-and-execute dg-edges-file-edn-edn dg-nodes-file-edn softland-edn dg-nodes-edn dg-edges-edn load-events dg-page-data-edn]]
            [missionary.core :as m]
            [app.server.rama.roam-ns :refer [roam-readers]]
            [app.server.rama.core :refer [node-events-module]])
  (:import (clojure.lang Keyword)
           (missionary Cancelled)
           [com.rpl.rama.integration TaskGlobalObject]
           [java.util.concurrent CompletableFuture]
           [java.util.function Supplier]
           [com.rpl.rama.helpers ModuleUniqueIdPState]))



(defrecord node-events [action-type node-data event-data])
;; uuid is unique generate using (java.util.UUID/randomUUID)
(defrecord registration [uuid username])
(defrecord update-user-graph-settings [user-id graph-name settings-data event-data])



(defonce !rama-ipc (atom nil))


(def ipc
  (let [c (create-ipc)]
    (println "--R--: Start ipc, launch module")
    (reset! !rama-ipc c)
    (launch-module! c node-events-module {:tasks 4 :threads 2})))


;; Define clj defs

(def event-depot                  (foreign-depot  @!rama-ipc (get-module-name node-events-module) "*node-events-depot"))
(def nodes-pstate                 (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$nodes-pstate"))
(def node-ids-pstate              (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$node-ids-pstate"))
(def dg-node-ids-pstate           (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$dg-node-ids-pstate"))
(def dg-pages-pstate              (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$dg-pages-pstate"))
(def dg-nodes-pstate              (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$dg-nodes-pstate"))
(def dg-edges-pstate              (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$dg-edges-pstate"))
(def event-id-pstate              (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$event-id-pstate"))
(def agent-runs-pstate            (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$agent-runs-pstate"))
(def cli-sessions-pstate          (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$cli-sessions-pstate"))
(def user-registration-pstate     (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$user-registration-pstate"))
(def user-registration-depot      (foreign-depot @!rama-ipc (get-module-name node-events-module) "*user-registration-depot"))
(def  user-graph-settings-pstate  (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$user-graph-settings-pstate"))
(def  user-graph-settings-depot   (foreign-depot @!rama-ipc (get-module-name node-events-module) "*user-graph-settings-depot"))
(def get-in-view-nodes-query  (foreign-query @!rama-ipc (get-module-name node-events-module) "get-in-view-nodeids"))
(def sidebar-pstate              (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$sidebar-pstate"))
(def settings-pstate             (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$settings-pstate"))
(def agent-trails-pstate         (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$agent-trails-pstate"))
(def flow-session-pstate         (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$flow-session-pstate"))
(def editor-state-pstate         (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$editor-state-pstate"))
(def workspace-truth-pstate      (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$workspace-truth-pstate"))


(defn update-event-id []
  (foreign-append! event-depot (->node-events
                                 :update-event-id
                                 {}
                                 {})
    :append-ack))

(defn get-event-id []
  (first (foreign-select [] event-id-pstate)))

(defn get-user-id [username]
  (first (foreign-select [username] user-registration-pstate)))


(defn get-user-graph-settings [user-id graph-name]
  (foreign-select [(keypath user-id) graph-name :ui-mode] user-graph-settings-pstate))


(defn update-user-setting [settings-data event-data save? update?]
  (let [user-id (get-user-id (:username event-data))
        graph-name (:graph-name event-data)]
    (do (foreign-append! user-graph-settings-depot (->update-user-graph-settings
                                                     user-id
                                                     graph-name
                                                     settings-data
                                                     event-data))
        (when save? (save-event "update-user-setting" [settings-data event-data] softland-edn))
        (when (or update?
                (some? (:event-id event-data)))
          (update-event-id)))))

(defn register-user
  ([username event-data]
   (register-user username event-data false false))
  ([username event-data save? update?]
   (let [uuid (str (java.util.UUID/randomUUID))]
     (do
       (foreign-append! user-registration-depot (->registration
                                                   uuid
                                                   username)
         :append-ack)
       (when save? (save-event "register-user" [username] softland-edn))
       (when (or update?
                (some? (:event-id event-data)))
          (update-event-id))))))


(defn proxy-callback [emit]
  (fn [new-val _diff _old-val]
    (emit new-val)
    nil))


(defn !subscribe [path pstate]
  (->> (m/observe
         (fn [!]
           ;; emit current value immediately, then stream updates
           (! (first (foreign-select path pstate)))
           ;; using subselect because foreign-procxy takes exactly one path
           (let [proxy (foreign-proxy-async path pstate
                         {:callback-fn (proxy-callback !)})]
             #(.close @proxy))))
    ; discard stale values, DOM doesn't support backpressure
    (m/relieve {})))


(defn add-new-node
  ([node-map event-data]
   (add-new-node node-map event-data false false softland-edn))
  ([node-map event-data save?]
   (add-new-node node-map event-data save? false softland-edn))
  ([node-map event-data save? update?]
   (add-new-node node-map event-data save? update? softland-edn))
  ([node-map event-data save? update? file-name]
   (do
     (foreign-append! event-depot (->node-events
                                    :new-node
                                    node-map
                                    event-data)
       :append-ack)
     (when save?
       (save-event
         "add-new-node"
         [node-map event-data]
         file-name))
     (when (or update?
             (some? (:event-id event-data)))
       (update-event-id)))))

(defn add-dg-page-data [data]
  (foreign-append! event-depot (->node-events
                                 :add-dg-page-data
                                 data
                                 {:graph-name :main})
    :append-ack))

(defn add-dg-nodes [data]
  (foreign-append!
    event-depot
    (->node-events
      :add-dg-nodes
      data
      {:graph-name :main})
    :append-ack))



(defn add-dg-edges [data]
  (println "DATA ** " data)

  (foreign-append!
    event-depot
    (->node-events
      :add-dg-edges
      data
      {:graph-name :main})
    :append-ack))

(defn update-node
  ([node-map event-data]
   (update-node node-map event-data false false))
  ([node-map event-data save? update?]
   (do
     (foreign-append! event-depot (->node-events
                                    :update-node
                                    node-map
                                    event-data)
       :append-ack)
     (when save?
       (save-event "update-node" [node-map event-data] softland-edn))
     (when (or update?
             (some? (:event-id event-data)))
       (update-event-id)))))

(defn send-llm-request
  [node-map event-data]
  (println "SEND LLM REQUEST: " event-data)
  (foreign-append! event-depot (->node-events
                                 :llm-request
                                 node-map
                                 event-data)
    :append-ack))


(defn get-cli-session
  "Look up the saved CLI session for a file+provider pair (for --resume)."
  [file-path provider]
  (first (foreign-select [(keypath file-path) (keypath provider)] cli-sessions-pstate)))

(defn update-cli-session
  "Store/update the CLI session-id for a file+provider pair in Rama.
   Called after a successful agent run to persist the session-id for --resume."
  [file-path provider session-id]
  (when (and (seq file-path) provider (seq session-id))
    (foreign-append! event-depot
      (->node-events :update-cli-session
                     {}
                     {:graph-name :main
                      :file-path file-path
                      :provider provider
                      :session-id session-id})
      :append-ack)))

(defn submit-agent-run
  "Append an :agent-run event and return the run-id.
   Auto-injects session-id from previous sessions for --resume support."
  [request-data]
  (let [run-id (or (:run-id request-data)
                   (str (java.util.UUID/randomUUID)))
        ;; Auto-inject session-id for --resume if not provided
        file-path (:file request-data)
        provider (:provider request-data)
        session (when (and file-path provider)
                  (get-cli-session file-path provider))
        request-data (cond-> (assoc request-data :run-id run-id)
                       (and session (not (:session-id request-data)))
                       (assoc :session-id (:session-id session)))
        now-ms (System/currentTimeMillis)]
    (foreign-append! event-depot
      (->node-events :agent-run
                     {}
                     {:graph-name :main
                      :run-id run-id
                      :request-data request-data
                      :create-time now-ms})
      :append-ack)
    run-id))


(defn get-agent-run
  [run-id]
  (first (foreign-select [(keypath run-id)] agent-runs-pstate)))


;; ── Sidebar Rama helpers ──────────────────────────────────────────

(defn get-sidebar-state
  "Read the current sidebar committed truth from Rama."
  []
  {:project       (first (foreign-select [:sidebar (keypath :project)] sidebar-pstate))
   :expanded-dirs (or (first (foreign-select [:sidebar (keypath :expanded-dirs)] sidebar-pstate)) #{})
   :selected-file (first (foreign-select [:sidebar (keypath :selected-file)] sidebar-pstate))})

;; Server-side atom — the reactive source for Electric e/watch.
;; Updated after each Rama write by emit-sidebar-event!.
;; Initialized from Rama PState at boot (picks up persisted state).
;;
;; foreign-proxy-async is broken in Rama 1.6.0 test IPC:
;;   - Root path [] → RocksDBWrapper serialization failure
;;   - Per-key paths → WorpResolveTimeout / connection manager collapse
;; The atom mirror approach bypasses proxy entirely and is proven stable.
(defonce !sidebar-truth-atom
  (atom (try (get-sidebar-state)
             (catch Exception _ {:project nil :expanded-dirs #{} :selected-file nil}))))

(defn emit-sidebar-event!
  "Submit a sidebar action to Rama. Updates the server-side truth atom
   (which Electric watches via e/watch) and returns the new state."
  [action-type data]
  (foreign-append! event-depot
    (->node-events action-type
                   data
                   {:graph-name :sidebar})
    :append-ack)
  (let [state (get-sidebar-state)]
    (reset! !sidebar-truth-atom state)
    state))

;; ── Settings Rama helpers ────────────────────────────────────────

(defn get-settings-state
  "Read the current user settings from Rama."
  []
  (or (first (foreign-select [:settings] settings-pstate)) {}))

;; Server-side atom — reactive source for Electric e/watch.
;; Same pattern as sidebar: atom mirror bypasses broken foreign-proxy-async.
(defonce !settings-truth-atom
  (atom (try (get-settings-state)
             (catch Exception _ {}))))

(defn emit-settings-event!
  "Submit a settings update to Rama. Merges partial settings map.
   Updates the server-side truth atom and returns the new state."
  [settings-data]
  (foreign-append! event-depot
    (->node-events :settings/update
                   settings-data
                   {:graph-name :settings})
    :append-ack)
  (let [state (get-settings-state)]
    (reset! !settings-truth-atom state)
    state))

;; ── Agent Trail Rama helpers ─────────────────────────────────────

(defn get-agent-trail
  "Read a single completed trail from Rama by run-id."
  [run-id]
  (first (foreign-select [(keypath run-id)] agent-trails-pstate)))

(defn get-latest-trail-run-id
  "Read the :latest-run-id marker from agent trails.
   Stored as a special key so we know which run to restore on reload."
  []
  (first (foreign-select [(keypath "__latest") (keypath :run-id)] agent-trails-pstate)))

;; Server-side atom — holds the most recently completed trail for Electric.
;; On boot, tries to restore the latest trail from Rama.
(defonce !agent-trail-atom
  (atom (try
          (when-let [run-id (get-latest-trail-run-id)]
            (when-let [trail-data (get-agent-trail run-id)]
              {:run-id run-id :trail-data trail-data}))
          (catch Exception _ nil))))

(defn save-agent-trail!
  "Persist a completed agent trail to Rama. Updates the latest-run marker
   and the server-side atom for Electric."
  [run-id trail-data]
  ;; Save the trail data
  (foreign-append! event-depot
    (->node-events :agent-trail/save-run
                   {:run-id run-id :trail-data trail-data}
                   {:graph-name :trails})
    :append-ack)
  ;; Update the latest-run marker
  (foreign-append! event-depot
    (->node-events :agent-trail/save-run
                   {:run-id "__latest"
                    :trail-data {:run-id run-id}}
                   {:graph-name :trails})
    :append-ack)
  (let [result {:run-id run-id :trail-data trail-data}]
    (reset! !agent-trail-atom result)
    result))

;; ── Workspace Truth Rama helpers ─────────────────────────────────

(defn get-workspace-truth
  "Read workspace truth from Rama."
  []
  (or (first (foreign-select [:workspace] workspace-truth-pstate)) {}))

(defonce !workspace-truth-atom
  (atom (try (get-workspace-truth)
             (catch Exception _ {}))))

(defn emit-workspace-truth-event!
  "Persist workspace truth fields to Rama."
  [truth-data]
  (foreign-append! event-depot
    (->node-events :workspace/save-truth
                   truth-data
                   {:graph-name :workspace})
    :append-ack)
  (let [state (get-workspace-truth)]
    (reset! !workspace-truth-atom state)
    state))

;; ── Editor State Rama helpers ────────────────────────────────────

(defn get-editor-doc
  "Read editor document state for a file path from Rama."
  [file-path]
  (first (foreign-select [(keypath file-path)] editor-state-pstate)))

(defonce !editor-doc-atom (atom nil))

(defn save-editor-doc!
  "Persist editor document state to Rama. Returns the round-trip time in ms."
  [file-path doc-state]
  (let [t0 (System/currentTimeMillis)]
    (foreign-append! event-depot
      (->node-events :editor/save-doc
                     {:file-path file-path :doc-state doc-state}
                     {:graph-name :editor})
      :append-ack)
    (let [t1 (System/currentTimeMillis)
          latency (- t1 t0)]
      (reset! !editor-doc-atom {:file-path file-path :doc-state doc-state :latency-ms latency})
      {:ok true :latency-ms latency})))

;; ── Flow Session Rama helpers ────────────────────────────────────

(defn get-flow-session-state
  "Read the current flow session from Rama."
  []
  (or (first (foreign-select [:flow] flow-session-pstate)) {}))

(defonce !flow-session-atom
  (atom (try (get-flow-session-state)
             (catch Exception _ {}))))

(defn emit-flow-session-event!
  "Persist flow session FSM state to Rama. Merges provided fields."
  [flow-data]
  (foreign-append! event-depot
    (->node-events :flow/save-state
                   flow-data
                   {:graph-name :flow})
    :append-ack)
  (let [state (get-flow-session-state)]
    (reset! !flow-session-atom state)
    state))

;; ─────────────────────────────────────────────────────────────────

(defn roam-query-request
  [node-map event-data]
  (println "SEND roam query REQUEST: " event-data)
  (foreign-append! event-depot (->node-events
                                 :roam-query
                                 node-map
                                 event-data)
    :append-ack))

(defn get-path-data [path pstate]
  (println "FOREIGN SELECT")
  (foreign-select path pstate))

(defn get-query-top [x y h w gn path]
  (foreign-invoke-query get-in-view-nodes-query x y h w gn path))


(defn save-dg [node]
  (let [{:keys [uid title id]} node
        node-map {(keyword uid) {:y {:pos (+ 0.00000201 (rand-int 400))
                                     :time 0}
                                 :fill "lightblue",
                                 :type "dg-node-rect",
                                 :id (keyword uid)
                                 :x {:pos (+ 0.00000201 (rand-int 400))
                                     :time 0}
                                 :type-specific-data {:db/id id
                                                      :width 20
                                                      :height 20
                                                      :text title}}}
        event-data {:graph-name :main
                    :event-id (get-event-id)
                    :create-time 0}]
    (save-event
      "add-new-node"
      [node-map event-data]
      dg-nodes-file-edn)))



(clojure.pprint/pprint roam-readers)
;(load-events softland-edn deserialize-and-execute false) ;; THIS IS A HACK: Will not work when we move away from ipc.
(println "------ ADDING DG PAGES------")
#_(load-events dg-page-data-edn add-dg-page-data) ;; THIS IS A HACK: Will not work when we move away from ipc.
(println "------ ADDING DG NODES------")
#_(load-events dg-nodes-edn add-dg-nodes) ;; THIS IS A HACK: Will not work when we move away from ipc.
(println "------ ADDING DG EDGES------")
#_(load-events dg-edges-edn add-dg-edges) ;; THIS IS A HACK: Will not work when we move away from ipc.


#_(load-events dg-nodes-file-edn deserialize-and-execute)

#_{:nodes {"[[ISS]] - According to literature, look into how much integrin expression do WTC-11 cells have."
           {:uid "kPsqOOAaS"
            :title "[[ISS]] - According to literature, look into how much integrin expression do WTC-11 cells have."
            :type :ISS},
           "[[HYP]] - Depending on the ECM substrate, we can vary the dynamics and relative abundance of the type of endocytosis in cells."
           {:uid "y384RFx3P"
            :title "[[HYP]] - Depending on the ECM substrate, we can vary the dynamics and relative abundance of the type of endocytosis in cells."
            :type :HYP}}
   :edges ([{:uid "kPsqOOAaS"
             :title "[[ISS]] - According to literature, look into how much integrin expression do WTC-11 cells have."}
            "InformedBy"
            {:uid "y384RFx3P"
             :title "[[HYP]] - Depending on the ECM substrate, we can vary the dynamics and relative abundance of the type of endocytosis in cells."}])}
