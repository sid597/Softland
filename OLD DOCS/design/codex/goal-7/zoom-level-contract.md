# Zoom Level Contract

Status: drafted, 2026-06-09.

## Origin Prompt

```text
Treat ZUI as lawful compression across scales of understanding, not as zoom
animation.

I want to know what changes at each zoom level, what survives every scale
change, how identity/provenance/relation/truth-state are preserved, how the user
knows where they are, and what small prototype slices can test this without
building the whole global map.
```

## Goal

Define the invariants that every Softland zoom level and zoom transition must
obey.

## Inputs Read

- `docs/current-mental-model/design/codex/orientation.md`
- `docs/current-mental-model/design/codex/goal-7/description.md`
- `docs/vision/epistemic-framework.md`
- `docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md`
- `docs/current-mental-model/architecture/object-container-spec.md`
- `docs/current-mental-model/architecture/object-container-ingester-contract.md`
- `docs/current-mental-model/build/knowledge-earth-zui/CATEGORY_THEORY_DESIGN_RESEARCH.md`
- External sources listed in `zui-research.md`.

## Scope

This is a product/model contract for semantic zoom. It is not a visual spec or
implementation plan.

## Core Claim

Zoom is allowed to compress, aggregate, fold, filter, and change representation.
Zoom is not allowed to lie.

```text
lawful compression = every folded thing remains recoverable enough for trust,
orientation, and action
```

## Findings / Design Decisions

1. Identity, source anchors, truth state, relation state, way back, and action
   legitimacy are hard invariants.
2. Compression is allowed only when its policy is nameable and recoverable.
3. Accepted/candidate distinction is a zoom-level contract, not a local edge
   detail.
4. Projection can invent presentation, but only model events/decisions can assert
   accepted relation truth.
5. Every prototype should be tested by following one object, one source span, one
   relation, one warning, and one trail across scales.

## Contract Summary

Every zoom level must preserve:

```text
identity
source/provenance recoverability
truth state
relation state
local orientation
way back
action legitimacy
compression policy
```

## Invariant 1 - Identity Survives Zoom

Rule:

```text
The same thing must remain the same thing across projections and scales.
```

Softland meaning:

- An ObjectContainer stays itself across outline, graph, inspector, timeline,
  source, substrate, and code-adjacent views.
- A DerivedUnit remains visibly derived until it graduates.
- A source span does not become a container; it anchors one.
- A region/aggregate is not a hidden copy of its member objects.
- A placement is not an object.

Required preserved fields or equivalents:

```text
object/container id
derived-unit id
source-artifact id
source-anchor id
relation/edge id
local-world id
view-instance + placement id when view-specific
projection id
```

What may change:

- Label density.
- Geometry.
- Visual representation.
- Grouping.
- Available gestures.
- Whether details are expanded.

What must not change:

- Canonical object identity.
- Whether the object is source, derived, graduated, accepted, candidate, stale,
  failed, inferred, or unanchored.

Failure mode:

```text
zooming from topic to source makes the user feel they have opened a different
object instead of revealing the object's source grounding
```

Acceptance check:

```text
Select an object at one scale, move across at least three scales, and verify the
selection identity can still be named and returned to.
```

## Invariant 2 - Source Anchors Remain Recoverable

Rule:

```text
Exact source may be folded; source anchoring status may not disappear.
```

Softland meaning:

- Overview scales may show coverage summaries instead of spans.
- Local scales should reveal exact source spans cheaply.
- Substrate scale should reveal source ref/hash/offset/native-id/distiller.
- Code scale should reveal implementation source, not only imported material.

Required states:

```text
source-backed
source-only
partially anchored
stale anchor
unanchored
parse failure
redacted
source unavailable
derived by distiller version
```

What may change:

- Raw text/image/code body can fold.
- Full source inventory can summarize.
- Exact offsets can hide until detail.

What must not change:

- Whether an anchor exists.
- Whether the anchor is current.
- Whether a native object was inferred from source or supplied by a native id.
- Whether an aggregate contains unanchored or stale material.

Failure mode:

```text
a polished domain map hides that half the region is source-unanchored inference
```

Acceptance check:

```text
At landscape/domain scale, choose any aggregate and reveal anchor coverage. At
local/ground scale, choose any object and reveal exact source.
```

## Invariant 3 - Truth State Survives Zoom

Rule:

```text
Zoom changes dosage, not truth.
```

Minimum truth-state distinctions:

```text
raw source
source artifact
native object
derived unit
graduated object
human-authored fact
AI inference
hypothesis
simulation output
accepted relation
candidate relation
inferred relation
rejected relation
partial parse
parse failure
stale anchor
unanchored object
disconnected island
projection-derived proximity
```

What may change:

- States can summarize into counts, badges, bands, or warnings.
- Low-severity details can fold.
- Uncertainty explanations can move to detail surfaces.

What must not change:

- Candidate cannot become accepted through aggregation.
- Inferred cannot look human-authored.
- Projection-derived proximity cannot look like semantic relation.
- Partial/failure/stale states cannot disappear at overview.

Failure mode:

```text
a generated cluster label becomes indistinguishable from a settled domain
category
```

Acceptance check:

```text
Create a mixed local world with accepted, candidate, inferred, and stale items.
Zoom out. Verify the mixed state remains visible and recoverable.
```

## Invariant 4 - Accepted/Candidate Distinction Survives Zoom

Rule:

```text
Accepted and candidate structure must remain impossible to confuse.
```

Softland meaning:

- Ingest may create candidate structure.
- Projection may layout or summarize structure.
- Situate may discover candidate relations.
- Only accepted events/decisions produce accepted relation truth.

At overview:

```text
show counts/ratios or distinct encodings
```

At local world:

```text
show edge state directly
```

At ground:

```text
show evidence and accept/reject action path
```

At substrate:

```text
show decision/event provenance
```

What may change:

- Candidate details can fold into candidate-count or candidate-band.
- Rejected relations can hide by default if their existence is summarized and
  recoverable.

What must not change:

- Candidate relation must never share the same primary encoding as accepted
  relation.
- Aggregating many candidate relations must not produce an accepted bridge.

Failure mode:

```text
domain overview shows a thick connection between two regions because many
candidate edges exist, but the connection reads as accepted synthesis
```

Acceptance check:

```text
At every scale where a relation appears, its state can be identified without
opening a raw metadata panel.
```

## Invariant 5 - The User Always Has A Way Back

Rule:

```text
Navigation creates an epistemic trail, not a disposable screen history.
```

A trail step records:

```text
scale
projection
local world
focus identity
relation followed or action taken
filters/scope
truth/provenance summary
entry source if applicable
```

The user can return to:

- Previous scale.
- Previous focus.
- Entry object.
- Entry source.
- Containing local world.
- Pre-action state.
- Route overview.

What may change:

- Visual representation of the trail.
- Whether long trails fold.
- Whether trail groups are summarized.

What must not change:

- Back cannot mean only "previous pixels."
- Back must restore epistemic context, not just viewport position.
- Relation-following must not strand the user in a different region.

Failure mode:

```text
user follows a relation from a local object into source/code and cannot tell how
to return to the original question
```

Acceptance check:

```text
Follow object -> relation -> source span -> substrate mapping -> code line.
Return to the original object/local world with focus and filters intact.
```

## Invariant 6 - Zoom Changes Dosage, Not Truth

Rule:

```text
Semantic zoom is pacing. It controls amount, sequence, and recoverability.
```

At each level, dosage includes:

- Number of entities visible.
- Relation depth.
- Source detail.
- Status detail.
- Available actions.
- Warning prominence.
- Whether the user sees content, structure, machinery, or code.

What may change:

- Detail density.
- Action density.
- Label density.
- Relation depth.
- Exact evidence visibility.

What must not change:

- Status semantics.
- Identity.
- Provenance path.
- Relation state.
- Model ownership.

Failure mode:

```text
zooming out reduces warning visibility until uncertainty feels resolved
```

Acceptance check:

```text
A partial parse warning at object scale remains visible as aggregate warning at
domain scale, then reveals exact failed span on descent.
```

## Invariant 7 - Compression Has A Named Policy

Rule:

```text
Every aggregate or folded representation must have a policy explaining what it
preserves and what it forgets.
```

Policy examples:

```text
count objects by accepted relation type
cluster by source-declared hierarchy
cluster by human-accepted domain membership
cluster by embedding similarity
summarize anchor coverage by source artifact
summarize truth-state by worst visible state
hide rejected edges but expose rejected count
```

What may change:

- Compression policy can be implicit in early prototypes if it is named in the
  artifact and inspectable by the designer/tester.

What must not change:

- Projection-derived grouping cannot be presented as accepted ontology.
- Similarity cannot become relation.
- Count cannot become evidence.

Failure mode:

```text
two topics are near each other because of embedding projection, but the user
reads that as a model relation
```

Acceptance check:

```text
For every aggregate in a prototype, ask: "why are these things together?" The
answer must identify source-declared, accepted, candidate, inferred, or
projection-derived grouping.
```

## Invariant 8 - Valid Actions Change By Scale

Rule:

```text
Zoom level changes what actions are safe and meaningful.
```

Examples:

```text
0.001  choose region, filter by coverage, follow route
0.01   enter domain, compare programs, inspect source coverage
0.1    inspect local topology, follow relation, reveal evidence
1.0    edit, test, ask, run, accept/reject candidate relation
10.0   inspect mapping, rerun distiller, compare drift, inspect projection
100.0  edit code, run tests, inspect implementation ownership
```

What must not happen:

- Landscape gesture silently changes canonical object truth.
- Projection drag creates accepted relationship.
- View-specific placement mutates object identity.
- Code edit is disconnected from higher-level contract impact.

Failure mode:

```text
user drags two nodes together at local topology scale and the system creates an
accepted semantic relation instead of a candidate/action request
```

Acceptance check:

```text
For each gesture in a prototype, label whether it changes ViewState, emits an
ActionRequest, reveals existing model truth, or changes code.
```

## Invariant 9 - Projection Must Not Invent Accepted Relationships

Rule:

```text
Projection can invent presentation. Situate can propose relationships. Neither
projection nor zoom can assert accepted relation truth.
```

Softland meaning:

- Layout proximity is presentation.
- Edge bundling is presentation.
- Clustering is presentation unless backed by accepted membership.
- Suggested edge is candidate.
- Accepted edge requires model event/decision.

Failure mode:

```text
ZUI layout produces a bridge, user assumes the bridge is accepted knowledge
```

Acceptance check:

```text
Inspect a visually close pair. The view must state whether closeness comes from
accepted relation, candidate relation, source grouping, embedding projection, or
layout only.
```

## Invariant 10 - One World, Many Projections

Rule:

```text
Outline, graph, source, timeline, substrate, code, and future canvas are
projections over the world, not separate worlds.
```

Softland meaning:

- A selected object remains selected across projections.
- Each projection can fold different details.
- Projection-specific state lives as ViewState keyed by placement.
- Model truth lives in common substrate rows/events.

Failure mode:

```text
same imported transcript message appears as three unrelated objects in outline,
topology, and source views
```

Acceptance check:

```text
Switch projections at the same scale and verify object identity, truth state,
source anchor status, and relation state persist.
```

## What This Makes Visible

- The rules governing scale changes.
- Which facts may fold and which cannot.
- What counts as a lie in Softland ZUI.
- How to evaluate early prototypes before building a global map.

## What This Keeps Folded But Recoverable

- Exact visual encoding choices.
- Rendering architecture.
- Animation policy.
- Full truth-state grammar.
- Full route/trail UI.

## What This Must Not Imply

- That all invariants are already implemented.
- That the UI must show every state at full detail all the time.
- That compression is bad.
- That global Knowledge Earth should be built first.

## Failure Modes

- Compressing by hiding warnings.
- Confusing view state with model truth.
- Treating source-specific read models as canonical object identity.
- Treating generated summaries as accepted knowledge.
- Letting a zoom transition change focus without preserving trail.

## Acceptance Checks

For any proposed ZUI prototype:

1. Pick one object and follow it across at least three scales.
2. Pick one source span and follow it to native object and back.
3. Pick one accepted relation and one candidate relation; zoom out and verify
   they remain distinguishable.
4. Pick one partial/stale/failure state; zoom out and verify it remains visible
   as aggregate status.
5. Pick one aggregate; inspect its compression policy.
6. Follow a route to code/bedrock and return to the original local-world focus.
7. Trigger one action and identify whether it changed ViewState, emitted an
   ActionRequest, or changed canonical model/code.

## Open Questions

1. Should compression policy be user-facing language or developer/tester-only
   metadata in the first slice?
2. What is the minimal aggregate truth-state rule: worst-state wins, counts,
   stacked bands, or state priority?
3. How should zoom handle plural/conflicting worldviews at overview scales?
4. How much of the way-back trail should be visible versus folded?
5. What is the smallest code-symbol identity scheme for zoom 100?

## Next Recommended Goal

Run Goal 4 next. This contract says truth-state must survive zoom; Goal 4 should
decide the minimum visual/interaction grammar that makes that survival
legible.
