# Missing View Queries

Status: draft, code-grounded on 2026-06-09.

## Origin Prompt

> I want a current substrate map for views: what the object-container kernel and
> ingesters can actually expose, what is common base truth, what is
> source-specific projection/index state, what read models exist, and what a
> view must not imply yet.

## Goal

List the queries a truthful view is likely to need, whether they exist now,
where they should probably live, and what breaks if the UI fakes them.

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
- `src/app/server/rama/dogfood/transcript.clj`
- `test/app/server/rama/object_container_test.clj`

## Scope

This is not an implementation plan. It is a factual gap list for future view
specs and Rama read-model work.

## Core Claim

The substrate has enough rows to build source/object/projection inspection for
markdown and transcript, but several view-critical questions still require too
many client-side reads or have no query at all. Those gaps must be shown as
missing/unknown in a view until Rama exposes them.

## Query Gap Table

| Query | Why the view needs it | Implemented? | Where it should probably live | Risk if the UI fakes it |
|---|---|---|---|---|
| List all imported sources by family/ref/latest version/status | Source inventory needs a starting set without already knowing source ids or refs. | No common query found. | `object-container-module` query topology over a source-inventory PState, likely fed from `SourceVersionRow` and import completion rows. | The view shows an incomplete world and makes hidden imports look absent. |
| Read source bundle by source ref | A source page needs latest source row, versions, completions, import keys, material counts, and source-family state. | Partially. `read-latest-source-by-ref` and `read-source-by-ref-version` exist, but no bundle. | `object-container-module` query topology. | The view conflates latest source with complete import state and misses partial/failure conditions. |
| Read hydrated common material for source | Source-to-world view needs full containers, derived units, anchors, and edges, not only refs. | Partially. `read-common-material-for-source` returns `SourceMaterialRefRow`s by category, not hydrated rows. | `object-container-module` query topology, using source material refs then colocated/partition-aware hydration. | The UI performs many point reads, may miss rows, and may accidentally treat refs as complete material. |
| Read object trust/provenance bundle | Object inspector needs current row, revisions, source anchors, source artifact, native claim, event, decision, and import completion. | No single query. Individual reads exist. | `object-container-module` query topology centered on target id. | The inspector shows "trusted" objects with missing provenance or hides rejection/conflict history. |
| Read source range to targets | Hover/highlight from raw source text needs byte/block range -> anchor(s) -> target row(s). | No. Target -> anchors exists; source -> anchor refs exists; no range overlap query. | `object-container-module` query topology plus a source-anchor-by-source-range PState or range-friendly index. | Hover implies exact grounding where only approximate or unknown grounding exists. |
| Read local topology bundle | A local view needs nodes, edges, current content, target kind, derived/graduated state, and projection membership together. | Partially. `read-outline`, `read-transcript-conversation-projection`, and edge reads exist separately. | Source-specific projection query for outline/transcript, plus common topology query for composition edges. | Client reconstructs topology inconsistently and may confuse projection order with canonical relation. |
| Read relation edges by target/source/local world | Future views need accepted/candidate semantic relations distinct from composition. | No `RelationEdge` rows/PStates found. | Future common relation topology in object-container/world kernel. | The UI turns structural edges into semantic claims and lies about supports/contradicts/depends-on. |
| Read candidate relation state | Views need to distinguish inferred/candidate/accepted/rejected relations. | No current candidate relation substrate found. | Future relation/situate module, with accepted decisions in common audit. | Inferred structure appears accepted, violating "the map must not lie." |
| Read source partialness/health | A source view needs accepted, rejected, parse-error, conflict, missing completion, stale, and unknown states. | Partially. Transcript has run/source-line/file-offset reads; common decisions exist by request id. No common aggregate by source. | Common source-health query in object-container, with source-specific extension hooks. | A source appears clean when rows failed, parse errors occurred, or no query exists. |
| Read transcript import status by conversation | Transcript view needs conversation projection plus run/source-line/file-offset state without already knowing every file key. | Partially. Conversation projection and line/run/file reads exist if ids/keys are known. | Transcript-specific query in object-container or transcript ops module. | A transcript looks complete while watch/harvest offset or source-line completion is unsafe. |
| Read markdown import status by document | Markdown view needs document container, source row, derived unit count, outline, and rejected import/edit decisions. | Partially. Outline/source/object reads exist; no document bundle or source-health aggregate. | `object-container-module` query topology. | The view cannot explain whether empty outline means empty file, failed import, or missing query. |
| Read native identity by source-native id | Future Roam/code/transcript views need source-native id -> container mapping. | Partially. `NativeIdentityClaimRow` is keyed by container id, not by source-native id. Transcript ids are deterministic but not exposed as a generic lookup. | Common native-identity index PState keyed by source family/native id. | UI guesses ids and may fork object identity. |
| Read projection membership/explanation | A view should explain why an item appears in an outline/transcript projection and which base rows it represents. | Partially. Projection rows carry target ids, event/import/source keys. No generic explanation query. | Source-specific projection query that returns projection row plus common target/provenance bundle. | Projection convenience becomes mistaken for base truth. |
| Read all children/parents with hydrated targets | Topology views need edge plus target rows in order. | Partially. `read-composition-children` and `read-composition-parents` return edge rows only. | `object-container-module` query topology. | UI performs N+1 reads and may render stale/missing content as if complete. |
| Read source anchor rows by source | Source inspection needs full anchor rows for all material from a source. | Partially. Source-side index stores `SourceMaterialRefRow` refs for anchors; full anchor rows are by target id. | `object-container-module` query topology or anchor-by-source PState containing full anchor rows/range index. | UI cannot faithfully highlight all source spans and may hide unanchored/partially anchored material. |
| Read view state by placement | Future views need placement-specific collapse/position/zoom/selection state. | No `ViewState` PState found. | Future view-state module or object-container extension keyed by view instance and placement. | UI stores layout as object truth or implies canonical spatial structure. |
| Read context membership/local-world membership | Views need to know which local world/page/board/thread a container belongs to. | No `ContextMembership` PState found. | Future common membership topology. | UI invents local-world boundaries from source/projection structure. |
| Read layer/overlay status | Views need base/active/candidate/draft differences. | No `LayerOverlay` PState found. | Future layer/overlay topology tied to decisions/events. | Draft or inferred material appears accepted in base. |
| Read actor-specific allowed actions | A view needs to know what actions are allowed without implementing policy in UI. | No object-container query found. Current request validation occurs on append. | Rama query topology over policy/target state. | UI enables actions that Rama rejects or hides actions that are valid, and cannot explain why. |
| Read code artifact/source map material | Code views need file/source/symbol/span provenance. | No code ingester/read model found in object-container. | Future code/artifact ingester feeding common import material plus code-specific indexes. | UI treats filesystem/editor state as native Softland truth before import exists. |
| Read Roam/DG uid material | Roam/DG views need page/block uid lookup and accepted graph edges. | No common Roam/DG ingester/read model found. | Future Roam/DG ingester feeding common material plus uid index and relation/candidate model. | UI implies Roam blocks are object containers and DG relations are accepted in Softland when they are not. |
| Read Linear issue material | Workflow views need issues/comments/status/provenance. | No Linear ingester/read model found. | Future Linear ingester feeding common material plus source-specific issue indexes. | UI makes external task state look imported and durable when it is not. |

## Findings

1. The most important missing query is a hydrated source bundle. It would reduce
   private reconstruction for source inventory, native object list, topology,
   and hover/highlight.

2. The second most important missing query is an object provenance/trust bundle.
   Without it, a view can show an object but cannot cheaply explain why it is
   trusted, accepted, derived, graduated, or partial.

3. The third missing piece is common source health. Transcript has richer
   operational state than markdown, but a view needs a common way to say
   "complete", "failed", "partial", "parse-error", "conflict", "unknown", and
   "not implemented".

4. Relation, layer, membership, and view-state queries are absent because their
   underlying rows are absent. A first view should mark those as future rather
   than mock them.

## Open Questions

1. Should source bundle hydration include full raw source text, or should raw
   text remain a separate explicit read?

2. Should source health be stored as a durable projection row or computed from
   decisions/completions/source-line state?

3. Should transcript conversation projection be wrapped by a common local
   topology query, or remain a transcript-specific projection with common target
   refs?

4. Which missing query is the first build blocker for Goal 6's first buildable
   Source-to-World View?

## Next Recommended Goal

Goal 5 is the best next step if the user wants concrete markdown/transcript
scenario walkthroughs against these missing queries. Goal 4 is the best next
step if the user wants the visual grammar for truth, uncertainty, provenance,
projection, and missing state.
