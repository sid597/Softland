---
name: rama-pitfalls
description: Use as a Rama failure-shield when designing, reviewing, or implementing Rama modules, depots, topologies, PStates, TaskGlobal/executor integrations, foreign clients, ack flows, partitioning, or dogfood-runtime Compute/LLM slices. Complements think-in-rama: that skill helps shape the design; this one falsifies the proposed design against recurring Rama foot-guns such as PState ownership, partitioner event boundaries, retry plus external side effects, ack-level confusion, ProxyState path issues, and Softland back-arrow violations.
allowed-tools: Read, Grep, Glob, Bash
---

# rama-pitfalls

Run this skill as an adversarial Rama design review. It is not here to invent the
architecture; it is here to catch the place where a plausible Rama-looking design
silently stops being Rama-correct.

## Relationship to think-in-rama

Use both skills together for serious Rama work:

```text
think-in-rama
  job:    design from Rama as the substrate
  asks:   what enters the world, what owns truth, what locality matters?
  output: recommended depot/PState/topology shape

rama-pitfalls
  job:    falsify a proposed shape before implementation
  asks:   where do retries, partitions, ownership, acks, and side effects break it?
  output: PASS/FAIL verdict with required fixes
```

Do not duplicate `think-in-rama` here. If the design is still vague, use
`think-in-rama` first. Use this skill once there is a concrete sketch, diff, or
module contract to attack.

## When to run

- **Before** proposing any Rama design (`defmodule`, `declare-depot`, `stream-topology`, `<<sources`, etc.).
- **Before** accepting a Rama design proposed by another agent.
- **Before** changing partitioning, hash-by extractors, or topology source structure.
- **Before** adding any external integration to a topology (process spawn, HTTP, file write, depot append to another module).
- **Whenever** the design includes the words "atomic," "transactional," or "exactly-once."

## Minimum inputs

Before reviewing, extract this inventory from the design or code:

```text
depots:
  *name -> partitioner/hash-by -> appended record shape

topologies:
  topology name -> stream/microbatch -> source blocks -> retry mode

PStates:
  $$name -> owning topology -> write paths -> readers

partitioners:
  source event -> |hash/|all/|global boundaries -> writes on each side

external actors:
  UI, worker, executor, agent, TaskGlobal, child process, HTTP service, file IO

foreign clients:
  foreign-append!, foreign-select, foreign-proxy, ack level, callback lifecycle
```

If you cannot fill this inventory, the design is not reviewable yet.

## The protocol

Walk through every section. For each, state explicitly:
- **PASS** with citation, OR
- **FAIL** with the specific failure mode, OR
- **N/A** with reason.

Do not skip sections by saying "obvious." The pitfalls are non-obvious — that is why they earned a skill.

---

### 1. EVENT BOUNDARY MAP

Draw the event boundaries in the proposed topology. Every `(|hash …)` / `(|all)` / `(|global)` / partitioner call is a hard line. PState writes on opposite sides are NOT visible together to readers.

For each PState write, label which event it lives in. Then for each pair of writes the design claims is "atomic," check they are in the same event (between the same partitioners).

Citation: `reference/rama/docs/23-acid-semantics.md` L41-52: *"A stream topology event is all code between partitioner calls."*

```
WRITES:
  $$pstate-A   in event 1
  $$pstate-B   in event 1   ← OK: atomic with $$pstate-A
  $$pstate-C   in event 2   ← NOT atomic with A or B
```

If the design needs cross-PState atomicity, redesign so all writes share a partitioner — same key value (one task one event) or `(|global)` (all writes on task 0).

---

### 2. SIDE-EFFECT RETRY TRACE

For every line in the topology that touches the world outside Rama (process spawn, HTTP call, file write, depot append to another module, depot append from `eachAsync`/`completable-future>`), write the retry timeline:

```
t=0   side effect fires (e.g. ProcessBuilder().start())
t=1   side effect persists (child PID, sent HTTP, written file)
t=2   topology event fails (exception, leader switch, timeout)
t=3   PState writes in this batch are DISCARDED
t=4   topology retries the depot record
t=5   side effect fires AGAIN
```

If t=5 produces a duplicate world effect (second child process, second HTTP POST, second file write) the design is **wrong**. `completable-future>` ties future *delivery* to event success. It does NOT make the world transactional.

Citation: `reference/rama/docs/11-stream-topologies.md` L156, L170: at-least-once retry; on event failure all batch PState writes are discarded but external side effects persist.

**Fix patterns** (back-arrow, AOR-style):
- Topology writes COMMITTED INTENT to a PState (e.g. `:status :pending`).
- External consumer (TaskGlobalObjectWithTick, separate process) reads committed intent.
- Consumer's `spawn-if-absent` registry dedupes within its own lifetime.
- Consumer's work is the side effect; observations stream BACK through an obs depot.

---

### 3. PSTATE OWNERSHIP

For every PState in the design, name its single owning topology. Writers from any other topology = design violation.

Citation: `reference/rama/docs/15-pstates.md` L41: *"a PState can only be updated by the ETL topology that owns it."*

```
$$compute-runs-by-id   owner: compute-runtime-topology   ✓
$$compute-log-by-run   owner: compute-runtime-topology   ✓
$$compute-decisions    owner: compute-runtime-topology   ✓
```

If two source flows need to write the same PState, they must live in the same topology as separate `(source> …)` blocks inside one `(<<sources …)`. NOT two topologies.

---

### 4. BACK-ARROW CHECK

The dogfood-runtime principle: **workers/agents stream observations BACK to Rama; Rama is truth; UI reads Rama.** Forward push from topology to executor inverts this and breaks retry safety.

For each non-Rama actor in the design (executor, agent CLI, browser), verify:
- It does NOT mutate Rama PStates directly. ✓
- The UI reads Rama state, not the actor's state. ✓
- The actor reads Rama as its source of truth (committed intent), not the topology calling it inline. ✓
- Observations from the actor flow into a depot; topology materializes; UI sees results. ✓

Citation: `history/old-docs/architecture/dogfood-runtime/README.md` L84-90.

---

### 5. RETRY IDEMPOTENCE OF GENERATED IDs

For every ID generated inside topology code (`(mint-id …)`, `(random-uuid)`, `(System/currentTimeMillis)`, etc.), verify the generation is deterministic from the source depot record.

If `(random-uuid)` runs inside topology code, every retry produces a different ID. Result: duplicate rows on different keys instead of one row overwritten.

Right pattern: derive from `:request/id` (UUID v5 with namespace), or have the appender mint the ID before `foreign-append!` and put it in the request envelope.

Citation: `reference/rama/docs/11-stream-topologies.md` L156: at-least-once means duplicate processing.

---

### 6. ACK LEVEL APPROPRIATENESS

For every `foreign-append!` in the design, name the ack level:

- `:ack` (default) — waits for colocated stream topologies to FINISH. Use for "submit and confirm processed" UI flows. Subject to `topology.stream.max.executing.per.task` throttle.
- `:append-ack` — waits only for replication. Use for high-volume appends (executor's stdout chunks, observation streams). Avoids throttle.
- `nil` (fire-and-forget) — no waiting. Use only when caller doesn't need confirmation.

A high-volume appender using `:ack` will hit throttle and throw. A confirmation-flow appender using `:append-ack` will succeed before the topology has materialized state, causing UI race.

Citation: `reference/rama/docs/14-depots.md` L198-218; `reference/rama/docs/11-stream-topologies.md` L292-299 (throttling).

---

### 7. STREAM vs MICROBATCH

State which topology type was chosen and why:

- **Stream**: few-ms latency, at-least-once retry, depot-append integration, no batch blocks. Use for live UI updates, low-latency reactivity.
- **Microbatch**: hundreds-of-ms latency, exactly-once, NO depot-append integration, batch blocks available (joins, two-phase aggregation, temporary PStates). Use for bulk recompute, global aggregation, exactly-once on external systems via `Ops.CURRENT_MICROBATCH_ID`.

If the design needs exactly-once and uses stream — wrong choice (or accept duplicate processing risk explicitly).

Citation: `reference/rama/docs/05-types-of-etls.md` L88 (table).

---

### 8. PROXYSTATE PATH SHAPE

For every `foreign-proxy` in the design:

- Path navigates to EXACTLY one value (per docs requirement).
- Path is per-key (e.g. `[(keypath run-id)]`), NOT root path (`[]`). Project memory: `[]`-path on global PStates triggered RocksDBWrapper crash in Rama 1.6.0.
- Subscriber calls `close` when done (`ProxyState` server resources leak otherwise).
- Failure: 10 errors in 120s → forcible termination + `UngracefulTerminationDiff`. Path must not throw.

Citation: `reference/rama/docs/15-pstates.md` L436-540 (proxy semantics, fault handling).

---

### 9. SUBINDEXING DECISION FOR LARGE NESTED STRUCTURES

For every nested `map-schema` / `set-schema` / `list-schema` the design declares, ask: how many elements can the inner structure hold?

- **<50 elements typical**: subindex makes no perf difference. Skip.
- **>50 elements**: every write WITHOUT subindex rewrites the full nested value. With `{:subindex? true}`: O(log n) writes, O(1) reads-by-key, sorted-map-range scans available.

Slice A example: `$$compute-log-by-run` inner `{Long → chunk}` can grow to thousands of chunks per run. MUST be subindexed.

Citation: `reference/rama/docs/15-pstates.md` L92-178.

---

### 10. HASH EXTRACTOR DETERMINISM

For each `(declare-depot setup *depot (hash-by …))`, verify the extractor function is deterministic on the appended record. Common breakages:
- Extractor returns `nil` for some inputs → all those records hash to the same partition.
- Extractor calls `(System/currentTimeMillis)` or `(rand)` → records non-deterministically partitioned, ordering broken.
- Extractor returns a complex Clojure data structure with non-canonical hash → cluster-internal hash may differ from JVM hash.

Right patterns: keyword extractor on a stable field (`:request/id`, `:run/id`, `:routing/key`), or a top-level `defn` that's deterministic and on the classpath.

Citation: `reference/rama/docs/14-depots.md` L37-65; `reference/rama/docs/28-clj-defining-modules.md` L78-83 (Clojure idiom: keyword or top-level `defn` only).

---

### 11. TASKGLOBAL / EXECUTOR HANDOFF

Task globals are good for module-owned resources and background machinery. They
do not automatically make irreversible work retry-safe.

Check:

- `TaskGlobalObject` is used for per-task resources, long-lived clients, or
  background worker state.
- Methods called from topology code are non-blocking.
- Any side effect triggered from topology code is idempotent under retry.
- Process runners prefer committed-state handoff: topology writes pending/intent
  state, executor observes or claims committed state, executor appends
  observations back to Rama.

Slice-A repair shape:

```text
request depot -> owning topology -> $$compute-runs[run-id] = :pending
                                      |
                                      v
                              executor sees committed intent
                                      |
                                      v
obs depot <- :started/:stdout/:stderr/:heartbeat/:exit <- child process
                                      |
                                      v
                         owning topology -> $$compute-runs, $$compute-log
```

Citation: `reference/rama/docs/25-integrating.md` L64-131 for task globals,
L219-268 for async external integration, and
`reference/rama/docs/28-clj-defining-modules.md` L146-153 for Clojure
`declare-object`.

---

## Common repairs

```text
two topologies write one PState
  -> one owning topology, multiple source blocks

random id minted in topology
  -> deterministic id from source record, or appender-minted id in envelope

request append uses :append-ack but UI expects materialized decision
  -> use :ack if appropriate, or poll/proxy $$decisions by request id

stdout observations use :ack
  -> use :append-ack plus retry/idempotent sequence keys

child process spawned from request topology
  -> write pending run row; executor acts after committed intent

claim queue appears for one executor
  -> prefer pending run row first; add claim depot/queue when competing executors exist

log PState is map<run-id, map<seq, chunk>> without subindex
  -> subindex the inner seq map

direct executor -> UI stream
  -> executor -> observation depot -> topology -> PState -> UI proxy
```

---

## Output format

After walking the protocol, produce a verdict block:

```
RAMA-PITFALLS VERDICT for <design name>

  1. EVENT BOUNDARY MAP        : PASS|FAIL|N/A — <citation or specific failure>
  2. SIDE-EFFECT RETRY TRACE   : ...
  3. PSTATE OWNERSHIP          : ...
  4. BACK-ARROW CHECK          : ...
  5. RETRY IDEMPOTENCE OF IDs  : ...
  6. ACK LEVEL APPROPRIATENESS : ...
  7. STREAM vs MICROBATCH      : ...
  8. PROXYSTATE PATH SHAPE     : ...
  9. SUBINDEXING DECISION      : ...
 10. HASH EXTRACTOR DETERMINISM: ...

OVERALL: READY-TO-CODE | NEEDS-REVISION | UNSAFE-AS-DESIGNED
RESIDUAL RISKS (acknowledged, deferred): <list, each with which slice will close it>
```

Do NOT mark a section PASS without naming a specific reason. "Looks fine" is not a citation.

## What this skill is NOT

- Not a substitute for reading the docs. The skill points to citations; you still verify against the actual file.
- Not a checklist to game ("write PASS to all sections so I can ship"). The exercise is the falsification, not the checkmark.
- Not exhaustive. The pitfalls listed are the ones that have bitten this codebase or been caught in review. New pitfalls discovered should be added to this skill, not noted only in conversation.

## How this skill grew

Originated 2026-05-02 after the slice-A v2 review of the dogfood-runtime ComputeDepot design. Two specific failures motivated it:
1. `(completable-future> (.spawn-async *executor …))` inside the request topology event — duplicate-spawn under retry.
2. Multi-key writes across `(|hash *request-id)` then `(|hash *run-id)` claimed as "atomic in one event" — actually two events.

Both failures had explicit doc citations I had read but not applied. The skill exists to make application non-optional, not to teach the docs.
