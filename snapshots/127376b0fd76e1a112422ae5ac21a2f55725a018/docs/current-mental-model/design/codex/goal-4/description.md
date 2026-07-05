# Goal 4 - Truth-State Visual Grammar

Status: initial session brief, 2026-06-09.

## Goal

Design the first visual grammar for truth, uncertainty, provenance, derivation,
and relation in Softland views.

This is the real beginning of the design system. Not colors first. Not buttons
first. Truth state first.

## Why This Goal Exists

Softland can become dangerous if a clean view makes uncertain or inferred
material look accepted. The UI must make the epistemic status of material
visible without requiring the user to inspect raw metadata.

The map must not lie.

## Inputs To Read

Minimum:

```text
docs/current-mental-model/design/codex/orientation.md
docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md
docs/current-mental-model/architecture/object-container-spec.md
docs/current-mental-model/architecture/object-container-ingester-contract.md
```

Recommended if available:

```text
docs/current-mental-model/design/codex/goal-1/charter.md
docs/current-mental-model/design/codex/goal-2/current-substrate-map.md
docs/current-mental-model/design/codex/goal-3/precedent-atlas.md
```

## Research And Exploration Questions

Answer:

```text
1. What are the minimum truth states Softland must show?
2. Which states are source states, object states, relation states, projection
   states, and action states?
3. Which states must be visible at overview scale?
4. Which states can be folded but must stay recoverable?
5. Which encodings should be redundant beyond color?
6. How does a user see the difference between accepted, candidate, inferred,
   stale, partial, failed, disconnected, and unanchored?
7. How does the grammar survive outline, graph, timeline, inspector, canvas,
   and semantic zoom?
```

## Required Output Files

Create these files in this folder:

```text
truth-state-grammar.md
visual-encoding-table.md
interaction-state-table.md
anti-lie-rules.md
```

## Required Format

`truth-state-grammar.md` should include:

```text
Status
Origin prompt
Core claim
State families
Visual principles
Encoding rules
Cross-view consistency rules
Failure modes
Open questions
Next recommended goal
```

`visual-encoding-table.md` should include one row per state:

```text
State
Meaning
Where it comes from in the model
Must be visible at overview? yes/no
Primary encoding
Secondary encoding
Interaction behavior
Tooltip/inspector language
What this must not imply
```

At minimum cover:

```text
raw source
SourceArtifact
native ObjectContainer
DerivedUnit
graduated object
Revision
SourceAnchor
accepted CompositionEdge
accepted RelationEdge
candidate relation
inferred relation
rejected relation
partial parse
parse failure
stale anchor
unanchored object
disconnected island
projection
local-world placement
view state
AI inference
human-authored fact
simulation output
```

`anti-lie-rules.md` should define hard prohibitions:

```text
Do not show inferred relation as accepted.
Do not hide missing anchors.
Do not collapse source-specific projection into common truth.
Do not show dimensional-reduction proximity as semantic relation without a
projection label.
Do not let color be the only truth-state encoding.
```

## Done When

A future prototype can reuse the grammar across Source World, Native World,
Trust World, topology, and ZUI without inventing state styling ad hoc.
