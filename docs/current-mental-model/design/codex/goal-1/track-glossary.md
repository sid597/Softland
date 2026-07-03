# Softland View Track Glossary

Status: Goal 1 glossary, initial version, 2026-06-09.

## Origin Prompt

```text
Use this for the charter. We are starting the Softland design/view research
track. Execute Goal 1 only. Create the required files inside
docs/current-mental-model/design/codex/goal-1/. The output should make the
design track clear enough that a future session knows what Softland views are,
what they are not, what laws govern them, and why the first view should
probably prove source-to-native trust before attempting a global map.
```

## Goal

Define the core vocabulary for the Softland design/view research track so
future sessions do not blur model, source, projection, truth, and view terms.

## Inputs Read

```text
docs/current-mental-model/design/codex/orientation.md
docs/current-mental-model/design/codex/goal-1/description.md
docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md
docs/vision/epistemic-framework.md
docs/current-mental-model/architecture/object-container-spec.md
```

## Scope

This glossary defines terms for Goal 1 only.

It is not a schema reference, implementation spec, UI copy deck, or complete
ontology. It should be strict enough to stop future design sessions from
turning Softland into a pretty graph without provenance or uncertainty.

## Core Claim

The design track must use terms that keep source, native object, projection,
relation, truth state, and local world separate until the product has earned a
lawful synthesis.

## Definitions

### view

A controlled coupling between a person and a larger structured reality.

In implementation terms, a view is a projection surface plus interaction
protocol over the canonical model. It helps a finite mind enter, inspect,
navigate, and act within a local world.

A Softland view is not canonical storage, not a copy of the model, not a
decorative layout, and not proof that the displayed arrangement is true.

### projection

A rendering of existing model facts into a usable form.

Projection may invent presentation:

```text
layout
ordering
folding
zoom level
visual grouping
screen placement
```

Projection must not invent accepted relationships. If a process computes new
relationships, it is situating or inference, and the results are candidates
until accepted.

### source

The outside material or captured origin from which Softland material is derived.

Examples:

```text
markdown file
chat transcript
Roam export
Linear issue
code file
PDF
image
runtime event stream
```

Source is not the same as native Softland terrain. The design track must keep
the raw source and the native result comparable.

### SourceArtifact

An immutable captured source version.

A SourceArtifact keeps raw material as-is and carries source identity such as:

```text
source ref
source hash
format
captured content
```

A SourceArtifact never becomes an ObjectContainer. It seeds native material
through SourceAnchors, DerivedUnits, and import policy.

### ObjectContainer

The thin, durable, identity-bearing atom of Softland.

An ObjectContainer carries identity plus minimal intrinsic facts such as origin,
ownership, content handle, policy, visibility, and per-kind capabilities. Text,
chat turns, canvas regions, code spans, slices, and future object kinds can all
be carried by the same container contract.

Documents, outlines, canvases, chats, and trails are compositions of containers,
not separate canonical container types.

### DerivedUnit

A re-runnable interpretation of source produced by a distiller.

A DerivedUnit is not yet durable native identity. It can be rebuilt when the
distiller reruns. When a user meaningfully touches it, edits it, quotes it,
selects it, replies to it, or policy explicitly promotes it, it can graduate
into an ObjectContainer while keeping its SourceAnchor.

Derived does not mean accepted. Derived does not mean durable.

### SourceAnchor

A link between source material and native or derived material.

A SourceAnchor may point to:

```text
source span
source block id
source item id
source hash region
line range
message id
```

The SourceAnchor is the trust bridge. A view should make anchor presence,
absence, staleness, and failure visible.

### CompositionEdge

A first-class structural edge that says how containers compose.

Examples:

```text
parent
child
order
contains
next sibling
section membership
```

CompositionEdge is not a hidden field on the object. It lives outside the
container so the same object can appear in many contexts and so overlays can
diff structure without forking object identity.

### RelationEdge

A first-class semantic or dependency edge between objects, units, sources, or
other meaningful entities.

Examples:

```text
supports
contradicts
references
derives from
depends on
answers
refines
duplicates
```

A RelationEdge has a truth state. It may be accepted, candidate, inferred,
stale, disputed, or otherwise calibrated. A line in a graph is not automatically
a RelationEdge.

### local world

The smallest identity-bearing packet of understanding that can be inhabited,
revisited, handed off, zoomed, and transformed.

A local world is a scoped region of:

```text
space
time
modality
plurality
material
process
artifact
provenance
action
```

The world is entered locally. A global map should be a later compression of
many trustworthy local worlds, not the first entry point.

### truth state

The visible epistemic status of something shown in a view.

Truth state answers:

```text
What is this allowed to mean?
How grounded is it?
What can I trust?
What should I inspect before acting?
```

Common states include:

```text
raw
derived
graduated
accepted
candidate
inferred
partial
stale
failed
unanchored
disconnected
disputed
```

Truth state must be encoded redundantly and not by color alone.

### candidate

A proposed object, edge, grouping, source interpretation, claim, or placement
that has not been accepted into stable model truth.

Candidates are useful. They let Softland preserve possible structure without
pretending synthesis has happened. A good view lets candidates be inspected,
compared, accepted, rejected, or left unresolved.

Candidate must never visually collapse into accepted.

### accepted

Admitted into the stable model by user action, explicit policy, or another
valid acceptance process.

Accepted does not mean eternal or unquestionable. It means the system is
allowed to treat the object, edge, or state as part of the current native
terrain while preserving provenance, revision, and possible later change.

### semantic zoom

Lawful compression across scale.

Semantic zoom changes representation, density, and pacing while preserving:

```text
identity
provenance
relation
truth state
local context
recoverable path to detail
```

If zoom only makes things bigger or smaller, it is not semantic zoom. If zoom
loses source or identity, it is fake semantic zoom.

### ZUI

Zoomable user interface.

In Softland, ZUI is not an infinite canvas aesthetic. It is a navigation and
pacing model for moving through scale while preserving epistemic invariants.

A Softland ZUI must answer:

```text
What remains the same across scale?
What becomes compressed?
How do I recover what is folded?
What truth state survives the zoom?
```

### inhabitable

Able to be entered by another mind without requiring private narration from the
person who produced it.

An inhabitable view gives enough public form to orient, inspect grounds,
understand local scope, see tensions, and take a next safe action.

Inhabitable does not mean maximal display. It means important structure is
explicit and compressed structure is recoverable.

### recoverable compression

Folding without severing.

Recoverable compression lets Softland reduce visible complexity while keeping:

```text
source path
object identity
relation counts or status
truth state
warning status
local context
reveal action
```

Collapsed detail is allowed. Lost provenance is not.

## Core Distinctions

Keep these pairs separate:

```text
view != model
projection != situating
source != native object
SourceArtifact != ObjectContainer
DerivedUnit != ObjectContainer
SourceAnchor != RelationEdge
CompositionEdge != RelationEdge
candidate != accepted
semantic zoom != visual scaling
inhabitable != fully expanded
recoverable compression != hiding
global map != trustworthy world
```

## Design Decisions

```text
Views are projections over one model, not copies.
Source-to-native trust is the first proof.
Truth state is a design primitive.
Local worlds are the entry unit.
Global maps come after local trust.
Edges are inspectable terrain.
Actions must preserve the projection/model boundary.
```

## Open Questions

```text
Which truth states are required in the first prototype and which can wait?
What exact visual grammar should encode source, native, anchor, candidate, and
accepted?
How much source context must be visible by default before detail is folded?
What is the smallest bidirectional source/native gesture that proves trust?
How does a local world expose enough for handoff without becoming maximal
display?
```

## Next Recommended Goal

Goal 2: map what the current object-container kernel and ingesters can expose
to the view layer.

Use this glossary as the vocabulary boundary for that substrate map.
