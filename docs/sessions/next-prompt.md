# Active work package: implement relation-kernel-module

This file has two sections with different rules:
- **STANDING** — the work package's contract-of-engagement. Do NOT edit or delete
  while this package is active; it is removed only when the package closes.
- **NOW** — the baton. Whoever runs the current session rewrites it freely at
  session end (model name, phase reached, artifact paths, next step). One
  writer at a time; it always reflects only the present.

---

## STANDING (keep until package closes)

- The decision log `docs/current-mental-model/decisions.md` is **binding**
  (D-001..D-006, CLOSED, countersigned by Sid). Do not reopen closed decisions;
  reopening requires evidence of a used form breaking.
- The design is decided: `docs/current-mental-model/build/relation-kernel/CONTRACT.md`
  is the Phase-0 input. Implement it; do not redesign it.
- Process: the `/rama` skill's full phased process — one phase per fresh
  session, each phase produces its artifact, validation phases emit PASS/FAIL
  and on FAIL the prior phase re-runs with the failure artifact as input.
- New files only: `src/app/server/rama/relation_kernel.clj` + test namespace.
  Do NOT edit existing kernels; only permitted dependency is plain helper fns
  from `app.server.rama.object-container` (`extract-object-key`,
  `fixed-width-order-key`, `actor-row`).
- Scope guard: CONTRACT.md §9 refusals (no FK validation, no confidence
  scores, no relation-as-container, no deletion). Do not add them.
- Verification duties (before code): check two contract claims against
  `.agents/skills/rama/references/` — (1) §5 microbatch fine print (mid-batch
  crash → batch replay; PState writes transactional per batch across tasks);
  (2) §7 query-topology idiom (per-key `|hash` fan-out, aggregate at
  `|origin`). Note findings in the phase artifact.
- Definition of done: all 10 acceptance gates in CONTRACT.md §11 green as IPC
  tests + style gates (typed defrecords, imported partition helpers,
  `{:allow-yield? true}` on unbounded range reads, consumers only via the two
  query topologies). Then Fable gate review.
- **Stop clause**: if the contract cannot be built as specified or is wrong
  under Rama semantics — STOP that thread, do not improvise around it. Record
  the problem under "Open questions" in decisions.md and in the phase
  artifact; it goes to Sid + a Fable session for amendment. Per D-006 this is
  also an evaluation result.
- Hard rules: commit only code files (`.clj` etc.); never commit `.md` or
  .gitignore changes; never read `src/app/server/env.clj`.
- **Precedence**: this file is a baton, not a source of truth. If it ever
  contradicts CONTRACT.md or decisions.md, those win — flag the discrepancy in
  the NOW section instead of following this file.
- After green gates (do NOT start without Sid): transcript→commit/doc join
  extractor + git-commit-metadata adapter (D-003 Regime-1 spine) feeding the
  27-04 trail view. Notes in D-003 and CONTRACT.md §12.

---

## NOW (rewrite freely at each session end)

- 2026-07-03, Fable: work package opened; CONTRACT.md written; handoff created.
- 2026-07-03, Codex: Rama Phase 0 completed. Artifact:
  `docs/current-mental-model/build/relation-kernel/IMPLICIT_SPEC.md`.
  No contract-breaking issue surfaced in Phase 0.
- 2026-07-03, Codex: Rama Phase 1 completed. Artifact:
  `docs/current-mental-model/build/relation-kernel/PLAN.md`.
  The two carried verification duties were checked against
  `.agents/skills/rama/references/` and recorded in PLAN.md:
  microbatch PState updates are exactly-once with cross-partition atomicity
  across a microbatch transaction; query topologies support dynamic `|hash`
  fan-out with final `|origin` aggregation. No implementation code or tests
  were written. The plan intentionally leaves Phase 2 watch items around the
  idempotency journal nesting/conflict behavior, accepted no-op status events,
  unary `:none` target-index shape, filtered query prefix seeks, and payload
  record split.
- Next fresh session: run **Rama Phase 2: Plan Validation** only. Say:
  "Read `docs/sessions/next-prompt.md` and execute the NOW baton. Use `/rama`
  first. This is Phase 2 plan validation for `relation-kernel-module`;
  adversarially validate `docs/current-mental-model/build/relation-kernel/PLAN.md`
  against CONTRACT.md and IMPLICIT_SPEC.md, produce
  `docs/current-mental-model/build/relation-kernel/PLAN_VALIDATION.md`, emit
  `PHASE_VALIDATION:pass` or `PHASE_VALIDATION:fail` as the last non-empty line,
  and stop. Do not write implementation code or tests."
