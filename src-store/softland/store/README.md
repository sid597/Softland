# Store — the act, and the store's module

[Parent: namespace root](../README.md) · [Store source entry](../../README.md).

This folder holds two things. The act: the one form of what a maker gives to the
store, and the check of that form. And the store's Rama module, `Store`: the
streaming depot, the streaming gate that decides each thing on it, the record
that the gate keeps, and the three reads by which the door reads the module. An
act that the gate accepts becomes facts in the record, and its answer is read
back by act-id.

The module works with layers of the class streaming. The base is a microbatch
layer; nothing in this folder can make it yet.

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
| [record.clj](record.clj) | The record's rows: the text of each key, the making and the reading of each row, and the test of what a caller gave a read. | None. It holds no record. |
| [decision.clj](decision.clj) | The checks of one act against rows of the record, and the rows that a decision writes, as data. | None. It reads no PState. |
| [record_pstates.clj](record_pstates.clj) | The record as PStates: the four schemas, written once. | None. It declares nothing. |
| [streaming_gate.clj](streaming_gate.clj) | The stream topology `streaming-gate` and the dataflow of one event. | It owns the four PStates of the streaming record, durable in the cluster. Nothing in the memory of a process. |
| [door_reads.clj](door_reads.clj) | The three query topologies: the answer by act-id, the locks of a request, the class of a layer. | None. It borrows the gate's PStates to read. |
| [module.clj](module.clj) | The module `Store`: the streaming depot, and the calls that add the gate and the reads. | It owns the streaming depot, durable in the cluster and never trimmed. |

The first nine are pure code and load with Clojure alone. The last four require
Rama.

## How the parts relate

A thing is appended to the streaming depot, which puts it on the task of its
`:layer`. The gate takes it there, in one event:

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

The door reads through [door_reads.clj](door_reads.clj). Each read goes straight
to the layer's task.

The gate's dataflow only reads rows, calls the pure functions and writes rows. A
pure function gives the same answer when Rama runs an event again, never throws
on what arrived, and is proven without a cluster.

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
- **Where a thing sits.** The depot's partitioner puts a thing on its layer's
  task, and the gate makes no hop. Every client's append goes through that
  partitioner. A topology can append to the partition of the task that it is on,
  whatever the partitioner; no topology of this build does. A thing that a
  topology appends to another partition is decided against that task's record. It
  can be refused as an act into a layer that is not made, and the answer of that
  refusal is then on that task, where no read by its act-id looks. Or, when it is
  a making act, it can be accepted there and leave rows and an answer on that task
  that no read finds. The rows of the true layer, on its own task, are not
  touched.
- **The order after a failure.** Without a failure the acts of a layer are decided
  in the depot's order. When something throws while a batch of events runs, that
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
| F6 A, Q7 a | Two depots, one for each gate: the streaming depot and the microbatch depot | [module.clj](module.clj) declares `*streaming-depot` |
| F2 A | The record is in the gates' PStates; the acts depots are the replay log | [streaming_gate.clj](streaming_gate.clj) owns the record; the depot is never trimmed |
| F1 A | The class of a layer is a fact on the layer | The row at the layer's own entity under `:class` ([record.clj](record.clj)) |
| F4 C | The base is a microbatch layer; every other layer starts streaming | `class-at-making` in [first_act.clj](first_act.clj) |
| F5 A | A gate reads on its own task | The gate's event has no hop; a layer holds facts under its own entity |
| Q22 A | The door reads an answer from the record by act-id; the ack is a signal | `answer-by-act-id` in [door_reads.clj](door_reads.clj); the gate gives nothing back in the ack |
| F7 A | A signal is not the truth; the record is; a gate reads its own record by act-id before it decides | Check (a), the first read of each event |
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
which the gate writes it, the layer's task, so each read routes by the layer and
none by a whole key.

**One accepted act with one sealed value costs 4 reads and 4 row writes**, in one
event, on one task: the answer, the permission, the lock, the task's last stamp.

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
sends, and a retry cannot make locks under other ids. The gate makes the bytes in
its event, at the write. The door reads all the locks of a request with one call
of `locks-of-request`.

**The cipher.** AES-256-GCM, a random part of 12 bytes for each seal, a tag of 16
bytes, the place `[layer entity key]` as associated data. A sealed value carries
no mark of its way of sealing: the lock's row holds the number of its way, 1 in
this build, and the word for the way lives in [seal.clj](seal.clj). So the way can
change later without a stored value being touched.

**The stamp's packing.** Milliseconds since 1970 × 65,536 + a counter. One
millisecond holds 65,536 stamps for a task; the packing reaches 2^62 in the year
4199. The task's last stamp is read from the record at each decision and is not
kept in memory: that would be a cache, and the project's rule sends a cache to
its own examination first.

**The one text and the digest.** One function from anything to a string, blind to
what a serialiser may change and no check can see. The digest is SHA-256 over it,
kept whole.

**The form of a making act.** Exactly two facts, in either order: the class on
the layer, and the act's own permission on its holder.

**The source of the gate.** It starts from the beginning of the depot, with the
retry mode "all after". Nothing is given back in the ack.

**Each query takes the layer as its first argument**, so that Rama's client sends
the query straight to the layer's task.

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
5. A lock's bytes are made in the gate's event and written only to the locks
   PState. A request holds a count. Each write of a decision is behind check (a),
   so a second run of an event makes no lock again for a value that is sealed.
6. For the microbatch gate; not built.
7. A streaming layer's class is a fact at a place found from the layer id. A
   streaming act's answer is found from its act-id alone. The gate has the step
   that asks whether the other gate's record holds a layer; it gives "no" in this
   build.
8. Each word is under "Words a session made", below.
9. For the microbatch gate; not built.
10. The test for a stray runs before check (a). A stray gets no answer; its digest
    is added to the set of its word, on the task where it arrived, once however
    often it comes. The count is the size of the set.
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
15. A refusal writes the answer and the task's last stamp, and no lock row. The
    sealing function takes a fresh random part at each call.
16. The gate opens each sealed value once with its lock's bytes, in the way that
    the lock's row names, and drops the plain bytes. The place is the cipher's
    associated data.
17. An act that carries a stamp more than 10 seconds ahead of the gate's clock is
    refused, before any stamp is made.
18. For the microbatch gate; not built. The next stamp takes the last stamp as a
    number.
19. Nothing is built for a rebuild of the record from the depot. What a rebuild
    would not give again: a lock's bytes, which are in no depot; a stamp, which
    holds the gate's clock at the decision; a refusal by rule 17; and the order
    across a failed batch.
20. Five numbers in [declaration.clj](declaration.clj), each held by a rule or by
    the test for a stray, before a row key is built from an id.
21. The aims: 1,000 acts a second on one task; 20 ms from the door's append to the
    answer in the door's hand; 110 bytes of lock or fewer for each value. None is
    measured. By the cost of a cold read, 4 reads give 480 to 770 acts a second on
    one task; the first aim is met only when the rows are read from memory. With
    a lock of 32 bytes for each value, the third does not hold for values that
    average under 55 bytes.

## Words a session made, Sid's to change

Each says what the thing is. None comes from the rig's code. Carried from the
contract and the decisions, not made here: `:act-id`, `:class`, `:sealed`,
`:lock-id`, `:permission`, `:lock-request`, `:not-sealed`, `:system`, `:base`,
`:streaming`, `:microbatch`, `:accepted`, `:refused`; "stray", "place", "holder",
"making act". "Class" and "act-id" are Sid's to change, and each word below that
holds one of them follows his word.

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
| `answer-by-act-id` | The query: act-id → the recorded answer, or nothing |
| `locks-of-request` | The query: a request's act-id and a range of indexes → those locks |
| `class-of-layer` | The query: layer → its class, or nothing |

**Namespaces**, each `softland.store.` and the word: `text`, `digest`,
`declaration`, `stamp`, `seal`, `first-act`, `decision`, `record`,
`record-pstates`, `streaming-gate`, `door-reads`, `module`. The table of files
says what each is. In `act.clj`, the one new name is `as-shape`. The two proofs
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
| `:last-stamp` | In the task's value: the last stamp that the gate gave on the task |
| `:strays` | In the task's value: by reason word, the digests of the strays seen |
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
| `:layer-belongs-to-the-other-gate` | The layer is made, and its class is the other gate's. No act of this build can earn it |
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

**Marks in the one text**: `#bytes`, `#double`, `#keyword`, `#other`,
`#too-deep`, `#unreadable`. [text.clj](text.clj) says what each marks.
