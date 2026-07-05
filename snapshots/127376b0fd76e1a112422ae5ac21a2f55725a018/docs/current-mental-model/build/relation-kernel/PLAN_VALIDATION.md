# Plan Validation - RelationEdge kernel

Phase: Rama Phase 2 - Plan Validation
Artifact under review: `docs/current-mental-model/build/relation-kernel/PLAN.md`
Verdict: PASS

This validation treats `CONTRACT.md`, `IMPLICIT_SPEC.md`, and `decisions.md` as
binding. Default verdict is FAIL; PASS is only after scenario tracing. The
current `PLAN.md` resolves both blockers from the prior validation:

- F1 routing is fixed by using the contract's map envelope on the depot wire,
  with top-level `:relation/routing-key` read by `(hash-by :relation/routing-key)`.
- F2 idempotency scope is resolved by Fable's binding ruling: idempotency is
  relation-scoped as `(relation-id, idempotency-key)`.

Inputs read:

- `docs/sessions/next-prompt.md`
- `docs/current-mental-model/decisions.md`
- `docs/current-mental-model/build/relation-kernel/CONTRACT.md`
- `docs/current-mental-model/build/relation-kernel/IMPLICIT_SPEC.md`
- `docs/current-mental-model/build/relation-kernel/PLAN.md`
- prior `PLAN_VALIDATION.md`
- `.agents/skills/rama/SKILL.md`
- `.agents/skills/rama/references/phases.md`
- `.agents/skills/rama/references/phase-2-plan-validate.md`
- `.agents/skills/rama/references/artifact-plan-validation.md`
- `.agents/skills/rama/references/microbatch.md`
- `.agents/skills/rama/references/query-topologies.md`
- `.agents/skills/rama/references/batch.md`
- `.agents/skills/rama/references/pstate-schema.md`
- `.agents/skills/rama/references/paths.md`
- `.agents/skills/rama/references/dataflow.md`
- `.agents/skills/think-in-rama/SKILL.md`
- `.agents/skills/rama-retro-lens/SKILL.md`
- `docs/architecture/think-in-rama.md`
- retro docs: `META_LEARNINGS.md`, `ARCHITECTURE_VERDICT.md`,
  `01-world-kernel-contract/RAMA_REVIEW.md`
- source idiom checks:
  `src/app/server/rama/object_container.clj:383-396`,
  `src/app/server/rama/object_container.clj:1657-1686`,
  `src/app/server/rama/object_container.clj:1693-1694`,
  `src/app/server/rama/object_container.clj:1784-1787`,
  `src/app/server/rama/dogfood/space.clj:169-171`,
  `src/app/server/rama/dogfood/space.clj:1456-1478`,
  `src/app/server/rama/dogfood/space.clj:2071-2076`

## Resolved Prior Blockers

### F1. Depot partitioner and request event now name the same key

Verdict: PASS

Source constraint, verbatim:

> `(declare-depot setup *relation-request-depot (hash-by :relation/routing-key))`
> - `CONTRACT.md:102-103`

> `routing-key = relation-id -> request, journal, decision, and the authoritative $$relations-by-id row all colocate on one task.`
> - `CONTRACT.md:104-105`

Plan evidence:

- `PLAN.md:262-272` defines the depot event type as the contract request map
  envelope with top-level `:relation/routing-key`.
- `PLAN.md:275-284` states that `(hash-by :relation/routing-key)` reads that
  top-level namespaced key, and that typed rows are constructed only inside the
  topology for PState storage.
- `PLAN.md:286-294` requires callers/tests to compute deterministic
  `relation-id` and set `:relation/routing-key` to it.
- `PLAN.md:617-644` repeats the depot declaration and carries a Phase 5
  partition-provenance test proving the journal and `$$relations-by-id` are
  readable from `hash(relation-id)`.
- `PLAN.md:696-704` traces topology ingress: each map envelope starts on
  `hash(relation-id)` and the journal read at `[relation-id idempotency-key]`
  is local.

Codebase idiom check:

- `object_container.clj:1657-1683` builds a map from `core/action-request` and
  adds routing/index keys with `assoc`; `object_container.clj:1685-1686` declares
  the depot as `(hash-by :partition/key)`.
- `object_container.clj:383-396` converts the request map into a typed
  `ObjectContainerRequestRow` after ingestion, matching the plan's
  map-on-wire/typed-PState split.
- `dogfood/space.clj:169-171` lists map-envelope keys including
  `:idempotency/key` and `:routing/key`; `dogfood/space.clj:1460` declares
  `(hash-by :routing/key)`.

Scenario trace:

1. Client/test appends:
   `{:relation/routing-key "rel:A" :request/id "req-1" :request/type
   :relation/assert :idempotency/key "idem-1" :actor sid :payload payload}`.
2. The depot partitioner reads `:relation/routing-key` directly from the map and
   places the record on `hash("rel:A")`.
3. The topology reads the journal at `["rel:A" "idem-1"]`, writes the decision,
   event, status log, and `$$relations-by-id` row on the same relation-id task,
   then explicitly hops to endpoint target keys for target indexes.
4. A later retry for the same pair starts on the same task and sees the same
   journal entry before relation truth is touched.

Fault-tolerance check:

- Worker restart: PStates are durable; no TaskGlobal or in-memory cache exists
  (`PLAN.md:874-875`), so restart preserves the relation-id-local journal and row.
- Topology retry: microbatch retry replays the same depot records and applies
  PState updates exactly once (`microbatch.md:35-48`).
- Multi-partition write failure: writes before and after endpoint `|hash` hops
  remain in one microbatch transaction (`microbatch.md:19-31`).

Race-condition check:

- Two clients writing `rel:A` use the same depot key and serialize on the
  relation-id task before any accepted fact is materialized.
- Endpoint target-index hops happen after the local decision/journal fold and
  are covered by the same microbatch transaction.

Flaws found: none found, with reasoning: the physical depot event now carries
the exact key the partitioner reads, and the plan carries an implementation test
to prove the routing provenance.

### F2. Idempotency scope is now binding and relation-scoped

Verdict: PASS

Source constraint, verbatim:

> `idempotency keys are relation-scoped -- the journal lives on the relation's task; effective uniqueness is per (relation-id, idempotency-key).`
> - `CONTRACT.md:137-139`

> `Global uniqueness across relations is explicitly NOT provided: the key is a client retry token, not an operation identity`
> - `CONTRACT.md:139-141`

> `the same idempotency-key submitted for two different relations -> both relations succeed independently`
> - `CONTRACT.md:279-281`

> `Everywhere this spec says "the same idempotency key," read "the same (relation-id, idempotency-key) pair."`
> - `IMPLICIT_SPEC.md:11-14`

Plan evidence:

- `PLAN.md:298-302` checks `[relation-id idempotency-key]`; an existing entry
  replays the stored decision and stops without relation-truth writes.
- `PLAN.md:339-340` applies the same relation-scoped replay rule to retractions.
- `PLAN.md:409-442` chooses
  `relation-id -> idempotency-key -> decision` and rejects the flat
  `idempotency-key -> decision` option because it violates colocation.
- `PLAN.md:842-849` records the Fable ruling and requires gate 11 in Phase 5.
- `PLAN.md:881-936` preserves the ruling rationale and explicitly names gate 11.
- `PLAN.md:981-984` asks this validation to confirm no global
  `hash(idempotency-key)` index crept in.

Scenario trace:

1. Append valid assertion for `rel:A` with idempotency key `idem-77`.
2. Append the same relation request again with `rel:A` and `idem-77`.
3. Both records route to `hash("rel:A")`; the second reads
   `$$relation-decisions-by-idempotency["rel:A"]["idem-77"]`, replays the stored
   decision, and writes no decision/event/status/index/descriptor rows.
4. Append a different valid assertion for `rel:B` with the same key `idem-77`.
5. This routes to `hash("rel:B")`; the journal key is `["rel:B" "idem-77"]`, so
   it is independent and the second relation can be accepted. That is exactly
   CONTRACT gate 11 after the Fable ruling.

Fault-tolerance check:

- Worker restart: both relation-scoped journal entries are durable.
- Topology retry: retrying a microbatch cannot duplicate relation truth because
  PState updates are exactly-once for the microbatch attempt.
- Multi-partition write failure: the journal, decision, row, history, target
  indexes, and descriptors are one microbatch transaction for accepted requests.

Race-condition check:

- Same relation/idempotency pair races converge on one journal entry on the
  relation-id task.
- Cross-relation key reuse intentionally does not serialize through a global
  idempotency-key shard; Fable ruled this is the product contract.

Flaws found: none found, with reasoning: the previously ambiguous implicit spec
now has a binding amendment, and the plan matches the amended contract.

## Query Topology Checks

### Query topology: `relations-for-targets`

- Input examples present: yes (`PLAN.md:152-181`, `PLAN.md:736-793`).
- Example 1: empty target list. N=0, M=0. N == M? yes
  (`PLAN.md:154`, `PLAN.md:740-744`).
- Example 2A: `["doc-A"]`, no filter, no rows. N=1 descriptor point read, M=1
  because the descriptor proves absence and avoids an empty target range seek
  (`PLAN.md:155-157`, `PLAN.md:752-753`). N == M? yes.
- Example 2B: `["doc-A"]`, no filter, visible rows. N=2, M=2
  (`PLAN.md:158-159`, `PLAN.md:755-756`). N == M? yes.
- Example 3: `["doc-A" "doc-B"]`, one kind, only one live prefix. N=3, M=3
  because missing prefixes are skipped by descriptor counts
  (`PLAN.md:163-165`, `PLAN.md:764-766`). N == M? yes.
- Example 4: two targets, two kinds, three live prefixes. N=5, M=5
  (`PLAN.md:166-170`, `PLAN.md:774-776`). N == M? yes.
- M values across examples: 0, 1, 2, 3, 5. All same? no.
- If M differs: must be marked variable with dynamic approach. Is it? yes. The
  plan uses `ops/explode`, descriptor point reads, generated nonzero range
  descriptors, `|origin`, and aggregation (`PLAN.md:172-181`,
  `PLAN.md:780-793`). This matches the query-topology rule that variable reads
  must be produced dynamically and aggregated (`query-topologies.md:18-20`,
  `batch.md:50-64`).
- Verdict: PASS.

### Query topology: `relation-detail`

- Input examples present: yes (`PLAN.md:221-235`, `PLAN.md:795-820`).
- Existing relation id: N=2, M=2: current row point read plus status-history
  range read (`PLAN.md:216-218`, `PLAN.md:803-804`).
- Missing valid-shaped relation id: N=1, M=1 because `keypath` on nil/absent
  data emits nil and the plan skips the empty history range
  (`PLAN.md:211-215`, `PLAN.md:812-814`; `paths.md:73-83`).
- M values differ: yes.
- Variable with dynamic approach? yes. The history range is conditionally emitted
  only when the row exists (`PLAN.md:234-235`, `PLAN.md:819-820`).
- Verdict: PASS.

## PState Schema Checks

- Any `Object` type? no. The plan explicitly says no PState schema uses `Object`
  (`PLAN.md:357-360`) and lists concrete record/interface types
  (`PLAN.md:361-407`).
- Uniform record-like values use concrete record schemas? yes. PState leaves are
  concrete defrecord classes: `RelationDecisionRow`, `RelationEventRow`,
  `RelationEdgeRow`, `RelationStatusLogRow`, and
  `RelationTargetDescriptorRow` (`PLAN.md:382-402`, `PLAN.md:669-692`).
- Different instances at the same PState position with different fields use
  definterface + defrecord? yes where polymorphism may appear:
  `IRelationRequestPayload` plus `RelationMutationPayload`, and if Phase 3
  splits assert/retract payload records both must implement the interface
  (`PLAN.md:364-407`). This matches `pstate-schema.md:219-226`.
- Inner collections that can exceed 100 elements subindexed? yes. The
  idempotency journal, status log, and target index inner maps are subindexed
  (`PLAN.md:670-688`, `PLAN.md:854-861`). The descriptor inner map is not
  subindexed because it is bounded by `2 x registered relation kinds`, enforced
  by the code-level registry (`PLAN.md:587-591`, `PLAN.md:862-864`).
- Verdict: PASS.

## Topology Checks

- Microbatch unless justified? yes. The plan uses one microbatch topology for
  cross-partition authoritative/index writes (`PLAN.md:656-665`). This matches
  the reference: microbatch gives exactly-once PState updates and
  cross-partition atomicity (`microbatch.md:1-5`, `microbatch.md:19-48`).
- Low-latency writes requiring stream topology: none. The implicit spec says
  assertion/retraction latency may be hundreds of milliseconds or seconds
  (`IMPLICIT_SPEC.md:61-63`, `IMPLICIT_SPEC.md:118-119`).
- Stream topology present? no (`PLAN.md:721`).
- Internal depot appends from microbatch? none (`PLAN.md:654`), avoiding the
  microbatch depot-append retry caveat (`microbatch.md:41-43`).
- Verdict: PASS.

## Production Readiness Checks

- Multiple concurrent clients: PASS. Same-relation writes route to
  `hash(relation-id)` and fold through one journal/current-row path
  (`PLAN.md:275-302`, `PLAN.md:696-719`). Different relation ids are independent
  by contract.
- Client process restart: PASS. Retry tokens are durable in
  `$$relation-decisions-by-idempotency`, and duplicate same-pair requests replay
  the stored decision without new truth writes (`PLAN.md:298-302`,
  `PLAN.md:718-719`).
- Worker process restart during topology execution: PASS. No TaskGlobal or
  in-memory cache is used (`PLAN.md:874-875`), and microbatch commit makes PState
  changes visible atomically after retry (`microbatch.md:44-48`).
- Large scale: PASS. Unbounded status history and target membership are
  subindexed; descriptor size is explicitly bounded by the registry
  (`PLAN.md:854-864`, `pstate-schema.md:51-57`).
- Stream topology non-idempotent writes: not applicable; no stream topology.
- Stream topology multi-partition partial failure: not applicable; no stream
  topology.
- Verdict: PASS.

## Cross-Topology Correctness

- Internal depots: none (`PLAN.md:654`).
- Can a sending topology produce duplicate records into internal depots? not
  applicable.
- Query topologies write to user PStates? no (`PLAN.md:732`;
  `query-topologies.md:143-148`).
- Verdict: PASS.

## Stream Topology Correctness

- `depot-partition-append!` in stream topology: none.
- Commit-boundary requirement: not applicable.
- Verdict: PASS.

## Spec Coverage

### W1. `:relation/assert`

- Source: "`:relation/assert` - assert or reassert a relation" with deterministic
  relation id, relation-scoped idempotency replay, no target FK check, no
  duplicate target membership, and separate asserters (`IMPLICIT_SPEC.md:50-111`).
- Trace through the plan: callers compute deterministic relation id and set
  `:relation/routing-key` (`PLAN.md:286-294`); same relation/idempotency pair
  replays (`PLAN.md:298-302`); new valid assertions write authoritative row,
  status log, accepted decision, accepted event, target index copies, and target
  descriptors (`PLAN.md:305-306`); accepted reassertions write event/status
  evidence and rewrite the same current copies (`PLAN.md:307-312`); different
  asserters compute different relation ids (`PLAN.md:317-318`).
- Fault-tolerance check: all accepted writes are in one microbatch transaction;
  duplicate same-pair replay writes nothing.
- Race-condition check: same relation id serializes on relation-id routing;
  different asserters have different deterministic ids by contract.
- Flaws found: none found, with reasoning: the plan enforces every W1 state
  transition from the implicit matrix.
- Verdict: PASS.

### W2. `:relation/retract`

- Source: "`:relation/retract` - retract a relation without deleting it" and only
  the original asserter can retract (`IMPLICIT_SPEC.md:113-149`).
- Trace through the plan: duplicate same-pair requests replay
  (`PLAN.md:339-340`); missing relation rejects (`PLAN.md:341`); actor mismatch
  rejects with no truth writes (`PLAN.md:342-343`); matching asserter transitions
  to `:retracted` and rewrites row/history/index/descriptor state in the same
  microbatch (`PLAN.md:344-348`); repeated valid retracts on an already retracted
  row write accepted status/history evidence without duplicate row membership
  (`PLAN.md:349-353`).
- Fault-tolerance check: retraction is a status transition, never deletion; all
  touched copies update atomically in microbatch.
- Race-condition check: assert/retract ordering for one identity is owned by the
  relation-id task.
- Flaws found: none found, with reasoning: rejected retractions leave relation
  truth untouched and accepted retractions preserve permanent history.
- Verdict: PASS.

### R1. `relations-for-targets`

- Source: "`relations-for-targets`: [target-keys kinds-filter
  include-retracted?] -> {target-key -> [RelationEdgeRow ...]}" with per-key
  `|hash`, range reads using `{:allow-yield? true}`, status filtering, and
  `|origin` aggregation (`CONTRACT.md:190-199`).
- Trace through the plan: deduplicate target keys; empty input returns `{}`;
  `|hash` each key; descriptor point-read proves absence/presence; target range
  reads are emitted only from nonzero descriptor counts; range reads use
  `{:allow-yield? true}`; statuses are filtered; rows return through `|origin`;
  unknown targets are merged back as empty vectors (`PLAN.md:122-150`,
  `PLAN.md:780-793`). `:allow-yield? true` matches the dataflow guidance for
  reads that may iterate many subindexed entries (`dataflow.md:166-184`).
- Fault-tolerance check: query topology is read-only; committed rows/descriptors
  come from the microbatch write transaction.
- Race-condition check: after commit, full-row endpoint copies and descriptors
  agree because they are written in the same microbatch attempt.
- Flaws found: none found, with reasoning: the plan avoids known-empty range
  seeks and keeps the public read path in one clustered query invocation.
- Verdict: PASS.

### R2. `relation-detail`

- Source: "`relation-detail`: [relation-id] -> {:row RelationEdgeRow :history
  [status events...]}" using `|hash relation-id`, point read, and one subindexed
  range read (`CONTRACT.md:201-204`).
- Trace through the plan: malformed ids return without scanning; valid-shaped ids
  hash to relation id; current row point read happens first; missing row skips
  the history read; existing row reads the status log with `{:allow-yield? true}`;
  result returns through `|origin` (`PLAN.md:204-220`, `PLAN.md:795-820`).
- Fault-tolerance check: query topology is read-only; row and status history are
  durable PState materializations.
- Race-condition check: after an accepted transition commits, detail reads the
  current row and ordered status log on the relation-id task.
- Flaws found: none found, with reasoning: missing ids do not scan or issue empty
  history range reads, and existing ids use the subindexed history shape.
- Verdict: PASS.

### V1. Decision/audit inspection for tests only

- Source: tests need to prove rejected decisions are durable and invalid requests
  wrote only the decision/audit spine; direct PState inspection must not become a
  product API (`IMPLICIT_SPEC.md:207-213`).
- Trace through the plan: V1 allows direct reads of decision/journal/event and
  materialization PStates only for validation, and explicitly forbids wiring
  those reads into Electric server, trail-view projections, or agent context
  builders (`PLAN.md:237-254`).
- Fault-tolerance check: decision rows are durable PStates, not helper return
  values.
- Race-condition check: validation reads occur after tests wait for microbatch
  processed count (`PLAN.md:320-322`, `PLAN.md:646-652`).
- Flaws found: none found, with reasoning: the plan preserves the public read
  boundary while leaving tests enough observability to prove rejection behavior.
- Verdict: PASS.

### Module Boundary and Ownership

- Source: "A new module: `relation-kernel-module`" and implementation files are
  `src/app/server/rama/relation_kernel.clj` plus a test namespace
  (`CONTRACT.md:23-27`, `CONTRACT.md:288-293`).
- Trace through the plan: new namespace, no edits to existing kernels, only
  helper fns from object-container (`PLAN.md:822-828`).
- Fault-tolerance check: relation truth is owned by relation-kernel PStates, not
  by source adapters or app helpers.
- Race-condition check: future transcript/git/doc importers emit relation
  requests; they do not own private relation rows.
- Flaws found: none found, with reasoning: the module owns accepted relation
  truth and consumers read through its query topologies.
- Verdict: PASS.

### Refusals and Extension Points

- Source: no foreign-key validation, no confidence scores, no relation-as-
  container, no auto-merge, no cascading behavior, no deletion
  (`CONTRACT.md:228-238`).
- Trace through the plan: no target FK validation (`PLAN.md:303-304`,
  `PLAN.md:837`); asserters remain separate (`PLAN.md:317-318`,
  `PLAN.md:829-833`); rejected requests write decisions only (`PLAN.md:710-712`);
  retraction is status, not deletion (`PLAN.md:838`).
- Fault-tolerance check: refusals are encoded in topology behavior and PState
  shape, not in transient helper state.
- Race-condition check: deterministic identity including asserter prevents
  cross-asserter storage merge.
- Flaws found: none found, with reasoning: the plan does not introduce any
  refused feature.
- Verdict: PASS.

### Data Model, Identity, and Target Rules

- Source: `relation-id` is deterministic over kind, from, to, and asserter;
  unknown target kinds fall back to target id; unary `:none` inherits the
  from-side target key (`CONTRACT.md:79-94`, `CONTRACT.md:113-120`).
- Trace through the plan: client/test preparation computes deterministic
  relation id and target keys (`PLAN.md:286-294`, `PLAN.md:331-335`); target key
  derivation follows contract rules and unknown target kinds fall back to target
  id (`PLAN.md:835-836`); unary `:none` may write both direction entries under
  one target key while R1 deduplicates output by `[target-key relation-id]`
  (`PLAN.md:148-150`, `PLAN.md:535-537`, `PLAN.md:878-879`).
- Fault-tolerance check: deterministic ids make re-imports converge after worker
  or client restart.
- Race-condition check: same logical assertion by same asserter maps to one
  relation id; same kind/from/to by different asserter maps to a separate id.
- Flaws found: none found, with reasoning: identity, target key derivation, and
  unary shape match the contract.
- Verdict: PASS.

### Acceptance Gates

1. Assert + dual read: PASS. Plan writes authoritative row plus both endpoint
   target copies in one microbatch, then R1 reads by target key
   (`PLAN.md:305-316`, `PLAN.md:727-793`).
2. Idempotent client retry: PASS. Same `(relation-id, idempotency-key)` replays
   stored decision and writes nothing (`PLAN.md:298-302`, `PLAN.md:718-719`).
3. Convergent re-import: PASS. Deterministic relation id plus accepted
   reassertion history preserves one target membership (`PLAN.md:286-294`,
   `PLAN.md:307-312`).
4. Retract consistency: PASS. Retraction rewrites authoritative row, history,
   all target copies, and descriptors in one microbatch (`PLAN.md:344-348`).
5. Retraction rights: PASS. Actor mismatch rejects with no truth writes
   (`PLAN.md:341-343`).
6. Registry: PASS. Unregistered kinds reject durably and do not materialize
   relation truth (`PLAN.md:303-306`, `PLAN.md:832-834`).
7. Dangling: PASS. Plan never checks target existence and accepts well-formed
   dangling targets (`PLAN.md:303-304`, `PLAN.md:837`).
8. Unary: PASS. `:none` inherits from-side target key, touches one target
   partition, and R1 deduplicates output (`PLAN.md:148-150`, `PLAN.md:535-537`,
   `PLAN.md:878-879`).
9. Asserter separation: PASS. Asserter is part of deterministic id, so Sid and
   LLM assertions remain separate rows (`PLAN.md:317-318`, `PLAN.md:829-833`).
10. Kind filter: PASS. Descriptor-gated prefix reads only emit nonzero
    direction/kind prefixes (`PLAN.md:141-145`, `PLAN.md:788-791`).
11. Cross-relation key reuse: PASS. Fable ruled relation-scoped idempotency;
    the plan stores `relation-id -> idempotency-key -> decision` and requires
    gate 11 in Phase 5 (`CONTRACT.md:279-281`, `PLAN.md:845-849`,
    `PLAN.md:931-936`).

Verdict: PASS.

### Implementation Style Gates

- Typed defrecords: PASS. PState rows are concrete defrecords
  (`PLAN.md:361-407`).
- Imported partition helpers: PASS. The only existing helper dependency is
  `extract-object-key`, `fixed-width-order-key`, and `actor-row`
  (`PLAN.md:822-828`).
- `{:allow-yield? true}` on unbounded range reads: PASS. R1 target range reads
  and R2 history reads use it (`PLAN.md:144-145`, `PLAN.md:216-218`,
  `PLAN.md:790-791`).
- Consumers only via two query topologies: PASS. Product consumers use R1/R2;
  direct PState reads are validation-only (`PLAN.md:237-254`,
  `PLAN.md:870-871`).

Verdict: PASS.

## Retro-Lens Check

Product contract restatement:

RelationKernel owns typed, provenance-carrying relation truth. Clients or later
source adapters submit relation requests. Rama accepts or rejects them. Accepted
assertions/retractions materialize durable relation rows, status history,
target-facing indexes, and descriptor gates. Product consumers read only through
`relations-for-targets` and `relation-detail`.

Physical Rama path:

```text
request map envelope with :relation/routing-key
  -> *relation-request-depot keyed by relation id
  -> microbatch journal/validation/fold
  -> RelationDecisionRow accepted or rejected
  -> RelationEventRow only for accepted truth transitions
  -> $$relations-by-id + $$relation-status-log-by-relation
     + $$relations-by-target + $$relation-target-descriptors
  -> relations-for-targets / relation-detail query topologies
```

Authoritative truth:

- The relation kernel owns accepted relation facts in `$$relations-by-id`.
- Target index rows and descriptors are denormalized read projections.
- Decision rows are durable request/audit truth.
- Event/status rows are accepted relation-transition history.

Learned:

- The plan preserves the request -> decision -> accepted event -> materialized
  state loop from `think-in-rama.md:39-61`.
- The plan answers the retro default gate: first physical record, proposal vs
  fact, durable rejection, duplicate semantics, worker restart, locality key,
  unbounded PState shape, and product-path test obligations
  (`META_LEARNINGS.md:536-558`).
- The plan does not create a source-specific truth island: transcript/git/doc
  importers are future request emitters, not relation owners.

Repeating:

- None found. The current plan avoids the old "named invariant without physical
  evidence" failure by aligning the depot key with the request map and
  carrying a partition-provenance test into Phase 5.

Repair next:

- Advance to Rama Phase 3 implementation in a fresh session, following the
  current `PLAN.md`.
- Phase 5 must include all 11 acceptance gates, including partition provenance
  and cross-relation idempotency-key reuse.

PHASE_VALIDATION:pass
