# The Waist Bench (bench 9) — handover, session 9, 2026-09-06

## Where it is

- Source: `docs/below-the-waist/path-kind/bench-9/waist-bench.html` (one file, ~95 KB, WebGL2, no build; only a Google Fonts link, with fallbacks).
- Live: https://claude.ai/code/artifact/da84fdb6-604a-488e-b797-e77c6f257cbc . To republish from another session: `Artifact` action `read` with that URL first, then publish with `url` set and this file as `file_path`.
- Run locally: open the file in any WebGL2 browser (`file://` is fine). Headless check: `google-chrome --headless=new --use-angle=swiftshader --use-gl=angle --enable-unsafe-swiftshader --window-size=1500,1000 --virtual-time-budget=4000 --screenshot=out.png "file://…/waist-bench.html#tool=pen"`; `--dump-dom` instead of `--screenshot` prints the panel's readouts.
- Deep links: the URL hash carries the whole state, e.g. `#tool=pressure&tip=ribbon&outline=1&zoom=6`. Keys: `tool`, `tip`, `cap`, `join`, `align`, `unit`, `dash`, `snap`, `fill`, `stroke`, `cover`, `bands`, `skin`, `outline`, `zoom`, `cx`, `cy`.

## What it is

The waist test as a thing to feel. The right column is laid out as the waist: the tool's record above (data, editable JSON), the kind's value on the line (derived, read-only), the pieces of code below, each reporting what it did and at which rate (per edit / per scale / per frame). The canvas draws the result through one pipeline: source → path → envelope → packer → filler. The filler is the text lane's per-pixel program (`text/renderer.cljs:139-264`, the Slug algorithm) in GLSL, with two changes: the band texture width is a uniform rather than a compile-time 4096, and even-odd is the parity of the same crossings.

Local units: 1 unit = 1 CSS pixel at zoom 1, Y down, as in the client. Presets sit in a 0–128 box; the view opens at zoom 3 centred on (64, 64).

## The record (what a tool supplies; the definer's side of the waist)

```
{
  "tool":   { "name": "...", ...numbers the width expression may read (size, thinning, ...),
              "streamline": 0..0.95, "fit": τ | 0 | "polyline", "taperStart": n, "taperEnd": n,
              "simulatePressure": bool, "width": "expression over p, s, v and the tool's numbers" },
  "source": { "kind": "pen",     "samples": [[x, y, pressure?, t?], ...] }
          | { "kind": "anchors", "contours": [ { "closed": bool, "anchors": [ { "id"?, "p": [x, y], "in"?: [x, y], "out"?: [x, y] } ] } ] }
          | { "kind": "rect",    "x", "y", "w", "h", "r" },
  "paint":  { "fill":   null | { "rule": "nonzero" | "evenodd", "color": [r, g, b, a] },
              "stroke": null | { "on"?: bool, "tip": "nib" | "ribbon", "width": number | "knot",
                                 "unit": "local" | "device", "cap": "round" | "butt" | "square",
                                 "join": "round" | "miter" | "bevel", "miterLimit"?: n,
                                 "align": "center" | "inside" | "outside", "dash"?: [on, off],
                                 "overlap"?: "union" | "accumulate", "spacing"?: n,
                                 "color": [r, g, b, a] } },
  "snap": bool,
  "identity": { "id": "...", "revision": n }
}
```

`width: "knot"` means the per-knot widths the source produced (for a pen: the width expression per sample). `width: "<expression>"` on the stroke is the definer's fix from attack 1: the expression is evaluated per flattened point on the *interpolated pressure* (and `s`), so a nonlinear response is not the interpolation of squared knot widths. `overlap: "accumulate"` is the definer's construction: round dabs at arc lengths 0, spacing, 2·spacing … each its own region, painted in drawing order through the same filler; `union` is one region painted once. The expression evaluator accepts `+ - * / ^`, parentheses, `min max abs sqrt pow clamp step smoothstep mix sin cos exp floor`, `pi`, the names `p` (pressure), `s` (arc-length fraction 0..1), `v` (speed, units per ms) and every numeric field of `tool`. A parse error is shown under the record and the width falls back to `size * p`.

## The pipeline (what executes; the composer's side)

1. **source → path** (per edit). Pen: streamline (a low-pass, tldraw's name), simulated pressure from speed when asked, the width expression per sample, taper, decimation at τ (Ramer–Douglas–Peucker), Catmull-Rom cubics through the knots (τ 0 keeps every sample; `"polyline"` emits lines). Rect: 4 lines + 4 arcs as cubics (κ = 0.5523). Anchors: a copy. Knots keep ids (sample indices or anchor ids) and widths.
2. **the value**: `{ subpaths: [ { closed, start, segs: [L | Q | C], knots: [ { id, w } ] } ] }` plus the paint and identity from the record. No tolerance, no zoom.
3. **geometry** (per edit; per scale when the width is in device pixels; per frame when snapping). The envelope tracer: the centerline is flattened at τ 0.1 local (or 0.25 device px for device-unit widths), then one closed outline is traced per open piece (side A forward, end cap, side B backward, start cap) or two loops per closed piece (alignment picks which two: the path itself and its inward offset, the two half-width offsets, or the outward offset and the path reversed, wound opposite). Tangent points per piece: **ribbon** = knot + r·(perpendicular of the piece); **nib** = knot + r·(external tangent normal of the two end discs), which leans with the taper. That is the one line where the two constructions differ. Outer joins: round arc (≤45° per quadratic), miter with a limit, or bevel. Inner joins: a fold through the pivot; it keeps the body's orientation so nonzero absorbs it and even-odd punches it out. Caps: round (an arc through the end direction), butt (the chord between the tangent points), square. Dash: a walk by arc length over the flattened centerline; each dash is an open piece with caps.
4. **packer** (per scale; per frame when snapping). Cubics → quads at τ = 0.25 device px (split count from the third-difference bound). Row and column bands, `ceil(sqrt(n/2))` each, curves sorted by far edge descending so the shader's early exit holds. Cover: one box, or cells the outline's boxes touch plus interior cells (a CPU winding test at the cell centre). Textures: RGBA32F curves, two texels per quad; RG32UI bands, header (count, offset) per band then the index lists; width 1024, wrapping. Snap: knots moved to the device grid, handles carried along, then everything re-runs.
5. **filler** (per frame). The shader above. One coverage draw per painted region; fill first, then the stroke's skin; premultiplied over. The skin's rule is nonzero by definition; the bench lets you switch it to even-odd to see the folds.

## Readouts

- Each piece: what it did, counts (samples → knots → cubics; pieces → outline lines + arc quads; cubics → quads; bands and curves per band; cover rects and device pixels; texels), milliseconds, and its rate badge.
- The filler: ≈ cover pixels × (curves per row band + curves per column band) per region, a count not a timing.
- The cursor: local coordinates; per region the integer winding and the fractional coverage from the CPU twin of the shader, evaluated at the centre of the pixel under the cursor; the composited alpha against the GPU pixel read back; "agree" within 0.05. For strokes, the signed distance to the drawn centerline under both constructions (session 4's `segRibbon` and Quilez's uneven capsule for the nib) with a flag where their signs differ.
- Show outline curves: the packed quads of every region as thin lines (fill in the data colour, skin in the code colour) and the path's knots as points.

## Fixtures

| Preset | Record | What to look at |
|---|---|---|
| tldraw's draw tool | 70 pen samples along a limaçon with synthetic pressure and times; tool size 10, thinning 0.6, streamline 0.35, fit 0.9, taperEnd 22 | union at the self-crossing at 85 % alpha; switch tip; edit `fit`, `taperEnd`, `width` |
| Figma's pen tool | 12 anchors with Hermite handles on the trefoil from bench 4, closed; fill nonzero; stroke 3, miter, butt | fill rule toggle on the lobes; align inside / outside |
| a UI border | rect 24, 36, 80×52, r 6; fill; stroke 1 device px, miter, inside; snap on | zoom: one pixel stays one pixel; snap off greys the edge; rate badges go per scale / per frame |
| star (textbook) | pentagram as anchors; fill nonzero; stroke 2.5 miter | fill rule at the centre |
| self-crossing ink (harness) | the golden `translucent-self-crossing`, width 16 × pressure, polyline | the crossing painted once at 62 % |
| the crossing, as dabs (definer) | the golden `translucent-self-crossing` with `overlap: accumulate`, spacing 12, radius 8 × pressure | attack 1's hard case: 24 dabs, two on the crossing, alpha 0.62 + 0.38 × 0.62 = 0.8556; switch overlap to union for 0.62 |
| pressure ink (harness) | the golden `pressure-ink` | Position 7: switch tip; the cursor shows both distances |
| shape with a hole (harness) | the golden `holed-concave`, both rings the same direction; fill even-odd | switch to nonzero; reverse the hole's anchors in the record |
| draw your own | empty pen source; simulatePressure on | the wet route: the tail rides raw, the rest is fit as you draw; pen-up fits once; Shift-drag pans |

## Measured on 2026-09-06 (headless SwiftShader, zoom 3, DPR 1)

| Preset | value | envelope | packer |
|---|---|---|---|
| draw tool | 25 cubic, 26 knots (70 samples) | 98 pieces → 391 lines + 101 arc quads, 1.1 ms | stroke 491 curves, bands 16×16 (≈42 per band), cover 35,444 px, texels 982 + 1378 |
| pen tool | 12 cubic, 13 knots | 84 pieces → 2 loops, 504 lines | fill 24 quads; stroke 504 curves, bands 16×16 (≈39 per band) |
| border | 4 cubic + 4 line | 24 pieces → 2 loops, 76 lines (per scale) | fill 12 curves; stroke 84 curves; snapped: repack per camera move |
| harness Z | 3 lines, 4 knots | 3 pieces → 10 lines + 16 arc quads | 26 curves, bands 4×4 |
| harness pressure | 3 lines, 4 knots | 3 pieces → 10 lines + 15 arc quads | 25 curves |
| the crossing, as dabs | 3 lines, 4 knots | centerline length 281.94 → 24 dabs of 8 arc quads | 24 small packs, 46,032 device px covered in all |

## Unfinished, as notes

- The envelope flattens the centerline before tracing, so a curved stroke's skin is many short lines rather than a few offset curves: about five to eight times the curves an offset stroker would emit, and the memory line on the page (near 3 KB per stroke) assumes the stroker. Facets show at extreme zoom. Arcs (caps, round joins) are real quadratics.
- The nib's butt cap on a taper is the chord between the tangent points, not perpendicular to the centerline. PostScript's butt is the constant-width case.
- Where one end disc contains the other (|r_a − r_b| ≥ piece length) the nib's tangent points collapse; the outline is approximate there.
- The wet route re-runs the envelope and the pack for the whole stroke on every pointer move (the readout shows the milliseconds); only the fit is incremental. Two regions (dry + wet) would double-blend at their seam, which is why the bench keeps one region.
- No clip by a path (the mask), no arrow caps (needs `tangent(t)`), no sweater depth (bench 4 has the halo), no gradients, no surface-reading brush (the definer's smudge recurrence needs surface state the bench does not have).
- Dabs are separate regions and separate draws (24 packs for the fixture); a real lane would batch them into one instanced draw with painter's order preserved.
- Cover cells mode tests every cell against every curve's box on the CPU at pack time; fine here, not a design.
- Zoom range 0.05–100, not the client's 0.01–1000; the CPU twin runs in doubles while the GPU runs f32, so agreement can drift at extreme zoom.
- Even-odd with antialiasing uses a triangle wave over the sum of absolute crossing contributions; exact away from edges, an estimate on them.
