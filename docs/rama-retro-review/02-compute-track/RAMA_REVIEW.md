# Rama Review - Compute Track

Status: reviewed.

## Contract Restatement

The compute slice is intended to make physical command execution a Rama-owned
state machine.

```text
First depot/input record:
*compute-depot receives a :compute/run-command request keyed by :run/id.

Record type:
The first record is a request, not an accepted fact. The accepted fact is the
topology-created decision/run lifecycle state.

Acceptance/rejection:
The request branch validates and writes an ActionDecision-like row in
$$compute-decisions-by-run-id. Accepted requests create a pending run row and
pending executor inbox entry. Rejected requests must not spawn work.

Durable accepted truth:
$$compute-runs[run-id], $$compute-decisions-by-run-id[run-id], and
$$compute-pending-by-task[executor-task-id][run-id].

Executor gate:
The executor appends a claim. The claim branch grants only when the durable run
row is :pending. The executor may spawn only after reading its own durable
:launching grant with matching :claimed-by and :claim-token.

Observation truth:
The executor reports :started/:stdout/:stderr/:exit through *compute-obs-depot.
The topology validates token and sequence, then folds observations into
$$compute-runs and $$compute-views.

Projection/read surface:
The UI reads $$compute-views. The view includes status/stdout/stderr/exit and
hides claim-token.
```

## Rama-First Reconstruction

```text
New data:
Compute requests, claims, process observations, decisions, run lifecycle state,
and UI projections.

Entity owning local ordering:
:run/id owns run lifecycle. :sequence owns observation order inside one granted
run.

First physical depot record:
The compute request in *compute-depot.

Request/proposal vs accepted fact vs observation:
- request: :compute/run-command asks to execute
- accepted fact: topology decision/run row
- observation: executor report about physical work after claim grant

Authoritative decision:
The Rama topology, not the UI and not the executor.

Depot partition:
Request, claim, and observation depots hash by :run/id so the hot lifecycle
decision is colocated.

PState colocation:
Runs, decisions, and views belong by run-id. Pending inbox intentionally keys by
executor-task-id and therefore needs a controlled repartition and bounded scan.

Fast reads:
read-view by run-id for UI; read-run by run-id for executor grant/final state;
read-pending by executor-task-id for reconcile.

Retries/duplicates/restarts/out-of-order:
Requests can be appended twice. Claims can race. Observations can duplicate,
arrive out of order, reference the wrong token, or arrive for an unknown run.
Executor process state can be lost on worker restart.

Side effects:
The OS child process spawn is the side effect. The durable grant row is the
side-effect gate.

Required evidence:
Happy path, failed process, double claim, wrong token, duplicate request,
unknown observation, duplicate observation, out-of-order observation, and
bounded pending/buffer behavior.
```

## Phase Diagnosis

```text
Phase 0 - Implicit spec:
Mostly sound. The committed docs correctly identify the back-arrow rule:
actors append depots, topology writes PStates, UI/executor read PStates.

Phase 1 - Plan:
Strong high-level shape, but incomplete production plan. It does not nail down
request idempotency, invalid-observation disposition, pending scan bounds, or
obs-buffer bounds. Restart/stall recovery is explicitly deferred.

Phase 2 - Plan validation:
Should have passed only as a V0 Slice A.0 proof. A production Rama review
should have failed the missing duplicate/retry and bounded-storage details.

Phase 3 - Implementation:
Main module shape exists, including TaskGlobal. The observation branch has an
implementation bug: it folds a nil run row and can fatal the topology.

Phase 4 - Implementation validation:
Happy path and double-claim behavior work, but bad-record and replay probes
expose production correctness failures.

Phase 5 - Tests:
Focused tests cover automatic execution, manual execution, failed process, and
double claim/wrong token. They do not cover unknown observations or duplicate
run requests.

Phase 6 - Test validation:
The green tests prove the slice shape, not retry/idempotency safety.

Phase 7 - Finish/runtime:
Focused test namespace passes. Runtime probes add evidence for two failing
paths.
```

Primary Failure Phase: phase-3

## Files Reviewed

### Phase Reconstruction

```text
docs/current-mental-model/build/rama-retro-review/02-compute-track/BRIEF.md
docs/current-mental-model/build/rama-retro-review/02-compute-track/PHASE_RECONSTRUCTION.md
```

### Code

```text
src/app/server/rama/dogfood/compute.clj
```

### Tests

```text
test/app/server/rama/dogfood_compute_test.clj
```

### Docs

```text
docs/current-mental-model/architecture/dogfood-runtime/README.md
docs/current-mental-model/architecture/dogfood-runtime/compute-track.md
docs/current-mental-model/architecture/dogfood-runtime/slice-a-compute-run-command.md
docs/current-mental-model/architecture/dogfood-runtime/three-depot-current-system.md
```

### Commit Anchors

```text
77833e6 2026-05-04 feat: add compute TaskGlobal executor
23f7ceb 2026-05-04 docs: update compute TaskGlobal architecture
db4f105 2026-05-03 docs: clarify compute executor tick ownership
ab362a7 2026-05-03 docs: update compute runtime context
99f1a20 2026-05-02 Capture Slice A handoff and cross-model trace
```

## Topology / Depot / PState Inventory

### Depots

```text
*compute-depot
  Declared at src/app/server/rama/dogfood/compute.clj:609.
  Partitioned by :run/id.

*compute-claim-depot
  Declared at src/app/server/rama/dogfood/compute.clj:610.
  Partitioned by :run/id.

*compute-obs-depot
  Declared at src/app/server/rama/dogfood/compute.clj:611.
  Partitioned by :run/id.
```

### Topologies

```text
compute-run-command-topology
  Declared at src/app/server/rama/dogfood/compute.clj:616.
  Request branch: src/app/server/rama/dogfood/compute.clj:623-638.
  Claim branch: src/app/server/rama/dogfood/compute.clj:640-651.
  Observation branch: src/app/server/rama/dogfood/compute.clj:653-660.
```

### PStates

```text
$$compute-runs
  src/app/server/rama/dogfood/compute.clj:617

$$compute-decisions-by-run-id
  src/app/server/rama/dogfood/compute.clj:618

$$compute-pending-by-task
  src/app/server/rama/dogfood/compute.clj:619

$$compute-views
  src/app/server/rama/dogfood/compute.clj:620
```

### Query / Read Helpers

```text
read-run: src/app/server/rama/dogfood/compute.clj:711-713
read-decision: src/app/server/rama/dogfood/compute.clj:715-717
read-view: src/app/server/rama/dogfood/compute.clj:719-721
read-pending: src/app/server/rama/dogfood/compute.clj:723-728
await helpers: src/app/server/rama/dogfood/compute.clj:730-759
```

### Client Append Helpers

```text
append-run-command!: src/app/server/rama/dogfood/compute.clj:686-691
append-claim!: src/app/server/rama/dogfood/compute.clj:693-698
append-observation!: src/app/server/rama/dogfood/compute.clj:700-705
```

### TaskGlobals / Caches / Executors

```text
ComputeExecutorTaskGlobal:
  src/app/server/rama/dogfood/compute.clj:563-602

Process-local registry:
  src/app/server/rama/dogfood/compute.clj:569, 579-586

Reconcile loop:
  src/app/server/rama/dogfood/compute.clj:523-536, 587-597

Command worker:
  src/app/server/rama/dogfood/compute.clj:824-871

Manual local helper:
  src/app/server/rama/dogfood/compute.clj:873-887
```

## Findings

### F1. Unknown-run observations can fatal the stream topology

Severity: critical

Evidence:
- `docs/current-mental-model/architecture/dogfood-runtime/slice-a-compute-run-command.md:175-188` says the observation branch validates and folds observations by token and sequence.
- `docs/current-mental-model/architecture/dogfood-runtime/slice-a-compute-run-command.md:482-485` says the observation branch validates claim-token and sequence.
- `src/app/server/rama/dogfood/compute.clj:653-657` selects the run row and immediately calls `fold-observation`.
- `src/app/server/rama/dogfood/compute.clj:452-455` computes expected sequence with `(:last-seq run-row)` and casts it to long.
- If the run row is nil, that cast throws before the code can record a safe invalid-observation error.

Runtime trace:
1. Start the compute IPC runtime.
2. Append `{:run/id "run_unknown_obs", :claim-token "bad-token", :observation/type :stdout, :sequence 0}` to `*compute-obs-depot` before any request.
3. Rama logs a fatal topology error:
   `NullPointerException: Cannot invoke "java.lang.Number.doubleValue()" because "x" is null`
   at `app.server.rama.dogfood.compute$fold_observation.invokeStatic(compute.clj:455)`.
4. Command exits with code 1 via Rama callback failure.

Rama concern:
Depot inputs are append-only physical records and can be duplicated, reordered,
or malformed. A stream topology must treat invalid records as data, not as fatal
exceptions that can stop processing.

Failure phase:
phase-3

Consequence:
An executor bug, stale observation, replay artifact, or bad client append can
crash the compute topology. This violates the request-first lifecycle because
an observation can affect topology health without any accepted run.

Repair shape:
Guard the observation branch before sequence folding. If no run row exists,
write a bounded invalid-observation/dead-letter record or no-op with telemetry.
Only call the sequence folder after row existence, token, type, and sequence
shape are known safe.

### F2. Duplicate run requests can reset completed lifecycle state and enqueue another spawn

Severity: high

Evidence:
- `docs/current-mental-model/architecture/dogfood-runtime/slice-a-compute-run-command.md:108-121` says the UI mints run-id, appends request, and Rama writes pending run state.
- `src/app/server/rama/dogfood/compute.clj:623-638` request branch writes decision/run/view/pending rows without selecting the existing run or decision first.
- `src/app/server/rama/dogfood/compute.clj:206-208` derives decision id from run id, so a second request for the same run overwrites the same decision key.
- `src/app/server/rama/dogfood/compute.clj:266-291` builds a fresh pending run row every time the accepted request branch runs.

Runtime trace:
1. Append run `run_dup_compute_wait` with command `["echo" "first"]`.
2. Wait for pending, manually claim/run it, and wait for terminal view.
3. Append another request with the same run id and command `["echo" "second"]`.
4. Result:
   `{:claim-state :granted-to-us, :spawned true, :first-status :succeeded, :first-stdout [first], :after-duplicate-status :pending, :after-duplicate-argv [echo second], :pending-after-duplicate true, :decision-request req_dup_compute_b}`

Rama concern:
Request depot appends and stream processing can be retried or duplicated. The
side-effect gate prevents double spawn only after a run is in the claim path; it
does not protect against replay resetting the run to pending.

Failure phase:
phase-1

Consequence:
A duplicate/replayed/conflicting request can erase terminal state and place the
same run id back into the pending executor inbox. That can produce a second OS
process for the same logical run.

Repair shape:
Define idempotency identity for `:run/id` and `:request/id`. In the request
branch, select existing decision/run state before writing. Preserve existing
terminal state for replay, return/reuse an existing decision for identical
requests, and reject or quarantine conflicting duplicates.

### F3. Pending inbox and observation buffer are not bounded or subindexed

Severity: medium

Evidence:
- `docs/current-mental-model/architecture/dogfood-runtime/three-depot-current-system.md:191-195` calls observations streaming/high-volume and append-heavy.
- `docs/current-mental-model/architecture/dogfood-runtime/slice-a-compute-run-command.md:80-85` describes pending and views, with views bounded.
- `docs/current-mental-model/architecture/dogfood-runtime/slice-a-compute-run-command.md:184-188` says out-of-order observations are stored in `:obs-buffer` and never dropped.
- `src/app/server/rama/dogfood/compute.clj:288-291` initializes `:obs-buffer {}`, `:stdout-tail []`, `:stderr-tail []`, and bounded error/tail structures.
- `src/app/server/rama/dogfood/compute.clj:472-473` stores future observations in `:obs-buffer` with no size cap.
- `src/app/server/rama/dogfood/compute.clj:619` declares `$$compute-pending-by-task` as a nested map without subindex metadata.
- `src/app/server/rama/dogfood/compute.clj:523-531` reads the whole pending map, sorts all keys, and appends claims every reconcile tick.

Runtime trace:
1. Any missing observation sequence causes later observations to accumulate in
   `:obs-buffer`.
2. Any executor task with many pending runs causes `read-pending` to fetch the
   whole inbox and `claim-new-pending-runs!` to sort all run ids.
3. The committed tests do not exercise large pending inboxes or permanent
   sequence gaps.

Rama concern:
Large nested collections in PStates require bounds or subindexing. Repeated full
map scans from a TaskGlobal every reconcile tick turn a convenient V0 shape into
production I/O pressure.

Failure phase:
phase-1

Consequence:
One missing sequence can grow a run row without bound. A large task-local inbox
can make the reconcile loop expensive and latency-sensitive.

Repair shape:
Add explicit Slice A.0 caps, or move pending/gap buffering to subindexed,
range-friendly PState shapes. Add dead-letter behavior for impossible sequence
gaps and page/limit pending scans.

### F4. Restart/stall safety is knowingly deferred, so this cannot be treated as production-complete

Severity: medium

Evidence:
- `docs/current-mental-model/architecture/dogfood-runtime/slice-a-compute-run-command.md:6-7` says cancel plus restart-reconcile are deferred to Slice A2.
- `docs/current-mental-model/architecture/dogfood-runtime/slice-a-compute-run-command.md:232-236` names AOR's separate retry/stall detection for dead nodes or lost work.
- `docs/current-mental-model/architecture/dogfood-runtime/slice-a-compute-run-command.md:297-303` says later slices need explicit status tracking, exceptions as observations, retry policy, and stall/lost-run detection.
- `src/app/server/rama/dogfood/compute.clj:538-561` keeps TaskGlobal executor states in process-local atom/registries and closes them on IPC close.

Runtime trace:
1. Pending rows are durable and can be rediscovered.
2. Once a run is :launching or :running, loss of the process-local worker state
   has no reviewed lease/stall recovery in Slice A.0.
3. The committed docs admit this is deferred.

Rama concern:
Rama PStates are durable and worker restart does not replay depot history to
reconstruct process-local side effects. Production async execution needs explicit
leases, heartbeats, retry/stall handling, or a documented non-production scope.

Failure phase:
phase-1

Consequence:
The slice is a valid proof of the architecture, but not a complete durable
compute executor. A claimed run can be stranded without a later reconciliation
policy.

Repair shape:
Keep the A.0 limitation visible. Before production use, add lease/heartbeat,
stall detection, and recovery rules for :launching/:running states.

### F5. Tests prove the spine, not the adversarial Rama contract

Severity: medium

Evidence:
- `test/app/server/rama/dogfood_compute_test.clj:35-54` tests automatic
  TaskGlobal execution.
- `test/app/server/rama/dogfood_compute_test.clj:56-80` tests manual echo path.
- `test/app/server/rama/dogfood_compute_test.clj:82-98` tests failed process
  exit materialization.
- `test/app/server/rama/dogfood_compute_test.clj:100-147` tests double claim
  and wrong-token observation after a valid run exists.
- No committed test covers unknown-run observation, malformed observation,
  duplicate run request, duplicate observation, out-of-order observation drain,
  permanent sequence gap, or process start exception.

Runtime trace:
1. Focused namespace passes: 4 tests, 36 assertions.
2. Unknown-run observation probe fails fatally.
3. Duplicate-run probe shows terminal state reset to pending.

Rama concern:
The green tests validate the intended flow but not the retry, duplicate,
restart, or bad-input behavior that Rama stream topologies must survive.

Failure phase:
phase-5

Consequence:
The implementation can look complete while missing the exact cases most likely
to break in production.

Repair shape:
Add focused tests for every depot branch's invalid/replay path before changing
code, then repair to those contracts.

## Phase-4 Style Check Matrix

### Redundant Conditionals

Verdict: PASS.

Evidence:
No redundant branch structure stood out in the reviewed compute module.

### Consecutive Keypath

Verdict: PASS.

Evidence:
The nested keypaths for `$$compute-pending-by-task` are intentional task-id then
run-id access (`src/app/server/rama/dogfood/compute.clj:638,651`).

### Select-Compute-Transform

Verdict: MIXED.

Evidence:
Claim and observation branches select the run row, compute a new value, then
write with `termval` (`src/app/server/rama/dogfood/compute.clj:643-660`), which
is the right shape. The request branch fails to select existing run/decision
state before overwriting (`src/app/server/rama/dogfood/compute.clj:623-638`).

### Unnecessary nil->val

Verdict: N/A.

Evidence:
No relevant nil-to-value cleanup pattern identified.

### :allow-yield?

Verdict: MIXED.

Evidence:
The topology local selects are point reads by run id (`src/app/server/rama/dogfood/compute.clj:643,656`), so
`:allow-yield?` is not needed there. The TaskGlobal pending scan reads an entire
task inbox outside a query topology (`src/app/server/rama/dogfood/compute.clj:523-531,723-728`), so the broader
read-shape concern remains.

### Non-Subindexed Collections Without Size Limits

Verdict: FAIL.

Evidence:
`$$compute-pending-by-task` is a nested map without subindexing
(`src/app/server/rama/dogfood/compute.clj:619`). `:obs-buffer` has no size cap
(`src/app/server/rama/dogfood/compute.clj:288,472-473`). stdout/stderr and error
lists are bounded (`src/app/server/rama/dogfood/compute.clj:34-36,366-390`).

### Stream Topology Idempotency

Verdict: FAIL.

Evidence:
Duplicate run/request append resets terminal lifecycle state and re-enqueues
pending work. The request branch does not check existing state before writing
(`src/app/server/rama/dogfood/compute.clj:623-638`).

### Partial Failure In Stream Topologies

Verdict: FAIL.

Evidence:
The claim gate prevents double claim for the tested case, but an unknown
observation can throw fatally in the observation branch. Restart/stall recovery
for :launching/:running is explicitly deferred (`slice-a-compute-run-command.md:297-303`).

### Single Depot Append Per Client Operation

Verdict: PASS.

Evidence:
`append-run-command!`, `append-claim!`, and `append-observation!` each perform
one `foreign-append!` (`src/app/server/rama/dogfood/compute.clj:686-705`).
Command execution legitimately appends multiple observation records because
stdout/stderr/exit are separate depot facts (`src/app/server/rama/dogfood/compute.clj:817-856`).

### Application-State Caches Survive Restart

Verdict: MIXED.

Evidence:
The process-local registry is not the official truth and pending rows are in
Rama. However, launching/running work has no restart/stall recovery in the
reviewed slice, and executor state is process-local
(`src/app/server/rama/dogfood/compute.clj:538-561`).

### No Reimplementation Of Built-In Operations

Verdict: PASS.

Evidence:
No custom replacement for a Rama built-in operation was identified.

## Tests / Commands

Focused test namespace:

```text
clojure -M:test -e "(require 'clojure.test 'app.server.rama.dogfood-compute-test) (clojure.test/run-tests 'app.server.rama.dogfood-compute-test) (shutdown-agents) (System/exit 0)"

Testing app.server.rama.dogfood-compute-test

Ran 4 tests containing 36 assertions.
0 failures, 0 errors.
{:test 4, :pass 36, :fail 0, :error 0, :type :summary}
```

Unknown-run observation probe:

```text
clojure -M:test -e "(require '[app.server.rama.dogfood.compute :as c]) ... append observation for run_unknown_obs before request ..."

Result:
Command exited with code 1.
Rama logged:
NullPointerException: Cannot invoke "java.lang.Number.doubleValue()" because "x" is null
at app.server.rama.dogfood.compute$fold_observation.invokeStatic(compute.clj:455)
```

Duplicate run request probe:

```text
clojure -M:test -e "(require '[app.server.rama.dogfood.compute :as c]) ... run echo first, append same run id with echo second ..."

Result:
{:claim-state :granted-to-us,
 :spawned true,
 :first-status :succeeded,
 :first-stdout [first],
 :after-duplicate-status :pending,
 :after-duplicate-argv [echo second],
 :pending-after-duplicate true,
 :decision-request req_dup_compute_b}
```

## Recommended Repair Queue

```text
1. Add tests for unknown-run observation, malformed observation, duplicate
   request/run replay, conflicting duplicate run id, duplicate observation, and
   out-of-order observation drain.
2. Guard observation folding so nil/malformed rows cannot throw.
3. Add explicit invalid-observation disposition: no-op with telemetry,
   bounded error PState, or dead-letter PState.
4. Define request idempotency semantics for :run/id and :request/id.
5. Change request branch to select existing state before writing new pending
   run state.
6. Bound or subindex :obs-buffer and pending-by-task scans.
7. Keep restart/stall limitations documented until A2 adds leases,
   heartbeats, and recovery rules.
```

## Verdict

The committed compute slice gets the central Rama shape right - request depot,
durable claim gate, TaskGlobal executor, observation depot, and Rama-owned views
- but it is not production-safe because bad observation records can crash the
topology and duplicate run requests can reset terminal state into a second
spawnable pending run.

```text
Primary Failure Phase: phase-3
```

```text
RETRO_RAMA_REVIEW:major-fail
```
