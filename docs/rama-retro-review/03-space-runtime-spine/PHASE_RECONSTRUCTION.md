# Phase Reconstruction - Space Runtime Spine

Status: reconstructed.

This artifact reconstructs the Rama phase pack that should have existed for the
committed Space runtime spine. It is not a new design proposal.

## Source Evidence

```text
docs/current-mental-model/build/rama-retro-review/03-space-runtime-spine/BRIEF.md
docs/current-mental-model/architecture/dogfood-runtime/README.md
docs/current-mental-model/architecture/dogfood-runtime/three-depot-current-system.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-derived-contract.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-slice-roadmap.md
src/app/server/rama/dogfood/space.clj
src/app/server/rama/dogfood/llm.clj
test/app/server/rama/dogfood_space_test.clj
test/app/server/rama/dogfood_llm_test.clj

Commit anchors:
3ebb272 2026-05-10 world: add chat turns and context bundles
2cae951 2026-05-10 llm: bridge compose send through world
bc56b16 2026-05-10 llm: add world-first run controls
3ea1855 2026-05-10 world: materialize catalog and raw item indexes
6de66b0 2026-05-10 world: add slices overlays and derivatives
c812646 2026-05-10 world: add LLM fork flows
584f8c5 2026-05-10 world: ingest LLM patch proposals
340a173 2026-05-11 world: add rebuildable projections
1ef1cbd 2026-05-13 rama: split text kernel and rename space
c0dfafe 2026-05-13 docs: update rama space rename context
```

## Phase 0 - Implicit Spec

The committed docs imply Space is the user-facing truth layer. LLM and compute
tracks can execute, observe, and propose, but they must not secretly mutate
accepted Space meaning.

```text
User/world problem:
Softland chat and local-world activity must become durable, inhabitable Space
state. LLM runs are execution traces inside that place, not the place itself.

First physical record:
For user-authored LLM work, the first physical record Rama sees is a Space
ActionRequest in *space-action-depot.

New data entering the world:
- Space create requests
- turn compose/send requests
- Space-only turns: comments, slices, derivatives, patch accepts/rejects,
  cancel/abandon/compact/steer
- frozen ContextBundles
- derived LLM turn-run requests
- LLM control records derived from Space actions
- LLM observations that become raw execution trace, pending proposals, catalog
  material, projection updates, or control state

Core identities:
- :space/id owns a user-facing local world/container.
- :turn/id owns one user-visible move in a Space.
- :context-bundle/id owns the immutable model input for one LLM-triggering turn.
- :llm-turn-run/id owns one model execution attempt.
- :llm-thread/id owns model-side thread continuity.
- :llm-item/id owns raw LLM items.
- :patch-proposal/id owns a pending proposal, not accepted user truth.
- :idempotency/key prevents duplicate compose-and-send materialization.

Invariants:
- Space owns meaning and acceptance.
- LLM owns execution trace.
- ContextBundle is authored/frozen by Space and consumed by LLM.
- Every LLM-triggering turn has exactly one frozen ContextBundle in V0.
- Every LLM run references exactly one frozen ContextBundle.
- Space materializes accepted Space facts before downstream LLM execution can
  depend on them.
- UI/helper paths must not append user sends directly to *llm-depot.
- Controls are Space-first, then become LLM control records.
- Approval resolution must correspond to a real pending approval and preserve
  the native JSON-RPC id from the pending approval.
- Patch proposal observations become pending Space proposals, not accepted
  patches.
- Patch accept/reject are separate Space turns and separate from tool approval.
- Space-only turns must not mint context bundles or LLM runs.
- Projection PStates are derived and rebuildable from canonical Space/LLM
  inputs.
- Old World names are transitional prose only after the Space rename; runtime
  contracts should use Space/turn names.

Failure cases:
- duplicate compose-and-send
- duplicate or conflicting idempotency key
- missing Space for Space-only turns
- missing or invalid LLM run id for controls
- approval resolve without pending approval
- non-patch observations being misclassified as patch proposals
- LLM direct append bypassing Space for user-authored work
- projection corruption requiring rebuild

Explicitly out of scope:
- standalone LLM executor behavior except where it validates Space-first
  contract
- Object-container Slice 1
- in-process chat ingester work
```

## Phase 1 - Plan

The reconstructed Rama plan uses a Space stream topology plus a separate LLM
stream topology. Space mirrors LLM depots for downstream appends.

```text
Space depot:
*space-action-depot
  First record for user-authored Space/LLM work.
  Partitioned by :routing/key, normally [:space space-id].

Mirrored downstream depots:
*llm-depot
  Written by SpaceTopology only for derived LLM run requests.

*llm-control-depot
  Written by SpaceTopology only for derived controls.

LLM depots:
*llm-depot
*llm-claim-depot
*llm-obs-depot
*llm-control-depot
  LLM owns execution lifecycle and observations.

Topology choice:
Stream topologies for both Space and LLM.

Space PStates:
$$space-requests-by-id
$$space-decisions-by-id
$$space-events-by-id
$$spaces
$$space-graph
$$turns
$$turns-by-space
$$context-bundles
$$context-bundles-by-turn
$$send-by-idempotency
$$llm-run-requests
$$llm-run-by-turn
$$llm-controls
$$llm-control-by-turn
$$objects
$$artifact-graph
$$artifact-graph-in
$$slices
$$overlays
$$derivatives
$$space-patch-proposals
$$projection-chat-canvas
$$projection-object-detail
$$projection-object-relations

LLM PStates needed for Space-first contract:
$$llm-turn-runs
$$llm-thread-by-space
$$llm-turn-run-by-turn
$$llm-items-by-turn-run
$$llm-item-by-id
$$llm-approvals-pending
$$llm-approvals-by-run-id
$$llm-token-usage-by-run-id
$$llm-cost-by-thread
$$projection-run-detail

Partitioning/locality:
- Space requests route by [:space space-id].
- Request, thread, turn, bundle, run, object, and projection rows repartition
  to their own keys as needed.
- LLM run requests route by [:llm-run run-id].
- LLM observations route by run id.
- Hot Space UI reads are by space id, object id, and turn/run id.

Idempotency:
- Compose-and-send must dedupe at the Space bridge before duplicate turns,
  bundles, and LLM runs can be minted.
- Dedupe rows must include enough material identity to distinguish exact replay
  from conflicting reuse of an idempotency key.
- LLM run ids and turn ids must not be overloaded for independent executions.

ContextBundle:
- Frozen inside SpaceTopology.
- Must exist before the foreign append to *llm-depot.
- Execution options that affect model behavior are bundle-owned, not duplicated
  in LLM request payload.
- Immutable after freeze.

Controls:
- User control request enters *space-action-depot.
- Space accepts/rejects the control turn.
- If accepted, Space derives an LLM control record.
- Approval resolve must be tied to a real pending approval and must preserve
  native JSON-RPC request id from the pending approval row.
- Cancel/compact/steer must target an existing LLM run.

Observations/proposals:
- Raw LLM observations are folded by LLMTopology.
- Only proposal-like observations should become Space proposal actions.
- Patch proposal rows are pending until a Space patch accept/reject turn.
- Tool approval is not patch acceptance.

Projection/rebuild:
- Chat canvas, run detail, object detail, and object relation projections are
  derived from canonical Space/LLM PStates/depot history.
- Tests should prove same depot inputs rebuild same surfaces.

I/O/subindexing:
- Turns by Space, artifact graph edges, object relation projections, LLM items,
  approvals, pending inboxes, and observation buffers need bounds or subindexed
  shapes before production scale.
```

## Phase 2 - Plan Validation

The reconstructed plan is a good Rama architecture for a green MVP, but a
production Rama review should have pushed harder on cross-module validation,
idempotency conflict semantics, and PState size/read shapes.

```text
What is strong:
- Space-first user request contract.
- ContextBundle bridge is explicit and immutable in concept.
- LLM run request is downstream of accepted Space facts.
- Controls are Space-first.
- Patch proposals are pending until Space accepts/rejects.
- Projection rebuildability is explicitly named and tested.

Plan risks:
- It does not fully specify how Space reads or validates LLM-side pending
  approval/run state before deriving controls.
- It does not fully specify whether LLMTopology or SpaceTopology owns
  observation-to-proposal derivation.
- It does not define conflict behavior for same idempotency key with different
  target/payload.
- It leaves large ordered/index PStates as maps/vectors without explicit
  subindexing or query topology design.

Verdict the plan should have received:
PASS as a contract-first MVP plan, but with required pre-production follow-up
for control validation, observation proposal filtering, idempotency conflict
detection, and scalable PState shapes.
```

## Phase 3 - Expected Implementation Shape

```text
Expected namespaces:
src/app/server/rama/dogfood/space.clj
src/app/server/rama/dogfood/llm.clj

Expected Space code:
- *space-action-depot
- mirror-depot for *llm-depot and *llm-control-depot
- space-topology
- request branch for :space/create
- request branch for :turn/compose-and-send
- request branch for controls
- request branch for Space-only turns
- request branch for fork/reconcile
- request branch for patch proposal create/accept/reject
- idempotency PState for compose-and-send
- catalog/materialization helpers
- projection helpers

Expected LLM code:
- LLM run request/claim/observation/control depots
- LLM topology folds run lifecycle and raw observations
- pending approvals with native JSON-RPC ids
- run detail projection
- item indexes and cost rollups

Expected tests:
- Space create
- compose-and-send freezes context bundle
- Space-first send
- context bundle before run
- one bundle per run
- idempotency replay
- controls enter Space first
- approval timeout/stale approval
- tool approval not patch acceptance
- space-only turns do not trigger LLM
- fork binding before turn start
- patch proposal creation
- patch accept/reject
- projection rebuildability
- writer-asymmetry property
- non-patch observation does not create patch proposal
- approval resolve without pending approval is rejected or safely ignored
- idempotency-key conflict is rejected
```

## Phase 4 - Expected Implementation Validation Focus

```text
1. Can user-authored work bypass Space and append directly to *llm-depot?
2. Does Space write the ContextBundle before appending the LLM run request?
3. Does replaying the same send key avoid duplicate turns/bundles/runs?
4. Does conflicting reuse of an idempotency key reject rather than alias?
5. Are Space-only turns free of LLM side effects?
6. Do controls target real LLM runs and real pending approvals when required?
7. Are native approval ids copied from pending approval state, not trusted from
   UI payload alone?
8. Do only patch-like observations become pending patch proposals?
9. Do patch accept/reject turns stay separate from tool approvals?
10. Are raw LLM items immutable after slices/derivatives?
11. Are projections rebuildable from canonical state?
12. Are large nested maps/vectors bounded or subindexed?
```

## Phase 5 - Expected Tests

```text
Required positive tests:
- Space create materializes user-facing Space.
- compose-and-send creates thread, turn, context bundle, LLM run request.
- bundle exists before LLM run is observable.
- one bundle and one run per compose send.
- replayed idempotency key does not duplicate.
- control requests enter Space first.
- approval timeout is durable.
- fork creates child Space and delays execution until binding exists.
- patch proposal observation becomes pending proposal.
- patch accept/reject are Space turns, not tool approvals.
- projections rebuild to identical snapshots.

Required negative/adversarial tests:
- direct LLM user-send path absent or rejected.
- non-patch observation does not create patch proposal.
- approval resolve without pending approval is rejected or no-op.
- control against missing run is rejected or no-op with rejected decision.
- same idempotency key with conflicting target/payload is rejected.
- patch accept for missing proposal is rejected.
- large/paged read shapes are covered or explicitly scoped.
```

## Phase 6 - Expected Test Validation

```text
Tests could still pass while the implementation is wrong if:
- idempotency tests only replay the same Space/payload and never conflict the key
  across targets.
- patch proposal tests only send patch observations through the helper.
- approval tests always create a pending approval before resolving.
- Space-first tests prove a bridge exists but not that all helper paths filter
  by observation/control type.
- projection rebuild tests use tiny PStates and do not expose read-shape or
  subindexing issues.
```

## Phase 7 - Expected Finish Evidence

```text
Focused Space+LLM tests pass.
Runtime probe proves non-patch observations do not create patch proposals.
Runtime probe proves missing-approval controls do not create approved approval
rows.
Runtime probe proves idempotency conflicts are rejected or explicitly
documented as globally scoped exact replays.
No lingering Rama IPC processes after focused tests/probes.
```

## Reconstruction Gaps

Committed docs do not fully define:

```text
Exact material-equality check for idempotency replay vs conflict.
Whether idempotency keys are global, actor-scoped, or space-scoped.
How SpaceTopology should validate LLM pending approval/run state before controls.
Whether LLMTopology or a helper/service appends proposal actions back to Space.
Which observation types are allowed to become Space proposal actions.
What happens to proposal/control/action records targeting missing entities.
Subindex/bounds requirements for turns, object relations, item histories,
pending inboxes, approval maps, and observation buffers.
```

## Reconstruction Verdict

The committed docs are strong enough to judge the Space-first contract and most
of the committed implementation. The green MVP plan is coherent, but it needed
additional Phase 1 validation around cross-module control checks,
observation-to-proposal filtering, idempotency conflict semantics, and scalable
PState shapes.

```text
PHASE_RECONSTRUCTION:complete
```
