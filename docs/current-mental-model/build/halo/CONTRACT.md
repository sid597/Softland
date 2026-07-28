# halo — one reserved meta-gesture, condensation at the citizen

Cut 2026-07-29 in the halo contract session (Sid: "i agree with all lets
go"), from the 2026-07-28 halo recognition (`vision/LOG.md` 2026-07-28 —
Self/Morphic halos shown, Sid: "have i not been saying this same thing…";
confirmed as C2's direct-manipulation half, banked verbatim since LOG
2026-07-11). Every manifest locator below was machine-verified on disk in
the cutting session (grep against the 0a33e15/88e1baf tree).

**ONE implementation phase, by Sid's standing ruling this session** ("keep
the implementation phases low — it always blows up and i am not sure its
that helpful; the implementer 5.6sol-xhigh is a great great implementer and
fable is the best for defining the major work covering all angles"). This
contract therefore carries plan-grade specificity itself; there is no
separate PLAN.md. Cadence: contract → ONE fresh default-fail validation
round over this document → one fresh implementer context builds P1 whole →
one fresh falsifier in-phase → one fresh slim gate → Sid's commit ruling.

## §1 Purpose

Matter-room built the skeleton: rooms, matter verbs, the briefing, the
gauge. The halo is the SKIN — the one gesture that makes all of it
reachable from any pixel. Right-click any rendered citizen and its meta
condenses in place: one identity line (what this is, which masters it
wears, honest citizenship) and four handles — **ask** (the briefing card in
place) · **enter** (the master's room) · **say** (one line, durably into
the master's room conversation, marked to this citizen) · **preview** (the
existing pure client membrane). Heavy verbs (activate / rollback / deviate)
get NO handles — they stay room-only. The gesture is universal and its
meaning is kernel-inviolate: it never branches per-component; only the
condensation's CONTENT varies, and that content is DERIVED from served data
and the verb registry's own declarations — never inferred, never dispatched
into component code.

## §2 Consumers, in order

1. **Sid's hand** — the halo is C2's first landed form; its true gate is
   his unscaffolded open (deliberately NOT a build gate — §8).
2. **The rooms** — enter and say make every citizen a doorway and a mouth
   into the matter-room machinery already gated (P1–P4).
3. **C1/C2 in BETS.md** — sharpened in the cutting session; the halo is
   C2's activation instrument and C1's entry ramp.

## §3 The laws this package builds (H1–H8)

- **H1 — THE GESTURE.** The DOM `contextmenu` event over the land's canvas
  normalizes to a NEW kernel gesture kind `:pointer/meta`, phase
  `:complete` (a discrete gesture; no right-drag continuation in v0).
  This rides the grammar's own pre-registered widening procedure
  ("a kernel change plus a grammar version — never a silent one",
  `binding_material.cljc:74`): `gesture-kinds` gains `:pointer/meta`;
  `legal-gestures` gains `[:pointer/meta :complete]`. `preventDefault`
  fires ONLY on the land's canvas node — the native browser menu survives
  everywhere outside it (sidebar, panels, dev surfaces). Inside the canvas
  the meaning is uniform, focused text blocks included (the sliver never
  branches; Ctrl+C/V keyboard paths are untouched). Cost accepted by Sid
  in-session: the native context menu inside focused blocks dies.
- **H2 — THE INVIOLATE SLIVER**, enforced structurally three ways:
  (a) `:halo/condense` is the ONLY verb the gesture reaches, filed as
  FLOOR rows on all four sites (`:block/user-hit-area`,
  `:block/machine-hit-area`, `:block/fold-header`, `:space/ground`;
  the fold-header claim's subject is the block, so header condensation
  lands on the same citizen — `binding_material.cljc` `probe-claims`).
  (b) `:halo/condense` is `:verb/floor-reserved? true` — material may
  never name it (the `:camera/pan` mechanism, `verb_registry.cljc:307`
  `bindable?`).
  (c) a new `meta-gesture-reserved?` predicate in `binding_material.cljc`
  refuses ANY material row (master or instance tier, ANY site) whose
  `:binding/gesture` is `:pointer/meta` — the `camera-gesture-reserved?`
  pattern (`:128`) widened to every site. One predicate var; read at the
  same validation lanes camera reservation rides (V3 pins them).
  Floor rows use modifiers `:any` — shift-right-click condenses too.
  The served interaction table and the floor drill show the meta rows
  honestly (`floor-drill-probes` gains meta probes; `table-rows` carries
  them like any row).
- **H3 — CONDENSATION IS DERIVED, NEVER INFERRED.** The halo renders from
  data the client already holds or the server already serves: the subject
  uid from the pick claim; worn facets from the claim's `:claim/facets` →
  masters via `facet-masters/by-facet`; per-master wear tier + revision
  from the client wears cache (`ground.cljs:262 current-material-wears`),
  labelled honestly — worn revision id, or `:code-owned`/floored where
  only the floor answers (P4's citizenship vocabulary). Handle labels and
  effect classes come from the verb registry's declarations
  (`:matter/preview`, `:matter/say`) — zero new inference paths, zero
  per-component dispatch, NO new server serve face in v0.
- **H4 — THE FOUR HANDLES, exactly.**
  - **ask** — citizen-scoped. The existing seventeen-question briefing
    card (`material_portal.cljc:912`) rendered in place via the existing
    `__portal` read surface. Pure projection over an existing read; the
    17-question floor is untouched.
  - **enter** — master-scoped. The existing room entry
    (`face_wiring.cljs:425 :enterRoom` → the shipped `?drill=` lane /
    `POST /api/matter-room/open`). Navigation, no new route.
  - **preview** — master-scoped. The existing `__bindings.preview` /
    `endPreview` client membrane; NO server function, NO HTTP route on
    this path (the P3 law, unchanged).
  - **say** — master-scoped. The ONE new write path (H5).
  Handles render ONLY where their requirements are supplied by the pick +
  served data — no pretend handles, ever. Plurality is honest: ALL worn
  masters are listed; master-scoped handles attach per listed master; no
  "primary master" concept exists anywhere.
- **H5 — SAY.** One line of the wearer's text lands durably in the picked
  master's room conversation, marked to the picked subject.
  - Route: `POST /api/matter-room/say` — the act lane's FOURTH disclosed
    endpoint (and the only widening; H6). Parameters normalized by a pure
    `matter_room.cljc` builder (the P3 pattern: nonblank text, registered
    master via `room-id-by-master`, actor defaulted to `matter-actor`
    `{:actor/id "sid" :actor/type :human}` only when omitted).
  - The utterance rides the EXISTING episode utterance-import artery
    (`episode.clj:654 utterance-import-request` — the P2 resident-birth
    artery, parameterized actor). Identity: the CLIENT mints one say-id
    (uuid) per act; the op-id derives from it, so a replayed request
    converges on the same block and a new say is a new block (the
    at-least-once law; identity-only ids, never content hashes — the P2
    lifecycle law). A replayed-say test ships in-phase.
  - The mark is an EXISTING relation-kernel edge (say-block → subject-uid,
    `material_circulation.clj` request path, asserted-by sid — custody
    makes it gold by the existing law `:83-91`), surfacing through the
    existing experience machinery (`face_projection.clj:657
    experience-around-many`). V1 rules whether an existing relation kind
    fits or the closed enum grows by ONE reviewed line (pre-approved as
    in-scope if no existing kind fits — the settled enum-growth law).
  - NO new import family, adapter, depot, topology, PState, or write
    artery. The say import must NOT spawn a resident turn or open an
    episode (V1 pins the non-spawning import shape against
    `episode.clj`'s spawn law).
- **H6 — HEAVY VERBS STAY IN THE ROOM.** No halo handle invokes
  `:matter/activate`, `:matter/rollback`, or deviate (instance or
  master). The disclosed act-endpoint set widens by exactly
  `/api/matter-room/say` and nothing else — the G7-class
  endpoint-disclosure test re-cuts to name four routes.
- **H7 — HONEST NOTHING-MATERIAL, UNIVERSAL ANSWER.** The gesture is never
  inert on the canvas. Where nothing is material, the condensation answers
  honestly (`:code-owned`, floored) with only the meetable handles.
  Right-click on empty ground condenses THE SPACE — `fm:space` is a
  registered master with a derived room like the other six
  (`matter_room.cljc:57-81`); a miss lands on the space, never on nothing
  (the rung-1 law).
- **H8 — PURE PROJECTION EXCEPT SAY.** Open/close, identity, ask, preview
  move appearance/attention only (ask's fetch is an existing read).
  enter navigates. Only say writes. Registry consequences: `:halo/condense`
  is `:pure-projection`; `:matter/say` is `:durable-via-request` and joins
  the `:durable-via-request` exact-set pin (re-cut, one entry). Both new
  registry entries get `ground.cljs` registrations per the
  closed-vocabulary equality invariant (`binding_dispatch_test.clj` — "the
  verb registry is the only vocabulary the kernel registers"):
  `:halo/condense` real (opens the halo), `:matter/say` inert
  `{:invoke (fn [_] nil)}` like its durable siblings (the P3-F2 ruling
  pattern — durable acts ride the disclosed HTTP lane, never kernel
  dispatch). `:matter/say` required-args `#{:master-id :subject-uid}` — no
  site supplies them, so strict v2+ material refuses the row
  (grammar-unbindable at current sites, same class as its P3 siblings).

## §4 Non-goals — each a named extension point, not a void

- The halo's own form as served material (the C2 tail: the halo face
  becoming an editable assembly) — v0 renders as a code-floor projection
  in `ground.cljs` (the binding-lint-card pattern). Extension point: the
  face lane, at a lived recurrence.
- Deviate/activate/rollback handles — the room owns them; if wear proves a
  direct handle is wanted, that is a NEW contract (it changes H6).
- Right-drag / continuous meta gestures, touch/long-press — LATER, at a
  real device or a real friction.
- Gauge refresh from the halo, and the durable gauge-report driver
  (GATE_P4 finding 2) — room-side tenants, re-banked (§10).
- Halo over non-block scene objects beyond the space (future citizens) —
  the universality law already covers them structurally via the claim
  chain; nothing bespoke lands now.

## §5 Placement ruling

Client: gesture + reservation + condensation render + handle wiring live
in the kernel files that own their stations today (`events.cljs`,
`ground.cljs`, `binding_material.cljc`, `verb_registry.cljc`,
`face_wiring.cljs`). Server: ONE pure builder in `matter_room.cljc` + ONE
route in `server_jetty.clj`; the say import and mark ride existing owners
(`episode.clj`, circulation's relation request path) — zero-diff on those
owners is the default; a compelled touch is a stop (§9), not an
improvisation. No new namespace unless the halo render genuinely warrants
one client file (implementer's call, disclosed in the receipt).

## §6 The phase — P1, everything

**Files (allowlist; substance-bound per the manifest law; docs under
`build/halo/` free):**
- `src/app/client/workspace/events.cljs` — `contextmenu` observation on
  the canvas node (scoped preventDefault).
- `src/app/client/workspace/ground.cljs` — normalization to
  `[:pointer/meta :complete]`, the halo condensation render, handle
  wiring, `register-verb!` entries for `:halo/condense` (real) +
  `:matter/say` (inert).
- `src/app/shared/binding_material.cljc` — `gesture-kinds` /
  `legal-gestures` widening + `meta-gesture-reserved?` + the four floor
  rows + meta probes in `floor-drill-probes`.
- `src/app/shared/verb_registry.cljc` — `:halo/condense` + `:matter/say`
  declarations.
- `src/app/shared/matter_room.cljc` — pure `say-request` builder.
- `src/app/server/server_jetty.clj` — `POST /api/matter-room/say`.
- `src/app/client/workspace/face_wiring.cljs` — `__portal.say` (console
  parity with the other acts) + the halo's handle→HTTP call.
- Tests: the focused namespaces the diff touches (binding_dispatch,
  material_portal, matter-room/episode-adjacent, face_wiring-adjacent);
  new assertions ride existing namespaces unless a new one is genuinely
  warranted (disclose in the receipt).
- CONDITIONAL, zero-diff preferred, stop if compelled:
  `src/app/server/episode.clj` (only if the artery lacks an arity — the
  P2 precedent: parameterize without changing default behavior),
  the circulation relation request path, `shared/material_portal.cljc`
  (only if a T9-class briefing sentence must name the say route).
- **FORBIDDEN:** `src/app/server/cascade.clj` · `binding_material.cljc`
  beyond the named widening · `env.clj` (NEVER read).

**Behavior at close:** every H-law above is live and suite-pinned; the
drill and served interaction table show the meta rows; the falsifier has
run; gates G1–G4 green.

## §7 Traps ledger — cite by number in code comments

- **T1** — a material row naming `:pointer/meta` accepted anywhere (any
  tier, any site, any grammar version) = FAIL. The reservation is the
  sliver.
- **T2** — condensation content produced by dispatching component code or
  per-component branching = FAIL. Derived-only (H3).
- **T3** — a second write path for say: any new import family / adapter /
  depot / topology / import-request constructor = FAIL. The tree-wide
  import-builder census must end at the eight pre-existing owners.
- **T4** — a pretend handle (rendered where its requirements are not met,
  or on nothing-material without honest labels) = FAIL.
- **T5** — say replay duplicating (two blocks from one say-id) = FAIL;
  ship the replay test.
- **T6** — native context menu suppressed OUTSIDE the canvas node = FAIL
  (scope of H1).
- **T7** — any heavy verb reachable from the halo = FAIL (H6).
- **T8** — the gesture inert anywhere on the canvas (no condensation, no
  honest answer) = FAIL (H7 universality; the space answers a miss).
- **T9** — the drill / served table missing or mislabelling the meta rows
  = FAIL (legibility is the point).
- **T10** — the grammar widened silently: the kind must enter through the
  documented kernel-change-plus-grammar-version procedure with a visible
  record (docstring + version note in `binding_material.cljc`), and v1/v2
  row semantics for existing material stay byte-frozen = FAIL otherwise.

## §8 Acceptance gates — slim tier (claim-risk sized), partition = all P1

- **G1 — suite + compile.** Focused namespaces green (selection
  diff-derived per the machine-cut rule); the closed-vocabulary equality
  holds with both new registrations; the `:durable-via-request` exact-set
  pin re-cut and green; CLJS full compile 0 warnings.
- **G2 — live drive on an isolated current-tree server** (the P3/P4 rig
  pattern; the shared land untouched): right-click a real block →
  condensation with honest identity and plural masters; ask unfolds the
  17-question card in place; enter opens the room through the shipped
  lane; preview wears + `endPreview` restores with server bytes immobile
  (the P3 measurement); **say lands durably in the room conversation and
  its mark surfaces in the master's experience** (re-open the room and
  read it back); right-click empty ground condenses the space; a
  console-injected material row naming `:pointer/meta` is REFUSED with an
  error card, live.
- **G3 — G10-machine.** Environment attested first (adapter identity);
  real ground edit echo p95 < 52ms WITH a halo open during sampling; warm
  pass is the receipt, cold transients honestly recorded.
- **G4 — one fresh falsifier**, in-phase, aimed at exactly: reservation
  bypass (T1) · say widening / undisclosed endpoints (T3, H6) · handle
  honesty (T4, T8). First-FAIL → smallest in-fence repair → exact
  counterexample re-run, counterexamples become fixtures (standing
  falsifier law).
- **Sid's headed half is NOT a gate.** His first unscaffolded open is
  C2's kill/confirm instrument (BETS.md); scaffolding it in a gate would
  poison the measurement. The gate record says so explicitly.

## §9 Stop clauses

Verbatim from the matter-room package, in force here: the implementing
session NEVER improvises policy on binding docs. A genuine conflict
between this contract and disk (or inside the contract) → STOP, write the
finding to `build/halo/NOW.md`, Sid/Fable rules, the ruling is recorded
before code resumes. Manifests bind on SUBSTANCE — locator drift
re-locates + logs, never stops (the fff0b76 class). The fresh falsifier
is never skipped, never replaced by the suite. Commit decisions are Sid's;
code and docs ride separate commits; docs only on the local docs branch,
never pushed; never Co-Authored-By.

## §10 Residue intake — discharged 2026-07-29

All eight banked findings (GATE_P3 ×3, GATE_P4 ×5) were reviewed in the
cutting session and RE-BANKED — none belongs in this fence. Named for the
next room-side touch: GATE_P4 f2 (no production driver lands the computed
gauge report durably — a room "refresh gauge" act is its natural tenant)
and f1 (its poisoned-report refreshable flag rides the same fix). The
rest: GATE_P3 f1 (mute act error card) + f2 (unseeded-master dev wedge) +
f3 (vacuous T9 negative) and GATE_P4 f3 (pre-P4 room position overlap) +
f4 (uncapped timeout) + f5 (dev-boot ambiguity note) stay banked with
their cheap falsifiers in the gate records.

## §11 Input manifest — substance binding; line numbers are hints
(machine-verified 2026-07-29)

- `binding_material.cljc` — `gesture-kinds :51` · `legal-gestures :60` ·
  the widening law `:69-75` · `sites :77` · `camera-gesture-reserved?
  :128` · `valid-row? :182` · `resolve-binding :373` · `space-claim :439`
  · `floor-drill-probes :455` · `table-rows :555`.
- `verb_registry.cljc` — `effect-classes :34` · floor-reserved mechanism
  `:29-31, :303-313` · the P3 matter-verb declaration pattern `:215-285`
  · `well-formed-registry? :360`.
- `events.cljs` — `>mouse :55-74` (no button field, no contextmenu
  listener exists — the gesture is virgin).
- `ground.cljs` — wears cache `:193, :262` · `claim-chain :2373` ·
  `instance-binding-rows :2340` · `register-verb! :2518` · the
  `__bindings` seam `:2970`.
- `matter_room.cljc` — `registered-master-ids :57` · `room-id-by-master
  :74` · `matter-actor :100` · the P3 pure-builder pattern `:159-250`.
- `facet_masters.cljc` — seven specs `:14-21` · `by-facet :29`.
- `episode.clj` — `utterance-import-request :654` (used `:747`).
- `material_circulation.clj` (server/rama/) — custody/gold law `:83-91` ·
  relation request path `:182-199`.
- `face_projection.clj` (server/rama/) — `experience-around-many` call
  `:657`.
- `material_portal.cljc` — seventeen questions `:153, :912`.
- `face_wiring.cljs` — `__portal` install `:372` · `:enterRoom :425`.
- `binding_dispatch_test.clj` — the closed-vocabulary equality test.
- Verification duties BEFORE code (the validation round's checklist):
  - **V1** — pin the say composition on disk: the import-request arity +
    actor threading; the non-spawning import shape vs `episode.clj`'s
    spawn law; the relation kind (existing, or one reviewed enum line);
    the exact circulation call site for the mark.
  - **V2** — `contextmenu` behavior on the actual canvas node: ordering
    vs mousedown, focused-textarea interaction (where must the listener
    sit so H1 holds inside focused blocks), keyboard menu-key freebie.
  - **V3** — the exact validation lanes that read
    `camera-gesture-reserved?` today (the "one var, three reads" space
    fence) — `meta-gesture-reserved?` must ride ALL of them; enumerate
    and pin each.
  - **V4** — every test pinning `verb-registry/names`, the
    `:durable-via-request` exact set, or endpoint disclosure — enumerate;
    each re-cuts in-phase, same commit.
  - **V5** — standing duty: every memory-derived platform claim checked
    against on-disk references.

## §12 Handoff

- Thread file: `build/halo/NOW.md` (STANDING frozen at open + newest-first
  entries). Receipt: `build/halo/P1.md` (diff-derived file list,
  falsifier verdicts with counterexamples, gate evidence). Gate record:
  `build/halo/GATE_P1.md`.
- Next session after the cut: ONE fresh default-fail validation round
  over this contract (V1–V5 + coherence). Minor-fail law applies
  (one-line sweeps land without a re-run); a substantive FAIL amends the
  contract in place and re-runs the round.
- Then: one fresh implementer context (the 5.6sol-xhigh lane) builds P1
  whole under §6; Fable orchestrates + gates.
- The package closes at: gate PASS + Sid's commit ruling + board flip.
  Retro joins the stratum batch (the 2026-07-27 cadence ruling). C2's
  clock arms at Sid's first wear session with the halo live.
