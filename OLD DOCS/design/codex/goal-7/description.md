# Goal 7 - ZUI And Semantic Zoom Model

Status: initial session brief, 2026-06-09.

## Goal

Research and define Softland's ZUI / semantic zoom model.

This goal handles the "Google Earth for knowledge" angle directly, but keeps it
grounded in object identity, provenance, relation, and pacing.

## Why This Goal Exists

Softland has always had a continuous semantic zoom intuition:

```text
knowledge landscape
domain
topic
interactive model
software substrate
code itself
```

But zoom can easily become theatrical motion. For Softland, zoom is not a
transition effect. It is lawful compression.

The question is:

```text
What changes with scale, and what must survive every scale change?
```

## Inputs To Read

Minimum:

```text
docs/current-mental-model/design/codex/orientation.md
docs/vision/epistemic-framework.md
docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md
docs/current-mental-model/architecture/object-container-spec.md
```

Recommended if available:

```text
docs/current-mental-model/design/codex/goal-3/precedent-atlas.md
docs/current-mental-model/design/codex/goal-4/truth-state-grammar.md
docs/current-mental-model/design/codex/goal-5/scenario-softland-self-import.md
```

External research is expected. Prioritize primary or durable sources.

Suggested references:

```text
Pad++
Jazz
Space-Scale Diagrams
Ben Bederson ZUI work
Prezi
Google Earth
GIS layers
game world maps and minimaps
Blender viewport/navigation
DAW timeline zoom
video editor timeline zoom
Figma canvas zoom
Observable notebooks
semantic zoom in information visualization
focus+context techniques
overview+detail and fisheye views
```

## Research And Exploration Questions

Answer:

```text
1. What does ZUI mean for Softland beyond animated zoom?
2. What are the zoom levels for imported knowledge?
3. What object identity survives across levels?
4. What provenance survives across levels?
5. What relation information survives across levels?
6. What gets compressed at each level?
7. What must never be severed?
8. How does the user know where they are?
9. What is the way back?
10. What is the relation between semantic zoom and pacing?
11. How does ZUI interact with Source-to-World View?
12. What are early prototype slices that test zoom without building the full
    world map?
```

## Required Output Files

Create these files in this folder:

```text
zui-research.md
semantic-zoom-model.md
zoom-level-contract.md
navigation-primitives.md
zui-prototype-slices.md
```

## Required Format

`zui-research.md` should include:

```text
Status
Origin prompt
Sources researched
What ZUI historically means
What Softland should borrow
What Softland should reject
Risks
Open questions
Next recommended goal
```

`semantic-zoom-model.md` should define:

```text
Softland zoom levels
For each level:
  scale name
  user question
  visible entities
  compressed entities
  preserved identity
  preserved provenance
  preserved relations
  interactions
  failure modes
```

At minimum include:

```text
0.001 knowledge landscape
0.01 domain
0.1 topic/local world
1.0 ground interaction
10.0 substrate/projection machinery
100.0 code
```

`zoom-level-contract.md` should define invariants:

```text
Identity survives zoom.
Source anchors remain recoverable.
Truth state survives zoom.
Accepted/candidate distinction survives zoom.
The user always has a way back.
Zoom changes dosage, not truth.
```

`zui-prototype-slices.md` should propose small tests, such as:

```text
source artifact -> object outline -> object detail -> source span
local topology -> selected relation -> source evidence
Softland project map -> object-container thread -> code file -> source line
```

## Done When

The ZUI angle is concrete enough to guide future prototypes without forcing a
global map too early.

A strong output will make this distinction clear:

```text
ZUI is not zoom animation.
ZUI is navigable, lawful compression across scales of understanding.
```
