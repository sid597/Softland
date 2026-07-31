# studio — CONTRACT (P0 spike + P1 build)

Cut 2026-07-31 in the origin direction session at Sid's word ("write the
contract here and now"). Authority: `BRIEF.md` — its eleven laws are binding
and are NOT restated as negotiable here; this contract implements them.
Re-cut once after the fresh default-fail round recorded in `VALIDATION.md`;
that round was not re-run and does not authorize P0 in the same context.
The brief's spike-before-contract sequencing is honored structurally: P0 IS
the spike, and its receipts bind this contract's named slots S1–S6 before
P1 opens. Locators in this contract are deliberately FILE-level substance;
P0 machine-pins exact symbols/lines (`grep -n`, `wc -l`) per the
work-package manifest rule — nothing here is hand-counted.

Validation: ONE fresh default-fail validation round runs over this contract
(few-and-large rule; no separate PLAN.md). `minor-fail` → fixes applied in
place, no re-run. `fail` → recut with the failure artifact as input.

## §1 Purpose and consumers

The studio: design development and iteration directly in the world — the
first surface over the smalltalk-ui-vm substrate whose gate is use-to-human,
not truth-to-substrate. Consumers in order: (1) Sid's hand on the canvas;
(2) the agent as ungated second hand through the existing shared controller;
(3) the Goal-2 pilot (next package) consuming the primitives this package
proves.

## §2 Non-goals / refusals (each an extension point, never a void)

- **Deliberate keep-as-named-component gesture** — P2 opens here (extension
  point: the accept lane + the naming layer). P1 drafts carry auto-names
  ("Draft 1, 2, …").
- **Margin thinking / annotation** (notes, arrows, connectors) — P2 ladder
  rung (extension point: the parts vocabulary).
- **Agent eyes** (specimen readback → image) — separate parallelizable
  package (extension point: the validation pass of §3f is its first
  consumer).
- **Goal-2 pilot** (clone an outside component) — next package.
- **New verb minting** — next ARC (brief law 5). This package BINDS existing
  registered verbs only; `verb_registry.cljc` is zero-diff protected.
- **Ink** — punted, door open forever (law 8): nothing in P1 may assume
  structured-only input in a way that structurally precludes a future
  stroke-as-part (extension point: a stroke primitive joins the same parts
  vocabulary later; validation round checks no design decision closes it).
- **X-ray changes** — the anatomy room stays byte-identical EXCEPT a
  disclosed S6 red-banner repair if triage proves a live defect
  contaminating studio lanes.
- **Line/ellipse/frame primitives beyond what §3b needs** — thinning; the
  vocabulary grows at the next lived need.

## §3 The build (plan-grade where the ground is known)

- **a — World surface, no mode.** Drawing happens on the ordinary canvas
  (law 1). The drafting table, if used in the lived script, is nothing but
  a drawn frame — zero modal machinery. Entry gesture bound per S3 from the
  EXISTING gesture grammar; camera-reserved gestures (naked drag/wheel at
  ground) are untouchable (TRAP-1).
- **b — Draft birth.** A drawn rect + typed text becomes a REAL functional
  draft: durable revisioned material from the first stroke (law 2; route
  per S1), rendering through the same assembly interpreter as everything
  else, surviving client restart, visibly wearing draft-ness (a provisional
  look that signals STATUS without signaling fake — the draft is alive).
- **c — Edit at the pixels.** Select a draft → handles + direct
  manipulation (move, resize, retune the drawn parts' props). Every
  structural change COMPILES to the existing seven-op edit grammar
  (add · remove · reorder · retune-props · rebind-slot · attach-def ·
  detach-def) through the existing shared controller lanes — the drawing
  hand is a compiler, never a writer (TRAP-6). No new edit ops; any
  genuinely unavoidable op addition is a disclosed stop-clause event.
- **d — Behavior wire.** ONE existing registered verb (picked at S5) is
  bound to a drawn part through the existing binding grammar rows
  (gesture/phase/verb/priority). The drawn thing responds to its gesture —
  "I drew it and it does something."
- **e — The trust loop on one real component.** Duplicate the block as a
  draft candidate (candidates coexist on the master; the default pointer
  untouched — law 3) → change one visible prop at the pixels → accept
  through the test gate (§3f) → reverse restores the prior default exactly.
- **f — Test-gated accept, v0 (law 4 — never broadcast).** Between
  candidate and default-flip sits the validation pass: an agent applies the
  candidate in preview scope to at least THREE real block instances of
  distinct shape (empty · wrapped multi-line · marks/machine-bearing),
  exercises real flows (focus, type, the bound gestures), asserts render +
  interaction invariants, and produces a breakage report. The report is a
  durable, queryable fact bound to the exact candidate revision, tested
  instance ids/shapes, checks, result, actor, and honest time; S1 pins its
  closed shape and existing write/read lane. The canonical activation owner —
  never a UI-only wrapper — reads that fact and refuses absent, non-passing,
  mismatched, or conflicting reports before pointer edit. Only then may the
  flip event carry the report id as an HONEST declared ground; P0 must prove
  the existing closed ground vocabulary can name it without pretending.
  Direct calls to the existing lower activation hand are the bypass adversary
  (TRAP-2). Preview lifecycle laws inherited from the substrate hold
  (endPreview-before-activate; served-recovery-refresh before rollback).
  Fail-closed: no queryable matching PASS, no flip.
- **g — Agent rider (ungated).** The same §3c/§3d ops driven by words
  through the existing agent lane on the same canvas, demonstrated once
  with a receipt. Not a gate; a demonstration (law 10).
- **h — Vocabulary (law 9).** Studio-visible strings speak Sid-words
  (component, draft, accept, put back, …). Zero `fm:*`, zero revision
  hashes on the studio surface. Studio-surface refusals render in
  Sid-words with a "show details" door; substrate detail lives behind it
  (TRAP-5). The x-ray keeps substrate vocabulary — that is its job.

## §4 Named open slots — bound by P0 receipts before P1 opens

- **S1 — draft + promotion truth route.** Cost the COMPLETE transition for
  both candidates: birth identity → edit target → candidate container →
  preview scope → durable validation report → canonical guarded activation →
  target default pointer → exact reverse. Storage candidates remain (a)
  per-instance STRUCTURE tier (the
  substrate today carries instance-tier VALUES; structure lives at master
  candidate level — verify) vs (b) draft-master-per-draft: every draft
  silently minted as its own master at first stroke, with a draft/real
  distinction in the master registry; trails durable by construction.
  For (a), name how an instance revision becomes a candidate of the shared
  target without cross-container sleight of hand. For (b), name the runtime
  master registry/read path and exactly which default becomes permanent.
  Also pin the report's closed schema, first physical durable request, product
  read/query, candidate+instance-set binding, same-id replay, conflicting-id
  refusal, and encoding into the closed activation-ground vocabulary. The
  canonical guard must defeat a direct `facet-master/activate!`-class bypass;
  a Studio UI check is not evidence. P0 costs both against the code, recommends
  one. The brief's laws
  constrain: drafts real + functional + durable; trails persist; candidates
  coexist with one default. STOP if both draft routes are cutover-class OR if
  the report/guard needs a new depot, PState, import-key family, protected
  kernel change, or other cutover-class ownership.
- **S2 — drawing primitives.** Inventory the registered primitive
  vocabulary; commission the missing pieces for rect + text-run drafts
  (amber additions to the primitives registry, the P1-block-cutover
  pattern). Receipt: what exists, what's minted, exact registry names.
- **S3 — entry gesture.** Pick from the existing gesture grammar; verify
  against the camera reservation predicate; disclose the binding.
- **S4 — latency lane.** Verify strokes can ride the optimistic-local echo
  class; pin the bar number from the measured family (the 52ms-class bars)
  and the measurement shape for G-T8.
- **S5 — the verb.** Enumerate the registered verb registry; pick ONE whose
  effect is observable without new machinery; disclose name + expected
  observable.
- **S6 — red banners.** Triage the two refusals photographed at Workshop
  open (`fm:provenance` parse error "EOF while reading"; `fm:space`
  zoom-clamp-invalid): live durable defect vs leftover experiment. Repair
  ONLY if it contaminates studio lanes; else banked residue with falsifier.

## §5 Traps ledger (implementers cite TRAP-n in code comments)

- **TRAP-1 camera reservation.** Naive: bind drawing to plain drag →
  fights the structurally-uncapturable camera gestures (the space-package
  reservation predicate, all three lanes). Ruling: entry gesture from
  unreserved space only; P0 verifies against the predicate; G-T7 proves
  pan/zoom live while a draft exists and during drawing.
- **TRAP-2 broadcast-on-accept.** Naive: activate flips the default and
  every live instance re-renders instantly. Concrete failure: brief law 4
  exists because one bad accept kills trust in the whole studio. Ruling:
  §3f matching durable PASS mandatory at the canonical activation owner; flip
  carries its honestly-declared report id; a direct lower-hand call without it
  is refused; fail-closed.
- **TRAP-3 phantom drafts.** Naive: draft = client-side overlay (render
  state, not material). Concrete failure: dies on restart, can't carry a
  binding → violates law 2 ("not dummies — real and functional"). Ruling:
  durable revisioned material from first stroke, route S1.
- **TRAP-4 x-ray crutch.** Naive: "for advanced edits, open the anatomy
  room." Concrete failure: the consumer-tool cap; Sid's "never never
  never" (law 6). Ruling: every P1 act completes with the anatomy room
  closed; G7 includes the check.
- **TRAP-5 vocabulary leak.** Naive: substrate teaching cards bubble to
  the studio surface. Concrete failure: photographed — the two red hash
  banners at the top of Sid's Workshop open. Ruling: §3h; G-T6 scans the
  studio string surface (scope: the new studio namespaces' user-visible
  strings; the scan STOPS at x-ray surfaces, which legitimately speak
  substrate).
- **TRAP-6 grammar bypass.** Naive: the drawing hand writes scene/instance
  state directly for speed. Concrete failure: unversioned change — no
  candidate, no preview, no reverse; the whole safety story dies silently.
  Ruling: all structural edits ride the existing controller's candidate
  lanes; G-T5 asserts op-receipt parity between gestures and grammar ops.
- **TRAP-7 draft identity and transitions.** Scope + transition, named:
  a NEW draft mints a NEW draft identity at first stroke (scope per the
  S1-bound route). Subsequent edits mint new CANDIDATE revisions on the SAME
  draft identity (rebase on current candidate, the controller's existing law).
  S1 names any promotion from that identity/container to the target master's
  candidate; no cross-container revision is relabeled. Accept mints no
  component material — after the matching §3f report is already queryable, it
  flips the S1-named default pointer. Naming waits for P2.
- **TRAP-8 unattested perf numbers.** Concrete failure: scene-substrate's
  91ms "FAIL" was SwiftShader; P2's 35.4ms echo is swiftshader-attested to
  this day. Ruling: every latency receipt's FIRST field is the
  adapter/device attestation; software-rendered numbers are marked so, and
  Sid's headed real-GPU wear is the only real-GPU instrument.

## §6 Fence (diff-derived allowlist verified at phase-artifact time)

Zero new: HTTP endpoints · Rama modules/depots/PStates/topologies ·
registry verbs · import-key families · gesture rows on reserved gestures.
Allowed, disclosed in the P1 artifact: new registered primitives (S2) ·
new scene actions via the existing `register-action!` seam · new client
studio namespaces (placement per the source-structure rules: shared `.cljc`
only if both sides consume) · the S1-route's minimal server surface IF the
spike proves draft birth, promotion, durable report, canonical guard, and
reverse all ride existing ObjectContainer/revision machinery. A canonical
activation/report guard is allowed only if S1 proves it needs no new depot,
PState, import-key family, or protected-kernel change; otherwise STOP. Any
other durable-tier or cutover-class need is a STOP, never an improvisation.
Zero-diff
protected (P0 pins hashes): `env.clj` (never read) · `relation_kernel.clj`
· `episode.clj` · `cascade.clj` · `verb_registry.cljc` ·
`matter_room.cljc`. The anatomy-room's visible projection and controller stay
behavior-identical except a disclosed S6 repair: P0 pins exact x-ray symbol
spans plus the existing projection/golden receipts. Shared host files such as
`ground.cljs` are NOT falsely whole-file-protected because they also own the
ordinary canvas; any diff inside a pinned x-ray span is S6-only. Changed-file
list is `git diff --name-only` + status
at artifact-writing time, every path classified allowlist / new-disclosed /
DRIFT.

## §7 Testable claims (executable; G4 runs them)

- **T1** A drawn draft is real: durable, restart-surviving, structure +
  binding-bearing, enumerable via the corpus surface P0 names.
- **T2** Draft-ness is visible exactly while provisional; gone at accept.
- **T3** Accept without a queryable, matching §3f PASS is refused by the
  canonical owner; the adversary calls the lowest existing activation hand
  directly with no report, a FAIL report, a different candidate, a different
  instance set, and conflicting reuse of one report id. Before the guarded
  flip, no instance outside preview scope ever renders the candidate.
- **T4** Reverse restores the prior default byte-exactly (served revision
  compare) via the existing recovery lanes.
- **T5** Gesture→grammar parity: every structural edit from the drawing
  hand arrives as seven-op compositions through the controller; op-receipt
  count matches the gesture script; no bypass writer exists (grep +
  runtime probe).
- **T6** Studio string surface carries zero `fm:*`/revision-hash literals
  (scope per TRAP-5; stops at x-ray surfaces).
- **T7** Camera pan/zoom (naked drag/wheel at ground) work unchanged with
  drafts present and mid-drawing-session.
- **T8** Stroke echo within the S4-pinned bar; receipt opens with adapter
  attestation (TRAP-8).

## §8 Gates — partition sum-checked; owners named

| Gate | What | Owner / where |
|------|------|---------------|
| G1 | Fast-lane suite + touched banks green | P1 implementer, in-phase |
| G2 | CLJS compile 0 warnings (Electric activation wall noted: gate session may inherit implementer attestation; Sid's next shadow compile is the free falsifier) | P1 implementer |
| G3 | Fence verification per §6 (diff-derived) | P1 implementer + gate session re-check |
| G4 | T1–T8 green (JVM + probe mix; T8 with attestation) | P1 implementer |
| G5 | The wearing: full §3 loop driven headed on an isolated rig (blind-symlink pattern, own port), receipts banked | P1 implementer (headed) |
| G6 | Fresh falsifier, ONE finder, default-fail, aimed at the genuinely-new machinery: the gesture→grammar compiler + the test-gated accept path | Fresh context after P1 |
| G7 | **LIVED GATE — ONE-SHOT.** Sid, cold, no reading, on the real land (not a rig): draw a rectangle with text → make it do something (S5 verb) → duplicate the block as a draft, change one visible thing → accept through the test gate → put it back. PASS = completed without reading anything + he can narrate each step + the anatomy room never opened. His verbatim reaction lands in the gate record (the arc's instrument). The cold-open precondition is destroyed by its own running: afterwards the committed harness asserts only the mechanical half (steps executable end-to-end); cold-ness is never re-claimable. A G7 FAIL is the arc gate speaking — route to a direction session; never patch (law 6 spirit). | **Sid**, LAST, after the slim gate |
| G8 | Agent-rider demonstration: one word-driven edit through the existing lane, receipt banked (ungated demo, law 10) | P1 implementer |

Sum-check: G1–G8 all assigned (P0 owns no gates — it produces the S1–S6
receipts). Order: P1 greens G1–G6-inputs + G8 → independent gate session
(SLIM tier by default — surface package on existing lanes; ESCALATES to
FULL if S1 requires source changes at any durable draft/promotion/report owner
or at canonical activation. SLIM survives only if P0 proves the whole S1 chain
already exists and P1 merely calls it, decided and recorded at P0 close) → G7
last, so the surface Sid cold-opens is the gated one → Sid's commit ruling
→ HEAD-dynamic suites re-run at committed HEAD (close rule).

## §9 Phases

- **P0 — the spike** (fresh context; no product code; probes allowed).
  Deliverable `P0.md`: S1–S6 answered with machine-verified citations
  (`grep -n` receipts), the §6 protected-file hashes pinned, the §10
  manifest locators pinned, x-ray symbol-span/golden pins, gate-tier
  recommendation. Stop conditions:
  S1 both-routes-cutover-class · S2 vocabulary cannot express rect/text
  without renderer-core change · no existing authoritative durable
  report+guard route · any law conflict — escalate per the
  work-package stop clause (options + recommendation into decisions.md
  Open Questions; Sid or Fable rules).
- **P1 — the build** (ONE fresh context, whole package; /rama skill loaded
  before any Rama-adjacent code; electric-docs consulted for client
  reactivity; verification duty: every memory-derived platform claim
  checked against disk before code). Deliverable `P1.md`: receipts for
  G1–G5 + G8, diff-derived file list per §6, judgment calls disclosed.
- Then: slim gate (fresh context) → G7 (Sid) → commit ruling (Sid) — code
  and docs commits separate, docs-local branch, never pushed.

## §10 Input manifest (substance-binding; P0 pins exact locators)

`BRIEF.md` (authority) · `build/smalltalk-ui-vm/{P2,GATE_P2}.md` (substrate
truth as of its gate; code outranks it) · the primitives registry
(`face_primitives.cljc`) · the assembly interpreter + compile-per-
[master revision] seam (`face_assembly.cljc`) · the anatomy grammar +
validators (`shared/anatomy_material.cljc`) · the shared edit controller +
seven-op grammar (the `window.__workshop` / `__portal.workshop` lanes) ·
binding + facet material (`binding_material.cljc`, `facet_material.cljc`) ·
the verb registry read surface (`verb_registry.cljc`, zero-diff) · the
scene store + `register-action!` seam · the camera reservation predicate
(space-as-entity package) · the corpus/census surfaces in `ground.cljs` ·
the instance/revision persistence tier (`object_container.clj`, zero-diff
expected — S1 verifies) · the synthesized instance/shared activation adapter
(`object_container/facet_master.clj`) · the closed activation event and ground
grammar (`activation_event.cljc`) · the existing product read/query that can
prove a validation report before pointer mutation (S1 must name it; absence is
a STOP, not an invented source family). On any conflict between this list and
disk, disk wins and the P0 artifact records the correction.

## §11 Handoff

The first fresh round is banked `FAIL` in `VALIDATION.md`; this is its recut and
was not re-run. Next session: ONE fresh default-fail validation round over this
re-cut contract → minor-fail fixes in place → P0 → P1 per §9. Thread file:
`build/studio/NOW.md` (STANDING frozen at open; NOW entries ≤15 lines).
Board line: the studio block on `docs/sessions/next-prompt.md`. Nothing
starts without Sid: G7, commit rulings, any spend. The brief's eleven laws
are not reopenable by any phase — a wall means stop-clause, never a patch.
