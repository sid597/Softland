# Fix Plan — Compute Track

> **Process note.** A fix session resumes the standard `/rama` skill process at **Phase 3
> (implement)** with the artifacts already in place: `IMPLICIT_SPEC.md` (Phase 0),
> `PLAN.md` v2 + `PLAN_VALIDATION.md` round-2 PASS (Phases 1–2), and this fix plan.
> The validated plan is the blueprint; `FINDINGS.md` carries the verified defect traces.
> After implementing, the session MUST re-run **Phase 4** (implementation validation)
> and **Phase 6** (test validation) from scratch — the existing
> `IMPLEMENTATION_VALIDATION.md` / `TEST_VALIDATION.md` record the pre-fix major-fails
> and stay as retro history.
>
> **Plan-vs-as-built preference rule used below:** where the as-built design conflicts
> with the validated plan, prefer the plan (it was adversarially validated, round 2 PASS,
> and the as-built alternative is in most cases the exact option the plan enumerated and
> rejected). The named exceptions where as-built wins are called out explicitly
> (validation depth D12; executor identity; provenance-rich decisions).
>
> **Deferred-scope guardrail (FINDINGS reconciliation #1):** stall detection,
> restart-reconcile, cancel, and lost-run recovery stay OUT of scope. No batch may
> preclude them: keep the durable inbox as the restart anchor, record
> `:last-heartbeat-ms`, keep the status keyword set open (`:cancelled` reserved).
>
> Batches 1, 2, and 4 are one coherent "implement the validated plan" move over the same
> topology; they are sequenced for reviewability (correctness invariants first), but a
> fix session rewriting the topology should expect to land them as one structural change
> set with three review gates, since Batch 1–2's guards are cleanest written against
> Batch 4's schema.

---

## Batch 1 — Correctness core: microbatch conversion + submit/claim atomicity + dedup anchor

**Findings addressed:** C-01, C-02, C-14.

**Change sketch** (implements the plan's choice; as-built stream design conflicts —
prefer the plan, whose decisive argument is atomicity, with the latency budget shown to
fit the 5 s bound with margin):

- Convert `compute-run-command-topology` from `stream-topology` to **microbatch**, per
  PLAN.md §Topologies and PStates (four-question check; items 3a–3c). This makes the
  submit pair (decision+run row on hash(run-id); inbox entry on hash(inbox-key)) and the
  grant pair (status CAS + inbox `disj`) exactly-once-atomic — C-02's permanent stale
  inbox and infinite claim-append loop become impossible by construction, and
  infrastructure replay of the submit record can no longer fire C-01's trigger B.
- Add the **submit dedup guard** per PLAN.md §Submit guard: process only if
  `$$compute-decisions-by-run-id[run-id]` is **absent**; present (accepted or rejected)
  → total no-op — no decision flip, no run-row touch, no inbox re-add. This closes
  C-01's trigger A (client redelivery), which microbatch alone does not cover.
  Run-id is single-use; the decision row is the durable dedup anchor (plan ambiguity
  resolutions 1/2).
- **Blank/missing run-id** (C-14): refuse client-side in `run-command-request` /
  `append-run-command!` AND add a topology-side guard that drops the record before any
  keyed write (plan: "refused client-side by the submit helper and dropped without state
  by the topology"). No decision row under nil/"" keys.
- Keep the claim branch's `:pending` CAS guard as-is (verified correct); keep the
  rejected-equals-absence representation (no run row, no inbox entry for rejections).
- **Keep as-built validation depth** (capability gate, drift checks, structured
  `:errors` on rejections — R3 D12 `as-built-better`); the plan's four-check minimal
  guard under-specifies. Keep the provenance-rich embedded-event decision record
  (R3 D3e equivalent) — Batch 4's admission caps bound its size.

**Verification (IPC scenarios to add — land with Batch 6's restructure):**
1. Duplicate submit redelivered while run is `:pending`, `:launching`, `:running`, and
   terminal → decision unchanged (including `:decided-at`), run row unchanged (no
   `:pending` regression), inbox entry present at most once, no second spawn (terminal
   stdout tail unchanged after a settle window).
2. Reused run-id with a *different* `:executor-task-id` hint → no second inbox entry
   anywhere; reused run-id with a now-invalid payload → decision still the original
   `:accepted`.
3. Blank/`nil` run-id submit appended raw to the depot (bypassing the helper) → no
   decision row, no run row, no view row; topology keeps processing subsequent records
   (settles DIRECT_REVIEW open doubt 1 about nil keys).
4. Claim for a granted run redelivered → grant fields byte-identical; pending inbox
   stays empty (the grant+removal pair commits atomically).

---

## Batch 2 — Correctness: observation guard chain (authorization, ghosts, ordering, audit)

**Findings addressed:** C-03, C-05, C-06 (cap half), C-07, C-08, C-12.

**Change sketch** (implements PLAN.md §Observation guard chain steps 1–5 verbatim; the
as-built `fold-observation` order — auth-incl-terminal → type → seq — conflicts; prefer
the plan's order, which was scenario-validated in PLAN_VALIDATION O3.a–O3.i):

1. **Absent row → drop, no state created** (closes C-05's ghost rows; ambiguity 8's A.0
   arm: not audited, documented).
2. **Authorization = grant exists AND token matches**: `:claim-token` must be **present
   (non-nil)** on the row AND equal to the observation's token. Closes C-03's
   `(= nil nil)` bypass. Failure (including any token while `:pending`) → append a
   token-free error record `{:reason :observation/not-authorized …}` to the capped ring
   + increment the total counter; nothing else mutates.
3. **Terminal status** → authorized observations ignored **silently** (no error entry);
   unauthorized → error per rule 2. Closes C-12: replays of a completed run's suffix
   (client-level redelivery or `:all-after`) no longer mint false audit entries.
   (Note: under Batch 1's microbatch conversion, topology-level replay disappears
   entirely; this guard still matters for client-level redelivery.)
4. **Sequence**: `seq < next-seq` → ignore (first-wins, applied path). `seq > next-seq`
   → **store-if-absent** into the buffer (navigate `(keypath seq)`, write only when nil
   — closes C-07), with **cap 1024**; overflow → `:observation/buffer-overflow` error +
   reject (closes C-06's unbounded growth; the cap is a never-hit-in-A.0 safety valve).
   `seq = next-seq` → apply, advance watermark, drain buffer while contiguous (delete
   drained entries; `loop<-` with `yield-if-overtime` once on the microbatch shape).
5. **Apply by type, open set**: `:started` → `:running`+pid; `:stdout`/`:stderr` →
   tail write; `:heartbeat` → `:last-heartbeat-ms` only (new field — arms A2 stall
   detection, deferred-scope guardrail); `:exit` → drain-then-terminal from `:running`
   OR `:launching`; `:failed` → terminal `:failed` + `:failure-reason` (new type —
   spawn-failure/timeout encoding per plan ambiguity resolutions 5/6). **Unknown types
   from an authorized writer fold to an observation error AT the apply step, after the
   sequence is consumed/advanced** — closes C-08's watermark wedge: an unrecognized
   type can never block the stream behind it.

**Verification (IPC scenarios):**
1. Tokenless observation against a `:pending` run → `:observation/not-authorized` error
   in run AND view, status `:pending`, inbox still contains the run (exact spec matrix
   row), and the run is subsequently claimable and runs normally.
2. Observation for a never-submitted run-id → `read-run`/`read-view` stay absent.
3. Out-of-order: append seq 2 (`:exit 0`), seq 1 (`:stdout`), seq 0 (`:started`) →
   final state `:succeeded` with the stdout line in the tail (drain order); duplicate of
   buffered seq 2 with a different payload while the gap is open → first payload wins.
4. Authorized `:heartbeat` mid-stream → timestamp updated, status unchanged, subsequent
   observations still apply (no wedge); authorized unknown type → error entry +
   subsequent observations still apply.
5. Post-terminal authorized redelivery of an applied `:stdout` and a second `:exit`
   with a different code → zero new error entries, tail/exit-code/status unchanged.

---

## Batch 3 — Robustness: executor observation pipeline + process lifecycle

**Findings addressed:** C-09, C-13, C-20.

**Change sketch** (patches the as-built executor in place; the plan specifies the
mechanisms at PLAN.md §Writes [acked-cursor send discipline], §Design Decisions —
Executor threading [pump threads, close semantics], §State primitive selection):

- **Acked-cursor sends** (C-09a): allocate a sequence number and consider it "sent" only
  once `foreign-append!` returns; on append failure, retry the SAME sequence (bounded
  backoff) instead of letting the pump thread die with a hole. No seq is ever consumed
  by a failed append.
- **Live streaming** (C-09b): replace `stream-lines` (`doall` to EOF) with an
  incremental read-line→append loop inside the pump threads, so output is observable
  while the process runs and executor memory stays O(1) per stream. (This is also where
  Batch 4's 4 KiB line truncation lands — same loop.)
- **Close semantics** (C-13): track live `Process` handles in the executor registry;
  `close()` destroys them (`destroyForcibly`) after stopping the scheduler; pump-thread
  `.join` gets a timeout everywhere (grandchild-held pipes can't block worker threads);
  the interrupted-`waitFor` path must NOT append a fabricated `:exit 127` for a process
  that is still alive — on deliberate close, kill first, then report truthfully (or
  leave non-terminal: the sanctioned stall arm), per O9 + "truth never lies".
- **Registry-before-append** (C-20): put the `:awaiting-grant` registry entry BEFORE
  appending the claim; remove it if the append definitively fails. Shrinks the
  lost-token window to the durable-append boundary.

**Verification (IPC scenarios):**
1. Live-logs: run `sh -c "echo first; sleep 2; echo second"` → `read-view` shows
   `"first"` in the stdout tail BEFORE the run reaches terminal state.
2. Close-while-running: submit `sleep 30`, close the runtime mid-run → the OS process
   is dead (no leak), no fabricated `:succeeded`, and any recorded terminal state is
   truthful (`:failed` with kill reason) or the run is left non-terminal.
3. Grandchild pipe-holder: `sh -c "(sleep 30 &); echo done"` with a short timeout →
   run still closes terminally within the timeout bound (join-timeout works) and the
   worker thread is released.

---

## Batch 4 — Perf/scale: plan schemas, query-topology views, admission + byte caps

**Findings addressed:** C-04, C-06 (subindex half), C-10, C-11, C-15, C-16.

**Change sketch** (implements the plan wholesale — the as-built whole-row design is the
plan's explicitly rejected Option A; prefer the plan at every point of conflict):

- **`$$runs` schema → PLAN.md §PState Design Option B**: `fixed-keys-schema` typed row;
  tails as `(map-schema Long String {:subindex? true})` keyed by dense per-stream line
  index with O(1) entry-write + trim-delete at `idx - cap` (no read needed); `:buffered`
  subindexed cap 1024 with `IObservation` + per-variant defrecords; `ObservationError`
  record; errors ring 100 + `:observation-error-count` total; `:next-seq` watermark;
  `:last-heartbeat-ms`; `:failure-reason`. No `Object` anywhere (C-15).
- **Drop `$$compute-views`; add `run-record`/`run-view` query topologies** per PLAN.md
  §Query Topologies (variable 1-vs-3 meaningful reads, `<<if` branching on nil-row/zero
  counts, leading `(|hash *run-id)`, `|origin`). Token secrecy moves from write-time
  whitelist to read-time structural omission (assembly never touches `:claim-token` /
  `:next-seq` / `:buffered`) — equivalent guarantee, half the write cost. Note: if a
  reactive UI subscription need lands before this batch, the plan itself sanctions
  re-adding a materialized view **additively** later; do not keep the as-built
  every-event full-copy either way.
- **Pending inbox → `{String (set-schema String {:subindex? true})}`** (C-16); executor
  discovery reads the set (the 5-field entry maps are consumed nowhere).
- **Grant poll → submap reads** per PLAN.md R5/R5b: poll only
  `:status :claimed-by :claim-token :executor/task-id`; fetch `:spec` once per granted
  run after classification. (Pays off only with this batch's schema — under the old
  whole-row blob it bought nothing, per R3 D9.)
- **Admission size caps + env allow-list** (C-10) per PLAN v2 amendment F1: argv ≤1024
  elems / ≤128 KiB, cwd ≤4 KiB, env ≤128 entries / ≤32 KiB; over-limit →
  `:rejected :request/spec-too-large`. Add the `:env` request field and apply it as the
  child-process environment allow-list (clear-then-set on `ProcessBuilder`), closing the
  full-env inheritance hole.
- **4 KiB per-line truncation + marker** (C-11) per PLAN v2 amendment F3: primary at
  both executor paths' pump loops (bounds the depot record itself), defensive at kernel
  ingest before buffering/applying.
- Drop the redundant `(|hash *run-id)` hops if the microbatch block structure allows
  (C-21 overlap; cosmetic).

**Verification (IPC scenarios):**
1. Chatty run (`seq 1 1000` via shell) → stdout tail is exactly the last 200 lines in
   order; view bounded; run record well-formed in every lifecycle state.
2. Single huge line (`head -c 100000 /dev/zero | tr '\0' 'x'`) → stored entry ≤4 KiB
   with truncation marker in both run record and view.
3. Over-cap argv (2,000 elements) → `:rejected :request/spec-too-large`, no run row, no
   inbox entry, view absent; a 300-arg command still accepted.
4. Env allow-list: submit with `:env {"FOO" "bar"}` running `sh -c 'echo $FOO $HOME'` →
   output shows `bar` and an empty `$HOME` (environment not inherited).
5. Token secrecy re-asserted on every view read in every state (now structural in the
   query topology) — including `:launching`, where a token exists on the row.

---

## Batch 5 — Hygiene/operational

**Findings addressed:** C-17, C-18, C-19, C-21.

**Change sketch** (patches as-built in place; no plan conflict — the plan's
§State primitive selection already describes per-instance lifecycle):

- **Per-instance executor state** (C-17): hold registry/scheduler/workers as instance
  state of `ComputeExecutorTaskGlobal` (internal per-instance atom or mutable field),
  not a JVM-global `defonce` atom keyed by `identityHashCode`. `close-compute-runtime!`
  must not reach into other runtimes; rely on module shutdown invoking each TaskGlobal's
  `close()`. Keep the **stable executor identity** (as-built, R3 D6 equivalent — the
  fresh per-claim UUID token already disambiguates restarts; the plan's per-launch nonce
  is optional belt-and-braces).
- **Observability** (C-18): log reconcile-loop exceptions (rate-limited), never
  bare-swallow `Throwable`.
- **Concurrency cap** (C-19): bounded worker pool (or semaphore) for simultaneous OS
  processes per task; pending runs beyond the cap simply wait in the durable inbox —
  no protocol change needed.
- **Mechanical** (C-21): fuse consecutive `keypath`s; use built-in `foreign-select-one`;
  manual path returns `nil` on a lost claim (spec contract); errors ring 100 + total
  counter; spawn-failure/timeout reporting switches to the `:failed` observation +
  `:failure-reason` introduced in Batch 2 (retire synthetic 124/127).

**Verification:** existing suite green; one scenario with two runtimes in one JVM where
closing the first leaves the second's automatic executor functional; a burst of N>cap
pending runs all reach terminal state without exceeding the cap (observable via
staggered `:started` timestamps or pid count).

---

## Batch 6 — Tests (Phase 5 work, validated by re-run Phase 6)

**Findings addressed:** T-01–T-07 (and pins the fixes from Batches 1–5).

**Change sketch:**

- **Restructure to ONE `deftest`** with `testing` blocks (T-01): run-ids and inboxes are
  already disjoint by construction; sequential blocks on one IPC. Budget: one
  `create-ipc`+`launch-module!` per suite run.
- Add blocks (each maps to a finding/spec row; all asserts include the token-secrecy
  check on every view read, T-07m):
  - Rejection path (T-02): bad target kind, empty argv, unauthorized actor, blank
    run-id, over-cap spec → rejected decision with reason/errors; run/view absent;
    inbox empty; subsequent claim for the rejected id is a no-op.
  - Duplicate submit in every state (T-03): Batch 1 verification scenarios 1–2.
  - Ordering/redelivery/dup-payload (T-04): Batch 2 verification scenario 3 + watermark
    redelivery of an applied seq (tail unchanged).
  - Terminal immutability (T-05): Batch 2 verification scenario 5 + claim against a
    terminal run (grant fields untouched).
  - Unknown-id writes/reads + `:pending × observation` + claim edges (T-06): Batch 2
    scenarios 1–2; winner-claim redelivery; same-token-different-executor-id claim →
    `:conflict-or-past` (the AND half of the grant check); reads of unknown ids/inboxes
    → absent/empty.
  - Boundedness/heartbeat/spawn-failure/exit-only/concurrency (T-07): Batch 4 scenarios
    1–3; Batch 2 scenario 4; nonexistent binary → terminal `:failed` with
    `:failure-reason :spawn-failed`; `:exit` as the only observation closes the run from
    `:launching`; two concurrent runs (one automatic, one manual inbox) complete
    independently.
- Keep the four existing scenarios as blocks (they pin what held up: happy paths,
  double-claim race, wrong-token observation).

**Verification:** the suite itself; then re-run Phase 6 (`phase-6-test-validate.md`
verbatim) — it must PASS against `IMPLICIT_SPEC.md`'s entity-state × write matrix.
