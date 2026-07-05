# Phase Reconstruction - World Kernel Contract

Status: reconstructed.

This is a retroactive reconstruction from the committed docs and tests. It is
not a new design proposal.

## Source Evidence

Committed docs and code read:

```text
docs/current-mental-model/build/rama-retro-review/01-world-kernel-contract/BRIEF.md
docs/current-mental-model/architecture/action-request-kernel-routing.md
docs/current-mental-model/architecture/logical-lifecycle-and-derived-depots.md
docs/current-mental-model/architecture/rama-world-kernel-text-instance.md
docs/current-mental-model/10-anchors/rama-world-kernel-text-instance.md
docs/current-mental-model/architecture/rama-policy-throughput-post.md
docs/current-mental-model/architecture/rama-blog-patterns.md
docs/architecture/think-in-rama.md
src/app/server/rama/core.clj
src/app/server/rama/util_fns.cljc
src/app/server/rama/text_kernel.clj
test/app/server/rama/text_kernel_test.clj
```

The listed `architecture/rama-world-kernel-text-instance.md` is a pointer to
`10-anchors/rama-world-kernel-text-instance.md`, so the anchor is treated as
committed source evidence for this reconstruction.

## Phase 0 - Implicit Spec

The committed docs imply this problem:

```text
Softland needs a shared world-kernel lifecycle where projected UI/agent intent
becomes a durable request, Rama makes the authoritative decision, accepted facts
materialize into PStates, and projections read those materializations.
```

The old, wrong model was:

```text
projection -> request-less accepted event -> Rama -> materialized state
```

The corrected model is:

```text
Projection
  -> ActionRequest
  -> request depot/routing
  -> Rama decision
  -> accepted KernelEvent or rejected ActionDecision
  -> materialized state / projections
```

Core entities and identities:

```text
ActionRequest
  Proposed change entering Rama. It is allowed to be wrong.

ActionDecision
  Durable answer recorded by Rama. Accepted decisions carry event id/event;
  rejected decisions carry event/id nil, reason, and errors.

KernelEvent
  Accepted world fact derived only after Rama accepts a request.

Target / Action / Policy / Context / Causal / Provenance
  Shared envelope contracts reused across carriers.
```

Invariants:

```text
The first physical record Rama sees for a user/world write is an ActionRequest.
ActionRequest is not a text/PDF/chat/code carrier.
ActionRequest must not have a top-level :event/id.
Hidden event identity in request payload is invalid.
:action/type is canonical; :request/type may exist but must not drift.
Rejected requests are durable ActionDecisions and do not create KernelEvents.
PStates are materialized views, not source truth.
Derived depots are downstream of accepted truth, not where truth first becomes true.
Authentication/edge guard may happen before Rama; authoritative world decisions happen inside Rama.
```

Scale expectations:

```text
V0 may prove traceability with one small text/world kernel.
Production Softland must not keep one random/global request inbox.
Depot and PState layout must match the entity whose state is being decided.
Target-local actions should route by semantic locality such as artifact, unit,
branch, policy target, agent run, or local world.
```

Failure cases:

```text
Malformed request envelope.
Top-level or payload :event/id on ActionRequest.
:request/type / :action/type drift.
Invalid routing key.
Invalid actor or target kind.
Unknown action type.
Actor not authorized.
Target missing.
Derived event invalid.
Duplicate/retried requests.
Worker restart while helper/executor/cache state exists.
Large projection reads over unbounded materializations.
```

What must be durable:

```text
ActionRequest audit row.
ActionDecision audit row.
Accepted KernelEvent row, accepted only.
Materialized reader PStates.
Any state needed to reconstruct helper/executor/cache behavior after restart.
```

Explicitly out of scope for this block:

```text
Object-container Slice 1.
In-process chat ingester work.
New kernel feature design.
```

## Phase 1 - Plan

The Rama-shaped plan implied by the committed docs:

```text
Depots:
  V0 text/world request depot receives ActionRequest records.
  Future production modules split request depots by relatedness, ordering,
  locality, and topology consumers.
  Derived depots are added only after accepted KernelEvents when downstream work
  deserves a lane.

Topology:
  Interactive world actions use stream topology.
  The topology validates the common request envelope before action dispatch.
  The topology dispatches to action-specific interpretation only after common
  validation.
  Accepted requests derive KernelEvents and materialize PStates.
  Rejected requests materialize ActionDecisions only.

Routing / partition:
  ActionRequest carries :routing/key.
  The request depot should hash by :routing/key.
  Request id is identity/audit, not business locality.
  A fallback/quarantine key may exist for invalid/unrouteable requests, but
  missing target state is a normal routed rejection, not a routing failure.

PStates:
  $$requests-by-id
  $$decisions-by-id
  $$events-by-id
  $$artifacts
  $$artifact-heads
  $$text-revisions
  $$units-by-artifact
  $$unit-status-by-branch
  $$policy-by-target / actor capability materializations later
  projection PStates or query topologies for projection reads

Read surfaces:
  Read request by request id for audit/debug.
  Read decision by request id/decision id.
  Read accepted event by event id.
  Read artifact, head revision, units, statuses, and projections from PStates.
  Projection filtering that requires loops/joins should move into Rama query
  topology or denormalized projection PStates.

Idempotency:
  The docs name request id, idempotency key, client op id, and proposed event id
  as distinct concepts, but do not fully define duplicate request-id semantics.
  A production plan should prevent a retried request from creating a second
  accepted fact or overwriting the original request/decision audit trail.

Side effects:
  This block has no external side-effect lane. Compatibility helpers may append
  legacy records, but any local mirror/cache state needs a durable reconcile path
  or must be explicitly outside the world-kernel contract.

Restart/reconcile:
  PStates are durable. In-memory atoms/caches are not. Any app-state cache
  exposed by compatibility helpers needs either a Rama-backed read surface or a
  documented rebuild path.

I/O / subindexing:
  Small V0 maps are acceptable for proof, but production materializations that
  can exceed roughly 100 entries should be subindexed or exposed through query
  topologies. Avoid loading full unit/status maps for projection reads at scale.
```

## Phase 2 - Plan Validation

The reconstructed plan would have received a mixed validation:

```text
PASS:
  The semantic lifecycle correction is right: request first, Rama decision,
  accepted KernelEvent only on accepted decisions, rejected ActionDecision
  durable.

FAIL / NEEDS REPAIR BEFORE PRODUCTION:
  Duplicate request/id and idempotency semantics are not fully specified.
  The V0 physical layout is acknowledged as rough: random/request-id/locality
  drift was explicitly described as not the final Rama shape.
  Compatibility atoms/caches do not have durable restart/reconcile contracts.
  Projection/read surfaces are described but not fully planned as query
  topology or denormalized PStates.
  Non-subindexed nested collections have no committed size caps.
```

A forward Rama Phase 2 should have produced at least `minor-fail`, and likely
`major-fail` for production, unless the artifact was explicitly scoped as a V0
traceability proof.

## Phase 3 - Expected Implementation Shape

Expected files and responsibilities:

```text
src/app/server/rama/core.clj
  Shared pure envelope builders, validators, accepted/rejected decision helpers,
  and compatibility request builders.

src/app/server/rama/text_kernel.clj
  V0 text carrier module proving the shared lifecycle with real depot, topology,
  PStates, append helpers, and projection reads.

src/app/server/rama/util_fns.cljc
  Transitional adapter only. It should not be mistaken for durable world truth.

test/app/server/rama/text_kernel_test.clj
  Contract tests proving request/event separation, common validation,
  accepted/rejected materialization, and projection target refs.
```

Expected topology shape:

```text
*text-requests-depot hash-by :routing/key
  -> source ActionRequest
  -> common request validation
  -> action dispatch
  -> action-specific interpretation/policy/state reads
  -> ActionDecision
  -> if accepted, KernelEvent + materializations
```

Expected helper shape:

```text
append-action-request! performs one client-side depot append per logical write.
read-request/read-decision/read-event are audit helpers.
accepted-event-or-throw may be a convenience wrapper, but rejected decisions must
remain queryable.
```

Expected tests:

```text
ActionRequest is not KernelEvent.
No top-level or payload event id on requests.
:request/type / :action/type drift is invalid.
Malformed unknown action fails common validation before dispatch.
Rejected target-missing request is durable and creates no event.
Accepted requests materialize request, decision, event, PStates, and projections.
Duplicate request/id replay does not create a second accepted fact.
Compatibility atoms/caches are either excluded from contract or have restart
tests.
```

## Phase 4 - Expected Implementation Validation Focus

Most important checks for this block:

```text
Stream topology idempotency for duplicate/retried request records.
Partial failure in stream topology when one request writes request, decision,
event, branch, artifact, revision, units, and status PStates.
Correct partition alignment after depot routing by :routing/key.
Whether internal |hash hops are V0-only or break production locality.
Whether non-subindexed nested PState maps have explicit size limits.
Whether projection reads require full-map client-side loops.
Whether compatibility in-memory atoms can survive restart or are outside the
contract.
Single depot append per logical user operation.
Common validation before action-specific dispatch.
No accepted event for rejected requests.
```

## Phase 5 - Expected Tests

Tests should cover:

```text
Envelope builders and validators.
Accepted decision shape.
Rejected decision shape.
Request/event separation.
Routing-key validity.
Action/request type drift.
Hidden event id rejection.
Unknown malformed action validation before unknown dispatch.
Accepted text ingest path.
Accepted status path.
Rejected missing target path.
Duplicate request/id and/or idempotency-key retry.
Projection target refs.
No event row for rejected decisions.
Restart/reconcile behavior for any in-memory adapters, or explicit exclusion.
```

## Phase 6 - Expected Test Validation

The tests could pass while the module is still wrong if they:

```text
Use only happy-path unique request ids.
Do not replay the same request id or idempotency key.
Do not simulate topology retry after partial writes.
Do not test worker restart with util_fns atoms.
Do not assert locality or subindexing.
Do not measure/read projection I/O shape.
Only prove V0 IPC behavior, not production-shaped failure modes.
```

## Phase 7 - Expected Finish Evidence

Before trusting this slice as production-shaped, expected evidence would include:

```text
Focused test namespace passes.
Duplicate/retry probe passes.
No lingering Rama test processes after tests.
Explicit documentation of any V0-only transitional locality/caches.
Either durable replacement for util_fns atoms or tests proving their rebuild path.
Plan for subindexed/read-efficient projection materializations.
```

## Reconstruction Gaps

Committed docs do not fully define:

```text
Whether duplicate :request/id is idempotency identity or only audit identity.
Whether :idempotency/key is required for every client write or optional.
Where request/decision audit rows should live when request id and routing key
would partition differently.
How partial failure and retry avoid duplicate accepted events.
Concrete PState size caps for units/statuses/projection maps.
Restart/reconcile contract for compatibility atom mirrors in util_fns.
When the V0 transitional locality shape must be replaced.
```

## Reconstruction Verdict

The committed docs are strong enough to judge the core lifecycle and envelope
implementation, but incomplete for production idempotency, locality, projection
I/O, and compatibility-cache restart safety.

```text
PHASE_RECONSTRUCTION:complete
```
