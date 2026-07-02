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
- 2026-07-03, Codex: Rama Phase 2 plan validation completed. Artifact:
  `docs/current-mental-model/build/relation-kernel/PLAN_VALIDATION.md`.
  Verdict: `PHASE_VALIDATION:fail`. No implementation code or tests were
  written. Blocking failures:
  1. PLAN.md changes duplicate idempotency-key semantics into a new rejected
     conflict decision, while CONTRACT.md and IMPLICIT_SPEC.md say duplicate
     idempotency keys replay the first decision and stop.
  2. PLAN.md treats accepted reassertions as decision-only no-ops, while the
     contract/implicit matrix require status/history evidence for
     reassertions.
  3. Filtered `relations-for-targets` and missing-id `relation-detail` examples
     have N > M read cases under the Rama query-validation template; the plan
     must either avoid those reads or explicitly stop for contract/skill
     adjudication.
- 2026-07-03, Codex: Rama Phase 1 plan revision completed after the failed
  Phase 2 validation. Artifact revised:
  `docs/current-mental-model/build/relation-kernel/PLAN.md`.
  No implementation code or tests were written. The revised plan addresses the
  three Phase 2 failures by:
  1. Making duplicate idempotency keys replay the stored decision and stop,
     without creating a separate rejected conflict decision.
  2. Treating accepted reassertions/repeated valid retractions as accepted
     status/history evidence, not decision-only no-ops, while preserving one
     target-visible relation membership.
  3. Adding a bounded `$$relation-target-descriptors` PState to descriptor-gate
     `relations-for-targets` range reads, and making `relation-detail` skip the
     status-history range read for missing relation ids.
- 2026-07-03, Codex: Rama Phase 2 plan validation completed against the revised
  `PLAN.md`. Artifact rewritten:
  `docs/current-mental-model/build/relation-kernel/PLAN_VALIDATION.md`.
  Verdict: `PHASE_VALIDATION:fail`. No implementation code or tests were
  written. The revised plan fixed the previous same-relation duplicate replay,
  accepted-reassertion history, and empty-range query-read failures, but two
  blockers remain:
  1. The planned typed `RelationRequestRow` field is `relation-routing-key`,
     while the depot is still `(hash-by :relation/routing-key)`. The plan does
     not prove the physical record can route to hash(relation-id), so the
     colocated journal/decision/row invariant is not safe.
  2. The plan scopes idempotency as `(relation-id, idempotency-key)`, while
     `IMPLICIT_SPEC.md` still says same idempotency key collapses to the first
     decision without stating that relation-id scope. This needs either a
     contract/implicit-spec amendment or a concrete global/caller-scoped
     idempotency mechanism.
- 2026-07-03, Claude (Opus 4.8): Rama Phase 1 plan revision after the 2nd
  `PHASE_VALIDATION:fail`. Split the two blockers by disposition. **F1
  (depot/request-row partition-key mismatch): FIXED in PLAN.md, no binding-doc
  change** — reverted the depot event type from the invented `RelationRequestRow`
  defrecord to CONTRACT §4's request map envelope (namespaced keys), which is the
  verified codebase idiom (map on the wire, defrecord only in PStates:
  `object_container.clj:1657-1686`, `:383`, `space.clj:171`). Dropped
  `RelationRequestRow`; added the partition-provenance test to carry into Phase 5.
  **F2 (idempotency scope): ESCALATED to Sid/Fable per the stop clause — NOT
  decided.** The binding CONTRACT (colocated relation-id journal, "copied from
  object-container's decisions-by-idempotency") and the binding IMPLICIT_SPEC
  (global-sounding `IdempotencyKey` entity) conflict, and they cannot both hold
  in Rama. Recorded with a high-confidence recommendation (relation-scoped) in
  `PLAN.md` → "Idempotency scope (ESCALATED)" and in `decisions.md` Open
  Questions (PROPOSED). No implementation code or tests written.
  *(This session's next-step — referral to Sid/Fable — was resolved by the Fable
  ruling immediately below; F2 is now CLOSED (A) relation-scoped. PLAN.md's
  "Idempotency scope" section is now titled RESOLVED. See Fable entry for the
  live next step.)*
- 2026-07-03, Claude (Opus 4.8) follow-up: after the ruling landed, aligned
  PLAN.md to it — F2 sections flipped ESCALATED→RESOLVED (no design change; the
  nested relation-scoped journal was already the ruled shape), and the plan now
  points at CONTRACT §11 gate 11 (cross-relation key reuse) for Phase 5.
- 2026-07-03, Fable: **RULED (A) relation-scoped** on Sid's referral. Executed:
  decisions.md Open Questions entry CLOSED with reasoning; CONTRACT §5 states
  the scope explicitly; CONTRACT §11 gained gate 11 (cross-relation key reuse:
  both relations succeed); IMPLICIT_SPEC has a binding amendment banner
  (every "same idempotency key" = "same (relation-id, idempotency-key)").
  No plan change required. D-006 evaluation notes updated with the honest
  letter-vs-spirit scoring of the contract amendment.
- Next fresh session: run **Rama Phase 2: Plan Validation** on the current
  PLAN.md. Say: "Read `docs/sessions/next-prompt.md` and execute the NOW baton.
  Use `/rama` first. Idempotency scope was RULED relation-scoped (see CONTRACT
  §5 amendment + IMPLICIT_SPEC banner). Re-run Phase 2 plan validation on
  PLAN.md; confirm both round-2 blockers (F1 routing, F2 scope) are resolved;
  verify the plan covers acceptance gate 11; rewrite PLAN_VALIDATION.md; emit
  `PHASE_VALIDATION:pass` or `PHASE_VALIDATION:fail` as the last non-empty
  line, and stop. Do not write implementation code or tests."
