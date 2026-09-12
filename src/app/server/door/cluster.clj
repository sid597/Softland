(ns app.server.door.cluster
  "Foreign handles for the external Rama cluster and explicit ingest controls.
   Resolves already-deployed object-container, relation, trail and face-arsenal
   resources; it does not deploy modules or launch the HTTP server. Explicit
   commands compose ingest, material migration and watcher owners in sibling
   folders. Durable state belongs to those Rama modules. This JVM retains the
   manager/bundles in !memo and default-address/failure atoms for projections.
   Failed acquisitions can be retried; retained handles are not health checks.
   No manager close/reset lifecycle is provided here. See README.md."
  (:use [com.rpl.rama])
  (:require [app.server.ingest.ingest-watchers :as ingest-watchers]
            [app.server.rama.envelope :as envelope]
            [app.server.rama.face-arsenal :as face-arsenal]
            [app.server.ingest.git-import :as git-import]
            [app.server.episode.machine-cut :as machine-cut]
            [app.server.episode.material-circulation :as circulation]
            [app.server.rama.object-container :as oc]
            [app.server.ingest.transcript-import :as transcript-import]
            [app.server.worn.facet-master :as facet-master]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.object-container.transcript-identity :as tid]
            [app.server.ingest.transcript :as transcript]
            [app.server.rama.relation-kernel :as rk]
            [app.server.rama.trail-view :as trail-view]
            [app.server.worn.facet-masters :as facet-masters]
            [app.server.worn.attention-material :as attention-material]
            [app.server.worn.foldable-material :as foldable-material]
            [app.server.worn.invocation-material :as invocation-material]
            [app.server.worn.positioned-material :as positioned-material]
            [app.server.worn.provenance-material :as provenance-material]
            [app.server.worn.threaded-material :as threaded-material]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; ── Legacy relation-log switch ────────────────────────────────────────────

(defn cluster-boot?
  "Return false only when LAND_CLUSTER is exactly 0.
   The current HTTP caller uses this to enable the legacy relation-assert file
   log. It still obtains external-cluster handles in either mode; this flag
   does not select an IPC application boot in the current source."
  []
  (not= "0" (System/getenv "LAND_CLUSTER")))

;; ── Process-owned manager and retained handles ────────────────────────────

(def conductor-config
  "Local conductor coordinates passed to open-cluster-manager."
  {"conductor.host" "localhost"})

(defonce ^:private !memo (atom {}))

(defn- memo-total
  "Retain a non-nil thunk result under k, serializing acquisition on !memo.
   Exceptions are printed and returned as nil; nil is not retained, so later
   calls retry acquisition. Successful entries are never invalidated here.
   Intended values are truthy manager/handle maps, not boolean results."
  [k thunk]
  (or (get @!memo k)
      (locking !memo
        (or (get @!memo k)
            (let [v (try (thunk)
                         (catch Throwable t
                           (println "[CLUSTER]" (name k) "unavailable:"
                                    (.getMessage t))
                           nil))]
              (when (some? v) (swap! !memo assoc k v))
              v)))))

(defn manager
  "Acquire and retain one RamaClusterManager for this JVM; nil if acquisition
   throws. Once retained it is returned without a fresh availability check.
   This namespace does not close it."
  []
  (memo-total :manager #(open-cluster-manager conductor-config)))

;; ── Foreign bundles using the runtime helper key conventions ──────────────

(defn- pstates
  "Resolve named module PStates into a keyword-keyed map; names omit the $$ prefix."
  [mgr module-name names]
  (into {} (map (fn [n] [(keyword n) (foreign-pstate mgr module-name (str "$$" n))]))
        names))

(defn- queries
  "Resolve module query names into a map keyed by <name>-query keywords."
  [mgr module-name names]
  (into {} (map (fn [n] [(keyword (str n "-query")) (foreign-query mgr module-name n)]))
        names))

(defn- object-container-bundle*
  "Resolve object-container and transcript-ops foreign handles on mgr.
   Uses the runtime helper key conventions, without owning an :ipc. Omitting
   :block-edit-log-path makes append-block-edit-request-durably! append only
   to the depot; no local edit log is written through this bundle."
  [mgr]
  (let [oc-name (get-module-name oc/object-container-module)
        ops-name (get-module-name oc/object-container-transcript-ops-module)]
    (merge
     {:module-name oc-name
      :transcript-ops-module-name ops-name
      :object-container-requests-depot
      (foreign-depot mgr oc-name "*object-container-requests-depot")
      :transcript-control-depot
      (foreign-depot mgr ops-name "*transcript-control-depot")
      :transcript-file-state-depot
      (foreign-depot mgr ops-name "*transcript-file-state-depot")}
     (pstates mgr oc-name
              ["requests-by-audit-id" "decisions-by-audit-id"
               "decisions-by-idempotency" "events-by-id"
               "import-completions-by-key" "source-artifacts-by-id"
               "source-versions-by-ref" "source-latest-by-ref"
               "source-ingest-completions-by-ref" "containers-by-id"
               "revisions-by-id" "revision-history-by-container"
               "derived-units-by-id" "unit-graduations-by-id"
               "source-anchors-by-target" "composition-children-by-parent"
               "composition-parent-by-child" "source-containers-by-source"
               "source-derived-units-by-source" "source-anchors-by-source"
               "source-edges-by-source" "native-identity-claims-by-container"
               "outline-by-document" "transcript-conversation-projection"
               "transcript-tool-calls-by-name" "transcript-audit-by-request"
               "transcript-last-message-by-conversation"
               "transcript-source-lines-by-file" "edit-order-by-target"])
     (pstates mgr ops-name
              ["transcript-runs" "transcript-file-offsets"
               "transcript-file-source-lines-by-file"])
     (queries mgr oc-name
              ["read-latest-source-by-ref" "read-source-by-ref-version"
               "read-unit" "read-current-revision"
               "read-common-material-for-source"]))))

(defn- trail-view-bundle*
  "Resolve a composite trail bundle over object-container, transcript-ops,
   relation-kernel and TrailView modules. :module-name names the relation
   kernel; :activity-by-bucket resolves $$relation-activity-by-bucket. This
   bundle borrows all state and does not own an IPC or a deployed module."
  [mgr]
  (let [oc-name (get-module-name oc/object-container-module)
        ops-name (get-module-name oc/object-container-transcript-ops-module)
        rel-name (get-module-name rk/relation-kernel-module)
        tv-name (get-module-name trail-view/trail-view-module)]
    (merge
     {:trail-view-module-name tv-name
      :oc-module-name oc-name
      :oc-ops-module-name ops-name
      :module-name rel-name
      :object-container-requests-depot
      (foreign-depot mgr oc-name "*object-container-requests-depot")
      :relation-request-depot
      (foreign-depot mgr rel-name "*relation-request-depot")
      :relations-by-id (foreign-pstate mgr rel-name "$$relations-by-id")
      :activity-by-bucket
      (foreign-pstate mgr rel-name "$$relation-activity-by-bucket")}
     (pstates mgr oc-name
              ["decisions-by-audit-id" "containers-by-id"
               "source-artifacts-by-id" "source-anchors-by-target"
               "source-versions-by-ref" "source-latest-by-ref"
               "outline-by-document"])
     (queries mgr rel-name
              ["relations-for-targets" "relation-detail" "relation-activity"])
     (queries mgr oc-name
              ["read-latest-source-by-ref" "read-source-by-ref-version"
               "read-current-revision"])
     (queries mgr tv-name
              ["context-bundle" "conversation-trail"
               "recent-file-activity" "recent-source-activity"]))))

(defn- face-arsenal-bundle*
  "Resolve face registry and wear handles without owning an IPC.
   Explicit :face-wear-log-path nil disables record-wear!'s file-log fallback;
   wear events still append to the deployed face-arsenal depot."
  [mgr]
  (let [arsenal-name (get-module-name face-arsenal/face-arsenal-module)]
    (merge
     {:face-arsenal-owns-ipc? false
      :face-arsenal-module-name arsenal-name
      :face-arsenal-depot (foreign-depot mgr arsenal-name "*face-arsenal-depot")
      :face-wear-log-path nil}
     (pstates mgr arsenal-name
              ["faces-by-name" "wear-events-by-face"
               "wear-counts-by-face" "wear-journal-by-face"]))))

(defn trail-runtime
  "Return the retained composite trail handle bundle, or nil if acquisition fails.
   A non-nil bundle does not establish that subsequent remote operations succeed."
  []
  (when-some [mgr (manager)]
    (memo-total :trail #(trail-view-bundle* mgr))))

(defn object-container-runtime
  "Return the retained object-container/transcript-ops bundle, or nil on failed
   acquisition. A non-nil bundle does not establish that subsequent remote operations succeed."
  []
  (when-some [mgr (manager)]
    (memo-total :object-container #(object-container-bundle* mgr))))

(defn face-arsenal-runtime
  "Return the retained face-arsenal handle bundle, or nil if acquisition fails.
   A non-nil bundle does not establish that subsequent remote operations succeed."
  []
  (when-some [mgr (manager)]
    (memo-total :face-arsenal #(face-arsenal-bundle* mgr))))

;; ── Runtime context consumed by HTTP projections and acts ─────────────────

(def default-conversation-prefix
  "Filename prefix used to locate the explicit first-light import source."
  "7c80ce2a")

(defn default-transcript-file
  "Find the first matching default-session JSONL file in the user's Softland
   Claude project directory, or nil. Directory iteration is not sorted; this
   locates a file without reading or importing its contents."
  []
  (let [dir (io/file (str (System/getProperty "user.home")
                          "/.claude/projects/-mnt-data-projects-Softland"))]
    (when (.isDirectory dir)
      (first (filter #(re-find (re-pattern (str "^" default-conversation-prefix
                                                "-.*\\.jsonl$"))
                               (.getName ^java.io.File %))
                     (.listFiles dir))))))

(defn default-conversation-id
  "Conversation id = the transcript file stem (Claude Code sessionId)."
  []
  (when-some [^java.io.File f (default-transcript-file)]
    (str/replace (.getName f) #"\.jsonl$" "")))

(defn default-address
  "Derive a transcript object key from the locally found default conversation
   id, or nil when that file is absent. Does not check that its material exists
   in Rama and performs no harvest or distillation."
  []
  (when-some [conv-id (default-conversation-id)]
    (tid/transcript-object-key :claude-code conv-id)))

(defonce ^:private !default-address (atom nil))
(defonce ^:private !first-light-failed (atom false))

(defn face-projection-runtime
  "Return {:oc-rt :arsenal-rt :rk-rt :!default-address :!first-light-failed}
   for HTTP projection/act callers. Resolves handles without launching modules
   or replaying imports. The stable atoms belong to this JVM; this namespace
   initializes the address when a manager exists, but does not set the failure
   atom. Individual bundles can be nil or fail on later remote use."
  []
  (when (and (nil? @!default-address) (some? (manager)))
    (reset! !default-address (default-address)))
  {:oc-rt (object-container-runtime)
   :arsenal-rt (face-arsenal-runtime)
   :rk-rt (trail-runtime)
   :!default-address !default-address
   :!first-light-failed !first-light-failed})

;; ── Explicit ingest and migration commands ────────────────────────────────

(def ^:private spine-run-id
  "Stable run id supplied to git-import's file cursor across explicit ingests.
   Rebuilding the external store requires coordinating the cursor file with
   that rebuild; this namespace does not detect a replaced store."
  "durable-ground-cluster")

(defn- repo-root
  "Return user.dir; command callers must run from the repository root."
  [] (System/getProperty "user.dir"))

(defn ingest-config
  "Build watcher/import configuration from user.dir and the external trail
   bundle. Includes existing docs/current-mental-model and vision roots, Claude
   transcript roots, and a stable Git cursor. :assert-log-path is nil, which
   git-import/replay-assert-log! interprets as its default bridge-log path,
   not as disabling replay. Missing roots are silently omitted."
  []
  (let [dir (repo-root)]
    {:runtime (trail-runtime)
     :roots (into []
                  (filter #(.exists (io/file %)))
                  [(str dir "/docs/current-mental-model")
                   (str dir "/vision")])
     :repo-root dir
     :transcript-roots (into []
                             (filter #(.exists (io/file %)))
                             [(str (System/getProperty "user.home")
                                   "/.claude/projects")])
     :spine-cursor-path (str dir "/data/git-spine-cursor.edn")
     :spine-run-id spine-run-id
     :assert-log-path nil}))

(defn ingest!
  "Run the initial corpus sweep, then the Git bridge replay/sync/join sequence
   using ingest-watchers. Requires a non-nil trail bundle. Returns {:sweep ...};
   the Git sequence logs its own results and catches stage failures, so this
   return is not an all-stages-success receipt. Explicit command, not startup.
   The one-argument clj -X entry exits the JVM with 0 after a normal return;
   the zero-argument REPL entry leaves the retained manager open."
  ([_argmap] (ingest!) (System/exit 0))
  ([]
  (let [cfg (ingest-config)]
    (when-not (:runtime cfg)
      (throw (ex-info "cluster unavailable — run bin/land up first" {})))
    (let [sweep (ingest-watchers/initial-sweep! cfg)]
      (println "[CLUSTER-INGEST] sweep:" (:imported sweep) "of"
               (:attempted sweep) "imported")
      (ingest-watchers/run-git-spine-boot! cfg)
      (println "[CLUSTER-INGEST] git-spine done")
      {:sweep sweep}))))

(defn facet-materials-ingest!
  "Ensure registered facet masters, import the named grammar revisions, and
   activate them through facet-master's revision/pointer operations. Returns
   per-migration results and final states; it does not make the whole sequence
   transactional or reject every failed subresult itself. This is an explicit
   deployment command. The one-argument entry exits on normal completion."
  ([_argmap] (facet-materials-ingest!) (System/exit 0))
  ([]
   (let [oc-rt (object-container-runtime)]
     (when-not oc-rt
       (throw (ex-info "cluster unavailable — run bin/land up first" {})))
     (let [provenance-v0
           (facet-master/ensure-master! oc-rt provenance-material/spec)
           provenance-v1
           (facet-master/ensure-active-source!
            oc-rt
            provenance-material/spec
            provenance-material/composition-source
            {:request-id "facet-master-provenance-v1"
             :activation-request-id
             "facet-master-provenance-activate-v1"
             :time-ms 1})
           masters
           (into
            {}
            (comp
             (remove
              #(= provenance-material/master-id
                  (:facet-master/id %)))
             (map
              (fn [spec]
                [(:facet-master/id spec)
                 (facet-master/ensure-master! oc-rt spec)])))
            facet-masters/specs)
           ;; editable-material P5: the three bindings-carrying masters take
           ;; their gesture-row grammar the same explicit way provenance took
           ;; its composition grammar — new immutable bytes, one activation.
           ;; v0 revisions keep their original meaning and stay rewearable.
           ;;
           ;; This ingest makes the rows MATERIAL (revisable, activatable). It
           ;; is not what makes them work: a master still serving v0 has no
           ;; rows, and the client's code-floor tier carries the behavior. The
           ;; client is correct before this ever runs.
           bindings-migrations
           (into
            (sorted-map)
            (map
             (fn [[spec source slug]]
               [(:facet-master/id spec)
                (facet-master/ensure-active-source!
                 oc-rt spec source
                 {:request-id (str "facet-master-" slug "-v1")
                  :activation-request-id
                  (str "facet-master-" slug "-activate-v1")
                  :time-ms 2})]))
            [[attention-material/spec
              attention-material/bindings-source "attention"]
             [foldable-material/spec
              foldable-material/bindings-source "foldable"]
             [positioned-material/spec
              positioned-material/bindings-source "positioned"]])
           ;; editable-material P6 · T10 — grammar v2 on the same three masters.
           ;; v2 = v1's rows verbatim under a validator that refuses a row whose
           ;; SITE cannot feed its verb's required args (the P5 gate's
           ;; arg-starved rebind). Additive: v1 keeps its own declaration and
           ;; every durable v1 revision stays rewearable under it.
           ;;
           ;; R3 — the time is HONEST. P1/P3/P5 stamped 0/1/2 here, which is
           ;; precisely what made the activation history non-monotone in clock
           ;; terms (P4's finding; the quirks registry's causal-order law). Those
           ;; three constants are grandfathered durable history and are never
           ;; rewritten; G11 exists to keep a FOURTH from joining them. Import
           ;; identity is content-hash keyed, so honesty costs no idempotency.
           strict-bindings-migrations
           (into
            (sorted-map)
            (map
             (fn [[spec source slug]]
               [(:facet-master/id spec)
                (facet-master/ensure-active-source!
                 oc-rt spec source
                 {:request-id (str "facet-master-" slug "-v2")
                  :activation-request-id
                  (str "facet-master-" slug "-activate-v2")
                  :time-ms (envelope/now-ms)})]))
            [[attention-material/spec
              attention-material/strict-bindings-source "attention"]
             [foldable-material/spec
              foldable-material/strict-bindings-source "foldable"]
             [positioned-material/spec
              positioned-material/strict-bindings-source "positioned"]])
           ;; smalltalk-ui-vm P1 · W7 — the threaded master's SECOND grammar
           ;; is an explicit immutable-source migration. T7: the new rail and
           ;; indent policy is material, never a hardcoded block branch. The
           ;; anatomy master itself joins through the generic `specs` sweep
           ;; above; no source-specific bootstrap path is needed.
           threaded-migrations
           (into
            (sorted-map)
            (map
             (fn [[spec source slug]]
               [(:facet-master/id spec)
                (facet-master/ensure-active-source!
                 oc-rt spec source
                 {:request-id (str "facet-master-" slug "-v1")
                  :activation-request-id
                  (str "facet-master-" slug "-activate-v1")
                  :time-ms (envelope/now-ms)})]))
            [[threaded-material/spec
              threaded-material/thread-edge-source
              "threaded"]])
           ;; smalltalk-ui-vm P2 · W7 — invocation joins the generic registry
           ;; sweep above (one existing bootstrap artery). Foldable v3 is the
           ;; explicit immutable grammar migration that births paste policy.
           paste-clamp-migrations
           {foldable-material/master-id
            (facet-master/ensure-active-source!
             oc-rt
             foldable-material/spec
             foldable-material/paste-clamp-source
             {:request-id "facet-master-foldable-v3"
              :activation-request-id "facet-master-foldable-activate-v3"
              :time-ms (envelope/now-ms)})}
           results
           (into
            (into
             {provenance-material/master-id (:state provenance-v1)}
             (map (fn [[master-id boot]]
                    [master-id (:state boot)]))
             masters)
            (map (fn [[master-id migration]]
                   [master-id (:state migration)]))
            (merge bindings-migrations
                   strict-bindings-migrations
                   threaded-migrations
                   paste-clamp-migrations))]
       (println
        "[CLUSTER-INGEST] facet materials:"
        (into
         (sorted-map)
         (map
          (fn [[master-id state]]
            [master-id
             {:latest-revision-id
              (some-> state :latest-revision :revision-id)
              :active-revision-id
              (some-> state :active-revision :revision-id)
              :pointer-revision-id
              (some-> state :active-pointer :revision-id)}]))
         results))
       {:provenance-v0 provenance-v0
        :provenance-v1 provenance-v1
        :masters masters
        :bindings-migrations bindings-migrations
        :strict-bindings-migrations strict-bindings-migrations
        :threaded-migrations threaded-migrations
        :invocation-bootstrap
        (get masters invocation-material/master-id)
        :paste-clamp-migrations paste-clamp-migrations
        :states results}))))

(defn first-light-ingest!
  "Harvest and distill the locally located default conversation into the
   external object-container, then replay the block-edit bridge log after its
   targets exist. Throws on missing runtime/file or incomplete harvest; returns
   the distillation summary and bridge counts. Later-stage failures can leave
   earlier imports present. Called by migrate!, not by HTTP startup."
  []
  (let [oc-rt (object-container-runtime)
        file (default-transcript-file)]
    (when-not oc-rt
      (throw (ex-info "cluster unavailable — run bin/land up first" {})))
    (when-not file
      (throw (ex-info "default transcript not found on this box" {})))
    (let [req (transcript/transcript-request
               :transcript/harvest
               {:transcript/request-id "durable-ground-migration"
                :transcript/source :claude-code
                :transcript/paths [(.getPath ^java.io.File file)]
                :time-ms 0})
          harvest (transcript/harvest-transcripts-into-object-container! oc-rt req)
          _ (when-not (= :complete (:status harvest))
              (throw (ex-info "harvest did not complete" {:harvest harvest})))
          summary (transcript-import/distill-conversation!
                   {:oc-rt oc-rt
                    :source :claude-code
                    :conversation-id (default-conversation-id)})
          edits (ocr/replay-block-edit-log!
                 (assoc oc-rt :block-edit-log-path
                        (ocr/default-block-edit-log-path {})))]
      (println "[CLUSTER-INGEST] distilled" (:object-key summary)
               "river=" (:river summary) "debris=" (:debris summary)
               "· block-edit bridge:" (:replayed edits) "replayed,"
               (:failed edits) "failed")
      {:object-key (:object-key summary)
       :river (:river summary)
       :debris (:debris summary)
       :block-edit-bridge edits})))

(defn machine-cut-bridge!
  "Replay the machine-cut annotation file log into the external relation
   runtime, returning replay counts. Requires a runtime; passes no LLM adapter
   to the replay owner and does not invoke classification here."
  []
  (let [rk-rt (trail-runtime)]
    (when-not rk-rt
      (throw (ex-info "cluster unavailable — run bin/land up first" {})))
    (let [mc (machine-cut/replay-wal! {:rk-rt rk-rt})]
      (println "[CLUSTER-INGEST] machine-cut bridge:" (:lines mc) "lines,"
               (:asserted mc) "asserted," (:retracted mc) "retracted,"
               (:failed mc) "failed")
      mc)))

(defn starter-culture-ingest!
  "Editable-material P4 migration adapter. Apply the two literal legacy
   predicates to the existing conversation window and assert silver
   :instance-of associations. These are associations only — no historical
   receipt is manufactured. The bounded river-page truncation fact is returned
   so this migration never overclaims whole-corpus coverage."
  [object-key]
  (let [oc-rt (object-container-runtime)
        rk-rt (trail-runtime)]
    (when-not (and oc-rt rk-rt)
      (throw (ex-info "cluster unavailable — run bin/land up first" {})))
    (let [page (transcript-import/river-page
                {:oc-rt oc-rt :object-key object-key}
                transcript-import/max-river-page-size)
          read-plan (:river-page/read-plan (meta page))
          result (circulation/seed-starter-culture!
                  rk-rt (vec page) (System/currentTimeMillis))
          receipt (assoc result
                         :window/blocks-returned (:blocks-returned read-plan)
                         :window/truncated? (boolean (:truncated? read-plan)))]
      (println "[CLUSTER-INGEST] starter culture:"
               (:materialized result) "of" (:matched result)
               "associations · truncated?" (:window/truncated? receipt))
      receipt)))

(defn terminal-escape-report
  "Compare repository commit facts with up to 100000 provenance active-pointer
   revisions through material-circulation's detector. Requires an OC runtime;
   returns that owner's report without changing stored material."
  []
  (let [oc-rt (object-container-runtime)]
    (when-not oc-rt
      (throw (ex-info "cluster unavailable — run bin/land up first" {})))
    (circulation/terminal-escape-report
     (git-import/read-commits (repo-root))
     (ocr/read-revision-history
      oc-rt
      (facet-master/active-pointer-container-id provenance-material/spec)
      ""
      100000))))

(defn migrate!
  "Run explicit migration in order: corpus ingest; default transcript
   harvest/distill and block-edit replay; bounded starter associations;
   terminal-escape report; machine-cut replay. Returns each stage's result.
   This is a sequential composition, not a cross-module transaction; inspect
   nested failure/coverage fields. Retry identity belongs to the stage owners.
   The one-argument clj -X entry exits on normal completion. Facet grammar
   installation is the separate facet-materials-ingest! command."
  ([_argmap] (migrate!) (System/exit 0))
  ([]
   (let [corpus (ingest!)
         first-light (first-light-ingest!)
         starter-culture (starter-culture-ingest! (:object-key first-light))
         terminal-escape (terminal-escape-report)
         machine-cut (machine-cut-bridge!)]
     (println "[CLUSTER-INGEST] migration receipt:")
     (println {:corpus corpus
               :first-light (select-keys first-light [:object-key :river :debris :block-edit-bridge])
               :starter-culture starter-culture
               :terminal-escape terminal-escape
               :machine-cut machine-cut})
     {:corpus corpus
      :first-light first-light
      :starter-culture starter-culture
      :terminal-escape terminal-escape
      :machine-cut machine-cut})))

(defn start-watchers!
  "Start document-root watchers using ingest-config; requires a trail bundle.
   Returns {:docs {:stop! ... :watch-service ... :roots ...}}. The caller must
   invoke the nested :stop! function to release watcher threads and resources.
   Does not perform the initial sweep or watch the configured transcript roots."
  []
  (let [cfg (ingest-config)]
    (when-not (:runtime cfg)
      (throw (ex-info "cluster unavailable — run bin/land up first" {})))
    {:docs (ingest-watchers/start-ingest-watchers! cfg)}))
