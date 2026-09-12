# Render-North INPUTS — the assembled manifest (Track D direction study)

References below to `BETS.md` are historical; that document is retired.
For vision orientation, use [carry-on](../../../docs/carry-on.md) as a reference summary
and [the vision log](../../../vision/LOG.md) as the primary source.

**Written 2026-07-05 (Fable, render-north direction session).** This file exists so
no future session re-gathers the inputs to the framework-north question. It is a
manifest with load-bearing gists — every gist cites its source; on any doubt the
source wins. NORTH.md (same directory) is the synthesis. Nothing here authorizes
building (D-001 untouched; direction study only).

**The question this study answered:** the ideal UI framework, built on WebGPU
(view layer) + Electric (reactive layer) over Rama (truth), for Softland as a
high-fidelity, extremely collaborative, expansive universe — one land at every
zoom, UIs made on the fly by humans and agents, eventually 3D, legible to agents
in minimal tokens.

**Two screening questions used against all prior art:**
- **K1** — can view-specs be DATA, written/edited at runtime — including by an
  agent mid-conversation — and interpreted live?
- **K2** — can the same spec project to BOTH pixels for humans and minimal-token
  text for agents?

---

## 1 · Vision inputs (the why and the scale)

- **`vision/LOG.md`** (Sid verbatim; read in full this session). Load-bearing
  entries for this study:
  - **2026-07-03 (HCI thesis):** versioning + collaboration + on-the-fly
    interfaces, natively versioned and collaborative, LLMs building with humans;
    the LLM is "boxed in the chatbox"; "given webgpu we can make any UI we want";
    Electric is the reactive layer that makes Bret-Victor-class frontends
    possible; Rama removes the backend-rebuild ceiling.
  - **2026-07-04 (the WebGPU-framework paragraph — this study's commission in
    embryo):** agents "CAN manipulate the views as needed because if there is
    option to customise views then user will"; "we do need some framework that we
    would have to develop for being the best on rendering the UI and being
    composable and minimal or atleast of a hierarchical or some sort of structure
    that is easy to reason about not like slop of html, then react on top."
  - **2026-07-04 (UI-evolution answer):** UIs are not regenerated per session —
    "we start with 'this might help' … we solidify and modify the ui based on the
    user's demand"; defaults may copy proven tools (Linear); "the core is the
    data, ui are a way to make that understood."
  - **2026-07-04 (face test, LAW-grade):** ZUI-based; zoomed-out view
    (title/decision/2-liner); open individual views while others stay closed;
    granular zoom in/out while keeping the sense of space and the trail.
  - **2026-07-04 (maximalist method):** design against thousands of threads;
    every affordance cascades (comments → inbox → reply cycle).
  - **2026-07-05 (handoff frame):** emperor designer → engineering head: see the
    whole final phase, judge the gunpowder, no refactoring/shit-patching.
- **`docs/current-mental-model/BETS.md`** — North (knowledge earth; every zoom a
  level of understanding; ecosystem of humans + agents; Rama ground truth,
  Electric reactive tissue, WebGPU the visible world, one day 3D) + ladder
  H1(active, trail view)→H5(gated, world scale). H3 re-sequenced: second user =
  agents/LLMs; solo-builder self-hosting gate before metascience.
- **`docs/vision/what-softland-is-claude.md`** — "place"; habitability criteria;
  **the Rendering Rhyme**: rendering IS making a vast reality inhabitable by a
  bounded observer; semantic zoom = LOD applied to knowledge; the viewport is
  the finite mind.
- **`docs/vision/what-softland-is-codex.md`** — "relation"; live epistemic
  couplings; compression without betrayal.
- **`docs/vision/epistemic-framework.md`**, **`terminology-glossary.md`** —
  background; known-knowns grammar is already in BETS North.

## 2 · Sid's own framework prior art (founder instinct, 2026-02/03)

- **`docs/architecture/gpu-component-library.md`** (2026-02-27): adopts Zed
  GPUI's **5-primitive model** (Quad, Shadow, Glyph, Sprite, Underline) —
  finite shader work, infinite components by pure composition of rect-tree
  nodes + design tokens. Component catalog + token system sketched.
- **`docs/architecture/component-library-jit.md`** (2026-03-01): components as
  `.cljc` files with a **data-only `schema`** + `render` + `demo`; JIT-converted
  by a **scoped agent conversation** on first use ("no one lives on Mars but the
  land is there"); live code/chat/demo panes; token maps per source library.
  → Founder instinct already: few primitives, specs-as-data, agent-in-the-loop
  authoring, live render feedback.

## 3 · Design-track demand side (Track C → D handoff)

- **`design/claude/render-demands-2026-07-05.md`** — Part I: the 15-demand
  catalog D1–D15 (addressed instances; one camera, lawful motion with labeled
  cuts; semantic LOD as data; cartographic text density; mark layer;
  open-in-place; folds+fog; routes/walks; the rim; **D10 view-specs-as-data**;
  rooms; ambient life; honest channels; doors held open; **D15 projection
  duality**) + the §0 **type-hypothesis**: camera-over-addressed-world, not a
  widget tree (STANCE for Track D to confirm or kill — verdict in NORTH.md §1).
  Part II: five machines (log / semantic layer / two projectors / camera-world
  renderer / assertion loop), five walls W1–W5 (address grammar; event schemas;
  one semantic layer never render-ready; spatial camera model in the first
  pixel face; view parameters as data), gunpowder audit (Electric = the one leg
  needing a verdict: land-scale deltas), shit-patching tripwires.
- **The three drivable artifacts** (same directory; demand evidence, not
  implementations): `write-gesture-sketch-2026-07-04.html` (gesture beats,
  1k-thread scale stage, walk, terrain descent, 27-item do-not-preclude ledger);
  `softland-horizon-2026-07-05.html` (full-scale dream; painting + manifesto);
  `softland-dummy-2026-07-05.html` (the real engine seed: ~450-line canvas2d
  walkable sim).
- **Session sweep — the extracted demand list** (Opus artifact read,
  2026-07-05; full 20-item list preserved here as the artifacts' operable
  evidence):
  1. Continuous cursor-anchored zoom across ~2 decades of scale (z 0.14→5),
     point-under-cursor invariant [dummy].
  2. Semantic zoom = representation swap via alpha cross-dissolve through a
     shared mid-band, not just scaling [dummy; sketch alt0/1/2].
  3. Text appears progressively as a function of camera altitude, thresholds
     distinct per element type [dummy; ledger-13 "altitude text forms are data"].
  4. Thousands of dim entities + tens of bright marks simultaneously (~2,530
     dots, ~2% kraft highlights, every frame) [dummy; horizon].
  5. Address is a pure function of viewport — every zoom state emits a
     resolvable `view://…`; a screenshot is a pointer [dummy `zoneName()`].
  6. Deterministic layout from the log: x=time, y=lane; never force-directed
     [sketch LAW + ledger-15].
  7. Open-in-place: detail costs local space only; siblings hold [sketch;
     founder face test].
  8. Fold-chips carry mark/dead/question/new counts at every altitude; no fold
     hides marks below [sketch dotfields; ledger-16].
  9. Emptiness renders as fog — first-class state, never blank [all three].
  10. Camera fly-to with geometric (perceptually-linear) z-interpolation
      [dummy].
  11. Contradiction renders side-by-side, badged, unmerged [all three].
  12. Assertion marks visually reserved (kraft) and structurally distinct from
      terrain at pixel AND token level [ledger-10].
  13. Retraction scars, never erases [sketch beat 7; dummy].
  14. "What's new" is a place-ordered, abandonable walk from a cursor — never
      an arrival-ordered inbox [sketch STOPS; dummy walk + Esc].
  15. Reading attests (staleness farmed by walking); writing needs an explicit
      sign gesture [sketch; dummy].
  16. Provenance edges travel across regions and re-hang downstream
      (refutation arrives as terrain; dependents tremble) [horizon; dummy].
  17. Agents are first-class walkers — warmth/dot presence, ranked+capped
      proposals at their place, badged machine; never a global queue [all
      three].
  18. Geography grows from use — roads pave themselves after enough walks
      [dummy roadT; horizon].
  19. One address space from knowledge-earth → claim → source → git bytes,
      CUT-labeled between regimes [sketch 6-frame descent].
  20. Deterministic, stable procedural scatter (seeded) — same land redraws
      identically across frames/resizes/themes [all three].
  What the artifacts CANNOT show (implied demands): multiplayer presence,
  persistence/event-sourcing behind the render, agent proposal lifecycle, the
  live write surface, true LOD at scale (2,530 dots is toy), view lineage /
  the forge, economy. The dummy CUTs to modal walk-cards rather than descending
  continuously into a claim's interior — continuous regime-descent is still
  unproven anywhere.
- Context (thought-space, not law): `design/claude/decision-log.md`, `taste.md`,
  `synthesis.md`, `giants-digest.md`.

## 4 · As-built ground truth (the code, verified this session)

- **Binding as-built docs:** `build/render-substrate-retro/PRIMITIVES.md`
  (mechanical inventory) + `RETRO.md` (no-rebuild verdict; V3-*/T-* fix list;
  friction ledger §5); `build/view-mvp/CONTRACT.md` (WP-B2, BINDING,
  countersigned — pure-cljc face core, verbatim View-3 law, 7-step mode wiring,
  "no framework extraction" refusal, delta-instrument slot at face gates);
  `build/view-mvp/CHARSET_AUDIT.md` (95-glyph ASCII baseline; regen mandated).
  **Working-tree note (2026-07-05):** B2 implementation is in flight — merged
  Ubuntu+DejaVu atlas landed, glyph fallback (U+FFFD, always-advance) landed,
  `rect_tree.cljs → .cljc` promotion done, `trail_face/` + `ingest_watchers.clj`
  exist. The charset-audit picture is partially repaired as-built.
- **CLAUDE.md laws (hard-won, any framework north must metabolize):** m/latest
  not m/ap+m/?< for combining; eduction+deref for event filtering; NO side
  effects in m/latest; unconditional RAF + `identical?` skip until Electric
  diffs exist as the change signal (Gap 3); try/catch not inside e/defn; the
  char-advance constant discipline.
- **Session sweep — GPU substrate map** (Opus direct read of `renderer.cljs`,
  `runtime/render.cljs`, `rect_tree.cljc`, `buffer_pool.cljs`; condensed):
  - 5 pipelines, all 6-vertex instanced quads, one render pass: SDF rich rect
    (28f: per-corner radii, per-side borders, 2-stop gradient), Gaussian shadow
    (20f, erf), MSDF glyph (12f), Slug glyph (24f, analytic Bézier coverage),
    dormant clear-quad. ABSENT: line/bezier, image/sprite, rotation, per-instance
    z, depth/stencil, MSAA, compute. Z-order = hardcoded draw-call sequence in
    `draw-frame!`; chrome sub-elements addressed by literal firstInstance slots.
  - **Camera:** every vertex shader applies `ndc = ((world*zoom+pan)/screen)*2-1`
    — a world-space camera exists in ALL shaders; zoom passed as literal 1.0,
    pan-y = -scroll-y. Slug shader is already zoom-aware (dilation 0.5/zoom).
    The substrate is a zoomable world canvas driven as a scrolling pane.
  - **Scene:** per-frame flat instance vectors derived from Missionary flows
    (world-snapshot map, 13-input m/latest); `rect_tree.cljc` is a real
    data-driven flexbox-ish scene DSL (rt-node: bounds/style/text/children/
    clip?/layout/actions) used by sidebar/trail-face/settings/cmd — BUT
    `:actions` values are **closures**, the one thing making scene trees
    non-serializable.
  - **Text:** monospace advance only (`fsize × char-width`); per-run size,
    per-glyph rgba; no kerning/shaping/bidi; full instance-buffer rebuild per
    change; U+FFFD fallback with always-advance (V3-5 landed).
  - **Buffers:** slot pools with generation handles + three diff engines
    (keyed-by-id, ordered, positional batch) doing O(changed) writeBuffer —
    a working retained-update seam. `gpu-mount` bridge to Electric incseq
    exists in `buffer_pool.cljs` but the live path uses the diff fns.
  - **Hit-testing:** CPU linear DFS over rt-trees + editor column math; no
    GPU picking, no spatial index.
  - **Verdict line:** a hand-rolled single-pass renderer specialized for
    scrolled text panels + SDF chrome; `rect_tree` is the visible seed of a
    general data-first scene engine, not the engine itself; distance to
    "arbitrary agent-authored views" = new-primitive cost (hand-written WGSL +
    plumbing) and hardcoded element kinds/z-order.
- **Session sweep — Electric/runtime map** (Opus direct read; condensed):
  - Electric's footprint is **boot-only + five server→client subscriptions**
    (`e/watch` on server mirror atoms → client reset!). After boot Electric does
    no per-frame work. Missionary is the runtime: one `m/join` of consumers;
    ~60 atoms; single sample point `(m/sample vector <world-snapshot >raf)` with
    the identical?-skip reduce.
  - Writes: fire-and-forget HTTP POSTs; each server handler appends to the Rama
    depot AND updates the mirror atom (the atom is a latency shim, not truth).
  - **View definition today:** `!effective-local-world` — a derived DATA map
    with `:mode` — but consumed via `case` branches scattered across ~6 sites
    (mouse, scroll, editor_compute, combined_text, render args). No plugin
    seam; a new view is an invasive edit (the 7-step recipe), not a
    registration.
  - Reusable substrate: render loop, rect-tree + hit-test, event flows +
    focus-deref routing, buffer pools, Rama bridge. Editor-specific: layout
    builders, tokenization, per-mode dispatch.

## 5 · What binds (constraints on any north)

- `decisions.md` D-001 (arbiter: build only on form-break), D-002 (first form =
  trail view; View 3 agent-legible), D-003 (two-regime code split), D-004
  (typed RelationEdge, asserted-by first-class), D-005 (view-first sequencing),
  D-006 (Fable window allocation), D-007 (bet foundry), D-008 (read-only MVP;
  write surface = CLI; faces render their own address).
- Substrate invariants: center loop (Projection → ActionRequest → Rama →
  ActionDecision → KernelEvent → materialized state → projection); back-arrow
  rule (agents/workers stream INTO Rama; Rama is truth; UI reads Rama).
- **Fork 2 (one-substrate-or-three) is Sid's founder fork — OPEN.** This study
  gives a recommendation in NORTH.md, never a resolution.
- Direction studies are free; anything touching the runtime stays D-001-gated.

## 6 · Prior-art research sweeps (6 Opus web researchers, 2026-07-05; verified against primary sources, not training data)

Full reports lived in the session; the load-bearing verdicts and steals are
preserved below. Every claim was source-cited in-session; items marked
(inference) were flagged by the researcher as reasoning beyond sources.

### 6.1 Linebender (vello / xilem / masonry)
- vello: compute-centric GPU 2D; **the scene is literally a flat multi-stream
  data buffer** (tag/path/draw/transform streams, GPU offsets via prefix sums);
  transforms NOT baked into geometry (re-instance without re-encode); retained
  scene fragments stitched per frame (vision doc); incremental present planned,
  unshipped; alpha maturity; WebGPU now default in all major browsers.
- xilem: view tree (ephemeral, typed Rust) → widget tree (retained, Masonry) →
  view-state tree; memoization + pointer-equality pruning + id-path event
  dispatch. **K1: FAIL** — views are compile-time Rust code, no interpreter/data
  story. **K2: partial precedent** — Masonry maintains an AccessKit semantic
  tree alongside vello pixels (one tree, two projections), but as a11y
  byproduct, not a co-equal projection.
- Steals: scene-as-flat-data-streams; transform-late; retained fragments;
  three-tree split with full-diff reconciliation; AccessKit dual-tree promoted
  to first-class; renderer tiering behind one scene format (GPU/hybrid/CPU —
  headless render path for agents).

### 6.2 Makepad · Flutter · rfw · immediate mode
- Makepad: `live_design!` DSL — appearance + **shaders (MPSL) as live-editable
  text**, hot-applied <10ms to a running app; AI-drivable by design. K1
  strong-partial (runtime-editable text, but Rust-ecosystem DSL; new behavior =
  recompile). K2 absent.
- Flutter three trees: immutable widget config / persistent element (identity +
  dirty list, keyed O(N)-per-child-list reconciliation, GlobalKey tree surgery)
  / render objects (constraints down, sizes up, ≤2 visits). Impeller lesson:
  **AOT-enumerate the finite shader set; never compile pipelines mid-frame**.
  K1: widgets are code — FAIL alone, BUT:
- **rfw (Remote Flutter Widgets, Flutter team): a real K1 existence proof** —
  runtime-interpreted widget DATA format (`.rfwtxt` text ↔ `.rfw` binary of the
  same library), with data binding, loops, switch, local state, and
  **events-as-declarative-emissions interpreted by the host**; deliberately NO
  logic in the format; capability boundary = the compiled local-widget
  vocabulary. Its ceiling (no custom painters/animations) marks exactly where
  the escape hatch belongs: the compiled primitive vocabulary.
- egui/ImGui: perf is fine (lazy repaint; comparable power draw), but per-frame
  identity fights persistent provenance-carrying nodes; a11y needed retained
  IDs anyway; Unity retired IMGUI for app UI. Wrong top model for a land; right
  for debug overlays.
- Steals: rfw's split (spec=data, fixed primitive vocabulary, host interprets
  events — matches the back-arrow rule); shaders-in-the-spec live editing;
  element-layer-as-diff-target; stable keys/tree surgery; draw-command IR
  between spec and GPU (the IR also serializes → K2 lever); AOT pipeline
  enumeration; id+typemap for transient UI state.

### 6.3 ECS / game-engine world-UI splits (bevy, Unity, Unreal, interest management)
- **Bevy BSN (scene-as-data):** validation, not adoptable tech. The shipped path
  (0.19, 2026-06) is a compile-time Rust macro; the `.bsn` file loader,
  hot-reload, and reactivity — the K1-satisfying half — are deferred/post-MVP.
  Softland's EDN + Electric live-interpretation already delivers what BSN is a
  multi-year effort to approximate. Vocabulary worth keeping: **scenes as
  patches over inherited scenes** (versioned/branchable view-specs by layering),
  required-components/template expansion (terse spec → full render aspects),
  **reflection-as-serialization** (one registry yields both the live editor and
  the wire format).
- **ECS fit, split verdict:** flat component storage + parallel passes is RIGHT
  for the *scene* layer (10⁴ knowledge nodes: position/glyph/tint/LOD-band/
  pick-tag as parallel arrays; cull/LOD/pick as passes). It is WRONG for the
  *panel/text* layer — hierarchy traversal and text flow are the documented
  ECS-UI failure points (Leafwing; Bevy delegates layout to Taffy and text to
  cosmic-text as an **opaque measured content-block** — the industry-proven
  seam). Sparse UI events: reactive (Electric/Missionary) is strictly better
  than system polling.
- **World/UI split lesson:** even world-space UI in Unity/Unreal lays panels
  out in local 2D and then projects the result into the world — **layout in
  local space, transform after; never make flexbox zoom-aware.** A thin
  screen-space layer survives for controls (rim/palette); world-space for all
  content. Unity UI Toolkit's UXML(structure)+USS(style) split = a shipping
  AAA validation of declarative-data UI.
- **Interest management (Rama→client at scale):** named patterns — AOI grid
  subscription; **query-based interest (QBI, SpatialOS): the viewport is a
  first-class query (bbox × zoom-band × type) with per-query fidelity** —
  zoom band = subscription fidelity tier, one knob for render-LOD and network
  interest; server-authoritative + optimistic local prediction (Rama already
  is the authoritative half). Do NOT import the simulation tick — Softland's
  truth is a log, semantic time a DAG.

### 6.4 Map/geo engines (maplibre style spec, deck.gl, tiling, 3D Tiles)
- **The MapLibre/Mapbox GL style spec is the strongest industrial K1 existence
  proof:** an entire zoomable visualization as one JSON value — sources +
  ordered layers (fixed ~10-primitive enum) + a **deliberately non-Turing-
  complete expression language** (JSON s-exprs, 11 op domains: get/match/case/
  interpolate/step/let/zoom/feature-state…) evaluated per feature and per
  camera. Zoom-dependent styling is pure data:
  `["interpolate",["linear"],["zoom"], 5,1, 10,5]`. Runtime editing is native:
  `setPaintProperty`/`addLayer` instant; `setStyle(new,{diff:true})` applies a
  minimal delta to the live scene.
- **Camera-vs-data expression separation** (recompute-on-zoom vs recompute-on-
  data) tells the reactive graph exactly what invalidates what — maps directly
  onto Missionary flow splitting and deck.gl-style `updateTriggers`.
- **deck.gl/json + pydeck:** an external program (Python — or an agent)
  authors live views as JSON over a registered class catalog; `@@=` micro-
  expressions for accessors; anything richer must be a *named, pre-registered*
  function — the JSON selects capabilities, it never defines new ones.
- **Label machinery (transferable recipe for "text appears as you descend"):**
  SDF glyph atlases + viewport **grid collision index** + priority sort keys +
  per-symbol `fade_opacity` (fade, never pop) + **cross-tile stable identity**
  so a label never jumps or duplicates across zoom seams (Softland analog:
  stable object ids as the cross-LOD key — identity survives strata).
- **Tiling as semantic zoom:** per-entity `minzoom` baked at build time
  (tippecanoe model) = "zoom 10 domains / 15 claims / 20 full text" as a data
  pipeline; overzoom = principled coarse rendering for the frontier; Cesium 3D
  Tiles' screen-space-error refinement (REPLACE vs ADD) generalizes to
  *semantic* coarseness and covers the 3D future. Caveat: pyramids assume
  stable precomputed coordinates — Softland layouts change, so tiles must be
  lazy/server-side from Rama, invalidated on re-layout.
- **Disqualifiers:** no box model / nested layout at all; interaction lives in
  imperative callbacks, not data; expression language cannot construct
  geometry or lay anything out; and the deepest mismatch — **maps render
  state; Softland renders a projection of a log.**

### 6.5 ZUI lineage · Figma · tldraw · server-driven UI · MCP Apps
- **ZUI lessons (Pad++/Piccolo/Raskin), hard-won:** semantic zoom + portals are
  the two load-bearing inventions; **manual spatial organization does not
  scale** (Bederson's own retrospective); **desert fog** (Jul & Furnas — zoom
  into cue-empty space and you are lost; fix = critical zones: always render
  "content lies this way"); continuous zoom needs damping (motion sickness is
  real); zoom empirically did NOT beat lists for general tasks — ZUIs won only
  in **bounded spatial domains** (Figma, whiteboards). The still-unsolved cost
  is **semantic-zoom authoring** — deciding what each thing IS at each scale.
  That authoring slot is exactly where Softland's altitude-text-as-data (D3)
  already sits.
- **tldraw:** every shape = a JSON record in a reactive store with a typed
  schema AND **per-record up/down migrations** (old snapshots keep opening);
  sync = git-like push/pull/rebase, server-authoritative, diffs of records.
  K1 split verdict: instances are data (agent-editable live); NEW render types
  are code (ShapeUtil). K2 weak (verbose JSON).
- **Figma (verified from their eng blog):** custom retained-mode tile-based
  GPU engine (C++→WASM, WebGPU backend since ~2023) — "a browser inside a
  browser." Multiplayer is **NOT a CRDT**: server-authoritative,
  **last-writer-wins per (object-id, property)**, client-generated IDs,
  reparenting as atomic parent+fractional-index property, server rejects
  cycles, optimistic clients discard conflicting server changes for unacked
  edits. Maps ~1:1 onto Rama (depot order = authority; PState = the property
  map). **The flag that matters:** LWW *erases* disagreement — acceptable for
  spatial/layout state, the inverse of Softland's law for epistemic state
  (disagreement preserved until synthesis). Steal the model for layout only.
- **SDUI anatomy (Airbnb Ghost / Lyft Canvas / Adaptive Cards):** component-
  type **registry** (curated enum) + typed JSON tree + layout separate from
  content + **actions-as-data routed to pre-registered handlers** + explicit
  versioning. Deliberately NOT Turing-complete; new behavior = client release;
  the registry is the capability boundary. An action in a spec is literally an
  ActionRequest — this maps straight onto the Softland center loop.
- **LLM-authors-the-spec precedents:** **MCP Apps (SEP-1865, official MCP
  extension 2025-11)** — servers register `ui://` templates, agent tools
  reference them, host renders live inline; proves the *interaction pattern*
  Softland needs, but the material is HTML-in-sandboxed-iframe: opaque,
  single-player, K2-hostile, blocks a unified zoomable scene. Vercel streamUI
  ships React code (not data). Adaptive Cards increasingly used as LLM output.
- **Family synthesis:** nobody has the union (spec-as-data + agent-authored
  live + zoom + multiplayer + text projection). Nearest spirit = tldraw's
  records-with-migrations + SDUI's registry/actions + Figma's LWW-for-layout
  over Rama. The union is Softland-original.

### 6.6 Reactive→GPU (Electric diffs, WebRender, rerun blueprints, incremental computation, AccessKit-as-K2)
- **Electric 3's diff protocol, verified:** Missionary `incseq` — a sequence
  diff is a map of six ops `:grow :degree :shrink :permutation :change
  :freeze`; `e/diff-by` stabilizes collections by key (React-key semantics);
  over the wire only diffs travel ("no collections sent, only diffs";
  `e/for-by` can diff on the server). It is *self-adjusting sequences*, NOT
  Naiad differential dataflow — no free incremental joins; don't over-claim.
  Non-DOM consumers are architecturally siblings of `electric-dom` (interpret
  the six ops at a retained consumer) but publicly undocumented — Softland
  would be writing the first serious `electric-gpu`.
- **WebRender = the structural reference:** display list as serialized DATA
  crossing a process boundary; **Scene (retained, over-scanned) vs Frame
  (per-vsync viewport cull + GPU prep) split** — one Scene, many Frames, which
  is what makes scroll/zoom cheap; interning: primitives registered by ID so
  damage = fast ID-list diff; picture caching with per-tile invalidation +
  quad-tree subdivision of hot tiles; scene building off the render thread.
- **Rerun Blueprint = the closest existing K1:** "blueprints are just data" —
  views/containers as archetypes in an isolated store with **their own
  timeline**, edited by appending events, serialized as `.rbl`, constructed
  programmatically, streamed over the same channel as data; the viewer is a
  pure per-frame projection of `blueprint@revision + data`. Overrides-as-data:
  default view + per-entity patches; agents patch views, never truth.
- **Incremental-computation survivors:** memoized DAG + cutoff pushed to
  per-instance granularity via diff keys; diff-proportional work (incr_map;
  Bonsai incrementalizes the state transition too); **salsa durability tiers**
  (tag inputs LOW=cursor/hover vs HIGH=committed truth; skip revalidating
  stable subgraphs); the composability cost is real — the diff-interpreting
  scene store will be the hard core.
- **AccessKit-as-K2, assessed:** tree of `{stable-id, role, attrs}` nodes,
  **push model — full tree once, then keyed deltas**; agents already consume
  exactly this (browser agents read the AXTree); verified caveat — naive
  1-render-node→1-tree-node yields 80–99%-of-tokens bloat (Mind2Web ~51k
  tokens); UIFormer-style merge/filter compression (48–88%) is required, with
  a completeness check so merging never drops semantic boundaries.
- **The sweep's synthesis (adopted, amended in NORTH.md):** put the diff
  boundary at a **client-side retained scene store** — Electric carries
  semantic diffs server→client (boundary 1, already solved); the scene store
  consumes diffs at the reduce/RAF consumer edge (never inside m/latest — the
  CLAUDE.md law); GPU encoder maps ops to buffer ops (`:grow`=append,
  `:shrink`=free-slot, `:change`=writeBuffer@offset, `:permutation`=
  indirection buffer); the SAME store feeds the AccessKit-style tree
  serializer — **stable IDs are the shared currency of both projections; the
  incseq stream is exactly the change signal CLAUDE.md's Gap 3 says
  conditional RAF has been waiting for.** Traps verified/flagged: permutation
  degrades to full re-upload without an index buffer; slot lifecycle on
  shrink (freed-but-uncleared slot masks truth); ID churn collapses both
  projections to rebuilds; blueprint timelines need GC/compaction.

---

*All nine sweeps (3 local readers, 6 web researchers) completed and are
condensed above. Session: 2026-07-05, Fable orchestrating, Opus 4.8 subagents.
NORTH.md (same directory) is the synthesis over exactly this manifest.*
