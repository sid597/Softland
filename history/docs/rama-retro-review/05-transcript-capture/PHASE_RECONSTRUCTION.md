# Phase Reconstruction - Transcript Capture

Status: reconstructed.

This artifact reconstructs the Rama phase pack that should have existed for the
committed passive transcript capture slice. It is not a new design proposal and
does not include later transcript/object ingest redesign work.

## Source Evidence

```text
docs/current-mental-model/build/rama-retro-review/05-transcript-capture/BRIEF.md
docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-claude-research.md
docs/current-mental-model/architecture/dogfood-runtime/README.md
src/app/server/rama/dogfood/transcript.clj
test/app/server/rama/dogfood_transcript_test.clj

Commit anchors:
67be839 2026-05-11 llm: add Claude executor and transcript capture
59be788 2026-05-11 Add Claude LLM and transcript capture docs
```

## Phase 0 - Implicit Spec

The committed docs imply transcript capture is a passive observation track. It
reads existing source files, appends redacted observations to Rama, and makes
the acquisition/audit trail queryable without turning passive source material
into accepted Space or active LLM truth.

```text
User/world problem:
Softland needs to ingest existing Claude/Codex transcript history as durable
source material while preserving privacy, provenance, and auditability.

First physical record:
*transcript-depot receives a :transcript/harvest or :transcript/watch request.

New data entering the world:
- harvest/watch request
- transcript run status
- source JSONL lines
- parse-error rows
- source file state/offset rows
- observed conversation entries
- tool-call index entries
- redaction metadata
- progress/terminal/failure status

Entities and identities:
- :transcript/request-id owns one harvest/watch run.
- :transcript/source identifies :claude-code, :codex, or future sources.
- :source/file-id identifies the physical file, preferably by device/inode.
- :source/byte-offset identifies the byte position where a source line starts.
- :source/line-hash identifies the raw line bytes.
- source line identity is source + file-id + byte-offset + line-hash.
- :transcript/conversation-id is a grouping key, not source identity.
- :tool-call/id indexes observed tool_use blocks.

Invariants:
- Capture is observation-only.
- Capture never spawns Claude/Codex, calls APIs, modifies source files, or
  triggers downstream Softland actions.
- Executor writes depots only; topologies write all PStates.
- Redaction runs before persistence.
- Parse errors persist hash, length, redacted preview, and error kind, never
  raw unredacted bytes.
- Harvest re-run is idempotent by source-record ledger identity.
- Watch resumes from durable file-offset ledger state.
- Watch withholds trailing partial lines until complete.
- Malformed lines are recorded without killing harvest/watch.
- Conversation grouping and tool-call indexing are derived projections.
- Rehydration, Space registration, continuation, activation, and object-container
  redesign are out of scope.

Scale expectations:
- Harvest may read hundreds of transcript files.
- Individual JSONL files can be large.
- Watch may tail many files over time.
- Source ledger and conversation PStates can grow without a small fixed bound.
- Reads should be shaped around ledger lookup, conversation replay, and tool
  lookup, not a future object-container redesign.

Failure cases:
- duplicate harvest over same source files
- executor death during harvest
- watch restart
- file rotation, deletion, permission error, and watcher disconnect
- trailing partial JSON line
- malformed JSON line containing secrets
- UTF-8 / non-ASCII transcript rows
- schema drift across Claude versions
- multiple tool_use blocks in one message
```

## Phase 1 - Plan

The reconstructed Rama plan is a three-depot stream topology with a passive
executor at the edge. The executor may perform file I/O; Rama owns request/run
state, ledger materialization, conversation grouping, and tool-call indexes.

```text
Depots:
*transcript-depot
  Writer: request ingress
  Partition: :transcript/request-id
  Contents: harvest/watch request

*transcript-claim-depot
  Writer: executor
  Partition: :transcript/request-id
  Contents: claim, progress, terminal, failure, cancelled

*transcript-obs-depot
  Writer: executor
  Partition: source-file identity, not conversation id
  Contents: one append per complete source line or parse event

Topology choice:
Stream topology. Capture needs low-latency watch updates and append-heavy
source observations. File I/O must stay outside topology event handlers.

PStates:
$$transcript-runs
  Keyed by request id. Folds request and claim/status depot rows.

$$transcript-source-ledger
  Keyed by source + file-id + byte-offset + line-hash for source rows.
  Also keeps source file state keyed by source + file-id for watch resume.

$$transcript-observed-conversations
  Keyed by conversation id; stores source-line refs in source order.

$$transcript-tool-call-index
  Keyed by tool id or tool name depending on read contract. Every observed
  tool_use block should be indexable.

Request/status:
- Request append creates pending run.
- Executor claims pending run before harvest/watch work.
- Claim/status rows update run status, progress, terminal/failure/cancelled.
- Spawn registry or equivalent prevents duplicate active executors per request.

Harvest:
- Walk configured paths.
- Match JSONL files.
- Read source bytes line by line.
- Parse, redact, and append one observation per complete line.
- Malformed lines append parse-error observations.
- On per-file errors, append failure/progress ledger information and continue
  when fail-isolated behavior is specified.
- Rerun from start is safe because ledger folds duplicates by source identity.

Watch:
- Initialize each file offset from durable source-file ledger state.
- Existing unknown files start at EOF unless backfill is requested.
- New files start at byte 0.
- Partial trailing lines are withheld until newline/complete record.
- On reconnect, compare current file size with last durable offset and tail
  catch-up content.

Redaction:
- Parsed rows persist redacted payload only.
- Parse-error rows persist redacted preview only.
- Redactions are recorded on the observation row.
- Raw unredacted bytes are never persisted.

I/O and locality:
- Source line identity must be computed from raw bytes, not mojibake strings.
- Byte offsets and byte lengths must be true file byte positions.
- Large scans should stream appends and progress instead of accumulating all
  observations before appending.
- Conversation and tool indexes should be shaped around actual reads in this
  slice.
```

## Phase 2 - Plan Validation

The committed plan is mostly sound and unusually explicit about boundaries. A
Rama review should have focused on raw-byte identity, redaction-before-persist,
source-file partitioning, and bounded file scanning.

```text
Plan strengths:
- Observation-only invariant is explicit.
- Deferred boundaries are explicit.
- Three-depot shape is correct for request/status/observations.
- Executor/topology asymmetry is explicit.
- Source-record identity uses file id + byte offset + line hash.
- Re-run idempotency is correctly a topology fold property.
- Parse errors and partial lines are called out.
- Watch resume is tied to durable ledger state.

Plan risks:
- "Standard" redaction is named but not fully specified as executable patterns.
- Tool-call index key is ambiguous between tool id and tool name.
- A.0 says watch is deferred, but the committed block/test surface includes
  watch; the implementation needs to be judged by its committed code.
- The docs require source-file partitioning for observations but do not spell
  out the exact Clojure partitioner expression.
- The docs do not define subindex/bounds for large conversation histories.

Verdict the plan should have received:
PASS for MVP with review gates around byte-accurate line reading, parse-error
redaction, streaming harvest, source-file partitioning, and multiple tool_use
index coverage.
```

## Phase 3 - Expected Implementation Shape

```text
Expected namespace:
src/app/server/rama/dogfood/transcript.clj

Expected tests:
test/app/server/rama/dogfood_transcript_test.clj

Expected depots:
*transcript-depot
*transcript-claim-depot
*transcript-obs-depot

Expected topology branches:
1. request branch -> $$transcript-runs
2. claim/status branch -> $$transcript-runs
3. observation branch -> ledger, file state, run counts, conversations,
   tool-call index

Expected helper APIs:
- transcript-request
- transcript-run-status-record
- transcript-observation
- walk-jsonl-files
- read-jsonl-observations or streaming equivalent
- harvest-transcripts!
- start-transcript-watch!
- read-run
- read-ledger-line
- read-source-file-state
- read-conversation
- read-tool-call

Expected executor shape:
- no Claude/Codex spawn
- no source file mutation
- claim/status depot writes only
- observation depot writes only
- durable status/progress terminal rows

Expected tests:
- harvest green path
- idempotent rerun by source identity
- parse-error row recorded
- parse-error preview redacted
- UTF-8 byte offsets/lengths/hashes correct
- builders do not write PStates
- watch resumes from ledger offset
- partial trailing line withheld
- existing unknown file starts at EOF
- new file starts at byte zero
- multiple tool_use blocks are indexed
- malformed/per-file failures do not kill whole run when fail-isolated
```

## Phase 4 - Expected Implementation Validation Focus

```text
1. Verify no active Claude/Codex spawn or credential path exists.
2. Verify helpers append depots only and topologies write PStates.
3. Verify raw byte identity: file id, byte offset, byte length, raw-line hash.
4. Verify parsed payload text is not corrupted by line reading.
5. Verify parse-error previews are redacted before persistence.
6. Verify duplicate harvests do not duplicate ledger/conversation/tool rows.
7. Verify watch starts from durable file offset and withholds partial lines.
8. Verify large harvest scans stream/bound memory.
9. Verify source observations partition by source file or justify deviation.
10. Verify every tool_use block is indexable.
```

## Phase 5 - Expected Tests

```text
Positive tests:
- harvest records complete, counts, conversations, ledger rows, and tool index.
- builder functions alone do not materialize PStates.
- watch resumes known files from ledger offsets.
- watch withholds partial line until newline.
- watch starts existing unknown files at EOF and new files at byte zero.

Negative/adversarial tests:
- malformed line with secret-looking material persists only redacted preview.
- UTF-8 line preserves payload text and byte-length equals raw file bytes.
- multiple tool_use blocks in one message all appear in tool-call index.
- duplicate request/status rows do not start duplicate active executors.
- per-file read failure records failure/progress and does not kill all capture.
- large file harvest does not accumulate all observations before depot append.
```

## Phase 6 - Expected Test Validation

```text
Tests can pass while implementation is wrong if they:
- use only ASCII transcript rows.
- use one tool_use block per message.
- assert parse-error counts but not parse-error redacted preview content.
- check redaction only in parsed payload keys, not malformed lines.
- use tiny fixture files, hiding full-file accumulation.
- test watch by manually calling poll-once!, not process restart/reconnect.
- test duplicate harvest conversation count, not request-run counters or ledger
  audit detail.
```

## Phase 7 - Expected Finish Evidence

```text
Focused transcript tests pass.
Runtime probe proves malformed secret line is redacted before persistence.
Runtime probe proves UTF-8 payload and byte length are correct.
Runtime probe proves all tool_use blocks are indexed.
Runtime probe proves watch resumes from durable offset after restarted runtime or
equivalent ledger re-read.
No lingering Rama IPC or Clojure test processes.
```

## Reconstruction Gaps

```text
Committed docs do not fully define:
- exact standard redaction patterns
- tool-call index primary key: tool id vs tool name
- whether watch should be judged as MVP or A.1 because docs say no watch in A.0
- subindex/bounds for observed conversation histories
- exact dead-letter shape for file permission/read failures
- exact claim/spawn-registry schema for transcript executors
```

## Reconstruction Verdict

The committed docs give enough information to judge the passive transcript
capture implementation. The target is clear: observation-only, redacted,
byte-accurate, ledger-idempotent capture.

```text
PHASE_RECONSTRUCTION:complete
```
