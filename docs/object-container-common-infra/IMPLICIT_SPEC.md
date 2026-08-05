# Implicit Spec - Object-Container Common Infra

<!-- Phase 0. Requirements analysis only. Do not design depots, PStates, topologies, or file/module layout here. -->

Status: Rama Phase 0 artifact, 2026-06-07.

This artifact starts the common-infra track described in
`docs/current-mental-model/build/object-container-common-infra/PRODUCT.md`.

The product behavior to preserve is:

```text
Markdown Ingester      \
                        -> Object-Container Kernel
Transcript Ingester    /
```

Markdown-created material and transcript-created material must become readable
as first-class material in the same common Object-Container substrate. Source
adapters may be source-specific. Canonical base identity may not be
source-specific.

## Inputs Read

```text
docs/current-mental-model/build/object-container-common-infra/PRODUCT.md
docs/current-mental-model/architecture/object-container-ingester-contract.md
docs/current-mental-model/architecture/object-container-spec.md
docs/current-mental-model/build/object-container/IMPLICIT_SPEC.md
docs/current-mental-model/build/chat-ingester/PRODUCT.md
docs/current-mental-model/build/chat-ingester/COMMON_KERNEL_REPLAN.md
src/app/server/rama/object_container.clj
src/app/server/rama/dogfood/transcript.clj
src/app/server/rama/dogfood/transcript_ingest.clj
```

Grounding from current code:

```text
object_container.clj currently owns common SourceArtifact/ObjectContainer/
Revision/DerivedUnit/SourceAnchor/CompositionEdge rows for markdown.

transcript_ingest.clj currently owns transcript-local TranscriptContainerRow,
SourceArtifactRow, SourceAnchorRow, and CompositionEdgeRow. Those are useful as
an ingester proof, but they are not the final shared base identity store.
```

## Phase 0 Product Answers

### 1. What Common Import Accepts

Common import accepts source-interpreted material from a trusted ingester after
edge guards have accepted the request envelope. The logical import shape is
`ObjectContainerImport`.

Required import-level fields:

```text
request id
actor
policy / visibility
source family
source format
source version
idempotency key
import mode, when relevant (:one-time-import, :passive-watch, :manual, etc.)
```

Required source artifact fields:

```text
source id or deterministic source identity input
source ref
source hash or source version
source family / format / version
raw, redacted, or hash/anchor-only captured material according to policy
source metadata needed to explain provenance
```

Required native object fields, when the source has identity:

```text
container id or stable native-id input
container kind
owner / actor
visibility / policy
source origin
current revision pointer or initial revision material
summary / content handle / structured content payload
```

Required derived unit fields, when the source does not have stable identity:

```text
unit id or deterministic span identity input
source span
distiller / interpreter id and version
derived content or summary
graduation state, if already known
```

Required revision fields:

```text
revision id or deterministic revision identity input
container id
parent revision, when applicable
content text or structured payload
content hash over the redacted/captured payload
created by / request id
```

Required source anchor fields:

```text
source id
target kind
target id
source-native id, when present
byte / line / span / JSON pointer, when present
anchor hash or line hash
```

Required composition edge fields:

```text
edge id or deterministic edge identity input
edge kind
parent / source target
child / destination target
source-declared or source-physical order key
source anchor or source proof, when needed
```

Optional projection hints:

```text
conversation projection facts
tool-call index facts
markdown outline hints
audit/read-model hints
parse-error display facts
```

Projection hints are not canonical base truth.

### 2. What Common Import Rejects

Common import rejects:

```text
missing request id or idempotency key
missing actor or policy/visibility when required by the source family
missing source family, source format, source ref, or source hash/version
unredacted transcript payload when the selected policy forbids durable raw storage
object containers without kind or stable identity source
derived units without source span and distiller/interpreter id
revisions without a target container or content hash
source anchors that cannot name a source and target
composition edges whose target identities cannot be resolved or repaired
conflicting duplicate ids under the same idempotency key
source-specific base object rows as the only durable identity for imported material
parse errors represented as fake base containers
tool-result placeholders with empty content when actual result content exists
```

Duplicate imports are not rejected when they are true idempotent replays of the
same logical input. They must replay or no-op while preserving the same common
read results.

### 3. Required Object Kinds

Required for markdown:

```text
:document
:text-block, after a DerivedUnit graduates
```

Required for transcript:

```text
:chat-conversation
:chat-message
:tool-call
:tool-result
:chat-artifact, when the source exposes an artifact/attachment/output
:agent-run or :execution-run, when the source distinguishes a run from a conversation
```

Future ingesters can add kinds, but they must still satisfy the same
ObjectContainer contract.

### 4. ObjectContainer vs DerivedUnit

Create an `ObjectContainer` immediately when:

```text
the source gives stable native identity
the import itself should be addressable as a whole document/conversation/source object
the imported item must be placeable, quotable, linkable, or revisable as a native Softland object now
```

Create a `DerivedUnit` when:

```text
the item is an addressable source-derived span
identity was produced by a distiller/interpreter, not by the source
the unit should remain re-runnable and not yet independent durable identity
```

Markdown headings, paragraphs, and list items are usually `DerivedUnit`s until
touched. Transcript messages, tool calls, and tool results usually become
`ObjectContainer`s immediately because transcript sources often carry native
ids or stable line identities. Parse errors do not become fake containers; they
become audit/projection facts with anchors to the source record.

### 5. Raw Source Preservation And Redaction

Markdown source can preserve exact raw text by default.

Transcript source is policy-bearing. The accepted modes are:

```text
local-private raw mode
redacted captured-source mode
hash/anchor-only mode
```

Transcript parsing and parse failures must not persist unredacted sensitive
payloads into shared or policy-forbidden storage. Source anchors still need
enough identity to trace a native object back to the original file/line/offset
or source-native item.

### 6. Required Source Anchors

Every source-created base object or derived unit needs an anchor.

Markdown anchors require:

```text
source id
target id
block path or span identity
start/end offsets when available
source hash
```

Transcript anchors require:

```text
source id
target id
source family
file id
file path or external ref
byte offset
byte length
line hash
source event type
source-native id or JSON pointer when available
```

Tool-call anchors point to the source record that introduced the tool call.
Tool-result anchors point to the source record that introduced the result
content, not to an earlier tool-use placeholder.

### 7. Composition Edges That Are Base Truth

Common composition edges include only source-declared or source-physical
structure:

```text
document contains derived block
block parent contains child block
block follows sibling by source order
conversation contains message
message follows previous message
message replies-to parent message, when source-provided
assistant message produced tool call
tool call produced tool result
message produced artifact
artifact derived from source message/tool result
```

Inferred semantic relations are not accepted at ingest time. They can later
enter as candidates through a separate workflow.

### 8. Source-Specific Read Models

Source-specific read models are allowed when they are projections, indexes, or
operational state:

```text
markdown outline projection
transcript conversation projection
transcript tool-call index
transcript ingest run status
transcript file offset / watch resume state
transcript audit entries
parse-error display rows
source-specific lookup indexes
```

They must be rebuildable, reconcilable, or explicitly non-canonical. They must
not be the only durable store for imported identity-bearing material.

### 9. Common Reads That Prove One Substrate

The common slice is not proven by source-specific projections. It is proven only
when common reads can:

```text
fetch a markdown-created document ObjectContainer
fetch a transcript-created chat conversation ObjectContainer
fetch a transcript-created chat message ObjectContainer
fetch a transcript-created tool-call ObjectContainer
fetch a transcript-created tool-result ObjectContainer with result content
fetch SourceArtifact rows for markdown and transcript sources
fetch SourceAnchor rows for markdown and transcript targets
traverse CompositionEdge rows for markdown document structure
traverse CompositionEdge rows for transcript conversation/tool structure
read DerivedUnits for markdown anonymous spans
read Revision content for markdown and transcript ObjectContainers
```

### 10. Invariants Preventing Source-Specific Base Islands

The commonness invariants are:

```text
all SourceArtifact base identity is common
all ObjectContainer base identity is common
all Revision content for native containers is common
all SourceAnchor provenance for native containers/derived units is common
all CompositionEdge source-declared structure is common
source-specific containers are projections only, never the only identity store
accepted import decisions cannot report success while common base writes are missing
source-ledger/idempotency rows cannot suppress repair of missing common base writes
projection rows can lag only if a repair/rebuild path is explicit
```

## Operations

### W1. `object-container/import-material` - accept common import material

This is the common logical write operation. It accepts `ObjectContainerImport`
material from markdown, transcript, or future ingesters.

- **Latency:** Hundreds of milliseconds is acceptable for one-time imports and
  bulk materialization. Passive watch should usually make new transcript records
  visible within low hundreds of milliseconds, but exact single-digit
  interactivity is not required for import. The operation must not block
  interactive editing of existing material.
- **Throughput:** Driven by imported source size and watch append rate. Markdown
  scales by files x blocks. Transcript scales by files x JSONL/source records x
  tool blocks/results. Folder harvest can be bursty; passive watch is long-lived
  and incremental.
- **Consistency/correctness invariants:**
  - Accepted import decisions mean common base truth exists or is explicitly
    repairable by a durable pending/repair state.
  - Duplicate idempotent imports produce the same common read results.
  - Source-specific projection writes never replace common base writes.
  - Rejected import decisions are durable/auditable; rejected input does not
    create partial fake base objects.
  - Every accepted ObjectContainer has a current revision or an explicit
    content-handle policy.
  - Every accepted source-created container or derived unit has a SourceAnchor.
  - Every accepted composition edge targets existing or explicitly pending
    repairable identities.
- **Data growth and scale:** Unbounded collections are source versions by ref,
  containers by source/world, revisions by container, anchors by target,
  composition edges by parent, decisions/events by request, and projection
  indexes. Dominant reads are point lookup by id and range reads by parent,
  document, conversation, request, or source ref.
- **Concurrency behavior:** Concurrent imports of the same source version must
  converge to one logical materialization. Concurrent import of the same ref
  with different hashes creates distinct source versions. Watch and harvest may
  observe the same transcript line; they must converge to the same container,
  anchor, revision, edge, and audit result.
- **Edge cases:** Empty markdown file, empty transcript file, malformed JSONL
  row, partial trailing watch line, duplicate request id, duplicate native id
  with conflicting content, tool result before tool call, missing source hash,
  unsupported source format, redaction policy mismatch, very large raw source,
  and retry after partial failure.

### W2. `markdown/import-source` - parse markdown and emit common import material

This is the markdown ingester operation. It interprets markdown/plain text and
emits common import material. It may use existing markdown distiller behavior,
but the common material is the contract.

- **Latency:** Hundreds of milliseconds per file is acceptable. Large files can
  take longer if progress/audit behavior remains explainable.
- **Throughput:** Driven by number of imported files and blocks per file.
  Markdown blocks can reach tens of thousands in one file; write volume scales
  with source-controlled input size.
- **Consistency/correctness invariants:**
  - Preserve exact raw markdown source.
  - Create a document-level ObjectContainer when the file is user-imported
    material.
  - Create DerivedUnits for anonymous headings, paragraphs, list items, and
    other block spans unless policy explicitly graduates them.
  - Preserve source order and parent/child structure as common CompositionEdges.
  - Re-ingesting the same `(source ref, source hash)` does not duplicate
    source artifacts, derived units, anchors, edges, or document containers.
  - Re-ingesting a changed source version does not overwrite graduated
    containers from a previous version.
- **Data growth and scale:** Dominant read after import is ordered outline
  range access. Derived units and edge count scale with block count.
- **Concurrency behavior:** Two markdown imports of the same source version
  converge. Import racing with edit/graduation must not clobber the edited
  container.
- **Edge cases:** Empty file, whitespace-only file, source with no headings,
  deeply nested headings/lists, duplicate headings, very long lines, CRLF/LF
  differences, same path with new hash, same content at different path.

### W3. `transcript/harvest` - one-time transcript acquisition

This is the transcript ingester's one-time acquisition mode. It walks selected
source paths, reads complete source records, redacts according to policy, and
emits transcript observations that become common import material.

- **Latency:** Seconds are acceptable for a folder harvest. Per-file progress
  and final run status must be auditable.
- **Throughput:** Driven by number of transcript files and source records. Large
  local histories can contain many JSONL files and many tool events.
- **Consistency/correctness invariants:**
  - Harvest does not spawn Claude/Codex, modify source files, or trigger
    downstream actions.
  - Harvest and watch produce the same native object identities for the same
    source records.
  - Byte offsets, byte lengths, and line hashes are computed from actual bytes.
  - Redaction happens before policy-sensitive durable persistence.
  - Parse errors are audit/projection facts, not fake common containers.
  - A harvest request's completion status cannot hide missing common base
    material for observed valid records.
- **Data growth and scale:** Source file state and audit entries grow with
  files/records. Conversation/message/tool containers grow with valid records.
  Tool-call indexes grow with tool_use blocks.
- **Concurrency behavior:** Re-running harvest over the same files is safe.
  Harvest racing with watch must not duplicate containers, revisions, anchors,
  edges, or audit entries for the same source line.
- **Edge cases:** Empty file, malformed JSON, invalid UTF-8 behavior after byte
  decoding, missing conversation id, missing message id, duplicate message id,
  files renamed between scans, source deleted during harvest, line read after
  partial write, secrets in parse-error previews, and mixed Claude/Codex
  source shapes.

### W4. `transcript/watch` - passive transcript acquisition

This is the transcript ingester's passive append/watch mode. It observes new
complete source records and emits the same common import material as harvest.

- **Latency:** New complete lines should become visible in low hundreds of
  milliseconds to a few seconds, depending on polling/watch cadence. It is not
  an editing hot path.
- **Throughput:** Driven by append rate of active transcript sources. Usually
  human/agent paced, but tool-heavy sessions can burst.
- **Consistency/correctness invariants:**
  - Watch is passive: no source mutation, no executor spawn, no follow-up
    actions.
  - Watch resumes from durable file/offset state when possible.
  - Partial trailing lines are not materialized until complete.
  - The same source line observed by watch and harvest maps to the same common
    identities.
  - File offset state cannot be treated as proof that common base material was
    successfully written unless repair is possible.
- **Data growth and scale:** Long-running watches grow file-offset state,
  audit entries, and transcript projections. Base growth is bounded by observed
  complete records.
- **Concurrency behavior:** Multiple watch loops over the same file must
  converge through source-line identity. Watch restart must not replay partial
  trailing records as complete records.
- **Edge cases:** File truncation, rotation, rename, inode change, partial final
  line, CRLF/LF, repeated line content at different offsets, clock skew in
  timestamps, and saved offset ahead of current file length.

### W5. `transcript/observe-record` - interpret one source record

This operation converts a parsed/redacted transcript observation into common
import material and transcript-specific projection/index hints.

- **Latency:** Low hundreds of milliseconds or less for an individual record in
  watch mode; batch harvest may amortize this over many records.
- **Throughput:** Driven by transcript source record count and number of nested
  tool/artifact blocks inside each record.
- **Consistency/correctness invariants:**
  - Conversation/session identity becomes a common `:chat-conversation`
    ObjectContainer.
  - Message identity becomes a common `:chat-message` ObjectContainer when the
    record represents a message.
  - Tool-use identity becomes a common `:tool-call` ObjectContainer.
  - Tool-result identity becomes a common `:tool-result` ObjectContainer with a
    Revision containing the redacted result content when result content exists.
  - Tool result materialization must not be an empty placeholder if the source
    carried result content.
  - Every valid transcript container gets a common SourceAnchor to the source
    record that introduced it.
  - Conversation/message/tool/result ordering and production structure becomes
    common CompositionEdges when source-declared or source-physical.
  - Transcript conversation/tool/audit projections are secondary read models.
- **Data growth and scale:** One source record may create zero containers
  (parse error), one message container, multiple tool-call/tool-result
  containers, and multiple edges. Tool-heavy rows can multiply write volume.
- **Concurrency behavior:** Duplicate observation of the same source-line key
  must converge. Tool result arriving before the matching tool call must still
  preserve the result as a real object and either defer/repair the produced
  edge or record an auditable missing-parent condition.
- **Edge cases:** Parse error, unknown role/event type, message with no content,
  tool call with missing id, tool result with missing/mismatched call id,
  multi-block assistant message, artifact output, attachment/file output,
  missing timestamp, duplicate UUID with conflicting source line, and redaction
  removing sensitive content while preserving useful summary/hash.

### W6. `transcript/file-state-observation` - record acquisition progress

This operation records file-level watch/harvest progress and empty-file facts.
It is operational state, not common base identity, except where it triggers a
valid SourceArtifact for an empty imported source.

- **Latency:** Low hundreds of milliseconds is acceptable.
- **Throughput:** One or a few writes per scanned file per run/watch cycle.
- **Consistency/correctness invariants:**
  - File offset state supports watch resume; it is not canonical object truth.
  - Empty files can create an auditable source fact without fake message
    containers.
  - File progress cannot advance in a way that permanently hides missing common
    base writes after partial failure.
  - File-state rows must record request/source identity and policy context
    enough for audit.
- **Data growth and scale:** Grows by source family and file identity. Updates
  overwrite latest operational position, while audit/history grows separately.
- **Concurrency behavior:** Concurrent scans of the same file should converge on
  the greatest safe complete offset, not skip unseen complete records.
- **Edge cases:** Empty file, file truncated, file replaced with same path,
  file id unavailable, permission error, path expansion failure, saved offset
  from prior policy/source version, and offset pointing into a partial record.

### W7. `object/edit` - graduate a DerivedUnit or revise an ObjectContainer

This operation already belongs to the common object-container lifecycle and
must remain common after the infra track. Markdown depends on it immediately;
transcript containers may later be edited, quoted, or revised through the same
model.

- **Latency:** Single-digit milliseconds desired for interactive editing.
- **Throughput:** Human-paced, but bursts can come from programmatic edits or
  multi-agent workflows later.
- **Consistency/correctness invariants:**
  - Editing a DerivedUnit graduates it at most once into an ObjectContainer.
  - Editing an existing ObjectContainer appends a Revision and advances current
    content without mutating raw SourceArtifact.
  - SourceAnchor provenance survives graduation and revision.
  - Re-import or re-distillation never overwrites authored revision content.
  - Duplicate edit request ids are idempotent.
  - Concurrent edits to the same not-yet-graduated unit produce one container
    and an ordered revision chain, not duplicate containers.
- **Data growth and scale:** Revisions grow append-only by edit count per
  container. Current revision reads dominate; history reads are less common.
- **Concurrency behavior:** Concurrent edits to the same target require
  ordering by request/edit lineage so an older callback cannot silently clobber
  a newer revision.
- **Edge cases:** Empty content edit, missing target, already graduated unit,
  edit of transcript container, edit after source version changed, stale edit
  callback, and unsupported target kind.

### W8. `source-specific/projection-upsert` - materialize read models

This covers markdown outline rows, transcript conversation rows, tool indexes,
audit views, run status, and parse-error display rows.

- **Latency:** Projection updates should be near the common base update for
  user-facing watch/import feedback, but they are not canonical base truth.
- **Throughput:** Projection write volume can equal or exceed base write volume
  because one base event may feed multiple read models.
- **Consistency/correctness invariants:**
  - Projection rows never become the only durable identity store.
  - Projection rows must carry enough references to common containers, anchors,
    decisions, or source records to explain themselves.
  - If projection writes happen after common base writes, they must be
    rebuildable or reconcilable.
  - A source-line ledger cannot suppress projection repair after partial
    failure.
- **Data growth and scale:** Conversation projections grow by conversation
  length; tool indexes grow by tool calls/results; audit grows by source record
  and request; outline projections grow by markdown block count.
- **Concurrency behavior:** Projection upserts are idempotent by common object
  id, source-line key, order key, or request id. Concurrent repair and live
  writes converge to the same view.
- **Edge cases:** Common base object exists but projection row missing,
  projection row exists for rejected/parse-error source, stale projection after
  revised container, missing anchor, duplicated source line, and projection
  generated before parent edge is repairable.

## Reads

All reads may be called in any entity state. Missing material returns nil,
empty collections, rejected/pending status, or an explicit error record rather
than throwing because a source is only partially imported.

### R1. `read-source-artifact`

- **Latency:** Interactive; point lookup should be fast. Large raw content may
  be paged/handled by policy later, but the read contract is source provenance.
- **Throughput:** Driven by provenance inspection, projections resolving
  anchors, and import auditing.
- **Invariants:** Raw/captured/redacted policy is visible. Markdown exact raw
  remains unchanged. Transcript redaction/hash-only policy is not bypassed.
- **Scale:** Source versions by ref are unbounded. Reads by id and by
  `(source ref, source hash/version)` must be supported by the common substrate.
- **Edge cases:** Missing source, hash-only mode, redacted mode, changed source
  ref, multiple versions for same ref, and deleted external file.

### R2. `read-object-container`

- **Latency:** Interactive point lookup.
- **Throughput:** High relative to writes; every projection/workflow may
  resolve containers.
- **Invariants:** A markdown document, transcript conversation, message,
  tool-call, and tool-result are all readable through the same common read.
- **Scale:** Containers are unbounded by import volume.
- **Edge cases:** Missing id, rejected import, parse-error row with no
  container, source-specific projection id, container with structured revision,
  and container whose source artifact uses hash-only mode.

### R3. `read-current-revision` / `read-revision-history`

- **Latency:** Current revision is interactive. History is range-readable and
  can be slower.
- **Throughput:** Current revision is common; full history is less frequent.
- **Invariants:** Revisions are append-only. Tool-result containers with source
  result content expose that content through common revision reads.
- **Scale:** Revision history is unbounded per edited/updated container.
- **Edge cases:** Container with no text but structured payload, first revision,
  stale parent pointer, duplicate edit replay, redacted transcript payload, and
  old revision after source changed.

### R4. `read-derived-unit`

- **Latency:** Interactive, especially for markdown outline views.
- **Throughput:** Driven by markdown block inspection and graduation.
- **Invariants:** DerivedUnits remain source-derived until touched. If
  graduated, the read exposes the common container identity and current
  revision content rather than stale source-derived text.
- **Scale:** Derived units grow by source spans without native identity.
- **Edge cases:** Missing unit, unit from old source version, already
  graduated unit, re-distilled unit drift, and source artifact unavailable
  except by hash/anchor.

### R5. `read-source-anchors-by-target`

- **Latency:** Interactive provenance lookup.
- **Throughput:** High for quote/provenance UI, projections, and audit trails.
- **Invariants:** Every accepted container/derived unit created from a source
  has an anchor. Tool results anchor to the result source record.
- **Scale:** Anchors grow with containers and derived units.
- **Edge cases:** Missing anchor, multiple anchors for one target, redacted
  source, hash-only source, source-native id without byte span, byte span
  without native id, and repaired anchor after partial import.

### R6. `read-composition-children` / `read-composition-parent`

- **Latency:** Interactive for outline and conversation traversal.
- **Throughput:** High; ordered child range reads dominate.
- **Invariants:** Markdown and transcript structure are traversed through the
  same common edge read. Source-specific conversation/outline projections are
  convenience views over this base.
- **Scale:** Edges grow with source-declared structure. Parents such as a large
  document or conversation can have many children.
- **Edge cases:** Parent has no children, missing parent, child with multiple
  contexts, pending/repaired tool-result edge, duplicate order key, and
  re-imported same structure.

### R7. `read-import-decision` / `read-import-audit`

- **Latency:** Interactive enough for run/audit UI.
- **Throughput:** Driven by imports, retries, watches, and debugging.
- **Invariants:** Accepted/rejected/replayed decisions are durable and explain
  what common base material was accepted, rejected, pending, or repaired.
- **Scale:** Grows with every request and source record audit.
- **Edge cases:** Duplicate request, replayed idempotency key, rejected
  redaction policy, parse error, partial failure, repair after ledger marker,
  and timeout while decision is still pending.

### R8. `read-common-material-for-source`

- **Latency:** Used for verification and provenance UI; range reads can be
  hundreds of milliseconds for large imports.
- **Throughput:** Lower than point reads, but important for import audit and
  rebuild flows.
- **Invariants:** The same read family can enumerate common containers,
  derived units, anchors, and edges for a markdown source or transcript source.
- **Scale:** Source-level enumeration can be large and must page/range.
- **Edge cases:** Source with only DerivedUnits, source with only native
  containers, redacted/hash-only source, mixed valid and parse-error records,
  and multiple versions of one ref.

### R9. `read-markdown-outline-projection`

- **Latency:** Interactive outline rendering.
- **Throughput:** High while viewing markdown imports.
- **Invariants:** Projection reflects common base material. Graduated units show
  authored current revisions. Projection is not canonical.
- **Scale:** Large documents require ordered range/paging behavior.
- **Edge cases:** Empty document, missing projection row with common base edge,
  graduated unit, stale projection after edit, and re-imported source version.

### R10. `read-transcript-conversation-projection`

- **Latency:** Interactive conversation rendering.
- **Throughput:** High for dogfood review/use.
- **Invariants:** Projection rows refer to common chat containers, common
  revisions, common anchors, and common composition edges. Parse-error rows can
  appear without fake base containers.
- **Scale:** Conversations can be long; ordered range access is required.
- **Edge cases:** Missing message content after redaction, tool result before
  tool call, parse-error row, missing projection row with common base object,
  duplicate source line, and multiple files contributing to one conversation.

### R11. `read-transcript-operational-views`

This includes run status, audit entries, tool-call index, and file offset/watch
resume state.

- **Latency:** Interactive for status/audit UI; file offset reads are used by
  watch startup.
- **Throughput:** Driven by watch loops, imports, and audit/debugging.
- **Invariants:** Operational views explain acquisition/projection state; they
  are not common base identity. Tool indexes point to common tool-call and
  tool-result containers.
- **Scale:** Audit grows by source record/request. Tool index grows by tool
  call/result count. File offsets grow by observed file identity.
- **Edge cases:** Rejected run, running run, complete run with missing repair,
  parse-error audit row, offset stale after truncate, source-line ledger marker
  without projection row, and duplicate tool name entries.

## Entity State x Write Matrix

### ImportRequest / Decision / Event

States:

```text
does-not-exist
accepted
rejected
replayed
pending-repair
```

#### `does-not-exist` x W1 `object-container/import-material`

- If valid, becomes `accepted` only when common base material is present or
  repairably pending.
- If invalid, becomes `rejected`.
- R7: returns accepted/rejected decision with request, actor, policy, reason,
  errors, and event ids when accepted.
- R1/R2/R3/R4/R5/R6/R8: accepted valid material is visible through common
  reads; rejected material is absent from common base reads.
- R9/R10/R11: projections/audit either reflect accepted/rejected state or show
  explicitly pending repair, not silent success.

#### `accepted` x W1 same idempotency key and same logical input

- State becomes `replayed` for the new audit attempt or returns the prior
  accepted decision.
- R7: shows replay relationship to prior decision.
- R1/R2/R3/R4/R5/R6/R8: unchanged; no duplicate common material.
- R9/R10/R11: unchanged except optional audit replay entry.

#### `accepted` x W1 same idempotency key but conflicting logical input

- Reject the conflicting replay.
- R7: returns rejected conflict with prior decision reference.
- R1/R2/R3/R4/R5/R6/R8: prior accepted common material remains unchanged.
- R9/R10/R11: prior projections remain; conflict audit is visible.

#### `pending-repair` x W1 retry or repair

- Missing base/projection material is repaired or the decision becomes rejected
  with durable explanation.
- R7: reports repaired or rejected state, not permanent silent pending.
- R1/R2/R3/R4/R5/R6/R8: after repair, reads prove required common material.
- R9/R10/R11: projection repair status is visible.

### SourceArtifact

States:

```text
does-not-exist
captured-raw
captured-redacted
hash-anchor-only
newer-version-exists
```

#### `does-not-exist` x W1/W2 markdown import

- Becomes `captured-raw`.
- R1: returns exact raw markdown, ref, hash, format, created-by/request.
- R2: document ObjectContainer is readable.
- R4: markdown DerivedUnits are readable.
- R5: source anchors exist for document and units.
- R6/R8/R9: composition/outline reads show source order.
- R10/R11: not related except audit may show import.

#### `does-not-exist` x W1/W3/W4/W5 transcript import/watch

- Becomes `captured-raw`, `captured-redacted`, or `hash-anchor-only` according
  to policy.
- R1: returns only material allowed by policy plus source identity.
- R2/R3: transcript conversation/message/tool containers and revisions are
  readable when valid records exist.
- R5: anchors point back to file/line/offset/hash or source-native item.
- R6/R8/R10/R11: conversation/tool structure and operational audit can resolve
  the source.
- R4/R9: unrelated unless the source also produced DerivedUnits.

#### `captured-*` x W1/W2/W3/W4 same ref/version/hash

- No duplicate SourceArtifact.
- R1: same source row/version.
- R2/R3/R4/R5/R6/R8/R9/R10/R11: unchanged except replay/audit views.

#### `captured-*` x W1/W2/W3/W4 same ref with new hash/version

- A new SourceArtifact version is created. Older source remains readable.
- R1: old and new versions are readable by id; latest-by-ref resolves by
  explicit policy.
- R2/R3/R4/R5/R6/R8/R9/R10: new material is visible under the new version;
  graduated/edited old containers are not overwritten.
- R11: audit explains changed source version.

#### `captured-*` x W7 object edit

- SourceArtifact remains immutable.
- R1: returns byte-identical or policy-identical captured source.
- R2/R3/R4/R5/R6/R8/R9/R10: edited container/revision reads may change, but
  source reads do not.

### ObjectContainer

States:

```text
does-not-exist
durable-current
durable-with-revisions
projection-only-invalid
```

#### `does-not-exist` x W1/W2 markdown document import

- Creates durable `:document` ObjectContainer.
- R2: common container read returns the document.
- R1: source artifact exists.
- R5: document source anchor exists.
- R6/R8/R9: document can be traversed to source-derived children.
- R3: current revision may be nil or document-level handle according to
  accepted content policy, but this must be explicit.
- R10/R11: unrelated except import audit.

#### `does-not-exist` x W1/W5 transcript native object import

- Creates durable transcript ObjectContainers for conversation, messages, tool
  calls, tool results, artifacts/runs when present.
- R2: common container read returns the object.
- R3: common revision read returns redacted/structured content or summary.
- R1/R5: source artifact and anchor explain origin.
- R6/R8/R10: conversation/tool structure is traversable/projection-readable.
- R11: audit/tool indexes point to common container ids.
- R4/R9: unrelated unless a source-derived unit is also involved.

#### `does-not-exist` x W5 parse-error observation

- Must remain `does-not-exist`; parse errors do not create fake containers.
- R2/R3/R5/R6/R8: no common container/revision/anchor/edge for a fake object.
- R10/R11: parse error is visible in projection/audit.
- R7: decision/audit explains rejected or non-container source record.

#### `does-not-exist` x W7 edit of DerivedUnit

- Creates durable `:text-block` ObjectContainer.
- R2: new container is readable.
- R3: first revision is readable as current.
- R4: derived unit reports graduated with container id.
- R5: source anchor is copied/preserved for the container.
- R6/R9: structure/order unchanged; projection shows edited content.
- R1: raw source unchanged.

#### `durable-current` x W7 edit

- Becomes `durable-with-revisions`; current revision advances.
- R2: same container id.
- R3: newest revision current; old revision remains in history.
- R4: if container came from a DerivedUnit, unit still points to same
  container.
- R5: provenance anchor remains.
- R6/R9/R10: projections/traversals show newest content after repair/rebuild.
- R1: source unchanged.

#### `projection-only-invalid` x any import/edit

- A projection-only container-shaped row is not accepted as base truth.
- R2/R3/R5/R6/R8: common reads return missing until a real common
  ObjectContainer/Revision/Anchor/Edge exists.
- R10/R11: projection can display operational data only if labeled as
  non-canonical and repairable.
- R7: audit must not claim common base acceptance.

### DerivedUnit

States:

```text
does-not-exist
derived
graduated
drift-detected
```

#### `does-not-exist` x W1/W2 markdown import

- Becomes `derived` for anonymous markdown spans.
- R4: returns unit with derived content, source span, distiller id/version, and
  `graduated = false`.
- R1/R5: source and anchor are readable.
- R6/R8/R9: unit appears in source order through common edges/projection.
- R2/R3: no text-block ObjectContainer/revision yet.
- R10/R11: unrelated except import audit.

#### `does-not-exist` x W1/W5 transcript import with stable native id

- Remains `does-not-exist`; transcript stable items become ObjectContainers
  rather than DerivedUnits.
- R4: nil.
- R2/R3/R5/R6/R8/R10: transcript object, revision, anchor, and edges are
  visible through common/projection reads.
- R11: audit/tool index may reference common containers.

#### `derived` x W7 edit

- Becomes `graduated`.
- R4: returns graduated state with container id and current revision content.
- R2/R3: container and first revision are readable.
- R5: source anchor remains available for unit and container.
- R6/R9: outline/composition position remains stable.
- R1: source unchanged.

#### `graduated` x W7 edit

- Stays `graduated`; target container receives a new revision.
- R4: same container id, newest content.
- R2/R3: same container, advanced revision.
- R5/R6/R9: anchor/structure unchanged.
- R1: source unchanged.

#### `graduated` x W1/W2 re-import or re-distill

- Stays `graduated`; authored container is not overwritten.
- R4/R2/R3: authored content remains current.
- R1/R5/R6/R8/R9: new source version may show drift separately, but base
  authored content is preserved.
- R7/R11: audit may report drift or re-import.

### Revision

States:

```text
does-not-exist
current
historical
structured-content
redacted-content
```

#### `does-not-exist` x W1/W5 transcript native object import

- Creates initial Revision for message/tool/artifact objects when content or
  state exists.
- R3: returns content/structured payload as current.
- R2: container points to current revision.
- R5: anchor explains source of the content.
- R10/R11: projections/tool indexes can summarize from common revision or
  reference it.
- R1/R6/R8: source and structure are readable.

#### `does-not-exist` x W7 DerivedUnit edit

- Creates first Revision for graduated text-block container.
- R3: current revision has edited content and hash.
- R2/R4/R5/R6/R9: container/unit/anchor/outline reads reflect graduation.
- R1: source unchanged.

#### `current` x W7 object edit

- Existing current becomes `historical`; new Revision becomes `current`.
- R3: current read returns newest; history read returns both in order.
- R2/R4/R9/R10: projections resolve newest content after update/repair.
- R5/R6: provenance/structure unchanged unless a separate structural write
  occurs.

#### `current` x W1/W2/W3/W4 re-import

- Re-import does not overwrite authored Revision.
- R3: current revision remains authored content.
- R1/R5/R6/R8/R9/R10/R11: source/audit may show new source version or drift.

### SourceAnchor

States:

```text
does-not-exist
exists
conflicting
repair-pending
```

#### `does-not-exist` x W1/W2 markdown import

- Creates anchors for SourceArtifact/document/DerivedUnits.
- R5: target lookup returns source id, source hash, span/path/offset.
- R1: source can be read according to policy.
- R2/R4/R6/R8/R9: containers/units/edges/projection can show provenance.

#### `does-not-exist` x W1/W5 transcript import

- Creates anchors for conversation/message/tool/result/artifact objects.
- R5: target lookup returns source family, file id/path, byte offset/length,
  line hash, event type, and source-native id/pointer when available.
- R2/R3/R6/R8/R10/R11: common/projection reads can trace back to source.
- R1: source artifact read obeys redaction/hash policy.

#### `exists` x W7 DerivedUnit graduation

- Anchor is preserved or copied to the graduated container.
- R5: unit and container both resolve provenance or the container anchor points
  to the same source span.
- R2/R3/R4/R6/R9: graduation reads keep provenance and position.
- R1: source unchanged.

#### `exists` x W1/W5 duplicate source observation

- No duplicate/conflicting anchor.
- R5: same target anchor result.
- R2/R3/R6/R8/R9/R10/R11: no duplicate common/projection structure.

#### `conflicting` x W1/W5 import

- Reject or enter `repair-pending`; do not silently overwrite provenance.
- R7/R11: conflict visible in audit.
- R5: prior accepted anchor remains or missing anchor is explicitly pending.
- R2/R3/R6/R8/R9/R10: base objects are not claimed fully accepted unless
  anchor invariant is satisfied or repairable.

### CompositionEdge

States:

```text
does-not-exist
exists
duplicate-same
conflicting
repair-pending
```

#### `does-not-exist` x W1/W2 markdown import

- Creates document/block containment and order edges.
- R6: children/parent reads traverse markdown structure in source order.
- R2/R4/R8/R9: document outline is visible through common base/projection.
- R1/R5: source and anchors explain edge origin.

#### `does-not-exist` x W1/W5 transcript import

- Creates conversation/message/tool/result/artifact source-declared edges.
- R6: common composition reads traverse transcript structure.
- R2/R3/R5/R8/R10/R11: transcript projection and tool index reference common
  structure.
- R1: source provenance readable by policy.

#### `exists` x W7 content edit

- Edge remains; content edit does not imply structural edit.
- R6: same parent/child/order.
- R2/R3/R4/R9/R10: node content changes through revision/projection, not by
  changing edge identity.

#### `duplicate-same` x W1/W2/W5 re-import

- No duplicate edge.
- R6/R8/R9/R10: same traversal result.
- R7/R11: replay/audit may record idempotent duplicate observation.

#### `conflicting` x W1/W5 import

- Reject, or mark `repair-pending` if source order/parent can be reconciled
  from later records.
- R6: prior accepted structure remains or conflict is absent until repaired.
- R7/R11: conflict visible.
- R2/R3/R5/R8/R9/R10: no projection should pretend the conflicting edge is
  accepted base truth.

#### `repair-pending` x W5 later matching record

- Becomes `exists` if missing parent/child is found, for example a tool result
  observed before the tool call.
- R6/R10/R11: repaired structure becomes visible.
- R2/R3/R5/R8: both objects and anchors remain real throughout.
- R7: audit explains repair.

### Source-Specific Projection / Index Row

States:

```text
does-not-exist
materialized
stale
repair-pending
non-canonical-parse-error
```

#### `does-not-exist` x W8 projection upsert after common base write

- Becomes `materialized`.
- R9/R10/R11: projection/index/audit views return the row.
- R2/R3/R5/R6/R8: common base reads also prove canonical identity.
- R7: decision/audit can link projection to accepted common event/request.

#### `does-not-exist` x W5 parse-error observation

- Becomes `non-canonical-parse-error`.
- R10/R11: parse error visible with redacted preview/source info.
- R2/R3/R5/R6/R8: no fake common container/edge is returned.
- R7: audit explains parse error.

#### `materialized` x W7 content edit

- Becomes `stale` until rebuilt/upserted, or remains materialized if updated in
  the same accepted flow.
- R9/R10: must eventually show newest common revision content.
- R2/R3: common current revision is authoritative while projection is stale.
- R7/R11: stale/repair state must be inspectable if user-facing.

#### `materialized` x W1/W2/W5 duplicate import

- Remains materialized; no duplicate projection row.
- R9/R10/R11: same row/order.
- R2/R3/R5/R6/R8: common base unchanged.

#### `repair-pending` x W8 repair

- Becomes `materialized`.
- R9/R10/R11: previously missing projection/index/audit row appears.
- R2/R3/R5/R6/R8: common base reads were already true or become true in the
  same repair flow.
- R7: repair is auditable.

### Transcript File Offset / Source-Line Ledger

States:

```text
does-not-exist
observed
advanced
stale-after-file-change
repair-needed
```

#### `does-not-exist` x W6 file-state observation

- Becomes `observed` or `advanced`.
- R11: file offset read returns file id/path/source/last complete byte offset,
  line count, request id, and update time.
- R7: audit/run status can explain acquisition progress.
- R1/R2/R3/R5/R6/R8/R10: common base reads are only guaranteed for source
  records whose import material was accepted.

#### `observed` x W5 source observation

- Source-line ledger may mark the line observed only in a way that still allows
  missing common base/projection repair.
- R11: audit/ledger view explains processed line and repair state.
- R2/R3/R5/R6/R8/R10: valid record is visible through common/projection reads
  after acceptance.
- R7: decision proves accepted/rejected/pending status.

#### `advanced` x W4 watch restart

- Watch resumes from greatest safe complete offset.
- R11: offset read returns resume point.
- R7: run status shows resumed watch.
- R1/R2/R3/R5/R6/R8/R10: no skipped complete records relative to accepted
  audit state.

#### `stale-after-file-change` x W4/W6

- Must repair, rescan, or reject unsafe resume. It must not silently skip
  records.
- R11/R7: stale state or repair decision visible.
- R1/R2/R3/R5/R6/R8/R10: common base reads remain consistent with accepted
  records only.

#### `repair-needed` x W5/W8 retry

- Missing common base/projection writes are repaired without being suppressed
  by an earlier ledger marker.
- R2/R3/R5/R6/R8/R10/R11: common and projection reads converge.
- R7: repair decision/audit visible.

### Ingest Run / Audit

States:

```text
does-not-exist
accepted-running
rejected
complete
failed
complete-with-repair-needed
```

#### `does-not-exist` x W3/W4 request

- Becomes `accepted-running` or `rejected`.
- R11: run status returns current state.
- R7: import decision/audit explains accepted/rejected request.
- R1/R2/R3/R5/R6/R8/R10: no common material is implied until observations are
  accepted.

#### `accepted-running` x W5 source observation

- Progress counts increase and per-record audit rows appear.
- R11: run/audit shows observed count, parse-error count, common object count,
  and policy/redaction context.
- R2/R3/R5/R6/R8/R10: valid records become common/projection material.
- R7: per-record accepted/rejected/pending state is inspectable.

#### `accepted-running` x W6 file-state observation for empty file

- Empty source is auditable; may create source artifact according to policy but
  no fake message containers.
- R1: source artifact/provenance available if policy accepts empty source.
- R2/R3/R5/R6/R8/R10: no fake conversation/message/tool objects solely because
  file is empty.
- R11/R7: empty-file audit visible.

#### `accepted-running` x completion claim

- Becomes `complete` only when required acquisition/progress facts are durable.
  If common base/projection repair remains, state is
  `complete-with-repair-needed` or equivalent, not silent success.
- R11: run status exposes completion/repair state.
- R7: decisions explain accepted/rejected/pending material.
- R1/R2/R3/R5/R6/R8/R10: accepted valid records are readable as common and
  projection material.

#### `failed` or `complete-with-repair-needed` x retry/repair

- Becomes `complete` after repair or remains failed with durable explanation.
- R11/R7: repair/failure visible.
- R1/R2/R3/R5/R6/R8/R10: reads prove the material that was accepted; missing
  material is not hidden by success status.

## Acceptance Proof Requirements For Later Phases

Phase 1 and later implementation/testing phases must be able to prove:

```text
one common read fetches a markdown-created document ObjectContainer
one common read fetches a markdown-graduated text-block ObjectContainer
one common read fetches a transcript-created chat conversation ObjectContainer
one common read fetches a transcript-created chat message ObjectContainer
one common read fetches a transcript-created tool-call ObjectContainer
one common read fetches a transcript-created tool-result ObjectContainer
one common Revision read returns redacted tool-result content when source has result content
one common SourceArtifact read works for markdown and transcript source
one common SourceAnchor read traces markdown and transcript targets to source
one common CompositionEdge read traverses markdown document structure
one common CompositionEdge read traverses transcript conversation/tool structure
transcript conversation projection remains available as a source-specific read model
markdown outline projection remains available as a source-specific read model
parse errors are visible without fake base containers
re-harvest and watch do not duplicate common objects
source-line/idempotency ledger cannot hide missing common base or projection writes
```

## Explicit Fail Conditions For Phase 1

A later plan fails this Phase 0 contract if it creates or preserves this as the
only durable identity path:

```text
markdown containers   -> common ObjectContainer store
transcript containers -> transcript-local durable container store
```

A later plan also fails if:

```text
tool_result content is stored only in a transcript projection or placeholder
SourceArtifact/SourceAnchor/CompositionEdge for transcript are only transcript-local base rows
common reads cannot fetch transcript-created containers
parse errors become fake ObjectContainers
redaction policy can be bypassed by raw transcript source persistence
idempotency/ledger success can mask partial missing base writes
projection correctness depends on private reconstruction from source-specific rows
```

## Stop Condition

This Phase 0 artifact intentionally stops at product behavior and implied
requirements. Phase 1 must design the Rama plan from this file and validate it
before implementation.
