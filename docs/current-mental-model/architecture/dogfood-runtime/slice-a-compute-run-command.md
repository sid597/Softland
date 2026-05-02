# Slice A — :compute/run-command  (Architecture)

Status: locked contract, 2026-05-02. Post Codex 3-round gate.

First vertical slice of the dogfood runtime. Hello/false proof for the compute
track. Cancel + restart-reconcile deferred to slice A2.

Implementation target: `src/app/server/rama/dogfood/compute.clj` (sibling to
`core.clj`, which remains the V0/V1 text-instance proof).

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
    │  renders status,       │        │  tick:                  │
    │  stdout-tail,          │        │   read pending queue    │
    │  exit-code             │        │   append claim          │
    │                        │        │   await grant (3-state) │
    │                        │        │   spawn child process   │
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
  │  write three PStates on this task partition:                  │
  │     $$compute-runs[rid]               :status :pending        │
  │     $$compute-decisions-by-run-id[rid] full decision shape    │
  │     $$compute-pending-by-task          append rid             │
  └────────────────────────────────────────────────────────────────┘
                          ▼
  ┌─[ Step 3 ]──────────── EXECUTOR — discover ───────────────────┐
  │  Each tick on task T:                                         │
  │     foreign-proxy/select                                      │
  │       $$compute-pending-by-task                               │
  │       {:pkey task-id}    →   vector of pending rids           │
  └────────────────────────────────────────────────────────────────┘
                          ▼
  ┌─[ Step 4 ]──────────── EXECUTOR — claim ──────────────────────┐
  │  For each pending rid not yet in local registry:              │
  │     generate claim-token (uuid)                               │
  │     foreign-append! *compute-claim-depot                      │
  │       {:run/id, :executor/id, :claim-token}                   │
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
  │     NOT-YET-PROCESSED  → keep awaiting next tick              │
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
