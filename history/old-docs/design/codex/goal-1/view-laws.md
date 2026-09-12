# Softland View Laws

Status: Goal 1 law set, initial version, 2026-06-09.

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

Define the laws that govern Softland views before prototype, survey, or UI-spec
work begins.

## Inputs Read

```text
docs/current-mental-model/design/codex/orientation.md
docs/current-mental-model/design/codex/goal-1/description.md
docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md
docs/vision/epistemic-framework.md
docs/current-mental-model/architecture/object-container-spec.md
```

## Scope

These laws govern the design track at the level of invariants.

They do not choose a layout, component library, color palette, interaction
model, or implementation architecture. They say what any future view must
conserve to count as a Softland view.

## Core Claim

A Softland view is valid only if it helps a finite mind safely enter transformed
knowledge without severing source, identity, relation, truth state, pacing, or
the ability to act.

## Design Decisions

```text
Source-to-native trust is the first proof.
Truth state is a design primitive, not decoration.
Local entry precedes global topology.
Projection and situating must stay separate.
Accepted and candidate structure must never visually collapse.
Recoverable compression is required for pacing.
View gestures must preserve the ActionRequest versus ViewState boundary.
```

## What These Laws Make Visible

```text
source-to-native transformation
object identity across projections
anchor status
accepted versus candidate structure
composition versus semantic relation
local scope
truth state
action boundary
view-state boundary
drift and failure
```

## What These Laws Keep Folded But Recoverable

```text
full source bodies
full import logs
large graph neighborhoods
revision history
candidate derivation details
alternate projections
inactive local worlds
```

Folding is allowed only when the folded thing remains reachable and its status
remains legible.

## What These Laws Must Not Imply

```text
all model state must be visible at once
a graph is the natural first view
clean presentation means truth
source provenance is secondary metadata
candidate relations are harmless if they look subtle
view-local manipulation may silently mutate canonical structure
```

## Law 1: Transformation Must Be Inspectable

Statement:

```text
If Softland changes representation, the user must be able to inspect the
mapping from source to native form.
```

Why it matters:

```text
The first trust question is whether imported material preserved understanding
or destroyed context while looking organized.
```

What violates it:

```text
Showing only a clean native object, summary, graph node, or outline row without
an exact path back to the raw source and transformation grounds.
```

Prototype test:

```text
Select one native object. The prototype must reveal its source artifact, exact
source anchor, native representation, and transformation status in one local
inspection flow.
```

## Law 2: Identity Must Survive Projection

Statement:

```text
The same object must remain recognizably the same object across outline, graph,
timeline, inspector, canvas, code, and agent-context projections.
```

Why it matters:

```text
Softland is one substrate with many projections. If view switching feels like
copying, the user loses the world.
```

What violates it:

```text
Different labels, unstable selection, duplicate-looking cards, projection-local
ids, or view-specific objects that hide the shared ObjectContainer identity.
```

Prototype test:

```text
Select an object in one projection, then show it in another projection with
stable identity affordance, breadcrumb, truth state, and source anchor access.
```

## Law 3: Source Must Remain Raw And Reachable

Statement:

```text
A SourceArtifact is immutable captured source. It never becomes a native object,
and a view must preserve the distinction.
```

Why it matters:

```text
Softland trust depends on being able to compare what came in with what became
native.
```

What violates it:

```text
Treating an imported markdown file, transcript, PDF, code file, or Roam export
as if it were already equivalent to the native ObjectContainers seeded from it.
```

Prototype test:

```text
Show one source artifact and its native descendants while keeping raw source,
source ref, source hash, anchors, and native objects visually distinct.
```

## Law 4: Projection Must Not Invent Accepted Relationships

Statement:

```text
Projection renders existing data into a form. It may invent presentation, but
it must not invent accepted relationships.
```

Why it matters:

```text
Layout can accidentally become epistemic claim. Softland must not let a pretty
arrangement assert false structure.
```

What violates it:

```text
Using visual proximity, grouping, clustering, force-directed layout, or pane
placement as if it were an accepted RelationEdge or CompositionEdge.
```

Prototype test:

```text
Display a local neighborhood where layout choices are visibly separate from
accepted edges and candidate edges.
```

## Law 5: Inferred Structure Is Candidate Until Accepted

Statement:

```text
Situating may propose relationships, groupings, claims, or slices, but inferred
structure remains candidate until accepted by policy or action.
```

Why it matters:

```text
The map must not lie. AI inference, parser inference, and user exploration can
help, but they cannot silently become truth.
```

What violates it:

```text
Importing inferred semantic links as accepted relations, hiding candidate
status, or making suggestions visually indistinguishable from stable structure.
```

Prototype test:

```text
Show accepted and candidate relations for one object with different labels,
strokes, interaction affordances, and acceptance actions.
```

## Law 6: Truth State Must Be First-Class And Redundant

Statement:

```text
Accepted, candidate, inferred, partial, stale, failed, disconnected, unanchored,
and raw states must be explicit and impossible to confuse.
```

Why it matters:

```text
Calibration prevents Softland from becoming a persuasive hallucination engine.
```

What violates it:

```text
Using color alone, hiding warnings in metadata, treating missing anchors as
normal, or rendering stale and current anchors with the same grammar.
```

Prototype test:

```text
In grayscale or low-color mode, a user must still distinguish accepted,
candidate, partial, stale, failed, disconnected, and unanchored states.
```

## Law 7: Compression Must Be Recoverable

Statement:

```text
Softland may fold detail, but it must never sever the path back to what was
folded.
```

Why it matters:

```text
A finite mind needs pacing. It does not need every detail at once, but it does
need confidence that hidden detail can be recovered.
```

What violates it:

```text
Collapsing source anchors, warnings, candidate status, relation counts, or
local context so thoroughly that the user cannot tell what is missing.
```

Prototype test:

```text
Fold a long source body and distant topology. The collapsed state must still
show anchor presence, warning count, truth state, and a stable reveal path.
```

## Law 8: The World Is Entered Locally

Statement:

```text
Softland should be entered through a source, object, relation, anchor, warning,
question, event, or local world, not through an undifferentiated map of
everything.
```

Why it matters:

```text
Understanding begins with orientation. A global map before local trust creates
scale without entry.
```

What violates it:

```text
Making a global graph the first proof, omitting local focus, hiding current
scope, or removing the way back after navigation.
```

Prototype test:

```text
Start from one imported object. The user must know current source, current
native object, nearby topology, scope boundary, and back path.
```

## Law 9: Pacing Is Epistemic Machinery

Statement:

```text
The view must control dosage: how much structure arrives, when detail unfolds,
and how safely the user can recover context.
```

Why it matters:

```text
The right projection at the wrong dose still overwhelms. Semantic zoom is a
knowledge pacing mechanism, not decoration.
```

What violates it:

```text
Dumping all edges, all sources, all transcripts, all revisions, or all warnings
at once without staged reveal, scope, or local priority.
```

Prototype test:

```text
Show the same imported material at source inventory, selected object, local
neighborhood, and source-span levels while preserving identity and status.
```

## Law 10: Semantic Zoom Must Preserve Identity, Provenance, And Relation

Statement:

```text
Zoom may change representation and density, but object identity, provenance,
and relation must survive across scale.
```

Why it matters:

```text
Softland's zoom is lawful compression. If zoom loses the thing, its source, or
its relations, it is just visual scaling.
```

What violates it:

```text
Turning objects into unlabeled blobs, hiding anchor status at overview scale,
or changing edge meaning across zoom levels without explanation.
```

Prototype test:

```text
Zoom from local object inspection to neighborhood overview and back. The same
object, source anchor, and relation status must remain recoverable.
```

## Law 11: View Gestures Emit ActionRequests

Statement:

```text
Gestures in a view request model changes; they do not directly mutate canonical
truth.
```

Why it matters:

```text
Softland views are instruments over the substrate. Structural edits must pass
through explicit model semantics so every projection can rederive consistently.
```

What violates it:

```text
Dragging, editing, grouping, accepting, or linking inside a view by changing
local projection data while bypassing canonical events.
```

Prototype test:

```text
For each gesture, label whether it emits an ActionRequest or updates ViewState.
Structural changes must reappear in other projections after rederivation.
```

## Law 12: ViewState Belongs To Placement

Statement:

```text
View-local state such as xy, collapse, zoom, and selection belongs to a
placement or view instance, not globally to the object.
```

Why it matters:

```text
The same object can appear in multiple local worlds and boards. Placement state
must not overwrite object identity or imply canonical structure.
```

What violates it:

```text
Storing canvas position, collapse, or local focus as if it were intrinsic to the
ObjectContainer.
```

Prototype test:

```text
Place one object in two local contexts. Move or collapse it in one context.
The other context must preserve its own placement while sharing object identity.
```

## Law 13: Edges Are Terrain

Statement:

```text
CompositionEdge and RelationEdge are first-class things a user can inspect,
not invisible fields behind objects.
```

Why it matters:

```text
In Softland, the connection itself is part of the place. Users must inspect how
structure is grounded and whether it is accepted or candidate.
```

What violates it:

```text
Hiding parent/child/order/supports/contradicts/depends-on/derives-from as
uninspectable metadata or treating all edges as the same generic line.
```

Prototype test:

```text
Select an edge. The prototype must show edge type, endpoints, truth state,
source or derivation grounds, and available actions.
```

## Law 14: Drift And Failure Must Increase Trust

Statement:

```text
Partial parse, stale anchor, failed import, disconnected island, and source
drift are not embarrassment states. They are trust states.
```

Why it matters:

```text
Showing limits honestly makes the system safer to inhabit. Hiding them makes a
clean UI dangerous.
```

What violates it:

```text
Suppressing failures, treating stale anchors as current, burying import
warnings, or rendering disconnected material as if it were integrated.
```

Prototype test:

```text
Create a partial import or stale source anchor scenario. The view must show what
worked, what failed, what remains reachable, and what action can resolve it.
```

## Law 15: Handoff Requires Public Form, Not Maximal Display

Statement:

```text
A view succeeds when another mind can enter the local world without privately
reconstructing the important structure.
```

Why it matters:

```text
Softland's design center is a world for holding understanding in public form.
Public form means important structure is explicit and the rest is compressed
but recoverable.
```

What violates it:

```text
Showing everything, hiding too much, relying on the author's memory, or
presenting output without material, process, artifact, provenance, tensions,
and transformation.
```

Prototype test:

```text
Give the view to another person without narration. They must identify current
source, native object, trust state, key relations, unresolved tensions, and the
next safe action.
```

## Failure Modes

```text
global graph before local trust
source hidden as secondary metadata
candidate and accepted relations visually collapsed
uncertainty encoded only by color
projection layout treated as model truth
derived units treated as durable objects
view gestures mutating projection state as canonical structure
collapsed detail with no recoverable path
semantic zoom that loses identity or source
handoff that requires private explanation
```

## Acceptance Checks

Before accepting any future view design, ask:

```text
What transformation does this make inspectable?
What identity survives across projections?
What source path can the user recover?
Which relations are accepted and which are candidate?
What uncertainty states are visible without color alone?
What is folded, and how is it recovered?
Where is the local entry point?
What actions target the model?
What state is only view-local?
Could another mind safely enter from this view?
```

## Open Questions

```text
What exact visual grammar should encode each truth state?
How many truth states should the first prototype expose without overwhelming
the user?
What is the minimum bidirectional source/native interaction that proves trust?
How should edge inspection work before a full topology view exists?
How should source drift be represented when a DerivedUnit has graduated?
```

## Next Recommended Goal

Goal 2: map what the current kernel and ingesters can expose to views.

That should happen before Goal 6 turns these laws into the first buildable
Source-to-World View spec.
