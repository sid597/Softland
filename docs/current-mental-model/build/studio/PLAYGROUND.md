# The Workshop — direction + playground stream (post-G7)

What this is: the settled direction after the G7 FAIL (`G7.md`) and the
running record of the playground stream that replaced the contract lane
for this arc's surface work. The eleven BRIEF laws stand except as
re-read in G7.md §Findings-7. Orchestration lives in the origin session
("engineer-to-designer-optimised-UI-VM") — the standing orchestrator,
judged against Sid's reactions, never its own synthesis.

## Direction (Sid, 2026-08-01)

Sid: "I lean towards A×B as well, C as capture, D as the second hand."

- **A — the drafting room**: the Workshop is a PLACE entered deliberately
  (a door), holding a shelf of what exists and per-design-thread tables;
  the notes land stays untouched. Implemented in playground 1 as a
  dedicated region of the SAME world-space reached by camera travel — a
  place with no mode machinery, dissolving the law-1 tension.
- **B — bench as material where cheap**: Workshop furniture (shelf,
  inspector, handles, notes, door) renders through the assembly
  interpreter as material. Rendered-as-material ≠ editable-from-inside:
  bench-edits-bench is a LATER cut's proof, never asserted early.
- **C — summon-capture** (later): take a thing up from the land into
  drafting context; the capture path INTO the place.
- **D — the agent as second hand** (later): type intent, the agent draws
  iterations on the same table, visibly. Never the primary hand (Sid's
  own rulings: goal 2 depends on goal 1; mediated-only repeats the
  alienation).

**The boundary** (Sid's corrected sentence, three-strata vocabulary of
smalltalk-ui-vm/DIRECTION.md): *physics at the host floor; interaction
machinery code-owned initially; visible Workshop surfaces increasingly
material.* Migration toward self-hosting stays open by seam design:
native controllers publish plain data; everything visible binds to it.

## The atom inventory (code truth, read 2026-08-01)

WebGPU is a GPU API, not a graphics library — nothing is drawable except
what our pipelines mint. `renderer.cljs` mints exactly **two atoms**:

1. **SDF rounded rect** — per-corner radii, borders, linear gradients,
   anti-aliased; axis-aligned only.
2. **MSDF glyph** — one monospace font atlas, char-advance layout; no
   shaping/kerning; crisp at any zoom.

Everything ever seen in Softland is composed of these two. No paths, no
rotation (container transform = offset + uniform scale + screen flag),
no images, no boolean ops. The vm layer above (open primitive registry,
measure→arrange, seven-op grammar, matter-room versioning) is
tool-agnostic — the gap is unbuilt atoms, not hostile architecture.
"Not possible" was never true; "not present at the atom layer" is. The
mirage: UI chrome is exactly the rect+text subset of vector graphics, so
Softland's screens looked like a design tool's output while possessing
none of a design tool's geometry.

## The three tiers (Sid's ask: current · tldraw · figma)

- **Tier I — current / rect-world.** Fluency over the existing atoms:
  select, move, anchored resize, restyle, annotate, fork — AND structure
  (draw a frame, put parts inside, rearrange/nest) since the seven-op
  grammar already has add/remove/reorder. No new renderer work.
  Playground 1 proved the fluency half; cut 2 surfaces the structure
  half. Sid's "make the rects and then minutely fix them" lives here.
- **Tier II — tldraw-class.** Lines, arrows, ellipses, freehand strokes,
  simple polygons; rotation through transform+pick; point-in-shape hit
  testing. ONE new renderer atom (path/stroke pipeline) + a shape family
  in the registry. Law 8's ink door physically opens here. Copyable
  decade: tldraw's editor model (docs/source-available) for interaction
  patterns — its renderer is DOM/SVG and does not transplant.
- **Tier III — figma-class.** Pen/bezier authoring, boolean ops, full
  typography, images, effects. Engines adoptable rather than reinvented
  (Skia/CanvasKit pathops, HarfBuzz shaping — domain knowledge, not in
  repo); the part copyable from nowhere is Softland's own thesis:
  authored geometry as LIVING material (versioned, functional,
  promotable, agent-editable, causal provenance). Softland's honest
  advantage is narrower but deeper than Figma's, and current
  human-facing capability does NOT exceed Figma — the architecture
  opens a path beyond it in specific dimensions (G7 keeps us honest).
  Multiplayer is deferred as itself; the agent hand is a different
  capability, not its replacement.

Tiers are deliberately UNPRICED — playground evidence prices them.

## Playground round 1 (2026-08-01)

Shipped (builder subagent, in-memory, disposable): door at viewport edge
→ camera-travel to the room (~x=100k) → shelf reading the real master
registry in plain words → "Take one" births a live typable Block
specimen → visible selection (glow ring), threshold-drag move, corner
resize anchored top-left → inspector (Fill/Frame/Corner/Text size/
Padding, click-cycle, instant) → typable margin notes with threads →
Variation forks with threads → Back door. Receipts: compile 279 files /
0 warnings · banks 30t/510a 0/0 · two additive lines in ground.cljs,
no existing hunk touched. Files: `src/app/shared/workshop_playground.cljc`,
`src/app/client/workspace/workshop_playground.cljs`.

**Sid's reaction — the arc's first positive lived signal:**

> ok btw this is much much useful to the previous halo and then last
> workshop example and smalltalk ui vm examples that were there — good
> iterative job

> now i don't know what to do next this is exhausted … maybe showing that
> now we can go to workshop, create a existing component, modifying its
> shape and nature, making forks to work off of, talking about each
> instance seperately … what is not possible is to build on top — i will
> give you the same example again of figma or for that matter drawing out
> outlines with pen-paper or tldraw — like atleast conceptualizing whats
> in mind as first step and then filling in the details later on

Margin thinking LIVED (law 7, first time ever): he thought in running
commentary typed into specimens, judged a fork against its parent, and
asked a design question from inside a note: *"where is the equivalent
for the workspace?? we should be able to say stuff about the workspace
itself"* — banked as a design contribution (workspace-level notes).

**Defects banked (round 1):** new notes stack on one spot (no cascade) ·
forks don't inherit text size · padding off · note layering/rendering
roughness ("the rendering is bitch — maybe for a different layer of
task") · right-click in room still opens the old halo · durable camera
settle can restore a reload INTO the room (furniture self-heals,
specimens gone — playground rule).

**The wall, precisely located:** not geometry, not Tier II — STRUCTURE
VERBS. Sketch an outline, put things inside things, arrange a new whole,
refine after. The grammar already speaks this; no surface exposes it.

## Next: cut 2 — the structure pencil (starter, paste-able)

> **Build session — Workshop playground cut 2: the structure pencil.**
> Cheap lane: builders on **Sonnet**, model explicit, never inherited.
> Boot: this file + `workshop_playground.cljc/.cljs`. Scope: sketch
> verbs in the room — draw a new empty frame on the table; draw INSIDE a
> selected specimen (add-part through the existing grammar ops); drag
> parts to rearrange/nest; details after structure ("conceptualize
> first, fill in later"). Fix round 1: note cascade, fork inherits text
> size, padding, note layering. Playground rules unchanged: in-memory,
> disposable, fake-over-delay, felt bar absolute (visible selection, one
> body, anchored transforms, plain words). Sid plays cold at the end;
> his verbatim reaction is the round's record, banked here.

## Process rulings (Sid, 2026-08-01)

- **The playground stream**: "build broad version and this is what it
  unlocks, go play while i build more — totally fine if by building more
  you are breaking the previous playground, it was just a slapped
  example." Ambition licensed ("fine to overpromise") WITH middle
  deliverables that keep him genuinely busy in Softland.
- **Fake or omit over delay** — a walkable loop in his hands beats full
  scope; the felt bar is the one thing never slapped.
- **Design before contract** for anything with a face; the recognized cut
  is the spec; contracts return only for durable substrate work.
- **Builders ride cheap lanes** with rich felt-spec briefs carrying his
  verbatim words; model always explicit (cut-1 builder left on inherit
  ran the orchestrator's model, ~335k tokens — the lesson is in memory).
- **Machine gates keep substrate truth only**; meaning is judged by
  Sid's hands at short cadence; one-shot cold lived gates reserved for
  arc ends.

## Tree state

Code UNCOMMITTED in two layers — studio P1 (ground.cljs,
facet_master.clj, server_jetty.clj, tests, studio.cljc ×2) + playground
cut 1 (workshop_playground ×2 + two ground.cljs lines). Archiving vs
survivorship are separate rulings (Sid), both deferred to a fresh
session. Docs land on `docs/current-mental-model-local` as usual.
