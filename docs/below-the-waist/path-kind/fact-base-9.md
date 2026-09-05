# Path kind — fact base, session 9 (the composer's chair, 2026-09-06)

Session 9 inherited the picture from session 8 (`HANDOFF-8.md`) and did the first item on the still-owed list from the code side: the waist test, three real tools walked through the picture as data to see whether any needs new code below the waist. It built bench 9 to make the test feelable, folded the findings into the page, and left the definer's data-side walk to meet at the value. No ruling from Sid this session; everything here is POSITION, CHECKED, DERIVED or FIELD as marked.

## Part 1 — what was checked in the tree this session (fence: src/app/client/ only)

- `text/renderer.cljs:100-264` (read directly): the Slug fragment shader. `calc_root_code` from three sign bits and the constant 0x2E74; `solve_horiz_poly` / `solve_vert_poly` with the near-linear fallback at 1/65536; `calc_band_loc` wrapping at a compile-time width of 4096 (`kLogBandTextureWidth = 12u`); `calc_coverage` as the weighted combination with the minimum-axis safeguard; `slug_render` with `fwidth` for the pixel scale, band lookup, two loops, and the early `break` when a curve lies entirely before the pixel. The 100-byte instance stride at `:266`. Bench 9's GLSL mirrors this line for line; the two deliberate changes are the band width as a uniform and the even-odd branch.
- `text/glyph_pack.cljs:58-64` and `text/fonts.cljs:135-172`: banding data is fetched as bytes (`:band-bytes`) from the offline bundle; no band builder exists in the client. Bench 9's band builder follows the published Slug scheme (uniform strips, per-band lists sorted by far edge descending) and is the first one written for this tree, in JS.
- Fixture coordinates reused from `bench-4/ink-bench.html:240-281`, which mirror `harness/path.cljs` goldens: `translucent-self-crossing` `[[26,28,.65],[102,100,.9],[28,100,1],[102,28,.7]]`, `pressure-ink` `[[26,72,.2],[48,36,.45],[78,84,.72],[102,42,1]]`, `holed-concave` outer 8 + hole 4, all with width = 16 × pressure; the pentagram and the trefoil are bench 4's own.

## Part 2 — what bench 9 measured (headless Chrome, SwiftShader, zoom 3, DPR 1; `bench-9/HANDOVER.md` has the table)

- The tree's text shader, lifted unchanged except two lines, filled every fixture: a pen stroke's traced skin, a cubic path under both rules, a one-device-pixel border ring, the three harness goldens. No shader build failure in SwiftShader.
- Draw tool: 70 samples → 26 knots → 25 cubics; envelope 98 pieces → 391 lines + 101 arc quads = 491 curves; bands 16×16 with ≈42 curves per band; cover 35,444 device px; ≈3.0 M curve tests per frame by the count; envelope 1.1 ms, pack 2.5 ms in software.
- Pen tool: fill 12 cubics → 24 quads (4×4 bands, ≈8 per band); stroke ring 504 lines.
- Border: fill 12 curves, ring 84 curves; with snap on, the pack re-runs on every camera move and the bench says so.
- CPU twin against the GPU pixel: agreement within 0.05 at every cursor position tried by hand on the author's machine is NOT claimed; the mechanism is the same curves, bands, root code and rule, so disagreement would be a bug in one port, not a definitional gap. The readout exists so Sid can test it.

## Part 3 — derivations

- Curve and byte counts (DERIVED from Part 2): the draw stroke's skin as traced = 982 curve texels × 16 B + 1378 band texels × 8 B ≈ 27 KB at τ 0.1 local, valid at every zoom. The page's earlier line (near 3 KB per hundred-sample stroke) assumed an offset stroker emitting a few quads per cubic per side; with 25 cubics that is roughly 150–200 quads ≈ 3–4 KB. So: the claim survives with the stroker, the number does not survive the flatten-then-trace shortcut. Today's mesh is ≈26 KB per zoom band (from session 7's arithmetic), so the shortcut costs about one band's worth and removes the bands.
- The fold at inner joins keeps the body's orientation (DERIVED by hand: a right-angle left turn, width 4; the outline runs …→(0,2)→(0,0)→(−2,0)→… and the lobe X→R2→P→R1 is clockwise like the body, so winding is 2 inside the fold, never 0). Hence nonzero absorbs folds and even-odd punches them out, as the page said; the bench shows both.
- Alignment as a ring (DERIVED): inside = [path forward, inward offset traced backward]; outside = [outward offset forward, path reversed]; centre = [half-width offset forward on one side, backward on the other]. Opposite winding leaves the middle at 0 under nonzero. Which side is inward comes from the flattened centerline's signed area; ambiguous for self-crossing closed paths (the trefoil, the star), which the bench does not hide.
- Which side is outer at a join: with `perp(u) = (−u.y, u.x)`, side s is outer when `s · cross(u_in, u_out) < 0` (DERIVED and confirmed visually on the pressure fixture: fans on the outside, folds on the inside).
- The two constructions in one tracer (DERIVED): tangent normal `m = c·u + s·√(1−c²)·perp(u)` with `c = (r_a − r_b)/d` for the nib, `c = 0` for the ribbon. For constant width they coincide; on a taper they diverge by `asin(c)`.

## Part 4 — FIELD claims made this session (unverified here; the definer may pin them)

- perfect-freehand (tldraw's draw tool): offsets each point perpendicular to the smoothed direction by a radius from `size` and `thinning` (`size · easing(0.5 − thinning · (0.5 − pressure))`), adds round arcs around a point only at sharp turns, caps by option, and low-passes input by `streamline`; `simulatePressure` derives pressure from speed. That is the ribbon with round joins. tldraw's draw shape also carries `dash` styles (solid, dashed, dotted, draw).
- Figma's pen tool: anchors with handles in a vector network; strokes carry weight, cap (none, round, square, arrow variants), join (miter, bevel, round), align (center, inside, outside), dash; fills carry a rule per region; "use as mask" is a clip.
- CSS borders sit inside the box; `border-width: 1px` is a device-pixel width under browser zoom and is snapped to the device grid by every browser engine.
- Skia's stroker (`SkStroke`) offsets curves themselves and connects inner joins through the pivot; Slug (Lengyel 2017) is the band scheme the tree's text shader implements; Quilez's "uneven capsule" is the exact distance to the hull of two discs.

## Part 5 — the waist test, from the code side (POSITION; on the page as a section)

1. All three records compile to the same value; nothing refused.
2. The same pieces drew all three; the filler is the tree's, two lines changed.
3. Pen tool: no code above the value; the waist sits at the tool.
4. Draw tool: the most code; one step is a function written as data (width from pressure). A function is data only if something runs it: the **evaluator**, a fifth piece, beside the builder in the source stage. Not on the page before.
5. Border: width has a **unit**; a device-pixel width makes the envelope per scale; **snapping** is per frame because it depends on the pan.
6. Pen tool's inside/outside: the **offset(path, d)** answer; the aligned stroke is a ring, wound opposite; no clip needed.
7. tldraw ships the ribbon; on the bench the two constructions are one line apart; Position 7 stands as a default with the other construction nameable.
8. No new piece under the value except the evaluator; offset and dash join the geometry, unit and snap join the packer's rates.

Corrected picture of the waist itself: the records are data; the source stage (builder, generator, evaluator) is code; the kind's value is one step lower. Figure 1 already drew the builder below the line; the test made the whole stage visible.

## Part 6 — fix list carried (never rank penalties)

- Envelope: offset the curves, not the flattened polyline (cost ×5–8 in curves; the memory line).
- Nib butt cap on a taper is the chord between tangent points.
- Degenerate nib pieces (one disc inside the other) are approximate.
- Wet route: incremental fit only; envelope and pack re-run per event; a two-region wet/dry split would double-blend at the seam.
- Even-odd antialiasing on edges is an estimate (triangle wave over absolute contributions).
- Not on the bench: clip by a path, arrow caps (`tangent(t)`), sweater depth, gradients, cover cells as a real design.
- From `from-0-ranking.md`, status: S8 capsule≠nib — folded (Position 7, stroke row, bench); S7 zoom-unconditional — folded (Position 4); S7 swept tip ≠ miter/butt — folded on bench 9 as outline-level constructions, with the butt-on-taper caveat above; S4 "no holes" / single SDF / min-max, S2 three-number evaluator — those were S4's and S2's pages, not this one; bench 9 uses winding, not SDF min/max, for fills.

## Part 7 — the meet: the definer's attack 1, read and folded (2026-09-06, same day)

`attack-1.md` landed in the directory while the bench was being built; neither chair had read the other's. Read in full; folded at full weight; nothing ranked.

Convergences (both sides, independently): the fifth piece (composer: an expression evaluator; definer: a construction executor with arithmetic, conditions, iteration, ordered accumulation, record updates, calls to geometry and surface, and a return arrow from geometry to constructions); the missing answers (composer: offset, length, tangent; definer: evaluate, length ↔ t, source correspondence, intersections, splits with id maps); the value survives all three tools; the per-edit / per-scale / per-frame axis.

The definer's contributions the composer did not have, now on the page: width as a function of the interpolated source attribute (16p² at the crossing's two visits is 9.6514 and 11.6145, not the interpolation of squared widths); accumulation as ordered dabs through the same filler (24 dabs on the harness Z, alpha 0.8556 at the crossing), with surface state only for brushes that read the surface; three tolerances (source fit, consumer request, device); membership as the one shared definition, with distance and coverage compared at a declared error; cover cells must include the interior; the network as the authored source with intersections and splits as answers; the crossing edit as (segment, parameter) pairs; wet and dry as one region; unequal border widths as a ring built from the box with CSS's inner radii; per-side colours need the corner divided; a device width under shear needs the full transform.

Built onto bench 9 from the attack: `overlap: accumulate` (the dab emitter; the fixture "the crossing, as dabs" gives 24 dabs on a centerline of length 281.94, matching the attack's arithmetic) and `width: "<expression>"` on the stroke, evaluated per flattened point on interpolated pressure.

Its Positions A to D read as constructions and hold with the page's: A (keep the common output; expose the executor and the geometry's returns) is Position 8 plus the return arrow; B (accumulation reuses the filler; surface-dependent painting needs ordered state) is now the compositing row and Position 2; C (keep source meaning; allow a frozen result) is Position 1; D (expose geometry by returned values before assigning a home) is the answers list and the "shared computation does not determine placement" line from session 3.

## Part 8 — what the definer can do with bench 9

- Paste a record for any of its three tools into the textarea; the pieces below say what ran. If a field of the record has nowhere to go, that is a finding.
- Session 5's change-it moves as bench actions: change the nib (tip toggle) or the pressure response (the `width` expression); edit the centerline after drawing (edit anchors or samples in the record); use the same geometry as a mask (not on the bench: a finding to carry); a thread over and under (not on this bench; bench 4's halo); save the construction so someone else adapts it (the record is the construction; the URL hash is the saved state).
- The counterexample from the round: pressure-ink fixture, cursor at a taper, both distances shown, sign disagreement flagged.
- Its own hard case: the fixture "the crossing, as dabs"; the cursor at the crossing reads the composited alpha on the CPU and the GPU pixel; switching overlap to union gives 0.62.
