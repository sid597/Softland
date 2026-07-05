# Render North — MECHANICS: the browser we have to rebuild, problem by problem

**Status: DIRECTION (thinking layer), non-binding on build.** Written
2026-07-05, same session as `NORTH.md`, at Sid's explicit ask: *"how do we
render a box within a box within a box with a close button… how will we know
in webgpu what to close? how will we render this box is on top of this? if
there are millions of points do we get all of them at once? — think in this
direction, deep, broad."*

The frame for every section: **a WebGPU canvas is a dumb framebuffer.** No
elements, no events, no "on top of," no memory of what it drew. The browser
silently provides ~a dozen services over its DOM; a canvas framework must
rebuild each one — but keyed by addresses, driven by data, and at scales the
DOM refuses. This document walks the full catalog. Each section:
**PROBLEM** (concrete, the way Sid posed his three) → **MECHANISM** (the data
structure / algorithm) → **TODAY** (what the as-built code does) → **NORTH**
(how the framework answers) → **TRAPS**.

The one diagram everything below hangs on:

```
                    ┌──────────────  SCENE STORE  ──────────────┐
   fill (queries,   │  address → {geometry, order, clip, style, │   read fwd = draw
   diffs, partial)  │             band, pick-shape, action}     │   read rev = hit-test
                    └────────────────────────────────────────────┘
   The thing drawn, the thing clicked, and the thing fetched are
   provably the same structure. Every section is a walk over it.
```

**CORRECTION (2026-07-05, same day — see `HARD-PROBLEMS.md` H6):** the key
above is wrong as drawn. Transclusion (the same claim visible on the ground
AND in an open room AND in an overlay) means one address has MANY
appearances: the store keys by `(view-instance, address) → slot` with a
fan-out index `address → #{appearances}`. Picking returns the pair — the
appearance (for local UI: which panel's ✕) and the address (for actions on
the thing itself). Everything else in this document survives the correction.

---

## 1 · Hit-testing: "which ✕ did I click?"

**PROBLEM.** Box in box in box, each with a close button. Pixels can't tell
you what they are. Click (412, 300) — close WHICH panel?

**MECHANISM.** Two indexes, one address space:
- **Islands (panels, cards, editor):** walk the scene tree front-to-back
  (reverse draw order). Transform pointer screen→world first
  (`world = (screen − pan) / zoom` — 3 ops), then screen-local per island.
  The hit result is a *containment path*
  `[room-7 › panel-A › panel-B › panel-C › close-C]`; deepest node wins;
  its `{:action :close :target addr-C}` is data. "What to close" is answered
  by construction: the button carries its owner's address because containment
  is in the data, not the pixels. Unhandled events bubble up the path.
- **World dots (10³–10⁵ points):** a tree walk per mousemove is wasteful. Use
  a **uniform grid / quadtree spatial hash** over world coords — O(1) bucket
  lookup per pointer event. Key insight: **this is the same GridIndex the
  label-collision system needs** (MapLibre precedent, INPUTS §6.4) — build it
  once per band change, serve both consumers (pick + label collision).
- **Optional third index at extreme density:** a GPU pick buffer (render
  instance-ids into an offscreen target, read 1px under cursor). WebGPU
  readback is async (`mapAsync`) → result arrives next frame. Acceptable for
  hover and even click (16ms is under perception), but it's a *fallback*, not
  the default: the CPU indexes keep picking synchronous and testable on JVM.

**TODAY.** `rect_tree/hit-test` returns exactly the root→leaf path;
`dispatch-event` bubbles; the editor has column-math picking. Correct — but
chat/flow rebuilt a *second* tree at click time (the drift bug the retro
caught; sidebar's cached `!sidebar-scene` is the correct form, now law via
B2 gate 13).

**NORTH.** One rule: **hit-testing reads the same scene store the frame was
encoded from, in reverse order.** Pick results are `(address, action-
descriptor)` pairs — which means agents can "click" too: the token face can
expose the same action descriptors per row, so a click and an agent's action
request are the same operation arriving by different faces.

**TRAPS.** (a) Two trees (render vs hit) will always eventually disagree —
the as-built bug class; (b) pick-buffer-only designs go dark on JVM tests and
add a frame of latency everywhere; (c) forgetting pointer *capture* — a drag
that starts on a handle must keep routing to that handle even when the
pointer exits it (DOM gives `setPointerCapture`; ours is: drag FSM latches
the hit path at mousedown).

## 2 · Stacking: "this box is on top of this"

**PROBLEM.** WebGPU has no z-index. Two floating panels overlap — who wins?
And the inner box must paint over its parent.

**MECHANISM.** Painter's algorithm: submission order IS stacking. Three
strata, each with its own ordering rule, concatenated:

```
draw order =  world content   (band order, then y/rank)
           <  islands         (sibling rank: fractional index; tree order inside)
           <  chrome / rim    (fixed, never zooms)
```

- Nesting is free: parent first, children after → inner paints on top.
- Overlapping siblings: an explicit **fractional-index rank** per island
  (Figma's trick — "bring to front" = one property write, no renumbering;
  and rank-as-single-property is also the collaboration-safe form).
- **The invariant:** hit-test walks the exact reverse of draw order. One
  ordering, two consumers — the thing you see on top is provably the thing
  your click hits. Two orderings = clicks falling "through" windows.

**TODAY.** Z-order is the hardcoded draw-call sequence in `draw-frame!` +
tree order within pools + literal firstInstance slots for chrome. Correct,
frozen.

**NORTH.** Order is data in the scene store `(stratum, band, rank, tree-pos)`;
the frame encoder sorts (stable sort, mostly-sorted input → cheap). Depth
buffer + per-instance z stays a *registry event* reserved for the 3D room
kind — painter's order carries 2D at our instance counts, and alpha-blended
quads want painter's order anyway (transparency + depth buffer = the classic
sorting problem; don't buy it early).

**TRAPS.** (a) Letting "draw order" and "hit order" be computed by two
different code paths; (b) per-frame full sorts at 10⁵ instances (sort once
per scene change, not per frame — order lives in the store, the frame only
culls); (c) opaque-overlap overdraw is NOT worth optimizing until measured —
quads are cheap, occlusion culling is furniture.

## 3 · Scale: "millions of points — do we get all of them at once?"

**PROBLEM.** The land at earth zoom holds millions of rows. Memory? Wire?
Frame budget?

**MECHANISM.** Never all at once — three containers, three magnitudes, and
**zoom changes WHAT you ask for, not just how much**:

```
Rama          10⁶⁺ rows      truth (this is what Rama is for)
   ↓  viewport-as-query: bbox × zoom-band × kind
Scene store   10³–10⁴ items  visible set + overscan ring
   ↓  per-frame cull
GPU           what's drawn   instances actually encoded this frame
```

At earth altitude the query returns a few hundred **aggregates** (region dots
carrying counts `{claims 48210, marks 312, ? 14}`) — folding is law; raw
millions are never rendered and fold-chips carry the counts so no altitude
hides that marks exist below. Descending narrows the bbox and deepens the
band. Pan/zoom *within* resident data = frame-only rebuild (no fetch);
crossing a band/region = re-query, diffs update the store.

**Residency policy (the mipmap-of-meaning):** coarse pyramid levels are tiny —
**keep level-0 (the earth aggregates) resident always**, like a mipmap's top.
Then a fast flick from editor-zoom to earth never waits on the network: you
always have *something honest* to draw at every altitude (coarse first,
detail streams in — the overzoom pattern: rendering coarse IS honest for the
frontier). Eviction: LRU beyond the overscan ring, coarse levels exempt.

**TODAY.** The editor already proves the pattern in 1D: only visible lines
tokenized/laid out. Everything else builds-all-then-culls (fine at current
corpus, measured by the `[RAF]` instrument per B2 gate 15).

**NORTH.** The interest query (bbox × band × kind) is *in the view-spec* from
day one (shape only; server-side execution activates at H5 scale per the
standing open question). Prefetch direction from pan velocity is a later
furniture item maps have proven.

**TRAPS.** (a) Fetch-on-band-cross without keeping the old band → blank flash
mid-zoom; see §11 (cross-dissolve requires BOTH bands resident briefly);
(b) evicting a slot without clearing its GPU instance (the `:shrink` lifecycle
trap — a freed-but-uncleared slot masks truth); (c) treating "millions" as a
render problem — it's a query-shape problem; the renderer should never see
more than ~10⁴.

## 4 · Precision: the trap none of the three questions named (NEW, wall-grade)

**PROBLEM.** One land at every zoom means world coordinates spanning ~7+
decimal orders (glyph at zoom-100 … earth extent). **f32 has a 24-bit
mantissa (~7 significant digits).** The as-built pipeline is f32 end to end —
verified this session: `pan: vec2<f32>`, `camera-floats = Float32Array(6)`,
instance positions f32, shader math `world_pos * zoom + camera.pan` in f32
(`renderer.cljs:7,41,1107`). Consequence at scale: an entity far from origin,
viewed close-up, quantizes — glyphs jitter and swim as you pan (the classic
large-world vertex-jitter failure; every game engine and Cesium hit it).
Today it's invisible because zoom=1 and the world is one document tall. The
knowledge-earth will hit it.

**MECHANISM (industry-standard, cheap if adopted at scene-store birth):**
- **CPU keeps f64** — free for us: every JS number is already f64. The scene
  store's world coordinates are plain CLJS numbers. Nothing to build.
- **GPU sees camera-relative f32:** each frame (or each camera-cell crossing),
  the encoder uploads `position − camera-anchor` — small numbers near the
  viewer, full f32 precision where the eye is. This is Cesium's RTC /
  "origin rebasing" — a subtraction in the encoder, not a renderer rewrite.
- Pairs perfectly with tiling: tile-local coordinates + tile anchor = the
  same trick statically. (WGSL has no f64 — there is no "just use doubles on
  GPU" escape; relative encoding is THE answer.)

**TODAY.** f32 throughout; safe at zoom=1; unwired zoom means no one has
seen the jitter yet.

**NORTH — proposed law N6:** *the scene store's coordinates are f64 world
values; GPU buffers only ever contain camera-relative (or anchor-relative)
f32.* Adopt when the scene store is born; retrofitting later means touching
every buffer writer.

**TRAPS.** (a) Doing the subtraction in the shader (uniform anchor) helps but
the instance data itself must also be small — subtract at encode; (b) mixing
absolute and relative instances in one buffer; (c) hit-testing must use the
same anchor convention or picks drift at deep zoom exactly like pixels do.

## 5 · Clipping: boxes that cut their children

**PROBLEM.** A scrolling panel inside a panel inside a room — content must
cut at each boundary, corners are rounded, and the whole stack sits under a
world camera.

**MECHANISM.** Three tools, cheapest first:
- **CPU cull at flatten:** fully-outside subtrees never emit (exists).
- **Per-instance clip rect in the shader:** every instance carries (or
  indexes) its island's clip rect; fragment discards/alphas outside. The rich-
  rect shader already does per-corner SDF math — rounded clip is the same
  math against the clip rect. Works under any zoom because it's world-space
  data, not a screen-space scissor.
- **Hardware scissor:** axis-aligned, screen-space, one per draw batch — good
  for whole-island batches, useless for rotated/3D later. Use as an
  *optimization* per island batch, never as the correctness mechanism.

**TODAY.** Three parallel CPU clip implementations with subtly different
behavior (retro friction §5) + T-4 clamp landed for bg rects; text ops drop
per-op at clip bands (line-pop granularity, taste-flagged).

**NORTH.** ONE clip representation in the scene store (a clip-stack index per
instance: islands push clips, children reference them), applied per-instance
in-shader; scissor as batch optimization. Pixel-exact text clipping falls out
(fragments clip, not ops), retiring the line-pop cosmetic.

**TRAPS.** (a) N clip implementations drifting (the as-built state); (b)
screen-space scissor under a zooming camera (must recompute per frame, breaks
under rotation/3D); (c) clip stacks deeper than the shader's budget — cap
island nesting honestly (depth ~8 covers any sane UI; the spec validator
enforces).

## 6 · Text: the material the land is made of

**PROBLEM.** Cursor placement, selection, editing, wrapping — on a canvas,
under a camera, at cartographic density (10³ live text elements, 10⁴–10⁵
glyph instances).

**MECHANISM.**
- **Resolution independence is already won:** MSDF/slug atlases are the
  correct early bet — one atlas serves every zoom; no rasterized-size
  pyramid. Below legibility (~8px effective) text should not render at all —
  the altitude band swaps glyphs→greeking/dots *by policy* (cartography
  answer), which also caps instance counts at earth views.
- **Editing:** a hidden offscreen `<textarea>` owns IME/composition/OS text
  services (the standard canvas-editor trick — Figma, Monaco, Google Docs);
  the canvas renders caret/selection as rects (exists today). Click→char =
  column math (monospace, exists). Selection is *addressed spans*, so it
  survives zoom, restyle, even re-layout — it's data, not pixels.
- **Density:** shaped runs cached by `(text, size, font)` key — WebRender's
  text-run interning; a changed line re-shapes one run, not the pane. (The
  as-built full-rebuild-per-change is the known cost center; interning is the
  cure when `[RAF]` demands it.)

**TODAY.** Mono advance only (~30 sites of 0.56), per-run size, per-glyph
rgba, U+FFFD fallback landed, full buffer rebuild per change.

**NORTH.** Mono stays the v0/v1 law (the design language is mono-first);
proportional/shaping enters — if cartographic labels ever demand it — as a
NEW text render-type beside the mono path (registry event), never a mutation
of it. Text layout stays an **opaque measured block** to the island layout
engine (the Taffy/cosmic-text seam — industry-unanimous).

**TRAPS.** (a) Letting anything but the text subsystem measure text; (b)
shaping on the render thread at scale (intern first, then worker if still
hot); (c) IME without the hidden-input trick = broken composition for CJK —
not optional if "others can visit."

## 7 · Input → intent: the gesture state machine

**PROBLEM.** The same mousedown might begin: click (select), double (open),
drag (pan the world? move a panel? marquee-select? scroll an island?), and
wheel might zoom the world or scroll a panel. Who decides?

**MECHANISM.** One small FSM, decided by **hit result kind + modifiers +
thresholds**, not by screen zones:
```
mousedown → latch hit-path (pointer capture)
  move < 4px & up quickly        → click  → deepest action descriptor
  move ≥ 4px on empty ground     → pan camera
  move ≥ 4px on island titlebar  → move island (rank/position = data write)
  move ≥ 4px with shift on ground→ marquee → selection set (addressed)
wheel over scrollable island     → island-local scroll (clamped, exists)
wheel elsewhere (or ctrl always) → zoom-to-cursor (dummy's exp model)
Esc                              → abandon current mode (walks: never nags)
```
Keyboard routes by focus (one focused island; the eduction+deref pattern —
as-built and law). Every *resolved* intent is `(address, descriptor)` — the
same currency as §1, which is what makes gestures replayable/loggable later
(walks are data; the select·type·sign write gesture rides this when D-008.5
opens the write surface).

**TODAY.** A zone *cascade* (order-sensitive cond over screen regions — trap
9 in the B2 contract guarded its fragility). Works, doesn't scale to a
zooming world where zones move.

**NORTH.** Hit-result-driven routing replaces geometric zones — the FSM reads
the scene store like everything else. Wheel disambiguation is the one
genuinely contested UX spot (zoom vs scroll); the dummy's answer (wheel=zoom,
islands claim scroll on hover) is the starting default, Sid tunes at first
light.

**TRAPS.** (a) Zone order-sensitivity (as-built, known); (b) forgetting
capture → drags drop when the pointer outruns the panel; (c) thresholds in
screen px, not world units (a 4px drag is a 4px drag at every zoom).

## 8 · Ephemeral state: hover, focus, pressed, drag-ghosts

**PROBLEM.** Hover highlights, focus rings, a drag ghost — state that changes
60×/sec and must NEVER touch the log (back-arrow rule), yet composes with
data-driven rendering.

**MECHANISM.** A thin **overlay stratum** of instances keyed by address,
driven by view-state atoms only. The salsa durability lesson applies: inputs
are tiered — LOW (hover/caret/drag: churny, local, never logged) vs HIGH
(committed truth: stable, diff-driven). The frame encoder composes
`base instance ⊕ overlay patch` per address; the world's identical?-skip
never fires for hover because hover lives in its own small always-cheap
layer.

**TODAY.** The shimmer/caret timers are watched by the CONTENT flow — a
pending tool-card forces full text re-shape at 2Hz (retro O-2: the exact
failure this section exists to prevent). The caret already has the right
shape (a dedicated chrome slot).

**NORTH.** Ambient life (kestrel, warmth pulses, shimmer) = the overlay
stratum + a small always-dirty buffer; the world stratum stays skippable.
Selection is the one ephemeral that graduates: the sign gesture reads it as
input to an assertion (ephemeral → log only through an explicit gesture).

**TRAPS.** (a) Any ephemeral watched by a world-content flow (as-built O-2);
(b) hover state stored per-node inside scene data (then hover invalidates the
scene — keep it in the overlay keyed by address); (c) rendering a mark from
client state that never hit the log — the shadow-log tripwire, already law.

## 9 · Animation: two clocks, never confused

**PROBLEM.** Camera flyTo, label fades, road-paving growth, cross-dissolves —
continuous motion in a system whose truth is a discrete log.

**MECHANISM.** Separate **history time** (the log; semantic; a DAG) from
**presentation time** (RAF; interpolators). Animations are presentation-layer
interpolators over scene-store targets: flyTo = geometric z-interp
(`z0·(z1/z0)^ease(t)` — the dummy's perceptually-linear law); reveals =
per-instance `fade_opacity` attributes (the MapLibre no-popping recipe);
choreography (walks/routes) = *data* (an ordered stop list in the log) whose
*playback* is presentation. An interpolator marks only its instances dirty —
frame cost stays O(animating), not O(scene).

**TODAY.** Blink/shimmer timers exist (wrong stratum, §8); no camera
animation (no camera); flyTo proven in the dummy at trivial cost.

**NORTH.** The interpolator set is small and enumerable (camera, opacity,
growth-t, dash-phase) — attributes, not arbitrary code, so agent-authored
specs can *request* animation (`{:reveal :fade}`) without owning a timeline.
CLAUDE.md's discipline holds: interpolators tick at the consumer edge,
unconditional RAF until diffs land, then animation becomes one of the two
legitimate "always dirty" sources (the other is presence).

**TRAPS.** (a) Animation state in specs/log (then history replays contain
easing junk); (b) per-frame scene mutation for motion (mutate the overlay/
attribute, not the scene); (c) unbounded concurrent interpolators — cap and
coalesce (a thousand fades = one clock, per-instance phase).

## 10 · The wire: what actually crosses, when

**PROBLEM.** Boot, steady state, band-cross, edit — what bytes move?

**MECHANISM.**
- **Boot:** active view-specs + the resident coarse pyramid (small) + the
  current band's rows. Specs are cacheable values (MCP-Apps' template/data
  split: ship the spec once, stream data).
- **Steady:** `incseq` diffs only (verified protocol) into the scene-store
  consumer; watcher epoch bumps trigger re-pulls (B2's pattern, kept).
- **Band-cross / region-cross:** new query; old band retained until new
  arrives (§11); diffs replace wholesale via `:grow/:shrink` batches.
- **Writes:** action descriptors → ActionRequests (D-008: not yet; the
  read-only MVP's HTTP+depot path is the placeholder).
**TODAY.** Five push subscriptions + pull e/defns + fire-and-forget HTTP
writes — boundary 1 exists and is proven at small scale.
**NORTH.** Same boundary, three payload kinds (specs, rows, diffs), one
pre-registered scale question (NORTH §9's 10⁴-incseq measurement).
**TRAPS.** (a) Render-ready rows on the wire (W3 breach — the token face dies
quietly); (b) re-sending collections where diffs suffice (Electric already
solves — don't bypass it with ad-hoc JSON fetches at scale); (c) spec and
data arriving through different consistency paths → a spec referencing rows
that haven't landed (render holes honestly; never block the frame).

## 11 · Semantic cross-dissolve: the zoom transition's hidden requirement

**PROBLEM.** The dummy's core visual law: earth-scatter and claim-ground
cross-FADE through a shared mid-zoom band (`earthA`/`groundA` overlap). That
means during the transition **both representations render simultaneously.**

**MECHANISM.** Band residency must overlap by one band: entering band k's
transition zone prefetches band k±1; the frame encoder renders both strata
with policy-driven alphas until the dissolve completes. This is a *memory
policy consequence of a design law* — discovered by walking the artifact
mechanics, invisible from the architecture alone. Costs are bounded: two
bands ≈ 2× the visible set for the transition's duration.

**TRAPS.** (a) Evict-on-band-cross → blank flash mid-dissolve (the exact
moment the eye is watching); (b) dissolving between *incompatible layouts*
(band k and k+1 must agree on anchor positions for shared entities — stable
addresses again: the same entity's dot and title must sit at the same world
point or the dissolve smears).

## 12 · Degradation: when things go wrong mid-frame

**PROBLEM.** GPU device lost (tab backgrounded, driver reset), atlas missing
a glyph, spec invalid, budget exceeded.

**MECHANISM & NORTH.** The retained scene store buys the big one for free:
**the GPU is a cache of the scene store** — on device-lost, recreate pipelines
and re-encode from the store; nothing of record lives only in GPU memory.
Missing glyph → visible tofu + correct advance (landed, V3-5). Invalid spec /
unknown construct → honest hole + notice (the marker-table law generalized).
Budget exceeded → **degrade by folding** (drop to coarser band, render
aggregates — degradation and honesty are the same move here; `gpu_budget`
already meters, today warn-only).

**TRAPS.** (a) Any state of record living only in a GPU buffer; (b) silent
degradation (a capped layer rendering as if complete — omissions are pixels,
already law); (c) crash-on-bad-spec (an agent typo must never take down the
land — total language + validator + holes).

## 13 · Determinism: the screenshot must be a pointer

**PROBLEM.** "Same land redraws identically across frames/resizes/themes"
(artifact demand 20) and D-008's screenshot loop require reproducibility.

**MECHANISM.** Determinism lives at the **data level, not the pixel level**:
layout derived from the log (seeded LCG for scatter — the dummy already does
this; no wall-clock, no Math.random in projections), addresses + geometry
identical for identical log states. AA fragments may differ across GPUs —
irrelevant, because the pointer is the ADDRESS in the rim, not the pixels.
(Same reasoning that makes the address the root noun.)

**TRAPS.** (a) Layout reading anything but the log (viewport-dependent layout
is fine ONLY for the frame, never for stored positions); (b) iteration order
leaking into layout (sort by address, not by map order); (c) time-of-render
in derived positions (the two-clock discipline, applied to geometry).

---

## The findings this pass adds (beyond NORTH.md)

1. **N6 (proposed law, wall-grade): f64 in the scene store, camera-relative
   f32 on the GPU.** The as-built f32-absolute path is verified and will
   jitter at land scale; the fix is a subtraction at encode-time IF adopted
   at scene-store birth, a full buffer-writer retrofit if not. This is the
   one item in this document with real cost-of-late.
2. **The collision grid and the pick grid are one structure** — build the
   spatial index once per band, serve labels and picking both.
3. **Cross-dissolve requires adjacent-band co-residency** — a memory policy
   forced by a design law; and it silently requires shared entities to keep
   the same world anchor across bands (addresses again).
4. **Coarse-pyramid always-resident (mipmap-of-meaning)** — the earth level
   is small; keeping it warm makes any zoom-out honest at 0ms network cost.
5. **The GPU is a cache of the scene store** — device-lost recovery, JVM
   testability, and headless/agent rendering all fall out of the same
   property.
6. **Ephemeral state gets its own stratum** — the as-built shimmer-forces-
   full-reshape (O-2) is the counterexample that proves the rule.
7. **Gesture routing must move from screen zones to hit-results** when the
   camera goes live — zones don't survive a moving world.

## What this changes upstream

- NORTH.md §12 gains N6 (recorded here as PROPOSED; folding it into NORTH's
  law list is a one-line edit at the next touch).
- The Electric-scale evidence item (NORTH §9) should measure with camera-
  relative encoding in place, or the measurement itself will jitter at the
  synthetic scale.
- Nothing here contradicts the B2 contract; §5/§6/§8 name as-built costs
  (clip triplication, shimmer coupling, zone cascade) that are already in
  the retro's friction ledger — this document only explains *why* they are
  the right shape to pay down later and what the paid-down form is.
