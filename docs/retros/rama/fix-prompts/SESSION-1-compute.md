# Fix Session 1 — Compute Kernel (the template fix)

Status: NOT STARTED (requires Session 0 DONE)

Self-contained prompt. You are hardening `src/app/server/rama/dogfood/compute.clj` against the retro findings. This fix is the TEMPLATE the other kernels' sessions will copy — favor shapes that generalize.

## Load first
- Skills `/rama` + `rama-retro`.
- Read: `docs/retros/rama/01-compute/FINDINGS.md` (21 findings; C-05 was corrected — unknown-run observation is a fatal NPE poison record), then `docs/retros/rama/01-compute/FIX_PLAN.md` (the 6 ordered batches — your work plan), with `docs/retros/rama/01-compute/PLAN.md` as the validated reference design and `IMPLICIT_SPEC.md` as the contract. Cross-check the prior retro's repair queue: `docs/current-mental-model/build/rama-retro-review/02-compute-track/RAMA_REVIEW.md`.

## Plan first (plan mode)
FIX_PLAN.md batches 1–3 are this session's scope (correctness): submit dedup + idempotent folds (C-01/C-02/C-14), observation guard chain (C-03 nil-token fence, C-05 nil-guard BEFORE fold + dead-letter, C-06/07/08/12), executor pipeline (C-09 acked-cursor + incremental streaming, C-13 close-kills-processes + join timeouts, C-20). Batches 4–6 (schemas/caps/views, hygiene, full test suite) go to a follow-up session if context runs short — say so explicitly in the plan.

## Protocol
1. `(require 'app.server.rama.dogfood.compute)` first.
2. Use Session 0's shared helpers from core.clj — do not re-implement guards locally.
3. Write FAILING probes first (probe harness + the FIX_PLAN verification scenarios): duplicate submit after terminal must not respawn; observation with no token / wrong token / unknown run must not mutate truth and must not crash the topology; grant + partial inbox-removal replay must not leave a permanent pending entry.
4. Fix until probes pass; keep what the retro verified as correct (token-stripped views, reorder buffer, spawn-after-grant, partition alignment).
5. Re-run Phase 4 (`.claude/skills/rama/references/phase-4-impl-validate.md`) on the changed module; record the verdict in `docs/retros/rama/01-compute/` as `IMPLEMENTATION_VALIDATION-postfix.md`.

## Hard rules
Commit only code files; never read env.clj / codex_implementation / .agents. On completion: Status → DONE here, note in `docs/sessions/next-prompt.md` which batches landed.
