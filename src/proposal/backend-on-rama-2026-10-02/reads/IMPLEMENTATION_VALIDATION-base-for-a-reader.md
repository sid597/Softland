# Implementation Validation: the base, for a reader

**Which way ruling 17 (d) went: the run holds.** The stop is not met. On the store's own module, a
real batch of the store's own microbatch gate was made to run a second time, with no hook in the
module's source. Both attempts were given one clock. The rows of the two attempts, set side by side,
give no key: 52 keys, and at each key the same row in every field, the stamp too. The repair stays
built, and nothing of "the row before, kept beside the new one" is built.

**What this paper is.** The build stage's check of its own code for the part `base-for-a-reader` of
the read build: the contract's A7 (`../BUILD-reads.md`), as rulings 16 and 17 of `PROGRESS.md` read it
and as `PLAN-base-for-a-reader.md` plans it after its check. It follows the skill's template
(`artifact-impl-validation.md`), each heading answered for what this part adds or changes.
**The stage.** The `build` stage of the Rama skill (`phase-build.md`), for one part of three.
**Who wrote it.** One fresh session (Opus 5.5), in the night of 11 to 12 October 2026. It wrote the
code of the part, its proof and its explanations under `src-store/` and `test-store/`, this paper,
`TEST_VALIDATION-base-for-a-reader.md`, and files under `runs/` whose names begin `raw-bfar-` or
`after-base-for-a-reader-`. It changed no paper of another stage. It did not commit and did not stage.
The tree was at head `ffc10f29`, with `src-store/` and `test-store/` as `371c6d2f` left them.

**Marks.** "Run" names a run of this session and its file under `runs/`. "Read" is read in the source
at the line given, in the tree as this session leaves it. "Derived" is reasoned here from lines that
are named. "Not run" is said where it is so. A file name without a folder is under
`src-store/softland/store/`.

## For the receipt: what was run where, and what was not run

**Run on the store's own module**, in Rama 1.6.0's in-process cluster, one machine, 4 tasks, 2
threads, 2 workers, one copy of each row. Each by the module's proof, `runs/raw-bfar-module-final.txt`,
cut to `runs/after-base-for-a-reader-module.txt`. The numbers below are of that file: the last of
four whole runs of the proof on the gate as built. Each of the four held: two attempts, one clock,
52 keys, no key that differs. A count that rests on a time is another in each run, such as how many
batches the first cluster decided.

**The tree of the last runs.** While this session wrote its papers, the read path's session changed
files that the four proofs load: it added six functions to `record/rows.clj` and sentences to the
docstrings of `module.clj` and `module/door_reads.clj`, on top of this part's lines there, which all
stand. So the four proofs were run once more, after those changes: the four count lines of this
paper's last section are of the tree with them in. No `.clj` file under `src-store/` or `test-store/`
changed after those runs began. That session also edited the maps after them; a proof loads no map.

- A second attempt of a real batch that failed while it was processed. 18 things in one batch, every
  one an act into the base. The first attempt threw in step 4, in the one call of `rows-to-send` that
  gave rows (20 rows), after every row of the attempt was noted and before anything was committed.
  Rama started the worker again and ran the batch again. All 18 things were answered 2,149 ms after
  the topology went on.
- That both attempts were given one clock, 1791771668202: the clock of the kept row
  `{:batch 9, :clock 1791771668202}` and of the batch's one record on the depot of batch clocks. The
  second attempt began 1,163 ms after that clock. Both were given the same last stamp and the same
  numbers, which is how the proof knows that they are two attempts of one batch and not two batches.
- That the two attempts made the same rows: 52 keys, no key in one attempt only, no field of any row
  different. By kind: 18 answers, 14 facts (one of them written twice in the batch, so 15 rows), 4
  keys of the facts by value (two rows in, two keys taken out), 7 whole rows of used locks, 7 home
  rows of those locks, 2 keys of new locks, which are noted with no row. The fields compared are every
  field that a row holds: of a fact `:value`, `:by-act`, `:by-index`, `:stamp`, `:who`, `:permission`
  and, in the row of the marked keys, `:level-since`; of an answer `:refused`, `:stamp`, `:digest`; of
  a row of the facts by value `:by-act`, `:by-index`, `:stamp`, `:who`, `:permission`; of a lock
  `:bytes`, `:way`, `:seals-act`, `:seals-index`.
- That the rows of `what-the-gates-keep` are among them (ruling 53): a person's fact with who wrote it
  and her permission; a write of each of the three new store keys; a fact under a marked key over a
  value, so that one row of the facts by value came in and one went out; two acts on one place under
  that key; the row of the marked keys with its derived stamps, `{:on <the stamp from before> :in
  <the last stamp of this batch>}` on every task; a give with the slot.
- That each row which the tables hold after the batch, selected on every task, is the row that both
  attempts made (0 rows otherwise), and that each answer as `answer-by-act-id` gives it has that row's
  verdict, word and stamp (0 otherwise).
- The act at the edge of the 10 seconds: the first attempt was given a clock 134 ms after the hand's
  reading and the second attempt began 1,297 ms after it, so the act was at the edge. Held to the kept
  clock it was refused `:carried-stamp-too-far-ahead` on both attempts.
- Ruling 17 (a): on that cluster the depot of batch clocks held 5 records for 5 batches with things,
  which made 6 attempts; a quiet 1.5 s added none; one more batch added one, under a later id.
  Ruling 17 (b): each record is a map of exactly `:batch` and `:clock`, two integers.
- On the proof's first cluster, with every function as the store defines it: 76 batches kept 76
  clocks, all on one partition; no lock's bytes are on that depot; the kept row was the same after
  the module was started again by Rama's module update, and a batch after it kept its clock under a
  later id; and 297 selects of a place that an act replaced, from before the append until the new row
  showed, each gave a row and never nothing.
- The same group, once, on the gate changed for that one run so that a later attempt reads the clock
  anew (`runs/raw-bfar-mutation.txt`): 4 of its 13 checks failed. 35 keys differed between the two
  attempts: all 18 answers, 15 facts and 2 rows of the facts by value, 32 of them in `:stamp` alone;
  the row of the marked keys also in `:level-since`; and the edge act was refused on the first
  attempt and accepted on the second, where it wrote a fact that the first did not. So the check sees
  the difference that the kept clock removes, in the rows of `what-the-gates-keep` too. The gate was put back and compared byte for byte with the file as
  built before any other run.

**Run only on a probe's module, not on the store's.** The window: a commit that fails after some tasks
have shown their rows, and what a reader is given across it (`batch-probe/RESULTS.md`, B7). A failure
of an attempt before its clock is kept; a record that the topology which keeps the clock fails; that
topology not answering until Rama's phase timeout (`plan-probes-base-for-a-reader/`, W5). What an
append costs inside an attempt, measured over many batches (B3.ii, W). This session ran no probe.

**Not run, by anyone in this build.** A worker whose process is killed. A move of a task's leader.
A real cluster, and so more than one copy of the kept row and what the ack of the append waits for
there. A module update between two attempts of one batch. The module update that brings this part
into a cluster that already runs the store without it: the proof's update is of the unchanged module.
The late ack of an attempt that Rama gave up. The gate's one throw inside the gate: no run reached
it; the proof calls its function by hand.

**What one batch with things pays more, as built.** In the attempt that keeps the clock, all on the
gate's task: 3 reads (the row at `microbatch_gate.clj:434`; the read of the topology `batch-clock` at
`batch_clock.clj:93`; the row again at `:442`), 1 record appended to the depot (`:441`), 1 row written
(`batch_clock.clj:96`), and the wait for the ack; no message between tasks. A later attempt of the
batch pays 1 read (`:434`). A run with nothing to decide pays nothing and reads no clock, which is one
reading less than before this part (`:432`, `:446`). Counted from the dataflow. One number was seen in
this session's runs and is no measure: the first attempt called `base` 4 to 14 ms after the clock
that it was given (14, 4, 4 and 12 ms in four runs), which is the append, the ack and the read back
on one machine with one copy.

## Where the plan could not be met, and what was done

Each is small. None changes the design.

1. **`clock-record` given a thing that is no integer.**
   *The plan's words.* Function 2: "the record to append, `{:batch :clock}`, both as longs", with "The
   id is a number: a `Long` in Rama 1.6.0". And of the pure proof: "the cases of the three pure
   functions, each also given things that are no record and no row, since none may throw."
   *What I found.* Written with `long` alone, `clock-record` throws on a thing that is no number. The
   two lines cannot both hold.
   *What I built.* `clock-record` gives nil when the id or the reading is no integer that a long holds
   (`gate/batch.clj:409`). The topology writes nothing for nil, the read back finds no clock, and
   `clock-must-be-kept` fails the attempt. So the fault ends where every other fault of the clock
   ends, with no stamp made.
   *Why.* The namespace says of itself that none of its functions throws on what it is given, and the
   one throw of the module's code is to be one.

2. **The name of the batch's clock in the dataflow.**
   *The plan's words.* "`:286` | The same call, with the batch's clock"; "`:295-324` | The same call,
   with the batch's clock".
   *What I found.* The variable was `*now`. A clock that a second attempt takes from a row is not now.
   *What I built.* `*batch-clock`, in the two calls (`microbatch_gate.clj:447`, `:488`). No other
   line of the walk below the head of step 2 is changed.

3. **Where the first cluster's snapshot of the base is taken.**
   *The plan's words.* The first sentence of VA7 is "placed after the path of a lock in the base and
   before the module is started again".
   *What I found.* The group of the path of a lock took the snapshot that the group after the start
   compares with. A group between them that writes to the base makes that snapshot stale.
   *What I built.* The new group takes the snapshot, as its last step (`module_proof.clj:2306`); the
   group of the path of a lock keeps the request that the snapshot reads. One line of an earlier
   check was moved. None was removed, and the comparison is the same one, with the kept clock's row
   added to what is compared.

4. **"The checks that rest on a time" gains the edge act.**
   *The plan's words.* "`test-store/softland/store/README.md` ... 'The checks that rest on a time'
   gains the edge act, which changes what the check shows and never whether it passes."
   *What I found.* That heading is a part of the door's proof, and says of its checks "If one fails on
   a slow machine, read its numbers before the door." The edge act is of the module's proof and never
   fails by a time.
   *What I built.* A heading of its own under the module's proof, "What in the module's proof rests on
   a time", with the edge act, the limit of the wait and the quiet time.

5. **Explanations beyond the plan's list**, as the task asks ("wherever else you find one").
   `gate/batch.clj`, the namespace's second paragraph: "The gate only reads rows, calls these
   functions and writes rows". `gate/decision.clj`, `refused-early`: "Takes the gate's clock now".
   `record/tables.clj`, of `:closed-through`: "a read gives no other". The store's page, the table of
   the entries of 11 and 12 October: a row for entry 8, with Sid's words on the decision side and, on
   the other side, what the read build does with it, marked as a session's reading. Each was replaced.

6. **Checks beyond the plan's list.** Three, each of this part's own code, none a step of the process:
   the gate's one throw called by hand, since no run reaches it; on the first cluster, that the kept
   row is the first record, in the depot's order, of the batch with the greatest id; and that the
   check of the two attempts can fail, shown on the first attempt's own note with one stamp moved,
   with one fact left out and with a row that goes out left out.

## Redundant conditionals

**PASS.** The part adds two conditionals to the gate and one to the new topology.
`microbatch_gate.clj:432-446`: with numbers the batch's clock is found or kept; with none it is nil.
The two branches do different work. `:436-444`: a clock that was found is taken; else a reading is
appended, the row read again, and the clock taken from the row. Different work.
`batch_clock.clj:95-96`: a row is written or nothing is. No branch repeats another with one variable
changed.

## Consecutive keypath

**PASS.** Each new path holds one `keypath`: `(keypath *gates-route)` at `microbatch_gate.clj:434`,
`:442` and `batch_clock.clj:93`, `:96`.

## Select-compute-transform

**PASS, with the reason.** The topology reads the row, calls `clock-row-to-keep`, and writes the row
with `termval` (`batch_clock.clj:93-96`). That is the form which the template asks to replace "when
possible". It is not possible here: the rule's result for a record of the row's own batch, of an older
batch, and for a thing that is no record is to write nothing, and an aggregator always writes. A
`term` with a function that gives back the held row would write the same row again for each record
that Rama hands over twice, one write and one replication more each time, and would try to write nil
into an empty table for a thing that is no record. The read is one seek; the write is a `termval` at a
fixed key, which reads nothing. So the event is 1 seek and 0 or 1 write, as the plan counts it.

## Unnecessary nil->val

**PASS.** None is used.

## :allow-yield?

**PASS.** No new read walks a collection: each is one row at one top-level key.

## Non-subindexed collections without size limits

**PASS.** The new PState holds one row of two longs under one key (`record/tables.clj:181`). Nothing in
it grows.

## Stream topology idempotency

**PASS.** One stream topology is added, `batch-clock`. Its event for one record
(`batch_clock.clj:91-96`), traced when Rama runs it again:

- *The one write.* `termval` of a whole row at the fixed key `:microbatch`. On a second run for the
  same record the read at `:93` gives the row of that record's batch; `clock-row-to-keep`
  (`gate/batch.clj:442`) gives nil because the held batch is not older; the `<<if` at `:95` writes
  nothing. Held by the pure proof ("a record handed over twice changes nothing"), and on the first
  cluster by the kept row being the same after Rama's module update.
- *A record of a closed batch handed over late.* The held batch is later; nil; nothing written. Held
  by the pure proof.
- *Two records of one batch with two clocks.* The first that is processed is written; the second
  finds a row of its batch and writes nothing. Held by the pure proof in both orders.
- *An id made inside the topology.* None.
- *A depot append from the topology.* None. The template's question about a commit boundary before an
  append is about a stream topology that appends, which this one does not.
- *Can it throw.* `clock-row-to-keep` catches every throw and gives nil (`gate/batch.clj:467`). The
  write takes a row of two longs that the same function made, which is the schema's class
  (`record/tables.clj:194`).

The append is made by the microbatch gate (`microbatch_gate.clj:441`), and the skill says of such an
append that a retry appends again. Traced: a later attempt appends only when the read at `:434` finds
no clock of its batch, that is when the first attempt failed before its record was processed. The
second record then meets the rule above. So the depot can hold more than one record for a batch, and
the row holds one clock for it. Seen in the run: the batch that ran twice left one record.

## Partial failure in stream topologies

**PASS.** The event writes one PState on one task and has no partitioner, so it has no part that can
commit without the rest.

## Single depot append per client operation

**PASS.** No client operation is added or changed. The new depot is declared `:disallow`
(`module.clj:106`), so no client can append to it.

## Application-state caches survive restart

**PASS.** The part adds no task global and nothing in the memory of a process. The kept clock is a
PState and its log a depot, both durable. This is the point of the design: a restart of a worker is
one cause of a second attempt, and the batch probe saw a task global come back empty after one. I did
not reach for a kept copy of what the cluster holds in the memory of a process, in the module or in
the read of the kept clock.

The proof does keep such a thing, and its map says so: the notes of each attempt are the memory of the
proof's process, and they outlast the worker's restart only because the in-process cluster is one JVM.

## No reimplementation of built-in operations

**PASS.** The id of the batch is `ops/current-microbatch-id` (`microbatch_gate.clj:433`). The clock is
`System/currentTimeMillis`, as the gate read it before this part. `as-long` (`gate/batch.clj:402`) is
a test and a cast in pure Clojure, in a namespace that must load without Rama.

## Plan conformance

**PASS, with the six points above.** Each function of the plan's table, in its order.

| # | Planned | Built | Same? |
|---|---|---|---|
| 1 | `record.tables/batch-clock`, `{Keyword -> fixed-keys{:batch Long, :clock Long}}` | `record/tables.clj:181` | Yes |
| 2 | `gate.batch/clock-record` | `gate/batch.clj:409` | Yes; nil for a thing that is no integer (point 1) |
| 3 | `gate.batch/kept-clock`, never throws | `:425` | Yes |
| 4 | `gate.batch/clock-row-to-keep`, never throws | `:442` | Yes |
| 5 | `gate.batch/base`, docstring only | `:475` | Yes |
| 6 | `module.batch-clock/add-to`: the stream topology, its PState, the event of one record | `module/batch_clock.clj:81-96` | Yes: source from the beginning, Rama's default retry mode, no option set |
| 7 | `module.microbatch-gate/clock-must-be-kept`, the one throw, with what it guards and costs | `module/microbatch_gate.clj:322` | Yes |
| 8 | `add-to`, step 2 by the plan's table | `:432-447`, `:488` | Yes, line for line: below |
| 9 | `Store` declares `*batch-clock-depot` with `:disallow` and calls `batch-clock/add-to` before the microbatch gate's | `module.clj:106`, `:108` | Yes |
| 10 to 13a | The proof | `test-store/softland/store/module_proof.clj` | `TEST_VALIDATION-base-for-a-reader.md` |
| 14 | The pure proof's cases | `test-store/softland/store/pure_proof.clj` | The same paper |

The head of step 2 against the plan's lines (i) to (v).

| The plan | The code | Trace |
|---|---|---|
| The route of the gate's task; go there; read `:last-stamp`, `:closed-through`, the numbers | `:424-428`, not changed | On the gate's task from `:425` to `:453` |
| Only when there are numbers | `:432` | `*numbers` is nil for a batch that took no thing or only strays; the same test gives a batch its base (`gate/batch.clj`, `base`) |
| (i) the microbatch id | `:433` | A `Long` in Rama 1.6.0 |
| (ii) read the row under the gate's route | `:434` | A PState of another topology on this task: its committed value |
| (iii) `kept-clock` of the row and the id | `:435` | The row's clock when the row is of this batch, else nil |
| (iv) when it gave a clock, that is the batch's clock; no clock is read | `:436-437` | Run: the second attempt was given the first attempt's clock, and the depot gained no record |
| (v) else read the clock, make the record, append with `:ack`, read the row again, `kept-clock` again, `clock-must-be-kept` | `:439-444` | Run: the clock that `base` was given is the clock of the kept row and of the record. The clock is taken from the row at `:443`, never from `*reading` |
| With no numbers the clock is nil; nothing is read or appended | `:445-446` | Run: a quiet 1.5 s added no record |
| `base` with the batch's clock | `:447` | The first function that makes a stamp, after the ack |
| `replies` with the batch's clock | `:488` | The one other use of the clock |

*Is anything of this batch able to make a stamp from a clock that is not kept?* Derived from `:432-447`.
`*batch-clock` is bound in three places. At `:437` it is a clock that `kept-clock` took from a row of
this batch. At `:444` it is what `clock-must-be-kept` gives, which is the same, or the attempt has
failed. At `:446` it is nil, and `base` gives nil for no numbers before it looks at the clock, so no
stamp is made. No other line of the file reads a clock: a search finds `System/currentTimeMillis`
once, at `:439`.

*Is the append on the partition that the read back reads?* Derived. `:425` goes to the gate's task and
nothing leaves it before `:453`. `depot-partition-append!` appends to the partition of the task that
the code is on. The topology's event has no partitioner (`batch_clock.clj:91-96`), so it writes on
that task, under the same key. Run: 76 records on partition 0 and none elsewhere.

*Does the wait let anything write what step 2 read before it?* Derived, as the plan's check traced it:
between `:441` and `:442` the batch has one event, and it waits; `:last-stamp` and `:closed-through`
are written at `:451` and `:455`, after the wait; `$$questions` is whole. What the proof adds: 76
batches on the first cluster and 6 attempts on the second decided as before, with the stamps of the
gate's answers going up in the order decided.

**Against the contract and the rulings, not the plan alone.**

| Asked | Where | How it is met |
|---|---|---|
| A7, first sentence, as ruling 16 (1) reads it: a read shows no fact that a second try could change | The contract; ruling 16 | Run: each fact's row of the second attempt is the first attempt's, in every field. What no run shows is said above |
| A7, second sentence: a place that holds a fact never gives nothing | The contract | A place holds one row, its current fact; the write is one `termval`. Run: 297 selects across a replace, each a row |
| "It is tried first, with a run" | The contract, A7 | The run is the module's proof's last group |
| Ruling 17 (a) | `PROGRESS.md` | `:447` is after `:441-444`; only with numbers (`:432`). Run, above |
| Ruling 17 (b) | `PROGRESS.md` | The record is `{:batch :clock}` and nothing else (`gate/batch.clj:409`). Run. The door's three reads are not changed |
| Ruling 17 (d) | `PROGRESS.md` | The run holds |
| Ruling 34: the clock's depot takes no act; the depots of acts stay two | `PROGRESS.md` | `module.clj:104-106`; no gate sources it; the store's page, row F6 A, says so on its "Where it shows" side |
| Ruling 35 | `PROGRESS.md` | Rows F6 A and F2 A and the new row 8 keep Sid's words on the decision side. `git diff src-store/README.md` is the tree's first paragraph alone |
| Rulings 39 and 58 | `PROGRESS.md` | Each head comment of a file that this part changed gains this plan: `module.clj`, `module/microbatch_gate.clj`, `module/streaming_gate.clj`, `module/door_reads.clj`. The new file `module/batch_clock.clj` begins with it |
| Rulings 46, 47, 50, 51 | `PROGRESS.md` | A topology of its own; one row; no trimming option; no phase timeout set |
| Ruling 59 | `PROGRESS.md` | The one throw, with its docstring: what it guards, what it costs, when it is reached (`module/microbatch_gate.clj:322-357`) |
| `src-store/CLAUDE.md` | The tree's rules | Each new namespace and function, the private one too, has a docstring; the maps of `module/`, `gate/`, `record/` and the tree hold no list of functions; untrue lines are replaced; what was run is in `test-store/` |
| The alias `:store` holds Clojure alone | The task | `gate/batch.clj` requires nothing new. Run: the pure proof and the act's proof pass on that alias |

## Lint

`clj-kondo` (the root `deps.edn` holds it) over the pure files that this part changed and the two
proofs that it changed: 0 errors, 7 warnings (`runs/raw-bfar-lint.txt`). Each warning is "Unresolved
var" of a var that the build before this one added (`act/allows`, `declaration/second-write-keys`,
`declaration/max-builds-on`, `declaration/max-marked-keys`), which a lint of a few files cannot
resolve. The same warnings stand on the same lines of those files at the last commit. None is in a
line of this part. The files of dataflow were not linted: the build before this one found that
`clj-kondo` cannot read Rama's dataflow.

## Self-consistency

Read again for a "gap" or a "tradeoff" that a PASS above contradicts. Two things are said plainly in
this paper and are not contradicted by a PASS, because no check of the template is about them: the
gate's throw was never reached inside the gate by a run; and the first attempt's rows were never in a
table, so the run sets side by side what each attempt made and not what a reader saw. Ruling 17 (d)
asks for the first of those two things, and rulings 17 (e) and 48 leave the second to the batch probe.

## Points for the build session

1. Ruling 17 (d): the run holds; the stop is not met. The line for `PROGRESS.md`: two attempts of one
   batch noted, one clock, and no key from `rows-side-by-side`.
2. `clock-record` gives nil for a thing that is no integer, where the plan has it only cast (point 1
   above). Strike it if a throw there is wanted.
3. The dataflow's variable for the batch's clock is `*batch-clock`, not `*now` (point 2).
4. One line of the check of the path of a lock in the base moved to the new group (point 3).
5. `door.clj:41` and `:696` say "the two depots and the three queries of the module". The module now
   declares three depots. In those sentences the two are the depots that the door appends to, so they
   stay true as read; I did not edit the door's file. If the sentence should say "the two depots of
   acts", it is two phrases. The read path's session has since changed other sentences of that file's
   docstrings; those two stand as they were.
6. The store's page has a row for entry 8 in the table of 11 and 12 October. Its decision side is the
   contract's wording of his decision. Its other side says what the read build does, as a session's
   reading that he did not strike. Strike the row if ruling 35 wants no row for an entry whose two
   consequences are a session's.
7. For the receipt, beside the lines that the plan lists: the module update that brings this part into
   a cluster that already runs the store was not run; no cluster runs the store today.
8. Scratch that this session left under `reads/runs/` and that is not for a commit unless wanted:
   `raw-bfar-load.txt`, `raw-bfar-lint.txt`, `raw-bfar-pure-1.txt`, `raw-bfar-module-1.txt`,
   `raw-bfar-mutation.txt`, and for each proof `raw-bfar-<proof>-final.txt` and `.exit`. The four
   files `after-base-for-a-reader-<proof>.txt` are the cut outputs that the task asks for.
9. The read path's session has files of its own in the working tree (`read_path.clj`, `read_path/`,
   three files under `test-store/`). I touched none. Near the end of this session it edited files
   that this part also edits: `record/rows.clj`, `module.clj`, `module/door_reads.clj`, and the maps
   `src-store/README.md`, `src-store/softland/store/README.md`, `record/README.md` and
   `test-store/README.md`. I read each afterwards for this part's lines: all stand. It also changed
   files that this part does not edit, among them `door.clj`, `door/README.md` and the two maps named
   `softland/README.md`; `git status` lists them beside this part's, and none of those is mine. Its
   last change of a file that the four proofs load was at 07:48:56 on 12 October, and this session's
   last runs of the four began at 07:49:11; its measuring run, `read_measures.clj`, changed after,
   and no proof loads it. In
   `test-store/README.md` it wrote this part's sentence on the two clusters again, for the third and
   the sixth proof together; that is right, see the next point. A commit of this part by path takes
   those files whole, with that session's lines in them.
10. The read path's proof makes a batch of the base run a second time too, on a second cluster of its
    own, by the same three vars of `softland.store.gate.batch` (read in
    `test-store/softland/store/read_path_proof.clj`, its lines 51 and 398 to 407; not run by me). Two
    things follow. Two proofs now hold that technique, each in its own JVM, so they cannot meet; when
    what `decide-or-send` or `rows-to-send` gives changes, both change. And its check, "the row that
    the second attempt made for the place is the first attempt's row, in every field, the stamp
    too", rests on this part's kept clock: when that proof passes, it is a second run of what ruling
    17 (d) asks, by other code. I do not know whether it passes.

## Verdict

**pass.** Every check above passed after a trace, and the four proofs pass with this code in. The
last runs, each from the repository root, on the tree as this session leaves it:

| Proof | Count line | Exit |
|---|---|---|
| `clojure -M:store -m softland.store.act-proof` | `{:test 8, :pass 160, :fail 0, :error 0}` | 0 |
| `clojure -M:store -m softland.store.pure-proof` | `{:test 18, :pass 686, :fail 0, :error 0}` | 0 |
| `clojure -M:store-module -m softland.store.module-proof` | `{:test 33, :pass 354, :fail 0, :error 0}` | 0 |
| `clojure -M:store-module -m softland.store.door-proof` | `{:test 27, :pass 199, :fail 0, :error 0}` | 0 |

Before this part: act 8 and 160, pure 17 and 671, module 31 and 333, door 27 and 199.
