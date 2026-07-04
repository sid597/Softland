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

## NOW (per-session log — append at session end)

- **2026-07-04, Fable — Track A session 1: WP1 contract OPENED.** Read
  INPUTS.md (14 items), decisions.md, relation-kernel CONTRACT + full
  `relation_kernel.clj`, object-container records/helpers/PStates/queries +
  ops module, Rama mirror/query references, BETS H1–H3, LOG 2026-07-04
  verbatims; one Opus Explore agent inventoried the ingest substrate.
  Artifacts: **`build/trail-view/CONTRACT.md` v1** (bundle/feed/trail/address
  shapes; verdicts = 3 stance kinds in the relation registry; activity
  projection + R3 in the relation kernel; read-only trail-view-module with
  mirrors only; 13-trap ledger; 16 gates; phased handoff);
  **object-kernel-revision RULED** (decisions.md: entirely gated as
  authority; two zero-cost disciplines adopted on Regime-1 grounds —
  two-clock naming, custody-vs-assertion recording); **D-008 PROPOSED**
  (decisions.md entry + Roam card). Verified facts the next session needs:
  envelope actor NOT persisted today (custody amendment is real, lands
  FIRST); md anchors CHAR offsets vs transcript BYTE offsets; OC decision
  rows stamp wall clock (`object_container.clj:428,448,467`) — activity
  write must not copy that; no cross-land recency index exists (feed =
  registry scans + R3). Answer to Track-B RETRO §6.5 doubt: WP1 demands NO
  sub-paragraph per-glyph provenance — anchors are span-level (char/byte
  ranges), text projection is plain text; T-1 does not grow. Judgment calls
  for countersign: CONTRACT §14 (9 items). NEXT: on countersign → Phase A1
  (custody) in a fresh implementer session; on objection → amend contract
  first. Track-B B2 is now unblocked on the contract side (needs Sid's
  countersign to be binding).

- **2026-07-04, Fable — same session, later: COUNTERSIGNED, package LIVE.**
  Sid countersigned in-session ("Countersigned as yes"): **D-008 CLOSED** in
  decisions.md; **CONTRACT.md v1 now BINDING** (header updated). All four
  Roam batches were approved (cards, track log, daily pointer live in the
  graph); a countersign-record block proposed under the track page.
  STANDING's pre-countersign guard line edited to reflect satisfaction —
  done as package-open completion, before any phase ran (trace note left in
  STANDING). **Phase 0 launched** (fresh-context spec re-derivation, Opus
  subagent, per Sid's standing lower-model instruction): deliverable
  `build/trail-view/IMPLICIT_SPEC.md` + findings classified
  implementer-fixable vs policy-fork; subagent barred from baton writes
  (one writer — parent session logs on its completion). Next after Phase 0:
  plan phase (fresh session/subagent), then default-fail plan validation.

- **2026-07-04, Fable — same session: Phase 0 COMPLETE, F-1 RULED, CONTRACT
  v1.1, plan phase LAUNCHED.** Phase-0 receipt (Opus, artifact
  `build/trail-view/IMPLICIT_SPEC.md`): 20 ops / 23 invariants / 26 matrix
  rows / 16 edge cases; ALL contract code citations verified, 0 failures;
  8 findings — 1 policy fork (F-1), 5 implementer-fixable, 2 platform notes.
  **F-1 ruled by Fable as contract author** (the fork was internal to the
  contract's own §6/gate-4/O(window) texts): feed window selection is
  ARRIVAL-ONLY; `:order :arrival|:claimed` orders within the window;
  claimed-window selection refused with a pre-named promotion path
  (claimed-keyed twin bucket index) — D-001 grounds, no consumer demands
  claimed windows. Sweep executed (§3 example, §6, gate 4 rewritten, trap 4,
  new §9.10) + F-2..F-6 fixes (§11 PState-access scope; §3 `:rev` =
  source-version key; §5.2 uniform 3-kind fold + max-status-changed
  dominance, tiebreak relation-id; §4 sibling-row rule; gate 7 supersedes).
  Recorded in IMPLICIT_SPEC §7 addendum + D-006 evaluation notes
  (letter-ding / spirit-held). **Incident:** raw NUL byte in CONTRACT §11
  (tool-JSON escape decode, 4th firing); file(1) said "data"; perl re-spell
  applied; now "UTF-8 text". STANDING PACKAGE GATE from here:
  file(1)-must-say-text on every doc/code write. **Plan phase (P1)
  launched** (Opus): deliverable `build/trail-view/PLAN.md` against
  CONTRACT v1.1 + IMPLICIT_SPEC; duties include the F-8 query→mirror-query
  scratchpad SPIKE (settle now, not in Phase B) and a baseline
  `clojure -M:test` run (stop on red). Next on its return: plan-validation
  phase, fresh context, default-fail.

- **2026-07-04, Fable — session CLOSE-OUT: PLAN COMPLETE, handoff to fresh
  sessions.** Plan receipt (Opus, artifact `build/trail-view/PLAN.md`):
  **F-8 spike VERDICT = WORKS** — query topology → mirror query via
  `invoke-query` proven live on create-ipc; single-roundtrip Phase-B design
  ADOPTED, client-composition fallback not needed. Spike also proved the
  load-bearing mirror rule: mirror-PState reads route ONLY via the mirror
  partitioner `(|hash$$ $$mirror *k)` — plain `|hash` silently mis-routes
  (recorded in PLAN as a hard rule + in implementation-quirks); one `|hash$$`
  sets the mirror-partition index for sibling same-module PStates (whole OC
  family in one hop); custom key-partitioner routing needs the EXTRACTED
  object-key. **Baseline suite GREEN** (2 tests, 165 assertions, 0
  failures). Plan covers A1/A2/A3/B with cited constructor call-sites,
  per-gate deftest map, fixture via the existing OC ingest-request builders,
  read plans with partition scopes inline. **No stop-clause escalations.**
  One recorded open item (PLAN §10): F-3 `:latest` follows CONTRACT §3
  (`read-latest-source-by-ref`) over IMPLICIT_SPEC C7 (contract > derived
  spec), with a CONDITIONAL escalation trigger — if re-ingest cannot yield a
  stable pinnable source-version-key for a spec doc, escalate per stop
  clause. NUL trap fired AGAIN in PLAN.md (5th repo firing) and was caught
  by the standing file(1) gate — the gate works; keep it. **HANDOFF: this
  Fable session ends here (token-economics ruling with Sid — no more
  Fable-priced wake-ups for mechanical phases). NEXT SESSION (fresh, cheaper
  model, one line: "Trail-view WP1 — run the next phase per this baton"):
  PLAN VALIDATION — fresh context, default-fail, scenario-trace PLAN.md
  against CONTRACT v1.1 + IMPLICIT_SPEC (incl. §7 resolutions) + the Rama
  refs; artifact `PLAN_VALIDATION_R1.md` (per-round files, never overwrite a
  FAIL). Then: A1 → A2 → A3 → B, one phase per fresh session. Fable
  re-enters ONLY on: stop-clause fork, twice-failed validation, or gates
  green (gate review).**

- **2026-07-04, Fable — SESSION ROUTING (standing for the rest of this
  package; Sid's ask: he opens every session conservatively on Opus; the
  session itself must catch a mis-route).**

  **Self-check rule — run this FIRST, before any phase work:** (1) find the
  next undone phase in this NOW log; (2) look it up in the table below;
  (3) your own system prompt says which model you are ("You are powered
  by..."). If the required model is NOT the one you're running on, your
  FIRST message must say: *"This session is `<phase>` — it needs
  `<model, effort>` because `<the why column>`. Please restart me there."*
  — then STOP; do no phase work. If the model is right but the effort
  isn't, say so and continue only if Sid says go (`/effort` is
  session-scoped). If a Fable re-entry trigger fires MID-session (genuine
  policy fork, second validation FAIL, gates green), do NOT attempt that
  work on Opus: write the escalation into this baton (and decisions.md
  Open Questions if it's a fork) and tell Sid to open a Fable session.

  | Phase (in order) | Model | Effort | Why |
  |---|---|---|---|
  | Plan validation (NEXT) | Opus 4.8 | xhigh | default-fail scenario tracing — the layer that caught both cycle-1 plan bugs; a miss here costs whole code rounds |
  | A1 custody impl | Opus 4.8 | high | careful code against cited call-sites |
  | A2 stance kinds | Opus 4.8 | medium | one registry line + grep + gates |
  | A3 activity + R3 | Opus 4.8 | high | 4th-hop write + replay determinism — discipline work, plan already rules it |
  | B trail-view module | Opus 4.8 | high | largest phase, but spike-proven mechanics + cited plan |
  | Diff falsification reviews (impl + tests, per skill layer 4) | Opus 4.8 | xhigh | fresh-context adversarial pass; caught the cycle-1 read-path divergence |
  | Stop-clause ruling | **Fable** | high | adjudication on binding docs — never improvised below Fable |
  | Gate review (gates green) | **Fable** | xhigh | the judgment layer; boots fresh from this baton (~40k context) |
  | Retro at close | Opus 4.8 | high | written from the trail |
  | Retro adversarial recheck | **Fable** (or strongest available) | xhigh | per skill: retros are written by the process they judge |

- **2026-07-04, Opus 4.8 (effort max) — Track A: PLAN VALIDATION R1 = PASS.**
  Fresh-context, default-fail scenario-trace of PLAN.md vs CONTRACT v1.1 +
  IMPLICIT_SPEC (+ its §7 resolutions) + code + Rama refs. Artifact:
  **`build/trail-view/PLAN_VALIDATION_R1.md`** (file(1)=UTF-8 text, byte-clean).
  Self-routing check: this is Plan validation → Opus 4.8/xhigh; ran on Opus 4.8
  at max (≥ xhigh, Sid set it explicitly) — no mis-route.
  **VERIFIED (affirmatively, with citations — default-fail earned):** read
  `relation_kernel.clj` IN FULL — every Phase-A edit-site citation EXACT (records
  :178-190; relation-outcome :314-395; actor bind :325/326; rejected-row helper +
  3 sites :309-312/:345/:351/:358; transition-row :271-286; endpoint-copy +
  copy-hops :288-297/:610/:618; envelope :705-718 + actor-default :714; journal
  dedup :562; accepted branch :579; to-side :617-621). Gate-9 replay determinism
  HOLDS — the kernel is entirely now-ms-free (even `decided-at-ms`=`ts` at :384,
  unlike OC's :428/:448/:467); duplicates dropped at :562 BEFORE the outcome;
  rejects skip the `<<if`; activity write is `termval` (write-only); vars carry
  across ≥2 partition hops (proven by existing `*row` at :587→:610→:618).
  `RelationActivityRow` 16-arg order correct; R3 out-of-`let` placement follows
  the working R1/R2 precedent; A2 one-line registry edit safe (grep: NO second
  kind enumeration in src/+test/). **Custody (A1) PROVABLY cannot regress the 165
  assertions:** no `->Relation*Row` literals in the test ns; T10 (:168) compares
  three copies of the same `*row` → custody-invariant for any actor; the existing
  distinct-`:actor` tests (llm :230, mallory :359/:400, empty :513) are all
  reject cases asserting on status/reason, not full rows. Two Opus subagents
  verified ALL object_container.clj + adapter + test citations (11 mirror PStates
  :1685-1770 all object-key-partitioned; ops module :2547-:2556;
  extract-object-key routing incl. `chat:`-segment kept; query arg vectors; join
  fields source-id :99-102 / source-ref :82-84; NO arbitrary-container-revision
  query exists) and ALL Rama-doc citations (mirrors.md skill tree + 13-/18- docs
  tree — every line MATCH; `|hash$$`→source-module task; sibling-index reuse;
  async-boundary snapshot honesty; launch-time dependency enforcement;
  leading-partitioner = latency-only). **NO correctness defect, NO citation
  failure, NO silently-resolved binding-doc conflict** (F-3 is contract-governed
  + §10-tracked; `:clock`→`:order` is spec-stale-flagged; plan follows contract).
  **ADVISORIES (non-blocking; fold into the phase):** N1 — §4.4 omits the
  `activity-order-key` row accessor its own §4.5 code calls (add it in A3;
  self-correcting at compile); N2 — A3 extract-placement style (extract before
  the hops per :591-592, optional); N3 — C7's container→source-ref join *read
  path* is the plan's thinnest spot (pin in Phase B: read the spec via
  `read-context-bundle` first, or hold an extra handle); N4 — cosmetic
  extract/leading-object-key label swap (both in :262-334); N6 — keep the bucket
  `"%08d"` width identical across §4.4 write / §4.6 R3 / §5.3 C2.
  **OPEN DOUBTS (falsifiers named; non-blocking):** (A) the F-8 spike is
  un-reverifiable here (throwaway modules discarded) — the docs make
  query→mirror-query inferable-but-not-exemplified for the query-topology case
  (18:68 is a stream topology); it rests on the baton attestation + the
  PRE-SPECIFIED client-composition fallback (unchanged wrapper signatures, §7
  "latency is not a gate"), so even if wrong Phase B has a defined path → the
  Phase-B smoke test is the falsifier; do NOT conflate invoke-of-mirror-query
  with a *leading* mirror-state partitioner (13:196 forbids only the latter,
  optimization-only — plan handles this at §10 F-7). (B) gate-12 must revise the
  spec by RE-INGEST (new SourceVersionRow), not an edit — plan §10 carries the
  conditional escalation trigger. Did NOT re-run the 165-assertion IPC suite
  (that is code-phase gate 16; kernel ns loads clean this session; baton attests
  it green twice today). **NO stop-clause escalation; NO Fable re-entry trigger
  fired.** **NEXT:** **A1 custody** — fresh session, Opus 4.8 / high, per §12 +
  PLAN §11; phase order A1→A2→A3→B stands. Roam track-A session-log block ready
  to post on Sid's word.

---

# Active work package: view-MVP WP-B2 (Track B) — the pixels

## STANDING (frozen at package open, 2026-07-04 — do not edit while active)

- **Binding docs:** `docs/current-mental-model/build/view-mvp/CONTRACT.md`
  (v1 **PROPOSED — awaiting Sid's countersign**; binding on countersign),
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
- **Does NOT start without Sid:** ANY phase (contract countersign first);
  P5+ additionally waits on WP1 gates green; any commit.

## NOW (per-session log — append at session end)

- **2026-07-04, Fable — Track B session 1 (same session as the retro,
  after Sid unblocked): WP-B2 package OPENED, contract PROPOSED.** Read
  WP1 CONTRACT.md v1 (binding) + decisions.md D-008/rulings tail. Key
  scope moves WP1 forced: View-3 text GENERATION is WP1 §8's
  (`render-bundle-text`) — B2's View-3 face is verbatim display +
  marker coloring, never re-wrap (trap 3); watchers are B2's (WP1
  §9.8/§12), placed OUTSIDE all Rama modules (ns
  `ingest-watchers`, epoch mirror atom — epoch is a counter, not truth);
  retro fix items mapped into contract (V3-5 → sanitize.cljc + shaper fix
  + atlas regen; T-4 → rect_tree clamp; T-6 → cached scene law; T-5 →
  gate-15 measurement artifact). Falsifiability strategy = pure-cljc face
  core + rect_tree .cljs→.cljc promotion so all 13 pure UI gates run on
  JVM against WP1-shaped fixtures (judgment call 1; the load-bearing
  placement). 17 gates + first-light; 13-trap ledger. RETRO §6.5 swept
  with Track A's answer (span-level anchors; T-1 unchanged). Charset
  audit subagent (Opus) launched → `build/view-mvp/CHARSET_AUDIT.md`
  (gate 4/5 fixture source). Countersign ask delivered as a Roam card on
  the track-B page (§14: 8 judgment calls). AUDIT LANDED (same session):
  `CHARSET_AUDIT.md` — ALL seven atlas JSONs (every font incl. slug meta)
  are the SAME 95-glyph printable-ASCII set; 69.8% of md docs / 26.8% of
  transcript messages carry ≥1 uncovered char; top-missing are structural
  (`─` 1.13M, `→` 371k, `—` 219k, `═`, `│`) — V3-5 confirmed with data,
  bigger than emoji edge cases. Gate 5 amended pre-countersign: must-have
  set now includes the U+FFFD fallback glyph itself (sanitizer renders
  nothing without it). NEXT: on countersign → B2-P0 (fresh
  session/subagent, spec re-derivation); P3 may run parallel to WP1 impl;
  P5+ waits on WP1 green. On objection → amend contract first.

---

# Cross-track baton (session-end entries + the window plan — verbatim)

## Session-end entry — 2026-07-04, Fable (Track B session 1: render-substrate retro)

TRACK: B (render substrate), step 1 DONE — the retro. Blind rule held: no
Track-D docs opened (gpu-component-library.md / component-library-jit.md
untouched). No code changed; two new docs only.
DECIDED (retro verdict, Fable judgment): **YES to both faces, NO rebuild.**
View-3 text face renderable today modulo wiring; threaded/DAG timeline face
needs only above-substrate additions (kernel-material card builders, lane
layout, Manhattan connectors from existing thin rects). No trail-face
requirement fails the disproportionate-cost rebuild test — closest
candidates (curved edges, font variants, zoom) each have cheaper outs or
belong to the parked canvas face. Output:
`build/render-substrate-retro/RETRO.md` (verdict + falsification pass +
trail-face-ordered fix list: V3-1..5, T-1..6, O-1..4 gated) and
`PRIMITIVES.md` (as-built inventory, Track-D input d). The only
substrate-file F0 change is V3-5 (glyph robustness).
VERIFIED (static; Fable direct-read ~7k lines, 2 Opus Explore inventory
agents for breadth): trail.cljs (1,337 lines) already renders trail→typed
markdown cards with wrap/collapse/nav/shimmer — the chat pane IS a card
timeline minus threading; all shaders carry pan+zoom (driven at zoom=1,
pan=scroll); text upload is full-rebuild while rects/shadows are
differential pools; only the editor virtualizes; dirty-present implemented
but OFF (`render.cljs:16`); new-view registration = the 7-step dg_flow
recipe (recorded in PRIMITIVES.md).
FOUND (defect-grade): (1) missing-glyph shaping bug — glyph misses skip
the x-advance (`renderer.cljs:1169-1184`), so emoji/astral/curly-quote
chars in ingested material silently vanish AND desync all count-based
width/wrap/clip math (V3-5, F0; editor never hits it, transcripts will);
(2) suspected bg-rect bleed at `:clip?` boundaries — partially-visible
nodes emit full-size rects (`rect_tree.cljs:221-228`), so a half-scrolled
card should paint over the chat header; statically derived, NOT
runtime-verified, repro in RETRO §3.2 (T-4); (3) file-layout tree built
2× per change (combined_text:357 text, editor_compute:423 rects) and
rebuilt per click in mouse.cljs — sidebar's cached `!sidebar-scene` is the
as-built correct pattern (T-6).
DOUBTED / OPEN: shaping throughput unmeasured — T-5 card windowing is
gated on the existing `[RAF]`>5ms instrument read against a real day's
material, not on estimates; atlas charset histogram of ingested material
unknown (V3-5 audit); WP1 shape risk recorded (RETRO §6.5: if the data
contract demands sub-paragraph per-glyph provenance, T-1 grows).
NEXT (Track B): B2 view-MVP contract consumes this verdict + WP1 shapes —
blocked on Track A's WP1 contract landing; the D×B1 delta step runs after
Track D's synthesis, using PRIMITIVES.md. Docs NOT committed (Track-A
session live on this branch mid-edit of decisions.md; commit is Sid's go —
files ready: `build/render-substrate-retro/{RETRO,PRIMITIVES}.md` + this
baton entry).
CONTINUATION (same session, after Sid's unblock message): WP1 contract
landed countersigned → B2 package OPENED this session — see the
"Active work package: view-MVP WP-B2 (Track B)" section above (contract
PROPOSED at `build/view-mvp/CONTRACT.md`; RETRO §6.5 swept; charset audit
commissioned). The D×B1 delta step still waits on Track D.

## Session-end entry — 2026-07-04, Fable (first firing of the adopted rule)

DECIDED: D-007 CLOSED (Sid countersigned in-session). Read-only MVP ruling
(Sid): first trail view is read-only; write surface = Claude CLI; watchers;
screenshot loop; read→write is the named second milestone (INPUTS item 14;
contract session proposes it as D-008 via Roam card). Face order RULED on
Sid's explicit delegation: View 3 → threaded/DAG timeline → canvas
(decisions.md). Solo-operator discipline rule ADOPTED (Sid, Roam card):
every session ends with an assertion-grade baton entry — this entry is its
first execution.
VERIFIED: relation-kernel suite green on committed code, fresh run this
session (2 tests, 165 assertions, 0 failures). All Roam batches approved
(morning orientation, 3 identity cards, triage, evening update); 2 small
Fable-confirmation blocks were pending Sid's approval at session end.
DOUBTED / OPEN: D-006 criterion-2 probe still unrun (fresh Opus + CONTRACT
§13 manifest — the one residue item needing a body); envelope/payload
server-side recheck now hard-gated BEFORE phase-1 daily use; intake sitting
and North vision sitting are Sid-paced.
NEXT: a FRESH session opens the trail-view data contract from
`build/trail-view/INPUTS.md` (14 items + the face-order ruling), per the
/work-package skill. Sid engages via Roam cards only.
LATE ADDITIONS (same session, after the entry above): the four-track window
plan (A rama / B render substrate / C read-write exploration / D render
north — section below) + the SESSION SCOPING LAW (one session = one track);
four Roam track pages created in the `softland` graph, each with an
append-only session log seeded 2026-07-04; the daily page carries the law
and the track links. Session closed clean; Sid signed off.

## Four-track plan (Sid + Fable, 2026-07-04 late — for the 36h Fable window)

**SESSION SCOPING LAW (Sid, 2026-07-04): one session = ONE track. Never mix.**
At session start: declare the track; read its Roam page (graph `softland`:
"track A - rama data + spine" / "track B - render substrate" /
"track C - read-write exploration" / "track D - render north") plus this
baton. At session end, write BOTH records: the assertion-grade baton entry
here (adopted rule) AND a dated block appended to the track's Roam session
log. The Roam track pages are the per-track state; this baton stays the
cross-track "now."

- **Track A — Rama (serial):** WP1 trail-view DATA-layer contract (Fable,
  fresh session, from INPUTS.md; pure Rama — bundles, queries, verdict rows,
  staleness, view addresses; also rules in-session: object-kernel-revision
  vs Regime-1 spine, and proposes D-008 read-only ruling as a Roam card) →
  WP1 impl (Codex/Opus) → WP2 spine contract (short; key adjudication:
  durable asserted edge vs projection-time query for transcript↔commit
  joins, + exactness-flag semantics) → WP2 impl. Needs Sid's go for WP2.
- **Track B — render substrate (parallel, disjoint files):**
  1. **Render-substrate retro** (Fable-xhigh, one session; Explore agents may
     inventory, judgment stays Fable): as-built adversarial review of
     `renderer.cljs` + `electric_flow.cljc` + workspace layer against ONE
     question — can this render View-3 text + the threaded/DAG timeline, and
     what is the minimal delta? Output: verdict + bounded trail-face-ordered
     fix list + primitive inventory. **Guardrail: output is a fix list,
     NEVER a framework design; a rebuild verdict must name the specific
     trail-face requirement current code cannot meet.** The framework is
     extracted AFTER a real face exists (D-001), not built ahead.
  2. **View-MVP package** contract (Fable) consumes retro verdict + WP1
     shapes; falsifiable UI acceptance gates are the hard part; then impl.
- **Track C — read→write exploration (Sid + Fable, design-track):** what
  writing looks/feels like in the land (select/type/sign made vivid;
  selection on a DAG; where assertion lives). Sid explicitly invokes the
  designer lens at session start; artifacts go to the design track; output
  binds phase 1 ONLY as a do-not-preclude constraint list — never scope.
  Schedulable anytime; needs Sid live.
- **Track D — the render layer's NORTH (added 2026-07-04, Sid's call —
  "ideal framework" study):** what the ideal Softland UI framework is,
  synthesized from (a) Sid's existing framework docs
  (`docs/architecture/gpu-component-library.md`, `component-library-jit.md`,
  + whatever Sid supplies from notes), (b) vision/LOG + BETS North (the
  feature horizon: continuous zoom, one-land-at-every-zoom, canvas, 3D,
  agent-manipulable view-specs-as-data, minimal-token legibility), (c) prior
  art (deep-research collection pass, cheap lane — vello/xilem, Makepad,
  Flutter layering, declarative models, ECS-UI; verify, don't trust
  training), (d) the retro's primitive inventory. **Guardrails:**
  non-binding on build, binding on direction — it supplies the DELTA
  INSTRUMENT (standing checklist at each face gate: "toward or away from
  North?") and tie-breaks equal-cost fixes; any fundamental-divergence
  finding becomes a CANDIDATE BET in BETS.md (pre-registered evidence,
  promoted at a sitting) — a rebuild happens only as a promoted bet, never
  as a slide. **Anchoring order:** the Track-B retro runs BLIND to this doc
  (fresh-cut rule); ideal × as-built × face-demands meet only at the delta
  step.
- Cheap lane throughout: WP1/WP2 impl phases, D-006 criterion-2 probe
  (Opus), model-UXR benchmark draft, `:workers 2` smoke, Track-D prior-art
  collection.
- Suggested Fable session order: A(WP1 contract) → B1(retro, blind) →
  D(ideal-framework synthesis) → D×B1(delta analysis, short) → A(WP2
  contract) → C(design sitting, anytime Sid wants) → B2(view-MVP contract)
  → gates as suites green.

Queue item 1 (work-package succession skill) DONE 2026-07-03, Fable session:

1. **Adversarial recheck of `build/relation-kernel/RETRO.md` ran FIRST** (as
   queued, xhigh): every scorecard/narrative claim traced to the 7 phase
   artifacts, the baton trail in git (per-phase commits on this file), the
   module + test source, and a fresh suite run on the committed code
   (`clojure -M:test` → 2 tests, 165 assertions, 0 failures). Verdict: retro
   substantially sound. Recorded in RETRO.md's "Adversarial recheck" addendum:
   4 corrections (stale "decisions.md not in git" residue; lesson-1 cost
   overcount; lesson-3 wrong causal story — read-plan clauses are hygiene, not
   a Phase-2 mover, since promises in binding docs don't self-enforce;
   lesson-2 slogan precision — partitioner-read key on a plain map envelope,
   typed records may ride inside), 2 new lessons (8: never overwrite a FAIL
   validation artifact — both PLAN_VALIDATION FAIL rounds are unrecoverable,
   only the baton saved the findings; 9: a ruling amendment is a sweep, not a
   banner — IMPLICIT_SPEC still says "ten gates"), 1 residue addition
   (importer-timestamp discipline must enter the D-003 extractor / trail-view
   contract).
2. **Skill written**: `.claude/skills/work-package/SKILL.md` — all five spec
   items from decisions.md (five-layer QC model / opening a package / gate
   review without Fable / re-entry triggers / retro procedure + the
   pre-registered retro-recheck step), mechanisms kept verbatim, lessons
   encoded in recheck-corrected form. decisions.md spec bullet marked
   EXECUTED; queue line struck.

COMMITTED 2026-07-04 (Sid's go; this branch only — never push, never merge to
main): three docs commits — (1) relation-kernel retro recheck + work-package
skill (skill force-added past the `.claude/` ignore rule, consistent with the
other already-tracked skills), (2) vision log (HCI thesis + April–June
back-fill + replies) + notebook photos + BETS H3/H5 updates + D-007 PROPOSED,
(3) trail-view INPUTS.md + this baton. Code files untouched across these
sessions (suite re-run only). Deliberately left uncommitted (not ours / not
docs): `.gitignore` (+external-resources/), `.agents/.../openai.yaml`,
`codex_implementation`, `scripts/`, `src-build/build/`.

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
