# Build notes — read exit (phase 5a build session)

Running log for the rama skill's `build` phase of the one-owner read exit
(PLAN-read-exit.md, validated minor-fail, fixes F1 to F12 in place). A fresh
session picks up from "Next". Newest entries at the bottom of "Log".

Worktree `/mnt/data/projects/Softland-rig-build-reads`, branch
`rig-build-reads`, off `rig-2026-09-25` at `b31ad57e`. Every cluster run
under `flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock`.

## State

- Step: done. The suite is green (last run 03:38 IST: 37 tests, 1,340
  assertions, 0 failures, 0 errors, 32 s; phase 1's gate suite on 8 tasks,
  the rest on 4). runs/phase5-read-exit-suite.txt, full output in the .log
  beside it.

## Next

- Nothing in this build. For builder A and the merge: phase 2's `open-row>`
  replaces the seam's body; phase 2's person forget calls `purge-writes`
  per value; after phase 2 a put page's 4,096-row cap assumes cheap opens,
  and a page holds its task for its length (no yields, by design).

## Brief overrides of the plan (the orchestrator's, binding)

- No `src/rig/store/locks.clj`: the open step is one seam in
  `rig.store.reads` (`open-row>`), phase 2's planned signature and return
  shapes, passing values through tonight. The merge replaces its body with a
  call to phase 2's `rig.store.locks/open-row>`.
- `reads/purge-writes` is built to be called by phase 2's forget, once per
  value, for a value forget and for every value a person forget kills.
- Entries go through `client/offer-until-answered!` as it is.
- Shared files (module.clj, gate.clj, client.clj, envelope.clj) changed only
  as [F10] names.

## Log
- 02:45 IST. Probe of the plan's [build checks], one cluster run
  (runs/phase5-read-build-probe.txt, 14 assertions, 0 failures): a runtime
  field in keypath, an index into a subindexed vector in a query, query
  topologies, a depot and a second `<<sources` call from functions in
  another namespace, and the page walk all work. An exception in a query
  topology is fatal to the worker (Rama logs "Will be treated as a fatal",
  the watchdog shuts the task system down; the client gets
  QueryTopologyInvokeFailed), so every query step stays total.
- Implemented `rig.store.reads` (pure functions, the `open-row>` seam, the
  `*index-ops` depot and source, the two queries) and `rig.store.read-exit`
  (`connect`, `read!`, `index-op!`, `rebuild!`); module.clj and gate.clj
  changed only as F10 names. Compiles; clj-kondo clean apart from dataflow
  syntax it reads as plain Clojure inside the two install `defn`s.
- Found while writing, fixed in the module (each a divergence, listed in
  IMPLEMENTATION_VALIDATION-read-exit.md "Plan conformance"):
  - `:read-fp/1` (FR12) is not EDN: a keyword's name cannot start with a
    digit, so every pattern line would be refused `:malformed` on its face.
    `:read-fp/v1` in its place (first-record).
  - A point read of zero fact ids cannot be "recorded as an entry with no
    rows": an act with no facts is refused `:empty-act` on its face. Refused
    `:bad-read` before anything is read.
  - Rebuild pages must not yield. A page reads the log, computes entries,
    then writes; a forget landing in a yield between would have a purged
    value written back (by a put page, a sweep's rewrite, or the plan's own
    `:ix-of` rewrite). No `yield-if-overtime`, no `:allow-yield?` in the
    `*index-ops` source; a page's size bounds how long it holds the task.
  - A sweep rewrites an entry at an address the log implies but with other
    content, where the plan deletes it: a delete there leaves a hole that
    nothing refills (the put pass is over), against the plan's own end state
    "exactly what the log implies".
  - The fact id's part of an address is the name's canonical text, U+0000,
    and the index as 8 hex digits: the canonical text of the whole id sorts
    index 10 before 9, which would order an act's facts out of act order and
    break `[:latest]`'s tie with the model's chain-head.
- Exploration run (scratch, not kept): point, pattern, as-of, tail, value
  index, visibility, refusals, the exit's entries, the no-copy line read from
  its row, purge, drop and the paged rebuild all behave on the first run.
- Implementation validation (IMPLEMENTATION_VALIDATION-read-exit.md):
  minor-fail, four findings fixed in place: a subvec could reach the
  `:answers` navigator from a put page's cursor (V1); the `[:kv]` open loop
  had no yield point (V2); the ops source read the clock for ops that do not
  use it (V3); `moment-stamp` renamed to the plan's `moment` (V4). Thirteen
  divergences from the plan listed with their reasons (D1 to D13).
- Tests written (phase-5-tests.md), loaded, not yet run: `rig.store.reads-test`
  (pure: parsers, addresses and order, index/purge/rebuild writes, the page
  loop driven over a sorted map as the dataflow drives it, fingerprint, entry
  facts, entry moments in the gate's stamp rule, T14 property tests),
  `rig.store.read-exit-test` (one 4-task cluster: T2 to T6, T8 to T16, T18,
  T19, T11's crashes last, and the global "every shown read has its entry"
  check), `rig.store.read-model-test` (T1: the model op by op in lockstep
  with the rig on simulated time, per-layer comparison of `[:all]`, the five
  patterns and point reads at every model read, every answer; the plan's
  single-moment mapping reported, not asserted; scenarios.clj's fixed
  histories listed with what each needs, checked).
- While writing: the queries now refuse a layer that is not a readable
  keyword before any read (`reads/check-layer`); `$$layers` is keyed by
  Keyword and a read with another class is not probed, and a query throw
  restarts the worker. `bounds` and `check-layer` made total.
- Test validation (TEST_VALIDATION-read-exit.md): minor-fail, nine missing
  cases added (M1 to M9: RD2's never-admitted and past-the-act fact ids, RD4
  as of 0, the line's role, RD6 and E8 after a forget, a purge during a
  paged read, a point read of an erased fact, `:exit-shown`, D5 and D7 in
  the cluster). Stand-ins named: T10's resend, T12's comparison, T18's homes,
  T1's per-layer moment.
- Runs, narrowest first. `rig.store.reads-test`: 2 failures on the first run,
  fixed: my test wrapped the fact-id list in an extra vector (test fix);
  the totality property found `entry-facts` destructuring its spec in the
  parameter list, outside its `try` (module fix; the exit's `check-call`
  likewise). Then 16 tests, 222 assertions, green. `rig.store.read-exit-test`:
  green on its first run, 1 test, 294 assertions (T11's crashes included;
  47 reads returned rows, each with its entry :yes). `rig.store.read-model-test`:
  green, 39 assertions: five histories, 25 model reads compared per layer,
  73 answers, 0 differences. The plan's single moment (the chosen fact's rig
  stamp for every layer) differs in 5 places, each traced to the model's
  per-partition clocks (its seed gives `:alice-hand`'s partition stamps 1 to
  5 before any history op); reported in the receipt, not asserted.
- The whole suite twice, green both times: 37 tests, 1,340 assertions
  (phase 1's 19 tests and 785 checks unchanged, plus this stage's 18 and
  555), 0 failures, 0 errors, 34 s and 32 s, phase 1's gate suite on 2 and
  then 8 tasks. The last run is runs/phase5-read-exit-suite.txt.
