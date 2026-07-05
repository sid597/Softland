# Phase Reconstruction - <Block Name>

Status: not reconstructed yet.

This artifact is a retroactive reconstruction. It is not a new design proposal.
Derive it only from committed source docs, tests, code comments, and commit
anchors listed in the block `BRIEF.md`.

The purpose is to give the Rama reviewer a clean target to compare against the
committed implementation.

## Source Evidence

List the committed docs/tests/code/comment sources used to reconstruct the
phases.

```text
```

## Phase 0 - Implicit Spec

What the committed docs imply the system was supposed to do.

Include:

- User/world problem.
- New data entering the world.
- Entities and identities.
- Invariants.
- Scale expectations.
- Failure cases.
- What must be durable.
- What must be replayable/auditable.
- What is explicitly out of scope.

```text
```

## Phase 1 - Plan

What a Rama-shaped plan would have been, reconstructed from the committed docs.

Include:

- Depots and first physical records.
- Topology choice: stream or microbatch.
- Partition/routing keys and why.
- PStates and key partitioners.
- Query/read surfaces.
- Idempotency strategy.
- Retry and partial-failure strategy.
- Side-effect gates.
- Restart/reconcile strategy.
- I/O and subindexing choices.

```text
```

## Phase 2 - Plan Validation

Would this reconstructed plan have passed an adversarial Rama review before code?

Include:

- Plan risks.
- Missing plan details.
- Wrong or ambiguous locality decisions.
- PState shape concerns.
- Retry/restart concerns.
- Verdict the plan should have received.

```text
```

## Phase 3 - Expected Implementation Shape

What code shape should exist if the reconstructed plan were implemented.

Include:

- Expected namespaces/files.
- Expected depots.
- Expected PStates.
- Expected topology branches.
- Expected helper APIs.
- Expected TaskGlobals/executors/caches.
- Expected tests.

```text
```

## Phase 4 - Expected Implementation Validation Focus

Which implementation checks matter most for this block.

```text
```

## Phase 5 - Expected Tests

What tests should exist if the Rama contract is correct.

```text
```

## Phase 6 - Expected Test Validation

How tests could still pass while the implementation is wrong.

```text
```

## Phase 7 - Expected Finish Evidence

What compile/runtime/integration evidence should exist before trusting this
slice.

```text
```

## Reconstruction Gaps

What the committed docs do not say clearly enough.

```text
```

## Reconstruction Verdict

One-sentence summary of whether the committed docs give enough information to
judge the implementation.

```text
PHASE_RECONSTRUCTION:complete
```
