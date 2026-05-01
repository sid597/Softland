# Session Resume — No Active Rama Implementation Handoff

Status: the active need is global context for discussing a new direction, not an
implementation handoff.

For a new session, start from:

```text
docs/current-mental-model/context-map.md
docs/current-mental-model/new-chat-bootstrap.md
```

Do not infer any next implementation from this file. If the user brings a new
direction, use the global context pack and discuss that direction first.

## Starting Point

We are working in:

```text
/mnt/data/projects/Softland
```

The private mental-model docs live on the local branch:

```text
docs/current-mental-model-local
```

This branch is private/local. Do not push it. Public code lives on `main`.

Current public `main` already contains the V0 and V1 Rama kernel code:

```text
bc058a7 Implement Rama world kernel request pipeline
d811906 Complete Rama world kernel V1 request contract
```

Those public commits touched only:

```text
src/app/server/rama/core.clj
src/app/server/rama/util_fns.cljc
test/app/server/rama/world_kernel_test.clj
```

The private docs branch contains the current mental model and implementation
trail. Keep docs commits local/private unless the user explicitly changes that
rule.

## Read Order

For Rama work, read these before editing:

```text
.agents/skills/think-in-rama/SKILL.md
docs/current-mental-model/README.md
docs/current-mental-model/new-chat-bootstrap.md
docs/current-mental-model/rama-world-kernel-text-instance.md
docs/current-mental-model/architecture/action-request-kernel-routing.md
docs/current-mental-model/architecture/logical-lifecycle-and-derived-depots.md
docs/current-mental-model/architecture/policy-granularity-mastodon-parallel.md
docs/current-mental-model/architecture/rama-blog-patterns.md
docs/current-mental-model/architecture/rama-policy-throughput-post.md
docs/current-mental-model/architecture/prompt-to-implementation-lossiness.md
```

For code, inspect:

```text
src/app/server/rama/core.clj
test/app/server/rama/world_kernel_test.clj
```

## Canonical Loop

The settled top-level lifecycle is:

```text
Projection
  -> ActionRequest
  -> Depot
  -> Topology
  -> ActionDecision
  -> KernelEvent?
  -> PStates
  -> Projection
```

Meaning:

```text
ActionRequest = proposed world change
Depot = durable Rama entry point
Topology = Rama-owned interpreter/decision logic
ActionDecision = durable Rama answer
KernelEvent = accepted world fact, only when accepted
PStates = materialized/queryable world views
Projection = inhabitable/readable world surface
```

Rejected actions are durable decisions, not accepted world events:

```clojure
{:decision/status :rejected
 :request/id ...
 :request/type ...
 :routing/key ...
 :event/id nil
 :decision/reason ...
 :errors ...}
```

Accepted decisions point to or contain the accepted event:

```clojure
{:decision/status :accepted
 :request/id ...
 :request/type ...
 :routing/key ...
 :event/id ...
 :event ...}
```

## Current Code Shape

V1 currently has:

```text
*world-requests-depot hashed by :routing/key
ActionRequest before KernelEvent
ActionDecision for accepted/rejected outcomes
KernelEvent only after acceptance
request validation before action dispatch
rejected decisions with :event/id nil and :decision/reason
helper APIs using :proposed-event-id, not ambiguous :event-id
tests proving the V1 contract
```

Covered action families:

```text
:artifact/ingest
:unit/status-set
:compat/record
unknown action rejection
```

Important current limitation:

```text
:routing/key is canonical on requests, but some PStates still key by plain ids.
This is documented as transitional, not final.
```

Current policy is scaffolding:

```text
authorized-request? checks capabilities in the request envelope.
Real policy must move into Rama-owned PStates/mirrors.
```

## No Implementation Handoff

There is no active Rama implementation handoff. The next implementation direction
must be chosen in conversation with the user.

Current known code pressure, descriptive only:

```text
:routing/key exists on requests
some PStates still use plain ids
policy is still scaffolding
future depot boundaries are still a systems question
```
