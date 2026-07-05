# ⚡ BATON — 2026-07-05 delivery session (READ FIRST; supersedes everything below)

**DONE THIS SESSION (delivery mode, Fable coded directly):**
**git-spine WP2 + trail-room R-1 GATE-PASSED, FIXED, COMMITTED, CLOSED.**
- Batched gate review `build/git-spine/GATE_REVIEW.md`: all 6 falsification
  should-fixes CONFIRMED + FIXED at gate (route custody validation ·
  deterministic `assert:<relation-id>` idempotency with optional override ·
  `:git-commit` dropped from route allowlist (B1 non-join) · per-line replay
  + per-file extractor isolation · UTF-8 pins · reader rebuilds from ALL
  stored keys). PLUS one gate-found defect (kraft-label overflow, fixed) and
  one root cause (rect_tree `clip?` REPLACED the ancestor clip → now
  INTERSECTS; expansion is clip? again; behavior-identical at every nil-clip
  production call site).
- Suite: **191 tests / 2202 assertions / 0 fail / 0 err** (one JVM, serial).
- CODE COMMITS (this branch; cherry-pick to main later): eac6dd5 spine P1P2 ·
  ed2db8a route PW · a1fda21 names P3+G11 · dc743d6 render R-1+clip fix ·
  7827f6f missionary regression ns + H11/H12 docstrings · 57b0113 chore
  strays (+/data/ gitignored).
- Light retros: `build/git-spine/RETRO.md`, `build/trail-room/RETRO.md`;
  D-006 wave-close note appended to decisions.md.
- AMENDMENTS Tier-2 H1–H10+H13 APPLIED (insights.md ×8, progressive-summary
  ×2, quirks ×2 + core-reframes (memory), reactive_master paths). **A4
  APPLIED on Sid's word (same session)** — CLAUDE.md slimmed to
  skill-pointers; Electric-3-Limitations absorbed (L13); disk-only edit,
  reversal path in AMENDMENTS.md.
- Hygiene: `_map.md` STALE banner; codex_implementation →
  `docs/history/codex-implementation-session7-audit.md`; strays committed.

**NEXT (in order):**
1. **SID'S APP BOOT — the finish line** (baton step 3 verbatim below in the
   old section): `clj -A:dev -X dev/-main` → `/trail timeline`. First boot
   runs replay→spine-sync→extract (long first sweep — watch `[TRAIL]` +
   `[GIT-SPINE]` server log lines). Collect gate-15 (`[RAF]` lines + feel
   verdict + one screenshot→re-resolve) → `MEASUREMENT_RAF.md` → **WP-B2
   CLOSED**. Threads render → screenshot the first kraft connector = the
   **H1 ARMING RECORD** (BETS verdict-log arming event). Write path live:
   `POST /api/relation/assert` (curl EDN body; asserter-id + keyword
   asserter-type now REQUIRED; `:git-commit` targets rejected).
   Open doubt to eyeball at boot: count DANGLING doc edges from the dual
   working-dir path mismatch (GATE_REVIEW open doubt 1) — they render
   honestly as kraft lines naming the far end.
2. **R-2 contract AUTHORED (Sid-AFK window): `build/trail-room/
   CONTRACT_R2.md` — PROPOSED, awaiting Sid's countersign.** Items 1/5/6
   (bands · lanes-from-edges · move chips) + both R-1 debts; probe
   obligations §2.5/trap 11 (order-as-data, no incremental bridges); 12-trap
   ledger; gates G1–G11 + W-1; process = delivery-mode wave + ONE contract
   validation round (Sid may waive). Build does NOT start without countersign.
3. Sid's remaining picks: **intake sitting** (`intake/2026-07-05-local-
   models.md` exists; Sid touched `vision/LOG.md` — committed e534f36) ·
   benchmark live runs (blocked on boot) · D-006 criterion-2 counterfactual
   probe (in flight this session if the window allows — see decisions.md
   evaluation notes).

**Old baton (2026-07-05 HQ marathon) kept below for step-3 detail; its
statuses are superseded by the above.**

---

# (superseded) SESSION-CLOSE BATON — 2026-07-05 HQ marathon

**CLOSED/BUILT TODAY:** WP1 **CLOSED** (light retro `build/trail-view/RETRO.md`).
WP-B2 gate-PASSED — closes on gate-15 evidence (Sid's app boot). **H1 clock
RULED: NOT started at first light; arming event = first render showing ≥1
typed relation over real material** (BETS.md verdict log; reversible on Sid's
review). Trail-room **R-1 BUILT** (rim v0 · addresses off cards · kraft marks;
gates green ns-level, `build/trail-room/PHASE_R1.md`). **git-spine WP2 BUILT**:
P1+P2 adapter/parents/extractor/replay (5t/113a green, `PHASE_P1P2.md`), PW
/assert route (4t/30a green; POST `/api/relation/assert`), P3 display-names
(G9 green; view-mvp fixture gate STRENGTHENED, `PHASE_P3.md`). **Electric
skill REBUILT from verified evidence** (14 platform-real / 3 agent-error / 3
unverified; 9-test regression ns `test/app/missionary_claims_test.clj`;
skill signed by Fable; `build/electric-skill/{HACKS-LEDGER,VERDICTS,
PROBE-EVIDENCE,AMENDMENTS}.md`). **Model-UXR benchmark v0 BUILT**
(`build/model-uxr/` — 32 questions, ablations A0–A4, dry-run harness;
6 join questions spine-gated). **CLAUDE.md A1–A3 APPLIED** under Sid's
in-session blanket (m/ap ban re-scoped · atomic-settle fenced · RAF sunset;
AMENDMENTS.md tier-1 note; revert-to-reverse). Sitting-3 queue captured
(`design/claude/sitting-3-queue-2026-07-05.md`; Roam mirror pending —
`docs/sessions/roam-pending-2026-07-05-sitting-3-queue.md`, bridge was down).

**INTEGRATION STATE:** serial **G10 GREEN — 187 tests / 2168 assertions /
0 fail / 0 err** (one JVM; both concurrent-run flakes proven contention
artifacts, not isolation bugs). Reviewer gates AUTHORED per WP1-gate
precedent: **G8 pair** (`test/app/server/rama/git_spine_gate_test.clj` —
route-written log line replays into a FRESH cluster with identical
relation-id) + **G11** (appended to `trail_view_test.clj` —
commit+session in one View-3 context). **BOTH GREEN AT CLOSE: G8 pair
PASSED (the §3.C serialization-drift risk is dead, empirically). G11 first
ran RED — a REAL catch: View-3's `edge-line` dropped the FAR endpoint, so
the projection silently lost WHO produced a target (map-must-not-lie
violation). FIXED in `trail_view.clj` `edge-line` (full triple
from → kind → to; this is one more uncommitted change in that file);
trail-view namespace re-run 4 tests / 118 assertions / 0 failures
including all pre-existing gates.**
**FALSIFICATION: LANDED + COMMITTED** (`build/git-spine/
DIFF_FALSIFICATION_R1.md`, 17:59). **Verdict: NO BLOCKING findings — all
gated happy paths real and tested.** SHOULD-FIXES = next session's first
coding batch (delivery mode, Fable codes directly): (1) route: asserter-id
UNVALIDATED → garbage gets a lying 200 AND poisons the assert-log; (2)
route: curl RETRY after timeout mints fresh ids → duplicate event rows;
(3) route allowlist accepts `:git-commit` targets that don't join (dangling
by design now — either drop from allowlist or document); (4) spine: replay
per-line and extractor per-file lack try/catch — one bad line/file kills
the whole pass; (5) render: removing the outer expansion `clip?` stripped
card-width text clipping from info/relations/holes/omissions sections
(only material got the `:material-clip` replacement) — uncaught by tests;
(6) reader field-pinning + charset DOUBTs (see artifact). Fix all six →
re-run the touched namespaces → ONE serial suite → gate artifact → commits.

**UNCOMMITTED CODE in the working tree** (commit ONLY after gate review;
per-package, code-only commits, never mixed with .md):
git_spine.clj + git_spine_test.clj (P1P2) · server_jetty.clj +
relation_assert_route_test.clj (PW) · trail_view.clj + trail_view_test.clj
(P3 + reviewer G11) · trail_face/{cards,scene}.cljc, workspace_actions.cljs,
combined_text.cljs, trail_face_test.clj + feed.edn (R-1 + P3 fixture) ·
missionary_claims_test.clj (verify) · git_spine_gate_test.clj (reviewer) ·
ingest_watchers.clj + file_viewer.cljc cfg (P1; NOTE: file lives at
`src/app/file_viewer.cljc`, not client/workspace/). bench/ + tools/model-uxr
are new uncommitted dirs (code-tier).

**NEXT SESSION, exact order:**
0. **MODE (Sid's close-of-session ruling — decisions.md D-006 notes,
   verbatim there): delivery-first. Fable codes directly; coding tasks
   batch FIRST, then ONE serial test batch, then ONE batched
   falsification+gate per wave — never sprinkled per-phase. CLAUDE.md
   slimming to skill-pointers is drafted as AMENDMENTS A4 (apply after
   Sid glances at wording).**
1. Falsification artifact + gate-test result → **Fable GATE REVIEW**
   (artifact `build/git-spine/GATE_REVIEW.md`; CLAUDE.md falsification
   protocol) → apply should-fixes → per-package CODE commits (fold
   AMENDMENTS H11/H12 code-docstring fixes into the matching commits) →
   close WP2 + R-1 with light retros; D-006 notes entry.
2. Apply AMENDMENTS.md Tier-2 H1–H10 + H13 (exact old→new pairs; re-grep
   anchors — line numbers drift).
3. **SID'S APP BOOT — the finish line:** `clj -A:dev -X dev/-main` →
   `/trail timeline`. First boot runs replay→spine-sync→extract (ingests the
   repo's commits + parent edges; expect a long first sweep — watch server
   log). Collect gate-15: `[RAF]` lines + feel verdict + one
   screenshot→re-resolve → write `MEASUREMENT_RAF.md` → **WP-B2 CLOSED**.
   **Threads render → screenshot the first kraft connector = the H1 ARMING
   RECORD** (BETS verdict-log arming event). Write path is live:
   `POST /api/relation/assert`.
4. Hygiene: baton queue item 1 below is STALE (that package closed 07-05);
   `docs/_map.md` needs a STALE banner (model-uxr SPEC §7 finding); strays
   triage (codex_implementation, scripts/, src-build/, manifest, atlas .bak,
   bench/, tools/, data/ ignore-or-keep).
5. Sid's pick thereafter: **R-2 contract** (binding probe obligations
   pre-recorded: order-as-data + C2-shaped consumer — PROBE-10K:15,153-155)
   · **intake sitting** (candidates: local-model sovereignty — see
   `intake/2026-07-05-local-models.md` if Sid's window produced it;
   behind-the-scenes distillation; MEMORY-off-land boundary — model-uxr
   SPEC §7) · benchmark live runs (needs threads + load claude-api skill
   before enabling stubs) · D-006 criterion-2 counterfactual probe.

**SESSION LESSONS (quirks-grade):** zero-work agent glitch fired ×2 (fresh
background agent returns boilerplate with 0 tool calls; fix = resume with an
explicit first action). Electric SNAPSHOT resolved TWO different jars the
same day (-44 render probe vs -45 claims tests — PROBE-10K:24; Missionary is
transitive+unpinned). **Test policy RULED by Sid:** ns-level tests during
phases; ONE serial full suite at integration (encode in future contracts).
Sid's time-box blanket ("adopt recommended, note alternatives") was
SESSION-SCOPED — it does NOT carry into the next session.

---

# Active work package: trail-view WP1 (Track A) — data layer

## STANDING (frozen at package open, 2026-07-04 — do not edit while active)

- **Binding docs:** `docs/current-mental-model/build/trail-view/CONTRACT.md`
  (WP1 contract v1 — **countersigned by Sid 2026-07-04 in-session**; binding)
  and `docs/current-mental-model/decisions.md`. This file is
  a baton, not a source of truth; if it contradicts CONTRACT.md or
  decisions.md, those win — flag the discrepancy in NOW.
- **Process:** one phase per FRESH session per
  `.claude/skills/work-package/SKILL.md` + `/rama` mechanics. Phase order:
  A1 custody → A2 stance kinds → A3 activity+R3 (relation-kernel amendments,
  gates 9/10/15/16) → B trail-view module + wrappers + text projection
  (remaining gates). Fresh-context validation, default-fail verdicts, never
  overwrite a FAIL artifact (per-round files).
- **File allowlist:** NEW `src/app/server/rama/trail_view.clj`,
  `test/.../trail_view_test.clj`, fixture resources. AMENDED
  `src/app/server/rama/relation_kernel.clj` ONLY per CONTRACT §5 (registry
  set; envelope `:request/sent-at-ms`; RelationDecisionRow/RelationEventRow/
  RelationEdgeRow custody fields; accepted-branch activity write; R3 query)
  + its test ns (additive, plus arity-forced mechanical updates). NOTHING
  else; object-container module untouched.
- **Verification duties (before code):** mirror PStates/queries from query
  topologies (`docs/reference/rama/18-module-dependencies.md`,
  `13-query-topologies.md:203-207`; fallback architecture CONTRACT §7);
  activity-write replay determinism (no wall clock in topology — OC's
  `decided-at-ms` divergence is the anti-pattern); harness launch order for
  module dependencies.
- **Definition of done:** CONTRACT §11 gates 1–16 green in one suite run,
  incl. gate 16 (pre-existing 165 assertions green).
- **Stop clause:** contract unbuildable / wrong under platform semantics /
  binding-doc conflict → classify implementer-fixable vs policy fork;
  escalate forks to decisions.md Open Questions as PROPOSED with verbatim
  citations. Never improvise policy.
- **Hard rules:** never read `src/app/server/env.clj`; code and docs in
  separate commits; commits only on Sid's word; docs only on this local
  branch, never pushed/merged.
- **Does NOT start without Sid:** WP2 (D-003 spine); any commit.
  (The contract-countersign guard that stood here was satisfied 2026-07-04
  in-session, before any phase ran — see NOW.)

## NOW (append ≤15 lines at session end; history → build/trail-view/SESSIONS.md)

- **2026-07-05, Fable — baton COMPACTED, process AMENDED.** All prior NOW
  entries moved verbatim to `build/trail-view/SESSIONS.md`. Ruling with Sid
  (decisions.md D-006 notes 2026-07-05): fresh context ≠ fresh session — QC
  layers run as fresh Opus subagents inside ONE orchestrating session;
  routing tables are advisory on model/effort, never binding on session
  structure; NOW entries cap ~15 lines. STANDING's "one phase per FRESH
  session" line is superseded (precedence: decisions.md wins; STANDING left
  unedited per protocol).
- **State:** Phase A COMPLETE (A1 custody, A2 stance kinds, A3 activity+R3);
  gates 9, 10-kernel-half, 15, 16 green; suite 2 tests / 222 assertions / 0
  failures; code + docs UNCOMMITTED (commits on Sid's word). Phase B
  (trail_view.clj + test ns + fixtures; remaining gates 1–8, 11–14) IN
  FLIGHT this session (Opus subagent), then batched A1+A2+A3+B
  diff-falsification (fresh subagent), then Fable gate review — same session.
- **⚠ PHASE B OWNERSHIP (corrected 2026-07-05 ~01:10):** Phase B belongs to
  the LIVE parallel Track-A session (actively writing trail_view.clj + test,
  ~50s save cadence, verified by md5 at 01:03–01:05 — client-composition
  path, mirror routing + N3 pin verified correct in snapshot). The Fable
  session's builders STOOD DOWN with ZERO writes (two dispatch attempts,
  both halted by collision protocol). The Fable session owns what follows
  green: independent suite re-run → batched A1+A2+A3+B diff-falsification
  (fresh subagent) → Fable gate review. Gate question parked: module took
  client-composition while PLAN adopted single-roundtrip (F-8 spike WORKS)
  — §7 says shape/honesty gate, latency doesn't; Fable rules at gate.
- **Anchors (GREP — kernel line numbers shifted 3×, never trust PLAN cites):**
  R3 query `"relation-activity"` `[bucket-lo bucket-hi]` (fixed-width bucket
  strings); PState `$$relation-activity-by-bucket`; wrappers
  `rk/read-relation-activity` (public) + `rk/read-activity-rows` (V1);
  mirror-PState reads route ONLY via `(|hash$$ $$mirror *k)` — plain `|hash`
  silently mis-routes; advisory N3: pin the C7 container→source-ref join read
  path; gate 12 revises specs by RE-INGEST (new SourceVersionRow), never edit.

- **2026-07-05, Fable (marathon session, Track-B-origin) — PHASE B GREEN;
  ownership clarified; falsification CLAIMED.** The "live parallel session"
  writing trail_view.clj was THIS session's fresh-context Opus subagent —
  cross-track work Sid authorized in-session ("Everything, one marathon":
  B2-P5 is contract-gated on WP1 green, so this session built Phase B to
  unblock it). Receipt: 16/16 WP1 gates green; trail-view suite 445
  assertions; cross-package run 28 tests/909/0. Path = CONTRACT §7
  client-composition fallback (wrapper shapes identical; §7 says latency is
  not a gate). PLATFORM FINDING: Rama 1.6.0 mirror local-select> REJECTS
  `:allow-yield?` — §11's allow-yield rule cannot bind mirror reads (quirks
  updated). **CLAIM: the batched A1+A2+A3+B diff-falsification is RUNNING in
  THIS session** (fresh Opus, launched ~01:20) — do not double-run.
  PROPOSED SPLIT: the parallel Track-A Fable session takes the Track-A GATE
  REVIEW (fresh eyes on a build this session orchestrated — strictly better
  QC; its parked client-composition-vs-PLAN question rules there); this
  session gates Track B. D-006 note ec69e74 is canonical — no second note.
  Docs/code uncommitted (Sid's word).

- **2026-07-05, marathon — FALSIFICATION R1 COMPLETE; A1+A2+A3 review debt
  DISCHARGED (kernel verdict: SOUND); Phase B: NO blockers.** Artifact:
  `build/trail-view/DIFF_FALSIFICATION_R1.md` (probes-held ledger + 7
  first-light notes; the 1 should-fix was in a Track-B file, applied).
  Track-A GATE REVIEW remains with the parallel Fable session per the
  split above — its inputs are that artifact + the Phase-B receipt.

- **2026-07-05, Fable (Track-A session) — WP1 GATE REVIEW: PASS. Package
  gate-complete.** Artifact: `build/trail-view/GATE_REVIEW.md`. Code read in
  full; suite independently re-run twice (final **3 tests / 302 assertions /
  0 failures** = 222 A-phase + 74 Phase B + 6 reviewer-added). Gap found AT
  gate: gate 10's trail-view half (custody projection) had no executed
  assertion — closed by a reviewer-authored TEST-ONLY block (bundle
  `:written-by` + "via" text badge + convergent absence), green first run.
  Client-composition ruled CONTRACT-SANCTIONED (§7 fallback; letter-ding/
  spirit-held in the artifact). Traps 3/4b/10/12 spot-checked, HELD. 8
  non-blocking open doubts with falsifiers in the artifact (top: `:written-by`
  present-with-nil vs "omitted"; text-projection arrow direction; feed
  address round-trip untested). decisions.md D-006 note appended. **NEXT:
  commit + package close on Sid's word (code and docs separate); light retro
  can fold into close. WP-B2 P5+ is unblocked (WP1 gates green + gated).**

---

# Active work package: view-MVP WP-B2 (Track B) — the pixels

## STANDING (frozen at package open, 2026-07-04 — do not edit while active)

- **Binding docs:** `docs/current-mental-model/build/view-mvp/CONTRACT.md`
  (v1 — **countersigned by Sid 2026-07-04 in-session**; BINDING),
  `build/trail-view/CONTRACT.md` (WP1, binding — its §7 wrappers are this
  package's ONLY data surface), and `decisions.md`. This file is a baton,
  not a source of truth; if it contradicts CONTRACT.md or decisions.md,
  those win — flag the discrepancy in NOW.
- **Process:** one phase per FRESH session per
  `.claude/skills/work-package/SKILL.md`. Phase order: B2-P0 spec
  re-derivation → P1 plan → P2 plan validation (default-fail, per-round
  artifacts) → P3 pure core + fixtures + gates 1–13 (parallel-safe with WP1
  impl) → P4 gates green + substrate amendments → P5 integration (**waits
  for WP1 gates green**) → P6 watchers + first light (H1 clock) → P7/P8
  validations → Fable gate. NOT the /rama skill (no Rama modules touched);
  CLAUDE.md Missionary/Electric patterns + implementation-quirks ARE in
  force.
- **File allowlist:** CONTRACT §12 verbatim — NEW `src/app/client/
  workspace/trail_face/*` (cljc + wiring.cljs), `src/app/server/
  ingest_watchers.clj`, tests + fixtures, atlas regen; RENAMED
  `rect_tree.cljs → .cljc` (+T-4 clamp only); AMENDED (mode-branch/wiring
  additions only): combined_text, editor_compute, scroll, mouse,
  workspace_actions, render, runtime, state, agent_flow, file_viewer,
  electric_flow, util_fns (epoch atom only), renderer.cljs (two shape fns
  only). trail.cljs / dg_flow / sidebar / settings / cmd_panel untouched.
- **Verification duties before code (P1):** import entry points + their
  observable completion (read runtime.clj/adapters, not memory); client
  cljc compiles under `clojure -M:test` (spike one require); wheel-cascade
  order in scroll.cljs; shadow-cljs resolves the cljc rename.
- **Definition of done:** CONTRACT §11 gates 1–17 green + first-light
  artifact (gate 15 evidence) recorded.
- **Stop clause:** unbuildable / platform-wrong / binding-doc conflict →
  classify implementer-fixable vs policy fork; escalate forks to
  decisions.md Open Questions as PROPOSED with verbatim citations.
  Pre-flagged: WP1 shape amendments → fixtures re-derive
  (implementer-fixable); cljc-on-JVM classpath failure → policy fork
  (placement ruling 2.1 is load-bearing), escalate.
- **Hard rules:** never read `src/app/server/env.clj`; code and docs in
  separate commits; commits only on Sid's word; docs only on this local
  branch, never pushed/merged.
- **Does NOT start without Sid:** P5+ waits on WP1 gates green; any commit.
  (The contract-countersign guard that stood here was satisfied 2026-07-04
  in-session, before any phase ran — see NOW.)

## NOW (append ≤15 lines at session end; history → build/view-mvp/SESSIONS.md)

- **2026-07-05, Fable — baton COMPACTED** (same ruling as Track A; all prior
  NOW entries verbatim at `build/view-mvp/SESSIONS.md`).
- **State:** P0 (IMPLICIT_SPEC + CONTRACT v1.1) + P1 (PLAN.md) + P2
  (PLAN_VALIDATION_R1 = PASS; advisories A1–A8; open doubts D1–D2) COMPLETE.
  No code written yet. Docs uncommitted (Sid's word).
- **NEXT: B2-P3** — pure core + fixtures + gates 1–13, parallel-safe with WP1
  impl; runs under the amended process (fresh subagents inside one
  orchestrating session). Fold at write time: A1 (gate-1 round-trip is pure
  EDN, no live resolve-address), A5 (`:panes` cases are a HARD must — no
  default in the case), A6 (corrected paths), A2/A3/A4/A7/A8 at their named
  sites. P5+ WAITS on WP1 gates green. Cross-package: OI-1 (no production OC
  runtime handle — shared with WP1, Sid/WP1 coordination at P5); OI-2
  (manifest default font vs regenerated MSDF atlas — Sid decision at P4).

- **2026-07-05, Fable (marathon; Sid collapsed the shell in-session) —
  P3+P4+P5+P6-server COMPLETE; gates 1–14+16 green, 17 mechanically green.**
  Pure core (5 cljc + wiring.cljs) + ASCII fixtures (\uXXXX salts) +
  rect_tree.cljc clamp; gates 1–13 as 13 deftests. Atlas = MERGED
  Ubuntu+DejaVu MSDF (Ubuntu lacks ALL arrows/shapes/star/warn — 340 cps;
  uniform 0.56 advance; flat schema preserved); OP-27 shaper fix landed;
  manifest default → merged MSDF per **Sid's OI-2 ruling: MSDF is TEMPORARY
  verification, SLUG stays the destination** (slug glyph-set expansion =
  named follow-up). OP-35 two-part done; watchers + gate 14 via subagent
  (decision-latch, no polling, loop survives poisoned file); 7-step wiring +
  Electric bridge (Trail* e/defns; trail-rt = defonce-delay IPC boot — OI-1
  first-light risk, named). Gate-16 fidelity test CAUGHT real drift
  (verdicts = full edge rows; :omission/kind; no :display-name; anchors
  +:block-path) → fixtures + cards re-derived same session. Suite 28/909/0;
  shadow :dev compiles (prod build pre-broken: prod.cljc wants Electric v2).
  REMAINING: falsification verdict → fixes → Track-B gate HERE; first light
  with Sid (gate 15 artifact + H1 clock).

- **2026-07-05, marathon — TRACK-B GATE REVIEW: PASS, conditional on first
  light.** Artifact: `build/view-mvp/GATE_REVIEW_B2.md` (gate ledger, S1–S5,
  first-light risk register — read it before driving). Falsification
  should-fix applied (scene cache now value-equality, not hash) + shadow
  recompiled green; falsification artifact at
  `../trail-view/DIFF_FALSIFICATION_R1.md`. Gate 15 + visual halves DEFER to
  first light. **NEXT: first light with Sid** — `/trail timeline` over real
  material; expect a seconds-long stall on the FIRST pull (in-process IPC
  boot — risk register item 1); record FIRST_LIGHT.md + MEASUREMENT_RAF.md;
  H1 clock starts. Slug glyph-set expansion = named follow-up (OI-2).

---

# Cross-track (compressed 2026-07-05 — verbatim history at `docs/sessions/session-log.md`)

- **SESSION SCOPING LAW (Sid, 2026-07-04): one session = ONE track, never
  mix.** Declare the track at start; read its Roam page (graph `softland`) +
  this baton. Session end: baton entry (≤15 lines) + Roam track-log block.
- **Track A — Rama (serial):** WP1 live above → then WP2 D-003 spine (needs
  Sid's go; key adjudication queued: durable asserted edge vs
  projection-time query for transcript↔commit joins, + exactness-flag
  semantics).
- **Track B — render substrate:** WP-B2 live above. The D×B1 delta step
  waits on Track D's synthesis (inputs:
  `build/render-substrate-retro/{RETRO,PRIMITIVES}.md`).
- **Track C — read→write design (Sid live):** sitting 1 DONE 07-04/05 —
  face-test law (trail always on screen · every node reads at
  title/decision/2-liner · open-in-place while others hold), 27-item
  do-not-preclude ledger (the ONLY phase-1-binding output), horizon + dummy
  artifacts in `design/claude/`. Roam batch 3 pending re-proposal
  (`docs/sessions/roam-pending-2026-07-05-track-c.md`). Queue on its Roam
  page.
- **Track C sitting 2 DONE 2026-07-05 (Fable, designer lens).** Rulings
  R1–R7 in `design/claude/room-card-lane-2026-07-05.md` (all PROPOSED,
  awaiting Sid on sight); drivable sketch `trail-room-sketch-2026-07-05.html`
  (altitude toggle, open-in-place, kraft edges, the band — real repo
  history); 3 decision-log entries appended. Gist: room = ground + rim only
  (HQ's full-screen toggle CONFIRMED w/ constraints C1–C3); cards =
  typography closed / surface open, 4 bands, address off the card face
  (answers F-L5); lane = asserted thread over lineage kinds, unthreaded =
  self-declaring BAND, family-key demotes to fold rule (answers F-L2).
  **FOR HQ:** the handoff block "what the render contract must build next"
  (7 falsifiable items, rulings doc end) — rec order: rim+address first
  (kills worst F-L5 on sight), kraft marks before H1 arming screenshot,
  lanes-from-edges after tonight's git-spine edges land. Docs uncommitted —
  HQ coordinates commits. Roam: batch 3 landed+approved; HQ proposes the
  sitting-2 session-log block separately — Track C stacked no Roam writes.
- **Track D — render north: STUDY DONE 2026-07-05** (Fable + 9 Opus sweeps:
  3 code/artifact readers, 6 web researchers, all primary-source-verified).
  Deliverables: `build/render-north/INPUTS.md` (evidence manifest — never
  re-gather) + `NORTH.md` (the framework north) + `MECHANICS.md` (Sid-
  commissioned same session: the browser-services-rebuilt layer — hit-test/
  stacking/scale/clip/text/gestures/animation/wire/degradation, each with
  mechanism+today+north+traps; NEW wall-grade finding **N6: f32-absolute
  coords jitter at land scale — scene store must hold f64, GPU gets
  camera-relative f32**, verified as-built at renderer.cljs:7,41,1107)
  + `HARD-PROBLEMS.md` (after Sid's "Zed bled — is that it?" challenge: the
  bottomless five — diff-transducing spec COMPILER not interpreter (H1/H2),
  frame pacing + epoch rule, the text pit and where the mono moat ends,
  optimistic write overlay, transclusion identity `(view,addr)→slot` — with
  3 explicit corrections to NORTH/MECHANICS; birth-critical pair = store
  keying (H6) + coordinate discipline (N6)). **DECIDED (direction-grade,
  non-binding on build):** camera-over-addressed-world ACCEPTED w/ two
  amendments (islands lay out in local 2D then project — never zoom-aware
  flexbox; root noun = ADDRESS, not camera); **view-specs are ASSERTIONS in
  the land** (Rerun-Blueprint-on-Rama) — versioning/collab/on-the-fly inherit
  from the log; two code layers only (pipeline + render-type/action
  registries, D-001-paced); actions become data (closes the rect-tree closure
  hole); LWW for layout only, epistemic state accumulates (law N5).
  **VERIFIED:** Electric 3 diff = incseq 6-op protocol, diffs-only on wire;
  K1 proofs real (MapLibre expressions, rfw, deck.gl/json, Blueprint, MCP
  Apps); NO rebuild-grade divergence — NORTH §8 embryo table (every north
  noun has a living as-built seed, incl. dormant `gpu-mount` incseq bridge in
  `buffer_pool.cljs`). **DOUBTED:** Electric at 10⁴ deltas (Gap 3) — now a
  pre-registered evidence item (NORTH §9: synthetic 10⁴ incseq → scene store
  → `[RAF]` instrument, at face-gate time); either outcome keeps the model.
  Fork 2 left OPEN (founder's) with rec: one substrate, three projection
  families over one address space. **NEXT:** Sid reads NORTH.md; Fork-2
  sitting at his call; D×B1 delta instrument now has its Track-D input.
- **Track D — PROBE-10K MEASURED 2026-07-05** (render-bench session; NORTH §9
  pre-registered evidence item DISCHARGED). Artifact:
  `build/render-north/PROBE-10K.md` (setup, tables, knee, threats). Headline:
  the knee is diff SHAPE, not element count. Change/append/tail-shrink hold at
  10⁴ end to end (server mint 2–5ms, apply ≤4.2ms p95, clean 60fps, idle O(1))
  — Gap 3's conditional-RAF change signal is real on Electric's own protocol.
  Reorder/front-drop/ID-churn diffs knee at 10³ (mint ~0.1s) and die at 10⁴
  (12–74s) — inside `->seq-differ` (the e/diff-by path), upstream of wire AND
  consumer (a 10⁴ permutation applies in 6.5ms via order-indirection).
  Fallback (client re-diff) holds to 10³; flat 17–30ms/frame at 10⁴ (30fps).
  Obligations recorded: order/window = data on rows, never incseq permutations;
  store consumes the six ops directly — as-built gpu-mount bridge CORRUPTS
  under :permutation (demo: 16,750 slots for 100 entities). Model kept; no
  rebuild candidate. NEW `bench/src/render_probe/*` (4 files) uncommitted; no
  product code touched. Feeds the D×B1 delta instrument + face-gate 15.
- **Local-model sovereignty — intake + recon DONE 2026-07-05 (Fable; Sid
  engaged same-day in-chat).** Deliverable:
  `intake/2026-07-05-local-models.md` (Candidates-sitting pre-read;
  BETS.md/decisions.md untouched). Three D-007 drafts: **LM-1**
  structure-substitutes-for-scale (the H2 link — model-uxr's
  pre-registered small×ablation headline; probe = fill the existing
  `nemotron-local` subjects.edn slot); **LM-2** low-wrongness slots go
  local (multi-arm blind A/B on band-2/display-name enrichment); **LM-3**
  own-material QLoRA (gated: saturation probe-0 + `asserted-by=sid`
  census). Sid same-day (verbatim in-file; LOG landing due at sitting):
  the unit is a POOL routed model×slot (Gemma-class for low-reasoning
  slots); NO closed-provider distillation ever — open-legal only (DeepSeek
  MIT/Qwen3 Apache clean; Gemma ≤3 distills propagate its license; custody
  D-008 §5.1 = the clean-corpus filter). Recon: Nemotron fits 48GB only
  quantized; 16-bit LoRA 60GB → dense-32B tune path. 3 frontier Qs in-file.
- **Standing cross-track pointers (from Track C, phase-1-binding):**
  Track A ← ledger 13 (altitude text forms are data: title·2-liner·full per
  bundle), 16 (mark/question counts survive folds), 17 (walk/attestation
  rows assertable). Track B ← design glyphs ⊢│├└• vs 95-ASCII atlases
  (extensible glyph pipeline before face 2) + wall W4 (face 2 =
  camera-model, never DOM-outliner). Electric-at-land-scale (Gap 3, 10⁴+
  elements) = THE powder question before face 2 scales.

## Queue (Sid sequences; per decisions.md Fable-window queue)

1. **Trail-view data contract** (next Fable-window item) — the contract +
   acceptance gates for D-002's first form, consuming the relation kernel via
   its two query topologies. Open it per the new `/work-package` skill, in a
   FRESH session. **INPUT MANIFEST: `build/trail-view/INPUTS.md`** (gathered
   2026-07-03/04: Sid-decided items incl. bundle=everything-layered,
   staleness-renders-differently, minimal-token agent legibility,
   view-specs-as-data; carried relation-kernel obligations; Fable-proposed
   verdict-capture + select/type/sign gesture). Prior trail-view/design docs
   are input NOT authority (Sid 2026-07-04: "the prev docs were written by
   lower reasoning fables") — ground truth is decisions.md + code + INPUTS.md.
2. **D-003 Regime-1 spine** (needs Sid's go): transcript→commit/doc join
   extractor + git-commit-metadata adapter emitting `:produced`/`:based-on`
   assertions with evidence anchors, feeding the 27-04 trail view.
3. Slot-anywhere, cheap-model: D-006 criterion-2 counterfactual probe (fresh
   Opus, input manifest = CONTRACT §13); `:workers 2` smoke test of the
   relation kernel; make the test-harness task count injectable (not only
   `rand-nth`-randomized) whenever the harness is next touched, so partition
   sweeps are reproducible.

Added 2026-07-03 (bet-foundry session, same day):

4. **D-007 awaits Sid's countersign** (decisions.md): the bet foundry —
   claims→bets intake via the BETS.md Candidates inbox + the Fable questioning
   practice (1–3 frontier questions per vision/bets-touching Fable session).
   Sid's verbatim proposal + HCI thesis landed in `vision/LOG.md` (2026-07-03
   entry).
5. **First BETS.md intake sitting** (Fable-grade, Sid live): seed the empty
   Candidates inbox. **PRE-READ: `docs/current-mental-model/intake/
   2026-07-04-painting.md`** — the full typed decomposition of the 07-03/04
   sessions (12 threads, parallelism map, carried questions). Sources:
   `vision/LOG.md` (HCI thesis, economy vision, April–June research notes)
   plus the wall panels. Status of the three posed frontier questions:
   Q1 (beachhead) ANSWERED — H3 re-sequenced in BETS.md 2026-07-04; Q3 (unit
   that leaves the chatbox) ANSWERED — "everything, raw + native + view-forward"
   (feeds the trail-view contract); Q2 (minimum viable discipline for a solo
   operator) still OPEN. New question posed 2026-07-04: what is the
   commit-gesture for thought (what mints a semantic version of an
   exploration)? — feeds the trail-view contract directly.
6. **Vision back-fill** — PROGRESSED 2026-07-04: April 28 / June 6 / June 11 /
   June 12 raw notes + the ~June 28 notebook pages landed in `vision/LOG.md`
   (photos preserved at `vision/images/2026-06-28-notebook-p{1,2}.png`).
   More may remain in Sid's notes apps; pull in under original dates when he
   supplies them.

Added 2026-07-04 (deep-thinking thread):

7. **Question-unit design** (Fable window; Sid: "lets make it a dedicated
   thing... have a solution for it"): the atomic unit of a question —
   hole-shaped relation rows (`? based-on B`), announcement-at-pattern-match,
   frontier = high-density holes. Sequencing: the trail-view contract only
   keeps the door open (INPUTS.md item 5); the dedicated design slot comes
   after/alongside that contract, build paced by D-005 (the view orders it).
   **D-001 evidence acquired 2026-07-04**: the decisions.md open-questions
   wall broke against Sid in daily use (verbatim in LOG; requirement recorded
   in INPUTS.md item 5) — this item now has a real form-break behind it, not
   an imagined demand. Interim: open questions go to Sid as Roam cards.
8. **Model-UXR benchmark design** ("user research for models", H3 machine
   half — BETS.md H3 2026-07-04 sharpening): frozen land snapshot +
   orientation-question bank (gold answers derivable only from the trail) +
   permission-scoped ablations (drop decisions.md / baton / relations, measure
   degradation). Metrics: tokens-to-orientation, wrong-authority rate,
   re-derivation ratio, join-question success, invented-structure rate.
   Cheap-model buildable once the trail view's queries exist; reruns as CI.
9. **Interim working protocol** (proposed by Fable 2026-07-04 after the
   form-break thread; zero-build relief, effective unless Sid objects):
   (a) thread-scoped sessions, not 400k monoliths — one painting-thread per
   session, Fable carries orientation via this baton + the intake pre-reads;
   (b) Sid engages decisions/questions ONLY via Roam cards — markdown stays
   the machine-facing record, maintained by models; (c) every session ends
   with an assertion-grade baton entry (the Q2 rule — **ADOPTED by Sid
   2026-07-04** via Roam card "yes adopt this"; also in decisions.md and
   Claude memory). Face order also RULED 2026-07-04 on Sid's delegation:
   View 3 → threaded/DAG timeline → canvas (decisions.md Open Questions).

Docs tracking (Sid, 2026-07-03): full docs tree tracked on this branch
(`84b3d82`), including `decisions.md`; docs commits ONLY on this local branch,
never pushed, never merged into main; code and docs always in separate
commits.

Session hygiene for whoever writes next: `memory/implementation-quirks.md` →
"NUL bytes in source" (the tool-JSON NUL-escape trap fired twice MORE in the
recheck session, both times while writing docs ABOUT the escape — file(1)-check
anything that mentions it) and "Microbatch test barrier" (reuse for every
kernel suite).
