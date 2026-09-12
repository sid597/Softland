# Phase Reconstruction - Compute Track

Status: reconstructed.

This artifact reconstructs the Rama phase pack that should have existed for the
committed compute slice. It is not a new design proposal.

## Source Evidence

```text
docs/current-mental-model/build/rama-retro-review/02-compute-track/BRIEF.md
docs/current-mental-model/architecture/dogfood-runtime/README.md
docs/current-mental-model/architecture/dogfood-runtime/compute-track.md
docs/current-mental-model/architecture/dogfood-runtime/slice-a-compute-run-command.md
docs/current-mental-model/architecture/dogfood-runtime/three-depot-current-system.md
src/app/server/rama/dogfood/compute.clj
test/app/server/rama/dogfood_compute_test.clj

Commit anchors:
77833e6 2026-05-04 feat: add compute TaskGlobal executor
23f7ceb 2026-05-04 docs: update compute TaskGlobal architecture
db4f105 2026-05-03 docs: clarify compute executor tick ownership
ab362a7 2026-05-03 docs: update compute runtime context
99f1a20 2026-05-02 Capture Slice A handoff and cross-model trace
```

## Phase 0 - Implicit Spec

The committed docs imply this slice exists to make physical command execution
observable and governable through Rama instead of through a hidden local process
runner.

```text
User/world problem:
Softland should dogfood itself. Build/test/run/serve/deploy style execution
must enter the world as Rama-owned state so the UI can watch durable truth, not
a side channel.

First concrete slice:
:compute/run-command.

New data entering the world:
- compute request records
- compute claim records
- compute observations from physical execution
- accepted/rejected run decisions
- durable run lifecycle rows
- live UI views of status/stdout/stderr/exit

Primary identities:
- :run/id owns local command lifecycle and ordering.
- :request/id identifies the user/client request that asked for the run.
- :executor/task-id owns a task-local pending inbox.
- :executor/id identifies the executor instance claiming work.
- :claim-token gates physical observations.
- :sequence orders observations within a granted run.

Invariants:
- The first physical record for a command is a compute request depot record.
- Rama accepts or rejects the request before any process can spawn.
- The executor cannot write PStates.
- The executor reads PStates and appends claim/observation depot records.
- The topology is the only writer of compute truth and views.
- A process can spawn only after a durable claim grant is visible in
  $$compute-runs.
- First valid claim wins; later claims no-op.
- Observations must match an existing run and the granted claim token.
- Observations must be sequence-safe: duplicates are ignored, gaps are buffered,
  and in-order records are folded exactly once.
- Process failure is a run status, not request rejection.
- UI views must not expose claim-token.
- stdout/stderr tails and observation-error lists must be bounded.

Scale expectations:
- Observation traffic can be high-volume and append-heavy.
- Pending execution is task-local, so each TaskGlobal should read its own inbox.
- The executor reconcile path should not scan an unbounded collection every tick
  without an explicit V0 cap.

Durability expectations:
- Request decision, run lifecycle, claim grant, observations, and UI projection
  are durable Rama state.
- Process-local executor registry is not truth; it may help avoid duplicate
  local submissions but must not be required to reconstruct the run state.

Replay/audit expectations:
- Requests, claims, and observations remain in depots.
- PStates materialize the latest durable lifecycle and UI view.
- Invalid requests must be rejected as decisions.
- Invalid claims and observations must not create truth or crash the topology.

Explicitly out of scope:
- LLM agent execution.
- Transcript capture.
- Cancel and restart-reconcile are deferred from Slice A.0 to A2.
- New compute operation types beyond :compute/run-command.
```

## Phase 1 - Plan

The reconstructed Rama plan is a stream topology with three input depots and
module-owned async execution.

```text
Depots:
*compute-depot
  First physical record for command intent.
  Partitioned by :run/id.

*compute-claim-depot
  Executor asks Rama for the right to spawn one run.
  Partitioned by :run/id.

*compute-obs-depot
  Executor reports physical process observations.
  Partitioned by :run/id.
  Observation branch should be retry-safe.

Topology choice:
Stream topology.

Reason:
The request/claim/observation path needs low-latency state changes and acked
coordination before spawning OS work. Physical work itself must not run on the
topology task thread.

PStates:
$$compute-runs
  Key: run-id.
  Lifecycle truth: pending, launching, running, succeeded, failed.
  Stores argv/cwd, claim owner/token, pid, exit code, sequence cursor, bounded
  tails, bounded errors, and any buffered out-of-order observations.

$$compute-decisions-by-run-id
  Key: run-id.
  Full request decision for accepted/rejected request.

$$compute-pending-by-task
  Key: executor-task-id, then run-id.
  Per-task inbox for TaskGlobal executor discovery.

$$compute-views
  Key: run-id.
  Bounded UI projection. Must hide claim-token.

Partitioning/locality:
- Request branch starts on :run/id.
- Claim branch starts on :run/id.
- Observation branch starts on :run/id.
- $$compute-runs, $$compute-decisions-by-run-id, and $$compute-views are colocated
  by run-id.
- $$compute-pending-by-task is keyed by executor-task-id, so request/claim
  branches intentionally repartition when adding/removing pending entries.

Request idempotency:
- The plan needs explicit semantics for repeated :run/id and :request/id.
- A replayed or duplicate request must not reset a terminal run or enqueue a
  second physical command.
- Same request/run replay should return or preserve the existing decision.
- Conflicting payload for an existing run id should be rejected or quarantined,
  not overwrite lifecycle truth.

Claim idempotency:
- Claim records are accepted only when the run row exists, status is :pending,
  run-id matches, and executor/task-id matches.
- First valid claim atomically moves :pending -> :launching and removes the
  pending inbox row.
- Later claims no-op and cannot spawn because the executor must read the durable
  grant before spawning.

Observation idempotency:
- Observation branch must first verify that a run row exists.
- Observation must match the granted claim token.
- Observation type and sequence must be validated before folding.
- Sequence less than or equal to last applied is duplicate/old and must no-op.
- Sequence gap must buffer within an explicit bound or dead-letter/quarantine.
- Sequence gap drain must apply buffered observations in order.
- Unknown-run, wrong-token, malformed, or post-terminal observations must not
  crash the topology.

Side-effect gate:
- The only side effect is OS process spawn.
- Spawn is allowed only after the executor reads $$compute-runs and sees
  :status :launching plus its own :claimed-by and :claim-token.
- Topology never blocks on command execution.

Restart/reconcile:
- Pending rows survive process restart and can be claimed by the TaskGlobal.
- Process-local registry can be lost.
- Slice A.0 explicitly defers full restart/stall reconciliation, but the design
  must still avoid duplicate side effects under duplicate appends and must not
  crash on bad observation records.

I/O and subindexing:
- Pending inbox scans must be bounded for the expected local executor shape or
  represented with a subindexed/range-friendly structure.
- Observation buffer and tails must have bounds.
- Query/read helpers should avoid multiple client-side roundtrips for hot UI
  reads once the slice graduates beyond V0.
```

## Phase 2 - Plan Validation

The reconstructed plan is Rama-shaped in the large, but would not pass a
production adversarial review without amendments.

```text
What is strong:
- Request/claim/observation are separated into depots.
- Physical execution is out of topology.
- Claim grant is durable before spawn.
- Views are materialized by topology.
- The TaskGlobal executor matches the AOR-style async execution lesson.

Plan risks:
- Duplicate/replayed :run/id and :request/id semantics are not made explicit in
  the committed architecture docs.
- Unknown-run observations are described as validated, but the invalid-record
  destination is not specified.
- Observation gap buffering says "NEVER drop" but does not define a memory/disk
  bound or dead-letter policy.
- Pending-by-task is described as a task-local inbox, but no scan bound, lease,
  or subindex requirement is specified.
- Restart/stall recovery is explicitly deferred, which is acceptable for a
  labeled Slice A.0 proof but not for a production compute substrate.

Wrong or ambiguous locality decisions:
- Hashing request/claim/observation by :run/id is correct for lifecycle
  decisions.
- $$compute-pending-by-task intentionally repartitions by task-id. This is valid
  only if the task inbox remains small/bounded or subindexed.

PState shape concerns:
- $$compute-runs contains nested maps/vectors for tails, errors, and
  obs-buffer.
- Those collections need explicit bounds or subindexing rules before scale.

Retry/restart concerns:
- Request replay and duplicate append must be idempotent.
- Observation replay is partially covered by sequence folding, but missing-run
  and malformed observations need a safe path.
- Launching/running runs with lost executor state need a later lease/stall
  mechanism.

Verdict the plan should have received:
FAIL for production, PASS only as a clearly labeled Slice A.0 proof after adding
explicit warnings that request idempotency, invalid observation handling,
bounded pending scans, bounded buffers, and restart/stall recovery are not done.
```

## Phase 3 - Expected Implementation Shape

```text
Expected namespace:
src/app/server/rama/dogfood/compute.clj

Expected tests:
test/app/server/rama/dogfood_compute_test.clj

Expected depots:
*compute-depot
*compute-claim-depot
*compute-obs-depot

Expected topology:
compute-run-command-topology with one <<sources block and three branches:
- request branch
- claim branch
- observation branch

Expected PStates:
$$compute-runs
$$compute-decisions-by-run-id
$$compute-pending-by-task
$$compute-views

Expected TaskGlobal:
ComputeExecutorTaskGlobal declared in the Rama module.
prepareForTask captures task id, opens foreign depot/PState clients, keeps a
process-local registry, schedules reconcile, and runs command workers outside
the topology task thread.

Expected request branch:
- validate envelope, target, action, capability, argv, cwd
- reject invalid/unauthorized requests
- check existing run/decision for idempotency or conflict
- write decision
- write initial run row only for first accepted request
- write view
- enqueue pending row by executor-task-id

Expected claim branch:
- select run by run-id
- grant only if row exists, status is :pending, task id matches, and claim is
  valid
- write claimed run/view
- remove pending row
- no-op otherwise

Expected observation branch:
- select run by run-id
- handle nil run without exception
- validate token/type/sequence
- fold started/stdout/stderr/exit in order
- ignore duplicates
- buffer bounded gaps or dead-letter them
- write run/view

Expected helper APIs:
- append-run-command!
- append-claim!
- append-observation!
- read-run
- read-decision
- read-view
- read-pending
- await helpers for IPC tests

Expected tests:
- automatic TaskGlobal happy path
- manual local path
- failed process exit becomes run :failed
- double claim and wrong-token observation
- duplicate request/replay does not reset or respawn
- unknown-run observation does not crash or create truth
- out-of-order observation drains correctly
- duplicate observation is idempotent
- missing sequence/gap is bounded
- process start exception becomes run failed
```

## Phase 4 - Expected Implementation Validation Focus

```text
1. Can any observation record crash the stream topology?
2. Can an observation create or mutate run truth without a prior accepted
   request?
3. Can a duplicate request or same run id reset a completed run?
4. Can two executors both spawn the same run?
5. Can a wrong claim token write stdout/stderr/exit?
6. Are duplicate and out-of-order observations retry-safe?
7. Are stdout/stderr/error tails bounded?
8. Is obs-buffer bounded or otherwise safe?
9. Is pending-by-task scan bounded enough for the reconcile tick?
10. Does the UI view hide claim-token?
11. Does process failure become run state rather than request rejection?
12. Does the TaskGlobal registry being process-local avoid becoming hidden
    truth?
```

## Phase 5 - Expected Tests

```text
Required positive tests:
- accepted run request creates decision, run row, view, and pending row
- TaskGlobal executor picks up normal request automatically
- manual local helper path claims before spawning
- echo succeeds and captures stdout
- false exits as :failed while request remains accepted

Required negative/idempotency tests:
- invalid request is rejected and does not enqueue pending work
- duplicate run/request replay is idempotent or rejected and never respawns
- conflicting duplicate run id cannot overwrite terminal state
- first claim wins
- second claim cannot spawn
- wrong-token observation cannot write output
- unknown-run observation cannot crash topology
- malformed observation cannot crash topology
- duplicate observation sequence no-ops
- out-of-order observation buffers then drains
- unclosed sequence gap is bounded or dead-lettered
- process start exception materializes :failed
```

## Phase 6 - Expected Test Validation

Tests could pass while the implementation is wrong if they only prove the happy
path plus one double-claim example.

```text
False confidence patterns:
- Echo and false prove physical execution, but not retry safety.
- Double-claim proves one gate, but not request replay.
- Wrong-token after a valid run proves token rejection, but not unknown-run or
  malformed observation handling.
- Automatic TaskGlobal test proves module-owned executor exists, but not restart
  or stall recovery.
- Manual local path can hide timing/pending-inbox behavior of the real
  TaskGlobal path.
- Bounded stdout/stderr tests can pass while obs-buffer remains unbounded.
```

## Phase 7 - Expected Finish Evidence

```text
Focused compute test namespace passes.
Runtime probe proves unknown-run observations are safe.
Runtime probe proves duplicate run/request cannot reset or respawn.
Runtime probe proves duplicate observations are idempotent.
Runtime probe proves out-of-order observations drain in order.
Explicit statement of A.0 restart/stall limitations remains visible.
No lingering Rama IPC or executor threads after focused tests.
```

## Reconstruction Gaps

Committed docs do not fully define:

```text
Whether :run/id, :request/id, or both are the idempotency identity.
What happens when the same run id arrives with a different payload.
Where invalid claim records are recorded, if anywhere.
Where unknown-run or malformed observations are recorded.
How large an obs-buffer can grow before being trimmed, rejected, or moved.
How large a pending task inbox can grow before requiring subindexing/paging.
Whether Slice A.0 is allowed to be crash-unsafe for launching/running work or
only missing full restart automation.
```

## Reconstruction Verdict

The committed docs are strong enough to judge the main Rama shape and the
request/claim/observation contract, but they leave production-critical
idempotency, invalid observation, bounded buffering, pending scan, and
restart/stall details under-specified.

```text
PHASE_RECONSTRUCTION:complete
```
