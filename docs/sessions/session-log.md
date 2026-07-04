# Cross-track session log — verbatim archive

Moved verbatim from `docs/sessions/next-prompt.md` on 2026-07-05 (baton
compaction). Session-end entries and the four-track window plan as
originally written; the baton keeps the compressed current state and the
queue. Future session-end entries: ≤15 lines in the baton, overflow here.

## Session-end entry — 2026-07-04/05, Fable (Track C sitting 1: write gesture → horizon)

TRACK: C (read→write exploration, design track), sitting 1 — ran 07-04 into
07-05, Sid live throughout; designer lens Sid-invoked. No code, no build docs
touched; all artifacts in `docs/current-mental-model/design/claude/`.
DECIDED (Sid, LAW-grade — the founder's face test, verbatim in vision/LOG
2026-07-04): trail always on screen · every node reads at title/decision/
2-liner · open-in-place while others hold. Derived: detail costs local space,
never the shared axis; layout deterministic, never force-directed. Sid also
ruled: design docs are thought-space (decisions are the designer's, marked);
design is a long task (Track-C Roam page = the design log); imagination is
unbound — D-001 governs build, never dreaming ("the designer unbound",
vision/LOG 2026-07-05); no biology examples — swap in other domains.
MADE (all drivable): `write-gesture-sketch-2026-07-04.html` v6 (gesture beats,
1,000-thread scale stage, the walk, terrain descent; **27-item do-not-preclude
ledger** = the only phase-1-binding output) · `softland-horizon-2026-07-05.html`
(the full-scale dream, laws-on) · `softland-dummy-2026-07-05.html` (walkable
simulation, shareable) · `render-demands-2026-07-05.md` (**Track D pre-read**:
15 demands + type-hypothesis + Part II emperor→engineering-head brief: five
machines, five load-bearing walls, gunpowder audit, shit-patching tripwires).
CROSS-TRACK POINTERS: Track A ← ledger 13 (altitude text forms are data:
title·2-liner·full per bundle), 16 (mark/question counts survive folds), 17
(walk/attestation rows assertable). Track B ← charset collision (design glyphs
⊢│├└• vs 95-ASCII atlases — extensible glyph pipeline needed before face 2)
+ wall W4 (face 2 must be camera-model, never DOM-outliner). Track D ← the
render-demands doc is input (e); prior-art pass (input c) still unrun, cheap
Opus lane, launchable anytime.
DOUBTED / OPEN (Sid's cards, in Roam batch 1): stepping-attests-visibly OK? ·
altitude auto-raise policy (rec: nothing but walk+pins) · version names vs
dates (rec: date-born, name-graduating) · does "sid, by fable's hand" feel
like his act. Electric-at-land-scale (Gap 3, 10⁴+ elements) named as THE
powder question for the engineering head — answer before face 2 scales.
RECORDS: vision/LOG.md +7 verbatim entries (07-04/05); design decision-log +
taste.md appended; Track-C Roam page: batches 1+2 proposed (status
UNVERIFIED — the write bridge went down at sitting close; check in Roam);
batch 3 NOT posted — saved for re-proposal at
`docs/sessions/roam-pending-2026-07-05-track-c.md`. Docs UNCOMMITTED —
awaiting Sid's go, docs branch only, separate from any code.
NEXT (Track C queue, on its Roam page): iterate dummy from Sid's driving
notes · Calibration lane (the scope; fact/hypothesis/guess frame) · the
newcomer's-first-day dream · onboarding ramp. Track D can open any time on
the render-demands doc.

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
