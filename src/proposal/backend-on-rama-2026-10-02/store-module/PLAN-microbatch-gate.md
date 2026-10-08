# Plan

<!-- Phase 1 Step 5. Fill in after completing Steps 1-4. After completing this, fill in PLAN_VALIDATION.md. -->

Part: `microbatch-gate`, the second of the two parts of the store's module (`DECOMPOSITION.json`, second
entry). Stage: Phase 1 of the Rama skill, "Plan". This paper is a design. It holds no topology code.
Written by: a fresh session (Opus 5.5, effort max), 8 October 2026, for the build session "softland-11".
The first part, `streaming-gate-and-record`, is built and proven; this part builds on it and does not
build it again.

## How to read this paper

**Sources.** The same as the first part's plan, cited the same way: `C` is the contract
(`BUILD-store-module.md`; `C item 4`, `C rule 6`), `R` is the README (`src-store/README.md`;
`R 7-Oct 5`), `IS` is the implied spec (`IS O2`, `IS X38`), `P1` is the first part's plan, `V1` the check
of that plan. Where the contract and the README differ, the README wins. Rama's behaviour is cited from
the skill's references (`skill microbatch.md`) and from the local copy of Rama's docs (`docs 12:124` is
`reference/rama/docs/12-microbatch-topologies.md`, line 124). No code of the rig, the server or Inland
was read. The code of the first part was read: each file of `src-store/softland/store/`.

**Four kinds of statement**, as in the first part's plan: *given* (a source says it), *derived* (it
follows; the reason follows), **the plan's choice** (the sources are silent; the choice, its reason and
what the other answer changes; all listed again near the end), *not verified* (a behaviour of Rama read
in a document and not run; this stage ran no cluster and no code).

**Words of this paper.** Those of the first part: *place*, *store key*, *plain key*, *thing*, *stray*,
*read* (one RocksDB point read or the first seek of a range, 0.3 to 0.5 ms from a cold disk),
*iteration* (one more row of a range read). New here, each listed for Sid near the end:

- a *batch*: the things that one run of the microbatch gate takes from the microbatch depot. An
  *attempt*: one try of Rama to process a batch; Rama can make several for one batch.
- a *turn*: the number of a thing in its batch. The acts of a batch are decided in the order of their
  turns. This is "the one order" of IS O2.
- the *home task* of a row: the task on which the row lives.
- the *deciding task* of an act: the task that the hash of the entity of its first fact gives. It is
  the task on which the depot put the act.
- two acts of a batch *meet* when both need the same row and cannot be put in order on one task. The
  *meeting task* of a layer is the task that the hash of the layer id gives.
- the *gate's task*: the task that the hash of the class word `:microbatch` gives. The gate keeps its
  own last stamp there.
- a *question*: what an act sends to the home task of a row that it needs. A *reply*: what comes back.
  A *parcel*: rows that one task sends to another to write, or an act sent to the meeting task.
- a batch is *closed* when every task has made it visible and the next batch has begun. Rama cannot run
  a closed batch again.

## The design in short

| Kind | Name | What it is | Where it lives |
|---|---|---|---|
| Depot | `*microbatch-depot` | Every thing sent for a microbatch layer, as it was sent. Never trimmed. | Partitioned by the hash of the entity of the thing's first fact |
| Topology | `microbatch-gate` (microbatch) | Decides the things of a batch in four steps, each a batch block, and writes the record. | Each act is decided on its deciding task; acts that meet are decided on their layer's meeting task |
| PState | `$$microbatch-answers` | The answer under each act-id of a microbatch layer | Schema of the first part. A row lives on the task of the act-id's uuid |
| PState | `$$microbatch-facts` | The current fact of each place of each microbatch layer | Schema of the first part. A row lives on the task of its entity; a row under `:class` or `:permission` lives on every task |
| PState | `$$microbatch-locks` | Each lock of each microbatch layer, and the fact that it seals | Schema of the first part. A row lives on the task of its request's uuid; a lock that seals a value has the same row a second time, on the task of that value's entity |
| PState | `$$microbatch-task` | What the gate keeps for one task: its strays, the stamp through which its rows are closed, its count of turns. On the gate's task also the gate's last stamp | One value for each task |
| Query topology | `answer-by-act-id` (first part's, completed) | act-id → the recorded answer, or nothing | Streaming: the layer's task, 1 read. Microbatch: the uuid's task, 1 read while there is nothing, 2 when the answer is there |
| Query topology | `locks-of-request` (first part's, completed) | a request's act-id and a range → those locks | Streaming: as built. Microbatch: the request uuid's task, 3 reads and one iteration for each lock |
| Query topology | `class-of-layer` (first part's, completed) | layer → its class, or nothing | The layer's task. 1 read for a layer that starts streaming, 2 for the base |

No new query topology, no internal depot, no TaskGlobal. Two new namespaces: `softland.store.batch`,
pure, and `softland.store.microbatch-gate`, which requires Rama.

**One batch, in four steps.** Each step is one batch block of Rama; the next starts when the one before
is done on every task (docs 09:432, 09:456).

1. *Arrive and question.* On the depot partition's task, each thing in the depot's order: its turn; a
   stray is kept and ends; the door's declaration; the standing of the act, read from the task's own
   copy of the layer's permissions. Then the act sends a question to the home task of each row that it
   needs and that is on another task: its answer, its locks, the places of its facts on other
   entities.
2. *Reply.* The gate's task closes the batch before this one, reads the clock once and gives the batch
   its base stamp. Each home task reads each row that was asked for, once, and replies; the reply also
   says whether another act of the batch needs the same row.
3. *Decide.* On each deciding task, in the order of the turns: an act that meets no act of another
   task is decided here, with the first part's functions, and its rows on this task are written. Its
   rows for other tasks, and each act that meets, are sent as parcels.
4. *Write and meet.* Each task writes the rows that it was sent. The meeting task decides the acts
   that meet, one at a time in the order of their turns, and sends their rows.

**One accepted act with one sealed value on one entity costs 3 disk reads and 4 row writes**: the
permission on its deciding task, the answer on the uuid's task, the lock on the request's task; then the
fact, the lock beside its value, the lock at its home, the answer. 6 messages between tasks, 3 hops on
the longest path. An act with three values on three entities on three tasks costs the same 3 reads, 10
row writes, 12 messages, and the same 3 hops. The working is in "The checks of one act".

**Three things that this stage found in Rama's docs shape the design.** Each is under "What Rama gives a
microbatch".

1. A batch has no order between two depot partitions. The gate makes one: the turn.
2. A batch becomes visible task by task, not at one instant.
3. Rama can show one task's part of a batch and then run the batch again. So a read of this design
   gives a row only when its batch is closed.

## What Rama gives a microbatch, and what it does not

*From the docs and the skill's references. Not run.*

- **The same things on every attempt.** "Each attempt for the same microbatch ID processes the exact
  same set of data from the depots" and "each microbatch attempt starts with the PStates at exactly
  their state after the previous microbatch ID" (docs 12:116). Batches run one after the other (skill
  microbatch.md, "Sequential processing").
- **The depot's order on one task, and none between tasks.** The things of a batch are emitted "per
  task, in depot append order" (skill microbatch.md). Events from two tasks reach a third in any order;
  only the events of one sender keep their order (skill formal-model.md, "Partition ordering").
- **Whole or nothing across tasks.** All writes of an attempt on all tasks are one transaction (skill
  formal-model.md §6.2; docs 23:37).
- **Visible task by task.** "After all PStates on a task have been checkpointed and replicated, their
  external versions are updated … all changes to all PStates on a task in a microbatch become visible
  at the exact same time (though changes on different tasks may become visible at slightly different
  times)" (docs 12:124). The skill says the same: "external readers can observe two tasks on different
  microbatches at the same moment" (skill microbatch.md, "Guarantees").
- **A batch can run again after a task has shown its part.** The commit keeps two versions "ensuring
  the microbatch can restart with the state from the last microbatch ID if any worker involved in the
  commit phase were to fail" (docs 12:124). And: "When a new version of a PState is computed for the
  same microbatch ID, the new version replaces the old version in the external view" (docs 12:128).
  So a reader can see an answer on one task, and after a failure in the commit see another row in its
  place, if the second attempt did not write the same row.
- **The next batch starts only when every task has committed** (skill microbatch.md, "Guarantees"). An
  iteration that follows a batch with things starts at once; Rama sleeps only after an iteration that
  processed nothing (docs 12:159).
- **A stream topology's reads of this gate's PStates, and this gate's reads of the streaming gate's,
  see committed state only** (docs 23:19, 23:102).

**What follows for this design.**

- *The finding as the contract words it.* C item 4 says "Decisions of one batch are visible together
  (the finding)", and the finding is "a microbatch is visible only when the slowest task is done". The
  second sentence is what Rama does: nothing is visible before every task has finished its work. The
  first is true on each task and not across tasks: the tasks open within one commit, "at slightly
  different times". No design on Rama makes two tasks open at one instant.
- *IS X38 says "Nothing of the attempt is visible on any task."* Rama alone does not give that: a task
  can show its part of an attempt that then fails elsewhere. The stamps and a new lock's bytes differ
  from one attempt to the next (a clock, a random source). So the implied spec's lines "a stamp that a
  reader could have seen never changes" (IS C7) and "once a reader could have read a lock's bytes,
  those bytes never change" (IS O6) need a mechanism. This design's is **closing**: under "Topologies".
- *Rule 6's "the same winner on every attempt"* has its reason here. It is met by the turn, which does
  not depend on the attempt.

## Reads
<!-- For each read operation: access method, path expression, partition -->

The three reads of the first part, completed for the class microbatch. Each is still one roundtrip
from the caller to one task. The first argument of the first two changes its meaning: it is the
*routing id* of the act-id, the layer for a streaming act-id and the uuid for a microbatch act-id. For
a streaming act-id no caller changes.

**R1. The answer by act-id (IS O8; C item 8; C rules 7 and 11).**
- Access method: the query topology `answer-by-act-id`, `[routing-id act-id]`.
- Microbatch path: `$$microbatch-answers` at the key `"<layer> <uuid>"`, the whole row; then, only when
  a row is there, `:closed-through` of `$$microbatch-task` on the same task.
- Partition: the task that the hash of the act-id's uuid gives. "A microbatch act's answer is found
  from its act-id alone" (C rule 7): the caller takes the routing id from the act-id and needs nothing
  of the act.
- It gives the answer when the row's stamp is not later than `:closed-through`; else nil, "nothing
  yet". The form of the answer is the first part's.
- Latency: single-digit milliseconds for the read. Throughput: the door reads until an answer is there
  (IS O8); the count is under "What the plan found out".

**R2. The locks of a request (IS O9; C item 7).**
- Access method: the query topology `locks-of-request`, `[routing-id request-act-id from to]`.
- Microbatch path: `$$microbatch-answers` at the request's key; `:closed-through`; then the range of
  `$$microbatch-locks` from `"<layer> <uuid> <from>"` to `"<layer> <uuid> <to>"`.
- Partition: the task that the hash of the request's uuid gives. A request's answer and its locks are
  on that one task, whatever the request's facts, so they become visible at the same instant.
- It gives the locks when the request's answer is accepted and closed; else an empty map. The bytes of
  a lock are given in each state of the lock (IS O9, E4).

**R3. The class of a layer (IS O10; C rule 7).**
- Access method: the query topology `class-of-layer`, `[layer]`. Unchanged for a caller.
- Path: the record of the gate whose class the layer starts with (`first-act/class-at-making`). For
  the base: `$$microbatch-facts` at `"<layer> <layer> :class"`, then `:closed-through`. For any other
  layer: `$$streaming-facts` at the same key, as built.
- Partition: the layer's task. Every task holds the class row of a microbatch layer, so that task
  holds it.
- *Derived limit:* in this build no act changes a class (C item 3), so a layer is in the record of the
  gate of the class that it starts with, and the read looks there only. A read of the second record
  would never find a row; the skill counts such a read as wasted.

**R4. The strays of a task (C rule 10).** As the first part: one foreign select on `$$microbatch-task`,
path `[:strays (keypath reason) (view count)]`. A stray of this gate is kept on the task where the depot
put it, so an operator reads each task.

The reads that the gate itself makes for one act are in "The checks of one act".

## Writes
<!-- For each write operation: depot, event type -->

**W1. A thing for a microbatch layer (IS O2, and O3 to O7 as far as they go through it).**
- Depot: `*microbatch-depot`, appended by the door (in this build, the proof's hand). The ack level
  does not matter to the answer: an ack says that the depot holds the thing and nothing about the
  decision (skill microbatch.md, "Ack semantics"; R 7-Oct 6). The hand appends with `:append-ack`.
- Event type: one, the thing as it arrives, as in the first part. There is no dispatch by type.
- Latency: an answer is readable when the act's batch is closed, which is when the iteration after it
  has committed on the answer's task. Hundreds of milliseconds, up to a few seconds under load (IS O2).

No other write enters the module in this part.

## PState Design
<!-- For each PState:
If obvious: why this is the only reasonable design
If not obvious:
  Option A: schema — query cost: N seeks, M iterations because why
  Option B: schema — query cost: N seeks, M iterations because why
  Chosen: A or B because why -->

### What the record of a microbatch layer must serve

The row shapes are settled: "Same record shape" (C item 4), and the first part wrote the four schemas
once (`softland.store.record-pstates`) so that this gate declares them under its own names. What is
open is **on which task each row lives**. For a streaming layer the answer was "the layer's task" for
every row. For a microbatch layer three things pull apart:

- the depot puts an act on the task of one entity of its facts (C item 1; R 6-Oct 10);
- the answer is found from the act-id alone (C rule 7), and an act-id holds no entity;
- a lock is asked for ahead, before any value names it (C item 7), so its id holds no entity either.

So one act has up to three kinds of home task: its entities', its answer's, its locks'. The cost model
is the skill's: a read is a seek; a message between tasks is a hop; work done on every task for one
event is paid N times.

### Where an answer lives

- **A. On the layer's task**, as in a streaming layer. A read: 1 seek, routed by the layer. But every
  answer of the base is then written on one task and read from one task, and check (a) of every act
  passes through it. That is "all acts through one task" (C rule 6) for the write and for the read.
- **B. On the act's deciding task, with a pointer on the uuid's task.** The answer sits with the facts
  of an act on one entity. A read: 2 seeks and 1 hop, because the reader has only the act-id. A write:
  2 rows for each act. A second, different thing under the same act-id can arrive on another task
  (IS X35), so the uuid's task must still decide who has the act-id.
- **C. On the task of the act-id's uuid** (chosen). A read: 1 seek, routed straight by the uuid. One
  row for each act. The uuid is random, so answers spread evenly. Each act sends one question there
  and one row.
- **D. On every task.** N rows for each act. Not costed further.

**Chosen: C.** It is the one placement in which the read is 1 seek with no hop, nothing passes through
one task, and two things under one act-id meet in one place whatever their facts.

### Where a fact lives

*Derived, and close to given:* on the task of its entity (IS O2: "the facts of a microbatch layer are
on several tasks, by entity"). The depot puts an act on the task of the entity of its first fact, so
an act whose facts are all on one entity finds every place it reads and writes on the task where it
arrives. A fact on another entity is written on that entity's task. One entity with very many keys is
one task's load; that follows from decision 10 of 6 October as "a very large layer is one task's load"
follows for a streaming layer.

### Where the class and the permissions live

Every act of the layer needs the permission of its `:who`, on whatever task it is decided (IS O2).

- **A. A copy of each such row on every task** (chosen). Check (d): 1 seek on the act's own task, no
  hop. A give: N row writes, one on each task. The base's making: 2N row writes, once.
- **B. On the holder's task only; each act sends a question there.** Check (d): 1 seek and 2 more
  messages for every act, and the act's standing is not known before its other questions are sent. A
  give: 1 row write.
- **C. On the layer's task only.** As B, and every act of the layer asks one task.

| | Disk reads for check (d) | Messages for check (d), each act | Row writes for one give | Bytes for H holders, N tasks |
|---|---|---|---|---|
| A | 1, own task | 0 | N | N × H × about 150 |
| B | 1, holder's task | 2 | 1 | H × about 150 |
| C | 1, layer's task | 2, all to one task | 1 | H × about 150 |

**Chosen: A**, by the reason that Sid gave for F5 A: "a copy is bytes and a rare write; a fetch is a
cross-task hop on every act" (R 7-Oct 5). To give a permission is rare and to check one is as common
as acts are (IS O5). At 128 tasks and 1,000 holders the copies are about 19 MB in all. The contract's
F5 line leaves out "a gate reads on its own task"; the README has it, and the README wins.

**Why a copy is safe to read without any order.** A store key takes one write for a place (C rule 13),
no permission ends and no class changes in this build. So a permission row that is there never
changes. An act that finds its permission on its own task is decided the same whatever else the batch
holds. Only an *empty* place can change inside a batch, by a give or a making; an act that finds it
empty goes to the meeting task, where the gives and the makings of the batch are.

### Where a lock lives, and how it comes to sit beside its value

**Its home is the task of its request's uuid.** A lock's id is `[request's act-id, index]` (P1), so
this is the one task that can be found from the lock id alone, by the gate and by the door. It is also
the task of the request's answer (the same uuid), so a request's answer and its locks are on one task
and become visible at the same instant; and when the request's first fact is its `:lock-request`
fact, whose entity is that uuid (P1), the depot puts the request there too, and the request is decided
with no message to another task. "A second value under a lock that already seals one" (C item 7) is
decided at this home, also when two acts of one batch name the lock from two tasks.

**How a lock that was asked for ahead comes to sit beside its value** (C item 7: "the plan's"). The
value's home is its entity's task; the lock's home is its request's. Three ways:

1. *It stays at its home; the value's row names it.* 1 lock row for a value. A forget is one delete on
   the lock's task. The lock is not on the value's task: "co-located with their values" (C item 7) is
   not met.
2. *When the act is accepted, the lock's row is written a second time, on the value's task* (chosen).
   The row is the first part's `lock-row-sealing`: the bytes, the way, the fact that it seals; the same
   row under the same key on both tasks. The home row stays whole: it decides for the lock, and
   `locks-of-request` gives its bytes in each state (IS O9, E4) with no hop. 2 lock rows for a value
   (one, when the two tasks are the same task). A forget deletes both; the value's row names the
   lock, so both keys are known from it.
3. *The lock moves: the bytes are written on the value's task and taken out of the home row.* 1 copy
   of the bytes. But `locks-of-request` must then hop to the value's task for a lock that is used, and
   the home row needs one more field to say where. About the same bytes as way 2.

| | Lock rows for a value | Row writes at accept | `locks-of-request`, used lock | A forget (not built) | Row shape |
|---|---|---|---|---|---|
| 1 | 1 | 1 | 1 seek | 1 delete, on another task than the value | the first part's |
| 2 | 2 | 2 | 1 seek | 2 deletes, on two tasks, in one batch | the first part's |
| 3 | 2, one without bytes | 2 | 1 seek and 1 hop for each used lock | 1 delete, on the value's task | one more field |

**The plan's choice: way 2.** It is the only one that meets "co-located" and "same record shape" and
keeps the read of a lock as built. What it costs: one more row write for each accepted value, and
about twice the lock bytes for each value of a microbatch layer; F9's third limit is then missed there
for values under about 110 bytes (see "Resource usage analysis"). **Sid should know.** If he wants one
row, way 1 is the change: the accept leaves out one row; nothing else moves. H1 is open, and this row
follows whatever grain he gives.

### What a task keeps, and what the gate keeps

```
fixed-keys { :strays         { Keyword -> set of String, subindexed }   as the first part
             :closed-through Long          every row on this task with a stamp not later than this
                                           is of a closed batch
             :turns          fixed-keys { :batch String    the text of the microbatch id that the
                                          :given Long }    count belongs to; how many things of that
                                                           batch this task has given a turn
             :last-stamp     Long }        on the gate's task only: the last stamp that the gate gave
```

- `:closed-through` is what a read holds a row's stamp against. See "Closing".
- `:turns` numbers the things of a batch on the task where the depot put them. It is durable state
  because Rama sets a PState back to its value after the last batch when an attempt fails (docs
  12:116), so a second attempt counts from the same number; a count in the memory of a process would
  go on from where the failed attempt stopped, and the turns would differ between attempts. The batch
  id's text is beside the count, so the count starts again with each batch and nothing has to set it
  back.
- `:last-stamp`: one for the whole gate, not one for each task. **The plan's choice.** With one last
  stamp, each stamp that the gate gives is later than every stamp it gave before, on every task; that
  is rule 18 with no gathering of N last stamps for each batch. It sits on the gate's task, the task
  that the hash of the class word `:microbatch` gives. Why that task and not task 0: the greatest
  carried stamp of a batch has to be gathered to one task, and step 1 gathers everything with one hash
  partitioner; a value hashes to the gate's task, none is known to hash to task 0. A global PState with
  a `|global` aggregate was costed: one more batch block for every batch.

### Where the record's shape is the first part's, and where it differs

| | The same | Different, and why |
|---|---|---|
| Answers | schema, key text, the row's fields, `record/answer-row`, `answer-of` | The home task: the uuid's, not the layer's (C rule 7) |
| Facts | schema, key text, `record/fact-row`, `written-by?`, `holds-plain-value?` | The home task: the entity's (C item 1). A row under `:class` or `:permission` is on every task (R 7-Oct 5) |
| Locks | schema, key text, `lock-row`, `lock-row-sealing`, `lock-of`, `locks-by-index` | The home task: the request uuid's. A lock that seals a value has its row on the value's task too (C item 7) |
| Task | `:strays`, `:last-stamp` as fields | `:last-stamp` is one for the gate, on one task (C rule 18). Two fields more: `:closed-through`, `:turns` (new schema function `record-pstates/microbatch-task`) |
| Class in a key or row | none, as in the first part: the PState says the class | none |

No row names a task. A row's home task follows from the second part of its key: the uuid of an
answer, the entity of a fact, the request's uuid of a lock.

**Why the three maps are not one PState** (the check's question): they share no key, as in the first
part, and here they do not share a placement either.

## Depots
<!-- - *name: (hash-by :key), event types [...] -->

- `*microbatch-depot`: `(hash-by batch/first-entity)`. One kind of record: a thing, as it was sent (W1).

*Given*: two depots, one for each gate (F6 A, Q7 a); the microbatch depot partitions by one entity that
the depot derives from the act's facts; the builder's choice is the entity of the first fact (C item 1;
R 6-Oct 10).

- **What the depot derives.** `softland.store.batch/first-entity` takes anything. For a plain map whose
  `:facts` is a sequence whose first part is a plain map, it gives that fact's `:e`. For anything else
  it gives nil. It never throws, and it runs on the appending client (skill depot-design.md), so no
  thing can stop a partition through it (IS C1). It is pure and loads with Clojure alone, so a door can
  have it on its classpath.
- **Why the first fact's entity.** An act on one entity then arrives where its places are. A request
  for locks arrives where its locks and its answer will be. The order of two facts in an act is the
  maker's; the gate does not need it to be any order (see the base's making, which arrives on the task
  of `:base` or of `:system` and is decided on the meeting task in both cases; V1's point 5).
- **Order.** The acts on one entity as first entity are one partition, so they keep the depot's order
  (skill depot-design.md, "Same depot when events require local ordering"). Acts on two partitions have
  no order in the depot. See "The one order".
- **Never trimmed** (C rule 4): no trimming option is set. **Nothing secret goes on it** (C rule 5): a
  request holds a count; this part appends to no depot at all.
- **A thing on another partition.** A topology can append to any partition (docs 14:254). Unlike the
  streaming gate, this gate reaches every row by a partitioner: the act's entry is sent to the task
  that the hash of its first entity gives, and each question and each row goes by its own hash. So a
  thing that sits on another partition is decided against the true rows. Only its turn takes the
  partition where it sits.
- **The source** starts from the beginning, as the streaming gate's (P1).

There is no internal depot.

## Topologies and PStates
<!-- Each topology owns zero or more PStates. Only the owning topology can write to a PState. (The
template's rules on topology type and count were read and are answered below.) -->

### `microbatch-gate`: microbatch

**Why microbatch.** *Given*: C item 4 names it. It is also the default, and neither stream reason
applies: "a microbatch act never has" an ack that carries an answer (R 7-Oct 6), and a batch of delay
is what a microbatch layer is for (R 7-Oct 4). And "an act that touches several tasks is accepted whole
or refused whole" (C rule 6) is what a microbatch gives by its construction; a stream topology cannot,
because its writes on a task that it has left are not taken back (skill stream.md, "Retry semantics").

**Why a second topology.** The template says to add to the topologies a module has. What forces a
second: the contract names two gates, one a stream topology and one a microbatch topology (C items 3
and 4); a topology has one type; and a PState has one owner. The streaming gate is not changed into
anything else, and gets no new source.

**The source.** `*microbatch-depot`, from the beginning.

**The one order: the turn.** *Given*: "two acts that replace the same fact get the same winner on every
attempt" (C rule 6). *Derived* (IS O2): the decisions of a batch are those of deciding its acts one at
a time in one order, the same on every attempt.

`turn = position × tasks + partition`, where *position* is the number of the thing among the things
that this batch took from its depot partition, from 0, in the depot's order; *partition* is the number
of that partition; *tasks* is the module's number of tasks, which never changes (docs 19:483).

- *The same on every attempt.* Each attempt takes the same things from each partition (docs 12:116).
  The position is counted in `:turns`, which Rama sets back when an attempt fails. Nothing in a turn
  comes from a clock, from the order in which messages arrive, or from a process.
- *The depot's order on one partition is kept.* A sender's two acts with one first entity are decided
  in the order sent. That is the one order that a depot holds.
- *Between two partitions the rule is fixed and means nothing more:* the lower position first, then
  the lower partition. Two other rules were weighed. *By partition, then position:* every act of
  partition 0 wins over every act of partition 1 in every batch, and a stamp needs the counts of all
  partitions gathered first. *By the act's uuid:* it ignores the depot's order, so a sender's second
  act on an entity could be decided before its first. Neither was taken.
- *A turn is also the act's place among the stamps of its batch*: see "The stamp".

**The walk of one batch.** Four batch blocks. A step's rows are gathered on the task that needs them
into a temporary PState of Rama's, which lives in memory for one attempt (docs 10:476–554). The pure
functions are in `softland.store.batch`; the checks are the first part's, unchanged.

*Step 1. Arrive and question* (on the depot partition's task, each thing in the depot's order).

1. *The turn.* Read and write `:turns`; take the partition number.
2. *A stray?* `declaration/stray-reason :microbatch thing`. A stray: its digest is added to `:strays`
   under its word, on this task, and it ends. No answer, no stamp (C rule 10).
3. *Check (b).* `declaration/check`. A thing that it refuses needs one row only, its answer. Its
   digest is taken now, and it goes on as a small entry, with no facts, to the task of its act-id's
   uuid.
4. *Standing: checks (c) and (d).* Read the place of `:who`'s permission in this task's own
   `$$microbatch-facts`; only when it is empty, the layer's class place. `decision/standing` gives:
   - nil, with the act's permission in the row: the act stands;
   - `:who-not-the-permissions-holder`, or `:permission-does-not-stand` with another permission in the
     row: refused, and that is final. A row that is there does not change; and a permission whose
     holder is not the act's `:who` can never come to be in `:who`'s place, because a permission fact
     sits on its holder;
   - in every other case the permission's place is empty and the act could still come to stand, by a
     give or a making of this batch: it goes to the meeting task. With the class place empty too, the
     record holds nothing of the layer, so the act asks for no row but its answer.
5. *The questions.* For every thing that is no stray: one for its answer, to the uuid's task. For an
   act that stands or may come to stand: one for each place of a fact under a plain key or under
   `:lock-request`, to the entity's task, saying whether the row is wanted (the fact names something
   to replace) and whether the place is the act's *own* (its entity is the act's first entity, so its
   home is the act's deciding task); one for each request whose locks the act names, with the range
   that `decision/lock-ranges` gives and the keys named, to the request uuid's task; and for a request
   of its own, one that says which lock keys it will make. A fact under `:class` or `:permission` asks
   nothing: its row is on every task.
6. *The entry.* The thing, the act in its one form, its turn and what step 4 found, sent to the task
   that the hash of its first entity gives. That is this task for every thing that a client appended.
7. *The batch's numbers.* The greatest stamp that the thing carries, read with a guard, and its
   position, sent to the gate's task. A combiner keeps the greatest of each, so each task sends one
   pair (docs 10:219–221).

Everything of points 5 to 7 goes through one hash partitioner and is gathered by key into
`$$questions`.

*Step 2. Reply.*

- *On the gate's task:* read `:last-stamp`; read the clock, once for the batch; take the batch's
  numbers. `base` is `stamp/next-stamp` of the clock, the last stamp, and the greatest carried stamp,
  held to the greatest stamp that rule 17 lets pass. Write the new `:last-stamp`. Then go to every
  task with the clock, the base, and the old last stamp.
- *On every task:* write `:closed-through` with the old last stamp (see "Closing"). The gate's task
  holds its own `:closed-through` and compares first, so the tasks write only when the value has
  changed: once after each batch with things, and never in a quiet time. Then, for each key that was
  asked for on this task, once:
  - *an answer:* read `$$microbatch-answers`. A row there: each asker is told `:used`. No row: the
    asker with the least turn is told `:yours`, each other `:taken-in-this-batch`. This is check (a),
    and it needs no more: the first thing under an act-id uses it whether it is then accepted or
    refused (IS X105).
  - *a place:* it is `:shared` when more than one act of the batch names it, unless each of them has
    it as its own. The row is read, once, when an asker that does not have it as its own wants it.
  - *locks:* one range read for each asker and request. A lock is `:shared` when more than one act
    names it, or when a request of this batch will make it; that request is then told `:shared` too.
  The replies go to each asker's deciding task and are gathered there by turn into `$$replies`. The
  reply about the answer carries the clock and the base.

*Step 3. Decide* (on every task that holds entries).

`batch/who-meets` takes the task's entries and their replies and gives the turns that go to the
meeting task (the rule is below). Then, in the order of the turns, in a loop that gives the task back
between two acts:

- told `:used` or `:taken-in-this-batch`: nothing. No row is written; a repeat is answered from the
  record, and a different thing under a used act-id gets no answer of its own (C rules 1, 11).
- refused by the declaration or by a final word of step 1, point 4: its answer's row, as a parcel.
- an act that meets: a parcel to the meeting task with the act, its digest and the rows it has: the
  standing rows of step 4, the rows of the replies, and its own places read now.
- any other act is decided here: the digest; `decision/refused-early` with the batch's clock; the
  rows of `decision/places-needed`, its own places read from `$$microbatch-facts` now, so that it sees
  what an earlier act of this pass wrote, the others from the replies; `decision/places`;
  `decision/seals` with the lock rows of the replies; the stamp, `base + turn`; `decision/writes`.
  `batch/routed` gives each row its home. The rows of this task are written at once. The others are
  parcels.

The parcels go through one hash partitioner and are gathered into `$$parcels`.

*Step 4. Write and meet* (on every task).

- Write each row of a parcel: a whole row at a fixed key, no read. A new lock's bytes are made here, at
  the write, on the lock's home task (C rule 5).
- *On a meeting task:* the acts sent here, sorted by turn. `batch/decide-next` decides one act with the
  first part's functions and gives the rows it writes; the pass keeps those rows and lays them over
  the carried rows of each later act. For a fact under a store key that every task holds, the row is
  read from this task's own copy at the act's turn. For an act into a layer of which neither the
  record nor the pass holds anything, and whose layer starts with the other gate's class, the layer's
  class place is read in `$$streaming-facts` on this task: that is X41. The pass gives the task back
  between two acts. Then the rows are sent: a row under `:class` or `:permission` to every task, each
  other row to its home; and written.

**Who goes to the meeting task.** An act of a batch is decided on its layer's meeting task when:

1. it holds a fact under `:class` or `:permission`: it writes a row that every task holds (a give, a
   making act, and each act that is refused for such a fact);
2. the place of its `:who`'s permission is empty on its deciding task and the act could still come to
   stand (step 1, point 4): the layer may be made, or the permission given, by an earlier act of this
   batch (IS X37);
3. a reply says `:shared`: a place or a lock that it names is also named by an act that cannot be put
   in order with it on one task;
4. on its deciding task, an act with an earlier turn that goes to the meeting task names one of its
   own places.

Every other act is decided where it arrived. In ordinary work that is nearly every act: an act under a
permission that stands, on places and locks that no other act of the batch names from another task.

**Why the decisions are those of deciding one at a time in the order of the turns.** Take the acts
decided where they arrived, and the acts decided on the meeting task.

- A row that is not on an act's own deciding task and that the act reads or writes is named by no
  other act of the batch, or the reply said `:shared` and the act is on the meeting task (rule 3). So
  for such a row the row before the batch is the row at the act's turn.
- On one deciding task the acts are walked in turn order. An act decided there reads its own places
  at its turn and writes them at once, so it sees what each earlier act of that task wrote.
- An act that goes to the meeting task reads its own places at its turn too, and carries them. No
  later act of that task that names one of those places is decided there (rule 4), so the carried row
  stays true until the meeting task has decided.
- A held permission row and a class row never change (C rule 13), so a read of one is the same at any
  turn. An empty one is read on the meeting task only (rules 1 and 2).
- On the meeting task the acts are decided in turn order, each with the rows that earlier acts of the
  pass wrote laid over what it carried.
- The answer under an act-id needs no order with anything but the turn: the least turn has it.

So each act is decided against each row as every act with an earlier turn left it. X32 to X37 are that
sentence for one row each. Two acts that write one place and name nothing (X33) are both accepted, and
the later turn's row is the one that is written last: on one task by the pass, from the meeting task
because it sends one row for a key, the last.

**Accepted whole or refused whole** (C rule 6; IS X36). An act is decided once, on one task, with
every row it needs in hand; a refusal writes its answer and nothing else (C rule 15). The rows of an
accept go to several tasks inside one attempt, and an attempt's writes on all tasks are kept or
dropped together (docs 23:37). Nothing is decided "for a task's part" of an act, so V1's point 4, the
checks by stage and not by fact, needs no change of the first part's functions.

**The stamp** (C item 6; C rules 17 and 18; IS X22, E6).

- One reading of the clock for a batch, on the gate's task. Rule 17 is held against it for every act
  of the batch: `decision/refused-early`, the first check after the declaration, as in the first
  part.
- `base = next-stamp(clock, the gate's last stamp, the greatest stamp that the batch carries)`, the
  last of the three held to `pack(clock + 10 s, 65,535)`. An act that passes rule 17 carries no stamp
  above that bound, so the base is later than each stamp of each act that can be accepted. A forged
  stamp can put the stamps of one batch up to 10 seconds ahead and no further, which is the bound that
  rule 17 gives a task of the streaming gate.
- `stamp of an act = base + turn`. One stamp for an act, on its answer and on each of its facts (C
  rule 18). Turns are different for different things, so two acts never share a stamp, and the stamps
  of a batch go up in the order of the turns, on every task. "Of two accepted facts on one place, the
  current one holds the later stamp" (IS O2).
- The new last stamp is `base + (greatest position + 1) × tasks − 1`: not below any stamp of the
  batch. The next batch's base is later than it. So each stamp that the gate gives is later than each
  stamp it gave before, anywhere: "strictly increasing on its task" and "later than the last stamp of
  each task on which it writes" hold on every task with one number.
- *The plan's reading of rule 18:* "the last stamp of each task" is the last stamp that this gate gave
  there. The streaming gate's last stamp of the same task is of another record and is not read. The
  other reading would cost one read on each task that an act writes on.
- The turn sits in the counter's bits and can run over them: with 1,000 positions (Rama's default for
  one partition in one batch; skill testing.md) and 128 tasks, a stamp can be 128,000 above its base,
  2 ms in the time part. The next batch's base is the clock again when the clock has passed.
- Below 2^62 by the same reasons as the first part: the clock before the year 4199, and no carried
  stamp above the 10-second bound enters the base.
- A stamp is given to each decided thing, accepted or refused. A thing that is not decided (a repeat,
  a stray) gets none.

**Closing: what a read may give.**

*The fact* (under "What Rama gives"): a task can show its part of a batch and Rama can then run the
batch again. The second attempt reads the clock again and makes other lock bytes.

*The rule.* Each task holds `:closed-through`. A read gives a row of a microbatch layer only when the
row's stamp is not later than it. In step 2 of each iteration, every task writes `:closed-through`
with the gate's last stamp as it was before this iteration. That iteration started only after every
task committed the batch before (skill microbatch.md), so when a task shows the new
`:closed-through`, every row of every batch up to it is visible on every task, and no attempt of those
batches can run again.

- A row of the running batch has a stamp above the base, and the base is above the old last stamp. So
  it is given only after the next iteration.
- The next iteration starts at once after a batch with things, also when it has nothing to process
  (docs 12:159), so a batch is closed one short iteration after it, not at the next act.
- `:closed-through` is the same on every attempt of the iteration that writes it: it is read from the
  state after the last batch.
- The streaming gate holds the same rule when it asks whether this gate's record holds a layer (X31).
- A lock's row has no stamp. `locks-of-request` holds the stamp of the request's answer, which is on
  the same task. A lock's bytes do not change after that.

*Three ways were costed.*

| | Reads for one answer, by the door | An answer is readable | Work for each batch | The first part changes | What stays |
|---|---|---|---|---|---|
| Take Rama's commit as it is | 1 | when its task commits its batch | none | X31's read only | A stamp, a lock's bytes or a reason word can change after a reader saw it (IS C2, C7, O6, X38) |
| The same rows on every attempt: the batch's clock kept by the streaming gate, a new lock taken from what a reader could see | 1 | the same | 1 append to an internal depot, 1 stream event, 1 read; 1 more read for each request | a second source and one more PState in the streaming gate | The word of X41 can still change; it rests on reading this gate's own committed rows from inside it, *not verified* |
| Closing (chosen) | 1 while there is nothing, 2 when the answer is there | one iteration later | one 8-byte write on each task | X31's read only | Rule 17 can come out otherwise on a second attempt; no read shows it |

**The plan's choice: closing.** It is the only way of the three that keeps "nothing of the attempt is
visible" (IS X38) for whatever the gate reads or makes, now or later, and it changes nothing in the
streaming gate's event. What it costs is in the table.

*What closing does not give.* (1) Two tasks still do not open at one instant: each shows the new
`:closed-through` when it commits the next iteration. So for a few milliseconds one read can give an
answer while a read on another task still gives nothing, as before but one iteration later. The one
pair of the door's reads that sits on two tasks is the answer of the base's making act and the class
of the base. (2) A select that goes straight to a PState, round the three reads, sees rows of a batch
that is not closed. The proof looks at rows that way. The module's README says both.

**What a second attempt writes.**

| Row | After a failed attempt |
|---|---|
| Turn of each thing | the same |
| Which act has an act-id; which acts meet; the order | the same |
| Verdict and word of each act | the same, but for rule 17 at the edge of its 10 seconds, and X41's word if the streaming gate made that layer in between |
| Stamp | another, from another reading of the clock |
| A new lock's bytes | other bytes |
| `:closed-through` | the same |
| A stray's digest | the same |

No row of the first column is given by a read before the batch is closed, and a closed batch has one
attempt that counts.

**Many senders, a sender that restarts, a worker that restarts.**

- *Two senders.* Their acts are in one batch or in two. In two: the later batch reads the record as
  the earlier left it. In one: the turn. Acts of two layers never meet: every key holds its layer.
- *A sender that restarts or is in doubt.* It reads by act-id; with no answer it sends the same thing
  again (IS O7). The same thing has the same first entity, so it arrives on the same partition; it is
  told `:used`, or `:taken-in-this-batch` when both are in one batch.
- *A worker that stops, a task whose leader changes, an attempt that fails.* The attempt is dropped
  on every task and made again from the state after the last batch (docs 12:116). The temporary
  PStates are cleared (docs 12:120). Nothing that a decision needs is in the memory of a process.
- *A thing that throws.* None can: each function that touches a thing is the first part's or is
  written to the same rule, and "a record that throws deterministically retries forever" (skill
  microbatch.md) is why (IS C1).
- *Scale.* Each durable collection is a top-level map on disk or a subindexed set. The temporary
  PStates hold one batch.

**Yielding.** Steps 3 and 4 walk a task's acts in a loop and give the task back between two acts
(`yield-if-overtime`), so a read is not held for a whole batch (IS X40). That is safe here and was not
in the streaming gate: what can run on the task in between is a read of committed rows, an event of
the streaming gate, or the gathering of this step's parcels; none writes a row that the pass reads.

**The processing concerns, and whether each belongs here.** The decision of the acts of a batch; the
making of locks, which must be in the same attempt as the request's answer; the sign of a stray,
which is found by the first test of each thing; the closing of the batch before. All are of one batch
and one latency class. None is split off.

**PStates** (no `Object` anywhere; the first three are the first part's schema functions):

- `$$microbatch-answers`: `{String -> fixed-keys{:refused Keyword, :stamp Long, :digest bytes}}`
- `$$microbatch-facts`: `{String -> fixed-keys{:value bytes, :lock-act UUID, :lock-index Long, :by-act UUID, :by-index Long, :stamp Long}}`
- `$$microbatch-locks`: `{String -> fixed-keys{:bytes bytes, :way Long, :seals-act UUID, :seals-index Long}}`
- `$$microbatch-task`: `fixed-keys{:strays {Keyword -> set of String, subindexed}, :closed-through Long, :turns fixed-keys{:batch String, :given Long}, :last-stamp Long}`

Rows are made only by the functions of `softland.store.record`, as in the first part.

## Query Topologies
<!-- - name:
  Input example 1: describe input → N total reads, M meaningful (non-empty) reads
  Input example 2: different input → N total reads, M meaningful (non-empty) reads
  Fixed or variable: fixed if M is same for all inputs, variable if M differs
  If variable, dynamic approach: ops/explode + aggregator | loop<- | etc. -->

No new query topology. The first part's three get the branch for the class microbatch that it left
empty (P1 "For the second part", 6). Each still begins with a hash partitioner on its first argument,
which the client evaluates, so the query goes straight to one task and never hops (skill
query-topologies.md, "Leading partitioner"). For the first two that argument is now the routing id:
the layer for a streaming act-id, the uuid for a microbatch act-id. `softland.store.batch/routing-id`
gives it from an act-id; the hand calls it. The head of each query tests what it was given before any
key is built, as in the first part: a thing that is no act-id, a routing id that is not the one of the
act-id's class, or an id over the limit costs no read. None aggregates. Where the number of reads
differs between inputs, the later read is behind a test of the earlier one, so no read is issued that
cannot change the result.

- **`answer-by-act-id`** `[routing-id act-id] → answer or nil`
  - Example 1: `:alice`, `[:alice :streaming u1]`, decided → 1 read, 1 meaningful. As built.
  - Example 2: `u2`, `[:base :microbatch u2]`, decided, its batch closed → 2 reads: the answer's row,
    then `:closed-through`. 2 meaningful.
  - Example 3: the same shape, not decided or never arrived → 1 read; the empty row is the output
    ("nothing yet"). The second read is not issued.
  - Example 4: the same shape, decided, its batch not yet closed → 2 reads, nil. The second read is
    what says "not yet".
  - Example 5: no act-id; `:base` given as routing id for a microbatch act-id; an id over 256
    characters → 0 reads, nil.
  - Variable for the class microbatch: 1 or 2. The second read is inside an `<<if` on the row.

- **`locks-of-request`** `[routing-id request-act-id from to] → {index {:bytes … :way …}}`
  - Example 1: a streaming request, as built → 1 read and one iteration for each lock.
  - Example 2: `u3`, `[:base :microbatch u3]`, a request for 16 locks, accepted and closed, range 0 to
    15 → 3 reads (the request's answer, `:closed-through`, the range) and 16 iterations. All meaningful.
  - Example 3: the same, not decided → 1 read, an empty map. Refused → 1 read, an empty map: a refused
    request made no lock.
  - Example 4: accepted, not yet closed → 2 reads, an empty map (IS X97).
  - Example 5: arguments of the wrong shape → 0 reads, an empty map.
  - Variable: 1, 2 or 3 seeks, each behind a test of the one before; the range is cut to 0–255 as built.

- **`class-of-layer`** `[layer] → class or nil`
  - Example 1: a layer that starts streaming, made → 1 read. Not made → 1 read, nil. As built.
  - Example 2: `:base`, made and closed → 2 reads: the class row in `$$microbatch-facts`, then
    `:closed-through`. Not made → 1 read, nil. Made, not yet closed → 2 reads, nil (IS X117).
  - Example 3: no id → 0 reads, nil.
  - Variable for the base: 1 or 2. Which record is read is told by `first-act/class-at-making`,
    before any read.

A lock's bytes leave the module through `locks-of-request`, to any caller, as rule 16 says. The
microbatch PStates are not declared private, as the first part's are not: a foreign select of them is
a second way to a row, and it is not held to `:closed-through`.

## Partitioning efficiency

**The placement that the reads want**, and the partitioner of each.

| Row | `f(key) → task(s)` | Partitioner | Why |
|---|---|---|---|
| A thing on the depot; an act's entry | the task of the entity of its first fact | `hash-by` on the depot; the hash partitioner in step 1 | An act on one entity is decided where its places are |
| An answer | the task of the act-id's uuid | the hash partitioner on the uuid | Found from the act-id alone, 1 seek, no hop; spread evenly |
| A fact under a plain key or `:lock-request` | the task of its entity | the hash partitioner on the entity | C item 1; the keys of one entity stay together |
| A fact under `:class` or `:permission` | every task | `|all` | Small, written rarely, read by every act on its own task (R 7-Oct 5) |
| A lock | the task of its request's uuid; and, once it seals a value, the task of that value's entity too | the hash partitioner on each | Found from the lock id alone; beside its value (C item 7) |
| The gate's last stamp; a batch's numbers | the gate's task | the hash partitioner on `:microbatch` | One value for the gate; read and written once for a batch |
| `:closed-through` | every task | `|all` | 8 bytes, once for a batch |
| An act that meets | its layer's meeting task | the hash partitioner on the layer | The acts of one layer that must be put in one order |

**The keyspaces.** Answers and locks hash a random uuid: large and even. Facts hash an entity: large;
one entity with very many acts is one task's load, by decision 10 of 6 October. Two partitioners hash
a single value, and that is what they are for: the gate's task takes one read and one write for a
batch, whatever the number of acts; a layer's meeting task takes only the acts that meet. Neither is on
the path of an act that meets no other.

**The dominant work: the gate's own reads for one act**, once for every act (as in P1). Seeks are
totals across all tasks. The proportions are this stage's assumption for the base; they are not
measured.

### N = 1 task (single-task baseline)
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| One sealed value on one entity, nothing replaced | 0.60 | 3 | 1 |
| One sealed value that replaces a fact | 0.20 | 4 | 1 |
| Three sealed values on three entities, locks of one request | 0.05 | 3 | 3 |
| A request for 16 locks | 0.05 | 2 | 0 |
| A retract that names the fact it replaces | 0.04 | 3 | 0 |
| A give, or the making of a layer | 0.01 | 3 | 0 |
| A repeat of an act of the first kind | 0.02 | 3 | 1 |
| A thing that the declaration refuses | 0.02 | 1 | 0 |
| A stray | 0.01 | 2 | 0 |

Weighted seeks = 3.10   |   Weighted iterator reads = 0.97

### N = 16 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| One sealed value on one entity, nothing replaced | 0.60 | 3 | 1 |
| One sealed value that replaces a fact | 0.20 | 4 | 1 |
| Three sealed values on three entities, locks of one request | 0.05 | 3 | 3 |
| A request for 16 locks | 0.05 | 2 | 0 |
| A retract that names the fact it replaces | 0.04 | 3 | 0 |
| A give, or the making of a layer | 0.01 | 3 | 0 |
| A repeat of an act of the first kind | 0.02 | 3 | 1 |
| A thing that the declaration refuses | 0.02 | 1 | 0 |
| A stray | 0.01 | 2 | 0 |

Weighted seeks = 3.10   |   Weighted iterator reads = 0.97

### N = 128 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| One sealed value on one entity, nothing replaced | 0.60 | 3 | 1 |
| One sealed value that replaces a fact | 0.20 | 4 | 1 |
| Three sealed values on three entities, locks of one request | 0.05 | 3 | 3 |
| A request for 16 locks | 0.05 | 2 | 0 |
| A retract that names the fact it replaces | 0.04 | 3 | 0 |
| A give, or the making of a layer | 0.01 | 3 | 0 |
| A repeat of an act of the first kind | 0.02 | 3 | 1 |
| A thing that the declaration refuses | 0.02 | 1 | 0 |
| A stray | 0.01 | 2 | 0 |

Weighted seeks = 3.10   |   Weighted iterator reads = 0.97

Flat from 1 task to 128. Each read of the gate goes to the one task that its key hashes to, and no
read goes to every task. The same holds for the door's reads: R1 is 1 or 2 seeks on one task, R2 up to
3 on one task, R3 1 or 2 on one task, at any number of tasks.

**What does grow with the number of tasks, said plainly.** Three writes go to every task: a give (N
rows), the making of a layer (2N rows, once), and `:closed-through` (N writes of 8 bytes for each batch
that has things). And each iteration sends three `|all` events. They are writes and events, not reads,
and none is for each act. The alternative for the first two is costed under "Where the class and the
permissions live". For the third, a mark on each task that holds rows of the batch would save the
write on the other tasks at the price of one more write on each task that has rows; at 128 tasks and
two batches a second the plain form is 256 small writes a second. Not taken; it is the first thing to
change if a measure shows it.

## Design Decisions
<!-- - Subindexing: which PState collections are subindexed and why
- Colocation: how depot partitioning aligns with PState keys -->

### Subindexing

As the first part: the only inner collections in a durable row are the sets of strays, subindexed,
with size tracking; the outer map of five reason words is not. `:turns` is a value of two fields. No
row holds a collection.

### Colocation

The depot's partitioner and the home of a fact are one hash, of an entity. So for an act on one
entity the depot partition, the entry, every place and the lock beside its value are one task. The
request for locks is colocated with its locks and its answer by the form that the first part gave it.
Whether Rama's `hash-by` and its hash partitioner give one task for one value is not said in one
sentence of the docs; the first part rests on it for the layer and its proof runs. If they differed
here, nothing would be decided wrongly: an entry, a question and a row each go by the hash
partitioner. An entry would move one task, at the cost of one message with the act's bytes.

### The pure namespace `softland.store.batch`

Everything of this gate that is not Rama, as in the first part: the dataflow only reads rows, calls
pure functions and writes rows. By role:

- what the depot derives from a thing; the routing id of an act-id;
- the turn of a thing, and the stamp of a turn in a batch; the base of a batch;
- the questions of an act; the replies of a home task to the questions about one key;
- which acts of a task meet;
- the decision of the next act of a pass, with the rows that earlier acts of the pass wrote laid over
  the rows it was given; it calls `softland.store.decision` and adds no check;
- the home of each row that a decision writes.

None reads a PState, a clock or a random source, and none throws on what arrived. Given the things of
a batch in any order of arrival, they give the same decisions; the pure proof shows it by shuffling.

**"The same checks" (C item 4) are the same functions.** `declaration/stray-reason` and `check`;
`decision/refused-early`, `standing`, `makes-the-layer?`, `places-needed`, `places`, `lock-ranges`,
`seals`, `writes`; `first-act/making-act?` and `class-at-making`; `digest/digest`; `stamp/next-stamp`,
`pack`; `seal/opens?`, `new-lock`; each function of `softland.store.record`. Each takes the gate's
class, and this gate gives `:microbatch`. No function of `decision.clj`, `declaration.clj`,
`first_act.clj`, `stamp.clj`, `seal.clj`, `digest.clj`, `text.clj` or `act.clj` changes. Of what
`decision/writes` gives, the gate leaves out one thing, the task's last stamp: this gate has one for a
batch.

**The order of the reasons (IS C4)** is the first part's, because the functions are called in the same
order on one act with all its rows: the declaration, rule 17, standing, the places, the store-key
step, the seal. One difference is in cost and not in the word: the rows of the places and of the locks
are asked for together in step 1, so an act that is then refused for a place has paid for its lock
rows. An act with no standing is refused before it asks (step 1, point 4), so it cannot make the gate
read 256 places.

### The forms of Rama that the steps use

Each is in the skill's references or the docs; none was run by this stage.

- Several batch blocks in one source block, each done on every task before the next (docs 09:456;
  skill microbatch.md, "`<<batch` blocks").
- A PState write in the first part of a batch block (skill batch.md, "Constraints").
- An aggregation, after one hash partitioner, into a temporary PState that later blocks read by a
  path (docs 10:476–554), with a combiner of this design's own (skill aggregators.md, "Custom
  combiner").
- `|all` at the head of a batch block (docs 10:494); a partitioner inside a branch (skill
  dataflow.md).
- The task's number, the module's number of tasks, the microbatch id (skill syntax.md; skill
  pstate-schema.md, "Partitioning control"; skill microbatch.md).
- A loop over a task's acts that gives the task back (skill dataflow.md, "Yielding").
- A read of another topology's PState on the same task (docs 23:102).

## State primitive selection
<!-- For every piece of state in the design, name the storage class and justify -->

- `$$microbatch-answers` (PState): the source of truth for a decision. One row for a decided thing.
  Durable, never deleted (IS C2).
- `$$microbatch-facts` (PState): the current facts, the class, the permissions. For one accepted act:
  at most 256 rows; a row under `:class` or `:permission` N times. Durable.
- `$$microbatch-locks` (PState): the only home of a lock's bytes. For one act: at most 256 new rows,
  or 256 rows written again, each of those twice. Durable.
- `$$microbatch-task` (PState): the strays; `:closed-through`, written on each task once for a batch;
  `:turns`, written for each thing and read from the batch's own buffer after the first; the gate's
  last stamp on one task. Durable: a stamp must increase through a restart, and a turn must be the
  same on a second attempt.
- `$$questions`, `$$replies`, `$$parcels` (temporary PStates of Rama, in memory, cleared between
  microbatches; docs 12:120): what one step of an attempt hands to the next. An attempt builds them
  again from the batch's things and the PStates; nothing in them outlives the attempt. They are not a
  copy kept to save a read: each row in them was read once, on its home task, for this batch, and is
  carried to the task that decides.
- Not a TaskGlobal: the count of turns, for the reason under `:turns`; the gate's last stamp, as in
  the first part.
- Not state: the clock reading of a batch; the random source of a process.
- External: none.

The rows that one thing can cause are bounded by the limits of rule 20 and by the number of tasks:
256 fact rows, 512 lock rows written again, 256 new lock rows, one answer; N for a permission, 2N for
a making.

## Resource usage analysis

Estimates from the sizes of the parts, as in the first part. Nothing was measured.

### Disk usage (PStates)

| Where | One entry | Grows by |
|---|---|---|
| `*microbatch-depot` | the thing as sent, as the first part's: about 450 bytes around its facts, plus the sealed bytes | one for each thing sent, for ever |
| `$$microbatch-answers` | key about 43, row about 70 accepted, about 100 refused | one for each decided thing, for ever |
| `$$microbatch-facts` | key 25 to 110, row about 112 plus the value's bytes; a row under `:class` or `:permission`, about 150, on each of N tasks | one for each new place; N for each permission |
| `$$microbatch-locks` | key about 47, row about 50 before it seals, about 93 after; a second row of about 93 on the value's task | one for each lock asked for, and one more for each lock that seals a value |
| `$$microbatch-task` | about 60 bytes for a task; about 70 for each different stray | strays only |

**The third limit of F9** (C rule 21): "the locks take no more than 2 times the bytes of the values".
In a microbatch layer a lock that seals a value has two rows: about 280 bytes as written, about 204
where RocksDB shares the front of neighbouring keys (*not verified*, as in the first part). "2 times"
then holds when the sealed values average about 102 to 140 bytes or more, against 55 for a streaming
layer. The lever is the second row (way 1 under "Where a lock lives" takes it away) and, under it, the
grain H1.

### Memory usage

No TaskGlobal. The temporary PStates hold one batch on each task: the entries, which hold the things
that Rama already has in memory for the batch; about 100 bytes for a question; for a reply about a
lock, the lock's row; for a parcel, the rows for another task, with the sealed bytes of a fact that
lives there. With acts of about 600 bytes and Rama's default of 1,000 things for a partition in a
batch, well under 2 MB for a task. The bound is not this design's: the largest act that rule 20 lets
pass is 16.8 MB, and Rama holds a batch's records in memory while it processes them (docs 12:156). So
`depot.microbatch.max.records` for this depot is a setting for whoever runs the cluster, to be chosen
with the size of the acts that the base takes. The plan sets nothing; the receipt names it.

### The length of a batch

- For each iteration, whatever it holds: four waits for all tasks; three `|all` events for each task;
  on the gate's task one read, one write and one reading of the clock; one small write on each task
  for the closing. How long Rama's wait for all tasks takes was not found as a number (docs
  12:163–165 name the settings).
- For each act: what "The checks of one act" counts.
- One pass gives the task back between two acts, not inside one. The largest act holds its task as
  long as in the first part (0.15 to 0.5 s; P1 "The length of one event").

### Minimization

- The second row of a lock that seals a value: the price of "co-located"; way 1 is the smaller form.
- The copies of permissions on every task: the price of a read on the act's own task.
- `:closed-through` on every task for each batch: see "Partitioning efficiency".
- A lock's bytes cross from the lock's task to the task that opens the value, inside a reply, and for
  a value on another entity from the deciding task to that entity's task, inside a parcel. They are
  in Rama's messages between tasks of the module and in no depot (C rule 5). The other way, sending
  the sealed value to the lock, moves up to 65,536 bytes where this moves 32, and makes a second place
  where a part of an act is judged.

## The checks of one act: what each reads, on which task, and what it costs

A *hop* is one message from one task to another. E is the act's deciding task, U the task of its
act-id's uuid, R the task of the uuid of the request whose locks it names, L its layer's meeting
task, G the gate's task. "One entity" is an act with one sealed value on one entity. "Three" is an act
with three sealed values on three entities on three tasks, the locks of one request.

| Check (C items 3, 7, 9; C rules 16–18) | State it reads | On which task | Hops | Disk reads: one entity | Disk reads: three entities |
|---|---|---|---|---|---|
| The turn | `$$microbatch-task`, `:turns` | where the depot put the thing | 0 | from disk once for a task and a batch; then from the batch's own buffer | the same |
| A stray, before (a) (rule 10) | nothing. (A stray then writes its digest: 2 reads with the set's size tracking) | where the depot put it | 0 | 0 | 0 |
| (a) the record holds an answer under this act-id | `$$microbatch-answers`, the act-id's key | U | 2: a question and a reply | 1 | 1 |
| (b) the door's declaration | nothing | E | 0 | 0 | 0 |
| Rule 17 | the clock, read once for the batch on G; it comes with the reply about the answer | E | 0 more | 0 | 0 |
| (c) the layer's class is this gate's | read off (d)'s row. When that place is empty: the layer's class place, the copy on E. When that is empty too and the layer starts with the other class: `$$streaming-facts`, the layer's class place | E; the last on L | 0; 1 to L for an act whose permission place is empty | 0 (or 1, or 2) | the same |
| (d) the act's permission stands | `$$microbatch-facts`, the place of `:who`'s permission, the copy on E | E | 0 | 1 | 1 |
| The first act of the base (item 9) | the two rows of (d) and (c), both empty; the form is tested on the act alone. The base starts with this gate's class, so the other record is not read | E, then decided on L | 1 to L | 2 | — |
| (e) each `:replaces` is the current fact | `$$microbatch-facts`, the place of each fact that names something to replace | the fact's entity's task: E at the act's turn, or another task in step 2 | 0 for a place on E; for another task the question and the reply, 2, which the act sends in any case | 0, or 1 | 0 to 3 |
| (f) no setting key on another entity | nothing | E | 0 | 0 | 0 |
| (g), with rule 13's "a place that holds one" | `$$microbatch-facts`, the place of each fact under `:permission`, the copy on L | L | 1 to L: an act with such a fact is decided there | 1 for each such fact | the same |
| What a store key does not take | rows already read | where the act is decided | 0 | 0 | 0 |
| The seal (item 7; rule 16) | `$$microbatch-locks`, one range for each request named | R; the rows come to E in the reply, and the value is opened on E | 2: a question and a reply | 1, and 1 iteration | 1, and 3 iterations |
| The stamp (item 6; rules 17, 18) | `:last-stamp`, once for a batch; the batch's greatest carried stamp, gathered in step 1 | G | 0 for an act: the base comes with the reply about the answer | 0 (1 read and 1 write for a batch) | 0 |

**One accepted act with one sealed value on one entity, nothing replaced.**

| Step | On | Disk reads | Row writes | Messages to another task |
|---|---|---|---|---|
| 1 | E | the permission of `:who`: 1 | — | a question to U, a question to R |
| 2 | U, R | the answer's key: 1 (not there). The lock: 1, and 1 iteration | — | a reply from each to E |
| 3 | E | 0 | the fact; the lock's row beside it | the answer's row to U; the lock's row to R |
| 4 | U, R | 0 | the answer; the lock's row at its home | — |

**3 disk reads, 4 row writes, 6 messages, 3 hops on the longest path** (E to U, U to E, E to U). If
the value replaces a fact: 4 reads. The streaming gate's count for the same act is 4 reads, 4 writes
and no message: this gate reads no last stamp for an act, writes one lock row more, and pays the
messages because the answer and the lock are not where the fact is.

**One accepted act with three sealed values on three entities on three tasks.**

| Step | On | Disk reads | Row writes | Messages to another task |
|---|---|---|---|---|
| 1 | E | the permission: 1 | — | questions to U, to R, and to the tasks of the second and the third entity: 4 |
| 2 | U, R, the two tasks | the answer's key: 1. The locks: 1, and 3 iterations. The two places: 0, when nothing is replaced | — | 4 replies to E |
| 3 | E | 0 | the first fact and its lock's row | each other fact with its lock's row, to its entity's task: 2. The three lock rows to R: 1. The answer to U: 1 |
| 4 | the two tasks, R, U | 0 | 2 and 2; 3; 1 | — |

**3 disk reads and 3 iterations, 10 row writes, 12 messages, 3 hops on the longest path.** With each
fact replacing one: 6 reads. If the act meets another: one message more, the act to L, and the rows go
out from L; 4 hops on the longest path.

**Other acts.** A request for n locks whose first fact is its `:lock-request`: 2 reads (permission,
answer), n + 2 row writes, and no message to another task: its deciding task is the task of its
answer and of its locks. A give: 3 reads (the giver's permission, the answer, the new holder's place
on L), N + 1 row writes. The base's making: 3 reads, 2N + 1 row writes, once. A repeat: the reads of
its first run and no write; it has sent its questions before it is told `:used`. So "a repeat costs
less than a decision" (IS O7) holds for the writes, the digest and the opening, and not for the reads.
A thing that the declaration refuses: 1 read, 1 write. An act refused for its standing: 2 or 3 reads,
1 write, and no question but the one for its answer.

## How each thing that belongs to the layer as a whole is reached from each task

| Thing | Where it is | How a gate reaches it, on whatever task it decides | How the door reaches it |
|---|---|---|---|
| The class | A row on every task | 1 read on its own task, and only when the permission's place is empty | `class-of-layer`, on the layer's task, which holds a copy like every task |
| The permissions | A row for each, on every task | 1 read on its own task, for every act | no read of this build |
| The locks | On the task of the request's uuid; a lock that seals a value also on the value's task | A question in step 1 and a reply in step 2, which brings the rows with the bytes. 2 hops, 1 read and one iteration for each lock | `locks-of-request`, routed by the request's uuid |
| The answers | On the task of the act-id's uuid | A question in step 1 and a reply in step 2. 2 hops, 1 read | `answer-by-act-id`, routed by the uuid |

"A gate reads on its own task" (R 7-Oct 5) holds for the class and the permissions, which are what
Sid's reason fits: a copy is bytes and a rare write. It cannot hold for a lock or an answer: there is
one for each value and one for each act, so a copy on every task would be N rows for each. Each is
reached by one question. The questions of an act go out together in one step and their replies come
back together in the next, so an act with three homes waits no longer than an act with one.

Why each hop is there: the answer's, because the answer is found from the act-id alone (C rule 7) and
the depot places the act by an entity (C item 1); the lock's, because a lock is asked for before its
value is known (C item 7); a fact's on another entity, because a fact lives with its entity.

## The path of a lock in a microbatch layer

1. **Asked for ahead.** An act with one fact `{:e u :k :lock-request :v n}`, `u` the uuid of its own
   act-id: the first part's form. The depot puts it on the task of `u`.
2. **Made.** The request is decided like any act. On accept, step 4 writes n rows on the task of `u`,
   each with fresh bytes made at the write, beside the request's fact and its answer, which are on
   that task too. A second attempt of the batch makes other bytes; no read gave the first ones,
   because the batch was not closed (C rule 5; IS X94).
3. **Read.** When the request's batch is closed, `locks-of-request` gives its locks in one call.
4. **Used by the door**, as in the first part.
5. **Checked.** The act's question to the task of `u` asks for the range of its locks. The reply
   brings the rows. The task that decides the act opens each sealed value once with
   `decision/seals` and keeps nothing of the plain bytes (C rule 16). The words are the first part's.
6. **Sitting beside its value.** On accept the lock's row, now saying which fact it seals, is written
   on the task of `u` and on the task of the value's entity, beside the fact's row, which names the
   lock.
7. **Later.** A fact that replaces the value writes over the value's row; both rows of the lock stay
   (IS E4). A forget, not built, deletes both.

**Two acts of one batch under one lock.** The lock's home says `:shared`; both acts go to the meeting
task; the earlier turn seals it and the later is refused `:lock-already-seals-a-value`. If the earlier
is refused for another reason, the lock is free and the later can seal with it (C rule 15).

**A request and a use of its lock in one batch** (IS X37; "only a forger sends the last"). The home
says `:shared` to both. On the meeting task the request, if it has the earlier turn and is accepted,
leaves the lock as made in this pass; its bytes are not made until the write. No sender can have
sealed with them. The pass gives `decision/seals` a row with bytes drawn for this test alone, so the
use is refused `:lock-does-not-open-the-value` by the unchanged function. With the request later or
refused: `:lock-not-made-for-this-layer`.

## Builder's rule 7 in both directions, and what changes in the first part

**How a gate tells that a layer belongs to the other gate.** First by its own record: a row at the
place of a permission or of the class says that the layer is this gate's (P1). When its own record
holds nothing of the layer, by one read of the other gate's record on the layer's task: the layer's
class place. That read is made only for a layer that starts with the other gate's class
(`first-act/class-at-making`); for any other layer the other record cannot hold a row in this build,
because no act changes a class.

**X31: a thing for the base on the streaming depot**, act-id `[:base :streaming u]`. It is no stray:
its class slot is the streaming gate's. Its event runs on the task of `:base`. The permission's place
and the class place in `$$streaming-facts` are empty. The base starts with the class microbatch, so
the gate reads the class place of `:base` in `$$microbatch-facts` on its own task, which holds it like
every task, and `:closed-through` of `$$microbatch-task`. A row that is there and closed:
`decision/standing` gives `:layer-belongs-to-the-other-gate`, recorded under the streaming act-id.
Before the base is made: `:layer-not-made`. Cost: 1 or 2 reads more, only for an act into the base on
the wrong depot. The event still has no partitioner.

**X41: a thing for a streaming layer on the microbatch depot**, act-id `[:alice :microbatch u]`. It is
no stray. In step 1 both places are empty on its deciding task, so it goes to the meeting task, the
task of `:alice`, and asks for its answer only. There, `:alice` starts streaming, so the pass reads the
class place of `:alice` in `$$streaming-facts`: 1 read on the task where that row lives. There:
`:layer-belongs-to-the-other-gate`. Not there: `:layer-not-made`. The answer is written under the
microbatch act-id on the uuid's task, where a reader with that act-id looks (IS E1).

**X107: one act-id on both depots.** Each gate answers only under an act-id of its own class
(`stray-reason`, unchanged), so an act-id has one answer.

**Every line of the first part that changes.**

| File | Lines | What changes |
|---|---|---|
| `streaming_gate.clj` | 93–101 | The `:require` gains `softland.store.first-act` |
| `streaming_gate.clj` | 103–111 | `other-gates-record-holds-layer?` takes the other gate's class row and the task's `:closed-through`, and gives true for a row that is there and closed. It gave false. Its docstring |
| `streaming_gate.clj` | 176 | The test gains a third part: the layer starts with the other gate's class |
| `streaming_gate.clj` | 177 | One call becomes three lines: the read of the class place in `$$microbatch-facts`, the read of `:closed-through`, the call |
| `streaming_gate.clj` | 4–92 | The namespace docstring says which two PStates the gate borrows to read |
| `door_reads.clj` | 59–71, 73–88, 90–98 | Each query: the first argument's name and test; the branch for the class microbatch with its reads; for the class, the record chosen by the starting class. The docstring, 4–49 |
| `record.clj` | 217–245 | `answer-read` and `lock-range-read` take the routing id and hold it to the act-id by its class. One function added, `closed?`. The docstring's list |
| `record_pstates.clj` | none changed | One function added, `microbatch-task`, and its paragraph in the docstring |
| `module.clj` | 46–53 | The second depot and the call that adds the microbatch gate. The docstring, 4–45 |

Not changed: `act.clj`, `text.clj`, `digest.clj`, `declaration.clj`, `stamp.clj`, `seal.clj`,
`first_act.clj`, `decision.clj`. In the streaming gate nothing changes but the four places above: its
PStates, its source, its rows, its words and the walk of one event are as built. The folder's README
and the two proofs are extended.

## The builder's rules, one line each

For this part. Rules 2 to 4, 12 to 16 and 20 are met by the first part's functions, which this gate
calls unchanged; the line says so only where the batch adds something.

1. The answer's row keeps the digest, as built. A different thing under a used act-id is told `:used`
   or `:taken-in-this-batch` and writes nothing.
2. A fact that names nothing asks for no row and is written as the row of its place. In one batch the
   later turn's row is the one that stays (IS X33).
3. As built.
4. No trimming option on `*microbatch-depot`.
5. A lock's bytes are made in step 4 on the lock's home task and written to `$$microbatch-locks` only.
   This part appends to no depot. A repeat of a request is told `:used`. A second attempt's bytes
   replace bytes that no read gave.
6. The turn; the meeting task; one decision for an act on one task; one attempt's writes kept or
   dropped together. Under "Topologies".
7. Under the section above. A microbatch answer is found by `answer-by-act-id` with the uuid of the
   act-id as routing id.
8. Each new word is under "Words a session made". No new reason word and no new word of a stray.
9. Under "What the plan found out".
10. `stray-reason :microbatch`, before any question; the digest goes to the set of its word on the task
    where the depot put the thing.
11. As built; the microbatch branch of the read gives the digest too.
12, 13, 14. The declaration's rules and `decision/places`, unchanged. A permission fact sits on its
    holder, and its row is on every task.
15. A refusal writes the answer and nothing else. A lock named by a refused act of the same batch is
    free for a later turn.
16. `decision/seals` on the task that decides the act. The lock's row comes there in a reply.
17. One clock reading for a batch; `decision/refused-early`.
18. One stamp, `base + turn`, on the answer and each fact; one last stamp for the gate.
19. Nothing is built for a rebuild. Under "What the plan found out".
20. The declaration's limits, unchanged. They bound the questions of one act: 256 places, 256 ranges
    of locks.
21. Under "What the plan found out".

## What the plan found out

### C rule 9: does a rebuild from the microbatch depot pick the same winner?

**No, not when the two acts are on two depot partitions.** What this stage found, from the docs:

- *Inside one partition, yes.* The depot holds the order, each batch takes the things in that order
  (skill microbatch.md), and a failed attempt is made again with the same things (docs 12:116). The
  window that the streaming gate has between a failed batch and its retry (P1, IS X29) does not exist
  here.
- *Between two partitions the depot holds no order.* A record is "identified by a partition index and
  an offset" (docs 14:234); nothing relates the offsets of two partitions. The order in which this
  gate decided two such acts is: the batch of each, then the turn. Which batch a thing fell into is a
  fact of timing: what had accumulated when Rama cut the batch, up to
  `depot.microbatch.max.records`, a setting that can change at any time (docs 12:122, 12:156). Rama
  writes the range of each batch for its own use and gives no call to read it back.
- *So a rebuild that reads the depot from its start cuts other batches.* Two acts that were in two
  batches can come into one, or change places: an act at offset 100 of a quiet partition and an act at
  offset 5,000 of a busy one, the second sent first. Then the other act has the earlier turn. This
  matters only for acts that meet from two partitions (IS X42): both name one lock, both make the
  base, both carry one act-id, or one names a place on the other's entity.
- *The number of tasks.* A turn holds the partition's number, and a thing's partition follows from the
  number of tasks. A rebuild into more tasks changes both.

**What would make it so.** The record of each batch's cut, where a rebuild can read it: for each batch
and each partition, how many things the batch took, which is the last value of `:turns`. With the
cuts, a rebuild takes the same things in each batch and gives the same turns. With the clock of each
batch beside the cut, rule 17 comes out the same and so do the stamps, since the base is a function of
the clock, the last stamp and the batch. That is a second log beside the acts depot. Sid's reason for
F2 A is against a second depot "of decisions"; a log of cuts holds no decision, and whether it is
wanted is his. The other way is one depot partition for a layer, which is the streaming form, against
decision 10 of 6 October and rule 6. Nothing is built for either.

### C rule 19: what else a rebuild of this gate's record would meet

- *Locks:* kept as they are, both rows of a lock that seals a value; never made again. The second row
  cannot be placed from the row alone (it does not hold the entity); it is placed from the fact's
  row, which names the lock.
- *Stamps:* new, unless the cuts and the clocks are kept (above).
- *Rule 17:* as the first part found, a refusal for a stamp too far ahead can come out otherwise at a
  later clock.
- *Moving the rows as they are* (docs 19:489) fits this record too: a row's home follows from the
  second part of its key. The rows under `:class` and `:permission` go to every new task;
  `:closed-through` and the gate's last stamp are taken as the greatest.
- *What F2 A reaches here*, for the receipt: the decisions and the current facts of the acts of one
  depot partition. Between two partitions, only with the cuts.

### C rule 21: what this gate aims at

1. *1,000 acts each second on one task.* For this gate: 1,000 acts a second arriving on one depot
   partition, every one decided. On the deciding task an act costs 1 disk read and about 0.1 ms of
   computing; its two other reads fall on the tasks of two random uuids. Rama's default takes 1,000
   things from a partition for a batch, so the aim needs an iteration, four steps, inside a second.
   *Not measured.*
2. *100 acts each second for one person, the slowest 1 in 100 at 20 ms.* Not this gate's: a
   microbatch has at least 300 ms (skill phase-1-plan.md), and F4 C puts what a person does directly
   in streaming layers. **The plan aims at an answer that is readable within two iterations of the
   append**: its own batch and the short one that closes it. *Not measured.*
3. *The locks take no more than 2 times the bytes of the values.* Not met in a microbatch layer for
   values under about 110 bytes, because of the lock's second row. Under "Resource usage analysis".

### IS O8: how many reads one microbatch act costs the door

There is no ack to wait for. The door reads until an answer is there. A read while there is nothing
costs 1 disk read; the read that gives the answer costs 2. With a first read 300 ms after the append
and one each 150 ms after it, 2 to 4 roundtrips at light load. The steps are the door's to choose; the
hand reads in a loop with a time limit.

### Open item ?15

?15 is "Task 0: all microbatch decisions, and some streaming layers". In this design no act is
decided on task 0. Rama's runner is there, and each batch block begins there with one event. The
gate's own numbers are on the gate's task: one read and one write for a batch. The acts that meet are
decided on their layer's task. What is left of ?15 is smaller and can now be measured: a streaming
layer that shares a task with the base's meeting task shares it with the acts of the base that meet,
and a streaming layer on any task shares it with the base's acts on the entities of that task.

## Where the contract and the README differ

The README wins. For this part:

1. **"A gate reads on its own task."** The contract's F5 line leaves the clause out; the README has it
   with Sid's reason (R 7-Oct 5). The plan follows the README: the class and the permissions of a
   microbatch layer are on every task.
2. **H1's other answer.** The contract speaks of "one lock per act"; the README of "one for each
   session and layer". The lock's rows leave room for both, as in the first part.
3. **E4 A and the first act.** In the base there is no exception: the system makes the base and acts
   under its own permission, `[:system :base :base]`.

One difference is between the contract and Rama's docs, not between the two files: "Decisions of one
batch are visible together (the finding)" (C item 4) against "changes on different tasks may become
visible at slightly different times" (docs 12:124). It is under "What Rama gives a microbatch" and in
the list of what could not be met.

## Words a session made, Sid's to change

Every word that this part makes. Each says what the thing is. None comes from the rig's code, which
this stage did not read. The module's README carries the list, after the first part's (C rule 8).

Not made here: every word of the first part's list. "Batch" and "attempt" are Rama's words
("microbatch", "microbatch attempt"). "The one order" is the implied spec's. `:microbatch` is Sid's.
**No new reason word and no new word of a stray**: every refusal of this gate is recorded with a word
of the first part.

**Names in Rama.**

| Word | What it is |
|---|---|
| `*microbatch-depot` | The microbatch depot: every thing sent for a microbatch layer |
| `microbatch-gate` | The microbatch topology that decides the things of the microbatch depot |
| `$$microbatch-answers` | The answer under each act-id of a microbatch layer |
| `$$microbatch-facts` | The current fact of each place of each microbatch layer |
| `$$microbatch-locks` | Each lock of each microbatch layer, and the fact that it seals |
| `$$microbatch-task` | What the microbatch gate keeps for one task |
| `$$questions`, `$$replies`, `$$parcels` | The three temporary PStates of one attempt: what step 1 gathered, what step 2 replied, what step 3 sent |

**Namespaces**, each `softland.store.` and the word.

| Word | What it is |
|---|---|
| `batch` | The pure code of a batch: turns, questions, replies, which acts meet, the next decision of a pass, the home of a row |
| `microbatch-gate` | The microbatch topology |

**Fields of a task's value.**

| Word | What it is |
|---|---|
| `:closed-through` | The stamp through which this task's rows are of closed batches. A read gives a row only when its stamp is not later |
| `:turns`, with `:batch` and `:given` | The batch that the count belongs to, as the text of its microbatch id, and how many things of it this task has given a turn |
| `:last-stamp` | The first part's field. In this gate there is one, for the whole gate, on the gate's task |

**Words of a batch**, in the explanations and in the data between the pure functions and the gate.

| Word | What it is |
|---|---|
| turn | The number of a thing in its batch. The acts of a batch are decided in the order of their turns |
| base | The first stamp of a batch. An act's stamp is the base plus its turn |
| deciding task | The task that the hash of the entity of an act's first fact gives; an act that meets no other is decided there |
| home task | The task on which a row lives |
| the gate's task | The task that the hash of the class word `:microbatch` gives; the gate's last stamp is there |
| to meet; meeting task | Two acts of a batch meet when both need one row and cannot be put in order on one task. The meeting task of a layer is the task of the layer id; acts that meet are decided there, in turn |
| entry | What step 1 keeps of a thing for step 3: its turn, the thing, the act in its one form, what was found of its standing |
| question, with `:answer`, `:place`, `:locks`, `:new-locks` | What an act sends to the home task of a row that it needs; the four kinds |
| `:own` | In a question about a place: the place is on the act's own deciding task |
| reply, with `:yours`, `:used`, `:taken-in-this-batch`, `:shared` | What a home task sends back: for an answer, that the act-id is the asker's to use, is used in the record, or goes to an earlier turn of this batch; for a place or a lock, that another act of the batch needs it too |
| parcel | Rows that one task sends to another to write, or an act sent to the meeting task |
| closed | A batch is closed when every task has made it visible and the next batch has begun |
| routing id | The first argument of `answer-by-act-id` and `locks-of-request`: the layer of a streaming act-id, the uuid of a microbatch act-id |

## What the proof adds for this part

It extends `test-store/softland/store/module_proof.clj`, on the same in-process cluster (several
tasks, more than one worker), in the same style: each line that the contract asks for on a `SHOWN`
line in the contract's words. The hand for a microbatch layer: it takes the class (for the base
before its making, the class that it starts with); makes the act-id `[layer :microbatch uuid]`; asks
for locks and seals as before; appends to `*microbatch-depot`; and reads `answer-by-act-id` with the
routing id in a loop with a time limit, as a door would, since no ack carries an answer. To put two
things into one batch it uses the test tool that pauses the microbatch topology, appends, and lets it
go on (skill testing.md). The design does not rest on that tool.

**The contract's two lines that the first part could not show:**

- the base made by the system, with the class microbatch (the answer, and `class-of-layer`);
- one act landing through the microbatch gate and read back by act-id, with the digest of what was
  sent.

**For this part's rules:**

- *X31, both ways.* An act for the base on the streaming depot: `:layer-not-made` before the base is
  made, `:layer-belongs-to-the-other-gate` after. An act for a streaming layer on the microbatch depot:
  `:layer-belongs-to-the-other-gate`; for a layer that is not made: `:layer-not-made`. One act-id sent
  on both depots: one answer.
- *Two acts of one batch that replace the same fact*, arriving on two tasks. One is accepted, the
  other refused `:replaced-fact-not-current`; the place holds the winner's fact; the winner's stamp is
  the lower. The same two, given to the pure functions in both orders of arrival: the same winner.
- *An act that touches several tasks, accepted whole:* three sealed values on three entities whose
  rows are on more than one task; each fact's row is there, each lock's row is beside its value and
  at its home, and all hold the answer's stamp. *One refused whole:* the same shape with the third
  fact stale; the answer is the refusal, none of the three places changed, and the three locks are
  still free.
- *A person acting in the base under a permission that the system gave:* the system gives
  `[person :base :base [:system :base :base]]`; the person's act under it is accepted; the
  permission's row is on more than one task.
- *The one stamp of rule 18:* the three rows of the act above hold one stamp, later than the stamp of
  each row written before on those tasks; the stamps of this gate's answers go up in the order
  decided.
- *Every check of the first part still passes*, unchanged.

**Beyond those, for what this part carries:** a repeat on the microbatch depot answered with the first
stamp; a different thing under a used act-id (the digests differ); one act-id twice in one batch, on
two tasks (one answer); a stray on the microbatch depot, kept once and given no answer; an act into
the base before it is made; two making acts of the base in one batch (one accepted); a give and an act
under it in one batch, in both orders on one entity; the path of a lock in the base: asked for, read
with one call, used, its two rows, a second value under it refused, two acts of one batch under one
lock; an answer that is given is of a closed batch (its stamp is not above `:closed-through` on its
task); after the module is started again, the base's answers, class, permissions and locks are as they
were. In `pure_proof.clj`: each function of `softland.store.batch`, with one case for each rule of
"who goes to the meeting task", and the decisions of a small batch compared with deciding its acts one
at a time, for every order of arrival.

**What this proof does not reach:** a second attempt of a batch; a commit that fails after a task
showed its part; the moment between two tasks' commits; a rebuild; a pass long enough to give the task
back; the three targets of speed and size.

## The plan's choices, and what could not be met as written

**The plan's choices**, each with its reason where it is made. None is a decision of Sid's.

1. An answer lives on the task of its act-id's uuid.
2. The class and the permissions of a microbatch layer are on every task (the README's reason for F5 A,
   read onto them).
3. A lock lives on the task of its request's uuid, and a lock that seals a value has the same row a
   second time beside the value. **Sid should know**: two rows for a lock in a microbatch layer. The
   other answer leaves one row out of the accept.
4. The one order: the position in the depot partition's part of the batch, then the partition's
   number. Between two partitions it is a fixed rule with no other meaning.
5. An act is decided on the task of its first entity. Acts that meet are decided on their layer's
   task. The four rules of who meets.
6. One clock reading for a batch; one last stamp for the gate; an act's stamp is the base plus its
   turn; rule 18 read as "the last stamp that this gate gave".
7. Closing: a read gives a row only when its stamp is not above the task's `:closed-through`.
8. The first argument of `answer-by-act-id` and `locks-of-request` is the routing id.
9. `class-of-layer`, and each gate's read of the other gate's record, go by the class that the layer
   starts with.
10. The gate's own last stamp is on the task that the hash of `:microbatch` gives, not on task 0.
11. Four batch blocks for a batch; three temporary PStates.
12. The count of turns is in a PState, with the batch id's text beside it.
13. A lock's bytes go to the task that opens the value; the sealed value does not go to the lock.
14. The hand appends to the microbatch depot with `:append-ack` and reads the answer in a loop.
15. A new pure namespace, `softland.store.batch`; no function of the first part's pure namespaces
    changes.
16. The words of the list above.

**What could not be met as written, and why.**

1. *"Decisions of one batch are visible together"* (C item 4), and the implied spec's line from it,
   "no reader sees the accepted answer of a microbatch act while a fact of that act on another task is
   not yet visible" (IS O2; also O8, O10). On one task: met exactly. Across tasks: Rama opens the
   tasks "at slightly different times" (docs 12:124), and no design changes that. With closing, the
   three reads give nothing of a batch before every task has made it visible; then each task opens
   when it commits the next iteration. The one pair of the door's reads on two tasks is the answer of
   the base's making and the class of the base.
2. *"Nothing of the attempt is visible on any task"* (IS X38), *"a stamp that a reader could have seen
   never changes"* (IS C7), and the like for a lock's bytes (IS O6). Met for the three reads, by
   closing. Not met for a select that goes straight to a microbatch PState. Declaring those PStates
   private would close it (the first part found that a query of the module still reads a private
   PState); the first part left its PStates readable and the proof looks into them, so this part
   does the same and says so. It belongs with open item ?11.
3. *"The same winner on every attempt"* (C rule 6), *"gives each act the same answer"* (IS X38). The
   order is the same on every attempt, and with it every check that reads the batch and the record.
   Two things can differ on a second attempt: rule 17, which reads the clock, for an act that carries
   a stamp at the edge of the 10 seconds; and the word of X41, if the streaming gate made that layer
   in between. No read gives a row of the first attempt. What would make both the same: the clock of
   a batch, and what the streaming gate said of a layer, kept for the batch by the streaming gate,
   since Rama sets this gate's own PStates back. That is one internal depot, a second source and one
   PState in the streaming gate. Not built; the build session's to rule.
4. *F9's third limit* in a microbatch layer, for values under about 110 bytes: the lock's second row.
5. *"A repeat costs less than a decision"* (IS O7): in writes and computing, yes; its reads are those
   of its first run.
6. *C rule 9:* found, and the answer is no. Nothing is built for it.
7. *"Either gate can tell which gate a layer belongs to"* (C rule 7) is met by a read that is made
   only for a layer that starts with the other gate's class. That is all of them while no act changes
   a class; a re-class would add the read for every layer.
8. *Not verified:* everything that this paper says of Rama is from its docs and the skill's
   references. The list for the build to check first follows.

**For the build to check first**, before anything rests on it:

1. An iteration that takes no thing from the depot commits what it wrote, and the iteration after a
   batch with things starts without Rama's sleep. The closing of the last batch before a quiet time
   rests on both. If either does not hold: the gate appends one mark for each batch with things to an
   internal depot that it reads itself, so that the next iteration has a record.
2. The things that a task emits from a batch run one after the other, each as far as the first
   partitioner, in the depot's order. The turn rests on it.
3. A PState read and write in the first part of a batch block that ends in an aggregation; an
   aggregation with a combiner of this design's own into a temporary PState; a read of that PState by
   key.
4. `hash-by` on the depot and the hash partitioner in a topology give one task for one keyword and
   for one UUID. If not, entries move one task; nothing is decided wrongly.
5. The microbatch id has the same text on each attempt of one batch.
6. The streaming gate reads a PState that the microbatch gate owns, on its own task.
7. Rama's default for the things of one partition in one batch, which this paper takes as 1,000.

## Design difficulty log
<!-- An honest, first-person record of where this design was hard to settle. -->

1. **Where the deciding happens.** This was the hard one. I began with the plain form: gather every
   row of every act to the layer's task and decide there one at a time, which is the streaming gate in
   a batch. It is correct and it is what rule 6 says not to do. Deciding each act where it arrives
   breaks as soon as two acts need one row: the earlier act's outcome can depend on a row on a third
   task, so "who is first at this row" does not settle "who wins". I looked for a rule that needs no
   meeting place, such as "the earlier turn takes the row", and each one I built could refuse an act
   for a row that the other act never wrote. So the acts that meet go to one task and all others stay.
   What I am least sure of is rule 4 of "who goes to the meeting task": it is the one place where an
   act is pulled along by another, and the argument that the carried row stays true took me three
   tries.
2. **How many steps.** Four waits for all tasks in every batch is a lot. I could not get below it
   without losing something: the homes must hear every question before they reply, the deciding tasks
   must have every reply before they decide, and the meeting task must have every act that meets
   before it starts. I did not find the cost of one wait in the docs.
3. **"Visible together", and a batch that runs again.** I found late that Rama can show one task's
   part of a batch and then make another attempt (docs 12:128). My first answer was to make every
   attempt write the same rows: keep the batch's clock in the streaming gate, and take a new lock's
   bytes from what a reader could already see. It worked on paper, changed the streaming gate, and
   still left one word that could differ. Closing came second and I took it: it does not care what
   differs between attempts. It was a real contest. Closing costs every answer one more iteration and
   one more read, and it leans on an iteration with no records committing its writes, which I could
   not confirm.
4. **The lock beside its value.** Three ways, none clean. One row and not beside the value; two rows;
   or a row that moves and a read that follows it. I chose two rows because the contract says
   "co-located" and the read of a lock stays as built. It doubles the lock bytes in the base, against
   a named target. I do not know which Sid would take.
5. **The stamp.** One stamp for an act that writes on three tasks, later than everything on each of
   them, with no gathering of last stamps. One last stamp for the whole gate did it. The carried
   stamps were harder: the base must be later than every carried stamp of the batch, and a forged
   stamp must not drag it. Holding the gathered greatest to rule 17's own bound settled it; before
   that I had a second pass over the batch.
6. **The turn.** I wanted the position of a thing in its partition without state, and found no
   documented way to get it. A count in a PState, set back by Rama on a failed attempt, is the one
   form that I could show to be the same on every attempt.
7. **A repeat.** In the streaming gate a repeat costs one read. Here it has sent its questions before
   it learns that it is one. A step only for the answers would fix it and would cost every act two
   more waits. I left it.
8. **Rule 17 on a second attempt.** It reads the clock, and a second attempt has a later clock. I
   could not make it the same without state outside this gate. I say so in place of a mechanism.
9. **Task 0.** The plain home for the gate's own number is a global PState on task 0. I put it on the
   task of the class word instead, because step 1 gathers with one hash partitioner and nothing is
   known to hash to task 0. It reads as odd; it saves a batch block.
10. **What I did not run.** Everything about Rama here is read, not run. The points that the design
    leans on hardest are in the list for the build to check first.
