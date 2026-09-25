# Test Validation — stage 1, "stream-store"

<!-- Phase 6 of the rama skill, inside the stage's build session (Claude Opus
5.5, 25 September 2026). Template: references/artifact-test-validation.md.
Tests reviewed: test/rig/store/envelope_test.clj (pure, no cluster) and
test/rig/store/stream_gate_test.clj (one in-process cluster, and a
module-free schema probe); test/rig/smoke_test.clj is the earlier smoke test
and is run with them. Against: IMPLICIT_SPEC.md sections 0-3 and its
stream-side entries (OP1, OP2, OP4-OP8, RD1, E1-E4), SPEC.md's rig choice R7,
the plan's test list (PLAN-stream-store.md "Namespaces and tests", with
F13), and the protocol: the public functions of rig.store.client,
rig.store.envelope and rig.store.gate. No test was run for this artifact
(phase 7 runs them); every entry is a walk-through of the test source.
Citations name the file (env = envelope_test.clj, sg = stream_gate_test.clj),
the deftest and the testing block. -->

Default verdict at the start: major-fail. The first walk found seven rows
of the implicit spec with no test (listed under "Found and fixed"); each was
added as a testing block or case inside an existing deftest, and the walk
below is of the tests after that.

## Found and fixed during this walk

1. OP1 concurrency, "two offers replacing the same head: the first decided
   is admitted, the second refused stale" was tested only in sequence.
   Added: two replaces of one head sent at once (sg `stream-gate`, "offers
   at once").
2. E1 N0 × first offer refused, RD1x "a refused first use still holds the
   name", and N3 × other content and × tag mismatch had no test. Added
   (sg, "refusals recorded ...", nested block "a refused first use still
   holds its name").
3. E1 N1 × tag mismatch while the original is in flight: added a mis-tagged
   copy sent at once with the original (sg, "offers at once").
4. E1 N3 × crash, replay: the completed-record replay used only a yes.
   Added a refused completed offer in the same attempts (sg, "a completed
   offer replayed ...").
5. E4 P1 (a session root) × revoke, no cascade: added at the end of the
   cluster test, after the crashes, because it revokes Alice's hand-session
   root (sg, "E4 P1 (a session root) × revoke ...").
6. OP5 concurrency, "an offer racing the layer's making: refused as data":
   added the making act and an offer into the layer sent at once (sg,
   "offers at once").
7. OP2 edge case, the operator under a reserved scheme: added to the pure
   face table (env `face-refusals-by-reason`) and to the cluster's face
   block (sg, "refused on its face ...").

## Minimize IPC launches

- One `create-ipc` + `launch-module!` in the new tests: sg `stream-gate`,
  `{:tasks (rand-nth [2 4 8]) :threads 2 :workers 1}`, every cluster
  scenario a `testing` block inside it, on disjoint names and, where a
  scenario changes a layer for good (re-class, a revoked root), on its own
  layer or at the end.
- sg `layers-schema-probe` and every env deftest launch no cluster: they
  use `create-test-pstate` or no Rama at all, so they cost no IPC launch.
  They are separate deftests only for reading; no cluster state is shared.
- test/rig/smoke_test.clj launches its own cluster; it is the earlier
  file, not part of this phase's writing, and is in the suite command as
  given.
- PASS.

## Implicit spec coverage

Rows of the implicit spec that belong to later stages (locks and forgetting,
reads RD2-RD7, the micro store, promotion, grammars and tools) are named as
such and not counted here; DECOMPOSITION.json gives each to its stage.

### I-G, I-O, I-P requirements (section 3)

| requirement | test |
|---|---|
| I-G1 a gate never throws; every refusal is data | env `parse-is-total` (the unguarded `parse*` on generated garbage and mutated offers, 1500 cases; `intake` never a gate error); env `decide-is-total-and-fits-the-schema` (the unguarded `decide*`, 700 cases, every write applied to a test PState of the real schema); env `parse-bounds` (nesting 3000 and 200000 deep, unguarded); sg "records a client should never send ..." (16 malformed records through the cluster, each answered through the ack, the append never failing); sg "... owner is not a keyword ..." (the phase 4 finding through the cluster) |
| I-G2 idempotent under replay; same answer, stamp, facts | sg "a crash before the writes", "a crash after the writes", "a completed offer replayed ..." |
| I-G3 resend, answer from the record | sg "a crash before the writes" (`offer-until-answered!` after the crash); "E1 ... a resend of the same map" |
| I-G4 answer by name plus layer, never a scan | sg "lookup by name plus layer (RD1, F13)" |
| I-G5 the check order | env `refusal-order` (each reason alone; ten pairs of reasons, the earliest wins; every reason one of the plan's); sg "refusals recorded ..." (each reason through the cluster) |
| I-G6 whole or not at all, one stamp | sg "chains ...": one good and one stale replace refused whole; "E1 ... admitted": every fact at the act's stamp; "an act of 600 facts" |
| I-G7 names tagged; reuse refused by digest; reserved schemes | sg "E1 ... other content", "the same name under another layer or class", "refused on its face"; env `digest-properties`, `names-and-tags` |
| I-G8 envelope; unknown parts refused | env `face-refusals-by-reason` (act and fact parts); sg "records a client should never send" |
| I-G9 per task: the log, answers by name, the stamp | sg "E1 ... admitted" (record, rows, stood-on, clock) |
| I-O2 clock promises | env `stamps` (property); sg "stamps keep the clock promises" (strictly increasing on a task, after a carried stamp, at the simulated wall, one past the clock when the wall stands still), "chains" (after the replaced fact) |
| I-O4 order across stores only through stood-on | the stream side carries stamps: sg "stamps ..." (a carried stamp from another layer's fact); the other store is stage 3's |
| I-P1 a person names a permission; in its layer | sg "refusals recorded ..." (nil permission, one kept in another layer) |
| I-P3 owner derivable from author, layer, permission | sg "E1 ... admitted": the record keeps who and permission, the layer is its key |
| I-P4 operator trusted, no signing | every operator act in sg (seed, grants, revokes, re-class) passes without a permission |

### OP1. Offer an act into a one-owner layer

| row | test |
|---|---|
| admitted whole on the home, one stamp | sg "E1 ... admitted"; "an act of 600 facts" |
| every fact names the act's layer | sg "refusals recorded ..." (a fact naming another layer); model parity (`:other-layer`) |
| class equals the layer's class fact | sg "refusals recorded ..." (before a re-class), "re-class ..." (after); model parity |
| permission covers, lives in the layer, exists, not revoked | sg "refusals recorded ...", "grant and revoke ..."; model parity for from-another-layer, no-permission, revoked |
| replace names an unreplaced fact of the same layer, entity, key; no doubled | sg "chains ..."; model parity for stale and doubled |
| subjects: the owner with what the tool carried | sg "E1 ... admitted" (`#{:alice :carol}`), "a value about no one ..." (`#{:alice}`); grammar subjects are stage 2's |
| a lock per value | stage 2 |
| stamp keeps I-O2; answer with digest and stamp on the home | sg "E1 ... admitted" (digest = the client's `offer-digest`, stamp, clock) |
| concurrency: decided in the home task's order | sg "stamps ..." (strictly increasing), "offers at once" |
| concurrency: two replaces of one head, first admitted, second stale | sg "offers at once" (added, item 1) |
| concurrency: a resend racing its original finds the record | sg "offers at once" (one name twice) |
| edge: empty act | env `face-refusals-by-reason`; sg "records a client should never send" |
| edge: a very large act | sg "an act of 600 facts" |
| edge: a store-placed fact mixed with ordinary facts | sg "the owner switches her grain, and back in an act that also writes a value" |
| edge: a value failing its key's grammar | stage 6 |
| edge: stood-on naming a fact the gate cannot see | sg "stamps ..." (a carried stamp for a fact of an unknown name: the carried stamp is used) |
| edge: a write about a person already forgotten | stage 2 (O11) |
| edge: a layer with no class fact | sg "refusals recorded ..." (`:yan`, `:no-such-layer`); env `refusal-order` |
| edge: a closed session | not ruled (O10); no close operation exists in this stage |
| edge: a value about no one | sg "a value about no one ..." |

### OP2. Resend; look up

| row | test |
|---|---|
| same content: recorded answer and stamp, even when the cause has gone | sg "a resend of the same map"; "granted since: the refused name stays refused ..." |
| other content: name taken, first record untouched | sg "other content under the name" (and a rebuilt map with a new claimed-when); "a refused first use still holds its name" |
| layer or class other than the tag: refused on its face, reading nothing | sg "the same name under another layer or class", "refused on its face ..." (clock unchanged, nothing recorded) |
| neither a taken name nor a face refusal written as the name's answer | sg "other content ..." (record unchanged), "refused on its face ..." (no record under the name, in the offer's layer or the name's) |
| "no answer yet" distinguishable from every refusal | sg "lookup by name plus layer" (`:no-answer` for an unknown name and for the name under another layer) |
| a yes and a read never contradict | stream side: every read after an acked append sees the act (sg "E1 ... admitted"); the frontier is stage 3's |
| concurrency: two contents under one name at once | sg "offers at once" |
| edge: an honest collision is a reuse | the same code path as sg "other content under the name" |
| edge: a reserved scheme from a person or the operator | sg "refused on its face ..." (both, item 7); env `face-refusals-by-reason` |
| edge: a resend after a re-class | sg "re-class ..." (decided before: the record answers; never decided: class mismatch) |
| edge: a resend after a forget | stage 2 |

### OP4. Replace and retract

| row | test |
|---|---|
| replace names the unreplaced fact; replaced once | sg "chains ..." (C1 × replace, C3) |
| replacing an erased fact | stage 2 |
| several unreplaced facts; the new one beside the head | sg "chains ..." (C1 × a new fact without replace: both heads) |
| a retract is a new fact, undoable | sg "chains ..." (retract with `:v nil`, its undo) |
| edge: a replace naming another layer's fact, or no fact | sg "chains ..." (both) |
| edge: two facts of one act replacing one fact | sg "chains ..."; model parity |
| edge: after a re-class, in the micro store | stage 3 |

### OP5. Make a layer

| row | test |
|---|---|
| first facts: kind, owner, class, grain, root permission | sg "seed ..." (the model's three layers and seven permissions); "making a layer ..." (`:xia`, made at run time: layers are data) |
| session kind from its start fact | sg "making a layer ..." (`:xia` made `:hand`; `:alice-agent` seeded `:agent`) |
| default visibility from seed policy facts | stage 5 (reads) |
| made twice | sg "making a layer ..." (`:layer-already-made`, the owner not rewritten; F8) |
| concurrency: an offer racing the making: refused as data | sg "offers at once" (added, item 6); sg "refusals recorded ..." (`:yan`) |
| edge: a person with no session cites own-layer permissions | sg "refusals recorded ..." (Bob granted `[:bob :alice :alice]`, then admitted) |

### OP6. Grant and revoke

| row | test |
|---|---|
| a permission is a fact in the layer it governs | sg "seed ..." (session permissions in the hand session only), "grant and revoke ..." |
| a revoke stands on the permission fact | sg "grant and revoke ..." (`:stood-on {grant-fid grant-stamp}`, stamp after it) |
| later decisions citing it refused; earlier ones stand | sg "grant and revoke ..." (P2 × write refused; the earlier act's resend unchanged); model parity |
| revoke of an unknown, a second revoke, a revoke placed elsewhere, two in one act | sg "grant and revoke ..." (all `:stale-revoke`); env `refusal-order` |
| re-grant | sg "grant and revoke ..." (admitted as a fact; the index keeps the first grant and its revocation) |
| no cascade from a session root | sg "E4 P1 (a session root) × revoke" (added, item 5) |
| only the operator grants and revokes (R13) | sg "only the operator writes control facts ..." |
| the revocation race in a shared layer; forgets citing a revoked permission; a landing's permission | stages 3, 2, 4 |

### OP7. Re-class

| row | test |
|---|---|
| a fact on the home, ordered with the layer's writes | sg "re-class ..." (the store places it: name class nil) |
| every stream offer decided after it: class mismatch; before: stays | sg "re-class ..."; model parity |
| later names tagged by entity reach one gate only | sg "re-class ..." (`:wrong-gate` on the stream gate) |
| the re-classer is the operator | sg "only the operator writes control facts ..." (a person re-classing: `:control-not-allowed`) |
| P16: a store-placed act of a re-classed layer stays with the stream gate | sg "re-class ..." (the grain switch) |
| re-class back | sg "re-class ..." (`:unsupported-reclass`, class unchanged; F8) |
| the move to the micro gate, the micro gate reading settled history | stage 3 |

### OP8. Switch lock grain

| row | test |
|---|---|
| a setting the owner changes, and back | sg "the owner switches her grain, and back ..." |
| a malformed grain value | sg "only the operator writes control facts ..." (`:malformed-control`, R13) |
| a person other than the owner | env `refusal-order` (Bob's switch on Alice's layer: `:control-not-allowed`) |
| affects only later values; per-act locks | stage 2 |
| on a shared layer | stage 3 |

### RD1

sg "lookup by name plus layer (RD1, F13)": with the right digest (the
record), with no digest (the record as data), with another digest (name
taken), an unknown name (`:no-answer`). RD10 (settings, a permission's
state): read throughout sg with `c/settings` and `c/permission`.

### E1. An offer's name and its answer record

| row | test |
|---|---|
| N0 × first offer admitted | sg "E1 ... admitted" |
| N0 × first offer refused (recorded, stamp, RD1x name taken) | sg "refusals recorded ..." (every `check-no`), "a refused first use still holds its name" (added, item 2) |
| N0 × tag mismatch | sg "refused on its face ..." |
| N1 × resend of the same content | sg "offers at once" |
| N1 × other content, both in flight | sg "offers at once" |
| N1 × tag mismatch in flight | sg "offers at once" (added, item 3) |
| N1 × crash mid-decision | sg "a crash before the writes", "a crash after the writes" |
| N2 × resend | sg "a resend of the same map" |
| N2 × other content | sg "other content under the name" |
| N2 × tag mismatch | sg "the same name under another layer or class" |
| N2 × crash, replay of the finished record | sg "a completed offer replayed ..." |
| N3 × resend | sg "granted since: the refused name stays refused" |
| N3 × other content, × tag mismatch | sg "a refused first use still holds its name" (added, item 2) |
| N3 × crash, replay | sg "a completed offer replayed ..." (the refused one, added, item 4) |

### E2. A chain (stream side)

C0 × new fact, C0 × replace of nothing, C1 × new fact without replace,
C1 × replace of the head, C3 × replace of a replaced fact, C1 × two
replaces in one act, C1 × retract: all in sg "chains ...". Value forget,
person forget, promotion, landing, C2 and C5 rows: stages 2, 4, 6.

### E3. A layer

L0 × offer: sg "refusals recorded ..." (`:yan`). L0 × make: sg "making a
layer ...". L1 × offer: throughout. L1 × grain switch, L2 × switch back: sg
"the owner switches her grain ...". L1 × re-class, L3 × offer, L3 × re-class
back: sg "re-class ...". L2 × value forget: stage 2. L4: stage 3. L5: not
ruled (O10).

### E4. A permission

P0 × write: sg "refusals recorded ..." (Bob). P0 × grant: sg "granted
since ...". P0 × revoke, P1 × write in its layer, P1 × write into another
layer, P1 × revoke, P2 × write, P2 × revoke again, P2 × re-grant: sg "grant
and revoke ...". P1 (session root) × revoke: sg "E4 P1 ..." (added). P1 ×
revoke racing a write in a shared layer, P2 × forget: stages 3 and 2.

### R7's list and the plan's test list

Every branch R7 lists is covered above: admit whole; the face refusals
(another layer, another class, reserved scheme); reuse by digest; retry
from the record; fact outside the act's layer; class mismatch before and
after a re-class; a permission that does not cover (Bob's, a nil one, one
for another layer), lives in another layer (`[:alice :alice-agent
:alice-hand]` on `:alice-agent`), does not exist, is revoked; stale (already
replaced, another layer, unknown, another key) and doubled replaces, and
one good with one stale refused whole; stamps (never backward, after a
carried stamp, after the replaced fact, at the simulated wall); the same
answer after a crash before and after the writes (one row set, one stamp,
processed at least twice, the append's failure recorded in the output); the
completed-record replay (F13); lookup with no digest (F13); a second
`:kind`/`:owner` and a re-class back (F8); a retract and its undo; the
malformed records of "no throw", `:who :store` among them (F13). The pure
list: parse and decide total on generated input with deep nesting and a
subvec (F13); the digest equal for `=` offers built otherwise and different
when a part differs; `tag-of` against the model's own offers from its fixed
histories. Model parity through `formal.model/run` where the model has the
branch: yes, outside the act's layer, class mismatch after a re-class, from
another layer, no permission, revoked, stale, doubled, retry, reuse, face,
and the failover answer (sg, the `model-answer` and `model-says?` calls).

### The protocol

Every public function of rig.store.client is called by sg: `connect`,
`build`, `offer!`, `record`, `lookup` (name and digest, nil digest, the
offer form through the smoke test), `offer-until-answered!` (and
`answer-of` through it), `facts`, `stood-on`, `settings`, `permission`,
`head`, `clock`, `model-world`, `make-layer-offer`, `grant-offer`,
`revoke-offer`, `seed!`. Of rig.store.envelope and rig.store.gate: `parse`,
`intake`, `decide`, `refusal`, `stamp-for`, `digest`, `offer-digest`,
`canonical`, `encode-value`/`decode-value`, `normalize-value`, `edn-value?`,
`readable-keyword?`, `name-for`, `make-name`, `crossing-name`,
`landing-name`, `uuid7`, `tag-of`, `mis-tagged?`, `pids-to-read`,
`heads-to-read` by env; `answer-from-record` and `face-ack` through the
cluster in sg.

PASS after the seven additions.

## Synchronization

- Stream topology, `:ack` appends: `c/offer!` returns after the event tree
  completes and its writes are visible (stream.md "Ack-Own-Stream"), so
  every read after a `send!` in sg reads the decision without a wait. This
  holds for every block before the crashes.
- Concurrent offers: sg "offers at once" derefs both futures before any
  read.
- Crashes: after an injected crash the worker restarts; every read in the
  crash blocks and after them goes through `retrying` (re-reads while the
  worker is down) or `wait-until` (the answer appearing by lookup, the
  crashed record's second pass); the completed-record attempts wait for the
  crashed offer's replay before reading the earlier offers; the last block
  uses `offer-until-answered!`.
- Simulated time: sg "stamps ..." opens `TopologyUtils/startSimTime` in
  `with-open`, so it closes on failure too.
- No microbatch topology exists in this stage, so no
  `wait-for-microbatch-processed-count` is needed.
- PASS.

## Test namespaces compile

Both new namespaces were loaded (`require`) in a JVM with the test
classpath: `rig.store.envelope-test` and `rig.store.stream-gate-test`
load; clojure-lsp diagnostics (clj-kondo 2025.01.17) reports nothing on
either. They use only public vars of the rig, except the two private
functions the pure tests drive on purpose (`#'env/parse*`,
`#'gate/decide*`, dereferenced as vars), and of the model (`fm/run`,
`fm/answers-for`, `fm/tag-of`, `fm/baseline`, `fs/a-cases`, `fs/b-cases`,
`fs/d-cases`, all public). No record constructors are referred. PASS.

## Self-consistency check

Every missing case the walk found is listed under "Found and fixed" with
the block that now covers it; no entry above certifies a row while naming a
gap. Rows given to later stages are named with their stage, not counted as
covered.

## After phase 7's first run

The suite's first run (13 tests, 720 assertions) had 2 failures and 2
errors; the tests changed as follows, with no new namespace and no case
removed:

- The mutations in `gen-record` could throw inside the generator when
  applied to an already mutated record (an `assoc-in` into facts that had
  become a symbol); a mutation that does not apply now leaves the record as
  it is (env). The two errors.
- The retract's head was asserted after its undo, bound in the same `let`,
  had already replaced it; the undo is now sent after the retract's
  assertions (sg "chains ..."). A test bug; the gate was right.
- `#{(float -3.0)}` is not `=` to its normalised `#{-3.0}` (Clojure hashes
  a Float apart from the equal Double): the envelope now refuses Java floats
  as malformed, and env `parse-bounds` lists a float and a set holding one
  among the malformed values. A module fix, recorded in
  IMPLEMENTATION_VALIDATION-stream-store.md item 6.
- The decision generator reached an admitted act in 8% of inputs; it now
  draws matching settings, granted rows, present heads, the own permission
  and mostly well-formed control values more often, and facts sometimes
  name a layer. Measured on 930 generated inputs: 29% admitted, 62 of them
  writing settings and 18 writing permission rows, and every recorded
  reason but `:unsupported-reclass` drawn (that one is in env
  `refusal-order` and sg "re-class ...").

## Verdict

**pass** after the seven additions. Before them the walk was a
**minor-fail**: each missing row was a testing block or case added to an
existing deftest, with no new namespace and no restructuring.

## Hybrid clock (26 September)

Validated by the clock session against PLAN-stream-store.md "Hybrid clock
(26 September)" and ruling 4. Default verdict fail. For each promise, the
test that exercises it, and whether the old millisecond stamp would fail it
(a test that passes either way checks nothing about the change).

- **The encoding** (`pack`, `ms-of`, `counter-of`): `clock_test.clj`
  `encoding` — fixed values, a 2,000-case round trip over milliseconds to
  2^46 and every counter, order of longs = order of (ms, counter) pairs, and
  every long, negative ones included, is one pair. Pass.
- **Never backward within a unit; never earlier than anything stood on;
  never before the wall's millisecond**: `clock_test.clj` `the-two-promises`
  (500 runs of up to 60 decisions each, walls that stand still, step back or
  jump, stood-on stamps up to 2 s either side of the wall);
  `envelope_test.clj` `stamps` (the gate's `stamp-for` against the formula
  with the clock, carried stamps and replaced heads generated around the
  wall's stamp, so each of them wins in some runs, and a fixed case tying it
  to `next-stamp`); `stream_gate_test.clj` `stream-gate`, "stamps keep the
  clock promises" (six decisions strictly increasing, yes and no alike, the
  clock at the last). The first two fail under the old unit (their expected
  values are packed). Pass.
- **Stamps stay near wall time at a high rate**: `stream-gate`, "stamps stay
  at the wall at a high rate" — 3,200 acts on `:alice-agent`'s task from 32
  offerers at once; for every act, the millisecond it was sent ≤ its stamp's
  millisecond ≤ the millisecond its ack came back; stamps all distinct; some
  millisecond held more than one decision (a counter above 0); the clock at
  the largest. It prints what the millisecond unit would have given over the
  same decisions. The old unit fails it (its stamps read as hybrid are
  milliseconds ÷ 65536). The same wall is read by the test and the gate
  (one JVM, `System.currentTimeMillis` with no simulated time), so the bound
  is exact; it assumes the machine's clock does not step back during the two
  seconds, and it runs before any act leaves a task's clock ahead of the
  wall, which its first assertion checks. `clock_test.clj`
  `stamps-stay-near-the-wall-at-a-high-rate` does the arithmetic at 2,250
  decisions a second for 18 s: lead 0 ms, against 22,500 ms for the
  millisecond unit. Pass.
- **A stood-on stamp ahead of the wall is honoured**: `stream-gate`, "a
  stood-on stamp ahead of the wall clock is honoured" — a carried stamp one
  minute ahead with counter 7 gives exactly one past it; the next act on the
  task is one past that (counter 9), the wall behind; `clock_test.clj`
  `a-stood-on-stamp-ahead-of-the-wall` also shows the wall catching up.
  Pass.
- **The counter rolling past 65,535 within one millisecond**: `stream-gate`,
  under simulated time, "the counter rolling past 65,535 within one
  millisecond" — a carried stamp at counter 65534 puts the task at 65535, the
  next act is the next millisecond's counter 0 though the wall stands still,
  and the wall reaching and passing it gives counter 1, then the wall's
  millisecond at 0. `clock_test.clj` `the-counter` runs 65,537 decisions in
  one millisecond: 65,536 fit, the next carries. The wall standing still
  (counter 0, then 1) is the simulated-time block's first two assertions.
  Pass.
- **A replayed or resent offer answered with its recorded stamp**:
  `stream-gate`, "a resend after the wall moved on" (the simulated wall two
  milliseconds on: the same answer, the record's stamp at the old
  millisecond and counter 0, no stamp given); "a completed offer replayed by
  a later crash" now also resends after the replay and asserts the recorded
  answer and stamp while the task's clock is past it; "a crash before the
  writes" already asserts the resend's recorded stamp. Pass.
- **Overflow is refused, not wrapped** (R16 stays): `clock_test.clj`
  `overflow-is-refused-not-wrapped`; `envelope_test.clj` `parse-bounds`
  (2^62 refused on the face, unchanged). Pass.
- **The model**: every comparison with `formal.model/run` still compares
  answers only; the namespace docstring of `stream_gate_test.clj` says how
  the model's stamps relate (one integer, a tick and a counter step the same
  size; the relations asserted hold in both). Pass.
- **Minimize IPC launches**: no new cluster. The gate's cases run in the one
  `stream-gate` cluster; `rig.store.clock-test` is a new namespace of pure
  tests of the new namespace, with no cluster. Pass.
- **Synchronization**: every send is an acked append, so the decision and
  its writes are visible when it returns; the burst's 32 futures are all
  dereferenced before any read. Pass.
- **Test namespaces compile**: every changed namespace and the three bench
  namespaces loaded with no cluster started. Pass.

Self-consistency: the one environmental assumption (a wall that does not
step back during the burst) is stated, not a missing case. **Verdict:
pass.**
