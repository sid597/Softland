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
- **Anchors (GREP — kernel line numbers shifted 3×, never trust PLAN cites):**
  R3 query `"relation-activity"` `[bucket-lo bucket-hi]` (fixed-width bucket
  strings); PState `$$relation-activity-by-bucket`; wrappers
  `rk/read-relation-activity` (public) + `rk/read-activity-rows` (V1);
  mirror-PState reads route ONLY via `(|hash$$ $$mirror *k)` — plain `|hash`
  silently mis-routes; advisory N3: pin the C7 container→source-ref join read
  path; gate 12 revises specs by RE-INGEST (new SourceVersionRow), never edit.

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
- **Track D — render north:** unopened; inputs ready
  (`design/claude/render-demands-2026-07-05.md` pre-read + PRIMITIVES.md);
  prior-art pass = cheap Opus lane, launchable anytime. Non-binding on
  build, binding on direction.
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
