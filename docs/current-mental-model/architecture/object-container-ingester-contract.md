# Object-Container Ingester Contract

Status: corrective starting doc, created after confusion over transcript ingest.

Origin prompt:

> Why is it not like this: `.md file -> Markdown Ingester`, `chat transcript -> Transcript Ingester`, `code artifact -> Code/Artifact Ingester`, all feeding the Object Container Kernel? What is the common part that is not common right now and should be? Did Phase 1 fail to capture the work needed for this to work correctly?

## The Decision

The intended architecture is:

```text
.md file         -> Markdown Ingester      \
chat transcript -> Transcript Ingester      -> Object-Container Kernel
code artifact   -> Code/Artifact Ingester  /
```

Transcript ingest is not outside object-container. It is a domain-specific
object-container ingester.

The current implementation does not fully realize this because the transcript
ingest module owns a transcript-local object-container-shaped store instead of
writing shared base objects through a common object-container contract.

## The Core Mistake

The transcript product and implicit spec say the right thing: transcript ingest
is source acquisition plus transcript interpretation plus object-container
ingest.

The Phase 1 plan then allowed the implementation to become:

```text
transcript_ingest.clj
  owns $$containers-by-id -> TranscriptContainerRow
  owns $$source-artifacts -> transcript-local SourceArtifactRow
  owns $$source-anchors-by-container -> transcript-local SourceAnchorRow
  owns $$composition-edges-by-parent -> transcript-local CompositionEdgeRow
```

That is a useful source-specific proof, but it is not the shared kernel shape.

The missing Phase 1 gate was:

```text
Does this ingester write base identity-bearing material into the common
object-container substrate, or does it create a source-specific base object
island?
```

If the answer is "source-specific base object island", the plan must fail for
the unified object-container architecture.

## Common vs Source-Specific

The common object-container substrate owns base truth:

```text
SourceArtifact
ObjectContainer
Revision
DerivedUnit
SourceAnchor
CompositionEdge
accepted request / decision / event audit
```

Source-specific ingesters own acquisition and interpretation:

```text
read source
watch source
parse source format
redact source format
extract native ids
derive anonymous units
emit import material
```

Source-specific modules may also own read models and operational indexes:

```text
transcript conversation projection
transcript tool-call index
transcript ingest-run status
transcript file-offset state
code symbol index
roam uid lookup
canvas placement cache
```

Those read models are not the canonical object store. They are projections or
indexes over imported material.

## Required Import Shape

Every ingester must produce an import materialization shaped like:

```text
source-artifacts
  immutable captured source versions or source records

object-containers
  identity-bearing native objects when the source has stable ids

derived-units
  addressable source-derived spans when the source lacks stable native ids

revisions
  versioned content/state for object containers

source-anchors
  links from containers/units back to exact source positions or source-native ids

composition-edges
  source-declared containment, ordering, production, and parent/child structure

projection/index events
  optional source-specific read models, not base truth
```

The ingester can emit this shape directly into the object-container module, or
through a future `ImportEnvelope`/depot owned by the object-container kernel.
The essential constraint is ownership: base object rows are common, not
source-local.

## Native IDs vs Derived Units

Use the source's own identity when it exists:

```text
Roam page uid       -> ObjectContainer
Roam block uid      -> ObjectContainer
chat conversation   -> ObjectContainer
chat message uuid   -> ObjectContainer
tool-use id         -> ObjectContainer
canvas node id      -> ObjectContainer
code file path      -> ObjectContainer, under a repo/snapshot policy
code symbol id      -> ObjectContainer, when parser identity is stable
```

Use `DerivedUnit` when identity is derived by Softland rather than supplied by
the source:

```text
markdown paragraph
markdown heading
anonymous text span
parser-derived source span without stable native id
```

Derived units may graduate into object containers on first meaningful touch
or by an explicit import policy.

## Current Repo Mapping

Current markdown path:

```text
src/app/server/rama/object_container.clj
  actual object-container module
  handles markdown source ingest
  creates SourceArtifactRow
  creates document ObjectContainerRow
  creates DerivedUnitRow for markdown blocks
  creates SourceAnchorRow and CompositionEdgeRow
  graduates DerivedUnitRow into ObjectContainerRow on edit
```

Current transcript path:

```text
src/app/server/rama/dogfood/transcript.clj
  source acquisition/parser/redaction/JSONL reader

src/app/server/rama/dogfood/transcript_ingest.clj
  transcript-specific object-container-shaped module
  creates TranscriptContainerRow
  creates transcript-local SourceArtifactRow
  creates transcript-local SourceAnchorRow
  creates transcript-local CompositionEdgeRow
  creates conversation/tool/audit projections
```

This transcript path is product-correct as an ingester proof, but storage-wise
it is not yet the shared object-container kernel.

## Why Phase 1 Missed It

Phase 1 was written as "Transcript Ingest (Object-Container Ingester)", but it
also explicitly chose:

```text
src/app/server/rama/dogfood/transcript_ingest.clj
```

and then declared transcript-owned PStates:

```text
$$containers-by-id        {String TranscriptContainerRow}
$$source-artifacts        {String SourceArtifactRow}
$$source-anchors-by-container
$$composition-edges-by-parent
```

The validation checked Rama mechanics: typed PStates, subindexing, idempotent
`termval` writes, file offsets, audit rows, and retry behavior. It did not check
the architectural invariant:

```text
Base object truth must be common across ingesters.
```

So from a Rama-module-local perspective, Phase 1 was coherent. From the
intended object-container-kernel perspective, Phase 1 was missing the central
commonness requirement.

## Corrective Gate For Any New Plan

Any next plan for markdown, transcript, Roam, code, canvas, or Linear import
must answer these questions before topology/PState design:

```text
1. Which rows are common object-container base truth?
2. Which rows are source-specific projections or operational indexes?
3. Which module owns the common base PStates?
4. If this ingester creates containers, are they ObjectContainer rows in the
   common substrate, or source-local container-shaped rows?
5. If source-local rows exist, are they explicitly read models rather than
   canonical object identity?
6. How does the ingester emit native-id containers vs DerivedUnits?
7. How are SourceArtifacts versioned and anchored?
8. What source-specific read models are still required for product use?
```

Fail condition:

```text
The plan declares a source-specific $$containers-by-id as the only durable
identity store for imported material.
```

Acceptable condition:

```text
The plan writes common ObjectContainer/Revision/SourceArtifact/SourceAnchor/
CompositionEdge material, and separately writes source-specific projections or
indexes for fast reads.
```

## What To Salvage

Keep from transcript work:

```text
source acquisition modes: harvest and watch
byte-correct JSONL reader
redaction behavior
native transcript id extraction
conversation/message/tool-call/tool-result mapping
file offset handling
audit/run status product behavior
tests and fixtures
known F5/F6 findings as source-specific correctness problems
```

Do not treat the transcript-local base PStates as the final shared object store.

## Immediate Consequence

Do not continue transcript F5/F6 as if the only remaining question is local
bug fixing unless the explicit goal is to harden the source-specific prototype.

If the goal is the user's diagram, redo the transcript plan from this contract:

```text
source acquisition
  -> transcript interpreter
  -> common object-container import material
  -> source-specific conversation/tool/audit projections
```

The next implementation artifact should make the common import boundary explicit
before changing more transcript topology code.
