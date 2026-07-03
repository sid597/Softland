# Codex Design Research Orientation

Status: initial scaffold, 2026-06-09.

Purpose: orient a new Codex session entering the Softland view/design research
track, then help it choose one focused goal instead of trying to solve the
whole UI/UX question in one pass.

This is not a task backlog. It is a research router.

## Core Frame

Softland is a place made of software. The view layer is where the design center
and engineering center touch:

```text
design center:
  a world for holding understanding in public form

engineering center:
  a programmable epistemic interface
```

The current near-term material is concrete:

```text
object-container kernel
markdown ingester
transcript ingester
ongoing code ingester
future Roam/DG/Linear/import paths
```

The design question is not:

```text
What should the app UI look like?
```

The design question is:

```text
What views let a finite mind safely enter transformed knowledge without losing
origin, identity, relation, uncertainty, pacing, or the ability to act?
```

## The View Track Thesis

Softland views are epistemic instruments.

They are not only pages, panes, dashboards, graphs, or canvases. A view is a
controlled coupling between a person and a larger structured reality. It must
make transformed material inhabitable without making it falsely certain.

The first important product anxiety is:

```text
Did this transformation preserve understanding,
or did it destroy context while making the result look organized?
```

The first family of views should answer:

```text
what came in
what became native
where it came from
how it is grounded
what connects
what is accepted
what is candidate
what is partial, stale, disconnected, failed, or unresolved
what can be acted on now
```

## Source Material To Read First

Read only what is needed for the selected goal. Do not perform a five-document
orientation dump unless the user asks for it.

Start here for this track:

```text
docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md
docs/current-mental-model/architecture/object-container-spec.md
docs/current-mental-model/architecture/object-container-ingester-contract.md
docs/current-mental-model/architecture/object-container-reviewer-world-model.md
docs/vision/epistemic-framework.md
```

Implementation substrate, only when the selected goal needs current code:

```text
src/app/server/rama/object_container.clj
src/app/server/rama/object_container/markdown_adapter.clj
src/app/server/rama/object_container/transcript_adapter.clj
src/app/server/rama/object_container/runtime.clj
test/app/server/rama/object_container_test.clj
test/app/server/rama/dogfood/transcript_ingest_test.clj
```

Project rule:

```text
Never read src/app/server/env.clj.
Do not commit markdown files.
```

## How To Choose A Goal

Pick one goal per session unless the user explicitly asks for synthesis across
goals.

Choose based on the user's opening prompt:

```text
Need orientation, boundaries, or "what is this design track?"
  -> goal-1

Need to know what the current kernel/ingesters can expose to views?
  -> goal-2

Need broad design/HCI/creative-tool precedents?
  -> goal-3

Need the visual language for truth, uncertainty, provenance, and relation?
  -> goal-4

Need concrete examples using Softland and Discourse Graph?
  -> goal-5

Need the first buildable Source-to-World View spec?
  -> goal-6

Need the ZUI / semantic zoom / Google Earth for knowledge angle?
  -> goal-7

Need to judge whether a view works?
  -> goal-8
```

## Suggested Dependency Shape

The goals are not a strict sequence, but this is the cleanest build-up:

```text
goal-1: charter
  -> goal-2: current substrate map
  -> goal-3: precedent atlas
  -> goal-4: truth-state visual grammar
  -> goal-7: ZUI and semantic zoom model
  -> goal-5: concrete scenario walkthroughs
  -> goal-6: first view spec
  -> goal-8: evaluation protocol
```

Goal 3 and Goal 7 can run in parallel. Goal 6 should not run before at least
one concrete scenario exists.

## Required Shape Of Every Output Artifact

Every produced document should include:

```text
Status
Origin prompt
Goal
Inputs read
Scope
Core claim
Findings or design decisions
Open questions
Next recommended goal
```

If a document makes a visual or interaction recommendation, it must also include:

```text
What this makes visible
What this keeps folded but recoverable
What this must not imply
Failure modes
Acceptance checks
```

## Research Standard

Do not gather references as decoration.

For each reference, ask:

```text
What problem does this solve?
What can Softland borrow?
What would be dangerous to borrow?
What concrete Softland primitive might it inspire?
What test would prove the borrowing worked?
```

Good references for this track include, but are not limited to:

```text
HCI and visualization:
  Shneiderman, Munzner, Heer, Bederson, Inselberg, treemaps, focus+context,
  semantic zoom, parallel coordinates, uncertainty visualization

ZUI and navigation:
  Pad++, Jazz, Space-Scale Diagrams, Prezi, Google Earth, GIS, game maps,
  minimaps, Blender, DAW timelines, video editors

Explorable explanations:
  Nicky Case, Distill, Loopy, Bret Victor-style direct manipulation

Malleable systems:
  Dynamicland, Ink and Switch, Folk Computer, WonderOS, Geoffrey Litt

Developer and lineage tools:
  source maps, DevTools, profilers, trace viewers, git history, dbt lineage,
  DataHub, Observable, notebooks

Creative professional tools:
  Figma, Lightroom, DaVinci Resolve, Ableton, game engines, architecture tools
```

## Non-Negotiable View Laws

Use these as starting laws until a later goal refines them:

```text
1. Transformation must be inspectable.
2. Identity must survive projection.
3. Compression must be recoverable.
4. Uncertainty must not masquerade as truth.
5. The world is entered locally.
6. Projection must not invent accepted relationships.
7. View gestures emit ActionRequests against the model.
8. The map must not lie.
9. Pacing is epistemic machinery, not polish.
10. Semantic zoom must preserve identity, provenance, and relation.
```

## Current Starting Bet

The first serious view is probably not the global knowledge map.

The first serious view is:

```text
Scoped Source-to-World Explorer
```

Epistemic pane jobs:

```text
Source World:
  where did this material come from?

Native World:
  what did it become in this scope?

Trust World:
  why should I trust this object or relation?
```

Signature interaction:

```text
select native object
  -> reveal exact source anchor
  -> show transformation path
  -> show accepted and candidate relations
  -> show warnings or gaps
  -> move to nearby object without losing local context
```

If that interaction works for Softland's own imported material, the view track
has found its first durable foothold.
