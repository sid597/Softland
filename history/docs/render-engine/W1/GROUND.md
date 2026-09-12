# W1 — shared render contracts

**Status:** SETTLED 2026-08-02 · contract package complete · implementation
receipts remain downstream. **Scope:** contracts only; no atom, renderer road,
product surface, or Studio custody changed here.

This is the shared law that must exist before any new drawable atom. It binds
every existing family as it migrates and every future family at admission. The
FULL-BREADTH CAMPAIGN LAW in `ENGINE.md` §0 remains the governing scope law:
receipts choose roads, failures repair roads, and no instrument narrows the
ratified envelope. `W0-C.md` §4 is the full envelope. `W0-C.md` §6.2 is the
settled ink-material choice: **centerline + pressure is authoritative; outline
is a deterministic, versioned derivation.** Only studio capability custody
remains reserved to Sid.

W1 settles five contracts:

1. one ordered scene truth projected into both paint and pick;
2. unconditional geometry truth relating mathematical classification, visual
   AA coverage, hit slop, boundary ties, transforms, and precision regimes;
3. one material-citizenship template, instantiated for D2=A ink;
4. one text layout-result seam used by measure, wrap, paint, caret, selection,
   clipping, and hit testing;
5. one color/alpha convention from durable material through presentation.

W1 does **not** select MSDF or Slug as the lasting text road. Retiring MSDF for
Slug-direct text remains a live road candidate, decided only by the separately
pre-registered dense-small-text fragment-cost receipt on real hardware,
tiny-text quality receipt, and implementation-provenance check. Either road
paints glyphs already positioned by the T1 shaper. Slug is not a shaper. The
MSDF geometry counterexample remains a permanent verifier case even if MSDF
retires.

## 1. Current-byte ground

All claims in this section were re-read on branch
`docs/current-mental-model-local` at HEAD
`27c1a457332c21719da180eb6e418e231f40800e`. They describe the migration
source, not accepted target behavior.

| Current fact | Byte citation |
|---|---|
| Scene-store pick sorts containers by descending `:layer`, then slots by `pr-str`; it inverse-transforms the point and returns the first addressed bounds hit. | `src/app/client/workspace/scene_store.cljc:196-226` |
| Rect-tree picking is axis-aligned bounds containment, with reverse child order; rounded corners, visual coverage, text outlines, shadows, and cross-family paint order do not participate. | `src/app/client/workspace/rect_tree.cljc:382-403` |
| One tree is flattened into separate text, rect, and shadow arrays before rendering, so the flattened values do not themselves preserve one interleaved atom order. | `src/app/client/workspace/scene_store.cljc:44-51` |
| Paint is a hand-written family/source sequence: shadows, sidebar rects, editor rects, content text, per-slot text, then hand-positioned chrome/settings branches. | `src/app/client/substrate/webgpu/renderer.cljs:1758-1889` |
| Per-slot text draw order is produced from `(vals slot-text-geos)`, rather than from the pick order or an explicit scene-order value. | `src/app/client/workspace/runtime/render.cljs:637-666` |
| Rich-rect coverage is a rounded-box SDF with a one-pixel AA ramp; MSDF coverage uses the atlas median isocontour; Slug computes analytic coverage from curve/band data. | `src/app/client/substrate/webgpu/renderer.cljs:67-137`, `src/app/client/substrate/webgpu/renderer.cljs:289-307`, `src/app/client/substrate/webgpu/renderer.cljs:375-514` |
| Current MSDF and Slug CPU placement each walk characters and advance by `font-size × char-width`; backend choice currently owns glyph placement as well as painting. | `src/app/client/substrate/webgpu/renderer.cljs:1243-1376` |
| Independent text consumers still derive wrap and clip by character counts/advance, derive caret/selection rectangles arithmetically, and derive click columns by division. | `src/app/client/workspace/rect_tree.cljc:154-192`, `src/app/client/workspace/rect_tree.cljc:286-349`, `src/app/client/workspace/combined_text.cljs:390-424`, `src/app/client/workspace/face_primitives.cljc:476-505`, `src/app/client/workspace/face_primitives.cljc:670-724`, `src/app/client/workspace/runtime/mouse.cljs:294-336` |
| Material RGBA is uploaded untagged; rect/text/Slug fragments return unassociated RGB with coverage in alpha; the live pipelines use `src-alpha / one-minus-src-alpha` for both color and alpha. | `src/app/client/substrate/webgpu/renderer.cljs:1247-1248`, `src/app/client/substrate/webgpu/renderer.cljs:103-136`, `src/app/client/substrate/webgpu/renderer.cljs:297-306`, `src/app/client/substrate/webgpu/renderer.cljs:507-514`, `src/app/client/substrate/webgpu/renderer.cljs:614-629`, `src/app/client/substrate/webgpu/renderer.cljs:801-816`, `src/app/client/substrate/webgpu/renderer.cljs:860-878` |
| The product chooses the adapter-preferred canvas format and configures the canvas with `alphaMode "premultiplied"`; the persistent intermediate is currently disabled. | `src/app/electric_flow.cljc:471-494`, `src/app/electric_flow.cljc:745-766`, `src/app/client/workspace/runtime/render.cljs:18` |
| The permanent verifier treats `rgba8unorm` byte `128` as a boundary tie and keeps geometry parity independent from golden-bank authorization. | `src/app/client/substrate/webgpu/verifier.cljs:236-265`, `test/render_engine/run_verifier.mjs:319-350` |

These facts establish a real divergence, not merely missing abstraction: pick
and paint currently consume different order and geometry projections, text
placement is coupled to paint backends, and alpha/color meaning is implicit.

## 2. Shared vocabulary and fail-closed admission

- **Material** is durable authoring truth with identity, revision, provenance,
  and schema. The renderer never defines it.
- **Instance** places material into a world/stacking context through a stable
  identity and affine transform.
- **Atom family** is a registered drawable/pickable/effect capability. A family
  may emit multiple ordered scene entries, but it has one citizenship record.
- **Geometry authority** is the versioned mathematical fact from which bounds,
  coverage, pick, derived contours/meshes, and export are projected.
- **Road** is an implementation of a projection: SDF, MSDF, Slug, tessellation,
  texture sampling, ray/depth, or a later backend. A road never becomes
  material truth.
- **Regime** is an explicit tuple of lifecycle (`hand`, `settle`, `scene`),
  legal zoom interval, shape extent, normalization, coordinate precision,
  coverage precision/format, and backend/environment class.
- **Ordered scene tape** is the one stable total ordering of compositable
  entries. Paint and pick are projections of this same value.

Registration and graph compilation fail closed if an enabled drawable lacks
any required order, geometry, material, regime, color, lifetime, or receipt
field. `:pick/policy :none` and a named derived effect are explicit policies;
absence is not a policy. No family is grandfathered because it predates W1.

