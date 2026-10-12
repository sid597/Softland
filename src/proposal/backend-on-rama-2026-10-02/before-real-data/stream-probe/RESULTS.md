# An append to a depot from inside an event of a stream topology — runs of 12 October 2026

The store is about to gain a log of what each gate decided: a depot that a gate
appends to from inside its own processing. Before a plan picks a form for that
log it needs facts about an append to a depot from inside an event of a stream
topology. The same was run for a microbatch topology earlier on this day
(`reads/batch-probe/RESULTS.md`). These are the runs for a stream topology.

**Blocked:** nothing. Three things could not be produced; they are named under
"Not settled by these runs".

- Rama 1.6.0, its in-process cluster, one machine, one JVM for the module and
  for the watchers. 4 tasks, 4 threads, so that each task has a thread of its
  own.
- A probe, not a design. Its module (`src/probe/gate.clj`) stands in for one
  shape: one stream topology, source `{:start-from :beginning :retry-mode
  :all-after}`, a client that appends with `:ack`, an event that stays on its
  task, reads three rows, writes three rows of its own PState, and appends one
  record to a second depot declared `:disallow`. It requires nothing of the
  store's tree, of the rig, or of any other tree. No table, name or form here
  is a proposal for the store.
- "The twelve ways" of the append, used throughout: its place (write, then
  append; or append, then write) × a commit boundary
  `(|direct (ops/current-task-id))` right before the append or none × the ack
  level `:append-ack`, `:ack` or none (`nil`).
- Run again, from this folder: `clojure -M:fail`, `-M:order`,
  `-M:order timeout`, `-M:seen`, `-M:cost`, `-M:read`, `-M:dead`. Each writes
  its own lines to `logs/<name>.log`, every line with the milliseconds since
  the JVM loaded the probe, so a line that an event said and a line that a
  watcher said are in the order of one clock. Run one at a time: two
  in-process clusters at once collide on a port.
- `logs/<name>-rama-lines.log` holds Rama's own ERROR lines of the four runs
  that had any, each distinct line once with its count, cut to 300 characters.
  Rama's other output was not kept.
- Times are of one in-process cluster on one machine and give an order of
  size, no more. Most scenarios were run once; where a thing was run twice and
  came out two ways, both are given.
- Every line below says whether it was **run** (with the log) or **read**
  (with file and line). Docs: `reference/rama/docs/` is an extract of March
  2026 whose pages name Rama 1.5.0; a line from it is read, not run. The
  skill's pages are `.claude/skills/rama/references/`.
- Who ran it: one fresh session for this probe alone (Opus 5.5), in the night
  of 12 October 2026. It read the names of Rama's options out of the loaded
  1.6.0 jar. It kept the JVMs' full output and its helper scripts in its
  scratch directory under `/tmp`, outside the repository. Nothing else was
  written outside this folder. `.cpcache/` in this folder is what `clojure`
  made when it ran here.
- Three things in this paper were first read wrongly by the session and then
  corrected against the logs. They are marked "corrected" where they stand, so
  that a reader can check the correction.

## The short of it

1. **A throw in an event of a stream topology makes Rama start the worker
   again**, all four tasks of it, not only the task that threw. 36 forced
   throws, 36 restarts; from the throw to the next begin 202 to 311 ms.
2. **After that restart Rama runs again every source record since the
   topology last wrote its progress**, on every task, also the records that
   had succeeded and whose clients had been answered. Each leaves one more
   record in the second depot. With throws 7 to 10 s apart the progress was
   never written: the first thing of the run ran 37 times and has 37 records.
   Between two restarts 49 s apart it had been written.
3. **A record of a failed attempt stays.** With `:append-ack` or `:ack`, an
   event that appended and then threw left its record, and the retry left a
   second: two records for one source record, in 8 of 8 ways. The rows of the
   failed attempt were gone unless a commit boundary stood between the write
   and the throw.
4. **With no ack level the failed attempt left no record** when the throw came
   in the same event (4 of 4), and it did leave one when a commit boundary
   stood between the append and the throw (4 of 4 here; 3 of 4 in an earlier
   run whose log was not kept). It is not to be relied on either way.
5. **Row and record are not seen together.** With no boundary a reader was
   given the record before the row for 165 to 185 of 200 things, never the row
   first. With a boundary before the append it was the row first, 197 to 200
   of 200. When the client's `:ack` append returned, it found the row every
   time and the record every time but for one way (boundary, no ack: 156 of
   200).
6. **An event that waits holds its task's stream topology.** While an event
   waited, no later source record of that task began. When the wait passed
   `topology.stream.timeout.seconds` (5 s as Rama's line gives it), Rama
   failed the batch with no restart, and `:all-after` ran the failed record
   and the ones behind it again; one that had not yet begun ran twice.
7. **Append, then write is not the same computation as write, then append.**
   The times of the two are equal. But a step that lets go of the thread
   between an event's read and its write (an append that waits, or the commit
   boundary) lets other events of the batch read the row first: of 2000 events
   sent at once that each add one to a row, 3 to 21 distinct numbers came out.
   Write, then append lost none in any run.
8. **The append costs about 1 ms inside the event** with `:append-ack` or
   `:ack`, nothing measurable with no ack, and about 1 ms more for the
   boundary behind a write. The client's `:ack` call went from 2.8 to 3.3–3.6
   ms at 100 a second, and from 2.0 to 3.0 ms for one client flat out.
9. **A client reads a partition by two calls**: its start and end offsets, and
   a range. One read gives the appended values and nothing else: no offset, no
   time. 10,000 small records of the second depot took 19 to 38 ms, in one
   read or in ten. No bound was met at 50,000 in one read, but that read held
   the task's thread for its 84 to 100 ms.
10. **`:ack` to a depot that the appending topology itself consumes** is the
    deadlock the skill warns of: the event never came back, and after 5.9 s
    Rama treated it as fatal and started the worker again. To a depot that
    another stream topology consumes, `:ack` worked and waited for it.

## Found

| # | Question | Finding | How |
|---|---|---|---|
| 1.1 | does Rama start the worker again after a throw in an event | yes, each time: "Unexpected throwable! Will be treated as a fatal!" then "Triggering system shutdown". All four task globals were made ready again once. 36 of 36 | run, `fail.log`, `fail-rama-lines.log` |
| 1.2 | F1, throw at the event's end, `:append-ack` or `:ack` | the failed attempt's record stays and the retry adds one: 2 records, in the order attempt 1, attempt 2. 8 of 8 ways | run, `fail.log` TABLE |
| 1.3 | F1, no ack | the failed attempt left no record; 1 record, the retry's. 4 of 4 ways | run, `fail.log` TABLE |
| 1.4 | F1, what became of the failed attempt's rows | gone, except with write, then a boundary, then append: there the retry found the row of attempt 1 (3 of 3) | run, `fail.log` TABLE, column "the row that attempt 2 found" |
| 1.5 | F2, a later source record of the same batch throws | the earlier event had ended its code. Its rows were gone (12 of 12). Its record stayed when it had no boundary (6 of 6, with no ack too); with a boundary its append had not been made yet, and nothing stayed (6 of 6) | run, `fail.log` TABLE |
| 1.6 | F3, a throw after the event's writes were committed | the row of attempt 1 was there for the retry (12 of 12); 2 records for each thing (12 of 12 here) | run, `fail.log` TABLE |
| 1.7 | a failure that is no throw | a wait past the stream timeout: "Detected stalled streaming batch. Forcing failure and moving on to next batch." No restart. The record of the failed attempt stays | run, `order.log` O4, `order-timeout.log` |
| 1.8 | what a restart runs again | every source record of every task since the progress was last written, the answered ones too | run, `order.log` O1, O2 and its last lines; `fail.log` last TABLE lines; `dead.log` |
| 2.1 | row before record, or record before row | no boundary, `:append-ack`/`:ack`: record first for 165–185 of 200, row first for 0. Write, boundary, append: row first for 197–200, record first for 0. No ack and no boundary: neither seen in 400 | run, `seen.log` COMES |
| 2.2 | what the `:ack` client finds at its return | the row 2400 of 2400. The record 2356 of 2400: all but write, boundary, no ack, where 44 of 200 came 0.03 to 0.62 ms later | run, `seen.log` COMES |
| 2.3 | an event held open | no boundary, or append first: the record is readable and no row is given, for as long as the event has not ended. Write, boundary, append: both are given | run, `seen.log` HELD |
| 2.4 | a record whose row no reader can see, in a failed attempt | yes. Seen from outside for 6.5 s and 11 s (the stalled batch). After a throw: from the append to the retry's write, 0.2 to 0.3 s, a time in which no reader was answered | run, `order-timeout.log`, `order.log` O4; `fail.log` |
| 3.1 | N throws, N+1 and N+2 in its batch | all three fail for their clients; all three run again after the restart, with everything else since the last write of progress | run, `order.log` O3 |
| 3.2 | their appends of the first attempt | no boundary: all three records stayed, then three more. With a boundary: only N's stayed in the kept run; all three stayed in an earlier run | run, `order.log` O3; the earlier outcome is from a log not kept |
| 3.3 | N+1 processed and answered while N is on its way | not reached on one task with no hop: no later record begins while a batch is open | run, `order.log` O4, `order-timeout.log` |
| 3.4 | `:all-after` with no restart | E before N: once. N: twice. N+1, appended while N waited: twice, both to the end, two records. N+2: once. With a boundary: N 3, N+1 3, N+2 2 | run, `order-timeout.log` |
| 4.1 | the append inside the event | `:append-ack`, `:ack`: about 1.0 ms at 100 a second and for one client; 3 to 4 ms with 2000 at once. No ack: 0.00 ms | run, `cost.log` |
| 4.2 | the event, and the client's call | see the tables under question 4 | run, `cost.log` |
| 4.3 | does the place of the append change what the event computes | yes, under load: see the short of it, 7 | run, `cost.log` LOST, `read.log` ORDER task 0 |
| 5.1 | the calls, and what one read gives | `foreign-object-info`, `foreign-depot-partition-info`, `foreign-depot-read`; a vector of the values | run, `read.log` CALLS, READ |
| 5.2 | a bound on one read | none met at 50,000. A range past the end throws for the whole read | run, `read.log` EDGE, R4 |
| 5.3 | 10,000 small records | 31–38 ms in one read, 19–26 ms in ten, 65–99 ms in a hundred | run, `read.log` READ |
| 5.4 | what orders a record against the source | its own offset in the partition of the same index; and whatever the event puts in it | run, `read.log` ORDER; read, the jar's `com.rpl.rama.ops` |
| D | `:ack` to a depot with a consumer | see the short of it, 10 | run, `dead.log`, `dead-rama-lines.log` |

## Question 1: a failed event after its append (`logs/fail.log`)

The three ways of forcing the failure, each for the twelve ways of the append,
each thing failing on its first attempt only, all on task 1:

- **F1**: the event throws at its end, after the write and the append.
- **F2**: a later source record throws. H holds the thread 250 ms; A (the way
  under test, no failure asked) and B (write, append with `:append-ack`, throw
  at its end) wait behind it and so run in one batch.
- **F3**: behind the write and the append stands a commit boundary, and the
  next event of the tree throws.

Nothing is read from outside while a scenario runs, since a read that is on
its way when the worker goes down is answered by nothing (see "Seen on the
way"). What a scenario left is told from inside, and from the records
themselves: each carries the attempt, and `:row-before`, the attempt of the row
that the event found under its id. The partition is read once, at the end.

One F1 scenario whole:

```
    3024.5  RUN     ---- F1 · write, then append · no boundary · :append-ack ----
    3030.0  INSIDE  F1-wa-p attempt 1 begins on task 1 (worker bb1ad573)
    3030.6  INSIDE  F1-wa-p attempt 1 is after the write, before the append
    3035.0  INSIDE  F1-wa-p attempt 1 is after the write and the append
    3035.1  INSIDE  F1-wa-p attempt 1 THROWS on purpose after the write and the append
    3120.9  CLIENT  the :ack append of F1-wa-p gave CallbackException: Callback failure after 96.1 ms · from the call to the event's first begin: 5.4 ms
    3294.0  INSIDE  the task global of task 0 was made ready again, time 2 : Rama started the worker again
    3346.4  INSIDE  F1-wa-p attempt 2 begins on task 1 (worker bb1ad573)
    3356.9  INSIDE  F1-wa-p attempt 2 : the event's code is done
    3758.0  RESULT  from the throw to the next begin of F1-wa-p : 311.3 ms · task globals made ready again: 4 · things of earlier scenarios whose event began again: 1 (1 begins)
```

The table at the end of the log. Columns: records of attempt 1 · records of
attempt 2 · the row that attempt 2 found (1: the failed attempt's row had been
committed; nil: it was gone).

| the append | F1: throw at the end | F2: a later record throws | F3: throw behind a commit |
|---|---|---|---|
| write, append · no boundary · `:append-ack` | 1 · 1 · nil | 1 · 1 · nil | 1 · 1 · 1 |
| write, append · no boundary · `:ack` | 1 · 1 · nil | 1 · 1 · nil | 1 · 1 · 1 |
| write, append · no boundary · no ack | **0** · 1 · nil | 1 · 1 · nil | 1 · 1 · 1 |
| write, boundary, append · `:append-ack` | 1 · 1 · **1** | **0** · 1 · nil | 1 · 1 · 1 |
| write, boundary, append · `:ack` | 1 · 1 · **1** | **0** · 1 · nil | 1 · 1 · 1 |
| write, boundary, append · no ack | **0** · 1 · **1** | **0** · 1 · nil | 1 · 1 · 1 |
| append, write · no boundary · `:append-ack` | 1 · 1 · nil | 1 · 1 · nil | 1 · 1 · 1 |
| append, write · no boundary · `:ack` | 1 · 1 · nil | 1 · 1 · nil | 1 · 1 · 1 |
| append, write · no boundary · no ack | **0** · 1 · nil | 1 · 1 · nil | 1 · 1 · 1 |
| boundary, append, write · `:append-ack` | 1 · 1 · nil | **0** · 1 · nil | 1 · 1 · 1 |
| boundary, append, write · `:ack` | 1 · 1 · nil | **0** · 1 · nil | 1 · 1 · 1 |
| boundary, append, write · no ack | **0** · 1 · nil | **0** · 1 · nil | 1 · 1 · 1 |

What the table shows (run, `fail.log`, the lines that begin `TABLE`):

- **The record of a failed attempt stays** wherever the append had been made
  before the throw. The second depot then holds two records for one source
  record, the failed attempt's first. Rama sets nothing back in a depot.
- **The rows do not stay** unless they were committed: F1 with write, boundary,
  append (the boundary commits the write before the append), and every F3. So
  there are both mismatches: a record with no row behind it (most of F1 and
  F2), and, with no ack, a row with no record (F1, write, boundary, no ack:
  `0 · 1 · 1`).
- **No ack**: the append of an attempt that then threw in the same event never
  reached the depot (F1, 4 of 4). When something that frees the thread came
  between the append and the throw, it did (F2 no boundary, F3). In an earlier
  run of the same code F3 · write, boundary, no ack left **0** records of
  attempt 1; here it left 1. That earlier log was 397 KB and was not kept, so
  this one disagreeing case rests on the session's word.
- **F2 with a boundary leaves nothing of A's first attempt**, row or record:
  A wrote, went through the boundary, and B threw before A's second half ran,
  so A's append was never made and its write never committed:

```
  132480.1  INSIDE  F2a-waBp attempt 1 begins on task 1 (worker bb1ad573)
  132480.6  INSIDE  F2b-waBp attempt 1 begins on task 1 (worker bb1ad573)
  132494.8  INSIDE  F2b-waBp attempt 1 is after the write, before the append
  132496.7  INSIDE  F2b-waBp attempt 1 THROWS on purpose after the write and the append
```

- **F2 with no boundary**: A's event had run to the end of its code, its
  record is in the depot, and its rows were gone, because B threw in the same
  batch. This is the case the question names: the earlier event's append stays
  while its PState write is dropped.

```
  101353.3  INSIDE  F2a-wa-p attempt 1 begins on task 1 (worker bb1ad573)
  101353.6  INSIDE  F2a-wa-p attempt 1 is after the write, before the append
  101353.7  INSIDE  F2b-wa-p attempt 1 begins on task 1 (worker bb1ad573)
  101355.5  INSIDE  F2a-wa-p attempt 1 : the event's code is done
  101355.6  INSIDE  F2b-wa-p attempt 1 THROWS on purpose after the write and the append
```

**Did Rama restart the worker?** For each of the three ways, yes, every time
(run, `fail-rama-lines.log`: 36 × "One or more monitored components have
become unhealthy! Triggering system shutdown."; 36 × the task global of task 0
made ready again; the count of task globals made ready again is 4 in each
scenario, four tasks once each). From the throw to the next begin, 36 throws:
least 201.7 · middle 239.4 · 9 of 10 under 269.1 · most 311.3 ms.
*Corrected*: the session first computed "least 4 ms" by splitting the table's
lines on a mark that one column contains; the numbers here are from the
`RESULT` lines.

**The client.** Its `:ack` append threw `CallbackException: Callback failure`
in all 36 scenarios, for A in F2 as well, though A had not been asked to fail.
The retry then succeeded, and the client is not told. Read:
`14-depots.md:214`, "by default success of colocated streaming topologies is
determined solely on the first attempt".

**What the replays added** (run, `fail.log`, the last two `TABLE` lines):

```
  343105.3  TABLE   what the replays added, for four things: [id · times its event began · its records in the second depot] (["warm" 37 37] ["F1-wa-p" 37 37] ["F2a-wa-p" 25 25] ["F3-wa-p" 13 13])
  343132.9  TABLE   events begun in all: 1207 for 61 source records · records in the second depot: 963 · the row "count", which each event that commits raises by one: 770
```

"warm" was appended once, succeeded, and was answered. It ran again at each of
the 36 restarts. The counts close: scenario 13's thing ran 1 + 1 + 23 = 25
times, scenario 25's 1 + 1 + 11 = 13. The 963 was not reconciled to the unit
against the 1207.

**A failure after the writes have committed, with no failure of the event at
all** — the skill's line (`stream.md:13`, "A stream topology can retry a record
even after all PState writes have completed and committed") — was produced in
two forms and not in a third. Produced: F3; and the replay after a restart of
records that had committed and been answered (question 3, and `dead.log`).
Not produced: a clean restart or a killed process; see "Not settled".

## Question 2: seen together or not (`logs/seen.log`)

No thing fails in this run. All things on task 1.

**A, the event held open.** The event writes and appends, then waits for a
future with the task's thread free, so that a reader is answered while the
event has not ended. Four of the twelve lines; the other eight repeat them:

```
    3335.6  HELD    write, then append · no boundary · :append-ack · while the event waited, a reader was given: row nil · records [["A-wa-p" 1]] · the client's append had returned: false ‖ after the event ended: row 1 · records [["A-wa-p" 1]] · the client's append gave {}
    3464.5  HELD    write, then append · no boundary · no ack · while the event waited, a reader was given: row nil · records [["A-wa-0" 1]] · the client's append had returned: false ‖ after the event ended: row 1 · ...
    3532.7  HELD    write, then append · boundary before the append · :append-ack · while the event waited, a reader was given: row 1 · records [["A-waBp" 1]] · the client's append had returned: false ‖ ...
    3716.8  HELD    append, then write · no boundary · :append-ack · while the event waited, a reader was given: row nil · records [["A-aw-p" 1]] · the client's append had returned: false ‖ ...
```

In 9 of 12 ways the record was readable and no row was given, for as long as
the event had not ended. In the 3 ways write, boundary, append, both were
given. The append itself does not commit the rows before it: only the boundary
does. That is the skill's rule run (`stream.md:49`, `stream.md:60`).

**B, as it comes.** 200 things for each way, one after the other, `:ack`. One
thread reads without a pause: the second depot's end offset, the thing's row,
the end offset again. A record in the first read with no row in the second was
readable before the row was given; a row in the second with no record in the
third was given before the record was readable.

| the append | record before row | row before record | at the client's return: row · record |
|---|---|---|---|
| write, append · no boundary · `:append-ack` | 165 | 0 | 200 · 200 |
| write, append · no boundary · `:ack` | 169 | 0 | 200 · 200 |
| write, append · no boundary · no ack | 0 | 0 | 200 · 200 |
| write, boundary, append · `:append-ack` | 0 | 198 | 200 · 200 |
| write, boundary, append · `:ack` | 0 | 197 | 200 · 200 |
| write, boundary, append · no ack | 0 | 200 | 200 · **156** |
| append, write · no boundary · `:append-ack` | 184 | 0 | 200 · 200 |
| append, write · no boundary · `:ack` | 185 | 0 | 200 · 200 |
| append, write · no boundary · no ack | 0 | 0 | 200 · 200 |
| boundary, append, write · `:append-ack` | 181 | 0 | 200 · 200 |
| boundary, append, write · `:ack` | 177 | 0 | 200 · 200 |
| boundary, append, write · no ack | 0 | 0 | 200 · 200 |

- Never both orders for one way. Which comes first is fixed by the way.
- "0 · 0" for no ack with no boundary does not show that the two appear at one
  instant. It shows that 200 things each went from neither to both between two
  rounds of reads. The held-open line for the same way does show a record with
  no row.
- The client that appended with `:ack` found the row every time: 2400 of 2400.
  It found the record every time but for write, boundary, no ack, where 44 of
  200 came 0.03 to 0.62 ms later.

**For a failed attempt** (question 2's last part): yes, there is a moment when
the second depot holds a record whose row no reader can see. Seen from outside
where the failure starts no worker again (run, `order-timeout.log`):

```
    2913.9  INSIDE  T1-wa-p-n0 attempt 1 is after the write and the append
    2939.8  OUTSIDE while N waits: row of T1-wa-p-n0: nil · records: [["T1-wa-p-e" 1] ["T1-wa-p-n0" 1]]
    6416.3  OUTSIDE after that append: row of T1-wa-p-n1: nil · records: [["T1-wa-p-e" 1] ["T1-wa-p-n0" 1]]
    9431.3  SELECT  T1-wa-p-n0 is now given as {:id "T1-wa-p-n0", :attempt 2, :count 3}
```

6.5 s there, with the timeout set to 3; 11 s in `order.log` O4 with Rama's own
5. After a throw the same state lasted from the append to the retry's write,
0.2 to 0.3 s by the events' own marks; no reader was answered in that time, so
it was not seen from outside. And it does not end with the retry: the failed
attempt's record stays in the depot for good, beside the retry's.

## Question 3: what `:all-after` does to the records behind a failed one (`logs/order.log`, `logs/order-timeout.log`)

Two different things run records again, and the runs had to tell them apart.

**(a) The restart, which is not the retry mode.** A throw starts the worker
again, and the topology then begins at the place where it last wrote its
progress, on every task (run, `order.log` O1). Six things on task 1 and three
on task 2 had succeeded and been answered; then one thing on task 1 threw:

```
    3653.4  INSIDE  events begun so far: [["O1-p1" 2] ["O1-p2" 2] ["O1-p3" 2] ["O1-p4" 2] ["O1-p5" 2] ["O1-p6" 2] ["O1-q1" 2] ["O1-q2" 2] ["O1-q3" 2] ["O1-x" 2]] · task globals made ready again: 4 · from the throw to the next begin of O1-x : 340.0 ms
   16370.1  RESULT  the second depot, task 1, since the scenario began: [["O1-p1" 1] ["O1-p2" 1] ["O1-p3" 1] ["O1-p4" 1] ["O1-p5" 1] ["O1-p6" 1] ["O1-x" 1] ["O1-p1" 2] ["O1-p2" 2] ["O1-p3" 2] ["O1-p4" 2] ["O1-p5" 2] ["O1-p6" 2] ["O1-x" 2] ["O1-after" 1]]
   16473.2  RESULT  the second depot, task 2, since the scenario began: [["O1-q1" 1] ["O1-q2" 1] ["O1-q3" 1] ["O1-q1" 2] ["O1-q2" 2] ["O1-q3" 2]]
```

The records *before* the failed one ran again, and those of another task.
`:all-after` names neither. O2 is the same after 35 seconds with no failure,
and the run's last lines give how far back a restart reaches:

```
  159588.7  RUN     at the end, events begun of O1's things: [["O1-p1" 2] ["O1-p6" 2] ["O1-q1" 2] ["O1-x" 2] ["O1-after" 1]] · of O2's things: [["O2-p1" 4] ["O2-p6" 4] ["O2-q1" 4] ["O2-x" 4] ["O2-after" 3]]
```

This run had four restarts, by the times of the four throws in the log: O1 at
3.3 s, O2 at 52.2 s, and the two of O3 at 66.4 s and 79.3 s. O1's things ran
again at O1's restart and at none of the three after it. O2's things ran again
at all three of theirs. So the progress was written at some point in the
48.9 s between the first two restarts (35 of them the run's quiet wait), and
was not written in the 14.2 s and the 12.9 s between the later ones. Read, from the loaded
jar's defaults: `topology.stream.periodic.checkpoint.seconds` is 30; read,
`11-stream-topologies.md:142` and `:166`, "those records will get retried".
Not run: any gap between 14 and 49 s, or the other trigger,
`topology.stream.checkpoint.progress.threshold`.

**(b) N throws while N+1 and N+2 wait behind it** (O3). The three began in one
batch; their clients all got `Callback failure`; all three ran again:

```
   66391.9  INSIDE  O3-wa-p-n0 attempt 1 begins on task 1 (worker bb1ad573)
   66392.3  INSIDE  O3-wa-p-n1 attempt 1 begins on task 1 (worker bb1ad573)
   66392.5  INSIDE  O3-wa-p-n2 attempt 1 begins on task 1 (worker bb1ad573)
   66394.5  INSIDE  O3-wa-p-n0 attempt 1 THROWS on purpose after the write and the append
   75869.3  RESULT  the second depot, the three things only, in order: [["O3-wa-p-n0" 1] ["O3-wa-p-n1" 1] ["O3-wa-p-n2" 1] ["O3-wa-p-n0" 2] ["O3-wa-p-n1" 2] ["O3-wa-p-n2" 2]]
   90560.9  RESULT  the second depot, the three things only, in order: [["O3-waBp-n0" 1] ["O3-waBp-n0" 2] ["O3-waBp-n1" 2] ["O3-waBp-n2" 2]]
```

With no boundary N+1 and N+2 had made their appends before N threw, and the
depot holds six records for three source records. With a boundary, in this
run, only N's first record is there. *Corrected*: the first time this scenario
ran, all three first records were there, and the session had taken that as the
rule. The same code gave both, so whether the append of a record behind a
failed one is in the depot depends on whether it was made before the throw.
The earlier log was written over by this one.

**(c) N+1 and N+2 answered while N is still on its way, then N fails** (O4,
T1) could not be reached on one task with no hop. N wrote, appended and
waited, the thread free. N+1 was appended with `:ack`. Its event did not begin:

```
   95593.8  OUTSIDE while N waits: row of O4-wa-p-n0: nil · records: [["O4-wa-p-n0" 1]]
  101527.4  CLIENT  the :ack append of O4-wa-p-n1 gave CallbackException: Callback failure after 5933.4 ms
```

Rama's line at that moment (run, `order-rama-lines.log`): "Detected stalled
streaming batch. Forcing failure and moving on to next batch.
{:time-limit-nanos 5000000000}". With `topology.stream.timeout.seconds` set to
3 the line gives 3000000000 (`order-timeout-rama-lines.log`), so the limit is
that option, and Rama's own value of it is 5 s. Read: `23-acid-semantics.md:29`,
"The topology runner on that task does not move on to the next batch until the
previous batch has finished".

**(d) `:all-after` with no restart** (T1, timeout 3 s, an earlier thing E
before N):

```
    9410.3  INSIDE  T1-wa-p-n1 attempt 1 begins on task 1 (worker bb1ad573)
    9410.9  INSIDE  T1-wa-p-n0 attempt 2 begins on task 1 (worker bb1ad573)
    9411.2  INSIDE  T1-wa-p-n1 attempt 2 begins on task 1 (worker bb1ad573)
    9411.4  INSIDE  T1-wa-p-n2 attempt 1 begins on task 1 (worker bb1ad573)
   10930.8  RESULT  from N's first begin to its second: 6501.9 ms · events begun: [["T1-wa-p-e" 1] ["T1-wa-p-n0" 2] ["T1-wa-p-n1" 2] ["T1-wa-p-n2" 1]] · task globals made ready again: 0
   10932.0  RESULT  the second depot since the scenario began: [["T1-wa-p-e" 1] ["T1-wa-p-n0" 1] ["T1-wa-p-n1" 1] ["T1-wa-p-n0" 2] ["T1-wa-p-n1" 2] ["T1-wa-p-n2" 1]]
```

- E, before N, did not run again. Here `:all-after` is what the docs say.
- N ran twice. Its first record stays.
- **N+1 ran twice**, 0.9 ms apart, both to the end, two records. Its first
  attempt had never begun before; nothing of its own failed. Its client had
  been told `Callback failure`.
- N+2 ran once and its client was answered `{}`, 3 s after its call.
- With a boundary before the append: N three times, N+1 three times, N+2
  twice; nine records for four source records (`order-timeout.log`, the second
  T1). Why a third round came was not found.
- The first attempt of N did not go on when its future was completed later
  (`RESULT  N's first attempt went on behind its wait: false`).

## Question 4: what the append costs an event (`logs/cost.log`)

Thirteen ways (no append, and the twelve). Three loads, all on task 1. Three
times: **event**, inside, from the event's first step to its last, the commit
of the rows left out; **append**, inside, around the append step alone;
**client**, from the `:ack` call to its return. Each cell: least · middle ·
9 of 10 under · most, in ms. Times of one in-process cluster on one machine:
an order of size, no more.

**Steady, 300 things at 100 a second** (count 300 in each cell):

| the append | event | append | client |
|---|---|---|---|
| no append | 0.03 · 0.05 · 0.09 · 0.21 | — | 2.29 · 2.76 · 3.75 · 8.02 |
| write, append · no boundary · `:append-ack` | 1.00 · 1.10 · 1.32 · 4.51 | 0.96 · 1.05 · 1.25 · 4.45 | 3.14 · 3.58 · 4.46 · 8.11 |
| write, append · no boundary · `:ack` | 1.00 · 1.10 · 1.29 · 4.72 | 0.96 · 1.05 · 1.23 · 4.67 | 3.13 · 3.61 · 4.13 · 7.34 |
| write, append · no boundary · no ack | 0.04 · 0.05 · 0.08 · 0.14 | 0.00 · 0.00 · 0.01 · 0.01 | 2.59 · 2.99 · 3.60 · 8.76 |
| write, boundary, append · `:append-ack` | 1.98 · 2.18 · 2.45 · 5.69 | 0.96 · 1.04 · 1.22 · 4.45 | 3.25 · 3.59 · 4.05 · 7.23 |
| write, boundary, append · `:ack` | 1.91 · 2.12 · 2.54 · 17.95 | 0.93 · 1.01 · 1.28 · 16.68 | 3.12 · 3.53 · 4.37 · 20.30 |
| write, boundary, append · no ack | 0.97 · 1.11 · 1.35 · 4.25 | 0.00 · 0.00 · 0.01 · 0.01 | 2.14 · 2.50 · 3.04 · 9.44 |
| append, write · no boundary · `:append-ack` | 0.94 · 1.03 · 1.22 · 4.46 | 0.91 · 0.99 · 1.17 · 4.42 | 2.92 · 3.27 · 3.63 · 7.57 |
| append, write · no boundary · `:ack` | 0.97 · 1.03 · 1.24 · 4.61 | 0.94 · 1.00 · 1.20 · 4.57 | 3.00 · 3.31 · 3.67 · 7.11 |
| append, write · no boundary · no ack | 0.03 · 0.04 · 0.06 · 0.19 | 0.00 · 0.00 · 0.00 · 0.02 | 2.49 · 2.72 · 3.09 · 7.09 |
| boundary, append, write · `:append-ack` | 1.04 · 1.13 · 1.31 · 11.00 | 0.94 · 1.01 · 1.18 · 4.18 | 3.03 · 3.36 · 3.71 · 13.24 |
| boundary, append, write · `:ack` | 1.00 · 1.10 · 1.34 · 10.91 | 0.92 · 0.98 · 1.20 · 1.92 | 2.97 · 3.24 · 3.59 · 12.98 |
| boundary, append, write · no ack | 0.09 · 0.12 · 0.18 · 9.04 | 0.00 · 0.00 · 0.01 · 0.02 | 2.53 · 2.85 · 3.20 · 12.23 |

**Flat out.** "One": 500 things from one client, each sent when the one before
was answered. "At once": 2000 things, none waiting for another; things a
second over the whole load in the last column.

| the append | one: event, middle · most | one: client, middle · 9 of 10 | one: a second | at once: append, middle · most | at once: a second |
|---|---|---|---|---|---|
| no append | 0.02 · 0.17 | 2.01 · 2.29 | 479 | — | 10197 |
| write, append · no boundary · `:append-ack` | 1.00 · 2.40 | 3.05 · 3.39 | 318 | 4.08 · 35.42 | 9892 |
| write, append · no boundary · `:ack` | 0.99 · 4.54 | 2.98 · 3.29 | 326 | 4.13 · 21.62 | 9914 |
| write, append · no boundary · no ack | 0.02 · 0.11 | 2.48 · 2.75 | 380 | 0.00 · 0.01 | 9958 |
| write, boundary, append · `:append-ack` | 1.94 · 5.34 | 3.07 · 3.37 | 314 | 2.92 · 22.53 | 9212 |
| write, boundary, append · `:ack` | 1.95 · 7.55 | 3.08 · 3.50 | 312 | 2.87 · 20.96 | 9736 |
| write, boundary, append · no ack | 0.96 · 101.19 | 2.98 · 3.32 | 284 | 0.00 · 0.01 | 10194 |
| append, write · no boundary · `:append-ack` | 0.99 · 3.34 | 2.97 · 3.31 | 325 | 3.93 · 33.69 | 10553 |
| append, write · no boundary · `:ack` | 1.00 · 4.58 | 2.99 · 3.30 | 325 | 3.93 · 34.27 | 10323 |
| append, write · no boundary · no ack | 0.02 · 0.09 | 2.47 · 2.74 | 392 | 0.00 · 0.02 | 10271 |
| boundary, append, write · `:append-ack` | 1.06 · 101.96 | 3.06 · 3.43 | 266 | 3.32 · 20.97 | 9848 |
| boundary, append, write · `:ack` | 1.05 · 101.51 | 3.03 · 3.35 | 302 | 3.31 · 33.94 | 9835 |
| boundary, append, write · no ack | 0.08 · 100.27 | 2.58 · 2.88 | 349 | 0.00 · 0.02 | 9262 |

What the lines show:

- With no append the event's code took 0.02 to 0.05 ms (middle). The client's
  2.0 to 2.8 ms is the append to the source depot and the commit of the rows.
- An append that waits (`:append-ack` or `:ack`) added about 1.0 ms to the
  event and 0.5 to 1.0 ms to the client's call. `:ack` and `:append-ack` are
  not told apart here, as nothing consumes the second depot; read,
  `14-depots.md:206`, "If there are no colocated stream topologies,
  AckLevel.ACK is equivalent to AckLevel.APPEND_ACK".
- An append with no ack cost the event nothing that could be measured, and the
  client between nothing and 0.5 ms (steady: 2.72 to 2.99 against 2.76; one
  client: 2.47 and 2.48 against 2.01).
- The boundary added about 1 ms to the event when it stands behind a write,
  and almost nothing to the client's call.
- For one client an append that waits took the rate from 479 to 312–326 a
  second. With 2000 at once the task did 9212 to 10553 a second with an
  append and 10197 without: at that load the append cannot be told from none.
- Under "at once" the time of an event with a boundary was 35 to 45 ms
  (middle). That is the time the event was on its way behind 2000 first
  halves, not work it cost; the log has it and the table above leaves it out
  for that reason.
- Not explained: under "one", four ways each have one event of 100 to 102 ms
  among their 500 (most 101.19, 101.96, 101.51, 100.27), and all four are ways
  with a boundary. It is why their rate is lower (284, 266, 302, 349). In the
  run before this one, whose log was written over, three ways had such an
  event, again only ways with a boundary. Twice in the same family is more
  than a pause that fell there by chance; the cause was not looked for.

**The times do not show the difference that matters between the places.** Each
event reads the row "count", adds one, writes it, and puts the number in its
record. The run's last lines count the numbers that came out (run, `cost.log`,
lines `LOST`):

```
   81496.8  RUN     the row "count": each event reads it, adds one and writes it. Events begun: 40350 · the row holds: 30402 · raises that were lost: 9948
   81517.4  LOST    atonce · append, then write · no boundary · :append-ack · 2000 · 21 · 1979
   81518.1  LOST    atonce · append, then write · no boundary · :ack · 2000 · 21 · 1979
   81519.1  LOST    atonce · append, then write · no boundary · no ack · 2000 · 2000 · 0
   81519.7  LOST    atonce · append, then write · boundary before the append · :append-ack · 2000 · 3 · 1997
   81520.3  LOST    atonce · append, then write · boundary before the append · :ack · 2000 · 3 · 1997
   81520.9  LOST    atonce · append, then write · boundary before the append · no ack · 2000 · 4 · 1996
```

The other 31 lines all end in 0. The five losses sum to 9948, the whole of
what the row lacks. So:

- **Write, then append lost nothing**, in every load, with or without a
  boundary, at every ack level: 16,800 events.
- **Append, then write lost nearly all under "at once"**, wherever a step
  that lets go of the thread stands between the read and the write: an append
  that waits, or the boundary. The boundary alone is enough, with an append
  that takes 0.00 ms. An append with no ack and no boundary lost none.
  *Corrected*: the session had first put this down to the ack level.
- **The two quiet loads lost nothing in any way, and that does not clear
  them.** At 100 a second two events of one task were seldom on their way
  together. 0 of 300 says it did not happen at that spacing.
- A second run of the same thing, on another task (run, `read.log`): 3000 at
  once, append, then write, `:append-ack`: "distinct 31 of 3000 · the row
  "count" now: 31".
- The cost run was made twice, since its last lines were added after the
  first; the log kept is the second, and every number of this section is its.
  The first gave 30403 where this one gives 30402. The numbers 21, 3 and 4 are
  this run's.

For a gate whose event reads rows, decides, and writes rows, this is the fact
of question 4: with the append between the read and the write, two acts on one
key that arrive together can both read the row as it was.

## Question 5: reading a depot back from a client (`logs/read.log`)

**The calls** (run, the lines `CALLS` and `READ`):

```
    2823.3  CALLS   (foreign-object-info log) => {:name "*log", :module-name "probe.gate/GateProbe", :num-partitions 4}
    2835.1  CALLS   (foreign-depot-partition-info log 1), nothing appended yet => {:start-offset 0, :end-offset 0}
    2841.1  CALLS   (foreign-depot-read log 1 0 0), nothing appended yet => []
    4488.5  READ    (foreign-depot-read log 1 0 3) => [{:id "R2-0", :attempt 1, :count 1, :row-before nil} {:id "R2-1", ...} {:id "R2-2", ...}] · the class of the result: clojure.lang.PersistentVector · of one record: clojure.lang.PersistentArrayMap
```

- A partition is read from its start by `foreign-depot-partition-info depot p`
  for `:start-offset` (first there) and `:end-offset` (one past the last), then
  `foreign-depot-read depot p from to`, `from` in, `to` out.
- One read gives a vector of the values as they were appended, and nothing
  else. A record's offset is the read's start plus its place in the vector.
- A client can read a depot declared `:disallow`. It cannot append to it
  ("Appending to this depot is disallowed", seen in a first trial; not in a
  kept log).
- The interface `com.rpl.rama.Depot` declares: append, appendAsync, close,
  getObjectInfo, getPartitionInfo, getPartitionInfoAsync, read, readAsync, and
  the three makers of a partitioner. No call gives the time of a record. Read:
  `14-depots.md:196` says the depot keeps the time of each append, for
  "start from" options.

**The edges** (run, lines `EDGE`): a range that goes past the end throws "Log
end offset out of range" for the whole read, the records that are there too.
The end to the end gives 0 records. An end before the start, and a start below
zero, throw. So a reader asks for the end offset first and reads up to it.

**The time of 10,000 small records**, three tries each, ms (the partition
info call itself: middle 0.24 ms over 200):

| reads of | calls | the second depot | the source depot |
|---|---|---|---|
| 10,000 | 1 | 37.5 · 32.6 · 31.4 | 19.5 · 23.7 · 15.2 |
| 1,000 | 10 | 25.8 · 21.0 · 18.8 | 22.5 · 18.1 · 17.2 |
| 500 | 20 | 29.5 · 23.8 · 21.4 | 21.3 · 22.0 · 22.0 |
| 100 | 100 | 67.9 · 64.5 · 98.7 | 56.1 · 54.5 · 58.4 |

**A bound on one read.** None was met: 50,000 records in one read, twice. But
the read is one event on the task's thread:

```
    9384.4  READ    one read of the second depot's partition 1 from 0 to 50000 => 50000 records in 83.8 ms · a read of a row of the same task, sent 5 ms after it, was answered after 77.5 ms
    9484.6  READ    one read of the second depot's partition 1 from 0 to 50000 => 50000 records in 99.9 ms · a read of a row of the same task, sent 5 ms after it, was answered after 93.5 ms
    9492.3  READ    the same read of a row with nothing else going on, 50 times: count 50 · least 0.11 · middle 0.12 · 9 of 10 under 0.17 · most 0.72 ms
```

Every reader and every event of that task waits for the length of the read.
Read: `14-depots.md:242`, "you shouldn't fetch more than 50kb worth of data at
a time".

**What orders a record against the source depot.**

- *Its place.* `depot-partition-append!` appends to the partition of the task
  that the event is on (read, `14-depots.md:254`). The event has no hop, so
  partition p of the second depot holds only records of source partition p
  (run: `{0 {"R5-aw-p" 3000}, 1 {"R2" 10000, "R4" 40000}, 2 {"R5-waBp" 3000},
  3 {"R5-wa-0" 3000}}`).
- *Its order, with no failure.* In four partitions and 59,000 records, each
  partition holding events of one way, the ids of the second depot were in the
  order of the source: "records at the same place in both partitions: 3000 of
  3000" for the boundary, for no ack and for append, then write; "the ids of
  the two partitions are in the same order: true" for the 50,000.
  *Corrected*: in its first failure run, whose log was not kept, the session
  saw records out of source order during a replay and took it for a property
  of the boundary. That batch mixed things with and without a boundary; an
  event with a boundary goes on in a later event, behind the first halves of
  the others. With one shape of event the order held here. It was not run with
  a failure.
- *Its order, with a failure.* Lost. A second attempt's record comes behind
  records of later source records, and a restart adds a record for every
  source record since the progress was written (questions 1 and 3).
- *Nothing from Rama.* A record carries no offset and no time of its own, and
  the event has no way to learn the offset of the source record it runs for:
  `com.rpl.rama.ops` of the 1.6.0 jar holds `current-task-id`,
  `current-microbatch-id`, `current-topology-type`, `module-instance-info`
  and no op for an offset (read, from the loaded jar).
- *What the event puts in it.* Whatever it has: the source record's own id,
  the attempt, and here a count kept in a row of the task. With write, then
  append, that count was the source record's offset plus one for 3000 of 3000
  and 50,000 of 50,000. It stops being that when an event that committed runs
  again (F3: attempt 1 carried 285, attempt 2 carried 325), and with append,
  then write it is no order at all (31 distinct numbers in 3000 records).
  When the failed attempt's rows were dropped and nothing ran between, the
  retry would read the same row again; in these runs a replay always ran
  between, and the two attempts carried different counts.

## The skill's deadlock warning (`logs/dead.log`)

The warning (`depot-design.md:66`) is about `:ack` to a depot that the
appending stream topology consumes. In every run above nothing consumes the
second depot, and `:ack` there did what `:append-ack` did. This run gives the
depot a consumer, in a module of its own with the stream timeout set to 5.

| the depot is consumed by | `:append-ack` | no ack | `:ack` |
|---|---|---|---|
| another stream topology of the module | answered, 76.2 ms (the JVM's first) | answered, 10.1 ms | answered, 13.1 ms; the consumer got the record before the event went on |
| the appending topology itself | answered, 20.7 ms | answered, 10.4 ms | **never came back** |

```
   10377.7  INSIDE  own-ack attempt 1 begins; it will append to own with :ack
   16305.2  CLIENT  the :ack append of own-ack gave CallbackException: Callback failure after 5932.2 ms · events of own-ack begun: 1
   16704.5  INSIDE  side-append-ack attempt 2 begins; it will append to side with :append-ack
   23806.8  RESULT  events begun, by thing: {"own-ack" 2, "own-append-ack" 2, "own-no-ack" 2, "side-ack" 2, "side-append-ack" 2, "side-no-ack" 2}
```

- The deadlock was produced. It did not end as a stalled batch. Rama's lines
  are those of a throw (`dead-rama-lines.log`: "Unexpected throwable! Will be
  treated as a fatal!" with `CallbackException: Callback failure`, then
  "Triggering system shutdown"): the worker was started again. This module has
  no task global, so the restart is told from Rama's lines and from what
  followed.
- What followed: all six things ran a second time, the five that had been
  answered too, and both depots hold twelve records. The consumers were given
  the six first records a second time before the new ones. Nothing in this run
  threw on purpose.
- Not found: whether the 5.9 s is the stream timeout that the run set to 5, or
  another limit near it. Only that one option was set.

## Seen on the way

1. **A duplicate with no failure of its own.** After a restart, every source
   record since the last write of progress leaves one more record, on every
   task (questions 1 and 3). Under `:all-after` with no restart, a record
   behind a failed one ran twice in a row (question 3, d).
2. **A record lost.** An append with no ack, issued before a throw in the same
   event, did not reach the depot (4 of 4). An append with `:append-ack` behind
   a boundary, issued in the batch in which an earlier record threw, was there
   in one run and not in another.
3. **A lost raise.** Append, then write (question 4).
4. **A stall.** An event that waits holds its task's stream topology; nothing
   behind it begins until it ends or the stream timeout fails it (question 3,
   c). An append with `:append-ack` is such a wait, of about 1 ms here.
5. **A deadlock**, and what Rama did with it (above).
6. **After a restart a client waits.** Reads sent in the 0.6 to 2.4 s after a
   throw were answered late; four throws in two runs of `order` (run,
   `order.log`, lines `PINGS`: the first read answered within 100 ms was sent
   639.9 and 2362.9 ms after the throw; in the run before, 620 and 1567). A
   few reads that were on their way threw (5 and 2 of about 120 and 150).
   Rama's client said "Unexpected channel pong timeout; closing channel".
7. **After restarts in quick succession a new thing waits long for its
   event.** In `fail.log`, with a restart 0.4 to 0.6 s before each call: from
   the client's call to the event's first begin 5.4 ms, 212, 811, 4239 ms, and
   from the fifth on about 9.43 s (24 things: middle 9434.5, most 9452.6). In
   `order.log`, with restarts 35 s or more apart: 209 and 1034 ms. Measured;
   the cause was not found.
8. **A consumer of the log is replayed too** (`dead.log`).
9. **The task that threw is not the only one set back.** The whole worker
   restarts; things of task 2 ran again after a throw on task 1 (`order.log`
   O1). With one worker for the module, that is every task.

## Not settled by these runs

- **A real cluster.** One JVM, one worker, no replication (the in-process
  cluster has a replication factor of 1). A killed process, a change of
  leader, a second worker that stays up while one restarts: not run, and not
  possible here. Every time above is of this machine.
- **A clean restart.** `rtest/update-module!` with the same module, to see
  whether a restart that is no crash also runs records again, was planned and
  not written into any run. Not run.
- **When the progress is written.** One gap of 48.9 s between restarts
  (written) and gaps of 14.2 and 12.9 s (not written). Not run: the gaps
  between;
  `topology.stream.checkpoint.progress.threshold`; whether the 30 s begin
  again at each worker start, which the 37 replays of `fail.log` fit and do
  not prove.
- **N+1 answered while N is on its way**, on one task: tried twice, not
  reachable (question 3, c). With a hop to another task it may be; the store's
  gate has no hop, and it was not run.
- **`depot.ack.failure.on.any.streaming.failure` set to false**, which by the
  docs lets the client's `:ack` wait for a retry (`14-depots.md:214`): not run.
- **An append with no ack**: 0 of 4 and 4 of 4 here, with one case that came
  out the other way in a run not kept. How often, and under what load, was not
  run.
- **Order under failure**, and order with events of mixed shape in one batch:
  seen wrong once, not run to a number.
- **Retry modes other than `:all-after`**: not run.
- **The third round** of attempts in T1 with a boundary, the events of
  100 ms in ways with a boundary in the cost run, the 963 against the 1207 in `fail.log`, and the 5.9 s
  of the deadlock: seen, not explained.
- **Most scenarios ran once.** Where a count is "n of n" over the twelve ways
  it is twelve tries of twelve different things, not twelve of one. Where two
  runs of one thing disagreed, both are given.
