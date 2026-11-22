---
name: rama-pitfalls
description: Use as a Rama failure-shield when designing, reviewing, or implementing Rama modules, depots, topologies, PStates, TaskGlobal/executor integrations, foreign clients, ack flows, partitioning, policy placement, or replay/idempotency logic. Complements $rama: $rama provides Rama's API, schema, and cost rules; this skill falsifies a concrete design or diff against recurring Rama and Softland foot-guns (event boundaries, partial hop retries, PState/module ownership, back-arrow violations, termval lifecycle resets, duplicate/conflict replay, out-of-order poison records, ack visibility, bounds, entity locality, and weak product-path tests).
allowed-tools: Read, Grep, Glob, Bash
---

# rama-pitfalls

Run this skill as an adversarial Rama design and implementation review. It is not
here to invent the architecture; it is here to catch the places where a
plausible Rama-looking design or module silently stops being Rama-correct.

Use `$rama` first for Rama API, schema, and performance rules; run
`rama-pitfalls` once there is a concrete module sketch, plan, or diff to attack.

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

Citation: `/mnt/data/projects/research/softland/rama-reference/docs/23-acid-semantics.md` L41-52: *"A stream topology event is all code between partitioner calls."*

```
WRITES:
  $$pstate-A   in event 1
  $$pstate-B   in event 1   ← OK: atomic with $$pstate-A
  $$pstate-C   in event 2   ← NOT atomic with A or B
```

If the design needs cross-PState atomicity, redesign so all writes share a partitioner — same key value (one task one event) or `(|global)` (all writes on task 0).

Also trace **partial retry across partitioner hops**: if event 1 commits a state/idempotency marker on partition A and hops (`|hash`) to partition B where event 2 fails, a naive retry of the depot record that short-circuits on partition A's marker will skip partition B's write forever, leaving split brain (grant without inbox entry, claim without account). Either collapse into one partition or make hop-1 forward idempotently until hop-2 confirms.

Citation: `history/docs/rama-retro-review/META_LEARNINGS.md` (invariants spanning partitioner hops); `history/old-docs/retros/rama/`.

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

Citation: `/mnt/data/projects/research/softland/rama-reference/docs/11-stream-topologies.md` L156, L170: at-least-once retry; on event failure all batch PState writes are discarded but external side effects persist.

**Fix patterns** (back-arrow, AOR-style):
- Topology writes COMMITTED INTENT to a PState (e.g. `:status :pending`).
- External consumer (TaskGlobalObjectWithTick, separate process) reads committed intent.
- Consumer's `spawn-if-absent` registry dedupes within its own lifetime.
- Consumer's work is the side effect; observations stream BACK through an obs depot.

---

### 3. PSTATE & MODULE OWNERSHIP

For every PState in the design, name its single owning topology. Writers from any other topology = design violation.

Citation: `/mnt/data/projects/research/softland/rama-reference/docs/15-pstates.md` L41: *"a PState can only be updated by the ETL topology that owns it."*

```
$$compute-runs-by-id   owner: compute-runtime-topology   ✓
$$compute-log-by-run   owner: compute-runtime-topology   ✓
$$compute-decisions    owner: compute-runtime-topology   ✓
```

If two source flows need to write the same PState, they must live in the same topology as separate `(source> …)` blocks inside one `(<<sources …)`. NOT two topologies.

Across modules, verify **dependency and mirror direction**: a lower/common module must never depend upward on a source-specific or adapter module, adapters must emit into common contracts rather than creating private truth islands unreadable through common APIs, and operational cursor/test helpers must not collapse into domain ownership boundaries.

Citation: `history/docs/rama-retro-review/ARCHITECTURE_VERDICT.md`; `history/docs/rama-retro-review/META_LEARNINGS.md` (wrong depot/PState ownership, truth islands, collapsed boundaries).

---

### 4. BACK-ARROW CHECK

The dogfood-runtime principle: **workers/agents stream observations BACK to Rama; Rama is truth; UI reads Rama.** Forward push from topology to executor inverts this and breaks retry safety.

For each non-Rama actor in the design (executor, agent CLI, browser), verify:
- It does NOT mutate Rama PStates directly. ✓
- The UI reads Rama state, not the actor's state (no in-memory atom or transitional compat mirror acting as permanent UI truth without a deterministic rebuild path from Rama). ✓
- The actor reads Rama as its source of truth (committed intent), not the topology calling it inline. ✓
- Observations from the actor flow into a depot; topology materializes; UI sees results. ✓

Citation: `history/old-docs/architecture/dogfood-runtime/README.md` L84-90; `history/docs/rama-retro-review/META_LEARNINGS.md` (compat/in-memory mirrors becoming truth).

---

### 5. RETRY IDEMPOTENCE OF GENERATED IDs

For every ID generated inside topology code (`(mint-id …)`, `(random-uuid)`, `(System/currentTimeMillis)`, etc.), verify the generation is deterministic from the source depot record.

If `(random-uuid)` runs inside topology code, every retry produces a different ID. Result: duplicate rows on different keys instead of one row overwritten.

Right pattern: derive from `:request/id` (UUID v5 with namespace), or have the appender mint the ID before `foreign-append!` and put it in the request envelope.

Citation: `/mnt/data/projects/research/softland/rama-reference/docs/11-stream-topologies.md` L156: at-least-once means duplicate processing.

---

### 6. ACK LEVEL & READ-AFTER-WRITE VISIBILITY

For every `foreign-append!` in the design, name the ack level:

- `:ack` (default) — waits for colocated stream topologies to FINISH. Use for "submit and confirm processed" UI flows. Subject to `topology.stream.max.executing.per.task` throttle.
- `:append-ack` — waits only for replication. Use for high-volume appends (executor's stdout chunks, observation streams). Avoids throttle.
- `nil` (fire-and-forget) — no waiting. Use only when caller doesn't need confirmation.

A high-volume appender using `:ack` will hit throttle and throw. A confirmation-flow appender using `:append-ack` will succeed before the topology has materialized state, causing UI race.

Even with `:ack`, **completion is only local to colocated stream events**: if the topology repartitions (`|hash` to another key), feeds a microbatch, or mirrors across modules, `:ack` returns *before* those downstream PStates update. Verify no public helper returns synchronous success or marks `:complete` before the product-readable PStates are actually queryable (unless explicitly returning an async handle).

Citation: `/mnt/data/projects/research/softland/rama-reference/docs/14-depots.md` L198-218; `/mnt/data/projects/research/softland/rama-reference/docs/11-stream-topologies.md` L292-299 (throttling); `history/docs/rama-retro-review/META_LEARNINGS.md` (helper completion before durable truth).

---

### 7. STREAM vs MICROBATCH

State which topology type was chosen and why:

- **Stream**: few-ms latency, at-least-once retry, depot-append integration, no batch blocks. Use for live UI updates, low-latency reactivity.
- **Microbatch**: hundreds-of-ms latency, exactly-once, NO depot-append integration, batch blocks available (joins, two-phase aggregation, temporary PStates). Use for bulk recompute, global aggregation, exactly-once on external systems via `Ops.CURRENT_MICROBATCH_ID`.

If the design needs exactly-once and uses stream — wrong choice (or accept duplicate processing risk explicitly).

Citation: `/mnt/data/projects/research/softland/rama-reference/docs/05-types-of-etls.md` L88 (table).

---

### 8. PROXYSTATE PATH SHAPE

For every `foreign-proxy` in the design:

- Path navigates to EXACTLY one value (per docs requirement).
- Path is per-key (e.g. `[(keypath run-id)]`), NOT root path (`[]`). Project memory: `[]`-path on global PStates triggered RocksDBWrapper crash in Rama 1.6.0.
- Subscriber calls `close` when done (`ProxyState` server resources leak otherwise).
- Failure: 10 errors in 120s → forcible termination + `UngracefulTerminationDiff`. Path must not throw.

Citation: `/mnt/data/projects/research/softland/rama-reference/docs/15-pstates.md` L436-540 (proxy semantics, fault handling).

---

### 9. SUBINDEXING & COLLECTION BOUNDS

For every nested `map-schema` / `set-schema` / `list-schema` the design declares, ask: how many elements can the inner structure hold, and what bounds its lifetime growth?

- **<50 elements typical**: subindex makes no perf difference, **provided a hard cap, TTL, or eviction rule keeps it `<50`**. Any un-subindexed set/map/vector that grows per request, decision, or rotated secret without bound causes whole-row write amplification and eventual OOM/task stalls.
- **>50 elements**: every write WITHOUT subindex rewrites the full nested value. With `{:subindex? true}`: O(log n) writes, O(1) reads-by-key, sorted-map-range scans available.
- Avoid catch-all `{String Object}` schemas on hot PState rows; declare explicit typed schemas.

Slice A example: `$$compute-log-by-run` inner `{Long → chunk}` can grow to thousands of chunks per run. MUST be subindexed.

Citation: `/mnt/data/projects/research/softland/rama-reference/docs/15-pstates.md` L92-178; `history/docs/rama-retro-review/META_LEARNINGS.md` (unbounded non-subindexed collections & `{String Object}` hot rows).

---

### 10. HASH EXTRACTOR DETERMINISM & ENTITY LOCALITY

For each `(declare-depot setup *depot (hash-by …))`, verify both **determinism** and **entity locality**:
- Extractor returns `nil` for some inputs → all those records hash to the same partition.
- Extractor calls `(System/currentTimeMillis)` or `(rand)` → records non-deterministically partitioned, ordering broken.
- Extractor returns a complex Clojure data structure with non-canonical hash → cluster-internal hash may differ from JVM hash.
- **Locality smell (`hash-by` request ID):** Keying a depot by a per-call `:request/id` just because every request has one routes the topology away from the target entity's PState partition, forcing an extra `|hash` hop (and losing single-event atomicity) to check permissions or transition entity state. Partition by the entity whose local ordering and authoritative state matter; treat request ID as an idempotency/index key unless the entity itself IS the run/request.

Right patterns: keyword extractor on the stable entity/locality field (e.g. `:run/id` when run is the entity, or a top-level deterministic `defn` on the classpath extracting the target entity key).

Citation: `/mnt/data/projects/research/softland/rama-reference/docs/14-depots.md` L37-65; `/mnt/data/projects/research/softland/rama-reference/docs/28-clj-defining-modules.md` L78-83; `history/old-docs/architecture/think-in-rama.md` (throughput & entity-locality rubric).

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

Citation: `/mnt/data/projects/research/softland/rama-reference/docs/25-integrating.md` L64-131 for task globals,
L219-268 for async external integration, and
`/mnt/data/projects/research/softland/rama-reference/docs/28-clj-defining-modules.md` L146-153 for Clojure
`declare-object`.

---

### 12. POLICY PLACEMENT & DURABLE OUTCOMES

Separate pre-append edge hygiene from authoritative world decisions:
- **Edge guards (before append):** transport auth/session check, payload size/weight bounds, rate limiting, and envelope construction. Do NOT stall single-threaded task threads on giant unvalidated payloads, and do NOT put replayable world/permission truth solely in an API helper or UI check outside Rama.
- **Authoritative decisions (inside Rama, post-depot):** target existence, permission/standing checks, and state transition validity must execute inside the topology against durable PStates, never by appending pre-blessed "accepted" events built in a helper.
- **Durable outcomes & derived depots:** rejected requests/refusals that callers or audits observe must be durable/queryable rather than silently dropped; projections that filter visibility should preserve why; topology-owned derived depots should reject direct external client appends.

Citation: `history/old-docs/architecture/think-in-rama.md`; `history/old-docs/architecture/action-request-kernel-routing.md`; `history/old-docs/architecture/rama-policy-throughput-post.md`; `history/docs/rama-retro-review/META_LEARNINGS.md` (feature-slice helpers).

---

### 13. LIFECYCLE MONOTONICITY, DUPLICATE REPLAY & CURSOR ADVANCE

Stream topologies are at-least-once, and clients retry appends. Verify:
- **No unguarded `termval` over lifecycle rows:** overwriting a live or terminal row (`:running`, `:closed`, `:ended`, `:decided`) with `:pending`/`:open` on duplicate or replayed delivery re-triggers physical work or reopens closed state. Guard transitions explicitly on current state.
- **Sticky terminal states:** writes arriving *after* an entity reaches a terminal state must not regress or mutate terminal outcome fields.
- **Same-payload replay vs. conflict:** a duplicate idempotency/act ID with the **same** payload must no-op and return the standing outcome; the same ID with a **different** payload must conflict-reject rather than silently alias or overwrite.
- **Operational cursor/offset ordering:** file offsets, stream cursors, or resume checkpoints advance *only after* the durable PState truth they represent has committed.

Citation: `history/docs/rama-retro-review/META_LEARNINGS.md` (unguarded `termval` — single most repeated retro defect; monotonic terminals; cursor advance); `history/old-docs/retros/rama/`.

---

### 14. UNTRUSTED OBSERVATIONS, POISON RECORDS & INPUT IDENTITY

Topologies must treat every depot record as potentially out-of-order, unauthorized, or malformed:
- **Unknown IDs & authority:** observations or control events arriving *before* their target/request exists, or carrying missing, wrong, or stale executor/actor tokens, must be safely ignored or rejected — never invent phantom rows or fold unauthorized state.
- **Poison-record NPEs before guards:** eager `let`-bindings, destructuring, or type casts placed *before* nil/validity guards throw uncaught exceptions in a stream topology, turning one malformed record into an infinite retry poison loop.
- **Identity normalization & secret redaction:** check non-ASCII / multi-byte UTF-8 inputs wherever identity, hashing, or slicing uses byte offsets (and use locale-independent `Locale/ROOT` when case-folding identifiers such as emails); verify malformed inputs carrying secrets do not leak raw secrets into durable error/refusal rows or logs.

Citation: `history/docs/rama-retro-review/META_LEARNINGS.md` & `history/old-docs/retros/rama/` (R5 depot-adversary probes, poison-record NPE, UTF-8 byte corruption, secret redaction); commit `d8ee975c` (`Locale/ROOT` email case-folding).

---

### 15. RUNTIME PROBE & TEST BAR

Static reading proposes; execution disposes. Before calling a module or review done, verify:
- **Namespace load step zero:** `(require 'every.reviewed.namespace)` runs cleanly before trusting any static trace.
- **Payload assertions on product paths:** tests exercise public client/product paths (not internal row builders alone) and assert the **exact payload the spec promised**, not mere row existence.
- **Adversarial matrix:** tests cover rejection paths, duplicate ID (same vs. different payload), post-terminal writes, unknown-ID/stale-token observations, and pushing a bounded collection past its cap.
- **Named invariant = proven invariant:** every invariant claimed in docs/plans names its enforcing mechanism and has a check proven to fail red when that mechanism is broken.

Citation: `history/old-docs/retros/rama/` & `history/docs/rama-retro-review/META_LEARNINGS.md` (R5 runtime probes, spine-only green tests, named vs. proven invariants).

---

## Common repairs

```text
two topologies write one PState
  -> one owning topology, multiple source blocks

random id minted in topology
  -> deterministic id from source record, or appender-minted id in envelope

unguarded (termval {:status :pending ...}) on stream retry
  -> conditional transform guarded on nil / allowed prior status; sticky terminals

duplicate id with different payload aliases original
  -> store payload digest/fields on first sight; replay on match, conflict-reject on mismatch

request append uses :append-ack (or crosses |hash / mirror) while helper returns :complete
  -> wait/query until downstream product-readable PState is visible, or return async handle

stdout observations use :ack
  -> use :append-ack plus retry/idempotent sequence keys

child process spawned from request topology
  -> write pending run row; executor acts after committed intent

claim queue appears for one executor
  -> prefer pending run row first; add claim depot/queue when competing executors exist

log PState is map<run-id, map<seq, chunk>> without subindex
  -> subindex the inner seq map; enforce explicit bounds/eviction on non-subindexed collections

depot hashed by :request/id before entity permission/state transition
  -> hash depot by target entity key; keep :request/id as idempotency/index key

direct executor -> UI stream
  -> executor -> observation depot -> topology -> PState -> UI proxy
```

---

## Output format

After walking the protocol, produce a verdict block:

```
RAMA-PITFALLS VERDICT for <design name>

  1. EVENT BOUNDARY & HOP RETRY : PASS|FAIL|N/A — <citation or specific failure>
  2. SIDE-EFFECT RETRY TRACE    : ...
  3. PSTATE & MODULE OWNERSHIP  : ...
  4. BACK-ARROW CHECK           : ...
  5. RETRY IDEMPOTENCE OF IDs   : ...
  6. ACK & READ-AFTER-WRITE     : ...
  7. STREAM vs MICROBATCH       : ...
  8. PROXYSTATE PATH SHAPE      : ...
  9. SUBINDEXING & BOUNDS       : ...
 10. HASH DETERMINISM & LOCALITY: ...
 11. TASKGLOBAL HANDOFF         : ...
 12. POLICY & DURABLE OUTCOMES  : ...
 13. LIFECYCLE & DUPLICATE IDs  : ...
 14. UNTRUSTED OBS & POISON REC : ...
 15. RUNTIME PROBE & TEST BAR   : ...

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
