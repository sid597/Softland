# Implicit Spec - RelationEdge kernel

<!-- Phase 0. Requirements analysis only - NO additional PState/depot/topology design. -->

Source specs:

- `docs/current-mental-model/decisions.md` D-001 through D-006.
- `docs/current-mental-model/build/relation-kernel/CONTRACT.md`.
- `docs/sessions/next-prompt.md` handoff, written 2026-07-03.

**Amendment 2026-07-03 (Fable ruling on PLAN_VALIDATION F2 — binding):**
idempotency is **relation-scoped**. Everywhere this spec says "the same
idempotency key," read "the same `(relation-id, idempotency-key)` pair." The
`IdempotencyKey` entity and its state machine are scoped within one relation's
journal on that relation's task. Global uniqueness across relations is
explicitly NOT provided: two requests reusing a key on *different* relations
are independent and both succeed (collapsing them would silently drop an
assertion — exactness rule). Authority: CONTRACT.md §5 as amended +
decisions.md Open Questions ruling.

This artifact treats the contract as binding. It does not reopen closed
decisions and does not redesign the module. It makes explicit the behavior the
implementation and tests must preserve.

## Slice scope (committed)

**In:** a new `relation-kernel-module` that owns typed, provenance-carrying
`RelationEdge` assertions; deterministic relation identity; registered relation
kinds; durable accepted and rejected decisions; status history; legal dangling
targets; unary `:dead-end` style marks; two public read query topologies:
`relations-for-targets` and `relation-detail`.

**Out:** foreign-key validation, confidence scores, credential algebra,
relation-as-container, auto-merge across asserters, cascading behavior on
retraction, deletion, semantic search, transcript-to-commit/doc extraction,
commit metadata import, editing existing kernels, or consumer reads that bypass
the two query topologies.

**Entities:** `RelationTargetRef`, `RelationEdge`, `RelationKind`,
`RelationRequest`, `RelationDecision`, `RelationEvent`,
`RelationStatusHistory`, `IdempotencyKey`, `Asserter`, and the target-facing
relation membership read shape.

**Binding refusals:** dangling targets are accepted; unregistered relation
kinds are rejected; relations never delete; Sid and LLM assertions of the same
edge are different rows because `asserter-actor-id` is part of identity.

## Operations

### W1. `:relation/assert` - assert or reassert a relation

- **Inputs:** request id, idempotency key, actor, relation kind, from target, to
  target, optional evidence source id, optional evidence anchor id, optional
  note, assertion timestamp, and precomputed deterministic `relation-id` used
  as `:relation/routing-key`.
- **Input preparation requirements:** the caller/test helper computes
  `relation-id` from relation kind, from kind/id, to kind/id, and
  asserter-actor-id. The caller/test helper derives each target key from the
  target kind rules in the contract. Implementation must not invent UUID
  relation ids.
- **Latency:** hundreds of milliseconds or seconds are acceptable. This is not
  an interactive text-edit path; the trail view can tolerate seconds-old
  relation truth.
- **Throughput:** driven by transcript/git/doc import volume and occasional
  human/agent assertions. The high-volume case is import replays that emit many
  already-seen logical assertions. Those must converge instead of accreting
  duplicates.
- **Consistency invariants:**
  - A valid first assertion creates exactly one logical `RelationEdge` identity.
  - The same idempotency key replays the first decision and creates no new
    relation event, status transition, or target-visible duplicate.
  - The same logical assertion with a different idempotency key uses the same
    `relation-id`; it may add audit/status history for a reassertion, but must
    not create a second relation row or second target membership copy.
  - Status after a successful assert is `:asserted`.
  - If the row was `:retracted`, assertion by the same asserter reasserts the
    same identity; it does not create a replacement id.
  - The row returned from either endpoint and by id must agree on status,
    asserter, evidence, note, timestamps, event id, and request id.
  - Assertions from different asserters create separate relation ids, even when
    kind/from/to are identical.
  - Target existence is not checked. A relation is an assertion about an
    address, not a claim that the addressed object is already imported.
- **Data growth and scale:** relation rows and status history grow with distinct
  asserted identities plus reassert/retract transitions. Target-facing reads can
  be unbounded because a document/commit/conversation can accumulate many
  incoming and outgoing relations. Dominant read access is "all relations
  touching these visible target keys", optionally narrowed by kind and status.
- **Concurrency:** concurrent assertions with the same idempotency key collapse
  to one decision. Concurrent assertions with different idempotency keys but the
  same deterministic relation id serialize to one final row and no duplicate
  target-visible relation. Concurrent assertions from different asserters are
  intentionally distinct.
- **Edge cases:**
  - Empty or missing request id, idempotency key, actor, relation kind, from
    target, or required target fields: reject durably.
  - Unregistered relation kind: reject durably and do not materialize a
    relation row or target-visible row.
  - Dangling target id: accept if the target ref is well-formed.
  - Unknown target kind: accept if well-formed; target key is derived by the
    contract fallback. Unknown target kind is not the same as unknown relation
    kind.
  - Unary mark: `to.target-kind = :none`, `to.target-id = nil`, and
    `to.target-key` inherits the from target key. It is discoverable from the
    from side and must not create a global nil-key hotspot.
  - Non-unary `:none` on the from side or a `:none` target carrying its own
    unrelated id/key: reject as malformed.
  - Optional evidence ids and note may be nil. Nil evidence does not make an
    assertion invalid.
  - Notes are short free text. The contract does not require search or
    full-text indexing of notes.

### W2. `:relation/retract` - retract a relation without deleting it

- **Inputs:** request id, idempotency key, actor, relation kind, from target, to
  target, asserter identity, timestamp, and deterministic `relation-id` for the
  relation being retracted.
- **Latency:** same as assertion: hundreds of milliseconds or seconds are
  acceptable.
- **Throughput:** human/agent paced, plus possible batch correction of imported
  assertions. Volume is lower than assertion volume but must be retry-safe.
- **Consistency invariants:**
  - Retraction is a status transition, never deletion.
  - Only the original asserter-actor-id can retract the relation identity.
  - A successful retract changes the row status to `:retracted` and records
    history. It does not remove the relation from detail history.
  - A failed retract by the wrong actor records a rejected decision and leaves
    the relation row, target-visible copies, and status history unchanged.
  - Repeated retract with the same idempotency key replays the first decision
    and creates no new status event.
  - Repeated retract by the original asserter with a new idempotency key must
    not create duplicate relation rows. It may record another no-op status
    event only if all target-visible copies remain a single retracted row.
  - `relations-for-targets` excludes retracted rows by default and includes
    them only when `include-retracted?` is true.
- **Data growth and scale:** retractions add status/audit history but do not
  delete or compact old assertion evidence. A relation with many status changes
  must remain detail-readable in order.
- **Concurrency:** an assert and retract for the same relation identity must
  serialize to one final status according to processed order. Read paths must
  never disagree by endpoint after either transition is visible.
- **Edge cases:**
  - Retract a non-existent relation: reject durably; public relation reads stay
    empty.
  - Retract using an unregistered relation kind: reject durably.
  - Retract a dangling-target relation: allowed if the relation exists and actor
    rights match; target existence still is not checked.
  - Retract unary `:none` relation: allowed if the identity and actor match.
  - Missing actor or missing asserter identity: reject durably.

### R1. `relations-for-targets` - batch read relations touching targets

- **Inputs:** target keys, kinds filter, and `include-retracted?`.
- **Latency:** should be one clustered query invocation for the whole visible
  trail/DAG region, not one query per node. Interactive view rendering needs a
  predictable batch read, not scattered client-side selects.
- **Throughput:** driven by trail view renders and agent context bundle
  assembly. Calls may ask for tens or hundreds of visible target keys at once.
- **Correctness invariants:**
  - Returns a map keyed by requested target key.
  - Each requested target key returns all matching relations that touch it, both
    outgoing and incoming, except retracted rows are hidden unless requested.
  - Kind filtering must not leak other kinds.
  - Multiple asserters for the same kind/from/to are returned as separate rows.
  - Dangling targets are returned normally from the key they address.
  - Unary `:none` marks are returned from the from-side target key; there is no
    separate nil-key target to query.
  - Empty target-key input returns an empty map, not an error.
  - Unknown target keys return empty vectors/maps for those keys, not an error.
- **Data growth and scale:** per-target relation membership is unbounded, so
  range reads must be yield-safe in implementation. Pagination is not required
  by the contract, but ordering must be stable enough that pagination can be
  added later without changing relation identity.
- **Concurrency:** a query may run while writes are being processed, but any
  visible row must be internally consistent. The same relation must not appear
  with different statuses depending on whether it is read from the from or to
  target.
- **Edge cases:** duplicate target keys in input should not duplicate returned
  rows; nil target keys are malformed input except the internal unary inheritance
  rule, and should produce empty/rejected-query behavior rather than a global
  scan.

### R2. `relation-detail` - read one relation and its status history

- **Inputs:** relation id.
- **Latency:** one clustered query invocation and a bounded number of reads for
  a single relation.
- **Throughput:** lower than target batch reads; used when inspecting why an
  edge exists or validating status history.
- **Correctness invariants:**
  - Existing relation id returns the current row plus status history in order.
  - Missing relation id returns no row and empty history.
  - Rejected decisions that never created or changed a relation do not appear as
    status history unless the implementation explicitly models rejected
    decisions separately from relation status events. Public detail must not
    imply a rejected request changed relation truth.
  - Retraction leaves row and history readable forever.
  - Evidence ids, note, asserter identity, request id, and event id are visible
    on the row/history needed to answer "why does this edge exist?"
- **Data growth and scale:** status history is unbounded per relation but human
  paced. Reads must be stable and ordered.
- **Concurrency:** after an assert/retract transition is visible to detail,
  target reads must agree with the same current status.
- **Edge cases:** nil or malformed relation id returns empty/rejected-query
  behavior; it must not scan all relations.

### V1. Decision/audit inspection - validation-only read surface

The contract says product consumers use only `relations-for-targets` and
`relation-detail`. Tests still need to prove rejected decisions are durable and
that invalid requests wrote only the decision/audit spine. Any direct decision
or PState inspection in tests is validation-only and must not become a product
consumer API.

## Cross-cutting requirements

- **Closed decisions:** D-001 through D-006 are not reopened during
  implementation. A contract-breaking flaw stops the thread and is recorded in
  `decisions.md` plus the current phase artifact.
- **Module boundary:** new files only for implementation:
  `src/app/server/rama/relation_kernel.clj` plus a test namespace. Existing
  kernels must not be edited, except the implementation may require plain
  helper functions from `app.server.rama.object-container`.
- **Read boundary:** Electric server code, trail-view projections, agents, and
  later consumers read only through R1/R2.
- **No target FK check:** target existence is a view-resolution concern, not a
  write-time policy.
- **No deletion:** retraction is the terminal negative status for this slice.
- **No source-specific truth island:** transcript/git/doc importers may later
  emit relation requests, but relation truth lives in the relation kernel.
- **Asserter calibration:** LLM/import/system/human claims stay separate in
  storage. Human endorsement is a separate human assertion, not a mutation of
  the LLM row.
- **Kind taxonomy:** the starter registry is `:based-on`, `:produced`,
  `:built-over`, `:new-direction`, `:dead-end`, `:elaborates`, and
  `:references`. Adding a kind is future code review; accepting arbitrary
  keywords is forbidden.
- **Phase 1 verification carry-forward:** before writing the plan, verify the
  contract's two memory-derived Rama claims against
  `.agents/skills/rama/references/` and linked official docs:
  microbatch mid-batch crash replay/transactional write semantics, and the
  query-topology fan-out/aggregate idiom.

## Entity State x Write Matrix

### RelationEdge identity - states: `does-not-exist`, `asserted`, `retracted`

- **`does-not-exist` x valid `:relation/assert`** -> becomes `asserted`.
  - R1 relations-for-targets: returns the row from both endpoint target keys,
    or from the single from-side key for unary `:none`.
  - R2 relation-detail: returns the row plus one assertion status event.
  - V1 decision/audit: returns an accepted decision for the request and
    idempotency key.
- **`does-not-exist` x `:relation/retract`** -> stays absent; rejected.
  - R1: returns no row for either endpoint.
  - R2: returns no row and empty status history for that relation id.
  - V1: returns a rejected decision explaining that the relation did not exist
    or could not be retracted.
- **`asserted` x valid `:relation/assert` by same asserter, new idempotency
  key** -> remains `asserted`; no duplicate row.
  - R1: returns one row per touched target key with status `:asserted`.
  - R2: returns one current row and ordered assertion/reassertion history.
  - V1: returns an accepted decision for the new request.
- **`asserted` x valid `:relation/assert` by different asserter** -> creates a
  different relation identity, because asserter is part of the id.
  - R1: returns both rows for the same kind/from/to grouping.
  - R2: each relation id returns only its own row/history.
  - V1: returns an accepted decision for the new asserter's request.
- **`asserted` x valid `:relation/retract` by original asserter** -> becomes
  `retracted`.
  - R1: excludes the row when `include-retracted?` is false; returns the
    retracted row when true, from every touched target key.
  - R2: returns the row with status `:retracted` and assertion/retraction
    history in order.
  - V1: returns an accepted retraction decision.
- **`asserted` x `:relation/retract` by different actor** -> remains
  `asserted`; rejected.
  - R1: still returns the asserted row from endpoint target keys.
  - R2: current row and status history are unchanged.
  - V1: returns a rejected decision for the unauthorized retract.
- **`retracted` x valid `:relation/assert` by original asserter** -> becomes
  `asserted` again on the same relation id.
  - R1: returns the asserted row by default from every touched target key.
  - R2: returns ordered assert/retract/assert history.
  - V1: returns an accepted decision.
- **`retracted` x valid `:relation/retract` by original asserter, new
  idempotency key** -> remains `retracted`; no duplicate row.
  - R1: still excluded by default and included only when
    `include-retracted?` is true.
  - R2: returns the same current retracted row and ordered history, including
    any recorded no-op status transition if the plan chooses to record one.
  - V1: returns an accepted or no-op accepted decision, but not a second
    relation identity.
- **Any state x duplicate request with the same idempotency key** -> replays
  the first decision for that key.
  - R1: identical to the state after the first request.
  - R2: identical to the state after the first request; no new status event.
  - V1: returns the prior decision.
- **Any state x unregistered kind or malformed target/actor** -> relation truth
  unchanged; rejected.
  - R1: unchanged.
  - R2: unchanged.
  - V1: returns a rejected decision and must show no non-audit materialization
    for the invalid request.

### IdempotencyKey - states: `unused`, `used-accepted`, `used-rejected`

- **`unused` x valid `:relation/assert`** -> `used-accepted`.
  - R1: reflects the accepted/asserted relation.
  - R2: reflects the accepted/asserted relation and history.
  - V1: accepted decision is stored under the idempotency key.
- **`unused` x valid `:relation/retract`** -> `used-accepted` only if the
  relation exists and actor rights match; otherwise `used-rejected`.
  - R1: reflects the accepted status change, or stays unchanged on rejection.
  - R2: reflects the accepted status change, or stays unchanged on rejection.
  - V1: accepted or rejected decision is stored under the idempotency key.
- **`unused` x invalid request** -> `used-rejected`.
  - R1: unchanged.
  - R2: unchanged.
  - V1: rejected decision is stored; no relation truth is materialized.
- **`used-accepted` x any duplicate request using the same key** -> remains
  `used-accepted`.
  - R1: unchanged from the first accepted request.
  - R2: unchanged from the first accepted request.
  - V1: replays the first accepted decision.
- **`used-rejected` x any duplicate request using the same key** -> remains
  `used-rejected`.
  - R1: unchanged from the first rejected request.
  - R2: unchanged from the first rejected request.
  - V1: replays the first rejected decision.

### RelationKind - states: `registered`, `unregistered`

- **`registered` x `:relation/assert`** -> request may proceed to target,
  actor, and identity validation.
  - R1: returns the row if the rest of validation accepts.
  - R2: returns row/history if the rest of validation accepts.
  - V1: accepted or rejected according to later validation.
- **`registered` x `:relation/retract`** -> request may proceed to existence
  and actor-rights validation.
  - R1: reflects the status change only if validation accepts.
  - R2: reflects the status change only if validation accepts.
  - V1: accepted or rejected according to later validation.
- **`unregistered` x `:relation/assert`** -> rejected.
  - R1: no row is created.
  - R2: no row/history is created.
  - V1: rejected decision is durable; this is acceptance gate 6.
- **`unregistered` x `:relation/retract`** -> rejected.
  - R1: unchanged.
  - R2: unchanged.
  - V1: rejected decision is durable.

### RelationTargetRef - states: `well-formed-addressed`, `dangling-addressed`, `unary-none`, `malformed`

- **`well-formed-addressed` x `:relation/assert`** -> accepted if kind and
  actor are valid.
  - R1: relation appears from the addressed target keys.
  - R2: detail returns the row and assertion history.
  - V1: accepted decision.
- **`dangling-addressed` x `:relation/assert`** -> accepted; target existence
  is not checked.
  - R1: relation appears from the derived target key even if no object/source
    row currently exists there.
  - R2: detail returns the row; endpoint resolution is a view concern.
  - V1: accepted decision.
- **`unary-none` x `:relation/assert`** -> accepted only when `to` is `:none`
  and inherits the from target key.
  - R1: relation appears from the from-side target key exactly once.
  - R2: detail returns `to.target-kind = :none`, nil target id, and inherited
    target key.
  - V1: accepted decision.
- **`malformed` x `:relation/assert`** -> rejected.
  - R1: no relation row appears.
  - R2: no relation row/history appears.
  - V1: rejected decision.
- **`well-formed-addressed`, `dangling-addressed`, or `unary-none` x
  `:relation/retract`** -> target existence is not revalidated; relation id and
  actor rights decide the result.
  - R1: reflects the retracted status if accepted; unchanged if rejected.
  - R2: reflects the retracted status if accepted; unchanged if rejected.
  - V1: accepted or rejected decision.
- **`malformed` x `:relation/retract`** -> rejected before changing truth.
  - R1: unchanged.
  - R2: unchanged.
  - V1: rejected decision.

### Asserter - states: `original-asserter`, `different-asserter`, `missing-or-invalid`

- **`original-asserter` x `:relation/assert` for existing row** -> reasserts
  the same identity.
  - R1: one current row for that asserter identity.
  - R2: same relation id, longer status history.
  - V1: accepted decision.
- **`different-asserter` x `:relation/assert` for same kind/from/to** ->
  creates a separate identity.
  - R1: both assertions are returned as separate rows.
  - R2: each detail call is scoped to one asserter's relation id.
  - V1: accepted decision for the different asserter.
- **`missing-or-invalid` x `:relation/assert`** -> rejected.
  - R1: unchanged.
  - R2: unchanged.
  - V1: rejected decision.
- **`original-asserter` x `:relation/retract`** -> accepted if relation exists.
  - R1: hides or returns retracted row according to `include-retracted?`.
  - R2: returns retraction in ordered history.
  - V1: accepted decision.
- **`different-asserter` x `:relation/retract`** -> rejected.
  - R1: unchanged; original row remains in its previous status.
  - R2: unchanged; no retraction history is added.
  - V1: rejected decision.
- **`missing-or-invalid` x `:relation/retract`** -> rejected.
  - R1: unchanged.
  - R2: unchanged.
  - V1: rejected decision.

### Target-facing relation membership - states: `empty`, `has-asserted`, `has-retracted`, `has-multiple-asserters`

- **`empty` x valid `:relation/assert` touching this target** -> becomes
  `has-asserted`.
  - R1: returns the asserted row for this target key.
  - R2: detail for the row agrees with R1.
  - V1: accepted decision.
- **`has-asserted` x valid `:relation/assert` same identity** -> remains
  `has-asserted` with one visible row for that identity.
  - R1: no duplicate row for this target key.
  - R2: detail history may grow; current row agrees with R1.
  - V1: accepted decision or idempotent replay.
- **`has-asserted` x valid `:relation/assert` different asserter** -> becomes
  `has-multiple-asserters`.
  - R1: returns one row per asserter identity.
  - R2: detail remains separated by relation id.
  - V1: accepted decision for the new identity.
- **`has-asserted` x valid `:relation/retract` original asserter** -> becomes
  `has-retracted` for that identity.
  - R1: excludes the retracted identity by default; includes it when requested.
  - R2: current row is retracted and history shows the transition.
  - V1: accepted decision.
- **`has-retracted` x valid `:relation/assert` original asserter** -> becomes
  `has-asserted` again.
  - R1: returns the asserted identity by default.
  - R2: history shows retraction followed by assertion.
  - V1: accepted decision.
- **`has-multiple-asserters` x retract one original asserter** -> mixed state:
  one identity retracted, others unchanged.
  - R1: default query returns only still-asserted identities; with
    `include-retracted?` it returns both asserted and retracted identities.
  - R2: each identity's detail reports only that identity's current status.
  - V1: accepted decision for the retracted identity only.
- **Any membership state x invalid/rejected request** -> unchanged.
  - R1: unchanged.
  - R2: unchanged.
  - V1: rejected decision only.

### RelationEvent and status history - states: `none`, `has-accepted-transition-history`

- **`none` x accepted `:relation/assert`** -> one accepted assertion event.
  - R1: returns asserted row.
  - R2: returns current row and the assertion event.
  - V1: accepted decision.
- **`none` x rejected request** -> remains no relation status history.
  - R1: unchanged/empty.
  - R2: unchanged/empty for the relation id.
  - V1: rejected decision is durable in audit.
- **`has-accepted-transition-history` x accepted `:relation/assert` or
  `:relation/retract`** -> appends an ordered accepted status event for the
  same relation id.
  - R1: reflects only the current status and filters retracted rows as
    requested.
  - R2: returns the full ordered status history.
  - V1: accepted decision for the request.
- **`has-accepted-transition-history` x rejected request** -> relation status
  history unchanged.
  - R1: unchanged.
  - R2: unchanged status history; rejected request must not look like a truth
    transition.
  - V1: rejected decision is durable in audit.

## Acceptance gates carried into later phases

The implementation/test phases must prove all ten contract gates:

1. Assert plus dual read across different partition keys.
2. Idempotent client retry.
3. Convergent re-import by deterministic relation id.
4. Retract consistency across both endpoint reads and detail.
5. Retraction rights.
6. Registry rejection with no non-audit materialization.
7. Dangling target acceptance.
8. Unary mark with no nil-key/global hotspot behavior.
9. Asserter separation.
10. Kind filter correctness.

Style gates to preserve: typed defrecords, imported partition helpers rather
than reimplementation, yield-safe unbounded range reads, and consumers only via
`relations-for-targets` / `relation-detail`.

## Phase 0 result

No contract-breaking issue surfaced in this requirements pass. The main
ambiguities are implementation-level choices for whether repeated valid
assert/retract requests with new idempotency keys record no-op status events;
those choices are allowed only if they preserve one relation identity, no
duplicate target-visible rows, durable decisions, and endpoint/detail
consistency.
