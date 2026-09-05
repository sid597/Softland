# Handoff from session 2 to its successor — the 3D kind, the composer's chair

Written 2026-09-06, end of session 2 of the 3D round (Claude lane, run on the expanded starter of 2026-09-06 with the store line, the pointer-rate hover line and the screenshot test). Sibling sessions ran the same starter in parallel in this directory (a Claude-lane session 1 with `fact-base-1.md` and `3d-kind.html`, and another lane's `3d-kind-working-model.md`, `3d-object-space-and-query.md`, `bench-0/`); none of them was read while this was written, so the perspectives are separate and can be ranked. Read in this order: this file; `3d-kind-2.html` (live at https://claude.ai/code/artifact/400dcba1-c53c-4eb2-bba8-7978dee838d7, or the `.md` twin); `bench-2/HANDOVER.md` and the bench itself (https://claude.ai/code/artifact/32169816-ae1c-4bbb-ba07-a9171eea419a); `fact-base-2.md` for every anchor. Then the path directory: `path-kind/HANDOFF-9.md`, `HANDOFF-8.md`, `path-kind.html`. The fence is Sid's: code only under `src/app/client/`, never `src/app/server/env.clj`.

## 1. The problem, in Sid's words

From the starter: "What I am trying to build is every tool, 2D and 3D. The way I want to get there is a waist: an engine and a few hardcoded primitives below it, and everything above defined as data, ECS-style … I want the 3D theory first: what a 3D thing is and what it is made of. I want one model for 2D and 3D in one window that is elegant and plays with differential reactive data and mouse actions over data, so reactivity can be fine-grained; click in, 'you hit this' out." And: "The store that merges is above this kind; what the kind owes it is stable identity and fine-grained diffs." The hardest case is the stroke on a face inside a portal on a page, with zoom, orbit, hover, an occluder, a portal in the portal, and a screenshot beside it.

## 2. The phase

Exploration, before any contract. Framing and the whole picture first; details are a fix list, never a reframe (Sid, 2026-09-05: "it's like starting a painting"). Positions are marked and carry exits; the verdicts are Sid's. Things he can feel before documents about them: the bench came first, the page's numbers are its.

## 3. Where the problem-solution space stands (all POSITION unless quoted)

1. **Four nouns.** A space is the entity (planar or spatial, with a unit); things live in spaces; a view is a camera onto a space and belongs to a session; a portal is a thing in either kind of space showing a view of another space. The surface a portal shows is a lease, derived, worn visibly.
2. **The seam is a pair of maps.** Picture up (render the view into a surface, paint it at the portal), pointer down (map the point through the portal into a ray, ask the space; on a face, into a planar space, ask the path kind). They agree at a tolerance: what you see is what you hit. Sid's composition primitive names the upward half only.
3. **The general 3D object** is a placed shape with a material, stored as its source made it (mesh, subdivision cage, brep, implicit, volume, points, splats), behind one answer set (hit, classify, bounds, surface at τ, section, silhouette, distance, correspondence, kernel operations). The triangle is the leak only when it stands in for another source; segment counts and normals in the value are the leak today (CHECKED component.cljc:53-60, :373-384). The picture is the larger leak and is never data.
4. **Ink on a face** is a planar surface: a planar space attached to the face with a unit; the path kind draws in it unchanged; the placement zoom of 1.0 dies (CHECKED on_plane.cljc:20).
5. **The hit is an address**, not a hit: every level with coordinates, and the diff. Hover is a value of (pointer, data, views), re-walked when data or a view moves under a still pointer. The CPU walker is the definition; GPU picks are approximations.
6. **Two cameras compose** by function composition; the page's zoom is per scale for the portal's surface, the region's camera is per view; a crossing is travel, a declared reaction that re-roots.
7. **The axis** is per edit · per view · per frame · per time; per scale is the planar case of per view.
8. **One waist, two floors.** Shared: identity and diffs, the seam walker, the compositor, the evaluator. Floors: planar (the path picture) and spatial (representations behind one answer set; a packer per view with a budget; a view render). Condition for two engines under one data model: the residency ceiling's frame planner.
9. **The evaluator** (the path page's Position 8) is forced to a decision by 3D: a construction (feature history, operator graph, shader graph, mate) is data only if code below runs it. Position 9 on the page: a node vocabulary as data with an executor, code-as-data as the escape hatch. Sid's to close; asked once.
10. **Today's code**, read at the level of inputs and outputs: the region row is a portal with the world stored inline (CHECKED component.cljc:601); the view and moved transforms already live in the session (renderer.cljs:1087, scene.cljc:1041-1060); pick is meshes only, no page-point map, ink not in the BVH (scene.cljc:965-982); any static edit is a full rebuild (scene.cljc:1088-1105); the composite is colour only through the page's group transform (renderer.cljs:270-275); `:extent` is required and read by nothing.

**The decision that remains:** Position 9, whether constructions are the kind's input and in which language.

**The exact open question:** the identity of a derived part (topological naming). Ink on face F of a constructed solid; edit the sketch; what makes the new F the same F? Ids from the construction with correspondence, or ink attached to the construction step and a (u, v) frame. It decides whether ink on a face survives a parametric edit, the hardest step of CAD and BIM. Unworked by anyone; the definer's chair should take it first.

## 4. What is on disk (this session)

| File | What it is |
|---|---|
| `3d-kind-2.html`, `3d-kind-2.md` | The page and its generated twin (the three drawings live only in the html). Republish with `url` set to keep the artifact's link. |
| `fact-base-2.md` | Every CHECKED line with file:line, this session's own reads at HEAD 4b8e986; Sid's words used, with sources; the FIELD claims listed. |
| `bench-2/seam-bench.html`, `bench-2/HANDOVER.md` | The Seam Bench and how to run, deep-link and headless-check it; the numbers on the page; the fix list. |
| `HANDOFF-2.md` | This file. |

## 5. Still owed

1. **Meet the siblings.** Rank the parallel pages under the exploration criterion (framing, how-to-think, the whole picture; detail errors as a fix list), one vote per model family, and fold what a peer had that this page lacks, with who found it. Do not average perspectives into mush.
2. **The derived-part identity question**, worked as a case on the bench: a box made by extruding a rect, ink on its top, then the rect edited. Which F survives, and what the address says.
3. **Lift bench 9's filler onto the face**: the path kind's coverage lane under the projection, replacing the bench's per-pixel distance; then the path page's Position 6 has its receipt.
4. **Travel** on the bench: a zoom crossing that re-roots into S, the page as a window; the first receipt for Position 5.
5. **A shadow and a curved face** on the bench (fix list).
6. **At Sid's word only, boots on the ground**: split the region row into space, view, portal and lease; move segment counts and normals out of the value; put placed ink in the pointer map; one grammar for the answer set. The cascade on the page says what changes where.

## 6. Guards, from this session

- The sibling's uncommitted files in a shared tree are not ground and were not read; `git status --short` before deriving anything from a working-tree file.
- A reserved identifier in GLSL (`gl_` prefix) fails silently as "WebGL2 unavailable"; the bench now prints the shader log into the DOM (`#glstat`) so a headless dump shows it.
- The bench came before the section; every number on the page is a dump-dom readout, none was estimated.
- Every seam is two maps; a proposal that names only the picture half has left the hit and the diff unsaid.

## 7. The starter Sid pastes into the successor

> You are the composer's chair on the 3D kind, successor to session 2. Read `docs/below-the-waist/3d/HANDOFF-2.md`, then the page it points at and bench 2, then the path directory's `HANDOFF-9.md`. Fence: code only under `src/app/client/`; never `src/app/server/env.clj`. I am building every tool, 2D and 3D, as data above a waist of code that exists once, and I want to hold the picture of what that code is, its input and its output, so every session talks about the same thing. Exploration: framing and the whole picture first; details are a fix list, never a reframe. Hold the picture; fold what the siblings and the definer found through the directory, counterexamples at full weight; bring me forks only where my word closes them. Position 9 is mine and has been asked. Continue from the still-owed list; the derived-part identity question is the first thing to work.
