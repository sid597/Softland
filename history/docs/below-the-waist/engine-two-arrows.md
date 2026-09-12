*Below the waist · Area C · one notch under the kept-code map — hops, not pieces*

# The Engine's Two Arrows

Twin of `engine-two-arrows.html` (Sid's eyes; published at
https://claude.ai/code/artifact/561d5133-0413-4d01-839a-ccb8c93be7d3). Both edited together in every commit — never one without the other.

How the parked WebGPU render engine moves data: one arrow down (truth → pixels), one arrow up (pointer → meaning), and why each hop is pure, atom, reactive, or imperative. Then the question this page exists for — why it isn't all "the Electric way" — split into the three things that phrase can mean, with receipts; and the gap that leaves, filled (§8).

Twin of `engine-two-arrows.html` (same claims, same anchors; edited together). Published at https://claude.ai/code/artifact/561d5133-0413-4d01-839a-ccb8c93be7d3. Grain: the hop (function · data in · data out · mechanism on the arrow). Anchors are `file:line` in the working tree; deleted-host anchors are `adc30c9^:path:line`. What outranks this page: the code it points at, then the receipts it cites. Sources and unsettled claims: §11.

- 1 The simple story
- 2 What any GPU-drawn UI must build
- 3 Arrow down
- 4 Arrow up
- 5 Where state lives
- 6 The paradigm, zone by zone
- 7 Why not the Electric way
- 8 The gap, filled
- 9 Wasteful vs irreducible
- 10 The scars
- 11 Sources & uncertainties

## 1 · The simple story

1. The GPU paints a picture from lists of numbers. It remembers nothing about what the numbers mean, and it cannot tell you what was clicked.
2. So we keep our own copy of "what is on screen": one plain value — the scene store — where each thing on screen is a slot holding its geometry, its address, and its order.
3. Everything that reaches the screen travels one way: truth in Rama → a served bundle over Electric → that store → an ordered tape → bytes per kind of drawable → GPU buffers → one draw per frame.
4. Everything the user does travels the other way: a pointer position → undone through the camera → walked against the same tape in reverse (topmost first) → "you are on address X, here is its context".
5. That answer goes one of two places: short-lived session state (hover, selection, a drag, the camera) that changes sixty times a second and never leaves the browser — or a real act (an edit, a settled position) that goes to Rama, becomes truth, and comes back down the first arrow.
6. So it is not "two-way": it is a loop. Down for truth, up for meaning, with a short local loop for the things that have no upstream.
7. State lives by who owns it and how fast it changes: truth on the server; the store as a projection of it; session things in their own atoms; GPU buffers as the renderer's private mirror — never truth.
8. The middle is pure functions over values (store, tape, rect trees, the shaper) so it runs on the JVM in tests, replays, and is legible to agents — actions are descriptors, never closures.
9. Mutation happens only at the edges — a user act, a server echo — by swapping an atom; each derived view is one Missionary `m/latest` over one watch.
10. The GPU part is imperative because the WebGPU API is imperative (make a buffer, write bytes, set a pipeline, draw). It is the sink — the one place effects are allowed.
11. The clock is a single rAF sample: take the latest world value once per frame, skip the frame if nothing changed. The camera and the frame counter may be read at that edge and nowhere upstream — the engine refuses them as inputs at load time.
12. "Why not Electric for all of it" is three questions. As *transport*: kept — but we used it as an RPC pipe (request atom → server → answer atom, 13 lanes, whole-page bundles) instead of its keyed reactive wire; that is the real hole. As *machinery inside the engine*: the idea is adopted (keyed diffs, per-item upkeep), the library's applier is not — it is shaped for the DOM and broke measurably on a GPU pool. As *the language faces are written in*: you ruled it out — faces are data.
13. And the engine already *is* a dataflow graph — declared inputs per family, changed-families, deltas, a reducer — just hand-wired, with no compiler deriving it from code. That is why adding a view touches seven sites, and why it reads as tons of code.
14. What is genuinely wasteful: double roads kept alive on purpose (batch oracle beside its incremental twin; roads built and never wired), accretion across many atom sessions, a verifier bigger than the renderer, ten dialects of "watch". What is irreducible: store, tape, shaper, families, pools, pick, frame edge.
15. Net: your instinct — one reactive system, recompute proportional to change — is the settled direction. The parked engine is the *before* picture of it, half migrated, with the hand-wired middle still standing.

```
DOWN — truth to pixels

 +------------+ served bundle   +--------------------+ ordered tape  +-------------------+
 |    Rama    |---------------->|    scene store     |-------------->| bytes per family  |
 | the truth  | Electric (RPC)  | one value · slots  |               | rect · text ·     |
 +------------+                 | keyed by address   |               | path · image · 3D |
       ^                        +--------------------+               +-------------------+
       |                                  ^                                    | writeBuffer
       |                                  : reads the same value               v
       |                                  : the GPU was fed          +---------------------+
       |                                  :                          |         GPU         |
       |                                  :                          | one draw per frame  |
       |                                  :                          |     · stateless     |
       |                                  :                          +---------------------+
       |                                  :                                    | paints
       |                                  :                                    v
       |                                  :                          +---------------------+
       |                                  :                          |       screen        |
       |                                  :                          +---------------------+
 one debounced write                      :                                    | pointer x,y px
 → truth → down again                     :                                    v
       |                                  :                          +---------------------+
       |                                  :                          |   camera inverse    |
       |                                  :                          |     px → world      |
       |                                  :                          +---------------------+
       |                       +--------------------------+                    |
       |                       |           pick           |<-------------------+
       |                       | walk the SAME store,     |
       |                       |       in reverse         |
       |                       +--------------------------+
       |                                  | address + context
       |    +--------------------------+  |
       |    | session state · 60 Hz    |<-+   stays in the browser —
       |    | hover · selection ·      |     read at the frame edge
       |    |     drag · camera        |...> (loops back to the frame edge)
       |    +--------------------------+
       |    +--------------------------+
       +----|      a durable act       |<-+
            | edit · settled position  |
            +--------------------------+

UP — pointer to meaning

The loop closes through the server for truth, and locally for things that have no upstream.
Nothing is read back from the GPU.
```

**Figure 1 — the loop.** Down: truth → store → tape → bytes → GPU. Up: pointer → camera inverse → reverse pick over the same store → address + context → either session state (local, 60 Hz, read at the frame edge) or a durable act (one debounced write to Rama, which comes back down the first arrow). The pick reads the very value the GPU was fed — that is how "what you see" and "what you hit" agree without the GPU saying a word.

## 2 · What any GPU-drawn UI must build (the architecture in general)

The browser gives a DOM app five organs for free: a retained tree, layout, paint, hit-testing, and an event system. Electric-dom is a reactive *writer* into that tree — it mints diffs and the browser does the rest. WebGPU gives you none of the five. It is a rasterizer with a command queue: you hand it buffers and a shader, it fills pixels, it forgets. So a WebGPU UI has to rebuild, in its own code, exactly the organs the browser would have supplied. That is the first honest answer to "why is there so much code": Softland is carrying a thin browser-engine slice of its own.

| Organ | DOM + Electric-dom | WebGPU + Softland | where it lives here |
| --- | --- | --- | --- |
| retained scene | the DOM tree — browser owns it | you own it: a value describing every drawable | scene_store.cljc (one atom in scene_runtime.cljs:34) |
| order | tree order / z-index — browser sorts | you own it: a sorted tape, paint forward, pick reverse | scene_tape.cljc · scene_store.cljc:155 |
| layout + text | CSS layout, font shaping — browser | you own it: rect trees, a shaper (text in → glyph placements out) | rect_tree.cljc · text_layout.cljc · text_shaper.cljs |
| paint | browser's compositor | you own it: per kind of drawable, a "family" that turns slots into bytes + a pipeline | chrome/connector/path/image/region3d _material + _gpu · renderer.cljs |
| GPU residency | invisible (browser) | you own it: buffer pools, glyph atlases, a budget | buffer_pool.cljs · gpu_budget.cljs |
| hit-test | `event.target` — browser | you own it: a pick over the scene, in reverse paint order | scene_store.cljc:406 · scene_tape.cljc:757 · rect_tree.cljc:669 |
| frame edge | browser's vsync / style recalc | you own it: one rAF sample, skip-if-unchanged, uploads, draw | adc30c9^:runtime/render.cljs:629 (deleted host) · frame_scheduler.cljc |
| session state | mostly DOM state (focus, scroll, :hover) | you own it: atoms for camera, gesture, hover, selection | chrome_runtime.cljs · region3d_runtime.cljs · (ground.cljs, deleted) |

### The axes you choose on

- **Retained or immediate.** Immediate-mode (ImGui) re-declares the whole UI every frame: simple, cost ∝ population every frame. Retained keeps a scene and patches it: cost ∝ change, but now you own identity, order, and invalidation. Softland is retained.
- **Where proportionality comes from.** A compiler that derives the dependency graph from your code (Electric, Solid, Incremental) — or hand-keyed inputs + dirty sets + reducers. Softland's engine is the second: the graph exists, hand-wired.
- **Where picking happens.** CPU over your scene (Figma, Flutter, browsers) or a GPU id-buffer read back (games). CPU gives you the address, the path, the caret position, synchronously, and the same answer for an agent that has no GPU; readback gives an id, a frame late. Softland is CPU — and has zero readback (§4).
- **State by cadence.** Durable truth (server), served mirrors, session-only (60 Hz), per-frame, GPU residency. Mixing cadences in one atom is the classic way to make a caret blink re-layout a document (§10).

| Reference instance | scene | proportionality | pick | loop |
| --- | --- | --- | --- | --- |
| DOM + Electric | browser tree | compiler-derived DAG; incseq diffs mount into the tree | browser | reactive, no frame concept |
| Flutter | three trees (widget → element → render object) | rebuild widgets, diff elements, relayout dirty render objects | CPU, render-tree walk | vsync-driven |
| Figma | own C++ scene graph | dirty flags on the graph, tile re-render | CPU over the graph | frame loop; multiplayer edits the graph |
| ImGui | none (re-declared per frame) | none — whole UI per frame | inline, per widget, last frame's rects | per frame |
| Games / ECS | entities + component arrays | systems iterate arrays; dirty masks | often GPU id-buffer | fixed tick |
| **Softland (parked)** | one EDN value: slots + sorted tape | hand-keyed: declared inputs per family → changed families → deltas → reducer; a batch twin as oracle | CPU, reverse tape | rAF sample of a Missionary snapshot; skip if unchanged; truth round-trips Rama |

The non-Softland rows are from general knowledge of those systems, not from reading their sources — treat them as orientation, not receipts. Reading across the rows: Softland sits nearest Figma/Flutter (retained scene, CPU pick) with an ECS-shaped paint layer (one family per drawable kind) and an Elm-shaped loop where the server is the reducer for durable acts.

## 3 · Arrow down — truth to pixels, hop by hop

This is the path as it was wired in the last living host (deleted at `adc30c9`, the waist cut) plus the engine that survived. Solid boxes are kept; dashed are deleted. The label on each arrow is the *mechanism* — that is what answers "declarative, functional, or imperative?" at every hop.

```
STAGE            MECHANISM (on the arrow)            DATA SHAPE OUT

[ deleted host ]
 1 · Rama serves a face bundle · file_viewer.cljc FacePull → face-projection/serve
   | e/watch !face-request → e/server → reset! !face-data
   |   Electric used as RPC · adc30c9^:electric_flow.cljc:278
   v  out: the whole data-context map · (page-sized, anonymous value)
 2 · mirror atom · face_wiring.cljs:700 "the R4 mirror-atom pattern"
   | add-watch !face-data → reset! !face-context
   |   21 such add-watch mirrors across the old client
   v  out: same map, second atom
 3 · the consumer edge · runtime/render.cljs:183,220 → ground/reconcile!
   | m/watch → m/latest → m/reduce, in a coalescing microtask
   |   "store mutations at edges only (T4), NOT the RAF edge"
   v  out: a rect tree per face · (36 builders, 12 block-*; face_primitives/registry)
 4 · the store swap · scene_runtime.cljs:186 register-face-instance!
   |   scene_store.cljc:169 upsert-slot (pure)
   | swap! !scene-store (pure fn under the swap)
   |   build-slot → ops lanes · patch address index · patch sorted
   |   tape BY KEY (sorted-map-by entry-key-compare, :155)
   v  out: slot {:tree :ops{text rects shadows images paths …} :addresses :stack-path}
 5 · the store-frame view · scene_runtime.cljs:452 <store-frame
   | (m/latest ss/derive-store-frame (m/watch !scene-store))
   |   ONE sharing point · rebuilds every lane whole per swap (O(N))
   v  out: {:rects :text-by-vi :chromes :ordered-vis :targets-by-address …}
[ deleted ]
 6 · the world snapshot · runtime/render.cljs:193 <world-snapshot
   | m/latest fan-in of 7 flows — camera deliberately NOT one
   |   "combined ONCE … no diamond (T3)"
   v  out: one snapshot map
 7 · the frame edge — the clock · runtime/render.cljs:629 · events.cljs:180 (>raf)
   |   frame_scheduler.cljc:165 decide-at! (kept, uncalled)
   | (m/sample vector <world-snapshot >raf) → m/reduce fold
   |   latest value once per vsync · prev-state carried · scheduler
   |   says encode? or skip · camera deref'd HERE, sink-local
   v  out: [world frame-time]
 8 · uploads · buffer_pool.cljs:275 ordered-diff-update-pool!
   |   renderer write-containers! · text geos (host side deleted)
   | identical?-gated; writeBuffer only where the value moved
   |   containers = one 1024×16 B uniform · rects = keyed pool
   |   text = per-slot identity fast path
   v  out: bytes in GPU buffers · (container-local f32; store is f64)
 9 · draw-frame! — the hand-built DAG · renderer.cljs:3678 · frame_inputs.cljc:134
   |   changed-families · renderer.cljs:3470 produce · :3506 arrangement · :3830 reducer
   |   (no caller in kept src — parked)
   | declared inputs → changed families → produce only those
   |   → patch arrangement by key → entry deltas → semantic
   |   reducer (one reset!) → camera enters via scissors (:3635)
   |   twin check: batch compile-frame-tape as ORACLE, every frame
   v  out: frame entries (ordered) + {:plan-hash …}
10 · encode · compositor_gpu.cljs draw-multipass! → renderer.cljs:3057
     setPipeline · setBindGroup · draw — imperative, the sink
       pools resolved at encode time (renderer.cljs:2715)
     out: pixels

dashed = deleted at adc30c9 (the host that drove the engine) · solid = kept · the engine
keeps the machinery, the host kept the clock.
```

**Figure 2 — arrow down, ten hops.** Read the middle column top to bottom and the paradigm census writes itself: Electric as RPC (1), a hand mirror (2), one Missionary edge whose only job is to coalesce and call (3), a pure swap (4), one `m/latest` per derived view (5–6), one sample per frame (7), identity-gated uploads (8), a keyed reducer pipeline with its batch twin (9), imperative encode (10). The store is a projection of the served bundle; the GPU is a projection of the store.

> **Three things worth holding from this arrow.**
>
> - There is exactly one reactive membrane in the kept engine — three `m/watch → m/latest` views in `scene_runtime.cljs:452,463,469` — and it computes nothing of its own: it wraps a pure JVM-testable function so the derivation can be shared and sampled. Missionary is the soil at named points, not the material.
> - The clock lived in the host and is gone: `requestAnimationFrame` has zero hits in kept `src/`; `draw-frame!` has no caller. The engine is a machine with no crank — which is what "parked" means physically.
> - The one remaining whole-population step is hop 5: `derive-store-frame` re-mapcats every slot's ops on any swap. Everything below it is keyed; everything above it is a page-sized anonymous value. That is the seam the electric-native arc names as "keyed at rest, anonymous in motion".

## 4 · Arrow up — pointer to meaning

WebGPU never says what was clicked, so the engine answers it from the thing it already has: the store. A pick is a walk over the maintained tape in the *exact reverse* of paint order — topmost drawn, first asked. There is no GPU read-back anywhere: `mapAsync`, `copyTextureToBuffer`, `MAP_READ`, `getMappedRange` have zero hits in the 198 KB renderer. The renderer's `:pick` field is a declaration on a draw entry (`renderer.cljs:2696`), never a pass.

```
  +------------------------+   m/observe    +---------------------------+
  |   DOM pointer event    |--------------->|      screen → world       |
  | canvas-relative CSS px |                | [(sx−x)/zoom (sy−y)/zoom] |
  +------------------------+                +---------------------------+
                                                          | @!camera
                                                          v
                            +----------------------------------+
                            |             ss/pick              |
                            | {:world [wx wy] :screen [sx sy]} |
                            +----------------------------------+
                                     | (rseq entries) — first hit wins
                                     v
   the maintained tape (sorted map, patched at writes)
   +--------------------------------------+ asked 1st   +----------------------------+
   | entry n — topmost painted            |             |         per entry          |
   +--------------------------------------+ asked 2nd   | 1 inverse-point into       |
   | entry n−1                            |-----------> |     container-local        |
   +--------------------------------------+             | 2 rect-tree hit-test       |
   | …  (invisible / :pick :none skipped) |             |   (bounds prune each level)|
   +--------------------------------------+ asked last  | 3 family narrow predicate  |
   | entry 1 — bottom of the paint        |             | 4 deepest addressed node   |
   +--------------------------------------+             +----------------------------+
                                     |
                                     v
   +------------------------------------------------------+
   |                       the hit                        |
   | {:vi :path :address :src-path :actions :point-local} |
   +------------------------------------------------------+
        |                        |                         |
        v                        v                         v
   +----------------------------------------------------+
   |     session atoms · 60 Hz · no events              |
   | !hover (idle moves) · !pointer gesture machine     |
   | !camera (pan/zoom local) · selection · drag        |
   | settle on release → ONE POST /api/episode/geometry |
   +----------------------------------------------------+
   +----------------------------------------------------+
   |                  action dispatch                   |
   | the slot's :actions are DESCRIPTORS (data)         |
   | → !action-registry handler (never a closure in     |
   | the store — agents can read the scene)             |
   +----------------------------------------------------+
   +----------------------------------------------------+
   |                 the deictic seat                   |
   | !last-pick keeps the world                         |
   | point; an agent turn re-picks                      |
   | from it at submit time                             |
   +----------------------------------------------------+
```

**Figure 3 — arrow up.** Path (with the deleted driver): `events.cljs >mouse` → `mouse.cljs` `m/reduce` → `ground/pointer-down!` → `pick-at` → `scene_runtime/pick-world:373` → `scene_store/pick:406` → `scene_tape/pick-reverse:757` → `containers/inverse-point:263` → `rect_tree/hit-test:669` → `deepest-addressed:249`. Region3D is a second stage: the 2D pick lands on a region rect, then a ray is cast inside it (`region3d_runtime.cljs:204`, `region3d_pointer.cljc:36`).

### Why the pick is CPU-side, over the store

- **One structure serves render and hit.** Trap T8 in the scene contract: hit-testing in screen space against transformed containers made click-space diverge from render-space. The fix is structural — inverse-transform the point into the container, then ask the same tree the painter used. Agreement by construction, not by calibration.
- **The answer must carry meaning, not an id.** A click needs the address, the path, the local point (for a caret), the actions. A GPU id-buffer gives an integer, a frame late (readback is async).
- **Agents and the mouse share one finger.** `bundle-for-viewport` (`scene_runtime.cljs:398`) re-picks from `!last-pick`'s world point when an agent turn is submitted — no GPU, same answer. "Pointing is a click act, never a hover side effect" (`adc30c9^:ground.cljs:3817`).
- **Cost shape (measured 2026-08-23, JVM):** linear over slots — `maintained-entries` (`scene_store.cljc:314`) rebuilds the O(N) entry vector on EVERY pick before the reverse walk; the walk itself short-circuits at the first hit, the setup does not (topmost hit = 62 % of a miss at 2000 slots). Medians at 10 / 200 / 2000 slots: topmost 15 / 88 / 278 µs · miss 24 / 129 / 452 µs (`history/docs/electric-native/receipts/2026-08-23-jvm-bench/`). The ~4.5 ms/event at 189 blocks on a swiftshader dev build (`history/docs/space-as-entity/GATE.md:102`) is ~30× this — hypothesis: that cost lives elsewhere (event path / JS bridge); kill-probe: `performance.now()` around `ss/pick` alone in the host.

> **In-flight gestures never become events.** `:camera/pan :move` is `reset! !camera`; only `:end` arms a settle, and `arm-settle!` coalesces bursts into one acked write (`adc30c9^:ground.cljs:3620–3630, 2112`). Trap T10: "committing an event per mousemove — log flood; gesture ≠ assertion." This is the short local loop in Figure 1.

## 5 · Where state lives, and why it is split that way

The old client held about 136 atoms. That number is the honest weight of "we own the organs the browser used to own" plus a tax from using Electric as RPC. Grouped by owner and cadence:

| Layer | What it holds | Cadence | Count (old client) | Why it is its own layer |
| --- | --- | --- | --- | --- |
| truth | Rama PStates — material, geometry, wear | per accepted act | 0 client atoms | "the scene store is a projection, never a second truth-owner"; only session truths with no upstream may originate client-side |
| served mirrors | 24 request/answer atom pairs (Electric lanes) + 13 runtime mirrors (`!face-context`, trail, overlay) | per pull (epoch bump) | 37 | Electric as RPC needs a mailbox per lane; each lane then mirrored once more for the Missionary side — this is the 13 + 21 = 34 watches the seam ruling wants collapsed |
| scene store | `!scene-store`, `!containers-registry`, delta journal, `!last-pick`, action registry, cid pools, `!vi-faces` | per edit echo / per register | 9 (kept) | one swap = one generation: truths that must be seen together live in one value (T3: no diamond off the store) |
| session-only | camera, pointer machine, hover, settle queue, selection, drag, snap, Region3D session, viewport, mouse xy, fonts, settings | 60 Hz | ~53 (23 kept in chrome/region3d/frame runtimes) | independent cadences stay separate and meet at the frame pull; a 60 Hz truth in the store would wake the whole store's derivation |
| renderer-private residency | buffer pools (capacity, free list, generations, prev-keyed-rects), glyph WeakMaps, `!frame-semantic-state`, prev-inputs, compositors by device | per frame | 14 | "residency is the applier's private business — never writes the store"; dropped on device swap |
| derived caches | layout caches, anatomy compile cache, bindings, probes/stall samples | varies | ~32 (old ground.cljs) | the accretion layer — many were probes and counters, never structure |

Counts from a census of `adc30c9^` plus the kept tree; `editing_runtime.cljs` (14 defonce) not itemized. The principle underneath: *ownership gives consistency; sampling does not.* Things that must agree share one atom and one swap; things on different clocks get their own atom and meet once, at the frame edge.

## 6 · The paradigm, zone by zone — and why each zone is what it is

| Zone | Paradigm | Why (the law or scar behind it) | anchors |
| --- | --- | --- | --- |
| scene algebra · tape · rect trees · containers · shaper · frame reducers/compilers | **pure functions over values** (`.cljc`, JVM-tested; no atoms, no GPU objects, no renderer imports) | testable without a browser; replayable; agent-legible (T1: actions as descriptors, never closures — the store walks itself for `fn?`); order is data, not position (T2) | scene_store.cljc:5-8 · scene_tape.cljc:6-8 · frame_semantic_state.cljc |
| owners: scene runtime, chrome runtime, region3d runtime | **atoms + swap!/reset!** at the edges | mutation only at edges — a user act or the server echo — never inside a flow (T4); one swap per entangled truth = one generation | scene_runtime.cljs:9-15,34-36 |
| the membrane | **Missionary, at named points**: `m/watch → m/latest` per derived view; `m/sample` at rAF; `m/reduce` as the frame-to-frame fold | L4: `m/latest` re-runs only on input change, never on sample — so effects inside it die (T4); L8/T3: two chains combined downstream tear frames — one `m/latest` per truth; L1/T9: per-slot `m/ap` forks over a watch crash ("Watch cancelled") — one watch, filter below it | scene_runtime.cljs:452-469 · adc30c9^:render.cljs:193,240,629 · electric-docs SKILL L1/L4/L6/L8 |
| the sink: renderer, GPU halves, pools, compositor | **imperative** — `set!`, `reset!`, JS objects, `writeBuffer`, `setPipeline`, `draw` | the WebGPU API is a command queue; effects are legal only here and at mutation sites; camera and time are read here and banned upstream (`forbidden-declared-inputs` enforced at load) | renderer.cljs:3057,3635,3889 · frame_inputs.cljc:25,91 · frame_scheduler.cljc:4 |

In one sentence: **functional core, imperative shell, one reactive membrane at the seams.** It is not "declarative UI" in the React/Electric-dom sense for a structural reason — there is no tree to declare into; the engine *is* the tree. What it has instead, inside `draw-frame!`, is a dataflow graph with explicit keys:

```
the maintained road (runs inside draw-frame!)

+------------------+   +------------------+   +------------------+   +----------------------+
| declared inputs  |-->| changed-families |-->| produce entries  |-->|     arrangement      |
| per family ·     |   | identical? →     |   | only changed     |   | sorted map, patched  |
|   frame_inputs   |   |   rev → =        |   |   families       |   |   by key             |
+------------------+   +------------------+   +------------------+   +----------------------+
                                                                                |
                                                                                v
                                                          +---------------------------------+
                                                          |    deltas → semantic reducer    |
                                                          | plan view · effect view ·       |
                                                          |            generation           |
                                                          +---------------------------------+
                                                                    ^           |
                                                                    |           v
                                                          +---------------------------------+
                                                          |  scissor with camera → encode   |
                                                          +---------------------------------+
                                                                    |
the batch twin (kept alive as the oracle, by law)                   |
+-----------------------------------------------------+            |
| compile-frame-tape — the whole tape, every frame     |            |
| renderer.cljs:3545 · keyed on [:frame idx]           |            |
|   (sink-only)                                        |            |
+-----------------------------------------------------+            |
              |                                                     |
              +--> +----------------------------------------------------+
                   | fence: frame-tape-twin-check! — throws on mismatch  |
                   +----------------------------------------------------+
```

**Figure 4 — the engine already is a dataflow graph.** Declared keyed inputs, a change classifier, producers that run only for what changed, a keyed arrangement, deltas, a reducer — that is the shape Electric builds for you from code. Here every node is hand-wired, and by the seam law a whole-batch twin runs beside it as the oracle. Two roads, on purpose — and the second reason the code is big.

## 7 · "Why not just do it the Electric way?" — the three things that phrase means

You asked this on 2026-08-03 and again on 2026-08-12, in your words:

> electric works for dom .. and i think with render-engine + webgpu we would have same????? so why not we building electric native and equivalent
>
> — vision/LOG.md, 2026-08-03

> truly electric should drive the rendering engine and vice versa .. i am also not truly sure what electric-dom means there is no dom previously also we used webgpu
>
> — vision/LOG.md, 2026-08-12 (the return sitting)

The earlier sessions answered in a constitution (`decisions.md` "The render seam"). In plain words it says: "the Electric way" is three different things, and they got three different answers.

### 7a · Electric as transport (client ↔ server) — kept, but badly used

This is the one place Electric should be doing its real work, and where it is doing the least. The census at `adc30c9^`: zero `e/for`, zero `e/for-by`, zero `e/diff-by`, zero `e/Token`, zero `dom/*` except one `<canvas>`. Electric's entire job was: 24 plain atoms, `e/watch` the request half, call an `e/server` fn, `reset!` the answer half, then `e/Task (start-loop! …)` and get out of the way (`adc30c9^:electric_flow.cljc:206-382`). That is RPC with a reactive trigger. The served value is a page-sized anonymous bundle re-sent on every epoch bump. The electric-native docs name it exactly: "keyed and act-linked at rest, anonymous in motion" — every felt bug lives at this seam. **This is the hole your instinct is pointing at.** The settled repair is not "more Electric in the renderer"; it is the keyed wire — server-side `e/diff-by :unit-id`, client-side per-key delivery — so a one-block change arrives as a one-block diff. Untested in both directions today (`RECON.md:91-97`); the Block is the named first probe.

### 7b · Electric's machinery as the scene applier — the idea adopted, the library's applier rejected on receipts

The principle (keyed per-item flows, diffs minted at the write, an item/applier split) *is* the store contract, re-keyed for a GPU target. The library instance was tried and measured:

| Receipt | What it showed | Consequence in the engine | source |
| --- | --- | --- | --- |
| L15 · the mount contract [measured] | Electric's DOM mount passes existing *child handles* back through `insert-before` on a permutation — in the DOM that is a MOVE. A GPU slot pool's "move" is an index update; the bridge allocated on every insert. 100 entities, 91 rotate frames → **16,750 active slots**, 160 ms/frame. The diff algebra stayed correct; the applier shape was wrong. | `gpu-mount` kept with a "⚠️ DO NOT WIRE AS-IS" banner; the store consumes the six diff ops directly instead | buffer_pool.cljs:449-457 · electric-skill/VERDICTS.md:410-455 |
| L16 · diff cost is shape-bound [measured, fenced] | incseq has no cheap no-change path (~2 ms to re-diff an unchanged 10⁴); any reorder falls into the permutation regime (seconds per mint at 10⁴). Cost sits at the producer, so changing transport or consumer does not save it. Fence: raw incseq only, snapshot −44 vs −45. | order is row data (sort keys), never sequence position; reorders travel as `:change` on rank fields (T2) | electric-skill/PROBE-EVIDENCE.md:39-70 · SKILL L16 |
| L4 · `m/latest` is demand-driven [measured] | the combine fn re-runs only when an *input* emits, never because it was sampled — a rAF `request!` inside it fires once and the loop dies (regression `claim-03`: 1 call over 5 samples) | T4: uploads and scheduling live at the reduce/consumer edge, never in a flow | VERDICTS.md:118-140 · scene_runtime.cljs:9-11 |
| L6 · coarse invalidation [measured] | one wide `m/latest` re-runs its whole combine on any input — the editor's 18-input combine had the caret blink as an input, so every 530 ms blink re-shaped the document: 1440 ms long-task per blink on a large file | split stable → overlay *chains*; the blink only selects paint; one `m/signal` at the sharing point | HACKS-LEDGER.md:128 · SEAM-STEP1-CONTRACT-HANDOFF.md:143-165 |
| L8 · diamonds glitch [measured] | two `m/latest` chains off one store combined downstream emit torn frames deterministically (4/4 runs) — no atomic settle at the raw-Missionary layer | T3: co-varying values derive in ONE `m/latest`; the world snapshot combines single-source flows once | SKILL L8 · scene-substrate/CONTRACT.md:181 |
| L1 · "Watch cancelled" [measured] | multiple `m/?<` over `m/watch` nested in one `m/ap` crash on the first change | T9: one watch, filter below it — never per-slot subscriptions forked off the store watch | SKILL L1 · CONTRACT.md:200 |

```
DOM target — what Electric's applier assumes

        permutation: insertBefore(C, A) = MOVE the node
        +------------------------------------------+
        v                                          |
     +-----+   +-----+   +-----+                   |
     |  A  |   |  B  |   |  C  |-------------------+
     +-----+   +-----+   +-----+
        becomes
     +-----+   +-----+   +-----+
     |  C  |   |  A  |   |  B  |
     +-----+   +-----+   +-----+
     same three nodes; identity travels with the handle.
     Allocation count: 0.

---------------------------------------------------------------------------------------------

GPU slot pool — what the engine actually has

     +--------+ +--------+ +--------+   bytes sit still; "move" = rewrite an index
     | slot 0 | | slot 1 | | slot 2 |
     +--------+ +--------+ +--------+

     the borrowed bridge heard "insert" and allocated a fresh slot each time:
     [ ][ ][ ][ ][ ][ ][ ][ ]  … 16,750 slots

     for 100 entities, after 91 rotate frames; 160 ms/frame.
     The diffs were right. The applier's contract was the DOM's.
     buffer_pool.cljs:449 — "DO NOT WIRE AS-IS … consume the six diff ops directly"
```

**Figure 5 — the one difference that broke the borrowed applier.** "Borrowed algebra, never borrowed appliers" is the seam ruling's phrase for this picture: take the diff vocabulary, write your own applier whose "move" means what a GPU pool means by it.

### 7c · Electric as the language faces are written in — ruled out

> Face as data yess all way
>
> — Sid, vision/LOG.md 2026-08-12; decisions.md: "faces are data all the way — a face's authored form is material for every inhabitant, human and agent, never source code"

So `e/defn`-composed faces are dead either way. What stays open is machinery: which host *executes* a face spec — an Electric generic host or a Missionary host — judged by one real face built both ways (the Block), on container close/reopen, mid-drag teardown, served-source hot-swap. An engineering verdict, not a product fork.

> **So, to your sentence "we have the principles and the instances to do it all":** the principles — yes, and the engine's keyed layers already embody them. The instance — Electric's DOM applier and its collection machinery — measured against a GPU pool, does not fit; and the reactive primitives underneath it (`m/latest`) have exact semantics that produced three of the scars in §10 when used "ambiently". The code is the result of learning that twice. What is missing is not more reactivity inside the renderer; it is (a) the keyed wire above it, and (b) a way of declaring the engine's dataflow that isn't seven hand-wired sites per view.

## 8 · The gap, filled

§7a named the gap as it stood in the last living host. The waist cut (closed 2026-08-21) changed it, and the electric-native road (`history/docs/electric-native/DIRECTION.md`, settled 08-12/13) was written *before* that cut — so first the gap as it is today, then the fill, then what the cut did to the road's order.

### The gap, as of today — three layers

| Layer | What is true now | Receipt |
| --- | --- | --- |
| G0 · there is no arrow down | No client host, no product bundle, Electric out of `deps.edn` (only Missionary remains). The server still serves: twelve face data-contexts (`face_projection/serve`, EDN pages), the SSE episode door (reply streams back on the POST — no GET wire survives), birth/geometry EDN routes, the ten-route write surface. Nothing consumes any of it. The engine is a machine with no crank. | deps.edn:4 · editing-waist-map.md:399 · kept-code-map.md §3b |
| G1 · the old arrow had the wrong shape | Whole data-context page re-pulled per epoch bump (~1/s); 24 mailbox atoms + 21 mirrors; one global epoch integer that geometry settles never bump; then `derive-store-frame` rebuilt every lane. One edit → one page → every slot re-derived. "Keyed and act-linked at rest, anonymous in motion." | PROBLEM-SPACE.md:58-60 · RECON.md §11 · scene_store.cljc:321 |
| G2 · the view never told the server what it needed | Demand was "the page". The server had no notion of what a lens is looking at; residency scaled with what you own, not what you see. | DIRECTION.md "2 · The engine drives Electric" |

Both ends of the gap are **already keyed**. Rama rows carry `unit-id` and `event-id` on every defrecord; `RevisionRow` history is indexed by time-prefixed order-keys with a cursor-range reader (`runtime.clj:363`) — "the since-reader is one function". The scene store is `{:slots {vi → slot} :index {address → #{vi}}}`, patched by key, never rescanned; the pool bridge already has `keyed-diff-update-pool!` — "the Missionary-side equivalent of what e/for-by would do" (`buffer_pool.cljs:214`). The middle was anonymous; now the middle is absent. That is the whole gap: *the wire between two keyed ends, in both directions.*

### The fill — the shape is settled, the courier is probe-decided

In the problem-space doc's words: **demand up hierarchical (membership) · delivery down flat and keyed · applied atomically per accepted act.** Here is the middle, before and after:

```
BEFORE (deleted) — anonymous in motion
  [ Rama — keyed rows ]
   |  whole data-context page, per epoch bump (~1/s)
   v  [ Electric as RPC · 24 mailbox atoms ]
   |  add-watch mirrors ×21
   v  [ reconcile the whole page → upsert every slot ]
   v  [ derive-store-frame — every lane, every swap ]
   v  [ keyed pools → GPU ]
  one edit → one page → every slot re-derived
  global epoch integer; geometry settles bump nothing
  demand = "the page"
---------------------------------------------------------------------------------------------
AFTER — keyed in motion, demand-scoped
  [ Rama — keyed rows (unit-id · event-id) ]
   |  since-reader over RevisionRow · settles notify
   v  [ the keyed feed, per lens
        {:gen act-id :units {id → row | tombstone}
        :order {cid → ranks} :members {cid → #{id}}} ]
   |  courier: SSE now · e/diff-by at the host probe
   v  [ client host: one flow → m/reduce edge (T4) ]
   |  upsert-slot / remove-slot BY KEY (exists)
   v  [ scene store — patched by key (exists) ]
   |  per-key views (the 34-watch collapse)
   v  [ ordered pool diff (exists; keyed twin unwired) → GPU ]
---------------------------------------------------------------------------------------------
  [ renderer publishes visibility facts ]
   |  what the lens is looking at
   v  [ demand = membership
        broker: visible containers/units
        → grow/shrink the subscription ]
   +---^ [ the keyed feed, per lens ]
  [ acts
    edit · settle → the write arms (+1 block-edit arm)
    → Rama → accepted → the feed (echo = feed) ]
   |  correlation = act-id; caps dissolve
   +---^ [ Rama — keyed rows (unit-id · event-id) ]

  one edit → one unit → one slot; a pan sends nothing; a second lens sees the move
  generation = the accepted act · tombstones linger until every lens passes
  catch-up rides the live path from a watermark — never a second reconcile
```

**Figure 6 — the middle, before and after.** Left: the deleted shape. Right: the settled shape ("demand up hierarchical, delivery down flat and keyed, applied atomically per accepted act"). Boxes marked "exists" are already in the tree; the feed, the courier, the client host, the broker, and the per-key views are the fill.

### The ten hops, after

| # | Hop (from §3) | After | Exists? |
| --- | --- | --- | --- |
| 1 | Rama serves a page | Rama rows → a **since-reader** over RevisionRow (time-prefixed order-keys, watermark cursor) → changes since *W*; geometry settles gain their missing notify | rows + cursor reader exist (text only); the since-watermark fn and the geometry bump do not; geometry has NO time-ordered history (cells overwrite in place) and no push exists — so the producer is notice → re-shape → diff-against-shadow first, the change-log PState at atom 3 (CONTRACT §9.3) |
| 2 | mirror atom | **dies** — the feed delivers units by id; no mailbox, no mirror | — |
| 3 | consumer edge | the **client host**: one flow over the courier → one `m/reduce` edge → per key `ss/upsert-slot` / `remove-slot`; the face tree per *unit* from the served assembly (36 builders now, no per-unit builder — `build-face-tree` over a one-unit data-context + one block face as data) | store fns exist; the host (five small namespaces) does not |
| 4 | the store swap | unchanged — already keyed, index never rescanned, tape patched by key | ✓ |
| 5 | `<store-frame` rebuilds all lanes | **per-key views**: the store contract's slice — per-key reads, write-site dispatch, signals at sharing points; `derive-store-frame` demotes to oracle with a fence | does not; this is the store-contract slice the seam ruling names |
| 6–7 | world snapshot · frame edge | unchanged — a host must own the rAF sample again (one line in the old host) | pattern exists (deleted); trivially rebuilt |
| 8 | uploads | unchanged — `ordered-diff-update-pool!` is the receiver (the per-vi offset math in `store-pool-entries` is ORDERED); `keyed-diff-update-pool!` is implemented but unwired and would break those offsets — the keyed swap is the store-contract slice's | ✓ (uncalled) |
| 9–10 | draw-frame! · encode | unchanged — already per family, keyed, with the oracle twin | ✓ |
| ↑ | pointer → acts | frame-rate stays local (as today); settles go through the geometry route *and notify*; edits go through the write routes; the echo is the feed, correlated by act-id | routes exist; notify does not |
| ↑↑ | (new) demand | the renderer's visibility facts → a **broker** → the lens's membership subscription (grow/shrink container sets, never position windows — L16); the feed is scoped by it | does not |

### What the cut did to the road's order

DIRECTION.md's road 1 runs 1a (typing stops lagging) → 1b (the screen maintains itself from one source) → 1c (edits travel by id). It assumed a living product: "ordinary blocks render through the engine today." They do not, since 2026-08-20. There is no typing to un-lag and no screen to maintain until an arrow exists. So the order inverts: **the arrow (1c's shape) is the first act**, because it is also the host; 1b rides it (the store is already the one source); 1a's shaping receipt is checked on the first real typing the new host carries. This is a forest call on ground you already changed by accepting the cut — stated here as a position, not landed anywhere.

### The one fork, and a position

**The courier.** The settled wording says `e/diff-by :unit-id` server-side / `e/for-by` client-side; the problem-space doc classifies feed transport as *probe-decided* ("Electric keyed machinery vs owned Missionary lanes"); and the tree no longer has Electric. POSITION (held, not pending): the wire's *content* is courier-agnostic (flat units-by-id + order/membership indexes + generation per act + tombstones + watermark), so build that content first over the cheapest courier that exists — the kept SSE door grown a long-lived GET stream, Missionary on the client — and let Electric's keyed machinery take its fair test at the host probe, where it returns anyway (the Block built both ways). The replacement trigger is named so this is a stage, not a patch: the transfer bench (PROBLEM-SPACE probe 1 — four algebras × `diff-by` losslessness × two-edits-one-tick). Risk of this position, stated: an owned lane can entrench; the fence is that the feed's content format is frozen in the contract and the courier is a one-file swap. Counter-position, equally lawful: re-add Electric now and ride `e/diff-by` from day one — pays the dependency earlier, gets reactive consistency for free, and makes the host probe real sooner; its cost is L16's producer-side comparison-minting (already accepted as the named first stage).

**AMENDED 2026-08-30 — the fork closed from outside.** Hyperfiddle left Clojure (Electric v4 = a JavaScript-embedded language + a low-level kernel compiled to WebAssembly + a portable WASM server runtime; v3 = a frozen alpha under a proprietary license). The Electric arm of this fork and the counter-position above are struck; the owned courier is the position, the frozen content format stays the fence, and the transfer bench stays as a test of our own feed. Electric's source remains a reading reference for the shape of keyed diffs (electric-docs skill §0). Sid, 2026-08-30: "for existing electric i think they can be used as reference on how to do things reactive way in missionary and clojure so that is useful part imo … i think we have move past that we are going to copy electric blindly because its for dom ours is multi-engine softland target".

### The smallest slice that proves the fill

- **One conversation container, keyed, end to end.** A keyed feed for one container over a long-lived stream; a client host of three namespaces (courier flow · apply edge · demand publisher); the store; the verifier-grade draw path; one edit through a write route; the feed echoing it; the store patched by key; the frame drawing only the changed family.
- **Three tripwires, no more:** one edit moves exactly one slot (store diff = 1 key; changed-families = 1); a pan sends zero bytes up and re-derives zero slots; a second lens on the same container sees a geometry settle without an unrelated bump.
- **What it retires on landing:** the whole-page pull as truth (it stays as the oracle, with its two named duties: committed-echo cross-check, cap-overflow reconcile), the epoch integer, the mirror pattern.

> This section is a landed position, not a contract — contest it, and keep filling (Sid, 2026-08-23: "just keep writing and filling in the gaps .. instead of waiting for me"). CUT 2026-08-23: `history/docs/electric-native/CONTRACT.md` (scope · refusals · laws · exact entry points · five scenarios · MUST-NOTs · the five-things table · three atoms · the atom-1 Codex starter); the courier position above is its §9.1 with the transfer bench as the named trigger; the Block probe stays the named instance that decides the host. Gatherer findings at the cut repaired this section in place (36 builders · ordered pools · no wire edit route · geometry has no time-ordered history).

## 9 · What is wasteful, and what is irreducible

### Irreducible — the organs a GPU UI cannot not have

- a retained scene value (store) — the DOM you no longer have
- an order (tape): paint forward, pick reverse
- layout + a shaper — text is the heavy one, by far
- one painter per drawable kind (families) → bytes + pipeline
- GPU residency: pools, atlases, a budget
- a pick over the scene
- a frame edge: sample once, skip if unchanged, upload, draw
- session-only atoms on their own clock
- a truth loop through the server for durable acts

### Wasteful or over-built — with the receipt

- **Double roads by law.** The batch tape compiler runs beside the maintained arrangement every frame as its oracle (`renderer.cljs:3545,3548`); `maintain-frame-plan` and `maintain-effect-spans` exist with test-only callers; dirty-present (persistent target + scissor + copy) is "fully implemented but OFF" (`PRIMITIVES.md:99`); `gpu-mount` is banned and kept. Defensible while the incremental road is unproven; real weight all the same.
- **Accretion.** Six dead ui-primitive components; three parallel clip implementations; the 0.56 advance constant at ~30 sites; adding a view touches ~7 hand-wired sites with curated watch sets (`render-substrate-retro/RETRO.md:231-249`).
- **Ten dialects of "watch".** `e/watch` 13 · `add-watch` 21 · `m/watch` 9 · `m/latest` 11 · `m/reduce` 9 · `m/ap` 9 · `m/observe` 9 · `m/relieve` 4 · `m/eduction` 3 · `m/sample` 1 — plus 164 raw `swap!/reset!` in `ground.cljs` alone.
- **A verifier bigger than the renderer.** `verifier.cljs` 297 KB vs `renderer.cljs` 198 KB — and it is the area's only live entry.
- **Unproven claims kept as if proven.** FRAME-RETENTION has no acceptance; its closing receipt's SHA does not match; "speed receipts are not correctness receipts" (`PERF-DOSSIER:452-522`).
- **The whole-population step that remains.** `derive-store-frame` rebuilds every lane on any swap (`scene_store.cljc:321-379`) — the O(N) root the electric-native problem-space names ("neither one-atom-deriving-everything nor per-component sockets").
- **The 16-builder presentation grammar** kept only for TrailView/anatomy/goldens — custody debt you already named (08-20).
- **Region3D at ~200 KB** — in your words on the dossier: "that is not product that is eng."

The honest summary: roughly half the bulk is organs (irreducible for this substrate), a quarter is the second road kept alive on purpose, and a quarter is accretion from many atom sessions each adding a view by hand. The parked engine is the before-picture of the settled direction, with its hand-wired middle still standing.

## 10 · The scars — why several shapes are the shape they are

Most of the "why is it built so" questions have a measured failure behind them. Each row: the disease, the mechanism in plain words, the fix that gave the code its current shape.

| Disease | Measured | Mechanism | Shape it left in the code | source |
| --- | --- | --- | --- | --- |
| cold settle / the 28 s frame | 35.678 s settle; one 28.7 s text interval; 6 s hover stalls (×3) | shaping filtered whole-glyph per cluster (~O(C×G)); and the host emitted ops without the layout, so the renderer re-derived shaped layout per op | one material-local layout authority under a declared layout key; shaped block-greedy wrap as the one break-choosing truth (the shaper = "the floor") | shaping-correction/CONTRACT.md:62-121 |
| per-frame tape recompile | structural (no ms banked) | the tape compiled + validated + sorted every rAF, keyed on the frame index | the store gained a sorted `:ordered` map patched at writes; the batch compiler demoted to oracle behind a fence | SEAM-STEP1-CONTRACT.md:33-36 |
| per-event pick recompile | structural | pick recompiled an unchanged world per event | `ss/pick` walks `(rseq (:ordered store))` | SEAM-STEP1-CONTRACT.md:36 |
| frame counter as ancestor | structural | world revision was literally `[:frame idx]` — the clock sat above derivation | `forbidden-declared-inputs` throws at load for frame-idx/pan/zoom/pixel-size | frame_inputs.cljc:25,91 |
| caret-blink reshape | 1440 ms long-task every 530 ms | the blink was an input of an 18-input `m/latest`; every blink re-ran full layout of a document the caret never touched — cost in Missionary propagation outside rAF, so rAF counters looked fine | stable → overlay chains; overlay only selects paint; `m/signal` at the one sharing point | HACKS-LEDGER.md:128 · HANDOFF:143-165 |
| the camera watch | produce 35–58 ms, 8–26 fps (iPhone) → produce 0.1 ms, comparator-calls 0, 45–60 fps | `!camera` was an input of the world snapshot, so every pan re-derived the scene | camera deref'd at the rAF sink only; pick takes `{:world :screen}` so the camera→registry edge lost its reason to exist | PERF-DOSSIER:179-211 · adc30c9^:render.cljs:243 |
| the borrowed mount | 16,750 slots / 100 entities / 91 frames | DOM "move" ≠ pool "move" (Figure 5) | store consumes diff ops directly; `gpu-mount` banned | buffer_pool.cljs:449 |
| open · Region3D shape-mint | 1974 comparator calls for 1 produced entry; 12–20 fps | a zoom crosses a lease rung → shape key changes → whole arrangement + plan rebuild for a 1-entry delta (dossier's own tag: hypothesis) | not fixed — the highest-value open probe | PERF-DOSSIER:212-295 |
| open · boot | 10.1 s slow reconcile; ~60 consecutive buffer reallocs from 13 KB | untraced ("reallocation thrash — hypothesis") | not fixed | PERF-DOSSIER:401-449 |

Every one of these is the read side paying to rediscover what the write side already knew — the one sentence the render-seam ruling is built on. It is also the strongest argument against "just make everything a reactive value": three of the scars (blink, camera, diamonds) were *caused* by exactly that, done ambiently.

## 11 · Sources, and what is not settled

### Read primary for this page

- `docs/decisions.md` §"One render substrate" (:302) and §"The render seam" (:450-628) — the binding law; `history/docs/scene-substrate/CONTRACT.md` §7 traps T1–T11; `vision/LOG.md` 2026-08-03 and 2026-08-12 entries (Sid's words, verbatim).
- Code, by seam (gatherers, anchors spot-checked): `scene_store.cljc` :97-446 · `scene_tape.cljc` :641-768 · `scene_runtime.cljs` :1-470 · `rect_tree.cljc` :640-693 · `containers.cljc` :185-277 · `frame_inputs.cljc` · `frame_delta.cljc` · `frame_scheduler.cljc` · `frame_semantic_state.cljc` · `renderer.cljs` windows at :2147, :2696-2748, :3057, :3392-3397, :3423-3545, :3635-3922 · `buffer_pool.cljs` :111-493 · deleted host at `adc30c9^`: `electric_flow.cljc`, `runtime/render.cljs`, `runtime.cljs`, `events.cljs`, `mouse.cljs`, `state.cljs`, `face_wiring.cljs` :700, `block_edit_wiring.cljs`, `ground.cljs` windows only.
- Receipts: `history/docs/electric-skill/VERDICTS.md`, `PROBE-EVIDENCE.md`, `HACKS-LEDGER.md`; `history/docs/render-engine/PERF-DOSSIER-2026-08-09.md`; `history/docs/shaping-correction/CONTRACT.md`; `history/docs/render-substrate-retro/RETRO.md`, `PRIMITIVES.md`; `history/docs/electric-native/DIRECTION.md`, `PROBLEM-SPACE.md`, `RECON.md`; the electric-docs skill §2 laws.

### Not settled — structure claimed, magnitude not measured

- [measured 2026-08-23, JVM] pick cost — see §4 "Cost shape": O(N) entry-vector rebuild per pick + short-circuiting walk; 0.28–0.45 ms at 2000 slots. Browser magnitude still owed: `performance.now()` around `ss/pick` in the host (CONTRACT atom 2 carries the counter).
- [measured 2026-08-23, JVM] `derive-store-frame` whole-lane rebuild per swap — 0.17 / 1.41 / 16.66 ms at 10 / 200 / 2000 slots (linear, 11.8× per 10×) against a flat ~0.3 ms `upsert-slot`: **54× amplification at 2000 slots — a dropped frame per edit**. This is the store-contract slice's numbered trigger (CONTRACT §2, §10 c). Receipt: `history/docs/electric-native/receipts/2026-08-23-jvm-bench/`.
- [resolved 2026-08-23] the verifier build never reaches `draw-frame!`: `start!` → `run-verifier!` → `run-w4-frame-runtime!` → `w4-capture!` → `compositor-gpu/draw-multipass!` directly (`verifier.cljs:5667, 5465, 3635, 2946`); one-shot, no rAF. `draw-frame!` has zero callers in `src/` — the new host is its first (CONTRACT §10 a). Build cmd: `clj -M:dev -m shadow.cljs.devtools.cli release render-verifier`.
- [one receipt] "the mutable-tree applier lesson, proven twice" — one proof on file (gpu-mount); the second is uncited.
- [fenced] L16 numbers: raw incseq, snapshot −44 vs claims at −45; "do not credit past that".
- [not read] `region3d_scene.cljc` pick internals; family narrow-phase predicates (path/connector) — could be hot per candidate; `editing_runtime.cljs` atoms.

Sibling pages: `docs/below-the-waist/kept-code-map.html` (the notch above — pieces); `render-engine-map.html` (the same area by LAYER, a parallel session, same day — its evidence in `render-engine-gather/README.md`); `editing-waist-map.html` (what was cut). Landed under `docs/below-the-waist/` at Sid's word, 2026-08-23 ("land the docs — how else would this be contested?"); twin `engine-two-arrows.md`, edited together. Contestable by construction: every claim carries an anchor; findings go to the next fresh-eyes round and repair in place.
