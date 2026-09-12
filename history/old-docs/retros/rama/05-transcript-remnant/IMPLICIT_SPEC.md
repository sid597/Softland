# Implicit Spec

<!-- Phase 0. Fill in before starting Phase 1 (Plan). After completing this, fill in PLAN.md. -->

> **RETROSPECTIVE artifact (2026-06-11).** The Transcript Kernel was built (May 2026) before the
> phased Rama skill existed. This is the Phase 0 artifact it never had, reconstructed from:
> (1) `docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md` (the May
> user-facing spec), (2) the module identity header, and (3) the public contract exercised by
> `test/app/server/rama/dogfood_transcript_test.clj`. Source code was NOT read. Scope is the
> ORIGINAL May-2026 acquisition layer: watch/harvest external CLI transcript files, parse,
> redact, dedup against a line ledger, index conversations and tool calls. June-era additions
> are listed under "Out of May Scope" and not specified here.

**Domain in one line:** external chat history (Claude Code / Codex JSONL transcript files on the
user's disk) becomes indexed, deduplicated, redacted source material — and the system that does
this is a *passive observer*: it never participates in the conversation, never mutates the
watched files, and never triggers downstream actions.

## System-Wide Invariants

These hold across every operation below; individual operations reference them by number.

- **I1 — Observation-only.** No operation may cause external state changes: no subprocess spawn,
  no API calls, no modification/move/rename/delete of files in the watched paths, no direct
  downstream triggers. Capture reads files and records observations, period.
- **I2 — Nothing persists unredacted.** Redaction runs before persistence under the request's
  policy (`:off` / `:standard` / `:strict`). This applies uniformly to parsed rows AND
  parse-error rows: a malformed line persists only its hash, byte length, redacted best-effort
  preview, and error kind — never raw bytes. Redaction events are recorded as metadata
  (span/kind/length) that answers "what was masked?" without revealing what was masked. The
  original unredacted content is not persisted anywhere, ever. (Tested: a secret string placed
  in a `tool_use` input must not appear in any readable projection — conversation or tool-call.)
- **I3 — Source-record identity.** Two observed lines are "the same record" iff they agree on
  the composite `(source, file-id, byte-offset, line-hash)`. File identity is physical
  (device+inode), surviving renames. Byte offset is where the line starts in the file. Line
  hash is over the raw line bytes, defending against in-place mutation at an existing offset.
  Conversation-id and message-uuid are grouping/secondary indexes, NOT identity.
- **I4 — Logs record, folds deduplicate.** The observation log is append-only; duplicate appends
  with the same I3 identity are expected (re-harvest, harvest∥watch overlap, redelivery after a
  crash) and MUST collapse to one materialized record in every derived view. Idempotency is a
  property of materialization, not of ingestion.
- **I5 — Byte-correct, newline-gated consumption.** A line is consumed only once its terminating
  newline is present. A trailing partial line must not be emitted, must not become a parse
  error, and must not advance the file cursor past itself; when the remainder arrives, exactly
  one observation for the completed line is emitted. Offsets are byte-accurate (multi-byte
  UTF-8 content must not skew them), and re-reading from a recorded offset must yield
  byte-identical content so line hashes re-match and dedup (I4) holds across redelivery.
- **I6 — Build/persist boundary.** Parsing and observation-building are pure: building
  observations from a file materializes nothing. Only explicit appends change observable state.
  (Tested: after building observations for a file, run status is absent and conversations are
  empty; after appending the request, run = `:pending`; after appending one observation, the
  conversation materializes.)
- **I7 — Monotonic run lifecycle.** Per request-id: absent → `:pending` → `:running` →
  (`:complete` | `:failed` | `:cancelled`). No state regression; terminal states are final.
- **I8 — Fail-isolated, not fail-fast.** A bad line doesn't kill a file; a bad file (perms,
  deletion mid-read) doesn't kill a run. Failures are recorded in the audit ledger and
  processing continues with the remainder.
- **I9 — Already-emitted observations are never retracted.** Rows from a file later deleted by
  the user remain: they are observations of state that existed, not claims about state that
  exists. (User-driven deletion gets a ledger tombstone, a June-adjacent nicety; the
  non-retraction requirement itself is May.)

## Operations

### 1. Submit harvest request (`:transcript/harvest`)

Bulk historical import: walk the configured source paths, parse every `*.jsonl` file found to
its current end, emit one observation per line, finish with a terminal envelope reporting
counts. Request carries: request-id (UUID), source (`:claude-code` in A.0), paths (default
`~/.claude/projects/`), optional `:since` time floor (defaults to epoch), redaction policy
(default `:standard`), and triggered-by attribution. The submitter can obtain a completion
result including `:observations-appended`.

- **Latency:** Acceptance (request visible as `:pending`) sub-second to ~1s — the user/UI polls
  for it. Completion is batch-grade: seconds for test-sized inputs, minutes acceptable for the
  real corpus (hundreds of conversations, 10^5–10^6 lines). Nothing downstream needs
  single-digit-ms harvest effects.
- **Throughput:** One-shot per request; volume driven by accumulated Claude/Codex usage history.
  A run is a burst of up to ~10^6 line observations. Requests themselves are rare
  (human-initiated, occasionally re-run).
- **Consistency/correctness invariants:** I1–I9. Specifically: re-running over the same paths
  (same or new request-id) is safe — duplicate appends fold away (I4); the run is restart-safe
  by idempotent rerun (a killed harvest is re-issued from the start, NOT cursor-resumed — A.0
  explicitly excludes executor-side resume); per-run counters must satisfy
  `observed-line-count` = lines processed by THIS run including parse errors, and
  `parse-error-count` ⊆ that. (Tested: 3 lines, 1 malformed → result 3 appended, run shows
  3 observed / 1 parse error, conversation shows 2 messages.)
- **Data growth and scale:** Each run appends O(corpus) observation rows; the materialized
  ledger grows only by previously-unseen line identities. Unbounded over time with usage.
- **Concurrency behavior:** A request-id is claimed/executed at most once even with multiple
  executor instances. Harvest concurrent with a live watch over the same files produces
  duplicate appends — absorbed by I4. Two concurrent harvests over the same paths likewise.
  A file being appended-to during the walk is read to its current end; the partial trailing
  line is skipped per I5 and the next run (or the watch) picks it up.
- **Edge cases:**
  - Empty directory / no `*.jsonl` matches → run completes with zero counts, not an error.
  - Empty file → zero observations, file still ledger-recorded as seen.
  - Non-existent or unreadable path → per-path/per-file failure recorded, run continues with
    the rest (I8); if nothing is processable, the run still reaches a terminal state.
  - File deleted mid-walk → error ledger-recorded, already-emitted rows stay (I9), run continues.
  - File rotated mid-run → some lines may be emitted twice across old/new identity; duplicates
    absorbed (I4); next harvest catches up.
  - Trailing partial JSON line (process killed mid-write) → skipped with a ledger note, not a
    parse error, cursor does not pass it (I5).
  - Malformed line mid-file (invalid JSON) → one parse-error observation (redacted per I2),
    counted in both counters, does NOT abort the file.
  - Unknown event type / schema drift across Claude Code versions → permissive default records
    `:unknown-shape` rows, preserving payload (redacted) for forensics; strict mode rejects.
  - Source version unavailable from the environment → rows tagged `:unknown` version, ingestion
    proceeds.
  - Encoding errors → `:encoding-error` parse-error rows; byte offsets remain accurate (I5).
  - Very long lines (base64 blobs, huge tool_results) → must not break the reader; under
    `:strict` policy they are redaction candidates.
  - `:since` floor: lines older than the floor are excluded (see Ambiguities — which timestamp
    governs is unspecified).
  - Disk full during the run → failure recorded if possible, run becomes `:failed`; rerun after
    space clears is safe (I4).
  - Duplicate submission of the SAME request-id → unspecified by the spec; must not corrupt the
    run record (see Ambiguities).

### 2. Start watch (`:transcript/watch`)

Long-running live capture: register interest in the source paths, then tail new complete lines
into the same observation pipeline. No terminal success state — it runs until cancelled or the
executor restarts. The contract (as tested) exposes a watch handle with a poll step and a stop
control, with a configurable poll interval.

- **Latency:** New lines should become queryable within one poll interval — seconds (tests use
  a 10s nominal interval with manual polling). This is not millisecond streaming; human-pace
  freshness is the bar.
- **Throughput:** Trickle: human conversation pace, ~1–100 lines/min per active session, with
  bursts (many lines/sec) when sessions record `stream_event` partial messages. Multiple
  concurrent sessions multiply this modestly.
- **Consistency/correctness invariants:** I1–I9, plus the cursor contract:
  - **Known files** (lines already in the ledger from a prior harvest/watch) resume from the
    recorded `(file-id, byte-offset)` — already-ingested lines are NOT re-emitted. (Tested:
    after harvesting a 1-message file, watching it adds nothing until new content arrives.)
  - **Existing-but-unknown files** (present at watch start, never ingested) baseline at EOF —
    historic content is NOT captured by watch; only appends after watch start are. (Tested.)
  - **New files** (created after watch start) are read from byte 0. (Tested.)
  - Restart resume: last `(file-id, byte-offset)` per file survives process restart so the
    watch picks up where it left off without re-emitting (and any re-emission that does happen
    is absorbed by I4).
- **Data growth and scale:** Cursor state is O(watched files). Observation growth is the live
  trickle, unbounded over the lifetime of usage.
- **Concurrency behavior:** Watch polling concurrent with the CLI tool appending → the partial
  line rule (I5) is the safety mechanism. Watch concurrent with harvest over the same files →
  duplicates absorbed (I4). One watch request-id runs at most once.
- **Edge cases:**
  - Partial append (no newline yet) → poll emits nothing, conversation counts unchanged; the
    completing append + newline later yields exactly one observation. (Tested with a line split
    at byte 20.)
  - File rotation (rename old, create new): track by inode, finish tailing the rotated file
    under its new path, start the replacement at byte 0.
  - File deletion → stop tailing it; existing rows persist (I9); a file re-created at the same
    path is a NEW file by inode (read from byte 0 as a new file).
  - File truncation / in-place rewrite at a known offset → line-hash mismatch makes the
    rewritten content a different record (I3); must not be silently treated as already-seen.
  - Watcher disconnection (e.g. inotify exhaustion) → detect, re-register, reconcile by
    comparing current size vs last-seen offset per file and tailing the catch-up content;
    log the disconnection so potentially-missed lines can be re-harvested.
  - New directory contents appearing (sessions starting) → picked up as new files.
  - Executor restart while watching → the run does not reach a false terminal state; cursors
    allow resumption (see Ambiguities for what the run STATUS shows post-restart).
  - Zero-byte new file → tracked at offset 0, nothing emitted until content.

### 3. Cancel watch (stop)

Stops a running watch. The run reaches the terminal status `:cancelled`, and that status is
materialized and queryable. (Tested.)

- **Latency:** Status visible within seconds (poll-grade).
- **Throughput:** Rare, human-initiated.
- **Consistency/correctness invariants:** I7 — `:cancelled` is terminal; no observations are
  emitted by this watch after cancellation completes; observations already emitted persist (I9).
- **Data growth and scale:** Negligible (one lifecycle record).
- **Concurrency behavior:** Cancel racing an in-flight poll: the poll may complete and emit its
  lines (acceptable — they're absorbed-or-valid per I4), but no NEW poll starts after
  cancellation. Cancelling twice must be harmless (idempotent terminal write). Cancelling a
  never-started or already-terminal run must not regress state (I7).
- **Edge cases:** Cancel before the first poll → clean `:cancelled`, zero observations. Cancel
  concurrent with executor restart → run must still end terminal, not stuck.

### 4. Record observation (per-line pipeline step: read → parse → redact → append)

The unit write of the whole system: one observed source line becomes one observation record
carrying source discriminator, source version (from harvester environment, `:unknown` if
unavailable), physical file identity, byte offset, raw-line hash, byte length, parsed
conversation-id (when available, else file-basename fallback, flagged), message-uuid (when the
row type carries one), source timestamp (when present), ingest timestamp, event type (incl.
`:parse-error` / `:unknown-shape`), the redacted payload, the redaction list, parse-error kind
(when applicable), host-id, and the originating request-id. The contract also exposes appending
a single built observation directly (tested via the boundary test).

- **Latency:** Materialization into queryable views within seconds of the append (tests poll
  with `await-materialized`). Append itself must not block on downstream view updates.
- **Throughput:** The dominant write — bursts of 10^5–10^6 during harvest, trickle during watch.
- **Consistency/correctness invariants:** I2 (payload is post-redaction, ALWAYS — including
  parse-error previews), I3 (identity fields present and correct), I4 (re-appending the same
  identity changes no materialized counts), I5 (only newline-terminated lines become
  observations), I6 (building ≠ appending). One JSONL line → exactly one observation, even when
  its content contains multiple blocks (a single assistant message with several `tool_use`
  blocks is still one line record; each block indexes separately — see op 8).
- **Data growth and scale:** Unbounded append-only log; the deduplicated ledger grows by unique
  line identity. Per-conversation message lists can reach thousands of entries (long sessions)
  and need ordered range access by conversation; per-line provenance needs point lookup by
  identity.
- **Concurrency behavior:** Appends for the same file arrive in offset order from a single
  reader, but harvest and watch may interleave appends for the same identities — order of
  duplicate arrival must not matter (I4). Appends for different files are independent; no
  cross-file ordering guarantee.
- **Edge cases:** Observation with nil conversation-id (parse errors, files without
  `system/init`) → excluded from conversation views but present in ledger and run counts.
  Observation with nil message-uuid → still ingested (uuid is secondary, I3). Redaction
  producing an empty payload → still a valid record with redaction metadata. Same byte-offset,
  different hash (file mutated) → distinct record (I3).

### 5. Run status progression (claim / progress / terminal lifecycle)

The execution layer reports lifecycle: a submitted request shows `:pending`; the claim flips it
to `:running`; periodic progress updates counters (files-seen / files-parsed / bytes-processed /
messages-emitted for harvest; messages-per-minute / lag / last-rotation for watch); the terminal
event lands `:complete` / `:failed` / `:cancelled`.

- **Latency:** Each transition queryable within ~1s–seconds (UI polls it; tests
  `await-materialized` on `:pending` and `:cancelled`).
- **Throughput:** Low — a handful of lifecycle events per run plus periodic heartbeats.
- **Consistency/correctness invariants:** I7 (monotonic, terminal-final). Counters on the run
  must agree with what the run actually processed (`observed-line-count` includes parse
  errors). A request must be claimed at most once across executor instances.
- **Data growth and scale:** O(runs) — small.
- **Concurrency behavior:** Late/duplicate lifecycle events (redelivery) must not regress
  status: a stale `:progress` arriving after `:terminal` must not flip `:complete` back to
  `:running`. Two executors racing to claim → exactly one wins; the loser skips.
- **Edge cases:** Terminal without claim (malformed history) and claim-after-terminal must not
  produce regressions; a run whose executor died shows the truth available (claimed but no
  terminal — see Ambiguities), never a fabricated `:complete`.

### 6. Read run (`read-run` by request-id)

Returns the run's status and counters (`:status`, `:observed-line-count`, `:parse-error-count`,
progress/liveness counters).

- **Latency:** Interactive — tens of ms; this backs UI status views and test polling loops.
- **Throughput:** Human/UI polling — low QPS, but polled repeatedly while a run is active.
- **Consistency/correctness invariants:** Returns nil/absent for unknown request-ids (tested —
  building observations without submitting must leave `read-run` nil). Never shows a state the
  lifecycle hasn't reached (no phantom `:running` before claim). Eventually consistent with the
  lifecycle writes is acceptable; the visible sequence must still be monotonic (I7).
- **Data growth and scale:** Point lookup by request-id.
- **Concurrency behavior:** Read during active progress may see slightly stale counters — fine;
  it must not see torn state (status from one event, counters from a contradictory one, e.g.
  `:complete` with counters missing the final tally).
- **Edge cases:** Unknown id → nil. Read immediately after submit → nil or `:pending`, never an
  error. Read after cancel → `:cancelled` persists indefinitely.

### 7. Read conversation (`read-conversation` by conversation-id)

Returns the observed messages of one conversation: parsed message rows grouped by
conversation-id, in source order (byte offset within their file), redacted content only.

- **Latency:** Interactive — this is the base for rendering/rehydration; tens of ms for a
  conversation of hundreds–thousands of messages.
- **Throughput:** Human/UI driven; one conversation at a time dominates.
- **Consistency/correctness invariants:** Count equals UNIQUE parsed messages — immune to
  re-harvest (tested: second harvest run leaves the count at 2). Parse-error rows do not
  appear (they carry no conversation-id; tested: 3 lines with 1 malformed → 2 messages).
  Content reflects I2 (no secret may appear; tested via `pr-str` scan for the planted secret).
  Ordering: per-file source order by byte offset.
- **Data growth and scale:** Messages per conversation up to thousands; needs efficient ordered
  range access per conversation-id, not N point lookups.
- **Concurrency behavior:** Reading while a watch appends → sees a prefix of the conversation;
  eventually consistent. Duplicate appends in flight must never make the count exceed the
  unique-message truth.
- **Edge cases:** Unknown conversation-id → empty (tested). Conversation whose file lacked
  `system/init` → grouped under file-basename fallback id, flagged. A conversation spanning
  rotated files (same session id across files) → all messages included; cross-file ordering
  unspecified (see Ambiguities).

### 8. Read tool call (`read-tool-call` by tool-use id)

Returns an observed tool call — at minimum its tool name (`:tool-call/name`) and redacted
inputs — addressable by the tool-use block's id (tested: `tool-1` → name `"bash"`).

- **Latency:** Interactive point lookup, tens of ms.
- **Throughput:** Low; the May motivation is future provenance correlation (matching tool_use
  blocks to git history), so occasional analytical access, plus by-name browsing per the May
  doc's index intent.
- **Consistency/correctness invariants:** I2 — redacted inputs only (tested: planted
  `api_key` value absent). One entry per unique tool-use block; re-harvest must not duplicate
  it (I4). A line containing multiple `tool_use` blocks yields one indexed entry per block
  while remaining one line record (op 4).
- **Data growth and scale:** Grows with tool usage across all history — large but point-lookup
  dominated; by-name grouping wants range access per tool name.
- **Concurrency behavior:** Same as op 7 — eventual visibility after append, dedup under
  duplicate appends.
- **Edge cases:** Unknown tool-use id → nil/absent. Tool-use ids are source-generated; a
  colliding id re-observed via redelivery resolves to one entry; a genuinely different call
  reusing an id is out of the capture layer's control (record per I3 line identity, index
  last-write-deterministically — flag in Ambiguities).

### 9. Audit-ledger query (per-line / per-file / per-run provenance)

The accountability read: for any ingested line — when it was read, what policy applied, how
many redactions fired, where it landed; per harvest run — files-seen / files-parsed /
bytes-processed / messages-emitted; per watch — liveness (messages-per-minute, lag,
last-rotation-at); plus notes for skipped partial lines, per-file failures, watcher
disconnections, and deletion tombstones. Must be able to answer: "what did Softland ingest
from my Claude history last week?" and "what was masked?" — without revealing masked content.

- **Latency:** Interactive-to-analytical; sub-second for per-line/per-file lookups.
- **Throughput:** Occasional, human-driven audit.
- **Consistency/correctness invariants:** Every ingestion event is recorded (the ledger is what
  makes "the world records what its inhabitants do" auditable); redaction history is complete
  per row; the ledger row count over a harvested corpus equals actual source line count
  including parse errors (tested at the conversation/run level; the May done-criteria state it
  for the ledger). Ledger entries are deduplicated by I3 identity.
- **Data growth and scale:** One row per unique source line — the largest materialized
  collection; needs range access per file (offset-ordered) and per request.
- **Concurrency behavior:** Append-side only; same eventual-visibility stance as ops 7–8.
- **Edge cases:** Querying a file that was rotated → both identities (old inode lineage)
  visible; querying a deleted file → rows persist (I9) plus tombstone note; querying a time
  window with zero ingestion → empty, not error.

### 10. Build observations (pure parse, `read-jsonl-observations`)

Parse a file from a byte offset into observation records WITHOUT persisting anything. Exists so
the reading/parsing/redaction logic is exercisable and reusable independent of ingestion.

- **Latency / Throughput:** Bounded by file I/O; no system state involved.
- **Consistency/correctness invariants:** I6 — zero observable side effects (tested: run nil,
  conversation empty afterward). Output records must be byte-correct (I5) and already redacted
  (I2) — a built-but-not-appended record must already be safe to hand around.
- **Edge cases:** Offset beyond EOF → empty. Offset mid-line (caller error) → must not
  fabricate a record whose offset/hash misidentify a real line; behavior should be defined
  (empty or error), never silent corruption of I3 identity.

## Entity State × Write Matrix

Reads used below: `read-run` (R-run), `read-conversation` (R-conv), `read-tool-call` (R-tool),
audit-ledger query (R-ledger). All reads can be called in every state.

### Entity: Run (per request-id)

States: **does-not-exist**, **pending**, **running**, **complete**, **failed**, **cancelled**.

```
does-not-exist × submit request (harvest or watch)
  - R-run: :pending with zeroed counters — the request is committed intent, not yet executed
  - R-conv: unchanged (no observations exist from this run yet)
  - R-tool: unchanged
  - R-ledger: a request record exists; no ingestion rows for this run yet
does-not-exist × claim / progress / terminal / cancel (orphan lifecycle event, redelivery artifact)
  - R-run: must not fabricate a full run from nothing; either absent or visibly partial — never a clean :complete for a request that was never submitted
  - R-conv: unchanged
  - R-tool: unchanged
  - R-ledger: the stray event may be recorded for forensics; no ingestion attributed
pending × claim
  - R-run: :running; counters still near zero
  - R-conv: unchanged until observations land
  - R-tool: unchanged
  - R-ledger: run marked claimed/started
pending × duplicate submit (same request-id re-appended)
  - R-run: still :pending, one logical run — no duplicate run records, no counter doubling
  - R-conv: unchanged
  - R-tool: unchanged
  - R-ledger: at most one effective request entry (duplicate may be visible as a log fact, not as a second run)
pending × cancel (watch cancelled before first poll)
  - R-run: :cancelled, terminal, zero counters
  - R-conv: unchanged — nothing was ever emitted
  - R-tool: unchanged
  - R-ledger: run terminal with zero ingestion
running × record observation (the run's own appends)
  - R-run: counters grow (observed-line-count incl. parse errors; parse-error-count for malformed)
  - R-conv: parsed rows with a conversation-id appear in that conversation, in source order; parse-error rows do not
  - R-tool: tool_use blocks in the row become readable by tool-use id, redacted
  - R-ledger: one provenance row per unique line identity; redaction counts recorded
running × progress heartbeat
  - R-run: :running with updated counters; never torn (status and counters from a consistent point)
  - R-conv: unchanged by the heartbeat itself
  - R-tool: unchanged
  - R-ledger: progress face updated (files-seen/bytes-processed or liveness)
running × terminal :complete (harvest finished)
  - R-run: :complete with final counters; observed-line-count = lines this run processed incl. parse errors
  - R-conv: stable — every parseable line of the walked corpus is represented exactly once
  - R-tool: stable — every observed tool_use indexed exactly once
  - R-ledger: run closed; per-run totals queryable
running × terminal :failed (disk full, fatal error)
  - R-run: :failed; counters reflect progress up to the failure point
  - R-conv: partial — rows emitted before failure persist (I9); no retraction
  - R-tool: partial, same
  - R-ledger: failure recorded with where ingestion stopped; rerun later is safe (I4)
running × cancel (watch stop)
  - R-run: :cancelled, terminal (tested)
  - R-conv: rows emitted before cancellation persist; count frozen until another run ingests more
  - R-tool: persists
  - R-ledger: cancellation recorded; cursors retained for future resume
running × executor dies (no terminal written — absence, not a write)
  - R-run: stays :running (stale) — must never invent :complete; see Ambiguity A3
  - R-conv: prefix persists
  - R-tool: prefix persists
  - R-ledger: last ingestion row shows where it stopped; idempotent rerun is the recovery path
complete/failed/cancelled × late progress or duplicate terminal (redelivery)
  - R-run: terminal status unchanged — no regression to :running, no terminal flip-flop (I7)
  - R-conv: unchanged
  - R-tool: unchanged
  - R-ledger: late event at most visible as a log fact, attributed to the closed run
complete × new submit with NEW request-id over same paths (re-harvest)
  - R-run (new id): independent lifecycle :pending → ... → :complete with its OWN processing counts
  - R-run (old id): untouched
  - R-conv: counts unchanged — all re-observed identities dedup away (tested: still 2 messages)
  - R-tool: unchanged — same entries
  - R-ledger: no new unique-line rows for unchanged files; the new run's activity is recorded
```

### Entity: Source-line record (per I3 identity `(source, file-id, byte-offset, line-hash)`)

States: **unobserved**, **materialized-parsed**, **materialized-parse-error**.

```
unobserved × record observation (parsed line)
  - R-ledger: one new provenance row (read-time, policy, redaction count, landing ref)
  - R-conv: +1 message in its conversation if it carries a conversation-id (or basename fallback); positioned by byte offset
  - R-tool: any tool_use blocks become readable by id, redacted
  - R-run: owning run's observed-line-count +1
unobserved × record observation (malformed line → parse error)
  - R-ledger: provenance row with parse-error-kind, hash, byte length, redacted preview — never raw bytes (I2)
  - R-conv: NO conversation entry (no parseable conversation-id; tested)
  - R-tool: no entry
  - R-run: observed-line-count +1 AND parse-error-count +1 (tested: 3/1)
unobserved × partial line present at file tail (NOT an observation — explicitly withheld, I5)
  - R-ledger: at most a "partial line pending/skipped" note; no line row
  - R-conv: unchanged (tested: count stays 1 until newline completes the line)
  - R-tool: unchanged
  - R-run: counters unchanged for this line
materialized-parsed × duplicate append, same identity (re-harvest, harvest∥watch, redelivery)
  - R-ledger: still ONE row for this identity (I4)
  - R-conv: count unchanged (tested)
  - R-tool: entry unchanged
  - R-run: the duplicating run's own processing count may include it; unique-line views must not
materialized-parsed × append at same (file-id, offset) with DIFFERENT hash (in-place mutation/truncate+rewrite)
  - R-ledger: a SECOND row — different identity (I3); both points in the file's history are recorded
  - R-conv: both messages present if both parsed with conversation-ids (the old observation is not retracted, I9)
  - R-tool: entries from both, if any
  - R-run: the observing run counts the new line normally
materialized-parse-error × duplicate append of the same malformed line
  - R-ledger: one row (I4)
  - R-conv: still absent
  - R-tool: still absent
  - R-run: duplicating run's own count only
```

### Entity: Conversation (per conversation-id, incl. file-basename fallback ids)

States: **unknown** (no parsed rows), **populated** (n ≥ 1 messages).

```
unknown × record observation carrying this conversation-id
  - R-conv: 1 message, redacted content, ordered by byte offset (tested via boundary test: single append → count 1)
  - R-ledger: the line's provenance row references the run and file
  - R-tool: populated iff the message contains tool_use blocks
  - R-run: owning run counters include the line
unknown × record observation WITHOUT conversation-id (parse error, init-less fragment)
  - R-conv: stays empty for every id — the row is ledger-visible but conversation-invisible
  - R-ledger: row present (flagged if basename-fallback was also impossible)
  - R-tool: n/a (unparsed rows index no tools)
  - R-run: counted
populated(n) × record observation, new message identity, same conversation
  - R-conv: n+1, inserted in source order (a lower-offset late arrival from another file region must still read in source order, not append order)
  - R-ledger: +1 row
  - R-tool: any new tool_use blocks appear
  - R-run: counted
populated(n) × duplicate append of an already-materialized message line
  - R-conv: still n (tested: re-harvest keeps 2)
  - R-ledger: unchanged
  - R-tool: unchanged
  - R-run: duplicating run's own count only
populated(n) × source file deleted by user (external event; capture writes only a tombstone note)
  - R-conv: still n — observations persist (I9)
  - R-ledger: tombstone recorded; cursors stop
  - R-tool: persists
  - R-run: a running watch stops tailing that file; status unaffected
```

### Entity: Tool-call entry (per tool-use id)

States: **unknown**, **indexed**.

```
unknown × record observation containing a tool_use block with this id
  - R-tool: entry exists with :tool-call/name and redacted input (tested: name "bash", secret absent)
  - R-conv: the carrying message is in its conversation
  - R-ledger: carried by the line's provenance row
  - R-run: line counted once regardless of how many blocks it contains
indexed × duplicate append of the carrying line
  - R-tool: one entry, unchanged (I4)
  - R-conv: unchanged
  - R-ledger: unchanged
  - R-run: duplicating run's count only
indexed × a DIFFERENT line reusing the same tool-use id (source anomaly / cross-conversation collision)
  - R-tool: deterministic single answer — never a merge of two calls' redacted inputs; see Ambiguity A6
  - R-conv: both carrying messages appear in their own conversations
  - R-ledger: both line identities present (I3 distinguishes them even if the index cannot)
  - R-run: both counted
```

### Entity: File cursor / watched file (per physical file-id)

States: **unknown** (never seen), **known-with-offset** (ledger has its lines), **baselined-at-EOF**
(existed at watch start, never ingested), **rotated**, **deleted**, **recreated-at-path** (new inode).

```
unknown × harvest walks the file
  - R-ledger: file recorded; one row per complete line; trailing partial noted, not consumed (I5)
  - R-conv: its parsed messages appear
  - R-tool: its tool calls appear
  - R-run: counters include its lines
unknown (existing at watch start) × watch first poll
  - R-conv: NOTHING from historic content — baseline at EOF (tested: existing-conv empty)
  - R-ledger: file registered with cursor at EOF; no line rows
  - R-tool: nothing
  - R-run: liveness only; no line counts from history
unknown (created AFTER watch start) × watch poll
  - R-conv: messages from byte 0 onward appear (tested: new-conv count 1)
  - R-ledger: rows from offset 0
  - R-tool: as contained
  - R-run: counted
known-with-offset × watch poll, no new bytes
  - R-conv: unchanged
  - R-ledger: cursor unchanged
  - R-tool: unchanged
  - R-run: liveness updated at most
known-with-offset × watch poll, new complete line(s) appended
  - R-conv: +k messages, only the NEW lines — no re-emission of pre-offset content (tested: 1→2)
  - R-ledger: +k rows; cursor advances past consumed newlines only (I5)
  - R-tool: new blocks indexed
  - R-run: counted
known-with-offset × watch poll, partial append (no newline)
  - R-conv: unchanged (tested)
  - R-ledger: cursor does NOT advance past the partial; optional pending note
  - R-tool: unchanged
  - R-run: unchanged line counts
known-with-offset × rotation (rename old, create new at path)
  - R-ledger: old inode keeps identity and is tailed to its end under the new path; replacement file enters as a new file at byte 0
  - R-conv: continuous — no gap, no duplication beyond what I4 absorbs
  - R-tool: continuous
  - R-run: rotation logged (last-rotation-at)
known-with-offset × deletion
  - R-ledger: tombstone; tailing stops
  - R-conv: existing messages persist (I9)
  - R-tool: persist
  - R-run: watch continues on remaining files (I8)
deleted × file recreated at the same path (new inode)
  - R-ledger: treated as a brand-new file (by inode), read from byte 0
  - R-conv: new content appears; old content still present from before
  - R-tool: likewise
  - R-run: counted as new-file ingestion
known-with-offset × watcher disconnection then reconnect (reconcile)
  - R-ledger: disconnection logged; per-file size-vs-offset check; catch-up lines ingested
  - R-conv: missed-window lines appear after reconcile (possibly late, never silently lost without a ledger trace)
  - R-tool: likewise
  - R-run: liveness shows the lag
```

## Out of May Scope (June-era additions — noted, not specified)

The module was half-rewritten in June 2026; these belong to that work and are intentionally NOT
specified above:

- **Object-container materialization** — folding transcript observations into the shared
  object-container model (integration with Space's object world). The May design's terminal
  views are the source ledger, conversation grouping, and tool-call index; object containers
  are the June re-target.
- **Object-container source adapters** (the June "split source adapters" / "common import path
  hardening" work) — the generalized many-source ingestion shape sitting above/behind the
  May Claude-Code-only parser.
- The May doc's own deferred list (rehydration, world registration, continuation, activation,
  full bidirectional provenance, passive-observation abstraction, git capture, strict-policy
  redaction beyond definition, multi-machine/host-id population, peer-Rama, multi-user sync)
  remains deferred and is likewise not specified here.

Note: the tested contract above (harvest, watch offsets, builder boundary, redaction, dedup) is
exercised by the current test file and is consistent with the May design; the test file does not
directly exercise object-container operations.

## Ambiguities Flagged (spec vs tests, and unstated decisions)

- **A1 — Tool-call addressing.** The May doc indexes tool calls by `:tool/name`; the tests read
  by tool-use id (`tool-1`). The tested contract (by-id retrieval) is taken as required;
  by-name browsing is treated as the May intent for future provenance work.
- **A2 — `:since` floor semantics.** Which timestamp the floor filters on (source timestamp vs
  file mtime) and whether filtered lines still get ledger rows is unspecified.
- **A3 — Run status after executor death.** Watch "runs until cancelled or executor restarts";
  the doc never says what `read-run` shows for a watch whose executor died (stuck `:running`?).
  Required behavior asserted here: never a fabricated terminal `:complete`.
- **A4 — Watch EOF-baseline vs the doc.** §5.1 says "on new file, start tailing from line 0";
  the tests distinguish files existing at watch start (baseline EOF, historic content skipped)
  from files created after (byte 0). The tested refinement is taken as the contract; whether
  the silent skip of historic content should be ledger-noted is unspecified.
- **A5 — Duplicate submission of the same request-id.** Unspecified; required here only to be
  non-corrupting (one logical run, no counter doubling).
- **A6 — Tool-use id collisions across distinct lines.** Source-controlled ids could collide;
  the by-id read must stay deterministic, but which record wins is unspecified.
- **A7 — Cross-file conversation ordering.** Ordering is by byte offset within a file; a
  conversation spanning rotated files has no specified cross-file order key.
- **A8 — Per-run counters under re-harvest.** Tests pin dedup of materialized views but never
  read the second run's counters; counters are taken to be per-run processing counts
  (inference, not tested).
- **A9 — Parseable-but-erroring rows with a conversation-id.** The tested parse-error line had
  no conversation-id, so its exclusion from the conversation view is forced, not chosen.
  Whether an `:unknown-shape` row WITH a session id appears in conversation reads is
  unspecified.
- **A10 — Redaction-policy default.** Doc examples always pass a policy; tested requests omit
  it. A.0 default `:standard` is assumed to apply when omitted.
