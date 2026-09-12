# Softland Terminology Glossary

> Canonical vocabulary from the Claude + Codex + Gemini + user convergence.
> Use this to keep language stable across docs and sessions.
> See also `docs/vision/epistemic-framework.md` for the deeper framework.
> See also `what-softland-is-codex.md` (Codex's perspective) and `what-softland-is-claude.md` (Claude's perspective).

---

## Two Centers

- **Design center**: "A world for holding understanding in public form."
  - The felt purpose of Softland.
  - What it should feel like to inhabit.

- **Engineering center**: "A programmable epistemic interface."
  - The machine description of Softland.
  - What kind of system is being built.

These are not competing definitions. They operate at different altitudes.

---

## System Layers

- **Host / dirt**
  - Browser, server, GPU, sockets, files, boot conditions.
  - The physical/runtime reality underneath the world.

- **Membrane**
  - The activation threshold where the world boots and becomes inhabitable.
  - In the current codebase, `electric_flow.cljc` belongs here.

- **Substrate**
  - Lower-level machinery that makes the world possible.
  - Rendering and other host-facing technical foundations.

- **Workspace**
  - Shared interactive machinery.
  - Layout, trails, shell, interaction surfaces, shared views.

- **Workflow**
  - A task-specific way of using the workspace.
  - DG, JIT, file/chat, future knowledge workflows.

- **Lineage**
  - The ancestry and branching relation between versions of Softland and versions of understanding.

---

## The Foundational Metaphor

- **Place, not tool**
  - A tool you pick up, use, put down. A place you inhabit.
  - Softland is not *like* a place. It IS a place. Made of software.
  - Every elaboration (epistemic interface, projection workshop, coupling device) is unpacking what it means for software to be a place rather than a tool.

- **Habitability criteria**
  - Not "can the system represent knowledge?" but "can a human live here?"
  - The qualities that make a place habitable:
    - terrain must be navigable
    - weather must be survivable
    - history must be present
    - the map must not lie
    - others must be able to visit
    - renovation must be possible

---

## Core Structural Terms

- **Split**
  - Not "three panes" specifically.
  - A composition that keeps coupled things together so the user does not have to mentally reconstruct the relation.

- **Trail**
  - A structured record of reasoning or exploration.

- **Replayable loop**
  - A trail that another mind can re-enter, not just read.

- **Artifact**
  - The consequence or product of a workflow's understanding process.

- **Artifact slot**
  - The general place where a workflow shows its consequence.
  - Different workflows fill it with different artifact types.

- **Material -> process -> artifact**
  - A recurring triad behind many views:
    - source stuff
    - active transformation
    - resulting thing

---

## Three-Level Separation

- **Engineering coordinates**
  - Tell you what kinds of machinery to build.

- **Conservation pressures**
  - Tell you what the system must not destroy when compressing something larger into human-scale form.

- **Dynamic loop**
  - The process by which understanding actually forms.

These three levels should not be mixed.

---

## Engineering Coordinates

- **Spatial arrangement**
  - What is visible together.

- **Temporal trace**
  - What happened and in what sequence.

- **Operational affordance**
  - What can be run, changed, tested, asked, executed.

- **Multi-perspective coexistence**
  - How multiple views or minds can exist side by side.

These are build-coordinates, not deep metaphysics.

---

## Conservation Pressures

- **Relation**
  - What belongs with what, what depends on what.

- **Provenance**
  - Where something came from, how it formed, why it has this shape.

- **Manipulability**
  - What can still be varied, tested, run, or probed.

- **Plurality**
  - Multiple valid worldviews preserved without premature collapse.
  - Important: plurality is not just another local property; it is a system-level or ecological pressure.

---

## Dynamic Process Terms

- **Epistemic loop**
  - `probe -> response -> projection -> revision`

- **Active mode**
  - You are the one probing.

- **Receptive mode**
  - You enter someone else's loop through a trail, paper, proof, or artifact.

- **Stabilization**
  - When the output of loops hardens into portable structure.

- **Couple**
  - Establish live contact with the thing.

- **Probe**
  - Test your current model.

- **Revise**
  - Update your model.

- **Stabilize**
  - Keep what survived contact.

---

## Human Constraints

- **Affect**
  - What understanding feels like: confusion, relief, vertigo, insight.
  - Guides attention.

- **Calibration**
  - Marking what is fact, hypothesis, analogy, inference, simulation, AI guess, or anchored observation.

- **Pacing**
  - Controlling dosage, reveal speed, zoom level, and how much arrives at once.

These are not decorative. They are part of whether the system helps or harms understanding.

---

## Genesis Terms

- **The arrow**
  - The central question of the ontology-arc: "what is the arrow from the thing-that-exists to the human's mind?"
  - Not a single morphism. A feedback loop.
  - Person-specific, moment-specific, reorderable.

- **Pre-categorical genesis**
  - The process of forming categories from nothing.
  - Before distinctions are stable, before boxes exist.
  - The phase ologs cannot capture. Trails and probing sequences live here.

- **Negotiation of primitives**
  - Before you can translate between worldviews, you must agree on what the basic units are.
  - More specific than genesis: forming *shared* categories between two parties with incompatible ontologies.

---

## Formal / Category-Theory-Adjacent Terms

- **Olog**
  - A stabilized worldview made explicit as types, aspects, and facts.

- **Pre-olog**
  - The earlier phase where the right distinctions are not stable yet.

- **Inter-olog**
  - The translation/common-ground phase between stabilized worldviews.

- **Common ground**
  - The overlap between two different worldviews.

- **Functor**
  - A structure-preserving map between already-formed structures.

- **Span**
  - A weaker relation through a shared third thing.
  - Useful for branching/version ancestry.

- **Profunctor-like correspondence**
  - A looser alignment relation when direct structure-preserving translation is too strong.

Practical takeaway:
- ologs fit the stabilization phase
- trails/logs help with the earlier genesis phase
- lineage/common-ground structures help with the later multi-version phase

---

## Interface Terms

- **Lens**
  - An older metaphor.
  - Still useful sometimes, but too passive if treated as the deepest object.

- **Coupling**
  - A live relation between a finite mind and a larger reality.

- **Epistemic interface**
  - The programmable medium that establishes and tunes that coupling.

- **Programmable epistemic interface**
  - The engineering description of Softland.

Current stance:
- `lens` is too passive
- `coupling` is better but too broad on its own
- `epistemic interface` is the best engineering term

---

## Design Criteria Terms

- **Private reconstruction**
  - When the user has to rebuild an important relation in their own head because the system severed it.

- **Minimize private reconstruction**
  - Core design criterion.

- **Recoverable compression**
  - Hide by folding, not by severing.
  - Compress without destroying the path back.

- **Late-bound consensus**
  - Do not flatten disagreement too early.

- **Synthesis**
  - Not summary, but meaningful transformation of preserved plurality.

- **Preserved tension**
  - The unresolved relation kept alive in a split so understanding can continue.

---

## Preferred Vocabulary

Prefer:
- design center
- engineering center
- membrane
- substrate
- workspace
- workflow
- split
- trail
- replayable loop
- engineering coordinates
- conservation pressures
- plurality
- calibration
- pacing
- lineage
- olog / pre-olog / inter-olog
- minimize private reconstruction
- recoverable compression

Use carefully:
- **local world**
  - Useful as a conceptual unit, but not yet a required code abstraction.
- **coupling**
  - Strong idea, but too broad without saying what kind.
- **dimensions**
  - Less precise than `pressures`, `coordinates`, and `loops`.

Demote:
- the old "8 dimensions" as the main theory
- "3-pane" as if it were the primitive
- "webgpu" as if it were the ontology of the app

---

## Short Canonical Summary

- Softland's design center is a world for holding understanding in public form.
- Its engineering center is a programmable epistemic interface.
- It builds with engineering coordinates.
- It protects conservation pressures.
- It supports epistemic loops and replayable loops.
- It preserves plurality.
- It manages affect, calibration, and pacing.
- It lives across host, membrane, substrate, workspace, workflows, and lineage.
