# multi-cascade — thread file

R1 (declaration slice) CLOSED 2026-07-27, code `34bf7c0` — trail in the
NOW log below. **R2 (durable runner + log-consuming topology) STAGED
2026-07-27**: `CONTRACT_R2.md` beside this file; first honest customer =
episode-retry (the banked stranded-lane defect). R2's activation of the
first NEW row fires the dark-interval lesson clause and opens the
stratum's first batch retro.

## STANDING — R2 (re-frozen at R2 open, 2026-07-27; R1's STANDING is in
## git — do not edit while the package is active)

- Binding docs: `CONTRACT_R2.md` (beside this file; `CONTRACT.md` remains
  the R1 record and governs the grammar R2 extends) + `decisions.md`
  (including "How engine work lands"). **This file is a baton, not a
  source of truth; if it contradicts the contracts or decisions.md, those
  win — flag the discrepancy in NOW.** Package shell per
  `.claude/skills/work-package/SKILL.md`; module phase mechanics per the
  `/rama` skill; `/rama` + `/rama-pitfalls` load BEFORE any topology work.
- **Durable-touch ⇒ FULL gate tier** (2026-07-27 cadence ruling).
- Allowlist — NEW: `src/app/server/rama/cascade_log.clj` ·
  `test/app/cascade_log_test.clj` · `test/app/episode_retry_test.clj`.
  EDITED: `cascade.clj` · `episode.clj` · `server_jetty.clj` (emission
  site #2 + boot sweep wiring only) · `cluster.clj` (cascade runtime
  accessors only) · `file_viewer.cljc` (runtime plumbing only, if IPC
  boot needs it) · `bin/land` (one MODULE_VARS line) · `test_runner.clj`
  (classification entries for the two new test namespaces only).
  READ-ONLY: `material_circulation.clj` · `verb_registry.cljc` · all
  OC/RK/llm module files. No live fence set; clean tree at entry.
- Verification duties: every CONTRACT_R2 §8 manifest line re-checked
  against disk before code (line numbers drift; the waiter ordering,
  adoption read, and `:system`-actor authorization seam are load-bearing).
- Definition of done: G1–G12 green (partition sum-checked in CONTRACT_R2
  §5) + one falsification finder + FULL Fable gate verdict recorded here
  and in `decisions.md` + board dark-lane line updated + the §7
  activation duties (dark-interval lessons; stratum batch retro opened).
- Dark-organ laws apply until activation (decisions.md): loads pure ·
  the sweep fires only from explicit boot wiring · suite-covered from
  birth · board-listed with the activation trigger.
- Stop clauses: CONTRACT_R2 §6. Escalate per the skill; never improvise
  on binding docs.
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

- 2026-07-27 · Fable (same gate session) · **CADENCE RULING landed (Sid:
  "land it")**: gate reviews tiered slim/full; retros batched at the
  stratum boundary — work-package skill amended + swept (gate-review +
  closing sections), decisions.md + board updated. For THIS package:
  close = Sid's code commit → post-commit HEAD-dynamic re-run → board
  prune; residue already banked in `GATE.md`; retro joins the dark-lane
  batch at first activation. CONTRACT §9's "close + retro per the skill"
  stays literally true — the skill now defines the cadence.

- 2026-07-27 · Fable (same session) · **CLOSED.** Code committed
  `34bf7c0` on Sid's word; both HEAD-reading suites are :head-override
  pinned so nothing re-runs (checked, not assumed); board line pruned
  to the done-pointer. Package ends here; retro at the stratum batch.

- 2026-07-27 · Fable (R2 staging session) · **R2 STAGED**:
  `CONTRACT_R2.md` authored against disk (stranded-lane mechanism traced:
  `note-episode-turn!` stamps before spawn, adoption reads the newest
  turn cell's `:episode-id`, the file belt covers only the lane-id case;
  waiter ordering read at `server_jetty.clj:1048–:1083`; autotag
  idempotency class verified at `material_circulation.clj:481–:605`;
  deploy seam at `bin/land:21–:31`). /rama + /rama-pitfalls loaded before
  design; staging-level pitfalls walk clean (back-arrow shape, hash-by
  :run/id colocation, no topology side effects, deterministic ids; the
  obligation-replay-after-terminal hazard found at staging → T6 + G3).
  R1 GATE doubts folded: 1→G2/§3c, 2→G8/§3g, 3→§5 INFO rule. Honesty
  note carried verbatim (§1). FULL tier declared. STANDING re-frozen for
  R2. Next: fresh build session runs P0 (rama design phases) per the
  starter below.

    Multi-cascade R2 — durable runner + log-consuming topology, first
    honest customer episode-retry. FULL gate tier (durable-touch). Boot:
    docs/current-mental-model/build/multi-cascade/CONTRACT_R2.md
    (binding) + NOW.md STANDING-R2 + decisions.md "How engine work
    lands". Load /rama + /rama-pitfalls BEFORE any topology work. PRE:
    clean tree (git status; foreign changes → flag, don't absorb).
    Re-verify CONTRACT_R2 §8 against disk, then run P0 (/rama phases
    0–2, artifacts beside the contract; re-run the rama-pitfalls
    protocol at plan level) → P1 (cascade_log module + G2/G3m/G4/G9m/
    G12) → P2 (seams + episode-retry row + G1/G3r/G5/G6/G7/G8/G9p/G10/
    G11), fresh context per phase (subagents fine). Bank every receipt;
    live receipts server-read with log4j INFO. Fences: CONTRACT_R2 §9
    allowlist; material_circulation.clj + verb_registry.cljc + all
    OC/RK/llm modules untouchable. Out of scope: clock/scheduler, retry
    backoff, auto-respawn of the resident, rows-as-material, any edit
    to existing durables. Stop clauses §6 — escalate, never improvise.

- 2026-07-27 · Codex · **R2 P0 STOP — genuine identity/lifecycle fork.**
  User accepted the two root probe artifacts as an untouched baseline; §8
  re-verification then passed semantically (including waiter ordering,
  adoption, deploy/test seams, and actual `:system` authorization). Fresh Rama
  Phase 0 produced `IMPLICIT_SPEC.md`; its full matrix found that a re-minted
  same-id fresh episode can fail twice: the pinned per-episode defunct imp-key
  then carries a later `:marked-at-ms` and fingerprint-conflicts, leaving the
  second failure unhealed. The fork + options + recommendation (transition-
  scoped import identity, stable projection order-key) are recorded in
  `decisions.md` Open Questions. No Phase 1 plan, source/test edit, test,
  deploy, commit, or push; fences and the two probe files untouched.

- 2026-07-27 · Fable · **RULED B — transition-scoped defunct import
  identity, stable order key** (on referral per §6/work-package path).
  Grounds verified against disk: precedent is `episode.clj:464-489`
  (turn cells — status-scoped imp-keys overwriting one stable cell,
  the law stated in the docstring); A leaves the second failure
  PERMANENTLY stranded (resume-failure is `fresh? false` → handler
  declines forever); B's advancement is causally monotone (fresh
  re-mint requires the prior marker visible; T6 + fingerprint-no-op
  close the regression routes — argument banked in T7). Pinned:
  imp-key `sha("episode-defunct " episode-id " " turn-id " "
  (name terminal-status))`, order key + value bytes unchanged. Sweep
  landed: §3f (idempotency string, identity pin, adoption law), T7
  rewrite, G3 repair half + G5 two-failure leg, §8 manifest line,
  IMPLICIT_SPEC (ambiguity section → ruled, T2 marker line fixed),
  decisions.md OQ deleted, board line. Next: fresh-context Rama P0
  re-run against the amended contract, then P1 per the R2 starter.

- 2026-07-27 · Codex · **R2 P0 FRESH; P1 PLAN STOP.**
  Clean-entry/HEAD `1d3e137` + every §8 seam reverified; probes accepted as
  untouched baseline. Fresh `IMPLICIT_SPEC.md` now covers 17 operations, five
  full matrices, and RULED-B's second-failure + late-replay lifecycle.
  Fresh Phase 1 created NO `PLAN.md`: §5 assigns G2/G4 to P1, but their
  `react!`/resume execution is reserved to P2 by §9 + the starter—two
  physically buildable phase readings. Separately, the pinned at-least-once
  observation record has no identity: an exact non-saturating late counter
  cannot be both retry-idempotent and bounded under the exactly-two-PState law.
  Both forks + options + recommendations are in `decisions.md` Open Questions.
  No source/test edit, test, deploy, commit, or push; fenced files + probe hashes
  unchanged. Next: Fable/Sid ruling + full ruling sweep, then fresh Rama P0.

- 2026-07-27 · Fable · **RULED B + B (P1 stop, both forks; on referral).**
  Fork 1: module-only P1 preserved; G2/G4 SPLIT into halves on the
  contract's own G3/G9 pattern — G2-module/G4-module in P1 (log +
  recovery substrate via IPC), G2-emitter/G4-sweep in P2 (react! barrier,
  resume-obligated!); §5 re-partitioned + sum-checked (all twelve, every
  half owned), §9 amended. Grounds: §3c/§3d verified — react!'s durable
  branch and the sweep's handler resolution are cascade.clj/boot-wiring
  machinery, P2 by §9; widening P1 would prove react! against a scaffold
  row only to re-prove it in P2. Fork 2: `:late-observations` count
  REPLACED by `:late-conflict` — nil | LAST conflicting observation's
  `{:status :receipt :observed-at-ms}`, overwrite-in-place: a VALUE
  converging under replay (§3b's own house law), bounded, and more
  diagnostic than a bare 0/1 at identical cost (late :completed after a
  :failed winner is exactly the evidence an operator needs). Sweep: §3b,
  §5 partition, G2/G3/G4, §9, IMPLICIT_SPEC (3 spots), decisions.md OQs
  deleted, board. Next starter (fresh context):

    Multi-cascade R2 — resume after the P1-stop ruling (B+B; contract
    amended in place). Boot: CONTRACT_R2.md (binding; §5 partition +
    G2/G4 halves + §3b :late-conflict are the ruled text) + NOW.md
    STANDING-R2 + this entry. Load /rama + /rama-pitfalls BEFORE any
    topology work. PRE: clean tree; probe-block-anatomy.* untracked,
    untouched. Re-run Rama P0 FRESH against the amended contract (the
    committed IMPLICIT_SPEC is prior-round input, not authority) → P1
    plan + validation → P1 the module (cascade_log.clj + IPC
    constructors ONLY; gates G2m/G3m/G4m/G9m/G12) → P2 (seams +
    episode-retry row; G1/G2e/G3r/G4s/G5/G6/G7/G8/G9p/G10/G11), fresh
    context per phase. Stop clauses §6 — escalate, never improvise.

- 2026-07-27 · Codex · **R2 P0 PRE-CODE STOP — §8 manifest count mismatch.**
  Fresh Phase 0 made no artifact: `CONTRACT_R2.md:511` says the whole
  `src/app/server/cascade.clj` is 47 lines; working tree + committed
  `HEAD 14fb406` are both 46. The named `declared-rows` / `rows` / `react!`
  shape matches, but §6 stops on ANY §8 manifest-line mismatch. Open
  Question records A contract count 47→46 (recommended), B inert code churn,
  C weaken the stop semantics, plus re-entry scope. No IMPLICIT_SPEC, plan,
  source/test edit, test, deploy, commit, or push. The probe hashes remain
  `3000c9f2…` / `e652d923…`; both ground source edits and the newly-visible
  foreign `ground_edit_test.clj` edit remain untouched. Next: Sid/Fable
  ruling + full ruling sweep, then fresh Rama P0.

- 2026-07-27 · Fable · **RULED — manifests bind on SUBSTANCE; class
  fixed, not the instance** (Sid: "fix it once and for all"). The count
  was a typo, but the stop's real cause is contract self-contradiction:
  §8's preamble said "line numbers drift" while §6 stopped on ANY
  manifest-line mismatch — and §9's allowlist orders edits to
  manifest-pinned files (P1: bin/land, test_runner.clj; P2: cascade.clj),
  so the literal reading had two MORE false stops scheduled. Sweep: §6
  clause 1 → SUBSTANCE mismatch only (hint drift re-locates + logs); §8
  preamble states the law; the 47-line pin DELETED (no count replaces
  it — P2 edits the file); EVERY §8 line machine-verified (grep/wc):
  all substance ✓, one more hint fixed (turn-record-request :472→:471).
  Class routed to the work-package skill (manifest-hygiene amendment).
  decisions.md OQ deleted; board updated. Next: fresh Rama P0, starter:

    Multi-cascade R2 — resume after the manifest ruling (§6/§8 amended
    in place; whole manifest machine-verified 2026-07-27). Boot:
    CONTRACT_R2.md (binding; §8 binds on SUBSTANCE — named
    symbols/forms/semantics; line hints re-locate + log, never stop) +
    NOW.md STANDING-R2 + this entry. Load /rama + /rama-pitfalls BEFORE
    any topology work. PRE: clean tree; probe-block-anatomy.* untracked,
    untouched. Re-run Rama P0 FRESH against the amended contract (the
    committed IMPLICIT_SPEC is prior-round input, not authority) → P1
    plan + validation → P1 the module (cascade_log.clj + IPC
    constructors ONLY; gates G2m/G3m/G4m/G9m/G12) → P2 (seams +
    episode-retry row; G1/G2e/G3r/G4s/G5/G6/G7/G8/G9p/G10/G11), fresh
    context per phase. Stop clauses §6 — escalate, never improvise.
