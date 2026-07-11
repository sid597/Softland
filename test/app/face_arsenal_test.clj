(ns app.face-arsenal-test
  "Framework Wave 2 · Lane W2-D gates G17–G21 (CONTRACT §19).

   Pure units (no IPC): adapter identity + T18 one-compiler verdicts + the
   CLOSED provenance→kind mapping (G19's impossible-by-construction half) +
   the G18 extract-object-key routing unit + the G21 mechanical read-only
   scan + projection totality.

   ONE IPC deftest drives everything durable (the lane's minimize-IPC rule):
   OC + arsenal share one in-process cluster; the relation kernel runs its own
   (its runtime ctor owns its IPC). Barriers are deterministic: arsenal writes
   use :ack (PState-visible on return); OC imports latch on the EXISTING
   append→await-decision seam; relation edges use rk's own sanctioned
   await-relation read-after-write barrier (microbatch). A boot-replay cluster
   (arsenal module alone) exercises G20's WAL reconstruction — the fresh
   cluster IS the point of that gate.

   G17 receipt scope (lane fence): the receipt runs over the REAL faces dir
   (whatever it holds — lane E lands more files in parallel) BY ENUMERATION,
   plus synthetic fixtures in a scratch root for the edited / malformed /
   identical-resave classes. Live-save runs against the SCRATCH root only —
   mutating the committed faces dir mid-test would corrupt repo state; the
   full-dir receipt re-runs at G26 (judgment call, recorded in the artifact)."
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [app.client.workspace.face-assembly :as face-assembly]
            [app.client.workspace.face-primitives :as face-primitives]
            [app.server.ingest-watchers :as watchers]
            [app.server.rama.face-arsenal :as fa]
            [app.server.rama.face-projection :as fp]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.assembly-adapter :as aa]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.relation-kernel :as rk]
            [app.server.rama.util-fns :as util-fns]
            [com.rpl.rama :refer [foreign-select foreign-select-one]]
            [com.rpl.rama.path :refer [keypath ALL]])
  (:import [java.io File]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.util.concurrent CountDownLatch TimeUnit]))

(def ^:private real-faces-dir "resources/public/faces")

;; ===========================================================================
;; Fixture sources (written into a scratch root at test time — never into the
;; committed faces dir; the fence).
;; ===========================================================================

(def ^:private boxes-face-source
  ;; VALID face with the FULL §17 provenance envelope (drives G19).
  (str "{:assembly/name \"boxes-w2d-test\"\n"
       " :assembly/grammar 0\n"
       " :assembly/belief \"w2d receipt fixture\"\n"
       " :assembly/status :candidate\n"
       " :assembly/author \"sid\"\n"
       " :assembly/birthed-by \"chat:w2d-test-conversation\"\n"
       " :assembly/based-on \"outline-face\"\n"
       " :assembly/supersedes \"old-face\"\n"
       " :root {:prim :stack :props {:gap 4}\n"
       "        :children [{:prim :badge :props {:label \"hi\"}}]}}\n"))

(def ^:private plain-face-source
  ;; VALID face with NO provenance fields (zero edges expected).
  (str "{:assembly/name \"plain-w2d-test\"\n"
       " :assembly/grammar 0\n"
       " :root {:prim :stack :props {}\n"
       "        :children [{:prim :text-run :props {:value \"plain\"}}]}}\n"))

(def ^:private malformed-face-source
  ;; Torn EDN — parse error; STILL ingests as source (R1); name falls back to
  ;; the file stem; derived material carries valid? false honestly.
  "{:assembly/name \"malformed-w2d-test\" :root {")

;; ===========================================================================
;; PURE · adapter identity (§17) + T18 verdicts + G19 closed mapping.
;; ===========================================================================

(deftest adapter-identity-pure
  (testing "object-key is deterministic on the NAME (§17), never the path"
    (is (= "asm:boxes" (aa/assembly-object-key "boxes")))
    (let [m1 (aa/assembly-materialization boxes-face-source "/a/boxes-w2d-test.edn" {})
          m2 (aa/assembly-materialization boxes-face-source "/elsewhere/moved.edn" {})]
      (is (= "asm:boxes-w2d-test" (:object-key m1) (:object-key m2))
          "envelope name wins; the file may move")
      (is (= :envelope (:name-source m1)))))
  (testing "name fallback: no usable envelope name → file stem (deterministic identity for R1 sources)"
    (let [m (aa/assembly-materialization malformed-face-source "/x/malformed-w2d-test.edn" {})]
      (is (= "asm:malformed-w2d-test" (:object-key m)))
      (is (= :file-stem (:name-source m))))
    (let [m (aa/assembly-materialization "{:assembly/name \"has:colon\" :root {:prim :stack}}"
                                         "/x/colonful.edn" {})]
      (is (= "asm:colonful" (:object-key m))
          "a colon-carrying name is unusable (breaks imp:asm: two-segment routing) → stem")))
  (testing "import key: imp:asm:<object-key>:<sha> — the markdown-import-key shape verbatim (§17)"
    (let [m (aa/assembly-materialization boxes-face-source "/a/boxes-w2d-test.edn" {})]
      (is (re-matches #"imp:asm:asm:boxes-w2d-test:[0-9a-f]{64}" (:import-key m)))
      (is (str/starts-with? (:import-key m) (str "imp:asm:" (:object-key m) ":")))))
  (testing "revision-id deterministic on CONTENT: identical bytes → same revision; edited → new, same object"
    (let [m1 (aa/assembly-materialization boxes-face-source "/a/b.edn" {})
          m2 (aa/assembly-materialization boxes-face-source "/a/b.edn" {})
          m3 (aa/assembly-materialization (str boxes-face-source "\n;; edited") "/a/b.edn" {})]
      (is (= (:revision-id (:revision-row m1)) (:revision-id (:revision-row m2))))
      (is (not= (:revision-id (:revision-row m1)) (:revision-id (:revision-row m3))))
      (is (= (:object-key m1) (:object-key m3)) "an edit is a new revision on the SAME object-key")
      (is (not= (:import-key m1) (:import-key m3)) "an edit is a NEW import")))
  (testing "T18: the adapter's verdict IS the client compiler's verdict (one compiler, two call sites)"
    (let [v (aa/validate-assembly-source boxes-face-source)
          compiled (face-assembly/compile-assembly face-primitives/registry
                                                   (edn/read-string boxes-face-source))]
      (is (true? (:valid? v)))
      (is (not (face-assembly/error? compiled))))
    (let [guarded "{:assembly/name \"g\" :assembly/grammar 0 :root {:prim :stack :props {:x (list 1)}}}"
          v (aa/validate-assembly-source guarded)
          compiled (face-assembly/compile-assembly face-primitives/registry
                                                   (edn/read-string guarded))]
      (is (false? (:valid? v)) "the §4 guard bites at ingest too")
      (is (face-assembly/error? compiled))
      (is (= (vec (face-assembly/compile-errors compiled)) (:errors v))
          "verdicts equal by construction — same errors, same order"))
    (let [v (aa/validate-assembly-source malformed-face-source)]
      (is (false? (:valid? v)))
      (is (= :assembly/parse-error (:type (first (:errors v))))))))

(deftest edge-specs-pure
  (let [m (aa/assembly-materialization boxes-face-source "/a/boxes-w2d-test.edn" {})
        identity {:object-key (:object-key m)
                  :document-id (:document-id m)
                  :import-key (:import-key m)
                  :created-at (:created-at m)
                  :provenance (:provenance m)}
        specs (aa/edge-specs identity)
        by-kind (into {} (map (juxt :kind clojure.core/identity)) specs)]
    (testing "G19: the closed field→kind map — EXISTING D-004 kinds only, impossible to name a kind"
      (is (= #{:produced :based-on :supersedes} (set (keys by-kind))))
      (doseq [spec specs]
        (is (contains? rk/relation-kinds (:kind spec))
            "every derivable kind is registered — an unregistered kind cannot be constructed"))
      ;; junk provenance-ish envelope fields derive NOTHING (no passthrough):
      (let [junk (aa/edge-specs (assoc identity :provenance
                                       {:relation-kind :pairs-with
                                        :kind :connected-with
                                        :frobnicate "x"}))]
        (is (empty? junk))))
    (testing "directions (§8/§17): produced = conversation→face; based-on/supersedes = face→target"
      (is (= "chat:w2d-test-conversation" (get-in by-kind [:produced :from :target-id])))
      (is (= (:document-id m) (get-in by-kind [:produced :to :target-id])))
      (is (= (:document-id m) (get-in by-kind [:based-on :from :target-id])))
      (is (= "oc:doc:asm:outline-face" (get-in by-kind [:based-on :to :target-id]))
          "a bare face NAME normalizes to that face's document container")
      (is (= "oc:doc:asm:old-face" (get-in by-kind [:supersedes :to :target-id]))))
    (testing "asserted-by (§17): envelope author when present, else the system watcher actor"
      (doseq [spec specs]
        (is (= "sid" (:asserter-actor-id spec)))
        (is (= :human (:asserter-type spec))))
      (let [no-author (aa/edge-specs (assoc identity :provenance {:based-on "outline-face"}))]
        (is (= aa/system-asserter-actor-id (:asserter-actor-id (first no-author))))
        (is (= :import (:asserter-type (first no-author))))))
    (testing "idempotency keys are stable across re-derivation (git_spine.clj:224 discipline)"
      (is (= (mapv :idempotency-key specs)
             (mapv :idempotency-key (aa/edge-specs identity)))))
    (testing "no provenance → no edges (plain + malformed files assert nothing)"
      (is (empty? (aa/edge-specs (assoc identity :provenance nil))))
      (is (empty? (aa/edge-specs (assoc identity :provenance {})))))))

;; ===========================================================================
;; PURE · G18 extract-object-key routing unit (the twice-fired latent class).
;; ===========================================================================

(deftest g18-extract-object-key-unit
  (testing "imp:asm: keys route to the object-key partition"
    (let [ik "imp:asm:asm:boxes-w2d-test:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"]
      (is (= "asm:boxes-w2d-test" (oc/extract-object-key ik))
          "the branch recovers the two-segment asm:<name> object-key")
      (is (= (oc/partition-by-object-key 4 ik)
             (oc/partition-by-object-key 4 "asm:boxes-w2d-test"))
          "a foreign completion read lands on the task the import wrote on")
      (is (not= ik (oc/extract-object-key ik))
          "the pre-branch failure shape (fall-through to :else = whole string) is dead")
      (is (not= "asm" (oc/extract-object-key ik))
          "the leading-object-key truncation failure shape is dead")))
  (testing "existing families untouched (additive branch)"
    (is (= "abc123" (oc/extract-object-key "imp:md:abc123:deadbeef")))
    (is (= "abc123" (oc/extract-object-key "imp:clj:abc123:deadbeef")))
    (is (= "chat:ffff" (oc/extract-object-key "imp:tr:chat:ffff:sb:deadbeef")))
    (is (= "plain" (oc/extract-object-key "plain")))))

;; ===========================================================================
;; PURE · G21 read-only-by-construction (mechanical scan) + projection totality.
;; ===========================================================================

(deftest g21-read-only-scan
  (testing "face_projection.clj: no writes, no topology forms, no raw PState paths"
    (let [src (slurp "src/app/server/rama/face_projection.clj")]
      (doseq [forbidden ["defmodule" "<<sources" "local-transform>" "->transform"
                         "foreign-append" "append-object-container-request"
                         "append-relation-request" "declare-depot" "declare-pstate"
                         "stream-topology" "microbatch-topology" "query-topology"
                         "foreign-select" "foreign-pstate" "keypath" "local-select"]]
        (is (not (str/includes? src forbidden))
            (str "must not contain write/topology/raw-path form: " forbidden)))
      ;; The read surface is EXACTLY the named APIs (G21): OC query APIs +
      ;; the arsenal's own named read fns.
      (is (= #{"ocr/read-source" "ocr/read-current-revision"}
             (set (re-seq #"ocr/[a-z\-]+" src)))
          "OC reads: river-page's :until-ms filter read (W1, G12-amended) + the wear-time revision read")
      (is (= #{"face-arsenal/read-face" "face-arsenal/list-faces"
               "face-arsenal/read-wear-count"}
             (set (re-seq #"face-arsenal/[a-z\-]+" src)))
          "arsenal reads go through the arsenal's OWN named read fns — no PState paths here"))))

(deftest g21-projection-totality-pure
  (testing ":assembly is total — nil runtimes, bad names, absent faces → error data-contexts, never a throw"
    (let [dc (fp/assembly-projection {:oc-rt nil :arsenal-rt nil} {:face :assembly :address nil})]
      (is (map? dc))
      (is (false? (:assembly/valid? dc))))
    (let [dc (fp/assembly-projection {:oc-rt nil :arsenal-rt nil} {:face :assembly :address "no:colons allowed"})]
      (is (= :assembly/bad-name (:type (first (:assembly/errors dc))))))
    (let [dc (fp/assembly-projection {:oc-rt nil :arsenal-rt nil} {:face :assembly :address "ghost"})]
      (is (false? (:assembly/found? dc)))
      (is (= :assembly/not-found (:type (first (:assembly/errors dc)))))))
  (testing ":face-list is total — missing/corrupt arsenal → honest error, empty list, never a throw"
    (let [dc (fp/face-list-projection {:arsenal-rt nil} {})]
      (is (= [] (:faces dc)))
      (is (= :arsenal-unavailable (:face-list/error dc))))
    (let [dc (fp/face-list-projection {:arsenal-rt {:faces-by-name :not-a-pstate}} {})]
      (is (= [] (:faces dc)))
      (is (= :arsenal-read-failed (:face-list/error dc)))))
  (testing "serve dispatches both registry entries and stays total (unit, request→data-context)"
    (let [dc (fp/serve {:oc-rt nil :arsenal-rt nil} {:face :face-list})]
      (is (map? dc))
      (is (contains? dc :faces)))
    (let [dc (fp/serve {:oc-rt nil :arsenal-rt nil} {:face :assembly :address "ghost"})]
      (is (map? dc))
      (is (false? (:assembly/found? dc))))))

;; ===========================================================================
;; IPC · gates G17–G21 durable halves (ONE shared OC+arsenal cluster; rk on
;; its own; a second arsenal-only cluster for the G20 boot replay).
;; ===========================================================================

(defn- scratch-dir! [prefix]
  (str (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn- edn-files-under [root]
  (vec (for [^File f (file-seq (io/file (str root)))
             :when (and (.isFile f) (str/ends-with? (.getName f) ".edn"))]
         f)))

(defn- revision-history-count
  "Physical receipt reader: revision rows for a container. `oc:doc:asm:<name>`
   routes correctly through the declared key-partitioner (full-remainder
   oc:doc: branch) — no :pkey needed."
  [oc-rt doc-id]
  (count (foreign-select [(keypath doc-id) ALL] (:revision-history-by-container oc-rt))))

(defn- read-latest-version-row
  "Physical receipt reader: $$source-latest-by-ref is written on
   |hash(source-ref-key) (a colonless sha) and declared with the default
   key-partitioner — a plain keyed read routes correctly."
  [oc-rt source-ref-key]
  (foreign-select-one (keypath source-ref-key) (:source-latest-by-ref oc-rt)))

(defn- read-derived-unit
  "Physical receipt reader for the verdict row. `du:asm:<name>:…` MISROUTES
   through the declared key-partitioner (leading-object-key truncates at
   \"asm\" — the named lane lack), so the receipt routes EXPLICITLY to the
   task the import wrote on: {:pkey object-key} (test-only, exempt per the
   G12 carve-out precedent)."
  [oc-rt object-key unit-id]
  (foreign-select-one (keypath unit-id) (:derived-units-by-id oc-rt)
                      {:pkey object-key}))

(defn- read-container-row
  [oc-rt doc-id]
  (foreign-select-one (keypath doc-id) (:containers-by-id oc-rt)))

(deftest ^:ipc face-arsenal-w2-gates
  (let [scratch-faces (scratch-dir! "w2d-faces")
        scratch-data (scratch-dir! "w2d-data")
        wear-log (str scratch-data "/face-wear-log.ednl")
        boxes-file (io/file scratch-faces "boxes-w2d-test.edn")
        plain-file (io/file scratch-faces "plain-w2d-test.edn")
        malformed-file (io/file scratch-faces "malformed-w2d-test.edn")
        _ (spit boxes-file boxes-face-source)
        _ (spit plain-file plain-face-source)
        _ (spit malformed-file malformed-face-source)
        oc-rt (ocr/start-object-container-runtime!)
        arsenal (fa/start-face-arsenal-runtime! {:ipc (:ipc oc-rt)
                                                 :wear-log-path wear-log})
        rk-rt (rk/start-relation-runtime!)
        ;; the watcher's merged runtime: OC seam + the arsenal handles + ONLY
        ;; the rk keys the edge path needs (avoids the :events-by-id /
        ;; :decisions-by-idempotency key collisions between the two runtimes).
        runtime (merge oc-rt
                       arsenal
                       (select-keys rk-rt [:relation-request-depot
                                           :relation-detail-query]))]
    (try
      ;; =====================================================================
      ;; G17 · watcher round-trip + receipt (initial-sweep over the REAL faces
      ;; dir + the synthetic scratch root; ASSERTED against durable state).
      ;; =====================================================================
      (testing "G17: initial sweep — every .edn ingests, materializes, indexes; epoch honest"
        (let [all-files (into (edn-files-under real-faces-dir)
                              (edn-files-under scratch-faces))
              epoch-before @util-fns/!ingest-epoch-atom
              sweep (watchers/initial-sweep! {:runtime runtime
                                              :roots [real-faces-dir scratch-faces]
                                              :classify-fn watchers/faces-classify})
              epoch-after @util-fns/!ingest-epoch-atom]
          (is (pos? (count all-files)) "the real faces dir holds at least the worn outline")
          (is (= (count all-files) (:attempted sweep)) "faces-classify picks every .edn, nothing else")
          (is (= (:attempted sweep) (:imported sweep))
              "EVERY file accepted — malformed included (R1: raw surface always lands)")
          (is (= (:imported sweep) (- epoch-after epoch-before))
              "the ingest epoch bumps by exactly one per accepted import")
          (doseq [^File f all-files]
            (let [raw (slurp f)
                  m (aa/assembly-materialization raw (.getPath f) {})
                  ok (:object-key m)
                  expected-valid (:valid? (:verdict m))
                  completion (ocr/read-import-completion oc-rt (:import-key m))
                  latest (read-latest-version-row oc-rt (:source-ref-key m))
                  unit (read-derived-unit oc-rt ok (:unit-id (:unit-row m)))
                  container (read-container-row oc-rt (:document-id m))
                  row (fa/read-face arsenal (:assembly-name m))]
              (testing (str "receipt for " (.getName f))
                ;; exactly one accepted import, family :assembly — the durable
                ;; completion (G18's foreign read, exercised per REAL file) +
                ;; the container kind:
                (is (some? completion) "import completion durably present (foreign imp:asm: read)")
                (is (= ok (:object-key completion)))
                (is (= :assembly (:container-kind container)) "family :assembly materialized")
                ;; one latest source version:
                (is (some? latest))
                (is (= (:source-hash m) (:source-hash latest)) "latest version = the file's bytes")
                ;; one derived assembly material row, valid? honest per file:
                (is (some? unit) "derived verdict row materialized")
                (is (= (if expected-valid :assembly/valid :assembly/invalid) (:unit-kind unit)))
                (let [verdict (edn/read-string (:derived-content-text unit))]
                  (is (= expected-valid (:assembly/valid? verdict)))
                  (when-not expected-valid
                    (is (seq (:assembly/errors verdict)) "an invalid file carries its errors")))
                ;; one $$faces-by-name entry (the pointer index, T14/T16):
                (is (some? row) "face indexed in the arsenal")
                (is (= ok (:object-key row)))
                (is (= (:import-key m) (:import-key row)))
                (is (= expected-valid (:valid? row)) "the index never lies about wearability"))))))

      (testing "G17: identical re-save replays convergently — no new revision, no new version"
        (let [m (aa/assembly-materialization boxes-face-source (.getPath boxes-file) {})
              doc-id (:document-id m)
              hist-before (revision-history-count oc-rt doc-id)
              versions-before (count (foreign-select [(keypath (:source-ref-key m)) ALL]
                                                     (:source-versions-by-ref oc-rt)))
              epoch-before @util-fns/!ingest-epoch-atom
              _ (spit boxes-file boxes-face-source) ;; byte-identical rewrite
              sweep (watchers/initial-sweep! {:runtime runtime
                                              :roots [scratch-faces]
                                              :classify-fn watchers/faces-classify})
              epoch-after @util-fns/!ingest-epoch-atom]
          (is (= 3 (:imported sweep)) "re-sweep of the scratch root replays all three")
          (is (= 3 (- epoch-after epoch-before)))
          (is (= hist-before (revision-history-count oc-rt doc-id)) "no new revision")
          (is (= versions-before
                 (count (foreign-select [(keypath (:source-ref-key m)) ALL]
                                        (:source-versions-by-ref oc-rt))))
              "no new source version")
          (let [row (fa/read-face arsenal "boxes-w2d-test")]
            (is (= (:import-key m) (:import-key row)) "pointer converged on the same import"))))

      (testing "G17: an EDITED save = exactly one new revision on the SAME object-key"
        (let [m0 (aa/assembly-materialization boxes-face-source (.getPath boxes-file) {})
              doc-id (:document-id m0)
              hist-before (revision-history-count oc-rt doc-id)
              edited (str boxes-face-source "\n;; w2d edited")
              m1 (aa/assembly-materialization edited (.getPath boxes-file) {})
              _ (spit boxes-file edited)
              sweep (watchers/initial-sweep! {:runtime runtime
                                              :roots [scratch-faces]
                                              :classify-fn watchers/faces-classify})]
          (is (= 3 (:imported sweep)))
          (is (= (:object-key m0) (:object-key m1)) "same face, same object")
          (is (= (inc hist-before) (revision-history-count oc-rt doc-id))
              "exactly one new revision")
          (let [latest (read-latest-version-row oc-rt (:source-ref-key m1))]
            (is (= (:source-hash m1) (:source-hash latest)) "latest = the edited bytes"))
          (let [row (fa/read-face arsenal "boxes-w2d-test")]
            (is (= (:import-key m1) (:import-key row))
                "pointer refreshed to the new import (T16 ruling)"))))

      (testing "G17: live-save through the WATCHER path (classify-fn plumbing, deterministic on-import latch)"
        (let [latch (CountDownLatch. 1)
              seen (atom nil)
              handle (watchers/start-ingest-watchers!
                      {:runtime runtime
                       :roots [scratch-faces]
                       :classify-fn watchers/faces-classify
                       :on-import (fn [ev]
                                    (when (= "live-w2d-test.edn" (.getName ^File (:file ev)))
                                      (reset! seen ev)
                                      (.countDown latch)))})]
          (try
            (spit (io/file scratch-faces "live-w2d-test.edn")
                  "{:assembly/name \"live-w2d-test\" :assembly/grammar 0 :root {:prim :stack :props {}}}")
            (is (.await latch 15 TimeUnit/SECONDS) "the live save imports (no polling — the latch is the signal)")
            (is (= :accepted (:status @seen)))
            (is (= :assembly (:kind @seen)))
            (is (some? (fa/read-face arsenal "live-w2d-test")) "live face indexed")
            (finally ((:stop! handle))))))

      ;; =====================================================================
      ;; G18 · durable half: the FOREIGN read-import-completion on an imp:asm:
      ;; key returns the row (per-file, already asserted inside the G17 loop);
      ;; here the multi-task alignment is asserted once more explicitly.
      ;; =====================================================================
      (testing "G18: foreign completion read on a 4-task cluster (the G-F2/F2 class, killed at birth)"
        (let [m (aa/assembly-materialization plain-face-source (.getPath plain-file) {})
              completion (ocr/read-import-completion oc-rt (:import-key m))]
          (is (some? completion))
          (is (= (:object-key m) (:object-key completion)))))

      ;; =====================================================================
      ;; G19 · lineage + provenance (durable half; the edited re-import above
      ;; already re-ran assert-envelope-edges! twice → idempotency is live).
      ;; =====================================================================
      (testing "G19: envelope provenance → exactly one edge per field, correct kind/asserter; re-imports duplicate nothing"
        (let [m (aa/assembly-materialization (slurp boxes-file) (.getPath boxes-file) {})
          specs (aa/edge-specs {:object-key (:object-key m)
                                    :document-id (:document-id m)
                                    :import-key (:import-key m)
                                    :created-at (:created-at m)
                                    :provenance (:provenance m)})]
          (is (= 3 (count specs)))
          (doseq [{:keys [kind from to asserter-actor-id asserter-type]} specs]
            (let [rid (rk/relation-id-for kind from to asserter-actor-id)
                  detail (rk/await-relation #(rk/read-relation-detail rk-rt rid)
                                            #(some? (:row %))
                                            10000)]
              (testing (str "edge " kind)
                (is (some? (:row detail)) "edge durably asserted")
                (is (= :asserted (get-in detail [:row :relation-status])))
                (is (= kind (get-in detail [:row :relation-kind])))
                (is (= "sid" (get-in detail [:row :asserter-actor-id]))
                    "asserted-by = the envelope author (§17)")
                (is (= :human (get-in detail [:row :asserter-type])))
                ;; idempotency: THREE sweeps have run (initial + identical +
                ;; edited — the edited one under a NEW import-key); the edge
                ;; history must hold exactly ONE transition.
                (is (= 1 (count (:history detail)))
                    "re-imports (same AND new import-key) duplicate nothing"))))
          ;; the no-provenance and malformed faces asserted NOTHING — checked
          ;; pure (edge-specs → []); the durable negative rides the pure proof
          ;; plus the closed-mapping unit (an unconstructible kind cannot
          ;; reach the depot: append-relation-request! is only ever called on
          ;; specs from the closed map).
          ))

      ;; =====================================================================
      ;; G20 · wearing log + index honesty.
      ;; =====================================================================
      (testing "G20: wear append — server-stamped, ordered, counted, WAL'd"
        (let [before-ms (System/currentTimeMillis)
              w1 (fa/record-wear! runtime {:wear-id "w2d-wear-1"
                                           :face-name "boxes-w2d-test"
                                           :wearer "sid"
                                           :address "chat:w2d-test-conversation"})]
          (is (<= before-ms (:wear/worn-at-ms w1) (System/currentTimeMillis))
              "worn-at-ms is the honest SERVER stamp")
          (let [cnt (fa/read-wear-count arsenal "boxes-w2d-test")]
            (is (= 1 (:wear-count cnt)))
            (is (= (:wear/worn-at-ms w1) (:last-worn-ms cnt))))
          (let [lines (str/split-lines (slurp wear-log :encoding "UTF-8"))]
            (is (= 1 (count lines)) "one WAL line per accepted wear")
            (is (= w1 (edn/read-string (first lines))) "the line IS the event, pure edn")))
        (fa/record-wear! runtime {:wear-id "w2d-wear-2"
                                  :face-name "boxes-w2d-test"
                                  :wearer "sid"
                                  :address "chat:w2d-test-conversation"})
        (let [evs (fa/read-wear-events arsenal "boxes-w2d-test")]
          (is (= ["w2d-wear-1" "w2d-wear-2"] (mapv :wear-id evs)) "append-only, ordered")
          (is (apply <= (mapv :worn-at-ms evs)))))

      (testing "G20: duplicate wear-id = journaled no-op (a client outbox re-fire re-stamps the clock — the journal keys on wear-id alone)"
        (let [journaled (fa/read-wear-journal-entry arsenal "boxes-w2d-test" "w2d-wear-1")]
          (is (some? journaled) "the journal row is the receipt")
          (fa/record-wear! runtime {:wear-id "w2d-wear-1"
                                    :face-name "boxes-w2d-test"
                                    :wearer "sid"
                                    :address "chat:w2d-test-conversation"})
          (is (= 2 (:wear-count (fa/read-wear-count arsenal "boxes-w2d-test")))
              "the count does not inflate")
          (is (= 2 (count (fa/read-wear-events arsenal "boxes-w2d-test")))
              "the log does not inflate")
          (is (= journaled (fa/read-wear-journal-entry arsenal "boxes-w2d-test" "w2d-wear-1"))
              "the original order-key stands — the duplicate wrote nothing")))

      (testing "G20: the pointer row holds NO assembly material (T16, asserted on the row shape)"
        (let [row (fa/read-face arsenal "boxes-w2d-test")]
          (is (= #{:face-name :object-key :import-key :status :valid? :source-ref
                   :registered-at-ms}
                 (set (keys (into {} row))))
              "pointers + status + provenance — no source, no form, no errors payload")))

      (testing "G20: boot replay of a copied WAL reproduces counts + stamps EXACTLY (fresh cluster)"
        (let [orig-evs (fa/read-wear-events arsenal "boxes-w2d-test")
              orig-cnt (fa/read-wear-count arsenal "boxes-w2d-test")
              arsenal2 (fa/start-face-arsenal-runtime! {:wear-log-path wear-log})]
          (try
            (let [stats (fa/replay-wear-log! arsenal2)]
              ;; 3 WAL lines: two accepted wears + the duplicate attempt (the
              ;; WAL is the write-AHEAD intent record; the journal is truth).
              (is (= 3 (:replayed stats)))
              (is (= 0 (:failed stats))))
            (let [cnt (fa/read-wear-count arsenal2 "boxes-w2d-test")
                  evs (fa/read-wear-events arsenal2 "boxes-w2d-test")]
              (is (= (:wear-count orig-cnt) (:wear-count cnt)) "counts reproduce exactly")
              (is (= (:last-worn-ms orig-cnt) (:last-worn-ms cnt)) "stamps travel verbatim")
              (is (= (mapv (juxt :wear-id :worn-at-ms) orig-evs)
                     (mapv (juxt :wear-id :worn-at-ms) evs))
                  "the event log reproduces exactly"))
            ;; double replay adds nothing (wear-id journal):
            (fa/replay-wear-log! arsenal2)
            (is (= (:wear-count orig-cnt)
                   (:wear-count (fa/read-wear-count arsenal2 "boxes-w2d-test"))))
            ;; torn/malformed WAL lines: per-line isolation, honest count:
            (let [wal2 (str scratch-data "/torn.ednl")]
              (spit wal2 (str "{:event/type :face/wear :face/name \"boxes-w2d-test\" "
                              ":wear/id \"w2d-wear-3\" :wear/worn-at-ms 1234}\n"
                              "{:torn"))
              (let [stats (fa/replay-wear-log! (assoc arsenal2 :face-wear-log-path wal2))]
                (is (= 1 (:replayed stats)))
                (is (= 1 (:failed stats)) "a torn line is counted and skipped, never aborts the rest"))
              (is (= (inc (:wear-count orig-cnt))
                     (:wear-count (fa/read-wear-count arsenal2 "boxes-w2d-test")))))

            ;; G20 honesty tail: this fresh arsenal has NO registered faces —
            ;; a face present in OC but missing from the index:
            (testing "G20: index-missing face is still servable; the lack is NAMED in the data, never invented"
              (let [dc (fp/assembly-projection {:oc-rt oc-rt :arsenal-rt arsenal2}
                                               {:face :assembly :address "boxes-w2d-test"})]
                (is (true? (:assembly/found? dc))
                    "name→key is deterministic (§17) — the face serves without the index")
                (is (true? (:assembly/valid? dc)))
                (is (false? (:assembly/indexed? dc)) "T17's gap window, honest in the data"))
              (let [fl (fp/face-list-projection {:arsenal-rt arsenal2} {})]
                (is (= [] (:faces fl)) ":face-list signals the lack — empty, never fabricated")
                (is (nil? (:face-list/error fl)) "an empty roster is not an error")))
            (finally (fa/close-face-arsenal-runtime! arsenal2)))))

      ;; =====================================================================
      ;; G21 · the projections over the REAL cluster (request → data-context).
      ;; =====================================================================
      (testing "G21: :assembly wear-time source serve over durable state (through serve — the artery's entrypoint)"
        (let [ctx {:oc-rt oc-rt :arsenal-rt arsenal}
              dc (fp/serve ctx {:face :assembly :address "boxes-w2d-test" :params {}})]
          (is (true? (:assembly/found? dc)))
          (is (true? (:assembly/valid? dc)))
          (is (true? (:assembly/indexed? dc)))
          (is (= :candidate (:assembly/status dc)))
          (is (str/includes? (:assembly/source dc) ":assembly/name")
              "the served source is the durable revision's bytes")
          (is (pos? (long (:face/rendered-at-ms dc)))))
        (let [dc (fp/serve {:oc-rt oc-rt :arsenal-rt arsenal}
                           {:face :assembly :address "malformed-w2d-test" :params {}})]
          (is (true? (:assembly/found? dc)) "a malformed face still serves (R1: the source landed)")
          (is (false? (:assembly/valid? dc)) "…and error-cards honestly (T18: same compiler verdict)")
          (is (seq (:assembly/errors dc)))))

      (testing "G21: :face-list over durable state — names + status + wear counts + last-worn, from Rama (T14)"
        (let [dc (fp/serve {:oc-rt oc-rt :arsenal-rt arsenal} {:face :face-list :params {}})
              by-name (into {} (map (juxt :name clojure.core/identity)) (:faces dc))]
          (is (contains? by-name "boxes-w2d-test"))
          (is (contains? by-name "outline-face") "the REAL dir's worn face lists too")
          (is (contains? by-name "malformed-w2d-test") "a failed compile LISTS — with valid? false, the map does not lie")
          (is (false? (:valid? (get by-name "malformed-w2d-test"))))
          (is (= 2 (:wear-count (get by-name "boxes-w2d-test"))))
          (is (pos? (long (:last-worn-ms (get by-name "boxes-w2d-test")))))
          (is (= 0 (:wear-count (get by-name "plain-w2d-test"))) "never-worn is an honest zero")))

      (finally
        (fa/close-face-arsenal-runtime! arsenal) ;; no-op: attached to the OC IPC
        (rk/close-relation-runtime! rk-rt)
        (ocr/close-object-container-runtime! oc-rt)))))
