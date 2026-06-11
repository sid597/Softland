# Fix Session 4 — Kernel Contract Layer

Status: NOT STARTED (requires Session 0 DONE)

Self-contained prompt. Hardening `src/app/server/rama/text_kernel.clj`, `src/app/server/rama/core.clj`, `src/app/server/rama/util_fns.cljc`, and making `kernel.clj`'s contract real.

## Load first
- Skills `/rama` + `rama-retro`.
- Read: `docs/retros/rama/04-kernel-contract/FINDINGS.md` (K-01..K-08, incl. the contract scorecard: C4/C8/C6/C12 + event immutability are prose-only or violated) + `04-kernel-contract/IMPLEMENTATION_VALIDATION.md`. Reference: `04-kernel-contract/PLAN.md` (**unvalidated**; its single zero-partitioner microbatch idea is the main design alternative to weigh). Cross-check prior retro: `build/rama-retro-review/01-world-kernel-contract/RAMA_REVIEW.md` (F1 duplicate request-id → two accepted events, runtime-proven; F2 util_fns atoms) and `06-text-kernel-shape/RAMA_REVIEW.md` (F3 compat path too broad; F4 identity headers are not contracts).

## Plan first (plan mode)
Correctness scope: (a) request commit atomicity — either collapse the 5-hop commit into a journaled/single-scope shape or guard every hop for replay (K-01); (b) decision/event dedup + immutability — existing decision for a request-id is final; colliding event ids reject; deterministic `:decided-at` (K-02/K-03, prior 01/F1); (c) revision-scoped unit identity so judgments never re-attach across re-ingest (K-04); (d) ingress validation before any keyed write — malformed appends get a durable rejection, never a poison record (K-05); (e) replace util_fns atom mirrors with PState-backed reads + a rebuild path, or explicitly quarantine them out of the kernel contract (K-07, prior 01/F2); (f) narrow `:compat/record` to allow-listed event types + validators (prior 06/F3). Then: update kernel.clj's KERNEL-SHAPE to match post-fix reality (it already loads after Session 0).

## Protocol
1. `(require 'app.server.rama.text-kernel 'app.server.rama.core 'app.server.rama.kernel)` first.
2. Failing probes first: duplicate request-id same/different payload (one accepted event, audit stable), status-set racing its own ingest, garbage append (no poison, durable rejection), re-ingest + old judgment (must not attach), restart with atom mirrors (reads must rebuild or be quarantined).
3. Fix until probes pass; preserve: envelope ontology, routing-key discipline, the passing contract items (C2, C9–C11, C13, C17–C19).
4. Re-run Phase 4 → `04-kernel-contract/IMPLEMENTATION_VALIDATION-postfix.md` with verdict.

## Hard rules
Commit only code files; never read env.clj / codex_implementation / .agents. All five kernels import core.clj — run every kernel's test namespace before committing. On completion: Status → DONE; note in `docs/sessions/next-prompt.md`.
