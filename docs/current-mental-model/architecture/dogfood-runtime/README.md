# Dogfood Runtime Direction

Status: current architecture direction, updated 2026-05-11.

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
5. llm-track-v2.md
6. llm-track-canonical.md
7. llm-track-derived-contract.md
8. llm-track-slice-roadmap.md
9. llm-track-claude-research.md
10. transcript-capture.md
```

`slice-a-compute-run-command.md` is the implemented Slice A.0 spine for the
first vertical slice. It captures the post-review compute shape: UI/executor
append depots, one topology owns PState writes, `ComputeExecutorTaskGlobal`
claims through Rama before spawning work, observations stream back through Rama,
and UI reads Rama PStates. It is architecture context, not an automatic
implementation handoff.

`llm-track-slice-roadmap.md` is now both the post-review roadmap and the
implementation record for the first LLM contract MVP. It points at the code and
test files, the slice commits, the verification counts, and the exact cost
rollup simplification tradeoff that was documented inline after implementation.

`llm-track-claude-research.md` and `transcript-capture.md` are adjacent
architecture notes, not part of the completed Codex-backed LLM MVP. They capture
Claude executor surface research and a first passive transcript-capture slice.

## Implementation State

The dogfood runtime now has two implemented Rama-backed vertical spines:

```text
Compute Slice A.0
  src/app/server/rama/dogfood/compute.clj
  test/app/server/rama/dogfood_compute_test.clj

LLM contract MVP
  src/app/server/rama/dogfood/world.clj
  src/app/server/rama/dogfood/llm.clj
  test/app/server/rama/dogfood_world_test.clj
  test/app/server/rama/dogfood_llm_test.clj
```

The LLM MVP is intentionally contract-first, not UI-complete. It proves the
World -> ContextBundle -> LLM run -> observations -> World/catalog/projection
loop in Rama with fake-executor coverage. Real UI workflows, reverse-MCP/custom
tools, full discourse graph projection, model-comparison lanes, and semantic
zoom integration remain outside this MVP.

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
  -> ComputeExecutorTaskGlobal
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
