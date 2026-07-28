# multi-cascade R2 — Rama Phase 1 PLAN

Status: Phase 1 design only, 2026-07-27. No topology, source, test, or deploy
implementation belongs to this phase. `CONTRACT_R2.md` is authoritative;
`IMPLICIT_SPEC.md` supplies the complete operation and adversarial inventory.
This plan does not reopen the fixed architecture.

The required manifest was re-read against disk before planning. Its substance
matches. One navigation hint drifted: the fail-closed inventory in
`test/app/test_runner.clj` is currently at lines 217–250, not 214–247. This is
locator drift only and is not a stop under CONTRACT §6/§8. The `:system` actor
path was also rechecked: `authorized-request?` accepts it. No physically
buildable contract/platform fork was found.

## Reads

### R1. One run

Public P2 API:

```clojure
(cascade/run ctx run-id) ; => bounded plain map or nil
```

P1 supplies the lower-level cascade-log reader; P2 exposes it from the one
dispatch namespace.

- Validate `run-id` as a nonblank String before I/O.
- Use exactly one direct point read:
  `foreign-select-one [(keypath run-id)] $$cascade-runs`.
- The PState and both depots are on `hash(run-id)`, so the point read reaches
  the one authoritative partition.
- Convert the stored `CascadeRunRow` and nested records to the contract's plain
  map shape at the foreign boundary. Do not rederive an id or read a cache.
- Missing or malformed ids return nil without a scan.

Cost: one meaningful seek for a shaped id, zero for malformed input. This is
interactive and does not need a query topology.

### R2. Pending runs

Public P2 API:

```clojure
(cascade/pending-runs ctx) ; => every obligated run, unordered
```

P1 supplies a lower-level administrative enumeration used by the P1 module
gates and later by P2.

1. Read `:num-partitions` from `$$cascade-pending`'s foreign object info.
2. Generate one known hashing key per task with Rama's public
   `Helpers/genHashingIndexKeys`.
3. For each task, issue one root `ALL` scan of `$$cascade-pending`, explicitly
   routed with that task's generated `:pkey`. This is one seek plus sequential
   iteration on that partition.
4. For each returned `[run-id obligated-at-ms]`, point-read
   `$$cascade-runs` at `[(keypath run-id)]` and return the full obligation row.
   The index timestamp must equal the current row's `:obligated-at-ms`; a
   mismatch is not silently returned as resumable truth.
5. Return `[]` for empty state. Ordering is deliberately non-semantic.

For `P` partitions and `K` pending runs, the cost is `P` range seeks, `K`
pending iterations, and `K` run point seeks: approximately
`(P + K) * 0.5ms + K * 5us`, excluding network scheduling. `K` is small by
construction and this is boot/admin work, not a hot request path.

### R3. Failed runs

Public P2 API:

```clojure
(cascade/failed-runs ctx) ; => first-terminal-failed rows, unordered
```

- Use the same one-generated-key-per-task technique.
- Root-scan `$$cascade-runs` once per partition with `ALL`.
- Convert and retain only rows whose base `:status` is `:failed`.
- Never classify from `:late-conflict`: completed plus a late failed conflict
  remains completed; failed plus a late completed conflict remains failed.

For `P` partitions and `R` lifetime runs, cost is `P` seeks plus `R`
iterations: approximately `P * 0.5ms + R * 5us`. The lifetime set is
unbounded, so this remains an explicit console/admin enumeration. R2 does not
add pagination or a third index.

### Why there is no query topology

The generic multi-read preference would normally favor a query topology.
Here the binding contract instead fixes direct point read plus client-side
per-partition enumeration and exactly two PStates. A query topology would
centralize the same `P` scans and `K` enrichments but would not remove their
RocksDB seeks or iterations; adding it would widen the fixed one-topology
shape. The two enumerations are administrative, pending is small, and neither
is a hot endpoint. This is a contract-specific exception, not a general
client-side multi-read pattern.

## Writes

There are exactly two durable writes. Both are plain, namespaced-key map
envelopes on the wire so `(hash-by :run/id)` reads the actual contract key.
Constructors reject wrong key sets, wrong types, oversized strings, non-EDN
values, and missing identity before append. The topology repeats total
validation defensively and drops an invalid raw depot record without throwing
or mutating state; the supported append path cannot construct one.

### W1. Record an obligation

Depot: `*cascade-obligations-depot`.

Exact wire envelope:

```clojure
{:run/id          String
 :cascade/id      Keyword
 :cascade/trigger Keyword
 :emission/id     String
 :payload         {episode-close payload keys exactly}
 :obligated-at-ms Long}
```

For R2 the exact payload key set is:

```clojure
{:turn-id         String
 :terminal-status Keyword
 :exit-code       Long-or-nil
 :duration-ms     Long
 :episode-id      String
 :fresh?          Boolean
 :seed?           Boolean
 :thread-id       String-or-nil
 :conversation-id String-or-nil
 :cwd             String
 :time-ms         Long
 :jsonl-exists?   Boolean}
```

The emitter constructs the wire map in P2. The pure identity constructors land
with the P1 module because G2-module pins their bytes:

- episode-close emission id =
  `"casc-em:episode-turn-closed:" + sha256(turn-id NUL
  name(terminal-status))`;
- run id =
  `"casc-run:" + sha256(name(cascade-id) NUL emission-id)`.

No time, cluster identity, random value, handler result, or runtime handle
participates. A malformed discriminator produces no identity. P2 appends with
`:ack`; when it returns, the colocated topology write is visible. P1 tests use
the same constructor and `:ack` directly, without implementing `react!`.

### W2. Record an observation

Depot: `*cascade-observations-depot`.

Exact wire envelope:

```clojure
{:run/id        String
 :status        Keyword ; exactly :completed or :failed
 :receipt       {bounded receipt keys exactly}
 :observed-at-ms Long}
```

The bounded receipt vocabulary is fixed at the cascade boundary:

```clojure
{:outcome       Keyword
 :reason        Keyword-or-nil
 :import-key    String-or-nil
 :order-key     String-or-nil
 :error-class   String-or-nil
 :error-message String-or-nil}
```

Completed handler results normalize to `:repaired` or `:skipped` plus a bounded
reason/import description. Throws normalize to `:error`, bounded class name,
and a capped error message. Arbitrary handler objects, stack traces, request
handles, and unbounded nested responses never ride the depot. P2 appends the
observation with `:ack`, so runner completion means terminal truth is
materialized. P1 uses the same constructor directly for G3/G4.

## PState Design

### Typed boundary reconciliation

The depot contracts remain plain maps. This is required both for EDN map
semantics and for `(hash-by :run/id)` to see the namespaced routing key; a
defrecord envelope would expose an unqualified record field instead. Inside
the topology, validated maps are converted to concrete records before PState
storage. Public readers convert those records back to plain maps.

No PState schema contains `Object`, `IPersistentMap`, or an untyped collection.
The exact internal records and field domains are:

```clojure
EpisodeTurnClosedPayload
  [turn-id:String
   terminal-status:Keyword
   exit-code:Long-or-nil
   duration-ms:Long
   episode-id:String
   fresh?:Boolean
   seed?:Boolean
   thread-id:String-or-nil
   conversation-id:String-or-nil
   cwd:String
   time-ms:Long
   jsonl-exists?:Boolean]

CascadeReceipt
  [outcome:Keyword
   reason:Keyword-or-nil
   import-key:String-or-nil
   order-key:String-or-nil
   error-class:String-or-nil
   error-message:String-or-nil]

CascadeTerminalEvidence
  [status:Keyword
   receipt:CascadeReceipt
   observed-at-ms:Long]

CascadeRunRow
  [run-id:String
   cascade-id:Keyword-or-nil
   cascade-trigger:Keyword-or-nil
   emission-id:String-or-nil
   payload:EpisodeTurnClosedPayload-or-nil
   obligated-at-ms:Long-or-nil
   status:Keyword
   receipt:CascadeReceipt-or-nil
   observed-at-ms:Long-or-nil
   late-conflict:CascadeTerminalEvidence-or-nil]
```

The nullable obligation fields are necessary for observation-before-obligation;
nullable terminal fields are necessary while obligated. A single uniform
`CascadeRunRow` avoids storing lifecycle variants as optional fixed-key maps
or as polymorphic `Object`. Constructors and validators enforce every nested
domain and bound before the record reaches a PState.

### `$$cascade-runs`

Exact Rama schema:

```clojure
{String CascadeRunRow}
```

Outer key is the globally scoped run id, on `hash(run-id)`. The row is bounded:
one fixed payload, one fixed primary receipt, and at most one fixed
late-conflict value. No per-run collection can grow, so subindexing would add
no value.

This is the authoritative lifecycle truth. One direct point read answers R1.
An alternative split into obligated and terminal PStates was rejected: it
would violate the fixed two-PState contract, require multi-read arbitration,
and create two sources of current truth.

### `$$cascade-pending`

Exact Rama schema:

```clojure
{String Long}
```

Key is run id; value is the current obligation's `obligated-at-ms`. It is the
authoritative resumable-membership index, not a second lifecycle truth. A
terminal event deletes the key in the same source event that writes the
terminal run row.

An inner subindexed map was rejected. There is no natural bounded parent key,
and the contract requires per-partition enumeration. A top-level map gives
one direct delete/upsert per run and one sequential root scan per partition.

## Depots

- `*cascade-obligations-depot`: `(hash-by :run/id)`, W1 maps only.
- `*cascade-observations-depot`: `(hash-by :run/id)`, W2 maps only.

They are separate because they are logically different ingress streams and
because external handler execution occurs between them. They share the same
routing key, so every event for a run begins on the task owning both PState
keys. There is no repartition in either source branch.

Ordering across the two depots is intentionally not assumed:
observation-before-obligation is a valid state transition. Within each depot,
the source uses `:retry-mode :all-after`. Restoring the failed record and all
later records in partition order is what keeps “latest obligation while still
obligated” and “last conflicting observation evidence” stable even after a
retry. Reducers are total and writes are still replay-idempotent.

## Topologies and PStates

There is exactly one stream topology, `"cascade-runs"`, containing exactly two
`source>` branches in one `<<sources`. It alone owns and writes both PStates.

Stream is required because W1's `:ack` is an interactive
materialize-before-call barrier. A microbatch's latency and append
coordination do not meet that contract. No `ack-return>` is needed: append
returns only after materialization, and the caller uses R1 when it needs the
row.

### Obligation source event

The event starts on `hash(:run/id)`.

1. Validate and convert the plain payload to
   `EpisodeTurnClosedPayload`.
2. Point-read the current `CascadeRunRow` locally.
3. If absent or `:obligated`, construct the new obligated row from this event,
   preserving no terminal fields, and in this same event:
   - `termval` the row at `[(keypath run-id)]` in `$$cascade-runs`;
   - `termval obligated-at-ms` at `[(keypath run-id)]` in
     `$$cascade-pending`.
   A replay while obligated intentionally makes this obligation payload/time
   current, so grace is evaluated from the resulting time.
4. If current status is `:completed` or `:failed`, perform no write. A terminal
   row can never be resurrected or re-pended.

Both writes are absolute value writes and replay-idempotent. They occur on one
task in one stream event.

### Observation source event

The event starts on the same `hash(:run/id)` task.

1. Validate and convert the receipt to `CascadeReceipt`.
2. Point-read the current row locally.
3. If absent, write a terminal `CascadeRunRow` with nullable obligation fields
   and delete pending with `NONE>`.
4. If obligated, preserve the obligation fields, set the first terminal
   status/receipt/time, leave `:late-conflict` nil, and delete pending.
5. If already terminal with the same status, do no row write; delete pending
   defensively.
6. If already terminal with the other status, preserve every base field and
   overwrite only `:late-conflict` with this event's
   `CascadeTerminalEvidence`; delete pending defensively.

The run write and pending delete are one colocated source event. First terminal
therefore wins; a conflicting terminal never regresses it. There is no
increment, list append, or seen-id set. Replaying an observation sequence under
`:all-after` restores the same last conflict value.

### Event boundaries, I/O, and retry trace

- Source boundary: one depot record.
- Partition boundary: none after source; depot routing already matches both
  PState keys.
- State I/O per valid obligation: one local point read, then zero or two value
  writes.
- State I/O per valid observation: one local point read, then one row value
  write or none, plus one idempotent pending delete.
- External-effect boundary: outside this topology. P2 waits for W1 `:ack`,
  starts the JVM handler, then appends W2.
- Retry after pre-commit failure repeats pure derivation and absolute writes.
- Retry after commit repeats the same absolute values/deletes.
- A later observation processed before an earlier obligation still leaves a
  terminal row; the obligation's terminal guard makes it a no-op.

There are no topology-side logs, handler calls, clocks, UUIDs, futures,
depot-appends, external requests, TaskGlobals, or query invocations.

## Query Topologies

None. The fixed read surface and reason are documented under Reads.

## Design Decisions

- **Colocation:** both depots route by their literal namespaced `:run/id`;
  both PStates use that String as their top-level key. No silent nil routing
  and no topology repartition.
- **Bounded maps:** plain maps exist only on the external contract boundary;
  concrete records exist in PState. This preserves map/EDN semantics without
  weakening the durable schema.
- **Subindexing:** neither PState has a per-entity growing nested collection.
  No subindex.
- **First terminal:** the base terminal is immutable after first write.
  `:late-conflict` is one replacement value, not a counter or history.
- **Time:** obligated/observed times come from the event envelopes. Topology
  code never reads wall time. P2 grace evaluation reads its invocation time
  outside the topology.
- **Identity:** emitter-side pure constructors are pinned in P1; topology code
  never mints or repairs ids.
- **Truth:** depots are append-only inputs/history, `$$cascade-runs` is current
  run truth, and `$$cascade-pending` is current resumable membership.
- **No scheduler:** the module never self-fires. P2 owns one explicit boot
  sweep after runtime binding.

## State primitive selection

- `$$cascade-runs` — PState; durable authoritative lifecycle truth. O(1)
  bounded writes per source event.
- `$$cascade-pending` — PState; durable derived membership required across JVM
  and worker death. O(1) bounded writes per source event.
- Both depots — durable Rama ingress/history.
- `!episode-chains` and handler futures — existing P2 JVM operational state,
  never authoritative cascade truth and never module state.
- TaskGlobals — none.
- External database/queue/journal — none.
- Query topology, timer, scheduler, retry queue, and cache — none.

## IPC and cluster runtime seam

P1 implements constructors in `app.server.rama.cascade-log`; invoking them is
the only way IPC launch occurs.

- `start-cascade-log-runtime!` accepts either no options or
  `{:ipc existing-ipc :launch-opts ...}`.
- Without an IPC it creates one, launches only `cascade-log-module`, and marks
  ownership true. With an IPC it launches the module into that IPC and marks
  ownership false.
- The returned bundle carries the IPC, ownership flag, module name, two depot
  handles, and two PState handles.
- `close-cascade-log-runtime!` closes only an owned IPC; it never closes a
  caller's shared cluster.
- Constructors, handle lookup, and close functions are inert until called.

P1 runtime-plumbing edits are narrowly allowed:

- `cluster.clj` adds the cascade bundle and memoized
  `cascade-log-runtime` accessor, matching the existing manager/handle pattern.
- Cluster `face-projection-runtime` includes `:cascade-rt`.
- The IPC branch in `file_viewer.cljc` launches the cascade module on the
  already-created OC IPC and includes `:cascade-rt`; the cluster branch reads
  the accessor. `face-ctx` selects the additional key. The existing lazy delay
  remains the launch boundary and must be total rather than cache a throw.
- P2 consumes that bound runtime; P2 alone wires and invokes the boot sweep.

## Phase ownership and file fence

### P1 — module package only

P1 may change only:

- NEW `src/app/server/rama/cascade_log.clj`: typed records, strict map
  constructors/validators, deterministic emission/run-id constructors, two
  depots, one stream topology, two PStates, foreign readers/enumerators, IPC
  start/close/bundle functions.
- NEW `test/app/cascade_log_test.clj`: G2m/G3m/G4m/G9m plus the committed
  post-deploy harness.
- `src/app/server/rama/cluster.clj`: cascade bundle/accessor only.
- `src/app/file_viewer.cljc`: the runtime-plumbing lines above only, and only
  because the shared IPC boot needs them.
- `bin/land`: exactly one sixth `MODULE_VARS` entry,
  `app.server.rama.cascade-log/cascade-log-module`.
- `test/app/test_runner.clj`: exactly one classification entry for
  `app.cascade-log-test`, in the same isolation class as
  `app.cascade-table-test`.

P1 owns G2-module, G3-module, G4-module, G9-module, and G12. It does not edit
`cascade.clj`, `episode.clj`, or `server_jetty.clj`; it does not add the P2 test
namespace early.

### P2 — seams and activation

P2 owns:

- `cascade.clj`: durable row grammar and row #2; durable `react!` branch;
  handler resolution; common runner; public run/pending/failed APIs;
  `resume-obligated!`.
- `server_jetty.clj`: episode-close emission before terminal overwrite and
  exactly one explicit boot sweep after runtimes bind.
- `episode.clj`: `repair-stranded-lane!`, defunct-cell request/read, runtime
  compare-remove, and fallback filtering. `decide-episode` remains pure and
  unedited.
- NEW `test/app/episode_retry_test.clj`, P2 changes to
  `cascade_table_test.clj`, and its one fail-closed inventory classification.

P2 owns react!, all handler behavior, boot sweep, episode repair/adoption,
emission, and row activation. P1 must not prebuild any of them.

Read-only across both phases:
`material_circulation.clj`, `verb_registry.cljc`, and every OC/RK/llm module
file. No default material-policy path changes, existing durable-shape edits,
new relation kinds, or serve-contract edits.

## Gate and test plan

### P1 focused gates

- **G2m:** launch through IPC constructors; append a valid obligation with
  `:ack`; immediately point-read an obligated run and pending membership.
  Derive emission/run ids for identical input in two independent IPC clusters
  and byte-compare.
- **G3m:** cover every cascade state-table transition: duplicate obligation;
  observation before obligation; obligation after terminal; duplicate same
  terminal; both conflict directions; repeated conflict record; ordered replay
  of the same observations. Assert first terminal, exact latest conflict value,
  and pending membership after every step.
- **G4m:** leave an old obligation without observation, discard all foreign
  handles, build a fresh runtime against the same IPC, enumerate it past grace,
  append the observation with `:ack`, then prove terminal row and empty pending.
  This tests substrate only—no handler or sweep.
- **G9m:** fresh namespace require with launch/append/sweep seams stubbed proves
  zero calls. Static scan permits declarations and constructors only; no
  top-level deref, future, launch, sweep, append, TaskGlobal, or external call.
- Use authoritative PState reads, never only constructor returns or
  reconstructed ids.

The exact focused invocation is:

```text
clj -M:test -e "(require 'clojure.test 'app.cascade-log-test) (let [r (clojure.test/run-tests 'app.cascade-log-test)] (when (pos? (+ (:fail r) (:error r))) (System/exit 1)))"
```

P1 also runs `clj -X:test full` because durable touch is FULL tier, and lints
the new source/test files. P2 remains formal owner of G11 and reruns the full
exact-tree suite plus every edited-path inventory scan.

### G12 deploy-under-proof

G12 is a live P1 gate, not an IPC substitute.

1. Build `target/land-modules.jar`.
2. Attest the release/cluster identity, license/node facts, supervisor count,
   and deployed module names before mutation. On first deploy, the exact five
   existing modules must be present and RUNNING and cascade-log absent.
3. Run `bin/land deploy`. Its settle loop must observe server-read RUNNING for
   each module. Afterward, the exact sixth cascade module is present and all
   prior five still report RUNNING. CLI exit codes alone are insufficient.
4. Append a named obligation with `:ack`; capture its authoritative run and
   pending rows plus the cascade-runs topology's server-readable consumed
   positions/checkpoint.
5. TERM only the identified cascade-log worker process; do not stop the
   cluster. Observe supervisor replacement and cascade module RUNNING.
6. Reopen cluster handles in a fresh process/runtime. Prove the obligation and
   pending row are byte-identical. Capture consumed positions/checkpoint as
   equal or advanced and the server restart receipt as native checkpoint
   restore, not a fresh depot-history start. If no server-readable offset
   evidence can be obtained, G12 is not claimable and the package stops.
7. Append its observation with `:ack`; prove terminal truth and empty pending.
8. Preserve the complete before/deploy/restart/after receipt with INFO logging
   enabled.

This is one-shot-aware: first deployment destroys the “module absent”
precondition. The committed harness therefore asserts the post-package
invariant—module present, server-read RUNNING, existing truth preserved, and
clean redeploy path—without pretending to rerun the first-deploy proof.
Deploy failure, non-RUNNING state, license/node-count doubt, or inability to
prove native progress preservation is a CONTRACT §6 stop.

### P2 gate mapping carried by this plan

- G1: byte-behavior identity for row #1/best-effort.
- G2e: receipt only after obligation `:ack` and authoritative obligated read.
- G3r: duplicate/concurrent repair convergence and second same-id failure
  advances the stable marker.
- G4s: fresh runtime sweep executes an old obligation through the same runner.
- G5/G6: full stranded-lane suite and live product-path drive.
- G7/G8: additive fence and scalar/EDN/no-`:lines` payload hygiene.
- G9p: explicit boot only, grace obeyed, failed poison never swept.
- G10/G11: declaration/read grammar and full suite/scans.

## RAMA-PITFALLS VERDICT for multi-cascade R2

### 1. Event boundary — PASS

One depot record is one stream event. It begins on `hash(run-id)` and all run
plus pending mutations are colocated and atomic within that event. There is no
hidden cross-event “transaction.”

### 2. Side effects in topology — PASS

The topology performs only validation, pure transition derivation, local
reads, and PState value writes/deletes. Handler execution, futures, logging,
OC requests, and observation append live JVM-side after obligation `:ack`.

### 3. PState ownership — PASS

One stream topology owns both PStates and consumes both sources through one
`<<sources`. There is no split writer, mirrored owner, or source-specific truth
island.

### 4. Back-arrow architecture — PASS

The durable arrow points inward first: obligation depot to Rama truth. Only
after materialization does P2 call outward. Outcome returns through the
observation depot and becomes queryable Rama truth.

### 5. Unique IDs — PASS

Emission and run ids are deterministic, globally prefixed, and minted by the
emitter before append. P1 byte-pins the pure constructors across independent
runtimes. Topology retries mint nothing.

### 6. Ack level — PASS

Obligations use `:ack`, the required converge-before-call barrier.
Observations also use `:ack`, making runner completion mean terminal truth is
visible. There is no fire-and-forget append or unjustified `ack-return>`.

### 7. Stream retry/idempotency — PASS

Every PState mutation is an absolute `termval` or `NONE>` delete. No counter or
collection append exists. `:all-after` preserves source-partition order so a
retry restores the latest obligation/conflict value. Cross-depot reorder is
handled by the state law.

### 8. ProxyState misuse — N/A

No ProxyState, mirror, or external PState is used.

### 9. Subindexing and growth — PASS

Each run row is fixed and bounded. Pending is a top-level index with no growing
per-run value. Lifetime run count can grow, but each row is independently
keyed and the only full scan is explicitly administrative. No large nested
value is rewritten.

### 10. Hash partitioning — PASS

Both plain depot envelopes carry a validated top-level namespaced `:run/id`;
both depots use `(hash-by :run/id)`; both PStates use the same String key.
There is no defrecord-key mismatch and no nil funnel.

### 11. TaskGlobal durability — N/A / PASS

The module uses no TaskGlobal. The only runnable intent is the durable
obligation row plus pending membership. P2 JVM futures and atoms are allowed
operational state because crash recovery reconstructs work from the PStates.

**Overall verdict: PASS / READY FOR PHASE 2 VALIDATION.** The design is
physically buildable under the fixed contract. Residual risks are bounded and
assigned:

- P1: prove exact record serialization, generated partition-key enumeration,
  `:all-after` conflict restoration, and server-readable offset preservation.
- P2: prove best-effort byte identity, handler-result normalization, duplicate
  execution tolerance, total runner failure behavior, and the actual
  stranded-lane product path.
- Ops: first deploy is one-shot and must retain its full receipt; no later
  green redeploy can retroactively prove module-absent.

## Rama retro-lens verdict

### Physical path

```text
P2 close emitter
  -> acknowledged obligation depot
  -> cascade-runs stream topology
  -> $$cascade-runs + $$cascade-pending
  -> P2 runner reads the committed obligation
  -> resolved idempotent handler
  -> acknowledged observation depot
  -> same topology
  -> terminal $$cascade-runs + pending deletion
  -> direct run read / administrative pending and failed reads
```

The authoritative current run is `$$cascade-runs`; authoritative resumable
membership is `$$cascade-pending`. Depots are durable input history, not
current-state truth. Declaration rows are code in R2. JVM futures, atoms,
receipts returned by helpers, and log lines are not authoritative.

### Learned

- The organ, not a feature slice, owns both lifecycle truth and its recovery
  index.
- Record-before-call plus deterministic ids is the causal boundary.
- First terminal and value-replacement conflict evidence make at-least-once
  replay converge without unbounded dedupe state.
- A proof must read the durable projection and restart it; helper completion
  and IPC-only success are insufficient.
- Every read path, including administrative scans, names its physical
  partitions and I/O cost.

### Repeating

No known retro failure is repeated in the planned design: there is no
app-shaped state island, wrong depot owner, topology side effect,
non-idempotent retry write, client/cache-only truth claim, or helper-first
completion claim. The remaining deliberate sharp edge is concurrent duplicate
handler execution; the contract does not add a claimant protocol, so each
durable row must tolerate it and first-terminal truth records disagreement.

### Repair next

P1 must falsify the complete depot adversary matrix and a real worker restart
before handing off. P2 must then prove the same durable row drives the actual
repair/adoption path, including duplicate execution, execution-time file
recheck, second same-id failure, old replay non-regression, and a fresh next
turn. If either proof only shows a helper return, log line, reconstructed id,
or local atom, the package remains unproven.

