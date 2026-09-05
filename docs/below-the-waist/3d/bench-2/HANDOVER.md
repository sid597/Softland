# The Seam Bench (bench 2) — handover, session 2, 2026-09-06

## Where it is

- Source: `docs/below-the-waist/3d/bench-2/seam-bench.html` (one file, ~56 KB, WebGL2 + a 2D canvas, no build; only a Google Fonts link, with fallbacks).
- Live: https://claude.ai/code/artifact/32169816-ae1c-4bbb-ba07-a9171eea419a . To republish from another session: `Artifact` action `read` with that URL first, then publish with `url` set and this file as `file_path`.
- Run locally: open the file in any WebGL2 browser (`file://` is fine). Headless check (proven this session, SwiftShader): `google-chrome --headless=new --no-sandbox --use-angle=swiftshader --use-gl=angle --enable-unsafe-swiftshader --hide-scrollbars --window-size=1500,1100 --virtual-time-budget=6000 --screenshot=out.png "file://…/seam-bench.html#aim=k"`; `--dump-dom` instead of `--screenshot` prints the address panel (`<pre id="addr">`), the diff (`#diff`), the rate lines (`#rEdit #rView #rScale #rFrame #rHover`) and the WebGL status (`#glstat`).
- Deep links (the URL hash): `zoom=<page zoom>` · `yaw=` `pitch=` `dist=` (view V) · `occ=<occluder x>` · `preset=0` (no preset stroke) · `shot=1` (paste the screenshot at load) · `px=&py=` (a virtual pointer in page pixels) · `aim=k|T|Q|I` (put the pointer on the stroke, the face portal, the occluder, the screenshot).

## What it is

The hardest case of the 3D starter as a thing to feel, and the seam walker made visible. The page (a planar space, px) holds a portal R onto a spatial space S (metres, Y up) through view V. In S: a box B; a sphere Q that can pass in front; a directional light; a planar surface Π on B's top face (millimetres, 1000 per metre) carrying a pen stroke k (samples with a width each, the swept round nib); and a portal T on B's front face showing a planar land L (a circle, a rect, a label) through view VL. Beside R the page can hold an image I, a screenshot of R.

The panel is laid out as the waist. Top: the address the walker returns for the pointer, level by level, and the diff against the last answer. Middle: what ran and at which rate (per edit, per view, per scale, per frame, hover). Bottom: the record, what is data above the waist, live: spaces, things, views, portals, the stroke's sample count and revision. No triangles, no pixels, no tolerance, no zoom in it.

## The seam walker (what the address means)

`walk(pagePoint)` in the script. Page pixel → page world (the page's pan and zoom are the page's view) → is it inside I? then the answer is a pixel of I and nothing else, no seam. Inside R? → portal-local → the surface's pixel → `rayFromRegionPoint` with the same matrices the renderer uses (`cameraOf` serves both, so what you see is what you hit by construction) → the nearest of `hitBox` (slab test, face id) and `hitSphere` → on face +y: `topFaceUV` → `classifyStroke` (min over segments of distance minus the interpolated half width: the union of discs, Position 7 of the path page) → the address ends at k with segment, t, arc length, width, distance to the edge; on face +z: `frontFaceUV` → T → `hitLand` in L → the address ends at c, r or L's background. Every level carries its coordinates. The footprint line on Π (`footprint`) projects one millimetre and measures it in page pixels: the filler's tolerance on the face, per view.

Hover is `hover(reason)` = the walk over `(state.pointer, data, views)`, re-run on pointer move and whenever data or a view changes with the pointer held (`state.pendingHover = 'data' | 'view'`). The diff line says `entered`, `left → entered`, or `still`, and names whether the pointer or the data moved.

## The renderer (the picture side)

One WebGL2 program. The box's faces carry a face id; the top face computes the stroke's coverage per pixel from a 512-texel float row of samples (x mm, y mm, width mm, s mm) with `fwidth` antialiasing, over a faint millimetre grid; the front face samples L's texture (L is drawn once with the 2D canvas API and uploaded). The sphere and the ground are plain Lambert. The surface (`glCanvas`) is sized per scale: rect × page zoom × DPR in 64-px quanta, capped at 2048, and the page draws it at the portal's rect with the 2D canvas; the screenshot is a copy of that canvas at paste time.

## Measured on the bench (default view, `aim=k`)

- `[page (283.5, 179.7) px · R portal-local (223.5, 109.7), surface 448×384 · S: t = 3.232 m at world (−0.00, 1.00, −0.20) · B face +y · Π (500.0, 302.0) mm, footprint 0.10 px/mm · k inside: segment 34, t 1.00, s 673.3 mm, width 30.0 mm, 15.00 mm from the edge]` — six levels.
- `aim=k&occ=0.36`: the same pointer → `Q at t = 2.260 m`, `entered Q`; four levels.
- `aim=T`: `B face +z → T face (0.500, 0.500) → L (128.0, 128.0) px → c, disc`; six levels, the walk continued into another land.
- `shot=1&aim=I`: `I pixel (224, 192) of 448×384 · no seam`; two levels.
- `zoom=2.4&aim=k`: surface 1024×832 (re-leased), footprint 0.23 px/mm, the same Π (500.0, 302.0) and the same s 673.3 mm: the view and the value did not change, the lease and the face's tolerance did.

## Not on it (fix list, not reframes)

- Shadows: the light casts none, so "a mark's pixels depend on other marks" is shown by occlusion only.
- A curved face: Π is a plane; ink on a sphere (a (u, v) → point map) is not built.
- The path kind's real filler (bench 9's Slug port) on the face: the bench evaluates the swept nib as a distance per pixel, which agrees with the CPU classify by construction but is not the banded coverage lane. Lifting bench 9's shader onto the face is the natural next cut.
- A portal from L back into S (the recursion bound) is not built; the bound is asserted on the page, not measured.
- Travel (re-rooting into S on a zoom crossing) is not built; the page root with a window is the only containment on the bench.
- The record is read-only; bench 9's editable-record textarea would let the definer paste cases.
