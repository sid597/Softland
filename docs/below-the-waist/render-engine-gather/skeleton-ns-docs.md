## app.client.substrate.chrome-derive
file: src/app/client/substrate/chrome_derive.cljc

Pure maintained chrome slots, independent full-recompute oracle, and live
   handle-pick providers. Runtime owns atoms and store mutation; this namespace
   owns the data transition laws and proportionality receipts.

## app.client.substrate.chrome-material
file: src/app/client/substrate/chrome_material.cljc

Pure grammar and hybrid anchor/metric geometry for render-family chrome.

   Vertex anchors are container-local. Pixel offsets are deliberately separate
   and are applied after the container affine and world camera by chrome-gpu.

## app.client.substrate.connector-material
file: src/app/client/substrate/connector_material.cljc

Pure citizenship and Contract-G truth for durable reference connectors.

   A connector material is the fail-closed merge of a relation-row projection
   and client-session dress. Resolved routes and meshes are disposable readers;
   none of their coordinates become relation truth.

## app.client.substrate.connector-route
file: src/app/client/substrate/connector_route.cljc

Pure attachment, routing, label-layout, and bounded connector cache.

   Cache state is explicit state-in/state-out. The only ambient value is the
   product pick mirror, which holds disposable route projections and a provider
   for the current effective-transform map; it never enters scene-store data.

## app.client.substrate.frame-delta
file: src/app/client/substrate/frame_delta.cljc

Small render-seam delta values.

   Deltas are minted by the owner that already knows the changed key.  This
   namespace classifies and shapes those values; it never scans a frame to
   discover that something changed.

## app.client.substrate.frame-effect-view
file: src/app/client/substrate/frame_effect_view.cljc

Incrementally maintained effect membership and execution-event view.

   Durable state is keyed by semantic entry/container ids.  Numeric entry
   ranges are projected only for the compositor's forward walk.

## app.client.substrate.frame-effects
file: src/app/client/substrate/frame_effects.cljc

Pure W4 container-effect grammar and span derivation.

   Effects are session/arrangement facts. They never enter scene-store
   derivation. Keyed inputs are the container registry's effect declarations
   and the ordered arrangement's semantic order tokens; mutation enters through
   set-effects!; this namespace owns validation, effect-chain projection, and
   the independent CPU color oracle. The maintained door re-derives only
   changed containers while derive-effect-spans is its full-recompute oracle.

## app.client.substrate.frame-graph
file: src/app/client/substrate/frame_graph.cljc

Pure W4 frame-plan compiler.

   Render-seam declarations: keyed inputs are effect-bearing container topology
   (ids, nesting, normalized values), enabled regions, capabilities, viewport
   resource shape, and color mode; doors are registry/effect/region/viewport/
   capability changes;
   this namespace owns pass structure and fresh per-frame range binding;
   projections are validated executor data, replay hash, and export plan; the
   independent oracle is oracle-compile-frame-plan, which never calls the
   maintained structure compiler. Structure reuse never retains entry indices.

## app.client.substrate.frame-inputs
file: src/app/client/substrate/frame_inputs.cljc

Declared semantic inputs and receipts for the retained frame edge.

   This namespace is deliberately pure-loadable: it knows no DOM or GPU.  The
   renderer supplies current values; this namespace owns the declaration
   fence, change ladder, camera quantization doors, and the live ledger.

## app.client.substrate.frame-plan-view
file: src/app/client/substrate/frame_plan_view.cljc

Keyed, incrementally maintained frame-plan fragments.

   The durable order is a sorted subview of stable pass ids.  Dense numeric
   ranks exist only in the executor projection and are never plan identity.

## app.client.substrate.frame-scheduler
file: src/app/client/substrate/frame_scheduler.cljc

Pure W4 cause scheduler plus the session deadline registry.

   The injected time value is consumed only by decide and sink-side helpers.
   It is never a scene/store derivation input. Runtime supplies the timestamp
   already carried by its frame pulse; replay rebinds the source to recorded
   logical time.

## app.client.substrate.frame-semantic-state
file: src/app/client/substrate/frame_semantic_state.cljc

One generation authority for arrangement, effect view, and plan view.

## app.client.substrate.image-material
file: src/app/client/substrate/image_material.cljc

Pure material, geometry, allocation, and packing laws for the image atom.

   This namespace owns no GPU objects and emits no scene-tape entries.  Source
   bytes resolve only through the digest-keyed registry below; renderer code is
   a consumer of these values, never a second material authority.

## app.client.substrate.path-material
file: src/app/client/substrate/path_material.cljc

Pure grammar, Contract-G truth, cache identity, normalization, and packing
   laws for the path atom. GPU meshes are disposable projections of this
   namespace's centerline/contour authority; no renderer state lives here.

## app.client.substrate.path-tessellation
file: src/app/client/substrate/path_tessellation.cljc

Deterministic, pure `.cljc` tessellation for path materials.

   Ink expands directly to segment quads plus round cap/join fans. Shapes use
   explicit hole bridging followed by ear clipping. Neither road promotes its
   mesh to material truth or routes through a JS triangulation dependency.

## app.client.substrate.region3d-evaluation
file: src/app/client/substrate/region3d_evaluation.cljc

Retained Region3D evaluation across authoritative material and transient
  session transforms. Transform dirtiness is a component, not a material
  invalidation: topology/material changes derive; transform changes maintain.

## app.client.substrate.region3d-material
file: src/app/client/substrate/region3d_material.cljc

Pure, fail-closed material grammars for Region3D.

   Region rows are ordinary EDN scene-store payloads. This namespace owns
   versioned defaults, canonical validation, semantic edit values, and the
   Contract-M citizenship descriptors. GPU resources and session state never
   enter this grammar. Unknown fields are preserved so later sculpting and
   node-authoring extensions can enter without silently losing meaning.

## app.client.substrate.region3d-placement
file: src/app/client/substrate/region3d_placement.cljc

Pure Region3D placement derivations.

   This namespace owns material-reference resolution, flat-plane math, text
   and ink packing, placed pick readers, and region-object anchor projection.
   Store values and explicit cache values enter as data; there is no ambient
   state and no execution clock.

## app.client.substrate.region3d-scene
file: src/app/client/substrate/region3d_scene.cljc

Pure Region3D scene derivation.

   Keyed inputs: a canonical region row plus session camera/selection/gizmo
   values joined at the renderer edge. Door: event causes only. Ownership:
   the region row is one generation; session camera is a separate stamped
   generation. Projections: instance rows, BVH/ray pick, tape entry, pass
   fragment, inspector rows, and the full batch oracle. The maintained view
   keeps the full derivation as its equivalence fence and refits only the
   affected hierarchy subtree for transform edits. No clock exists here.

## app.client.substrate.region-rungs
file: src/app/client/substrate/region_rungs.cljc

(no docstring)

## app.client.substrate.scene-tape
file: src/app/client/substrate/scene_tape.cljc

W2-B's pure render contract and ordered-scene-tape core.

   Family registration owns citizenship, geometry, and color declarations.
   Scene entries own semantic order.  Paint and pick are projections of the
   same compiled tape: paint walks forward, pick walks exact reverse.  This
   namespace is data-only and has no GPU objects, atoms, renderer imports, or
   family-specific ordering branches.

## app.client.substrate.snap
file: src/app/client/substrate/snap.cljc

Pure world-space object snapping and smart-guide derivation.

   The applied delta, alignment set, guide geometry, and equal-gap ticks are
   minted by one gesture-step result so paint cannot advertise a snap that the
   arrangement did not actually take.

## app.client.substrate.webgpu.buffer-pool
file: src/app/client/substrate/webgpu/buffer_pool.cljs

Slot-based GPU buffer pool for differential rendering.
   Default: 29 words (116 bytes) per rect — 28 floats + container u32
   (scene-substrate P2). Configurable for other item types (e.g. shadows:
   21 words, 84 bytes) via :floats-per-item and :pack-fn.
   Supports per-slot updates via writeBuffer for O(1) partial writes,
   and batch-update with diff for O(changed) bulk sync.

## app.client.substrate.webgpu.chrome-gpu
file: src/app/client/substrate/webgpu/chrome_gpu.cljs

WebGPU projection for hybrid-metric selection/manipulation chrome. One
   private interleaved vertex buffer is repacked only when the chrome mesh set
   changes; camera and followed-container motion remain shader values.

## app.client.substrate.webgpu.compositor-gpu
file: src/app/client/substrate/webgpu/compositor_gpu.cljs

W4's generic WebGPU compositor capability.

   The frame graph names resources and producer edges; this namespace owns the
   recycling target pool, linear group/mask/blur composition, exactly-one
   presentation transfer, per-draw scissor state, and asynchronous raster
   readback. Family pipelines arrive through a lazy variant-layer builder so
   textures/registries/instance bytes remain owned by their existing systems.

## app.client.substrate.webgpu.connector-gpu
file: src/app/client/substrate/webgpu/connector_gpu.cljs

WebGPU upload and text-geo custody for connector route projections.

   Route resolution and mesh caching remain pure in connector-route. This
   namespace owns only the packed vertex buffer, upload identity gate, and one
   label geo cloned from the live content text system.

## app.client.substrate.webgpu.gpu-budget
file: src/app/client/substrate/webgpu/gpu_budget.cljs

Console-first GPU resource tracker for WebGPU buffers/textures.
   Tracks reserved bytes by subsystem, plus optional active bytes where the
   runtime knows how much of a reservation is currently populated.

## app.client.substrate.webgpu.path-gpu
file: src/app/client/substrate/webgpu/path_gpu.cljs

WebGPU projection for the path family. The system owns one repacked vertex
   lane and a content/version/regime mesh cache. Uploads occur only when the
   mesh-set identity or zoom regime changes; camera/container motion remains a
   shader value.

## app.client.substrate.webgpu.region3d-gpu
file: src/app/client/substrate/webgpu/region3d_gpu.cljs

Atom A's WebGPU region family.

   `prepare-region3d-frame!` is the only CPU->GPU upload door and never mints
   an encoder. `encode-region-passes!` is the compositor-owned plan producer;
   `execute-region3d-batch!` is the ordinary scene-tape composite executor.
   Region attachment bytes remain compositor target-pool leases throughout.

## app.client.substrate.webgpu.region3d-placement-gpu
file: src/app/client/substrate/webgpu/region3d_placement_gpu.cljs

WebGPU packing and paint for Region3D text/ink placements.

   Packing is content/session keyed and owns no source material, atlas, or
   tessellation authority. The caller threads the path cache value through and
   supplies the current text-system atlas handles for each frame.

## app.client.substrate.webgpu.region-bindings
file: src/app/client/substrate/webgpu/region_bindings.cljs

Device-local Region3D binding owner.

   Semantic ids address desired leases and stable composite slots.  GPU lease
   objects stay here, outside frame semantic state, and are stamped by the
   compositor identity epoch.

## app.client.substrate.webgpu.renderer
file: src/app/client/substrate/webgpu/renderer.cljs

(no docstring)

## app.client.substrate.webgpu.verifier
file: src/app/client/substrate/webgpu/verifier.cljs

Permanent W0-A browser half.

   This harness deliberately instantiates the production renderer's public
   pipeline/update functions and repository font assets. It owns only capture,
   comparison inputs, a candidate geometry-contract probe, and receipts. It is
   not a product renderer and it never substitutes lookalike WGSL.

   The CPU point-in-path probe is NOT today's product picking path. Product
   picking remains axis-aligned rect-tree bounds; the receipt carries an
  explicit rounded-corner divergence sentinel so those truths cannot collapse.

## app.client.workspace.chrome-runtime
file: src/app/client/workspace/chrome_runtime.cljs

Flag-boot wiring for CHROME. The namespace is load-pure: boot installs the
   two verb stranglers, selection clear/tap hooks, live providers, and the
   session-only slot lifecycle.

## app.client.workspace.containers
file: src/app/client/workspace/containers.cljc

Pure container registry and W2-A affine composition.

   A container owns one canonical six-number affine in SVG/CSS order:
   [a b c d tx ty], where x' = a*x + c*y + tx and
   y' = b*x + d*y + ty.  Container transforms compose through the parent
   chain once; the resulting value is shared by GPU paint, CPU pick, bounds,
   clipping/culling projections, and context receipts.

   Semantic container ids never index the GPU table.  Each live container has
   a compact, stable :transport-slot.  That indirection is the Q8 transport
   seam: sparse material ids do not amplify the 32-byte storage table.  cid 0
   and transport slot 0 are permanently reserved for the identity world
   container.

## app.client.workspace.face-assembly
file: src/app/client/workspace/face_assembly.cljc

Framework Wave 1, lane A — the two-stage assembly interpreter.

   A *face* is an arrangement of the land's UI vocabulary described as pure EDN
   data (an *assembly*). This namespace is the pure interpreter that turns an
   assembly + a data context into an rt-node tree, through a *registry* of
   primitive builders. Faces are DATA, so what renders them is an INTERPRETER —
   never code generation into the compiled substrate (CONTRACT §3 seam law;
   D-011 middle regime).

   Two-stage law (CONTRACT §5):
     (compile-assembly registry assembly-edn) -> compiled   ; WEAR-TIME, once
     (apply-assembly   compiled data view-ctx) -> rt-tree    ; DATA-CHANGE-TIME
   Frame-time does nothing new: the existing RAF sample + `identical?` skip +
   keyed pool diff consume the produced scene like any pane. Interpretation cost
   lives on the data-change path, never the frame path (trap T3).

   Both functions are TOTAL — they NEVER throw. A malformed assembly, an unknown
   primitive, a corrupted registry, or a primitive that throws all resolve to a
   renderable ERROR-CARD rt-node carrying the assembly name, grammar version,
   address, and the first <=5 validation errors. The render path has no `try`
   upstream (L13: `try` is TODO inside e/defn), so one bad AI-authored assembly
   must land as a visible card beside the chat, never as a black screen (trap
   T4). The error-card constructor is built into THIS namespace and is
   registry-independent — a corrupted registry cannot take the error path down
   with it.

   Grammar v0 — five keys, one guard (CONTRACT §4):
     prim node  {:prim <kw> :props <map>? :children [<node>...]?}
     each node  {:each [<kw>...] :template <node>}   ; iterate seq at path,
                                                       descend context per item
     bind ref   {:bind [<kw>...]}                     ; legal ONLY as a prop
                                                       value (not nested)
   THE GUARD (V4, trap T2): the assembly form contains no list and no symbol
   anywhere — keywords, strings, numbers, booleans, nil, vectors, maps only.
   Arrangement only, no logic ever. Anything that wants to be a program gets to
   be a real one, in the code lane (primitives §6, projections §7).

   `.cljc` so the golden harness runs JVM-side with no browser — the block-kernel
   goldens discipline applied to UI. Depends only on `rect_tree` (also cljc).

## app.client.workspace.face-primitives
file: src/app/client/workspace/face_primitives.cljc

Framework Wave 1 — lane W1-B: the primitive VOCABULARY (CONTRACT §6).

   A `face` is pure-EDN arrangement (an assembly) walked by lane A's interpreter
   over a REGISTRY of primitive builders. This ns is that registry, assembled as
   a plain value (`registry`, at the bottom) — no global mutable state; tests
   pass their own map (§6).

   Three kinds of entry live here:

   1. HARVESTED BUILDERS — extraction, not invention (§6 / the
      copy-then-harmonize ruling, CONTRACT §2). The `ui-*` builders and the
      `list-*`/`typo-*`/`dt` support defs came from the old workspace's
      ui_primitives.cljs; `omission-line`/`omissions-block`/
      `hole-endpoint-card` came from the retired trail-face drawing island.
      Those origins are gone, so THIS file is the canonical home of all of
      them. Gate G7 (`face_primitives_test.clj`) pins their continued presence.

   2. §6 BUILDER-FN wrappers — the fixed interface `(fn [ctx props children]
      -> rt-node)`. The harvested builders keep their original bounds-passing
      signatures; the `*-prim` wrappers adapt the §6 interface to them, MEASURING
      each node's bounds bottom-up from its already-built children before
      returning (the measure rule, §6 / trap T7 — measure in the primitive,
      arrange in the engine). These `*-prim` fns are the registry values.

   3. The two genuinely-NEW primitives (`:text-run`, `:indent-rail`) plus the
      bare `:stack` layout node, written directly in the §6 interface.

   PURITY (G9): every fn here is pure `.cljc` returning `rect_tree` rt-nodes;
   no `js/`, no reader conditionals — the whole vocabulary loads and runs JVM-
   side. Persisted form is keyword + code-address, never fn values (trap T5, §6);
   the runtime map below binds keyword->fn at load.

## app.client.workspace.frame-runtime
file: src/app/client/workspace/frame_runtime.cljs

Flag-only W4 product join.

   Loading this namespace is pure. `boot!` installs session-only effect
   fixtures after chrome has published its selection census; `mount!` owns the
   export chord; `sync-pulse-deadline!` is the sink-edge bridge from selection
   state to the scheduler registry. No clock value enters scene derivation.

## app.client.workspace.rect-tree
file: src/app/client/workspace/rect_tree.cljc

Scene graph for nested UI.
   Everything is a rect. The tree replaces scattered compute-*-rects fns with
   one generic walk that produces flat GPU-compatible vectors.

## app.client.workspace.region3d-pointer
file: src/app/client/workspace/region3d_pointer.cljc

Pure coordinate, navigation, handle-metric, and wheel-court math for
   Region3D pointer interaction. Every screen computation consumes a point and
   viewport from one named representation; DOM/session ownership stays at the
   workspace edge.

## app.client.workspace.region3d-runtime
file: src/app/client/workspace/region3d_runtime.cljs

Flag-only Region3D focus, camera, pick, gizmo, and felt-fixture runtime.

   Loading is pure. `boot!` owns every capture listener and every fixture slot;
   the shared ground/mouse/event grammar stays byte-untouched. Session values
   join rendering through the renderer-edge snapshot and never enter store
   derivation or durable event vocabulary.

## app.client.workspace.runtime.fonts
file: src/app/client/workspace/runtime/fonts.cljs

Font manifest helpers and runtime font asset loading.

## app.client.workspace.scene-runtime
file: src/app/client/workspace/scene_runtime.cljs

scene-substrate P3a — the LIVE scene store + container registry, the
   effective-transforms + GPU-op derivations, and the face-path plurality
   wiring. The pure logic lives in scene_store.cljc / containers.cljc (JVM
   tested); THIS ns owns the ONE store atom + ONE registry atom and the
   missionary flows over them.

   Discipline:
   - Store/registry MUTATIONS happen at edges only — user actions (the
     sceneFaces window API) and the render consumer edge (the echo fan-out).
     NEVER inside an m/latest (T4).
   - The GPU upload (write-containers!) and the op merge run at the render
     m/reduce edge (render.cljs), NOT here (T4).
   - <store-frame is ONE m/latest over the store — text/rects/shadows
     co-derived together, no diamond off the store (T3).
   - In-flight container transforms are client-side 60Hz, NO events (T10);
     only a future SETTLE commits an assembly event (out of P3a scope).

   Additive by construction: with NO registered slots the store contributes
   nothing and every legacy path renders byte-identical (CONTRACT §3). This ns
   holds the EXTRA reader-face view-instances; the MAIN worn face keeps riding
   its singleton legacy path untouched, and the store fans the SAME projection
   into every extra instance on each edit echo.

## app.client.workspace.scene-store
file: src/app/client/workspace/scene_store.cljc

The one client scene store (scene-substrate P1): view-instances → resolved
   rect-trees, keyed for address fan-out, picked through per-container transforms.

   PURE .cljc: pure functions over an EDN store value, JVM- and CLJS-runnable.
   No atoms, no interop, no renderer imports. The one runtime atom that holds the
   live store, and the GPU upload, live at the reduce/consumer edge elsewhere
   (CONTRACT §5, traps T3/T4) — NOT here.

   Store value: {:slots {vi → slot} :index {address → #{vi}}
                 :ordered {[order-token entry-id] → entry}}. A slot is one
   view-instance's resolved tree + flattened ops + its address→paths subtree
   index (CONTRACT §5):
     {:vi <edn> :container <int> :tree <resolved rt-tree, container-LOCAL>
      :ops {:text [...] :rects [...] :shadows [...] :images [...] :paths [...]
            :connectors [...] :regions [...]}
      :addresses {address → #{index-path}} :meta {...} :stratum :world|:overlay}
   The :index fan-out is maintained incrementally by upsert/remove, NEVER
   recomputed by scanning slots at read time. Resolution route:
   address → :index → vis → slot :addresses → index-paths → nodes.

## app.client.workspace.selection
file: src/app/client/workspace/selection.cljc

Pure session selection truth for the flagged chrome lane.

   Selection identities are the same occurrence identities returned by scene
   picking: {:vi view-instance :address semantic-address}.  The model owns no
   atoms and has no persistence surface; callers carry state from one
   transition to the next and project only block addresses to the legacy group
   selection lane.

## app.client.workspace.text-layout
file: src/app/client/workspace/text_layout.cljc

Contract-T's single text-layout seam.

   T0 deliberately keeps the shipped monospace behavior. The value returned by
   `layout` is nevertheless the one immutable, versioned owner of wrap,
   measurement, positioned glyphs, caret stops, selection geometry, clipping,
   and hit testing. T1 replaces this provider with shaping; readers do not
   acquire another metric route.

## app.client.workspace.text-layout-planes
file: src/app/client/workspace/text_layout_planes.cljc

Columnar retained storage and derived views for Contract-T layout results.

   Raw arrays are private to this namespace.  Callers receive line/result
   handles and rich values reconstructed at the accessor boundary; no derived
   glyph or cluster map is retained in the layout result.

## app.client.workspace.text-shaper
file: src/app/client/workspace/text_shaper.cljs

HarfBuzz + Unicode-bidi provider for Contract T.

   This namespace owns font-program interpretation only. It returns glyph IDs,
   clusters, advances, offsets, extents, directions, and font provenance in
   font units. `text-layout` is the sole owner of material-space placement.

