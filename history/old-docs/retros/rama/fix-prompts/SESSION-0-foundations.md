# Fix Session 0 — Cross-Cutting Foundations

Status: DONE (2026-06-11)

Self-contained prompt. Do not load other global prompts. You are fixing the root causes shared by all five Rama kernels, identified by two independent retros. Do this session BEFORE any per-module fix session.

## Load first
- Skill `/rama` (mandatory before touching Rama code), skill `rama-retro` (probe discipline).
- Read: `docs/retros/rama/UNIFIED-RETRO.md` (verdict + weakness groups 1, 3, 6), then `docs/retros/rama/04-kernel-contract/FINDINGS.md` (K-07, K-08) and the prior retro's `docs/current-mental-model/build/rama-retro-review/06-text-kernel-shape/RAMA_REVIEW.md` (F1: kernel.clj does not load).

## Plan first (plan mode)
Produce an implementation plan covering the four deliverables below, with file-level changes and the probe list, before writing code.

## Deliverables
1. **`src/app/server/rama/kernel.clj` loads.** `(require 'app.server.rama.kernel)` must succeed. Fix whatever breaks it; do not change KERNEL-SHAPE semantics in this session beyond what loading requires.
2. **Shared guarded-fold helpers in `src/app/server/rama/core.clj`** (pure fns usable from every kernel's topology): decision-existence dedup gate (same id+payload → replay decision, no state writes; same id+different payload → conflict-rejected decision, no state writes); write-if-absent row guard; sticky-terminal fence (terminal statuses never regress); monotonic watermark helper. Follow the naming/DSL conventions already in core.clj.
3. **Shared observation/control authorization helper** in core.clj: target-exists ∧ state-accepts ∧ claim-token-proof ∧ valid-sequence ∧ terminal-rejects-late. Invalid input returns a bounded dead-letter/error value — NEVER throws (a throw in a topology = poison record), never silently mutates truth.
4. **Probe harness** in `test/app/server/rama/probe_harness.clj`: reusable helpers to run the depot-adversary matrix against any kernel module on IPC (append-before-request, duplicate-id-same/different-payload, no/wrong token, post-terminal write, collection past its bound). Pure helpers + docstring examples; per-module probes come in later sessions.

## Verification
- `clojure -M:test -e "(require 'app.server.rama.kernel 'app.server.rama.core)"` exits clean.
- Unit tests for each helper: replay, conflict, terminal-regression, dead-letter paths (assert semantic payloads, not just row existence).
- Existing test namespaces still pass (text_kernel, compute, llm, space, transcript).

## Hard rules
- Commit only code files (.clj/.cljc/.cljs); never read `src/app/server/env.clj`; never read `codex_implementation/` or `.agents/`.
- Do NOT rewire the five kernels to use the helpers yet — that is Sessions 1–5. This session only provides the foundations and proves them.
- On completion: set Status above to DONE (commit message: `rama: fix kernel load, add shared fold guards + probe harness`), and note completion in `docs/sessions/next-prompt.md`.
