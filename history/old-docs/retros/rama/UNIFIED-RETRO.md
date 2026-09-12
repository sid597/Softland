# Unified Retro — All Findings, All Learnings, One Fix Queue

The single entry point merging BOTH retros of the pre-skill Rama work:
- **Retro A (probes)**: `docs/current-mental-model/build/rama-retro-review/` — phase reconstruction + review lens + **runtime probes**; 30 findings (4 critical / 11 high / 15 medium); 6/6 blocks major-fail.
- **Retro B (pipeline)**: `docs/retros/rama/` (this folder) — blind spec/plan re-derivation + official phase-4/6 run verbatim; ~70 findings incl. test gaps; 5/5 tracks major-fail.
- Cross-comparison: `COMPARISON-prior-retro.md`. Method codified for reuse: skill `rama-retro` (`.claude/skills/rama-retro/SKILL.md`).

**Unified verdict** (both retros agree independently): *architecturally promising, Rama-contract immature, production-unsafe until hardened.* The product ideas and the kernel shape are right; the implementations were built as feature slices, not truth machines. Sharpest diagnosis (Retro A): "We confused 'the thing exists in Rama' with 'Rama owns the truth safely.'"

## The seven weakness groups (merged taxonomy)

1. **Unguarded lifecycle overwrites under at-least-once delivery** — duplicate/replayed requests reset live or terminal state → duplicate physical side effects (second OS spawn, second model run). Present in EVERY module. [B: C-01, L-01, SP-02, K-02/03, TR-02 ≡ A: 02/F2, 04/F2, 01/F1]
2. **Invariants spanning partitioner hops** (transaction scope = between partitioners) — grant/inbox pairs partially commit → permanent stale state + infinite claim loops; compose retry permanently loses facts; 5-hop commits race. Found almost exclusively by Retro B. [C-02, L-02, SP-01/03/04, K-01, TR-01]
3. **Observations/controls trusted without authority** — folding without existence/state/token checks: nil-token bypass, claimless observations flipping runs, approvals invented for requests never made, non-patch observations minting patch proposals, unknown-run observation = fatal NPE poison record. [B: C-03/C-05, L-04, SP-06/07 ≡ A: 04/F1, 03/F1/F2, 02/F1]
4. **Named invariant ≠ proven invariant** — docs state the rule; validation accepted weaker evidence (row exists ≠ semantics hold). The root process failure both retros converged on.
5. **PState shape as storage convenience** — `{String Object}` everywhere, unbounded non-subindexed collections, whole-row × 2-PStates write amplification on the hottest paths. [B: C-04/06, L-09, SP-09, K-06 ≡ A: every block's F-medium]
6. **Transitional/compat paths became permanent truth** — util_fns atom mirrors as UI truth with no rebuild path; broad `:compat/record`; identity headers as comments; **`app.server.rama.kernel` does not load**. [B: K-07/08 ≡ A: 01/F2, 06/F1-F4]
7. **External I/O not treated as source identity** — redaction hardcoded off, UTF-8 byte corruption, partial-line ingestion, first-tool_use-only indexing, accumulate-then-append harvest. [B: TR-03/04/07 ≡ A: 05/F1-F5]

Full taxonomy detail + 17-question done-gate: `build/rama-retro-review/META_LEARNINGS.md`. Per-module traces: `0N-*/FINDINGS.md` (B) and `build/rama-retro-review/0N-*/RAMA_REVIEW.md` (A).

## Master fix queue (cross-module order)

### Batch 0 — Cross-cutting foundations (do ONCE, before any module fix)
1. **Make `app.server.rama.kernel` load** (A-06/F1, critical — blocks everything that claims to follow KERNEL-SHAPE).
2. **Shared guarded-fold helpers in core.clj** so five kernels stop hand-rolling the same hazard: decision-existence dedup gate (same id+payload → replay; same id+different payload → conflict-reject); write-if-absent; sticky-terminal fence; monotonic watermarks. [kills Group 1 at the root]
3. **Shared observation/control authorization helper**: target exists ∧ state accepts mutation ∧ claim-token proof ∧ valid sequence ∧ terminal rejects late writes; invalid → bounded dead-letter, never a throw (poison) and never silent truth mutation. [kills Group 3 at the root]
4. **Probe harness**: reusable depot-adversary-matrix test helpers (per `rama-retro` skill R5 list) so every fix batch ships with adversarial probes, not just happy tests.

### Batch 1 — Compute (the pattern-setter; its fix is the template)
Per `01-compute/FIX_PLAN.md` (6 batches) merged with A-02's repair queue. Order: microbatch conversion (or journaled guarded folds) + submit dedup [C-01/02 ≡ A/F2] → nil-guard the obs branch BEFORE fold + dead-letter unknown runs [C-05 corrected, A/F1 NPE] → nil-token fence [C-03] → executor pipeline (acked-cursor sends, incremental line streaming, close-kills-processes, join timeouts) [C-09/13/20] → schemas/caps/subindex + drop $$compute-views copy [C-04/06/10/11/15/16] → adversarial test suite [T-01..07].

### Batch 2 — LLM
Request dedup + sticky terminals [L-01/03 ≡ A/F2] → claim-proof on every observation (extend envelope with executor-id+token; topology validates) [A/F1 critical] → approval lifecycle: resolve only existing pending approvals, native id from the pending row, timeouts [L-07 ≡ A/F3, A-03/F2] → claim/inbox partial-commit repair + executor recovery index [L-02/05] → live streaming + stderr redaction [L-11] → bounds/subindexing [L-09 ≡ A/F4].

### Batch 3 — Space
Journal-entry-first atomic fold (idempotency check+write in ONE scope; all non-idempotent writes in segment 1) [SP-01..04] → idempotency conflict semantics (material hash; conflicting reuse → explicit reject, not aliasing) [A/F3 + SP-03] → typed observation bridge: gate proposal-creation with `patch-proposal-observation?`, single append per op [A/F1 + SP-05] → proposal/approval lifecycle guards (first-resolution-wins, no phantom rows) [SP-06/07 ≡ A/F2] → replay-deterministic decisions [SP-08] → bounds [SP-09].

### Batch 4 — Kernel contract (rest)
Single-hop or journaled request commit + decision/event dedup + deterministic decided-at [K-01/02/03 ≡ A-01/F1] → revision-scoped unit identity [K-04] → ingress validation before keyed writes (no poison records) [K-05] → replace util_fns atom mirrors with PState-backed reads + rebuild path [K-07 ≡ A-01/F2] → narrow the compat path (allow-listed event types, validators, removal criteria) [A-06/F3] → make C1–C19 enforced (helpers/tests), then update KERNEL-SHAPE to match reality [K-08 ≡ A-06/F2/F4].

### Batch 5 — Transcript (must not break the June object-container layer above it)
Real redaction before persistence (needs a small Phase-1 plan) [TR-03 ≡ A/F1 critical] → raw-byte line reading + UTF-8-safe offsets [A/F2 + TR-04] → idempotent folds + sticky terminals [TR-01/02] → streaming harvest appends; partition by source-file identity (or document deviation) [A/F3/F4] → all tool_use blocks + claim registry [TR-07/05 ≡ A/F5/F6] → test suite incl. multi-byte fixtures, redaction-on-malformed-JSON, ledger assertions [TT-01..04].

## Fix-session protocol (every batch)

1. Open the track folder as impl-root; resume the standard `/rama` skill at **Phase 3** (spec + plan + findings already in place; tracks 2–4 plans are unvalidated references — see README scope note).
2. Step zero: `require` every namespace you will touch.
3. Write the failing adversarial probes FIRST (depot adversary matrix for the touched branches), then fix until they pass.
4. Re-run Phase 4 (and Phase 6 if tests changed) on the result; record verdicts in the track folder.
5. Tests assert semantic payloads, not row existence; one IPC launch per suite unless shared state demands more.

## Handoff — how to actually run the fixes

**One file = one session**: `docs/retros/rama/fix-prompts/SESSION-0..5.md` are self-contained work packets (inputs, scope, failing-probes-first protocol, done criteria, status line). Start a fresh session and say:

> Read docs/retros/rama/fix-prompts/SESSION-0-foundations.md and execute it: plan the work first, then do it.

Run Session 0 first; then 1 (compute, the template); then 2–5 in any order (2 before 3 recommended). `fix-prompts/README.md` has the dependency table.
