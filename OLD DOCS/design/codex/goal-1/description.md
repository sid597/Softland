# Goal 1 - Design Research Charter

Status: initial session brief, 2026-06-09.

## Goal

Create the charter for the Softland view/design research track.

This session should answer:

```text
What is this design track?
What is it not?
What does it need to prove?
What laws should govern future view work?
What questions should a future session refuse to blur together?
```

## Why This Goal Exists

Softland's view problem is too large to treat as normal UI design. The track
needs a stable frame before individual prototypes, research surveys, or specs
start accumulating.

The charter should protect against two failure modes:

```text
1. reducing Softland to ordinary dashboard/canvas/editor UI
2. staying poetic and never producing buildable view contracts
```

## Inputs To Read

Minimum:

```text
docs/current-mental-model/design/codex/orientation.md
docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md
docs/vision/epistemic-framework.md
docs/current-mental-model/architecture/object-container-spec.md
```

Optional, only if needed:

```text
docs/vision/what-softland-is-claude.md
docs/vision/what-softland-is-codex.md
docs/vision/terminology-glossary.md
```

## Research And Exploration Questions

Answer these in the user's language, but make the answers reusable:

```text
1. What is a Softland view?
2. What makes a view epistemic rather than decorative?
3. What must the view layer conserve?
4. What is the relation between view, projection, source, object, local world,
   and action?
5. What should be forbidden in early view design?
6. What counts as progress for this track?
7. What is the smallest prototype that can prove the track is real?
```

## Required Output Files

Create these files in this folder:

```text
charter.md
view-laws.md
track-glossary.md
```

## Required Format

`charter.md` should include:

```text
Status
Origin prompt
Core thesis
What the track is
What the track is not
Primary anxieties the UI must answer
Conservation pressures
First prototype family
Open questions
Next recommended goal
```

`view-laws.md` should include:

```text
10 to 15 laws
For each law:
  statement
  why it matters
  what violates it
  how a prototype can test it
```

`track-glossary.md` should define:

```text
view
projection
source
SourceArtifact
ObjectContainer
DerivedUnit
SourceAnchor
CompositionEdge
RelationEdge
local world
truth state
candidate
accepted
semantic zoom
ZUI
inhabitable
recoverable compression
```

## Done When

The next session can read `charter.md` and correctly explain why Softland's
first view should probably prove source-to-native trust before attempting a
global map.

The output should be broad enough to cover future knowledge worlds, but strict
enough to reject a pretty graph that cannot show provenance or uncertainty.
