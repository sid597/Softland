# Cross-Model Architecture Loop

Status: workflow insight, 2026-05-02.

This note captures the experiment that produced the Slice A compute spine. It is
not a model leaderboard and not a permanent claim about model identity. It is a
workflow discovery about how Claude, Codex, and the user combined to produce a
better architecture artifact than any one pass produced alone.

## Origin Prompt

The loop began from this pressure:

```text
Read the current dogfood runtime direction.

Do not assume there is an implementation handoff.

We are continuing the Rama/AOR dogfood-runtime direction. The settled shape is:
WorldDepot is truth, ComputeDepot handles physical execution, LLMDepot handles
agent/LLM execution. Workers and agents stream observations back into Rama; the
UI reads Rama PStates.

First, help choose the first vertical implementation slice.
Use ASCII diagrams and concrete Rama contracts, not prose abstractions.
```

The docs that Claude read for its first pass had themselves been shaped earlier
with Codex help. The experiment was therefore not "Claude versus Codex". It was
a chain:

```text
Codex-shaped docs
  -> Claude broad architecture initializer
  -> Codex contract review / gate
  -> Claude ingestion and redraw
  -> canonical readable artifact
```

## Artifact Lineage

```text
v2.1
  original stepping-stone design
  strong back-arrow insight, but still had weak claim-before-spawn mechanics

Full Shape
  Codex 5.5 acted as initializer, then another Codex reviewed
  strong system overview, showed Codex can initialize when needed

Locked post-gate artifact
  Claude 4.7 initialized, Codex reviewed, Claude repaired
  contract improved, but dense and hard to inhabit

Parts-flow artifact
  same repaired contract redrawn after user said it was not readable
  split into View 1 static parts/ownership and View 2 dynamic flow
  became the best canonical architecture artifact
```

The final artifact was not best because it was longer. It was best because it
separated two projections:

```text
View 1: what pieces exist, and who can write/read each one?
View 2: what actually happens from click to UI?
```

The redraw was not cosmetic. It was the step that made the corrected contract
inhabitable.

## Role Discovery

The practical role split:

```text
Claude initializes.
Codex falsifies and gates.
Claude ingests and redraws.
Codex implements.
```

More precisely:

```text
Claude broad draw
  - choose the slice
  - name the primitive
  - draw the human-scale system
  - preserve project meaning and future mirror paths

Codex contract pressure
  - check Rama docs and existing code
  - find PState ownership violations
  - find retry/side-effect bugs
  - find fake ack/partition/query claims
  - demand smallest concrete corrections

Claude final artifact stewardship
  - accept real corrections without defensive retreat
  - preserve valid broad structure
  - redraw the corrected shape so it can be used by another mind

Codex implementation
  - implement from the canonical artifact
  - keep scope tight
  - run tests
  - report implementation-proven contract deltas
```

Codex can initialize, and did produce a strong "Full Shape" artifact. But the
default architecture loop should still start with Claude because the first move
is broader than contract correctness. It must hold meaning, scope, naming,
future mirrors, and readability before mechanics collapse the design too early.

## Skills / Prompt Tools

Claude-side:

```text
ask-codex-for-feedback
  packages an artifact for Codex review
  names MODE / ALTITUDE / AUTHORITY
  preserves settled decisions and prevents relitigation

ingest-codex-feedback
  ingests Codex review
  classifies findings
  repairs minimally
  emits the next Codex prompt when useful

second-order-mirror
  prevents accommodation or defensiveness from masquerading as reasoning

rama-pitfalls
  domain checklist for Rama failure modes
```

Codex-side:

```text
review-cross-model-artifact
  review the supplied artifact, not a new universe

think-in-rama
  keep the loop depot -> topology -> decision/event -> PState -> projection
  central
```

The user should not have to invent every inter-model prompt. The skill loop
should carry the baton:

```text
Claude artifact
  -> /ask-codex-for-feedback
  -> Codex review
  -> /ingest-codex-feedback
  -> Claude repaired/readable artifact
  -> optional Codex gate
```

## Key Discoveries From The Slice A Loop

1. The right first slice is a spine, not a feature.

```text
compute-run-observation-spine
  request -> run -> claim -> observations -> live view
```

The UI feature is "run command", but the architectural primitive is the
Rama-owned run/claim/observation spine.

2. Claim-before-spawn is load-bearing.

The executor must not spawn merely because it saw `:pending`. It must ask Rama
to claim the run, then spawn only after reading a durable grant from the run row.

3. Back-arrow applies recursively.

The obvious back-arrow is:

```text
executor -> observations depot -> Rama -> UI
```

The subtler back-arrow is:

```text
executor -> claim depot -> Rama-owned claim -> executor
```

The runner is an actor in the Rama protocol, not a sidecar that mutates state.

4. Correctness pressure is not enough.

The locked post-gate artifact was more correct than the initial sketch but too
dense. The final View 1 / View 2 redraw made the architecture usable.

5. Docs and handoff must stay separate.

The architecture note is stable context. The implementation prompt is a handoff
only when the user chooses implementation. Do not smuggle an old task into the
global context pack.

## Default Future Protocol

For architecture work:

```text
1. Claude initializes from current docs.
2. Claude runs ask-codex-for-feedback.
3. Codex reviews at the requested altitude.
4. Claude runs ingest-codex-feedback.
5. Claude redraws final artifact into:
     View 1 -- static parts / ownership
     View 2 -- dynamic flow
     Load-bearing invariants
     Implementation appendix or prompt
6. Codex implements from the canonical artifact when the user chooses coding.
```

For implementation work:

```text
1. Use the implementation prompt/handoff.
2. Codex plans and implements the smallest slice.
3. Tests prove the contract.
4. Docs update only with implementation-proven deltas.
5. Code/test commits stay separate from private docs commits.
```

## Anti-Patterns

```text
"Same prompt to both models" as default
  Useful for independent sanity checks, but not the main workflow.

"Reviewer becomes architect"
  Codex should review the artifact unless a concrete claim is invalid.

"Dense corrected artifact is done"
  Not done until redrawn into an inhabitable projection.

"Implementation prompt hidden in global context"
  Keep active task handoff in docs/sessions/next-prompt.md or a dedicated
  implementation prompt, not in the global context summary.

"Infinite ping-pong"
  After two review/repair cycles, gate or ask the user for a decision.
```

## Current Result

The experiment produced:

```text
architecture/dogfood-runtime/slice-a-compute-run-command.md
```

That file is the current canonical architecture candidate for the first
dogfood-runtime vertical. The next implementation prompt is:

```text
90-prompts/implementation-slice-a-compute-run-command-prompt.md
```

The episode trace is:

```text
trails/2026-05-02-slice-a-cross-model-experiment.md
```

Until Rama stores trails natively, this trace file is the manual preservation
layer for origin pressure, artifact lineage, decisions, and implementation
proof.

If implementation changes the viable contract, update the architecture note
with the implementation-proven delta, not with speculative revisions.
