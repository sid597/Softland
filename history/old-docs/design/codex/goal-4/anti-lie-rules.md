# Anti-Lie Rules

Status: revised Goal 4 draft after reading completed Goals 1-3, 2026-06-09.

## Origin prompt

```text
Design the truth-state visual grammar for Softland ... Include anti-lie rules.

Recheck if Goals 1-3 are done; if so, use that and redo.
```

## Goal

Define hard prohibitions that prevent Softland views from overstating current
truth, current implementation, source-map exactness, projection meaning,
relation status, authority, or action safety.

## Inputs read

```text
docs/current-mental-model/design/codex/orientation.md
docs/current-mental-model/design/codex/goal-4/description.md
docs/current-mental-model/design/codex/goal-1/charter.md
docs/current-mental-model/design/codex/goal-1/view-laws.md
docs/current-mental-model/design/codex/goal-1/track-glossary.md
docs/current-mental-model/design/codex/goal-2/current-substrate-map.md
docs/current-mental-model/design/codex/goal-2/import-material-matrix.md
docs/current-mental-model/design/codex/goal-2/view-data-contract.md
docs/current-mental-model/design/codex/goal-2/missing-view-queries.md
docs/current-mental-model/design/codex/goal-3/precedent-atlas.md
docs/current-mental-model/design/codex/goal-3/borrow-reject-table.md
docs/current-mental-model/design/codex/goal-3/research-gaps.md
docs/current-mental-model/design/codex/goal-3/annotated-bibliography.md
```

## Scope

These rules apply to all future Softland source, native, trust, topology,
outline, graph, timeline, trace, canvas, local-world, and semantic-zoom views.

They do not define the whole screen. They define what the screen may not lie
about.

## Core claim

The dangerous Softland lie is not visual messiness. The dangerous lie is clean
overstatement:

```text
the view looks organized,
therefore the transformation preserved understanding,
therefore the user can trust it.
```

Goal 1 says this is the central anxiety. Goal 2 says the current substrate is
real but partial. Goal 3 says source maps, local topology, uncertainty
visualization, trace, and projection health are the right precedents only if
Softland refuses their failure modes.

## Hard prohibitions

### 1. Do not show inferred relation as accepted

AI, heuristic, parser, embedding, situate, or policy output must stay inferred
or candidate until an accepted relation event/row exists.

Implementation check:

```text
Dotted/inferred treatment cannot become solid accepted treatment because it is
displayed, repeated, useful, or high confidence.
```

### 2. Do not show candidate relation as accepted

Candidates must stay dashed/proposed and excluded from accepted topology counts.

Implementation check:

```text
Accepted-edge queries and accepted topology views do not include candidates.
```

### 3. Do not show semantic RelationEdges as current when the substrate lacks them

Goal 2 found no implemented `RelationEdge` rows/PStates in the current
object-container substrate.

Implementation check:

```text
Current markdown/transcript topology may show CompositionEdges. Semantic
supports/contradicts/depends-on style RelationEdges require actual relation
storage or must be labelled future/candidate/inferred.
```

### 4. Do not collapse CompositionEdge into RelationEdge

Parent/child, contains, follows, produced, and order are structural relations.
They are not semantic claims unless separately represented.

Implementation check:

```text
Composition uses bracket/rail/containment/order language; semantic relation
uses typed connector language only when relation state exists.
```

### 5. Do not hide missing anchors

Missing SourceAnchors must appear as empty sockets or count residue.

Implementation check:

```text
ObjectContainer/DerivedUnit without known SourceAnchor does not render as fully
grounded.
```

### 6. Do not hide stale anchors

Source hash/version/span drift must show broken tether treatment until repaired
or explicitly accepted/ignored.

Implementation check:

```text
Stale anchors cannot share healthy anchor styling in any projection.
```

### 7. Do not show target-to-source support as full bidirectional source maps

Goal 2 says target -> anchors exists, while arbitrary source-range -> hydrated
target lookup is weaker/missing.

Implementation check:

```text
Raw source hover must show exact only when source-range/anchor/target rows are
actually hydrated and precise.
```

### 8. Do not show coarse, partial, or missing mapping as exact

Source-map precision is a truth state.

Implementation check:

```text
Exact, partial, target-only, stale, unanchored, source-only, not hydrated, and
missing-query mappings use distinct treatments.
```

### 9. Do not collapse source-specific projection into common truth

Markdown outline and transcript conversation/tool/audit/source-line rows are
projections/indexes. They are not canonical object rows.

Implementation check:

```text
Projection rows use lens bodies and source-family labels, not ObjectContainer
shells.
```

### 10. Do not show operational state as object truth

Transcript run/file-offset/source-line status is useful operational state. It
is not common object identity.

Implementation check:

```text
Operational rows use run/status strips and ops badges.
```

### 11. Do not show source-specific status as common health

Transcript has richer operational status than markdown. The common source-health
query does not exist yet.

Implementation check:

```text
Common source health renders unknown unless a real common aggregate exists.
```

### 12. Do not show missing query as empty success

Missing source inventory, hydrated source bundle, object trust bundle,
source-range lookup, source health, local topology, relation, ViewState,
ContextMembership, LayerOverlay, code/Roam/Linear/Linear-source queries must
show unknown/future/missing state if they affect the view.

Implementation check:

```text
An absent read path creates a missing-query state, not a blank green state.
```

### 13. Do not treat SourceMaterialRefRow as hydrated material

Source refs enumerate material by category. They are not full rows.

Implementation check:

```text
Ref-only material uses not-hydrated styling until target rows are read.
```

### 14. Do not make raw source look like native object truth

Raw source is preserved input. It is not native object identity.

Implementation check:

```text
Raw source uses source slab/rail; native object uses identity shell.
```

### 15. Do not make SourceArtifact look like ObjectContainer

A SourceArtifact captures source version. An ObjectContainer is durable native
identity.

Implementation check:

```text
SourceArtifact gets source body/version cap; ObjectContainer gets identity
shell/kind mark.
```

### 16. Do not make DerivedUnit look durable

Markdown blocks are DerivedUnits until graduation.

Implementation check:

```text
DerivedUnit uses hollow/dashed shell and lacks durable identity bead.
```

### 17. Do not let graduation erase derivation

Graduated objects retain source stripe, derived notch, SourceAnchor, and
promotion history.

Implementation check:

```text
Graduated shell remains visibly derived-from-source.
```

### 18. Do not show code, Roam/DG, Linear, or Softland-doc-specific import as implemented

Goal 2 found no common object-container ingesters for those source families.
Softland docs can only be treated as generic markdown if imported that way.

Implementation check:

```text
Future source families are absent, disabled, or ghosted as not implemented.
```

### 19. Do not show ViewState as model state

Hover, selection, collapse, zoom, filter, and xy are presentation state unless
explicitly stored in a model-backed row.

Implementation check:

```text
ViewState uses chrome treatment and does not persist as canonical object truth.
```

### 20. Do not show ContextMembership/local-world placement as implemented current truth unless backed by rows

Goal 2 found no `ContextMembership` PState.

Implementation check:

```text
Local-world placement uses future/view-only labels unless model-backed
membership exists.
```

### 21. Do not let local placement imply semantic relation

Objects near each other or sharing a board/thread/local world are not related
unless an edge says so.

Implementation check:

```text
Placement pins and RelationEdges use different visual vocabularies.
```

### 22. Do not show projection proximity as semantic relation without projection label

Embedding maps, clusters, force layouts, timeline grouping, search order, and
similarity maps need projection labels and health metadata.

Implementation check:

```text
Projection proximity cannot use accepted edge styling.
```

### 23. Do not let color be the only truth-state encoding

Truth state must survive grayscale, print, low contrast, and zoomed-out
aggregates.

Implementation check:

```text
Status differs by stroke, shape, label, glyph, position, and interaction.
```

### 24. Do not hide parse failure or partial parse

Transcript parse-error lines create no containers. Markdown/trancript partial
states must remain visible.

Implementation check:

```text
Parse failure uses blocked source segment, not empty list.
Partial parse uses segmented coverage, not complete shell.
```

### 25. Do not hide disconnected islands

Disconnected material is a view state that matters. It should not be silently
bridged by candidate/inferred/projection proximity.

Implementation check:

```text
Disconnected islands show shoreline/count and separate candidate/inferred
bridges if any.
```

### 26. Do not make human-authored mean verified

Human authorship is authority/provenance, not truth guarantee.

Implementation check:

```text
Human mark is separate from accepted/candidate/source-grounded/verified state.
```

### 27. Do not make AI inference mean truth

AI output needs model/run/input provenance and remains inferred/candidate until
accepted.

Implementation check:

```text
AI mark is separate from accepted status and source-anchor status.
```

### 28. Do not make simulation output look like observed fact

Simulation output is run-produced material with parameters and inputs.

Implementation check:

```text
Simulation output uses run/output skin and reveals input/parameter/seed/version.
```

### 29. Do not make shared/team annotation mean accepted model truth

Collaboration and authority state must not collapse into consensus.

Implementation check:

```text
Personal/team annotations carry scope marks and separate accepted status.
```

### 30. Do not summarize disagreement into consensus

Preserved disagreement stays structurally visible until a synthesis/acceptance
event exists.

Implementation check:

```text
Competing claims/relations render as split/forked state with source/authority
marks, not one flattened summary.
```

### 31. Do not show accepted decisions as universal truth

Accepted means accepted into Softland's model with provenance. It does not mean
external truth is guaranteed.

Implementation check:

```text
Accepted audit stamp explains decision, source, actor, and scope.
```

### 32. Do not let confidence scores create false certainty

Uncalibrated confidence numbers should not be shown as precision.

Implementation check:

```text
If confidence appears, method, calibration, input set, and limits are visible.
```

### 33. Do not let pending actions appear committed

Accept, reject, promote, edit, reanchor, import, reparse, simulate, and create
relation actions need pending state before commit.

Implementation check:

```text
Pending ring/stroke is distinct from accepted shell/edge.
```

### 34. Do not let failed actions leave half-accepted visuals

Failed actions restore prior visual state and leave failure residue.

Implementation check:

```text
Failed pending action cannot leave a solid accepted edge/object behind.
```

### 35. Do not make global overview erase local trust

Zoomed-out views may compress, but must preserve source-map, warning, unknown,
projection, and relation-status residue.

Implementation check:

```text
Overview clusters show counts for stale, unanchored, failed, candidate,
inferred, disconnected, and missing-query states.
```

### 36. Do not hide provenance in modal-only paths

The main view should show enough source/anchor/status residue that trust does
not require opening a deep modal.

Implementation check:

```text
Selected object shows source stripe/socket and truth-state glyph outside the
deep inspector.
```

## Required prohibitions from Goal 4 brief

```text
Do not show inferred relation as accepted.
Do not hide missing anchors.
Do not collapse source-specific projection into common truth.
Do not show dimensional-reduction proximity as semantic relation without a
projection label.
Do not let color be the only truth-state encoding.
```

These are covered by Rules 1, 5, 9, 22, and 23.

## What this makes visible

```text
implemented vs future substrate
common base truth vs projection/index/ops/view state
source-map precision and query availability
composition vs semantic relation
authority vs acceptance
projection vs relation
known vs unknown vs missing
pending vs committed
```

## What this keeps folded but recoverable

```text
full row payload
full source text
full SourceAnchor and SourceMaterialRef rows
full missing-query explanation
full import/decision/audit history
full transcript operational state
full model/simulation/inference metadata
full rejected/disagreement history
```

## What this must not imply

```text
that every future concept should be displayed now
that uncertainty is bad
that missing substrate means the product is broken
that every object must be source-grounded to be useful
that source grounding means truth
that accepted means externally verified
```

The point is not to shame partial knowledge. The point is to make partial
knowledge inhabitable without letting it masquerade as complete knowledge.

## Failure modes

### Implementation overclaim

The view uses architecture vocabulary as if every noun already has storage and
queries.

### Query overclaim

The view shows a complete trust path when it only fetched a row, a ref, or a
projection entry.

### Projection capture

The user believes the markdown outline, transcript projection, graph layout, or
embedding is the model.

### Source-map theater

The user believes every source span and native object has exact bidirectional
mapping.

### Relation laundering

Composition, proximity, candidate, inferred, and accepted semantic relation all
look like the same line.

### Authority laundering

Human, AI, simulation, policy, personal, team, and audit state collapse into one
"trusted fact" mark.

## Acceptance checks

1. A reviewer can look at a screenshot and identify base/projection/ops/view/
   future/unknown states without opening a modal.
2. The screenshot does not show any current semantic RelationEdge unless a real
   relation row exists.
3. Markdown DerivedUnits and transcript ObjectContainers are visually distinct.
4. Source-specific projection rows never use ObjectContainer shell.
5. Source-to-native hover indicates exactness, partialness, target-only support,
   or missing query.
6. Unknown health and missing query states do not look successful.
7. Code/Roam/DG/Linear source families are not shown as implemented current
   imports.
8. View-only selection/collapse/zoom/xy does not look model-backed.
9. AI/human/simulation/policy/personal/team/audit authority states are separate
   from accepted/candidate/inferred state.
10. Collapsed or zoomed views retain warning/unknown residue.

## Open questions

1. Should the renderer enforce a required substrate badge for every mark?
2. Should first prototype screenshots include a visible legend, or should the
   glyph stack be self-explanatory through labels/tooltips?
3. Should future/unimplemented source families appear in Goal 5 scenarios, or
   should Goal 5 stay only on implemented markdown/transcript paths?
4. What is the minimum "accepted relation" substrate required before semantic
   edges can appear in any first prototype?
5. How should unknown/missing-query copy be phrased so users trust the honesty
   rather than reading it as failure?

## Next recommended goal

Goal 5 should pressure-test these anti-lie rules with concrete markdown and
transcript examples, including at least one parse failure, one source-map
partialness case, one projection-only row, one not-hydrated source ref, and one
future/unimplemented relation or source family.

