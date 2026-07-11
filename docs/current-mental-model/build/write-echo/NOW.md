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
