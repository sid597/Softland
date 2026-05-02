# Trail -- Slice A Cross-Model Experiment

Status: pre-Rama trace capture, 2026-05-02.

This is not a future Rama design and not an implementation handoff. It is the
manual trace we need now because the Rama-backed trail substrate does not exist
yet.

The purpose is to preserve how this understanding became buildable:

```text
conversation pressure
  -> architecture artifact
  -> cross-model review
  -> repaired artifact
  -> gate
  -> readable canonical shape
  -> implementation handoff
```

Without this kind of capture, future Softland can only see final docs and git
diffs. It cannot see the path that made the final shape true.

## Origin Pressure

The immediate user pressure:

```text
Read the current dogfood runtime direction.

Do not assume there is an implementation handoff.

We are continuing the Rama/AOR dogfood-runtime direction. The settled shape is:
WorldDepot is truth, ComputeDepot handles physical execution, LLMDepot handles
agent/LLM execution. Workers and agents stream observations back into Rama; the
UI reads Rama PStates.

First, help choose the first vertical implementation slice.
Use ASCII diagrams and concrete Rama contracts, not prose abstractions.
```

The later pressure that produced this note:

```text
I want structures like this now so that when Softland is in Rama we can trace:
at this point in that chat we discussed X, arrived at Y, experimented, built,
and got to Z.
```

## Starting Material

The Claude initializer did not start from nowhere. The docs it read had already
been shaped by earlier Codex work.

Relevant starting docs:

```text
docs/current-mental-model/context-map.md
docs/current-mental-model/README.md
docs/current-mental-model/architecture/dogfood-runtime/README.md
docs/current-mental-model/architecture/dogfood-runtime/compute-track.md
docs/current-mental-model/architecture/dogfood-runtime/agent-track-aor.md
docs/current-mental-model/architecture/dogfood-runtime/three-depot-current-system.md
```

So the real chain was:

```text
Codex-shaped docs
  -> Claude broad architecture initializer
  -> Codex contract review / gate
  -> Claude ingestion and redraw
  -> implementation handoff
```

## Artifact Sequence

```text
A0  v2.1 old shape
    Had the back-arrow insight but still allowed executor-spawn races by
    treating claim-before-spawn as later hardening.

A1  Claude/Sonnet first vertical proposal
    Named the right spine:
      World truth -> compute request -> run -> claim -> observations -> live view
    Strong at human-scale architecture.

A2  Claude 4.7 ask-codex-for-feedback prompt
    Best prompt-packaging artifact:
      MODE / ALTITUDE / AUTHORITY
      preserve settled direction
      check Rama pitfalls
      do not relitigate the whole system

A3  Codex contract review
    Found concrete failures:
      ack contract
      executor PState ownership / claim race
      scalar decision instead of full ActionDecision
      request envelope drift
      observation ordering overclaim

A4  Claude ingest / repaired artifact
    Accepted the real failures, preserved Slice A and the back-arrow, introduced
    claim depot and topology-owned claim transition.

A5  Codex gate review
    Found remaining holes:
      ephemeral claim result
      pending queue routing
      retry-mode not encoded in source form
      :workspace target validation drift

A6  Locked post-gate artifact
    Corrected but too dense to inhabit.

A7  User readability pressure
    "Can you redraw this, it is not very readable to me."

A8  Parts-flow artifact
    Split the architecture into:
      View 1 -- static parts and ownership
      View 2 -- dynamic 9-step flow
    This became the best canonical architecture artifact.

A9  Implementation handoff
    docs/current-mental-model/implementation-slice-a-compute-run-command-prompt.md
```

## Durable Decisions

```text
D1  First vertical is Slice A: :compute/run-command.

D2  The primitive is not a terminal feature. It is:
      compute-run-observation-spine

D3  UI and executor append depots only.

D4  The topology is the only PState writer.

D5  The topology never spawns processes.

D6  The executor never mutates PStates.

D7  The executor must claim through Rama before spawning.

D8  The durable claim grant surface is the run row, not a topology-local
    dataflow var.

D9  Observations carry run-id, claim-token, and sequence.

D10 UI reads Rama PStates only.

D11 LLM/agent runtime should mirror the compute spine later, not block Slice A.
```

## Technical Corrections Learned

```text
C1  Ack is an append-time caller contract, not a depot declaration option.

C2  :append-ack only proves append persistence/replication. If the caller needs
    materialized PState writes before return, use :ack or explicitly poll.

C3  PState writes belong to the topology that owns the PState.

C4  A TaskGlobal executor is outside the Clojure dataflow event boundary.

C5  A local spawn registry is only a per-process idempotency aid, not cluster
    authority.

C6  Hashing observations by :run/id gives locality but does not by itself solve
    retry ordering.

C7  Observation source must encode {:retry-mode :all-after} if the contract
    depends on all-after retry ordering.

C8  Gap handling must buffer or store by sequence; silently dropping older or
    out-of-order observations loses truth.
```

## Meta Discovery

The useful split was not "which model is smarter." It was a workflow:

```text
Claude initializes.
Codex falsifies and gates.
Claude ingests and redraws.
Codex implements.
```

The best artifact came after correctness and readability both had pressure:

```text
broad draw
  -> contract falsification
  -> minimal repair
  -> gate
  -> readability redraw
```

The redraw was not cosmetic. It made the contract public and inhabitable.

## Current Captured Outputs

Architecture:

```text
docs/current-mental-model/architecture/dogfood-runtime/slice-a-compute-run-command.md
```

Cross-model workflow note:

```text
docs/current-mental-model/architecture/cross-model-architecture-loop.md
```

Implementation handoff:

```text
docs/current-mental-model/implementation-slice-a-compute-run-command-prompt.md
```

Active session handoff:

```text
docs/sessions/next-prompt.md
```

## What To Append Later

When implementation starts, append only implementation-proven facts:

```text
commit refs
test names
test outputs
contract deltas discovered in code
decisions changed by implementation reality
open questions that survived code
```

Do not turn this trail into speculative architecture. It exists to preserve the
causal path from pressure to artifact to implementation proof.
