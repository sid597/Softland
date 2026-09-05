# Seam bench — handover (session 1, 2026-09-06)

Live at https://claude.ai/code/artifact/5fd52994-becf-4840-871e-0453bcd7bc3e (republish with `url` set, or a second artifact is made). `seam-bench.html` is one WebGL2 file, no dependencies, that lets the hardest case on the 3D page be lived: a pen stroke on the front face of a box, inside a 3D region on a page, with a portal on the back wall into another land. Zoom the page, orbit, move the box, hover and click the stroke, look through the portal, draw a new stroke on the face. Everything the page claims about the seam is on it as a thing to feel, in both directions.

## What it shows, and where on the page it lands

| On the bench | On the page |
|---|---|
| The page (a big shape below, a note, a stroke, a translucent bar over the region) drawn in declared order; the region as one item in that order | a space with declared order; the region as a portal on it |
| S1: floor, wall, box, sphere, sun; drawn with depth; edges by MSAA | a space with order derived from the view; the visibility resolve |
| The stroke on the box's front face: the same coverage function as the page stroke, evaluated in chart units (u right, v down across the face) with the footprint from `fwidth`, mixed into the face's albedo before lighting, so it is lit, shadowed and occluded with the face | the chart; Position 7 (a chart layer in the face's look, never a decal) |
| `direct`: S1 drawn straight into the page pass with `R · P · V · M`, where `R` is the rect's place on the canvas and the page's zoom in one clip-space affine, clipped by scissor, depth cleared inside the rect | the composed matrix chain; page zoom as the region camera's focal length |
| `surface`: S1 rendered into a multisampled surface sized by the tree's ladder, `ceil(w·zoom·dpr)` to a 256 quantum capped at 2048, resolved, then painted at the rect; resampled between quanta | today's execution; the region as a picture, the rungs and leases as a size policy |
| The sun's shadow: S1 rendered from the sun's orthographic camera into a depth surface the shading compares against; its counter rises when the box moves and not when you orbit | render(space, camera) → surface as the space primitive; derived surfaces keyed on their own inputs |
| `window` portal: S2 rendered with S1's camera shifted into S2's frame, at the target's size, and the quad samples that surface at its own screen pixels | a portal with a derived camera; the seam one level down |
| `picture` portal: S2 rendered once from a fixed camera into a 768² surface the quad samples by its uv | a portal with a fixed camera; a picture on a wall |
| `focal length` versus `dolly` for the wheel over the region | the open question on the page: what page zoom means at a region |
| The chain panel: screen → page → page hit (declared order; the bar passes through) → region-local → a ray in S1 (page zoom does not enter it) → nearest thing, face, chart uv → classify against the stroke with a footprint taken one device pixel to the right through the same chain → through the portal into S2 | Position 8: the pick is a chain, hover is the same call; each row flashes when its value changes, at its own rate |
| `GPU id at the cursor`: an id pass of S1 with the same composed matrices, one pixel read back, compared to the CPU chain's object and face | the parity check the path round also runs: CPU and GPU agreeing on nearest |
| `draw on the face`: knots appended in chart units from the chain's uv; the new stroke is a second layer in the same shader | placing ink on a face adds a path value to a chart, nothing else |

## What it simplifies, so nobody reads more into it

- The coverage function is the swept nib as a distance (Quilez's uneven capsule per segment, the union by `min`), the path page's wet lane, not bench 9's band filler. The point shown is where the coverage runs and in which units, not which filler.
- The chart is a frame chart: the box's own +z face, u and v across it. Face and uv charts are not on the bench.
- The window portal has no oblique near plane: S2 content that would sit in front of the wall still shows through the quad. S2 is placed eight units behind the wall so this does not arise at the default view.
- The id read covers S1 only; the chain through the portal into S2 is CPU only.
- Lighting is Lambert with one shadow tap (hardware 2×2 PCF), not the tree's GGX/Smith/Schlick; colour is gamma at the end of the fragment, surfaces are RGBA8, not the tree's rgba16float and tone map.
- The surface ladder caps at 2048, not the tree's 4096, to keep the bench light.
- The pick is analytic (a slab test on the box, a sphere, planes), not a BVH; the same lowered geometry the GPU draws, so the parity check is meaningful.
- No text on the face, no copies, no exact shapes, no sampled shapes. Nothing about the four "things 2D never had" beyond occlusion, the shadow surface and the portal is on this bench.

## Deep links

State lives in the URL hash, so one command shows one state:

- `#exec=direct&portal=window` — the default: composed matrices, a window on the wall.
- `#exec=surface&portal=picture&zoom=2.4&pan=-560,-160` — the tree's execution at zoom 2.4: surface 1536×1280 on the ladder, resampled at the rect.
- `#exec=direct&zoom=4&pan=-1400,-500` — zoom 4, direct: the stroke's edge at true pixel scale.
- `#wheel=dolly` — the other reading of zoom.

## Headless check (the road proven on the path benches)

```
google-chrome --headless=new --no-sandbox --use-angle=swiftshader --use-gl=angle \
  --enable-unsafe-swiftshader --hide-scrollbars --window-size=1500,1000 \
  --virtual-time-budget=8000 --screenshot=out.png "file://…/seam-bench.html#exec=direct&portal=window"
```

`--dump-dom` in place of `--screenshot` prints the chain panel and the counters. Verified 2026-09-06 on SwiftShader: both executions render, the chain populates, the id read agrees with the CPU chain at the box's front face.

## Known fix list

- The initial chain is computed at a fixed point on the box's front face; if the box is moved via the hash it may land elsewhere.
- Stroke B is capped at 64 knots and replaced on each new drag; there is no wet route and no fit; knots are appended raw.
- `history.replaceState` runs on every pointer move during a drag.
