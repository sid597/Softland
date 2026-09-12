# malleable-software — Dossier

Status: drafted, 2026-06-09
Seed links: Ink & Switch (malleable software, local-first), Geoffrey Litt, Folk
Computer (Omar Rizwan — omar.website / lobste.rs / forum.malleable.systems),
WonderOS, Jackson Dahl (dialectic with Litt); + Alan Kay / Smalltalk, HyperCard,
Bret Victor / Dynamicland (added — the lineage the links sit on).

## Map

The tradition that treats software not as a finished product you consume but as a
material you reshape. The nodes that matter:

- **Alan Kay / Smalltalk / Dynabook** (Xerox PARC, 1970s) — the computer as a
  *metamedium*: a medium that can become any other medium, fully under the user's
  control. Smalltalk is *live* — the whole system runs and is modifiable at all
  times; every object can be opened, inspected, changed; no black boxes. The
  Dynabook was for children to *author*, not just run apps. ("Personal Dynamic
  Media," Kay & Goldberg, 1977.)
- **HyperCard** (Bill Atkinson, 1987) — the most successful malleable medium ever
  shipped. Stacks of cards; millions of non-programmers built games, databases,
  teaching tools with no sense of "programming." The canonical *gentle slope*:
  use → tweak the layout → open the HyperTalk script → edit it live. Its
  discontinuation is itself a lesson.
- **Bret Victor** — "Inventing on Principle" (2012), "Media for Thinking the
  Unthinkable"; **Dynamicland** (2014–) — the principle of *immediate connection*
  between creator and creation; computing as a physical, communal room where
  programs are real paper objects, legible and hackable by everyone present, with
  no screens-behind-glass.
- **Ink & Switch** — "Malleable Software" and "Local-First Software" essays:
  modern apps are sealed silos that don't compose and that you don't own; the
  alternative is software you reshape, data you own (local-first / CRDTs), and
  tools that compose over a shared substrate.
- **Geoffrey Litt** — end-user / malleable software; LLMs as a way to lower the
  authoring wall further ("describe the tool you want, get it").
- **Folk Computer** (Omar Rizwan et al.) — Dynamicland-lineage; computing as
  communal physical objects, deeply legible and hackable; a reaction against
  opaque, individual, glass-bound computing.

What unites them: the wall between *using* and *making* should be a *slope*; the
system should be *live and inspectable*; software should be a *place/medium you
inhabit and reshape*, not a product.

## Extract — the principles they solved

1. **The gentle slope.** Power comes from a *continuum* consumer → tweaker →
   author, with no cliff. HyperCard's one-keystroke descent into the script is
   the canonical example. Wherever there's a hard wall ("now Learn To Program"),
   almost no one crosses it.
2. **Immediate connection / liveness.** The system is always running and
   inspectable; changes show their effect instantly. The cause→effect gap is
   where understanding and authoring-confidence die. (Victor)
3. **Uniform substrate / no black boxes.** Everything is the same kind of
   inspectable, modifiable object (Smalltalk objects, HyperCard cards, Folk
   pages). Uniformity is what *makes the slope walkable* — you reshape everything
   the same way.
4. **You own and compose the material.** Local-first: your data is yours; many
   small tools compose over one substrate, instead of sealed apps each hoarding a
   copy. (Ink & Switch)
5. **Legibility precedes hackability.** You can only reshape what you can *read*.
   Opaque magic cannot be inhabited or renovated. (Folk / Dynamicland)

## Transpose — what Softland derives (through the lens)

- **Zoom 100 is the gentle slope, literalized.** "The code editor IS Softland at
  zoom 100, the bedrock of the world" is HyperCard's descent generalized to a
  *continuous* gesture: inhabiting → tweaking → authoring becomes a camera dolly,
  not a mode switch. Strong validation — but it imports HyperCard's obligation:
  the descent must be smooth and ever-present. If reaching zoom 100 feels like
  "entering dev mode," the slope is already broken.
- **Renovation = the malleable thesis made architectural.** Softland's
  collaboration loop (requests flow *down* to zoom 100, new software flows *up*,
  merges to canonical) is the use→author slope turned into a social loop across
  people and zoom levels. The tradition's condition: each step must stay *live*
  (you see the renovation's effect) and *legible* (you can read what you change).
- **"AI interaction should be material, not output" gets a lineage.** Litt's
  LLM-as-tool-builder + Ink & Switch's composable tools say the AI's product
  should be a *malleable object you open and bend*, not a sealed answer. Softland's
  "material not output" is the malleable-software stance applied to AI.
- **Event-sourcing gives Softland a liveness HyperCard could not have.** Because
  Rama is append-only truth, Softland can offer *time-travel liveness*: not just
  "see the effect of a change now," but "replay how the world became this."
  Malleability + provenance fuse into something neither tradition had alone — a
  combination Softland is uniquely positioned to own.
- **Vocabulary this hands a non-designer:** gentle slope · immediate connection /
  liveness · uniform substrate · local-first / ownership · legibility-before-
  hackability · the metamedium. These are the words to judge any "make it
  editable" feature.

## Guard — what is dangerous to borrow

- **The malleable graveyard.** Smalltalk images, most Dynabook-likes, even
  HyperCard — beloved, then barely used at scale or abandoned. Total malleability
  = total responsibility + no shared ground. *Power is not inhabitability.* The
  habitability criteria are the antidote, but only if enforced.
- **Malleability vs. public form / handoff — a real tension, not a footnote.** If
  every inhabitant reshapes the world freely, "transferable local worlds" and
  "another mind can enter without reconstruction" erode into N incompatible
  Softlands. Folk / Dynamicland dodge this by being *co-located and communal* (one
  room, shared in person). Softland is *distributed* — so "infinite Softlands in
  Rama" must reconcile with a shared canonical form. This is the central
  unresolved tension the field hands you.
- **Liveness can lie.** Immediate connection makes a change *feel* understood; at
  scale that masks system-wide consequences (you see the local effect, not the
  global one). Calibration — "the map must not lie" — has to police liveness.
- **Don't fetishize "no programming."** The slope is real, but some structure is
  irreducible; pretending otherwise produces toys. The goal is a *walkable* slope,
  not a flat plain.

## Open — questions this forces

1. **One substrate, or three?** The gentle slope assumes a *uniform* object
   (Smalltalk object, HyperCard card). Softland has Rama / Electric / WebGPU. Is
   there a single inhabitant-facing "object" the slope reshapes — or does the
   slope *break* at substrate seams? If it breaks, malleability hits a wall and
   the thesis weakens. (The sharpest design question in this dossier.)
2. **What is fixed bedrock vs. renovatable?** Some spine must be fixed for
   handoff / public form to survive. Where is the line, and who draws it?
3. **How does distributed malleability reconcile with canonical form?** The
   merge-up loop needs a real model; preserved disagreement helps but isn't the
   whole answer.
4. **Can time-travel liveness (Rama) be the signature** that distinguishes
   Softland's malleability from the entire prior tradition?
