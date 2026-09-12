# Slice A — :compute/run-command  (Architecture)

Status: implemented Slice A.0, 2026-05-04. Contract originally locked
2026-05-02 after the Codex 3-round gate.

First vertical slice of the dogfood runtime. Hello/false proof for the compute
track. Cancel + restart-reconcile deferred to slice A2.

Implementation:

```text
src/app/server/rama/dogfood/compute.clj
test/app/server/rama/dogfood_compute_test.clj
```

`core.clj` remains the V0/V1 text-instance proof. Slice A.0 now uses a
module-owned `ComputeExecutorTaskGlobal` for normal execution. The
`run-one-pending-local!` helper remains as a manual/test path for the explicit
`"local"` inbox, not the normal executor architecture.

## Architectural Shape — 4-Tier Read/Write Asymmetry

The Rama design has a strict 4-tier read/write asymmetry that becomes the
cleanest mental model. Once you see it, every component falls into one of
four roles:

```text
                    WRITES ↓                     READS ↑
  DEPOTS         everyone (UI, executor)        topology only
  TOPOLOGY       n/a (it's code, not data)      depots
  PSTATES        topology only                  everyone (UI, executor)
  EXECUTOR       writes depots                  reads PStates
```

Depots are append-only mailboxes; PStates are the truth surface; topology is
the only thing allowed to translate between them; the executor only reads
PStates and only writes depots — never the reverse. The whole "back-arrow"
rule is just this asymmetry, applied recursively.

## View 1 — The parts (static)

```text
                   Slice A — :compute/run-command


    ┌──────────────────────────────────────────────────────┐
    │                       DEPOTS                         │
    │                  (append-only inputs)                │
    │                                                      │
    │     *compute-depot          ←  UI writes here        │
    │     *compute-claim-depot    ←  executor writes here  │
    │     *compute-obs-depot      ←  executor writes here  │
    │                                                      │
    │     all three:  (hash-by :run/id)                    │
    └──────────────────────────────┬───────────────────────┘
                                   │  consumed by
                                   ▼
    ┌──────────────────────────────────────────────────────┐
    │                      TOPOLOGY                        │
    │      (one <<sources block, three source branches;    │
    │       the ONLY writer of any PState)                 │
    │                                                      │
    │     REQUEST BRANCH    ← *compute-depot               │
    │     CLAIM BRANCH      ← *compute-claim-depot         │
    │     OBS BRANCH        ← *compute-obs-depot           │
    │                         {:retry-mode :all-after}     │
    └──────────────────────────────┬───────────────────────┘
                                   │  writes
                                   ▼
    ┌──────────────────────────────────────────────────────┐
    │                       PSTATES                        │
    │              (read-only outside topology)            │
    │                                                      │
    │     $$compute-runs                hash-by :run/id    │
    │       lifecycle truth                                │
    │                                                      │
    │     $$compute-decisions-by-run-id hash-by :run/id    │
    │       full V0/V1 ActionDecision                      │
    │                                                      │
    │     $$compute-pending-by-task     task-id-           │
    │       per-task inbox of pending   partitioner        │
    │       run-ids                     ⚠ different!       │
    │                                                      │
    │     $$compute-views               hash-by :run/id    │
    │       UI projection (bounded)                        │
    └──────────────────────────────┬───────────────────────┘
                                   │  read by
                  ┌────────────────┴────────────────┐
                  ▼                                 ▼
    ┌────────────────────────┐        ┌─────────────────────────┐
    │       UI (Electric)    │        │  EXECUTOR (TaskGlobal)  │
    │                        │        │                         │
    │  e/watch $$compute-    │        │  prepareForTask:        │
    │    views[run-id]       │        │    capture task-id      │
    │                        │        │                         │
    │  renders status,       │        │  reconcile loop:        │
    │  stdout-tail,          │        │   read task inbox       │
    │  exit-code             │        │   append task claim     │
    │                        │        │   await grant (3-state) │
    │                        │        │   submit worker job     │
    │                        │        │   stream observations   │
    └────────────────────────┘        └─────────────────────────┘
```

## View 2 — The flow (dynamic, 9 steps)

```text
  ┌─[ Step 1 ]────────────── UI ──────────────────────────────────┐
  │  Click "run echo hello".                                      │
  │  Electric mints run-id locally, builds the full kernel        │
  │  envelope, then:                                              │
  │     (foreign-append! *compute-depot request :ack)             │
  └────────────────────────────────────────────────────────────────┘
                          ▼
  ┌─[ Step 2 ]──────────── REQUEST BRANCH ────────────────────────┐
  │  validate (compute-target-kinds includes :workspace)          │
  │  decide → ActionDecision                                      │
  │  assign executor/task-id for this Rama task and write:        │
  │     $$compute-runs[rid]               :status :pending        │
  │     $$compute-decisions-by-run-id[rid] full decision shape    │
  │     $$compute-pending-by-task[task-id] append rid             │
  └────────────────────────────────────────────────────────────────┘
                          ▼
  ┌─[ Step 3 ]──────────── EXECUTOR — discover ───────────────────┐
  │  On reconcile wake for task T:                                │
  │     foreign-proxy/select                                      │
  │       $$compute-pending-by-task                               │
  │       {:pkey task-id}    →   vector of pending rids           │
  └────────────────────────────────────────────────────────────────┘
                          ▼
  ┌─[ Step 4 ]──────────── EXECUTOR — claim ──────────────────────┐
  │  For each pending rid not yet in local registry:              │
  │     generate claim-token (uuid)                               │
  │     foreign-append! *compute-claim-depot                      │
  │       {:run/id, :executor/id, :executor/task-id, :claim-token} │
  │       :append-ack                                             │
  │     local-registry[rid] = {:awaiting-grant token}             │
  └────────────────────────────────────────────────────────────────┘
                          ▼
  ┌─[ Step 5 ]──────────── CLAIM BRANCH ──────────────────────────┐
  │  apply-claim-if-pending:                                      │
  │     if (:status row) == :pending                              │
  │        $$compute-runs[rid]: :pending → :launching             │
  │                              :claimed-by, :claim-token set    │
  │        $$compute-pending-by-task: REMOVE rid                  │
  │     else                                                      │
  │        no-op (someone else won, or run already advanced)      │
  └────────────────────────────────────────────────────────────────┘
                          ▼
  ┌─[ Step 6 ]──────────── EXECUTOR — 3-state await ──────────────┐
  │  read $$compute-runs[rid] via foreign-proxy/select.           │
  │                                                               │
  │     GRANTED-TO-US      → spawn (Step 7)                       │
  │       (:status :launching) ∧ (:claimed-by us)                 │
  │       ∧ (:claim-token ours)                                   │
  │                                                               │
  │     NOT-YET-PROCESSED  → keep awaiting next reconcile         │
  │       (nil? row) OR (:status :pending)                        │
  │                                                               │
  │     CONFLICT-OR-PAST   → drop from local registry             │
  │       any other shape                                         │
  └────────────────────────────────────────────────────────────────┘
                          ▼
  ┌─[ Step 7 ]──────────── EXECUTOR — spawn + stream ─────────────┐
  │  ProcessBuilder.start(argv, cwd, env-allow)                   │
  │  foreign-append! *compute-obs-depot :started (with pid)       │
  │                                                               │
  │  for each stdout/stderr line:                                 │
  │     foreign-append! obs with monotonic :sequence              │
  │                                                               │
  │  on process exit:                                             │
  │     foreign-append! :exit obs with :exit-code                 │
  └────────────────────────────────────────────────────────────────┘
                          ▼
  ┌─[ Step 8 ]──────────── OBS BRANCH ────────────────────────────┐
  │  source> *compute-obs-depot {:retry-mode :all-after}          │
  │  branch on (:observation/type *obs):                          │
  │     :started   → :status :running, :pid                       │
  │     :stdout    → fold-with-buffer                             │
  │     :stderr    → fold-with-buffer                             │
  │     :heartbeat → touch :heartbeat-at                          │
  │     :exit      → drain :obs-buffer in seq order, close run    │
  │                                                               │
  │  fold-with-buffer:                                            │
  │     if seq == :last-seq + 1                                   │
  │        apply, advance :last-seq, drain pending entries        │
  │     else                                                      │
  │        :obs-buffer[seq] = obs    (NEVER drop)                 │
  └────────────────────────────────────────────────────────────────┘
                          ▼
  ┌─[ Step 9 ]──────────── UI ────────────────────────────────────┐
  │  e/watch on $$compute-views[run-id]                           │
  │  Electric panel renders live:                                 │
  │     status badge, pid, stdout tail, exit code                 │
  └────────────────────────────────────────────────────────────────┘
```

## How to read these together

```text
View 1 answers   "what pieces exist, and who can write/read each one?"
View 2 answers   "what actually happens from click to UI?"

Spot the back-arrow in View 2 by looking at the writers:
  Steps 1, 4, 7  →  UI/executor write DEPOTS
  Steps 2, 5, 8  →  topology writes PSTATES
  Steps 3, 6, 9  →  executor/UI READ pstates

Nothing outside topology ever writes a PState. Nothing inside topology
talks to a process. The asymmetry is the architecture.
```

## Executor Placement — Origin Question And Authoritative Answer

Origin question:

```text
The "local executor" is mysterious. Is it part of Rama? If not, why not? If it
is eventually always-running code, why is that good? Why not instantiate it
inside Rama instead of having some separate watcher? Where is the compute
happening?
```

Authoritative Rama/AOR answer from Rama Slack, supplied by the user:

```text
High-latency work should happen out of band from topologies in a separate
thread. Results that need to become PState truth should be communicated back
through a foreign depot append.

Agent-o-Rama uses this shape for LLM calls:
  - a TaskGlobal with a virtual thread pool executes high-latency work
  - another TaskGlobal opens foreign depot clients for result appends
  - when out-of-band work finishes, it appends the result to a depot
  - the topology acts as a state machine tracking initiated work
  - separate retry/stall detection handles dead nodes or lost work
```

Mapping to Slice A:

```text
ComputeTopology
  = Rama hot path.
  = validate request, accept/reject, grant claim, fold observations, write
    PStates.
  = must not block on builds, shell commands, Codex, or human/LLM latency.

ComputeExecutor
  = out-of-band physical worker.
  = claims pending runs, waits for durable grant, spawns child process, reads
    stdout/stderr/exit, appends observations.
  = should be Rama-integrated, but not topology event code.

Observation depots
  = bridge from out-of-band physical work back into Rama truth.

PStates
  = official materialized state after topology validates/folds depot records.
```

Implemented A.0 executor status:

```text
ComputeExecutorTaskGlobal
  declared in compute-module as *compute-executor
  prepareForTask(task-id)
    - captures the Rama task id
    - opens foreign depot/PState clients through the cluster retriever
    - owns process-local per-task registry state
    - owns a daemon reconcile scheduler
    - owns a daemon worker pool for command processes

normal request path
  :compute/run-command enters *compute-depot
  request branch assigns :executor/task-id from the current Rama task
  topology writes $$compute-pending-by-task[executor-task-id] -> run-id
  that task's ComputeExecutorTaskGlobal reads its own inbox

reconcile loop
  read task-local pending inbox
  append claim with run-id + executor-id + executor/task-id + claim-token
  read $$compute-runs for durable grant
  if grant matches, submit command execution to worker pool
  command worker appends :started/:stdout/:stderr/:exit observations

manual/test path
  run-one-pending-local! still exists for explicit local protocol tests.
  It targets $$compute-pending-by-task["local"] unless given a task id.
  This is not the normal module-owned executor path.

ComputeTopology
  remains the state machine
  never waits for the command to finish
  remains the only writer of PStates
```

Fault-tolerance implication:

```text
Once topology initiates or exposes out-of-band work, the original depot append
can succeed without knowing whether the work will finish. Therefore later slices
need explicit status tracking, exceptions as observations, retry policy, and
stall/lost-run detection. This is deferred from A.0.
```

## View 3 — Loop / Back-Arrow Diagram

Use this view when the architecture feels too linear. The important shape is
not one forward pipeline, but repeated loops:

```text
actor appends depot
  -> topology writes PState truth
  -> actor reads PState truth back
```

Back-arrow meaning:

```text
A back-arrow to ComputeExecutor is not a function call from Rama.
It means the same module-owned executor reads a PState later, on its own
reconcile loop, after Rama has consumed the depot record and materialized new
state.
```

Full Slice A loop:

```text
                      SLICE A.0 TASKGLOBAL COMPUTE RUNTIME
                      loop view, with explicit back-arrows


                         ┌──────────────────────────────┐
                         │          SOFTLAND UI          │
                         │                              │
                         │ user asks: run command        │
                         └──────────────┬───────────────┘
                                        │
                                        │ append request
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                                  DEPOT                                       │
│                                                                              │
│                            *compute-depot                                    │
│                         :compute/run-command                                 │
└────────────────────────────────────┬─────────────────────────────────────────┘
                                     │
                                     │ Rama stream topology consumes
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                            COMPUTE TOPOLOGY                                  │
│                                                                              │
│  request branch                                                              │
│                                                                              │
│  validate request                                                            │
│  accept / reject                                                             │
│  create run state                                                            │
└──────────────┬──────────────────────┬──────────────────────┬────────────────┘
               │                      │                      │
               │ write                │ write                │ write
               ▼                      ▼                      ▼
┌──────────────────────┐   ┌──────────────────────┐   ┌──────────────────────┐
│ $$compute-decisions- │   │ $$compute-runs        │   │ $$compute-pending-   │
│ by-run-id            │   │                      │   │ by-task              │
│                      │   │ truth row             │   │ executor inbox       │
│ accepted/rejected    │   │                      │   │                      │
│ request decision     │   │ :status :pending      │   │ task-id -> run-id    │
└──────────────────────┘   └──────────┬───────────┘   └──────────┬───────────┘
                                       │                          │
                                       │ project                  │
                                       ▼                          │
                            ┌──────────────────────┐             │
                            │ $$compute-views      │             │
                            │                      │             │
                            │ UI projection        │             │
                            │ :status :pending     │             │
                            └──────────┬───────────┘             │
                                       │                         │
                                       │ back-arrow: UI reads     │ back-arrow:
                                       │ Rama view                │ executor reads
                                       ▼                         ▼
                            ┌──────────────────────┐   ┌──────────────────────┐
                            │      SOFTLAND UI      │   │  COMPUTE EXECUTOR    │
                            │                      │   │                      │
                            │ sees pending run      │   │ finds pending run    │
                            └──────────────────────┘   └──────────┬───────────┘
                                                                   │
                                                                   │ append claim
                                                                   ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                                  DEPOT                                       │
│                                                                              │
│                         *compute-claim-depot                                  │
│                                                                              │
│             run-id + executor-id + executor/task-id + claim-token             │
└────────────────────────────────────┬─────────────────────────────────────────┘
                                     │
                                     │ Rama stream topology consumes
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                            COMPUTE TOPOLOGY                                  │
│                                                                              │
│  claim branch                                                                │
│                                                                              │
│  check $$compute-runs[run-id]                                                 │
│  if status is :pending, grant first claim                                     │
└───────────────────┬──────────────────────────────┬──────────────────────────┘
                    │                              │
                    │ update                       │ remove
                    ▼                              ▼
        ┌──────────────────────┐        ┌──────────────────────┐
        │ $$compute-runs        │        │ $$compute-pending-   │
        │                      │        │ by-task              │
        │ :status :launching   │        │                      │
        │ :claimed-by executor │        │ remove run-id        │
        │ :claim-token token   │        │                      │
        └──────────┬───────────┘        └──────────────────────┘
                   │
                   │ project
                   ▼
        ┌──────────────────────┐
        │ $$compute-views      │
        │                      │
        │ :status :launching   │
        └──────────┬───────────┘
                   │
                   │ back-arrow:
                   │ executor reads durable grant
                   ▼
        ┌──────────────────────────────────────────────────────────────────────┐
        │                         COMPUTE EXECUTOR                             │
        │                                                                      │
        │  spawn only if $$compute-runs says:                                  │
        │                                                                      │
        │    :status      :launching                                           │
        │    :claimed-by  me                                                   │
        │    :claim-token mine                                                 │
        │                                                                      │
        │  If not granted, do not spawn.                                       │
        └──────────────────────────────┬───────────────────────────────────────┘
                                       │
                                       │ physical side effect
                                       ▼
        ┌──────────────────────────────────────────────────────────────────────┐
        │                         OS CHILD PROCESS                             │
        │                                                                      │
        │  echo / false / clj / npm / codex / build / test                     │
        │                                                                      │
        │  This is outside topology hot path.                                  │
        └──────────────────────────────┬───────────────────────────────────────┘
                                       │
                                       │ stdout / stderr / exit
                                       ▼
        ┌──────────────────────────────────────────────────────────────────────┐
        │                         COMPUTE EXECUTOR                             │
        │                                                                      │
        │  converts physical output into observations:                         │
        │                                                                      │
        │    :started                                                          │
        │    :stdout                                                           │
        │    :stderr                                                           │
        │    :exit                                                             │
        │                                                                      │
        │  every observation carries run-id + claim-token + sequence           │
        └──────────────────────────────┬───────────────────────────────────────┘
                                       │
                                       │ append observations
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                                  DEPOT                                       │
│                                                                              │
│                            *compute-obs-depot                                │
│                                                                              │
│             claim-tokened observations from physical execution               │
└────────────────────────────────────┬─────────────────────────────────────────┘
                                     │
                                     │ Rama stream topology consumes
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                            COMPUTE TOPOLOGY                                  │
│                                                                              │
│  observation branch                                                          │
│                                                                              │
│  validate claim-token                                                        │
│  validate sequence                                                           │
│  fold stdout/stderr/exit                                                     │
│  update official state                                                       │
└───────────────────┬──────────────────────────────┬──────────────────────────┘
                    │                              │
                    │ update truth                 │ update projection
                    ▼                              ▼
        ┌──────────────────────┐        ┌──────────────────────┐
        │ $$compute-runs        │        │ $$compute-views      │
        │                      │        │                      │
        │ :status :running     │        │ status               │
        │ stdout-tail          │        │ stdout-tail          │
        │ stderr-tail          │        │ stderr-tail          │
        │ exit-code            │        │ exit-code            │
        │ obs-buffer           │        │ observation-errors   │
        │ observation-errors   │        │                      │
        │                      │        │ no claim-token       │
        └──────────┬───────────┘        └──────────┬───────────┘
                   │                               │
                   │ back-arrow: executor may      │ back-arrow: UI watches
                   │ read final/updated run state  │ live view
                   ▼                               ▼
        ┌──────────────────────┐        ┌──────────────────────┐
        │  COMPUTE EXECUTOR    │        │     SOFTLAND UI      │
        │                      │        │                      │
        │ sees terminal state  │        │ sees logs + result   │
        └──────────────────────┘        └──────────────────────┘
```

Compressed loop, lane view:

Important: there is one `ComputeExecutor` lane in this view. In the implementation
that lane is a module-owned TaskGlobal instance per Rama task, not topology
event code. The executor is not created by the topology and the topology does
not call it. The same executor reads Rama state, appends depot records, then
reads Rama state again.

```text
                 UI                    RAMA                         COMPUTE EXECUTOR
                 │                     depots/topology/PStates       TaskGlobal per task
                 │                                                  │
1. request       │ append run request                               │
                 ├──────────────────▶ *compute-depot                │
                 │                    -> ComputeTopology             │
                 │                    -> $$compute-runs              │
                 │                    -> $$compute-pending-by-task   │
                 │                    -> $$compute-views             │
                 │                                                  │
                 │ ◀──── read/watch ── $$compute-views              │
                 │                                                  │
2. pending       │                    $$compute-pending-by-task      │
                 │                    ─────────────── read ───────▶ │
                 │                                                  │
3. claim         │                    *compute-claim-depot           │
                 │                    ◀──────── append claim ─────── │
                 │                    -> ComputeTopology             │
                 │                    -> $$compute-runs              │
                 │                       status: :launching          │
                 │                       claimed-by: executor        │
                 │                       executor/task-id: task-id   │
                 │                       claim-token: token          │
                 │                    -> remove pending              │
                 │                    -> $$compute-views             │
                 │                                                  │
                 │                    $$compute-runs                 │
                 │                    ───────── read grant ────────▶ │
                 │                                                  │
4. spawn         │                                                  │ if grant matches:
                 │                                                  │   spawn OS child process
                 │                                                  │   collect stdout/stderr/exit
                 │                                                  │
5. observe       │                    *compute-obs-depot             │
                 │                    ◀────── append observations ── │
                 │                    -> ComputeTopology             │
                 │                    -> validate claim-token + seq  │
                 │                    -> $$compute-runs              │
                 │                    -> $$compute-views             │
                 │                                                  │
                 │ ◀──── read/watch ── $$compute-views              │
                 │                                                  │
                 │                    $$compute-runs                 │
                 │                    ───── read final state ──────▶ │
```

The important back-arrows:

```text
UI -> depot -> topology -> PState -> UI

Executor -> claim depot -> topology -> run PState -> Executor

Executor -> obs depot -> topology -> view PState -> UI

Executor -> obs depot -> topology -> run PState -> Executor
```

The rule:

```text
Actors never write truth directly.
Actors ask/report through depots.
Topology writes truth.
Actors read truth back from PStates.
```
