# Compute Track

Status: current architecture direction, 2026-05-01.

This note captures the compute side of the dogfood runtime:

```text
build / test / run / deploy / serve / command execution
```

## Contract

```text
ComputeDepot owns requests to perform physical execution.
ComputeRunPState owns the durable lifecycle of each run.
ComputeObservationDepot owns streamed observations from the executor.
ComputeViewsPState owns live/readable projections for the UI.
```

The executor does the physical work. Rama owns the truth of the run.

## Diagram

```text
                         COMPUTE TRACK
          build / test / run / deploy / command execution


  Softland UI
      |
      | append compute request
      v
+-----------------------+
| ComputeDepot          |
|                       |
| :build-request        |
| :test-request         |
| :run-command          |
| :serve-request        |
| :deploy-request       |
| :cancel-request       |
+-----------+-----------+
            |
            | Rama topology consumes request
            v
+-------------------------------+
| ComputeTopology               |
|                               |
| validates request             |
| checks target world refs      |
| assigns run id                |
| records accepted/rejected     |
| creates runnable job state    |
+-----------+-------------------+
            |
            | updates
            v
+-------------------------------+
| ComputeRunPState              |
|                               |
| run-id                        |
| request-id                    |
| target refs                   |
| status                        |
| command/build/deploy plan     |
| claimed-by                    |
| started/ended                 |
| latest heartbeat              |
| log cursor                    |
| artifact refs                 |
+-----------+-------------------+
            |
            | claimed / launched by
            v
+-------------------------------+
| ComputeExecutor               |
|                               |
| module-owned async runner     |
| or trusted external runner    |
|                               |
| runs examples:                |
|   npm build                   |
|   clj test                    |
|   shell command               |
|   deploy script               |
|   server process              |
+-----------+-------------------+
            |
            | streaming observations
            | stdout / stderr / heartbeat / artifact / exit
            v
+-------------------------------+
| ComputeObservationDepot       |
|                               |
| :started                      |
| :stdout                       |
| :stderr                       |
| :heartbeat                    |
| :artifact-produced            |
| :server-started               |
| :failed                       |
| :succeeded                    |
| :cancelled                    |
+-----------+-------------------+
            |
            | Rama topology materializes
            v
+-------------------------------+
| ComputeViewsPState            |
|                               |
| live logs by run              |
| run status                    |
| artifacts by build            |
| builds by world snapshot      |
| deploys by artifact           |
| active servers                |
+-----------+-------------------+
            |
            | query / subscription
            v
  Softland UI
```

## Request Shape

Sketch only. Exact names can change during implementation.

```clojure
{:request/id "compute_req_1"
 :request/type :compute/build
 :request/time-ms 1710000000000
 :routing/key [:compute/project "softland"]

 :actor {:actor/id "sid"
         :actor/type :human}

 :target {:target/kind :world-snapshot
          :target/id "world_snap_1"}

 :action {:action/type :compute/build
          :action/capability :compute/run-build
          :action/params {:command ["npm" "run" "build"]
                          :cwd "/mnt/data/projects/Softland"}}

 :payload {:project/id "softland"
           :snapshot/ref "world_snap_1"
           :runner/profile :local-trusted}}
```

## Observation Shape

```clojure
{:observation/id "compute_obs_1"
 :run/id "compute_run_1"
 :request/id "compute_req_1"
 :observation/type :stdout
 :time-ms 1710000000001
 :sequence 42
 :payload {:text "compiled 83 files\n"}}
```

Common observation types:

```text
:started
:stdout
:stderr
:heartbeat
:artifact-produced
:server-started
:port-opened
:exit
:failed
:succeeded
:cancelled
```

## Why Not Just Run Inside The Topology?

Topology work should stay small:

```text
validate
route
accept/reject
materialize
enqueue/claim
```

Build/test/deploy can be slow, dirty, and dependency-heavy:

```text
filesystem access
package installs
compilers
secrets
ports
long logs
CPU/memory/disk pressure
untrusted or semi-trusted code
```

So the right shape is:

```text
topology creates durable run state
executor performs physical work
executor streams observations back to Rama
topology materializes live run views
```

## World Boundary

Compute results do not automatically become world truth.

```text
build succeeded
  -> ComputeObservationDepot
  -> ComputeViewsPState
  -> optional WorldDepot request:
       "record artifact as produced"
       "deploy this artifact"
       "mark version as served"
```

World mutation still goes through:

```text
WorldDepot -> WorldTopology -> ActionDecision -> KernelEvent? -> WorldPStates
```

