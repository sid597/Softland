# Three Depot Current System

Status: current architecture direction, 2026-05-01.

This note captures the current dogfood runtime shape:

```text
WorldDepot   = truth
ComputeDepot = physical execution
LLMDepot     = epistemic / agent execution
```

Conceptually there are three depot families. Physically, observation depots may
be split for streaming volume and topology clarity.

## Diagram

```text
                         TRUE CURRENT SYSTEM
                   world / compute / llm-agent


                         Softland UI
                              |
                              | append requests
                              v

+--------------------------------------------------------------------+
|                                RAMA                                |
|                                                                    |
|  +----------------+   +----------------+   +----------------+      |
|  | WorldDepot     |   | ComputeDepot   |   | LLMDepot       |      |
|  |                |   |                |   |                |      |
|  | :world-action  |   | :build-request |   | :chat-request  |      |
|  | :object-create |   | :test-request  |   | :agent-invoke  |      |
|  | :object-update |   | :run-command   |   | :codex-run     |      |
|  | :ref-create    |   | :serve-request |   | :claude-run    |      |
|  | :decision      |   | :deploy-request|   | :cancel-request|      |
|  +-------+--------+   +-------+--------+   +-------+--------+      |
|          |                    |                    |               |
|          v                    v                    v               |
|  +----------------+   +----------------+   +----------------+      |
|  | WorldTopology  |   | ComputeTopology|   | LLMTopology    |      |
|  |                |   |                |   | AgentTopology  |      |
|  | accept/reject  |   | accept/reject  |   | accept/reject  |      |
|  | materialize    |   | create run     |   | create run     |      |
|  | maintain refs  |   | track lifecycle|   | track lifecycle|      |
|  +-------+--------+   +-------+--------+   +-------+--------+      |
|          |                    |                    |               |
|          v                    v                    v               |
|  +----------------+   +----------------+   +----------------+      |
|  | WorldPStates   |   | ComputeRun     |   | AgentRun       |      |
|  |                |   | PState         |   | PState         |      |
|  | objects        |   |                |   |                |      |
|  | refs           |   | run status     |   | run status     |      |
|  | versions       |   | logs cursor    |   | token cursor   |      |
|  | decisions      |   | artifacts      |   | messages       |      |
|  +-------+--------+   +-------+--------+   +-------+--------+      |
|          |                    |                    |               |
|          |                    v                    v               |
|          |           +----------------+   +----------------+       |
|          |           | ComputeExecutor|   | AgentExecutor  |       |
|          |           |                |   |                |       |
|          |           | build/test/run |   | AOR/Codex/    |       |
|          |           | deploy/serve   |   | Claude/chat   |       |
|          |           +-------+--------+   +-------+--------+       |
|          |                    |                    |               |
|          |                    v                    v               |
|          |           +----------------+   +----------------+       |
|          |           | ComputeObsDepot|   | LLMObsDepot   |       |
|          |           |                |   |                |       |
|          |           | stdout/stderr  |   | token/tool/   |       |
|          |           | heartbeat      |   | patch/status  |       |
|          |           | artifact/exit  |   | result/error  |       |
|          |           +-------+--------+   +-------+--------+       |
|          |                    |                    |               |
|          +--------------------+---------+----------+               |
|                                       |                            |
|                                       v                            |
|                          +--------------------------+              |
|                          | UnifiedProjectionPStates |              |
|                          |                          |              |
|                          | world object view        |              |
|                          | live run views           |              |
|                          | refs across tracks       |              |
|                          | activity timeline        |              |
|                          | current dashboard        |              |
|                          +------------+-------------+              |
+---------------------------------------+----------------------------+
                                        |
                                        | query / subscription
                                        v
                                  Softland UI
```

## Boundaries

```text
WorldDepot
  owns the actual Softland world:
    objects
    refs
    versions
    decisions
    accepted facts

ComputeDepot
  owns physical execution:
    build
    test
    run command
    serve
    deploy
    artifacts
    logs

LLMDepot
  owns epistemic / agent execution:
    chat
    AOR-style agents
    Codex
    Claude
    streamed tokens
    patches
    tool traces
```

## Write Paths

LLM proposes a code/world change:

```text
LLMDepot
  -> AgentTopology
  -> AgentRunPState
  -> AgentExecutor
  -> LLMObservationDepot records streamed run + patch proposal
  -> LLMViewsPState
  -> WorldDepot receives "apply this?" ActionRequest
  -> WorldTopology accepts/rejects
  -> WorldPState changes only if accepted
```

Compute builds a snapshot:

```text
ComputeDepot
  -> ComputeTopology
  -> ComputeRunPState
  -> ComputeExecutor
  -> ComputeObservationDepot records logs + artifact
  -> ComputeViewsPState
  -> WorldDepot receives "record/deploy this?" ActionRequest
  -> WorldTopology accepts/rejects
  -> WorldPState changes only if accepted
```

World creates work for compute or LLM:

```text
WorldDepot
  -> WorldTopology accepts KernelEvent
  -> WorldPStates record new object/ref/decision
  -> derived request into ComputeDepot or LLMDepot when execution is needed
```

## Conceptual Three, Physical Maybe Five

Conceptual depot families:

```text
WorldDepot
ComputeDepot
LLMDepot
```

Likely physical depots for V0:

```text
WorldDepot
ComputeDepot
ComputeObservationDepot
LLMDepot
LLMObservationDepot
```

Reason:

```text
requests are lower-volume and decision-oriented
observations are streaming/high-volume and append-heavy
```

The conceptual model remains three tracks. The physical model can split
observation traffic when that helps Rama topology and PState design.

## First Implementation Target

Do not start by trying to store the entire codebase perfectly.

Start by making the two execution tracks observable through Rama:

```text
1. Append an LLM/agent request.
2. Launch a trusted local executor.
3. Stream observations back through LLMObservationDepot.
4. Materialize live run state.

5. Append a compute request.
6. Launch a trusted local executor.
7. Stream stdout/stderr/exit back through ComputeObservationDepot.
8. Materialize live run state.
```

Then add code/world object storage as the target of those runs:

```text
agent run mentions/proposes code refs
compute run builds/tests code refs
world accepts/rejects proposed changes
```

