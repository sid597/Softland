# spatial-wayfinding — Dossier

Status: drafted, 2026-06-09
Seed links: none from the user — this is one of the fields named but unlinked,
added so the research isn't HCI-heavy. Sources below are standard, named from
knowledge (not freshly re-fetched).

## Map

Architecture, urban design, and cartography — the disciplines of making space
navigable and meaningful.

- **Kevin Lynch — "The Image of the City" (1960)** — how people build mental maps
  of cities. Five elements: **paths** (movement channels), **edges**
  (boundaries), **districts** (regions of shared character), **nodes** (junctions
  / focal points), **landmarks** (external reference points). *Imageability /
  legibility* = the ease with which parts cohere into a recognizable pattern. The
  foundational wayfinding text.
- **Christopher Alexander — "A Pattern Language" (1977), "The Timeless Way of
  Building," "The Nature of Order"** — design as a network of named, recurring
  solved problems (patterns); *the quality without a name* (QWAN) — the alive,
  whole feeling of good places; centers, wholeness, piecemeal growth, and
  *structure-preserving transformations*. (Software's "design patterns" were
  stolen from here.)
- **Cartographic generalization** — how detail drops *lawfully* as scale
  decreases: **selection, simplification, aggregation, displacement,
  typification**. A village becomes a dot; roads thin; labels drop by importance.
- **You-are-here research** — orientation maps must be *aligned to the viewer's
  heading* and *structure-matched* to the visible environment, or they confuse
  more than help (robust finding).
- **Gibson — affordances / ecological perception** — we perceive the environment
  as action possibilities; wayfinding is reading affordances.
- **Romedi Passini — "Wayfinding in Architecture"** — wayfinding as spatial
  problem-solving: decision-making + execution + information-processing; spatial
  legibility vs. signage.
- **Tobler's First Law** — "near things are more related than distant things"
  (the basis of locality / spatial autocorrelation).
- **Norberg-Schulz — genius loci** — places have identity and character; a
  phenomenology of place.

## Extract — the principles they solved

1. **Legibility from a small element vocabulary.** Lynch's five types let anyone
   build a mental map. A navigable world isn't "detailed" — it has clear paths,
   edges, districts, nodes, landmarks. Imageability is *designable*.
2. **Landmarks anchor orientation.** People navigate by memorable reference
   points, not coordinates. A world needs distinctive, persistent landmarks at
   every scale.
3. **Generalization: detail drops lawfully with scale.** Cartography solved
   semantic zoom centuries ago — you don't shrink everything, you *select,
   simplify, aggregate, displace* to keep legibility.
4. **You-are-here must align to the viewer.** Orientation aids work only when
   matched to the inhabitant's current heading and context.
5. **Patterns + QWAN.** Good places come from composing named, recurring
   solutions, judged by whether the whole feels *alive* — not from bespoke
   novelty. (Alexander)
6. **Every projection distorts; you choose what to preserve.** Cartography is the
   discipline of *honest, chosen distortion*.

## Transpose — what Softland derives (through the lens)

- **Lynch's five elements are a ready-made wayfinding grammar for the knowledge
  landscape** — arguably the single most actionable output of the whole research
  program. Name Softland's: *paths* (trails / workflows), *edges* (domain
  boundaries), *districts* (neighborhoods / local worlds — already in the vision),
  *nodes* (key claims / junctions), *landmarks* (canonical artifacts you orient
  by). This is the concrete spec for the "Navigable" criterion.
- **Cartographic generalization is the rigorous answer to "lawful semantic
  zoom."** Softland's riskiest claim has a 500-year-old solution discipline.
  Pair with the InfoVis dossier: Shneiderman's mantra says *re-represent per
  level*; cartography *names the operations* (select / simplify / aggregate /
  displace). Together they convert "semantic zoom" from a slogan into a
  checklist. **This is the load-bearing transposition of the whole program.**
- **Landmarks → Softland needs persistent, distinctive anchors at every zoom.**
  "Identity survives zoom" (the functor requirement) is, experientially, *the
  landmark stays recognizable from far away.*
- **You-are-here alignment → the orientation overlay is egocentric**, relative to
  the inhabitant's current focus and heading, not a god's-eye map.
- **Alexander's QWAN validates the user's "feelings first" evaluation.** "The
  quality without a name" *is* "does it feel like a place / have you used it in
  your dreams." Alexander legitimizes felt wholeness as a real, designable
  property — and supplies method (pattern languages, centers,
  structure-preserving transformation). For a feelings-first founder, this is the
  tradition that says your compass is valid *and* workable.
- **"Choose what to preserve" = the calibration discipline.** "One ontology, many
  projections" must, like a map, declare what each projection sacrifices.
- **Vocabulary:** paths/edges/districts/nodes/landmarks, imageability, the four
  generalization operations, you-are-here alignment, affordance, genius loci,
  QWAN, structure-preserving transformation.

## Guard — what is dangerous to borrow

- **Physical-space metaphors mislead for semantic space.** Knowledge has no fixed
  north; "distance" is contested; a claim can sit in two districts. Borrow Lynch's
  element *types* and the legibility goal — not a literal map. (Reinforced by the
  InfoVis warning on t-SNE distances.)
- **QWAN is inspiring but anti-operational.** It resists specification — a north
  star, not a spec. Pair it with the harder criteria so "it should feel alive"
  can't become an excuse for vibes.
- **Over-skeuomorphism.** A literal 3D knowledge-city could be cute and useless
  (the spectacle trap). The goal is legibility and orientation, achievable
  abstractly.
- **Static landmarks vs. fast-moving knowledge.** Cities change slowly;
  understanding changes fast. Anchors that should steady orientation may
  themselves move — a real tension with persistence.

## Open — questions this forces

1. **What are Softland's five Lynch elements, concretely?** A real, answerable
   design exercise — possibly the most useful single artifact to produce next.
2. **Which generalization operation maps to which zoom transition?** (select =
   drop detail; aggregate = cluster local worlds; simplify = compress trails;
   displace = avoid overlap.) Can they be made lawful / automatic?
3. **Is there a stable "north" for knowledge, or is all orientation egocentric?**
   (Connects to the DOI question in hci-infovis.)
4. **Does Alexander's "structure-preserving transformation" formalize "recoverable
   compression / fold-don't-sever"?** They sound identical — worth testing.
