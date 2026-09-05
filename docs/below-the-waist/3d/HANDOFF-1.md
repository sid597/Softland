# Handoff from session 1 to its successor — the composer's chair on the 3D kind

Written 2026-09-06, end of session 1 (Sid named it "3d session 1 max"; it ran at about 500k tokens and stops here). You inherit the picture and the responsibility for it. Everything you need is in this directory, in git, on main.

Read in this order: this file; `3d-kind.html` (the artifact link below, or the file; `3d-kind.md` is its generated twin for diffing); `fact-base-1.md` Parts 5 and 6, then Parts 1 to 3 when a code claim needs its anchor; `bench-0/HANDOVER.md`. Then, only if you need more than the fact base holds, the code itself under `src/app/client/`, by the READMEs down to docstrings down to code. Docs allowed beyond this directory: `docs/below-the-waist/path-kind/` and `docs/below-the-waist/3d-ceilings-starter.md`. Never `src/app/server/env.clj`.

## 1. The problem, in Sid's words

His framing, verbatim, from the ceilings starter:

> "we have build engine and some primitives that get hardcoded and we leave other heigher level layers for ecs and call that above the waist. Anything above the waist is stored as data, is collaborative editable, createable, agents and humans without getting into git merge deadlocks, anything in ecs layer should be creatable and then saved for reuse or build higher order things from it"

> "When you see the ceilings, do not try to hack through them. We want to build through them. the question we are asking is: what is the below-the-waist layer that should exist for everything?"

His essence for this kind, from `STARTER-0.md` (his words of several dates, not quoted): every tool, 2D and 3D; the hardest 3D tools are the ceilings; one waist for both or a reason for two; the 3D theory first, what a 3D thing is and what it is made of; one model for 2D and 3D in one window that plays with differential reactive data and mouse actions over data; click in, "you hit this" out; hover matters because hover is where a person's focus is; judged from what Softland is for, not from the shape of today's renderer.

His late words of the same night, about 02:00, to the path kind's session 9 (`path-kind/HANDOFF-9.md` §6), which bear on this kind and are on the page's ledger: on tools as data, "Yeah i know thats why softland exists those tools are dinasarus"; on the executor, "ooooooo nice but one executor idk seems like the constraints and hard part would be getting them in line throughput seems small...."; on the field, "We should be ahead of everyone or at the very least adopt the best in field"; on the bench, "the very first thing that i want to do after the code buildout is done is to make the whole system visible in softland itself so that i can use softland to understand softland and build more of it".

## 2. The phase, and how Sid wants it worked

Exploration, the phase before any contract. His ruling of 2026-09-05 about 16:50 holds: "yeah fuck the correctness and penalising it … framing and how to think is much more important in exploration phases." Details are a fix list, never a reason to reframe.

Sid carries contributions between chairs by pasting them. Fold what he carries at full weight for counterexamples and definitional sharpenings, at no weight for rankings. Each model family ranks its own family higher; do not rank. Three sessions answered this starter without reading each other and reached the same shape; that convergence is on the page and is the strongest evidence an exploration gets. He did not rule on anything this session; the page's ledger keeps his words apart from every position.

He reads visually and plainly: story first, at most fifteen numbered sentences in everyday words; one-idea pictures; then depth on the item he pokes. Anchors for code claims, reasoning for direction questions. Examples only from the tree or his own words. Positions marked; his rulings verbatim with time; options only where his word closes a fork. Things he can feel come before documents about them; the bench is what he could feel this round.

## 3. Where the picture stands

The page's story, in its own fifteen sentences, is the picture. Compressed to the bone:

1. A 3D space's pixel question is "which face is nearest along this line of sight, and what colour under the lights"; shading's inputs are other renders of the same space, so the question is recursive where 2D's is not.
2. A 3D thing is a definition that owns its edits, placed by occurrences (Sid's copies), with a look and stable identity for definition, faces, edges and occurrences; its shape is the description an edit acts on plus derived representations each with a way back. Sources are above it. A mesh is the leak only when it replaces information a later operation needs.
3. A space is a frame plus an order rule: declared on a page, derived from the view in 3D. A portal is a thing whose look is another space through a camera; a chart is a flat declared-order space on a face at the face's depth; an attachment binds one to a copy and a face. The region today is space, scene, camera and portal fused; split, it is one way of seeing a space.
4. Cameras compose as a matrix chain; page zoom is the region camera's focal length; entering is another action; a zoom-to-enter transition is data.
5. Three crossings: present (render a space through a camera into a surface, callable as a value), embed (an attachment), derive (section, silhouette, extrude). Pixels do only the first.
6. The rate axis with two rows added: derived surfaces on their own inputs; time, queries and arrivals as triggers. Fine grain names the changed meaning; consequences may be large.
7. The pick is a query with a mode; hover is the same query re-run when anything on its chain changes, emitting only what differed; a stationary pointer loses the stroke when the sphere passes in front.
8. Two per-pixel resolves nest in one waist: coverage inside a face's look, a space's resolve inside the page's order.
9. A thin executor over thick typed libraries; constraints are a solver library; throughput lives in the libraries and the GPU. This is where Sid's doubt landed and the page agrees with him.
10. One waist, several libraries beneath it. The 3D code is shelves on the path picture's pieces plus the visibility resolve, the material stage, scheduling for long work, and the promotion of the executor.

Positions 1 to 11 with exit conditions are on the page. Held against the lanes, with reasons: the order rule as the 2D/3D distinction; zoom as focal length by default; forward with MSAA as the road from here; render to surface as a value.

**The three things that remain, named once on the page:**

- **Decision, Sid's to size:** adopt the three-verb interface (Construct, Present, Query), and decide what an above-waist definition of a new spatial representation must supply (bounds, sampling, intersection and selection semantics, correspondence to editable inputs) so the engine can construct, present and query it without a new hardcoded kind, and which of those arrive as saved programs and which as kernel capabilities. An implicit or sampled object is the test. That is the limit of "a new tool needs no engine change".
- **Definition, the definer's exercise:** what the geometry operation returns when the face under the stroke is split, merged or deleted: a relation (unchanged, generated, split, merged, deleted, ambiguous, with parameter maps), so a saved attachment policy can keep, split, trim or detach without consulting display triangles. The construction that tests it: one extruded sketch, a stroke attached to a named face, a boolean split, the surviving ink returned as a 2D drawing, queried from both views.
- **Open, Sid's feel, asked once:** what page zoom means at a region, focal length or a step into the room. Do not ask again unless he reopens it.

## 4. What is on disk

All under `docs/below-the-waist/3d/`, committed on main.

| File | What it is |
|---|---|
| `3d-kind.html` | The page. Published at https://claude.ai/code/artifact/ced3dcf5-d276-46ab-8456-3a1be0141bc5 . To republish from a fresh session: `Artifact` action `read` with that url first, then publish with `url` set and this file as `file_path`; publishing without `url` makes a second artifact. |
| `3d-kind.md` | Generated twin for reading and diffing; the three drawings live only in the html. Regenerate after editing: the converter is a ninety-line `html.parser` script that maps h1/h2/h3, p, dl, tables, ol/ul, pre and the words block; write it again rather than hunting for it. |
| `fact-base-1.md` | Parts 1 to 3: three hunter reports (contract and scene; renderer and engine seam; placed content, harness and oracle), every fact file:line. Part 4: what session 1 read itself. Part 5: derivations. Part 6: the fold record. |
| `bench-0/seam-bench.html` | The seam bench, one WebGL2 file. Published at https://claude.ai/code/artifact/5fd52994-becf-4840-871e-0453bcd7bc3e . Both executions of the seam, both portal mappings, both readings of zoom, drawing on the face, the chain from the pointer, the GPU id read as the parity check, keys for the stationary-pointer case. `HANDOVER.md` beside it says what it shows, what it simplifies, deep links and the headless command. |
| `STARTER-0.md` | Session 0's starter, three versions; Irrespective is the one both lanes received. |
| `3d-kind-working-model.md` | The Codex lane's answer, folded. Not this chair's file; do not edit. |
| `3d-object-space-and-query.md`, `HANDOFF-object-space-and-query.md` | The object-space contribution, folded. Not this chair's; do not edit. |
| `3d-kind-2.html`, `3d-kind-2.md`, `fact-base-2.md`, `bench-2/` | Session 2's page, fact base and bench (a Claude session that read HANDOFF-9 and the LOG). Untracked at this writing; unread past their headers; unfolded until Sid carries them. Not this chair's. |

Elsewhere: `path-kind/HANDOFF-9.md` §6 holds Sid's late words; `path-kind/attack-2.md` is the definer's second attack on the path kind, unread by this chair and not this chair's to fold.

## 5. The code facts that decide things (anchored in fact-base-1; do not re-hunt)

- The region row fuses id, one revision, extent (written by the harness, read by nothing), scene, view, background, ambient and a page rect (`component.cljc:593-615`; Part 1 §1).
- Objects: TRS, six primitives or indexed triangles with positions, normals, indices only; no uvs, faces, textures, copies or object revision; ≤ 8 lights drawn; one shadow map from the first directional caster read by every casting light (Part 1 §2-3, Part 2 §8).
- Nothing of the page camera reaches a region's interior except lease size, aspect and a 1.12-power bucket; between buckets the interior is resampled; lease sizes quantise to 256 up to 4096; rungs admit by bytes only (Part 2 §2, §4).
- Placed ink is a separate transparent mesh, 88-byte vertices, tessellated at zoom 1.0 forever, negative depth bias, unlit, not in the BVH; resolved placements come only from the harness; `ray->placement-plane` has no caller; `path/classify` has no caller outside the harness; there is no hover anywhere in client/ (Part 3 §2-4).
- `pick-region` returns a triangle index and is called only by the harness; triangle identity is positional and renumbers silently; there is no face anywhere (Part 1 §5, §8).
- No region can contain a region; no scene reference; the placed-ref address is opaque and unresolved (Part 1 §7).
- f64 on the CPU narrows to f32 at upload as absolute world coordinates in both dimensions; no rebasing; forward [0,1] depth24plus (Part 1 §6, Part 2 §6, §9).
- The shadow pass renders the scene from the light's camera into a depth surface the shading reads: the composition primitive, once, keyed on the region's revision (Part 2 §8).
- The harness never composes a frame across kinds (four drivers joined by Promise.all); the page camera is written once at zoom 1.0 (Part 3 §6).

## 6. How the chairs work together

Through this directory. Sid pastes you what other lanes wrote; you fold into the page: change a sentence, add a row, mark who found it, repaint a drawing if it changed; regenerate the twin; republish with `url`; commit with exact paths and a plain `git commit`, no trailers of any kind, one commit per milestone. Push is Sid's alone. Never leave working material in `/tmp` or a scratchpad only; land it here.

The tree is shared with several live sessions. Before any git write: `git status --short` and `git log -1`; stage exact paths only; never `git add` a directory; a file you did not write that changed on disk is a sibling at work, not ground. Never touch `path-kind/` files; that chair folds its own. Never edit another session's files in this directory.

## 7. Guards, from this session's own failures

- I claimed "the executor is the centre of the waist" as one piece doing the work. Sid's words the same night and both lanes corrected it: thin executor, thick libraries, constraints as a solver. When a sentence makes one piece carry the throughput of a field, stop.
- I used "surface" for the face a stroke sits on, the image a view produces, and the GPU target, until the Codex lane split them. Three things under one word is how a renderer quantity becomes a definition.
- I wrote "hover is the same call on every move". Both lanes had the stationary pointer under a moving occluder. Hover is a query over the pointer and the scene; the bench now shows it.
- I wrote "a kernel is bought, never data". The dividing rule is that difficulty does not prove permanence; native kernels are the practical default. Sid's want is the tie-breaker, and his want is data.
- Two bench bugs got past a careful write and were caught only by the one headless look: a dropped `abs` in the capsule distance (the stroke filled a half-plane) and a missing precision qualifier on a shadow sampler (the script died before the UI). Take the one look; do not skip it because the code "looks right".
- A renderer quantity promoted to a definition was the shape of every error in the path round and again here: the rect, the lease, the composite, the bias, the triangle index. When a sentence defines a thing by what a shader or a lease does, stop.

## 8. Still owed, in order

1. Fold session 2's page when Sid carries it; the definer's construction (extruded sketch, attached stroke, boolean split, ink back as a drawing, queried from both views) when it lands; nothing to rank.
2. The waist test in 3D form, the code side: the hardest operations of the ceilings written as the data their own tools hold, walked through the three verbs, saying what executes, in which library, and where a step needs code the picture has no name for. The path round did this with three 2D tools and found the evaluator; this is where the 3D picture gets its boots.
3. On the bench: query modes; a curved chart with a metric; the boolean split with the attachment's outcomes.
4. The board's pointer at this directory: outside the fence, Sid's call.
5. At Sid's word only, boots on the ground in the tree: the region row split into a space by reference, a portal with a mapping, a revision per thing; the pick chain joined from its existing links (pick-region, ray-to-plane, classify) with a face and a chart; the 88-byte ink road cut for a chart layer in the mesh shader. The page's today table says what each becomes.

## 9. The starter Sid pastes into the successor

> I am working through the engine under `src/app/client/` one kind at a time, at the level of inputs and outputs: what goes into a kind, what comes out, and whether the kind is really a library. The path kind has its picture in `docs/below-the-waist/path-kind/`. The 3D kind has one now: `docs/below-the-waist/3d/3d-kind.html`, and `HANDOFF-1.md` beside it says where it stands, what it folded and what it still owes. You are the composer's chair on the 3D kind, successor to session 1. Fence: code only under `src/app/client/`, reached through its READMEs and docstrings; docs only `docs/below-the-waist/3d/`, `docs/below-the-waist/path-kind/` and `docs/below-the-waist/3d-ceilings-starter.md`; never `src/app/server/env.clj`. Exploration, the phase before any contract: framing and the whole picture first, details are a fix list. Hold the picture; fold what I carry to you through the directory, counterexamples at full weight, rankings at none; bring me forks only where my word closes them. Zoom at a region is mine and has been asked.
>
> What I am trying to build is every tool, 2D and 3D, as data above a waist of code that exists once, so that humans and agents create, edit and reuse tools without merge deadlocks, and the ceilings, parametric CAD, film tools, open worlds, BIM, the planet, volumes and scans, are built through, not hacked through. Three sessions derived the same shape of the 3D kind without reading each other: one waist with several libraries beneath it; the region is a view of a space, not the thing; nested spaces both ways through portals; ink on a face is an attachment drawn as a layer of the face's look; the pick is a route from page to stroke; a thin executor over thick libraries. I said one executor's throughput seems small and the constraints are the hard part, and the page now agrees with me. Those tools are dinosaurs, and that is why Softland exists; we should be ahead of everyone or at least adopt the best in the field.
>
> Now I want the picture tested the way the path round tested its own: the hardest operations of the ceilings written as the data their own tools hold, walked through construct, present and query, to see what executes, in which library, and where a step needs code the picture has no name for. Take the hardest operations, not the tractable ones: a sketch extruded and filleted with a stroke attached to a named face and then a boolean that splits that face; a scatter of copies under a deformer; an implicit object that supplies only a field; a tile stream at planet scale. I want to know what a definition of a new spatial representation must supply so the engine can construct it, present it and return an editable hit without a new hardcoded kind, and which of those things arrive as saved programs and which as kernel capabilities, because that is the limit of the promise that a new tool needs no engine change. And I want it as a thing I can feel before a document about it.
>
> Work the hardest case through whatever you propose: the boolean split under the stroke, from the operation's return through the attachment's outcome to the ink queried from both the space and a page. Where does a named thing, attachment, correspondence, executor, library, still hide unfinished work? After qualifying, say what decision about the extension interface remains and which exact question is still open. Where you take a position: what supports it, under what conditions does it become only a workable default rather than the answer, and what else in the picture changes with it, on the 3D page and in the path-kind picture?
