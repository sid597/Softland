# Test validation — stage 5a, the one-owner read exit

<!-- Phase 6 of the rama skill (phase-6-test-validate.md, template
artifact-test-validation.md), done inside the build session (phase-build.md
step 4), 26 September 2026, by the builder (Claude Opus 5.5, effort max).
Default verdict major-fail. Read: PLAN-read-exit.md "Namespaces and tests"
(T1 to T19) and the exported functions' docstrings (the protocol here),
IMPLICIT_SPEC.md RD2 to RD6 and E8 (the read entry's states), and the three
test namespaces in full: test/rig/store/reads_test.clj,
test/rig/store/read_exit_test.clj, test/rig/store/read_model_test.clj. The
tests were not run in this step. The reviewer wrote them; the first pass
found nine missing cases (below), added before this artifact was finished. -->

## Verdict

**minor-fail**: nine cases the first pass found missing were added to the
existing namespaces (a localized edit each); with them every row below is
covered. Two tests stand in for the plan's wording in ways named below
(T10's forced append error, T12's second run); neither weakens what is
asserted.

## First pass: missing cases, added

| # | source | missing | added in read_exit_test.clj |
|---|---|---|---|
| M1 | RD2 edge cases | a point read of a fact id never admitted, and of an index past its act's rows | T8: both absent alike, beside one admitted after the moment; the entry records `:absent` for each |
| M2 | RD4 edge cases | "T before any fact: empty" | T8: `[:all]` as of 0 |
| M3 | RD3, ruling 3 | the line's role (only the pure test checked it) | T4: the default `:shown`, and a role given (`:stood-on`) recorded |
| M4 | RD6, E8 R1 × forget | a re-run after a forget cannot reach the first fingerprint; the earlier entry is never rewritten | T9: both |
| M5 | RD3 concurrency | a forget while a read runs: value or date, never a match on a value erased before | T9: a purge during a paged read of 1,500 entries; the row shows the value or the date |
| M6 | E8 R3 × forget | a point read of an erased fact reads "erased on F" | T19: under the erasure double, the row's date and no value; the entry's `:shown :erased` |
| M7 | the plan's hooks | `:exit-shown` armed | T11 (b2) |
| M8 | D5 (the build's divergence) | act order and `[:latest]` past index 9, in the cluster | "D5": 12 facts on one cell |
| M9 | D7 (the build's divergence) | a re-classed layer refused, after visibility | "D7" |

## Minimize IPC launches

- `rig.store.reads-test`: no cluster (16 `deftest`s of pure functions;
  `testing.md` "test-pstate is module-free" does not even apply: no PState).
- `rig.store.read-exit-test`: one `deftest`, one `create-ipc` +
  `launch-module!`, every scenario a `testing` block on its own entities or
  layers (`:t2`, `:t5`, `:t5b`, `:t6`, `:t8`, `:t9`, `:t9b`, `:t10`, `:t12`,
  `:t18`, `:t20`, `:t21`, and so on), crashes last.
- `rig.store.read-model-test`: one `deftest`, one more cluster. The shared
  mutable state that would interfere: `TopologyUtils/startSimTime` is
  JVM-global, and T1 holds the gate's wall on simulated time for its whole
  replay, one millisecond per model op, so that stamps follow the op order
  across tasks. Inside read-exit-test that clock would stand still for T5's
  500-fact act, T18's clock push and T11's worker restarts, and when closed
  it leaves every task's clock ahead of the real wall. The plan also names
  the model test its own namespace. Histories inside it share its one
  cluster by renaming the rig's layers per history.
- **PASS.**

## Implicit spec coverage

The plan's tests, T1 to T19:

| test | where | what it asserts |
|---|---|---|
| T1 | read-model-test `read-model` | per history (two fixed, three seeded random, 25 ops each), at every model read and the closing one: per layer, `[:all]`, `[:e]` (and an absent entity), `[:ek]`, `[:latest]`, `[:k]`, `[:kv :note v]`, point reads of every shown fact and one later fact; every offer's answer, retries answered from the record, reuses refused; `formal.model/run` agrees with the stepwise replay; the plan's single moment reported (OBSERVED); scenarios.clj's cases each shown to need a later phase |
| T2 | read-exit `T2` | the gate's `:after-writes` for the entry before `:exit-shown`; the entry `:yes`; the returned entry stamp is the record's |
| T3 | `T3` | a revoked permission: `:permission-revoked`, the entry's name, no rows, the gate's recorded no; a working layer never made: `:no-such-layer` |
| T4 | `T4` | `[:e :nobody]`: no rows, `:count 0`, `:complete`, `(reads/fingerprint #{})`, empty exact list, role and pattern; a given role recorded |
| T5 | `T5`; reads-test `page-loop` | limits 5, 3, 4: complete, partial, partial; first three in address order; the fourth's id and stamp nowhere in the return (F3); each mark and count on the line. F8 in the cluster (partial where a larger limit completes) and exactly 176 scanned in the pure driver |
| T6 | `T6` | a replace changes the fingerprint; an unmatched fact and an unmatched chain do not; the first moment again gives the first fingerprint; it equals `reads/fingerprint` of `:matched` |
| T7 | reads-test `fingerprint` | the hashed bytes are the label and the set's canonical text, no value text; order-independent; keyed (not the unkeyed SHA-256); nil for anything but pairs |
| T8 | `T8` | as of s1: `[:ek]` and `[:latest]` show f1, f2 absent, `[:all]` stamped ≤ s1, the moment recorded; as of now both and f2; FR2's clamp; after a purge dated s3 the read as of s1 shows f1 with s3 and no value and nothing after s1; M1, M2 |
| T9 | `T9` | no value entry holds the value's text, no `:ix-of` entry, one tombstone per id index; `[:kv]` no longer matches; `[:e]` shows the date only; the four fields hold the text nowhere; M4, M5 |
| T10 | `T10`; reads-test `rebuild-writes` | the fields equal what the log implies; a rebuild over them deletes and rewrites nothing; dropped to empty and rebuilt, entry for entry; a stale entry deleted; an offer between pages kept; a page sent twice changes nothing; pure: put pages equal admission, purge agrees with rebuild, sweep keeps, deletes, rewrites (D4), and removes from `:ix-of` |
| T11 | `T11` (a), (b), (b2), (c), (d), and the global check | after the query: thrown, no answer, no line; after the entry and before the return: thrown, `:yes`, one line; the gate crashing before or after the entry's writes: rows shown, one line; every read that returned rows (over 30) has its entry `:yes` |
| T12 | `T12` | an ordinary offer crashed after its writes: after the replay the fields equal what the log implies, each fact once in each id index and once in `:ix-of` |
| T13 | `T13` | Bob refused Alice's layer, nothing recorded; Bob reads the base; a model reader for Alice reads her personal and agent layers, its entry's `:who` the agent in `:alice-agent`; for Bob, refused |
| T14 | reads-test `totality`, `generated-offers`; read-exit `T14` | 300 generated garbage inputs to 19 functions the topologies call, none throws; 300 generated offers: no index error, replay-equal, two id entries per fact, one value entry per note, value text round-trips; in the cluster, 16 malformed query inputs and 7 malformed ops answered as data with no exception at the client (a query throw would reach it), no crash fired, the gate and queries still answering; the exit's `:bad-read` cases (D6, D12, an unknown role) |
| T15 | `T15, T16` | a model's line in the agent session layer carries the exact list |
| T16 | `T15, T16` | person and model exact; a tool without, with `:rows? true` with; a point read of three ids records three rows, each naming the id, never the value |
| T17 | the suite (phase 7) | phase 1's namespaces run again against the changed gate |
| T18 | reads-test `entry-moments-and-the-stamp`; read-exit `T18` | pure: moment + 1 above clock and wall; nothing for a non-line value, a moment at the bound, a negative one; cluster: a working layer on another task (found by its clock, see below) records an entry stamped after its moment and after every matched stamp, and its task's clock jumps past the moment |
| T19 | `T19` | a layer never made: `:not-visible`; a `[:kv]` candidate the double erases: not shown, not counted, not fingerprinted; the line's id entries `:copy false` with no value, and `[:k :read/pattern]` still shows the line's value from its row; M6 |

IMPLICIT_SPEC.md rows:

| row | case | where |
|---|---|---|
| RD2 | the chain's head as of the moment | `[:latest]`: T8, T1, D5 |
| RD2 | its value, or "erased on" | T8, T9, T19 (M6) |
| RD2 | "opaque" for an opaque key | the showing is phase 6's (not built); `[:kv]` on an opaque key refused: reads-test `parse-pattern` |
| RD2 | a key never written: empty, never an error | T4, T1 (`[:e :nobody]`), M1 |
| RD2 | a layer the reader cannot see (O22) | T13, T19: same answer as a missing layer (F4) |
| RD3 | never matches an erased value | T9, T19 |
| RD3 | one line with pattern, moment, role, fingerprint, mark; empty reads included | T4 (M3), T5, T15 |
| RD3 | exact list by reader kind | T15, T16 |
| RD3 | eager; agent session layers recorded (default 4) | T13, T15 |
| RD3 | indexes rebuildable or purgeable by value id | T9, T10 |
| RD3 | against a forget: opened or not, never a match on a value erased before | T9 (before), M5 (during) |
| RD3 | a huge match is partial | T5 (limit), F8 (budget) |
| RD4 | every fact at or before T, erasure dates even after T, nothing else after T | T8 |
| RD4 | T before any fact: empty | M2 |
| RD4 | T past any stamp given (O23) | T8 FR2: the clamp |
| RD6 | a re-run reaches the fingerprint while nothing is forgotten | T6 |
| RD6 | after a forget it cannot | M4 |
| RD6 | the fingerprint is keyed | T7 |
| E8 | R0 × tool, person, model, empty, point, agent session | T16, T15, T4, T16, T15 |
| E8 | R1 and R3 × forget of a matched value | M4, M6 |
| E8 | R1/R2 × the other store admits a fact at or before the moment | not tonight: no shared-layer reads (phase 3); the plan's scope |
| E8 | R2 × re-run | T5 (the mark) |

Stand-ins, named:
- **T10's "a page resent after a forced append error"**: the ops source has
  no crash hook, so the resend is the same record sent twice, which is what
  a resend is; asserted equal answers and unchanged fields.
- **T12's "the same history run without the crash"**: compared with what
  the log implies (`reads/implied`), the stronger check, since a second run
  has other names and stamps.
- **T18's "computes both homes from the depot's partitioner"**: the homes
  are observed instead: after the read layer's task clock is pushed 2^40
  ahead, a working layer whose clock is still behind is on another task
  (eight candidates on four tasks).
- **T1's moment**: per layer, as the module's docstring explains; the
  plan's single number is run beside it and every difference it gives is
  printed, not asserted.

Not covered here, and why (each the plan's scope, not a gap in these
tests): standing reads (not built); shared-layer reads and the other
store's late facts (phase 3); a forget's reach through phase 2's
`open-row>` and person forget (phase 2; stood in for by the test-only
`:purge` and the double); reads during a rebuild (named in the plan, not
asserted there either).

**PASS** after M1 to M9.

## Synchronization

The store under test is stream-only. Every write is an acked append
(`client/offer!`, `offer-until-answered!`, `read-exit/index-op!` all use
`:ack`, which waits for the gate's event and its commit), so a read that
follows sees it (`testing.md`: "ACK blocks downstream"; stream.md RAW).
Query topologies and foreign selects read committed state. After an
injected crash every read is wrapped in `retrying` (read-exit-test T11,
T12) or waits for the answer (`wait-until` on `lookup`, T12). The
concurrent case (M5) derefs its future before asserting. The model test's
offers are acked through `offer-until-answered!` before its reads. No
microbatch topology exists, so no `wait-for-microbatch-processed-count` is
needed. **PASS.**

## Test namespaces compile

All three load cleanly: `clojure -M` with the test paths and test.check,
`(require 'rig.store.reads-test 'rig.store.read-exit-test
'rig.store.read-model-test)` printed `:loaded` (after M1 to M9 too). They
use public functions only, except reads-test's `#'gate/log-rows` and
`#'reads/kv-prefix`, taken by var so the test checks the gate's own row
builder rather than a copy. No record constructors. **PASS.**

PHASE_VALIDATION:minor-fail
