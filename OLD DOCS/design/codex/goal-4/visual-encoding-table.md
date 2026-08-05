# Visual Encoding Table

Status: revised Goal 4 draft after reading completed Goals 1-3, 2026-06-09.

## Origin prompt

```text
Design the truth-state visual grammar for Softland: raw source, SourceArtifact,
ObjectContainer, DerivedUnit, graduated object, SourceAnchor, accepted edge,
candidate edge, inferred relation, stale anchor, unanchored object, projection,
local-world placement, AI inference, human-authored fact, simulation output,
etc.

Recheck if Goals 1-3 are done; if so, use them and redo.
```

## Goal

Provide the reusable state table for `TruthStateGlyph`, `SoftlandSourceMap`,
projection health, local topology, authority state, and missing-substrate
states.

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

This table defines visual states. It does not define the full screen, palette,
or final icon set.

## Core claim

The table must encode not only what exists, but what is implemented, projected,
operational, unknown, missing, or future. Otherwise the first beautiful view
will overstate the current substrate.

## Encoding table

| State | Meaning | Where it comes from in the model | Must be visible at overview? yes/no | Primary encoding | Secondary encoding | Interaction behavior | Tooltip/inspector language | What this must not imply |
|---|---|---|---|---|---|---|---|---|
| raw source | Preserved input material before native transformation. | Source raw text/body from source file, markdown source, transcript source line, code/text/etc before import. | yes | Source slab with source rail. | Source-family label, raw/excerpt texture, span highlight. | Hover/select reveals SourceArtifact/version and anchored native targets when available. | "Raw source. Preserved input material; not native object truth by itself." | Raw source is not automatically parsed, native, accepted, or complete. |
| SourceArtifact | Immutable captured source version. | `SourceArtifactRow`; markdown implemented per source, transcript implemented per observed source line. | yes | Source body with immutable version cap. | Source ref/hash/version label, source-family badge, capture status. | Select shows raw capture, ref, hash/version, import completion, anchors, projection rows. | "SourceArtifact. Captured source version that seeds native material through anchors." | SourceArtifact never becomes ObjectContainer. |
| SourceVersion/latest source | Version/freshness identity for a source ref. | `SourceVersionRow`, `$$source-latest-by-ref`. | yes in source inventory | Version cap, latest marker. | Hash/ref chip, stale/current glyph. | Select compares versions where available. | "Source version. This is the captured version used for import." | Latest source does not mean complete or error-free import. |
| implemented source family | A source family that currently writes common object-container import material. | Goal 2: markdown and transcript common paths. | yes | Normal source-family badge. | Base substrate badge. | Select reveals implemented rows and available reads. | "Implemented common import path." | This must not generalize to code/Roam/Linear. |
| future source family | A named source family not currently implemented as common object-container ingester. | Goal 2: code, Roam/DG, Linear, Softland-doc-specific imports absent. | yes if shown | Ghost source badge. | "future/not imported" label, disabled actions. | Select explains missing ingester/read model. | "Not implemented in current common substrate." | Future source names are not current data. |
| native ObjectContainer | Durable identity-bearing native object. | `ObjectContainerRow`; markdown document, transcript conversation/message/tool-call/tool-result. | yes | Solid identity shell. | Kind mark, base substrate badge, source stripe if anchored. | Selection persists across projections; inspector shows revisions, anchors, composition, audit. | "ObjectContainer. Durable native object." | Durable does not mean verified, public, or semantically connected. |
| DerivedUnit | Re-runnable source-derived unit without durable identity. | `DerivedUnitRow`; implemented for markdown blocks. | yes in source scope | Hollow/dashed shell. | Distiller mark, source stripe, no durable identity bead. | Select inspects source span and promotion path. | "DerivedUnit. Re-runnable and not durable until graduated." | Markdown blocks are not child containers before graduation. |
| graduated object | DerivedUnit promoted into ObjectContainer. | `UnitGraduationRow`, new `ObjectContainerRow`, `RevisionRow`, retained anchors. | yes | Solid shell with derived notch. | Source stripe, promotion/audit mark. | Select shows promotion event and derived-from source/unit. | "Graduated object. Durable now, with preserved derivation." | Graduation does not erase origin or drift risk. |
| Revision | Versioned content/state for a container. | `RevisionRow`, `$$revision-history-by-container`. | no broad overview; yes inspector/timeline | Stratum/ledger tick. | Author/event/time mark, current revision marker. | Select compares or reveals revision history. | "Revision. Versioned content for this object." | Revision is not a separate object unless materialized. |
| NativeIdentityClaim | Claim tying native object to source-native id. | `NativeIdentityClaimRow`. | no overview; yes trust inspector | Native-id badge. | Source-family/native-id reveal. | Select shows source-native id, claim origin, conflicts if any. | "Native identity claim." | Native id does not imply semantic relation. |
| SourceAnchor | Grounding link from source to native target/unit. | `SourceAnchorRow`; target-side reads implemented. | yes | Solid anchor tether or paired source/native highlight. | Source stripe/socket, exact span label, precision mark. | Hover either side highlights the other when hydrated. | "SourceAnchor. Grounding link to source material." | Anchor proves origin, not correctness. |
| exact source map | Bidirectional source/native mapping is hydrated and precise. | SourceAnchor plus hydrated target/source rows and usable range/span. | yes when active | Bright paired highlight and solid tether. | "exact" precision tag. | Hover native highlights source; hover source highlights native. | "Exact mapping available for this span/target." | Exact mapping still does not verify semantic truth. |
| partial source map | Mapping exists but is incomplete/coarse. | Anchor/span coverage partial, partial import, incomplete hydration. | yes | Segmented tether/bracket. | Coverage count/percent, partial label. | Select reveals known and unknown portions. | "Partial mapping. Some source/native relation is unresolved." | Partial must not look exact. |
| target-to-source only | Native target has anchors, but reverse source range lookup is not directly available. | Goal 2: target anchors exist; source-range overlap query missing. | yes in source/native hover contexts | One-way tether arrow from target to source. | Reverse-lookup pending/unknown badge. | Native hover can reveal source; raw-source hover may show resolving/unknown. | "Target has source anchors; source-to-target lookup may need hydration." | Do not imply arbitrary raw-source hover is exact. |
| stale anchor | Existing anchor no longer matches source version/span/hash. | Drift/staleness detection over SourceAnchor and SourceVersion/hash. | yes | Broken tether. | Drift/hash mismatch mark, compare action. | Select compares old/current source and repair options. | "Stale anchor. Grounding exists but source has drifted." | Stale must not look healthy/current. |
| unanchored object | Native object has no SourceAnchor. | ObjectContainer/DerivedUnit with no anchor row, manual/unavailable/lost source. | yes | Empty anchor socket. | Warning label and reason if known. | Select explains no anchor and possible link/add-anchor action. | "Unanchored object." | Unanchored is not invalid, but lower provenance trust. |
| source span with no native target | Source material captured but no native unit/container maps to it. | SourceArtifact span plus absent source-material/anchor target, parse failure, partial import. | yes | Source-only socket on span. | "no native target" residue, parse/coverage mark. | Select explains whether unparsed, ignored, failed, or unknown. | "Captured source with no native target." | No target must not look like successful clean omission. |
| accepted CompositionEdge | Canonical structural edge such as parent/child, contains, follows, produced, order. | `CompositionEdgeRow`; implemented for markdown structure and transcript structure. | yes | Solid bracket/rail/containment/order connector. | Edge type label, order chip, base substrate badge. | Select edge to inspect type, endpoints, source/import/audit. | "Accepted CompositionEdge. Structural relation." | Composition is not semantic support/contradiction. |
| accepted RelationEdge | Accepted semantic edge such as supports/contradicts/depends-on. | Future common relation substrate; no current `RelationEdge` rows found in Goal 2. | yes only when substrate exists; otherwise future mark | Solid typed semantic connector. | Relation label, audit/accepted badge. | Select shows provenance and accepted decision. | "Accepted RelationEdge. Not currently implemented in the object-container substrate unless a future relation row exists." | Must not be shown as current truth today. |
| candidate relation | Proposed relation awaiting decision. | Future candidate/situate layer; no current candidate relation substrate found. | yes when proposals exist | Dashed typed connector. | Proposal source mark, accept/reject handles. | Select reveals evidence and action path. | "Candidate relation. Proposed, not accepted." | Candidate must not affect accepted topology. |
| inferred relation | Relation proposed by AI/heuristic/model/projection/situate. | Future inference/situate output or AI/model run; not accepted relation. | yes when visible | Dotted typed connector. | AI/policy/procedure mark, input/evidence reveal. | Select reveals inference source and accept/reject path. | "Inferred relation. Computed/proposed, not accepted." | Inferred is not accepted or verified. |
| rejected relation | Proposal explicitly rejected but recoverable. | Rejected decision/audit event or future relation review history. | no by default; yes audit/counts | Recessed/struck connector. | Rejection mark, muted endpoints. | Select in audit mode shows reason and history. | "Rejected relation. Not in accepted topology." | Rejected must not vanish from history. |
| disconnected island | Object/group in scope without accepted connection to current local topology. | Query over visible scope and accepted CompositionEdges/future RelationEdges. | yes | Island shoreline boundary. | Count badge, candidate/inferred bridge residue. | Select opens members, anchors, possible bridges. | "Disconnected island. Present, not connected by accepted edges." | Do not auto-bridge by projection proximity. |
| projection | View/read model/layout over material. | Interpreter/view, source-specific projection/index rows, query result, layout algorithm. | yes | Lens boundary/watermark. | Projection name, input set, method label. | Select projection label for explanation and limitations. | "Projection. It arranges or indexes material; it does not create accepted truth." | Projection is not canonical model. |
| markdown outline projection | Source-specific outline read model for markdown. | `OutlineNodeRow`, `$$outline-by-document`. | yes in outline | Lens row plus markdown badge. | Target-id/common-row coverage mark. | Select reveals represented DerivedUnit/ObjectContainer and projection row. | "Markdown outline projection." | Outline row is not a native object row. |
| transcript projection/index | Transcript conversation/tool/audit/source-line read model. | Transcript projection/index/status rows from Goal 2. | yes in transcript views | Lens row plus transcript badge. | Target-id/common-row coverage mark; ops state if relevant. | Select reveals common target if any and source-specific fields. | "Transcript projection/index." | Projection/index rows are not canonical object truth. |
| operational state | Harvest/watch/run/file-offset/source-line operational progress. | `TranscriptRunRow`, `TranscriptFileOffsetRow`, `TranscriptSourceLineStatusRow`, request/decision/import rows. | yes when source completeness matters | Run/status strip. | Ops badge, progress/failure/pending mark. | Select reveals run, offset, completion, failure details. | "Operational state. Source-specific progress/status." | Transcript ops state does not generalize to all source families. |
| partial parse | Some source material was not materialized/interpreted. | Parse coverage, decisions, transcript source-line status, import completion gaps. | yes | Segmented coverage bar/shell. | Warning count, unparsed span reveal. | Select shows parsed/unparsed source regions. | "Partial parse." | Partial success is not complete success. |
| parse failure | Source region/artifact failed to parse. | Parse-error status/decision/source-line status; no native containers for failed transcript line. | yes | Blocked source segment. | Error mark, failure scope. | Select shows error, source span, retry/reparse path if available. | "Parse failure. No native material was produced for this region." | Failure must not render as empty success. |
| missing query | The view cannot answer because read/query contract is absent. | Goal 2 missing query list: global source inventory, hydrated bundles, range overlap, source health, relation state, etc. | yes where it affects trust | Hollow unresolved data socket. | Query-name label, "unknown" badge. | Select explains missing query and safe fallback. | "Unknown because current query does not exist." | Missing query must not become inferred success. |
| not hydrated | Refs exist, full rows are not hydrated yet. | `SourceMaterialRefRow` refs without target rows; point reads pending/missing. | yes in loading/trust contexts | Half-filled socket/shell. | "ref only" badge, loading or unresolved mark. | Select/hydrate attempts fetch target rows. | "Reference known; full material not hydrated." | Ref is not full object/anchor/edge truth. |
| unknown health | Source/object health cannot be aggregated from current rows. | No common source health query; partial reads only. | yes | Neutral unknown badge, not green/success. | "health unknown" label. | Select shows which reads exist and which are missing. | "Health unknown in current substrate." | Unknown is not success. |
| view state | Selection, hover, collapse, zoom, filter, xy, transient focus. | Client/view state; no common `ViewState` PState found. | no except orientation | Subtle chrome, not model shell. | View badge, temporary focus ring/caret/filter chip. | Changes view only unless explicit ActionRequest. | "View state. Presentation state, not model truth." | ViewState must not look canonical. |
| local-world placement | Object situated in a local world/board/thread/region. | Future `ContextMembership`/placement; not found in current substrate. | yes when shown | Placement pin distinct from relation line. | Local-world label, future/base badge as appropriate. | Select distinguishes model-backed membership from view-only placement. | "Placement. Current substrate support must be verified." | Placement is not semantic relation. |
| LayerOverlay / draft layer | Base/active/candidate/draft overlay status. | Future layer/overlay topology; not found in current substrate. | yes if shown | Layer chip with ghost/future badge. | Draft/base label, pending/review mark. | Select explains unavailable or future overlay semantics. | "Layer/overlay status." | Draft must not look accepted base truth. |
| AI inference | Content/relation/classification/summary proposed by a model. | Agent/model output or future inference rows; may create candidates. | yes | Dotted inference skin. | AI mark, model/run/input reveal. | Select shows inputs, model/run, evidence, accept/reject path. | "AI inference. Proposed by model." | AI does not mean accepted, grounded, or verified. |
| human-authored fact | Assertion authored by human or imported human-authored source. | Revision/source author/audit actor metadata where available. | yes when mixed with other agency states | Human/author mark. | Author/time/source label. | Select shows author, revision, source, accepted/candidate status. | "Human-authored assertion." | Human-authored is not externally verified. |
| simulation output | Output from parameterized run/model/computation. | Future simulation artifact/run rows or imported output objects. | yes when mixed with facts | Run/output skin. | Parameter/seed/input/version badges. | Select reveals inputs, parameters, seed, code/model version, output links. | "Simulation output." | Simulation is not observed fact. |
| import-policy output | Object/unit/relation generated by import/distiller policy. | Distiller id/version, import material, `DerivedUnitRow`, projection hints. | yes | Policy/distiller mark. | Distiller version, source-map channel. | Select reveals policy, source span, output row. | "Produced by import/distiller policy." | Policy output was not necessarily intentionally authored. |
| personal annotation | Personal note/pin/mark not accepted as shared truth. | Future annotation/collaboration state; Goal 3 authority gap. | yes if shown | Personal scope mark. | Author-only badge, noncanonical substrate badge. | Select shows owner/scope and promote/share path if allowed. | "Personal annotation." | Personal mark is not accepted model truth. |
| team annotation | Shared annotation not necessarily accepted canonical truth. | Future collaboration state. | yes if shown | Shared annotation mark. | Team scope badge, status glyph. | Select shows authors, scope, review state. | "Team annotation." | Shared visibility is not accepted truth. |
| accepted decision/audit event | Accepted/rejected/promotion/edit/import decision that changes status. | `ObjectContainerDecisionRow`, `ObjectContainerEventRow`, request/audit rows. | no broad; yes trace/inspector | Audit stamp or ledger tick. | Actor/time/before-after status. | Select reveals decision payload and affected rows. | "Audit event. Decision that changed model state." | Decision records acceptance, not universal truth. |
| pending ActionRequest | Requested model change not committed yet. | In-flight request/action state. | yes near action | Pending ring/stroke around current nonaccepted state. | Action label, progress mark. | Select shows requested operation and target. | "Pending action. Not model truth yet." | Pending must not render as accepted. |
| unresolved disagreement | Preserved conflict between claims/relations/interpretations/agents. | Future disagreement/candidate records; Goal 3 authority gap. | yes when it affects understanding | Split/forked glyph. | Side count, source/agent labels, synthesis status. | Select expands positions and grounds. | "Unresolved disagreement." | Disagreement must not be summarized into consensus. |

## What this makes visible

```text
implemented vs future substrate
common base truth vs source-specific projection
raw/source/native/derived/graduated material
source-map precision and query availability
composition vs semantic relation
accepted/candidate/inferred/rejected status
authority/agency/disagreement
missing query and unknown health
```

## What this keeps folded but recoverable

```text
full source body
full row payloads
full projection inputs
full operational logs
full audit/decision history
full model/run/simulation metadata
full missing-query explanation
```

## What this must not imply

```text
that every architecture noun has current storage
that projection rows are common truth
that unknown means successful
that SourceAnchor means semantic correctness
that RelationEdge exists today
that ViewState/ContextMembership/LayerOverlay exist today
```

## Failure modes

```text
table omits missing/future state
source-specific projection uses object shell
semantic relation styling appears over composition/proximity
source-to-target hover is shown exact before query support exists
unknown health uses success styling
authority and acceptance collapse into one mark
```

## Acceptance checks

1. Every visible item in a future prototype can be mapped to exactly one
   material body and one substrate badge.
2. States marked future/unimplemented cannot use current/base styling.
3. Markdown block `DerivedUnitRow` and transcript message `ObjectContainerRow`
   look different.
4. Projection rows never look like object rows.
5. Missing query, not hydrated, and unknown health are visually distinct from
   success.
6. RelationEdge styling is gated behind actual relation substrate.

## Open questions

1. Should "not hydrated" and "missing query" be separate user-facing states or
   one expandable unknown state?
2. Should the first prototype show future source families at all, or omit them
   entirely?
3. Should `TruthStateGlyph` include authority by default, or only when mixed
   human/AI/simulation material appears?
4. Should source-map precision labels be textual in the first prototype or
   glyph-only with inspector copy?

## Next recommended goal

Goal 5: run concrete markdown and transcript scenarios against this table.

