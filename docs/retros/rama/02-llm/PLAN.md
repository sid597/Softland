# Plan

<!-- Phase 1 Step 5. Fill in after completing Steps 1-4. After completing this, fill in PLAN_VALIDATION.md. -->

> **RETROSPECTIVE MODE.** Re-derived from `IMPLICIT_SPEC.md` alone (code-blind). Designed for
> production: at-least-once stream retries, worker restarts, node failures. Every write below is
> audited for correctness under replay. The executor lives OUTSIDE the module; the module-side
> pull protocol it must follow is specified at the end of "Topologies and PStates".

## Depot record types (shared vocabulary for Reads/Writes)

All records implement `definterface ILLMRecord` and carry `:run-id` (the per-run ordering scope,
spec invariant 4: routing key `[:llm-run <run-id>]`). Defined as `defrecord`s (polymorphic depot
data per skill rule — never a single fixed-keys bag):

- `TurnRunRequest` — run-id, thread-id, space-id, world-turn-id, request-id, time-ms, bundle-id
  (reference only), inbox-key, backend (`nil`→`:codex`), auth-mode, restart-policy, fork-from
  (optional native-thread ref).
- `RunClaim` — run-id, thread-id, executor-id, claim-token, claimed-at, inbox-key.
- `RunObservation` — run-id, thread-id, obs-type (`:item-completed` `:token-usage` `:tool-call`
  `:approval-request` `:run-finished`), seq (per-run Long), obs-id, received-at, payload
  (type-specific defrecord), raw-json (String, **redacted before append** — executor obligation),
  native-thread-id (optional, rides on any obs), claude-session-id (optional). `:run-finished`
  payload carries `:outcome` ∈ `:succeeded` `:failed` `:cancelled` (+ structured error) — this is
  how provider abort confirmations and provider-side errors close the run.
- `RunControl` — control-id, run-id, thread-id, control-type (`:cancel` `:steer`
  `:approval-resolve` `:compact` `:approval-timeout`), time-ms, payload (e.g. approval-id +
  native-request-id for resolve; steer text; compact args). `:approval-timeout` is appended
  internally by the timeout scheduler — making wall-clock timeouts part of appended history so
  state stays a deterministic fold (spec invariant 1).
- `StaleApprovalsMark` — run-id, executor-id, time-ms.

Item payloads are polymorphic: `definterface IRawItem` + one defrecord per item type
(message / reasoning / tool-call / tool-result), each recording `:source-backend`. Raw provider
payloads are stored as redacted JSON `String`s (the replay primitive is the literal line).

Determinism rule applied throughout: **no wall-clock reads inside any fold**. Every timestamp
written to a PState comes from the record itself (`time-ms`, `claimed-at`, `received-at`).
The only wall-clock consumer is the timeout scheduler, and it converts time into an appended
`:approval-timeout` record, so the fold of history remains deterministic (writer-asymmetry
property holds).

## Reads

All reads must be safe in every entity state (absent ids → nil/empty, never errors). `keypath`
on an absent key navigates to nil, which gives this for free on direct selects; query topologies
short-circuit on nil with `<<if`.

| # | Read | Access method | Path expression (partition) |
|---|---|---|---|
| R1 | decision by request id | `foreign-select-one` | `(keypath request-id)` on `$$decisions` (request-id partition) |
| R2 | run by run id | `foreign-select-one` | `(keypath run-id (submap :status :backend :auth-mode :native-thread-id :claude-session-id :restart-policy :last-seq :error :request-id :bundle-id :thread-id :claim ...))` on `$$runs` (run-id) |
| R2b | buffered sequences | `foreign-select` | `(keypath run-id :buffer ALL FIRST)` or bounded range on `$$runs` (run-id) — surfaces stalled gaps |
| R3 | live view (bounded) | **query topology** `live-view` | multiple reads, one PState, one partition → batched (run-id) |
| R4 | run-detail projection | **query topology** `run-detail` | multiple sections of `$$runs` (run-id), wrapped self-describing |
| R5 | thread by thread id | **query topology** `thread-view` | native id + ordered run list = 2 reads, same partition (thread-id) |
| R6 | thread binding by space | `foreign-select-one` | `(keypath space-id)` on `$$space->thread` (space-id) |
| R7 | runs-by-thread | `foreign-select` | `(keypath thread-id :runs (sorted-map-range-from k {:max-amt n}))` on `$$threads` (thread-id) |
| R8 | run-for-turn | `foreign-select-one` | `(keypath world-turn-id)` on `$$turn->run` (world-turn-id) |
| R9 | pending inbox by lane | `foreign-select` | `(keypath inbox-key (sorted-map-range-from nil {:max-amt n}))` on `$$pending-inbox` (inbox-key) |
| R10 | items by run (ordered, paged) | `foreign-select` | `(keypath run-id :items (sorted-map-range lo hi))` on `$$runs` (run-id) |
| R11 | items by thread (full history, paged) | **query topology** `thread-items` | nested range scan on `$$thread-items` (thread-id) |
| R12 | item by item id | **query topology** `item-by-id` | `$$item->run` then hop to `$$runs` — 2 PStates, 2 partitions |
| R13 | raw response items by run | `foreign-select` | `(keypath run-id :raw (sorted-map-range lo hi))` on `$$runs` (run-id) |
| R14 | tool calls by run | `foreign-select` | `(keypath run-id :tool-calls ALL)` on `$$runs` (run-id) |
| R15 | approvals by run (trail) | `foreign-select` | `(keypath run-id :approvals ALL)` on `$$runs` (run-id) |
| R16 | pending approval by approval id | **query topology** `approval-by-id` | `$$approval->run` then hop to `$$runs :approvals` — returns row only if `:status = :pending` (carries native JSON-RPC request id) |
| R17 | token usage by run | `foreign-select-one` | `(keypath run-id :usage)` on `$$runs` (run-id) |
| R18 | cost rollup by thread | **query topology** `cost-rollup` | totals + per-run page + run-count on `$$thread-costs` (thread-id), self-describing wrapper |
| R19 | control by control id | **query topology** `control-by-id` | `$$control->run` then hop to `$$runs :controls` |
| R20 | controls for a run (executor poll) | `foreign-select-one` | `(keypath run-id (submap :status :controls))` on `$$runs` (run-id) |
| R21 | claim state | client-side pure fn | over R2's `:status` + `:claim` + caller's token → `:pending` / `:granted-to-us` / `:granted-to-other` (spec: "pure function") |
| R22 | active runs by executor (restart recovery) | `foreign-select` | `(keypath executor-id ALL)` on `$$executor-active-runs` (executor-id) |
| R23 | orphaned records by run id (ops surface) | `foreign-select` | `(keypath run-id ALL)` on `$$orphaned-records` (run-id) |

Reads R3/R4/R12/R16/R18/R19 need >1 PState read or >1 read on one PState → query topologies
(client-side multi-select is an anti-pattern). R5/R11 batch same-partition multi-reads. Everything
else is a single path on a single partition → direct foreign select.

Latency: all point reads are 1–2 RocksDB seeks (≈0.5–1 ms) + network — well inside "low tens of
ms". The hot UI read (live view) is one partition, ~3 seeks + one bounded range iteration.

## Writes

One depot append encodes all side effects of an operation (single-append rule). All five write
operations append to **one depot** `*llm-records` (per-run total order requires all record types
about one run to be serialized on one partition — spec invariant 4; see Depots).

| Write | Record | Appender | Ack level |
|---|---|---|---|
| W1 submit turn-run request | `TurnRunRequest` | world-side bridge | `:ack` — `ack-return>` carries the decision; "the UI awaits the decision" |
| W2 claim pending run | `RunClaim` | executor | `:ack` — `ack-return>` carries grant result; the durable grant the executor awaits |
| W3 streamed observation | `RunObservation` | executor | `:append-ack` for ordinary deltas (throughput); `:ack` for gating observations (fork-completed / native-id discovery) where the executor must see the binding before proceeding |
| W4 control | `RunControl` | world-side bridge (derived records; UI never writes directly); `:approval-timeout` appended internally by the scheduler | `:ack` (bridge) / `:append-ack` (internal, same-stream deadlock rule) |
| W5 mark stale approvals | `StaleApprovalsMark` | executor (crash/restart cleanup) | `:ack` — `ack-return>` carries {stale-ids, action, policy} |

Throughput: W3 dominates (10–100 ev/s per active run × concurrent runs); W1/W2/W4/W5 are
human-paced/rare. The W3 fold is designed to be a **single-task event with zero partitioner hops**
in the common case (see topology section) so sustained streaming neither starves reads nor pays
network latency per token.

## PState Design

Worked backwards from the reads. Partitioning: everything per-run lives under the run-id key on
the depot's own partition (zero-hop hot path); per-thread, per-space, per-turn, per-lane,
per-executor, per-item, per-approval, per-control state is keyed and partitioned by its own
lookup id because clients query by exactly that id.

### `$$runs` — the run row (canonical per-run state + trail) — partitioned by run-id

```
{run-id<String> (fixed-keys-schema
   {:status            clojure.lang.Keyword   ; :pending :claimed :blocked-awaiting-approval
                                              ; :cancel-requested :succeeded :failed :cancelled
    :request-id        String
    :space-id          String
    :world-turn-id     String
    :thread-id         String
    :bundle-id         String                 ; reference only — kernel never loads content
    :inbox-key         String
    :backend           clojure.lang.Keyword
    :auth-mode         clojure.lang.Keyword
    :restart-policy    clojure.lang.Keyword   ; e.g. :fail-on-stale-approval
    :time-ms           Long                   ; request time — deterministic clock for this run
    :thread-ordinal    Long                   ; position in the thread's run list
    :fork-from         String
    :native-thread-id  String                 ; inherited at acceptance, or set on discovery
    :claude-session-id String
    :claim             (fixed-keys-schema {:executor-id String :token String :claimed-at Long})
    :last-seq          Long                   ; folded progress; initial sentinel -1
    :buffer            (map-schema Long RunObservation {:subindex? true})   ; out-of-order obs
    :items             (map-schema Long IRawItem {:subindex? true})         ; seq → item
    :raw               (map-schema Long String  {:subindex? true})          ; seq → redacted JSON
    :tool-calls        (map-schema String ToolCallRow {:subindex? true})    ; tool-call-id → row
    :approvals         (map-schema String ApprovalRow {:subindex? true})    ; approval-id → row
    :controls          (map-schema String ControlRow)                       ; control-id → row
    :usage             UsageRow
    :error             (fixed-keys-schema {:reason clojure.lang.Keyword
                                           :decision clojure.lang.Keyword
                                           :message String})})}
```

`ApprovalRow` (defrecord): approval-id, native-request-id, status
(`:pending`/`:approved`/`:declined`/`:expired`), requested-seq, resolved-by (control-id),
reason (`:executor-stale` / `:timeout` / `:run-closed` / nil), time-ms.
`ToolCallRow` (defrecord): tool-call-id, type, name, status, seq.
`ControlRow` (defrecord): control-id, control-type, time-ms, payload, effect (what the fold did).
`UsageRow` (fixed-keys): `:input-total :cached-input :output :reasoning-output :context-window
:messages-used Long` each, `:billing-mode clojure.lang.Keyword`, `:seq Long` (latest report's
seq — guards stale re-reports). Separate fields by spec (honest cost accounting).

**Why one PState, not many**: items/raw/tools/approvals/usage/status all share the run-id key and
partition and are read together by `live-view`/`run-detail`. One fixed-keys row with subindexed
inner maps gives single-seek field access, atomic single-event folds (one task owns everything the
fold touches), and avoids the "many PStates, same key" anti-pattern.

Alternatives considered for items-by-run: (A) separate `$$items {run-id {seq item}}` PState —
same partition, same cost, +1 PState with no read benefit; (B) inside the run row (chosen) —
identical I/O (subindexed inner map = own RocksDB entries), simpler queries. Chosen B.

Read cost (live view, run with 1k items, window 200): 1 seek (row fields submap) + 1 seek
(usage) + 1 seek + 200 iter (items range) ≈ 1.5–2 ms. Point lookups: 1 seek.

### `$$decisions` — request decision trace — partitioned by request-id

```
{request-id<String> (fixed-keys-schema
   {:status Keyword :reason clojure.lang.Keyword
    :event-ids (vector-schema String)         ; derived deterministically from request-id
    :routing-scope clojure.lang.Keyword :routing-id String   ; echoes [:llm-run <run-id>]
    :decided-at Long                          ; = request's time-ms (replay determinism)
    :run-id String})}
```

Keyed by request-id because R1 queries by request-id. Decision content is a **pure function of
the request (+ acceptance outcome)**, so rewriting it on replay/duplicate is byte-identical —
"never two decisions for one request id" holds as: one key, idempotently rewritten with the same
value. `decided-at` derives from `time-ms`, never the wall clock. Rejected requests get a row
here and nothing anywhere else (Ambiguity #1 resolved: record kernel-side rejection rows per
contract invariant I16 — cheap, and "rejected requests remain queryable as decisions" demands it).

### `$$threads` — thread row — partitioned by thread-id

```
{thread-id<String> (fixed-keys-schema
   {:native-thread-id   String
    :native-conflicts   (map-schema String Long {:subindex? true})  ; conflicting-id → first time-ms
    :runs               (map-schema Long String {:subindex? true})  ; ordinal → run-id (ordered)
    :run-ordinals       (map-schema String Long {:subindex? true})  ; run-id → ordinal (dedup guard)
    :next-ordinal       Long
    :compaction-markers (map-schema String CompactionMarker {:subindex? true})})}  ; control-id →
```

`:runs` keyed by ordinal gives ordered render via one range scan (1 seek + N iter), and
`:run-ordinals` is the existence guard that makes ordinal assignment idempotent under stream
retry (see topology audit). Alternative — `(vector-schema String {:subindex? true})` with
`AFTER-ELEM` — rejected: AFTER-ELEM is non-idempotent under stream retry; the guarded
ordinal-map costs one extra subindexed point read per request (requests are human-paced).

### `$$space->thread` — `{space-id<String> String}` — partitioned by space-id
### `$$turn->run` — `{world-turn-id<String> String}` — partitioned by world-turn-id

Obvious single-key lookups (R6, R8); termval writes, idempotent.

### `$$pending-inbox` — `{inbox-key<String> (map-schema String Long {:subindex? true})}` — by lane

run-id → enqueued-at (request `time-ms`, deterministic). Sorted + subindexed: discovery is one
range scan; a backlogged lane can exceed 100 entries. Empty/unknown lane → empty map → empty read.

### `$$executor-active-runs` — `{executor-id<String> (map-schema String Long {:subindex? true})}`

run-id → claimed-at. Added on grant, removed on terminal fold. **This is the executor-restart
discovery index** required by spec Op 6 ("executor restart mid-run → resume or fail per recorded
policy"): a restarted executor with no memory reads its own key, then per run verifies
`granted-to-us` via R2 and runs close-out (failure observation and/or stale protocol). Without
this index a crashed executor can never find its orphaned claims and `:claimed` runs leak forever.

### `$$orphaned-records` — `{run-id<String> (map-schema String ILLMRecord {:subindex? true})}`

record-key (obs-id / control-id) → record. Resolves Ambiguity #2's "never silently lost" vs
"never invent a run": observations/controls for unknown run ids fold here (durable, queryable by
ops) and create NO run row. Not auto-replayed into runs (out-of-protocol arrivals indicate a bug;
surfacing beats guessing).

### `$$thread-items` — full-history item index — partitioned by thread-id

```
{thread-id<String> (map-schema Long                                  ; run thread-ordinal
                     (map-schema Long IRawItem {:subindex? true})    ; seq → item (full copy)
                     {:subindex? true})}
```

Options for "ordered scan of a thread's items (full history)" — a spec-dominant access pattern:
- **Option A (chosen)**: materialized nested index, full item copies. Read: page of recent runs =
  reverse range over ordinals (1 seek + R iter) + per rendered run one inner range scan (1 seek +
  items iter). A 20-run page ≈ 21 seeks ≈ 10 ms, no network hops. Write cost: one extra
  microbatch-side write per item (amortized, off the latency path). Storage doubles for items —
  accepted: items are immutable so no consistency risk, and seeks are the scarce resource, not disk.
- Option B: store `{run-id seq}` refs, join at read → every history render fans out one network
  hop + seek per run to the run partitions; R unbounded over thread lifetime. Rejected: pays per
  read forever, and the spec explicitly names a "full history index".
- Option C: single-level map keyed by encoded `(ordinal << 32 | seq)` Long — 1 seek + N iter,
  cheapest read, but depends on serialized-Long lexicographic order matching numeric order and
  needs key-encoding tricks; nested form keeps per-run access free and is unambiguous. Rejected.

### `$$item->run` — `{item-id<String> (fixed-keys-schema {:run-id String :seq Long})}` — by item-id

Back-pointer only (no third copy of content); R12 joins to `$$runs` in a query topology (2 seeks).

### `$$approval->run` — `{approval-id<String> String}` — by approval-id
### `$$control->run` — `{control-id<String> String}` — by control-id

Global-id → run-id back-pointers for R16/R19 joins. Canonical approval/control rows stay on the
run row (atomic with status transitions); these indexes lag ≤ one microbatch (audit/UI lookups,
not on the resolve path — the resolve control carries run-id + approval-id + native request id
copied by the bridge from the pending row, so correctness never depends on these indexes).

### `$$thread-costs` — cost rollup — partitioned by thread-id

```
{thread-id<String> (fixed-keys-schema
   {:totals  UsageTotals                                       ; summed separate fields
    :per-run (map-schema String UsageRow {:subindex? true})    ; run-id → latest usage
    :run-count Long})}
```

`UsageTotals` (fixed-keys): same separate Long fields as `UsageRow` (input-total, cached-input,
output, reasoning-output, messages-used). Totals updated by **delta against the stored per-run
entry** in the same event (read old, write new, adjust totals atomically on one task) → re-reports
never double-count; run-count increments only on nil→present transition. Derived, never source
truth: rebuildable by re-folding canonical per-run `$$runs :usage` (repair rule in spec); the
rollup read declares `{:derived-from [:runs :usage]}`.

No `Object` appears anywhere; polymorphic positions use `definterface` + `defrecord`
(`ILLMRecord`, `IRawItem`); raw payloads are `String`.

## Depots

- `*llm-records` — `(hash-by :run-id)` — record types: `TurnRunRequest`, `RunClaim`,
  `RunObservation`, `RunControl`, `StaleApprovalsMark` (dispatched with `<<subsource`).
  **One depot, not five**: spec invariant 4 requires all state transitions for one run to be
  serialized with the run as ordering scope. Same partition key in separate depots gives NO
  cross-depot ordering — a cancel could fold before its run's request. One depot + `hash-by
  :run-id` gives true per-run total order mechanically. Appended by the world bridge (requests,
  controls), the executor (claims, observations, stale-marks), and internally by the timeout
  scheduler (`:approval-timeout` controls via `depot-partition-append!`). Partitioning by run-id
  colocates depot processing with `$$runs` — the dominant write (observations) is zero-hop.
  Stream source options: `{:retry-mode :individual}` — safe because the per-run seq buffer
  absorbs any reordering a retried record causes (an obs that overtakes a retrying predecessor
  just buffers until the gap fills), and all folds are guarded/idempotent.
- `*timeout-tick` — `(declare-tick-depot setup *timeout-tick 30000)` — drives
  `TopologyScheduler.handleExpirations` for approval timeouts. 30 s granularity is ample for a
  minutes-scale timeout policy.

## Topologies and PStates

Two ETL topologies. PState symbols are module-scoped (any topology/query can read all of them);
ownership below is write-ownership.

### `run-fold` — **stream** topology

Why stream (all three skill criteria for stream apply):
1. `foreign-append!` must block until PState changes are visible — the UI awaits the decision
   (W1), the executor awaits the durable grant (W2) and the fork-binding visibility gate (W3
   gating obs), and the stale protocol returns its result (W5).
2. Values computed during processing return to the caller via `ack-return>` (decision; grant
   3-state; {stale-ids, action, policy}).
3. Latency: observation fold-to-queryable must be tens of ms ("feel like streaming", ≤200 ms
   end-to-end). Microbatch's ≥300 ms floor fails this.

Owns: `$$runs`, `$$decisions`, `$$threads`, `$$space->thread`, `$$turn->run`, `$$pending-inbox`,
`$$executor-active-runs`, `$$orphaned-records`, and the `TopologyScheduler` PStates
(`$$approval-timeouts`). The scheduler lives in this topology because `.scheduleItem` (on
approval-request fold) and `.handleExpirations` must share a topology; its expiration block runs
sequentially on the run task and reaches its end exactly once (stream constraint satisfied — it
only does a commit-boundary `(|direct (ops/current-task-id))` + `depot-partition-append!` of an
`:approval-timeout` control with `:append-ack`; `:ack` would deadlock since this same stream
topology consumes `*llm-records`).

Event flows and **per-write idempotency audit** (stream retries replay the whole event tree;
writes committed at partitioner boundaries on earlier tasks are NOT rolled back — every write
must tolerate re-execution, and no hop may depend on state consumed by a previous attempt):

**TurnRunRequest** (on run task → thread task → run task → lane task → turn task → space task →
request-id task):
1. Run task: validate (pure pre-check: execution options smuggled in payload →
   `:payload/execution-options-not-bundle-owned`; `:passive-observe` → reject; missing backend →
   normalize `:codex`). Duplicate gate: run row exists → recompute the (deterministic) decision,
   `ack-return>` it, and still traverse the remaining hops (all idempotent; see each). Rejected →
   skip to decision hop with a rejection row.
2. `|hash thread-id`: read `:native-thread-id`; ordinal assignment **guarded** — if
   `:run-ordinals run-id` exists, reuse it (idempotent); else ordinal := `:next-ordinal`
   (nil→0), write `:runs{ordinal}=run-id`, `:run-ordinals{run-id}=ordinal`, `:next-ordinal`
   +1 — read-check-then-write is atomic on the single-threaded task, so retry/duplicate replay
   reuses the stored ordinal and never double-appends. Idempotent.
3. `|hash run-id`: write the full run row with `termval`, **guarded by `:status` nil** — never
   clobbers a row that later records (claim/obs) already advanced; carries inherited
   `:native-thread-id` + `:thread-ordinal` + `:pending`. Idempotent.
4. `|hash inbox-key`: inbox add **guarded by the run-task status check** (only hop here when
   status was `:pending`/absent at step 3) — prevents a retried request from resurrecting an
   inbox entry after a claim removed it. Map-assoc keyed by run-id → no duplicate entry.
   Idempotent. (Race note: the re-add-vs-removal order is safe because both the request retry and
   the claim are serialized on the run task first, and task-to-task event delivery preserves
   per-sender order.)
5. `|hash world-turn-id`: `$$turn->run` termval. Idempotent. 6. `|hash space-id`:
   `$$space->thread` termval (records what the world asserts; cardinality policy is upstream —
   Ambiguity #10). Idempotent. 7. `|hash request-id`: `$$decisions` termval — value is a pure
   function of the request, so rewrite is byte-identical. Idempotent.
   `ack-return>` the decision (fires once per event tree).

Acceptance visibility: by the time `:ack` returns to the bridge, the ENTIRE event tree has
committed — run row `:pending`, thread run list, inbox entry, turn→run, space→thread are all
visible together to anyone the bridge then notifies. Within the tree, hops commit a few ms apart;
the only observer that could race it is an executor polling the inbox, and inbox-add commits
*after* the run row (hop order), so discovery implies the run row exists. Strict cross-partition
atomicity would require microbatch, which conflicts with ack-return and single-owner status
(documented deviation: the observable contract — nothing discoverable before it is claimable,
everything visible at ack — is preserved).

**RunClaim** (run task → executor task → lane task):
1. Run task: grant iff `:status = :pending`, or the stored token already equals this claim's
   token (winner replay → still granted-to-us). Write `:claim` + `:status :claimed` (termvals).
   Loser (status ≠ pending, different token) / unknown run / terminal run: NO write — a claim
   never invents a run, never regresses a state. `ack-return>` the 3-state result. Idempotent:
   token equality makes the winner's retry a no-op rewrite; losers write nothing.
2. (granted only) `|hash executor-id`: `$$executor-active-runs` assoc run-id → claimed-at.
   Idempotent set-add.
3. (granted only) `|hash inbox-key`: dissoc run-id. Idempotent removal (absent → no-op).
   Partial-failure window (grant committed, removal not yet): a second executor can still
   *discover* the run, but its claim hits the run-task arbiter and gets `granted-to-other`;
   retry completes the removal. Exactly-one-winner is enforced at the single serialization
   point, never by the inbox.

**RunObservation** (run task; rare conditional hop to thread task):
1. Run task: run absent → `$$orphaned-records` termval keyed obs-id (Ambiguity #2), stop.
2. Sequencing: `seq ≤ :last-seq` → duplicate delivery (Ambiguity #4): re-apply the obs's own
   writes idempotently (below) and stop — no double-fold possible because every write is keyed
   or guarded. `seq > :last-seq + 1` → buffer termval keyed seq (idempotent), `:last-seq`
   unchanged, no materialization. `seq = :last-seq + 1` → apply, then drain the buffer's
   contiguous prefix in seq order (sorted range scan from `:last-seq + 1`, `yield-if-overtime`
   for storm-sized drains), removing drained entries; `:last-seq` := highest contiguous
   (monotonic max — idempotent). Buffered-never-dropped, drains in order, buffer empty after.
3. Apply (per obs type, all on the run task, all idempotent):
   - `item-completed`: `:items{seq}` / `:raw{seq}` termval (immutable, keyed by seq — replay
     rewrites same value). Records source backend.
   - `token-usage`: guarded by `UsageRow :seq` (apply only if newer) → `:usage` termval.
   - `tool-call`: `:tool-calls{tool-call-id}` termval.
   - `approval-request`: `:approvals{approval-id}` **write-only-if-absent** (a replay must not
     reset a resolved approval to `:pending`); status → `:blocked-awaiting-approval` only if
     that approval is (still) pending; `.scheduleItem` the timeout (run-id, approval-id,
     `received-at + policy-ms`). `.scheduleItem` is not idempotent — a retry can double-schedule;
     harmless: the expiry handler is check-pending-first, so the dud fires and no-ops.
   - `run-finished`: status := outcome (`:succeeded`/`:failed`/`:cancelled`) **only if not
     already terminal** (sticky — cancel-vs-finish races are serialized on the run task; first
     fold wins, the later record is retained in the trail without regressing status). Any
     still-`:pending` approvals → `:expired`, reason `:run-closed` (Ambiguity #5: a closed run's
     native request ids are unanswerable; trail retained). Hop `|hash executor-id` → remove from
     `$$executor-active-runs` (idempotent dissoc; executor-id read from the locally-stored claim).
   - Late obs on a terminal run (Ambiguity #2): trail keeps folding (items/usage/tools recorded —
     never delete, never drop), status sticky.
   - Status transitions are a pure sticky fold `f(status, record)` — deterministic, replay-stable.
4. (only if the obs carries a native thread/session id) `|hash thread-id`:
   `:native-thread-id` **set-if-absent**; same id re-asserted → no-op; conflicting id →
   `:native-conflicts` keyed by the conflicting id, binding never silently flips (Ambiguity #8:
   first-write-wins + recorded anomaly). Claude session id → run row (step 3, local). This hop is
   driven by data on the record itself, so a retry re-executes it even when the dedup gate says
   "already folded" — cross-partition hops in this topology are never derived from drain output,
   which is consumed on first fold. Idempotent.
   The fork gate works because the executor appends fork-completed with `:ack`: ack returns only
   after this hop commits, so "binding visible before first turn on the new native thread" holds
   even if the executor crashes and re-reads.

**RunControl** (run task; conditional hops):
1. Run task: run absent → `$$orphaned-records` (recorded, nothing invented — Ambiguity #3 for
   unknown runs), stop. Else `:controls{control-id}` termval (idempotent; duplicate replay
   rewrites; first resolution wins because effects below are guarded).
2. By type (all guarded, all sticky-terminal-safe — a control on a terminal run is recorded but
   never regresses status):
   - `:cancel` × `:claimed`/`:blocked`: status := `:cancel-requested` (truth transitions to
     `:cancelled` only on the provider's abort-confirmation `run-finished {:outcome :cancelled}`).
     Executor sees the marker by polling R20 within its ≤1 s cadence.
   - `:cancel` × `:pending` (never claimed — Ambiguity #3): no executor exists to confirm; the
     kernel closes out directly: status := `:cancelled`, hop `|hash inbox-key` (read from the
     row) → dissoc (no longer claimable; idempotent). A claim racing this cancel is serialized
     on the run task: cancel-first → claim sees terminal, not granted; claim-first → normal
     two-phase cancel.
   - `:approval-resolve`: approval row must be `:pending` → status := `:approved`/`:declined`,
     `:resolved-by` control-id; recompute: if no `:pending` approvals remain and status =
     `:blocked-awaiting-approval` → status := `:claimed` (unblocked). Already
     resolved/expired → no-op on the row (no resurrection), the late attempt stays recorded in
     `:controls`. Unknown approval id → no-op. The control carries the native JSON-RPC request
     id (copied by the bridge from the pending row) — the executor replies to the provider from
     the control record, never inferring the mapping.
   - `:approval-timeout` (internal, from scheduler): same as resolve but outcome `:expired`,
     reason `:timeout`, then restart policy applies (as stale-marking below).
   - `:steer` (Ambiguity #7): recorded in `:controls`; deliverable only while `:claimed` —
     the executor polls R20 and injects; in any other state it is a recorded no-op.
   - `:compact`: hop `|hash thread-id` → `:compaction-markers{control-id}` termval. Kernel trail
     (items/raw/usage/approvals) retains everything pre-compaction — nothing is deleted, future
     bundles can cite pre-compaction items.
3. "Pending approval row" reads (R16) are the approval rows filtered to `:status = :pending` —
   clearing the pending row = flipping status, so the full history stays on the trail forever.

**StaleApprovalsMark** (run task; conditional hops):
1. Range-scan `:approvals` for `:pending` (small; subindexed). For each: status := `:expired`,
   reason `:executor-stale`, and a synthetic `ControlRow` keyed by the **derived control id**
   `"<run-id>/<approval-id>/stale"` (deterministic — no minting). Then the run follows its
   recorded `:restart-policy`: `:fail-on-stale-approval` → status := `:failed` (only if not
   already terminal — approvals are cleaned on terminal runs but status never regresses) +
   structured `:error {:reason :approval-declined :decision :expired}`; hops: `|hash executor-id`
   dissoc active-runs; `|hash inbox-key` dissoc (covers the evidenced approval-on-unclaimed-run
   case). All guarded/idempotent.
2. `ack-return>` {stale-approval-ids, action, policy}. Re-invocation finds nothing pending →
   returns empty, does not double-fail the run (spec-required idempotency; a retried invocation
   legitimately returns the empty shape).

Stream-write summary: **every PState write in `run-fold` is idempotent** — `termval` at
deterministic keys (seq, obs-id, control-id, tool-call-id, approval-id, request-id, ordinal),
write-if-absent guards (run row, approval rows, native id, ordinal assignment), monotonic max
(`:last-seq`), guarded sticky status folds, and idempotent map assoc/dissoc (inbox,
active-runs). The two non-idempotent primitives in the design were eliminated structurally:
list-append → guarded ordinal map; counter `+= ` → delta-from-stored / nil→present guards.
`.scheduleItem` double-scheduling is tolerated by a check-pending expiry handler.

### `trail-indexes` — **microbatch** topology

Sources `*llm-records` independently (`{:start-from :beginning}`). Owns the derived cross-key
indexes: `$$thread-items`, `$$item->run`, `$$approval->run`, `$$control->run`, `$$thread-costs`.

Why microbatch: (1) none of these are on an ack-coordination or tens-of-ms path — they serve
render/audit lookups that tolerate ~300–600 ms (the live per-run view is entirely stream-side);
(2) exactly-once makes the rollup arithmetic and fan-out writes trivially replay-safe;
(3) keeping these hops out of `run-fold` keeps the dominant write (observations) a zero-hop
single-task event — trading microbatch lag on cold indexes for maximum hot-path throughput.

Flows (each starts on the run-id partition where the record lands):
- `item-completed`: read `$$runs (submap :thread-id :thread-ordinal)` locally (safe: causality —
  the executor appends observations only after the grant, which is after acceptance committed the
  run row in the stream topology, so the microbatch always sees it) → `|hash thread-id` →
  `$$thread-items {ordinal {seq item}}` termval → `|hash item-id` → `$$item->run` termval.
- `token-usage`: read `:thread-id` locally → `|hash thread-id` → `$$thread-costs`: read stored
  per-run entry; apply only if `:seq` newer; totals += (new − old) field-wise; `:run-count` +1
  only on nil→present. Exactly-once microbatch + same-task read-modify-write → no double count
  ever, even on re-report (delta) or replay (exactly-once).
- `approval-request`: `|hash approval-id` → `$$approval->run` termval.
- `RunControl`: `|hash control-id` → `$$control->run` termval.

### Executor pull protocol (module-side mechanisms for an OUT-of-module executor)

The executor never mutates kernel state directly — everything it learns enters as appends
(back-arrow rule). The module exposes exactly these mechanisms:

1. **Work discovery**: poll R9 (`$$pending-inbox` range scan on its lane key) at sub-second
   cadence. Discovery implies the run row is visible (hop ordering in acceptance).
2. **Claim/lock**: append `RunClaim` with `:ack`; the `ack-return>` IS the durable 3-state grant
   (`granted-to-us` / `granted-to-other` / no-run). Spawn only after `granted-to-us`
   (`:spawned-after-grant?`). Crash between append and reading the ack: on restart, R22
   (`$$executor-active-runs`) + R2/R21 (token comparison) recover the grant durably — the grant
   never lives only in an ack message.
3. **Bundle**: the run row hands out `:bundle-id` (reference only); content loads through the
   external loader; execution options are bundle-owned. If the run row carries no
   `:native-thread-id` on a thread that should have one, the executor re-checks R5 at claim time
   (inheritance at acceptance is a snapshot; the bridge's await-binding-before-follow-up protocol
   makes the race practically empty, and the kernel never silently writes a stale id).
4. **Observation ingestion**: append `RunObservation`s with executor-assigned contiguous per-run
   seqs. Redelivery is safe (seq dedup); reordering is safe (buffer); crash-and-resend is safe
   (idempotent folds). Gating observations (fork-completed / native-id discovery) use `:ack` so
   the binding is visible before the first turn on the new native thread.
5. **Control delivery**: poll R20 (one point read: `:status` + `:controls`) at ≤1 s cadence;
   `:cancel-requested` in status is the interrupt marker; controls are keyed by control-id and
   few, so the executor diffs against its in-memory seen-set — after a restart the provider
   process is gone, so undelivered steers are moot and cancel/resolve handling restarts from
   durable state.
6. **Crash/restart**: read R22 for its executor-id → for each run, verify token via R21 → append
   `StaleApprovalsMark` (`:ack`, returns {stale-ids, action, policy}) and/or a
   `run-finished {:outcome :failed}` observation per the run's recorded `:restart-policy`. A new
   provider process never answers an old native request id — stale-marking expires them durably.

## Query Topologies

All are single-result reads; absent entities short-circuit via `<<if` on the first (point) read —
no wasted seeks are padded.

- **`live-view`** [run-id, window-n] — bounded stream view (Ambiguity #6: window = last N items,
  default 200, with `{:truncated? true :total-items (view count)}` marker).
  Input run with 1k items: 4 total reads (row submap, usage, item count, items reverse range), 4
  meaningful. Input absent run: 1 total read (row submap → nil), 1 meaningful → emit nil view.
  **Variable** → conditional branch (`<<if` on row presence); the items read is a single
  `sorted-map-range-to` (1 seek + N iter), never per-item point reads.
- **`run-detail`** [run-id] — self-describing projection `{:projection/type :llm/run-detail
  :derived-from [:$$runs] :data …}` assembling row + bounded sections. Same shape as live-view:
  present → 4–6 reads all meaningful; absent → 1. **Variable** → `<<if` short-circuit.
- **`thread-view`** [thread-id] — native id + ordered run page: 2 reads same partition (submap +
  `:runs` range). Absent thread → 1 read. **Variable** → `<<if`. (Batches what would otherwise
  be 2 client roundtrips.)
- **`thread-items`** [thread-id, cursor, page-runs] — reverse range over `:runs` ordinals of
  `$$thread-items` (1 seek) then per returned ordinal one inner range scan. 20-run page → 21
  reads, all meaningful (ordinals come from the outer scan, so inner scans never miss). Empty
  thread → 1 read. **Variable** — inner reads are driven by `ops/explode` over the outer scan's
  actual results + `aggs/+vec-agg` (only reads that exist are issued).
- **`item-by-id`** [item-id] — `$$item->run` point read; hit → `|hash run-id` → `(keypath run-id
  :items seq)` (+ `:raw` if requested). Present: 2–3 reads, all meaningful. Absent: 1 read.
  **Variable** → `<<if` on index hit (no second seek on miss).
- **`approval-by-id`** [approval-id] — same join shape via `$$approval->run` → `:approvals` row
  (nil unless `:pending` for the pending-row read; full row for trail reads). 2 reads / 1 on
  miss. **Variable** → `<<if`.
- **`control-by-id`** [control-id] — same shape via `$$control->run`. 2 / 1. **Variable** → `<<if`.
- **`cost-rollup`** [thread-id] — totals (1 read) + run-count + per-run page (1 range scan),
  wrapped self-describing with `:derived-from [:runs :usage]`. Present: 3 reads meaningful;
  absent: 1. **Variable** → `<<if`.

## Design Decisions

**Subindexing** (worst-case reasoning, not typical-case): `:items` `:raw` (hundreds–thousands per
run — dominant data), `:buffer` (redelivery storms), `:tool-calls` (agent loops are unbounded),
`:approvals` (pathological approval loops), `$$threads :runs`/`:run-ordinals` (run lists grow
unbounded over thread lifetime), `:native-conflicts` (runaway-executor pathology),
`:compaction-markers` (long-lived threads), `$$pending-inbox` lanes (backlog), 
`$$executor-active-runs` (fork fan-out bursts), `$$thread-items` (both levels — full history),
`$$thread-costs :per-run` (unbounded runs/thread), `$$orphaned-records` (bug storms).
NOT subindexed: `:controls` ("a handful per run at most" — writer is human-action-derived plus
timeout records bounded by approvals; the executor poll reads the whole map each second, so a
single-value read beats seek+iterate), `:claim`/`:usage`/`:error` (fixed-size records),
`$$decisions`/`$$turn->run`/`$$space->thread`/`$$item->run`/`$$approval->run`/`$$control->run`
values (scalars/records).

**Colocation**: `*llm-records (hash-by :run-id)` colocates the depot with `$$runs` — the
dominant write (observations, 10–100/s/run) is a zero-hop single-task event. Requests/claims/
controls hop to thread/lane/turn/space/executor/request partitions, but those records are
human-paced. Per-thread state (`$$threads`, `$$thread-items`, `$$thread-costs`) shares the
thread-id key so thread reads are single-partition.

**Ordering**: one depot for all record types = per-run total order (invariant 4). Cross-run /
cross-thread ordering is not promised mechanically; the bridge/executor ack-await causality
(claim only after pending visible; obs only after grant; follow-up only after decision/binding)
closes the remaining races, and every fold is safe (recorded, no state invented) if a record
arrives out of causal order anyway. Thread run-list order is arrival order at the thread
partition — matches arrival order for human-paced sends.

**Ambiguity resolutions** (spec flags 11; decision + reasoning):
1. Kernel-side rejection rows: **yes** — `$$decisions` row with reason + empty event-ids
   (contract I16; "rejected requests remain queryable" requires a queryable row).
2. Late/orphaned observations & unfilled gaps: terminal-run obs keep folding into the trail with
   sticky status (never delete + never drop both hold); unknown-run records go to
   `$$orphaned-records` (durable, ops-queryable, no run invented); stalled gaps are surfaced by
   R2b (buffered seqs + last-seq) — no auto-close, correctness beats guessing.
3. Cancel of never-claimed run: kernel closes out directly (`:pending` → `:cancelled`) and
   removes the inbox entry; the run-task serialization point arbitrates claim-vs-cancel races.
4. Duplicate observation delivery: seq-based dedup + all-writes-idempotent (keyed termvals,
   guards, monotonic max, delta-from-stored) — same seq can never double-fold.
5. run-finished with unresolved approvals: close per outcome; remaining pending approvals →
   `:expired` reason `:run-closed` (their native request ids are unanswerable once the process
   ends); trail retained.
6. View boundedness: last-K window (default 200, parameter) + truncation marker + total count in
   `live-view`.
7. Steer: always recorded; deliverable only while `:claimed`; recorded no-op otherwise.
8. Conflicting native thread ids: first-write-wins, conflict recorded in `:native-conflicts`,
   binding never silently flips.
9. Send idempotency: kernel-side dedup scope is run-id existence (decision is a pure function of
   the request → identical replay shape); request-id↔run-id binding consistency is the world
   bridge's contract (a same-request-id/different-run-id collision routes to a different
   partition and is upstream misuse).
10. Thread cardinality: kernel records the asserted space→thread binding last-write-wins;
    one-thread-per-space-per-agent-kind policy stays upstream where the spec leaves it open.
11. `:passive-observe`: rejected at validation for active runs (durable rejected decision); its
    legitimate consumer is outside this module.

**Retry/restart survival summary**: claims survive because the grant is a guarded single-task
arbitration keyed by token (replays re-grant identically, losers write nothing) and is recoverable
from durable state (R22 + R2), never only from an ack. Observations survive because sequencing,
buffering, and every materialization are idempotent folds of the record against stored state, and
the only cross-partition hop (native id) is derived from the record itself, so a partial apply
replays to completion. Controls survive because effects are guarded by current row state (sticky
terminals, pending-only resolves) and the record itself is keyed by control-id. Worker restart:
PStates are durable storage (no depot replay on restart); both topologies resume from persisted
offsets; the scheduler's queue is PState-backed; there is no in-memory module state to rebuild.

## State primitive selection

- `$$runs` (PState): O(1) writes per source record (a few keyed entries + row fields; drains are
  bounded by previously-buffered records). Durable — source of truth for runs and trails.
- `$$decisions`, `$$turn->run`, `$$space->thread` (PStates): O(1) per request. Durable —
  decision/identity truth, queryable forever.
- `$$threads` (PState): O(1) per request / native-discovery / compact. Durable truth.
- `$$pending-inbox`, `$$executor-active-runs` (PStates): O(1) add/remove per request/claim/
  terminal. Durable **deliberately** — these are the crash-recovery surface for the external
  executor (discovery after kernel restart; claim recovery after executor restart). An in-memory
  primitive here would break the pull protocol on worker restart. (Their removals are operational
  queue semantics, spec'd — the trail equivalents live on the run row.)
- `$$orphaned-records` (PState): O(1) per orphan. Durable — "never silently lost".
- `$$thread-items`, `$$item->run`, `$$approval->run`, `$$control->run`, `$$thread-costs`
  (PStates): O(1) per observation/control — write volume bounded by the input stream the
  application already carries. Durable derived views; `$$thread-costs` declares its canonical
  source (`$$runs :usage`) and is rebuildable from it (spec repair rule).
- Scheduler PStates (`$$approval-timeouts`, via `TopologyScheduler`): O(1) per approval. Durable —
  pending timeouts must survive worker restart or blocked runs leak.
- **TaskGlobal: none.** No derived cache or external client lives in-module, so there is no
  non-durable state and no rebuild path needed. (The skill's rebuild-path requirement is
  satisfied vacuously and deliberately: every piece of module state above is a durable PState.)
- External: the executor runtime (provider process spawning, JSON-RPC clients, streaming,
  redaction, env stripping), the context-bundle store/loader (kernel holds references only), and
  the secret store (API keys resolved at spawn; credentials never enter depots or PStates —
  records arrive pre-redacted, which keeps the no-credentials invariant out of the durable layer
  entirely).
