(ns app.server.rama.face-arsenal
  "Face-name pointers and wear usage in one Rama stream module.
   Registration/unregistration maps change the roster; wear maps append history
   and update counts, deduplicated by face-name/wear-id. The roster is colocated
   under the constant faces key; wear state is partitioned by face name.
   Face material and revisions belong to object-container, not this registry.
   Foreign wrappers borrow handles; the IPC launcher records whether it owns
   its cluster. record-wear! optionally writes a local EDN log before the depot;
   door/cluster explicitly disables that file path for durable-cluster use."
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
  "Build a plain namespaced wear-event map from caller-supplied identity, address, and time."
  [{:keys [wear-id face-name wearer address worn-at-ms]}]
  {:event/type :face/wear
   :face/name face-name
   :wear/id wear-id
   :wear/wearer wearer
   :wear/address address
   :wear/worn-at-ms worn-at-ms})

(defn registered-event
  "Build a registration map pointing a face name at object/import identity and validity metadata."
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
  "Build an event removing the face-name pointer. The topology retains wear history
   and does not delete the referenced object-container material."
  [{:keys [face-name]}]
  {:event/type :face/unregistered
   :face/name face-name})

;; -- topology helper fns (plain defns used as dataflow ops) -------------------

(defn event-type
  "Return :event/type from e, or nil when absent."
  [e] (:event/type e))
(defn event-face-name
  "Return :face/name from e, or nil when absent."
  [e] (:face/name e))
(defn event-wear-id
  "Return :wear/id from e, or nil when absent."
  [e] (:wear/id e))

(defn wear-order-key
  "Deterministic from the DEPOT RECORD (retry idempotence, rama-pitfalls §5):
   fixed-width(worn-at-ms) : wear-id — time-ordered, id-disambiguated."
  [e]
  (oc/fixed-width-order-key (:wear/worn-at-ms e) (:wear/id e)))

(defn wear-row
  "Convert a wear event and computed order-key into a WearEventRow."
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
  "Convert registration event metadata into a FaceRegistryRow; contains no face source text."
  [e]
  (->FaceRegistryRow (:face/name e) (:face/object-key e) (:face/import-key e)
                     (:face/status e) (:face/valid? e) (:face/source-ref e)
                     (:face/registered-at-ms e)))

(def roster-key
  "Constant top-level roster key. Reads and registration/removal writes route by
   this key, while wear state stays partitioned by face-name."
  "faces")

(defn roster-key-of
  "Return the constant roster key regardless of the event, for roster task routing."
  [_] roster-key)

(defn valid-face-event?
  "Check event map/name and event-specific required fields before stream processing.
   Wear events require a nonblank ID and numeric timestamp; registration requires
   object-key. Invalid records are dropped without a separate rejected-decision row."
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
  "Default optional local EDN wear-log path, relative to the repository root."
  "data/face-wear-log.ednl")

(defn default-wear-log-path
  "Resolve data/face-wear-log.ednl under :repo-root or the JVM working directory."
  [{:keys [repo-root]}]
  (str (or repo-root (System/getProperty "user.dir")) "/" wear-log-relative-path))

(defn start-face-arsenal-runtime!
  "Launch the face module in a new owned IPC, or in the supplied :ipc. Returns
   foreign handles and an ownership flag; close only releases an owned IPC.
   :launch-opts controls task/thread counts and :wear-log-path overrides the local
   log path. Nil in these startup options falls back to the default log path;
   the durable-cluster bundle is constructed separately by door/cluster."
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
  "Close only an IPC owned by this face runtime; borrowed IPCs stay open.
   Close exceptions are swallowed."
  [runtime]
  (when (and (:face-arsenal-owns-ipc? runtime) (:face-arsenal-ipc runtime))
    (try (.close ^java.lang.AutoCloseable (:face-arsenal-ipc runtime))
         (catch Exception _ nil))))

;; ===========================================================================
;; Foreign append helpers. Validation here complements the topology guard.
;; ===========================================================================

(def ^:private wear-log-lock (Object.))

(defn- append-wear-log-line!
  "Append one UTF-8 EDN event line under a process-local lock, creating parent
   directories and closing the writer. Does not fsync or coordinate other processes."
  [path event]
  (locking wear-log-lock
    (let [f (io/file path)]
      (io/make-parents f)
      (with-open [w (io/writer f :append true :encoding "UTF-8")]
        (.write w (pr-str event))
        (.write w "\n")))))

(defn record-wear!
  "Validate wear-id/face-name, stamp server time, optionally write the local EDN
   line, then append with :ack and return the event. The topology journals by
   face-name/wear-id, so a repeated ID does not increment the count again.
   An explicitly nil :face-wear-log-path disables logging; an absent key uses
   the default path. File and depot writes are not one transaction, and the file
   writer does not fsync. In a fresh IPC replay, the first accepted logged event
   for an ID determines its timestamp; replay does not replace an existing journal entry."
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
  "Validate name/object-key, stamp registration time, append with :ack, and return
   the event. This overwrites the roster pointer without a local log; it does not
   inspect material validity or wait for an object-container import itself."
  [runtime {:keys [face-name object-key import-key] :as reg}]
  (when-not (oc/string-present? face-name)
    (throw (IllegalArgumentException. "register-face! requires a non-blank :face-name")))
  (when-not (oc/string-present? object-key)
    (throw (IllegalArgumentException. "register-face! requires a non-blank :object-key")))
  (let [event (registered-event (assoc reg :registered-at-ms (System/currentTimeMillis)))]
    (foreign-append! (:face-arsenal-depot runtime) event :ack)
    event))

(defn unregister-face!
  "Validate face-name, append its roster removal with :ack, and return the event.
   Removing an absent pointer is harmless; usage history remains stored."
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
  "Read the configured or default EDN log and append valid events verbatim with
   :ack, never writing them back to the file. Returns successful append/failed
   line counts; blank lines are ignored, malformed lines and append exceptions
   are skipped, and missing files yield zero counts. The validator also accepts
   registration/unregistration events. An explicitly nil path still falls back
   to the default here, unlike record-wear!; callers must choose whether to replay."
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
;; Named foreign reads used by page/face_projection and lifecycle callers.
;; ===========================================================================

(defn read-face
  "The pointer row for a face name, or nil."
  [runtime face-name]
  (foreign-select-one [(keypath roster-key face-name)] (:faces-by-name runtime)))

(defn list-faces
  "Read all roster pointer rows as a vector in name-key order. There is no page limit."
  [runtime]
  (vec (foreign-select [(keypath roster-key) MAP-VALS] (:faces-by-name runtime))))

(defn read-wear-count
  "The WearCountRow for a face, or nil (never worn)."
  [runtime face-name]
  (foreign-select-one [(keypath face-name)] (:wear-counts-by-face runtime)))

(defn read-wear-events
  "Read the earliest order-key page of wear events for face-name, default limit 1000.
   Ordering is by stamped wear time and wear-id, not necessarily append order."
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
