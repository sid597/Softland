# The Engine at the Waist

*below the waist · the render engine · 2026-08-25 · a map with positions — nothing on this page is a ruling.*
*Twins: `engine-waist-map.html` (the rendered page), `engine-waist-map.pdf`. This file is the contestable one.*

Nouns above, roads below. What in the parked render engine has to exist as code, what should be worn as facets in ECS land — and the old-world instance sitting on each class that still holds.

> **You click a card.** Today: `selection/transition` (a `case` on `:toggle`) → `chrome_derive/apply-selection` → five form-nodes → `chrome_material/material-quads` (a `case` on `:selection-outline`) → `chrome_gpu` vertices → tape entries carrying `:pick {:chrome-form …}`. Six files know the words *selection-outline* and *blue*.
>
> **Above the waist:** the pointer system puts `:selected` on the card entity; a recipe — data — says *selected things wear a 1 px blue outline and four 10 px corner marks*; the floor draws any `:mark` it is handed. No file knows the word selection. The blue is yours to change in-land, and a caret, a hover halo, a focus ring are the same recipe with different numbers.

## The picture

*The drawn version is in the `.html` / `.pdf` twins — one SVG at one grain. This is its shape in text.*

```
ABOVE THE WAIST — the old page's instances · every box goes · reborn above as recipes (data) + systems (code over facets)
 ┌ THE TREE ────────────────┐ ┌ MARKS VOCABULARY ────────┐ ┌ ARROWS ──────────────────┐
 │ rect_tree · scene_store  │ │ chrome_material forms    │ │ connector_material       │
 │ scene_runtime · selection│ │ chrome_derive · snap     │ │ connector_route          │
 │ goes · your ruling       │ │ chrome_runtime   goes    │ │ connector_gpu  goes·pos. │
 └──────────────────────────┘ └──────────────────────────┘ └──────────────────────────┘
 ┌ THE SMALL BLENDER ───────┐ ┌ DEMOS·ORPHANS·AMBIENT ───┐ ┌ EDITOR LEFTOVERS ────────┐
 │ region3d_runtime/pointer │ │ frame_runtime · fixtures │ │ inside renderer.cljs:    │
 │ orbit · gizmo · menus    │ │ maintain-* · gpu-mount   │ │ bracket-rects · hit-test │
 │ gizmo+grid shaders ·pick │ │ providers · globals      │ │ snapper · editor-state   │
 │ goes · either way        │ │ goes                     │ │ goes                     │
 └──────────────────────────┘ └──────────────────────────┘ └──────────────────────────┘
━━━━ THE WAIST ━━ ◆ render packet: road id + resource key ✓ ━━ ◆ :paint/source live handle ✗ ━━━━━━━━━━━━━━
              ━━ ◆ family key → facet key (build stage) ━━ ◆ placement's slot input ✗ · chrome_gpu form input ✗
 ① recipes + facets → packets ↓                                            ② pick walks the tape backward ↑
BELOW THE WAIST — the render floor · what has to exist as code · never editor code
 [THE KEY — today: ten families · wanted: facets, several per entity · the fence stays, the key changes — build stage]
 SHAPERS — said → geometry (pure .cljc, JVM)
  TEXT (floor)        STROKE/SHAPE (floor)   IMAGE·TRANSFORM (floor)  3D KERNEL (open — yours)   EFFECTS·ORDER (floor)
  text_shaper         path_material          image_material           region3d_scene             scene_tape · frame_effects
  text_layout         path_tessellation      containers               region3d_placement ✗ slot  frame_plan_view · delta
  planes · fonts                                                      region_rungs               semantic_state · graph=oracle
 PIPELINES — geometry → pixels (nine, as-is)
  GLYPHS msdf·slug | CONTOUR path_gpu | BOX·SHADOW·IMAGE (renderer) | 3D PASSES region3d_gpu+placement_gpu | MARKS chrome_gpu ✗ forms | COMPOSITOR+CLIP → pixels
 CUSTODY — the machine that owns GPU memory and order
  CONDUCTOR renderer.cljs | POOL·BUDGET·LEASES buffer_pool · gpu_budget · region_bindings | DECLARATIONS frame_inputs (✗ ledger) | STATICS sorted: stay / go
 THE GUARD — verifier.cljs oracles + goldens stay · fixture atoms of a cut family go · W4 receipt rehomed
```

## The story

1. The engine is 46 files, about 1.48 MB, and it is **dark**: the only thing that boots it is the verifier's own shadow-cljs module (`shadow-cljs.edn:15-16`); `draw-frame!` has no caller in `src/` (`renderer.cljs:3678`). Every call on this page is on *shape*, not liveness — your criterion.
2. Your test for the waist (08-19): *"something that has to exist as code vs what can and should be built on top, ecs or whatever style."* Smallest version: *"text in placement out."*
3. One refinement both reviews forced, and it matches your three strata (LOG 07-30: material · Softland code · host floor): **"has to exist as code" is not the same as "below the waist."** ECS systems are code too — a route solver, a layout rule, selection → marks can all be code *above* the waist. The floor is narrower: what is reusable without knowing Softland's meanings or gestures, takes and gives stable geometric values, or owns GPU resources.
4. The picture above is the engine at one grain. Below the line, three rows: **shapers** (said → geometry — pure `.cljc`, runs on the JVM, has tests), **pipelines** (geometry → pixels — `.cljs`, stateful), **custody** (the machine that owns GPU memory and order). Above the line: the old page's instances — every box marked *goes*.
5. Read the shapers row: it exists today, family by family — text (your yardstick), stroke, image + transform, the 3D kernel, effects + order. Not all of it is floor (see 3); the table below sorts it.
6. Read the pipelines row: **nine, as-is** — glyphs (msdf, slug), contour, box / shadow / image, the 3D passes, marks (the code says chrome), the compositor with clip. They do not change in anything below.
7. Here is the old world: the engine is **keyed by family** — ten nouns, a closed menu, the same ten words at every layer. A family fuses *a kind of thing* with *a pipeline* and *a grammar* (pick modalities, edit operations, hit tolerance, zoom regimes). An entry belongs to exactly one.
8. The fence that throws at load (`renderer.cljs:3364`) is *not* the class system — it is a completeness law any registry needs (every facet an entity may wear must have a road). What closes the world is the **key**. Keep the fence, change the key.
9. The receipt that the key is wrong: a labelled arrow is a connector family that **clones a text system** (`connector_gpu.cljs:185-226`), because one entry can be one family only. In your ECS words an entity wears several facets at once — `:text` and `:contour` and `:mark` — and each road takes its own.
10. The engine half-knows this: the tape's `paint-forward` "contains no family branch" (`scene_tape.cljc:749`); the renderer's per-family path is a data table, not a `cond` (`renderer.cljs:3297`). What is left of the class system is the key, the grammar per family, and the frame description bucketed by family.
11. The instances sitting on it: the box tree (you ruled: goes), the five mark forms + snapping + marquee + selection, the elbow-arrow conventions, the small Blender inside the 3D window, the demos — and, cross-cutting, ambient providers set by mutation, browser globals, and kind menus guarded by refusing validators.
12. The move, at its real size: **connector stops being a family** and becomes a system above; **the registry key goes from family to facet**; **an entity can wear several facets**. The pipelines stay. The named roads (`:text :contour :formula :image :mesh :mark`) are a target vocabulary, not a source fact — each needs a small contract before the registry changes.
13. So: what we need from the engine = the floor shapers, the pipelines, the order / passes / upload / budget custody, the guard's oracles. What we don't = every named kind and the key that names them. What waits = the key change — until the instances are gone and the renderer can be seen at its own grain.

## The waist, drawn

```
ABOVE — the open world
  entities · facets · recipes · ECS systems · what a gesture means · arbitrary new combinations
  material is data; systems are code keyed by the facet they serve, never by what a thing IS
        ↓ value-only render packets — a road id + a resource key, never a live handle ↓
BELOW — the finite machine
  road registry (exhaustive, fenced) · geometry and coverage preparation · GPU resource ownership
  order, passes, composition · buffers, upload, execution
  reusable without Softland's meanings; stable geometric values in and out; the GPU never gets invalid data
```

Three strata, in your words (LOG 07-30, "the code editor is Softland at zoom 100"): **material** — data · **Softland code** — the systems, code above the waist · **host floor** — what must remain outside the land. In this phase the systems stratum has no home of its own yet, so the cut is still binary: floor code stays in the repo as engine; family-shaped code goes; git keeps whatever pure geometry is worth lifting later.

## What the old world is here

```
TODAY — keyed by family (10 nouns; each = kind + pipeline + grammar)      WANTED — keyed by facet
  :rect       → rounded-box pipeline                                        entity wears several facets;
  :shadow     → shadow pipeline                                             each road takes one:
  :msdf       → glyph pipeline (atlas)     ┐ one facet,                        :text     → glyph road
  :slug       → glyph pipeline (curves)    ┘ two backends                      :contour  → triangle road
  :image      → image pipeline                                                 :formula  → formula road
  :path       → contour pipeline                                               :image    → image road
  :connector  → (a route SYSTEM, above) → contour pipeline                     :mesh     → mesh road
  :chrome     → marks pipeline                                                 :mark     → mark road
  :region-3d  → 3D passes → compositor lease                                   :effects  → compositor
  :clip       → scissor                                                        :order    → the tape
  one entry = one family · grammar per family · fenced                     recipes (data): card · arrow · outline · 3D window
```

Where the key lives — the places a facet key would replace it: `scene_tape.cljc:14` (`family-ids`) and `:480` (ten registrations carrying pick modalities and edit operations) · `frame_inputs.cljc:28-69` (the declaration table) · `renderer.cljs:3297` (the executor registry) and `:3418` (the frame description's buckets: `:rects :shadows :images :paths :connectors :chromes :regions`) · `scene_store.cljc:319` (`derive-store-frame` fills the buckets) · `rect_tree.cljc:205-611` (eight walkers, one per kind) · `selection.cljc:12,145` and `chrome_derive.cljc:17-20` (family sets deciding behaviour) · the verifier's per-family atoms.

The one waist violation inside the tape itself: an entry's `:paint/source` carries a **live JS system object** (`path_gpu.cljs:367-368`, `region3d_gpu.cljs:1401`). Data holding a mutable handle — an entry cannot cross the waist as a value today. The future packet names a road and a resource key; runtime-owned state is resolved below.

## Every family, three bands

KB are file sizes (fact). Splits inside a file are estimates. Sorts: **floor** stays as code below · **system ↑ / recipe ↑** belongs above (code or data) · **goes** cut now · **open** not yet closable.

| family | said → geometry (the shaper) | pipeline | the old-world instance on it | where it sorts | KB |
|---|---|---|---|---|---|
| **text** | `text_shaper` — one shaper (HarfBuzz wasm, `:13`): string + features → glyph runs in font units (`:268-290`). `text_layout` — + size, width, wrap → lines, glyph positions, metrics in material-local space (`:682-1000`); the cache is a value the caller threads (`:1029-1084`). `planes` — typed-array storage (`:290-304`). The caret / selection / hit *readers* (`:1304, :1513`) stay with it — nobody else measures. | msdf or slug, chosen downstream (`fonts.cljs:203-207`) | A second layout road, `legacy-layout` (monospace grid, 138 lines, `:174-311`) — but at least one JVM test asserts it (`text_layout_test.clj:151`): it is the JVM's stand-in shaper. A closed 16-keyword `ground-op-roles` (`:23-27`). | **floor**; *open*: the legacy road | 125 |
| **box** | `rect_tree/layout-children` — children + direction / gap / padding / align → bounds; a pure pre-pass (`:38, :54-132`), shape-blind, ~80 lines. | rounded box + shadow, inside the renderer | **The tree**: node `{:id :type :bounds :style :children :data …}` (`rect_tree.cljc:19-32`) with one `:data` slot per kind and eight walkers; store slots by `vi` (`scene_store.cljc:96-126`); pick by bounds (`:404`); `selection` bound to `vi`/address with "rect = blocks" (`selection.cljc:145`). | **goes** — the tree (your ruling) · **system ↑** — layout goes with the tree now; 80 lines to re-derive over facets | 79 |
| **stroke / shape** | `path_material` — contours, or centerline + pressure (`:8-10, :81`) → `path_tessellation/tessellate [material zoom]` → triangles (`:383-402`). Pure. **No editor code.** You named this road yourself. | contour — `path_gpu`, pure `prepared-op` → `pack-vertices` (`:217-252`); its one `case` is a clipping algorithm (`:154`) | Point-edit verbs inside the description ns (`move-knot`, `move-contour-point`, `:288, :336`) — small. The live handle in `:paint/source` (`path_gpu.cljs:367`). | **floor** | 63 |
| **image** | `image_material` — digest + size → atlas placement + a 13-word quad (`:257, :325`); registry and atlas are values in, values out (`:67, :250`). | image, inside the renderer | Almost none — the packer refuses per-node transforms by design (`:326-327`). | **floor** | 15 |
| **transform** | `containers` — spec → one affine composed through parents; forward / inverse point (`:87, :232-253`). Pure; highest fan-in in the tree. | feeds every pipeline | none | **floor** | 12 |
| **marks** (code: chrome) | `chrome_material/material-quads` — world anchor + px offset → quads (`:241-257`) — but it switches over the five forms, and `chrome_gpu` calls it (`chrome_gpu.cljs:169`). | the hybrid-metric vertex: anchor in world, offset in px (`chrome_gpu.cljs:182-204`) | The five forms `#{:selection-outline :handle :marquee :guide-line :gap-tick}` (`chrome_material.cljc:16`); px constants (snap 8, handle 10, slop 6, `:10-14`); `snap` (guides, gap ticks); `chrome_derive` (selection + gesture → marks; two `defonce` providers, `:23-24`); `chrome_runtime` (dead; throws without host hooks, `:330-333`). | **floor** — the hybrid-metric pipeline · *open* — a form-free mark road is new work · **goes** — the rest, now | 75 |
| **arrows** | No shaper — `connector_route` is a **system**: bound thing → its boundary quad → clip from center (`:173`) → elbow Z-route (`:185-195`) → trim for heads (`:207, :218`) → label at *t* (`:390`) → synthesize a path material (`:334`) → tessellate (`:360`). | contour (borrowed) + its own buffer + a cloned text system (`connector_gpu.cljs:185-226`) | The whole family: `:elbow/v1`, triangle heads (`connector_material.cljc:14-17`), a live pick mirror + provider by mutation (`connector_route.cljc:18-21`), a 142-line per-edge cache (`:544-685`), twelve system atoms (`connector_gpu.cljs:115-126`). | **system ↑** · **position**: cut the family whole now; the pure geometry named here is listed for lifting from git when arrows are rebuilt over facets. Codex holds this open; one line from you vetoes it. | 74 |
| **3D** | `region3d_scene` — primitive → mesh (`:325`), parent table → matrices (`:140-160`), triangles → BVH (`:394`), ray → object (`:516, :947`). `region3d_placement` — 2D text / ink onto a plane, reusing the text shaper (`:180, :334`) — but it reads the old slot shape (`:22, :41, :60, :72, :78`). `region_rungs` — texture budget grant (`:50-93`). | region passes (depth, shading, placement glyphs / ink, grid, gizmo overlays) → compositor lease (`region3d_gpu.cljs:1122-1387`) | **The small Blender**: kind menus (`region3d_material.cljc:18-22`), nine edit ops by `:op/id` (`:82-86, :511`), orbit / gizmo / handle picks (`region3d_scene.cljc:616, :696, :932`), gizmo + grid shaders (`region3d_gpu.cljs:257-378, :206`), a global `!pick-state` + device WeakMap (`:705-706`), `region3d_runtime` (panel at `[236 146]`, fixture scene, `window.region3d`, `:23, :78-118, :602-620`), `region3d_pointer` (dolly / wheel), a CPU PBR reference only the verifier calls (`region3d_scene.cljc:1141`, `verifier.cljs:4587`). | *open* — the kernel (mesh · hierarchy · BVH · ray · placement): your question · **goes** — the small Blender, either way · *open* — placement's input after the store goes | ~260 |
| **effects + order** | `frame_effects/derive-effect-spans` (`:204-227`); `scene_tape/compile-tape` (`:724`) + `paint-forward` / `pick-reverse` (`:748, :757`); `frame_plan_view` (the incremental plan, `:232, :315`); `frame_delta`, `frame_semantic_state`; `frame_graph` = the verifier's plan *oracle* (`:668, :692-764`). | compositor — passes, targets, blur / mask / opacity / clip (`compositor_gpu.cljs:1303`) | The family declaration table (`frame_inputs.cljc:28-69`) with `:editor-pool-info` in the rect row; the ledger written to `js/globalThis` (`:242-248`); two orphaned maintained twins — `maintain-frame-plan` (`frame_graph.cljc:677`) and `maintain-effect-spans` (`frame_effects.cljc:250`), test-only. | **floor** · **goes** — the two orphans, the ledger · the table changes with the key | 145 |
| **GPU core** | `renderer/shape-text` — a real shaper: text ops + fonts → placed paint rects, placement backend-independent (`:2345-2353, :2277-2279`). | shaders (≈18 % of the file), systems, frame compile / execute (`:3470-3677`); `compositor`; `buffer_pool` (slot / generation, `:120-141`); `gpu_budget`; `region_bindings` | The family registry (`:3297`) + fence (`:3364`) + buckets (`:3418`); editor leftovers — `calculate-bracket-rects` (`:730`), `hit-test` (`:750`), `make-snapper` (`:2134`), `create-editor-state` at 50 000 rects (`:2077`), `draw-comparison-frame!` (`:2637`); `gpu-mount` with its "DO NOT WIRE AS-IS" banner (`buffer_pool.cljs:448-457`). | **floor** · **goes** — the leftovers, `gpu-mount` · *open* — the key change | 313 |
| **the guard** | CPU oracles — reference geometry for the pipelines: `region3d-lit-oracle` (`verifier.cljs:4579-4648`), `point-in-curves?` (`:317`), `route-boundary-distance` (`:2482`). | 67 golden files, headless puppeteer (`test/render_engine/run_verifier.mjs`) | Per-family fixture atoms — image 14 %, W4 frame 27 %, 3D 18 %, marks 6.5 %, arrows 9 % of the file; an estimated 60–70 % of it is scene construction (sampled, not counted). | **floor** — oracles + pipeline goldens · **goes** — the fixture atoms of whatever family goes; the W4 receipt rehomed | 304 |
| **demos** | — | — | `frame_runtime`: eight fixed trees at hard-coded coordinates + a PNG export chord (`:61-163, :268-274`); the 3D runtime's fixture; the `?live-atoms` / `?region3d` readers nobody calls; `resources/public/faces/`. | **goes** | 12+ |

## The cross-cutting instances

### Statics — three kinds, not one

**Go**
- Providers set by mutation: `!effective-provider`, `!live-route-cache` (`connector_route.cljc:18-21`); `!live-camera-provider`, `!live-effective-provider` (`chrome_derive.cljc:23-24`).
- Browser globals: `__softlandFrameLedger` (`frame_inputs.cljc:242-248`), `window.region3d` (`region3d_runtime.cljs:602-620`), `window.sceneContext` (`scene_runtime.cljs:216`).
- The runtimes' session atoms (19 across three files) — free, they go with the runtimes; the one store atom (`scene_runtime.cljs:25`).
- The 3D global `!pick-state` (`region3d_gpu.cljs:706`) — a cross-family mutable mirror.

**Stay below**
- Device-owned resources: `!systems-by-device` (`region3d_gpu.cljs:705`), `!compositors-by-device` (`renderer.cljs:3397`).
- System-local buffers and capacities (`buffer_pool`; every `!buffer` / `!capacity`).
- Bounded derived caches (glyph maps `renderer.cljs:2147-2148`, mesh caches); budget accounting (`gpu_budget`); compositor leases.
- Explicitly owned frame history where incremental rendering needs it (`frame_plan_view` state, `region_bindings`).

Only the *go* list is a build-stage item. "Thread every atom through as values" is a style choice with a per-call cost, not a ruling.

### Gates — where "describe, never gate" applies

The law is about product descriptions, and it carves out the floor itself — *"Totality gates on the meta-material's OWN form (error cards) are the dull floor, not a violation"* (`docs/editable-material/DIRECTION.md:94`). So: **above**, an unknown facet still exists and gets an error / fallback projection — an untyped block renders, forever. **Below**, a road refuses a malformed packet (NaN, a bad index, an oversize mesh, a cyclic parent) and shows the fallback; the GPU is never handed invalid data.

- **Go — kind menus that are product vocabulary:** `legal-forms` (`chrome_material.cljc:16`); `legal-object-kinds` · `-primitive-` · `-light-` · `-camera-` (`region3d_material.cljc:18-22`); `legal-route-policies` · `legal-heads` (`connector_material.cljc:14-17`).
- **Stay — shape and totality checks:** `validate-region!` (finite vectors, indices, caps, acyclic parents; `region3d_material.cljc:432-464`); contour / knot validation (`path_material.cljc:156`); entry shape (`scene_tape.cljc:653-688`); the exhaustiveness fence (`renderer.cljs:3364`).

### Twins — maintained view + oracle

The guard's pattern everywhere is an incremental view checked against a full recompute. An oracle is off the live path *by design* — it is not dead. Keep: `frame_graph` (the verifier's plan oracle, called six times from `verifier.cljs`), `frame_effects/derive-effect-spans`, `chrome_derive/full-recompute-oracle` (`:231`), the scheduler's `decide` (the verifier's, `verifier.cljs:4986`). Orphaned — only tests call them: `maintain-frame-plan` (`frame_graph.cljc:677`; `frame_graph_test.clj:44-56`), `maintain-effect-spans` (`frame_effects.cljc:250`; `frame_effects_test.clj:67-71`), and `gpu-mount` (`buffer_pool.cljs:442-493`). Position: the three orphans go; the oracles stay with the guard.

## Where this leaves the board

**Cut — convergent, contract-ready at your word** (two independent reads, one list)
- the tree — `rect_tree` · `scene_store` · `scene_runtime`; `selection` with it
- the demos — `frame_runtime`, the 3D runtime's fixture, the two flag readers, `resources/public/faces/`; the W4 receipt rehomed into the verifier
- the marks vocabulary — five forms, px constants, `snap`, `chrome_derive`, `chrome_runtime`
- the small Blender — `region3d_runtime`, `region3d_pointer`, orbit / gizmo / handle picks + edit ops + kind menus in `_scene` / `_material`, gizmo + grid shaders, the global pick state — **regardless of the 3D answer**
- the three orphans; the ambient providers; the browser globals; the renderer's editor leftovers
- *position, vetoable:* the arrow family whole, with the lift list

**Keep — floor candidates**
- text shaping + layout + planes (+ the fonts loader)
- path authority + tessellation + the contour pipeline
- image preparation · `containers`
- scene ordering (the tape) · effects + plan + deltas
- compositor · pool · budget · region bindings
- the renderer's shaders, systems and frame execution
- road-level oracles + goldens
- the 3D kernel (mesh · hierarchy · BVH · ray · placement) — *if* a 3D facet is possible

**Not yet contractable — build stage** (seen at the renderer's own grain, after the cuts)
- family key → facet key, with multi-facet entities (registry, declaration table, buckets, the per-family prepare / upload in `draw-frame!`)
- the render packet: road id + resource key; no live handle in `:paint/source`
- a form-free mark road (a neutral vertex input for `chrome_gpu`)
- `region3d_placement`'s input once the store is gone
- validator sorting — kind menus out, shape checks in
- the *go* statics · the legacy text road (needs a JVM shaped provider first) · the connector lift list, if the whole-family cut is vetoed

**Only Sid** — *Can an entity wear a 3D mesh / material facet that needs depth-aware rendering and spatial queries?* Yes — the mesh road and the pure scene geometry stay. No — the 3D kernel goes too. The small Blender goes either way. (Position withheld on purpose: this one is yours, and dressing it in your past words was the thing you'd catch.)

**Sequence** (both reviews, and I agree): rule the 3D facet → land this page → cut the instances → then look at the key change at the renderer's grain, with about 40 % less code to see through.

## What is not sure

- Magnitudes — 60–70 % fixtures in the verifier, ~180 editor lines in `region3d_scene`, ≈18 % shader text in the renderer — are sampled estimates, not counts.
- The legacy text road: one JVM test asserts it; whether it is the *only* JVM road for layout is unchecked.
- `apply-variations!` (`text_shaper.cljs:261`) mutates HarfBuzz faces per call — variation axes may be order-dependent (a structure claim from reading, not a measured fault).
- The numerical checks Codex cites near `region3d_material.cljc:252` were not re-read here; `validate-region!` (`:432-464`) was.
- Whether the connector's pure geometry is worth lifting or cheaper to rebuild is a build-stage call, not a shape fact.

## Words

- **waist** — your test (08-19): what has to exist as code vs what can be built on top, ECS or whatever style. The name is not yours — you asked why it was called that — the test is.
- **floor** — the code under the waist; here, the render floor. Narrower than "must be code": reusable without Softland's meanings, stable geometric values, GPU custody.
- **facet · entity · recipe · system · binding · verb** — your July words for ECS: a facet is one attached piece of data saying one true thing about an entity; a recipe is the list of facets a kind of thing wears; a system is code doing one job for everything wearing a facet.
- **family** — the engine's own word for its ten kinds (`:render.family/*`) — the key this page says is the old world.
- **road · pipeline** — session words. A pipeline is what exists on the GPU today (nine). A road is the target: one way to pixels keyed by one facet. Your phrases: "text in placement out"; "stroke shape → tessellator → triangles".
- **marks** — your March word; the code says "chrome"; in the felt pass you said "the blue box", "bounding box".
- **the tape** — session word (2026-08): the one ordered list of entries for a frame; paint walks it forward, pick walks it backward.
- **the small Blender** — this page's phrase for the 3D window's editor — orbit, gizmo, lights menu, grid, edit ops, the draggable panel.
- **maintained view · oracle** — the guard's pattern: an incremental computation and its from-scratch twin, checked against each other.

## Evidence

Built from code, not from the prior maps: six Opus read-only gatherers by seam (GPU core · GPU families + verifier · frame machine · material families · workspace + the shaper yardstick · a docs-vs-code check), one gatherer indexing your own words (recall over every prompt you typed + `vision/LOG.md`), parent spot-checks on every decision-changing fact. Two independent reviews were folded in — a sibling Claude session and Codex — on: the fence is not the class system · the move stated at its real size · quotes withdrawn where they carried a position · statics classified · gates re-scoped · twins made precise · the placement coupling · layout's purity · the 3D question restated · pick split into ray-hit and gizmo-hit.

The prior maps — `render-engine-map.md`, `engine-two-arrows.md`, `kept-code-map.md` — are evidence strata, not the current map: their relationship claims hold (eight of nine checked), their counts went stale within a day, and they rule while saying they don't ("leaks", CUSTODY as a delete). Your own words this page leans on: the waist test and "text in placement out" (08-19), "what should exist as code committed … versus the ECS or small talk VM layer" (08-16), "i hate using the word type … what we want is ecs style" (LOG 08-12), "a minimal VM-like floor beneath it" and the three strata (LOG 07-30), "why is box below the waist?? can it not be defined above the waist in ecs??" and the rect ruling (08-25).

*Sources: HEAD at `ed84a6e`, 2026-08-25. Positions are marked as positions; rulings are Sid's.*
