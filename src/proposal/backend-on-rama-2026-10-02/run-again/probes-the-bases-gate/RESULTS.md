# What the probes of the base's gate printed

13 and 14 October 2026. Rama 1.6.0, in-process cluster, Clojure 1.12.4.

These are probes, not a design. No table, name or form here is a proposal for the store.

## What was asked

A gate will decide every act of a layer on ONE task. It will decide them one at a time, in the order of the layer's one depot partition. Each act is decided against the rows that the earlier acts wrote. Then the gate will copy each written row to EVERY task of the module.

Nine claims of Rama stand under that. Each was run here as a question.

## How to read this page

Every statement ends with one of two words.

- **run** means: a probe in this folder did it, and the log has the line.
- **read** means: Rama's docs or the Rama skill say it, and no probe here did it. The file and line are given.

A statement that follows from two run lines, and was not run by itself, says **derived**.

## How the probes were run

From this folder, one at a time, each behind the shared lock:

    flock ../../before-real-data/runs/cluster.lock clojure -M:<alias> > logs/<alias>-stdout.txt 2>&1

Each probe writes its own lines to `logs/<alias>.log`. The whole stdout of the run is in `logs/<alias>-stdout.txt`.

Every module ran with 4 tasks, 2 task threads and 2 workers, unless a claim says another number of tasks. In the runs that print it, tasks 0 and 1 shared one thread in one worker, and tasks 2 and 3 shared the other thread in the other worker (run: the line "Threads, by task" in `order.log`).

No option of Rama was set, with two exceptions that are named where they stand: one depot option in `:long-batch`, and a logging file in `:failed`.

Three things about the logs. A log named `...-first-run` or `...-second-run` is an earlier run of the same probe, kept. `logs/long-batch-did-not-compile-stdout.txt` is a first try of `:long-batch` that did not compile, because its module was named `Long`, which is a Java class. And `gate.clj` got two additions after the first runs: the line that writes the order of the decisions as runs, and the places `:back` and `:copy` for the throw of C5. The four steps of the walk were the same in every run.

## The stand-in gate

It is in `src/probe/gate.clj`. A microbatch topology `gate` reads a depot `*acts` declared `(hash-by :layer)`. A record is `{:layer :id :key :val :replaces}`. The walk for one record is the four steps of the brief:

1. Read `$$answers` at the id. If it is there, stop.
2. Read `$$rows` at `[layer key]`. Accept when `:replaces` equals the `:id` that the row holds. Else refuse.
3. n = the layer's last number + 1. Write `$$last`, the answer, and for an accept the row. All on this task.
4. For an accept: `|all`, and write the same row on every task except the deciding one.

The records of a layer are a chain. Record i is on key (i mod keys). It says that it replaces the record before it on that key. So one record that does not find its predecessor's row is refused, and every later record of that key's chain is refused after it.

What the probe added, and what it does not change:

- Two tables that only the probe reads: `$$trace` (the microbatch, the task at the source, the deciding task) and `$$attempts` (which attempt wrote a row).
- A stream topology `ping` and a table `$$here`. Every task writes its own number there. A client asks `$$here` under one candidate key after another to find a partitioning key that lands on each task. Every per-task read in these probes uses a key found that way.
- A journal in an atom, outside Rama: one entry for each decision and each copy, in the order in which each task ran them.
- Three switches by the layer's name: `noskip` (the deciding task writes its own copies too), `yield` (the walk calls `(yield-if-overtime)` before step 1), `slow` (the walk keeps the thread busy for 200 microseconds on every record).

---

## C1. The order inside a batch

**The question.** 3,000 records of one layer are appended without waiting for the gate. Does a later record of a microbatch read the rows that the earlier records of the same batch wrote?

**What was run.** Alias `:order`, log `logs/order.log` (a first run of the same probe is kept as `logs/order-first-run.log`). Alias `:long-batch`, log `logs/long-batch.log`.

**What it printed.**

    C1 VERDICT (c1-live): every record accepted, so every record read the rows of the records before it: yes. 3000 of 3000 accepted, in 87 microbatches, at most 99 in one.
    C1 VERDICT (c1-paused): every record accepted, so every record read the rows of the records before it: yes. 3000 of 3000 accepted, in 3 microbatches, at most 1000 in one.
    C1 VERDICT (c2-long): every record accepted, so every record read the rows of the records before it: yes. 20000 of 20000 accepted, in 20 microbatches, at most 1000 in one.
    LONG BATCH VERDICT (l1-long): 20000 of 20000 accepted, in 1 microbatches, at most 20000 in one. Decisions out of the depot's order: 0.

| layer | how it was appended | accepted | microbatches | most in one |
|---|---|---|---|---|
| c1-live | while the gate ran | 3,000 of 3,000 | 87 (first run: 88) | 99 (first run: 45) |
| c1-paused | while the gate was paused | 3,000 of 3,000 | 3 | 1,000 |
| c2-long | while the gate was paused | 20,000 of 20,000 | 20 | 1,000 |
| l1-long | paused, the depot's batch limit set to 20,000 | 20,000 of 20,000 | 1 | 20,000 |

**The statements.**

- Every record was accepted in all of these layers. Every record got the number of its place in the depot (1 to N). — run
- So inside one microbatch a later record read the row that an earlier record of the same batch had written on the deciding task. This held in a batch of 99, of 1,000 and of 20,000. — run
- With no option set, a microbatch took at most 1,000 records of the partition. — run. The option is `depot.microbatch.max.records`. — read (`reference/rama/docs/12-microbatch-topologies.md:156`)
- `(set-launch-depot-dynamic-option! setup *acts "depot.microbatch.max.records" 20000)` in the module made one microbatch of 20,000. — run
- All of this is with a walk that does not yield. With `(yield-if-overtime)` in the walk the answer is another one. See C2. — run

**What it means for the gate.** A walk that reads and writes on the deciding task, one record after the other, sees its own earlier writes inside the batch, as long as nothing in the walk yields.

---

## C2. The order of the copies on every task

**The question.** The deciding task sends each accepted row to every task with `|all`. Do the rows arrive on every task in the order in which they were sent? Does a yield in the deciding walk change it? Does a longer batch change it?

**What was run.** Alias `:order`, log `logs/order.log`. Alias `:yield`, log `logs/yield.log` (the yield looked at closer; a first run is kept as `logs/yield-first-run.log`). Alias `:long-batch`, log `logs/long-batch.log`.

The yield is one call of `(yield-if-overtime)` at the head of each record's walk, before step 1. No `loop<-` was used: the walk of one record has no loop.

For every layer the probe read `$$rows` on every task with one select. It counted the keys that hold the row of the last record of their chain, with the same number as on the deciding task. From the journal it counted, on every task, the copies that were written out of the order in which the deciding task accepted their records.

**What it printed, with no yield.**

    C2 VERDICT (c1-live): every task holds the last row of every key's chain: yes. Every task holds the deciding task's row for every key: yes. Every other task wrote every copy in the order sent: yes. Decisions out of the depot's order: 0.

The same line, with the same four answers, came for `c1-paused`, `c2-slow`, `c2-long`, and in `:long-batch` for `l1-long`.

| layer | records | on each of the 4 tasks: keys with the last row | keys with another row | copies written on each other task | of them out of the sent order |
|---|---|---|---|---|---|
| c1-live | 3,000 | 50 of 50 | 0 | 3,000 | 0 |
| c1-paused | 3,000 | 50 of 50 | 0 | 3,000 | 0 |
| c2-slow | 3,000 | 50 of 50 | 0 | 3,000 | 0 |
| c2-long (20 batches) | 20,000 | 50 of 50 | 0 | 20,000 | 0 |
| l1-long (1 batch) | 20,000 | 50 of 50 | 0 | 20,000 | 0 |

**What it printed, with a yield in the walk.**

    C1 VERDICT (c2-slow-yield): every record accepted, so every record read the rows of the records before it: NO. 1235 of 3000 accepted, in 3 microbatches, at most 1000 in one.
    C2 VERDICT (c2-slow-yield): every task holds the last row of every key's chain: NO. Every task holds the deciding task's row for every key: yes. Every other task wrote every copy in the order sent: yes. Decisions out of the depot's order: 3.
    The order in which the deciding task decided, as runs of records in the depot's order: 0..12, 14..55, 57..127, 129..149, 151..171, 173..193, 195..208, 210..225, 227..288, 290..329, 331..346, 348..368, 370..390, 392..433 ... (211 runs)

And from `logs/yield.log`, for a layer that came in short batches:

    C2 YIELD VERDICT (y4-slow-yield-live): accepted 3000 of 3000. Numbered 1 to 3000 in the depot's order: NO. Decisions out of the depot's order: 68. Every task holds the deciding task's row for every key: yes. Every other task wrote every copy in the order sent: yes.
    The order in which the deciding task decided, as runs of records in the depot's order: 0..30, 32..43, 31..31, 44..63, 65..78, 64..64, 79..97, 99..115, 98..98, 116..134, 136..149, 135..135, 150..166, 168..185 ... (209 runs)

| layer | what the walk does | accepted | decisions out of the depot's order |
|---|---|---|---|
| c2-yield | yield, nothing burnt, 3 batches of 1,000 | 3,000 of 3,000 | 0 |
| c2-long-yield | yield, nothing burnt, 20 batches of 1,000 | 20,000 of 20,000 | 0 |
| l2-long-yield | yield, nothing burnt, 1 batch of 20,000 | 20,000 of 20,000 | 0 |
| c2-slow-yield | yield, 200 microseconds burnt a record | 1,235 of 3,000 (first run: 1,166) | 3, one in each batch |
| y1-slow-yield | the same, 1 batch of 1,000 | 656 of 1,000 (first run: 662) | 1 |
| y4-slow-yield-live | the same, appended while the gate ran, 80 batches of at most 42 | 3,000 of 3,000, but not numbered in the depot's order | 68 (first run: 62) |
| y5-slow-yield | the same, 3 batches of 1,000 | 1,143 of 3,000 (first run: 1,373) | 3, one in each batch |
| c3-noskip-yield-r2 (from C3) | yield, nothing burnt | 2,984 of 3,000 | 1 |
| c3-noskip-long-yield (from C3) | yield, nothing burnt | 19,927 of 20,000 | 1 |

**The statements.**

- With no yield in the walk, every task held, for every key, the row of the last record of the key's chain, with the deciding task's number. — run
- With no yield, every other task wrote every copy, and wrote them in the order in which the deciding task had accepted the records. Not one copy out of order in 3,000, in 20,000 in 20 batches, or in 20,000 in one batch. — run
- This covers a copy to a task on the deciding task's own thread and a copy to a task in the other worker. — run
- "Partitioners ensure ordering is maintained between any two tasks." — read (`reference/rama/docs/16-partitioners.md:129`). The runs agree with it.
- A longer batch did not change it. — run
- A yield changed the order of the DECISIONS, not of the copies. When `(yield-if-overtime)` yielded in the walk of one record, the gate went on with the records that follow it in the batch and decided them first. The record that yielded was decided after them, at the batch's end. — run (the two lines of runs above: in the first, record 13 is missing between 0..12 and 14..55, and record 56 between 14..55 and 57..127; in the second, record 31 is decided after 32..43, and record 64 after 65..78)
- A record decided late does not find the row it replaces, or the records after it on its key do not. So records were refused: 1,765 of 3,000 in `c2-slow-yield`. — run
- The copies still arrived in the order in which the deciding task accepted, and every task still held the deciding task's row for every key. All tasks agreed on a state that is not the one the depot's order gives. — run
- In a layer that came in short batches (`y4-slow-yield-live`) every record was still accepted, because no batch held two records of one key. But 68 decisions were out of the depot's order, so the layer's numbers no longer followed the depot's order. — run
- The yield needs the walk to be over 5 ms in one event. — read (`reference/rama/docs/09-intermediate-dataflow.md:209`). With 200 microseconds burnt on every record it yielded in every run. With nothing burnt it did not change the order in 9 runs (of `c2-yield`, `c2-long-yield`, `y3-yield`, `l2-long-yield`, `c3-noskip-yield-r1` and `-r3`), and it did in 2 (the last two lines of the table). — run
- "Do NOT yield on a path where correctness depends on same-key events processing in order." — read (`.claude/skills/rama/references/dataflow.md:156`). The runs agree with it.

**What it means for the gate.** The copies can be trusted to arrive in the order sent; the deciding walk must not yield, because a yield lets later records of the batch be decided before an earlier one.

---

## C3. The deciding task's own copy

**The question.** Without the skip of step 4, the deciding task also writes the copies that `|all` brings back to it. Does any record get refused? Does any key end on an older row on the deciding task? Is the skip needed?

**What was run.** Alias `:own-copy`, log `logs/own-copy.log`: 14 layers, all without the skip. Alias `:long-batch`, log `logs/long-batch.log`: two more, each one microbatch of 20,000.

**What it printed.**

    C3 VERDICT: runs in which the deciding task decided every record in the depot's order: 9 of 14. Among them, a refused record, or a key that does not end on the last row of its chain, was seen in 0.
    C3 VERDICT: runs with decisions out of the depot's order (a yield that yielded): 5 of 14: ["c3-noskip-yield-r2" "c3-noskip-slow-yield-r1" "c3-noskip-slow-yield-r2" "c3-noskip-slow-yield-r3" "c3-noskip-long-yield"]. Among them a refused record was seen in 5. `probe.yield` shows the same with the skip on.
    C3 VERDICT: an own copy that set a key back to an older row for a time was seen in 13 of 14 runs. An own copy that came back between two decisions of one microbatch was seen in 0 of 14 runs.
    LONG BATCH VERDICT (l3-noskip-long): 20000 of 20000 accepted, in 1 microbatches, at most 20000 in one. Decisions out of the depot's order: 0. Every task holds the last row of every key's chain: yes. Every other task wrote every copy in the order sent: yes. Own copies that set a key back for a time: 50. Decisions made on an older row: 0. Stretches: 1.

| layers | records | refused | keys not on their last row at the end | own copies that set a key back for a time |
|---|---|---|---|---|
| c3-noskip-live-r1, r2, r3 | 3,000 each, live | 0, 0, 0 | 0, 0, 0 | 50, 50, 0 |
| c3-noskip-paused-r1, r2, r3 | 3,000 each, 3 batches | 0, 0, 0 | 0, 0, 0 | 150, 150, 150 |
| c3-noskip-yield-r1, r3 | 3,000 each, 3 batches | 0, 0 | 0, 0 | 150, 150 |
| c3-noskip-long | 20,000, 20 batches | 0 | 0 | 1,000 |
| l3-noskip-long, l4-noskip-long-r2 | 20,000 each, 1 batch | 0, 0 | 0, 0 | 50, 50 |
| c3-noskip-yield-r2 | 3,000 | 16 | 50 | 150 |
| c3-noskip-slow-yield-r1, r2, r3 | 3,000 each | 2,158, 1,883, 1,803 | 50 each | 66, 79, 82 |
| c3-noskip-long-yield | 20,000 | 73 | 1 | 997 |

**The statements.**

- A refused record, or a key that ends on an older row on the deciding task: not seen in 11 runs without the skip in which every decision was made in the depot's order (9 in `:own-copy`, 2 in `:long-batch`). — run
- In the 5 runs with a refused record the deciding task had decided out of the depot's order. That is the yield of C2. The same was seen with the skip on (`logs/yield.log`). So these 5 runs say nothing about the skip. — run, and the last sentence derived
- In all 16 runs every own copy of a microbatch came back to the deciding task only after the last decision of that microbatch. Not once between two decisions. This held in one microbatch of 20,000, whose walk took about 3 seconds. — run (the journal: the number of stretches equals the number of microbatches)
- Without the skip, the own copies do set keys back. The copy of an early record of a key arrives after a later record of that key was decided, and writes the older row over the newer one. The copies that follow put it right. This was seen in 15 of 16 runs. In the one run where it was not seen, no batch held two records of one key. — run (counted from the journal)
- No decision was made while a key stood on an older row, in those 11 runs. — run (counted from the journal)
- The next microbatch does not start before the current one has finished. — read (`.claude/skills/rama/references/microbatch.md:30`). So the own copies of one batch are all written before the first decision of the next. The runs agree: the rows were right at every batch's end.
- Why the own copies wait for the batch's last decision was not found out. It is what was seen, in 16 runs. The deciding task did serve a client's reads during a batch (in `l3-noskip-long` the slowest read waited 14.1 ms while the batch of 20,000 ran for 3,032 ms), so the batch's walk is not one event that holds the thread. — run for the numbers; the reason is not known

**What it means for the gate.** In these runs the skip was not needed for a right end state; without it the deciding task's own rows go back to older rows for a time inside every microbatch, and what keeps a decision out of that time is an order of events that was seen in 16 runs and that no doc read here promises.

---

## C4. Several layers on one task and on several

**The question.** Six layers, 500 records each, sent interleaved from 3 threads. Is every record accepted? Does every task hold the last row of every key of every layer? Does each layer's number run 1 to 500 with no gap, whatever other layer shares its task?

**What was run.** Alias `:layers`, log `logs/layers.log`. The six names were found so that two layers share depot partition 0, two share partition 3, and partitions 1 and 2 have one each. The two layers of one partition were sent from two threads. Each thread sent one record of each of its two layers in turn.

**What it printed.**

    C4 VERDICT: every record of every layer accepted, each layer numbered 1 to 500 in its depot order, and every task holds the last row of every key of every layer: yes

| layer | depot partition | decided on task | accepted | numbers 1 to 500, no gap, in depot order |
|---|---|---|---|---|
| layer06 | 0 | 0 | 500 of 500 | yes |
| layer20 | 0 | 0 | 500 of 500 | yes |
| layer00 | 1 | 1 | 500 of 500 | yes |
| layer08 | 2 | 2 | 500 of 500 | yes |
| layer05 | 3 | 3 | 500 of 500 | yes |
| layer12 | 3 | 3 | 500 of 500 | yes |

Every one of the 4 tasks held the last row of 60 of 60 keys, with the deciding task's number. The gate ran 42 microbatches.

**The statements.**

- Every record of every layer was accepted. — run
- Each layer's numbers were 1 to 500 with no gap and in the layer's depot order, also for the two pairs of layers that shared a task. — run
- Every task held the last row of every key of every layer. — run
- A layer's name lands on the same partition as a depot key (`hash-by :layer`) and as a read's partitioning key. The probe chose the layers by the second and the trace showed the first. — run

**What it means for the gate.** Layers that share a deciding task do not disturb each other's order or count when the count is kept by layer.

---

## C5. A failed attempt

**The question.** The gate throws once, on purpose, in the middle of a batch. After the retry, is every table on every task equal to a run with no failure? Is any row of the failed attempt left on any task? What did Rama log? Did a worker restart?

**What was run.** Alias `:failed`, log `logs/failed.log`. Two earlier runs are kept: `logs/failed-first-run.log` (the first case only) and `logs/failed-second-run.log` (three cases). The alias names `log4j2-info.xml`, so `logs/failed-stdout.txt` holds what Rama logged at INFO and above, each line behind the word RAMA.

Two modules ran the one gate over the same 1,500 records, each on its own depot. `Gate` threw once. `Twin` never threw. Records 500 to 1,099 were appended while both gates were paused, so they were ONE microbatch of 600. The throw was always at record 900, the 401st of that batch. The function that throws first sleeps 300 ms on its thread, then throws an `ex-info`.

**Where the throw stood.** Four cases, each on a cluster of its own.

| case | where exactly |
|---|---|
| `:walk` | on the deciding task (1), at the head of the walk of record 900, before step 1 |
| `:back` | on the deciding task (1), when the copy of record 900 came back to it through `|all`, where the skip stands |
| `:copy-near` | on task 0, which shares the deciding task's thread and worker, when the copy of record 900 arrived, before it was written |
| `:copy-far` | on task 2, in the other worker, when the copy of record 900 arrived, before it was written |

The brief asked for the throw "on the deciding task after some records were decided and copied". The case `:walk` is that place as written. But at that place no copy had been written yet on any task (see below). So the three other cases were added, where copies of the failed attempt were on the other tasks.

**What it printed.**

    C5 VERDICT (:walk): the gate threw once: yes . Decisions and copies that the failed attempt had made before the throw, all tasks: 400 . After the retry every table on every task equal to the module that did not fail: yes . Rows of the failed attempt left on any task: 0 .
    C5 VERDICT (:back): the gate threw once: yes . Decisions and copies that the failed attempt had made before the throw, all tasks: 2698 . After the retry every table on every task equal to the module that did not fail: yes . Rows of the failed attempt left on any task: 0 .
    C5 VERDICT (:copy-near): the gate threw once: yes . Decisions and copies that the failed attempt had made before the throw, all tasks: 2200 . After the retry every table on every task equal to the module that did not fail: yes . Rows of the failed attempt left on any task: 0 .
    C5 VERDICT (:copy-far): the gate threw once: yes . Decisions and copies that the failed attempt had made before the throw, all tasks: 2200 . After the retry every table on every task equal to the module that did not fail: yes . Rows of the failed attempt left on any task: 0 .

| case | failed attempt: decisions on the deciding task | failed attempt: copies written, by task | worker that died | from the throw to the retry's first decision | the batch took (threw / did not) |
|---|---|---|---|---|---|
| `:walk` | 400 | none | the deciding task's | 607 ms | 1,424 / 169 ms |
| `:back` | 600 | task 0: 498, task 2: 600, task 3: 600 | the deciding task's | 588 ms | 1,332 / 160 ms |
| `:copy-near` | 600 | task 0: 400, task 2: 600, task 3: 600 | the deciding task's | 597 ms | 1,316 / 169 ms |
| `:copy-far` | 600 | task 0: 600, task 2: 400 | the other one | 540 ms | 1,239 / 166 ms |

In every case the retry made 600 decisions on task 1 and wrote 600 copies on each of tasks 0, 2 and 3. At the end both modules had accepted 1,500 of 1,500, numbered 1 to 1,500 in the depot's order. `$$rows`, `$$answers` and `$$last` on every one of the 4 tasks held the same entries in both modules, 0 that differ. `$$attempts` held 0 entries of the failed attempt and 600 of the retry on every task. The gate never found a record already answered (step 1): 0 in the whole run.

**What Rama logged**, in every case, in this order (from `logs/failed-stdout.txt`; these are the lines of the case `:walk`, and the other cases differ only in the port of the worker that died):

    ERROR throwable-handler - Unexpected throwable! Will be treated as a fatal! {:event-tags [:microbatch :gate :topology-event]}
    clojure.lang.ExceptionInfo: PROBE: thrown on purpose, once, by the probe
    ERROR throwable-handler - Uncaught throwable in task thread outer loop {:event-tags nil}
    ERROR recurrent-task - Uncaught exception in watchable promise
    ERROR recurrent-task - Unexpected throwable in recurrent task {:description Worker bb1ad573 00000000@2005 DTT 0 0-queue-consumer}
    ERROR watchdog - One or more monitored components have become unhealthy! Triggering system shutdown. {:unhealthy-component-keys [:task-thread-systems]}
    ERROR core - Component :task-context-runner threw during unclean stop-async! ...
    WARN  worker - Failed to remove heartbeat ...
    WARN  worker-sync - Port 2005 process has died unexpectedly. Moving to IDLE {:port 2005, :pid 1}
    INFO  sim-worker-supervision - Launching worker in simulated mode {:port 2005, :module-name probe.gate/Gate, ...}

**The statements.**

- After the retry every table on every task was equal to the module that did not fail, in all four cases, and in the two earlier runs. — run
- No row of the failed attempt was left on any task. This held on tasks of the worker that died and on tasks of the worker that stayed up. In `:copy-far` the deciding task's worker stayed up, and its 600 decisions of the failed attempt were gone all the same. — run
- A worker did restart, in every case. A throw in the gate's code is "treated as a fatal" for the worker whose task thread ran it. That worker shut down, the supervisor logged that it died, and launched it again. The module's other worker did not die. — run
- The same microbatch was attempted again with the same 600 records: part B is in one microbatch id in the module that threw. — run. "Each attempt for the same microbatch ID processes the exact same set of data" and "each microbatch attempt starts with the PStates at exactly their state after the previous microbatch ID". — read (`reference/rama/docs/12-microbatch-topologies.md:116`)
- The failed batch cost about 1.1 to 1.3 seconds more than the one that did not fail. About 0.3 of that is the probe's own sleep. — run
- At the place `:walk`, with 400 records decided and 300 ms waited, no copy had been written on any other task. This came out the same in three runs. So the copies of a batch were not written on the other tasks while the deciding task had not yet finished deciding. — run for the count; the last sentence derived
- A walk that throws every time is retried without end and blocks the topology. — read (`.claude/skills/rama/references/microbatch.md:129`). Not run. Here each try would also kill a worker. — derived

**What it means for the gate.** A failed attempt leaves nothing behind on any task, on the deciding task or on a task that only holds copies; but a throw in the gate is not a quiet retry, it costs a worker restart of more than half a second each time.

---

## C6. Run again, the same number of tasks

**The question.** A second module mirrors `*acts` and runs the same gate from the depot's beginning. Is every table on every task equal to the first module's, for C1's and C4's records?

**What was run.** Alias `:again`, log `logs/again.log`. The first module decided C1's layer (3,000 records) and C4's six layers (500 each) as they arrived: 6,000 records. Then `Again` was launched with 4 tasks.

**What it printed.**

    first: 124 microbatches, at most 86 records in one.
    again: 4 microbatches, at most 3000 records in one.
    A record of depot partition p is emitted in the second module on task: {0 [0], 1 [1], 2 [2], 3 [3]}
    C6 VERDICT: every table on every task of the second module equal to the first module's: yes . Every record accepted in its depot order in the first: yes , in the second: yes .

| task | `$$rows` entries (first / second / differ) | `$$answers` | `$$last` |
|---|---|---|---|
| 0 | 110 / 110 / 0 | 1,000 / 1,000 / 0 | 2 / 2 / 0 |
| 1 | 110 / 110 / 0 | 500 / 500 / 0 | 1 / 1 / 0 |
| 2 | 110 / 110 / 0 | 500 / 500 / 0 | 1 / 1 / 0 |
| 3 | 110 / 110 / 0 | 4,000 / 4,000 / 0 | 3 / 3 / 0 |

**The statements.**

- Every table on every task of the second module was equal to the first module's. — run
- The first module decided the 6,000 records in 124 microbatches, the second in 4. The batches being cut in other places changed nothing. — run
- With the same number of tasks, a record of depot partition p was emitted in the second module on task p. — run

**What it means for the gate.** A run again over the same depot, with the same number of tasks, makes the same tables on the same tasks.

---

## C7. Run again, another number of tasks

**The question.** The same, with the second module on 6 tasks, and on 3, while the first has 4. Does Rama allow it? On which task of the second module is a record of depot partition p emitted? Are the tables the same, with the walk as it is and with `(|hash *layer)` at its head? Is each layer still decided in its depot order?

**What was run.** Alias `:other-tasks`, log `logs/other-tasks.log`. One cluster for each number: 6, 3, 8, 2.

**What it printed for 6 and for 3.** Rama refused. The error, the same for a module that owns its depot and for one that mirrors:

    org.apache.thrift.TException: Invalid config: Num tasks must be power of two | CAUSED BY: rpl.rama.generated.InvalidConfigException:  | CAUSED BY: rpl.rama.generated.InvalidConfigException:

    C7 VERDICT (6 tasks): NOT RUN. Rama refused to launch a module with 6 tasks.
    C7 VERDICT (3 tasks): NOT RUN. Rama refused to launch a module with 3 tasks.

So C7 could not be run as written. It was run with 8 and with 2, the nearest numbers that Rama takes.

**What it printed for 8 and for 2.**

    C7 VERDICT (8 tasks, no hop): Rama let a module with 8 tasks mirror and consume the depot of a module with 4. Same rows as the first module's, every task holds every row, each layer in its depot order: yes.
    C7 VERDICT (8 tasks, hop): same rows as the first module's, every task holds every row, each layer in its depot order: yes.
    C7 VERDICT (2 tasks, no hop): Rama let a module with 2 tasks mirror and consume the depot of a module with 4. Same rows as the first module's, every task holds every row, each layer in its depot order: yes.
    C7 VERDICT (2 tasks, hop): same rows as the first module's, every task holds every row, each layer in its depot order: yes.

On which task a record of depot partition p was emitted in the second module:

| second module | partition 0 | partition 1 | partition 2 | partition 3 |
|---|---|---|---|---|
| 8 tasks | task 0 | task 1 | task 2 | task 3 |
| 2 tasks | task 0 | task 0 | task 1 | task 1 |

On which task each layer was decided:

| layer (depot partition) | first, 4 tasks | 8 tasks, no hop | 8 tasks, hop | 2 tasks, no hop | 2 tasks, hop |
|---|---|---|---|---|---|
| c1 (3) | 3 | 3 | 7 | 1 | 1 |
| layer06 (0) | 0 | 0 | 0 | 0 | 0 |
| layer20 (0) | 0 | 0 | 0 | 0 | 0 |
| layer00 (1) | 1 | 1 | 1 | 0 | 1 |
| layer08 (2) | 2 | 2 | 6 | 1 | 0 |
| layer05 (3) | 3 | 3 | 7 | 1 | 1 |
| layer12 (3) | 3 | 3 | 7 | 1 | 1 |

In all four runs: every task of the second module held 110 rows, 0 that differ from the first module's 110. `$$answers` of all tasks as one set: 0 differ of 6,000, and no answer on two tasks. `$$last` as one set: 0 differ of 7. Every layer accepted in full and numbered in its depot order. Each second module decided in 4 microbatches, at most 3,000 records in one.

**The statements.**

- Rama refuses to launch a module with 6 tasks or with 3 tasks. — run. "The number of tasks specified must be a power of two." — read (`reference/rama/docs/19-operating-rama.md:242`)
- Rama lets a module with 8 tasks, and one with 2, mirror and consume a depot of a module with 4. — run
- With 8 tasks, depot partition p was emitted on task p, so tasks 4 to 7 got no partition (derived). With 2 tasks, partitions 0 and 1 were emitted on task 0, and partitions 2 and 3 on task 1. — run. Every partition of a mirrored depot goes to one task of the module, and a task can get several or none. — read (`reference/rama/docs/18-module-dependencies.md:108`)
- With the walk as it is, the tables' rows were the same as the first module's as sets, every task held every row, and each layer was decided in its depot order. — run
- With `(|hash *layer)` at the head, the same. The hop from the source's task to the layer's task kept each layer's order. — run
- With the walk as it is, a layer's answers and its last number are on the task that got its depot partition. That is not always the task that the layer's name lands on in that module: `c1` was decided on task 3 of 8, and its name lands on task 7 of 8; `layer00` was decided on task 0 of 2, and its name lands on task 1 of 2. With the hop they are on the task of the layer's name. — run
- Two layers from two depot partitions that are decided on one task in the second module still each kept their own order and count. — run

**What it means for the gate.** A run again on another number of tasks gives the same rows on every task, but only a power of two can be asked for, and without a hop the deciding task of a layer is the task that got its depot partition, not the task of its name.

---

## C8. A fresh module with another number of tasks, fed the same records

**The question.** A client reads the records of each partition of the first module's depot in their order and appends them to a freshly launched copy of the module with 6 tasks. Are the tables the same as sets of rows?

**What was run.** Alias `:other-tasks`, log `logs/other-tasks.log`, in the same clusters as C7. The client used `foreign-depot-partition-info` and `foreign-depot-read`, 500 records at a time, one thread for each of the 4 source partitions, all four at once.

**What it printed.**

    C8 VERDICT (6 tasks): NOT RUN. Rama refused to launch a module with 6 tasks.
    C8 VERDICT (3 tasks): NOT RUN. Rama refused to launch a module with 3 tasks.
    Records read from the first depot, by partition: [1000 500 500 4000] . Appended to the fresh module: 6000
    C8 VERDICT (8 tasks): same rows as the first module's, every task holds every row, each layer in its order: yes.
    C8 VERDICT (2 tasks): same rows as the first module's, every task holds every row, each layer in its order: yes.

| fresh module | microbatches | `$$rows` on each task against the first's 110 | `$$answers` as one set | `$$last` as one set |
|---|---|---|---|---|
| 8 tasks | 96, at most 165 in one | 110 held, 0 differ, on each of 8 tasks | 0 differ of 6,000 | 0 differ of 7 |
| 2 tasks | 139, at most 106 in one | 110 held, 0 differ, on each of 2 tasks | 0 differ of 6,000 | 0 differ of 7 |

**The statements.**

- With 6 tasks, as written: not run. Rama refused the launch, with the error of C7. — run
- A client can read a depot partition in its order with `foreign-depot-partition-info` and `foreign-depot-read`. — run. The calls are described in `reference/rama/docs/28-clj-defining-modules.md:423` and `:430`. — read
- A fresh module with 8 tasks, and one with 2, fed these records, made the same rows as the first module's, on every task, with each layer in its order. — run
- In the fresh module each layer was emitted and decided on the task of its name (for example `c1` on task 7 of 8). — run

**What it means for the gate.** The records of a depot, read by a client and appended to a module with another number of tasks, give the same tables, as long as each layer's records are appended in their order.

---

## C9. What a copy costs

**The question.** With 4 tasks: how long from the append until the row is readable on the deciding task, and on another task? One record at a time, 100 records. An order of size only.

**What was run.** Alias `:cost`, log `logs/cost.log`. For each record the clock started, the record was appended (waiting only for the depot), and one thread for each of the four tasks read that task's `$$rows`, 0.2 ms apart, until the row held the record's id. Two series of 100.

**What it printed.** Times in ms, from the start of the clock.

| series | until the append came back | readable on task 0 (deciding) | task 1 | task 2 | task 3 |
|---|---|---|---|---|---|
| the next record at once: least / middle / most | 3.2 / 4.0 / 8.4 | 81.9 / 87.1 / 278.8 | 81.9 / 87.2 / 278.9 | 78.6 / 84.7 / 291.2 | 78.6 / 84.7 / 291.2 |
| the next after 500 ms of rest: least / middle / most | 1.9 / 2.4 / 10.8 | 88.9 / 239.4 / 335.9 | 88.9 / 239.5 / 336.2 | 87.8 / 237.4 / 333.0 | 87.7 / 237.5 / 333.0 |

**The statements.**

- One record at a time, the row was readable about 80 to 90 ms after the append in the middle case, when the next record followed at once. — run
- When the gate had rested for 500 ms before the record came, the middle was about 240 ms and the most about 335 ms. — run
- The row was readable on the other tasks at the same time as on the deciding task, within a few ms either way. In these runs the copy did not add a time that can be told apart from the rest. — run
- An idle microbatch topology looks for new data every 200 ms by default. — read (`.claude/skills/rama/references/microbatch.md:34`). That would explain the higher middle after a rest. It was not tested.
- The numbers include the append, the wait for the next microbatch, the walk, the copy, the commit, and the reader's 0.2 ms step. The gate here also wrote the probe's own tables. One cluster in one process on one machine. — run

**What it means for the gate.** A copy to every task costs no time of its own that shows; what a writer waits for is the microbatch, about a tenth to a third of a second for a single record.

---

## Not settled

- **6 tasks and 3 tasks.** Rama does not launch them. C7 and C8 were run with 8 and 2.
- **Why the deciding task's own copies come back only after the batch's last decision.** Seen in 16 runs, and in the three runs of the case `:walk` of C5. No doc that was read says it. A test that tries to break it was not built.
- **When a yield with nothing burnt yields.** It changed the order in 2 runs and not in 9. What made the event go over 5 ms in those two was not looked for.
- **Other shapes of threads and workers.** Everything ran on 4 tasks over 2 threads in 2 workers. One task to a thread, or all tasks on one thread, was not run.
- **A real cluster.** All runs are in one process, with a replication factor of 1 (read: `.claude/skills/rama/references/testing.md:83`). Several machines, replicas, and a change of leader were not run.
- **A failure in the commit phase, a timeout, or a worker killed from outside.** Only a throw in the gate's own code was run.
- **A throw that comes every time.** Read, not run.
- **The cost under load.** C9 is one record at a time on an idle module.
- **More than one depot partition feeding one deciding task with a hop, under a yield or a failure.** C7 ran the hop only with a walk that does not yield and does not fail.
- **A yield inside a `loop<-` in the walk.** The yield that was run is one call at the head of each record's walk.
- **A batch longer than 20,000 records.**
