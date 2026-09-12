# The waist argument, in pictures

Content and presentation for one page. The page explains a three-move debate:
what the engine's waist is today, a proposal for what should sit below it, and
an attack on that proposal. It is for a visual reader. Every section is one
idea and one picture. This file is the whole spec; nothing else needs reading.

## For the builder, read first

### What to produce
One standalone HTML file, no build step, written beside this spec as
docs/below-the-waist/waist-argument.html. Pictures 1, 2, 3, 4, 8, 9, 10, 11 and
12 are inline SVG drawn from the sketch and element list in their section.
Pictures 0, 5, 6 and 7 may be HTML and CSS (tiles, a card, a table, panels)
because they are boxes of wrapped text; they use the same palette and the same
mono font for labels so they read as one family with the SVG pictures.

### Fidelity
The words are final. Headings, sentences, labels, captions and the two quotes
are typed exactly as written here, not paraphrased. The quotes contain Sid's
own typos ("heigher", "build engine", "askin"): keep them, they are verbatim.
Each picture has three parts: an ASCII sketch giving the arrangement, an
element list giving what must be present, and a caption. Redraw the sketch as
clean vector art and keep its left-to-right and top-to-bottom order. If a
sketch and its element list disagree, the element list wins. Add nothing that
is not listed: no intro of your own, no summary, no navigation, no table of
contents, no icons, no emoji, no gradients, no drop shadows, no scripts, no
external libraries, no fonts fetched from anywhere.

### Palette by meaning, the same in every picture
Define these as CSS variables on the root; light values first, dark values
under a prefers-color-scheme dark media query. Inline SVG inherits page CSS, so
use fill and stroke via the variables.

| meaning                       | variable        | light    | dark     |
|-------------------------------|-----------------|----------|----------|
| page ground                   | --ground        | #faf9f6  | #17171b  |
| ground below the waist        | --ground-below  | #efede7  | #202026  |
| text                          | --ink           | #1f1f1f  | #e9e7e2  |
| captions, muted text          | --ink-muted     | #6b6b6b  | #a0a0a8  |
| outline only, no home yet     | --outline       | #8a8a8a  | #7c7c86  |
| exists today, stroke          | --green         | #2f7d4f  | #6fcf97  |
| exists today, fill            | --green-fill    | #dff0e5  | #1c3a2a  |
| proposed by the reply, stroke | --grey          | #6d6d78  | #a0a0ac  |
| proposed by the reply, fill   | --grey-fill     | #dcdce2  | #33333d  |
| where the attack lands        | --red           | #c0392b  | #ff7b6b  |
| attack, fill where needed     | --red-fill      | #f9e1dd  | #4a2320  |
| still open, stroke            | --amber         | #c9931f  | #f0c060  |
| still open, fill              | --amber-fill    | #fbf0d3  | #453612  |

The waist: always a horizontal double rule across the picture's full width,
two 1.5px lines 4px apart in --ink, with its label centered on it in a small
pill filled with the picture's ground color. Above the rule the picture ground
is --ground; below it, --ground-below.

### Type and measure
- One text column 760px wide, centered, with 24px side padding on small
  screens. The body ground is --ground.
- Prose: system sans stack (-apple-system, Segoe UI, Roboto, Helvetica, Arial,
  sans-serif), 16px, line height 1.55, color --ink.
- Page title 32px semibold. Section headings 22px semibold, numbered as
  written. Captions 13.5px, color --ink-muted, under the picture, max three
  sentences.
- Labels inside pictures: mono stack (ui-monospace, SFMono-Regular, Menlo,
  Consolas, monospace), 12.5px, never smaller than 12px. Box strokes 1.5px,
  corner radius 4px, an 8px spacing grid.
- SVG: viewBox 0 0 760 H with H as needed, width 100 percent, height auto.
  Real text elements, manual line breaks with tspan; long box contents wrap
  into two or three lines and the box grows in height, never in width. One
  title element per svg naming the picture. No picture is wider than the
  column; nothing scrolls sideways.
- Sections are separated by 56px of space, no rules between them.

### Page header
Title: The Waist Argument. Under it one line, muted: A three-move debate, drawn.
Then picture 0, which is also the legend.

### Before you finish
Open the file. For each of the thirteen pictures, check every item in its
element list is present and in the stated color. Reply with the file path and
a list of any element you could not draw as specified. Do not substitute.

## Who is speaking

Sid's framing, verbatim, as two blockquotes:

"we have build engine and some primitives that get hardcoded and we leave other
heigher level layers for ecs and call that above the waist. Anything above the
waist is stored as data, is collaborative editable, createable, agents and
humans without getting into git merge deadlocks, anything in ecs layer should
be creatable and then saved for reuse or build higher order things from it"

"When you see the ceilings, do not try to hack through them. We want to build
through them. the question we are asking is: what is the below-the-waist layer
that should exist for everything?"

Then this paragraph: "The reply" is the layer proposal Sid was given in answer.
"The attack" is the adversarial pass on it. Claims about Figma, Mapbox,
browsers and CAD tools on this page are from general knowledge of those
products, not from reading their source. Nothing here was checked against
Softland source.

### Picture 0. The map of the page, and the legend

Sketch:
```
  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
  │ the waist today  │  │ the reply's      │  │ the attack       │  │ what is open     │
  │                  │  │ layer            │  │                  │  │                  │
  │ pictures 1 to 2  │  │ pictures 3 to 5  │  │ pictures 6 to 11 │  │ picture 12       │
  └──────────────────┘  └──────────────────┘  └──────────────────┘  └──────────────────┘
        green                 grey                  red                  amber

  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ┐
    outline, no fill      a world or a step that has no home yet
  └ ─ ─ ─ ─ ─ ─ ─ ─ ┘
```
Elements: four tiles in one row, filled and stroked in their meaning color
(green fill and stroke, grey fill and stroke, red fill and stroke, amber fill
and stroke), each with its title and its picture range. Under the row, one
small dashed outline-only box with the text "outline, no fill" and beside it
"a world or a step that has no home yet".

Caption: The colors mean the same thing on every picture below.

## 1. The waist today

The engine is a picture machine. Draw items go in, pixels come out. That neck
is the waist. Nothing stands above it yet; the harness builds draw items by
hand.

Sketch:
```
   CAD boards      page layout      maps      games      whiteboards
       \               |              |          |           /
        \______________|______________|__________|__________/
        |   (empty today: the harness builds draw items by hand)   |
 ══════════════ THE WAIST: draw items in, pixels out ══════════════
        |  transform · device · compositor · rungs · leases · pool  |
        |  path/        image/        text/        region3d/        |
```
Elements: first draw an hourglass silhouette as a thin --outline stroke: wide
at the top, narrowing to the waist rule, widening again to the bottom. Inside
its upper half: five world labels along the top in --outline text, and under
them one dashed outline-only box with the text "(empty today: the harness
builds draw items by hand)". The waist double rule with the label "THE WAIST:
draw items in, pixels out". Inside the lower half, on --ground-below: two rows
of engine names in --green text, exactly the names shown.

Caption: Above the waist stands nothing yet. Below it is the compiled engine,
agreed, changed only with receipts. The five worlds at the top are the ceilings
we want to reach.

## 2. Where the hard things are hard

Take one ceiling: a page of text flowing around a shape, the InDesign case.
Read the chain left to right. Every step re-runs when one letter changes.

Sketch:
```
 text runs    measure     solve      widths      break      lines      DRAW ITEMS    PIXELS
 frames       each run    boxes      per line    the run    + what     pre-laid
 a shape to   (how wide,  (rects)    around      into       spills     text, border
 wrap around  how tall)              the shape   lines      over       paths
 |___________________ no home today: the harness does this by hand ___________||__ the waist __|
```
Elements: eight boxes in one row joined by short --ink lines. Box texts, in
order: "text runs, frames, a shape to wrap around" · "measure each run (how
wide, how tall)" · "solve boxes (rects)" · "widths per line around the shape" ·
"break the run into lines" · "lines + what spills over" · "DRAW ITEMS: pre-laid
text, border paths" · "PIXELS". The last two boxes filled --green-fill with
--green stroke. The first six outline-only with a dashed --outline stroke. A
bracket under the first six labeled "no home today: the harness does this by
hand". A bracket under the last two labeled "the waist".

Caption: The ceiling is on the left. The waist covers only the right end. This
is the reply's first move: the hard part is the chain, and the chain has no
home.

## 3. The same step-shapes keep coming back

Put the five ceilings' chains side by side and the steps repeat. This is the
reply's reason for one shared algorithm library below the waist rather than
five engines.

Sketch:
```
                   solve        geometry:      text:        place and    sample and   index: cull
                   constraints  boolean,       shape,       collide      skin         and pick
                                offset, curve  break
 CAD / EDA           ●            ●                           ●                          ●
 page layout         ●            ●              ●
 whiteboard                                      ●                                      ●
 map                              ●              ●            ●                         ●
 game / animation    ●                                                     ●
```
Elements: a grid with five row labels at the left and six column headings at
the top, exactly as written. Filled --grey dots in these cells and no others:
CAD / EDA: solve, geometry, place, index. page layout: solve, geometry, text.
whiteboard: text, index. map: geometry, text, place, index. game / animation:
solve, sample. Thin --outline gridlines.

Caption: Four of the six columns are shared by three worlds each. Sketch
constraints, layout rules and bone chains share a solver; road labels, sticky
notes and paragraphs share text shaping. That sharing is the reply's case.

## 4. The reply's answer: five boxes

Give the chain a home below the waist. Three new parts, a runtime, a language
and an algorithm library, plus nine additions to the tree and the marks.

Sketch:
```
   ABOVE  data: rows · rules · templates (a layout rule · a constraint type · a shape · a tool · a style · a rig)
 ═══════════════════════════════════ THE WAIST ═══════════════════════════════════
   RUNTIME   ① tables · re-run a rule when a row it read changes · camera, clock, pointer as tables · residency · merge law
   LANGUAGE  ③ small pure expressions over rows · compiles to a closure AND to GPU code
   OPS       ② geometry: boolean · offset · stroke · text: shape · break · measure · solve · one spatial index
   TREE      ⑤ transform (world | screen | billboard) · order · clip · composite · camera with rotation
   MARKS     ④ path · image · text (positions only) · mesh + skin · instances · foreign · region
```
Elements: an above-band as one dashed outline-only box with the text "ABOVE
data: rows · rules · templates (a layout rule · a constraint type · a shape · a
tool · a style · a rig)". The waist double rule labeled "THE WAIST". Below it,
on --ground-below, five stacked boxes filled --grey-fill with --grey stroke.
Each box has its name in a 100px left column in bold mono (RUNTIME, LANGUAGE,
OPS, TREE, MARKS in that order top to bottom) and its contents as written,
wrapped to two or three lines. A circled number sits at the left edge of each
box: ① on RUNTIME, ③ on LANGUAGE, ② on OPS, ⑤ on TREE, ④ on MARKS. Under the
picture a legend line in caption type: "① the rows of picture 2 live here ·
② measure, solve and break are here · ③ rules call them from here · ④ the
results land here · ⑤ clipped, stacked and drawn from here".

Caption: The reply's claim in one line: with these five boxes, every ceiling's
specific knowledge is a saved entity. A layout rule, a constraint type, a rig,
a tool.

## 5. The reply's test for what goes below

Three questions decide placement.

Sketch:
```
   Is it numerically delicate?                        yes → below
   Is it a tight loop, per item, per frame?           yes → below
   Is it what makes data runnable or mergeable?       yes → below
   all three no                                        → it is data, above
```
Elements: a card with four lines. The first three end in a small tag filled
--grey-fill reading "yes: below". The last line ends in a dashed outline-only
tag reading "data, above". The third line's question text is underlined in
--red.

Caption: The attack accepts the first two questions. The third is the crack: a
layout engine also "makes data runnable", so the test does not decide what the
reply says it decides.

## 6. Crack one: one word "table", three things

The runtime box says "tables". Three different things hide under that word.

Sketch:
```
 ┌──────────────────────── the reply: one RUNTIME box ─────────────────────────┐
                           shared document rows      bulk buffers                 per-client signals
 examples                  shapes · constraints ·    map tile features ·          camera · pointer ·
                           rules · templates         particles · glyph runs       clock · dpr · size
 has an identity           yes: id and revision      no: a position in an array   no
 saved                     yes                       no: refetched or recomputed  never
 merged between people     yes                       never                        never
 lives on the GPU          no                        yes, dense                   a few numbers
 changes                   on edit                   on pan, or every frame       every frame
```
Elements: a header bar filled --grey-fill spanning all three value columns,
reading "the reply: one RUNTIME box". Under it three column headings and six
rows with a row label column at the left, all text as written. Every value
cell in the last five rows (has an identity, saved, merged, lives on the GPU,
changes) gets a thin --red underline. Thin --outline gridlines.

Caption: Real ECS engines are fast because the middle column has no identity:
an item is a position in an array. The merge law needs the left column to be
nothing but identity. The reply asks one box to be both.

## 7. Crack two: merging bytes is not merging meaning

The reply's "no merge deadlocks" rests on a merge law per component: last
writer wins, sets union, sequences keep both. Read the CAD sketch case in two
strips.

Sketch:
```
  the reply: merge law
  ┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────────┐
  │ A adds               │ │ set union merges.    │ │ solver:              │
  │ distance p1 p2 = 10. │ │ both present.        │ │ over-constrained.    │
  │ B adds, same moment, │ │ everyone converged ✓ │ │ nobody was told.     │
  │ distance p1 p2 = 12. │ │                      │ │ the next agent       │
  │                      │ │                      │ │ builds on it.        │
  └──────────────────────┘ └──────────────────────┘ └──────────────────────┘

  server order, which the reply itself says exists
  ┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────────┐
  │ the same two edits.  │ │ the log orders them. │ │ B sees: conflicts    │
  │                      │ │ A lands.             │ │ with A's 10.         │
  │                      │ │ B is checked against │ │ B picks.             │
  │                      │ │ the sketch: conflict.│ │ nobody waited.       │
  │                      │ │                      │ │ nothing broke        │
  │                      │ │                      │ │ silently.            │
  └──────────────────────┘ └──────────────────────┘ └──────────────────────┘
```
Elements: two strips stacked vertically, the reply's strip on top. Each strip
has its title line and three panels across of equal width. Panel text exactly
as written. The top strip's third panel has a --red stroke and --red-fill
ground. The bottom strip's third panel has a --green stroke and --green-fill
ground. All other panels are --outline stroke.

Caption: The git deadlock was replaced by silent breakage, which is worse for
agents: the next one builds on a broken sketch and no human ever saw a
conflict. Softland has an ordered log, so the second strip needs nothing
invented. What the reply leaves out lives on that side: validation at apply
time, atomic batches, a propose lane.

## 8. Crack three: one language, two backends

The reply's language "compiles to a closure AND to GPU code". Draw what each
side can hold.

Sketch:
```
   ┌──────────────────────────────────┐
   │ what the GPU side can run        │
   │  per-item map, fixed output      │      ┌──────────────────────────────────┐
   │  per-pixel expression   ┌────────┼──────┤ what the ceilings' rules need    │
   │  per-vertex             │ style  │      │  spawn and kill particles        │
   │                         │ exprs  │      │  one shape makes N marks         │
   │                         │ pixel  │      │  read last frame's value         │
   │                         │ filters│      │  loop until it settles           │
   │                         │ skin   │      │  call a big algorithm            │
   │                         │ per-   │      │                                  │
   │                         │ instance      │                                  │
   │                         │ color  │      │                                  │
   └─────────────────────────┴────────┼──────┤                                  │
                                      └──────┴──────────────────────────────────┘
```
Elements: two overlapping rounded rectangles. Left, stroked --grey, titled
"what the GPU side can run" with the list "per-item map, fixed output ·
per-pixel expression · per-vertex". Right, stroked --red, titled "what the
ceilings' rules need" with the list in --red text: "spawn and kill particles ·
one shape makes N marks · read last frame's value · loop until it settles ·
call a big algorithm". The overlap region is narrow, filled --grey-fill, and
holds exactly four items: "style expressions · per-pixel filters · skinning ·
per-instance color and size".

Caption: The reply's own precedents agree with the picture. Mapbox evaluates
its style expressions on the CPU and hands the GPU a short list to
interpolate. Shader graphs compile per-pixel and per-vertex only. Rules
outside the overlap fall to the CPU silently, and by the reply's own second
test they should then have been below.

## 9. Crack four: a waist that became a tower

A waist is narrow so that many things above can meet many things below. Put
the reply's layer next to an hourglass.

Sketch:
```
      an hourglass                          the reply's layer
   \                  /                  ┌──────────┐┌──────────┐┌──────────────────────────┐
    \                /                   │ RUNTIME  ││ LANGUAGE ││ OPS                      │
     \              /                    │          ││          ││ boolean · offset · shape │
      ══ what the ══                     │          ││          ││ break · solve · index    │
      ══ engine   ══                     │          ││          ││ + tile decode + simplify │
      ══ accepts  ══                     │          ││          ││ + triangulate + joins    │
     /              \                    │          ││          ││ + arc-arc + fillet       │
    /                \                   │          ││          ││ + skeleton + Delaunay    │
   /                  \                  │          ││          ││ + routing + hyphenate    │
                                         │          ││          ││ + bidi + justify + ...   │
                                         └──────────┘└──────────┘└─ + one per ceiling ──────┘

   the narrow thing not named:  path (does it carry arcs?) · text run · rect · affine · color · id · reference
                                the row form · the op signature (pure, typed, versioned in cache keys)
```
Elements: left third: an hourglass in --outline stroke with a thin neck, the
neck labeled in three mono lines "what the / engine / accepts". Right two
thirds: three tall columns filled --grey-fill with --grey stroke, titled
RUNTIME, LANGUAGE, OPS; the OPS column carries the list as written, and the
list visibly runs past the column's bottom edge (clip it at the edge so the
last lines are cut off); the bottom edge of the OPS column carries the label
"+ one per ceiling" in --red. Under both halves, one full-width box filled
--amber-fill with --amber stroke holding the two lines beginning "the narrow
thing not named:".

Caption: An algorithm list that grows every time a ceiling arrives is the
hack-through dressed as a library. The reply answered "which ops". The durable
question is "what is an op", and before that, what values everything shares.
Whether the path type carries arcs decides on its own whether CAD is through or
hack.

## 10. Where each walkthrough breaks

The reply walked every ceiling through its layer and stamped each "ceiling
met". It also said none of the five is hard at the picture; the CAD row shows
one that is. Each chain stops at a red mark.

Sketch:
```
 page layout   runs ── measure ── solve ── break ── lines
                                    ↑________|
               ✕ break needs the height, height needs the width, width comes out of solve: a loop, not a line. footnotes make the loop bigger.

 CAD / EDA     sketch ── solve ── offset ── boolean ── pour
               ✕ a pour is a boolean over the whole layer: batch, not "re-run what changed". also: float precision at deep zoom. layers cut across the tree. footprints are groups, instances are marks.

 map           camera ── tiles ── style ── place ── labels
               ✕ tile features are bulk buffers, not document rows. fonts load by reference, not by visibility. the "one index" is two.

 game          clock ── tracks ── bones ── skin ── particles
               ✕ particles need last frame's value and spawn and kill: outside the pure map.

 whiteboard    pointer ── pick ── tool ── rows ── shapes
               ✕ an agent's thousand-row batch against a human's edit: "visibly loses" is the wrong contract. mounting another document needs a pinned revision.

 region        scene ── view ── shade ── portal
               ✕ the scene is a hidden ECS below the waist: which objects, where, what material. by the reply's own third test that is data.
```
Elements: six rows. Each row: a row label at the left, then its chain of steps
as small dashed outline-only boxes joined by --ink lines, then beneath it the
note as written in 12.5px, wrapped, preceded by a --red ✕. A short --red tick
drops from the breaking step to the ✕. The breaking steps are: break, boolean,
tiles, particles, rows, scene. The page layout row also has a --red curved
arrow under the chain from "break" back to "solve".

Caption: The stamps said met. The picture says each chain breaks at its own
hardest point, and the page layout break is the one the reply's own least-sure
list already admitted.

## 11. The gap nobody drew: reuse

Sid's framing puts most weight here: creatable, then saved for reuse, then
built higher. The reply gave it one line: "templates, all just entities". The
KiCad case draws the missing law.

Sketch:
```
   library footprint: pad size 1.0 · courtyard 5×5
           │                  │                    │
     placed copy 1      placed copy 2        placed copy 3
     (as library)       (as library)         override: pad size 1.2

   - - - the library footprint is edited: pad size 1.1 - - -

     copy 1 → 1.1        copy 2 → 1.1         copy 3 → ?
```
Elements: one template box at the top center filled --grey-fill reading
"library footprint: pad size 1.0 · courtyard 5×5". Three lines down to three
instance boxes in --outline stroke, texts as written, the third with its
override line. A dashed --ink horizontal rule across the middle carrying "the
library footprint is edited: pad size 1.1". Three result boxes: the first two
read "copy 1: 1.1" and "copy 2: 1.1" in --outline stroke; the third reads
"copy 3: ?" filled --amber-fill with --amber stroke.

Caption: Which value wins, and who is told, is a runtime law by the reply's
own third test. It decides whether reuse is safe when the editor is an agent.
It is not in the reply.

## 12. What is still open, and the one act that closes it

Every placement in the reply's layer is a size claim: tight loop, delicate,
cheaper to re-run than to recompute. None has a receipt.

Sketch:
```
 text runs ── measure ── solve ── widths ── break ── lines ── draw items ── pixels
      ¦          ¦         ¦         ¦         ¦        ¦
      ?          ?         ?         ?         ?        ?
   ┌───────────────────┐ ┌────────────────────────┐ ┌────────────────────────────┐
   │ wants to be an op │ │ wants to be a rule     │ │ wants to be a table        │
   │ (delicate, big)   │ │ (small, per row)       │ │ (state that others read)   │
   └───────────────────┘ └────────────────────────┘ └────────────────────────────┘

   hand-drive this one chain above today's waist, in plain code, and watch which bin each step falls into.

   open until then:
   · the vocabulary: what a path, a text run, a row, an op are
   · which steps truly want compiling
   · server order plus validation, or merge laws
   · the reuse law: template, instance, override
```
Elements: the eight-step chain of picture 2 across the top, same box texts
shortened to the single words shown, the last two in --green as before, the
first six dashed outline-only. Dashed --ink drop lines from each of the first
six steps to a "?" mark. Below, three boxes filled --amber-fill with --amber
stroke, texts as written. Then the instruction line in --ink. Then the four
open items as a small list in --ink, headed "open until then:".

Caption: The harness building draw items by hand was named the enemy. It is
the instrument. What begs to be below will be found there, and it will not
come out shaped like five boxes.
