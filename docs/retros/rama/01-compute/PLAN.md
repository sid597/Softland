# Plan

<!-- Phase 1 Step 5. Fill in after completing Steps 1-4. After completing this, fill in PLAN_VALIDATION.md. -->

Module: `ComputeKernelModule` — requested workspace commands become observable run state.
Derived solely from `IMPLICIT_SPEC.md` (retrospective Phase 1; code-blind).

## Reads

| # | Read | Access method | Path / shape | Partition |
|---|------|---------------|--------------|-----------|
| R1 | `read-decision(run-id)` (O4) | `foreign-select-one` | `[(keypath run-id)]` on `$$decisions` → decision map or nil | task = hash(run-id) |
| R2 | `read-run(run-id)` (O5, full truth incl. token + tails) | **query topology** `run-record` | fields submap + stdout/stderr tail scans, assembled server-side | task = hash(run-id), leading `(|hash *run-id)` |
| R3 | `read-view(run-id)` (O6, UI projection, NEVER token) | **query topology** `run-view` | same reads as R2, assembly **omits** `:claim-token`, `:next-seq`, `:buffered` | task = hash(run-id), leading `(|hash *run-id)` |
| R4 | `read-pending(inbox-key)` (O7) | `foreign-select` | `[(keypath inbox-key) ALL]` on `$$pending-by-executor` → run-id collection | task = hash(inbox-key) |
| R5 | executor grant poll (O8 step 3; internal, hot path) | `foreign-select-one` | `[(keypath run-id) (submap :status :claimed-by :claim-token :executor/task-id)]` on `$$runs` | task = hash(run-id) |
| R5b | executor spec fetch (once per granted run, after R5 classifies granted-to-us) | `foreign-select-one` | `[(keypath run-id) (keypath :spec)]` on `$$runs` | task = hash(run-id) |

Notes:
- R2/R3 are query topologies because each needs >1 read (record fields + two subindexed tail maps) — multi-read on one partition must be batched into one roundtrip, not N client foreign-selects.
- R5 exists separately from R2 because the executor polls on the spawn-latency path and does NOT need tails. Polling `run-record` would waste 2 seeks per poll (tail scans). A single `foreign-select-one` with a submap is one roundtrip, ~1 seek. Never trade I/O efficiency for code simplicity: the 3-state grant classification (granted-to-us / not-yet-processed / conflict-or-past) only needs status + claimed-by + token. `:spec` is deliberately NOT in the poll submap — it is needed exactly once (to spawn), so the repeated poll must never re-read spec bytes; the executor fetches it with one extra point read (R5b) only after the poll classifies granted-to-us. `:spec` size is admission-capped at the submit guard (see Writes/topology), so R5b is a small bounded read (≤ ~164 KiB absolute worst case, typically < 1 KiB).
- All reads are callable in every entity state; absent keys navigate to nil (`keypath` nil semantics), which the contract maps to "absent".

Latency/throughput summary:
- R1, R4, R5, R5b: ms-scale point lookups, low volume (poll loops, once-per-submit checks, once-per-grant spec fetch).
- R2: executor/test truth reads, modest volume.
- R3: polled per visible run for the run's lifetime — hot read; bounded to ~3 seeks + ≤2×tail-cap iterations (~0.5ms·3 + 400·5µs ≈ 3.5ms worst case); byte-bounded too (≤200 × 4 KiB per tail by the line cap; typical lines ~100 B).

## Writes

| # | Write | Depot | Event type / shape |
|---|-------|-------|--------------------|
| W1 | Submit run-command request (O1) | `*compute-request-depot` | `{:run/id String, :request/id String, :request/type :compute/run-command, :request/time-ms Long, :actor String, :target Keyword, :action {:argv [String] :cwd String :env {String String}}, :executor-task-id (optional String hint)}` |
| W2 | Claim a run (O2) | `*compute-claim-depot` | `{:run/id String, :executor/id String, :claim-token String, :claimed-at-ms Long, :executor/task-id String}` |
| W3 | Report observation (O3) | `*compute-observation-depot` | `{:run/id String, :claim-token String, :observation/type Keyword, :sequence Long, :at-ms Long, + type-specific payload (:pid / :stream+:line / :exit-code / :reason+:message)}` |

Ack levels (foreign side): all three append with `:append-ack` — durability confirmation only. No caller requires read-your-write at ack time (the whole contract is poll-based: `await-decision`, `await-run`, `await-view`), and there are no stream topologies to coordinate with. The executor uses the `:append-ack` success to advance its own send cursor (an observation is "sent" only once durably appended — this is what makes "never dropped" hold from the executor side).

Throughput: W1/W2 are human/agent-paced (≤ a few/sec). W3 is the dominant volume (10³–10⁵ per run in bursts) — this drives the topology-type and PState-schema decisions below. Each W3 record is itself byte-bounded: the executor truncates every captured output line to ≤ 4 KiB with an explicit truncation marker before building the observation, so a single pathological line can never produce a giant depot record.

## PState Design

### `$$decisions` — request decision record (truth; never flips)

Obvious design: point lookup by run-id, record-shaped → top-level map keyed by run-id with a fixed-keys value. No alternative is cheaper than 1 seek for R1.

```clojure
{String ; run-id
 (fixed-keys-schema
   {:decision/status clojure.lang.Keyword   ; :accepted | :rejected
    :request/id      String
    :request/type    clojure.lang.Keyword   ; echoes the request, e.g. :compute/run-command
    :reason          clojure.lang.Keyword   ; rejection reason; absent for :accepted
    :decided-at-ms   Long})}
```

Kept separate from `$$runs` (not a category dimension of the same schema): a **rejected** request produces a decision and **no run row at all** — so "rejected" can never look like a pending/claimable run (resolves spec ambiguity 1), and `$$decisions` presence is the single dedup anchor for duplicate submits and run-id reuse (resolves ambiguity 2: run-id is single-use, first submit wins).

### `$$runs` — run lifecycle truth (central entity)

Not obvious; two schemas considered, the decision is driven by the dominant write path (W3 stdout/stderr lines).

**Option A — plain bounded vectors for tails:**
`:stdout-tail (vector-schema String)` capped at 200, ring semantics via read-modify-write.
Cost per output line: read whole tail (~200 lines ≈ 20KB deserialize) + rewrite whole tail. At 10⁵ lines per chatty run this is gigabytes of serialization churn per run — write amplification on the hottest path. Rejected.

**Option B — subindexed sorted maps keyed by dense per-stream line index, trimmed on write:**
`:stdout (map-schema Long String {:subindex? true})` keyed by line index 0,1,2,… (dense, from a `:stdout-count` field that is read in the same fields-read every observation event already performs). Cost per line: 1 entry write at `idx = count`, plus when `idx ≥ cap` one entry delete at exactly `idx - cap` (keypath delete, no read needed because the index is dense). O(1), no large-value rewrites. Tail read = `(subselect ALL)` over a ≤cap-entry map: 1 seek + ≤cap iter-reads (a range scan, not cap point seeks). Sorted ascending by index = chronological order.

**Chosen: B.** Per-line write cost is constant and tiny; reads stay one range scan. The per-event fields read (status/token/next-seq/counts) is a hot-key read — the row is rewritten every event, so it sits in the memtable/block cache (µs, not a cold 0.5ms seek).

```clojure
;; Polymorphic observation payloads for the out-of-order buffer — no Object,
;; no all-fields fixed-keys: interface + per-variant records.
(definterface IObservation)
(defrecord StartedObs   [sequence pid at-ms]            IObservation)
(defrecord OutputObs    [sequence stream line at-ms]    IObservation) ; stream = :stdout | :stderr
(defrecord HeartbeatObs [sequence at-ms]                IObservation)
(defrecord ExitObs      [sequence exit-code at-ms]      IObservation)
(defrecord FailedObs    [sequence reason message at-ms] IObservation)

(defrecord ObservationError [reason sequence obs-type at-ms]) ; never stores the offending token

{String ; run-id
 (fixed-keys-schema
   {:status             clojure.lang.Keyword ; :pending :launching :running :succeeded :failed (set open — :cancelled reserved for A2)
    :request/id         String
    :actor              String
    :target             clojure.lang.Keyword ; :workspace
    :spec               (fixed-keys-schema
                          {:argv (vector-schema String)        ; admission-capped at submit (≤1024 elems, ≤128KiB total) — no subindex
                           :cwd  String                        ; admission-capped (≤4KiB)
                           :env  (map-schema String String)})  ; allow-list, admission-capped (≤128 entries, ≤32KiB total) — no subindex
    :executor/task-id   String               ; assigned inbox key (durable assignment)
    :created-ms         Long
    :claimed-by         String               ; grant winner identity — written once
    :claim-token        String               ; grant secret — written once; ONLY exposed via R2/R5
    :claimed-at-ms      Long
    :pid                Long
    :exit-code          Long
    :failure-reason     clojure.lang.Keyword ; :spawn-failed | :timeout | … (ambiguities 5/6)
    :last-heartbeat-ms  Long
    :next-seq           Long                 ; in-order application watermark (next expected sequence)
    :stdout-count       Long                 ; dense line indexes for trim-without-read
    :stderr-count       Long
    :stdout             (map-schema Long String {:subindex? true}) ; line-idx → line (each ≤4KiB, line byte cap), trimmed to tail-cap (200)
    :stderr             (map-schema Long String {:subindex? true}) ; same caps as :stdout
    :buffered           (map-schema Long IObservation {:subindex? true}) ; obs-seq → obs; normally empty; cap 1024
    :observation-errors (vector-schema ObservationError) ; most-recent ≤100, app-enforced cap
    :observation-error-count Long})}        ; total ever, so spam volume stays auditable past the cap
```

Subindex rationale per collection:
- `:stdout`/`:stderr`: cap 200 > 100-element threshold and they are the per-line write path → subindexed (size tracking disabled — trim uses the dense counter, not SIZE). Entries are also byte-bounded by the per-line 4 KiB cap (see observation guard chain), so tails are bounded in entries AND bytes (≤200 × 4 KiB per stream).
- `:buffered`: cap 1024 > 100 → subindexed. Normally empty (single sequential in-process writer in A.0); exists for out-of-order/redelivered appends.
- `:observation-errors`: hard app-enforced cap of 100 most-recent entries (ring: drop oldest on overflow) → not subindexed; rare-path rewrite of a ≤100-element vector is acceptable and keeps R2/R3 to one fields-read. Total count preserved separately (resolves ambiguity 7).
- `:spec` argv/env: not subindexed — bounded by an explicit admission-time validation cap, which is the named enforcement mechanism: the submit guard **rejects** (decision `:rejected`, reason `:request/spec-too-large`) any request with `:argv` > 1024 elements or > 128 KiB total bytes, `:cwd` > 4 KiB, or `:env` > 128 entries or > 32 KiB total bytes. Generous for real commands (a 300-arg link line is ~3 KiB) yet hard-bounds every later `:spec` read (R5b). argv/env are write-once and read-whole, so with the cap enforced subindexing buys nothing.

### `$$pending-by-executor` — executor discovery inbox

Obvious design: R4 is "all pending run-ids for one inbox key" → map keyed by inbox key with a set of run-ids.

```clojure
{String ; inbox key: automatic executors use their stable task-id string; tests use arbitrary keys ("test-executor-a")
 (set-schema String {:subindex? true})} ; run-ids
```

Subindexed: normally near-empty (runs are claimed within ~1s), but a dead/never-existing inbox accumulates without bound in A.0 (stall handling is explicitly deferred), so worst case is unbounded → subindex. Executors can page discovery with `sorted-set-range-from` if an inbox is ever large; `ALL` is a single range scan otherwise.

This PState is deliberately **durable** (PState, not TaskGlobal): it is the restart-reconciliation anchor — after a worker restart, a fresh executor instance re-polls its stable inbox key and re-discovers every still-pending run.

### No materialized view PState (decision)

R3 (`read-view`) is served by the `run-view` query topology over `$$runs`, not by a second materialized PState:
- avoids doubling the per-line write cost on the dominant write path (every stdout line would be written twice);
- token secrecy becomes **structural at the only sanctioned view surface**: the query topology assembles the result map and simply never includes `:claim-token` (the spec's `(not (contains? view :claim-token))` holds by construction);
- the view's other invariants (bounded size, errors visible, terminal readable forever) fall out of `$$runs` directly.
Tradeoff noted: a query topology cannot back a PState reactive subscription. The exercised contract is poll-based (`await-view`), and a reactive `$$run-views` PState can be added later **additively** (new PState fed by the same topology) without breaking this design — so future reactive UI is not precluded.

## Depots

- `*compute-request-depot` — `(hash-by :run/id)`. Event types: `:compute/run-command` request envelopes (W1). `:request/type` dispatch inside the topology keeps the depot open to future compute request types (build/test/serve/deploy) without precluding them.
- `*compute-claim-depot` — `(hash-by :run/id)`. Event types: claim records (W2).
- `*compute-observation-depot` — `(hash-by :run/id)`. Event types: observation records (W3) — `:started :stdout :stderr :heartbeat :exit :failed` in A.0; `:artifact-produced :server-started :port-opened :succeeded :cancelled` named-not-precluded (unknown types from an authorized token fold to an observation error, not a crash).

Why three depots, not one: the three streams have radically different throughput profiles (observations dominate by 3–5 orders of magnitude) and **no intra-depot ordering requirement between them** — the executor's protocol externally serializes them (it appends a claim only after foreign-reading a committed `:pending` row; it appends observations only after foreign-reading a committed grant; foreign reads only see committed state, so the prerequisite write is always in an earlier microbatch than its dependent write). Cross-depot "early" arrivals (claim before run visible, observation before grant) are defined by the spec as safe no-ops / not-authorized errors and are exactly what the no-op guards produce.

Why `hash-by :run/id` on all three: every consumer-side write lands first on the run's row (`$$decisions`/`$$runs` are keyed by run-id), so depot processing is colocated with the primary PState writes — zero repartitioning on the dominant (observation) path; per-run total order on one task. The only repartition anywhere is the single `(|hash inbox-key)` hop for inbox add/remove.

## Topologies and PStates

**One topology**: `compute-kernel`, **microbatch**, sourcing all three depots. It owns all three PStates.

Why one topology: `$$runs` is written by all three processing concerns (submit creates the row, claim writes the grant, observations fold lifecycle/output). A PState has exactly one owning topology, so the three concerns must live in one topology. This also gives per-run serialization of all writes (same task, single-threaded).

Why microbatch and not stream (skill rule check, per write):

1. Latency: stream is only justified for single-digit-ms updates or ack-coordinated read-your-write. The spec explicitly does not require single-digit ms, and every consumer polls. Budget for the 5s submit→terminal bound (`echo`): submit append→committed `:pending` (≤1 microbatch cycle, ~200–500ms) + executor tick discovery (≤250ms) + claim append→committed grant (≤1 cycle) + grant poll (≤100ms) + spawn + process (~50ms) + observations→committed terminal (≤1 cycle) ≈ 1.5–2.5s worst case. Comfortable margin.
2. `ack-return>` need: none — nothing returns computed values at append time.
3. Idempotency / atomicity — the decisive factor:
   - **Submit** writes span two partitions (decision + run row on hash(run-id); inbox entry on hash(inbox-key)). Under stream, a failure after the run-task commit but before the inbox-task commit, followed by replay, would hit the "row already exists" guard and could skip the inbox add — orphaning the run (never discoverable). Microbatch makes the cross-partition pair exactly-once-atomic; no partial-apply window exists.
   - **Claim grant** also spans two partitions (grant CAS on run task; `disj` from inbox on inbox task) and is the single-grant safety core; same partial-apply hazard under stream (grant committed, removal lost on replay because the `:pending` guard is now false → stale inbox entry re-arms discovery). Microbatch removes the hazard.
   - **Observations** include non-idempotent-shaped writes (tail entry insert + counter advance + errors-ring append). Microbatch gives exactly-once per depot record, so topology-side retries can never double-apply a line. Non-idempotent writes under a stream topology would be a design error here.
4. Throughput: observations are the dominant volume; microbatch's batched I/O is the right default for it.

Stream-retry analysis (required by template): not applicable — no stream topology. All writes are exactly-once from depot record to PState under microbatch. Remaining duplicate sources are **client-level duplicate depot records** (redelivered submit, re-sent claim, re-sent observation with the same `:sequence`), which exactly-once does not cover; they are handled by app-level guards that are pure functions of already-committed state:

- **Submit guard**: process only if `$$decisions[run-id]` is **absent**. Present (accepted or rejected) → total no-op: no decision flip, no run-row touch (no `:pending` regression — the double-spawn re-arm path is closed), no inbox re-add. Validation on the absent path: `:request/type` known, `:target` kind ∈ `#{:workspace}`, `:run/id` non-blank, `:argv` non-empty vector of strings, and the spec within the admission size caps — `:argv` ≤ 1024 elements and ≤ 128 KiB total bytes, `:cwd` ≤ 4 KiB, `:env` ≤ 128 entries and ≤ 32 KiB total bytes (over-limit → `:rejected`, reason `:request/spec-too-large`) — → `:accepted` (decision + run row `:pending` + inbox add) else `:rejected` (decision only; reason recorded). The size caps are the enforcement mechanism that makes the non-subindexed `:spec` collections bounded: nothing over-cap ever lands in `$$runs`, so every `:spec` read (R5b) is hard-bounded. Blank/missing `:run/id` cannot be keyed → refused client-side by the submit helper and dropped without state by the topology. Empty argv → rejected (structurally unrunnable); nonexistent binary → accepted (surfaces later as a `:failed` run, per spec).
  Inbox assignment on accept: explicit `:executor-task-id` hint wins verbatim (test path; may dangle — spec-sanctioned: run waits forever); otherwise `(mod |hash(run-id)| task-count)` as a stable string — deterministic, balanced across the per-task automatic executors, durably recorded in the run row as `:executor/task-id`.
- **Claim guard**: run row absent → no-op (claims never create state). `:status = :pending` → grant: write `:status :launching`, `:claimed-by`, `:claim-token`, `:claimed-at-ms` (write-once fields), then `(|hash (:executor/task-id <run-row>))` — the **run row's recorded** assignment, not the claim's claimed value — and `disj` the run-id from that inbox. Any other status → strict no-op (winner's fields never overwritten; redelivered winning claim is a no-op because status is already `:launching`; competing claims in the same microbatch are serialized on the run's task — first wins deterministically).
- **Observation guard chain** (single task, in order; `:stdout`/`:stderr` line payloads are defensively truncated to the 4 KiB line cap + truncation marker on ingest — before buffering or applying — so the byte bound holds kernel-side even against a non-conforming authorized writer):
  1. Run row absent → drop, no state created (ambiguity 8: not audited in A.0 — there is no row to attach an error to; flagged as an A2 audit-counter candidate rather than funneling attacker-controlled volume to a global task).
  2. Authorization: `:claim-token` present on the row AND equal to the observation's token. Fail (including any token while status is `:pending`) → append `ObservationError{:reason :observation/not-authorized, …}` to the capped errors ring + `inc` `:observation-error-count`; status/output/terminal fields untouched. The error entry never contains the offending token (it would leak through the view).
  3. Terminal status (`:succeeded`/`:failed`) → authorized observations ignored silently (redelivery; the watermark already covers `seq < next-seq`), terminal record immutable (ambiguity 4: unauthorized→error per rule 2 above, authorized→ignore).
  4. Sequence: `seq < next-seq` → duplicate, ignore (first payload kept; never double-applied). `seq > next-seq` → store in `:buffered` **only if `:buffered[seq]` is absent** (navigate `(keypath seq)`, write only when nil — first payload kept; a redelivered buffered sequence with a different payload is a no-op and can never overwrite the first, mirroring the applied-path first-wins rule) (cap 1024; overflow → ObservationError `:observation/buffer-overflow` + reject — a never-hit-in-A.0 safety valve, since the A.0 writer is sequential). `seq = next-seq` → apply, advance `next-seq`, then drain `:buffered` while the next seq is present (delete each drained entry; `loop<-` with `yield-if-overtime` so a large drain cannot starve the task).
  5. Apply by type: `:started` → `:status :running`, `:pid` (legal from `:launching`); `:stdout`/`:stderr` → write line at `idx = count`, `inc` count, delete `idx - cap` when `idx ≥ cap` (cap 200); `:heartbeat` → `:last-heartbeat-ms` only; `:exit` → after drain, `:exit-code` recorded, `:status` `:succeeded` if 0 else `:failed` (legal from `:running` **or** `:launching` — covers "process died before `:started` was reported"); `:failed` → after drain, `:status :failed` + `:failure-reason` (spawn-failure/timeout path — no fabricated exit code; resolves ambiguities 5/6).
  Gap-never-fills at exit (ambiguity 3): strict in-order application means a buffered `:exit` behind a permanent gap leaves the run non-terminal. Accepted for A.0 (the single sequential writer cannot produce a permanent gap; an unfillable gap is indistinguishable from a lost observation, which is the A2 stall-detection problem). Truth never lies — the run simply never claims success.

PStates owned by `compute-kernel`: `$$decisions`, `$$runs`, `$$pending-by-executor` — full typed schemas above; no `Object` anywhere (polymorphic buffer values use `IObservation` + variant records).

## Query Topologies

- `run-record` (R2): input `*run-id` → full truth map (includes `:claim-token`, `:stdout-tail`/`:stderr-tail` as line vectors, `:observation-errors`, all lifecycle fields).
  - Input example 1: existing `:running` run with output → 3 total reads, 3 meaningful: fields submap on `$$runs` (1 seek), `(subselect ALL)` over `:stdout` (1 seek + ≤200 iter), same over `:stderr` (1 seek + ≤200 iter).
  - Input example 2: unknown run-id → 1 total read, 1 meaningful: fields submap navigates to nil → emit nil result, **skip both tail reads**.
  - Input example 3: `:pending` run (no output yet) → fields read shows zero counts → tail reads skipped → 1 meaningful read, well-formed empty-tail result.
  - Fixed or variable: **variable** (1 vs 3). Dynamic approach: conditional branching (`<<if` on nil-row / zero counts) so empty scans are never issued — no padded reads.
  - Leading `(|hash *run-id)` partitioner → evaluated client-side, no random-task hop; `|origin` to return.
- `run-view` (R3): input `*run-id` → UI projection `{:status :pid :exit-code :failure-reason :stdout-tail :stderr-tail :observation-errors :observation-error-count}`. Same read structure, same variable-read handling as `run-record` (1 vs 3 meaningful reads). Assembly **never touches** `:claim-token`/`:next-seq`/`:buffered` — token secrecy by construction; a rejected request has no `$$runs` row → view absent (never looks like a pending run).

R1/R4/R5 are single-path single-partition reads → plain foreign selects, no query topology (per the access-method decision rules).

## Design Decisions

- **Subindexing**: `$$runs :stdout/:stderr` (per-line write path, cap 200 > threshold), `:buffered` (cap 1024, normally empty), `$$pending-by-executor` sets (unbounded worst case while stall handling is deferred). NOT subindexed: `:observation-errors` (hard cap 100, ring), `:spec` argv/env (enforced admission caps at the submit guard: argv ≤1024 elems/≤128KiB, cwd ≤4KiB, env ≤128 entries/≤32KiB; over-limit submits rejected `:request/spec-too-large`).
- **Colocation**: all three depots `hash-by :run/id` ⇒ depot processing lands on the same task as the `$$decisions`/`$$runs` rows. The only repartition is the one `(|hash inbox-key)` hop per accept/grant for the inbox — negligible volume. The dominant path (observations) is zero-hop, single-task, O(1) writes against a memtable-hot row.
- **Single microbatch topology** for exactly-once + cross-partition atomicity (submit's row+inbox pair; grant's CAS+removal pair) — see topology section for the per-write idempotency reasoning.
- **External serialization makes cross-depot ordering a non-issue**: the executor awaits committed state (foreign reads) between protocol steps, so a claim is always in a later microbatch than its run row, and observations later than their grant. "Early" arrivals are spec-defined no-ops/errors, and the retry loops (executor reconcile, grant poll) make them self-healing.
- **Spawn exactly-once-or-reconciled mechanism** (binding ground rule), layered:
  1. *Durable single grant*: at most one claim ever wins the `:pending → :launching` CAS (write-once grant fields, exactly-once microbatch, per-run task serialization).
  2. *Spawn-after-grant*: an executor spawns only after foreign-reading a **committed** grant matching `claimed-by = me ∧ claim-token = mine` (R5 3-state check: granted-to-us / not-yet-processed (absent ∨ `:pending`) / conflict-or-past). Never spawn on hope. The check requires identity AND token — same-token-different-id classifies as conflict.
  3. *Per-instance in-flight registry* (TaskGlobal memory): dedups discovery across reconcile ticks — prevents the instance racing itself with a second claim/new token while its first claim is in flight, and prevents re-spawn attempts for runs it already spawned.
  4. *Across instances/restarts*: a fresh instance holds a fresh identity and fresh tokens; the write-once grant means at most one (instance, token) pair in history ever passes check 2 for a given run ⇒ **at most one spawn per run, ever**, across all retries, races, and restarts.
- **Worker-restart reconciliation** (what survives, what stalls — matches the spec's A.0/A2 split):
  - Still-`:pending` runs: durable in `$$pending-by-executor` under the executor's **stable inbox key (task-id string)**; the new instance re-discovers and re-claims with a fresh token → run proceeds, exactly one spawn. (If an in-flight pre-restart claim wins instead, the run sits `:launching` with a lost token — stalled-but-safe, never double-spawned.)
  - Granted/`:running` runs at crash time: the token existed only in lost memory; no observation can ever be authorized again → run stays non-terminal with a stale heartbeat. **Recorded truth never claims success for work that didn't finish.** Un-stalling is explicitly A2 (stall/lost-run detection); `:last-heartbeat-ms` is already recorded to enable it.
  - Orphaned OS children: may outlive the worker; they cannot write truth (no authorized channel) → no false success. A2 reconcile concern.
- **Ambiguity resolutions** (spec §Open Ambiguities): (1) rejected ⇒ decision only, no run row; (2) run-id single-use, dedup anchor = `$$decisions` presence, first-wins; (3) strict in-order apply, permanent gap blocks closure (impossible in A.0, A2 owns it), buffer capped with auditable overflow error; (4) post-terminal: authorized→ignore, unauthorized→error entry; (5) spawn failure ⇒ executor sends `:failed` observation `{:reason :spawn-failed}` (no fabricated exit code); (6) manual-path timeout ⇒ kill forcibly + `:failed` observation `{:reason :timeout}`; (7) errors ring-capped at 100 + total counter; (8) unknown-run observations dropped without state, not audited in A.0 (documented gap); (9) extensibility preserved: open keyword status set (`:cancelled` reserved), open `IObservation` interface, `:request/type` dispatch on the request depot, additional views/depots are additive.
- **Executor placement and identity**: one automatic executor per task (TaskGlobal instance), inbox key = stable task-id string (the durability anchor), executor identity = task-id + per-launch nonce (a restarted instance unambiguously classifies pre-restart grants as conflict-or-past). The kernel's normal-path assignment targets exactly these inbox keys; test inboxes use disjoint keys, so automatic executors can never steal manual-path runs (and the dangling-hint "waits forever" behavior is exactly what tests exploit).
- **Executor threading**: physical work runs on executor-owned threads inside the TaskGlobal (reconcile scheduler started in `prepareForTask`; per-process stdout/stderr pump threads that **truncate every captured line to the 4 KiB byte cap with an explicit truncation marker** (e.g. `…[truncated N bytes]`) before building the `OutputObs` — the primary line-bound enforcement site, bounding the depot record itself as well as the stored tail; all stopped / `destroyForcibly`'d in `close()`), never on task threads — request/claim/observation processing is never blocked by process execution (out-of-band requirement; also satisfies O9 clean start/close per test cycle). The executor touches the kernel **only** through the public foreign surface: appends to the claim/observation depots, foreign reads of inbox + grant fields. The back-arrow rule holds even for the kernel-owned executor.
- **Manual path** (`run-one-pending-local!`): a pure foreign client performing exactly one discover→claim→await-grant→spawn→stream cycle against an explicit inbox key with `:timeout-ms`; returns the protocol classification (`{:claim-state :granted-to-us :spawned? true :spawned-after-grant? true}`, or nil when it found nothing / lost the claim). Identical guards as the automatic path (including the 4 KiB per-line truncation at capture) — a protocol probe, not a bypass. No module-side state.
- **Hot-row read cost**: the per-observation fields read targets a row rewritten every event → memtable/cache hit (µs), not a cold seek; with 10³–10⁵ observation bursts this keeps per-observation cost small and constant as required. If multi-run scale ever pressures this, per-run pre-aggregation inside the microbatch is the escape hatch (noted, not built).

## State primitive selection

- `$$decisions` (PState): source of truth for request outcomes; write volume O(1) per submit; grows with total requests forever (point-lookup access; decisions must stay readable indefinitely — no deletion). Durable.
- `$$runs` (PState): source of truth for run lifecycle/output; write volume O(1) per observation (entry insert + dense-index trim delete + watermark/count fields), bounded per event; on-disk size per run bounded by caps (tails 2×200 lines × ≤4 KiB/line, buffer ≤1024 entries of line-capped payloads, errors ≤100, spec ≤ ~164 KiB by admission caps) + fixed fields; row count grows with runs forever (terminal records readable indefinitely). Durable.
- `$$pending-by-executor` (PState): discovery inbox; write volume O(1) per accept/grant; **must** be durable — it is the restart rebuild anchor for pending work. Durable.
- Automatic executor (TaskGlobal, one per task): non-durable by design. Holds: reconcile scheduler thread, in-flight registry (run-id → {claim-token, Process handle, pump threads}), foreign client handles. **Rebuild path on worker restart / module update (cited per ground rule):** the registry starts empty; the instance re-reads its stable inbox key from durable `$$pending-by-executor` (R4) and re-derives all claimable work; per-run grant state is re-classified from durable `$$runs` via the R5 3-state check. Nothing in the TaskGlobal is truth — every datum is a cache of, or a handle derived from, durable PState rows, except live Process handles and claim tokens, whose loss is the spec-sanctioned A.0 stall case (truth never corrupted, no double spawn possible). Registry memory is capped: entries are released when the run is observed terminal or classified conflict-or-past; the inbox keyspace is one key per task (bounded).
- OS processes (external): spawned only under check 2 of the spawn mechanism (exactly-once-or-reconciled: durable single grant + spawn-after-grant + per-instance registry + fresh-identity-on-restart, detailed above); stdout/stderr/exit captured by pump threads and reported as observations; killed on `close()` (O9) and on manual-path timeout.
- Manual-path executor (external client code): stateless per invocation; no rebuild concern.

## Amendments (Phase 2 round 1)

PLAN_VALIDATION.md round 1 returned FAIL on three plan-text-level gaps (F1/F2/F3). Minimal repairs below; every other design decision (three depots hash-by run-id, one microbatch topology, three PStates, layered spawn-exactly-once, query-topology view) is untouched.

- **F1 — `:spec` argv/env enforcement + hot-poll exposure.** Added explicit admission-time size caps to the submit validation — `:argv` ≤ 1024 elements and ≤ 128 KiB total bytes, `:cwd` ≤ 4 KiB, `:env` ≤ 128 entries and ≤ 32 KiB total bytes; over-limit → `:rejected` with reason `:request/spec-too-large`. This validation rule is the named enforcement mechanism for the non-subindexed `:spec` collections ("small, app-bounded" is now an enforced invariant, not an assumption). Additionally removed `:spec` from the R5 grant-poll submap: the repeated poll reads only the 3-state classification fields, and the executor fetches `:spec` exactly once per granted run via a new bounded point read (R5b) — spec bytes never ride the hot poll loop.
- **F2 — buffered out-of-order observations are store-if-absent.** Guard step 4's `seq > next-seq` branch now writes `:buffered[seq]` only when that key is absent (first-write-wins). A redelivered sequence number with a different payload is a no-op — the first payload is kept, matching the applied-path `seq < next-seq` first-wins rule, so "must keep the first, never corrupt" holds across the entire sequence window.
- **F3 — per-line byte cap (4 KiB) with truncation marker.** Primary enforcement at the executor's stdout/stderr pump threads (automatic and manual paths): every captured line is truncated to ≤ 4 KiB with an explicit marker before the observation is built, bounding the depot record itself. Defensive secondary enforcement kernel-side: line payloads are truncated to the same cap on ingest, before buffering or tail application, so the bound holds even against a non-conforming authorized writer. Tails (and the buffer) are now bounded in entries AND bytes (≤ 200 × 4 KiB per stream); affected schema/cost notes updated accordingly.
