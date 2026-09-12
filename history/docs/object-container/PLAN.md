# Plan - Object Container kernel, Slice 1

<!--
Phase 1 Rama artifact.
Inputs:
- docs/current-mental-model/architecture/object-container-spec.md
- docs/current-mental-model/architecture/object-container-reviewer-world-model.md
- docs/current-mental-model/build/object-container/IMPLICIT_SPEC.md

This phase is design only. Do not write topology, ETL, query topology, or test
implementation code here.
-->

## Slice Boundary

This plan implements only the Slice 1 spine:

```text
markdown source
  -> immutable SourceArtifact
  -> document ObjectContainer
  -> DerivedUnits + SourceAnchors + CompositionEdges
  -> outline projection
  -> edit DerivedUnit
  -> graduated ObjectContainer + first Revision
  -> later edits create Revisions on the same container
```

Explicitly out of scope for this plan: layers/base-active promotion, RelationEdge, situate, semantic
search/global pool, ViewState, canvas/chat/code interpreters, non-markdown distillers, deletion, drift
resolution, and inferred relationship acceptance.

Phase 3 should create a new Rama module source file:

```text
src/app/server/rama/object_container.clj
```

Phase 3 should also extend `app.server.rama.core/target-kinds` with the target kinds used by this slice:

```text
:source-artifact
:object-container
:derived-unit
:composition-edge
:source-anchor
```

The existing `text_kernel.clj` remains a proof/reference, not the storage shape to copy. In particular,
this plan must not reintroduce the old collapsed `textArtifact` shape.

## ID, Routing, and Ordering Contract

All entity creation IDs that can be written by a stream topology are deterministic or supplied in the
request. No topology-generated random IDs are allowed in this slice.

### Stable Keys

```text
source-ref-key     = sha256(source/ref)
object-key         = sha256(source/ref + "\u0000" + source/hash)
source/id          = "src:" + object-key
document/id        = "oc:doc:" + object-key
derived-unit/id    = "du:" + object-key + ":" + distiller/id + ":" + block-path
source-anchor/id   = "sa:" + target/id
composition-edge/id = "ce:" + object-key + ":" + parent-slot-id + ":" + child-order-key
block-container/id = "oc:block:" + object-key + ":" + unit-local-id
revision/id        = request-supplied revision/id, else "rev:" + object-key + ":" + request/id
event/id           = "evt:" + object-key + ":" + request/id
audit/id           = partition/key + "/request/" + request/id
decision/id        = audit/id + "/decision"
revision/order-key = fixed-width request/time-ms + ":" + request/id
```

`block-path` is a lexicographically sortable pre-order path such as `000000`, `000001/000003`,
or another fixed-width path chosen by the markdown distiller. It is also the outline order key.

### Depot Partition Key

The module request depot is partitioned by a top-level request field:

```text
:partition/key String
```

Rules:

```text
source/ingest partition/key = source-ref-key
object/edit   partition/key = object-key
```

`source/ingest` starts on the logical source-ref partition so versions of the same ref are serialized.
It repartitions to `object-key` for document/object materialization. `object/edit` starts on the
document/object partition so graduation/revision decisions for the same document are sequential.

Requests still carry the shared kernel `:routing/key` vector for the common envelope:

```text
source/ingest routing/key = [:source-ref source-ref-key]
object/edit   routing/key = [:object-container object-key]
```

### Custom PState Partitioner

Phase 3 should define a top-level partitioner function for object-container IDs:

```text
partition-by-object-key(num-partitions, id-or-key)
```

It extracts `object-key` from `src:`, `oc:doc:`, `du:`, `sa:`, `ce:`, `oc:block:`, `rev:`, and `evt:`
IDs, then hashes that key. PStates keyed by these IDs use `{:key-partitioner partition-by-object-key}`.
This keeps source/document/unit/container/revision/outline rows for one imported source colocated while
still allowing one-key `foreign-select` calls by entity id.

`$$source-latest-by-ref` and `$$source-versions-by-ref` are keyed by `source-ref-key`, not `object-key`.

### Audit Partitioning

Request and decision audit rows are stored by `audit/id`, not by bare `request/id`.

```text
audit/id = partition/key + "/request/" + request/id
```

Phase 3 should define:

```text
partition-by-audit-id(num-partitions, audit/id)
```

It extracts the prefix before `"/request/"` and hashes that prefix. `read-request` and
`read-decision` helpers must accept either the original request map or `(partition/key, request/id)`, build
the `audit/id`, and read the audit PStates with that key. There is intentionally no bare
`read-decision(request/id)` helper in this slice because a request id alone does not determine the Rama
partition that wrote the row.

### Edit Ordering

`object/edit` requests include:

```text
:idempotency/key String
:edit/client-id String
:edit/seq Long
```

For an edit lineage key and client id, the stream stores the highest accepted `:edit/seq`. The lineage
key is the stable unit/outline slot id when a block came from a `DerivedUnit`; after graduation, edits to
the resulting `ObjectContainer` still use that source unit id. Containers with no source unit use their
container id as the lineage key.

A request with the same idempotency key returns the prior decision. A request with a lower or equal edit
seq for the same lineage/client is rejected as `:edit/stale` unless it is the same idempotency key. This
prevents older client callbacks from advancing the current revision after a newer edit from the same
client already won, even when the target ref changed from `DerivedUnit` to `ObjectContainer`. Across
different clients, the Rama partition order is the conflict order for Slice 1.

## Reads

### R1. `read-source`

Direct source lookup by source id:

```clojure
(foreign-select-one [(keypath source-id)] $$source-artifacts-by-id)
```

Access method: single `foreign-select-one`, one PState, one partition.

Partition: `partition-by-object-key(source-id)`.

Cost: 1 seek. The row contains the immutable raw markdown text exactly as captured.

Lookup by `(source/ref, source/hash)` computes `source/id` deterministically and uses the same path.

Latest source by ref uses query topology `read-latest-source-by-ref` because it needs the ref index and,
when present, the source row:

```text
input ref with no versions -> 1 meaningful read, returns nil
input ref with versions    -> 2 meaningful reads, returns latest SourceArtifactRow
variable source read       -> use <<if; only read $$source-artifacts-by-id when latest source/id exists
```

### R2. `read-outline`

Outline is a denormalized projection PState keyed by document id/object key and sorted by pre-order path:

```clojure
(foreign-select [(keypath document-id)
                 (sorted-map-range-from cursor limit)
                 MAP-VALS]
                $$outline-by-document)
```

Access method: single `foreign-select` range scan. A full outline is the same path with an unbounded
range or a sufficiently large limit, but the public helper should prefer pagination.

Partition: `partition-by-object-key(document-id)`.

Cost for N visible nodes: 1 seek + N sequential iterations. Example: 1,000 outline nodes cost roughly
0.5ms + 1,000 * 5us = 5.5ms before serialization/rendering.

The row already contains the correct rendered content:

```text
if not graduated -> source-derived text
if graduated     -> current revision text
```

This avoids an N+1 query over unit/container/revision PStates.

### R3. `read-container`

Direct container lookup by container id:

```clojure
(foreign-select-one [(keypath container-id)] $$containers-by-id)
```

Access method: single `foreign-select-one`.

Partition: `partition-by-object-key(container-id)`.

Cost: 1 seek. The row includes the current revision summary, so the common "container + current
revision" read is one seek. Full revision history is a separate range read:

```clojure
(foreign-select [(keypath container-id)
                 (sorted-map-range-from cursor limit)
                 MAP-VALS]
                $$revision-history-by-container)
```

Cost for M revisions: 1 seek + M sequential iterations.

### R4. `read-unit`

Use query topology `read-unit` because the result combines the derived unit row and optional graduation
row, and client-side multi-read would be two network roundtrips.

Query topology shape:

```text
extract object-key from unit/id
|hash object-key
local-select $$derived-units-by-id[unit/id]
if no unit row:
  |origin
  return nil / not found
if unit row exists:
  local-select $$unit-graduations-by-id[unit/id]
  merge:
    no graduation row -> derived content, graduated? false
    graduation row    -> current content from graduation row, graduated? true, container/id
|origin
return one UnitReadResult
```

Read count examples:

```text
missing unit             -> 1 meaningful point read; nil unit affects output
derived, not graduated   -> 2 meaningful point reads; nil graduation affects output
graduated/revised unit   -> 2 meaningful point reads; graduation row carries current content
```

Fixed or variable: variable. Missing units need one meaningful read; existing units need two.

Dynamic approach:

```text
Use <<if after the DerivedUnitRow read. Only issue the UnitGraduationRow read in the branch where the
DerivedUnitRow exists. Do not pad the missing-unit case with a graduation read that cannot affect output.
```

## Writes

### W1. `source/ingest`

Request type:

```text
:source/ingest
```

Required request fields:

```text
:request/id String
:request/type :source/ingest
:partition/key source-ref-key
:routing/key [:source-ref source-ref-key]
:idempotency/key String
:actor ActorRow
:target TargetRefRow {:target/kind :source-artifact, :target/id source/id}
:payload SourceIngestPayload
```

`SourceIngestPayload` fields:

```text
:source/ref String
:source/hash String
:source/raw-text String
:source/format :markdown
:distiller/id "markdown-block-v0"
:distiller/version Long
```

Validation:

```text
reject :source/ref-invalid       when source/ref is blank or not a String
reject :source/hash-invalid      when hash is blank or does not match raw text
reject :source/format-invalid    when format is not :markdown
reject :distiller/unsupported    when distiller is not markdown-block-v0
reject :actor-not-authorized     when actor lacks source/ingest capability
```

Dedupe:

```text
dedupe key = source/id = sha256(source/ref + "\u0000" + source/hash)
```

Completion:

```text
completion key = [source-ref-key, source/hash]
```

`source/ingest` may skip block materialization only when
`$$source-ingest-completions-by-ref[source-ref-key][source/hash]` exists. A pre-existing
`$$source-artifacts-by-id[source/id]` row is not enough to prove completion, because a stream retry can
fail after object-key writes and before source-ref indexes are written.

If the completion row is missing, the topology must idempotently ensure every deterministic
materialization row for the source version exists, even if some rows already exist from a partial prior
attempt. It then writes the source-ref indexes and completion row last on the source-ref-key partition.
Duplicate successful re-ingests of the same `(source/ref, source/hash)` write only the new request audit
row and an accepted duplicate decision that points at the completed source/document ids; they do not
rewrite units, edges, anchors, outline rows, or containers.

Duplicate accepted decisions use the `event/id` stored in `SourceIngestCompletionRow`; they do not create a
new `:source/ingested` event for already-completed materialization.

Accepted event:

```text
:event/type :source/ingested
:target/kind :source-artifact
:target/id source/id
:payload:
  :source/id
  :source/ref
  :source/hash
  :document/container-id
  :distiller/id
  :distiller/version
  :derived/unit-count
  :composition/edge-count
```

Materialization:

```text
$$source-artifacts-by-id[source/id] = SourceArtifactRow(raw text included)
$$source-versions-by-ref[source-ref-key][source/hash] = SourceVersionRow
$$source-latest-by-ref[source-ref-key] = SourceVersionRow
$$source-ingest-completions-by-ref[source-ref-key][source/hash] = SourceIngestCompletionRow
$$containers-by-id[document/id] = ObjectContainerRow(kind :document)
$$source-anchors-by-target[document/id] = SourceAnchorRow(document span)
$$derived-units-by-id[unit/id] = DerivedUnitRow for each markdown block
$$source-anchors-by-target[unit/id] = SourceAnchorRow for each unit span
$$composition-children-by-parent[parent-slot-id][child-order-key] = CompositionEdgeRow
$$composition-parent-by-child[child-slot-id] = CompositionEdgeRow
$$outline-by-document[document/id][block-path] = OutlineNodeRow
$$events-by-id[event/id] = ObjectContainerEventRow
```

Scale behavior:

```text
write volume = O(number of markdown blocks)
```

This is bounded by the imported file, which the app controls. The stream loop must call
`yield-if-overtime` while walking/writing the distilled block sequence so large files do not monopolize a
task thread. All row writes use `keypath + termval` to avoid read-before-write.

Edge cases:

```text
empty file        -> SourceArtifact + document container + zero units + empty outline
whitespace file   -> raw preserved; distiller may produce zero or one trivial unit
same ref/hash     -> idempotent, no duplicate rows
same ref/new hash -> new SourceArtifact/document version; old graduated blocks untouched
```

### W2. `object/edit`

Request type:

```text
:object/edit
```

Required request fields:

```text
:request/id String
:request/type :object/edit
:partition/key object-key
:routing/key [:object-container object-key]
:idempotency/key String
:actor ActorRow
:target TargetRefRow
:payload ObjectEditPayload
```

`TargetRefRow`:

```text
target kind = :derived-unit or :object-container
target id   = derived-unit/id or object-container/id
```

`ObjectEditPayload` fields:

```text
:document/container-id String
:object/key String
:content/text String
:content/hash String
:edit/client-id String
:edit/seq Long
:edit/lineage-key String optional; derived by topology when omitted
:revision/id String optional
```

Validation:

```text
reject :content/hash-invalid    when hash does not match content/text
reject :target/not-found        when target is neither an existing DerivedUnit nor ObjectContainer
reject :target/kind-invalid     when target kind is not :derived-unit or :object-container
reject :edit/stale              when edit seq is older than last accepted seq for lineage/client
reject :actor-not-authorized    when actor lacks object/edit capability
```

Accepted behavior, target is a not-yet-graduated `DerivedUnit`:

```text
container/id = deterministic block-container/id derived from unit/id
revision/id  = supplied revision/id or deterministic request revision id
parent-revision/id = nil

write ObjectContainerRow(kind :text-block)
write RevisionRow(first revision)
write UnitGraduationRow(unit/id -> container/id + current content)
copy SourceAnchor to container target
update OutlineNodeRow to target object-container and show edited content
update composition edge materialization to resolve child target to container/id
write EditOrderRow for lineage/client
write accepted decision and :object/graduated event
```

Accepted behavior, target is an already-graduated `DerivedUnit`:

```text
read UnitGraduationRow
use existing container/id
create new RevisionRow(parent = container current revision)
advance ObjectContainerRow current revision
update UnitGraduationRow current content
update OutlineNodeRow current content
write EditOrderRow for lineage/client
write accepted decision and :object/revised event
```

Accepted behavior, target is an `ObjectContainer`:

```text
read ObjectContainerRow
create new RevisionRow(parent = current revision)
advance ObjectContainerRow current revision
if container has a source unit/outline placement, update UnitGraduationRow and OutlineNodeRow
write EditOrderRow for lineage/client
write accepted decision and :object/revised event
```

Idempotency:

```text
duplicate idempotency/key -> return prior decision, no new revision
duplicate request/id      -> same decision/id, no new revision
same DerivedUnit touched twice by distinct edits -> one container, multiple revisions
```

Empty string edits are valid. They create or revise a durable text-block container with empty content.

## PState Design

### Typed Rows

Phase 3 should define concrete `defrecord`/interface types for polymorphic rows and payloads. PState
schemas must use those concrete classes or explicit `fixed-keys-schema` values. No PState schema in this
module may use `Object`.

Named row types:

```text
ActorRow
TargetRefRow
SourceIngestPayload
ObjectEditPayload
ObjectContainerRequestRow
ObjectContainerDecisionRow
ObjectContainerEventRow
SourceArtifactRow
SourceVersionRow
SourceIngestCompletionRow
ObjectContainerRow
RevisionRow
DerivedUnitRow
UnitGraduationRow
SourceAnchorRow
CompositionEdgeRow
OutlineNodeRow
EditOrderRow
UnitReadResult
```

Payload interfaces:

```text
IObjectContainerRequestPayload implemented by SourceIngestPayload, ObjectEditPayload
IObjectContainerEventPayload implemented by SourceIngestedPayload, ObjectGraduatedPayload, ObjectRevisedPayload
```

Representative row fields:

```text
SourceArtifactRow:
  source/id String
  source/ref String
  source/hash String
  source/format clojure.lang.Keyword
  source/raw-text String
  document/container-id String
  content/byte-count Long
  created-at-ms Long
  created-by String
  event/id String

SourceIngestCompletionRow:
  source/id String
  source/ref-key String
  source/hash String
  document/container-id String
  derived/unit-count Long
  composition/edge-count Long
  completed-at-ms Long
  completed-by-request/id String
  event/id String

ObjectContainerRow:
  container/id String
  container/kind clojure.lang.Keyword
  object/key String
  visibility clojure.lang.Keyword
  source/id String optional
  source-anchor/id String optional
  source-unit/id String optional
  current-revision/id String optional
  current/content-text String optional
  current/content-hash String optional
  created-at-ms Long
  created-by String
  event/id String

RevisionRow:
  revision/id String
  container/id String
  parent-revision/id String optional
  content/text String
  content/hash String
  order/key String
  created-at-ms Long
  created-by String
  event/id String

DerivedUnitRow:
  unit/id String
  document/container-id String
  source/id String
  unit/kind clojure.lang.Keyword
  block/path String
  parent-slot/id String optional
  source-anchor/id String
  derived/content-text String
  derived/content-hash String
  distiller/id String
  distiller/version Long
  event/id String

UnitGraduationRow:
  unit/id String
  container/id String
  current-revision/id String
  current/content-text String
  current/content-hash String
  graduated-at-ms Long
  last-revised-at-ms Long
  event/id String

OutlineNodeRow:
  document/container-id String
  node/slot-id String
  block/path String
  parent-slot/id String optional
  target/kind clojure.lang.Keyword
  target/id String
  source-anchor/id String
  content/text String
  content/hash String
  graduated Boolean
  container/id String optional
```

### PStates

```text
$$requests-by-audit-id:
  {String ObjectContainerRequestRow}
  key-partitioner = partition-by-audit-id
  private? false

$$decisions-by-audit-id:
  {String ObjectContainerDecisionRow}
  key-partitioner = partition-by-audit-id

$$decisions-by-idempotency:
  {String {String ObjectContainerDecisionRow}}
  outer key = partition/key, inner key = idempotency/key
  inner map subindexed

$$events-by-id:
  {String ObjectContainerEventRow}
  key-partitioner = partition-by-object-key

$$source-artifacts-by-id:
  {String SourceArtifactRow}
  key-partitioner = partition-by-object-key

$$source-versions-by-ref:
  {String {String SourceVersionRow}}
  outer key = source-ref-key, inner key = source/hash
  inner map subindexed

$$source-latest-by-ref:
  {String SourceVersionRow}
  outer key = source-ref-key

$$source-ingest-completions-by-ref:
  {String {String SourceIngestCompletionRow}}
  outer key = source-ref-key, inner key = source/hash
  inner map subindexed

$$containers-by-id:
  {String ObjectContainerRow}
  key-partitioner = partition-by-object-key

$$revision-history-by-container:
  {String {String RevisionRow}}
  outer key = container/id, inner key = revision order/key
  key-partitioner = partition-by-object-key
  inner map subindexed

$$derived-units-by-id:
  {String DerivedUnitRow}
  key-partitioner = partition-by-object-key

$$unit-graduations-by-id:
  {String UnitGraduationRow}
  key-partitioner = partition-by-object-key

$$source-anchors-by-target:
  {String SourceAnchorRow}
  top key = target/id
  key-partitioner = partition-by-object-key

$$composition-children-by-parent:
  {String {String CompositionEdgeRow}}
  outer key = parent slot/container id, inner key = child order key
  key-partitioner = partition-by-object-key
  inner map subindexed

$$composition-parent-by-child:
  {String CompositionEdgeRow}
  top key = child slot id
  key-partitioner = partition-by-object-key

$$outline-by-document:
  {String {String OutlineNodeRow}}
  outer key = document/container-id, inner key = block/path
  key-partitioner = partition-by-object-key
  inner map subindexed

$$edit-order-by-target:
  {String {String EditOrderRow}}
  outer key = edit lineage key, inner key = edit/client-id
  key-partitioner = partition-by-object-key
  inner map subindexed
```

### Non-Obvious Design Choices

#### Outline Read Shape

Option A: normalize the outline completely and have `read-outline` scan composition edges, then read each
unit/container/revision row.

Cost for 1,000 blocks:

```text
1 edge range seek + 1,000 edge iterations
+ up to 1,000 unit/container point seeks
= roughly 0.5ms + 5ms + 500ms before serialization
```

This is too expensive and creates an N+1 read path.

Option B: maintain `$$outline-by-document` as a denormalized, sorted projection row per outline node.

Cost for 1,000 blocks:

```text
1 range seek + 1,000 sequential iterations
= roughly 0.5ms + 5ms before serialization
```

Chosen: Option B. Composition edges are still stored as first-class facts, but outline rendering reads the
projection PState built for that access pattern.

#### Source Latest By Ref

Option A: duplicate raw source text under both `source/id` and `source/ref` indexes.

Cost: latest source by ref is 1 seek, but every source version duplicates potentially large raw text.

Option B: store raw text once in `$$source-artifacts-by-id`; store only version pointers in
`$$source-latest-by-ref` and `$$source-versions-by-ref`.

Cost: latest source by ref is 2 seeks when present.

Chosen: Option B. The extra seek on the rare latest-by-ref read is cheaper than duplicating raw file
contents on every import.

#### Graduation State

Option A: mutate `DerivedUnitRow` to become the durable text content row.

Risk: collapses derived source text and authored durable text, recreating the old textArtifact problem.

Option B: keep `DerivedUnitRow` source-derived and store `UnitGraduationRow` plus container/revision rows.

Chosen: Option B. The source-derived unit remains recoverable; the graduation row supplies durable current
content for reads.

#### Container Partitioning

Option A: hash containers by raw `container/id`, documents by `document/id`, and units by `unit/id`.

Cost: edit needs cross-partition reads/writes to update unit, container, revision, outline, and edge rows.

Option B: embed `object-key` in all Slice 1 IDs and use a shared custom PState key partitioner.

Chosen: Option B. It keeps all document-local edit state on one task, making graduation/revision a local
sequential decision.

## Depots

```text
*object-container-requests-depot:
  declaration: (declare-depot setup *object-container-requests-depot (hash-by :partition/key))
  owner: object-container module
  event types:
    :source/ingest
    :object/edit
  client ack:
    edit callers use :ack for read-after-write and decision visibility
    ingest callers may use :ack when they need the decision immediately; otherwise :append-ack plus polling
```

Same depot rationale:

```text
source/ingest and object/edit both affect the same source/document/object PStates.
object/edit must observe whether a derived unit exists and whether it is already graduated.
Using one depot and one owner topology avoids PState ownership conflicts.
```

Partitioning:

```text
source/ingest -> source-ref-key for same-ref version ordering
object/edit   -> object-key for same-document edit ordering
```

No internal depots are needed in Slice 1.

## Topologies and PStates

### `object-container-topology`

Type: stream.

Reason:

```text
object/edit is interactive and must support :ack read-after-write behavior.
The same topology must own source, unit, container, revision, edge, and outline PStates because ingest and
edit both materialize the same world substrate.
```

Source options:

```text
(source> *object-container-requests-depot {:retry-mode :all-after} :> *request)
```

`all-after` preserves local order after failures for a given partition key. This matters for same-ref
source versions and same-document edits.

Stream retry safety:

```text
No write uses append/vector AFTER-ELEM, counters, or topology-generated random ids.
Every entity id is deterministic from source/ref+hash+block path or request id.
Every PState write is keypath + termval of a complete row.
Duplicate processing overwrites the same keys with the same values.
```

Non-idempotent writes in this stream topology: none.

Cooperative multitasking:

```text
source/ingest loops over distilled markdown blocks.
The implementation must call yield-if-overtime inside the loop before writing each block group.
Large local-select range reads in query topologies must use {:allow-yield? true}.
```

PStates owned by this topology:

```text
$$requests-by-audit-id
$$decisions-by-audit-id
$$decisions-by-idempotency
$$events-by-id
$$source-artifacts-by-id
$$source-versions-by-ref
$$source-latest-by-ref
$$source-ingest-completions-by-ref
$$containers-by-id
$$revision-history-by-container
$$derived-units-by-id
$$unit-graduations-by-id
$$source-anchors-by-target
$$composition-children-by-parent
$$composition-parent-by-child
$$outline-by-document
$$edit-order-by-target
```

### Materialization Branches

`source/ingest` branch:

```text
1. validate request envelope, actor, source hash, markdown format
2. compute audit/id and write $$requests-by-audit-id[audit/id]
3. check $$decisions-by-idempotency[partition/key][idempotency/key]
4. if duplicate, write $$decisions-by-audit-id[audit/id] as an idempotency replay and stop
5. check $$source-ingest-completions-by-ref[source-ref-key][source/hash]
6. if completion exists, write accepted duplicate decision rows and stop; do not rewrite block materialization
7. partition to object-key
8. distill markdown blocks with markdown-block-v0
9. idempotently ensure SourceArtifactRow, document ObjectContainerRow, anchors, units, edges, outline rows,
   and ObjectContainerEventRow on the object-key partition
10. partition back to source-ref-key
11. write source ref version/latest indexes
12. write SourceIngestCompletionRow last on the source-ref-key partition
13. write accepted decision rows to $$decisions-by-audit-id and $$decisions-by-idempotency
```

Retry repair rule:

```text
If the event retries after step 9 but before step 12, the completion row is absent, so the retry repeats
the object-key ensure writes and then repairs the source-ref indexes/completion. Existing object-key rows
must not short-circuit the repair. All ensure writes use deterministic keys and complete termval rows.
```

`object/edit` branch:

```text
1. validate request envelope, actor, content hash, target kind
2. compute audit/id and write $$requests-by-audit-id[audit/id]
3. check $$decisions-by-idempotency[partition/key][idempotency/key]
4. if duplicate, write $$decisions-by-audit-id[audit/id] as an idempotency replay and stop
5. resolve edit lineage key, then check $$edit-order-by-target[lineage-key][edit/client-id]
6. target :derived-unit:
   - read DerivedUnitRow
   - read UnitGraduationRow
   - if no graduation, graduate once
   - if graduation exists, revise its container
7. target :object-container:
   - read ObjectContainerRow
   - revise existing container
8. write revision/history/container/graduation/outline/edge rows as complete termval rows
9. write accepted decision/event rows to $$decisions-by-audit-id, $$decisions-by-idempotency, and $$events-by-id
```

Rejected requests:

```text
always write ObjectContainerRequestRow to $$requests-by-audit-id
always write ObjectContainerDecisionRow to $$decisions-by-audit-id and $$decisions-by-idempotency
never silently drop a request
never emit KernelEvent for a rejected request
```

## Query Topologies

### `read-latest-source-by-ref`

Input:

```text
source/ref String
```

Output:

```text
SourceArtifactRow or nil
```

Examples:

```text
no versions for ref:
  read $$source-latest-by-ref[source-ref-key] -> nil
  total reads = 1, meaningful reads = 1

one or more versions for ref:
  read $$source-latest-by-ref[source-ref-key] -> SourceVersionRow
  read $$source-artifacts-by-id[source/id] -> SourceArtifactRow
  total reads = 2, meaningful reads = 2
```

Fixed or variable: variable, because the source row read exists only when a latest pointer exists.

Dynamic approach:

```text
use <<if on latest SourceVersionRow; issue the source-id local-select only in the present branch
```

### `read-unit`

Input:

```text
unit/id String
```

Output:

```text
UnitReadResult or nil
```

Examples:

```text
missing unit:
  read $$derived-units-by-id[unit/id] -> nil
  total reads = 1, meaningful reads = 1

derived unit:
  read $$derived-units-by-id[unit/id] -> DerivedUnitRow
  read $$unit-graduations-by-id[unit/id] -> nil
  total reads = 2, meaningful reads = 2

graduated unit:
  read $$derived-units-by-id[unit/id] -> DerivedUnitRow
  read $$unit-graduations-by-id[unit/id] -> UnitGraduationRow
  total reads = 2, meaningful reads = 2
```

Fixed or variable: variable. Missing units need one meaningful read; existing units need two.

Dynamic approach:

```text
Use <<if after the DerivedUnitRow read. Only issue the UnitGraduationRow read in the branch where the
DerivedUnitRow exists.
```

### `read-outline-expanded` (Optional Helper)

The normal outline read is a direct `foreign-select` against `$$outline-by-document`. If the UI wants a
single query call that accepts a ref/hash or source id and returns outline metadata plus nodes, use this
query topology.

Examples:

```text
document id input:
  read outline range only
  total reads = 1 range scan, meaningful reads = 1

source id input:
  read source row to find document/container-id
  read outline range
  total reads = 2, meaningful reads = 2
```

Fixed or variable: variable by input mode.

Dynamic approach:

```text
dispatch by input mode; do not issue source lookup when document id is already supplied
```

Implementation note: local range scans over `$$outline-by-document` that may return thousands of rows
must use `{:allow-yield? true}`.

## Design Decisions

### Subindexing

Subindexed:

```text
$$decisions-by-idempotency inner map
$$source-versions-by-ref inner map
$$source-ingest-completions-by-ref inner map
$$revision-history-by-container inner map
$$composition-children-by-parent inner map
$$outline-by-document inner map
$$edit-order-by-target inner map
```

Reason: each can grow beyond 100 entries for a large source, long-lived document, or repeated edits.

### Colocation

All document-local entities embed `object-key` and use `partition-by-object-key`. This makes these paths
local on the document task:

```text
DerivedUnit -> graduation -> container -> revision -> outline node -> composition edge
```

`source/ingest` is the one write path that also touches `source-ref-key` indexes. Those writes are
idempotent and separated from document-local materialization.

### No-Read Writes

When the topology has already computed a complete row, it should write:

```clojure
(local-transform> [(keypath *id) (termval *row)] $$pstate)
```

Do not use a `multi-path` field update for full-row writes; that causes an avoidable read of the existing
row.

Read-before-write is required only for:

```text
dedupe/idempotency checks
source already exists check
unit exists check
graduation exists check
container current revision lookup
edit stale-order check
```

### Re-Ingest and Drift

Re-ingesting the same `source/ref` with a new hash creates a new SourceArtifact/document version. It does
not write to old unit/container/revision rows because their IDs contain the old `object-key`.

Drift detection is deferred. The only Slice 1 guarantee is that re-running a distiller cannot overwrite a
graduated container's authored content.

### Projection Target Refs

Every `OutlineNodeRow` carries a target ref:

```text
not graduated -> {:target/kind :derived-unit, :target/id unit/id}
graduated     -> {:target/kind :object-container, :target/id container/id}
```

The outline node slot id remains stable across graduation. The rendered target changes; the user's place
in the document does not.

## State Primitive Selection

PStates:

```text
$$requests-by-audit-id:
  PState. O(1) write per request. Durable request audit.

$$decisions-by-audit-id:
  PState. O(1) write per request. Durable accepted/rejected decision audit.

$$decisions-by-idempotency:
  PState. O(1) write per request. Needed for retry and duplicate client request safety.

$$events-by-id:
  PState. O(1) write per accepted request. Durable accepted fact audit.

$$source-artifacts-by-id:
  PState. O(1) write per source ingest. Stores immutable raw source text.

$$source-versions-by-ref:
  PState. O(1) write per source ingest. Subindexed version lookup for a logical source ref.

$$source-latest-by-ref:
  PState. O(1) write per source ingest. Latest pointer, no raw text duplication.

$$source-ingest-completions-by-ref:
  PState. O(1) write per completed source ingest. Durable completion marker that prevents successful
  duplicate ingests from rewriting block materialization while allowing partial stream retries to repair
  missing indexes before completion.

$$containers-by-id:
  PState. O(1) write for document create, graduation, and revise. Direct container reads.

$$revision-history-by-container:
  PState. O(1) write per accepted edit. Subindexed range read for history.

$$derived-units-by-id:
  PState. O(number of markdown blocks) writes per source ingest. Durable derived view over immutable source.

$$unit-graduations-by-id:
  PState. O(1) write per accepted edit. Preserves at-most-once graduation.

$$source-anchors-by-target:
  PState. O(number of markdown blocks) writes per source ingest and O(1) on graduation.

$$composition-children-by-parent:
  PState. O(number of composition edges) writes per source ingest; O(1) rewrite on graduation if the child target resolves to a container.

$$composition-parent-by-child:
  PState. O(number of composition edges) writes per source ingest; O(1) rewrite on graduation if needed.

$$outline-by-document:
  PState. O(number of markdown blocks) writes per source ingest; O(1) write per accepted edit. Serves hot outline reads.

$$edit-order-by-target:
  PState. O(1) write per accepted edit. Subindexed inner map prevents stale same-client edit callbacks
  without assuming a bounded number of clients per target.
```

TaskGlobals:

```text
none in Slice 1
```

The markdown distiller is a pure function. It may allocate ordinary local values while processing one
request, but no non-durable TaskGlobal cache is part of the correctness model.

External systems:

```text
none in Slice 1
```

The source raw text is supplied in the `source/ingest` request. File watching, filesystem indexing, and
external storage are outside this slice.

## Phase 3 Implementation File Checklist

Phase 3 should create:

```text
src/app/server/rama/object_container.clj
```

Phase 3 should expose helper functions analogous to the existing text/space module helpers:

```text
source-ingest-request
object-edit-request
start-object-container-runtime!
close-object-container-runtime!
append-object-container-request!
read-source
read-latest-source-by-ref
read-outline
read-container
read-revision-history
read-unit
await-object-container-decision
```

`await-object-container-decision` must accept the original request or `(partition/key, request/id)` and
read `$$decisions-by-audit-id[audit/id]`. It must not expose a bare `request/id` lookup because
`request/id` alone does not identify the PState partition.

Phase 5 should create:

```text
test/app/server/rama/object_container_test.clj
```

Minimum invariant tests:

```text
1. re-ingest same ref/hash is idempotent
2. edit derived unit does not mutate SourceArtifact raw text
3. edit derived unit creates exactly one ObjectContainer
4. edit already-graduated unit revises same container
5. SourceAnchor survives graduation
6. outline shows current revision after graduation
7. composition/order survives graduation
8. re-ingest cannot overwrite graduated authored content
9. duplicate idempotency key creates no second revision
10. stale same-client edit seq is rejected durably
11. rejected requests are durable decisions
12. empty file and empty edit are valid edge cases
```
