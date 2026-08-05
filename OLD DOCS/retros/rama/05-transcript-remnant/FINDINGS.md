# Findings — Transcript Remnant Track (inline consolidation)

Sources: `IMPLEMENTATION_VALIDATION.md` (R4, **major-fail**, blame-scoped to the 589 May-era lines; 7 June-era observations listed separately there) + `TEST_VALIDATION.md` (R6, **major-fail**). Abbreviated pipeline: no PLAN.md exists for this track — fixes sketch against IMPLICIT_SPEC.md + skill rules, and the June object-container layer sits ON TOP of these paths, so fixes must not break it.

## HIGH

- **TR-01 Ledger-write-then-retry loses derived writes.** Retry after the ledger write commits → the dedup gate skips → conversation/tool-call/counter writes are permanently lost (cross-task writes don't roll back).
- **TR-02 Lifecycle regression.** Duplicate submit resets a `:complete` run to `:pending`; a late progress claim flips terminal back to `:running`; watch `:failed` can be overwritten by `:cancelled`. Same root class as compute C-01.
- **TR-03 Redaction breach.** Parse-error preview persists raw bytes (truncation ≠ redaction); `:transcript/redactions` is hardcoded `[]` — "what was masked?" is unanswerable. This is the spec's I2 invariant, violated outright.
- **TR-04 Partial-line ingestion.** Harvest ingests trailing partial lines and advances the cursor mid-line; watch's length-snapshot TOCTOU emits partials — the byte-correct/newline-gated invariant (I5) is broken in the May-era paths. (Note: the June F4 "byte-correct reader" hardened the reader; these surviving May call-paths still violate it.)

## MEDIUM

- **TR-05** No at-most-once claim arbitration; `:complete` races in-flight observations → torn status/counters.
- **TR-06** Watch restart: never-ingested or recreated-at-path files are silently EOF-baselined (history skipped); in-memory cursor advances before the append is durable → dropped lines.
- **TR-07** Only the first `tool_use` block per line is indexed; redacted inputs never stored.
- **TR-08** Conversation view: unbounded non-subindexed inner map, no message content, no source ordering; audit ledger has point lookups only (no per-file range access, run notes, liveness).

## LOW

- **TR-09** Cluster: novel-only counters, `:since` unimplemented, empty files unrecorded, parse errors conversation-visible, fail-isolation (I8) absent, redundant conditionals/keypath/select-compute-transform/`foreign-select-one` reimpl, obs-depot single-task funnel.

## Test findings (R6)

- **TT-01** Redaction scans are vacuous — both scanned projections carry no payload field; the ledger (where redacted payload persists) is never scanned.
- **TT-02** Watch "resume from ledger offset" assert is confounded by dedup — passes whether resumed or re-read.
- **TT-03** Post-`:complete` reads race across depots (`:append-ack` on separate depots ≠ materialization).
- **TT-04** Untested: audit ledger ops entirely, lifecycle non-regression, harvest edges (rotation, `:since`, unreadable, partial trailing line), multi-block tool_use, multi-byte UTF-8 byte-correctness, same-offset/different-hash mutation. 4 IPC launches where 1 suffices.

## Fix direction

(1) Idempotent fold guards + sticky terminals (TR-01/02), (2) redaction made real (TR-03 — design-level: "needs a Phase 1 plan first" for the redaction pipeline), (3) newline-gated cursor discipline both paths (TR-04/06), (4) claim arbitration + tool-call completeness (TR-05/07), (5) views/ledger (TR-08 — needs Phase 1 plan), (6) test suite per TT-01..04. Every batch must be checked against the June object-container call-sites inside transcript.clj before merge.
