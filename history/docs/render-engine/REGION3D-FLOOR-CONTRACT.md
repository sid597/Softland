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
**Falsification round ingested (2026-08-07, fresh-eyes Codex-class, 15
decision-changing findings — all accepted; repairs landed in place the same
day):** the order token, the producer/color-mode/store/pick roads, the T2
seam reading, lease ownership, and the §5.2/§5.10 schemas were re-cut against
re-verified bytes (`scene_tape.cljc:584-680/751`, `frame_graph.cljc:69/262/572`,
`compositor_gpu.cljs:111-246/369-401/752-878`,
`renderer.cljs:3238-3330/3482-3545`, `scene_store.cljc:47-100/214-344/386-410`,
`runtime/mouse.cljs:31-54/699-725`); S1–S5 each gained the leg that kills the
round's named wrong build.

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
(Contract O §3.2.5 is the charter. What exists today: O §3.1's DOC schema
names a `:region-router` entry slot, and the PLAN vocabulary already admits
the `:region` frame-pass kind — `frame-pass-kinds`, `scene_tape.cljc:592`.
What does NOT exist in code: any router field or hit-fn (`grep router` over
`scene_tape.cljc` is empty) and any region pass structure —
`frame_graph.cljc:572` `region-executable?` REFUSES plans carrying `:region`
passes today. This atom mints the road; it inherits vocabulary only).
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
- `W4-FRAME-RUNTIME-CONTRACT.md` — the plan compiler, the plan pass-kind
  vocabulary (`:region` already legal — `frame-pass-kinds`,
  `scene_tape.cljc:592`; the ENTRY-level pass-rank `:region 3` at `:587` is
  a different token and stays unused by A, pin 5.1), the ABSOLUTE aliasing
  law (producer edges for sampled intermediates), the color-mode law (any
  effect ⇒ whole-frame linear; regions join the trigger, pin 5.3), the
  scheduler causes, the target pool + M10, the negative-space law S1.
- `T2-INPUT-FLOOR-CONTRACT.md:243-288` — dispatch precedence session > chrome
  > legacy; sessions OWN their listeners (the editing runtime's own dblclick/
  pointer road — T2's ONLY shared mouse hook is the paste predicate,
  `runtime/mouse.cljs:31-54`); A's focus input rides that session pattern,
  pin 5.5; the camera-gesture reservation (naked drag/wheel at ground stays
  the world camera's, always).
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
  init + `prepare-region3d-frame!` (pre-pass UPLOADS ONLY: instance arrays,
  material/light/camera uniforms, shadow constants — queue writes, zero
  encoders, plan-independent) + `encode-region-passes!` (the registered
  `:region` pass producer the compositor invokes at plan execution — the
  compositor bullet below) + `region3d-entries` (produce) +
  `execute-region3d-batch!` (composite draw) + region lease REQUESTS + the
  S5 upload/encode receipt counters (lease OWNERSHIP stays with the
  compositor pool — pin 5.6).
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
  at the END of the pre-pass upload block (`renderer.cljs:3482-3495`, after
  `prepare-chrome-frame!` — uploads run BEFORE the arrangement/plan at
  `:3523`/`:3538`, which is exactly why prepare is upload-only); thread the
  region session snapshot into the frame map (`:region3d-session`, the
  renderer-edge session-lane precedent — the frame map already carries
  session values like `:pulse-alpha`, `renderer.cljs:3502-3522`) and thread
  `region3d-gpu/encode-region-passes!` into the compositor call as the
  `:region` pass producer (the `execute-entry!` threading precedent,
  `compositor_gpu.cljs:853-856`).
- `frame_graph.cljc` — the `:region` pass KIND is already legal
  (`legal-pass-kinds` = `frame-pass-kinds`, `frame_graph.cljc:16` /
  `scene_tape.cljc:592`); what frame_graph mints is region pass STRUCTURE:
  `structure-input` (`frame_graph.cljc:80`) derives
  `:regions [{:region/id … :size [qw qh] :shadow? …}]` from the
  arrangement's region-3d entries (the arrangement is already its input);
  `select-color-mode` (`frame_graph.cljc:69`) gains that key —
  `(seq regions)` ⇒ `:scene-color/linear` (`:capabilities` stays out of
  color mode; the live call keeps `#{}`, `renderer.cljs:3542`);
  `compile-plan-structure` (`frame_graph.cljc:262`) emits per region one
  `:region` pass (`:region/role :interior`) plus, when `:shadow?`, one
  preceding `:region` pass (`:region/role :shadow`), their `:depth`
  resource rows (add `:depth` to `frame-resource-kinds` —
  `scene_tape.cljc:595`, a one-line set edit) and the region resolve target
  with an explicit producer edge into the scene pass that draws the
  composite entry (W4 aliasing law); extend the fail-closed validators
  (`validate-pass!`/`validate-plan!`, `frame_graph.cljc:371/440`): depth
  resource without owner, region pass without producer edge,
  sampled/attachment aliasing. `region-executable?` (`frame_graph.cljc:572`)
  keeps refusing region plans on the LEGACY road — a region frame always
  compiles linear. This is the lawful "genuinely new pass/resource
  capability" of W0-C §5.1 — the executor gains a capability, never a
  family name.
- `compositor_gpu.cljs` — `draw-multipass!` gains a `:pass-producers` opts
  key (`{:region region3d-gpu/encode-region-passes!}`, threaded exactly like
  `execute-entry!`): before `encode-linear-scene!` it walks the plan's
  `:region` passes IN PLAN ORDER and invokes the producer with the encoder,
  the pass row, and that region's leases — today the plan is receipt-only
  (`compositor_gpu.cljs:853-878` never dispatches `(:passes plan)`); this
  dispatch is the new capability. `create-target!`
  (`compositor_gpu.cljs:145`) gains format generality + `:sample-count`
  (depth formats, 4× MSAA; `texture-bytes`, `:111`, learns depth and MSAA
  multipliers; `target-pool-version` 2). Region targets are HELD LEASES in
  a NEW `:region-leases` map on the compositor struct (`create-compositor!`,
  `:369`) — the compositor is the ONE owner of every target byte; exact API
  and lifecycle in pin 5.6.
- `runtime/mouse.cljs` — UNTOUCHED (the round's seam repair: T2's only
  shared mouse hook is the paste predicate, `runtime/mouse.cljs:31-54`, and
  `mouse.cljs` handles no wheel/dblclick at all — the ground owns the whole
  pointer grammar, `mouse.cljs:699-725`). Region input is session-owned
  listeners in `region3d_runtime` — pin 5.5.
- `runtime/render.cljs` — the fixture/flag mount (the islands probe's 5-line
  mount precedent, production-shaped).
- `rect_tree.cljc` — one lane walker `tree->regions` (the `tree->images`
  pattern): collects region nodes (nodes carrying `:region3d/scene` data)
  with their resolved rects.
- `scene_store.cljc` — three thin data edits (the round's missing store
  road, now written): `flatten-ops` gains the `:regions` lane
  (`scene_store.cljc:47-58`); `derive-store-frame` output gains `:regions`
  (`:315` — signature stays `[store]`, the MUST-NOT holds; the renderer's
  frame map already carries `:store-frame`, so `region3d-entries` reads
  `(:regions (:store-frame frame))`); the pick terminal (`:391-409`) gains
  the region-node dispatch — when the deepest addressed hit node carries
  region data, return `{:route :region3d :region-id … :region-local [lx ly]
  …}` (session-free; the store never sees a camera).
- `scene_runtime.cljs` — `pick-world` (`:319`) gains the completion hook: a
  `:route :region3d` result is handed to the registered resolver
  (`region3d_runtime/resolve-region-pick`: session camera +
  `region3d_scene`'s ladder) before returning — one pick seam, one named
  new step.

MUST-NOT list for entry points: `containers.cljc` (Q8 transport) untouched —
3D transforms NEVER ride the affine slot lane; `text_layout.cljc` /
`text_shaper.cljs` untouched (A has no text); `scene_store.cljc`
`derive-store-frame` signature stays `[store]` (the W4 effects law) — the
region slot is ordinary store DATA, not a new derivation input.

## 5. Design pins — ruled defaults; every adjective expanded

**5.1 Family shape — one tape family, two material grammars.** The tape gains
exactly ONE family: `:render.family/region-3d`, whose entries are region
composite quads with the order token
`{:stratum :world :pass-class :direct :stack-path <ordinary> :part-rank 0}` —
the SAME token class every store slot mints (`scene_store.cljc:266-271`).
That token is FORCED by `compare-order` (stratum → pass-class → stack-path,
`scene_tape.cljc:638`): only a `:world`/`:direct` entry can sit between two
`:world`/`:direct` siblings, which is S1's sandwich. Written fork, ruled: the
`:region-composite` STRATUM (rank 2, above `:overlay` —
`scene_tape.cljc:584`) and the entry-level pass-rank `:region 3` (`:587`)
both order the region OUTSIDE the 2D sibling ladder and can never satisfy
S1; both stay unused by A (the region's `:region` vocabulary lives at the
PLAN layer, `frame-pass-kinds` `:592`). Produced entries also carry
`:region-router <region-id>` as declarative data (Contract O §3.1's schema
slot; the operative router is the pick road, pin 5.8). Interior objects are
NOT tape entries — depth owns inside (Contract O §3.2.5). Two citizenship records are admitted under Contract M:
the **region family** (tape citizen; compositor entry; router; region
grammar) and the **object-3d family** (mesh/light/camera/empty grammar;
identity, provenance, versioning, edit operations; pick identity resolved
through the router; painted by the region pass). Both carry full M1–M12; the
object family's M3 order receipts are DEPTH receipts (occlusion-by-distance is
its order law), and its scene-tape reference declares `:via-router` (the
connector atom's row-projection precedent: a citizen whose paint rides another
family's pass).

**5.2 Region material grammar (the region slot value, EDN, pure store
payload) — every field pinned, fail-closed.** Coordinate law first: region
space is right-handed, +Y up, −Z camera-forward (glTF's), CCW front faces,
back-face cull on opaque meshes.
`{:region3d/version 1, :extent {:width :height :depth}` (region-local units,
each in (0, 10^4] — beyond is a LATER regime), `:background {:kind
:opaque|:transparent :color <tagged straight sRGB>}` (default opaque
viewport grey, versioned constant), `:ambient {:color <tagged>
:intensity <≥0, default 0.1>}`, `:view-default <camera pose map:
pivot/distance/yaw/pitch + lens, used when no session camera exists>`,
`:scene {<object-id> → object-row}}`.
Object rows: `{:object/id <unique in scene>, :object/kind
:mesh|:light|:camera|:empty, :parent <object-id|nil>` (cycle = validation
refusal), `:transform {:translation [x y z] :rotation [x y z w]
:scale [x y z]}, :provenance {…asserted-by…}` + kind field. QUATERNION LAW:
`[x y z w]` component order (glTF's); the validator REJECTS zero-length and
`|‖q‖−1| > 1e-3`, and canonicalizes by exact normalization inside that
tolerance — no other rotation value ever enters the store.
Mesh: `{:mesh {:kind <primitive> :params <per-kind map below>} |
{:kind :indexed-triangles :positions <f32 flat, 3·n> :normals <f32 flat,
3·n, same n> :indices <u32 flat, 3·m, every index < n>}` (caps: n ≤ 65536
vertices, m ≤ 131072 triangles — A's regime; NaN/∞ anywhere = refusal) +
`:material {:base-color <tagged straight sRGBA — alpha < 1 ⇒ the
transparent road, 5.3> :metallic <0-1> :roughness <0-1> :emissive <tagged>}`.
Primitive params, versioned generator `:region3d/primitives v1`
(deterministic vertex order, CCW winding; normals flat for box/plane, smooth
elsewhere; the values shown ARE the versioned defaults):
`:box {:size [1 1 1]}` · `:sphere {:radius 0.5 :width-segments 32
:height-segments 16}` · `:cylinder {:radius 0.5 :height 1
:radial-segments 32}` · `:plane {:size [1 1]}` (XZ, +Y normal) ·
`:cone {:radius 0.5 :height 1 :radial-segments 32}` · `:torus {:radius 0.5
:tube 0.2 :radial-segments 32 :tubular-segments 16}`. The derived triangle
mesh is a cache keyed source-revision + algorithm version (Contract G
derivations — never a second authority).
Light: `{:light {:kind :directional|:point|:spot :color <tagged>
:intensity <≥0: lux for directional, candela for point/spot>
:range <point/spot only: cutoff distance > 0, default 100>
:cone <spot only: {:inner-deg :outer-deg}, 0 ≤ inner ≤ outer ≤ 89>}
:cast-shadow <bool — the validator accepts it on :directional only in A>}`.
Camera: `{:camera {:kind :perspective :fov-y-deg <(0,170), default 50>
:near <>0, default 0.1> :far <>near, default 10^4>} | {:kind :ortho
:ortho-scale <>0, default 10> :near :far}}`.
Edit operations (M1 semantic IDs, the family's `:edit-operations`):
`:region3d/add-object` · `:region3d/remove-object` ·
`:region3d/set-transform` · `:region3d/set-parent` ·
`:region3d/set-material` · `:region3d/set-light` · `:region3d/set-camera` ·
`:region3d/set-region` (extent/background/ambient/view-default). Every op
payload is `{:region-id :object-id :before :after}` where before/after
carry ONLY the named sub-map — the per-object diff grain 5.7 and S3 assert.
Serialization: canonical EDN, version 1; unknown fields: PRESERVE (Contract
M unknown-field-policy) — this is the door sculpting/node-authoring enter by
later; migration: v1 is the base, the chain starts here.

**5.3 The region pass.** Region rendering is DECLARED PLAN DATA with a
WRITTEN execution road (the round's chain repair — today's compositor holds
the plan as receipt only, so the road is named end-to-end): per enabled
region the plan carries one `:region` pass (`:region/role :interior`)
rendering the interior into that region's held color lease (rgba16float,
linear premultiplied, 4× MSAA resolved at pass end) against its held
depth24plus attachment, plus — when a shadow-casting directional light
exists — one preceding `:region` pass (`:region/role :shadow`) into the
held shadow depth target (5.10), with an explicit producer edge
shadow → interior. The composite tape entry samples the resolved target
through an explicit producer edge into the scene pass that draws it (the W4
aliasing law). COLOR MODE, the exact road (W4's law gains its second
trigger): `structure-input` derives `:regions` from the arrangement,
`select-color-mode` returns `:scene-color/linear` when regions are present
(`frame_graph.cljc:69` — today effect-spans-only; `:capabilities` stays out
of it), so a region frame ALWAYS rides `draw-multipass!` — where the new
`:pass-producers` dispatch encodes the region passes in plan order before
the scene encode (§4 compositor bullet). CLEAN-REGION LAW: the region pass
stays DECLARED in every plan (plan structure is entry-independent); its
ENCODE is conditional on that region's dirty cause — a clean region encodes
nothing and the composite samples the held lease (the receipt records
encoded-vs-held per region; S5 asserts it). Draw order inside the pass:
opaque meshes front-to-back with depth write; grid/overlays with declared
depth policies; transparent meshes (material alpha < 1, or compositing over
a `:transparent` background) back-to-front by view-space depth,
depth-test-on/write-off, premultiplied source-over per Contract C §8.1 —
material alpha multiplies linear RGB and alpha through `A = a×c×o`, never a
forced alpha 1 (S4's transparency leg kills that build); gizmos and
selection outline last with depth cleared or depth-test-off (overlay ladder
declared per element). Tone-map (Khronos PBR Neutral, version-tagged) is the
pass's final shading step; the target holds tone-mapped linear premultiplied
values; Contract C's one-present-transfer law is untouched.

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
only through FOCUS, and focus is a SESSION in T2's exact sense —
`region3d_runtime` OWNS its listeners (T2's session-lifecycle precedent,
T2-CONTRACT:243-288: "a flag-only dblclick listener owned by the editing
runtime … zero shared-file listeners"; the round's repair — T2 offers no
shared pointer-intercept seam, `runtime/mouse.cljs` handles no wheel or
dblclick at all, and the ground owns the pointer grammar wholesale,
`mouse.cljs:699-725`). Flag-gated capture-phase listeners
(dblclick/pointerdown/pointermove/pointerup/wheel) on the canvas: dblclick →
`pick-world` through the ONE road → region hit ⇒ focus session opens;
anything else passes untouched. While focused: unmodified pointer/wheel
whose point lands in the focused region's 2D bounds are session input
consumed wholesale — drag orbits, shift-drag pans, wheel dollies, click
picks interior objects, gizmo drags win over orbit; pointerdown outside =
end session, then the event proceeds to its normal meaning; Escape exits
(precedence session > chrome > legacy — T2's pinned ladder). Unfocused: the
region is an ordinary 2D object (single-click selects, drag moves it, wheel
zooms the world; zero behavior change anywhere while no session is live).
FELT-TUNABLE, not architecture: single-click focus, hover-scroll dolly, and
focus-ring styling are one-line session-predicate changes; Sid adjusts at
the felt pass. (Written fork, default chosen: double-click-focus. The
alternative — wheel-over-region dollies WITHOUT focus — was rejected because
it makes world zoom dead over every region, the exact iframe-feel §8
forbids.)

**5.6 Region target lifetime — ONE owner, exact API.** The compositor's
target pool owns EVERY target byte, held leases included (Contract M10: one
named machine-driven owner; the round killed the split-ownership reading).
New compositor API — `acquire-region-lease!` / `release-region-lease!` /
`release-all-region-leases!` over a new `:region-leases` map on the
compositor struct. Lease KEY: `[region-id qw qh]` where region-id is the
region node's durable object id (one region slot = one region instance in
A) and qw/qh = pixel size (region 2D bounds × world zoom × DPR) quantized
UP on the versioned ladder: multiples of 256 px, clamped to 4096
(`region-lease-quant 256` / `region-lease-max 4096`, versioned constants).
Lease VALUE: `{:color-msaa <rgba16float 4×> :depth <depth24plus 4×>
:resolve <rgba16float 1×> :shadow <depth32float 1×, 2048², present only
while a shadow-casting light exists> :bytes}` — bytes counted by the
extended `texture-bytes` (format × sample-count) into the SAME 384MiB pool
budget as the transient pool (`W4-NOW:33`'s ruling governs the FREE pool; a
held lease is an owned resource with this named owner). LIFECYCLE, all
machine-driven: released when the region leaves the enabled arrangement
(region close — the renderer's region maintenance detects departure);
re-keyed on resize crossing a quantization step (acquire new, release old
after the next submit); `destroy-compositor!` releases all (the existing
teardown, `compositor_gpu.cljs:396-401`); DEVICE LOSS: leases are
per-compositor and the compositor is minted per device
(`ensure-frame-compositor!` WeakMap-by-device, `renderer.cljs:3313`) — a
new device starts with zero leases and the next frame re-acquires; no
separate recovery machinery is invented. REFUSAL: when a lease would exceed
the budget cap, `acquire-region-lease!` returns a refusal receipt (the
pool's `:refusals` road), the region pass halts, and the composite draws
the declared refusal fill (background color + corner glyph) — never a
silent skip (W0-C §5.7 rule 8's spirit at runtime scale). A CLEAN region
(no dirty cause) encodes NO region pass; the composite samples the held
resolve target — the sleeping-island receipt made production law
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
`maintained_view_test.clj`). Proportionality pins (the round forced the
fork into writing — ruled): effective transforms are composed on the CPU in
`region3d_scene` and packed per object; a GPU hierarchy-composing instance
lane was REJECTED because the CPU BVH needs effective transforms for
picking regardless, so it cannot shrink any affected set — it only
duplicates the composition. Therefore: an object transform edit dirties its
SUBTREE's packed instance slots + the BVH refit for the subtree's leaves
(the subtree IS the affected set of a parent edit — decisions.md
proportionality law: to the affected set, never the population) + one
region-pass cause — never a scene rebuild, and never a diff whose payload
is the scene (the diff stays the one per-object op, 5.2's grain; the
maintained derivation fans it out to the subtree); a camera move dirties
the region pass ONLY (zero instance uploads); 2D entries are untouched by
any in-region cause.

**5.8 Picking.** One road: CPU ray. The chain, written against the REAL
bytes (the round's repair — today's terminal hard-codes `rt/hit-test` and
consults no entry hit road, `scene_store.cljc:391-409`):
`mouse.cljs:470 pick-world → scene_runtime.cljs:319 → ss/pick
(scene_store.cljc:386-410)` — inverse Q8 affine into slot-local 2D,
`rt/hit-test` walks the tree exactly as today; NEW: when the deepest
addressed hit node carries region data, the terminal returns
`{:route :region3d :region-id … :region-local [lx ly] …}` (session-free —
the store never sees a camera) → `scene_runtime/pick-world` hands that
route to the registered resolver → `region3d_runtime/resolve-region-pick`
(session camera joins here) → `region3d_scene/pick-region`: region-local 2D
→ NDC → ray via inverse view-projection → the ladder: gizmo handles
(screen-metric slop per G §4.7, declared radius) → objects (BVH
ray-triangle; NEAREST positive t wins — depth owns inside, and S2 asserts
the winner's identity and t at interpenetration pixels; light/camera/empty
glyphs pick as screen-metric billboards with declared slop) → MISS ⇒ the
region itself (`:route :region-background`) — a pick inside region bounds
NEVER falls through to 2D beneath (the region is a container;
space-as-entity's outermost-rung law, region-scoped). Returns
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
(directional/point/spot with declared units: lux / candela), flat ambient
term (the region's `:ambient` color × intensity, 5.2), emissive additive.
THE SHADOW ALGORITHM, `:region3d/shadow v1` (every former adjective now a
fact — Contract G derivation facts, W4 producer/lifetime law): one
directional light (the first `:cast-shadow :directional` by object-id
order) renders scene depth into the held 2048² `depth32float` shadow lease
(5.6) through its `:region/role :shadow` plan pass, producer edge into the
interior pass. Light space: view = looking down the light's −Z (its
effective transform), up = world +Y (fallback +X when |dir·Y| > 0.99);
bounds = the light-space AABB of ALL meshes padded 5% in x/y, near/far =
the padded light-space z range; the light-space origin snaps to the shadow
texel grid (extent/2048 world units per texel) so a static scene never
shimmers; DEGENERATE scenes (no meshes, zero extent) skip the shadow pass
and the shadow factor is 1. Filtering: 3×3 PCF, texel-centered offsets,
mean of comparison taps. Bias, declared constants (versioned in the
`:region3d/shadow v1` block): pipeline depthBias 2, depthBiasSlopeScale
2.0, shader comparison offset 0.0015. All shading in linear (Contract C
working space); tone-map per pin 5.3. The CPU reference BRDF in
`region3d_scene` is the S4 oracle; the oracle's shadow factor is a BVH
ray-toward-light occlusion test, and S4's pinned pixels are DECLARED ≥3
shadow-texels from any shadow boundary so PCF and the ray agree exactly
(penumbra pixels belong to the golden, never the oracle). Error bound,
pre-registered from declared facts (G §4.3's law): ±2/255 per channel on
the rgba8 readback of pinned pixels — fp32 shading + tone-map + one
quantization; declared BEFORE results.

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
  the path beneath. THEN the order is EARNED, not fixtured: reorder the
  region above the text (an ordinary 2D sibling reorder) and the composite +
  pick assertions FLIP accordingly; rebuild the same scene from shuffled
  registration/map insertion order and assert the identical order hash and
  entry sequence (Contract O O1/O3). *Wrong-build named:* a region drawn as
  a top-most overlay (ignoring tape order) passes a naive "is it visible"
  check — the sandwich's above-AND-below assertions kill it; a producer
  that hard-codes the fixture's stack-path constant passes the static
  sandwich — the reorder + shuffle legs kill it.
- **S2 · depth truth.** Two interpenetrating boxes (mutual partial occlusion
  impossible under painter's order) golden-imaged; pinned-pixel ray-parity:
  CPU classifier vs GPU silhouette on declared pixels covering interior /
  outside / boundary / gizmo-over-mesh / glyph-slop classes, per declared
  regime cells; AND at declared overlap pixels the CPU pick asserts the
  EXPECTED WINNING OBJECT-ID and its t (nearest positive t, within declared
  tolerance) — identity, not just silhouette. *Wrong-build named:*
  per-object painter-sorted quads (no real depth buffer) pass any
  single-object golden — interpenetration kills it; bounds-only picking
  passes center-hits — the near-silhouette boundary pixels kill it; a
  farthest-t picker matches every hit/miss silhouette — the
  expected-identity/t assertions kill it.
- **S3 · hierarchy edit round-trip.** Parent an object; gizmo-translate the
  parent; the child follows through composed transforms; gesture end mints
  exactly ONE keyed diff with provenance, keyed `[region-id object-id]`,
  whose payload carries ONLY the edited object's transform before/after
  (assert: the payload for the same edit is identical in a 2-object and a
  50-object scene, modulo ids — never a `:scene` replacement); replaying
  the recorded rows reloads the identical scene (M8) and the identical pick
  results. *Wrong-build named:* per-frame durable writes during drag pass
  an end-state check — the one-diff-per-gesture assertion kills it; baking
  effective transforms into children passes render checks — reparenting
  kills it; ONE whole-region replacement diff passes a naive
  "exactly one diff" count — the payload-grain assertions kill it.
- **S4 · lit color law.** A PBR sphere (declared metallic/roughness) under
  directional+point+spot over the declared background, one directional
  shadow; PLUS the transparency leg: one translucent mesh (base-color alpha
  0.5) overlapping an opaque mesh over a `:transparent` region background.
  GPU pinned pixels (shadow pixels ≥3 texels from any shadow boundary per
  5.10; blended translucent-overlap pixels included) match the CPU
  BRDF+tone-map oracle within the pre-declared bound; the composite obeys
  Contract C (linear-premultiplied seam, material alpha through `A = a×c×o`
  per §8.1, C4-style exactly-one-transfer sentinel through the region road;
  no fringe on non-black background per C2). *Wrong-build named:* shading
  in sRGB (double transfer) matches "looks right" screenshots — the oracle
  and the transfer sentinel kill it; a shader that forces every material
  alpha to 1 passes every opaque golden — the translucent-overlap oracle
  pixels kill it.
- **S5 · the proportional wake + the lease lifecycle.** Orbiting the view
  camera re-encodes the region pass ONLY (zero object-instance uploads,
  zero 2D family uploads, measured by upload/encode receipt counters that
  `region3d_gpu` owns in its prepare path, asserted by this tripwire); a
  clean frame encodes NO region pass and composites the held target
  byte-identically; a fully clean scene sleeps (no frames). THEN the lease
  legs (5.6's M10, driven): close the region → its leases release and pool
  bytes return to the pre-open baseline (receipt); resize across a
  quantization step → new lease acquired, old released (receipt); a forced
  tiny budget cap → refusal receipt + the refusal fill composite, never a
  silent skip; destroy/recreate the compositor → zero leases survive and
  the next frame re-acquires cleanly. *Wrong-build named:* re-uploading
  instance arrays per frame passes every visual check — the upload counters
  kill it; re-rendering the region every frame passes visuals — the
  clean-frame byte receipt and sleep receipt kill it; a build that
  allocates per size and never releases passes every visual forever — the
  close/resize/budget receipts kill it.

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
- `runtime/mouse.cljs`, `events.cljs`, and the ground pointer grammar:
  byte-untouched — region input is session-owned listeners (pin 5.5).
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

# Implementer's opening prompt — Atom A (paste into a fresh Codex session)

> Build Atom A — the region-3d floor — under
> `docs/render-engine/REGION3D-FLOOR-CONTRACT.md` (read it PRIMARY, whole;
> it is post-falsification-round — every road it names was verified against
> the code bytes on 2026-08-07, and every constant it pins is the law: use
> it, never a substitute). Boot docs, byte-priced: the contract (~52KB) ·
> `W1/CONTRACT-O.md` (4KB) · `W1/CONTRACT-G.md` (10KB) · `W1/CONTRACT-M.md`
> (4KB) · `W1/CONTRACT-C.md` (5KB) — read whole. `W4-FRAME-RUNTIME-
> CONTRACT.md` (52KB) + `T2-INPUT-FLOOR-CONTRACT.md` (54KB): scoped
> sections only as the contract cites them (the color-mode law, the
> aliasing law, scheduler/pool; T2's session-lifecycle precedent at
> :243-288). Code by seam, skeleton-first (`grep -n "^(def"`), whole-file
> reads only for files you are editing; the contract's §4 lists every entry
> point with verified line anchors, and one Package-2 family pair is the
> admission exemplar (`path_material.cljc` + `path_gpu.cljs`).
> Build the WHOLE atom straight through: material grammars (5.2) → pure
> scene/camera/ray/BVH derivation in `region3d_scene` (JVM tests as you go)
> → store lanes + pick dispatch (§4 rect_tree/scene_store/scene_runtime
> hooks) → frame-graph region structure + color-mode trigger (§4) →
> compositor pass-producer dispatch + held leases (§4, 5.6) → GPU pass +
> composite (5.3) → picking through the one seam (5.8) → gizmos (5.9) →
> lighting + shadow v1 (5.10) → overlays → session input (5.5) → the felt
> fixture behind `?region3d=1`. Keep your own falsification pass; fix
> in-session; a genuine fork is ONE noted question in
> `REGION3D-FLOOR-NOW.md`, never a stop. Close per contract §8 (S1–S5
> frozen as tripwires INCLUDING the round-added legs, 2–3 goldens, NOW ≤15
> lines, board flip). Foreign test failures are board debt, recorded,
> passed by. Acceptance is Sid's word at the felt fixture.
