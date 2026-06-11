# Plan Validation — Round 2

<!-- Phase 2 (retrospective), ROUND 2. Adversarial validation of the AMENDED PLAN.md against IMPLICIT_SPEC.md. Default verdict: FAIL. -->

**Round-1 record (one line):** Round 1 FAILED on F1 (`:spec` argv/env had no size-enforcement mechanism and rode the R5 grant-poll hot path), F2 (out-of-order `:buffered` insert was not store-if-absent, so a redelivered sequence with a different payload could overwrite the first), F3 (no per-line byte cap — a huge single line broke the bounded-tail/bounded-view invariant).

Validator stance: code-blind; only inputs are `IMPLICIT_SPEC.md` and the amended `PLAN.md`. The three amendments are validated like everything else — by scenario-tracing, not by trusting the amendment note. Every check quotes the spec verbatim and cites plan lines/sections. PASS only after explicit scenario-tracing.

## Amendments under scenario-tracing (summary; full traces in their blocks below)

- **F1 (admission caps + R5b split).** Trace: submit with 2,000-element argv → submit guard (PLAN §Topologies, submit guard, l.168) rejects with `:decision/status :rejected, :reason :request/spec-too-large`; nothing lands in `$$runs`, so no later read can ever see an over-cap spec. Submit with a 303-element / ~3 KiB link argv → under caps (≤1024 elems, ≤128 KiB) → accepted, as it should be. The enforcement mechanism is now a named code rule at the only admission point, and the rejection is recorded (`:reason` field exists in the `$$decisions` schema, l.53). Hot-poll exposure is gone: R5's submap (l.16) is exactly `:status :claimed-by :claim-token :executor/task-id` — no `:spec`; the executor fetches `:spec` once per granted run via R5b (l.17), bounded ≤ ~164 KiB worst case (128+32+4 KiB, consistent with the caps). **Holds.** Detailed verdicts: PState-schema section, Scale block, O1.g.
- **F2 (store-if-absent buffer).** Trace: next-seq=0; obs seq 2 `:stdout "AAA"` arrives → `:buffered[2]=OutputObs("AAA")`. Redelivered seq 2 with payload `"BBB"` arrives while next-seq is still 0 → guard 4 (l.175): "store in `:buffered` **only if `:buffered[seq]` is absent** (navigate `(keypath seq)`, write only when nil)" → key 2 present → no-op → `"AAA"` kept; drain later applies `"AAA"`. First-wins now holds across the entire sequence window (applied path via watermark, buffered path via store-if-absent). The check-then-write is safe because the event runs single-threaded on the run's task. **Holds.** Detailed verdict: O3.g.
- **F3 (4 KiB per-line cap + truncation marker).** Trace: child prints one ~1.3 GB line → pump thread truncates to ≤4 KiB + explicit marker *before building the observation* (l.210) → the W3 depot record itself is byte-bounded (l.39) → tail entry ≤4 KiB. Defense in depth: kernel-side ingest truncation "before buffering or applying" (l.171) bounds `$$runs` even against a non-conforming *authorized* writer, and bounds `:buffered` entries too (≤1024 × ~4 KiB ≈ 4 MiB worst). Manual path applies the same truncation at capture (l.211). Tails are now bounded in entries AND bytes (≤200 × 4 KiB per stream, l.114). **Holds.** Detailed verdict: O3.h.

## Query topology: run-record (R2)

- Input examples present: **yes** (PLAN §Query Topologies, three examples, l.184-186).
- Example 1 (`:running` run with output): N=3 total reads, M=3 meaningful (fields submap seek + `:stdout` range scan + `:stderr` range scan). N == M? **yes**.
- Example 2 (unknown run-id): N=1, M=1 (fields submap → nil → emit nil, "skip both tail reads"). N == M? **yes**.
- Example 3 (`:pending` run): N=1, M=1 ("fields read shows zero counts → tail reads skipped"). N == M? **yes**.
- Any N > M: no — empty scans are branched away (`<<if` on nil-row / zero counts, l.187).
- M values across examples: 3, 1, 1. All same? **no**.
- If M differs: must be marked variable with dynamic approach. Is it? **yes** — "Fixed or variable: **variable** (1 vs 3). Dynamic approach: conditional branching ... no padded reads" (l.187).
- Partition alignment: leading `(|hash *run-id)` evaluated client-side, `|origin` return (l.188). Correct.

**PASS.**

## Query topology: run-view (R3)

- Input examples present: **yes, by explicit structural reference** — "Same read structure, same variable-read handling as `run-record` (1 vs 3 meaningful reads)" (l.189); same PState, same reads, different assembly, so the three run-record examples apply verbatim.
- Example 1: N=3, M=3 — yes. Example 2 (unknown): N=1, M=1 — yes. Example 3 (`:pending`): N=1, M=1 — yes.
- M values 3, 1, 1 — variable, marked with the same dynamic branching. **yes**.
- Assembly "never touches `:claim-token`/`:next-seq`/`:buffered`" (l.189) — secrecy is structural; no extra reads needed to enforce it.

**PASS.**

## PState schemas

- Any Object type? **no** — "no `Object` anywhere (polymorphic buffer values use `IObservation` + variant records)" (l.179). Verified against all three schemas: `$$decisions` (fixed-keys of Keyword/String/Long), `$$runs` (fixed-keys; typed collections `(map-schema Long String ...)`, `(map-schema Long IObservation ...)`, `(vector-schema ObservationError)`), `$$pending-by-executor` (`{String (set-schema String {:subindex? true})}`). PASS.
- Uniform record-like values use fixed-keys-schema? **yes** — `$$decisions` value, `$$runs` value, nested `:spec` all fixed-keys. PASS.
- Positions where different instances have different fields use definterface + defrecord? **yes** — the only genuinely polymorphic position is `:buffered` values: different observation *kinds* with different field sets (`StartedObs[sequence pid at-ms]` vs `OutputObs[sequence stream line at-ms]` vs `ExitObs[sequence exit-code at-ms]` …), handled by `definterface IObservation` + per-variant defrecords (l.73-82). The fixed-keys positions are single-kind records whose declared field set is identical across instances; fields like `:claimed-by`/`:pid`/`:exit-code` are lifecycle-progressively populated, not variant-discriminated — no type dimension splits `$$runs` rows into different record kinds (same for `$$decisions`: `:reason` is a nullable attribute of the one decision-record kind). The variant rule is applied exactly where instance kinds differ, and only there. PASS.
- Inner collections that can exceed 100 elements subindexed, with named enforcement for every non-subindexed collection?
  - `$$runs :stdout` / `:stderr`: cap 200 > 100 → subindexed (l.106-107, l.114). Cap enforcement named: dense-index trim "delete `idx - cap` when `idx ≥ cap`" (l.176). Entries additionally byte-bounded by the F3 line cap. OK.
  - `$$runs :buffered`: cap 1024 > 100 → subindexed (l.108, l.115); overflow enforcement named (`:observation/buffer-overflow` error + reject, l.175). OK.
  - `$$runs :observation-errors`: NOT subindexed; enforcement named — "hard app-enforced cap of 100 most-recent entries (ring: drop oldest on overflow)" (l.116); 100 ≤ threshold; rare-path rewrite of a ≤100-entry vector of small token-free records. OK.
  - `$$pending-by-executor` sets: unbounded worst case while stall handling is deferred → subindexed (l.128). OK.
  - `$$runs :spec :argv` and `:env`: NOT subindexed. Named enforcement mechanism: **the submit guard's admission caps** — "the submit guard **rejects** (decision `:rejected`, reason `:request/spec-too-large`) any request with `:argv` > 1024 elements or > 128 KiB total bytes, `:cwd` > 4 KiB, or `:env` > 128 entries or > 32 KiB total bytes" (l.117, restated as the validation rule at l.168). Scenario trace: (a) 10⁶-element argv → rejected, never lands in `$$runs` — "nothing over-cap ever lands in `$$runs`, so every `:spec` read (R5b) is hard-bounded" (l.168); (b) 303-element/~3 KiB link command → accepted (legitimate command, must not be rejected); (c) 150-var CI env allow-list ≤32 KiB → accepted, bounded. The template's failure condition for a non-subindexed collection is "If no enforcement mechanism exists, it is not bounded — subindex it"; an enforcement mechanism now exists, is a specific code rule at the single admission point, and bounds both element count and bytes. On the residual question of whether a 1024-capped collection should be subindexed anyway: these collections are write-once and read-whole (argv/env are only ever consumed in full, once per run at spawn via R5b), so subindexing converts one bounded value read into a seek + up-to-1024-entry range scan per spec fetch and buys no memory benefit the byte cap doesn't already provide — that would violate the skill's I/O-efficiency rule for zero gain. The byte caps, not element counts, are what bound worst-case I/O (≤ ~164 KiB), and round 1's required change ("add explicit size caps ... and cite that validation as the enforcement mechanism — a validation cap is the right fix") is implemented verbatim. OK.
- Hot-path exposure re-check (the second half of round-1 F1): R5's poll submap (l.16, l.21) excludes `:spec` — "the repeated poll must never re-read spec bytes; the executor fetches it with one extra point read (R5b) only after the poll classifies granted-to-us." Spec bytes no longer ride the grant-poll loop. OK.

**PASS.**

## Topologies

- Microbatch unless justified? **yes** — single microbatch topology `compute-kernel` (l.152); the plan runs the skill's four-question check explicitly (latency budget, no `ack-return>`, idempotency/atomicity decisive, throughput; l.156-164).
- Low-latency writes vs microbatch's ≥300ms floor — per write, against verbatim spec text:
  - W1 submit: "Decision and `:pending` run visibility should be sub-second; hundreds of ms is acceptable; **single-digit ms is not required**." Microbatch fits. OK.
  - W2 claim: "Claim-grant visibility bounds time-to-spawn; sub-second expected... the kernel's only obligation is bounded visibility, not push." OK.
  - W3 observation: "observation-to-readable-view should be sub-second; hundreds of ms acceptable." OK.
  - End-to-end against the only hard bound ("submit→terminal ≤ 5 s for `echo`"): submit append→committed `:pending` ≤1 cycle (~200-500 ms) + executor tick (≤250 ms) + claim append→committed grant ≤1 cycle + grant poll (≤100 ms) + R5b spec fetch (~1 ms, one point read — the amendment adds this hop and it is negligible) + spawn+echo (~50 ms) + observations→committed terminal ≤1 cycle ≈ 1.5-2.5 s worst case (l.158). Comfortable margin; no write requires single-digit-ms visibility, so no stream topology is required.

**PASS.**

## Production readiness

- Multiple concurrent clients? **yes.** Same-run-id concurrent submits serialize on hash(run-id)'s task; `$$decisions`-presence guard makes the second a total no-op (l.168). Competing claims serialize on the run's task; first-wins status CAS (l.170). Observations have exactly one authorized writer (token check, l.173). Distinct run-ids are independent rows/tasks. PASS.
- Client process restarts? **yes.** Requester restart → possible duplicate submit → durable dedup anchor. Executor restart → fresh identity+nonce (l.209), re-reads durable inbox, re-classifies via R5; pre-restart grants classify conflict-or-past — stall-safe, no double spawn (l.205-206). Manual-path client stateless per invocation (l.211, l.221). PASS.
- Worker restart at any point during topology execution? **yes.** Microbatch exactly-once: a batch that fails partway retries in full; PState writes from a failed attempt are never durably visible; offsets resume. TaskGlobal rebuild path cited per ground rule (l.219): registry starts empty, re-derives from durable `$$pending-by-executor` (R4) + `$$runs` (R5 3-state check); the only unrecoverable data — live Process handles and in-memory claim tokens — is the spec-sanctioned A.0 stall case ("Recorded truth never claims success for work that didn't finish," l.206), matching the spec's "kernel restart with a live child process (reconcile deferred to A2, but recorded truth must never claim success for work that didn't finish)". PASS.
- Large scale (millions of entities, unbounded growth)? **yes.** Top-level maps keyed by run-id grow forever — point lookups, durable, no deletion, per spec "results must remain readable after completion indefinitely". Every inner collection is either subindexed (tails, buffer, inbox sets) or has a named enforced cap (errors ring ≤100; argv/env/cwd admission caps with `:rejected` over-limit — the round-1 F1 hole is closed). Per-run on-disk size is capped: tails 2×200×≤4 KiB, buffer ≤1024 line-capped entries, errors ≤100, spec ≤ ~164 KiB (l.217). PASS.
- Stream topology non-idempotent writes: none — no stream topologies. The plan separately enumerates the client-level duplicate sources exactly-once does NOT cover (redelivered submit, re-sent claim, re-sent observation) and gives each a guard that is a pure function of committed state (l.166-177). N/A → OK.
- Stream multi-partition partial failure: N/A (no stream topology). The plan inverts the check correctly: it *rejects* stream precisely because submit (decision+run row on hash(run-id); inbox on hash(inbox-key)) and grant (CAS + inbox disj) are cross-partition pairs whose partial-apply-then-replay under stream would orphan a run or leave a stale inbox entry (l.161-162). Microbatch makes both pairs exactly-once-atomic. OK.

**PASS.**

## Cross-topology correctness

- Internal depots between topologies: **none** — one topology, three foreign-appended depots; no topology→depot→topology flow, no `depot-partition-append!` anywhere in the plan.
- Client-level duplicate records per depot (the analogous hazard) each have a dedup mechanism that is a pure function of committed state on the run's task: request depot → `$$decisions` presence (total no-op); claim depot → status≠`:pending` strict no-op; observation depot → `seq < next-seq` watermark ignore + store-if-absent buffer (the F2 amendment closes the buffered-window case). OK.

**PASS.**

## Stream topology correctness

- `depot-partition-append!` in stream topologies: **none exist** (no stream topologies, no internal appends). N/A.

## Spec coverage — trace every operation and constraint

### Visibility contract — bounded-time visibility, 5s end-to-end, reads in any state
- **Source**: "Writes are acknowledged; after acknowledgment the effect must become visible to the corresponding reads within bounded time. The test contract polls ... with an end-to-end upper bound of **5000 ms** from submit to terminal view for a trivial command (`echo`). Reads must be callable at any time, in any entity state, and must never observe a state that violates the invariants below..."
- **Trace**: submit `{:run/id "r1" :argv ["echo" "hello"]}` at t=0, `:append-ack` durable ~t+5 ms. Microbatch commits decision+run+inbox ≤ ~t+500 ms. Executor tick ≤ t+750 ms discovers; claim committed ~t+1.25 s; grant poll passes ~t+1.35 s; R5b spec fetch ~t+1.35 s; spawn+echo ~t+1.4 s; `:started`/`:stdout`/`:exit` committed ~t+1.9 s; `await-view` sees `:succeeded` ≈ t+2 s < 5000 ms. Reads in any state: absent keys navigate to nil (l.22), R2/R3 branch on nil-row/zero-counts → well-formed result in all six lifecycle states; no read can see a token in a view (structural omission) or unauthorized output (auth guard precedes all mutation).
- **Fault-tolerance**: worker restart mid-cycle → batch retries whole; visibility delayed one cycle; exactly-once excludes double-apply; cross-partition pairs commit atomically per batch.
- **Race**: readers see only committed state; batch atomicity means no partial submit/grant pair is ever observable.
- **Flaws found**: none found, with reasoning: every leg of the 5 s path is bounded by a quantified microbatch cycle or poll interval, including the new R5b hop (~1 ms).
- **Verdict**: PASS

### O1.a — Exactly one decision per request
- **Source**: "Every submitted request gets exactly one recorded decision (`:accepted` or `:rejected`) that is readable afterward, echoing `:request/type`."
- **Trace**: submit "r1" → guard reads `$$decisions["r1"]` → absent → validate → write `{:decision/status :accepted :request/type :compute/run-command :request/id ... :decided-at-ms ...}`. Redelivered submit of "r1" 2 s later → present → "total no-op: no decision flip" (l.168). `:request/type` echoed as a schema field (l.52).
- **Fault-tolerance**: decision row durable; microbatch retry exactly-once; decision+run+inbox are one atomic batch — no decision-without-run window.
- **Race**: two concurrent submits of "r1" hash to the same task, processed sequentially → first writes, second no-ops (within-batch, the second event on the same task sees the first's PState write).
- **Flaws found**: none found, with reasoning: `$$decisions` presence is a single durable dedup anchor checked on the same task that writes it.
- **Verdict**: PASS

### O1.b — Accepted ⇒ exactly one `:pending` run + exactly one inbox entry
- **Source**: "An accepted request creates exactly one run in `:pending` and exactly one entry in exactly one executor's pending inbox."
- **Trace**: accept writes run row `{:status :pending :spec ... :executor/task-id "3"}` on hash("r1"), then `(|hash "3")` → add "r1" to `$$pending-by-executor["3"]`. One run row, one set entry, one inbox key; later removal targets "the **run row's recorded** assignment, not the claim's claimed value" (l.170) — the entry can never be stranded in a different inbox than the one consulted.
- **Fault-tolerance**: stream would risk row-committed/inbox-lost on replay (the plan traces this exact hazard and rejects stream, l.161); microbatch commits the pair atomically. Restart after commit: both durable.
- **Race**: duplicate submit with a *different* `:executor-task-id` hint → `$$decisions` present → total no-op → no second inbox entry; "the same run must never appear in two inboxes" holds because only the single accept event ever adds.
- **Flaws found**: none found, with reasoning: atomicity + single-accept + recorded-assignment removal close all paths to 0 or 2 entries.
- **Verdict**: PASS

### O1.c — Rejected request never spawnable
- **Source**: "A rejected request must never become spawnable: no pending-inbox entry, no run that an executor could claim." Matrix: "read-run: no spawnable run...; must never be :pending"; "read-pending: must NOT contain run-id".
- **Trace**: submit `{:run/id "r2" :argv [] :target :workspace}` → empty argv → reject: "decision only; reason recorded" (l.168). No `$$runs` row, no inbox write. Same for the new over-cap rejection (`:request/spec-too-large`): a 2,000-element argv produces a decision row and nothing else — the amendment's reject path rides the existing rejected-equals-absence design. Later claim for "r2" → "run row absent → no-op (claims never create state)" (l.170). `read-view "r2"` → no row → absent ("never looks like a pending run", l.189).
- **Fault-tolerance**: retry → exactly-once decision write; single-partition write, nothing to partially apply.
- **Race**: claim racing the rejection → row absent → no-op. No path creates run state from a rejection.
- **Flaws found**: none found, with reasoning: rejection is represented by *absence* of run state — there is structurally nothing to claim, for both validation-reject and cap-reject arms.
- **Verdict**: PASS

### O1.d — Target-kind validation
- **Source**: "Request validation must include target-kind checking (workspace targets are valid; the compute track doc requires 'checks target world refs')."
- **Trace**: submit `:target :nonsense` → "`:target` kind ∈ `#{:workspace}`" (l.168) fails → `:rejected` with reason. `:target :workspace` → passes.
- **Fault-tolerance / Race**: pure validation inside the guarded accept path; same properties as O1.a.
- **Flaws found**: none found, with reasoning: the check exists at the only admission point.
- **Verdict**: PASS

### O1.e — Decision latency independent of command duration
- **Source**: "Request acceptance must not wait for, or depend on, process execution ('the topology never waits for the command to finish')."
- **Trace**: submit `["sleep" "3600"]` → decision + `:pending` row committed next cycle (~hundreds of ms) regardless; the process is spawned later by the TaskGlobal executor on "executor-owned threads ... never on task threads" (l.210). Topology code never touches a Process.
- **Fault-tolerance**: a hung process cannot stall the topology (different threads); restart orphans the process without blocking processing.
- **Race**: 10 concurrent long-running runs → new submits still commit at normal cycle latency (per-event observation cost is O(1) and bounded).
- **Flaws found**: none found, with reasoning: physical execution is structurally out-of-band (TaskGlobal threads), not merely deferred.
- **Verdict**: PASS

### O1.f — Duplicate submit / run-id single-use / no lifecycle regression
- **Source**: "A duplicate submit of an existing `:run/id` must not create a second run, must not duplicate the inbox entry, and must never regress an in-flight or terminal run back to `:pending` (that would re-arm the spawn path → double spawn)." Also: "reuse of a `:run/id` from a finished run (must not reopen it)."
- **Trace**: "r1" is `:running` (pid 42, next-seq 3). Redelivered submit of "r1" → `$$decisions["r1"]` present → "total no-op: no decision flip, no run-row touch (no `:pending` regression — the double-spawn re-arm path is closed), no inbox re-add" (l.168). Run stays `:running`, inbox stays empty. Same trace at `:succeeded`: terminal record untouched; the dedup anchor never expires (no deletion anywhere) so run-ids are never reusable.
- **Fault-tolerance**: anchor durable across restarts; retry exactly-once.
- **Race**: duplicate in the same microbatch as the original → same task, sequential → second sees the just-written decision → no-op.
- **Flaws found**: none found, with reasoning: the no-op is *total* — it touches none of the three PStates — so no field can regress.
- **Verdict**: PASS

### O1.g — Submit edge cases: empty argv, blank run-id, nonexistent binary, dangling hint, over-cap spec
- **Source**: "empty argv; argv referencing a nonexistent binary (acceptance is still valid — failure surfaces later as a failed run...); missing/blank `:run/id` (must be rejected or refused...); ... executor-assignment hint pointing at a nonexistent inbox (run would sit pending forever — acceptable for the manual/test path ... but the normal path must always assign a live executor inbox)."
- **Trace**: (a) `:argv []` → rejected ("structurally unrunnable", l.168). (b) `["no-such-bin"]` → accepted; spawn later fails → executor sends `:failed {:reason :spawn-failed}` → terminal `:failed` (O8.e). (c) `:run/id ""` → "refused client-side by the submit helper and dropped without state by the topology" — the spec allows "rejected **or refused**"; a blank id cannot key a decision row, so refusal is the only coherent arm, and the topology drop creates no state. (d) hint `"test-executor-zzz"` → run `:pending` in that inbox forever — spec-sanctioned; normal path assigns `(mod |hash(run-id)| task-count)` as a stable string targeting per-task automatic executors whose "inbox key = stable task-id string" (l.169, l.209) — every normal-path key is a live executor's key by construction. (e) over-cap spec (amendment): 2,000-element argv or 200-entry env → `:rejected :request/spec-too-large` — a new, spec-compatible rejection arm at the same admission point; under-cap real commands (303-arg link line) accepted.
- **Fault-tolerance**: (d) survives restart — assignment durable in the run row, inbox durable, the task's fresh executor instance owns the same stable key.
- **Race**: all single-event guards; none specific.
- **Flaws found**: none found, with reasoning: each edge maps to a quoted plan mechanism; the F1 caps add a rejection arm without touching any accept-path invariant.
- **Verdict**: PASS

### O2.a — Single-grant invariant
- **Source**: "**Single-grant invariant (the core safety property):** for any run, at most one claim is ever granted. The first claim processed against a `:pending` run wins: the run moves `:pending → :launching` and durably records the winner's identity (`:claimed-by`) and `claim-token`, and the run is removed from the pending inbox."
- **Trace**: executors A (id "3+n1", token tok-a) and B (id "b+n2", token tok-b) both claim "r1". Both records hash to "r1"'s task, processed sequentially. First (A): status `:pending` → write `:launching`, `:claimed-by "3+n1"`, `:claim-token tok-a`, `:claimed-at-ms` (write-once), hop `(|hash "3")` (run row's recorded assignment) → `disj` "r1" (l.170). Second (B): status `:launching` → "Any other status → strict no-op (winner's fields never overwritten)". One grant ever — matches `double-claim-and-wrong-token-observation-test` ("second claim leaves `claimed-by`/`claim-token` at the first claimant's values").
- **Fault-tolerance**: grant+removal are one atomic microbatch pair — no committed-grant/lost-removal window (the plan's stated reason for rejecting stream, l.162). Retry → exactly-once. Restart after grant → fields durable, write-once.
- **Race**: both claims in one microbatch → "serialized on the run's task — first wins deterministically" (l.170). The inbox `disj` follows the CAS in the same atomic batch.
- **Flaws found**: none found, with reasoning: single-threaded task + status CAS + write-once fields + batch atomicity jointly exclude a second grant in every interleaving.
- **Verdict**: PASS

### O2.b — Claim vs non-`:pending` run is a strict no-op
- **Source**: "A claim against a run in any state other than `:pending` is a strict no-op — the recorded winner's identity and token are never overwritten." Matrix rows: `:launching × claim`, `:running × claim`, `:succeeded/:failed × claim` all no-op.
- **Trace**: claim for "r1" at `:running` → status ∉ {absent-handled-separately, `:pending`} → no write to any PState. Claimant's R5 read: `claimed-by ≠ me` → conflict-or-past → walks away.
- **Fault-tolerance**: no-op is trivially retry-safe.
- **Race**: late claim concurrent with `:exit` observation → both serialized on the run's task; either order leaves grant fields and terminal result untouched.
- **Flaws found**: none found, with reasoning: the no-op covers all four non-pending states uniformly.
- **Verdict**: PASS

### O2.c — Claim for unknown run-id creates no state
- **Source**: "A claim against an unknown `:run/id` must not create run state." Matrix: "read-run: still absent — claims must not create run state; claimant classifies 'not-yet-processed' and retries."
- **Trace**: claim `{:run/id "ghost" :claim-token t}` → "run row absent → no-op (claims never create state)" (l.170). `read-run "ghost"` → nil → 3-state check → not-yet-processed; kernel does not error (guard, not exception). Can the automatic path hit claim-before-row? No: discovery reads the *committed* inbox; the batch that wrote inbox+row atomically commits before the claim (appended after discovery) can be in any batch — so the claim's batch always sees the row. The no-op fires only for genuinely unknown/rejected ids, where retry-forever is correct.
- **Fault-tolerance / Race**: no state → nothing to corrupt or duplicate.
- **Flaws found**: none found, with reasoning: microbatch commit ordering closes the early-claim window for the kernel-owned path; the guard covers the rest.
- **Verdict**: PASS

### O2.d — Claim retry-safety (redelivery)
- **Source**: "Claim processing must be retry-safe: redelivery of the winning claim must not change the outcome; redelivery of a losing claim must remain a no-op."
- **Trace**: winning claim (tok-a) redelivered after grant → status `:launching` → strict no-op → outcome identical ("redelivered winning claim is a no-op because status is already `:launching`", l.170). Losing claim redelivered → same no-op. Topology-side retry excluded separately by microbatch exactly-once.
- **Fault-tolerance**: all redelivery × restart interleavings reduce to "status ≠ :pending → no-op."
- **Race**: redelivered winner racing first observation → serialized on the run task; no-op either side.
- **Flaws found**: none found, with reasoning: idempotence comes from the status CAS, not from comparing claim contents — even a redelivered winner is inert.
- **Verdict**: PASS

### O2.e — Concurrent claims: deterministic winner, loser can detect loss
- **Source**: "Outcome must be deterministic: exactly one winner, chosen by processing order, and the loser must be able to *detect* it lost by reading the run record (sees another's identity/token → interprets conflict)."
- **Trace**: as O2.a. Loser B reads R5: `{:status :launching :claimed-by "3+n1" :claim-token tok-a}` → `claimed-by ≠ "b+n2"` → conflict-or-past → manual path returns nil (test: "executor-b gets `nil` after executor-a's claim won").
- **Fault-tolerance**: grant fields durable; classification survives restart.
- **Race**: covered by task serialization.
- **Flaws found**: none found, with reasoning: R5 exposes exactly the fields the 3-state classification needs (and, post-amendment, nothing more).
- **Verdict**: PASS

### O2.f — Grant check is identity AND token
- **Source**: "claim with the same token as the winner but different executor id (must not be treated as granted — grant check is identity AND token)."
- **Trace**: winner ("3+n1", tok-a). Executor C holds tok-a but id "c+n3". 3-state check: "granted-to-us (`:launching` ∧ claimed-by is me ∧ token is mine)" → `claimed-by ≠ "c+n3"` → conflict-or-past. Plan states it verbatim: "The check requires identity AND token — same-token-different-id classifies as conflict" (l.201).
- **Fault-tolerance / Race**: pure read of durable write-once fields.
- **Flaws found**: none found, with reasoning: the conjunction is explicit in the plan's R5 classification.
- **Verdict**: PASS

### O3.a — Attribution/authorization + auditable errors
- **Source**: "an observation is applied only if its `claim-token` matches the token durably granted for that run. Unauthorized observations (wrong token, or any token while no grant exists) must NOT mutate run output/status, and MUST be recorded as observation errors with a reason (test evidence: reason `:observation/not-authorized`), visible in both the run truth record and the UI view — unauthorized attempts are auditable, not silently dropped."
- **Trace**: "r1" `:launching`, tok-a granted. Observation `{:claim-token "wrong" :type :stdout :sequence 0 :line "evil"}` → guard 2 (l.173): token ≠ tok-a → append `ObservationError{:reason :observation/not-authorized :sequence 0 :obs-type :stdout}` + `inc :observation-error-count`; "status/output/terminal fields untouched" → `:stdout-tail` stays empty (exact test scenario). While `:pending` (no grant): "Fail (including any token while status is `:pending`)" → same error path; run stays `:pending` and stays in the inbox (matrix row matched). R2 includes `:observation-errors`; R3 includes `:observation-errors` + count → auditable on both surfaces. The error record "never contains the offending token (it would leak through the view)" (l.173) and carries no line payload (`ObservationError [reason sequence obs-type at-ms]`, l.82) — so the audit trail cannot itself become a byte-unbounded or secret-leaking surface.
- **Fault-tolerance**: error append is inside the exactly-once batch → no double-append on retry.
- **Race**: unauthorized obs concurrent with the authorized stream → serialized on the run's task; authorized application unaffected.
- **Flaws found**: none found, with reasoning: the auth check precedes terminal/sequence handling, so every unauthorized attempt in every state is recorded, and only recorded.
- **Verdict**: PASS

### O3.b — Sequence ordering: buffered, never dropped, exit drains in order
- **Source**: "observations are applied in per-run `:sequence` order regardless of arrival order. Out-of-order observations are buffered, **never dropped**, and applied when the gap fills. `:exit` closes the run only after draining buffered observations in sequence order."
- **Trace**: next-seq=0. Arrivals: seq 2 (`:exit 0`), seq 1 (`:stdout "hello"`), seq 0 (`:started` pid 42). seq 2 > 0 → `:buffered[2]=ExitObs` (store-if-absent). seq 1 > 0 → `:buffered[1]=OutputObs`. seq 0 = next-seq → apply `:started` (→ `:running`, pid 42), next-seq=1, drain: `[1]` present → apply stdout (idx 0), next-seq=2, `[2]` present → apply exit → `:exit-code 0`, `:status :succeeded`; drained entries deleted. Output landed before terminal status — exactly the matrix row. "Exit closes only after drain" is structural: a buffered exit *cannot* apply until the watermark reaches it. Drain uses "`loop<-` with `yield-if-overtime`" (l.175) per the skill's cooperative-multitasking rule; interleaved events during a yield are safe because every guard is a pure function of the current committed-on-task watermark/buffer state.
- **Fault-tolerance**: buffer lives in durable `$$runs :buffered` → survives restart mid-gap; exactly-once excludes double-application on retry.
- **Race**: stdout/stderr pump threads share the per-run monotonic sequence; arrival inversions are exactly the buffered case; per-run order comes from the seq number, as the spec requires. (Stronger: the acked-cursor send discipline — "an observation is 'sent' only once durably appended", l.37 — means the A.0 writer cannot even open a gap, so the buffer is defense in depth.)
- **Flaws found**: none found, with reasoning: strict watermark + durable buffer implements "applied in sequence order regardless of arrival order" by construction. (Buffer cap vs "never dropped": the spec itself also demands "the out-of-order buffer ... must not grow without bound" — at the adversarial limit the two conflict; the plan keeps never-dropped for all A.0-reachable behavior — acked-cursor sends mean observations are redelivered-not-lost, and a sequential writer cannot open a >1024 gap — and makes the unreachable valve auditable (`:observation/buffer-overflow`). This resolves a spec-internal tension rather than violating a satisfiable constraint.)
- **Verdict**: PASS

### O3.c — Lifecycle effects per observation type
- **Source**: "authorized `:started` → status `:running` + pid recorded; `:stdout`/`:stderr` → folded into bounded recent-output tails in sequence order; `:heartbeat` → updates a liveness timestamp, no status change; `:exit` → terminal status derived from exit code (0 → `:succeeded`, non-zero → `:failed`) with the exit code recorded."
- **Trace**: guard step 5 (l.176) maps one-to-one: `:started` → `:status :running`, `:pid`; `:stdout`/`:stderr` → "write line at `idx = count`, `inc` count, delete `idx - cap` when `idx ≥ cap`" (application order = sequence order; ascending dense index = chronological tail); `:heartbeat` → "`:last-heartbeat-ms` only"; `:exit` → "`:exit-code` recorded, `:status :succeeded` if 0 else `:failed`". The `false` run: `{:exit-code 1}` → `:failed`, `:exit-code 1` — while `read-decision` still shows `:accepted` (observations never touch `$$decisions`). Unknown types from an authorized token "fold to an observation error, not a crash" (l.144) — a safety valve; the A.0 writers emit only the five known types, and adding a deferred type later is a new guard-5 branch + defrecord (see Deferred-ops block).
- **Fault-tolerance**: all type-applications inside the exactly-once batch.
- **Race**: heartbeat interleaved with stdout → both seq-ordered on one task.
- **Flaws found**: none found, with reasoning: each spec'd effect has a literal counterpart in guard step 5.
- **Verdict**: PASS

### O3.d — Terminal immutability
- **Source**: "once a run is `:succeeded`/`:failed`, late or redelivered observations must not reopen the run or mutate its terminal result." Matrix flag: "(FLAGGED: ignore vs record-as-error is unspecified; either is acceptable, mutation is not)."
- **Trace**: "r1" `:succeeded`, exit 0, tail ["hello"]. Redelivered authorized `:stdout` seq 1 → guard 3 (l.174): "authorized observations ignored silently"; tail/exit/status untouched. Unauthorized obs at terminal → guard 2 fires *before* guard 3 → error recorded, terminal result untouched — both spec-permitted arms, chosen per-case, both non-mutating. A second `:exit` with a different code → terminal → ignored → `:exit-code` unchanged.
- **Fault-tolerance**: ignore is retry-trivial.
- **Race**: late observation racing a read → reader sees committed terminal state either way.
- **Flaws found**: none found, with reasoning: the terminal check precedes all mutating steps (4-5) in the guard chain.
- **Verdict**: PASS

### O3.e — Retry safety: duplicate sequence numbers (applied path)
- **Source**: "redelivery of an already-applied sequence number must not double-apply (e.g. the same stdout line must not appear twice in the tail)."
- **Trace**: seq 1 (`:stdout "hello"`) applied, next-seq=2, `:stdout {0 "hello"}`, count=1. Redelivery of seq 1 → guard 4: "`seq < next-seq` → duplicate, ignore" → tail still exactly `{0 "hello"}`. Topology-side retry separately excluded by exactly-once.
- **Fault-tolerance**: watermark durable; restart between original and redelivery still ignores.
- **Race**: duplicate concurrent with new seqs → task-serialized; watermark decides.
- **Flaws found**: none found, with reasoning: dual protection (client-level watermark + topology-level exactly-once) covers both duplicate sources the plan enumerates.
- **Verdict**: PASS

### O3.f — Bounded readable surfaces + constant per-observation cost
- **Source**: "Per-run output is unbounded in principle; the readable run record and view keep bounded tails (`:stdout-tail`, `:stderr-tail`) plus the exit code. The out-of-order buffer must drain on gap fill / exit and must not grow without bound. The observation-errors list is attacker/bug-influenced ... and must be bounded." And: "Per-observation processing cost must be small and constant; readable surfaces must stay bounded no matter how much a process prints."
- **Trace**: run prints 100,000 lines. Each line: 1 entry write at `idx = count`, 1 counter inc, from idx ≥ 200 one entry delete at `idx-200` (dense index ⇒ no read to find the victim) → O(1) per line, no large-value rewrite (Option A rejected for exactly this scenario's "gigabytes of serialization churn", l.65). Final `:stdout`: entries 99,800-99,999 → 200 entries, each ≤4 KiB (F3 cap) → tail bounded in entries AND bytes (≤200 × 4 KiB ≈ 800 KiB absolute ceiling, typical ~20 KB). Tail read: 1 seek + 200 sequential iterations ≈ 1.5 ms. Buffer drains on gap fill (entries deleted as applied), capped 1024 with auditable overflow; entries line-capped → ≤ ~4 MiB worst. Error ring capped 100 + total counter (resolves spec ambiguity 7). Per-event fields read targets a row rewritten every event → memtable/cache hit (l.212); the row value includes the admission-capped `:spec`, so the per-observation cost ceiling is set by the caps (≤ ~164 KiB deserialization in the adversarial max-spec corner, typically ~1 KiB) — constant, independent of output volume, with the plan naming per-run pre-aggregation as the escape hatch if multi-run scale pressures it (l.212).
- **Fault-tolerance**: caps are enforced inside the same exactly-once events that grow the collections — no retry path can overshoot a cap.
- **Race**: per-run only; different runs land on different tasks via hash-by run-id — "must not contend" holds.
- **Flaws found**: none found, with reasoning: every named surface now has a cap on both the entry axis and the byte axis, each with a named write-path enforcement mechanism (dense-index trim, store-if-absent + overflow valve, errors ring, F1 admission caps, F3 line cap).
- **Verdict**: PASS

### O3.g — Edge: duplicate sequence number with different payload (buffered path) — **F2 amendment validation**
- **Source**: "duplicate sequence numbers with different payloads (must keep the first, never corrupt)"
- **Trace**: next-seq=0. Obs seq 2 (`:stdout "AAA"`) arrives → `:buffered[2]=OutputObs("AAA")`. Redelivered/corrupted obs seq 2 (`:stdout "BBB"`) arrives while next-seq is still 0 → guard 4 (l.175): "store in `:buffered` **only if `:buffered[seq]` is absent** (navigate `(keypath seq)`, write only when nil — first payload kept; a redelivered buffered sequence with a different payload is a no-op and can never overwrite the first, mirroring the applied-path first-wins rule)" → key 2 present → no-op → `"AAA"` kept; drain later applies `"AAA"`. Round 1's failing scenario now traces to the correct outcome. Window sweep: duplicate of an applied seq → watermark ignore (first kept); duplicate of a buffered seq → store-if-absent (first kept); duplicate of a drained-then-redelivered seq → by then seq < next-seq → watermark ignore. First-wins holds across the entire sequence window with no remaining unprotected interval. The check-then-write is atomic because the event executes single-threaded on the run's task.
- **Fault-tolerance**: store-if-absent is idempotent under topology retry (exactly-once anyway) and under arbitrary client redelivery — the class this guard exists for.
- **Race**: two same-seq records in one microbatch → sequential on one task → first stores, second sees the key present → no-op.
- **Flaws found**: none found, with reasoning: the buffered window was the only first-wins hole; the amendment closes it with a mechanism cited at the exact guard step, at the cost of one keypath read on the rare buffered path.
- **Verdict**: PASS

### O3.h — Edge: huge single line — **F3 amendment validation**
- **Source**: "huge single line (must not break the bounded tail)" and (O6) "Bounded size regardless of process output volume."
- **Trace**: process runs `head -c 1G /dev/urandom | base64 | tr -d '\n'` → pump thread reads one ~1.3 GB line → truncates to ≤4 KiB + explicit marker (e.g. `…[truncated N bytes]`) *before building the `OutputObs`* (l.210) → the W3 depot record is ≤ ~4 KiB (l.39: "a single pathological line can never produce a giant depot record") → tail entry ≤4 KiB → R2/R3 reads bounded. Second layer: kernel-side ingest truncation "before buffering or applying" (l.171) — if a non-conforming *authorized* writer ships an over-cap line, the stored tail entry and any `:buffered` entry are still ≤4 KiB, so `$$runs` and both view surfaces stay byte-bounded regardless of writer behavior. In A.0 the only token holders are the automatic executor and the manual path, and both truncate at capture (l.210, l.211), so the depot record bound holds for every sanctioned writer too. Faithfulness tension: truncation is explicit (marker), not silent loss, and the spec's own bounded-tail edge demands exactly this; the faithful-capture test cases (`echo hello`, `false`) are orders of magnitude below the cap and unaffected.
- **Fault-tolerance**: truncation is deterministic per line — a redelivered truncated observation is byte-identical, deduped by watermark/store-if-absent.
- **Race**: n/a — single-writer transformation at capture time.
- **Flaws found**: none found, with reasoning: the byte axis round 1 found unbounded is now capped at both the primary (capture) and defensive (ingest) sites, bounding depot record, buffer entry, tail entry, and both read surfaces.
- **Verdict**: PASS

### O3.i — Remaining O3 edges: gap-never-fills at exit; unknown run-id; exit-only run; unauthorized while pending
- **Source**: "sequence gap that never fills before `:exit` (flagged ...)"; "observation for an unknown `:run/id` (must not create run state)"; "`:exit` as the only observation (process died before `:started` was reported — run must still close terminally)"; "unauthorized observation while run is `:pending` (no token granted yet → not-authorized error, status unchanged — exact test scenario)."
- **Trace**: (a) Gap at exit: spec ambiguity 3 explicitly leaves this open; the plan resolves it as "strict in-order apply, permanent gap blocks closure" and shows why it is unreachable in A.0 (acked-cursor sends → redelivered-not-lost; a sequential writer cannot skip a seq), with A2 named as the owner per the spec's own deferral ("A2 retry/stall work will force this decision"); "Truth never lies — the run simply never claims success" (l.177). A sanctioned ambiguity resolution within its stated hard requirement. (b) Unknown run-id: guard 1 → "drop, no state created" (l.172); spec ambiguity 8 says auditability "is unspecified" → the plan documents the A.0 drop and names the A2 audit-counter candidate, deliberately not funneling attacker-controlled volume to a global task — a permitted arm of an explicitly unspecified behavior. (c) Exit-only: spawn succeeded, process died instantly, executor sent only `:exit` seq 0 → seq 0 = next-seq → apply; guard 5: exit "legal from `:running` **or** `:launching` — covers 'process died before `:started` was reported'" → closes terminally. (d) Unauthorized while `:pending`: traced in O3.a — error recorded, status `:pending` unchanged, still in inbox.
- **Fault-tolerance / Race**: (a),(b) are no-ops/drops — trivially safe; (c),(d) inside exactly-once events.
- **Flaws found**: none found, with reasoning: each edge either lands on a cited mechanism or is a spec-flagged ambiguity resolved within its stated hard requirement.
- **Verdict**: PASS

### O4 — Read decision
- **Source**: "Readable forever once recorded; never flips after being recorded; reflects the *request* outcome, not the process outcome (the `false` run's decision is `:accepted` even though the run ends `:failed`)." Edges: "unknown run-id → absent/nil; read before the submit is processed → absent, then appears (poll contract)."
- **Trace**: R1 `foreign-select-one [(keypath "r1")]` on `$$decisions` → after accept: `{:decision/status :accepted :request/type :compute/run-command ...}`. After the `false` run fails: unchanged — observations only write `$$runs`. Never flips: the only writer is the submit guard, which writes solely on the absent branch. Forever: no deletion anywhere (l.216). Unknown id → nil. Before processing → nil, appears after batch commit — poll contract.
- **Fault-tolerance**: durable PState; exactly-once write.
- **Race**: concurrent readers see committed state only.
- **Flaws found**: none found, with reasoning: single-writer-single-branch makes flips structurally impossible.
- **Verdict**: PASS

### O5 — Read run record + 3-state classification + token surface
- **Source**: "This is the *only* surface that exposes `claim-token` — it is what the grant check reads. Must always reflect every applied write... Given a run record and a claim, the claimant must be able to classify deterministically: **granted-to-us** (`:launching` ∧ claimed-by is me ∧ token is mine), **not-yet-processed** (record absent ∨ `:pending`), **conflict-or-past** (anything else)." Edge: "unknown run-id → absent (claimant must treat as not-yet-processed, not error); read in every lifecycle state must be well-formed."
- **Trace**: R2 returns full truth assembled directly from `$$runs` — reflects every applied write because it reads the truth PState with no derived copy. The grant check reads R5: submap `:status :claimed-by :claim-token :executor/task-id` (l.16) — the classification text matches the spec verbatim (l.201). Post-amendment R5 carries no `:spec`; the executor still gets argv/cwd/env via R5b (l.17) exactly once, *after* classifying granted-to-us — the spawn input is read strictly after the durable grant, so the amendment does not weaken spawn-after-grant (R5b reads a field written atomically with run-row creation, committed long before any grant exists). Token exposure: R2 and R5 are both narrowed access methods to the same O5 truth surface (`$$runs`); the spec's "only surface" contrasts truth (O5) with the view (O6), and the view never includes the token. States: absent → nil (not-yet-processed); `:pending` → no claim fields, well-formed; later states add their fields; R2's 1-vs-3 branching returns well-formed results in all six states.
- **Fault-tolerance**: reads of durable committed state; restart-safe. Restart between grant and R5b: the fresh instance classifies the old grant conflict-or-past and never fetches spec — the sanctioned stall case, no half-spawn.
- **Race**: claimant reading mid-grant sees pre-batch (`:pending` → keep waiting) or post-batch (`:launching` + all grant fields) — never a partial grant (single-event write on one task).
- **Flaws found**: none found, with reasoning: classification fields are written atomically with the status CAS; the R5/R5b split changes read granularity, not the classification or its inputs.
- **Verdict**: PASS

### O6.a — Token secrecy
- **Source**: "**Token secrecy:** the view must NEVER contain `:claim-token` (asserted in every test that reads a view: `(not (contains? view :claim-token))`). The secret that authorizes physical work must not leak to the UI-readable surface."
- **Trace**: `run-view` assembles `{:status :pid :exit-code :failure-reason :stdout-tail :stderr-tail :observation-errors :observation-error-count}` (l.189) — "Assembly **never touches** `:claim-token`/`:next-seq`/`:buffered`": the key is absent because it is never read into the result, not filtered post-hoc. Secondary channels: `ObservationError` "never stores the offending token" (l.82, l.173); `:buffered` omitted; no other view surface exists (no materialized view PState, l.132-138). `(contains? view :claim-token)` → false in every state.
- **Fault-tolerance / Race**: structural property of the query topology; independent of timing.
- **Flaws found**: none found, with reasoning: omission-by-construction at the single sanctioned view surface plus the token-free error records closes direct and indirect channels.
- **Verdict**: PASS

### O6.b — View: bounded, errors visible, terminal forever, rejected ≠ pending, well-formed everywhere
- **Source**: "Bounded size regardless of process output volume. Must show unauthorized-observation errors (auditability reaches the UI). Terminal views (`:succeeded`/`:failed`) remain readable indefinitely." Edges: "unknown run-id → absent; view of a `:pending`/`:launching` run has no output yet but is well-formed; view after rejected request must never look like a pending run."
- **Trace**: tails ≤200 entries × ≤4 KiB/line (F3), errors ≤100 small records, fixed scalars → bounded in entries AND bytes (round 1's byte-axis hole is closed; the conditional PASS is now unconditional). Errors + total count in the view → auditability reaches the UI. Terminal: `$$runs` rows never deleted → readable indefinitely. Unknown → nil. `:pending`/`:launching` → zero counts → tail reads skipped → well-formed empty tails. Rejected → no `$$runs` row → view **absent** — structurally cannot resemble `:pending`.
- **Fault-tolerance / Race**: pure reads of committed durable state.
- **Flaws found**: none found, with reasoning: every view field is either a capped collection or a scalar, and rejection-as-absence makes the rejected/pending confusion impossible.
- **Verdict**: PASS

### O7 — Pending inbox iff-invariant
- **Source**: "A run-id appears here iff the run is `:pending` and assigned to this inbox; it is added exactly once on acceptance and removed when a claim is granted. Nothing else removes it (a pending run with no live executor waits forever in A.0...)." Edges: "empty inbox → empty collection (not an error); inbox key that has never existed → empty; the same run must never appear in two inboxes."
- **Trace**: add happens only on the accept branch (atomic with the `:pending` row); remove happens only on the grant branch (atomic with the `:launching` CAS) ⇒ entry present ⟺ accepted ∧ not yet granted ⟺ `:pending`. No other code path touches the inbox (no reaper/timeout in A.0, matching the deferral). Both iff directions hold because each transition is a single atomic batch — no observable moment where row says `:pending` but inbox is empty, or `:launching` with the entry remaining. Losing claim: exits at the status guard, never reaches the removal hop — "a losing claim never removes someone else's pending entry". Empty/never-existing key → keypath → nil → empty collection. Two inboxes: single add ever (O1.b) + removal targets the run row's recorded assignment. (Colocation note: the inbox row for key "3" lives on hash("3")'s task, not necessarily task 3 — correct and harmless, since R4 is a foreign read routed by key and the add/remove hop is an explicit `(|hash inbox-key)` partitioner.)
- **Fault-tolerance**: durable PState, named "the restart-reconciliation anchor" (l.130); exactly-once add/remove.
- **Race**: duplicate submit → no re-add (total no-op); grant racing discovery → executor may claim a just-granted run → status no-op, loser walks away.
- **Flaws found**: none found, with reasoning: both inbox mutations are fused atomically to the run-status transitions they must mirror.
- **Verdict**: PASS

### O8.a — Automatic execution end-to-end
- **Source**: "an accepted `:compute/run-command` must reach a terminal state with NO further actor involvement (test evidence: `task-global-executor-runs-command-automatically-test` submits a request and only waits)." Protocol steps 1-5 (discover → claim → 3-state await → spawn-after-grant → stream).
- **Trace**: submit `["echo" "hello"]`, no hint → assigned inbox "3" via `(mod |hash(run-id)| task-count)`. Task 3's TaskGlobal executor (reconcile scheduler started in `prepareForTask`): tick reads inbox "3" (R4) → "r1" not in registry → registry entry + fresh tok-a → append claim (W2) → poll R5 → `:launching` ∧ claimed-by me ∧ token mine → R5b fetches `:spec` (≤ ~164 KiB bounded, typically <1 KiB) → spawn `echo hello` → pump threads stream `:started`(0), `:stdout "hello"`(1), `:exit 0`(2) with monotonic seqs, each sent only once durably appended (acked cursor) → kernel folds → `:succeeded`, tail ["hello"]. No actor involvement after submit. Latency traced under Visibility (≈2 s < 5 s).
- **Fault-tolerance**: see O8.b (spawn dedup) and Production readiness (restart); the R5b step adds no new failure class — failing the fetch just delays the spawn to the next reconcile attempt, before any physical work has begun.
- **Race**: multiple in-flight runs → independent registry entries, processes, pump threads; per-run independence via hash-by run-id.
- **Flaws found**: none found, with reasoning: each protocol step maps to a named plan mechanism (R4 / registry / W2 / R5 / R5b / spawn gate / W3).
- **Verdict**: PASS

### O8.b — At most one spawn per run, ever
- **Source**: "At most one spawn per run, ever (consequence of single-grant + spawn-after-grant + the executor's local registry deduplicating discovery)." And step 4: "**Spawn the OS process only after observing the durable grant** ... Never spawn on hope."
- **Trace** (attacking each layer, l.199-203): (1) Two claims → one grant (O2.a). (2) Spawn requires R5 to show *committed* `claimed-by = me ∧ claim-token = mine` — write-once fields ⇒ at most one (identity, token) pair in history ever passes ⇒ at most one holder may spawn. (3) Holder spawning twice? Registry entry from the first tick is held until terminal/conflict, and a terminal run is no longer in the inbox ⇒ no second claim or spawn attempt by the same instance. (4) Restart attack: instance dies after claim, before spawn → new instance, fresh nonce; if the old claim won, R5 shows `claimed-by = "3+old-nonce" ≠ "3+new-nonce"` → conflict-or-past → drop → run stalls `:launching` — "stalled-but-safe, never double-spawned" (l.205); if the old claim had not been processed, the run is still in the inbox → new instance claims fresh → exactly one of the two claims wins → one spawn or zero. (5) Restart after spawn: orphan process may live, but the new instance classifies conflict-or-past and never re-spawns; the orphan's pump threads died with the worker, so nothing holding the token can still report → no false success (l.207). (6) Amendment interaction: R5b sits between grant classification and spawn — a crash there is case (4)/(5) territory with zero processes spawned; no new double-spawn window opens because the registry entry and grant classification still gate the retry.
- **Fault-tolerance**: restart at every protocol step (before claim, after claim, after grant, mid-R5b, after spawn) yields one-or-zero spawns, never two.
- **Race**: instance racing itself → registry pins the in-flight entry; two instances → grant CAS decides.
- **Flaws found**: none found, with reasoning: the four layers were attacked at each restart/race point, including the new R5b step, and no double-spawn interleaving exists.
- **Verdict**: PASS

### O8.c — Out-of-band execution + back-arrow rule
- **Source**: "Physical work happens out-of-band from request/claim/observation processing. The executor never writes truth directly — its only write channels are claim and observation submission (back-arrow rule)." Spec summary: "actors (UI, executor) only *request* and *report*; only the kernel writes truth; all actors read truth back from kernel state."
- **Trace**: "reconcile scheduler started in `prepareForTask`; per-process stdout/stderr pump threads ... never on task threads" (l.210). Write channels: "appends to the claim/observation depots, foreign reads of inbox + grant fields" — "The executor touches the kernel **only** through the public foreign surface." It never holds a PState transform handle; truth is written exclusively by the `compute-kernel` topology folding depot records. Reads back: discovery via R4, grant via R5, spec via R5b — all kernel state, not local belief.
- **Fault-tolerance / Race**: structural property; the module-owned executor obeys the same contract as an external client, so no privileged path can bypass the guards.
- **Flaws found**: none found, with reasoning: the executor's entire kernel interface is the same foreign surface external actors use.
- **Verdict**: PASS

### O8.d — Faithful capture
- **Source**: "Process stdout/stderr/exit are captured and reported faithfully (test: `echo hello` → `\"hello\"` in stdout tail; `false` → positive exit code, `:failed`)."
- **Trace**: pump threads read child stdout/stderr line-by-line → `OutputObs` per line with stream tag (truncated only past 4 KiB, with an explicit marker — the test lines are bytes long, untouched); exit waiter → `ExitObs{:exit-code}`. `echo hello` → `:stdout {0 "hello"}` → view tail `["hello"]`. `false` → exit 1 → `:failed`, `:exit-code 1`, decision still `:accepted` (O4).
- **Fault-tolerance**: acked-cursor sending → a captured line is never silently lost (redelivered at worst, deduped by watermark/store-if-absent).
- **Race**: stdout/stderr threads share the per-run monotonic sequence; cross-stream interleavings land via seq order (buffer absorbs inversions, per O3.b).
- **Flaws found**: none found, with reasoning: capture→observation→fold is lossless-by-cursor and ordered-by-sequence; truncation applies only beyond the cap and is explicitly marked.
- **Verdict**: PASS

### O8.e — Spawn failure, manual timeout, kernel restart with live child
- **Source**: "spawn failure (binary missing, cwd invalid) — the run must still reach a terminal `:failed`-shaped outcome, not hang in `:launching`"; "process outliving any timeout (manual path takes `:timeout-ms`...)"; "kernel restart with a live child process (reconcile deferred to A2, but recorded truth must never claim success for work that didn't finish)."
- **Trace**: (a) spawn of `["no-such-bin"]` throws → executor sends `:failed` observation `{:reason :spawn-failed}` (seq 0) → guard 5: "`:failed` → after drain, `:status :failed` + `:failure-reason` ... no fabricated exit code" (l.176) → terminal, not hung (resolves spec ambiguity 5 with exactly the `:failed` observation type the track doc names). (b) manual `:timeout-ms` expiry → "kill forcibly + `:failed` observation `{:reason :timeout}`" (l.208) → terminal (resolves ambiguity 6; "must not be left in `:running` forever" holds for the manual path; automatic-path stall detection is the spec's explicit A2 deferral). (c) restart with live child: traced in O8.b(5) — child cannot report, run stays non-terminal, "Recorded truth never claims success for work that didn't finish"; `:last-heartbeat-ms` already recorded to arm A2.
- **Fault-tolerance**: the `:failed` observation rides the same exactly-once + watermark path as any observation.
- **Race**: timeout kill racing natural exit → both observations come from the same single writer with ordered seqs; whichever applies first wins, the other hits the terminal-ignore guard — either terminal outcome is truthful.
- **Flaws found**: none found, with reasoning: all three edges land on cited mechanisms; the only open behavior (automatic-path stall) is the spec's own A2 deferral.
- **Verdict**: PASS

### O8.f — Manual single-step path
- **Source**: "the contract also exposes a single-step executor (`run-one-pending-local!` with `{:executor-id, :timeout-ms}`) that performs exactly one discover→claim→await-grant→spawn→stream cycle against an explicit test inbox and returns `{:claim-state :granted-to-us, :spawned? true, :spawned-after-grant? true}` on success or `nil` when there is nothing it can run ... This path must obey the identical claim/grant/observation protocol as the automatic path — it is a protocol probe, not a bypass."
- **Trace**: plan l.211: "a pure foreign client performing exactly one discover→claim→await-grant→spawn→stream cycle against an explicit inbox key with `:timeout-ms` ... Identical guards as the automatic path (including the 4 KiB per-line truncation at capture) — a protocol probe, not a bypass. No module-side state." Returns the classification map on success, nil when nothing claimable or claim lost (executor-b test). Test inboxes use disjoint keys, so automatic executors can never steal manual-path runs; the dangling-hint wait-forever behavior is what makes the manual inbox stable for tests. The F3 amendment explicitly extends to this path, so the byte bound has no manual-path bypass.
- **Fault-tolerance / Race**: stateless per invocation; protocol safety (single grant, spawn-after-grant) comes from kernel guards, which it shares.
- **Flaws found**: none found, with reasoning: same depots, same reads, same 3-state check, same truncation — a bypass would require a write channel the plan does not give it.
- **Verdict**: PASS

### O9 — Runtime lifecycle
- **Source**: "Close must cleanly stop executor loops and worker processes so test runs don't leak threads/processes; repeated start/close cycles (one per test) must work ... in production worker restart must not lose accepted runs or recorded results."
- **Trace**: TaskGlobal `close()`: scheduler + pump threads stopped, children `destroyForcibly`'d (l.210) — no leaked threads/processes per cycle; all executor state is per-instance (registry, clients), nothing static to poison the next start. Production: `$$decisions`/`$$runs`/`$$pending-by-executor` durable; microbatch resumes from persisted offsets (skill: PStates are durable storage, restart does not replay depot history); accepted runs and recorded results survive.
- **Fault-tolerance**: that *is* this constraint; traced.
- **Race**: close during an in-flight run → process killed; run left non-terminal in durable state — truthful, consistent with A.0 stall semantics.
- **Flaws found**: none found, with reasoning: per-instance lifecycle hooks cover the test contract; durability covers the production half.
- **Verdict**: PASS

### Scale — unbounded growth over time
- **Source**: "Run/decision records accumulate forever (every command ever run). Access pattern is point lookup by `:run/id`; results must remain readable after completion indefinitely (no deletion implied anywhere in the contract)."
- **Trace**: 5M runs over years → 5M top-level keys in `$$decisions` + `$$runs`, spread by hash; each read stays a 1-seek point lookup; per-run on-disk size hard-capped post-amendment (tails 2×200×≤4 KiB, buffer ≤1024 line-capped entries, errors ≤100, spec ≤ ~164 KiB by admission caps — l.217). A pathological row can no longer be arbitrarily large: the over-cap submit is rejected before any `$$runs` write exists (round 1's residual F1 scenario re-traced: 10⁶-element argv → `:rejected :request/spec-too-large`, zero bytes in `$$runs`). No deletion anywhere (skill rule honored). Inbox sets subindexed against the deferred-stall accumulation case.
- **Fault-tolerance / Race**: n/a beyond prior blocks.
- **Flaws found**: none found, with reasoning: every per-run surface now has an enforced entry cap and byte cap; total growth is row-count growth, which point-lookup access handles by design.
- **Verdict**: PASS

### Deferred-ops non-preclusion (spec ambiguity 9)
- **Source**: "cancel (`:cancel-request`/`:cancelled`), restart-reconcile, stall/lost-run detection, heartbeat-based liveness policy, and the broader compute-track request types ... and richer views ... are named by the track doc but deferred; the design must not preclude them (e.g. terminal-status set must be extensible to `:cancelled`)."
- **Trace**: status is an open Keyword field ("set open — `:cancelled` reserved for A2", l.86); `IObservation` is an open interface (new variants = new defrecords); request depot dispatches on `:request/type` ("keeps the depot open to future compute request types", l.142); unknown observation types from an authorized token fold to an observation error, not a crash (l.144) — and when A2 adds a type, the addition is a guard-5 branch + defrecord, no schema migration; `:last-heartbeat-ms` is recorded now to arm stall detection; a reactive `$$run-views` PState "can be added later **additively** ... without breaking this design" (l.138); richer views are new PStates/topologies beside the existing ones. The F1 caps do not preclude future request types either — they are per-type validation rules inside the `:request/type` dispatch.
- **Fault-tolerance / Race**: n/a (extensibility property).
- **Flaws found**: none found, with reasoning: every deferred item has a named extension point and none requires migrating the A.0 schemas.
- **Verdict**: PASS

### Entity-state × write matrix — row-by-row citation sweep
Each matrix row mapped to the guard that decides it (rows restate invariants block-traced above):
- Decision: `absent × submit(valid)` → accept branch (O1.a/b) — PASS. `absent × submit(invalid)` → reject branch incl. the new cap-reject arm, no row/inbox (O1.c/d/g) — PASS. `accepted × duplicate submit` → presence no-op (O1.f) — PASS. `rejected × resubmit` → presence no-op; run-id single-use, "actor must mint a new run-id" = the plan's ambiguity-2 resolution — PASS.
- Run: `does-not-exist × submit` → accept branch — PASS. `× claim` → "row absent → no-op" (O2.c) — PASS. `× observation` → guard 1 drop (O3.i) — PASS. `:pending × duplicate submit` → total no-op (O1.f) — PASS. `:pending × claim` → grant CAS + atomic inbox removal (O2.a) — PASS. `:pending × observation` → guard 2 not-authorized error, stays pending+in-inbox (O3.a) — PASS. `:launching × claim` → status no-op (O2.b) — PASS. `:launching × duplicate submit` → total no-op, no `:pending` regression (O1.f) — PASS. `:launching × :started` → guard 5 → `:running`+pid (O3.c) — PASS. `:launching × out-of-seq output` → buffered store-if-absent, not in tails until in-order (O3.b/g) — PASS. `:launching × wrong token` → error, empty tail (O3.a) — PASS. `:launching × :exit w/ buffer` → drain-then-terminal (O3.b) — PASS. `:running × in-order output` → tail append exactly-once, line byte-capped (O3.c/e/h) — PASS. `:running × :heartbeat` → timestamp only (O3.c) — PASS. `:running × :exit` → terminal by code, decision untouched (O3.c, O4) — PASS. `:running × claim` → no-op (O2.b) — PASS. `:running × wrong token` → error only (O3.a) — PASS. `terminal × claim` → no-op (O2.b) — PASS. `terminal × observation` → authorized-ignore / unauthorized-error, immutable (O3.d) — PASS. `terminal × duplicate submit` → no-op, no re-add (O1.f) — PASS.
- Inbox: `empty × submit accepted` → atomic add (O1.b) — PASS. `contains × grant` → atomic remove (O7) — PASS. `contains × losing claim` → never removes others' entries (O7) — PASS. `contains × duplicate submit` → no re-add (O1.f) — PASS.

## Self-consistency check

Re-read of all entries above: no entry describes an unresolved gap, an unhandled edge, or a deferred-but-required behavior. The three round-1 FAILs (O3.g, O3.h, argv/env enforcement + scale) were re-traced against the amended mechanisms and each now lands on a cited, scenario-verified plan rule; their verdicts are PASS on the merits, not on the amendment note's say-so. The two places where the plan declines to act (unknown-run-id audit, gap-never-fills closure) are both arms of ambiguities the spec itself marks unspecified/deferred, each resolved within its stated hard requirement ("must not create run state"; "recorded truth must never claim success") — sanctioned resolutions, not gaps. O6.b, conditional in round 1, is unconditional now that the byte axis is capped. No pass-and-contradict entries remain.

## Failures (spec line ↔ plan line ↔ why)

None. The three round-1 failures are closed by the amendments, each verified by re-running the original failing scenario against the amended text:
1. **F1** — 10⁶-element argv / 150-var env: now rejected at admission (`:request/spec-too-large`, PLAN l.117/l.168) with the validation rule cited as the enforcement mechanism for both non-subindexed collections; `:spec` removed from the R5 hot poll and fetched once via bounded R5b (l.16-17, l.21). Closed.
2. **F2** — buffered seq 2 "AAA" then redelivered "BBB": store-if-absent (l.175) keeps "AAA"; first-wins now holds across the whole sequence window. Closed.
3. **F3** — 1.3 GB single line: truncated to ≤4 KiB + marker at capture (both executor paths, l.210-211) and defensively at kernel ingest (l.171); depot record, buffer, tails, and both read surfaces are byte-bounded (l.39, l.114). Closed.

## Verdict — Round 2

**PASS.** The unchanged architecture (three depots hash-by run-id, one microbatch topology, three PStates, layered spawn-exactly-once, query-topology view with structural token secrecy) re-survived every core-safety trace this round: single grant, no double spawn, no lifecycle regression, terminal immutability, token secrecy, atomic submit/grant pairs, restart behavior at every protocol step, and the full state×write matrix. The three amendments were validated adversarially by re-running round 1's failing scenarios plus new interaction checks (R5b's position relative to the spawn gate, store-if-absent atomicity, manual-path truncation coverage, cap-reject vs rejected-equals-absence) — all hold with cited mechanisms. No check in this artifact is a FAIL.
