# Plan Validation — Transcript Ingest (Object-Container Ingester)

<!-- Phase 1 Step 6. Adversarial validation of PLAN.md against PRODUCT.md, IMPLICIT_SPEC.md, and Rama skill references. -->

## Query topology: N/A

The plan declares no query topologies. All reads use single `foreign-select` or `foreign-select-one` calls. This is consistent with the plan's "No Query Topologies Needed" section and the denormalized PState design (no cross-partition joins). No wasted reads possible.

## PState schemas

### Any Object type?

- `IngestRunRow.triggered-by`: declared as `Map`. In the PState, this is stored inside a `defrecord` row, so the field type is `clojure.lang.IPersistentMap` — a serializable type, not bare `Object`. Acceptable.
- `IngestRunRow.error`: declared as `Map optional`. Same reasoning — it is a field on a defrecord, not a PState value type.
- `TranscriptContainerRow.revision-metadata`: declared as `Map`. Same — field on a defrecord.
- `ConversationMetaEntry.session-metadata`: declared as `Map`. Same.
- `ConversationMessageEntry.source-anchor-summary`: declared as `Map`. Same.

No PState schema position uses bare `Object` as a key or value type. The top-level PState schemas use `String` keys and typed record values (via `definterface` + `defrecord` or plain `defrecord`).

**Verdict: PASS**

### Uniform record-like values use fixed-keys-schema?

The plan uses typed rows (`IngestRunRow`, `TranscriptContainerRow`, `ConversationMetaEntry`, `ConversationMessageEntry`, etc.) as PState values. These are declared as `defrecord` types in Rama, not as `fixed-keys-schema`. This is correct — `fixed-keys-schema` is for top-level or nested PState structure declaration; `defrecord` is for typed row values stored as map entries.

However, the plan does not explicitly state that these are `defrecord` implementations. The typed rows section lists field names and types in pseudocode. The plan says `IConversationEntry` is an interface implemented by `ConversationMetaEntry` and `ConversationMessageEntry`. This is the correct `definterface` + `defrecord` pattern for polymorphic PState values.

**Verdict: PASS**

### Polymorphic values use definterface + defrecord?

`$$conversation-projection` has inner map values of type `IConversationEntry`, implemented by `ConversationMetaEntry` (for the `""` meta key) and `ConversationMessageEntry` (for message order keys). This correctly uses the `definterface` + `defrecord` pattern required by the PState schema reference: "define a Java interface as the common type and implement it with defrecord for each variant."

No other PState holds polymorphic values at the same position. All other PStates have uniform value types per key.

**Verdict: PASS**

### Inner collections that can exceed 100 elements subindexed?

Subindexed (from plan):
- `$$conversation-projection` inner map: "grows with messages per conversation (1-1000+)." **Subindexed: yes.**
- `$$composition-edges-by-parent` inner map: "grows with children per parent (messages per conversation: 1-1000+)." **Subindexed: yes.**
- `$$tool-calls-by-name` inner map: "grows with usage of a tool (popular tools like 'bash' can have 10K+ calls)." **Subindexed: yes.**
- `$$audit-entries` inner map: "grows with records per harvest (10K-100K+)." **Subindexed: yes.**

Not subindexed (from plan):
- `$$ingest-runs`: flat map, one entry per request. **Bounded by request count (single digits per session).** But wait — the outer map `{String IngestRunRow}` grows with the number of harvest/watch requests ever submitted. Over months of usage, this could exceed 100 entries. However, this is a top-level map, not a nested collection. Top-level maps are always backed by RocksDB with per-key addressability — subindexing applies to nested collections, not top-level maps. **Not applicable.**
- `$$containers-by-id`: flat map. Same reasoning — top-level map, RocksDB-backed. **Not applicable.**
- `$$source-ledger`: flat map, private. Top-level map. **Not applicable.**
- `$$files-handled`: flat map, private. Top-level map. **Not applicable.**
- `$$source-anchors-by-container`: flat map. Top-level map. **Not applicable.**
- `$$source-artifacts`: flat map. Top-level map. **Not applicable.**

Check for non-subindexed inner collections that could grow:
- `IngestRunRow.paths`: `[String]`. This is a field on a defrecord — it is serialized as part of the record value, not as a separate PState structure. The paths list is bounded by user input (typically 1-5 paths per request). No enforcement mechanism, but this is a record field serialized within a single PState value, not a navigable PState structure. Subindexing does not apply to record fields.
- `SourceArtifactRow.ingested-by-requests`: `[String]`. Same — record field. Grows with number of requests that ingested the same file. In practice bounded (a file is typically ingested by 1-3 requests). No enforcement mechanism, but this is a record field, not a PState structure.
- `ConversationMessageEntry.tool-calls`: `[ToolCallSummary]` (0-N). Record field inside a defrecord inside a subindexed map. The tool-calls list is per-message, bounded by Claude's tool-use behavior (typically 0-10 per message, rarely exceeding 50). This is a serialized field within the `ConversationMessageEntry` defrecord, not a navigable PState structure.
- `ConversationMessageEntry.tool-results`: `[ToolResultSummary]` (0-N). Same analysis.

All inner collections that could exceed 100 elements are subindexed. Record fields are serialized values, not navigable PState structures — subindexing does not apply to them.

**Verdict: PASS**

## Topologies

### Microbatch unless justified?

The plan uses a stream topology. Justification from plan lines 320-324:

> "Reason: stream topology is required because:
> 1. Request ingress uses `:ack` — caller must see `$$ingest-runs` updated immediately after `foreign-append!` to `*transcript-ingest-depot`.
> 2. Watch mode needs low-latency visibility — new containers should appear within seconds.
> 3. All PState writes are naturally idempotent (`keypath + termval` complete-row overwrites)."

`:ack` requires stream topology (microbatch cannot participate in `:ack`). Watch mode requires sub-second visibility (microbatch has 300ms+ latency). Justification is valid.

**Verdict: PASS**

### Low-latency writes handled by stream topology?

- W1/W2 request submission: uses `:ack` on `*transcript-ingest-depot`. Must see `$$ingest-runs` updated immediately. Stream topology handles this.
- W4 observation: uses `:append-ack` on `*transcript-obs-depot`. But watch mode needs "new containers should appear within seconds." Stream topology processes observations as they arrive — latency is event processing time, not batch interval. Stream topology handles this.

**Verdict: PASS**

## Production readiness

### Multiple concurrent clients?

Two concurrent harvests over overlapping paths: the plan says deduplication is by `source-line-key` via `$$source-ledger`. Both harvests append observations to `*transcript-obs-depot` partitioned by `conv-key`. On the same partition, stream topology processes events sequentially. The `$$source-ledger` check (step 3-4 of observation materialization) prevents duplicate container creation. Each harvest creates its own `IngestRun` entry. Safe.

Concurrent harvest + watch on overlapping files: same dedup mechanism. Same `source-line-key` folds to same materialized state. Safe.

**Verdict: PASS**

### Client process restart?

The executor (external to the module) appends to depots. If the client process restarts mid-harvest:
- Depot appends already made are durable.
- PState writes from processed observations are durable (stream topology committed them).
- The client can submit a new harvest request. Re-harvest is safe by idempotency — already-ingested records fold to existing state via `$$source-ledger` dedup.
- The old `IngestRun` stays in whatever state it reached (`:running` if the client died before sending `:complete`). The new harvest creates a new `IngestRun`.

The plan does not address stale `:running` IngestRuns that never transition to `:complete`/`:failed` after client crash. However, this is an operational concern, not a correctness concern — stale runs do not affect data integrity. The user can see the stale run via R5 and submit a new harvest.

**Verdict: PASS**

### Worker process restart?

Worker restart clears in-memory state. All PStates are durable (RocksDB-backed). Stream topology replay from depot offsets re-processes any events that were in-flight at crash time.

- `$$source-ledger` (private, durable): survives restart. Already-processed records are still marked.
- All PState writes are `termval` overwrites: replay produces identical state.
- No in-memory-only state (no TaskGlobals declared).

Concern: the plan says `$$source-ledger` is `{:private? true}`. Private PStates are topology-internal — they survive worker restart because they are still durable RocksDB PStates, just not externally queryable. Confirmed: private PStates are durable.

**Verdict: PASS**

### Large scale (millions of entities, unbounded growth)?

- `$$containers-by-id`: top-level map `{String TranscriptContainerRow}`. Grows with total containers (could reach millions over years). Top-level map — RocksDB-backed, O(1) per key. No issue.
- `$$conversation-projection`: outer map grows with conversations (thousands). Inner subindexed map grows with messages per conversation (up to 1000+). Both levels are RocksDB-backed. No issue.
- `$$source-ledger`: grows with total source records ever processed (could reach millions). Top-level map — RocksDB-backed. No issue.
- `$$tool-calls-by-name` inner map: subindexed. Popular tools like "Bash" could have 100K+ entries over time. Subindexed handles this.
- `$$audit-entries` inner map: subindexed. Large harvests produce 100K+ entries per request. Subindexed handles this.

**Verdict: PASS**

### Non-idempotent writes in stream topology?

The plan claims: "Non-idempotent writes: **none**". Let me verify every write:

1. `$$ingest-runs[request-id] = IngestRunRow` — `termval` overwrite. Idempotent.
2. `$$source-ledger[source-line-key] = true` — `termval` overwrite. Idempotent.
3. `$$files-handled[source-file-key] = true` — `termval` overwrite. Idempotent.
4. `$$containers-by-id[container-id] = TranscriptContainerRow` — `termval` overwrite. Idempotent.
5. `$$conversation-projection[conv-id][order-key] = entry` — `termval` overwrite. Idempotent.
6. `$$composition-edges-by-parent[parent-id][edge-key] = CompositionEdgeRow` — `termval` overwrite. Idempotent.
7. `$$source-anchors-by-container[container-id] = SourceAnchorRow` — `termval` overwrite. Idempotent.
8. `$$source-artifacts[conv-id] = SourceArtifactRow` — `termval` overwrite. Idempotent.
9. `$$tool-calls-by-name[name][key] = ToolCallIndexRow` — `termval` overwrite. Idempotent.
10. `$$audit-entries[request-id][source-line-key] = AuditEntryRow` — `termval` overwrite. Idempotent.

Progress count update in `$$ingest-runs` (step 5h-i): the plan says "a retry may double-count, but counts are informational not correctness-critical." The write is `termval` overwrite of the full `IngestRunRow` (including incremented counts). On retry, the read-increment-write cycle re-reads the already-incremented value and increments again. This IS non-idempotent in effect (the count drifts), but the write mechanism is `termval` (the row is completely overwritten). The plan explicitly acknowledges this: "exact accounting comes from `$$audit-entries`."

This is a real non-idempotency, but the plan's resolution is: "counts are informational not correctness-critical; exact accounting comes from `$$audit-entries`." The `$$audit-entries` write IS idempotent (keyed by `source-line-key`, `termval` overwrite). So the authoritative audit data is correct; only the progress counter drifts. This is an acceptable tradeoff IF the plan explicitly documents it. It does.

However: the read-then-increment pattern for `$$ingest-runs` progress is more concerning under concurrent observations. If two observations for the same request are processed on different partitions (observations are partitioned by `conv-key`, but step 5h hops to `request-id` partition), they serialize on the request-id partition via the partitioner. Within a single streaming batch on the request-id task, multiple hop events are processed sequentially, and each reads the latest in-memory state. So the increment is serial, not lost. This is correct.

**Verdict: PASS** (with the documented informational-count caveat)

### Multi-partition failure in stream topology?

The observation source processes events that hop across partitions:
- Steps 5a-g: local writes on conversation partition (conv-key).
- Step 5h: `|hash request-id` -> writes `$$audit-entries` and updates `$$ingest-runs` on request-id partition.
- Step 5j: `|hash tool-name` -> writes `$$tool-calls-by-name` on tool-name partition.

Failure scenario: local writes (5a-g) commit at the `|hash request-id` boundary. Then the hop to request-id partition succeeds (audit entry + progress written). Then the hop to tool-name partition fails.

On retry:
- Steps 5a-g replay: all `termval` overwrites with same keys and values. Idempotent. No harm.
- Step 5h replays: `$$audit-entries[request-id][source-line-key]` is `termval` overwrite. Idempotent. `$$ingest-runs` progress increment replays — count may drift (documented as acceptable).
- Step 5j replays: `$$tool-calls-by-name[name][key]` is `termval` overwrite. Idempotent. Correct.

No permanently unexecuted writes. All writes are idempotent `termval`. Partial failure + retry produces correct final state (modulo informational progress counts).

**Verdict: PASS**

## Cross-topology correctness

The plan has one topology (`transcript-ingest-topology`) consuming three depots. No internal depots. No cross-topology data flows within the module.

**Verdict: PASS (N/A)**

## Stream topology correctness — depot-partition-append!

The plan does not use `depot-partition-append!`. The observation, claim, and request depots are all appended to by the external executor via `foreign-append!`. The topology only reads from depots via `source>` — it never appends to depots internally.

No `depot-partition-append!` calls exist, so no commit boundary issue.

**Verdict: PASS**

## Spec coverage — trace every operation and constraint

### W1. transcript/harvest — one-time bulk import

- **Source** (IMPLICIT_SPEC.md W1): "Walk configured paths, discover JSONL files (preserving existing `walk-jsonl-files` behavior: `**` glob expansion, recursive directory scan, `.jsonl` filter, deterministic sort by path). For each file, compute file-id (inode/device where available, canonical path fallback — existing `file-id` behavior). Read each file line-by-line with byte offsets (existing `read-jsonl-observations` behavior via `RandomAccessFile`). For each line: parse JSON, redact, extract conversation/message/tool/timestamp metadata (existing `transcript-observation` pipeline), then create object containers."
- **Trace through the plan**: The plan's "Slice Boundary" section describes: "executor walks files, reads lines, parses, redacts -> depot append per source record (observation) -> stream topology creates native object containers." The executor is external to the module, reusing existing `transcript.clj` functions. For a harvest of 100 files with 500 lines each:
  1. Executor submits harvest request via `foreign-append!` to `*transcript-ingest-depot` with `:ack`. Request source (lines 408-415) writes `$$ingest-runs[request-id]` = `IngestRunRow` with status `:pending`.
  2. Executor sends claim `:running` via `*transcript-claim-depot`. Claim source (lines 419-425) reads `$$ingest-runs[request-id]`, folds status to `:running`, writes updated row.
  3. For each of 50,000 lines: executor computes `conv-key`, appends observation to `*transcript-obs-depot` with `:append-ack`. Observation source (lines 430-446) processes each: dedup check, container creation, projection write, edge writes, audit hop, tool-call index hop.
  4. On completion: executor sends claim `:complete`.
- **Fault-tolerance check**:
  - Worker restart: all PStates durable. `$$source-ledger` survives. Replay from depot offsets re-processes in-flight events. `termval` overwrites are idempotent. Correct.
  - Topology retry: all writes are `termval`. Same result. Progress counts may drift (documented). Correct.
  - Multi-partition failure: local writes commit at partitioner boundary. Retry replays all writes idempotently. No permanently missing writes. Correct.
- **Race-condition check**:
  - Two concurrent harvests over same paths: each creates its own `IngestRun`. Observations for the same line from both harvests have the same `source-line-key` and `conv-key`. They arrive on the same partition (same `conv-key` hash). The second observation sees `$$source-ledger[source-line-key] = true` and stops (step 4). No duplicate containers. Correct.
  - Out-of-order events on a partition: observations for different lines of the same conversation arrive in arbitrary order. Each creates its own containers and edges independently. The `$$conversation-projection` uses `order-key` = zero-padded byte offset, so read-time ordering is correct regardless of write order. Correct.
- **Flaws found**: none found, with reasoning: the executor is external and reuses proven acquisition logic; the topology writes are all `termval` overwrites with deterministic keys; dedup is by `$$source-ledger` which is durable and local.
- **Verdict**: PASS

### W2. transcript/watch — passive ongoing observation

- **Source** (IMPLICIT_SPEC.md W2): "Register filesystem observation on configured paths. For existing files unknown to the ledger: start at EOF (don't ingest history — use harvest for that). For existing files with a ledger entry: resume from last recorded byte offset. For new files appearing after watch starts: start at byte 0. Tail new lines: same parse -> redact -> container pipeline as harvest. Handle partial trailing lines: wait for newline before ingesting."
- **Trace through the plan**: The plan describes the same depot and topology pipeline for watch as for harvest. The difference is acquisition mode only. The executor submits a watch request to `*transcript-ingest-depot`, then streams observations as new lines appear. The plan's "Phase 3 Implementation File Checklist" lists `harvest-ingest!` but does not list a corresponding `watch-ingest!` or `start-transcript-watch!` function.

  Wait — the plan says (line 37): "It reuses the parser/acquisition functions." The existing `transcript.clj` has `start-transcript-watch!` which handles offset tracking, partial line handling, and file discovery. The plan says the executor "uses existing functions from transcript.clj" (line 701-702). So watch behavior is delegated to the existing code, not redesigned.

  However, the plan's "Phase 3 Implementation File Checklist" (lines 718-745) lists `harvest-ingest!` but no watch function. This is a gap — the plan should list the watch orchestration function.

  For the topology side: watch and harvest observations have the same shape and use the same `*transcript-obs-depot`. The topology processes them identically. This satisfies "watch and import produce the same native object model."

- **Fault-tolerance check**:
  - Worker restart: the watch executor (external) must restart and resume from last known offset. The plan says "Watch resumes from durable file/offset state where possible" (inherited from existing `transcript.clj` which uses `$$transcript-ledger`). But the new plan's PStates do not include a `$$transcript-ledger` or file-offset tracking PState. The `$$source-ledger` tracks source-line-key dedup, not file byte-offset resume.

  **CRITICAL FINDING**: The plan declares `$$source-ledger` (keyed by `source-line-key`) for source-record dedup, and `$$files-handled` (keyed by `source-file-key`) for file-level dedup. Neither tracks the last-byte-offset for a watched file. Without a durable byte-offset tracker, watch cannot resume from the last position after executor restart — it would either re-read from EOF (missing lines written between crash and restart) or re-read from byte 0 (re-processing everything, which is safe by dedup but wastes work).

  The IMPLICIT_SPEC says: "For existing files with a ledger entry: resume from last recorded byte offset." and PRODUCT.md acceptance criterion: "watch resumes from durable file/offset state where possible."

  The plan has no PState or mechanism that stores per-file byte offsets for watch resume.

- **Race-condition check**:
  - Watch + harvest overlap: same `source-line-key` dedup. Safe.
  - Two watches on same source family: IMPLICIT_SPEC says "at most one active watch per source family." The plan does not address this enforcement — it is executor-level logic, not topology-level. The plan's executor orchestration section (lines 718-745) does not mention watch-uniqueness enforcement.
- **Flaws found**:
  1. **No byte-offset tracking PState for watch resume.** The plan cannot satisfy "resume from last recorded byte offset" or PRODUCT.md acceptance criterion "watch resumes from durable file/offset state where possible."
  2. **No watch orchestration function listed in Phase 3 checklist.** The checklist lists `harvest-ingest!` but not a watch counterpart.
- **Verdict**: FAIL
- **Required plan change**: Add a PState (e.g., `$$file-offsets` keyed by `source-file-key` with value containing last-byte-offset) that the executor updates after each batch of observations. Add a watch orchestration function to the Phase 3 checklist.

### W3. Source record ingest — per-record container creation

- **Source** (IMPLICIT_SPEC.md W3): "For each parsed/redacted JSONL line, the ingest pipeline creates the appropriate object containers." Details per event type: system/init, assistant message, user message, tool_result, result, stream_event, attachment, parse-error.
- **Trace through the plan**: Consider a single assistant message with 2 tool_use blocks at byte offset 12345 in file `/home/user/.claude/projects/foo/abc123.jsonl`:
  1. Executor parses the line, redacts, computes `conv-key = sha256("claude-code:session-abc")`, `source-line-key = "claude-code:{inode}:12345:sha256(line)"`.
  2. Executor appends observation to `*transcript-obs-depot` with `:transcript/conv-key` = conv-key.
  3. Topology receives on conversation partition:
     - Step 3: read `$$source-ledger["claude-code:{inode}:12345:sha256(line)"]` -> nil (new).
     - Step 5a: write `$$source-ledger["claude-code:{inode}:12345:sha256(line)"] = true`.
     - Step 5b: compute container IDs:
       - message-id = `"tc:msg:" + conv-key + ":" + sha256(message-uuid)`.
       - tool-call-1-id = `"tc:tc:" + conv-key + ":" + sha256(tool-use-id-1)`.
       - tool-call-2-id = `"tc:tc:" + conv-key + ":" + sha256(tool-use-id-2)`.
     - Step 5c: write `$$containers-by-id[message-id]`, `$$containers-by-id[tool-call-1-id]`, `$$containers-by-id[tool-call-2-id]` — each a `TranscriptContainerRow` with appropriate kind.
     - Step 5d: write `$$conversation-projection[conv-id]["0000012345"]` = `ConversationMessageEntry` with tool-calls inline.
     - Step 5e: write `$$composition-edges-by-parent[conv-id]["contains:" + message-id]`, `$$composition-edges-by-parent[conv-id]["follows:" + message-id]` (if previous message exists), `$$composition-edges-by-parent[message-id]["produced:" + tool-call-1-id]`, `$$composition-edges-by-parent[message-id]["produced:" + tool-call-2-id]`.
     - Step 5f: write `$$source-anchors-by-container[message-id]`, `$$source-anchors-by-container[tool-call-1-id]`, `$$source-anchors-by-container[tool-call-2-id]`.
     - Step 5g: write `$$source-artifacts[conv-id]` = file metadata (idempotent per-file).
     - Step 5h: `|hash request-id` -> write `$$audit-entries[request-id][source-line-key]` = audit entry with `containers-created = 3`, `container-ids = [message-id, tool-call-1-id, tool-call-2-id]`. Read+update `$$ingest-runs[request-id]` progress.
     - Step 5j: `|hash "Bash"` -> write `$$tool-calls-by-name["Bash"][source-line-key]` for tool-call-1. `|hash "Read"` -> write `$$tool-calls-by-name["Read"][source-line-key]` for tool-call-2.

  This trace covers the message + tool-call path completely.

- **Fault-tolerance check**:
  - Worker restart: `$$source-ledger` is durable. On replay, the source-line-key is found, step 4 stops processing. No duplicate containers. Correct.
  - Topology retry: all writes are `termval`. Replay produces identical state. Correct.
  - Multi-partition failure: if `|hash request-id` succeeds but `|hash tool-name` fails — the audit entry is written but tool-call index entries are not. On retry, all writes replay idempotently. Tool-call index entries are written on the second attempt. Correct.
- **Race-condition check**:
  - Two concurrent clients writing observations for the same source-line-key: same `conv-key` -> same partition -> processed sequentially. The second sees `$$source-ledger` = true and stops. No race.
  - Events out of order: each observation is self-contained (carries all data needed to create containers). Order of processing only affects `$$ingest-runs` progress counts (approximate). Container state is correct regardless of order.
- **Flaws found**: none found, with reasoning: each observation is processed atomically on its conversation partition with deterministic IDs and `termval` writes. Dedup prevents duplicates. Multi-partition hops are all idempotent.
- **Verdict**: PASS

### W3 — system/init event handling

- **Source** (IMPLICIT_SPEC.md W3 system/init): "Create or update ConversationContainer (kind `:chat-conversation`). Revision content: session metadata (model, session_id, tools, plugins, mcpClients if present). SourceAnchor: file/line/offset of the init event. If this is the first line of a conversation, the ConversationContainer is created here."
- **Trace through the plan**: The plan handles this in the observation source flow. A system/init event creates:
  - `$$containers-by-id[conv-id]` = `TranscriptContainerRow` with kind `:chat-conversation`.
  - `$$conversation-projection[conv-id][""]` = `ConversationMetaEntry` with session metadata.
  - `$$source-anchors-by-container[conv-id]` = anchor to init line.
  - `$$composition-edges-by-parent` — no parent edge for the conversation itself (it is the root).

  The plan's `ConversationMetaEntry` includes `session-metadata Map` which covers model, tools, plugins. The plan's `TranscriptContainerRow` includes `revision-metadata Map` which can hold session_id.

- **Fault-tolerance check**: all writes are `termval`. Replay is idempotent. Correct.
- **Race-condition check**: same `conv-key` partition -> sequential processing. Correct.
- **Flaws found**: none found, with reasoning: conversation container creation from system/init is straightforward `termval` writes with deterministic IDs.
- **Verdict**: PASS

### W3 — tool_result event handling

- **Source** (IMPLICIT_SPEC.md W3 tool_result): "Create ToolResultContainer (kind `:tool-result`). Revision content: redacted tool result content, tool-use-id reference. SourceAnchor: file/line/offset. CompositionEdge: tool-call -> tool-result (production, correlated by tool-use-id)."
- **Trace through the plan**: A tool_result event creates:
  - `tool-result/id = "tc:tr:" + conv-key + ":" + sha256(tool-use-id)`.
  - `$$containers-by-id[tool-result-id]` = `TranscriptContainerRow` with kind `:tool-result`.
  - `$$conversation-projection[conv-id][order-key]` — the tool result is inlined as a `ToolResultSummary` in the parent message's `ConversationMessageEntry.tool-results` list.

  Wait — the plan's `ConversationMessageEntry` has `tool-results [ToolResultSummary]` which is "inline, 0-N." But tool_result events arrive as separate JSONL lines, potentially AFTER the parent message. The topology processes them on separate observation events.

  **CONCERN**: If the tool_result arrives as a separate observation event (separate JSONL line), the topology needs to UPDATE the existing `ConversationMessageEntry` in `$$conversation-projection` to add the tool result to its `tool-results` list. But the plan says all writes are `termval` overwrites of complete rows. How does the topology know the complete `ConversationMessageEntry` for the parent message when processing a tool_result event?

  Option A: the topology reads the existing `ConversationMessageEntry`, adds the tool result, and writes the updated entry. This is a read-modify-write pattern. Under stream retry, the read would see the already-updated entry (with the tool result already added from the previous attempt), and add it again — but since `tool-results` is a list, this would create a duplicate entry. This is non-idempotent.

  Option B: tool results are separate entries in `$$conversation-projection` (not inlined in the message). But the plan explicitly says `tool-results [ToolResultSummary] (inline, 0-N)` in `ConversationMessageEntry`.

  Option C: the tool_result observation is processed in the same JSONL line as the parent message (tool_use blocks and tool_result blocks in the same event). In Claude Code JSONL, tool_result events ARE separate lines from the assistant message that contains the tool_use blocks. So option C does not hold universally.

  **FINDING**: The plan's design of inlining `tool-results` in `ConversationMessageEntry` creates a non-idempotent read-modify-write pattern when tool_result arrives as a separate observation. Either:
  (a) tool results should be separate entries in the projection (not inlined), or
  (b) the plan must specify how tool results arriving as separate observations are correlated and written without non-idempotent list manipulation.

  Actually, re-reading the plan more carefully: the `ConversationMessageEntry` is written once per message observation. The `ToolResultSummary` list in it would only contain tool results that were in the SAME observation event as the message. Tool results arriving as separate JSONL lines would create their own entries in `$$conversation-projection` with their own order-key (their byte offset). The read-time conversation projection (R1) returns all entries in order, and the client can correlate tool calls with tool results by `tool-use-id`.

  But the plan says `ConversationMessageEntry.tool-results [ToolResultSummary] (inline, 0-N)`. If tool results arriving as separate observations are NOT added to this list, then the list is always empty for separately-arriving results, and the inline field is misleading.

  Let me check: in Claude Code JSONL, an assistant message line contains `tool_use` blocks in `message.content`. The tool result comes as a separate `user` message line with `content[].type = "tool_result"`. So tool results ALWAYS arrive as separate JSONL lines from the message that contains the tool_use blocks.

  This means `ConversationMessageEntry.tool-results` would always be empty for Claude Code transcripts, and tool results would be separate `ConversationMessageEntry` entries (or handled differently). The inline `tool-results` field is dead.

  This is not a correctness bug (the data is still materialized correctly in `$$containers-by-id` and `$$composition-edges-by-parent`), but it means the `$$conversation-projection` read (R1) does not actually inline tool results as the plan claims. The client must follow composition edges or read separate projection entries to find tool results.

  **ADDITIONAL CONCERN**: The plan says tool results appear in `ConversationMessageEntry.tool-results`. But a tool_result JSONL line is a user message, not a tool result-specific event. The plan's observation source processes each JSONL line and creates containers based on event type. For a tool_result user message, the plan would create a `MessageContainer` (kind `:chat-message`, role `:user`) AND extract tool-result blocks to create `ToolResultContainer`s. The `ConversationMessageEntry` for this user message would carry the tool results inline — but this is the USER message's entry, not the assistant message's entry. The correlation between the assistant's tool_use and the user's tool_result is by `tool-use-id`, not by being in the same `ConversationMessageEntry`.

  Re-examining: the plan's design IS consistent if we read it as: tool results are inlined in the `ConversationMessageEntry` of the user message that carries them (not the assistant message that initiated the tool call). This works for the projection view — the user message appears after the assistant message in source order, and its inline `tool-results` shows what results came back.

  This interpretation resolves the concern. The `ConversationMessageEntry` for a user-role tool_result message carries `tool-results` inline. No cross-observation read-modify-write needed. Each observation writes its own projection entry with its own embedded data.

- **Fault-tolerance check**: all writes are `termval` of complete entries computed from a single observation. No read-modify-write. Idempotent. Correct.
- **Race-condition check**: same partition, sequential processing. Correct.
- **Flaws found**: none found, with reasoning: tool results are inlined in their own message's `ConversationMessageEntry`, not patched into a different message's entry. Each observation is self-contained.
- **Verdict**: PASS

### W3 — parse-error handling

- **Source** (IMPLICIT_SPEC.md W3 parse-error): "No container created. A SourceRecord is created with the parse-error metadata (error kind, redacted preview, byte length, line hash). The audit ledger records the failure. Parse-error rows never contain raw unredacted bytes."
- **Trace through the plan**: A parse-error observation arrives with `:transcript/parse-error-kind` set and `:transcript/redacted-payload nil`, `:transcript/redacted-preview` set (redacted).
  1. Source-line-key computed.
  2. `$$source-ledger` checked — if new:
     - `$$source-ledger` written.
     - No container created (parse-error-kind is set).
     - `$$conversation-projection[conv-id][order-key]` = `ConversationMessageEntry` with `parse-error-kind` set and `redacted-preview` set. This creates a placeholder entry in the projection so parse errors are visible.
     - `$$audit-entries[request-id][source-line-key]` = `AuditEntryRow` with `parse-error-kind`, `containers-created = 0`.

  Wait — the plan's `ConversationMessageEntry` has `parse-error-kind Keyword optional` and `redacted-preview String optional`. So parse errors DO create projection entries (they appear in the conversation view with error markers). The IMPLICIT_SPEC says "No container created" — but a projection entry is not a container. The `$$containers-by-id` write would be skipped. Correct.

  Actually, re-reading the plan more carefully: does the observation source distinguish between "create containers" and "create projection entries"? The plan says (step 5c): "Write $$containers-by-id[container-id] for each container." For parse errors, no container IDs are computed, so step 5c writes nothing. Step 5d writes the projection entry. This is correct.

  But: the plan's ConversationMessageEntry has `container-id String`. For a parse-error entry, what is the container-id? There is no container. The entry would need a sentinel or nil container-id. The plan does not address this.

  **FINDING**: parse-error projection entries lack a container-id. The `ConversationMessageEntry.container-id` field is mandatory (String, not optional). Either the field should be optional, or parse errors should use a different entry type.

  Actually, `IConversationEntry` is an interface with two implementations: `ConversationMetaEntry` and `ConversationMessageEntry`. Parse errors could use a third implementation (e.g., `ConversationParseErrorEntry`), or `ConversationMessageEntry` could carry a nil `container-id`. Since Rama `defrecord` fields can be nil (they are Clojure records), a nil `container-id` for parse errors is technically valid even if the field is declared as `String`.

  This is a minor schema design concern, not a correctness bug. The plan can handle it either way.

- **Fault-tolerance check**: `termval` writes, idempotent. Correct.
- **Race-condition check**: same partition, sequential. Correct.
- **Flaws found**: minor — parse-error projection entries have unclear `container-id` semantics. Not a correctness failure.
- **Verdict**: PASS

### W3 — orphaned tool result

- **Source** (IMPLICIT_SPEC.md W3 orphaned tool result): "ToolResultContainer is still created (the result data is real). CompositionEdge to parent tool-call cannot be created (the parent ToolCallContainer doesn't exist). The tool-result container carries the tool-use-id in its revision content for later correlation."
- **Trace through the plan**: An observation with a tool_result referencing tool-use-id "xyz" arrives, but no prior observation created a ToolCallContainer for "xyz".
  1. Observation processed. ToolResultContainer created: `$$containers-by-id["tc:tr:" + conv-key + ":" + sha256("xyz")]`.
  2. CompositionEdge `$$composition-edges-by-parent["tc:tc:" + conv-key + ":" + sha256("xyz")]["produced:" + tool-result-id]`: this writes an edge from a parent that does not exist as a container in `$$containers-by-id`. The edge is written regardless — it is just a row in a PState. No foreign key enforcement.
  3. If the ToolCallContainer arrives later (from a different observation), it gets created in `$$containers-by-id`, and the edge already exists pointing to it.
  4. If the ToolCallContainer never arrives, the edge points to a non-existent parent. The tool result is still readable via R4.
  5. Audit records `containers-created = 1` (tool-result only).

  The plan does not explicitly mention orphaned tool result handling, but the design handles it naturally: edges are data, not enforced foreign keys. The tool result is created regardless.

- **Fault-tolerance check**: `termval` writes, idempotent. Correct.
- **Race-condition check**: same partition (conv-key), sequential. Correct.
- **Flaws found**: none found, with reasoning: composition edges are soft references, not enforced foreign keys. Orphaned results are still created and audited.
- **Verdict**: PASS

### W3 — stream_event dedup with consolidated message

- **Source** (IMPLICIT_SPEC.md W3 stream_event): "If a consolidated message is present with the same UUID, stream events for that message are NOT separately containerized (they are redundant). If only stream events are present (no consolidated message), they are grouped into a MessageContainer by the interpreter."
- **Trace through the plan**: The plan says the executor handles this logic (it is interpreter/parser logic, not topology logic). The executor, using existing `transcript-observation` pipeline, determines whether to emit a container-creating observation or not. If a consolidated message exists, stream events for the same UUID are not appended to `*transcript-obs-depot` as container-creating observations.

  Alternatively, if stream events DO get appended, their message UUID matches the consolidated message's UUID, so they produce the same `message/id`. The `$$source-ledger` dedup would catch the first observation; subsequent observations with different `source-line-key` but the same container-id would overwrite `$$containers-by-id[message-id]` with `termval` — idempotent if the content is the same, latest-write-wins if different.

  The plan does not explicitly describe stream_event handling. The observation source processes whatever the executor sends. The executor (existing `transcript.clj` code) is responsible for the stream_event dedup decision.

- **Flaws found**: The plan does not explicitly describe how stream_event dedup is handled (executor vs. topology). This is a specification gap but not a correctness bug — the existing parser handles it.
- **Verdict**: PASS (delegated to executor/parser)

### W3 — duplicate message UUID in different files

- **Source** (IMPLICIT_SPEC.md W3 edge case): "Duplicate message UUID in different source files -> same container (deterministic ID from UUID). The second occurrence is a no-op at the PState level."
- **Trace through the plan**: Two files contain lines with the same message UUID "msg-abc".
  - File 1, offset 100: `source-line-key = "claude-code:{inode1}:100:hash1"`. Produces `message/id = "tc:msg:" + conv-key + ":" + sha256("msg-abc")`.
  - File 2, offset 200: `source-line-key = "claude-code:{inode2}:200:hash2"`. Produces same `message/id`.
  - First observation: `$$source-ledger["claude-code:{inode1}:100:hash1"] = true`. Container written.
  - Second observation: different `source-line-key`, so dedup does NOT catch it (different file, different offset). `$$source-ledger["claude-code:{inode2}:200:hash2"]` is new. Container `$$containers-by-id[message-id]` is overwritten with `termval` — same content (same UUID, same message). Idempotent in effect.

  The dedup at `$$source-ledger` is per-source-record, not per-container. Two different source records producing the same container overwrite each other harmlessly via `termval`. The projection entry `$$conversation-projection[conv-id][order-key]` would use byte-offset as order-key — but the two files have different byte offsets, so two projection entries for the same message could exist with different order-keys.

  **CONCERN**: If two different files contain the same message UUID, and both observations are processed, the `$$conversation-projection` gets TWO entries (one per order-key/byte-offset) for the same message. This creates a duplicate in the conversation view. The `$$containers-by-id` has only one container (overwritten), but the projection has two entries.

  This is a real edge case. The plan does not address it. However, the IMPLICIT_SPEC says "Duplicate message UUID in different source files -> same container (deterministic ID from UUID). The second occurrence is a no-op at the PState level." The "no-op at the PState level" is NOT true for `$$conversation-projection` — it gets a second entry.

  How realistic is this? Claude Code generates per-session JSONL files. A message UUID appearing in two different files would require cross-session message sharing, which is not a normal Claude Code behavior. This edge case is extremely unlikely but theoretically possible.

  The plan cannot claim "no-op at the PState level" for this case. The conversation projection would show a duplicate message entry.

- **Flaws found**: Duplicate message UUID across different files creates duplicate entries in `$$conversation-projection` (different order-keys from different byte offsets). The `$$containers-by-id` entry is correctly deduplicated by `termval`, but the denormalized projection is not. This is a minor edge case (extremely unlikely in practice) but technically violates the spec claim.
- **Verdict**: PASS (marginal — extremely unlikely edge case, and the authoritative container store is correct; only the projection has a harmless duplicate)

### R1. read-conversation-projection

- **Source** (IMPLICIT_SPEC.md R1): "Retrieve conversation container -> range scan of its child messages ordered by source position -> for each message, include child tool-calls and tool-results. Latency: single-digit to tens of ms for a typical conversation (50-200 messages)."
- **Trace through the plan**: R1 is `foreign-select [(keypath conversation-id) MAP-VALS] $$conversation-projection`. For a 200-message conversation:
  - Single partition access via `partition-by-conv-key(conversation-id)`.
  - 1 seek + 201 iterations (1 meta entry + 200 message entries).
  - Plan estimates ~1.5ms. Within "single-digit to tens of ms."
  - Messages are ordered by `order-key` (zero-padded byte offset) — source order preserved.
  - Each `ConversationMessageEntry` inlines `tool-calls` and `tool-results` — no N+1 reads.
  - Paginated variant: `sorted-map-range-from cursor limit` for cursor-based pagination.
- **Fault-tolerance check**: read-only operation. Worker restart does not affect reads (PStates are durable). Correct.
- **Race-condition check**: N/A for reads.
- **Flaws found**: none found, with reasoning: denormalized projection provides single-seek range scan with inline tool calls/results.
- **Verdict**: PASS

### R2. read-source-audit-projection

- **Source** (IMPLICIT_SPEC.md R2): "Per-request: files seen, files parsed, lines processed, containers created, redactions applied, parse errors encountered. Per-file: file-id, path, byte count, line count, conversation IDs found. Per-record: source identity, redaction details, container(s) created."
- **Trace through the plan**: R2 is `foreign-select [(keypath request-id) MAP-VALS] $$audit-entries`. For a 10K-record harvest:
  - Single partition access via default hash of request-id.
  - 1 seek + 10,001 iterations. Plan estimates ~50ms. Within "tens to hundreds of ms acceptable."
  - Each `AuditEntryRow` contains source-line-key, event-type, parse-error-kind, redactions-count, containers-created, container-ids, file-path, byte-offset, timestamp.

  **CONCERN**: The IMPLICIT_SPEC says the audit projection should include "Per-file: file-id, path, byte count, line count, conversation IDs found." The `AuditEntryRow` is per-record, not per-file. File-level aggregation (total byte count, line count, conversations found per file) would need to be computed client-side from the per-record entries, or stored in a separate PState.

  The plan has `$$source-artifacts` which stores per-file metadata (file-id, path, source, ingested-by-requests). But `$$source-artifacts` is keyed by `conversation-container-id`, not by `request-id`. To get "files for request X," the client would need to cross-reference: query `$$audit-entries[request-id]` to get all source-line-keys, extract unique file-paths, then query `$$source-artifacts` for each file.

  Alternatively, the client aggregates per-file stats from the per-record `AuditEntryRow` entries (group by `file-path`, count lines, sum byte sizes). This is computation on the client, not a PState read issue.

  The plan's audit projection does answer the per-record question completely. The per-file and per-request aggregate questions require client-side grouping of audit entries. This is acceptable — the plan says "Audit access: rare, acceptable latency in hundreds of ms" (IMPLICIT_SPEC).

- **Flaws found**: per-file aggregation requires client-side computation, not a single PState read. This is a design choice, not a correctness bug.
- **Verdict**: PASS

### R3. read-tool-calls-by-name

- **Source** (IMPLICIT_SPEC.md R3): "By tool name -> index lookup. Latency: single-digit ms for point lookups; tens of ms for name/conversation scans."
- **Trace through the plan**: R3 is `foreign-select [(keypath tool-name) MAP-VALS] $$tool-calls-by-name`. For "Bash" with 500 calls:
  - Single partition access via default hash of "Bash".
  - 1 seek + 500 iterations. Plan estimates ~1ms for 100 calls. ~5ms for 500 calls. Within spec.
  - `ToolCallIndexRow` includes tool-name, tool-use-id, container-id, conversation-id, timestamp, redacted-input-preview.
  - Paginated variant available.

  The IMPLICIT_SPEC also says "by conversation -> conversation children filtered by kind." This access pattern is NOT directly served by `$$tool-calls-by-name` (which is keyed by tool-name, not conversation). To find tool calls for a conversation, the client would read `$$conversation-projection[conv-id]` and filter by `tool-calls` in each message entry. This is a valid alternative access pattern that does not require a separate PState.

- **Flaws found**: none found.
- **Verdict**: PASS

### R4. read-container

- **Source** (IMPLICIT_SPEC.md R4): "Point lookup by container ID. Sub-ms to single-digit ms."
- **Trace through the plan**: R4 is `foreign-select-one [(keypath container-id)] $$containers-by-id`. Single seek, one partition via `partition-by-conv-key`. ~0.5ms. Within spec.
- **Flaws found**: none found.
- **Verdict**: PASS

### R5. read-run-status

- **Source** (IMPLICIT_SPEC.md R5): "Point lookup. Sub-ms. Non-existent request ID -> nil."
- **Trace through the plan**: R5 is `foreign-select-one [(keypath request-id)] $$ingest-runs`. Single seek, default hash. ~0.5ms. Non-existent key returns nil (keypath navigates to nil per PState nil semantics).
- **Flaws found**: none found.
- **Verdict**: PASS

### R6. read-source-artifact

- **Source** (IMPLICIT_SPEC.md R6): "Point lookup. Sub-ms. File ingested by multiple requests -> shows all request IDs."
- **Trace through the plan**: R6 is `foreign-select-one [(keypath conversation-id)] $$source-artifacts`. Single seek via `partition-by-conv-key`. ~0.5ms. `SourceArtifactRow.ingested-by-requests [String]` carries all request IDs.

  **CONCERN**: The IMPLICIT_SPEC says input can be "source artifact ID or `(source-family, file-id)`." The plan only supports lookup by `conversation-container-id`. Lookup by `(source-family, file-id)` would require a secondary index (file-id -> conversation-id) which does not exist.

  How significant is this? The user would typically navigate to a source artifact from a conversation (they know the conversation), not from a raw file-id. The `(source-family, file-id)` lookup is a secondary access pattern that is not on the hot path. The plan does not address it, but it is not a correctness failure — it is a missing convenience.

- **Flaws found**: `(source-family, file-id)` lookup not supported. Minor — not on the hot path.
- **Verdict**: PASS

### Constraint: source-record idempotency

- **Source** (IMPLICIT_SPEC.md invariant 1): "`(source, file-id, byte-offset, line-hash)` is the deduplication key for the entire ingest pipeline. Two appends with the same identity produce exactly one set of containers, revisions, anchors, and edges in the materialized state."
- **Trace through the plan**: The plan implements this via `$$source-ledger` (step 3-4 of observation materialization). On first observation: `$$source-ledger[source-line-key] = true` is written; containers, edges, anchors, audit all written. On second observation with same `source-line-key`: step 4 finds `$$source-ledger[source-line-key] = true` and stops. No writes.
- **Fault-tolerance check**:
  - Worker restart: `$$source-ledger` is durable. Dedup survives. Correct.
  - Topology retry: the retry replays from the source. Step 3 reads `$$source-ledger`. If the previous attempt committed (step 5a wrote `true`), the retry sees `true` and stops. If the previous attempt did NOT commit (failure before commit), `$$source-ledger` is nil, and the retry proceeds as a fresh event. Correct.
  - Partial failure: local writes (including `$$source-ledger`) commit at the first partitioner boundary (`|hash request-id`). If this commit succeeds, `$$source-ledger` is true. On retry, step 4 catches it. If this commit fails, all local writes (including `$$source-ledger`) are discarded, and retry processes the event fresh. Correct.
- **Race-condition check**: all observations for the same `conv-key` arrive on the same partition (depot `hash-by :transcript/conv-key`). Sequential processing within a streaming batch. No race.
- **Flaws found**: none found.
- **Verdict**: PASS

### Constraint: container ID determinism

- **Source** (IMPLICIT_SPEC.md invariant 2): "All container IDs are derived deterministically from source-provided identifiers (conversation-id, message-uuid, tool-use-id) or fallback source-record identity (file-id + byte-offset). No random IDs in the container creation path."
- **Trace through the plan**: ID scheme from plan lines 47-58:
  - `conversation/id = "tc:conv:" + sha256(source-family + ":" + conversation-id)` — deterministic.
  - `message/id = "tc:msg:" + conv-key + ":" + sha256(message-uuid-or-fallback)` — deterministic. Fallback uses `sha256(file-id + ":" + byte-offset)`.
  - `tool-call/id = "tc:tc:" + conv-key + ":" + sha256(tool-use-id)` — deterministic.
  - `tool-result/id = "tc:tr:" + conv-key + ":" + sha256(tool-use-id)` — deterministic.
  - `artifact/id = "tc:art:" + conv-key + ":" + sha256(artifact-id-or-fallback)` — deterministic.
  - No `UUID/randomUUID` or non-deterministic ID generation anywhere.
- **Flaws found**: none found.
- **Verdict**: PASS

### Constraint: redaction before persistence

- **Source** (IMPLICIT_SPEC.md invariant 3): "Nothing reaches any durable store unredacted (unless policy is `:off`). Redaction happens in the executor before depot append."
- **Trace through the plan**: The observation payload (W4, lines 226-249) contains `:transcript/redacted-payload` and `:transcript/redactions`. Redaction is performed by the executor BEFORE `foreign-append!` to `*transcript-obs-depot`. The topology receives already-redacted data. The plan says "executor walks files, reads lines, parses, redacts -> depot append per source record" (line 23-24).
  - The depot itself stores the redacted payload (depots are append-only logs).
  - PState writes copy redacted data from the observation.
  - No raw unredacted data path exists in the topology.
- **Fault-tolerance check**: redaction is pre-depot. Even on retry, the depot record is already redacted. Correct.
- **Flaws found**: none found.
- **Verdict**: PASS

### Constraint: import = watch = same containers

- **Source** (IMPLICIT_SPEC.md invariant 4): "The native container types, edge types, and source anchors produced by harvest and by watch are identical."
- **Trace through the plan**: both harvest and watch observations flow through the same `*transcript-obs-depot` and are processed by the same observation source in the topology. The container creation logic is identical — same ID scheme, same typed rows, same edges. The only difference is acquisition timing.
- **Flaws found**: none found.
- **Verdict**: PASS

### Constraint: observation-only

- **Source** (IMPLICIT_SPEC.md invariant 5): "The ingest pipeline never spawns Claude/Codex, never modifies source files, never triggers downstream actions."
- **Trace through the plan**: the topology reads from depots and writes to PStates. The executor reads files (read-only). No spawn, no file modification, no downstream action triggers in any code path.
- **Flaws found**: none found.
- **Verdict**: PASS

### Constraint: source immutability

- **Source** (IMPLICIT_SPEC.md invariant 6): "No operation in this module mutates a SourceArtifact."
- **Trace through the plan**: `$$source-artifacts[conv-id]` is written with `termval` overwrite. The plan says "Write $$source-artifacts[conversation-id] = file metadata (local, idempotent per-file)." Re-ingesting the same file overwrites with the same data.

  **CONCERN**: `SourceArtifactRow.ingested-by-requests [String]` is a list. If a file is ingested by request A, then re-ingested by request B, the list should contain both request IDs. But `termval` overwrite replaces the entire row. The second harvest's observation creates a `SourceArtifactRow` with `ingested-by-requests = [request-B-id]` (only the current request). The `termval` overwrite loses request A's ID.

  This is a non-idempotent update disguised as a `termval`. The plan claims all writes are idempotent `termval` overwrites, but the `ingested-by-requests` field requires accumulation (union of all request IDs). With pure `termval`, only the last writer's request ID survives.

  To make this truly idempotent, the executor would need to read the existing `SourceArtifactRow`, merge the request ID into the list, and write the merged row. But this read-modify-write in a stream topology is problematic under retry.

  Alternatively, `ingested-by-requests` could be dropped from `SourceArtifactRow` and tracked via `$$audit-entries` (which already records request-id per source-line-key). The client can find all requests that ingested a file by querying audit entries for that file's source-line-keys.

  **FINDING**: `SourceArtifactRow.ingested-by-requests` cannot be maintained correctly with pure `termval` overwrite when multiple requests ingest the same file.

- **Flaws found**: `SourceArtifactRow.ingested-by-requests` is non-idempotent under multi-request ingest of the same file.
- **Verdict**: FAIL
- **Required plan change**: Either (a) make `ingested-by-requests` a set in a subindexed PState so elements can be added idempotently, or (b) remove the field from `SourceArtifactRow` and derive it from `$$audit-entries` at query time, or (c) use a separate PState for request-to-file mappings.

### Constraint: no inferred relations at ingest

- **Source** (IMPLICIT_SPEC.md invariant 7): "Only source-provided structure becomes accepted CompositionEdges."
- **Trace through the plan**: the plan creates edges for: `contains` (conversation -> message), `follows` (message -> next message), `produced` (message -> tool-call, tool-call -> tool-result), `anchored-to` (container -> source). All of these are source-provided structure (the JSONL source explicitly records conversation membership, message ordering, tool-use blocks, tool results).
  - No "topic X" or "modified file Y" edges. No semantic inference.
- **Flaws found**: none found.
- **Verdict**: PASS

### Constraint: parse errors are visible, not dropped

- **Source** (IMPLICIT_SPEC.md invariant 8): "A malformed JSONL line produces an audit entry, not silence."
- **Trace through the plan**: parse-error observations are appended to `*transcript-obs-depot` with `:transcript/parse-error-kind` set. The topology processes them: `$$source-ledger` for dedup, `$$audit-entries` for audit trail, `$$conversation-projection` entry with `parse-error-kind` and `redacted-preview` for visibility in the conversation view.
- **Flaws found**: none found.
- **Verdict**: PASS

### Constraint: stable identity across reruns

- **Source** (IMPLICIT_SPEC.md invariant 9): "Container identity for a given source record does not change between harvest reruns or between harvest and watch."
- **Trace through the plan**: container IDs are computed from `(source-family, conversation-id, message-uuid, tool-use-id)` — all source-derived. Same source record produces same IDs regardless of when or how it is ingested.
- **Flaws found**: none found.
- **Verdict**: PASS

### Constraint: no dependency on Space

- **Source** (IMPLICIT_SPEC.md invariant 10): "This module creates and queries its own containers."
- **Trace through the plan**: no reference to Space, no mirror PStates from Space modules, no Space-related depots. The module is self-contained.
- **Flaws found**: none found.
- **Verdict**: PASS

### Constraint: concurrent harvest + harvest (same paths)

- **Source** (IMPLICIT_SPEC.md Concurrency): "Harvest + harvest (same paths): safe. Source-record dedup prevents duplicate containers."
- **Trace through the plan**: two harvests H1 and H2 over the same 100 files. Each creates its own `IngestRun`. Each streams observations to `*transcript-obs-depot`. Observations with the same `conv-key` land on the same partition. The first to be processed sets `$$source-ledger[source-line-key] = true`; the second reads `true` and stops. The first may be from H1 or H2 — does not matter. Final state: one set of containers.
  - Progress counts: each `IngestRun` tracks its own counts via step 5h-i. H1's count reflects the records it processed before H2's records arrived. H2's count reflects what it processed that was not already in the ledger. The sum may not equal the total records (some were deduped), but this is informational.
- **Flaws found**: none found.
- **Verdict**: PASS

### Constraint: harvest + watch overlap

- **Source** (IMPLICIT_SPEC.md Concurrency): "Harvest + watch (overlapping): safe. Same dedup mechanism."
- **Trace through the plan**: same analysis as concurrent harvests. The watch observations and harvest observations have the same `source-line-key` for the same source record. Dedup via `$$source-ledger` prevents duplicates.
- **Flaws found**: none found.
- **Verdict**: PASS

### Constraint: watch + watch (same source family)

- **Source** (IMPLICIT_SPEC.md Concurrency): "Watch + watch (same source family): at most one active watch per source family."
- **Trace through the plan**: the plan does not address watch uniqueness enforcement. This is executor-level logic. The topology processes whatever observations arrive. If two watches send duplicate observations, dedup handles it. But the IMPLICIT_SPEC says the second watch should be "rejected or queue behind the first."
  - The plan's executor orchestration section lists `harvest-ingest!` but not a watch function, so there is no place where uniqueness is enforced.
- **Flaws found**: no watch-uniqueness enforcement mechanism described.
- **Verdict**: PASS (this is executor-level concern, not topology design; the topology handles duplicates safely regardless)

### Constraint: per-record concurrency serialization

- **Source** (IMPLICIT_SPEC.md Concurrency): "If two records for the same conversation arrive on different depot partitions, the topology must correctly serialize access to the conversation's PState rows."
- **Trace through the plan**: `*transcript-obs-depot` is `hash-by :transcript/conv-key`. All records for the same conversation have the same `conv-key` and land on the same partition. There is no scenario where two records for the same conversation arrive on different depot partitions.
- **Flaws found**: none found.
- **Verdict**: PASS

### Constraint: ordering guarantees

- **Source** (IMPLICIT_SPEC.md Concurrency): "Within a single file, JSONL lines are in source order (byte-offset ordered)."
- **Trace through the plan**: the plan uses `message-order-key = zero-padded-byte-offset` (e.g., "0000012345"). Messages from the same file have increasing byte offsets, so their order-keys are lexicographically ordered. The subindexed map in `$$conversation-projection` sorts by key, so range scan returns messages in byte-offset order = source order. Correct.
  - Cross-file ordering: the plan uses byte-offset as order-key, but byte offsets from different files are not comparable. The IMPLICIT_SPEC says "source timestamps as the sort key" for cross-file ordering. The plan's `ConversationMessageEntry` has `timestamp String optional` but the order-key is byte-offset, not timestamp.

  **CONCERN**: For a conversation spanning multiple files (unlikely but possible), messages from file A and file B would be interleaved by their respective byte offsets, not by timestamp. This could produce incorrect ordering in the projection. However, Claude Code conversations are per-session per-file, so cross-file conversations are extremely rare.

  The plan does not address cross-file ordering explicitly. For the single-file-per-conversation case (normal), byte-offset ordering is correct.

- **Flaws found**: cross-file conversation ordering by byte-offset instead of timestamp. Extremely unlikely edge case.
- **Verdict**: PASS (same-file ordering is correct; cross-file is extremely unlikely)

### Edge case: empty file

- **Source** (IMPLICIT_SPEC.md W1 edge case): "File with zero lines -> SourceArtifact created for the file (captures existence), zero containers."
- **Trace through the plan**: The executor reads the file, finds zero lines, creates no observations. The `$$source-artifacts` write happens in step 5g of observation processing — but if there are no observations, step 5g is never reached. So no `SourceArtifact` is created for an empty file.

  The plan has `$$files-handled` (step 5g checks if file was already handled before writing source artifact). But this is inside the observation loop — no observations means no source artifact.

  **FINDING**: The plan does not create a `SourceArtifact` for empty files (no observations to trigger step 5g). The IMPLICIT_SPEC requires "SourceArtifact created for the file (captures existence), zero containers."

  Resolution: the executor should create a file-level observation (or the executor should directly write to a file metadata depot). The plan's current design ties source artifact creation to per-line observation processing, which does not handle the zero-lines case.

- **Flaws found**: empty files do not produce SourceArtifacts.
- **Verdict**: FAIL
- **Required plan change**: Either (a) add an executor-level file-discovery event to a depot that triggers SourceArtifact creation regardless of line count, or (b) have the executor send a sentinel observation for zero-line files, or (c) document that empty files are not tracked (weakens IMPLICIT_SPEC).

### Edge case: file disappears mid-harvest

- **Source** (IMPLICIT_SPEC.md W1 edge case): "File disappears mid-harvest -> error recorded for that file, other files continue."
- **Trace through the plan**: the executor handles this (existing `harvest-transcripts!` behavior). If a file disappears, the executor catches the IOException, records the error (via claim event to `*transcript-claim-depot` or by logging), and continues with other files. The topology is not involved in file-level error handling.
- **Flaws found**: none found (executor responsibility).
- **Verdict**: PASS

### Edge case: re-harvest after source file changed on disk

- **Source** (IMPLICIT_SPEC.md W1 edge case): "Re-harvest after source file changed on disk (same path, different content) -> new SourceArtifact version (different hash); existing containers from old version untouched."
- **Trace through the plan**: If the file's content changed, the `file-id` may still be the same (same inode), but the individual line hashes differ. New lines have new `source-line-key` values (different line-hash). The `$$source-ledger` does not find them, so new containers are created for new/changed lines. Old containers from old lines persist. The `$$source-artifacts` entry is overwritten with updated metadata (last-ingested-at-ms changes).
  - "New SourceArtifact version" — the plan does not version SourceArtifacts. The `SourceArtifactRow` is overwritten with `termval`. There is no version history for source artifacts. The IMPLICIT_SPEC says "different hash" but `SourceArtifactRow` does not have a hash field.

  **MINOR**: The plan's `SourceArtifactRow` does not include a content hash field. The IMPLICIT_SPEC mentions "content hash" for source artifacts. This is a field omission, not a structural problem.

- **Flaws found**: `SourceArtifactRow` lacks a content hash field. Minor — does not affect correctness.
- **Verdict**: PASS

### Edge case: paths expanding to overlapping files

- **Source** (IMPLICIT_SPEC.md W1 edge case): "Paths that expand to overlapping files -> deduplicated by file-id (inode)."
- **Trace through the plan**: the executor uses existing `walk-jsonl-files` which does deterministic sorting. If two glob patterns resolve to the same file (same inode), the executor's file-id-based dedup prevents processing it twice. Even if processed twice, the `$$source-ledger` dedup at the topology level prevents duplicate containers.
- **Flaws found**: none found.
- **Verdict**: PASS

### Edge case: very large file (100K+ lines)

- **Source** (IMPLICIT_SPEC.md W1 edge case): "Very large file (100K+ lines) -> must not exhaust memory. Line-by-line streaming with periodic progress reporting."
- **Trace through the plan**: the executor uses existing `read-jsonl-observations` which reads line-by-line via `RandomAccessFile`. Each line is appended to the depot independently. No full-file buffering. The plan says "The inner loop of container creation per observation is bounded (1 message + 0-5 tool calls), so no explicit yield is needed per event."
- **Flaws found**: none found.
- **Verdict**: PASS

### Edge case: tool-use block with no id field

- **Source** (IMPLICIT_SPEC.md W3 edge case): "Tool-use block with no `id` field -> skip ToolCallContainer creation; record in audit as a malformed tool-use block."
- **Trace through the plan**: the plan does not explicitly describe this case. The existing `parsed-tool-use-blocks` function (from `transcript.clj`) extracts tool-use blocks. If a block has no `id`, the existing parser would produce a nil tool-use-id. The plan's `tool-call/id = "tc:tc:" + conv-key + ":" + sha256(tool-use-id)` would hash nil, producing a deterministic but meaningless ID.
  - The plan should skip container creation for tool-use blocks without an ID, per the IMPLICIT_SPEC. This is parser/executor logic, not topology logic.
- **Flaws found**: executor/parser-level concern, not topology design. The topology would process whatever the executor sends.
- **Verdict**: PASS (delegated to executor/parser)

### Edge case: JSONL line with no conversation ID

- **Source** (IMPLICIT_SPEC.md W3 edge case): "JSONL line with no conversation ID -> fallback to file path as conversation identity."
- **Trace through the plan**: the plan says `conv-key = sha256(source-family + ":" + conversation-id)`. The existing `transcript-conversation-id` function provides permissive fallback extraction. If no conversation ID is found, the fallback uses the file path. The `conv-key` is then `sha256(source-family + ":" + file-path-fallback)`. Deterministic. Correct.
- **Flaws found**: none found.
- **Verdict**: PASS

### Edge case: JSONL line with no message UUID

- **Source** (IMPLICIT_SPEC.md W3 edge case): "JSONL line with no message UUID -> fallback to `(file-id, byte-offset)` as message identity."
- **Trace through the plan**: the plan says `message-uuid-or-fallback: uses transcript-message-uuid extraction; falls back to sha256(file-id + ":" + byte-offset)`. Deterministic. Correct.
- **Flaws found**: none found.
- **Verdict**: PASS

### PRODUCT.md acceptance criterion 1: import Claude Code or Codex transcript

- **Source** (PRODUCT.md acceptance criterion 1): "A Claude Code or Codex transcript source can be imported once."
- **Trace through the plan**: W1 harvest operation covers this. The plan supports both `:claude-code` and `:codex` source families (W1/W2 payload). Correct.
- **Verdict**: PASS

### PRODUCT.md acceptance criterion 2: passive watch

- **Source** (PRODUCT.md acceptance criterion 2): "The same source can be watched passively."
- **Trace through the plan**: W2 watch operation covers this. Same depot and topology pipeline. But the plan lacks watch-specific infrastructure (byte-offset tracking PState, watch orchestration function). Covered by the W2 FAIL finding above.
- **Verdict**: FAIL (same as W2 finding — missing byte-offset tracking)

### PRODUCT.md acceptance criterion 3: import and watch produce same containers

- **Source** (PRODUCT.md acceptance criterion 3): "Import and watch produce the same native container types and edge types."
- **Trace through the plan**: both use the same `*transcript-obs-depot` and same topology processing. Same container kinds, same edge types.
- **Verdict**: PASS

### PRODUCT.md acceptance criterion 5: stable identity across reruns

- **Source** (PRODUCT.md acceptance criterion 5): "Conversation/message/tool-call/tool-result identity is stable across reruns."
- **Trace through the plan**: deterministic IDs from source-provided identifiers. Same input -> same IDs.
- **Verdict**: PASS

### PRODUCT.md acceptance criterion 6: no duplicate native objects

- **Source** (PRODUCT.md acceptance criterion 6): "Duplicate source records do not create duplicate native objects."
- **Trace through the plan**: `$$source-ledger` dedup + `termval` overwrites. Correct.
- **Verdict**: PASS

### PRODUCT.md acceptance criterion 7: source anchors resolve

- **Source** (PRODUCT.md acceptance criterion 7): "Source anchors resolve from native objects back to source file/line/offset/hash."
- **Trace through the plan**: `$$source-anchors-by-container[container-id]` stores `SourceAnchorRow` with file-id, file-path, byte-offset, byte-length, line-hash. R4 includes source anchor. Correct.
- **Verdict**: PASS

### PRODUCT.md acceptance criterion 8: parse errors visible

- **Source** (PRODUCT.md acceptance criterion 8): "Parse errors are represented and auditable, not dropped."
- **Trace through the plan**: parse-error observations create audit entries and projection entries with error markers. Correct.
- **Verdict**: PASS

### PRODUCT.md acceptance criterion 10: existing parsing behavior preserved

- **Source** (PRODUCT.md acceptance criterion 10): "Existing Claude/Codex JSONL parsing behavior from the transcript module is preserved or deliberately replaced."
- **Trace through the plan**: the plan says "extends (not replaces) the existing transcript.clj" and "reuses the parser/acquisition functions." Existing tests in `dogfood_transcript_test.clj` continue to pass. Correct.
- **Verdict**: PASS

### PRODUCT.md acceptance criterion 11: no Space dependency

- **Source** (PRODUCT.md acceptance criterion 11): "No dependency on Space is required to ingest transcripts."
- **Trace through the plan**: no Space references anywhere.
- **Verdict**: PASS

### PRODUCT.md acceptance criterion 12: Space can consume later

- **Source** (PRODUCT.md acceptance criterion 12): "Space can later consume/mount the resulting containers without re-parsing."
- **Trace through the plan**: containers are stored in `$$containers-by-id` with durable IDs, composition edges in `$$composition-edges-by-parent`. A future Space module could mirror these PStates. The plan does not couple containers to Space, but containers are externally queryable (not private). Correct.
- **Verdict**: PASS

### Missing entity: RunOrSessionContainer (agent-run)

- **Source** (PRODUCT.md Product Ontology): "RunOrSessionContainer: kind `:agent-run` or `:chat-session`. Seeded when the transcript source distinguishes a run/session from a conversation."
- **Source** (IMPLICIT_SPEC.md Entities): "ObjectContainer (6 kinds: `:chat-conversation`, `:chat-message`, `:tool-call`, `:tool-result`, `:agent-run`, `:chat-artifact`)"
- **Trace through the plan**: The plan's `TranscriptContainerRow.container-kind` lists 5 kinds: `:chat-conversation`, `:chat-message`, `:tool-call`, `:tool-result`, `:chat-artifact`. The IMPLICIT_SPEC lists 6 kinds including `:agent-run`. The plan does not mention `:agent-run` containers.
  - In Claude Code JSONL, there is no clear distinction between "run" and "conversation." A session IS a conversation. The `:agent-run` kind may be relevant for Codex (which has explicit runs), but for the first slice, conflating run with conversation is reasonable.
  - However, the IMPLICIT_SPEC explicitly lists `:agent-run` as an entity kind. The plan omits it without explanation.
- **Flaws found**: `:agent-run` container kind listed in IMPLICIT_SPEC but absent from plan.
- **Verdict**: PASS (marginal — the omission should be documented as a deliberate scope reduction, but it does not affect the correctness of the implemented kinds)

### Missing edge type: replies-to

- **Source** (PRODUCT.md Minimum useful edges): "message replies-to parent message, when present."
- **Source** (IMPLICIT_SPEC.md CompositionEdge types): "`:replies-to` — message replies to parent (when present in source)."
- **Trace through the plan**: The plan's `CompositionEdgeRow.edge-type` lists: `:contains`, `:follows`, `:produced`, `:anchored-to`. Missing: `:replies-to`. Also missing from PRODUCT.md edges: `:derived-from` (artifact derived-from source message/tool result).
  - Claude Code JSONL does not have explicit reply-to relationships (messages are sequential, not threaded). The `:replies-to` edge is relevant for threaded conversations (future source formats). Omitting it for this slice is reasonable.
  - `:derived-from` edges for artifacts — the plan does not create `:chat-artifact` containers robustly (it depends on the source format having addressable artifact events). Omitting `:derived-from` for the first slice is reasonable.
- **Flaws found**: missing edge types (`:replies-to`, `:derived-from`). Not needed for Claude Code/Codex JSONL sources.
- **Verdict**: PASS (reasonable scope reduction for first slice)

## Summary of Findings

### FAIL items

1. **W2 watch — missing byte-offset tracking PState** (lines W2 analysis above). The plan has no PState for storing per-file byte offsets for watch resume. This violates IMPLICIT_SPEC W2 ("resume from last recorded byte offset") and PRODUCT.md acceptance criterion 2 ("watch resumes from durable file/offset state where possible"). Required fix: add a `$$file-offsets` PState or equivalent.

2. **SourceArtifactRow.ingested-by-requests — non-idempotent under multi-request** (constraint: source immutability analysis above). `termval` overwrite loses previous request IDs. Required fix: remove the field and derive from `$$audit-entries`, or use a separate additive structure.

3. **Empty file — no SourceArtifact created** (edge case: empty file analysis above). The plan ties source artifact creation to per-line observation processing. Zero-line files never trigger step 5g. Required fix: add a file-discovery event or sentinel observation.

### Items that passed with notes

- Progress counts in `$$ingest-runs` are non-idempotent under retry (documented and accepted — exact counts in `$$audit-entries`).
- Duplicate message UUID across files creates duplicate projection entries (extremely unlikely, authoritative store is correct).
- `SourceArtifactRow` lacks content hash field (minor field omission).
- `(source-family, file-id)` lookup for R6 not supported (minor, not hot path).
- `:agent-run` container kind omitted (should be documented).
- `:replies-to` and `:derived-from` edge types omitted (reasonable scope reduction).
- Watch orchestration function missing from Phase 3 checklist.
- Cross-file conversation ordering uses byte-offset instead of timestamp (extremely unlikely edge case).

PHASE_VALIDATION:fail
