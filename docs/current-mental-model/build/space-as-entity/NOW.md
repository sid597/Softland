# space-as-entity — thread file

STAGED 2026-07-26; CONTRACT.md authored same session (settled in-session:
Sid's word "I think we should build the space-type" + five redlines
absorbed). Order ruled: space first, reaction-declaration second; return
wear remains the campaign's true gate — this stages what follows it.

## STANDING (frozen at open — do not edit while the package is active)

- Binding docs: `CONTRACT.md` (beside this file) + `decisions.md`. **This
  file is a baton, not a source of truth; if it contradicts CONTRACT.md or
  decisions.md, those win — flag the discrepancy in NOW.** Phases and QC
  per `.claude/skills/work-package/SKILL.md`.
- Allowlist — NEW: `src/app/shared/space_material.cljc` + test files (named
  per phase). EDITED: `ground.cljs`, `binding_material.cljc`,
  `facet_masters.cljc`, `material_circulation.clj` (the one set line), the
  T8-swept test files, and (RULING R1 2026-07-26, after the P2 stop)
  `facet_material.cljc` — scope: the `:form-validators` seam only, per
  contract T9/6b. `verb_registry.cljc` is READ-ONLY; touching it or
  `instance-legal-sites` is a stop clause.
- Verification duties: every platform/behavior claim in the contract's
  manifest is re-checked against the on-disk file before code (memory and
  contract line numbers can drift).
- Definition of done: contract gates G1–G12 green (G12 added by R1) + live
  drive + the one falsification finder run + Fable gate verdict recorded
  here and in `decisions.md`.
- Stop clauses: contract §Stop clauses. Escalate per the skill; never
  improvise on binding docs.
- Hard rules: no Co-Authored-By in commits · never read
  `src/app/server/env.clj` · code and docs never mixed in one commit ·
  docs only on the local docs branch, never pushed.

## NOW log (append ≤15 lines per session)

- 2026-07-26 · Fable (staging session) · package STAGED: order settled,
  five Sid redlines absorbed, fence derived (master-tier capture — fence
  moved INTO rung 2), T1/T2 traps discovered by verification (space-claim
  is a vector; verbatim-bindings impossible under bindable? grammar),
  CONTRACT.md authored. Next: fresh build session runs P1 (rung 1) per the
  contract; boot from CONTRACT.md + this STANDING.

- 2026-07-26 · Codex · P1 (rung 1) · `P1.md` · **PASS**.
  Manifest claims reverified against disk before code; no binding conflict.
  Landed T1 into-append, real wheel hit, one T7 builder, probe 12, T8 sweep.
  G1: suite 17/329 + live 3 masters × 12 probes; probe 12 depth 1, floor/space.
  G2 final server read: 6 masters/29 revisions/22 rows; active violations 0,
  non-active residue 0; stop clause did not fire.
  G3: real block wheel zoomed via depth 1; empty wheel/pan/tap-anchor unchanged.
  G10g: one builder/ref/route; key/eval decision exact.
  T8 pin suites 26/423 + fast lane 183/1,724 green; dev compile 0 warnings.
  Forbidden files/fence untouched; no commit/push; cluster + dev app left live.
  Next: P2 only in a fresh context; P2 was not started here.

- 2026-07-26 · Codex · P2 pre-code manifest · **STOP**.
  Deliverable 6/G6 requires cross-field `min < max` candidate refusal.
  `facet_material/compile-form` passes validators one field value only;
  `space_material.cljc` therefore cannot enforce the pair invariant alone.
  The needed optional whole-form compiler seam is outside P2's frozen allowlist.
  Independent bounds would falsely accept some `min >= max` candidates.
  Fork + recommendation recorded in `decisions.md` Open Questions.
  No P2 code/test edit; no gate claimed. P1 is committed at `43a57a0`.
  Next: Sid rules allowlist seam (recommended) vs composite clamp form.

- 2026-07-26 · Fable · RULING R1 · **Option A, strengthened — RESOLVED**.
  Verified the diagnosis against the compiler myself; found the second
  enforcement site the recommendation missed: `valid-material?` is the
  wear-time validator `resolved-wear` floors through — compile-only would
  WEAR a durable min≥max revision. Seam spec now contract T9 + 6b: one
  optional `:form-validators` declaration read by BOTH `compile-form` and
  `valid-material?`; predicates over the material-keys projection, total;
  six existing declarations untouched (byte-identical no-op); instance
  masters inherit via `instance-grammars` pass-through. Allowlist +
  `facet_material.cljc` (seam only); gate G12 added (no-op + both sites);
  DoD now G1–G12. decisions.md OQ deleted (ruling executed as a sweep).
  Composite-clamp (B) rejected: recurring gap papered over, coarser edit
  surface, public-form change. Next: P2 resumes deliverables 6–10 + 6b,
  gates G4/G5/G6/G8/G9/G11/G12, fresh context from this exact state.

- 2026-07-26 · Codex · P2 (rung 2) · `P2.md` · **PASS**.
  Landed fm:space, bindable-subset material, the one three-lane camera fence,
  wear-sourced zoom clamp, registry/label reconcile, and escape membership.
  R1 seam is read by compile-form + valid-material? over material projections;
  six prior specs are no-op exact; valid/malformed instance forms inherit it.
  G4/G11 live hostile previews refused; no preview wore; 12-probe drill passed.
  G6 live: activated max 2.0; trusted wheel clamped 1→2; tap/marquee master,
  camera probes floor; rollback + reload restored trusted-wheel max 8.0.
  Malformed min 5/max 4 activation refused; served synthetic fell to floor.
  G8 membership + G9 live server/client `code-floor:fm:space:v0` agreed.
  Focused 6/52; affected 77/1,208; final fast 189/1,777; compile 0 warnings.
  Fresh-context fence/T9/P1/fence-boundary falsification found no code defect.
  verb_registry, instance-legal-sites, frozen v1/v2 untouched; diff-check clean.
  No commit/push; base 8.0 active, malformed candidate inactive; rung 3 not begun.
  Next: Fable gate verdict + decisions.md record only; do not start rung 3.

- 2026-07-26 · Fable · GATE (rungs 1+2, G1–G12) · `GATE.md` · **PASS — G7 OPEN**.
  Independently re-ran: fast lane 189/1,777 + five isolated affected nss
  (54t/829a) green; G2 server receipt :green (7 masters/33 revs/28 rows, zero
  active wheel rows). Live re-drive under FRESH gate request ids: drill
  12-probe pass; G9 client/server identical (code-floor:fm:space:v0 both);
  G4/G11 previews refused, no preview state; G3 wheel-over-block, saturates
  at exactly 8.0 and 0.1; G6 full cycle — 2.0 activated → trusted wheel
  clamps at exactly 2.0 → rollback → 8.0 restored; malformed refused fresh
  with [:space/zoom-clamp-invalid]. Re-imports minted byte-identical
  revision ids. Falsification: no code defect. FINDING: G7 ran in NO phase
  (P1 + R1 gate lists both dropped it — partition must sum-check vs the
  contract; retro item). Mechanism receipt banked: pick ≈4.5ms/event, same
  class as the hover pick; felt 52ms-bar half = Sid's first headed zoom;
  T6 fallback pre-approved. Cluster base-active; dev app stopped.
  Next: Sid's headed echo-bar glance closes G7 → package CLOSE + retro.

- 2026-07-26 · Fable (same gate session, attended) · G7 CLOSED · **ALL
  GATES GREEN**. Sid drove the felt half in his headed browser: typing
  through a sustained wheel burst over the dense area (canvas 191 blocks,
  567k served chars), zoom driven to the served 0.1 floor. Receipt
  (`__ground.report()`): echo n=378 p50=14.5 p95=23.3 p99=29.5 max=40.3
  vs the 52 bar; slow-echo outliers []. T6 fallback never needed. Adapter
  attestation absent from the report — accepted; the rule guards false
  FAILs and a degraded adapter cannot fake a green. Residue (not gate
  signal): two >100ms client stalls (144/283ms, 09:29:36/39Z), zero echo
  outliers coincident. G1–G12 all green; GATE.md updated in place.
  Remaining to CLOSE: Sid's code-commit word → HEAD-dynamic suites at
  committed HEAD → RETRO + adversarial recheck → board prune. Rung 3
  stays shut until close completes.

- 2026-07-26 · Fable (same session, Sid attended) · **PACKAGE CLOSED**.
  Code committed on Sid's word: `f7945fd` (rung 2; rung 1 was `43a57a0`).
  HEAD-dynamic suites at committed HEAD: code-atoms 9/198 green; recheck's
  fresh fast lane 189/1,777 green. RETRO.md written from the full trail;
  adversarial recheck (fresh subagent) verdict: sound after three
  factual-precision corrections, applied in place. Lesson routed: gate-
  partition sum-check rule → work-package SKILL.md (amendment 2026-07-26).
  Board pruned to done-pointer. Rung 3 (G10 lift) stages in a FRESH
  session; its contract cites the fence's three lanes as standing law.

## The cut (rungs 1+2, one package — binding terms in CONTRACT.md)

1. claim-chain: `(if (seq claims) claims binding-material/space-claim)`
   becomes `(into claims binding-material/space-claim)` — space-claim is a
   one-element VECTOR (binding_material.cljc:418); conj nests it and kills
   every space gesture (drill probe 8 catches it). handle-wheel! passes
   the real pick hit instead of :hit nil. Unify dispatch-key-eval!'s
   hand-built chain through the same builder (one chain-builder, no drift).
   PROOF (behavior-identical AT THE CUT, against served truth): receipt
   asserting zero wheel rows at block sites in the LIVE served interaction
   table · wheel-at-a-block probe added to floor-drill-probes pinning
   verb=:camera/zoom-at-pointer tier=:floor at the space depth · all 11
   existing probes byte-unchanged, suite + live console drill agreeing ·
   pick-per-wheel-event measured against the 52ms echo bar during a zoom
   burst (if it shows: pick once per burst).

2. Mint src/app/shared/space_material.cljc (attention_material pattern):
   - form: :space/zoom-min + :space/zoom-max, clamp-of-clamps validator
     (min >= 0.01, max <= 1000, min < max);
     :camera/zoom-at-pointer reads the wear, literal floor as fallback.
   - bindings: the BINDABLE subset only — tap→:anchor/place,
     press-threshold-shift→:selection/marquee-begin. The camera rows CANNOT
     be material (grammar refuses floor-reserved verbs) and stay floor-only
     by design — "space-floor-bindings verbatim" was wrong and is dead.
   - CAMERA-GESTURE RESERVATION (lands HERE, not rung 3 — a served fm:space
     master revision could otherwise capture camera gestures by tier):
     one predicate in binding_material refusing any MATERIAL row at
     :space/ground, any tier, for [:pointer/press :threshold] or
     [:wheel :complete] with modifiers #{} or :any. Shift-variants stay
     open. Read from that ONE place by the fm:space grammar validators,
     the console seam, and the instance lane (G10's pattern). Fence
     receipts: a violating candidate → grammar refusal + error card,
     floor drill still pans/zooms under any served fm:space revision.
   - register in facet-masters/specs; reconcile the hand-entered
     :space → "code-floor:space" entry (facet_masters.cljc:51) + its G14
     label expectations.
   - ADD space_material.cljc to
     material_circulation/default-material-policy-paths (escape detector
     from birth).

3. Rung 3, separate cut after 1+2 prove out: lift G10
   (binding_material.cljc:106) — per-space instance deviations become
   legal. Inherits the reservation fence; adds the instance-lane fence
   probe.

4. Then reaction-declaration (~a day): react! table at the emission sites,
   autotag = row #1, future stays the runner; reactions served/enumerable.
   Named future customers: episode-retry (durable runner's first honest
   customer), zoom band-crossing acts (below).

## Zoom bands — two halves (Sid ruling 2026-07-26)

- LOOKS half: band → render-policy as fm:space FORM data, read at derive
  time. Lands when felt at wear.
- ACTS half — WILL happen (Sid: "i do want to keep the zoom level will
  make something happen that is going to happen"): band-crossing minted as
  an EVENT at the camera settle (burst end — never per-frame), consumed by
  reaction rows like any event kind. Prerequisite: the reaction
  declaration table. Trigger: the first named band-act.

## Punts, each with its named trigger

| Punt | Trigger |
|---|---|
| Real type stratum (recipe over space-facets) | a second space instance exists |
| Cross-container pick path + per-space camera | first embedded canvas genuinely wanted |
| Zoom-band LOOKS half | felt at wear (form field, cheap) |
| Zoom-band ACTS half | reaction table exists + first named band-act |
| Camera verbs unreserved | probably never; own drill (unbrickable escape proven first) |
| Durable reaction runner | the episode-retry board item |
| Reaction rows as editable material | 2–3 real reactions exist to show the grammar |
| Log-consuming topology | with the durable runner, via /rama + rama-pitfalls |

Honesty note on the log topology: it observes events AT-LEAST-ONCE;
truth-writes converge to exactly-once EFFECT via deterministic ids;
externally-reaching reactions must each carry their own idempotency story
(obligation-recorded-first, deterministic run-id, converge-before-call —
the autotag pattern) or retries double-fire the external act. That is what
:external-via-derived-worker marks.

## Starter prompt (rungs 1+2) — paste-ready

    Space-as-entity, rungs 1+2. Boot:
    docs/current-mental-model/build/space-as-entity/CONTRACT.md (binding)
    + NOW.md (this file). Read ground.cljs (claim-chain ~2330,
    handle-wheel! ~2888, :camera/zoom-at-pointer ~2649),
    binding_material.cljc, attention_material.cljc first. Implement
    exactly the contract's phases; bank every numbered gate receipt.
    Fences: camera verbs stay floor-reserved; do NOT lift
    binding_material.cljc:106. Out of scope: rung 3, reactions,
    cross-container pick, per-space camera, type stratum, zoom bands.
