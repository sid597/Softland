# multi-cascade — thread file

STAGED 2026-07-26 (the monster-round session; dark-lane organ #1). R1
declaration slice only — table + `react!` + autotag as row #1,
behavior-identical. Build session runs AFTER space-as-entity rung 3
lands.

## STANDING (frozen at open — do not edit while the package is active)

- Binding docs: `CONTRACT.md` (beside this file) + `decisions.md`
  (including "How engine work lands"). **This file is a baton, not a
  source of truth; if it contradicts CONTRACT.md or decisions.md, those
  win — flag the discrepancy in NOW.** Phases and QC per
  `.claude/skills/work-package/SKILL.md`.
- Allowlist — NEW: `src/app/server/cascade.clj` +
  `test/app/cascade_table_test.clj`. EDITED: `server_jetty.clj` (autotag
  block + call site only) · `test_runner.clj` (RULING 2026-07-26: exactly
  one `isolation-exceptions` entry for `app.cascade-table-test` — swept
  here per the ruling-execution-sweep rule). READ-ONLY:
  `material_circulation.clj`,
  `verb_registry.cljc`. FENCED (rung-3 in-flight WIP — do not touch):
  `binding_material.cljc` · `ground.cljs` · `facet_master.clj` ·
  `space_material.cljc` · `face_wiring.cljs` + their tests.
- Verification duties: every manifest line in CONTRACT §8 re-checked
  against disk before code (line numbers drift; the threading wrapper
  and guard shape are load-bearing).
- Definition of done: G1–G8 green (partition sum-checked in CONTRACT §5)
  + one falsification finder + Fable gate verdict recorded here and in
  `decisions.md` + board dark-lane line updated.
- Dark-organ laws apply (decisions.md): loads pure · routes nowhere
  beyond row #1 · suite-covered from birth · board-listed. The
  dark-interval lesson clause (CONTRACT §7) fires at the first NEW row.
- Stop clauses: CONTRACT §6. Escalate per the skill; never improvise on
  binding docs.
- Hard rules: no Co-Authored-By in commits · never read
  `src/app/server/env.clj` · code and docs never mixed in one commit ·
  docs only on the local docs branch, never pushed.

## NOW log (append ≤15 lines per session)

- 2026-07-26 · Fable (monster-round session) · package STAGED: contract
  authored against disk (autotag seam read: `server_jetty.clj:806` defn,
  `:988` call under `when`-guard + `future` + `try/catch`;
  `autotag-material!` idempotency branches `:571–:605`; effect-class
  vocabulary verified). Traps T1–T7; gates G1–G8 all assigned to P1 with
  owners (G6 live half: implementer drives, Fable re-drives). Landing
  sort: mechanism-shaped, deploy-under-proof; zero durable touch (G7).
  Next: fresh build session runs P1 AFTER rung 3 lands; boot from
  CONTRACT.md + this STANDING.

- 2026-07-26 · Codex · P1 pre-code manifest · **STOP**.
  Rung 3 is landed (`ecbd572`; close HEAD `ffc3b6a`) and the tree was clean.
  Every named §8 call/guard/arity/idempotency line matches disk, but the
  manifest omits `test/app/test_runner.clj`: its fail-closed inventory discovers
  every `*_test.clj` and rejects an unclassified namespace. Therefore the new
  `cascade_table_test.clj` cannot run in G8's full suite without an edit outside
  §9's allowlist. Recommended ruling: widen §8/§9 for one isolated-namespace
  classification entry (the tests own fresh OC/RK/LLM runtimes); then re-enter
  P1 from the clean code tree. No source/test edit, no test run, no commit/push.

- 2026-07-26 · Fable (ruling; the rung-3 close session) · **RULING:
  option A.** Stop CONFIRMED genuine — the runner verified fail-closed
  (`test_runner.clj:214–:247`) and G8's full-suite bar cannot hold beside
  the staged §9; tier fit verified against the registry's own precedent
  (fresh OC/RK/LLM runtimes = the machine-cut/circulation isolation
  class). §8 + §9 amended in place: allowlist gains
  `test/app/test_runner.clj` scoped to EXACTLY one `isolation-exceptions`
  entry for `app.cascade-table-test`; STANDING allowlist swept with the
  same marker; G8 text unchanged — full suite stays the bar. Option B
  rejected: smearing the organ's suite into a foreign namespace weakens
  exactly what the fail-closed runner protects. decisions.md open
  question resolved + deleted (the contract is the law's home); the stop
  round is preserved at `c4d73ea`. Next: P1 re-enters from the clean
  tree, same starter prompt below.

    Multi-cascade R1, single phase P1 — dark-lane organ #1. Boot:
    docs/current-mental-model/build/multi-cascade/CONTRACT.md (binding)
    + NOW.md (this file) + decisions.md "How engine work lands".
    PRE-CHECK: space-as-entity rung 3 must be LANDED (git log; if its
    WIP is still uncommitted in the tree, STOP and flag). Re-verify
    CONTRACT §8 manifest against disk before code. Implement §3 exactly;
    run gates G1–G8 to green; bank every receipt. Fences: CONTRACT §6/§9
    (rung-3 files, material_circulation.clj, verb_registry.cljc
    untouchable). Out of scope: durable runner, log topology,
    rows-as-material, clock pulse, any durable event change.

- 2026-07-26 · Codex · **P1 IMPLEMENTED; G1–G8 GREEN.** Pre-check passed:
  rung 3 code `ecbd572` + close `ffc3b6a` landed; clean tree at entry; §8
  reverified line-by-line. §3 cut is the four allowed code/test paths only.
  Focused: 1t / 49a / 0f / 0e (actual OC projection record IDs byte-compared;
  replay `:already-recorded` + adapter false; literal old-guard false cases).
  Full exact-tree suite: 52ns / 464t / 6,441a / 0f / 0e; registered
  `dogfood-llm-test` stale-approval flake fired attempt 1, passed attempt 2;
  registry unchanged. G6 real Ctrl+Enter: 54 visible candidates; durable SSE
  444.8ms; `[CASCADE]` before resident start; autotag `:completed`, 32
  candidates, run `llm-run-material-autotag:e07aa38f…`, relation nil.
  G7: fenced files byte-untouched (`material_circulation` cb8f399…,
  `verb_registry` 3e90f54…); no durable/topology edit. One fresh finder first
  falsified guard/record-id proofs; corrections rechecked **PASS**. New-file
  lint 0/0; diff-check clean. No commit/push. Next: Fable gate review.

- 2026-07-26 · Fable · **GATE PASS** (`GATE.md`). Independent re-runs:
  focused 1t/49a/0/0; full 52ns/464t/6,441a/0/0 (all flakes attempt-1
  green); lint 0/0; G7 hashes + fences re-verified, one ruled runner
  entry only. G6 re-driven twice on `:8081` (swiftshader attestation;
  `:8080` untouched): `[CASCADE]` at +547ms on the request thread after
  durable acceptance, `TURN-START` +1ms later, `[CIRCULATION][AUTOTAG]`
  `:completed`/4 candidates/+16.6s async with the pre-cut field set;
  drive-1 autotag row durable for the gate's own block, completed after
  the browser died. Accidental A/B: Sid live on `:8080` (old path) —
  identical durable row shape beside the through-table rows. Traps
  T1–T7 spot-checked at diff level. Open doubts (non-blocking, GATE.md):
  optimistic dispatch receipts (R2 falsifier) · `:lines` payload seam ·
  dev stdout hides INFO (log4j2 default ERROR). Two gate turns left in
  genesis, disclosed. Next: Sid's commit decision, then close + retro.
