# CONTRACT — face-2: the camera ground (scene-store birth · world camera · spec-shaped face) · v1.1 — PROPOSED

**Status: PROPOSED (Fable, Trunk-4, 2026-07-06, authored at 719a9d9 while the
t4 branches build — text only, no code). v1.1 amends v1 after
`CONTRACT_VALIDATION_R1.md` returned FAIL (2 blockers, 2 should-fixes, 3
advisories — all folded; the FAIL artifact is kept verbatim per process).
B1: the Δ13 clip gate collided with this contract's own rect_tree fence →
the ONE lawful rect_tree edit is now named (§6). B2: the Δ9 server-side
placement fired on imagined-demand grounds against D-001 + R-2 §2.1's
binding form-break condition AND was unreachable in the allowlist
(trail-view mirrors object-container only; relations are client-composed)
→ Δ9's plural named projections land CLIENT-side this wave; the Rama
placement is pre-staged (§10) behind R-2 §2.1's frame-profile trigger.
Honest-ledger note for the sitting: the Trunk-4 standing order's phrase
"Δ9 named plural layout projections in Rama" is deliberately NOT satisfied
at the server this wave — validation found it conflicts with binding docs;
Sid may override at countersign with his own authority. S1: the chrome
"separate camera buffer" citation was FALSE (clone-text-system shares
camera + bind-group; the separate update-camera call is dead code) —
corrected in §3.2/§4.5, new duty §8.8, gate G8b. A second validation round
is Sid's call at countersign. COUNTERSIGN HAPPENS AT SID'S SITTING, AFTER
R-2 FIRST LIGHT — the sitting's evidence (staircase cured or not, band
pressure, feel verdict) rides into the countersign. The face-2 BUILD wave
does not open without it.**

Authority chain (binding first): `decisions.md` D-001 (trigger pacing),
D-002/D-005 (the trail view, view-first), D-006 notes (delivery mode),
D-008 (read-only MVP — nothing here writes), **D-009 (ONE SUBSTRATE — this
contract is ordered under it; full record `../render-north/FORK-2.md`)**;
`../render-north/DELTA-B1.md` (the ordered instrument — the Δ-numbers below
are its items; their verdicts and triggers are adopted, not re-derived);
`../render-north/PROBE-10K.md` (measured obligations, §2.5);
`../trail-room/CONTRACT_R2.md` (the material this face builds over — its
gates are floor, never contradicted); the OI-2 ruling (MSDF temporary, SLUG
destination); `design/claude/render-demands-2026-07-05.md` (walls W1–W5,
demands D1–D15) and `design/claude/room-card-lane-2026-07-05.md` (R1–R7).
NORTH/MECHANICS/HARD-PROBLEMS are direction-grade inputs; **where this
contract names one of their items, that item becomes binding here** — the
documents themselves stay non-binding.

Terminology: "face 2" is the threaded/DAG timeline face per the face-order
ruling (decisions.md, ruled 2026-07-04) — the trail ground Sid reads daily.
It exists and renders today (WP-B2 + R-1 + R-2). **This contract does not
build a new face; it promotes the existing face's substrate** to the
camera-world model: promote, wire, generalize — never replace (NORTH §8).

---

## §1 Purpose and consumers

**Purpose.** Birth the one substrate's retained heart under its birth laws,
and put the first pixel face on the world camera before habits calcify
around the scroll interim (wall W4: the DOM-outliner-shaped shortcut is the
single most expensive one). Concretely, six Δ-items whose DELTA-B1 triggers
all name this contract: Δ1/Δ2 (birth laws), Δ3 (keyed scene store — the
core), Δ4 (overlay stratum, same commit family), Δ6 (actions become data —
slots must be serializable), Δ9 (named plural layout projections —
CLIENT-side this wave, v1.1/B2; the Rama placement is pre-staged §10 behind
R-2 §2.1's frame-profile form-break), Δ10 (spec-shaped view params), Δ11
(per-spec token face). Plus the
pre-staged trigger cascade (§10): the camera going live within this wave
lawfully fires Δ13 (one clip) and Δ14 (gesture routing off zones); Δ3 plus
RAF evidence fires Δ15 (conditional RAF); a new view entering fires Δ12
(registry). Δ5 (one tree for render and hit) is subsumed by Δ3's falsifier.
Δ8 (ephemeral timers leave the content flow) fires with Δ4 by its own
trigger text.

**Consumers, in order:** (1) Sid's daily ground — the read loop over real
material at zoom; (2) agents — the per-spec token face (D15: citizenship,
not export); (3) every later face — face-3 canvas, rooms, the forge — which
consume the store, the spec shape, and the projection registry rather than
re-deriving them; (4) the D-008.5 write milestone — the overlay stratum is
shaped to host its optimistic-write layer; (5) the H2/H3 bets — the token
face is their measurement surface.

**Why now (D-001 grounds, not imagined demand):** the store's trigger is
this contract by DELTA-B1's own text ("the first face that renders world +
islands over live diffs — not before"); the drift-bug class (second tree at
click time, `mouse.cljs`) and the O-2 shimmer-reshape are used-form breaks
already on record; PROBE-10K §4 proved the as-built incremental bridge
corrupts under `:permutation` — the C2-shaped store is the recorded cure;
W4 says the camera is cheapest at the first pixel face, and R-2's first
light is that face's settling moment.

## §2 Birth laws — these head the contract because retrofit touches everything

**LAW B1 (Δ1, ruled by Sid at the Track-D sitting): the store is keyed
`(view-instance-id, address) → slot`, with a fan-out index
`address → #{(view-instance-id, address)}`.** Meaning lives at the address;
geometry/window-state lives at the appearance. Picking returns the pair:
the ✕ closes an APPEARANCE (view mutation); retraction acts on an ADDRESS
(a log assertion, when writes exist). A store keyed by address alone will
eventually delete truth when someone meant to close a window (H6).
View-instance ids are **stable values minted at view entry** (data in
view-state, deterministic under test) — never per-frame, never random (N3).

**LAW B2 (Δ2, ruled — N6): the store's world coordinates are f64 (plain
CLJS numbers — nothing to build); GPU buffers only ever hold
camera-relative (anchor-relative) f32.** The subtraction happens at encode;
the anchor is the camera cell, re-based on cell crossing so precision never
decays as the camera travels (a fixed origin silently becomes absolute-f32
again — trap 17). Hit-testing runs in f64 world space client-side
(pointer screen→world in f64), so picks cannot drift where pixels would.
As-built is f32-absolute end to end (`renderer.cljs:7,41` shader math;
`camera-floats (js/Float32Array. 6)`; instance packs) — safe only because
zoom=1; this wave is the birth moment N6 names.

**LAW B3 (the update wall): the store is updated ONLY at the
reduce/consumer edge — never inside `m/latest`** (the CLAUDE.md law as
architecture). The encoder reads exactly ONE store snapshot per frame
(value semantics; a frame never renders half-old half-new — H4's epoch rule
at v1 cost: one deref before encode).

**LAW B4 (probe obligation, PROBE-10K §6 — carried from R-2 §2.5, now at
store grain): order and position are DATA on rows (rank fields, arrival-ms,
lane/thread attributes), never sequence position. `:permutation` ops never
carry the change signal from our own producers; the store's order is an
indirection over rank data.** A permuted input canonicalizes to the
identical scene. (The measured knee: reorder-shaped diffs die at 10³ in the
producer; a 10⁴ permutation applies in 6.5ms through indirection — the
consumer was never the problem.)

**LAW B5 (the six-op consumer — C2 shape): the store consumes the incseq
vocabulary directly** (`:grow :degree :shrink :permutation :change
:freeze`): `:change` → slot write; `:grow`/`:shrink` → slot lifecycle
(freed slots cleared — a stale slot masks truth); `:freeze` → settled rows
leave the diff stream. **DOM-shaped mount contracts are not reusable here**
(PROBE §4: `gpu-mount` ended 91 frames with 16,750 slots for 100 entities);
the as-built bridge is evidence, not a component — it is quarantined, not
promoted (§7 disposition).

## §3 Scope — what this wave builds

### 3.1 The store and the projector (Δ3 + Δ4 + Δ6 + Δ8; Δ5 subsumed)

- **NEW `src/app/client/substrate/scene_store.cljc`** (pure cljc — the GPU
  is a cache of this store; JVM tests exercise it fully): keyed slots per
  LAW B1, six-op consumer per B5, order-indirection per B4, fan-out index,
  EDN-round-trippable slot values (no closures — Δ6).
- **The per-entity projector** (H1's grain, NEW pure cljc under
  `trail_face/`): entity row(s) + layout-projection row + spec → that
  entity's resolved appearance value — its instance data, its hit shape,
  its token rows. One entity's change re-runs ONE entity's projection
  (bounded: an entity emits 1–50 instances); the old/new instance sets diff
  structurally into slot writes. Islands still resolve their interiors via
  `rect_tree` in local 2D (Δ21 LEAVE; N4) — the projector CALLS the island
  grammar, the result lands in the store. Cross-entity structure (threads,
  lanes, folds) is NOT the projector's job — it arrives as rows (§3.3).
- **Draw, hit-test, diff-apply, and the token projection read the same
  store instance** (Δ3 falsifier; retires the drift-bug class — no
  `build-*-tree` reachable from a trail-path mouse handler).
- **Transport this wave (honest and pre-staged):** the existing pull path
  feeds a client-side keyed differ that EMITS the six ops into the store
  (NORTH §9's fallback shape — measured to carry 10³ comfortably; today's
  corpus is 10²–10³). Boundary-1 `e/diff-by` incseq streaming is a
  transport swap BEHIND the same store interface, activated on scale
  evidence, never pre-paid (NORTH §9). Nothing above the store may know
  which transport fed it.
- **The overlay stratum (Δ4):** ephemeral instances keyed by address,
  driven by view-state atoms, composed `base ⊕ overlay` at encode. The
  world stratum stays `identical?`-skippable. Shimmer/caret/pending timers
  move OUT of the content flow (Δ8 — the O-2 counterexample is the reason).
  The stratum is SHAPED to later hold H7's optimistic-write entries
  (request-id-keyed, "proposed" channel) — shape only; D-008.5 fills it.
- **Actions become data (Δ6):** scene/slot data carries descriptors
  `{:action <kw> :target <address>}` resolved at dispatch against a
  registered handler map (NEW, small). Registry admits READ-ONLY kinds this
  wave (`:open :close-appearance :zoom-to :follow :toggle-band
  :copy-address` class); world-changing kinds are REFUSED at registration —
  D-008's wall, with D-008.5 as the named extension point. A descriptor IS
  a proto-ActionRequest; nothing else changes shape at the write milestone.
  `rect_tree.cljc` itself stays UNTOUCHED (§6 fence): descriptor dispatch
  lives beside the store; if that proves impossible, stop-clause.

### 3.2 The camera goes live (W4 discharge; Δ2 encode; Δ13; Δ14)

- **Zoom wires from constant to value.** The camera struct already carries
  `zoom` in all four content shaders (rect / rich-rect / MSDF / slug —
  clear-quad is cameraless, correctly; v1.1/A1); `1.0` is hardcoded at
  exactly four call sites (`renderer.cljs:1516,1517,1559,1562` — grep
  `update-camera`, line numbers drift; NOTE v1.1/A3: the `:1562` chrome
  call is DEAD code — its `:1561` guard is always false, see the chrome
  bullet below); `update-camera` already takes the parameter; slug AA is
  already zoom-aware (`renderer.cljs:320`). Wiring is "pass the live
  value" — the retro's O-3 verdict, verified again at 719a9d9.
- **Camera = data in the spec** (§3.4): `{:x :y :zoom}` per view-instance.
  The trail ground gets cursor-anchored wheel zoom (the dummy's
  perceptually-linear exponential model; the world point under the cursor
  is invariant across a zoom step) and drag-pan on empty ground (Δ14 FSM).
  Scroll-as-camera continuity: the face's scenes are ALREADY
  scroll-independent ("the camera carries scroll," `scene.cljc` ns
  docstring) — pan-y generalizes it, no scene change.
- **Every other mode keeps an identity camera** (zoom=1, pan=(0,−scroll)):
  behaviorally untouched; migration is opportunistic later (Δ12's clock,
  not this wave's).
- **Chrome/rim renders under an identity camera on a NEW dedicated uniform
  buffer + NEW bind-group** (v1.1/S1 — the v1 claim of an existing separate
  buffer was FALSE: `clone-text-system` (`renderer.cljs:902-915`) shares
  the parent's camera buffer AND bind-group, so chrome would zoom WITH the
  world — exactly the trap-7 swim this bullet exists to prevent; the
  "separate" chrome `update-camera` at `:1562` is dead code behind an
  always-false guard at `:1561`). The build allocates chrome's own camera
  buffer + bind-group (duty §8.8; gate G8b). The as-built `+scroll-y`
  counter-compensation in chrome coordinates retires WITH that change —
  under a live zoom the compensation trick lies (trap 7). Rim behavior
  (four slots, never zooms) is R-1 law and does not change.
- **Encode discipline (LAW B2 lands here):** pack fns take
  `(world-f64, anchor)` pairs; no buffer writer receives absolute world
  coordinates; anchor re-base on camera-cell crossing. Zoom changes with no
  band crossing re-encode ZERO world instances (uniform-only frame — trap
  8's gate); band crossings re-project only entities whose representation
  changed.
- **One clip representation (Δ13 — trigger fires when the camera wires).**
  The three parallel CPU clip implementations (retro friction §5) converge
  to ONE: islands push world-space clip rects onto a clip stack; every
  instance carries a clip-stack index; the fragment shader resolves it
  (rect + MSDF + slug pipelines; stride grows accordingly — a named,
  bounded renderer surgery). Shadow pipeline is OUT of this wave's clip
  set (v1.1/A2 — trail bands 0–2 are typography, no shadows; a band-3
  island's shadow is whole-island and ground islands are not
  parent-clipped; trigger to bring shadows in = the first clipped-shadow
  visual defect or a clipped-island-shadow demand). Text clips at pixel
  grain — the line-pop per-op drop retires, which requires the ONE lawful
  `rect_tree.cljc` edit named in §6 (v1.1/B1: `tree->text-ops` performs
  the per-op drop/truncate at `rect_tree.cljc:305-349` and the trail face
  flattens through it, `combined_text.cljs:284` — the retirement cannot
  happen anywhere else). Fully-outside culling stays a CPU optimization.
  Hit-testing resolves against the SAME clip data (a point outside a
  clipped region does not hit clipped content). Clip-stack depth cap 8,
  validator-enforced, honest error past it. Hardware scissor demotes to
  batch optimization (it is not the correctness mechanism — MECHANICS §5
  trap b).
- **Gesture routing moves to hit-results (Δ14 — same trigger).** For faces
  under the live camera: mousedown latches the hit path (pointer capture —
  drags keep routing when the pointer outruns the target), intent =
  hit-result kind × modifiers × thresholds, thresholds in SCREEN px by law
  (a 4px drag is 4px at every zoom), wheel = zoom-to-cursor on ground /
  island-local scroll over scrollable islands, Esc abandons. The FSM's
  decision fn is pure (JVM-tested over synthetic hit results). The zone
  cascade remains for non-camera modes (correct and LEAVE until their
  migration).

### 3.3 The named layout projection registry (Δ9 — client-side this wave; v1.1/B2)

- **World layout is derived data**: deterministic, log-projected, no
  wall-clock, sort by address — H1's line and H3's law (no world position
  may depend on another entity's rendered size — D6 is the firewall that
  keeps world layout O(1) per change).
- **Projections are NAMED and PLURAL from birth, running CLIENT-side at
  diff rate** (never per frame). A small projection registry maps
  projection-id → pure cljc fn over the client-assembled feed, returning
  address-keyed rows `{address → {:thread :lane :rank :band?}}`. The
  first named projection: **`:layout/trail-time-lane@v1`** — exactly R-2's
  thread/lane/band assignment (connected components over lineage kinds,
  fold-first precedence, band for the unthreaded, arrival-ms rank),
  registered under its name, not rewritten. The second:
  **`:layout/trail-flat@v1`** (single-column time order, no thread
  structure — the dense overview/agent lens). The view-spec's
  `:projection` field names which one it reads (Sid's world-and-lenses
  demand, satisfied at the spec layer). Re-running a projection over the
  same feed state reproduces positions byte-identically (Δ9 falsifier's
  determinism half).
- **The Rama placement is DEFERRED, pre-staged, and stays a lift** (v1.1
  B2 — validation traced two independent kills of the v1 server-side
  scope): (a) grounds — R-2 §2.1 (COUNTERSIGNED) binds the promotion to
  frame-profile form-break evidence, and none exists at 10²–10³ corpus
  (PROBE-10K §3/§6 + H1 both say client-side is fine here); DELTA-B1 is a
  direction-grade instrument and cannot override a countersigned
  contract's D-001 gate; (b) mechanics — the trail-view module mirrors
  ONLY object-container PStates (`trail_view.clj:286-300`); relations
  reach the client via foreign queries + client-side `assemble-feed`
  (`:596`, WP1 §7 client-composition), so a server-side
  connected-components would need a NEW cross-module mirror + gather
  topology, which "queries ONLY — additive" never authorized. The
  promotion trigger and its cost are pre-staged in §10; the transforms
  stay pure and address-keyed with rank fields as data so the move remains
  a lift, not a rewrite (the same do-not-preclude R-2 carried).

### 3.4 The spec-shaped face (Δ10) and the per-spec token face (Δ11)

- **Δ10 — one EDN spec value carries the face's parameters from birth:**
  `{:spec/schema-version 1, :query {…bbox × band × kind SHAPE, executed
  client-side…}, :projection :layout/trail-time-lane@v1, :camera {:x :y
  :zoom}, :policy {:band … :folds …}, :style-rules …, :actions …,
  :lineage …}`. The schema-version field and a migration hook exist FROM
  BIRTH (Sid's spec-of-specs demand — the grammar must be forkable later;
  the version field is the do-not-preclude, costing ~nothing now).
  The face's full render state reproduces from (spec value + feed state)
  alone; changing a param is a data edit; the spec round-trips EDN with
  version intact. `!trail-face-state` holds the spec VALUE; ephemerals
  (hover, drag) live in overlay-stratum atoms, never in the spec.
  **Specs-as-assertions (N1) is an explicit NON-goal** — it waits for the
  write surface (D-008.5); spec-SHAPE does not wait (W5).
  View-state that must survive re-projection is keyed by ADDRESS
  (selection, expansion — already target-id-keyed in R-2), never by
  position (H10's rule, adopted at zero cost now).
- **Δ11 — the token face generalizes per-spec:** a pure cljc projection
  `(store snapshot, spec) → token rows` — addresses, marks, fold counts,
  action descriptors — beside the store (NEW
  `src/app/client/substrate/token_face.cljc`; pure cljc runs on BOTH
  hosts, which is what one-substrate honestly means for the token family).
  WP1's server `render-bundle-text` (View 3 bundles) stands unchanged;
  `text_face.cljc` (the verbatim View-3 DISPLAY face) stands unchanged.
  Fold policy lives in the spec and MUST carry counts that sum to the
  unfolded set (ledger 16 at the token layer). Pixel face and token face
  of one spec agree on the address set and mark set — the drift test is
  the wall W3/D15 gate. Shaped for walk-as-context (rows keyed by address
  join future walk rows); no capture built (D-008.5).

### 3.5 NOT in scope (each refusal names its extension point)

- **No write surface, no spec assertions in the log** (D-008; N1) →
  D-008.5 milestone; the overlay stratum and descriptor registry are its
  prepared seams.
- **No expression-language interpreter/compiler** (H2) → `:style-rules`
  exists as a data FIELD consumed by hand-built builders; the compiler
  enters when agent-authored specs are real (the forge), designed then
  under H2's partition rule (`[:zoom]`-outermost).
- **No interest-query execution** → shape in the spec only (NORTH §11.7);
  activation is a backend change at H5 scale.
- **No new GPU primitives, no depth buffer** (Δ17); **no proportional/
  shaped text** (Δ18/H9 — the cost tag stands); **no text-run interning**
  (Δ16 — [RAF]-objection-gated, pre-staged §10).
- **No CRDT/multiplayer machinery**; the LWW line (N5) is ruled at
  D-008.5.
- **No behavior change to editor/sidebar/dg/settings/cmd-panel** —
  mode-branch wiring lines only; their camera stays identity.
- **No Rama module beyond the trail-view query surface**; no kernel
  schema changes.
- **Band→zoom mapping**: the spec's `:policy` maps camera zoom to band;
  R-2's `/trail band` command becomes one WRITER of that policy value —
  the builders' band parameter (R-2's shape) is unchanged. If R-2's
  as-landed shape differs materially, verification duty §8.1 catches it
  before code.

## §4 Adjudications (Fable, recorded; alternatives preserved)

**4.1 Store placement: `src/app/client/substrate/` (substrate-grade), not
`trail_face/`.** Grounds: D-009 — the store IS the one substrate's client
heart; every later face consumes it; placing it under one face invites a
per-face fork (the exact three-substrate failure D-009 closed). Cost of
wrongness low: pure cljc moves cheaply. Alternative not taken: birth it
under `trail_face/` and promote later — rejected because the promotion
would be a rename touching every consumer at the exact moment a second
face appears (the worst time).

**4.2 Slot values hold the entity's RESOLVED appearance (instances + hit
shape + token rows), not flat instance arrays only.** Grounds: hit-testing
needs containment paths (MECHANICS §1); the token face needs rows; flat
instances would force a second structure for each (trap 18) — the store
must be the single source for all three reads. The island's interior tree
(rect_tree output) is retained per-entity as the hit shape. Alternative
not taken: store-as-flat-SoA-only — right for GPU, wrong as the sole
structure; the pack path derives SoA from slots at encode (H5's two-data-
models rule: EDN at diff rate, typed arrays at frame rate, the projector
is the boundary; nothing downstream of it allocates per frame).

**4.3 Transport: client-side keyed differ emitting six ops now; boundary-1
incseq later.** Grounds: PROBE §6 — the fallback carries 10³ comfortably
(current corpus 10²–10³); wiring `e/diff-by` end-to-end needs product
entry points PROBE explicitly left unmeasured (threat 1); the store
interface is identical under both. Falsifier for the swap: `[RAF]`/pull
cost evidence at larger corpus. Alternative not taken: wire Electric
incseq streaming now — pre-pays an unmeasured integration during the same
wave that births the store; two unknowns in one wave violates the
delivery-mode shape.

**4.4 Δ9 runs client-side in a named projection registry this wave; the
Rama placement waits for R-2 §2.1's frame-profile form-break.** (v1.1/B2;
full grounds §3.3. Alternatives preserved: v1's server-side query — killed
on D-001 grounds + allowlist unreachability; a PState fold ETL — the
eventual materialized form, which would today breach WP1 gate 14's
structural read-only.)

**4.5 Chrome camera: identity uniform on a NEW dedicated buffer +
bind-group; counter-compensation retires.** (v1.1/S1 — corrected: chrome
currently SHARES text-sys's camera buffer and bind-group via
`clone-text-system`; no separate buffer exists; the separate-looking
`update-camera` call is dead code.) Grounds: trap 7. Alternative not
taken: keep compensation and freeze chrome at zoom=1 by arithmetic — a
lie that holds only while pan is 1-D vertical.

**4.6 Camera continuity for non-camera modes: the frame passes per-mode
camera values; nothing global flips.** The 4 hardcoded `1.0` sites become
pass-throughs; editor et al. pass `(0, −scroll, 1)` — bit-identical
behavior. Alternative not taken: a global camera atom shared across modes
(one mode's zoom would leak into another's frame).

**4.7 View-instance minting: stable data at view entry** (e.g.
`[:trail-ground 1]`-class values held in view-state) — deterministic in
tests, stable across re-projection (H10/N3). Alternative not taken:
gensym/random ids — kills replay determinism and address-keyed state
survival.

## §5 Traps ledger (naive alternative → concrete failure → ruling)

1. **Store keyed by address alone** → second appearance of a transcluded
   claim overwrites the first; ✕ on an appearance retracts the claim —
   truth deleted by a window close (H6). → LAW B1 pair keying + fan-out;
   picking returns the pair. Cite at the store's key fns.
2. **Absolute world f32 in GPU buffers** → invisible at zoom=1 today;
   glyph jitter/swim at land scale; retrofit = every buffer writer
   (MECHANICS §4). → LAW B2; encode-time subtraction; G2/G3.
3. **Order rides `:permutation`** → producer knees at 10³, dies at 10⁴
   (PROBE §2: 12–74s per reorder) → LAW B4; rank fields as `:change`;
   indirection in the store; G5/G6.
4. **Reusing the DOM-shaped mount bridge** → slot-pool corruption: 16,750
   slots for 100 entities, 160ms frames (PROBE §4) → LAW B5 C2-direct;
   `gpu-mount` quarantined (§7); any insert-before-shaped consumer is a
   stop-clause.
5. **Store written inside `m/latest`** → CLAUDE.md side-effect ban breach;
   diamond-glitch double-applies a diff → LAW B3; updates at the
   reduce/consumer edge only; falsification hunts this class.
6. **Ephemeral state watched by the world/content flow** → the as-built
   O-2 failure: a pending tool-card forces full text re-shape at 2Hz →
   Δ4/Δ8 overlay stratum; world stratum skippable; G7.
7. **Chrome counter-compensation kept under live zoom** → rim/chrome swims
   the moment zoom ≠ 1 (compensation is scroll-shaped, not
   camera-shaped) → §4.5 identity camera on chrome's own uniform buffer.
8. **Zoom re-runs layout / zoom-aware flexbox** → N4 breach; frame cost
   O(scene) per zoom tick; every system that tried it paid (NORTH Am. 1)
   → camera is a uniform; islands resolve local 2D then project; G9
   (zoom with no band cross re-encodes zero world instances).
9. **Hit-testing a second/stale tree** → the retro's drift-bug class
   (`mouse.cljs:325,374` as-built counterexample) → Δ3/Δ5: hit reads the
   frame's store snapshot; G4; grep-gate on `build-*-tree` in mouse paths.
10. **Face params scattered across atoms/code** → W5 breach — the face's
    views stop being data; the interpreter door closes → Δ10 spec value
    with `:spec/schema-version` from birth; G11.
11. **Band derived by truncation or hardcoded thresholds in builders** →
    D3 breach (semantic LOD stops being data); tripwire 4's cousin →
    `:policy` maps zoom→band in the spec; builders keep their band
    parameter (R-2 shape).
12. **Token face as a second serializer over different data** → W3/D15
    silent divergence — what Sid sees and what agents read drift apart
    (the map lies to its other audience) → Δ11 reads the same store+spec;
    G12 drift gate.
13. **Folds hide counts at the token layer** → ledger-16 breach: an
    altitude hides that marks exist below → fold rows carry counts summing
    to the unfolded set; G12b.
14. **Clip consolidated onto hardware scissor** → screen-space scissor
    breaks under a zooming camera; axis-aligned only; useless for later
    rotation/3D (MECHANICS §5 trap b) → Δ13 per-instance world-space clip
    index in-shader; scissor = optimization only; G14.
15. **Action closures in store slots** → non-serializable store: EDN
    round-trip dies, token face can't print actions, replay impossible
    (the one hole N2 names) → Δ6 descriptors + registry; world-changing
    kinds refused (D-008); G13.
16. **Camera/animation state in the log or spec lineage now** → history
    replays contain easing junk (MECHANICS §9 trap a); N1 entered
    prematurely without a write surface → camera is spec-value
    view-state; assertions wait for D-008.5.
17. **Camera-relative to a FIXED origin** → precision decays as the camera
    travels; the discipline silently degenerates to absolute-f32 → anchor
    = camera cell, re-based on crossing; G2 includes the far-origin case.
18. **Store holds only flat instances** → hit paths and token rows need
    second structures; three structures drift (the clip-triplication
    pattern at store grain) → §4.2 slots hold resolved appearances.
19. **Line-number citations trusted from prior docs** → PRIMITIVES'
    `!zoom-factor` cite (`global_flow.cljs:59` ⓘ) is already stale — no
    such atom exists at 719a9d9 → verification duties §8: re-grep every
    anchor; agent-sourced ⓘ cites are hypotheses.
20. **NUL bytes / non-text artifacts** (fired 4× historically) → `file(1)`
    must say text for every touched file; G19.
21. **"Cloned system = separate GPU resources" assumed** → v1 of THIS
    contract shipped the error: `clone-text-system` shares camera buffer +
    bind-group, the "separate" chrome camera call is dead code, and the
    plan would have zoomed the rim with the world (caught by validation
    R1/S1, would otherwise have surfaced only at first light) → every
    per-stratum camera claim names the buffer AND bind-group it rides;
    duty §8.8; gate G8b.

## §6 File allowlist

**NEW:** `src/app/client/substrate/scene_store.cljc` ·
`src/app/client/substrate/token_face.cljc` · per-entity projector +
gesture-FSM + spec ns as pure cljc under `src/app/client/workspace/
trail_face/` (names the plan's; one builder per file) · test namespaces +
fixtures (`test/resources/trail_face/` extensions; fixtures mirror live
constructors, never invented shapes).

**AMEND:** `renderer.cljs` (camera pass-through at the four `1.0` sites;
clip-index attribute + shader resolve for rect/MSDF/slug; NEW chrome
camera buffer + bind-group per §4.5; pack fns take (world, anchor); stride
updates) · `runtime/render.cljs` (consumer edge: store snapshot read,
camera values, idle-encode + GC instrumentation columns) ·
`trail_face/{scene,cards,lanes,wiring}` (consume projection rows + spec;
emit descriptors; overlay split) · `runtime/state.cljs` (spec value +
overlay atoms) · `mouse.cljs`/`scroll.cljs` (trail-path routing → FSM;
zone cascade stays for other modes) · `editor_compute.cljs`/
`combined_text.cljs` (mode-branch lines) · `agent_flow.cljs` (`/trail`
spec-param commands) · **`rect_tree.cljc` — the ONE lawful edit (v1.1/B1):
`tree->text-ops` (and, only if the T-4 clamp interacts, `tree->rects`)
gains an opt-in clip mode that EMITS a clip-stack index instead of
applying the per-op drop/truncate; default behavior byte-identical for
every non-camera consumer; layout resolution, hit-path fns, `dispatch-
event`, and the node grammar untouched. Any rect_tree change beyond this
named edit = stop-clause.**

**UNTOUCHED (= stop-clause on touch):** all server files (v1.1/B2 —
`trail_view.clj` is OFF the allowlist; face-2 now touches zero server
code) · relation/object kernels · `git_spine.clj` · `buffer_pool.cljs`'s
`gpu-mount` (quarantine = docstring deprecation at most, no behavior
edit) · dg_flow / sidebar / settings / cmd_panel behavior ·
`electric_flow.cljc` beyond wiring lines · `env.clj` (never read).
LEAVE-ledger items Δ17–Δ22 touched at all = a gate finding (DELTA-B1's own
rule), with TWO lawful exceptions: Δ19's frame loop may change exactly
where Δ15's trigger fires (Δ19's own sunset text), and the named
`tree->text-ops` clip-emission edit above — which is Δ13's declared
territory (the per-op drop IS one of the "three parallel CPU clip
implementations" Δ13 exists to converge), not a Δ21 layout-grammar touch.

**Fence note (branch collision):** this contract is TEXT while t4 branches
build. At build time it consumes the R-2 end-state of `trail_face/*` and
t4-spine's landed `claimed-ms`/`trail_view.clj`; §8.1 re-verification is
mandatory, and the build does not open while any branch fence is live.

## §7 As-built dispositions (so "leave" is a verdict, not silence)

- `!sidebar-scene` cached-scene pattern: the store's embryo — superseded on
  the trail path by the store; sidebar itself untouched (its migration is
  opportunistic, Δ12's clock).
- Buffer pools' three diff engines: remain under the store as the GPU
  write layer (slot pools are exactly the C2 target PROBE measured).
- `gpu-mount` bridge: quarantined with a docstring pointing at PROBE §4;
  never driven by `:permutation`-bearing streams.
- Dirty-present machinery (`use-persistent-render-target? false`): stays
  off until Δ15's trigger; it is the mechanism Δ15 switches on.
- The `[RAF]`>5ms instrument: gains an idle-encodes-per-second counter and
  a GC-pause column (H5: the dragon gets a gauge the day the camera goes
  live).
- Zone cascade in `scroll.cljs`/`mouse.cljs`: LEAVE for non-camera modes;
  replaced by the FSM only where the camera is live.
- `text_face.cljc` (View-3 verbatim display) + WP1 `render-bundle-text`:
  unchanged; Δ11's per-spec token face is a sibling, not a replacement.

## §8 Builder verification duties (before code)

1. **Re-read the R-2 END-STATE as landed** (BRANCH_REPORT_R2 + the landed
   diff): builder signatures in `cards.cljc`/`lanes.cljc`/`scene.cljc`,
   the component/moves ns name, `:trail-face/thread`/`:band?` node data,
   the prev-assignment cache atom — this contract cites R-2's CONTRACTED
   shape; the landed shape wins where they differ (flag drift in the
   phase artifact).
2. Re-grep every code anchor here (line numbers drift; trap 19):
   `update-camera` call sites; `camera-floats`; slug AA dilation;
   `!trail-face-state`; chrome `+scroll-y` compensation sites
   (`combined_text.cljs`, `dg_flow.cljs`); `mouse.cljs` click-time tree
   rebuild sites; `hit-test`/`dispatch-event` in `rect_tree.cljc`.
3. Verify glyph coverage for any NEW glyph against the CURRENT atlas +
   BRANCH_REPORT_D7's outcome (G17's source of truth).
4. Confirm the feed's projection inputs (`:relation-transition` details,
   arrival-ms, claimed-ms from t4-spine) at the landed shape.
5. Spike ONE far-origin f32 quantization case in a REPL before writing G2
   (pin the ε and the failing absolute-f32 counterpart with real numbers).
6. Confirm shader stride math for the clip index against the existing
   28/12/24-float layouts (rect/MSDF/slug — shadow's 20-float layout is
   OUT of the clip set this wave, v1.1/A2) before touching pack fns;
   budget the change per pipeline.
7. `clojure -M:test` compiles the new cljc namespaces on the JVM before
   any wiring (the B2 placement-ruling spike, repeated).
8. **Chrome camera isolation (v1.1/S1):** confirm `clone-text-system`
   still shares camera buffer + bind-group at build time; allocate
   chrome's OWN camera buffer + bind-group; delete or repair the dead
   guard/call at `renderer.cljs:1561-1562`; wire G8b.

## §9 Acceptance gates (numbered; executable unless marked visual)

- **G1 (Δ1/B1):** one address in TWO view-instances with independent
  geometry; an address-level write patches both slots via the fan-out
  index; picking each returns (same address, distinct appearance). Pure
  JVM.
- **G2 (Δ2/B2 math):** far-origin entity (~1e9) at deep zoom:
  `f32(world − anchor)` error < 0.5px while `f32(world)` error exceeds it
  by orders of magnitude; anchor re-base on cell crossing preserves the
  bound. Pure JVM float math (duty §8.5 pins constants).
- **G3 (B2 discipline):** no pack/encode fn accepts absolute world coords
  (signature takes (world, anchor)); one encode-path assertion verifies
  emitted values are anchor-relative. Grep + test.
- **G4 (Δ3/Δ5):** draw ops, the hit result, and token rows for one fixture
  all derive from ONE store snapshot (identity-checked in test); no
  `build-*-tree` call reachable from trail-path mouse handlers (grep gate).
- **G5 (B5):** a scripted six-op stream (incl. `:degree`, `:freeze`, a
  `:permutation`, interleaved `:grow`/`:shrink`) applies with patch-vec
  equivalence (PROBE's verification pattern); freed slots are cleared; a
  permuted-input fixture canonicalizes to a byte-identical scene (B4).
- **G6 (B4 API):** assignment/order APIs return address-keyed maps; rank/
  order are node/slot DATA; no API returns a re-sorted sequence as a
  contract surface. (R-2 G7's form, at store grain.)
- **G7 (Δ4/Δ8):** with a pending-shimmer overlay active, the world-stratum
  build fn executes ZERO times across N ticks (counter assertion); the
  base stratum value is `identical?` across those ticks; overlay composes
  at encode.
- **G8 (camera/W4):** cursor-anchored zoom invariant — the world point
  under the cursor is fixed across a zoom step (pure math); camera lives
  in the spec value; chrome stratum output is invariant under camera
  change. **G8b (v1.1/S1, renderer-integration half):** chrome-text-sys
  carries a camera-uniform-buffer AND bind-group that are NOT
  object-identical to text-sys's (asserted at init), chrome's camera
  updates with identity values every frame, and the dead guard at the old
  `:1561-1562` site is gone; visual confirmation (no rim swim at zoom
  0.5/2) rides G-FL.
- **G9 (trap 8):** a zoom change with no band crossing dirties zero world
  slots (uniform-only frame; counter assertion); a band crossing
  re-projects only entities whose band representation changed.
- **G10 (Δ9, reworded v1.1/B2):** the projection registry resolves
  `:layout/trail-time-lane@v1` and `:layout/trail-flat@v1` by name; each
  returns address-keyed rows; re-running one over the same feed state is
  byte-identical; the scene build consumes ROWS (projection fns run at
  diff rate — **zero PER-FRAME world-layout recompute**, asserted by
  counter across N frames with a static feed); TWO named projections over
  the same fixture render two distinct scenes by a spec `:projection`
  data edit alone.
- **G11 (Δ10):** the spec round-trips EDN with `:spec/schema-version`
  intact; (spec + feed fixture) → scene is reproducible (same pair, same
  scene, twice); each spec param change is a data edit exercised by test
  (band, projection, camera).
- **G12 (Δ11):** drift gate — pixel scene and token rows for one (spec,
  fixture) agree exactly on the address set and mark set; **G12b:** folded
  token output carries counts that sum to the unfolded set.
- **G13 (Δ6):** a scene/store value round-trips EDN with descriptors
  intact and replays identically; every descriptor kind is registered;
  registering a world-changing kind FAILS (D-008 wall test).
- **G14 (Δ13):** ONE clip path — flatten in `:emit-index` mode (the §6
  lawful `tree->text-ops` edit) emits clip-stack indices and drops/
  truncates NOTHING; non-camera consumers on the default mode produce
  byte-identical ops to pre-wave behavior (regression half); a point
  outside a clip does not hit clipped content (hit half); depth cap 8
  enforced with an honest error. Visual half at first light: text clips
  at pixel grain under fractional zoom.
- **G15 (Δ14):** the FSM decision fn yields the same intents at zoom
  0.5/1/2 with the world panned (synthetic hit results, pure test);
  thresholds asserted in screen px; drag latches the mousedown hit path.
- **G16 (Δ15, conditional by design):** IF the trigger fires (Δ3 live +
  idle-cost evidence): diff-quiet + no animation → zero encodes/sec; a
  diff arrival encodes within one frame (no lost wakeup — the O-3 death
  class stays dead). IF NOT fired: the idle measurement + the deferral
  decision are recorded in the phase artifact — measurement-gated
  deferral is a passing outcome.
- **G17 (Δ7 join):** the face's design-language glyph set renders per
  BRANCH_REPORT_D7's outcome — slug backend if Δ7 landed; otherwise the
  OI-2 interim stands RECORDED with the named blocker (silent MSDF
  re-commitment is a gate finding). Tofu-with-advance for missing glyphs
  (V3-5) holds either way.
- **G-B3 (v1.1/S2 — trap 5's enforcing gate):** no store mutation
  (`swap!`/`reset!`/slot write) is reachable from inside an `m/latest`
  body or a projector fn; the store mutates only at the named
  reduce/consumer edge — asserted by grep over the store's mutation API
  call sites plus a test that the projector fns are pure (same inputs →
  same value, no store delta).
- **G18:** ONE serial cross-package suite green at the then-current HEAD
  baseline (never a hardcoded count), one JVM.
- **G19:** `file(1)` says text for every touched file.
- **G-FL (visual, at first light with Sid):** trail ground at zoom
  0.5/1/2 panned — rim constant and unzoomed, kraft connectors legible,
  no chrome swim, no clip pop; screenshots into the phase artifact.
- **G-Δ (the instrument's own slot):** at gate time, read DELTA-B1
  against the wave's diff — every LEAVE item untouched (Δ19 exception per
  §6), every touched FIX/EVOLVE item's falsifier green, every fired
  trigger's Δ in the contract. A miss on any is a gate finding by
  DELTA-B1's own rule.

## §10 Pre-staged trigger cascade (paced, not planned — D-001)

| Δ | Trigger | Status in this wave |
|---|---|---|
| Δ12 registry | the NEXT new view/mode added | pre-staged: face-2 adds no new mode; IF one appears mid-wave it enters via registry entry, zero dispatch-site edits |
| Δ15 conditional RAF | Δ3 live + idle-cost evidence | in-wave, measurement-gated (G16 both outcomes valid) |
| Δ16 interning | `[RAF]` objection on a text-heavy face | pre-staged only; the instrument exists |
| Δ9 Rama placement (v1.1/B2) | frame-profile form-break on the client projection pass (R-2 §2.1's binding condition) | client registry ships now; transforms stay pure/address-keyed so the move is a lift; needs a cross-module relation-PState mirror + gather topology, priced here so nobody discovers it mid-wave |
| H7 optimistic overlay | D-008.5 write milestone | stratum SHAPED this wave (Δ4), filled then |
| H2 spec compiler | agent-authored specs (the forge) | `:style-rules` field exists as data; compiler designed then under `[:zoom]`-outermost |
| Interest-query execution | H5-scale material | query SHAPE in spec now (Δ10) |
| N1 specs-as-assertions | D-008.5 | lineage field shape present, unasserted |
| N5 LWW line | D-008.5 | default-to-accumulate stands until ruled |

## §11 Process

Delivery mode (D-006 notes 2026-07-05): coding batched — internally
ordered store → camera/clip/FSM → projection/spec/token — then ONE serial
test batch, then ONE batched falsification (hunt by CLASS: "absolute
coords in a buffer writer", "store write outside the consumer edge",
"second tree", "ephemeral in a world flow", "drifted token projection")
plus a Fable gate (`GATE_REVIEW_F2.md`), then per-package CODE commits on
Sid's word (code/docs separate). The fresh-context validation round RAN
2026-07-06 (Opus, default-FAIL): **FAIL — 2 blockers, 2 should-fixes, 3
advisories, all folded into this v1.1** (`CONTRACT_VALIDATION_R1.md`, kept
verbatim; B1/B2 were real Fable-authored text errors — the round's kill
record goes to the D-006 honest ledger at the wave note). A second round
is Sid's call at countersign. Countersign at Sid's sitting after R-2
first light; the build wave opens only after both countersign AND all t4
branch fences closing.

## §12 Stop clause

Standard form: unbuildable / platform-wrong / binding-doc conflict → stop,
classify (implementer-fixable vs policy fork), escalate forks to
decisions.md Open Questions as PROPOSED with verbatim citations; never
improvise policy. Pre-flagged stops: any `rect_tree.cljc` change beyond
the §6 named `tree->text-ops` clip-emission edit (v1.1/B1) · any
insert-before-shaped diff consumer (trap 4) · clip-index stride change
breaking an existing pipeline's visual identity for non-trail modes ·
any server-side Δ9 work this wave (v1.1/B2 — the placement waits for
R-2 §2.1's form-break evidence; attempting it without that evidence is a
stop, and its mirror+gather cost is priced in §10) · R-2's landed shape
contradicting a §3.3 assumption (flag, amend, honest-ledger note).
Implementer-fixable examples: glyph substitutions (record); stale line
anchors (re-grep, duty §8.2); fixture extension mechanics.

## §13 Input manifest (D-006 counterfactual-probe feed)

`decisions.md` (D-001..D-009 + D-006 notes) · `build/render-north/
DELTA-B1.md` + `FORK-2.md` + `NORTH.md` + `MECHANICS.md` +
`HARD-PROBLEMS.md` + `PROBE-10K.md` · `build/trail-room/CONTRACT_R2.md`
(+ its landed BRANCH_REPORT_R2 at build time) · `design/claude/
render-demands-2026-07-05.md` (W1–W5, D1–D15) + `room-card-lane-
2026-07-05.md` (R1–R7) · `build/render-substrate-retro/PRIMITIVES.md`
(as-built inventory; trap 19 applies to its ⓘ cites) · `build/view-mvp/
MEASUREMENT_RAF.md` (RAF baseline + OI-2) · code at 719a9d9:
`renderer.cljs` (shaders/camera/`update-camera` sites), `buffer_pool.cljs`
(`gpu-mount` §419-437), `rect_tree.cljc` (`:actions`:51,
`dispatch-event`:407), `trail_face/*` (esp. `scene.cljc` docstring,
`text_face.cljc`), `runtime/render.cljs`:443-449, `runtime/state.cljs`:198,
`trail_view.clj`:286-300,596 (mirror set + client-side `assemble-feed` —
B2's mechanical basis) · `.claude/skills/work-package/SKILL.md` ·
`build/face-2/CONTRACT_VALIDATION_R1.md` (the R1 FAIL round, verbatim).

## §14 Handoff

Implementer: the face-2 wave (post-sitting; Fable-direct or fresh-context
subagents per delivery mode — Sid's call at wave open). Reviewer gate:
§11's batched falsification + Fable gate, receipts in `GATE_REVIEW_F2.md`.
After green: first light with Sid over the real corpus at zoom — G-FL
evidence + the H1-family screenshot of the ground under a live camera;
`[RAF]` + GC + idle columns recorded. Then the D-008.5 conversation has
its prepared seams (overlay, descriptors, spec lineage) and face-3 has a
substrate instead of a precedent.
