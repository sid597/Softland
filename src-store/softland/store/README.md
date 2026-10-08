# Store — the act, and the store's module

[Parent: namespace root](../README.md) · [Store source entry](../../README.md).

This folder holds two things. The act: the one form of what a maker gives to the
store, and the check of that form. And the store's Rama module, `Store`: two
depots, the gate of each, the record that each gate keeps, and the three reads by
which the door reads the module. An act that a gate accepts becomes facts in the
record, and its answer is read back by act-id.

A layer has a class, streaming or microbatch. The class says which depot takes
its acts and which gate decides them. The streaming gate decides each thing in
one event, on the layer's task. The microbatch gate decides the things of a
batch, across tasks. The base is the one layer that starts as a microbatch
layer; every other layer starts streaming.

The door is not built. The [module's proof](../../../test-store/softland/store/README.md)
plays it by hand.

## The files

| File | Responsibility | State and lifetime |
|---|---|---|
| [act.clj](act.clj) | The declared shape of an act and of what it holds, the rules that a shape cannot say, and the check that reads both. It gives its engine to the door's declaration. | None. |
| [text.clj](text.clj) | The one text of a thing: one function from anything to a string, the same in every process. | None. |
| [digest.clj](digest.clj) | The digest of a thing, SHA-256 over its one text. The door and the gate share it. | None. |
| [declaration.clj](declaration.clj) | The door's declaration: what arrives on a depot, as shapes and rules in `act.clj`'s language; the store keys; the limits; the test for a stray; the check. | None. |
| [stamp.clj](stamp.clj) | The stamp: clock time and a counter in one integer, the next stamp, the 10-second rule. | None. It reads no clock. |
| [seal.clj](seal.clj) | Locks and the seal: a lock's bytes, sealing a value to its place, opening it. | One random source for each process. |
| [first_act.clj](first_act.clj) | The class that a layer starts with, and the form of a making act. | None. |
| [record.clj](record.clj) | The record's rows: the text of each key, the making and the reading of each row, the routing id of an act-id, and the test of what a caller gave a read. | None. It holds no record. |
| [decision.clj](decision.clj) | The checks of one act against rows of the record, and the rows that a decision writes, as data. | None. It reads no PState. |
| [batch.clj](batch.clj) | The pure code of one batch of the microbatch gate: what the depot derives from a thing, the turn of a thing, what an act asks of other tasks and what they reply, which acts meet, the decision of an act with its rows in hand, the home of each row. | None. It reads no PState. |
| [record_pstates.clj](record_pstates.clj) | The record as PStates: the schemas, written once, and what each gate keeps for a task. | None. It declares nothing. |
| [streaming_gate.clj](streaming_gate.clj) | The stream topology `streaming-gate` and the dataflow of one event. | It owns the four PStates of the streaming record, durable in the cluster. It borrows two PStates of the microbatch gate to read. Nothing in the memory of a process. |
| [microbatch_gate.clj](microbatch_gate.clj) | The microbatch topology `microbatch-gate` and the dataflow of one batch, in four steps. | It owns the four PStates of the microbatch record, durable in the cluster. It borrows one PState of the streaming gate to read. Three temporary PStates of Rama live for one attempt of one batch. Nothing in the memory of a process. |
| [door_reads.clj](door_reads.clj) | The three query topologies, for layers of both classes: the answer by act-id, the locks of a request, the class of a layer. | None. It borrows the two gates' PStates to read. |
| [module.clj](module.clj) | The module `Store`: the two depots, and the calls that add the two gates and the reads. | It owns the two depots, durable in the cluster and never trimmed. |

The first ten are pure code and load with Clojure alone. The last five require
Rama.

## How the parts relate

The two gates share the pure code. Both decide with [declaration.clj](declaration.clj),
[digest.clj](digest.clj), [decision.clj](decision.clj),
[first_act.clj](first_act.clj), [stamp.clj](stamp.clj), [seal.clj](seal.clj) and
[record.clj](record.clj). Where what a function gives depends on the gate, it
takes the gate's class: the test for a stray, the form of a making act, each
check against the record. No check is written twice, and the two records have
one shape of row.

**A thing for a streaming layer** is appended to the streaming depot, which puts
it on the task of its `:layer`. Every row of a streaming layer is on that task.
The streaming gate takes the thing there, in one event:

1. [declaration.clj](declaration.clj) tests it for a stray. A stray gets no
   answer; the gate keeps its digest.
2. The gate reads the answer under the thing's act-id. If one is there, the event
   ends. A repeat is answered, not decided twice.
3. [digest.clj](digest.clj) takes the digest of the thing, over its one text
   ([text.clj](text.clj)). [declaration.clj](declaration.clj) checks the thing; it
   reads its shapes with [act.clj](act.clj)'s engine.
4. [decision.clj](decision.clj) checks the act against rows that the gate reads
   for it: its standing ([first_act.clj](first_act.clj) knows the form of a making
   act), its places, its seals ([seal.clj](seal.clj) opens each sealed value).
5. [stamp.clj](stamp.clj) gives the stamp. [decision.clj](decision.clj) gives the
   rows to write, each made by [record.clj](record.clj), and the gate writes them
   to the PStates whose schemas [record_pstates.clj](record_pstates.clj) gives.

**A thing for a microbatch layer** is appended to the microbatch depot, which
puts it on the task of the entity of its first fact
([batch.clj](batch.clj) derives it). The rows of a microbatch layer are on
several tasks: a fact on the task of its entity, an answer on the task of its
act-id's uuid, a lock on the task of its request's uuid, and the class and each
permission on every task. So no one task holds all that an act needs, and the
depot holds no order between two partitions. The microbatch gate decides the
things of a batch in four steps. Each step is done on every task before the next
starts:

1. Arrive and question. On the task where it arrived, each thing gets its turn,
   its number in the batch. [declaration.clj](declaration.clj) tests it for a
   stray and checks it. The gate reads the permission of its `:who` from the
   task's own copy. The act sends a question to the home task of each row that
   it needs.
2. Reply. One task reads the clock once and gives the batch its first stamp.
   Each home task reads each row that was asked for, once, and replies. The
   reply about an answer is check (a): the earliest turn under an act-id has it.
3. Decide. Each task takes its acts in the order of their turns. An act that
   needs no row that an act of another task needs is decided there, by
   [decision.clj](decision.clj)'s checks in their order. Acts that meet over a
   row are sent to one task, their layer's.
4. Write and meet. Each task writes the rows that it was sent. The layer's task
   decides the acts that meet, one at a time in the order of their turns.

[batch.clj](batch.clj) holds each pure part of those steps;
[microbatch_gate.clj](microbatch_gate.clj) walks them.

**Each gate asks the other's record one thing**: whether it holds a layer of
which its own record holds nothing. The streaming gate reads the base's class
row in the microbatch record. The microbatch gate reads a streaming layer's
class row in the streaming record. So an act sent to the wrong depot is refused
as an act into a layer of the other gate.

**The door reads** through [door_reads.clj](door_reads.clj). Each read goes
straight to one task: the layer's task for a streaming act-id and for a class,
the task of the uuid for a microbatch act-id. A read of a microbatch layer gives
a row only when its batch is closed; "Engineering choices" says why.

Each gate's dataflow only reads rows, calls the pure functions and writes rows.
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
  whatever the partitioner; no topology of this build does. A thing that a
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
- **Who reads a lock.** Any caller of the module can read a lock's bytes. The door
  proper is the guard, later. The seal is for a forget of the depot's and the
  disk's copy; it does not hide a value from a client of the cluster.
- **What a lock opens.** The gate checks that the lock opens the sealed bytes and
  keeps nothing of the plain bytes. It does not check that they are a value of the
  form.
- **A direct read of the tables.** The three reads give a row of a microbatch
  layer only when its batch is closed. A select that goes straight to a PState of
  the microbatch gate is not held to that: it sees a batch that is not closed,
  which Rama can still run again with another clock and other lock bytes. The
  PStates are not declared private. The proof looks at rows that way.
- **That a lock seals a value, one run early.** A lock's row holds no stamp. The
  read of locks is held to the closing of the request's batch, and not of a later
  batch in which an act uses one of the request's locks. So from the moment when
  that later batch is visible on the lock's home task, the read gives the lock
  without its bytes: one run before that batch is closed and the act's answer is
  readable. The bytes that a read gave never change.
- **One instant for two tasks.** Rama makes a batch visible task by task. The
  reads give nothing of a batch before every task has made it visible; then each
  task opens when it commits the next run of the gate. So for a few milliseconds
  one read can give an answer while a read on another task still gives nothing.
  The one pair of the door's reads on two tasks is the answer of the base's
  making act and the class of the base.
- **The same word on every attempt of a batch.** The order of a batch is the same
  on every attempt, and with it each check that reads the batch and the record.
  Two words can differ on a second attempt: the 10-second rule reads the clock,
  for an act that carries a stamp at the edge of the 10 seconds; and the word for
  a layer of the other gate follows what that gate had made by then. No read gives
  a row of an attempt that Rama ran again. To make both the same would take state
  outside the microbatch gate. Not built.
- **What an act with no permission costs the microbatch gate.** An act in a made
  layer whose `:who` holds no permission there could still come to stand by a give
  of the same batch. So it asks for each of its rows and goes to the layer's task:
  up to 256 reads of places and 256 ranges of locks, where the streaming gate
  reads 4 rows at most. The door proper is the guard, later.
- **How long one act can hold a task of the streaming gate.** The streaming
  gate's event does not give the task back: a yield would let another act of the
  layer in between this act's reads and its writes. So one act holds its task for
  all its reads, and for the opening of each of its sealed values. Only the
  limits of the declaration bound them: up to 256 places, up to 256 sealed
  values of 65,536 bytes each, and for each request whose locks the act names
  one range of locks, from the least to the greatest index that it names. An act
  of 256 sealed facts that names the two ends of 128 requests reads 32,768 lock
  rows. Such an act needs standing in the layer, and requests that were accepted
  before. Nothing is built against it: the remedy is a choice of reads or of
  limits, and no speed is measured. The door proper is the guard, later.

## Decisions this module stands on

Sid's decisions are in [the store source entry](../../README.md), with his
reasons. This lists them by row and says where each shows here.

The act (6 October): the twelve decisions of "Decisions of 6 October 2026".
[act.clj](act.clj) is their code. Decision 2 is why
[declaration.clj](declaration.clj) contains the maker's shapes and does not write
them again. Decision 3 is why a gate, not the form, refuses an unsealed value.
Decision 5 is the act-id, `[layer class uuid]`.

The store's module (7 October):

| Row | Decision | Where it shows |
|---|---|---|
| F6 A, Q7 a | Two depots, one for each gate: the streaming depot and the microbatch depot | [module.clj](module.clj) declares `*streaming-depot` and `*microbatch-depot` |
| F2 A | The record is in the gates' PStates; the acts depots are the replay log | Each gate owns the PStates of its record; neither depot is ever trimmed. What a rebuild from the microbatch depot reaches is under rules 9 and 19 |
| F1 A | The class of a layer is a fact on the layer | The row at the layer's own entity under `:class` ([record.clj](record.clj)) |
| F4 C | The base is a microbatch layer; every other layer starts streaming | `class-at-making` in [first_act.clj](first_act.clj) |
| F5 A | A gate reads on its own task | The streaming gate's event has no hop. In a microbatch layer the class and each permission are on every task, so an act finds its permission where it arrives. A layer holds facts under its own entity |
| Q22 A | The door reads an answer from the record by act-id; the ack is a signal | `answer-by-act-id` in [door_reads.clj](door_reads.clj); the streaming gate gives nothing back in the ack, and an ack of the microbatch depot says only that the depot holds the thing |
| F7 A | A signal is not the truth; the record is; a gate reads its own record by act-id before it decides | Check (a): the first read of each event of the streaming gate, and the reply about the answer in step 2 of the microbatch gate |
| F8 C | The gate refuses a setting key on an entity that is not the act's layer | Check (f) in [decision.clj](decision.clj); `setting-keys` in [declaration.clj](declaration.clj) |
| F9 | The three limits are targets | See "Engineering choices", rule 21 |

Who acts, and what the store reads (8 October):

| Row | Decision | Where it shows |
|---|---|---|
| E1·H8 A | The system stands at the top of the base's chain | `system` and `base` in [first_act.clj](first_act.clj) |
| K2 | A signed-in user has a personal layer and a permission in it; the person who creates a group makes its layer | The form of a making act in [first_act.clj](first_act.clj) |
| E3·H7 B, E4 A, E2 A | Permission is a hierarchy that ties back to one person; each actor acts under its own; whoever holds one can give one beneath it | Checks (d) and (g) in [decision.clj](decision.clj) |
| H2 A | A stamp holds the clock time and a counter | [stamp.clj](stamp.clj) |
| K1 A | The store's keys are three: the class, a permission, the door's request for locks | `store-keys` in [declaration.clj](declaration.clj) |
| H1 | Open. Working answer: a lock for each value | [seal.clj](seal.clj); the lock's row in [record.clj](record.clj) |

## Engineering choices

Each is the builder's, with its reason. None is a decision of Sid's.

**The module and its classpath.** The module is `Store` in
`softland.store.module`. It has its own alias, `:store-module`; the reason is in
[the store source entry](../../README.md).

**The one change of `act.clj`.** Its engine takes the map of shapes, and
`as-shape` gives the engine to another namespace. So the door's declaration is
data that the same engine reads. The leaves and the nesting limits stay
`act.clj`'s own. No leaf fits a run of bytes, so a fact's `:sealed` is set aside
from the engine's pass and held by two rules in code.

**The record: three flat PStates with string keys.** An answer is found by an
act's uuid, a fact by its place, a lock by its id; they share no key. With the
layer as the top-level key and three maps under it, one common act would cost 6
to 8 seeks, against 4. A key is a string made of the printed texts of its ids,
with a space between: one class, and Rama gives the order of string keys, on
which the range read of a request's locks rests. A row is placed by the task on
which its gate writes it, and none by a hash of its whole key. In a streaming
layer that is the layer's task for every row.

**In the streaming gate, one accepted act with one sealed value costs 4 reads
and 4 row writes**, in one event, on one task: the answer, the permission, the
lock, the task's last stamp.

**A permission fact sits on its holder.** The gate then finds the permission of
an act's `:who` with one read. It follows that a holder has one permission in a
layer. Sid should know this one; the other answer changes one rule of the
declaration and the key that the gate reads.

**The gate does not read the permissions above the act's own.** A permission row
is written only by a right making act or by a give whose own permission stood and
is exactly the one above, and nothing in this build ends a permission. Reading
them would cost up to 3 reads on every act to find what cannot be false.

**The class of a layer costs no read for a layer that is made here.** A gate's
record holds rows only for layers of its class, so a row at the permission's
place or at the class's place says that the layer is this gate's.

**How locks are asked for and read.** A request is an act with one fact under
`:lock-request`, on the uuid of its own act, holding a count of 1 to 256. The id
of a lock is `[the request's act-id, index]`, so the door knows the ids before it
sends, and a retry cannot make locks under other ids. A gate makes the bytes at
the write. The door reads all the locks of a request with one call of
`locks-of-request`.

**The cipher.** AES-256-GCM, a random part of 12 bytes for each seal, a tag of 16
bytes, the place `[layer entity key]` as associated data. A sealed value carries
no mark of its way of sealing: the lock's row holds the number of its way, 1 in
this build, and the word for the way lives in [seal.clj](seal.clj). So the way can
change later without a stored value being touched.

**The stamp's packing.** Milliseconds since 1970 × 65,536 + a counter. One
millisecond holds 65,536 stamps for a task; the packing reaches 2^62 in the year
4199. A gate's last stamp is read from the record, by the streaming gate at each
decision and by the microbatch gate once for a batch, and is not kept in memory:
that would be a cache, and the project's rule sends a cache to its own
examination first.

**The one text and the digest.** One function from anything to a string, blind to
what a serialiser may change and no check can see. The digest is SHA-256 over it,
kept whole.

**The form of a making act.** Exactly two facts, in either order: the class on
the layer, and the act's own permission on its holder.

**The source of each gate.** It starts from the beginning of its depot. The
streaming gate's has the retry mode "all after", and nothing is given back in the
ack.

**The first argument of a query routes it.** Rama's client hashes the first
argument and sends the query straight to one task. For `class-of-layer` it is
the layer. For `answer-by-act-id` and `locks-of-request` it is the routing id of
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
- A lock is on the task of its request's uuid, with the request's answer. That is
  the one task that can be found from the lock's id.
- A layer's class and each permission are on every task. Every act needs the
  permission of its `:who` where it is decided. A copy is bytes and a rare write;
  a fetch would be a hop between tasks on every act. That is Sid's reason for
  F5 A, read onto the permissions. A give writes one row on each task.

**Where an act of a batch is decided.** On the task of the entity of its first
fact, where it arrived, when no other act of the batch needs a row that it needs
from another task. Two acts meet when both need one row and cannot be put in
order on one task; they are decided on the task of their layer id, in turn. An
act goes there when it writes a row that every task holds, when the place of its
permission is empty and a give of the batch could fill it, when a home task says
that a row is shared, or when an earlier act of its task that goes there names
one of its own places. In ordinary work nearly every act stays. Sending every act
to the layer's task would be simpler and is what rule 6 says not to do.

**The order of one batch: the turn.** A thing's turn is its position in its depot
partition's part of the batch, times the number of tasks, plus the partition's
number. The decisions of a batch are those of deciding its acts one at a time in
the order of their turns. On one partition that is the depot's order. Between two
partitions it is a fixed rule with no other meaning. The count of positions is in
the gate's PState, not in memory: Rama sets a PState back when an attempt of a
batch fails, so the second attempt gives the same turns.

**The stamp of a batch.** The gate reads the clock once for a batch. The batch's
base is later than the gate's last stamp and than each stamp that an act of the
batch carries, the last held to 10 seconds ahead. An act's stamp is the base plus
its turn. The gate keeps one last stamp, on the task of the class word
`:microbatch`, so each stamp is later than every stamp that the gate gave before
on any task.

**Closing.** Rama makes a batch visible task by task, and it can run a batch
again after one task has shown its part; the second attempt reads the clock again
and makes other lock bytes. So a reader could see a stamp or a lock's bytes that
later change. Each task therefore holds `:closed-through`, and a read gives a row
of a microbatch layer only when the row's stamp is not later than it. Each run of
the gate writes `:closed-through` with the gate's last stamp as it was before the
run. That run began only after every task had committed the batch before, so a
closed batch is visible everywhere and cannot run again. The run after a batch
with things starts at once, so an answer is readable one short run after its
batch. The cost: one more read for an answer that is there, and one small write
on each task for each batch. And one thing that closing leaves to a read of
facts: a place holds one row, its current fact. So after a batch replaces a
fact, its place holds no row of a closed batch until that batch is closed, and a
place that is replaced in every batch never holds one. No read of this build
meets it: the one fact that a read gives is the class of a layer, which is
written once.

**One copy of a lock's bytes.** A lock is asked for before its value is known, so
its home cannot be the task of its value. When a lock comes to seal a value, its
whole row, with the bytes, is written beside the value, on the task of the
value's entity; and the row at its home is written again with the fact that it
seals and no bytes. The bytes are in one row. A later forget destroys one row.
"A second value under a lock that already seals one" is still decided at the
home, where the row names the fact. When the home is also the value's task, the
two rows have one key and the whole row is the one that stays: a task writes
each home's row before any row with bytes. **Sid should know two things.**
`locks-of-request` gives a lock of a microbatch layer with its bytes only while
it seals nothing; the bytes of a lock that seals a value are read beside the
value. And the home's row still costs about 105 bytes for each used lock.

**One accepted act with one sealed value on one entity costs the microbatch gate
3 reads and 4 row writes**: the permission where it arrived, the answer on the
uuid's task, the lock at its home; then the fact, the lock's whole row beside it,
the home's row, the answer. It also costs 6 messages between tasks. Beside
these, each thing that arrives, a stray and a repeat too, costs one read and one
write of its task's count of turns. Each batch adds a few reads that do not
grow with its acts.

**The placing of the checks that the contract does not place.** The 10-second
rule comes after the declaration, before any row is read. What a store key does
not take comes after check (g). The seal comes last: it is the most costly. A
check that examines facts takes them in the act's order.

**How each of the builder's rules is met.** The numbers are the contract's.

1. Each answer keeps the SHA-256 of its thing. A second, different act under a
   used act-id ends at check (a) and writes nothing; its sender reads an answer
   whose digest is not the digest of what it sent.
2. A fact that names nothing to replace reads no row and is written as the row of
   its place. No check can call it stale.
3. A retract is written as the row of its place, with no value. A later fact that
   names it passes check (e).
4. The module sets no trimming option on the depot.
5. A lock's bytes are made by a gate at the write and written only to its locks
   PState. A request holds a count, and no code appends to a depot. Each write of
   a decision is behind check (a), so a second run of an event makes no lock
   again for a value that is sealed. The microbatch gate makes the bytes in step
   4, on the lock's home task. A second attempt of a batch makes other bytes; no
   read gave the first ones, because that batch was not closed.
6. The turn gives two acts that replace one fact the same winner on every
   attempt. An act is decided once, on one task, with each row that it needs in
   hand; the rows of an accept go to several tasks inside one attempt, which Rama
   keeps or drops whole. Only the acts that meet pass one task.
7. A layer's class is a fact at a place found from the layer id; in a microbatch
   layer every task holds it. An act's answer is found from its act-id alone, by
   its routing id. Each gate reads the other gate's record for a layer that its
   own record does not hold and that starts with the other class; no act of this
   build changes a class, so no other layer can be there.
8. Each word is under "Words a session made", below.
9. Found: no, not when the two acts are on two depot partitions. Inside one
   partition the depot holds the order. Between two it holds none: the order in
   which the gate decided two such acts is the batch of each and then the turn,
   and which batch a thing fell into is a fact of timing. A rebuild that reads
   the depot from its start cuts other batches, so the other act can have the
   earlier turn. It matters only for acts that meet from two partitions. What
   would make it the same is a record of each batch's cut and clock, a second log
   beside the depot. Nothing is built for it; whether it is wanted is Sid's.
10. The test for a stray runs before check (a). A stray gets no answer; its digest
    is added to the set of its word, on the task where it arrived, once however
    often it comes. The count is the size of the set. In the microbatch gate that
    is the task where the depot put the thing.
11. The answer's row keeps the digest and `answer-by-act-id` gives it.
    [digest.clj](digest.clj) is the one function. The gate writes nothing for a
    second, different act.
12. Two rules of the declaration: `:place-named-once` and
    `:no-fact-replaces-a-fact-of-its-act`.
13. A second write on a store key's place, each retract under a store key, and a
    `:class` fact after the making are refused, with four words that say which. A
    permission fact sits on its holder.
14. `granted-in` must be the act's layer, in a first permission and in one given.
    The gate checks nothing more about a holder; see "What the gate does not
    check".
15. A refusal writes the answer and no lock row; in the streaming gate also the
    task's last stamp. The sealing function takes a fresh random part at each
    call.
16. The gate opens each sealed value once with its lock's bytes, in the way that
    the lock's row names, and drops the plain bytes. The place is the cipher's
    associated data.
17. An act that carries a stamp more than 10 seconds ahead of the gate's clock is
    refused, before any stamp is made. The microbatch gate holds each act of a
    batch to the one clock reading of the batch.
18. A microbatch act has one stamp, the batch's base plus its turn, on its answer
    and on each of its facts. The gate has one last stamp for all tasks, so the
    stamp is later than each stamp that the gate gave on each task where the act
    writes. "The last stamp of each task" is read as the last stamp that this
    gate gave there; the streaming gate's last stamp of the same task is of
    another record.
19. Nothing is built for a rebuild of the record from the depot. What a rebuild
    would not give again: a lock's bytes, which are in no depot; a stamp, which
    holds the gate's clock at the decision; a refusal by rule 17; the order
    across a failed batch; and, in either record, which of the two words an act
    sent to the wrong depot got, the word for a layer of the other gate or the
    word for a layer that is not made: that rests on an order between the two
    depots that no depot holds. For the microbatch record also: rule 9's answer;
    and the whole row of a lock that seals a value, which is placed from the
    fact's row that names it, and for a value that is no longer current from the
    act that the row names.
20. Five numbers in [declaration.clj](declaration.clj), each held by a rule or by
    the test for a stray, before a row key is built from an id.
21. The aims: 1,000 acts a second on one task; 20 ms from the door's append to the
    answer in the door's hand; 110 bytes of lock or fewer for each value. None is
    measured. By the cost of a cold read, 4 reads give 480 to 770 acts a second on
    one task; the first aim is met only when the rows are read from memory. With
    a lock of 32 bytes for each value, the third does not hold for values that
    average under 55 bytes. For the microbatch gate: 1,000 acts a second arriving
    on one depot partition, which needs a run of the gate inside a second; an
    answer readable within two runs of the gate after the append, its own batch
    and the short one that closes it, not 20 ms, because a microbatch layer is not
    for what a person does directly; and the third limit is not met for values
    under about 90 to 123 bytes, because a used lock has a row at its home beside
    the row with its bytes. None is measured.

## Words a session made, Sid's to change

Each says what the thing is. None comes from the rig's code. Carried from the
contract and the decisions, not made here: `:act-id`, `:class`, `:sealed`,
`:lock-id`, `:permission`, `:lock-request`, `:not-sealed`, `:system`, `:base`,
`:streaming`, `:microbatch`, `:accepted`, `:refused`; "stray", "place", "holder",
"making act". "Class" and "act-id" are Sid's to change, and each word below that
holds one of them follows his word.

The three store keys, `:class`, `:permission` and `:lock-request`, are bare
words: they have no namespace. So in every layer they are the store's. A fact of
a tool under one of them is read as the store's fact and is held to its rules,
whatever the tool meant; a tool cannot have a key of its own with one of the
three words. A namespace on the three would give the bare words back to the
tools. Whether they get one is Sid's.

**Names in Rama.**

| Word | What it is |
|---|---|
| `Store` | The Rama module |
| `:store-module` | The alias of the root `deps.edn` that holds the store's paths, Clojure and Rama |
| `*streaming-depot` | The streaming depot: every thing sent for a streaming layer |
| `streaming-gate` | The stream topology that decides each thing of the streaming depot |
| `$$streaming-answers` | The answer under each act-id of a streaming layer |
| `$$streaming-facts` | The current fact of each place of each streaming layer |
| `$$streaming-locks` | Each lock of each streaming layer, and the fact that it seals |
| `$$streaming-task` | What the streaming gate keeps for one task: its last stamp, its strays |
| `*microbatch-depot` | The microbatch depot: every thing sent for a microbatch layer |
| `microbatch-gate` | The microbatch topology that decides the things of the microbatch depot |
| `$$microbatch-answers` | The answer under each act-id of a microbatch layer |
| `$$microbatch-facts` | The current fact of each place of each microbatch layer |
| `$$microbatch-locks` | Each lock of each microbatch layer, and the fact that it seals |
| `$$microbatch-task` | What the microbatch gate keeps for one task |
| `$$questions`, `$$replies`, `$$parcels` | The three temporary PStates of one attempt of a batch: what step 1 gathered, what step 2 replied, what step 3 sent |
| `answer-by-act-id` | The query: act-id → the recorded answer, or nothing |
| `locks-of-request` | The query: a request's act-id and a range of indexes → those locks |
| `class-of-layer` | The query: layer → its class, or nothing |

**Namespaces**, each `softland.store.` and the word: `text`, `digest`,
`declaration`, `stamp`, `seal`, `first-act`, `decision`, `record`, `batch`,
`record-pstates`, `streaming-gate`, `microbatch-gate`, `door-reads`, `module`.
The table of files says what each is. In `act.clj`, one name is a session's:
`as-shape`, which gives the act's engine to another namespace. The two proofs
are `pure-proof` and `module-proof`.

**Fields of a row, and of an answer as a caller gets it.**

| Word | What it is |
|---|---|
| `:refused` | In an answer's row: the reason word. Absent when the act was accepted |
| `:stamp` | In an answer's row and a fact's row: the stamp of the decision |
| `:digest` | In an answer's row: the digest of the thing that was decided |
| `:value` | In a fact's row: the bytes of the fact's value, sealed or plain. Absent for a retract |
| `:lock-act`, `:lock-index` | In a fact's row: the lock that seals `:value`, as the uuid of the act that asked for it and its index |
| `:by-act`, `:by-index` | In a fact's row: the fact that wrote the row, as the uuid of its act and its index |
| `:bytes` | In a lock's row: the lock's 32 bytes |
| `:way` | In a lock's row, and in a lock as a caller gets it: the number of the way of sealing that the lock is for. 1 in this build |
| `:aes-256-gcm` | In code only, beside the number 1: the word for the one way of sealing that this build knows |
| `:seals-act`, `:seals-index` | In a lock's row: the fact that the lock seals, once it seals one |
| `:last-stamp` | In the task's value: the last stamp that the gate gave on the task. The microbatch gate has one, for the whole gate, on the gate's task |
| `:strays` | In the task's value: by reason word, the digests of the strays seen |
| `:closed-through` | In the microbatch gate's task value: the stamp through which this task's rows are of closed batches. A read gives a row only when its stamp is not later |
| `:turns`, with `:batch` and `:given` | In the microbatch gate's task value: the batch that the count belongs to, as the text of its microbatch id, and how many things of it this task has given a turn |
| `:verdict`, `:reason` | In an answer as `answer-by-act-id` gives it: `:accepted` or `:refused`, and the reason word |

**The words of a stray.** A stray has no answer; its digest is kept under its
word.

| Word | What it is |
|---|---|
| `:no-act-id` | The thing is no plain map, or holds no act-id |
| `:act-id-malformed` | Its act-id is not `[layer class uuid]` |
| `:id-too-long` | The layer slot of its act-id prints to more than 256 characters. The same word is a reason word of the declaration, for every other id |
| `:act-id-of-the-other-gate` | The class slot of its act-id is not the class of the gate where it arrived |
| `:act-id-of-another-layer` | The layer slot of its act-id is not its own `:layer` |

**The reason words of the declaration**, beside `act.clj`'s seven.

| Word | What it is |
|---|---|
| `:class-not-the-act-ids-class` | `:class` is absent, is no class word, or is not the class slot of the act-id |
| `:lock-id-malformed` | `:lock-id` is not `[act-id index]` with an index of 0 to 255 |
| `:too-many-facts` | More than 256 facts in one act |
| `:too-many-based-on` | More than 1,024 entries of `:based-on` |
| `:store-key-sealed` | A fact under a store key carries a seal |
| `:class-fact-not-a-class-word` | A `:class` fact's value is not `:streaming` or `:microbatch` |
| `:permission-fact-not-a-permission` | A `:permission` fact's value is not a permission id |
| `:permission-fact-not-on-its-holder` | A `:permission` fact's entity is not the holder that its permission names |
| `:lock-request-not-a-count` | A `:lock-request` fact's value is not an integer of 1 or more |
| `:too-many-locks-requested` | A request asks for more than 256 locks |
| `:lock-request-not-on-its-act` | A `:lock-request` fact's entity is not the uuid of its act's act-id |
| `:sealed-with-a-plain-value` | A fact carries `:sealed` and a plain `:v` |
| `:seal-incomplete` | A fact carries `:sealed` with no `:lock-id`, or `:lock-id` with no `:sealed` |
| `:sealed-not-bytes` | `:sealed` is not a run of bytes, or holds none |
| `:sealed-value-too-large` | `:sealed` holds more than 65,536 bytes |
| `:place-named-twice` | Two facts of the act are on one entity and key |
| `:replaces-own-fact` | A fact names a fact of its own act in `:replaces` |
| `:lock-named-twice` | Two facts of the act are under one lock |
| `:id-too-long` | An id that the act holds, or a fact's key, prints to more than 256 characters |

**The names in the declaration.** Its shapes: `:arrived-act`, `:arrived-fact`,
`:class-word`, `:lock-id` (the same word as the part of a fact), `:lock-count`.
Its rules, each named for what must hold: `:class-is-the-act-ids-class`,
`:facts-within-the-limit`, `:based-on-within-the-limit`,
`:store-key-fact-has-no-seal`, `:class-fact-holds-a-class-word`,
`:permission-fact-holds-a-permission`, `:permission-fact-sits-on-its-holder`,
`:lock-request-holds-a-count`, `:lock-request-within-the-limit`,
`:lock-request-sits-on-its-act`, `:plain-value-is-sealed`,
`:sealed-fact-has-no-plain-value`, `:seal-is-whole`, `:sealed-is-bytes`,
`:sealed-within-the-limit`, `:place-named-once`,
`:no-fact-replaces-a-fact-of-its-act`, `:lock-named-once`,
`:ids-within-the-limit`. In the table of store keys: `:why`, `:value`,
`:sits-on`, `:setting`, and the three words that say where a fact sits.

**The reason words of the gate.**

| Word | What it is |
|---|---|
| `:carried-stamp-too-far-ahead` | The act carries a stamp more than 10 seconds ahead of the gate's clock |
| `:layer-belongs-to-the-other-gate` | The layer is made, and its class is the other gate's: the act was sent to the wrong depot |
| `:layer-not-made` | The record holds nothing for the layer, and the act is not a right making act |
| `:who-not-the-permissions-holder` | The holder that the act's permission names is not the act's `:who` |
| `:permission-does-not-stand` | The record does not hold the act's permission for its holder |
| `:replaced-fact-not-current` | A fact that the act replaces is not the current fact of its place |
| `:setting-not-on-the-layer` | A fact under a setting key is on an entity that is not the act's layer |
| `:permission-fact-for-another-layer` | A `:permission` fact writes a permission for a layer that is not the act's |
| `:permission-fact-granted-in-another-layer` | A `:permission` fact writes a permission whose `granted-in` is not the act's layer |
| `:permission-fact-not-beneath-the-acts-own` | A `:permission` fact writes a permission that is not straight beneath the one the act acts under |
| `:ending-a-permission-not-decided` | A `:permission` fact with no value: who may end a permission is not decided |
| `:changing-a-class-not-decided` | A `:class` fact, in a layer that is made, with a class that is not the layer's |
| `:store-key-takes-one-write` | A fact under a store key on a place that already holds one |
| `:store-key-retract-not-built` | A `:class` or `:lock-request` fact with no value |
| `:lock-not-made-for-this-layer` | A sealed value's lock is not one that the store made for the act's layer |
| `:lock-already-seals-a-value` | A sealed value's lock already seals an accepted value |
| `:lock-way-not-known` | The way of sealing that the lock's row names is not one that the code knows. No lock of this build can earn it |
| `:lock-does-not-open-the-value` | The lock's bytes do not open the sealed bytes at this fact's place |

**Words between the decision and the gate**, for the rows of one decision:
`:fact`, `:lock`, `:new-lock`, `:answer`, `:last-stamp`.

**The words of a batch**, in the explanations and in the data between
[batch.clj](batch.clj) and the microbatch gate. No reason word and no word of a
stray is new: each refusal of the microbatch gate is recorded with a word of the
lists above. "Batch" and "attempt" are Rama's words.

| Word | What it is |
|---|---|
| turn | The number of a thing in its batch. The acts of a batch are decided in the order of their turns |
| base | The first stamp of a batch. An act's stamp is the base plus its turn |
| deciding task | The task that the hash of the entity of an act's first fact gives. An act that meets no other is decided there |
| home task | The task on which a row lives |
| the gate's task | The task that the hash of the class word `:microbatch` gives. The gate's last stamp is there |
| to meet; meeting task | Two acts of a batch meet when both need one row and cannot be put in order on one task. The meeting task of a layer is the task of the layer id; acts that meet are decided there, in turn |
| pass | The walk of a meeting task over the acts that were sent to it |
| route | An id whose hash gives a task: the uuid of an answer, the entity of a fact, the request's uuid of a lock, the layer of an act that meets |
| entry, `:entry` | What step 1 keeps of a thing for step 3: its turn, the thing, the act in its one form, what was found of its standing |
| `:standing`, with `:stands`, `:final`, `:open` | In an entry: what step 1 found. The permission is there; the act is refused for good; or the permission's place is empty and a give of the batch could fill it |
| question, with `:answer`, `:place`, `:locks`, `:new-locks` | What an act sends to the home task of a row that it needs; the four kinds |
| `:wanted?`, `:own?` | In a question about a place: the fact names something to replace, so the row is needed; the place is on the act's own deciding task |
| reply, with `:yours`, `:used`, `:taken-in-this-batch`, `:shared?` | What a home task sends back. For an answer: the act-id is the asker's to use, is used in the record, or goes to an earlier turn of this batch. For a place or a lock: another act of the batch needs it too |
| `:numbers`, with `:carried` and `:position` | What each task sends the gate's task for the base: the greatest stamp that its things carry, and its greatest position |
| parcel, with `:rows` and `:meet` | Rows that one task sends to another to write, or an act sent to the meeting task |
| `:lock-home` | Beside `:fact`, `:lock`, `:new-lock` and `:answer`, for a row on its way to its home: the home's row of a lock, with no bytes |
| `:home`, `:every-task?` | In a row on its way: the route of its home; and that every task holds it |
| closed | A batch is closed when every task has made it visible and the next run of the gate has begun. Rama cannot run it again |
| routing id | The first argument of `answer-by-act-id` and `locks-of-request`: the layer of a streaming act-id, the uuid of a microbatch act-id |
| the home's row of a lock | The row of a lock on the task of its request's uuid. Once the lock seals a value it holds the way and the fact, and no bytes |

**Marks in the one text**: `#bytes`, `#double`, `#keyword`, `#other`,
`#too-deep`, `#unreadable`. [text.clj](text.clj) says what each marks.
