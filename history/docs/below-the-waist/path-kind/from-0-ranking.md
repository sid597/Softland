# Path kind — ranking of nine responses (2026-09-05)

Criteria: problem defined before mechanism · trade-offs with conditions · hard cases worked · checked defects in client code · positions + what only Sid decides · holdable register.
Sid's prompt + one push: S1–S4. Claude-written prompt, one shot: S5–S8. Me: post-catch turns only.

1. S8 (mine) — hunters + fact base; checked: five places decide pixel size with different rules; band-texture width compile-time constant vs asset metadata; pressure-ink fixture never run; harness is the only maker/caller; stroked rectangle / filled ink loop not expressible; f32 jitter at zoom 1000. Three change rates (edit / camera scale / frame), code bakes per-scale into per-edit key. Wet/dry lanes. Four-shape renderer table; MSAA memory ↔ local-agents question. Position: settle contract, Slug-shaped lane online + capsule wet lane; exits named. Lossless spline (one anchor per sample, exact to cubics) dissolves gesture-vs-curve.
2. S4 (Sid's) — "renderer convenience never leaks into the model"; pick even-odd / text nonzero / clipper throws; harness golden shows darkening; bench; vector-vs-raster fork; refused to invent the marks list.
3. S7 (mine) — PostScript stroke = special case of a swept tip; three things the glyph route never needed (tolerance home, cover policy, wet route); cascade if stored form changes; positions with exits; page + fact bases. Paste garbled.
4. S2 (Sid's) — one page, two ends, wet/dry, exact image + slow reference renderer, list every reader; checked: no live input, no reference image, re-tessellate per sample, cache never evicts, width stored / pressure optional; three questions only Sid answers.
5. S6 (mine) — S5 + Skia conics, polar stroking, brush-program hidden work, decisive test cases, pen-to-pixel latency metric. Dense.
6. S5 (mine) — hidden work behind "stroke"; ribbon vs nib; vector vs painting worked (0.5 vs 0.75); ECS brush record ≠ data-defined brush. Dense.
7. Me (post-catch) — three camps; settled vs contested; five-layer map; rate rule; inputs sorted by layer; four renderers, zero shared vocabulary. Missing: definition before vocabulary, stroke=fill flat, no hard case, no Sid-only list.
8. S1 (Sid's) — preserve-meaning definition; "two results the input cannot distinguish" test; invariants.
9. S3 (Sid's) — same family as S1, less concrete.

Prompt effect: Sid's prompt + push → places 2, 4, 8, 9. Claude prompt one-shot → 1, 3, 5, 6.

## Agreement across all nine
- geometry / region (fill|stroke) / paint / composition separate; closure ≠ fill
- self-crossing = fill rule + union-vs-accumulate + weave needs data the pen never produced
- "how we got the points doesn't matter" true for drawing, false for the model
- Slug = feasibility evidence, sharing at coverage only; the shader knows no glyphs, the glyph-ness is the offline bake
- CPU vs GPU = where the inside test is decided, per stage / per change rate, after semantics
- current grammar fitted to the tessellator
- (S5–S8) the polyline is a valid path; the leak is the zoom band in cache/frame key + the double blend, not the samples in the component

## Divergence — updated
1. truth of a pen mark: S8's lossless spline dissolves most of it; remaining: reduce at store time? (S8: never; keep pen record beside)
2. ribbon vs swept nib: S7, S8 pick swept tip (CPU classify already computes that union); S5, S6 leave to Sid
3. marker vs ink: S7, S8 default one region no darkening; darkening = declared brush semantic
4. painting: S8 raster kind beside path kind; S5, S6 brush-deposition op on same waist; S7 defers
5. below the waist: S8 Slug-shaped lane + capsule wet lane; S7 one shared filler; S2, S4 pixel test; exits differ
6. Sid only: what marks people make in Softland; does a stroke change after dry

# Revision after eight peer rankings (same day)

Inputs: peer critiques verified by arithmetic; prompt bias (5–8 started inside the frame; Fable rider induced over-commitment, Astra rider induced density); camp pattern (Codex four: identical top-4 all Codex; Claude four: all put S8 first).

Verified errors:
- S8: "current width rule = union of swept discs" is false. Counterexample (S5): segment x 0→10, r 1→4, point (5, 2.6): projection rule outside (r 2.5, d 2.6); disc at x=6 r 2.8, d 2.79, inside. Current code = ribbon with discs at samples. S8 conceded. One-paragraph fix.
- S4: "a stroke never has holes" false (stroked circle is a ring); single SDF for fill and stroke too strong; min/max of SDFs not exact distance.
- S2: three numbers per pixel cannot carry several passages through a crossing; "exact at every zoom / AA free / hit never disagrees" overclaimed; "text does not transfer" withdrawn by author.
- S7: "zoom leaves every key, unconditionally" too strong (derived caches may key on scale; authored geometry is camera-free); swept round tip does not give butt/miter, so PostScript stroke is not strictly its special case.

Revised order (peer avg in parens): 1 S6 (2.5) · 2 S8 (3.0) · 3 S5 (2.8) · 4 S1 (4.8) · 5 S7 (4.9) · 6 S4 (6.4) · 7 S2 (6.1) · 8 S3 (5.4) · 9 me. Top three close; gap to the rest is not.

Settled by all rankers: fill/stroke are declarations, never kinds · self-crossing stroke = one region once, darkening is a declared brush semantic · samples are a source, curve derived, pen record kept · CPU vs GPU is not the axis · text shader glyph-agnostic, bake glyph-specific.

Definition to write first: varying width = ribbon (current code) vs swept round nib (S7, S8, S4-ranker) vs both named (S5). Nothing is agreed by construction until this is.

# FINAL — exploration criterion (Sid: framing and how-to-think over correctness; details fixable)
1 S8 · 2 S2 · 3 S4 · 4 S7 · 5 S6 · 6 S5 · 7 S1 · 8 me · 9 S3
Fix list carried separately (not rank penalties): S8 capsule≠swept-nib; S4 "no holes", single SDF, min/max; S2 three-number evaluator; S7 zoom-unconditional, swept tip ≠ miter/butt.
