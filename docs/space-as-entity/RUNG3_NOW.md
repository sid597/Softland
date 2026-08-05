# space-as-entity rung 3 (G10 lift) — thread file

STAGED 2026-07-26; RUNG3_CONTRACT.md authored same session (Fable). Rungs
1+2 CLOSED at `43a57a0` + `f7945fd`; cluster base-active. This package
opens the space's instance tier; reaction-declaration is NOT started here.

## STANDING (frozen at open — do not edit while the package is active)

- Binding docs: `RUNG3_CONTRACT.md` (beside this file) + `decisions.md`;
  rungs-1+2 `CONTRACT.md` is standing law (fence, T1–T9), never
  re-litigated. **This file is a baton, not a source of truth; if it
  contradicts RUNG3_CONTRACT.md or decisions.md, those win — flag the
  discrepancy in NOW.** Phases and QC per
  `.claude/skills/work-package/SKILL.md`.
- Allowlist — EDITED: `binding_material.cljc` (legality + docstrings;
  fence var untouched), `ground.cljs` (console seam + extraction bridge +
  clamp read + table label + docstrings), `facet_master.clj` (write-lane
  owner-aware site check), `space_material.cljc` (the `space-subject`
  constant), `face_wiring.cljs` (the `:subjects` line), and the T-R6-swept
  test files (named in the phase artifact). NEW: test files named per
  phase. READ-ONLY: `verb_registry.cljc`, `facet_material.cljc`,
  `facet_masters.cljc`, `material_truth.clj`, `episode.clj`,
  `face_projection.clj`. Touching a read-only file is a stop clause.
- Verification duties: every line-cited claim in the contract's input
  manifest is re-checked against the on-disk file before code.
- Definition of done: contract gates R3-G1…R3-G8 green (partition: P1
  runs all eight — sum-checked, zero remainder; every gate
  machine-runnable, no Sid-bound gate staged) + the one falsification
  finder run + Fable gate verdict recorded here and in `decisions.md`.
- Stop clauses: contract §Stop clauses. Escalate per the skill; never
  improvise on binding docs.
- Hard rules: no Co-Authored-By in commits · never read
  `src/app/server/env.clj` · code and docs never mixed in one commit ·
  docs only on the local docs branch, never pushed.

## NOW log (append ≤15 lines per session)

- 2026-07-26 · Fable (staging session) · package STAGED. Contract authored
  from disk-verified reads; three traps found by verification before any
  phase exists: T-R1 (claim subject `:space` keyword vs durable string —
  a naive lift serves a deviation that silently never fires), T-R2 (the
  zoom clamp reads the SHARED tier — a space deviation would be worn,
  never felt), T-R3 (wholesale site-lift mints dead durable rows for
  non-space facets — the P6-F2 class). Write-lane fence confirmed
  INHERITED via instance-grammars (no fourth call site; census stays 3).
  Gate partition sum-checked at authoring per the new skill rule: P1 =
  all of R3-G1…G8; no environment/actor-bound gate staged (grounds in
  contract §Gate partition + owners). Next: fresh build session runs P1
  per the contract; boot from RUNG3_CONTRACT.md + this STANDING.

- 2026-07-26 · Codex (P1 implementer) · IMPLEMENTER PASS; artifact: `RUNG3_P1.md`.
  Seven contract deliverables complete; R3-G1…R3-G8 green in context.
  Focused: 24 tests / 400 assertions; fast: 190 / 1,795; zero failures/errors.
  Isolated gates: 15/232, 8/61, 5/165, 16/284, 11/103; all zero failures/errors.
  Working-tree shadow-cljs compile: 272 files, 3 compiled, 0 warnings.
  Fresh live G3 proved deviation `[0.1 3.0]`, release, and exact worn revision.
  Fresh live G4 proved pin `[0.1 8.0]`, unpin to shared `[0.1 2.0]`, rollback.
  Fresh live G5 refused all three hostile writes with zero append delta.
  Falsification finder found three bridge/cache counterexamples; all fixed; PASS.
  Judgment flag: repeat G3 bootstrap import exposes the pre-existing normal
  import-fingerprint conflict; fresh activation still lands the exact revision.
  Base fm:space `[0.1 8.0]` active; instance remains released/inherit; console clear.
  Rama cluster + UI and working-tree dev app remain live for the handoff.
  No commit or push. Next owner: Fable independent slim gate review/verdict.

- 2026-07-26 · Fable (gate session) · **GATE PASS**; artifact: `RUNG3_GATE.md`.
  Independent re-runs: focused 24/400, fast 190/1795, isolated 15/232 · 8/61 ·
  5/165 · 16/284 · 11/103, all zero fail/error; censuses re-derived (legality
  1 defn/3 owner reads; fence 1 var/3 reads, no fourth; read-only zero diff).
  Live re-drives, fresh ids: G5 refusals append nothing (4 rev/13 ptr stable);
  G3 deviation felt at exactly 3.0, release restores; G4 pin holds 8.0 over
  shared 2.0, unpin feels 2.0, rollback restores 8.0/0.1; G2 console cycle
  green (fence-ordered refusal, foreign-subject refusal, label exact).
  Judgment call CONFIRMED: repeat-import fingerprint conflict is the
  pre-existing OC bootstrap-vs-normal asymmetry; residue + falsifier in GATE.
  Findings (non-blocking): src-dev/dev.cljc LAND_PINNED guard is allowlist
  drift, unflagged by P1 — surface at commit; g1 harness :no-space-instance?
  check permanently stale post-G3; handoff dev app was dead at boot,
  relaunched pinned. decisions.md updated. Next: Sid's commit word →
  HEAD-dynamic suites at committed HEAD → close + retro.

- 2026-07-26 · Fable (close session) · **CLOSED + RETRO'D**; artifact:
  `RUNG3_RETRO.md`. Code landed `ecbd572` on Sid's word (code-only, clean).
  Adversarial recheck (fresh context): CORRECTIONS REQUIRED — 2 substantive
  (committed harness covers the four server-read gates only; 9→13 pointer
  attribution unauditable) + 6 precision; all applied in the retro body.
  Recheck independently re-ran the four HEAD-dynamic suites at `ecbd572`:
  code-atoms 9/198 · git-spine 7/167 · git-spine-gate 1/10 · focused
  24/400, all green — close protocol's HEAD step discharged. Git clean:
  no mixed commits, no Co-Authored-By. Lessons routed: two rules → the
  work-package skill (diff-derived file lists; one-shot preconditions);
  OC import asymmetry + LAND_PINNED boot → implementation-quirks;
  decisions.md bullet flipped CLOSED. Board pruned; multi-cascade build
  session UNBLOCKED (shared-file fence lifted). Residue in RETRO: OC
  falsifier (next OC package) · g1 one-line fix (next harness-touching
  commit) · consumer-2 felt payoff rides Sid's return wear.

## Starter prompt (P1) — paste-ready

    Space-as-entity rung 3: the G10 lift, phase P1. Boot:
    docs/current-mental-model/build/space-as-entity/RUNG3_CONTRACT.md
    (binding) + RUNG3_NOW.md STANDING. Re-verify the input manifest
    against disk before code. Implement the seven deliverables exactly;
    run gates R3-G1..R3-G8 green in-context; bank every numbered receipt
    (live receipts server-read, fresh request ids). Standing law: the
    fence's three lanes, one reservation var, no fourth call site;
    verb_registry.cljc and facet_material.cljc are READ-ONLY; frozen
    v1/v2 grammars untouched. Out of scope: nested spaces, per-space
    camera, instance preview, reaction-declaration, zoom bands. Append
    the NOW entry (<=15 lines) + phase artifact RUNG3_P1.md at session
    end; do not commit code.
