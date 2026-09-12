---
name: rama-retro-lens
description: >-
  Use when reviewing or implementing Softland Rama work through the prior
  retro-review failure patterns: feature-slice thinking, wrong depot/PState
  ownership, weak retry/idempotency, source-specific truth islands, inadequate
  product-path tests, collapsed boundaries, or helper completion before durable
  Rama truth is queryable.
---

# Rama Retro Lens

Use this skill after loading `$rama` for any Softland Rama review, repair, or implementation that might repeat old architectural mistakes. This skill is not a replacement for the Rama phased process. It is the adversarial lens that checks whether the phase artifacts and code actually learned from prior failures.

## Load Order

1. Load `$rama` first for Rama API/process constraints.
2. Load `$think-in-rama` when depot/PState/module ownership is being designed.
3. Use `$rama-retro-lens` before trusting a plan, after implementation, and before final reporting.

## Retro Documents

Use these as failure-pattern lenses, not as current implementation truth:

```text
history/docs/rama-retro-review/META_LEARNINGS.md
history/docs/rama-retro-review/ARCHITECTURE_VERDICT.md
history/docs/rama-retro-review/*/RAMA_REVIEW.md
```

Open only the RAMA_REVIEW files relevant to the current module or source path.

## Core Question

Ask this before accepting any Rama implementation:

```text
Did we build the product/world contract in Rama,
or did we create source-shaped rows and helper APIs that merely look like Rama truth?
```

## Failure Lenses

Check for these patterns explicitly:

1. Feature-slice thinking

The code follows a UI/helper/source workflow rather than the durable world contract. Look for helpers that "do the feature" while Rama owns only a thin append/read layer.

2. Wrong depot/PState ownership

The common kernel depends on source-specific modules, or source-specific truth owns what should be common truth. Check module launch order, mirror direction, depot partitioners, and which module owns accepted facts.

3. Weak retry/idempotency

The code assumes one append, one execution, one success. Check duplicate depot events, native identity conflicts, terminal status monotonicity, idempotency keys, retried completion events, and source-line/file-offset replay.

4. Source-specific truth islands

A product source creates private rows that are not readable through common APIs. Check whether markdown, transcript, and future sources converge into the same ObjectContainer/Revision/SourceAnchor/CompositionEdge contract.

5. Helper completion before durable truth

A public helper returns success or marks `:complete` before the PStates the product reads are queryable. Check read-after-write behavior through actual foreign/runtime APIs, not internal return values.

6. Inadequate product-path tests

Tests prove row construction, not the real product flow. Require tests for public helpers and existing product paths, including rejection, watch/resume, duplicate/idempotent append, and cross-module mirror timing when relevant.

7. Collapsed boundaries

A large namespace may be fine, but it is a failure if common kernel code, source-specific adapter code, operational resume state, and test/runtime helpers are mixed without real ownership boundaries.

## Review Procedure

1. Restate the product contract in one paragraph.
2. Draw the physical Rama path:

```text
external/input record
  -> depot
  -> topology decision
  -> accepted/rejected row
  -> event/materialization PStates
  -> query/helper read path
```

3. Identify authoritative truth:

```text
Which module owns the accepted fact?
Which PState is the durable source for each product claim?
Which rows are projections or operational state only?
```

4. Inspect actual code, not summaries.
5. Run at least one product-path test or create one if missing.
6. For every "complete" status, prove the product-readable PStates are already queryable.
7. For every source-specific adapter, prove it emits common material and does not become a private truth island.
8. For every retryable depot source, prove duplicate/replayed events are safe.
9. For every module dependency/mirror, prove the dependency direction matches ownership.

## Required Findings Shape

Lead with findings and file/line evidence:

```text
P1 - Title.
Evidence: path:line
Why this repeats/avoids retro failure:
Repair:
Test:
```

Then give a verdict:

```text
Learned:
Repeating:
Repair next:
```

If there are no findings, say which retro failure modes were actively checked and which tests prove them.

## Implementation Gate

Do not call Rama work "done" until these are true or explicitly scoped out:

```text
Product path uses the intended depot/module, not just a helper.
Accepted and rejected decisions are durable and queryable.
Terminal statuses are monotonic.
Public helpers wait for product-readable truth or clearly return async handles.
Duplicate/retry/idempotency behavior is tested or reasoned from deterministic keys.
Source-specific material is readable through common APIs.
Operational resume/offset/cursor state advances only after durable truth exists.
Cross-module mirror or dependency direction follows ownership.
Focused tests include happy path, rejection, and one retry/watch/resume edge when applicable.
```

## Best Use With Rama Phases

Use the standard `$rama` phases for new module design. Add this retro lens as a gate around the phases:

```text
Before Phase 0
  read relevant retro docs and name old failure modes that apply

After Phase 0 / implicit spec
  check product contract is not source-shaped

After Phase 1 / plan
  check depot/PState ownership, partitioning, retry/idempotency, terminal statuses

After implementation
  run product-path tests against real runtime helpers
  force rejection and duplicate/retry scenarios
  verify helpers do not return before queryable truth

Before final answer
  report what learned from the retro and what still repeats it
```

The common mistake is treating the phase artifacts as proof. They are prompts for better code, not evidence. Evidence is file/line inspection plus runtime tests through the actual product path.
