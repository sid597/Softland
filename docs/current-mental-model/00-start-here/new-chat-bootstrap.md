# New Chat Bootstrap

Use this prompt to start a new chat when the next direction is open and the new
session needs global context at the right level.

```text
We are working in /mnt/data/projects/Softland.

Before answering, read:

1. docs/current-mental-model/context-map.md
2. docs/current-mental-model/README.md
3. docs/current-mental-model/00-start-here/conversation-trail.md
4. docs/current-mental-model/trails/2026-05-02-slice-a-cross-model-experiment.md
5. docs/current-mental-model/10-anchors/rama-world-kernel-text-instance.md
6. docs/current-mental-model/architecture/action-request-kernel-routing.md
7. docs/current-mental-model/architecture/logical-lifecycle-and-derived-depots.md
8. docs/current-mental-model/architecture/object-container-spec.md
9. docs/current-mental-model/architecture/object-container-reviewer-world-model.md
10. docs/current-mental-model/architecture/dogfood-runtime/README.md
11. docs/current-mental-model/architecture/dogfood-runtime/compute-track.md
12. docs/current-mental-model/architecture/dogfood-runtime/agent-track-aor.md
13. docs/current-mental-model/architecture/dogfood-runtime/three-depot-current-system.md
14. docs/current-mental-model/architecture/dogfood-runtime/slice-a-compute-run-command.md
15. docs/current-mental-model/architecture/dogfood-runtime/llm-track-slice-roadmap.md
16. docs/current-mental-model/architecture/dogfood-runtime/llm-track-claude-research.md
17. docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md
18. docs/current-mental-model/architecture/rama-policy-throughput-post.md
19. docs/current-mental-model/architecture/rama-blog-patterns.md

Ignore older Softland lore unless I explicitly ask for it. Give highest weight
to the current mental model in this folder, especially the April 28 kernel
correction, the May 1 dogfood-runtime direction, the May 11 LLM contract MVP
implementation record, and the May 13 `1ef1cbd` rename/split that made active
code vocabulary space/turn plus `core.clj`/`text_kernel.clj`.

Do not assume there is an active implementation handoff. I may be bringing a new
potential direction to discuss. Use the current mental model as context, not as
marching orders.

The current center:

Projection shows materialized state
  -> user/agent acts on a projected target
  -> helper/API builds an ActionRequest
  -> ActionRequest enters a Rama depot
  -> Rama interpreter/policy derives accepted KernelEvent or rejected ActionDecision
  -> Rama ETL updates materialized state
  -> projection changes

The general kernel is:

event contract
action request contract
action decision contract
target contract
action contract
materialization contract
projection contract
policy contract
distillation contract

The event contract remains the accepted fact contract. The corrected lifecycle is:

ActionRequest asks.
ActionDecision records the answer.
KernelEvent happened.

ActionRequest is not a text/PDF/chat/code instance. It is a lifecycle envelope inside the general kernel.

Active source names after `1ef1cbd`:

shared contracts: src/app/server/rama/core.clj
text instance: src/app/server/rama/text_kernel.clj
dogfood space runtime: src/app/server/rama/dogfood/space.clj
LLM runtime: src/app/server/rama/dogfood/llm.clj

Historical docs may say world-thread/world-turn. In current code, read those as
space/turn unless the doc is explicitly preserving history.

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

Text-specific, PDF-specific, chat-specific, and code-specific details plug into payload and target/address. They should not change the kernel envelope.

Text is not the ontology. Text is the first carrier/instance used to exercise the kernel.

Current object-container correction:

ObjectContainer is the durable identity atom. SourceArtifact is immutable raw
source. Revision is versioned authored content. DerivedUnit is distiller output
until touched. CompositionEdge gives structure. Projection renders; Situate
proposes relationship/context. The old textArtifact concept retires because it
collapsed source, object identity, and revision into one text-shaped proof.

Do not collapse planes into fake options. Distinguish:

Plane = system-level concern / contract
Instance = concrete carrier, e.g. text/chat/PDF/code
Module = implementation boundary
Walkthrough = one vertical path through all contracts

Current implementation state:

V0 and V1 prove the request-first loop in code. Compute Slice A.0 and the LLM
contract MVP prove Rama-backed dogfood execution spines. This bootstrap alone
does not choose an implementation direction. If I say to resume active
implementation work, read `docs/sessions/next-prompt.md`; it may name a
currently chosen handoff. Otherwise treat physical routing/locality, policy
state, depot splitting, UI integration, real executor wiring, passive capture,
and editor batching as open systems questions unless I explicitly choose one.

Never add Rama I/O just because the code looks simpler. Follow the collaborative editor pattern: local buffer -> semantic edit object/batch -> document-keyed depot -> local transform.

V0 proof covered:

1. Ingest one text artifact.
2. Materialize artifact/text state.
3. Unitize into lines or paragraphs.
4. Accept/reject one unit in a branch.
5. Query canonical and discarded views separately.
6. Preserve provenance back to raw artifact/event.

Object-container first implementation direction, if explicitly chosen:

Preserve raw source as SourceArtifact, seed a document ObjectContainer, derive
markdown/plain-text units, render an outline, and prove that editing a derived
unit graduates it to a durable ObjectContainer with a Revision and SourceAnchor.
Do not start canvas, situate, global search, or full layer promotion before that
spine proves the invariant.

Current dogfood runtime direction:

Softland should dogfood itself through Rama with two execution tracks and three
conceptual depot families.

Execution tracks:

1. Compute track: build / test / run / deploy / serve / command execution.
2. LLM-agent track: chat / AOR-style agents / Codex / Claude / streamed
   reasoning / patches.

Conceptual depot families:

1. WorldDepot: truth, objects, refs, versions, decisions, accepted facts.
2. ComputeDepot: physical execution, builds, tests, commands, deploys,
   artifacts, logs.
3. LLMDepot: epistemic/agent execution, chat, AOR agents, Codex, Claude,
   streamed tokens, patches, tool traces.

Critical runtime rule:

Workers and agents stream observations back into Rama. Rama materializes live
PStates. The UI reads Rama. Workers and agents do not stream directly to the UI
as the source of truth.

Implemented dogfood-runtime verticals:

Compute Slice A.0 is `:compute/run-command` through a compute spine:

UI/executor append depots only; one topology owns PState writes; executor
claims through Rama before spawning; observations carry claim authority back
into Rama; UI reads the live PState projection.

The LLM contract MVP is the world-first Codex-backed LLM spine:

World action -> WorldTurn -> frozen ContextBundle -> derived LLM run request
-> LLM lifecycle/control/executor claim -> observations -> raw items/catalog
-> slices/forks/patch proposals/projections/cost rollups.

These slices are architecture context and implementation records. They are not
a forced task handoff unless I explicitly say we are starting new implementation
from them.

AOR is the main pattern for the agent track:

agent depot
  -> topology creates invoke/node state
  -> module-owned async executor runs work
  -> streaming depot receives chunks
  -> PStates materialize live state
  -> UI watches Rama

This is a current direction, not a forced task handoff. Ask what exact vertical
slice to implement before changing code if the user has not already named one.

When answering, do not give word-porn abstractions. Answer in context:

Here is the contract.
Here is the kernel data structure.
Here is how it wires to Rama.
Here is how text instantiates it.
Here is what is settled vs still open.
Here are the open choices and tradeoffs.

Prefer ASCII diagrams when the user is shaping architecture.
```
