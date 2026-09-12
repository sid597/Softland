# Common-Kernel Replan - Transcript Ingest

Status: corrective Phase 0/1 gate artifact, 2026-06-07.

Purpose: repair the boundary error where transcript ingest was product-described
as an object-container ingester but Phase 1 planned a transcript-local
object-container-shaped store.

This is not implementation code. Do not write Rama topology changes from this
artifact alone; use it to redo a proper Phase 1 plan.

## Verdict

The user's intended architecture is correct:

```text
.md file         -> Markdown Ingester      \
chat transcript -> Transcript Ingester      -> Object-Container Kernel
code artifact   -> Code/Artifact Ingester  /
```

The current transcript implementation is not factored that way. It should be
treated as a source-specific proof whose source acquisition, parser, redaction,
and tests should be preserved, while its transcript-local base object PStates
should not be treated as the final shared object-container substrate.

## Inputs Read

```text
docs/current-mental-model/architecture/object-container-ingester-contract.md
docs/current-mental-model/architecture/object-container-spec.md
docs/current-mental-model/build/chat-ingester/PRODUCT.md
docs/current-mental-model/build/chat-ingester/IMPLICIT_SPEC.md
docs/current-mental-model/build/chat-ingester/PLAN.md
docs/current-mental-model/build/chat-ingester/PLAN_VALIDATION.md
docs/sessions/next-prompt.md
.agents/skills/rama/SKILL.md
.agents/skills/rama/references/phases.md
.agents/skills/rama/references/phase-1-plan.md
```

## The Failure In One Line

The common object-container substrate is not common yet.

Current transcript plan/code makes these source-local:

```text
container identity
source artifact row
source anchor row
composition edge row
```

Those are common base facts and must be owned by the object-container substrate,
not by each source ingester.

## Base Truth vs Transcript-Specific Read Models

### Common Base Truth

These must be stored in the shared object-container substrate:

```text
SourceArtifact
ObjectContainer
Revision
DerivedUnit, only when source lacks native identity
SourceAnchor
CompositionEdge
accepted request / decision / event audit
```

For transcript ingest, native identity exists for most child objects:

```text
conversation id / session id -> ObjectContainer
message uuid                 -> ObjectContainer
tool-use id                  -> ObjectContainer
tool-result tool-use id       -> ObjectContainer
artifact id, when present     -> ObjectContainer
run/execution id, when present -> ObjectContainer
```

Transcript import should therefore create durable child `ObjectContainer`s
immediately, not `DerivedUnit`s.

### Transcript-Specific Read Models

These may remain transcript-owned because they are projections, indexes, or
operational state:

```text
ingest run status
file offset state for watch resume
source-line dedup ledger
conversation projection
tool-call index
audit/read projection
parse-error visibility view
```

These should not be the only durable identity store for transcript material.

## Common Object Kinds For Transcript

Required `ObjectContainer` kinds:

```text
:chat-conversation
:chat-message
:tool-call
:tool-result
:chat-artifact, when present
:agent-run or :execution-run, when present
```

Each container needs:

```text
container id
container kind
visibility / policy
source id
source anchor id
current revision id
current content or summary
current content hash
created-at
created-by / request id
event id
```

The current `TranscriptContainerRow` fields must be mapped into the common
`ObjectContainerRow` plus `RevisionRow` shape, not preserved as the only base
truth.

## SourceArtifact For Transcript

Transcript import needs a file-level `SourceArtifact`:

```text
source family: :claude-code | :codex | future chat source
source ref: file path or stable external artifact ref
source hash: byte-correct content hash or source version hash
source format/version
raw or policy-redacted captured source material
created-at / created-by / event id
```

The source artifact is immutable. Re-harvest of a changed file creates a new
source artifact version. It does not mutate old containers.

For very large transcripts, a future plan may decide to store redacted source
chunks instead of a single raw text field, but the logical `SourceArtifact`
identity still belongs to the common substrate.

## SourceAnchor For Transcript

Every transcript container created from a source record should have a source
anchor:

```text
container id
source id
file id
file path
byte offset
byte length
line hash
source event type
optional JSON pointer / block pointer inside the parsed line
```

For tool calls and tool results, anchors must point to the line that actually
introduced that object:

```text
tool call   -> assistant line with tool_use block
tool result -> user/tool_result line with result content
```

Do not anchor a real tool result to the earlier placeholder tool_use line.

## Revision For Transcript

Transcript containers should carry content through common `RevisionRow`s.

Examples:

```text
conversation revision:
  metadata / source summary / session info

message revision:
  redacted message text or structured message payload summary

tool-call revision:
  tool name, tool-use id, redacted input

tool-result revision:
  tool-use id, redacted result content

artifact revision:
  artifact metadata or redacted artifact payload summary
```

The current transcript-local `revision-content` field maps to common
`RevisionRow.content-text` or a future structured revision payload. Do not keep
it only inside `TranscriptContainerRow` as canonical truth.

## CompositionEdges For Transcript

Common edges:

```text
conversation contains message
message follows previous message
assistant message produced tool call
tool call produced tool result
message produced artifact
artifact derived-from message/tool result, if source supports it
```

The source-declared or source-physical order must be preserved. Within a single
file, byte offset is an acceptable order key. For conversations spanning
multiple files, timestamp + file/offset tie-breaker should be planned rather
than pretending byte offsets are globally comparable.

## Import Flow

Correct flow:

```text
source acquisition
  read/watch files
  byte-correct line reader
  parse/redact source-specific JSONL
  produce transcript observations

transcript interpreter
  extract native ids
  extract content summaries
  derive source anchors
  produce common import material

object-container substrate
  accept/reject import material
  write SourceArtifact/ObjectContainer/Revision/SourceAnchor/CompositionEdge

transcript read models
  write conversation projection
  write tool-call index
  write audit/run/file-offset PStates
```

The next Phase 1 must decide whether the object-container substrate accepts
import material by:

```text
A. extending object_container.clj with a generic import request/depot, or
B. creating a new common object-container import module/API owned by the same
   canonical object substrate.
```

Do not force transcript through the current markdown-only
`source-ingest-request`. That request has `:source-format :markdown` and
`markdown-block-v0`; it is not the general import contract.

## Reads And Writes To Replan

### Writes

Required write operations:

```text
transcript/harvest-request
transcript/watch-request
transcript/source-observation
transcript/file-state-observation
object-container/import-source-artifact
object-container/upsert-native-container
object-container/upsert-revision
object-container/upsert-source-anchor
object-container/upsert-composition-edge
transcript/projection-upsert
transcript/audit-upsert
transcript/tool-index-upsert
transcript/run-status-upsert
```

The Phase 1 plan must state which writes enter the common object-container
depot/topology and which writes remain transcript-local projection/index writes.

### Reads

Required reads:

```text
read object container by id, common
read source artifact by id/ref/hash, common
read source anchor by target id, common
read composition children by parent, common
read conversation projection, transcript-specific
read tool calls by name/tool-use id, transcript-specific
read audit entries by ingest request, transcript-specific
read ingest run status, transcript-specific
read file offset for watch resume, transcript-specific
```

The plan must avoid client-side multi-read patterns when a query topology is
needed. It must also state which transcript read models are denormalized from
common object-container writes.

## PState Ownership Rule

Pass:

```text
common module owns:
  $$source-artifacts-...
  $$containers-by-id
  $$revision-history-by-container
  $$source-anchors-...
  $$composition-...

transcript module owns:
  $$ingest-runs
  $$source-ledger
  $$file-offsets
  $$conversation-projection
  $$tool-calls-by-name
  $$audit-entries
```

Fail:

```text
transcript module owns $$containers-by-id as the only durable identity store
for conversations/messages/tool calls/tool results.
```

## What To Do With Current F5/F6

F5 and F6 should be carried forward as correctness requirements, not blindly
patched into the transcript-local base store.

F5 requirement:

```text
tool_result content block
  -> common :tool-result ObjectContainer
  -> common Revision with redacted result content
  -> SourceAnchor to the result line
  -> CompositionEdge tool-call produced tool-result when matching tool-call exists
  -> transcript projection exposes result content
```

F6 requirement:

```text
source-line dedup must not suppress repair of secondary projections/indexes
after partial failure.
```

The replan must decide whether transcript-local projections are:

```text
1. written after common object-container writes in the same topology,
2. derived from common object-container events via internal depot,
3. repairable/reconcilable from common base PStates.
```

Do not put a source-ledger marker in front of later non-repairable projection
writes.

## Test Gate

Tests must prove:

```text
transcript conversation is readable as a common ObjectContainer
transcript message is readable as a common ObjectContainer
tool call is readable as a common ObjectContainer
tool result is readable as a common ObjectContainer with redacted result content
source anchors are readable through the common anchor read path
composition edges are readable through the common edge read path
conversation projection remains available as transcript-specific read model
re-harvest does not duplicate common containers
watch and harvest produce the same common object identities
parse errors create audit/projection entries without fake base containers
```

A test that only reads `TranscriptContainerRow` from transcript-local
`$$containers-by-id` does not prove the common-kernel contract.

## Open Design Decisions For Proper Phase 1

The next real Phase 1 plan must choose:

```text
1. Extend `object_container.clj` or introduce a new common import API/module?
2. How to represent structured revisions without collapsing into text-only
   `content-text`?
3. How to version large SourceArtifacts without storing huge raw blobs in one
   PState value?
4. How transcript projections subscribe to or derive from common base writes.
5. Whether import writes use one topology or split common-base writes and
   transcript projections through internal depots.
6. How to preserve low-latency watch visibility without making projection rows
   the base truth.
7. How to migrate or deprecate current `TranscriptContainerRow` tests.
```

## Current Verdict

```text
COMMON_KERNEL_REPLAN: architecture-gap-confirmed
```

Do not continue implementation until a new Phase 1 plan passes the commonness
gate.
