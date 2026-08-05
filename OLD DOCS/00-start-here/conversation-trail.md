# Conversation Trail

Status: context note, 2026-05-11.

This note explains how we arrived at `10-anchors/rama-world-kernel-text-instance.md`. It is not a transcript. It preserves the conceptual path so a new chat can understand why the current model has the shape it has.

## Starting Pressure

The user was trying to understand the object shape behind Softland-like work:

```text
code
chat history
PDFs
LLM replies
text editors
commitable artifacts
accepted/rejected fragments
branches of exploration
permissions over granular structure
Rama event sourcing
```

The core problem was not "how do we store text?" It was:

```text
How can raw work history become addressable, forkable, distillable, permissioned, and reinterpretable later?
```

## Chat As Failure Mode

The user identified that normal chat loses context:

```text
rich local world in the user's head
  -> flattened chat message
  -> detached LLM answer
  -> user manually remaps answer back into their world
```

This led to the requirement that LLM responses should not be detached text. They should attach to the current context/world:

```text
this restates node A
this extends thought B
this disagrees with hypothesis C
this creates branch D
this should become checkpoint E
```

## Commitable Artifacts

The user then sharpened the problem with a concrete UI pressure:

```text
If an assistant replies with 10 lines,
the UI must not force accept whole / reject whole.
```

This produced the distinction:

```text
Artifact = thing that arrived or was produced
Unit = addressable part or interpretation of an artifact
Annotation/status = branch-local judgment over a unit
Commit = selected units promoted into a world under an interpretation
```

Rejected material should not be deleted. It is data:

```text
rejected by user
hidden from canonical view
kept for provenance, learning, later debugging, and redistillation
```

## Category-Theory Pass

The model was then category-theorized using Ologs/CatColab-style thinking:

```text
Olog/schema/category = concepts and lawful arrows
World_b : O -> Set = branch-indexed instance/model of that schema
Events = generators of causal history
Distillers = versioned translations from raw artifacts to units/relations
Views = projections/lenses out of world state
Permissions = allowed observations, not merely row access
```

Important result:

```text
The identity of an object is not only its bytes.
It is the family of lawful ways it can be addressed, observed, transformed,
related, permissioned, projected, and redistilled.
```

## CatColab / Topos Interpretation

CatColab stood out as evidence for:

```text
model first
view/analysis second
migration between logics third
collaborative structured editing fourth
```

But the gap for this project is earlier than CatColab:

```text
CatColab starts once the thing is already formal.
This system must help raw chat/PDF/code/thought become progressively formal
without losing provenance.
```

## The First Bad Cut

A first implementation framing said:

```text
raw text artifact
  -> addressable text surface
  -> unitized interpretations
  -> branch-local accept/reject/promote statuses
  -> views/projections
  -> permissions over observations

That is the first Rama slice.
```

The user pushed back correctly: this collapsed system concerns into one "text slice" and made text look like the ontology.

## Revised Cut

The correction was to stop treating concerns as peer options or independent slices. Instead:

```text
Plane = system-level concern / contract
Instance = concrete carrier, e.g. text/chat/PDF/code
Module = implementation boundary
Walkthrough = one vertical path through all contracts
```

The planes are intertwined, but separable by contracts:

```text
truth/event plane
object/target plane
action plane
address plane
materialization plane
projection plane
policy plane
distillation plane
```

## Final Current Model

The model first clicked when expressed as a single loop, and was later corrected
to split request from accepted event:

```text
Projection shows materialized state
  -> user/agent acts on a projected target
  -> helper/API builds an ActionRequest
  -> ActionRequest enters a Rama depot
  -> Rama interpreter/policy derives accepted KernelEvent or rejected ActionDecision
  -> Rama ETL updates materialized state
  -> projection changes
```

The general kernel is the set of contracts inside this loop:

```text
action request
action decision
event / accepted fact
target
action
materialization
projection
policy
distillation
```

Text is the first instance used to test the kernel:

```text
text artifact
text revision
text range/anchor
text unitization
unit status update
canonical/discarded projections
```

## Latest Sharpening: Kernel Data Structure First

The user clarified that the first step is to work out the **general kernel data structure** before moving into text.

The current kernel event shape:

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

This answers seven core questions:

```text
What happened?       event/type
Who did it?          actor
In which world?      branch
From what context?   context
To what target?      target
By what action?      action
With what causality? causal
```

Text, PDF, chat, and code plug into `payload` and `target/address`. They should not change the kernel envelope.

## Important Communication Constraint

When continuing from this context, do not answer with detached abstractions.

Prefer:

```text
Here is the contract.
Here is the kernel data structure.
Here is how it wires to Rama.
Here is how text instantiates it.
Here is the current implementation boundary, if relevant.
Here are the open choices and tradeoffs.
```

Avoid:

```text
word lists with no wiring
choosing a path without presenting the tree
collapsing conceptual planes into implementation slices
making text/editor the ontology
```

## 2026-05-01: Dogfood Runtime Direction

The user then shifted from the text-instance kernel walkthrough to the first
major dogfood direction:

```text
store Softland through Rama
use Rama to build/test/run/deploy/serve Softland
use Rama/AOR-style execution for LLM agents, Codex, Claude, and streamed patches
```

The important correction was that the first useful system is not "code as the
bottom layer". The current focus is narrower and more concrete:

```text
two execution tracks
  1. compute
  2. LLM-agent

three conceptual depot families
  1. World
  2. Compute
  3. LLM-agent
```

The user also caught a missing architecture arrow:

```text
worker/agent -> Rama -> PStates -> Softland UI
```

This means:

```text
Workers and agents do not stream directly to the UI as source of truth.
They stream observations back into Rama.
Softland reads the materialized Rama state.
```

AOR supplied the key implementation pattern for the agent track:

```text
agent depot
  -> topology creates invoke/node state
  -> module-owned async executor runs node work
  -> streaming depot receives chunks
  -> PStates materialize live run state
  -> UI watches Rama
```

The corresponding Softland shape:

```text
LLMDepot
  -> AgentTopology
  -> AgentRunPState
  -> AgentExecutor
  -> LLMObservationDepot
  -> LLMViewsPState
  -> Softland UI
```

Compute uses the same back-arrow but with runner-like execution:

```text
ComputeDepot
  -> ComputeTopology
  -> ComputeRunPState
  -> ComputeExecutor
  -> ComputeObservationDepot
  -> ComputeViewsPState
  -> Softland UI
```

The stable boundary:

```text
WorldDepot is truth.
ComputeDepot does physical execution.
LLMDepot does epistemic / agent execution.

Compute and LLM can propose, observe, and request world changes.
They do not secretly mutate WorldPStates.
```

## 2026-05-11: LLM Contract MVP Landed

The LLM-agent track moved from architecture contract to an implemented Rama
spine. The important shape is world-first:

```text
World action
  -> WorldTurn
  -> frozen ContextBundle
  -> derived LLM run request
  -> LLM run lifecycle / controls / executor claim
  -> observations
  -> raw items, catalog materialization, projections, patch proposals, costs
```

The implementation proved the same core rule as the dogfood direction:

```text
helpers and executors append depots only
topologies write PStates
derived projections are rebuildable from canonical state and depot history
```

What the MVP includes:

```text
WorldTurns and ContextBundles
LLMThread / LLMTurnRun lifecycle
world-first approval, cancel, compact, and steer controls
executor claim/grant/observation boundary
follow-up runs on bound native threads
raw LLM item indexes and object catalog rows
slices, overlays, derivatives
fork/reconciliation flows
patch proposal creation and accept/reject turns
rebuildable projections
per-thread token/cost rollups
```

The topology simplification around cost rollups is explicitly documented
because it is the kind of tradeoff future sessions could otherwise forget:

```text
lost: hot-path self-repair if $$llm-cost-by-thread is manually corrupted
kept: deterministic replay from depot history and no double-counting
repair: rebuild the rollup from canonical run token usage
```

The durable implementation record is
`architecture/dogfood-runtime/llm-track-slice-roadmap.md`.
