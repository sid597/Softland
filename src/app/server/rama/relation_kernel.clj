(ns app.server.rama.relation-kernel
  "Typed, provenance-carrying relation assertions and retractions.
   A plain request envelope enters the relation-keyed depot. The microbatch
   journals a decision, then writes accepted events, current edges, status history,
   endpoint copies/descriptors, and arrival-day activity in the same batch.
   Target material belongs to other modules; dangling references are allowed.
   Three query topologies serve target sets, relation detail, and activity.
   Foreign wrappers construct requests/read results; the IPC helper owns its
   test cluster, while door/cluster supplies durable-cluster handles.
   Identity includes the asserter when built by relation-id-for. Incoming routing
   keys and endpoint keys are supplied data, not recomputed by the topology."
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
;;   Own depot, one microbatch topology, and three query topologies. The
;;   object-container dependency is limited to extract-object-key for target
;;   routing and fixed-width-order-key for stored index/history keys.
;;
;;   Why microbatch, not stream (CONTRACT §5, trap 1): a relation write lands in
;;   three places on up to three tasks — the authoritative row on hash(rel-id)
;;   and a full-row copy on each endpoint's target task. Microbatch gives
;;   cross-partition exactly-once atomicity per attempt, so the three copies can
;;   never disagree across a batch boundary. Stream commits per hop, so a replay
;;   between hops would partially apply the triple write.
;;
;;   Target, detail, and activity queries are the composed read surface.
;;   Direct PState readers below also expose decisions and rows to callers
;;   that need audit/read-after-write checks.
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
    :confirms :refutes :supersedes
    ;; code-atom mechanical dependency edges (code-atom CONTRACT §2, SPEC §5.1):
    ;; :requires (ns→ns, from the ns form) + :calls (var→var continuant, clj-kondo).
    :requires :calls
    ;; sense-block mechanical floor + composition (block-kernel CONTRACT §12, SPEC
    ;; §11.1/§11.3): :grounds (read-tool observation), :assembled-from (assembly,
    ;; §8.1), :refines (demand refinement, §5). :produced is already registered
    ;; above and carries the write-tool floor edge. One-line reviewed add.
    :grounds :assembled-from :refines
    ;; machine-cut discourse structure (machine-cut CONTRACT §4.1; the framework
    ;; CONTRACT §8 pre-named form-break, fired at G25; authorized by Sid
    ;; 2026-07-12, decisions.md dated entry). Directed: from = response event,
    ;; to = user-message/prompt event ("this response pairs-with that prompt").
    ;; That package's ONE kernel edit. One-line reviewed add.
    :pairs-with
    ;; editable-material P4 reviewed line 1/2: semantic kind predicate.
    :instance-of
    ;; editable-material P4 reviewed line 2/2: machine-proposed aboutness.
    :felt-at})

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
  "True only for a nonblank string."
  [x]
  (and (string? x) (not (str/blank? x))))

(defn blank-string?
  "True for a nonstring or blank string."
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
  "Build rel:<sha1> from kind, directed endpoint kinds/IDs, and asserter identity
   separated by NULs. Kind and endpoint kinds must support name."
  [kind from-ref to-ref asserter-actor-id]
  (str "rel:"
       (sha1-hex (str/join id-part-separator
                           [(name kind)
                            (name (:target-kind from-ref)) (str (:target-id from-ref))
                            (name (:target-kind to-ref)) (str (:target-id to-ref))
                            (str asserter-actor-id)]))))

(defn valid-relation-id?
  "Check only that the value is a string beginning rel:; does not validate its hash."
  [relation-id]
  (and (string? relation-id) (str/starts-with? relation-id "rel:")))

;; Decision-id / event-id embed the relation-id so a key-partitioner can recover
;; it and colocate audit rows on hash(relation-id). relation-id itself contains
;; no "/decision/" or "/event/" marker, so the split is unambiguous.
(def ^:private decision-marker "/decision/")
(def ^:private event-marker "/event/")

(defn decision-id-for
  "Embed relation-id and request-id in an audit decision key."
  [relation-id request-id] (str relation-id decision-marker request-id))
(defn event-id-for
  "Embed relation-id and transition order-key in an event key."
    [relation-id order-key]  (str relation-id event-marker order-key))

(defn relation-id-from-decision-id
  "Extract the prefix before /decision/, or preserve the string without that marker."
  [decision-id]
  (let [s (str decision-id) i (str/index-of s decision-marker)]
    (if i (subs s 0 i) s)))

(defn relation-id-from-event-id
  "Extract the prefix before /event/, or preserve the string without that marker."
  [event-id]
  (let [s (str event-id) i (str/index-of s event-marker)]
    (if i (subs s 0 i) s)))

(defn partition-by-decision-relation
  "Route a decision key by its embedded relation-id."
  [num-partitions decision-id]
  (positive-partition num-partitions (relation-id-from-decision-id decision-id)))

(defn partition-by-event-relation
  "Route an event key by its embedded relation-id."
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
(defn direction-code
  "Encode :outgoing as o or :incoming as i; other directions throw."
  [direction] (case direction :outgoing "o" :incoming "i"))

(defn target-descriptor-key
  "Build the direction/kind key used to describe an endpoint index range."
  [direction kind]
  (str (direction-code direction) ":" (name kind)))

(defn target-sort-key
  "Build a direction/kind/first-assertion-time/relation-ID key that stays stable
   when an existing relation changes status."
  [direction kind first-asserted-at-ms relation-id]
  (str (target-descriptor-key direction kind) ":"
       (oc/fixed-width-order-key first-asserted-at-ms relation-id)))

(defn descriptor-prefix-bounds
  "[lo hi) bounds that select exactly the sort-keys under descriptor-key. `;`
   (0x3B) is the char just above the `:` (0x3A) delimiter, so every
   \"<dk>:<...>\" sorts below \"<dk>;\" and nothing else does."
  [descriptor-key]
  [(str descriptor-key ":") (str descriptor-key ";")])

;; ── Relation-activity bucketing (trail-view §5.3) ────────────────────────────
;; Arrival-time UTC-day buckets for the recent-activity feed. The bucket width
;; lives in ONE place so the write path, R3, and the trail-view feed wrapper can
;; never drift (PLAN §4 note N6). The topology NEVER reads the wall clock —
;; bucket + order-key are pure functions of client-supplied stamps, so a replayed
;; microbatch re-derives byte-identical rows (trap 4b; contrast the wall-clock
;; stamp at object_container.clj:428/448/467, which double-buckets under replay).
(def ^:private ms-per-utc-day 86400000)
(def ^:private activity-bucket-fmt "%08d")

(defn arrival-day-index
  "Convert arrival milliseconds to a UTC-day index using integer quotient; nil becomes zero."
  [arrival-at-ms] (quot (long (or arrival-at-ms 0)) ms-per-utc-day))
(defn bucket-for-day
  "Format a numeric day index as a zero-padded activity bucket key."
     [day-index]     (format activity-bucket-fmt (long day-index)))
(defn bucket-key
  "Derive the activity bucket key from supplied arrival milliseconds."
         [arrival-at-ms] (bucket-for-day (arrival-day-index arrival-at-ms)))

(defn activity-order-key
  "Row accessor (keywords cannot sit in dataflow operation position)."
  [activity-row]
  (:order-key activity-row))

(defn activity-bucket-range
  "Enumerate inclusive fixed-width day keys for numeric-string bounds; nil or
   reversed bounds return []. Each key is routed by hash, so different days may
   share a task. Non-numeric bounds throw; the day range has no explicit size cap."
  [bucket-lo bucket-hi]
  (let [lo (some-> bucket-lo str Long/parseLong)
        hi (some-> bucket-hi str Long/parseLong)]
    (if (and lo hi (<= lo hi))
      (mapv bucket-for-day (range lo (inc hi)))
      [])))

(defn sort-activity-rows
  "Deterministic ascending order by order-key (arrival-ms prefix + event-id). The
   feed wrapper (Phase B) applies the final desc/clock ordering."
  [rows]
  (vec (sort-by :order-key (or rows []))))

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

;; Relation-activity projection row (trail-view §5.3): one per ACCEPTED transition,
;; arrival-bucketed, feeding R3 + the recent-activity feed. Carries the writer id
;; (envelope-actor-id) but NOT its type — the feed shows who wrote, not the actor
;; kind. Two clocks: claimed-at-ms (payload semantic time) vs arrival-at-ms
;; (envelope :request/sent-at-ms). Written ONLY in the accepted branch (gate 9).
(defrecord RelationActivityRow
  [order-key bucket relation-id relation-kind from-kind from-id to-kind to-id
   asserter-actor-id asserter-type envelope-actor-id relation-status
   previous-status event-id claimed-at-ms arrival-at-ms])

;; ── Envelope / payload accessors (namespaced keys read off the wire map) ─────
(defn relreq-routing-key
  "Return :relation/routing-key from request, or nil when absent."
     [request] (:relation/routing-key request))
(defn relreq-id
  "Return :request/id from request, or nil when absent."
              [request] (:request/id request))
(defn relreq-type
  "Return :request/type from request, or nil when absent."
            [request] (:request/type request))
(defn relreq-idempotency-key
  "Return :idempotency/key from request, or nil when absent."
  [request] (:idempotency/key request))
(defn relreq-actor
  "Return :actor from request, or nil when absent."
           [request] (:actor request))
(defn relreq-payload
  "Return :payload from request, or nil when absent."
         [request] (:payload request))
(defn relreq-sent-at-ms
  "Return :request/sent-at-ms from request, or nil when absent."
      [request] (:request/sent-at-ms request))  ; arrival clock (§5.3)

;; ── Validation (CONTRACT §5 step 3; IMPLICIT_SPEC edge cases). No target
;;    existence check — dangling targets are legal (CONTRACT §8, trap 3). ───────
(defn registered-kind?
  "Test membership in the code-defined relation kind set."
  [kind] (contains? relation-kinds kind))

(defn well-formed-target?
  "Require a keyword kind and nonblank ID/routing key; does not look up target material."
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
  "Construct a rejected relation decision with supplied audit/time/custody fields and errors."
  [decision-id relation-id request-id request-type idempotency-key reason errors ts material-hash
   envelope-actor-id envelope-actor-type]
  (->RelationDecisionRow decision-id relation-id request-id request-type idempotency-key
                         :rejected reason (vec errors) nil ts nil material-hash
                         envelope-actor-id envelope-actor-type))

(defn relation-outcome
  "Compute a rejected decision or accepted event/edge/history/index/activity write set
   from the supplied request, routing ID, and current row. Retraction requires an
   existing relation and its stored asserter matching the envelope actor. Assertions
   have no equivalent actor check. Assumes numeric timestamps and builder-consistent
   relation/endpoint keys; request-shape-errors does not recompute those identities."
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
            sort-i      (target-sort-key :incoming kind first-ms relation-id)
            ;; ── activity projection (trail-view §5.3): TWO client clocks, never
            ;;    the wall clock (trap 4b). claimed = payload semantic time;
            ;;    arrival = envelope :request/sent-at-ms (fallback asserted-at-ms).
            ;;    bucket + order-key are pure fns of client stamps → replay-stable.
            claimed-at-ms ts
            arrival-at-ms (long (or (relreq-sent-at-ms request) (:asserted-at-ms payload) 0))
            bucket        (bucket-key arrival-at-ms)
            activity-ok   (oc/fixed-width-order-key arrival-at-ms event-id)
            activity-row  (->RelationActivityRow
                            activity-ok bucket relation-id kind
                            (:target-kind from) (:target-id from)
                            (:target-kind to) (:target-id to)
                            asserter-id asserter-typ actor-id new-status prev-status
                            event-id claimed-at-ms arrival-at-ms)]
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
         :to-copy   (endpoint-copy :incoming kind (:target-key to)   sort-i ts)
         ;; activity row + its bucket — the 4th microbatch hop (accepted only).
         :activity-row activity-row
         :bucket bucket}))))

;; Small dataflow-position accessors for the outcome (keywords cannot sit in
;; operation position; these keep the topology body readable).
(defn outcome-accepted?
  "Return :accepted? from o, or nil when absent."
       [o] (:accepted? o))
(defn outcome-decision
  "Return :decision from o, or nil when absent."
        [o] (:decision o))
(defn outcome-event
  "Return :event from o, or nil when absent."
           [o] (:event o))
(defn outcome-row
  "Return :row from o, or nil when absent."
             [o] (:row o))
(defn outcome-log
  "Return :log from o, or nil when absent."
             [o] (:log o))
(defn outcome-order-key
  "Return :order-key from o, or nil when absent."
       [o] (:order-key o))
(defn outcome-total-delta
  "Return :total-delta from o, or nil when absent."
     [o] (:total-delta o))
(defn outcome-asserted-delta
  "Return :asserted-delta from o, or nil when absent."
  [o] (:asserted-delta o))
(defn outcome-from-copy
  "Return :from-copy from o, or nil when absent."
       [o] (:from-copy o))
(defn outcome-to-copy
  "Return :to-copy from o, or nil when absent."
         [o] (:to-copy o))
(defn outcome-activity-row
  "Return :activity-row from o, or nil when absent."
    [o] (:activity-row o))
(defn outcome-bucket
  "Return :bucket from o, or nil when absent."
          [o] (:bucket o))
(defn decision-row-id
  "Return :decision-id from d, or nil when absent."
         [d] (:decision-id d))
(defn event-row-id
  "Return :event-id from e, or nil when absent."
            [e] (:event-id e))
(defn copy-target-key
  "Return :target-key from c, or nil when absent."
         [c] (:target-key c))
(defn copy-sort-key
  "Return :sort-key from c, or nil when absent."
           [c] (:sort-key c))
(defn copy-descriptor-key
  "Return :descriptor-key from c, or nil when absent."
     [c] (:descriptor-key c))
(defn copy-descriptor
  "Return :descriptor from c, or nil when absent."
         [c] (:descriptor c))

;; ── Query R1 helpers (relations-for-targets) ─────────────────────────────────
(defn distinct-present-target-keys
  "Dedup + drop nil/blank target keys. Empty input → [] → the query returns {}
   with zero PState reads. A nil key never becomes a global scan (IMPLICIT_SPEC R1)."
  [target-keys]
  (vec (distinct (filter present-string? target-keys))))

(defn relation-read-ranges
  "Plan a target-index read from direction/kind descriptors. Return [] when no
   requested group has visible counts, [:all] for an unfiltered whole-map read,
   or [lo,hi) prefix ranges for selected kinds. Later filtering removes retracted
   rows when requested. A prefix bounds a key range, not the number of results."
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
  "Return the current row and order-key-sorted history; an absent row has empty history."
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
    ;; Relation-activity projection (trail-view §5.3): accepted transitions bucketed
    ;; by arrival UTC-day. Outer key = fixed-width bucket on hash(bucket); inner
    ;; subindexed map order-key → row on that bucket's task. Read via R3 (the ONLY
    ;; public surface); one task hosts one day's writes (phase-1 volume tens/day).
    (declare-pstate mb $$relation-activity-by-bucket
                    {String (map-schema String RelationActivityRow {:subindex? true})})

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
        ;; Activity row extracted here (still on the relation-id task) so only the
        ;; record + two scalars cross the partitioner hops — same idiom as above.
        (outcome-activity-row *outcome :> *activity-row)
        (outcome-bucket *outcome :> *bucket)
        (activity-order-key *activity-row :> *activity-ok)

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
                          $$relation-target-descriptors)

        ;; ── 4th hop: relation-activity projection (trail-view §5.3; feeds R3 + §6
        ;;    recent-activity). Reached ONLY on the accepted branch, so rejected
        ;;    decisions and journal-replayed duplicates (dropped at the
        ;;    (nil? *prior-decision) gate) add ZERO activity rows (gate 9). Same
        ;;    microbatch = cross-partition exactly-once per attempt (trap 1).
        ;;    *bucket / *activity-ok / *activity-row derive from client stamps only
        ;;    — NO wall clock (trap 4b) — so a replayed batch re-derives them byte-
        ;;    identical and the write is idempotent.
        (|hash *bucket)
        (local-transform> [(keypath *bucket *activity-ok) (termval *activity-row)]
                          $$relation-activity-by-bucket))))

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
      (|origin)))

  ;; R3: relation-activity — accepted transitions across a UTC-day bucket range
  ;; (CONTRACT §5.3). Buckets live on hash(bucket), so the day range is enumerated
  ;; + fanned per bucket, each read subindexed + yield-safe, aggregated at |origin,
  ;; sorted by order-key. Terminal aggregation emits exactly once (empty range →
  ;; []). trail-view calls this through its foreign-client feed wrapper.
  (<<query-topology topologies "relation-activity" [*bucket-lo *bucket-hi :> *result]
    (activity-bucket-range *bucket-lo *bucket-hi :> *buckets)
    (ops/explode *buckets :> *bucket)
    (|hash *bucket)
    (local-select> [(keypath *bucket) MAP-VALS]
                   $$relation-activity-by-bucket {:allow-yield? true} :> *row)
    (|origin)
    (aggs/+vec-agg *row :> *rows)
    (sort-activity-rows *rows :> *result)))

;; ─────────────────────────────────────────────────────────────────────────────
;;   FOREIGN CLIENT — request builders, three composed queries, and direct
;;   audit/material reads. Append acknowledgement does not wait for microbatch
;;   processing; callers needing read-after-write use an explicit read barrier.
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
  "Construct a plain relation request with a typed payload and deterministic routing ID.
   Uses supplied IDs/times; sent-at defaults to asserted-at and actor to the asserter.
   Does not validate or append the request."
  [request-type {:keys [kind from to asserter-actor-id asserter-type actor
                        evidence-source-id evidence-anchor-id note asserted-at-ms
                        sent-at-ms request-id idempotency-key]}]
  (let [relation-id (relation-id-for kind from to asserter-actor-id)]
    {:relation/routing-key relation-id
     :request/id           request-id
     :request/type         request-type
     ;; arrival clock (trail-view §5.3): when the CLIENT sent this request. The
     ;; activity projection buckets on this, never the wall clock. Falls back to
     ;; the semantic asserted-at-ms when the caller omits it.
     :request/sent-at-ms   (or sent-at-ms asserted-at-ms)
     :idempotency/key      idempotency-key
     :actor                (or actor {:actor/id asserter-actor-id :actor/type asserter-type})
     :payload              (->RelationMutationPayload relation-id kind from to
                                                      asserter-actor-id asserter-type
                                                      evidence-source-id evidence-anchor-id
                                                      note asserted-at-ms)}))

(defn assert-request
  "Build a :relation/assert envelope. Required opts: :kind :from :to
   :asserter-actor-id :asserter-type :asserted-at-ms :request-id :idempotency-key.
   Optional: :actor (defaults to the asserter), :sent-at-ms (arrival clock, §5.3;
   defaults to :asserted-at-ms), :evidence-source-id, :evidence-anchor-id, :note."
  [opts]
  (envelope :relation/assert opts))

(defn retract-request
  "Build a :relation/retract envelope. The relation-id is reconstructed from
   :kind/:from/:to/:asserter-actor-id (the ORIGINAL asserter); pass :actor to
   set the retracting actor (retraction is rejected unless it matches the
   original asserter)."
  [opts]
  (envelope :relation/retract opts))

(defn relation-id-of-request
  "Return the supplied :relation/routing-key from a request."
  [request] (relreq-routing-key request))

;; ── Runtime (mirrors the dogfood kernels' start/stop shape) ─────────────────
(defn start-relation-runtime!
  "Create an owned IPC, launch the relation module, and return depot/PState/query
   handles. Defaults to four tasks and two threads. Caller must close the IPC."
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
      :relation-detail-query (foreign-query ipc module-name "relation-detail")
      :activity-by-bucket (foreign-pstate ipc module-name "$$relation-activity-by-bucket")
      :relation-activity-query (foreign-query ipc module-name "relation-activity")})))

(defn close-relation-runtime!
  "Close runtime :ipc when present, swallowing close exceptions."
  [runtime]
  (when-let [ipc (:ipc runtime)]
    (try (.close ^java.lang.AutoCloseable ipc) (catch Exception _ nil))))

(defn append-relation-request!
  "Reject a blank routing key, append once with :append-ack by default, and return
   the request. Neither :append-ack nor :ack waits for this microbatch topology.
   Observe a decision/query result separately before claiming acceptance or visibility."
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

(defn read-relation-activity
  "R3 (public): accepted relation transitions across the inclusive fixed-width
   day-bucket range [bucket-lo bucket-hi], ordered by order-key. Empty range → []."
  [runtime bucket-lo bucket-hi]
  (foreign-invoke-query (:relation-activity-query runtime) bucket-lo bucket-hi))

;; ── Direct PState reads for audit/material inspection ──────────────────────
(defn read-decision-by-idempotency
  "Read the first decision journaled for relation-id and idempotency-key, or nil.
   Uses a direct PState handle and the malformed-key sentinel when needed."
  [runtime relation-id idempotency-key]
  (first (foreign-select [(keypath relation-id (safe-journal-key idempotency-key))]
                         (:decisions-by-idempotency runtime))))

(defn read-decision-by-id
  "Directly read a decision by its relation-scoped audit key, or nil."
  [runtime decision-id]
  (first (foreign-select [(keypath decision-id)] (:decisions-by-id runtime))))

(defn read-event-by-id
  "Directly read a relation event by its embedded-relation event key, or nil."
  [runtime event-id]
  (first (foreign-select [(keypath event-id)] (:events-by-id runtime))))

(defn read-relation-row
  "Directly read the current relation row, or nil."
  [runtime relation-id]
  (first (foreign-select [(keypath relation-id)] (:relations-by-id runtime))))

(defn read-target-index
  "All [sort-key row] entries under a target key (subindexed → read via ALL)."
  [runtime target-key]
  (foreign-select [(keypath target-key) ALL] (:relations-by-target runtime)))

(defn read-target-descriptors
  "Directly read the direction/kind descriptor map for target-key, or nil."
  [runtime target-key]
  (first (foreign-select [(keypath target-key)] (:target-descriptors runtime))))

(defn read-activity-rows
  "V1 (tests only): all RelationActivityRow values physically under a bucket. Gate
   9 negatives (rejected + journal-replayed add ZERO) read the PState directly —
   R3's sort/agg would MASK a leaked row. Same [(keypath k) ALL] idiom as
   read-target-index on the identically-shaped $$relations-by-target."
  [runtime bucket]
  (mapv second (foreign-select [(keypath bucket) ALL] (:activity-by-bucket runtime))))

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
