# Object-Container Common Infra Track

Status: product/Rama seed, 2026-06-07.

This is the new track. It replaces the old "continue transcript F5/F6 next"
direction.

This document is not a Rama Phase 0 artifact yet. It is the product-level
starting point that a Rama Phase 0 session should read before creating
`IMPLICIT_SPEC.md` in this folder.

## Origin Prompt

```text
ok now agreeing on that .. lets fuck the f5, f6 we are not going to work on
that create a new folder for the common infra track and lets populate it with
a base doc that is more geared towards what the product is and something that
the rama skill can take and work off of got my point?
```

Immediate prior question:

```text
the plan is now to just get started on the common infra and after this task we
will have the

.md file         -> Markdown Ingester      \
chat transcript -> Transcript Ingester      -> Object Container Kernel

right???
```

## Decision

Yes. The next track is common object-container import infrastructure.

Do not continue transcript F5/F6 as the next implementation task. Those defects
remain real in the transcript-local prototype, but they are no longer the
frontier. The frontier is making the common substrate actually common.

Target shape:

```text
.md file         -> Markdown Ingester      \
chat transcript -> Transcript Ingester      -> Object-Container Kernel
code artifact   -> Code/Artifact Ingester  /   later
Roam/outliner   -> Roam Ingester           /   later
canvas/tldraw   -> Canvas Ingester         /   later
```

Immediate slice:

```text
.md file         -> Markdown Ingester      \
chat transcript -> Transcript Ingester      -> Object-Container Kernel
```

The product success condition is simple:

```text
markdown-created material and transcript-created material both become
first-class ObjectContainers in the same common substrate.
```

## Product Statement

Softland needs a single native object substrate that imported material can enter
without becoming trapped in source-specific islands.

When a user imports a markdown file or a chat transcript, Softland should:

```text
1. preserve the raw source,
2. identify the source-native or derived units,
3. create native Softland objects where identity exists,
4. keep exact anchors back to the source,
5. preserve source-declared order and composition,
6. expose the objects through common reads,
7. allow source-specific projections without making them canonical truth.
```

The user-facing result is not:

```text
"markdown got indexed"
"a transcript parser ran"
"a module has rows named containers"
```

The user-facing result is:

```text
I can bring source material into Softland.
The original source is preserved.
The material becomes native enough to inhabit, reference, quote, edit, relate,
continue, and project later.
Markdown and transcript objects can meet in the same world because they share
the same base object substrate.
```

## The Mistake This Track Fixes

The current repo has two useful tracks, but they do not yet meet at the right
layer.

Current markdown path:

```text
markdown/plain text source
  -> src/app/server/rama/object_container.clj
  -> SourceArtifact / ObjectContainer / Revision / DerivedUnit /
     SourceAnchor / CompositionEdge
```

Current transcript path:

```text
chat transcript source
  -> src/app/server/rama/dogfood/transcript.clj
  -> src/app/server/rama/dogfood/transcript_ingest.clj
  -> transcript-local TranscriptContainerRow / SourceArtifactRow /
     SourceAnchorRow / CompositionEdgeRow
```

The transcript path is product-correct as an ingester proof. It preserves
important work:

```text
harvest/watch acquisition modes
byte-correct JSONL reader
redaction before durable persistence
native transcript id extraction
conversation/message/tool-call/tool-result mapping
file offset handling for watch resume
audit/run status behavior
tests and fixtures around parsing and ingest
```

But storage-wise it is too local. It creates an object-container-shaped island
instead of feeding the common object-container kernel.

This track fixes that boundary.

## Product Ontology

### Object-Container Kernel

The object-container kernel owns canonical base truth:

```text
SourceArtifact
ObjectContainer
Revision
DerivedUnit
SourceAnchor
CompositionEdge
accepted request / decision / event audit
```

These are not markdown-specific or transcript-specific.

### Source Ingester

A source ingester owns source-specific acquisition and interpretation:

```text
read or watch source material
parse the source format
redact source-specific sensitive data before durable writes
extract stable native ids when available
derive units when stable native ids are absent
produce anchors back to exact source positions
produce composition/order facts declared by the source
emit optional projection hints or source-specific read model events
```

The ingester may own operational and read-model state:

```text
file offsets
watch run status
parse errors
conversation projection
tool-call index
markdown outline cache
source-specific lookup index
```

But it must not own the only durable identity store for imported material.

### Import Material

Every ingester should emit the same common import shape.

Working name:

```text
ObjectContainerImport
```

Required logical parts:

```text
import request
  request id
  actor
  policy / visibility
  source family
  source format
  source version
  idempotency key

source artifact
  source id
  source ref
  source hash or version
  raw or policy-redacted material
  source metadata

object containers
  native identity-bearing objects
  container kind
  owner / actor
  source origin
  current revision pointer
  summary or content handle

derived units
  addressable source-derived spans without stable native id
  distiller/interpreter id
  source span
  graduation state

revisions
  versioned content or state for object containers
  content hash
  parent revision, when applicable

source anchors
  source id
  target id
  source-native id, when present
  byte/line/span/json pointer, when present
  anchor hash

composition edges
  parent contains child
  child follows sibling
  message produced tool call
  tool call produced tool result
  artifact derived from source item
  order key

projection hints
  optional source-specific hints for read models
  not canonical base truth
```

## Format Mapping

### Markdown

Markdown usually has weak native identity.

Expected mapping:

```text
file/ref/hash             -> SourceArtifact
document/file             -> ObjectContainer kind :document
heading/paragraph/list    -> DerivedUnit, unless already graduated
source spans              -> SourceAnchor
document/block tree       -> CompositionEdge
edit touched derived unit -> ObjectContainer kind :text-block + Revision
```

Markdown proves:

```text
anonymous spans become DerivedUnits
DerivedUnits can graduate into ObjectContainers
outline is a projection, not the canonical ontology
```

### Transcript

Transcripts usually have stronger native identity.

Expected mapping:

```text
transcript file/version   -> SourceArtifact
conversation/session      -> ObjectContainer kind :chat-conversation
message uuid/line key     -> ObjectContainer kind :chat-message
tool_use id               -> ObjectContainer kind :tool-call
tool_result id/line key   -> ObjectContainer kind :tool-result
artifact id, if present   -> ObjectContainer kind :chat-artifact
raw line / byte offset    -> SourceAnchor
conversation order        -> CompositionEdge
tool call/result relation -> CompositionEdge
```

Transcript proves:

```text
stable source-native ids become ObjectContainers immediately
chat material is native enough to inspect, quote, fork, continue, and relate
tool calls/results are not hidden inside a blob
conversation view is a projection over common objects
```

## Boundaries

### In This Track

```text
common import contract
common object-container base write path
markdown adapter into common import
transcript adapter into common import
common reads that can see material from both ingesters
minimal source-specific projections only where needed to verify product shape
tests proving both markdown and transcript feed the same substrate
```

### Out Of This Track

```text
fixing transcript F5/F6 inside the old transcript-local store
Roam/outliner import
code/repo import
canvas/tldraw import
Space runtime
LLM continuation runtime
semantic search
active/base layer promotion
view layout state
full migration of old transcript-local data
```

The old F5/F6 bugs become carried requirements, not the next implementation
target.

```text
F5 carried requirement:
  tool_result content must materialize as real common ObjectContainer +
  Revision content, not as an empty placeholder.

F6 carried requirement:
  retry/idempotency must not allow a ledger write to hide later missing common
  base writes or source-specific projection writes.
```

## Rama Build Instructions

This folder should now become the Rama phased build folder for common infra.

Use the Rama skill normally:

```text
Phase 0 -> IMPLICIT_SPEC.md
Phase 1 -> PLAN.md
Phase 2 -> PLAN_VALIDATION.md
Phase 3 -> implementation code
Phase 4 -> IMPLEMENTATION_VALIDATION.md
Phase 5 -> tests
Phase 6 -> TEST_VALIDATION.md
Phase 7 -> passing tests
```

The first Rama session should create:

```text
docs/current-mental-model/build/object-container-common-infra/IMPLICIT_SPEC.md
```

It should read:

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

It must not treat old transcript F5/F6 as the active task.

## Phase 0 Questions

The Phase 0 artifact must answer product behavior before PState/depot design:

```text
1. What does common import accept?
2. What does common import reject?
3. What object kinds are required for markdown and transcript?
4. When does an ingested thing become ObjectContainer vs DerivedUnit?
5. What raw source is preserved, and what must be redacted first?
6. What source anchors are required for source-level trust?
7. What composition edges are common enough to store as base truth?
8. What read models are source-specific projections only?
9. What reads prove the two ingesters share one substrate?
10. What invariants prevent source-specific base object islands?
```

## Non-Negotiable Gates

A plan fails if it creates this shape:

```text
markdown containers     -> common ObjectContainer store
transcript containers   -> transcript-local durable identity store
```

A plan passes only if it creates this shape:

```text
markdown source         -> markdown interpreter      \
                                                       -> common ObjectContainer
transcript source       -> transcript interpreter    /   substrate
```

Required proof:

```text
one common read can fetch a markdown-created object container
one common read can fetch a transcript-created object container
one common composition read can traverse common source-declared structure
one common source-anchor read can trace both objects back to their sources
source-specific projections are explicitly rebuildable or non-canonical
```

## Product Acceptance Story

After the common infra slice lands, the product should be able to say:

```text
I imported a markdown file.
I imported a transcript file.

The markdown document and transcript conversation both entered the same object
container kernel.

Markdown anonymous blocks are visible as derived units and can graduate.
Transcript conversations/messages/tool calls/tool results are visible as
native object containers immediately.

Both retain source artifacts and anchors.
Both expose composition/order through common edges.
Both can later be mounted into views, spaces, agent context, or canvas without
re-parsing the original source as a private one-off.
```

That is the whole point of this track.

