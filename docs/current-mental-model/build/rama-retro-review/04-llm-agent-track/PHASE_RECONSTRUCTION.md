# Phase Reconstruction - LLM Agent Track

Status: reconstructed.

This artifact reconstructs the Rama phase pack that should have existed for the
committed LLM agent track. It is not a new design proposal.

## Source Evidence

```text
docs/current-mental-model/build/rama-retro-review/04-llm-agent-track/BRIEF.md
docs/current-mental-model/architecture/dogfood-runtime/README.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-v2.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-canonical.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-derived-contract.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-slice-roadmap.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-claude-research.md
src/app/server/rama/dogfood/llm.clj
src/app/server/rama/dogfood/space.clj
test/app/server/rama/dogfood_llm_test.clj
test/app/server/rama/dogfood_space_test.clj

Commit anchors:
d7baea4 2026-05-10 llm: add dogfood LLM run lifecycle
a330c26 2026-05-10 llm: run Codex executor from Rama claims
ccfa4dc 2026-05-10 llm: support follow-up runs on bound threads
5402128 2026-05-11 harden: add llm cost rollups
287699c 2026-05-11 llm: document cost rollup tradeoff
67be839 2026-05-11 llm: add Claude executor and transcript capture
59be788 2026-05-11 Add Claude LLM and transcript capture docs
5479656 2026-05-11 docs: record LLM contract MVP completion
```

## Phase 0 - Implicit Spec

The committed docs imply the LLM track is a Rama-owned async execution spine:
Space decides user meaning, LLMTopology owns execution lifecycle, and executors
only read PStates plus append depot records.

```text
User/world problem:
Softland needs LLM work to be durable, inspectable, replayable, and integrated
with the Space/world substrate instead of being an opaque child process stream.

First physical record for user-authored LLM work:
*space-action-depot receives a Space action. SpaceTopology accepts it, freezes a
ContextBundle, then foreign-appends the derived LLM run request to *llm-depot.

First physical record inside the LLM track:
*llm-depot receives a :llm/turn-run-request derived by SpaceTopology.

New data entering the LLM track:
- LLM turn-run requests
- executor claims
- streamed Codex observations
- streamed Claude observations
- approval requests and approval resolutions
- tool calls
- patch proposals
- token usage, cache, quota, and cost observations
- cancel, compact, steer, and stale-approval controls
- native Codex thread ids and Claude session ids

Entities and identities:
- :llm-turn-run/id owns one execution attempt and observation sequence.
- :llm-thread/id owns native conversation continuity across runs.
- :space/id binds the LLM thread back to the user-facing Space.
- :turn/id binds the run to the Space turn that caused it.
- :context-bundle/id identifies the immutable model input.
- :executor/task-id identifies the pending inbox partition.
- :claim/token should be the durable proof that one executor is allowed to run
  and stream observations for a run.
- :approval/id identifies one native tool/permission request.
- :llm-item/id identifies a raw response item.
- native Codex thread id and Claude session id remain backend adapter facts.

Invariants:
- LLMTopology is the only writer of LLM PStates.
- Executors never write PStates.
- A run request creates :pending run state and a pending executor inbox entry.
- A claim is required before executor side effects start.
- Only the granted claimant may write observations for that run.
- Wrong-token or stale executors must not mutate run truth.
- Observations are folded in sequence; out-of-order observations are buffered.
- Duplicate observations below last-seq are ignored.
- Duplicate or replayed run requests must not reset completed lifecycle state.
- Approval requests are durable pending approval rows.
- Approval resolutions must correspond to real pending approval rows and must
  preserve the native JSON-RPC request id.
- Tool approval does not equal Space patch acceptance.
- Raw LLM items are immutable; later projections/slices should not rewrite them.
- Cost and token usage are observations, not UI estimates.
- Cost rollups must avoid double counting when usage for a run is updated.
- Follow-up turns create new LLMTurnRuns in the same LLMThread.
- Codex and Claude differences are adapter details under the same LLM ontology.
- Claude subscription mode must not put OAuth tokens into Softland-owned env or
  Rama state.

Scale expectations:
- Observation streams are append-heavy and can be high volume.
- A thread can accumulate many runs and many raw items.
- A task inbox can accumulate many pending runs.
- A run can accumulate many observations, tool calls, approvals, and buffered
  out-of-order observations.
- Read surfaces should avoid loading unbounded maps when a user only needs a
  bounded run detail, pending work item, or thread slice.

Failure cases:
- duplicate run request
- duplicate claim
- stale claim
- wrong-token observation
- observation before claim
- observation after terminal state
- observation gap that never fills
- unknown run observation
- approval resolution for missing approval
- executor death with unresolved approvals
- native thread binding not durable before follow-up/fork execution
- duplicate token usage update
- provider output containing secrets or auth material

Must be durable:
- request decisions
- run lifecycle
- claim grant identity/token
- raw observations and raw response item payloads after redaction
- approvals and resolution controls
- token usage/cost facts
- native thread/session bindings
- stale approval failures
- run detail projections

Explicitly out of scope:
- passive transcript capture except Claude executor overlap
- in-process chat ingester work
- full UI workflows
- object-container Slice 1
```

## Phase 1 - Plan

A Rama-shaped plan would use a stream topology for low-latency executor
coordination and observation folding. It must protect each side effect with a
durable claim gate and make every write retry-safe.

```text
Depots:
*llm-depot
  Input: :llm/turn-run-request
  Writer: SpaceTopology only
  Partition: [:llm-run run-id]

*llm-claim-depot
  Input: executor claim request
  Writer: LLMExecutor
  Partition: [:llm-run run-id]

*llm-obs-depot
  Input: executor observation
  Writer: granted LLMExecutor
  Partition: [:llm-run run-id]

*llm-control-depot
  Input: Space-derived control
  Writer: SpaceTopology, stale-approval helper for executor-death policy
  Partition: [:llm-run run-id]

Topology choice:
Stream topology. Runs, claims, controls, and observations need interactive
latency and ordered per-run folding.

Core PStates:
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

Request branch:
- Validate envelope, backend, routing key, context bundle reference, and
  bundle-owned execution options.
- Reject invalid requests durably.
- If accepted and no existing run exists, write initial run, decision, thread,
  run-by-thread, run-by-turn, pending-by-task, and views.
- If the same run request replays exactly, keep the existing row stable.
- If the same run id appears with different material, reject or write an
  explicit conflict decision. Never reset terminal state to pending.

Claim branch:
- A claim is grantable only when the run is pending, the run/thread/task match,
  and no prior claim has won.
- The claim branch writes :claimed, claimed-by, claim token, claim time, removes
  the run from pending-by-task, and updates views.
- Duplicate same-claim replay returns the same claim state.
- Competing claims become conflict-or-past without changing run state.

Observation branch:
- Observation records must include enough claimant identity to prove they came
  from the granted executor. Minimum gate: run id, thread id, routing key,
  executor id, claim token, sequence.
- Reject or record an error for observations before claim, wrong token, wrong
  executor, wrong thread, wrong routing key, invalid type, or invalid sequence.
- Unknown-run observations should not crash the topology and should be auditable
  if the contract needs a dead-letter trace.
- Observations are folded by sequence. Gaps buffer deterministically and drain
  when the missing sequence arrives.
- Observation buffers need a bound, timeout, or subindexed storage shape.
- Terminal status should prevent later non-terminal observations from reopening
  or mutating terminal lifecycle unless an explicit repair/retry event exists.

Control branch:
- Controls are Space-derived, except the stale-approval helper appends the same
  control shape as recorded policy.
- Approval resolution must find an existing pending approval for the same run.
- Native JSON-RPC request id should come from the pending approval, not from
  untrusted caller payload.
- Missing approval/run controls should be rejected or recorded as invalid
  controls, not materialized as successful approvals.
- Cancel removes pending work and closes the run.
- Compact/steer are durable controls but do not themselves accept Space meaning.

Backend adapter plan:
- Codex and Claude share the LLM run ontology.
- Backend-specific event parsing converts provider events into normalized
  observations.
- Claude subscription mode strips OAuth/API env vars from child env.
- Claude API-key mode injects only a resolved per-call API key.
- Passive-observe is not valid for active LLM run requests.

Read/query surfaces:
- Read run by run id.
- Read pending work by executor task id without scanning an unbounded task map.
- Read bounded run detail by run id.
- Read item by item id.
- Read bounded thread history by llm-thread id.
- Read pending approval by approval id.
- Read cost by thread and token usage by run.

I/O/subindexing:
- Thread items, thread runs, pending-by-task, run item maps, approval maps,
  tool-call maps, observation buffers, and raw item maps need subindexed or
  bounded shapes before production scale.
- Client helpers should avoid sorting/loading whole pending inbox maps to pick
  one run.
```

## Phase 2 - Plan Validation

The committed docs contain the right high-level lifecycle, but an adversarial
Rama review should have stopped the plan on the observation authorization gate.

```text
Plan strengths:
- Correct Space-first origin for user-authored LLM work.
- Correct run vs thread split.
- Correct claim-before-spawn idea.
- Correct depot family: request, claim, observation, control.
- Correct sequence-buffering requirement.
- Correct raw item immutability and follow-up thread binding.
- Correct cost/cache distinction.
- Correct Claude auth boundary at the adapter layer.

Plan risks:
- The docs say claim -> executor -> observations, but do not explicitly require
  observations to carry and validate the granted claim token.
- The docs do not specify what happens if *llm-depot receives the same run id
  again after the run is claimed or terminal.
- The docs do not define terminal-state mutation policy for late observations.
- The docs do not specify bounds or storage shape for observation buffers.
- Pending inbox, thread item history, and per-run maps are described logically
  but not production-shaped.
- Approval resolution semantics do not say "must match pending approval row" as
  an implementation gate.

Verdict the plan should have received:
FAIL for production Rama. Green-MVP approval only if claim-token observation
auth, duplicate run request semantics, approval existence checks, and bounded
read/storage shapes were explicitly deferred and covered by negative tests.
```

## Phase 3 - Expected Implementation Shape

```text
Expected namespaces/files:
src/app/server/rama/dogfood/llm.clj
src/app/server/rama/dogfood/space.clj
test/app/server/rama/dogfood_llm_test.clj
test/app/server/rama/dogfood_space_test.clj

Expected depots:
*llm-depot
*llm-claim-depot
*llm-obs-depot
*llm-control-depot

Expected topology branches:
1. turn-run request branch
2. claim branch
3. observation branch with retry-mode all-after
4. control branch

Expected helper APIs:
- turn-run-request
- claim-record
- observation
- control-record
- append-turn-run-request!
- append-claim!
- append-observation!
- append-control!
- read-run, read-view, read-pending, read-pending-approval
- run-one-pending-with-adapter!
- run-one-pending-with-claude!
- mark-stale-approvals!

Expected executor shape:
- Executor polls/reads pending work.
- Executor claims the run.
- Executor waits for durable claim grant.
- Executor loads the ContextBundle only after grant.
- Executor starts backend process only after grant.
- Executor appends observations that prove the claim grant.
- Executor handles stale approvals on death according to run policy.

Expected adapter shape:
- Codex fake adapter for tests and real app-server adapter shape.
- Claude stream-json adapter maps events into normalized observations.
- Secret redaction before provider payload persistence.
- No active run request for passive-observe mode.

Expected tests:
- request envelope and execution-options tests
- writer-asymmetry property test
- lifecycle materialization test
- claim conflict/stale claim tests
- observation before claim rejected
- wrong-token observation rejected
- out-of-order observation buffer/drain
- duplicate run request does not reset terminal state
- approval pending native id and resolution tests
- missing approval resolve rejected
- stale approval on executor death
- follow-up new run same thread
- cost rollup update and no double count
- Claude env and stream-json adapter tests
```

## Phase 4 - Expected Implementation Validation Focus

```text
1. Observation records cannot mutate a run unless they match the granted claim.
2. Duplicate run requests cannot reset or overwrite an existing run lifecycle.
3. Unknown-run observations cannot crash the stream topology.
4. Missing approval resolutions cannot invent approvals.
5. Terminal runs cannot be reopened by late observations or duplicate requests.
6. Out-of-order observations buffer and drain deterministically.
7. Pending inbox reads do not load/sort an unbounded map.
8. Thread/run/item history reads are bounded or subindexed.
9. Cost rollup handles updated usage without double counting.
10. Backend adapters preserve one LLM ontology across Codex and Claude.
11. Claude child env never carries OAuth or unintended API credentials in
    subscription mode.
```

## Phase 5 - Expected Tests

```text
Positive tests:
- LLM run request materializes pending run and pending inbox.
- Claim grants exactly one executor and removes pending entry.
- Claimed executor streams item, token usage, tool call, approval, and terminal
  observations.
- Out-of-order observations buffer and drain.
- Approval request creates pending approval with native JSON-RPC id.
- Stale approvals fail the run according to recorded policy.
- Follow-up run reuses native thread but creates a new LLMTurnRun.
- Cost rollup sums per-thread token usage and updates per-run usage by delta.
- Claude stream-json maps to normalized LLM observations and redacts secrets.

Negative tests:
- observation before claim does not mutate run truth.
- wrong executor id or wrong claim token does not mutate run truth.
- observation after terminal run is ignored or recorded as invalid.
- duplicate *llm-depot request with same run id is exact replay only.
- conflicting duplicate run request is rejected and cannot reset lifecycle.
- approval resolve for missing approval is rejected or recorded invalid.
- control for missing run is rejected or dead-lettered.
- pending inbox with many entries can be read one item at a time.
- stuck observation gap has an explicit bound or timeout policy.
```

## Phase 6 - Expected Test Validation

```text
Tests can pass while the implementation is wrong if they:
- always append observations after a claim but never assert observations require
  the claim token.
- use helpers that naturally claim before spawning but leave *llm-obs-depot
  open to any append client.
- test duplicate Space sends but not duplicate *llm-depot run requests.
- test approval resolution after a real approval but not missing approvals.
- test out-of-order drain for a small gap but not unbounded stuck buffers.
- use tiny maps, hiding pending inbox and thread-history read shape.
- use only one executor task id.
- treat green IPC as restart/fault-tolerance proof.
```

## Phase 7 - Expected Finish Evidence

```text
Focused LLM tests pass.
Focused Space+LLM tests pass for Space-first bridge overlap.
Runtime probe proves observation before claim is rejected or recorded invalid.
Runtime probe proves wrong-token observation is rejected or recorded invalid.
Runtime probe proves duplicate run request cannot reset terminal state.
Runtime probe proves missing approval resolution cannot invent approval state.
Runtime probe proves unknown-run observation does not crash topology.
No lingering Rama IPC or Clojure test processes.
```

## Reconstruction Gaps

```text
Committed docs do not fully define:
- exact observation claim-token envelope
- whether observation auth is run id only or claim-token protected
- duplicate *llm-depot request semantics after terminal state
- late observation policy after terminal state
- dead-letter/audit shape for unknown observations and invalid controls
- observation buffer bounds/expiry
- production shape for pending-by-task and thread history reads
- whether stale-approval helper is allowed to append control without a fresh
  claim proof
```

## Reconstruction Verdict

The committed docs are strong enough to judge the LLM lifecycle, but the
retroactive phase reconstruction exposes a missing Phase 1 gate: the plan needed
an explicit claim-token authorization contract for observations.

```text
PHASE_RECONSTRUCTION:complete
```
