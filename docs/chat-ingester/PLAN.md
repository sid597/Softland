# Plan — Transcript Ingest (Object-Container Ingester)

<!--
Phase 1 Rama artifact.
Inputs:
- docs/current-mental-model/build/chat-ingester/PRODUCT.md
- docs/current-mental-model/build/chat-ingester/IMPLICIT_SPEC.md
- docs/current-mental-model/architecture/object-container-spec.md
- src/app/server/rama/dogfood/transcript.clj (parser/acquisition knowledge)

This phase is design only. Do not write topology, ETL, query topology, or test
implementation code here.
-->

## Slice Boundary

This plan implements the transcript ingest pipeline:

```text
on-disk JSONL transcript files (Claude Code / Codex)
  -> source acquisition (harvest request or watch request)
  -> executor walks files, reads lines, parses, redacts
  -> depot append per source record (observation)
  -> stream topology creates native object containers
  -> conversation/audit/tool-call projections as reads
```

Out of scope: Space dependency, LLM continuation, live-stream ingest, semantic extraction, drift
detection, strict redaction, canvas/layers, multi-user sync.

Phase 3 should create:

```text
src/app/server/rama/dogfood/transcript_ingest.clj
```

This extends (not replaces) the existing `transcript.clj`. It reuses the parser/acquisition functions
and adds object-container materialization.

## ID, Routing, and Ordering Contract

All container IDs are deterministic from source-provided identifiers. No topology-generated random IDs.

### Stable Keys

```text
conv-key             = sha256(source-family + ":" + conversation-id)
conversation/id      = "tc:conv:" + conv-key
message/id           = "tc:msg:" + conv-key + ":" + sha256(message-uuid-or-fallback)
tool-call/id         = "tc:tc:" + conv-key + ":" + sha256(tool-use-id)
tool-result/id       = "tc:tr:" + conv-key + ":" + sha256(tool-use-id)
artifact/id          = "tc:art:" + conv-key + ":" + sha256(artifact-id-or-fallback)
source-line-key      = source-family + ":" + file-id-str + ":" + byte-offset + ":" + line-hash
                       (preserved from existing transcript.clj source-line-key)
message-order-key    = zero-padded-byte-offset (e.g. "0000012345")
edge-key             = edge-type + ":" + child-id
audit-entry-key      = source-line-key (one audit entry per source record)
```

`message-uuid-or-fallback`: uses transcript-message-uuid extraction (existing permissive function);
falls back to sha256(file-id + ":" + byte-offset) when no UUID is available.

### Depot Partition Keys

```text
*transcript-ingest-depot:   (hash-by :transcript/request-id)
*transcript-claim-depot:    (hash-by :transcript/request-id)
*transcript-obs-depot:      (hash-by :transcript/conv-key)
```

Observations partition by conv-key so all records for a conversation are processed on the same task.
This colocates container creation, dedup, and composition-edge writes. Secondary indexes (tool-call-
by-name, audit) repartition.

### Custom PState Partitioner

```text
partition-by-conv-key(num-partitions, id-or-key)
```

Extracts conv-key from `tc:conv:`, `tc:msg:`, `tc:tc:`, `tc:tr:`, `tc:art:` prefixed IDs by taking
the substring after the second `:` up to the next `:` (the conv-key is always 64 hex chars). Hashes
that conv-key to determine partition. PStates using this partitioner colocate all conversation-local
data on the same task.

## Reads

### R1. `read-conversation-projection`

Full conversation view: metadata + ordered messages with inline tool calls/results.

```clojure
(foreign-select [(keypath conversation-id) MAP-VALS] $$conversation-projection)
```

Access method: single `foreign-select` range scan on one PState, one partition.

Partition: `partition-by-conv-key(conversation-id)`.

Cost for N messages: 1 seek + (N+1) sequential iterations (includes 1 meta entry + N message entries).
Example: 200-message conversation ≈ 0.5ms + 200 * 5μs = 1.5ms before serialization.

Paginated variant:

```clojure
(foreign-select [(keypath conversation-id) (sorted-map-range-from cursor limit)] $$conversation-projection)
```

### R2. `read-source-audit-projection`

Audit entries for a harvest/watch request.

```clojure
(foreign-select [(keypath request-id) MAP-VALS] $$audit-entries)
```

Access method: single `foreign-select` range scan on one PState.

Partition: default hash of request-id.

Cost for N records ingested: 1 seek + N iterations. For a 10K-record harvest: ~50ms.

### R3. `read-tool-calls-by-name`

Tool call index entries for a given tool name.

```clojure
(foreign-select [(keypath tool-name) MAP-VALS] $$tool-calls-by-name)
;; or paginated:
(foreign-select [(keypath tool-name) (sorted-map-range-from cursor limit)] $$tool-calls-by-name)
```

Access method: single `foreign-select` range scan on one PState.

Partition: default hash of tool-name.

Cost: 1 seek + N iterations for N tool-call entries of that name.

### R4. `read-container`

Single container lookup by ID.

```clojure
(foreign-select-one [(keypath container-id)] $$containers-by-id)
```

Access method: single `foreign-select-one`, one PState, one partition.

Partition: `partition-by-conv-key(container-id)`.

Cost: 1 seek. The row contains kind, metadata, current revision content, and source-anchor inline.

### R5. `read-run-status`

Harvest/watch request status and progress.

```clojure
(foreign-select-one [(keypath request-id)] $$ingest-runs)
```

Access method: single `foreign-select-one`, one PState.

Partition: default hash of request-id.

Cost: 1 seek.

### R6. `read-source-artifact`

File-level source metadata by conversation container ID.

```clojure
(foreign-select-one [(keypath conversation-id)] $$source-artifacts)
```

Access method: single `foreign-select-one`, one PState.

Partition: `partition-by-conv-key(conversation-id)`.

Cost: 1 seek.

### No Query Topologies Needed

All reads are answered by a single `foreign-select` or `foreign-select-one` against one PState on one
partition. No cross-partition joins required in this slice. The denormalized `$$conversation-projection`
inlines message content, tool calls, tool results, and anchors — avoiding N+1 reads.

## Writes

### W1. `transcript/harvest` and W2. `transcript/watch` — request submission

Depot: `*transcript-ingest-depot` (hash-by `:transcript/request-id`)

Event payload (same shape as existing `transcript-request`):

```text
:request/type :transcript/harvest | :transcript/watch
:transcript/request-id String
:transcript/source :claude-code | :codex
:transcript/paths [String]
:transcript/redaction-policy :standard
:transcript/triggered-by {:agent String}
:routing/key [:transcript/request request-id]
:request/time-ms Long
```

### W3. Executor lifecycle — claims and status

Depot: `*transcript-claim-depot` (hash-by `:transcript/request-id`)

Event payload (same shape as existing `transcript-run-status-record`):

```text
:transcript/request-id String
:claim/type :transcript/run-status
:status :running | :complete | :failed | :cancelled
:time-ms Long
:counts {:observed-line-count Long :parse-error-count Long} optional
:error {:message String} optional
:routing/key [:transcript/request request-id]
```

### W4. Source record observation — per JSONL line

Depot: `*transcript-obs-depot` (hash-by `:transcript/conv-key`)

Event payload (extends existing `transcript-observation` shape):

```text
:transcript/conv-key String (sha256 of source + conversation-id)
:transcript/source :claude-code | :codex
:transcript/source-version String | :unknown
:transcript/conversation-id String
:transcript/message-uuid String | nil
:transcript/event-type Keyword
:transcript/source-timestamp String | nil
:transcript/ingest-timestamp String
:transcript/redacted-payload Map | nil
:transcript/redacted-preview String | nil (for parse errors)
:transcript/redactions [Map]
:transcript/parse-error-kind Keyword | nil
:transcript/ingest-request-id String
:transcript/host-id String | nil
:source/file-id Map
:source/file-path String
:source/byte-offset Long
:source/line-hash String
:source/byte-length Long
:routing/key [:transcript/request request-id]
```

The executor computes `conv-key` before appending (from transcript-conversation-id extraction).

### W5. File-state observation — per-file offset tracking and empty-file sentinel

Depot: `*transcript-file-state-depot` (hash-by `:source/file-key`)

Event payload:

```text
:source/file-key String (source-file-key from existing transcript.clj)
:source/file-id Map
:source/file-path String
:transcript/source Keyword
:transcript/conv-key String
:transcript/conversation-id String
:transcript/ingest-request-id String
:source/last-byte-offset Long
:source/line-count Long
:source/is-empty Boolean (true for zero-line files)
:time-ms Long
```

The executor appends one file-state observation per file:
- After completing all lines of a file (harvest): records final byte offset and line count.
- Periodically during watch: records current byte offset for resume.
- For empty files: records `is-empty = true`, `line-count = 0`. This triggers SourceArtifact
  creation even when no per-line observations exist.

This depot is partitioned by `source-file-key` so offset reads are direct point lookups.

## PState Design

### $$conversation-projection — denormalized conversation view

Option A: Normalize — store messages in `$$containers-by-id`, read conversation by scanning composition
edges, then read each message container.

Cost for 200 messages: 1 edge-range seek + 200 iterations + 200 container point seeks = 0.5ms + 1ms +
100ms ≈ 101ms. Unacceptable for interactive use.

Option B: Denormalize — maintain a sorted projection map per conversation. Each entry contains the
rendered message content inline (role, text, tool calls with content, tool results, anchors).

Cost for 200 messages: 1 seek + 201 iterations ≈ 1.5ms.

**Chosen: Option B.** Same pattern as object-container module's `$$outline-by-document`. Write
amplification is 1 entry per message at ingest time (no later updates for transcript containers).

### $$containers-by-id — canonical container identity

Simple key-value. Not denormalized. Used for R4 single-container reads and as the canonical truth.

Why not merge with $$conversation-projection: containers need to be addressable by arbitrary ID (not
just by conversation + order). This PState serves point-lookup by container-id for any kind (conversation,
message, tool-call, tool-result, artifact).

### Cost Summary

| Read | PState | Seeks | Iterations | Estimated latency |
|---|---|---|---|---|
| R1 (200 msgs) | $$conversation-projection | 1 | 201 | ~1.5ms |
| R2 (10K audit) | $$audit-entries | 1 | 10,001 | ~50ms |
| R3 (100 calls) | $$tool-calls-by-name | 1 | 100 | ~1ms |
| R4 | $$containers-by-id | 1 | 0 | ~0.5ms |
| R5 | $$ingest-runs | 1 | 0 | ~0.5ms |
| R6 | $$source-artifacts | 1 | 0 | ~0.5ms |

## Depots

```text
*transcript-ingest-depot:
  declaration: (declare-depot setup *transcript-ingest-depot (hash-by :transcript/request-id))
  owner: transcript-ingest module
  event types: :transcript/harvest, :transcript/watch
  client ack: :ack (caller needs to see run status immediately)

*transcript-claim-depot:
  declaration: (declare-depot setup *transcript-claim-depot (hash-by :transcript/request-id))
  owner: transcript-ingest module
  event types: :transcript/run-status (:running, :complete, :failed, :cancelled)
  client ack: :append-ack (executor doesn't block on materialization)

*transcript-obs-depot:
  declaration: (declare-depot setup *transcript-obs-depot (hash-by :transcript/conv-key))
  owner: transcript-ingest module
  event types: source-record observations (one per JSONL line)
  client ack: :append-ack (executor streams observations without blocking)

*transcript-file-state-depot:
  declaration: (declare-depot setup *transcript-file-state-depot (hash-by :source/file-key))
  owner: transcript-ingest module
  event types: per-file offset/state records (one per file per harvest, periodic during watch)
  client ack: :append-ack
```

Same depot rationale: request + claims share request-id ordering (status transitions must be ordered).
Observations are a separate stream — independent of request lifecycle, partitioned differently
(by conversation not request).

## Topologies and PStates

### `transcript-ingest-topology`: stream

Reason: stream topology is required because:
1. Request ingress uses `:ack` — caller must see `$$ingest-runs` updated immediately after
   `foreign-append!` to `*transcript-ingest-depot`.
2. Watch mode needs low-latency visibility — new containers should appear within seconds.
3. All PState writes are naturally idempotent (`keypath + termval` complete-row overwrites).

Sources:

```text
(source> *transcript-ingest-depot :> *request)
(source> *transcript-claim-depot :> *claim)
(source> *transcript-obs-depot {:retry-mode :all-after} :> *obs)
```

`:all-after` on the observation depot preserves per-conversation ordering across retries.

Stream retry safety: every write is `keypath + termval` with deterministic keys and complete row
values. Duplicate processing overwrites the same keys with the same values. No counters, no
AFTER-ELEM appends, no random IDs. Non-idempotent writes: **none** (approximate progress counts in
`$$ingest-runs` use `termval` overwrite of the full row — a retry may double-count, but counts are
informational not correctness-critical; exact accounting comes from `$$audit-entries`).

Cooperative multitasking: the observation source may process many records for one conversation in a
burst (harvest). The inner loop of container creation per observation is bounded (1 message + 0-5
tool calls), so no explicit yield is needed per event. But the source itself may batch many events
per streaming batch — Rama handles batch boundaries automatically.

PStates owned by this topology:

```text
$$ingest-runs:
  {String IngestRunRow}
  key = request-id
  no custom partitioner (hash by request-id = depot partition)

$$source-ledger:
  {String Boolean}
  key = source-line-key
  {:private? true}
  no custom partitioner (written and read only from conversation partition)

$$files-handled:
  {String Boolean}
  key = source-file-key
  {:private? true}
  no custom partitioner (written and read only from conversation partition)

$$file-offsets:
  {String FileOffsetRow}
  key = source-file-key
  no custom partitioner (hash by source-file-key)
  Used by watch executor to resume from last byte offset after restart.
  Written by topology from file-state observations (see W5 below).

$$containers-by-id:
  {String TranscriptContainerRow}
  key = container-id (tc:conv:..., tc:msg:..., tc:tc:..., tc:tr:..., tc:art:...)
  {:key-partitioner partition-by-conv-key}

$$conversation-projection:
  {String (map-schema String IConversationEntry {:subindex? true})}
  outer key = conversation-container-id
  inner key = order-key (zero-padded byte offset; "" for meta entry)
  {:key-partitioner partition-by-conv-key}

$$composition-edges-by-parent:
  {String (map-schema String CompositionEdgeRow {:subindex? true})}
  outer key = parent-container-id
  inner key = edge-key (edge-type + ":" + child-id)
  {:key-partitioner partition-by-conv-key}

$$source-anchors-by-container:
  {String SourceAnchorRow}
  key = container-id
  {:key-partitioner partition-by-conv-key}

$$source-artifacts:
  {String SourceArtifactRow}
  key = conversation-container-id (one file = one conversation)
  {:key-partitioner partition-by-conv-key}

$$tool-calls-by-name:
  {String (map-schema String ToolCallIndexRow {:subindex? true})}
  outer key = tool-name
  inner key = source-line-key (deterministic, globally unique)
  no custom partitioner (hash by tool-name)

$$audit-entries:
  {String (map-schema String AuditEntryRow {:subindex? true})}
  outer key = request-id
  inner key = source-line-key
  no custom partitioner (hash by request-id)
```

### Materialization Branches

**Request source (`*transcript-ingest-depot`):**

```text
1. validate request (request-validation-errors from existing transcript.clj)
2. if valid: write $$ingest-runs[request-id] = initial IngestRunRow (status :pending)
3. if invalid: write $$ingest-runs[request-id] = rejected IngestRunRow (status :failed, errors)
```

No repartition needed — depot partition = PState key = request-id. Zero hops.

**Claim source (`*transcript-claim-depot`):**

```text
1. read $$ingest-runs[request-id]
2. if exists: fold-run-status (existing function), write updated row
```

Zero hops — same partition as request.

**Observation source (`*transcript-obs-depot`):**

```text
1. Already on conversation partition (depot hash-by conv-key)
2. Compute source-line-key
3. Read $$source-ledger[source-line-key] (local)
4. If already exists: stop (idempotent dedup)
5. If new:
   a. Write $$source-ledger[source-line-key] = true (local)
   b. Compute container ID(s), revision content, edges, anchors from observation
   c. Write $$containers-by-id[container-id] for each container (local via partition-by-conv-key)
   d. Write $$conversation-projection[conversation-id][order-key] = entry (local)
   e. Write $$composition-edges-by-parent[parent-id][edge-key] (local)
   f. Write $$source-anchors-by-container[container-id] (local)
   g. Write $$source-artifacts[conversation-id] = file metadata (local, idempotent per-file)
   h. |hash request-id → write $$audit-entries[request-id][source-line-key] (1 hop)
   i. Read+update $$ingest-runs[request-id] progress (on request-id partition)
   j. If tool calls exist: explode → |hash tool-name → write $$tool-calls-by-name[name][key] (terminal)
```

Hops per new observation: 1 (to request-id) + 0-N (to tool-name, conditional terminal).
Typical: 1-4 hops. All hops write idempotent termval rows.

**File-state source (`*transcript-file-state-depot`):**

```text
1. Already on file-key partition (depot hash-by source/file-key)
2. Write $$file-offsets[source-file-key] = FileOffsetRow (termval overwrite with latest offset)
3. If is-empty = true:
   a. |hash conv-key → write $$source-artifacts[conversation-id] = SourceArtifactRow (for empty files)
   b. |hash request-id → write $$audit-entries[request-id][sentinel-key] = empty-file audit entry
```

This handles the empty-file case: the executor sends a file-state observation for every file
(including zero-line files), and the topology creates the SourceArtifact from the file-state event.

### Typed Rows

```text
IngestRunRow:
  request-id String
  request-type Keyword
  status Keyword (:pending :running :complete :failed :cancelled)
  source Keyword
  paths [String]
  redaction-policy Keyword
  triggered-by Map
  created-at-ms Long
  updated-at-ms Long
  observed-line-count Long
  parse-error-count Long
  containers-created-count Long
  error Map optional

TranscriptContainerRow:
  container-id String
  container-kind Keyword (:chat-conversation :chat-message :tool-call :tool-result :chat-artifact)
  conv-key String
  conversation-id String
  source Keyword
  visibility Keyword (:private)
  revision-content String optional (redacted text content for messages)
  revision-metadata Map (role, timestamp, tool-name, etc.)
  source-anchor SourceAnchorRow
  created-at-ms Long
  created-by-request-id String

IConversationEntry (interface):
  implemented by ConversationMetaEntry, ConversationMessageEntry

ConversationMetaEntry:
  entry-type :meta
  conversation-id String
  conv-key String
  source Keyword
  source-version String
  session-metadata Map (model, tools, plugins, etc.)
  message-count Long
  first-timestamp String optional
  last-timestamp String optional
  source-file-path String
  source-file-id Map
  created-at-ms Long

ConversationMessageEntry:
  entry-type :message
  container-id String
  order-key String
  role Keyword (:user :assistant :system)
  content-text String (redacted)
  content-hash String
  timestamp String optional
  source-anchor-summary Map (file-path, byte-offset, line-hash)
  tool-calls [ToolCallSummary] (inline, 0-N)
  tool-results [ToolResultSummary] (inline, 0-N)
  event-type Keyword
  parse-error-kind Keyword optional
  redacted-preview String optional (for parse errors)

ToolCallSummary:
  container-id String
  tool-use-id String
  tool-name String
  redacted-input String

ToolResultSummary:
  container-id String
  tool-use-id String
  redacted-content String

CompositionEdgeRow:
  edge-key String
  edge-type Keyword (:contains :follows :produced :anchored-to)
  parent-id String
  child-id String
  child-kind Keyword
  order-key String optional
  created-at-ms Long

SourceAnchorRow:
  container-id String
  source Keyword
  file-id Map
  file-path String
  byte-offset Long
  byte-length Long
  line-hash String

SourceArtifactRow:
  conversation-id String
  conv-key String
  source Keyword
  file-id Map
  file-path String
  source-version String
  first-ingested-at-ms Long
  last-ingested-at-ms Long
  last-ingested-by-request String

FileOffsetRow:
  source-file-key String
  file-id Map
  file-path String
  source Keyword
  conv-key String
  conversation-id String
  last-byte-offset Long
  line-count Long
  last-request-id String
  updated-at-ms Long

ToolCallIndexRow:
  tool-name String
  tool-use-id String
  container-id String
  conversation-id String
  message-uuid String optional
  source-line-key String
  timestamp String optional
  redacted-input-preview String (first 200 chars)

AuditEntryRow:
  source-line-key String
  event-type Keyword
  parse-error-kind Keyword optional
  redactions-count Long
  containers-created Long
  container-ids [String]
  file-path String
  byte-offset Long
  timestamp String optional
  processed-at-ms Long
```

## Query Topologies

None required. All reads use single `foreign-select` or `foreign-select-one` calls. The denormalized
`$$conversation-projection` PState eliminates N+1 reads for the hot conversation-view path.

If a future slice needs cross-conversation search or multi-PState joins, add query topologies then.

## Design Decisions

### Subindexing

Subindexed:

```text
$$conversation-projection inner map:
  grows with messages per conversation (1-1000+). Range scan is the primary access pattern.

$$composition-edges-by-parent inner map:
  grows with children per parent (messages per conversation: 1-1000+).

$$tool-calls-by-name inner map:
  grows with usage of a tool (popular tools like "bash" can have 10K+ calls).

$$audit-entries inner map:
  grows with records per harvest (10K-100K+).
```

Not subindexed:

```text
$$ingest-runs: flat map, one entry per request (single digits). Point lookup only.
$$containers-by-id: flat map, one entry per container. Point lookup only.
$$source-anchors-by-container: flat map, one per container. Point lookup only.
$$source-artifacts: flat map, one per conversation. Point lookup only.
$$source-ledger: flat map, entries distributed by conversation across tasks. Point lookup only.
$$files-handled: flat map, one per file per task. Point lookup only.
```

### Colocation

The observation depot partitions by conv-key. The custom partitioner `partition-by-conv-key` extracts
the same conv-key from container IDs. This means:

```text
depot partition for observation with conv-key X
  = PState partition for any container ID embedding conv-key X
```

All container writes for a conversation are local (zero hops). The topology starts on the correct
partition for all primary writes.

### Denormalization Trade-off

`$$conversation-projection` duplicates data that's also in `$$containers-by-id`. This is intentional:
- The projection is the hot-path read (conversation view). It must be one range scan.
- Container writes are one-time at ingest (no later edits for transcript containers), so the
  write-amplification cost is paid exactly once per message.
- Total extra write per observation: 1 `termval` to the projection PState. Negligible.

### Source Artifact Scope

One file maps to one conversation (Claude Code sessions are per-file). So `$$source-artifacts` is
keyed by conversation-container-id (not file-key). This keeps source-artifact reads local (same
partition as the conversation) and avoids a separate hop for file metadata.

For the case where one file contains multiple conversations (not observed in Claude Code JSONL but
theoretically possible): the source artifact captures the PRIMARY conversation for that file. This is
a simplification acceptable for the first slice.

### Progress Counts

`$$ingest-runs` progress counts (observed-line-count, parse-error-count) are approximate under stream
retry. The topology reads the current run row, increments, and writes with `termval`. On retry, the
same increment applies twice. This is acceptable because:
1. Counts are informational (for progress display), not correctness-critical.
2. Exact per-record accounting is available in `$$audit-entries`.
3. Stream retries are rare in practice.

### No DerivedUnit Lifecycle

Unlike markdown ingest (which produces DerivedUnits that graduate on edit), transcript containers are
full ObjectContainers from ingest. Reason: transcript sources carry native IDs (session_id, message
UUIDs, tool_use IDs). The derived-until-touched pattern only applies to sources without native identity.

## State Primitive Selection

PStates:

```text
$$ingest-runs (PState):
  O(1) write per request + O(1) per claim event. Durable run lifecycle.

$$source-ledger (PState, private):
  O(1) write per new source record. Durable dedup. Per-event write = 1.

$$files-handled (PState, private):
  O(1) write per new file encountered. Small. Survives restart (prevents re-writing source artifacts).

$$containers-by-id (PState):
  O(1-5) writes per new source record (1 message + 0-N tool calls/results). Durable identity store.

$$conversation-projection (PState):
  O(1) write per new source record (one entry per message). Durable denormalized view.

$$composition-edges-by-parent (PState):
  O(2-5) writes per new source record (contains + follows + produced edges). Durable.

$$source-anchors-by-container (PState):
  O(1-5) writes per new source record (one per container created). Durable.

$$source-artifacts (PState):
  O(1) write per new file encountered. Durable file metadata.

$$tool-calls-by-name (PState):
  O(0-5) writes per new source record (one per tool_use block). Secondary index.

$$audit-entries (PState):
  O(1) write per new source record. Durable audit trail.

$$file-offsets (PState):
  O(1) write per file-state event. Durable watch resume state. Per-event write = 1.
```

TaskGlobals:

```text
None. All state is durable in PStates. The executor (harvest/watch) is external to the module —
it uses existing functions from transcript.clj (walk-jsonl-files, read-jsonl-observations, etc.)
and appends to depots via foreign-append!.
```

External systems:

```text
Filesystem (read-only): the executor reads JSONL files. It never modifies them.
No databases, queues, or external services.
```

## Phase 3 Implementation File Checklist

Phase 3 should create:

```text
src/app/server/rama/dogfood/transcript_ingest.clj
```

Phase 3 should expose helper functions:

```text
;; Request construction
transcript-ingest-request        (harvest/watch request builder)

;; Runtime lifecycle
start-transcript-ingest-runtime! (IPC + module launch)
close-transcript-ingest-runtime!

;; Depot appends
append-ingest-request!           (submit harvest/watch request)
append-ingest-claim!             (executor lifecycle events)
append-ingest-observation!       (per-record source observation)
append-file-state!               (per-file offset/state tracking)

;; Reads
read-ingest-run                  (R5 - run status)
read-conversation-projection     (R1 - full conversation)
read-container                   (R4 - single container)
read-tool-calls-by-name          (R3 - tool index)
read-source-artifact             (R6 - file metadata)
read-audit-entries               (R2 - audit trail)
read-file-offset                 (file byte-offset for watch resume)

;; Executor orchestration (reuses existing transcript.clj functions)
harvest-ingest!                  (full harvest pipeline: request → walk → parse → append)
start-watch-ingest!              (watch pipeline: request → poll/watch → parse → append)
```

Phase 5 should create:

```text
test/app/server/rama/dogfood/transcript_ingest_test.clj
```

Minimum invariant tests:

```text
1.  Harvest creates conversation + message + tool-call containers with correct kinds
2.  Re-harvest same files is idempotent (no duplicate containers)
3.  Source-record identity dedup works (same line-key → no new containers)
4.  Conversation projection returns messages in source order
5.  Tool-call index is populated and queryable by tool name
6.  Parse errors create audit entries but not containers
7.  Redacted content does not contain raw secrets
8.  Container IDs are deterministic (same input → same IDs)
9.  Composition edges correctly link conversation→message→tool-call→tool-result
10. Source anchors trace containers back to file/line/offset/hash
11. Watch + harvest overlap does not create duplicate containers
12. Empty file produces source artifact (via file-state sentinel) but zero containers
13. Run status transitions through pending → running → complete
14. Audit entries record per-record provenance
15. File-offset PState tracks last byte offset for watch resume
16. Watch resumes from durable file offset after executor restart
```
