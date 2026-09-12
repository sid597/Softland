# P4 gate — circulation (Gate 2) — PASS

2026-07-25 · Fable orchestration session. Gate object: the INTEGRATED branch
(`1cb02fc`, rebased onto P3's `13f1dac` per the deferral ruling), landed on
the main line as `2642ad6` (clean cherry-pick — parent already on main).

## Receipts (this session)

- Worktree full suite: **46 ns / 397 t / 5,416 a / 0 / 0 — fully green**
  (matches Codex's integration receipt; env.clj copied to the worktree
  unread for the run — gitignored there too).
- Cherry-pick to main + HEAD-reading suites green post-merge (16t/365a).
- **Backup fence: `backups/20260725-clean-predeploy-p4` — this cluster's
  FIRST clean cold backup** (`CLUSTER-SHUTDOWN-COMPLETE` observed; no
  unwedge needed — the depot-flush hang is INTERMITTENT, not absolute: a
  data point for the open platform probe). Verified byte-complete:
  apparent size 2.9G and 7,287 files identical to source (the 11G/1.6G
  delta is sparse preallocation). Ledger updated.
- Deploy: single-module `--action update` of relation-kernel from the
  module-jar (built `clj -X:build module-jar`; note `clj -T:build uberjar`
  trips the registered pre-broken :prod client build — module-jar is the
  deploy artifact, and it excludes env.clj by construction). Module
  returned RUNNING; five workers stable.
- **Kinds live-proven**: `:instance-of` and `:felt-at` each
  assert→`:asserted`→retract→`:retracted` on the real cluster (probe actor
  `fable:p4-gate-probe`, retracted trace retained).
- Starter-culture migration ran live: `:completed`, 27-block window,
  truncated? false, **0 matched** — the #TASK/#Feedback corpus is NOT in
  the main conversation's river window. Finding, non-blocking: locate the
  corpus conversations and re-run (idempotent, one line each).

## Falsification pass — held (recorded in-session pre-merge)

- Receipts structurally co-presence-only (identities/placement/material
  refs; no aboutness fields; no material bytes copied).
- Kernel growth = exactly the two reviewed lines (byte-hash verified
  pre/post rebase; Sid's one-line review still open).
- Silver visibly machine (`≈ machine guess ·`), gold a target-side
  PROJECTION of the durable wish (`⌁ wish ·`) — records are marks,
  projected, never held. Epistemic strata never fuse with relation kinds
  (a human `:felt-at` is still not a wish — in code).
- Causal as-of: single-tip parent-chain reconstruction; ambiguity surfaced,
  never invented; clock regressions explicit. Terminal-escape detector
  armed and honest (`:ambiguous-activation-history` over P3's deterministic
  time-ms history — P6's contract must mandate honest event times).
- One batched relation roundtrip; query plan exposed; gauges count relation
  identity once. materialized-to? asserted at every write (A-F4).
- OC/RK actor-type asymmetry honored (`:agent` custody, `:llm` epistemic).
- The ambient autotag lane fires asynchronously on ordinary gold-less
  Ctrl+Enter — which made the kinds deploy load-bearing BEFORE next live
  app use (delivered above). The lane is best-effort off the response
  thread; failures log, never block the turn.

## Gate-2 closure

The live drill (Codex, on the real cluster): banked pressure entered during
ordinary work → receipt automatic → queryable path (ONE batched roundtrip,
composition {receipt 2, silver 0, gold 1}) → deterministic return to the
exact wish origin → worn material resolved causally. Terminal-escape
detector armed. **Gate 2 (circulation) CLOSED.** first-light P3 fulfilled
under its CONTRACT terms (gold path live-proven; flag A honored).

## Deferred honestly (non-blocking, owed at natural touchpoints)

- Live model turn for silver (canned stream-JSON seam proven through the
  real in-memory llm-module; first organic gold-less utterance in live use
  is the real proof — watch the ambient-autotag log line).
- Headed visual of the ⌁/≈ marks (Sid's next headed session; the scene
  fixtures are suite-proven).
- Starter culture over the actual #TASK corpus (locate its conversations).
- In-process LLM runtime inside the app JVM (deliberate ephemeral-organ
  design; first live invocation is the seam to watch for worker-registry
  noise).

## Campaign state

P1–P4 CLOSED, Gates 1+2 closed. P5 green-lit (running/sendable, Opus 5).
Next after P5's report: FRESH Fable session — gate P5 + author
CONTRACT_P6.md (owner condensation, honest event times, the carried
doubts). Worktree `Softland-p4` retired (branch refs preserved; cleanup =
`git worktree remove` at any calm moment).
