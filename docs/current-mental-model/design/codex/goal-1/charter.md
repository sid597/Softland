# Softland View Design Research Charter

Status: Goal 1 charter, initial version, 2026-06-09.

## Origin Prompt

```text
Use this for the charter.

We are starting the Softland design/view research track.

Read:
- docs/current-mental-model/design/codex/orientation.md
- docs/current-mental-model/design/codex/goal-1/description.md

Execute Goal 1 only.

Create the required files inside docs/current-mental-model/design/codex/goal-1/.

The output should make the design track clear enough that a future session
knows what Softland views are, what they are not, what laws govern them, and
why the first view should probably prove source-to-native trust before
attempting a global map.

Do not implement UI. Do not broaden into other goals. Do not read
src/app/server/env.clj. Do not commit docs.
```

## Goal

Create the charter for the Softland design/view research track.

This document should let a future session enter the track without confusing
Softland views with normal dashboards, generic graph views, decorative canvases,
or a premature full-world map.

## Inputs Read

```text
docs/current-mental-model/design/codex/orientation.md
docs/current-mental-model/design/codex/goal-1/description.md
docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md
docs/vision/epistemic-framework.md
docs/current-mental-model/architecture/object-container-spec.md
```

## Scope

In scope:

```text
track definition
view boundaries
first principles
conservation pressures
prototype direction
questions future sessions must keep separate
```

Out of scope:

```text
UI implementation
component design
visual styling
global map design
research survey
current substrate inventory
first buildable view spec
```

## Core Thesis

Softland views are epistemic instruments.

A view is not just a pane, page, graph, dashboard, canvas, inspector, or editor
mode. A Softland view is a controlled coupling between a finite mind and a
larger structured reality. It must let someone enter transformed material while
preserving enough origin, identity, relation, uncertainty, pacing, and action
surface to avoid false understanding.

The near-term design problem is therefore not:

```text
What should the app UI look like?
```

It is:

```text
What views let someone trust what outside material became inside Softland?
```

The first serious view should probably prove source-to-native trust before any
global map, because the global map is only useful if the local transformations
it summarizes are inspectable and truthful.

## What The Track Is

The design/view research track is the place where Softland turns its ontology
into inhabitable projection contracts.

It exists to define:

```text
what a view is allowed to show
what a view is forbidden to imply
what must remain recoverable when detail is folded
how source becomes native terrain without losing provenance
how truth, uncertainty, and relation become visible
how a person can act through a view without corrupting the model
```

The track joins the two Softland centers:

```text
design center:
  a world for holding understanding in public form

engineering center:
  a programmable epistemic interface
```

The design center asks whether the place can be inhabited by another mind.
The engineering center asks whether the instrument exposes lawful operations
against the substrate. A good view must satisfy both.

## What The Track Is Not

The track is not ordinary UI exploration.

It is not:

```text
a style guide
a dashboard exercise
a graph visualization contest
a canvas-first product direction
a file explorer redesign
an analytics surface
a metadata table
a marketing narrative
a component library plan
a substitute for the model contract
```

It should reject early designs that make Softland look organized while hiding
the transformation that produced the organization.

The dangerous failure mode is a beautiful view that cannot answer:

```text
Where did this object come from?
What exactly was preserved?
What was inferred?
What is accepted?
What is candidate?
What is stale, partial, disconnected, failed, or unanchored?
What can I safely do now?
```

## Primary Anxieties The UI Must Answer

The first product anxiety is:

```text
Did this transformation preserve understanding,
or did it destroy context while making the result look organized?
```

The view layer must repeatedly answer:

```text
what came in
what became native
where it came from
how it is grounded
what structure was preserved
what structure was inferred
what relation is accepted
what relation is only candidate
what is partial, stale, disconnected, failed, or unresolved
what action is available without overstating certainty
```

If a view cannot answer those questions for one object, it cannot honestly
answer them for a whole world.

## Conservation Pressures

Softland views must conserve these pressures.

```text
Identity:
  The same thing must remain the same thing across projections.

Provenance:
  The path back to source must remain visible or cheaply recoverable.

Relation:
  What belongs with what, depends on what, supports what, contradicts what,
  contains what, and derives from what must not be destroyed by presentation.

Truth state:
  Accepted, candidate, inferred, partial, stale, failed, disconnected, and
  unanchored states must not collapse into one visual treatment.

Manipulability:
  A view must expose lawful actions, not merely present static output.

Pacing:
  The view must dose complexity so a finite mind can enter locally and recover
  more detail when ready.

Plurality:
  Multiple interpretations, candidates, and local worlds must be preservable
  until synthesis is meaningful.
```

These pressures are stronger than layout preference. A layout that violates
them is not a valid Softland view, even if it looks polished.

## Core Design Decisions

1. A view is a projection and coupling, not canonical storage.
2. Projection may invent presentation, but must not invent accepted relations.
3. Situating may compute or propose relationships, but those relationships are
   candidates until accepted.
4. A SourceArtifact never becomes an ObjectContainer. It seeds native objects
   and anchors while remaining raw preserved source.
5. A DerivedUnit is not yet durable native identity. It may graduate when
   touched or accepted by policy.
6. CompositionEdge and RelationEdge are first-class terrain, not hidden fields.
7. View gestures emit ActionRequests against the model. The view then rederives.
8. ViewState belongs to a placement, not globally to the object.
9. Global topology should be composed from trustworthy local neighborhoods,
   not used as the first proof.
10. The map must not lie.

## First Prototype Family

The first prototype family should be:

```text
Scoped Source-to-World Explorer
```

Its first proof should be smaller than a full explorer:

```text
Trust Inspector for one imported object
```

The irreducible interaction:

```text
I select a native Softland object.
I see what it is.
I see where it came from.
I see the exact raw source span.
I see what transformation produced it.
I see its accepted relations.
I see candidate, partial, stale, disconnected, failed, or unanchored states.
I can move to nearby objects without losing my place.
```

Only after this works locally should the track attempt larger local topology
or a global map.

## Why Source-To-Native Comes Before Global Map

A global map is seductive because it visually promises a world.

But without source-to-native trust, it has no epistemic floor. It can show
relations before the user knows whether the objects are grounded, whether the
edges are accepted, whether candidates were promoted too early, whether anchors
are stale, or whether the import silently severed context.

Source-to-native trust is the smaller proof with the larger consequence:

```text
Can Softland transform outside material into native terrain without lying?
```

If yes, a global map can later be a lawful compression of many local worlds.
If no, a global map is only spectacle.

## What This Makes Visible

The first view family must make visible:

```text
raw source
native object
source anchor
derived unit versus graduated object
accepted edge versus candidate edge
partial import
stale anchor
unanchored object
disconnected island
local neighborhood
available action
```

## What This Keeps Folded But Recoverable

The view may fold:

```text
full raw body
distant neighborhoods
long revision history
secondary metadata
candidate details
alternate projections
inactive local worlds
```

The fold is valid only if the path back remains visible, cheap, and stable.

## What This Must Not Imply

A view must not imply:

```text
candidate means accepted
derived means durable
source means native object
layout means relation
visual proximity means semantic relationship
folded means absent
clean means true
global means understood
AI inference means grounded fact
projection means model mutation
```

## Failure Modes

Reject early designs if:

```text
the first screen is a global graph
source is hidden behind a modal as secondary metadata
candidate links look like accepted links
partial, stale, failed, disconnected, or unanchored states are absent
the inspector is only a property table
selection does not preserve identity across projections
the same object feels duplicated across views
the raw/native distinction is unclear
the view cannot show markdown and transcript material in the same substrate
the visual polish is stronger than the trust contract
```

## Acceptance Checks

A Goal 1 compatible first-view proposal should pass these checks:

```text
Can the user inspect the exact source behind a selected native object?
Can the user move from native object to raw span and back?
Can the user distinguish SourceArtifact, DerivedUnit, and ObjectContainer?
Can the user distinguish CompositionEdge from RelationEdge?
Can the user distinguish accepted, candidate, partial, stale, failed,
disconnected, and unanchored states without relying only on color?
Can the user see local scope before being shown large topology?
Can the user tell which actions mutate the model and which only change view
state?
Could another mind enter this view without private narration from the author?
```

## Questions Future Sessions Must Not Blur Together

Keep these distinctions sharp:

```text
view versus model
projection versus situating
source versus native object
SourceArtifact versus ObjectContainer
DerivedUnit versus graduated ObjectContainer
SourceAnchor versus RelationEdge
CompositionEdge versus RelationEdge
accepted versus candidate
truth state versus visual style
local neighborhood versus global map
recoverable compression versus hidden deletion
ActionRequest versus ViewState
layout proximity versus semantic relation
public form versus maximal display
```

## Open Questions

```text
What exact truth-state grammar should Softland use visually?
What does the current object-container substrate already expose to views?
Which interactions best prove bidirectional source/native trust?
How should drift be shown when source changes after native graduation?
How should multiple local worlds preserve disagreement without overwhelming
the first view?
```

## Next Recommended Goal

Goal 2: map what the current kernel and ingesters can expose to views.

Do not start with the global map. First verify the substrate available for a
Scoped Source-to-World Explorer: SourceArtifact, ObjectContainer, DerivedUnit,
SourceAnchor, CompositionEdge, RelationEdge, truth state, revision, drift, and
view/action boundaries.
