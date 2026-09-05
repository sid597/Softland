# Handoff from session 9 to its successor — the composer's chair on the path kind

Written 2026-09-06, end of session 9. Read `HANDOFF-8.md` first: the problem in Sid's words, the phase, the two chairs, the code facts that decide things, the guards. It still holds; this file is the delta. Then the page (`path-kind.html`, live at https://claude.ai/code/artifact/af4a8019-6383-4eea-a9b2-1ebb3f89f478), then `fact-base-9.md`, then `bench-9/HANDOVER.md`. The fence is unchanged: code only under `src/app/client/`, never `src/app/server/env.clj`.

## 1. What session 9 did

The first item on 8's still-owed list, the waist test, from the code side. Three real tools (tldraw's draw tool, Figma's pen tool, a UI border) written as the records their own tools hold, walked through the picture: what executes, in which piece, where a step needs code the picture had no name for. It was built as a thing to feel first and a section second.

- **Bench 9, the Waist Bench** (`bench-9/waist-bench.html`, live at https://claude.ai/code/artifact/da84fdb6-604a-488e-b797-e77c6f257cbc). Records above, the value on the line, four pieces below, each reporting what it did and at which rate. The filler is the tree's text shader lifted into GLSL with two lines changed. Curves throughout, both tip constructions one line apart, caps, joins, alignment, dash, device-unit width, snapping, cover cells, bands, the CPU twin at the cursor against the GPU pixel, a wet route. Seven fixtures plus draw-your-own. Verified headless in SwiftShader; the panel's numbers are in `bench-9/HANDOVER.md`.
- **The page** gained a section (the test, its drawing, the plain-words findings, the table of every executing step, what it changes and what it only costs), a unit and two fields in the contract, three answers, two axis-table entries, a sharpened Position 7, a Position 8 (the evaluator), and a "folded in from session 9" list. The twin was regenerated.
- **Findings** (all POSITION): every record compiles to the same value; the same pieces drew all three; the pen tool needs no code above the value; the draw tool needs an **evaluator** (a function written in a record is data only if code below runs it) — a fifth piece, beside the builder; the border shows the **width has a unit** (device-pixel widths make the envelope per scale) and **snapping runs per frame**; alignment needs the **offset(path, d)** answer; tldraw ships the ribbon, so Position 7 is a default with the other construction nameable. The waist itself sits one step above the kind's input: records are data, the source stage (builder, generator, evaluator) is code, the value is what the kind takes.
- **Fix list, not reframes**: the bench's envelope flattens the centerline before tracing, so a stroke's skin costs five to eight times the curves an offset stroker would, and the page's near-3-KB memory line assumed the stroker; the nib's butt cap on a taper is a chord; the wet route re-packs the whole stroke per event.

Then the definer's `attack-1.md` landed in the directory (same day, unread by either side while working) and was folded at full weight: its construction executor is the evaluator at larger scope (Position 8 now carries both), its missing answers join the contract, its width-as-a-function fix and its accumulate-through-the-same-filler construction are on the page and on the bench (the fixture "the crossing, as dabs": twenty-four dabs, 0.8556 at the crossing), its three tolerances, its membership-versus-coverage distinction and its interior-cells correction are folded, and the page has a "where the two walks met" table. `fact-base-9.md` Part 7 is the record of the meet.

Sid did not rule on anything this session; it ran on the starter alone.

## 2. Still owed (updated)

1. **Keep meeting the definer.** Attack 1 is folded. Its next attack folds the same way: each counterexample a fix at full weight; its records paste into bench 9's textarea, and a field with nowhere to go is a finding. Two of its constructions are not on the bench and could be: a surface-reading brush (the smudge recurrence needs surface state) and a vector network with faces (intersections and splits as answers).
2. **Position 7** is Sid's, asked once; the sliver is on both benches. Do not ask again unless he reopens it.
3. **The offset stroker**: the envelope builder should offset curves, not the flattened polyline. On the bench that is the one fix that changes the numbers.
4. **A clip by a path** on the bench (a second region multiplying coverage) — session 5's "use the same geometry as a mask" move, and the Figma mask.
5. **The board's pointer at this directory**: still not touched (outside the fence; Sid's call).
6. **At Sid's word only, boots on the ground**: one grammar replacing the two kinds and a builder from samples to cubics, checked against the harness. Bench 9's JS is a first cut of each piece in about 900 lines; the cascade table on the page says where each lands in cljc.
7. Image and region3d come after path, read against the same frame.

## 3. What is on disk (added this session)

| File | What it is |
|---|---|
| `bench-9/waist-bench.html` | The bench. One file, WebGL2. |
| `bench-9/HANDOVER.md` | How to run and republish it, the record schema (the definer's paste target), the pipeline piece by piece, the measurements, the unfinished list. |
| `fact-base-9.md` | What was checked in the tree, what the bench measured, the derivations, the FIELD claims, the test's findings, the fix list with the ranking fix list's status, what the definer can do with the bench. |
| `path-kind.html`, `path-kind.md` | The page with the waist test folded; the twin regenerated (the converter is a sixty-line `html.parser` script; write it again). |

## 4. Guards, added to 8's

- **A bench before a section.** The test was worth writing only after the pipeline ran the three records; every number on the page came off the bench, none was estimated first.
- **Read the shader, mirror it, then change as little as possible.** The filler on the bench is the tree's algorithm; the two changes are named on the page. Any "the shader could also…" is a fix-list item until it runs.
- **The waist is where the executing starts, not where the kind's input is.** Naming something "a source" or "above the kind" does not make it data; the test is whether code has to run to get from the record to the value.
- **Rates are measured, not asserted.** "Camera-free" was true of the value and false of the border's envelope; the badge on the bench moved before the page did.

## 5. The starter Sid pastes into the successor

> You are the composer's chair on the path kind, successor to session 9; a Codex session holds the definer's chair. Read `docs/below-the-waist/path-kind/HANDOFF-9.md`, then `HANDOFF-8.md`, then the page it points at and bench 9. Fence: code only under `src/app/client/`; never `src/app/server/env.clj`. I am building every 2D tool as data above a waist of code that exists once, and I want to hold the picture of what that code is, its input and its output, so every session talks about the same thing. Exploration: framing and the whole picture first; details are a fix list, never a reframe. Hold the picture; fold what the definer finds through the directory, its counterexamples at full weight; bring me forks only where my word closes them. Position 7 is mine and has been asked. Continue from the still-owed list; the definer's attack on the waist test is the first thing to meet.
