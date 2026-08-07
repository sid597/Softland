# PACKAGE 3 — THE BLENDER FLOOR + COEXISTENCE · Atom A contract: the region-3d floor

**Cutter:** Fable 5 (`claude-fable-5`) · effort **max** · 2026-08-07 · one pass, one
session, one document (work-package law). **Package 3 of the render-engine
campaign** (`ENGINE.md` §0): the coexistence package, the campaign's completion
gate. Binding docs read PRIMARY this cut: `W1/GROUND.md`, `W1/CONTRACT-O.md`,
`W1/CONTRACT-G.md`, `W1/CONTRACT-M.md`, `W1/CONTRACT-T.md`, `W1/CONTRACT-C.md`,
`W1/D2-INK.md`, `W1/EXECUTION.md`, `ENGINE.md` §0–§11, `W0-C.md` §3.3/§4/§5,
`docs/decisions.md` (settled ground incl. the render-seam constitution),
the work-package skill. Code seams verified at the bytes:
`scene_tape.cljc:13-24`, `renderer.cljs:3238-3298`. Breadth indexes (Opus,
verbatim-anchored): the Package-2 contract seam index and the full code index —
`W4-FRAME-RUNTIME-CONTRACT.md`, `T2-INPUT-FLOOR-CONTRACT.md`, `W2-A-T0.md`,
`W2-B-T1.md`, probe reports, `DISCOVERIES.md` §8.

**Two Sid rulings this contract lands under (2026-08-07):**
1. Package 2 is complete and stays closed; its seam-courtroom obligation is
   ABSORBED into Package 3's close — one courtroom over both packages' atoms,
   ridden by the coexistence seam demo. No separate Package-2 close session.
2. Full engine, no MVP shapes (the FULL-BREADTH CAMPAIGN LAW, `ENGINE.md` §0):
   the preference is the largest coherent atom honestly buildable straight
   through in one implementation session, preserving the full Blender-style
   scene-composition obligation and the final coexistence obligation.

## 0. The package ruling — shape

Package 3 is **two atoms**, cut in sequence:

- **Atom A — the region-3d floor** (THIS contract, cut in full below): 3D
  regions as first-class tape citizens · per-region camera + depth · meshes ·
  hierarchy · the material/lighting ladder through interactive PBR · 3D
  picking · gizmos · viewport overlays. The whole Blender scene-composition
  floor of the ratified envelope (`W0-C.md` §4), minus nothing that the
  envelope's Blender clause names except strokes-in-3D (Atom B's seam work).
- **Atom B — the coexistence seam** (obligations FROZEN in §9 below; its
  contract is cut in a fresh session after A lands): 2D text and ink placed in
  3D · cross-region anchoring · identity across space kinds · THE SEAM DEMO ·
  the package courtroom over Packages 2+3.

Why two and not one (the honesty ruling, stated for Sid's touch #1): A alone is
already the largest single atom this campaign has cut — three new families of
machinery (region pass, mesh/object material, gizmo chrome) against six for all
of Package 2 combined. B's entry points *depend on A's landed shapes* (the
region pass, the router, the in-region instance lanes): cutting B's exact entry
points today would be invention, not contract. And the courtroom needs A live
on screen to be seam-shaped. Two sessions is the honest straight-through count;
anything less smuggles a marathon. Packages are dependency boundaries, never
scope gates (campaign law §0): **nothing in B awaits reauthorization** — its
obligations are frozen binding in §9 and the board carries them.

Why A is not smaller: region-without-mesh is a screenshot; mesh-without-pick
violates G-cover (rendered + pickable + grammar-editable + versioned — all
four or it is not material); pick-without-gizmo leaves the floor uninhabitable
and un-feelable. The envelope's Blender clause is one floor; A is that floor.

---

# ATOM A — THE REGION-3D FLOOR

## 1. Scope in plain words

A **3D region** is a first-class container the 2D world hosts: one ordered
compositor entry in the scene tape, with its own camera and its own depth
buffer, rendered offscreen and composited in tape order like any other entry
(Contract O §3.2.5 — the schema slot `:region-router` and the pass-rank
`:region 3` already exist as validated, unminted data; this atom mints them).
Inside the region, **depth owns picture order**; outside, the tape does. Two
picture ontologies, shared atoms, never one space model (`ENGINE.md` §10).

The region's interior is a real 3D scene made of **objects**: meshes (grey-box
parametric primitives on a general indexed-triangle grammar), **lights**,
**cameras**, and **empties** — each a full material citizen with identity,
revision, provenance, hierarchy (parent + local TRS), and semantic edit
operations. A **material/lighting ladder through interactive PBR**: flat/
normal-preview debug modes, punctual lights (directional/point/spot), glTF
metallic-roughness BRDF, one directional shadow map, tone-mapped into the
linear scene. **Picking** is a CPU ray road through the region's registered
router, returning resolved inner identity through the same one pick seam every
family uses. **Gizmos** (translate/rotate/scale) manipulate objects with
screen-constant metrics; their drags mint keyed diffs through the draft/settle
law. **Viewport overlays**: grid floor, axes, light/camera glyphs, selection
outline.

What lands live: the family joins the real canvas behind a flag
(`?region3d=1`, the path/connector admission pattern) with a felt fixture
scene — grey-boxes, lights, gizmos, orbit — that Sid drives at accept. All
material in A is fixture/session material; **durable write custody stays
unflipped** (the staged activation at Sid's line after the courtroom —
`T2-INPUT-FLOOR-CONTRACT.md:36-40` — governs Package 3 exactly as it governed
Package 2).

What A is NOT: no 2D text or ink inside regions (Atom B — the fwidth
perspective variant is B's, per the islands receipt), no cross-region
anchoring (B), no glTF import (refusal below), no physics/simulation (Box3D
stays its own lane), no animation clock consumption (regions in A are
event-driven only).

## 2. Named refusals — one line each, every one an extension point routed LATER

- **glTF/OBJ mesh import** — the object grammar carries general indexed
  triangles + PBR material refs from day one, so import lands as an ingestor
  + asset rows later (the D7 binary-assets-as-material tripwire fires then);
  grey-box parametric primitives ARE the ratified floor ("grey-box geometry",
  `W0-C.md` §4).
- **Point/spot shadow maps** — one directional shadow map lands in A (the sun;
  grey-boxes grounded); omni/spot shadows are one more declared depth pass per
  light through the same region-pass machinery, LATER.
- **Image-based lighting / HDR environments** — ambient is a flat declared
  term in A; IBL is a resource-kind addition behind the same BRDF, LATER.
- **Transparent-mesh order-independent blending** — A sorts transparent
  meshes back-to-front per object; OIT is a road upgrade, LATER.
- **GPU ID-buffer picking** — the CPU ray+BVH road is A's (no GPU→CPU sync in
  the hand path — W0-C §5.5 law 9); an ID-buffer road may join for dense
  scenes via the declared `:object-id` resource, LATER.
- **Clocked regions / turntable / camera animation** — the W4 deadline
  registry accepts regions whenever motion work starts; A registers no clock.
- **Region nesting (a region inside a region) and portals** — the router
  contract does not preclude recursion; A refuses it; LATER at a real want.
- **Camera-relative rendering for large extents** — A pins region-local f32
  with a declared extent budget; precision beyond it is a declared-regime
  road, LATER.
- **Sculpting · procedural geometry/shader-node authoring · volumes · path
  tracing** — campaign-scoped exclusions (Sid's rider, `W0-C.md` §4): nothing
  in A's grammar or registries may make them architecturally impossible; the
  object/mesh grammar's open extension fields are the door they enter by.
- **Multi-region shared scenes** (two regions viewing ONE interior scene) —
  A's region owns its scene 1:1; sharing arrives with identity-across-spaces
  work in B or later.
- **Snapping/precision-input in 3D** (increment/vertex snap) — 2D snap exists
  (`snap.cljc`); its 3D sibling is LATER; gizmo drags in A are free-form.
- **requestDevice limits negotiation** — A stays within default WebGPU limits
  (the islands 128MB storage-bind ceiling is far above A's fixture scale);
  high-instance regions request limits at boot LATER.

## 3. Binding laws — pointers, never restatements

- `W1/CONTRACT-O.md` §3 — one ordered tape; §3.2.5 IS this atom's charter.
- `W1/CONTRACT-G.md` §4 — geometry descriptors for every new family; §4.9's
  mesh/region row; §4.7 slop; §4.8 regime matrices over legal zoom.
- `W1/CONTRACT-M.md` §5 — citizenship template + M1–M12 for each new family.
- `W1/CONTRACT-C.md` §8 — straight→linear-premultiplied→one present transfer;
  depth/ID/normal textures are data, never sRGB (§8.1.4).
- `W4-FRAME-RUNTIME-CONTRACT.md` — the plan compiler, pass-rank ladder
  (`:region 3`), the ABSOLUTE aliasing law (producer edges for sampled
  intermediates), the color-mode law (any effect ⇒ whole-frame linear), the
  scheduler causes, the target pool + M10, the negative-space law S1.
- `T2-INPUT-FLOOR-CONTRACT.md:245-288` — dispatch precedence session > chrome
  > legacy; the intercept-predicate seam A's input rides; the camera-gesture
  reservation (naked drag/wheel at ground stays the world camera's, always).
- `docs/decisions.md` "The render seam" — no derivation without a contract
  (keyed inputs · door · ownership · projections · oracle+fence); no execution
  clock as derivation ancestor; proportional recompute; minted diffs as
  values.
- `docs/decisions.md` "One render substrate" — store slots are EDN values;
  f64 world coords, f32 container-relative GPU; settled arrangements commit as
  events, in-flight gestures stay client-side.
- `ENGINE.md` §0 (campaign law) · §7 (regime-tagged verdicts) · §8
  (coexistence decomposition) · §10 (two picture ontologies).
- `W0-C.md` §4 (the ratified envelope — the Blender clause is A's scope
  authority) · §5 (frame-graph/scheduler contract; §5.1 negative space, §5.5
  validation laws, §5.7 scheduler rules).
- Work-package skill — execution straight through, close mechanics, the
  courtroom shape.

## 4. Entry points — exact

**New namespaces (all real code lives here):**

- `src/app/client/substrate/region3d_material.cljc` — region material grammar
  (extent, background, view defaults) + object grammar (mesh/light/camera/
  empty rows: id, kind, parent, local TRS, kind-specific fields) + fail-closed
  validators + versioned defaults + semantic edit operations + the two
  citizenship descriptors + the scene-tape registration data (the
  path/connector pattern: `scene_tape.cljc:326-477`).
- `src/app/client/substrate/region3d_scene.cljc` — PURE derivation, JVM-
  testable: hierarchy composition (effective transforms; cycle refusal),
  camera math (perspective/ortho view-proj; orbit/dolly/pan operators), ray
  construction (region-local 2D point → ray), BVH build/query over object
  triangles, the pick-resolution ladder (gizmo → object → region background),
  instance-array packing layout, tape-entry production, region-pass plan
  fragment, tone-map + BRDF reference implementations (the CPU oracle for S4).
- `src/app/client/substrate/webgpu/region3d_gpu.cljs` — WGSL (mesh opaque ·
  mesh transparent · overlay/grid/glyph · gizmo · shadow-depth) + pipeline
  init + `prepare-region3d-frame!` (uploads; region-pass + shadow-pass encode
  per plan) + `region3d-entries` (produce) + `execute-region3d-batch!`
  (composite draw) + region target/depth lease management.
- `src/app/client/workspace/region3d_runtime.cljs` — session state (per-region
  view camera, focus, in-region selection, gizmo drag), the input intercept
  (registered through T2's dispatch seam), the felt fixture scene, the
  `?region3d=1` join.
- Tests: `test/app/client/substrate/region3d_material_test.clj`,
  `region3d_scene_test.clj` (the tripwire home), verifier cases in
  `src/app/client/substrate/webgpu/verifier.cljs` + golden entries in
  `test/render_engine/run_verifier.mjs`.

**Thin hooks only, in the big files (each a few lines, data or one call):**

- `scene_tape.cljc:13-24` — add `:render.family/region-3d` to `family-ids`;
  add its registration to the registry assembly (data from
  `region3d-material`). NOTE the load-time coverage lock: `renderer.cljs:3282-
  3298` throws unless family-ids and executor registrations match EXACTLY —
  both edits land in the same change or the app does not boot.
- `renderer.cljs:3238-3280` — one `frame-family-registry` entry
  `{:contract … :produce region3d-gpu/region3d-entries :execute!
  region3d-gpu/execute-region3d-batch!}`; one `prepare-region3d-frame!` call
  in the pre-pass upload block (`renderer.cljs:3508-3524`).
- `frame_graph.cljc` — mint the `:region` pass-kind (rank 3 already in the
  ladder, `scene_tape.cljc:584-588`) + the `:depth` resource kind + region
  target resources; extend the fail-closed validation set (depth resource
  without owner, region pass without producer edge to its composite entry,
  sampled/attachment aliasing — the W4 laws extended, not new machinery).
  This is the lawful "genuinely new pass/resource capability" of W0-C §5.1 —
  the executor gains a capability, never a family name.
- `compositor_gpu.cljs` — execute `:region` passes in `draw-multipass!` via
  the registered region-pass producer; extend the target pool key/sizing for
  depth formats and MSAA sample counts (`target-pool-version` 2); region
  targets are HELD LEASES with declared M10 lifetime (see pin 5.6), distinct
  from the transient per-frame pool.
- `runtime/mouse.cljs` — register the region intercept predicate through T2's
  dispatch seam (T2-CONTRACT:245-288); no new event paths.
- `runtime/render.cljs` — the fixture/flag mount (the islands probe's 5-line
  mount precedent, production-shaped).

MUST-NOT list for entry points: `containers.cljc` (Q8 transport) untouched —
3D transforms NEVER ride the affine slot lane; `text_layout.cljc` /
`text_shaper.cljs` untouched (A has no text); `scene_store.cljc`
`derive-store-frame` signature stays `[store]` (the W4 effects law) — the
region slot is ordinary store DATA, not a new derivation input.

## 5. Design pins — ruled defaults; every adjective expanded

**5.1 Family shape — one tape family, two material grammars.** The tape gains
exactly ONE family: `:render.family/region-3d`, whose entries are region
composite quads (stratum `:world`, pass-class `:region-composite`, ordinary
`stack-path`). Interior objects are NOT tape entries — depth owns inside
(Contract O §3.2.5). Two citizenship records are admitted under Contract M:
the **region family** (tape citizen; compositor entry; router; region
grammar) and the **object-3d family** (mesh/light/camera/empty grammar;
identity, provenance, versioning, edit operations; pick identity resolved
through the router; painted by the region pass). Both carry full M1–M12; the
object family's M3 order receipts are DEPTH receipts (occlusion-by-distance is
its order law), and its scene-tape reference declares `:via-router` (the
connector atom's row-projection precedent: a citizen whose paint rides another
family's pass).

**5.2 Region material grammar (the region slot value, EDN, pure store
payload):**
`{:region3d/version 1, :extent {:width :height :depth}` (region-local units;
declared budget: extent components ≤ 10^4 — beyond is a LATER regime),
`:background {:kind :opaque|:transparent :color <tagged straight sRGB>}`
(default opaque viewport grey, versioned constant), `:view-default {camera
pose used when no session camera exists}`, `:scene {<object-id> → object-row}}`.
Object rows: `{:object/id, :object/kind :mesh|:light|:camera|:empty,
:parent <object-id|nil>, :transform {:translation [x y z] :rotation
<quaternion> :scale [x y z]}, :provenance {…asserted-by…}` + kind fields —
mesh: `{:mesh {:kind :box|:sphere|:cylinder|:plane|:cone|:torus :params …}
| {:kind :indexed-triangles :positions … :normals … :indices …}` +
`:material {:base-color <tagged> :metallic 0-1 :roughness 0-1 :emissive
<tagged>}`; light: `{:light {:kind :directional|:point|:spot :color <tagged>
:intensity <number> :range … :cone …} :cast-shadow <bool, directional only in
A>}`; camera: `{:camera {:kind :perspective|:ortho :fov-y-deg … :near :far
| :ortho-scale}}`. Unknown fields: PRESERVE (Contract M unknown-field-policy)
— this is the door sculpting/node-authoring enter by later. Parametric
primitives are generators minting deterministic indexed triangles (versioned
algorithm; the derived mesh is a cache keyed source-revision + algorithm
version, Contract G derivations — never a second authority).

**5.3 The region pass.** Region rendering is DECLARED PLAN DATA: the frame
graph gains, per enabled region, a `:region` pass (rank 3) rendering that
region's interior into a leased color target (rgba16float, linear
premultiplied) with a leased depth24plus attachment, 4× MSAA resolved at pass
end, plus (when a shadow-casting directional light exists) one preceding
shadow-depth pass into a leased depth target. The composite entry samples the
resolved target with an explicit producer edge (the W4 aliasing law). Region
presence enables the linear color mode for the frame (the W4 color-mode law —
plan-level, never mixed); the machinery is landed, this is its second
consumer. Draw order inside the pass: opaque meshes front-to-back with depth
write; grid/overlays with declared depth policies; transparent meshes
back-to-front depth-test-no-write; gizmos and selection outline last with
depth cleared or depth-test-off (overlay ladder declared per element).
Tone-map (Khronos PBR Neutral, version-tagged) is the pass's final shading
step; the target holds tone-mapped linear premultiplied values; Contract C's
one-present-transfer law is untouched.

**5.4 Camera model.** Two camera notions, never conflated: **the view camera**
(session truth, per region instance, lives in `region3d_runtime` — orbit
pivot + distance + yaw/pitch + projection; originated client-side exactly like
2D pan/zoom) and **camera objects** (durable material rows you select and
gizmo like any object). A region MAY bind its view to a camera object
("look through": view camera derives from the object's effective transform +
lens until unbound). Defaults: perspective, fov-y 50°, near 0.1, far 10^4
region units, orbit pivot = selection centroid else scene origin; wheel =
exponential dolly; declared in one constants block, versioned.

**5.5 Input + focus.** The camera-gesture reservation stands untouched: naked
drag/wheel at GROUND always drives the world camera. A region gains gestures
only through FOCUS: double-click on a region enters it (Esc / click outside /
double-click outside exits); focus is session truth in `region3d_runtime`.
Unfocused: the region is an ordinary 2D object (single-click selects, drag
moves it, wheel zooms the world). Focused: drag orbits, shift-drag pans,
wheel dollies, click picks interior objects, gizmo drags win over orbit — all
registered through T2's intercept-predicate seam at session precedence.
FELT-TUNABLE, not architecture: single-click focus, hover-scroll dolly, and
focus-ring styling are one-line dispatch-predicate changes; Sid adjusts at the
felt pass. (Written fork, default chosen: double-click-focus. The alternative
— wheel-over-region dollies WITHOUT focus — was rejected because it makes
world zoom dead over every region, the exact iframe-feel §8 forbids.)

**5.6 Region target lifetime.** Region color/depth targets are LEASES HELD
ACROSS FRAMES, keyed by region instance + pixel size (region 2D bounds ×
world zoom × DPR, quantized to a declared step ladder to bound re-allocation
churn; ladder versioned) — released on region close/resize-out-of-step/
device loss. This is declared M10 lifetime/budget, NOT the transient
per-frame pool (`W4-NOW:33`'s ruling governs the FREE pool; a held lease is an
owned resource with a named owner). Budget: region targets count into the
384MiB pool accounting; on refusal the region pass halts with a receipt and
the composite draws the declared refusal fill (background color + corner
glyph) — never a silent skip (W0-C §5.7 rule 8's spirit at runtime scale).
A CLEAN region (no dirty cause) encodes NO region pass; the composite samples
the held target — the sleeping-island receipt made production law
(`islands REPORT:113-120`; scheduler §5.7 rule 3).

**5.7 Derivation contract (the render-seam five, declared here).** The region
family's frame derivation: **keyed inputs** = region slot rows (per-object
diffs minted at edit sites: gizmo settle, runtime edit API) + session view
camera + selection/focus state + in-flight gizmo preview + viewport scale —
the session inputs join at the RENDERER-EDGE frame lane (the W4 `set-effects!`
precedent: session state never enters store derivation); **door** = event-driven
causes only (`:world` for material edits, `:interaction` for camera/gizmo/
selection, `:viewport`, `:resource`; never `:clock` in A); **ownership** = the
region slot value is one generation authority (store swap); session camera is
its own (they meet at the frame pull, stamped); **projections** = region pass
encode (forward), router pick (reverse-ladder), inspector-by-id, the oracle;
**oracle+fence** = full re-derive of instance arrays + BVH vs the maintained
increments, asserted in tests (the maintained-view fence pattern,
`maintained_view_test.clj`). Proportionality pins: an object transform edit
dirties that object's instance slot + the BVH refit for its leaves + one
region-pass cause — never a scene rebuild; a camera move dirties the region
pass ONLY (zero instance uploads); 2D entries are untouched by any in-region
cause.

**5.8 Picking.** One road: CPU ray. The chain, quoted end-to-end:
`mouse.cljs:470 pick-world → scene_runtime.cljs:319 → scene_store.cljc:386-410`
(inverse Q8 affine into region-local 2D) → region entry hit-fn →
`region3d_scene/pick-region`: region-local 2D → NDC → ray via inverse
view-projection → the ladder: gizmo handles (screen-metric slop per G §4.7,
declared radius) → objects (BVH ray-triangle; nearest-t wins — depth owns
inside; light/camera/empty glyphs pick as screen-metric billboards with
declared slop) → MISS ⇒ the region itself (`:route :region-background`) —
a pick inside region bounds NEVER falls through to 2D beneath (the region is
a container; space-as-entity's outermost-rung law, region-scoped). Returns
`{:object-id :point3 :normal :t :route}` as the router's resolved inner
identity through the ordinary pick result. Boundary semantics: ray-triangle
boundary counts as hit (G §4.2.3); ties between coincident surfaces resolve
by declared object-id order (stable, versioned tie rule). fp32 ray precision
+ extent budget declared in the regime matrix (G §4.8): the legal-zoom domain
maps to region pixel density; the matrix states extent × zoom × precision
cells with the fixture receipts proving the default cells and declared roads
for the rest.

**5.9 Gizmos.** Translate (3 axes + 3 planes), rotate (3 rings + view ring),
scale (3 axes + uniform center) — session chrome (the chrome atom's
ephemeral-citizenship pin; never durable material). Screen-constant sizing:
handle geometry scaled by distance-to-camera × fov factor in-shader (the
chrome hybrid anchor/metric law's 3D sibling, declared in the family WGSL
law). Drag mints ONE preview stream (session lane) and ONE settled keyed diff
per gesture end (`:object/set-transform` op with before/after, provenance
`asserted-by :sid` via the existing channel) — draft/settle, never
per-frame durable writes. Axis-constrained math in `region3d_scene` (pure,
tested): translate = ray-plane projections, rotate = ray-ring angle deltas,
scale = pivot-distance ratios.

**5.10 Lighting + material ladder, pinned exactly.** Display modes per
region (session state): `:flat` (base-color unlit) · `:normal` (normal
visualization) · `:lit` (the default). `:lit` = glTF metallic-roughness core:
Lambert diffuse × (1−metallic), GGX NDF + Smith visibility + Schlick Fresnel
specular, punctual lights per the glTF KHR punctual model
(directional/point/spot with declared units: lux / candela), flat ambient term
(scene-level declared color × intensity), emissive additive. One directional
shadow map: single 2048² depth map, orthographic fit to a declared bounds
heuristic (fit-to-scene-AABB, versioned), 3×3 PCF, declared bias constants —
version-tagged as algorithm facts under Contract G derivations. All shading in
linear (Contract C working space); tone-map per pin 5.3. The CPU reference
BRDF in `region3d_scene` is the S4 oracle — GPU output matches it on pinned
pixels within a pre-declared per-regime error bound (declared BEFORE results,
G §4.3's law).

**5.11 Store + persistence lanes.** The region slot value rides the EXISTING
store artery (`scene_store` slots — EDN, pure payload, image-atom precedent);
edits mint keyed diffs consumed by the maintained region derivation (5.7).
NOTHING new touches Rama in A: fixture scenes are session/flag material;
M8 persistence/replay receipts run against recorded material rows + replay
(the durable custody flip for real product scenes stays the staged Sid-line
activation after the courtroom, T2's pattern). Boundary test (decisions.md
"How engine work lands"): A introduces NO new durable event vocabulary — if
implementation surfaces a genuine need for one, that is a CUTOVER-CLASS flag,
raised as the atom's one fork question, never improvised.

**5.12 Scheduler + budgets.** Causes per 5.7; ≤1 encode per rAF unchanged;
clean scene sleeps (S5 receipt). GPU budget: region instance buffers via
`gpu_budget` tracking; declared caps: ≤64 regions enabled, ≤10^4 objects per
region in A's regime matrix (beyond = declared LATER regimes; the islands
receipt prices the road to 1.5M instances when a want arrives — A's grammar
does not preclude it, A's receipts do not claim it).

## 6. The decisive scenarios — frozen as tripwires at close

Each carries its named wrong-build (the receipt-gaming pass, written):

- **S1 · the sandwich.** A region entry sits between two 2D entries in tape
  order (text-block entry above, path entry below, all overlapping). Paint
  composites in exact tape order; reverse pick: click on the overlapping text
  → text wins; click on the region (not over text) → router resolves a mesh;
  click inside the region missing all meshes → `:region-background`, never
  the path beneath. *Wrong-build named:* a region drawn as a top-most overlay
  (ignoring tape order) passes a naive "is it visible" check — the sandwich's
  above-AND-below assertions kill it.
- **S2 · depth truth.** Two interpenetrating boxes (mutual partial occlusion
  impossible under painter's order) golden-imaged; pinned-pixel ray-parity:
  CPU classifier vs GPU silhouette on declared pixels covering interior /
  outside / boundary / gizmo-over-mesh / glyph-slop classes, per declared
  regime cells. *Wrong-build named:* per-object painter-sorted quads (no real
  depth buffer) pass any single-object golden — interpenetration kills it;
  bounds-only picking passes center-hits — the near-silhouette boundary
  pixels kill it.
- **S3 · hierarchy edit round-trip.** Parent an object; gizmo-translate the
  parent; the child follows through composed transforms; gesture end mints
  exactly ONE keyed diff with provenance; replaying the recorded rows reloads
  the identical scene (M8) and the identical pick results. *Wrong-build
  named:* per-frame durable writes during drag pass an end-state check — the
  one-diff-per-gesture assertion kills it; baking effective transforms into
  children passes render checks — reparenting kills it.
- **S4 · lit color law.** A PBR sphere (declared metallic/roughness) under
  directional+point+spot over the declared background, one directional
  shadow: GPU pinned pixels match the CPU BRDF+tone-map oracle within the
  pre-declared bound; the composite obeys Contract C (linear-premultiplied
  seam, C4-style exactly-one-transfer sentinel through the region road; no
  fringe on non-black background per C2). *Wrong-build named:* shading in
  sRGB (double transfer) matches "looks right" screenshots — the oracle and
  the transfer sentinel kill it.
- **S5 · the proportional wake.** Orbiting the view camera re-encodes the
  region pass ONLY (zero object-instance uploads, zero 2D family uploads,
  measured by upload/encode receipt counters that `region3d_gpu` owns in its
  prepare path, asserted by this tripwire); a clean frame encodes NO region pass and
  composites the held target byte-identically; a fully clean scene sleeps
  (no frames). *Wrong-build named:* re-uploading instance arrays per frame
  passes every visual check — the upload counters kill it; re-rendering the
  region every frame passes visuals — the clean-frame byte receipt and sleep
  receipt kill it.

## 7. MUST-NOTs — real only

- NEVER read/require `src/app/server/env.clj`.
- `containers.cljc` Q8 affine transport: untouched. No 3D transform ever
  enters the 2D affine slot lane.
- The text seam (`text_layout.cljc`, `text_shaper.cljs`) and the
  shaping-correction SEAM: untouched (A contains no text).
- No hand-positioned central family branch — the S1/O6 static fence and the
  W4 negative-space law stay green; region work enters ONLY through the
  registries and the generic `:region` pass capability.
- The 44-image golden bank stays byte-identical; the MSDF 47-mismatch
  counterexample stays RED and untouched.
- No synchronous GPU readback anywhere in the hand path (pick is CPU ray).
- No new durable event vocabulary, no Rama-side change, no custody flip (all
  staged at Sid's line; a discovered need = the atom's one fork question).
- `derive-store-frame` stays `[store]`; effects/session state never enter
  store derivation.
- No wall-clock/frame-counter as derivation ancestor (the frame-scheduler
  test law extends to every region3d namespace).

## 8. Atom close

- Freeze S1–S5 as tripwires in `region3d_scene_test.clj` + verifier cases;
  2–3 representative goldens join the bank via `run_verifier.mjs`
  (the sandwich composite · the interpenetration+shadow lit scene · one
  gizmo/overlay frame), environment-fingerprinted like the existing 44.
- Focused suite only (`region3d_*` + `scene_tape_test` + `frame_graph_test` +
  `maintained_view_test`); foreign failures = board debt, recorded, passed by.
- Changed-file list diff-derived. One NOW entry (≤15 lines,
  `REGION3D-FLOOR-NOW.md`) + board line flip. Self-audit line included.
- **The felt moment (the dark-corner discharge):** before close, the fixture
  scene runs on the REAL canvas behind `?region3d=1` — Sid orbits a lit
  grey-box arrangement sitting between his real 2D material, drags a gizmo,
  feels the focus model. His word is the accept (touch #2). Package-2's
  pattern (fixtures, zero custody cost) — but on screen and driven.
- Implementer keeps its own falsification pass in-session (it catches real
  bugs); a genuine fork = ONE question in the NOW file, route around, keep
  building. Never a stop.

---

# ATOM B — THE COEXISTENCE SEAM (obligations frozen; contract cut after A lands)

Binding obligations B's cut MUST carry — frozen now so nothing shrinks
(campaign law: packages are dependency boundaries, never scope gates):

1. **2D text placed in 3D** — positioned glyphs from THE one layout result
   (Contract T; zero new text consumers; instance transform becomes a 3D
   placement) painted by a region-pass MSDF variant with fwidth-derived
   screenPxRange (the islands receipt: the 2D pipeline's per-vertex
   visual_size BREAKS in perspective — `islands REPORT:133-140`).
2. **Ink placed in 3D** — the D2=A centerline+pressure authority, its
   versioned derived outline given a 3D transform (the grease-pencil
   convergence, ENGINE §8); no second geometry truth.
3. **Cross-region anchoring** — a 3D anchor endpoint kind (region instance +
   object + local point) for the connector lane; the anchor's 2D projection
   is a DECLARED derivation (render-seam five: keyed on camera pose + object
   transform); 3D-point→2D-label and 2D-gesture→ray both live.
4. **Identity across space kinds** — one material shown in the 2D world AND
   placed in a region as two instances of ONE identity (ENGINE §8's open
   unknown, resolved by the instances default; a scenario proves it).
5. **THE SEAM DEMO** — one scene: real text + real ink + a live component +
   a 3D region under ONE picking, annotation, provenance, persistence, and
   authoring model. The campaign's done condition, demonstrable and felt.
6. **The package courtroom over Packages 2+3** (§10 below) rides B's close.
7. **The durable custody flip stays Sid's line** after the courtroom
   (T2-CONTRACT:36-40/417-420) — B stages it; the demo's persistence leg runs
   on it once Sid speaks it.
8. glTF import stays refused unless Sid pulls it into B.

B's contract is cut in a fresh session once A's shapes exist on disk; its
entry points are A's landed namespaces plus the connector/label lanes.

# THE PACKAGE CLOSE — the seam courtroom, defined (absorbs Package 2's)

Nowhere else defines the courtroom's format; this contract does, once, for
both packages (Sid's absorption ruling):

- **Full repo suite once** — failures triaged: package-caused (fix) vs
  foreign (board debt list, named).
- **Cross-atom seams on screen, real material through the real artery:** one
  composed scene driving image + path + connector + chrome + edited text +
  frame effects + the 3D region together — the kill record lives BETWEEN
  independently-green subsystems. The seam demo (B.5) IS this drive.
- **Broad goldens where they earn it** — the composed-scene golden joins the
  bank; no Cartesian matrices.
- **The felt/lived pass** (decisions.md lived-gate law): Sid inhabits the
  demo scene cold — task script performed on the real surface, narrating what
  he sees. The felt receipt is the primary receipt for this projection work.
- **After the courtroom:** the durable-custody activation decision goes to
  Sid (his reserved capability-level call), plus the board debt list and the
  campaign's done-condition check against `ENGINE.md` §0.

# Implementer's opening prompt — Atom A (paste into a fresh session)

> **Preflight (before the first prompt):** set permission mode now; no
> /remote-control, no MCP connects, no permission-mode changes mid-session
> (prefix-rewrite law). Plan the session to END at the build+fix completion —
> repairs batch, never fix-by-fix at depth.
>
> Build Atom A — the region-3d floor — under
> `docs/render-engine/REGION3D-FLOOR-CONTRACT.md` (read it PRIMARY, whole).
> Boot docs, byte-priced: the contract (~36KB) · `W1/CONTRACT-O.md` (4KB) ·
> `W1/CONTRACT-G.md` (10KB) · `W1/CONTRACT-M.md` (4KB) · `W1/CONTRACT-C.md`
> (5KB) — read whole. `W4-FRAME-RUNTIME-CONTRACT.md` + `T2-INPUT-FLOOR-
> CONTRACT.md`: scoped sections only as the contract cites them (pass-rank/
> aliasing/color-mode/scheduler/pool; the dispatch seam at :245-288). Code by
> seam, skeleton-first (`grep -n "^(def"`), whole-file reads only for files
> you are editing: `scene_tape.cljc`, `frame_graph.cljc`, `compositor_gpu.cljs`,
> `renderer.cljs:3238-3320`/`3453-3620`, one Package-2 family pair as the
> admission exemplar (`path_material.cljc` + `path_gpu.cljs`).
> Build the WHOLE atom straight through: material grammars → pure scene/camera/
> ray/BVH derivation (JVM tests as you go) → frame-graph `:region` capability →
> GPU pass + composite → picking through the one seam → gizmos → lighting
> ladder + shadow → overlays → the felt fixture behind `?region3d=1`. Keep
> your own falsification pass; fix in-session; a genuine fork is ONE noted
> question in `REGION3D-FLOOR-NOW.md`, never a stop. Close per contract §8
> (tripwires, goldens, NOW ≤15 lines, board flip). Foreign test failures are
> board debt. Acceptance is Sid's word at the felt fixture.
