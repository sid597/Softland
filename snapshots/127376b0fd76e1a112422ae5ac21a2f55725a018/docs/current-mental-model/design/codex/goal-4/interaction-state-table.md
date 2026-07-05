# Interaction State Table

Status: revised Goal 4 draft after reading completed Goals 1-3, 2026-06-09.

## Origin prompt

```text
Design the truth-state visual grammar for Softland ... Start from how truth,
uncertainty, provenance, derivation, and relation appear.

Recheck if Goals 1-3 are done; if so, use that and redo.
```

## Goal

Define how truth-state visuals behave when users hover, select, inspect, fold,
zoom, hydrate, accept, reject, promote, repair, project, place, or encounter
missing current substrate.

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

This is not a screen interaction spec. It is the interaction behavior that must
hold across outline, topology, source view, inspector, timeline, trace, canvas,
and semantic zoom.

## Core claim

Softland interactions must reveal whether an action is view-only, query-bound,
projection-bound, source-specific, operational, or model-changing.

The interaction grammar must preserve the Goal 1 boundary:

```text
view != model
projection != situating
candidate != accepted
recoverable compression != hiding
```

And the Goal 2 boundary:

```text
common base rows != source-specific projection rows
known row != hydrated trust bundle
missing query != empty success
implemented markdown/transcript != implemented code/Roam/Linear
```

## Interaction table

| Interaction / state | Applies to | Visual response | Model or view action | Inspector / tooltip response | Anti-lie check |
|---|---|---|---|---|---|
| Hover raw source span | Raw source, SourceArtifact | Highlight source span; highlight native targets only if anchors/targets are hydrated. | ViewState hover only. May trigger hydration/read attempt. | Shows source ref/hash/span, mapping precision: exact, partial, target-only, unknown, or no target. | Do not imply exact source-to-target mapping when range query/hydration is missing. |
| Hover native ObjectContainer | ObjectContainer | Emphasize solid shell, base badge, anchor stripe/socket, immediate accepted CompositionEdges. | ViewState hover only. | Shows object kind, current revision, source anchors if known, hydration state. | Hover cannot accept, link, or verify anything. |
| Hover DerivedUnit | DerivedUnit | Emphasize hollow/dashed shell and source stripe. | ViewState hover only. | Shows distiller/version, source span, non-durable status. | Do not show as editable durable identity unless promotion path is explicit. |
| Select SourceArtifact | Source inventory/source slab | Persistent source focus, version cap, coverage residues. | ViewState selection; may read source and source-material refs. | Shows source row, latest version, import/completion, projection rows, known/missing material. | No global source inventory should be implied if source was opened by known ref/id. |
| Select ObjectContainer | Native item | Persistent selection across projections with same shell/glyph stack. | ViewState selection; may read trust rows individually. | Shows whether trust bundle is complete, partial, not hydrated, or impossible with current queries. | Object row alone is not a complete trust trail. |
| Select projection row | Outline/transcript projection/index | Lens boundary brightens; represented target highlights if available. | ViewState selection; projection read only. | Shows projection type, source-specific fields, common target id, and missing explanation fields. | Projection row must not become object shell. |
| Hydrate source material refs | SourceMaterialRefRow lists | Ref-only sockets become target/anchor/edge bodies as reads complete. | Query/read operation, not model change. | Shows which refs are unresolved, failed, or hydrated. | Source refs are not full rows until hydrated. |
| Native-to-source reveal | ObjectContainer, DerivedUnit, SourceAnchor | Target -> source tether/paired highlight appears. | ViewState reveal/read `read-source-anchors` and source row. | Shows anchor id, source id/ref/hash, span/block path. | Anchor proves grounding, not correctness. |
| Source-to-native reveal | Raw source span | If supported, show targets; otherwise show resolving/unknown/partial socket. | Read via source-anchor refs plus hydration; future range query needed for arbitrary offsets. | Shows exactness and query limitation. | Do not round to broad source region and call it exact. |
| Select SourceAnchor | Anchor tether/socket | Anchor path becomes primary; source and target both highlighted. | ViewState selection. | Shows freshness, precision, source version/hash, target id, stale/missing state. | Healthy/stale/partial/missing anchors must not share styling. |
| Repair stale anchor | Stale anchor | Broken tether enters compare mode. | ActionRequest if reanchor/accept-drift/ignore is chosen. | Shows old source, current source, proposed target/span, audit outcome. | Repair does not happen until accepted event/action result exists. |
| Add anchor to unanchored object | Unanchored object, source span | Empty socket enters candidate-anchor state. | ActionRequest on proposed source span. | Shows proposed anchor precision and actor. | Candidate anchor is not healthy anchor. |
| Promote DerivedUnit | DerivedUnit | Pending ring, then solid shell with derived notch after success. | ActionRequest / `:object/edit`; creates ObjectContainer/Revision/graduation rows. | Shows promotion event, source unit, new object id, retained anchor. | Do not optimistically replace derived shell before commit. |
| Select Revision | Revision stratum/tick | Revision highlighted; object shell remains current unless comparing. | ViewState selection; restore/edit would be ActionRequest. | Shows revision payload, actor/event/time, current/previous relation. | Inspecting revision is not rollback. |
| Select accepted CompositionEdge | CompositionEdge | Solid bracket/rail highlights endpoints and order/containment. | ViewState selection. | Shows edge type, source/import/provenance, target kinds. | Do not describe as semantic support/contradiction. |
| Select accepted RelationEdge | Future/current semantic relation only if real row exists | Solid typed connector highlights endpoints. | ViewState selection. | Shows relation type, provenance, accepted decision. | If no `RelationEdge` substrate row exists, this interaction is unavailable/currently future. |
| Select candidate relation | Candidate relation | Dashed connector focus; accept/reject/defer controls appear. | ViewState selection; accept/reject emits ActionRequest. | Shows proposal source, evidence, authority, current substrate support. | Candidate remains nonaccepted until accepted model event. |
| Select inferred relation | AI/policy/procedure output | Dotted connector focus; inference mark remains. | ViewState selection; accept/reject emits ActionRequest where supported. | Shows model/procedure/input/evidence and calibration limits. | Inference is not accepted relation. |
| Accept candidate/inference | Candidate/inferred relation or object proposal | Pending ring around dashed/dotted mark; solid only after commit. | ActionRequest; accepted event/row required. | Shows action status, actor, time, resulting row if created. | Never use accepted styling while pending. |
| Reject candidate/inference | Candidate/inferred relation or object proposal | Pending ring, then recessed/struck audit residue. | ActionRequest; rejected decision/audit event. | Shows rejected proposal, reason, actor/time. | Rejected is not deleted from provenance. |
| Inspect parse failure | Source failure/status row | Blocked segment expands to failure scope. | ViewState expansion; retry/reparse if available emits action/import. | Shows parser/status error, source line/span, no-native-target explanation. | Failure is not empty success. |
| Inspect partial parse | Source/source group | Coverage segments expand. | ViewState expansion; reads available status rows. | Shows parsed/unparsed counts and missing health query if needed. | Partial coverage must not look complete. |
| Inspect missing query | Unknown/missing-query socket | Socket expands into explanation. | No model change; may link to required future query contract. | Shows missing read, why needed, safe fallback. | Missing query cannot be silently inferred by client. |
| Inspect not-hydrated ref | SourceMaterialRefRow/ref socket | Half-filled socket shows hydration attempt/result. | Read/hydration attempt. | Shows category, ref id, target kind if known, unresolved reason. | Ref-only is not full material. |
| Inspect unknown source health | Source/source group | Unknown health badge expands. | Reads known decisions/status rows; no common aggregate exists. | Shows available status rows and missing source-health query. | Unknown health is not success. |
| Open disconnected island | Local topology group | Island expands with accepted internal edges and separate candidate/inferred bridge residues. | ViewState expansion. | Shows members, anchors, known accepted edges, missing relation query if relevant. | Do not create fake bridge for coherence. |
| Change projection/filter | Projection lens | Layout/order/visibility changes; projection label updates. | ViewState/query state, not model change. | Shows input set, filter, method, projection limits. | Filtered out does not mean deleted or resolved. |
| Select projection health | Projection label/lens | Lens boundary and method badge become primary. | ViewState selection. | Shows method, input set, parameters/revision if meaningful, interpretation warnings. | Projection proximity/order is not semantic relation. |
| Collapse group | Source group, object group, island, relation bundle | Detail folds into count/status residue. | ViewState collapse. | Tooltip shows hidden counts: stale/unanchored/failed/candidate/inferred/unknown. | Collapse cannot erase warnings. |
| Zoom out | Semantic zoom/local topology | Bodies compress to glyphs; status residues remain. | ViewState zoom/projection. | Selected item keeps path back and glyph stack explanation. | Zoom cannot change meaning of edge/status without label. |
| Follow edge | CompositionEdge or real/candidate/inferred relation | Endpoint focus shifts with breadcrumb and followed-edge status. | ViewState navigation. | Shows previous object, followed connector, status, return path. | Following candidate/inferred edge does not accept it. |
| Drag object in canvas/local view | Object/placement | Shows placement pin/view-position marker, not relation line. | ViewState xy unless explicit membership/placement ActionRequest exists. | Explains view-only vs model-backed placement. | Spatial nearness is not semantic relation. |
| Add to local world | Object/local world | Placement pin appears with pending/base/future badge depending substrate. | Future ActionRequest if membership substrate exists; otherwise view-only/future. | Shows whether ContextMembership exists. | Local-world placement is not relation. |
| Create AI inference | Selected source/object/projection | New material appears dotted/inferred with AI mark. | Model/run/action output; accepted state requires separate decision. | Shows model/run/input/evidence, limits, accept/reject path. | AI output cannot enter as accepted silently. |
| Author human fact | Edit/assertion/annotation | Human mark plus candidate/accepted status depending path. | Edit/revision/action/proposal. | Shows author/time/source and status. | Human-authored is not verified. |
| Run simulation | Simulation source/object/output | Pending run skin; completed output uses simulation skin. | Run request/output artifact; accepted claims require separate status. | Shows inputs, parameters, seed, code/model version, outputs. | Simulation output is not observed fact. |
| Add personal annotation | Pin/comment/mark | Personal scope mark, noncanonical. | Future annotation state or view-local state. | Shows owner/scope and whether shared/accepted. | Personal pin is not accepted truth. |
| Resolve disagreement | Competing candidates/claims | Split glyph transitions through pending synthesis, then accepted synthesis only after decision. | ActionRequest/decision event. | Shows preserved sides, sources, accepted synthesis event. | Synthesis must not erase prior disagreement. |
| Failed action | Any pending model change | Pending ring becomes failed state; original status restored/preserved. | Failed ActionRequest/error event. | Shows action, target, error, retry path. | Failed action must not leave accepted visuals behind. |

## Interaction state families

### View-only

```text
hover
selection
focus
collapse
zoom
filter
projection selection
breadcrumb navigation
```

These use chrome treatment and must not look like model truth.

### Read/hydration-bound

```text
hydrate source refs
hydrate anchors
read trust rows
read projection explanation
read source status
read transcript operational state
```

These must show pending, partial, failed, or missing-query states. The view must
not pretend a partial read is complete trust.

### Model-changing

```text
promote DerivedUnit
edit ObjectContainer
accept/reject candidate
repair/reanchor stale source
add anchor
create relation
create local membership
run import/reparse
```

These must pass through pending and accepted/rejected/failed states.

### Future/unimplemented

```text
semantic RelationEdge creation
candidate relation substrate
ContextMembership/local-world membership
ViewState PState
LayerOverlay
code/Roam/Linear source imports
global source inventory
hydrated source/trust/topology bundles
source-range overlap query
```

These interactions must be absent, disabled, or explicitly labelled as future.

## What this makes visible

```text
view-only vs model-changing action
query/hydration availability
current vs future substrate
projection vs common truth
anchor precision and directionality
pending vs committed state
source-specific operational state
authority and agency
```

## What this keeps folded but recoverable

```text
full ActionRequest payload
full source/projection row payload
full hydration plan and failed reads
full transcript operational logs
full import/decision/audit history
full model/simulation run metadata
```

## What this must not imply

```text
hover means acceptance
selection means edit
hydration means verification
missing query means success
projection filter means deletion
drag means relation
pending means committed
AI run means accepted truth
simulation output means observed fact
```

## Failure modes

### Exactness illusion

Source-to-native hover looks exact despite missing range/hydration query.

### Optimistic acceptance

Pending accept/promote/reanchor appears as accepted model state before commit.

### Projection mutation

Changing projection/filter/layout appears to mutate common truth.

### Future interaction leak

UI exposes local-world, semantic relation, layer, or source-family actions as if
the current substrate supports them.

### Collapse laundering

Collapsed groups hide warnings or unknown states without residue.

## Acceptance checks

1. Every interaction is classifiable as view-only, read/hydration-bound,
   model-changing, source-specific operational, or future/unimplemented.
2. Source-to-native hover states expose exact/partial/unknown availability.
3. Model-changing actions never skip pending state.
4. Projection/filter/zoom never appears as canonical model mutation.
5. Missing query states are interactively explainable.
6. Future substrate interactions are not shown as current capabilities.
7. Collapsed/zoomed states preserve warning and unknown residues.

## Open questions

1. Should first prototype interactions deliberately disable all future-state
   controls, or show them as labelled unavailable substrate?
2. Should source-to-native hover be deferred until a range query exists, or
   ship with explicit target-to-source-only language?
3. Should the Trust Inspector load a best-effort multi-read bundle or wait for a
   dedicated hydrated query?
4. What interaction language makes "unknown because query missing" feel honest
   rather than broken?
5. Should accepted/rejected relation review exist before actual RelationEdge
   storage?

## Next recommended goal

Goal 5: run markdown and transcript scenarios through these interactions,
especially the cases where the substrate is partial, projected, operational, or
missing.

