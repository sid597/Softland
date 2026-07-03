# Brief - World Kernel Contract

## Review Block

This block reviews the committed foundation for:

```text
Projection -> ActionRequest -> depot/routing -> Rama decision
  -> accepted KernelEvent or rejected ActionDecision
  -> materialized state / projection
```

The goal is to verify whether the shared contract actually supports the later
compute, space, LLM, transcript, and text-kernel slices.

## Commit Anchors

```text
bc058a7 2026-04-29 Implement Rama world kernel request pipeline
7a579b3 2026-04-29 Complete Rama world kernel V1 request contract
81bd089 2026-04-29 Canonize rejected ActionDecision shape
175f7cf 2026-05-01 Capture logical lifecycle and derived depots
```

Related architecture/context commits:

```text
6f9e62d 2026-04-29 Capture current mental model docs
f69add7 2026-04-29 Clarify Rama lifecycle mental model
19d53ef 2026-04-29 Capture Rama blog implementation patterns
b0ee81b 2026-05-01 Separate global context from implementation handoff
```

## Code Files

```text
src/app/server/rama/core.clj
src/app/server/rama/util_fns.cljc
```

## Current Test Files

```text
test/app/server/rama/text_kernel_test.clj
```

Historical note: the old `world_kernel_test.clj` path appears in commit
history, but the current test path was renamed to `text_kernel_test.clj`.

## Architecture Docs

```text
docs/current-mental-model/architecture/action-request-kernel-routing.md
docs/current-mental-model/architecture/logical-lifecycle-and-derived-depots.md
docs/current-mental-model/architecture/rama-world-kernel-text-instance.md
docs/current-mental-model/architecture/rama-policy-throughput-post.md
docs/current-mental-model/architecture/rama-blog-patterns.md
docs/architecture/think-in-rama.md
```

## Main Review Question

Does the committed shared kernel contract truly separate proposed requests,
Rama decisions, accepted world facts, and materialized projections in a way that
later modules can reuse without inventing parallel envelopes?

## Specific Questions

- Is the first durable record for user/world writes an ActionRequest, not a
  preaccepted event?
- Can rejected requests be durably queried without creating KernelEvents?
- Are request ids, idempotency keys, proposed event ids, and routing keys
  distinct?
- Does common validation run before action-specific dispatch?
- Are target kinds and action shapes general enough for the existing modules
  without being a vague catch-all?
- Are compatibility helpers clearly transitional, or do they keep old accepted
  event behavior alive?
- What tests would fail if a module accepted facts before Rama decided them?

## Out Of Scope

- Object-container Slice 1 implementation review.
- In-process chat ingester work.
- New kernel feature design.
