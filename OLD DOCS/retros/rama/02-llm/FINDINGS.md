# Findings — LLM Track (inline consolidation)

Source: `IMPLEMENTATION_VALIDATION.md` (R4, **major-fail**) — full line-cited runtime traces live there; this file is the actionable index. Test validation (R6) was not run for this track (defect-core scope decision); test gaps fold into the fix session. The validated blind PLAN.md in this folder is the reference design for fixes.

## HIGH

- **L-01 Request replay resets a claimed/streaming run.** Unguarded request-row `termval`: a request retry resets a claimed/streaming run to `:pending` and re-adds the inbox entry → **duplicate provider spawn** + permanently stalled sequence gap. No request dedup anchor. Same root class as compute C-01.
- **L-02 Claim grant / inbox-removal partial commit.** Grant commits, executor-task hop fails, retry sees non-pending and skips removal forever → lane head-of-line livelock. Same root class as compute C-02. Fix for both: PLAN.md's single-records-depot + guarded idempotent folds (or microbatch where atomicity spans hops).
- **L-03 Terminal-status regressions (4 sites) + pending-approval resurrection on observation retry.** Sticky-terminal invariant unenforced.
- **L-04 Never-drop violated.** Observations/controls for unknown runs are silently dropped (four-depot split means no orphan ledger).
- **L-05 Executor restart recovery impossible.** Claimed-run set is memory-only; after restart `claim-state` returns `:conflict-or-past` for `:running` rows — no recovery index exists (PLAN.md specifies `$$executor-active-runs`).

## MEDIUM

- **L-06** Cancel is one-phase and undeliverable mid-turn (spec requires mid-stream intervention).
- **L-07** Approvals: second-resolve wins (first-wins required), resurrection, invention of unknown approvals, no timeout path; buffered approvals never reach the pending index.
- **L-08** Whole-row fold + ~12 full-copy derived writes per observation → O(n²) write amplification at streaming rates (hot path).
- **L-09** All PStates `{String Object}`; items/raw/buffers/thread histories/rollups unbounded, nothing subindexed, no caps.
- **L-10** `mark-stale-approvals!` appends one control per approval; crash mid-doseq leaves a `:failed` run with approvals still pending (single-append-per-op rule).
- **L-11** Claude executor appends nothing until process exit (no live streaming); stderr stored unredacted.
- **L-12** Wall-clock fallbacks inside the fold (replay nondeterminism); unbounded live view.

## LOW

- **L-13** Hygiene cluster: consecutive `(keypath a)(keypath b)` ×6 (llm.clj:1327, 1335, 1352, 1383, 1384, 1424); `select-pstate-one` reimplements `foreign-select-one`.

## Fix direction

Batch order mirrors compute's FIX_PLAN: (1) idempotent request/claim folds + dedup anchor + sticky terminals (L-01/02/03), (2) approval lifecycle + control delivery (L-06/07/10), (3) executor recovery index + live streaming + redaction (L-05/11), (4) schemas/subindexing/write-path (L-08/09/12), (5) hygiene (L-13). Fix session resumes the standard skill process at Phase 3 against PLAN.md; re-run Phase 4 (and add Phase 6) after changes.
