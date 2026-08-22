---
name: feedback-transcript-ingest-review
description: Codex adversarial review of transcript-ingest module — 6 findings showing feature-slice instincts where Rama production instincts are needed
metadata: 
  node_type: memory
  type: feedback
  originSessionId: 8df02868-2b22-49b2-b9ed-84ce48a2ce14
---

Transcript ingest module (`transcript_ingest.clj`) received a major-fail Rama review (2026-06-07).
The architecture/docs were praised; the implementation carries feature-slice instincts.

**Why:** The review found that the module lets unaccepted external facts become durable Rama truth,
imports old parser behavior without re-proving privacy/source invariants, and tests prove row
presence instead of production invariants.

**How to apply:** Every Rama topology write must answer: "is this an accepted fact?" before
materializing. Apply to all current and future modules.

Six findings (2 critical, 4 high):

1. **F1 (critical) — observations create truth without accepted run**: Gate observation materialization
   on `$$ingest-runs[request-id]` being in accepted state. [[feedback_rama_side_effects]]
2. **F2 (high) — terminal state mutation**: `fold-claim-into-run` must be monotonic — terminal states
   ignore late claims.
3. **F3 (critical) — parse-error previews leak raw secrets**: `redacted-preview` is just `subs` with no
   redaction. Apply string-level secret redaction before persistence.
4. **F4 (high) — UTF-8 corruption**: `RandomAccessFile.readLine()` uses modified UTF-8, not standard.
   Read raw bytes and decode explicitly.
5. **F5 (high) — tool_result content lost**: Actual `tool_result` events are treated as regular messages.
   Must update/create the result container with actual result content.
6. **F6 (high) — source-ledger hides partial failures**: Ledger written before cross-partition hops.
   If later hops fail, retry skips the event permanently. Move ledger to completion position or make
   secondary indexes replayable.

**Meta-learning:** `parse → write visible objects → test rows exist` is the wrong instinct.
`accept → validate state transitions → write durable facts → make derived views replayable →
test crash/retry/replay/privacy/source identity` is the Rama instinct.
