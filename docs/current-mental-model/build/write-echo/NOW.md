# write-echo — spike result (2026-07-11)

## STANDING — what this is, and whose call the verdict is

- **Measurement spike**, model Opus 4.8, one session. Probe code stays
  **UNCOMMITTED** (evidence, not product — framework probe convention):
  `src/app/probe/write_echo_probe.clj` (untracked).
- **Question:** is DIRECT-to-Rama editor echo (write to Rama, stream back, **no
  optimistic echo**) feasible at typing speed? This decides the `block-write`
  package's route. Sid's target (2026-07-11 vision LOG): *direct unless
  infeasible.*
- **Pre-registered criterion** (drafted by Fable BEFORE any data; **NOT adjusted
  after seeing numbers**): direct-write **STANDS iff p95 ≤ 50 ms AND ≤ 1 stall
  (>100 ms) per sustained minute. FAILS otherwise.**
- **The verdict is NOT this session's to rule.** Numbers here → a Fable session
  drafts the `decisions.md` PROPOSED entry → Sid countersigns. Sid's fingers on
  the real editor remain the final acceptance in both directions.

## RESULT — **FAILS**, decisively (~6× over budget; every event a stall)

Against the **current microbatch text-kernel** (`text_kernel.clj`, the lawful
existing request path), on the **in-process IPC substrate Softland runs today**:

| scenario (60 s) | echo p50 | echo **p95** | echo p99 | echo max | stalls >100 ms | verdict |
|---|---|---|---|---|---|---|
| content 12/s (fast typist) | 216 ms | **307 ms** | 388 ms | 478 ms | **720 / 720** | FAILS |
| content 3/s (relaxed) | 237 ms | **394 ms** | 827 ms | 1007 ms | **180 / 180** | FAILS |
| status 12/s (smallest write) | 228 ms | **313 ms** | 409 ms | 485 ms | **720 / 720** | FAILS |

*echo = leg1 + leg2 = client emit → PState materialized (legs 1+2 only; leg 3
below only adds).* p95 misses the 50 ms budget by ~6×; **every single event**
is a >100 ms stall (criterion allows ≤1/min).

## What the numbers say (mechanism, not just outcome)

Leg decomposition (append-ack vs materialization), content 12/s:

| leg | p50 | p95 | p99 | max |
|---|---|---|---|---|
| leg1 emit → depot **append-ack** | **5.8 ms** | 58.6 ms | 69.6 ms | 167 ms |
| leg2 append-ack → **materialized** | **211 ms** | 295 ms | 376 ms | 475 ms |

1. **leg2 is the entire problem.** The durable depot append (leg1) is cheap
   (~5 ms p50). The ~210 ms lives **between append-ack and PState visibility** —
   i.e. the microbatch materialization.
2. **Payload-independent floor.** Content-write (re-derives all line units every
   event) and status-set (one tiny row) both show leg2 p50 ~210–218 ms. The cost
   is **not** per-event work — it is the **microbatch iteration cadence**.
3. **Load-independent floor, load-sensitive tail.** p50 ~210 ms at *both* 12/s
   and 3/s. But the **tail is worse under lighter load** (3/s: p95 394 ms, max
   **1007 ms**) — sparse events each wait a fuller microbatch cycle. Lower load
   did not help; it hurt the tail.
4. **Universal stall.** 720/720, 180/180, 720/720 events over 100 ms. Cold
   first-keystroke echo: 142–271 ms.

**Root cause is architectural, not a tuning accident.** The text-kernel is a
**microbatch** topology (chosen for exactly-once atomicity across its
multi-PState effect set — `text_kernel.clj:423`). Rama's own guidance
(rama skill, core-concepts.md:14): *microbatch does not participate in depot
ack; use **stream** only when single-digit-ms latency OR write-then-read-back is
needed.* Those are **exactly** the editor's two needs. The measured ~210 ms is
the microbatch cadence on this substrate; the failure follows from the topology
class, not from load or payload.

## Leg 3 (stream-back) — characterized, not measured — and why the verdict holds anyway

Leg 3 = server-side mirror atom (updated **after** materialization completes —
`file_viewer.cljc:326`, gated on distill `:complete`) → `e/watch` → client atom.
Rama's `foreign-proxy-async` is broken in 1.6.0 IPC, so there is **no PState
subscription and no independent poller** — the writer path bumps the mirror,
`e/watch` streams the change (the S40 pattern). Two shapes exist:
- **direct truth-atom** (`WatchSidebarTruth`): pure Electric localhost transport
  — low single-digit ms (prior art 2026-03: localhost network 2–4 ms).
- **epoch + client-debounce + re-pull** (faces artery, `file_viewer.cljc:320-325`):
  adds a debounce + a `FacePull` round-trip — strictly larger.

**Not measured this session** (a full browser E2E was out of scope; result is
**COMPOSED, NOT END-TO-END** — the honest label). It does not need to be:
legs 1+2 **alone** fail the criterion by ~6×, and leg 3 is **sequential and
strictly additive** (it happens *after* materialization). **Even at leg-3 = 0 ms
the criterion still FAILS.** Measuring it could only make the number worse.

## Caret-relevant latency (pre-registered sub-measurement)

Under pure direct-write with no optimistic echo, the caret cannot advance until
the echo returns — so the caret-relevant latency **is** the echo: **~210 ms p50 /
~310 ms p95**. That is ~4–6× beyond perceptible-instant (~16–50 ms); typed
characters would appear a fifth-to-a-third of a second late, one at a time. The
pre-registered fallback (LOCAL CARET AFFORDANCE, text truth still streamed) would
be required for feel even before considering text throughput. (Not ruled here —
flagged for the Fable/Sid decision.)

## Method — exactly what was measured

- **Path:** the existing lawful request path only — no new event types, no kernel
  edits (per fences). `:artifact/ingest` (new revision per event, same artifact —
  the content-write path, **conservative**: it re-derives line units each event)
  as primary; `:unit/status-set` (smallest keyed write) as comparison. Both route
  to one artifact's key, stressing per-key serialization as the load spec demands.
- **Runtime:** `tk/start-text-runtime!` → `create-ipc` (InProcessCluster),
  `{:tasks 4 :threads 2}` — the real launch opts. **This is the substrate
  Softland runs on today** (whole app uses test IPC; `foreign-proxy` broken in
  1.6.0 IPC — `file_viewer.cljc:116`).
- **Load:** open-loop — emit at a fixed cadence (83.3 ms / 333 ms) for 60 s
  **regardless of whether prior events materialized** (this is what "sustained
  12/s" means, and the only way to expose per-key queue backup). Achieved rate
  12.00 / 3.00 /s (exact).
- **Legs:** leg1 = `foreign-append! … :append-ack` call→return (emitter thread,
  `System/nanoTime`). leg2 = append-ack → a tight-polling thread first observes
  the materialization signal (`$$artifact-heads` for content, `$$unit-statuses`
  `:event/id` for status; monotonic in emit order under same-key serialization).
  Poll resolution **0.077 ms/read** (~780 k reads/run) — far finer than the 50 ms
  budget. Batch-jumps (N revisions commit together) correctly share one
  visibility time.
- **Isolation:** each scenario has its OWN artifact + tag-scoped id space.
  *Required*: reused request-ids dedup to no-ops and reused event-ids hit the
  collision guard; a stale head from a prior scenario would have stamped
  **fake-instant** materialization — the flattering artifact this spike most
  needed to avoid. (Caught in smoke-test; fixed before the measured run.)
- **No dropped tail:** events never materialized within grace count as failures,
  not excluded from stats. (0 unmaterialized in every run.)
- Machine: 24 cores; JDK per repo default; raw summary EDN in scratchpad.

## Deviations from the pre-registered method (honest disclosure)

1. **COMPOSED, not END-TO-END** — leg 3 characterized (mechanism + bound), not
   browser-measured. Blessed by the opening prompt as the honest fallback; the
   verdict is robust to it (see leg-3 section).
2. **Substrate = in-process IPC**, not a production Rama cluster. This is what
   Softland actually runs today, so the number is the *current real substrate*,
   not an approximation — but a distributed cluster's microbatch cadence could
   differ (adds network; microbatch remains batch-oriented by design). A
   production-cluster re-measure is the natural follow-up **only if** someone
   wants to rescue microbatch; Rama's own guidance already points elsewhere.
3. **Keystroke proxy** = ingest-new-revision / status-set, not a literal in-place
   single-char edit — the kernel has no such request type and the fences forbid
   adding one. The content path is the *heavier* honest analog (re-derives units),
   so the primary number is conservative, not flattering.

## Hand-off

- **To Fable:** these numbers → draft the `decisions.md` PROPOSED entry for the
  `block-write` route. The measured result says direct-write (no optimistic echo)
  against the **current microbatch topology** is **infeasible** on today's
  substrate by the pre-registered criterion. The architectural alternatives
  (a **stream** topology for the write→read-back path, per Rama's own guidance;
  and/or the pre-registered LOCAL CARET AFFORDANCE fallback) are **not ruled
  here** — that is Fable's draft + Sid's countersign.
- **Reproduce:** `clojure -J-Xss16m -M -m app.probe.write-echo-probe`
  (probe file untracked; ~3.5 min; prints the tables above + writes summary EDN).

═══════════════════════════════════════════════════════════════════════════════

# write-echo-2 — STREAM result (2026-07-12)

Continuation (board thread 10; D-013 ruling-2). SAME pre-registered criterion,
SAME method + isolation, DIFFERENT topology CLASS. Model Opus 4.8, one session.
Probe UNCOMMITTED: `src/app/probe/stream_echo_probe.clj` (self-contained minimal
stream module + the write-echo harness method verbatim; `write_echo_probe.clj`
left untouched so its repro still stands).

## RESULT — **STANDS**, decisively (~6.5× under budget; zero stalls)

Direct write→read-back against a MINIMAL **stream** topology (head/revision +
status writes, per-key serialized on `hash-by :routing/key = [:artifact id]` —
the text-kernel's exact scheme), on the SAME in-process IPC substrate:

| scenario (60 s) | echo p50 | echo **p95** | echo p99 | echo max | stalls >100ms | verdict |
|---|---|---|---|---|---|---|
| content 12/s (fast typist)  | 5.50 | **7.66**  | 10.09 | 13.20 | **0 / 720** | STANDS |
| content 3/s (relaxed)       | 5.14 | **14.36** | 15.01 | 18.63 | **0 / 180** | STANDS |
| status 12/s (smallest write)| 3.74 | **5.84**  | 8.78  | 10.92 | **0 / 720** | STANDS |

*echo = leg1 + leg2 (client emit → PState materialized), ms.* p95 ≤ 50 AND
stalls ≤ 1/min BOTH hold in every scenario, with margin. **0 / 1620** events
stalled; 0 unmaterialized. Cold first-keystroke echo 4.1–7.1 ms. Poll resolution
~0.07 ms/read (~810–865 k reads/run) — far finer than the 50 ms budget.

## The leg that moved — microbatch vs stream, SAME method

| leg (content 12/s, p50) | microbatch (write-echo) | stream (write-echo-2) |
|---|---|---|
| leg1 emit → append-ack (durable)   | 5.8 ms  | 2.3 ms |
| **leg2 append-ack → materialized** | **211 ms** | **3.1 ms** |

**leg2 fell ~70× (211 → 3.1 ms).** leg1 (durable append) was always cheap in
both. The entire write-echo failure lived in leg2, and leg2 is exactly what the
topology CLASS governs: microbatch materializes on a batch clock (~210 ms
cadence, payload/load-independent); stream participates in depot ack and makes
writes visible per event (auto-batches to size ~1 at ≤12/s — stream ref L138).
The ~210 ms was the cadence, proven by its disappearance under an unchanged
measurement method.

**The microbatch tail-inversion is gone.** write-echo's tail got WORSE at lower
load (3/s max 1007 ms — sparse events each waited a fuller batch). Here the 3/s
tail is only mildly higher (max 18.6 ms) and lives in **leg1** (emit/durability
jitter, p95 11.3 ms), not leg2 (materialized p95 3.2 ms). No cadence to wait on.

## Cross-check — the real write-then-read-back call agrees

Closed-loop `:ack` round-trip (n = 300): `foreign-append! … :ack` blocks until
the stream event tree completes AND the write is visible, then read-back — ONE
call, the actual editor pattern. **p50 3.45 / p95 3.90 / p99 5.71 / max 19.62 ms.**
This independently reproduces the polled leg1+leg2 (~single-digit ms) → the
poller is not manufacturing an optimistic number; the leg decomposition is trusted.

## Leg 3 (stream-back) — characterized, not measured — E2E now depends on it

Leg 3 = server mirror atom bumped AFTER materialization → `e/watch` → client atom
(the S40 pattern; `foreign-proxy-async` still broken in 1.6.0 IPC, so no PState
subscription). Prior art (2026-03 localhost Electric transport): **2–4 ms** for
the direct truth-atom shape; the faces epoch+debounce+re-pull shape is strictly
larger. Additive and sequential (fires after materialization). Composed full-E2E
p95 ≈ 7.66 + ~3 ≈ **~11 ms** — and the pass is robust to a GENEROUS leg-3: E2E
stays ≤ 50 ms for any leg-3 up to ~40 ms. **COMPOSED, NOT END-TO-END** (leg-3 not
browser-measured — the same honest label as write-echo). Unlike write-echo (legs
1+2 failed, so leg-3 was moot), here leg-3 is load-bearing for the E2E claim; the
margin absorbs it, but a browser E2E is the clean confirmation.

## Contract input to NAME (not built here) — the price of leaving microbatch

Stream is **at-least-once** with per-event (per-task, between-partitioner)
atomicity ONLY. Microbatch's **cross-PState exactly-once** — the stated reason
the text-kernel is microbatch (`text_kernel.clj:417`) — is LOST. The eventual
**block-write** design therefore owes idempotency at the application layer: a
deterministic **op-id derived from the request-id**, so a retried stream event
**overwrites the same keys (idempotent by value)** instead of duplicating — the
face-arsenal wear-id-journal (G20) and relation-kernel idempotency-key
precedents. NB: THIS probe's topology is deliberately NOT retry-hardened (clean
IPC, no retries in the measured run) — retry-idempotence is the named block-write
obligation, not a probe deliverable.

## Method (deltas from write-echo; the rest carries over verbatim)

- **Module:** own request depot `(hash-by :routing/key)` → ONE `stream-topology`
  → PStates `$$echo-heads` (head pointer), `$$echo-revisions` (the full ~185-char
  block — a realistic-sized write, mirrors `$$text-revisions`), `$$echo-statuses`
  (status row, mirrors `$$unit-statuses`). Content path writes revision+head in
  ONE event (zero partitioner hops → same task → atomic; timing the head times
  the revision). Launch opts `{:tasks 4 :threads 2}` (identical to write-echo).
- **Legs / loads / isolation:** unchanged — `:append-ack` leg1 + tight-poll leg2;
  open-loop 12/s & 3/s for 60 s (emit regardless of materialization); per-scenario
  fresh artifact + tag-scoped ids (the stale-head fake-instant trap; re-verified
  in smoke — a fresh artifact reads index −1). Achieved rate 12.00 / 3.00 exact.
- **Added:** the `:ack` closed-loop cross-check (n = 300).
- **Repro:** `clojure -J-Xss16m -M -m app.probe.stream-echo-probe` (~3.5 min;
  prints the tables + writes summary EDN). Probe UNCOMMITTED.

## Deviations from the pre-registered method (honest disclosure)

1. **Minimal topology omits line-unit re-derivation** (the content path's bulk
   per-event work). Bounded single-digit ms two ways: (a) write-echo's
   payload-independence (microbatch content-leg2 211 ≈ status 218 ms); (b)
   RE-CONFIRMED here — stream content-leg2 (3.1 ms, incl. the revision write) vs
   status-leg2 (1.7 ms) differ by only ~1.4 ms. Per-event application work is
   single-digit ms; it cannot threaten the ~6.5× margin.
2. **Status path skips unit-existence validation** (the kernel reads
   `$$units-by-artifact` first) — one colocated seek (~0.5 ms), within noise.
3. **COMPOSED, not END-TO-END** — leg 3 characterized, not browser-measured (see
   above). Here the margin makes the composition robust.
4. **Substrate = in-process IPC** (today's real substrate). This caveat cuts the
   OPPOSITE way from write-echo's: there, IPC-vs-cluster could only ADD latency
   to an already-failing microbatch (moot); HERE a production cluster adds
   replication network to leg1 (and some to leg2's external visibility) that a
   passing stream must SURVIVE. Stream stays few-ms by design and the margin is
   large, but the decisive pass is on IPC — a clustered re-measure is the clean
   confirmation, flagged not hidden.

## Hand-off

- **The verdict is NOT this session's to rule.** These numbers fill D-013
  ruling-2's evidence slot → a Fable session drafts the `decisions.md` update →
  Sid countersigns. Sid's fingers on the real editor stay final both directions.
- **What the evidence says:** direct write→read-back against a STREAM topology
  PASSES the pre-registered criterion decisively (legs 1+2, the Rama half — the
  dominant unknown D-013 named) on today's substrate. This clears the path D-013
  ruling-4 gated (the block-write contract). NOT ruled here: the transport
  commitment, the named op-id idempotency obligation, and the clustered-substrate
  confirmation — Fable's draft + Sid's countersign.

## NOW (≤15 lines per entry; newest last)

- 2026-07-11 · write-echo (Opus, this session) · **SPIKE DONE — direct-write
  FAILS the pre-registered criterion decisively.** Content 12/s echo p95
  **307 ms** (budget ≤50), 720/720 stalls; 3/s p95 394 ms / max 1007 ms;
  status 12/s p95 313 ms. leg1 (append-ack) ~5 ms; **leg2 (microbatch
  materialization) ~210 ms p50 — payload- and load-independent floor = the
  microbatch iteration cadence.** Legs 1+2 alone fail ~6×; leg3 (Electric
  stream-back, characterized not measured) is strictly additive → verdict robust
  even at leg3=0. Root cause architectural: text-kernel is microbatch (exactly-
  once atomicity); Rama's guidance says use **stream** for single-digit-ms /
  write-then-read-back — the editor's two needs. Substrate = in-process IPC
  (what Softland runs today). Probe UNCOMMITTED. Verdict routing: Fable drafts
  decisions.md PROPOSED → Sid countersigns; not ruled here.
- 2026-07-12 · write-echo-2 (Opus, this session) · **STREAM PASSES the
  pre-registered criterion, decisively.** Content 12/s echo p95 **7.66 ms**
  (budget ≤50), 0/720 stalls; 3/s p95 14.36 ms; status 12/s p95 5.84 ms;
  0/1620 stalled, 0 unmaterialized. **leg2 (materialization) 211 → 3.1 ms vs
  microbatch — SAME method, only the topology class changed** → the ~210 ms was
  the microbatch cadence, now gone (tail-inversion gone too: 3/s tail lives in
  leg1, not leg2). `:ack` round-trip cross-check p95 3.90 ms validates the
  decomposition. leg3 (Electric stream-back) characterized 2–4 ms (localhost
  prior art), additive → composed E2E p95 ~11 ms, robust to leg-3 ≤ ~40 ms;
  COMPOSED not browser-measured. Named contract input (not built): stream drops
  microbatch's cross-PState exactly-once → block-write owes op-id idempotency
  (overwrite-by-value on retry). Substrate = IPC (clustered re-measure the one
  caveat that could erode margin — flagged). Probe UNCOMMITTED
  (`stream_echo_probe.clj`). Verdict routing: Fable drafts decisions.md → Sid
  countersigns; not ruled here.
