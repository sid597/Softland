# multi-cascade R1 · Fable gate review — 2026-07-26

**VERDICT: PASS.** P1 implemented exactly within the ruled cut; all eight
gates independently re-verified this session; falsification pass found no
blocking defect. Code remains uncommitted — the commit decision is Sid's.

## Independent re-runs (never the implementer's word)

- **Focused gate suite** (`app.cascade-table-test`): 1 test / 49
  assertions / 0 failures / 0 errors — byte-matches the P1 receipt.
- **Full suite** (`clj -X:test full`, this session, final tree): 52
  namespaces / 464 tests / 6,441 assertions / 0 / 0; six shards green;
  flake registry unchanged. All three registered flakes passed on
  attempt 1 this run (the P1 run's `dogfood-llm-test` retry was the
  registered stale-approval flake behaving as registered).
- **Lint** (clj-kondo, both new files): 0 errors / 0 warnings.
- **G7 re-check:** working tree = exactly the four allowed code/test
  paths + NOW.md; `material_circulation.clj` at
  `cb8f399b82b726e932dcb5da5a6aefc3a05a69e1`, `verb_registry.cljc` at
  `3e90f5481b77fa64b8c1c30ca667eef2818cbf04` (pre-package hashes); all
  five fenced rung-3 files + their tests untouched; no depot/PState/
  topology/durable-event-kind edit anywhere in the diff;
  `test_runner.clj` diff = exactly the one ruled `isolation-exceptions`
  entry. Old `[CIRCULATION][AUTOTAG-FAILED]` tag has zero remaining
  references; `react!` has exactly one production call site.

## G6 re-drive (Fable, this session)

Environment attestation first (scene-substrate rule): WebGPU adapter =
google/swiftshader (fallback) in headless Chromium (playwright 1.61.1)
— all G6 receipts are stream/server/durable behavior, no pixel claims.
Proof server: `:8081`, working tree, `LAND_PINNED=1`, cluster boot
(shared durable dev cluster); pinned `:8080` app untouched throughout.

Two real Ctrl+Enter drives on the ground (type-without-click → Ctrl+Enter):

- **Drive 1** (durable-receipt drive): first SSE chunk (durable turn
  ack) at **562.8 ms**; resident streaming from ~1.3 s. Autotag row
  `llm-run-material-autotag:2952b094…` landed durably for MY typed
  block (`ep:b653377e:000000`), `:completed` / `:succeeded`, 1
  candidate, recorded ~16.4 s after the turn — after the driving
  browser had already closed (asynchrony proven durably).
- **Drive 2** (INFO logging enabled — the dev stdout swallows INFO by
  default: slf4j-api 2.0.17 routes to log4j-slf4j2-impl and log4j-core
  has no config, so the root logger is ERROR; booted with
  `-Dorg.apache.logging.log4j.level=INFO`): request in at 23:50:17.731;
  `[CASCADE] {:cascade/id :cascade/material-autotag, :cascade/trigger
  :episode/turn-durable}` at **23:50:18.278 on the Jetty request
  thread** (after durable acceptance; client first chunk 550.3 ms);
  `[EPISODE][TURN-START]` at 23:50:18.279 — **1 ms after dispatch**;
  LLM runtime boot on the send-off pool INSIDE the handler's future
  (T2 shape); `[CIRCULATION][AUTOTAG] {…:candidates 4, :status
  :completed, :run-id llm-run-material-autotag:0a105323…, :relation-id
  nil}` at 23:50:34.913 (+16.6 s) with the exact pre-cut field set.
- **Accidental A/B on durable truth:** Sid was live on the pinned
  `:8080` app (old inline code) during the re-drive; his turn's autotag
  row (`415fb546…`, `ep:54ec72c7`) and the through-table rows share
  identical durable shape and vocabulary — old path and new path
  corroborated side by side on the same cluster.
- Side effects disclosed: two gate turns + their autotag rows landed in
  the genesis conversation (visible on Sid's canvas: "gate re-drive…",
  "fable gate second drive…"); proof servers stopped; `:8080` verified
  serving after cleanup.

## Trap spot-checks (diff level)

- **T1** — handler passes the exact `autotag-material!` argument shapes;
  `:lines` rides `cond->` into the PRE-EXISTING canned-stream seam
  (material_circulation.clj:572, read-only, byte-untouched); G1
  byte-compares run-id / durable projection record-id / relation-id
  across two independent clusters.
- **T2** — cascade.clj top level is `ns`/`def`/`defn` only; rows carry
  SYMBOLS; the llm-runtime `delay` stays private in server_jetty and is
  deref'd inside the handler (live-proven: LLM boot on the future's
  thread, drive 2).
- **T3** — `react!` is filter → mapv → `future` per row; no executor,
  no queue, no retry.
- **T4** — per-row `future` + `try/catch Throwable` +
  `[CASCADE][FAILED]` with row id + trigger; G4 proves the thrower
  cannot starve a blocked sibling and receipts hold declaration order.
- **T5** — triggers are in-process keywords; no durable vocabulary
  minted (G7 diff scan).
- **T6** — emission is unconditional at the call site; the handler head
  carries the literal old guard `(and (nil? gold-receipt) rk-rt)`;
  suite drives gold-present, `gold-receipt = false`, and `rk-rt = false`
  (Clojure literal-truth semantics preserved).
- **T7** — `(cascade/rows)` public; G5 asserts completeness, uniqueness,
  vocabulary membership (against `verb-registry/effect-classes`), actor
  = `circulation/autotag-actor-id`, and printable-EDN round-trip.

## Falsification pass — findings

No blocking findings. Open doubts, non-blocking, cheap falsifiers named:

1. **Dispatch receipts are optimistic.** `{:dispatched? true}` is minted
   before the future body runs; a handler whose namespace fails to load
   still receipts true, and the truth lives only in `[CASCADE][FAILED]`
   logs. Contract-conformant (receipts mean dispatched, not succeeded).
   Falsifier: R2's durable runner converts dispatch into durable
   records; until then the log line is the only witness.
2. **The `:lines` passthrough rides the production payload shape.** The
   call site never sends `:lines`, but a future emission-site edit could
   silently switch circulation to canned mode in production. Falsifier:
   when the second emission site appears (R2), add a one-line scan/guard
   that production payloads never carry `:lines`.
3. **Dev stdout hides INFO logs** (environment, not package): the
   log4j2 provider wins over the logback 1.2 binding and defaults to
   ERROR, so `[CASCADE]`/`[CIRCULATION][AUTOTAG]` are invisible in a
   default dev boot. Ops nit for the runbook; the P1 artifact's log
   claims were re-proven here under `-Dorg.apache.logging.log4j.level=
   INFO`.

## D-006-style evaluation notes

- Every P1 receipt reproduced exactly on independent re-run (suite
  counts, hashes, log ordering, durable row identity) — the layered QC
  held; the P1 fresh finder's three corrections (literal guard,
  physical record-id assertion, null-equality hole) were verified as
  present in the final test text.
- The behavior-identity cut (guard moved INTO the handler, emission
  unconditional) is the contract's own T6 ruling executed faithfully —
  the one true behavior delta (a `future` now spawns even on decline)
  is contract-prescribed and observable only as one pool task.
- Gate G8's "through the full suite" letter is satisfied by the
  production table shipping exactly row #1 (asserted in G5) plus the
  fixture dark row proven inert within its test scope; the full-suite
  sweep runs the same production table everywhere else by construction.

Next: commit decision (Sid) — code and docs in separate commits — then
close + retro per the work-package skill.
