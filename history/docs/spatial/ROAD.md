# spatial — ROAD (direction-grade, NOT binding)

**Status:** direction analysis, 2026-07-11 Fable session. Input to the future
`container-transforms`, `islands`, `point-and-say`, `scene-diff` contracts —
the way `framework/ROAD.md` fed CONTRACT v1. Nothing here is countersigned;
decisions.md wins on any conflict. Sid's verbatim asks: `vision/LOG.md`
2026-07-11 entries (per-object control · 3D readiness · Box3D · point-and-say ·
"liveable and controllable from inside" criterion).

Cluster board line: next-prompt.md thread 12. Probes: thread 11 (`box3d-spike`),
thread 13 (`islands-probe`) — their findings amend this doc.

---

## 1. Verified substrate facts (all at file:line, 2026-07-11)

- **Whole-frame zoom exists in every shader and is never driven.** All four
  pipelines apply `(world_pos * camera.zoom) + camera.pan` (Camera struct:
  `renderer.cljs:7,131,234,283`); slug even has zoom-aware SDF dilation
  (`renderer.cljs:320`). But `draw-frame!` hardcodes zoom `1.0`
  (`renderer.cljs:1559`) and pan is only `(0, -scroll-y)` (`render.cljs:451`).
  No zoom gesture handlers exist anywhere in the client. Continuous zoom was
  pre-paid at the substrate (that's also why MSDF/slug fonts were chosen) and
  never wired to a hand.
- **Screen-fixed chrome is counter-baked, and it taxes every scroll.** Cmd
  panel/status bar stay put by adding `scroll-y` into their world coords
  (`combined_text.cljs` chrome ops) so the camera pan cancels. Consequence:
  `chrome-same?` requires `(= scroll-y prev-scroll-y)` (`render.cljs:273`) →
  chrome text re-layouts + re-uploads on EVERY scroll tick. Single-global-
  camera workaround wearing a permanent cost.
- **No per-container transform exists.** Everything reaches the GPU in
  absolute world pixels; faces flatten scene trees to absolutes CPU-side
  (`offset-text-ops`/`offset-rects` = CPU coordinate baking). Moving/zooming
  one container = CPU re-layout + full instance-buffer rewrite.
- **Text upload is monolithic**: `update-text-data` (`renderer.cljs:1348`)
  reshapes ALL visible glyphs (editor+sidebar+faces+chat) into a fresh
  Float32Array on any text change; text changes always force full-viewport
  redraw (dirty-rect nil, `render.cljs:374`). This is the 64-editor wall.
- **2D by construction, deliberately (D-001):** Camera struct is 2D (pan
  vec2 + zoom scalar, NDC by screen-dimension division — no matrices); NO
  depth buffer in the pass (painter's order); 2D rect-tree hit-testing;
  dirty-rect/scissor assumes static 2D viewport. Nothing about WebGPU itself
  precludes 3D; our layer never needed it.
- **A split-screen comparison draw path already exists**
  (`draw-frame-comparison`-style, `renderer.cljs:1507` — primary/comparison
  systems at half width; built for font comparison). Precedent for side-by-
  side faces (scene-diff).

## 2. container-transforms — the keystone package

**Design:** transform hierarchy `screen ← world-camera ← container-transform
← local coords`. Instances carry a container index; a storage buffer holds
per-container transforms (offset + scale; rotation later); vertex shaders do
two multiplies instead of one. Chrome becomes a container on a SCREEN-layer
camera (two cameras, world/screen — kills the counter-bake tax). Instance
data becomes LOCAL coords — this is the bulk of the refactor: every op
producer (combined_text, faces, sidebar, dg_flow) bakes absolutes today.
Hit-testing gains a transform node (inverse-transform the point per
container). Per-window/per-source text geos extend the Phase-6 pool pattern
(rects/shadows already solved this; text hasn't).

**One refactor pays four debts:** per-container pan/zoom control (Sid's ask);
the chrome-scroll tax; the 64-editor wall (a keystroke stops reshaping other
windows); smooth whole-frame zoom (drive the dormant uniform). It is also the
seam physics writes into (§10) and the thing islands composite through later.

**Transforms-as-data law (proposed):** where a container sits/scales is
assembly data (D-011 arrangement — agents may lawfully propose arrangements
via A2). In-flight gestures stay client-side at 60Hz (renderer law:
presentation-transient); the SETTLED position commits to Rama as an event.
Same settle→commit pattern as physics rest-states.

**Evidence state:** chrome tax + dormant zoom are code facts; the
per-container form-break arrives from Sid WEARING Boxes/Minimap (dev app
left running at W2 close). Contract after his wearing notes. Est. 5–8
sessions; the local-coords refactor is the risk center.

## 3. The islands ladder (3D)

"3D island" = a container whose content is rendered by a separate 3D pipeline
(own perspective camera + depth buffer) into a texture, composited into the
2D land as a quad. Four rungs of citizenship:

- **Rung 0 — the window (NEVER build):** foreign 3D content (wasm demo,
  three.js-style) composited blind. State invisible to Rama: no truth, no
  provenance, no lineage, no agent legibility. A TV set in the land. Named
  only as the anti-pattern.
- **Rung 1 — the rendered island:** our minimal 3D pipeline; scene DESCRIBED
  as data (EDN like a face assembly: nodes, transforms, materials, camera).
  Renderer is Regime-1 code; the scene is arrangement (D-011 verbatim).
- **Rung 2 — the live island:** a simulation as transform writer — Box3D
  (wasm, client-side, RAF-stepped) writes body transforms into island
  instance buffers exactly as a pan gesture writes a container transform.
  Settle-states commit as events. Deterministic stepping ⇒ a run is
  REPLAYABLE from its seed ⇒ a sim is a trail.
- **Rung 3 — the citizen:** scene nodes are ADDRESSES of land objects (a
  claim-block as a card in space; relation edges as visible joints). Not a
  build step — it's what happens automatically once rung 1 exists and nodes
  are addresses; grows face-by-face by desire path.

**The citizenship law: the island is a face, not a window.** Scene tree =
land data; contents = addresses; camera = wearable state; interactions =
ActionRequests. Then provenance/history/legibility/D-012 all apply with ZERO
new governance. Sid's criterion "liveable and controllable from inside" is
satisfied at rung 3 by construction — inside the island and inside the land
are the same place seen through a different camera. Liveability comes from
addresses, not rendering technique.

## 4. What islands GET

- **Performance isolation:** island frame rate decouples from land frame
  rate (heavy sim at 30fps inside, land at 60); a sleeping island (unchanged
  scene) costs ~zero GPU/frame; texture = natural LOD boundary (smaller when
  zoomed out, dropped when offscreen). ← islands-probe falsifies these
  claims (P5).
- **Multi-camera plurality:** two islands, same scene data, different
  angles — W2's plurality direction extended into space.
- **Cross-projection identity:** same address as 3D node and 2D card
  simultaneously; click-through between them. Renderer law paying out —
  identity survives strata that now differ in dimension.
- **The diving bell:** zoom island quad to fullscreen → hand input to its
  camera → scoped 3D-native immersion → step back out. Immersion as a place
  you visit, not a climate. No rearchitecture ever forced.
- **Provenance as solidity:** known/proposed/derived/guessed rendered as
  solid/wireframe/ghost/translucent — the map-must-not-lie gains a channel
  in 3D.
- **Camera-as-assertion:** a saved viewpoint = "look from here" with
  provenance — pointing made first-class; agents can propose camera paths
  via A2.

## 5. What islands LOSE (honest seams)

- No cross-island occlusion/lighting/shadows — each flattens before
  composite; object crossing islands = evented handoff (lawful, historied,
  visually a seam/pop).
- Text-at-angle legibility trap contained inside the frame, not removed.
- Texture memory scales with island count × resolution (pool/virtualize;
  gpu_budget exists).
- Input seam: 2D point → per-island ray cast; cross-boundary drags need
  explicit design.
- We own one more renderer (small, forever; idioms transfer from the
  existing one).
- 3D-NATIVE UI (whole land in perspective) stays behind form-break with
  pre-registered skepticism: ray-cast hit-testing everywhere, z-sorted
  transparency, dirty-rects die, 3D document layout is a mostly-lost design
  problem. The *feeling* of depth is largely purchasable in 2.5D (layers,
  parallax, scale, shadows) inside the transform hierarchy.

## 6. Eyes-of-Softland mapping

History-is-terrain: scene mutations are events; sim runs are trails; replay
is a log projection. One-land-every-zoom: island participates via its quad;
zoom stays compositional (arguably MORE faithful than one global 3D camera).
Map-must-not-lie: solidity channel + the boundary itself is honest ("this is
a projection with its own camera"). Desire paths: rungs climb on form-breaks.
Visitable: 3D faces ship as data + addresses, not pixels. Buildable-from-
inside (D-012): a design conversation → proposed 3D assembly → worn beside
the chat — the self-hosting formulation extends unchanged; new primitives
enter the git code lane like face primitives (D-011 boundary holds verbatim).

## 7. Controllability surface

Island as 2D citizen (container transform: drag/zoom/layer/physics-pushable) ·
island camera (orbit/dolly/fov = wearable data, animatable, agent-proposable) ·
per-node control (each scene node = address + transform; moves are
ActionRequests → evented, provenance-carrying) · time (pause/scrub/replay as
log projections) · fidelity (per-island resolution + rate as data).

## 8. point-and-say (the deictic loop) — DIMENSION-AGNOSTIC

Loop: navigate (camera pose = data) → utter via cmd/chat (exists) → CONTEXT
BUNDLE assembles from the viewpoint: `{island/face address, assembly address,
camera pose, picked point (ray/cursor hit), visible node ADDRESSES (frustum
or viewport query) ranked by screen-space size (= free attention model)}` —
small EDN → LLM gets DUAL-CHANNEL context (rendered texture as image +
address bundle; addresses are queryable back into Rama — View-3 legibility,
strictly better than screenshots) → reply = proposed assembly delta
(`based-on` edge to the birthing conversation) via A2: validated,
error-carded, rendered as GHOST/proposed material until accepted → accept =
event. This is D-012's loop gaining a spatial pointer — deixis ("here",
"this") is what pixel-based chat-with-tool interfaces cannot resolve;
addresses resolve it.

**Key sequencing insight (2026-07-11):** the loop works over the live 2D
Boxes face TODAY — point at a block, "make xyz here". Rehearse in 2D first;
3D inherits the proven pattern. Est. 2–3 sessions (2D form).

## 9. scene-diff / two-from-one-base

Branching is already the ontology: assemblies are EDN values in an
event-sourced log; `based-on`/`built-over`/`new-direction`/`dead-end` (D-004,
shipped) ARE branch semantics; semantic time is a DAG. The delta is the
PRIMARY object (authored first, appended as event); the "new build" is a
projection (base value + delta, persistent-structure sharing — fork costs
only the delta). **Runtime napkin:** mechanical fork+materialize+stream+
re-render ≈ <100ms total (extrapolated from the 7.5ms committed path + face
pull; unmeasured over real scene payloads — write-echo prices the identical
path); LLM authorship is the only slow step (seconds). Structural EDN diff
beats line diff (no formatting noise). Wearable diff: two islands/faces from
one base camera; split-screen precedent at `renderer.cljs:1507`. MERGE is the
named gap (late-bound consensus, core-reframes) — deferred per D-001 while
single-author. Est. 1–2 sessions for fork affordance + diff face (2D now).

## 10. Box3D (Erin Catto, v0.1.0, portable C17 — VERIFIED 2026-07-11 via
## box2d.org/documentation3d/: it IS the 3D sibling, not "version 3 docs")

Role: rung-2 transform writer. Client-side wasm stepping in RAF; transforms →
per-container/island buffers; settle → event. Determinism outranks speed
(deterministic ⇒ replayable trail; nondeterministic ⇒ just motion) — the
spike (thread 11) tests build + determinism + JS transform-read seam; a clean
FAILURE report is a success (gates honestly). v0.1.0 churn ⇒ vendor pinned,
don't track. For pan/zoom FEEL alone no engine is needed (critically damped
spring = 5 lines); an engine earns its keep at contact/stacking/joints.
Sid's "convert box3d to our system" carries an UNANSWERED fork: the engine,
the DOCS as explorable material (ingest + live island demos — candidate
first form for rung 3), or both.

## 11. Sequencing + napkins (calibrated to relation-kernel/block-kernel
## measured cycle times; ±50%)

NOW (probes, parallel-safe): `islands-probe` (rung-1 pipeline truth, thread
13) ∥ `box3d-spike` (thread 11) ∥ Sid wears Boxes/Minimap (transform
evidence, no session). THEN: `container-transforms` CONTRACT (5–8 sessions,
after wearing notes) → `islands` CONTRACT (4–6 sessions; needs probe
findings + Sid's Box3D reading + first-form pick) → rung 2 (3–5 sessions,
needs both spikes + seam) → rung-3 first citizen face (1–2 sessions).
Anytime: `point-and-say` 2D (2–3), `scene-diff` 2D (1–2). End-to-end to
"stand in a 3D map and say 'make xyz here'": ≈11–17 sessions, ~3–4 weeks at
current cadence.

## 12. Open questions (Sid)

1. Box3D reading: engine / docs-as-material / both. 2. Islands first form
(Box3D-docs conversion is the poetic candidate: docs as land material whose
islands run the engine they document). 3. `G-perf` standing gate clause
(one-paragraph work-package template amendment) — adopt? 4. Post-wearing:
does container-transforms cut next, or does an editor-cluster package go
first? (Contention is Sid-attention, not files.)
