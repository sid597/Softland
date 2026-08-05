# Semantic Zoom Model

Status: drafted, 2026-06-09.

## Origin Prompt

```text
Research and define Softland's ZUI / semantic zoom model. Treat ZUI as lawful
compression across scales of understanding, not as zoom animation.

I want to know what changes at each zoom level, what survives every scale
change, how identity/provenance/relation/truth-state are preserved, how the
user knows where they are, and what small prototype slices can test this without
building the whole global map.
```

## Goal

Define Softland zoom levels and the contract for what each level shows,
compresses, preserves, and permits.

## Inputs Read

- `docs/current-mental-model/design/codex/orientation.md`
- `docs/current-mental-model/design/codex/goal-7/description.md`
- `docs/vision/epistemic-framework.md`
- `docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md`
- `docs/current-mental-model/architecture/object-container-spec.md`
- `docs/current-mental-model/architecture/object-container-ingester-contract.md`
- `docs/current-mental-model/build/knowledge-earth-zui/CATEGORY_THEORY_DESIGN_RESEARCH.md`
- External ZUI, semantic zoom, focus+context, GIS scale, source-map,
  provenance, uncertainty, and olog/category-theory sources listed in
  `zui-research.md`.

## Scope

This is a conceptual model. It does not implement UI and does not prescribe a
specific visual layout.

## Core Claim

Softland has one world across scales. The entities and actions change by zoom
level, but object identity, provenance recoverability, relation state, truth
state, and the way back survive every scale change.

```text
zoom changes dosage, representation, and valid action
zoom must not change truth
```

## Findings / Design Decisions

1. Treat the zoom ladder as semantic levels, not UI modes.
2. Give every level a different user question, entity set, compression policy,
   and action set.
3. Make Source-to-World the first trustworthy ZUI entrance; do not start with a
   global map.
4. Require a semantic address at every level so the user knows world, scale,
   projection, focus, scope, state, and trail.
5. Treat code as Softland at zoom 100, not as separate developer tooling.

## Softland Zoom Levels

The canonical levels for Goal 7:

```text
0.001  knowledge landscape
0.01   domain
0.1    topic / local world
1.0    ground interaction
10.0   substrate / projection machinery
100.0  code / bedrock
```

The numbers are semantic anchors, not mandatory UI stops. A future interface may
allow continuous motion, but the model contract should still resolve to these
levels.

## What Changes Across Levels

| Zoom | Main change | Surface entity | Main compression | Main action |
|---|---|---|---|---|
| 0.001 | Total terrain becomes orientable | fields, regions, frontiers | objects into regions | choose where to enter |
| 0.01 | Field becomes domain structure | subdomains, programs, models | claims into topic clusters | choose path or region |
| 0.1 | Domain becomes inhabitable local world | claims, evidence, objects, trails | source bodies and distant context | inspect and follow |
| 1.0 | Structure becomes manipulable | active object/model/source relation | overview context | test, edit, ask, run |
| 10.0 | Projection becomes debuggable | ingesters, anchors, mappings, policies | authored content | inspect transformation |
| 100.0 | World becomes modifiable bedrock | code, schemas, tests, source lines | product-level map | change implementation |

## What Survives Every Scale Change

1. Stable identity:

   ```text
   region/local-world/object/source/code symbol identities do not become copies
   when represented differently
   ```

2. Source/provenance recoverability:

   ```text
   exact anchors may be folded, but anchor presence, status, and path to reveal
   survive
   ```

3. Relation state:

   ```text
   accepted/candidate/rejected/inferred/aggregated relation status survives
   ```

4. Truth state:

   ```text
   fact/hypothesis/inference/simulation/source-backed/partial/stale/unanchored
   distinctions survive
   ```

5. Local orientation:

   ```text
   current world, current scale, current focus, current path, and way back remain
   visible or one gesture away
   ```

6. Action legitimacy:

   ```text
   the user can tell which actions are valid at this scale and whether an action
   changes canonical model truth or only view state
   ```

## Level 0.001 - Knowledge Landscape

Scale name:

```text
knowledge landscape
```

User question:

```text
Where is this body of understanding in the larger world?
```

Visible entities:

- Fields.
- Large domains.
- Knowledge frontiers.
- Major schools or traditions.
- Maturity and controversy regions.
- Imported corpora as terrain regions.
- Local worlds as named entry points.
- Aggregate truth/provenance coverage.
- Routes/trails through prior exploration.

Compressed entities:

- Individual ObjectContainers.
- DerivedUnits.
- Source bodies.
- Exact SourceAnchors.
- Local relation evidence.
- Code and projection machinery.
- Detailed agent trails.

Preserved identity:

- Domain/region id.
- Local-world id.
- Source collection id.
- Aggregate identity must be traceable to member object ids.
- If an object is selected before zooming out, it remains marked as folded inside
  its aggregate.

Preserved provenance:

- Provenance coverage is visible as a summary:

  ```text
  source-backed / partially anchored / inferred / stale / unanchored
  ```

- Exact spans are folded but recoverable through descent.
- Aggregation policy is inspectable.

Preserved relations:

- High-level relations are aggregates:

  ```text
  related domains
  dependency between fields
  contradiction fronts
  bridge regions
  evidence-density regions
  unresolved islands
  ```

- Accepted/candidate/inferred ratios survive as state summaries.

Interactions:

- Choose an entry region.
- Filter by source coverage, uncertainty, maturity, or frontier.
- Reveal why a region exists.
- Descend to domain.
- Return to previous local world.
- Follow a named route/trail.

Failure modes:

- Global map implies total coverage.
- Embedding proximity is mistaken for accepted relation.
- Aggregate labels sound canonical when generated.
- Missing anchors vanish into a pretty landscape.
- User cannot recover the local world they came from.

## Level 0.01 - Domain

Scale name:

```text
domain
```

User question:

```text
What are the neighborhoods, models, programs, sources, and open questions in
this domain?
```

Visible entities:

- Subdomains.
- Research programs.
- Major models.
- Datasets and source collections.
- Named debates.
- Known methods.
- Labs/communities/agents when relevant.
- Open-question clusters.
- Imported source groups.
- Local worlds available for entry.

Compressed entities:

- Paper-level details.
- Individual claims.
- Evidence spans.
- Transcript messages.
- Revision/event history.
- Code files and functions.

Preserved identity:

- Domain ids and subdomain ids.
- Local-world memberships.
- SourceArtifact collection ids.
- Object count and status summaries.
- Selected aggregate has member list recovery.

Preserved provenance:

- Domain coverage by source type:

  ```text
  docs / transcripts / code / papers / external references / manual notes
  ```

- Anchor health summaries:

  ```text
  anchored / partially anchored / stale / failed / source-only
  ```

Preserved relations:

- Typed domain relations:

  ```text
  contains
  depends-on
  contradicts
  supports
  refines
  imports-from
  unresolved-bridge
  ```

- Relation truth-state is summarized, not erased.

Interactions:

- Enter a topic/local world.
- Compare two programs or schools.
- Filter to unresolved/candidate relation areas.
- Reveal domain source inventory.
- Follow a bridge relation.
- Save a route into a local world.

Failure modes:

- Domain map becomes taxonomy cosplay.
- Generated clusters imply accepted ontology.
- Candidate bridges look like real synthesis.
- Source-specific silos become separate base truths.
- Domain overview hides parse failures.

## Level 0.1 - Topic / Local World

Scale name:

```text
topic / local world
```

User question:

```text
What is the structure of this question, debate, artifact, or workflow, and how
can I enter it safely?
```

Visible entities:

- ObjectContainers.
- DerivedUnits that are still not durable.
- Claims.
- Evidence.
- Decisions.
- Relations.
- Source anchors.
- Imported document outline.
- Local topology.
- Reasoning trails.
- Disconnected islands.
- Candidate edges.
- Warnings for partial/stale/failed states.

Compressed entities:

- Full source body.
- Distant domain context.
- Detailed revision history.
- Full code bedrock.
- Long agent transcript/tool details.

Preserved identity:

- ObjectContainer id.
- DerivedUnit id.
- SourceArtifact id.
- Revision id where relevant.
- Local-world placement id.
- Stable selection across outline, graph, inspector, timeline, and future canvas.

Preserved provenance:

- SourceAnchor presence is visible on object and relation surfaces.
- Exact source span is one reveal away.
- Derived/grafted/graduated state is visible.
- Import/run/distiller version is folded but recoverable.

Preserved relations:

- CompositionEdges:

  ```text
  contains
  parent/child
  order
  next-sibling
  source-declared structure
  ```

- RelationEdges:

  ```text
  supports
  contradicts
  references
  derives-from
  depends-on
  candidate bridge
  rejected edge
  inferred edge
  ```

- Accepted/candidate distinction is visible without opening metadata.

Interactions:

- Select object.
- Reveal source span.
- Follow relation to neighbor without losing place.
- Compare accepted and candidate relation evidence.
- Promote a DerivedUnit by meaningful touch.
- Save the local world.
- Ask for a teaching path or next probe.
- Filter by truth/provenance state.

Failure modes:

- A local graph overwhelms instead of orienting.
- Candidate relations look accepted.
- Source anchors are hidden in an inspector only.
- DerivedUnit feels like a durable object before touch.
- The user can follow links but cannot return to the entry point.

## Level 1.0 - Ground Interaction

Scale name:

```text
ground interaction
```

User question:

```text
What can I inspect, test, vary, ask, edit, run, accept, or reject now?
```

Visible entities:

- Active selected object.
- Object content/revision.
- Immediate source span.
- Immediate accepted/candidate/rejected relations.
- Local evidence.
- Model/REPL/simulation if present.
- Action affordances.
- Pending ActionRequests.
- Result/decision feedback.

Compressed entities:

- Whole domain map.
- Distant source inventory.
- Full relation graph.
- Full event history.
- Full codebase.

Preserved identity:

- Active ObjectContainer id.
- Current Revision id.
- SourceAnchor id.
- RelationEdge id.
- ActionRequest id when the user acts.
- Placement id if view-specific state changes.

Preserved provenance:

- Source span or source object is directly visible or one reveal away.
- Edit/test/action result records event provenance.
- If an edit promotes a DerivedUnit, source anchor survives.
- If a relation is accepted/rejected, the decision trail is recoverable.

Preserved relations:

- Immediate relation neighborhood.
- Relation role and state.
- Evidence for selected relation.
- Model dependency edges where a simulation/REPL is present.

Interactions:

- Edit.
- Quote.
- Annotate.
- Ask.
- Run computation or simulation.
- Accept/reject candidate relation.
- Create relation proposal.
- Compare source and native object.
- Jump to substrate mapping if trust is unclear.

Failure modes:

- Projection gesture mutates canonical truth directly.
- Editing severs source provenance.
- Relation creation bypasses candidate/accepted distinction.
- Action feedback looks final when request was rejected or partial.
- The user cannot tell whether they changed the model or only the view.

## Level 10.0 - Substrate / Projection Machinery

Scale name:

```text
substrate / projection machinery
```

User question:

```text
How did this view/object/relation get produced, and can I trust the
transformation?
```

Visible entities:

- SourceArtifact.
- ObjectContainer.
- DerivedUnit.
- Revision.
- SourceAnchor.
- CompositionEdge.
- RelationEdge.
- ContextMembership.
- LayerOverlay.
- ViewState.
- Interpreter/projection.
- ImportEnvelope or import materialization shape.
- Ingest run.
- Distiller version.
- Parse status.
- Drift/stale status.
- Decision/event audit.

Compressed entities:

- Full authored source content.
- Domain-level meaning.
- Cosmetic view details.
- Full code implementation.

Preserved identity:

- Canonical ids for every base row.
- Source-specific ids remain source ids, not common truth ids.
- View-specific placement ids are distinct from object ids.
- Projection identity is separated from object identity.

Preserved provenance:

- Source hash/ref.
- Source offsets/native ids.
- Ingester/distiller version.
- Revision history.
- ActionRequest/KernelEvent trail.
- Accepted/rejected decision trail.
- Projection policy.

Preserved relations:

- Transformation relations:

  ```text
  source produced derived unit
  derived unit graduated to container
  container revision anchored to source
  edge accepted by decision
  projection rendered object
  view gesture emitted action request
  ```

- Composition and relation edges remain first-class rows, not hidden fields.

Interactions:

- Inspect source-to-native mapping.
- Reveal projection policy.
- Compare source version drift.
- Rerun a distiller in a candidate layer.
- Inspect why an edge is accepted/rejected/candidate.
- Validate whether an ingester wrote common base truth or a source-specific
  island.
- Descend to code when machinery behavior is wrong.

Failure modes:

- Substrate view becomes backend dashboard noise.
- Source-specific object-shaped rows masquerade as common ObjectContainers.
- Projection invents relationships.
- Anchor drift is hidden.
- ViewState is mistaken for model truth.

## Level 100.0 - Code / Bedrock

Scale name:

```text
code / bedrock
```

User question:

```text
What code defines this world, and how can it be changed without breaking the
truth/provenance/relation contract above?
```

Visible entities:

- Repository snapshot.
- Code files.
- Namespaces/modules.
- Functions.
- PStates/topologies where relevant.
- Schemas/contracts.
- Tests.
- Source lines.
- Build/runtime errors.
- Commits when available.
- Implementation quirks and review findings when relevant.

Compressed entities:

- Knowledge landscape.
- Domain map.
- Topic map.
- User-facing visual state.
- Long product history unless needed.

Preserved identity:

- File path.
- Namespace.
- Function/symbol id where parser identity is stable.
- Line/span anchor.
- Test id.
- Contract/doc pointer as provenance, not canonical code truth.
- Runtime object-to-code mapping where available.

Preserved provenance:

- Git history.
- Test output.
- Action/request/event flow back to code path.
- Source maps or source-like mappings from runtime/projection to authored code.
- Architecture decision trail when available.

Preserved relations:

- Code dependency relations.
- Schema ownership.
- Event/write/read ownership.
- Tests covering behavior.
- Mapping from object-container concepts to implementation rows.

Interactions:

- Open source line.
- Edit code.
- Run tests.
- Inspect runtime mapping.
- Trace user-visible state back to implementation.
- Return to ground/local-world view after code inspection.

Failure modes:

- Code editor is treated as separate developer tooling instead of Softland at
  zoom 100.
- Refactor breaks object identity, provenance, or relation contracts at higher
  zoom.
- Docs become a false source of runtime truth.
- Tests cover mechanics but not the user-facing source-to-world trust loop.

## How The User Knows Where They Are

A Softland ZUI view needs a semantic address. The exact UI can vary, but the
model should always expose:

```text
world:
  current local world or aggregate region

scale:
  0.001 / 0.01 / 0.1 / 1.0 / 10.0 / 100.0

projection:
  outline / topology / timeline / source / substrate / code / future canvas

focus:
  selected object, relation, source span, or aggregate

scope:
  what is included, filtered, hidden, or out of bounds

truth/provenance status:
  aggregate state of current focus and visible neighborhood

trail:
  where the user came from and how to return
```

At minimum:

```text
you are in [local world]
at [scale]
looking through [projection]
focused on [identity]
with [truth/provenance status]
entered from [trail]
```

## The Way Back

The way back is not browser history. It is an epistemic trail:

```text
entry source or region
  -> selected aggregate
  -> local world
  -> object
  -> relation
  -> source span
  -> substrate mapping
  -> code line
```

Each step records:

- Focus identity.
- Scale.
- Projection.
- Filters.
- Relation followed or action taken.
- Source/provenance/truth state at the time.

The user should be able to return to:

- The last focus.
- The entry object.
- The source artifact.
- The containing local world.
- The previous scale.
- The pre-action state.

## Semantic Zoom And Source-To-World View

Source-to-World is the first trustworthy entrance into ZUI:

```text
raw source
  -> SourceArtifact
  -> DerivedUnit / ObjectContainer
  -> SourceAnchor
  -> CompositionEdge / RelationEdge
  -> LocalWorld
```

Goal 7 extends it:

```text
Source-to-World
  proves transformation did not sever trust

ZUI
  lets the resulting world be entered at multiple scales without losing that
  trust
```

Therefore the first ZUI prototype should not be a global Knowledge Earth. It
should be a small source-to-world zoom ladder where identity, provenance,
relation, truth-state, and way-back are visibly conserved.

## What This Makes Visible

- What changes at each scale.
- Which entities are currently visible.
- Which entities are folded.
- Which invariants survive every transition.
- Which interactions are legitimate at each scale.
- Where source trust enters the zoom model.

## What This Keeps Folded But Recoverable

- Raw source bodies at overview scales.
- Exact source spans until inspected.
- Distiller/projection machinery until trust is questioned.
- Code until the user descends to bedrock.
- Full relation neighborhoods until the local scope requires them.

## What This Must Not Imply

- That all scales are equally authoritative.
- That aggregates are accepted objects.
- That generated clusters are canonical ontology.
- That zooming into a topic discovers accepted relations automatically.
- That source provenance is optional once material becomes native.

## Failure Modes

- Scale changes hide state changes.
- The same object has different labels without stable identity cues.
- User zooms into an aggregate and lands in an arbitrary detail.
- The system shows a beautiful overview before it can prove anchor coverage.
- A projection's aggregation policy is invisible.

## Acceptance Checks

- At every level, a selected item has a stable semantic address.
- At every level, the user can tell whether visible relation structure is
  accepted, candidate, rejected, inferred, or aggregate.
- At every level, source/provenance health is visible or one reveal away.
- Every zoom transition can explain what it preserved and what it folded.
- Every model-changing gesture routes through an ActionRequest, not direct view
  mutation.

## Open Questions

1. Should the scale ladder use these exact numeric labels in UI, or only in the
   model?
2. What is the smallest semantic address shape that remains stable across
   outline, topology, source, substrate, and code?
3. How should the user inspect aggregation policy without seeing backend noise?
4. What is the right default entry scale for an imported project?
5. How does continuous zoom interact with discrete truth-state changes?

## Next Recommended Goal

Run Goal 4 next to turn the truth/provenance/relation states named here into a
cross-view visual and interaction grammar. Then Goal 5 can stress this model
against concrete Softland and Discourse Graph scenarios.
