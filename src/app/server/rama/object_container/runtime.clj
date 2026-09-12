(ns app.server.rama.object-container.runtime
  "Foreign-client access to the object-container and transcript-operations modules.
   Request maps enter their depots; point reads and queries return stored rows,
   decisions, or material bundles. Handles are borrowed from the runtime map.
   start-object-container-runtime! creates an owned IPC for focused use;
   door/cluster builds equivalent handles for the durable cluster.
   Optional block-edit file logging belongs to the appender, not the PStates.
   Default append acknowledgement and decision polling are separate operations."
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [app.server.rama.object-container :as oc]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [com.rpl.rama.test :refer [create-ipc launch-module!]]))

(defn start-object-container-runtime!
  "Create an IPC, launch the common module then transcript operations, and return
   their depot/PState/query handles. The caller owns this IPC and must close it.
   This does not attach to the durable cluster or replay edit files."
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
  "Close the IPC in runtime when present, swallowing close exceptions. A
   cluster-backed handle map without :ipc has nothing to close here."
  [runtime]
  (when-let [ipc (:ipc runtime)]
    (try
      (.close ipc)
      (catch Exception _ nil))))

(def block-edit-log-relative-path "data/block-edit-log.ednl")

(defn default-block-edit-log-path
  "Resolve data/block-edit-log.ednl beneath :repo-root or the JVM working directory."
  [{:keys [repo-root]}]
  (str (or repo-root (System/getProperty "user.dir")) "/"
       block-edit-log-relative-path))

(def ^:private block-edit-log-lock (Object.))

(defn- append-block-edit-log-line!
  "Append one UTF-8 EDN request line under a process-local lock, converting
   records to maps and creating parent directories. Closes the writer; does not
   fsync or coordinate writers in other processes."
  [path request]
  (locking block-edit-log-lock
    (let [f (io/file path)]
      (io/make-parents f)
      (with-open [w (io/writer f :append true :encoding "UTF-8")]
        (.write w (pr-str (walk/postwalk #(if (record? %) (into {} %) %) request)))
        (.write w "\n")))))

(defn append-block-edit-request-durably!
  "Append an edit request to the optional :block-edit-log-path first, then to
   the object-container depot with :ack by default. A missing/nil path skips
   file logging. Returns Rama append acknowledgements, not an acceptance verdict;
   read the decision to distinguish accepted and rejected requests. The local
   file append has no fsync or transaction coupling to the depot append."
  ([runtime request]
   (append-block-edit-request-durably! runtime request :ack))
  ([runtime request ack-level]
   (when-let [path (:block-edit-log-path runtime)]
     (append-block-edit-log-line! path request))
   (foreign-append! (:object-container-requests-depot runtime) request ack-level)))

(defn replay-block-edit-log!
  "Read the configured or default EDN edit log and append each :object/edit
   request to the depot with :ack. Does not append back to the file. Returns
   {:replayed n :failed n}; replayed counts successful append calls, including
   rejected or already-decided requests. Missing file returns zero counts;
   malformed lines and append exceptions are counted and skipped."
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
  "Append a request with :append-ack by default and return Rama acknowledgements.
   That default waits for depot storage, not the topology decision. Pass :ack
   for stream completion or poll await-object-container-decision."
  ([runtime request]
   (append-object-container-request! runtime request :append-ack))
  ([runtime request ack-level]
   (foreign-append! (:object-container-requests-depot runtime) request ack-level)))

(defn append-transcript-control!
  "Append a transcript run/control request with :ack by default. Returns Rama
   acknowledgements; the operation records control state and does not harvest files."
  ([runtime request]
   (append-transcript-control! runtime request :ack))
  ([runtime request ack-level]
   (foreign-append! (:transcript-control-depot runtime) request ack-level)))

(defn append-transcript-file-state!
  "Append a file observation to transcript operations with :append-ack by default.
   This acknowledgement does not establish that its safe resume offset advanced."
  ([runtime file-state]
   (append-transcript-file-state! runtime file-state :append-ack))
  ([runtime file-state ack-level]
   (foreign-append! (:transcript-file-state-depot runtime) file-state ack-level)))

(defn foreign-one
  "Select one value from a supplied foreign PState and Rama path; callers own the handle."
  [pstate path]
  (foreign-select-one path pstate))

(defn read-request
  "Read the audited raw-request row by a request map or partition-key/request-id pair."
  ([runtime request]
   (read-request runtime (oc/request-partition-key request) (oc/request-id request)))
  ([runtime partition-key request-id]
   (foreign-one (:requests-by-audit-id runtime)
                [(keypath (oc/audit-id partition-key request-id))])))

(defn read-audit-request
  "Alias of read-request with the same request-map or explicit-key arities."
  ([runtime request]
   (read-request runtime request))
  ([runtime partition-key request-id]
   (read-request runtime partition-key request-id)))

(defn read-decision
  "Read the audit decision by request or partition-key/request-id; nil means no
   visible decision. Accepted and rejected rows are both returned."
  ([runtime request]
   (read-decision runtime (oc/request-partition-key request) (oc/request-id request)))
  ([runtime partition-key request-id]
   (foreign-one (:decisions-by-audit-id runtime)
                [(keypath (oc/decision-id-for-audit-id
                           (oc/audit-id partition-key request-id)))])))

(defn read-source
  "Read a source-artifact row by source-id, or derive the markdown-style source-id
   from source-ref and source-hash. Returns nil when absent."
  ([runtime source-id]
   (foreign-one (:source-artifacts-by-id runtime) [(keypath source-id)]))
  ([runtime source-ref source-hash]
   (read-source runtime (oc/source-id-for source-ref source-hash))))

(defn read-latest-source-by-ref
  "Invoke the source-ref query to resolve its latest indexed source-artifact row."
  [runtime source-ref]
  (foreign-invoke-query (:read-latest-source-by-ref-query runtime) source-ref))

(defn read-source-by-ref-version
  "Resolve an exact source-ref/source-version-key through the module query."
  [runtime source-ref source-version-key]
  (foreign-invoke-query (:read-source-by-ref-version-query runtime)
                        source-ref
                        source-version-key))

(defn read-import-completion
  "Read the completion row for an import-key; nil means no visible completion."
  [runtime import-key]
  (foreign-one (:import-completions-by-key runtime) [(keypath import-key)]))

(defn read-source-material-refs
  "Read all source-index reference rows under source-id from the runtime PState
   selected by pstate-key. These are references, not the referenced material."
  [runtime pstate-key source-id]
  (foreign-select [(keypath source-id) MAP-VALS] (pstate-key runtime)))

(defn read-source-containers
  "Read container reference rows indexed under source-id."
  [runtime source-id]
  (read-source-material-refs runtime :source-containers-by-source source-id))

(defn read-source-derived-units
  "Read derived-unit reference rows indexed under source-id."
  [runtime source-id]
  (read-source-material-refs runtime :source-derived-units-by-source source-id))

(defn read-source-anchor-refs
  "Read source-anchor reference rows indexed under source-id."
  [runtime source-id]
  (read-source-material-refs runtime :source-anchors-by-source source-id))

(defn read-source-edge-refs
  "Read composition-edge reference rows indexed under source-id."
  [runtime source-id]
  (read-source-material-refs runtime :source-edges-by-source source-id))

(defn read-common-material-for-source
  "Query source-indexed material for selected categories, each with an inclusive
   cursor and limit. Defaults to all common categories and the outline page size.
   Returns a CommonMaterialBundle; the module resolves references to current rows."
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
  "Read all source-anchor rows indexed under target-id."
  [runtime target-id]
  (foreign-select [(keypath target-id) MAP-VALS]
                  (:source-anchors-by-target runtime)))

(defn read-composition-children
  "Read all child composition edges under parent-slot-id."
  [runtime parent-slot-id]
  (foreign-select [(keypath parent-slot-id) MAP-VALS]
                  (:composition-children-by-parent runtime)))

(defn read-composition-parents
  "Read all parent composition edges indexed under child-slot-id."
  [runtime child-slot-id]
  (foreign-select [(keypath child-slot-id) MAP-VALS]
                  (:composition-parent-by-child runtime)))

(defn read-native-identity-claim
  "Read the stored native identity claim for container-id, or nil."
  [runtime container-id]
  (foreign-one (:native-identity-claims-by-container runtime)
               [(keypath container-id)]))

(defn read-outline
  "Read an outline page for document-id from the inclusive order-key cursor.
   Defaults to an empty cursor and the common outline page size."
  ([runtime document-id]
   (read-outline runtime document-id "" oc/default-outline-page-size))
  ([runtime document-id cursor limit]
   (foreign-select [(keypath document-id)
                    (sorted-map-range-from (or cursor "") (or limit oc/default-outline-page-size))
                    MAP-VALS]
                   (:outline-by-document runtime))))

(defn read-transcript-conversation-projection
  "Read a conversation projection page from an inclusive order-key cursor;
   defaults to the first page and the common outline page size."
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
  "Read a tool-name index page from an inclusive order-key cursor."
  ([runtime tool-name]
   (read-transcript-tool-calls-by-name runtime tool-name "" oc/default-outline-page-size))
  ([runtime tool-name cursor limit]
   (foreign-select [(keypath tool-name)
                    (sorted-map-range-from (or cursor "")
                                           (or limit oc/default-outline-page-size))
                    MAP-VALS]
                   (:transcript-tool-calls-by-name runtime))))

(defn read-transcript-audit-entries
  "Read transcript audit entries under request-id from an inclusive cursor."
  ([runtime request-id]
   (read-transcript-audit-entries runtime request-id "" oc/default-outline-page-size))
  ([runtime request-id cursor limit]
   (foreign-select [(keypath request-id)
                    (sorted-map-range-from (or cursor "")
                                           (or limit oc/default-outline-page-size))
                    MAP-VALS]
                   (:transcript-audit-by-request runtime))))

(defn read-transcript-last-message
  "Read the last-message projection for conversation-container-id, or nil."
  [runtime conversation-container-id]
  (foreign-one (:transcript-last-message-by-conversation runtime)
               [(keypath conversation-container-id)]))

(defn read-transcript-run
  "Read transcript operational run state by request-id, or nil."
  [runtime request-id]
  (foreign-one (:transcript-runs runtime) [(keypath request-id)]))

(defn read-transcript-file-offset
  "Read transcript operational offset/resume state by file-key, or nil."
  [runtime file-key]
  (foreign-one (:transcript-file-offsets runtime) [(keypath file-key)]))

(defn read-transcript-source-line
  "Read one common-module source-line completion by file-key and order-key."
  [runtime file-key order-key]
  (foreign-one (:transcript-source-lines-by-file runtime)
               [(keypath file-key order-key)]))

(defn read-transcript-source-lines
  "Read a page of common-module source-line completions from an inclusive cursor."
  ([runtime file-key]
   (read-transcript-source-lines runtime file-key "" oc/default-outline-page-size))
  ([runtime file-key cursor limit]
   (foreign-select [(keypath file-key)
                    (sorted-map-range-from (or cursor "")
                                           (or limit oc/default-outline-page-size))
                    MAP-VALS]
                   (:transcript-source-lines-by-file runtime))))

(defn read-transcript-file-source-lines
  "Read a page of observed source lines from the operations module. Observed
   lines are distinct from the common module completion rows."
  ([runtime file-key]
   (read-transcript-file-source-lines runtime file-key "" oc/default-outline-page-size))
  ([runtime file-key cursor limit]
   (foreign-select [(keypath file-key)
                    (sorted-map-range-from (or cursor "")
                                           (or limit oc/default-outline-page-size))
                    MAP-VALS]
                   (:transcript-file-source-lines-by-file runtime))))

(defn read-container
  "Read a current object-container row by container-id, or nil."
  [runtime container-id]
  (foreign-one (:containers-by-id runtime) [(keypath container-id)]))

(defn read-revision-history
  "Read revision rows for container-id from an inclusive order-key cursor;
   defaults to the first page and the common outline page size."
  ([runtime container-id]
   (read-revision-history runtime container-id "" oc/default-outline-page-size))
  ([runtime container-id cursor limit]
   (foreign-select [(keypath container-id)
                    (sorted-map-range-from (or cursor "") (or limit oc/default-outline-page-size))
                    MAP-VALS]
                   (:revision-history-by-container runtime))))

(defn read-current-revision
  "Query the revision named by the current container pointer, or nil."
  [runtime container-id]
  (foreign-invoke-query (:read-current-revision-query runtime) container-id))

(defn read-revision
  "Point-read a revision row by revision-id using the module key partitioner."
  [runtime revision-id]
  (foreign-one (:revisions-by-id runtime) [(keypath revision-id)]))

(defn read-unit
  "Query a derived unit with its graduation, if any; return the current target
   and content as a UnitReadResult, or nil when the unit is absent."
  [runtime unit-id]
  (foreign-invoke-query (:read-unit-query runtime) unit-id))

(defn await-object-container-decision
  "Poll the audit decision every 25 ms until visible or the deadline passes.
   Defaults to 2000 ms; returns the accepted/rejected row or nil on timeout.
   A visible decision alone does not prove every cross-partition import index
   has finished; this helper does not poll those projections."
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
