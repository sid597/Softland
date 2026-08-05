# Test Validation

<!-- Phase 6. Fill in after tests are written in Phase 5. -->

Review the test source files. For each check, state pass or fail with evidence. Then emit one of three verdicts at the end of this artifact, per the rubric below.

**Mode note (retrospective):** the tests under validation
(`test/app/server/rama/dogfood_compute_test.clj`) were written before the skill existed.
This module has no separate protocol file; per the retro adaptation, the "protocol" is the
module's public foreign-facing surface in `src/app/server/rama/dogfood/compute.clj`
(request builders, append helpers, claim/observation constructors, read/await helpers,
`run-one-pending-local!`). The only docstring on that surface is
`run-command-request`: *"Build the ActionRequest entering the compute Rama module."* —
all other contract language comes from `IMPLICIT_SPEC.md`, which was derived from the
same surface plus the dogfood-runtime docs. Validation below is against
`IMPLICIT_SPEC.md` + that public surface. Module internals were NOT validated here
(Phase 4's job); they were read only to resolve synchronization semantics (ack levels,
write ordering within events) and arity/visibility of called functions.

## Minimize IPC launches
<!-- Each create-ipc + launch-module! adds 30+ seconds. Default to one deftest with testing blocks for organization. Justify every additional deftest by naming the specific shared mutable state that would interfere — "different operation" or "different concern" is not a valid justification. Scenarios on disjoint keys do not interfere and belong in one deftest. -->

**FAIL.**

The suite has four deftests, each wrapped in `with-compute-runtime` (lines 10–16), which
calls `compute/start-compute-runtime!` → `create-ipc` + `launch-module!`. That is four
IPC launches where one suffices:

1. `task-global-executor-runs-command-automatically-test` (line 35)
2. `echo-command-runs-through-rama-owned-lifecycle-test` (line 56)
3. `false-command-materializes-failed-status-test` (line 82)
4. `double-claim-and-wrong-token-observation-test` (line 100)

Interference walk-through (could these share one IPC?):

- **Run-ids are disjoint**: `"run_auto_echo"`, `"run_echo"`, `"run_false"`,
  `"run_double_claim"`. All depots hash by `:run/id`; all PStates are keyed by run-id or
  inbox key. Disjoint keys ⇒ no PState interference.
- **Inboxes are disjoint between the automatic and manual paths**: test 1 omits
  `:executor-task-id`, so its run lands in the accepting task's own inbox (a numeric task
  id), which only the module-owned TaskGlobal executor polls. Tests 2–4 pin
  `:executor-task-id compute/pending-task-id` (`"local"`), an inbox the TaskGlobal
  executor never polls (it polls its own task id). So the automatic executor cannot steal
  the manual tests' runs and the manual path (`run-one-pending-local!`, which reads only
  the `"local"` inbox) cannot see the automatic test's run.
- **Sequencing within one deftest is safe**: `run-one-pending-local!` picks
  `(first (sort (keys pending)))` from the `"local"` inbox; executed as sequential
  `testing` blocks, at most one un-claimed run exists in `"local"` at any point
  (`run_echo` and `run_false` are claimed to terminal in their own blocks;
  `run_double_claim` is removed from the inbox at claim grant). No cross-block pickup.
- **Shared mutable state candidates**: the process-wide atom
  `!compute-executor-task-global-states` and `close-compute-runtime!` →
  `close-all-compute-executor-states!` (which closes ALL executor states globally) are
  shared across runtimes — but under clojure.test's sequential deftest execution this is
  not interference between the four scenarios, and it is certainly not a justification
  *for* four launches (if anything it makes parallel deftests unsafe, an argument for one
  deftest).

No specific shared mutable state justifies any of the three extra launches. The four
scenarios belong in one deftest with four `testing` blocks. Cost as written: ~3 × 30+s of
unnecessary IPC startup per suite run. The fix is a suite restructuring (merge four
deftests into one), which per the phase rubric cannot be classified minor.

## Implicit spec coverage
<!-- Read IMPLICIT_SPEC.md and verify every edge case and entity state × write combination is tested. List each one and the test that covers it. If any are missing, add tests. -->

**FAIL.** The four tests cover the happy path, the double-claim race, and one
wrong-token observation. Large portions of the spec's invariants and the entity
state × write matrix have NO test. Walk-through follows: first what IS covered (with
citations), then every missing case.

### Covered (cited)

| Spec item | Test evidence |
|---|---|
| O1 valid submit → decision `:accepted`, echoes `:request/type :compute/run-command` | `echo-command-runs-through-rama-owned-lifecycle-test`, lines 71–72 |
| O1 valid submit → run `:pending` before claim | helper `append-run-command-and-await-pending!` line 29; asserted line 73 (`run-before`) |
| O1 valid submit → pending inbox contains run-id | helper lines 30–32; `(contains? pending-before run-id)` line 73 |
| O1 "decision latency independent of command duration" (weak: only via 5 s end-to-end bound) | all tests via `await-view ... 5000` |
| O2 single-grant: first claim moves `:pending → :launching`, records winner's `:claimed-by`/`:claim-token` | `double-claim-and-wrong-token-observation-test` lines 118–122 |
| O2 grant removes run from pending inbox | line 123 `(not (contains? (compute/read-pending runtime) run-id))` |
| O2 competing claim is a strict no-op; winner's identity/token never overwritten | lines 126–129 |
| O2 loser classifies `:conflict-or-past` from the run record | line 130 (`compute/claim-state row-b claim-b`) |
| O8 loser cannot spawn — `run-one-pending-local!` returns `nil` | lines 131–134 |
| O3 unauthorized (wrong-token) observation: status unchanged, `:stdout-tail` empty, `:observation-errors` gains `{:reason :observation/not-authorized}` in run truth AND view | lines 136–147 (matrix row `:launching × observation (wrong token)`) |
| O3 lifecycle effects `:started`/`:stdout`/`:exit` (indirect, end-to-end only) | tests 1–3: terminal view with `:exit-code` and stdout line (`"task-global"` line 51, `"hello"` line 79) |
| O4 read-decision content (`:decision/status`, `:request/type`) | lines 71–72, 93 |
| O4 decision reflects request outcome, not process outcome (`false` run stays `:accepted`) | `false-command-materializes-failed-status-test` lines 93, 97 |
| O5 run record exposes `:claimed-by`, `:claim-token`, `:executor/task-id` (string), `:status`, `:stdout-tail`, `:observation-errors` | lines 52–53 (test 1), 119–124, 140–145 (test 4) |
| O5 3-state grant check `granted-to-us` | line 124 (`:granted-to-us` from `claim-state row-a claim-a`); also `(:claim-state result)` lines 74, 94 |
| O6 token secrecy `(not (contains? view :claim-token))` | test 1 line 54, test 2 line 80 — **omitted in tests 3 and 4; see gap (m)** |
| O6 terminal view readable with status/exit-code/tails | tests 1–3 |
| O7 read-pending membership by inbox key, `contains?` semantics | helper lines 30–32; lines 73, 123 |
| O8 automatic module-owned execution: submit-and-only-wait reaches `:succeeded` ≤ 5 s, claimed by `compute-executor-*` | `task-global-executor-runs-command-automatically-test`, lines 39–54 |
| O8 manual single-step contract `{:claim-state :granted-to-us, :spawned? true, :spawned-after-grant? true}` / `nil` when nothing runnable | lines 74–76, 94–96, 131–134 |
| O9 start/close per test; repeated start/close cycles | `with-compute-runtime` fixture, used 4× |

Correction to the spec's own evidence note: IMPLICIT_SPEC O3 edge list calls
"unauthorized observation while run is `:pending`" an *exact test scenario*. It is not.
In test 4 the wrong-token observation is appended at line 136 AFTER claim-a was granted
(line 118), so the run is `:launching`, matching the matrix row
`:launching × observation (wrong token)` (which the spec also marks `[exact test
scenario]` — that one is correct). The `:pending × observation` case is uncovered; see
gap (g).

Weak assertion noted (not counted as a failure on its own):
`(is (:spawned-after-grant? result))` (lines 76, 96) asserts the manual path's documented
return value, but that flag is computed by the executor from the same run-row that gated
the spawn — it is not an independent witness of spawn-after-grant ordering and cannot be
false on this code path when `:spawned?` is true. It exercises the documented contract
shape, not the ordering property itself.

### Missing — spec cases with NO test

Each item names the spec section / matrix row and states that no deftest exercises it.

- **(a) Rejected request — the entire rejection path is untested.** Spec O1 invariants
  ("A rejected request must never become spawnable: no pending-inbox entry, no run that
  an executor could claim"; "Request validation must include target-kind checking"),
  matrix `absent × submit (invalid request)` (read-decision rejected; read-run never
  `:pending`; read-view never looks pending; read-pending must NOT contain run-id), and
  O1 edge cases: empty argv, missing/blank `:run/id`, bad target kind, unauthorized
  actor. No test ever submits an invalid request. Zero coverage of one of the two
  decision outcomes.
- **(b) Duplicate submit (same run-id) — untested in every state.** Matrix rows
  `accepted × duplicate submit`, `:pending × duplicate submit`,
  `:launching × duplicate submit` ("must NOT regress to :pending — regression would
  re-arm the spawn path and risk double spawn"), `:succeeded/:failed × duplicate submit`,
  and inbox row `contains run-id × duplicate submit` (present exactly once). The spec
  names lifecycle regression as a double-spawn safety risk; no test redelivers a submit.
- **(c) Observation ordering/buffering — untested.** Matrix rows
  `:launching × observation :stdout/:stderr (authorized, out-of-sequence)` (buffered,
  never dropped, not visible until gap fills) and
  `:launching × observation :exit (authorized, with earlier seqs buffered)` (buffer
  drained in sequence order BEFORE terminal status). No test appends observations out of
  order; the only manually-appended observation is a single unauthorized one. The O3
  ordering invariant — a core contract property — has zero coverage.
- **(d) Duplicate/redelivered sequence — untested.** O3 retry safety: "redelivery of an
  already-applied sequence number must not double-apply (e.g. the same stdout line must
  not appear twice in the tail)". Matrix `:running × observation :stdout (authorized,
  in-order)` "exactly once even under redelivery". No test redelivers an observation.
- **(e) Terminal immutability — untested.** Matrix rows `:succeeded/:failed × observation
  (authorized late/duplicate)` (no reopen, no double-applied output, exit-code unchanged)
  and `:succeeded/:failed × claim` (terminal record untouched). No test writes anything
  after a run reaches terminal state.
- **(f) Writes against unknown run-ids — untested.** Matrix rows
  `does-not-exist × claim` (claims must not create run state) and
  `does-not-exist × observation` (observations must not create run state). No test
  appends a claim or observation for a run-id that has no run row.
- **(g) Unauthorized observation while `:pending` — untested.** Matrix row
  `:pending × observation (any token)`: not-authorized error recorded, run remains
  claimable, pending inbox still contains run-id. Test 4's unauthorized observation
  arrives at `:launching`, after a grant exists; the no-grant-yet case is distinct
  (authorization must fail against a nil token) and uncovered.
- **(h) Redelivery of the winning claim — untested.** O2: "redelivery of the winning
  claim must not change the outcome" and edge "claim redelivered after grant (no-op,
  idempotent)". Test 4 sends a *different* claimant's claim, never the winner's claim
  twice.
- **(i) Same token, different executor id — untested.** O2 edge: "claim with the same
  token as the winner but different executor id (must not be treated as granted — grant
  check is identity AND token)". `claim-state`'s AND-check is never exercised on this
  half: test 4's claim-b differs in both id and token.
- **(j) Bounded tails / bounded observation-errors — untested.** O3 throughput/data-growth
  invariants ("readable surfaces must stay bounded no matter how much a process prints";
  "the observation-errors list ... must be bounded"; O6 "Bounded size regardless of
  process output volume"). No test emits more output lines than the tail limit or more
  than a couple of observation errors.
- **(k) `:heartbeat` — untested.** Matrix row `:running × observation :heartbeat
  (authorized)`: liveness timestamp updated, status unchanged. Never appended.
- **(l) Spawn failure — untested.** O8 edge: "spawn failure (binary missing, cwd
  invalid) — the run must still reach a terminal `:failed`-shaped outcome, not hang in
  `:launching`" and O1 edge "argv referencing a nonexistent binary (acceptance is still
  valid — failure surfaces later as a failed run)". All tested argvs (`echo`, `false`)
  spawn successfully.
- **(m) Token secrecy not asserted on every view read.** Spec O6: "asserted in every test
  that reads a view: `(not (contains? view :claim-token))`". Test 4 reads `view-c`
  (line 141) of a `:launching` run — a state where a token *does* exist on the run row —
  and never asserts its absence (lines 141–147). Test 3 also reads a view (line 92) and
  asserts nothing about the token (its assertions at lines 93–98 cover only decision,
  claim-state, spawned flags, status, exit-code). Two of the four view-reading tests omit
  the secrecy assertion, contradicting the spec's stated evidence.
- **(n) `:exit` as the only observation — untested.** O3 edge: "process died before
  `:started` was reported — run must still close terminally".
- **(o) Unknown-id reads — untested.** O4/O5/O6 edge "unknown run-id → absent" and O7
  "inbox key that has never existed → empty". Trivial but specified; no test reads a
  nonexistent id or inbox.
- **(p) Concurrent/multiple in-flight runs — untested.** O8: "multiple in-flight runs
  must execute independently"; O7 "the same run must never appear in two inboxes". All
  tests run one run at a time.
- **(q) Duplicate sequence numbers with different payloads — untested.** O3 edge: "must
  keep the first, never corrupt".

Items the spec itself flags as Open Ambiguities (sequence gap at exit, post-terminal
ignore-vs-error choice, manual-path `:timeout-ms` expiry semantics, rejected-request
run-row representation, run-id reuse semantics beyond the hard requirements,
observations-for-unknown-runs auditability) are not counted as coverage failures *as
ambiguities* — but the hard requirements attached to them (never `:pending` for rejected,
never two runs / never regression for duplicates, never create run state for unknown
ids, terminal immutability) ARE requirements and are counted above in (a), (b), (e), (f).

Per the template instruction "If any are missing, add tests": retro mode records the
gaps instead of fixing; the list above is the required-additions list. Items (a)–(f) are
the highest-severity gaps — (a) is an entire untested decision branch and (b)–(f) are
exactly the retry/duplicate-delivery safety properties the spec centers on.

## Synchronization
<!-- Every write that precedes a read must be followed by `(harness/wait-for-processing! client)` before the read. Verify no write-then-read sequences skip the wait. -->

**PASS.** This harness has no `wait-for-processing!`; the contract's synchronization
mechanisms are (1) polling waiters (`await-decision` / `await-run` / `await-view` /
`await-materialized`, all bounded polls over foreign reads) and (2) full-ack appends
(`:ack`), which for stream topologies block until all colocated downstream processing —
including post-partitioner writes — completes. Walk-through of every write→read sequence:

1. **Helper `append-run-command-and-await-pending!` (lines 18–33):**
   `append-run-command!` defaults to `:append-ack` (durability only, no processing wait)
   → followed by `await-decision` (poll), `await-run` for `:pending` (poll), and
   `await-materialized` on the pending inbox (poll, covers the write that lands after the
   `|hash` repartition). No unsynchronized read. PASS.
2. **Test 1 (lines 39–54):** append (`:append-ack`) → `await-decision` → `await-view`
   terminal (5000 ms) → `read-run`. The terminal run-row write and view write occur in
   the same event on the same task (run row written before view), so a terminal view
   implies the run row already carries terminal status and grant fields when `read-run`
   executes after the await. PASS.
3. **Test 2 (lines 60–80):** `read-decision` / `read-run` / `read-pending` all execute
   after the helper's three awaits. `run-one-pending-local!` internally appends the claim
   and polls `await-claim-resolution` before spawning; its observation appends
   (`:append-ack`) are followed by the test's `await-view` terminal poll before any
   assertion on view contents. PASS.
4. **Test 3 (lines 86–98):** identical shape to test 2. PASS.
5. **Test 4 (lines 104–147):** `append-claim! ... :ack` (line 118) → immediate
   `read-run` / `read-pending` (lines 119–123): full ack guarantees the grant write AND
   the pending-inbox removal (which sits after `(|hash *executor-task-id)`) are complete
   before the append returns. Same for the second claim (line 126 → reads 127–130) and
   the observation append (lines 136–139 → reads 140–147, `:ack` covering the
   `{:retry-mode :all-after}` observation source's writes). PASS.

No write-then-read sequence skips synchronization.

## Test namespaces compile
<!-- Every test namespace must load cleanly. Verify imports, `:refer` entries on record constructors (e.g. `->FooRecord`), and that no test depends on a private namespace. -->

**PASS** (static verification; phase rule forbids running anything).

- Requires: `app.server.rama.dogfood.compute :as compute`, `clojure.string :as str`,
  `clojure.test :refer [deftest is testing]` — all resolvable; `str/starts-with?` exists.
- Every `compute/*` var referenced exists, is public, and is called at a defined arity:
  `start-compute-runtime!` (0), `close-compute-runtime!` (1), `pending-task-id` (def),
  `run-command-request` (argv + opts), `append-run-command!` (2),
  `await-decision` (2), `await-run` (3), `await-materialized` (2), `await-view` (4),
  `read-pending` (1 and 2), `read-decision` (2), `read-run` (2), `read-view` (2),
  `run-one-pending-local!` (2), `claim-record` (run-id + executor-id + opts),
  `append-claim!` (3, ack-level `:ack` is a valid Rama ack level),
  `claim-state` (2), `observation` (5), `append-observation!` (3).
- No record constructors are used, so no `:refer [->Foo]` entries are needed.
- The test depends only on the module's public namespace
  `app.server.rama.dogfood.compute` (the protocol surface per the retro adaptation), not
  on any private/internal namespace.

## Verdict

**major-fail** — the implicit-spec coverage check fails on entire untested contract
regions (the rejection path (a), duplicate-submit/regression safety (b), observation
ordering/redelivery/terminal-immutability (c)–(e), unknown-id writes (f)), whose fix
requires many new testing blocks, and the IPC check fails in a way that requires
restructuring the suite (merging four deftests into one) — both individually exceed the
minor-fail bar of "fixable by editing specific lines."

- **pass**: every check above passed. — Not met: two of four checks failed.
- **minor-fail**: at least one check failed, but every failure is fixable by editing specific lines in existing test namespaces. — Not met: coverage gaps need substantial new test scenarios; IPC fix is a restructure.
- **major-fail**: at least one failure requires more significant changes or restructuring. — Met (twice over).

Self-consistency check performed: the coverage section explicitly lists missing cases
(a)–(q); per phase rules those force a FAIL on that check, and the verdict reflects it.
No check marked PASS contains an acknowledged gap (the `:spawned-after-grant?` and
token-secrecy notes are recorded under the failing coverage check, not under a passing
one).
