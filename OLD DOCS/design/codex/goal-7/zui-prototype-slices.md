# ZUI Prototype Slices

Status: drafted, 2026-06-09.

## Origin Prompt

```text
I want to know what changes at each zoom level, what survives every scale
change, how identity/provenance/relation/truth-state are preserved, how the user
knows where they are, and what small prototype slices can test this without
building the whole global map.
```

## Goal

Define small prototype slices that test Softland's ZUI model without building a
global Knowledge Earth.

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

This is a research/prototype planning artifact. It does not implement UI and
does not require a global map.

## Core Claim

The first ZUI prototype should prove conservation, not scale. It should show
that one object, one source anchor, one relation, one warning, and one way-back
trail survive a few scale changes.

```text
small ladder before world map
```

## Findings / Design Decisions

1. Start with small ladders that test conservation of identity, provenance,
   relation state, truth state, and way back.
2. Use Source-to-World as the first ZUI entry point.
3. Avoid global graph/world-map work until source/object/relation trust is
   proven locally.
4. Use real imported Softland material where possible.
5. Rank prototype slices by what invariant they prove, not by visual ambition.

## Prototype Principles

1. Use real imported material, not fake sample data.
2. Start from Source-to-World trust.
3. Test three or four adjacent scales, not all of Knowledge Earth.
4. Preserve identity visibly.
5. Preserve source/provenance visibly.
6. Preserve accepted/candidate/truth state visibly.
7. Preserve way back.
8. Avoid global graph as first screen.
9. Avoid animation work unless it directly supports orientation.
10. Judge by whether another mind can enter without private reconstruction.

## Slice 1 - Source Artifact To Object To Source Span

Prototype path:

```text
source artifact
  -> object outline
  -> object detail
  -> exact source span
  -> back to object
```

Zoom levels tested:

```text
0.1 topic/local world
1.0 ground interaction
10.0 substrate/source mapping
```

User question:

```text
What did this imported source become, and can I recover exactly where a native
object came from?
```

Material:

- One markdown file imported through object-container markdown path.
- One document-level ObjectContainer.
- Several DerivedUnits.
- SourceAnchors.
- At least one edited/promoted object if available.

What this makes visible:

- SourceArtifact identity.
- ObjectContainer or DerivedUnit identity.
- Derived vs graduated state.
- SourceAnchor presence.
- Exact source span.
- Way back from source to native object.

What this keeps folded but recoverable:

- Full source body until reveal.
- Full object-container substrate rows.
- Full revision history.

What this must not imply:

- That every DerivedUnit is already a durable ObjectContainer.
- That exact source span equals accepted relation truth.

Failure modes:

- Source reveal loses local-world context.
- DerivedUnit looks durable.
- Object identity changes between outline and source reveal.
- Anchor status is only visible in backend metadata.

Acceptance checks:

- Selecting a native item shows whether it is source-backed, derived, or
  graduated.
- Exact source span is recoverable.
- From source span, native object is recoverable.
- Returning preserves original focus and scope.

What this proves:

```text
identity + provenance can survive small zoom movement
```

What this does not prove:

```text
large-scale domain navigation
multi-source relation density
global landscape
```

## Slice 2 - Local Topology To Selected Relation To Evidence

Prototype path:

```text
local topology
  -> selected relation
  -> relation detail
  -> source evidence / decision trail
  -> back to local topology
```

Zoom levels tested:

```text
0.1 topic/local world
1.0 ground interaction
10.0 substrate/provenance
```

User question:

```text
Why are these two things connected, and is this accepted or only candidate?
```

Material:

- Two or more native objects.
- One accepted CompositionEdge or RelationEdge.
- One candidate or inferred relation.
- Evidence/source anchors for at least one edge.

What this makes visible:

- Relation type.
- Relation state:

  ```text
  accepted / candidate / inferred / rejected / projection-derived
  ```

- Source/evidence backing.
- Decision provenance for accepted relation if available.
- Way back to local topology.

What this keeps folded but recoverable:

- Full graph.
- Full evidence list.
- Rejected relation detail.
- Event audit.

What this must not imply:

- That candidate and accepted relations are equivalent.
- That proximity or layout is a relation.
- That relation strength is known if it is only count/projection-derived.

Failure modes:

- Candidate edge looks accepted.
- Relation evidence is hidden too deeply.
- Following the relation strands the user at the target object.
- Aggregated edge hides mixed relation states.

Acceptance checks:

- Accepted and candidate relation encodings remain distinguishable at topology
  scale.
- Selecting relation reveals why it is visible.
- Evidence/decision is recoverable.
- Returning restores source object and local topology.

What this proves:

```text
relation truth-state can survive zoom and inspection
```

What this does not prove:

```text
large graph layout
automatic relation discovery
multi-agent synthesis
```

## Slice 3 - Softland Project Map To Object-Container Thread To Code Line

Prototype path:

```text
Softland project region
  -> object-container local world
  -> selected architecture object
  -> substrate mapping
  -> code file/source line
  -> back to architecture object
```

Zoom levels tested:

```text
0.01 domain
0.1 topic/local world
10.0 substrate/projection machinery
100.0 code/bedrock
```

User question:

```text
How does this design concept connect to the code that makes it real?
```

Material:

- Softland docs/current-mental-model material.
- Object-container spec objects.
- Object-container implementation code references.
- Tests or relevant source line anchors where available.

What this makes visible:

- Project/domain region.
- Local-world entry.
- Object-container concept identity.
- Mapping from concept to substrate row or function.
- Code file/line.
- Way back to design object.

What this keeps folded but recoverable:

- Full global project map.
- Full codebase.
- Full docs history.
- Full test suite output.

What this must not imply:

- That docs are runtime truth.
- That code is detached developer tooling.
- That every design concept already has implementation.

Failure modes:

- Code line opens as a separate world.
- Architecture object loses source/provenance.
- User cannot return from code to local-world design focus.
- The view implies unimplemented docs are implemented.

Acceptance checks:

- Concept identity survives descent to code.
- Code/source line is clearly bedrock scale.
- Implementation status is clear:

  ```text
  implemented / partial / planned / doc-only / stale
  ```

- Returning restores the original design object and local-world context.

What this proves:

```text
the code editor can behave as Softland at zoom 100
```

What this does not prove:

```text
global Knowledge Earth navigation
semantic search across all code
automatic architecture conformance
```

## Slice 4 - Truth-State Aggregation Ladder

Prototype path:

```text
mixed imported local world
  -> aggregate status overview
  -> warning cluster
  -> failed/partial/stale object
  -> exact source or substrate reason
```

Zoom levels tested:

```text
0.01 domain or source collection overview
0.1 topic/local world
1.0 ground interaction
10.0 substrate/projection machinery
```

User question:

```text
Does zooming out hide uncertainty, or does it preserve calibration?
```

Material:

- One source-backed object.
- One candidate relation.
- One inferred relation.
- One partial parse or parse failure.
- One stale/unanchored object if possible.

What this makes visible:

- Mixed truth-state summary.
- Anchor coverage.
- Partial/failure/stale warning.
- Exact object/source causing warning.
- Compression policy for aggregate.

What this keeps folded but recoverable:

- Per-object detail.
- Full error logs.
- Raw source body.
- Projection internals.

What this must not imply:

- That a clean overview means clean data.
- That the worst state applies to every item.
- That warnings are failures of the product rather than honest calibration.

Failure modes:

- Overview hides partial/stale/failure states.
- Warning count cannot be traced to exact object/source.
- Candidate relation is summarized as accepted relation.

Acceptance checks:

- Mixed-state region remains visibly mixed.
- Aggregate can explain how it summarized states.
- Warning descends to exact item/source/substrate reason.
- Returning preserves scope and filters.

What this proves:

```text
truth-state can survive compression
```

What this does not prove:

```text
final visual grammar for all states
large-scale uncertainty visualization
```

## Slice 5 - Reasoning Trail Zoom

Prototype path:

```text
trail overview
  -> decision group
  -> specific turn/event
  -> source/tool/code evidence
  -> back to trail overview
```

Zoom levels tested:

```text
0.1 topic/local world
1.0 ground interaction
10.0 substrate/provenance
100.0 code if the trail touches implementation
```

User question:

```text
Can a reasoning trail be replayed at different dosages without losing causality,
uncertainty, or evidence?
```

Material:

- One agent/human reasoning trail.
- At least one decision.
- At least one unresolved/candidate claim.
- At least one tool output or source/code reference.

What this makes visible:

- Trail as replayable loop.
- Decision points.
- Evidence and source anchors.
- Candidate/unresolved states.
- Semantic time as path rather than false single-line causality.

What this keeps folded but recoverable:

- Full transcript.
- Low-value status messages.
- Full tool output.
- Full code/source context.

What this must not imply:

- That summary equals synthesis.
- That linear log order equals causality when parallel work exists.
- That unresolved claims are settled decisions.

Failure modes:

- Trail overview becomes static prose.
- Decisions lose evidence.
- Candidate claims look accepted.
- Back from evidence/code loses the trail focus.

Acceptance checks:

- Overview shows what was decided, unresolved, and evidence-backed.
- Descending reveals exact event/source/tool material.
- Returning restores the decision group.
- Unresolved/candidate state survives all levels.

What this proves:

```text
semantic zoom can act as pacing for trails
```

What this does not prove:

```text
global research-paper-as-log workflow
multi-agent synthesis UI
```

## Slice 6 - Cross-Source Local World

Prototype path:

```text
source collection overview
  -> local world containing markdown + transcript + code
  -> selected cross-source relation
  -> source anchors for each endpoint
  -> substrate ownership check
```

Zoom levels tested:

```text
0.01 domain/source collection
0.1 topic/local world
1.0 ground interaction
10.0 substrate/projection machinery
```

User question:

```text
Can Softland show a markdown block, transcript message, and code span in one
world without creating source-specific truth islands?
```

Material:

- One markdown source.
- One transcript source if available.
- One code artifact or code-span object.
- Common ObjectContainer/SourceArtifact/SourceAnchor shape or clearly labeled
  source-specific prototype rows.

What this makes visible:

- Common object identity versus source-specific read model.
- Source type.
- Source anchor for each endpoint.
- Accepted/candidate cross-source bridge.
- Substrate ownership.

What this keeps folded but recoverable:

- Full source-specific projections.
- Full ingest operational indexes.
- Full code parser details.

What this must not imply:

- That transcript-local object-shaped rows are common ObjectContainers if they
  are not.
- That cross-source proximity is accepted bridge.
- That source-specific projections are base truth.

Failure modes:

- Each source type becomes its own world.
- Cross-source bridge looks accepted because it is visually useful.
- Substrate ownership is hidden.

Acceptance checks:

- The view can say which rows are common base truth and which are source-specific
  projections.
- Cross-source relation state is explicit.
- Each endpoint reveals source anchor.
- User can return to source collection overview.

What this proves:

```text
ZUI can cross source types while preserving object-container architecture
```

What this does not prove:

```text
all ingesters are already unified
full multi-silo import product
```

## Slice 7 - Preserved Disagreement Mini-Diagram

Prototype path:

```text
topic overview
  -> two conflicting paths
  -> path assumptions/evidence
  -> preserved disagreement artifact
  -> possible synthesis prompt/action boundary
```

Zoom levels tested:

```text
0.1 topic/local world
1.0 ground interaction
10.0 substrate/provenance
```

User question:

```text
Can Softland preserve disagreement structurally instead of summarizing it away?
```

Material:

- Two claims or models.
- Evidence for each path.
- A relation indicating contradiction/tension/non-commuting path.
- No forced synthesis.

What this makes visible:

- Each path's objects and relations.
- Shared overlap.
- Divergence point.
- Accepted/candidate status of conflict relation.
- Evidence and source anchors.
- Whether synthesis is possible or premature.

What this keeps folded but recoverable:

- Full debate history.
- Full source bodies.
- Alternate paths not in current focus.

What this must not imply:

- That disagreement is resolved.
- That summary is synthesis.
- That one path is accepted unless model says so.

Failure modes:

- Conflict becomes a comment thread.
- Overview collapses disagreement into neutral summary.
- Synthesis action appears before enough preserved structure exists.

Acceptance checks:

- User can identify both paths, their evidence, and where they diverge.
- The disagreement state survives zoom out.
- Synthesis remains late-bound.

What this proves:

```text
ZUI can preserve plurality at local scale without building global collective
intelligence UI
```

What this does not prove:

```text
full colimit/synthesis artifact workflow
multi-user collaboration
```

## Recommended First Prototype Order

1. Slice 1 - Source Artifact To Object To Source Span.
2. Slice 2 - Local Topology To Selected Relation To Evidence.
3. Slice 4 - Truth-State Aggregation Ladder.
4. Slice 3 - Softland Project Map To Object-Container Thread To Code Line.
5. Slice 6 - Cross-Source Local World.
6. Slice 5 - Reasoning Trail Zoom.
7. Slice 7 - Preserved Disagreement Mini-Diagram.

Reason:

```text
prove source trust
then prove relation trust
then prove compression trust
then prove zoom-100 continuity
then prove cross-source worldhood
then prove trail pacing
then prove preserved plurality
```

## Minimum Data Fixture For First Slice

Use one imported markdown source that includes:

- Document-level SourceArtifact.
- Document-level ObjectContainer.
- At least five DerivedUnits.
- SourceAnchors with offsets or source-native references.
- One manual edit or selection that graduates a DerivedUnit.
- One accepted composition edge.
- One candidate relation.
- One partial/failure/stale/unanchored state if available, or a deliberately
  mocked state clearly labeled as prototype fixture.

## What This Makes Visible

- Small testable ladders.
- Which invariants each slice proves.
- Why global map comes later.
- How Source-to-World becomes the first ZUI entry.

## What This Keeps Folded But Recoverable

- Final screen layout.
- Rendering approach.
- Global terrain.
- Full visual grammar.
- Full ingester unification.

## What This Must Not Imply

- That these slices are implementation tickets.
- That all slices must be built before Goal 6.
- That mocked state is acceptable in real trust views unless clearly labeled.
- That global Knowledge Earth is rejected forever.

## Failure Modes

- Prototype tests animation instead of preservation.
- Prototype uses fake clean data and misses trust states.
- Prototype starts with global graph and hides source-to-world trust.
- Prototype has no way-back trail.
- Prototype cannot state which invariant it is proving.

## Acceptance Checks

A ZUI prototype slice is valid when it can answer:

1. What identity is being followed?
2. What scale changed?
3. What representation changed?
4. What source/provenance survived?
5. What relation state survived?
6. What truth state survived?
7. What was folded?
8. How can the folded detail be recovered?
9. What is the way back?
10. What did this slice prove?

## Open Questions

1. Which real imported Softland material should become the first fixture?
2. Are partial/failure/stale states already available in current object-container
   reads, or does the first prototype need a labeled fixture state?
3. Which cross-source material is safest to use without re-opening transcript
   ingester architecture?
4. Should the first prototype expose substrate scale directly, or only through
   source reveal?
5. What user-facing language should describe zoom levels without sounding like
   developer controls?

## Next Recommended Goal

Run Goal 4 next, then use Goal 5 to instantiate Slice 1 and Slice 3 against real
Softland self-import material. Goal 6 should design the first Source-to-World
View only after those scenario pressures are concrete.
