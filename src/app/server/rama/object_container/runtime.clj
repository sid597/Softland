(ns app.server.rama.object-container.runtime
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [app.server.rama.object-container :as oc]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [com.rpl.rama.test :refer [create-ipc launch-module!]]))

(defn start-object-container-runtime!
  []
  (let [ipc (create-ipc)
        module-name (get-module-name oc/object-container-module)
        transcript-ops-module-name (get-module-name oc/object-container-transcript-ops-module)
        launch-opts {:tasks 4 :threads 2}]
    (launch-module! ipc oc/object-container-module launch-opts)
    (launch-module! ipc oc/object-container-transcript-ops-module launch-opts)
    {:ipc ipc
     :module-name module-name
     :transcript-ops-module-name transcript-ops-module-name
     :object-container-requests-depot
     (foreign-depot ipc module-name "*object-container-requests-depot")
     :transcript-control-depot
     (foreign-depot ipc transcript-ops-module-name "*transcript-control-depot")
     :transcript-file-state-depot
     (foreign-depot ipc transcript-ops-module-name "*transcript-file-state-depot")
     :requests-by-audit-id (foreign-pstate ipc module-name "$$requests-by-audit-id")
     :decisions-by-audit-id (foreign-pstate ipc module-name "$$decisions-by-audit-id")
     :decisions-by-idempotency (foreign-pstate ipc module-name "$$decisions-by-idempotency")
     :events-by-id (foreign-pstate ipc module-name "$$events-by-id")
     :import-completions-by-key (foreign-pstate ipc module-name "$$import-completions-by-key")
     :source-artifacts-by-id (foreign-pstate ipc module-name "$$source-artifacts-by-id")
     :source-versions-by-ref (foreign-pstate ipc module-name "$$source-versions-by-ref")
     :source-latest-by-ref (foreign-pstate ipc module-name "$$source-latest-by-ref")
     :source-ingest-completions-by-ref
     (foreign-pstate ipc module-name "$$source-ingest-completions-by-ref")
     :containers-by-id (foreign-pstate ipc module-name "$$containers-by-id")
     :revisions-by-id (foreign-pstate ipc module-name "$$revisions-by-id")
     :revision-history-by-container
     (foreign-pstate ipc module-name "$$revision-history-by-container")
     :derived-units-by-id (foreign-pstate ipc module-name "$$derived-units-by-id")
     :unit-graduations-by-id (foreign-pstate ipc module-name "$$unit-graduations-by-id")
     :source-anchors-by-target (foreign-pstate ipc module-name "$$source-anchors-by-target")
     :composition-children-by-parent
     (foreign-pstate ipc module-name "$$composition-children-by-parent")
     :composition-parent-by-child (foreign-pstate ipc module-name "$$composition-parent-by-child")
     :source-containers-by-source
     (foreign-pstate ipc module-name "$$source-containers-by-source")
     :source-derived-units-by-source
     (foreign-pstate ipc module-name "$$source-derived-units-by-source")
     :source-anchors-by-source
     (foreign-pstate ipc module-name "$$source-anchors-by-source")
     :source-edges-by-source
     (foreign-pstate ipc module-name "$$source-edges-by-source")
     :native-identity-claims-by-container
     (foreign-pstate ipc module-name "$$native-identity-claims-by-container")
     :outline-by-document (foreign-pstate ipc module-name "$$outline-by-document")
     :transcript-conversation-projection
     (foreign-pstate ipc module-name "$$transcript-conversation-projection")
     :transcript-tool-calls-by-name
     (foreign-pstate ipc module-name "$$transcript-tool-calls-by-name")
     :transcript-audit-by-request
     (foreign-pstate ipc module-name "$$transcript-audit-by-request")
     :transcript-last-message-by-conversation
     (foreign-pstate ipc module-name "$$transcript-last-message-by-conversation")
     :transcript-source-lines-by-file
     (foreign-pstate ipc module-name "$$transcript-source-lines-by-file")
     :transcript-runs
     (foreign-pstate ipc transcript-ops-module-name "$$transcript-runs")
     :transcript-file-offsets
     (foreign-pstate ipc transcript-ops-module-name "$$transcript-file-offsets")
     :transcript-file-source-lines-by-file
     (foreign-pstate ipc
                     transcript-ops-module-name
                     "$$transcript-file-source-lines-by-file")
     :edit-order-by-target (foreign-pstate ipc module-name "$$edit-order-by-target")
     :read-latest-source-by-ref-query
     (foreign-query ipc module-name "read-latest-source-by-ref")
     :read-source-by-ref-version-query
     (foreign-query ipc module-name "read-source-by-ref-version")
     :read-unit-query (foreign-query ipc module-name "read-unit")
     :read-current-revision-query (foreign-query ipc module-name "read-current-revision")
     :read-common-material-for-source-query
     (foreign-query ipc module-name "read-common-material-for-source")}))

(defn close-object-container-runtime!
  [runtime]
  (when-let [ipc (:ipc runtime)]
    (try
      (.close ipc)
      (catch Exception _ nil))))

(def block-edit-log-relative-path "data/block-edit-log.ednl")

(defn default-block-edit-log-path
  [{:keys [repo-root]}]
  (str (or repo-root (System/getProperty "user.dir")) "/"
       block-edit-log-relative-path))

(def ^:private block-edit-log-lock (Object.))

(defn- append-block-edit-log-line!
  [path request]
  (locking block-edit-log-lock
    (let [f (io/file path)]
      (io/make-parents f)
      (with-open [w (io/writer f :append true :encoding "UTF-8")]
        (.write w (pr-str (walk/postwalk #(if (record? %) (into {} %) %) request)))
        (.write w "\n")))))

(defn append-block-edit-request-durably!
  "WAL-first append for interactive :object/edit requests. A runtime without
   :block-edit-log-path (focused IPC tests) keeps the existing direct append."
  ([runtime request]
   (append-block-edit-request-durably! runtime request :ack))
  ([runtime request ack-level]
   (when-let [path (:block-edit-log-path runtime)]
     (append-block-edit-log-line! path request))
   (foreign-append! (:object-container-requests-depot runtime) request ack-level)))

(defn replay-block-edit-log!
  "Replay stored edit intents into a freshly rebuilt object-container runtime.
   Replay appends depot-only, never back to the WAL; request identity and the
   object-scoped journal make duplicate lines no-ops. Malformed lines are
   isolated so one torn tail cannot hide later edits."
  [runtime]
  (let [path (or (:block-edit-log-path runtime)
                 (default-block-edit-log-path {}))
        f (io/file path)]
    (if-not (.exists f)
      {:replayed 0 :failed 0}
      (with-open [rdr (io/reader f :encoding "UTF-8")]
        (reduce
         (fn [stats [line-idx line]]
           (if (str/blank? line)
             stats
             (try
               (let [request (edn/read-string line)]
                 (if (= :object/edit (oc/request-type request))
                   (do (foreign-append! (:object-container-requests-depot runtime)
                                        request :ack)
                       (update stats :replayed inc))
                   (do (binding [*out* *err*]
                         (println "[BLOCK-WRITE] replay: line" line-idx
                                  "is not :object/edit, skipping"))
                       (update stats :failed inc))))
               (catch Exception e
                 (binding [*out* *err*]
                   (println "[BLOCK-WRITE] replay: line" line-idx
                            "failed, skipping:" (.getMessage e)))
                 (update stats :failed inc)))))
         {:replayed 0 :failed 0}
         (map-indexed vector (line-seq rdr)))))))

(defn append-object-container-request!
  ([runtime request]
   (append-object-container-request! runtime request :append-ack))
  ([runtime request ack-level]
   (foreign-append! (:object-container-requests-depot runtime) request ack-level)))

(defn append-transcript-control!
  ([runtime request]
   (append-transcript-control! runtime request :ack))
  ([runtime request ack-level]
   (foreign-append! (:transcript-control-depot runtime) request ack-level)))

(defn append-transcript-file-state!
  ([runtime file-state]
   (append-transcript-file-state! runtime file-state :append-ack))
  ([runtime file-state ack-level]
   (foreign-append! (:transcript-file-state-depot runtime) file-state ack-level)))

(defn foreign-one
  [pstate path]
  (foreign-select-one path pstate))

(defn read-request
  ([runtime request]
   (read-request runtime (oc/request-partition-key request) (oc/request-id request)))
  ([runtime partition-key request-id]
   (foreign-one (:requests-by-audit-id runtime)
                [(keypath (oc/audit-id partition-key request-id))])))

(defn read-audit-request
  ([runtime request]
   (read-request runtime request))
  ([runtime partition-key request-id]
   (read-request runtime partition-key request-id)))

(defn read-decision
  ([runtime request]
   (read-decision runtime (oc/request-partition-key request) (oc/request-id request)))
  ([runtime partition-key request-id]
   (foreign-one (:decisions-by-audit-id runtime)
                [(keypath (oc/decision-id-for-audit-id
                           (oc/audit-id partition-key request-id)))])))

(defn read-source
  ([runtime source-id]
   (foreign-one (:source-artifacts-by-id runtime) [(keypath source-id)]))
  ([runtime source-ref source-hash]
   (read-source runtime (oc/source-id-for source-ref source-hash))))

(defn read-latest-source-by-ref
  [runtime source-ref]
  (foreign-invoke-query (:read-latest-source-by-ref-query runtime) source-ref))

(defn read-source-by-ref-version
  [runtime source-ref source-version-key]
  (foreign-invoke-query (:read-source-by-ref-version-query runtime)
                        source-ref
                        source-version-key))

(defn read-import-completion
  [runtime import-key]
  (foreign-one (:import-completions-by-key runtime) [(keypath import-key)]))

(defn read-source-material-refs
  [runtime pstate-key source-id]
  (foreign-select [(keypath source-id) MAP-VALS] (pstate-key runtime)))

(defn read-source-containers
  [runtime source-id]
  (read-source-material-refs runtime :source-containers-by-source source-id))

(defn read-source-derived-units
  [runtime source-id]
  (read-source-material-refs runtime :source-derived-units-by-source source-id))

(defn read-source-anchor-refs
  [runtime source-id]
  (read-source-material-refs runtime :source-anchors-by-source source-id))

(defn read-source-edge-refs
  [runtime source-id]
  (read-source-material-refs runtime :source-edges-by-source source-id))

(defn read-common-material-for-source
  ([runtime source-id]
   (read-common-material-for-source runtime
                                    source-id
                                    oc/common-material-categories
                                    {}
                                    oc/default-outline-page-size))
  ([runtime source-id categories]
   (read-common-material-for-source runtime
                                    source-id
                                    categories
                                    {}
                                    oc/default-outline-page-size))
  ([runtime source-id categories cursor-map limit]
   (foreign-invoke-query (:read-common-material-for-source-query runtime)
                         source-id
                         (vec categories)
                         (or cursor-map {})
                         limit)))

(defn read-source-anchors
  [runtime target-id]
  (foreign-select [(keypath target-id) MAP-VALS]
                  (:source-anchors-by-target runtime)))

(defn read-composition-children
  [runtime parent-slot-id]
  (foreign-select [(keypath parent-slot-id) MAP-VALS]
                  (:composition-children-by-parent runtime)))

(defn read-composition-parents
  [runtime child-slot-id]
  (foreign-select [(keypath child-slot-id) MAP-VALS]
                  (:composition-parent-by-child runtime)))

(defn read-native-identity-claim
  [runtime container-id]
  (foreign-one (:native-identity-claims-by-container runtime)
               [(keypath container-id)]))

(defn read-outline
  ([runtime document-id]
   (read-outline runtime document-id "" oc/default-outline-page-size))
  ([runtime document-id cursor limit]
   (foreign-select [(keypath document-id)
                    (sorted-map-range-from (or cursor "") (or limit oc/default-outline-page-size))
                    MAP-VALS]
                   (:outline-by-document runtime))))

(defn read-transcript-conversation-projection
  ([runtime conversation-container-id]
   (read-transcript-conversation-projection runtime
                                            conversation-container-id
                                            ""
                                            oc/default-outline-page-size))
  ([runtime conversation-container-id cursor limit]
   (foreign-select [(keypath conversation-container-id)
                    (sorted-map-range-from (or cursor "")
                                           (or limit oc/default-outline-page-size))
                    MAP-VALS]
                   (:transcript-conversation-projection runtime))))

(defn read-transcript-tool-calls-by-name
  ([runtime tool-name]
   (read-transcript-tool-calls-by-name runtime tool-name "" oc/default-outline-page-size))
  ([runtime tool-name cursor limit]
   (foreign-select [(keypath tool-name)
                    (sorted-map-range-from (or cursor "")
                                           (or limit oc/default-outline-page-size))
                    MAP-VALS]
                   (:transcript-tool-calls-by-name runtime))))

(defn read-transcript-audit-entries
  ([runtime request-id]
   (read-transcript-audit-entries runtime request-id "" oc/default-outline-page-size))
  ([runtime request-id cursor limit]
   (foreign-select [(keypath request-id)
                    (sorted-map-range-from (or cursor "")
                                           (or limit oc/default-outline-page-size))
                    MAP-VALS]
                   (:transcript-audit-by-request runtime))))

(defn read-transcript-last-message
  [runtime conversation-container-id]
  (foreign-one (:transcript-last-message-by-conversation runtime)
               [(keypath conversation-container-id)]))

(defn read-transcript-run
  [runtime request-id]
  (foreign-one (:transcript-runs runtime) [(keypath request-id)]))

(defn read-transcript-file-offset
  [runtime file-key]
  (foreign-one (:transcript-file-offsets runtime) [(keypath file-key)]))

(defn read-transcript-source-line
  [runtime file-key order-key]
  (foreign-one (:transcript-source-lines-by-file runtime)
               [(keypath file-key order-key)]))

(defn read-transcript-source-lines
  ([runtime file-key]
   (read-transcript-source-lines runtime file-key "" oc/default-outline-page-size))
  ([runtime file-key cursor limit]
   (foreign-select [(keypath file-key)
                    (sorted-map-range-from (or cursor "")
                                           (or limit oc/default-outline-page-size))
                    MAP-VALS]
                   (:transcript-source-lines-by-file runtime))))

(defn read-transcript-file-source-lines
  ([runtime file-key]
   (read-transcript-file-source-lines runtime file-key "" oc/default-outline-page-size))
  ([runtime file-key cursor limit]
   (foreign-select [(keypath file-key)
                    (sorted-map-range-from (or cursor "")
                                           (or limit oc/default-outline-page-size))
                    MAP-VALS]
                   (:transcript-file-source-lines-by-file runtime))))

(defn read-container
  [runtime container-id]
  (foreign-one (:containers-by-id runtime) [(keypath container-id)]))

(defn read-revision-history
  ([runtime container-id]
   (read-revision-history runtime container-id "" oc/default-outline-page-size))
  ([runtime container-id cursor limit]
   (foreign-select [(keypath container-id)
                    (sorted-map-range-from (or cursor "") (or limit oc/default-outline-page-size))
                    MAP-VALS]
                   (:revision-history-by-container runtime))))

(defn read-current-revision
  [runtime container-id]
  (foreign-invoke-query (:read-current-revision-query runtime) container-id))

(defn read-revision
  "Foreign point read for an immutable revision. `rev:fm:...` routing is
   covered by the P1 prefix gate in object_container/extract-object-key."
  [runtime revision-id]
  (foreign-one (:revisions-by-id runtime) [(keypath revision-id)]))

(defn read-unit
  [runtime unit-id]
  (foreign-invoke-query (:read-unit-query runtime) unit-id))

(defn await-object-container-decision
  ([runtime request]
   (await-object-container-decision runtime request 2000))
  ([runtime request timeout-ms]
   (let [partition-key (oc/request-partition-key request)
         request-id (oc/request-id request)]
     (await-object-container-decision runtime partition-key request-id timeout-ms)))
  ([runtime partition-key request-id timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop [decision (read-decision runtime partition-key request-id)]
       (cond
         (some? decision) decision
         (>= (System/currentTimeMillis) deadline) decision
         :else (do
                 (Thread/sleep 25)
                 (recur (read-decision runtime partition-key request-id))))))))
