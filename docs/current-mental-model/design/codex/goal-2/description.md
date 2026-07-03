# Goal 2 - Current Substrate Map

Status: initial session brief, 2026-06-09.

## Goal

Map what the current Softland substrate can expose to the view layer.

This is the engineering grounding pass for design. The output should tell a
designer or future Codex session what data exists, what is queryable, what is
only planned, and what would be dangerous to imply in a view.

## Why This Goal Exists

Softland cannot design truthful views without knowing what the object-container
kernel and ingesters actually materialize.

The key distinction:

```text
common base truth
  SourceArtifact, ObjectContainer, Revision, DerivedUnit, SourceAnchor,
  CompositionEdge, accepted decisions/events

source-specific projections or indexes
  transcript conversation view, tool-call index, file offsets, run status,
  code symbol indexes, Roam uid lookup, etc.
```

The view must not present source-specific read models as canonical object truth.

## Inputs To Read

Minimum:

```text
docs/current-mental-model/design/codex/orientation.md
docs/current-mental-model/architecture/object-container-spec.md
docs/current-mental-model/architecture/object-container-ingester-contract.md
docs/current-mental-model/architecture/object-container-reviewer-world-model.md
src/app/server/rama/object_container.clj
test/app/server/rama/object_container_test.clj
```

Read only if needed:

```text
src/app/server/rama/object_container/markdown_adapter.clj
src/app/server/rama/object_container/transcript_adapter.clj
src/app/server/rama/object_container/runtime.clj
test/app/server/rama/dogfood/transcript_ingest_test.clj
docs/current-mental-model/build/object-container/IMPLEMENTATION_VALIDATION.md
docs/current-mental-model/build/object-container-common-infra/IMPLEMENTATION_VALIDATION.md
```

Never read:

```text
src/app/server/env.clj
```

## Research And Exploration Questions

Answer:

```text
1. Which imported source types are currently supported?
2. Which rows are common object-container base truth?
3. Which rows are projections, indexes, or operational state?
4. Which read functions already exist for views?
5. Which useful view questions cannot yet be answered?
6. Which states must the UI show as missing, partial, or future?
7. What should a first view avoid implying?
```

## Required Output Files

Create these files in this folder:

```text
current-substrate-map.md
import-material-matrix.md
view-data-contract.md
missing-view-queries.md
```

## Required Format

`current-substrate-map.md` should include:

```text
Status
Origin prompt
Files inspected
Implemented source paths
Common object-container rows
Source-specific rows
Existing projections/read models
Known architectural caveats
Open questions
Next recommended goal
```

`import-material-matrix.md` should be a table:

```text
Source type
SourceArtifact behavior
Native ObjectContainers
DerivedUnits
SourceAnchors
CompositionEdges
RelationEdges
Projections/indexes
Current status
View implication
```

Include at least:

```text
markdown
transcript
code
Roam/DG graph
Linear
Softland docs
```

`view-data-contract.md` should describe the minimum data a view needs for:

```text
source inventory
native object list
object trust inspector
local topology
source-to-native hover/highlight
warning and partial-state display
```

`missing-view-queries.md` should list:

```text
query
why the view needs it
whether it is implemented
where it should probably live
risk if the UI fakes it
```

## Done When

A future view spec can say exactly which current rows/projections it consumes,
which missing queries block implementation, and which product states must be
shown as unknown rather than silently invented.
