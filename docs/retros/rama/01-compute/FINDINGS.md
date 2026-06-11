# Findings — Compute Track (R7 Consolidation)

<!-- R7. Merge of R3 (DESIGN_DIFF), R4 (IMPLEMENTATION_VALIDATION), R6 (TEST_VALIDATION),
     the R2 plan-validation loop (round-1 F1/F2/F3 → PLAN.md v2 amendments), and
     DIRECT_REVIEW.md (pre-pipeline benchmark). Every line citation re-verified against
     src/app/server/rama/dogfood/compute.clj and test/app/server/rama/dogfood_compute_test.clj
     at HEAD. Fix-refs point at FIX_PLAN.md batches. -->

## Verdict

The as-built module gets the **protocol shape** right — claim/grant/token fencing, spawn-after-grant discipline, back-arrow (executor writes only via depots), durable pending-inbox as restart anchor, token secrecy via the `run-view` whitelist — and the v1 design lesson (never spawn inside a topology event) was absorbed. But the implementation reintroduces the retry × state-reset bug class one layer down, at the PState-write level: the submit path is non-idempotent under both client redelivery and stream replay (run rows regress to `:pending` → second grant → **second OS spawn**), the grant/inbox-removal pair can partially commit under the stream topology (permanent stale inbox → infinite claim loop), and the authorization guard admits tokenless observations against ungranted runs (lifecycle fully forgeable without execution). Every core safety invariant the spec names — single grant, at-most-one-spawn-ever, inbox-iff-pending, observation authorization — is violable as built. The dominant write path (observations, 10³–10⁵/run) carries whole-row read-modify-write amplification into two PStates. Tests cover the happy path, one claim race, and one wrong-token observation; the rejection path, duplicate delivery, ordering, and terminal immutability have zero coverage. Recorded phase verdicts: **R2 round 1 FAIL (F1/F2/F3) → round 2 PASS** (the re-derived plan is a validated blueprint); **R4 major-fail**; **R6 major-fail**.

## Reconciliations (where sources disagreed)

1. **Stuck-state recovery (DIRECT_REVIEW F2, rated HIGH there).** The spec/plan explicitly DEFER stall detection and restart-reconcile to A2 ("must not be precluded", IMPLICIT_SPEC scope + ambiguity 9). Resolution — F2 splits into three:
   - **F2(a) executor restart → granted runs stall `:launching`/`:running` forever**: the spec-sanctioned A.0 stall ("recorded truth never claims success for work that didn't finish"). NOT a defect now. The fix plan must not preclude A2 recovery: keep the durable inbox anchor, add the `:last-heartbeat-ms` field (C-08's fix arms this), don't make terminal-status assumptions that block a future lease/sweep.
   - **F2(b) lost claim ack** (append lands, executor throws before registry put → token lost, run granted to nobody): same stalled-but-safe class, but the window is executor-created and cheaply shrinkable → kept as LOW hardening finding **C-20**, not HIGH.
   - **F2(c) pump-thread `.join` without timeout** (grandchild holds the pipe → worker thread blocked forever, exit observation never appended even though the process was killed): an avoidable executor defect NOW, not deferred scope → merged into **C-13** (MEDIUM).
2. **Write amplification severity** (R3 D3b HIGH vs DIRECT F7 MEDIUM): consolidated **HIGH** (C-04). It violates an explicit spec requirement on the spec's declared dominant path ("per-observation processing cost must be small and constant"), and because tasks are single-threaded the churn stalls every other run/read on the task. No truth corruption — it is perf-class HIGH, so its fix batch sits after the correctness batches.
3. **Heartbeat/type-set wedge** (R4 S5 MAJOR vs R3 D11c/D11d LOW-latent): consolidated **MEDIUM** (C-08). Unreachable with A.0's only writers, but it converts spec-NAMED observation types into permanent run wedging + unbounded buffer growth — that is preclusion, which the spec forbids; the fix is cheap.
4. **Post-terminal audit pollution** (R4 S7 / DIRECT F6 MEDIUM vs R3 D11b LOW-equivalent): consolidated **MEDIUM** (C-12). R3 was right that ignore-vs-error is a sanctioned ambiguity for a single late redelivery; the `:all-after` replay path (one retry late in a chatty run sprays up to 50 mislabeled entries, evicting genuine attack audit) is what lifts it to a real defect.
5. **Unknown-run ghost state** (R4 counts S4 among the structural failures; R3 D11a MED): consolidated **MEDIUM** (C-05). Severity here ranks consequence (permanent attacker-mintable garbage rows; no spawn path from them), not fix size — R4's "major" reflected fix shape, which FIX_PLAN handles.
6. **Dedup of the double-spawn surface**: R3 D10 + R3 D5(a) + R4 S2 + DIRECT F1 are **one root cause** (non-monotonic unconditional submit writes with no dedup anchor) with two triggers (client redelivery; stream replay) and several symptoms (lifecycle regression, double spawn, decision flip, two-inbox violation, destroyed terminal truth) → single finding **C-01**. The stream-vs-microbatch topology choice is a distinct root cause (it additionally breaks the grant/removal pair even with C-01 fixed) → **C-02**. R3 D5(b) + R4 S3 + DIRECT F3 are one root cause → folded into C-02.
7. **PLAN v2 amendments vs as-built** (R3/R4 ran against PLAN v1; R7 checked the v2 mechanisms against code directly): all three amendment mechanisms are absent as built — no admission size caps and no env support at all (**C-10**), buffered insert overwrites instead of store-if-absent (**C-07**, independently found by R4 as S6), no per-line byte cap anywhere (**C-11**). R7 code verification also surfaced one defect no artifact had: `stream-lines` materializes the whole output before any append (**C-09**, folded with DIRECT F4).

## What held up (do not break in the fix)

Verified by R4 and DIRECT_REVIEW, re-confirmed against code: token secrecy by `run-view` whitelist (compute.clj:311-317, asserted in tests); spawn-after-grant on both executor paths (504-521, 873-887); claim CAS first-wins under in-order processing (339-344, test-pinned); out-of-order drain core (441-450); deterministic decision/event ids — no id minting inside the topology (206-213); partition alignment throughout; claims against absent rows create no state (341); single `foreign-append!` per client operation; as-built admission validation depth (capability gate, drift checks, structured rejection errors, 129-191/198-204) is **better than the plan** (R3 D12) and must be kept.

---

## Findings

### HIGH

#### C-01 — HIGH — Submit path has no dedup anchor; duplicate/replayed submit regresses live or terminal runs → double spawn, decision flip, inbox re-arm

- **Citations:** compute.clj:622-638 (no existence guard; unconditional `termval`s), 627 (decision overwrite before the `<<if`), 635 (run row reset via `initial-run-row`), 638 (inbox re-add), 266-291 (fresh row: `:status :pending` at 274, `:claim-token nil` at 282, `:last-seq -1` at 287), 339-344 (`grantable-claim?` accepts the regressed `:pending`), 500-502 (registry entry already released → executor re-claims), 243/256 (`(now-ms)` inside the topology — even an identical duplicate rewrites `:decided-at`).
- **Scenario:** Run "r1" submitted, accepted, claimed, granted, spawned, completes `:succeeded`. Trigger A (no infra failure needed): the client redelivers the submit after a lost ack — the spec names this as expected (requester mints `:run/id`). Trigger B: the stream record replays after commit (stream.md:9). Either way the branch re-runs: decision overwritten (a now-invalid payload even flips `:accepted`→`:rejected`, destroying the audit record), run row reset to `:pending` with the grant winner's identity/token wiped, inbox entry re-added. The executor's registry no longer holds "r1" → next 50ms tick re-discovers it, claims, `grantable-claim?` sees `:pending` → second grant → **second OS spawn of the same command** (`deploy`, `git push`, `rm` …), and the recorded terminal truth is gone. A reused run-id with a different `:executor-task-id` hint also lands the run in a second inbox (O7: "never in two inboxes"). Violates O1 ("must never regress … back to `:pending`"), O2 single-grant, O8 at-most-one-spawn, O4 "decisions never flip".
- **Provenance:** both (R3 D10 + D5a; R4 S2 + stream-idempotency check; DIRECT_REVIEW F1).
- **Fix-ref:** Batch 1.

#### C-02 — HIGH — Stream topology where microbatch atomicity is required: grant/inbox-removal partial commit → permanent stale inbox entry + infinite claim-append loop

- **Citations:** compute.clj:616 (stream topology), 648-649 (grant writes, task = hash(run-id)), 650 (`(|hash *executor-task-id)` — commit boundary), 651 (inbox `NONE>` removal on the other task), 643-644 (`local-select>` + `:pending` guard skips the whole branch on replay), 523-531 + 519-521 (executor re-discovers the stale entry every tick, claims, classifies `:conflict-or-past`, drops registry entry, repeats), 38 (50ms tick). Submit-side window: 631-638 (row commits on task A, inbox write on task B can fail; a manual-path claim by run-id, 774-785, can grant before the retry tramples it).
- **Scenario:** Claim for "r1" processed: grant commits on the run's task (status `:launching`, token recorded); the streaming batch carrying the inbox removal fails before commit. Retry replays from the source block: the row now reads `:launching` → `grantable-claim?` false → the entire `<<if` including the removal is skipped — **the inbox entry is permanent**. The automatic executor then loops forever: re-discover → fresh claim append → no-op → registry drop → next tick (one depot append per ~50ms per stuck entry, unbounded claim-depot growth, no log line). Violates O7's iff-invariant. PLAN.md §Topologies cites exactly this hazard (and the submit row+inbox pair) as the decisive reason it chose microbatch; PLAN_VALIDATION round 2 re-verified that the latency budget fits microbatch with margin.
- **Provenance:** both (R3 D5b; R4 S3 + partial-failure check; DIRECT_REVIEW F3).
- **Fix-ref:** Batch 1.

#### C-03 — HIGH — Nil-token authorization bypass: a tokenless observation against any ungranted run is authorized; full lifecycle forgeable without execution

- **Citations:** compute.clj:392-396 (`authorized-observation?` — `(= (:claim-token run-row) (:claim-token obs))`), 282 (`:pending` row has `:claim-token nil`), 424-429 (forged `:exit` applies terminal status), 651 (only a grant removes the inbox entry), 523-531/519-521 (resulting infinite claim loop).
- **Scenario:** Run "r1" is `:pending`, never claimed. Any actor appends `{:run/id "r1" :observation/type :exit :sequence 0 :exit-code 0}` with **no** `:claim-token` key → `(= nil nil)` → authorized → seq 0 = expected → applied → "r1" is recorded `:succeeded` **without ever being claimed, granted, or executed**. A tokenless `:started` flips it `:running` with an attacker-chosen pid. The pending entry survives (nothing but a grant removes it), so the executor claims forever-ungrantable state — the same infinite claim loop as C-02. Directly violates O3's authorization invariant ("wrong token, **or any token while no grant exists**, must NOT mutate run output/status") and the `:pending × observation` matrix row. The plan's guard chain (authorization = grant-exists AND token-match) closes it.
- **Provenance:** skill-process (R4 S1). Not found by the direct review — the single most consequential pipeline-only finding.
- **Fix-ref:** Batch 2.

#### C-04 — HIGH — Dominant-path write amplification: whole-row read-modify-write per observation, ×2 PStates, plus whole-row grant polls

- **Citations:** compute.clj:617-620 (single-blob `Object` rows; no subindexing anywhere), 289-290 (tails as in-row vectors), 366-372 + 414-422 (`append-bounded` rewrite per line), 656-660 (every observation: `local-select>` whole row → fold → `termval` whole row into `$$compute-runs` AND `$$compute-views`), 311-317 (the view copy carries both 200-line tails and `:argv`), 711-713 + 511 (executor grant poll reads the full row incl. tails/buffer every 50ms per awaiting claim), 626/642/655 (redundant `(|hash *run-id)` after sources already partitioned by `:run/id`).
- **Scenario:** A build run prints 50k lines (spec: 10³–10⁵ observations/run is the dominant volume). Every line deserializes a row holding 400 capped tail lines + buffer + errors (~40KB+ once chatty), folds, and reserializes it **twice** — ~4GB serialization churn for one run, exactly the "Option A" PLAN.md enumerates and rejects ("write amplification on the hottest path"). Tasks are single-threaded, so the churn delays every other run and read colocated on the task. Violates the spec's "per-observation processing cost must be small and constant" (constant ✓, small ✗) and the skill's I/O-efficiency rule. Perf-class: no truth corruption, so it is fixed after the correctness batches despite HIGH severity (reconciliation #2).
- **Provenance:** both (R3 D2 + D3b + D9 note; R4 select-compute-transform FAIL + S10 + D8; DIRECT_REVIEW F7).
- **Fix-ref:** Batch 4.

### MEDIUM

#### C-05 — HIGH (corrected) — Observations for unknown run-ids FATALLY CRASH the topology (poison record), they do not create ghost rows

- **CORRECTION (post-comparison with the prior runtime-probed retro):** R3/R4/R7 and DIRECT_REVIEW all statically mis-traced this path as "creates a ghost row via `add-observation-error` on nil." Wrong: `fold-observation` (compute.clj:452-455) binds `expected (inc (long (:last-seq run-row)))` **eagerly in the let, before any cond guard** — with a nil run-row this throws `NullPointerException` at :455 inside the topology event. The prior retro (`docs/current-mental-model/build/rama-retro-review/02-compute-track/RAMA_REVIEW.md` F1, severity critical) **proved this at runtime**: one observation appended for an unknown run-id produced a fatal topology error and IPC exit 1.
- **Citations:** compute.clj:653-657 (obs branch selects row, calls fold with no nil guard), 452-455 (eager `(long (:last-seq nil))` NPE).
- **Scenario:** Any actor (stale executor, replay artifact, malformed client append) appends one observation for a never-submitted run-id → NPE in the event → under default `:individual` retry the record retries forever as a **poison record**, permanently erroring on that partition. Severity raised MEDIUM → HIGH: this is a one-record denial of the observation branch, strictly worse than the ghost-row consequence originally recorded.
- **Provenance:** input class flagged by both (R3 D11a; R4 S4); correct mechanism + severity from the prior retro's runtime probe — a method lesson: static tracing missed an eager let-binding that execution exposed immediately.
- **Fix-ref:** Batch 2 (absent-row guard BEFORE fold; dead-letter or bounded no-op for unknown-run observations).

#### C-06 — MEDIUM — Out-of-order observation buffer is unbounded

- **Citations:** compute.clj:473 (`(assoc-in run-row [:obs-buffer seq-id] obs)` — no cap), 441-450 (drain only on gap fill), 288 (in-row plain map, reserialized every event via C-04).
- **Scenario:** Any authorized writer that opens a sequence gap (bug, failed append per C-09, heartbeat desync per C-08, A2 multi-attempt writer) buffers every subsequent observation forever: 10⁵ post-gap lines = 10⁵ full observation maps in a non-subindexed in-row map, rewritten on every later event for that run; the run never closes and the row eventually dominates the task's I/O. Spec is unconditional: "the out-of-order buffer … must not grow without bound." Plan: subindexed, cap 1024, overflow → auditable `:observation/buffer-overflow` error.
- **Provenance:** both (R3 D3c; R4 non-subindexed-collections FAIL; DIRECT_REVIEW F4 "no buffer bound").
- **Fix-ref:** Batch 2 (cap + overflow error), Batch 4 (subindexing).

#### C-07 — MEDIUM — Buffered duplicate with a different payload overwrites the first (not store-if-absent)

- **Citations:** compute.clj:473 (unconditional `assoc-in` overwrite), 466-467 (watermark protects only already-applied seqs).
- **Scenario:** next-seq=0; seq 2 `:stdout "AAA"` buffers; a redelivered/corrupted seq 2 `:stdout "BBB"` arrives while the gap is open → `"BBB"` **replaces** `"AAA"` and is what the drain applies. Violates the spec edge "duplicate sequence numbers with different payloads (must keep the first, never corrupt)". PLAN v2 amendment F2 specifies store-if-absent (write only when `(keypath seq)` navigates to nil), making first-wins hold across the whole sequence window.
- **Provenance:** skill-process (R4 S6; anticipated plan-side by the R2-loop F2 amendment).
- **Fix-ref:** Batch 2.

#### C-08 — MEDIUM — Error-path watermark wedge + closed observation-type set: spec-named types (`:heartbeat`, `:failed`, …) permanently wedge a run; no liveness timestamp exists

- **Citations:** compute.clj:47-48 (`observation-types #{:started :stdout :stderr :exit}`), 460-461 (type-invalid checked before the sequence compare; `:last-seq` never advances on any error path), 473 (subsequent observations buffer forever → feeds C-06), 266-291 (no `:last-heartbeat-ms` field anywhere).
- **Scenario:** A spec-compliant writer sends authorized `:heartbeat` at seq 2 → `:observation/type-invalid` error, watermark stays at 2 → stdout seq 3 buffers, `:exit` seq 4 buffers → gap never fills → run stuck `:running` forever with an ever-growing buffer. The spec's matrix row (`:running × :heartbeat` → liveness timestamp updated, no status change) fails outright, and the missing timestamp removes the data A2 stall detection is specified to need. Unreachable with A.0's only writers, but the spec forbids precluding the named types (reconciliation #3). Plan: unknown types from an authorized token fold to an error **at the apply step, after the sequence is consumed/advanced** — no deadlock; `:heartbeat` and `:failed`+`:failure-reason` applied per guard step 5.
- **Provenance:** skill-process (R3 D11c + D11d; R4 S5).
- **Fix-ref:** Batch 2.

#### C-09 — MEDIUM — Executor observation pipeline: sequence consumed before durable append (one failed append wedges the run), and output is fully buffered to EOF before any append (no live logs, unbounded memory)

- **Citations:** compute.clj:817-822 (`(swap! seq* inc)` at 819 BEFORE `append-observation!`, which can throw), 802-805 (`stream-lines` = `with-open` + `(doall (line-seq reader))` — entire stream realized in memory before the caller's `doseq` appends anything), 840-847 (pump threads call `(doseq [line (stream-lines …)] (append! …))`), 854-856 (`:exit` appended after both joins).
- **Scenario:** (a) A throwing append (transient depot failure) kills the pump thread with a permanent hole in the sequence — every later observation including `:exit` buffers forever (into C-06's unbounded buffer); the run never closes. The plan's acked-cursor discipline ("an observation is 'sent' only once durably appended" — retry the SAME seq until acked) prevents holes by construction. (b) Because `doall` realizes the whole stream first, no `:stdout`/`:stderr` observation is appended until the process closes the stream: a long-running build shows zero output until the end (violates the O3 "live logs" sub-second observation-to-view expectation) and the executor holds the entire output in memory (unbounded; the tests' `echo`/`false` never expose it).
- **Provenance:** direct-review (F4) for the sequence hole; the EOF-buffering half found in R7 code verification (no prior artifact had it).
- **Fix-ref:** Batch 3.

#### C-10 — MEDIUM — No admission size caps, and no env support at all: an arbitrarily large spec lands in three PStates forever; child inherits the full kernel environment

- **Citations:** compute.clj:129-191 (validation checks shape, never size — argv only "non-empty vector of strings" at 182-184), 94-118 (`run-command-request` has no `:env` field anywhere; PLAN/spec O1 name an env allow-list), 834 (`ProcessBuilder` with no environment manipulation → full parent env inherited), 233-243 (accepted decision embeds the full request payload — the giant spec stored a second time), 311-317 (`run-view` includes `:argv` — it reaches the UI surface too), 656-660 (the giant row then rides every whole-row rewrite, ×C-04).
- **Scenario:** A submit with a 100 MB argv is accepted and stored verbatim in `$$compute-runs`, `$$compute-decisions-by-run-id` (embedded event), and `$$compute-views` — permanently (no deletion), reserialized twice per output line, breaking O6's bounded-view invariant. Separately, every spawned process sees the kernel's entire environment — the spec's env-allow-list constraint is unimplemented, not just uncapped. PLAN v2 amendment F1 specifies the enforcement: argv ≤1024 elems/≤128 KiB, cwd ≤4 KiB, env ≤128 entries/≤32 KiB, over-limit → `:rejected :request/spec-too-large`.
- **Provenance:** skill-process (R2-loop amendment F1, verified against code in R7; in no review artifact).
- **Fix-ref:** Batch 4.

#### C-11 — MEDIUM — No per-line byte cap: a huge single line breaks the byte-bounded tail/view invariant

- **Citations:** compute.clj:842-847 (pump threads forward lines verbatim), 414-422 (tails store the full line; `append-bounded` caps entries at 200, not bytes), 473 (buffered entries equally unbounded), 311-317 (the line reaches the view).
- **Scenario:** A process emits one 1.3 GB line (`base64`-style) → one observation record of ~1.3 GB through the depot, one ~1.3 GB tail entry in `$$compute-runs` and `$$compute-views`, then rewritten whole on every subsequent line (×C-04). Spec edge: "huge single line (must not break the bounded tail)"; O6 "bounded size regardless of process output volume" — the entry axis is capped, the byte axis is not. PLAN v2 amendment F3: truncate to ≤4 KiB + explicit marker at the pump threads (both executor paths) AND defensively at kernel ingest.
- **Provenance:** skill-process (R2-loop amendment F3, verified against code in R7; in no review artifact).
- **Fix-ref:** Batch 4.

#### C-12 — MEDIUM — Post-terminal authorized observations are mislabeled `:observation/not-authorized`; `:all-after` replay sprays false audit entries that evict genuine ones

- **Citations:** compute.clj:396 (terminal-status check folded into `authorized-observation?`), 457-458 (error appended before the watermark compare is reached), 653 (`{:retry-mode :all-after}` replays already-processed records), 366-372 + 36 (ring cap 50 — false entries evict real ones).
- **Scenario:** A chatty run completes; a later record on the partition fails → `:all-after` replays the run's own already-applied observation suffix → each replayed record now fails the terminal conjunct of "authorization" → up to 50 `:observation/not-authorized` entries, written into truth and view, evicting any genuine unauthorized-writer audit entries. The audit surface — the spec's mechanism for seeing real attacks — is corrupted by an infrastructure retry, with a misleading reason label. Plan order fixes it: authorization (grant+token) first, then terminal → authorized-ignore-silently / unauthorized-error, then watermark.
- **Provenance:** both (R4 S7 + idempotency check; DIRECT_REVIEW F6; R3 D11b noted the replay angle).
- **Fix-ref:** Batch 2.

#### C-13 — MEDIUM — `close()` leaks live OS processes and fabricates `:failed`/127 terminal truth; pump-thread joins can block worker threads forever

- **Citations:** compute.clj:549-554 + 598-602 (`shutdownNow` on pools; live `Process` objects never destroyed), 848 (worker interrupted at `.waitFor`), 863-865 (`catch Throwable` → synthetic `:stderr` + `:exit 127` appended for a process that is still running), 852 (only the manual-timeout path calls `destroyForcibly`), 854-855 (`.join` with no timeout — a grandchild holding the pipe blocks the join forever; the exit observation is then never appended).
- **Scenario:** (a) Runtime closed while a command runs → process leaks (O9: "cleanly stop … worker processes so test runs don't leak threads/processes") AND a `:failed`/127 terminal state is recorded for work still executing — fabricated truth (never false *success*, the spec's hard line, but false failure with a fake exit code). (b) `destroyForcibly` kills the child but a grandchild keeps stdout open → pump thread never reaches EOF → `.join` blocks the worker thread permanently and no `:exit` is appended → run stalls and a thread leaks.
- **Provenance:** both (R4 S8; DIRECT_REVIEW F2c).
- **Fix-ref:** Batch 3.

#### C-14 — MEDIUM — Blank/missing run-id rejection writes the decision under a nil/blank key into a `{String …}` PState

- **Citations:** compute.clj:154-156 (validation flags blank run-id), 245-256 (rejected decision still built with the bad `:run/id`), 625-627 (`*run-id` = nil/"" → `(|hash nil)` → `(keypath nil)` `termval` into `$$compute-decisions-by-run-id`, declared `{String (map-schema Keyword Object)}` at 618).
- **Scenario:** A malformed submit with `:run/id nil` reaches the topology. If Rama enforces the key type, the write throws → under `:individual` retry the record becomes a poison event retrying forever; if it doesn't, an unreachable decision row exists under a nil key (and all blank-string submits collide on `""`). Either way the spec's "rejected **or refused**" arm is not implemented kernel-side. Plan: refuse blank ids client-side in the submit helper AND drop them topology-side before any keyed write. Open doubt preserved from DIRECT_REVIEW: which arm Rama actually takes — the Batch 1 test settles it.
- **Provenance:** direct-review (F5). Not found by R3/R4.
- **Fix-ref:** Batch 1.

### LOW

#### C-15 — LOW — All PState values are `(map-schema Keyword Object)`: Rama schema enforcement lost entirely

- **Citations:** compute.clj:617-620; plain maps for buffered observations (473) and errors (374-380).
- **Scenario:** Any malformed write (typo'd key in `grant-claim`, string exit-code) is accepted silently and surfaces only at read time, far from the writing event. Plan: `fixed-keys-schema` + typed leaves; `IObservation` interface + per-variant defrecords for the buffer; `ObservationError` record — "no `Object` anywhere".
- **Provenance:** skill-process (R3 D3a; R4 D5).
- **Fix-ref:** Batch 4.

#### C-16 — LOW — Pending inbox is a non-subindexed nested map of 5-field entry maps; unbounded via the sanctioned dangling-hint path

- **Citations:** compute.clj:619 (schema), 303-309 (entry map — consumers only ever use the keys: 526, 881; test:73), 638/651 (add/remove), 723-728 (whole-map read every 50ms tick).
- **Scenario:** Requests hinted at a dead inbox accumulate forever (stall handling deferred); each accept rewrites the whole inbox map and each reconcile tick deserializes it whole. Plan: `{String (set-schema String {:subindex? true})}`.
- **Provenance:** skill-process (R3 D3f; R4 non-subindexed check).
- **Fix-ref:** Batch 4.

#### C-17 — LOW — JVM-global executor state atom; `close-compute-runtime!` tears down every executor in the JVM; identity-hash keying

- **Citations:** compute.clj:538-539 (`defonce` atom), 578/599 (`System/identityHashCode` keys — collision-unsafe), 556-561 + 678-684 (`close-all-compute-executor-states!` closes ALL executors, any runtime).
- **Scenario:** Two coexisting runtimes (parallel tests, second module instance) kill each other's executors on close. Harness-level today (one runtime per test), but it is module code and forbids parallel test execution.
- **Provenance:** both (R4 S9; DIRECT_REVIEW F10; R3 D6 note).
- **Fix-ref:** Batch 5.

#### C-18 — LOW — Reconcile loop swallows every Throwable silently

- **Citations:** compute.clj:592-594 (`(catch Throwable _ nil)`).
- **Scenario:** Persistent failure (bad PState handle, serialization error) → the executor does nothing, forever, invisibly — operational blindness; also masks DIRECT_REVIEW's open doubt about TaskGlobal reads racing module launch.
- **Provenance:** both (R4 D7 note; DIRECT_REVIEW F8).
- **Fix-ref:** Batch 5.

#### C-19 — LOW — Unbounded process concurrency per task

- **Citations:** compute.clj:572 (cached thread pool), 523-531 (claims everything pending each tick).
- **Scenario:** N pending runs → N simultaneous OS processes + 2N pump threads per task, no cap. Human-paced in A.0; agent-initiated compute changes that.
- **Provenance:** direct-review (F9).
- **Fix-ref:** Batch 5.

#### C-20 — LOW — Lost-ack claim window: registry updated only after the claim append returns

- **Citations:** compute.clj:528-530 (`append-claim!` at 529 before `.put` at 530).
- **Scenario:** Append lands, executor throws before the registry put → grant goes to a token nobody holds → run stalls `:launching` (the sanctioned stalled-but-safe class — see reconciliation #1, F2b — but the window is executor-created and cheaply shrinkable: register before appending, remove on append failure).
- **Provenance:** direct-review (F2b).
- **Fix-ref:** Batch 3.

#### C-21 — LOW — Mechanical/contract hygiene cluster

- **Items + citations:** consecutive `keypath` pairs (compute.clj:638, 651 → `(keypath *executor-task-id *run-id)`); hand-rolled `foreign-select-one` (`select-pstate-one`, 707-709 — built-in exists); redundant `(|hash *run-id)` after sources already partitioned by `:run/id` (626, 642, 655); manual path returns the claim-result map instead of the spec's `nil` when it loses a claim outright (884-887 — tests only exercise the empty-inbox nil); errors ring 50 with no total counter (36 — plan: 100 + `:observation-error-count`, fully resolving ambiguity 7); synthetic exit codes 124/127 conflate infra failure with process exits and record no `:failure-reason` for A2 (852-853, 865 — spec-acceptable per ambiguities 5/6, plan's `:failed`+`:failure-reason` is strictly more informative).
- **Provenance:** skill-process (R4 mechanical checks; R3 D7/D3d notes).
- **Fix-ref:** Batch 5 (the `:failed`/`:failure-reason` item lands with Batch 2's type work).

---

## Test-Coverage Findings (R6)

#### T-01 — MEDIUM — Four IPC launches where one suffices

- **Citations:** dogfood_compute_test.clj:35, 56, 82, 100 (four `deftest`s, each through `with-compute-runtime`, 10-16); run-ids and inboxes verified disjoint (automatic path uses the task's own inbox; manual tests pin `"local"`).
- **Scenario:** ~3 × 30+ s of unnecessary IPC startup per suite run; no shared mutable state justifies any extra launch (the JVM-global atom C-17 is an argument FOR one deftest). Restructure to one `deftest` with `testing` blocks.
- **Provenance:** skill-process (R6).
- **Fix-ref:** Batch 6.

#### T-02 — HIGH — The entire rejection path is untested

- **Citations:** no test submits an invalid request (suite read in full); spec O1 invariants + matrix `absent × submit (invalid)`.
- **Scenario:** Zero coverage of one of the two decision outcomes: bad target kind, empty argv, blank run-id (the C-14 path has never executed), unauthorized actor, never-claimable/never-pending guarantees.
- **Provenance:** both (R6 gap a; DIRECT_REVIEW "validation rejections through the topology … never executed").
- **Fix-ref:** Batch 6.

#### T-03 — HIGH — Duplicate submit / replay untested in every state

- **Citations:** no test redelivers a submit; matrix rows `accepted/:pending/:launching/terminal × duplicate submit`.
- **Scenario:** The double-spawn re-arm surface (C-01) — the spec's own named risk — has no regression test; nor does decision-never-flips or inbox-exactly-once.
- **Provenance:** both (R6 gap b; DIRECT_REVIEW "replay/restart" zero coverage).
- **Fix-ref:** Batch 6.

#### T-04 — HIGH — Observation ordering, redelivery, and duplicate-payload semantics untested

- **Citations:** the only manually-appended observation is one wrong-token write (test:136-139); matrix out-of-sequence rows; O3 retry-safety; "must keep the first" edge.
- **Scenario:** The core O3 invariants — buffering, gap-fill order, exit-drains-buffer-first, watermark dedup, buffered-duplicate first-wins (C-07) — have zero coverage.
- **Provenance:** both (R6 gaps c, d, q; DIRECT_REVIEW "observation gaps").
- **Fix-ref:** Batch 6.

#### T-05 — MEDIUM — Terminal immutability and post-terminal writes untested

- **Citations:** no test writes anything after a run reaches terminal state; matrix `terminal × observation` / `terminal × claim`.
- **Scenario:** Late/redelivered observations and claims against terminal runs (C-12's surface) unpinned; no test would catch a reopened run.
- **Provenance:** both (R6 gap e; DIRECT_REVIEW "claims against terminal runs").
- **Fix-ref:** Batch 6.

#### T-06 — MEDIUM — Unknown-id writes/reads, `:pending × observation`, and claim-protocol edges untested

- **Citations:** R6 gaps f, g, h, i, o; R6's correction: the spec's "`:pending × observation` is an exact test scenario" note is wrong — test 4's wrong-token observation arrives at `:launching` (after the grant at test:118), not `:pending`.
- **Scenario:** Claims/observations against absent rows (C-05's surface), no-grant-yet authorization (C-03's surface — nil-token), winner-claim redelivery, same-token-different-id (the AND half of the grant check), unknown-id reads.
- **Provenance:** skill-process (R6).
- **Fix-ref:** Batch 6.

#### T-07 — MEDIUM — Boundedness, heartbeat, spawn failure, exit-only, concurrency, and per-view secrecy asserts untested

- **Citations:** R6 gaps j, k, l, n, p, m; token secrecy asserted in only 2 of 4 view-reading tests (test:54, 80 — missing at 92 and 141, the latter a `:launching` view where a token DOES exist on the row).
- **Scenario:** No test exceeds the tail/error caps (C-04/C-11 surfaces), sends `:heartbeat` (C-08), spawns a nonexistent binary, closes a run with `:exit` alone, runs two runs concurrently, or asserts secrecy on every view read as the spec's evidence note claims.
- **Provenance:** skill-process (R6).
- **Fix-ref:** Batch 6.

---

## Calibration (for the method report)

Code findings: 21 (4 HIGH, 10 MEDIUM, 7 LOW). Test findings: 7.

| Provenance | Code findings | Count |
|---|---|---|
| **both** | C-01, C-02, C-04, C-06, C-12, C-13, C-17, C-18 | 8 |
| **only skill-process** | C-03 (R4), C-05 (R3+R4), C-07 (R4, R2-loop), C-08 (R3+R4), C-10 (R2-loop), C-11 (R2-loop), C-15, C-16, C-21 | 9 |
| **only direct-review** | C-09*, C-14, C-19, C-20 | 4 |

*C-09's EOF-buffering half was found in R7 code verification, in no prior artifact.

Test findings: T-02..T-05 both (DIRECT's one-sentence coverage note vs R6's 17 itemized gaps); T-01, T-06, T-07 only-skill (R6).

**Takeaways:**
- **Both:** the pipeline independently re-found the entire DIRECT F1/F3 class (retry × state reset, cross-partition partial writes) plus the amplification cluster — the README's pilot trust gate is met. Convergence on C-01/C-02 from a code-blind plan (which *predicted* both hazards as the reason to choose microbatch) is the strongest evidence the phased method works.
- **Only skill-process:** the single worst attack-shaped finding (C-03 nil-token bypass) came from R4's verbatim guard-tracing, and the whole boundedness family (C-10/C-11 admission and byte caps, C-07 store-if-absent) exists only because the R2 adversarial loop forced the plan to name enforcement mechanisms the direct review never formulated as requirements. The spec matrix discipline (R0) is what made C-05/C-08 and R6's 17 test gaps enumerable rather than ad hoc.
- **Only direct-review:** executor-side JVM mechanics — failed-append sequence holes (C-09), the nil-key poison-event hazard (C-14), concurrency caps (C-19), the lost-ack window (C-20). The pipeline's R4 is topology/PState-centric; the direct falsification pass ranged wider over process/thread/ack lifecycles. The methods are complementary, not redundant.
- **Severity framing:** the pipeline's spec/plan anchoring prevented one mis-rank the direct review made — F2's HIGH conflated sanctioned deferred-scope stalls with defects-now (reconciliation #1); the spec's explicit A.0/A2 split is what made that separation principled.
