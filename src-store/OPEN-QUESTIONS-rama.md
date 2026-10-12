# The store — open questions about Rama

Things that the store rests on and does not know: how Rama behaves in ten places. They are kept here
as the store's own open questions; nothing was sent to Rama's team (Sid, 12 October 2026). This is
not the queue of what Sid has not decided. That queue is
`src/proposal/backend-on-rama-2026-10-02/decisions/OPEN-QUESTIONS.md`, and its answers are in the
decisions of the [source entry](README.md).

Each entry says what is still open, what Rama's public sources already say, what the store's runs
saw, and what the store does meanwhile and where. When a question closes, its answer goes into the
docstring or the decision that it changes, and the entry leaves this page.

**What the store saw.** Every run was on Rama 1.6.0, on the in-process cluster, on one machine;
nothing here was seen on a real cluster. The working is in
`src/proposal/backend-on-rama-2026-10-02/reads/`, written `reads/` below. Never open a file under
its `runs/` whose name begins `raw-`.

**What was searched, on 12 October 2026.** Rama's pages (the snapshot of 22 March 2026 in
`reference/rama/docs/`, and the live site, which is at Rama 1.9.0), its Javadoc and Clojure API, its
release notes from 1.0.0 to 1.9.0, the mailing list rama-user from August 2023 to September 2026, Red
Planet Labs' public code, and their Rama skill as this repository holds it (`.claude/skills/rama/`,
of 5 August 2026). Of Rama's Slack only a public mirror was reached, and it holds 27 May to
28 September 2026. The Slack before that, and Rama's Discord, were not searched. The captures are in
`/mnt/data/projects/research/softland/rama-questions-2026-10-12/`.

Rama's licence, `META-INF/RAMA-LICENSE.txt` in its jar, forbids reverse engineering, decompiling and
disassembling it. Nothing below comes from inside the jar.

| | Question | Now |
|---|---|---|
| 1 | A watched row that holds a byte array is sent whole | open |
| 2 | Which task a partitioning key goes to | open |
| 3 | `genHashingIndexKeys` outside a test | open, but for a select |
| 4 | `pstate.proxy.limit` | open, but that it can be set |
| 5 | The order of String keys with rare letters | open |
| 6 | The threads of Rama's client | answered for callbacks |
| 7 | A path that Rama does not serve | open for the stopped worker |
| 8 | An append with `:ack` inside a microbatch | open for the microbatch case |
| 9 | The id of a microbatch | open for a restore and a new declaration |
| 10 | Records handed again after a restart | answered, but for two defaults |

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
[tables.clj](softland/store/record/tables.clj). The rows of the answers and of the locks keep their
byte arrays, and nothing watches them.

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
many pushes". The 900 is kept and not decided.

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
(`reads/PLAN_VALIDATION-what-the-gates-keep.md`, point 6).

**Meanwhile.** Check (a) of the streaming gate and the clock keeper's "first write wins" make a
record that comes again harmless.
