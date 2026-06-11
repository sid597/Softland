# Fix Session 2 — LLM Kernel

Status: NOT STARTED (requires Session 0 DONE; read Session 1's diff first if DONE — it is the pattern)

Self-contained prompt. Hardening `src/app/server/rama/dogfood/llm.clj` (2,126 lines; four depots incl. the control plane).

## Load first
- Skills `/rama` + `rama-retro`.
- Read: `docs/retros/rama/02-llm/FINDINGS.md` (L-01..L-13) and `docs/retros/rama/02-llm/IMPLEMENTATION_VALIDATION.md` for the full traces. Reference design: `02-llm/PLAN.md` (**unvalidated** — strong reference, not gospel; where it conflicts with a working as-built shape, prefer the minimal correct fix). Contract: `02-llm/IMPLICIT_SPEC.md`. Cross-check the prior retro (it has the runtime-proven critical): `docs/current-mental-model/build/rama-retro-review/04-llm-agent-track/RAMA_REVIEW.md` — F1: observations fold with NO claim authorization (probe: a claimless append flipped a `:pending` run to `:running`); F3: approval resolution invents missing approvals.

## Plan first (plan mode)
Correctness scope for this session: (a) request dedup + sticky terminals (L-01, L-03); (b) **claim-proof on every observation** — extend the observation envelope with executor-id + claim-token and validate in `fold-observation` (prior-retro F1, our L-04 never-drop: invalid → dead-letter, not silent drop); (c) approval lifecycle — resolve only existing pending approvals, native JSON-RPC id copied from the pending row, run-finished expires leftovers (L-07); (d) claim/inbox partial-commit repair + executor recovery index (L-02, L-05). Defer (perf/UX): live streaming for the Claude executor + stderr redaction (L-11), bounds/subindexing (L-09), hygiene (L-13) — flag if deferred.

## Protocol
1. `(require 'app.server.rama.dogfood.llm)` first.
2. Use Session 0's shared helpers; copy Session 1's fixed compute shapes where the structure matches (it inherited compute's executor pattern).
3. Failing probes first: observation-without-claim, wrong-token observation, duplicate run request after `:succeeded` (must not respawn), approval-resolve for a never-requested approval, grant + inbox-removal partial replay.
4. Fix until probes pass; preserve what held: deterministic ids, cost-rollup no-double-count, follow-up thread binding.
5. Re-run Phase 4 on the changed module → `02-llm/IMPLEMENTATION_VALIDATION-postfix.md` with verdict.

## Hard rules
Commit only code files; never read env.clj / codex_implementation / .agents. Space mirrors these depots — if you change depot record shapes, check `space.clj` call sites compile and note any contract change for Session 3. On completion: Status → DONE; note in `docs/sessions/next-prompt.md`.
