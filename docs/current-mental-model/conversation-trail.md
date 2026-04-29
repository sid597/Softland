# Conversation Trail

Status: context note, 2026-04-28.

This note explains how we arrived at `rama-world-kernel-text-instance.md`. It is not a transcript. It preserves the conceptual path so a new chat can understand why the current model has the shape it has.

## Starting Pressure

The user was trying to understand the object shape behind Softland-like work:

```text
code
chat history
PDFs
LLM replies
text editors
commitable artifacts
accepted/rejected fragments
branches of exploration
permissions over granular structure
Rama event sourcing
```

The core problem was not "how do we store text?" It was:

```text
How can raw work history become addressable, forkable, distillable, permissioned, and reinterpretable later?
```

## Chat As Failure Mode

The user identified that normal chat loses context:

```text
rich local world in the user's head
  -> flattened chat message
  -> detached LLM answer
  -> user manually remaps answer back into their world
```

This led to the requirement that LLM responses should not be detached text. They should attach to the current context/world:

```text
this restates node A
this extends thought B
this disagrees with hypothesis C
this creates branch D
this should become checkpoint E
```

## Commitable Artifacts

The user then sharpened the problem with a concrete UI pressure:

```text
If an assistant replies with 10 lines,
the UI must not force accept whole / reject whole.
```

This produced the distinction:

```text
Artifact = thing that arrived or was produced
Unit = addressable part or interpretation of an artifact
Annotation/status = branch-local judgment over a unit
Commit = selected units promoted into a world under an interpretation
```

Rejected material should not be deleted. It is data:

```text
rejected by user
hidden from canonical view
kept for provenance, learning, later debugging, and redistillation
```

## Category-Theory Pass

The model was then category-theorized using Ologs/CatColab-style thinking:

```text
Olog/schema/category = concepts and lawful arrows
World_b : O -> Set = branch-indexed instance/model of that schema
Events = generators of causal history
Distillers = versioned translations from raw artifacts to units/relations
Views = projections/lenses out of world state
Permissions = allowed observations, not merely row access
```

Important result:

```text
The identity of an object is not only its bytes.
It is the family of lawful ways it can be addressed, observed, transformed,
related, permissioned, projected, and redistilled.
```

## CatColab / Topos Interpretation

CatColab stood out as evidence for:

```text
model first
view/analysis second
migration between logics third
collaborative structured editing fourth
```

But the gap for this project is earlier than CatColab:

```text
CatColab starts once the thing is already formal.
This system must help raw chat/PDF/code/thought become progressively formal
without losing provenance.
```

## The First Bad Cut

A first implementation framing said:

```text
raw text artifact
  -> addressable text surface
  -> unitized interpretations
  -> branch-local accept/reject/promote statuses
  -> views/projections
  -> permissions over observations

That is the first Rama slice.
```

The user pushed back correctly: this collapsed system concerns into one "text slice" and made text look like the ontology.

## Revised Cut

The correction was to stop treating concerns as peer options or independent slices. Instead:

```text
Plane = system-level concern / contract
Instance = concrete carrier, e.g. text/chat/PDF/code
Module = implementation boundary
Walkthrough = one vertical path through all contracts
```

The planes are intertwined, but separable by contracts:

```text
truth/event plane
object/target plane
action plane
address plane
materialization plane
projection plane
policy plane
distillation plane
```

## Final Current Model

The model first clicked when expressed as a single loop, and was later corrected
to split request from accepted event:

```text
Projection shows materialized state
  -> user/agent acts on a projected target
  -> helper/API builds an ActionRequest
  -> ActionRequest enters a Rama depot
  -> Rama interpreter/policy derives accepted KernelEvent or rejected ActionDecision
  -> Rama ETL updates materialized state
  -> projection changes
```

The general kernel is the set of contracts inside this loop:

```text
action request
action decision
event / accepted fact
target
action
materialization
projection
policy
distillation
```

Text is the first instance used to test the kernel:

```text
text artifact
text revision
text range/anchor
text unitization
unit status update
canonical/discarded projections
```

## Latest Sharpening: Kernel Data Structure First

The user clarified that the first step is to work out the **general kernel data structure** before moving into text.

The current kernel event shape:

```text
KernelEvent =
  identity
  + type
  + actor
  + branch
  + context
  + target
  + action
  + payload
  + causality
  + ordering
  + policy
  + provenance
```

This answers seven core questions:

```text
What happened?       event/type
Who did it?          actor
In which world?      branch
From what context?   context
To what target?      target
By what action?      action
With what causality? causal
```

Text, PDF, chat, and code plug into `payload` and `target/address`. They should not change the kernel envelope.

## Important Communication Constraint

When continuing from this context, do not answer with detached abstractions.

Prefer:

```text
Here is the contract.
Here is the kernel data structure.
Here is how it wires to Rama.
Here is how text instantiates it.
Here is the next implementation boundary.
Here are the open choices and tradeoffs.
```

Avoid:

```text
word lists with no wiring
choosing a path without presenting the tree
collapsing conceptual planes into implementation slices
making text/editor the ontology
```
