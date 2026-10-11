# What a second attempt of a microbatch does, and what survives a failed one — runs of 12 October 2026

The reads build's A7 names a repair to try first: "keep each batch's clock, so
that a second try comes out the same" (`BUILD-reads.md`). The store's page says
of it: "To make both the same would take state outside the microbatch gate",
because "Rama sets a PState back when an attempt of a batch fails". The plan
stage needs facts about a second attempt and about what survives a failed one.
These are the runs.

**Blocked:** nothing.

- Rama 1.6.0, its in-process cluster, one machine, one JVM for the module and
  for the watchers. 4 tasks. 4 threads in the runs for B2, B3, B4 and B7, so
  that each task has a thread of its own; 2 threads in the runs for B5 and B6.
- A probe, not a design. Its tables stand in for the questions. It requires
  nothing of the store's tree and nothing of the rig. No table, name or form
  here is a proposal for the store.
- One watcher of each kind per row. Small values. Times are of one in-process
  cluster on one machine and mean an order of size, no more.
- Run again, from this folder: `clojure -M:fail`, `-M:keep`, `-M:redef`,
  `-M:idle`, `-M:commit`, and `clojure -M:commit control`. Each writes its own
  lines to `logs/<name>.log`, every line with the milliseconds since the JVM
  loaded the probe, so a line that a topology said and a line that a watcher
  said are in the order of one clock. Run one at a time: two in-process
  clusters at once collided on a port here ("BindException: Address already in
  use").
- `logs/fail-rama-lines.log` and `logs/commit-rama-lines.log` hold Rama's own
  ERROR lines of those two runs, cut to 300 characters. Rama's other output was
  not kept.
- Docs: `reference/rama/docs/` is an extract of 22 March 2026 whose pages name
  Rama 1.5.0. Every line cited from it is "read, not run". The skill's pages
  are `.claude/skills/rama/references/`.
- Who ran it: one fresh session for this probe alone (Opus 5.5), in the night
  of 11 to 12 October 2026. It read the names of Rama's options out of the 1.6.0
  jar by unpacking a few class files into its scratch directory under `/tmp`,
  which is outside the repository. Nothing else was written outside this folder.

## The short of it

1. The window that the store's page names is real, and it was run. With one
   task's thread held, three tasks showed a batch and the fourth did not, for
   as long as the hold lasted. When the attempt then failed, by the phase
   timeout or by a throw, Rama ran the same batch again, and a reader that had
   been given a row of the first attempt on those three tasks was given another
   row after the second (B7, S3 and S6).
2. With the batch's clock kept outside the microbatch topology, and the rows
   otherwise the same, the row that a reader was given first is the row that
   stayed (B7, S5 and S7; B4).
3. The road that kept it: a depot append from inside the attempt, with `:ack`,
   to a depot that a stream topology of the same module consumes into its own
   PState, first write wins by batch id. It cost about 3 ms inside each attempt
   here, and every run of the topology paid it, the runs that got nothing too
   (B3).
4. Nothing that is the memory of a process survived a failure by a throw: Rama
   treats a throw on a task's thread as fatal for the worker and starts the
   worker again. A task global came back empty (B3).
5. A throw in the topology's own code can only fail an attempt while it is
   being processed. Then no row of the failed attempt was ever given to a
   reader (B2).

## Found

| # | Question | Finding | How |
|---|---|---|---|
| B1 | what the docs say | see "B1, by the lines" below | read, not run |
| B2.1 | is a row of an attempt that failed while it was processed ever visible | no: by select, by query topology and by push, on four tasks, across one failure and across two in a row | run, `fail.log` |
| B2.2 | what a push receives across that failure | nothing of the failed attempt. After the worker's restart a `ResyncDiff` with the value it already had, then the second attempt's row | run, `fail.log` |
| B2.3 | same things, same batch id on the second attempt | yes: the same things in the same order on each task, and the same `ops/current-microbatch-id`, on 2 and on 3 attempts. A thing appended while attempt 1 ran was not in attempt 2; it came in the next batch | run, `fail.log` |
| B2.4 | what a throw in the topology does | Rama logs "Unexpected throwable! Will be treated as a fatal!" and starts the worker again: 350 to 580 ms here from the throw to the next attempt, in 7 throws | run, `fail.log`, `fail-rama-lines.log` |
| B3.i | a PState of the microbatch topology itself | set back: attempts 2 and 3 read what the batch before had left, not what attempt 1 wrote. A row is set back the same way | run, `fail.log` |
| B3.ii | a depot append inside the attempt, consumed by a stream topology of the module | survives, is durable, and the second attempt reads the first attempt's value. With `:ack` the same attempt read it back 162 of 162 times; with `:append-ack` or no ack 0 of 82 | run, `keep.log` |
| B3.ii cost | what that road costs one batch | about 3 ms inside the attempt (2.8 to 4.5 ms over 80 batches); an attempt up to its last step went from 51 to 54 ms. Each run pays it, a run that got nothing too, and each run that appends leaves one depot record and one row | run, `keep.log` |
| B3.iii a | a task global | holds from one run to the next while the worker lives; came back empty after the throw, since the worker was started again. Memory of a process: does not count as kept | run, `fail.log` |
| B3.iii b | state of the JVM outside Rama | survived here only because the in-process cluster is one JVM. Memory of a process | run, all logs |
| B3.iii c | a store outside Rama, written under the batch id | the docs' own road for writes that Rama cannot set back | read, not run |
| B4 | the whole pattern | attempt 1 read the clock and kept it; forced failure; attempt 2 read another clock and wrote the kept one: all four rows carry the first attempt's clock. On both roads that wait for the keeper, and again in the commit window | run, `keep.log`, `commit.log` S4, S5, S7 |
| B5.a | a function replaced before the launch | takes effect, when the replacement is a function that a var holds. A replacement that closes over an atom makes the launch fail: "Object cache disallowed {:class clojure.lang.Atom}" | run, `redef.log` |
| B5.b | a function replaced after the launch | no effect on a function that the dataflow calls, until the module is updated; at once on a function that plain Clojure code calls | run, `redef.log` |
| B6.1 | how often a topology runs with nothing new | every 363 to 475 ms with nothing set in the in-process cluster (middle 366). `topology.microbatch.empty.sleep.time.millis` governs it: set to 2000 the gap was 2116 to 2220 ms, set to 60000 it was 60268 ms | run, `idle.log` |
| B6.2 | does a batch block that starts from a constant run then | yes, in every run, and a block that goes to every task reached all four tasks in every run. The batch id goes up by one on each run, a run that got nothing too | run, `idle.log` |
| B6.3 | the run after a batch with things | began 116 to 125 ms after that batch began, also with the sleep set to one minute | run, `idle.log` |
| B7.1 | do tasks show a batch at different instants | yes. With nothing held, 0.3 to 3.0 ms apart over ten batches. With one task's thread held, three tasks showed the batch and the fourth showed it 1.0 s later, and 3.3 s later in the control | run, `commit.log` S1, S2, `commit-control.log` |
| B7.2 | a failure while the batch commits, after three tasks showed their part | run, by two ways: the phase timeout, and a throw on the held task's thread. Both times Rama ran the batch again and the three tasks' rows were replaced | run, `commit.log` S3, S6 |
| B7.3 | the same with the clock kept | the clock that a reader was given first did not change; with rows that are the same in every part, a select was given no change at all | run, `commit.log` S4, S5, S7 |
| B7.4 | what was not run | a real cluster, a killed process, a change of leader; see the end | — |

## B1, by the lines

All of this is read, not run. Where a run of this probe met the same point, the
row above says so.

**When a write becomes visible.**
`12-microbatch-topologies.md:124`: "After all PStates on a task have been
checkpointed and replicated, their external versions are updated to what was
just checkpointed. This is when changes are made visible to clients and other
topologies. This means all changes to all PStates on a task in a microbatch
become visible at the exact same time (though changes on different tasks may
become visible at slightly different times)."
`12-microbatch-topologies.md:120`: "User PStates have separate *internal* and
*external* views which are managed separately. This reset only affects the
internal view, and topology code always operates on the internal views of user
PStates."

**Who sees which view.** `23-acid-semantics.md:17`: "The internal view of
PStates is only seen by topologies accessing their own PStates."
`23-acid-semantics.md:19`: "The external view is always seen by clients outside
the cluster or topologies querying PStates they don't own." The skill says it
for each reader, `microbatch.md:130`: "readers outside the owning topology —
query topologies, foreign reads, other topologies — see only committed state."
For a push, the page on PStates says that a `ProxyState`'s "value updates in
the background" (`15-pstates.md:436`). No line was found that says at which
instant a push of a microbatch PState is sent. The runs: at the task's commit,
within a millisecond of the select.

**Whether tasks show a batch at different instants.** Line 124 above says "at
slightly different times". The ACID page says the other thing in the same
breath as atomicity, `23-acid-semantics.md:37`: "Since none of the changes
become visible until the commit phase, all the computation in a microbatch to
all PStates across all partitions become visible together." The skill is plain,
`microbatch.md:128`: "each task commits independently and its writes become
visible as soon as its own commit finishes — so external readers can observe
two tasks on different microbatches at the same moment. The topology does not
start the next microbatch until ALL tasks have committed successfully."

**When and why Rama runs a batch again.** `12-microbatch-topologies.md:116`: "If
a microbatch fails (e.g. an exception in the topology, a leadership change
during processing, or a timeout), it is re-attempted with the same microbatch
ID and an incremented version." Line 158: "`topology.microbatch.phase.timeout.seconds`:
This is the timeout for each phase of a microbatch. If a phase takes longer than
this, it will fail and the microbatch will retry from the start." The skill,
`microbatch.md:129`: "A record that throws deterministically retries forever."

**What Rama does to the topology's PStates on a second attempt.**
`12-microbatch-topologies.md:116`: "each microbatch attempt starts with the
PStates at exactly their state after the previous microbatch ID." Line 120: the
priming phase "resets every user PState ... to the state at the end of the
previous microbatch ID." Line 124: "The latest versions for the last two
microbatch IDs are always kept, ensuring the microbatch can restart with the
state from the last microbatch ID if any worker involved in the commit phase
were to fail." And the line that the store's closing rests on, line 128: "If you
use `shufflePartition`, random numbers, or query mirror PStates in your
topology, the results after a retry could be different than the previous version
for the same microbatch ID. ... **When a new version of a PState is computed for
the same microbatch ID, the new version replaces the old version in the external
view of the PState.**" (The bold is mine.)

**Whether the records and the batch id are the same.** Line 116: "Each attempt
for the same microbatch ID processes the exact same set of data from the depots,
even if more data has accumulated on the depots since the first attempt." Line
122: "The first time `explodeMicrobatch` is called on a depot source for a
microbatch ID, the range of data to read per partition for that microbatch ID is
written and replicated." `25-integrating.md:336`: "The microbatch ID is a 64 bit
value that increments by one with each successful microbatch. ... if a microbatch
attempt fails, the microbatch is retried with the same microbatch ID and the
exact same data from each depot." The docstring of `ops/current-microbatch-id`
in the 1.6.0 jar: "Returns ID of current microbatch attempt. ... This can be
used to achieve fault-tolerant exactly-once update semantics when updating
external systems".

**A depot append from inside an attempt.** `12-microbatch-topologies.md:130`:
"If you do depot appends as part of your microbatch topology to either the same
module or other modules, those currently do not have exactly-once semantics in
the face of failures and retries. However, this is on our roadmap." The skill,
`microbatch.md:127`: "A retry re-appends." The docstring of
`depot-partition-append!` in the 1.6.0 jar: "Append data to the partition of the
depot represented by current task. ... Ack level can be `:ack`, `:append-ack`,
or `nil`." What `:ack` waits for, `14-depots.md:206`: "`AckLevel.ACK` waits for
the same condition plus waiting for all colocated stream topologies to process
the data, including replication of any updated PStates."

**State that Rama does not set back.** The skill, `microbatch.md:138`: "writes to
TaskGlobals, external databases, or other non-PState stores are NOT rolled back
on retry. Use `ops/current-microbatch-id` to make these writes idempotent."
`25-integrating.md:338` to 359 give the pattern for a store outside Rama: each
value keeps the batch id that last wrote it and the value before.
`task-globals.md:153`: "Not durable — TaskGlobals are recreated on worker
restart."

## B2: a failure while the batch is processed (`logs/fail.log`)

The module's last batch block throws on task 3, after the block that writes the
rows is done on every task. Batch 4 carried four things, one for each task.

```
    4121.3  INSIDE  batch 4 attempt 1 began at 4094.5 and read the clock 1791747415687
    4121.4  INSIDE    its own PState held before: {:batch "3", :clock 1791747415519, :attempt 1}
    4121.6  INSIDE    the task global held before: {"note" {:batch "3", :clock 1791747415519, :attempt 1}}
    4149.1  INSIDE  batch 4 attempt 1 task 3 : the last step THROWS on purpose, after 4 rows were written
    4153.2  OUTSIDE one more thing, value "extra" for task 0's key, was appended while attempt 1 still ran
    4324.9  SELECT  task 1 key-c now gives [:read-threw "CallbackException: Callback failure"]
    4535.1  INSIDE  the task global of task 0 was made ready, and it is empty
    4753.4  INSIDE  batch 4 attempt 2 began at 4726.4 and read the clock 1791747416319
    4753.5  INSIDE    its own PState held before: {:batch "3", :clock 1791747415519, :attempt 1}
    4753.5  INSIDE    the task global held before: {}
    4796.5  SELECT  task 0 key-a now gives {:value "A", :clock 1791747414964, :attempt 1, :batch "1"}
    4797.1  PUSH    task 0 key-a got ResyncDiff · new {:value "A", ... :batch "1"} · THE SAME as the value before
    4845.3  SELECT  task 0 key-a now gives {:value "B", :clock 1791747416319, :attempt 2, :batch "4"}
    4845.2  PUSH    task 0 key-a got NewValueDiff · new {:value "B", :clock 1791747416319, :attempt 2, :batch "4"} · before {:value "A", ...}
    6533.1  SUMMARY the batch that carried "B" has id 4 and began 2 time(s)
    6533.3  SUMMARY   attempt 1 got, by task: {0 [["key-a" "B"]], 1 [["key-c" "B"]], 2 [["key-j" "B"]], 3 [["key-b" "B"]]}
    6533.4  SUMMARY   attempt 2 got, by task: {0 [["key-a" "B"]], 1 [["key-c" "B"]], 2 [["key-j" "B"]], 3 [["key-b" "B"]]}
    6533.5  SUMMARY   every attempt got the same things in the same order on each task: true
    6533.5  SUMMARY the batch that carried "extra" has id 5 and began 1 time(s)
```

What the lines show:

- No watcher was ever given a row with `:attempt 1` of batch 4. The selects and
  the queries read every 0.2 ms; they gave row "A" until their reads began to
  throw, about 175 ms after the throw, and row "A" again after the restart,
  then the second attempt's row on all four tasks within 0.8 ms (4844.9 to
  4845.7). A watcher could not read for about 470 ms in between, so "never"
  holds for the instants at which a read came back.
- Each push got, after the restart, a `ResyncDiff` that carried the value it
  already had, and then one `NewValueDiff` with the second attempt's row.
- The second attempt had the same batch id and the same four things. The thing
  that was appended while attempt 1 still ran was not in attempt 2. It was
  batch 5.
- Batch 10 failed twice in a row and began three times, with the same things
  each time (`fail.log`, 7025 to 11494).
- Rama's own lines for the throw (`fail-rama-lines.log`): "Unexpected throwable!
  Will be treated as a fatal! {:event-tags [:microbatch :mb :topology-event]}",
  then "One or more monitored components have become unhealthy! Triggering
  system shutdown. {:unhealthy-component-keys [:task-thread-systems]}".

**This surprised me.** I had taken "an exception in the topology" to mean that
the runner drops the attempt and starts another. In this cluster a throw in the
topology's code brings the worker down and up again. Whether a worker on a real
cluster ends its process there was not run; the log's word is "fatal".

## B3: what survives a failed attempt

**(i) The topology's own PState: set back** (`fail.log`). Step 1 of each attempt
reads a note in a PState of the topology, then writes its own. Attempt 1 of
batch 4 wrote `{:batch "4" :attempt 1}`. Attempt 2 read `{:batch "3" ...}`, what
the run before had left. The same for a row: attempt 2 saw row "A" before its
write, though attempt 1 had written row "B" at that key. Attempts 2 and 3 of
batch 10 both read batch 9's note. Durable, and of no use for this: Rama sets it
back.

**(ii) A depot append, consumed by a stream topology of the same module**
(`keep.log`). Inside the attempt, on task 0:
`(depot-partition-append! *clocks {batch id, clock} :ack)`, then a
`local-select>` on the stream topology's PState on the same task. The stream
topology, `keeper`, writes a batch id only when it holds none.

```
   31722.4  COST    read first, append with :ack when nothing is kept : the road, inside the attempt, in batches that got a thing: 40 runs · least 2.9 · middle 3.2 · 9 of 10 under 3.6 · most 3.9 ms
   31722.5  COST    ... in runs that got nothing: 41 runs · least 2.9 · middle 3.2 · 9 of 10 under 3.5 · most 3.8 ms
   31722.6  COST    ... from the start of step 1 to step 3 on task 0, batches that got a thing: 40 runs · least 53.9 · middle 54.4 · 9 of 10 under 54.9 · most 55.1 ms
   31722.7  COST    ... from the append call to the row read from outside: 40 runs · least 126.0 · middle 192.5 · 9 of 10 under 363.3 · most 371.8 ms · 0 never came
   31722.8  COST    ... what the road did: {:appended-then-read 81} · clocks that came out nil: 0
   44503.0  COST    append with :ack on every attempt : ... in batches that got a thing: 40 runs · least 2.8 · middle 3.0 · 9 of 10 under 3.6 · most 4.5 ms
   19230.5  COST    no road : from the start of step 1 to step 3 on task 0, batches that got a thing: 40 runs · least 50.9 · middle 51.3 · 9 of 10 under 51.6 · most 51.8 ms
   19230.6  COST    no road : from the append call to the row read from outside: 40 runs · least 129.7 · middle 198.4 · 9 of 10 under 281.2 · most 374.6 ms · 0 never came
   64572.5  COST    append with :append-ack : what the road did: {:appended-append-ack-then-read 41} · clocks that came out nil: 41
   71325.1  COST    append with no ack : what the road did: {:appended-no-ack-then-read 41} · clocks that came out nil: 41
   71941.1  RUN     records on the depot of clocks, task 0's partition: {:start-offset 0, :end-offset 252}
```

What the lines show:

- The ack level must be `:ack`. With it the same attempt read the keeper's
  PState back every time. With `:append-ack` and with no ack it read nil every
  time: the attempt went on before the keeper had written.
- The road took about 3 ms inside the attempt. An attempt of this module up to
  its last step took 51 ms without the road and 54 ms with it. From outside,
  from the append call to the row, nothing can be told apart: the middle was
  180 to 198 ms on every road, since the wait for the next run is most of it.
- A run that got nothing paid the same 3 ms and appended a record too, since
  step 1 starts from a constant and runs in every run (B6). 252 records were on
  the depot of clocks after about 65 s. Nothing in the probe removes a record or
  a kept row.
- One thing here is not explained: with `:append-ack` the road itself took
  1.4 ms, and the attempt up to its last step took 77 ms, not 51.

What is durable: the depot's record and the keeper's PState, both state of the
cluster. Read: `23-acid-semantics.md:112`, "Writes to PStates and depots are not
made visible until they're durable on disk on the leader and durable on disk on
all ISR ("in-sync replica") followers." Run: the second attempt, after the
worker's restart, read the kept row. That restart is inside one JVM (see B7,
"What could not be run").

**(iii) Other roads.**

- A task global (`declare-object`), run in `fail.log`: it held the note of the
  run before, from one run to the next, while the worker lived. After the throw it was
  "made ready, and it is empty", and attempts 2 and 3 read `{}`. Memory of a
  process. It would hold across a failure that does not restart the worker, such
  as the phase timeout of B7; that was not run for a task global.
- The JVM's own state (the atoms by which this probe counts attempts) survived
  every failure here, because the in-process cluster shares one JVM with the
  check. Memory of a process.
- A store outside Rama, written under the batch id: the docs' road, read, not
  run. No such store is here.
- A depot or a PState of another module (a mirror): read, not run. The skill
  says an ack on a mirror depot does not wait for the other module's
  topologies (`depot-reference.md:67`), so the same attempt could not read the
  value back as it does in (ii).
- What needs no road: the batch id, the things of the batch and their order on
  each task, and every row as the batch before left it, are the same on each
  attempt (B2, B3.i).

## B4: the whole pattern (`logs/keep.log`)

```
   72724.6  INSIDE  batch 448 attempt 1 task 3 : the last step THROWS on purpose
   73424.0  INSIDE  batch 448 attempt 1 began at 72673.5 · read the wall clock 1791746701702 · the road :appended-then-read gave 1791746701702 in 2.97 ms · things 4
   73424.0  INSIDE  batch 448 attempt 2 began at 73298.5 · read the wall clock 1791746702329 · the road :found-kept gave 1791746701702 in 0.12 ms · things 4
   73424.2  INSIDE  the keeper got, at 72672.0 : batch 448 clock 1791746701702 · it held before: nil → written
   73424.4  INSIDE  the keeper got, at 73168.1 : batch 448 clock 1791746701702 · it held before: 1791746701702 → not written
   73427.8  OUTSIDE $$kept gives for batch 448 : 1791746701702
   73429.1  OUTSIDE the row of task 0 : {:value "X", :clock 1791746701702, :attempt 2, :batch "448"}
   73432.6  RESULT  batch 448 : attempt 1 read 1791746701702 · attempt 2 read 1791746702329 · the rows that attempt 2 wrote carry #{1791746701702} → the rows carry the FIRST attempt's clock: true
```

The second attempt read a wall clock 627 ms later and wrote the first attempt's.
The same on the road that appends on every attempt (batch 451): the keeper got
the second attempt's clock and did not write it.

**The keeper was given the first record twice.** At 73168.1, after the worker's
restart, Rama gave the keeper the record of attempt 1 again; attempt 2 had
appended nothing on this road. So the stream topology ran the record at least
twice, and "first write wins" is what kept the second run harmless. The docs
say a stream topology is at least once; the run shows it happen.

A batch that never fails pays what B3 measured: the 3 ms, in each run.

## B5: failing an attempt of a module without a hook in its source (`logs/redef.log`)

```
    2145.1  OUTSIDE the launch gave [:threw "ExceptionInfo: Object cache disallowed"]
    3350.5  INSIDE  RedefBefore : an attempt of batch 1 got "a-1"
    3350.6  INSIDE  the REPLACEMENT of direct-before ran: it throws, this once
    3773.1  INSIDE  RedefBefore : an attempt of batch 1 got "a-1"
    3773.3  INSIDE  the REPLACEMENT of direct-before ran: it gives its argument
    5756.4  OUTSIDE direct-after, which the DATAFLOW calls, is now replaced
    5792.8  INSIDE  RedefAfter : an attempt of batch 3 got "b-2, after direct-after was replaced"
    5833.3  OUTSIDE its row came: true
    6133.5  OUTSIDE inner-after, which PLAIN CODE calls, is now replaced
    6163.6  INSIDE  the REPLACEMENT of inner-after ran: it throws, this once
    6461.4  INSIDE  RedefAfter : an attempt of batch 5 got "b-3, after inner-after was replaced"
   11410.3  OUTSIDE update-module! gave :updated
   11673.0  INSIDE  the REPLACEMENT of direct-after ran: it throws, this once
   15085.3  OUTSIDE with-redefs has ended: the first function is back in its var
   15149.9  INSIDE  the REPLACEMENT of direct-with ran: it gives its argument
```

- (a) `alter-var-root` before `launch-module!` takes effect on a function that
  the dataflow calls. The replacement must be a function that a var holds. One
  that closes over an atom made the launch fail with "Object cache disallowed
  {:class clojure.lang.Atom}".
- (b) After the launch, replacing a function that the dataflow calls did
  nothing: the batch ran with the first function. It did nothing after the
  worker's restart that a throw caused, either. It took effect after
  `update-module!`. Replacing a function that plain Clojure code calls took
  effect at once.
- (c) `with-redefs` around the launch works as (a) does, and the module kept the
  replacement after `with-redefs` had ended.
- Read from these three together, not run as such: Rama takes the function that
  the var holds when the module is launched or updated; a call from plain code
  goes through the var each time.
- A throw made this way fails an attempt while it is processed, and with it the
  worker starts again (B2). It does not reach the commit.

## B6: a topology whose depot holds nothing new (`logs/idle.log`)

```
   84100.1  REPORT  default : 224 runs began · batch ids 0 to 223 · the block that goes to every task reached, in each run, the tasks {#{0 1 3 2} 224}
   84100.7  REPORT  default : between two runs that both got nothing — 214 gaps, least 363 ms, middle 366 ms, most 475 ms
   84103.0  REPORT  default : run 16 batch 16 began at 8719 · 369 ms after the run before · got things [1], the first 201 ms after its append call
   84103.0  REPORT  default : run 17 batch 17 began at 8841 · 122 ms after the run before · got nothing
   84103.6  REPORT  two-sec : between two runs that both got nothing — 35 gaps, least 2116 ms, middle 2121 ms, most 2220 ms
   84104.3  REPORT  minute : run 7 batch 7 began at 18194 · 3124 ms after the run before · got things [12 15], the first 155 ms after its append call
   84104.5  REPORT  minute : run 8 batch 8 began at 18310 · 117 ms after the run before · got nothing
   84104.5  REPORT  minute : run 9 batch 9 began at 78578 · 60268 ms after the run before · got nothing
```

- A run that got nothing was followed by the next after 363 to 475 ms when the
  module set nothing. A run takes about 117 ms here, so the sleep is about
  250 ms.
- The setting is `topology.microbatch.empty.sleep.time.millis`, run: 2000 gave
  2116 to 2220 ms, 60000 gave 60268 ms. Read: the skill gives one minute as
  Rama's default (`microbatch.md:34`), and the 1.6.0 jar's cluster simulator
  holds the constant 250 beside that option's name
  (`sim_cluster_manager$add_testing_default_dynamic_options`). So the
  in-process cluster's 366 ms is its own, and a real cluster would run an idle
  topology once a minute unless the module sets the option.
- A batch block that starts from a constant ran in every run. The batch id went
  up by one on every run.
- The run after a batch with things began 116 to 125 ms after that batch began,
  in 13 of 13 cases, also with the sleep at one minute. After that run, nothing
  for the whole sleep.
- A thing appended during the sleep began a run 13 to 259 ms after its append
  call, also with the sleep at one minute. Read: the setting for that is
  `topology.microbatch.poll.new.data.frequency.millis`.

## B7: the window in which one task has shown its part (`logs/commit.log`)

Step 3 of the module goes to task 1 only. There it asks for a hold of task 3's
thread, waits until the hold has begun, and ends 150 ms later. Task 3 has done
all of its part by then. So tasks 0, 1 and 2 commit, and task 3 cannot until its
thread is free.

**S2, a hold of 1200 ms, less than the phase timeout of 2 s.** Tasks 0 to 2 gave
the batch's rows at 10344; task 3 gave its row at 11355, when its thread was
free. One attempt. A thing appended at 10459, during the hold, was in batch 26,
which began at 11369.9: Rama began no run before task 3 had committed.

**The control, `logs/commit-control.log`: a hold of 3500 ms with no phase timeout
set.** Tasks 0 to 2 gave the rows at 6069 to 6071, task 3 at 9385.7. One attempt.
So the second attempts below come from the timeout set to 2 s and from nothing
else.

**S3, a hold of 3500 ms, more than the phase timeout.**

```
   13221.0  INSIDE  task 3 : its thread is HELD for 3500 ms from now
   13371.1  INSIDE  batch 32 attempt 1 : step 3 ends; the attempt has no more code to run
   16721.1  INSIDE  task 3 : its thread is free again
   16757.6  INSIDE  batch 32 attempt 2 begins · it read the wall clock 1791747458978 · it will write the clock 1791747458978
   18375.3  SUMMARY "held-long" : batch 32 began 2 time(s)
   18375.4  SUMMARY   PUSH    task 0 key-a was given, in order: [["13433" {:clock 1791747455388, :attempt 1}] ["17006" {:clock 1791747458978, :attempt 2}]]
   18375.5  SUMMARY   PUSH    task 3 key-b was given, in order: [["16874" {:clock 1791747458978, :attempt 2}]]
   18375.5  SUMMARY   SELECT  task 0 key-a was given, in order: [["13433" {:clock 1791747455388, :attempt 1}] ["16873" {:clock 1791747458978, :attempt 2}]]
   18375.6  SUMMARY   SELECT  task 3 key-b was given, in order: [["16874" {:clock 1791747458978, :attempt 2}]]
```

On tasks 0, 1 and 2 a select and a push were given the first attempt's row, with
the first attempt's clock, for 3.44 s. Then Rama ran batch 32 again, and they
were given the second attempt's row with another clock. Task 3 was only ever
given the second. This is the case that the store's page describes and closes
against. It is seen here. No worker was restarted: no read threw.

The second attempt began only when task 3's thread was free, about 3.4 s after
the commit began and about 1.4 s after the timeout: an attempt needs every task
to begin.
So the first attempt's rows stay readable for as long as the late task is away.

**S6, a hold of 700 ms and then a throw on task 3's thread** (from a stream
topology of the module). The same from a reader's side: tasks 0 to 2 gave the
first attempt's row at 31679, the worker was started again, and all four gave
the second attempt's row with another clock at 32739.

**S4, S5, S7: the same holds with the clock kept as in B3 (ii).**

```
   22803.5  INSIDE  batch 39 attempt 2 begins · it read the wall clock 1791747465023 · it will write the clock 1791747461423
   24420.1  SUMMARY   SELECT  task 0 key-a was given, in order: [["19489" {:clock 1791747461423, :attempt 1}] ["22918" {:clock 1791747461423, :attempt 2}]]
   28867.5  INSIDE  batch 46 attempt 2 begins · it read the wall clock 1791747471087 · it will write the clock 1791747467502
   30488.8  SUMMARY   PUSH    task 0 key-a was given, in order: [["25548" {:clock 1791747467502}] ["29118" {:clock 1791747467502}]]
   30488.9  SUMMARY   SELECT  task 0 key-a was given, in order: [["25548" {:clock 1791747467502}]]
   30488.9  SUMMARY   SELECT  task 3 key-b was given, in order: [["28986" {:clock 1791747467502}]]
   29118.3  PUSH    task 0 key-a got NewValueDiff · new {:value "held-same", :clock 1791747467502, :batch "46"} · THE SAME as the value before
```

- S4: the row still carried the attempt's number, so it changed, and its clock
  did not.
- S5: the row carried no attempt's number, so both attempts wrote the same row.
  A select on tasks 0 to 2 was given the row once, 3.4 s before task 3 showed
  it, and never a change. Each push on those tasks got one more callback after
  the second attempt, a `NewValueDiff` whose new value is the value before.
- S7, with the throw: the same row before and after the worker's restart. Each
  push got a `ResyncDiff` with the same value.

**What could not be run, and why.**

- A worker's process killed while it commits, a machine lost, a change of
  leader. The in-process cluster is one JVM with one copy of each task. Its
  "restart" is Rama taking a worker's parts down and up inside that JVM. What a
  task has written to disk and not yet shown when the process dies was not
  reached. The docs' line for it is `12-microbatch-topologies.md:124`: "The
  latest versions for the last two microbatch IDs are always kept, ensuring the
  microbatch can restart with the state from the last microbatch ID if any
  worker involved in the commit phase were to fail."
- The instant inside one task between its checkpoint and its external view.
  Nothing of ours runs there.
- A failure of the commit on a real cluster by its own causes. S3 made the
  timeout happen by holding a thread with the probe's own code; S6 by a throw
  from the probe's own stream topology.
- Rama's own phase timeout when nothing is set. The control shows only that it
  is more than 3.5 s.

## Seen on the way

- After a worker's restart, reads from outside threw "CallbackException:
  Callback failure" for about 0.5 s (1.2 s in S7), and some reads did not come
  back for much longer. In `fail.log`, attempt 3 of batch 10 had committed by 8038 (task 1's
  select gave its row then); the selects of tasks 2 and 3 gave it at 9191 and
  task 0's at 9993. After `update-module!` three of four selects were 1.4 s
  behind the fourth. I have not found the cause; it looks like a call that was
  in flight across the restart.
- A push was given a `NewValueDiff` whose new value is its old value about
  100 ms after it was opened, and again after a resync (`fail.log` 3656.5,
  10086.6; `commit.log` 3994).
- When a second attempt replaced a row that a task had already shown, the push
  of it came 131 to 133 ms after a select could read it, in S3, S4 and S5
  (17006 against 16873). On task 3, which had shown nothing before, select and
  push came together. Not explained.
- Rama logged nothing at ERROR or WARN when an attempt failed by the phase
  timeout.

## Not settled by these runs

- A real cluster, two nodes, replication. `:ack` waits for the depot's record
  and the keeper's row to be replicated (`14-depots.md:206`), so the 3 ms here
  is a floor of one machine with one copy, not a cost.
- What happens to the attempt when the keeper itself fails on the record. Read:
  `14-depots.md:214` says an `:ack` append gets an exception when a colocated
  stream topology fails the record on its first try. Not run.
- Whether the road is paid in a run that got nothing is the module's choice.
  The probe paid it in every run.
- How the depot of clocks and the kept rows are ever made smaller.
- The clock is one of the things that an attempt of the store makes anew. Its
  page names two more: the lock bytes, and the word for a layer of the other
  gate. This probe kept one number. The road of B3 (ii) puts what it keeps on a
  depot, and the store's rule 5 says that a lock's bytes are never on a depot.
- Many batches a second, large batches, and the store's own module. The probe
  does not load it.
- B5 on code that is compiled ahead of time or with direct linking.
