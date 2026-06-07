# Implicit Spec — Transcript Ingest (Object-Container Ingester)

<!-- Phase 0. Requirements analysis only — NO PState/depot/topology design (that is Phase 1). -->

Source spec: `docs/current-mental-model/build/chat-ingester/PRODUCT.md`. This slice realizes the transcript
ingest pipeline: source acquisition (harvest/watch) → transcript interpreter (Claude Code / Codex JSONL
parser) → object-container ingest (SourceArtifact, ObjectContainer, Revision, SourceAnchor,
CompositionEdge, audit ledger) for **on-disk JSONL transcript files**.

Architecture substrate: `docs/current-mental-model/architecture/object-container-spec.md`. Transcript
ingest is a domain-specific object-container ingester that differs from the markdown ingester (object
container Slice 1) in that transcript sources carry **native IDs** — session IDs, message UUIDs, tool-use
IDs — so child containers are seeded immediately as full ObjectContainers, not DerivedUnits.

Existing code to preserve: `src/app/server/rama/dogfood/transcript.clj` contains the parser, acquisition,
and observation pipeline. Its source-format knowledge (path discovery, file-id extraction, line-level
parsing, conversation-id/message-uuid extraction, tool-call indexing, redaction integration,
complete-line handling, offset tracking) must be preserved or extracted. The live-stream parser in
`src/app/server_jetty.clj` (`parse-stream-json-lines`) is a separate source-format interpreter for a
future sub-slice and is not part of this module's scope.

## Slice Scope (committed)

**In:** ingest on-disk Claude Code and Codex JSONL transcripts via one-time harvest or passive watch;
preserve raw source as SourceArtifact with redaction policy; create native Softland containers for
conversations, messages, tool calls, tool results, and artifacts; create composition edges for
containment, ordering, and production relationships; create source anchors from containers back to
file/line/offset/hash; produce an auditable ingest ledger; support conversation, source/audit, tool-call,
and object projections as reads.

**Out (deferred, must not be designed in):** Space refactor or dependency; LLM continuation from
imported transcript; automatic activation/follow-up behavior; semantic claim extraction (inferred
relations are NOT accepted truth at ingest); code-to-conversation provenance correlation (git-track);
multi-user sync; global search; canvas placement; full chat UI; live-stream ingest (server_jetty.clj
parser); strict-policy redaction (start with standard); multi-machine ingestion (host-id field exists
but single-host only).

**Entities:** SourceArtifact, ObjectContainer (6 kinds: `:chat-conversation`, `:chat-message`,
`:tool-call`, `:tool-result`, `:agent-run`, `:chat-artifact`), Revision, SourceAnchor, CompositionEdge,
IngestRun, AuditLedgerEntry.

**Not entities in this module:** DerivedUnit (transcript sources carry native IDs; no derived-until-touched
lifecycle here — all containers are durable from ingest).

## Operations

### W1. `transcript/harvest` — one-time bulk import of transcript files

- **Inputs:** source family (`:claude-code` or `:codex`), paths (glob patterns or directories, default
  from existing `default-source-paths`), redaction policy (`:standard` initially; `:off` and `:strict`
  deferred), optional time floor (`:since`), request ID, triggered-by actor.
- **Behavior:**
  1. Walk configured paths, discover JSONL files (preserving existing `walk-jsonl-files` behavior:
     `**` glob expansion, recursive directory scan, `.jsonl` filter, deterministic sort by path).
  2. For each file, compute file-id (inode/device where available, canonical path fallback — existing
     `file-id` behavior).
  3. Read each file line-by-line with byte offsets (existing `read-jsonl-observations` behavior via
     `RandomAccessFile`).
  4. For each line: parse JSON, redact, extract conversation/message/tool/timestamp metadata (existing
     `transcript-observation` pipeline), then create object containers.
  5. Record audit trail: per-request progress, per-file metadata, per-record provenance.
  6. Emit terminal status on completion or failure.
- **Latency:** seconds to minutes acceptable. Not a hot path — the user imports an archive.
- **Throughput:** driven by number of files × lines per file. A user with 6 months of Claude Code usage
  could have hundreds of session files with thousands of lines each. Total volume: tens of thousands to
  low hundreds of thousands of JSONL lines per harvest. Bursty (one request processes everything).
- **Consistency invariants:**
  - Re-running harvest over the same files is safe. Duplicate source records do not create duplicate
    containers (idempotent by source-record identity).
  - The harvest produces the same native container types as a watch would for the same content.
  - Parse errors do not kill the whole import. Each file is processed independently; each line within a
    file is processed independently. Failures are recorded, not propagated.
  - Raw source material is never persisted unredacted (unless redaction policy is `:off`).
  - Harvest does not spawn Claude/Codex, modify source files, or trigger downstream actions (observation-
    only invariant from `transcript-capture.md`).
- **Data growth & scale:** Write volume = O(total JSONL lines across all files). Each line produces at
  minimum: 1 source-record entry, 1 container (message/tool/etc), 1 revision, 1 source-anchor, 1+
  composition edges. A 10,000-line harvest creates ~10,000 containers with ~10,000 revisions, ~10,000
  source anchors, and ~15,000-20,000 composition edges (messages have 2+ edges: contains + follows).
- **Concurrency:** Two concurrent harvests over overlapping paths are safe. The source-record identity
  `(source, file-id, byte-offset, line-hash)` ensures deduplication. The second harvest may append
  duplicate records to the depot, but they fold to the same materialized state. No ordering dependency
  between harvests.
- **Edge cases:**
  - Empty directory → harvest completes with zero files, zero containers. Valid no-op.
  - Directory with no `.jsonl` files → same as empty.
  - File with zero lines → SourceArtifact created for the file (captures existence), zero containers.
  - File with only malformed JSON → SourceArtifact created, zero message containers, N parse-error
    audit entries.
  - Very large file (100K+ lines) → must not exhaust memory. Line-by-line streaming with periodic
    progress reporting. Cooperative yielding required.
  - File disappears mid-harvest → error recorded for that file, other files continue.
  - Permission denied on a file → error recorded, other files continue.
  - Re-harvest after source file changed on disk (same path, different content) → new SourceArtifact
    version (different hash); existing containers from old version untouched. Same path, same content →
    dedupe, no new artifacts.
  - Paths that expand to overlapping files → deduplicated by file-id (inode). Same file discovered via
    two glob patterns → one set of containers.

### W2. `transcript/watch` — passive ongoing observation

- **Inputs:** same as harvest plus poll interval or filesystem watcher config.
- **Behavior:**
  1. Register filesystem observation on configured paths.
  2. For existing files unknown to the ledger: start at EOF (don't ingest history — use harvest for
     that). For existing files with a ledger entry: resume from last recorded byte offset.
  3. For new files appearing after watch starts: start at byte 0.
  4. Tail new lines: same parse → redact → container pipeline as harvest.
  5. Handle partial trailing lines: wait for newline before ingesting (existing
     `read-complete-appended-lines` behavior).
  6. Run until cancelled or executor restarts.
- **Latency:** new lines should appear in containers within seconds of being written to disk (not
  real-time, but prompt — governed by poll interval or fs notification lag).
- **Throughput:** driven by Claude/Codex activity rate. During active usage: 1-10 lines per second.
  During idle: zero. Watch must handle both gracefully.
- **Consistency invariants:**
  - Watch and harvest produce the same native container types and edge types (PRODUCT.md acceptance
    criterion 3).
  - Watch does not spawn Claude/Codex, modify source files, or trigger downstream actions.
  - Watch resumes from durable file/offset state where possible (PRODUCT.md acceptance criterion).
  - Duplicate source records (from harvest + watch overlap) fold to the same materialized state.
- **Data growth & scale:** steady-state growth matches Claude/Codex usage rate. Not bursty like harvest.
  Offset state grows with number of watched files (bounded by user's session count).
- **Concurrency:** only one watch per source-family should be active at a time. Concurrent watch requests
  for the same source → the second should either be rejected or merge into the first. Two watches on
  different source families (`:claude-code` + `:codex`) are independent and safe.
- **Edge cases:**
  - File rotation mid-watch (rename + new file at old path) → track by inode, not path. Continue
    tailing renamed file; start new file at byte 0.
  - File deletion → stop tailing. Existing containers persist. Ledger records deletion event.
  - Watcher disconnection (inotify limit exhausted) → detect, re-establish, reconcile offsets.
  - Partial JSON line at EOF → do not ingest; wait for complete line.
  - Claude Code version change (schema drift) → parse with permissive fallback; record
    `:transcript/source-version` per record. Unknown event types recorded as `:unknown-shape`.
  - Watch started with `:backfill? true` → start existing files at byte 0, not EOF (existing
    `start-transcript-watch!` behavior).
  - No files exist at watched paths → watch runs but produces nothing until files appear.

### W3. Source record ingest — per-record container creation (internal)

This is not a user-facing operation but an internal step driven by both harvest and watch. For each
parsed/redacted JSONL line, the ingest pipeline creates the appropriate object containers.

- **Inputs per record:** parsed/redacted JSONL payload, source metadata (file-id, byte-offset, line-hash,
  byte-length, file-path), transcript metadata (source family, source version, conversation ID, message
  UUID, event type, source timestamp, redaction info, ingest request ID).
- **Behavior by event type:**

  **`system/init` or `system`:**
  - Create or update ConversationContainer (kind `:chat-conversation`).
  - Revision content: session metadata (model, session_id, tools, plugins, mcpClients if present).
  - SourceAnchor: file/line/offset of the init event.
  - If this is the first line of a conversation, the ConversationContainer is created here.

  **`assistant` message:**
  - Create MessageContainer (kind `:chat-message`).
  - Revision content: role, redacted message content, timestamp, model.
  - SourceAnchor: file/line/offset.
  - CompositionEdge: conversation contains message (containment).
  - CompositionEdge: message follows previous message (ordering, when previous exists).
  - For each `tool_use` block in `message.content`:
    - Create ToolCallContainer (kind `:tool-call`).
    - Revision content: tool name, redacted input, tool-use ID.
    - SourceAnchor: same source line as the parent message.
    - CompositionEdge: message produced tool-call (production).

  **`user` message:**
  - Same as assistant message but with role `:user`.
  - User messages typically don't contain tool_use blocks.

  **`tool_result` or content block with `type: "tool_result"`:**
  - Create ToolResultContainer (kind `:tool-result`).
  - Revision content: redacted tool result content, tool-use-id reference.
  - SourceAnchor: file/line/offset.
  - CompositionEdge: tool-call produced tool-result (production, correlated by tool-use-id).

  **`result` (terminal):**
  - Update ConversationContainer metadata (total cost, final status).
  - This may create a new Revision of the ConversationContainer (metadata update).

  **`stream_event`:**
  - In the on-disk JSONL format, stream events are per-delta updates. For the on-disk ingester, stream
    events within a conversation contribute to the conversation's message containers only when the
    consolidated message hasn't been separately recorded. In many Claude Code JSONL files, the
    consolidated `assistant` message appears alongside (or instead of) individual stream events.
  - Decision: if a consolidated message is present with the same UUID, stream events for that message
    are NOT separately containerized (they are redundant). If only stream events are present (no
    consolidated message), they are grouped into a MessageContainer by the interpreter.

  **`attachment`, `tool_use_summary`, other event types:**
  - Create ArtifactContainer (kind `:chat-artifact`) when the event represents a produced artifact,
    patch, file output, or attachment with an addressable identity.
  - For unknown or unhandled event types: record the SourceRecord and audit entry, but do NOT create a
    container. Unknown event types are preserved in the source but not interpreted into containers.

  **`parse-error`:**
  - No container created. A SourceRecord is created with the parse-error metadata (error kind, redacted
    preview, byte length, line hash). The audit ledger records the failure.
  - Parse-error rows never contain raw unredacted bytes. Redaction applies even to malformed lines.

- **Latency per record:** sub-millisecond for the container creation logic itself (deterministic ID
  computation, row assembly). The bottleneck is file I/O and depot append latency, not container logic.
- **Throughput:** must handle burst from harvest (thousands of records in sequence). The per-record logic
  is pure and stateless; throughput is bounded by depot append rate and topology processing rate.
- **Consistency invariants:**
  - Source-record identity is `(source, file-id, byte-offset, line-hash)`. Two appends with the same
    identity fold to one materialized record. This is the idempotency key for the entire ingest.
  - A given source record creates at most one set of containers. Re-processing the same record (via
    retry, re-harvest, or harvest+watch overlap) is idempotent.
  - Container IDs are deterministic from source-record identity + container role. No random IDs in the
    container creation path.
  - Containers created from source records are never DerivedUnits. Transcript sources carry native IDs;
    containers are full ObjectContainers from ingest.
  - The SourceArtifact is never mutated by container creation. Source material is immutable.
  - Only source-provided structure becomes accepted composition at ingest. No inferred semantic relations.
- **Edge cases:**
  - JSONL line with no conversation ID → fallback to file path as conversation identity (existing
    `transcript-conversation-id` behavior).
  - JSONL line with no message UUID → fallback to `(file-id, byte-offset)` as message identity.
  - JSONL line with unknown event type → SourceRecord created, no container. Audit records the unknown
    type.
  - Tool-use block with no `id` field → skip ToolCallContainer creation; record in audit as a
    malformed tool-use block.
  - Tool-result referencing an unknown tool-use ID → create ToolResultContainer but the
    tool-call-produced-tool-result edge cannot be created (the parent ToolCallContainer doesn't exist).
    Record as an orphaned tool result in audit.
  - Empty message content → valid MessageContainer with empty revision content. An empty message is
    still a message.
  - Message content with mixed blocks (text + tool_use + tool_use) → one MessageContainer, multiple
    ToolCallContainers.
  - Duplicate message UUID in different source files → same container (deterministic ID from UUID).
    The second occurrence is a no-op at the PState level.
  - Very long message content (>100KB) → valid. No content size limit at ingest. Redaction applies.

### R1. `read-conversation-projection` — conversation view

- **Inputs:** conversation container ID or conversation source ID (session_id).
- **Output:** the conversation container with its messages in source order, each message carrying: role,
  content (from current revision), timestamp, tool calls (with results), artifacts, parse-error rows,
  and source anchors.
- **Access pattern:** retrieve conversation container → range scan of its child messages ordered by
  source position → for each message, include child tool-calls and tool-results.
- **Latency:** single-digit to tens of ms for a typical conversation (50-200 messages). Must be efficient
  enough for interactive UI rendering.
- **Scale:** a Claude Code conversation can have 1-1000+ messages. Tool calls per message: 0-50+.
  The dominant cost is the range scan over messages and their children.
- **Edge cases:**
  - Conversation with zero messages (only system/init) → returns conversation metadata, empty message
    list.
  - Conversation with parse errors → parse-error source records included in the projection with
    appropriate markers, not silently dropped.
  - Message with orphaned tool results → tool results appear in the projection even if their parent
    tool-call is missing.

### R2. `read-source-audit-projection` — audit view

- **Inputs:** optional filter by ingest request ID, source family, time range, file path.
- **Output:** per-request: files seen, files parsed, lines processed, containers created, redactions
  applied, parse errors encountered. Per-file: file-id, path, byte count, line count, conversation IDs
  found. Per-record: source identity, redaction details, container(s) created.
- **Access pattern:** depends on filter. By request ID: point lookup + range scan of that request's audit
  entries. By time range: range scan.
- **Latency:** tens to hundreds of ms acceptable. Audit is not a hot path.
- **Edge cases:**
  - Request with no files found → audit shows zero files, request completed.
  - File with all parse errors → audit shows N errors, zero containers for that file.

### R3. `read-tool-call-projection` — tool-call view

- **Inputs:** tool name, conversation ID, or tool-call container ID.
- **Output:** tool calls matching the filter, each with: tool name, redacted input, tool result (if
  available), source conversation, source message, source anchor.
- **Access pattern:** by tool name → index lookup; by conversation → conversation children filtered by
  kind; by ID → point lookup.
- **Latency:** single-digit ms for point lookups; tens of ms for name/conversation scans.
- **Edge cases:**
  - Tool name that doesn't exist → empty result.
  - Tool call with no result → included in projection, result field nil.

### R4. `read-container` — single object view

- **Inputs:** container ID.
- **Output:** the ObjectContainer with: kind, current revision (content), source anchor(s), composition
  edges (parent, children), creation metadata.
- **Access pattern:** point lookup by container ID.
- **Latency:** sub-ms to single-digit ms.
- **Edge cases:**
  - Non-existent container ID → nil / not found.
  - Container with no source anchor (shouldn't happen for transcript containers, but defensive) →
    returns container without anchor.

### R5. `read-run-status` — harvest/watch request status

- **Inputs:** ingest request ID.
- **Output:** request status (`:pending`, `:running`, `:complete`, `:failed`, `:cancelled`), progress
  counters (files seen, lines processed, containers created, parse errors), timestamps.
- **Access pattern:** point lookup.
- **Latency:** sub-ms.
- **Edge cases:**
  - Non-existent request ID → nil.
  - Watch request → status is `:running` until cancelled. Progress updates continuously.

### R6. `read-source-artifact` — file-level source info

- **Inputs:** source artifact ID or `(source-family, file-id)`.
- **Output:** file-level metadata: file path, file-id (inode/device), content hash, byte count, line
  count, source family, ingested-by request IDs, last-known byte offset (for watch resume).
- **Access pattern:** point lookup.
- **Latency:** sub-ms.
- **Edge cases:**
  - File ingested by multiple requests (harvest + watch) → shows all request IDs.
  - File that was deleted after ingest → source artifact persists with its recorded metadata.

## Entity State × Write Matrix

### IngestRun — states: `does-not-exist`, `pending`, `running`, `complete`, `failed`, `cancelled`

**`does-not-exist` × W1 `transcript/harvest`:**
  - Becomes `pending`.
  - R5 read-run-status: returns `{:status :pending, :observed-line-count 0, ...}`.
  - R1 read-conversation-projection: no conversations exist yet from this request.
  - R2 read-source-audit-projection: shows the request was submitted, no processing yet.

**`does-not-exist` × W2 `transcript/watch`:**
  - Same as harvest → becomes `pending`.
  - All reads: same as harvest `pending`.

**`pending` × executor claims run:**
  - Becomes `running`.
  - R5: returns `{:status :running, :observed-line-count 0, ...}`.
  - R1, R2, R3, R4, R6: unchanged until source records are processed.

**`running` × W3 source record ingest (each line):**
  - Stays `running`; progress counters increment.
  - R5: `observed-line-count` increases, `parse-error-count` increases on errors.
  - R1: conversations may now be readable (new containers appear).
  - R2: audit entries accumulate.
  - R3: tool calls may now be readable.
  - R4: individual containers now readable.
  - R6: source artifacts for discovered files now readable.

**`running` × harvest completes:**
  - Becomes `complete`.
  - R5: returns `{:status :complete, ...}` with final counts.
  - All other reads: unchanged — containers and source records persist.

**`running` × harvest fails:**
  - Becomes `failed` (with error details).
  - R5: returns `{:status :failed, :error {...}, ...}`.
  - Containers created before failure persist and are readable.
  - Partial progress is visible in audit.

**`running` × watch cancelled:**
  - Becomes `cancelled`.
  - R5: returns `{:status :cancelled}`.
  - Containers created before cancellation persist.

**`complete` × W1 `transcript/harvest` (re-harvest, new request ID):**
  - A NEW IngestRun is created (`pending`). The old one stays `complete`.
  - R5 for old ID: still `complete`. R5 for new ID: `pending`.
  - Source-record deduplication prevents duplicate containers.

**`complete` × W2 `transcript/watch`:**
  - Valid. A watch can follow a completed harvest. New IngestRun created.

**`failed` × W1 `transcript/harvest` (retry):**
  - A new IngestRun is created. The old one stays `failed`.
  - Re-harvest from scratch is safe by idempotency. Already-ingested records fold to existing state.

**Terminal states (`complete`, `failed`, `cancelled`) × any write:**
  - The terminal IngestRun is never mutated. New operations create new IngestRuns.

### SourceArtifact — states: `does-not-exist`, `recorded`

SourceArtifact represents a file-level source record. It captures the file's existence and metadata, not
the file's content (content is in per-line SourceRecords / container revisions).

**`does-not-exist` × W3 source record ingest (first line of a new file):**
  - Becomes `recorded`.
  - R6 read-source-artifact: returns file metadata (path, file-id, source family).
  - R2 read-source-audit-projection: shows this file was discovered.

**`recorded` × W3 source record ingest (subsequent lines of the same file):**
  - Updated: line count increments, last-byte-offset advances.
  - R6: returns updated counts.

**`recorded` × W3 source record ingest (same file, same content via re-harvest):**
  - Idempotent. Counts do not double.
  - All reads: unchanged.

**`recorded` × W1/W2 from a different request but same file-id:**
  - The SourceArtifact may be updated with the new request ID added to the ingested-by set.
  - R6: shows both request IDs.

### ConversationContainer — states: `does-not-exist`, `created`, `updated`

**`does-not-exist` × W3 source record ingest (first record with this conversation ID):**
  - Becomes `created`.
  - Container ID: deterministic from `(source-family, conversation-id)`.
  - Revision: initial metadata (session_id, source family, model if known from system/init).
  - R1 read-conversation-projection: returns conversation with its first message(s).
  - R4 read-container: returns the conversation container.
  - R2: audit records this container was created.

**`created` × W3 source record ingest (additional records for same conversation):**
  - Becomes `updated`. New child containers (messages) added via composition edges.
  - Conversation metadata may update (latest timestamp, message count).
  - R1: conversation now shows additional messages in source order.
  - R4: container metadata updated.

**`created`/`updated` × W3 source record ingest (same record, re-ingest):**
  - Idempotent. Container ID is deterministic. No duplicate containers.
  - Composition edges are idempotent (same parent + child + order = same edge).
  - R1: unchanged.

**`created`/`updated` × W3 source record ingest (new record for same conversation via watch):**
  - Same as `updated` above. New message containers appear; conversation metadata updates.
  - R1: new messages visible in source order.

### MessageContainer — states: `does-not-exist`, `created`

**`does-not-exist` × W3 source record ingest (message line):**
  - Becomes `created`.
  - Container ID: deterministic from `(source-family, message-uuid)` when UUID available; fallback to
    `(source-family, file-id, byte-offset)`.
  - Revision: role, redacted content, timestamp.
  - SourceAnchor: file/line/offset/hash.
  - CompositionEdge: conversation → message (containment).
  - CompositionEdge: previous-message → this-message (ordering, when previous exists).
  - R1: message appears in conversation projection at its source-order position.
  - R3: if message contains tool calls, they appear in tool-call projection.
  - R4: message container is readable.

**`created` × W3 source record ingest (same record, re-ingest):**
  - Idempotent. No duplicate container or revision.
  - All reads: unchanged.

**`created` × W3 source record ingest (stream events for same message UUID):**
  - If a consolidated message already exists, stream events are NOT separately containerized.
  - The container is unchanged. Stream events may contribute to audit but not to containers.
  - R1, R4: unchanged.

### ToolCallContainer — states: `does-not-exist`, `created`

**`does-not-exist` × W3 source record ingest (message with tool_use block):**
  - Becomes `created`.
  - Container ID: deterministic from `(source-family, tool-use-id)`.
  - Revision: tool name, redacted input.
  - SourceAnchor: same source line as the parent message.
  - CompositionEdge: message → tool-call (production).
  - R3 read-tool-call-projection: tool call appears indexed by name.
  - R4 read-container: tool-call container is readable.
  - R1: tool call appears as child of its message in conversation projection.

**`created` × W3 source record ingest (same record, re-ingest):**
  - Idempotent.

**`created` × W3 source record ingest (tool-result arrives for this tool-use-id):**
  - ToolCallContainer unchanged. A new ToolResultContainer is created and linked via edge.
  - R3: tool call now shows its result.
  - R1: tool result appears after tool call in conversation projection.

### ToolResultContainer — states: `does-not-exist`, `created`

**`does-not-exist` × W3 source record ingest (tool_result event):**
  - Becomes `created`.
  - Container ID: deterministic from `(source-family, tool-use-id, "result")` — linked to the call.
  - Revision: redacted tool result content.
  - SourceAnchor: file/line/offset.
  - CompositionEdge: tool-call → tool-result (production, when parent tool-call exists).
  - R3: result appears alongside its tool call.
  - R4: tool-result container is readable.

**`created` × W3 source record ingest (same record, re-ingest):**
  - Idempotent.

**`does-not-exist` × W3 with unknown tool-use-id reference (orphaned result):**
  - ToolResultContainer is still created (the result data is real).
  - CompositionEdge to parent tool-call cannot be created (parent doesn't exist).
  - The tool-result container carries the tool-use-id in its revision content for later correlation.
  - R3: appears as an orphaned result (no parent tool call).
  - R4: readable, source anchor present.
  - Audit: records orphaned tool-result status.

### ArtifactContainer — states: `does-not-exist`, `created`

**`does-not-exist` × W3 source record ingest (artifact/attachment event):**
  - Becomes `created` only when the event represents an addressable artifact (has an ID or stable
    identity).
  - Container ID: deterministic from `(source-family, artifact-id)`.
  - Revision: artifact metadata, redacted content if present.
  - SourceAnchor: file/line/offset.
  - CompositionEdge: message → artifact (production, when source message is identifiable).
  - R1: artifact appears linked to its source message in conversation projection.

**`does-not-exist` × W3 with non-addressable artifact:**
  - No container created. The source record is preserved but not promoted to a container.
  - Audit records the event type.

### Revision — states: `does-not-exist`, `exists` (append-only)

Revisions for transcript containers are created at ingest time, not by user edit.

**`does-not-exist` × W3 (container creation):**
  - First revision `exists` with ingest-derived content.
  - R4 read-container: returns container with this revision as current.

**`exists` × W3 (conversation metadata update, e.g., result event):**
  - A new revision may be appended (metadata changed). Previous revision remains.
  - R4: current revision updated.

**`exists` × W3 (re-ingest same record):**
  - Idempotent. No duplicate revision created.

### SourceAnchor — states: `does-not-exist`, `exists` (immutable)

**`does-not-exist` × W3 (container creation):**
  - Becomes `exists`. Links container to file/line/offset/hash.
  - R4: container shows its source anchor.
  - R2: audit can trace container provenance.

**`exists` × any write:**
  - Immutable. Never modified.

### CompositionEdge — states: `does-not-exist`, `exists`

**`does-not-exist` × W3 (container creation):**
  - Becomes `exists`. Types:
    - `:contains` — conversation contains message
    - `:follows` — message follows previous message
    - `:replies-to` — message replies to parent (when present in source)
    - `:produced` — message produced tool-call; tool-call produced tool-result; message produced artifact
    - `:derived-from` — artifact derived from message/tool-result
    - `:anchored-to` — container anchored to source artifact / source record
  - R1: conversation projection uses containment and ordering edges.
  - R3: tool-call projection uses production edges.

**`exists` × W3 (re-ingest same record):**
  - Idempotent. Same parent + child + type + order = same edge.

### AuditLedgerEntry — states: `does-not-exist`, `recorded` (append-only)

**`does-not-exist` × W1/W2 (request submitted):**
  - Request-level entry `recorded`: request metadata, submitted timestamp, configured policy.

**`does-not-exist` × W3 (source record processed):**
  - Record-level entry `recorded`: source identity, redaction details, containers created, parse status.

**`recorded` × any read:**
  - R2 read-source-audit-projection: returns accumulated audit entries.
  - R5 read-run-status: progress counters derived from audit entries.

## Cross-Cutting Consistency Invariants

1. **Source-record idempotency.** `(source, file-id, byte-offset, line-hash)` is the deduplication key
   for the entire ingest pipeline. Two appends with the same identity produce exactly one set of
   containers, revisions, anchors, and edges in the materialized state.

2. **Container ID determinism.** All container IDs are derived deterministically from source-provided
   identifiers (conversation-id, message-uuid, tool-use-id) or fallback source-record identity
   (file-id + byte-offset). No random IDs in the container creation path.

3. **Redaction before persistence.** Nothing reaches any durable store unredacted (unless policy is
   `:off`). Redaction happens in the executor before depot append. Parse-error records apply the same
   redaction rule.

4. **Import = watch = same containers.** The native container types, edge types, and source anchors
   produced by harvest and by watch are identical. They differ only in acquisition mode and timing.

5. **Observation-only.** The ingest pipeline never spawns Claude/Codex, never modifies source files,
   never triggers downstream actions. This is a hard architectural rule, not a soft guideline.

6. **Source immutability.** No operation in this module mutates a SourceArtifact. The raw source material
   (at the redaction-policy level) is preserved exactly as captured.

7. **No inferred relations at ingest.** Only source-provided structure becomes accepted CompositionEdges.
   "This message is about topic X" or "this tool call modified file Y" are inferred relations that do
   NOT become accepted edges at ingest time.

8. **Parse errors are visible, not dropped.** A malformed JSONL line produces an audit entry, not silence.
   The audit surface must answer "what failed and why" for any ingest run.

9. **Stable identity across reruns.** Container identity for a given source record does not change between
   harvest reruns or between harvest and watch. A message ingested by harvest and re-encountered by watch
   resolves to the same container ID.

10. **No dependency on Space.** This module creates and queries its own containers. Space may later
    consume, mount, or transform these containers, but transcript ingest does not require Space to exist
    or function.

## Concurrency Behavior

- **Harvest + harvest (same paths):** safe. Source-record dedup prevents duplicate containers. The second
  harvest creates its own IngestRun for audit trail but folds to the same materialized containers.

- **Harvest + watch (overlapping):** safe. Same dedup mechanism. Records observed by both fold to one
  set of containers.

- **Watch + watch (same source family):** at most one active watch per source family. Second watch
  request for the same family should be rejected or queue behind the first.

- **Per-record concurrency:** if two records for the same conversation arrive on different depot
  partitions, the topology must correctly serialize access to the conversation's PState rows. This is
  a Phase 1 design concern (partition key choice), but the invariant is: no lost edges, no duplicate
  edges, correct ordering.

- **Ordering guarantees:** within a single file, JSONL lines are in source order (byte-offset ordered).
  Cross-file ordering (messages from different files in the same conversation) uses source timestamps
  as the sort key; when timestamps are identical or absent, byte-offset within file is the tiebreaker.
  The ordering guarantee: reads return messages in a deterministic order that respects source order
  within each file and approximates chronological order across files.

## Data Growth and Scale

- **Files:** a user with 6 months of daily Claude Code usage: ~100-500 session files.
- **Lines per file:** typical session: 50-500 lines. Long session: 1,000-5,000+ lines.
- **Total records per harvest:** 5,000-250,000. Bursty write at harvest time.
- **Containers per record:** 1-5 (one message + 0-4 tool calls/results/artifacts).
- **Total containers after full harvest:** 10,000-500,000.
- **Edges per container:** 2-3 on average (contains + follows + possibly produced).
- **Growth rate during watch:** matches Claude/Codex usage. 50-500 new records per day for an active
  user.
- **Dominant access pattern after ingest:** read-conversation-projection (range scan over a conversation's
  messages). This is the hot path. Must be efficient for 50-1000 message conversations.
- **Secondary access pattern:** read-tool-call-projection by tool name. Used for provenance correlation.
- **Audit access:** rare, acceptable latency in hundreds of ms.
- **Source anchor access:** per-container, point lookup. Low volume.

## Source Granularity Decision

The PRODUCT.md asks the implementation to explicitly decide source granularity. The decision is:

**Two-level source model:** file-level SourceArtifact + per-record SourceAnchors.

Rationale:
- File-level SourceArtifact captures the file's existence, path, file-id, and ingestion metadata. It
  does NOT store the file's full content (which could be very large and contains sensitive material).
- Per-record source identity is `(source, file-id, byte-offset, line-hash)`. Each container gets a
  SourceAnchor pointing to this identity, making the bridge between native objects and source
  file/line/offset explicit and queryable.
- The raw JSONL line content (after redaction) is preserved in the container's Revision, not in a
  separate per-line SourceArtifact. This avoids double-storing the parsed content.
- This matches the existing transcript.clj model where the source ledger keys by `(source, file-id,
  byte-offset, line-hash)` and the observation row carries the redacted payload.
- The alternative of storing each JSONL line as its own SourceArtifact would create N SourceArtifacts
  per file (redundant with the N containers already being created). The two-level model avoids this
  duplication while preserving full traceability.

The product invariant holds:
- The source is preserved (redacted payload in container revisions; file metadata in SourceArtifact;
  byte offset/hash in SourceAnchors).
- The native containers are real Softland objects (ObjectContainers with durable IDs).
- The bridge is explicit and queryable (SourceAnchors from containers to file/line/offset/hash).
