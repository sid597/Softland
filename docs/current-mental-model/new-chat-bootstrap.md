# New Chat Bootstrap

Use this prompt to start a new chat when the next direction is open and the new
session needs global context at the right level.

```text
We are working in /mnt/data/projects/Softland.

Before answering, read:

1. docs/current-mental-model/context-map.md
2. docs/current-mental-model/README.md
3. docs/current-mental-model/conversation-trail.md
4. docs/current-mental-model/trails/2026-05-02-slice-a-cross-model-experiment.md
5. docs/current-mental-model/rama-world-kernel-text-instance.md
6. docs/current-mental-model/architecture/action-request-kernel-routing.md
7. docs/current-mental-model/architecture/logical-lifecycle-and-derived-depots.md
8. docs/current-mental-model/architecture/dogfood-runtime/README.md
9. docs/current-mental-model/architecture/dogfood-runtime/compute-track.md
10. docs/current-mental-model/architecture/dogfood-runtime/agent-track-aor.md
11. docs/current-mental-model/architecture/dogfood-runtime/three-depot-current-system.md
12. docs/current-mental-model/architecture/dogfood-runtime/slice-a-compute-run-command.md
13. docs/current-mental-model/architecture/rama-policy-throughput-post.md
14. docs/current-mental-model/architecture/rama-blog-patterns.md

Ignore older Softland lore unless I explicitly ask for it. Give highest weight
to the current mental model in this folder, especially the April 28 kernel
correction and the May 1 dogfood-runtime direction.

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

Do not collapse planes into fake options. Distinguish:

Plane = system-level concern / contract
Instance = concrete carrier, e.g. text/chat/PDF/code
Module = implementation boundary
Walkthrough = one vertical path through all contracts

Current implementation state:

V0 and V1 prove the request-first loop in code. This bootstrap alone does not
choose an implementation direction. If I say to resume active implementation
work, read `docs/sessions/next-prompt.md`; it may name a currently chosen
handoff. Otherwise treat physical routing/locality, policy state, depot
splitting, and editor batching as open systems questions unless I explicitly
choose one.

Never add Rama I/O just because the code looks simpler. Follow the collaborative editor pattern: local buffer -> semantic edit object/batch -> document-keyed depot -> local transform.

V0 proof covered:

1. Ingest one text artifact.
2. Materialize artifact/text state.
3. Unitize into lines or paragraphs.
4. Accept/reject one unit in a branch.
5. Query canonical and discarded views separately.
6. Preserve provenance back to raw artifact/event.

Current new direction:

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

Current first vertical candidate:

Slice A is `:compute/run-command` through a compute spine:

UI/executor append depots only; one topology owns PState writes; executor
claims through Rama before spawning; observations carry claim authority back
into Rama; UI reads the live PState projection.

This slice is architecture context. It is not a forced task handoff unless I
explicitly say we are starting implementation.

AOR is the main pattern for the agent track:

agent depot
  -> topology creates invoke/node state
  -> module-owned async executor runs work
  -> streaming depot receives chunks
  -> PStates materialize live state
  -> UI watches Rama

This is a current direction, not a forced task handoff. Ask what exact vertical
slice to implement before changing code.

When answering, do not give word-porn abstractions. Answer in context:

Here is the contract.
Here is the kernel data structure.
Here is how it wires to Rama.
Here is how text instantiates it.
Here is what is settled vs still open.
Here are the open choices and tradeoffs.

Prefer ASCII diagrams when the user is shaping architecture.
```
