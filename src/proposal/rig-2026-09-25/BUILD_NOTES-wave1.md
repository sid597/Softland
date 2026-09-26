# Build notes — wave 1, phases 2 and 3 merged onto the read exit (26 September)

The rama skill's build step (phase-build.md: implement, validate the
implementation, write tests, validate the tests, run to green) for the merge
of phase 2 (locks and forgetting, `rig-build-locks` at `fd41f6d2`) and phase
3 (the micro store, `rig-build-micro` at `e60c8ee1`) onto `rig-2026-09-25`
at `703b8e26` (phase 1, the hybrid clock, the revision reader, the one-owner
read exit), and the wiring of the seams RIG.md's "Overnight state" item 1
and the builds' receipts name. Worktree `/mnt/data/projects/Softland-rig-wave1`,
branch `rig-wave1`. Claude Opus 5.5, from 04:30 IST. Every cluster run
under `flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock`.

The artifacts: `IMPLEMENTATION_VALIDATION-wave1.md` (minor-fail, six
findings fixed), `TEST_VALIDATION-wave1.md` (minor-fail, two missing cases
added), the new namespace `rig.store.wave1-test`, the bench
`test/rig/bench/put_page_bench.clj`, and this file.

## Log

- 04:30 to 04:40 Loaded the rama skill (phases, phase-build, phase 3 to 7),
  SPEC.md, RIG.md's overnight sections, both builds' receipts and the read
  exit's, the rulings' "Now" on main (the nine, the sharpenings, the rig
  constraints), and phase 2's `locks.clj`, the read exit's `reads.clj`,
  `micro.clj` and `micro_client.clj` in full.
- 04:40 Merged `rig-build-micro` (conflicts in `gate.clj`'s and `module.clj`'s
  requires and tails, both kept; `f29f792d`), then `rig-build-locks` at
  `fd41f6d2`, its final head (conflicts in `gate.clj`'s `decide*` and
  `module.clj`'s event and schema, combined; `d83e5ff8`). The merge would not
  compile until the micro store's `$$persons` placeholder was off: two
  declarations of one PState do not launch.
- 04:41 to 04:48 Baseline suite on the bare merge (`runs/wave1-baseline.log`,
  git-ignored): 91 tests, 5,036 passed, 34 failures, 3 errors. Phase 2's
  namespaces and the micro prepare test green but for `:members`; the read
  exit's failing (its open step passing sealed values through as nil);
  the micro cluster test stopped at the placeholder's person acts.
- 04:50 to 05:12 The seams (below), in four commits, each with its tests
  adapted: `0b69bd23` (persons, `:members`), `ee239a04` (an agent's lease),
  `843dd1ae` (the open step and the purges), `57c46159` (the micro value
  forget). Then, from the validation and the runs: `b481c41d` (`:members`
  refused at the stream gate), `b56cb2de` (one read of each person per
  read), `aa33d905` (micro_test's two expectations), `f8bd4050` (the put
  page's cap), `493313e7` (the seam tests), `fd45cc5b` (the validations).
- Runs: the read tests (`runs/wave1-readexit-try2.log`, 13 tests, 580
  assertions, green after two fixes the first try found: T10's sweep
  rewriting every sealed entry, W-V1; the agents' reads, W-V2; the model
  comparison counting the door's lease acts); the seam tests
  (`runs/wave1-seams-try1.log`: 2 failures, both test bugs, fixed); the whole
  suite (`runs/wave1-suite-try1.log`, 05:13 to 05:20: 93 tests, 5,535
  assertions, 2 failures, both in micro_test's expectations, W-V6).
- 05:15 to 05:30 Implementation validation (W-V3 to W-V5 found and fixed:
  `:members` at the stream gate, the per-row person reads, the micro
  forget's gather carrying rows to the leader) and test validation (T-W1,
  T-W2 added).
- 05:20 to 05:28 The one full run at the end: green (below). 05:27 to 05:30
  the put page bench at 4,096 and at 2,048 rows, and the two namespaces the
  last fixes touched, green.

## The seams, wired

Line numbers are the files' at this branch's head.

| # | Seam (RIG.md item 1 and the receipts) | Wired | Where | Tested |
|---|---|---|---|---|
| 1 | The read exit's open step to phase 2's `open-row>` `[layer fid row stamp T]`, three return shapes | yes | `reads.clj` 410 to 440 (`open-row>`, `open-row-with>`); `locks.clj` 1312 to 1349 (`open-row-with>`, `open-row>`); every read loop carries a person map: `reads.clj` 1092, 1124, 1216, 1272, 1298, 1302 | wave1_test "the exit opens sealed values ..."; read_exit_test T2 to T19 over sealed values; reads_test `open-shapes`; read_model_test (0 differences but the named) |
| 2 | A value forget to `reads/purge-writes`, in the forget's own event | yes | `module.clj` 206 to 211 (after `locks/write-decision>`, in the decision's group); `reads.clj` 276 to 340 (`purge-fid`, `purge>`) | wave1_test "a value forget purges ...", "the purge is the forget's own event" (crash); read_exit_test T19; forget_test's purge recorder |
| 3 | A person forget to the purge, through the enumeration of the values dying with the person | yes | `locks.clj` 1253 to 1310 (`dying>` emits `[layer erased date]`, `fan-out>` emits them); `module.clj` 138 to 139 (record path), 219 to 220 (fresh) | wave1_test "a person forget purges every value ...", "two forgets of one value, in both orders ..."; forget_test's A cases |
| 4 | `reads/index-writes`' plaintext argument from the gate's opened values | yes | `gate.clj` 247 to 255 (`plain-texts`), 360, 373 | reads_test `gate-integration`, `generated-offers` |
| 5 | The exit's read entries lease and seal at the door | yes | `read_exit.clj` unchanged (entries go through `client/offer-until-answered!`, which seals); `gate.clj` 237 to 272 (`with-opened`, `stamp-for`'s moment from the opened value); `locks.clj` 576 to 606 (`lease-under`: an agent's lease under its person's lock) | wave1_test "the exit's read entries lease and seal ...", "an agent's reads are recorded ...", `a-sealed-read-entry-is-stamped-after-its-moment`; read_exit_test T13, T15, T18 |
| 6 | Phase 2's `read-as-of` internal; the exit the one way to read | yes, by contract | `locks.clj` 1369 to 1380, 1421 to 1430; `client.clj` 391 to 400 | contract only (see "Divergences", 8) |
| 7 | The put page's 4,096-row cap re-measured once opens read lock rows | measured; the cap halved to 2,048 (W1-12) | `test/rig/bench/put_page_bench.clj`; `reads.clj` 889 to 896; `runs/wave1-put-page-bench.txt` | see "The put page's cap"; read_exit_test T10 and T12 (the rebuild) rerun green at the new cap |
| 8 | `:members` in phase 2's control keys | yes | `envelope.clj` 43 to 52; `gate.clj` 219 to 235, 357 (refused at the stream gate) | micro_prepare_test "members only in the making act"; wave1_test ":members ..." (both gates) |
| 9 | R19's permission walk wins in `gate.clj` over phase 2's four clauses | yes (the merge) | `gate.clj` 142 to 211 (`pids-to-read`, `refusal`) | micro_test's permission cases on both gates |
| 10 | `persons-placeholder?` off; `make-person!`/`forget-person!` phase 2's acts | yes, the placeholder removed | `micro.clj` (`declare!` has no placeholder); `micro_client.clj` 147 to 162 | micro_test (seed, leases, A cases); wave1_test "persons are phase 2's acts ..." |
| 11 | The lock effect of a value forget in a shared layer | yes | `micro.clj` 169 (`:entities`), 628, 730 to 830 (`forget-target-of` to `forget-effect`), 885 to 990 (`decide-envelope`), 1360 to 1402 (block 1's target gather), 1455 to 1480 (block 2a's writes); `micro_client.clj` 362 to 387 | wave1_test's four group forget blocks |
| 12 | The micro fold's `gate/refusal` and `gate/stamp-for` against phase 2's `decide`: shared layers get phase 2's sealing and opening (ruling 7, default 6) | yes, reconciled | traced in IMPLEMENTATION_VALIDATION-wave1.md, plan conformance item 8 | micro_test's six group A cases against the model; forget_test's base cases; wave1_test |

## Divergences from the plans and the builders' notes, with why

1. **The purge is `reads/purge>`, wired in `module.clj`, not a body inside
   phase 2's `purge-read-indexes>`** (removed). `rig.store.reads` requires
   `rig.store.locks` for the open step, so phase 2's ops cannot call back
   into the read exit: a cycle does not compile. `write-decision>` leaves
   `:purge` to the module; `fan-out>` emits the dying values. Same writes,
   same events.
2. **A person forget's purge is dated by the day the value's wrap closed**
   (`wrap-closed` over the entries on the task), which is the date the open
   step and a rebuild give; phase 2's seam passed the person's own date,
   which would move the date of a value an earlier forget had closed. A
   value with a ledger entry is skipped (its value forget purged it).
3. **The open step carries a person map through each read** (W-V4): one
   read, one read of each person. The contract's three shapes are the same;
   `open-row>` `[layer fid row stamp T]` stays for single rows.
4. **An agent's lease is sealed under its layer's person owner** (W-V2): a
   change to phase 2's lease road, following default 1's text ("wrapped
   under the session owner's person lock"). Without it no agent could
   record a read.
5. **`:members` is refused at the stream gate** (W-V3), beside joining the
   control keys as the micro build asked.
6. **The `$$persons` placeholder is removed, not set false** (the brief said
   "set to false"): turned on it would declare `$$persons` twice and the
   module would not launch, so it had no state to keep but off.
7. **The micro value forget names its target's entity in the forget fact's
   `:e`** (as the model's forget does), and **an act whose one lock spans
   entities keeps them on its name row** (`$$micro-names [name :entities]`,
   a field the micro plan's schema lacks), so a per-act forget reaches
   every entity. The micro answer carries no `:how`; the ledger says it.
8. **`read-as-of` is internal by contract, not by construction**: the rig has
   no caller identity, so any client can still invoke the query; every
   docstring says it records nothing and is never a reader's path. Kept
   because phase 4's and phase 8's plans use `c/read-as-of` and `c/opens?`
   as the store's own view. `:by-stamp` (one write per admitted act) stays
   for wave 2 to decide against its `:ix-s` (PLAN-reads-rest.md).
9. **A sweep compares entries by content** (W-V1), where the read exit
   compared with `=`: phase 2's rows carry byte arrays.
10. **Tests adapted, each change marked where it is**: read_exit_test T3
    (`:no-such-lock` where the lease into a layer never made is refused, For
    Sid 14) and T19 (a real forget in place of the removed test double, and
    a planted entry for F5 through the new `:copy?` of the test-only `:put`
    op); read_model_test (phase 2's seed with persons; the door's lease acts
    left out of the comparison, For Sid 16; Bob's offers answered
    `:no-such-lock` where the model records a permission reason, and reuses
    of such names not sent, For Sid 14; a reuse of a recorded refusal
    answered as recorded, For Sid 2: each counted in an OBSERVED line, every
    other answer and read compared exactly); reads_test (sealed offers and
    lock contexts; the pass-through's `open-row` deftest checks phase 2's
    pure half instead, its test-double assertions gone with the double);
    micro_test (phase 2's person acts; a row compared by its bytes; the
    chain in another layer asserted on the lease act, For Sid 14).

## The put page's cap, re-measured

The read exit built its rebuild's put page to stop once its acts hold 4,096
rows, when an open read nothing; a page is one event that never yields, so
its length is how long it holds its layer's task, and the read exit's plan
estimated 0.1 to 0.3 s a put page. With phase 2's open step every sealed row
now costs a ledger read, a lock row read (personal and hand layers), the
wrap's person entries (once per page since W-V4), an unwrap and an open.

Method: `clojure -M:bench rig.bench.put-page-bench run 512 10 7` under the
cluster lock: one in-process cluster (4 tasks, 2 threads, 1 worker) on this
machine (24 cores, Linux, Java 21.0.12.1); 10 acts of 512 facts into each
of three layers of Alice's through the door; then the first put page of
each layer sent 7 times (a page is idempotent), timed at the client from
append to ack. Rows are "about" the cap: a page takes whole acts, and the
seed's and the door's lease acts (one or two rows each) sort among the
bench's. The in-process cluster is no proxy for production latency: only
the order of magnitude and the ratio between layers mean anything.

| layer | what the open does | rows | median ms | max ms | µs per row |
|---|---|---|---|---|---|
| `:bench-retracts` (personal, retracts) | nothing (no seal) | about 4,096 | 164.8 | 215.8 | 40.2 |
| `:alice` (personal, sealed notes) | ledger, lock row, persons, unwrap, open | about 4,096 | 467.3 | 531.8 | 114.1 |
| `:alice-agent` (agent, sealed notes) | ledger, record lock, persons, unwrap, open | about 4,096 | 445.4 | 505.2 | 108.8 |

(`runs/wave1-put-page-bench.txt`, run 1, 05:27:47 to 05:28:13 IST.) A
4,096-row page of sealed values holds its task about 0.45 to 0.53 s, 2.8
times the page of rows that open nothing and over the plan's 0.1 to 0.3 s.
The cap is now 2,048 rows (`reads.clj` 889 to 896, W1-12). Run 2, the same
method at the new cap (05:29:18 IST start):

| layer | rows | median ms | max ms | µs per row |
|---|---|---|---|---|
| `:bench-retracts` | about 2,048 | 95.8 | 122.7 | 46.8 |
| `:alice` (lock rows) | about 2,048 | 233.9 | 263.6 | 114.2 |
| `:alice-agent` (record locks) | about 2,048 | 231.0 | 287.9 | 112.8 |

About 0.23 s a page of sealed values, inside the plan's bound, and far under
the stream topology's 5 s. The cost a row does not change with the cap
(about 114 µs sealed, 40 to 47 µs with nothing to open), so the cap is the
lever. Two limits stay: an act larger than the cap goes whole into
one page (the check comes before each act), so one act of 10,000 values
makes a page of about 1.1 s; and the sweep page (512 entries, each with a
record read, a row read and an open) was estimated at 0.5 to 0.8 s before
sealing and was not re-measured tonight.

## For RIG.md

### New rig choices (the orchestrator assigns R numbers)

Each can change later without touching a record's form unless it says so.

- **W1-1. An agent's or a tool's lease is sealed under its layer's person
  owner.** A writer who is a person seals under their own lock (L23); the
  root actor's rows are bare; a writer with no person entry (an agent, a
  tool) seals under the layer's person owner, default 1's "session owner",
  so the owner's forget reaches the agent's unconsumed rows; with no person
  owner (the base, a group) the writer is refused `:no-such-person` as
  before. Why: the read exit records an agent's reads in its person's
  session layer (default 4), and a sealed entry needs a lease there.
  `locks.clj` 591 to 606. The micro gate's lease road is unchanged (wave 2).
- **W1-2. A forget's purge is wired in the module.** `reads/purge>` runs in
  the value forget's decision group right after the lock writes, and in the
  person forget's fan-out child once per value that died on that task. Why:
  the read exit requires phase 2's namespace for its open step, so phase 2's
  ops cannot call the read exit back.
- **W1-3. A person forget's purge is dated by the day the value's wrap
  closed** (`wrap-closed` over the entries on the task: the date the open
  step and a rebuild give); a value with a ledger entry is not purged again;
  a resent person forget purges again, idempotently.
- **W1-4. One read, one view of who is forgotten.** The open step carries
  the person entries through each read or page and reads each person once;
  a read racing a person forget judges all its rows by the entries it first
  read (the same edge as For Sid 19 and 30).
- **W1-5. A micro value forget names its target's entity** in the forget
  fact's `:e` (as the model's forget does, and phase 2's client already
  builds it); forgets at the micro gate stay the operator's (M14).
- **W1-6. An act whose one lock spans entities keeps them on its name
  row** (`$$micro-names [name :entities]`, entity to batch, written only for
  such acts: per-act grain across entities), so a forget of any of its
  values reaches every entity and dates each entity's ledger.
- **W1-7. `:members` is a control key, refused at the stream gate**
  (`:control-not-allowed`), the mirror of the micro gate's
  `foreign-control-keys`.
- **W1-8. The micro store's `$$persons` placeholder is removed**; the micro
  gate reads the gate topology's `$$persons`.
- **W1-9. `read-as-of` is internal by contract**: kept as the store's own
  view for tests and the operator (phases 4 and 8 plan on `c/read-as-of`
  and `c/opens?`), never a reader's path; `:by-stamp` stays until wave 2's
  `:ix-s` decides it.
- **W1-10. Index entries compare by content** in a sweep (bytes as
  vectors).
- **W1-11. The test-only `:put` index op can copy a fact's log row**
  (`:copy? true`), so a test can plant a value-index entry whose value no
  longer opens.
- **W1-12. The rebuild's put page stops at 2,048 rows** (was 4,096), from
  the measurement above.

### Questions for Sid

1. **Whose lock seals an agent's lease?** Built (W1-1): the session layer's
   person owner. The alternatives: an agent is its own principal with a
   lock of its own (then Alice's forget does not reach her agent's lease
   rows until its session closes, For Sid 9's window); or agents lease as
   the operator, bare (For Sid 32's road for tools). Touches no record's
   form: lease rows are consumed state.
2. **Per-act grain across entities** (IMPLICIT_SPEC D12, O8). Built:
   ruling 7's letter, one lock for all of an act's values across entities,
   forgotten as a whole (W1-6). The model keeps one lock per act per
   partition (`[:act name p]`), so forgetting one value of an act spanning
   entities erases only that partition's values there. Phase 8 will report
   the difference. Which is meant?
3. **A control key the gate does not act on.** Phase 1's and 2's stream gate
   admits a fact under a control key when the fact is not one it projects
   (a setting key on an entity other than the layer's; `:promote-request`
   or `:crossed` before phase 4) and keeps its value as plaintext, outside
   every lock: such a value can never be forgotten. Wave 1 closed it for
   `:members` only (W1-7). Should any such fact be refused
   (`:malformed-control`)? It narrows what a record may carry, so it is
   Sid's (first-record: which keys are control keys).
4. **Forgetting one's own values after a re-class.** At the micro gate a
   forget is the operator's (M14, kept); a personal layer re-classed to the
   micro store keeps row locks, but its owner can no longer forget a value
   there. IMPLICIT_SPEC keeps "whether a person can always forget their own
   values" open. Should the owner's forget follow the layer?
5. **The person forget's enumeration at scale** (phase 2's, carried): it
   scans every act on each task inside the fan-out child, over any
   per-event budget at scale. Built: as phase 2 left it, now purging. The
   named fix is an index from a person to the locks wrapped under them, or
   PLAN-reads-rest.md's paged `dying-with>` run by the operator after the
   forget; not built tonight (not small).
6. **Forget's reach over reads, again** (For Sid 28): after a person is
   forgotten their read entries cannot lease, so the exit refuses their
   reads; `rig.store.wave1-test` reads their layer below the exit. Intended?

### First-record placeholders

- **The forget fact's entity is its target's** (W1-5): phase 2's client
  already built it so; the micro gate now relies on it to find the target.
  A kept forget fact carries this convention.
- No other form changed tonight: the lease rows' `:under`, the name row's
  `:entities`, the index entries and the cap are store state, rebuildable or
  consumed.

### Defaults applied

- Default 1 (sealed at the door, leases, the session owner's lock): the
  exit's entries lease and seal like any offer; W1-1 follows its wording.
- Default 4 (one exit; agents' reads recorded): the exit is the one read
  path (W1-9); agents' reads recorded (W1-1).
- Default 6 (the base's lock rules from day one): traced on both gates
  (IMPLEMENTATION_VALIDATION-wave1.md, plan conformance item 8).
- Default 8 (anything else, the simplest thing that changes without
  touching a record): W1-2 to W1-12.

## The suite

One full run at the end, by Sid's rule (`runs/wave1-suite.txt`; full output
`runs/wave1-suite.log`, git-ignored), from the rig folder:

```
flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock clojure -M:test rig.smoke-test rig.claims-test rig.revision-test rig.store.clock-test rig.store.envelope-test rig.store.stream-gate-test rig.store.reads-test rig.store.read-exit-test rig.store.read-model-test rig.store.lock-test rig.store.forget-test rig.store.micro-prepare-test rig.store.micro-test rig.store.wave1-test
```

05:20:22 to 05:27:47 IST (7 min 25 s): **93 tests, 5,538 assertions, 0
failures, 0 errors.** The code and tests as committed at `aa33d905`, with
`rig.store.wave1-test` as first written. Two fixes came after it started,
and only the namespaces they touch were rerun (05:29:39 to 05:30:18,
`runs/wave1-touched.log`, git-ignored): the "two forgets of one value"
block (T-W1, T-W2) in `rig.store.wave1-test`, and the put page's cap in
`rig.store.read-exit-test` (its rebuild runs put pages): 3 tests, 459
assertions, 0 failures, 0 errors. The micro cluster ran on 8 tasks, the
stream gate suite on 8, the forget suite on 2 (each draws its count).

Earlier runs, for the record: the bare merge 91 tests, 34 failures, 3
errors (`runs/wave1-baseline.log`); the first whole run after the wiring 93
tests, 5,535 assertions, 2 failures (`runs/wave1-suite-try1.log`), both in
micro_test's expectations (W-V6). All git-ignored.

Not suite members (the brief): `test/rig/bench/*` and the two runners; all
four benches load on the merge (`rig.bench.lock-slice`, `lock-bench`,
`stream-bench`, `put-page-bench`); whether the older benches still measure
what they did under sealing is phase 7's.

## Open issues, for Sid and wave 2

- **The micro leader holds no opened value**, so a read entry written into a
  shared working layer would not have its moment counted in its stamp
  (`gate/stamp-for`'s 4-arity): wave 2's shared reads must carry the moment
  from the arrival task.
- **The micro gate's lease road still refuses an agent** (`persons-to-check`,
  `mint-rows` under the writer): W1-1 is the stream gate's only.
- **The micro forget exposes no list of the fact ids it erased** to a seam:
  PLAN-reads-rest.md's micro purge takes "the list of fact ids erased, with
  the date" from phase 3's forget; `forget-effect` computes it (its
  `sharing` rows) and wave 2 can emit it.
- **PLAN-reads-rest.md changes the person purge** (only the value-bearing
  entries, paged, after the forget, through `dying-with>`) and the rebuild
  with it; wave 1 keeps today's invariant (tombstones in both), under which
  purge and rebuild agree.
- **The person forget's enumeration is over any per-event budget at scale**
  (phase 2's; For Sid question 5 above).
- **A control key the stream gate does not act on keeps its value as
  plaintext** (For Sid question 3; W1-7 closed `:members` only).
- **The sweep page was not re-measured** under sealing (512 entries, each an
  open; estimated 0.5 to 0.8 s before sealing).
- **An act larger than the put page's cap makes one page of its size**.
