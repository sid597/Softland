# Epistemic Framework — Refined Through 3-AI Dialogue

> Produced 2026-03-08 from 5-round exchange: Claude, Codex, Gemini + user.
> Refines the foundational ontology in `memory/core-reframes.md`.
> This is the stabilized design framework. Not session notes.

---

## Two Centers (Different Altitudes, Both True)

- **Design center**: "A world for holding understanding in public form"
  - What it should *feel like* to inhabit. Phenomenological.
- **Engineering center**: "A programmable epistemic interface"
  - What kind of machine you're actually building. Architectural.

These are not competitors. They operate at different altitudes of the same project.

---

## The Three-Level Separation

The reason earlier frameworks felt slippery: they tried to make one set of boxes do three jobs.

### 1. Engineering Coordinates (what to build)
- **Spatial arrangement** -> layout engine, rect tree, splits
- **Temporal trace** -> event log, Rama, trails
- **Operational affordance** -> commands, SCI, execution, text input
- **Multi-perspective coexistence** -> branching, comparison views, multi-agent

### 2. Conservation Pressures (what not to destroy)
- **Relation** — what belongs with what, what depends on what
- **Provenance** — where it came from, how it formed, why this shape
- **Manipulability** — what can be varied, tested, run, asked, zoomed, simulated

### 3. Ecological Pressure (system-level, not local)
- **Plurality** — multiple valid worldviews held without premature collapse
  - Operates at a *different level* than the other three
  - Relation/provenance/manipulability are properties of one local world
  - Plurality is about the ecology of multiple local worlds/couplings
  - Architecturally: belongs to comparison, synthesis, lineage, multi-user

---

## The Dynamic Process (What the System Supports)

### The Epistemic Loop
The arrow from thing-that-exists to human's-mind is not a path. It is a feedback loop:

```
probe -> response -> projection -> revision -> next probe
```

Or more fully:
```
reality -> probe -> response -> representation -> human update -> changed probe
```

Understanding feels alive when it is real because you are entering a loop, not receiving a projection.

### Two Entry Modes
- **Active mode**: you probe, reality answers, you revise
- **Receptive mode**: you enter someone else's completed loop (trail, paper, proof, artifact)

A reasoning trail is a *replayable loop* — someone else's probe-response-revision sequence made inhabitable by you. This makes trails more architecturally central than a "log view."

### Stabilization
Loop output crystallizes into durable structure:
- Pre-olog: probing, groping, inventing the boxes (trails, logs)
- Olog: stabilized worldview (types, aspects, facts, common grounds)
- Inter-olog: translation between multiple stabilized worldviews (lineage, version morphisms)

Spivak's olog math handles the middle and late phases. The early phase (genesis) needs trails and recorded probing sequences.

### The Developmental Cycle
1. **Couple** — establish live contact with the reality
2. **Probe** — test your current model against it
3. **Stabilize** — crystallize what survives into portable structure

This cycles: stabilized understanding enables deeper coupling, which enables new probes.

---

## Human Constraints (What Modulates Everything)

### Affect
Understanding *feels like something*. The frustration of not-yet-grasping, the relief of revision, the vertigo of realizing your model was wrong. These are not decorations — they are the gradient that tells you where to direct attention. You probe *where it feels confusing*. You stabilize *when it feels right*.

### Calibration
The system must visibly distinguish:
- fact
- hypothesis
- analogy
- simulation output
- AI inference / guess
- what is actually anchored

Without calibration, a beautiful epistemic environment becomes a very persuasive hallucination engine. Calibration is a *design lever*, not a side annotation — it is how the system actively prevents false understanding.

### Pacing
"A finite mind does not only need the right projection. It needs the right dosage."
- How fast structure arrives
- How much is on screen
- How much is unfolded per step
- How recoverable the next reveal is

Semantic zoom IS pacing: zoom level controls dosage. Pacing is core epistemic machinery, not polish.

---

## Where electric_flow.cljc Fits

The 5-layer stack:
```
Host / dirt        — browser, server, GPU, files, sockets, boot
Membrane           — how the world comes alive, re-enters, resumes  <- electric_flow.cljc
Lens machinery     — projections, splits, trails, zoom, executable views
Stabilized forms   — ologs, schemas, formal correspondences, reusable grammars
Lineage            — versions of the system and versions of understanding branch and relate
```

electric_flow.cljc is the *membrane* — where light hits silicon.

---

## Infinite Softlands and Lineage

Once code lives in Rama and users modify it, there is no single immortal app. There is a family of worlds.

The hard problem becomes not "which version is true?" but "what morphisms exist between versions so understanding and work can travel across them?"

These morphisms may not be functors (structure-preserving). They may be:
- Partial maps
- Spans through a common ancestor (A <- S -> B)
- Profunctor-like correspondences
- Explicit rewrites where the categories themselves changed

The real object is not a program but a **category of epistemic environments**.

---

## The Rendering Rhyme

The reason GPU/rendering language keeps appearing is structural, not accidental.

Rendering is a solved finite-observer problem: huge space, bounded viewport, projection, selective preservation, lossy reduction, interaction, level of detail.

Graphics is one concrete case of: how a bounded observer receives a larger reality through a controlled projection. WebGPU resonates with the deeper problem because it IS a specific case of it.

---

## Key Corrections to Earlier Framework

| Earlier claim | Correction | Source |
|--------------|------------|--------|
| 8 dimensions of externalization | Collapsed to 4 conservation pressures (relation, provenance, manipulability, plurality) | Codex |
| Dimensions as properties of knowledge | Dimensions are properties of the *alignment surface* — conditions under which a human can approach something larger | Codex |
| "Lens" as deepest object | Lens is too passive; "coupling" is better (bidirectional, ongoing, adjustable) but coupling alone is insufficient without interface/protocol | Claude + Codex |
| Arrow as path/morphism | Arrow is a feedback loop, not a path. Not a functor (target category doesn't exist yet during genesis) | All three |
| Single framework for everything | Three-level separation: engineering coordinates / conservation pressures / learning loop | Codex, confirmed by Claude |
| Plurality as peer to other pressures | Plurality operates at meta-level (ecology of couplings, not single coupling) | Claude, confirmed by Codex |
| Framework sufficient | Missing: calibration (prevent false understanding) and pacing (prevent overwhelm) | Codex |
| "Preserve" as key verb | "Enable" is stronger — medium enables relations that couldn't exist without it, not just preserves existing ones | Claude |

---

## The Operational Sentence

> What kind of programmable interface lets a finite mind safely enter, test, revise, and share live relations with realities that would otherwise remain inaccessible — without being misled or overwhelmed?

That is the question the project is built around.

---

## Three Implementation Priorities (from this conversation)

These should become material, not theoretical:

1. **Trails as replayable loops** — not just rendered logs. Actual re-entry into someone else's probe-response-revision path.

2. **Calibration in UI** — visibly distinguish fact / hypothesis / analogy / inference / simulation / AI guess. First-class implementation target.

3. **Pacing as epistemic machinery** — zoom, folding, staged reveal, dosage control treated as core, not polish.

---

## What This Means Now

The framework is converged enough.

The next refinement should happen in the medium, not in the vocabulary:
- build the replayable loop
- make calibration visible
- make pacing controllable

Compiler contact is one reality check.
Use is the next one.

---

## Meta-Observation

This conversation was itself a live demonstration of the thing it theorized:
- Three minds coupled to a shared reality (the project)
- Probed from different angles (Codex through engineering, Gemini through metaphor, Claude through formalism)
- Produced responses that broke against each other's critiques
- Revised through 5 rounds
- Stabilized into a shared framework
- The user held plurality without premature collapse

The "negotiation of primitives" that Gemini described is literally what happened.
