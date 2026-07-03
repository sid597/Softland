# Rama Review - World Kernel Contract

Status: reviewed.

## Contract Restatement

The intended lifecycle is:

```text
Projection / helper
  -> ActionRequest
  -> request depot
  -> Rama validation / interpretation / policy-state decision
  -> durable ActionDecision
  -> accepted KernelEvent only when accepted
  -> materialized PStates
  -> projection/read surface
```

The first physical Rama record for a normal world write should be an
`ActionRequest`, not a `KernelEvent`. Rejected requests should remain durable
as `ActionDecision` rows with `:event/id nil`; accepted requests should derive a
`KernelEvent` and materialize PStates. PStates are materialized views, not source
truth.

## Rama-First Reconstruction

- New data entering the world: proposed user/agent world actions as
  `ActionRequest` records.
- Entity owning local ordering: the target-local entity, usually artifact/unit,
  branch/policy target, agent run, or local world; request id is audit identity,
  not business locality.
- First physical depot record: V0 text `ActionRequest` in `*text-requests-depot`.
- Request/proposal vs accepted fact: `ActionRequest` asks; `ActionDecision`
  answers; `KernelEvent` happened only if accepted.
- Authoritative decision: Rama topology after common validation and any
  action-specific durable state reads.
- Partition key: semantic `:routing/key`, ideally matching the PState shard used
  for the decision.
- PStates colocated for hot decisions and reads: request/decision audit,
  target existence/state, event, artifact/revision/unit/status, policy, and
  projection materializations.
- Fast reads: request/decision/event audit, current artifact head, unit/status
  projections, and rejected-decision explanations.
- Retry/duplicate/restart risks: duplicate request ids, duplicate proposed event
  ids, partial stream writes, worker restart with in-memory compatibility atoms,
  and full-map projection reads.
- Side effects: this block should have no external side effects; compatibility
  atom mirrors are local side effects and need exclusion or rebuild strategy.
- Test evidence: request/event separation, accepted/rejected materialization,
  malformed validation before dispatch, duplicate request/id behavior, and cache
  restart/reconcile behavior.

## Phase Diagnosis

```text
Phase 0 - Implicit spec:
  Strong. The committed docs clearly corrected the ontology from request-less
  accepted events to ActionRequest -> ActionDecision -> KernelEvent.

Phase 1 - Plan:
  Mixed. Lifecycle and envelope plan are good, but production idempotency,
  request/decision audit locality, projection I/O, subindexing, and
  compatibility-cache restart strategy are under-specified.

Phase 2 - Plan validation:
  Should have passed for V0 traceability only. Should have failed for production
  because duplicate request semantics and locality/restart details are missing.

Phase 3 - Implementation:
  The shared envelope and V0 text topology implement the main lifecycle, but
  util_fns compatibility mirrors keep local app-state outside Rama.

Phase 4 - Implementation validation:
  Duplicate request ids can create multiple accepted facts. The topology also
  performs several nonlocal hash hops and keeps unbounded non-subindexed maps.

Phase 5 - Tests:
  Tests cover the core happy/rejected envelope paths but not duplicate request
  ids, retry, restart, or projection I/O shape.

Phase 6 - Test validation:
  Existing tests can pass while duplicate request ids create multiple events and
  while local compatibility atoms remain unrecoverable after restart.

Phase 7 - Finish/runtime:
  Focused tests pass, but the process required explicit termination in the
  first run due Rama background threads; production finish evidence is not yet
  enough for idempotency/restart/locality confidence.
```

Primary Failure Phase: phase-1

## Files Reviewed

### Phase Reconstruction

```text
docs/current-mental-model/build/rama-retro-review/01-world-kernel-contract/PHASE_RECONSTRUCTION.md
```

### Code

```text
src/app/server/rama/core.clj
src/app/server/rama/util_fns.cljc
src/app/server/rama/text_kernel.clj
```

`text_kernel.clj` was read because the current tests and util adapter use it as
the concrete implementation of the shared contract.

### Tests

```text
test/app/server/rama/text_kernel_test.clj
```

### Docs

```text
docs/current-mental-model/build/rama-retro-review/01-world-kernel-contract/BRIEF.md
docs/current-mental-model/architecture/action-request-kernel-routing.md
docs/current-mental-model/architecture/logical-lifecycle-and-derived-depots.md
docs/current-mental-model/architecture/rama-world-kernel-text-instance.md
docs/current-mental-model/10-anchors/rama-world-kernel-text-instance.md
docs/current-mental-model/architecture/rama-policy-throughput-post.md
docs/current-mental-model/architecture/rama-blog-patterns.md
docs/architecture/think-in-rama.md
```

### Commit Anchors

```text
bc058a7 2026-04-29 Implement Rama world kernel request pipeline
7a579b3 2026-04-29 Complete Rama world kernel V1 request contract
81bd089 2026-04-29 Canonize rejected ActionDecision shape
175f7cf 2026-05-01 Capture logical lifecycle and derived depots
6f9e62d 2026-04-29 Capture current mental model docs
f69add7 2026-04-29 Clarify Rama lifecycle mental model
19d53ef 2026-04-29 Capture Rama blog implementation patterns
b0ee81b 2026-05-01 Separate global context from implementation handoff
```

## Topology / Depot / PState Inventory

### Depots

```text
*text-requests-depot
  Declared in src/app/server/rama/text_kernel.clj:395-396
  Partitioner: (hash-by :routing/key)
```

### Topologies

```text
text-kernel-topology
  Declared in src/app/server/rama/text_kernel.clj:397
  Stream topology over *text-requests-depot.
```

### PStates

```text
$$requests-by-id
$$decisions-by-id
$$events-by-id
$$artifacts
$$artifact-heads
$$branches
$$policies
$$text-revisions
$$units-by-artifact
$$unit-status-by-branch
$$projection-cache
```

Declared in `src/app/server/rama/text_kernel.clj:398-408`.

### Query / Read Helpers

```text
read-request
read-decision
read-event
read-branch
read-artifact
read-text-head
read-units
read-unit-statuses
read-canonical-view
read-discarded-view
```

Implemented in `src/app/server/rama/text_kernel.clj:538-655`.

### Client Append Helpers

```text
append-action-request!
ingest-text!
set-unit-status!
util_fns compatibility append helpers
```

Implemented in `src/app/server/rama/text_kernel.clj:529-615` and
`src/app/server/rama/util_fns.cljc:23-103`.

### TaskGlobals / Caches / Executors

No Rama TaskGlobal is in this block. `util_fns.cljc` has in-process atoms:

```text
!cli-sessions
!agent-runs
!sidebar-truth-atom
!settings-truth-atom
!agent-trail-atom
!workspace-truth-atom
!editor-doc-atom
!flow-session-atom
```

Declared in `src/app/server/rama/util_fns.cljc:81-88`.

## Findings

### F1. Duplicate request ids can create multiple accepted KernelEvents

Severity: high

Evidence:

- `src/app/server/rama/core.clj:299-312` derives `:decision/id` solely from
  `:request/id` and accepted decisions point at the accepted event.
- `src/app/server/rama/text_kernel.clj:180-208` derives accepted event ids from
  `:proposed/event-id` or `request-id/event`.
- `src/app/server/rama/text_kernel.clj:425-450` accepts and materializes an
  ingest request without first checking whether the request id already has a
  decision.
- Runtime probe:

```text
{:decision-event evt_dup_b,
 :event-a? true,
 :event-b? true,
 :head rev_b,
 :request-content two}
```

Runtime trace:

1. Append `:artifact/ingest` request `req_dup` with proposed event `evt_dup_a`
   and revision `rev_a`.
2. Rama accepts it, writes decision `req_dup/decision`, event `evt_dup_a`, and
   materialized artifact head `rev_a`.
3. Append another `:artifact/ingest` request with the same `:request/id`
   `req_dup`, same artifact, different proposed event `evt_dup_b`, revision
   `rev_b`, and content.
4. The topology does not check existing `req_dup/decision`; it accepts the
   second request.
5. Both event rows exist, the request row is overwritten with the second
   request, the decision now points at `evt_dup_b`, and the artifact head moves
   to `rev_b`.

Rama concern:

Stream topology idempotency and duplicate depot records are not safe. A retried
or duplicated request can create additional accepted world facts.

Failure phase:

phase-1

Consequence:

Request id is not a stable idempotency/audit identity. Replay, retry, or client
duplicate behavior can produce multiple accepted facts and overwrite the audit
request/decision for the same request id.

Repair shape:

Define duplicate request semantics in the plan, then enforce them in topology.
At minimum, after routing to the request/decision audit partition, select the
existing decision for `request-id`; if present, replay/ignore without deriving a
new event or rewriting audit rows. Add tests for same request id with different
proposed event ids and payloads.

### F2. Compatibility adapters keep hidden in-memory truth outside Rama

Severity: high

Evidence:

- `src/app/server/rama/util_fns.cljc:7-10` says the namespace is a thin adapter
  and remaining legacy names are compatibility shims.
- `src/app/server/rama/util_fns.cljc:79-88` declares transitional local atom
  mirrors.
- `src/app/server/rama/util_fns.cljc:237-247`,
  `src/app/server/rama/util_fns.cljc:252-261`,
  `src/app/server/rama/util_fns.cljc:290-299`, and
  `src/app/server/rama/util_fns.cljc:305-317` append compatibility events and
  then mutate local atoms.

Runtime trace:

1. A legacy caller invokes a util helper such as `emit-sidebar-event!`,
   `emit-settings-event!`, `emit-workspace-truth-event!`, or `save-editor-doc!`.
2. The helper appends a compatibility ActionRequest.
3. The helper also mutates an in-process atom and read helpers return the atom.
4. If the worker/process restarts, those atoms are empty and there is no local
   rebuild path from Rama PStates.
5. If Rama later rejects or fails to materialize the compatibility request, the
   atom can still present state as if it were accepted.

Rama concern:

The API/helper layer becomes a hidden source of truth, violating the committed
Rama loop that clients append requests and projections read PStates.

Failure phase:

phase-3

Consequence:

The shared world-kernel contract is true for the text-kernel proof but not for
all public adapter surfaces in `util_fns`. Compatibility state can be lost on
restart and can drift from durable Rama state.

Repair shape:

Either explicitly exclude these shims from the world-kernel contract and keep
them quarantined, or migrate each read helper to Rama-backed projection/PState
reads with a durable rebuild path. Add restart/reconcile tests before treating
these helpers as kernel-compliant.

### F3. Physical locality and projection read shape are still V0/transitional

Severity: medium

Evidence:

- `docs/architecture/think-in-rama.md:137-164` says request ids are not business
  locality and calls request-id then artifact/branch hashing a bad smell.
- `src/app/server/rama/text_kernel.clj:40-43` marks the routing contract
  transitional and says current PStates still re-hash.
- `src/app/server/rama/text_kernel.clj:420-450`,
  `src/app/server/rama/text_kernel.clj:455-474`, and
  `src/app/server/rama/text_kernel.clj:479-499` route from the request depot to
  request id, decision id, artifact id, event id, and branch id partitions in a
  single stream event.
- `src/app/server/rama/text_kernel.clj:405-407` uses non-subindexed nested maps
  for revisions, units, and statuses.
- `src/app/server/rama/text_kernel.clj:564-570` reads full unit/status maps, and
  `src/app/server/rama/text_kernel.clj:634-647` does projection filtering in
  client/helper code.

Runtime trace:

1. The request depot starts on `:routing/key`, which is good.
2. Ingest then writes audit rows by request/decision id, event by event id,
   branch by branch id, and artifact/revision/unit rows by artifact id.
3. Status-set first hashes to artifact to read the unit, then writes audit rows
   by request/decision id, event by event id, branch by branch id, and status by
   branch id.
4. Projection helpers fetch whole unit/status maps and filter locally.

Rama concern:

This violates the production locality/I/O goal: topology work does not stay
near the shard that owns the target decision, and large projections will require
full-map reads instead of subindexed/range/query topology reads.

Failure phase:

phase-1

Consequence:

The envelope contract is correct, but the physical PState layout is not yet the
production Rama shape. Branch-level status maps can become hot/unbounded; large
artifact projections can load full maps; audit/event writes add partition hops.

Repair shape:

Keep the semantic `:routing/key`, but define colocated audit/materialization
keys for each module or add query/secondary indexes intentionally. Subindex
large inner maps, denormalize projection PStates, or move projection filtering
into query topologies. Treat current shape as V0 only.

### F4. Tests prove the lifecycle but not production retry/restart/locality

Severity: medium

Evidence:

- `test/app/server/rama/text_kernel_test.clj:6-61` covers request/event
  separation, proposed event id, routing key validity, and drift validation.
- `test/app/server/rama/text_kernel_test.clj:76-136` covers accepted request,
  decision, materialization, and projections.
- `test/app/server/rama/text_kernel_test.clj:138-188` covers hidden event id and
  malformed unknown action rejection.
- `test/app/server/rama/text_kernel_test.clj:190-209` covers durable rejection
  without event row.
- No test covers duplicate request id/idempotency, topology retry, worker
  restart/reconcile for util atoms, or large projection read shape.

Runtime trace:

1. The focused test namespace passes with unique request ids.
2. A separate duplicate-request probe creates two accepted events for one
   request id.
3. Therefore the green suite proves the lifecycle path but not idempotency.

Rama concern:

Tests can pass while an important stream-topology idempotency contract is broken.

Failure phase:

phase-5

Consequence:

The test suite is good evidence for the ontology correction but weak evidence
for production correctness.

Repair shape:

Add duplicate/retry/idempotency tests, restart/reconcile tests for compatibility
state or explicit exclusion tests, and projection-read shape tests once the
production PState/query design is chosen.

## Phase-4 Style Check Matrix

### Redundant Conditionals

Verdict: PASS for reviewed scope.

Evidence:

The reviewed topology branches have distinct materialization bodies for invalid,
ingest, status, compat, and unknown actions
(`src/app/server/rama/text_kernel.clj:416-499`).

### Consecutive Keypath

Verdict: FAIL.

Evidence:

`src/app/server/rama/text_kernel.clj:449`,
`src/app/server/rama/text_kernel.clj:456`,
`src/app/server/rama/text_kernel.clj:474`, and
`src/app/server/rama/text_kernel.clj:562` use consecutive keypath navigators
where `(keypath *outer *inner)` / `(keypath artifact-id revision-id)` would be
the expected path style.

### Select-Compute-Transform

Verdict: PASS / not the primary issue.

Evidence:

`src/app/server/rama/text_kernel.clj:455-457` reads the unit before deciding a
status request; the selected value is part of the authoritative decision, not
just a read-modify-write that can be replaced blindly.

### Unnecessary nil->val

Verdict: PASS.

Evidence:

No `nil->val` occurrences were found in the reviewed files.

### :allow-yield?

Verdict: FAIL for projection/read direction, not for current topology loops.

Evidence:

The current read helpers fetch full nested maps (`text_kernel.clj:564-570`) and
then projection filtering iterates in client/helper code
(`text_kernel.clj:634-647`). These are not Rama `local-select>` range scans, so
`:allow-yield?` is not directly applicable there, but the shape avoids the
subindexed/yieldable Rama query path that would be needed once unit/status maps
can exceed roughly 100 entries.

### Non-Subindexed Collections Without Size Limits

Verdict: FAIL.

Evidence:

`src/app/server/rama/text_kernel.clj:405-407` declares nested maps for
`$$text-revisions`, `$$units-by-artifact`, and `$$unit-status-by-branch` without
subindexing. The committed docs and code do not enforce a maximum number of
revisions, units, or statuses.

### Stream Topology Idempotency

Verdict: FAIL.

Evidence:

Finding F1 shows duplicate request ids can create multiple accepted KernelEvents
and overwrite request/decision audit rows.

### Partial Failure In Stream Topologies

Verdict: FAIL / unproven.

Evidence:

An accepted ingest can write request, decision, event, branch, artifact,
artifact head, revision, and units across several partitions
(`src/app/server/rama/text_kernel.clj:425-450`). The reviewed plan/code does not
show a retry guard that first checks whether this request was already accepted
before re-materializing.

### Single Depot Append Per Client Operation

Verdict: PASS for text-kernel helpers; mixed for compatibility helpers.

Evidence:

`append-action-request!` performs one `foreign-append!`
(`src/app/server/rama/text_kernel.clj:529-532`), and `ingest-text!` /
`set-unit-status!` use it once (`text_kernel.clj:597-615`). Compatibility
helpers append once, but also mutate local atoms afterward
(`src/app/server/rama/util_fns.cljc:237-317`), which is captured as F2.

### Application-State Caches Survive Restart

Verdict: FAIL.

Evidence:

`src/app/server/rama/util_fns.cljc:81-88` declares several in-memory atoms used
by read helpers. No durable rebuild path is present in the reviewed scope.

### No Reimplementation Of Built-In Operations

Verdict: PASS for reviewed scope.

Evidence:

No custom replacement for a Rama built-in operation was identified in the
reviewed shared contract files.

## Tests / Commands

```text
clojure -M:test -e "(require 'clojure.test 'app.server.rama.text-kernel-test) (clojure.test/run-tests 'app.server.rama.text-kernel-test)"

Result printed:
Testing app.server.rama.text-kernel-test
Ran 7 tests containing 69 assertions.
0 failures, 0 errors.
{:test 7, :pass 69, :fail 0, :error 0, :type :summary}

Note:
The first run printed the green summary but did not exit promptly because Rama
background threads stayed alive; the session later exited and no lingering test
process remained.
```

Duplicate request-id probe:

```text
clojure -M:test -e "(require '[app.server.rama.text-kernel :as tk]) ... duplicate request probe ..."

Result:
{:decision-event evt_dup_b, :event-a? true, :event-b? true, :head rev_b, :request-content two}
```

## Recommended Repair Queue

```text
1. Define duplicate request/id and :idempotency/key semantics for the shared
   kernel contract.
2. Add topology-level replay/dedupe before accepted event materialization.
3. Add duplicate request/id tests and retry-shaped tests.
4. Quarantine or replace util_fns in-memory compatibility mirrors with
   Rama-backed projections or documented rebuild paths.
5. Promote V0 locality notes into a concrete production PState/depot/query plan
   before treating the text/world kernel shape as final.
6. Subindex or denormalize large unit/status/projection reads.
```

## Verdict

The shared envelope ontology is correct and the focused V0 tests pass, but the
committed plan/implementation is not production-safe because duplicate requests
can create multiple accepted facts, compatibility adapters keep unrecoverable
in-memory truth, and the physical PState/locality shape remains transitional.

```text
Primary Failure Phase: phase-1
```

```text
RETRO_RAMA_REVIEW:major-fail
```
