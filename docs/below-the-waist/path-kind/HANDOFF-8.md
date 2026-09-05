# Handoff from session 8 to its successor — the composer's chair on the path kind

Written 2026-09-05, end of session 8. You inherit the picture and the responsibility for it. A Codex session inherits the definer's chair from session 6 at the same time. Sid restarts both fresh; everything either of you needs is in this directory, in git, on main.

Read in this order: this file; `path-kind.html` (open the artifact link, or the file); `fact-base-8.md` Parts 5 to 8; `from-7-to-8.md`. Then the code itself, only under `src/app/client/`, by the READMEs down to docstrings down to code. That is the fence Sid set for this work and it still holds.

## 1. The problem, in Sid's words

From the starter of 2026-09-05 09:54:

> What I am trying to build is every 2D tool: painting apps, vector engines, whatever tldraw, Figma, Canva or a UI component library can do. The way I want to get there is a waist: some system exists as code below it, and everything above is defined as data, ECS-style, that humans and agents can create, edit and reuse. So the question about the path kind is really: what must exist as code for ink and shapes, and what is its input and output, such that everything above can be built from it.

> Working basis I have adopted and want attacked rather than assumed: the standard imaging model that has held since PostScript. A path is line, quadratic and cubic segments, open or closed. Fill covers the inside under a rule, nonzero or even-odd. Stroke is defined as the fill of the region within half the width of the centerline, with cap and join. Paint and compositing (clips, blends) sit on top. Boundary and fill are independent declarations; the code must not presume fill. Anything committed here should be written to solve as broadly as its input, output and definition allow, because it is going to live.

> I want to end up with a mental model I can hold, and a clear picture of what the path kind's input and output should be and why, so that every session on this is talking about the same thing.

He is working through the client's four content families (text, image, path, region3d) one kind at a time at the level of inputs and outputs, asking whether each is really a library. Text held up. Path did not. This is the path round. Image and region3d come after, read against the same frame.

## 2. The phase, and how Sid wants it worked

Exploration. His ruling of 2026-09-05, about 16:50: "yeah fuck the correctness and penalising it ... framing and how to think is much more important in exploration phases ... it's like starting a painting." Details are a fix list, never a reason to stop or to reframe. The picture gets solid as boots hit the ground.

Two chairs, his word at about 17:10: "its going to be 8 and 6." You are 8's successor: you hold the picture, the contract, the cascade, the fork list, the page. 6's successor holds the definitions: whether each named thing means one thing, the hard cases worked through, the work hiding behind a name. Roles are points of attention, not limits; either may contribute a construction, an alternative, or a clearer picture. When the definer pushes, it pushes on correctness and will be right about the detail: take every counterexample as a fix, never as a reframe. Take Codex geometry, hold the frame.

Sid reads visually and plainly. Story first, at most fifteen numbered sentences in everyday words; one-idea pictures; then depth on the item he pokes. Anchors for code claims, reasoning for direction questions. Examples only from the tree or his own words, never invented product scenarios. Positions marked POSITION; his rulings quoted verbatim with time; options only where his word closes a fork. These are in the memory directory; MEMORY.md loads for you at boot and points at each.

## 3. Where the problem-solution space stands

**Settled by Sid (his words, on the page's ledger):** the stored form is curve-consumable ("now we need a format that a curve can consume. That's sorted", 08:38); the imaging model is the working basis, open to attack; the fence; framing over correctness; the two chairs.

**The picture, as it stands (all POSITION unless above):**

1. Everything on the canvas ends as one question per pixel: how much of me is inside this region, and what colour.
2. At that boundary a pen stroke, a rectangle, a designer's outline and a letter are one thing: a path. Pen, formula, designer and font file are sources, not kinds.
3. A path says where the boundary goes, not what to do with it. Fill, stroke, clip, hit-test are declarations against one path.
4. Stroke is an operation: sweep a round nib along the path, width allowed to vary; the region is the union of every disc it covered. That region is filled, always nonzero, because the skin folds over itself on the inside of every bend.
5. The imaging model is the right language for the output and the wrong language for storing ink: its stroke has one width, a pen has one per sample. PostScript's stroke is the swept tip that never changes.
6. Zoom is a camera move; nothing stored or cached knows the zoom. Per-scale caches belong to the packer, keyed on projected error.
7. Four pieces of code must exist below the waist: the path type, the geometry (envelope, regions, answers), the packer (cubic to quad, bands, cover cells, at the camera's tolerance), the filler (per-pixel coverage under a rule; the program the text lane already runs). Everything else is data.
8. Contract. In: path (subpaths of line/quad/cubic, closed flag, knots with stable ids and optional width/opacity/pressure/time) + paint (fill? {rule, colour}; stroke? {tip, cap, join, overlap union, colour}) + identity (id, revision, group). Out: to the GPU, one coverage draw per painted region (cover, curves, bands, instance row); to the CPU, classify/bbox/outline/flatten(τ) from the same path with the same nib definition. Tolerance is never in the value.
9. Painting (dabs, smudge, accumulate) is a raster lane beside the path kind, not a change to it. Vector or raster ink is a decision Sid has not ruled; it is named once on the page.
10. Today's code: two kinds that fuse geometry to fill/stroke; samples read as corners; triangles cached at three zoom bands baked into the mesh key and the frame key; a flat lane with no coverage that double-blends overlaps while the CPU classifier says union and the harness names the CPU the authority; the text lane is the published Slug algorithm with offline assets and no encoder in the client. Only the harness calls any of it.

**The seven positions with exit conditions are on the page.** The open fork is Position 7: what a varying width means. Swept round nib (union of discs of radius w(t)/2) versus ribbon (perpendicular offset at the nearest centerline point, which is what today's CPU rule computes). They differ at every taper; counterexample on the page. Sid closes this, verbatim and timestamped, asked once. The sliver is visible on 4's bench: pressure-ink fixture, cursor across a taper, both numbers at the readout.

**Consensus of the eight-session round (Codex 1, 3, 5, 6 and Claude 2, 4, 7, 8), so close to settled:** samples are a source, not the stored geometry; fill and stroke are declarations on one path, never kinds; a self-crossing stroke is one region painted once, darkening is a brush behaviour chosen on purpose, over/under needs data the pen never produced; CPU versus GPU is not the axis, sort work by when it changes (per edit, per scale, per frame) and then place it.

**Still owed:** a wet-stroke route on the bench and curves on the bench (it is polylines only); the waist test (three real tools, tldraw's draw tool, Figma's pen tool, a UI border, walked through the picture as data to see whether any needs new code below the waist); the board's pointer at this directory (not touched this session because of the fence; Sid's call); and, at Sid's word only, the first boots on the ground: one grammar replacing the two kinds, and a builder from samples to cubics, checked against the harness.

## 4. What is on disk, and what each thing is for

All under `docs/below-the-waist/path-kind/`, committed on main (`ec5f3a5`).

| File | What it is |
|---|---|
| `path-kind.html` | The page. Sessions 7 and 8 folded into one. Published at https://claude.ai/code/artifact/af4a8019-6383-4eea-a9b2-1ebb3f89f478 . To republish from a fresh session: `Artifact` action `read` with that url first, then publish with `url` set and this file as `file_path`; the URL is what Sid has. Publishing without `url` makes a second artifact. |
| `path-kind.md` | Generated twin of the page for reading and diffing; the two drawings live only in the html. Regenerate from the html after editing (the converter is a forty-line html.parser script; write it again rather than hunting for it). |
| `fact-base-8.md` | Parts 1 to 4: session 8's read of the four path files and three hunter reports (text lane, engine camera, makers and consumers), every fact file:line and tagged. Part 5: the folds the six closing sessions left. Part 6: session 8's own fold. Part 7: 7's checked facts not in 1 to 4. Part 8: 4's bench handover. |
| `from-7-to-8.md` | Session 7's brief: the decided-versus-proposed ledger, the constructions and facts its page carried, pointers. |
| `sources-7/` | Session 7's page (html and md) and its three hunt reports, verbatim. The page's figure 1 is the four-code-boxes composition now folded into ours. |
| `bench-4/` | Session 4's Ink Bench, one WebGL2 file, and its handover with a verification note. Live at https://claude.ai/code/artifact/2f98a7e6-beb1-43df-9247-3974586256d6 . One path, three roads (today's triangles as a mirror of the tessellator, per pixel with one cover, per pixel with a cover per segment), the harness goldens by their own coordinates, both stroke rules at the cursor. Lift its shader when a definition needs to be seen. |
| `from-0-sid-questions.md` | Session 0's cleaning of Sid's own questions from the morning, verbatim minus transcription noise, in his order: what he understood the path kind to be, why it is wrong, whether the best teams think this way. The starters for sessions 5 to 8 were distilled from this. |
| `from-0-ranking.md` | Session 0's three ranking passes over the eight (initial, after peer critiques with the verified-error list, final under the exploration criterion), the settled and divergence lists, and the fix list that must not reopen the frame. |
| `from-6-carry-forward.md` | Session 6's carry-forward for the definer's chair, verbatim: its role, its five working proposals, and its standing question (how could someone create a new tool, change its behavior through data, and reuse its results). |
| `from-0-starters.md` | The two starters Sid pastes into the successor chairs, Claude and Codex, both pointing at this directory. |

Elsewhere: recall holds Sid's morning session (2026-09-05 07:48 to 08:38, prompt uuids 7caba2dd, 255b8f3e, caca9242) where the three sources and the sweater were first said in his words. Memory has today's rulings: `feedback-exploration-rank-framing-over-correctness.md`, `feedback-cross-model-rankings-family-bias.md`, `feedback-durable-work-lands-in-docs-not-tmp.md`.

## 5. The code facts that decide things (anchored; do not re-hunt these)

- `path/component.cljc:15, 17, 41-57, 59-81`: two kinds, `:ink` = stroke points with a required width each, round cap/join only; `:shape` = polygon contours with outer/hole roles. No curve segment type anywhere. A stroked rectangle or a filled ink loop is inexpressible.
- `component.cljc:176-190, 230-252`: the ink hit rule projects onto the segment and reads the width at the foot: the ribbon plus end discs. Docstring: "not an independent exact-distance solver for every tapered outline". `:199-218`: shape classify is even-odd per ring.
- `tessellation.cljc:14-37, 42-55`: three zoom bands changing only fan resolution, in the mesh cache key for both kinds; `:543-544` zoom unused by shapes yet they re-derive on a band crossing. `:205`: "overlapping stroke pieces remain separate triangles". `:285-567`: hole bridging and ear clipping, about 280 lines that exist because the lane takes only triangles. `:585`: coverage `:aliased-v1`. `:590-607`: cache never evicts.
- `renderer.cljs:17-18, 24-62`: 28-byte vertex, flat colour, camera = pan, one f32 zoom, screen size; zoom is a camera move in the shader. `:192-240`: whole-frame repack and one upload on any change.
- `frame.cljc:9-22`: frame key = rows + zoom band.
- `harness/path.cljs:74-140`: the only makers of paths, literal point lists, `width = 16/zoom × pressure`; no formula shape, no circle. `:245, 517-518`: the pressure-ink fixture is built and never run. `:341-393`: parity check on the holed shape only, skipping a 1.25-pixel band at the boundary. `:556-559`: `:product-pick :cpu-path-authority`, `:self-overlap-alpha :direct-triangle-double-blend-declared`.
- `text/renderer.cljs:139-264`: the Slug algorithm, quadratics two texels each, band headers, signed ray crossings per pixel, analytic coverage; nothing glyph-specific in the shader. `:112-113, 127, 321`: band texture width a compile-time 4096 while the texture reads its width from metadata. `fonts.cljs:120-136, renderer.cljs:305-330`: assets built offline, written once, no encoder in the client.
- `engine/device.cljs:149-162`: the camera is six f32. `transform.cljc`: f64 on the CPU, narrowed to f32 at upload, never rebased around the camera. Five places decide how big a pixel is: path bands; `region3d/renderer.cljs:1032-1054` ceil(w·zoom·dpr); `:995-1013` a 1.12-power bucket ladder; `on_plane.cljc:20` placement zoom fixed at 1.0; `text/renderer.cljs:531-537` snap step. `rungs.cljc` is offscreen admission, not zoom.
- `region3d/on_plane.cljc:101-117, on_plane_renderer.cljs:19, 189-235, 271-308`: placed ink takes triangle vertices only, shares the path renderer's mesh cache object, 88 bytes per vertex with a 4×4 matrix each, 4× MSAA; only `:ink` placeable.

If you need more, the hunter method is in memory (`feedback-waist-sort-hunters-code-only.md`): `code-hunter` agents on opus, one per disjoint slice, facts only, fenced to `src/app/client/`, never `src/app/server/env.clj`.

## 6. How the two chairs work together

Through this directory. The definer writes `attack-N.md` (its hard cases against the contract block and the attack table, the hidden work it finds, the forks it raises). Sid pastes you the path. You fold: change an arrow, add a capability, or simplify; repaint; regenerate the twin; republish with `url`; commit with exact paths and a plain `git commit`, no `Co-Authored-By`, one commit per milestone. Push is Sid's alone. Never leave working material in `/tmp` or a scratchpad only; land it here.

The change-it moves that test whether the pieces compose (from session 5): change the nib or the pressure response; edit the centerline after drawing; use the same geometry as a mask and as a text guide; make a thread cross over here and under there; save the construction so someone else adapts it.

## 7. Guards, from this session's own failures

- A renderer quantity promoted to a definition was the shape of every definitional error in the round: distance, a cache key, the projection rule. When a sentence on the page defines something in terms of what a shader computes, stop.
- I equated the CPU projection rule with the swept-nib union. Wrong where width varies. Three sessions caught it. The fix is on the page; the lesson is above.
- I ranked my own family higher when asked to rank. Codex rankers did so perfectly. Weight a family's rankings as one vote; take counterexamples at full weight, rankings at none.
- Dense, cited, hedged prose is what Sid cannot read. Commitment with named exit conditions is what he can rule on.
- The bench is the only thing in this round Sid could feel. Things he can feel come before documents about them. When a definition needs to be seen, lift the bench's shader rather than write a paragraph.

## 8. The starter Sid pastes into you

> You are the composer's chair on the path kind, successor to session 8; a Codex session takes the definer's chair from session 6. Read `docs/below-the-waist/path-kind/HANDOFF-8.md` first, then the page it points at, then the fact base Parts 5 to 8. Fence: code only under `src/app/client/`; never `src/app/server/env.clj`. Exploration phase: framing and the whole picture first, details are a fix list. Hold the picture; fold what the definer finds; bring me forks only where my word closes them. Position 7, what a varying width means, is mine to close and is asked once. Continue from the "still owed" list.
