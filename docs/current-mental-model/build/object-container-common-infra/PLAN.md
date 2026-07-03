# Plan - Object-Container Common Infra

<!--
Phase 1 Rama artifact. Design only.

Inputs:
- docs/current-mental-model/build/object-container-common-infra/PRODUCT.md
- docs/current-mental-model/build/object-container-common-infra/IMPLICIT_SPEC.md
- docs/current-mental-model/architecture/object-container-ingester-contract.md
- docs/current-mental-model/architecture/object-container-spec.md
- docs/current-mental-model/build/object-container/PLAN.md
- docs/current-mental-model/build/chat-ingester/COMMON_KERNEL_REPLAN.md
- src/app/server/rama/object_container.clj
- src/app/server/rama/dogfood/transcript.clj
- src/app/server/rama/dogfood/transcript_ingest.clj

Do not write topology, ETL, query topology, or test implementation code in
this phase. Phase 2 validates this plan first.
-->

## Contract Restatement

This plan starts the common object-container import infrastructure:

```text
Markdown Ingester      \
                        -> common Object-Container substrate
Transcript Ingester    /
```

The common substrate owns canonical base truth:

```text
SourceArtifact
ObjectContainer
Revision
DerivedUnit
SourceAnchor
CompositionEdge
accepted/rejected/replayed ActionDecision
KernelEvent / import event audit
```

Markdown and transcript may keep source-specific acquisition, parsing, and
projection code. They must not keep source-specific durable identity stores as
the only base truth for imported material.

Current shape:

```text
object_container.clj
  common-ish markdown kernel
  already owns SourceArtifact/ObjectContainer/Revision/DerivedUnit/
  SourceAnchor/CompositionEdge for markdown

transcript_ingest.clj
  source-specific proof
  currently owns TranscriptContainerRow, transcript-local SourceArtifactRow,
  SourceAnchorRow, and CompositionEdgeRow
```

Rama-shaped concern:

```text
TranscriptContainerRow and transcript-local source/anchor/edge rows are a
projection/index shape, not the common base object substrate.
```

Recommended implementation direction for Phase 3:

```text
Extend src/app/server/rama/object_container.clj into the common import owner.
Keep src/app/server/rama/dogfood/transcript.clj parser/acquisition behavior.
Retire or bypass transcript_ingest.clj's transcript-local base container store
for this slice; preserve its run/projection behaviors as common-module
projection/operational PStates, not base identity.
```

This is not a greenfield replacement of source-specific parser knowledge. It
is a boundary correction: transcript observations become common
`ObjectContainerImport` records.

## Slice Boundary

In this slice:

```text
markdown file import
transcript one-time harvest
transcript passive watch
generic ObjectContainerImport materialization
common reads over markdown and transcript containers
common reads over source artifacts, anchors, revisions, derived units, edges
markdown outline projection
transcript conversation/tool/audit/file-offset projections
object/edit for DerivedUnit graduation and ObjectContainer revision
```

Out of this slice:

```text
Roam/outliner import
code/repo import
canvas/tldraw import
Space runtime
LLM continuation runtime
semantic search
active/base layer promotion
view layout state
full migration of old transcript-local data
fixing old transcript F5/F6 inside transcript-local base PStates
```

F5/F6 are carried as requirements:

```text
F5:
  transcript tool_result content becomes a common :tool-result
  ObjectContainer with a common Revision carrying redacted result content.

F6:
  idempotency/source-line completion cannot hide missing common base writes or
  projection writes after partial failure.
```

## ID, Routing, and Ordering Contract

All IDs written by stream topology code are deterministic or request-supplied.
No topology-generated random IDs are allowed.

### Locality Keys

`object/key` is the physical locality key for a local world of imported
material.

Markdown:

```text
source-ref-key = sha256(source/ref)
object/key     = "md:" + sha256(source/ref + "\u0000" + source/hash)
```

Transcript:

```text
conversation-key = source-family + ":" + stable conversation/session id
object/key       = "chat:" + sha256(conversation-key)
```

Transcript source records use the conversation `object/key` as locality even
when their source identity is a file/line/offset key. This keeps messages, tool
calls, tool results, produced edges, and conversation projection rows on one
task per conversation.

### Import Keys

Every import request carries:

```text
:partition/key String
:object/key String
:import/key String
:idempotency/key String
:material/fingerprint String
```

Rules:

```text
markdown file import:
  partition/key = source-ref-key
  object/key    = md object/key
  import/key    = "imp:md:" + source-ref-key + ":" + source/hash

transcript source-record import:
  partition/key = transcript object/key
  object/key    = transcript object/key
  import/key    = "imp:tr:" + object/key + ":" + source-line-key-hash

object/edit:
  partition/key = object/key
  object/key    = object/key
```

Markdown starts on `source-ref-key` to serialize versions of the same logical
source ref and protect latest-by-ref indexes. It hops to `object/key` for base
materialization, returns to `source-ref-key` to write ref indexes, then
explicitly routes to the object, audit, import-completion, and idempotency
partitions for their respective writes.

Transcript starts on `object/key` because conversation order and tool
production locality matter more than request id locality. Request id is an
audit/index key, not physical locality.

### Deterministic IDs

Representative IDs:

```text
source/id:
  markdown   = "src:" + object/key
  transcript = "src:tr:" + object/key + ":" + source-line-key-hash

document container/id:
  "oc:document:" + object/key

chat conversation container/id:
  "oc:chat-conversation:" + object/key

chat message container/id:
  "oc:chat-message:" + object/key + ":" + message-native-key

tool call container/id:
  "oc:tool-call:" + object/key + ":" + tool-use-id-hash

tool result container/id:
  "oc:tool-result:" + object/key + ":" + tool-result-key

chat artifact container/id:
  "oc:chat-artifact:" + object/key + ":" + artifact-native-key-hash

agent/execution run container/id:
  "oc:run:" + object/key + ":" + run-native-key-hash

native identity claim/key:
  container/id

derived unit/id:
  "du:" + object/key + ":" + distiller-id + ":" + block-path

text-block container/id after graduation:
  "oc:text-block:" + object/key + ":" + unit-local-id

revision/id:
  import   = "rev:" + object/key + ":" + target-id-hash + ":" + import/key-hash
  edit     = request-supplied revision/id or "rev:" + object/key + ":" + request/id

source-anchor/id:
  "sa:" + object/key + ":" + target-id-hash + ":" + anchor-hash

source/file-generation-key:
  sha256(source/family + "\u0000" + source/file-key + "\u0000" +
         source/file-id-or-empty + "\u0000" +
         source/file-stat-fingerprint + "\u0000" + policy/version)

source-line/key:
  "line:" + sha256(source/file-generation-key) + ":" +
  zero-padded-byte-offset + ":" + line-hash

source-line/order-key:
  source/file-generation-key + ":" + zero-padded-byte-offset + ":" +
  source-line-key-hash

composition-edge/id:
  "ce:" + object/key + ":" + edge-kind + ":" + parent-id-hash + ":" + child-id-hash

event/id:
  "evt:" + object/key + ":" + request/id

audit/id:
  partition/key + "/request/" + request/id
```

IDs embed enough locality for `partition-by-object-key` or
`partition-by-import-key` to route single-key reads to the correct task.

`source/file-stat-fingerprint` is a file-generation identity fingerprint, not
the current append length. Prefer device/inode/birthtime or platform file id.
When file id is unavailable, use a deterministic stable fingerprint such as
path/ref plus first-observed prefix hash and creation metadata when available.
Current length and mtime are stored on `TranscriptFileOffsetSchema` and compared
separately so ordinary append growth does not create a new generation, while
truncation, replacement, or path reuse can still be detected.

### Material Fingerprint

Every request computes a deterministic `material/fingerprint` before
idempotency decisions. The fingerprint is a SHA-256 over the canonical logical
input, excluding `request/id`, `audit/id`, and wall-clock timestamps so a true
retry can replay. It includes:

```text
request/type
partition/key
object/key
import/key for imports
target/edit lineage for edits
actor and policy/visibility
source family/format/ref/hash/version
stable material ids
content hashes
anchor hashes
edge ids and order keys
projection hint hashes that completion certifies
```

`$$decisions-by-idempotency` stores the accepted or rejected decision row with
the fingerprint. On an idempotency hit, Rama compares the prior fingerprint to
the current fingerprint before checking completion. A mismatch writes a rejected
conflict decision for the current audit id and does not materialize common base
rows or overwrite the prior idempotency row. Only a matching fingerprint can
take the replay or repair path.

### Completion Rule

`$$import-completions-by-key[import/key]` is the only completion marker that
allows an import to short-circuit materialization.

The completion row is written after:

```text
common base rows
common source indexes
common anchors/edges
source-specific projection rows required by this slice
transcript last-message state, when applicable
audit rows
```

If a retry sees base rows but no completion row, it must repair all missing
rows and then write completion. A source-line ledger or file offset never
short-circuits common base/projection repair.

### Current Revision Rule

Every accepted `ObjectContainerSchema` row in this slice must have:

```text
:current-revision/id
a matching RevisionSchema row in $$revisions-by-id
a matching latest entry in $$revision-history-by-container
```

There is no accepted nil-current-revision container path in this slice.
Document and conversation containers that do not have user-visible body text
still receive an initial revision whose payload is the document/conversation
metadata, redacted summary, or content handle chosen by the source policy.
Empty markdown files receive an empty-document initial revision. Future
content-handle-only policies can extend this rule, but this plan chooses the
stronger invariant: accepted container identity and current revision are
materialized together.

## Reads

### R1. `read-source-artifact`

By source id:

```clojure
(foreign-select-one [(keypath source-id)] $$source-artifacts-by-id)
```

Access method: direct `foreign-select-one`.

Partition: `partition-by-object-key(source-id)`.

Cost: 1 seek.

By `(source/ref, source/hash/version)`:

```text
markdown:
  use query topology read-source-by-ref-version when only source metadata is known

transcript source record:
  use query topology read-source-by-ref-version when only file/source metadata
  is known
```

W1 writes `$$source-versions-by-ref` for every SourceArtifact that participates
in exact ref/version lookup, including transcript source records. Latest source
by ref uses query topology `read-latest-source-by-ref` only for source families
with an explicit latest policy. In this slice, markdown whole-file imports update
latest-by-ref. Transcript source records support exact ref/version lookup and do
not imply a latest source for the whole transcript file unless a future source
policy opts in.

### R2. `read-object-container`

```clojure
(foreign-select-one [(keypath container-id)] $$containers-by-id)
```

Access method: direct `foreign-select-one`.

Partition: `partition-by-object-key(container-id)`.

Cost: 1 seek.

This one read must work for:

```text
markdown :document
markdown graduated :text-block
transcript :chat-conversation
transcript :chat-message
transcript :tool-call
transcript :tool-result
transcript :chat-artifact
transcript :agent-run / :execution-run
```

The row includes current revision id/content summary for the common
container card. Full current revision row uses R3.

### R3. `read-current-revision` and `read-revision-history`

Full current revision by container id uses query topology
`read-current-revision`:

```text
read $$containers-by-id[container/id]
if container is missing:
  return nil
if current-revision/id is missing:
  return invalid accepted state / repair-needed
read $$revisions-by-id[revision/id]
return RevisionSchema
```

Access method: query topology because the caller supplies container id, but
the full answer may require two PStates.

History range:

```clojure
(foreign-select [(keypath container-id)
                 (sorted-map-range-from cursor limit)
                 MAP-VALS]
                $$revision-history-by-container)
```

Access method for history: direct range `foreign-select`.

Cost for M revisions: 1 seek + M sequential iterator reads.

### R4. `read-derived-unit`

Use query topology `read-derived-unit` because the answer combines
`DerivedUnitSchema` with optional `UnitGraduationSchema`.

```text
missing unit:
  read derived unit -> nil
  1 meaningful read

existing not graduated:
  read derived unit
  read graduation -> nil
  2 meaningful reads

graduated:
  read derived unit
  read graduation -> UnitGraduationSchema
  2 meaningful reads
```

Variable reads. Use `<<if` after the derived-unit read and only read
graduation when the unit exists.

### R5. `read-source-anchors-by-target`

```clojure
(foreign-select [(keypath target-id)
                 (sorted-map-range-from cursor limit)
                 MAP-VALS]
                $$source-anchors-by-target)
```

Access method: direct range `foreign-select`.

Partition: `partition-by-object-key(target-id)`.

Cost for A anchors: 1 seek + A sequential iterator reads.

This replaces the current singleton-anchor shape. The plural shape is required
because a native object can later acquire more than one provenance anchor.

### R6. `read-composition-children` and `read-composition-parents`

Children by parent:

```clojure
(foreign-select [(keypath parent-id)
                 (sorted-map-range-from cursor limit)
                 MAP-VALS]
                $$composition-children-by-parent)
```

Parents by child:

```clojure
(foreign-select [(keypath child-id)
                 (sorted-map-range-from cursor limit)
                 MAP-VALS]
                $$composition-parents-by-child)
```

Access method: direct range `foreign-select`.

Partition: `partition-by-object-key(parent-id or child-id)`.

Cost for E edges: 1 seek + E iterator reads.

The same read family traverses markdown document/block structure and transcript
conversation/message/tool/result structure.

### R7. `read-import-decision` and `read-import-audit`

Request:

```clojure
(foreign-select-one [(keypath audit-id)] $$requests-by-audit-id)
```

Decision:

```clojure
(foreign-select-one [(keypath decision-id)] $$decisions-by-audit-id)
```

Access method: direct `foreign-select-one` when caller has `(partition/key,
request/id)`.

Partition: `partition-by-audit-id(audit/id)`.

Cost: 1 seek each.

Audit entries for transcript runs:

```clojure
(foreign-select [(keypath request-id)
                 (sorted-map-range-from cursor limit)
                 MAP-VALS]
                $$transcript-audit-by-request)
```

Access method: direct range `foreign-select`.

### R8. `read-common-material-for-source`

For one category at a time, use direct range reads:

```clojure
$$source-containers-by-source[source-id][order-key]
$$source-derived-units-by-source[source-id][order-key]
$$source-anchors-by-source[source-id][order-key]
$$source-edges-by-source[source-id][order-key]
```

Access method: direct `foreign-select` range.

For a bundled response, use query topology `read-common-material-for-source`
to read the requested categories in one roundtrip.

Examples:

```text
categories = [:containers]
  1 meaningful range read

categories = [:containers :anchors :edges]
  3 meaningful range reads

categories = []
  0 PState reads, returns empty bundle
```

Variable reads. Use `ops/explode` over requested categories and aggregate the
category pages; do not issue empty category reads.

### R9. `read-markdown-outline-projection`

```clojure
(foreign-select [(keypath document-id)
                 (sorted-map-range-from cursor limit)
                 MAP-VALS]
                $$markdown-outline-by-document)
```

Access method: direct range `foreign-select`.

Partition: `partition-by-object-key(document-id)`.

Cost for N visible nodes: 1 seek + N sequential iterator reads.

The row carries target refs:

```text
not graduated -> derived-unit id + derived content
graduated     -> object-container id + current revision content
```

### R10. `read-transcript-conversation-projection`

```clojure
(foreign-select [(keypath conversation-container-id)
                 (sorted-map-range-from cursor limit)
                 MAP-VALS]
                $$transcript-conversation-projection)
```

Access method: direct range `foreign-select`.

Partition: `partition-by-object-key(conversation-container-id)`.

Cost for N entries: 1 seek + N sequential iterator reads.

Projection rows carry common container ids, revision ids, source anchor ids,
and parse-error/audit refs. A parse-error entry may have no common container
id and must be visibly non-canonical.

### R11. `read-transcript-operational-views`

Run status:

```clojure
(foreign-select-one [(keypath request-id)] $$transcript-runs)
```

File offset:

```clojure
(foreign-select-one [(keypath file-key)] $$transcript-file-offsets)
```

Source-line repair ledger:

```clojure
(foreign-select [(keypath file-key)
                 (sorted-map-range-from source-line-order-cursor limit)
                 MAP-VALS]
                $$transcript-source-lines-by-file)
```

Tool calls by name:

```clojure
(foreign-select [(keypath tool-name)
                 (sorted-map-range-from cursor limit)
                 MAP-VALS]
                $$transcript-tool-calls-by-name)
```

Audit by request: same path as R7.

Access method: direct lookups/range reads. None of these reads proves base
identity. Source-line status is an operational repair ledger and file-offset
gate; base materialization is still proven by common SourceArtifact,
ObjectContainer, Revision, SourceAnchor, CompositionEdge, and import-completion
rows. File offsets expose resume/status and source/file-generation-key so watch
startup can distinguish safe resume from stale-after-file-change. Tool indexes
point to common tool-call/tool-result containers.

## Writes

### W1. `object-container/import-material`

Depot record type:

```text
:request/type :object-container/import-material
```

Depot:

```text
*object-container-requests-depot
```

Required envelope:

```text
request/id
request/type
partition/key
object/key
import/key
routing/key
idempotency/key
actor
policy/visibility
payload ObjectContainerImportPayload
requested-at-ms
material/fingerprint
```

Payload contains vectors of typed material records:

```text
source artifacts
object containers
revisions
derived units
source anchors
composition edges
projection hints
```

Accepted event types:

```text
:object-container/imported-source
:object-container/imported-source-record
```

Validation:

```text
reject missing request/id, import/key, object/key, partition/key
reject missing material/fingerprint
reject missing actor/policy when required
reject unsupported source family/format
reject SourceArtifact rows without source/ref, source hash or source version,
  source family/format, capture mode, or provenance metadata required by policy
reject unredacted transcript capture when policy forbids it
reject containers without kind or stable id
reject missing required artifact/run containers when transcript source exposes
  artifact, attachment, output, agent-run, or execution-run identity
reject accepted containers without current-revision/id
reject accepted containers whose current-revision/id is missing from payload
reject revisions without container id/content hash
reject anchors without source id, target id, target kind, and anchor/hash or
  line hash
build source/provenance closure for the payload before common base row writes
reject or mark repair-pending source-created ObjectContainers or DerivedUnits
  without at least one SourceAnchor in the payload or accepted durable anchor
  state
reject or mark repair-pending SourceAnchor rows whose source/id does not resolve
  to a SourceArtifact in the payload or accepted durable source state
reject or mark repair-pending SourceAnchor rows whose target/id does not resolve
  to an ObjectContainer or DerivedUnit in the payload or accepted durable target
  state
reject non-repairable edges whose targets are missing
reject parse errors represented as fake base containers
reject tool-result placeholders when result content exists
build incoming NativeIdentityClaim candidates for every source-native
  ObjectContainer in the payload before checking PStates
reject or mark repair-pending duplicate incoming native-id claims with the same
  claim/key or container/id when source-native id, source-line/provenance,
  anchor hash, content hash, material/fingerprint, or import/key differs
canonicalize deterministic exact-duplicate incoming native-id claims before any
  common base row write
reject or mark repair-pending duplicate native-id claims whose material,
  source-line/provenance, or content hash conflicts with the accepted claim
reject same idempotency key with different material/fingerprint
```

Materialization:

```text
compute material/fingerprint from canonical logical input
write request audit row
check idempotency decision by partition/key + idempotency/key
if prior decision exists:
  compare prior material/fingerprint to current material/fingerprint
  if different: hop to audit partition and write rejected conflict decision for
                this audit id only without overwriting idempotency
  if same and prior accepted: verify import completion by prior/current import/key
  if same and prior rejected: hop to audit partition and write replay decision
                              for this audit id only
check import completion by import/key
if complete:
  compare completion material/fingerprint to current material/fingerprint
  if different: hop to audit partition and write rejected conflict decision for
                this audit id only without writing idempotency
  if same: ensure event row exists on object-key partition, write duplicate
           decision on audit partition, repair idempotency row on partition/key
           when absent
if not complete:
  construct a deterministic incoming native-identity claim set for all
    import-created source-native ObjectContainers before checking PStates
  if duplicate incoming claims for the same claim/key or container/id differ in
    source-native id, source-line/provenance, anchor hash, content hash,
    material/fingerprint, or import/key:
    hop to audit partition and write rejected or repair-pending conflict decision
    without overwriting common base rows
  canonicalize exact-duplicate incoming native claims deterministically
  check accepted native identity claims in PState for every canonical incoming
    claim before overwriting any common base rows
  if a native claim exists with different content/provenance/material:
    hop to audit partition and write rejected or repair-pending conflict decision
    without overwriting common base rows
  construct source/provenance closure from payload SourceArtifacts,
    source-created ObjectContainers, DerivedUnits, and SourceAnchors
  point-read referenced durable SourceArtifact/ObjectContainer/DerivedUnit/
    SourceAnchor rows only when a required source/target is not in the payload or
    an existing accepted anchor must prove a payload source-created target
  if any source-created container or derived unit lacks an accepted SourceAnchor,
    any SourceAnchor source/id lacks a SourceArtifact, or any SourceAnchor
    target/id lacks common material:
    hop to audit partition and write rejected or repair-pending provenance
    decision without overwriting common base rows, writing import completion,
    writing transcript source-line :import-complete, or writing accepted
    idempotency
  write common source artifacts
  write common containers
  write common revisions and revision history entries
  write native identity claims for canonical source-native ObjectContainers
  write common derived units
  write common source anchors
  write common composition edges and pending edge repair rows
  write source enumeration indexes
  write exact source ref/version indexes for every participating SourceArtifact
  write markdown outline projection hints
  write transcript conversation/tool/artifact/audit projection hints
  write transcript last-message state when the record has a message
  write accepted event row on the object-key partition
  write accepted decision row on the audit partition
  write import completion after every certified row exists
  write or repair transcript source-line completion status when applicable
  write accepted idempotency row after completion exists
```

All row writes are `keypath + termval` full-row writes. No counters, vector
appends, random ids, or read-modify-write increments.

### W2. `markdown/import-source`

This is an adapter operation, not a separate base substrate.

The markdown adapter may reuse the existing `markdown-block-v0` distiller but
must emit an `ObjectContainerImportPayload` and append W1.

Source behavior:

```text
raw markdown file -> SourceArtifact
document/file     -> :document ObjectContainer + initial RevisionSchema
heading/paragraph/list blocks -> DerivedUnit
source spans      -> SourceAnchor
block tree/order  -> CompositionEdge
outline rows      -> projection hints
```

The document ObjectContainer always carries a current revision. For non-empty
files, the initial revision payload contains the document-level redacted/raw
content handle and source summary chosen by policy. For empty markdown files,
the adapter emits an empty-document initial revision rather than accepting a
container with nil `:current-revision/id`.

The markdown adapter must emit a SourceArtifact for the imported file and
SourceAnchor rows for the document ObjectContainer and every DerivedUnit it
emits. Anchor targets must be the document/container id or derived unit id in the
same payload unless the adapter is intentionally adding an anchor to already
accepted durable common material. W1 rejects or marks repair-pending any markdown
payload whose source-created document or DerivedUnit cannot be traced through a
SourceAnchor to a common SourceArtifact.

Depot record:

```text
:request/type :object-container/import-material
:source/family :markdown
:source/format :markdown
```

This replaces treating `:source/ingest` as a markdown-only kernel request.
Phase 3 may keep the old helper as compatibility sugar if it constructs the
same import payload and uses the same materializer.

### W3. `transcript/harvest`

Depot record type:

```text
:request/type :transcript/harvest
```

Depot:

```text
*transcript-control-depot
```

Purpose:

```text
record the run request/status and let application-side acquisition walk files,
parse/redact records, and append W1 records for each complete source record
```

This write does not create base ObjectContainers. It creates/updates:

```text
$$transcript-runs[request-id]
```

The existing `transcript.clj` functions are preserved as acquisition/parser
helpers:

```text
walk-jsonl-files
read-jsonl-observations
read-complete-appended-lines
transcript-conversation-id
transcript-message-uuid
parsed-tool-use-blocks
redacted-preview / redaction behavior
source-line-key
file-id
```

### W4. `transcript/watch`

Depot record type:

```text
:request/type :transcript/watch
```

Depot:

```text
*transcript-control-depot
```

Purpose:

```text
record/passively maintain a watch run and drive application-side polling that
appends W1 for each newly complete source record
```

It does not spawn Claude/Codex and does not modify source files.

### W5. `transcript/observe-record`

This is implemented as W1 with:

```text
:source/family :transcript
:import/mode :one-time-import or :passive-watch
:request/type :object-container/import-material
```

The transcript adapter interprets one parsed/redacted source record into common
material:

```text
source-record SourceArtifact
exact SourceVersion row for source/ref + source/hash/version lookup
conversation ObjectContainer + initial RevisionSchema
message ObjectContainer + initial RevisionSchema, when record is a message
tool-call ObjectContainer(s) + initial RevisionSchema, when tool_use blocks exist
tool-result ObjectContainer(s) + initial RevisionSchema, when tool_result content exists
chat-artifact ObjectContainer(s) + initial RevisionSchema, when artifact,
  attachment, or output identity exists
agent-run or execution-run ObjectContainer + initial RevisionSchema, when the
  source distinguishes a run from the conversation
NativeIdentityClaim rows for each transcript source-native ObjectContainer
Revision rows with redacted content/structured payload
SourceAnchor rows to file/byte/line/hash/source-native id
CompositionEdge rows for contains/follows/produced/derived-from, including
  message-produced-artifact and artifact-derived-from-source edges when declared
TranscriptProjectionHint rows for conversation/tool/artifact/audit views
```

Artifact and run objects follow the same current-revision rule as messages and
tools. Their revisions contain redacted artifact/output content or structured
metadata according to the selected capture policy. If source policy exposes an
artifact/run identity but redaction removes all body content, the revision still
stores a redacted summary/hash payload rather than accepting a nil-current
container.

The transcript adapter must emit at most one canonical NativeIdentityClaim
candidate per source-native container id in a W1 payload. If one parsed source
record contains two blocks with the same source-native object id, the adapter may
dedupe only when the derived claim is byte-for-byte equivalent for source-native
id, source-line/provenance hash, anchor hash, content hash, material/fingerprint,
and import/key. If the duplicate claims differ, W1 rejects or marks the import
repair-pending before any common base row, completion row, source-line
`:import-complete` row, or accepted idempotency row is written.

For every valid transcript source record, the transcript adapter must emit the
source-record SourceArtifact and SourceAnchor rows for every source-created
conversation, message, tool-call, tool-result, chat-artifact, and run container
introduced by that record. Each anchor's `source/id` must resolve to the
source-record SourceArtifact in the same payload or to an accepted durable
SourceArtifact explicitly read by W1. Each anchor's `target/id` must resolve to a
payload or accepted durable common ObjectContainer/DerivedUnit. W1 rejects or
marks repair-pending transcript payloads that would make R5/R1 provenance reads
depend on private reconstruction from projections or parser-local rows.

Parse-error source records emit:

```text
SourceArtifact according to policy
exact SourceVersion row when a SourceArtifact is emitted
TranscriptAudit/ConversationProjection parse-error hints
no fake ObjectContainer
no fake Revision
no fake SourceAnchor to a fake target
```

### W6. `transcript/file-state-observation`

Depot record type:

```text
:request/type :transcript/file-state
```

Depot:

```text
*transcript-file-state-depot
```

Writes:

```text
$$transcript-file-offsets[file-key] = TranscriptFileOffsetSchema
$$transcript-source-lines-by-file[file-key][source-line/order-key] =
  TranscriptSourceLineStatusSchema
optional empty-file audit projection
optional empty-file SourceArtifact through W1 if policy says the empty source
is an imported source artifact
```

File offset state is operational. It cannot prove base materialization and must
not advance the safe resume offset past a complete source line until W1 has an
import completion row for that line's `import/key`, or an explicit
parse-error-complete audit row says no base container materialization was
expected. Watch restart reads the source-line status ledger before skipping an
observed line; incomplete or missing completion status is repaired by reappending
W1 or by writing the explicit parse-error audit state before offset advancement.

File resume is generation-aware. The acquisition helper must include the current
`source/file-generation-key`, current file length, file id when available,
policy/version, and a deterministic file-stat fingerprint. If the saved
TranscriptFileOffsetSchema row is for a different generation, a shorter current
file length, a changed file id/fingerprint, or a prior policy/version, W6 writes
`:resume/status :stale-after-file-change` or
`:unsafe-resume-rejected` and does not skip ahead from the stale offset. The next
watch/harvest scan starts at byte 0 for the new generation or fails visibly when
policy says unsafe rescan is not allowed. Old source-line ledger rows remain
under their old generation-prefixed order keys, so byte offset 0 in a replacement
file cannot collide with byte offset 0 from the previous file generation.

### W7. `object/edit`

Depot record type:

```text
:request/type :object/edit
```

Depot:

```text
*object-container-requests-depot
```

This preserves the existing common edit lifecycle:

```text
DerivedUnit edit -> graduate once into :text-block ObjectContainer + first Revision
ObjectContainer edit -> append Revision and advance current revision pointer
```

It must work for future edits to transcript-created containers too, but the
Phase 3 test focus remains markdown DerivedUnit graduation and common revision
behavior.

### W8. `source-specific/projection-upsert`

Projection writes are not client appends in this slice. They are materialized
inside W1 from typed projection hints after common base rows are ensured.

Projection classes:

```text
MarkdownOutlineHint
TranscriptConversationHint
TranscriptToolCallHint
TranscriptArtifactHint
TranscriptAuditHint
```

If a projection row is missing after a partial failure, absence of
`ImportCompletionSchema` forces W1 retry to repair it. If completion exists,
projection rows are part of the accepted complete state.

## PState Design

### Schema Types

Phase 3 should define concrete typed schemas. No PState schema should use
`Object`.

Uniform record-like PState values use `fixed-keys-schema` because every row at
that PState position has the same fields. Phase 3 may still use constructor
helpers, but values stored into these PStates should be maps matching the
schema, not whole-row defrecord classes.

Polymorphic positions use `definterface` + `defrecord` because different
instances at the same position have different fields.

Common uniform fixed-key schemas:

```text
ActorSchema =
  fixed-keys-schema {:actor/id String
                     :actor/type clojure.lang.Keyword}

TargetRefSchema =
  fixed-keys-schema {:target/kind clojure.lang.Keyword
                     :target/id String
                     :target/address String}

ObjectContainerRequestSchema =
  fixed-keys-schema {:audit/id String
                     :partition/key String
                     :object/key String
                     :import/key String
                     :request/id String
                     :request/type clojure.lang.Keyword
                     :routing/key String
                     :idempotency/key String
                     :material/fingerprint String
                     :actor ActorSchema
                     :target TargetRefSchema
                     :payload IObjectContainerRequestPayload
                     :requested-at-ms Long}

ObjectContainerDecisionSchema =
  fixed-keys-schema {:decision/id String
                     :audit/id String
                     :partition/key String
                     :request/id String
                     :request/type clojure.lang.Keyword
                     :idempotency/key String
                     :material/fingerprint String
                     :status clojure.lang.Keyword
                     :reason clojure.lang.Keyword
                     :conflict-with-decision/id String
                     :event/id String
                     :import/completion-key String
                     :decided-at-ms Long
                     :replayed-from-decision/id String}

ObjectContainerEventSchema =
  fixed-keys-schema {:event/id String
                     :event/type clojure.lang.Keyword
                     :object/key String
                     :target/kind clojure.lang.Keyword
                     :target/id String
                     :actor/id String
                     :payload IObjectContainerEventPayload
                     :event-time-ms Long
                     :request/id String
                     :audit/id String}

SourceArtifactSchema =
  fixed-keys-schema {:source/id String
                     :object/key String
                     :source/ref String
                     :source/hash String
                     :source/version String
                     :source/family clojure.lang.Keyword
                     :source/format clojure.lang.Keyword
                     :source/capture-mode clojure.lang.Keyword
	                     :source/captured-text String
	                     :source/file-id String
	                     :source/file-path String
	                     :source/file-generation-key String
	                     :source/file-length Long
	                     :source/file-stat-fingerprint String
	                     :source/byte-offset Long
	                     :source/byte-length Long
                     :source/line-hash String
                     :created-at-ms Long
                     :created-by String
                     :event/id String}

SourceVersionSchema =
  fixed-keys-schema {:source/ref-key String
                     :source/ref String
                     :source/version-key String
                     :source/hash String
                     :source/id String
                     :object/key String
                     :source/has-container Boolean
                     :container/id String
                     :order/key String
                     :created-at-ms Long
                     :event/id String}

ImportCompletionSchema =
  fixed-keys-schema {:import/key String
                     :object/key String
                     :source/id String
                     :material/fingerprint String
                     :event/id String
                     :decision/id String
	                     :containers/count Long
	                     :native-claims/count Long
	                     :derived-units/count Long
                     :anchors/count Long
                     :edges/count Long
                     :projections/count Long
                     :completed-at-ms Long
                     :completed-by-request/id String}

ObjectContainerSchema =
  fixed-keys-schema {:container/id String
                     :container/kind clojure.lang.Keyword
                     :object/key String
                     :visibility clojure.lang.Keyword
                     :source/id String
                     :source/unit-id String
                     :current-revision/id String
                     :current/content-text String
                     :current/content-hash String
                     :created-at-ms Long
                     :created-by String
                     :event/id String}

RevisionSchema =
  fixed-keys-schema {:revision/id String
                     :container/id String
                     :parent-revision/id String
                     :content/text String
                     :content/hash String
                     :content/payload IRevisionPayload
                     :order/key String
                     :created-at-ms Long
                     :created-by String
                     :event/id String}

DerivedUnitSchema =
  fixed-keys-schema {:unit/id String
                     :object/key String
                     :container/document-id String
                     :source/id String
                     :unit/kind clojure.lang.Keyword
                     :source/span-key String
                     :source/start-offset Long
                     :source/end-offset Long
                     :distiller/id String
                     :distiller/version Long
                     :derived/content-text String
                     :derived/content-hash String
                     :event/id String}

UnitGraduationSchema =
  fixed-keys-schema {:unit/id String
                     :container/id String
                     :current-revision/id String
                     :current/content-text String
                     :current/content-hash String
                     :graduated-at-ms Long
                     :last-revised-at-ms Long
                     :event/id String}

SourceAnchorSchema =
  fixed-keys-schema {:source-anchor/id String
                     :object/key String
                     :target/kind clojure.lang.Keyword
                     :target/id String
                     :source/id String
                     :source/ref String
                     :source/hash String
	                     :source/native-id String
	                     :source/file-id String
	                     :source/file-path String
	                     :source/file-generation-key String
	                     :source/byte-offset Long
                     :source/byte-length Long
                     :source/line-hash String
                     :source/json-pointer String
                     :anchor/hash String
                     :event/id String}

CompositionEdgeSchema =
  fixed-keys-schema {:edge/id String
                     :object/key String
                     :edge/kind clojure.lang.Keyword
                     :parent/kind clojure.lang.Keyword
                     :parent/id String
                     :child/kind clojure.lang.Keyword
                     :child/id String
                     :order/key String
                     :source/id String
                     :source-anchor/id String
                     :event/id String}

PendingCompositionEdgeSchema =
  fixed-keys-schema {:edge/id String
                     :object/key String
                     :missing-target/id String
                     :edge/kind clojure.lang.Keyword
                     :parent/id String
                     :child/id String
                     :order/key String
                     :source/id String
                     :created-at-ms Long
                     :event/id String}

SourceMaterialRefSchema =
  fixed-keys-schema {:source/id String
                     :object/key String
                     :target/kind clojure.lang.Keyword
                     :target/id String
                     :order/key String
                     :event/id String}

NativeIdentityClaimSchema =
  fixed-keys-schema {:claim/key String
                     :container/id String
                     :container/kind clojure.lang.Keyword
                     :object/key String
                     :source/native-id String
                     :source/id String
                     :source-line/key String
                     :source-anchor/id String
                     :content/hash String
                     :anchor/hash String
                     :material/fingerprint String
                     :import/key String
                     :claim/status clojure.lang.Keyword
                     :event/id String
                     :accepted-at-ms Long}

EditOrderSchema =
  fixed-keys-schema {:lineage/key String
                     :edit/client-id String
                     :edit/seq Long
                     :idempotency/key String
                     :material/fingerprint String
                     :request/id String
                     :revision/id String
                     :event/id String
                     :accepted-at-ms Long}
```

Projection and operational fixed-key schemas:

```text
MarkdownOutlineNodeSchema =
  fixed-keys-schema {:document/container-id String
                     :node/slot-id String
                     :block/path String
                     :parent/slot-id String
                     :target/kind clojure.lang.Keyword
                     :target/id String
                     :source-anchor/id String
                     :content/text String
                     :content/hash String
                     :graduated Boolean
                     :container/id String
                     :event/id String}

TranscriptIngestRunSchema =
  fixed-keys-schema {:request/id String
                     :request/type clojure.lang.Keyword
                     :status clojure.lang.Keyword
                     :source/family clojure.lang.Keyword
                     :paths-hash String
                     :redaction-policy clojure.lang.Keyword
                     :triggered-by String
                     :observed-line-count Long
                     :parse-error-count Long
                     :containers-created-count Long
                     :created-at-ms Long
                     :updated-at-ms Long
                     :error String}

TranscriptFileOffsetSchema =
  fixed-keys-schema {:source/file-key String
                     :source/file-id String
                     :source/file-path String
                     :source/file-generation-key String
                     :source/file-length Long
                     :source/file-mtime-ms Long
                     :source/file-stat-fingerprint String
                     :source/family clojure.lang.Keyword
                     :policy/version String
                     :conversation/container-id String
                     :last-byte-offset Long
                     :observed-byte-offset Long
                     :complete-line-count Long
                     :observed-line-count Long
                     :resume/status clojure.lang.Keyword
                     :unsafe-resume/reason clojure.lang.Keyword
                     :previous/file-generation-key String
                     :repair-needed Boolean
                     :last-repair-source-line/key String
                     :last-completion-key String
                     :last-request/id String
                     :updated-at-ms Long}

TranscriptSourceLineStatusSchema =
  fixed-keys-schema {:source/file-key String
                     :source/file-generation-key String
                     :source-line/key String
                     :source-line/order-key String
                     :object/key String
                     :import/key String
                     :material/fingerprint String
                     :source/byte-offset Long
                     :next-byte-offset Long
                     :line/hash String
                     :status clojure.lang.Keyword
                     :import/completion-key String
                     :request/id String
                     :updated-at-ms Long}

TranscriptToolCallIndexSchema =
  fixed-keys-schema {:tool/name String
                     :tool/use-id String
                     :tool-call/container-id String
                     :tool-result/container-id String
                     :conversation/container-id String
                     :message/container-id String
                     :source-line/key String
                     :timestamp-ms Long
                     :redacted-input-preview String}

TranscriptAuditEntrySchema =
  fixed-keys-schema {:source-line/key String
                     :request/id String
                     :event/type clojure.lang.Keyword
                     :parse-error/kind clojure.lang.Keyword
                     :redactions/count Long
                     :containers/count Long
                     :container-ids-hash String
                     :file/path String
                     :byte-offset Long
                     :processed-at-ms Long
                     :import/completion-key String}

TranscriptLastMessageSchema =
  fixed-keys-schema {:conversation/container-id String
                     :message/container-id String
                     :source-line/key String
                     :order/key String
                     :updated-at-ms Long}
```

`TranscriptSourceLineStatusSchema :status` values in this slice are
`:observed`, `:import-complete`, `:parse-error-complete`, and
`:repair-needed`. File offsets may advance only through `:import-complete`
statuses whose completion row exists, or `:parse-error-complete` statuses with
an audit row that explicitly says no base container was expected.

`TranscriptFileOffsetSchema :resume/status` values in this slice are `:safe`,
`:repair-needed`, `:stale-after-file-change`, and `:unsafe-resume-rejected`.
When the current file id, file-stat fingerprint, policy/version, or current
length contradicts the saved row, the file-state branch must write one of the
unsafe statuses instead of advancing from the stale offset.

Polymorphic payload/projection interfaces:

```text
IObjectContainerRequestPayload
IObjectContainerEventPayload
IImportedProjectionHint
ITranscriptConversationEntry
IRevisionPayload
```

Use interfaces + defrecords for polymorphic payload/projection values rather
than storing heterogeneous maps under a broad schema.

`SourceVersionSchema :container/id` points at the primary ObjectContainer for
normal source artifacts. For parse-error source artifacts that policy keeps as
source truth without a base container, `:source/has-container` is false and
`:container/id` is an empty string sentinel. That sentinel is never interpreted
as a fake ObjectContainer id.

`NativeIdentityClaimSchema :claim/status` values in this slice are `:accepted`
and `:repair-pending`. Accepted claims are the durable guard against two
different source records silently overwriting the same source-native
ObjectContainer. A rejected conflict is an ActionDecision/audit row, not an
overwrite of the prior accepted claim. The prior accepted claim remains the
repair/replay pointer.

Before consulting or writing `$$native-identity-claims-by-container`, W1 builds a
request-local deterministic map of incoming claim candidates keyed by
`:claim/key` / `:container/id`. This map is not durable state and is not a
TaskGlobal; it is a pure validation result derived from the request payload. If
two candidates for the same key are identical across source-native id,
source-line/provenance hash, source-anchor id, anchor hash, content hash,
material/fingerprint, and import/key, W1 keeps one canonical candidate in stable
input order. If any compared field differs, W1 writes a rejected or
repair-pending decision and stops before base-row overwrites.

W1 also builds a request-local source/provenance closure before common base-row
writes. The closure contains payload SourceArtifact ids, payload source-created
ObjectContainer ids, payload DerivedUnit ids, payload SourceAnchor rows grouped
by target/id, and explicit point reads for any referenced durable
SourceArtifact/ObjectContainer/DerivedUnit/SourceAnchor rows not present in the
payload. This closure is pure validation state, not a PState or TaskGlobal. In
this slice, missing required provenance is not silently accepted: W1 writes a
rejected or repair-pending decision and stops before base rows, import
completion, transcript source-line `:import-complete`, or accepted idempotency.

### Common PStates

```text
$$requests-by-audit-id:
  {String ObjectContainerRequestSchema}
  key-partitioner = partition-by-audit-id

$$decisions-by-audit-id:
  {String ObjectContainerDecisionSchema}
  key-partitioner = partition-by-audit-id

$$decisions-by-idempotency:
  {String (map-schema String ObjectContainerDecisionSchema {:subindex? true})}
  outer key = partition/key
  inner key = idempotency/key

$$events-by-id:
  {String ObjectContainerEventSchema}
  key-partitioner = partition-by-object-key

$$import-completions-by-key:
  {String ImportCompletionSchema}
  key-partitioner = partition-by-import-key

$$source-artifacts-by-id:
  {String SourceArtifactSchema}
  key-partitioner = partition-by-object-key

$$source-versions-by-ref:
  {String (map-schema String SourceVersionSchema {:subindex? true})}
  outer key = source-ref-key
  inner key = source hash/version/order key

$$source-latest-by-ref:
  {String SourceVersionSchema}
  outer key = source-ref-key

$$containers-by-id:
  {String ObjectContainerSchema}
  key-partitioner = partition-by-object-key

$$revisions-by-id:
  {String RevisionSchema}
  key-partitioner = partition-by-object-key

$$revision-history-by-container:
  {String (map-schema String RevisionSchema {:subindex? true})}
  outer key = container/id
  inner key = revision order/key
  key-partitioner = partition-by-object-key

$$derived-units-by-id:
  {String DerivedUnitSchema}
  key-partitioner = partition-by-object-key

$$unit-graduations-by-id:
  {String UnitGraduationSchema}
  key-partitioner = partition-by-object-key

$$source-anchors-by-target:
  {String (map-schema String SourceAnchorSchema {:subindex? true})}
  outer key = target/id
  inner key = anchor/id
  key-partitioner = partition-by-object-key

$$composition-children-by-parent:
  {String (map-schema String CompositionEdgeSchema {:subindex? true})}
  outer key = parent/id
  inner key = child order key + edge id
  key-partitioner = partition-by-object-key

$$composition-parents-by-child:
  {String (map-schema String CompositionEdgeSchema {:subindex? true})}
  outer key = child/id
  inner key = parent id + edge id
  key-partitioner = partition-by-object-key

$$pending-composition-edges-by-missing-target:
  {String (map-schema String PendingCompositionEdgeSchema {:subindex? true})}
  outer key = missing target/container id
  inner key = edge/id
  key-partitioner = partition-by-object-key

$$source-containers-by-source:
  {String (map-schema String SourceMaterialRefSchema {:subindex? true})}
  outer key = source/id
  inner key = order key + container/id
  key-partitioner = partition-by-object-key

$$source-derived-units-by-source:
  {String (map-schema String SourceMaterialRefSchema {:subindex? true})}
  outer key = source/id
  inner key = order key + unit/id
  key-partitioner = partition-by-object-key

$$source-anchors-by-source:
  {String (map-schema String SourceMaterialRefSchema {:subindex? true})}
  outer key = source/id
  inner key = order key + anchor/id
  key-partitioner = partition-by-object-key

$$source-edges-by-source:
  {String (map-schema String SourceMaterialRefSchema {:subindex? true})}
  outer key = source/id
  inner key = order key + edge/id
  key-partitioner = partition-by-object-key

$$native-identity-claims-by-container:
  {String NativeIdentityClaimSchema}
  key-partitioner = partition-by-object-key

$$edit-order-by-target:
  {String (map-schema String EditOrderSchema {:subindex? true})}
  outer key = edit lineage key
  inner key = edit/client-id
  key-partitioner = partition-by-object-key
```

### Projection And Operational PStates

```text
$$markdown-outline-by-document:
  {String (map-schema String MarkdownOutlineNodeSchema {:subindex? true})}
  outer key = document/container-id
  inner key = block/path
  key-partitioner = partition-by-object-key

$$transcript-runs:
  {String TranscriptIngestRunSchema}
  outer key = transcript request/id

$$transcript-file-offsets:
  {String TranscriptFileOffsetSchema}
  outer key = source/file-key

$$transcript-source-lines-by-file:
  {String (map-schema String TranscriptSourceLineStatusSchema {:subindex? true})}
  outer key = source/file-key
  inner key = source-line/order-key

$$transcript-conversation-projection:
  {String (map-schema String ITranscriptConversationEntry {:subindex? true})}
  outer key = conversation container/id
  inner key = order key
  key-partitioner = partition-by-object-key

$$transcript-tool-calls-by-name:
  {String (map-schema String TranscriptToolCallIndexSchema {:subindex? true})}
  outer key = tool name
  inner key = source-line key + tool-use id

$$transcript-audit-by-request:
  {String (map-schema String TranscriptAuditEntrySchema {:subindex? true})}
  outer key = transcript request/id
  inner key = source-line key or sentinel key

$$transcript-last-message-by-conversation:
  {String TranscriptLastMessageSchema}
  outer key = conversation container/id
  key-partitioner = partition-by-object-key
  private? true
```

### Non-Obvious Design Choices

#### A. Transcript SourceArtifact Granularity

Option A: whole-file transcript SourceArtifact for each observed file version.

Cost and risk:

```text
watch append changes file version on every completed line
large transcripts duplicate large raw/redacted payloads per version
source-line anchors still need byte offset and line hash indexes
repair after partial line import must reason about file snapshots
```

Option B: source-record SourceArtifact for each complete JSONL/source record,
with file-state PStates preserving file-level operational progress.

Cost:

```text
one SourceArtifact row per source record
one point seek resolves exact source record provenance
file-level reconstruction uses audit/file-offset views, not a huge raw blob
```

Chosen: Option B for transcript in this slice.

Why:

```text
It preserves exact source-line provenance, avoids storing huge changing file
snapshots, handles redacted/hash-only policy cleanly, and makes watch/harvest
idempotency identical at source-record granularity.
```

Markdown remains whole-file SourceArtifact because markdown import is naturally
file/version oriented and exact raw text is lower privacy risk by default.

#### B. Transcript Projection Ownership

Option A: keep transcript projections in `transcript_ingest.clj`, and have that
module append common material to `object_container.clj`.

Cost and risk:

```text
cross-module append ack does not prove remote stream materialization
projection writes can succeed while common writes fail, or vice versa
source-line ledger in transcript module can hide common repair
tests may still pass by reading transcript-local rows
```

Option B: write first-slice transcript projections in the common
object-container module from the same W1 import event that writes common base
rows.

Cost:

```text
object_container.clj owns more PStates in this slice
later extraction may move projections behind mirrored common events
```

Chosen: Option B.

Why:

```text
It makes F6 enforceable: completion is written after both common base and
required projection rows. Source-specific projections remain non-canonical
because common base rows are still separate and common reads prove identity.
```

Later, projections can move to a source-specific module by consuming common
events, but only after a repair/rebuild path exists.

#### C. SourceAnchor Shape

Option A: one `SourceAnchorSchema` row per target id.

Cost:

```text
1 seek for simple provenance
cannot represent multiple anchors per object without overwriting
tool/artifact objects may later need multiple provenance points
```

Option B: `target/id -> subindexed map anchor/id -> SourceAnchorSchema`.

Cost:

```text
1 range seek + A iterator reads
```

Chosen: Option B. Plural anchors are part of the common contract, and even a
single anchor still costs only one range seek.

#### D. Composition Parent Shape

Option A: one parent edge per child.

Cost:

```text
simple for markdown tree
wrong for transclusion/multiple contexts and future source overlays
```

Option B: `child/id -> subindexed map edge/id -> CompositionEdgeSchema`.

Cost:

```text
1 range seek + P iterator reads for P parents
```

Chosen: Option B. Common composition must allow a container to appear in more
than one context.

#### E. Current Revision Access

Option A: every `read-container` joins to `RevisionSchema`.

Cost:

```text
2 seeks for every container card read
```

Option B: duplicate current revision id/content hash/summary on
`ObjectContainerSchema`; keep full `RevisionSchema` in revision PStates.

Cost:

```text
1 seek for common container card
2 seeks only when full current RevisionSchema row is needed
```

Chosen: Option B. It spends bounded write-path duplication once per import/edit
to avoid an extra seek on every high-volume container read.

#### F. Read Common Material For Source

Option A: scan all containers/units/edges and filter by source id.

Cost:

```text
unbounded all-partition scan; unacceptable
```

Option B: maintain source enumeration indexes by category.

Cost:

```text
one additional termval index write per material row
one range seek per requested category
```

Chosen: Option B. The write fanout is bounded by imported material size, and
source audit/provenance reads become predictable.

## Depots

```text
*object-container-requests-depot:
  declaration: (declare-depot setup *object-container-requests-depot (hash-by :partition/key))
  owner: object-container module
  event types:
    :object-container/import-material
    :object/edit
  ack:
    :ack for edit and tests requiring read-after-write
    :append-ack + polling allowed for bulk imports

*transcript-control-depot:
  declaration: (declare-depot setup *transcript-control-depot (hash-by :transcript/request-id))
  owner: object-container module for this slice
  event types:
    :transcript/harvest
    :transcript/watch
    :transcript/run-status
  ack:
    :ack when caller wants immediate run status

*transcript-file-state-depot:
  declaration: (declare-depot setup *transcript-file-state-depot (hash-by :source/file-key))
  owner: object-container module for this slice
  event types:
    :transcript/file-state
  ack:
    :append-ack normally; :ack in tests
```

Depot boundary rationale:

```text
common import/edit share a depot because they affect the same common PStates
and need per-source/per-object ordering.

transcript control requests are operational and keyed by request id, not
conversation identity.

file state is operational and keyed by file identity for watch resume.
```

No internal depots are required in this slice. Projection rows are written by
the same common import event so completion can prove they exist.

## Topologies and PStates

### `object-container-topology`

Type: stream.

Reason:

```text
object/edit needs interactive read-after-write behavior.
watch/import tests and dogfood UI benefit from :ack-visible decisions.
All writes are deterministic full-row upserts, so stream retry can be safe.
```

Source options:

```text
(source> *object-container-requests-depot {:retry-mode :all-after} :> *request)
(source> *transcript-control-depot {:retry-mode :all-after} :> *request)
(source> *transcript-file-state-depot {:retry-mode :all-after} :> *file-state)
```

Why `:all-after`:

```text
same markdown ref versions must preserve local latest/completion order
same transcript conversation records must preserve follows/last-message order
same edit lineage must preserve stale edit checks
file-state retries should replay later updates on that file partition
```

Stream idempotency:

```text
all created ids are deterministic or request-supplied
all materialization writes are keypath + termval complete-row overwrites
no AFTER-ELEM appends
no counters
no random ids
no source-line completion before base/projection writes
```

Non-idempotent stream writes:

```text
none
```

PStates owned by this topology:

```text
$$requests-by-audit-id
$$decisions-by-audit-id
$$decisions-by-idempotency
$$events-by-id
$$import-completions-by-key
$$source-artifacts-by-id
$$source-versions-by-ref
$$source-latest-by-ref
$$containers-by-id
$$revisions-by-id
$$revision-history-by-container
$$derived-units-by-id
$$unit-graduations-by-id
$$source-anchors-by-target
$$composition-children-by-parent
$$composition-parents-by-child
$$pending-composition-edges-by-missing-target
$$source-containers-by-source
$$source-derived-units-by-source
$$source-anchors-by-source
$$source-edges-by-source
$$native-identity-claims-by-container
$$edit-order-by-target
$$markdown-outline-by-document
$$transcript-runs
$$transcript-file-offsets
$$transcript-source-lines-by-file
$$transcript-conversation-projection
$$transcript-tool-calls-by-name
$$transcript-audit-by-request
$$transcript-last-message-by-conversation
```

Cooperative multitasking:

```text
large import material loops must call yield-if-overtime
large local-select range reads in query topologies must use {:allow-yield? true}
```

### W1 Materialization Branch

```text
1. compute audit/id, material/fingerprint, and request row
2. write $$requests-by-audit-id[audit/id]
3. validate common envelope and payload
4. check $$decisions-by-idempotency[partition/key][idempotency/key]
5. if prior decision exists:
     compare prior material/fingerprint to current material/fingerprint
     if different:
       hop to partition-by-audit-id(audit/id)
       write rejected conflict decision to $$decisions-by-audit-id[audit/id]
       do not overwrite $$decisions-by-idempotency
       ack-return rejected conflict
     if same and prior decision is rejected:
       hop to partition-by-audit-id(audit/id)
       write replay decision for this audit id only
       ack-return prior/replay decision
     if same and prior decision is accepted:
       verify $$import-completions-by-key[prior import/completion-key] exists
       if completion is missing, ignore the shortcut and continue repair
       write replay decision for this audit id only when matching completion exists
       ack-return prior/replay decision
6. check $$import-completions-by-key[import/key]
7. if completion exists:
     compare completion material/fingerprint to current material/fingerprint
     if different:
       hop to partition-by-audit-id(audit/id)
       write rejected conflict decision to $$decisions-by-audit-id[audit/id]
       do not write common base rows or idempotency row
       ack-return rejected conflict
     ensure accepted event row referenced by completion exists
     hop to partition-by-audit-id(audit/id) and write accepted duplicate
       decision using completion event/material refs
     repair transcript source-line completion status from completion when applicable
     hop to partition/key and write missing accepted idempotency row if absent
     ack-return duplicate decision
8. partition to object/key when not already there
9. derive incoming NativeIdentityClaim candidates for every source-native
   ObjectContainer in the payload, keyed by claim/key / container/id
10. group incoming candidates by claim/key. For duplicate candidates with the
    same key:
      if source/native-id, source-line/provenance hash, source-anchor/id,
      anchor/hash, content/hash, material/fingerprint, and import/key all match,
      keep one canonical candidate in stable input order;
      if any field differs, hop to partition-by-audit-id(audit/id), write a
      rejected or repair-pending same-payload native-id conflict decision, do not
      write common base rows, do not write import completion, do not write
      transcript source-line :import-complete, do not write accepted idempotency,
      and ack-return
11. check $$native-identity-claims-by-container[container/id] for each canonical
    incoming claim
12. if a native claim exists with the same material/fingerprint, content/hash,
    source/native-id, and source-line/provenance hash, continue as an idempotent
    duplicate. If the claim differs, hop to partition-by-audit-id(audit/id),
    write a rejected or repair-pending native-id conflict decision, and
    ack-return before overwriting common base rows
13. if the native claim is missing but $$containers-by-id[container/id] already
    exists, read the existing current RevisionSchema and SourceAnchorSchema rows
    needed to synthesize the prior claim. If they match the incoming claim,
    repair $$native-identity-claims-by-container before completion. If they
    differ, reject or mark repair-pending before overwriting the existing rows
14. build source/provenance closure:
      collect SourceArtifact ids in the payload
      collect source-created ObjectContainer ids in the payload
      collect source-created DerivedUnit ids in the payload
      group SourceAnchor rows by target/id
      for anchor source ids not present in the payload, point-read
        $$source-artifacts-by-id[source/id]
      for anchor target ids not present in the payload, point-read
        $$containers-by-id[target/id] or $$derived-units-by-id[target/id] by
        target kind
      for each source-created ObjectContainer or DerivedUnit without a payload
        SourceAnchor, point-read $$source-anchors-by-target[target/id] to find an
        accepted durable anchor
15. if any source-created ObjectContainer or DerivedUnit lacks an accepted
    SourceAnchor, any SourceAnchor source/id lacks a payload or durable
    SourceArtifact, or any SourceAnchor target/id lacks payload or durable common
    material, hop to partition-by-audit-id(audit/id), write a rejected or
    repair-pending source-provenance-incomplete decision, and ack-return before
    overwriting common base rows, writing import completion, writing transcript
    source-line :import-complete, or writing accepted idempotency
16. ensure common base rows, including container current revisions and revision
    history entries, plus source enumeration indexes
17. write $$native-identity-claims-by-container rows for canonical source-native
    ObjectContainers whose claim was absent or matched
18. repair pending composition edges whose missing target appears in this import
19. materialize projection hints
20. update transcript last-message state after follows edge handling
21. for every SourceArtifact with exact ref/version lookup, hop to source-ref-key
    and write $$source-versions-by-ref[source-ref-key][source/version-key]
22. for source families with a latest policy, hop to source-ref-key and write
    $$source-latest-by-ref. In this slice that is markdown whole-file import
23. hop to object/key and write accepted event row to $$events-by-id
24. hop to partition-by-audit-id(audit/id) and write accepted decision row to
    $$decisions-by-audit-id
25. hop to import completion partition and write ImportCompletionSchema after all
    base, projection, source-index, event, and decision rows it certifies
26. for transcript source records, hop to source/file-key and write
    TranscriptSourceLineStatusSchema with status :import-complete and
    import/completion-key. File offset advancement must still verify completion
27. hop to partition/key and write accepted idempotency row after completion
    exists
28. ack-return decision
```

Retry repair:

```text
If failure happens after incoming native-claim canonicalization or durable
native-claim checks but before completion, retry sees no completion row and
repeats both checks before ensuring rows. Same-payload duplicate conflicts reject
or mark repair-pending again before any common base overwrite. Matched
NativeIdentityClaimSchema rows are rewritten with the same values. Conflicting
durable claim rows reject or mark repair-pending before any common base
overwrite.

If failure happens after source/provenance closure checks but before completion,
retry sees no completion row and repeats the closure check. Missing required
anchors, missing SourceArtifacts, and missing anchor targets reject or mark
repair-pending again before any common base overwrite.

If failure happens after step 16, retry sees no completion row and repeats all
ensure writes. Already-written rows are overwritten with the same values.

If failure happens after last-message update but before completion, retry can
see last-message equal to the current source record. It must detect
last-source-record-key == current-source-record-key and avoid a self follows
edge while still repairing missing rows and writing completion.

If failure happens after accepted event/decision but before completion, retry
sees no completion row and repairs materialization rather than taking an
idempotency shortcut. Accepted idempotency is deliberately written after
completion.

If failure happens after completion but before transcript source-line status or
idempotency, retry sees completion, verifies the event row referenced by
completion, repairs source-line status when applicable, and writes the missing
accepted idempotency/duplicate decision rows from completion.

If a retry or second client uses the same idempotency key with a different
material/fingerprint, Rama writes a rejected conflict decision for that audit id
before any materialization branch. The prior accepted idempotency row remains the
replay pointer for the original logical input.
```

### W7 Edit Branch

Preserve the current object-container edit branch with the plural anchor/parent
shape updates:

```text
1. compute material/fingerprint and validate envelope, actor, content hash,
   target kind
2. write request audit row
3. check idempotency by partition/key + idempotency/key
   if prior decision exists:
     compare prior material/fingerprint to current material/fingerprint
     if different:
       hop to partition-by-audit-id(audit/id)
       write rejected conflict decision to $$decisions-by-audit-id[audit/id]
       do not overwrite $$decisions-by-idempotency
       ack-return rejected conflict
     if same:
       hop to partition-by-audit-id(audit/id)
       write replay decision for this audit id only
       ack-return prior/replay decision
4. check EditOrderSchema by lineage/client:
     if existing row has same material/fingerprint:
       ensure event on object/key, decision on audit/id, and idempotency on
       partition/key from the edit-order refs
       ack-return repaired duplicate decision
     if existing row has different material/fingerprint or newer edit/seq:
       hop to partition-by-audit-id(audit/id)
       write rejected stale/conflict decision for this audit id
       ack-return rejected stale/conflict
5. resolve target:
     derived-unit -> read DerivedUnitSchema and UnitGraduationSchema
     object-container -> read ObjectContainerSchema
6. check stale parent/current revision for existing containers
7. if ungraduated derived unit:
     write ObjectContainerSchema(:text-block)
     write first RevisionSchema to $$revisions-by-id and revision history
     write UnitGraduationSchema
     copy/write SourceAnchorSchema under plural anchor map
     update MarkdownOutlineNodeSchema
     update composition edge child target from derived unit to container
8. if existing container:
     write new RevisionSchema to $$revisions-by-id and revision history
     advance ObjectContainerSchema current revision summary
     update UnitGraduationSchema/projection row if source unit exists
9. write EditOrderSchema with material/fingerprint, revision/id, and event/id
   after material rows are ensured
10. write accepted event row to $$events-by-id on object/key
11. write accepted/rejected decision to $$decisions-by-audit-id on audit/id
12. write $$decisions-by-idempotency[partition/key][idempotency/key] after the
    decision/event rows exist
13. ack-return decision
```

W7 retry repair:

```text
If failure happens after material rows but before EditOrderSchema, topology retry
of the same request uses deterministic request-supplied ids or request/id-derived
ids and overwrites the same rows. It then writes EditOrderSchema.

If failure happens after EditOrderSchema but before event/decision/idempotency,
retry sees the edit-order row with the same material/fingerprint and repairs the
missing event, decision, and idempotency rows from its refs instead of appending
a new revision.

If a later request uses the same idempotency key with a different
material/fingerprint, W7 rejects it as an idempotency conflict before resolving
or mutating the target.
```

### Transcript Control Branch

```text
:transcript/harvest or :transcript/watch:
  validate request
  write TranscriptIngestRunSchema accepted-running or rejected
  ack-return run row

:transcript/run-status:
  write TranscriptIngestRunSchema running/complete/failed/repair-needed
  status complete must not imply every source record succeeded unless audit
  and import completion rows say so
```

### Transcript File-State Branch

```text
1. validate file-key/request/source policy fields
2. read existing TranscriptFileOffsetSchema for source/file-key when present and
   compare source/file-generation-key, source/file-id, source/file-stat-
   fingerprint, policy/version, current file length, and last-byte-offset
3. if current length is less than saved last-byte-offset, file id/fingerprint
   changed, policy/version changed, or saved offset points into a partial record,
   write TranscriptFileOffsetSchema with :resume/status
   :stale-after-file-change or :unsafe-resume-rejected, repair-needed true,
   previous/file-generation-key set, and safe last-byte-offset reset to 0 for the
   new generation unless policy rejects unsafe rescan. Do not skip complete lines
   from the new generation
4. for each newly complete source line in the active generation, write or repair
   TranscriptSourceLineStatusSchema with status :observed, import/key,
   material/fingerprint, source/file-generation-key, source-line/order-key, byte
   offsets, and line/hash before W1 append
5. before advancing safe resume state, scan contiguous source-line statuses from
   the current safe byte offset. A line can be crossed only when:
     - status is :import-complete and $$import-completions-by-key[import/key]
       exists with the same material/fingerprint, or
     - status is :parse-error-complete and the audit row says no base
       ObjectContainer materialization was expected
6. if a line is observed but incomplete, missing completion, or mismatched,
   write TranscriptFileOffsetSchema with repair-needed true and do not advance
   :last-byte-offset past that line
7. write TranscriptFileOffsetSchema. :last-byte-offset is the safe resume offset;
   :observed-byte-offset may be ahead, but restart must repair the ledger before
   skipping observed source lines
8. write empty-file audit row when applicable
9. if an empty imported source artifact is required, caller should also append
   W1 with a source artifact and zero containers; file-state alone is not base
   source truth
```

## Query Topologies

### `read-source-by-ref-version`

Input:

```text
source/ref
source/version-key
```

The `source/version-key` is the exact source hash/version key stored in
`$$source-versions-by-ref`, not "latest".

Examples:

```text
missing version:
  read $$source-versions-by-ref[source-ref-key][source/version-key] -> nil
  total reads = 1, meaningful reads = 1

version exists:
  read $$source-versions-by-ref[source-ref-key][source/version-key] -> SourceVersionSchema
  route to partition-by-object-key(SourceVersionSchema.object/key)
  read $$source-artifacts-by-id[source/id] -> SourceArtifactSchema
  total reads = 2, meaningful reads = 2
```

Fixed or variable: variable.

Dynamic approach:

```text
Use <<if after the SourceVersionSchema read. Only issue the
$$source-artifacts-by-id read when the requested version exists. When it exists,
route from the source-ref task to
partition-by-object-key(SourceVersionSchema.object/key) before reading
$$source-artifacts-by-id[source/id]. This avoids falling back to latest-by-ref
and proves exact-version lookup for transcript source records.
```

### `read-latest-source-by-ref`

Input:

```text
source/ref
```

Examples:

```text
no versions:
  read $$source-latest-by-ref[source-ref-key] -> nil
  total reads = 1, meaningful reads = 1

versions exist:
  read $$source-latest-by-ref[source-ref-key] -> SourceVersionSchema
  route to partition-by-object-key(SourceVersionSchema.object/key)
  read $$source-artifacts-by-id[source/id] -> SourceArtifactSchema
  total reads = 2, meaningful reads = 2
```

Fixed or variable: variable.

Dynamic approach:

```text
Use <<if after latest-row read. Only read source artifact when latest row
exists. When it exists, route from the source-ref task to
partition-by-object-key(SourceVersionSchema.object/key) before reading
$$source-artifacts-by-id[source/id].
```

### `read-current-revision`

Input:

```text
container/id
```

Examples:

```text
missing container:
  read $$containers-by-id[container/id] -> nil
  total reads = 1, meaningful reads = 1

invalid accepted container missing current-revision/id:
  read $$containers-by-id[container/id]
  return repair-needed invalid-state result, not a normal empty revision
  total reads = 1, meaningful reads = 1

container with current revision:
  read $$containers-by-id[container/id]
  read $$revisions-by-id[revision/id]
  total reads = 2, meaningful reads = 2
```

Fixed or variable: variable.

Dynamic approach:

```text
Use <<if branches for missing container and invalid nil current revision. Only
issue the revision read for accepted containers whose current-revision/id exists.
The nil-current branch is a repair signal for corrupt or legacy state, not an
accepted output shape.
```

### `read-derived-unit`

Input:

```text
unit/id
```

Examples:

```text
missing unit:
  read $$derived-units-by-id[unit/id] -> nil
  total reads = 1, meaningful reads = 1

derived unit:
  read $$derived-units-by-id[unit/id]
  read $$unit-graduations-by-id[unit/id] -> nil
  total reads = 2, meaningful reads = 2

graduated unit:
  read $$derived-units-by-id[unit/id]
  read $$unit-graduations-by-id[unit/id]
  total reads = 2, meaningful reads = 2
```

Fixed or variable: variable.

Dynamic approach:

```text
Only read graduation row after unit row exists.
```

### `read-common-material-for-source`

Inputs:

```text
source/id
categories vector
cursor map
limit
```

Examples:

```text
categories [:containers]:
  read $$source-containers-by-source[source/id] range
  total reads = 1 range, meaningful reads = 1

categories [:containers :derived-units :anchors :edges]:
  read four category ranges
  total reads = 4 ranges, meaningful reads = 4

categories []:
  no PState reads
  total reads = 0, meaningful reads = 0
```

Fixed or variable: variable.

Dynamic approach:

```text
Use ops/explode over requested categories and aggregate category pages into one
bundle. Do not issue reads for categories not requested.
```

### `read-transcript-object-bundle`

Optional helper for UI/test ergonomics. Input is a transcript container id.

Reads:

```text
container
current revision when present
source anchors
child composition edges, when requested
```

Examples:

```text
message container, no children requested:
  container read
  revision read
  anchor range
  total reads = 3 meaningful reads

tool-call container, children requested:
  container read
  revision read
  anchor range
  child edge range
  total reads = 4 meaningful reads

missing container:
  container read only
  total reads = 1 meaningful read
```

Fixed or variable: variable.

Dynamic approach:

```text
Branch after container read; branch on include-children?; aggregate anchors and
children only when requested.
```

Direct reads R2/R3/R5/R6 remain the acceptance proof. This bundle is not the
only common read path.

## Design Decisions

### Subindexing

Subindexed inner maps:

```text
$$decisions-by-idempotency
$$source-versions-by-ref
$$revision-history-by-container
$$source-anchors-by-target
$$composition-children-by-parent
$$composition-parents-by-child
$$pending-composition-edges-by-missing-target
$$source-containers-by-source
$$source-derived-units-by-source
$$source-anchors-by-source
$$source-edges-by-source
$$edit-order-by-target
$$markdown-outline-by-document
$$transcript-source-lines-by-file
$$transcript-conversation-projection
$$transcript-tool-calls-by-name
$$transcript-audit-by-request
```

Reason:

```text
Each can grow beyond 100 entries for a large source, long conversation, tool
heavy session, long revision history, or repeated imports. Reads are dominated
by sorted range access, so subindexed maps turn pages into one seek plus cheap
iterator reads.
```

Not subindexed:

```text
top-level entity lookup PStates
$$native-identity-claims-by-container
$$transcript-runs
$$transcript-file-offsets
$$transcript-last-message-by-conversation
```

Reason:

```text
They are point lookups or single latest rows by key.
```

### Colocation

Colocated on `object/key`:

```text
container -> revision -> anchor -> composition edge -> projection row
conversation -> message -> tool call -> tool result -> artifact/run ->
  follows/produced/derived edges -> native identity claim
document -> derived unit -> graduation -> outline row
```

Colocated on `source-ref-key`:

```text
markdown source-ref latest/version indexes
transcript exact source-record version indexes by source-ref-key
```

Colocated on request/file keys:

```text
transcript run status by request/id
transcript file offset by source/file-key
transcript source-line status by source/file-key and file-generation-prefixed
  source-line/order-key
```

Cross-partition hops are intentional only for:

```text
markdown import: source-ref partition <-> object-key partition
transcript import: conversation partition -> source-ref/file/audit/tool-name partitions
object edit: normally object-key only
```

### Policy Placement

Edge/application guards:

```text
session authorization
payload size
filesystem access
path allowlist
```

Inside Rama common import decision:

```text
source family/format allowed
redaction/capture mode allowed
required source/object/anchor/edge fields present
idempotency conflicts by material/fingerprint comparison
source-native identity conflicts inside the same W1 payload by incoming
  NativeIdentityClaim candidate canonicalization before PState reads or base-row
  overwrite
source-native identity conflicts against accepted durable state by
  NativeIdentityClaimSchema comparison before base-row overwrite
source/provenance closure for source-created ObjectContainers, DerivedUnits, and
  SourceAnchors before base-row overwrite
target existence for non-repairable edges
tool_result content not silently empty when source has result content
artifact/run content or metadata not silently dropped when source exposes it
```

Projection filtering for "who can see what" is later. This slice stores
visibility/policy fields but does not implement multi-user sync.

### Pending Edges

Transcript tool results can arrive before matching tool calls in malformed or
nonstandard sources.

Accepted behavior:

```text
create real :tool-result ObjectContainer and Revision
create SourceAnchor to the result source record
if tool-call parent missing:
  write PendingCompositionEdgeSchema keyed by expected tool-call id
  audit missing parent
when tool-call arrives:
  repair pending edge into common CompositionEdge rows
```

Do not reject useful tool-result content solely because the source order is
surprising. Do not store it only in projection.

Transcript artifact/run objects follow the same rule: create a real
`:chat-artifact`, `:agent-run`, or `:execution-run` ObjectContainer and Revision
when the source exposes that identity. Edges such as message-produced-artifact
and artifact-derived-from-source are common CompositionEdge rows when
source-declared or source-physical.

### No-Read Write Rule

When the topology has computed a full row, use:

```clojure
(local-transform> [(keypath *id) (termval *row)] $$pstate)
```

Use read-before-write only for:

```text
idempotency checks
import completion checks
source latest/version decisions
native identity claim conflict checks
source/provenance closure checks
edit target existence
edit current revision parent
edit stale-order check
transcript last-message/follows edge handling
pending edge repair
transcript source-line status repair
transcript file-offset advancement checks
stale file-generation/resume checks
```

### Compatibility With Existing Code

Phase 3 should not preserve old transcript-local base reads as proof.

Allowed compatibility:

```text
old transcript parser/acquisition helpers stay or move
old transcript projection helper names can wrap new common-module reads
old tests can be ported to assert common containers/anchors/edges/revisions
```

Not allowed:

```text
tests that pass only by reading TranscriptContainerRow from transcript-local
$$containers-by-id
```

## State Primitive Selection

All state in this plan is durable PState state. No TaskGlobal is required in
the first implementation slice.

PStates:

```text
$$requests-by-audit-id:
  PState. One row per request/audit. Durable request trail. Write volume O(1)
  per request.

$$decisions-by-audit-id:
  PState. Durable accepted/rejected/replayed decision. Write volume O(1) per
  request.

$$decisions-by-idempotency:
  PState. Durable idempotency index. Write volume O(1) per request.

$$events-by-id:
  PState. Durable accepted world/import events. Write volume O(1) per accepted
  request.

$$import-completions-by-key:
  PState. Durable repair/completion marker. Write volume O(1) per completed
  import material request.

$$source-artifacts-by-id:
  PState. Canonical source artifact rows. Write volume O(1) per markdown file
  or transcript source record. W1 source/provenance closure performs point reads
  against this PState for referenced source ids that are not present in the
  current payload before accepting anchors or source-created targets.

$$source-versions-by-ref and $$source-latest-by-ref:
  PStates. Source ref version indexes. Write volume O(1) per source artifact
  version that participates in ref lookup.

$$containers-by-id:
  PState. Canonical ObjectContainer identity. Write volume O(containers in
  import material) per import; O(1) per edit.

$$revisions-by-id and $$revision-history-by-container:
  PStates. Canonical revision content and range history. Write volume
  O(revisions in import material) per import; O(1) per edit.

$$derived-units-by-id and $$unit-graduations-by-id:
  PStates. Markdown/source-derived unit lifecycle. Write volume O(derived units
  in import) and O(1) per graduation/edit.

$$source-anchors-by-target:
  PState. Common provenance index. Write volume O(anchors in import material).
  W1 source/provenance closure range-reads a target's existing anchors only when
  a source-created ObjectContainer or DerivedUnit in the current payload lacks a
  payload SourceAnchor and must prove an already accepted durable anchor.

$$composition-children-by-parent and $$composition-parents-by-child:
  PStates. Common structure indexes. Write volume O(edges in import material).

$$pending-composition-edges-by-missing-target:
  PState. Durable repair queue for source-declared edges whose target is not
  present yet. Write volume O(pending edges in import material), bounded by the
  source record/tool block count.

$$source-containers-by-source, $$source-derived-units-by-source,
$$source-anchors-by-source, $$source-edges-by-source:
  PStates. Durable source enumeration indexes for common material. Write volume
  O(material rows in import), bounded by source-controlled input size.

$$native-identity-claims-by-container:
  PState. Durable source-native identity conflict guard. One row per accepted
  source-native ObjectContainer. Write volume O(source-native containers in
  import material). W1 first builds an in-memory request-local incoming-claim map
  of the same cardinality to reject or canonicalize duplicate claim keys inside
  the payload. Read volume against the PState is O(canonical source-native
  containers in the incoming source record) before W1 base-row overwrites.

W1 source/provenance closure:
  Pure request-local validation state, not a PState or TaskGlobal. It is derived
  from the payload plus explicit point/range reads of existing durable common
  PStates. Missing required provenance in this slice is rejected or marked
  repair-pending before common base rows, import completion, transcript
  source-line :import-complete, or accepted idempotency.

$$edit-order-by-target:
  PState. Durable stale edit guard. Write volume O(1) per accepted edit.

$$markdown-outline-by-document:
  PState. Source-specific projection, non-canonical. Write volume O(markdown
  blocks) per markdown import and O(1) per edit/graduation.

$$transcript-runs:
  PState. Operational projection. Write volume O(run status updates).

$$transcript-file-offsets:
  PState. Operational watch resume state with generation/status metadata. Write
  volume O(files observed per watch/harvest cycle). Stale generation/truncation
  rows overwrite the latest operational position with visible repair/reject
  status rather than advancing from an unsafe offset.

$$transcript-source-lines-by-file:
  PState. Durable operational source-line repair ledger. Write volume O(source
  records observed); range reads are by source/file-key and generation-prefixed
  source-line/order-key from safe resume offset to observed offset during watch
  repair.

$$transcript-conversation-projection:
  PState. Source-specific projection, non-canonical. Write volume O(transcript
  projection entries in import material).

$$transcript-tool-calls-by-name:
  PState. Source-specific index over common tool containers. Write volume
  O(tool calls/results in source record).

$$transcript-audit-by-request:
  PState. Source-specific audit/read model. Write volume O(source records per
  request).

$$transcript-last-message-by-conversation:
  PState. Private durable helper for follows-edge materialization. Write volume
  O(message records).
```

TaskGlobals:

```text
none in this slice
```

External systems:

```text
filesystem transcript/markdown sources are external inputs read by acquisition
helpers before depot append. They are not authoritative after import; common
SourceArtifact/SourceAnchor rows preserve the imported source identity
according to policy.
```

## Phase 2 Validation Gates

Phase 2 should fail this plan if any of these are not satisfied:

```text
transcript conversation/message/tool-call/tool-result identity is common
transcript chat-artifact and agent-run/execution-run identity is common when
  the source exposes artifact/output/attachment or run identity
every accepted ObjectContainer has current-revision/id and a matching
  RevisionSchema row
tool_result content is in common RevisionSchema, not only projection
artifact/run content or structured metadata is in common RevisionSchema, not
  only projection
SourceArtifact/SourceAnchor/CompositionEdge for transcript are common rows
transcript SourceArtifact rows populate $$source-versions-by-ref for exact
  source/ref + source/hash/version reads
every accepted source-created ObjectContainer or DerivedUnit has a SourceAnchor
  in the payload or accepted durable common anchor state
every SourceAnchor source/id resolves to a SourceArtifact in the payload or
  accepted durable common source state
every SourceAnchor target/id resolves to an ObjectContainer or DerivedUnit in
  the payload or accepted durable common material state
missing source/provenance closure is rejected or marked repair-pending before
  base-row overwrite, completion, source-line import-complete, or accepted
  idempotency
parse errors do not create fake ObjectContainers
source-line/import completion is written after common base and projection rows
transcript file offsets advance only through source-line statuses backed by
  import completion or explicit parse-error audit state
transcript stale file generation, truncation, rotation, path reuse, and saved
  offset ahead of current length produce visible repair/reject state and do not
  skip records
same idempotency key with different material/fingerprint is rejected before
  materialization
same W1 payload with duplicate source-native container ids is canonicalized only
  when duplicate NativeIdentityClaim candidates are identical; conflicting
  duplicate candidates are rejected or marked repair-pending before PState claim
  reads, base-row overwrite, completion, source-line import-complete, or accepted
  idempotency
same source-native container id with different content/provenance/material is
  rejected or marked repair-pending before base-row overwrite, even with a
  different idempotency key
common reads prove markdown and transcript share one substrate
source-specific projections are explicitly non-canonical
PState schemas avoid broad Object storage
large per-source collections are subindexed
query topologies list variable meaningful reads, dynamic handling, and required
  cross-partition routing
```
