# Implementation Validation (post-fix, batches 1–3)

<!-- Phase 4, fresh-context adversarial pass against the fix-session output.
     Validates src/app/server/rama/dogfood/compute.clj against IMPLICIT_SPEC.md
     and PLAN.md (v2). Shared guard helpers traced in
     src/app/server/rama/core.clj (decision-dedup-gate, with-request-fingerprint,
     authorize-mutation, bounded-dead-letter; core.clj:405-660).
     Scope rule applied: every failure tagged [batch-1-3-scope] (this session's
     responsibility) or [deferred-batch-4-6] (known deferred work: Option-B
     schemas, query-topology views, admission caps + env allow-list, 4 KiB line
     cap, hygiene/mechanical items, test restructure). -->

Module: `compute-module` — `src/app/server/rama/dogfood/compute.clj`
Topology: `compute-run-command-topology` (microbatch, compute.clj:703), sources
`*compute-depot`, `*compute-claim-depot`, `*compute-obs-depot` (all
`hash-by :run/id`, compute.clj:689-691); owns `$$compute-runs`,
`$$compute-decisions-by-run-id`, `$$compute-pending-by-task`, `$$compute-views`
(compute.clj:704-707).

## Redundant conditionals

**Check:** if every branch of an `<<if`/`<<cond`/`<<switch` does the same operation with only a variable differing, replace with a single operation.

**Trace:** Two `<<if`s exist, both single-branch guards: submit-accepted (compute.clj:729-739) gates run-row/view/inbox writes behind `(decision-accepted? *decision)`; claim-grantable (compute.clj:747-754) gates grant/view/inbox-removal behind `(grantable-claim? *run-row *claim)`. Neither has a second branch, so there is no same-operation-different-variable duplication. The `case` forms in `apply-observation-effect` (compute.clj:405-445) and `fold-observation` (compute.clj:498-531) are plain Clojure with genuinely different operations per branch.

**PASS.**

## Consecutive keypath

**Check:** `(keypath *a) (keypath *b)` → `(keypath *a *b)`.

**Trace:** Two violations, both on `$$compute-pending-by-task`:
- compute.clj:739 — `[(keypath *executor-task-id) (keypath *run-id) (termval *pending-entry)]`
- compute.clj:754 — `[(keypath *executor-task-id) (keypath *run-id) NONE>]`

Both must be `(keypath *executor-task-id *run-id)` per the rule (paths.md:154: "Prefer multi-arity — `[(keypath *a *b *c) ALL]` not `[(keypath *a) (keypath *b) (keypath *c) ALL]`"). Behavior is equivalent; the single-arity chain is the form the skill forbids. These lines are inside the submit/claim paths this fix session rewrote for atomicity, so it is in-session code.

**FAIL [batch-1-3-scope]** — two-line mechanical fix (F4 below).

## Select-compute-transform

**Check:** `local-select>` → compute → `local-transform>` with `termval` — replace with `+compound`/aggregator when possible.

**Trace per path:**
- Submit (compute.clj:722-728): selects the stored decision, computes the dedup gate, writes the decision with `termval`. The selected value also drives `filter>` dataflow branching (compute.clj:725) and the `<<if` (compute.clj:729); the write value is consumed downstream by `initial-run-row`. An aggregator cannot produce the branching, so the pattern is not replaceable.
- Claim (compute.clj:746-754): the selected `*run-row` feeds `grantable-claim?` branching, the grant write, the `$$compute-views` write, AND `(|hash *executor-task-id)` partition routing (compute.clj:753). Not replaceable by an aggregator — the computed row is needed for three downstream consumers.
- Observation (compute.clj:765-771): the folded row feeds the `identical?` no-op skip (compute.clj:768), the `$$compute-runs` write, and the `$$compute-views` write. With two PState targets and a skip filter, a single `+compound` cannot express it.

**PASS** — every select-compute-transform result has multiple downstream consumers or controls dataflow branching; none is a pure single-PState read-modify-write replaceable by an aggregator.

## Unnecessary nil->val

**Check:** no `nil->val` unless the next navigator requires a non-nil value.

**Trace:** `grep nil->val` — zero occurrences in the module.

**PASS.**

## :allow-yield?

**Check:** topology-side selects iterating subindexed structures over ~100 entries need `{:allow-yield? true}`.

**Trace:** All three `local-select>` calls (compute.clj:722, 746, 765) are single-`keypath` point lookups returning one row — no `ALL`/`MAP-KEYS`/range navigators, no subindexed structures anywhere in the schemas (compute.clj:704-707). No iteration to yield from. (The unboundedness of what those point lookups return is a schema problem, covered in the next check, not an `:allow-yield?` problem.)

**PASS.**

## Non-subindexed collections without size limits

**Check:** every write to a non-subindexed inner collection must have an explicitly enforced maximum size, else it must be subindexed.

**Trace, collection by collection:**

Capped (pass):
- `:stdout-tail`/`:stderr-tail` — every write goes through `append-bounded` with limit 200 (compute.clj:34-35, 376-382, 415, 420). Entry-count cap enforced.
- `:observation-errors` — `append-bounded` limit 50 (compute.clj:36, 396-399). Entry-count cap enforced.
- `:obs-buffer` — explicit cap: `(>= (count (:obs-buffer run-row)) obs-buffer-limit)` → buffer-overflow error instead of insert (compute.clj:40, 527-528). Enforced.

Uncapped (fail):
1. `$$compute-pending-by-task` inner map `{String (map-schema Keyword Object)}` (compute.clj:706): one entry per pending run per inbox key (compute.clj:739). A dangling executor hint or dead inbox accumulates without bound (the spec itself says a hinted run "waits forever", IMPLICIT_SPEC.md:99-101) and there is no cap code. PLAN.md requires this subindexed (PLAN.md:123-128). Reading it back (`read-pending`, compute.clj:840-845) deserializes the whole inner map.
2. `:argv` vector and `:cwd` string in `$$compute-runs` rows (compute.clj:283-284) and embedded in the decision row's `:event` payload (compute.clj:241-251, 218-239): no admission size caps anywhere in `request-validation-errors` (compute.clj:137-199). PLAN.md's enforcement mechanism (argv ≤1024 elems/≤128 KiB, cwd ≤4 KiB, reject `:request/spec-too-large`; PLAN.md:117, 168) is absent.
3. Rejection `:errors` embedding raw values: `rejected-decision` stores validation errors carrying the offending values verbatim (e.g. `{:type :payload/argv-invalid :value argv}`, compute.clj:190-192, 263) into `$$compute-decisions-by-run-id` — an over-large argv lands in the decision row precisely when it is rejected.
4. Per-line byte cap: no 4 KiB truncation at the pump (compute.clj:919-932 — docstring explicitly marks it "Batch-4 anchor") nor kernel-side on ingest, so a single huge line is stored whole in the tail, the view, and the depot record.
5. `:env` allow-list: absent entirely from request shape and validation (W1 requires it, PLAN.md:33).

**FAIL [deferred-batch-4-6]** — all five are exactly the deferred Option-B-schema / admission-cap / line-cap work (D1, D2, D8 below). No uncapped collection outside the declared deferred scope was found.

## Stream topology idempotency

**Check:** for each stream topology, trace retry behavior of every write and side effect.

**Trace:** There are no stream topologies — the single topology is microbatch (compute.clj:703). Microbatch gives exactly-once PState updates across retries of the same microbatch ID (microbatch.md:37, 125), so topology-side retry cannot double-apply any write. No IDs are minted inside the topology: `:run/id`, `:request/id`, decision id, and event id are all deterministic functions of the depot record (compute.clj:214-216, 220-221, 241-264) — a replayed attempt recomputes identical keys. (`(now-ms)` in `decided-at`, compute.clj:251/264, varies per attempt, but failed attempts' writes are never committed — prime resets PStates, microbatch.md:44-46 — so no divergent value can ever be observed.) No `depot-partition-append!` anywhere in the topology.

The remaining duplicate source is client-level duplicate depot records, handled by guards; traced individually:
- **Duplicate submit:** stored decision read (compute.clj:722) → `core/decision-dedup-gate` (core.clj:472-496) → `filter> (= :proceed ...)` (compute.clj:725). Identical redelivery → same fingerprint → `:replay` → filtered: no decision flip, no run-row touch (no `:pending` regression re-arming spawn), no inbox re-add. Same run-id with different content → `:conflict` → filtered: committed decision untouched. Two submits for the same run-id inside ONE microbatch also dedup: records process sequentially on the run's task during the process phase, and the second record's `local-select>` sees the first's in-attempt write (prime/process/commit model, microbatch.md:44-46).
- **Redelivered winning claim:** row status is already `:launching` → `grantable-claim?` false (compute.clj:349-354) → `<<if` skips → grant fields never overwritten. Losing/competing claim: same no-op; first claim processed on the run's task wins deterministically. Claim against absent row: `grantable-claim?` false on nil row → no state created.
- **Redelivered observation (same `:sequence`):** `authorize-mutation` classifies `seq ≤ watermark` as `:replay` (core.clj:646-648) → `fold-observation` returns the identical row (compute.clj:499-500) → `identical?` filter (compute.clj:768) skips both PState writes. Buffered duplicates: store-if-absent via `contains?` check (compute.clj:524-525) — first payload wins.

**PASS** — no stream topology; exactly-once covers topology retries; all three client-duplicate guards trace to no-ops. (A separate guard-chain hole unrelated to retries — buffered-drain past terminal — is reported under Spec conformance, F1.)

## Partial failure in stream topologies

**Check:** can a partial commit + retry leave any multi-partition write permanently unexecuted?

**Trace:** The two cross-partition writes are the submit pair (decision + run row + view on hash(run-id), then `(|hash *executor-task-id)` → inbox entry; compute.clj:728-739) and the grant pair (grant CAS + view on hash(run-id), then `(|hash ...)` → inbox removal; compute.clj:751-754). Both execute inside one microbatch attempt; commit is atomic across partitions (microbatch.md:46-47, 124). A failure mid-attempt discards all of the attempt's writes (prime resets to the previous microbatch state) and the retry redoes the whole pair. The stream-mode hazard the plan calls out (decision committed → replay hits the dedup gate → inbox add skipped → orphaned run; PLAN.md:161-162) cannot occur: the dedup gate and the inbox add commit or vanish together.

**PASS.**

## Single depot append per client operation

**Check:** each client write operation calls `foreign-append!` exactly once.

**Trace:** `append-run-command!` (compute.clj:797-808), `append-claim!` (compute.clj:810-815), `append-observation!` (compute.clj:817-822) each contain exactly one `foreign-append!`. The multi-append flows (`run-granted-command!` streaming observations, `run-one-pending-local!` claim-then-observe) are multi-*operation* protocols where each step is independently meaningful and the executor awaits committed state between steps — exactly the externally-serialized protocol PLAN.md:146/198 specifies, not a single logical operation split across appends. A crash between steps leaves a consistent intermediate state (e.g. claimed-but-unspawned → the sanctioned stall arm). No topology-internal `depot-partition-append!`.

**PASS.**

## Application-state caches survive restart

**Check:** every TaskGlobal/in-process cache must have a named durable source and a concrete rebuild path.

**Trace:** The `ComputeExecutorTaskGlobal` (compute.clj:640-682) holds: claim/in-flight `registry`, live-`processes` map, scheduler and worker pools, and per-run claim tokens.
- Durable sources: `$$compute-pending-by-task` (pending assignments survive restart; written compute.clj:739, removed only at grant compute.clj:754) and `$$compute-runs` (grant truth: status/claimed-by/claim-token, compute.clj:751).
- Rebuild path, cited: `prepareForTask` starts the reconcile loop (compute.clj:667-677); each tick `claim-new-pending-runs!` re-reads the stable inbox key via `read-pending` (compute.clj:581-584) and re-claims anything not in the (empty-after-restart) registry with a fresh token (compute.clj:586-598); `reconcile-awaiting-claims!` re-classifies grants from durable `$$compute-runs` via `claim-state` (compute.clj:562-579, 366-374).
- Loss cases: a pre-restart granted token existed only in lost memory → the run stays `:launching`/`:running`, non-terminal — the spec-sanctioned A.0 stall (IMPLICIT_SPEC.md:289, PLAN.md:204-207); truth is never fabricated. Double-spawn after restart is excluded even though the executor id is reproducible (`"compute-executor-" task-id`, compute.clj:645): the grant check requires identity AND token (compute.clj:370-372), the token is a fresh UUID per claim (compute.clj:334), and the write-once grant (claim guard) means at most one (id, token) pair in history ever classifies `:granted-to-us`.
- Registry memory is released on terminal/conflict (compute.clj:557-560, 578-579), and the registry-before-append ordering with rollback-on-failed-append (compute.clj:592-598) keeps the at-most-one-claim-in-flight property across ticks.

**PASS** — every cached datum is rebuildable from `$$compute-pending-by-task` + `$$compute-runs` or is a sanctioned-loss handle (Process objects, tokens).

## No reimplementation of built-in operations

**Check:** no custom code duplicating Rama built-ins.

**Trace:** `select-pstate-one` (compute.clj:824-826) is `(first (foreign-select path pstate))` — a hand-rolled `foreign-select-one`, which Rama provides (used throughout the skill, e.g. microbatch.md:138). All seven client reads route through it. PLAN.md R1/R5 explicitly name `foreign-select-one` as the access method. Everything else checked clean: `append-bounded`, `await-materialized`, pump threads have no Rama built-in equivalents; `now-ms`/`random-id` delegate to core.

**FAIL [deferred-batch-4-6]** — mechanical one-liner squarely in the deferred "hygiene/mechanical items" batch (D5 below); behaviorally equivalent for single-key paths.

## Plan divergences (all expected per the declared deferred scope)

Validated against PLAN.md §PState Design / §Query Topologies / §Design Decisions. Each is a real divergence and a real spec exposure, reported honestly; all fall inside the declared batch-4-6 deferral.

- **D1 [deferred-batch-4-6] — whole-row `(map-schema Keyword Object)` schemas** (compute.clj:704-707) instead of Option-B `fixed-keys-schema` + subindexed `:stdout`/`:stderr`/`:buffered` + `IObservation` records (PLAN.md:72-117). Consequence traced: every stdout line rewrites the entire run row AND the entire view row (compute.clj:770-771) — the write-amplification profile PLAN.md:65-66 rejected as Option A. At the spec's stated 10³–10⁵ lines/run (IMPLICIT_SPEC.md:147-150) per-observation cost is O(row), not the required "small and constant". Bounded in entries (caps above) but not in cost.
- **D2 [deferred-batch-4-6] — admission size caps absent** (argv/cwd/env; see Non-subindexed check, items 2-3, 5).
- **D3 [deferred-batch-4-6] — materialized `$$compute-views`** (compute.clj:707, 737, 752, 771) instead of `run-record`/`run-view` query topologies (PLAN.md:132-138, 181-189). Doubles every dominant-path write. Token secrecy nevertheless holds as-built — traced below under Spec conformance (S1).
- **D4 [deferred-batch-4-6] — error ring cap 50 vs plan 100, and `:observation-error-count` total counter missing** (compute.clj:36 vs PLAN.md:109-110, 116). Spam volume past the cap is not auditable (plan's ambiguity-7 resolution unimplemented).
- **D5 [deferred-batch-4-6] — `select-pstate-one`** (see check above).
- **D6 [deferred-batch-4-6] — global executor-state registry**: `!compute-executor-task-global-states` is a JVM-global atom keyed by `identityHashCode` (compute.clj:605-606, 657-666), and `close-all-compute-executor-states!` (compute.clj:633-638) nukes every state in the JVM, not the closing runtime's. The named deferred "per-instance executor state" item. Single-runtime test cycles are correct; concurrent runtimes in one JVM would interfere.
- **D7 [deferred-batch-4-6] — executor identity lacks the per-launch nonce** (compute.clj:645 vs PLAN.md:209). Safety consequence traced under the cache-rebuild check: none — fresh per-claim tokens carry the disambiguation. Belt-without-braces, not a hole.
- **D8 [deferred-batch-4-6] — `:env` allow-list absent** from the request shape (compute.clj:114-117 vs PLAN.md:33; IMPLICIT_SPEC.md:65 names "env constraints" in the request shape).
- Non-failure divergences noted for the record: depot names (`*compute-depot`/`*compute-obs-depot` vs plan's `*compute-request-depot`/`*compute-observation-depot`) — cosmetic; drain loop is a plain Clojure loop bounded by the 1024 buffer cap instead of `loop<-` + `yield-if-overtime` (PLAN.md:175) — bounded, so starvation is capped; view carries extra non-secret fields (argv/cwd/claimed-by) beyond the plan's field list — spec only forbids the token; inbox values are entry maps rather than a set — membership semantics (`contains?`) preserved.

## Spec conformance findings (beyond template checks)

Traced against IMPLICIT_SPEC.md operations/matrix. Passing traces first, then failures.

**S1 — Token secrecy (O6): PASS.** `run-view` (compute.clj:321-327) whitelists fields; `:claim-token`, `:obs-buffer`, `:last-seq` are not in the list. All three `$$compute-views` writers go through `run-view` (compute.clj:734/737, 750/752, 769/771). `pending-entry` (compute.clj:313-319) carries no token (none exists pre-claim). Error entries never carry the offered token (compute.clj:384-390). `(not (contains? view :claim-token))` holds by construction on every write path.

**S2 — Decision semantics (O1/O4): PASS.** Exactly one decision per run-id, never flips (dedup gate filters every redelivery/conflict); echoes `:request/type` (compute.clj:247, 259); rejected ⇒ decision only, no run row, no view row, no inbox entry (`<<if` compute.clj:729 guards all three) — a rejected request can never look pending or be claimed; acceptance never waits on execution (the topology only writes state; spawning lives in the TaskGlobal); target-kind check present (compute.clj:181-184, 45-46); blank run-id refused client-side (compute.clj:805-806) and dropped in-topology (compute.clj:721); empty argv rejected (compute.clj:190); normal path assigns the current task's own executor inbox (compute.clj:303-307, 731-732) — a live inbox, since `declare-object` instantiates an executor on every task.

**S3 — Claim protocol (O2/O5): PASS** with one tension noted. Single grant: first `:pending`-state claim processed on the run's task wins; all later claims no-op; unknown run-id creates nothing; same-token-different-id classifies conflict (compute.clj:370-372). Tension: `grantable-claim?` additionally requires the claim's `:executor/task-id` to equal the row's assignment (compute.clj:353) — stricter than spec O2's "first claim processed wins". A claim minted before the run row is visible carries no task-id and can never win; the claimant sees `:not-yet-processed` indefinitely rather than ever winning or conflicting. The exercised paths are immune (`claim-run!` back-fills the assignment from the row, compute.clj:895-899; the automatic executor claims only from its own inbox, compute.clj:586), and the spec's claim shape includes the assignment id, so kernel-side validation of it is defensible. Recorded as a documented tension, not a failure: no invariant in the spec's matrix is violated (the run stays claimable; a retry-minted claim wins normally).

**S4 — Observation guard chain, in-order paths: PASS.** Traced per the matrix: unknown run-id dropped before the fold, no state, no NPE (compute.clj:764-766); `:pending` × any token → `:no-claim-granted` → `:observation/not-authorized` error, status untouched, still claimable (core.clj:633-634, compute.clj:503-505); wrong token → `:token-mismatch` → same error, tails untouched; authorized `:started` → `:running`+pid; `:stdout`/`:stderr` → bounded tails in seq order; `:heartbeat` → liveness only; in-order `:exit` → terminal from exit code, `:exit` legal from `:launching` (the "died before :started" edge); `:failed` → `:failed`+`:failure-reason` (ambiguities 5/6 per plan); unknown type from authorized writer → auditable error AND watermark advances, stream never wedges (compute.clj:442-445, 449-453); seq-replay ignored with first-payload-wins; gap → store-if-absent buffer with capped overflow error; post-terminal authorized → silent ignore, post-terminal unauthorized → auditable error (auth-before-terminal order, compute.clj:466-470) — matrix-compliant ("either is acceptable, mutation [of the terminal result] is not"). Every no-op returns the identical row and skips both writes (compute.clj:768).

**S5 [batch-1-3-scope] — F1: buffered drain applies observations past a terminal transition — terminal immutability violated. FAIL.**
`fold-observation` checks terminal status only at fold entry (compute.clj:514), but `drain-observation-buffer` (compute.clj:455-464) applies every contiguous buffered observation with no terminal check, and `apply-observation-effect :started` sets `:status :running` unconditionally (compute.clj:406-411). Concrete trace: authorized writer appends seq 1 = `:exit {:exit-code 0}` and seq 2 = `:started {:pid p}` while expected seq is 0 — both buffer (row non-terminal at fold entry each time). Seq 0 arrives → applied → drain applies seq 1 (status `:succeeded`) → drain continues → applies seq 2 → status `:running`. A terminal run is reopened and stays `:running` forever; milder variants append output to a closed run's tails. Reachable through the public `append-observation!` surface by any holder of the granted token (the test surface does exactly such manual out-of-order appends); the as-built executor cannot emit it (its appends are strictly sequential, so cross-seq reordering cannot occur), but the spec requires reads to *never* observe an invariant-violating state and lists terminal immutability as a hard invariant (IMPLICIT_SPEC.md:168-169). This is the observation guard chain — batch 2 of this session. Fix is line-level: stop the drain (or skip application) once `(contains? terminal-statuses (:status row))`.

**S6 [batch-1-3-scope] — F2: observation-error entries copy attacker-controlled fields unbounded; the guard discards `authorize-mutation`'s bounded dead-letter. FAIL.**
`observation-error` (compute.clj:384-390) stores `(:observation/type obs)`, `(:sequence obs)`, and `(:observed-at obs)` verbatim. For an UNAUTHORIZED writer (no token needed — wrong-token spam is the spec's named abuse case, IMPLICIT_SPEC.md:174-176) these are arbitrary attacker-supplied values: a 1 MiB blob as `:observation/type` lands in the errors vector, hence in the run row AND the view, on every spam record. The ring caps entry COUNT at 50 but not entry BYTES, so the "observation-errors list must be bounded" requirement holds only in entries, not size — and each spam record rewrites the whole (now huge) row. Meanwhile `authorize-mutation` already returns `:auth/dead-letter` built by `bounded-dead-letter` with a 512-char preview cap designed for exactly this (core.clj:554-571, 603), and `fold-observation` throws it away (compute.clj:498-511), hand-rolling the unbounded entry instead. Guard chain = batch 2 scope. Fix is line-level: build the error entry from sanitized/truncated fields or reuse the dead-letter preview.

**S7 [batch-1-3-scope] — F3: pump threads can outlive `close` spinning in the append retry loop — O9 clean-close violated in a race window. FAIL.**
`append-process-observation!` retries a failed append forever with capped backoff (compute.clj:956-968); only `InterruptedException` exits (compute.clj:960-961). Pump threads are raw daemon threads (compute.clj:1002-1011, 934-942) — not in the `workers` pool — so `close-executor-state!` (compute.clj:616-631) never interrupts them; it relies on `destroyForcibly` producing EOF. Trace the race: runtime close while a process is streaming → executor states closed (process killed) → `(.close ipc)` (compute.clj:789-795); a pump that read a line before EOF and is now inside the retry loop gets `Throwable` from the closed IPC → `false` → sleeps → retries forever. Result: one permanently spinning (≤1.6 s period) daemon thread per affected pump, accumulating across repeated test start/close cycles — exactly what O9 forbids ("close must cleanly stop executor loops ... so test runs don't leak threads", IMPLICIT_SPEC.md:300-307). Process lifecycle = batch 3 scope. Fix is localized: track pump threads in the executor state and interrupt them in `close-executor-state!`, or bound the retry loop with a closed-flag check.

**S8 — Liveness/latency (O8): PASS (static).** Reconcile delay 50 ms (compute.clj:38), grant resolution ≤1 microbatch cycle, observation poll 25 ms (compute.clj:857) — submit→terminal for `echo` fits the 5 s bound with margin per the plan's budget (PLAN.md:158). Not runtime-verified (this phase is static tracing); no structural blocker found. Note: post-`:exit` lines from a pump that outlived its 2 s join (compute.clj:1016-1017) carry later seqs and are silently terminal-ignored — documented executor tradeoff ("can delay log tails", compute.clj:978), bounded-tail semantics make it spec-acceptable.

## Self-consistency check

Re-read performed. Every check whose body contains a gap/tradeoff that violates a requirement is marked FAIL (Consecutive keypath; Non-subindexed; No-reimplementation; S5/S6/S7). Items described as tradeoffs but verified spec-acceptable (post-exit line drop under bounded tails; assignment-validation tension with claimant retry intact; missing nonce with token-based safety intact; bounded plain-loop drain) are explicitly traced to a satisfied requirement, not waved through. No PASS entry above asserts an unresolved gap.

## Verdict

**Failure inventory:**

[batch-1-3-scope] — 4 failures, all line-level fixes in the existing module:
- F1 (S5): buffered drain applies past terminal — terminal-immutability hole in the guard chain.
- F2 (S6): unbounded attacker-controlled bytes in observation-error entries; bounded dead-letter discarded.
- F3 (S7): pump-thread infinite append-retry leak on close (O9).
- F4: consecutive keypath at compute.clj:739, 754.

[deferred-batch-4-6] — 8 findings (D1–D8): Option-B schemas absent (whole-row Object rows, non-subindexed tails/buffer, per-line whole-row write amplification on the dominant path), unbounded pending-inbox inner map, admission size caps + env allow-list absent, 4 KiB line cap absent, materialized `$$compute-views` instead of query topologies, error cap 50 + missing total counter, `select-pstate-one` reimplementation, global executor-state atom, missing identity nonce.

**Verdict: `major-fail`** — per the rubric, the strongest single failure governs: D1/D3 (Option-B schema restructure and dropping the materialized view for query topologies) require restructuring the module's PState schemas and read surface, not line edits. These are, however, the explicitly declared deferred batch-4-6 scope.

**Conditional verdict excluding [deferred-batch-4-6] findings: `minor-fail`** — every batch-1-3-scope failure (F1–F4) is fixable by editing specific lines: a terminal check inside `drain-observation-buffer`, field truncation (or dead-letter reuse) in `observation-error`, pump-thread tracking/interruption in `close-executor-state!`, and two keypath merges. The fix session's core deliverables — submit dedup + microbatch atomicity (traced airtight), the authorization/replay/buffer guard order, and the acked-cursor observation pipeline — all validated correct; the four residual failures are edge-hardening within the same components.

---

## Fix disposition (session orchestrator addendum, post-verdict)

Per the phase rubric, the conditional in-scope verdict (`minor-fail`) takes the
localized-fix path without a full phase re-run. All four [batch-1-3-scope]
failures were fixed in the same session, immediately after this validation:

- **F1 fixed** — `drain-observation-buffer` now carries a terminal fence: the
  moment the row turns terminal mid-drain, every remaining buffered entry is
  discarded unapplied and the buffer is cleared. Pinned by probe **B2-P6**
  (buffered `:started` after buffered `:exit` → run stays `:succeeded`, pid
  nil, buffer empty).
- **F2 fixed** — `observation-error` routes copied record fields through
  `bounded-audit-field` (small scalars only, 64-char cap, garbage → nil) and
  `:observed-at`/`:updated-at` only accept numbers. Pinned by probe **B2-P7**
  (100 KB hostile type/sequence/timestamp → bounded audit entry, truth
  untouched).
- **F3 fixed** — the executor's process registry now stores
  `{:process p :pump-threads [t1 t2]}`; `close-executor-state!` destroys live
  processes AND interrupts live pump threads, releasing any pump stuck in the
  append-retry loop after IPC close. (Manual-path pump threads remain
  untracked — test-client scope, noted for batch 5.)
- **F4 fixed** — both inbox writes use the fused
  `(keypath *executor-task-id *run-id)`.

Post-fix evidence: full compute suite + probe matrix green
(`{:test 5, :pass 97, :fail 0, :error 0}`), other kernel suites green
(29 tests/304 assertions and 41 tests/342 assertions).

The 8 [deferred-batch-4-6] findings (D1–D8) are intentionally NOT fixed here;
they are the declared scope of the follow-up session (FIX_PLAN batches 4–6)
and this artifact is their tracking record.
