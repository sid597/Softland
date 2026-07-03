# Rama Review - LLM Agent Track

Status: reviewed.

## Contract Restatement

The LLM agent track should make executor work Rama-owned async execution. The
child process may do the slow physical work, but Rama owns request identity,
claim grants, observation folding, approval state, raw item trace, token usage,
cost rollups, and run projections.

```text
First depot/input record:
For user-authored LLM work, Space first appends a Space action. Inside the LLM
track, *llm-depot receives a Space-derived :llm/turn-run-request.

Record types:
*llm-depot records are run requests.
*llm-claim-depot records are executor claim requests.
*llm-obs-depot records are execution observations.
*llm-control-depot records are controls derived from Space decisions or
stale-approval policy.

Acceptance/rejection:
LLMTopology decides run request acceptance/rejection, claim grant, observation
folding, and control effects.

Durable accepted truth:
$$llm-decisions-by-run-id, $$llm-turn-runs, $$llm-threads,
$$llm-pending-by-task, $$llm-items-by-turn-run, $$llm-raw-response-items,
$$llm-approvals-pending, $$llm-approvals-by-run-id,
$$llm-token-usage-by-run-id, $$llm-cost-by-thread, $$llm-controls-by-run-id,
$$llm-views, and $$projection-run-detail.

Projection/read surface:
Executors read pending runs and ContextBundles, then append claims and
observations. UI/projections read run detail, pending approvals, item streams,
thread history, and cost/token surfaces from PStates.
```

## Rama-First Reconstruction

```text
New data:
LLM run requests, claims, observations, controls, raw response items, tool
calls, approvals, token usage, native thread/session bindings, stale approval
policy events, and backend adapter facts.

Entity owning local ordering:
:llm-turn-run/id owns observation sequence and run lifecycle.
:llm-thread/id owns conversation continuity.
:executor/task-id owns the pending inbox partition.

First physical depot record:
Inside the LLM module, *llm-depot receives a :llm/turn-run-request.

Request/proposal vs accepted fact vs observation:
- Run request asks LLMTopology to create a run.
- Accepted decision/run row makes the run available for execution.
- Claim asks for executor ownership.
- Observation reports provider/executor output after a durable claim.
- Control reports a Space-derived user/system control decision.

Authoritative decision:
LLMTopology, not executor helpers.

Partition:
All LLM depots route by [:llm-run run-id]. Approval rows and item rows
repartition by approval id or item id only for secondary lookup materialization.

Fast reads:
next pending work by task, run detail by run id, pending approval by approval
id, item by item id, bounded thread history by thread id, token usage by run,
cost by thread.

Retries/duplicates/restarts:
Run request appends can duplicate. Claim appends can duplicate or compete.
Observations can duplicate, arrive out of order, arrive from stale executors, or
arrive after terminal state. Executors can die while approvals are pending.

Side effects:
Executor process spawn is the side effect. It must happen only after durable
claim grant. Observation writes must prove they come from the granted claimant.
```

## Phase Diagnosis

```text
Phase 0 - Implicit spec:
Strong. The docs choose the right Rama ownership model: depots are truth,
PStates are projections, LLM owns execution trace, Space owns meaning.

Phase 1 - Plan:
Major gap. The lifecycle says executor claims before streaming observations,
but the committed plan does not explicitly require observations to carry and
validate the claim token. Duplicate direct LLM run request semantics and
production PState shapes are also under-specified.

Phase 2 - Plan validation:
Should have failed a production Rama gate until claim-token observation auth,
run-request idempotency/conflict behavior, approval existence checks, and
bounded/subindexed read shapes were specified.

Phase 3 - Implementation:
The happy path is strong: request -> pending run, claim -> grant, observation
sequence folding, approvals, stale approval policy, follow-up binding, cost
rollups, and Claude adapter coverage are implemented. The adversarial path
fails: observations are trusted without claim proof, duplicate run requests can
reset terminal state, and approval controls can create missing approvals.

Phase 4 - Implementation validation:
Focused tests pass, but runtime probes hit the missing gates directly.

Phase 5 - Tests:
Tests cover the intended spine but miss negative authorization/idempotency
cases.

Phase 6 - Test validation:
Green IPC proves the happy path, not production retry or stale-executor safety.

Phase 7 - Finish/runtime:
Focused LLM tests pass; adversarial probes fail the contract.
```

Primary Failure Phase: phase-1

## Files Reviewed

### Phase Reconstruction

```text
docs/current-mental-model/build/rama-retro-review/04-llm-agent-track/BRIEF.md
docs/current-mental-model/build/rama-retro-review/04-llm-agent-track/PHASE_RECONSTRUCTION.md
```

### Code

```text
src/app/server/rama/dogfood/llm.clj
src/app/server/rama/dogfood/space.clj
```

### Tests

```text
test/app/server/rama/dogfood_llm_test.clj
test/app/server/rama/dogfood_space_test.clj
```

### Docs

```text
docs/current-mental-model/architecture/dogfood-runtime/README.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-v2.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-canonical.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-derived-contract.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-slice-roadmap.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-claude-research.md
```

### Commit Anchors

```text
d7baea4 2026-05-10 llm: add dogfood LLM run lifecycle
a330c26 2026-05-10 llm: run Codex executor from Rama claims
ccfa4dc 2026-05-10 llm: support follow-up runs on bound threads
5402128 2026-05-11 harden: add llm cost rollups
287699c 2026-05-11 llm: document cost rollup tradeoff
67be839 2026-05-11 llm: add Claude executor and transcript capture
59be788 2026-05-11 Add Claude LLM and transcript capture docs
5479656 2026-05-11 docs: record LLM contract MVP completion
```

## Topology / Depot / PState Inventory

### Depots

```text
*llm-depot
  src/app/server/rama/dogfood/llm.clj:1277

*llm-claim-depot
  src/app/server/rama/dogfood/llm.clj:1278

*llm-obs-depot
  src/app/server/rama/dogfood/llm.clj:1279

*llm-control-depot
  src/app/server/rama/dogfood/llm.clj:1280
```

### Topologies

```text
llm-track-topology
  src/app/server/rama/dogfood/llm.clj:1281

Request source branch:
  src/app/server/rama/dogfood/llm.clj:1305-1337

Claim source branch:
  src/app/server/rama/dogfood/llm.clj:1339-1352

Observation source branch:
  src/app/server/rama/dogfood/llm.clj:1354-1397

Control source branch:
  src/app/server/rama/dogfood/llm.clj:1399-1424
```

### PStates

```text
$$llm-threads
$$llm-thread-by-space
$$llm-thread-graph
$$llm-turn-runs
$$llm-turn-runs-by-thread
$$llm-turn-run-by-turn
$$llm-decisions-by-run-id
$$llm-pending-by-task
$$llm-items-by-turn-run
$$llm-items-by-thread
$$llm-item-by-id
$$llm-raw-response-items
$$llm-tool-calls-by-run-id
$$llm-approvals-pending
$$llm-approvals-by-run-id
$$llm-token-usage-by-run-id
$$llm-cost-by-thread
$$llm-controls-by-run-id
$$llm-control-by-id
$$llm-views
$$projection-run-detail

Declared at src/app/server/rama/dogfood/llm.clj:1282-1302.
```

### Query / Read Helpers

```text
read-thread, read-thread-binding, read-run, read-run-for-turn, read-decision,
read-view, read-run-detail-projection, read-pending, read-items-by-run,
read-items-by-thread, read-item-by-id, read-runs-by-thread,
read-raw-response-items, read-tool-calls-by-run, read-approvals-by-run,
read-controls-by-run, read-control, read-token-usage, read-cost-by-thread,
read-pending-approval.

Defined at src/app/server/rama/dogfood/llm.clj:2003-2097.
```

### Client Append Helpers

```text
append-turn-run-request!
append-claim!
append-observation!
append-control!

Defined at src/app/server/rama/dogfood/llm.clj:1466-1492.
```

### TaskGlobals / Caches / Executors

```text
No Rama TaskGlobal is declared for the LLM executor. The executor boundary is a
helper/runtime layer:

claim-run!
  src/app/server/rama/dogfood/llm.clj:1518-1531

run-one-pending-with-adapter!
  src/app/server/rama/dogfood/llm.clj:1913-1956

run-one-pending-with-claude!
  src/app/server/rama/dogfood/llm.clj:1958-1965

mark-stale-approvals!
  src/app/server/rama/dogfood/llm.clj:1988-2001

Claude adapter/env helpers:
  src/app/server/rama/dogfood/llm.clj:1533-1887
```

## Findings

### F1. Observations Mutate Run Truth Without Claim Authorization

Severity: critical

Evidence:
- docs/current-mental-model/architecture/dogfood-runtime/llm-track-derived-contract.md:63 - executor reads pending, claims, gets durable grant before Codex.
- docs/current-mental-model/architecture/dogfood-runtime/llm-track-derived-contract.md:923 - fresh run lifecycle has LLMExecutor claim before streaming observations.
- src/app/server/rama/dogfood/llm.clj:587 - claim records contain :claim/token.
- src/app/server/rama/dogfood/llm.clj:641 - grantable-claim? requires pending run and matching run/thread/task.
- src/app/server/rama/dogfood/llm.clj:649 - grant-claim stores :claimed-by and :claim/token on the run row.
- src/app/server/rama/dogfood/llm.clj:704 - observation records do not require executor id or claim token.
- src/app/server/rama/dogfood/llm.clj:1093 - fold-observation validates run id, thread id, routing key, type, and sequence, but not claim state, executor id, or claim token.
- src/app/server/rama/dogfood/llm.clj:1354 - observation branch folds any known-run observation.
- test/app/server/rama/dogfood_llm_test.clj:578 - fake executor test proves the happy claim-before-stream path, not the depot-level authorization gate.

Runtime trace:
1. Appended a run request and waited for :pending.
2. Appended :codex/item-completed sequence 0 without any claim.
3. Run became :running with :claimed-by nil, :claim/token nil, :last-seq 0, and item row materialized while the run was still present in pending-by-task.
4. In a separate probe, claimed a run with token "good-token", then appended an observation carrying "wrong-token" and executor "executor-bad".
5. The wrong-token observation still wrote the item and moved the claimed run to :running.

Probe result:
```text
{:observation-without-claim
 {:status :running,
  :claimed-by nil,
  :claim-token nil,
  :last-seq 0,
  :pending-still? true,
  :item {:llm-item/id "item-probe-no-claim", ...}},
 :wrong-token-observation
 {:status :running,
  :claimed-by "executor-good",
  :stored-claim-token "good-token",
  :bad-observation-token "wrong-token",
  :last-seq 0,
  :item-written? true}}
```

Rama concern:
The durable claim is supposed to be the gate that protects side effects and
their observations. In production, any retrying/stale/wrong executor or append
client that can reach *llm-obs-depot can mutate lifecycle truth by knowing the
run id and sequence.

Failure phase:
phase-1

Consequence:
Run truth is not executor-owned by the granted claim. A stale process can write
items, approvals, token usage, failure, or success after losing the race. A
process can write before claiming and leave the run both pending and running.

Repair shape:
Add claim proof to the observation envelope and require LLMTopology to validate
run status, claimed executor id, and claim token before folding effects. Invalid
observations should be rejected/dead-lettered or recorded as observation errors
without mutating run truth.

### F2. Duplicate LLM Run Requests Can Reset A Completed Run

Severity: high

Evidence:
- src/app/server/rama/dogfood/llm.clj:343 - accepted decision id is derived from run id.
- src/app/server/rama/dogfood/llm.clj:385 - initial-turn-run-row always starts status at :pending.
- src/app/server/rama/dogfood/llm.clj:1305 - request branch interprets every *llm-depot request.
- src/app/server/rama/dogfood/llm.clj:1311 - request branch constructs a fresh initial run row.
- src/app/server/rama/dogfood/llm.clj:1329 - request branch writes the fresh run row to $$llm-turn-runs without checking an existing run row.
- src/app/server/rama/dogfood/llm.clj:1335 - request branch writes the run back into pending-by-task.
- src/app/server/rama/dogfood/llm.clj:1276 - no idempotency/conflict PState exists in the LLM module.

Runtime trace:
1. Appended run request A.
2. Claimed run A and appended run-finished sequence 0.
3. Verified run status :succeeded.
4. Appended another *llm-depot request with the same run id but different
   request id, turn id, and context bundle id.
5. The run row was overwritten back to :pending with last-seq -1 and the new
   request/turn/bundle ids; pending-by-task contained the run again.

Probe result:
```text
{:duplicate-run-request
 {:before {:status :succeeded,
           :request/id "req-probe-duplicate-a",
           :turn/id "WT-probe-duplicate-a",
           :context-bundle/id "B-probe-duplicate-a",
           :last-seq 0},
  :after {:status :pending,
          :request/id "req-probe-duplicate-b",
          :turn/id "WT-probe-duplicate-b",
          :context-bundle/id "B-probe-duplicate-b",
          :last-seq -1,
          :pending-after-duplicate? true},
  :decision-request-id "req-probe-duplicate-b"}}
```

Rama concern:
Stream topologies must be retry/duplicate safe. A duplicate or conflicting
request must be exact replay, conflict, or no-op; it cannot reset terminal state
or re-enqueue executor work.

Failure phase:
phase-3

Consequence:
A replay, foreign-append retry, operator mistake, or conflicting derived request
can erase terminal lifecycle state and cause another executor spawn for the same
run id.

Repair shape:
Before accepting a run request, read the existing run/decision for the run id.
Exact replay should preserve the existing row and decision. Conflicting material
should write a rejected/conflict decision and not modify run, thread, pending,
or run-by-turn state.

### F3. Approval Resolution Can Invent A Missing Approval

Severity: high

Evidence:
- docs/current-mental-model/architecture/dogfood-runtime/llm-track-derived-contract.md:1003 - approval lifecycle starts with a Codex approval server request.
- docs/current-mental-model/architecture/dogfood-runtime/llm-track-derived-contract.md:1005 - LLMTopology writes $$llm-approvals-pending before UI resolves it.
- src/app/server/rama/dogfood/llm.clj:816 - approval rows are derived from approval observations.
- src/app/server/rama/dogfood/llm.clj:1183 - resolve-approval looks for an existing approval but falls back to constructing a new approval map.
- src/app/server/rama/dogfood/llm.clj:1205 - the synthesized approval is written into approvals-by-id.
- src/app/server/rama/dogfood/llm.clj:1418 - control branch removes pending approval by id regardless of whether one existed.
- test/app/server/rama/dogfood_llm_test.clj:542 - approval native id test covers real approval observation only.
- test/app/server/rama/dogfood_space_test.clj:1127 - Space-first approval test resolves after appending a real approval observation only.

Runtime trace:
1. Prior block probe created a run.
2. Appended an approval resolve control for approval-never-requested without any
   prior approval observation.
3. LLM state accepted the control, kept the run pending/running path alive, and
   wrote an approved approval row with no pending approval row.

Probe result:
```text
{:decision-status :accepted,
 :llm-control-type :approval/resolve,
 :run-status :pending,
 :approval {:approval/id approval-never-requested,
            :status :approved,
            :native/json-rpc-request-id 999,
            :llm-turn-run/id run-missing-approval,
            :decision :approved},
 :pending-approval nil}
```

Rama concern:
Controls are durable facts. Resolving an approval must be a transition of an
existing pending approval, not creation of a new approved row from control
payload.

Failure phase:
phase-3

Consequence:
The system can record user approval for a tool request that Codex/Claude never
made. The native JSON-RPC id can also come from caller payload rather than the
pending approval row.

Repair shape:
Require pending approval existence, matching run/thread, and pending status
before resolution. Copy native JSON-RPC id from the pending row. Missing
approval controls should be rejected or recorded as invalid controls without
changing approval truth.

### F4. Hot LLM PStates Are Map-Shaped Without Production Bounds

Severity: medium

Evidence:
- src/app/server/rama/dogfood/llm.clj:421 - each run stores :obs-buffer as a plain map.
- src/app/server/rama/dogfood/llm.clj:422 - each run stores :items-by-id as a plain map.
- src/app/server/rama/dogfood/llm.clj:424 - each run stores :raw-response-items as a plain map.
- src/app/server/rama/dogfood/llm.clj:428 - each run stores :approvals-pending as a plain map.
- src/app/server/rama/dogfood/llm.clj:1282 - PStates are declared as {String Object}, with no subindex declarations.
- src/app/server/rama/dogfood/llm.clj:1335 - pending-by-task stores run entries under one task id map.
- src/app/server/rama/dogfood/llm.clj:1384 - items-by-thread writes a nested run id -> item map under the thread key.
- src/app/server/rama/dogfood/llm.clj:1496 - first-pending-entry reads, sorts, and scans the whole pending map.
- src/app/server/rama/dogfood/llm.clj:2035 - read-pending selects the whole pending map for a task.
- src/app/server/rama/dogfood/llm.clj:2047 - read-items-by-thread selects the whole thread item map.

Runtime trace:
Not needed; this is a static production-shape issue visible in the PState and
read helper design.

Rama concern:
The Rama skill requires subindexing or bounded shapes for large collections and
warns against repeated full-map reads. The LLM track is explicitly
append-heavy, and run/thread/task maps can grow beyond small IPC test sizes.

Failure phase:
phase-1

Consequence:
Production read paths can become whole-map RocksDB reads and in-memory sorts.
Observation gaps can accumulate unbounded buffers. A busy executor task can pay
for every pending run just to claim one.

Repair shape:
Define bounded run-detail projections and subindexed collections for pending
inbox, observation buffer, per-run items, per-thread runs/items, approvals, and
tool calls. Provide query helpers for "next pending" and bounded history slices
instead of whole-map selects.

### F5. Tests Prove The Happy Spine But Miss The Rama Failure Modes

Severity: medium

Evidence:
- test/app/server/rama/dogfood_llm_test.clj:363 - lifecycle test claims before appending observations.
- test/app/server/rama/dogfood_llm_test.clj:498 - sequence-buffer test covers out-of-order drain but not auth or stuck gap bounds.
- test/app/server/rama/dogfood_llm_test.clj:542 - approval native id test starts from a real approval observation.
- test/app/server/rama/dogfood_llm_test.clj:578 - fake executor test proves spawn after grant through the helper.
- test/app/server/rama/dogfood_llm_test.clj:616 - stale approval tests cover recorded policy after real approval.
- test/app/server/rama/dogfood_llm_test.clj:701 - follow-up test covers native thread binding and separate runs.
- test/app/server/rama/dogfood_space_test.clj:1080 - idempotency test covers Space send replay but not duplicate *llm-depot run request replay.

Runtime trace:
Focused tests passed:

```text
Testing app.server.rama.dogfood-llm-test
Ran 12 tests containing 114 assertions.
0 failures, 0 errors.
```

The adversarial probes in F1 and F2 then failed the intended Rama safety
contract.

Rama concern:
IPC green tests are not production correctness evidence unless they exercise the
retry, stale executor, duplicate input, and invalid control cases that Rama will
see in production.

Failure phase:
phase-5

Consequence:
The implementation appears green while allowing direct observation mutation,
run reset, and missing approval resolution.

Repair shape:
Add negative tests before refactor: observation before claim, wrong-token
observation, duplicate run request exact replay/conflict, terminal late
observation, missing approval resolve, and pending-inbox bounded read behavior.

## Phase-4 Style Check Matrix

### Redundant Conditionals

Verdict: PASS

Evidence:
Dispatch by request/control/observation type is explicit and readable. No major
duplicated conditional chain is itself causing the observed failures.

### Consecutive Keypath

Verdict: PASS

Evidence:
The reviewed topology generally selects a colocated row, computes a new value,
and writes with termval. The failures are missing guards, not repeated path
navigation.

### Select-Compute-Transform

Verdict: MIXED

Evidence:
The pattern is mostly appropriate, but request handling should select existing
run/decision state before writing a fresh initial row. See
src/app/server/rama/dogfood/llm.clj:1305-1337.

### Unnecessary nil->val

Verdict: PASS

Evidence:
No meaningful nil->value transform smell was observed in the reviewed branches.

### :allow-yield?

Verdict: N/A

Evidence:
The topology does not do a large MAP-VALS local-select loop. The production
issue is unbounded map-shaped rows and client reads, not a missing
allow-yield option on a local scan.

### Non-Subindexed Collections Without Size Limits

Verdict: FAIL

Evidence:
PStates are {String Object}; pending inboxes, thread histories, run item maps,
approval maps, raw response maps, tool call maps, and observation buffers have
no subindex or bound. See src/app/server/rama/dogfood/llm.clj:421-429 and
src/app/server/rama/dogfood/llm.clj:1282-1302.

### Stream Topology Idempotency

Verdict: FAIL

Evidence:
Duplicate run request probe reset a succeeded run to pending. The request branch
writes initial state without existing-run replay/conflict checks.

### Partial Failure In Stream Topologies

Verdict: FAIL

Evidence:
Observation branch ignores unknown runs safely, but wrong-token/stale
observations can still mutate known runs. Duplicate requests can re-enqueue
terminal runs.

### Single Depot Append Per Client Operation

Verdict: PASS

Evidence:
Append helpers write one depot record per call:
src/app/server/rama/dogfood/llm.clj:1466-1492. Executor helper appends one
observation record per adapter event after claim.

### Application-State Caches Survive Restart

Verdict: PASS

Evidence:
No app-side mutable cache is source truth. Cost rollup uses PState projection
with an inline repair note at src/app/server/rama/dogfood/llm.clj:544-549.

### No Reimplementation Of Built-In Operations

Verdict: PASS

Evidence:
No custom reimplementation of Rama built-ins was observed.

## Tests / Commands

```text
git show --stat --oneline --name-only d7baea4 a330c26 ccfa4dc 5402128 287699c 67be839 59be788 5479656 -- ...

Result:
Confirmed the block commit anchors touch llm.clj, dogfood_llm_test.clj, Claude
docs, and implementation-record docs. The passive transcript capture doc was
not reviewed beyond commit-bound Claude overlap.

clojure -M:test -e "(require 'clojure.test 'app.server.rama.dogfood-llm-test) (clojure.test/run-tests 'app.server.rama.dogfood-llm-test) (shutdown-agents) (System/exit 0)"

Result:
Testing app.server.rama.dogfood-llm-test
Ran 12 tests containing 114 assertions.
0 failures, 0 errors.

Runtime probe command:
clojure -M:test -e "(do (require (quote [app.server.rama.dogfood.llm :as l])) ... probes ...)"

Probe result:
{:observation-without-claim
 {:status :running,
  :claimed-by nil,
  :claim-token nil,
  :last-seq 0,
  :pending-still? true,
  :item ...},
 :wrong-token-observation
 {:status :running,
  :claimed-by "executor-good",
  :stored-claim-token "good-token",
  :bad-observation-token "wrong-token",
  :last-seq 0,
  :item-written? true},
 :duplicate-run-request
 {:before {:status :succeeded,
           :request/id "req-probe-duplicate-a",
           :turn/id "WT-probe-duplicate-a",
           :context-bundle/id "B-probe-duplicate-a",
           :last-seq 0},
  :after {:status :pending,
          :request/id "req-probe-duplicate-b",
          :turn/id "WT-probe-duplicate-b",
          :context-bundle/id "B-probe-duplicate-b",
          :last-seq -1,
          :pending-after-duplicate? true},
  :decision-request-id "req-probe-duplicate-b",
  :run-by-turn-a "probe_llm_duplicate_run",
  :run-by-turn-b "probe_llm_duplicate_run"}}

ps -eo pid,ppid,etime,stat,command | rg "clojure|-M:test|probe_llm|dogfood-llm-test|rama"

Result:
No lingering probe/test process found.

Command notes:
Two earlier probe-loading attempts failed before appending Rama records:
1. a large multi-form -e command hit EOF while reading.
2. stdin/load-string script loading hit Rama source-info validation.
Those failed attempts are not used as runtime evidence.
```

## Recommended Repair Queue

```text
1. Add failing negative tests for observation before claim and wrong-token
   observation.
2. Extend observation envelope with executor id and claim token, or an
   equivalent durable claim proof.
3. Gate fold-observation on claimed run, matching claimed-by, matching
   claim/token, matching thread, routing key, type, and sequence.
4. Add duplicate *llm-depot run request tests for exact replay and conflicting
   material after pending/claimed/terminal states.
5. Make request branch preserve exact replay and reject conflicting duplicate
   run ids without resetting run state.
6. Require approval resolve controls to match an existing pending approval row.
7. Add late-observation-after-terminal tests and define the policy.
8. Redesign pending inbox, observation buffer, per-run item maps, per-thread
   history, approval maps, and raw item maps with bounded/subindexed read
   shapes.
```

## Open Questions

```text
- Should invalid observations be rejected into a dead-letter PState, appended to
  observation-errors on the run, or both?
- Should stale-approval helper append controls without a fresh claim proof, or
  should stale approval be a separate system-control authority?
- Should duplicate direct *llm-depot requests be keyed only by run id, or should
  there be a separate idempotency key for exact replay diagnostics?
- Should terminal runs ignore late observations or record them as invalid
  observation errors?
```

## Verdict

The LLM track has a strong happy-path Rama spine, but it fails the production
Rama contract at the executor boundary: observations are not claim-authorized
and duplicate run requests can reset durable lifecycle state.

```text
Primary Failure Phase: phase-1
```

```text
RETRO_RAMA_REVIEW:major-fail
```
