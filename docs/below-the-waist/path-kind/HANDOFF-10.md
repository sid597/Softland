# Handoff from session 10 to its successor — the composer's chair on the path kind

Written 2026-09-06, end of session 10. Read `HANDOFF-9.md` and `HANDOFF-8.md` first (the problem in Sid's words, the phase, the two chairs, the code facts, the guards; both still hold), then the page (`path-kind.html`, live at https://claude.ai/code/artifact/af4a8019-6383-4eea-a9b2-1ebb3f89f478 ), then `fact-base-10.md`, then `bench-9/HANDOVER.md` (the session 10 section at the end). The fence is unchanged: code only under `src/app/client/`, never `src/app/server/env.clj`. The starter for the successor is `STARTER-10.md` beside this file.

## 1. What session 10 did

The definer's `attack-2.md` (a brush that reads the paint under each dab; a network whose faces are discovered) was met first and folded at full weight; then the two things Sid said at the end of session 9 were worked through the picture: clips, blend modes and layer stacks, and the executor. Everything landed on the bench before it landed on the page.

- **Bench 9 gained a compositor stage and an executor** (`bench-9/waist-bench.html`, same URL https://claude.ai/code/artifact/da84fdb6-604a-488e-b797-e77c6f257cbc ). A clip is a second region whose curves and bands ride in the same draw; a blend reads a copy of the backdrop under the region; a layer is a surface a group paints into and then paints as one region into the root; a painting surface is retained at its declared resolution and advances per brush step. A sixty-line executor runs a record's construction over a capability table; the definer's two records from attack 2 run verbatim, the pickup brush on the GPU and on a CPU twin with equal numbers, the network through an arrangement (lines exact, curves refined). Fields of a record that no code read are listed under it; an unknown operation stops the construction and paints nothing. Verified headless in SwiftShader; the numbers are in the handover and in `fact-base-10.md` Part 2.
- **The page**: figure 1 repainted (the waist above the source stage where session 9 put it, a construction as a fifth kind of record, the executor in the source stage, the compositor under the filler, the loop); Sid's three late statements under "what is decided" as his words; the contract gains clip, blend and the group's fields, the surface and the construction beside the value, the arrangement and contacts-plus-spans and the dab packet in the answers, the compositor's two operations; the axis table gains a per-brush-step row; the waist-test table gains four rows; a table of attack 2's findings and where each landed; the attack row on compositing rewritten ("HOLDS as declarations; SHORT as a placement"); the fork table gains a column; the cascade gains the engine's compositor row from a hunter's read; four "best team" bullets; Position 8 rewritten as the executor; Position 9 new, the compositor; "folded in from session 10". The twin regenerated with `twin-10.py`.
- **Positions (all POSITION, asked now, together)**: the executor is a vocabulary, an order and compiled expressions, not one interpreter; instances are many; Sid's "getting them in line" is the vocabulary and his "throughput seems small" is the one serializing operation, a surface read, which belongs to the compositor and only to brushes that read. The compositor is the sixth piece, paint and sample, and it is the same piece a clip, a blend, a layer and a surface-reading brush need; adopt Vello's scene and Skia's canvas as the contract, said as data; the filler road first, the compute road as the end state with the same contract. The count of pieces is closed by outputs.
- **Checked in the tree** (a hunter, fenced): WebGPU, no compute pass; `engine/compositor.cljs` owns a target pool (512 MiB budget) and a present pass; region3d's composite draws a resolved MSAA texture as a quad; a scissor rect per draw from text's caller; groups are transform nodes with one flag; no blend mode, no group opacity, no clip by a path, no stencil. `fact-base-10.md` Part 1.

Sid did not rule on anything this session; it ran on the starter alone.

## 2. Still owed (updated)

1. **Keep meeting the definer.** Attacks 1 and 2 are folded. The next attack folds the same way: each counterexample a fix at full weight; its records paste into bench 9 (every new field shows up as consumed or as "nowhere to go"). Constructions the bench cannot run yet: a program painting twice from one revision (the two hosts disagree by the GPU host's design), a tangent contact, a partial overlap, a group mask on a soft edge, a replay from a checkpoint.
2. **Positions 8 and 9 are Sid's**, asked now, together. Position 7 is his and was asked once. Do not ask again unless he reopens them.
3. **The offset stroker**: the envelope still flattens the centerline before tracing (×5–8 curves over an offset stroker); on the bench that is the one fix that changes the numbers.
4. **The carry-on-GPU route** for a surface-reading brush (no CPU round-trip per dab) and layers allocated at the group's cover; both on the fix list, neither changes the picture.
5. **Determinism across clients** for a surface-reading brush: named on the page as open and Sid's; the bench's CPU twin is the seed of an answer.
6. **The board's pointer at this directory**: still not touched (outside the fence; Sid's call).
7. **At Sid's word only, boots on the ground**: one grammar replacing the two kinds, a builder from samples to cubics, checked against the harness; the cascade table says where each piece lands in cljc, now including the compositor and the group record.
8. Image and region3d come after path, read against the same frame; the 3D chair's directory is `docs/below-the-waist/3d/`.

## 3. What is on disk (added this session)

| File | What it is |
|---|---|
| `attack-2.md` | The definer's second attack, landed as found (commit 53e419d). |
| `bench-9/waist-bench.html` | The bench with the compositor stage, the executor, the arrangement, the unread-field list, six new fixtures, a deep-linked cursor (`&at=x,y`). Pure declarations still precede the GL setup, so the definer's Node extraction keeps working; `buildPath`, `buildDabs`, `lowerPath`, `packRegion`, `coverageAt`, `normStroke`, `splitCubic` keep their names. |
| `bench-9/HANDOVER.md` | The session 10 section: what a record may declare, the executor, the compositor, the arrangement, the fixtures, the measurements, the unfinished list. |
| `fact-base-10.md` | The tree's compositor facts, the bench's measurements, the derivations, the FIELD claims, the fold of attack 2 row by row, the two positions in short, the fix list, what the definer can do next. |
| `path-kind.html`, `path-kind.md`, `twin-10.py` | The page; the twin; the converter that makes it (`python3 twin-10.py path-kind.html path-kind.md`). |
| `HANDOFF-10.md`, `STARTER-10.md` | This file; the two-paragraph starter for the successor. |

Commits, exact paths only, plain `git commit`: `53e419d` attack 2 as found; `2684577` the bench; the page, fact base, handoff and starter in one commit after this file. Push is Sid's alone.

## 4. Guards, added to 9's

- **Name a piece by its output.** The count of pieces grew from four to six in three sessions and each time the new piece had an output no other piece had (a record's results; a surface). If a proposed seventh piece has no seventh kind of output, it is a capability inside a piece that exists.
- **"On top" and "beside" are where hidden work lives.** Both words on the page turned out to name a piece nobody had drawn. When a sentence places something outside the pieces, draw it and see what it costs.
- **A construction runs before its picture is drawn.** Attack 2's records ran in Node against the bench's functions before the executor existed; then the executor ran them unchanged; then the page changed. The definer's numbers were the receipt at every step.
- **GPU state is a fact, not a detail.** Two hours of "nothing draws" were two sampler types on one unit and a backdrop copy bound on the wrong unit. A `gl.getError()` printed into the page after the first render is the first thing to add to a bench that shows nothing.
- **A fallback font is the widest font.** Figure text that fits under the web font overflows in a headless render; trim until it fits without the font, and the page holds everywhere.

## 5. Working with siblings on `main`

Other chairs commit to the same branch during a session (the 3D chair did twice while this one ran). Commit with exact paths only, never `git add -A`; check `git status` for the directory before each commit; a sibling's uncommitted file in a shared directory is not landed ground until it is committed.
