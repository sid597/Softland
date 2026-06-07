# Next Session: Transcript Ingest F5-F6 Fixes

## What was done (earlier this session, 2026-06-07)

Complete Rama 7-phase build of transcript ingest module + Codex review hardening (F1/F2/F3),
then F4 (byte-correct reader) in this session.

### Commits on `docs/current-mental-model-local`:

```
cfcc559 rama: harden transcript ingest production boundaries (F1/F2/F3)
3001f70 rama: add transcript ingest object-container module
a60c921 docs: capture transcript ingest Rama phased build artifacts
```

(F4 changes below are in the working tree, NOT yet committed — user has not asked to commit.)

### Codex review findings — fixed:

- F1 (critical): Observations now gate on `$$ingest-runs[request-id]` existence
- F2 (high): `fold-claim-into-run` is monotonic — terminal states ignore late claims
- F3 (critical): `redact-raw-string` with pattern-based secret detection applied pre-persist
- F4 (high): byte-correct UTF-8 reader (see below)

## F4 — DONE (2026-06-07, byte-correct transcript reader)

Source identity foundation. `RandomAccessFile.readLine()` decodes with modified UTF-8
(zero-extends each byte → char), corrupting content (`hé`→`hÃ©`), `byte-length`, and `line-hash`
all at once. Fixed IN-PLACE in the shared reader so both transcript modules + the watch path benefit.

Changed:
- `src/app/server/rama/core.clj` — added `sha-256-bytes` (byte-array hash); `sha-256` delegates to it.
- `src/app/server/rama/dogfood/transcript.clj` — added `line-hash-bytes`; `transcript-observation`
  now takes a precomputed byte-correct hash; **`read-jsonl-observations` rewritten** to read raw bytes
  (`FileInputStream` channel position + `BufferedInputStream`), split on byte `0x0A`, decode UTF-8
  explicitly, and compute `byte-offset` / `byte-length` (content + 1 for `\n`; +0 for an unterminated
  final line) / `line-hash` from the exact byte slice.
- `test/app/server/rama/dogfood/transcript_ingest_test.clj` — added `write-utf8!` + `write-bytes!`
  helpers and 5 tests: `read-jsonl-byte-correct-utf8-test`, `read-jsonl-no-trailing-newline-test`,
  `read-jsonl-empty-and-newline-terminated-test`, `read-jsonl-invalid-utf8-byte-correct-test`,
  `harvest-preserves-non-ascii-content-test`.

Verified: pure reader tests 22/22; ingest suite 157/158; old transcript suite 20/20. Adversarial
falsification workflow (4 lenses / 12 axes) found no real defect. The one flagged item — old↔new hash
divergence for *invalid* UTF-8 — was rejected as a non-issue (fresh module, no persisted ledger, old
hashes were corrupt by construction; valid-UTF-8 hashes are identical across the change). Gotcha
captured in `memory/implementation-quirks.md` → "RandomAccessFile.readLine".

KNOWN FLAKE (pre-existing, NOT F4): `tool-call-index-queryable-test` intermittently sees 0 instead of
2 bash calls in the FULL suite (passes 3/3 in isolation). Root cause: the tool-call index is written
by a cross-partition hop on the observation path that can lag the run-`:complete` signal that
`harvest-ingest!` awaits. Fix the TEST to `await-materialized` on the tool-call index (not the run
status) if it becomes annoying. Tangential to F5/F6.

## What is incomplete — F5, F6 (do NOT combine into one session)

### F5 — tool_result materialization (do NEXT)

Actual `tool_result` events (user messages carrying `tool_result` content blocks) must populate
the ToolResultContainer with redacted result content. Currently treated as regular messages.

Key area: `build-message-containers` in `transcript_ingest.clj` — needs event-type dispatch that
recognizes tool_result content blocks and updates/creates result containers.

Probe that should FAIL before fix, PASS after:
```clojure
;; Ingest a tool_result JSONL line
;; Assert: ToolResultContainer has redacted result content (not nil/empty)
;; Assert: projection includes tool-result summary with actual content
```

Note: the test ns already has `tool-result-line`, `standalone-tool-result-event-test`, and
`orphaned-tool-result-test` scaffolding — but they currently only assert container existence / message
ingestion, NOT that the result CONTENT is materialized. F5 must make the content assertion real.

### F6 — Source-ledger / retry ordering (do LAST, separately)

Topology/fault-tolerance design change. Source-ledger marker is written before cross-partition
hops. If later hops fail and retry finds the ledger entry, audit/tool-call index rows are
permanently lost.

Fix direction: either move source-ledger to completion position (requires hop back to conv-key
partition after all cross-partition writes), or make secondary indexes replayable/reconcilable
independently of the source-ledger.

This is NOT a small patch. Design the fix carefully.

### Key Rama insight discovered this session:

Rama's `hash-by` depot partitioner and `|hash` topology partitioner use DIFFERENT hash functions.
Custom PState `key-partitioner` functions must match `|hash` (Clojure's `hash`). Topologies MUST
`|hash` before `local-transform>` on key-partitioned PStates — relying on depot `hash-by` colocation
is wrong. The object_container module gets this right (explicit `|hash` before writes).

### Memory saved:

- `memory/feedback_transcript_ingest_review.md` — full review findings and meta-learning
- `memory/implementation-quirks.md` — "RandomAccessFile.readLine" UTF-8 gotcha (F4)
