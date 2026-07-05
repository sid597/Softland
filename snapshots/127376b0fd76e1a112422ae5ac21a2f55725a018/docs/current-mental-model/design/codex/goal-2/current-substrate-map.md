# Current Substrate Map

Status: draft, code-grounded on 2026-06-09.

## Origin Prompt

> Continue the Softland design/view research track. Read the Goal 2 docs and
> minimum substrate files. Execute Goal 2 only. Create the required files inside
> `docs/current-mental-model/design/codex/goal-2/`. I want a current substrate
> map for views: what the object-container kernel and ingesters can actually
> expose, what is common base truth, what is source-specific projection/index
> state, what read models exist, and what a view must not imply yet. Stay
> factual and code-grounded. Do not design screens yet.

## Goal

Map what the current Softland substrate can expose to the view layer without
inventing product state that the kernel does not yet materialize.

## Inputs Read

Required:

- `docs/current-mental-model/design/codex/orientation.md`
- `docs/current-mental-model/design/codex/goal-2/description.md`
- `docs/current-mental-model/architecture/object-container-spec.md`
- `docs/current-mental-model/architecture/object-container-ingester-contract.md`
- `docs/current-mental-model/architecture/object-container-reviewer-world-model.md`
- `src/app/server/rama/object_container.clj`
- `test/app/server/rama/object_container_test.clj`

Additional substrate files inspected because the required files pointed at
source-specific ingester behavior:

- `src/app/server/rama/object_container/markdown_adapter.clj`
- `src/app/server/rama/object_container/transcript_adapter.clj`
- `src/app/server/rama/object_container/runtime.clj`
- `src/app/server/rama/object_container/transcript_identity.clj`
- `src/app/server/rama/dogfood/transcript.clj`
- `src/app/server/rama/dogfood/transcript_ingest.clj`
- `docs/current-mental-model/build/object-container/IMPLEMENTATION_VALIDATION.md`
- `docs/current-mental-model/build/object-container-common-infra/IMPLEMENTATION_VALIDATION.md`

## Files Inspected

- `docs/current-mental-model/design/codex/orientation.md`
- `docs/current-mental-model/design/codex/goal-2/description.md`
- `docs/current-mental-model/architecture/object-container-spec.md`
- `docs/current-mental-model/architecture/object-container-ingester-contract.md`
- `docs/current-mental-model/architecture/object-container-reviewer-world-model.md`
- `src/app/server/rama/object_container.clj`
- `src/app/server/rama/object_container/markdown_adapter.clj`
- `src/app/server/rama/object_container/transcript_adapter.clj`
- `src/app/server/rama/object_container/runtime.clj`
- `src/app/server/rama/object_container/transcript_identity.clj`
- `src/app/server/rama/dogfood/transcript.clj`
- `src/app/server/rama/dogfood/transcript_ingest.clj`
- `test/app/server/rama/object_container_test.clj`
- `docs/current-mental-model/build/object-container/IMPLEMENTATION_VALIDATION.md`
- `docs/current-mental-model/build/object-container-common-infra/IMPLEMENTATION_VALIDATION.md`

## Scope

This is an engineering grounding artifact for future view design. It does not
design screens, visual language, layouts, navigation, or interaction flows.

It treats current code as stronger evidence than older validation notes where
they disagree.

## Core Claim

The current object-container substrate can expose a real common base for
markdown and transcript imports, but only for the rows that flow through
`:object-container/import-material` and `:object/edit`. Markdown and transcript
have different source-specific projections on top of that base. Code, Roam/DG,
Linear, and Softland-doc-specific import are not implemented as common
object-container ingesters yet.

Views can truthfully show:

- captured source artifacts
- native object containers
- revisions and current content
- derived markdown units before graduation
- source anchors
- composition edges
- accepted/rejected request decisions
- markdown outline projection
- transcript conversation/tool/audit/source-line projections
- transcript run and file-offset operational state

Views must not imply:

- accepted semantic relations
- global cross-source topology
- common code/Roam/Linear ingesters
- durable markdown block containers before graduation
- ViewState, ContextMembership, LayerOverlay, or RelationEdge storage
- source-specific transcript or outline projections as canonical object truth

## Implemented Source Paths

### Markdown

Current path:

```text
markdown_adapter/source-ingest-request
  -> :object-container/import-material request
  -> *object-container-requests-depot
  -> object-container-module
```

The markdown adapter currently materializes:

- one `SourceArtifactRow`
- one document `ObjectContainerRow`
- one document `RevisionRow`
- one `SourceVersionRow`
- one document `SourceAnchorRow`
- one `DerivedUnitRow` per markdown block produced by `markdown-block-v0`
- one `SourceAnchorRow` per derived unit
- one `CompositionEdgeRow` per derived unit placement
- `OutlineNodeRow` projection hints with `:projection-kind :markdown-outline`

Important view consequence:

Markdown headings, paragraphs, and list items are `DerivedUnitRow`s, not durable
child `ObjectContainerRow`s, until an edit/touch graduates them through
`:object/edit`.

### Transcript

Current common path:

```text
dogfood/transcript harvest/watch acquisition
  -> transcript_adapter/transcript-observation-import-request
  -> :object-container/import-material request
  -> *object-container-requests-depot
  -> object-container-module
```

The transcript adapter currently materializes each source-line observation as:

- one `SourceArtifactRow` for the source line
- one `SourceVersionRow`
- zero or more `ObjectContainerRow`s:
  - `:chat-conversation`
  - `:chat-message`
  - `:tool-call`
  - `:tool-result`
- matching `RevisionRow`s
- matching `SourceAnchorRow`s with byte offsets and source line hash in
  `:block-path`
- `CompositionEdgeRow`s for conversation/message/tool structure
- transcript projection hints:
  - `TranscriptConversationProjectionRow`
  - `TranscriptToolCallIndexRow`
  - `TranscriptAuditEntryRow`
  - `TranscriptLastMessageRow`
- source-line status hints:
  - `TranscriptSourceLineStatusRow`

Transcript parse errors produce source/projection/status rows but no containers,
revisions, anchors, or composition edges for that line.

### Transcript Operational Control

Current path:

```text
object-container-transcript-ops-module
  *transcript-control-depot
  *transcript-file-state-depot
```

This module stores source-specific operational state:

- `TranscriptRunRow` in `$$transcript-runs`
- `TranscriptFileOffsetRow` in `$$transcript-file-offsets`
- observed file source lines in `$$transcript-file-source-lines-by-file`

It mirrors completed source-line rows from the common object-container module
and advances file offsets only when common source-line completion has landed.

### Legacy Transcript Prototype

`src/app/server/rama/dogfood/transcript_ingest.clj` still exists and declares a
transcript-local object-container-shaped module:

- `$$containers-by-id`
- `$$source-artifacts`
- `$$source-anchors-by-container`
- `$$composition-edges-by-parent`
- `$$conversation-projection`
- `$$tool-calls-by-name`
- `$$audit-entries`

However, its public harvest/watch helpers delegate to `dogfood/transcript.clj`
when the runtime has object-container depots. For Goal 2, the common
object-container runtime is the current view substrate; the older module is a
source-specific prototype/fallback, not the common base truth.

### Not Implemented As Common Object-Container Ingesters

No current common object-container ingester was found for:

- code artifacts or code symbols
- Roam/DG graph imports
- Linear imports
- Softland docs as a distinct source family

Softland docs can be imported as ordinary markdown if the markdown adapter is
called, but there is no Softland-doc-specific object-container source type,
index, or projection.

## Common Object-Container Rows

These rows belong to the common object-container substrate in
`src/app/server/rama/object_container.clj`.

### Request, Decision, Event Audit

- `ObjectContainerRequestRow`
- `ObjectContainerDecisionRow`
- `ObjectContainerEventRow`

PStates:

- `$$requests-by-audit-id`
- `$$decisions-by-audit-id`
- `$$decisions-by-idempotency`
- `$$events-by-id`

These are common lifecycle/audit facts. They are not presentation state.

### Source Capture And Import Completion

- `SourceArtifactRow`
- `SourceVersionRow`
- `SourceIngestCompletionRow`
- `ImportCompletionRow`

PStates:

- `$$source-artifacts-by-id`
- `$$source-versions-by-ref`
- `$$source-latest-by-ref`
- `$$source-ingest-completions-by-ref`
- `$$import-completions-by-key`

These rows let a view answer some source/version/completion questions, but not
yet enumerate all source refs globally.

### Native Object Material

- `ObjectContainerRow`
- `RevisionRow`
- `DerivedUnitRow`
- `UnitGraduationRow`
- `SourceAnchorRow`
- `CompositionEdgeRow`
- `SourceMaterialRefRow`
- `NativeIdentityClaimRow`
- `EditOrderRow`

PStates:

- `$$containers-by-id`
- `$$revisions-by-id`
- `$$revision-history-by-container`
- `$$derived-units-by-id`
- `$$unit-graduations-by-id`
- `$$source-anchors-by-target`
- `$$composition-children-by-parent`
- `$$composition-parent-by-child`
- `$$source-containers-by-source`
- `$$source-derived-units-by-source`
- `$$source-anchors-by-source`
- `$$source-edges-by-source`
- `$$native-identity-claims-by-container`
- `$$edit-order-by-target`

Important distinction:

`SourceMaterialRefRow` indexes enumerate material refs by source. They do not
hydrate the full target rows. A view that needs full rows must point-read those
targets or use future hydrated queries.

## Source-Specific Rows

### Markdown Projection

- `OutlineNodeRow`

PState:

- `$$outline-by-document`

This is a projection/read model. It is useful for a local outline view, but it
must not be treated as the canonical object store.

### Transcript Projections And Indexes

- `TranscriptConversationProjectionRow`
- `TranscriptToolCallIndexRow`
- `TranscriptAuditEntryRow`
- `TranscriptLastMessageRow`
- `TranscriptSourceLineStatusRow`

PStates:

- `$$transcript-conversation-projection`
- `$$transcript-tool-calls-by-name`
- `$$transcript-audit-by-request`
- `$$transcript-last-message-by-conversation`
- `$$transcript-source-lines-by-file`

These rows are transcript-specific projections and indexes over common import
material. They are not canonical object truth.

### Transcript Operational State

- `TranscriptRunRow`
- `TranscriptFileOffsetRow`
- observed `TranscriptSourceLineStatusRow`s in the ops module

PStates:

- `$$transcript-runs`
- `$$transcript-file-offsets`
- `$$transcript-file-source-lines-by-file`

These are operational state for harvest/watch progress, resume safety, and
partial/failure display.

## Existing Projections And Read Models

Runtime wrapper: `src/app/server/rama/object_container/runtime.clj`.

### Source Reads

- `read-source`
- `read-latest-source-by-ref`
- `read-source-by-ref-version`
- `read-import-completion`
- `read-source-containers`
- `read-source-derived-units`
- `read-source-anchor-refs`
- `read-source-edge-refs`
- `read-common-material-for-source`

View caveat:

`read-common-material-for-source` returns refs grouped by category, not hydrated
container/unit/anchor/edge rows.

### Object And Revision Reads

- `read-container`
- `read-current-revision`
- `read-revision-history`
- `read-unit`
- `read-native-identity-claim`

View caveat:

There is no single "object trust inspector" query that hydrates container,
current revision, source anchors, source artifact, import completion, event, and
decision trail together.

### Anchor And Composition Reads

- `read-source-anchors` by target id
- `read-composition-children` by parent slot id
- `read-composition-parents` by child slot id

View caveat:

There is no reverse source-range query such as "given source id and byte range,
which targets overlap this span?"

### Markdown Reads

- `read-outline` by document id with cursor and limit

View caveat:

The outline projection may point to `:derived-unit` or `:object-container`
targets. A view must show the distinction, especially after graduation.

### Transcript Reads

- `read-transcript-conversation-projection`
- `read-transcript-tool-calls-by-name`
- `read-transcript-audit-entries`
- `read-transcript-last-message`
- `read-transcript-source-line`
- `read-transcript-source-lines`
- `read-transcript-run`
- `read-transcript-file-offset`
- `read-transcript-file-source-lines`

View caveat:

Transcript source-line status and run/file-offset state are transcript-specific
operational read models. They are the right data for partial/failure display,
but not general object truth.

## Known Architectural Caveats

1. `RelationEdge` is named in the architecture spec but not implemented in the
   current object-container module. Current graph structure is
   `CompositionEdgeRow` only.

2. `ContextMembership`, `LayerOverlay`, and `ViewState` are named in the spec
   but have no current PStates in `object_container.clj`.

3. Container visibility currently materializes as `:private` in the implemented
   markdown/transcript rows inspected. The `:public-material` promotion path is
   a spec concept, not a current read model.

4. Markdown block containers are not born durable. They start as
   `DerivedUnitRow`s. Editing a derived unit writes a `UnitGraduationRow`, an
   `ObjectContainerRow`, a `RevisionRow`, a copied source anchor, and updated
   outline/composition rows.

5. Transcript observations create native containers immediately because the
   adapter can derive deterministic conversation/message/tool identities from
   source-native ids and source-line keys. Parse-error observations create
   projection/status evidence but no native containers.

6. The common source material query enumerates category refs, not complete
   material rows. Future views should not assume a one-call hydrated source
   bundle exists.

7. The old transcript-local module remains in the repository. It should not be
   mistaken for the shared object-container substrate when running through the
   current object-container runtime.

8. Validation artifacts record prior and partial implementation concerns. The
   current code and tests show many repairs, including markdown block parsing
   and transcript common import, but this Goal 2 pass did not rerun the full
   test suite.

## What A First View Must Not Imply Yet

- Do not imply that markdown headings/paragraphs/list items are durable
  containers before graduation.
- Do not imply that transcript conversation projection rows are canonical
  object rows.
- Do not imply that composition edges are semantic relations such as supports,
  contradicts, depends-on, or cites. They are structural composition edges.
- Do not imply a global source inventory, global object search, or cross-source
  topology unless a future query is added.
- Do not imply source-specific indexes are common base truth.
- Do not imply code, Roam/DG, Linear, or Softland-doc-specific ingestion exists.
- Do not imply accepted candidate relations, layer overlays, or view placements
  exist in Rama.
- Do not imply source-to-native hover can work by arbitrary source byte range
  today. Current anchors are target-addressable; source-side anchor lookup is
  only an index of refs by source.
- Do not imply projection filtering or actor-specific visibility explanations
  exist.

## Findings

1. The current common base is implemented around request, decision, event,
   source, container, revision, derived unit, anchor, composition edge, import
   completion, source-material ref, native identity claim, and edit-order rows.

2. Markdown and transcript both reach the common substrate through
   `:object-container/import-material`, but they expose different truthful view
   affordances because markdown imports anonymous derived blocks and transcript
   imports source-native conversation/message/tool objects.

3. Markdown outline and transcript conversation/tool/audit/source-line rows are
   useful read models, not object-container base truth.

4. The biggest current read-model gap is hydration: several queries enumerate
   refs or projection rows, but a future view still needs bundled source,
   object, provenance, topology, and partial-state reads.

5. Relation, context membership, layer overlay, placement view state, code
   ingest, Roam/DG ingest, Linear ingest, and Softland-doc-specific ingest are
   not current substrate.

## Open Questions

1. Should `read-common-material-for-source` be upgraded to return hydrated rows,
   or should a separate hydrated source-bundle query exist?

2. What is the first implemented shape for `RelationEdge`, and how will a view
   distinguish candidate, accepted, rejected, and inferred relations?

3. Should transcript source-line parse errors be represented only in transcript
   projections/status rows, or also in a common warning/error material model?

4. What common query owns source inventory across all source refs and source
   families?

5. How should object trust/provenance be bundled for a view without forcing the
   client to perform many point reads?

6. When code/Roam/Linear ingesters arrive, what source-specific indexes are
   allowed, and what rows must always land in the common substrate?

## Next Recommended Goal

Goal 3 or Goal 4.

Goal 3 is useful if the next step is broad precedent research for source,
lineage, uncertainty, and import inspection. Goal 4 is useful if the next step
is a truth-state visual grammar. Do not start Goal 6 until this substrate map is
paired with concrete scenarios and missing-query decisions.
