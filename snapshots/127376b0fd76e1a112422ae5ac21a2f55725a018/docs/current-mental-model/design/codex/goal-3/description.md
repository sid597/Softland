# Goal 3 - Precedent Atlas

Status: initial session brief, 2026-06-09.

## Goal

Build a precedent atlas for Softland's view layer.

This is not a moodboard. It is a research map of what other fields have already
learned about orientation, transformation, provenance, zoom, manipulation,
trust, collaboration, and inhabitable work surfaces.

## Why This Goal Exists

Softland should not start from unknowns alone. It should stand on the shoulders
of giants without copying the wrong surface forms.

The atlas should help the user and future sessions reason across:

```text
HCI
information visualization
ZUI and semantic zoom
explorable explanations
malleable software
professional creative tools
developer tools
data lineage systems
scientific visualization
games
architecture
photography and cinema
music software
```

## Inputs To Read

Minimum local context:

```text
docs/current-mental-model/design/codex/orientation.md
docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md
docs/vision/epistemic-framework.md
```

External research is expected for this goal. Use primary sources or official
project pages where possible, and cite URLs in the artifact.

Suggested starting references:

```text
Munzner nested model
Shneiderman visual information-seeking mantra
Bederson ZUI work
Pad++ / Jazz / Space-Scale Diagrams
Prezi
Google Earth / GIS
Nicky Case / Explorable Explanations
Distill
Loopy
Ink and Switch malleable software
Dynamicland
Folk Computer
WonderOS
Figma
Blender
Lightroom
DaVinci Resolve
Ableton
DevTools source maps
profilers and trace viewers
dbt lineage / DataHub
Obsidian local graph
parallel coordinates
UMAP/t-SNE cautions
game maps and minimaps
architecture wayfinding
cinematic montage and contact sheets
```

## Research And Exploration Questions

For each precedent, answer:

```text
1. What problem does this solve?
2. What does Softland borrow?
3. What should Softland refuse to borrow?
4. What view primitive could this inspire?
5. What failure mode does this precedent warn against?
6. What test would show the borrowing worked?
```

## Required Output Files

Create these files in this folder:

```text
precedent-atlas.md
annotated-bibliography.md
borrow-reject-table.md
research-gaps.md
```

## Required Format

`precedent-atlas.md` should group references by domain:

```text
HCI and visualization
ZUI and navigation
Explorable explanations
Malleable systems
Creative professional tools
Developer and lineage tools
Scientific visualization
Games
Architecture and spatial wayfinding
Photography, cinema, and music
```

Each entry should use this template:

```text
Reference
Source URL
Problem it solves
Softland borrowing
Dangerous borrowing
Candidate primitive
Relevance score: 1-5
Confidence: high/medium/low
```

`borrow-reject-table.md` should make hard calls:

```text
Borrow now
Borrow later
Study more
Reject for first prototype
Reject as misleading
```

`research-gaps.md` should list missing domains or under-researched questions,
especially around HCI, ZUI, visualization uncertainty, and collaborative
sensemaking.

## Done When

The atlas helps choose design primitives without collapsing Softland into any
one reference product.

A strong output will say things like:

```text
Borrow source maps for reversible transformation debugging.
Borrow game minimaps for local orientation and way back.
Borrow ZUI for lawful compression, not for flashy zoom transitions.
Reject global graph as first screen.
Reject dimensionality reduction as truth unless calibrated as projection.
```
