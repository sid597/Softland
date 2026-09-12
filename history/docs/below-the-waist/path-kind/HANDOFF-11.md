# Handoff from session 11 to its successor — the composer's chair on the path kind

Written 2026-09-06, end of session 11. Read `HANDOFF-10.md` first (then 9 and 8; the problem in Sid's words, the phase, the two chairs, the code facts, the guards; all still hold), then the page (`path-kind.html`, live at https://claude.ai/code/artifact/af4a8019-6383-4eea-a9b2-1ebb3f89f478 ), then `fact-base-11.md`, then `bench-9/HANDOVER.md` (the session 11 section at the end). The fence is unchanged: code only under `src/app/client/`, never `src/app/server/env.clj`. The starter for the successor is `STARTER-11.md` beside this file.

## 1. What session 11 did

Sid's instruction at the start: no random fixes, no new artifacts, the code first. The definer's `attack-3.md` (a result that survives its next use) was the only new thing in the directory. It was met at full weight, code before page.

- **Bench 9** (`bench-9/waist-bench.html`, same URL https://claude.ai/code/artifact/da84fdb6-604a-488e-b797-e77c6f257cbc ): the executor gained a compile pass over the program's text that records where every binding is last read, and passes `paint` a context (the step, the iteration, whether the input is at its last use); the GPU host paints in place only then and otherwise copies into a pooled target, gives a target back after a value's last read, and refuses a read of a superseded value with an error instead of a wrong pixel; every result carries a key (the step that made it) and its parent, with the revision kept as depth. `runProgram` runs a prefix (`until`) or continues from a saved state (`from`); a checkpoint (`checkpoint.at` on the record) is taken on each host from its own prefix as content plus carried state plus the next item, with a dependency key (`checkpointKey`, `checkpointWhy`, both pure) over the program, the record bindings the construction reads, the surface declaration, the item fields it binds and a capabilities string; the rest resumes on both hosts through a content resolver and is compared with the straight run. `group.clip` compiles as a region and multiplies into the layer's one composite draw, drawn in local units under the camera; the record's own `clip` stays on each child. Two fixtures verbatim (two paint proofs; the crossing masked as one stroke), a checkpoint control, the results and storage receipts on the panel.
- **The residual attributed and fixed.** The definer's unattributed `.461` against `.465` on the child-clip route: the region shader took its local position from the interpolated vertex varying, which inherits the rasterizer's sub-pixel snap of the cover's fractional corners; both fragment programs now derive the position from `gl_FragCoord` through the inverse map. After it the single clipped dab reads `.31` on both hosts and the two hosts agree across the whole pickup surface to `1.2e-7` (from `5.8e-3`). The tree's filler interpolates the same varying.
- **The page**: the contract's paint entry (value lifetime, key and parent), the group's clip scope, a checkpoint beside the value; the per-brush-step row rewritten with the checkpoint's inputs and validity; a table of attack 3's findings and where each landed; Position 8 qualified (replay is the vocabulary's defined behaviour plus captured inputs plus numeric rules; dependencies are stated by the construction and the compiler schedules around them); Position 9's either/or between a document's layer stack and the group's fields removed; story item 15, the best-team bullet and figure 1's surfaces box brought into line; "folded in from session 11". The twin regenerated with `twin-10.py`.
- **Positions**: 8 and 9 stay asked, together, with their text amended by the fold; nothing new is asked. Sid did not rule on anything this session.

## 2. Still owed (updated)

1. **Keep meeting the definer.** Attacks 1, 2 and 3 are folded. The next attack folds the same way. Constructions the bench cannot run yet: a clip bound to a construction's returned path (a network face as a mask); a saved checkpoint reloaded from bytes; a surface aliased through a definition's `emit` (the compile pass does not see it; the host refuses the read); a tangent contact; a partial overlap.
2. **Positions 8 and 9 are Sid's**, asked now, together. Position 7 is his and was asked once. Do not ask again unless he reopens them.
3. **The offset stroker**: unchanged, the one fix that changes the bench's numbers.
4. **The carry-on-GPU route** and **layers at the group's cover**: on the fix list, neither changes the picture.
5. **Determinism across clients**: open and Sid's; the two hosts' agreement to `1.2e-7` is a seed (one GPU, software), not an answer.
6. **The board's pointer at this directory**: still not touched (outside the fence; Sid's call).
7. **At Sid's word only, boots on the ground**: as in handoff 10, now with the fragment-position fix as a known one-line change to the tree's filler when the filler is lifted.
8. Image and region3d after path, read against the same frame; the 3D chair's directory is `docs/below-the-waist/3d/`.

## 3. What is on disk (added this session)

| File | What it is |
|---|---|
| `attack-3.md` | The definer's third attack, landed as found. |
| `bench-9/waist-bench.html` | The bench with the compile pass, the pool, keys and parents, the checkpoint, the group mask, the fragment-position fix, two fixtures, a control. The pure declarations still precede the GL setup and now include `checkpointKey` and `checkpointWhy`. |
| `bench-9/HANDOVER.md` | The session 11 section: what a record may declare, the results contract, the checkpoint, the mask, the residual attributed, the measurements, the unfinished list. |
| `fact-base-11.md` | The measurements, the derivations, the FIELD claims, the fold of attack 3 row by row, the fix list, what the definer can do next, and the measured cost of republishing (Part 7, for Sid's question). |
| `path-kind.html`, `path-kind.md` | The page and its twin. |
| `HANDOFF-11.md`, `STARTER-11.md` | This file; the two-paragraph starter for the successor. |

Commits, exact paths only, plain `git commit`: attack 3 as found; the bench with its handover; the page, twin, fact base, handoff and starter. Push is Sid's alone.

## 4. Guards, added to 10's

- **A receipt that disagrees with the reference is a lead, not a tolerance.** The `.461` residual sat inside the cursor's "agree" band for a session. One point where a single operation had to give an exact answer (one clipped dab on the mask's edge) found the cause in three runs. Ask where the answer must be exact and read there.
- **The executor knows what the host may forget.** Storage reuse is a liveness question, and liveness is in the program's text; the host should never guess it from handles. When a host needs to know whether an input is still needed, the answer comes from the compile pass, as a flag on the call.
- **A saved result is never a writable target.** The frozen start, a resolved checkpoint, and any value still to be read are painted by copying; only a value at its last use advances in place. The bench's stale-read refusal is the check that the rule held.
- **Republishing costs a full read of the live version.** Both artifacts this session were byte-identical to the committed files and still had to be read in full before the tool would publish. Publish once per session, at the end; the definer reads the committed files.

## 5. Working with siblings on `main`

As in handoff 10: exact paths only, never `git add -A`; check `git status` for the directory before each commit; a sibling's uncommitted file is not landed ground. The 3D chair had uncommitted files under `docs/below-the-waist/3d/` throughout this session; none were touched.
