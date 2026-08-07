# PACKAGE 3 — THE BLENDER FLOOR + COEXISTENCE · Atom B contract: the coexistence seam + THE SEAM DEMO

**Cutter:** Fable 5 (`claude-fable-5`) · effort **max** · 2026-08-07 · one pass, one
session, one document (work-package law). Package 3's second and FINAL atom;
its close carries the ONE seam courtroom over Packages 2+3 (Sid's absorption
ruling, frozen in `REGION3D-FLOOR-CONTRACT.md` §10 — that section is the
courtroom's format authority; this contract adds to it, never restates it).
Binding docs read PRIMARY this cut: `REGION3D-FLOOR-CONTRACT.md` (whole — §9's
frozen obligations are this contract's charter), `W1/CONTRACT-O.md`,
`W1/CONTRACT-G.md`, `W1/CONTRACT-M.md`, `W1/CONTRACT-T.md`, `W1/CONTRACT-C.md`,
`W1/D2-INK.md`, `W1/GROUND.md`, `ENGINE.md` §0/§7/§8/§10, `W0-C.md` §3.3/§4/§5.1,
`docs/decisions.md` (render-seam constitution · one-render-substrate · how
engine work lands · Only-Sid list), `T2-INPUT-FLOOR-CONTRACT.md:28-65/243-288/415-425`,
`CONNECTOR-ATOM-CONTRACT.md:15-175/296-330`, `docs/islands-probe/REPORT.md:105-141`,
the work-package skill. Code seams verified at the bytes this session:
`renderer.cljs:457-506/1445-1487/2224-2258`, `text_layout.cljc:1112-1133`,
`connector_route.cljc:222-303`, `editing_runtime.cljs:54-105` (+ the two Opus
breadth indexes over the four region3d namespaces and the ten 2D-lane files,
verbatim-anchored, scratchpad-archived).

**The eight frozen obligations (A-contract §9) this cut discharges — none
shrunk:** (1) 2D text placed in 3D · (2) ink placed in 3D · (3) cross-region
anchoring · (4) identity across space kinds · (5) THE SEAM DEMO · (6) the
package courtroom over P2+P3 rides this close · (7) durable custody flip stays
Sid's line after the courtroom — B stages it · (8) glTF stays refused unless
Sid pulls it. Obligation (4) resolves `ENGINE.md` §8's open unknown ("one
identity across space kinds — likely-yes via instances [HYP]") by construction
+ scenario S1; the store today has NO one-identity-two-instances precedent
(grep-verified ABSENT; `connector_route.cljc:74 expand-edge-instances` is the
nearest analogue and lives outside the store) — this atom mints the first one.

**Carried debt, binding on the close (board law, Sid 2026-08-07):** object
manipulation in A's felt pass FELT SLOW — **observed / unmeasured /
unattributed**. It stays exactly that until the courtroom's one adapter-pinned
frame trace (§9). No cause claim, no fix, no speculative optimization anywhere
in this atom rides that observation (investigation-fence law: receipts before
verdicts). A wrong build this rule kills: an implementer who "fixes the
slowness" mid-atom by restructuring A's upload or encode paths — that work is
unauthorized here even if it helps.

---

## 1. Scope in plain words

Atom A left the region a **closed 3D world**: meshes, lights, cameras, empties
— its own citizens only. Atom B opens the wall in both directions, which is the
campaign's entire final promise (`ENGINE.md` §0 done condition; §8's
decomposition — regions landed in A; **cross-anchoring** and **shared atoms
in-region** are B; W0-C §4's Blender clause names "annotations/strokes in 3D").

**Inward — shared atoms placed in 3D.** The region's object grammar gains two
object kinds, `:text` and `:ink`, each a PLACEMENT of an existing 2D material
into the region's space: a flat (planar) instance carrying a material
REFERENCE (by address — the store's identity currency, the same one connector
`:node` bindings use) plus the ordinary object TRS. The glyphs come from THE
one layout result (Contract T — zero new text truth; the placement is exactly
§7.2's "instance affine is a shared projection input… not a reason to
reshape", generalized to a 3D instance transform); the ink comes from the D2=A
centerline authority through the SAME versioned outline/tessellation
derivation the 2D path road uses (one geometry truth, one cache — the
grease-pencil convergence, ENGINE §8). Painting rides two new pipelines inside
A's existing `:region/role :interior` pass: an unlit flat-mesh road for ink
and an MSDF glyph road whose screenPxRange is **fwidth-derived** (the Chlumsky
formula — the islands receipt proved the 2D pipeline's per-vertex
`visual_size` BREAKS in perspective, `islands REPORT:133-140`; the break site
is real and verified: `renderer.cljs:477-486` computes `v_visual_size` from a
2D axis length and emits `z=0, w=1`). Picking extends A's ladder: ray → the
placement's plane → material-local 2D point → the material's OWN pick reader
(layout hit-test for text; the path classifier for ink) — resolved inner
identity is the 2D MATERIAL's identity. One identity, two spaces, one pick
seam.

**Outward — cross-region anchoring.** The connector grammar gains a third
endpoint kind, `:region-object`: bind an edge end to an object inside a
region. Resolution projects the object's effective 3D position through the
region's session camera into region-local 2D, then through the region node's
ordinary 2D transform into the edge's anchor space — a DECLARED derivation
(render-seam five) keyed on camera pose + object transform + region placement,
driven through the connector's existing frame-edge value-diff door ("never a
clock", `CONNECTOR-ATOM-CONTRACT.md:52-61`). Orbit the camera and the arrow +
its label follow, re-deriving exactly the affected edges. 3D-point→2D-label is
the connector's existing label lane consuming the projected route;
2D-gesture→ray is A's landed pick road. Both live (obligation 3 verbatim).

**THE SEAM DEMO.** One composed fixture arrangement on the real canvas: real
edited text (the T2 proportional-paragraph fixture) + real ink + an image + a
connector + chrome + a W4 frame-effect group (the live component) + a 3D
region containing grey-boxes AND placements of the same text and ink material
— under ONE picking, annotation, provenance, persistence, and authoring model.
The authoring beat: Sid double-clicks the 2D paragraph, types, and the placed
3D instance echoes the SAME carried layout result live, keystroke by
keystroke. The demo IS the courtroom's cross-atom drive (A-contract §10) and
the campaign's completion gate (ENGINE §0 law 4).

What B is NOT: no in-region text EDITING (caret/IME in perspective — refusal
below; authoring happens through the material's 2D session and the identity
law carries it), no live-component/arbitrary-2D-subtree placement in 3D
(refusal), no new tape family, no new pass kind, no new plan/compositor/lease
machinery (A landed all of it — this atom adds content INSIDE the interior
pass and grammar at two existing families), no durable/Rama change.

## 2. Named refusals — one line each, every one an extension point routed LATER

- **In-region text editing** (caret, selection, IME rendered in perspective) —
  the placement paints the carried result; editing rides the material's 2D
  session (T2), and the identity law mirrors it live; a perspective editing
  session is the editing-expansion road, LATER at a real want.
- **Live components / arbitrary 2D subtrees placed in 3D** — that is
  render-to-texture of a 2D sub-arrangement (the portal road A refused);
  obligation 5's live component coexists in the 2D world of the demo scene;
  the placement door stays open via the object grammar's preserve policy.
- **Slug-in-region** — the islands receipt proves MSDF under perspective;
  Slug's analytic coverage there is unpriced; the region text road is MSDF,
  declared (W1 backend-parity law means this is a paint-road choice, not a
  text-truth choice); Slug-in-region is a road candidate, LATER.
- **Curved/deformed 3D text and ink** (text-on-path, per-glyph 3D warp,
  stroke depth) — placements are FLAT planar instances in B; deformation is a
  derivation-kind addition, LATER.
- **Ink AUTHORING in 3D** (grease-pencil draw mode, 3D cursor) — B places
  material authored in 2D; the 3D draw tool is an authoring-artery expansion,
  LATER, reserved with studio custody.
- **Depth-aware anchor occlusion** (hiding/dimming the arrow when the
  anchored object is occluded in-region) — the projection is depth-blind v1,
  declared (the W4 effect-blind-pick precedent of honest v1s); occlusion
  queries are a LATER road on the same resolver.
- **Region-to-region identity moves** (drag a placement between region and
  world, or between two regions, as one gesture) — B proves identity by
  reference; the move GESTURE is manipulation-expansion work, LATER.
- **Anchor kinds beyond `:region-object`** (bind to a mesh FACE/vertex, to a
  3D free point, or region-boundary anchors) — one kind proves the seam; the
  binding grammar's case dispatch is the door, LATER.
- **Multi-line / wrapped / pill labels on anchored edges** — the connector
  atom's own routed refusal stands unchanged.
- **Occlusion-correct translucency between placements and meshes beyond
  back-to-front** — A's OIT refusal stands; placements join the same declared
  transparent stage.
- **glTF/OBJ import** — stays refused unless Sid pulls it into B (frozen
  obligation 8; A's refusal row is the routing).
- **Durable custody flip** — B STAGES it (recorded-rows M8 receipts + the
  flip-ready grammar note in 5.11); the flip itself is Sid's line after the
  courtroom (frozen obligation 7; `T2-INPUT-FLOOR-CONTRACT.md:36-40/417-420`).

## 3. Binding laws — pointers, never restatements

- `REGION3D-FLOOR-CONTRACT.md` — §5.1-5.12 are STANDING LAW for everything
  region-shaped (order token, region grammar, pass, camera, input/focus,
  leases, derivation, pick ladder, gizmos, lighting, store lanes, budgets);
  §9 is this contract's charter; §10 is the courtroom's format authority.
- `W1/CONTRACT-T.md` §7.2 — instance transform is a shared projection input,
  never a reason to reshape; the seven readers; T1-2 backend parity (a new
  paint road changes coverage receipts, never wrap/caret/selection/hit
  identity). §7.3's fence: no private metrics, ever.
- `W1/D2-INK.md` — one centerline+pressure authority; every reader a
  projection of the same pinned revision + algorithm version; no second
  geometry body.
- `W1/CONTRACT-M.md` — citizenship deltas at 5.9; M6 versioning law governs
  both grammar bumps (region objects v2, connector bindings v2); M8 governs
  the persistence receipts.
- `W1/CONTRACT-O.md` §3.2.5 — the router owns inner identity; O2's
  region-composite overlap receipts; NO new tape entries in B.
- `W1/CONTRACT-G.md` §4.7 slop · §4.8 regimes · §4.9's text and 3D rows
  (both families' declared relationships compose here).
- `W1/CONTRACT-C.md` §8 — placements shade/blend in linear premultiplied
  inside the region pass; glyph coverage multiplies RGB and alpha together
  (C2: no fringe).
- `T2-INPUT-FLOOR-CONTRACT.md:42-58/243-288` — the session model + the ONE
  layout call per transition whose result is "carried unchanged into paint
  and all readers" (`editing_runtime.cljs:80-83` is that law in code); B's
  mirror consumes THAT carried value, never a second layout call.
- `CONNECTOR-ATOM-CONTRACT.md` — the two-truths law; the frame-edge
  value-diff door (:52-61); anchor space (op-container == slot-container,
  :79-92); absent-target as a declared runtime state (:99-103); the label
  seam (:131-167). B extends this lane under its own laws.
- `docs/decisions.md` "The render seam" — the derivation-contract five for
  the anchor projection and the placement derivations; proportionality to the
  affected set; no execution clock as ancestor. "One render substrate" —
  store slots are EDN; f64 world / f32 container-relative GPU; in-flight
  session, settled events. "How engine work lands" — the boundary test: B
  introduces no durable vocabulary; the flip note in 5.11 is CUTOVER-CLASS
  material for Sid's line, never improvised.
- `ENGINE.md` §0 — the full-breadth law + the done condition (the courtroom
  checks against it verbatim) · §7 regime-tagged verdicts · §8 coexistence ·
  §10 two picture ontologies.
- `docs/islands-probe/REPORT.md:133-140` — the fwidth/screenPxRange receipt;
  B's WGSL carries the formula, versioned.
- Work-package skill — execution straight through; close mechanics; the
  courtroom once per package.

## 4. Entry points — exact

**New namespaces (all substantial new code lives here):**

- `src/app/client/substrate/region3d_placement.cljc` — PURE, JVM-testable:
  placed-ref resolution helpers (address → material value + content-keyed
  revision, walking the owning slot's resolved tree via the store's
  `:addresses` index); plane math (material-local ↔ object-local mapping,
  ray→plane→material-local point); glyph-quad packing (consumes
  `text-layout/paint-result` glyph positions + atlas metrics — the SAME
  planeBounds×font-size derivation `renderer.cljs:2243-2252` uses, re-emitted
  as region-space instance data); ink packing (consumes
  `path_tessellation/derive-mesh-set` triangles — the one cache, keyed by
  `path_material/material-cache-key`); the anchor projection
  (`project-region-anchor`: object effective transform × local offset →
  `region3d_scene/project-point` → region-local → node-local → anchor space;
  behind-camera and off-rect handling per 5.6); `region-anchor-resolver`
  (the injectable closure the renderer threads to the connector lane); the
  placed pick legs (`pick-placed-text`, `pick-placed-ink`).
- `src/app/client/substrate/webgpu/region3d_placement_gpu.cljs` — WGSL
  (placed-flat unlit mesh shader · placed-MSDF glyph shader with
  fwidth-derived screenPxRange) + pipelines (rgba16float, 4× MSAA,
  depth24plus, depth-test on / depth-write off, cull `:none`, the versioned
  placement depth-bias 5.3) + buffers + `prepare-placements!` (uploads only)
  + `draw-placements!` (invoked inside A's interior pass encode) + placement
  receipt counters.
- `src/app/client/workspace/seam_demo.cljs` — the composed demo arrangement
  (registers via `scene-runtime/register-face-instance!`, the
  `region3d_runtime.cljs:125-134` pattern) + the `?seam-demo=1` join.
- Tests: `test/app/client/substrate/region3d_placement_test.clj` (the
  tripwire home; the JVM lane runs on the legacy layout road —
  `text_layout.cljc` is cljc and pure); verifier cases in `verifier.cljs` +
  golden entries in `run_verifier.mjs`.

**Edits in the two families' own homes (grammar owners, not protected files):**

- `src/app/client/substrate/region3d_material.cljc` — schema v2:
  `legal-object-kinds` (`:11-22` block) gains `:text` `:ink`;
  `canonical-object` (`:330-345`) validates their kind field
  (`{:text {:ref {:address …} :params …}}` / `{:ink {:ref {:address …}}}`,
  fail-closed, exact shapes in 5.2); `edit-operation-ids` (`:82-87`) gains
  `:region3d/set-placed`; `:versioning` (`:624`) bumps
  `:compatibility {:reader-min 1 :reader-max 2}` with the total v1→v2
  migration (identity — v1 values are valid v2 values); `edit-diff` /
  `apply-edit` (`:441/:467`) cover the new op. The region-family and
  object-family citizenship declarations (`:498-647`) gain the 5.9 deltas.
- `src/app/client/substrate/region3d_scene.cljc` — `pick-region` (`:876`)
  ladder gains the placed legs between gizmo and mesh resolution (dispatching
  to `region3d_placement`; nearest-positive-t law unchanged);
  `derive-instance-row`/`maintain-scene` (`:740/:783`) treat placed rows as
  transform-bearing objects with NO BVH triangles (their pick is the plane
  road; their subtree/refit behavior is unchanged hierarchy law).
- `src/app/client/substrate/connector_material.cljc` — the binding `case`
  (`:97-116`) gains `:region-object` (exact keys
  `#{:bind :region :object :local}`, 5.6); grammar version bump, same M6
  mechanics as above.
- `src/app/client/substrate/connector_route.cljc` — `resolved-binding`
  (`:119`) gains the `:region-object` case calling the INJECTED resolver;
  `resolve-route-geometry` (`:222`) threads an opts map
  `{:region-anchor-resolver f}` down to it; new census statuses
  `:region-anchor-absent` (5.6). The `:resolved-anchor-tuple` (`:298-299`)
  already carries the projected centers — the route/label cache re-keys by
  existing machinery, zero new cache design.
- `src/app/client/substrate/webgpu/connector_gpu.cljs` —
  `prepare-connector-frame!` (`:224`) accepts the resolver + the per-region
  door values; the frame-edge value-diff (the `:145/:190-201` token-compare
  shape) gains the per-region `[view-value scene-revision]` compare so orbit
  dirties exactly the region-bound edges (5.6/5.7).

**Thin hooks only, in the big/landed files (each a few lines, data or one call):**

- `src/app/client/substrate/webgpu/region3d_gpu.cljs` —
  `prepare-region3d-frame!` (`:963`) calls
  `placement/prepare-placements!` after `write-material-gpu!`;
  `encode-interior!` gains the `draw-placements!` call in the declared
  transparent stage (after the mesh draw at the `:1155-1159` block's
  ladder position, before gizmos); the destroy list (`:934`) and
  `region3d-receipt` (`:1254`) gain the placement buffers/counters.
- `src/app/client/workspace/scene_store.cljc` — the `:regions` assembly in
  `derive-store-frame` (`:332`) gains the pure ref-resolution step
  (`placement/resolve-placed-refs` over the ordered slots; signature stays
  `[store]` — refs resolve from store data only; session values NEVER join
  here, they join at prepare, 5.3).
- `src/app/client/workspace/editing_runtime.cljs` — one public getter,
  `session-layout-snapshot` (≈5 lines over the private `!session`,
  `editing_runtime.cljs:54`): `{:vi … :address … :revision … :layout …}`
  when a session is live, else nil.
- `src/app/client/substrate/webgpu/renderer.cljs` — inside the existing
  region prepare block (`:3491-3516`): thread `font-assets` and the editing
  snapshot into `prepare-region3d-frame!`'s options; build
  `placement/region-anchor-resolver` from the store-frame + region3d session
  + prepared state and inject it into the connector produce exactly like
  `:connector-label-entry` (`:3271-3276`) and into
  `prepare-connector-frame!`'s call. The placed-MSDF pipeline binds the SAME
  atlas texture view + sampler the 2D system owns
  (`create-msdf-font-resources`, `renderer.cljs:1463-1487` — the text system
  remains the one owner; region placement holds a reference, M10).
- `src/app/client/workspace/region3d_runtime.cljs` — the fixture assembly
  grows the demo hooks `seam_demo` needs (exported helpers only); session
  snapshot shape unchanged.

**Explicit negative space (untouched, byte-identical):** `scene_tape.cljc`
(family-ids stay the ten; pass/resource kinds stay as landed —
`:595-602`), `frame_graph.cljc`, `compositor_gpu.cljs`, `containers.cljc`,
`text_layout.cljc`, `text_shaper.cljs`, `path_material.cljc`,
`path_tessellation.cljc`, `runtime/mouse.cljs`, `events.cljs`, `ground.cljs`,
`rect_tree.cljc` (region ops already carry `:address` — nothing to add). The
fence's owner array (`verify_text_layout_fence.mjs:7-27`) GAINS the placement
namespace as an audited owner (the connector precedent: audited, not exempt).

## 5. Design pins — ruled defaults; every adjective expanded

**5.1 The placement instance model (obligation 4 made exact).** A placement is
an OBJECT ROW in the region scene (A's grammar) whose kind references 2D
material by ADDRESS. Identity law: the placed instance's `:object/id` is its
INSTANCE identity; the referenced address is its MATERIAL identity; pick
through either space resolves the SAME material identity (S1 asserts it). The
placement carries NO copy of the material's content — content joins at
derivation from the store (settled) or the carried session layout (live, 5.3).
Written fork, ruled: placements-as-new-tape-families was REJECTED — placed
content paints inside the region pass and routes pick through the region
router (A's `:via-router` citizenship precedent), so a new family id would add
registry surface with zero new order/paint semantics; the ten family-ids stand.
Contract-O consequence: B adds ZERO tape entries; the region composite entry
(A's) is the only outer footprint, and O2's overlap receipts hold unchanged.

**5.2 Grammar v2, exact shapes (fail-closed, preserve-unknown).**
Text placement: `{:object/kind :text, :text {:ref {:address <address>}
:params {:color <tagged straight sRGBA | nil → material's own style>
:max-inline-size <number | nil → the material's carried constraint>}}}`.
Ink placement: `{:object/kind :ink, :ink {:ref {:address <address>}}}`.
Shared placement laws: the plane is the object's local XY at z=0, facing +Z;
material-local +x → local +X, material-local +y (screen-down) → local −Y (the
baseline sits upright in region space); double-sided (cull `:none`);
`:transform` scale maps material units → region units (no global unit
constant — the fixture picks readable scales; default transform stays A's
identity). The new edit op `:region3d/set-placed` carries
`{:region-id :object-id :before :after}` where before/after hold ONLY the
kind's sub-map (`:text`/`:ink`) — A's S3 diff grain extends to it verbatim.
Validation refuses: a ref without `:address`; unknown `:params` keys are
PRESERVED (the M unknown-field door). A ref whose address is ABSENT from the
current projection is a DECLARED runtime state, not an error: the placement
paints nothing, is counted in the placement census, and picks as absent (the
connector's absent-target law, `CONNECTOR-ATOM-CONTRACT.md:99-103`, applied
to placements).

**5.3 The placed-text road — one layout truth, two consumers, written
end-to-end.** SETTLED source: `resolve-placed-refs` (store lane, pure)
resolves the address to the referenced node's text content + style and stamps
a CONTENT-KEYED revision (the `path_material/material-content-key` pattern —
text + style + constraints hashed; provider identity joins at packing). LIVE
source: when the editing session's target address equals a placed ref's
address, `prepare-placements!` substitutes the session's CARRIED layout
result (`editing_runtime/session-layout-snapshot` — the result
`layout-session-document` minted ONCE per transition, `editing_runtime.cljs:80-83`)
— ZERO additional layout calls (T2's one-call receipt stays intact; S1's
counter asserts it). Where no session is live, packing obtains the layout
through the ONE seam (`text-layout/layout`) as a NEW lawful provider-calling
owner: the call carries the live provider from font-assets and
`provider-identity` joins the packing key (the connector-label precedent,
`CONNECTOR-ATOM-CONTRACT.md:135-147`), and the packing cache is keyed
`[address content-revision provider-identity]` — an unchanged placement does
ZERO layout work (the live substitution keys on the session snapshot's
`:revision` in place of content-revision). Packing itself: `paint-result` glyphs → per-glyph
material-local rects via planeBounds×font-size + atlas UVs (the
`renderer.cljs:2243-2252` derivation, re-implemented in the placement
namespace against the SAME font-assets atlas metrics — MSDF selects coverage
metadata only; a missing glyph never changes placement), emitted as placed
glyph instances `[rect uv color object-index]` in material-local units; the
GPU transforms material-local → object plane → region clip via the object's
effective matrix × the region camera (one matrix chain, in-shader). WGSL law:
screenPxRange = `max(pxRange * 0.5 * dot(vec2(distanceRange)/atlasSize,
1.0/fwidth(uv_texels)) …, 1.0)` — the fwidth form (islands receipt), NEVER a
per-vertex visual_size; the formula constant block is versioned
`:region3d/placed-msdf v1`. Depth: test on, write off, transparent-stage
order (A's 5.3 back-to-front law; placements sort with transparent meshes by
view depth; co-planar ties by `:object/id` — the stable versioned tie rule);
placement depth-bias constants (pipeline depthBias −1, slopeScale −1.0,
versioned in the same block) keep decals-on-surfaces stable. Placements never
enter the `:region/role :shadow` pass in B — transparent-stage citizens cast
no shadows (a shadow-casting placement is a LATER road on the same pass). Color: Contract C
exactly — glyph coverage multiplies premultiplied RGB and alpha; the atlas
texture is data (rgba8unorm, no sRGB transfer — it already is,
`renderer.cljs:1467`).

**5.4 The placed-ink road — one authority, one cache.** Resolution pulls the
referenced path MATERIAL row; triangles come from
`path_tessellation/derive-mesh-set` under `path_material/material-cache-key`
— the SAME content-keyed cache the 2D road uses (D2 law: readers are
projections of one pinned revision + algorithm version; a region placement is
one more reader, never a second tessellation authority). The zoom argument of
the cache key is pinned to the SETTLE regime's zoom-1 cell for placements v1
(region placements are settled material; regime declared in 5.10 — a
perspective-adaptive LOD re-key is a LATER road on the same key). Packing
converts the 2D triangles to placed flat vertices (material-local, z=0) drawn
by the unlit placed-flat pipeline: the path's own paint color through the
tagged straight → linear premultiplied conversion (Contract C), depth-test
on / write off, transparent stage, double-sided, same depth-bias block.
Placed ink ignores the region's display mode (`:flat`/`:normal`/`:lit` govern
MESH shading only — placements are always material-colored; written so a
`:normal`-mode debug view never repaints a note).

**5.5 In-region pick, extended (one seam, two new legs).** A's ladder order
becomes: gizmo handles → {meshes ∪ placements} by nearest positive t →
object glyphs → region background. Placed-text pick body: the carried
layout's `:metrics :logical-bounds` rect on the plane (interior policy,
Contract G's text row); a hit inside it resolves the cluster route via the
SAME layout result (`hit-test-result` — Contract T's seventh reader; the
inverse chain is ray→plane→material-local, then result geometry). Placed-ink
pick body: the path family's own classifier in material-local space
(centerline-width/outline parity per its declared policy). Both legs return
`{:route :placed-text|:placed-ink :object-id … :address … :t …}` — the
material identity (address) rides the resolved inner identity, and for text
the cluster subroute rides along (annotation-grade precision). Slop: G §4.7
screen-metric slop maps through the inverse chain like A's glyph slop
(declared radius shared with A's `glyph-hit-radius-px` block). The
2D instance's pick is UNTOUCHED — same store terminal, same routes as today.

**5.6 The `:region-object` endpoint, exact.** Grammar:
`{:bind :region-object, :region <the region NODE's address>,
:object <object-id in that region's scene>, :local [x y z] (default [0 0 0],
finite, object-local offset)}` — fail-closed, exact keys. Resolution (the
injected resolver, built at the renderer edge, pure math in
`region3d_placement`): region address → the region op row (rect + container
+ scene, from the store frame's `:regions`) + that region's SESSION camera
(view from `region3d_runtime`'s snapshot; the A-contract 5.4 view-default
when no session row exists) → object effective transform (from the region3d
system's maintained scene — the prepared state the same frame already built;
a pure recompute via `region3d_scene/compose-hierarchy` is the JVM/test
road) → point3 = effective × local → `project-point` → region-local 2D →
node-local → the region node's container transform into the edge's anchor
space (`cross-space-point`, the existing road — never a second transform
path). Returns a `:point`-shaped resolution `{:center [x y] :camera
<anchor-camera> :clip :none}` (tip = center; there is no bounds quad to clip
at — the arrow lands ON the projection). Edge cases, all declared and
censused: object id absent from the scene OR region address absent →
`:region-anchor-absent` (paints nothing — the connector's absent-target
state); point BEHIND the camera (w ≤ 0 or z outside clip) →
`:region-anchor-absent`; projection in front but OUTSIDE the region rect →
CLAMP to the rect boundary along the segment from rect center to the
projection, censused `:anchor-clamped` (the region is the window; the arrow
points into it). Doors (proportionality): the per-frame value-diff compares,
per region with bound edges, `[the session view value, the maintained scene
revision, the region node's effective transform]` — all cheap value
compares in the connector prepare (the `:190-201` token-compare shape); a
change re-projects ONLY that region's bound edges (a `bound-edges-by-region`
index beside the existing `bound-edges-by-container`), and the route/label
re-derive only when the resolved tuple actually moved (the existing
`:resolved-anchor-tuple` key). New receipt counter: `:anchor-projections`.
No clock, ever. Region-bound endpoints are FIXTURE/session edges in B
(custody 5.11); `:mixed-camera` law untouched (a region anchor inherits the
region node's camera flag).

**5.7 Derivation contracts (the render-seam five, declared for each new
derivation).** (a) Placement packing: keyed inputs = placed rows (per-object
diffs via A's maintained scene) + resolved ref values (content-keyed
revisions from the store lane) + carried session layout when live (a
frame-lane session input, joining at PREPARE — the A-contract 5.7
renderer-edge session-lane law; NEVER inside `derive-store-frame`) +
provider-identity + atlas metrics; door = store-change events + the session
snapshot value-diff at the frame edge; ownership = the placement pack cache
inside the region3d system's prepared state (device-scoped, dies with it);
projections = interior-pass draw (forward), placed pick legs (reverse),
census, the oracle; oracle+fence = full repack vs incremental, asserted in
tests (A's maintained-view fence pattern). (b) Anchor projection: 5.6's five
as written. Proportionality receipts: an edit to a placed material's TEXT
dirties exactly that placement's pack + its region's interior encode (2D
lanes update by their own existing doors — no cross-wake); a camera orbit
dirties the region pass + that region's bound-edge projections and NOTHING
in any 2D family (S5 extends A's counters across the seam); an edit to a
NON-placed material leaves every region receipt at zero.

**5.8 The demo fixture, pinned.** Flags: `?live-atoms=1&region3d=1&seam-demo=1`
(each lane keeps its own flag law; `seam-demo` adds the composed arrangement
— pattern `region3d_runtime/flag-enabled-search?`, `:28-29`). The
arrangement (all fixture/session material, `seam_demo.cljs` owns it): the T2
proportional-paragraph fixture (its OWN editable fixture — T2's editable
population pin is untouched) · one ink stroke (path fixture material) · one
image · one chrome-selectable rect · one W4 effect group (the live
component: the landed 50% live group with pulse — W4's fixture vocabulary) ·
one connector 2D→2D · THE REGION (A's felt fixture scene) grown with: a
`:text` placement referencing the T2 paragraph's address, an `:ink`
placement referencing the ink stroke's address, and one `:region-object`
edge from a 2D block to a grey-box (label on). Every piece already exists
behind its flag except the placements, the region-bound edge, and the
composition. The demo's task script (the courtroom's felt pass reads it
cold): orbit · click a mesh · click the placed text (watch the cluster
route) · dblclick the 2D paragraph and TYPE (watch the 3D echo) · drag the
grey-box gizmo (watch the arrow + label follow) · reload with the flags
(watch M8 absence law hold: session dress gone, recorded-material replay
identical).

**5.9 Citizenship deltas (Contract M — no new family records).** The
object-3d family record (A's) bumps to grammar v2: the two kinds, the new
edit op, migration, unknown-field policy unchanged; its pick result vocabulary
gains the two placed routes. The text family's citizenship gains the region
instance context under M2 (instance identity survives placement) and a
declared second paint road under its render field (placed-MSDF, coverage-only
per T1-2). The path family's citizenship likewise (placed-flat road; same
derivation keys). The connector family's grammar bumps for the third binding
kind (M6 versioning; malformed refusal receipts extend). Receipts: each delta
lands as test rows in the EXISTING receipt homes (placement tests + connector
tests), never as new receipt machinery.

**5.10 Budgets + regimes.** Placed glyph instances ≤ 16384 per region and
placed ink vertices ≤ the path family's existing caps (versioned constants in
`region3d_placement_gpu`; refusal at cap = the placement census's
`:over-budget` state + nothing painted for the offending placement — never a
silent truncation of glyphs mid-string). Buffer bytes ride `gpu_budget`
tracking like A's buffers. Regime row (G §4.8): placements declare the settle
lifecycle, zoom-1 material cell, region pixel-density domain from A's matrix;
the pre-registered S2 error bound derives from fp32 shading + MSDF coverage +
rgba8 readback exactly as A's S4 bound did (±2/255 on pinned pixels ≥3
texels from glyph edges; declared BEFORE results).

**5.11 Custody, staged exactly.** Everything B mints is fixture/session
material: placements live in the region slot value (store, client), region
edges in the connector fixture lane, the demo arrangement in `seam_demo`.
NOTHING touches Rama; no durable event vocabulary moves (the boundary test).
M8 receipts run as recorded material rows + replay (A's pattern). THE FLIP
NOTE (for Sid's line after the courtroom, written now so the flip is a
decision, not a design session): flipping durable custody for coexistence
material requires (a) the region slot value — already EDN, replay-proven —
riding the settled-arrangement event artery; (b) the `:region-object`
binding entering the relation kernel's endpoint vocabulary (RelationEdgeRow
endpoints are address-shaped today — the region-object form is
`[region-address object-id local]`; a kernel-side vocabulary addition =
CUTOVER-CLASS, scheduled, Sid's word); (c) T2's own staged flip for the
edited materials. B ships (a)'s and (b)'s client grammar flip-ready and
stops.

## 6. The decisive scenarios — frozen as tripwires at close

Each carries its named wrong-build (the receipt-gaming pass, written):

- **S1 · one identity, two spaces (the mirror).** The T2 paragraph placed in
  the region; a session opens on the 2D instance; three keystrokes land.
  Assert per transition: layout-call counter +1 TOTAL (the carried result
  serves both consumers — `editing_runtime` receipt + the placement pack
  counter); the placed instance's glyph pack re-derives from the SAME
  `:layout/id`; pick through the 2D road and through the region road return
  the SAME address; session exit → settled content-keyed revision takes
  over with identical glyphs. THEN the absent leg: retarget the ref to a
  dead address → declared absent state (census, no paint, no crash).
  *Wrong-build named:* a placement that COPIES text into its own material
  passes every visual — the shared-`:layout/id` and same-address-pick
  assertions kill it; a second layout call per keystroke passes visuals —
  the call counter kills it; a mirror that polls the session per frame
  passes everything visible — the door receipt (zero packs on a frame with
  no transition) kills it.
- **S2 · perspective truth for placed atoms.** The paragraph and an ink
  stroke placed at a steep oblique angle between two grey-boxes (one
  in front, one behind — real depth interleaving); golden + pinned-pixel
  legs: CPU ray→plane→layout/classifier picks vs GPU silhouette on declared
  pixel classes (glyph interior ≥3 texels from edges · ink interior ·
  plane-miss · mesh-occludes-placement · placement-in-front-of-mesh), within
  the 5.10 pre-registered bound; the depth interleave asserts placement
  pixels lose to the nearer box and win over the farther one. *Wrong-build
  named:* reusing the 2D per-vertex `visual_size` road passes every
  FACE-ON golden — the oblique-angle pinned pixels kill it (edges blur or
  alias, coverage departs the bound); billboarding placements
  (camera-facing) passes face-on views — the oblique golden's foreshortening
  kills it; drawing placements as overlay (depth-test off) passes
  single-box scenes — the in-front/behind interleave kills it.
- **S3 · the anchor follows everything that can move.** A labeled edge from
  a 2D block to `{:bind :region-object}` on a grey-box. Five moves, each
  asserting the projected tip + route + label move correctly AND the
  counters stay proportional (`:anchor-projections` touches only this
  region's edges; `:route-resolutions` only re-fires when the tuple moved):
  orbit the camera · gizmo-drag the box (settle mints A's one diff; the
  anchor follows) · drag the region as a 2D object (Q8 road) · drag the 2D
  block (existing container door) · focus-exit with zero change (zero
  re-derivations). THEN the declared edges: dolly until the box leaves the
  frustum → `:region-anchor-absent`, paints nothing; orbit it off-rect but
  in front → clamped tip on the rect boundary, censused. *Wrong-build
  named:* projecting once at bind time passes every static golden — the
  orbit leg kills it; a per-frame full re-route of all edges passes all
  motion — the proportionality counters kill it; a rAF-driven refresh
  passes motion — the no-clock door receipt (zero projections across N
  idle frames) kills it.
- **S4 · one model across the seam (the demo, mechanically).** The full 5.8
  arrangement. Reverse-pick parity walk over declared points covering: 2D
  text · image · path · connector stroke + label · chrome · effect-group
  content · region background · in-region mesh · placed text (cluster
  route) · placed ink · gizmo — every hit returns its declared route and
  material identity through the ONE seam; provenance readable on each
  (`asserted-by` present); then RECORD → cold reload → REPLAY: identical
  scene value, identical order hash, identical pick results, session dress
  honestly gone (M8 + the absence law). Rebuild the arrangement from
  shuffled registration order → identical order hash (O1 extended across
  the composed scene). *Wrong-build named:* a pick road special-casing the
  demo fixture ids passes the walk — the shuffled rebuild + a second
  arrangement with permuted ids kill it; a replay that REGENERATES (fresh
  ids) passes visuals — the identity-stability assertions kill it.
- **S5 · the seam sleeps proportionally.** From the full demo at rest:
  full-scene sleep (zero encodes, zero uploads — A's receipt extended to
  the composed scene); type in the clipped-card fixture (editable, NOT
  placed): text lanes wake, every region receipt stays ZERO; type in the
  placed paragraph: exactly that region wakes; orbit:
  region + its bound edges wake, every 2D family's upload counters stay
  ZERO; drag the image: image lane wakes, region stays asleep. *Wrong-build
  named:* any cross-wake (camera dirties 2D uploads; a text edit dirties a
  region not placing it) passes all visuals — the per-family counters kill
  it; re-packing placements every frame passes visuals — the pack counter
  over idle frames kills it.

## 7. MUST-NOTs — real only

- NEVER read/require `src/app/server/env.clj`.
- `text_layout.cljc` / `text_shaper.cljs`: byte-untouched (new consumers
  audit INTO the fence's owner array; the seam itself does not move).
- `path_material.cljc` / `path_tessellation.cljc`: byte-untouched (the
  placement is a reader of the existing cache, never a new authority).
- `containers.cljc` Q8 transport: untouched — 3D never enters the 2D affine
  lane; the anchor projection OUTPUTS a 2D point into the existing chain.
- `scene_tape.cljc`, `frame_graph.cljc`, `compositor_gpu.cljs`: untouched —
  no new family, pass kind, resource kind, plan structure, or lease shape.
  The S1/O6 negative-space fence and A's landed receipts stay green.
- `runtime/mouse.cljs`, `events.cljs`, ground pointer grammar:
  byte-untouched (B rides A's session listeners + T2's landed hooks).
- The 47-image golden bank stays byte-identical; the MSDF 47-mismatch
  counterexample stays RED and untouched; golden/input roads stay
  append-only.
- No synchronous GPU readback in any hand path.
- No durable event vocabulary, no Rama-side change, no custody flip (5.11's
  note is decision material for Sid, not work).
- `derive-store-frame` stays `[store]`; the session mirror joins at prepare
  only.
- No wall-clock/frame-counter as derivation ancestor anywhere B touches.
- No perf/latency work motivated by the observed object-interaction
  slowness — that debt is unmeasured until the courtroom trace (header law).

## 8. Atom close

- Freeze S1–S5 as tripwires in `region3d_placement_test.clj` (+ the
  connector test home for S3's grammar legs); 2–3 representative goldens
  join the bank via `run_verifier.mjs`: the oblique placed-text/ink depth
  interleave · the anchored-edge-over-region composite · THE SEAM DEMO
  frame (the composed arrangement — the courtroom's broad golden),
  environment-fingerprinted like the existing bank.
- Focused suite only: `region3d_*` + connector + `scene_tape_test` +
  `frame_graph_test` + `maintained_view_test`; foreign failures = board
  debt, recorded, passed by.
- Changed-file list diff-derived. One NOW entry (≤15 lines,
  `REGION3D-SEAM-NOW.md`, STANDING frozen at open) + board line flip.
  Self-audit line included.
- **The felt moment:** the demo runs on the real canvas behind the three
  flags; Sid drives the 5.8 task script. His word is the accept (touch #2).
- Implementer keeps its own falsification pass in-session; a genuine fork =
  ONE noted question in the NOW file, route around, keep building. Never a
  stop.

## 9. THE PACKAGE COURTROOM — rides this close (format: A-contract §10)

A-contract §10 defines the courtroom once for both packages — full repo
suite triaged · the cross-atom seam drive (the demo IS it) · broad goldens
where earned · the felt/lived pass cold · then the custody decision, board
debt list, and the done-condition check against `ENGINE.md` §0 (verbatim: "I
can draw like tldraw, design precisely like Figma, compose and inspect a 3D
scene like Blender, and freely connect those acts without leaving Softland
or losing identity, provenance, editability, or history"). This contract
ADDS the debt receipt:

- **The latency trace.** ONE frame trace captured while driving the
  felt-slow interaction (gizmo drag on the fixture scene), the receipt
  stating adapter/device identity FIRST (SwiftShader vs hardware
  reclassifies every number — work-package field note), covering the
  interaction's full frame timeline (input → prepare → encode → submit →
  present). Until it exists the slowness stays observed/unmeasured/
  unattributed; the trace's reading may mint cause CLAIMS with named
  kill-probes (investigation-fence modality law), and any repair is board
  debt routed AFTER the courtroom unless Sid pulls it in.
- Courtroom outputs, recorded in the NOW file: suite triage · demo drive
  receipt · felt verdict (Sid's words verbatim) · the trace + its reading ·
  the board debt list · the done-condition check · the custody question put
  to Sid (5.11's flip note is the decision packet).

# Implementer's opening prompt — Atom B (paste into a fresh Codex session)

> PREFLIGHT before the first prompt: set permission mode / remote-control /
> MCP toggles NOW (a later toggle rewrites the whole cached prefix).
> Build Atom B — the coexistence seam + THE SEAM DEMO — under
> `docs/render-engine/REGION3D-SEAM-CONTRACT.md` (read it PRIMARY, whole;
> every road it names was verified against the code bytes on 2026-08-07;
> every constant it pins is the law). Boot docs, byte-priced:
> this contract (~33KB) · `REGION3D-FLOOR-CONTRACT.md` (52KB — §4/§5 are
> standing law for every region seam you touch; read whole) ·
> `W1/CONTRACT-T.md` (5.7KB) + `W1/D2-INK.md` (5.2KB) + `W1/CONTRACT-M.md`
> (4KB) + `W1/CONTRACT-C.md` (4.6KB) — whole; `W1/CONTRACT-G.md` (10.3KB)
> §4.7-4.9 · `CONNECTOR-ATOM-CONTRACT.md` (41.6KB): the two-truths law,
> bindings/anchor space, labels, value-diff door (:15-175) only ·
> `T2-INPUT-FLOOR-CONTRACT.md` (54.5KB): :28-65 + :243-288 only ·
> `docs/islands-probe/REPORT.md:105-141`. Code by seam, skeleton-first
> (`grep -n "^(def"`), whole-file reads only for files you are editing; §4
> lists every entry point with verified line anchors — re-locate on drift,
> log it, never stop (line hints re-locate; contracts bind on substance).
> Build order: grammar v2 (region3d_material + connector_material,
> validators + tests) → `region3d_placement.cljc` pure derivations +
> resolver + pick legs (JVM tests as you go, legacy-layout road) → store
> ref-resolution step → placement GPU (WGSL per 5.3/5.4, prepare/draw
> hooks into A's interior pass) → pick ladder legs (5.5) → the
> `:region-object` endpoint end-to-end (5.6: resolver injection at the
> renderer edge, connector doors, census) → the session mirror thread
> (editing_runtime getter → prepare) → `seam_demo.cljs` (5.8) → drive the
> demo yourself, then close per §8 (S1–S5 frozen as tripwires, 3 goldens,
> NOW ≤15 lines, board flip). The MUST-NOT list in §7 is absolute — the
> named untouched files stay byte-identical; the observed slowness is
> UNMEASURED debt: no perf work rides it. Keep your own falsification
> pass; fix in-session; a genuine fork is ONE noted question in
> `REGION3D-SEAM-NOW.md`, never a stop. Foreign test failures are board
> debt, recorded, passed by. Acceptance is Sid's word at the demo; the
> package courtroom (contract §9) runs at this close.
