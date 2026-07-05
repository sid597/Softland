# Goal 5 - Concrete Scenario Walkthroughs

Status: initial session brief, 2026-06-09.

## Goal

Write concrete scenario walkthroughs for the first two major examples:

```text
1. Softland importing and inspecting itself
2. Discourse Graph project import and cross-silo inspection
```

The goal is to make the abstract design research answer real use questions.

## Why This Goal Exists

Softland's view design must be judged against actual worlds, not generic sample
data. The user specifically wants two examples, and Softland itself is the
strongest stress test because it is history-heavy, vibe-coded, agent-mediated,
and currently evolving through object-container work.

## Inputs To Read

Minimum:

```text
docs/current-mental-model/design/codex/orientation.md
docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md
docs/current-mental-model/architecture/object-container-spec.md
```

Recommended if available:

```text
docs/current-mental-model/design/codex/goal-2/current-substrate-map.md
docs/current-mental-model/design/codex/goal-4/truth-state-grammar.md
```

For Softland scenario:

```text
docs/current-mental-model/README.md
docs/current-mental-model/context-map.md
docs/current-mental-model/00-start-here/where-is-what.md
docs/thread-map-claude.md
docs/vision/epistemic-framework.md
```

For Discourse Graph scenario, use known context from the user or ask for the
specific import material if the session needs exact files.

## Research And Exploration Questions

For each scenario, answer:

```text
1. What source material is imported?
2. What native objects should exist?
3. What source anchors should be inspectable?
4. What relations are accepted vs candidate?
5. What would the user want to ask first?
6. What does the view show at overview scale?
7. What does the user inspect locally?
8. What failure or uncertainty states must be visible?
9. What next action should be possible?
10. What private reconstruction does the view remove?
```

## Required Output Files

Create these files in this folder:

```text
scenario-softland-self-import.md
scenario-discourse-graph-import.md
cross-scenario-design-requirements.md
scenario-open-questions.md
```

## Required Format

Each scenario file should include:

```text
Status
Origin prompt
Scenario premise
Imported materials
User questions
Object model expectations
View sequence
Signature interactions
Trust moments
Pacing moments
Failure states
Actions emitted
What the scenario proves
What it does not prove
Open questions
Next recommended goal
```

The view sequence should be written as steps:

```text
1. User enters through source inventory.
2. User selects source artifact.
3. User sees native objects produced.
4. User selects object.
5. View highlights source anchor.
6. User inspects accepted/candidate relations.
7. User moves to neighboring object.
8. User acts or saves a local world.
```

## Done When

The scenarios are specific enough that Goal 6 can design a first
Source-to-World View without inventing fake user needs.

The Softland scenario should answer the user's target feeling:

```text
If this view can show me how we got here, it is chef's kiss.
```
