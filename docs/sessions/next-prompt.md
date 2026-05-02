# Session Resume — Active Slice A Implementation Handoff

Status: active implementation handoff, 2026-05-02.

For a new session, start from:

```text
docs/current-mental-model/context-map.md
docs/current-mental-model/implementation-slice-a-compute-run-command-prompt.md
```

The user has chosen to start implementing the first dogfood-runtime vertical:
Slice A `:compute/run-command`. Use the implementation prompt above to plan and
implement the smallest safe Slice A.0. Do not restart architecture selection.

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

For this implementation handoff, read these before editing:

```text
.agents/skills/think-in-rama/SKILL.md
docs/current-mental-model/README.md
docs/current-mental-model/implementation-slice-a-compute-run-command-prompt.md
docs/current-mental-model/architecture/action-request-kernel-routing.md
docs/current-mental-model/architecture/logical-lifecycle-and-derived-depots.md
docs/current-mental-model/architecture/dogfood-runtime/README.md
docs/current-mental-model/architecture/dogfood-runtime/slice-a-compute-run-command.md
docs/current-mental-model/architecture/rama-blog-patterns.md
docs/current-mental-model/architecture/rama-policy-throughput-post.md
docs/current-mental-model/architecture/prompt-to-implementation-lossiness.md
docs/reference/rama/28-clj-defining-modules.md
docs/reference/rama/29-clj-dataflow-lang.md
docs/reference/rama/15-pstates.md
docs/reference/rama/11-stream-topologies.md
docs/reference/rama/25-integrating.md
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

## Active Implementation Handoff

Implement Slice A.0:

```text
:compute/run-command
  -> Rama-owned run lifecycle
  -> Rama-owned claim before process spawn
  -> claim-tokened stdout/stderr/exit observations
  -> live UI-readable PState
```

Primary implementation target:

```text
src/app/server/rama/dogfood/compute.clj
test/app/server/rama/dogfood_compute_test.clj
```

Do not implement in this slice:

```text
LLM / Codex / Claude agent track
cancel
restart reconcile / :lost
serve / daemon lifecycle
artifact production
Compute -> World mutation bridge
full UI polish beyond the minimal live state proof
```

Before coding, follow the preflight in:

```text
docs/current-mental-model/implementation-slice-a-compute-run-command-prompt.md
```

The first pass should prove:

```text
["echo" "hello"] -> :succeeded with stdout in live view
["false"]        -> :failed with nonzero exit code
double claim     -> only one durable winner, no double spawn
```

The concurrent architecture exploration is the LLM/agent mirror of this spine.
That work should not block Slice A.0 implementation and should not be
implemented in the compute patch.
