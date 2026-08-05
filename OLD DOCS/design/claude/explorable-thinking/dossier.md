# explorable-thinking — Dossier

Status: drafted, 2026-06-09
Seed links: Nicky Case (ncase.me), Loopy (growable / MITRE Loopy2). + Bret Victor,
Distill, Observable/Jupyter, the spreadsheet, Papert (added). Sources named from
knowledge, not freshly re-fetched.

## Map

Making understanding inspectable, manipulable, and re-derivable — "tools for
thought."

- **Bret Victor** — "Explorable Explanations" (2011), "Up and Down the Ladder of
  Abstraction" (scrub a parameter, see the whole family of outcomes), "Media for
  Thinking the Unthinkable," "Learnable Programming." Core: understanding comes
  from *seeing and manipulating* dynamic behavior, not reading about it.
- **Nicky Case** — playable explorables (The Evolution of Trust, Parable of the
  Polygons); **Loopy** (causal-loop diagrams that *run*). Teach systems by letting
  you *play* and *enact* them; emotional, self-paced.
- **Distill.pub** — interactive ML research; the standard for "the paper IS the
  interactive artifact," explorable figures embedded in argument.
- **Notebooks — Jupyter / Observable** — literate computing: prose + live code +
  outputs interleaved; Observable's **reactive dataflow** (a cell changes,
  dependents recompute — a spreadsheet for narrative).
- **The spreadsheet (VisiCalc/Excel)** — the most successful tool for thought
  ever: a live, direct, immediate-feedback dependency graph usable by
  non-programmers. The gentle slope realized at scale.
- **Papert — Logo / constructionism** — learning by building; "objects to think
  with"; the computer as a place to *construct* understanding.

## Extract — the principles they solved

1. **See and manipulate the dynamic — don't read about it.** The model is live and
   perturbable; understanding is built by playing with behavior.
2. **Show the whole space, not one instance.** (Ladder of Abstraction) Scrub a
   parameter, see the family of outcomes; abstraction becomes navigable.
3. **Play teaches systems.** You *enact* the mechanism (you become the trust-game
   player); interactive, emotional, self-paced.
4. **Literate computing: prose + live model interleaved.** Argument and executable
   evidence in one surface the reader can re-run and modify.
5. **Reactive dataflow: change propagates** through a dependency graph that
   recomputes live (spreadsheet / Observable).
6. **Constructionism: build to understand** — objects to think with; rediscovery
   by construction.

## Transpose — what Softland derives (through the lens)

- **This field IS "the REPL for knowledge" — Softland's most direct ancestor.**
  The README's "no REPL, no visual model I can break/play," the "3D interactive
  C. elegans," "papers as logs you rebuild" — that *is* the explorable-explanations
  program. Softland is its generalization: from hand-authored explorables to **a
  world where every artifact is explorable by default.** This field proves the
  core thesis is real (others have done it small) and sets the bar.
- **"Rediscovery over consumption" = constructionism + Victor's manipulable
  model.** The user's value has a 40-year lineage (Papert: objects to think with).
  Borrow build-to-understand and scrub-the-parameter directly.
- **Reactive dataflow = "the artifact pole is executable," validated.** The
  memory's "preview pane is a REPL for the current local world" is Observable's
  reactive notebook; the interaction model already exists and the engineering
  already leans reactive (Electric). Softland generalizes it across substrates.
- **Ladder-of-abstraction scrubbing ↔ semantic zoom for *behavior*.** "See all
  values at once / scrub the parameter" is zoom applied to a model's behavior
  space — a *fourth* field (after film, music, HCI) confirming scrub/zoom as the
  master navigation idiom.
- **JIT + LLMs solve the field's fatal bottleneck.** Distill/Case explorables are
  *painfully hand-made* — that's why there are so few. Softland's JIT views + LLM
  ("describe the view, get it") make explorables *cheap and default* rather than
  artisanal. **This is Softland's genuine advance over the field: explorables at
  scale.**
- **Vocabulary:** explorable explanation, reactive document, ladder of abstraction
  / parameter-scrub, literate computing, constructionism / objects-to-think-with,
  reactive dataflow, the computational essay.

## Guard — what is dangerous to borrow

- **Explorables seduce and can lie.** An interactive model implies a
  confidence/completeness the underlying knowledge may lack — "you can play it, so
  it feels true." The model-must-not-lie risk is acute precisely when the model is
  *fun*; calibration must mark illustrative vs. fitted vs. validated.
- **Hand-authoring is the field's fatal ceiling** — if Softland's explorables
  aren't near-free (JIT), the vision collapses into "a few beautiful demos," the
  field's current state.
- **Interactivity ≠ understanding.** Aimless fiddling can substitute for thinking
  (the "insight porn" the user named, now interactive). Designed-discovery
  (game-worlds) must shape play toward inference.
- **Reactivity at scale is genuinely hard** (Observable dependency graphs get
  gnarly) — the uniform-substrate question returns.

## Open — questions this forces

1. **Can explorables be *generated* (JIT/LLM) at quality, not hand-built?** The
   single make-or-break question for Softland's core promise.
2. **What stops an explorable from lying** — how is a model's epistemic status
   (illustrative / fitted / validated) marked *inside* the playable surface?
3. **Is "scrub the parameter / see all values" the behavior-space form of semantic
   zoom**, and can it be a universal idiom?
4. **How is play *shaped* toward rediscovery** rather than aimless fiddling
   (designed-discovery from game-worlds applied here)?
