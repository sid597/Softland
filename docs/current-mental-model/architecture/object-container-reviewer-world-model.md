# Object Container Reviewer World Model

Status: reviewer lens for future sessions, 2026-06-06.

This note is not a competing spec. It is Codex's reviewer pass over the
object-container conversation and `object-container-spec.md`: what to preserve,
where it fits in the current Rama kernel language, what is implementation-ready,
and what still needs an explicit contract before code.

Origin questions:

```text
What is the final world model I should have, given all the questions from my
side and this chat?

Where does all this fit in the current kernel language and context we have?

Is the current spec ready for implementation?

Does ObjectContainer have textArtifact? For keeping the original source at
ingest, should the imported thing directly map to an ObjectContainer, or is
that still an open question?
```

Why this matters:

```text
The conversation resolved the meaning-level confusion around text, canvas,
outliner, chat, raw source, projection, and situating. The risk now is losing
that resolution during implementation by either:

1. turning the object-container model into a parallel kernel, or
2. treating a good architecture spec as if it already contains all depot,
   PState, event, idempotency, and test contracts.
```

## Reviewer Verdict

The current object-container spec is the right architecture substrate.

It is ready to drive a first implementation slice, but not ready as a whole
system implementation plan. The next work should not be "build everything in
the spec." The next work should be one narrow proof:

```text
markdown/plain-text source
  -> immutable SourceArtifact
  -> document ObjectContainer
  -> derived block units
  -> outline projection
  -> edit derived unit
  -> graduated durable ObjectContainer + Revision + SourceAnchor
```

The spec fits inside the current kernel. It does not replace the kernel.

```text
current kernel = lifecycle/law/event envelope
object-container spec = world-object substrate inside that lifecycle
```

## What To Preserve

These decisions should not be reopened unless implementation proves a concrete
contradiction:

```text
1. The atom is ObjectContainer, not text.
2. textArtifact retires as a name and concept.
3. SourceArtifact, ObjectContainer, and Revision are separate identities.
4. Imported source is preserved raw and immutable.
5. A parsed boundary is not automatically durable truth.
6. Derived units are re-runnable until touched; touched units graduate.
7. Composition lives as edges, not parent/children fields on containers.
8. Projection and situate are different operations.
9. ViewState belongs to placement, not object.
10. Inferred structure is candidate until accepted.
```

The key correction is this:

```text
text_kernel collapsed three roles:
  raw source
  editable object
  content revision

object-container splits them:
  SourceArtifact
  ObjectContainer
  Revision
```

So the answer to "does ObjectContainer have textArtifact?" is:

```text
No.

ObjectContainer can have a text Revision.
ObjectContainer can be anchored to a SourceArtifact.
ObjectContainer can be rendered in text affordances.

But textArtifact is not a child field, wrapper, or ontology layer. It is the old
collapsed proof shape.
```

## Kernel Fit

Object-container work should use the same Rama lifecycle as every other world
change:

```text
Projection
  -> ActionRequest
  -> Rama depot/topology
  -> ActionDecision
  -> KernelEvent? if accepted
  -> PState materialization
  -> Projection
```

Vocabulary mapping:

```text
ActionRequest
  The user/agent asks to ingest a source, edit a unit, revise a container,
  create an edge, update view state, or accept a candidate.

ActionDecision
  Rama records whether the request was accepted or rejected, with reason,
  request id, routing key, and event id if accepted.

KernelEvent
  The accepted world fact: source captured, container created, revision
  created, derived unit graduated, composition edge created, view state updated,
  relation accepted.

Target
  SourceArtifact, ObjectContainer, Revision, DerivedUnit, SourceAnchor,
  CompositionEdge, RelationEdge, ContextMembership, LayerOverlay, ViewState,
  projection item.

Payload
  The carrier-specific parameters: raw source hash/ref, markdown distiller id,
  span offsets, text content, edge type, order, placement id, etc.

Distillation
  Versioned derivation from immutable source into DerivedUnits, relations,
  summaries, or candidate structures.

PState
  Materialized indexes and views over accepted events: source by hash/ref,
  containers by id, revisions by container, units by source/document, edges by
  parent/child/order, outline projection cache.

Projection
  Readable/renderable view over PStates. It emits ActionRequests; it does not
  mutate truth directly.
```

The object-container model belongs under the shared contracts in `core.clj` and
is closest to the existing `space.clj` object/edge direction. The current
`text_kernel.clj` is a useful proof and refactor target, not the final ontology.

## Two Floors Of Base

The phrase "base layer" now needs two floors:

```text
raw base
  SourceArtifact
  immutable captured source version
  content-addressed by hash, with logical source ref

model base
  accepted ObjectContainers, Revisions, SourceAnchors, CompositionEdges,
  ContextMemberships, policies, and decisions
```

When a user imports a markdown file, the file is not "the container." The import
creates raw base first:

```text
SourceArtifact(path, hash, raw bytes/text)
```

Then it usually seeds model base:

```text
document ObjectContainer
SourceAnchor back to the SourceArtifact
DerivedUnits for parser boundaries
CompositionEdges for the readable outline
```

Child containers are created immediately only when the source itself already
carries stable child identity, such as Roam block uids, canvas node ids, or chat
message ids. Markdown headings and paragraphs do not carry source-native
identity; they are distiller-produced DerivedUnits until touched or explicitly
promoted by import policy.

## Projection Vs Situate

This distinction is central to the outliner/canvas confusion.

```text
Projection
  makes existing facts legible in a medium
  examples: outline tree, canvas layout, chat thread, agent context bundle
  may create presentation state
  must not invent accepted semantic relationships

Situate
  discovers or proposes relationships and consequences
  examples: this block supports that claim, this edit affects a parent,
  this local artifact connects to global material
  may create candidate relations/context bundles
  must not pretend those candidates are accepted truth
```

Outliner and canvas are interpreters over the same substrate. They differ
because the interpreter makes different relations visually native:

```text
outline
  parent/child/order is native and readable

canvas
  placement, grouping, proximity, and selected relation edges are native and
  readable
```

Naively rendering every outline parent/child edge as a canvas arrow preserves
data but destroys legibility. That is a projection failure, not a storage
failure.

## The Three Loops

Think about the object-container substrate through three loops.

### 1. Ingest Loop

```text
external source
  -> ActionRequest :source/ingest
  -> SourceArtifact captured
  -> document container seeded when addressable
  -> distiller emits DerivedUnits + SourceAnchors + composition candidates/facts
  -> outline projection reads model
```

Reviewer rule:

```text
preserve source first; model later; inferred semantics last
```

### 2. Graduation Loop

```text
DerivedUnit selected/edited/replied-to/quoted
  -> ActionRequest
  -> Rama decides
  -> ObjectContainer created once
  -> Revision created
  -> SourceAnchor retained
  -> outline now resolves node content from the durable container
```

Reviewer rule:

```text
a derived unit may graduate at most once; retries/concurrent edits must not
create duplicate containers
```

### 3. Interpretation Loop

```text
accepted containers/edges/context
  -> interpreter chooses a readable form
  -> projection item carries target refs
  -> user gesture becomes ActionRequest
  -> accepted facts re-materialize
  -> all affected projections update
```

Reviewer rule:

```text
the view can be creative about readability, but not authoritative about truth
```

## What Needs To Be Done

There are two levels of "ready."

### Ready As Architecture

The following is settled enough for future sessions to use:

```text
ObjectContainer is the durable identity atom.
SourceArtifact is immutable source.
Revision is versioned authored content.
DerivedUnit is distiller output until touched.
CompositionEdge gives structure.
Projection renders.
Situate proposes relationship/context.
ActionRequest/Decision/Event remains the lifecycle.
```

### Ready For Slice 1 Implementation

The architecture is ready if implementation is scoped to the first spine:

```text
one source format
one distiller
one outline interpreter
one graduation path
one composition edge family
no canvas
no semantic situate
no global search
no full layer promotion system
```

Before writing code, the slice still needs an explicit implementation contract:

```text
1. exact ActionRequest types and payloads
2. exact accepted/rejected ActionDecision fields
3. exact KernelEvent types or event payload variants
4. PState keys and query shapes
5. idempotency keys for ingest, edit, and graduation
6. routing key choices
7. derived-unit id/source-anchor format
8. distiller versioning and rerun behavior
9. projection item target refs
10. tests that prove the invariants
```

`docs/current-mental-model/build/object-container/IMPLICIT_SPEC.md` is the
right place to continue that implementation contract. Treat it as the first
slice contract, not as global context.

## Implementation Gate

A first implementation is acceptable only if these are true:

```text
1. Re-ingesting the same source ref+hash is idempotent.
2. Editing a derived unit never mutates the SourceArtifact.
3. Editing a derived unit creates exactly one durable ObjectContainer.
4. Editing an already-graduated unit revises the same container.
5. The SourceAnchor survives graduation.
6. Outline reads show current revision content after graduation.
7. Composition/order survives graduation.
8. A distiller rerun cannot overwrite graduated authored content.
9. Projection gestures emit ActionRequests rather than mutating PStates directly.
10. Rejected requests are durable ActionDecisions, not silent failures.
```

If these are not true, the implementation may look like the spec but it has
missed the object-container model.

## Deferred Question

The one meaningful architecture question left is promotion granularity:

```text
per-fact promotion
  layers are tags/worksets over individual proposed facts

all-or-nothing promotion
  layers are atomic snapshots
```

Reviewer recommendation:

```text
choose per-fact promotion when the base/active feature is actually implemented
```

Reason:

```text
preserved plurality, late-bound synthesis, and selective consensus all want
individual facts to be promotable without forcing the entire active layer to
settle at once.
```

This does not block Slice 1 because Slice 1 can ignore full layer promotion and
only implement source -> derived -> graduated durable object.

## Red Flags

Push back if an implementation does any of this:

```text
1. treats markdown paragraphs as durable child containers just because the parser
   found boundaries;
2. lets SourceArtifact be edited as the Softland object;
3. stores parent/children directly on ObjectContainer;
4. keys canvas position by object id instead of placement;
5. accepts inferred semantic relations during ingest;
6. makes outline or canvas the canonical store;
7. creates KernelEvents outside Rama for user-world writes;
8. drops rejected decisions;
9. builds canvas/situate/layers before proving graduation;
10. renames textArtifact but keeps the old collapsed shape.
```

## New Session Compression

If a future session asks "what are we building and why?", the short answer is:

```text
We are extracting the general object substrate from the text proof.

The substrate preserves raw source, gives Softland its own durable addressable
containers, lets parsed structure remain derived until a user makes it matter,
and allows outline/canvas/chat/agent views to become native projections over one
canonical object graph.

The reason is not storage elegance. The reason is that Softland needs one world
where objects can be born in one affordance, interpreted in another, situated in
many contexts, and promoted from active exploration into accepted public form
without losing source, provenance, or disagreement.
```
