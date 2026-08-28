(ns app.server.door.cluster
  "Cluster-backed runtime handles and explicit import controls.
   Takes: cluster-manager coordinates, module names, and ingest or migration requests.
   Gives: runtime handle maps, ingest and migration results, and watcher controllers.
   Holds: !memo, !default-address, and !first-light-failed."
  (:use [com.rpl.rama])
  (:require [app.server.ingest.ingest-watchers :as ingest-watchers]
            [app.server.rama.core :as core]
            [app.server.rama.face-arsenal :as face-arsenal]
            [app.server.ingest.git-spine :as git-spine]
            [app.server.episode.machine-cut :as machine-cut]
            [app.server.episode.material-circulation :as circulation]
            [app.server.rama.object-container :as oc]
            [app.server.ingest.block-distiller :as block-distiller]
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

;; ── Boot flag (CONTRACT §7 P3) ──────────────────────────────────────────────

(defn cluster-boot?
  "The dev boot rides the durable cluster BY DEFAULT (the package-close flip,
   CONTRACT §7 Close; G1–G6 green behind it). LAND_CLUSTER=0 opts back into
   the in-memory IPC boot. Tests are untouched either way — they use the IPC
   constructors directly; this seam governs only the app boot."
  []
  (not= "0" (System/getenv "LAND_CLUSTER")))

;; ── Manager + total-with-retry memo (T6) ────────────────────────────────────

(def conductor-config
  "Plain config map, localhost conductor, no secrets (CONTRACT §2)."
  {"conductor.host" "localhost"})

(defonce ^:private !memo (atom {}))

(defn- memo-total
  "Cache the first non-nil result of `thunk` under `k`; a throw or nil is
   printed and returned as nil WITHOUT being cached, so the next call
   retries (T6: no poisoned delays, cluster-down heals without a restart)."
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
  "The one RamaClusterManager for this JVM; nil while the cluster is down."
  []
  (memo-total :manager #(open-cluster-manager conductor-config)))

;; ── Handle bundles (shape-identical to the IPC constructors) ────────────────

(defn- pstates
  [mgr module-name names]
  (into {} (map (fn [n] [(keyword n) (foreign-pstate mgr module-name (str "$$" n))]))
        names))

(defn- queries
  [mgr module-name names]
  (into {} (map (fn [n] [(keyword (str n "-query")) (foreign-query mgr module-name n)]))
        names))

(defn- object-container-bundle*
  "Mirror of ocr/start-object-container-runtime!'s handle map (minus :ipc —
   only the close fns read it, and they are when-let-guarded). NO
   :block-edit-log-path: on the durable cluster the depot IS the log
   (DEPLOY.md correction-of-record), so append-block-edit-request-durably!'s
   when-let skips the WAL line natively."
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
  "Mirror of trail-view/start-trail-view-runtime!'s handle map (minus :ipc),
   including its exact key quirks (:module-name = the RELATION kernel name;
   :activity-by-bucket = $$relation-activity-by-bucket)."
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
  "Mirror of face-arsenal/start-face-arsenal-runtime!'s handle map. The wear
   WAL is OFF on the durable cluster (:face-wear-log-path nil — record-wear!
   honors an explicitly-nil path; the depot is the log)."
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
  "Cluster-backed trail runtime bundle; nil while the cluster is down (T6)."
  []
  (when-some [mgr (manager)]
    (memo-total :trail #(trail-view-bundle* mgr))))

(defn object-container-runtime
  "Cluster-backed OC runtime bundle (the face/distiller :oc-rt); nil while
   the cluster is down (T6)."
  []
  (when-some [mgr (manager)]
    (memo-total :object-container #(object-container-bundle* mgr))))

(defn face-arsenal-runtime
  "Cluster-backed arsenal bundle; nil while the cluster is down (T6)."
  []
  (when-some [mgr (manager)]
    (memo-total :face-arsenal #(face-arsenal-bundle* mgr))))

;; ── The face-projection runtime map (file_viewer's cluster branch) ──────────

(def default-conversation-prefix
  "The first-light default conversation (the G8 conversation; the one literal
   the IPC boot pins in file_viewer/find-default-transcript — kept in sync
   until the IPC branch retires)."
  "7c80ce2a")

(defn default-transcript-file
  "The default conversation's transcript file, nil off-box."
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
  "The deterministic first-light address: tid/transcript-object-key is a pure
   sha over source:conversation-id, so the durable cluster's boot COMPUTES the
   address and reads whatever material migration (P4) put there — it never
   re-harvests or re-distills at startup."
  []
  (when-some [conv-id (default-conversation-id)]
    (tid/transcript-object-key :claude-code conv-id)))

(defonce ^:private !default-address (atom nil))
(defonce ^:private !first-light-failed (atom false))

(defn face-projection-runtime
  "Cluster-mode replacement for file_viewer's face-projection-runtime delay
   body: the same map shape, no launches, no boot replays, no harvest/distill
   (those are migration/ingest — T9). Total under a down cluster: the atoms
   are stable defonces (resolve-request derefs them outside serve's try) and
   nil bundles ride the projections' honest-degrade paths (G20/G21/MC-T12)."
  []
  (when (and (nil? @!default-address) (some? (manager)))
    (reset! !default-address (default-address)))
  {:oc-rt (object-container-runtime)
   :arsenal-rt (face-arsenal-runtime)
   :rk-rt (trail-runtime)
   :!default-address !default-address
   :!first-light-failed !first-light-failed})

;; ── Explicit ingest (T9) + migration day (CONTRACT §7 P4) ───────────────────

(def ^:private spine-run-id
  "STABLE spine cursor run-id: the durable cluster is one long-lived instance,
   so the cost-only cursor may skip unchanged files across ingest runs (the
   IPC boot minted a fresh UUID per JVM because its cluster was ephemeral).
   If the cluster is ever destroyed and rebuilt empty, delete
   data/git-spine-cursor.edn with it — correctness stays the idempotency
   journal either way (G7)."
  "durable-ground-cluster")

(defn- repo-root [] (System/getProperty "user.dir"))

(defn ingest-config
  "The same cfg shape the IPC boot builds in file_viewer's trail delay, over
   the CLUSTER trail bundle. :assert-log-path stays the default (the bridge
   log replay-assert-log! reads at migration; the /assert route's write side
   is jetty's, addressed at P4)."
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
  "Explicit corpus ingest (T9: `bin/land ingest` / REPL — never startup).
   Same order the proven IPC boot used: initial sweep, then the git-spine
   sequence (assert-log replay → spine-sync → extract). The one-arity form
   is the `clj -X` entry (`bin/land ingest`) — it must System/exit: the
   manager's foreign-client threads are non-daemon, so a completed one-shot
   would otherwise hang the JVM forever (observed live at P3)."
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
  "Idempotently preserve provenance v0, explicitly activate its composition
   grammar revision, and install every other registered facet master through
   the same revision/pointer path. This is a deploy-time action, never an
   application startup write."
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
                  :time-ms (core/now-ms)})]))
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
                  :time-ms (core/now-ms)})]))
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
              :time-ms (core/now-ms)})}
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
  "Harvest + distill the default conversation into the DURABLE store, then
   bridge-replay the block-edit WAL (T8: replay only after the distill has
   created the target units — the proven IPC face-boot order). One-shot at
   migration; every later boot just reads."
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
          summary (block-distiller/distill-conversation!
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
  "Bridge-replay the machine-cut annotation WAL into the durable relation
   kernel (zero LLM calls — replay-wal! takes no adapter)."
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
    (let [page (block-distiller/river-page
                {:oc-rt oc-rt :object-key object-key}
                block-distiller/max-river-page-size)
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
  "Run P4's mechanical terminal-escape detector against git-spine commit facts
   and the provenance master's activation log."
  []
  (let [oc-rt (object-container-runtime)]
    (when-not oc-rt
      (throw (ex-info "cluster unavailable — run bin/land up first" {})))
    (circulation/terminal-escape-report
     (git-spine/read-commits (repo-root))
     (ocr/read-revision-history
      oc-rt
      (facet-master/active-pointer-container-id provenance-material/spec)
      ""
      100000))))

(defn migrate!
  "Migration day (CONTRACT §7 P4), T8 order: corpus ingest (imported bases
   first) → transcript harvest/distill + block-edit bridge → machine-cut
   bridge. Returns the G4 receipt map. Idempotent —
   every stage rides deterministic ids + idempotency journals. The one-arity
   form is the `clj -X` entry (`bin/land migrate`) — System/exit for the same
   non-daemon-thread reason as ingest!."
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
  "Live ingest watchers over the document roots (T9: watching resumes
   whenever this is called — REPL/dev-time, not startup). Returns the handle
   maps ({:stop! ...})."
  []
  (let [cfg (ingest-config)]
    (when-not (:runtime cfg)
      (throw (ex-info "cluster unavailable — run bin/land up first" {})))
    {:docs (ingest-watchers/start-ingest-watchers! cfg)}))
