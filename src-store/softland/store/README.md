# Store — the act, the store's module, the door, and the read path

[Parent: namespace root](../README.md) · [Store source entry](../../README.md).

This folder holds four things. The act: the one form of what a maker gives to
the store, and the check of that form. The store's Rama module, `Store`: the two
depots of acts, the gate of each, the record that each gate keeps, and the two
reads by which the door reads the module. The door: the client code that takes a
maker's act into a depot and reads its answer back. And the read path: the
client code by which a fact comes back out to a caller, once or kept live. An
act that a gate accepts becomes facts in the record; its answer is read back by
act-id, and its facts through the read path.

A layer has a depot, streaming or microbatch: the depot that takes its acts,
whose gate decides them. A layer is one partition of its depot, so its acts are
one line, in the depot's order. Both gates decide a thing with one walk, on the
layer's task. The streaming gate decides each thing in one event. The microbatch
gate decides each thing of a batch in the depot's order, and copies the rows of
an accepted act's facts to every task. The base is the one layer that starts as
a microbatch layer; every other layer starts streaming.

A gate decides from the thing, which holds the time that the door wrote on it,
and from its own rows. No gate reads a clock or a table of the other gate. So a
second module that reads the two depots from their beginning makes the same
tables: the store comes out the same when it is run again. The proof
[run_again_proof.clj](../../../test-store/softland/store/run_again_proof.clj)
runs that.

The door is [door.clj](door.clj): it takes a maker's act into a depot and reads
its answer back. The [module's proof](../../../test-store/softland/store/README.md)
still plays it by hand, so that the store stays proven without the door.

The read path is [read_path.clj](read_path.clj): a caller hands it a query
written as data, and it gives the facts of the layers that the asker may see,
put together, the nearest layer that holds a fact first. In this build its
proof is its only asker.

## The parts

| Part | Responsibility | State and lifetime |
|---|---|---|
| [act.clj](act.clj) | The declared shape of an act and of what it holds, the rules that a shape cannot say, and the check that reads both. It gives its engine to the door's declaration. | None. |
| [door.clj](door.clj) | The door: it opens on the module, gives act-ids, appends each act as its maker gave it, with an act-id, the layer's depot and the time of its server's clock added, and reads the answer back by act-id. It is a client of the module, and checks nothing of an act. | None: it keeps nothing between two calls. It borrows four handles of Rama's client and holds one number, fixed at its opening. |
| [door/](door/README.md) | What arrives on a depot, as the door hands it over and a gate checks it: the door's declaration, the one text of a thing and its digest. And the door's own pure parts: what it makes of one act, the numbers of a wait, and its words. | None. |
| [read_path.clj](read_path.clj) | The read path: it opens on the module, reads a query once or keeps it live, and says when an asker left. It is a client of the module: it selects and watches the four tables of facts, and reads nothing else. It writes nothing. | Who looks at what, what each asked for, and the pushes that they share, with a count of them: the memory of one process, empty at each opening. It borrows four handles of Rama's client. It holds no row and no value. |
| [read_path/](read_path/README.md) | The rules of a read, as pure code: the form of a query and its check, the layers of a read and who may see each, the sources that a read asks of Rama, the answer, and the read path's words. | None. |
| [gate/](gate/README.md) | What a gate decides with, as pure code that the walk of one thing calls for both gates: the checks of one act against rows of the record, the first act of a layer, and the stamp. | None. It reads no PState and no clock. |
| [record/](record/README.md) | What the record is made of: its rows, the keys of its two orders of facts, and the schemas of the PStates that hold them. | None. It holds no record and declares nothing. |
| [module/](module/README.md) | What runs in Rama beside the depots that the module declares: the walk of one thing, the two gates' topologies, which call it, and the two reads. | Each gate owns the four PStates of its record, durable in the cluster. The walk and the reads own nothing. |
| [module.clj](module.clj) | The module `Store`: the two depots of acts, and the one call that adds the two gates and the reads. | The module has two depots, durable in the cluster and never trimmed. This file declares both. No topology appends to either. |

[act.clj](act.clj), [door/](door/README.md), [read_path/](read_path/README.md),
[gate/](gate/README.md) and the rows of
[record/](record/README.md) are pure code
and load with Clojure alone. [door.clj](door.clj), [read_path.clj](read_path.clj),
[module.clj](module.clj), [module/](module/README.md) and the schemas of
[record/](record/README.md) require Rama.

Each folder's map says what its files are. The sections below are about the
module as a whole: how a thing passes through these parts, what a gate does not
check, the order of a layer's acts after a failure, the decisions and the
builders' choices, and the words.

## How the parts relate

The two gates share one walk and the pure code under it. The walk of one thing
([walk.clj](module/walk.clj)) is the decision, written once: each gate calls it
with its own four tables and its depot word. The walk decides with
[declaration.clj](door/declaration.clj), [digest.clj](door/digest.clj),
[decision.clj](gate/decision.clj), [first_act.clj](gate/first_act.clj),
[stamp.clj](gate/stamp.clj) and [rows.clj](record/rows.clj). Where what a
function gives depends on the gate, it takes the gate's depot: the test for a
stray, the form of a making act, each check against the record. No check is
written twice, and the two records have one shape of row.

**The module has two depots**, one for each gate: a client appends a thing to
the depot of its layer. [module.clj](module.clj) declares both. No topology
appends to a depot, so each depot holds what clients appended and nothing else.
What a gate decided of a thing is in the gate's tables and in no depot. The
tables are the record that is read.

**A layer is one line.** Each depot puts a thing on the partition that the hash
of its `:layer` gives. So the acts of a layer are one partition of their depot,
in the depot's order, and the partition's task is the layer's task. That holds
for the base too. A gate hands the walk each thing of a partition on that task,
in that order, and the walk does not give its task back between its first read
and its last write. So, without a failure, each act is decided against the rows
that the acts before it in its layer wrote: a give and the first act under it,
and two acts that replace one fact, are decided in the order of their appends.
"The order of a layer's acts after a failure", below, says what a failure can
do to that. A decision reads rows of the act's own layer only, so it needs no
order between two layers, and the depots hold none.

**A decision is made of the thing and of the gate's own rows on the layer's
task**, and of nothing else. No gate reads a clock: the time of a decision is
the time on the act, which the door wrote from the clock of its server. No gate
reads a table of the other gate. Nothing of a batch is in a decision: not its
id, not its size, not where it is cut. So the same things in the same order
give the same rows, with the same stamps, whenever they are decided.

**The walk of one thing**, on the layer's task, for either gate:

1. [declaration.clj](door/declaration.clj) tests it for a stray. A stray gets no
   answer; the walk adds its digest to the set of its word, in what the gate
   keeps for the task, and ends.
2. The walk reads the answer under the thing's act-id. If one is there, the walk
   decides nothing, writes no row, and ends. A repeat is answered, not decided
   twice.
3. [digest.clj](door/digest.clj) takes the digest of the thing, over its one text
   ([text.clj](door/text.clj)), with the time on the act left out.
   [declaration.clj](door/declaration.clj) checks the thing; it
   reads its shapes with [act.clj](act.clj)'s engine, and in its last step makes
   the one text of each value and holds the bound on one value and on the act.
   A thing with no usable time is refused here.
4. [decision.clj](gate/decision.clj) checks the act against rows that the walk reads
   for it: its standing ([first_act.clj](gate/first_act.clj) knows the form of a making
   act), which also asks what the act's permission allows; and its places. Of a
   layer for which the record holds nothing and which starts with the other
   gate's depot, the check answers from the layer id alone.
5. Only for an act that no check refused, and only when it holds a fact under a
   plain key or under the layer's marked keys: the walk reads the row of the
   layer's marked keys, and the row before of each place under a marked key.
6. The walk reads the last stamp of the act's layer, and
   [stamp.clj](gate/stamp.clj) gives the stamp: of the time on the act, that
   last stamp, and the stamps that the act carries.
   [decision.clj](gate/decision.clj) gives the
   rows to write, each made by [rows.clj](record/rows.clj), and the walk writes them
   to the PStates whose schemas [tables.clj](record/tables.clj) gives: the answer
   and the layer's last stamp, and for an accept the row of each fact. A fact's row
   holds the text of its value, who wrote it and the permission that the act named. For
   a fact under a marked key the walk also writes its row in the facts by value,
   and takes the row of the value before out. The last write is the walk's last
   step.

The walk's own docstring walks the same thing in eight steps, numbered 0 to 7
([walk.clj](module/walk.clj)), and says what each kind of thing costs. The walk
gives back the rows of an accept that a gate may copy to other tasks: the rows
of the facts and of the facts by value.

**A thing for a streaming layer** is appended to the streaming depot. The
streaming gate takes it on the layer's task, in one event, and the event is one
call of the walk: it makes no hop, appends nothing and waits for nothing. It
does not use what the walk gives back, so every row of a streaming layer is on
the layer's task and on no other. The writes of one event become visible
together or not at all. A sender's append with the ack level `:ack` returns
when the event is done and its rows are visible
([streaming_gate.clj](module/streaming_gate.clj)).

**A thing for a microbatch layer** is appended to the microbatch depot. The
microbatch gate takes each thing of a batch on the task of its partition, in the
depot's order, and calls the walk for it there. Then, for an accept, it goes to
every task and writes the rows that the walk gave back on every task but the
deciding one, in the order in which the walk wrote them. So the rows of a
microbatch layer live in two ways: the facts and the facts by value are the same
rows on every task, and the answers and the layer's last stamp are on the layer's
task alone. A stray's digest is on the task where the stray arrived. The copies
of one decision go out before the next thing of the partition is decided, so
each task writes the copies of a layer in the order of the decisions. Rama
commits a batch on every task or on none, and a second attempt of a batch makes
the same rows. An append to this depot does not wait for the gate: its ack says
only that the depot holds the thing
([microbatch_gate.clj](module/microbatch_gate.clj)).

**An act sent to the wrong depot** is refused as an act into a layer of the
other gate, on both depots, whether or not that layer is made. A gate whose own
record holds nothing for a layer asks the layer id which depot the layer starts
with ([first_act.clj](gate/first_act.clj)), and reads nothing of the other gate
for it.

**The door reads** through [door_reads.clj](module/door_reads.clj), by two reads: the
answer under an act-id, and the depot of a layer. Each read goes straight to the
layer's task, for a layer of either depot, and reads one row there. A read gives
a row when the write of it is committed on that task: for a microbatch layer,
when the batch that decided the act is committed on the layer's task. Neither
read waits for anything after that.

**The read path reads** the gates' tables straight: the facts and the facts by
value of each gate, with a partitioning key for each read, once by a select and
kept live by Rama's push. It calls no query of the module. Every source of a
microbatch layer is read under the routing id of the read's home: the layer
that the read starts from, `:from-layer` or the first of `:layers`. So the base
is read on the task where the asker's own layer lives. The read path asks no
task by number. [read_path/](read_path/README.md) says how a read is put
together.

Each gate's dataflow reads rows, calls the pure functions and writes rows, and
nothing else: it reads no clock and appends to no depot. A pure function gives
the same answer when Rama runs an event or a batch again, never throws on what
arrived, and is proven without a cluster.

## The act

The form of an act is declared, not implied. [act.clj](act.clj) holds two
declarations as plain data, and they are the place to start:

- the shape: the parts of an act, what each part holds, which parts must be
  there, and the reason word when a part is wrong; and the same for a fact, an
  act-id, a fact id, a permission id and a carried stamp;
- the rules: what must hold between two places of an act, which a shape cannot
  say, such as that the permission's layer is the act's layer.

The check holds no knowledge of the form. It reads the two declarations: first the
shape, part by part in the order declared, then the rules in their order. So the
order of the declarations is the order of the reasons.

Code is in three places only, and each is small: the leaves, which are the few
shapes that a declaration cannot say in data (an id, a key, a value); the engine
that reads a shape; and one function for each rule.

The shape is compiled with the store. It is not a fact in the store, because each
shape that a tool gives enters the store as a fact inside an act; the act's own
shape must hold when each fact is wrong.

## What the gate does not check, said plainly

- **Who makes a layer.** Until sign-in is built, any sender can make any layer
  that is not made, for any holder. The gate cannot tell a person from a session
  or an agent, and it cannot check `:who`: `:who` is a claim. It holds `:who` only
  against the holder of the act's permission and against the form of a making act.
- **Where a thing sits on its depot.** Each depot's partitioner puts a thing on
  its layer's task, and neither gate makes a hop before it decides. Every
  client's append goes through that partitioner. A topology can append to the
  partition of the task that it is on, whatever the partitioner; no topology of
  this module appends to a depot. A thing that a topology appended to another
  partition would be decided against that task's record, with that task's last
  stamp of the layer. On the streaming depot it can be refused as an act into a
  layer that is not made, and the answer of that refusal is then on that task,
  where no read by its act-id looks. Or, when it is a making act, it can be
  accepted there and leave rows and an answer on that task that no read finds.
  The rows of the true layer, on its own task, are not touched. On the
  microbatch depot such a thing finds the layer's facts, which every task
  holds, and not the layer's last stamp or its answers: it is decided out of
  the layer's line, and its answer is on a task where no read by its act-id
  looks. When its permission stands it is accepted there, and the copies of
  its facts write over the rows of the same places on every other task, the
  base's own among them. Read from the dataflow; no run made such an append.
- **The time on an act.** A gate takes the time as it arrived. It cannot tell
  the clock of a door's server from a number that another sender wrote, and it
  holds the time against no clock: only to its range, 0 or more and below 2^45
  milliseconds. Two doors on two servers write two clocks. No check holds one
  clock against the other by itself; the 10-second rule does through a layer's
  stamps, since an act that carries a true stamp of its layer is refused when
  that stamp's time is more than 10 seconds ahead of the time on the act. A
  time that is behind the layer's last stamp does not put a stamp back: the
  stamp is then the last stamp plus one. A time that is far ahead is taken
  when the act is accepted, and each later stamp of its layer is later than
  it; the time on a thing that is refused moves nothing.
  "Engineering choices" says what is kept there and not decided.
- **The order of a layer's acts after a failure.** No check of a gate looks at
  it. "The order of a layer's acts after a failure", below, says what was run,
  what was read and what was not reached.
- **Who reads a value.** Any client of the cluster can read every value: in a
  table, and in a log too, where each value is plain, as its maker gave it. A
  copy of a log taken alone holds every value. In the runtime the module has
  two clients, the door and the read path. A guard of the record is not built
  (?11 is open).
- **What a fact's row holds for a value.** The gate keeps the one text of a
  value that passed `value?`, in its normal form. No arrived value is a run of
  bytes: a value that is no value of the form is refused `:malformed`, and the
  thing that held it is in the log. `read-value` in [rows.clj](record/rows.clj)
  tells a reader of a row whose String is the text of no value; no write of a
  gate gives such a row. One thing a gate cannot see: a string that holds half
  of a surrogate pair comes off the depot with `?` in the place of the half, by
  Rama's writing of a record, and is kept so. Its maker reads her answer as her
  own all the same, because the digest is taken of UTF-8, which makes the same
  replacement. A probe saw that on Rama 1.6.0 in-process, on a module of its
  own and not on this one.
- **Who may write a layer's setting, its list and its marks.** Not decided, and
  kept only so that the read build can run: whoever may act in the layer. So any
  holder of "see and act" in a layer can set that layer to `:everyone`, and any
  such holder in the base can set the base to `:members`. Sid's word for who
  writes the setting is "the owner", and no layer names an owner as a fact. The
  one place for the other answer is `who-may-write-fault` in
  [decision.clj](gate/decision.clj).
- **What a list of layers names.** A gate takes the list as written: a layer that
  is not made, the layer itself, a layer named twice, and two lists that name
  each other are all accepted. A read handles them.
- **What a marked key holds.** Every fact with a value under a marked key is kept
  in the order by value, whatever the value is: a gate does not judge what an id
  is. One value that very many places hold is one run, without a bound: in a
  streaming layer on the layer's task, and in the base on every task. Not
  decided.
- **The facts of a key from before its mark.** The order by value holds the facts
  that are written while the key is marked. Nothing is filled from before, and no
  event or batch walks a layer. The row of the marked keys says since which stamp
  each key's order is level. When a mark is taken off and put on again, the rows
  of the earlier marking stay where they are, for ever, and no read names them.
- **One moment for a fact and its row of the order by value.** On one task the
  two become visible together, in both gates: in a streaming layer they are one
  event, and in the base each task shows its part of a batch at once.
- **A direct read of the tables.** The PStates are not declared private. A
  select that goes straight to a PState of a gate sees a row when the event or
  the batch that wrote it is committed on the task that it reads. The read path
  reads so, for every fact that a caller is given, and so do the proofs' own
  hands. In the base a direct read is given one task's part of a batch: a row
  that a task shows is the row that stays, and that every task shows a batch
  together is not given. When a commit
  fails after some tasks have shown their part, the others show it only when
  the second attempt commits; a probe saw 3.4 seconds between them, on a module
  of its own and not on this one.
- **One instant for two tasks.** Rama makes a batch visible task by task, and
  two tasks do not commit at one moment. So for the time of a commit a reader
  of another task can see the copy of a fact whose answer is not yet visible on
  the layer's task, or not yet see the fact of an answer that is. The door's
  two reads of a layer are on one task, the layer's, where an answer and each
  fact of its act become visible together. This is from Rama's docs on the
  phases of a microbatch; no run observed a commit from outside.
- **The same rows on every attempt of a batch.** When an attempt of a batch
  fails, Rama sets the gate's tables back on every task and processes the same
  records again, in the depot's order. A decision is made of the thing and of
  those rows, with no clock and nothing of the attempt in it. So each check
  gives the same verdict and the same word on every attempt, the 10-second rule
  too, and each row has the same stamp: a row that a reader was given does not
  change. That holds while one version of the module's code runs every attempt.
  That a retry makes the same tables was run on a stand-in gate; that a reader
  was given a row before a retry was not run
  ([door_reads.clj](module/door_reads.clj)).
- **What an act with no permission costs a gate.** For an act in a made layer
  whose `:who` holds no permission there: 5 reads and 2 row writes, whatever
  the act carries, in either gate. It reads the answer, the two places of its
  standing and the layer's last stamp, which is two reads, and writes the
  answer and the layer's last stamp. Its refusal raises the layer's last stamp
  by one and moves no time of the layer, whatever time it carries. The door is
  no guard of it: it stops no act. Each act
  of such a who reaches the gate and is recorded, refused under its own act-id
  (entry 7 of 12 October). In the base each is decided on the layer's task, in
  the layer's one line.
- **How long one act can hold a task.** Between its first read and its last
  write the walk does not give the task back, in either gate: a yield would let
  another act of the
  layer in between this act's reads and its writes. So one
  act holds its task
  for its digest, for all its reads, for the making of the text of each of its
  values, and, under marked keys, for the digests and the writes of the order by
  value. Only the limits of the declaration bound them: up to 256 places, each
  read once at most, for a check or as the row before; up to 256 values, the
  text of each of 65,536 bytes at most; and the bound on the act, 18,874,368
  bytes of text in the rows of its facts. The greatest case that the order by
  value adds: an act of 256 facts, each under a marked key, each a value whose
  text is 65,536 bytes over a place that holds such a value written since the
  mark, none naming something to replace. No check of that act reads a place.
  For the order by value it reads the row of the marked keys and 256 rows
  before, about 16.8 MB of rows; takes 512 digests, one of each new value and
  one of each value before, over about 33.5 MB; and makes 512 writes of the
  order by value beside its 256 fact rows. Such an act needs standing in the
  layer. In the base the task that it holds is the one that decides every act
  of the layer, and the gate then writes the same 768 rows on each other task.
  Nothing is built against it: the remedy is a choice of reads or of
  limits, and no speed is measured. The door bounds nothing: a thing of any
  size that Rama's client writes reaches the gate. The gate takes the digest of
  the whole thing before the declaration's check refuses it, and a value over
  its bound costs the making of its text, once, before its refusal.

## The order of a layer's acts after a failure

Sid, 13 October 2026: "I am against the Rama setting that orders a layer's acts
after a failure. Nobody has shown whether a failure in a fast layer can make
acts that were in flight come out in another order when the store is run again.
Run it and find out. If it can, look for a cheap way to close it. If there is
none, say it on the store's page, and the proof names such acts."

The setting is `topology.stream.max.executing.per.task` at 1: a task then has
one record pending at a time. The cost is one act in each batch of a task, so
each act waits for the replication of the act before it; that is paid against
the target of 1,000 acts a second on one task. The module leaves the setting at
Rama's default.

The question is of a streaming layer. Without a failure the streaming gate
decides the acts of a layer in the depot's order, and a run again decides them
in that order too. A run again meets none of the failures of the first run. So
if a failure made the first run decide two acts of a layer in another order
than the depot's, the answers and the stamps of those acts can differ from what
a run again gives. The gate's own code does not throw on what arrives, so such
a failure needs a fault of the machine, of Rama or of this module. In the base
a failed attempt of a batch leaves no row on any task, and Rama processes the
same records again in the depot's order
([microbatch_gate.clj](module/microbatch_gate.clj) says how far that was run).

**What was run.** On a stand-in gate of the store's form, and not on the store:
a stream topology with no partitioner, no append and no wait in an event, a
depot that partitions by the layer, and the retry mode "all after". Rama 1.6.0,
in process, on one machine. In each run a first module decided acts as they
arrived, with a failure put in; then a second module decided the same depot
from its beginning with no failure, and the two were held against each other.

- A throw, with later acts of the layer in flight. Rama starts the worker
  again, the rows of the failed batch are gone, and the records are then
  decided in the depot's order. The run again decided the same.
- A stall on the task's thread past Rama's stream timeout, the thread held,
  with later acts of the layer in flight, in every window tried: the later act
  in the depot before the stalled event began, sent while it stalled, sent
  after the timeout had passed, and sent when it had woken. Rama starts no
  worker again and drops nothing: the stalled act's rows stay, Rama runs its
  record a second time, and the second run finds its answer and writes nothing.
  In the first of those windows the later act's first run began before the
  stalled act's second run; the stalled act's rows were there, so the later act
  was decided as in the run again. The run again decided the same.
- In all runs with that retry mode the run again decided the same, 87 of 87:
  those with a throw, those with a stall, and beside them runs with an answer
  that Rama could not write, with a slow write of the batch, with the stream
  timeout set very high, with a module update under load, and with no failure.
- What a sender is told is another matter. In these runs Rama failed the `:ack`
  append of an act that was accepted and whose rows stayed, of acts that were
  decided after a restart, and of acts that never reached the depot. The ack is
  a signal; the sender reads the record.

**What was read, and what was seen before.** The swap that a probe saw on
12 October needed an event that waited with its thread free. Rama then forced
the stalled batch to fail with no restart, dropped its rows and went on to the
next batch, so a later act of the layer was decided before the failed one. The
store's event waits for nothing: it passes no partitioner and makes no append.
Rama's docs say of the retry mode "all after" that a failed record makes Rama
retry all records after it on the same depot partition, also those that had
succeeded.

**What was not reached.** One case could not be made in process: a batch that
Rama fails with no restart of the worker and whose rows are dropped, while a
later act of the layer is already in flight behind it. In process the end of a
batch waits for nothing. On a cluster with more than one copy the end of a
batch waits for the copies; whether that wait can make Rama force such a
failure on a batch of this gate was not run. If that happens, the acts of that
layer that were in flight can be decided in another order than the depot's, and
a run again decides them in the depot's order: the two then hold other answers
for those acts. That was computed with the stand-in gate's own rule over the
records of its runs, and not run. What a change of a task's leader does to acts
in flight was not reached either.

**No cheap way to close it was found.** The operations that Rama documents
give a stream event no position of its record in the depot, and no way to fail
a record or put it back but a throw; so an event cannot see that it runs ahead
of an earlier record of its layer. Another retry mode does not close it: "individual" gave the same as
"all after", and with "none" the acts of a failed batch were never decided.
Rama's stream timeout set very high turned a stall into a delay in the runs;
that it would also keep Rama from forcing a slow batch to fail was derived and
not run. The setting that Sid is against is the one known way.

**How such acts are named.** Within one layer the stamps rise with the places
in the layer's partition of its depot. An act is out of its line when the depot
holds it and it has no answer, with the layer at rest; or when an act of its
layer stands before it in the depot with a higher stamp, or behind it with a
lower one. Both acts of a swapped pair are named. An act of the layer that
stands behind a named one can differ too, and the rule does not say which. The
run-again proof
([run_again_proof.clj](../../../test-store/softland/store/run_again_proof.clj))
holds this rule and names such acts. On the stand-in the rule named nothing in
each run that came out the same; its second part, the stamps out of their
order, was shown by computing and not by a run, since no run made the swap.

**What was run on the store itself.** The run-again proof makes three kinds of
failure on the store's own module and gates, with no option of Rama's set. Each
is made with acts whose answers would show another order: a later act that
names the fact of the act before it to replace.

- A throw in the decision of an act of the base, in the middle of a batch, with
  a later act behind it in that batch and one more sent after the throw.
- A stall of 8 seconds on the task's thread in an event of a streaming layer,
  past Rama's own stream timeout, with one later act of the layer in the depot
  before the stalled event began and one sent a second into the stall.
- A throw in an event of a streaming layer, once with a later act right behind
  it and once with a later act in the depot before the event began.

After each, a second module decided the two depots again from their beginning,
with no failure. No row of any table differed between the two, and the rule
named no act. At the stall the store did what the stand-in did: the stalled act
was decided when it woke; the later act's first run came before the stalled
act's second run, which found its answer and wrote nothing. A throw started the
worker again, in either gate. Two makers were told that their append had failed
though both acts were accepted and hold their rows.

The proof applies the rule to every partition of both depots in each of its
checks, also in the runs with no failure. An act that the rule names is printed
on a line that begins `OUT OF ITS LINE`, and the proof fails. So the day a
layer's acts are decided out of the depot's order in a run of the proof, the
proof says which acts. The case under "What was not reached" was not reached on
the store either.

## Decisions this module stands on

Sid's decisions are in [the store source entry](../../README.md), with his
reasons. This lists them by row and says where each shows here.

The act (6 October): the twelve decisions of "Decisions of 6 October 2026".
[act.clj](act.clj) is their code. Decision 2 is why
[declaration.clj](door/declaration.clj) contains the maker's shapes and does not write
them again. Decision 3 stands in its first half: the act holds plain values. Its other
half, that the door seals and a gate refuses an unsealed value, is set aside by
entry 2 of 12 October.
Decision 5 is the act-id, `[layer depot uuid]`.

The store's module (7 October):

| Row | Decision | Where it shows |
|---|---|---|
| F6 A, Q7 a | Two depots, one for each gate: the streaming depot and the microbatch depot | [module.clj](module.clj) declares `*streaming-depot` and `*microbatch-depot`, and the module has no other depot. No topology appends to either |
| F2 A | The record is in the gates' PStates | Each gate owns the PStates of its record, and they are the record that is read; no depot is ever trimmed. What a gate decided is in its tables and in no depot. A second run of the gates over the two depots of acts makes the same tables; rule 19 says how, and what it rests on |
| F1 A | The depot of a layer is a fact on the layer | The row at the layer's own entity under `:softland.store/depot` ([rows.clj](record/rows.clj)) |
| F4 C | The base is a microbatch layer; every other layer starts streaming | `depot-at-making` in [first_act.clj](gate/first_act.clj) |
| F5 A | A gate reads on its own task | Neither gate's decision has a hop: the walk ([walk.clj](module/walk.clj)) reads and writes on the layer's task, where the depot put the thing. Each row that a decision reads is a row of the act's own layer, and the layer's task holds them all. In a microbatch layer every other task holds a copy of each fact's row, the layer's depot fact and each permission among them, so a read finds them on the task where it runs. A layer holds facts under its own entity |
| Q22 A | The door reads an answer from the record by act-id; the ack is a signal | `answer-by-act-id` in [door_reads.clj](module/door_reads.clj); the streaming gate gives nothing back in the ack, and an ack of the microbatch depot says only that the depot holds the thing |
| F7 A | A signal is not the truth; the record is; a gate reads its own record by act-id before it decides | Check (a): the first read of the walk for each thing that is no stray, in both gates |
| F8 C | The gate refuses a setting key on an entity that is not the act's layer | Check (f) in [decision.clj](gate/decision.clj); `setting-keys` in [declaration.clj](door/declaration.clj) |
| F9 | The three limits are targets | See "Engineering choices", rule 21 |

Who acts, and what the store reads (8 October):

| Row | Decision | Where it shows |
|---|---|---|
| E1·H8 A | The system stands at the top of the base's chain | `system` and `base` in [first_act.clj](gate/first_act.clj) |
| K2 | A signed-in user has a personal layer and a permission in it; the person who creates a group makes its layer | The form of a making act in [first_act.clj](gate/first_act.clj) |
| E4 A, E2 A; and E3·H7 B, which is a session's reading and not his answer | Permission is a hierarchy that ties back to one person; each actor acts under its own; whoever holds one can give one beneath it | Checks (d) and (g) in [decision.clj](gate/decision.clj). Since the read build a holder of a permission that says `:see` cannot give: her give is an act, and is refused `:permission-does-not-allow-acting` |
| H2 A | A stamp holds the clock time and a counter | [stamp.clj](gate/stamp.clj). The clock time is the time on the act, which the door wrote (decision 1 of 13 October, below) |
| K1 A | The store's keys of that day are the depot and a permission. The third of his list, the door's request for locks, is none of the store's keys, by entry 2 of 12 October | `store-keys` in [declaration.clj](door/declaration.clj). The table holds those two and the three of the decisions of 11 and 12 October: who may see a layer, what it builds on, its marked keys |

The door's four questions (9 October):

| Row | Decision | Where it shows |
|---|---|---|
| ?4 A | A person's id is a UUID that says nothing; her sign-in accounts are joined to it by links | Nowhere yet: no code makes a person. An id can be a UUID in [act.clj](act.clj) |
| H1 A | One lock seals one value | Nowhere: entry 2 of 12 October sets it aside, and [the store source entry](../../README.md) says where the code of the locks is |
| K3 A | The system can hold many layers | The form of a making act in [first_act.clj](gate/first_act.clj): `system` can be the first holder of any layer |
| K4 B | The store's own keys carry the namespace `softland.store`, and the word for a layer's kind is "depot". A gate refuses a key in that namespace that the store does not know | `store-namespace`, `store-keys` and the rule `:store-key-is-known` in [declaration.clj](door/declaration.clj); the part `:depot` of what arrives, and "depot" in each name of this tree |

Reading back out (11 and 12 October), the entries that the gates carry, and the
entries that the read path carries. [The read path's own page](read_path/README.md)
lists its entries again, by file.

| Entry | Decision | Where it shows |
|---|---|---|
| 1 | A layer has a setting, "members" or "everyone". A permission says what it allows: see, or see and act | The store key `:softland.store/who-may-see` and the rule of its value in [declaration.clj](door/declaration.clj). The slot of a given permission in [act.clj](act.clj), and `allows`, which reads it. `standing` in [decision.clj](gate/decision.clj) refuses an act under a permission that says `:see`. For a read: `standing` in [layers.clj](read_path/layers.clj) says whether an asker may see a layer, from the layer's setting and her permission row |
| 2 | A layer says which layers it builds on, in order, as a fact on the layer | The store key `:softland.store/builds-on` and its two rules in [declaration.clj](door/declaration.clj). For a read: `in-order` in [layers.clj](read_path/layers.clj) gives the layers of a read, from that fact or from the list that the query gives; the walk of a place in [answer.clj](read_path/answer.clj) lets the nearest layer that holds a fact win |
| 3 | A read is done in the runtime, straight from the tables, with Rama's push. The pushed value is not a cache | [read_path.clj](read_path.clj): a select for a read asked once, a push for a read kept live, one push for those who look at the same thing, closed when the last one leaves. It holds no value of its own beside the pushes |
| 4 | Only programs on machines that Sid runs may reach Rama's ports | Nowhere in code: it is a setting of the network. The read path is such a program: Rama sends it whole rows, and it hands a caller only the layers that she may see and the parts that she asked for |
| 5 | The tables keep a value plain | `fact-row` in [rows.clj](record/rows.clj) keeps the one text of the value in its normal form, as a String, as the last step of the check in [declaration.clj](door/declaration.clj) made it; `plain-bytes` gives its UTF-8. The act in the depot's log is as its maker gave it, each value plain, with an act-id, the layer's depot and the time of the door's clock added |
| 6 | For the keys that a layer marks, the gates keep the facts in one more order, by value | The store key `:softland.store/marked-keys`; `facts-by-value` in [tables.clj](record/tables.clj) and the PState that each gate declares with it; the key of that order, the two ends of a run and the value digest in [rows.clj](record/rows.clj); `writes` in [decision.clj](gate/decision.clj). For a read: the four words of a query in [query.clj](read_path/query.clj), and what each reads in one layer in [sources.clj](read_path/sources.clj) |
| 7 | From a fact, a read can get who wrote it, and under which permission | The fields `:who` and `:permission` of a fact's row, and of a row of the facts by value, in [rows.clj](record/rows.clj). For a read: `place-answer` in [answer.clj](read_path/answer.clj) puts either in only when the query asked |
| 8 | The base's second is fine. The base stays a microbatch layer | The base is decided in batches, by the microbatch gate. What the read build does with that is a session's reading, which he did not strike: a row of the base that a task shows is the row that every attempt of its batch makes, because a decision holds no clock and nothing of the attempt ("The same rows on every attempt of a batch", above); and a place holds one row, its current fact, so a select of a place that holds a fact never gives nothing. The read path reads a row of the base straight, as it reads a row of a streaming layer |

Before real data is kept (12 October), the entries that show in code:

| Entry | Decision | Where it shows |
|---|---|---|
| 1 | Every table can be made again from the logs | By a second run of the gates over the two logs of acts. `add-gates-and-reads` in [module.clj](module.clj) adds the gates and the reads to any module that has the two depots, or mirrors of them, under their names; such a module decides every thing again from the beginning of each depot, into tables of its own. Rule 19 says what that rests on. The box that a session had put under this entry, a log of what a gate decided and a rebuild that applies it, is set aside by decision 5 of 13 October: the module holds neither |
| 2 | Every value is plain in the log, and the store keeps no locks | [door.clj](door.clj) appends the maker's act with `:act-id`, `:depot` and `:door-time` added, and nothing else changed. In [declaration.clj](door/declaration.clj) what arrives as a fact is the maker's fact, and a part that the shape does not name is refused `:unknown-part`. No PState and no query of the module holds a lock |
| 6, its fourth | The limit of four on the depth of a permission is lifted, and a bound on the size of an act takes its place | `nesting` in [act.clj](act.clj) gives a permission the value's own number, 32. `max-act-bytes` in [declaration.clj](door/declaration.clj), held by the last step of the check with the word `:act-too-large`. "Engineering choices" says what each counts |
| 7 | An act from a maker who may not act is written to the log, and then refused. The door checks nothing first | The door's three tests and no other, `reading` in [additions.clj](door/additions.clj): each is of what its three additions need. `standing` in [decision.clj](gate/decision.clj) refuses such an act under its own act-id, and no fact of it is in the tables |

So that the store comes out the same when it is run again (13 October). Sid sent
eight decisions that day, and the numbers here are those of his list.
[The store source entry](../../README.md) holds them in full under "Decisions of
13 October 2026", each entry under the same number. Each line in quotation
marks is as he sent it on 13 October 2026.

| Decision | What Sid said | What it shapes here |
|---|---|---|
| 1 | "The clock comes before the depot. The door writes the time on the act. No gate reads a clock." | The door adds `:door-time` to a maker's thing, beside `:act-id` and `:depot` ([door.clj](door.clj)). The walk takes the time of a decision from the thing (`time-on` in [decision.clj](gate/decision.clj)), and [stamp.clj](gate/stamp.clj) is handed each number. The digest leaves the time out ([digest.clj](door/digest.clj)). No code of the module reads a clock |
| 2 | "There is local ordering: the order Rama gives inside one partition." | The one order that a decision rests on is the order of a layer's partition in its depot. The module makes no order between two partitions or between the two depots: no gate reads a table of the other gate, and a decision reads rows of the act's own layer only |
| 3 | "Every act is placed by its layer key, the base too. A layer is one line." | [module.clj](module.clj) declares both depots with the hash of the thing's `:layer`. The routing id of an act-id is its layer, for both depots (`routing-id` in [rows.clj](record/rows.clj)), and both reads of the door go to the layer's task ([door_reads.clj](module/door_reads.clj)) |
| 4 | "The base's rows: a copy on every task. One task decides, in the line's order." | [microbatch_gate.clj](module/microbatch_gate.clj): for each thing of the partition, in the depot's order, the walk on the layer's task, and then the copy of an accept's rows of facts, and of facts by value, to every other task. That the answers and the layer's last stamp stay on the layer's task alone is the builder's, under "Engineering choices" |
| 5 | "The two depots that hold the answers go, and the rebuild with them. The depot of batch clocks goes. Two depots stay." | The module has the two depots of acts and no other, and no topology appends to a depot. A gate closes no batch, and the door's reads wait for none. No code makes a row from a log of decisions: the tables are made again by a second run of the gates (rule 19) |
| 8 | "A number counted per layer. The clock the door carries is the clock of the server that runs the door. What the door does with a thing it cannot label stays as built." | `task` in [tables.clj](record/tables.clj) keeps one last stamp for each layer, under the text of the layer id, and the walk reads and writes the number of the act's layer: two layers on one task count apart. The door's part of this decision is in [door/README.md](door/README.md) |

His decision 6 of that day, on a change in what a gate accepts, is under rule
19. His decision 7 is the section "The order of a layer's acts after a
failure", above.

## Engineering choices

Each is the builder's, with its reason. None is a decision of Sid's. Where a
choice stands under a decision of his, the paragraph names the decision and
says which part is the builder's. A choice that is marked "for Sid to review"
is one that a session made and he has not seen.

**The module and its classpath.** The module is `Store` in
`softland.store.module`. It has its own alias, `:store-module`; the reason is in
[the store source entry](../../README.md).

**`act.clj` gives its engine to other namespaces.** Its engine takes the map of
shapes, and `as-shape` gives the engine to another namespace. So the door's
declaration, and the check of a query in the read path, are data that the same
engine reads. The leaves and the nesting limits stay
`act.clj`'s own. The limit on the depth of a permission is the value's own
number, so the two roads of a permission, an act's own and one that a fact
gives, end at one depth.

**The record: flat PStates with string keys.** An answer is found by an
act's uuid and a fact by its place; they share no key. With the
layer as the top-level key and maps under it, each row would cost two seeks to
read and one more to write. A key is a string made of the printed texts of its ids,
with a space between: one class, and Rama gives the order of string keys, on
which the run of one value rests. A row is placed by the task on
which its gate writes it, and none by a hash of its whole key. In a streaming
layer that is the layer's task for every row. In a microbatch layer it is the
layer's task for an answer, and every task for a row of the facts and of the
facts by value.

**What one accepted act costs the walk**, on the layer's task, in either gate.
The act: one value under a plain key, on one entity, nothing named to replace.
Counted from the dataflow, not measured.

| | Reads | Row writes |
|---|---|---|
| The key not marked, or no key of the layer marked | 5: the answer, the permission, the marked keys, and two for the layer's last stamp | 3: the fact, the answer, the layer's last stamp |
| The key marked | 6: and the row before | 4: and the entry in. 5 when the entry of a value before goes out |

Without the order by value it would be 4 reads. The one read more, of the
layer's marked keys, is paid by every accepted act with a fact under a plain
key, also in a layer that marks no key: a gate must know whether the key is
marked where it writes the fact. An act with no fact under a plain key, a
give, a setting or a list, reads no marked keys. A refused act reads none
either: no check reads the marked keys. An act that the declaration refuses, a
value or an act over its bound and a thing with no usable time among them,
costs 3 reads, the answer and the two of the layer's last stamp, and 2 row writes. Keeping
the marks in memory
between two events would be a cache, which the project's rule sends to its own
examination. The marks are a fact of the layer, in the table of facts, and the
layer's last stamp is in what the gate keeps for a task; so they are three
reads, one for the marks and two for the last stamp. A form that would give 3
reads for the whole act, where the count is 5, was found and is not built: one
row for each layer that holds the layer's last stamp and its marks. Kept as
built, and not decided.

A thing under an act-id that has an answer costs 1 read and no write. A stray
reads no row and costs one add to a set; the set tracks its size, and Rama's
docs say that size tracking adds a read on disk to each write. The read of a
layer's last stamp is counted as two: the number sits one level down in its
table, under a field and then the layer's key, and Rama's skill text counts
each level of a path as a read on disk ([tables.clj](record/tables.clj) says
why it is kept so). Its write is one. No run here measured a read on disk.
[walk.clj](module/walk.clj) holds the count for each kind of thing. The streaming gate's event is the walk and nothing else, so
this is the whole cost of a thing of a streaming layer.

**A permission fact sits on its holder.** The gate then finds the permission of
an act's `:who` with one read. It follows that a holder has one permission in a
layer: she cannot hold "see" from one giver and "act" from another. Sid should
know this one. It is not decided, and is kept as built; the other answer changes
one rule of the declaration and the key that the gate reads.

**What a permission allows is a slot of the permission.** A given permission is
`[who layer granted-in parent]` or `[who layer granted-in parent allows]`, where
`allows` is `:see` or `:see-and-act`; a first permission has no slot. Without the
slot a permission reads as "see and act" (`allows` in [act.clj](act.clj)). The
row of a permission holds the text of the whole permission, slot and all, and
check (d) stays one read and one comparison, of that text with the text of the
permission that the act names: so nobody raises her right
by naming it, and each act in the log says under which right it was made. The
check stands with the standing, before any place is read. A making act names a
first permission, so one whose permission has the slot is no making act and is
refused `:layer-not-made`. The other form that was weighed keeps what a
permission allows beside the permission and not in it; `allows`, the shape and
check (d) are what it would change. **"See" stays "see"**: a permission's place
takes one write and nothing ends a permission, so a permission that says `:see`
cannot be raised. That is kept, and not decided.

**How deep a permission can be is not decided.** Kept: as deep as a value may
be. A permission can hold 32 permission ids, itself counted, as an act's own
permission and as one that a fact gives; one deeper is refused `:malformed` on
either road. The one place for the other answer is `nesting` in
[act.clj](act.clj). A smaller number there ends both roads. A greater one needs
the value's own limit too, and must stay under the depth at which the one text
stops writing a part out. The text of the whole permission is a field of each
fact's row that is written under it, and of each row of the facts by value;
that is what the bound on an act counts.

**The value in a fact's row is plain, and a String.** For each fact that has a
value the row holds the one text of the value in its normal form. The last step
of the declaration's check makes it once, and the row takes it as it is. The
field is a String and no byte array. The reason is
Rama's push. On rows that hold a byte array a push was sent the whole value
anew, about once a second, and a live reader of a fact was told a change about a
second late; on the same rows with a String a push got one change for each
write. That was run on Rama 1.6.0 in-process; its cause was not found, and is a
question for Rama's team. The answers keep their byte array: no
read is kept live on them.

**The bound on one value, and the bound on one act.** A gate holds both, in the
last step of the declaration's check, before it reads any row of the act's
standing, of its places or of the marked keys. A value under a plain key holds
65,536 bytes at most, counted as the UTF-8 bytes of its one text, which are the
bytes that the fact's row holds; one over is refused `:value-too-large`. A value
under a store key keeps its own limits. An accept of one act writes 18,874,368
bytes of text at most into the rows of its facts: for each fact the text of its
value, and the texts of the act's who and of its permission, which each fact's
row holds. One over is refused `:act-too-large`; an act with both faults gets
the first word. The number is 256 facts, each with 65,536 bytes of value and
8,192 of who and permission. The bound does not count the rows of the facts by
value: which keys are marked is read only after every check. The step is the
last because it is the most costly: it makes the text of each value once, and
the String that it counted is the String that the row holds. Each number is the
builder's, kept, and decided by nobody.

**Who wrote a fact, and under which permission, are two fields of its row**,
each a String, the one text of what it is: a UUID has two texts, and the one
text reads back as a UUID. `:who` is what the act claimed; a gate cannot check
it until sign-in is built.

**The three keys that take a second write.** Who may see a layer, what it builds
on, and its marked keys are each one fact on the layer's own entity, a setting in
the code's sense, with a plain value. Each can be written again at any time,
with `:replaces` or without: two writers at once leave the later one, and a
writer who must not lose a write names what she replaces. A retract of one is
refused; a list is emptied, and a mark taken off, by a second write of a value
that says so. **Who may write them is not decided**; kept: whoever may act in
the layer (see "What the gate does not check"). **What a layer starts with is
not decided**; kept: a new layer is seen by its members, the base by everyone,
and a layer builds on nothing and marks no key until a fact says so. No fact is
written at the making: `starts-with` in [first_act.clj](gate/first_act.clj) reads
"no fact", and is the one place for the other answer. A later change there
changes how every layer with no fact reads.

**The mark of a key is one fact that holds a set.** A gate then reads the marks
of an act with one read, where one fact for each key would cost a read for each
key of the act. A layer marks 32 keys at most. A mark of a key in the store's
own namespace is refused by the declaration: the store's facts are not kept in
the order by value.

**Since which stamp a key's order is level is kept in the row of the marked
keys**, as the field `:level-since`, the text of a map from each marked key to a
stamp. No maker can write a gate's stamp, so the gate derives the map at the
write: a key that the row before held keeps its stamp, and a key that is newly
marked gets a new one: the stamp of the act that marks it, in both gates. An
act is decided with the layer's marks as they were before it, so a fact of the
act that marks its own key is not in the order by value, and a mark holds from
the next act of the layer. So
one sentence holds for both gates: the order of a marked key holds every fact
under that key whose stamp is later than the key's stamp, and no other. A field
of that one row, and not a table of its own: the gate reads one row for the
marks and has both.

**The facts by value: one more table for each gate.** Its key is
`"<layer> <key> <since> <value digest> <entity>"`: the layer, the marked key,
the key's stamp, the SHA-256 of the UTF-8 bytes of the row's value as 64 hex
characters, then
the entity. So every place that holds one value under one key is a run of
neighbouring keys, found with one seek, and no run holds a row of another value.
A digest and not the value's text: a key then has a bound whatever the value is,
and a text may hold a space; two values
share a run only by a collision of the hash. The stamp is in the key so that a
mark which is taken off and put on again names other keys: the rows of the
earlier marking are then in no run that a read names, and need no walk to take
out. A row holds the fact's row without its value, so a read of a run gets who
wrote each place with no second read. It costs each write under a marked key one
read, of the row before, since the key of the entry to take out holds the digest
of the value before. A table of its own and not more keys of the facts table:
its key has five parts where a place has three, so in one map a key of this
order could sort into the run of an entity.

**The gate does not read the permissions above the act's own.** A permission row
is written only by a right making act or by a give whose own permission stood and
is exactly the one above, and nothing in this build ends a permission. Reading
them would cost up to 31 reads on every act to find what cannot be false.

**The depot of a layer costs no read for a layer that is made here.** A gate's
record holds rows only for layers of its depot, so a row at the permission's
place or at the depot's place says that the layer is this gate's.

**The stamp's packing.** Milliseconds since 1970 × 65,536 + a counter. One
millisecond holds 65,536 stamps for a layer; the packing reaches 2^62 in the year
4199, and the declaration holds the time on an act below half of that, 2^45
milliseconds, in the year 3084. A layer's last
stamp is read from the record at each decision, by the walk, in both gates, and
is not kept in memory:
that would be a cache, and the project's rule sends a cache to its own
examination first.

**The one text and the digest.** One function from anything to a string, blind to
what a serialiser may change and no check can see. The digest is SHA-256 over it,
kept whole. It leaves the time on the act out: a second send of one act under
one act-id carries another time, and both sends must read the one answer as
their own. The door takes it of the thing that it appends, and a gate of the
thing as it comes off its depot, each value plain; nothing between the two gives
a part another form. That a maker reads her answer as her own rests on Rama's
writing and reading of a record leaving the one text of a thing as it was;
[the proofs' map](../../../test-store/softland/store/README.md) says which
checks hold that.

**The form of a making act.** Exactly two facts, in either order: the depot on
the layer, and the act's own permission on its holder.

**The source of each gate.** It starts from the beginning of its depot, so no
thing that is in the depot is skipped whenever the topology first runs on a
partition. The
streaming gate's has the retry mode "all after", and nothing is given back in the
ack.

**The first argument of a query routes it.** Rama's client hashes the first
argument and sends the query straight to one task. For `depot-of-layer` it is
the layer. For `answer-by-act-id` it is the routing id of
the act-id, which is its layer, for both depots. The
caller still needs the act-id alone.

**One walk for both gates.** The decision of one thing is the same in both
gates but for the names of the tables and the gate's word. So it is written
once, as one Rama operation that is handed the four tables and the word
([walk.clj](module/walk.clj)), and each gate calls it. It gives back the rows
of an accept that a gate may copy to other tasks. The streaming gate does not
use them: a stream topology commits each task's writes apart, and a copy must
be committed with its answer or not at all.

**The walk never gives its task back.** Between its first read and its last
write stands no partitioner, no yield, no read that may yield and no append,
and none may be added. Rama runs other events of a task while one event waits
at such a place. A walk that waited there would let the next thing of the
partition be decided first, against a record that lacks this thing's rows. A
stand-in gate whose walk did yield decided things out of the depot's order and
refused them (the plan of the base's gate, claim 4). The cost: the walk holds
the task's thread for the whole of one act, and a read of the task waits that
long. No check of a gate looks for a yield at run time: it is a contract of the
walk's code.

**The time of a decision is the time on the act.** That is Sid's decision 1 of
13 October, above. The door reads the clock of its server once for a thing and
writes it under `:door-time`, in milliseconds since 1970
([door.clj](door.clj)); it is the one read of a clock on the way of an act
through the store. The rest is a session's, each for Sid to review:

- A refused thing moves no layer's time. The stamp of a refusal, whatever its
  word, is the layer's last stamp plus one, or 0 in a layer with no stamp yet:
  neither the time on the thing nor a stamp that it carries is in it. Only an
  act that a gate accepts puts a time into a layer's stamps (`stamp-of` in
  [decision.clj](gate/decision.clj)). One edge: after a last stamp whose
  counter is full, "last plus one" carries into the time by one millisecond,
  which takes 65,536 decisions in the layer in that millisecond. Why: anything can arrive on a depot.
  Were the time of a refused thing in its stamp, one thing with a time far
  ahead, from a sender with no permission, would set its layer's stamps ahead
  for good, and the 10-second rule would then refuse each honest act that
  carries a true stamp of that layer. The module's proof holds, in both gates,
  that a refused thing with a time one hour ahead gets the last stamp plus
  one.
- Where the time stands in the order of the reasons. A part that the form does
  not know, and a `:depot` that is no depot word, are told before a missing
  time; each of the maker's own parts after it. It follows from where the part
  stands in the shape ([declaration.clj](door/declaration.clj)).
- Two strays that differ only in the time are one stray. It follows from the
  digest, which leaves the time out for every thing alike.
- The 10-second rule holds a carried stamp against the time on the act, in both
  gates. With no usable time no carried stamp is too far ahead; such a thing
  has the declaration's word.

**What the time on the act is called is not decided.** Kept for now:
`:door-time`, a session's working name. The one place is `time-part` in
[declaration.clj](door/declaration.clj); the shape of what arrives, the door's
additions, the digest and `time-on` read the name from there. It is Sid's to
say. Every record of both depots holds the name ("What Rama cannot change once
real data is kept", below).

**What a second send of the same act carries as its time is not decided.** Kept
for now: the door's clock of the second send; the door does nothing special for
it. The digest leaves the time out, so both sends read the one answer as their
own, and a gate decides the first of the two to arrive, with the time of that
one. The one place is `send-act` in [door.clj](door.clj). It is Sid's to say.

**What happens to a record that arrives with no time is not decided.** Kept for
now: a gate refuses it `:no-door-time`, and the refusal is recorded under its
act-id, with a stamp of the layer's last plus one. The same word is for a time
that is nil, no fixed-width integer, below zero, or 2^45 or more. The one place
is the entry of the time's part in the shape `:arrived-act`, in `shapes` of
[declaration.clj](door/declaration.clj). It is Sid's to say.

**Where a time that is far ahead is stopped is not decided.** Kept for now:
nowhere but at the end of the range. The declaration holds the time to 0 or
more and below 2^45 milliseconds, and no check holds it against a clock. A
thing that anything appends with a time far ahead of every clock is accepted
when the rest of it is right and its sender may act in the layer, and each
later stamp of its layer is later than that time. A thing that is refused
moves nothing, whatever time it carries. The one place is `max-time` in
[declaration.clj](door/declaration.clj). It is Sid's to say.

Why the range ends at 2^45 and not at 2^46, where the packing reaches 2^62: an
act can carry no stamp of 2^62 or more, so a layer whose stamps reach 2^62 has
facts that no later act can name. With the time below 2^45 a stamp of the time
alone is below 2^61. The greatest stamp that an accepted act carries is 10
seconds ahead of its time, and that plus one is 655,360,000 above 2^61. From
there only "last plus one" raises a layer's stamps, one for each decision, an
accept or a refusal: about 2^61 decisions in one layer to reach 2^62. With the
range at 2^46 it would take one accepted act, with a time of 2^46 less one and
the greatest stamp of that millisecond carried.

**The stamp is counted in the layer.** That is Sid's decision 8 of 13 October,
above. A gate keeps one last stamp for each layer, and an act's stamp is the
greatest of three numbers: the time on the act with a counter of zero, the
layer's last stamp plus one, and the greatest stamp that the act carries plus
one. So a stamp is strictly increasing in its layer, in the order in which the
gate decided, and two layers can hold one stamp, also on one task: a layer's
stamps do not depend on which layers share its task or on how many tasks the
module has. Nothing of a batch is in a stamp. The time in a stamp is not always
the time on its act: an act whose time is not later than the time in its
layer's last stamp gets that stamp plus one. A session's, for Sid to review:
the key of a layer's last stamp is the text of the layer id, since a layer id
is a keyword or a UUID and a table's key has one class
([tables.clj](record/tables.clj)).

**Where each row of a microbatch layer lives.** Sid decided on 13 October that
the base's rows are a copy on every task and that one task decides (decision 4,
above). Which rows are copied, and where the rest lives, is the builder's.

- A fact's row and a row of the facts by value are on every task: the layer's
  task writes them in the decision, and the gate copies them to each other
  task. So a read of a microbatch layer's facts needs one task, whichever it
  is. The layer's depot fact, each permission, who may see the layer, what it
  builds on and its marked keys are facts, and are on every task with the rest.
- An answer and the layer's last stamp are on the layer's task alone. A
  decision reads both there, and a maker reads her answer there, as in a
  streaming layer: she has the act-id, which holds the layer.
- A stray's digest is on the task of the partition where the stray arrived.

What a copy costs, a session's: the layer is stored once on each task, an
accept is a write on each task, and a reader on another task sees a change a
moment later than a reader on the layer's own task.

**Where an act of a microbatch layer is decided.** On the layer's task, where
its depot partition is, one act after the other in the depot's order (decisions
3 and 4 of 13 October, above). The gate is built for the base: the layer that
the system provides to everyone, which few hands change and which does not need
to be instant. A microbatch layer that many people write into is not designed.
Whether such a layer is a layer apart from the base is not decided, and nothing
here is built for it.

**The deciding task is passed over in the copy.** It holds the rows already.
Its own copy would come back to it after it has decided later things of the
batch, and would set a key back to an older row until the later copy arrives. A
stand-in gate that did write its own copies showed such a row for a time (the
plan of the base's gate, claim 3). The pass costs one comparison.

**The word of an act that is sent to the wrong depot comes from the act
alone.** A session's rule, of 14 October 2026, for Sid to review; no decision
of his. A gate whose own record holds nothing for a layer that starts with the
other gate's depot answers `:layer-belongs-to-the-other-gate`, whether or not
the other gate has made the layer, and never `:layer-not-made`. What forces it:
a word that followed what the other gate had made by then would follow an order
across two depots, which no second run gives again. The choice of that word
over the other is the session's. `standing` in
[decision.clj](gate/decision.clj) is the one place.

**A thing that Rama runs again.** Each write of a decision stands behind check
(a), and the answer is written with the rest. A stream topology can process a
record again, also after its writes were committed: the second run finds the
answer and writes no row, so a layer's last stamp is not raised twice for one
thing. A microbatch topology sets its tables back when an attempt of a batch
fails, on every task, and the second attempt writes the same rows with the same
stamps and sends the same copies. A stray's write is an add to a set, the same
however often it runs. One limit follows for a gate that copies: the copies of
a decision and its answer must be committed together or not at all, which a
microbatch topology gives and a stream topology does not. A throw is no quiet
retry: Rama treats a throw in a gate's code as fatal for the worker whose
thread ran it, launches that worker again, and then tries again. The gates' own
code does not throw on what arrives. [walk.clj](module/walk.clj) and each
gate's namespace docstring say how far each of these was read in Rama's docs
and how far it was run.

**What the copy adds for one accepted act of the microbatch gate**, in a module
of N tasks. The act: one value under a plain key, on one entity, nothing named
to replace. Counted from the dataflow, not measured.

| | On the layer's task | On each other task | Messages |
|---|---|---|---|
| The key not marked | The walk: 5 reads, 3 row writes | 1 row write: the fact | One to each of the N tasks |
| The key marked | The walk: 6 reads, 4 row writes, or 5 | 2 row writes: the fact and the entry in. 3 when the entry of a value before goes out | One to each of the N tasks |

The copy reads nothing. For an accept that wrote c rows of facts and of the
facts by value it is c row writes on each of the N - 1 other tasks: 3 + (N - 1)
row writes in all for the first row of the table. Two costs are not in the
table: each message carries the c rows whole, so the bytes of an accept's rows
are sent to each other task; and each row of the base's facts is stored once
on every task, N times what a row of a streaming layer takes. A refusal, a thing under an
act-id that has an answer and a stray give no rows, and the gate then goes
nowhere.

**The placing of the checks that the contract does not place.** The making and
the counting of the values' texts is the last step of the declaration's check:
it is the most costly. The time on the act is a part of the shape of what
arrives, checked behind `:depot` and before each of the maker's own parts. The 10-second
rule comes after the declaration, before any row is read. What a store key does
not take comes after check (g). A
check that examines facts takes them in the act's order.

**How each of the builder's rules is met.** The numbers are the contract's.

1. Each answer keeps the SHA-256 of its thing. A second, different act under a
   used act-id ends at check (a) and writes nothing; its sender reads an answer
   whose digest is not the digest of what it sent.
2. A fact that names nothing to replace reads no row for a check and is written
   as the row of its place. No check can call it stale. Under a marked key the
   gate reads its place all the same, after the last check, for the order by
   value; that read refuses nothing.
3. A retract is written as the row of its place, with no value. A later fact that
   names it passes check (e).
4. The module sets no trimming option on either of its two depots.
5. The module makes no lock. No topology of the module appends to a depot: each
   depot holds what clients appended, and nothing else.
6. Two acts of one microbatch layer that replace one fact are decided one after
   the other on the layer's task, in the depot's order: the first wins, on
   every attempt of a batch and in every run again. An act is decided once, on one task, with
   each row that it needs there; the rows of an accept go to every task inside
   one attempt, which Rama keeps or drops whole. Every act of a microbatch
   layer passes one task, the layer's. The rule says not to do that unless the
   plan shows that it must. Sid decided it on 13 October 2026 ("One task
   decides, in the line's order"), and the plan of the base's gate shows what
   forces it: Rama gives an order inside one partition only, and an order that
   a gate makes of its own between partitions is one that a run again does not
   give.
7. A gate tells which gate a layer belongs to from the layer id: the depot that
   the layer starts with (`starts-with-the-other-gates-depot?` in
   [first_act.clj](gate/first_act.clj)). No act of this build changes a depot,
   so a layer stays where it starts, and no gate reads the other gate's record.
   A layer's depot is also a fact, at a place found from the layer id; in a
   microbatch layer every task holds it. An act's answer is found from its
   act-id alone: its routing id is its layer, for both depots.
8. Each word is under "Words a session made", below.
9. Found: yes. Each act of a layer is on one partition of its depot, whatever
   its entities, and the gate decides the things of a partition in the depot's
   order. A second run of the gates over the two depots decides them in that
   order again, so the same act wins. The proof
   `softland.store.run-again-proof` holds it: a second module over mirrors of
   the two depots, with every row of every table compared.
10. The test for a stray runs before check (a). A stray gets no answer; its digest
    is added to the set of its word, on the task where it arrived, once however
    often it comes. The count is the size of the set. In both gates that is the
    task of the partition where the depot put the thing. Two strays that differ
    only in the time on them have one digest, and are one.
11. The answer's row keeps the digest and `answer-by-act-id` gives it.
    [digest.clj](door/digest.clj) is the one function. The gate writes nothing for a
    second, different act.
12. Two rules of the declaration: `:place-named-once` and
    `:no-fact-replaces-a-fact-of-its-act`.
13. A second write on a permission's place, each retract under a store key, and a
    `:softland.store/depot` fact after the making are refused, with four words
    that say which. A permission fact sits on its holder. The three keys of the
    read build take a second write and any later one: who may see a layer, what
    it builds on, and its marked keys.
14. `granted-in` must be the act's layer, in a first permission and in one given.
    The gate checks nothing more about a holder; see "What the gate does not
    check".
15. A refusal writes the answer and the layer's last stamp, and nothing else,
    in both gates.
16. The gate keeps the one text of each value in the fact's row. The text of a
    value of a refused act is dropped with the decision.
17. An act that carries a stamp more than 10 seconds ahead of the time on the
    act is refused, before any stamp is made, in both gates. The rule says "the
    gate's wall clock", so that a forged stamp cannot push a task's stamps to
    the end of their range. No gate has a clock: the stamp is held against the
    time that arrived on the act. So the rule holds a carried stamp to the
    act's own time, and nothing holds that time to a clock: a thing that is
    appended with no door can carry a time far ahead and a stamp beside it.
    Such a thing moves its layer's stamps only when a gate accepts it: the
    stamp of a refusal is the layer's last stamp plus one.
    "Where a time that is far ahead is stopped is not decided", above, says
    what is kept.
18. A microbatch act has one stamp, on its answer and on each of its facts, on
    every task that holds a copy. The stamp is later than the last stamp of
    the act's layer, and a layer's rows reach each task in the order of the
    decisions; so on each task where the act writes, its stamp is later than
    each stamp that the layer has there. A stamp is counted in its layer and
    not on a task ("A number counted per layer", Sid, 13 October 2026): the
    rule's "last stamp of each task" is read as the layer's last stamp.
19. The store has no rebuild: no code makes a row from a log of what a gate
    decided, and no depot holds such a log. The record is made again by a
    second run of the gates. `add-gates-and-reads` in [module.clj](module.clj)
    adds everything of the module but its two depots; a module that has a
    mirror of each depot under the depot's name, and calls it, decides every
    thing again from the beginning of each depot, into tables of its own. It
    gives the same tables because a decision is made of the thing and of the
    gate's own rows, in the order of the layer's partition: the time is on the
    act, the stamp is counted in the layer, and the word of an act for a layer
    of the other gate comes from the act alone. The proof
    `softland.store.run-again-proof`
    ([the proofs' map](../../../test-store/softland/store/README.md)) runs a
    second module over mirrors of the two depots and compares every row of
    every table. It also feeds a kept set of acts to a fresh store, with
    another number of tasks too, and compares the tables with the kept ones.
    What it rests on, and what is not built: the second module has the number
    of tasks of the first, since a module that mirrors a depot with another
    number of tasks does not get a layer's partition on the task that its own
    hash of the layer gives; how a store with real data moves to another
    number of tasks is not designed; and the exchange of a running store's
    tables for tables that were made again is not built. The order of a
    streaming layer's acts after a failure has its own section, above. For a
    change in what a gate accepts, Sid's decision 6 of 13 October 2026 is the
    rule: "A change in what a gate accepts is a new version of the act. The
    old version's rules stay in the code. This starts with the first real
    record. Until then the fixture is made again only when a rule is changed
    on purpose, and the receipt says so. In this build it means the rule on
    the page and the fixture; the code still knows one version." So the rule
    stands here and with the proof's fixture, and no code holds two versions
    of a rule.
20. Eight numbers in [declaration.clj](door/declaration.clj). Five are held by a
    rule or by the test for a stray, before a row key is built from an id. Two
    are held by the last step of the check: the bound on one value, counted of
    its text, and the bound on one act. One is held by the shape: the time on
    an act is below 2^45 milliseconds. The two of the read build are among the
    five: a layer builds on 64 layers at most, and marks 32 keys at most.
21. The aims: 1,000 acts a second on one task; 20 ms from the door's append to the
    answer in the door's hand. The contract's third is of the bytes of a lock
    for each value, and the store keeps no lock. Neither of the two is
    measured on a cluster with a disk and a network. The measures of the read
    build time a gate under 100 acts a second on the in-process cluster, with
    every row in memory ([the proofs' map](../../../test-store/softland/store/README.md),
    "The measures"); that is a floor for a round trip and no measure of an aim.
    By the cost of a cold read, the 5 reads of one accepted act give
    384 to 616 acts a second on one task, and the 6 of an act under a marked key
    320 to 513; the first aim is met only when the rows are read from memory.
    For the microbatch gate: 1,000 acts a second arriving
    on one depot partition, which needs a run of the gate inside a second; and an
    answer readable when the batch that decided the act is committed on the
    layer's task, not 20 ms, because a microbatch layer is not
    for what a person does directly. Each act of a microbatch layer is decided
    on the layer's task with the reads of the walk, so what the cost of a cold
    read gives above bounds it too. None is measured.

## Words a session made, Sid's to change

Each says what the thing is. None comes from the rig's code. Carried from the
contract and the decisions, not made here: `:act-id`, `:depot`,
the two store keys of 8 October, `:system`, `:base`,
`:streaming`, `:microbatch`, `:accepted`, `:refused`; "stray", "place", "holder",
"making act". "Act-id" is Sid's to change, and each word below that holds it
follows his word. "Depot" is his word, of 9 October: the contract and his
decisions before that day say "class" for it. "Members" and "everyone" are his
words, of 10 October. "See" and "see and act" are a session's; he said "yes"
on 12 October to a sentence that holds them. The keywords made of the four are
a session's.

The store keys carry the namespace `softland.store`. The two of 8 October are
`:softland.store/depot` and `:softland.store/permission`; the three of the read
build are in the table
below. Sid decided on 9 October that the store's keys
get a namespace (K4 B), and gave the words: `softland.store` before the slash,
and "depot". So the bare words are the tools'. A fact of a tool under `:depot`
or `:permission`, or under one of those words in another
namespace, is a fact under a plain key: the store
reads nothing from its value. The store owns the whole of its own namespace. He was
asked that day whether a gate should refuse a key in `softland.store` that the
store does not know, and said "yes"; such a fact is refused
`:store-key-not-known`. The namespace is taken exactly: a key in
`softland.store.x` is not in it. That reading is a session's.

**The words of the read path** are on [its own page](read_path/README.md): its
calls, the words of a query, what an answer holds, and its eight words.

**The words of the read build, for what the gates keep.** Each is a session's.
Two of them are also names in the rig's code, `:by-value` and `value-digest`;
neither was taken from it: "by value" is the contract's phrase for the one more
order, and "digest" is this store's word since its module.

| Word | What it is |
|---|---|
| `:softland.store/who-may-see` | The store key of a layer's setting: who may see the layer |
| `:members`, `:everyone` | Its two values: whoever holds a permission in the layer, or everyone |
| `:softland.store/builds-on` | The store key of the layers that a layer builds on, in the writer's order |
| `:softland.store/marked-keys` | The store key of the keys that a layer marks |
| `:see`, `:see-and-act` | The fifth slot of a given permission: what it allows |
| the facts by value | The one more order: for each marked key, each fact that has a value, by the key, the key's stamp, the value's digest and then the entity |
| value digest | The SHA-256 of a value's bytes, as 64 hex characters: the value's text in that order |
| the run of a value | The neighbouring keys of the facts by value that hold one value under one key: what points at that thing |
| the row before | The row that a place held before a fact was written there |
| `allows` | In [act.clj](act.clj): a permission → `:see` or `:see-and-act` |
| `starts-with` | In [first_act.clj](gate/first_act.clj): what a layer reads as under one of the three keys while no fact says it |
| `who-may-write-fault`, `marked-keys-place`, `reads-marked-keys?`, `since-by-place`, `rows-before-needed` | In [decision.clj](gate/decision.clj): who may write the three keys, and what a gate reads for the order by value |
| `plain-bytes`, `plain-bytes-of` | In [rows.clj](record/rows.clj): the UTF-8 bytes of the String that a fact's row holds for its value; and those bytes of a row's value |
| `holds-value?`, `value-of` | In [rows.clj](record/rows.clj): whether a row's value is the one text of a value given, and the first form that a row's value reads as |
| `read-value`, with `:no-fact`, `:nothing-here`, `:value`, `:not-a-value` | In [rows.clj](record/rows.clj): what a row says of its value, for a reader |
| `who-of`, `permission-of`, `marked-keys-of` | In [rows.clj](record/rows.clj): a row read back |
| `with-level-since`, `by-value-key`, `by-value-run`, `by-value-row`, `by-value-entity`, `value-digest-of`, `id-of-text`, `place-of-key` | In [rows.clj](record/rows.clj): the keys and the rows of the order by value, and a key back to its parts |
| `second-write-keys`, `max-builds-on`, `max-marked-keys` | In [declaration.clj](door/declaration.clj): the keys that take a second write, and the two limits, 64 and 32 |

**The words of the build before real data is kept, for a value and its
bounds.** Each is a session's. None was taken from the rig's code; whether the
rig's code holds one of them by chance is not known.

| Word | What it is |
|---|---|
| `:text` | In a fact of the act's one form, as the declaration's check gives it to a gate: the one text of the fact's value in its normal form, which the fact's row holds. Beside it `:v` holds the value under a store key, and nil under a plain key |
| `with-texts` | In [declaration.clj](door/declaration.clj), private: the last step of the check, which makes each text once and holds the two bounds |
| `max-value-bytes` | In [declaration.clj](door/declaration.clj): the most UTF-8 bytes of the one text of a value under a plain key. 65,536 |
| `max-act-bytes` | In [declaration.clj](door/declaration.clj): the most bytes of text that an accept of one act writes into the rows of its facts. 18,874,368 |
| `:value-too-large`, `:act-too-large` | The two reason words of that step; each is in the list of the declaration's reason words, below |

The door's words of that build, the namespace `softland.store.door.wait` among
them, are in [door/README.md](door/README.md).

**The words of the time on the act, of the walk and of the copy.** Each is a
session's, of 13 and 14 October 2026. None comes from the rig's code. "A layer
is one line" and "run again" are Sid's words, of 13 October. "Batch" and
"attempt" are Rama's.

| Word | What it is |
|---|---|
| the time on the act; `:door-time` | The part that the door adds to a maker's thing beside `:act-id` and `:depot`: the clock of the server that runs the door, in milliseconds since 1970. `:door-time` is a working name, kept for now and not decided |
| `time-part` | In [declaration.clj](door/declaration.clj): the one def that holds the name of that part |
| `max-time` | In [declaration.clj](door/declaration.clj): the time on an act is below it. 2^45 milliseconds |
| `usable-time` | In [declaration.clj](door/declaration.clj): a thing as it arrived → its time when the shape takes it, or nil |
| `:no-door-time` | The reason word of a thing whose time is absent or outside the range; in the list of the declaration's reason words, below |
| `time-on` | In [decision.clj](gate/decision.clj): the time of a decision, which is the usable time of the thing |
| `starts-with-the-other-gates-depot?` | In [first_act.clj](gate/first_act.clj): a gate's depot and a layer id → whether the layer starts with the other gate's depot. `layer-is-the-other-gates?`, private in [decision.clj](gate/decision.clj), asks it of an act |
| walk | The decision of one thing, from the test for a stray to the last write, on one task. In code the namespace `softland.store.module.walk` |
| `decide` | In [walk.clj](module/walk.clj): the walk as one Rama operation, which both gates call |
| copy; `copies-of` | A row of an accept that the microbatch gate writes on every task but the deciding one: a fact's row, or a row of the facts by value. `copies-of` in [walk.clj](module/walk.clj) takes them from what a decision writes |
| the layer's task | The task of the layer's partition in its depot: the task that the hash of the layer id gives |
| deciding task | The task on which the walk decided a thing. For a thing that its depot placed, the layer's task |
| a layer's line | The acts of a layer in the order of the layer's partition of its depot |
| `add-gates-and-reads` | In [module.clj](module.clj): adds everything of the module but its two depots, so that a second module over the same depots runs the same gates and reads |
| run again | A second run of the gates over the two depots from their beginning, by a second module, into tables of its own |
| out of its line | Said of an act whose decision is not in the order of its layer's partition; "The order of a layer's acts after a failure" gives the rule |
| routing id | The first argument of `answer-by-act-id`: the layer of the act-id, for both depots. `routing-id` in [rows.clj](record/rows.clj) |
| `server-clock`, `parts-added`, `without-the-time` | Private names of the door's code, in [door.clj](door.clj), [additions.clj](door/additions.clj) and [digest.clj](door/digest.clj): the read of the server's clock, the three parts that the door adds, and a thing with its time taken out for the digest |

**What Rama cannot change once real data is kept.** Read in Rama's docs and not
run: Rama cannot change the name of a depot or of a PState, the keys of a
PState, or the number of tasks of a module. In a tree that holds no real data
each is one word to change. So these, of the words above, are Sid's to change
before real data is kept and not after. What breaks is read from the code, and
not run.

| What the build made | Why it cannot be changed later | What breaks if it is changed then |
|---|---|---|
| The name `:door-time` of the time on the act | A record stays in its depot as it was appended, and the build writes no migration of a depot | Every record that was kept holds the old name. A gate that reads a new name finds no time on such a record, and a run again refuses it `:no-door-time` where the first run decided it with its time |
| The keys `:last-stamp` and `:strays` of `$$streaming-task` and `$$microbatch-task`, and under `:last-stamp` the text of a layer id | The keys of a PState | A gate that reads a new key finds no last stamp for a layer, and nothing then holds the layer's next stamp above the stamps that its rows have |
| Where a record sits: each depot places a thing by its `:layer` | A record stays on the partition where it was appended | A layer's line is one partition of its depot. A record that is placed another way is not in one line with the records that were kept |

The same limit of Rama holds of each depot and PState under "Names in Rama".

**Names in Rama.**

| Word | What it is |
|---|---|
| `Store` | The Rama module |
| `:store-module` | The alias of the root `deps.edn` that holds the store's paths, Clojure and Rama |
| `*streaming-depot` | The streaming depot: every thing sent for a streaming layer |
| `streaming-gate` | The stream topology that decides each thing of the streaming depot |
| `$$streaming-answers` | The answer under each act-id of a streaming layer |
| `$$streaming-facts` | The current fact of each place of each streaming layer |
| `$$streaming-facts-by-value` | For each marked key of each streaming layer, the same facts in the order of their values |
| `$$streaming-task` | What the streaming gate keeps for one task: the last stamp of each layer, and its strays |
| `*microbatch-depot` | The microbatch depot: every thing sent for a microbatch layer |
| `microbatch-gate` | The microbatch topology that decides each thing of the microbatch depot, and copies the rows of an accept's facts to every task |
| `$$microbatch-answers` | The answer under each act-id of a microbatch layer, on the layer's task |
| `$$microbatch-facts` | The current fact of each place of each microbatch layer, the same rows on every task |
| `$$microbatch-facts-by-value` | For each marked key of each microbatch layer, the same facts in the order of their values, the same rows on every task |
| `$$microbatch-task` | What the microbatch gate keeps for one task: the last stamp of each layer, and its strays |
| `answer-by-act-id` | The query: act-id → the recorded answer, or nothing |
| `depot-of-layer` | The query: layer → its depot, or nothing |
| `StoreAgain` | The second module of the run-again proof: a mirror of each of the two depots under the depot's own name, and `add-gates-and-reads`. No part of the module `Store` |

**Folders and namespaces.** Five folders: `door`, `read_path`, `gate`, `record`,
`module`.
Each namespace is `softland.store.`, its folder, and the word: `door.text`,
`door.digest`, `door.declaration`, `door.additions`, `door.wait`,
`door.words`; `gate.decision`,
`gate.first-act`, `gate.stamp`; `record.rows`, `record.tables`;
`module.walk`, `module.streaming-gate`, `module.microbatch-gate`,
`module.door-reads`; the five of `read-path` are on its own page. Four
are at the top with no folder: `act`, `module`, `door` and `read-path`. "Rows" and "tables" are
the words for the record's two files: what a gate writes and reads, and the
PStates that hold it. Each folder's map says what its files are. In `act.clj`,
one name is a session's: `as-shape`, which gives the act's engine to another
namespace. The proofs are `act-proof`, `pure-proof`, `module-proof`,
`door-proof`, `read-path-pure-proof`, `read-path-proof` and
`run-again-proof`; `read-measures` and
`gate-measures` are runs that are no proof. The words that the door's build made are in
[door/README.md](door/README.md).

**Fields of a row, and of an answer as a caller gets it.**

| Word | What it is |
|---|---|
| `:refused` | In an answer's row: the reason word. Absent when the act was accepted |
| `:stamp` | In an answer's row, a fact's row and a row of the facts by value: the stamp of the decision |
| `:digest` | In an answer's row: the digest of the thing that was decided |
| `:value` | In a fact's row: the fact's value, plain, as a String: the one text of the value in its normal form. Absent for a retract |
| `:by-act`, `:by-index` | In a fact's row, and in a row of the facts by value: the fact that wrote the row, as the uuid of its act and its index |
| `:who`, `:permission` | In a fact's row, and in a row of the facts by value: who wrote the fact, and the permission that the act named, each as its one text |
| `:level-since` | In the row at a layer's marked-keys place only: the one text of a map from each marked key to the stamp since which its order by value is level |
| `:last-stamp` | In the task's value: a map from the text of a layer id to the last stamp that the gate gave in that layer. A layer of which the gate has decided nothing on the task has no key |
| `:strays` | In the task's value: by reason word, the digests of the strays seen |
| `:verdict`, `:reason` | In an answer as `answer-by-act-id` gives it: `:accepted` or `:refused`, and the reason word |

**The words of a stray.** A stray has no answer; its digest is kept under its
word.

| Word | What it is |
|---|---|
| `:no-act-id` | The thing is no plain map, or holds no act-id |
| `:act-id-malformed` | Its act-id is not `[layer depot uuid]` |
| `:id-too-long` | The layer slot of its act-id prints to more than 256 characters. The same word is a reason word of the declaration, for every other id |
| `:act-id-of-the-other-gate` | The depot slot of its act-id is not the depot of the gate where it arrived |
| `:act-id-of-another-layer` | The layer slot of its act-id is not its own `:layer` |

**The reason words of the declaration**, beside `act.clj`'s seven.

| Word | What it is |
|---|---|
| `:depot-not-the-act-ids-depot` | `:depot` is absent, is no depot word, or is not the depot slot of the act-id |
| `:no-door-time` | The time on the act, under `:door-time`, is absent, nil, no fixed-width integer, below zero, or 2^45 milliseconds or more |
| `:too-many-facts` | More than 256 facts in one act |
| `:too-many-based-on` | More than 1,024 entries of `:based-on` |
| `:store-key-not-known` | A fact's key is in the namespace `softland.store` and is not one of the store keys |
| `:depot-fact-not-a-depot-word` | A `:softland.store/depot` fact's value is not `:streaming` or `:microbatch` |
| `:permission-fact-not-a-permission` | A `:softland.store/permission` fact's value is not a permission id |
| `:permission-fact-not-on-its-holder` | A `:softland.store/permission` fact's entity is not the holder that its permission names |
| `:who-may-see-fact-not-members-or-everyone` | A `:softland.store/who-may-see` fact's value is not `:members` or `:everyone` |
| `:builds-on-fact-not-a-list-of-layers` | A `:softland.store/builds-on` fact's value is not a vector of ids |
| `:builds-on-too-many-layers` | A `:softland.store/builds-on` fact names more than 64 layers |
| `:marked-keys-fact-not-a-set-of-keys` | A `:softland.store/marked-keys` fact's value is not a set of keys |
| `:too-many-marked-keys` | A `:softland.store/marked-keys` fact holds more than 32 keys |
| `:marked-key-in-the-stores-namespace` | A `:softland.store/marked-keys` fact marks a key in the namespace `softland.store` |
| `:place-named-twice` | Two facts of the act are on one entity and key |
| `:replaces-own-fact` | A fact names a fact of its own act in `:replaces` |
| `:id-too-long` | An id that the act holds, or a fact's key, prints to more than 256 characters |
| `:value-too-large` | The one text of a value under a plain key holds more than 65,536 bytes |
| `:act-too-large` | The texts that an accept of the act would write into the rows of its facts hold more than 18,874,368 bytes |

**The names in the declaration.** Its shapes: `:arrived-act`,
`:depot-word`, `:who-may-see-word`, `:layer-list`, `:key-set`.
Its rules, each named for what must hold: `:depot-is-the-act-ids-depot`,
`:facts-within-the-limit`, `:based-on-within-the-limit`, `:store-key-is-known`,
`:depot-fact-holds-a-depot-word`,
`:permission-fact-holds-a-permission`, `:permission-fact-sits-on-its-holder`,
`:who-may-see-fact-holds-its-word`,
`:builds-on-fact-holds-layers`, `:builds-on-within-the-limit`,
`:marked-keys-fact-holds-keys`, `:marked-keys-within-the-limit`,
`:marked-keys-are-plain-keys`, `:place-named-once`,
`:no-fact-replaces-a-fact-of-its-act`,
`:ids-within-the-limit`. In the table of store keys: `:why`, `:value`,
`:sits-on`, `:setting`, `:written-again`, and the two words that say where a
fact sits.

**The reason words of the gate.**

| Word | What it is |
|---|---|
| `:carried-stamp-too-far-ahead` | The act carries a stamp more than 10 seconds ahead of the time on the act |
| `:layer-belongs-to-the-other-gate` | The record holds nothing for the layer, and the layer starts with the other gate's depot: the act was sent to the wrong depot. The same word whether or not the other gate has made the layer |
| `:layer-not-made` | The record holds nothing for the layer, the layer is not the other gate's, and the act is not a right making act |
| `:who-not-the-permissions-holder` | The holder that the act's permission names is not the act's `:who` |
| `:permission-does-not-stand` | The record does not hold the act's permission for its holder |
| `:permission-does-not-allow-acting` | The record holds the act's permission, and it says `:see`: an act needs "act" |
| `:replaced-fact-not-current` | A fact that the act replaces is not the current fact of its place |
| `:setting-not-on-the-layer` | A fact under a setting key is on an entity that is not the act's layer |
| `:permission-fact-for-another-layer` | A `:softland.store/permission` fact writes a permission for a layer that is not the act's |
| `:permission-fact-granted-in-another-layer` | A `:softland.store/permission` fact writes a permission whose `granted-in` is not the act's layer |
| `:permission-fact-not-beneath-the-acts-own` | A `:softland.store/permission` fact writes a permission that is not straight beneath the one the act acts under |
| `:ending-a-permission-not-decided` | A `:softland.store/permission` fact with no value: who may end a permission is not decided |
| `:changing-a-depot-not-decided` | A `:softland.store/depot` fact, in a layer that is made, with a depot that is not the layer's |
| `:store-key-takes-one-write` | A permission fact on a place that already holds one, or a depot fact with the layer's own depot after the making. Not earned by the three keys that take a second write |
| `:store-key-retract-not-built` | A fact with no value under any store key but a permission's |

**Words between the decision and the walk**, for the rows of one decision:
`:fact`, `:by-value`, `:answer`, `:last-stamp`. A row of
the kind `:by-value` that is nil says that its key is to be taken out. The
first two kinds are what the walk gives a gate to copy. And for
what the walk hands `writes` for an accept:
`:since-by-place`, `:place-rows`, `:level-from`.

**Marks in the one text**: `#bytes`, `#double`, `#keyword`, `#other`,
`#too-deep`, `#unreadable`. [text.clj](door/text.clj) says what each marks.
