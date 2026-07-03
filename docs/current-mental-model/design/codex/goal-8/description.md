# Goal 8 - Evaluation Protocol

Status: initial session brief, 2026-06-09.

## Goal

Define how Softland will judge whether a view works.

This goal turns taste, research, and prototypes into falsifiable acceptance
checks.

## Why This Goal Exists

Softland views can look impressive while failing the actual job. The evaluation
protocol should catch false orientation, false trust, hidden uncertainty,
overwhelm, and private reconstruction.

The question is not:

```text
Do users like the screen?
```

The question is:

```text
Can another mind enter the local world, understand what happened, verify what
is grounded, see what is uncertain, and act without rebuilding all context
privately?
```

## Inputs To Read

Minimum:

```text
docs/current-mental-model/design/codex/orientation.md
docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md
docs/vision/epistemic-framework.md
```

Recommended if available:

```text
docs/current-mental-model/design/codex/goal-1/view-laws.md
docs/current-mental-model/design/codex/goal-4/truth-state-grammar.md
docs/current-mental-model/design/codex/goal-5/scenario-softland-self-import.md
docs/current-mental-model/design/codex/goal-6/source-to-world-view-spec.md
docs/current-mental-model/design/codex/goal-7/semantic-zoom-model.md
```

## Research And Exploration Questions

Answer:

```text
1. What does successful orientation mean?
2. What does successful trust inspection mean?
3. What does successful source-to-native reversibility mean?
4. What counts as a false-confidence incident?
5. What counts as private reconstruction?
6. How do we test pacing and overwhelm?
7. How do we test ZUI without building the whole map?
8. What are the acceptance gates for a prototype?
9. What are the anti-patterns that should fail a design review?
```

## Required Output Files

Create these files in this folder:

```text
evaluation-protocol.md
task-script.md
scoring-rubric.md
failure-mode-catalog.md
ship-gates.md
```

## Required Format

`evaluation-protocol.md` should include:

```text
Status
Origin prompt
Evaluation thesis
What is being tested
What is not being tested
Core tasks
Measures
Observation notes
Pass/fail gates
Open questions
Next recommended goal
```

`task-script.md` should contain test tasks like:

```text
Find where this object came from.
Explain what became native after import.
Identify which relation is accepted and which is candidate.
Find a stale or missing source anchor.
Move from a native object to exact raw source and back.
Explain why two silos are connected.
Find what the system does not know.
Continue work from the view without reading the full transcript.
Use zoom to move from project overview to source line without losing identity.
```

`scoring-rubric.md` should score:

```text
orientation
trust
source reversibility
truth-state clarity
relation clarity
pacing
recoverable compression
actionability
ZUI continuity
private reconstruction reduction
```

`failure-mode-catalog.md` should include:

```text
beautiful but false
global graph fog
AI summary overconfidence
hidden missing source
candidate shown as truth
projection mistaken for model
zoom loses identity
canvas sprawl
dashboard deadness
metadata without grounding
overwhelming completeness
```

## Done When

Future view prototypes can be reviewed against explicit gates instead of taste
alone.

The protocol should preserve the user's feelings-first evaluation style while
giving those feelings instruments:

```text
confusion
trust
overwhelm
curiosity
relief
desire to continue
ability to act
```
