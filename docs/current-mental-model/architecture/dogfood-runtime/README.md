# Dogfood Runtime Direction

Status: current architecture direction, 2026-05-01.

This folder captures the current direction that emerged from the Rama/AOR
discussion:

```text
Softland should dogfood itself through Rama.

The first concrete runtime has two execution tracks:

1. Compute track
   build / test / run / deploy / serve / command execution

2. LLM-agent track
   chat / AOR-style agents / Codex / Claude / streamed reasoning / patches

The current system shape has three conceptual depot families:

1. World
2. Compute
3. LLM-agent
```

This is not an implementation handoff. It is the current architectural shape to
think with before choosing exact files to change.

## Origin Pressure

The direction came from these user pressures:

```text
"store the whole softland code in rama and then use that to build and serve the
whole softland ... dogfooding the dogfooding"

"can we access the underlying terminal or smth say for codex or claude headless
commands via rama?"

"what i see very much missing ... is that there is now arrow that goes back to
the system ... to see into whats going on with some tasks we would need the
dashboard equivalent but in the archi diagrams i don't see how this goes to rama"

"i only want to now focus on the actual rama and AOR resource based on what we
want to implement in the compute and the agent side of depots now we have 2
definitive tracks"
```

## Read Order

```text
1. compute-track.md
2. agent-track-aor.md
3. three-depot-current-system.md
4. slice-a-compute-run-command.md
```

`slice-a-compute-run-command.md` is the current canonical candidate for the
first vertical slice. It captures the post-review compute spine: UI/executor
append depots, one topology owns PState writes, executor claims through Rama
before spawning, observations stream back through Rama, and UI reads Rama
PStates. It is architecture context, not an automatic implementation handoff.

## Settled For Now

```text
WorldDepot is truth.
ComputeDepot does physical execution.
LLMDepot does epistemic / agent execution.

Compute and LLM may produce observations, proposals, patches, artifacts, and
requests.

Compute and LLM do not secretly mutate WorldPStates.

The UI watches Rama.
Workers and agents stream back into Rama.
Workers and agents do not stream directly to the UI.
```

## Core Loop

```text
Softland UI
    |
    | request
    v
Rama depot
    |
    | topology accepts/rejects and creates run state
    v
Rama-owned run PState
    |
    | claimed/launched by executor
    v
executor / agent / child process
    |
    | streaming observations
    v
Rama observation depot
    |
    | materialized by topology
    v
Rama live-view PStates
    |
    | query / subscription
    v
Softland UI
```

## Rama / AOR Lesson

AOR is the closest available pattern for the agent track:

```text
agent depot
  -> topology creates invoke/node state
  -> module-owned async node executor runs work
  -> stream chunks append back into streaming depot
  -> topology materializes root/streaming PStates
  -> UI watches Rama state
```

Softland should borrow that shape:

```text
LLMDepot
  -> AgentTopology
  -> AgentRunPState
  -> AgentExecutor
  -> LLMObservationDepot
  -> LLMViewsPState
  -> Softland UI
```

For build/test/deploy, the same back-arrow rule applies, but the execution body
is dirtier and should be treated more like a runner:

```text
ComputeDepot
  -> ComputeTopology
  -> ComputeRunPState
  -> ComputeExecutor
  -> ComputeObservationDepot
  -> ComputeViewsPState
  -> Softland UI
```

## Language

Use "module-owned async execution" when precision matters.

```text
topology compute
  = small Rama event work on task threads

module-owned async execution
  = Rama module owns lifecycle/state, executor does long work, observations
    come back through depots

foreign/external execution
  = outside process initiates or polls as a Rama client
```

For this direction, "Rama-integrated" means:

```text
Rama owns request identity, acceptance, run state, observations, status,
provenance, and projections.

The physical execution may still be a child process, CLI, or runner.
```
