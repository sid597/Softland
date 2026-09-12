# From session 7 to session 8 — restart brief contribution

Written 2026-09-05, on session 6's ask, before session 7 closes. Everything session 7 produced is in this directory. Nothing here has landed in the repo.

## Files

| File | What it is |
|---|---|
| `path-kind-waist-7.html` | The page, editable source, current as of the post-round revision. Two hand-drawn SVG figures inside (viewBox 1180×760 and 1080×700, `currentColor`, theme-safe). Published at https://claude.ai/code/artifact/38bcc965-844b-4a6e-8841-5fd8a25fbb6c |
| `path-kind-waist-7.md` | Plain-text twin of the page for reading; figures are placeholders, read the html for those |
| `hunt-path-7.md` | Fact base: path/component, tessellation, frame, renderer, harness/path — five-question cards, grammar, tessellation steps and bands, 28-byte vertex + shaders verbatim, CPU classify, fixtures, curves/pressure absence |
| `hunt-text-7.md` | Fact base: text/fonts, shaper, glyph_pack, renderer, shaped_line — the Slug per-pixel walk anchored (renderer.cljs:194-256), texture formats, the 100-byte instance layout, what is glyph-specific vs general, the 4096 band-width constant (127, 181 vs meta 319-322) |
| `hunt-engine-7.md` | Fact base: engine/*, region3d placed ink, harness frame drivers — rungs are NOT zoom (offscreen admission), camera + group buffers, the renderer contract across the four kinds (agreements/differences), the placed-ink call chain, shared WGSL |

## Ledger: what Sid decided vs what is proposed

Sid's words, so they do not get promoted or demoted by either of us. Times are from recall where I have them; the afternoon ones are approximate.

Decided / adopted by Sid:
- 2026-09-05 08:38 (recall uuid caca9242): "Previously we did it in the format the GPU can consume, and now we need a format that a curve can consume. That's sorted." — the stored form is curve-consumable. His stance, stated as sorted.
- 2026-09-05 09:54 (this session's starter): "Working basis I have adopted and want attacked rather than assumed: the standard imaging model that has held since PostScript ... Boundary and fill are independent declarations; the code must not presume fill." — adopted as a working basis, explicitly open to attack. Also the fence: "Do not read any docs or code files outside `client/`."
- 2026-09-05 ~16:50: "yeah fuck the correctness and penalising it ... framing and how to think is much more important in exploratoion phases" — ranking and review criterion for this phase. Now in memory as feedback-exploration-rank-framing-over-correctness.md.
- 2026-09-05 ~17:10: "its going to be 8 and 6" — the team.

NOT decided by Sid (proposals; on the page each is marked POSITION):
- P1 store the burst raw, derive the path at a declared tolerance (tolerance zero = interpolating spline, 8's Catmull-Rom form).
- P2 imaging model as the OUTPUT language; stroke generalised to the swept tip; raster brushes a separate lane.
- P3 one filler shared with text; strokes via envelope.
- P4 zoom leaves the stored value and its identity; per-scale caches are the renderer's, keyed on projected error (revised after the round; the first form "every key, unconditionally" was wrong).
- P5 cubic in the stored path, quads at the packer.
- P6 placed ink on the coverage lane on the plane; the 88-byte mesh road cut (revised; the first form kept triangles "until better", which Sid's law forbids: feedback-dead-means-wrong-form-not-uncalled.md).
- P7 stroke region = swept round nib (union of discs of radius w(t)/2); caps/joins declared on top; the CPU projection rule named as the ribbon approximation. THE OPEN FORK. Sid's to close, verbatim, timestamped (feedback-rulings-quote-sid-verbatim.md). Ask once.
- The "three tiers" and "stroke is a fill of an offset region" from this morning were Claude's replies (uuids 6b009826, 28d31058, 418cb10c), not Sid's words. His words in that session were the questions.

## What is on the page that I did not see in 8's reply (from the paste)

Constructions and arguments:
1. The imaging model is the OUTPUT language and the wrong STORAGE language: its stroke has one width, a pen has one per sample; PostScript's stroke is the special case where the tip never changes; the stroke declaration is designed from ink.
2. Why the envelope is always filled NONZERO: the skin of a curved or turning stroke folds over itself on the inside of every bend and at every join; nonzero is what turns overlapping pieces into one region, so the envelope builder may be sloppy as long as orientation is consistent. Even-odd on a skin punches holes.
3. The three things glyphs never needed and ink needs from day one: where the screen-space tolerance lives (packer/flattener, never the value); a cover policy for a long thin outline in a huge bbox (cells touched by the outline, computed in local units at pack time); a cheap route for the wet stroke while the pen is down.
4. Band building at edit time as NEW code: fonts get banding offline outside client/; a user path must be banded at pen-up, in cljc, fast.
5. The attack table, claim by claim: HOLDS / DEFAULT / SHORT, each with the condition under which it flips. This is the form Sid can rule on.
6. Cascade table, file by file, today (checked) → becomes.
7. Memory arithmetic, derived from checked layouts: a 100-sample stroke ≈ 310 triangles ≈ 26 KB per zoom band today; as ~70 quadratics + bands + one 100-byte instance ≈ 3 KB, zoom-free. The GPU-memory question lives in render targets, not path data (8's fork table places it on the MSAA road; same conclusion).
8. Sid's seven questions answered one by one, short, with anchors.

Checked facts (in the hunt files) that may not be in 8's base:
- Harness parity skips pixel centres with `boundary-distance·zoom > 1.25` (harness/path.cljs:341-393) and runs only on the holed shape; ink parity is never checked.
- Renderer contract across the four kinds as a table: all take device + fformat + camera-buffer + groups-buffer; all draw into an open pass except region3d's encode-region-pass! (takes an encoder); group index baked at prepare in all four; zoom arg only path and region3d; text packs every call with no equality cache; text draw needs attachment-size for scissor.
- Text's `draw-text-system!` has NO caller; the harness draws text by hand.
- `slug_render(render_coord, band_transform, glyph)` reads no font size and no glyph id (text/renderer.cljs:194-256); em-box and glyph-id assumptions live in glyph_pack (186-201) and the offline bake.
- Region3d placed ink: shares the path renderer's `!mesh-cache` object (renderer.cljs:1116-1123), fixed placement zoom 1.0 (on_plane.cljc:20), 88-byte vertices with mat4 per vertex, 4× MSAA, depth test no-write, one/one-minus blend; only :ink placeable; placed text exists on CPU with no caller.
- Compositor: no blend modes of its own, no stencil/mask, rect scissor only (apply-scissor! 612-629).

Diagrams:
- Figure 1 (should-be): sources row → one input box (path / paint / identity) → waist line → four code boxes (path type · geometry · packer · filler, filler shared with text) → outputs (CPU answers left, GPU coverage draws right) → engine strip. Glyphs bypass the path type via a dashed route into the packer.
- Figure 2 (today): harness fixtures → component → tessellation → renderer → GPU, with region3d placed ink as a consumer and the text lane (slug filler + glyph packer) alongside; red marks on the leaks. Both are inline SVG; lift them.

## What changed on the page after the peer round (so you know what is already fixed)
- Stroke row now names ribbon vs swept nib and anchors the CPU rule (component.cljc:176-190) as the ribbon.
- Trade-off on precision corrected (was "nothing new"; now the f32 narrowing, credited to 8's hunter, not re-read by mine).
- P4, P6 rewritten; P7 added.
- New section "Folded in from the other seven sessions": thirteen items, each named to its finder, anchored where checkable.

## What 8 has that the page only carries second-hand (own the anchors)
f32 narrowing without rebasing; the five pixel-size decisions; the renderer fork table with memory; the capsule wet lane; the Catmull-Rom exactness argument; "no shape generator anywhere"; the grammar cannot express a stroked rectangle or a filled ink loop (component.cljc:15, 89-92).

## Pointers
- recall: session "Client folder structure and purpose" 2026-09-05 07:48–08:38, prompt uuids 7caba2dd, 255b8f3e, caca9242 — Sid's own words on the burst, CPU vs GPU, the sweater, the tiers.
- memory: feedback-exploration-rank-framing-over-correctness.md, feedback-dead-means-wrong-form-not-uncalled.md, feedback-rulings-quote-sid-verbatim.md, feedback-waist-sort-hunters-code-only.md, feedback-code-maps-one-notch-in-out-why.md (page order: picture before story; at Sid's word the page lands as .md + .html twins under docs/).
- Landing rule: nothing in the repo until Sid says; when he does, twins under docs/ so it can be contested on disk.
