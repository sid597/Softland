# View Data Contract

Status: draft, code-grounded on 2026-06-09.

## Origin Prompt

> I want a current substrate map for views: what the object-container kernel and
> ingesters can actually expose, what is common base truth, what is
> source-specific projection/index state, what read models exist, and what a
> view must not imply yet.

## Goal

State the minimum data contract a truthful first view can depend on for source
inventory, native objects, object trust, local topology, source-to-native
inspection, and partial-state warnings.

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

This is a data contract, not a screen spec. "View" here means any projection
that reads current substrate rows and must not lie about what is known,
accepted, partial, source-specific, or missing.

## Core Claim

A first view can be truthful if it treats the common substrate as the source of
base object truth, treats outline/transcript rows as source-specific read
models, and explicitly marks unimplemented or unhydrated questions as unknown.

## 1. Source Inventory

### Minimum Data A View Needs

- source id
- source ref
- source hash/version key
- source format/family
- document or conversation container id when available
- raw capture availability
- latest version for a source ref
- import/completion status
- source-specific run/status state when the source has acquisition machinery

### Current Substrate It Can Consume

Common rows:

- `SourceArtifactRow`
- `SourceVersionRow`
- `SourceIngestCompletionRow`
- `ImportCompletionRow`

Common reads:

- `read-source`
- `read-latest-source-by-ref`
- `read-source-by-ref-version`
- `read-import-completion`

Transcript-specific reads:

- `read-transcript-run`
- `read-transcript-file-offset`
- `read-transcript-source-lines`
- `read-transcript-file-source-lines`

### Current Gap

There is no implemented common query that lists all sources, all source refs,
all latest versions, or all imports across source families. A source inventory
view currently needs a known `source-ref`, `source-id`, `import-key`, transcript
request id, or transcript file key.

### Must Not Imply

- all imported sources are enumerable today
- code/Roam/Linear/Softland-doc-specific source families exist
- transcript file run state is a common source-state model

## 2. Native Object List

### Minimum Data A View Needs

- container id
- container kind
- object key
- visibility
- source id
- source anchor id
- source unit id when graduated from a derived unit
- document/conversation container id
- current revision id
- current content text/hash
- created metadata and event id
- whether the item is native or derived

### Current Substrate It Can Consume

Common rows:

- `ObjectContainerRow`
- `DerivedUnitRow`
- `UnitGraduationRow`
- `RevisionRow`
- `SourceMaterialRefRow`

Common reads:

- `read-container`
- `read-unit`
- `read-current-revision`
- `read-revision-history`
- `read-source-containers`
- `read-source-derived-units`
- `read-common-material-for-source`

### Current Gap

`read-common-material-for-source` and the source-material helper reads return
`SourceMaterialRefRow` refs, not hydrated object/unit rows. A native object list
by source requires additional point reads or a future hydrated query.

### Must Not Imply

- markdown blocks are containers before graduation
- a source-material ref is the same thing as a full object row
- visibility promotion beyond currently stored `:private` rows
- source-specific projections are the object list

## 3. Object Trust Inspector

### Minimum Data A View Needs

For a container or derived unit:

- target kind and target id
- current row (`ObjectContainerRow` or `DerivedUnitRow`)
- graduation row if applicable
- current revision and revision history
- source anchors
- source artifact(s)
- composition parents and children
- native identity claim if applicable
- creation/revision event id
- accepted/rejected decision trail when request id/import key is known
- whether the object is base truth, derived, projection-only, parse-error-only,
  or missing

### Current Substrate It Can Consume

Common rows:

- `ObjectContainerRow`
- `DerivedUnitRow`
- `UnitGraduationRow`
- `RevisionRow`
- `SourceAnchorRow`
- `SourceArtifactRow`
- `CompositionEdgeRow`
- `NativeIdentityClaimRow`
- `ObjectContainerEventRow`
- `ObjectContainerDecisionRow`
- `ObjectContainerRequestRow`
- `ImportCompletionRow`

Common reads:

- `read-container`
- `read-unit`
- `read-current-revision`
- `read-revision-history`
- `read-source-anchors`
- `read-source`
- `read-composition-parents`
- `read-composition-children`
- `read-native-identity-claim`
- `read-request`
- `read-decision`
- `read-import-completion`

### Current Gap

There is no single trust/provenance bundle query. The view must either perform
multiple reads or wait for a query that hydrates target, source, anchors,
revisions, event, decision, import completion, and projection membership in one
contract.

### Must Not Imply

- a complete trust trail has been fetched when only the object row is visible
- rejected decisions can be ignored
- source anchors prove semantic correctness; they prove source grounding
- parse-error projection rows are native objects

## 4. Local Topology

### Minimum Data A View Needs

- local root id
- target ids and target kinds for nodes/items
- parent/child or follows/produced/contains edges
- order keys
- edge ids
- whether each target is derived or durable
- projection row that made the topology convenient to read, if applicable

### Current Substrate It Can Consume

Common rows:

- `CompositionEdgeRow`
- `ObjectContainerRow`
- `DerivedUnitRow`

Common reads:

- `read-composition-children`
- `read-composition-parents`
- `read-container`
- `read-unit`

Source-specific reads:

- `read-outline` for markdown document topology
- `read-transcript-conversation-projection` for transcript sequence
- `read-transcript-tool-calls-by-name` for a transcript tool-call index
- `read-transcript-last-message` for transcript continuation

### Current Gap

There is no generic "local topology by object key" query that hydrates a region
with nodes, edges, projection rows, and current object/unit content. There is
also no implemented `RelationEdge` topology.

### Must Not Imply

- outline parent/child structure is a semantic relation
- transcript follows/produced edges are general RelationEdges
- a canvas/local-world placement exists
- layout state exists in common object-container rows

## 5. Source-To-Native Hover/Highlight

### Minimum Data A View Needs

- source id
- source ref and hash
- source byte or block range
- target kind/id
- source anchor id
- exact anchor range
- current target content and trust state
- handling for source spans with no native target
- handling for native objects with no visible source range

### Current Substrate It Can Consume

Common rows:

- `SourceAnchorRow`
- `SourceMaterialRefRow`
- `ObjectContainerRow`
- `DerivedUnitRow`

Current reads:

- `read-source-anchors` by target id
- `read-source-anchor-refs` by source id
- `read-common-material-for-source` with `:anchors`
- `read-container`
- `read-unit`

### Current Gap

Source anchors are easy to read from target to source. Source-to-target lookup
is weaker: the source-side anchor index stores refs, not hydrated anchors, and
there is no byte-range overlap query. Arbitrary hover from raw source offset to
native targets is not implemented as one query.

### Must Not Imply

- every source byte range has a target
- every target can be highlighted from a source offset without additional reads
- source highlighting means relation correctness
- anchor presence means the source was interpreted without loss

## 6. Warning And Partial-State Display

### Minimum Data A View Needs

- request status and reason
- validation errors
- import completion or missing completion
- parse-error kind
- source-line completion state
- run status and progress
- file offset/resume state
- native identity conflict state
- target not found/stale edit state
- explicit "unknown because no query exists" state

### Current Substrate It Can Consume

Common rows:

- `ObjectContainerDecisionRow`
- `ObjectContainerRequestRow`
- `ImportCompletionRow`
- `TranscriptSourceLineStatusRow`
- `NativeIdentityClaimRow`
- `EditOrderRow`

Transcript operational rows:

- `TranscriptRunRow`
- `TranscriptFileOffsetRow`

Current reads:

- `read-decision`
- `read-request`
- `read-import-completion`
- `read-transcript-audit-entries`
- `read-transcript-source-line`
- `read-transcript-source-lines`
- `read-transcript-run`
- `read-transcript-file-offset`

### Current Gap

There is no common per-source "health" query that aggregates decisions,
parse errors, import completions, line statuses, conflicts, and missing query
states. Transcript has richer partial-state reads than markdown because
harvest/watch requires operational state.

### Must Not Imply

- absent rows mean success
- parse errors are objects
- transcript operational status generalizes to all source families
- a future source family has warning states before its ingester exists

## Findings

1. The view contract should be target-aware: every visible item should know
   whether it is an `:object-container`, `:derived-unit`, projection row, or
   operational row.

2. The view contract should be source-aware: source-specific projections may be
   used for ergonomics, but common object rows are the base truth.

3. The view contract should be partial-aware: unknown/missing must be displayed
   as unknown/missing, not silently filled in.

4. The first blocking substrate gap is not absence of rows. It is absence of
   hydrated, view-oriented read queries that bundle rows without making the UI
   reconstruct provenance manually.

## Open Questions

1. Should the first hydrated read model be source-centered, object-centered, or
   local-topology-centered?

2. Which partial-state categories must become common across all ingesters, and
   which should remain source-specific?

3. How much raw source text should a view read directly from `SourceArtifactRow`
   versus through a redacted/projection-specific read model?

4. Should projection rows carry an explicit pointer to the common row(s) they
   depend on, beyond current target ids and event/import keys?

## Next Recommended Goal

Goal 4 if the next question is how to represent truth/uncertainty/projection
states. Goal 5 if the next question is to walk concrete markdown and transcript
scenarios against this contract.
