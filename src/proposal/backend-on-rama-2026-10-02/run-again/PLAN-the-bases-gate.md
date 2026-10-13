# Plan: the base's gate

The one plan of the build "run again". Written by the build session (Opus 5.5, effort
high) in the night of 13 to 14 October 2026, after the runs of
`probes-the-bases-gate/` and before any line of the gate was changed. It is a plan: the
code, once written, is what holds, and its docstrings say what it does.

## The problem

The store has to come out the same when it is run again from its depots. For the base
it does not. The first proof showed it on the store as it was
(`runs/first-proof-run-again.txt`): the first module decided 2,067 things of the base in
40 batches and the run again in 1, and

- the run again answered the give to Alice before the act that makes the base, so 341
  facts of the base were in the first module only;
- two acts that write one place swapped;
- every stamp differed, because the stamp was made of a clock that the gate read;
- a mark held "from the batch after", and the run again had one batch, so no fact of
  the base was in the order by value.

All four have one cause. The base's acts were spread over the depot's partitions by the
entity of their first fact, and Rama gives an order inside one partition only. The gate
made an order of its own for each batch, and a batch is cut where the gate happens to
run.

## Sid's decisions that the plan builds

"2. There is local ordering: the order Rama gives inside one partition." · "3. Every act
is placed by its layer key, the base too. A layer is one line." · "4. The base's rows: a
copy on every task. One task decides, in the line's order." · "5. ... The depot of batch
clocks goes. Two depots stay." · "1. ... No gate reads a clock."

## The gate

**The depot.** `*microbatch-depot` partitions by the thing's `:layer`, as the streaming
depot does. A layer is one partition, so its acts have the depot's order.

**One task decides.** A record of partition p is emitted on task p, which is the
layer's task. The gate decides it there, at once, with the walk that the streaming gate
has: the test for a stray, check (a), the digest, the declaration, the time on the act,
the standing, the places, the marked keys, the layer's last stamp, and the writes. Every
read and every write of the decision is on that task. The next record of the partition
is decided when this one's rows are written, so it reads them.

**One walk for both gates.** The walk is the same in both gates but for the names of the
tables and the gate's word. So it is written once, as one operation that is handed the
four tables and the word, in a file of its own (`module/walk.clj`), and both gates call
it. It gives back the rows of an accept that a gate may copy: the rows of the facts and
of the facts by value.

**A copy on every task.** After an accept the gate goes to every task and writes those
rows there, in the order in which the walk gave them. The deciding task has them
already and is passed over. The answer, the layer's last stamp and a stray's digest stay
on the deciding task alone: a maker reads her answer on the layer's task, as in a
streaming layer.

**The stamp.** The greatest of the time on the act, the layer's last stamp plus one, and
the greatest carried stamp plus one. Nothing of a batch is in it. `$$microbatch-task`
has the form of `$$streaming-task`: the strays, and each layer's last stamp.

**The marks.** An act is decided with the layer's marks as they were before it, and a
newly marked key gets the stamp of the act that marks it. "From the batch after" is
gone.

**A layer of the other gate.** No table of the streaming gate is read. The word comes
from the act alone, as part A did it for the streaming gate.

## What goes

- The depot of batch clocks, its topology (`module/batch_clock.clj`), its table and the
  throw `clock-must-be-kept`.
- The microbatch gate's depot of answers, `:to-keep`, `:to-close`, and the closing of a
  batch with `:closed-through`.
- `:turns`, the three temporary tables of questions, replies and parcels, the meeting
  task, the keeping task, and the whole of `gate/batch.clj`.
- In `record/rows.clj`: `answer-record`, `stray-record`, `log-record`, `closed?`,
  `by-value-route`. In `record/tables.clj`: `microbatch-task` and `batch-clock`.

## The door's reads

- The routing id of an act-id is its layer, for both depots.
- `answer-by-act-id` and `depot-of-layer` read one row on the layer's task and give it.
  Nothing is held to a closed batch: a second attempt of a batch makes every row as the
  first made it (claim 5), so a row that a read gave does not change.

## The read path

Every row of a microbatch layer's facts, and of its facts by value, is on every task. So
a read of such a layer needs one task and not all of them. It goes to the task of the
layer that the read starts from: an asker reads the base where her own layer lives. A
read that starts from the base itself goes to the base's task.

What goes with it: the asking of every task for a run across entities, the one routing
id for each task that a class of Rama's gave, the route by the digest of a value, the
route by the entity, and the rule that takes the newest of several tasks' parts.

## The Rama claims, and what the runs gave

Each was run on a stand-in gate of this form before the plan was written
(`probes-the-bases-gate/RESULTS.md`). "Run" means that a run printed it.

1. **A later record of a batch reads the rows of the records before it.** Run: 3,000
   records of one layer, each replacing the one before it on its key, were all
   accepted, when the gate took them in 88 batches and when it took them in 3.
2. **Rows sent to every task from one task are written there in the order in which they
   were sent.** Run: every task ended on the last row of every key, and wrote every
   copy in the order sent. Read: "Partitioners ensure ordering is maintained between
   any two tasks" (`reference/rama/docs/16-partitioners.md:129`).
3. **The deciding task must not write its own copies.** Run: without the skip, a copy
   that came back set a key on the deciding task back to an older row for a time, in 13
   of 14 runs. No decision read such a row in those runs. The skip costs nothing and
   takes the question away.
4. **The walk must never give its task back.** Run: with a yield in the walk that did
   yield, records were decided out of the depot's order and refused (662 of 1,000
   accepted in one run). So the walk has no yield, no read that may yield, and no
   partitioner before its last write. An act has a bounded number of facts, so each of
   its loops ends soon.
5. **A failed attempt of a batch leaves nothing, on any task, and the retry makes the
   same tables.** Run: a throw in the middle of a batch; after the retry every table on
   every task was equal to a module that did not fail.
6. **A second module over the same depot from its beginning makes the same tables.**
   Run, with 4 tasks in both.
7. **Another number of tasks.** Run: Rama launches a module only with a number of tasks
   that is a power of two ("Invalid config: Num tasks must be power of two"). A fresh
   module with 8 tasks, and one with 2, fed the same records with each layer's records
   in their order, held the same rows on every task. A module with 8 tasks that mirrors
   the depot of one with 4 did too.
8. **Several layers on one task count apart.** Run: six layers, 500 records each, each
   layer numbered 1 to 500 with no gap.

## What the gate rests on, and does not test at run time

- The depot's partitioner puts a thing on its layer's task. A second module that
  mirrors the depot with ANOTHER number of tasks gets partition p on its task p, which
  is then not the task of the layer's hash: its answers would be on a task where the
  door's read does not look. Rama's own way to add tasks (`19-operating-rama.md`, "Task
  scaling") has the new module copy the depots into depots of its own. That is not
  built here. The proof of "another number of tasks" is a fresh store fed the kept acts.
- That no code path of the walk yields. The proof holds it by deciding a chain of acts
  in one batch.

## How it is built

1. The walk as one operation, and the gate, are written and run on a module of their
   own (`draft-the-bases-gate/`) while part A's proofs are adapted: the walk file is new
   and nothing of the store loads it yet.
2. When part A is committed: the switch. The streaming gate calls the walk; the
   microbatch gate's file is written again; the module, the tables, the rows, the
   door's reads and the read path change; the files that go are taken out with
   `git rm`.
3. The proofs are adapted. The module's proof gets the base's checks anew: a chain in
   one batch, the copies on every task, a failed attempt, a mark and a fact after it,
   two layers. The run-again proof must pass. Then the fixture is made.
