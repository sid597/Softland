# The store — open questions about Rama

Things that the store rests on and does not know: how Rama behaves in fourteen places. They are kept
here as the store's own open questions; nothing was sent to Rama's team (Sid, 12 October 2026). This
is not the queue of what Sid has not decided. That queue is
`src/proposal/backend-on-rama-2026-10-02/decisions/OPEN-QUESTIONS.md`, and its answers are in the
decisions of the [source entry](README.md).

Each entry says what is still open, what Rama's public sources already say, what the store's runs
saw, and what the store does meanwhile and where. When a question closes, its answer goes into the
docstring or the decision that it changes, and the entry leaves this page.

The store rests on three of the first ten no more, since the build of 13 and 14 October 2026 that
followed Sid's decisions of 13 October (the [source entry](README.md), "Decisions of 13 October
2026 — the store comes out the same when it is run again"): questions 3, 8 and 9. None of the
three is answered. Each keeps its text, as the record of what was asked, under one line at its head
that says why the store no longer rests on it. Questions 11 to 14 are of that build.

**What the store saw.** Every run was on Rama 1.6.0, on the in-process cluster, on one machine;
nothing here was seen on a real cluster. The working of questions 1 to 10 is in
`src/proposal/backend-on-rama-2026-10-02/reads/`, written `reads/` below. The working of questions
11 to 14 is in `src/proposal/backend-on-rama-2026-10-02/run-again/`, written `run-again/` below;
its probes ran stand-in gates on modules of their own, and an entry that names a run on the
store's own module says so. Never open a file under a `runs/` folder whose name begins `raw-`.

**What was searched, on 12 October 2026.** Rama's pages (the snapshot of 22 March 2026 in
`reference/rama/docs/`, and the live site, which is at Rama 1.9.0), its Javadoc and Clojure API, its
release notes from 1.0.0 to 1.9.0, the mailing list rama-user from August 2023 to September 2026, Red
Planet Labs' public code, and their Rama skill as this repository holds it (`.claude/skills/rama/`,
of 5 August 2026). Of Rama's Slack only a public mirror was reached, and it holds 27 May to
28 September 2026. The Slack before that, and Rama's Discord, were not searched. The captures are in
`/mnt/data/projects/research/softland/rama-questions-2026-10-12/`.

**What was read for questions 11 to 14, on 13 and 14 October 2026.** Rama's pages in
`reference/rama/docs/` and the Rama skill, by the sessions that ran the probes, and Mastodon's
source for question 11. The mailing list, the Slack and Red Planet Labs' other code were not
searched for these four.

Rama's licence, `META-INF/RAMA-LICENSE.txt` in its jar, forbids reverse engineering, decompiling and
disassembling it. Nothing below comes from inside the jar.

| | Question | Now |
|---|---|---|
| 1 | A watched row that holds a byte array is sent whole | open |
| 2 | Which task a partitioning key goes to | open |
| 3 | `genHashingIndexKeys` outside a test | open, but for a select; the store no longer rests on it |
| 4 | `pstate.proxy.limit` | open, but that it can be set |
| 5 | The order of String keys with rare letters | open |
| 6 | The threads of Rama's client | answered for callbacks |
| 7 | A path that Rama does not serve | open for the stopped worker |
| 8 | An append with `:ack` inside a microbatch | open for the microbatch case; the store no longer rests on it |
| 9 | The id of a microbatch | open for a restore and a new declaration; the store no longer rests on it |
| 10 | Records handed again after a restart | answered, but for two defaults |
| 11 | A batch of a stream topology that fails with no restart of its worker | open; not reached in process |
| 12 | The order in which a microbatch runs the records of one partition | open: seen, and promised by no page that was read |
| 13 | When the copies that a batch sends with `\|all` are written | open: seen, and promised by no page that was read; the store is built so that it does not rest on it |
| 14 | Another number of tasks for a module whose rows are placed by the layer's hash | open for the way that keeps a layer on the task of its hash |

## 1. A watched row that holds a byte array is sent whole

**Open.** Why a reactive query (`foreign-proxy`) on a row with a byte array is handed a `ResyncDiff`
where the same row with a String gets a diff for each write. Whether a byte array inside a watched
value is supported. How bytes should be kept in a table that is watched.

**Rama's sources.** None names a byte array in a watched value. The pages say when a `ResyncDiff`
comes: "diffs are sent back to a `ProxyState` along with a CRC. The CRC is checked after applying
the diff, and if it doesn't match the `ProxyState` will resync from scratch"
(`reference/rama/docs/15-pstates.md:718`). The Javadoc of `ResyncDiff` names "module update
transitions, leader changes, or network problems". Release 1.4.0 fixed a case "where proxies would
resync unnecessarily", so Rama's team counts a resync that is not needed as a defect. That the CRC
fails for a byte array is a guess that nobody ran.

**Seen.** 20 writes 100 ms apart: 20 `KeyDiff` for rows with a String, 7 `ResyncDiff` for the same
rows with a byte array. 100 writes one after the other: 100 against 1. On the store's own tables a
live reader was told a change a middle of 1,000 ms after the ack
(`reads/runs/review-probe-held-value.txt`; `reads/PROGRESS.md`, ruling 86).

**Meanwhile.** A fact's row holds its value as a String: the docstring of `facts` in
[tables.clj](softland/store/record/tables.clj). The rows of the answers keep their byte array, the
digest; nothing watches them.

## 2. Which task a partitioning key goes to

**Open.** Whether a client can learn the partition of a key under the default partitioner: a call
for it, or the name of the hash.

**Rama's sources.** "`hashPartition`: Chooses target task based on hash of input modded by number of
tasks in module" (`reference/rama/docs/16-partitioners.md:153`). Nathan Marz, Slack, 5 June 2026:
"the queries are choosing tasks by hash/mod of the key". No source names the hash, and no public
call goes from a key to a task, in 1.6.0 or in 1.9.0. A PState can declare a key partitioner of its
own, "a function taking in as arguments the number of partitions for the PState and the extracted
key" (`reference/rama/docs/15-pstates.md:238`); the store's declare none.

**Seen.** Read, not run: no such call in Rama's public API of 1.6.0 (`reads/FULL_SPEC_REVIEW.md`, "A
ruling that could not hold as worded"; `reads/PROGRESS.md`, ruling 93).

**Meanwhile.** The read path counts its pushes by table and routing id, not by table and task: the
docstring of `counted-under` in [read_path.clj](softland/store/read_path.clj).

## 3. `genHashingIndexKeys` outside a test

**14 October 2026: the store no longer rests on this, because the read path names no task.** It
asks no task by number and uses no class of Rama's for that: every source of a microbatch layer is
read under the routing id of the layer that the read starts from (`run-again/PART-B.md`, section
7). The function that "Meanwhile" names is gone. The proofs still use the Clojure form of these
keys, in a test, which is what Rama's sources show them for.

**Open.** Whether `com.rpl.rama.Helpers/genHashingIndexKeys` is supported for a client in
production. How a client holds a reactive query on every partition of a PState that is not global.

**Rama's sources.** For a select on every partition Rama's author names another way. Nathan Marz,
rama-user, 16 November 2024: "If you want to query all partitions of a PState, you should make a
query topology." (https://groups.google.com/g/rama-user/c/s_gFziEgm3c). And on 8 October 2024: "A
query that reads across all partitions on every invocation has a scaling limit."
(https://groups.google.com/g/rama-user/c/uaXwkuw3Cz4). On the keys themselves nothing is said for or
against production. The Javadoc says only "Generates a key that hashes to each task in a range of
tasks". The Clojure form, `gen-hashing-index-keys`, is in `com.rpl.rama.test`, and Red Planet Labs'
public code uses it in tests only. On a reactive query on every partition: nothing.

**Seen.** On the in-process cluster it works for a select and for a reactive query
(`reads/PROGRESS.md`, ruling 62).

**Meanwhile.** The read path names the tasks of a microbatch layer with these keys, for a run across
the entities of the base: the docstring of `task-routing-ids` in
[read_path.clj](softland/store/read_path.clj). Sid's line on this, in ruling 62: "we will do it
right as rama team will suggest". For a select, what Rama's author suggests is a query topology and
what the read path does is these keys. Whether the read path changes is Sid's, and is not decided.

## 4. `pstate.proxy.limit`

**Open.** What the option bounds: one PState on one task, all the PStates of a task, or a module,
and whether for all clients together. What a higher value costs a task. Whether a refused opening is
meant to be slow and to log much.

**Rama's sources.** The option can be set for one PState when a module is launched; Rama's Clojure
API uses it as its example: `(set-launch-pstate-dynamic-option! setup $$my-pstate
"pstate.proxy.limit" 123)`. Red Planet Labs raise it themselves on Rama 1.6.0: their TPC-C code runs
with `pstate.proxy.limit=20000` among the module's options
(https://github.com/redplanetlabs/rama-tpcc, README, 12 March 2026). Rama's configs page says that
dynamic options "are documented on the Cluster UI"; nobody looked there. On cost the pages give no
number: "The PState partition uses memory to track the subscription and uses CPU to process diffs
for the subscription" (`reference/rama/docs/15-pstates.md:724`), and a write processes its
subscribers "synchronously with the transform up to a time limit" (`:1230`).

**Seen.** 1,000 reactive queries opened on one table on one task, the next was refused, and one
that was closed made room for one more. About 10,000 openings that Rama refused took 9.3 s; the
98,794 that it refused in one run left 8.8 million lines of log, about 1.0 GB. With 898 open on runs
that hold the rows being written, one act took a middle of 15.5 ms where it took 4.7 ms; with 100
open, 4.9 ms; with 898 open on rows that are not written, no more than with one (`reads/RECEIPT.md`,
sections 1 and 12; `reads/PROGRESS.md`, rulings 87 and 91). No run set the option.

**Meanwhile.** The read path holds 900 on one table under one routing id and turns a read away
itself, at once: the namespace docstring of [read_path.clj](softland/store/read_path.clj), "How
many pushes". The 900 is kept and not decided. In the base the routing id is the layer that a read
starts from, so the 900 bounds the pushes on the base of the askers who start from one layer, and
not what all such layers of one task hold on the base together; Rama's own bound counts those.
Derived, not run: each such layer holds a push on the base's own rows and one at least on a fact,
so the default bound holds about 500 of them for each task (the read path's
[page](softland/store/read_path/README.md), "The read path bounds its own pushes").

## 5. The order of String keys with rare letters

**Open.** How a String key is put into bytes on disk. By which order the sorted map that a client is
handed for a range finds a key. The two orders that could be meant, the order of UTF-8 bytes and
Java's order of Strings, differ only for a key that holds a letter from U+E000 on or one outside the
first plane.

**Rama's sources.** Keys on disk follow the bytes: "The way serialized forms are compared on disk is
with lexicographic order. Rama implements this property as well for built-in serialization for
relevant types like numbers and strings" (`reference/rama/docs/17-serialization.md:169`). Nathan
Marz, rama-user, 27 October 2023: "It's important for any type used as a key that it's serialized
order matches its unserialized order." (https://groups.google.com/g/rama-user/c/4Nb8OtVxnC4). So the
pages promise that the two orders agree for a String, and what the store saw looks like a place
where they do not. Nobody verified that, and no source reports a range that goes wrong with such
letters. The release notes list two repairs to the "map type returned from sorted range
navigators", in 1.1.0 and in 1.9.0, neither about lookup.

**Seen.** In two runs of a proof, a range between two ends that hold such letters gave fewer rows
than the table holds; a probe that asked the same ends by hand got all of them. The map that the
client makes of a range missed a key that it holds. The cause was not found (`reads/PROGRESS.md`,
ruling 88).

**Meanwhile.** A run between two such ends is turned away, and a key of such a run is held to its
two ends before it is given: the docstrings of `first-read-holds?`, `keys-of-run` and `row-in` in
[sources.clj](softland/store/read_path/sources.clj), and `before?` in
[rows.clj](softland/store/record/rows.clj).

**A thing to try, that reads no source.** `(class m)` and `(.comparator m)` on a range that a client
is handed. Red Planet Labs' public library `vector-backed-sorted-map` finds a key by binary search
over rows that it does not sort; whether the client's map is of that kind was not checked.

## 6. The threads of Rama's client

**Answered for callbacks.** "Every callback for a `proxy` call runs in the background on a dedicated
thread from a thread pool" (`reference/rama/docs/15-pstates.md:554`). The pool's size is a
configuration of the client, `foreign.proxy.thread.pool.size`, which "defaults to 2"
(`reference/rama/docs/26-all-configs.md:72`). That is the two threads that the store counted, and
the same holds on a real cluster unless the client sets it. Nathan Marz, rama-user, 22 April 2026:
"Everything for the proxy runs on the same thread, so if your callback function is blocking it will
prevent heartbeats from being sent." (https://groups.google.com/g/rama-user/c/tFEBAnOlUfk). In that
thread the worker's timeout for a heartbeat is 90 seconds, and the asker's callbacks stopped with no
word after it.

**Open.** Whether async selects complete on the same threads. Release 1.1.0 says that futures "are
now delivered on the client thread pool" and does not say which pool. No run of the store set the
size.

**Seen.** Two such threads. While one callback stayed for 2.5 s, other reactive queries' callbacks
on that thread waited, and so did selects and openings from other threads. A call of Rama that
blocks, made inside a callback, hung in one run of three (`reads/PROGRESS.md`, ruling 75, and the
paragraph "For the receipt, from this check" after ruling 80).

**Meanwhile.** The asker's function must return at once, and a read asked from inside it is turned
away: the namespace docstring of [read_path.clj](softland/store/read_path.clj), "A live read, and
the read's turn".

## 7. A path that Rama does not serve

**Answered for a whole table.** A reactive query on a whole top-level map or a whole subindexed
collection is not allowed: "you cannot proxy objects which index their elements separately
(top-level map or subindexed schemas)" (`reference/rama/docs/15-pstates.md:654`). The 100 seconds
follow from Rama's rule and the store's own log. The rule: "if a proxy errors 10 times within 120
seconds, it is forcibly terminated", on the first query too, and "the failure window size and
threshold can be configured" (`:732`). The log: the server wrote the error every 10 seconds
(`reads/push-probe/RESULTS.md`, "The three refusals").

**Open.** Whether a select with a path that the PState does not serve can stop a worker of a real
cluster. No source speaks of it.

**Seen.** One such select stopped a worker of the in-process cluster (`reads/PROGRESS.md`, ruling
70).

**Meanwhile.** The read path sends five forms of path and no other, and never asks for a whole table
or a whole collection.

## 8. An append with `:ack` inside a microbatch

**14 October 2026: the store no longer rests on this, because no topology of the store appends to
a depot.** The depot of batch clocks, its topology and the throw `clock-must-be-kept` are out of the
store: the door writes the time on the act, and no gate reads a clock (the
[source entry](README.md), decisions of 13 October 2026, entries 1 and 5; `run-again/PART-B.md`,
sections 1 and 4). The file that "Meanwhile" names is written again and holds no such step.

**Rama's sources.** What `:ack` waits for: all colocated stream topologies "to process the data,
including replication of any updated PStates" (`reference/rama/docs/14-depots.md:206`). Nathan Marz,
rama-user, 24 September 2026, of a client's append: "By "finished processing", it means that all
PState writes have replicated and are now visible."
(https://groups.google.com/g/rama-user/c/y2njqXKNMfA). For the call inside a topology, Rama's skill:
"`:ack`: Emits after append to depot is successful AND all colocated stream topologies have finished
processing it. DO NOT use `:ack` if depot is consumed by same stream topology as is appending it"
(`.claude/skills/rama/references/depot-design.md:66`). And a record of an attempt that failed stays,
while the next attempt appends again: such appends "currently do not have exactly-once semantics in
the face of failures and retries" (`reference/rama/docs/12-microbatch-topologies.md:130`); "A retry
re-appends" (`.claude/skills/rama/references/microbatch.md:127`).

**Open.** `:ack` from a batch block of a microbatch topology. No page and no public code shows it:
where Red Planet Labs' own code appends from a batch block it passes `:append-ack` (agent-o-rama,
`retries.clj`). And whether the read of the stream topology's PState after that ack, in the same
batch, is a thing to rest on; that it is follows from the lines above and nobody states it.

**Seen.** The append works in place, the stream topology's row is there when the append returns, and
a record appended by an attempt that failed stays. When the clock's topology did not answer, the
attempt waited until Rama's phase timeout, about 30 s. More than one copy was not run
(`reads/batch-probe/RESULTS.md`; `reads/PROGRESS.md`, rulings 17, 46 and 51).

**Meanwhile.** The microbatch gate rests on it: step 2 and the docstring of `clock-must-be-kept` in
[microbatch_gate.clj](softland/store/module/microbatch_gate.clj).

## 9. The id of a microbatch

**14 October 2026: the store no longer rests on this, because it keeps no row by the id of a
batch.** The table `batch-clock` is out of the store, and nothing of a batch is in a decision: not
its id, not its size, not where it is cut (the docstring of
[microbatch_gate.clj](softland/store/module/microbatch_gate.clj); the [source entry](README.md),
decisions of 13 October 2026, entry 5).

**Answered for an attempt and a restart.** "The microbatch ID is a 64 bit value that increments by
one with each successful microbatch" (`reference/rama/docs/25-integrating.md:336`). A batch that
fails "is re-attempted with the same microbatch ID and an incremented version", and both are "stored
on an internal PState dedicated to the topology"
(`reference/rama/docs/12-microbatch-topologies.md:116`). Through a restart: topologies "persist
their consumed offset / microbatch ID and resume from where they left off on restart of worker
processes" (`.claude/skills/rama/SKILL.md:28`).

**Open.** Whether an id can come again after a restore from a backup, after a topology is removed
and declared again under its name, or after a module is destroyed and launched again. Rama's backups
page says that a backup holds "Topology progress" beside the PStates and the depots. So a restore
probably takes the id back together with every PState of the module; that is a reading, and not
Rama's words.

**Seen.** The id held across a failed attempt, a restart of a worker and a module update with the
unchanged module. A restore, a killed process, a move of a leader and a topology declared again were
not run (`reads/RECEIPT.md`, section 12).

**Meanwhile.** The store keeps one row by this id. If ids began anew while that row stayed, no batch
of the base could ever be decided: the docstring of `batch-clock` in
[tables.clj](softland/store/record/tables.clj), and `clock-must-be-kept`.

## 10. Records handed again after a restart

**Answered.** A stream topology writes its progress on a depot partition "only ... after a
configurable number of records have been processed or a configurable amount of time has passed"
(`reference/rama/docs/11-stream-topologies.md:142`). And: "If a leader restarts or suddenly loses
leadership after it has successfully processed some records but before it has checkpointed that
progress, those records will get retried" (`:166`). So a restart hands again what was processed
after the last written progress, and not the depot from its start. The two settings are
`topology.stream.checkpoint.progress.threshold` and `topology.stream.periodic.checkpoint.seconds`.

**Open.** The defaults of those two settings; no public page gives them. And whether a restart on
the in-process cluster is the same as a real one in this. Rama's testing page says "There are no
differences in module capabilities or execution semantics between an InProcessCluster and a real
cluster" (`reference/rama/docs/27-testing.md:20`), and its skill names a module update with the
unchanged module as the way to stand in for a worker's restart
(`.claude/skills/rama/references/testing.md:222`).

**Seen.** After a worker was started again, a stream topology was handed every record of its
partition once more, in order: eight of eight, each processed seconds before. That fits the rule,
and eight records cannot tell it from a start at the beginning
(`reads/PLAN_VALIDATION-what-the-gates-keep.md`, point 6). On a stand-in gate of the streaming
gate's form, after a throw: the worker started again, and the records of the partition were then
decided in the depot's order, an act behind the failed one once, after it; 23 runs of 23
(`run-again/probes-order-after-a-failure/RESULTS.md`, S1).

**Meanwhile.** Check (a) of the walk that both gates call makes a record that comes again
harmless: a second run after a commit finds the answer and writes no row (the namespace docstring
of [walk.clj](softland/store/module/walk.clj), "When a thing is run again").

## 11. A batch of a stream topology that fails with no restart of its worker

**Open.** Whether Rama can fail a batch of a stream topology with no restart of its worker, and
drop its rows, when the event's code holds no partitioner, no append and no wait: on a cluster with
more than one copy, where a batch waits for its followers at its end. And, if it can, in which
order the records behind it on the partition then run: whether the first attempt of a later record
can run before the second attempt of the failed one.

**Rama's sources.** With the retry mode "all after", "a failed record will cause all records after
it on the same depot partition to also be retried (even if they already succeeded). If the order of
processing is critical ... this mode is appropriate"
(`reference/rama/docs/11-stream-topologies.md:162`). "Rama also fails a record if its event tree
doesn’t succeed within a configurable timeout" (`:164`). After a throw or a crash "all PState writes
during that batch of execution will be discarded" (`:170`). At a batch's end a leader "replicates
that entry to followers before making the changes from that entry **visible**"
(`reference/rama/docs/21-replication.md:41`). The in-process cluster has one copy: "The replication
factor cannot be configured for a module on an `InProcessCluster`"
(`reference/rama/docs/27-testing.md:63`). Mastodon's source sets the retry mode "all after" on the
sources whose order matters, sets no option for this and uses no offset of a depot
(`run-again/probes-order-after-a-failure/RESULTS.md`, K4). None of these says whether a batch can
be failed while its worker stays up and its event's code neither throws nor waits.

**Seen.** On a stand-in gate of the form that the store's streaming gate has, with the same options
of its source, 110 runs, each on a fresh cluster
(`run-again/probes-order-after-a-failure/RESULTS.md`). No failure that could be produced in process
made a second module's run over the same depot come out in another order: 87 runs of 87. A throw
starts the worker again, and the records are then decided in the depot's order: 23 of 23. A stall
on the task's thread past the stream timeout starts no worker again and drops nothing: Rama fails
the maker's `:ack` append and runs the record a second time, which finds its answer: 38 of 38. The
line "Detected stalled streaming batch. Forcing failure and moving on to next batch.", which a
probe of 12 October got with a wait inside the event, did not come once. Computed with the gate's
rule over the records of 60 runs, and not run: if such a batch failed with the first attempt of a
later act behind it, the first module would hold the earlier act refused and the later accepted,
and a second run the other way.

**Meanwhile.** An event of the streaming gate holds no partitioner, no append and no wait, and its
source retries all after: [streaming_gate.clj](softland/store/module/streaming_gate.clj). The
module leaves `topology.stream.max.executing.per.task` at Rama's default; Sid is against that
setting (the [source entry](README.md), decisions of 13 October 2026, entry 7). What the store says
of this case is on the [store's page](softland/store/README.md), under the order of a layer's acts
after a failure. It is row V47 of the queue.

## 12. The order in which a microbatch runs the records of one partition

**Open.** Whether it is a promise that a microbatch topology runs the records of one depot
partition in the depot's order, each to its end before the next, when the code of a record holds no
partitioner and no yield. And whether that holds for a batch of any size.

**Rama's sources.** Rama's page on microbatch topologies has no line on the order in which the
records of a batch are run: the word "order" is not on it
(`reference/rama/docs/12-microbatch-topologies.md`, searched on 14 October 2026). What was read
speaks of how the order is lost: "Do NOT yield on a path where correctness depends on same-key
events processing in order" (`.claude/skills/rama/references/dataflow.md:156`), and
`yieldIfOvertime` yields when an event has used more than `worker.event.target.max.millis`,
"default 5ms" (`reference/rama/docs/09-intermediate-dataflow.md:209`). A batch reads 1,000 records
of a partition at most unless `depot.microbatch.max.records` is set
(`reference/rama/docs/12-microbatch-topologies.md:156`; the number was run).

**Seen.** On a stand-in gate (`run-again/probes-the-bases-gate/RESULTS.md`, C1 and C2). 3,000
records of one layer, each replacing the one before it on its key, were all accepted, each with the
number of its place in the depot, when the gate took them in 87 batches and when it took them in 3;
and 20,000 in 20 batches, and in one batch of 20,000. So a later record of a batch read the rows
that the earlier records of the same batch wrote. With `(yield-if-overtime)` at the head of the
walk and 200 microseconds burnt on each record, a record that yielded was decided after the records
behind it, and records were refused: 1,235 of 3,000 were accepted in one run. On the store's own
module, by hand: a chain of 200 acts in one batch was accepted with stamps rising in the depot's
order (`run-again/PART-B.md`, "The smoke run of part B").

**Meanwhile.** The microbatch gate rests on it: "What the gate rests on and does not test" in
[microbatch_gate.clj](softland/store/module/microbatch_gate.clj). The walk holds no partitioner, no
yield, no read that may yield and no append, as a contract of the operation: the namespace
docstring of [walk.clj](softland/store/module/walk.clj), "It never gives its task back". No check
looks for a yield at run time.

## 13. When the copies that a batch sends with `|all` are written

**Open.** Whether it is a rule that the rows which one task sends to every task with `|all`, inside
a microbatch, are written only after the sending task has run the last record of the batch: on the
other tasks, and on the sending task itself when it does not pass itself over. And why it is so.

**Rama's sources.** "Partitioners ensure ordering is maintained between any two tasks. If task A
sends events 1, 2, and 3 to task B across a partitioner, task B will receive and process those
events in exactly that order" (`reference/rama/docs/16-partitioners.md:129`). That is the order of
the copies among themselves, and not the moment at which they are written. "Microbatches process
sequentially, and the next one does not start until all data in the current microbatch finishes
processing and committing all changes to PStates" (`.claude/skills/rama/references/microbatch.md:30`).
The paper of the runs, of the copies that come back to the sending task: "No doc that was read says
it."

**Seen.** On a stand-in gate (`run-again/probes-the-bases-gate/RESULTS.md`, C2, C3 and C5). Every
other task wrote every copy in the order sent: not one out of order in 3,000, or in 20,000 in one
batch. In 16 runs in which the deciding task wrote its own copies too, every own copy of a batch
came back to it only after the batch's last decision, and not once between two decisions; such a
copy then set a key back to an older row for a time, in 15 of the 16. With a throw at the head of
the walk of the 401st record of a batch, after 300 ms on the thread, no copy had been written on
any other task: the same in three runs. Why the copies wait was not found out.

**Meanwhile.** The store is built so that it does not rest on this. The deciding task is passed
over, so no copy writes a row on the task where a decision reads: the docstring of
[microbatch_gate.clj](softland/store/module/microbatch_gate.clj), "The deciding task is passed
over". What the gate rests on is the order of what one task sends to another, which Rama's page
promises.

## 14. Another number of tasks for a module whose rows are placed by the layer's hash

**Open.** How a module goes to another number of tasks when its gate decides a layer on the task of
the layer's depot partition and its reads look for the layer by its hash. In Rama's manual way the
new module makes "new depots that are a copy of the old depots": whether that copy keeps the
records of one partitioning key in their order, so that a layer is one line again. On which task a
record of a mirrored partition is emitted when the two modules differ in their number of tasks,
as a rule and not only as seen. And whether a hop by the key at the head of the dataflow is a
supported way for a topology whose order matters.

**Rama's sources.** "Currently Rama does not support changing the number of tasks for a module"
(`reference/rama/docs/19-operating-rama.md`, "Task scaling", line 483). The manual way, from the
same section: "launch a new module with the same code and more tasks that recomputes all PStates
from scratch. This new module would set up a mirror to each depot and set up each topology to start
processing each depot from the beginning. The processing would also make new depots that are a copy
of the old depots." And: "This approach only works if your processing is deterministic". On a
mirror: "Rama assigns each task of the current module to zero or more partitions of each mirrored
depot or PState" (`reference/rama/docs/18-module-dependencies.md:108`). "The number of tasks
specified must be a power of two" (`reference/rama/docs/19-operating-rama.md:242`).

**Seen.** On a stand-in gate (`run-again/probes-the-bases-gate/RESULTS.md`, C7 and C8). Rama
refused to launch a module with 6 tasks and one with 3: "Invalid config: Num tasks must be power of
two". It let a module with 8 tasks, and one with 2, mirror and consume the depot of a module with
4. With 8, partition p was emitted on task p; with 2, partitions 0 and 1 on task 0 and partitions 2
and 3 on task 1. With the walk as it is, every task held the first module's rows and each layer was
decided in its depot order; but a layer's answers and its last number were on the task that got its
partition, which was not always the task of its name in that module: one layer was decided on task
3 of 8, and its name lands on task 7 of 8. With `(|hash *layer)` at the head of the dataflow they
were on the task of the name, and each layer kept its order; that hop was run only with a walk that
does not yield and does not fail. A fresh module with 8 tasks, and one with 2, that a client fed
the same records, each partition in its order, made the same rows. On the store's own module: the
run-again proof feeds the kept acts to a fresh store of 8 tasks and holds its tables against those
that a store of 4 gave (`test-store/softland/store/run_again_proof.clj`, its third check).

**Meanwhile.** Not built: the store has no way to another number of tasks for a store with real
data. The gate says what would go wrong with a mirror alone, under "What the gate rests on and does
not test" in [microbatch_gate.clj](softland/store/module/microbatch_gate.clj): a layer's answers
would be on a task where a read by the layer does not look. It is row V48 of the queue.
