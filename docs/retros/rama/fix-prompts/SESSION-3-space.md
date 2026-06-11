# Fix Session 3 — Space Kernel

Status: NOT STARTED (requires Session 0 DONE; Session 2 recommended first — Space mirrors LLM depots)

Self-contained prompt. Hardening `src/app/server/rama/dogfood/space.clj` (1,868 lines; mirrors `*llm-depot` / `*llm-control-depot`).

## Load first
- Skills `/rama` + `rama-retro`.
- Read: `docs/retros/rama/03-space/FINDINGS.md` (SP-01..SP-10) + `03-space/IMPLEMENTATION_VALIDATION.md` (full traces). Reference design: `03-space/PLAN.md` (**unvalidated** reference — its journal-entry-first Segment-1 pattern is the key idea to adopt). Contract: `03-space/IMPLICIT_SPEC.md`. Cross-check prior retro: `docs/current-mental-model/build/rama-retro-review/03-space-runtime-spine/RAMA_REVIEW.md` — F1: non-patch observations become patch proposals (`patch-proposal-observation?` exists at space.clj:1026 but is never called); F2: approvals invented; F3: idempotency is conflict-blind (probe: same key, different Space → aliased).

## Plan first (plan mode)
Correctness scope: (a) journal-entry-first atomic fold — idempotency check + journal write + all non-idempotent writes in ONE transaction scope before any partitioner hop (kills SP-01 lost-facts, SP-02 no-journal resets, SP-03 check/write race, SP-04 turn-order race); (b) idempotency conflict semantics — store material hash, conflicting reuse → explicit conflict decision, never aliasing (prior F3); (c) typed observation bridge — gate proposal creation with `patch-proposal-observation?`, make `append-llm-observation!` single-append per op (prior F1 + SP-05); (d) proposal/approval lifecycle guards — first-resolution-wins, no phantom rows, resolve-only-existing (SP-06/07, prior F2). Defer: replay-deterministic decision polish (SP-08), bounds (SP-09), hygiene (SP-10) — flag if deferred.

## Protocol
1. `(require 'app.server.rama.dogfood.space)` first.
2. Use Session 0 helpers; respect any depot-contract changes noted by Session 2.
3. Failing probes first: compose retry after idempotency write (catalog/relations/dispatch must survive), same-key-different-material send (must conflict, not alias), non-patch observation through the bridge (must NOT create a proposal), resolve-nonexistent-proposal/approval, concurrent turns (no dropped turn-order entries).
4. Fix until probes pass; preserve: bundle-freeze determinism, fork-binding gate, space-only-turn isolation.
5. Re-run Phase 4 → `03-space/IMPLEMENTATION_VALIDATION-postfix.md` with verdict.

## Hard rules
Commit only code files; never read env.clj / codex_implementation / .agents. On completion: Status → DONE; note in `docs/sessions/next-prompt.md`.
