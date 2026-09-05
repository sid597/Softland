# Ink Bench handover (from session 4, closing 2026-09-05)

## Where it is

- Source: `/tmp/claude-1000/ink-bench/ink-bench.html` (single file, ~55 KB, no build, no dependencies beyond a Google Fonts link with fallbacks).
- Live: https://claude.ai/code/artifact/2f98a7e6-beb1-43df-9247-3974586256d6 (Sid's account; a Claude session can `Artifact read` that URL to get the raw HTML, and `Artifact publish` with `url` to update it in place).
- Run: open the file in any browser with WebGL2 (`file://` is fine). Nothing is fetched except the fonts.

## What it is

One canvas, one path, drawn by three roads, with the pick formula run on the CPU at the cursor. World units: 1 unit = 1 CSS pixel at zoom 1. Presets sit in a 0–128 box; the view opens at zoom 3 centered on (64, 64). Y is down, as in the client.

## Fixtures (Geometry section)

| Preset | Source | Points | Defaults |
|---|---|---|---|
| self-crossing ink (harness) | `harness/path.cljs` golden `:translucent-self-crossing` | `[[26,28,.65],[102,100,.9],[28,100,1],[102,28,.7]]`, half-width = 8 × pressure | open, color (0.84,0.36,0.94), opacity 0.62, road: today |
| pressure ink (harness) | golden `:pressure-ink` | `[[26,72,.2],[48,36,.45],[78,84,.72],[102,42,1]]` | open, (0.16,0.68,0.96), 0.94, road: today |
| shape with a hole (harness) | golden `:holed-concave` | outer 8 points + hole 4 points, both listed in the same direction, half-width 0 | closed, even-odd, (0.94,0.32,0.18), 0.96; toggle "reverse the hole's direction" |
| closed loop crossing itself | a limaçon r = 18 + 30 cos θ, 72 samples, half-width 0 | closed, nonzero (inner loop has winding 2) |
| star (textbook case) | pentagram, half-width 0 | closed, nonzero |
| thread over and under | trefoil, 96 samples, half-width 3.2, z = −sin 3t per point | closed, no fill, road: per segment, halo 3, order: by thread height |
| draw your own | pointer input; each drag is one contour; half-width = 8 × pen pressure (0.5 for a mouse); ≥ 2.5 screen px between samples; ≤ 1500 points per contour | open |

Checkboxes: closed (last point joins the first, applies to all contours), show points.

Note: fixture widths use the harness rule `width = 16 × pressure` at zoom 1; the harness also inverse-scales positions by zoom, which the bench does not need.

## Controls

- **Road**: today: CPU triangles / per pixel, one cover / per pixel, a cover per segment.
  - Per-segment options: blend over (sum) or max (union); order as drawn, reversed, by thread height (mean z of the segment's two points, stable sort); halo 0–8 world units.
  - Today's road: "show today's triangles as wire" overlay.
- **Paint**: stroke rule ribbon + end discs / swept round nib (disabled on today's road, which is ribbon by construction); bands fill / outline only / border + gap + fill / double line; fill rule none / nonzero / even-odd (closed shapes only, not on the per-segment road); width 0.5–12 world units (band scale); opacity; smooth edges (coverage from s).
- **Formula shape**: none / rounded box / circle / box minus circle; combine formula alone / union with path / path minus formula / intersect. Box center (64,64), half extents (36,26), corner radius 10; circle center (84,58), radius 14. One-cover road only.
- **View**: zoom slider (log, 0.04×–63×), drag to pan, wheel to zoom about the cursor. In "draw your own", plain drag draws and Shift-drag pans.
- **Readouts**: at the cursor (s under the chosen rule, inside/boundary/outside at ±0.5 units, and what the other rule says; winding and crossings when a fill rule is active); pixel × segment tests this frame; today's zoom band and fan resolution; point/segment/triangle counts.

## What each road computes

### Today: CPU triangles
A JS mirror of `stroke-triangles-normalized` in `path/tessellation.cljc`:
- per segment, a quad from the two endpoints offset along the left normal by each endpoint's half-width (two triangles);
- round caps: fans at the first point from the start normal to its negation, and at the last point from the negated end normal to the end normal, counter-clockwise;
- round joins: at each interior point, a fan on the outer side of the turn; turn sign from the cross product of the incoming and outgoing directions; counter-clockwise for a left turn from −n_in to −n_out, clockwise for a right turn from n_in to n_out;
- fan steps = max(1, ceil(resolution × |Δangle| / π)) with resolution 4 (zoom < 0.1), 8 (zoom ≤ 8), 16 (zoom > 8), i.e. `legal-zoom-lods`;
- degenerate triangles dropped by an area epsilon; zero-length segments skipped (the real code throws).
Drawn with `antialias: false`, straight alpha premultiplied in the fragment shader, blend ONE, ONE_MINUS_SRC_ALPHA. Overlapping pieces blend one over another, which is the behaviour the harness declares as `:direct-triangle-double-blend-declared`.
Not mirrored: the bounding-box normalization step, hole bridging and ear clipping (closed shapes show a message instead).

### Per pixel, one cover
One quad = bounding box of all points, expanded by the largest half-width + the largest finite band edge + 2 pixels. Each fragment:
1. `s = min over ALL segments of segSigned(segment, p)` under the chosen stroke rule (below). Segments come from the point texture (RGBA32F, one texel per point: x, y, half-width, index of its contour's first point); a segment closes a contour only when "closed" is on.
2. If closed and a fill rule is set: winding number and crossing count by a horizontal ray, one loop over all segments; inside = winding ≠ 0 (nonzero) or odd crossings (even-odd); if inside, `s = −|s|`.
3. Formula shapes: `sdRoundBox`, circle, or `max(box, −circle)`; combined with the path by min (union), `max(d, −f)` (subtract), max (intersect), or used alone.
4. Paint: an ordered list of intervals `[from, to)` on s, each with a color; coverage of an edge = `clamp((s − edge)/px + 0.5, 0, 1)` when smoothing is on, a step otherwise; intervals composite front to back with premultiplied "over". Presets: fill = (−∞, 0); outline = (−w, w); border + gap + fill = (−∞, −2.5w) at 35 % alpha and (−w, 0) full; double line = (−1.5w, −0.5w) and (0.5w, 1.5w).
Blend over onto the paper.

### Per pixel, a cover per segment
One quad per segment (its bounding box expanded by the larger half-width + band edge + halo + 2 px), drawn in the chosen order. Each fragment evaluates only its own segment with `segSigned`, no winding (fills are off on this road by construction: the inside of a closed shape is not inside any segment's cover).
- Blend over: overlapping covers add up (double alpha at crossings and at every joint, since consecutive capsules overlap).
- Blend max: union for a single color, drawn into an offscreen RGBA8 layer cleared to transparent, then composited over the paper with "over" (max straight onto light paper would keep the paper).
- Halo (over blend only): a paper-colored band `0 < dSeg < halo` painted under the segment's own ink, but only where the neighbouring segments within ±4 along the path are not inked (`dNb > 0`), so the gap appears only against strands far along the path, i.e. at crossings. Order decides which strand is over.

### The stroke rule (both per-pixel roads and the cursor)
- **ribbon + end discs** (`segRibbon`): `|p − proj(p)| − lerp(hw_a, hw_b, t)` with t the clamped projection parameter. Identical to `segment-delta` in `path/component.cljc`, so it is today's CPU pick. Its zero set on the segment's interior is the straight offset line from one endpoint offset to the other, which is the quad today's tessellator builds; at the ends it is a disc.
- **swept round nib** (`segNib`): exact distance to the convex hull of the two end discs, which is the union of discs of linearly varying radius along the segment (Quilez's uneven capsule; guard for one disc containing the other returns the larger disc). Checked against the counterexample from session 6: segment (0,0)–(10,0), radii 1→4, point (5, 2.6): ribbon +0.10 (outside), nib −0.02 (inside).
- Union over segments by min in both rules. Round joins and caps come out of the union for free; no butt, square, miter or bevel exists.
- Today's triangle road is the ribbon rule by construction.

### Cost readout
A count, not a timing: one cover = cover area in device pixels × (segments, + segments again when a fill rule runs the winding loop, + 1 for a formula); per segment = Σ cover areas × (1 + 2 × 4 when the halo is on); today = triangle count.

## Unfinished, as notes

- CPU pick would need `segNib` too before pick = draw holds under the nib rule; the bench mirrors both on the CPU already, the client does not.
- Curves: polyline only. Quadratic segments need root solving in the winding loop and an iterative or subdivided distance; nothing here does that.
- Caps and joins other than round: absent under both rules.
- Fill on the per-segment road: impossible by construction; needs a tile or scanline structure.
- The halo's neighbour cut (`dNb > 0`) is not anti-aliased; the ±4 window is a constant.
- Formula covers: union uses the joint rectangle; subtract/intersect reuse the path's rectangle.
- The wet-stroke case ("draw your own") re-evaluates the whole path per pointer move on the one-cover road; the per-segment road is the shape of a wet lane but the bench still redraws all segments each frame.
- Zoom slider covers 0.04×–63×, not the client's legal 0.01–1000; the bands mirror the client's three.
- No GPU timing, no boundary epsilon from the client (the cursor uses ±0.5 world units for "boundary").
- Point texture width = point count; fine up to the device's max texture size (typically ≥ 4096).
- Colors of the harness fixtures are the fixtures' own; the three question shapes use theme-safe colors.

## Verified by session 8, 2026-09-05 17:40
Rendered headless in Chrome with SwiftShader (software WebGL2). The default road draws the self-crossing Z golden with the double-blend visible at the crossing. Selecting "pressure ink", "per pixel, one cover" and "swept round nib" renders the tapered stroke with smooth edges and round caps, cost readout 164,640 pixel×segment tests, no "shader failed to build" message. The segNib block compiles.
