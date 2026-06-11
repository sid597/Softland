# Design Diff — as-built `compute.clj` vs re-derived PLAN.md

<!-- Retro phase R3. Structural diff of the as-built module against the skill-process
     re-derived plan. Inputs: IMPLICIT_SPEC.md, PLAN.md, src/app/server/rama/dogfood/compute.clj,
     test/app/server/rama/dogfood_compute_test.clj, .claude/skills/rama/SKILL.md + references.
     Independence: DIRECT_REVIEW / PLAN_VALIDATION / IMPLEMENTATION_VALIDATION / TEST_VALIDATION
     and docs/current-mental-model/ were NOT read. -->

Classifications: `as-built-defect` | `equivalent` | `as-built-better` | `plan-defect`.
Severity: consequence severity of the divergence (HIGH/MED/LOW). Citations are
`compute.clj:<line>` for as-built and PLAN.md section names for the plan.

---

## D1. Depots — names, partitioning, separation

- **As-built:** three depots, all `(hash-by :run/id)`: `*compute-depot`, `*compute-claim-depot`, `*compute-obs-depot` (compute.clj:609-611).
- **Plan:** three depots, all `(hash-by :run/id)`: `*compute-request-depot`, `*compute-claim-depot`, `*compute-observation-depot` (PLAN.md "Depots").
- **Classification:** `equivalent`. Identical structure (3-way stream separation, identical partitioner); names differ only.
- **Consequence:** None. LOW.

## D2. PState set — materialized view PState exists

- **As-built:** four PStates, including `$$compute-views {String (map-schema Keyword Object)}` (compute.clj:620), written with a full `run-view` projection on **every** submit (636), grant (649), and observation (660). No query topologies.
- **Plan:** three PStates only; `read-view` is served by a `run-view` **query topology** over `$$runs`. The plan explicitly rejects a second materialized PState because it "doubl[es] the per-line write cost on the dominant write path (every stdout line would be written twice)" (PLAN.md "No materialized view PState (decision)").
- **Classification:** `as-built-defect`. Violates SKILL.md "Never trade I/O efficiency for code simplicity" and "Minimize storage I/O" (SKILL.md:30, 67): the spec's dominant write volume is observations (IMPLICIT_SPEC O3: 10³–10⁵ per run), and each observation event serializes and writes **two** near-full row copies (`$$compute-runs` at :659 and `$$compute-views` at :660 — the view includes both 200-line tails via `run-view` select-keys, compute.clj:311-317). The poll-based read contract (`await-view`) is fully satisfiable by a query topology at zero extra write cost. The one capability a materialized view adds — reactive PState subscription — is neither required by the spec nor used by the as-built code.
- **Concrete scenario:** a build run emits 10⁵ stdout lines → 10⁵ × 2 full-row serializations instead of 10⁵ × O(1) subindexed entry writes + 0 view writes. Multiplies the D3b amplification by 2.
- **Severity:** MED (amplifier on the HIGH-severity D3b path).

## D3. PState schemas

### D3a. `Object` map schemas vs typed fixed-keys schemas

- **As-built:** every PState value is `(map-schema Keyword Object)` (compute.clj:617-620). No fixed-keys schemas, no typed leaves, no typed records for buffered observations or observation errors (plain maps, compute.clj:374-380, 473).
- **Plan:** `fixed-keys-schema` with typed leaves for `$$decisions` and `$$runs`; `IObservation` interface + per-variant defrecords for the buffer; `ObservationError` record; "no `Object` anywhere" (PLAN.md "$$runs", "Topologies and PStates").
- **Classification:** `as-built-defect`. pstate-schema.md:219: when a position holds typed data, "do NOT use `Object` ... this makes schema enforcement weak." Failure scenario: any malformed write (e.g. a typo'd key in `grant-claim`, a string exit-code) is accepted silently and surfaces only at read time, far from the writing event.
- **Severity:** LOW (no runtime failure today; loses Rama's schema enforcement entirely).

### D3b. Output tails: in-row plain vectors (read-modify-write) vs subindexed dense-index maps

- **As-built:** `:stdout-tail` / `:stderr-tail` are plain vectors inside the run row, capped at 200 via `append-bounded` (compute.clj:366-372, 414-422); every observation event reads the **entire** run row (`local-select> [(keypath *run-id)]`, compute.clj:656), folds, and rewrites the **entire** row with `termval` (compute.clj:659) — tails, obs-buffer, errors, spec fields, everything — plus the view row (D2).
- **Plan:** this exact design is enumerated as **Option A and rejected**: "read whole tail (~200 lines ≈ 20KB deserialize) + rewrite whole tail. At 10⁵ lines per chatty run this is gigabytes of serialization churn per run — write amplification on the hottest path." Chosen design is Option B: `(map-schema Long String {:subindex? true})` keyed by dense line index with O(1) entry-write + trim-delete per line (PLAN.md "$$runs" Options A/B).
- **Classification:** `as-built-defect`. Violates SKILL.md:30 ("Never trade I/O efficiency for code simplicity" — per-event cost is multiplied by every observation in production) and the subindexing rule (SKILL.md:61; pstate-schema.md:51-57 — cap 200 exceeds the ~100-element threshold, and the rule says reason about worst case, not typical). The spec makes this the dominant write path and demands "per-observation processing cost must be small and constant" (IMPLICIT_SPEC O3 throughput).
- **Concrete scenario:** chatty test run printing 50k lines: every line deserializes + reserializes a row holding 400 capped tail lines + buffer + errors (~40KB+), twice (runs + views) → ~4GB serialization churn for one run; task-thread occupancy creates latency spikes for every other run and read on that task (tasks are single-threaded, SKILL.md:24-26).
- **Severity:** HIGH.

### D3c. Out-of-order observation buffer: unbounded vs capped

- **As-built:** `:obs-buffer` is a plain in-row map with **no cap**: `(assoc-in run-row [:obs-buffer seq-id] obs)` (compute.clj:473); drain on gap-fill only (compute.clj:441-450).
- **Plan:** `:buffered` subindexed, **cap 1024**, overflow → recorded `:observation/buffer-overflow` error + reject (PLAN.md "$$runs", guard chain step 4).
- **Classification:** `as-built-defect`. IMPLICIT_SPEC O3 (data growth) is explicit: "The out-of-order buffer ... must not grow without bound." An authorized writer that skips a sequence number (bug, or A2 multi-attempt writer) buffers every subsequent observation forever — and because of D3b, the ever-growing buffer is reserialized into the row on **every** event.
- **Concrete scenario:** executor bug emits seq 1..N without seq 0 → N observations accumulate in `:obs-buffer`; row grows unboundedly; every further observation rewrite gets slower; run never closes and the row eventually dominates the task's I/O.
- **Severity:** MED (requires a misbehaving authorized writer; spec requirement is unconditional).

### D3d. Observation-errors bound: ring of 50 vs ring of 100 + total counter

- **As-built:** `append-bounded` ring, cap 50 (`default-error-limit`, compute.clj:36, 386-389). No total counter.
- **Plan:** ring cap 100 + `:observation-error-count` total-ever counter "so spam volume stays auditable past the cap" (PLAN.md "$$runs").
- **Classification:** `equivalent`. Spec ambiguity 7 requires bounded + visible; both deliver. The plan's counter is a nicety (full spam volume auditable); cap size is taste.
- **Consequence:** as-built loses count of unauthorized attempts beyond the most recent 50. LOW.

### D3e. Decision record contents: full embedded event vs compact record

- **As-built:** accepted decisions embed the entire KernelEvent including the full request payload (compute.clj:233-243), stored per run-id forever.
- **Plan:** compact `fixed-keys-schema` (status, request id/type, reason, decided-at) (PLAN.md "$$decisions").
- **Classification:** `equivalent`. Write volume is human-paced (spec O1); storage duplication of the spec/payload across `$$compute-decisions-by-run-id` and `$$compute-runs` is a taste/cost tradeoff, and the embedded event is genuine provenance. LOW.

### D3f. Pending inbox: nested map of entry-maps (not subindexed) vs subindexed set of run-ids

- **As-built:** `$$compute-pending-by-task {String {String (map-schema Keyword Object)}}` (compute.clj:619) — inner collection not subindexed; each entry is a 5-field `pending-entry` map (compute.clj:303-309) although consumers only use the keys (`contains?`/`sort (keys ...)`, compute.clj:526, 881; test:74).
- **Plan:** `{String (set-schema String {:subindex? true})}` — subindexed **because** the worst case is unbounded: "a dead/never-existing inbox accumulates without bound in A.0 (stall handling is explicitly deferred)" (PLAN.md "$$pending-by-executor").
- **Classification:** `as-built-defect`. pstate-schema.md:51-57: subindex if ANY instance can grow large; "bounded in practice" doesn't count, and A.0 has no stall handling, so a dangling executor-assignment hint (spec O1 edge case, exploited by tests) grows an inbox forever. Without subindexing, every accept/remove on that key rewrites the whole inbox map, and every executor poll (`read-pending`, every 50ms per task, compute.clj:723-728, 587-597) deserializes the whole map.
- **Concrete scenario:** a batch of requests hinted at a dead inbox accumulates 10k entries; each subsequent accept on that inbox rewrites a 10k-entry map; each reconcile tick reads it whole.
- **Severity:** LOW (human-paced write volume in A.0; unbounded only via the deferred-stall path).

## D4. Partitioning & write colocation

- **As-built:** all three depots hash-by `:run/id`; submit/claim/obs writes land on the run's task; single `(|hash *executor-task-id)` hop for inbox add/remove (compute.clj:626, 637-638, 642, 650-651, 655).
- **Plan:** identical colocation: depot processing colocated with `$$decisions`/`$$runs`, one inbox hop (PLAN.md "Design Decisions — Colocation").
- **Classification:** `equivalent`. The leading `(|hash *run-id)` after each `source>` is a same-task hop (depots are already partitioned by `:run/id`) — harmless, adds only an event-tree boundary. Structure agrees with plan exactly.
- **Consequence:** None. LOW.

## D5. Topology type — stream vs microbatch (retry/idempotency core)

- **As-built:** one **stream** topology, `"compute-run-command-topology"` (compute.clj:616); request and claim sources use default `:individual` retry; obs source uses `{:retry-mode :all-after}` (compute.clj:653). No client-duplicate guards on the submit path (see D10).
- **Plan:** one **microbatch** topology; the decisive argument is idempotency/atomicity, not latency (PLAN.md "Topologies and PStates" items 1-4): (a) submit and grant each span two partitions (run task + inbox task) and need cross-partition exactly-once; (b) observation folds include non-idempotent-shaped writes. Latency budget shows microbatch fits the 5s bound with margin.
- **Classification:** `as-built-defect`. The skill rule is absolute: "Never trade fault tolerance for code simplicity. Duplicate side effects from retried processing are bugs" (SKILL.md:32). Stream semantics that make the as-built design wrong (stream.md:9, 35-41, 92-96; core-concepts.md:10-11):
  - A stream topology can replay a record **after all its PState writes committed** (stream.md:9).
  - Writes commit at partitioner boundaries; cross-partition writes commit independently and committed groups are NOT rolled back on retry (stream.md:35, 92-96).

  Two concrete failure traces, both impossible under the plan's microbatch (microbatch.md:3, 31, 37 — exactly-once PState updates, cross-partition atomicity by construction):

  **(a) Submit replay → lifecycle regression → double spawn.** Submit processing has no already-processed guard: it unconditionally `termval`s a fresh `:pending` row with nil claim fields over `$$compute-runs[run-id]` (compute.clj:631-638 via `initial-run-row`, compute.clj:266-291). Trace: submit commits → automatic executor (50ms tick) claims, is granted, spawns, run completes `:succeeded` → Rama's progress tracking for the submit record fails → record replays → row overwritten back to `:pending`, inbox entry re-added (compute.clj:638) → executor registry no longer contains the run-id (removed in the `finally` at compute.clj:500-502) → executor re-claims → `grantable-claim?` sees `:pending` (compute.clj:339-344) → second grant → **second OS spawn of the same command**, and the recorded `:succeeded` truth was destroyed. Violates spec O1 ("must never regress an in-flight or terminal run back to `:pending`"), O8 ("at most one spawn per run, ever"), and terminal immutability.

  **(b) Grant/inbox-removal partial commit → permanent stale inbox → infinite claim loop.** The grant (`$$compute-runs` write, compute.clj:648) commits at the `(|hash *executor-task-id)` boundary (compute.clj:650) before the inbox `NONE>` removal (compute.clj:651) commits. If the removal-side streaming batch fails, retry replays from the source; `grantable-claim?` now sees `:launching` → the whole `<<if` is skipped → the inbox entry is **never** removed. The executor then re-discovers the run on every 50ms tick: not in registry → append fresh claim → `claim-state` = `:conflict-or-past` → registry entry removed (compute.clj:519-521, 523-531) → next tick repeats, forever. Unbounded claim-depot growth + a busy loop per stale entry; violates O7's invariant ("a run-id appears here iff the run is `:pending`"). No double spawn (status never returns to `:pending`), but a permanent, self-sustaining leak.

  Credit where due: the observation path **is** retry-safe as built — single-task atomic group, `:last-seq` watermark makes replays no-ops (compute.clj:466-467), and `:all-after` preserves per-partition order under retry. The defect is the submit and claim paths.
- **Severity:** HIGH.

## D6. State primitives — PState vs TaskGlobal vs external

- **As-built:** PStates for decisions/runs/pending/views; one TaskGlobal `*compute-executor` per task (compute.clj:612-615) holding scheduler thread, cached worker pool, `ConcurrentHashMap` registry. Per-instance state is held in a **process-global atom** keyed by `System/identityHashCode` (compute.clj:538-539, 578-586), and `close-compute-runtime!` calls `close-all-compute-executor-states!`, which tears down every executor in the JVM (compute.clj:556-561, 678-684). Executor identity is the stable `"compute-executor-<task-id>"` with no per-launch nonce (compute.clj:568).
- **Plan:** same primitive split (PLAN.md "State primitive selection"): durable PStates as truth, non-durable TaskGlobal executor whose registry is a cache rebuilt from `$$pending-by-executor` + the R5 3-state check; identity = task-id + per-launch nonce.
- **Classification:** `equivalent`. The global-atom workaround is functionally per-instance and `close()` removes its own entry (compute.clj:598-602); the nonce is redundant belt-and-braces — the fresh per-claim UUID token (compute.clj:324) is what disambiguates pre-restart grants in `claim-state` (identity AND token, compute.clj:356-364), so a restarted instance classifies old grants `:conflict-or-past` either way. Notes: `close-all-...` closing every executor in the JVM is a cross-runtime leak if two IPCs ever coexist in one test JVM (LOW); nothing in the TaskGlobal is truth in either design.
- **Consequence:** None under the exercised one-runtime-per-test lifecycle. LOW.

## D7. Executor protocol — claim/grant/spawn/observe lifecycle

- **As-built:** discover pending (sorted) → skip if in registry → mint fresh UUID token claim → `:append-ack` → registry `:awaiting-grant` → poll `read-run` → 3-state `claim-state` → `:granted-to-us` → spawn on worker thread; stdout/stderr pump threads; `:started`/`:stdout`/`:stderr`/`:exit` with monotonic seq from 0 (compute.clj:504-536, 817-871). Timeout → `destroyForcibly` + `:exit` 124; spawn exception → `:stderr` line + `:exit` 127 (compute.clj:848-871). Manual path `run-one-pending-local!` runs the identical claim/grant/spawn cycle (compute.clj:873-887).
- **Plan:** the same five-step protocol, spawn-after-committed-grant, in-flight registry, identical manual path (PLAN.md "Design Decisions — Spawn exactly-once-or-reconciled", "Manual path"). Ambiguity resolutions differ: spawn failure / timeout are reported as a `:failed` observation with `:reason :spawn-failed` / `:timeout` (no fabricated exit code) vs as-built's synthetic exit codes 127/124.
- **Classification:** `equivalent`. Core protocol agrees point-for-point with the spawn-after-grant mechanism. The spawn-failure encoding is spec ambiguity 5/6 territory — "either is acceptable" as long as the run terminally fails, which as-built achieves (`:exit` is applied from `:launching` too, satisfying ":exit as the only observation"). Notes (LOW): synthetic codes 124/127 conflate infra failure with process exit codes and leave no `:failure-reason` for A2 policy; the reconcile loop swallows all Throwables silently (compute.clj:594 — operational blindness); manual path returns the claim-result map rather than nil when it loses a claim race outright (compute.clj:884-887 — spec's nil contract is exercised only via the empty-inbox path, which as-built satisfies); grant poll reads the full row including tails instead of a fields submap (forced by D3b's whole-row schema; plan's R5 submap only pays off under the plan's schema).
- **Consequence:** None contract-breaking. LOW.

## D8. Restart / reconciliation story

- **As-built:** durable `$$compute-pending-by-task` is the restart anchor — a fresh executor instance starts with an empty registry, re-polls its stable task-id inbox, claims with fresh tokens; granted/`:running` runs whose token died with the worker stall non-terminally (no channel can ever be authorized again); orphan OS children cannot write truth. No liveness timestamp exists (no heartbeat support, see D11d).
- **Plan:** identical story, explicitly matched to the spec's A.0/A2 split (PLAN.md "Worker-restart reconciliation"), plus `:last-heartbeat-ms` recorded now to enable A2 stall detection.
- **Classification:** `equivalent`. Same anchor, same stalled-but-safe outcome, same "truth never claims success for unfinished work." The missing liveness timestamp is classified under D11d.
- **Consequence:** None in A.0. LOW.

## D9. Query/read path — foreign selects vs query topologies

- **As-built:** all reads are client `foreign-select` whole-row point lookups (compute.clj:707-728); no query topologies; token secrecy enforced at **write** time (`run-view` whitelist select-keys excludes `:claim-token`, `:obs-buffer`, `:last-seq`, compute.clj:311-317; asserted by every test reading a view).
- **Plan:** R2/R3 are query topologies assembling from `$$runs` (multi-read batching: fields + two subindexed tail scans in one roundtrip); token secrecy enforced at **read** time (assembly never touches the token); R1/R4/R5 are plain foreign selects (PLAN.md "Reads", "Query Topologies").
- **Classification:** `equivalent` — conditional on each design's own schema. Given as-built's single-serialized-row schema, one foreign select returns everything in ~1 seek and a query topology would add nothing; given the plan's subindexed schema, query topologies are mandatory to avoid N client roundtrips. The read-path divergence is entirely derivative of D2/D3b — the schema rows carry the defect, not this row. Both secrecy mechanisms are structural single-point whitelists and both satisfy `(not (contains? view :claim-token))`.
- **Consequence:** None independent of D2/D3b. LOW.

## D10. ID / dedup strategy

- **As-built:** requester mints `:run/id` (compute.clj:81); observation dedup via `:last-seq` watermark + buffer (compute.clj:452-473 — same mechanism as plan's `:next-seq`); claim dedup via the `:pending`-only guard (compute.clj:339-344). **Submit dedup does not exist**: the submit path never reads existing state before writing (compute.clj:622-638).
- **Plan:** `$$decisions[run-id]` **presence is the dedup anchor** — "Present (accepted or rejected) → total no-op: no decision flip, no run-row touch (no `:pending` regression — the double-spawn re-arm path is closed), no inbox re-add"; run-id is single-use, first submit wins (PLAN.md "Topologies and PStates — Submit guard"; "Design Decisions — Ambiguity resolutions (1)(2)").
- **Classification:** `as-built-defect`. The spec makes this a hard requirement, not an ambiguity: O1 concurrency — "A duplicate submit of an existing `:run/id` must not create a second run, must not duplicate the inbox entry, and must never regress an in-flight or terminal run back to `:pending`"; matrix rows `:launching × duplicate submit`, `:succeeded/:failed × duplicate submit` ("run-ids are not reusable"), and "decisions never flip" (O4). As-built violates all of them for any redelivered or reused run-id, independent of D5's infrastructure retries:
  - **Client-retry double spawn:** UI/agent resubmits after an ack timeout (exactly the redelivery the spec anticipates) once the run is in flight or terminal → row reset to `:pending`, inbox re-armed → second grant → second spawn (full trace in D5a — same terminal state, different trigger).
  - **Decision flip:** a reused run-id with a now-invalid payload → `rejected-decision` `termval`s over the recorded accepted decision (compute.clj:627), destroying the audit record.
  - **Two-inbox violation:** a reused run-id with a different `:executor-task-id` hint writes the run into a second inbox while the first inbox may still hold it (spec O7: "the same run must never appear in two inboxes").
- **Severity:** HIGH (it is the client-side half of the double-spawn surface; D5 is the infrastructure-side half; the plan's single `$$decisions`-presence guard closes both).

## D11. Observation fold/guard chain semantics

### D11a. Observations for unknown run-ids create ghost run state

- **As-built:** the obs source has **no absent-row guard**: with `*run-row` = nil, `authorized-observation?` is false (nil ≠ obs run-id, compute.clj:392-396), so `add-observation-error` runs `(update nil :observation-errors ...)` — which **creates a map from nil** — and the topology `termval`s that ghost (`{:observation-errors [...] :updated-at t}`, no `:status`) into both `$$compute-runs` and `$$compute-views` under the unknown run-id (compute.clj:457-458, 382-390, 657-660).
- **Plan:** guard chain step 1: "Run row absent → drop, no state created" (PLAN.md "Observation guard chain").
- **Classification:** `as-built-defect`. Direct spec violation — O3 edge cases: "observation for an unknown `:run/id` (must not create run state)"; matrix `does-not-exist × observation`: "read-run: still absent — observations must not create run state"; O6: "unknown run-id → absent". After one stray observation, `read-run`/`read-view` return a malformed status-less row forever, and (D10 interacting) a later legitimate submit of that run-id proceeds as if fresh — masking that pre-creation occurred.
- **Concrete scenario:** a stale executor from a previous test/worker generation flushes a buffered observation for a run-id that was never submitted in this world → permanent ghost rows in truth and view, indistinguishable from a real-but-broken run to the UI.
- **Severity:** MED (no spawn path exists from a ghost — `grantable-claim?` requires `:pending` — but unauthenticated input materializes permanent state, breaching the back-arrow/spec invariant).

### D11b. Post-terminal authorized observations: recorded-as-error vs ignored

- **As-built:** `authorized-observation?` includes the terminal-status check (compute.clj:396), so any observation against a terminal run — including authorized redeliveries — becomes a `:observation/not-authorized` error entry.
- **Plan:** authorization first, then terminal → authorized redeliveries **ignored silently**, unauthorized → error (PLAN.md guard chain step 3; ambiguity 4 resolution).
- **Classification:** `equivalent`. Spec ambiguity 4 explicitly sanctions either ("ignore vs record-as-error is unspecified; either is acceptable, mutation is not"), and both keep terminal records immutable. Note (LOW): under as-built's `:all-after` retry, a failure on a neighboring record can replay a completed run's whole observation suffix, each replayed obs minting a not-authorized error and evicting genuine audit entries from the 50-slot ring — redelivery noise polluting the auditability surface the errors exist for. The mislabeled reason (`not-authorized` for a correctly-authorized late delivery) also degrades audit quality.
- **Severity:** LOW.

### D11c. Invalid-type check ordered before sequence handling → watermark deadlock (latent)

- **As-built:** `fold-observation` checks type validity **before** the sequence compare and never advances `:last-seq` on error (compute.clj:460-461); `observation-types` is `#{:started :stdout :stderr :exit}` (compute.clj:47-48). An authorized observation with an out-of-set type consumes a sequence number at the writer but never advances the kernel watermark → every subsequent observation buffers forever (into the unbounded D3c buffer) and the run never closes.
- **Plan:** unknown types from an authorized token fold to an observation error **at the apply step, after the sequence is consumed/advanced** — no deadlock (PLAN.md "Depots"; guard chain steps 4-5).
- **Classification:** `as-built-defect` (latent). Unreachable in A.0 (the only authorized writer emits only the four supported types), but the spec requires the named future types (`:heartbeat`, `:artifact-produced`, …) not be precluded; as-built turns "writer adds a type before the kernel set is updated" into a permanent run hang plus unbounded buffer growth, where the plan degrades to an error entry.
- **Severity:** LOW (latent; first A2 writer extension trips it).

### D11d. `:heartbeat` unsupported / no liveness timestamp

- **As-built:** `:heartbeat` is not in `observation-types`; an authorized heartbeat would be recorded as `:observation/type-invalid` (and deadlock the watermark per D11c). No `:last-heartbeat-ms` field exists anywhere in the row (compute.clj:266-291).
- **Plan:** `:heartbeat` applied as "update `:last-heartbeat-ms` only, no status change", explicitly recorded now because A2 stall detection needs it (PLAN.md guard chain step 5; "Worker-restart reconciliation").
- **Classification:** `as-built-defect`. The spec's O3 lifecycle invariants and the matrix row `:running × observation :heartbeat (authorized)` specify the liveness-timestamp behavior; as-built fails that row (error entry, no timestamp). Mitigation: no A.0 writer sends heartbeats, so the row is unreachable today.
- **Severity:** LOW (A.0-unreachable; removes the data A2 stall detection was meant to find).

## D12. Request validation / admission depth

- **As-built:** validation covers envelope completeness, request/action type agreement, run-id presence and payload/routing-key drift, actor type, target kind, **capability authorization** (`authorized-request?` — actor must hold `:compute/run`, compute.clj:198-204, 258-264), argv shape, cwd, branch id — with structured per-error reasons recorded on the rejected decision (compute.clj:129-191, 246-256).
- **Plan:** minimal admission: request/type known, target kind ∈ `#{:workspace}`, run-id non-blank, argv non-empty vector of strings (PLAN.md "Submit guard").
- **Classification:** `as-built-better`. The spec's floor is target-kind checking (O1), which both meet, but the as-built actor-capability gate and drift checks are genuine admission-control value the re-derived plan under-specifies — the spec's own requirement language ("validates the request", actor identity in the envelope) points at more than four checks, and rejected decisions carrying structured `:errors` make rejections actionable. No skill rule is violated; the cost is once-per-submit at human pace.
- **Consequence:** Gained: unauthorized actors cannot mint runs; rejections are diagnosable. LOW.

---

## Summary

| Classification | Count | Rows |
|---|---|---|
| `as-built-defect` | 10 | D2, D3a, D3b, D3c, D3f, D5, D10, D11a, D11c, D11d |
| `equivalent` | 9 | D1, D3d, D3e, D4, D6, D7, D8, D9, D11b |
| `as-built-better` | 1 | D12 |
| `plan-defect` | 0 | — |

### Top-3 highest-consequence divergences

1. **D10 — no submit dedup anchor (HIGH).** Any redelivered or reused run-id `termval`s a fresh `:pending` row over in-flight/terminal truth, re-arms the inbox, and yields a second OS spawn of the same command while destroying the recorded outcome; decisions can also flip. The plan's `$$decisions`-presence guard closes the entire surface.
2. **D5 — stream topology without retry-safe writes (HIGH).** Rama's at-least-once stream semantics (replay after commit; independent cross-partition commit groups) turn the unguarded submit into infra-triggered double spawn (D5a) and the grant/inbox-removal pair into a permanent stale-inbox entry driving an infinite 50ms claim-append loop (D5b). The plan's microbatch + pure-function guards make both impossible by construction.
3. **D3b — output tails as in-row vectors with whole-row read-modify-write (HIGH), ×2 via D2.** Every stdout line on the spec's dominant write path (10³–10⁵ obs/run) deserializes and rewrites the entire run row twice (runs + views) instead of one O(1) subindexed entry write — gigabytes of serialization churn per chatty run and task-wide latency spikes, the exact Option A the plan rejects.
