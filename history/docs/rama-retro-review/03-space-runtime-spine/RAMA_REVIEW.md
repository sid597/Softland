# Rama Review - Space Runtime Spine

Status: reviewed.

## Contract Restatement

Space should remain the user-facing truth layer. LLM state is execution trace
and proposal material downstream of accepted Space facts.

```text
First depot/input record:
*space-action-depot receives a Space ActionRequest.

Record type:
The first record is a request/proposal. Accepted Space facts are produced only
after SpaceTopology decides.

Acceptance/rejection:
SpaceTopology validates, accepts/rejects, writes a decision, writes accepted
events, and materializes Space PStates. For compose/send and controls, accepted
Space state is created before Space appends derived records into LLM depots.

Durable accepted truth:
$$space-decisions-by-id, $$space-events-by-id, $$spaces, $$turns,
$$context-bundles, $$objects, $$slices/$$overlays/$$derivatives,
$$space-patch-proposals, and projection PStates.

Downstream LLM execution:
Space derives LLM run/control records. LLMTopology separately accepts/folds
execution lifecycle, raw observations, approvals, token usage, and run views.

Projection/read surface:
Space projections expose chat canvas and object relation/detail views. LLM
projections expose run detail and cost/item surfaces. Projections are derived,
not source truth.
```

## Rama-First Reconstruction

```text
New data:
Space actions, turns, context bundles, LLM run requests, LLM controls, raw LLM
observations, catalog objects, proposals, patches, slices, overlays,
derivatives, and projections.

Entity owning local ordering:
:space/id owns Space/turn ordering. :llm-turn-run/id owns LLM observation
ordering. :object/id owns catalog/relation reads.

First physical depot record:
For user-authored LLM work, *space-action-depot.

Request/proposal vs accepted fact vs observation:
- Space ActionRequest asks for a Space fact.
- Space accepted event/decision makes the fact true.
- LLM run/control request is derived execution/control intent.
- LLM observation is execution trace.
- Patch proposal is pending Space material, not accepted patch.

Authoritative decision:
SpaceTopology for user meaning. LLMTopology for execution lifecycle.

Partition:
Space depot routes by :routing/key / space id. LLM depots route by run id.

Fast reads:
chat canvas by space id, context bundle by id/turn, run by id, object detail by
object id, object relations by object id, patch proposal by id.

Retries/duplicates/restarts:
Compose sends can replay. LLM observations can duplicate/out-of-order. Controls
can repeat or target stale runs. Projection rebuild must be deterministic.

Side effects:
Space appends derived LLM run/control depot records. LLM executor side effects
are outside this block except where the Space-first contract depends on them.
```

## Phase Diagnosis

```text
Phase 0 - Implicit spec:
Strong. The docs correctly separate Space meaning from LLM execution trace and
make ContextBundle the immutable bridge.

Phase 1 - Plan:
Mostly sound for MVP, but under-specified around control validation against LLM
state, observation-to-proposal filtering, idempotency conflict semantics, and
large PState read shapes.

Phase 2 - Plan validation:
Should have passed as a green MVP but not as production-complete. The missing
cross-module validation/filtering questions should have been explicit gates.

Phase 3 - Implementation:
The main Space-first compose/send bridge is implemented and tested. The
implementation drifts at helper/control edges: any LLM observation can be
converted to a pending patch proposal, and approval controls can invent an
approved approval row without a prior approval observation.

Phase 4 - Implementation validation:
Focused suite passes and proves a lot of the contract, but targeted probes find
truth-boundary leaks.

Phase 5 - Tests:
Tests are broad, but the missing negative cases line up with the observed leaks.

Phase 6 - Test validation:
The green suite proves the intended spine, not all adversarial inputs.

Phase 7 - Finish/runtime:
Focused Space+LLM tests pass; runtime probes expose failures.
```

Primary Failure Phase: phase-3

## Files Reviewed

### Phase Reconstruction

```text
docs/current-mental-model/build/rama-retro-review/03-space-runtime-spine/BRIEF.md
docs/current-mental-model/build/rama-retro-review/03-space-runtime-spine/PHASE_RECONSTRUCTION.md
```

### Code

```text
src/app/server/rama/dogfood/space.clj
src/app/server/rama/dogfood/llm.clj
```

### Tests

```text
test/app/server/rama/dogfood_space_test.clj
test/app/server/rama/dogfood_llm_test.clj
```

### Docs

```text
docs/current-mental-model/architecture/dogfood-runtime/README.md
docs/current-mental-model/architecture/dogfood-runtime/three-depot-current-system.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-derived-contract.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-slice-roadmap.md
```

### Commit Anchors

```text
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

## Topology / Depot / PState Inventory

### Depots

```text
*space-action-depot
  src/app/server/rama/dogfood/space.clj:1170

Mirrored *llm-depot
  src/app/server/rama/dogfood/space.clj:1168

Mirrored *llm-control-depot
  src/app/server/rama/dogfood/space.clj:1169

LLM depots:
*llm-depot, *llm-claim-depot, *llm-obs-depot, *llm-control-depot
  src/app/server/rama/dogfood/llm.clj:1276-1280
```

### Topologies

```text
space-topology
  src/app/server/rama/dogfood/space.clj:1171
  sources start at src/app/server/rama/dogfood/space.clj:1197.

llm-track-topology
  src/app/server/rama/dogfood/llm.clj:1281
  sources start at src/app/server/rama/dogfood/llm.clj:1304.
```

### PStates

```text
Space:
$$space-requests-by-id, $$space-decisions-by-id, $$space-events-by-id,
$$spaces, $$space-graph, $$turns, $$turns-by-space, $$context-bundles,
$$context-bundles-by-turn, $$send-by-idempotency, $$llm-run-requests,
$$llm-run-by-turn, $$llm-controls, $$llm-control-by-turn, $$objects,
$$artifact-graph, $$artifact-graph-in, $$slices, $$overlays, $$derivatives,
$$space-patch-proposals, $$projection-chat-canvas,
$$projection-object-detail, $$projection-object-relations.

Declared at src/app/server/rama/dogfood/space.clj:1172-1195.

LLM:
$$llm-threads, $$llm-thread-by-space, $$llm-thread-graph,
$$llm-turn-runs, $$llm-turn-runs-by-thread, $$llm-turn-run-by-turn,
$$llm-decisions-by-run-id, $$llm-pending-by-task, $$llm-items-by-turn-run,
$$llm-items-by-thread, $$llm-item-by-id, $$llm-raw-response-items,
$$llm-tool-calls-by-run-id, $$llm-approvals-pending,
$$llm-approvals-by-run-id, $$llm-token-usage-by-run-id,
$$llm-cost-by-thread, $$llm-controls-by-run-id, $$llm-control-by-id,
$$llm-views, $$projection-run-detail.

Declared at src/app/server/rama/dogfood/llm.clj:1282-1302.
```

### Query / Read Helpers

```text
Space read helpers:
src/app/server/rama/dogfood/space.clj:1739-1843

Space await helpers:
src/app/server/rama/dogfood/space.clj:1845-1868

LLM runtime/read helpers:
src/app/server/rama/dogfood/llm.clj:1426-1492 and later read helpers in the
same namespace.
```

### Client Append Helpers

```text
append-space-action!
  src/app/server/rama/dogfood/space.clj:1705-1710

append-llm-observation!
  src/app/server/rama/dogfood/space.clj:1712-1737

LLM append helpers:
  src/app/server/rama/dogfood/llm.clj:1466-1492
```

### TaskGlobals / Caches / Executors

```text
No Space TaskGlobal.
LLM executor helpers live outside LLM topology in runtime/helper functions. This
is in scope only where Space-first contract depends on run/control/observation
records.
```

## Findings

### F1. Non-patch LLM observations become pending Space patch proposals

Severity: high

Evidence:
- `docs/current-mental-model/architecture/dogfood-runtime/llm-track-derived-contract.md:449-473` describes ordinary LLM observations as execution trace folded by LLMTopology.
- `docs/current-mental-model/architecture/dogfood-runtime/llm-track-derived-contract.md:362-383` distinguishes patch proposal requests from ordinary observations/catalog progress.
- `src/app/server/rama/dogfood/space.clj:1026-1028` defines `patch-proposal-observation?`, but the helper below does not use it.
- `src/app/server/rama/dogfood/space.clj:1712-1737` appends every supplied LLM observation to the LLM observation depot and then unconditionally appends a `:turn/patch-proposal-create` Space action.
- `test/app/server/rama/dogfood_space_test.clj:814-849` proves patch observations create proposals, but there is no inverse test for non-patch observations.

Runtime trace:
1. Create a Space compose/send and wait for the derived LLM run to become pending.
2. Call `space/append-llm-observation!` with a normal `:codex/item-completed`
   observation.
3. The LLM item is indexed, and Space also materializes a pending patch proposal.
4. Probe result:
   `{:item? true, :proposal {:observation/id obs-nonpatch-item, :status :pending, :patch-proposal/id obs-nonpatch-item, :patch/files nil, ...}}`

Rama concern:
The bridge from execution trace back into Space meaning must be typed. Ordinary
raw LLM observations are not pending patch proposals. A helper that appends two
depots for every observation creates extra Space meaning not justified by the
observation type.

Failure phase:
phase-3

Consequence:
Any caller using the general-sounding helper for non-patch observations can
pollute Space with bogus pending patch proposals. This violates the boundary
that LLM owns execution trace while Space owns accepted/proposed meaning.

Repair shape:
Split the helper or guard it with `patch-proposal-observation?`. Ordinary
observations should append only to `*llm-obs-depot`; only patch-like observations
should derive a Space proposal action.

### F2. Approval controls can approve an approval that was never requested

Severity: high

Evidence:
- `docs/current-mental-model/architecture/dogfood-runtime/llm-track-derived-contract.md:498-524` says pending approvals must retain the native JSON-RPC id and World/Space copies it from the pending approval row into the control record.
- `src/app/server/rama/dogfood/space.clj:494-509` builds the LLM control record from request payload fields, including `:native/json-rpc-request-id`; it does not read or verify LLM pending approval state.
- `src/app/server/rama/dogfood/space.clj:628-649` validates only nonblank run id and approval id for approval controls.
- `src/app/server/rama/dogfood/llm.clj:1183-1201` creates a default approval row when none exists.
- `test/app/server/rama/dogfood_space_test.clj:1127-1173` tests approval resolution only after an approval observation exists.

Runtime trace:
1. Create a Space compose/send and wait for the LLM run to become pending.
2. Without any approval-request observation, append `:turn/tool-approval-resolve`
   for `approval-never-requested`.
3. Space accepts the control and LLM records an approved approval row.
4. Probe result:
   `{:decision-status :accepted, :llm-control-type :approval/resolve, :run-status :pending, :approval {:approval/id approval-never-requested, :status :approved, :native/json-rpc-request-id 999, ...}, :pending-approval nil}`

Rama concern:
Controls are not just user-authored turns; they authorize live executor
behavior. Approval resolution must be tied to durable pending approval state,
not invented from UI/control payload. Otherwise the native request id mapping is
not Rama-owned truth.

Failure phase:
phase-3

Consequence:
Space can record and forward an approval that no executor ever requested. That
corrupts approval audit state and can train callers to trust UI-provided native
ids instead of durable LLM observation state.

Repair shape:
Reject or no-op approval controls unless a matching pending approval exists for
the target run. Copy `:native/json-rpc-request-id` from the pending approval row
or require a topology-visible proof that the payload matches it.

### F3. Compose idempotency is conflict-blind

Severity: medium

Evidence:
- `docs/current-mental-model/architecture/dogfood-runtime/llm-track-slice-roadmap.md:155-163` says idempotency dedup returns the same decision/result and does not mint duplicate turns, bundles, or runs.
- `src/app/server/rama/dogfood/space.clj:577-593` stores a dedupe row with original material.
- `src/app/server/rama/dogfood/space.clj:597-617` builds an idempotent decision from that row.
- `src/app/server/rama/dogfood/space.clj:1231-1245` checks only whether the idempotency key exists, not whether the replay matches the same target/payload.
- `test/app/server/rama/dogfood_space_test.clj:1080-1125` tests replay within the same Space, but not conflicting reuse.

Runtime trace:
1. Append a compose/send for `chat-cross-idem-a3` with idempotency key
   `idem-cross3`.
2. Append a second compose/send for `chat-cross-idem-b3` with the same
   idempotency key but different Space, turn, bundle, run, and prompt.
3. Result:
   `{:second-space chat-cross-idem-a3, :second-replayed true, :second-status :accepted, :second-turn WT-cross-a3, :space-b? false, :run-b? false, :idem-row {:space/id chat-cross-idem-a3, :turn/id WT-cross-a3, :llm-turn-run/id run-cross-a3}}`

Rama concern:
Idempotency must distinguish exact replay from conflicting duplicate keys. A
conflict-blind dedupe table can make a second request appear accepted while
silently aliasing it to unrelated prior material.

Failure phase:
phase-1

Consequence:
The second request receives an accepted replayed decision for the first Space.
This avoids duplicate side effects, but it can mislead clients and audit trails
unless global idempotency-key uniqueness and conflict behavior are explicit.

Repair shape:
Store a request material hash in the dedupe row and reject conflicting reuse
with a clear decision reason. Alternatively document idempotency keys as
globally unique operation ids and still emit a conflict when target/material
differs.

### F4. Large Space/LLM PStates are not shaped for production read costs

Severity: medium

Evidence:
- `docs/current-mental-model/architecture/dogfood-runtime/llm-track-derived-contract.md:537-604` lists many growing PStates: turns by thread, artifact graphs, LLM item histories, approvals, token usage, and projections.
- `src/app/server/rama/dogfood/space.clj:1172-1195` declares these Space PStates mostly as `{String Object}`.
- `src/app/server/rama/dogfood/llm.clj:1282-1302` declares LLM histories and indexes mostly as `{String Object}`.
- `src/app/server/rama/dogfood/llm.clj:420-435` stores run-local nested maps/vectors including `:obs-buffer`, `:items-by-id`, `:item-order`, tool calls, approvals, controls, token usage, and errors.
- Tests exercise tiny materialized examples and deterministic rebuilds, not large scans/range reads.

Runtime trace:
1. Space and LLM tests prove small examples materialize and rebuild.
2. No reviewed test drives large turn histories, object graphs, or long LLM raw
   item streams through query/read paths.

Rama concern:
PState collections expected to exceed roughly small toy sizes need explicit
subindexing, bounds, paging, or query topology design. Rebuildability and
denormalization help correctness but do not by themselves make reads efficient.

Failure phase:
phase-1

Consequence:
The MVP is coherent, but using it as a production Space/LLM substrate would
create avoidable RocksDB reads and large value rewrites.

Repair shape:
For each hot UI read, define bounded/subindexed PState paths and query topology
surfaces. Start with turns-by-space, artifact graph edges, LLM item streams,
pending approvals, and object relation projections.

### F5. Missing negative tests let the truth-boundary leaks pass

Severity: medium

Evidence:
- Focused Space+LLM tests pass with 38 tests and 322 assertions.
- `test/app/server/rama/dogfood_space_test.clj:814-849` covers patch proposal creation from a patch observation.
- `test/app/server/rama/dogfood_space_test.clj:1127-1173` covers approval resolution after pending approval exists.
- `test/app/server/rama/dogfood_space_test.clj:1080-1125` covers idempotent replay for the same send key.
- Missing tests: non-patch observation through Space helper, approval resolve
  without pending approval, control against missing run, and conflicting reuse
  of idempotency key.

Runtime trace:
1. Green suite passes.
2. Non-patch observation probe creates bogus pending proposal.
3. Missing approval probe creates approved approval row.
4. Conflicting idempotency probe aliases request B to request A.

Rama concern:
The tests prove the intended path, but not the bad inputs that define the
actual contract boundary between Space meaning and LLM execution trace.

Failure phase:
phase-5

Consequence:
The implementation can regress at exactly the edges the docs care about while
still preserving all current green tests.

Repair shape:
Add the missing negative tests first, then repair the helper/control/dedupe
behavior to those expectations.

## Phase-4 Style Check Matrix

### Redundant Conditionals

Verdict: MIXED.

Evidence:
`interpret-control-turn` calls `validate-or-reject` twice in the first branch
(`src/app/server/rama/dogfood/space.clj:628-633`). It is not the main
correctness issue but is avoidable repeated work.

### Consecutive Keypath

Verdict: MIXED.

Evidence:
Nested keypaths for artifact graph and pending maps are semantically meaningful,
but most are not declared subindexed. See `src/app/server/rama/dogfood/space.clj:1332-1351`
and `src/app/server/rama/dogfood/llm.clj:1334-1335`.

### Select-Compute-Transform

Verdict: MIXED.

Evidence:
The code frequently selects existing rows and writes `termval`, which is good.
However, idempotency conflict detection does not compare request material before
returning an existing dedupe row (`src/app/server/rama/dogfood/space.clj:1231-1245`).

### Unnecessary nil->val

Verdict: N/A.

Evidence:
No central nil-to-value cleanup issue identified.

### :allow-yield?

Verdict: MIXED.

Evidence:
Reviewed topology reads are mostly point reads, but growing map/vector values
are stored under top-level keys. Larger query surfaces need query topology or
subindexed read design before production.

### Non-Subindexed Collections Without Size Limits

Verdict: FAIL.

Evidence:
Space and LLM declare many PStates as `{String Object}` and store nested maps or
vectors without reviewed bounds/subindexing (`space.clj:1172-1195`,
`llm.clj:1282-1302`, `llm.clj:420-435`).

### Stream Topology Idempotency

Verdict: MIXED.

Evidence:
Compose-send exact replay is handled and tested. Conflicting reuse of an
idempotency key is not rejected and aliases to the first request.

### Partial Failure In Stream Topologies

Verdict: MIXED.

Evidence:
LLM observation branch guards unknown run with `known-run-row?`
(`src/app/server/rama/dogfood/llm.clj:1354-1397`), which is better than the
compute slice. Control and proposal helper edges still allow invalid meaning to
be materialized.

### Single Depot Append Per Client Operation

Verdict: FAIL for `append-llm-observation!`.

Evidence:
`append-llm-observation!` appends to LLM observation depot and Space action
depot in one helper call (`src/app/server/rama/dogfood/space.clj:1712-1737`).
For patch observations this may be intentional bridge behavior; for all
observations it is too broad.

### Application-State Caches Survive Restart

Verdict: PASS for reviewed Space scope.

Evidence:
Space reviewed state is PState-backed. No Space process-local cache is used as
truth in the reviewed files.

### No Reimplementation Of Built-In Operations

Verdict: PASS.

Evidence:
No custom replacement for a Rama built-in operation was identified.

## Tests / Commands

Focused Space+LLM test run:

```text
clojure -M:test -e "(require 'clojure.test 'app.server.rama.dogfood-space-test 'app.server.rama.dogfood-llm-test) (clojure.test/run-tests 'app.server.rama.dogfood-space-test 'app.server.rama.dogfood-llm-test) (shutdown-agents) (System/exit 0)"

Testing app.server.rama.dogfood-space-test
Testing app.server.rama.dogfood-llm-test

Ran 38 tests containing 322 assertions.
0 failures, 0 errors.
{:test 38, :pass 322, :fail 0, :error 0, :type :summary}
```

Non-patch observation bridge probe:

```text
clojure -M:test -e "(require '[app.server.rama.dogfood.space :as s] '[app.server.rama.dogfood.llm :as l]) ... append :codex/item-completed through s/append-llm-observation! ..."

Result:
{:item? true,
 :proposal {:raw/json {},
            :summary/text nil,
            :observation/id obs-nonpatch-item,
            :llm-thread/id llm-thread-nonpatch,
            :space/id chat-nonpatch-obs,
            :status :pending,
            :turn/id WT-nonpatch,
            :llm-turn-run/id run-nonpatch,
            :patch-proposal/id obs-nonpatch-item,
            :patch/files nil,
            :turn-diff/id obs-nonpatch-item}}
```

Missing approval control probe:

```text
clojure -M:test -e "(require '[app.server.rama.dogfood.space :as s] '[app.server.rama.dogfood.llm :as l]) ... resolve approval-never-requested ..."

Result:
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

Conflicting idempotency-key probe:

```text
clojure -M:test -e "(require '[app.server.rama.dogfood.space :as s] '[app.server.rama.dogfood.llm :as l]) ... same idempotency key across different Spaces ..."

Result:
{:first-status :accepted,
 :first-space chat-cross-idem-a3,
 :second-status :accepted,
 :second-space chat-cross-idem-a3,
 :second-turn WT-cross-a3,
 :second-replayed true,
 :space-b? false,
 :run-b? false,
 :idem-row {:space/id chat-cross-idem-a3,
            :turn/id WT-cross-a3,
            :llm-turn-run/id run-cross-a3}}
```

Process check:

```text
ps -eo pid,ppid,etime,stat,command | rg "clojure|-M:test|dogfood-space-test|dogfood-llm-test|chat-nonpatch|idem-cross|rama"

No lingering test process was found before the probes were rerun.
```

## Recommended Repair Queue

```text
1. Add negative tests for non-patch observation bridge, missing approval
   resolution, missing run control, and conflicting idempotency reuse.
2. Split `append-llm-observation!` or guard the Space proposal append with
   `patch-proposal-observation?`.
3. Require approval controls to match a pending approval row and copy native
   JSON-RPC id from that row.
4. Define idempotency conflict semantics and compare material hash/target before
   returning replayed decisions.
5. Reject patch accept/reject when the proposal id is missing.
6. Design subindexed/bounded read shapes for growing Space/LLM collections.
```

## Verdict

The committed Space spine is a strong green MVP and the central compose/send
path is genuinely Space-first, but the implementation leaks at the exact
boundary edges Rama should protect: ordinary observations can become Space
patch proposals, nonexistent approvals can be approved, and idempotency replay
is conflict-blind.

```text
Primary Failure Phase: phase-3
```

```text
RETRO_RAMA_REVIEW:major-fail
```
