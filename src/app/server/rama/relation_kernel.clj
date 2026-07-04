;; IMPORTANT: Before modifying this file, re-read
;; docs/current-mental-model/build/relation-kernel/PLAN.md and CONTRACT.md, and
;; check the NOW baton in docs/sessions/next-prompt.md.
;; Adhere to all previously decided design decisions. If the plan needs to
;; change, FAIL the phase — do not silently redesign while implementing.
;;
;; AMENDED under trail-view WP1 (build/trail-view/CONTRACT.md §5.1, PLAN.md §2):
;; the decision/event/edge records carry envelope-actor custody fields, so an
;; agent-authored write on Sid's instruction is distinguishable from Sid's own
;; hand (trap 10). Identity stays asserter-scoped (trap 11). Authorized by that
;; contract's §2 "Exception, ruled here".

(ns app.server.rama.relation-kernel
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.ops :as ops]
            [com.rpl.rama.aggs :as aggs]
            [com.rpl.rama.test :refer [create-ipc launch-module!]]
            [app.server.rama.object-container :as oc]
            [clojure.string :as str])
  (:import (java.security MessageDigest)))

;; ────────────────────────────────────────────────────────────────────────────
;;   RELATION KERNEL  (relation-kernel-module)
;;
;;   Implements decision D-004: the missing noun is a typed, provenance-carrying
;;   RelationEdge — "A based-on B", "chat produced commit", "panel-2 elaborates
;;   panel-1", "branch X dead-end". It is the epistemic edge the wall is made of
;;   and the substrate lacked.
;;
;;   Own module (CONTRACT §2): own depot, ONE microbatch topology, TWO query
;;   topologies. No edits to existing kernels; the only dependency on
;;   object-container is the two plain helper fns `extract-object-key` and
;;   `fixed-width-order-key` (used in client-side id/target-key derivation).
;;
;;   Why microbatch, not stream (CONTRACT §5, trap 1): a relation write lands in
;;   three places on up to three tasks — the authoritative row on hash(rel-id)
;;   and a full-row copy on each endpoint's target task. Microbatch gives
;;   cross-partition exactly-once atomicity per attempt, so the three copies can
;;   never disagree across a batch boundary. Stream commits per hop, so a replay
;;   between hops would partially apply the triple write.
;;
;;   Read surface is ONLY the two query topologies (CONTRACT §7). Consumers never
;;   foreign-select the PStates directly; that seam keeps the module-reversal
;;   cost low (CONTRACT §2).
;; ────────────────────────────────────────────────────────────────────────────

;; ── Kind registry (CONTRACT §3, trap 6) ─────────────────────────────────────
;; Closed code-level set. Requests with an unregistered kind are REJECTED at
;; decision time. Adding a kind is a one-line reviewed code change. This is the
;; guard against LLM-glue kind mush (`:relates-to`, `:connected-with`, ...).
(def relation-kinds
  #{:based-on :produced :built-over :new-direction :dead-end :elaborates :references
    ;; stance kinds (trail-view §5.2, traps 1-2): verdict rows ARE relations, so
    ;; "confirms / refutes / supersedes" ride the registry and inherit identity,
    ;; idempotency, retraction, history, and evidence anchoring for free. Binary
    ;; directed (from = judgment carrier, to = judged thing); :supersedes is belief
    ;; displacement, distinct from :built-over / :new-direction construction lineage.
    :confirms :refutes :supersedes})

(def request-types #{:relation/assert :relation/retract})

(def relation-statuses #{:asserted :retracted})

;; Sentinel journal key for malformed requests missing an idempotency key, so a
;; rejected decision still has a non-nil map key to land under (and its retries
;; replay). Valid requests always carry a present key and never hit this.
(def missing-idempotency-key "\u0000relation/missing-idempotency")

(def ^:private id-part-separator "\u0000")

;; ── Hashing / string helpers (self-contained; no object-container coupling) ──
(defn sha1-hex
  "Deterministic SHA-1 hex digest of a string. Deterministic identity is what
   makes re-imports converge instead of accreting duplicate rows (trap 2)."
  [s]
  (let [digest (.digest (MessageDigest/getInstance "SHA-1") (.getBytes (str s) "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn present-string?
  [x]
  (and (string? x) (not (str/blank? x))))

(defn blank-string?
  [x]
  (or (not (string? x)) (str/blank? x)))

(defn positive-partition
  "Stable partition index for a key. Own copy of the 2-line idiom — the module
   depends on object-container only for extract-object-key / fixed-width-order-key."
  [num-partitions k]
  (if (pos? num-partitions) (mod (hash k) num-partitions) 0))

;; ── Deterministic identity + id/key formats ─────────────────────────────────
;; relation-id = "rel:" + sha1(kind, from.kind, from.id, to.kind, to.id,
;; asserter-actor-id). Identity INCLUDES the asserter (CONTRACT §3, trap 5): Sid
;; asserting `based-on` and an LLM proposing the same `based-on` are two facts.
(defn relation-id-for
  [kind from-ref to-ref asserter-actor-id]
  (str "rel:"
       (sha1-hex (str/join id-part-separator
                           [(name kind)
                            (name (:target-kind from-ref)) (str (:target-id from-ref))
                            (name (:target-kind to-ref)) (str (:target-id to-ref))
                            (str asserter-actor-id)]))))

(defn valid-relation-id?
  [relation-id]
  (and (string? relation-id) (str/starts-with? relation-id "rel:")))

;; Decision-id / event-id embed the relation-id so a key-partitioner can recover
;; it and colocate audit rows on hash(relation-id). relation-id itself contains
;; no "/decision/" or "/event/" marker, so the split is unambiguous.
(def ^:private decision-marker "/decision/")
(def ^:private event-marker "/event/")

(defn decision-id-for [relation-id request-id] (str relation-id decision-marker request-id))
(defn event-id-for   [relation-id order-key]  (str relation-id event-marker order-key))

(defn relation-id-from-decision-id
  [decision-id]
  (let [s (str decision-id) i (str/index-of s decision-marker)]
    (if i (subs s 0 i) s)))

(defn relation-id-from-event-id
  [event-id]
  (let [s (str event-id) i (str/index-of s event-marker)]
    (if i (subs s 0 i) s)))

(defn partition-by-decision-relation
  [num-partitions decision-id]
  (positive-partition num-partitions (relation-id-from-decision-id decision-id)))

(defn partition-by-event-relation
  [num-partitions event-id]
  (positive-partition num-partitions (relation-id-from-event-id event-id)))

(defn safe-journal-key
  "Non-nil map key for the idempotency journal. Valid requests carry a present
   key (→ identity). Malformed ones fall back to a sentinel so their rejected
   decision is still durably journaled and its retries replay."
  [idempotency-key]
  (if (present-string? idempotency-key) idempotency-key missing-idempotency-key))

;; ── Target-facing sort/descriptor keys (CONTRACT §6) ─────────────────────────
;; direction: "o" (from/outgoing copy) | "i" (to/incoming copy).
;; descriptor-key = "<dir>:<kind>"     — enumerated, bounded (2 × registry).
;; sort-key       = "<dir>:<kind>:<fixed-width-order-key(first-ms, rel-id)>"
;; The descriptor-key is a strict "<dir>:<kind>:" prefix of the sort-key, so a
;; range read over that prefix returns exactly one (dir, kind) group. Both keys
;; are built from the SAME helper so they can never drift.
(defn direction-code [direction] (case direction :outgoing "o" :incoming "i"))

(defn target-descriptor-key
  [direction kind]
  (str (direction-code direction) ":" (name kind)))

(defn target-sort-key
  [direction kind first-asserted-at-ms relation-id]
  (str (target-descriptor-key direction kind) ":"
       (oc/fixed-width-order-key first-asserted-at-ms relation-id)))

(defn descriptor-prefix-bounds
  "[lo hi) bounds that select exactly the sort-keys under descriptor-key. `;`
   (0x3B) is the char just above the `:` (0x3A) delimiter, so every
   \"<dk>:<...>\" sorts below \"<dk>;\" and nothing else does."
  [descriptor-key]
  [(str descriptor-key ":") (str descriptor-key ";")])

;; ── Typed rows (PLAN PState Design). object-container style: typed defrecords in
;;    PStates; the depot event stays a plain map envelope (F1). ─────────────────
(defrecord RelationTargetRef
  [target-kind    ; keyword — :container :source :git-commit :doc-file :conversation :none (open)
   target-id      ; string address, e.g. "oc:doc:notes.md", commit sha; nil for :none
   target-key])   ; derived partition key (CONTRACT §4)

(defrecord RelationMutationPayload
  [relation-id relation-kind from to asserter-actor-id asserter-type
   evidence-source-id evidence-anchor-id note asserted-at-ms])

;; NOTE (F1, PLAN Writes): there is deliberately NO `RelationRequestRow`
;; defrecord. The depot record is the CONTRACT §4 map envelope with namespaced
;; keys, exactly as every existing kernel appends a map (object_container.clj:
;; 1686 `(hash-by :partition/key)`, space.clj:1460 `(hash-by :routing/key)`). A
;; defrecord cannot carry a namespaced-keyword field like `:relation/routing-key`,
;; so `(hash-by :relation/routing-key)` on one would read nil and funnel every
;; request to one task. The relation kernel also stores NO raw-request PState
;; (CONTRACT §6 lists none — audit is via decisions + events).

(defrecord RelationDecisionRow
  [decision-id relation-id request-id request-type idempotency-key status reason
   errors event-id decided-at-ms replayed-from-decision-id request-material-hash
   ;; custody (trail-view §5.1, trap 10): the envelope actor that WROTE this
   ;; decision — distinct from the payload asserter, so an agent appending on
   ;; Sid's instruction is auditable. Identity stays asserter-scoped (trap 11).
   envelope-actor-id envelope-actor-type])

(defrecord RelationEventRow
  [event-id relation-id event-type relation-status relation-kind from to
   asserter-actor-id asserter-type evidence-source-id evidence-anchor-id note
   event-time-ms request-id decision-id previous-status
   ;; custody (§5.1): the writer of THIS transition. Events + decisions hold the
   ;; full per-transition custody trail.
   envelope-actor-id envelope-actor-type])

(defrecord RelationEdgeRow
  [relation-id relation-kind from to asserter-actor-id asserter-type
   relation-status evidence-source-id evidence-anchor-id note
   first-asserted-at-ms status-changed-at-ms event-id request-id
   ;; custody (§5.1): the writer of the LATEST transition on this edge (re-assert
   ;; / retract overwrite it). Full trail lives in events/decisions.
   envelope-actor-id envelope-actor-type])

(defrecord RelationStatusLogRow
  [order-key event-id relation-id relation-status changed-at-ms request-id
   decision-id previous-status])

(defrecord RelationTargetDescriptorRow
  [descriptor-key target-key direction relation-kind asserted-count total-count
   updated-at-ms])

;; ── Envelope / payload accessors (namespaced keys read off the wire map) ─────
(defn relreq-routing-key    [request] (:relation/routing-key request))
(defn relreq-id             [request] (:request/id request))
(defn relreq-type           [request] (:request/type request))
(defn relreq-idempotency-key [request] (:idempotency/key request))
(defn relreq-actor          [request] (:actor request))
(defn relreq-payload        [request] (:payload request))

;; ── Validation (CONTRACT §5 step 3; IMPLICIT_SPEC edge cases). No target
;;    existence check — dangling targets are legal (CONTRACT §8, trap 3). ───────
(defn registered-kind? [kind] (contains? relation-kinds kind))

(defn well-formed-target?
  [ref]
  (and (some? ref)
       (keyword? (:target-kind ref))
       (present-string? (:target-id ref))
       (present-string? (:target-key ref))))

(defn unary-target?
  "Unary mark (CONTRACT §3): `to = {:target-kind :none :target-id nil
   :target-key (from's target-key)}`. Inheriting from's key means the reverse
   index lands on the from task — no nil-key global hotspot (trap 8)."
  [ref from-ref]
  (and (some? ref)
       (= :none (:target-kind ref))
       (nil? (:target-id ref))
       (present-string? (:target-key ref))
       (= (:target-key ref) (:target-key from-ref))))

(defn request-shape-errors
  "Errors computable from the request alone — no PState read. Existence and
   retraction-rights (which need the current row) are decided in relation-outcome."
  [request relation-id]
  (let [payload (relreq-payload request)
        actor   (relreq-actor request)
        from    (:from payload)
        to      (:to payload)
        kind    (:relation-kind payload)
        rtype   (relreq-type request)]
    (cond-> []
      (not (present-string? relation-id))
      (conj {:type :relation/routing-key-missing})
      (not (present-string? (relreq-id request)))
      (conj {:type :request/id-missing})
      (not (present-string? (relreq-idempotency-key request)))
      (conj {:type :idempotency/key-missing})
      (not (present-string? (:actor/id actor)))
      (conj {:type :actor/id-missing})
      (not (contains? request-types rtype))
      (conj {:type :request/type-invalid :value rtype})
      (not (registered-kind? kind))
      (conj {:type :relation/kind-unregistered :value kind})
      (not (well-formed-target? from))
      (conj {:type :relation/from-malformed})
      (not (or (well-formed-target? to) (unary-target? to from)))
      (conj {:type :relation/to-malformed}))))

;; ── Transition logic (pure). Returns a full "outcome" describing every write.
;;    The dataflow topology just executes the writes the outcome names. ─────────
(defn count-deltas
  "Descriptor count changes for one accepted transition, applied to BOTH endpoint
   prefixes. total-count never decreases (retraction is not deletion, CONTRACT §9)."
  [current-row new-status]
  (let [old-status (when current-row (:relation-status current-row))]
    (cond
      (nil? current-row)        {:total 1 :asserted (if (= new-status :asserted) 1 0)}
      (= old-status new-status) {:total 0 :asserted 0}                 ; reassert / retract-affirm
      (= new-status :asserted)  {:total 0 :asserted 1}                 ; retracted → asserted
      :else                     {:total 0 :asserted -1})))             ; asserted → retracted

(defn transition-row
  "New id → full row (first-asserted = status-changed = ts). Existing id → keep
   identity + first-asserted-at-ms, flip status, bump status-changed-at-ms +
   event/request. Preserving first-asserted-at-ms keeps the target sort-key
   stable across status changes, so copies overwrite in place (trap 7).
   Custody (§5.1): the edge reflects the LATEST transition's writer, so the
   existing-row branch overwrites envelope-actor-id/-type too."
  [current-row relation-id kind from to asserter-actor-id asserter-type
   new-status ts event-id request-id evidence-source-id evidence-anchor-id note
   envelope-actor-id envelope-actor-type]
  (if current-row
    (assoc current-row
           :relation-status new-status
           :status-changed-at-ms ts
           :event-id event-id
           :request-id request-id
           :envelope-actor-id envelope-actor-id
           :envelope-actor-type envelope-actor-type)
    (->RelationEdgeRow relation-id kind from to asserter-actor-id asserter-type
                       new-status evidence-source-id evidence-anchor-id note
                       ts ts event-id request-id
                       envelope-actor-id envelope-actor-type)))

(defn endpoint-copy
  "The write spec for one endpoint's full-row copy + its bounded descriptor row.
   Descriptor is initialized with zero counts; count-deltas are applied on the
   target task in one microbatch transaction (see apply-descriptor-delta)."
  [direction kind target-key sort-key ts]
  (let [dk (target-descriptor-key direction kind)]
    {:target-key    target-key
     :sort-key      sort-key
     :descriptor-key dk
     :descriptor    (->RelationTargetDescriptorRow dk target-key direction kind 0 0 ts)}))

(defn apply-descriptor-delta
  "term fn: current descriptor (last arg, may be nil) → updated. On init use the
   zero-count base carried from the relation-id task. `max 0` guards underflow."
  [desc-base total-delta asserted-delta existing]
  (let [cur (or existing desc-base)]
    (assoc cur
           :total-count    (max 0 (+ (long (or (:total-count cur) 0)) (long total-delta)))
           :asserted-count (max 0 (+ (long (or (:asserted-count cur) 0)) (long asserted-delta)))
           :updated-at-ms  (:updated-at-ms desc-base))))

(defn rejected-decision-row
  [decision-id relation-id request-id request-type idempotency-key reason errors ts material-hash
   envelope-actor-id envelope-actor-type]
  (->RelationDecisionRow decision-id relation-id request-id request-type idempotency-key
                         :rejected reason (vec errors) nil ts nil material-hash
                         envelope-actor-id envelope-actor-type))

(defn relation-outcome
  "Pure decision for one request against the current authoritative row.
   Rejected → {:accepted? false :decision ...}. Accepted → the full write set.
   Note: because the asserter is part of the identity, an existing row on the
   relation-id task is always the SAME asserter — so :relation/assert needs no
   asserter check; only :relation/retract compares the ENVELOPE actor against
   the stored asserter (retraction rights, CONTRACT §8)."
  [request relation-id current-row]
  (let [request-type (relreq-type request)
        request-id   (relreq-id request)
        idem-key     (relreq-idempotency-key request)
        actor        (relreq-actor request)
        actor-id     (:actor/id actor)
        actor-type   (:actor/type actor)   ; envelope custody (§5.1)
        payload      (relreq-payload request)
        kind         (:relation-kind payload)
        from         (:from payload)
        to           (:to payload)
        asserter-id  (:asserter-actor-id payload)
        asserter-typ (:asserter-type payload)
        ev-src       (:evidence-source-id payload)
        ev-anch      (:evidence-anchor-id payload)
        note         (:note payload)
        ts           (long (or (:asserted-at-ms payload) 0))
        material-hash (sha1-hex (pr-str [request-type kind
                                         (:target-id from) (:target-id to) asserter-id]))
        decision-id  (decision-id-for relation-id request-id)
        errors       (request-shape-errors request relation-id)]
    (cond
      ;; shape rejection (bad kind, malformed target, missing field, ...)
      (seq errors)
      {:accepted? false
       :decision (rejected-decision-row decision-id relation-id request-id request-type
                                        idem-key (:type (first errors)) errors ts material-hash
                                        actor-id actor-type)}

      ;; retract of a relation that does not exist
      (and (= request-type :relation/retract) (nil? current-row))
      {:accepted? false
       :decision (rejected-decision-row decision-id relation-id request-id request-type idem-key
                                        :relation/absent [{:type :relation/absent}] ts material-hash
                                        actor-id actor-type)}

      ;; retract by someone other than the original asserter
      (and (= request-type :relation/retract)
           (not= actor-id (:asserter-actor-id current-row)))
      {:accepted? false
       :decision (rejected-decision-row decision-id relation-id request-id request-type idem-key
                                        :relation/retraction-forbidden
                                        [{:type :relation/retraction-forbidden}] ts material-hash
                                        actor-id actor-type)}

      ;; accepted transition (new assert, reassert, reassert-after-retract,
      ;; retract, or retract-affirm). All write event/row/log/copies; only the
      ;; descriptor deltas differ (count-deltas).
      :else
      (let [new-status  (if (= request-type :relation/retract) :retracted :asserted)
            prev-status (when current-row (:relation-status current-row))
            first-ms    (if current-row (long (:first-asserted-at-ms current-row)) ts)
            order-key   (oc/fixed-width-order-key ts request-id)
            event-id    (event-id-for relation-id order-key)
            row         (transition-row current-row relation-id kind from to asserter-id asserter-typ
                                        new-status ts event-id request-id ev-src ev-anch note
                                        actor-id actor-type)
            event       (->RelationEventRow event-id relation-id
                                            (if (= new-status :retracted) :relation/retracted :relation/asserted)
                                            new-status kind from to asserter-id asserter-typ
                                            ev-src ev-anch note ts request-id decision-id prev-status
                                            actor-id actor-type)
            log         (->RelationStatusLogRow order-key event-id relation-id new-status ts
                                                request-id decision-id prev-status)
            deltas      (count-deltas current-row new-status)
            sort-o      (target-sort-key :outgoing kind first-ms relation-id)
            sort-i      (target-sort-key :incoming kind first-ms relation-id)]
        {:accepted? true
         :decision (->RelationDecisionRow decision-id relation-id request-id request-type
                                          idem-key :accepted nil [] event-id ts nil material-hash
                                          actor-id actor-type)
         :event event
         :row row
         :log log
         :order-key order-key
         :total-delta (:total deltas)
         :asserted-delta (:asserted deltas)
         ;; from-side copy (outgoing) and to-side copy (incoming). For unary
         ;; :none, to's target-key == from's, so both copies land on one task
         ;; under distinct "o:"/"i:" sort-keys; R1 dedups by relation-id.
         :from-copy (endpoint-copy :outgoing kind (:target-key from) sort-o ts)
         :to-copy   (endpoint-copy :incoming kind (:target-key to)   sort-i ts)}))))

;; Small dataflow-position accessors for the outcome (keywords cannot sit in
;; operation position; these keep the topology body readable).
(defn outcome-accepted?      [o] (:accepted? o))
(defn outcome-decision       [o] (:decision o))
(defn outcome-event          [o] (:event o))
(defn outcome-row            [o] (:row o))
(defn outcome-log            [o] (:log o))
(defn outcome-order-key      [o] (:order-key o))
(defn outcome-total-delta    [o] (:total-delta o))
(defn outcome-asserted-delta [o] (:asserted-delta o))
(defn outcome-from-copy      [o] (:from-copy o))
(defn outcome-to-copy        [o] (:to-copy o))
(defn decision-row-id        [d] (:decision-id d))
(defn event-row-id           [e] (:event-id e))
(defn copy-target-key        [c] (:target-key c))
(defn copy-sort-key          [c] (:sort-key c))
(defn copy-descriptor-key    [c] (:descriptor-key c))
(defn copy-descriptor        [c] (:descriptor c))

;; ── Query R1 helpers (relations-for-targets) ─────────────────────────────────
(defn distinct-present-target-keys
  "Dedup + drop nil/blank target keys. Empty input → [] → the query returns {}
   with zero PState reads. A nil key never becomes a global scan (IMPLICIT_SPEC R1)."
  [target-keys]
  (vec (distinct (filter present-string? target-keys))))

(defn relation-read-ranges
  "Descriptor-gated read plan for one target and status mode. Returns one of:
     []            — no descriptor survives ⇒ no visible relations ⇒ NO seek
                     (nil seed; the Phase-2 empty-seek rule).
     [:all]        — no kind filter, one-or-more descriptors survive ⇒ ONE
                     whole-map read of $$relations-by-target[target-key]. This is
                     CONTRACT §6's dominant 'all relations touching X = 1 seek'
                     path (F1 fix): the surviving (dir,kind) groups are contiguous
                     in the inner sorted map, so one seek + sequential iteration
                     beats the up-to-14 (2 dir x 7 kind) separate prefix seeks it
                     replaces. relation-visible? still drops the retracted rows a
                     whole-map read carries.
     [[lo hi] ...] — kind filter present ⇒ one bounded prefix range per surviving,
                     requested (direction, kind) group (unchanged read path).
   descriptor-map may be nil."
  [descriptor-map kinds-filter include-retracted?]
  (let [kinds     (when (seq kinds-filter) (set (map keyword kinds-filter)))
        survives? (fn [desc]
                    (pos? (if include-retracted?
                            (long (or (:total-count desc) 0))
                            (long (or (:asserted-count desc) 0)))))]
    (if (nil? kinds)
      ;; No kind filter: collapse every surviving (dir,kind) group into ONE
      ;; whole-map read, or [] when the target has no visible relations.
      (if (some survives? (vals descriptor-map)) [:all] [])
      ;; Kind filter: one prefix range per requested (dir,kind) group that survives.
      (vec
       (for [[dk desc] descriptor-map
             :when (and (survives? desc)
                        (contains? kinds (:relation-kind desc)))]
         (descriptor-prefix-bounds dk))))))

(defn whole-map-plan?
  "True when `relation-read-ranges` selected the single whole-map read (the F1
   no-kind-filter path): distinguishes the `[:all]` marker from `[[lo hi] ...]`."
  [ranges]
  (= :all (first ranges)))

(defn relation-visible?
  "Row filter after a range read. A nil row is the per-target presence seed
   (keeps empty/all-retracted targets in the result as [])."
  [row include-retracted?]
  (or (nil? row)
      (true? include-retracted?)
      (= :asserted (:relation-status row))))

(defn relations-pairs->map
  "Post-agg: fold [target-key row|nil] pairs into {target-key [rows...]}. Every
   requested target emits at least one pair (a real row, or a nil seed), so
   empty targets appear as []. Dedup by relation-id (unary :none can surface a
   row under both its o/i entries); stable order by (first-asserted-at-ms, id)."
  [pairs]
  (let [grouped
        (reduce
         (fn [acc pair]
           (let [tk  (nth pair 0)
                 row (nth pair 1)]
             (cond
               (nil? row)
               (if (contains? acc tk) acc (assoc acc tk []))
               (some (fn [r] (= (:relation-id r) (:relation-id row))) (get acc tk))
               acc
               :else
               (assoc acc tk (conj (get acc tk []) row)))))
         {}
         pairs)]
    (persistent!
     (reduce-kv
      (fn [m tk rows]
        (assoc! m tk (vec (sort-by (juxt :first-asserted-at-ms :relation-id) rows))))
      (transient {})
      grouped))))

(defn relation-detail-result
  [row log-vec]
  {:row row
   :history (if row
              (vec (sort-by :order-key (or log-vec [])))
              [])})

;; ─────────────────────────────────────────────────────────────────────────────
;;   MODULE
;; ─────────────────────────────────────────────────────────────────────────────
(defmodule relation-kernel-module [setup topologies]
  ;; Depot partitioned by relation-id (F1): the map envelope carries a top-level
  ;; :relation/routing-key = relation-id, so request, idempotency journal,
  ;; decision, event, status log, and the authoritative row all colocate on
  ;; hash(relation-id).
  (declare-depot setup *relation-request-depot (hash-by :relation/routing-key))

  (let [mb (microbatch-topology topologies "relation-kernel-topology")]
    ;; Journal gate — relation-scoped (relation-id, idempotency-key) per the F2
    ;; ruling (CONTRACT §5). Colocated on hash(relation-id): the entry for rel:A
    ;; lives on hash(A). Subindexed inner map — an import can issue many keys for
    ;; one relation over time.
    (declare-pstate mb $$relation-decisions-by-idempotency
                    {String (map-schema String RelationDecisionRow {:subindex? true})})
    ;; Audit by decision-id, colocated on hash(relation-id) via key-partitioner.
    (declare-pstate mb $$relation-decisions-by-id
                    {String RelationDecisionRow}
                    {:key-partitioner partition-by-decision-relation})
    ;; Accepted-transition audit by event-id, colocated on hash(relation-id).
    (declare-pstate mb $$relation-events-by-id
                    {String RelationEventRow}
                    {:key-partitioner partition-by-event-relation})
    ;; Authoritative current row.
    (declare-pstate mb $$relations-by-id {String RelationEdgeRow})
    ;; Ordered status history, subindexed (unbounded per relation).
    (declare-pstate mb $$relation-status-log-by-relation
                    {String (map-schema String RelationStatusLogRow {:subindex? true})})
    ;; THE read shape (CONTRACT §6): target-key → sort-key → full row copy.
    ;; Subindexed — a popular doc/commit/conversation accrues unbounded relations.
    (declare-pstate mb $$relations-by-target
                    {String (map-schema String RelationEdgeRow {:subindex? true})})
    ;; Bounded read-descriptor index that gates R1 range reads. NOT subindexed:
    ;; ≤ 2 × registry rows per target (14 with the starter registry).
    (declare-pstate mb $$relation-target-descriptors
                    {String (map-schema String RelationTargetDescriptorRow)})

    (<<sources mb
      (source> *relation-request-depot :> %requests)
      (%requests :> *request)

      ;; Records already arrive on hash(relation-id) — no |hash at ingress.
      (relreq-routing-key *request :> *relation-id)
      ;; Blank routing key = unkeyable: drop before any read/write (client refuses
      ;; too). This is the ONLY silent drop; every other malformation is a durable
      ;; rejected decision.
      (filter> (present-string? *relation-id))

      (relreq-idempotency-key *request :> *idempotency-key)
      (safe-journal-key *idempotency-key :> *journal-key)

      ;; Journal gate (CONTRACT §5 step 2): duplicate (relation-id, key) →
      ;; the first decision is already durable; replay = write NOTHING and stop.
      ;; Exactly-once microbatch handles record replay; this handles client
      ;; foreign-append! retries.
      (local-select> [(keypath *relation-id *journal-key)]
                     $$relation-decisions-by-idempotency :> *prior-decision)
      (filter> (nil? *prior-decision))

      ;; Read the authoritative row (colocated) and decide the whole outcome.
      (local-select> [(keypath *relation-id)] $$relations-by-id :> *current-row)
      (relation-outcome *request *relation-id *current-row :> *outcome)

      ;; Every processed request produces a decision (accepted or rejected),
      ;; journaled + audited on the relation-id task.
      (outcome-decision *outcome :> *decision)
      (decision-row-id *decision :> *decision-id)
      (local-transform> [(keypath *relation-id *journal-key) (termval *decision)]
                        $$relation-decisions-by-idempotency)
      (local-transform> [(keypath *decision-id) (termval *decision)]
                        $$relation-decisions-by-id)

      ;; Accepted → world-truth writes. Rejected → nothing beyond the decision
      ;; (gate 6: unregistered kind writes zero rows outside the decision spine).
      (<<if (outcome-accepted? *outcome)
        (outcome-event *outcome :> *event)
        (outcome-row *outcome :> *row)
        (outcome-log *outcome :> *log)
        (outcome-order-key *outcome :> *order-key)
        (event-row-id *event :> *event-id)
        ;; colocated audit + authoritative + history (all on hash(relation-id))
        (local-transform> [(keypath *event-id) (termval *event)] $$relation-events-by-id)
        (local-transform> [(keypath *relation-id) (termval *row)] $$relations-by-id)
        (local-transform> [(keypath *relation-id *order-key) (termval *log)]
                          $$relation-status-log-by-relation)

        ;; Extract endpoint write specs + shared deltas while still on the
        ;; relation-id task, so only scalars/records cross the partitioner hops.
        (outcome-from-copy *outcome :> *from-copy)
        (outcome-to-copy *outcome :> *to-copy)
        (outcome-total-delta *outcome :> *total-delta)
        (outcome-asserted-delta *outcome :> *asserted-delta)
        (copy-target-key *from-copy :> *from-tk)
        (copy-sort-key *from-copy :> *from-sk)
        (copy-descriptor-key *from-copy :> *from-dk)
        (copy-descriptor *from-copy :> *from-desc)
        (copy-target-key *to-copy :> *to-tk)
        (copy-sort-key *to-copy :> *to-sk)
        (copy-descriptor-key *to-copy :> *to-dk)
        (copy-descriptor *to-copy :> *to-desc)

        ;; from-side copy: hop to hash(from-tk). termval = write-only (no read),
        ;; so the whole row is rewritten every time — copies can't disagree
        ;; across a batch boundary (trap 7). Descriptor delta via term.
        (|hash *from-tk)
        (local-transform> [(keypath *from-tk *from-sk) (termval *row)] $$relations-by-target)
        (local-transform> [(keypath *from-tk *from-dk)
                           (term (partial apply-descriptor-delta *from-desc *total-delta *asserted-delta))]
                          $$relation-target-descriptors)

        ;; to-side copy: hop to hash(to-tk). Same batch = cross-partition
        ;; atomicity; a mid-batch crash replays the whole attempt (trap 1).
        (|hash *to-tk)
        (local-transform> [(keypath *to-tk *to-sk) (termval *row)] $$relations-by-target)
        (local-transform> [(keypath *to-tk *to-dk)
                           (term (partial apply-descriptor-delta *to-desc *total-delta *asserted-delta))]
                          $$relation-target-descriptors))))

  ;; ── Query topologies — the ONLY public read surface (CONTRACT §7) ───────────

  ;; R1: batch-first "all relations touching these targets". Fans |hash per
  ;; target, descriptor-gates the range reads, aggregates at |origin. Consumers
  ;; resolve a whole visible DAG region in one roundtrip.
  (<<query-topology topologies "relations-for-targets"
    [*target-keys *kinds-filter *include-retracted? :> *result]
    (distinct-present-target-keys *target-keys :> *target-keys-clean)
    (ops/explode *target-keys-clean :> *target-key)
    (|hash *target-key)
    (local-select> [(keypath *target-key)] $$relation-target-descriptors :> *descriptor-map)
    (relation-read-ranges *descriptor-map *kinds-filter *include-retracted? :> *ranges)
    (<<if (empty? *ranges)
      ;; No visible rows: emit a nil seed so the target still appears as [].
      (identity nil :> *row)
      (else>)
      (<<if (whole-map-plan? *ranges)
        ;; No kind filter (F1): ONE whole-map read. The surviving (dir,kind)
        ;; groups are contiguous, so 1 seek + sequential iteration beats up to
        ;; 14 prefix seeks (CONTRACT §6 "all relations touching X = 1 seek").
        ;; relation-visible? below drops any retracted rows this carries.
        (local-select> [(keypath *target-key) MAP-VALS]
                       $$relations-by-target {:allow-yield? true} :> *row)
        (else>)
        ;; Kind filter: one bounded prefix range read per requested (dir, kind)
        ;; group; yield-safe because a target can accrue unbounded relations.
        (ops/explode *ranges :> [*lo *hi])
        (local-select> [(keypath *target-key) (sorted-map-range *lo *hi) MAP-VALS]
                       $$relations-by-target {:allow-yield? true} :> *row)))
    (filter> (relation-visible? *row *include-retracted?))
    (vector *target-key *row :> *pair)
    (|origin)
    (aggs/+vec-agg *pair :> *pairs)
    (relations-pairs->map *pairs :> *result))

  ;; R2: one relation + its ordered status history. Single partition
  ;; (hash(relation-id)) → subselect collects history into one vector, no
  ;; aggregation needed. Missing/malformed id → {:row nil :history []}, no
  ;; history seek.
  (<<query-topology topologies "relation-detail" [*relation-id :> *result]
    (|hash *relation-id)
    (<<if (valid-relation-id? *relation-id)
      (local-select> [(keypath *relation-id)] $$relations-by-id :> *row)
      (<<if (some? *row)
        (local-select> [(keypath *relation-id) (subselect MAP-VALS)]
                       $$relation-status-log-by-relation {:allow-yield? true} :> *log-vec)
        (relation-detail-result *row *log-vec :> *result)
        (|origin)
        (else>)
        (relation-detail-result *row [] :> *result)
        (|origin))
      (else>)
      (relation-detail-result nil [] :> *result)
      (|origin))))

;; ─────────────────────────────────────────────────────────────────────────────
;;   FOREIGN CLIENT  (request builders + the two read wrappers)
;;
;;   Product consumers (Electric server, trail-view projections, agents) use
;;   ONLY read-relations-for-targets / read-relation-detail. The direct PState
;;   readers below are validation-only (V1) and must not be wired into product
;;   code.
;; ─────────────────────────────────────────────────────────────────────────────

(defn ->target-ref
  "Build a RelationTargetRef with the CONTRACT §4 target-key derivation. For a
   unary :none target, pass the from-side target-key so it is inherited."
  ([target-kind target-id] (->target-ref target-kind target-id nil))
  ([target-kind target-id from-target-key]
   (let [tkey (cond
                (= :none target-kind) (str from-target-key)
                (contains? #{:container :source :doc-file :conversation} target-kind)
                (oc/extract-object-key target-id)
                (= :git-commit target-kind) (str target-id)
                :else (str target-id))]     ; unknown kinds: verbatim, never throw
     (->RelationTargetRef target-kind target-id tkey))))

(defn unary-to-ref
  "The `to` ref for a unary mark (e.g. dead-end) on `from-ref`."
  [from-ref]
  (->target-ref :none nil (:target-key from-ref)))

(defn- envelope
  [request-type {:keys [kind from to asserter-actor-id asserter-type actor
                        evidence-source-id evidence-anchor-id note asserted-at-ms
                        request-id idempotency-key]}]
  (let [relation-id (relation-id-for kind from to asserter-actor-id)]
    {:relation/routing-key relation-id
     :request/id           request-id
     :request/type         request-type
     :idempotency/key      idempotency-key
     :actor                (or actor {:actor/id asserter-actor-id :actor/type asserter-type})
     :payload              (->RelationMutationPayload relation-id kind from to
                                                      asserter-actor-id asserter-type
                                                      evidence-source-id evidence-anchor-id
                                                      note asserted-at-ms)}))

(defn assert-request
  "Build a :relation/assert envelope. Required opts: :kind :from :to
   :asserter-actor-id :asserter-type :asserted-at-ms :request-id :idempotency-key.
   Optional: :actor (defaults to the asserter), :evidence-source-id,
   :evidence-anchor-id, :note."
  [opts]
  (envelope :relation/assert opts))

(defn retract-request
  "Build a :relation/retract envelope. The relation-id is reconstructed from
   :kind/:from/:to/:asserter-actor-id (the ORIGINAL asserter); pass :actor to
   set the retracting actor (retraction is rejected unless it matches the
   original asserter)."
  [opts]
  (envelope :relation/retract opts))

(defn relation-id-of-request [request] (relreq-routing-key request))

;; ── Runtime (mirrors the dogfood kernels' start/stop shape) ─────────────────
(defn start-relation-runtime!
  ([] (start-relation-runtime! {:tasks 4 :threads 2}))
  ([launch-opts]
   (let [ipc (create-ipc)
         module-name (get-module-name relation-kernel-module)]
     (launch-module! ipc relation-kernel-module launch-opts)
     {:ipc ipc
      :module-name module-name
      :relation-request-depot (foreign-depot ipc module-name "*relation-request-depot")
      :decisions-by-idempotency (foreign-pstate ipc module-name "$$relation-decisions-by-idempotency")
      :decisions-by-id (foreign-pstate ipc module-name "$$relation-decisions-by-id")
      :events-by-id (foreign-pstate ipc module-name "$$relation-events-by-id")
      :relations-by-id (foreign-pstate ipc module-name "$$relations-by-id")
      :status-log-by-relation (foreign-pstate ipc module-name "$$relation-status-log-by-relation")
      :relations-by-target (foreign-pstate ipc module-name "$$relations-by-target")
      :target-descriptors (foreign-pstate ipc module-name "$$relation-target-descriptors")
      :relations-for-targets-query (foreign-query ipc module-name "relations-for-targets")
      :relation-detail-query (foreign-query ipc module-name "relation-detail")})))

(defn close-relation-runtime!
  [runtime]
  (when-let [ipc (:ipc runtime)]
    (try (.close ^java.lang.AutoCloseable ipc) (catch Exception _ nil))))

(defn append-relation-request!
  "Single foreign-append! of one request envelope. Blank routing key is refused
   client-side (the topology also drops it)."
  ([runtime request] (append-relation-request! runtime request :append-ack))
  ([runtime request ack-level]
   (when (blank-string? (relreq-routing-key request))
     (throw (IllegalArgumentException.
             "relation request requires a non-blank :relation/routing-key (relation-id)")))
   (foreign-append! (:relation-request-depot runtime) request ack-level)
   request))

;; ── Public reads (the only product surface) ─────────────────────────────────
(defn read-relations-for-targets
  "Batch read: {target-key [RelationEdgeRow ...]}. Empty/blank target list short-
   circuits to {} with no query roundtrip."
  ([runtime target-keys] (read-relations-for-targets runtime target-keys nil false))
  ([runtime target-keys kinds-filter include-retracted?]
   (let [clean (distinct-present-target-keys target-keys)]
     (if (empty? clean)
       {}
       (foreign-invoke-query (:relations-for-targets-query runtime)
                             clean (vec (or kinds-filter [])) (boolean include-retracted?))))))

(defn read-relation-detail
  "{:row RelationEdgeRow|nil :history [RelationStatusLogRow ...]}."
  [runtime relation-id]
  (foreign-invoke-query (:relation-detail-query runtime) relation-id))

;; ── Validation-only PState reads (V1 — tests only, never product code) ───────
(defn read-decision-by-idempotency
  [runtime relation-id idempotency-key]
  (first (foreign-select [(keypath relation-id (safe-journal-key idempotency-key))]
                         (:decisions-by-idempotency runtime))))

(defn read-decision-by-id
  [runtime decision-id]
  (first (foreign-select [(keypath decision-id)] (:decisions-by-id runtime))))

(defn read-event-by-id
  [runtime event-id]
  (first (foreign-select [(keypath event-id)] (:events-by-id runtime))))

(defn read-relation-row
  [runtime relation-id]
  (first (foreign-select [(keypath relation-id)] (:relations-by-id runtime))))

(defn read-target-index
  "All [sort-key row] entries under a target key (subindexed → read via ALL)."
  [runtime target-key]
  (foreign-select [(keypath target-key) ALL] (:relations-by-target runtime)))

(defn read-target-descriptors
  [runtime target-key]
  (first (foreign-select [(keypath target-key)] (:target-descriptors runtime))))

;; ── Read-after-write barrier for microbatch (poll, like the compute kernel) ──
(defn await-relation
  "Poll `(read-f)` until `pred` holds or timeout. Microbatch :append-ack does NOT
   imply PState visibility, so tests must wait on the materialized read."
  ([read-f pred] (await-relation read-f pred 3000))
  ([read-f pred timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop [v (read-f)]
       (cond
         (pred v) v
         (>= (System/currentTimeMillis) deadline) v
         :else (do (Thread/sleep 25) (recur (read-f))))))))
