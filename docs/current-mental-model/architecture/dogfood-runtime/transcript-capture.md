# Transcript Capture — Harvest + Watch
*Status: candidate slice in the dogfood-runtime/transcript-track family. Scope: **capture only.** Rehydration, world registration, continuation, activation, the full bidirectional provenance contract, the broader passive-observation pattern, git capture, and peer-Rama topology are explicitly out of scope here — see §6 for the deferred list.*

*Revision 1 (2026-05-11): Codex review repairs — three-depot shape (request/claim/observation), strict executor-writes-depots-only asymmetry, source-record identity by `(file-id, byte-offset, line-hash)` with UUIDs as secondary indexes, partition by source-file not conversation, parse-error rows redacted-or-hashed never raw, A.0 restart-safe rerun distinguished from future cursor resume, source-version moved to harvester-environment.*

*Related: `llm-track-claude-research.md` introduced the passive-observer pattern at §4.7 and §7.4. This doc is the first concrete slice of that pattern.*

---

## §0 What this doc is — and isn't

**This doc designs two operations:** `:transcript/harvest` (bulk historical import of Claude Code conversation files) and `:transcript/watch` (live tail of new conversation activity), plus the depot, the common pipeline, and the first concrete implementation slice.

**This doc does NOT design:**
- *Rehydration*: turning raw captured records into Softland conversation projections that can be rendered. Deferred.
- *World registration*: registering imported conversations as catalog entries / artifact-graph edges in WorldDepot. Deferred.
- *Continuation*: forking an observed conversation into a new managed run via ContextBundle. Deferred.
- *Activation*: producing slices, comments, derivatives, claims, evidence, or synthesis from observed conversations. Deferred.
- *Bidirectional provenance contract* (full version): the formal contract that every Softland object can trace back to source records and vice versa. Deferred — needs rehydration + world registration to be real first.
- *Passive observation pattern abstraction*: extracting the shared shape across transcript capture, git capture, web capture, etc. Deferred — abstracts from one designed instance is premature.
- *Git capture*: same pattern, different source. Deferred.
- *Peer-Rama instances and multi-user sync*: the eventual story of local Rama on user's machine syncing with server Rama. Deferred — principle noted in `llm-track-claude-research.md` §5.

**Why this scope:** following the slice-A discipline already established for the compute-track (`slice-a-compute-run-command.md`). One vertical capability designed end-to-end before adjacent phases are designed. Capture's depot schema is the contract that adjacent phases will need; getting capture right unblocks all of them without coupling their designs.

---

## §1 The two operations

### §1.1 `:transcript/harvest` — bulk historical import

The user has hundreds of Claude Code conversations already on disk from prior usage. Harvest is a one-shot (per request) operation that walks the configured source paths, parses every JSONL file found, and emits one row per message into `*transcript-obs-depot`. It has a definite endpoint: when every file under the configured paths has been parsed to its current end, the harvest is *complete* and emits a terminal envelope.

Harvest is **idempotent**. Re-running a harvest over the same paths is safe: duplicate appends may exist in `*transcript-obs-depot`, but the source-ledger topology folds them by source-record identity into one materialized row. No special re-ingest dance — re-run is just run.

Harvest is **restart-safe by idempotent rerun** in A.0. If the executor dies partway through, rerunning the harvest from the start is safe (the source-record natural key prevents duplicate materialized rows in derived PStates). **Cursor-resume** — picking up from the last `(file, byte-offset)` per source without re-processing already-emitted records — is a future optimization, not part of A.0. See §4.1 for the explicit A.0 scope.

```clojure
;; Request shape
{:request/type :transcript/harvest
 :transcript/request-id "<uuid>"
 :transcript/source :claude-code
 :transcript/paths ["~/.claude/projects"]
 :transcript/since (optional time floor)   ;; defaults to epoch
 :transcript/redaction-policy :strict      ;; or :standard, :off
 :transcript/triggered-by {:agent :sid}}
```

### §1.2 `:transcript/watch` — live capture

Watch is the long-running sibling. It registers a filesystem watcher on the configured source paths, tails new lines as they appear, and emits them into the same `*transcript-obs-depot`. It has **no terminal state** — it runs until cancelled or until the executor restarts.

Watch must handle:
- New files appearing in watched directories (sessions starting)
- New lines appended to existing files (turns happening)
- File rotation (Claude Code rolling session files)
- File deletion (user clearing sessions)
- Watcher disconnection / re-registration (fs notification stack hiccups)
- Restart resume — last `(file-id, byte-offset)` per file lives in the ledger so a process restart picks up from the same place without re-emitting

```clojure
;; Request shape
{:request/type :transcript/watch
 :transcript/request-id "<uuid>"
 :transcript/source :claude-code
 :transcript/paths ["~/.claude/projects"]
 :transcript/redaction-policy :strict
 :transcript/triggered-by {:agent :sid-daemon}}
```

### §1.3 The observation-only invariant

**Capture never causes external state changes.** This is a hard architectural rule, not a soft guideline.

The executor reads files. The executor writes to depots. The executor does not:
- Spawn Claude Code subprocesses
- Make Anthropic API calls
- Modify the source JSONL files
- Move, rename, or delete files in the watched paths
- Trigger downstream Softland actions directly

If a future contributor proposes "let the watch executor automatically follow up when the user gets stuck," reject it on architecture grounds. That capability belongs to the active llm-track, not here. Capture and action stay separate; the boundary is what makes capture trustworthy.

This invariant connects directly to the OAuth/policy story in `llm-track-claude-research.md` §3.5: capture is *Test A* clean (Anthropic policy) because it's not using OAuth at all, and *Test B* clean (Softland's stricter invariant) because Softland's process never spawns Claude Code or touches credentials. The two tests are satisfied trivially by what capture *doesn't do*.

---

## §2 Common pipeline

Both operations share the same downstream pipeline once a raw line is read off disk:

```
read line ──→ parse ──→ redact ──→ append observation ──→ topology folds ledger
```

### §2.1 Parse

Source-format dependent. For Claude Code (the only source in Slice A), the schema is the JSONL written by `recordTranscript` (defined in `services/sessionStorage.ts`; called from `QueryEngine.ts` per-message and per-system-event). Each line is one of the SDK message types: `system/init`, `assistant`, `user`, `stream_event`, `attachment`, `tool_use_summary`, `result`, etc.

Parsers are versioned per source-version. The Claude Code JSONL schema changes between minor versions (the leaked tree has feature flags `HISTORY_SNIP`, `CONTEXT_COLLAPSE`, `CACHED_MICROCOMPACT` that gate which event types appear). **Source version is not carried in the JSONL itself** — `system/init` carries model, tools, plugins, mcpClients, but not the Claude Code binary version. Capture takes source-version from the **harvester environment** at run start (read `claude --version` once and tag the request); if unavailable, store `:transcript/source-version :unknown` on the rows. Schema-fingerprinting (inferring version from which fields appear) is a future fallback if explicit detection becomes unreliable.

Parse failures don't kill the harvest or watch. A malformed line becomes a `:transcript/observed-event` with `:event-type :parse-error`, **but never with the raw unredacted bytes** — see §2.2 for what parse-error rows actually persist.

### §2.2 Redact

Transcripts contain everything the user told Claude — including, sometimes, secrets pasted inline, API keys, credentials, private data. Redaction runs before persistence.

Three policies:

| Policy | Behavior |
|---|---|
| `:off` | No redaction. For environments where the user explicitly accepts the risk (e.g. trusted local dev with no shared Rama) |
| `:standard` | Pattern-match common secret shapes: AWS keys, GitHub tokens, JWT prefixes, `sk-*` Anthropic API keys, etc. Replace with `<REDACTED:type:length>` markers preserving length metadata for citation |
| `:strict` | `:standard` plus heuristics: long high-entropy strings, base64 blobs over a threshold, file contents inside tool_result blocks. False-positive rate higher; tradeoff toward safety |

Redactions are recorded on the observation row itself (`:transcript/redactions`) so the audit ledger can answer "what was masked?" without revealing what was masked. The original unredacted content is **not** persisted anywhere by capture; if the user wants reversibility, that's a different (and harder) story, deferred.

**Parse errors apply the same rule.** A malformed line is not exempt from redaction just because it didn't parse. Parse-error rows persist:
- `:source/line-hash` — cryptographic hash of the raw line for re-correlation if needed
- `:source/byte-length` — original byte length
- `:transcript/redacted-preview` — the line passed through the configured redaction policy as best-effort text (may be partial if encoding is broken)
- `:transcript/parse-error-kind` — `:invalid-json`, `:unknown-event-type`, `:encoding-error`, etc.

Never the raw unredacted bytes. The redaction invariant applies uniformly: nothing leaves the executor unredacted.

### §2.3 Audit ledger

A PState that records every ingestion event for accountability. Per request, per file, per row:

- When was it read
- What policy applied
- How many redactions fired
- Where it landed in the depot
- (For harvest) progress: files-seen / files-parsed / bytes-processed / messages-emitted
- (For watch) liveness: messages-per-minute / current-lag / last-rotation-at
- On user-driven deletion: a tombstone record

The ledger is what makes the "world records what its inhabitants do" claim auditable rather than aspirational. A user asking "what did Softland ingest from my Claude history last week?" gets a precise answer.

---

## §3 Depots, PStates, and the executor/topology asymmetry

This section is the contract that the executor and the topologies have to honor. The dogfood-runtime invariant: **executor writes depots only; topologies (and only topologies) write PStates.** Capture follows this strictly.

### §3.1 The three depots

```
*transcript-depot           — incoming harvest/watch requests
*transcript-claim-depot     — executor claims, lifecycle events for a run
*transcript-obs-depot       — source records, parse events, progress, terminal/failure observations
```

| Depot | Writer | Partition | Contents |
|---|---|---|---|
| `*transcript-depot` | Request ingress (user/system action after edge validation) | `:transcript/request-id` | Validated `:transcript/harvest` / `:transcript/watch` requests; one append per request |
| `*transcript-claim-depot` | Executor | `:transcript/request-id` | Executor's lifecycle for one request: `:claim`, `:progress` (periodic counters), `:terminal`, `:failure`. Co-locates a run's lifecycle on one task |
| `*transcript-obs-depot` | Executor | `[:transcript-source-file :source/file-id]` | One append per ingested source record. Partitioned by source file so file-tail ordering is local; cross-file queries fan out |

The naming follows the existing dogfood-runtime convention (`*compute-*-depot`, `*llm-obs-depot`). Whether the transcript-track becomes a canonical "fourth depot family" alongside World/Compute/LLM, or is folded into the LLM-track family, is **explicitly not decided in this doc** — the depots are a proposal that lands their rows correctly regardless of that family-membership question.

### §3.2 The PStates (all topology-fold derivatives)

Topologies fold depots into PStates. The executor does not touch PStates directly.

| PState | Folded from | Keyed by | Purpose |
|---|---|---|---|
| `$$transcript-runs` | `*transcript-depot` + `*transcript-claim-depot` | `:transcript/request-id` | Per-run status: `:pending`, `:running`, `:complete`, `:failed`, with progress counters |
| `$$transcript-source-ledger` | `*transcript-obs-depot` | `[:transcript/source :source/file-id :source/byte-offset :source/line-hash]` | Idempotent ledger: per-source-record provenance, redaction history, ingest timestamps. The "audit ledger" of §2.3 is the queryable face of this PState |
| `$$transcript-observed-conversations` | `*transcript-obs-depot` | `:transcript/conversation-id` (parsed from rows where available) | Conversation-grouped view: messages per conversation, ordered by `:source/byte-offset`. The base for downstream rehydration |
| `$$transcript-tool-call-index` | `*transcript-obs-depot` | `:tool/name` | Tool calls observed, indexed by tool name. Future bidirectional-provenance correlation (with `git-track`) uses this |

If a depot says "X happened," a PState answers "what is true now." Topologies are the only place that distinction collapses. Both harvest and watch generate the same observation shapes, so the same topologies fold both.

### §3.3 Observation-row schema (`*transcript-obs-depot`)

```clojure
{:transcript/source          :claude-code                  ;; source discriminator
 :transcript/source-version  "2.1.128" | :unknown          ;; from harvester env, see §2.1
 :source/file-id             {:device 64513 :inode 9123445};; stable physical identity
 :source/file-path           "~/.claude/projects/<workspace>/<session>.jsonl"
 :source/byte-offset         4287                          ;; where the line started in the file
 :source/line-hash           "blake3:..."                  ;; hash of the raw line bytes
 :source/byte-length         842                           ;; length of the raw line
 :transcript/conversation-id "<source-conv-id>" | nil      ;; parsed from system/init when available
 :transcript/message-uuid    "<source-msg-uuid>" | nil     ;; secondary index, present on most row types
 :transcript/source-timestamp "2026-05-09T13:42:11Z"       ;; from JSONL when present
 :transcript/ingest-timestamp "2026-05-11T09:00:00Z"       ;; when the executor wrote this row
 :transcript/event-type      :assistant-message            ;; or :user-message, :tool-use,
                                                           ;; :system-init, :stream-event,
                                                           ;; :result, :parse-error, etc.
 :transcript/redacted-payload {...parsed JSONL after redaction...}  ;; never raw
 :transcript/redactions      [{:span ... :kind ... :length ...}]
 :transcript/parse-error-kind nil | :invalid-json | :unknown-event-type | ...
 :transcript/host-id         "sid-laptop"                  ;; for multi-machine future
 :transcript/ingest-request-id "<request-uuid>"}           ;; which harvest/watch run
```

### §3.4 Source-record identity and idempotency

**Primary identity** (what makes two appends "the same record"):

```clojure
[:transcript/source
 :source/file-id
 :source/byte-offset
 :source/line-hash]
```

The composite is the physical source-record identity. `file-id` survives renames (inode-based); `byte-offset` is the position in the file; `line-hash` defends against the rare case of in-place file mutation at an existing offset (rotation, truncation, careful overwrite). Together they uniquely identify one line of one file at one point in its history.

**Secondary indexes** (used for grouping, not identity):
- `:transcript/conversation-id` — derived from `system/init` row when available, fallback to file basename. Used by `$$transcript-observed-conversations`.
- `:transcript/message-uuid` — from the JSONL row's own uuid field; present on most row types (assistant, user, stream_event, attachment, etc.) but not guaranteed for all. Used for rehydration's message-level addressing.

**Idempotency is a topology property, not a depot property.** The `*transcript-obs-depot` is an append-only log. Multiple appends with the same primary identity may exist in the log. `$$transcript-source-ledger` folds the depot deterministically by primary identity, so duplicate appends do not produce duplicate materialized rows. Re-running harvest over a directory the watch has already streamed is safe in this sense — duplicate appends happen and are filtered at the fold.

This corrects a common Rama anti-shape: "depot deduplicates" is wrong because depots are logs. Topologies deduplicate; depots record.

### §3.5 Designed for rehydration without redesign

Even though rehydration is out of scope here, the depot+PState shape is designed so that rehydration can be a pure-read derivation later. Specifically:

1. **`:transcript/redacted-payload` preserves the JSONL row's parsed structure** (after redaction). Rehydration can extract whatever sub-structure it needs without re-parsing the source file.
2. **Per-line granularity** — one depot row per JSONL line. Rehydration can stream rows for a conversation in source order.
3. **`source-timestamp` preserved** — chronological reconstruction is direct.
4. **`tool_use` blocks preserved in `redacted-payload`** — future correlation with git history (the deferred bidirectional code↔conversation provenance) can match by tool inputs.
5. **`:transcript/source-version`** — rehydration can pick the right interpreter per Claude Code version, even when version is `:unknown` (in which case rehydration falls back to permissive parsing).
6. **`$$transcript-observed-conversations` is already grouped** — rehydration consumes this PState rather than re-grouping from raw rows.

Rehydration will land as a separate slice that reads from these depots/PStates and writes derived PStates of its own. No back-edits to capture required.

---

## §4 First implementation slice — Claude Archive Import A.0

The concrete first target. Following the slice-A pattern from `slice-a-compute-run-command.md`: one capability, one source, one executor, one happy path proven end-to-end.

### §4.1 Scope

- **Operation**: `:transcript/harvest` only (no watch in A.0)
- **Source**: `:claude-code` only
- **Paths**: a single user-supplied directory, default `~/.claude/projects/`
- **Redaction**: `:standard` policy
- **Result**: every parseable JSONL row in the source paths lands in `*transcript-obs-depot`; the audit ledger records the run; a terminal `:result` envelope reports counts

Out of scope for A.0 (deferred to A.1+):
- `:transcript/watch`
- Multiple sources
- Multi-machine (`:transcript/host-id` field exists but populated from a single config value)
- Strict-policy redaction (start with standard; strict added once standard is proven)
- Resume from a previous interrupted harvest (re-running from the start is safe by idempotency; explicit resume is a later optimization)

### §4.2 Topology (strict executor/topology asymmetry)

Mirrors compute-track's slice A. Four moving parts; the boundaries between them are load-bearing.

**Part 1 — Request ingress (appends `*transcript-depot`).**
Receives `:transcript/harvest` requests from the outside world (user action, system trigger), applies edge validation to the request shape, and appends a single validated row to `*transcript-depot` keyed by `:transcript/request-id`. Request ingress does **not** spawn the executor, does **not** touch the filesystem, and does **not** write any PState. It commits intent into a depot, period. (rama-pitfalls #2: keep external work out of stream events.)

**Part 2 — Status-fold topology (writes `$$transcript-runs`).**
Folds `*transcript-depot` (request appends) and `*transcript-claim-depot` (executor lifecycle) into `$$transcript-runs`. Materializes per-request status: a request appended to `*transcript-depot` shows as `:pending`; the matching `:claim` from the executor flips it to `:running`; subsequent `:progress` events update counters; the `:terminal` event flips to `:complete` or `:failed`. This is the only place run-status is written.

**Part 3 — Executor (TaskGlobalObjectWithTick, writes `*transcript-claim-depot` and `*transcript-obs-depot`).**
Reactively reads the `$$transcript-runs` PState, sees runs in `:pending`, claims them by appending `:claim` to `*transcript-claim-depot`. Walks the configured source paths, opens each file, reads line-by-line, parses, redacts, and appends to `*transcript-obs-depot` (one append per source line). Periodically appends `:progress` rows to `*transcript-claim-depot` (counter heartbeats). On completion or failure, appends a `:terminal` row to `*transcript-claim-depot`. Uses a spawn-registry pattern: each `:transcript/request-id` is claimed at most once (multiple executor instances see the claim and skip).

**The executor never writes a PState. Ever.** Run status, source ledger, conversation grouping, tool-call index — all PStates — are folded by topologies from depots. This is the asymmetry that makes the system event-sourced; violating it is the most common dogfood-runtime anti-pattern.

**Part 4 — Observation-fold topologies (write `$$transcript-source-ledger`, `$$transcript-observed-conversations`, `$$transcript-tool-call-index`).**
Fold `*transcript-obs-depot` into the three observation PStates. `$$transcript-source-ledger` is the dedup-and-provenance layer (keyed by `(source, file-id, byte-offset, line-hash)`, idempotent). `$$transcript-observed-conversations` groups rows by `:transcript/conversation-id`. `$$transcript-tool-call-index` indexes tool-use rows by `:tool/name`.

**Diagram**:

```
User/system action
       │
       ▼ append
*transcript-depot ───────── fold ──────────┐
       │                                   │
       ▼ read PState                       ▼
   Executor ─── append :claim ────► *transcript-claim-depot ─── fold ──┐
       │                                                                │
       │ (does the actual work)                                         │
       │                                                                ▼
      ├── append ──► *transcript-obs-depot
      │                       │
      │                       ├── fold ──► $$transcript-source-ledger
       │                       │            (idempotent dedup + audit)
       │                       │
       │                       ├── fold ──► $$transcript-observed-conversations
       │                       │            (grouped by conv-id)
       │                       │
       │                       └── fold ──► $$transcript-tool-call-index
       │
       ▼ append :progress / :terminal ──► *transcript-claim-depot
                                          (continues into status fold above)
```

**Back-arrow rule**: depot writes flow into Rama; UI reads PStates from Rama; the executor does not push to UI directly.

### §4.3 Claude Code JSONL specifics

Path discovery: walk `:transcript/paths` recursively, match `*.jsonl`. For each file:

- The first line is typically `{"type":"summary",...}` or `{"type":"system","subtype":"init",...}` — used to extract conversation metadata (model, session_id).
- Subsequent lines are messages: `user`, `assistant`, `result`, optionally `stream_event` rows if the session was recorded with partial messages.

Conversation-id derivation: from `system/init`'s `session_id` field. If the file lacks an init event (older or interrupted sessions), fall back to the file's basename minus extension. Files without a clean conversation-id are still ingested but flagged.

Tool calls: their parsed structure is preserved in `:transcript/redacted-payload` after redaction. The deferred bidirectional provenance work will eventually correlate tool_use blocks with actual filesystem changes (git history); A.0 just needs to preserve them.

### §4.4 Done criteria

- A.0 ships when the user can issue one `:transcript/harvest` request (one append to `*transcript-depot`) and:
  - Observe `$$transcript-runs` flip from `:pending` → `:running` → `:complete` for that `:transcript/request-id`
  - Query `$$transcript-source-ledger` and see N rows where N is the actual line count across the source JSONL files (parse errors included)
  - Query `$$transcript-observed-conversations` and see the conversations grouped, each with messages in source order
  - Read the audit-ledger face of `$$transcript-source-ledger` to verify what was redacted and where
- Re-issuing the same `:transcript/harvest` request produces a new `:transcript/request-id` but does not double-count rows in `$$transcript-source-ledger` (idempotency at the source-record level holds via the natural-key fold).
- Restarting harvest from scratch (after a kill) re-walks source files; duplicate appends to `*transcript-obs-depot` are absorbed by `$$transcript-source-ledger`'s idempotent fold. **Cursor resume** (skip already-ingested rows on the executor side, not just at the fold) is deferred.
- No PState write from the executor (verifiable by reading the topology code — executor only appends to depots).

---

## §5 Failure modes

Capture-only failures. Failures in rehydration, world registration, etc. are out of scope.

### §5.1 File rotation mid-capture

Claude Code may rotate session files (rename old, create new). Watch must:
- Track files by inode where possible, not just path
- On rename, continue tailing the rotated file under its new path until it's fully read
- On new file, start tailing it from line 0

Harvest is simpler: it walks once. If a file is rotated during a harvest run, the next harvest catches up; the in-progress run may emit some lines twice. Duplicate appends remain in the depot log and are absorbed by the source-ledger fold.

### §5.2 Schema drift across Claude Code versions

The `:transcript/source-version` field is recorded per row. Older transcripts (pre-feature-flag) lack fields that newer ones have. The parser is versioned; on a version it doesn't recognize, it falls back to a permissive parser that records the row with `:event-type :unknown-shape` and leaves the payload as-is.

Strict mode rejects unknown shapes. Permissive mode (default) preserves them for later forensic recovery.

### §5.3 Partial JSON writes

A Claude Code process killed mid-write can leave a trailing partial JSON line. The parser tolerates this: trailing partial lines are skipped (with a ledger note) until the file gets a newline or a clean next-record.

### §5.4 Disk full

The executor's depot appends can fail if the disk fills. The executor appends a failure row to `*transcript-claim-depot` when possible and stops; if even that append fails, the absence of further claim/observation rows is visible once Rama catches up. The audit ledger's last materialized source record records where ingestion stopped. Re-running after the user clears space is safe by the source-ledger fold.

### §5.5 Permission errors

If a file in a watched directory becomes unreadable (perms change, ACL update), watch logs to the ledger and continues with the remaining files. Harvest treats per-file failures the same way — fail-isolated, not fail-fast.

### §5.6 File deletion

User deletes a session file during harvest: the in-progress walk gets an error on that file, ledger-records it, continues. Rows already emitted from that file stay in the depot (they're observations of state that existed, not claims about state that exists).

Watch: file deletion stops tailing that file. Existing rows persist. Future re-creation of a file at the same path is treated as a new file by inode.

### §5.7 Watcher disconnection

OS-level filesystem watchers can fail (e.g., inotify limit exhausted on Linux). Watch must:
- Detect disconnection (the watcher's event stream ends or errors)
- Re-establish the watcher
- Reconcile state on reconnect: for each watched file, check current size vs last-seen offset, tail any catch-up content

Disconnection-reconnection is logged in the ledger so missed lines (if any) can be retroactively re-harvested.

---

## §6 Deferred (short local list)

Everything listed below is intentionally **not** designed in this doc. Each item gets its own slice/doc when intent matures.

- **Rehydration** — turning raw depot rows into Softland conversation projections. Needs its own slice that reads `*transcript-obs-depot` and produces derived PStates (e.g., `*conversation-index`, `*message-stream`).
- **World registration** — registering rehydrated conversations as catalog entries / artifact-graph edges in WorldDepot. Needs the bidirectional provenance pointers wired through.
- **Continuation** — forking an observed conversation into a new managed run via ContextBundle. This crosses tracks (passive transcript → active llm), so it belongs to a doc that designs the bridge, not here.
- **Activation** — slices, comments, derivatives, claims, evidence, synthesis. A category of downstream uses, each its own slice.
- **Bidirectional provenance contract (full)** — the formal "every Softland object can trace back to source records, and vice versa" claim. Requires rehydration + world registration to govern. Not contract-shaped until then.
- **Passive observation pattern abstraction** — the family-level pattern that transcript-capture and (future) git-capture both instantiate. Abstracts from one designed instance is premature; the pattern emerges when there's a second concrete instance to cross-check.
- **Git capture** — same shape, different source: `:git/harvest` and `:git/watch` over user repos. Enables the bidirectional code↔conversation provenance use case. Deferred until transcript-capture is real.
- **Strict-policy redaction** — heuristic-based redaction beyond pattern matching. Add after standard-policy is proven and the false-positive cost is understood.
- **Multi-machine ingestion** — user has Claude on laptop and desktop, both ingesting into one Rama. The `:transcript/host-id` field is reserved for this; the actual sync story belongs to a later slice.
- **Peer-Rama instances** — local Rama on user's machine syncing bidirectionally with server Rama. Captured as a principle in `llm-track-claude-research.md` §5 (deferred principles).
- **Multi-user sync / collaboration** — many users in one Softland world. CRDT-flavored merge, conflict resolution. Far future.

Resist the urge to design any of these into this doc to "make it complete." Capture is one slice. Each deferred item is its own slice when its time comes.

---

## §7 Cross-references

- `llm-track-claude-research.md` — Where the passive-observer pattern was first introduced (§4.7) and where the active/passive auth boundary is articulated (§3.5). Also where peer-Rama and multi-user-sync are captured as deferred principles (§5).
- `llm-track-canonical.md` — The Codex side of the LLM track. Capture is sibling to that doc's active execution flows, not a replacement.
- `three-depot-current-system.md` — The current canonical depot model (World/Compute/LLM). Whether `*transcript-obs-depot` joins as a fourth family or is folded into LLM is out of scope here.
- `slice-a-compute-run-command.md` — Pattern reference for the executor shape (TaskGlobalObjectWithTick, request ingress commits intent, spawn registry, back-arrow rule).
- `rama-pitfalls` skill — Especially "PState ownership", "side-effect retry trace", and "partitioner = event boundary" all apply directly here.

---

## §8 What this slice unblocks once it ships

- The user can issue one harvest request and have hundreds of existing Claude conversations land in Rama. Nothing about the user's Claude usage changes — they keep using Claude normally; Softland now has a record.
- The depot rows are a stable substrate for everything deferred. Rehydration, world registration, continuation can all be designed as separate slices reading from this depot, without re-doing capture.
- The audit ledger is a precedent for how all passive observation should record its own activity. When git-capture is designed, the ledger shape generalizes.
- The observation-only invariant is established as architectural law in the passive observation family. Future contributors trying to bolt active behavior onto a passive track hit the invariant first.

The shape is small on purpose: capture is the foundation. Everything interesting comes from what's built on top of it, and building on top of it is exactly what the deferred slices are for.
