# Brief - Compute Track

## Review Block

This block reviews the committed Rama-backed compute execution slice:

```text
compute request
  -> Rama acceptance/rejection
  -> pending run PState
  -> module-owned executor claim
  -> child process
  -> observation depot
  -> run/view PStates
```

## Commit Anchors

```text
77833e6 2026-05-04 feat: add compute TaskGlobal executor
23f7ceb 2026-05-04 docs: update compute TaskGlobal architecture
db4f105 2026-05-03 docs: clarify compute executor tick ownership
ab362a7 2026-05-03 docs: update compute runtime context
99f1a20 2026-05-02 Capture Slice A handoff and cross-model trace
```

## Code Files

```text
src/app/server/rama/dogfood/compute.clj
```

## Test Files

```text
test/app/server/rama/dogfood_compute_test.clj
```

## Architecture Docs

```text
docs/current-mental-model/architecture/dogfood-runtime/README.md
docs/current-mental-model/architecture/dogfood-runtime/compute-track.md
docs/current-mental-model/architecture/dogfood-runtime/slice-a-compute-run-command.md
docs/current-mental-model/architecture/dogfood-runtime/three-depot-current-system.md
```

## Main Review Question

Does the compute slice keep physical command execution behind Rama-owned request,
claim, observation, and materialized-view state, with no executor-side hidden
truth?

## Specific Questions

- Is the first physical record a compute request depot record?
- Does Rama accept/reject the run before a process can spawn?
- Is the claim path durable enough to prevent double spawning?
- Are observations authorized by run id and claim token?
- Are out-of-order or duplicate observations safe?
- Does process failure become run state rather than request rejection?
- Are pending-run scans bounded enough for the expected local executor shape?
- Are stdout/stderr tails bounded and retry-safe?
- Do tests prove double-claim and wrong-token behavior?

## Out Of Scope

- LLM agent execution.
- Transcript capture.
- New compute operations beyond the committed command-run slice.
