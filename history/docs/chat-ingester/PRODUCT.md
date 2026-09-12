# Transcript Ingest Product Doc

Status: product/architecture note, 2026-06-07.

This is not a Rama phase artifact yet. It is the product contract for transcript
ingest after the object-container correction. The later implementation folder
should treat this as input, then produce `IMPLICIT_SPEC.md`, `PLAN.md`,
validation artifacts, code, and tests through the Rama phased process.

Related:

```text
architecture/object-container-spec.md
architecture/object-container-reviewer-world-model.md
architecture/dogfood-runtime/transcript-capture.md
src/app/server/rama/dogfood/transcript.clj
src/app/server_jetty.clj
test/app/server/rama/dogfood_transcript_test.clj
test/app/server/parser_test.clj
test/fixtures/claude-stream-sample.jsonl
```

## Origin Questions

```text
so for the next implementation i think we should only focus on what we have
right now and build the different connectors later on ... so for now i have .md
files, softland code files, chat artifacts that needs to be ingested ..
( how is hte chat ingest articture different or like when is it going to be
used vs this object one????)

so we already have one type of ingester example but it is not using the general
architecture that we have created ... or maybe what i am calling chat ingester
right now is doing much more and hence convoluted .... in my mind what chat
ingester should do is have a way to take the existing chat structures and have
them in softland native representation lie the atomic container which we have
made now as object container??????

no transcript capture is an example of object container .... it is the way that
type of data is ingested by the tarnscript interpretor .. we keep both the raw
but also store it in softland way ... space is different it is not directly
related to this conversation ... and transcript capture can be passive watch but
also one time import just like normal import works
```

## Why This Matters

The old conversation accidentally made "chat ingest" sound like three different
things:

```text
1. importing existing transcript/chat artifacts
2. representing those artifacts as native Softland objects
3. using chat as an active local world with LLM execution
```

Only the first two belong to transcript ingest.

The third belongs to Space/LLM runtime. Space is important, but it is not the
right boundary for this product doc.

The corrected product move is:

```text
Transcript ingest is one domain-specific object-container ingester.

It acquires transcript-shaped source data, interprets the source format, keeps
the source material, and emits Softland-native containers, anchors, edges, and
audit rows.
```

## Product Statement

Softland should be able to ingest existing chat/transcript material and make it
native enough to inspect, reference, compose, search, quote, continue later, and
inhabit in future views without losing provenance.

The user-facing result is not "a log file was indexed." The result is:

```text
I can bring my Claude Code / Codex / chat artifacts into Softland.
Softland preserves what came from the source.
Softland gives the conversation durable native identities.
I can view the conversation as a conversation.
I can point at a message, tool call, result, artifact, or source line.
I can later use those objects in spaces, LLM runs, trails, code provenance, or
other workflows without re-parsing the original log privately in my head.
```

## Corrected Boundary

```text
external transcript source
  -> source acquisition
       one-time import OR passive watch
  -> transcript interpreter
       Claude Code / Codex / future chat parser
  -> object-container ingest
       SourceArtifact
       ObjectContainer
       Revision
       SourceAnchor
       CompositionEdge
       ingest/audit ledger
  -> projections and later workflows
       conversation view
       source/audit view
       future Space mount
       future LLM continuation
       future code/conversation provenance
```

Not this:

```text
transcript ingest -> Space
```

And not this:

```text
Space -> object-container ingest
```

Space may later consume, mount, continue, or transform the resulting containers.
But Space is not the source interpreter and not the canonical ingest substrate.

## Three Roles

### Source Acquisition

Source acquisition answers:

```text
where did the transcript-shaped material come from?
```

Modes:

```text
one-time import
  read existing files/artifacts and finish

passive watch
  keep observing append-only or changing transcript sources
```

Both modes feed the same interpreter. The product must not fork the meaning of
"imported transcript" and "watched transcript." They differ by acquisition
mode, not by native representation.

### Transcript Interpreter

The transcript interpreter answers:

```text
what does this source format mean?
```

Examples:

```text
Claude Code JSONL
Codex session JSONL
future exported chat artifact
future Softland-native chat artifact
```

The interpreter is source-specific. It knows how to extract conversation id,
message id, role, timestamp, tool use, tool result, result rows, attachments,
parse errors, and source-version hints.

### Object-Container Ingest

Object-container ingest answers:

```text
what native Softland objects now exist?
```

It writes the model base:

```text
SourceArtifact       preserved source material / source record
ObjectContainer      durable identity-bearing conversation/message/tool/artifact objects
Revision             content/state for containers
SourceAnchor         links native objects back to source file/line/span/item
CompositionEdge      conversation/message/tool/result ordering and containment
ActionDecision       accepted/rejected ingest decisions
KernelEvent          accepted world facts, when applicable
```

## Product Ontology

Transcript sources carry more native identity than markdown.

Markdown headings and paragraphs usually have no source-native ids, so the first
markdown slice derives `DerivedUnit`s until touched. Transcript logs often do
carry source-native ids: session ids, message uuids, tool call ids, item ids, or
stable physical line identities. Where those exist, transcript ingest should
seed child containers immediately.

Minimum useful containers:

```text
ConversationContainer
  kind: :chat-conversation
  seeded from source conversation/session id when available
  fallback identity from source file or deterministic source-record grouping

MessageContainer
  kind: :chat-message
  seeded from source message uuid when available
  fallback identity from source line key

ToolCallContainer
  kind: :tool-call
  seeded from tool_use id / call id when available

ToolResultContainer
  kind: :tool-result
  seeded from tool result id / related call id when available

RunOrSessionContainer
  kind: :agent-run or :chat-session
  seeded when the transcript source distinguishes a run/session from a
  conversation

ArtifactContainer
  kind: :chat-artifact or source-specific artifact kind
  seeded when the transcript records a produced artifact, patch, file output,
  attachment, or other addressable result
```

Minimum useful edges:

```text
conversation contains message
message follows previous message
message replies-to parent message, when present
assistant message produced tool call
tool call produced tool result
message produced artifact
artifact derived-from source message/tool result
container anchored-to source artifact / source record
```

Only source-provided structure should become accepted composition at ingest.
Inferred semantic relations are not accepted truth at ingest time.

## Raw Source And Privacy

The product requirement is:

```text
keep the source material
and
store a Softland-native representation
```

For transcript data, "keep the source material" is policy-bearing because
transcripts can contain secrets, private context, credentials, pasted file
contents, and tool outputs.

The product invariant is:

```text
Do not silently sync unredacted transcript source into a shared or remote world.
```

Allowed raw-source modes:

```text
local-private raw mode
  exact raw transcript material may be kept as SourceArtifact if the store is
  local/private and the user has chosen that policy

redacted captured-source mode
  SourceArtifact stores the redacted captured source plus hashes, byte offsets,
  source refs, and anchors back to the external original

hash/anchor-only mode
  SourceArtifact stores enough source identity to correlate with the external
  file, but not the sensitive payload itself
```

This does not weaken the object-container rule. It makes transcript
SourceArtifact obey transcript privacy policy instead of pretending chat logs
are the same risk class as markdown notes.

The existing transcript-capture invariant remains valuable:

```text
parse failures do not persist raw unredacted bytes
redaction happens before shared persistence
the ledger records what was read and what policy applied
```

## Existing Code To Preserve

Preservation means preserving source-format behavior and test coverage. It does
not require the final implementation to depend directly on the old namespaces if
that would drag the old bespoke kernel/PStates into the new object-container
shape.

The existing transcript module is not the final ontology, but it contains
source-specific knowledge that must be preserved.

Do not discard or rewrite from scratch without carrying forward:

```text
src/app/server/rama/dogfood/transcript.clj

default-source-paths
  :claude-code -> ~/.claude/projects/**/*.jsonl
  :codex       -> ~/.codex/sessions/**/*.jsonl

walk-jsonl-files
  path expansion, recursive JSONL discovery, deterministic ordering

file-id
  inode/device where available, canonical path fallback

source-line-key
  source + file id + byte offset + line hash

read-jsonl-observations
  RandomAccessFile byte offsets, byte lengths, UTF-8 conversion

read-complete-appended-lines
  partial trailing line handling for watch mode

transcript-conversation-id
  permissive extraction across session_id, conversation_id, thread_id, uuid,
  parentUuid, cwdSessionId, and fallback

transcript-message-uuid
  permissive extraction across message_uuid, uuid, id, message id, message uuid

transcript-event-type
  event/type/role extraction

parsed-tool-use-blocks and tool-call-index-rows
  tool_use/tool-use preservation and index shape

transcript-observation
  parse, redact, source metadata, timestamps, parse-error shape

harvest-transcripts!
  one-time import behavior

start-transcript-watch!
  passive watch behavior and offset resume from ledger
```

Also preserve the behavioral tests in:

```text
test/app/server/rama/dogfood_transcript_test.clj
```

They prove idempotent harvest, redaction, parser boundary behavior, watch offset
behavior, and complete-line handling. The new object-container implementation
may replace the old PState shape, but it must not lose these tested source
behaviors.

Also preserve the live Claude stream parser behavior separately:

```text
src/app/server_jetty.clj

parse-stream-json-lines
  parses streaming Claude NDJSON events into canonical stream kinds:
  :run-start
  :text-delta
  :tool-use-start
  :tool-input-delta
  :block-stop
  :tool-result
  :run-done
  :run-error

  handles malformed JSON lines, missing terminal events, and unknown tool
  references
```

Relevant tests and fixtures:

```text
test/app/server/parser_test.clj
test/fixtures/claude-stream-sample.jsonl
```

The live-stream parser is not the same acquisition mode as on-disk transcript
harvest/watch. It should be preserved as source-format knowledge for a later
sub-slice, closer to live executor output.

Adjacent but not parser input:

```text
src/app/server/review_pack.clj
test/app/server/review_pack_test.clj
test/design_converter_e2e_test.clj
```

These are related to chat/review artifacts and may become downstream consumers
or test references. They are not transcript source parsers.

Ignore for this build:

```text
codex_implementation
```

That untracked folder is unrelated to transcript/chat ingest.

## Product Flows

### One-Time Import

```text
User chooses source family and paths
  -> Softland reads matching transcript artifacts
  -> interpreter parses/redacts/version-tags rows
  -> object-container ingest creates source + native containers
  -> user can open conversation projection
  -> audit view explains what was imported
```

Required product behavior:

```text
re-running import is safe
duplicates do not create duplicate messages/tool calls
parse errors are visible as source/audit facts
unparseable rows do not kill the whole import
source anchors let the user trace native objects back to file/line/offset/hash
```

### Passive Watch

```text
User enables watch for source family and paths
  -> Softland starts observing new transcript lines/files
  -> same interpreter emits same native container shape
  -> user can see newly observed messages appear through projections
  -> audit view records liveness, offsets, redaction, and failures
```

Required product behavior:

```text
watch does not spawn Claude/Codex
watch does not modify source files
watch does not trigger downstream actions
watch and import produce the same native object model
watch resumes from durable file/offset state where possible
```

## Views This Enables

Transcript ingest should enable views without making any view canonical.

Useful first projections:

```text
conversation projection
  grouped messages in source order, with roles, timestamps, tool calls, tool
  results, artifacts, parse-error rows, and anchors

source/audit projection
  what files/artifacts were read, when, by which request, with what redaction
  policy, with what parse failures

tool-call projection
  tool calls and results grouped by call id/name/source conversation

object projection
  a single message/tool/artifact container with source anchors and revision
  history
```

Projection is read-only. Gestures in a projection emit ActionRequests against
the canonical object-container model.

## Non-Goals

This product doc does not design:

```text
Space refactor
LLM continuation from imported transcript
automatic activation/follow-up behavior
semantic claim extraction
code-to-conversation provenance correlation
multi-user sync
global search
canvas placement
full chat UI
```

Those can consume transcript containers later. They are not transcript ingest.

## Acceptance Criteria

The transcript ingest product is working when:

```text
1. A Claude Code or Codex transcript source can be imported once.
2. The same source can be watched passively.
3. Import and watch produce the same native container types and edge types.
4. Raw/captured source material is preserved according to the selected privacy
   policy.
5. Conversation/message/tool-call/tool-result identity is stable across reruns.
6. Duplicate source records do not create duplicate native objects.
7. Source anchors resolve from native objects back to source file/line/offset/hash
   or source item id.
8. Parse errors are represented and auditable, not dropped.
9. Redaction policy is visible in the audit surface.
10. Existing Claude/Codex JSONL parsing behavior from the transcript module is
    preserved or deliberately replaced with equal-or-better tests.
11. No dependency on Space is required to ingest transcripts.
12. Space can later consume/mount the resulting containers without re-parsing
    the original transcript source.
```

## Implementation Direction

The later Rama implementation should not begin by refactoring Space.

The implementation should begin with the transcript-specific object-container
ingester:

```text
source acquisition request
  -> transcript interpreter
  -> object-container SourceArtifact/ObjectContainer/Revision/SourceAnchor/
     CompositionEdge rows
  -> conversation/source/tool projections
```

The old transcript-capture doc remains valuable, but its product role changes:

```text
old transcript-capture
  capture-only dogfood runtime slice
  preserve as source-acquisition/parser/audit knowledge

new transcript-ingest product contract
  transcript is an object-container ingester
  import/watch are acquisition modes
  Space is downstream, not the ingest boundary
```

The implementation phase should explicitly decide source granularity:

```text
whole-file SourceArtifact with line SourceAnchors
per-line SourceArtifact/source-record rows
or a two-level source model that keeps both file snapshot and line records
```

Whatever the chosen storage shape, the product invariant is stable:

```text
the source is preserved
the native containers are real Softland objects
the bridge between them is explicit and queryable
```

## Rama Implementation Entry

This file is the single driver for the Rama phased implementation.

Start with Phase 0. Do not code directly from this product doc.

Phase 0 should produce:

```text
docs/current-mental-model/build/chat-ingester/IMPLICIT_SPEC.md
```

Phase 0 inputs:

```text
docs/current-mental-model/build/chat-ingester/PRODUCT.md
docs/current-mental-model/architecture/object-container-spec.md
docs/current-mental-model/architecture/object-container-reviewer-world-model.md
docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md
docs/current-mental-model/build/object-container/IMPLICIT_SPEC.md
docs/current-mental-model/build/object-container/PLAN.md
src/app/server/rama/dogfood/transcript.clj
test/app/server/rama/dogfood_transcript_test.clj
src/app/server_jetty.clj
test/app/server/parser_test.clj
test/fixtures/claude-stream-sample.jsonl
```

The Phase 0 agent should derive the implicit implementation contract:

```text
operations
latency expectations
throughput expectations
consistency invariants
entity state x write matrix
read behavior in every state
edge cases
concurrency behavior
data growth and scale
```

Do not produce Rama code until the later phases derive and validate:

```text
IMPLICIT_SPEC.md
PLAN.md
PLAN_VALIDATION.md
```

Implementation guardrails:

```text
do not create a parallel chat/transcript kernel
do not refactor Space as part of transcript ingest
do not make transcript ingest depend on Space
do not treat chat as markdown-shaped DerivedUnits by default
do not rewrite Claude/Codex parser knowledge from scratch
do preserve or extract existing parser/acquisition/watch behavior and tests
do make PRODUCT.md the source-of-truth for what this build means
```
