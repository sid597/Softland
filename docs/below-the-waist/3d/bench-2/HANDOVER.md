# The Seam Bench (bench 2) — handover, session 2 and its successor, 2026-09-06

## Where it is

- Source: `docs/below-the-waist/3d/bench-2/seam-bench.html` (one file, ~77 KB, WebGL2 + a 2D canvas, no build; only a Google Fonts link, with fallbacks).
- Live: https://claude.ai/code/artifact/32169816-ae1c-4bbb-ba07-a9171eea419a . To republish from another session: `Artifact` action `read` with that URL first, then publish with `url` set and this file's sibling `seam-bench.html` as `file_path`.
- Run locally: open the file in any WebGL2 browser (`file://` is fine). Headless check (proven, SwiftShader): `google-chrome --headless=new --no-sandbox --use-angle=swiftshader --use-gl=angle --enable-unsafe-swiftshader --hide-scrollbars --window-size=1500,1100 --virtual-time-budget=6000 --screenshot=out.png "file://…/seam-bench.html#aim=k"`; `--dump-dom` instead of `--screenshot` prints the address panel (`<pre id="addr">`), the diff (`#diff`), the rate lines (`#rEdit #rView #rScale #rFrame #rHover`), the derived line under the record (`#derived`) and the WebGL status (`#glstat`).
- Deep links (the URL hash): `zoom=<page zoom>` · `yaw=` `pitch=` `dist=` (view V) · `occ=<occluder x>` · `preset=0` (no preset stroke) · `shot=1` (paste the screenshot at load) · `px=&py=` (a virtual pointer in page pixels) · `aim=k|T|Q|I|floor` (put the pointer on the stroke, the face portal, the occluder, the screenshot, the groove floor) · **the construction:** `w=<rect width in m>` · `cut=1` (the boolean subtract on) · `anchor=origin|normalized|edge` (the attachment rule) · `onsplit=clip|detach`.

## What it is

The hardest case of the 3D starter as a thing to feel, the seam walker made visible, and, since the fold, the derived-part identity question as a thing to feel. The page (a planar space, px) holds a portal R onto a spatial space S (metres, Y up) through view V. In S: a solid B whose shape is what a saved construction CB evaluates to (a rectangle R0 on the ground, extruded to E, optionally cut by a slab C across the top from back to front); a sphere Q that can pass in front; a directional light; a planar surface Π attached to B's face `E.top` (millimetres, 1000 per metre) carrying a pen stroke k (samples with a width each, the swept round nib), by a saved **attachment rule** that says what the ink preserves when the face changes; and a portal T attached to B's face `E.front` showing a planar land L (a circle, a rect, a label) through view VL. Beside R the page can hold an image I, a screenshot of R.

The panel is laid out as the waist. Top: the address the walker returns for the pointer, level by level, now with each face's **lineage** against the previous evaluation, and the diff against the last answer. Middle: what ran and at which rate (per edit, per view, per scale, per frame, hover). Bottom: the record, what is data above the waist, live: spaces, things, views, portals, the construction, the attachment rule, the stroke's sample count and revision; and one line under it, marked derived, saying what the construction evaluated to (blocks, faces, parts) and whether the attachment resolved. No triangles, no pixels, no device tolerance, no zoom in the record.

## The seam walker (what the address means)

`walk(pagePoint)` in the script. Page pixel → page world (the page's pan and zoom are the page's view) → is it inside I? then the answer is a pixel of I and nothing else, no seam. Inside R? → portal-local → the used viewport's pixel → `rayFromRegionPoint` with the same matrices the renderer uses (`cameraOf` serves both) → the nearest of `hitBlocks` (a slab test per evaluated block, each face tagged with its construction face and part) and `hitSphere` → on `E.top`: the attachment resolves (`resolveAttachment`: rule + the face's frame from the evaluation → origin, scale, offset) → `attachUV` → `classifyStroke` (the exact swept nib: the uneven-capsule signed distance per segment, min over segments) → the address ends at k with segment, t, arc length, width, and a lower bound on the distance to the edge; on `E.front`: T's normalized attachment → `hitLand` in L → the address ends at c, r or L's background; on `C.floor` (a face the cut generated): no attachment resolves, the address says so. Every level carries its coordinates. The footprint line on Π projects one Π millimetre and measures it in page pixels: the filler's tolerance on the face, per view, under the attachment rule.

Hover is `hover(reason)` = the walk over `(state.pointer, data, views)`, re-run on pointer move and whenever data or a view changes with the pointer held (`state.pendingHover = 'data' | 'view'`). An edit of the construction or of the attachment rule is data: it re-walks the hover under a still pointer. The diff line says `entered`, `left → entered`, or `still`, and names whether the pointer, the data or the view moved.

**What "agree by construction" covers, and what it does not.** `cameraOf` serving both the renderer and the walker guarantees the coordinate relation between the picture and the hit: a world point lands on the same portal pixel in both. It does not make the two agree about *meaning*: the fold found the renderer and the picker agreeing with each other through a wrong aspect (both read it from the lease) and through a wrong nib (both projected onto the centreline). Meaning is defined by the formulas the walker and the shader each implement and is checked apart: the classify is now the closed-form swept nib on both sides, and the framing is the portal's on both sides.

## The evaluator and the attachment (the fold's addition)

`evaluate(construction, previous)` runs per edit of CB and returns convex blocks whose faces name the construction face they belong to (`E.top`, `E.front`, `E.left`…, `C.floor`, `C.wall-`, `C.wall+`) and which part of it, plus a **lineage** per face against the previous evaluation: `unchanged`, `modified (1.00 → 1.40 m wide)`, `split into 2 by C`, `merged from 2 (the cut was undone)`, `generated by C`, `deleted`. The report goes to the per-edit rate line and the address.

`resolveAttachment(rule, face)` turns the rule and the evaluated face's frame into where Π sits: Π mm = ((x, z) − origin) · scale + offset, × 1000.

| rule | what the ink preserves | scale, offset |
|---|---|---|
| `origin` | its distances from the sketch's origin corner | 1, 0 |
| `normalized` | its proportions of the face | authored extents / current extents, 0 |
| `edge` | its distance from the +x edge | 1, (authored width − current width, 0) |

`onSplit: clip` keeps the authored stroke whole in Π and paints it only where a surviving part of `E.top` is under it (the shader runs the Π material on `E.top` parts only; a generated face gets no ink); `onSplit: detach` leaves k in the record on no face and the address says so. The same resolution feeds the shader's uniforms and the walker: one definition of where Π is. The portal T is an attachment too (`normalized`): widen the rectangle and the land stretches with the face; cut, and the notch cuts the picture.

## Measured on the bench (default view unless stated; DPR 1, headless SwiftShader, 2026-09-06 after the fold)

- `aim=k`: `[page (282.4, 179.7) px · R portal-local (222.4, 109.7), surface 420×330 used of a 448×384 lease, framing 1.273 · S: t = 3.232 m at world (0.00, 1.00, −0.20) · B solid, construction CB revision 0, face E.top, lineage from E · Ftop attachment resolved, rule origin, authored on 1000×1000 mm, face now 1000×1000 · Π (500.0, 302.0) mm, footprint 0.09 px per Π mm · k inside: segment 34, t 1.00, s 673.3 mm, width 30.0 mm, at least 15.00 mm from the edge]` — six levels, `entered k`.
- `aim=k&zoom=2.4`: the same page point, the same portal-local (222.4, 109.7), viewport 1008×792 in a 1024×832 lease, the same t, the same Π (500.0, 302.0), footprint 0.22 px per Π mm. The lease changed; the framing, the view and the value did not (the fold's repair: before it, the normalized portal x of one world point moved from 0.5324 to 0.5307 between the two leases).
- `aim=k&occ=0.36`: the same pointer → `Q at t = 2.260 m`, `entered Q`; four levels.
- `aim=k&w=1.4&anchor=origin`: `E.top modified (1.00 → 1.40 m wide)`; Π (500.0, 302.0) at world x 0.00: the ink kept its distances from the origin corner; the new strip of face is blank.
- `aim=k&w=1.4&anchor=normalized`: the same Π (500.0, 302.0) is now at world x 0.20; footprint 0.14 px per Π mm (a Π millimetre is 1.4 world millimetres along x): the ink stretched with the face.
- `aim=k&w=1.4&anchor=edge`: the same Π (500.0, 302.0) at world x 0.40: the ink slid with the +x edge.
- `aim=k&cut=1`: `E.top part 1 of 2, lineage split into 2 by C · attachment resolved on 2 parts (clip)`; k inside at the same Π; the derived line: `CB → 3 blocks, faces E.top×2 E.front E.bottom E.left E.right E.back C.floor C.wall- C.wall+`.
- `aim=k&cut=1&onsplit=detach`: `attachment detached: split into 2 by C and the rule says detach · k stays in the record, on no face`; leaf `B:E.top (ink detached)`.
- `aim=floor&cut=1&yaw=0.05&pitch=0.62`: `t = 3.175 m at world (0.21, 0.72, 0.00) · face C.floor, lineage generated by C · no attachment here: a generated face inherits no ink (the rule)`.
- `aim=T&cut=1&w=1.3`: `E.front modified (1.00 → 1.30 m wide) · T attachment normalized · face (0.500, 0.500) → L (128.0, 128.0) px → c, disc`; six levels, the walk continued into another land through a notched, widened face.
- `shot=1&aim=I`: `I pixel of the used viewport · no seam`; two levels (the screenshot copies the used viewport, not the lease).

## Repaired in the fold (both found by the review Sid pasted, both reproduced in Node against the committed functions before the change)

- **The classifier was not the declared swept disc.** The committed `classifyStroke` projected the point onto the centreline and read the width at the projection: on samples (0,0,w 2) → (10,0,w 18) it put (0, 1.5) outside by 0.5, while the disc at t = 0.2 (centre (2,0), radius 2.6) contains it; the brute-force minimum of |p − c(t)| − w(t)/2 is −0.10. The shader shared the approximation, so CPU/GPU agreement was agreement in the same error. Now: the uneven-capsule closed form on both sides (`capsuleSD`), exact membership; the readout's t is the minimiser on the winning segment; the inside distance is labelled a lower bound.
- **Lease rounding changed the projection.** `cameraOf` took its aspect from the allocated surface (448×384, aspect 1.167; 1024×832, aspect 1.231) while the portal's rect is 420×330 (aspect 1.273). Now the framing is the portal's aspect, the viewport is the used sub-rect (rect × zoom × DPR, cap scaling both axes) and the allocation is quantised around it; the page paints the used sub-rect; the screenshot copies it. Today's tree is closer than the bench was: its aspect comes from `ceil(w·s)/ceil(h·s)` (fact-base-2 Part 4), within a pixel of the rect's, but still from a derived pixel size rather than from the portal.

## Not on it (fix list, not reframes)

- Shadows: the light casts none, so "a mark's pixels depend on other marks" is shown by occlusion only. Bench 0 has the sun's depth surface with a counter that rises when the box moves and not on orbit; that is the receipt for "derived pictures on their own inputs", and the tree's own join is the shadow's separate dirty role (renderer.cljs:1134-1139). Bench 0 stays live for it.
- A curved face: Π is a plane; ink on a sphere (a (u, v) → point map with a metric) is not built. Before it, the precursor the reviews of 6f62711 put first and this bench skipped when it built the split: a stroke across a parameterization seam on a curved host, the face reparameterized without changing the intended surface, the stroke staying where its attachment says and the pick returning its identity. It isolates the mapping from the topology change; the definer's starter names it as the definer's to construct.
- The path kind's real filler (bench 9's Slug port) on the face: the bench evaluates the swept nib as a distance per pixel; it is the exact nib now, but not the banded coverage lane. Lifting bench 9's shader onto the face is the natural next cut.
- The other split behaviour, split the centreline and add fresh caps at the groove, is not built; the page names it as the picture the `clip` rule does not produce.
- A merge case where two faces become one and both carried ink (whose Π wins) is not built; the lineage vocabulary has `merged`, the attachment rule has no clause for it yet.
- A portal from L back into S (the recursion bound) is not built; the bound is asserted on the page, not measured.
- Travel (re-rooting into S on a zoom crossing) is not built; the page root with a window is the only containment on the bench.
- Query modes (visible, geometric, nearest, sample) are not on it: the walk is visible-nearest only; no glass, volume or splat is on the bench to need candidates.
- The record is read-only; bench 9's editable-record textarea would let the definer paste cases.
- Not carried from bench 0, which stays in the directory as their receipt: the `surface` execution through the lease ladder, the `picture` portal with a fixed camera, the GPU id read at the cursor as the parity check, and the three wheel operations (magnify, lens, dolly) with the aperture, the lens and the effective focal scale read out.
