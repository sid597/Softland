# Rama Review - Transcript Capture

Status: reviewed.

## Contract Restatement

Transcript capture should be passive source observation. It reads existing JSONL
files, redacts source records, appends observations to Rama, and folds ledger,
conversation, and tool-call PStates. It must not become active LLM execution or
accepted Space truth.

```text
First depot/input record:
*transcript-depot receives a :transcript/harvest or :transcript/watch request.

Record type:
The first record is a request. Source lines become observations only after the
passive executor reads files.

Acceptance/rejection:
Transcript topology validates request shape into $$transcript-runs. Status
rows from *transcript-claim-depot move runs through pending/running/complete,
failed, or cancelled.

Durable accepted truth:
There is no accepted Space/LLM truth in this slice. Durable transcript truth is
source-observation materialization: $$transcript-source-ledger,
$$transcript-observed-conversations, $$transcript-tool-call-index, and
$$transcript-runs.

Projection/read surface:
Read run status by request id, source ledger by source-line identity, source
file offset by source file id, observed conversation by conversation id, and
tool call by tool id/name.
```

## Rama-First Reconstruction

```text
New data:
harvest/watch request, status/claim rows, source JSONL observations, parse-error
rows, source ledger rows, source file state, observed conversation entries, and
tool-call index rows.

Entity owning local ordering:
Source file owns byte-offset ordering. Request id owns run status. Conversation
id owns grouped replay order.

First physical depot record:
*transcript-depot request.

Request/proposal vs accepted fact vs observation:
- request asks to harvest/watch
- status row reports executor lifecycle
- observation reports one source line or parse event
- ledger/conversation/tool-call PStates are derived, not source files

Authoritative decision:
Transcript topology for PState truth. Executor only reads files and appends
depots.

Partition:
Request/status by request id. Observations should be partitioned by source-file
identity so file-tail ordering/locality is natural.

Fast reads:
run status by request id, source file state by file id, source line by natural
key, conversation by conversation id, tool-call index by tool id/name.

Retries/duplicates/restarts:
Harvest can rerun from the start. Watch can restart and resume from ledger
offset. Duplicate observation appends are absorbed by source-ledger identity.

Side effects:
Only file reads and depot appends. No Claude/Codex spawn, API calls, source file
mutation, or downstream action.
```

## Phase Diagnosis

```text
Phase 0 - Implicit spec:
Strong. Passive observation, redaction, source identity, and deferred boundaries
are explicit.

Phase 1 - Plan:
Mostly sound. It could be more exact about redaction patterns, tool-call index
keys, and subindexing, but the review gates are clear.

Phase 2 - Plan validation:
Should have passed MVP planning with gates for byte-accurate reading,
parse-error redaction, source-file partitioning, and streaming harvest.

Phase 3 - Implementation:
The committed implementation materializes the basic harvest/watch spine and
keeps PState writes in topology, but misses core capture guarantees:
parse-error previews are not redacted, UTF-8 source identity is corrupted,
harvest is not streaming/bounded, observation partitioning does not match the
source-file contract, and only the first tool_use block is indexed.

Phase 4 - Implementation validation:
Focused tests pass, but probes expose redaction, byte identity, and indexing
failures.

Phase 5 - Tests:
Tests cover happy ASCII harvest/watch behavior and builder boundaries, but miss
the adversarial cases the docs call out.

Phase 6 - Test validation:
Green tests are not enough because fixture size and ASCII-only rows hide the
capture contract failures.

Phase 7 - Finish/runtime:
Focused tests pass; runtime probes fail important finish criteria.
```

Primary Failure Phase: phase-3

## Files Reviewed

### Phase Reconstruction

```text
docs/current-mental-model/build/rama-retro-review/05-transcript-capture/BRIEF.md
docs/current-mental-model/build/rama-retro-review/05-transcript-capture/PHASE_RECONSTRUCTION.md
```

### Code

```text
src/app/server/rama/dogfood/transcript.clj
```

### Tests

```text
test/app/server/rama/dogfood_transcript_test.clj
```

### Docs

```text
docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-claude-research.md
docs/current-mental-model/architecture/dogfood-runtime/README.md
```

### Commit Anchors

```text
67be839 2026-05-11 llm: add Claude executor and transcript capture
59be788 2026-05-11 Add Claude LLM and transcript capture docs
```

## Topology / Depot / PState Inventory

### Depots

```text
*transcript-depot
  src/app/server/rama/dogfood/transcript.clj:414

*transcript-claim-depot
  src/app/server/rama/dogfood/transcript.clj:415

*transcript-obs-depot
  src/app/server/rama/dogfood/transcript.clj:416
```

### Topologies

```text
transcript-capture-topology
  src/app/server/rama/dogfood/transcript.clj:417

Request source branch:
  src/app/server/rama/dogfood/transcript.clj:424-433

Claim/status source branch:
  src/app/server/rama/dogfood/transcript.clj:435-441

Observation source branch:
  src/app/server/rama/dogfood/transcript.clj:443-467
```

### PStates

```text
$$transcript-runs
$$transcript-source-ledger
$$transcript-observed-conversations
$$transcript-tool-call-index

Declared at src/app/server/rama/dogfood/transcript.clj:418-421.
```

### Query / Read Helpers

```text
read-run
read-ledger-line
read-source-file-state
read-conversation
read-tool-call

Defined at src/app/server/rama/dogfood/transcript.clj:516-537.
```

### Client Append Helpers

```text
append-transcript-request!
append-transcript-status!
append-transcript-observation!

Defined at src/app/server/rama/dogfood/transcript.clj:491-510.
```

### TaskGlobals / Caches / Executors

```text
No Rama TaskGlobal is declared in the committed implementation.

Harvest helper:
  harvest-transcripts!
  src/app/server/rama/dogfood/transcript.clj:552-579

Watch helper:
  start-transcript-watch!
  src/app/server/rama/dogfood/transcript.clj:604-671
```

## Findings

### F1. Parse-Error Rows Persist Unredacted Preview Text

Severity: critical

Evidence:
- docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md:102 - parse failures do not kill capture and must never persist raw unredacted bytes.
- docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md:116 - original unredacted content is not persisted anywhere by capture.
- docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md:118 - parse-error rows persist redacted preview, hash, byte length, and error kind.
- docs/current-mental-model/architecture/dogfood-runtime/llm-track-claude-research.md:355 - transcript ingestion requires secret/token redaction at ingest.
- src/app/server/rama/dogfood/transcript.clj:280 - redacted-preview returns a raw substring.
- src/app/server/rama/dogfood/transcript.clj:311 - parse failures store redacted-preview from that raw substring.
- test/app/server/rama/dogfood_transcript_test.clj:36 - harvest test includes one parse error but does not assert preview redaction.

Runtime trace:
1. Wrote malformed line containing "api_key" and "never-store-me".
2. Harvested the file.
3. Read the source-ledger row.
4. The persisted parse-error preview still contained "never-store-me".

Probe result:
```text
{:parse-error-preview
 {:event-type :parse-error,
  :parse-error-kind :invalid-json,
  :redacted-preview "{\"api_key\":\"never-store-me\"",
  :contains-secret? true}}
```

Rama concern:
The ledger is durable. Once a malformed line persists an unredacted preview,
the passive observation layer has violated its privacy contract permanently.

Failure phase:
phase-3

Consequence:
Secrets in malformed/truncated transcript lines can be written into Rama even
though the docs make parse-error redaction a hard invariant.

Repair shape:
Run text redaction before storing parse-error preview. If structured redaction
cannot parse the line, use conservative text-pattern redaction or store only
hash/length/error kind until a safe redactor exists.

### F2. UTF-8 Transcript Rows Corrupt Byte Identity And Payload Text

Severity: high

Evidence:
- docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md:180 - source/file-id, byte offset, line hash, and byte length define the source record.
- docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md:210 - file-id + byte-offset + line-hash uniquely identify one source line.
- src/app/server/rama/dogfood/transcript.clj:349 - read-jsonl-observations reads with RandomAccessFile.
- src/app/server/rama/dogfood/transcript.clj:359 - RandomAccessFile.readLine is used.
- src/app/server/rama/dogfood/transcript.clj:362 - byte length is recomputed from the decoded line string as UTF-8.
- src/app/server/rama/dogfood/transcript.clj:364 - line-utf8 is reconstructed from those bytes.
- src/app/server/rama/dogfood/transcript.clj:190 - line-hash hashes the reconstructed string, not raw source bytes.

Runtime trace:
1. Wrote a UTF-8 JSONL line whose payload content was "h" plus character 233.
2. Actual byte length including newline was 98.
3. The observation reported byte length 100.
4. The parsed payload content became "hÃ©" instead of the original text.

Probe result:
```text
{:unicode-byte-identity
 {:expected-byte-length 98,
  :observed-byte-length 100,
  :payload-content "hÃ©",
  :line-hash "sha256:d7097b99abc85e20e964633c8b6358a69448b420b61f91445eb9a1fc20e67938"}}
```

Rama concern:
Source-record identity and watch resume depend on byte-accurate offsets,
lengths, and hashes. Reconstructing text through RandomAccessFile.readLine
breaks both provenance and parsed transcript content for non-ASCII rows.

Failure phase:
phase-3

Consequence:
Ledger keys can drift from real file bytes, watch offsets can advance
incorrectly, line hashes cannot reliably re-correlate to source files, and
conversation content can be corrupted.

Repair shape:
Read raw bytes until newline, compute byte length/hash from raw bytes, decode
with UTF-8 exactly once for JSON parsing, and handle malformed UTF-8 as an
encoding parse-error row.

### F3. Harvest Accumulates File Observations Instead Of Streaming Appends

Severity: high

Evidence:
- docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md:265 - executor should read line-by-line and append one observation per source line.
- docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md:350 - disk full/failure handling depends on visible last materialized source row.
- src/app/server/rama/dogfood/transcript.clj:357 - read-jsonl-observations loops with an observations vector.
- src/app/server/rama/dogfood/transcript.clj:367 - every line is conjed into the vector before return.
- src/app/server/rama/dogfood/transcript.clj:559 - harvest creates a lazy sequence of all file observations.
- src/app/server/rama/dogfood/transcript.clj:561 - counts reduce consumes observations before appends.
- src/app/server/rama/dogfood/transcript.clj:568 - only after that are observations appended to Rama.

Runtime trace:
Not needed; the full-file vector and pre-append count pass are visible in code.

Rama concern:
Rama topologies stay clean, but the executor-side acquisition path is not
bounded. Large transcript archives can be retained in memory before any depot
append, and mid-run failure may leave no ledger row for work already parsed.

Failure phase:
phase-3

Consequence:
Large harvests can use excessive memory and lose audit precision on failure.
The docs wanted capture to make ingestion progress auditable as it happens.

Repair shape:
Stream raw lines and append observations as they are read. Maintain progress
counters incrementally through status rows. Avoid retaining the head of a lazy
sequence of all observations.

### F4. Observation Depot Partitions By Request Instead Of Source File

Severity: medium

Evidence:
- docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md:158 - *transcript-obs-depot should partition by source file.
- docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md:270 - source ledger is keyed by source/file/offset/hash.
- src/app/server/rama/dogfood/transcript.clj:416 - *transcript-obs-depot is declared `(hash-by :transcript/ingest-request-id)`.
- src/app/server/rama/dogfood/transcript.clj:443 - observation branch then repartitions by source-line key before ledger writes.

Runtime trace:
Not needed; the depot declaration differs from the committed contract.

Rama concern:
Partitioner choice is the event boundary. The source-file contract preserves
file-tail locality and avoids funneling all observations for a large harvest
through one request partition.

Failure phase:
phase-3

Consequence:
File ordering/locality is not natural at the depot boundary. A large request
can concentrate ingestion traffic by request id and then repartition per line.

Repair shape:
Partition observation depot by source file identity or explicitly document why
request partitioning is acceptable for the committed MVP and add tests for the
ordering/locality behavior expected by watch.

### F5. Only The First Tool Use Block Is Indexed

Severity: medium

Evidence:
- docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md:170 - tool-call index records observed tool calls.
- docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md:227 - tool_use blocks are preserved for future provenance correlation.
- docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md:310 - A.0 needs to preserve tool calls.
- src/app/server/rama/dogfood/transcript.clj:262 - tool-call-index-rows can produce multiple rows.
- src/app/server/rama/dogfood/transcript.clj:401 - obs-tool-call-first keeps only the first row.
- src/app/server/rama/dogfood/transcript.clj:463 - topology writes at most one tool-call row per observation.
- test/app/server/rama/dogfood_transcript_test.clj:42 - committed test has only one tool_use block.

Runtime trace:
1. Wrote one assistant JSONL row containing tool_use blocks tool-a and tool-b.
2. Harvested the file.
3. tool-a was indexed.
4. tool-b was nil.

Probe result:
```text
{:multi-tool-index
 {:tool-a {:tool-call/id "tool-a", :tool-call/name "bash", ...},
  :tool-b nil}}
```

Rama concern:
The index is a derived PState. Dropping secondary tool_use blocks during the
fold makes later provenance queries incomplete even though the raw redacted
payload still contains the data.

Failure phase:
phase-3

Consequence:
Conversation rows with multiple tool calls cannot be fully correlated to future
git/filesystem provenance.

Repair shape:
Emit/index every row returned by tool-call-index-rows, either by a loop in the
topology or by storing a bounded/subindexed set under an observation or tool
name key.

### F6. Status Rows Are Not A Real Claim/Spawn Registry

Severity: medium

Evidence:
- docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md:264 - executor should claim pending runs and use a spawn-registry pattern.
- src/app/server/rama/dogfood/transcript.clj:156 - transcript-run-status-record is a generic run-status record.
- src/app/server/rama/dogfood/transcript.clj:180 - fold-run-status blindly applies incoming status.
- src/app/server/rama/dogfood/transcript.clj:552 - harvest helper appends request and then writes :running directly.
- src/app/server/rama/dogfood/transcript.clj:604 - watch helper appends request, writes :running, and starts a daemon thread.

Runtime trace:
Not needed; the claim state is absent in code.

Rama concern:
Even passive work should have a durable single-executor claim when the docs
promise one. Idempotent source-ledger folding mitigates duplicate observations,
but it does not prove only one watcher/harvester owns the run.

Failure phase:
phase-3

Consequence:
Duplicate helpers can start duplicate passive acquisition work for the same
request id. The ledger may absorb duplicate source rows, but lifecycle truth and
resource usage are not claim-safe.

Repair shape:
Split claim/progress/terminal shapes or add explicit claim owner/token and
grant logic. Watch/harvest should start file work only after durable claim
grant.

## Phase-4 Style Check Matrix

### Redundant Conditionals

Verdict: PASS

Evidence:
No redundant conditional chain is the main source of failure.

### Consecutive Keypath

Verdict: PASS

Evidence:
Topology writes are straightforward keypath/termval updates.

### Select-Compute-Transform

Verdict: MIXED

Evidence:
Observation branch uses select-compute-transform correctly for idempotent
ledger rows, but status branch does not validate status transitions or claim
ownership before transform.

### Unnecessary nil->val

Verdict: PASS

Evidence:
No meaningful nil->value transform smell was observed.

### :allow-yield?

Verdict: N/A

Evidence:
No large local-select loop is present in topology. The large-scan problem is in
executor-side file reading.

### Non-Subindexed Collections Without Size Limits

Verdict: FAIL

Evidence:
Conversation entries are nested under conversation id with no declared subindex
or bound: src/app/server/rama/dogfood/transcript.clj:420 and
src/app/server/rama/dogfood/transcript.clj:462.

### Stream Topology Idempotency

Verdict: MIXED

Evidence:
Source ledger dedupes duplicate source rows by line key, but request/status
claim idempotency is not implemented as a true claim grant.

### Partial Failure In Stream Topologies

Verdict: MIXED

Evidence:
Parse errors become rows and do not kill harvest. Per-file read errors and
status failures are not fail-isolated in harvest helper.

### Single Depot Append Per Client Operation

Verdict: PASS

Evidence:
Append helpers each write one depot record:
src/app/server/rama/dogfood/transcript.clj:491-510.

### Application-State Caches Survive Restart

Verdict: PASS

Evidence:
Watch local offsets are initialized from durable source-file state at
src/app/server/rama/dogfood/transcript.clj:615-630. In-memory offsets are local
runtime convenience, not source truth.

### No Reimplementation Of Built-In Operations

Verdict: PASS

Evidence:
No custom reimplementation of Rama built-ins was observed.

## Tests / Commands

```text
git show --stat --oneline --name-only 67be839 59be788 -- ...

Result:
Confirmed the committed block touched transcript.clj, dogfood_transcript_test,
transcript-capture.md, and llm-track-claude-research.md.

clojure -M:test -e "(require 'clojure.test 'app.server.rama.dogfood-transcript-test) (clojure.test/run-tests 'app.server.rama.dogfood-transcript-test) (shutdown-agents) (System/exit 0)"

Result:
Testing app.server.rama.dogfood-transcript-test
Ran 3 tests containing 20 assertions.
0 failures, 0 errors.

Runtime probe command:
clojure -M:test -e "(do (require (quote [app.server.rama.dogfood.transcript :as t])) ... probes ...)"

Probe result:
{:parse-error-preview
 {:event-type :parse-error,
  :parse-error-kind :invalid-json,
  :redacted-preview "{\"api_key\":\"never-store-me\"",
  :contains-secret? true},
 :unicode-byte-identity
 {:expected-byte-length 98,
  :observed-byte-length 100,
  :payload-content "hÃ©",
  :line-hash "sha256:d7097b99abc85e20e964633c8b6358a69448b420b61f91445eb9a1fc20e67938"},
 :multi-tool-index
 {:tool-a {:tool-call/id "tool-a", :tool-call/name "bash", ...},
  :tool-b nil}}

ps -eo pid,ppid,etime,stat,command | rg "clojure|-M:test|transcript-probe|dogfood-transcript-test|rama"

Result:
No lingering probe/test process found.
```

## Recommended Repair Queue

```text
1. Add failing tests for parse-error redaction, UTF-8 byte identity, and multiple
   tool_use block indexing.
2. Replace parse-error preview with conservative text redaction or hash-only
   persistence until text redaction is safe.
3. Replace RandomAccessFile.readLine with raw-byte line reading and explicit
   UTF-8 decode.
4. Stream harvest observations and append progress incrementally instead of
   accumulating full-file/full-run observation sequences.
5. Align *transcript-obs-depot partitioning with source-file identity or
   document/test the intentional deviation.
6. Index all tool_use blocks.
7. Add a real claim/spawn-registry transition for harvest/watch helpers.
8. Add bounded/subindexed conversation history reads before production scale.
```

## Open Questions

```text
- Should parse-error rows store any preview at all, or only hash/length/error
  until text redaction is stronger?
- Is the tool-call index keyed by tool id, tool name, or both?
- Should watch remain in this committed slice even though the A.0 doc scoped it
  to A.1+, or should docs be updated to reflect committed reality?
- What is the intended dead-letter/audit shape for per-file permission errors?
```

## Verdict

The committed transcript capture code preserves the passive spine, but it fails
the capture contract on the details that make passive ingestion trustworthy:
redaction-before-persist, byte-accurate source identity, bounded acquisition,
source-file partitioning, and complete tool-call indexing.

```text
Primary Failure Phase: phase-3
```

```text
RETRO_RAMA_REVIEW:major-fail
```
