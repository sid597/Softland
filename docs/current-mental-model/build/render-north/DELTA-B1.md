# DELTA-B1 — the delta instrument: ideal × as-built × demands, one ordered list

**Status: DIRECTION-GRADE INSTRUMENT.** Commissioned by Sid at the Track-D
sitting of 2026-07-05/06 (Act 3 of the guided-read session). This document
diffs three legs — **ideal** (`NORTH.md` + `MECHANICS.md` + `HARD-PROBLEMS.md`,
as amended by the sitting's rulings), **as-built**
(`../render-substrate-retro/PRIMITIVES.md`), **demands**
(`../../design/claude/render-demands-2026-07-05.md`) — into ONE ordered work
list for the render substrate. **It does not start code.** It feeds the next
render contract; every item is D-001-paced (named trigger, never imagined
demand) and falsifiable (named check). This is where the WebGPU framework work
actually starts.

**Rulings incorporated from the sitting (all Sid, in-session):**
- Birth laws **H6** (store keyed `(view-instance, address)` + fan-out index)
  and **N6** (f64 store coords, camera-relative f32 on GPU) — ruled YES.
- **Fork 2 ruled: ONE SUBSTRATE** — three projection families over one
  address space (`FORK-2.md`). This list is ordered under that ruling.
- **PROBE-10K measured** (parallel window, CLAUDE.md): Electric diffs viable
  at 10⁴ for change/append/tail-shrink (apply ≤4.2ms p95 @60fps);
  permutation-shaped diffs must NEVER carry the change signal.
- **Sid's three demands landed this sitting** (verbatim in `vision/LOG.md`
  2026-07-05 entry): walk-as-context for agents; world-and-lenses (layout
  projections are plural, workzone defaults); spec-of-specs (the spec schema
  itself forkable/inheritable — future, must not be precluded).

**Verdict vocabulary:**
- **BIRTH-LAW** — binds at the moment the scene store is first built;
  retrofit touches every consumer. Not code now; heads the store's contract.
- **FIX** — as-built shape is wrong against the north; correct at next touch
  of that code (fix-don't-defer applies within files already being edited).
- **EVOLVE** — as-built is the correct embryo; promote when the named
  trigger fires. Promote, wire, generalize — never replace.
- **LEAVE** — as-built is correct against the north; touching it is negative
  work. Recorded so "leave" is a verdict, not silence.

---

## The ordered list

Ordering principle: birth laws first (they gate everything), then the central
promotion and its immediate family, then items paced to the face-2 contract,
then trigger-paced evolutions, then the leave ledger.

### Tier 0 — birth laws (bind the scene-store contract, whenever it opens)

**Δ1 · BIRTH-LAW — store keying `(view-instance, address) → slot` + fan-out
index `address → #{appearances}`.** (H6; ruled.) Meaning lives at the
address; geometry/window-state lives at the appearance; picking returns the
pair (✕ closes an appearance, retraction acts on an address).
*Trigger:* first commit of the keyed scene store (Δ3).
*Falsifier:* a test renders ONE address in TWO view-instances with
independent geometry; one address-level write patches both slots via the
index; a pick on each copy returns (same address, different appearance).

**Δ2 · BIRTH-LAW — coordinates: store holds f64 world values (plain CLJS
numbers); GPU buffers only ever hold camera-relative (or anchor-relative)
f32.** (N6; ruled. As-built is f32-absolute end to end —
`renderer.cljs:7,41,1107` — safe only because zoom=1.)
*Trigger:* same as Δ1. Hit-testing adopts the same anchor convention in the
same change.
*Falsifier:* no buffer writer uploads absolute world coordinates (grep +
encode-path assertion); synthetic far-origin deep-zoom scene shows no glyph
jitter under pan.

### Tier 1 — the central promotion (the next render contract's core)

**Δ3 · EVOLVE — birth the keyed scene store.** Promote the per-mode cached
scene atoms (`!sidebar-scene` pattern), the buffer pools' three diff engines,
and the dormant `gpu-mount` incseq bridge into ONE client-side keyed store of
resolved drawables — the retained heart, the diff boundary, the hit-test
source, the token-face source (one substrate: this is the "one table").
Updated only at the reduce/consumer edge (CLAUDE.md law as architectural
wall). Diff grammar rule from PROBE-10K: **reorders travel as `:change` on
rank fields (order-is-data, MECHANICS §2), never as `:permutation` ops.**
*Trigger:* the face-2 (threaded/DAG timeline) render contract — the first
face that renders world + islands over live diffs. Not before.
*Falsifier:* in that face, draw, hit-test, diff-apply, and the token
projection all read the same store instance (no second tree anywhere);
`[RAF]` p95 within gate budget while diffs stream.

**Δ4 · EVOLVE — the overlay stratum (ephemeral state gets its own home).**
Hover, caret, drag-ghosts, shimmer, presence — instances keyed by address,
driven by view-state atoms, composed `base ⊕ overlay` at encode; world
stratum stays `identical?`-skippable. Shaped to later host H7's optimistic
write overlay (request-id-keyed pending entries, "proposed" channel) — shape
now, fill at D-008.5.
*Trigger:* same commit family as Δ3 (cheap at store birth; the as-built
counterexample is already on record — see Δ8).
*Falsifier:* with a pending tool-card shimmering, the world stratum performs
zero re-shapes across 60 idle frames.

**Δ5 · FIX — one tree serves render and hit-test, everywhere.** As-built:
chat/flow REBUILD a second tree at click time (`mouse.cljs:325,374`) — the
drift-bug class the retro caught; sidebar's cached scene is the correct form
(B2 gate-13 law).
*Trigger:* next touch of chat/flow/dg click paths; fully subsumed by Δ3 at
face 2.
*Falsifier:* click-target and drawn-target derive from the same structure in
the same frame (no `build-*-tree` call reachable from a mouse handler).

**Δ6 · FIX — actions become data.** `rect_tree` `:actions` closures → 
descriptor vocabulary `{:action <kw> :target <address>}` resolved against a
registered handler map (N2: specs never smuggle code; closes the one
non-serializable hole in scene data; descriptor IS a proto-ActionRequest).
*Trigger:* next touch of rect_tree consumers, at latest Δ3 (slots must be
serializable values).
*Falsifier:* a scene tree round-trips through EDN with actions intact and
replays identically; the token face can print every element's actions.

### Tier 2 — paced to the face-2 contract (demands with named collisions)

**Δ7 · FIX — the glyph pipeline finishes its landing.** Demand D4's known
collision (design glyphs ⊢ │ ├ └ • ✓ ✗ vs 95-glyph ASCII atlases) is
half-paid: merged Ubuntu+DejaVu MSDF atlas (340 cps) landed in B2, but Sid's
OI-2 ruling stands — MSDF is TEMPORARY verification, SLUG is the
destination. Remaining: slug glyph-set expansion (only DejaVu Sans Mono has
slug data today).
*Trigger:* before face 2 ships the design language (already a named
follow-up under OI-2).
*Falsifier:* the design-language glyph set renders in the slug backend with
correct advances; missing-glyph still renders tofu-with-advance (V3-5 law).

**Δ8 · FIX — ephemeral timers leave the content flow.** Shimmer/caret are
watched by the CONTENT flow: a pending tool-card forces full text re-shape
at 2Hz (retro O-2 — the exact failure MECHANICS §8 exists to prevent).
*Trigger:* Δ4's stratum existing; or next touch of the trail/chat render
path, whichever first.
*Falsifier:* same as Δ4.

**Δ9 · EVOLVE — layout projections are NAMED and PLURAL, and live in Rama.**
World layout = derived data in the semantic layer (deterministic,
log-projected, no wall-clock, sort by address); the trail lens
(x=time, y=lane) is the FIRST named projection, not the universal one —
Sid's world-and-lenses ruling: different workzones may carry different
default worlds, a view-spec's query names which projection it reads. Lane
assignment on insert is Rama's fold (H1's line: cross-entity structure is
Rama's job; H3: no world position may depend on another entity's rendered
size — the firewall law D6 already gives).
*Trigger:* the face-2 data contract (it consumes these rows).
*Falsifier:* two view-specs over the same material with different named
layout projections render both, no client-side layout recompute of the
world; re-running the projection from the log reproduces positions
byte-identically.

**Δ10 · EVOLVE — view params become the spec shape (wall W5, day one of any
face).** The `!effective-local-world` mode map + face params promote to a
spec-shaped EDN map `{query, projection-policy, camera, style-rules,
actions, lineage}` — carrying a **schema-version field from birth** and
migration hooks (Sid's spec-of-specs demand: the spec grammar itself must be
forkable/inheritable LATER — the version field + migrations are the
do-not-preclude, costing ~nothing now). Interest-query SHAPE
(bbox × band × kind) present in the spec from day one; execution stays
client-side until scale demands (NORTH §11.7). Specs-as-ASSERTIONS (N1)
waits for the write surface (D-008); spec-SHAPE does not wait.
*Trigger:* face-2 contract (W5 is already a standing wall; this makes it
concrete).
*Falsifier:* a face's full render state reproduces from (spec value + log
state) alone; changing a param is a data edit, no code touch; the spec
value round-trips EDN with schema-version intact.

**Δ11 · EVOLVE — the token face generalizes per-spec.** WP1's server-side
`render-bundle-text` → every face projects tokens from the same store/spec
(D15: citizenship right, never an export; W3: never a second pipeline).
Fold policy lives in the spec and MUST carry counts (ledger 16 at the token
layer). Shaped to later carry **walk-as-context** (Sid's demand this
sitting: "where has the user moved from" as agent context) — the face
already renders its address (D-008.4); recent-walk rows join when D-008.5
opens walk capture. Do-not-preclude now; no capture built.
*Trigger:* face-2 contract (each face's gate includes its token half).
*Falsifier:* pixel face and token face of one spec disagree on zero
addresses/marks (drift test); folded token output carries counts that sum
to the unfolded set.

### Tier 3 — trigger-paced evolutions (the trigger is the pace, not the plan)

**Δ12 · EVOLVE — registration replaces mode-`case` dispatch.** The 7-step
recipe's case branches (`editor_compute.cljs:457-464`,
`combined_text.cljs:253-255`, scroll/mouse zone additions) → the render-type
registry. NORTH §8 calls this "the one real refactor," incremental per mode.
*Trigger:* the NEXT new view/mode added — it enters via registry entry
instead of step 8 of the recipe; existing modes migrate opportunistically.
*Falsifier:* adding that view touches registry entries + spec data only —
zero edits in editor_compute/combined_text/scroll/mouse dispatch sites.

**Δ13 · EVOLVE — one clip representation.** Three parallel CPU clip
implementations (retro friction §5) → clip-stack index per instance,
resolved in-shader (world-space correct under any camera; retires the
line-pop text cosmetic); hardware scissor demotes to batch optimization.
*Trigger:* wiring the world camera (screen-space scissor breaks under a
zooming camera — MECHANICS §5 trap b), or the next clip-drift bug,
whichever first.
*Falsifier:* one code path computes clipping for render AND hit-test; text
clips at pixel grain (no whole-op drop at clip bands).

**Δ14 · EVOLVE — gesture routing moves from screen zones to hit-results.**
The order-sensitive zone cascade (`scroll.cljs`, `mouse.cljs`; trap 9
guarded) → the small FSM over hit-path + modifiers + thresholds (MECHANICS
§7), with pointer capture (drag latches the hit path at mousedown).
Thresholds in screen px by law.
*Trigger:* the camera goes live — zones do not survive a moving world
(MECHANICS finding 7). Until then the cascade is correct and LEAVE.
*Falsifier:* the same gesture set passes at zoom 0.5/1/2 with the world
panned — no gesture depends on a fixed screen region.

**Δ15 · EVOLVE — conditional RAF on the diff signal.** Unconditional RAF +
`identical?` skip stays LAW until the store (Δ3) consumes real diffs; then
the incseq stream is the change signal Gap 3 waited for, and the
already-built dirty-present machinery (`use-persistent-render-target?
false`) may switch on. PROBE-10K says the signal is viable
(change/append/tail-shrink); the two legitimate always-dirty sources are
animation interpolators and presence (MECHANICS §9).
*Trigger:* Δ3 live AND `[RAF]`/power evidence that idle cost matters.
Measurement-gated, never assumed.
*Falsifier:* with diffs quiet and no animation, zero encodes per second;
any diff arrival renders within one frame (no lost-wakeup — the O-3 RAF
death class stays dead).

**Δ16 · EVOLVE — text-run interning.** Full-rebuild-per-change
(`update-text-data` re-shapes everything) → shaped runs cached by
`(text, size, font)`; a changed line re-shapes one run (WebRender's
interning; MECHANICS §6 names it the cure).
*Trigger:* `[RAF]` objection on a text-heavy face (the instrument exists,
5ms threshold).
*Falsifier:* single-line edit in a 10³-line face re-shapes O(1) runs
(counter assertion), output pixels identical to full rebuild.

### Tier 4 — the leave ledger (verdicts, not omissions)

**Δ17 · LEAVE — the five pipelines + painter's order.** Rect/shadow/MSDF/
slug/clear-quad, AOT-enumerated, no depth buffer, alpha blending — correct
against the north (Impeller's lesson; alpha + depth = the classic sorting
problem, don't buy it early). New primitives only by registry event:
line/curve when Manhattan breaks (O-1 queued), image/sprite when a form
demands, per-instance z reserved for the 3D room kind.

**Δ18 · LEAVE — monospace advance (~30 sites of 0.56).** Mono is the v0/v1
design law; the moat is load-bearing (H9: kills shaping/kerning/measure).
Proportional/shaped text enters ONLY as a NEW render-type beside the mono
path, with H9's cost tag read aloud at that registry event — the most
expensive primitive the registry will ever admit.

**Δ19 · LEAVE — the Missionary frame loop.** 13-input `m/latest` world
snapshot, RAF sample, reduce with `identical?` skip, ~24 prev-keys — the
CLAUDE.md-lawful shape, untouched until Δ15's trigger. New discipline from
the parallel window binds all future derivation chains: co-varying values
derive in a SINGLE `m/latest` over the shared source (diamond glitch,
Claim 17).

**Δ20 · LEAVE — the Electric boundary.** Five Watch e/defns + server
mirror-atom workaround: boundary 1 is proven and PROBE-measured; the
workaround is an implementation detail UNDER the boundary, replaceable
without moving it. No ad-hoc JSON fetches beside it at scale (W3/wire trap).

**Δ21 · LEAVE — rect-tree layout as the island grammar's core.** Column/row,
padding shorthand, auto-height, clip, hit-path — this IS the island grammar
(N4: islands resolve local-2D, then project; never zoom-aware). Incremental
dirty-subtree layout waits for an instrument objection (H3: interiors are
bounded by law). Editor virtualization stays the 1D exemplar (Δ3 is its 2D
generalization).

**Δ22 · LEAVE — gpu_budget warn-only.** Enforcement = degrade-by-folding
(drop to coarser band, render aggregates — degradation and honesty are the
same move, MECHANICS §12), which needs Δ3 + fold data (D7). Until then the
meter stands.

---

## Demand coverage map (no silent drops)

| Demand (render-demands) | Where it lands |
|---|---|
| D1 addressed instances | Δ1/Δ3 (store), Δ6 (actions carry targets); address grammar = wall W1 (Track-A territory) |
| D2 one camera, lawful motion | camera wiring = face-2 contract work over Δ2/Δ3; flyTo/cuts = presentation interpolators (MECHANICS §9) |
| D3 semantic LOD as data | Δ10 (projection-policy in spec); altitude text forms = Rama contract data (W2/D3, Track A) |
| D4 cartographic text | Δ7 (glyphs), Δ18 (mono moat), greeking below legibility = band policy in Δ10 |
| D5 mark layer | renders as world-stratum instances over Δ3; proposed≠signed = honesty channels (face contract) |
| D6 open-in-place | design law already; enforced by Δ9's falsifier (no sibling-size dependency) |
| D7 folds and fog | fold counts = Rama aggregates (Δ9's family); fog = semantic-layer data, renderer never synthesizes (NORTH §11.8) |
| D8 routes and walks | walk playback = interpolators over Δ3 targets; capture gated D-008.5; token-side = Δ11 |
| D9 the rim | face-contract chrome (screen-space stratum exists as-built); address slot = D-008.4 law |
| D10 view-specs as data | Δ10 now (shape), N1 at write surface; compiler discipline (H2) binds the spec grammar design |
| D11 rooms | island frames as world entities at Δ3; editor-as-room = the existing editor + Δ2 anchor |
| D12 ambient life | Δ4 (overlay stratum; always-dirty budget per MECHANICS §9) |
| D13 honest channels | face contracts; substrate obligation = per-glyph rgba (exists) + theme tokens (exist) |
| D14 doors held open | 3D = registry events (Δ17); presence = rows + Δ4; nothing here precludes |
| D15 projection duality | Δ11; W3 stands (no render-ready wire rows — Δ20) |

## How this instrument is used

At each face gate (per the view-MVP contract's delta-instrument slot): read
this list against the face's diff — did the work walk toward or away from
the north? A FIX touched without its falsifier passing, a LEAVE item
touched at all, or a trigger fired without its Δ item entering the
contract — each is a gate finding. The list itself is amendable the way the
north is: by evidence, at a sitting, recorded here with date and grounds.

*Consumers: the next render contract (face 2); Track-B gate reviews; the
scene-store work package when Δ3's trigger fires. Companions: `NORTH.md`
(the ideal), `FORK-2.md` (the substrate ruling this list is ordered under),
`../render-substrate-retro/PRIMITIVES.md` (the as-built leg),
`../../design/claude/render-demands-2026-07-05.md` (the demand leg).*
