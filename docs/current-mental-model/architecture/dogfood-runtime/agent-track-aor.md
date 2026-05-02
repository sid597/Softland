# LLM / Agent Track With AOR Pattern

Status: current architecture direction, 2026-05-01.

This note captures the LLM-agent side of the dogfood runtime:

```text
chat / AOR-style agents / Codex / Claude / streamed reasoning / patches
```

## Contract

```text
LLMDepot owns requests to think, answer, inspect, propose, or edit.
AgentRunPState owns the durable lifecycle of each run.
LLMObservationDepot owns streamed tokens, tool events, patches, and status.
LLMViewsPState owns live/readable projections for the UI.
```

The model/agent/CLI does the thinking or editing work. Rama owns the run.

## AOR Pattern To Borrow

Agent-o-Rama's useful shape:

```text
agent depot
    |
    v
topology creates agent/invoke/node state
    |
    v
module-owned async node executor
    |
    | streamChunk!
    v
agent streaming depot
    |
    v
streaming/root PStates
    |
    v
UI watches Rama state
```

The important part is the back-arrow:

```text
agent execution -> streaming depot -> PStates -> UI
```

The agent does not stream directly to the UI as the source of truth.

## Diagram

```text
                       LLM / AGENT TRACK
       chat / AOR agents / Codex / Claude / streamed patches


  Softland UI
      |
      | append LLM or agent request
      v
+-------------------------+
| LLMDepot                |
|                         |
| :chat-request           |
| :agent-invoke           |
| :codex-run              |
| :claude-run             |
| :tool-call-request      |
| :cancel-request         |
+------------+------------+
             |
             | Rama topology consumes request
             v
+--------------------------------+
| LLMTopology / AgentTopology    |
|                                |
| validates request              |
| checks refs/context            |
| creates agent-run id           |
| creates node/invoke state      |
| records accepted/rejected      |
+------------+-------------------+
             |
             | updates
             v
+--------------------------------+
| AgentRunPState                 |
|                                |
| run-id                         |
| request-id                     |
| agent kind                     |
| target refs                    |
| prompt/context refs            |
| status                         |
| current node/tool              |
| streaming cursor               |
| produced refs                  |
+------------+-------------------+
             |
             | launched by
             v
+--------------------------------+
| AgentExecutor                  |
|                                |
| AOR-style module-owned async   |
| execution                      |
|                                |
| implementations:               |
|   LLM chat call                |
|   AOR agent node               |
|   codex CLI                    |
|   claude CLI                   |
|   tool call                    |
|                                |
| may use subscription auth on   |
| trusted machines               |
+------------+-------------------+
             |
             | token / tool / patch / status stream
             v
+--------------------------------+
| LLMObservationDepot            |
|                                |
| :run-started                   |
| :token                         |
| :reasoning-chunk               |
| :tool-call-started             |
| :tool-call-output              |
| :patch-proposed                |
| :file-mentioned                |
| :heartbeat                     |
| :run-failed                    |
| :run-succeeded                 |
+------------+-------------------+
             |
             | Rama materializes
             v
+--------------------------------+
| LLMViewsPState                 |
|                                |
| live stream by run             |
| messages by conversation       |
| agent trace by run             |
| patches by run                 |
| refs mentioned by run          |
| produced artifacts             |
+------------+-------------------+
             |
             | query / subscription
             v
  Softland UI
```

## Request Shape

Sketch only. Exact names can change during implementation.

```clojure
{:request/id "llm_req_1"
 :request/type :agent/codex-run
 :request/time-ms 1710000000000
 :routing/key [:agent/conversation "conv_1"]

 :actor {:actor/id "sid"
         :actor/type :human}

 :target {:target/kind :world
          :target/id "softland"}

 :action {:action/type :agent/codex-run
          :action/capability :agent/run
          :action/params {:prompt "inspect this build failure"
                          :working-dir "/mnt/data/projects/Softland"}}

 :payload {:agent/kind :codex
           :context/refs [{:ref/kind :compute-run
                           :ref/id "compute_run_1"}]
           :auth/profile :local-chatgpt-session}}
```

## Observation Shape

```clojure
{:observation/id "llm_obs_1"
 :run/id "agent_run_1"
 :request/id "llm_req_1"
 :observation/type :token
 :time-ms 1710000000001
 :sequence 128
 :payload {:text "The failing test points to ..."}}
```

Common observation types:

```text
:run-started
:token
:reasoning-chunk
:tool-call-started
:tool-call-output
:stdout
:stderr
:patch-proposed
:file-mentioned
:heartbeat
:run-failed
:run-succeeded
:cancelled
```

## Codex / Claude Subscription Auth

Codex and Claude can be run as CLIs on trusted machines using local subscription
auth/session state.

The architecture does not require API-pricing-only execution.

But the boundary is:

```text
auth cache / local session / secrets
  live on the trusted runner machine
  are referenced by capability/profile
  are not stored in Rama
```

Rama stores:

```text
which capability/profile was used
which actor requested the run
which refs were given as context
what streamed back
what patches or artifacts were proposed
what decision was later made
```

## World Boundary

Agent output does not automatically become world truth.

```text
Codex proposes patch
  -> LLMObservationDepot
  -> LLMViewsPState
  -> optional WorldDepot request:
       "apply this patch"
       "create this note"
       "record this decision"
       "accept this artifact"
```

World mutation still goes through:

```text
WorldDepot -> WorldTopology -> ActionDecision -> KernelEvent? -> WorldPStates
```

## Rama-Integrated Meaning

For this track, "Rama-integrated" means:

```text
Rama owns:
  request
  decision
  run id
  node/tool state
  streaming cursor
  observations
  produced refs
  projections

The executor owns:
  calling the model
  running the CLI
  using local auth
  handling process IO
  turning output into observations
```

