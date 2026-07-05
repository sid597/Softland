# Borrow And Reject Table

Status: draft research sweep, 2026-06-09.

## Origin Prompt

```text
Execute Goal 3 only. Create the required files. For every reference, say what
Softland should borrow, what it should reject, what primitive it could inspire,
and what failure mode it warns about.
```

## Goal

Make hard calls from the precedent atlas so the research can guide view design
instead of remaining a neutral list of inspirations.

## Inputs Read

- `docs/current-mental-model/design/codex/orientation.md`
- `docs/current-mental-model/design/codex/goal-3/description.md`
- `docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md`
- `docs/vision/epistemic-framework.md`
- `docs/current-mental-model/design/codex/goal-1/description.md`
- `docs/current-mental-model/design/codex/goal-2/description.md`
- External sources cited in `precedent-atlas.md` and `annotated-bibliography.md`

## Scope

This table prioritizes the first family of Softland views, especially a
Source-to-World View with source inventory, native object browser, trust
inspector, local topology, and provenance/history. It does not specify UI
layout or implementation.

## Core Claim

Borrow the primitives that make transformation inspectable and local worlds
navigable. Reject anything that makes projected structure feel more accepted,
global, certain, or complete than the underlying model can prove.

## Borrow Now

| Call | References | Borrow | Primitive | Failure mode it prevents | First prototype test |
|---|---|---|---|---|---|
| Bidirectional source-to-native mapping | ECMA-426 Source Maps, Chrome DevTools, Principal Design Research | Native object to raw span and raw span to native object | `SoftlandSourceMap` | Hidden severance between source and transformed object | Hover native field highlights source span; hover source span highlights native object |
| Scoped local topology | Obsidian Local Graph, DataHub lineage, dbt lineage, Shneiderman | Center one object and expand by typed edges/depth | `LocalTopologyLens` | Global graph seduction | User can answer "why connected?" without opening all nodes |
| Truth-state visual grammar | MacEachren uncertainty visualization, Principal Design Research | Redundant encodings for accepted, candidate, inferred, partial, stale, failed, disconnected, unanchored | `TruthStateGlyph` | Uncertainty masquerading as truth | States remain distinguishable without color or hover |
| Trust inspector | Chrome DevTools, DataHub, Figma Dev Mode | Selected object shows source, mappings, revisions, relations, warnings, actions | `TrustInspector` | Clean UI hiding transformation risk | User can diagnose bad source, bad parse, bad relation, or stale anchor |
| Object identity across projections | Munzner, Pad++, Blender, Figma | Same object stays selected and recognizable across outline, graph, timeline, inspector | `ScaleStableObject` | View switching creates duplicate-feeling objects | Selection persists across view changes with stable label/status |
| Contact-sheet comparison for candidates | Magnum Contact Sheets, Lightroom Survey/Compare, Distill | Show accepted/rejected/candidate transformations together | `TransformationContactSheet` | Final artifact hides the edit path | Reviewer sees why one candidate became accepted |
| You-are-here stack | Legible London, Kevin Lynch, Google Earth | Persistent context: local world, source, selected object, projection, return path | `YouAreHereStack` | Private reconstruction after deep inspection | User can return from source span to topology in one action |
| Transformation trace | Perfetto, OpenTelemetry, Jupyter, Observable | Event/trace lane for import, parse, derive, relate, accept/reject, fail | `TransformationTrace` | Linear summary hides actual process | Selecting an event reveals changed rows/objects and downstream effects |
| Projection health | t-SNE Distill, UMAP docs, Munzner | Show method, parameters, input set, stability, and interpretation warnings | `ProjectionHealthPanel` | Projection laundering | User knows proximity is projection, not accepted relation |
| Folded but recoverable compression | Shneiderman, Pad++, Space-Scale Diagrams, Legible London | Fold details with counts, reasons, and direct reveal paths | `RecoverableFold` | Compression becomes deletion | Folded neighborhoods disclose count/type/status and can expand locally |

## Borrow Later

| Call | References | Borrow later | Why later | Primitive | Risk if pulled forward |
|---|---|---|---|---|---|
| Full semantic zoom model | Pad++, Space-Scale Diagrams, Google Earth | Continuous scale bands across corpus, domain, source, object, field, span | Needs stable truth-state grammar and object identity first | `ZoomContract` | Flashy zoom before trust |
| User-authored view patches | Ink and Switch Malleable Software, Dynamicland, Folk | Let users reshape views/workflows locally with lineage | Needs base view contract and permission/version model | `ViewPatch` | Malleability creates private unreviewable worlds |
| Guided replay paths | Prezi, Distill, reasoning trails | Authored traversal through local world for handoff | Needs trail persistence and source-to-native anchors | `GuidedTrailPath` | Choreography replaces evidence |
| Epistemic minimap | Game minimaps, Legible London, Google Earth | Small local context surface for orientation | Needs actual local-world topology and landmarks | `LocalWorldMinimap` | Persistent clutter steals attention from source/trust |
| Workflow pages | DaVinci Resolve, Blender, Ableton | Stage-specific professional surfaces | Needs first prototype validation | `WorkflowPage` | Heavy suite before core trust problem is solved |
| Executable narrative artifact | Jupyter, Observable, Distill, Ableton | Runnable explanation with source-grounded outputs | Needs artifact slot contract and freshness model | `ExecutableNarrative` | Stale outputs look current |
| Region-object authoring | Folk, Potluck, Dynamicland | Let selected spans/regions graduate into objects | Needs clear candidate/accepted graduation states | `RegionObject` | Easy extraction feels lossless |

## Study More

| Question | References | Why study more | Needed evidence |
|---|---|---|---|
| How many truth states should ship in the first visual grammar? | MacEachren, DataHub, Principal Design Research | Too many states can overwhelm; too few can lie | Prototype recognition tests for accepted/candidate/partial/stale/unanchored |
| What is the right source-to-native granularity? | Source Maps, Chrome DevTools, transcript/markdown ingesters | Field-level mapping may be expensive; document-level is too coarse | Current substrate map plus hover/highlight prototype |
| How should semantic time appear if the log is a DAG? | Perfetto, OpenTelemetry, Softland epistemic framework | Linear timelines are usable but causality may be braided | Design exploration of trace lanes plus causal links |
| How do collaborative annotations avoid becoming false truth? | Figma, DataHub manual lineage, Zelda pins | User marks, manual edges, and accepted model state must not blur | Visual grammar for owner, authority, and status |
| Can contact-sheet views scale beyond small candidate sets? | Magnum Contact Sheets, Lightroom, Munzner | Candidate comparison is powerful but can overload | Prototype with 3, 8, and 30 candidates |
| How should projection health be explained without scaring users away? | t-SNE Distill, UMAP, Munzner | Projection caveats need clarity, not academic warning walls | Copy and interaction tests for "projection, not truth" |

## Reject For First Prototype

| Reject | References warning against it | Why reject now | Safer substitute |
|---|---|---|---|
| Global graph as homepage | Obsidian, dbt, DataHub, Principal Design Research | Looks visionary but does not answer source-to-native trust | Source inventory plus selected-object local topology |
| Infinite canvas as main product metaphor | Pad++, Figma, Dynamicland | Spatial freedom before identity/provenance creates clutter | Bounded local world with stable entry and way back |
| Full workflow-suite navigation | DaVinci Resolve, Blender, Ableton | Professional pages only work after stable substrate and jobs | Three epistemic jobs: Source World, Native World, Trust World |
| Manual lineage editing in first trust path | DataHub manual lineage caveats | It can conflict with generated truth and obscure ownership | Candidate relation layer with review/accept actions |
| User-programmable view patches | Ink and Switch, Folk, Dynamicland | Malleability before lineage is dangerous | Read-only projection controls plus recorded probes |
| Embedding map as default orientation | t-SNE Distill, UMAP docs | Users will overread clusters and distance | Typed local topology and projection health panel |
| Cinematic zoom/path transitions | Prezi, Google Earth | Motion can imply understanding without evidence | Instant or restrained transitions with persistent breadcrumbs |

## Reject As Misleading

| Misleading pattern | Why it is misleading for Softland | Failure mode | Replacement law |
|---|---|---|---|
| "Clean means trustworthy" | Clean organization can hide context loss | Persuasive hallucination engine | The map must not lie |
| "All links are relations" | Links, candidates, inferred edges, accepted edges, and source anchors differ | Relation laundering | Projection must not invent accepted relationships |
| "Stored provenance is enough" | Provenance that cannot be reached in the interaction does not reduce private reconstruction | Hidden severance | Transformation must be inspectable |
| "Zoom is semantic by default" | Zoom may only resize marks while changing meaning invisibly | Semantic discontinuity | Identity, provenance, and relation must survive scale |
| "Collaboration equals shared understanding" | Presence and comments do not prove shared truth | Social confidence over epistemic calibration | Calibration is first-class |
| "Malleability equals agency" | Users can create chaos without lineage, review, or handoff | Unreviewable private worlds | Renovation must be traceable |
| "AI summary is a view" | Summary can erase source, disagreement, uncertainty, and process | Compression by severing | Compress by folding, not erasing |

## What This Makes Visible

- Which precedents are allowed to shape first-prototype primitives.
- Which seductive references are intentionally deferred or rejected.
- Which rejection is due to sequencing rather than permanent dismissal.

## What This Keeps Folded But Recoverable

- Detailed source notes stay in `precedent-atlas.md`.
- Bibliographic metadata stays in `annotated-bibliography.md`.
- Unresolved domains stay in `research-gaps.md`.

## What This Must Not Imply

- "Reject for first prototype" does not mean "never."
- "Borrow now" does not mean "copy surface form."
- "Study more" should not block a narrow Source-to-World prototype unless the missing question affects truth, provenance, or pacing.

## Failure Modes

- Borrow creep: later references sneak into the first prototype without earning their place.
- Category collapse: local topology, semantic zoom, lineage, and trail replay become one overloaded screen.
- Sequencing error: malleability and global maps arrive before source-to-native trust.
- False compromise: everything becomes a toggle instead of a hard product call.

## Acceptance Checks

- A Goal 6 first view spec can pick from "Borrow now" without rereading the entire atlas.
- Every "Borrow now" item has a primitive name and a first-prototype test.
- Every "Reject" item states what safer substitute should be used instead.
- The table preserves the project's hard rule: trust before topology spectacle.

## Open Questions

- Which "Borrow now" primitive is the absolute first prototype: `SoftlandSourceMap`, `TrustInspector`, or `LocalTopologyLens`?
- Should `ProjectionHealthPanel` be included in the first prototype if embeddings are not yet present?
- Should user pins be blocked entirely until the status grammar exists, or allowed as clearly personal annotations?

## Next Recommended Goal

Goal 4: truth-state visual grammar. The borrow/reject table makes `TruthStateGlyph` a first-prototype dependency; it should be specified before a buildable Source-to-World View.
