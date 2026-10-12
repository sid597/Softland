# Store — the act, the store's module, the door, and the read path

[Parent: namespace root](../README.md) · [Store source entry](../../README.md).

This folder holds four things. The act: the one form of what a maker gives to
the store, and the check of that form. The store's Rama module, `Store`: the two
depots of acts, the gate of each, the record that each gate keeps, what keeps the
clock of each batch of the microbatch gate, and the two reads by
which the door reads the module. The door: the client code that takes a
maker's act into a depot and reads its answer back. And the read path: the
client code by which a fact comes back out to a caller, once or kept live. An
act that a gate accepts becomes facts in the record; its answer is read back by
act-id, and its facts through the read path.

A layer has a depot, streaming or microbatch: the depot that takes its acts,
whose gate decides them. The streaming gate decides each thing in one event, on
the layer's task. The microbatch gate decides the things of a batch, across
tasks. The base is the one layer that starts as a microbatch layer; every other
layer starts streaming.

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
| [door.clj](door.clj) | The door: it opens on the module, gives act-ids, appends each act as its maker gave it, with an act-id and the layer's depot added, and reads the answer back by act-id. It is a client of the module, and checks nothing of an act. | None: it keeps nothing between two calls. It borrows four handles of Rama's client and holds one number, fixed at its opening. |
| [door/](door/README.md) | What arrives on a depot, as the door hands it over and a gate checks it: the door's declaration, the one text of a thing and its digest. And the door's own pure parts: what it makes of one act, the numbers of a wait, and its words. | None. |
| [read_path.clj](read_path.clj) | The read path: it opens on the module, reads a query once or keeps it live, and says when an asker left. It is a client of the module: it selects and watches the four tables of facts, and reads nothing else. It writes nothing. | Who looks at what, what each asked for, and the pushes that they share, with a count of them: the memory of one process, empty at each opening. It borrows four handles of Rama's client. It holds no row and no value. |
| [read_path/](read_path/README.md) | The rules of a read, as pure code: the form of a query and its check, the layers of a read and who may see each, the sources that a read asks of Rama, the answer, and the read path's words. | None. |
| [gate/](gate/README.md) | What a gate decides with, as pure code that both gates call: the checks of one act against rows of the record, the first act of a layer, the stamp, and the steps of one batch of the microbatch gate. | None. It reads no PState and no clock. |
| [record/](record/README.md) | What the record is made of: its rows, the keys of its two orders of facts, and the schemas of the PStates that hold them. | None. It holds no record and declares nothing. |
| [module/](module/README.md) | What runs in Rama beside the depots: the two gates' topologies, the topology that keeps a batch's clock, and the two reads. | Each gate owns the PStates of its record, and the topology that keeps a batch's clock owns one PState; all durable in the cluster. |
| [module.clj](module.clj) | The module `Store`: the two depots of acts and the depot of batch clocks, and the calls that add the two gates, the topology that keeps a batch's clock, and the reads. | It owns the three depots, durable in the cluster and never trimmed. |

[act.clj](act.clj), [door/](door/README.md), [read_path/](read_path/README.md),
[gate/](gate/README.md) and the rows of [record/](record/README.md) are pure code
and load with Clojure alone. [door.clj](door.clj), [read_path.clj](read_path.clj),
[module.clj](module.clj), [module/](module/README.md) and the schemas of
[record/](record/README.md) require Rama.

Each folder's map says what its files are. The sections below are about the
module as a whole: how a thing passes through these parts, what a gate does not
check, the decisions and the builders' choices, and the words.

## How the parts relate

The two gates share the pure code. Both decide with [declaration.clj](door/declaration.clj),
[digest.clj](door/digest.clj), [decision.clj](gate/decision.clj),
[first_act.clj](gate/first_act.clj), [stamp.clj](gate/stamp.clj) and
[rows.clj](record/rows.clj). Where what a function gives depends on the gate, it
takes the gate's depot: the test for a stray, the form of a making act, each
check against the record. No check is written twice, and the two records have
one shape of row.

**A thing for a streaming layer** is appended to the streaming depot, which puts
it on the task of its `:layer`. Every row of a streaming layer is on that task.
The streaming gate takes the thing there, in one event:

1. [declaration.clj](door/declaration.clj) tests it for a stray. A stray gets no
   answer; the gate keeps its digest.
2. The gate reads the answer under the thing's act-id. If one is there, the event
   ends. A repeat is answered, not decided twice.
3. [digest.clj](door/digest.clj) takes the digest of the thing, over its one text
   ([text.clj](door/text.clj)). [declaration.clj](door/declaration.clj) checks the thing; it
   reads its shapes with [act.clj](act.clj)'s engine, and in its last step makes
   the one text of each value and holds the bound on one value and on the act.
4. [decision.clj](gate/decision.clj) checks the act against rows that the gate reads
   for it: its standing ([first_act.clj](gate/first_act.clj) knows the form of a making
   act), which also asks what the act's permission allows; and its places.
5. Only for an act that no check refused, and only when it holds a fact under a
   plain key or under the layer's marked keys: the gate reads the row of the
   layer's marked keys, and the row before of each place under a marked key.
6. [stamp.clj](gate/stamp.clj) gives the stamp. [decision.clj](gate/decision.clj) gives the
   rows to write, each made by [rows.clj](record/rows.clj), and the gate writes them
   to the PStates whose schemas [tables.clj](record/tables.clj) gives. A fact's row
   holds the text of its value, who wrote it and the permission that the act named. For
   a fact under a marked key the gate also writes its row in the facts by value,
   and takes the row of the value before out.

**A thing for a microbatch layer** is appended to the microbatch depot, which
puts it on the task of the entity of its first fact
([batch.clj](gate/batch.clj) derives it). The rows of a microbatch layer are on
several tasks, and three kinds of row have a task of their own: a fact on the
task of its entity, an answer on the task of its act-id's uuid, and a row of
the facts by value on the task of its value's digest. Five facts are on every task: the
layer's depot, each permission, who may see the layer, what it builds on, and
its marked keys. So no one task holds all that an act
needs, and the microbatch depot holds no order between two partitions. The
microbatch gate decides the things of a batch in four steps. Each step is done
on every task before the next starts:

1. Arrive and question. On the task where it arrived, each thing gets its turn,
   its number in the batch. [declaration.clj](door/declaration.clj) tests it for a
   stray and checks it. The gate reads the permission of its `:who` from the
   task's own copy. The act sends a question to the home task of each row that
   it needs.
2. Reply. One task takes the batch's clock: the one that is kept for the batch,
   or, when none is, a reading that it keeps first. It gives the batch its first
   stamp.
   Each home task reads the marked keys of each layer that was asked about, once
   for a layer, and then each row that was asked for, once, and replies. A reply
   about a place under a marked key carries the key's stamp, and the row before
   for an asker that does not have the place as its own. The reply about an
   answer is check (a): the earliest turn under an act-id has it.
3. Decide. Each task takes its acts in the order of their turns. An act that
   needs no row that an act of another task needs is decided there, by
   [decision.clj](gate/decision.clj)'s checks in their order. Acts that meet over a
   row are sent to one task, their layer's.
4. Write and meet. Each task writes the rows that it was sent; a row of the
   facts by value that is nil takes its key out. The layer's task decides the
   acts that meet, one at a time in the order of their turns.

[batch.clj](gate/batch.clj) holds each pure part of those steps;
[microbatch_gate.clj](module/microbatch_gate.clj) walks them.

**Each gate asks the other's record one thing**: whether it holds a layer of
which its own record holds nothing. The streaming gate reads the base's depot
row in the microbatch record. The microbatch gate reads a streaming layer's
depot row in the streaming record. So an act sent to the wrong depot is refused
as an act into a layer of the other gate.

**The door reads** through [door_reads.clj](module/door_reads.clj), by two reads: the
answer under an act-id, and the depot of a layer. Each read goes
straight to one task: the layer's task for a streaming act-id and for a layer's
depot, the task of the uuid for a microbatch act-id. A read of a microbatch
layer gives a row only when its batch is closed; "Engineering choices" says why.

**The read path reads** the gates' tables straight: the facts and the facts by
value of each gate, with a partitioning key for each read, once by a select and
kept live by Rama's push. It calls no query of the module, and a row of a
microbatch layer is not held to the closing of its batch for it:
"A direct read of the tables" says what such a read is given.
[read_path/](read_path/README.md) says how a read is put together.

The streaming gate's dataflow only reads rows, calls the pure functions and
writes rows. The microbatch gate's dataflow does one thing more, for a batch's
clock: when none is kept for the batch it reads the clock, appends one record to
the depot of batch clocks, and throws in the one case where the clock is then
not kept. [batch_clock.clj](module/batch_clock.clj) keeps the clock; "The clock
of a batch", under "Engineering choices", says how.
A pure function gives the same answer when Rama runs an event or a batch again,
never throws on what arrived, and is proven without a cluster.

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
- **Where a thing sits, on the streaming depot.** The depot's partitioner puts a
  thing on its layer's task, and the streaming gate makes no hop. Every client's
  append goes through that partitioner. A topology can append to the partition of the task that it is on,
  whatever the partitioner; no topology of this build appends to the streaming
  depot. The one append that a topology makes is the microbatch gate's, of a
  batch's clock, to the depot of batch clocks. A thing that a
  topology appends to another partition is decided against that task's record. It
  can be refused as an act into a layer that is not made, and the answer of that
  refusal is then on that task, where no read by its act-id looks. Or, when it is
  a making act, it can be accepted there and leave rows and an answer on that task
  that no read finds. The rows of the true layer, on its own task, are not
  touched. The microbatch gate reaches every row by a partitioner, so a thing on
  another partition of its depot is decided against the true rows; only its turn
  takes the partition where it sits.
- **The order after a failure, in the streaming gate.** Without a failure the acts
  of a layer are decided in the depot's order. When something throws while a batch of events runs, that
  batch is discarded, and a later act of the layer can be decided before the
  failed one is retried. The gate's own code does not throw on what arrives, so
  this needs a fault of the machine, of Rama, or of this module. Rama's setting
  `topology.stream.max.executing.per.task`, at 1, closes the window: a task then
  has one record pending at a time. The cost is one act in each batch of a task,
  so each act waits for the replication of the act before it; that is paid against
  the target of 1,000 acts a second on one task. The module leaves the setting at
  Rama's default. The choice is Sid's.
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
  is. One value that very many places hold is one run on one task, without a
  bound; in the base that is the task of the value's digest, so a key whose
  values are few puts its writes on few tasks. Not decided.
- **The facts of a key from before its mark.** The order by value holds the facts
  that are written while the key is marked. Nothing is filled from before, and no
  event or batch walks a layer. The row of the marked keys says since which stamp
  each key's order is level. When a mark is taken off and put on again, the rows
  of the earlier marking stay where they are, for ever, and no read names them.
- **One moment for a fact and its row of the order by value, in the base.** The
  two are on two tasks, and Rama shows a batch task by task. In a streaming layer
  they are one event on one task.
- **A direct read of the tables.** The door's two reads give a row of a microbatch
  layer only when its batch is closed. A select that goes straight to a PState of
  the microbatch gate is not held to that: it sees a batch that is not closed,
  which Rama can still run again. The read path reads so, for every fact that a
  caller is given, and so do the proofs' own hands. A second attempt makes each fact's row, and
  each answer's stamp and verdict, as the first made them: both take the
  batch's kept clock. That holds while one version of the module's code runs
  both attempts. One thing it can make otherwise, and a direct read sees it:
  in an answer, which of two words
  an act into a layer of the other gate is refused with. And a direct read is
  given one task's part of a batch: a row that a task shows is the row that
  stays, and that every task shows a batch together is not given. When a commit
  fails after some tasks have shown their part, the others show it only when
  the second attempt commits; a probe saw 3.4 seconds between them, on a module
  of its own and not on this one. The
  PStates are not declared private. The proof looks at rows that way.
- **One instant for two tasks.** Rama makes a batch visible task by task. The
  reads give nothing of a batch before every task has made it visible; then each
  task opens when it commits the next run of the gate. So for a few milliseconds
  one read can give an answer while a read on another task still gives nothing.
  The one pair of the door's reads on two tasks is the answer of the base's
  making act and the depot of the base.
- **The same word on every attempt of a batch.** The order of a batch is the same
  on every attempt, and so is its clock, kept before the first stamp is made
  from it. With them each check that reads the batch, the clock and this gate's
  record gives the same verdict and the same word, the 10-second rule too. One
  word can differ: the word for a layer of the other gate follows what that gate
  had made by then; the act is refused on each attempt. No read of the door
  gives it before the batch is closed.
- **What an act with no permission costs the microbatch gate.** An act in a made
  layer whose `:who` holds no permission there could still come to stand by a give
  of the same batch. So it asks for each of its rows and goes to the layer's task:
  up to 256 reads of places, where the streaming gate
  reads 4 rows at most. The door is no guard of it: it stops no act. Each act
  of such a who reaches the gate and is recorded, refused under its own act-id
  (entry 7 of 12 October).
- **How long one act can hold a task of the streaming gate.** The streaming
  gate's event does not give the task back: a yield would let another act of the
  layer in between this act's reads and its writes. So one act holds its task
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
  layer. Nothing is built against it: the remedy is a choice of reads or of
  limits, and no speed is measured. The door bounds nothing: a thing of any
  size that Rama's client writes reaches the gate. The gate takes the digest of
  the whole thing before the declaration's check refuses it, and a value over
  its bound costs the making of its text, once, before its refusal.

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
| F6 A, Q7 a | Two depots, one for each gate: the streaming depot and the microbatch depot | [module.clj](module.clj) declares `*streaming-depot` and `*microbatch-depot`. It declares a third depot, `*batch-clock-depot`, which takes no act and from which no gate decides |
| F2 A | The record is in the gates' PStates | Each gate owns the PStates of its record; no depot is ever trimmed. That the acts depots are the replay log is a session's sentence, and no decision of his. What a rebuild from the microbatch depot reaches is under rules 9 and 19 |
| F1 A | The depot of a layer is a fact on the layer | The row at the layer's own entity under `:softland.store/depot` ([rows.clj](record/rows.clj)) |
| F4 C | The base is a microbatch layer; every other layer starts streaming | `depot-at-making` in [first_act.clj](gate/first_act.clj) |
| F5 A | A gate reads on its own task | The streaming gate's event has no hop. In a microbatch layer five rows are on every task: the layer's depot fact, each permission, who may see the layer, what it builds on, and its marked keys (`on-every-task?` in [batch.clj](gate/batch.clj)). So an act finds its permission where it arrives, and the home task of a place finds the marked keys where it reads the place. A layer holds facts under its own entity |
| Q22 A | The door reads an answer from the record by act-id; the ack is a signal | `answer-by-act-id` in [door_reads.clj](module/door_reads.clj); the streaming gate gives nothing back in the ack, and an ack of the microbatch depot says only that the depot holds the thing |
| F7 A | A signal is not the truth; the record is; a gate reads its own record by act-id before it decides | Check (a): the first read of each event of the streaming gate, and the reply about the answer in step 2 of the microbatch gate |
| F8 C | The gate refuses a setting key on an entity that is not the act's layer | Check (f) in [decision.clj](gate/decision.clj); `setting-keys` in [declaration.clj](door/declaration.clj) |
| F9 | The three limits are targets | See "Engineering choices", rule 21 |

Who acts, and what the store reads (8 October):

| Row | Decision | Where it shows |
|---|---|---|
| E1·H8 A | The system stands at the top of the base's chain | `system` and `base` in [first_act.clj](gate/first_act.clj) |
| K2 | A signed-in user has a personal layer and a permission in it; the person who creates a group makes its layer | The form of a making act in [first_act.clj](gate/first_act.clj) |
| E4 A, E2 A; and E3·H7 B, which is a session's reading and not his answer | Permission is a hierarchy that ties back to one person; each actor acts under its own; whoever holds one can give one beneath it | Checks (d) and (g) in [decision.clj](gate/decision.clj). Since the read build a holder of a permission that says `:see` cannot give: her give is an act, and is refused `:permission-does-not-allow-acting` |
| H2 A | A stamp holds the clock time and a counter | [stamp.clj](gate/stamp.clj) |
| K1 A | The store's keys of that day are the depot and a permission. The third of his list, the door's request for locks, left the store's own keys with entry 2 of 12 October | `store-keys` in [declaration.clj](door/declaration.clj). The table holds those two and the three of the decisions of 11 and 12 October: who may see a layer, what it builds on, its marked keys |

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
| 5 | The tables keep a value plain | `fact-row` in [rows.clj](record/rows.clj) keeps the one text of the value in its normal form, as a String, as the last step of the check in [declaration.clj](door/declaration.clj) made it; `plain-bytes` gives its UTF-8. The act in the depot's log is as its maker gave it, each value plain, with an act-id and the layer's depot added |
| 6 | For the keys that a layer marks, the gates keep the facts in one more order, by value | The store key `:softland.store/marked-keys`; `facts-by-value` in [tables.clj](record/tables.clj) and the PState that each gate declares with it; the key of that order, the two ends of a run and the value digest in [rows.clj](record/rows.clj); `writes` in [decision.clj](gate/decision.clj). For a read: the four words of a query in [query.clj](read_path/query.clj), and what each reads in one layer in [sources.clj](read_path/sources.clj) |
| 7 | From a fact, a read can get who wrote it, and under which permission | The fields `:who` and `:permission` of a fact's row, and of a row of the facts by value, in [rows.clj](record/rows.clj). For a read: `place-answer` in [answer.clj](read_path/answer.clj) puts either in only when the query asked |
| 8 | The base's second is fine. The base stays a microbatch layer | The base is decided in batches, by the microbatch gate. What the read build does with that is a session's reading, which he did not strike: a row of the base that a task shows is the row that every attempt of its batch makes, because each batch's clock is kept across its attempts ("The clock of a batch", below); and a place holds one row, its current fact, so a select of a place that holds a fact never gives nothing. The read path reads a row of the base straight, as it reads a row of a streaming layer |

Before real data is kept (12 October), the entries that show in code:

| Entry | Decision | Where it shows |
|---|---|---|
| 2 | Every value is plain in the log, and the store keeps no locks | [door.clj](door.clj) appends the maker's act with `:act-id` and `:depot` added, and nothing else changed. In [declaration.clj](door/declaration.clj) what arrives as a fact is the maker's fact, and a part that the shape does not name is refused `:unknown-part`. No PState and no query of the module holds a lock |
| 6, its fourth | The limit of four on the depth of a permission is lifted, and a bound on the size of an act takes its place | `nesting` in [act.clj](act.clj) gives a permission the value's own number, 32. `max-act-bytes` in [declaration.clj](door/declaration.clj), held by the last step of the check with the word `:act-too-large`. "Engineering choices" says what each counts |
| 7 | An act from a maker who may not act is written to the log, and then refused. The door checks nothing first | The door's three tests and no other, `reading` in [additions.clj](door/additions.clj): each is of what its two additions need. `standing` in [decision.clj](gate/decision.clj) refuses such an act under its own act-id, and no fact of it is in the tables |

## Engineering choices

Each is the builder's, with its reason. None is a decision of Sid's.

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
layer that is the layer's task for every row.

**What one accepted act costs the streaming gate**, in one event, on one task.
The act: one value under a plain key, on one entity, nothing named to replace.
Counted from the dataflow, not measured.

| | Reads | Row writes |
|---|---|---|
| The key not marked, or no key of the layer marked | 4: the answer, the permission, the marked keys, the task's last stamp | 3: the fact, the answer, the last stamp |
| The key marked | 5: and the row before | 4: and the entry in. 5 when the entry of a value before goes out |

Without the order by value it would be 3 reads. The one read more, of the
layer's marked keys, is paid by every accepted act with a fact under a plain
key, also in a layer that marks no key: a gate must know whether the key is
marked where it writes the fact, and no row that such an act already reads is
the layer's. An act with no fact under a plain key, a
give, a setting or a list, reads no marked keys. A refused act reads none
either: no check reads the marked keys. An act that the declaration refuses, a
value or an act over its bound among them, costs 2 reads, the answer and the
last stamp, and 2 row writes. Keeping the marks in memory
between two events would be a cache, which the project's rule sends to its own
examination. A form that would give 3 reads again was found and not taken: one
row for each streaming layer that holds the layer's last stamp and its marks. It
changes what a stamp is, from "increasing on its task" to "in its layer".

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
marked gets a new one. In the streaming gate that is the stamp of the act that
marks it. In the microbatch gate it is the last stamp that the batch can hold. So
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
its key has five parts where a place has three, and in a microbatch layer the
two are placed by different parts.

**The gate does not read the permissions above the act's own.** A permission row
is written only by a right making act or by a give whose own permission stood and
is exactly the one above, and nothing in this build ends a permission. Reading
them would cost up to 31 reads on every act to find what cannot be false.

**The depot of a layer costs no read for a layer that is made here.** A gate's
record holds rows only for layers of its depot, so a row at the permission's
place or at the depot's place says that the layer is this gate's.

**The stamp's packing.** Milliseconds since 1970 × 65,536 + a counter. One
millisecond holds 65,536 stamps for a task; the packing reaches 2^62 in the year
4199. A gate's last stamp is read from the record, by the streaming gate at each
decision and by the microbatch gate once for a batch, and is not kept in memory:
that would be a cache, and the project's rule sends a cache to its own
examination first.

**The one text and the digest.** One function from anything to a string, blind to
what a serialiser may change and no check can see. The digest is SHA-256 over it,
kept whole. The door takes it of the thing that it appends, and a gate of the
thing as it comes off its depot, each value plain; nothing between the two gives
a part another form. That a maker reads her answer as her own rests on Rama's
writing and reading of a record leaving the one text of a thing as it was;
[the proofs' map](../../../test-store/softland/store/README.md) says which
checks hold that.

**The form of a making act.** Exactly two facts, in either order: the depot on
the layer, and the act's own permission on its holder.

**The source of each gate.** It starts from the beginning of its depot. The
streaming gate's has the retry mode "all after", and nothing is given back in the
ack. The source of the topology that keeps a batch's clock starts from the
beginning of the depot of batch clocks too, with Rama's default retry mode, a
failed record alone: its rule writes nothing wrong for a record that it reads
again or out of order.

**The first argument of a query routes it.** Rama's client hashes the first
argument and sends the query straight to one task. For `depot-of-layer` it is
the layer. For `answer-by-act-id` it is the routing id of
the act-id: the layer of a streaming act-id, the uuid of a microbatch one. The
caller still needs the act-id alone.

**Where each row of a microbatch layer lives.** The microbatch depot places an
act by one entity that it derives from the act's facts; that it is the entity of
the first fact is a choice, which the contract names as the builder's. The
reason: with a fact on the task of its entity, an act on one entity arrives
where its places are. The rest is the builder's too.

- A fact is on the task of its entity. An act on one entity then finds each place
  that it reads and writes where it arrives.
- An answer is on the task of its act-id's uuid. A reader has the act-id and
  nothing else, so this is the one placement where the read is one seek with no
  hop and nothing passes through one task.
- A layer's depot fact and each permission are on every task. Every act needs the
  permission of its `:who` where it is decided. A copy is bytes and a rare write;
  a fetch would be a hop between tasks on every act. That is the reason that a
  session gave him for F5 A and that he agreed with, read onto the permissions. A give writes one row on each task.
- Who may see the layer, what it builds on and its marked keys are on every task
  too. A gate reads the marked keys on the home task of each place, and a reader
  finds the setting and the list on the task where it reads. Each is one small
  row for a layer and is written rarely.
- A row of the facts by value is on the task of its value's digest. So the run of
  one value is on one task, found from the value alone, whatever the entities of
  its places are. It is not the task of the fact: the row is sent there inside
  the attempt.

**Where an act of a batch is decided.** On the task of the entity of its first
fact, where it arrived, when no other act of the batch needs a row that it needs
from another task. Two acts meet when both need one row and cannot be put in
order on one task; they are decided on the task of their layer id, in turn. An
act goes there when it writes a row that every task holds, when the place of its
permission is empty and a give of the batch could fill it, when a home task says
that a row is shared, or when an earlier act of its task that goes there names
one of its own places. Two acts of one batch on one place under a marked key
meet too, also when each has the place as its own: a row of the facts by value
is on a third task, where what two tasks or two steps send has no order, and in
the pass the two are decided in turn and one row is sent for each key, the last.
In ordinary work nearly every act stays. Sending every act
to the layer's task would be simpler and is what rule 6 says not to do.

**The order of one batch: the turn.** A thing's turn is its position in its depot
partition's part of the batch, times the number of tasks, plus the partition's
number. The decisions of a batch are those of deciding its acts one at a time in
the order of their turns. On one partition that is the depot's order. Between two
partitions it is a fixed rule with no other meaning. The count of positions is in
the gate's PState, not in memory: Rama sets a PState back when an attempt of a
batch fails, so the second attempt gives the same turns. One thing is not decided
in the order of the turns: a mark of a key and a fact under that key in one
batch. The home task of each place reads the layer's marked keys once for a
batch, as every task held them when the batch began; so a mark holds from the
batch after, whatever the two turns are. The other way would send a question
from every act of every batch to one task, for a write that is rare.

**The stamp of a batch.** The gate takes one clock for a batch, the same on
every attempt. The batch's
base is later than the gate's last stamp and than each stamp that an act of the
batch carries, the last held to 10 seconds ahead. An act's stamp is the base plus
its turn. The gate keeps one last stamp, on the task of the depot word
`:microbatch`, so each stamp is later than every stamp that the gate gave before
on any task.

**The clock of a batch.** Rama can run a batch a second time, also after one task
has shown its part of the first attempt. A fact's row holds a stamp, and a stamp
holds the clock; so a second attempt that read the clock again would change each
row that a task had shown. The gate therefore keeps each batch's clock across its
attempts. Three things do it.

- A third depot, `*batch-clock-depot`, which no client can append to. A record is
  `{:batch :clock}`: the microbatch id of a run and a reading of the clock. It
  holds no act, and no gate decides from it.
- A stream topology of its own, `batch-clock`
  ([batch_clock.clj](module/batch_clock.clj)), with one PState, `$$batch-clock`:
  one row, the clock of the newest batch that kept one. First write wins: a
  record of the row's own batch, or of an older one, writes nothing. So the first
  clock that is kept for a batch stays, however often Rama hands the topology a
  record, and a record that comes late never puts the row back. One row and not a
  row for each batch, because a kept clock has one use, a later attempt of the
  same batch, and Rama starts no batch before the one before is committed on
  every task. The depot keeps every record.
- The head of step 2 of the walk, on the gate's task, only in a batch that has
  things to decide. It reads the row. When the row is of this batch, that is the
  batch's clock, and no clock is read. When it is not, the task reads the clock,
  appends the batch's id and that reading with the ack level `:ack`, which waits
  until the topology has processed the record and the row is durable, reads the
  row again, and uses what the row holds, never what it offered.

So the clock is kept before the first stamp is made from it, and before any task
can show a row of the batch. Why the row is not in the gate's own PStates: Rama
sets those back when an attempt fails. Why it is not in the memory of a process:
a restart of a worker is one cause of a second attempt. Why a topology of its
own, beside the streaming gate's: as a second source of the
streaming gate, every batch of the base would wait in that gate's queue on one
task, and a streaming layer that is stuck there would stop the base. The depot
and its one used partition, the row and the topology's event are all on the
gate's task, so nothing hops.

What it rests on, and what it costs. The id of a batch is the same on each
attempt, goes up with each run, and never comes again; two probes ran that across
a failed attempt, a restart of a worker and a module update. An attempt that
cannot read its clock back after the ack makes no stamp: one function of the
gate throws, and Rama runs the batch again. On the in-process cluster a throw on
a task's thread stops the worker, with both gates and the reads on it, until
Rama has started it again; and while its cause stays, the batch fails on each
attempt. No traced case reaches that throw without a fault of the cluster or of
this module. When the topology that keeps the clock does not answer, the attempt
waits until Rama's phase timeout fails it, about 30 seconds where nothing is
set, and no stamp is made meanwhile; the module sets no such option. The depot of
batch clocks is never trimmed, as the depots of acts are not. And the kept clock
gives the same rows only while one version of the module's code runs every
attempt of a batch: after a module update that changes the code from which a row
is made, a second attempt writes its rows with the new code.

**Closing.** Rama makes a batch visible task by task, and it can run a batch
again after one task has shown its part. A second attempt can give an act into a
layer of the other gate the other of two words. So a
reader could see a word that later changes. Each task therefore
holds `:closed-through`, and the door's two reads and the streaming gate's
question about the base give a row
of a microbatch layer only when the row's stamp is not later than it. Each run of
the gate writes `:closed-through` with the gate's last stamp as it was before the
run. That run began only after every task had committed the batch before, so a
closed batch is visible everywhere and cannot run again. The run after a batch
with things starts at once, so an answer is readable one short run after its
batch. The cost: one more read for an answer that is there, and one small write
on each task for each batch. And one thing that closing leaves to a read of
facts: a place holds one row, its current fact. So after a batch replaces a
fact, its place holds no row of a closed batch until that batch is closed, and a
place that is replaced in every batch never holds one. The door's two reads do
not meet it: the one fact that they give is the depot of a layer, which is
written once. The read path is the read that meets a replaced place, and it
reads the place straight: the row that the task shows, which every attempt of
its batch makes.

**What one accepted act costs the microbatch gate.** The act: one value under a
plain key, on one entity, nothing named to replace, alone in its batch. Counted from the
dataflow, not measured; the messages are those between two different tasks.

| | Reads | Row writes | Messages |
|---|---|---|---|
| The key not marked | 3: the permission where it arrived, the answer on the uuid's task, the marked keys on the act's own task | 2: the fact, the answer | 3: the question about the answer, its reply, the answer's row |
| The key marked | 4: and the row before, at the act's turn on its own task | 3: and the entry in. 4 when the entry of a value before goes out | 4, or 5: each entry to the task of its value's digest |

Without the order by value it would be 2 reads. The marked keys cost one read
for a task and a layer in a batch, however many acts the batch holds: for 1,000
acts of one layer on one task it is one read, and for an act whose facts are on
several tasks it is one on each. An act with no fact under a plain key reads
what it read before. A fact under one of the five keys whose row every task
holds is one row write on each task. Beside
these, each thing that arrives, a stray and a repeat too, costs one read and one
write of its task's count of turns. Each batch adds a few reads that do not
grow with its acts. And a batch with things keeps its clock: 3 reads, one
record appended and one row written, all on the gate's task, once for the batch
and not for an act; a later attempt of the batch pays 1 read, and a run with
nothing to decide pays none. The attempt waits for the ack of that append,
which on a real cluster is two rounds of replication to the followers of that
one task, the record and then the row; on one machine with one copy two probes
measured 3 to 6 ms, a floor and not a cost. In the microbatch gate a reply carries the whole row before,
with its value of up to 65,536 bytes, where the decision uses its stamp and the
digest of its value; the same holds of a fact that names something to replace.

**The placing of the checks that the contract does not place.** The making and
the counting of the values' texts is the last step of the declaration's check:
it is the most costly. The 10-second
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
4. The module sets no trimming option on the depot.
5. The module makes no lock. The one append that its code makes is of a
   batch's id and clock.
6. The turn gives two acts that replace one fact the same winner on every
   attempt. An act is decided once, on one task, with each row that it needs in
   hand; the rows of an accept go to several tasks inside one attempt, which Rama
   keeps or drops whole. Only the acts that meet pass one task.
7. A layer's depot is a fact at a place found from the layer id; in a microbatch
   layer every task holds it. An act's answer is found from its act-id alone, by
   its routing id. Each gate reads the other gate's record for a layer that its
   own record does not hold and that starts with the other depot; no act of this
   build changes a depot, so no other layer can be there.
8. Each word is under "Words a session made", below.
9. Found: no, not when the two acts are on two depot partitions. Inside one
   partition the depot holds the order. Between two it holds none: the order in
   which the gate decided two such acts is the batch of each and then the turn,
   and which batch a thing fell into is a fact of timing. A rebuild that reads
   the depot from its start cuts other batches, so the other act can have the
   earlier turn. It matters only for acts that meet from two partitions. What
   would make it the same is a record of each batch's cut and clock, a second log
   beside the depot. A first part of it is built, for another purpose: the depot
   of batch clocks holds the clock of each batch that had things, by the id of
   the run that decided it. It holds no cut, and a rebuild cuts other batches, so
   it does not make a rebuild the same. Whether the rest is wanted is Sid's.
10. The test for a stray runs before check (a). A stray gets no answer; its digest
    is added to the set of its word, on the task where it arrived, once however
    often it comes. The count is the size of the set. In the microbatch gate that
    is the task where the depot put the thing.
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
15. A refusal writes the answer and nothing else; in the streaming gate also the
    task's last stamp.
16. The gate keeps the one text of each value in the fact's row. The text of a
    value of a refused act is dropped with the decision.
17. An act that carries a stamp more than 10 seconds ahead of the gate's clock is
    refused, before any stamp is made. The microbatch gate holds each act of a
    batch to the batch's one clock, on every attempt.
18. A microbatch act has one stamp, the batch's base plus its turn, on its answer
    and on each of its facts. The gate has one last stamp for all tasks, so the
    stamp is later than each stamp that the gate gave on each task where the act
    writes. "The last stamp of each task" is read as the last stamp that this
    gate gave there; the streaming gate's last stamp of the same task is of
    another record.
19. Nothing is built for a rebuild of the record from the depot. What a rebuild
    would not give again: a stamp, which
    holds the gate's clock at the decision; a refusal by rule 17; the order
    across a failed batch; and, in either record, which of the two words an act
    sent to the wrong depot got, the word for a layer of the other gate or the
    word for a layer that is not made: that rests on an order between the two
    depots that no depot holds. For the microbatch record also: rule 9's answer; and
    the stamps, although the clock of each batch with things is on the depot of
    batch clocks by the id of its run, because a rebuild cuts other batches
    (rule 9).
20. Seven numbers in [declaration.clj](door/declaration.clj). Five are held by a
    rule or by the test for a stray, before a row key is built from an id. Two
    are held by the last step of the check: the bound on one value, counted of
    its text, and the bound on one act. The two of the read build are among the
    five: a layer builds on 64 layers at most, and marks 32 keys at most.
21. The aims: 1,000 acts a second on one task; 20 ms from the door's append to the
    answer in the door's hand. The contract's third is of the bytes of a lock
    for each value, and the store keeps no lock. Neither of the two is
    measured on a cluster with a disk and a network. The measures of the read
    build time a gate under 100 acts a second on the in-process cluster, with
    every row in memory ([the proofs' map](../../../test-store/softland/store/README.md),
    "The measures"); that is a floor for a round trip and no measure of an aim.
    By the cost of a cold read, the 4 reads of one accepted act give
    480 to 770 acts a second on one task, and the 5 of an act under a marked key
    384 to 616; the first aim is met only when the rows are read from memory.
    For the microbatch gate: 1,000 acts a second arriving
    on one depot partition, which needs a run of the gate inside a second; and an
    answer readable within two runs of the gate after the append, its own batch
    and the short one that closes it, not 20 ms, because a microbatch layer is not
    for what a person does directly. None is measured.

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
| `marks-places-asked`, `batch-end` | In [batch.clj](gate/batch.clj): the marked-keys places that a task reads in a batch, and the last stamp that a batch can hold |
| `plain-bytes`, `plain-bytes-of` | In [rows.clj](record/rows.clj): the UTF-8 bytes of the String that a fact's row holds for its value; and those bytes of a row's value |
| `holds-value?`, `value-of` | In [rows.clj](record/rows.clj): whether a row's value is the one text of a value given, and the first form that a row's value reads as |
| `read-value`, with `:no-fact`, `:nothing-here`, `:value`, `:not-a-value` | In [rows.clj](record/rows.clj): what a row says of its value, for a reader |
| `who-of`, `permission-of`, `marked-keys-of` | In [rows.clj](record/rows.clj): a row read back |
| `with-level-since`, `by-value-key`, `by-value-run`, `by-value-row`, `by-value-route`, `by-value-entity`, `value-digest-of`, `id-of-text`, `place-of-key` | In [rows.clj](record/rows.clj): the keys and the rows of the order by value, and a key back to its parts |
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

**The words of the read build, for the clock of a batch.** Each is a session's.
None is from the rig's code, and none is a name of the probes that ran the way
first. "Batch", "attempt" and "microbatch id" are Rama's.

| Word | What it is |
|---|---|
| the batch's clock | The one clock time from which a batch's base is made and against which the 10-second rule is held, the same on every attempt |
| to keep a clock; the kept clock | To put a batch's clock where a later attempt of the batch reads it; that clock |
| the depot of batch clocks | `*batch-clock-depot`, under "Names in Rama" |
| `:batch`, `:clock` | In a record of the depot of batch clocks, and in the one row of `$$batch-clock`: the batch that the clock belongs to, as the number of its microbatch id, and the clock time in milliseconds since 1970. In `:turns` the same id is held as text |
| `clock-record`, `kept-clock`, `clock-row-to-keep` | In [batch.clj](gate/batch.clj): the record that the gate appends, the clock that a row holds for a batch, and the rule by which the first clock that is kept stays |
| `clock-must-be-kept` | In [microbatch_gate.clj](module/microbatch_gate.clj): the one throw of the module's code, for a clock that cannot be read back after its append was acked |
| `batch-clock` | In [tables.clj](record/tables.clj): the schema of the kept clock |

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
| `$$streaming-task` | What the streaming gate keeps for one task: its last stamp, its strays |
| `*microbatch-depot` | The microbatch depot: every thing sent for a microbatch layer |
| `microbatch-gate` | The microbatch topology that decides the things of the microbatch depot |
| `$$microbatch-answers` | The answer under each act-id of a microbatch layer |
| `$$microbatch-facts` | The current fact of each place of each microbatch layer |
| `$$microbatch-facts-by-value` | For each marked key of each microbatch layer, the same facts in the order of their values |
| `$$microbatch-task` | What the microbatch gate keeps for one task |
| `$$questions`, `$$replies`, `$$parcels` | The three temporary PStates of one attempt of a batch: what step 1 gathered, what step 2 replied, what step 3 sent |
| `*batch-clock-depot` | The depot of batch clocks: one record for each batch of the microbatch gate that had things to decide. No client can append to it |
| `batch-clock` | The stream topology that keeps the clock of the newest batch |
| `$$batch-clock` | The clock that is kept for the newest batch that had things: one row, under the depot word `:microbatch` |
| `answer-by-act-id` | The query: act-id → the recorded answer, or nothing |
| `depot-of-layer` | The query: layer → its depot, or nothing |

**Folders and namespaces.** Five folders: `door`, `read_path`, `gate`, `record`,
`module`.
Each namespace is `softland.store.`, its folder, and the word: `door.text`,
`door.digest`, `door.declaration`, `door.additions`, `door.wait`,
`door.words`; `gate.decision`,
`gate.first-act`, `gate.stamp`, `gate.batch`; `record.rows`, `record.tables`;
`module.streaming-gate`, `module.microbatch-gate`, `module.batch-clock`,
`module.door-reads`; the five of `read-path` are on its own page. Four
are at the top with no folder: `act`, `module`, `door` and `read-path`. "Rows" and "tables" are
the words for the record's two files: what a gate writes and reads, and the
PStates that hold it. Each folder's map says what its files are. In `act.clj`,
one name is a session's: `as-shape`, which gives the act's engine to another
namespace. The proofs are `act-proof`, `pure-proof`, `module-proof`,
`door-proof`, `read-path-pure-proof` and `read-path-proof`; `read-measures` is a
run that is no proof. The words that the door's build made are in
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
| `:last-stamp` | In the task's value: the last stamp that the gate gave on the task. The microbatch gate has one, for the whole gate, on the gate's task |
| `:strays` | In the task's value: by reason word, the digests of the strays seen |
| `:closed-through` | In the microbatch gate's task value: the stamp through which this task's rows are of closed batches. The door's two reads and the streaming gate's question about the base give a row only when its stamp is not later |
| `:turns`, with `:batch` and `:given` | In the microbatch gate's task value: the batch that the count belongs to, as the text of its microbatch id, and how many things of it this task has given a turn |
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
| `:carried-stamp-too-far-ahead` | The act carries a stamp more than 10 seconds ahead of the gate's clock |
| `:layer-belongs-to-the-other-gate` | The layer is made, and its depot is the other gate's: the act was sent to the wrong depot |
| `:layer-not-made` | The record holds nothing for the layer, and the act is not a right making act |
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

**Words between the decision and the gate**, for the rows of one decision:
`:fact`, `:by-value`, `:answer`, `:last-stamp`. A row of
the kind `:by-value` that is nil says that its key is to be taken out. And for
what a gate hands `writes` for an accept:
`:since-by-place`, `:place-rows`, `:level-from`.

**The words of a batch**, in the explanations and in the data between
[batch.clj](gate/batch.clj) and the microbatch gate. No reason word and no word of a
stray is new: each refusal of the microbatch gate is recorded with a word of the
lists above. "Batch" and "attempt" are Rama's words.

| Word | What it is |
|---|---|
| turn | The number of a thing in its batch. The acts of a batch are decided in the order of their turns |
| base | The first stamp of a batch. An act's stamp is the base plus its turn |
| deciding task | The task that the hash of the entity of an act's first fact gives. An act that meets no other is decided there |
| home task | The task on which a row lives |
| the gate's task | The task that the hash of the depot word `:microbatch` gives. The gate's last stamp is there, and the clock that is kept for the newest batch |
| to meet; meeting task | Two acts of a batch meet when both need one row and cannot be put in order on one task. The meeting task of a layer is the task of the layer id; acts that meet are decided there, in turn |
| pass | The walk of a meeting task over the acts that were sent to it |
| route | An id or a text whose hash gives a task: the uuid of an answer, the entity of a fact, the value digest of a row of the facts by value, the layer of an act that meets |
| entry, `:entry` | What step 1 keeps of a thing for step 3: its turn, the thing, the act in its one form, what was found of its standing |
| `:standing`, with `:stands`, `:final`, `:open` | In an entry: what step 1 found. The permission is there; the act is refused for good; or the permission's place is empty and a give of the batch could fill it |
| question, with `:answer`, `:place` | What an act sends to the home task of a row that it needs; the two kinds |
| `:wanted?`, `:own?` | In a question about a place: the fact names something to replace, so the row is needed; the place is on the act's own deciding task |
| `:marks-place`, `:k` | In a question about a place: the key of the layer's marked-keys place, and the fact's key |
| `:since` | In a read of a place and in a reply about one: the stamp of the place's key, when the layer marks it |
| `:batch-end` | In a reply about an answer, and in an item: the last stamp that the batch can hold, which a newly marked key gets |
| `:since-by-place` | In an item, and in an act sent to the meeting task: the stamp of each of its places whose key is marked |
| reply, with `:yours`, `:used`, `:taken-in-this-batch`, `:shared?` | What a home task sends back. For an answer: the act-id is the asker's to use, is used in the record, or goes to an earlier turn of this batch. For a place: another act of the batch needs it too |
| `:numbers`, with `:carried` and `:position` | What each task sends the gate's task for the base: the greatest stamp that its things carry, and its greatest position |
| parcel, with `:rows` and `:meet` | Rows that one task sends to another to write, or an act sent to the meeting task |
| `:home`, `:every-task?` | In a row on its way: the route of its home; and that every task holds it |
| closed | A batch is closed when every task has made it visible and the next run of the gate has begun. Rama cannot run it again |
| routing id | The first argument of `answer-by-act-id`: the layer of a streaming act-id, the uuid of a microbatch act-id |

**Marks in the one text**: `#bytes`, `#double`, `#keyword`, `#other`,
`#too-deep`, `#unreadable`. [text.clj](door/text.clj) says what each marks.
