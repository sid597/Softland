# Rama Retro Review

Purpose: review already committed Rama implementation slices with the Rama skill
after the fact.

This is not a new implementation handoff and not a roadmap. It is a review
workspace for committed code that predates the current Rama-skill workflow.
The goal is to find whether the old work was done the wrong way, and if so
whether the problem came from requirements, plan, validation, implementation,
tests, or runtime evidence.

## Start Here

Use this folder as a closed review system.

Local operating files:

- `PROMPT.md` - self-contained prompt for running a retroactive Rama review.
- `PHASE_RECONSTRUCTION_TEMPLATE.md` - artifact format for reconstructing what
  the Rama phases should have produced from committed docs.
- `REVIEW_TEMPLATE.md` - artifact format to copy into a block's
  `RAMA_REVIEW.md`.
- `<block>/BRIEF.md` - committed slice scope and evidence list.
- `<block>/PHASE_RECONSTRUCTION.md` - reconstructed phase artifacts derived
  from the committed product/implementation/architecture docs.
- `<block>/RAMA_REVIEW.md` - review artifact to fill.

For this retro review, the local files above are the operating system. This
folder is review-only.

## Scope Rule

Only committed work is in scope.

Out of scope:

- In-process work by other agents.
- `build/chat-ingester/`.
- `build/transcript-object-ingest/`.
- New feature design.
- Object-container Slice 1 as a target of this retro review, because it was
  implemented with the Rama skill already. It can be used as a comparison point
  when useful.

## Review Blocks

1. `01-world-kernel-contract/`
   - ActionRequest, ActionDecision, KernelEvent, routing, and shared envelope.
2. `02-compute-track/`
   - Compute request, claim, observation, executor, and view lifecycle.
3. `03-space-runtime-spine/`
   - Space/world user-facing truth, turns, context bundles, catalog, projections,
     slices, forks, patch proposals, and controls.
4. `04-llm-agent-track/`
   - LLM run lifecycle, claims, observations, approvals, cost rollups, Codex and
     Claude executor surfaces.
5. `05-transcript-capture/`
   - Committed passive transcript capture implementation only.
6. `06-text-kernel-shape/`
   - Kernel shape data, text-kernel split, identity headers, and consistency of
     kernel instances.

## File Pattern

Each block has:

- `BRIEF.md` - committed scope, source files, test files, docs, and the review
  question.
- `PHASE_RECONSTRUCTION.md` - retroactive reconstruction of the phase artifacts
  that the committed docs imply.
- `RAMA_REVIEW.md` - the future Rama-skill review artifact for findings,
  evidence, tests, and recommended repairs.

To review a block:

1. Read `PROMPT.md`.
2. Read the block's `BRIEF.md`.
3. Read the product/implementation/architecture docs listed in `BRIEF.md`.
4. Use `PHASE_RECONSTRUCTION_TEMPLATE.md` to fill the block's
   `PHASE_RECONSTRUCTION.md` from those committed docs.
5. Copy `REVIEW_TEMPLATE.md` into the block's `RAMA_REVIEW.md`, or use it as the
   structure if `RAMA_REVIEW.md` already exists.
6. Read only the committed code and tests listed in the block brief.
7. Compare the committed code/tests against `PHASE_RECONSTRUCTION.md`.
8. Let the Rama lens decide what is wrong, including whether the failure belongs
   to spec, plan, implementation, tests, or runtime evidence.
9. Fill `RAMA_REVIEW.md`.
10. Stop. Do not patch code.

## Review Lens

For each block, check:

- What is the first physical record Rama sees?
- Does Rama own acceptance/rejection?
- Are request/proposal and accepted fact separated?
- Are rejected decisions durable?
- Is the routing key semantic locality, not just request identity?
- Are depot partitioners aligned with the PStates touched by the topology?
- Are PStates shaped around actual read/projection questions?
- Are side effects behind claims or otherwise retry-safe?
- Are duplicate depot records/idempotency cases correct?
- Are large scans subindexed or yielded where needed?
- Are query paths avoiding unnecessary seeks and network roundtrips?
- Do tests prove the contract that would fail if the interpretation was wrong?
