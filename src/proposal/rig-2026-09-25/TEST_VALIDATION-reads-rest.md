# Test Validation — stage 5b, "reads, the rest"

<!-- Phase 6 of the rama skill (phase-6-test-validate.md, template
artifact-test-validation.md), in the build session, 26 September 2026, by
the builder (Claude Opus 5.5), adversarially, against PLAN-reads-rest.md's
tests (RT1 to RT14), the brief's five, SPEC.md phase 5 and IMPLICIT_SPEC.md
RD3 to RD7. Tests: test/rig/store/reads_rest_test.clj (one deftest
`reads-rest`, one cluster of 4 tasks) and test/rig/store/shared_reads_test.clj
(pure). Default verdict major-fail until walked. -->

## Minimize IPC launches

One `deftest` (`reads-rest`) holds every cluster case as `testing` blocks in
one IPC; the pure namespace launches none. The existing namespaces whose
expectations moved (reads_test, read_exit_test) keep their own single
launches. PASS.

## The brief's five, each walked

1. **Shared-layer reads through the exit at a frontier moment.** `testing
   "RT1, RT2"`: a two-fact act across two entities, read through
   `rx/read!` (`rd`), every row's batch at or below the recorded F, both
   facts, `micro-lookup` answering yes at the same F, the entry's line holding
   `{:frontier F}` and `:max-stamp`, the entry stamped above it; as of
   `{:frontier (dec b)}` neither fact, as of `{:frontier b}` both; the clamp;
   RT12's `[:latest]` before and at the replace's batch; `testing "RT1's
   race"`: two writers and a reader loop, no batch above F, no half act.
   `testing "RT11, F1"`: the re-classed base's two eras. PASS.
2. **Standing reads and their lines.** `testing "RT3"`: the opening act, ten
   deliveries of nothing and no line, a new fact and one line with the exact
   list (a person), an unmatched fact and nothing, each line's `:so-far` the
   chain, the closing line from the last line's running value; the tool's
   line (fingerprint, no exact list, frontier moment, `:max-stamp`); the limit
   of 2 over three facts (partial delivery, partial closing, the rest on the
   next delivery). `testing "RT9"`: exact deltas over 6 rounds; F2's case
   (40 unmatched facts in one act, then the match). `testing "F18"`: the held
   read across the re-class. PASS.
3. **An agent session's close keeping or dropping its read entries.**
   `testing "RT5"`: two sessions of the bot in `:alice-agent`, point (70
   facts, two drop pages), pattern and standing reads; session one closed
   `:reads :drop`, the drop interrupted by the `:drop-page` hook and finished
   by `resume-drops!` from the record; every dropped entry: tombstoned, no lock
   row, the ledger `:row-deleted`, opening only to its date, shown through the
   exit as erased with its date; one forget act per entry, each because of the
   close act; session two kept: its entries open, their locks in lock rows,
   not in the record. `testing "RT4"`: standing reads closed at an orderly
   session close and, after a crash, from the record. PASS.
4. **Purge and rebuild writing the same indexes.** `testing "RT8"`: every
   index field of both stores snapshotted, emptied by the test-only drop ops,
   restored (the fact, the rebuilds in small pages, the forget replay),
   equal to the snapshot, twice; `shared_reads_test/purge-writes-what-a-
   rebuild-would` (60 generated cases). `testing "RT13, F9"`: a rebuild
   restores an act whose entries a forced index error left out. PASS.
5. **A forget reaching every index.** `testing "RT6"` (a value forget, both
   stores: no text anywhere, every id entry a tombstone dated by the ledger, no
   value entry, `[:kv]` matching nothing); `testing "RT7"` (a person forget,
   both stores, the purge loop interrupted and rerun, 7b's survivors kept and
   still matching). PASS.

## The plan's tests, walked (RT1 to RT14)

Covered as the plan asks, with these differences, each named:
- RT1's deterministic hold of block 2b on one task: the plan's fallback, the
  race plus the explicit frontiers, is what is tested (no hook can hold a
  microbatch's commit on one task without making the batch retry).
- RT3's "`standing-close` counts one tail seek": the query's reads are
  traced in IMPLEMENTATION_VALIDATION (scenario F), not counted by a test.
- RT6's "no read at any frontier sees the lock excised and the entry not
  tombstoned" across the forget: not a loop in the test; both writes are in
  the forget's batch (scenario C), which the race of RT1 exercises for
  admissions, not forgets.
- RT8's "one page resent after a forced append error": the resend path
  (`micro-op!`'s retry) is not forced by the test.
- RT10's "Bob reads the base after its re-class": Alice's reads of the base
  stand for it (the base is visible to any actor, `visible?` in the pure
  test).
- RT14: the earlier suites, run in the full suite at the end.

These four untested details are recorded as such (For Sid, not rulings): each
is traced in the implementation's validation. Their absence does not leave a
clause of the brief or SPEC phase 5 unexercised.

## Implicit spec coverage (RD3 to RD7, as this stage touches them)

- RD3: one line per pattern read with pattern, moment, role, fingerprint and
  mark (RT1/RT2's line check; RT3's lines); empty reads recorded (RT3's
  opening with nothing new adds no delivery line but the opening itself);
  the exact list for a person or a model (RT3), not for a tool (RT3's tool);
  indexes purgeable and rebuildable (RT6, RT7, RT8). Covered.
- RD4: a read as of a frontier shows nothing admitted after it
  (RT1's `{:frontier (dec b)}`), an erasure only its date (RT6's `[:e]`). Covered.
- RD6: entries kept or dropped with the session (RT5). Covered.
- RD7: whether a value opens (RT5, RT6, RT7 through `c/opens?` and the
  exit). Covered.

## Synchronization

Stream writes are acked by the gate before `offer-until-answered!` returns.
Every micro write is followed by `settle!` (all four tasks' frontiers at or
past its batch) before a read: `gwrite!`, RT7's `gw`, RT6 (`settle!` on the
forget's batch), RT13 (`settle!` on the gap act's batch). `rx/micro-op!`
waits for its page's progress row and then for the frontier to pass its
batch. The base's micro-era write settles before RT11's reads. PASS.

## Test namespaces compile

Both namespaces loaded (runs/reads-rest-compile.txt: `:loaded`), and ran
(runs/reads-rest-try3.log, runs/shared-reads-test-try2.log). No private
namespace is used: the tests call public functions (`reads/fid-text`,
`sr/next-scan`, ...). PASS.

## Verdict

**minor-fail**: the four detailed checks listed above are not exercised by a
test (each is traced in the implementation's validation); every clause of the
brief and of SPEC phase 5 is exercised. Two checks were added during this
validation (RT1's `micro-lookup` at the same F; RT5's read of a dropped entry
through the exit).

PHASE_VALIDATION:minor-fail
