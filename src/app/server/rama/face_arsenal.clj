(ns app.server.rama.face-arsenal
  "Faces-as-assemblies Wave 2 · the face-arsenal micro-kernel (framework
   CONTRACT §16; lane W2-D). INTENT-ONLY kernel per the KERNEL-SHAPE taxonomy
   (`kernel.clj`; text/space are the precedents): ONE depot (wear events +
   face-registered events), one stream topology, pointer/usage PStates.

   THE PLACEMENT RULING (§16): OC has no by-family enumeration and reading the
   faces DIRECTORY for the face list would violate the back-arrow (trap T14:
   the map lies — a failed import would list as wearable). The index + wearing
   log live here. §8 carve-out, enforced by gate G20: this kernel holds USAGE
   EVENTS and POINTERS only — never assembly material (trap T16); assemblies,
   their revisions, their lineage live ONLY in OC + the relation kernel.

   Durability (§16 ruling): the dev runtime is in-memory IPC. Faces re-enter
   via the watcher's initial-sweep!; wear events get the /assert WAL treatment
   (`git_spine.clj:47-55,:582-655` precedent): every accepted wear appends one
   pure-edn line to `data/face-wear-log.ednl` BEFORE the depot append, and
   boot replays the log idempotently (wear-id journal). The wearing log is the
   desire-path instrument — losing it at reboot would defeat it.

   Event-boundary map (rama-pitfalls §1): a wear event's writes (journal +
   events + counts) all execute between the same partitioners on
   hash(:face/name) — atomic together. A registered event hops to the roster
   task and does its single write there. The OC-accept → arsenal-register dual
   append is NOT atomic (trap T17): the register is idempotent by import-key
   (row overwrite converges) and every accepted decision — replays included —
   re-fires it, so the gap window is honest degradation that converges on the
   next change event / boot sweep.

   Write path (§16): `record-wear!` is called by the codebase's FIRST write
   e/defn at W2-INT; the CLIENT mints the wear-id at the outbox (ids before
   append — the Rama event-boundary law); THIS fn stamps `worn-at-ms` with the
   honest server clock, never the client's. It must NOT ride `FacePull`/serve
   (trap T15: the read artery stays read-only; an epoch re-pull is not a wear)."
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [app.server.rama.object-container :as oc]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [com.rpl.rama.test :refer [create-ipc launch-module!]])
  (:import [java.io File]))

;; ===========================================================================
;; Rows (POINTERS + usage events — no material field, ever; T16/G20).
;; ===========================================================================

(defrecord FaceRegistryRow
  ;; The pointer index row (§16): name → object-key + status + import-key
  ;; provenance + honest validity flag. NO source text, NO assembly form, NO
  ;; errors payload (material lives in OC; G20 asserts this row shape).
  [face-name object-key import-key status valid? source-ref registered-at-ms])

(defrecord WearEventRow
  [wear-id face-name wearer address worn-at-ms order-key])

(defrecord WearCountRow
  [face-name wear-count last-worn-ms last-wear-id])

;; ===========================================================================
;; Depot events — PLAIN MAPS (the relation-kernel F1 lesson: a defrecord with
;; a namespaced hash-by extractor funnels to one task; maps route correctly).
;; ===========================================================================

(defn wear-event
  [{:keys [wear-id face-name wearer address worn-at-ms]}]
  {:event/type :face/wear
   :face/name face-name
   :wear/id wear-id
   :wear/wearer wearer
   :wear/address address
   :wear/worn-at-ms worn-at-ms})

(defn registered-event
  [{:keys [face-name object-key import-key status valid? source-ref registered-at-ms]}]
  {:event/type :face/registered
   :face/name face-name
   :face/object-key object-key
   :face/import-key import-key
   :face/status status
   :face/valid? valid?
   :face/source-ref source-ref
   :face/registered-at-ms registered-at-ms})

(defn unregistered-event
  "G26 fix (rename ghost): a face whose name no longer backs a source file is
   REMOVED from the roster — a permanent ghost entry is the map lying about
   wearability (T14 spirit). Emitted by the watcher's roster reconcile when an
   accepted import shows a file's envelope name changed; the OC object itself
   is untouched (durable history; the rename stays a fork per §17)."
  [{:keys [face-name]}]
  {:event/type :face/unregistered
   :face/name face-name})

;; -- topology helper fns (plain defns used as dataflow ops) -------------------

(defn event-type [e] (:event/type e))
(defn event-face-name [e] (:face/name e))
(defn event-wear-id [e] (:wear/id e))

(defn wear-order-key
  "Deterministic from the DEPOT RECORD (retry idempotence, rama-pitfalls §5):
   fixed-width(worn-at-ms) : wear-id — time-ordered, id-disambiguated."
  [e]
  (oc/fixed-width-order-key (:wear/worn-at-ms e) (:wear/id e)))

(defn wear-row
  [e order-key]
  (->WearEventRow (:wear/id e) (:face/name e) (:wear/wearer e)
                  (:wear/address e) (:wear/worn-at-ms e) order-key))

(defn next-count-row
  "Fold one wear into the count row. `last-worn-ms` takes the max (a WAL
   replay may arrive out of order across faces; within a face depot order =
   append order, but max is the honest fold either way)."
  [existing e]
  (let [worn (long (or (:wear/worn-at-ms e) 0))]
    (if existing
      (->WearCountRow (:face-name existing)
                      (inc (long (:wear-count existing)))
                      (max (long (or (:last-worn-ms existing) 0)) worn)
                      (if (>= worn (long (or (:last-worn-ms existing) 0)))
                        (:wear/id e)
                        (:last-wear-id existing)))
      (->WearCountRow (:face/name e) 1 worn (:wear/id e)))))

(defn registry-row
  [e]
  (->FaceRegistryRow (:face/name e) (:face/object-key e) (:face/import-key e)
                     (:face/status e) (:face/valid? e) (:face/source-ref e)
                     (:face/registered-at-ms e)))

(def roster-key
  "The ONE top-level key of $$faces-by-name. A constant top key + the default
   key-partitioner gives every write and every foreign read the same task
   (hash(\"faces\")) with the PRECEDENTED nested-map navigation shape
   ($$transcript-source-lines-by-file) — no :global? declaration, no
   root-path reads (the Rama 1.6.0 root-path proxy crash class stays far away)."
  "faces")

(defn roster-key-of [_] roster-key)

(defn valid-face-event?
  "Topology ingress guard: never throw, never write garbage. The lawful
   appenders (record-wear!/register-face!) validate client-side; this guard
   drops a malformed depot record honestly (a torn WAL line that slipped
   replay validation, a foreign append). `worn-at-ms` must be NUMERIC, not
   merely present — a non-numeric stamp passing the guard throws inside
   `wear-order-key`'s (long …) cast AFTER acceptance, and under
   :retry-mode :all-after that wedges the face's whole task partition and
   hangs boot replay (G26 falsification finding, 2026-07-11)."
  [e]
  (and (map? e)
       (oc/string-present? (:face/name e))
       (case (:event/type e)
         :face/wear (and (oc/string-present? (:wear/id e))
                         (number? (:wear/worn-at-ms e)))
         :face/registered (oc/string-present? (:face/object-key e))
         :face/unregistered true
         false)))

;; ===========================================================================
;; The module — ONE depot, ONE stream topology, pointer/usage PStates.
;; ===========================================================================

(defmodule face-arsenal-module [setup topologies]
  ;; hash-by :face/name: every wear for a face lands on that face's task, so
  ;; journal + events + counts write in ONE event (atomic; rama-pitfalls §1).
  (declare-depot setup *face-arsenal-depot (hash-by :face/name))
  (let [s (stream-topology topologies "face-arsenal-topology")]
    ;; $$faces-by-name — the POINTER index (T14/T16): one constant top key →
    ;; name → FaceRegistryRow. Subindexed: the roster outlives any size guess.
    (declare-pstate s $$faces-by-name
                    {String (map-schema String FaceRegistryRow {:subindex? true})})
    ;; $$wear-events-by-face — APPEND-ONLY usage log (§8: the desire-path
    ;; instrument), ordered by order-key. Subindexed: wears accumulate.
    (declare-pstate s $$wear-events-by-face
                    {String (map-schema String WearEventRow {:subindex? true})})
    (declare-pstate s $$wear-counts-by-face {String WearCountRow})
    ;; $$wear-journal-by-face — wear-id → order-key. The duplicate-wear-id
    ;; journal (G20): a client outbox re-fire re-stamps worn-at-ms, so dedup
    ;; MUST key on wear-id alone, never the order-key.
    (declare-pstate s $$wear-journal-by-face
                    {String (map-schema String String {:subindex? true})})
    (<<sources s
      (source> *face-arsenal-depot {:retry-mode :all-after} :> *event)
      (<<if (valid-face-event? *event)
        (event-type *event :> *etype)
        (event-face-name *event :> *face-name)
        (<<cond
          (case> (= :face/wear *etype))
          (event-wear-id *event :> *wear-id)
          (local-select> [(keypath *face-name *wear-id)]
                         $$wear-journal-by-face :> *journaled-order-key)
          (<<if (some? *journaled-order-key)
            ;; duplicate wear-id = journaled no-op (G20): nothing appended,
            ;; nothing counted; the journal row IS the receipt.
            (ack-return> *journaled-order-key)
            (else>)
            (wear-order-key *event :> *order-key)
            (wear-row *event *order-key :> *wear-row)
            ;; one event, three writes, same task — atomic together (§1).
            (local-transform> [(keypath *face-name *order-key) (termval *wear-row)]
                              $$wear-events-by-face)
            (local-transform> [(keypath *face-name *wear-id) (termval *order-key)]
                              $$wear-journal-by-face)
            (local-select> [(keypath *face-name)] $$wear-counts-by-face
                           :> *existing-count)
            (next-count-row *existing-count *event :> *count-row)
            (local-transform> [(keypath *face-name) (termval *count-row)]
                              $$wear-counts-by-face)
            (ack-return> *order-key))

          (case> (= :face/registered *etype))
          (registry-row *event :> *registry-row)
          (roster-key-of *event :> *roster-key)
          ;; hop to the roster task; single write there (its own event).
          ;; Idempotent by VALUE overwrite (T17): re-registering the same
          ;; import-key rewrites an identical row; a NEW import-key for the
          ;; same name refreshes the pointer (\"refreshed on every accepted
          ;; import\", T16 ruling).
          (|hash *roster-key)
          (local-transform> [(keypath *roster-key *face-name) (termval *registry-row)]
                            $$faces-by-name)
          (ack-return> *registry-row)

          (case> (= :face/unregistered *etype))
          ;; G26 fix (rename ghost): roster removal — idempotent (removing an
          ;; absent key is a no-op); wear history/counts stay (usage happened;
          ;; the log never unhappens — only the POINTER goes).
          (roster-key-of *event :> *roster-key)
          (|hash *roster-key)
          ;; NONE> (never `(termval NONE)` — NONE is unusable in dataflow code,
          ;; rama ref 29 §pstates; the llm/compute kernels' removal idiom)
          (local-transform> [(keypath *roster-key *face-name) NONE>]
                            $$faces-by-name)
          (ack-return> *face-name))))))

;; ===========================================================================
;; Runtime (mirrors the dogfood kernels' start/stop shape).
;; ===========================================================================

(def wear-log-relative-path
  "The WAL path literal (the git-spine `assert-log-relative-path` §3.E
   precedent)."
  "data/face-wear-log.ednl")

(defn default-wear-log-path
  [{:keys [repo-root]}]
  (str (or repo-root (System/getProperty "user.dir")) "/" wear-log-relative-path))

(defn start-face-arsenal-runtime!
  "Launch the arsenal module. With no args: own IPC. With {:ipc <ipc>}:
   attach to an EXISTING in-process cluster (one JVM, one cluster — the OC
   runtime's IPC is the intended host at W2-INT/test time; :owns-ipc? tells
   close! whose lifecycle it is). {:wear-log-path} pins the WAL location
   (tests use a scratch path)."
  ([] (start-face-arsenal-runtime! {}))
  ([{:keys [ipc launch-opts wear-log-path]}]
   (let [owns-ipc? (nil? ipc)
         ipc (or ipc (create-ipc))
         module-name (get-module-name face-arsenal-module)]
     (launch-module! ipc face-arsenal-module (or launch-opts {:tasks 4 :threads 2}))
     {:face-arsenal-ipc ipc
      :face-arsenal-owns-ipc? owns-ipc?
      :face-arsenal-module-name module-name
      :face-arsenal-depot (foreign-depot ipc module-name "*face-arsenal-depot")
      :faces-by-name (foreign-pstate ipc module-name "$$faces-by-name")
      :wear-events-by-face (foreign-pstate ipc module-name "$$wear-events-by-face")
      :wear-counts-by-face (foreign-pstate ipc module-name "$$wear-counts-by-face")
      :wear-journal-by-face (foreign-pstate ipc module-name "$$wear-journal-by-face")
      :face-wear-log-path (or wear-log-path (default-wear-log-path {}))})))

(defn close-face-arsenal-runtime!
  [runtime]
  (when (and (:face-arsenal-owns-ipc? runtime) (:face-arsenal-ipc runtime))
    (try (.close ^java.lang.AutoCloseable (:face-arsenal-ipc runtime))
         (catch Exception _ nil))))

;; ===========================================================================
;; Write fns — the ONLY lawful appenders (back-arrow: everything streams INTO
;; Rama through these; the UI reads Rama).
;; ===========================================================================

(def ^:private wear-log-lock (Object.))

(defn- append-wear-log-line!
  "One pure-edn line, UTF-8, single-writer-locked (the git-spine route-writer
   discipline). Plain maps only — safe under clojure.edn at replay."
  [path event]
  (locking wear-log-lock
    (let [f (io/file path)]
      (io/make-parents f)
      (with-open [w (io/writer f :append true :encoding "UTF-8")]
        (.write w (pr-str event))
        (.write w "\n")))))

(defn record-wear!
  "Record ONE wear (§16 write-path ruling). The caller (the W2-INT outbox)
   mints the wear-id BEFORE this call; the SERVER stamps `worn-at-ms` here —
   the honest server clock, never the client's. Order: WAL line first
   (write-ahead — an append failure after the WAL line converges at boot
   replay; the reverse order could silently lose an acked wear), then depot
   append with :ack (PState-visible on return — the deterministic barrier).
   Duplicate wear-id = journaled no-op in the topology. Returns the event.
   Stamp semantics (G26/G20 carve-out): the WAL's FIRST line per wear-id is
   the canonical worn-at-ms — a failed-append retry re-stamps live state
   until the next boot replay converges it back to the first attempt's time,
   which IS the wear time (the retry was infra, not a wear)."
  [runtime {:keys [wear-id face-name wearer address]}]
  (when-not (oc/string-present? wear-id)
    (throw (IllegalArgumentException. "record-wear! requires a non-blank :wear-id")))
  (when-not (oc/string-present? face-name)
    (throw (IllegalArgumentException. "record-wear! requires a non-blank :face-name")))
  (let [event (wear-event {:wear-id wear-id
                           :face-name face-name
                           :wearer wearer
                           :address address
                           :worn-at-ms (System/currentTimeMillis)})
        ;; durable-ground P4: an EXPLICITLY-nil :face-wear-log-path means the
        ;; WAL is OFF (cluster runtimes — the depot IS the durable log); only
        ;; an ABSENT key falls back to the legacy default path.
        path (if (contains? runtime :face-wear-log-path)
               (:face-wear-log-path runtime)
               (default-wear-log-path {}))]
    (when path
      (append-wear-log-line! path event))
    (foreign-append! (:face-arsenal-depot runtime) event :ack)
    event))

(defn register-face!
  "Append ONE face-registered event (called by the ingest watcher on every
   ACCEPTED assembly import decision — trap T17: idempotent by import-key, so
   replays converge any accept→register gap). Server stamps registered-at-ms.
   NOT WAL'd: faces re-enter at boot via the watcher's initial-sweep! (§16
   durability ruling — only wears carry the /assert treatment)."
  [runtime {:keys [face-name object-key import-key] :as reg}]
  (when-not (oc/string-present? face-name)
    (throw (IllegalArgumentException. "register-face! requires a non-blank :face-name")))
  (when-not (oc/string-present? object-key)
    (throw (IllegalArgumentException. "register-face! requires a non-blank :object-key")))
  (let [event (registered-event (assoc reg :registered-at-ms (System/currentTimeMillis)))]
    (foreign-append! (:face-arsenal-depot runtime) event :ack)
    event))

(defn unregister-face!
  "Remove ONE roster entry (G26 rename-ghost fix; called by the watcher's
   reconcile when a file's envelope name changed — the old name no longer
   backs a file and must not list as wearable). Idempotent; NOT WAL'd (the
   roster reconstructs from the boot sweep); wear history is untouched."
  [runtime {:keys [face-name]}]
  (when-not (oc/string-present? face-name)
    (throw (IllegalArgumentException. "unregister-face! requires a non-blank :face-name")))
  (let [event (unregistered-event {:face-name face-name})]
    (foreign-append! (:face-arsenal-depot runtime) event :ack)
    event))

;; ===========================================================================
;; Boot replay (§16 durability ruling; the git-spine replay-assert-log!
;; precedent verbatim: per-line isolation, UTF-8 pinned, idempotent).
;; ===========================================================================

(defn replay-wear-log!
  "Re-append every stored wear event to the depot VERBATIM — the stored
   `worn-at-ms` travels untouched (G20: a replay reproduces counts AND stamps
   exactly; re-stamping would forge the desire-path record). Appends to the
   DEPOT ONLY — never back to the WAL (a WAL-writing replay would double the
   log every boot). Within one cluster lifetime double replay adds nothing
   (wear-id journal); a fresh cluster reconstructs state exactly. Malformed/
   torn lines are counted and SKIPPED (never allowed to abort the reduce and
   un-replay everything after them). Missing file → no-op."
  [runtime]
  (let [path (or (:face-wear-log-path runtime) (default-wear-log-path {}))
        f (io/file path)]
    (if-not (.exists ^File f)
      {:replayed 0 :failed 0}
      (with-open [rdr (io/reader f :encoding "UTF-8")]
        (reduce
         (fn [stats [line-idx line]]
           (if (str/blank? line)
             stats
             (try
               (let [event (edn/read-string line)]
                 (if (valid-face-event? event)
                   (do (foreign-append! (:face-arsenal-depot runtime) event :ack)
                       (update stats :replayed inc))
                   (do (binding [*out* *err*]
                         (println "[FACE-ARSENAL] replay: line" line-idx
                                  "invalid, skipping"))
                       (update stats :failed inc))))
               (catch Exception e
                 (binding [*out* *err*]
                   (println "[FACE-ARSENAL] replay: line" line-idx "failed, skipping:"
                            (.getMessage e)))
                 (update stats :failed inc)))))
         {:replayed 0 :failed 0}
         (map-indexed vector (line-seq rdr)))))))

;; ===========================================================================
;; Named read fns — the arsenal's ONLY product read surface (G21 names these;
;; face_projection reads Rama through them, never PState paths of its own).
;; ===========================================================================

(defn read-face
  "The pointer row for a face name, or nil."
  [runtime face-name]
  (foreign-select-one [(keypath roster-key face-name)] (:faces-by-name runtime)))

(defn list-faces
  "All pointer rows (the roster), name-ordered. Bounded: the roster holds one
   row per face (tens), one seek + sequential iteration."
  [runtime]
  (vec (foreign-select [(keypath roster-key) MAP-VALS] (:faces-by-name runtime))))

(defn read-wear-count
  "The WearCountRow for a face, or nil (never worn)."
  [runtime face-name]
  (foreign-select-one [(keypath face-name)] (:wear-counts-by-face runtime)))

(defn read-wear-events
  "The wear events for a face, order-key-ascending (append order). Test/receipt
   grade reads pass a limit; the log is append-only (G20)."
  ([runtime face-name] (read-wear-events runtime face-name 1000))
  ([runtime face-name limit]
   (vec (foreign-select [(keypath face-name)
                         (sorted-map-range-from "" limit)
                         MAP-VALS]
                        (:wear-events-by-face runtime)))))

(defn read-wear-journal-entry
  "Validation-grade read (G20 journal assertion): the order-key journaled for
   a wear-id, or nil."
  [runtime face-name wear-id]
  (foreign-select-one [(keypath face-name wear-id)] (:wear-journal-by-face runtime)))
