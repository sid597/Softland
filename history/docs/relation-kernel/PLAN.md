# Plan - RelationEdge kernel

<!-- Phase 1. Design only - NO topology, ETL, query topology, or test code. -->

Source inputs:

- `docs/current-mental-model/build/relation-kernel/CONTRACT.md`
- `docs/current-mental-model/build/relation-kernel/IMPLICIT_SPEC.md`
- `docs/current-mental-model/decisions.md` D-001 through D-006
- `.agents/skills/rama/SKILL.md`
- `.agents/skills/rama/references/phase-1-plan.md`
- `docs/current-mental-model/build/relation-kernel/PLAN_VALIDATION.md`
  (`PHASE_VALIDATION:fail`, used as input for this Phase 1 revision)

This plan treats the contract as binding. It does not reopen the closed
decisions, does not move the module into an existing kernel, and does not add
CONTRACT.md section 9 refusals.

## Revision status (Phase 1 revision after 2nd `PHASE_VALIDATION:fail`)

This revision addresses the two blockers in `PLAN_VALIDATION.md`. They resolve
differently:

- **F1 (depot partitioner vs request-row key) — FIXED in this plan, no
  binding-doc change.** The prior plan made a typed `RelationRequestRow`
  defrecord the depot event type. A defrecord field `relation-routing-key` is
  looked up by the keyword `:relation-routing-key`; the depot partitioner
  `(hash-by :relation/routing-key)` reads the *namespaced* key `:relation/routing-key`,
  which the record does not carry, so it would extract `nil` and funnel every
  request to one task — destroying the contract's colocated-journal invariant.
  The fix reverts to CONTRACT §4's request **map envelope** (namespaced keys),
  which is the verified codebase idiom: every kernel appends a plain map and
  the partitioner reads a namespaced key off it — `object_container.clj:1657-1686`
  (request built by `assoc :partition/key …`; `(hash-by :partition/key)`),
  `space.clj:171` (`:idempotency/key :routing/key :actor :target :action :payload`).
  The map→typed-defrecord conversion happens *inside* the topology for PState
  storage only (`object_container.clj:383 request-row`, stored at `:1784-1785`),
  never on the wire. See revised **Writes**, **PState Design**, **Depots**,
  **Topologies**.

- **F2 (idempotency-key scope) — RESOLVED. Fable ruled (A) relation-scoped
  `(relation-id, idempotency-key)` on 2026-07-03** (referred by Sid per the stop
  clause; `decisions.md` Open Questions, CLOSED). This matched the plan's retained
  scope, so **no design change** was needed. Fable executed the binding-doc
  amendments: CONTRACT §5 (explicit scope ruling), CONTRACT §11 gate 11
  (cross-relation key reuse), IMPLICIT_SPEC amendment banner. The plan now aligns
  with the amended contract and is **ready for Phase 2 re-validation**. Rationale
  of record in **Idempotency scope (RESOLVED)** below.

## Rama Semantic Verification

Required by the handoff before code.

### Microbatch claim

Contract claim: a mid-batch failure replays the batch, and PState writes are
transactional per batch across tasks.

Finding: supported by `.agents/skills/rama/references/microbatch.md`.

- The reference states that microbatch topologies provide exactly-once PState
  updates and cross-partition atomicity.
- Its formal model says partitioners do not fragment the transaction scope:
  writes before and after a partitioner remain in the same microbatch
  transaction.
- Retry of the same microbatch id replays the same depot records
  deterministically and applies PState updates exactly once.
- The attempt phases are `prime -> process -> commit`; prime resets PStates to
  the previous microbatch state, and commit checkpoints/replicates with PState
  changes visible atomically.
- Caveat: `depot-partition-append!` inside a microbatch is explicitly not
  exactly-once on retry. This plan uses no internal depot appends in the
  relation write topology.

Plan implication: the contract's three-copy write
`$$relations-by-id` plus both target indexes can be one microbatch
transaction. A failed attempt must not leave only one endpoint index updated.

### Query-topology claim

Contract claim: `relations-for-targets` can fan out per target key with
`|hash`, read locally, and aggregate at `|origin`.

Finding: supported by `.agents/skills/rama/references/query-topologies.md` and
`batch.md`.

- Query topologies are distributed, read-only batch blocks.
- The reference requires final pre-agg partitioning back to `|origin`, with
  exactly one emitted output variable.
- Variable read counts must be handled dynamically with `ops/explode`,
  `loop<-`, or equivalent, then aggregated.
- The cross-partition join example hashes per related entity, performs
  `local-select>`, returns to `|origin`, and aggregates the rows.
- `batch.md` confirms the core pattern: explode inputs, process each emitted
  row, then aggregate.

Plan implication: `relations-for-targets` must not do client-side N query
invocations or hard-coded fixed local selects. It explodes the requested target
keys, descriptor-gates the range prefixes, hashes each target key, reads locally,
and aggregates the per-target results at origin.

## Reads

### R1. `relations-for-targets`

Input:

```clojure
[*target-keys *kinds-filter *include-retracted? :> *result]
```

Result:

```clojure
{target-key [RelationEdgeRow ...]}
```

Access method: query topology. This is cross-partition and batch-shaped by
definition; product consumers must not perform N foreign selects over
`$$relations-by-target`.

Partition and path:

- Deduplicate `*target-keys`.
- If the target list is empty, return `{}` with zero PState reads.
- For each requested key, `|hash` the target key.
- First point-read `$$relation-target-descriptors` at
  `[(keypath *target-key)]`. `keypath` on an absent target emits nil, so this
  is the meaningful absence/presence proof for the target key rather than an
  empty range seek.
- The descriptor map is bounded by the code-level kind registry times two
  directions. Each descriptor records `total-count` and `asserted-count` for a
  target/direction/kind prefix.
- If the descriptor map is nil, or if the relevant status count is zero, emit an
  empty result for that target key and perform no `$$relations-by-target` range
  read.
- Read `$$relations-by-target` at
  `[(keypath *target-key) MAP-VALS]` only when there is no kind filter and the
  descriptor summary proves at least one row survives the
  `*include-retracted?` mode.
- When the kind filter is present, derive direction/kind prefixes from
  descriptors whose count is nonzero for the requested status mode, and read
  sorted subranges under `[(keypath *target-key) (sorted-map-range *lo *hi)]`.
- Every range read over `$$relations-by-target` uses
  `{:allow-yield? true}` because a target can accumulate unbounded relations.
- Filter retracted rows unless `*include-retracted?` is true.
- Return through `|origin` and aggregate into a map keyed by target key.
- Deduplicate by `[target-key relation-id]` during aggregation so unary
  `:none` rows do not appear twice if both outgoing and incoming index entries
  land under the same inherited target key.

Read cost examples:

- `[]`, no filter -> 0 total reads, 0 meaningful reads.
- `["doc-A"]`, no filter, no rows -> 1 descriptor point read, 0 target range
  reads. The descriptor read is meaningful because it proves absence without
  navigating an empty `$$relations-by-target` range.
- `["doc-A"]`, no filter, rows present -> 1 descriptor point read + 1 target
  range read. Both reads are meaningful.
- `["doc-A" "doc-B" "doc-C"]`, no filter, only doc-A/doc-C have visible rows ->
  3 descriptor point reads + 2 target range reads. No empty target range seek is
  issued for doc-B.
- `["doc-A"]`, filter `#{:based-on}`, only outgoing asserted rows present -> 1
  descriptor point read + 1 outgoing prefix range read. The missing incoming
  prefix is skipped from descriptor counts.
- `["doc-A" "doc-B"]`, filter `#{:based-on :produced}`, with visible rows only
  for doc-A/outgoing/based-on and doc-B/incoming/produced -> 2 descriptor point
  reads + 2 prefix range reads. The possible `targets x directions x kinds`
  prefixes are generated dynamically but only nonzero descriptors become range
  reads.

Fixed or variable: variable, based on target count, filter count, descriptor
presence, and include-retracted mode.

Dynamic approach: `ops/explode` target keys; point-read the bounded descriptor
map for each target; use a helper to emit zero or more read descriptors from the
nonzero status counts; for filtered reads, the emitted descriptors are
direction/kind prefixes. Each emitted row carries the target key through the
range read and aggregation. Empty input emits no reads. Aggregation/post-process
merges in requested target keys with empty vectors so unknown targets are still
represented without empty range seeks.

Why the descriptor PState is worth the extra write: the Phase 2 validation
template treats empty prefix/range seeks as a failure. The descriptor map adds a
bounded O(2) write-side update per accepted status event and preserves the hot
full-row target index, while allowing filtered queries to issue only range reads
known to contain rows that can survive the status mode.

### R2. `relation-detail`

Input:

```clojure
[*relation-id :> *result]
```

Result:

```clojure
{:row RelationEdgeRow
 :history [RelationStatusLogRow ...]}
```

Access method: query topology. The read starts with a nil-safe point lookup on
the relation-id partition and performs the status-history range read only when
the relation row exists. It remains part of the public read surface, so it is not
exposed as direct foreign selects to consumers.

Partition and path:

- Reject/empty-return on nil or malformed relation ids without scanning.
- `|hash *relation-id`.
- Point read `$$relations-by-id` at `[(keypath *relation-id)]`.
- If the row is nil, return `{:row nil :history []}` without reading status
  history.
- If the row exists, range read `$$relation-status-log-by-relation` at
  `[(keypath *relation-id) MAP-VALS]` with `{:allow-yield? true}`. Status
  history is human-paced but unbounded by contract.
- Return through `|origin`.

Read cost examples:

- Malformed id -> 0 PState reads if rejected before partitioning.
- Valid-shaped missing id -> 1 point read. This is meaningful because Rama
  `keypath` on an absent key emits nil as the absence proof; no empty history
  range read is issued.
- Existing relation with 1 status event -> 2 seeks, 1 history iteration.
- Existing relation with 20 status events -> 2 seeks, 20 history iterations.

Fixed or variable: variable. Valid-shaped missing ids need one meaningful point
read; existing ids need one point read plus one history range read. History
iteration count is variable.

Dynamic approach: conditionally emit the history range read only if the current
row point read returns non-nil. The history range is one subindexed range scan.

### V1. Decision/audit inspection for tests only

Product consumers use only R1 and R2. IPC tests may inspect audit PStates
directly to prove idempotency replay and rejected-decision durability.

Allowed validation-only direct reads:

- `$$relation-decisions-by-idempotency`, by relation id plus idempotency key.
- `$$relation-decisions-by-id`, by decision id.
- `$$relation-events-by-id`, by event id, for accepted decisions only.
- `$$relations-by-id`, `$$relations-by-target`, and
  `$$relation-status-log-by-relation`, to prove no invalid request wrote
  outside the decision/audit spine.
- `$$relation-target-descriptors`, to prove invalid requests and duplicate
  idempotency-key replays did not update R1 read descriptors.

These are not product APIs and must not be wired into Electric server,
trail-view projection code, or agent context builders.

## Writes

### W1. `:relation/assert`

Depot: `*relation-request-depot`

Event type (on the wire): the CONTRACT §4 request **map envelope**, a plain map
with namespaced keys — NOT a defrecord. This is the F1 fix and the verified
codebase idiom (`object_container.clj:1657-1683`, `space.clj:171`):

```clojure
{:relation/routing-key <relation-id>      ; = the deterministic relation-id
 :request/id           <request-id>
 :request/type         :relation/assert
 :idempotency/key      <idempotency-key>
 :actor                <actor>
 :payload              <RelationMutationPayload>}  ; typed payload, see below
```

The depot partitioner `(hash-by :relation/routing-key)` reads the top-level
namespaced key `:relation/routing-key` off this map, so the request lands on
`hash(relation-id)`. The topology then reads envelope keys via accessor fns and
constructs the typed PState-stored defrecords (`RelationEdgeRow`,
`RelationDecisionRow`, `RelationEventRow`, `RelationStatusLogRow`) — the
map→defrecord conversion happens inside the topology only, mirroring
`object_container.clj:383 request-row`. `:payload` may carry a typed
`RelationMutationPayload` (with `RelationTargetRef` `from`/`to`) to honor the
typed-defrecord style gate; the partitioner never reads inside `:payload`, so
its shape does not affect routing.

Client/test preparation:

- Compute deterministic `relation-id` from relation kind, from kind/id, to
  kind/id, and `asserter-actor-id`, per the contract identity rule.
- Set `:relation/routing-key` to `relation-id` (the map key the partitioner
  reads — this is what F1 got wrong).
- Derive `from.target-key` and `to.target-key` before append using the
  contract's target-kind rules. The implementation may expose helper fns, but
  it must not invent UUID relation ids.

Topology behavior:

- Look up the idempotency journal on the relation-id task at
  `[relation-id idempotency-key]`.
- If a journal entry exists, replay that stored decision and stop. Do not compare
  request material, do not write a new conflict decision, and do not touch
  relation truth. This follows the contract's duplicate-key rule exactly.
- Validate required fields, registered relation kind, target shape, and unary
  `:none` rules. Do not check target existence.
- New valid assertion creates the authoritative row, status log entry, accepted
  decision, accepted event, target index copies, and target descriptors.
- Existing asserted row from the same asserter with a new idempotency key is an
  accepted reassertion: write the new accepted decision, accepted event, status
  log row, authoritative row with bumped `status-changed-at-ms`, and full target
  row copies at the same target sort keys. This preserves one relation identity
  and one target-visible membership per endpoint while making reassertion
  visible in `relation-detail`.
- Existing retracted row from the same asserter transitions back to
  `:asserted` on the same relation id and rewrites every full-row copy in the
  same microbatch transaction. Target descriptor asserted counts increment from
  0 to 1 for each endpoint prefix.
- Same kind/from/to from a different asserter computes a different relation id
  and is a separate row.

Latency: microbatch latency is acceptable. Callers needing read-after-write in
tests must append with append durability and wait for the microbatch processed
count before reading.

### W2. `:relation/retract`

Depot: `*relation-request-depot`

Event type (on the wire): the same CONTRACT §4 request **map envelope** as W1,
with `:request/type :relation/retract` (map, not defrecord — F1 fix).

Client/test preparation:

- Use the same deterministic `relation-id` and target refs as the original
  relation identity.
- Set `:relation/routing-key` to `relation-id`.

Topology behavior:

- Duplicate idempotency key under the same relation id replays the stored
  decision and stops, regardless of request material hash.
- Missing relation -> rejected decision only.
- Actor mismatch with `asserter-actor-id` -> rejected decision only; no
  relation row, status history, target index, or descriptor writes.
- Existing relation with matching asserter transitions to `:retracted` and
  rewrites `$$relations-by-id`, `$$relation-status-log-by-relation`, and all
  target index copies in the same microbatch transaction. Target descriptor
  asserted counts decrement from 1 to 0 for each endpoint prefix; total counts
  remain unchanged because retraction is not deletion.
- Repeated valid retract when already retracted is an accepted status
  affirmation: write the new accepted decision, accepted event, status log row,
  authoritative row with bumped `status-changed-at-ms`, and full target row
  copies at the same target sort keys. Descriptor counts do not change because
  the relation was already retracted.

Latency: microbatch latency is acceptable.

## PState Design

All PState schemas are concrete. No PState schema uses `Object`.

Record/interface types to define in `relation_kernel.clj` during Phase 3:

```clojure
(definterface IRelationRequestPayload)

(defrecord RelationTargetRef
  [target-kind target-id target-key])

(defrecord RelationMutationPayload
  [relation-id relation-kind from to asserter-actor-id asserter-type
   evidence-source-id evidence-anchor-id note asserted-at-ms])

;; NOTE (F1): there is deliberately NO `RelationRequestRow` defrecord. The
;; depot request is the CONTRACT §4 map envelope (namespaced keys), exactly as
;; every existing kernel appends a map (object_container.clj:1657-1686,
;; space.clj:171). A defrecord cannot carry a namespaced-keyword field like
;; `:relation/routing-key`, so a defrecord request could not be routed by
;; `(hash-by :relation/routing-key)`. The relation kernel also does NOT store
;; raw requests in a PState (unlike object-container's `$$requests-by-audit-id`);
;; CONTRACT §6 lists no requests PState — audit is via decisions + events.

(defrecord RelationDecisionRow
  [decision-id relation-id request-id request-type idempotency-key status reason
   errors event-id decided-at-ms replayed-from-decision-id request-material-hash])

(defrecord RelationEventRow
  [event-id relation-id event-type relation-status relation-kind from to
   asserter-actor-id asserter-type evidence-source-id evidence-anchor-id note
   event-time-ms request-id decision-id previous-status])

(defrecord RelationEdgeRow
  [relation-id relation-kind from to asserter-actor-id asserter-type
   relation-status evidence-source-id evidence-anchor-id note
   first-asserted-at-ms status-changed-at-ms event-id request-id])

(defrecord RelationStatusLogRow
  [order-key event-id relation-id relation-status changed-at-ms request-id
   decision-id previous-status])

(defrecord RelationTargetDescriptorRow
  [descriptor-key target-key direction relation-kind asserted-count total-count
   updated-at-ms])
```

`RelationMutationPayload` implements `IRelationRequestPayload`. If Phase 3
needs separate assert/retract payload records, they should both implement that
interface rather than using `Object`.

### `$$relation-decisions-by-idempotency`

Chosen schema:

```clojure
{String (map-schema String RelationDecisionRow {:subindex? true})}
;; relation-id -> idempotency-key -> decision
```

Why this refines the contract shape: CONTRACT.md listed the journal as
`{String Object}` while also requiring the journal gate to be colocated with the
relation-id task. A flat top-level idempotency-key map would hash by
idempotency key, not relation id, and would violate the colocated journal gate.
This nested shape preserves the contract behavior while making the colocation
claim true.

Option A: relation-id -> idempotency-key -> decision.

- Assert/retract duplicate check: 1 local seek on the ingress relation-id task.
- Same-relation retries serialize naturally with the authoritative row.
- Duplicate journal entries replay the stored decision and stop regardless of
  request material. `request-material-hash` remains an audit/debug field, not a
  conflict policy in this contract.
- Inner map is subindexed because import runs can generate many idempotency
  keys for the same relation over time.

Option B: idempotency-key -> decision, default partitioning.

- Duplicate check: 1 seek, but on hash(idempotency-key), requiring a partition
  hop before returning to relation-id.
- Breaks the contract sentence that request, journal, decision, and
  authoritative row colocate on relation id.

Chosen: Option A.

### `$$relation-decisions-by-id`

Chosen schema:

```clojure
{String RelationDecisionRow}
;; decision-id -> decision
```

Partitioning: key-partitioner extracts relation id from decision id. Decision
ids must be formatted so relation id is recoverable, for example
`rel:<hash>/decision/<request-id>`.

Why obvious: validation and audit need point lookup by decision id. The value
is one typed row. A nested map by relation id would make tests and debugging
perform an extra relation-id derivation step and would not reduce write cost.

Cost: 1 point seek.

### `$$relation-events-by-id`

Chosen schema:

```clojure
{String RelationEventRow}
;; event-id -> event
```

Partitioning: key-partitioner extracts relation id from event id. Event ids
must be formatted so relation id is recoverable, for example
`rel:<hash>/event/<fixed-width-order-key>`.

Rejected decisions do not write relation events. Relation events mean accepted
world-truth transitions only.

Cost: 1 point seek for validation/debugging.

### `$$relations-by-id`

Chosen schema:

```clojure
{String RelationEdgeRow}
;; relation-id -> current authoritative row
```

Why obvious: relation id is deterministic, request routing is relation id, and
R2 needs one current row by relation id.

Cost:

- Write fold current row: 1 local point read before deciding transition.
- R2 current row: 1 point seek.

### `$$relation-status-log-by-relation`

Chosen schema:

```clojure
{String (map-schema String RelationStatusLogRow {:subindex? true})}
;; relation-id -> order-key -> status log row
```

Option A: subindexed map by relation id.

- R2 detail: 1 seek plus H sequential iterations for H status events.
- Supports stable chronological order and future pagination.

Option B: vector or non-subindexed map inside `RelationEdgeRow`.

- R2 detail: entire history loaded as one serialized value.
- Bad if a relation is repeatedly corrected/reasserted over time.

Chosen: Option A.

### `$$relations-by-target`

Chosen schema:

```clojure
{String (map-schema String RelationEdgeRow {:subindex? true})}
;; target-key -> target-sort-key -> full relation row copy
```

Sort key:

```text
<direction>:<relation-kind>:<fixed-width-order-key(first-asserted-at-ms, relation-id)>:<relation-id>
```

`direction` is `o` for the from-side copy and `i` for the to-side copy.
For unary `:none`, `to.target-key` inherits `from.target-key`; the write may
land both direction entries under the same target key, and R1 deduplicates the
returned row by relation id for that target.

Option A: full row copies by target.

- R1 unfiltered read for T targets with E total rows: T seeks plus E sequential
  iterations.
- R1 single-kind read: up to `T x 2` prefix seeks plus matching sequential
  iterations.
- Status changes rewrite all full row copies in the same microbatch
  transaction, so endpoint reads cannot disagree after commit.

Option B: target index stores relation ids only, then query reads
`$$relations-by-id` per relation.

- R1 for 30 visible edges: 1 target seek plus 30 point seeks, roughly 15ms of
  seek cost before CPU/serialization.
- Cross-partition relation-id fetches add fan-out and aggregation complexity.

Option C: separate outgoing and incoming PStates.

- Fewer prefix ranges for direction-specific reads, but product read R1 always
  needs both directions.
- More PStates and conditional query code with no dominant-read benefit.

Chosen: Option A, matching the contract's denormalization rationale.

### `$$relation-target-descriptors`

Chosen schema:

```clojure
{String (map-schema String RelationTargetDescriptorRow)}
;; target-key -> descriptor-key -> descriptor row
;; descriptor-key = <direction>:<relation-kind>
```

Purpose: descriptor-gate R1 so filtered and empty-target queries do not issue
known-empty `$$relations-by-target` range seeks.

Why this does not replace `$$relations-by-target`: descriptors answer only
"which target/direction/kind prefixes have rows for this include-retracted
mode?" The full row copies remain the product read shape.

Counts:

- `total-count`: number of relation identities under this target/direction/kind
  prefix, including retracted rows.
- `asserted-count`: number of currently asserted relation identities under the
  prefix.

The descriptor inner map is intentionally not subindexed. Its size is bounded by
the code-level relation kind registry times two directions; with the starter
registry this is 14 rows per target, and adding relation kinds is a reviewed code
change. Reading the whole descriptor map is cheaper and simpler than subindexed
point reads for each possible prefix.

Update rules:

- New assertion: create/increment descriptor rows for the from and to endpoint
  prefixes; increment both `total-count` and `asserted-count`.
- Accepted reassertion while already asserted: update `updated-at-ms` only;
  counts do not change.
- Retraction from asserted to retracted: decrement `asserted-count`; do not
  decrement `total-count`.
- Reassertion from retracted to asserted: increment `asserted-count`; do not
  increment `total-count`.
- Repeated retract while already retracted: update `updated-at-ms` only; counts
  do not change.
- Rejected requests and duplicate idempotency-key replays do not touch
  descriptors.

Cost:

- R1 first read per target: 1 bounded descriptor point read. It is meaningful
  even when nil because it proves target absence without navigating an empty
  unbounded range.
- Accepted status event write: O(2) bounded descriptor updates, one per endpoint
  direction. Unary `:none` may update two descriptor keys under the same target
  key but still touches only that one target partition.

## Depots

```clojure
*relation-request-depot: (hash-by :relation/routing-key)
```

Appended record: the CONTRACT §4 request **map envelope** (namespaced keys). The
partitioner reads the top-level `:relation/routing-key` (= relation-id) that the
map literally carries. This is the F1 fix: the depot record must be a map, not a
defrecord, because `(hash-by :relation/routing-key)` on a defrecord whose field
is `relation-routing-key` would read the absent namespaced key and route on
`nil`.

Event kinds carried in the envelope (`:request/type`):

- `:relation/assert`
- `:relation/retract`

One depot is correct because assert and retract are order-dependent for the
same relation identity. Partitioning by relation id colocates the request with
the authoritative row, idempotency journal, decision rows, event rows, and
status history. Target indexes are secondary write shapes and are reached by
explicit `|hash` hops inside the microbatch transaction.

Partition-provenance test (carried to Phase 5, per PLAN_VALIDATION F1): append
a request and prove the idempotency journal and the `$$relations-by-id` row are
readable from `hash(relation-id)` — i.e. that the envelope actually routed by
relation-id, not to a `nil`/accidental partition.

Ack level:

- Runtime helpers should default to append durability (`:append-ack` or the
  local convention used by tests), then tests wait for the microbatch processed
  count before reading.
- Do not promise read-after-write from depot ack alone. The microbatch
  reference explicitly says depot ack does not imply PState visibility.

No internal depots are needed in this slice.

## Topologies and PStates

### `relation-kernel-topology`

Type: microbatch.

Why: relation writes update the authoritative row plus target indexes that can
live on up to three tasks. Microbatch gives exactly-once PState updates and
cross-partition atomicity; single-digit millisecond latency and append ack
coordination are not required.

Owns:

```clojure
$$relation-decisions-by-idempotency
  {String (map-schema String RelationDecisionRow {:subindex? true})}

$$relation-decisions-by-id
  {String RelationDecisionRow}
  {:key-partitioner relation-derived-key-partitioner}

$$relation-events-by-id
  {String RelationEventRow}
  {:key-partitioner relation-derived-key-partitioner}

$$relations-by-id
  {String RelationEdgeRow}

$$relation-status-log-by-relation
  {String (map-schema String RelationStatusLogRow {:subindex? true})}

$$relations-by-target
  {String (map-schema String RelationEdgeRow {:subindex? true})}

$$relation-target-descriptors
  {String (map-schema String RelationTargetDescriptorRow)}
```

Processing outline:

1. Source `%microbatch` from `*relation-request-depot`.
2. Emit each request envelope (a map). It starts on hash(relation-id) because
   the depot hashes by `:relation/routing-key`, which the map envelope carries
   (F1). Read `:request/type`, `:idempotency/key`, `:actor`, and `:payload` off
   the map via accessor fns; construct typed rows for PState writes only.
3. Read `$$relation-decisions-by-idempotency` at
   `[relation-id idempotency-key]` — a colocated local read on the relation-id
   task (see the idempotency-scope note in Design Decisions; this is why the
   depot MUST route by relation-id).
4. If present, stop relation-truth processing for that request and replay the
   prior decision. Do not compare request material and do not write a new
   conflict decision.
5. Validate request shape, relation kind registry, target refs, unary `:none`,
   and retract rights. Never perform target foreign-key validation.
6. Rejected request: write `RelationDecisionRow` to both decision indexes; no
   relation event, status log, authoritative row, target index, or descriptor
   write.
7. Accepted request: write decision, event, authoritative row, status log row,
   all target index copies with `termval` full rows, and descriptor count/touch
   updates in the same microbatch transaction. This includes accepted
   reassertions and repeated valid retractions; they are not decision-only
   no-ops.
8. Duplicate idempotency-key replay: write nothing and leave all PStates
   unchanged.

No stream topology is needed. There are no stream retries to reason about.

### Query topologies

Declared at module body level and read the microbatch-owned PStates.

- `relations-for-targets`: descriptor-gated dynamic fan-out query described in
  R1.
- `relation-detail`: nil-safe point read plus conditional history read
  described in R2.

No query topology writes to user PStates.

## Query Topologies

### `relations-for-targets`

Input example 1:

```clojure
[] #{} false
```

Reads: 0 total, 0 meaningful. Result `{}`.

Input example 2:

```clojure
["doc-A"] #{} false
```

Case A, no visible rows: 1 descriptor point read, 0 target range reads.
Meaningful reads: 1, because the nil/zero descriptor result proves absence.

Case B, visible rows present: 1 descriptor point read + 1 target range read.
Meaningful reads: 2.

Input example 3:

```clojure
["doc-A" "doc-B"] #{:based-on} false
```

If descriptors show only `doc-A/o/:based-on` has asserted rows: 2 descriptor
point reads + 1 prefix range read. Meaningful reads: 3. The missing doc-A
incoming prefix and both doc-B prefixes are not read.

Input example 4:

```clojure
["doc-A" "doc-B"] #{:based-on :produced} true
```

If descriptors show `doc-A/o/:based-on`, `doc-A/i/:produced`, and
`doc-B/i/:produced` have total rows: 2 descriptor point reads + 3 prefix range
reads. Meaningful reads: 5. The other possible prefixes are not read.

Fixed or variable: variable.

Dynamic approach:

- Normalize target keys and kind filters.
- `ops/explode` the target keys.
- For each target, `|hash` target key and point-read the bounded descriptor map.
- For no kind filter, generate one `:all` range-read descriptor only when the
  descriptor summary has a nonzero `asserted-count` or `total-count` according
  to `*include-retracted?`.
- For kind filters, generate direction/kind prefix descriptors only from
  nonzero descriptor counts for the requested status mode.
- For each emitted range descriptor, perform one local subindexed range read on
  `$$relations-by-target` with `{:allow-yield? true}`.
- `|origin`, aggregate rows by target key, filter statuses, and deduplicate
  `[target-key relation-id]`.

### `relation-detail`

Input example 1:

```clojure
"rel:abc"
```

Reads: 2 total local reads: current row and status log range. Meaningful reads
are 2 when the relation exists.

Input example 2:

```clojure
"rel:missing"
```

Reads: 1 total local point read. Meaningful reads: 1, because `keypath` emits
nil for the absent key and proves absence. No status-history range read is
issued.

Fixed or variable: variable by relation existence; history iteration is
variable.

Dynamic approach: after the point read, conditionally emit the status-history
range read only when the row exists. Return through `|origin`.

## Design Decisions

- Module boundary: new `relation-kernel-module` in new namespace
  `app.server.rama.relation-kernel`; no edits to existing kernels.
- Existing helper dependency: require only plain helper fns from
  `app.server.rama.object-container`: `extract-object-key`,
  `fixed-width-order-key`, and `actor-row`.
- Relation id: deterministic content identity per contract. Phase 3 should
  implement the exact identity helper in the new namespace and keep callers and
  tests on the same helper.
- Kind registry: code-level set exactly:
  `#{:based-on :produced :built-over :new-direction :dead-end :elaborates :references}`.
  Unknown relation kinds reject durably.
- Target key derivation: contract rules; unknown target kinds fall back to
  target id verbatim and do not reject.
- No target FK validation: dangling targets are legal relation assertions.
- No deletion: retraction is a status transition, never removal.
- Accepted vs rejected: rejected requests write decision rows only; accepted
  requests write event/status/relation/index rows, including accepted
  reassertions and repeated valid retractions.
- Idempotency replay mechanics (not scope): duplicate idempotency keys under the
  same relation id replay the stored decision and stop, regardless of request
  material. There is no separate idempotency-conflict decision in this contract.
- Idempotency **scope** — `(relation-id, idempotency-key)`, **RULED relation-scoped
  by Fable 2026-07-03** (CONTRACT §5 as amended; `decisions.md` CLOSED). The
  relation-scoped nested journal the plan already carries is the ruled design. See
  "## Idempotency scope (RESOLVED)" below. Cross-relation key reuse is now
  CONTRACT §11 gate 11 and must be tested in Phase 5.
- Accepted affirmations: a new idempotency key for an already-current status is
  accepted history, not a decision-only no-op. It writes an accepted event,
  status log row, bumped current row, and target row copies at the same sort
  keys. Descriptor counts change only when the asserted/retracted count changes.
- Subindexing:
  - `$$relation-decisions-by-idempotency` inner map is subindexed because
    import replays can issue many request ids/idempotency keys for one
    relation.
  - `$$relation-status-log-by-relation` inner map is subindexed because status
    history is unbounded.
  - `$$relations-by-target` inner map is subindexed because a popular
    document/commit/conversation can accumulate unbounded relations.
  - `$$relation-target-descriptors` inner map is not subindexed because its
    maximum size is bounded by `2 x registered relation kinds`, enforced by the
    code-level kind registry.
- Colocation:
  - Depot partition key is relation id.
  - Authoritative relation row, idempotency journal, decision row, event row,
    and status log are colocated by relation id.
  - Target index and descriptor writes explicitly hop to endpoint target keys.
- Query boundary: product consumers use only `relations-for-targets` and
  `relation-detail`; test-only PState inspection must stay in tests.
- Cooperative multitasking: all unbounded local range reads use
  `{:allow-yield? true}`. No long synchronous loops are planned.
- Worker restart: no TaskGlobal or in-memory cache exists in this design, so
  there is no non-durable rebuild path to specify.
- Write volume: per accepted transition is bounded by request shape:
  decision journal + decision id + event + status + one authoritative row + two
  target-index writes + two bounded descriptor updates. Unary `:none` touches
  one target key, though it may use both direction entries under that key.

## Idempotency scope (RESOLVED — Fable ruled relation-scoped, 2026-07-03)

**Outcome: relation-scoped `(relation-id, idempotency-key)`.** This was
PLAN_VALIDATION **F2**, unresolved across two validation rounds. Per the stop
clause it was referred (not decided by the implementing session) to Sid, who
referred it to Fable; Fable ruled option (A) and amended the binding docs
(CONTRACT §5 scope ruling + §11 gate 11; IMPLICIT_SPEC banner; `decisions.md`
CLOSED). Per D-006 this is also an evaluation result: implementation contact
surfaced a scope ambiguity between the contract and the derived implicit spec,
resolved by an explicit amendment rather than a silent narrowing. The analysis
below is retained as the rationale of record.

**The conflict.** Two binding sources disagree on whether an idempotency key is
unique per relation or globally:

- CONTRACT §5 step 2 says the journal gate is *"copied from object-container's
  decisions-by-idempotency pattern"* and §4 says request, journal, decision, and
  authoritative row *"all colocate on one task"* keyed by relation-id.
- IMPLICIT_SPEC models `IdempotencyKey` as its own state entity and says "the
  same idempotency key replays the first decision" / "concurrent assertions with
  the same idempotency key collapse to one decision" — which the validator read
  as *global* uniqueness across relation-ids.

**Why these cannot both be literally true in Rama.** A colocated journal on the
relation-id task is physically relation-scoped: the entry for `rel:A` lives on
`hash(A)`, the entry for `rel:B` on `hash(B)`. A *global* idempotency guarantee
(same key across different relations collapses to one decision) requires an index
partitioned by `hash(idempotency-key)` — a different task from either relation.
You cannot have both the contract's colocation and global-key uniqueness in one
PState.

**Recommendation (high confidence): confirm relation-scoped `(relation-id,
idempotency-key)`; treat the IMPLICIT_SPEC's global framing as drift to
reconcile.** Grounds:

1. The CONTRACT explicitly points the journal at object-container's pattern.
   That pattern, verified in source, is colocation-key-scoped:
   `$$decisions-by-idempotency {String (map-schema String … {:subindex? true})}`
   read at `[(keypath *partition-key *idempotency-key)]`
   (`object_container.clj:1693-1694`, `:1786`). Relation-scope is a one-to-one
   copy of the hardened idiom the contract names.
2. Global collapse would be *silently wrong*: two clients/imports reusing a key
   for two different relations would drop one relation (data loss), violating the
   exactness rule ("the map must not lie"). The idempotency key exists for
   client append-retry safety, which is always same-relation.
3. IMPLICIT_SPEC is a Phase-0 artifact that "treats the contract as binding"; on
   a genuine conflict the CONTRACT governs. Its `IdempotencyKey` entity is not
   wrong — a per-relation key still has an `unused → used-*` lifecycle — it just
   needs its scope stated.

**Ruling: option (A), relation-scoped — CHOSEN.** No design change to this plan
(the nested `relation-id → idempotency-key → decision` journal was already the
relation-scoped shape). Testable consequence formalized as **CONTRACT §11 gate
11 (cross-relation key reuse)**: the same idempotency-key submitted for two
different relations → both relations succeed independently (two ids, two journal
entries, nothing collapses). Phase 5 must include this gate.

The rejected option (B, global uniqueness) would have required amending CONTRACT
§4-5 colocation and re-running Phase 1 to add a `hash(idempotency-key)` index +
a partition hop before the relation-id fold — larger, and contradicting the
contract's stated colocation.

## State primitive selection

- `$$relation-decisions-by-idempotency` (PState): durable retry journal.
  Per-source-event write volume O(1), bounded by one idempotency key in the
  request.
- `$$relation-decisions-by-id` (PState): durable audit by decision id.
  Per-source-event write volume O(1), bounded by one decision per request.
- `$$relation-events-by-id` (PState): durable accepted-transition audit.
  Per-source-event write volume O(1), bounded by at most one event per accepted
  truth transition.
- `$$relations-by-id` (PState): authoritative current relation row.
  Per-source-event write volume O(1), bounded by one relation id in the
  request.
- `$$relation-status-log-by-relation` (PState): durable status history.
  Per-source-event write volume O(1), bounded by at most one status log row per
  accepted truth transition.
- `$$relations-by-target` (PState): durable denormalized read index.
  Per-source-event write volume O(2), bounded by the request's from/to target
  refs.
- `$$relation-target-descriptors` (PState): durable bounded read-descriptor
  index for R1. Per-source-event write volume O(2), bounded by the request's
  from/to target refs; per-target descriptor map size is bounded by the
  code-level relation kind registry.
- TaskGlobal: none.
- External system: none.

## Phase 2 Validation Checklist

**Gating note:** both blockers are resolved — F1 fixed in-plan, F2 ruled
relation-scoped and the binding docs amended. This plan is **ready for Phase 2
re-validation**.

The next validation should specifically challenge:

- **F1 (fixed) — verify:** the depot record is the map envelope with
  `:relation/routing-key`, no `RelationRequestRow` defrecord is on the wire, and
  the partition-provenance test proves journal + `$$relations-by-id` are readable
  from `hash(relation-id)`.
- **F2 (ruled relation-scoped) — verify alignment:** the plan's nested
  `relation-id → idempotency-key → decision` journal matches CONTRACT §5 as
  amended, and the plan carries CONTRACT §11 gate 11 (cross-relation key reuse)
  into Phase 5. Confirm no global `hash(idempotency-key)` index crept in.
- Whether duplicate idempotency-key replay now exactly matches the contract:
  stored decision replay and no new decision/event/status/index/descriptor
  writes, even if request material differs.
- Whether accepted reassertions and repeated valid retractions now satisfy the
  required status/history evidence without creating duplicate target-visible
  memberships.
- Whether unary `:none` should store both direction entries under one target
  key with query dedupe, or only an outgoing entry. This plan chooses the
  contract-compatible two-direction write plus R1 dedupe.
- Whether `$$relation-target-descriptors` fully eliminates known-empty target
  and prefix range seeks under the query-topology validation template without
  making write volume unbounded.
- Whether `relation-detail` correctly treats a nil point-read as a meaningful
  absence proof and skips the status-history range read for missing relation ids.
- Whether `RelationMutationPayload` as one typed payload is enough, or whether
  separate assert/retract payload defrecords are clearer while keeping the
  common `IRelationRequestPayload` interface.
