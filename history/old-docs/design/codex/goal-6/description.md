# Goal 6 - Source-to-World View Spec

Status: initial session brief, 2026-06-09.

## Goal

Produce the first buildable product spec for the Scoped Source-to-World View.

This is the point where research becomes a concrete view contract.

## Why This Goal Exists

The first view should prove that imported material becomes inhabitable without
losing source, identity, relation, uncertainty, or actionability.

Do not start with a global graph. Start with a scoped import and one selected
object.

## Inputs To Read

Minimum:

```text
docs/current-mental-model/design/codex/orientation.md
docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md
docs/current-mental-model/architecture/object-container-spec.md
docs/current-mental-model/architecture/object-container-ingester-contract.md
```

Strongly recommended before starting:

```text
docs/current-mental-model/design/codex/goal-2/current-substrate-map.md
docs/current-mental-model/design/codex/goal-4/truth-state-grammar.md
docs/current-mental-model/design/codex/goal-5/scenario-softland-self-import.md
docs/current-mental-model/design/codex/goal-5/scenario-discourse-graph-import.md
```

If those files do not exist, either produce a smaller provisional spec or first
run the missing goal.

## Research And Exploration Questions

Answer:

```text
1. What is the first screen?
2. What are the pane jobs?
3. What is selected by default?
4. How does source-to-native highlighting work?
5. How are accepted, candidate, partial, stale, and failed states shown?
6. What is the local topology depth?
7. What can the user do from the view?
8. What ActionRequests can be emitted?
9. What data queries are required?
10. What must be explicitly out of scope?
```

## Required Output Files

Create these files in this folder:

```text
source-to-world-view-spec.md
wireflow.md
data-contract.md
interaction-contract.md
prototype-acceptance.md
out-of-scope.md
```

## Required Format

`source-to-world-view-spec.md` should include:

```text
Status
Origin prompt
Product name
Engineering name
Core job
Primary user questions
Layout model
Pane responsibilities
Truth-state requirements
Object selection behavior
Source anchor behavior
Relation inspection behavior
Warnings and failure states
Actions
Empty/loading/error states
Out of scope
Open questions
Next recommended goal
```

Use the pane jobs:

```text
Source World:
  where did this material come from?

Native World:
  what did it become in this scope?

Trust World:
  why should I trust this object or relation?
```

`wireflow.md` should be textual, not a decorative mock:

```text
State
What is visible
What is selected
What the user can do
What changes after action
What data is read
What ActionRequest is emitted, if any
```

`data-contract.md` should list exact projection needs:

```text
source inventory projection
native object projection
object detail projection
object relations projection
source anchor lookup
source span lookup
warning/partial-state projection
local topology projection
```

`prototype-acceptance.md` should define pass/fail checks.

## Done When

An engineer can implement a first non-global Source-to-World prototype without
guessing the product behavior.

The spec must make it impossible to confuse:

```text
source with native object
derived with accepted
candidate with truth
projection with canonical model
local topology with global world map
```
