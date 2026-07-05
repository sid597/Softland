# hci-infovis — Dossier

Status: drafted, 2026-06-09
Seed links: Shneiderman ("insight not pictures" interview), Munzner (Nested Model
paper), Bederson (treemaps / IEEE test-of-time), Inselberg (parallel coordinates),
t-SNE / UMAP (metwarebio), Kittur. + Bertin, Tufte, Furnas, Card/Mackinlay,
Heer, Pirolli (added — the field the links sit on).
Note: knowledge-based synthesis; named works are real, not freshly re-fetched.

## Map

Information Visualization + HCI, as it bears on rendering large *structured*
knowledge legible and navigable.

- **Jacques Bertin — "Semiology of Graphics" (1967)** — the periodic table of
  visual encoding. Visual variables (position, size, value, texture, color hue,
  orientation, shape) and what each can express (selective / associative /
  ordered / quantitative). Encoding is a *grammar*, not decoration.
- **Edward Tufte** — data-ink ratio, chartjunk, small multiples, sparklines,
  "smallest effective difference," graphical integrity. The ethic: do not lie
  with graphics.
- **Ben Shneiderman** — the Visual Information-Seeking Mantra: *"overview first,
  zoom and filter, then details-on-demand"* (1996); dynamic queries; treemaps
  (space-filling hierarchy); direct manipulation. The linked line: *the purpose
  of visualization is insight, not pictures.*
- **Tamara Munzner — Nested Model (2009)** — four layers: domain problem →
  data/task abstraction → visual encoding + interaction idiom → algorithm. Key:
  an error at an outer layer invalidates everything inside it; validate at each
  layer. Effectiveness is governed by *perception*, not taste.
- **George Furnas — Generalized Fisheye Views (1986)** — degree-of-interest
  `DOI = a priori importance − distance from focus`. The formal basis of
  focus+context.
- **Bederson** — Pad++ / Jazz (zoomable UIs), ordered/quantum treemap layouts,
  space-scale diagrams (with Furnas).
- **Card, Mackinlay & Shneiderman — "Readings in InfoVis: Using Vision to Think"
  (1999)** — the InfoVis reference model (data → tables → visual structures →
  views, human in every loop); vis as *external cognition*.
- **Inselberg — Parallel Coordinates** — see N-dimensional data: each axis a
  vertical line, each datum a polyline; reveals correlation and clusters in high D.
- **t-SNE / UMAP** — nonlinear dimensionality reduction; the *map metaphor* for
  abstract data, preserving local neighborhoods. Caveat: between-cluster distance
  and cluster size are **not** meaningful; they can hallucinate structure.
- **Pirolli & Card — Sensemaking loop / Information Foraging** — forage →
  schematize → synthesize; "information scent"; the cost structure of finding and
  structuring knowledge.
- **Aniket Kittur** — sensemaking + crowdsourced synthesis of complex knowledge
  from many sources; directly adjacent to "synthesis from many minds."

## Extract — the principles they solved

1. **Encoding is governed by perception, not taste.** There is a ranked
   effectiveness of channels per data type (position beats color for quantity;
   hue is categorical). You *match channel to data semantics*; you don't pick a
   nice color.
2. **Overview → zoom/filter → details-on-demand.** The universal sequence for
   entering large data. Never details-first; never hide the overview.
3. **Focus + context.** Furnas DOI: show local detail *embedded in* global
   context, simultaneously — don't force a choice between detail and overview.
4. **Validate at the right altitude (Nested Model).** Most vis fails at the wrong
   task/data abstraction, not at pixels. Characterize the problem before encoding.
5. **Vis is a tool to think *with*, not a picture to look *at*** (external
   cognition / "insight not pictures").
6. **Graphical integrity.** Smallest effective difference; show uncertainty;
   never imply false precision.
7. **The map metaphor has limits.** DR maps distort global structure — a vis can
   lie by hallucinating clusters.

## Transpose — what Softland derives (through the lens)

- **The Visual Information-Seeking Mantra IS Softland's semantic zoom, stated 25
  years early.** "Overview → zoom/filter → details" = zoom 0.001 → 1.0 → 100.
  Softland is the *continuous* generalization of Shneiderman's *discrete* mantra.
  The warning rides along: the mantra works because each level is a *different
  representation chosen for that altitude's task* — so Softland's zoom levels must
  each be perceptually designed for their task, not "the same thing smaller."
- **The Nested Model is a gift to a non-designer founder.** Your hardest design
  risk isn't pixels — it's whether you've characterized the *task*. "Hold
  understanding in public form" is the domain layer; what's an object / arrow /
  trail is the abstraction layer; encoding comes last. And it gives you a way to
  *validate each layer separately* — a rigor backstop for a feelings-first founder.
- **Bertin's visual variables = the raw alphabet for the truth-state grammar.**
  Encode epistemic status (fact / hypothesis / guess / accepted / candidate) with
  the *right* channels; never color-alone. Calibration becomes an encoding budget.
- **Focus + context (DOI) is the simultaneous answer to "navigable" AND "not
  overwhelmed."** `importance − distance` is a literal formula for what to show at
  the current focus — the cure for both the hairball and the lost-in-detail
  failure at once.
- **The foraging/sensemaking loop is the actual cognitive shape of "inhabiting
  knowledge."** Softland is a place to *forage and synthesize*, not a display.
  "Information scent" = how Softland signals "something worth going to is over
  there." Kittur's synthesis work feeds "synthesis from preserved disagreement."
- **Vocabulary:** visual variables, the mantra, focus+context / degree-of-interest,
  the Nested Model, external cognition, information scent, the sensemaking loop,
  "insight not pictures."

## Guard — what is dangerous to borrow

- **Vis-as-spectacle** is the trap Shneiderman himself names. The hairball graph,
  the pretty t-SNE blob — impressive, useless. Beauty-over-insight is a product
  failure.
- **DR maps lie about global structure.** If Softland renders a "knowledge map"
  via t-SNE/UMAP, between-cluster distances are meaningless — calibration must say
  so, or the map literally violates "the map must not lie."
- **The mantra assumes a known dataset.** Knowledge is open, contested, growing —
  there may be no clean "overview." Softland's overview must show *uncertainty and
  incompleteness*, not a falsely complete map.
- **InfoVis is mostly quantitative; Softland's content is semantic/argumentative.**
  Borrow the perceptual grammar and interaction idioms; don't assume claims and
  evidence behave like scatterplots.

## Open — questions this forces

1. **What is Softland's "overview"?** For open, contested, growing understanding,
   what does "overview first" render — and how does it show what's *not yet known*?
2. **Can degree-of-interest be computed over a *semantic* graph** (claims /
   evidence / trails), not spatial distance? What are "importance" and "distance"
   in epistemic space?
3. **Which visual variables does Softland reserve** for epistemic status vs.
   content vs. provenance? (The truth-state grammar's encoding budget.)
4. **Is continuous semantic zoom perceptually honest** — or does "the same world
   at every scale" collide with the fact that each level needs a *different*
   representation? (The functor question, from the perception side.)
