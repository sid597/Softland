# Current Mental Model

> Historical context pack. The guidance below describes an earlier project
> state; use [the current documentation](../../docs/README.md) to begin work.
> See [the archive guidance](../README.md) when consulting these records.

Status: global context pack, updated 2026-06-06.

This folder is the current working mental model for the Rama kernel and
dogfood-runtime work. Use it to start new chats without rehydrating the whole
prior conversation.

Post `1ef1cbd`, active code vocabulary is:

```text
shared kernel contracts  -> src/app/server/rama/core.clj
text instance            -> src/app/server/rama/text_kernel.clj
dogfood space runtime    -> src/app/server/rama/dogfood/space.clj
LLM runtime              -> src/app/server/rama/dogfood/llm.clj
```

Older docs may still say world-kernel, world-thread, or world-turn. Treat those
as historical vocabulary unless the doc explicitly says it is current.

This is not the same thing as a task handoff. Global context tells a new session
how to think with us; a handoff tells it what to execute next. See
`context-map.md`.

## Read Order

1. `context-map.md`
2. `00-start-here/where-is-what.md`
3. `00-start-here/new-chat-bootstrap.md`
4. `00-start-here/conversation-trail.md`
5. `trails/2026-05-02-slice-a-cross-model-experiment.md`
6. `10-anchors/rama-world-kernel-text-instance.md`
7. `architecture/action-request-kernel-routing.md`
8. `architecture/logical-lifecycle-and-derived-depots.md`
9. `architecture/object-container-spec.md`
10. `architecture/object-container-ingester-contract.md`
11. `build/object-container-common-infra/PRODUCT.md`
12. `build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md`
13. `build/knowledge-earth-zui/CATEGORY_THEORY_DESIGN_RESEARCH.md`
14. `architecture/object-container-reviewer-world-model.md`
15. `build/chat-ingester/PRODUCT.md`
16. `architecture/dogfood-runtime/README.md`
17. `architecture/dogfood-runtime/compute-track.md`
18. `architecture/dogfood-runtime/agent-track-aor.md`
19. `architecture/dogfood-runtime/three-depot-current-system.md`
20. `architecture/dogfood-runtime/slice-a-compute-run-command.md`
21. `architecture/dogfood-runtime/llm-track-v2.md`
22. `architecture/dogfood-runtime/llm-track-canonical.md`
23. `architecture/dogfood-runtime/llm-track-derived-contract.md`
24. `architecture/dogfood-runtime/llm-track-slice-roadmap.md`
25. `architecture/dogfood-runtime/llm-track-claude-research.md`
26. `architecture/dogfood-runtime/transcript-capture.md`
27. `architecture/rama-world-kernel-v0-pr-trail.md`
28. `architecture/rama-policy-throughput-post.md`
29. `architecture/rama-blog-patterns.md`
30. `architecture/prompt-to-implementation-lossiness.md`
31. `architecture/cross-model-architecture-loop.md`

Read `90-prompts/implementation-review-prompt.md` only when the user is ready to review or
change code. Do not treat it as the default bootstrap for open-ended discussion.

`90-prompts/implementation-slice-a-compute-run-command-prompt.md` is historical
now that Slice A.0 has landed; read it only to understand the original
implementation protocol.

## Active Explorations

- `architecture/object-container-spec.md` — settled object substrate: source,
  container, revision, derived unit, composition, projection, and graduation.
- `architecture/object-container-ingester-contract.md` — corrective contract
  for source-specific ingesters: all ingesters may own acquisition and read
  models, but common `SourceArtifact` / `ObjectContainer` / `Revision` /
  `SourceAnchor` / `CompositionEdge` base truth must not become source-local
  islands.
- `build/object-container-common-infra/PRODUCT.md` - current active product
  seed for the common infra track. This supersedes the old "continue transcript
  F5/F6 next" handoff: markdown and transcript must both feed the shared
  Object-Container Kernel.
- `build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md` - founder-level
  design research thesis for the first imported-material view: Source-to-World
  as an accountable transformation surface, not a dashboard or graph spectacle.
- `build/knowledge-earth-zui/CATEGORY_THEORY_DESIGN_RESEARCH.md` - big-vision
  design research for Softland as a continuous semantic zoom world and
  category-theoretic environment for composing local worlds, learning arrows,
  conflict, synthesis, and collective intelligence.
- `architecture/object-container-reviewer-world-model.md` — Codex reviewer lens:
  where the object-container spec fits in the current kernel, what is ready, and
  what the first implementation slice must prove.
- `build/chat-ingester/PRODUCT.md` — product contract for
  transcript ingest as a source-specific object-container interpreter; separates
  import/watch, native containers, and downstream Space/LLM use.
- `build/chat-ingester/COMMON_KERNEL_REPLAN.md` — corrective gate for
  transcript ingest after the Phase 1 commonness failure: transcript acquisition
  and projections are source-specific, but base `ObjectContainer` material must
  feed the common object-container substrate.
- `architecture/policy-granularity-mastodon-parallel.md` — open question on
  fine-grained policy behavior without per-unit permission materialization.
- `architecture/cross-model-architecture-loop.md` — workflow note on using
  Claude for broad architecture initialization, Codex for contract review/gate,
  Claude for ingestion/redraw, and Codex for implementation.

## Current Trace Captures

- `trails/2026-05-02-slice-a-cross-model-experiment.md` — pre-Rama trace of
  how the Slice A compute spine emerged from Claude/Codex/user review loops,
  including artifact lineage, durable decisions, and the implementation handoff.

## Current Center

The system is one loop:

```text
Projection shows materialized state
  -> user/agent acts on a projected target
  -> helper/API builds an ActionRequest
  -> ActionRequest enters a Rama depot
  -> Rama interpreter/policy derives accepted KernelEvent or rejected ActionDecision
  -> Rama ETL updates materialized state
  -> projection changes
```

Everything else is a contract inside this loop.

## Current Dogfood Runtime Direction

The new direction is to make Softland dogfood itself through Rama, with two
execution tracks and three conceptual depot families.

Execution tracks:

```text
compute track
  = build / test / run / deploy / serve / command execution

LLM-agent track
  = chat / AOR-style agents / Codex / Claude / streamed reasoning / patches
```

Conceptual depot families:

```text
WorldDepot
  -> truth: objects, refs, versions, decisions, accepted facts

ComputeDepot
  -> physical execution: builds, tests, commands, deploys, artifacts, logs

LLMDepot
  -> epistemic / agent execution: chat, AOR agents, Codex, Claude, patches,
     streamed tokens, tool traces
```

The key runtime rule:

```text
workers and agents stream observations back into Rama
Rama materializes live PStates
the UI reads Rama
workers and agents do not stream directly to the UI as source of truth
```

See `architecture/dogfood-runtime/`.

Implemented verticals:

```text
architecture/dogfood-runtime/slice-a-compute-run-command.md
  -> Slice A compute spine: one local command run, Rama-owned claim,
     claim-tokened observations, and one live UI-readable PState.

architecture/dogfood-runtime/llm-track-slice-roadmap.md
  -> LLM contract MVP: world-first chat turns, frozen ContextBundles,
     LLM run lifecycle, controls, executor claims, raw items, catalog
     materialization, slices, forks, patch proposals, projections, and
     cost rollups.
```

These are stable architecture context plus implementation records. They are not
the current session handoff.

## Current Kernel Shape

The general kernel is still the outer shared structure. It was not replaced by
`ActionRequest`. The correction was to add lifecycle around the event contract:

```text
ActionRequest asks.
ActionDecision records the answer.
KernelEvent happened.
```

The event contract remains the accepted fact contract:

```text
KernelEvent =
  identity
  + type
  + actor
  + branch
  + context
  + target
  + action
  + payload
  + causality
  + ordering
  + policy
  + provenance
```

Text-specific, PDF-specific, chat-specific, and code-specific details plug into `payload` and `target/address`. They should not change the kernel envelope.

The same shared contracts appear in the request and accepted event:

```text
actor
branch
context
target
action
payload
causality
policy/provenance
```

What changes is timing and ownership:

```text
ActionRequest is proposed by the edge/helper.
ActionDecision is recorded by Rama.
KernelEvent is derived by Rama only after acceptance.
```

## Key Correction

Text is not the ontology. Text is the first carrier we use to exercise the general kernel.

The current object-substrate correction is:

```text
ObjectContainer is the durable identity atom.
SourceArtifact is immutable imported/captured source.
Revision is versioned authored content.
DerivedUnit is re-runnable distiller output until touched.
CompositionEdge gives structure.
Projection renders; Situate proposes relationship/context.
```

The old `textArtifact` name is retired as a final ontology term because it
collapsed source, object identity, and revision into one text-shaped proof.

The kernel is:

```text
event contract
action request contract
action decision contract
target contract
action contract
materialization contract
projection contract
policy contract
distillation contract
```

`ActionRequest` is not a text/PDF/chat/code instance. It is a lifecycle envelope
inside the general kernel. Text/PDF/chat/code are carriers.

Text plugs into this kernel as one instance:

```text
text artifact
text revision
text address/anchor
text-derived unit
unit status in a branch
text/editor/unit projections
```

## Working Rule

Do not collapse planes into fake options.

The right separations are:

```text
Plane = system-level concern / contract
Instance = concrete carrier, e.g. text/chat/PDF/code
Module = implementation boundary
Walkthrough = one vertical path through all contracts
```

## Current Implementation State

V0 and V1 prove the request-first loop in code. The compute Slice A.0 and LLM
contract MVP prove the same runtime shape for dogfood execution. The global
context does not prescribe the next implementation direction.

Open design pressure to preserve:

```text
physical routing/locality still needs clearer Rama shape
policy state is not yet a full Rama-owned authorization model
future depot splitting remains a systems question
editor/write batching remains a systems question
LLM UI integration and real executor wiring remain separate from the contract MVP
transcript capture / passive observation is designed but not implemented
full repair/rebuild tooling for derived rollups is still future work
```

Do not make every physical UI gesture a world action. For typing, follow the
collaborative editor pattern: local buffer, versioned edit operation or edit
batch, artifact/document-keyed depot, and Rama-side transform/decision. See
`architecture/rama-blog-patterns.md`.

V0 proof covered:

```text
1. Ingest one text artifact.
2. Materialize artifact/text state.
3. Unitize into lines or paragraphs.
4. Accept/reject one unit in a branch.
5. Query canonical and discarded views separately.
6. Preserve provenance back to raw artifact/event.
```
