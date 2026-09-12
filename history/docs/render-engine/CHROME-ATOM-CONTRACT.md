# CHROME ATOM — one-pass contract (selection / manipulation / snapping / guides / drawing chrome)

Cut 2026-08-06 by Fable — **cutter model `claude-fable-5`, effort `max`** (the
model-routing law, CLAUDE.md; Sid's two-second header check). One-pass law:
`.claude/skills/work-package/SKILL.md` + decisions.md "How we work". Two
Sid-touches: this cut · the accept. The cut closes through ONE bounded
fresh-eyes falsification round, run as the armed A/B (identical brief pasted
into a fresh Claude session AND a Codex session, both at Sid's hand; pooled
findings, evidence-cited, no verdict authority, no recut; author repairs
in-session; the role-vs-model datum lands in the baton). The four-lens author
pass ran BEFORE the round (lens 1/4 Opus sweeps; lens 2/3 author judgment).
Structural precedent AT THE SOURCE LEVEL: the path atom and the connector atom
(both accepted 2026-08-06, landed `10dd8e5`). Do not imitate
IMAGE-ATOM/SEAM-STEP1 docs; they predate the law.

## Scope — what this atom is

Package 2 wave 4 (ENGINE.md §0: "Selection/manipulation · snapping · guides ·
drawing chrome (the chrome audit's demand classes land as material or code per
its findings)"; the board's NEXT line). One new tape family
`:render.family/chrome` — the first STORE-SLOT consumer of the tape's
overlay stratum (`compare-order` stratum-rank `{:world 0 :overlay 1
:region-composite 2}`, `scene_tape.cljc:550-600` — order DATA, never a
family tie-break; the stratum's EXISTING population is renderer frame
entries: the product-chrome ladder `frame-order :overlay 0..70` —
agent-background · command panel · status · settings · diagnostics,
`renderer.cljs:2824-2922`). Chrome's pinned place in that order: ABOVE
every `:world` entry, BELOW the existing product-chrome ladder
(`frame-order :overlay 0` is the ceiling) — the exact order-token encoding
is the implementer's, verified by S4's composition assertion; one line
from Sid re-ranks it. Plus the
interaction machinery whose truth it projects: a scene-level SELECTION model,
ARRANGEMENT manipulation (translate + fixture scale), an object SNAP engine,
and smart guides. Everything ships behind the SAME `?live-atoms=1` flag the
image/path/connector atoms ride; flag absent → byte-identical product.

**The hybrid metric law (this atom's novel obligation).** W0-C §0's hard
verdict: the screen flag "does not split anchor space from metric space" — no
provider may infer fixed-pixel behavior from a whole container's screen flag
(P0 demand 3). Every chrome form therefore declares its ANCHOR SPACE
(world/container-local geometry, transformed by the W2-A effective chain) and
its METRIC SPACE (screen pixels, immune to both container and camera scale)
as separate facts, and the chrome pipeline's own WGSL vertex law composes
them:

```
screen_pos = (anchor · container_affine · camera) + offset_px
```

`anchor` rides the same `ContainerTransform` struct + `is_screen` idiom every
family uses (`renderer.cljs:58-61,87-89`; `path_gpu.cljs:21-24,36-38`);
`offset_px` is applied AFTER camera, so handle sizes, border widths, guide
widths, and tick lengths hold constant screen pixels at every zoom. PRIOR ART,
named and not inherited: the rect shader already passes `border_widths` /
`corner_radii` through unscaled (`renderer.cljs:212-219`) — an implicit
screen-metric accident whose only production consumer is the marquee's 1.0px
border (`ground.cljs:3074-3078`). The chrome family DECLARES what that code
accidentally does; the rect pipeline is not modified.

**Selection (the model).**
- Scene selection EXISTS today as Task-18 attention state: private
  `!group-sel` unit-id set + shift-marquee verbs + a private AABB walk over
  `(:blocks @!world)` (`ground.cljs:150-153,3064-3101,3552-3570`). That walk
  is a second pick road — exactly the one-order/one-pick divergence W0-C P0.2
  names. This atom upgrades the model; it does NOT delete the legacy road
  (unflagged product stays byte-identical).
- Selection identity is the pick identity: `{:vi <view-instance> :address
  <address>}` (what `ss/pick` returns, `scene_store.cljc:361-384`) — blocks,
  fixture atoms, and connector edge-instances alike. State is SESSION TRUTH
  with no upstream (the same class as camera, decisions.md render-seam
  ownership law; Task 18's own Law-10 lineage: never saved, never restored).
  A new pure model owns it (`selection.cljc`); reload → empty; stale
  identities (target left the scene projection) are pruned at frame derive
  with a census count, never a crash.
- Transitions: **shift-click** toggles membership (pick-topmost through the
  one pick road) · **marquee** = shift+drag on empty ground — the gesture door
  that already exists and is reservation-legal (space-as-entity: naked
  drag/wheel at ground is CAMERA, structurally uncapturable; "tap and
  shift-marquee are bindable master material") · **plain tap / Escape /
  fresh marquee** clear. Clear-truth law: the new model's clear rides a
  callback installed at `clear-group-sel!`'s ENTRY — BEFORE its
  `(when (seq uids))` guard (`ground.cljs:1891-1895`), because a
  fixture-only selection leaves the legacy atom empty and the guard would
  otherwise skip the seam; `delete-group!`'s direct `!group-sel` reset
  (`ground.cljs:2936`) routes through the same fn (its one-line reset
  becomes the clear call). The tap branch's UNIVERSAL clear-pair
  (`ground.cljs:3872-3875`) stays universal for plain taps; the shift-tap
  conditional routes to the toggle INSTEAD of that clear-pair for the
  shift-tap case (otherwise toggle could never accumulate a second
  member).
- **Selection ≠ focus (CH-01).** Plain click keeps its first-light meaning —
  focus/caret — UNTOUCHED. This atom adds no click-to-select; selection
  arrives only via shift-click and marquee. The two states coexist and are
  visually distinct (focus = existing anatomy; selection = chrome outline).
- Marquee hits route through THE one truth: bounds-intersect over the store
  frame's `:targets-by-address` index (`scene_store.cljc:492`, stamped into
  every store frame at `:323` — the connector atom landed it), never
  `(:blocks @!world)`. TWO index facts are load-bearing and handled in
  writing: the index's bounds are ABSOLUTE CONTAINER-LOCAL
  (`scene_store.cljc:472-476`), so hit/candidate math COMPOSES them to
  world through the effective-transform map (`containers/transform-bounds`,
  pure `.cljc` — and this atom's own per-fixture containers make the
  identity shortcut false precisely for manipulable fixtures); and the
  index rows carry no family, so `addressed-rects`/`targets-by-address`
  rows gain an additive `:family` field (from node data; existing
  consumers key-read and are unaffected — named thin hook). Marquee-commit
  REPLACES the selection with the hit set (the legacy "fresh marquee
  dissolves" semantics; additive marquee is a named widening). Hit policy
  v1: AABB-intersect, declared as per-family data — with two pinned
  EXCLUSIONS evaluated off the new `:family` field:
  `:render.family/connector` instances (container-SPANNING bounds by the
  connector contract's own pick ruling — bounds-intersect would select
  every edge; connectors are shift-click-only day-one, through their
  honest narrow predicate) and all chrome/gesture slots (chrome never
  selects or snaps to itself). The population is CLIP-BLIND, exactly as
  product pick is today (`addressed-rects` walks children with no clip
  check, `scene_store.cljc:479-489`; `rect_tree` hit-test is an AABB walk
  with no clip participation, W1 §1) — a clipped-away descendant is
  selectable and snappable v1, declared honestly with a census count;
  clip-aware pick/marquee/snap is ONE named road, LATER, taken for pick
  and this population together (never one without the other). Flag-on,
  SHIFT-TAP is chrome-lane input WHOLESALE: on an addressed pick → the
  selection toggle; on empty ground → swallowed no-op — the
  `:pointer/tap :any → :anchor/place` floor row
  (`binding_material.cljc:488`) does NOT fire for a flag-on shift-tap,
  no clear runs, and no scene-descriptor action fires (the tap is the
  selection gesture, nothing else). Unflagged, every tap behavior is
  byte-identical.
- **One truth under the flag.** Flag-on, the new model owns selection and
  WRITES the legacy `!group-sel` atom as a derived projection (its block-unit
  subset) so every legacy reader — anatomy `:gsel?` tint, `delete-group!`,
  Escape — keeps working unmodified. One truth, one projection, declared;
  never two writers. Flag-off, the legacy verbs run untouched and the new
  model does not exist at runtime.
- Verb + door strategy. Verb impls live in a PRIVATE registry
  (`ground.cljs:3417` `defonce ^:private !verb-impls`; `:3424` `defn-
  register-verb!`), and press routing is NOT a table — it is
  binding-material resolution (`resolve-binding`, `ground.cljs:3662`) over
  served rows, whose claim chain drops every subject that is not a block
  (`ground.cljs:3292` `(when (contains? blocks (:claim/subject c)) c)`).
  Two consequences are law here:
  (a) the strangler seam requires ONE ground.cljs enabling edit (scalpel
  edit 4): `register-verb!` goes public AND returns the
  previously-registered impl — after which flag-boot RE-REGISTERS
  `:selection/marquee-begin` and `:placement/drag-group` from
  chrome_runtime, capturing the originals for composition. CHECKED CLAIM:
  impl re-registration is last-wins over the impls atom; a tripwire
  asserts the flagged registry serves the new impls; if not last-wins,
  the fallback is one thin dispatch line per verb body — a fork note in
  the baton, never a stop.
  (b) fixture and handle presses can never carry a claim through that
  chain, and a raw routing line in `decide` would bypass the closed
  binding vocabulary — so day-one, ALL chrome-lane manipulation rides the
  SHIFT-GESTURE door this atom already owns: the re-registered
  `:selection/marquee-begin` impl dispatches INTERNALLY on the press pick
  (empty ground → marquee sweep · manipulable fixture body → translate ·
  chrome handle → scale, corner from the handle's node data). No new verb
  name exists (no `verb_registry.cljc` edit); plain-drag routing for
  fixtures/handles is a refusal routed to binding-material rows at the
  authoring slice. BYTE-CONFIRMED at the round: the ground floor row
  `(floor-row :pointer/press :threshold #{:shift}
  :selection/marquee-begin 0)` (`binding_material.cljc:490`) routes EVERY
  shift+threshold press with no surviving block claim to the marquee verb
  — fixture and handle presses reach the re-registered impl with no
  routing edit anywhere. The remaining input door with no verb path is
  the SHIFT-TAP door (taps never cross the 4.0px drag threshold) —
  scalpel edit 3 in Entry points, with the empty-ground swallow pinned in
  the transitions bullet above.

**Manipulation (arrangement only — the scene-substrate vocabulary).**
decisions.md: "in-flight gestures stay client-side at 60Hz; settled
arrangements (where a container sits/scales) commit as assembly events."
This atom manipulates ARRANGEMENT — position and (for fixtures) scale —
never material geometry.
- **Translate, blocks — the input-shift composition.** The original
  `:placement/drag-group` internals are PRIVATE (`!world`, `arm-settle!` —
  `ground.cljs:3583,2096`); the composed verb therefore never reimplements
  or reaches into them. It CAPTURES the original impl (`register-verb!`
  returns the previously-registered impl — part of the one enabling edit)
  and, on `:move`, computes the raw candidate position from the SAME press
  fields the original uses (`press/grabs` origin + world delta,
  `ground.cljs:3575-3593`), runs `snap/resolve`, and hands the original
  impl a world point shifted by the snap delta — the original's move law
  is linear in the world point, so its computed position lands EXACTLY on
  the snapped value, its own `!world` merge + `set-transform!` +
  debounced settle (`arm-settle!`/`fire-settle!`, `ground.cljs:2052-2107`)
  run untouched, and the settle POST (cells `{:unit-id :x :y}`) carries
  snapped coordinates with no new fields and no schema change. Drag scope
  v1 = the pressed unit's legacy rigid drag group ONLY (`ground.cljs:
  3103-3113`); dragging a selected member does NOT move the rest of the
  selection — group drag needs the private grabs/press seam and is a
  routed refusal, never a reconstruction through private reads.
- **Translate + scale, fixtures (the shift-gesture door, public roads
  only):** `live_atoms.cljs` restructures its mount so each manipulable
  fixture (image · two inks · holed shape) registers its OWN container
  (today all nodes share `fixture-vi`, `live_atoms.cljs:173-206`; the
  connector fixture DEFS update their endpoint vi references to the new
  per-fixture vis — fixture data only, zero connector-namespace edits; the
  verifier's scenes are self-contained in `verifier.cljs`, so no golden
  can move). Fixture translate and handle scale run INSIDE the
  re-registered shift-gesture verb (scope bullet above) through
  `scene-rt/set-transform!` — a public road — session-only, no durable
  write; connector fixture edges bound to a moved fixture re-route through
  the existing `bound-edges-by-container` machinery (free regression
  receipt). Fixtures never enter `:press/grabs` or the settle lane.
- **Scale via corner handles, fixtures only:** 4 corner handles per selected
  fixture; the handle drag writes the container's `{:x :y :scale}` COMPOSED
  so the OPPOSITE corner stays world-fixed (the invariant is law; the exact
  algebra is the implementer's recorded default) — uniform only:
  free/non-uniform scale is a named refusal (it edits material aspect, not
  arrangement — and container transforms carry one scalar `:scale` today,
  `scene_runtime.cljs:145-147`). Blocks present NO handles (outline only):
  the settle lane has no durable scale field — block resize is a routed
  refusal, never an improvisation.
- **Rotate: refused** (the path atom's own chain: Q5 → frame-kind → W4).

**Snap engine (pure).**
- Candidates: addressed instances' bounds EDGES (left/right/top/bottom) +
  CENTERS (x/y) in WORLD space — each row's container-local bounds
  composed through the effective-transform map
  (`containers/transform-bounds`) before candidacy — read from the store
  frame's `:targets-by-address` index: the FULL frame population, no
  viewport cull (the walk is the batch road; a maintained/spatial
  candidate index is the named proportional road, LATER). Excluded from
  candidacy via the `:family` field: the dragged unit itself, all
  chrome/gesture slots, and connector instances (container-spanning bounds
  are not snap geometry). The moving geometry is the dragged unit's world
  AABB (v1 single-unit drag).
- Resolve: per-axis independent; snap when |candidate − moving-edge/center| ≤
  **8.0 screen px / zoom** (the screen-px→local division precedent:
  `path_material.cljc:486-488`; zoom read from the SAME live provider the
  pick slop uses). Deterministic ties: smaller distance wins; equal distance
  → center beats edge; still equal → smaller candidate coordinate. No
  candidate in threshold → identity (the gesture's raw position; no grid).
- Equal-gap: among the candidate neighbors on the snapped axis, detect
  equal-spacing triples (the moving box completing a gap equal to an existing
  adjacent gap, tolerance = the same 8.0px/zoom) → gap-tick chrome.
- The snapped delta is THE single source for both the applied transform and
  the guide geometry — a guide line may only be emitted for an alignment the
  applied position actually satisfies (exact coordinate equality in the
  resolved axis). This is the anti-gaming law; S3 freezes it.
- Counters, state-in/state-out (the connector cache shape,
  `connector_route` precedent): `:snap-resolutions` increments per resolve
  call; a static gesture step (unchanged candidate set + unchanged raw
  position) adds 0 derivation work.

**Chrome forms (the family's grammar — closed v1 set, widenable):**
| form | anchor space | metric (px) | pick |
|---|---|---|---|
| `:selection-outline` | target bounds, target's container | border 1.0 | `:none` |
| `:handle` (4/corner; centered on the corner; fixture instances only day-one) | bounds corner, target's container | quad 10.0×10.0 | interior + slop 6.0 px |
| `:marquee` | world sweep rect | border 1.0, translucent fill | `:none` |
| `:guide-line` | alignment span, world | width 1.0 | `:none` |
| `:gap-tick` | gap span midpoints, world | tick length 8.0, width 1.0 | `:none` |

Selected CONNECTOR instances draw NO chrome form v1 — membership + census
only (their honest selection visual needs route-based geometry; routed with
the connector-chrome refusal below). No ensemble/group bounds box exists v1
(per-instance outlines only; the group box + group handles are a routed
refusal).
Colors: one declared data map in `chrome_material` (straight RGBA, Contract C;
exact values implementer-recorded; marquee keeps its established translucent
blue family). Every form carries a full Contract-G declaration: geometry
authority = `:derived-chrome` (source = target identity + bounds + selection
revision, or gesture state), classification tri-state over the form's own
quad geometry, coverage `:aliased-v1` (the path/connector honest declaration),
`:pick :none` explicit where listed (absence is not a policy — W1 §2), handle
hit-slop `{:metric :screen-px :radius 6.0}` mapped through the inverse
transform at pick time — the full divide `6.0/(zoom ×
container-effective-scale)` via the live zoom AND effective-transforms
providers (the SLOP DIMENSION LAW, Entry points), never zoom alone.

**Chrome placement (how it stays crisp, and what motion costs).**
- Per-selected-target chrome rides a dedicated chrome slot in its OWN
  root-level container (`:stratum :overlay` — a SLOT field that already
  flows into the order token: `build-slot` destructures `:stratum`,
  `scene_store.cljc:98`; `slot-entry` reads `(or (:stratum slot) :world)`
  into `:order`, `:261` — no containers.cljc change exists in this atom).
  Container PARENTING is deliberately NOT used: `remove-container` throws
  on any container with children (`containers.cljc:166-169`) and block
  slots close on the ordinary reconcile path (`ground.cljs:2289-2291`), so
  a parented chrome container would crash routine slot lifecycle. Instead,
  target-following rides the connector's proven door: chrome_runtime holds
  a `bound-chrome-by-container` index and, at the frame edge, VALUE-DIFFS
  the effective-transform map (`identical?`/`=` per container — the
  `connector_route` precedent); a changed target container updates exactly
  its bound chrome containers' transforms via `set-transform!` (receipt:
  the `:chrome-transform-updates` counter — a moved target updates its own
  chrome only, siblings 0, static frames 0). CAMERA motion (pan/zoom)
  costs zero — the hybrid WGSL law absorbs it in-shader. Anchors are
  target-local bounds coordinates; px offsets ride the vertex data.
- Gesture chrome (marquee · guides · ticks) rides a chrome gesture slot in
  the world, updated by the ANCHOR PATTERN the legacy marquee already proves
  (upsert once, `set-transform!` per move, `refresh-marquee!`
  `ground.cljs:3071-3088`); guide/tick slots upsert only when the SNAP RESULT
  changes (value-diff — an unchanged alignment writes nothing), and close at
  gesture end. Snap-result identity for the value-diff is the resolved
  alignment set itself — value equality over `#{[axis kind
  candidate-coordinate] …}`; an unchanged set writes nothing.
  Gesture-lifetime is a declared Contract-M lifecycle fact: chrome never
  persists (M8's receipt proves the ABSENCE — reload → zero chrome slots,
  empty selection).
- The chrome derivation carries the render seam's five declarations
  (decisions.md): keyed inputs = (selection revision · member target
  identities + bounds · gesture state · snap result · zoom for pick-slop
  and thresholds only); doors = selection events, gesture events, and the
  per-container transform VALUE-DIFF at the frame edge (the connector's
  door; never a clock — no rAF/interval anywhere in chrome code); ownership
  = the chrome lane's session state, reset when the flag lane unmounts;
  projections = paint forward / pick reverse / census; oracle = a pure
  full-recompute fn (selection × store frame → expected chrome slot set)
  fenced against the maintained slots in a JVM tripwire (the
  `maintained_view_test` fence shape).

**Citizenship for an ephemeral family (the pin).** Chrome is the first
NON-MATERIAL family: no durable home, no provenance rows, no export
(authoring chrome does not export — W0-C P2 default). Its Contract-M
citizenship record declares this explicitly: grammar = the versioned form
spec above (fail-closed validation); identity = `[form target-vi
target-address corner]` with `corner ∈ #{:nw :ne :sw :se}` for handles and
`nil` for every other form (the pressed handle's corner rides its node
data — the scale gesture's opposite-corner law is uncomputable without
it); gesture forms carry gesture ids; provenance = `{:derived-from <target
identities> :selection-rev n}`; versioning = `algorithm-version` on the
derive; persistence/export = NONE BY DESIGN with receipts proving the
absence.
"All applicable receipts" (W1 §5.2) is satisfied by declaration + absence
receipts, never by silence.

## Ruled at cut — defaults with Sid veto slots (one line reverses any)

1. **Everything flag-gated day-one.** Unflagged product byte-identical
   (legacy Task-18 selection untouched). Unflagged activation of the new
   selection/chrome = a Sid word at the felt pass, not this atom's call.
2. **Selection ≠ focus; plain click untouched.** Selection only via
   shift-click + marquee day-one.
3. **Under the flag BOTH selection visuals show** (legacy anatomy `:gsel?`
   tint via the projection + the chrome outline). Redundant, honest;
   Sid's felt pass rules which survives.
4. **Snap threshold 8.0 screen px · handle 10.0 px + 6.0 px slop · chrome
   widths 1.0 px** — recorded defaults, one line retunes any.
5. **Fixture scale uniform-only** (one `:scale` scalar today); free/aspect
   scale refused → routed.
6. **Marquee policy `:intersect`** for all families (per-family data,
   `:contain` = the named widening).
7. **Snap candidates = addressed instances only** (the FULL frame
   population — clip-blind per #11, matching product pick; never "visible":
   no function computes visibility on this road) — no grid snap, no
   guide-to-guide snap day-one.
8. **Chrome-lane manipulation carries SHIFT v1** (fixture translate ·
   handle scale · marquee — one gesture door, internally dispatched);
   plain drag keeps its legacy meanings everywhere (blocks drag, ground
   pans).
9. **Drag scope v1 = the pressed unit's legacy rigid group** — selection
   membership does not extend a drag (group drag is a routed refusal).
10. **Chrome ranks BELOW the product-chrome ladder** (above all world
    content, under `frame-order :overlay 0..70` — selection chrome never
    covers the command/status/settings surfaces); one line re-ranks it.
11. **Marquee/snap population is clip-blind v1**, matching product pick
    exactly (census-counted); clip-aware pick + population is one named
    LATER road, taken together.

## Refusals — deliberately not in this atom (each routed, never a void)

- Clocked chrome (marching ants · pulse · laser · caret blink in chrome) →
  W4 animation clock/scheduler (W0-C O-CLOCK; no execution clock may be a
  derivation ancestor — decisions.md).
- Rotation handles + rotated-bounds chrome → W4 (the path atom's Q5
  frame-kind chain).
- Block resize (durable scale/size writes; the settle lane carries x/y only)
  → arrangement-write expansion + custody, reserved to Sid.
- Numeric distance/measurement labels (CH-07's label column) → measurement
  chrome expansion; the connector's proven `layout-label` road waits (NO text
  in chrome v1 — no new T-fence owners).
- Durable user-placed guides/rulers (material + custody + persistence) →
  material slice at custody opening.
- Path vertex/tangent edit handles (CH-05) → the authoring slice (editing
  gestures don't exist; chrome for them would be dead UI).
- Connector endpoint/reconnect chrome — endpoint handles, target outlines,
  snap circles, intended-vs-bound stubs (CH-06; the connector atom's routed
  debt to this wave) → RE-ROUTED to the authoring slice WITH CAUSE: they are
  authoring-gesture feedback, and the authoring gestures themselves are
  custody-reserved. Connector selection MEMBERSHIP is in (shift-click,
  census); connector selection VISUALS (route-following outline) ride the
  same connector-chrome slice.
- Ensemble/group selection bounds box + group scale handles → group
  manipulation expansion (per-instance outlines only day-one).
- Multi-select GROUP DRAG (dragging one selected member moves the whole
  selection) → the grabs/press seam expansion: it requires either private
  `ground.cljs` reads (`!world`, `press-record`) or a public grabs seam —
  never reconstructed; v1 drag moves the pressed unit's legacy rigid
  group only, with snap.
- Plain-drag (unmodified) routing for fixtures and handles → binding-
  material rows at the authoring slice (the closed binding vocabulary is
  the lawful router; a raw code line in the claim chain is not) — v1
  chrome-lane manipulation carries SHIFT.
- Hover/preselect unification (CH-22) → LATER; the existing hover surfaces
  stay untouched.
- Eyedropper / any O-READ chrome → W4 scene-color/readback contract.
- Crop/mask chrome (CH-02) → W4 masks.
- Coarse-pointer/touch hit variants + keyboard nudge → input floor T2.
- Accessibility mirrors for canvas chrome → LATER (W0-C P2.12).
- Scene undo/redo → the minted-diffs/ledger road (decisions.md open
  register: undo is a waiting consumer of write-site diffs; the only scene
  reversal today stays `fire-settle!`'s refusal revert).
- Align/distribute/duplicate/delete-verb changes on selection → verb-surface
  expansion (legacy `delete-group!` keeps working through the projection,
  unmodified).
- Dashed/stippled guide paint (no dash capability exists anywhere) →
  paint-axis expansion (path's refusal).
- Selection persistence/restore → never (session truth by design; Law 10).
- Chrome theming/contrast states beyond the declared color map → theme
  expansion (W0-C P1.8).
- Low-zoom simplification policies (W0-C CH-20) → regime expansion; day-one
  chrome renders at all legal zooms with constant px metrics, receipts
  tagged.

## Laws — pointers, not restatements (each with its operationalizing scenario)

- `W1.md` Contract O — one ordered truth, stratum/part-rank are DATA;
  chrome = the overlay stratum's first store-slot family, ranked below
  the existing product-chrome frame-entry ladder; paint forward, pick
  reverse, chrome first in reverse; no family tie-break anywhere. → S4.
- `W1.md` Contract G — unconditional geometry per form (authority,
  tri-state, coverage `:aliased-v1`, explicit `:pick :none`, screen-px
  hit-slop through the inverse transform); the hybrid anchor/metric
  declaration per W0-C P0.3. → S2, S4.
- `W1.md` Contract M — the ephemeral-citizenship pin above; fail-closed
  grammar (→ S1, S2); the M8 persistence-absence receipt is REAL, not
  declared: S4's no-persistence token check + S5's lived reload step; the
  M10 resource receipt is S4's upload-gate/budget/destroy rows (the path
  atom's established shape — device loss rides the existing
  system-recreate road, not a new per-atom receipt).
- `W1.md` Contract T — NO new text consumers. The T1 fence's owners array
  cannot see new files, so the operative receipt is S4's no-text token
  check over the five new namespaces; the owners array itself stays
  untouched. → S4.
- `W1.md` Contract C — chrome colors are straight RGBA through the tagged
  linear-premultiplied seam exactly like path (`configure-*-color-shader`
  family; default-OFF respected); the reference receipt covers both paint
  classes — translucent marquee fill AND an opaque form over colored
  content. → S4.
- ENGINE.md §0 gate sentence — the family REGISTERS through the W2-B seam
  (`family-ids` `scene_tape.cljc:12-22` · `registration` `:324-339` ·
  `family-contracts` `:445-458` · `register-family` fail-closed `:533-548` ·
  `frame-family-registry` `renderer.cljs:2984-3001` with its load-time
  cross-check); zero central branches; `verify_scene_tape_fence.mjs`
  families array gains chrome. → S4.
- decisions.md "The render seam" — the five declarations on the chrome
  derive; transform door = value-diff at the frame edge (connector
  precedent); no execution clock as ancestor; proportional to the affected
  set (parented containers make target motion zero-derivation). → S2.
- decisions.md settled architecture (scene substrate) — in-flight gestures
  client-side at 60Hz; settled arrangements commit as assembly events; the
  snapped position rides the EXISTING settle artery unchanged. → S3, S5.
- space-as-entity camera reservation — naked drag/wheel at ground stays
  camera at every tier; selection enters only through the shift doors.
  → S1, S5.
- Dark-lane laws (decisions.md) — all new namespaces load PURE; flag absent
  → no construction, no verbs re-registered, byte-identical product; zoom
  envelope legal [0.01, 1000], floor-default [0.1, 8.0] its own regime.
  → S4, S5.

## Exact entry points

New namespaces (ALL new code lives here):
- `src/app/client/workspace/selection.cljc` — the selection model (pure,
  state-in/state-out): the state value + revision · transitions
  (shift-click toggle · marquee-commit · clear · frame-prune with census) ·
  marquee world-rect math (press→pointer, the `marquee-rect` law) ·
  bounds-intersect hit policy over (index × effective-transform map) with
  local→world composition via `containers/transform-bounds` and the
  `:family` exclusions · the legacy projection (block-unit subset) ·
  census (`selected/pruned/blocks/fixtures/edges`).
- `src/app/client/substrate/chrome_material.cljc` — the form grammar +
  fail-closed validation · per-form Contract-G declaration data (the table
  above, exact) · the constants (snap 8.0 · handle 10.0/6.0 · widths 1.0) ·
  the color data map · `chrome-screen-rect` — the pure CPU TWIN of the
  hybrid WGSL law (`(anchor · container-affine · camera) + offset-px` →
  screen rect; S2's producer, which the chrome_gpu WGSL mirrors) · census
  mirror (`assert-corpus-coverage!` shape).
- `src/app/client/substrate/snap.cljc` — candidate extraction from
  (index × effective-transform map), local→world composed, `:family`
  exclusions + dragged unit excluded · per-axis resolve with the pinned
  threshold/tie law · equal-gap triple detection · the pure GESTURE-STEP
  pipeline (raw candidate position → snapped position + resolved alignment
  set + guide/tick geometry — ONE fn, the anti-gaming single source; the
  cljs wrapper only input-shifts by its delta) · counters
  (`:snap-resolutions`).
- `src/app/client/substrate/chrome_derive.cljc` — TWO separate code paths,
  by design: (a) the INCREMENTAL transition application (state-in/state-out;
  what chrome_runtime drives per event) and (b) the INDEPENDENT pure
  full-recompute ORACLE ((selection × store frame × gesture × snap) →
  expected chrome slot set); the fence compares (a)-accumulated state
  against (b) — never one fn fenced against itself. Slot specs are nodes
  carrying BOTH `[:data :chrome/material]` AND explicit
  `[:data :render/family :render.family/chrome]` (the rect-tree dispatcher
  recognizes `:render/family` first and DEFAULTS UNREGISTERED NODES TO
  HIT, `rect_tree.cljc:548-557` — an untagged chrome node would become a
  universal pick body). The five seam declarations live here · the
  `bound-chrome-by-container` index + `:chrome-transform-updates` counter ·
  bounded per-target accounting (`:chrome-derives` counter) ·
  `handle-hit?` — the pick predicate — CO-LOCATED with
  `set-live-camera-provider!` AND the effective-transforms provider in
  THIS namespace (the connector precedent: predicate + live providers in
  one home, `connector_route`; rect_tree's entry calls
  `chrome-derive/handle-hit?`; no live zoom provider exists anywhere
  today — this atom mints it, and S2 injects both directly in the pure
  lane). SLOP DIMENSION LAW: product pick inverse-transforms the point by
  the node's container effective affine BEFORE the family predicate runs
  (`scene_store.cljc:373-376`), so the predicate's local point is
  container-local — the 6.0px screen slop maps to local radius
  `6.0 / (camera-zoom × container-effective-scale)` (scale = the affine's
  axis length), NEVER `6.0 / zoom` alone: a chrome container following a
  scaled fixture would otherwise widen its hit body by that scale.
- `src/app/client/substrate/webgpu/chrome_gpu.cljs` — the hybrid-vertex WGSL
  (`(anchor·container·camera) + offset_px`; same `ContainerTransform` struct
  + `is_screen` idiom) · `init-chrome-system` / `prepare-chrome-frame!` /
  `chrome-entries` / `execute-chrome-batch!` / `destroy-chrome-system!` ·
  private interleaved vertex buffer (the path/connector shape,
  `path_gpu.cljs:77` precedent; own stride in `chrome_material`) ·
  gpu-budget registration.
- `src/app/client/workspace/chrome_runtime.cljs` — flag-boot wiring: verb
  re-registration capturing the returned original impls (the marquee
  shift-gesture dispatcher · the input-shift drag wrapper — both thin cljs
  glue over the pure `snap`/`selection` fns) · the clear-seam callback
  install · per-selection chrome slot lifecycle (register/close
  independent overlay containers) · the frame-edge transform-follow pass
  (value-diff over the effective map → `set-transform!` on bound chrome
  containers) · gesture chrome slots (anchor pattern; snap-result
  value-diff upserts) · provider installs
  (`chrome-derive/set-live-camera-provider!` + the effective-transforms
  accessor) · the `!group-sel` projection writes · publishes
  `globalThis.__softlandChromeReceipt` `{verbs-rebound prev-impls-captured
  clear-seam-installed providers-installed chrome-system}` at flag boot
  (the live_atoms/live_edges receipt precedent).

Thin hooks only (few lines each):
- `scene_tape.cljc` — `:render.family/chrome` in `family-ids` (:12-22) ·
  `chrome-registration` via `registration` with
  `[:grammar :entry-paint-required-keys] [:vertex-count]` (the path form,
  :398-417) · entry in `family-contracts` (:445-458) · require.
- `scene_store.cljc` — `:chromes` lane in `flatten-ops` (:52-57) /
  `stamp-ops-container` (:76-83) / `store-frame` + `:ops-count-by-vi`
  (:321-338), the path shape · `addressed-rects` (:472) /
  `targets-by-address` (:492) rows gain the additive `:family` field (from
  node data; existing consumers key-read, unaffected) — and while touching
  those rows, drop "visible" from `targets-by-address`'s docstring (:493
  says "every visible occurrence"; the walk is clip-blind, and that one
  source word seeded this round's population finding). NO order-token edit
  exists: `:stratum` is ALREADY a slot field flowing into `:order`
  (`build-slot` destructures it :98; `slot-entry` reads
  `(or (:stratum slot) :world)` :261) — chrome slots simply register with
  `:stratum :overlay`, and maintained view + batch oracle agree because
  both read the slot (containers.cljc is NOT touched by this atom).
- `rect_tree.cljc` — `:render.family/chrome` entry in
  `family-hit-predicates` (:536-546) calling `chrome-derive/handle-hit?`
  (predicate + zoom provider co-located there; handle forms classify by
  declaration + px slop, every `:pick :none` form returns miss) ·
  `tree->chromes` walk beside `tree->connectors` (:382), presence
  `[:data :chrome/material]` — and chrome nodes ALSO carry
  `[:data :render/family]` explicitly (the dispatcher default is HIT for
  unrecognized nodes, :548-557).
- `scene_runtime.cljs` — `register-face-instance!` (:145-147): CHECKED
  CLAIM — the slot map already carries arbitrary fields into `build-slot`
  (which destructures `:stratum`, `scene_store.cljc:98`); if the fn
  filters its slot keys, ONE passthrough line admits `:stratum`. No
  `:parent` support is needed (parenting is not used — see Chrome
  placement).
- `ground.cljs` — the SCALPEL LIST, exactly FOUR enumerated edits, all
  flag-inert (nil-default callbacks / flag-gated conditionals; nothing
  else in ground.cljs changes):
  (1) the callback seam at `clear-group-sel!` ENTRY, before the
  `(when (seq uids))` guard (:1891-1895) — chrome_runtime registers at
  flag boot; unflagged the callback is nil and behavior is byte-identical;
  (2) `delete-group!`'s direct `!group-sel` reset (:2936) becomes a
  `clear-group-sel!` call (same effect unflagged — the guard sees the
  same non-empty set);
  (3) the SHIFT-TAP conditional in the pointer-up tap branch (:3862-3891;
  shift readable via `:press/modifiers` :3884) — flag-on, a shift-tap is
  consumed WHOLESALE by the chrome lane: the universal clear-pair, the
  scene-descriptor `dispatch-action` call, AND the `:pointer/tap`
  dispatch ALL skip — addressed pick → the selection toggle (no node
  actions fire); empty ground → swallowed no-op (so the `:anchor/place`
  floor row, `binding_material.cljc:488`, never fires on a flag-on
  shift-tap); plain taps and ALL flag-off behavior byte-identical;
  (4) `register-verb!` goes public AND returns the previously-registered
  impl (:3424 — `defn-` → `defn` plus returning the swap's prior value;
  its verb-registry validation untouched) — the strangler + composition
  seam.
- `renderer.cljs` — the path atom's exact 4-hook shape: require ·
  `frame-family-registry` entry (:2984-3001) with
  `{:contract … :produce chrome-gpu/chrome-entries :execute! …}` ·
  `draw-frame!` kwarg + one `prepare-chrome-frame!` call beside path's
  (:3162-3163) · frame-map key.
- `runtime/render.cljs` — pass `:chrome-system` at the one `draw-frame!`
  call site.
- `live_atoms.cljs` — per-fixture containers (each manipulable fixture its
  own vi/container; connector fixtures untouched) · chrome system
  construction in `augment-pipelines!` (:216-245) · chrome_runtime boot.
- `electric_flow.cljc` — one chrome_runtime mount line beside the live-edges
  mount (:756-757).
- `test/app/test_runner.clj` — register `selection-test` ·
  `chrome-material-test` · `snap-test` · `chrome-derive-test` in
  `pure-namespaces`.
- Verifier lane (append-only, the path/connector shape): `verifier.cljs`
  `run-chrome-atom!` (3 golden cases + zoom-parity + dark-lane +
  arrangement + the `:hybrid-metric` receipt block) wired into the
  top-level `Promise.all` (:2557-2589) · `run_verifier.mjs` chrome rows +
  `chromeAtomInputs` source-digest set + one-shot `--append-chrome-goldens`
  (preflight: prior banks byte-identical + no existing `chromeAtomCases` +
  exactly 3 rows) + `--append-chrome-input-amendment` · `manifest.json`
  gains `chromeAtomInputs`/`chromeAtomCases` ·
  `verify_scene_tape_fence.mjs` families array (:63-71) gains chrome.
  SHARED-FILE LAW: this atom's thin hooks touch shared sources
  (scene_tape/scene_store/rect_tree/containers/renderer) — the prior
  families' input digests SHIFT; run the existing
  `--append-*-input-amendment` road for image/path/connector in the same
  change, never a golden edit.

## Decisive scenarios (frozen as tripwires + goldens at close)

1. **Selection truth** [JVM]: transitions exact (toggle/add/clear;
   marquee-commit REPLACES); marquee hits = bounds-intersect over
   (`:targets-by-address` × effective-transform map) with local→world
   composition through NON-IDENTITY container affines (the fixture case)
   across zoom stations incl. legal-domain tags, and the `:family`
   exclusions — connector instances and chrome slots never enter marquee
   hits or snap candidacy; the legacy projection equals the block subset
   after every transition, including the fixture-only case (projection
   empty, model non-empty — the clear seam still fires: the callback sits
   before the legacy guard, asserted at the model level); frame-prune
   drops vanished identities with census, never crashes; malformed
   identities/forms refuse by name (fail-closed); the model exposes NO
   transition for a naked-drag input (the camera reservation's model
   half; the live half is S5's receipt — `verbs-rebound` lists exactly
   the two rebound verbs, `:camera/pan` absent).
2. **Chrome derive + hybrid law** [JVM]: (selection × frame) → slot specs
   deterministic; the ORACLE fence — the incremental transition
   application's accumulated state == the independent full recompute after
   every transition sequence (two code paths, never one fn against
   itself); proportionality — a 1-target selection delta changes exactly 1
   chrome slot (`:chrome-derives`), and a target transform STEP through
   the value-diff door updates exactly its bound chrome
   (`:chrome-transform-updates` == moved-target count, siblings 0, static
   frames 0); every slot spec carries `[:data :render/family
   :render.family/chrome]` (asserted — an untagged node is a universal
   pick hit); per-form Contract-G declarations complete (explicit
   `:pick :none` everywhere but handles; handle identity carries its
   corner); `chrome-screen-rect` — the CPU twin of the WGSL law — exact at
   stations [0.01 0.1 1 8 100 1000] (px sizes constant, anchors scale,
   non-identity container affines composed); `handle-hit?` hits inside
   and misses outside the 6.0px screen radius at every station INCLUDING
   under a scaled chrome container (the full divide
   `6.0/(zoom × container-scale)` — a zoom-only wrong build must fail the
   scaled-fixture case; both providers injected directly in the pure
   lane).
3. **Snap truth + anti-gaming** [JVM]: candidates from (index ×
   transforms) exclude the dragged unit, chrome, and connectors — with
   non-identity fixture containers in the test frame; per-axis resolve is
   ABSOLUTE — the gesture-step output places the snapped edge/center
   EXACTLY at the candidate's world coordinate (equality with the
   CANDIDATE, not merely internal consistency; a wrong-units build that
   keeps guide and position self-consistent must fail this) at the
   8.0px/zoom threshold across zoom stations; deterministic ties
   (distance → center-over-edge → smaller coordinate); equal-gap triples
   detected within tolerance; the ONE gesture-step fn returns snapped
   position + alignment set + guide/tick geometry together — every
   emitted guide's claimed alignment is satisfied by the returned
   position, no guide exists without its satisfied alignment, and the
   input-shift law is asserted at the pure level (raw position r, snapped
   s ⇒ the wrapper's shift δ = s − r reproduces s under any linear move
   law); identity when no candidate; counters — `:snap-resolutions` ==
   resolve calls, an unchanged step adds 0. (The live glue — the wrapper
   handing the shifted world point to the captured original impl, the
   settle POST carrying those coordinates — is S5's drive: the felt drag
   lands flush and the ack round-trips unchanged.)
4. **Tape citizenship + goldens** [verifier]: chrome entries sort after ALL
   world entries AND BELOW the existing product-chrome ladder — the
   composition assertion drives a scene with a world block + selection
   chrome + the command-panel overlay entries and asserts the order
   world < chrome < `frame-order :overlay 0..70` in BOTH the maintained
   view and the executor tape (stratum + rank receipt; a build that paints
   selection chrome over the command panel must fail); paint forward /
   pick reverse with a handle returned FIRST over a block beneath it
   (arrangement gate); the GPU-RESOURCE receipts (the path atom's shape,
   Contract M10): uploads occur only on chrome mesh-set change (upload
   gate), the gpu-budget registration is present, and
   `destroy-chrome-system!` releases its buffers against the budget
   tracker — M10's remaining behaviors are NAMED, not silent: budget
   refusal mirrors the path system's established policy (same receipt
   shape); device loss rides the existing system-recreate road (Laws
   above); asset-unavailable is N/A BY GRAMMAR — the family loads no
   external assets, and the no-text token check is its absence proof;
   3 goldens appended —
   (a) selection outline + handles over mixed block/image/path/connector
   content at zoom 1 · (b) the SAME scene at zoom 8 · (c) marquee +
   alignment guide + gap ticks at zoom 0.1 — with the `:hybrid-metric`
   receipt asserting BOTH halves of the law across (a)/(b): measured chrome
   px widths/sizes EQUAL while world content scales 8×, AND chrome anchors
   TRACK the scaled content (outline corners coincide with the target's
   scaled bounds within a declared ε — a pure-screen-space wrong build that
   holds px widths constant must fail the anchor half); determinism ×2; the
   Contract-C receipt covers BOTH paint classes — the translucent marquee
   fill AND one opaque form (outline over colored content) against the CPU
   linear-premultiplied reference (the path `run-color-receipts!` shape);
   the NO-TEXT/NO-PERSISTENCE static receipt — the chrome verifier block
   token-checks ALL SIX new namespaces (`selection` · `chrome_material` ·
   `snap` · `chrome_derive` · `chrome_gpu` · `chrome_runtime`) for
   text-layout/font requires and persistence surfaces (fetch/localStorage),
   both absent by contract (the
   T1 fence's hardcoded owners array cannot see new files — this receipt
   is what makes the Contract-T law falsifiable here); ALL existing
   goldens byte-identical (verifier scenes are self-contained in
   verifier.cljs, so the live_atoms fixture restructure cannot move them;
   image/path/connector input amendments recorded for the shared-source
   digests, never golden edits); the MSDF counterexample stays RED; the
   scene-tape fence (families + chrome) and the untouched T1 fence stay
   green.
5. **The felt receipt** [Sid's eyes, `?live-atoms=1`]: shift-click a real
   block and a fixture ink → outlines (+ fixture corner handles);
   shift-marquee a mixed group (connectors stay unselected by sweep);
   drag the block — its rigid unit moves with alignment guides + gap
   ticks firing against neighbors, release settles the snapped position
   durably through the existing artery (ack unchanged) while its
   connector edges follow AND its chrome follows through the
   transform-follow pass; shift-drag a fixture body to translate it;
   shift-drag a corner handle to scale it (opposite corner fixed); zoom
   0.1 → 8 — every chrome form holds constant px width/size, crisp;
   Escape/tap clears everywhere through the one seam INCLUDING a
   fixture-only selection (the pre-guard callback); RELOAD the flagged
   page — selection empty, zero chrome slots survive (the M8 absence
   receipt, lived); `__softlandChromeReceipt` reads `{verbs-rebound
   [the two verbs only] prev-impls-captured true clear-seam-installed
   true …}`. Flag absent → byte-identical product: tripwire extends the
   path flag-off receipt (pipelines unaugmented · no verbs re-registered ·
   all new namespaces load pure · zero chrome constructs).

## MUST-NOTs

Never read `src/app/server/env.clj` · NO durable/Rama writes beyond the
EXISTING block-geometry settle POST for translate (body stays
`{:cells [{:unit-id :x :y}] :camera …}` — no new fields, no scale writes,
no schema change; fixtures/selection/chrome are client-session values) ·
never edit existing goldens/manifest rows or the MSDF RED case — append
only; shared-source drift rides the input-amendment road · no
hand-positioned central draw/pick branch · no new JS/npm dependency · no
execution clock anywhere in chrome code (no rAF/interval/`js/Date` in
derive paths) · NO text rendering in chrome (no new T-fence owners; the
owners array is untouched) · no second transform path (W2-A effective
transforms + the one chrome WGSL law only) · no second pick road under the
flag (marquee/selection hits ride the store frame index; the legacy AABB
walk survives ONLY in the unflagged legacy verb) · `ground.cljs` = the
FOUR enumerated scalpel edits ONLY (clear-seam at entry · delete-group
reset routes through it · shift-tap conditional · register-verb!
public + returns-previous), each flag-inert; the claim chain, `decide`,
`press-record`, `!world`, and `arm-settle!` are UNTOUCHED ·
`renderer.cljs`/`electric_flow.cljc` thin hooks only; zero edits to
path/connector/image namespaces (libraries/precedents; live_atoms.cljs's
named hooks are the one exception)
· commits only at Sid's word — code and docs separate, both on
`docs/current-mental-model-local`, exact-path staged, never push.

## Close

Scenarios 1–3 freeze as ~5 tripwires across the four pure JVM namespaces
(`selection_test` · `chrome_material_test` · `snap_test` ·
`chrome_derive_test` — all pure-lane; no IPC need exists in this atom);
scenario 4's goldens + receipts join the permanent bank via the one-shot
scoped append road; focused suite = the four namespaces GREEN + `npm run
verify:render-engine` whose ONLY red is the preserved MSDF counterexample
(W1's canonical exit — never "fixed" by this atom). Foreign failures are
board debt, never stops. One NOW entry (≤15 lines, self-audit included) in
`CHROME-ATOM-NOW.md` + the board line flip. Acceptance = Sid's word.

## Codex opening prompt (implementation — after the cut closes)

```
You are implementing the CHROME ATOM for Softland's render engine — one
pass, whole atom, one session.

Read first, nothing else needed:
1. docs/render-engine/CHROME-ATOM-CONTRACT.md   (this contract)
2. docs/render-engine/W1.md                     (contracts O/G/M/T/C your family satisfies)
3. .claude/skills/work-package/SKILL.md         (the one-pass law: build, close, when to ask)

The path and connector atoms are your structural precedents AT THE SOURCE
LEVEL ONLY — path_material.cljc / path_gpu.cljs (family + private vertex
buffer shape), connector_route.cljc (live providers, value-diff door,
bounded cache + counters), live_atoms.cljs (flag lane), the path/connector
blocks in verifier.cljs and run_verifier.mjs (the append road). Consume
them as precedents; ZERO edits to them except live_atoms.cljs's named
hooks. Do NOT read PATH/CONNECTOR docs beyond their contracts if you need
scope comparison; never read IMAGE-ATOM-*/SEAM-STEP1 docs (pre-law).

This contract has been through its four-lens author pass and its bounded
falsification round — build what is WRITTEN; the repairs are law.

Build the WHOLE atom straight through. Keep your own falsification pass
and fix what it surfaces in-session. Ambiguity → strongest default + a
note in docs/render-engine/CHROME-ATOM-NOW.md. A genuine fork (two
readings that cannot both hold) is ONE question in that file — route
around it and keep building; never stop. Foreign test failures are board
debt, never stops. The checked claims to verify FIRST (minutes, not
hours): register-verb! impl re-registration is last-wins (and the
public+returns-previous edit compiles warning-free); shift+drag whose
claim chain yields no block subject resolves to the marquee verb (Task
18's row) — a fixture press must reach the re-registered impl; the
:stratum slot field flows through register-face-instance! into
build-slot; the :family additive field breaks no targets-by-address
consumer.

Close per SKILL.md: tripwires + goldens frozen from the contract's
scenarios; focused suite: the four pure JVM namespaces GREEN, and
verify:render-engine red ONLY on the preserved MSDF counterexample (its
canonical state — do not fix it). NOW entry ≤15 lines, board flip.
Commits only at Sid's word. Acceptance is Sid's word — you never wait on
a review round.
```
