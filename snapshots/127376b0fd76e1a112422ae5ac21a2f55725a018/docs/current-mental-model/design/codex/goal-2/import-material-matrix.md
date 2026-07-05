# Import Material Matrix

Status: draft, code-grounded on 2026-06-09.

## Origin Prompt

> I want a current substrate map for views: what the object-container kernel and
> ingesters can actually expose, what is common base truth, what is
> source-specific projection/index state, what read models exist, and what a
> view must not imply yet.

## Goal

Compare source types against the current common object-container import shape.

## Inputs Read

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

## Scope

This matrix describes import material and view implications only. It does not
rank sources, design views, or propose UI components.

## Core Claim

Only markdown and transcript currently have implemented common object-container
paths. Code, Roam/DG, Linear, and Softland-doc-specific imports are contract
concepts or future source families, not current common object-container
ingesters.

## Matrix

| Source type | SourceArtifact behavior | Native ObjectContainers | DerivedUnits | SourceAnchors | CompositionEdges | RelationEdges | Projections/indexes | Current status | View implication |
|---|---|---|---|---|---|---|---|---|---|
| markdown | One immutable `SourceArtifactRow` per imported source/ref/hash. `source-raw-text` stores the full raw markdown text. `SourceVersionRow` and latest-by-ref are written. | One document `ObjectContainerRow` is created immediately with a document `RevisionRow`. Child markdown blocks are not containers until graduation. | Yes. `markdown-block-v0` emits headings, paragraphs, and list items as `DerivedUnitRow`s with `:unit-kind`, `:block-path`, `:parent-slot-id`, distiller id, and version. | Document anchor plus one anchor per derived unit. Anchors include source offsets and block path. | One structural edge per derived block. Parent is the document or another derived unit. Edges are updated when a derived unit graduates. | None implemented. | `OutlineNodeRow` hints become `$$outline-by-document`. Common source material refs are written for containers, units, anchors, and edges. | Implemented through `markdown_adapter/source-ingest-request` -> `:object-container/import-material`. Tested in `object_container_test.clj`. | A view can show raw source, document object, derived block outline, anchors, and structural composition. It must show block units as derived/ungraduated until edited. |
| transcript | One `SourceArtifactRow` per observed transcript source line. `source-ref` is file path plus byte offset. `source-raw-text` is redacted preview/payload text, not necessarily the whole JSONL file. `SourceVersionRow` is written per line. | Conversation, message, tool-call, and tool-result containers are created immediately when the source line parses. Parse-error lines create no containers. | No current transcript `DerivedUnitRow`s. Transcript source has native ids or deterministic line-derived ids for native containers. | Anchors per created container. Anchors use byte offsets and line hash; line hash is stored in `:block-path`. | Contains/follows/produced structural edges connect conversation, messages, tool calls, and tool results. | None implemented. | Transcript conversation projection, tool-call index, audit entries, last-message index, source-line status rows, run rows, and file-offset rows. | Implemented through `transcript_adapter/transcript-observation-import-request`; harvest/watch route through common object-container runtime when available. Tested in `object_container_test.clj`. | A view can show conversation sequence, tool events, import audit, parse errors, and harvest/watch partialness. It must not treat transcript projections as canonical object rows. |
| code | No code `SourceArtifactRow` behavior found in current object-container files. | No implemented code file, code span, repo snapshot, or symbol containers in the common object-container module. | No implemented code-derived units. | No code anchors implemented in object-container. | No code composition edges implemented in object-container. | None implemented. | No code symbol index found in current object-container substrate. | Not implemented as a common object-container ingester. Mentioned as a future source family in architecture docs. | A view must show code ingest as missing/future, not silently browse code as native Softland material. |
| Roam/DG graph | No Roam/DG `SourceArtifactRow` behavior found in the current object-container module. | No implemented Roam page/block containers in common object-container. The contract says stable Roam uids should become native containers later. | No Roam-derived units implemented here. | No common Roam source anchors implemented here. | No Roam composition edges implemented here. | None implemented. | No Roam uid lookup/index implemented in object-container. `src/app/server/rama/roam_ns.clj` exists but is outside this current object-container map. | Not implemented as a common object-container ingester. | A view must not imply Roam pages/blocks are queryable through the object-container substrate yet. |
| Linear | No Linear `SourceArtifactRow` behavior found in current object-container files. | No issue/comment/workflow containers implemented in common object-container. | No Linear-derived units implemented. | No Linear anchors implemented. | No Linear composition edges implemented. | None implemented. | No Linear issue index implemented. | Not implemented as a common object-container ingester. | A view must not show Linear tickets as native object-container material unless a separate current source supplies them. |
| Softland docs | No distinct Softland-doc source family exists. A docs file can enter as ordinary markdown if the markdown adapter is invoked. | Same as markdown: document container only, plus graduated block containers after edits. No Softland-doc-specific object kind. | Same as markdown when imported through markdown. | Same as markdown when imported through markdown. | Same as markdown when imported through markdown. | None implemented. | Markdown outline only. No docs-specific provenance, thread-map, session, or architecture indexes in object-container. | Partially available only as generic markdown import; not implemented as a distinct docs ingester. | A view can treat imported docs as markdown material, but must not imply awareness of Softland doc semantics, privacy class, thread-map role, or architecture-doc type. |

## Findings

1. The common import contract is real in code for markdown and transcript:
   both adapters create `:object-container/import-material` requests.

2. Markdown and transcript differ correctly at the native-id boundary:
   markdown blocks become `DerivedUnitRow`s, while transcript conversation,
   message, tool-call, and tool-result items become native containers.

3. Source-specific projections are necessary and currently implemented for
   markdown outline and transcript conversation/tool/audit/source-line views.
   They are not common base truth.

4. `RelationEdge` is not implemented for any source type in the current
   object-container module.

5. Code, Roam/DG, Linear, and Softland-doc-specific rows are future substrate,
   not current view data.

## Open Questions

1. Should Softland docs remain generic markdown imports, or should they become a
   source-specific ingester with doc/session/thread-map indexes?

2. What is the first source that should exercise `RelationEdge`: transcript
   tool causality, Roam/DG relations, code evidence links, or markdown claims?

3. Will code file paths be native container ids, source refs, or both under a
   repo/snapshot policy?

4. How should future Roam/DG import preserve native uid identity while keeping
   inferred discourse relations candidate-only until accepted?

## Next Recommended Goal

Goal 5 after at least one concrete scenario is chosen, or Goal 4 if the next
step is to define truth-state language for implemented/missing/future material.
