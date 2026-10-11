# Plan: the base, for a reader — each batch's clock kept across its attempts

**What this paper is.** The plan of the part `base-for-a-reader` of the read build: the contract's A7
(`../BUILD-reads.md`), as rulings 16 and 17 of `PROGRESS.md` read it. It designs how the microbatch gate
keeps the clock of each batch, so that a second attempt of a batch makes each fact's row and each
answer's stamp and verdict again as the first made them. It writes no code of the store.
**The stage.** Phase 1 of the Rama skill (`phase-1-plan.md`), for one part of three
(`DECOMPOSITION.json`, the entry `base-for-a-reader`).
**Who wrote it.** One fresh session (Opus 5.5), in the night of 11 to 12 October 2026, for the build
session to judge and a fresh session to check. It wrote this file and the folder
`plan-probes-base-for-a-reader/`, and changed no other file. The tree was at head `a8c1783b`;
`src-store/` and `test-store/` are there as at `c3e65c06`, so the line numbers below are the split's.

**Blocked.** Nothing.

**Changed by the check of this plan** (`PLAN_VALIDATION-base-for-a-reader.md`, 12 October 2026, a fresh
session). Ruling 49 is applied: the check of the second attempt has a cluster of its own. Nine faults
are repaired in place, and one citation is corrected. That paper lists each change with the plan's lines before and after. "T" below
is the check's run, `plan-check-probes-base-for-a-reader/`, with its log `logs/two-clusters.log`.

**Ruling 53, which came while this plan was checked: this part is built second.** `what-the-gates-keep`
is built before it. Three things follow, and the text below is to be read with them.
1. The line numbers of this plan are of head `c3e65c06` and will not hold when the part is built. The
   build finds each place by the words that are quoted here. The change of the walk is the same one,
   the head of step 2: that part's plan changes no line of it ("The microbatch gate's four steps, line
   by line", its first paragraph), and the file holds one reading of the clock.
2. The batch of the proof holds the rows of both parts. N15 of that plan's proofs lists what the batch
   gains and what must be sent before it, and this part's build makes it: "The batch of the check".
3. Each explanation that this plan replaces is replaced as it stands after that build, found by the
   words under "Says today". Where that build has written a sentence again, this part's change is made
   to the sentence as it then is.

**A ruling that cannot hold.** None. One ruling is read, and the reading is said here so that the build
session can strike it: ruling 17 (b) says "Only the clock goes on that depot." A record of that depot
holds two numbers, the batch's microbatch id and its clock. The id is what makes the number the clock
*of a batch*. So (b) is read as: nothing of an act and no lock's bytes.

**How the skill's template is adapted.** The template plans a module. This part changes one walk of a
module that stands, and adds one depot, one topology and one table to it. Every heading of the template
is kept and answered for what the part adds or changes. "Reads" are the reads that the part touches: the
gate's own read of the kept clock, and what a read of a fact may rest on. "Writes" is the one new append
and the changed step. Seven sections are added, because the task and the split ask for them: why the
attempts come out the same, the scenarios by hand, what the part hands on, the proof and the stop test,
the functions in the order to build, the words and numbers, and each explanation that the part makes
untrue. "Open points" are for the build session; none is Sid's to decide here.

**Marks.** "Read at" is read in the source at that line. "Run" names the run and its log. "Read, not
run" is a line of Rama's pages under `reference/rama/docs/`, an extract of March 2026 that names Rama
1.5.0, where the store runs 1.6.0. Of those pages this session opened `11-stream-topologies.md` and `14-depots.md` at the lines given; a
line of `12-microbatch-topologies.md`, `23-acid-semantics.md` or `25-integrating.md` is given as the
batch probe's "B1, by the lines" quotes it. "Derived" is reasoned here from lines that are named. A file name
without a folder is under `src-store/softland/store/`, as in the implied spec. "The batch probe" is
`batch-probe/RESULTS.md` with its B numbers. "W" and "S" are this plan's two runs, below.

## What was run for this plan

Two probes, in `plan-probes-base-for-a-reader/`, W and S. Each is a probe: its code is no code of the store. Both
on Rama 1.6.0, in-process, one machine, launched as the store's proofs launch their cluster: 4 tasks, 2
threads, 2 workers. Run again: W from that folder with `clojure -M:walk`; S from the repository root
with the command at the end of its `deps.edn`. One at a time: two in-process clusters collide on a port.

**W, `src/probe/walk.clj`: the changed step 2 on a stand-in module.** A walk of four batch blocks
shaped as the store's: things gather a number on the gate's task; the gate's task takes the clock; every
task replies and writes a row with the clock; a late point can throw. Two logs. `logs/walk.log`: the
route of the gate's task is `:microbatch`, as in the store; with 4 tasks that is task 0, the task where
a batch block starts. `logs/walk-other-task.log`, by `clojure -M:walk other-task`: the route is a key
of task 2, so the hash partitioner of step 2 does change the task. The line numbers below are of
`walk.log` unless the other log is named.

| # | Asked | Found |
|---|---|---|
| W1 | Does a depot append with `:ack` work where this plan puts it: in a batch block, after a hash partitioner, inside two conditionals, before the block goes to every task and gathers into a temporary PState? | Yes, on task 0 and on task 2. In each log: 25 batches with things, 25 records on the depot, all on the partition of the gate's task (`{0 25}`; `{2 25}` in the other log), each read back in the same attempt (each `:appended` line; the last RESULT line). |
| W2 | Of which class is the microbatch id? | `java.lang.Long` (each `:read-the-clock` line). |
| W3 | One row for the newest batch, under one key: is it one select, and does a second attempt read the first attempt's clock? | Yes. Batch 43: attempt 1 read the clock, appended, wrote its rows, and threw at the late point; attempt 2 found the kept clock and read no clock; the four rows carry the first attempt's clock (lines 14 to 26). The stream topology was handed the record a second time and wrote nothing (line 23). |
| W4 | A run that got nothing | 6 runs of step 2 in 3 s: no clock read, no read of the kept row, 2 records on the depot before and 2 after (line 12). |
| W5 | The stream topology fails the record on its first try (the batch probe's "Not settled") | In each run it threw, its worker was started again, Rama handed it the record again 235 to 267 ms later, and it kept it. The second attempt of the batch found that clock, the first attempt's, and read none itself. One record on the depot for the batch. **How soon the second attempt came differs.** With the gate's task on task 0: 1.4 s after the first (lines 27 to 38). With the gate's task on task 2, on the other worker: 31.4 s after, and Rama's own lines hold "Microbatch processing phase timed out" (`walk-other-task.log`, lines 26 to 37; `logs/walk-other-task-rama-lines.log`). So there the attempt that appended was not failed by the throw: it waited for its ack until Rama's phase timeout, which the module does not set. The other order, a second attempt that reads before the record is kept, was not seen; it is traced below. |
| W6 | Across a module update | The kept row was the same before and after. The first batch after it had the id 56, after 45 before it: the ids went on (lines 39 to 50). The batch probe's `logs/redef.log` shows the same: batch 8 before `update-module!`, batch 20 after. |
| cost | The append with `:ack` and the read back, inside the attempt | 20 batches: least 4.1 ms, middle 5.2, most 29.2. In the first run of W: 4.1, 4.7, 6.3 (`logs/walk-run-1.log`). On task 2: 4.2, 5.0, 12.2. The batch probe measured about 3 ms with a thread for each task and one worker (B3.ii cost). |

The first run of W threw in a run that got nothing, which shows nothing about a kept clock; its log is
kept as `logs/walk-run-1.log`, the throw was held to a batch with things, and the run was made again.

**S, `src/probe/store_second_attempt.clj`, log `logs/store-second-attempt.log`: a second attempt of a
real batch of the store's own module, as it stands today.** No file under `src-store/` is changed: the
store's tree is on the classpath by the alias `:store-module`.

| # | Asked | Found |
|---|---|---|
| S1 | Does a function of `gate/batch.clj` that the gate's dataflow calls, replaced in its var before the launch by a function that a var holds (B5.a), run inside the store's module? | Yes, for `base`, `decide-or-send` and `rows-to-send`. |
| S2 | Does a throw from `rows-to-send`, in step 4, make Rama run the batch again, with two workers? | Yes. Two attempts, 1,088 ms apart. All 8 things of the batch were answered 2.1 s after the topology went on: 7 accepted, 1 refused. |
| S3 | Can a check learn what the first attempt made, without a table? | Yes. The three replaced functions call the first function and note what it gave. Each attempt noted 15 rows, 7 facts and 8 answers, under the same 15 keys. That is every fact row and every answer row of the 8 things. |
| S4 | Today, in which fields do the two attempts' rows differ? | 15 of 15 rows differ, each in `:stamp` and in no other field. The 6 fact rows that were selected from the table after the batch are the second attempt's. |

S settles the first risk of the split ("whether the proof can make a real batch of the store's own
microbatch gate run a second time with no hook in the module's source"), and S4 is the other side of
the stop test: the check sees a difference when there is one, and today the clock is the one thing that
moves a row. That is the implied spec's table "What would have to be the same on a second attempt",
run.

## The design in short

```
step 2 of a batch, on the gate's task                      the batch clock's topology, same task
  the batch's numbers ── none ──▶ no clock, no stamp
        │ some
        ▼
  read $$batch-clock ── a row of this batch ──▶ its clock
        │ none, or of an older batch
        ▼
  read the clock of the gate's task
  append {batch, clock} to *batch-clock-depot, :ack ─────▶ holds no row, or one of an older batch:
        │   (waits until the row is durable)                 write {batch, clock}
        ▼                                                  holds a row of this batch, or of a later
  read $$batch-clock again ── a row of this batch ──▶ its clock     one: write nothing
        │ else: fail the attempt
        ▼
  the base, the stamps and the 10-second rule of the batch, from that clock alone
```

1. One more depot, `*batch-clock-depot`, that no client may append to. A record is
   `{:batch <microbatch id> :clock <milliseconds>}`. Only step 2 of the microbatch gate appends to it,
   on the gate's task, and only in a run that has things to decide.
2. One more stream topology, `batch-clock`, with one PState, `$$batch-clock`: one row, the clock of the
   newest batch that kept one. A record of that batch or of an older one writes nothing. So the clock
   of a batch is written once, however often Rama hands the topology a record.
3. Step 2 takes the batch's clock from that row and from nowhere else. It reads the clock of the task
   only to offer a reading, appends it with `:ack`, and then uses what the row holds, not what it
   offered. An attempt that cannot read a row of its batch back makes no stamp and fails.
4. Everything after takes the clock as today: the base from it, each stamp from the base and the turn,
   the 10-second rule from the reply about the act's answer.
5. The door's three reads and the streaming gate's question stay held to closing, and
   `:closed-through` stays (rulings 16 (2), 16 (5), 17 (b)).

## Reads

| Read | Who | How | Cost | Changed? |
|---|---|---|---|---|
| The kept clock of the batch | Step 2, on the gate's task | `local-select>` of the one row of `$$batch-clock`, a PState of another topology on the same task, so its committed value | 1 seek. Once in an attempt that finds it; twice in the attempt that keeps it | New |
| The row before a record is kept | The topology `batch-clock`, on the same task | `local-select>` of the same row | 1 seek for each record | New |
| A fact's row of a microbatch layer, straight from `$$microbatch-facts` | The proofs today; the read path after the part `read-path` | A select or a push with a partitioning key (implied spec F3) | 1 seek, as today | The read is not changed. What it may rest on is: "What this part hands on" |
| `answer-by-act-id`, `locks-of-request`, `depot-of-layer` | The door | As built, held to closing | As built | No |
| The base's depot row and `:closed-through`, in the streaming gate's event | The streaming gate | As built, held to closing | As built | No |
| The kept row and the depot's log, from outside | The proof, an operator | `foreign-select-one` of the row with `:microbatch` as the partitioning key; `foreign-depot-read` | 1 round trip each | New. No code of `src-store/` makes them |

No read needs a query topology: each is one select of one row on one task (the template's step 1,
first question).

## Writes

| Write | Where | Form |
|---|---|---|
| A batch's clock, offered | Step 2 of the microbatch gate, on the gate's task: `depot-partition-append!` to `*batch-clock-depot` with `:ack` | `{:batch Long :clock Long}` |
| The kept row | The topology `batch-clock`, on the same task | The whole row at one fixed key, `termval`: the same write however often it runs |

No act, no fact and no lock's bytes go on the new depot. No client appends to it: it is declared
`:disallow`.

## PState design

**`$$batch-clock`: `{Keyword -> fixed-keys{:batch Long, :clock Long}}`**, declared on the topology
`batch-clock`. One key, the depot word of the gate whose clock it is, `:microbatch`
(`batch/gates-depot`). The value under a top-level key is one entry on disk, as each row of the record
is (`tables.clj:14-19`), so the read is one seek and the write one put. No `Object`, no collection in a
row, each field of one class (`tables.clj:57-60`). W3 ran a table of this schema.

Why a PState, and of another topology. The value must be durable, must not be set back when an attempt
of the batch fails, and must be readable by the next attempt on the same task. A PState of the
microbatch gate is set back (B3.i). A task global came back empty after the worker's restart (B3.iii a).
A PState of another topology of the module, written through a depot, held (B3.ii; W3, W5).

Three schemas were weighed.

| | A. One row: the newest batch (chosen) | B. A row for each batch, for ever: `{Long -> Long}` | C. Two fields more in `$$streaming-task` |
|---|---|---|---|
| The gate's read | 1 seek | 1 seek | 1 or 2 seeks |
| The stream event | 1 seek, 0 or 1 write | 1 seek, 0 or 1 write | the same |
| What grows | Nothing: one row | One entry for each batch with things, about 16 bytes of key and value and the store's overhead: 86,400 a day at one batch a second, on one task | Nothing |
| "First write wins" rests on | The id of a batch goes up and never comes again | Nothing more | As A |
| What it is | The gate's working state | A second copy of what the depot already holds | The microbatch gate's working state inside the streaming gate's table |

Chosen: A. The reason is what the row is for. A kept clock has one use: a later attempt of the same
batch. A batch can be attempted again only until the next batch begins: "The topology does not start the
next microbatch until ALL tasks have committed successfully" (`microbatch.md`, "Guarantees"; run: the
batch probe's S2, where Rama began no run before the held task had committed). So when a record of a
later batch arrives, the row of the batch before can never be read again. It is working state, as
`:turns` and `:closed-through` are, and not a record: no read of the contract asks for a past batch's
clock, and the stamp of every row of that batch already holds it. The skill's rule "Never delete data
unless the spec explicitly requires it" is about data that a read depends on; nothing reads the row
before. And nothing is lost with it: the depot keeps every record (see "Depots"). B would keep a second,
growing copy of the depot on the gate's task for no read. C is the form that follows the skill's rule
against a second PState of one key and partitioner; it goes with the choice of the topology, below, and
is costed there.

What A rests on, and what was run for it. The id is the same for each attempt of a batch, and goes up
by one with each run, a run that got nothing too (B2.3, B6.2; read, not run:
`25-integrating.md:336`). It goes on across a worker's restart (B2.3; W3, W5) and across a module update
(W6; `batch-probe/logs/redef.log`, batch 8 before the update and 20 after). It is a `Long` (W2). What
would make an id come again: a microbatch topology that is deleted and declared again under its name.
Nothing of this build does that. If it happened, the row would be of a "later" batch for ever, no
attempt could keep a clock, and the gate would stop at once and loudly (see step 2, the last line). The
docstring of the table says so.

## Depots

**`*batch-clock-depot`: `:disallow`.** Declared in `module.clj` beside the two depots of acts. It is
partitioned as every depot is, and only one partition ever holds a record: `depot-partition-append!`
appends to the partition of the task that the code is on, and step 2 is on the gate's task there. A
global depot would have its one partition on task 0, and the gate's task is the task of the hash of
`:microbatch`: the append would then cost two hops, there and back, in every batch with things. The
empty partitions cost what an empty depot partition costs; the skill gives no figure.

Why a depot at all (the validation list, "Internal depot usage"). It carries a value that is on no
client's depot, a reading of the clock. And a depot is the one way by which a microbatch topology can
hand a value to state that Rama does not set back: only the topology that owns a PState writes it.

What it holds, and for how long. One record for each batch that had things to decide, and one more for
each attempt that found no clock kept (after a failure before the keep; in the race of W5). The module
sets no trimming option on it, as on the two depots of acts (`module.clj:33-35`), so Rama keeps every
record. Whether a log of the store is ever trimmed is Sid's, on the sheet that comes after this build
(entry 9, Z3: "forever never trim", which is no decision yet). So the depot is, as it falls out, the
clock of every batch that had things, by the id of the run that decided it. That is a first part of
what rule 9 left to Sid, and no more: it holds no cut of a batch, and a rebuild cuts other batches
(ruling 16 (6); the store's page, `:454-461`).

Ruling 34: the depot takes no act, and no gate decides from it. The two depots of acts stay two.

## Topologies and PStates

### `batch-clock`: stream

- **Why stream.** Reason (b) of the template: ack coordination. The attempt must not go on until the
  clock is durable in a place that a later attempt reads. `:ack` waits for colocated stream topologies
  and for nothing else (`14-depots.md:206`). With `:append-ack` and with no ack the same attempt read
  nothing back, 0 of 82 times (B3.ii).
- **Its one concern**: to keep the clock of the newest batch. It needs the stream type: without it
  there is no ack to wait for.
- **Its source**: `*batch-clock-depot`, from the beginning, as each gate's source starts (the store's
  page, `:332-334`). With the rule below, reading old records again writes nothing. Rama's default retry
  mode, a failed record alone: the order of two records does not matter to the rule.
- **Its event, for one record**, on the task of the depot's partition, with no partitioner:
  1. read the row under `:microbatch`;
  2. `clock-row-to-keep` of the row and the record: the row to write, or nil;
  3. when it is a row, write it whole at that key.
- **Each write, and whether it is the same when the event runs again.** One write, `termval` of a whole
  row at a fixed key. A second run of the event for the same record finds a row of that batch and writes
  nothing. Rama did hand the topology a record twice: B4, and W3 line 23. The topology appends to no
  depot, so the list's rule on a commit boundary before a depot append does not apply.
- **What the rule gives, by case.** The row is of an older batch, or there is none: the record's clock
  is written. The row is of the record's batch: nothing is written, so the first clock that was kept for
  a batch stays. The row is of a later batch: nothing is written, so a record of a closed batch that
  Rama hands over late (a retry, or the records after the topology's last checkpoint at a restart)
  never puts the row back. A record that is no map of two integers: nothing is written; the function
  never throws, since a record that throws is tried for ever.
- **PStates:** `$$batch-clock`, above.

**A second stream topology, and what forces it.** The template says at most one, and the module has
one, `streaming-gate`. Nothing in Rama forces a second: the same event could be a second source of
`streaming-gate`, and the row two fields of `$$streaming-task` (schema C). The store-module's plan
costed that form and did not run it ("the batch's clock kept by the streaming gate ... a second source
and one more PState in the streaming gate ... *not verified*", `PLAN-microbatch-gate.md:674`). The two
forms, side by side:

| | Its own topology (chosen) | A second source of `streaming-gate` |
|---|---|---|
| Reads and writes for a batch with things | 3 seeks, 1 append, 1 row write | The same |
| What the `:ack` waits for | A streaming batch that holds this one record: 1 seek, 1 small write, one replication | A streaming batch of `streaming-gate` on the gate's task: every act of every streaming layer of that task that is queued, at 1.3 to 2.1 ms of seeks for an act (4 reads; the store's page, `:504-507`), and the replication of all their rows together. With 10 acts queued: 13 to 21 ms more inside the attempt |
| What fails the keep | A failure of this event | A failure of any event of that streaming batch |
| Which settings govern it | Its own | Those of the streaming gate. `topology.stream.max.executing.per.task` at 1 is a choice that the store's page leaves to Sid for the acts (`:155-164`); it would then also put each batch's clock in line behind the acts of that task, one replication each |
| What the microbatch gate waits on, in every batch with things | A queue that holds clocks only | The streaming gate's queue on one task. A streaming layer that is stuck there (a record that keeps failing, with the retry mode "all after") would stop the base too |
| Who can write the row | This topology's one event | The streaming gate's dataflow as a whole |
| Fixed cost | One PState and the topology's own progress state, a partition of each on every task, all but one empty | None: no new PState, no new topology |
| Run | B3.ii, B4, B7.3; W | Not run |

The left side rests on a line of Rama's page, read and not run: events are batched "for that stream
topology", and "if any one of those events throws an exception all the events will fail"
(`11-stream-topologies.md:138`, `:168`). So the batch is the unit of the commit and of the failure, and
it is one topology's. The choice is the left side, for the fifth row above all: today neither gate
waits on the other, each reads one row of the other and goes on, and the right side would make every
batch of the base wait on the streaming gate's topology. The cost of a second topology that the
template names, that a PState can then be written by one topology only, is here what is wanted. What
the left side costs is the fixed-cost row; the skill gives no figure for an empty partition. This is
open point 1.

### `microbatch-gate`: microbatch, as built. Step 2, line by line

The part changes the head of step 2, `microbatch_gate.clj:284-286`, and no other line of the walk.
Ruling 38: every other change of the walk is the part `what-the-gates-keep`'s.

| Line today | Today | After |
|---|---|---|
| `:280-281` | The route of the gate's task; go there | The same |
| `:282-283` | Read `:last-stamp` and `:closed-through` of `$$microbatch-task` | The same |
| `:284` | Read the batch's numbers from `$$questions`: nil for a batch that took no thing, or only strays | The same |
| `:285` | Read the clock of the task, in every run | Gone from here. In its place, only when there are numbers: |
| new | | (i) the microbatch id of the run (`ops/current-microbatch-id`; step 1 has it in its own block) |
| new | | (ii) read the row of `$$batch-clock` under the gate's route |
| new | | (iii) `kept-clock` of that row and the id: the row's clock when the row is of this batch, else nil |
| new | | (iv) when it gave a clock: that is the batch's clock. Nothing more is read, and no clock of the task |
| new | | (v) else: read the clock of the task (`System/currentTimeMillis`, the one reading); `clock-record` of the id and the reading; append it to `*batch-clock-depot` with `:ack`; read the row again; `kept-clock` again; `clock-must-be-kept`: the clock that the row holds, or a throw when the row is not of this batch |
| new | | With no numbers: the batch's clock is nil. No row is read, no clock is read, nothing is appended |
| `:286` | `batch/base` of the clock, the last stamp and the numbers | The same call, with the batch's clock. `base` gives nil for no numbers before it looks at the clock (`batch.clj:332`) |
| `:287-290` | With a base: write the new `:last-stamp` | The same |
| `:291-294` | Whether the tasks write `:closed-through`; go to every task; write it | The same |
| `:295-324` | Each home task reads what was asked and replies; `batch/replies` takes the clock and the base (`:320`) | The same call, with the batch's clock |

Each claim that the task asks for, against that table.

- **Where the clock is read.** In line (v) alone: on the gate's task, in the first attempt of a batch
  with things that finds no clock kept. A later attempt reads none (W3: one clock was read in the
  batch).
- **Where it is kept.** In the same line, by the append and the topology `batch-clock`, before line
  `:286`.
- **Where it is read back.** In the same line, after the ack; and in line (ii) of every later attempt.
  The attempt uses what the row holds and never what it offered. That matters once: in the race traced
  under W5 below, the row can hold the reading of an earlier attempt.
- **Kept before any stamp is made from it.** `batch/base` at `:286` is the first function that makes a
  stamp, and it is after the ack. The ack comes when the depot's record and the row are durable on the
  leader and on each in-sync follower (read, not run: `14-depots.md:206`, `23-acid-semantics.md:112`).
  Run on one copy: the same attempt read the row back 162 of 162 times (B3.ii) and 25 of 25 (W).
- **Kept before any task can show a row stamped from it.** A task shows a batch's rows at its commit,
  and no task commits before the attempt's last step is done on every task (`microbatch.md`,
  "Guarantees", the phases). Step 2 is before step 3.
- **Only in a run that has things to decide** (ruling 17 (a)). "Things to decide" is "the batch has
  numbers": each thing that is no stray sends one (`batch.clj:297`, `:302`, `:312`), and a stray sends
  nothing (`microbatch_gate.clj:252-257`). The same test gives a batch its base today. Run: W4.
- **A batch with no numbers has no use for a clock.** The one other use is `batch/replies` at `:320`.
  Replies are made for reads, reads for questions, and a question comes only with an entry and its
  numbers (`batch.clj:298-312`). So with no numbers the loop of `:297-321` runs no time. Derived.
- **What every later step takes the clock from.** As today, and nothing is changed for it. The base goes
  to every task with the clock (`:292`). The reply about an act's answer carries both to the task that
  decides it (`batch.clj:391-400`). Step 3 holds the 10-second rule against `:clock` of the item
  (`batch.clj:615`, `:634`, `:685`) and makes the stamp from `:base` and the turn (`:621`, `:682`,
  `:689`). An act that meets takes both to the meeting task in its parcel (`:693-703`). No later step
  reads a clock.
- **The suspension.** The append with `:ack` gives the task back while it waits (`dataflow.md`,
  "Yielding"). What can run on the gate's task meanwhile: a read of committed rows, an event of the
  streaming gate, and the event of `batch-clock`, which must. No event of this batch can: step 1 is
  done on every task, and step 2 has not yet gone to every task. The three values that step 2 read
  before the wait are not written by anything in between: `:last-stamp` and `:closed-through` of the
  gate's task are written by this step, after the wait, and `$$questions` is whole. So the docstring's
  paragraph "Yielding" gains one sentence and stays true.
- **The throw of line (v).** It is the one place where the module's own code throws, and it is not on
  what arrived: it guards the state of the cluster. After an ack that came back, the row is of this
  batch in every case that is traced below. If it is not, something that this plan rests on is false,
  and a stamp from a clock that is not kept is the one thing that must not be made. A throw fails the
  attempt, and Rama runs the batch again.
  What a throw costs, said whole. In the in-process cluster Rama treats a throw on a task's thread as
  fatal for the worker and starts the worker again (B2.4). Every task of that worker stops for that
  time, the streaming gate's and the reads' too, and a read from outside throws for about half a second
  (the batch probe, "Seen on the way"). Whether a worker of a real cluster ends its process there was
  not run. And while the cause stays, the batch throws on each attempt and Rama runs it again for ever
  (`microbatch.md`, "Guarantees"): the base stands still, and the worker goes down and up, 1 to 15 s
  apart in T. Two causes are known, and neither was seen in a run: an id of a batch that comes again
  ("PState design"), and an append whose ack returns although the row was not written. The docstring
  of `clock-must-be-kept` says all of this.
- **How the names reach the gate's file.** The dataflow of `add-to` names `*batch-clock-depot` and
  `$$batch-clock` as it names `*microbatch-depot` (`:236`) and `$$streaming-facts` (`:383`) today: the
  depot is declared in `module.clj`, the PState by another file's `add-to`. W ran the append inside one
  `defmodule`; that the same holds from a function of another namespace is read from those two lines,
  not run. The first launch of the build shows it.

## Query topologies

None is added, and none is changed. Each of the three is one or two selects on one task, as planned in
`PLAN-microbatch-gate.md:738-794`.

**What stays held to closing, and why.**

| Read | What it gives | The same on a second attempt, with the clock kept? | Held |
|---|---|---|---|
| `locks-of-request` | A new lock's bytes | No: made at the write, from a random source (`microbatch_gate.clj:143-151`) | Yes. It must be: ruling 17 (b), rule 5 |
| `answer-by-act-id` | Verdict, reason word, stamp, digest | All but one word: an act into a layer of the other gate is refused with one of two words, by what that gate had made by then | Yes. It must be: ruling 16 (2) |
| `depot-of-layer`, for the base | A fact's row | Yes | Yes, by ruling 16 (2): the contract keeps the door's requests as built |
| The streaming gate's question about the base (`streaming_gate.clj:112-124`) | A fact's row | Yes | Yes, by ruling 16 (5) |

For the last two, closing is no longer what keeps the row from changing; the kept clock is. They stay
held as one rule for the door and for the other gate. One reason is left that is true: both then say
the same thing of the base at one moment. If the streaming gate counted the base as made one run before
`depot-of-layer` gives it, a door could read a refusal that says the layer is made while its own read
still says that it is not. The docstring of `other-gates-record-holds-layer?` gives its reason as "it
could then refuse the making", which the kept clock makes untrue; it is replaced (see the list of
explanations).

`:closed-through` stays, with its one small write on each task for each batch with things.

## Partitioning efficiency

**The placement.** The row is one, for one gate, and it is read where the batch's base is made: on the
gate's task, where `:last-stamp` is and where every task's numbers gather. So the row, the depot's one
used partition and the event that writes the row are all on that task: the append is to the partition
of the task that the code is on, and the stream event has no partitioner. No hop, at any number of
tasks. It is one task for the whole gate, as it is for `:last-stamp` today: what is done there is done
once for a batch and never for an act, so it does not grow with the acts or with the tasks.

**The dominant read of this part** is the gate's read of the kept clock. Its categories are the runs of
the gate. The proportions are one stated workload: steady work, where each batch with things is
followed by the short run that closes it (the store's page, `:392-394`), and one batch in a thousand
runs twice. Seeks are totals over all tasks.

### N = 1 task (single-task baseline)
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| A run with nothing to decide | 0.500 | 0 | 0 |
| A batch with things, the attempt that keeps the clock | 0.499 | 3 (the row; the stream event's read of it; the row again) | 0 |
| A later attempt of a batch | 0.001 | 1 | 0 |
Weighted seeks = 1.498   |   Weighted iterator reads = 0

### N = 16 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| A run with nothing to decide | 0.500 | 0 | 0 |
| A batch with things, the attempt that keeps the clock | 0.499 | 3, all on the gate's task | 0 |
| A later attempt of a batch | 0.001 | 1 | 0 |
Weighted seeks = 1.498   |   Weighted iterator reads = 0

### N = 128 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| A run with nothing to decide | 0.500 | 0 | 0 |
| A batch with things, the attempt that keeps the clock | 0.499 | 3, all on the gate's task | 0 |
| A later attempt of a batch | 0.001 | 1 | 0 |
Weighted seeks = 1.498   |   Weighted iterator reads = 0

Flat. The work is for a batch and not for an act, and it does not grow with the tasks. A read of a
fact's row is not this part's read; it stays one seek on the task of the fact's entity.

## Design decisions

- **Subindexing.** None: the table holds one row.
- **Colocation.** Above: one task for the read, the append, the stream event and the row.
- **The pure parts stay pure.** The three functions that the dataflow calls for the clock are in
  `gate/batch.clj`, which reads no PState and no clock and loads with Clojure alone; the pure proof
  holds them. `batch/base`, `batch/replies` and `decision/refused-early` keep what they take.
- **The record of the depot is a map with two words.** It is the one thing of this part that is kept
  for good, and a later reader of that log should not need this plan to read it.
- **`depot.ack.failure.on.any.streaming.failure` stays at Rama's default**, so an append whose record
  failed on the stream topology's first try gets an exception (`14-depots.md:214`, read, not run; W5
  ran the case and saw the attempt end by its worker's restart or by the phase timeout). Set to false,
  the append would wait through the retries. Each outcome is traced below and gives one clock, so no
  setting is needed.
- **`topology.microbatch.phase.timeout.seconds` stays unset**, as it is today. W5 shows what that
  costs: after a failure of the batch clock's topology on another worker, the base stood still for
  about 30 s before Rama ran the batch again. A lower value would shorten that and would fail a long
  batch that is in no trouble; the store has never set it. Open point 6.

**Each thing that is added, and what breaks without it.**

| Taken out | What breaks |
|---|---|
| The depot | No way is left from the microbatch gate to state that Rama does not set back: a topology writes only its own PStates. A7's repair, "keep each batch's clock", cannot be built |
| The stream topology | Nothing answers the `:ack`, so the attempt cannot know that the clock is kept before it makes a stamp (ruling 17 (a)); with `:append-ack` the read back gave nothing, 0 of 82 (B3.ii). Merged into `streaming-gate`: the table under "Topologies" |
| The table `$$batch-clock` | A later attempt has nothing to read: topology code cannot read a depot's log, and the gate's own source would give the record to the next batch, not to this one (B2.3: a thing appended while attempt 1 ran came in the next batch) |
| The read before the append | Every later attempt would append again and wait again: about 3 ms and one record more for each, where the read costs 0.12 ms (B4) |
| The read after the append | The attempt would use what it offered. In scenario 1a the row can hold an earlier attempt's reading, and two attempts would then stamp from two clocks |
| The rule "a record of the same batch or an older one writes nothing" | A record that Rama hands over twice, or late, would change a batch's clock or put the row back to a closed batch (B4; W3 line 23) |
| The throw | An attempt whose clock is not kept would make stamps from it |

## State primitive selection

- `$$batch-clock` (PState): durable; one row; one write for each batch with things, so the write volume
  for a source event is 1 and is bounded by the gate's own runs.
- `*batch-clock-depot` (depot): durable; one record for each batch with things.
- No task global and nothing in the memory of a process: a task global was empty after the worker's
  restart (B3.iii a), and a restart is one cause of a second attempt.
- Not state: a reading of the clock that was offered and not kept.

## Resource usage analysis: the cost

**What one batch with things pays more**, in the attempt that keeps the clock, all on the gate's task:

| | Count | By the skill's figures | Run |
|---|---|---|---|
| Reads | 3 seeks: the row; the stream event's read; the row again | 0.9 to 1.5 ms | |
| Writes | 1 depot record; 1 row | | |
| Messages between tasks | 0 | | |
| Replication, on a real cluster | 2 rounds to the followers of that one task: the record, then the row | not run | |
| Time inside the attempt | | | about 3 ms, 2.8 to 4.5 over 80 batches (B3.ii cost: a thread for each task, one worker); 4.1 to 6.3, and once 29.2, over 40 batches (W: 2 threads for 4 tasks, 2 workers) |

It is paid once for a batch, not for an act. Against the store's own figures: an answer of the base was
readable 873, 885 and 893 ms after its append in three runs (`store-module/PROGRESS.md:175-180`), and rule 21 asks "a run of
the gate inside a second": 3 to 6 ms is under 1 in 100 of that. From outside nothing could be told
apart in the batch probe: the middle wait from the append call to the row was 180 to 198 ms on every
road.

**What a later attempt pays:** 1 seek, and no clock reading (0.12 ms in B4).

**What a run with nothing to decide pays:** nothing more. No seek, no append, no record (W4). It pays
one thing less than today: it reads no clock. The batch probe's line "Each run pays it, a run that got
nothing too" is of the probe's own module, whose first block ran in every run; here the keep stands
behind the batch's numbers.

**What `:ack` waits for on a real cluster.** Read, not run. First that the record is appended on the
leader of the gate's task and replicated to each in-sync follower. Then that the stream topology has
processed it, "including replication of any updated PStates" (`14-depots.md:206`): the row, again on
the leader and each in-sync follower, each durable on disk before it is visible
(`23-acid-semantics.md:112`). So two rounds of replication, each a round trip of the network to the
followers and a write to disk on each. The 3 to 6 ms here are of one machine with one copy: a floor,
not a cost. If a follower is slow, the attempt waits; past `topology.microbatch.phase.timeout.seconds`
it fails and is run again, with no stamp made.

**What grows.**

| What | By how much | Bound |
|---|---|---|
| `*batch-clock-depot` | One record for each batch with things, and one for each attempt that found no clock kept. Two words and two longs: about 50 bytes before Rama's own cost for a record, which was not measured. At one batch with things a second: 86,400 records, about 4 MB, a day. A run with things took about 117 ms in the batch probe, so at the very most about 740,000 a day. All on the one partition of the gate's task | None: never trimmed, as the two depots of acts. Sid's (Z3) |
| `$$batch-clock` | Nothing: one row of two longs | 1 row |
| The topology's own progress state | Rama's | Rama's |

**Minimization.** The row holds the two numbers that the read needs and nothing else. The depot's
record could be a pair and not a map, about 30 bytes less for each batch; the plan takes the map (see
"Design decisions"). Nothing is held twice but the newest batch's clock, which is in the row and in the
depot's last record; the row is what a topology can read.

## Why the attempts come out the same

**The one claim that the design adds (K).** For one microbatch id, every attempt that makes a stamp
makes it from one clock.

Derived, from four things. (1) An attempt takes a clock only out of a row of `$$batch-clock` whose
`:batch` is its own id: lines (iii) and (v) of step 2. (2) A row for an id is written once:
`clock-row-to-keep` gives a row only when the table holds none or one of an older batch; a later record
of the same id writes nothing. (3) The row of an id is replaced only by a record of a later id. Only
step 2 of an attempt of that later batch appends such a record, the depot takes nothing from a client,
and Rama starts a batch only when the one before is committed on every task; from then on no attempt of
the earlier batch runs. (4) The row is durable on every in-sync copy before the append returns. So every
attempt of a batch that comes to line `:286` holds the `:clock` of the one row that was written for its
id. What (K) rests on that this plan does not prove: that an id never comes again (run across an
attempt, a restart and an update); that Rama runs one batch at a time and gives each attempt the same
things (read at `12-microbatch-topologies.md:116`; run: B2.3); and (4) on more than one copy (read, not
run).

**The walk, step by step, on a second attempt.** With (K), by what each step reads.

| Step | What it reads | The same on each attempt, because |
|---|---|---|
| 1 | The things; `:turns`; this task's permission and depot rows | Rama gives the same things in the same order on each partition (B2.3) and sets the gate's PStates back (B3.i). The rest is pure |
| 2, the gate's task | `:last-stamp`, `:closed-through`, the numbers, the clock | Set back; set back; gathered from step 1 by a function that does not rest on the order of arrival (`batch.clj:138-148`); (K) |
| 2, each home task | Each row that was asked for | Set back |
| 3 | A task's entries and replies; its own places at each turn | From steps 1 and 2; the rows that earlier turns of this attempt wrote, in the order of the turns |
| 4, the rows sent | What step 3 sent | From step 3 |
| 4, the acts that meet | The meeting task's own copy; the other gate's row; a lock's bytes drawn for the call | The first: set back. The other two are not the same; see below |
| 4, a new lock | A random source | Not the same; see below |

**What still differs between two attempts, exactly, and why no reader of a fact's row gets it.**

1. **A new lock's bytes.** Made at the write, on the lock's home task
   (`microbatch_gate.clj:212-214`). They are in a row of `$$microbatch-locks` and in no other row. A
   fact's row names a lock by the act that asked for it and an index (`rows.clj:15-22`), and those come
   from the act; after A1 it names none. The bytes beside a sealed value are copied from a lock's row
   as the batch found it, and the door can hold only a lock of a closed batch, so they are of a closed
   batch. A use of a lock that the same batch asks for is refused on every attempt (item 3). Only
   `locks-of-request` gives a lock's bytes, held to closing.
2. **The word for a layer of the other gate.** Step 4 reads `$$streaming-facts` on the meeting task
   (`microbatch_gate.clj:381-385`), and that table is another topology's: it holds what the streaming
   gate had committed by then. The read is made only for an act into a layer of which this record and
   the pass hold nothing and that starts streaming (`batch.clj:747-756`). Such an act is refused on
   every attempt. `decision/standing` gives it `:layer-belongs-to-the-other-gate` when the row is
   there. When it is not, it would pass only as a right making act for this gate, and the first test
   of that form is that the layer starts with this gate's depot (`first_act.clj:65`, `:92`), which
   is false here; so it gets `:layer-not-made` (`decision.clj:116-153`). A refusal writes its answer
   and nothing else. So one answer's word can differ, its verdict and its stamp do not, and no fact's
   row comes of it. Only `answer-by-act-id` gives the word, held to closing.
3. **The bytes drawn for the check of a lock that is not made yet** (`microbatch_gate.clj:386`). They
   are given to the check of a seal for a lock that a request of the same pass makes
   (`batch.clj:798-835`). No sender can have sealed with bytes that are not made, so the use is refused
   as a value that the lock does not open, on every attempt, unless 32 random bytes open a seal whose
   tag is 16 bytes. They are kept nowhere.

Nothing else. In particular, with (K): each fact's row is the same in each field (the value's bytes,
the lock's name, the fact that wrote it, the stamp); each answer's verdict, stamp and digest are the
same; each answer's word is the same but in case 2; `:last-stamp`, `:closed-through`, `:turns` and the
strays are the same. S4 ran the other side: with the clock read anew, 15 of 15 rows differ, in the
stamp and in nothing else.

## The scenarios, by hand

One batch, id 41, with 8 things on 3 partitions. The gate's last stamp before it is L. Attempt 1 would
read the clock c1 = 1,791,750,432,650; an attempt one second later would read c2 = c1 + 1,088. The
table holds `{:batch 39, ...}`. "Rows" are the fact rows and answer rows of the batch.

| # | Scenario | What each attempt computes | The rows |
|---|---|---|---|
| 1 | Attempt 1 fails before the clock is kept: in step 1, or in step 2 before the stream topology wrote | Attempt 1 made no base: `:286` is after the ack. Attempt 2 finds the row of batch 39, reads c2, appends, reads back `{41, c2}`, base from c2 | Attempt 1 made no row. One set of rows, from c2 |
| 1a | The same, but attempt 1's record is on the depot and not yet processed when the worker stops | At the restart Rama hands the topology that record. If before attempt 2's first read: attempt 2 finds `{41, c1}`. If after: attempt 2 reads c2 and appends; the topology writes the first of the two records that it processes and nothing for the other; attempt 2 reads the row back and takes what it holds, c1 or c2 | Attempt 1 made no row. One set of rows, from the one clock that the row holds |
| 2 | Attempt 1 fails after the keep and before any row is written (in step 2 after the ack, or early in step 3) | Attempt 2 finds `{41, c1}` at line (ii), reads no clock; base = `next-stamp(c1, L, carried)`, the same | The same as attempt 1 would have made |
| 3 | Attempt 1 fails after some tasks have shown their rows (the batch probe's window, B7.2) | The same as 2. Each task writes the rows again | The same row at every key, also on the tasks that had shown it. A select there is given no change; an open push gets one more callback with the same value (B7.3, S5 and S7; ruling 17 (c) is the read path's) |
| 4 | Two failures in a row | Attempts 2 and 3 each find `{41, c1}` | The same. Run: B2, batch 10 began three times with the same things |
| 5 | A worker starts again in the middle | The row and the depot are durable. Attempt 2 finds c1. The topology may be handed the record again and writes nothing | The same. Run: B4; W3 line 23; S2 with two workers |
| 6 | The stream topology fails the record on its first try (`14-depots.md:214`) | The append does not return cleanly. Rama's page says that it gets an exception. W5 saw two ends of attempt 1: its own worker started again, 1.4 s to the next attempt; or it waited for the ack until the phase timeout, 31.4 s. Either way attempt 1 made no base. Rama tries the record again, and the row is written. Then as 1a | Attempt 1 made no row. One set of rows. Run in one order: W5, both logs |
| 7 | A batch with nothing to decide: no thing, or only strays | No numbers: no row read, no clock read, no append, no base, `:last-stamp` not written | None. Run: W4 |
| 8 | The first batch ever | The table holds nothing: `kept-clock` gives nil; read c1, append; the topology writes the first row; read back c1 | From c1 |
| 9 | Rama runs the stream topology's event again | For the newest batch's record: the row is of that batch, nothing written. For an older batch's record after a newer one: the row is of a later batch, nothing written | Not touched |
| 10 | A module update | The ids go on, and the row and the depot stay (W6). A batch that was in flight is run again under its id and finds its clock | The same, when the update did not change the code from which a row is made. After an update the dataflow runs the functions of the new module (B5.b), so an update that changes that code can make the second attempt write other rows: the paragraph below |
| 11 | The stream topology does not answer in time | The attempt waits in line (v). The phase timeout fails it: about 30 s with nothing set (W5, the other log). No base was made. The gate decides no batch with things until a clock can be kept: it stands still, and makes no stamp from a clock that is not kept | None made |
| 12 | The leader of the gate's task moves after the ack | The row was on each in-sync follower before the ack came. Read, not run | The same |
| 13 | A batch in which every thing is refused by the declaration | Each still sends its numbers (`batch.clj:298-302`), so a clock is kept and each answer gets its stamp from it | The same answers |
| 14 | An act that carries a stamp at the edge of the 10 seconds: its time is c1 + 10,400 ms | Attempt 1: `too-far-ahead?` of c1: refused `:carried-stamp-too-far-ahead`. Attempt 2, at c2 on the wall: still held to c1: refused, the same word, the same stamp. Today it would be accepted there | The same answer. The proof makes this act (see the proof) |

One kind of moment where two attempts can still differ, said plainly: a module update that changes the
code or the forms from which a batch's rows are made, when it falls between two attempts of one batch.
The second attempt then runs the new code, on the same things and with the same clock. This build
makes two such updates. The one that brings this change into a cluster that runs: a batch whose first
attempt ran in the old code, and showed its part, is run again in the new code, finds no clock kept
and reads its own. And the one that brings the part `what-the-gates-keep`: it changes what a fact's row
holds. No reader of facts exists before those two, and no cluster runs the store today. Whether Rama lets an
update fall between one task's commit of a batch and another's was not run, and no line read here says.
It is a line for the receipt, and a thing to know for whoever changes the gate's code on a cluster that
runs.

## What this part hands on

**To `what-the-gates-keep`, to take whole.** Under ruling 53 that part is built first, from ruling 17 and
the split, and its plan holds this rule already ("Every new row is the same on a second attempt").
This part's build then holds each of that part's rows to it, by the proof.

A row of a microbatch layer that a task shows is the row that every attempt of its batch makes, so a
read may take it straight from the table, when the row is made from these three things and from pure
code only:

1. the things of the batch, with their turns;
2. the rows of this gate's own PStates as they were when the batch began, and what earlier turns of the
   same batch wrote;
3. the batch's kept clock, which reaches a row only through the base and through the 10-second rule.

It does not hold for a row that takes anything from: a clock that the attempt reads; a random source; a
PState of another topology (the other gate's record; `$$batch-clock` by any way but step 2's); the
memory of a process; the order in which messages arrive; or which attempt it is.

The conditions under which it holds are this part's, and are in "Why the attempts come out the same":
the clock is kept before the first stamp; Rama gives each attempt the same things and sets the gate's
PStates back; an id of a batch never comes again; and one version of the module's code runs every
attempt of the batch (scenario 10).

What each new row must keep, so that it stays true:

- A plain value (A1): opened from the act's sealed bytes with a lock's row as step 2 read it, never
  with drawn bytes. A value whose lock is not made yet is refused, as today.
- The who and the permission (A2), the setting and the list (A3, A5): from the act.
- The marks, and the other order (A6): from the fact's row and from the marks as the task held them
  when the batch began (ruling 18 says so for another reason; it is also what keeps this true).
- A new refusal ("an act needs act"): from the rows of this record. A word that rests on the other
  gate's record may differ between attempts; it must stay in an answer and reach no fact's row.

What is handed over with it:

- The walk of one batch as this part leaves it: only `microbatch_gate.clj:284-286` changes, by the
  table of step 2. `base`, `replies`, `decide-or-send`, `decide-next` and `rows-to-send` take and give
  what they do today.
- The names that this part makes, so that none is made twice: `*batch-clock-depot`, `batch-clock`,
  `$$batch-clock`, the fields `:batch` and `:clock` of its row, `softland.store.module.batch-clock`,
  `clock-record`, `kept-clock`, `clock-row-to-keep`, `clock-must-be-kept`, the schema function
  `batch-clock`.
- The proof's hand for a second attempt and the check that sets the rows of two attempts side by side
  (see the proof). They live on the proof's second cluster (ruling 49). That part's rows are in the
  batch of that check, and what they need before the batch is sent on that cluster too: under ruling
  53 this part's build puts them there, as N15 of that plan lists them. The check must still give no
  key. The noting functions take what `decide-or-send` and `rows-to-send` take and give after that
  part's build. Every act of that batch stays in the base: the throw comes in the one call of
  `rows-to-send` that gives rows.
- Which way ruling 17 (d) went: the build of this part says it on `PROGRESS.md`, by the stop test.

**To `read-path`.** What still differs between two attempts, and so is never given as a fact: a new
lock's bytes, the word of one refusal, and the drawn bytes. None is in a row of `$$microbatch-facts`.
The read path reads no lock and no answer. A push on a row that a second attempt writes again with the
same value calls back once more with that value (B7.3): ruling 17 (c) is that part's.

And one thing that the kept clock does not give. Each row that a task shows is the row that stays; that
every task shows a batch together is not given. Without a failure the tasks showed a batch 0.3 to
3.0 ms apart (B7.1). When a commit fails after some tasks have shown their part, those tasks show the
batch's rows and the others do not until the second attempt commits: 3.4 s in the batch probe's S3,
and with nothing set Rama's phase timeout, about 30 s, is the order of it. So a read must not rest on
a batch being whole across tasks: a row that every task holds, or an act with facts on two entities,
can be there on one task and not yet on another for that long.

## The proof of ruling 17 (d), and the stop test

**Where it lives and how it is run.** In the module's proof, `test-store/softland/store/module_proof.clj`,
by the one command of today: `clojure -M:store-module -m softland.store.module-proof`. No new proof
file. The four proofs stay; the count lines of the module's proof and of the pure proof go up.

**Two clusters, one after the other, in the one run** (ruling 49). The proof's cluster comes first, as
today. Every check of today, and each check of this part that needs no second attempt, runs there on
the functions as the store defines them: nothing is replaced in the JVM while it runs. When it is
closed, `-main` puts the three noting functions in place, creates a second cluster, launches `Store` in
it with the same four tasks, two threads and two workers, and runs one group there: the batch that
runs twice. Then it closes that cluster and puts the first functions back, in a `finally`. The count
line is one: the counts of the two runs, added. Run (T): three clusters one after the other in one JVM
each launched the store's module, decided its batches and closed, the first of them after a module
update as the proof makes one. A later cluster was created and launched in 1.5 s and closed in under
1 s. So ruling 49's other road, a proof file with a command of its own, is not needed.

What the second cluster needs of its own. First, the hand's count of things sent:
`wait-until-processed!` (`module_proof.clj:316-320`) waits until the gate has processed as many things
as the hand has sent, and Rama counts from the launch of the cluster. So `-main` empties the hand's
atoms, all but the shown lines, between the two clusters. Second, a base of its own: the group first
makes the base, asks for the locks that its batch uses, and writes the fact that its batch replaces,
each in a batch that runs once.

**How a real batch is made to run a second time, with no hook in the module's source.** As S ran it,
in the second cluster. Before that cluster's `launch-module!`, `-main` puts another function in the var
of each of three functions of `gate/batch.clj` that the gate's dataflow calls: `base`, `decide-or-send`
and `rows-to-send`. Each replacement is a function that a var of the proof's namespace holds, and it
calls the first function, which a `def` of the proof took when the namespace was loaded. It closes over
nothing: a replacement that closes over an atom makes the launch fail (B5.a). Rama takes the function
that a var holds at each launch. Run (T2, T3): three functions that were replaced after the module
object was defined, and after the module had been launched once in the same JVM, ran in the second
cluster; with the first functions put back, a third cluster ran those. A replacement after a launch
does nothing to that launch until the module is updated (B5.b). So the three are in place before the
second cluster is launched and until it is closed, and each gives exactly what the first function
gives.

One of them, `rows-to-send`, throws once when the check asked for it: in step 4, in the call that
gives rows. Step 4 calls `rows-to-send` on every task, and only the meeting task of a layer gets rows
from it (`microbatch_gate.clj:389`; `batch.clj:837-848`). Every act of the check's batch is into the
base, so one call gives rows, on the base's meeting task. It notes them and then throws. Every row of
step 3 is written by then on every task, and nothing is committed. Rama then starts the worker again
and runs the batch again (B2.4; S2).

Why not on the first task that comes there, as S threw. The note of the first attempt is then whole
only if the meeting task's call still comes. Run (T4): thrown on the first call, the throw came 6 of 6
times in a call that gave no rows, on one worker. The note held the rows of the acts that meet only
because the base's meeting task is on the other worker, whose thread went on. Thrown in the call that
gives rows, 6 of 6, the note is whole by the order of the code.

The group forces one failure. Run (T): the second attempt came 1.0 to 1.5 s after the first for the
first two failures of a worker, and 5 to 15 s after for the third to the sixth.

**How the proof learns what the first attempt made.** By the same three functions. `base` is called
once in each attempt, on the gate's task, and with numbers it begins the note of an attempt: the clock
and the last stamp that it was given, the base, and the wall time of the call. `decide-or-send` gives
each row of an act that is decided where it arrived; `rows-to-send` gives each row of an act that
meets. Each row is added to the newest note. So after the batch the proof holds, for each attempt,
every fact row and every answer row as that attempt made it (S3: 15 rows for 8 things, under the same
keys in both attempts); the first attempt's too, since its throw comes after the one call that gives
rows. The notes are the memory of the proof's process. They last through the worker's restart only
because the in-process cluster is one JVM (B3.iii b); the map says so. A row of an attempt that failed
while it was processed is never in a table (B2.1), so no read could give it.

**The batch of the check**, sent in one batch as `decide-in-one-batch!` does, with the throw asked for.
For this part, with the rows of the store as it stands:

1. acts of the system on several entities, so on several tasks: a fact with no value, and a fact with
   a sealed value under a lock that the hand holds, which also gives the two rows of a used lock;
2. an act that names a fact to replace, and a second act on the same place;
3. a give: it meets, and its row goes to every task;
4. a request for locks;
5. an act whose permission no act gave, and a thing that the declaration refuses;
6. an act that carries a stamp whose time is the hand's clock plus 10,700 ms: scenario 14. The hand
   makes this act last: it reads the clock for it right before it appends it, and lets the topology go
   on at once. The act is at the edge when the first attempt takes its clock less than 700 ms after
   that reading and the second attempt more than 700 ms after it. S: the first attempt's clock came
   122 ms after the topology went on, and the second attempt 1,088 ms after the first. T: 1,001 to
   1,548 ms between the two for a worker's first failure. A reading before the pause and the appends,
   with 400 ms, would have missed in S's own run: the pause and the eight appends took about 300 ms
   before those 122.

7. under ruling 53, the rows that `what-the-gates-keep` added, as N15 of its plan lists them: a fact
   under a key that an earlier batch marked, over a value to take out; two acts on one place under
   that key; a write of the marks, of the setting and of the list; a give with the slot for what it
   allows. The mark and the first value are sent on this cluster before the batch. That part makes
   `decide-or-send` and `rows-to-send` give rows of the kind `:by-value`, a row that is nil among them
   for a key that is taken out. The notes keep them, and `rows-side-by-side` tells a key that was
   noted with no row from a key that was not noted.

No act into a layer of the other gate is in it, so every field of every row must be the same.

**What the check holds.** In this order.

1. The notes hold two attempts of one batch: two notes since the send, one throw taken between them,
   and both given the same last stamp and the same numbers. The last stamp tells two attempts from two
   batches without the clock: Rama sets it back for a second attempt, and a batch after this one is
   given a later one. Run (T5): the same in both notes, in 12 of 12 batches that ran twice.
2. Both were given one clock, and it is the clock of the row that the proof selects from
   `$$batch-clock` after the batch, and of the batch's one record on the depot.
3. The wall time at which each attempt began, as the noting `base` read it. The check holds one thing
   to them: the second attempt began later than the batch's clock, so the one clock is not two
   readings in one millisecond. It prints both. They say what the run shows: with the clock read anew,
   as S4 ran it, each stamp would be another; and when the edge of item 6 lies between the two, the
   act of item 6 would have got the other verdict.
4. `rows-side-by-side` of the two notes gives no key: the two attempts made rows under the same keys,
   and at each key the same row in every field, a run of bytes compared by its content.
5. Each fact row that the proof selects on the task of its entity after the batch, each row of the give
   on every task, and each answer row, is that row.
6. Each answer as `answer-by-act-id` then gives it has the verdict, the word and the stamp of that row.

**Rulings 17 (a) and (b), as things that can be observed**, in the same group, on the second cluster,
where `base` notes each attempt. (a): the records on `*batch-clock-depot` are as many as the batches
with things since that cluster's launch. That is the notes less one: the second attempt of the batch
that ran twice found its clock kept and appended nothing. A quiet time of 1.5 s adds none. (b): each
record of that depot, read back, is a map of exactly `:batch` and `:clock`, two integers. And on the
proof's first cluster: the last check of the base, which today shows that no lock's bytes that the
door read are on the microbatch depot, reads this depot too.

**The first sentence of VA7, by the proof's own select.** One more check, with no failure in it, on the
proof's first cluster. A fact is written to a place of the base. Then an act replaces it, and from before that append until the new
row shows, the proof selects the place on the task of its entity every few milliseconds. Each select
gave a row, the old or the new, and never nothing. Today the same holds of a straight select; what
this part adds is that the row which it gives stays. The read path's own proof shows it again through
the read path.

**After the module is started again** (the group that is there today, on the proof's first cluster):
the kept row is as it was; a batch with things after it keeps a clock under a later id.

**The pure proof gains** the cases of the three pure functions, each also given things that are no
record and no row, since none may throw. And one case in its run of a batch in memory (`run-batch`):
the fixture's batch run twice with one clock gives the same record in every row but a new lock's bytes;
run with a clock one second later, each stamp is another, and an act at the edge of the 10 seconds gets
the other verdict. That is the implied spec's table of A7 as a run of the pure code.

**The window on the store's own module: left to the probe.** Said plainly: this plan does not show, on
the store's module, a commit that fails after some tasks have shown their rows. It can be done with no
hook in the source, and this is what it would take. A cluster of its own, with a thread for each task
(the proof's has 2 threads for 4 tasks, and a held thread would hold two tasks): a third cluster in the
run, since clusters one after the other work (T), or a proof file of its own. A fourth replaced
function, the test for a stray that the streaming gate's event calls first,
which sleeps on a layer's task and then throws there, as the batch probe's S6 did from a stream
topology; S3's way needs the phase timeout set at the launch, which is the module's source. A latch in
`rows-to-send` that asks for the hold only when the held task has done its part, in a batch where no
act meets, and stays 150 ms more. Watchers that select each row during the hold. And a pass that rests
on times: the other tasks must commit inside a hold of about 700 ms. What it would add: nothing about
the store's code. The check above shows that the store's second attempt makes the same rows; B7.3 shows
what Rama gives a reader when a second attempt writes rows that are the same. The window on the store
would run the two together. Ruling 17 (e) already words the receipt for it. Open point 3.

**What the map then says the proof does not reach** (the lines that replace
`test-store/softland/store/README.md:325-329`): a commit that fails after one task showed its part, and
the moment between two tasks' commits, which the batch probe ran on a module of its own; the first
attempt's new lock bytes, which are made inside the write and are not learned; a worker whose process
is killed, a move of a task's leader, more than one copy of the row; a module update between two
attempts of one batch; and that what the notes hold of the first attempt is the memory of the proof's
process. The map also says where each check runs: the batch that runs twice on a cluster of its own,
with three functions of `gate/batch.clj` replaced by ones that give what the first functions give;
every other check on the functions as the store defines them.

### The stop test

Ruling 17 (d): "If the build cannot make that hold on the store's own gate, it stops the repair and
keeps the row before beside the new one."

**The run does not hold** when the check above is run on the built repair, its first line holds (the
notes hold two attempts of one batch), and one of these is seen:

1. the second attempt's `base` was given another clock than the first's;
2. `rows-side-by-side` gives a key: a row under a key that the other attempt did not make, or a fact's
   row or an answer's row that differs in a field;
3. a fact's row or an answer's row that the proof selects after the batch is not the row that both
   attempts made;

and it is still seen after the build session has read the difference and found the code to be as this
plan writes it. A difference that comes from code that is not as planned is a fault of the build, and
is repaired and run again.

Under ruling 53 the batch holds the rows of `what-the-gates-keep` too. A key whose rows differ only in
a field that that part added, with one clock given to both attempts, says that this field takes
something from outside the three things of "What this part hands on". That is a fault of that row's
making, repaired in that part's code and run again. It is the stop only when it stays with the code as
the two plans write it.

**Not the stop:** the notes hold one attempt, or two notes that were given two last stamps, which are
two batches. Then the check's hand did not make one batch run twice, and the check's first line fails.
S and T made a second attempt on the store's own gate, so the hand is repaired. A new lock's bytes that
differ are no stop: they must differ, and no noted row holds them (`batch.clj:600`: the row of a new
lock is nil until the write). An act of item 6 that was not at the edge is no stop: the check prints
it.

In one line, for `PROGRESS.md`: two attempts of one batch noted, one clock, and no key from
`rows-side-by-side`, the run holds; two attempts of one batch noted and another clock or a key that
stays, stop.

**What "the row before, kept beside the new one" would then ask of each part.** Stated, not designed
(ruling 33). This plan would run again.

- **Of this part.** The depot, the topology, the table and the changed step 2 come out. A place of a
  microbatch layer holds its row as of the last closed batch beside the new one. Each write of a fact's
  row in steps 3 and 4 takes the row before with it, and two acts of one batch on one place must not
  leave the earlier turn's row as the row before (implied spec A7, the second table). "One row for a
  place" (`tables.clj:17-19`) no longer holds for this gate. `:closed-through` becomes what every read
  of a fact is held to. The proof shows that a read held to closing gives the row before until the
  batch is closed, and never nothing.
- **Of `what-the-gates-keep`.** Each row that it adds or changes in a microbatch layer is kept with its
  row before: the plain value, the who and the permission, the setting, the list, the marks. The other
  order holds, for a place whose fact is replaced, the entry before and the new one, each told apart by
  closing. Items 1, 6, 7, 8 and 9 of its scope are planned again for the microbatch gate (its entry of
  the split, `plans_ahead_of_builds`).
- **Of `read-path`.** Every read in the base reads `:closed-through` of each task that it touches and
  takes the newest row that is not later: a place, a run, what points at a thing, and the facts that
  decide a read. A live read follows `:closed-through` of each such task by a push of its own, which
  calls back for every batch with things. Each count of seeks under "Latency and growth" grows for the
  base (implied spec P16, point 4). Rulings 16 (3) and 17 (c) are ruled again.

## Every function added or changed, in the order to build

| # | Namespace | Name | Kind | Takes | Gives | What must hold |
|---|---|---|---|---|---|---|
| 1 | `softland.store.record.tables` | `batch-clock` | new, a schema | nothing | `{Keyword -> fixed-keys{:batch Long, :clock Long}}` | A row of another class throws at the write, so the row is made only by function 4 |
| 2 | `softland.store.gate.batch` | `clock-record` | new, pure | the microbatch id of a run; a reading of the clock in milliseconds | the record to append, `{:batch :clock}`, both as longs | The id is a number: a `Long` in Rama 1.6.0 (W2) |
| 3 | `softland.store.gate.batch` | `kept-clock` | new, pure | the row of `$$batch-clock`, or nil; the microbatch id of the run | the row's `:clock` when the row's `:batch` is that id; else nil | It never throws |
| 4 | `softland.store.gate.batch` | `clock-row-to-keep` | new, pure | the row that the table holds, or nil; a record of the depot | the row to write, `{:batch :clock}`, when the table holds none or a row of an older batch; nil when the row is of the record's batch or of a later one, and for a thing that is no map of two integers | It never throws on what a depot holds: a record that throws is tried for ever |
| 5 | `softland.store.gate.batch` | `base` | docstring only | as today | as today | Its first word becomes: the batch's clock, the one that is kept for the batch, nil for a batch with no numbers |
| 6 | `softland.store.module.batch-clock` | `add-to` | new, dataflow | the module's `topologies` | declares the stream topology `batch-clock`, its PState `$$batch-clock` and the event of one record, as "Topologies" walks it | The module declares `*batch-clock-depot` |
| 7 | `softland.store.module.microbatch-gate` | `clock-must-be-kept` | new, a function of the gate | what `kept-clock` gave after the append; the microbatch id | the clock; throws when there is none | It is the one throw of the module's code, and its docstring says why: no stamp from a clock that is not kept |
| 8 | `softland.store.module.microbatch-gate` | `add-to` | changed, dataflow | as today | as today, with step 2 by the table of step 2 | The module declares `*batch-clock-depot` and has the topology `batch-clock` |
| 9 | `softland.store.module` | `Store` | changed | | declares `*batch-clock-depot` with `:disallow`; calls `batch-clock/add-to` before the microbatch gate's | |
| 10 | `softland.store.module-proof` | `noting-base`, `noting-decide-or-send`, `noting-rows-to-send` | new, the proof's | what the first function takes | what the first function gives; each notes it. `noting-base` also notes the clock and the last stamp that it was given, and the wall time. `noting-rows-to-send` throws once when asked, in the call that gives rows, after it noted them | Each is held by a var and closes over nothing. `-main` puts them in place after the proof's first cluster is closed and before the second is launched, and puts the first functions back when the second is closed |
| 11 | `softland.store.module-proof` | `decide-twice!` | new, the proof's hand, for the second cluster | things for one batch | the two attempts' notes, and the result of each thing | It reads each answer in a loop that takes a read that throws as nothing yet: for about half a second after the restart a read throws (the batch probe, "Seen on the way") |
| 12 | `softland.store.module-proof` | `rows-side-by-side` | new, the proof's | two attempts' notes | each key whose row is in one note only or differs, with the fields that differ; bytes by content | A key that is noted with no row, as the key of a new lock is (`batch.clj:600`), is a key all the same: it must be in both notes |
| 13 | `softland.store.module-proof` | the checks | new | | "The proof", above. On the first cluster: the first sentence of VA7, placed after the path of a lock in the base and before the module is started again; the kept row across that start, in the group that is there today; and the last check of the base reads the new depot. On the second cluster: one group, the batch that runs twice, with rulings 17 (a) and (b) | The second cluster's group makes its own base, its locks and the fact that its batch replaces |
| 13a | `softland.store.module-proof` | `-main`, `test-ns-hook` | changed | as today | two clusters one after the other, as "Two clusters" says: today's groups and this part's on the first, the one group on the second; one count line, the two runs' counts added | The hand's atoms, all but the shown lines, are emptied between the two. The first functions are put back in a `finally`. Exit status as today, over both runs |
| 14 | `softland.store.pure-proof` | cases | new | | the cases of 2, 3 and 4, and the run of one batch twice | |

Functions 2 to 4 are in `gate/batch.clj` because they are the pure code of a batch's stamp, beside
`base`; the namespace stays as it says of itself, with no PState, no clock and no throw on what arrived.

## Words that the part makes, for "Words a session made, Sid's to change"

Each says what the thing is. None is from the rig's code, and none is taken from the batch probe's
names.

| Word | What it is |
|---|---|
| `*batch-clock-depot` | The depot of batch clocks: one record for each batch of the microbatch gate that had things to decide. No client may append to it |
| `batch-clock` | The stream topology that keeps the clock of the newest batch |
| `$$batch-clock` | The clock that is kept for the newest batch that had things: one row, under the depot word `:microbatch` |
| `:batch`, in a record of that depot and in that row | The batch that the clock belongs to, as the number of its microbatch id. In `:turns` the same id is held as text |
| `:clock`, in the same two | The clock time of the batch, in milliseconds since 1970 |
| the batch's clock | In the explanations: the one clock time from which a batch's base is made and against which the 10-second rule is held, the same on every attempt |
| to keep a clock; the kept clock | To put a batch's clock where a later attempt of the batch reads it; that clock |
| `softland.store.module.batch-clock`, `module/batch_clock.clj` | The namespace and the file of the topology |
| `clock-record`, `kept-clock`, `clock-row-to-keep`, `clock-must-be-kept`, `batch-clock` (the schema) | The functions of the table above |

"Batch", "attempt" and "microbatch id" are Rama's.

## Every number of the build

- No number in the module's code. The depot and the topology set no option of Rama. One number of
  Rama's own came into view: its phase timeout for a microbatch, about 30 s when nothing is set (W5).
- The ack level of the append: `:ack`. The source: from the beginning; Rama's default retry mode.
- In the proof: 10,700 ms, how far ahead of the hand's clock the edge act's stamp is, which is the
  store's 10,000 (`stamp.clj:36-40`) and 700, the middle of what the runs give between the two
  attempts' clocks (item 6 of the batch); 1,500 ms, the quiet time in which no record may come; the limit of the
  wait for the answers of the batch that runs twice, 60 s: S saw 2.1 s, and an attempt that Rama ends
  by its phase timeout takes about 31 s (W5). Each is the proof's and is in its docstring.
- The checks of the door's proof that rest on a time (`test-store/softland/store/README.md:577-600`):
  the two that are of the base hold a least time (195 ms or more; no answer at the first read), and a
  batch that takes 3 to 6 ms longer moves each toward its pass. The build runs the door's proof at the
  end of the part, as the split asks.

## Every explanation that the part makes untrue, and what it will say

Replaced, not amended. The decisions sections of `src-store/README.md` are Sid's and are not touched.

**The store's page, `src-store/softland/store/README.md`.**

| Lines | Says today | Will say |
|---|---|---|
| `:5-10` | The module is "two depots, the gate of each, the record that each gate keeps, and the three reads" | The two depots of acts, the gate of each, the record that each gate keeps, what keeps the clock of each batch of the microbatch gate, and the three reads |
| `:31-32` | `module/`: "the two gates' topologies and the three reads". `module.clj`: "the two depots ... It owns the two depots" | `module/`: also the topology that keeps a batch's clock, which owns one PState. `module.clj`: the two depots of acts and the depot of batch clocks; it owns the three, durable and never trimmed |
| `:86` | Step 2: "One task reads the clock once and gives the batch its first stamp." | One task takes the batch's clock: the one that is kept for the batch, or, when none is, a reading that it keeps first. It gives the batch its first stamp |
| `:110-112` | "Each gate's dataflow only reads rows, calls the pure functions and writes rows." | True of the streaming gate. The microbatch gate's dataflow does one thing more, for a batch's clock: when none is kept it reads the clock, appends one record to the depot of batch clocks, and throws in the one case where the clock is then not kept. The sentence on a pure function stays |
| `:145-146` | "A topology can append to the partition of the task that it is on, whatever the partitioner; no topology of this build does." | ...; no topology of this build appends to the streaming depot. The one append that a topology makes is the microbatch gate's, of a batch's clock, to the depot of batch clocks |
| `:173-177` | "A direct read of the tables": it sees a batch "which Rama can still run again with another clock and other lock bytes" | It sees a batch that is not closed, which Rama can still run again. A second attempt makes each fact's row, and each answer's stamp and verdict, as the first made them: both take the batch's kept clock. That holds while one version of the module's code runs both attempts. Two things it can make otherwise, and a direct read sees them: a new lock's bytes, in the locks table; and, in an answer, which of two words an act into a layer of the other gate is refused with |
| `:190-196` | "The same word on every attempt of a batch": two words can differ; "To make both the same would take state outside the microbatch gate. Not built." | The order of a batch is the same on every attempt, and so is its clock, kept before the first stamp is made from it. With them each check that reads the batch, the clock and this gate's record gives the same verdict and the same word, the 10-second rule too. One word can differ: the word for a layer of the other gate follows what that gate had made by then; the act is refused on each attempt. No read of the door gives it before the batch is closed |
| `:235` | F6 A, "Where it shows": "`module.clj` declares `*streaming-depot` and `*microbatch-depot`" | The same, and: a third depot, `*batch-clock-depot`, takes no act, and no gate decides from it. His wording of the decision is not written again (ruling 35) |
| `:236` | F2 A, "Where it shows": "neither depot is ever trimmed" | No depot is trimmed |
| `:332-334` | "The source of each gate." | Also the batch clock's topology: from the beginning, Rama's default retry mode |
| `:378-383` | "The stamp of a batch. The gate reads the clock once for a batch." | The gate takes one clock for a batch, the same on every attempt. The rest of the paragraph stays. A new paragraph follows it, "The clock of a batch": the depot, the topology, the one row, why the first clock of a batch stays, that it is kept before a stamp and only in a run with things, what it costs, and that an attempt which cannot read it back fails by a throw, with what a throw does to the worker |
| `:385-400` | "Closing": "the second attempt reads the clock again and makes other lock bytes. So a reader could see a stamp or a lock's bytes that later change." | A second attempt makes other lock bytes, and can give an act into a layer of the other gate the other of two words. So a reader could see a lock's bytes or a word that later change. Each task therefore holds `:closed-through`, and the door's three reads and the streaming gate's question give a row only when its stamp is not later. The run of the paragraph stays. Its last sentences, on a place that is replaced, stay until the part `read-path` (the split) |
| `:415-421` | "Each batch adds a few reads that do not grow with its acts." | And a batch with things keeps its clock: 3 reads, one append and one row write on the gate's task, once for the batch |
| `:438-443` | Rule 5: "A request holds a count, and no code appends to a depot." | A request holds a count, and no code appends a lock's bytes to a depot: the one append that the module's code makes is of a batch's id and clock. The rest stays |
| `:454-461` | Rule 9: "Nothing is built for it; whether it is wanted is Sid's." | A first part of it is built, for another purpose: the depot of batch clocks holds the clock of each batch that had things, by the id of the run that decided it. It holds no cut, and a rebuild cuts other batches, so it does not make a rebuild the same. Whether the rest is wanted is Sid's |
| `:483-485` | Rule 17: "holds each act of a batch to the one clock reading of the batch" | Holds each act of a batch to the batch's one clock, on every attempt |
| `:486-491` | Rule 18: an act's one stamp, the batch's base plus its turn; the gate's one last stamp | Read for this part, and no line of it is made untrue. It names no clock. That the stamp is the same on every attempt is said in "The stamp of a batch" |
| `:492-501` | Rule 19: a rebuild would not give again "a stamp, which holds the gate's clock at the decision; a refusal by rule 17" | The same list, and for the microbatch record: the clock of each batch with things is on the depot of batch clocks, by the id of its run; a rebuild cuts other batches (rule 9), so that does not give the stamps again |
| `:540-561`, `:563-575`, `:577-595`, `:673-700` | The names in Rama, the namespaces, the fields, the words of a batch | The words of the table above, each in its list |

**`module.clj`.** The head comment gains this plan (ruling 39). The namespace docstring: `:6-7`, "its
two depots", becomes the two depots of acts and the depot of batch clocks, and the calls that add the
two gates, the batch clock's topology and the reads. `:9-23`, what a client opens, gains two lines:
`*batch-clock-depot`, which no client can append to and whose log a client can read; and
`$$batch-clock`, for an operator. `:27-31`, "State. The module owns the two depots", becomes three, and
names the topology that owns `$$batch-clock`. After `:33-37` a paragraph says what the third depot
holds, who appends to it and when, and that it is never trimmed; the sentence "no code of the module
appends a lock's bytes" stays true.

**`module/microbatch_gate.clj`.** The head comment gains this plan. The namespace docstring: `:15-24`,
what it changes and borrows: it also appends to `*batch-clock-depot`, and borrows `$$batch-clock` to
read on the gate's task; "The gate keeps nothing in the memory of a process" stays. `:33-37`,
"Approach. The dataflow only reads rows, calls pure functions and writes rows": for a batch's clock it
also appends one record, and one function of the gate throws; "None of them throws, so no thing can
stop the gate" stays true of the pure code and of what arrived, and the paragraph says what the one
throw guards. `:56-61`, step 2, by the table of step 2. `:74-77`, "The stamp. One reading of the clock for a batch", becomes one clock
for a batch, kept, the same on every attempt. `:84-96`, "Closing": the reason, as on the store's page.
`:98-103`, "When Rama runs a batch again": the stamps are the same; a new lock's bytes are others; one
word can differ. `:105-109`, "Yielding": the wait for the ack, and what can run meanwhile. `:223-228`,
the docstring of `add-to`: what must hold.

**Other source files, a docstring each.**
`module/streaming_gate.clj:112-122`, `other-gates-record-holds-layer?`: "Rama can run that batch again,
and it could then refuse the making" becomes: the row would be the same on a second attempt; the
question stays held to closing so that this gate and `depot-of-layer` say the same of the base at one
moment. `module/door_reads.clj:40-52`: "with another clock and other lock bytes ... So an answer, a
layer's depot or a lock's bytes that a read gave never changes" becomes: with other lock bytes, and with
the other of two words for an act into a layer of the other gate; a stamp and a layer's depot would be
the same, and the three reads are held to closing as one rule. `record/rows.clj:248-256`, `closed?`: "A
read gives such a row ... only when this is true" becomes: the door's three reads and the streaming
gate's question. `record/tables.clj`: the namespace docstring gains the new schema and why it is one
row; `:30-32`, "a read gives no other", becomes the door's reads. `gate/batch.clj`: the namespace
docstring's line for step 2 (`:50-52`) names the three new functions. `gate/stamp.clj:3-4`: "the gate
reads the clock once for a decision and gives the number" becomes: a gate gives the number, the
streaming gate its one reading for the decision, the microbatch gate the batch's clock.

**Two head comments more** (ruling 39). `module/streaming_gate.clj` and `module/door_reads.clj` each
begin with the comment that tells whoever changes the file to read the store-module's plans again, and
this part changes a docstring in each. Each comment gains this plan. Ruling 39 counts three such files;
`door_reads.clj:1-3` is a fourth.

**The maps.** `module/README.md`: `:5-8` and the table gain the file `batch_clock.clj`, with its
responsibility and its state; the microbatch gate's row says that it borrows the kept clock and appends
to one depot; under "How the files relate", line 18, "Each gate's dataflow only reads rows, calls pure
functions and writes rows", is replaced as `:110-112` of the store's page is. `record/README.md:14` and
`gate/README.md:22`: the responsibility of each row gains what its file now also holds, the schema of
the kept clock and the pure code of a batch's clock. No function is named there: a map holds no list
of functions (`src-store/CLAUDE.md`). `src-store/README.md:6-8`, the tree's first paragraph only: "the
two depots" becomes the two depots of acts and what keeps a batch's clock.

**The proofs' maps.** `test-store/softland/store/README.md`: a group for the second attempt in the list
of the module's proof (`:211-311`), with the line that it runs on a cluster of its own, after the
proof's first cluster is closed, with three functions replaced, and that every other group runs on the
functions as the store defines them; `:325-329` replaced by "What the map then says", above; the pure
proof's list gains its cases; "The checks that rest on a time" gains the edge act, which changes what
the check shows and never whether it passes. `test-store/README.md`: "Neither shows ... a failure in
the middle of an event or of a batch" becomes: the third shows one failure of a batch while it is
processed, made by the proof, and none while a batch commits. The namespace docstring of
`module_proof.clj`: "State. The proof starts one in-process cluster" (`:29-37`) becomes two clusters,
one after the other, and says which checks run on which and that the hand's atoms are emptied between
them; its atoms, with the notes; the three replaced functions, in place for the second cluster only;
and that Rama prints lines marked ERROR for the throw, which are no failed checks (`:50-53`).

## Open points for the build session

1. **One stream topology, or two.** The skill's template says at most one. (a) The topology
   `batch-clock` of its own, as planned and as the probes ran it. (b) A second source of
   `streaming-gate`, with the row as two fields of `$$streaming-task`: no new topology and no new
   PState, and every batch of the base then waits on the streaming gate's queue on one task. The table
   is under "Topologies". Recommended: (a).
2. **What the table keeps.** (a) One row, the newest batch, as planned. (b) A row for each batch, for
   ever. The table is under "PState design". Recommended: (a); the depot keeps every record either way.
3. **The window on the store's own module.** (a) Left to the batch probe, said in the map and in the
   receipt. (b) A proof file of its own, as sketched under the proof. Recommended: (a).
4. **Settled by ruling 49: the check of the second attempt has a cluster of its own.** Every other
   check of the module's proof runs on the functions as the store defines them. The three replaced
   functions are in place only for that cluster and are put back after it. This plan had recommended
   the other way, the three in place for the whole run. Two clusters one after the other in one JVM
   were run (T): a later cluster took about 1.5 s to create and launch, and the store is still proven
   by its one command.
5. **The depot of batch clocks is never trimmed**, as the two depots of acts. Nothing reads its old
   records. Recommended: leave it so, and give it a line in the receipt: whether a log is trimmed is on
   Sid's next sheet.
6. **Rama's phase timeout for the microbatch gate.** (a) Left unset, as today: after a fault of the
   batch clock's topology on another worker than the one that runs the attempt, the base stands still
   for about 30 s (W5). (b) Set lower at the launch: a shorter stand, and a long batch can then fail
   with nothing wrong. Recommended: (a), with a line in the receipt; no run of this build says how long
   a real batch of the base takes on a cluster.

## For the receipt

- Ruling 16 (2): a reader can see a fact of the base up to one run before its maker reads "accepted"
  for it at the door.
- Ruling 16 (6): the depot of batch clocks is a first part of what rule 9 left to Sid. It holds each
  batch's clock and no cut.
- Ruling 17 (e): what was run where. On the store's own module: a second attempt of a batch that failed
  while it was processed (S; then the proof). On a module of the probe: the window, and what a reader is
  given across it. Not run: a killed process, a move of a leader, a real cluster, and what `:ack` waits
  for on more than one copy.
- Ruling 34, and one line more of Sid's to set beside it. 7 October, 1: "Two depots, one for each
  gate". 7 October, 2: "the record is recomputable from the acts depot ... so a second depot of
  decisions adds nothing. The acts depots are the replay log." The new depot holds no decision. It does
  hold a thing that the acts depots cannot give again, a reading of the clock, as each stamp did before
  it (the store's page, rule 19). Nobody asked him.
- The module has a second stream topology (open point 1).
- After a fault of the batch clock's topology, the base can stand still for Rama's phase timeout, about
  30 s here, before the batch is run again. No stamp is made meanwhile (W5; open point 6).
- A module update that changes the code from which a batch's rows are made is the one kind of moment
  at which two attempts of a batch can still differ, when it falls between the two. This build makes
  two such updates: the one that brings the repair, and the one that brings `what-the-gates-keep`.
- The one throw of the module's code, when a batch's clock cannot be read back after the ack: in the
  in-process cluster a throw starts the worker again, so it stops every task of that worker for that
  time, both gates and the reads, and it comes again on each attempt while its cause stays.
- A proposed row for the queue: is the log of batch clocks kept for ever, as the logs of acts are?

## Design difficulty log

- **The topology.** The hardest call, and not a forced one. I first took the probe's form, a topology
  of its own, as given. The skill's rule sent me to build the other form, and it is leaner: no new
  PState and no new topology. What settled it was reading where the wait stands. The keep puts a wait
  inside every batch of the base that has things, and in the other form that wait stands in the
  streaming gate's queue on one task. I could not put a number on the fixed cost of the empty
  partitions, and I say so. If the build session weighs that cost higher, (b) is a small change: the
  event and the three pure functions are the same.
- **One row, or a row for each batch.** Close. "Never delete data" pulled toward a row for each batch.
  What settled it: no read depends on a past batch's clock, the stamp of each row holds it, and the
  depot keeps every record. A row for each batch needs nothing of the order of the ids, which is its
  one real merit; I took one row because the order was run across an attempt, a restart and an update.
- **A road with no depot, weighed and left.** Each run could write its own reading of the clock into
  the gate's own PState, for the next batch to use: Rama sets it back with the rest, so each attempt
  would read the same. No depot, no topology, no wait. It was left for three reasons. Ruling 17 names
  the depot's road. A batch would then be stamped with the time of the run before it, which on a real
  cluster is up to a minute old when the gate was idle (B6.1), so the 10-second rule would refuse an
  act that carries a fresh stamp of the streaming gate. And every run, a run with nothing too, would
  pay a write. It is said here so that the build session knows that it exists.
- **Offer, then use what is kept.** My first walk used the reading that the attempt had just appended.
  The trace of a record that fails on its first try showed the hole: the row can hold an earlier
  attempt's reading. The walk now takes the clock only out of the row.
- **What the gate does when the row is not there after the ack.** No case that I traced reaches it. I
  kept the throw: the other choices are to go on with a clock that is not kept, which is the fault that
  this part exists to close, or to skip the batch, which loses its things.
- **Not contested:** stream and not microbatch for the keeping; `:ack` and no weaker level; the gate's
  task as the place; the pure functions in `gate/batch.clj`; the proof in the module's proof.
