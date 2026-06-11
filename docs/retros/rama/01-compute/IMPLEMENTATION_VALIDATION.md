# Implementation Validation

<!-- Phase 4. Fill in after Phase 3 produces the module source. -->

Review all topology, query topology, and foreign client code. For each check, state pass or fail with evidence. Then emit one of three verdicts at the end of this artifact, per the rubric below.

**Mode:** RETROSPECTIVE. Module under validation: `src/app/server/rama/dogfood/compute.clj`
(read in full, 897 lines). Validated against `PLAN.md` (retro-derived, code-blind) **and**
`IMPLICIT_SPEC.md` (per phase doc: the plan can be wrong; the spec binds). Rama semantics
cited from the skill references:
- `references/stream.md:9` — a stream record can retry **after all PState writes committed**; every write must be idempotent or explicitly deduplicated.
- `references/stream.md:96,168` — partitioner hops split a stream event into independently-committing streaming batches; on retry, writes already committed on other tasks are **not rolled back**; the event replays from the source block start.
- `references/stream.md:87-91` — `:all-after` retries the failed record **and all subsequent records** on the partition (duplicates of already-processed records).
- `references/core-concepts.md:10-14` — stream transaction scope = between partitioners; microbatch = cross-partition exactly-once; default to microbatch.
- `references/task-globals.md:145` — TaskGlobals are not durable; recreated on worker restart.
- `references/paths.md:33` — `foreign-select-one` is a built-in.

## Plan divergence (Phase 4 step 3)

The implementation does not match the plan on its load-bearing decisions:

| # | Plan | Implementation |
|---|------|----------------|
| D1 | **Microbatch** topology, chosen explicitly for (a) cross-partition atomicity of submit's row+inbox pair and grant's CAS+removal pair, (b) exactly-once observation folding (PLAN.md §Topologies, items 3a-3c) | **Stream** topology (`compute.clj:616`). Every hazard the plan enumerated as the reason to reject stream is live in the code (traced below: idempotency + partial-failure checks). |
| D2 | Submit dedup guard: "process only if `$$decisions[run-id]` is absent" (PLAN.md §Topologies, Submit guard) | No guard of any kind. Submit branch unconditionally overwrites decision, run row, view, and inbox (`compute.clj:627-638`). |
| D3 | No materialized view PState; `run-view` query topology; "avoids doubling the per-line write cost on the dominant write path" (PLAN.md §No materialized view PState) | Fourth PState `$$compute-views` (`compute.clj:620`), rewritten in full on every submit, claim, and observation (`compute.clj:636,649,660`). |
| D4 | Tails as subindexed sorted maps with dense index + trim-without-read; plain-vector Option A explicitly **rejected** as "write amplification on the hottest path" (PLAN.md §`$$runs`, Option A/B) | Plain vectors with `append-bounded` read-modify-write of the whole row per line (`compute.clj:366-372,416,421`) — the rejected Option A. |
| D5 | Typed `fixed-keys-schema` everywhere, "no `Object` anywhere" (PLAN.md §PStates owned) | All four PStates are `{String (map-schema Keyword Object)}` (`compute.clj:617-620`). |
| D6 | `:buffered` subindexed, cap 1024, overflow → auditable error (PLAN.md §`$$runs`) | `:obs-buffer` plain map, **no cap** (`compute.clj:473`). |
| D7 | Errors ring cap 100 + `:observation-error-count` total (ambiguity 7) | Cap 50 (`compute.clj:36`), no total counter. Boundedness holds; auditability-past-the-cap does not. |
| D8 | Query topologies `run-record`/`run-view`; executor grant poll = submap `foreign-select-one`, no tail transfer (PLAN.md §Reads R2/R3/R5) | No query topologies. `read-run` fetches the whole row including both 200-line tails and the buffer on every 50ms grant poll (`compute.clj:711-713,511`). |
| D9 | `:heartbeat` applied (liveness timestamp), `:failed` observation type for spawn failure/timeout (PLAN.md §Observation guard step 5) | `observation-types #{:started :stdout :stderr :exit}` (`compute.clj:47-48`); heartbeat → `:observation/type-invalid` error; spawn failure/timeout → synthetic `:exit` 127/124 (`compute.clj:852-853,865`). Synthetic exit codes are spec-acceptable (ambiguities 5/6); heartbeat handling is not (see S6). |
| D10 | Executor identity = task-id + per-launch nonce (PLAN.md §Executor placement) | Stable `"compute-executor-"+task-id` (`compute.clj:568`). Traced safe: `claim-state` requires identity AND token match (`compute.clj:360-362`), and a restarted instance only ever evaluates claims it minted with fresh tokens, so pre-restart grants classify `:conflict-or-past`. Divergence without violation. |
| D11 | Inbox = subindexed set of run-ids | Inbox = non-subindexed map run-id → pending-entry map (`compute.clj:619,638`). Functional for the read contract; unbounded (see size-limits check). |

Per the phase doc, divergence alone is not the verdict — the traces below test the
implementation against the spec under production rules. It fails.

## Redundant conditionals
<!-- if every branch of an <<if, <<cond, or <<switch does the same operation with only a variable differing, replace with a single operation using that variable directly -->

**Sites:** `<<if` at `compute.clj:631` (submit accept branch, no else) and `compute.clj:644` (claim grant branch, no else). No `<<cond`/`<<switch` in the module.

**Trace:** Each `<<if` has a single then-branch performing writes that the false path must not perform (accepted-only run-row/inbox writes; grantable-only grant writes). No branch pair duplicates an operation differing only by a variable.

**PASS.**

## Consecutive keypath
<!-- (keypath *a) (keypath *b) → (keypath *a *b) -->

**Sites:**
- `compute.clj:638`: `[(keypath *executor-task-id) (keypath *run-id) (termval *pending-entry)]`
- `compute.clj:651`: `[(keypath *executor-task-id) (keypath *run-id) NONE>]`

**Trace:** Both are two consecutive single-key `keypath` navigators into `$$compute-pending-by-task`; the canonical form is `(keypath *executor-task-id *run-id)`.

**FAIL** (mechanical, two sites).

## Select-compute-transform
<!-- local-select> followed by computation followed by local-transform> with termval — replace with +compound and an aggregator when possible -->

**Sites:**
- Claim branch: `local-select>` row (`compute.clj:643`) → `grant-claim` (`compute.clj:645`) → `termval` whole row (`compute.clj:648`).
- Observation branch: `local-select>` row (`compute.clj:656`) → `fold-observation` (`compute.clj:657`) → `termval` whole row (`compute.clj:659`).

**Trace (observation branch, the dominant path):** every observation — including no-op duplicates where `fold-observation` returns the row unchanged (`compute.clj:466-467`) — reads the entire row (two 200-line tails + buffer + errors, tens of KB once a run is chatty), folds, then rewrites the entire row at `:659` **and** rewrites the entire view row at `:660`. Per stdout line, that is two full-row serializations to update one vector element. The restructure that removes this (targeted per-field writes against subindexed tails, dense-index trim — i.e., exactly PLAN.md Option B) is possible and was specified. The select→whole-row-termval shape is what forces the amplification; "compute new value then termval" is only the sanctioned no-read pattern when the write itself is proportionate, and here it is not.

**FAIL** (requires schema + write-path restructuring, not a line edit).

## Unnecessary nil->val
<!-- navigators handle nil as empty collection — do not add nil->val unless the next navigator requires a non-nil value (e.g., (nil->val 0) before (term inc)) -->

**Trace:** No `nil->val` appears anywhere in the module (verified over the full source). Nil-row handling is done in Clojure functions, not navigators.

**PASS** (vacuous).

## :allow-yield?
<!-- local-select> or select> that iterates over a subindexed structure on a non-mirror PState should include {:allow-yield? true} whenever the iteration count can exceed ~100 entries. -->

**Sites:** All topology reads are single-key point reads: `[(keypath *run-id)]` at `compute.clj:643,656`. No subindexed structures exist in the module (see size-limits check), and no read uses `ALL`/`MAP-VALS`/range navigators inside the topology.

**Trace:** Point reads of one top-level key do not iterate entries; `:allow-yield?` has no applicable site.

**PASS** (vacuous — but the *absence* of subindexing is itself failed under the next check).

## Non-subindexed collections without size limits
<!-- for every write to a non-subindexed inner collection (map, set, vector), verify the application explicitly enforces a maximum size. If there is no code that caps the collection size, it must be subindexed. -->

Per-collection audit (all PStates are non-subindexed `map-schema Keyword Object`, `compute.clj:617-620`):

| Collection | Write site | Cap? | Verdict |
|---|---|---|---|
| `:stdout-tail` / `:stderr-tail` | `compute.clj:416,421` via `append-bounded` (`:366-372`), limits 200 (`:34-35`) | Yes, enforced in code | pass |
| `:observation-errors` | `compute.clj:386-389` via `append-bounded`, limit 50 (`:36`) | Yes | pass |
| `:obs-buffer` | `compute.clj:473` `(assoc-in run-row [:obs-buffer seq-id] obs)` | **No cap anywhere** | **FAIL** |
| `$$compute-pending-by-task` inner map (run-id → entry) | added `compute.clj:638`, removed only by grant `compute.clj:651` | **No cap; removal can be permanently skipped (see partial-failure check)** | **FAIL** |
| `:argv` | input, validated non-empty vector of strings (`compute.clj:182-184`) | App-bounded input | pass |

**Trace for `:obs-buffer`:** an authorized writer (or the heartbeat desync in S6, or `:all-after` redelivery interleavings) that produces a sequence gap buffers every subsequent observation. A run printing 10⁵ lines after a gap stores ~10⁵ full observation maps in a non-subindexed inner map that is deserialized and reserialized **on every later event** for that run. Plan specified cap 1024 + overflow error; implementation has neither.

**Trace for the inbox:** the spec itself states a dead/dangling inbox accumulates without bound in A.0 (IMPLICIT_SPEC O7, O1 edge cases — the dangling `:executor-task-id` hint is a sanctioned state). Unbounded growth in a non-subindexed inner map means every inbox read (`compute.clj:727`, polled every 50ms per executor, `:587-597`) deserializes the whole map.

**FAIL.**

## Stream topology idempotency
<!-- for each stream topology, trace through what happens if any event retries. -->

The single topology is a **stream** topology (`compute.clj:616`). Per `stream.md:9`, every record can retry even after all writes committed.

**Submit branch (`compute.clj:622-638`) — FAIL, the worst finding.**
There is no dedup guard. Retry trace:
1. Submit for run R processes fully: decision written (`:627`), run row `:pending` written (`:635`), view (`:636`), inbox entry (`:638`). All committed.
2. Rama's progress tracking fails after commit (`stream.md:9`) → the record retries from the source block start.
3. Between commit and retry, the executor reconcile loop (50ms ticks, `compute.clj:587-597`) discovered R, claimed it, the claim branch granted it (status `:launching`, token T1, inbox entry removed), the process possibly already spawned.
4. The retried submit re-runs `interpret-run-command-request` → accepted → `initial-run-row` (`compute.clj:266-291`) builds a **fresh** row: `:status :pending` (`:274`), `:claim-token nil` (`:282`), `:last-seq -1` (`:287`) → `termval` at `:635` **overwrites the granted row**. The run regresses to `:pending`; the grant (winner identity + token) is destroyed; `:638` re-adds the inbox entry.
5. The executor finishes the first process, removes R from its registry (`compute.clj:501`), next tick re-discovers R in the inbox (`:523-531`), mints a fresh claim T2, `grantable-claim?` sees `:pending` (`:339-344`) → **second grant, second spawn**.

This breaks the spec's single-grant invariant (O2: "for any run, at most one claim is ever granted") and at-most-one-spawn-ever (O8), and the explicit O1 requirement: "A duplicate submit of an existing `:run/id` ... must never regress an in-flight or terminal run back to `:pending` (that would re-arm the spawn path → double spawn)." The identical trace fires with **no infrastructure failure at all** on a plain client-side duplicate submit (redelivered `foreign-append!` after a lost ack), which the spec names as expected (O1 concurrency: "duplicate delivery/retry of the same submission is possible"). Decisions also flip: a resubmit after `:rejected` re-interprets and overwrites the decision (`:627`), and even an identical duplicate rewrites `:decided-at` (`(now-ms)` at `compute.clj:243` — nondeterministic value computed inside the topology).

ID generation inside the topology: none (event-id is derived deterministically from `:request/id`, `compute.clj:212-213`) — that sub-check passes; the branch fails on the regression.

**Claim branch (`compute.clj:640-651`) — write itself guarded, but see partial-failure check.**
`grantable-claim?` (`compute.clj:339-344`) requires `:status :pending`, so a redelivered winning claim and any competing claim are no-ops and grant fields are never overwritten (matches O2). The grant write is a guarded `termval` — idempotent on same-task replay. The cross-partition removal is NOT retry-safe — traced in the next check.

**Observation branch (`compute.clj:653-660`, `:retry-mode :all-after`) — FAIL.**
- Pre-terminal replay of an applied sequence: `(< seq-id expected)` → row returned unchanged (`compute.clj:466-467`) → no double-applied lines. That sub-check passes.
- **Post-terminal replay:** `:all-after` (`stream.md:87-91`) replays already-processed records. After `:exit` is applied (status terminal), `authorized-observation?` (`compute.clj:392-396`) returns false for **every** observation — including the run's own legitimately-applied ones — because of the `(not (contains? terminal-statuses ...))` conjunct. Each replayed record therefore appends a `:observation/not-authorized` error (`compute.clj:457-458`) via `append-bounded` — a **non-idempotent write with no dedup mechanism**. One retry late in a chatty run sprays up to 50 false audit entries into `$$compute-runs` and `$$compute-views`, evicting any genuine unauthorized-writer audit entries (ring cap 50). The spec's auditability invariant (O3: errors are how real attacks are seen) is corrupted by an infrastructure retry.
- **Unknown-run observations create state:** `local-select>` at `:656` yields nil; `fold-observation nil obs` → not authorized → `add-observation-error` does `(update nil :observation-errors ...)` (`compute.clj:386`) producing `{:observation-errors [...], :updated-at t}`, which `:659-660` then write under the run-id into **both** `$$compute-runs` and `$$compute-views`. Direct violation of O3 ("observation for an unknown `:run/id` must not create run state") and the matrix row `does-not-exist × observation → read-run: still absent`. The created rows have no `:status` and are attacker-mintable across the whole keyspace.

**FAIL.**

## Partial failure in stream topologies
<!-- for each stream event that writes to multiple PStates across multiple partitions, consider what happens if the event fails and retries after some writes have committed but others have not. Is it possible for a partial failure + retry to leave any writes permanently unexecuted? -->

Per `stream.md:96,168`: writes before a partitioner hop commit independently of writes after it; on retry the event replays from the source block start and committed writes are not rolled back.

**Claim event — YES, a write is permanently unexecuted. FAIL.**
The claim branch spans two partitions: grant writes on task hash(run-id) (`compute.clj:648-649`), then `(|hash *executor-task-id)` (`:650`), then inbox removal (`:651`). Trace:
1. Grant commits on task A (run row now `:launching`, token T1). Streaming batch on task B (inbox removal) fails.
2. Retry replays from the source block: `local-select>` at `:643` now reads `:launching` → `grantable-claim?` is **false** → the entire `<<if` body — including the `:651` removal — is skipped.
3. The inbox entry for a granted run is **permanent**. Spec O7's iff-invariant ("a run-id appears here iff the run is `:pending`") is violated forever.
4. Live consequence: the automatic executor's registry entry for R is removed when the run finishes (`compute.clj:501`) or classified `:conflict-or-past` (`:520-521`); every subsequent reconcile tick re-discovers the stale entry (`:523-531`), appends a fresh claim to the depot, the claim no-ops (status not `:pending`), the registry entry is dropped again — an **infinite claim-append loop**, one depot append per ~100ms per stuck entry, forever, with unbounded depot growth.

This is precisely the hazard PLAN.md §Topologies item 3 cited as the reason stream was rejected ("grant committed, removal lost on replay because the `:pending` guard is now false → stale inbox entry re-arms discovery").

**Submit event — partial failure does not lose writes (retry re-executes everything unconditionally) but the unconditional re-execution is itself the regression catastrophe traced in the idempotency check.** Additional window: if the task-A batch (decision/run/view) commits and the task-B inbox write fails, a manual-path claim (which goes by run-id, not inbox discovery, `compute.clj:774-785`) can grant the run before the retry, and the retry then tramples the grant.

**Observation event — single hop (`(|hash *run-id)` at `:655`), all writes on one task in one streaming batch → atomic per `stream.md:96`. No cross-partition partial failure (the full-commit-then-retry case is covered above).**

**FAIL** (claim removal permanently unexecuted; fix is structural — microbatch per the plan, or a removal path that does not live behind the `:pending` guard).

## Single depot append per client operation
<!-- each client write operation must call foreign-append! exactly once. -->

**Sites:** submit = one append (`compute.clj:686-691`); claim = one append (`:693-698`); each observation = one append (`:700-705`). No client operation performs two depot appends to complete one logical write.

**Trace:** The executor protocol performs claim-then-observations, but those are distinct spec operations (O2, O3) with the run-record poll between them; a crash between them leaves a `:launching` run, which is the spec-sanctioned A.0 executor-death stall, not an inconsistent half-operation. The spawn-failure path appends `:stderr` then `:exit` (`compute.clj:864-865`) — two observations, each a complete O3 operation; a crash between them leaves a non-terminal run (same sanctioned stall class). No topology-internal `depot-partition-append!` exists.

**PASS.**

## Application-state caches survive restart
<!-- for each TaskGlobal or in-process cache holding application state, verify durable source + concrete rebuild path. -->

**Cache:** `ComputeExecutorTaskGlobal` per-task registry (`ConcurrentHashMap`, `compute.clj:569`) holding run-id → `{:state :awaiting-grant|:running, :claim}`; plus scheduler/worker pools.

**Durable sources, named:** `$$compute-pending-by-task` (pending work, written by the submit branch at `:638`) and `$$compute-runs` (grant/lifecycle state, `:635,648,659`). TaskGlobals are recreated empty on worker restart (`task-globals.md:145`).

**Concrete rebuild path, cited:** `prepareForTask` (`compute.clj:565-597`) starts the reconcile loop; `claim-new-pending-runs!` (`:523-531`) re-reads the durable inbox each tick and re-claims undiscovered pending runs with fresh tokens; `reconcile-awaiting-claims!` (`:504-521`) re-classifies in-flight claims from the durable run row via `claim-state` (`:356-364`). Trace of restart: registry starts empty → still-`:pending` runs are re-discovered from the durable inbox and proceed with exactly one grant (the durable `:pending → :launching` guard arbitrates between a pre-restart in-flight claim and the new one). Runs granted before the crash hold a token that existed only in lost memory → they stall non-terminally; IMPLICIT_SPEC explicitly defers this to A2 (O8 edge cases: "reconcile deferred to A2, but recorded truth must never claim success") and the implementation never records success for them. Nothing in the registry is truth.

**PASS** for this check as scoped (durable source + rebuild path both exist and are cited). Related but distinct O9/cleanup defects (process leak on close, the JVM-global state atom) are recorded as S8/S9 in the spec-conformance section and counted in the verdict there — they are lifecycle violations, not rebuildability violations.

## No reimplementation of built-in operations
<!-- Scan the module source for any custom code that duplicates functionality already provided by Rama's built-in namespaces. -->

**Site:** `select-pstate-one` (`compute.clj:707-709`) = `(first (foreign-select path pstate))` — a hand-rolled `foreign-select-one`, which is a built-in (`paths.md:33`), used by every read helper (`:711-728`).

Other scanned candidates: `append-bounded` (no built-in Clojure-side ring vector), `daemon-thread-factory` (plain Java), `current-task-id` (uses the built-in, `:631`), `now-ms`/`random-id` (delegate to project core). Only the one duplication.

**FAIL** (mechanical swap).

## Spec conformance findings (beyond the template checks)

Phase-doc requirement: validate against the spec, not just the plan. Findings not already
fully covered above, plus consolidation of severity:

**S1 — Nil-token authorization bypass. MAJOR.**
`authorized-observation?` (`compute.clj:392-396`) tests `(= (:claim-token run-row) (:claim-token obs))`. A `:pending` row has `:claim-token nil` (`compute.clj:282`). An observation submitted **without** a `:claim-token` key yields `(= nil nil)` → authorized. Trace: run R is `:pending`, never claimed. Anyone appends `{:run/id R :observation/type :exit :sequence 0 :exit-code 0}` (no token) → authorized → type valid → seq 0 = expected → applied → R becomes `:succeeded` **without ever being claimed or executed**; a tokenless `:started` flips it to `:running` with an attacker-chosen pid. The inbox entry survives (only a grant removes it), so the executor then claims forever-ungrantable state (infinite claim loop as in the partial-failure trace). This directly violates the O3 authorization invariant ("wrong token, **or any token while no grant exists**, must NOT mutate run output/status") and the spec's exact `:pending × observation` matrix row. Fix is a guard restructure (grant-existence check), not a line tweak.

**S2 — Duplicate submit / submit retry regression → double spawn. MAJOR.** (Full trace in the idempotency check.) Violates O1 "never regress to `:pending`", O2 single-grant, O8 at-most-one-spawn.

**S3 — Claim partial failure → permanent stale inbox + infinite claim-append loop. MAJOR.** (Full trace in the partial-failure check.) Violates O7's iff-invariant.

**S4 — Unknown-run observations create run/view state. MAJOR.** (Trace in the idempotency check.) Violates O3 edge case and the `does-not-exist × observation` matrix row.

**S5 — Heartbeat breaks the run. MAJOR for any spec-compliant external executor.**
Spec O3 lifecycle effects and the matrix (`:running × :heartbeat → liveness timestamp updated, no status change`) include `:heartbeat`; observations carry one shared monotonic sequence. Implementation: `:heartbeat ∉ observation-types` (`compute.clj:47-48`) → `fold-observation` records `:observation/type-invalid` (`:460-461`) **without advancing `:last-seq`**. Trace: authorized heartbeat at seq 2 → error, expected stays 2 → stdout seq 3 buffers (`:473`), exit seq 4 buffers → gap never fills → run stuck `:running` forever, buffer grows. Even under the weakest spec reading ("named, must not be precluded"), an observation type the docs name causes permanent run wedging — that is preclusion. There is also no `:last-heartbeat-ms` field at all (`compute.clj:266-291`), which A2 stall detection is specified to need.

**S6 — Buffered duplicate with different payload corrupts.** Spec O3 edge: "duplicate sequence numbers with different payloads (must keep the first)". The watermark protects only *applied* seqs; while a seq is still buffered, `(assoc-in run-row [:obs-buffer seq-id] obs)` (`compute.clj:473`) **overwrites** the first payload with the second, which is then applied on drain. Minor (requires a misbehaving authorized writer) but spec-listed.

**S7 — Post-terminal `:all-after` replay falsifies the audit trail.** (Trace in the idempotency check.) Genuine not-authorized entries can be evicted by up to 50 retry-generated false ones.

**S8 — O9 close leaks OS processes and fabricates terminal truth.**
`close` (`compute.clj:598-602`) / `close-executor-state!` (`:549-554`) call `shutdownNow` on the pools but never touch live child processes. Trace: close while a command runs → worker thread interrupted at `.waitFor` (`compute.clj:848`) → `InterruptedException` caught by `catch Throwable` (`:863`) → appends synthetic `:stderr` + `:exit 127` (`:864-865`) for a process that was **never destroyed and is still running** → process leak (O9: "cleanly stop executor loops and worker processes so test runs don't leak threads/processes") plus a recorded `:failed`/127 terminal state for in-flight work. It never claims false *success* (the spec's hard line), but it leaks the process and only the manual-timeout path calls `destroyForcibly` (`:852`). Pump threads (`:840-847`) are unmanaged daemon threads that block until the leaked process exits.

**S9 — Cross-runtime executor teardown.** `!compute-executor-task-global-states` is a JVM-global atom (`compute.clj:538-539`) keyed by `System/identityHashCode` (`:578,599`); `close-compute-runtime!` (`:678-684`) calls `close-all-compute-executor-states!` (`:556-561`), which closes **every** executor in the JVM — concurrent runtimes (parallel tests, or a second module instance) kill each other's executors. Identity-hash keying is also collision-unsafe. Harness-level, but it is module code.

**S10 — Dominant-path write amplification.** (Counted under Select-compute-transform.) Whole-row read + two whole-row writes per output line, vs the spec's O3 requirement that per-observation cost be "small and constant" and the skill rule "never trade I/O efficiency for code simplicity". The cost is constant (caps) but carries 10-100x avoidable serialization on the 10³-10⁵/run path, plus a redundant `(|hash *run-id)` after each source (`compute.clj:626,642,655`) on depots already partitioned by `:run/id` (`:609-611`).

**What was verified to hold (with traces):**
- **Token secrecy (O6):** every `$$compute-views` write goes through `run-view` (`compute.clj:311-317`), a fixed `select-keys` that includes neither `:claim-token`, `:obs-buffer`, nor `:last-seq`; write sites `:636,649,660` all use it. `observation-error` (`:374-380`) never stores the offending token. Holds by construction.
- **Single-grant under in-order processing without retries:** per-run task serialization + the `:pending` guard (`:339-344`) → first claim wins, competing/redelivered claims no-op, grant fields never overwritten (matches the double-claim matrix row) — *conditional on S2 never firing*.
- **Claims never create state:** `grantable-claim?` on a nil row is false (`:341` — `(= :pending (:status nil))`) → no write (matrix `does-not-exist × claim`).
- **Out-of-order core:** seq-1-before-0 buffers (`:473`), applies in order on gap fill via `drain-observation-buffer` (`:441-450`); `:exit` as the only observation closes the run from `:launching` (authorized, seq 0 = expected, `:424-429`).
- **Spawn-after-grant protocol:** both executor paths spawn only after reading a committed grant — automatic: `reconcile-awaiting-claims!` classifies via `claim-state` on a foreign read and only `:granted-to-us` submits work (`:512-515`); manual: `run-one-pending-local!` requires `:granted-to-us` from `await-claim-resolution` before `run-granted-command!` (`:883-886`).
- **Rejected requests:** decision only, no run row, no view, no inbox (the `<<if` at `:631` gates all three) — never claimable, never pending-looking.
- **Manual-path timeout:** `destroyForcibly` + exit 124 → terminal `:failed` (`:848-853`); spawn failure → `:exit` 127 → terminal `:failed` (`:863-865`). Hard requirements of ambiguities 5/6 met.

## Verdict

**major-fail.**

Justification: at least five failures require structural change rather than line edits —
S1 (authorization guard admits tokenless writes to ungranted runs), S2 (stream submit
branch with no dedup anchor regresses granted/terminal runs and re-arms double spawn),
S3 (grant/removal pair is not atomic under stream and the removal is permanently skipped
on retry, leaving a stale inbox and an infinite claim loop), S4 (observation path
materializes state for unknown runs), and S5 (heartbeat wedges runs permanently) — the
first three sit exactly on the spec's core safety properties (single grant, at most one
spawn ever, inbox-iff-pending). The plan's own microbatch + dedup-guard design existed to
prevent S2/S3 and was not implemented. Minor failures (consecutive keypath,
`foreign-select-one` reimplementation, error-ring sizing) are real but immaterial next to
these.

`major-fail`
