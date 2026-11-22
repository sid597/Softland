---
name: think-in-rama
description: Use when designing, reviewing, or implementing Rama-backed Softland architecture, especially depot boundaries, PState shape, stream vs microbatch topology choice, partitioning/routing keys, ActionRequest/KernelEvent semantics, policy placement, accepted/rejected decision trails, throughput concerns, or migration away from app-shaped modules toward Rama-first systems.
---

# Think In Rama

## Overview

Think from Rama as the application substrate, not Rama as a database attached to
an app server. New data enters through depots, topologies own interpretation and
state transitions, PStates are materialized views, and queries/projections read
those views.

For substantial architecture work, read:

```text
docs/architecture/think-in-rama.md
```

For the specific Softland policy/throughput debate, read:

```text
history/old-docs/architecture/action-request-kernel-routing.md
history/old-docs/architecture/logical-lifecycle-and-derived-depots.md
history/old-docs/architecture/rama-policy-throughput-post.md
history/old-docs/architecture/rama-blog-patterns.md
history/old-docs/architecture/prompt-to-implementation-lossiness.md
```

For policy granularity questions, especially "do we need a row per tiny unit?",
read:

```text
history/old-docs/architecture/policy-granularity-mastodon-parallel.md
```

## Core Stance

Prefer this loop:

```text
Projection -> ActionRequest -> depot -> Rama decision
  -> accepted KernelEvent or rejected ActionDecision
  -> PState materialization -> Projection
```

Do not let the API, UI, helper namespace, or current app module shape become the
hidden source of truth.

## Review Workflow

When reviewing or designing Rama work:

1. Restate the contract before editing so interpretation is visible.
2. Identify the new data entering the world.
3. Distinguish request/proposal from accepted fact.
4. Identify the entity whose local ordering matters.
5. Choose depot boundaries by relatedness, ordering, locality, and topology consumers.
6. Choose partition/routing keys before writing topology code.
7. Shape PStates around the questions/actions they must serve.
8. Place authoritative policy decisions inside Rama when they affect world truth.
9. Choose stream for interactive low-latency actions and microbatch for bulk/derived work.
10. Use query topologies for clustered reads or allowed-action decisions that should not live in UI code.
11. Preserve traceability from projection item to request, decision, event, and materialized state.
12. Name the tests that would fail if the contract was misunderstood.

Preflight for implementation:

```text
What is the first physical record Rama sees?
What fields must accepted/rejected ActionDecision carry?
Can ActionRequest contain any event id, and under what explicit name?
Does common request validation run before dispatch for every request?
Is :routing/key semantic identity or exact physical locality?
Which tests prove those answers?
```

## Softland Defaults

Use these defaults unless the codebase or user explicitly proves otherwise:

```text
ActionRequest is depot input.
ActionDecision is the durable answer.
KernelEvent is an accepted world fact.
PStates are derived materialized views.
Projection items carry target refs.
Policy decisions happen in Rama if they must be replayable/auditable.
Routing key follows the affected world entity.
Request id is an index key, not usually a locality key.
Depots split by relatedness/local ordering/locality, not by old namespaces.
Topology-owned derived depots should reject client appends.
```

## Policy Placement

Separate these layers:

```text
edge guard: session, payload size, rate limit
request typing/routing: construct envelope and routing key
authoritative decision: target existence, permission, transition validity
projection filtering: what this actor can see and why
```

Edge guards and request packaging may happen before append. Authoritative world
decisions should happen after depot entry inside Rama, from durable PStates.

## Throughput Rubric

Treat throughput as a locality question:

```text
Is the depot keyed by the entity being changed?
Does the topology start near the PState shard it needs?
Are we hashing by request id before business logic?
Are unrelated request families forcing filtering in one topology?
Are global/all-partition operations rare and intentional?
Could microbatch replace per-record stream work for derived rebuilds?
```

Request-first is not the throughput smell. Random generic depots, wrong keys,
and unnecessary partition hops are the smell.

Never trade Rama I/O efficiency for code simplicity. Prefer a little more code
over extra per-query/per-action disk reads, network hops, or hot-path PState
writes.

For editor-like work, use the collaborative editor pattern: local buffer,
semantic edit object or edit batch, depot keyed by document/artifact id, local
transform against versioned edit history, and reactive reads for client updates.

## Red Flags

Push back when you see:

```text
"Validate everything before Rama."
"One world depot forever."
"This PState is source truth."
"We will add partitioning later."
"The UI knows whether this is allowed."
"Rejected actions can disappear."
"Key it by request id because every request has one."
"Append accepted events built outside Rama for user actions."
"The projection hides something but cannot explain why."
```

## Output Style

For architecture answers, give the user:

```text
current shape
Rama-shaped concern
recommended shape
why it preserves traceability
what code/doc should change next
```

For code changes, prefer small patches that move the system toward the loop
above without preserving old app-shaped module boundaries just because they
already exist.
