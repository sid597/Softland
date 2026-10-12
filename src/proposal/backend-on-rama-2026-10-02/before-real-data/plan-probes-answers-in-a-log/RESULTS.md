# The keeping of an answer, in the shape that the plan gives each gate — runs of 13 October 2026

The plan of the part `answers-in-a-log` (`../PLAN-answers-in-a-log.md`) turns on three things that the two
probes before it did not run. This folder runs them. Each is a probe: its module stands in for a gate, it
requires nothing of the store's tree, and no table, name or form here is a proposal for the store.

**Blocked:** nothing. What these runs do not reach is under "Not reached".

- Rama 1.6.0, its in-process cluster, one machine, one JVM for the module and the hand. Launched as the
  store's proofs launch theirs: 4 tasks, 2 threads, 2 workers.
- Run again, from this folder, one at a time, each behind the build's lock:
  `flock ../runs/cluster.lock clojure -M:stream`, `-M:batch`, `-M:one`. Each takes under a minute.
- Each run writes its own lines to `logs/<name>.log`, every line with the milliseconds since the JVM loaded
  the probe. `logs/stream-rama-lines.log` and `logs/batch-rama-lines.log` hold Rama's own ERROR lines of
  those two runs, each distinct line once with its count, cut to 300 characters. Rama's other output was
  not kept.
- S and O ran once. M ran three times with the same outcomes. The first two were of an earlier form of
  its walk, in which each task kept and appended its own; the second of them is
  `logs/batch-each-task.log` (in the first the thread that watches from outside read nothing, and its
  line said so). The third is of the form that the plan takes, one keeping task for a batch, and is
  `logs/batch.log`.
- Who ran it: the session that wrote the plan (Opus 5.5), in the night of 12 to 13 October 2026. `.cpcache/`
  in this folder is what `clojure` made when it ran here.

## The short of it

1. **S, the streaming shape: write, commit boundary, append with `:append-ack`, behind a first read of
   whether the key has a row.** In 60 runs of an event for 28 source records, with four forced throws,
   every record of the second depot held the row that the table holds under its id (57 of 57), and every
   row had a record (26 of 26). No record of a failed attempt was ever one that the table does not hold.
2. **A throw between the commit and the append leaves a row with no record, and the next run of the
   source record repairs it.** The retry found the row, wrote nothing, and appended a copy (S2).
3. **Each run of an event for a record that is already answered appends one copy.** After four restarts
   of one worker with no progress written between them, an id of that worker that was answered before
   the first has five records, all the same (S, "WHOLE").
4. **M, the microbatch shape: what a batch decided is kept in the topology's own table in the attempt
   that writes it, on one task for the batch, and the next run appends it and takes it out.** An attempt that failed after its
   writes left nothing in the depot; the attempt that counted left each answer once (M1). A run that
   failed after its appends left each of those answers twice, the same both times (M2, M3). In all: 21
   kept answers of 16 ids, none that differs from the table's row, no row without one.
5. **A record of the microbatch shape comes after its row.** From outside, the row of one id was first
   given 147 ms before its record was in the depot (M0).
6. **O: Rama's setting `topology.stream.max.executing.per.task` can be set to 1 from outside a module's
   own code**, by a second module that calls the first one's definition and sets the option at the
   launch. With it, one record of a task was pending at a time, where 399 were without it.

## S: the streaming shape (`logs/stream.log`)

The module (`src/probe/stream.clj`): one stream topology, source `{:start-from :beginning :retry-mode
:all-after}`. The event reads the row under the thing's id. When there is none it reads a number of the
task, writes the row and the number, and keeps the row; when there is one it writes nothing and keeps a
copy of it. Then `(|direct (ops/current-task-id))`, then
`(depot-partition-append! *kept record :append-ack)`. A row holds which source record and which attempt
wrote it; a record holds the row, and which source record and attempt appended it.

| | What was forced | The table after | The second depot after |
|---|---|---|---|
| S0 | nothing; 20 things | each row by attempt 1 | one record each |
| S1 | a throw after the writes, before the boundary, attempt 1 | the row of attempt 2: attempt 1's was dropped | one record, attempt 2's. None of attempt 1 |
| S2 | a throw after the boundary, before the append, attempt 1 | the row of attempt 1: it was committed | one record, a copy that attempt 2 appended |
| S3 | a throw after the append, attempt 1 | the row of attempt 1 | two records: attempt 1's, and attempt 2's copy. The same row in both |
| S4 | two source records under one id, sent together | the row of the one that ran first | two records: the first's, and the second's copy |
| S5 | X and Y of one task sent together; Y throws before its writes | both rows | one record each |
| S6 | the thing of S3 sent again, a new source record | unchanged | four records under that id: the two of S3, one more from a replay after a later restart, and the new record's copy. The same row in all |

The lines for S2 and S3:

```
    4592.7  INSIDE  F2 attempt 1 THROWS on purpose at after-boundary
    6381.9  RESULT  F2 · ready: true · the table holds: {:stamp 7, :by-rid "F2", :by-attempt 1} · records: 1 [[:copy "row by F2#1 stamp 7" "appended by F2#2" "p0@17"]] · every record holds the table's row: true
    7193.6  INSIDE  F3 attempt 1 THROWS on purpose at after-append
   12756.1  RESULT  F3 · ... records: 2 [[:decided "row by F3#1 stamp 8" "appended by F3#1" "p0@18"] [:copy "row by F3#1 stamp 8" "appended by F3#2" "p0@26"]] · every record holds the table's row: true
```

The whole run:

```
   33960.9  WHOLE   ids with a row: 26 of 26 · records in the second depot: 57 · records that hold another row than the table's: 0 () · rows with no record: 0 ()
   33962.1  WHOLE   how many ids have 1, 2, 3 ... records: {1 17, 3 1, 4 3, 5 5}
   33962.2  WHOLE   events begun in all: 60 for 28 source records · forced throws: 4
```

- 60 events began. Three of them threw before their append (S1, S2, S5). The other 57 each appended one
  record: 28 first runs less those three, and the rest copies. So the count of records is the count of
  events that reached the append, and nothing else.
- The five ids with five records are things of S0 on the worker that was started again four times: one
  record from their first run and one copy from each replay. Rama's line for each throw is the one that
  the stream probe saw ("Triggering system shutdown", `logs/stream-rama-lines.log`, 4 times). The 17 ids
  with one record were on the other worker, or were sent after the last throw.
- In S5, Y reached the depot before X (the two were sent from two threads), so the run says nothing of a
  later record that is decided before a failed one.
- The client's `:ack` append threw `CallbackException: Callback failure` for each thing whose first event
  threw. For F2 and F3 the row was committed all the same.

## M: the microbatch shape (`logs/batch.log`)

The module (`src/probe/batch.clj`): one microbatch topology, three batch blocks. One task keeps what a
batch decided: the task that the hash of the batch's id gives. Block 3 writes a row for each thing on the
task of its id, and sends what is to keep of them to that task, which puts it into a table of the
topology under a key of this batch and of the sending task. Block 2 of the next run goes from one fixed
task, which holds the id of the batch before when that batch left something, to that batch's keeping
task. There it reads the keys of that batch, puts their rows together into records of two rows at most,
appends each record with `:append-ack`, and takes the keys out. A thing marked as a stray is sent in
block 1 to the batch's keeping task. The depot of things gives a batch two records of one partition at
most, so that a run can both close a batch and hold things. A row holds the number of the attempt that
wrote it.

| | What was forced | The table after | The second depot after |
|---|---|---|---|
| M0 | nothing; 7 things and a stray | rows of attempt 1 | each answer once, the stray once |
| M1 | the attempt that writes throws after the rows are written and what to keep is put | rows of attempt 2 | each answer once, of attempt 2. Nothing of attempt 1. The stray once |
| M2 | the run that closes a batch throws after its appends | rows of attempt 1 (their batch had committed) | each answer twice, the same |
| M3 | four things of one partition, so two batches; the run that closes the first holds a stray and a thing that throws after its writes | the first two: attempt 1; the failing thing: attempt 2 | the first two twice each, the same; the failing thing once, of attempt 2; the stray once |

The lines for M3. Run 12 closes batch 11 and holds things of its own. Its first attempt appends what
batch 11 left, and nothing of its own, and then fails; its second attempt appends batch 11's again; run 13
appends what batch 12 left:

```
    9877.2  INSIDE  run 12 attempt 1 task 3 : read 2 key(s), of the batches (11) , appended 1 record(s) and took the keys out · 2.74 ms for the appends
    9903.4  INSIDE  batch 12 attempt 1 task 0 THROWS on purpose, after the rows were written and what to keep of them was put
   10509.6  INSIDE  run 12 attempt 2 task 3 : read 2 key(s), of the batches (11) , appended 1 record(s) and took the keys out · 2.61 ms for the appends
   10701.8  INSIDE  run 13 attempt 1 task 0 : read 2 key(s), of the batches (12) , appended 1 record(s) and took the keys out · 2.97 ms for the appends
   11114.4  RESULT  FAIL-2 · ready: true · the table holds: {:stamp 12000, :batch 12, :attempt 2} · kept answers: 1 [["attempt 2" "p0@4"]] · each holds the table's row: true
```

The whole run:

```
   13616.1  QUIET   the end of each partition of the depot: [5 0 0 8] · 2.5 s later, with nothing sent: [5 0 0 8]
   13619.8  WHOLE   records in the depot: 13 · kept answers: 21 of 16 ids · kept answers that hold another row than the table's: 0 () · rows with no kept answer: () · kept strays: {"stray-0" 1, "stray-3" 1, "stray-1" 1}
   13620.9  WHOLE   how many ids have 1, 2, 3 ... kept answers: {1 11, 2 5}
   13621.2  COST    the appends of one task in one closing, [records ms]: [[3 9.92] [1 2.79] [2 5.91] [2 5.13] [2 4.84] [1 2.74] [1 2.61] [1 2.97]]
```

- A run that has nothing to close appends nothing: 2.5 s of idle runs added no record.
- One append with `:append-ack` from inside an attempt took 2.6 to 3.0 ms once the JVM was warm, and two
  took 4.8 to 5.9 ms: they are made one after the other on the one task. The batch probe measured 1.4 ms
  for the same ack level on one task.
- The first closing read 4 keys, from four tasks that had written rows, and appended 3 records: 6 rows,
  two to a record, and the stray in the first.
- The six batches with things fell on two of the four tasks (`[5 0 0 8]`). Six say nothing of how the
  hash of a batch's id spreads batches over tasks; that was not run.
- From outside: `SEEN A0: its row was first given at 3986.6 ms and its record was first in the depot at
  4134.1 ms · the record came 147.5 ms after the row`.

**The earlier form, `logs/batch-each-task.log`.** In it each task kept what it had written in its own
part of the table, and in the next run every task read its own part, appended and took out. The same
four scenarios gave the same outcomes: 21 kept answers of 16 ids, none that differs, no row without one;
the row of A0 134.2 ms before its record; one append 2.8 to 4.2 ms, the tasks side by side. Its source is
`src/probe/batch_each_task.clj` (`-M:batch-each-task`); the plan says how the two differ and why it takes
the later one.

## O: the setting at 1, from outside the module (`logs/one.log`)

```
    3139.0  ONE     the module as it is defined · module probe.stream/KeepStream · 400 things for one task, sent at once with :ack: all answered after 219.2 ms · ... the most records of one task pending at once: 399 ...
    5828.7  ONE     the same definition, with topology.stream.max.executing.per.task set to 1 at the launch · module probe.stream/KeepStreamAtOne · ... all answered after 1568.3 ms · ... the most records of one task pending at once: 1 ...
```

- The second module is three lines: it calls `(.define KeepStream setup topologies)` and then
  `(set-launch-topology-dynamic-option! setup "gate" "topology.stream.max.executing.per.task" 1)`. The
  first module's code does not name the option. The second module has a name of its own, so a client
  opens it by that name.
- The in-process cluster has no call that changes a dynamic option of a launched module (read, from the
  loaded jar: `com.rpl.rama.test.InProcessCluster` has launch, update, destroy, pause, resume and wait).
- With the setting at 1 and the keeping in the event, 400 things of one task took 1,568 ms, about 255 a
  second, against 219 ms without it. One machine, one run: an order of size.

Two more lines of that log give the bytes that Nippy makes of a record of each kind. An accepted act: 131
to 136. A refused act with a word of 25 letters: 172. A stray: 110. What a batch keeps of itself: 55. The
line for 256 acts in one record (5,394 bytes) is of digests that are all zeros, which Nippy packs; a real
record holds 32 random bytes and a random uuid for each act, so it is not below 14 KB. Rama's own writing
of a record to a depot was not measured.

## Not reached

- A real cluster, a killed process, a move of a task's leader, replication: one JVM, one copy.
- A failure of the commit itself, in either shape: every failure here is a throw in the module's code.
  For a batch, the batch probe ran a commit that fails after three tasks showed their part (its B7).
- An append whose ack does not come: every append here was answered.
- A later source record of a task that commits before a failed one is run again.
- When a stream topology writes its progress, and so how many records one restart runs again.
- The store's own gates: these modules stand in for them.
