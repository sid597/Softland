# The Softland backend on Rama: the architecture

Date: 2 October 2026.
Language: ASD-STE100 (Simplified Technical English).
Status: this document is not a ruling. Only Sid makes decisions. Section 9
gives the decisions that are open.

## 0. About this document

### 0.1 What this document is

One session drew this architecture with Sid on 2 October 2026. The first
drawing is in `ARCHITECTURE.md` in this folder.

Two recheck sessions examined that drawing on the same day. They compared
each statement with its source. They also did two runs on Rama.

This document gives the full architecture, with all the corrections from the
two recheck sessions. Section 8 gives each correction and its source.

This document does not change `ARCHITECTURE.md`.

### 0.2 Marks

Each statement has a mark. The mark tells you the source of the statement.

| Mark | Source |
|---|---|
| [RULED] | A ruling by Sid: the "Now" section of `src/proposal/frame-2026-09-15/PROGRESS.md` on the branch `main`. |
| [PROPOSAL] | Sid's proposal: `src/proposal/frame-2026-09-15/PICTURE.md` and `PARTS-4-8.md`. |
| [SETTLED] | `docs/decisions.md`. |
| [SID] | Words that Sid said in the session of 2 October 2026, or wrote on Sid's card of 29–30 September. A recheck session found them in the record of the session. |
| [VISION] | `docs/carry-on.md` or `vision/LOG.md`. |
| [RAMA DOCS] | The local copy of the Rama documents: `reference/rama/docs/`. |
| [RAN 25 SEP] | The Rama check of 25 September 2026: `src/proposal/rama-check-2026-09-25/RESULTS.md` on `main`. |
| [RAN PROBE] | The depot-append probe of 2 October 2026, by recheck session 2. Section 7.2 gives the results. |
| [RAN CHECK] | The append check of 2 October 2026, by the other recheck session. Section 7.3 gives the results. |
| [RIG] | The code or the notes of the rig (`src/proposal/rig-2026-09-25/`), or the code of Inland (`src-inland/`). |
| [DRAWING] | A statement of the drawing of 2 October that the recheck sessions did not examine. |
| [DERIVED] | A conclusion of a session. It does not come from a source. Examine these statements first. |
| [OPEN] | A question that nobody decided. Section 9 gives the list. |

When a statement has two or more sources, the marks are in one bracket, with
"+" between them.

### 0.3 The language

This document obeys the rules of ASD-STE100:

- One sentence has one topic.
- A sentence has 25 words or fewer.
- The verbs are in the active voice where possible.
- A word is used for one thing only.

Section 1 gives the technical names and the technical verbs. This document
uses each one only as section 1 tells.

The writer did not have the STE dictionary. Thus, the writer did not compare
each word with the dictionary.

## 1. Technical names and technical verbs

### 1.1 Data

**Entity.** An id. A fact is about an entity. An entity has no other property.
[PROPOSAL]

**Key.** The part of a fact that tells what the fact says about its entity. A
key is an id. The word and the grammar of a key are facts about the key.
[RULED] In this document, "key" never means an encryption key.

**Value.** The part of a fact that holds the data.

**Fact.** An entity, a key, a value, and the version that the fact replaces, if
it replaces one. [RULED]

**Act.** A set of one or more facts. A gate accepts the full set, or refuses
the full set. An act writes one layer. [RULED] On 2 October, Sid changed the
name "offer" to "act". [SID]

**Name.** The id of an act. The door makes the name at random, before the gate.
The name tells the layer and the class of layer that the act is for. [RULED]

**Answer.** The result of a gate for an act: accepted, or refused with a
reason. The gate records the answer under the name of the act. [RULED]

**Stamp.** The time that a gate puts on an act that it accepts. [RULED]

**Basis.** The facts, with their versions, that an act converter read before it
made an act. The act holds its basis one time. [RULED]

**Read entry.** The record of one read: what the reader read, at which moment,
and the role of the read. [RULED: ruling 3]

**Grammar.** A fact that gives the shape of the values for a key. [PROPOSAL]

**Permission.** A fact that lets an actor write in a layer. A permission is in
the layer that it controls. [RULED]

**Lock.** An encryption key. Each value has its own lock. [RULED]

**Subjects.** The people that an act is about. [RULED: ruling 8]

### 1.2 Layers

**Layer.** A copy that has members. A layer tells whose version of things it
holds, who can see it, and who can write it. [SID]

**The kinds of layer by use.** The base, a group layer, a personal layer, and a
session layer. A session layer is a hand layer (for a person) or an agent layer
(for an agent). [RULED]

**Stream layer.** A layer that the stream gate controls. All of its data is on
one task. [SID]

**Microbatch layer.** A layer that the microbatch gate controls. Its data is on
all tasks, divided by entity. [SID]

**Class.** A fact on a layer that tells if the layer is a stream layer or a
microbatch layer. [RULED: ruling 2 + SID]

**Home task.** The one task that holds a stream layer. Rama finds it from the
hash of the id of the layer.

**Era.** The part of a layer that one gate wrote. A layer that had a re-class
has two eras. [RIG]

### 1.3 The parts of the store

**The store.** All the parts of Softland that keep, examine, and give data. The
store has two parts: the runtime and the module.

**The fixed part.** The code of the store. It is not data. Sid's proposal calls
it "the one tool that is not a fact". The version of the fixed part is a fact.
[PROPOSAL] The fixed part has two parts: the runtime and the code of the
module. [PROPOSAL: PARTS-4-8.md:323 + DERIVED]

**The runtime.** The program that connects the outside to the module. It is the
only Rama client. [DERIVED]

**The module.** The one Rama module. It holds the acts depots, the gates, the
PStates, and resolve. [SID]

**Act converter.** Data that changes something that occurred into zero, one, or
more acts. [SID]

**Inside inlet.** The act converters for things that occur in the store: a
gesture, a save, a rule. These act converters are tools. [PROPOSAL]

**Outside inlet.** The act converters for things that come from the outside: a
commit, a reply, a tick. These act converters are adapters. [PROPOSAL]

**Inward return.** The step of the outside inlet that makes the identity, the
address, and the basis that the outside does not supply. [PROPOSAL]

**The door.** The part of the runtime that puts each act in its envelope and
appends the act to an acts depot. [DERIVED]

**Gate.** A topology that decides acts. Only a gate writes the record.
[PROPOSAL + RULED]

**Stream gate.** The gate of the stream layers. It is a stream topology.
[RULED]

**Microbatch gate.** The gate of the microbatch layers. It is a microbatch
topology. [RULED]

**The record.** The facts and the answers that the gates write. [DERIVED + OPEN:
D2]

**Resolve.** The one exit for reads. [PROPOSAL]

**Bell.** A signal to the runtime that something changed. A bell carries a
stamp, a frontier, or a date of erasure. A bell carries no fact and no value.
[RIG + DERIVED]

**Feed.** A depot that a gate appends to after it commits. A feed is a bell for
topologies. [DERIVED]

**Crossing out.** Data that goes from the store to the outside: paint, enact,
prompt, send. [PROPOSAL]

**Promotion.** The move of a value from one layer into another layer. A
promotion is two acts. [RULED]

**Crossing fact.** The fact that the source gate writes when it reads a value
out for a promotion. [RULED]

**Landing.** The act that puts a promoted value into the target layer. [RULED]

**Re-class.** A change of the class of a layer. [RULED: ruling 2]

**Maintain step.** The step that runs a tool again when a fact that the tool
read changes. [PROPOSAL]

**Placement rule.** A fact that tells which indexes the store makes for a kind
of fact. [PROPOSAL]

**Primitive.** A small, general part of the fixed part that touches the
machine. Examples: bytes, files, processes, the GPU, the clock, random numbers,
cryptography, input. [SID]

**Base function.** Data that does a general job, for example a parser. [SID]

**Actor.** A person or an agent that sends acts. [RULED]

**Envelope.** The parts of an act around its facts. Section 4 gives them.
[RULED]

**Policy.** The facts that tell who can see what, and who can write what.
[PROPOSAL]

**Grant.** A permission for a program to use a primitive. [SID]

**Evaluator.** The part of the fixed part that runs data, for example act
converters. [SID]

**Provenance.** Who made a fact, and from what. [PROPOSAL]

**Ingest.** The act converter that reads the files of the host. [PROPOSAL]

**Ruling.** A decision that Sid made and recorded. The rulings are in the "Now"
section of `PROGRESS.md` on `main`. [RULED]

**The drawing.** The architecture that one session drew with Sid on 2 October
2026: the file `ARCHITECTURE.md` in this folder. The drawing session is the
session that made it.

**Lease.** In the rig: a row that gives a session the use of a value lock.
[RIG]

**Fold.** In the rig: one calculation on task 0 that decides all the acts of a
batch, in order. [RIG]

### 1.4 Rama

**Depot.** A Rama log. A client or a topology appends records to it. [RAMA
DOCS]

**Acts depot.** A depot that the door appends acts to. There are two: the
stream acts depot and the microbatch acts depot. [DERIVED]

**PState.** A Rama store of data. Only one topology writes a PState. [RAMA DOCS]

**Private PState.** A PState that no client can read. The topologies of the
same module can read it. [RAMA DOCS: 15-pstates.md:222–228 + RAN CHECK]

**Task.** One division of the module. The module has N tasks. [RAMA DOCS]

**Topology.** Rama code that reads depots and writes PStates. A query topology
only reads. [RAMA DOCS]

**Event.** The work that a stream topology does on one task, between two
partitioners. [RAMA DOCS: 23-acid-semantics.md:52]

**Batch.** The work that a microbatch topology does on all tasks, for many
records together. [RAMA DOCS]

**Proxy.** A client object that receives each change to one path in a PState.
[RAMA DOCS: 15-pstates.md:436]

**Ack.** The reply that a client gets after an append. [RAMA DOCS]

**Atomic.** All the writes occur together, or no write occurs. [RAMA DOCS]

**Attempt.** One time that Rama runs an event or a batch. After a failure, Rama
makes a new attempt. [RAMA DOCS]

**Checkpoint.** The point in a depot up to which a topology recorded its
progress. [RAMA DOCS]

**Worker.** A process that runs tasks. [RAMA DOCS]

**Partition.** The part of a depot or of a PState that is on one task. [RAMA
DOCS]

**Partitioner.** The code that selects the partition for a record. [RAMA DOCS]

**Frontier.** A value that moves when a batch is visible on all tasks. [RIG]

### 1.5 Technical verbs

| Verb | What it is |
|---|---|
| append | To add a record at the end of a depot. |
| hash | To calculate a number from an id. The number selects a task. |
| query | To read through a query topology. |
| decide | For a gate: to accept or refuse an act, and record the answer. |
| refuse | For a gate: to not accept an act. The answer gives the reason. |
| commit | For Rama: to make the writes of an event or of a batch visible and permanent. |
| seal | To encrypt a value with its lock. |
| wrap | To encrypt a lock with the lock of a person. |
| ring | For a bell: to tell the runtime that something changed. |
| promote | To move a value into another layer with a promotion. |
| retract | To add a fact that cancels an earlier fact. A person can undo a retract. [RULED] |
| forget | To destroy a lock. Nobody can read the values under that lock again. [RULED] |
| excise | For the operator: to remove the bytes of a value from the record. [RULED] |
| merge | For resolve: to make one set of facts from two sets, by the rules of resolve. |

## 2. The picture

```
 OUTSIDE        screens · minds (people, models) · the host (git, builds, disks) · other instances
                  gestures, replies, commits, ticks ▼          ▲ paint, prompts, enactments, sends
 ─────────────────────────────────────────────────────────────────────────────────────────────────────
 THE RUNTIME    sign-in        Google tells who the person is · ?4 who the person is in the store
 the only       act converters data: tools (inside inlet) and adapters (outside inlet), made from
 Rama client ·                 base functions (data) and primitives (code) · the clock and
 part of the                   random numbers: for outside tools only
 fixed part     inward return  makes the identity, the address and the basis of an outside thing
                the door       for each act: makes the name, puts on the envelope, reads the
                               class ③ of the layer again, appends to the acts depot of that class
                every read     queries resolve · appends a read entry (an act in the hand layer
                               of the reader) · shows the result only after that act is accepted
                match, tools,  ?2 where they run: in the module (a feed), in the runtime (bells),
                maintain step  or on a budget · after each signal, they read the answer by name
                bells          proxies on the bell PStates · a ring is not a read: after a ring,
                               the runtime reads again through resolve
                crossings out  paint · enact · prompt · send
                       │ append                          │ query                    ▲ proxy
                       │ (acts, read entries)            │ (resolve)                │ (bells)
 ─────────────────────────────────────────────────────────────────────────────────────────────────────
 THE MODULE     ③ the class of each layer: stream or microbatch · a fact on the layer · the
 one module ·      members set it by the policy of the layer · ?5 the class at the start
 N tasks ·      a re-class: the stream era stops · the microbatch gate reads it as history ·
 part of the       the gate refuses an act for the old class
 fixed part                        ┌──────── landing of a promotion ────┬──────────────────────┐
                                   ▼                                    ▼                      │
                ACTS DEPOTS   stream acts depot                  microbatch acts depot         │
                              hash of the layer id               hash of an entity id (?7)     │
                              each act as sent, until a trim (?3) · a client can read them     │
                                   │                                    │                      │
                                   ▼                                    ▼                      │
                GATES         stream gate                        microbatch gate               │
                              one event on the home task         one batch on all tasks        │
                              at least once: it reads its        exactly once for PStates      │
                              answer by name first               rig: all decisions in one     │
                              the answer by name, in             ordered fold on task 0        │
                              milliseconds · the ack is          visible when the slowest      │
                              not the answer                     task is done                  │
                              both examine: shape, expected versions, permission, class        │
                              both write the record · a refusal is data, not an exception      │
                              promotion: the source gate reads the value out, commits the      │
                              crossing fact with the landing in it, then appends the landing ──┘
                              from a microbatch layer: only what the previous batch committed (?10)
                                   │                                    │
                                   ▼                                    ▼
                PSTATES       stream layers                      microbatch layers
                (private)     each on its home task              divided by entity · settings
                                                                 and permissions on the task
                                                                 of the layer
                              the record (D2): facts and answers by name · current versions ·
                              permissions · grammars (?13) · indexes from placement rules (?13) ·
                              read entries · locks: own rows (personal, hand) or in the record ·
                              the lock of each person on each task · a clock on each task ·
                              a re-classed layer: the stream era and the microbatch era
                BELL PSTATES  not private · stamps, frontiers and dates of erasure only (?11)
                FEED (?2)     names and stamps only, appended after a commit
                                   │                                    │
                QUERY              └───────────────► resolve ◄──────────┘
                TOPOLOGIES    the one exit · who can see what · the nearer layer wins · a status
                              on each read · the moment of each layer (?6) · two eras merged
                              as built: one query for each layer · the nearer layer is found in
                              the runtime (Inland) · one query for a full context: not built (?16)
```

The picture shows three items that nobody built yet: private PStates, bell
PStates, and a feed. Section 9 gives them as open items ?11 and ?2. [DERIVED]

## 3. The parts, from the outside to the inside

### 3.1 The outside

The outside is all that is not the store. [PROPOSAL] It has these parts:

- screens,
- minds: people and models,
- the host: git, builds, and disks,
- other instances of Softland.

Gestures, replies, commits, and ticks come in from the outside. Paint, prompts,
enactments, and sends go out to the outside. [PROPOSAL]

### 3.2 The runtime

The runtime is the only Rama client. No other program connects to the module.
[DERIVED]

The runtime is a part of the fixed part. The code of the module is also a part
of the fixed part. The version of each part is a fact. [PROPOSAL: PARTS-4-8.md:323]

#### 3.2.1 Sign-in

Google tells the runtime who the person is. [DRAWING]

At the first sign-in, the store makes a person and the personal layer of that
person. The id of a person in the store is open (?4). [DRAWING + OPEN]

At each sign-in, the store starts a session. The session is the root of all the
permissions of that person in that session. A narrower permission is a fact
under the session. [RULED]

The policy that tells which adapters can start a session is a fact. [DRAWING]

#### 3.2.2 Act converters

An act converter is data. [SID: a position, not a ruling]

An act converter works in these steps:

1. The runtime finds the act converters whose patterns match what arrived. The
   patterns are facts. [PROPOSAL]
2. The act converter reads facts from the store, through resolve. [PROPOSAL]
3. The act converter makes zero, one, or more acts. Each act goes into one
   layer. [RULED]
4. The facts that the act converter read become the basis of each act. [RULED]
5. If the act converter makes a series of acts, the door names all of them
   before it sends the first act. [RULED]

There are two inlets: the inside inlet and the outside inlet. The acts from both
inlets go to the same gates. [PROPOSAL]

These sources are examples. The list is not complete. [DRAWING]

| Source | What arrives | What the act converter does |
|---|---|---|
| The screen | A local signal. A local signal is never a fact before a rule tells what it is. | The hit-test changes the point into an address: which thing, at which version. A rule, which is a fact, tells what the point means here. A tool, which is a fact, makes the act. An editor's save and the reader are also act converters of the screen. [PROPOSAL] |
| The store itself | A new fact that a rule waits for. | The rules whose patterns ask for that kind of fact make acts. A long activity reports its status with acts. [PROPOSAL] |
| The files of the host | Files and lines, with no ids and no basis. | Ingest cuts the files into things, and makes their ids. It keeps the link between the versions of a thing by name and hash. The basis is the commit. A transcript thread is an entity, and each turn is an act. [PROPOSAL + RULED] |
| Minds | A reply, with no reads of its own. | The basis of a reply is what the mind was shown. The runtime records that when it sends the prompt. [PROPOSAL] |
| The work of the host | An outcome, or a moment. | An outcome says "check passed at revision R", or it tells the revision that runs now. A tick gives the time as an act from outside. [PROPOSAL] |
| Google | A signed token. | Sign-in. [DRAWING] |
| Other instances | Facts that another instance made. | Their facts come in as acts. This door makes their provenance. [PROPOSAL] |

#### 3.2.3 Time and random numbers

A leaf of a tool must not read a clock, a random number, or a thing outside
that can change. A tool that needs those is an outside tool, and it gives only acts.
[PROPOSAL: PARTS-4-8.md:78 + PICTURE.md:95]

Time comes into the store in two ways only: [PROPOSAL: PARTS-4-8.md:72–74]

- as the stamp that a gate puts on an act,
- as an act from outside, for example a tick.

Thus, the clock and the random numbers are primitives for outside tools only.
[DERIVED]

The fixed part can use the clock and random numbers. For example, the gate
stamps acts, and the door makes names at random. [RULED + DERIVED]

#### 3.2.4 Base functions, primitives, and security

A base function is data. Sid writes the first base functions, as the first user
of Softland. Other people use them, or change copies of them, as in open source.
[SID: a position, not a ruling]

A primitive is code. It is small and general. Each primitive runs under a grant,
and each primitive records what it read. [SID: a position, not a ruling]

The fixed part also holds the evaluator and the guarantees: the gates, resolve,
the record, the locks and forget, the budgets, and the grants. [SID: a position,
not a ruling]

A new primitive is necessary only when a receipt, a measurement, or a guarantee
shows it. The rate of new primitives is the gauge of the cut between data and
code. [PROPOSAL: PICTURE.md:122]

For security, Sid named security reviews and formal proofs, with Lean or Bend.
[SID] The statement "capabilities first, then proofs" came from the drawing
session, not from Sid. [SID: the record] Bend runs pure code in parallel. It is
not a tool for proofs. [DERIVED]

The position "all converters and base functions are data" touches two lines of
the settled ground and of the proposal. Open item ?8 gives them.

#### 3.2.5 The door

All acts go through the door, from both inlets. [DERIVED]

For each act, the door does these steps:

1. It makes the name of the act at random. The name tells the layer and the
   class. [RULED]
2. It puts the act in the envelope. Section 4 gives the envelope. [RULED]
3. It reads the class of the layer. It reads the class again for each act, and
   it does not keep it. [RIG: micro_client.clj:455–466]
4. It appends the act to the acts depot of that class. [DERIVED]

If the PStates are private, step 3 reads through a query topology. [DERIVED]

The act converter already reads the layer for the permission and the expected
versions. Thus, the class can come with those reads, in the basis. This removes
one read for each act. [DERIVED]

If the class changes after step 3, the gate refuses the act. The reason is
"class mismatch". The runtime then sends the act again, with a new name.
[RULED + RIG]

The door appends directly to the acts depot of the gate. Rama does not move a
record from one depot into another depot. [RAMA DOCS]

The ack of an append waits only for the stream topologies that read that depot.
[RAMA DOCS: 14-depots.md:206] Thus, if the door appends to a different depot
first, the answer of the gate does not come back on the ack.

#### 3.2.6 Every read

Each read goes through resolve. [PROPOSAL]

For each read, the runtime does these steps: [RULED: ruling 3 + RIG: read_exit.clj:4–6]

1. It queries resolve.
2. It appends a read entry. The read entry is an act in the hand layer of the
   reader.
3. It shows the result only after the gate accepts that act. If the gate refuses
   the act, the runtime does not show the result.

Thus, each read costs one decision of the stream gate. [DERIVED]

A query topology cannot write. Thus, the runtime, not resolve, appends the read
entry. [RAMA DOCS + RIG]

Each act that the stream gate accepts moves the bell of its layer. Read entries
are also acts. [RIG: gate_event.clj:90] Nobody examined if a reader that watches
its own hand layer rings itself with its own read entries. [OPEN: ?12]

#### 3.2.7 Bells

A proxy is the only method that Rama has to push a change to a client. [RAMA
DOCS: 15-pstates.md:436] A query topology cannot have a proxy. [RAMA DOCS:
13-query-topologies.md:175]

A proxy reads a PState directly. Thus, a proxy that carries a value is a read
that does not go through resolve. [DERIVED]

Thus, the bells obey these rules:

- A bell carries only a stamp, a frontier, or a date of erasure. [RIG + DERIVED]
- A bell is not a read. After a bell rings, the runtime reads again through
  resolve. [RIG: bell.clj:15–19]
- The bells are in bell PStates, which are not private. The record is in
  private PStates. [DERIVED + OPEN: ?11] Thus, Rama itself makes sure that no
  client reads the record around resolve.

In the rig, the bell of a person watches the full entry of that person. That
entry holds the lock of the person. Thus, the lock goes to the runtime. [RIG:
bell.clj:68 + locks.clj:1083–1086] A bell on the date of erasure only is
sufficient. [DERIVED]

#### 3.2.8 Crossings out

The crossings out are paint, enact, prompt, and send. [PROPOSAL]

A crossing out that comes back comes through the inward return. There, the
store makes the identity, the address, and the basis. [PROPOSAL]

### 3.3 The module

#### 3.3.1 One module

All the acts depots, the gates, the PStates, and resolve are in one Rama module.
[SID: agreed on 30 September + not ruled]

The module has N tasks. N does not change after the start. [RULED + RAMA DOCS:
19-operating-rama.md:483] Choose N for the next two years. [RULED]

#### 3.3.2 The three switches, and the class of a layer

Three switches control how an act goes into the store. Sid agreed to them on 2
October. [SID]

1. **What the act says.** The act adds a fact, or it names the version that it
   replaces. The tool sets this, for each act.
2. **Which layer the act goes into.** The tool or the person sets this, for each
   act.
3. **The class of the layer.** The class is stream or microbatch. The members of
   the layer set it, by the policy of the layer. The class is a fact on the
   layer. It stays until a re-class.

Sid did not agree to a class at the start for any layer. That is open item ?5.
[SID]

Switch 3 changes these ruled lines of 24 September: [RULED]

- Ruling 1: "stream for one-owner layers, microbatch for shared ones".
- Ruling 2: placement by layer for one-owner layers, by entity for shared ones.
- "Layer kinds": "Group layers are shared: microbatch gate, by entity".
- The first of "Two consequences": a promotion into a group layer or the base
  goes from the stream gate to the microbatch gate.

Sid's reason on 2 October: shared work also needs "own work feels instant".
[SID] Sid did not yet say that switch 3 replaces these lines. That is decision
D1. [OPEN]

The class is for a full layer, not for each act. Rama lets only one topology
write a PState. If one layer has acts in two gates, the layer has two
orders. [RAMA DOCS + DERIVED]

An act that links two canvases is one fact in one layer. Each canvas sees the
link when it reads. [RULED: "an act writes one layer" + DERIVED]

#### 3.3.3 The acts depots

There are two acts depots, one for each gate: [DERIVED]

- **The stream acts depot.** Rama hashes the id of the layer. Thus, all the acts
  of one stream layer go to its home task.
- **The microbatch acts depot.** Rama hashes the id of an entity. Thus, the acts
  of one microbatch layer go to all tasks. Which entity, when an act is about
  many entities, is open item ?7.

Two depots is a choice. One depot with a custom partitioner is also possible.
[RAMA DOCS: 14-depots.md:42–58, 121–125] With one depot, each gate must read and
discard the acts of the other gate.

The acts depots keep each act as the sender sent it, until a trim by count. A
partition with few acts can wait a long time for a trim. [RULED + RAMA DOCS:
14-depots.md:314–327]

A client can read a depot. Rama cannot make a depot private. [RAMA DOCS + RAN
CHECK] Thus, a value in an acts depot is safe only if the door seals it (?3), or
because the runtime is the only client.

A module update can change a depot record, or excise it. [RAMA DOCS:
14-depots.md:288–312]

#### 3.3.4 The stream gate

- It decides one act in one event, on the home task of the layer. [RAMA DOCS +
  RAN 25 SEP]
- An event is atomic on its one task. [RAN 25 SEP]
- Rama can do an event two or more times. [RAN 25 SEP + RAN PROBE] Thus, before
  it decides, the gate reads the record for the name of the act. If an answer is
  there, the gate gives that answer again. [RIG: gate_event.clj:123–128 + RAN
  PROBE: Q2]
- The answer comes in milliseconds. The runtime finds the answer by name. [RULED]
- The ack is not the answer. The ack can be an error for an act that the gate
  accepted. [RAN 25 SEP + RAN PROBE + RAN CHECK]
- The gate never throws an exception. A refusal is data. [RAN 25 SEP] An
  exception stops the worker. Rama then does again all the records after the
  last checkpoint, also the records that had an ack. [RAN 25 SEP + RAN PROBE]
- The work for one act must be much shorter than the time limit of the event. If
  it is not, Rama starts the event again while the first event still runs. [RAN
  25 SEP]

#### 3.3.5 The microbatch gate

- It decides many acts in one batch, on all tasks. [RAMA DOCS]
- The PState writes of a batch occur exactly one time. [RAMA DOCS + RAN 25 SEP]
- No write of a batch is visible before the commit. [RAN 25 SEP]
- The batch is visible only when the slowest task is done. This applies to all
  microbatch layers together. [RAMA DOCS + RAN PROBE: Q5]
- During the commit, a reader can see one task with the new batch and another
  task without it. [RAN 25 SEP]
- In the rig, the gate decides all the acts of all microbatch layers in one
  ordered fold on task 0. [RIG: micro.clj:21, 1744–1749] Thus, the decisions
  use one thread. The storage and the work on each value use all tasks.
- In the rig, each act also goes to the task of its layer, for the settings and
  the permissions. [RIG: micro.clj:1549–1555]
- The answer comes one batch later. The screen shows no act before the gate
  accepts it. Thus, a microbatch layer shows each act one batch later. [RULED +
  VISION + DERIVED]

#### 3.3.6 What both gates examine

Each gate examines these items, for each act: [PROPOSAL: PICTURE.md:66 + RULED]

- the shape of each value, against the grammar of its key,
- the expected versions, against the current versions,
- the permission that the act names, against the policy of the layer,
- the class in the name of the act, against the class of the layer.

Only the gates write the record. [PROPOSAL]

A gate examines only the permissions that are in the layers that it controls. An
act that names a permission in another layer is refused. [RULED]

#### 3.3.7 What a stream gate must have on its task

A stream event is atomic on one task only. [RAMA DOCS: 23-acid-semantics.md:41,
82] Thus, a stream gate can examine an item in two ways only:

- The item is on the home task of the layer.
- The gate reads the item from another task, and it records the read. That read
  is not atomic with the write. [RULED: "a check one gate makes against the
  other store is a read, stamped in the checking store"]

These items are in this condition:

| Item | Where it is | Source |
|---|---|---|
| Permissions | In the layer that they control. | [RULED] |
| Current versions | In the layer. | [DERIVED] |
| Grammars | Open. A copy in each layer costs a change in each layer when the grammar changes. A read from another task costs one hop for each act. | [DERIVED + OPEN: ?13] |
| Placement rules | Open, for the same reason. | [PROPOSAL + DERIVED + OPEN: ?13] |
| The lock of each person | A copy on each task. | [RIG + DERIVED from rulings 6 and 7] |
| The facts that stood on a fact | Open. The maintain step needs them. | [PROPOSAL + OPEN: ?14] |

#### 3.3.8 The record

The record is in the PStates of the gates. It is not in a depot that a gate
writes. [RAN PROBE + RAN CHECK + RIG] Sid did not decide this yet. That is
decision D2. [OPEN]

The reason, from the two runs of 2 October:

- When an attempt of a gate fails, Rama removes the PState writes of that
  attempt. [RAN PROBE + RAN CHECK]
- In most runs, Rama kept the depot appends of that failed attempt. [RAN PROBE +
  RAN CHECK]
- Thus, a depot that a gate writes can hold an act that the record never
  accepted. [RAN PROBE: Q1-none]
- Such a depot can also hold one act two or more times. The repeats can have
  stamps that belong to other acts. [RAN PROBE: Q1, Q2]
- The microbatch gate does the same. [RAMA DOCS: 12-microbatch-topologies.md:130 +
  RAN PROBE: Q4 + RAN CHECK]

The PStates are the only place in Rama where a decision and all that comes from
it occur together, or do not occur. [DERIVED]

Sid's card agrees: "Event log is the source of truth and event is the atomic
unit". [SID] The event log is in a PState that the gate only adds to. [DERIVED]

The costs: [DERIVED]

- Rama lets one topology write a PState. But that topology can change what it
  wrote. Thus, the code of the gate must keep the rule "never rewritten".
- To go past N tasks, a topology copies the PStates into a new module. [RAMA
  DOCS: 19-operating-rama.md:489]
- To make an index again, a topology reads the record from the PStates.

The acts depots still hold each act as its sender sent it. Thus, the PStates are
the only copy of what the gates decided, and when. They are not the only copy of
what the senders said. [DERIVED]

The record PStates are private. Only the topologies of the module can read them.
[DERIVED + OPEN: ?11 + RAMA DOCS: 15-pstates.md:222–228 + RAN CHECK] As built, the
rig reads its PStates directly from the client. [RIG] Thus, the rig cannot make
them private now.

#### 3.3.9 A feed

A feed is necessary only if something in the module reacts to landed acts. That
is open item ?2. [DERIVED]

A feed obeys these rules: [DERIVED + RAN CHECK + RAN PROBE]

1. The gate commits the record first. Then it appends to the feed.
2. The feed carries names and stamps only. It carries no values.
3. A record in a feed is a bell. The reader of the feed reads the answer by name
   from the record.
4. A record in a feed can come two or more times. The name of the act tells the
   repeats apart.

A microbatch gate cannot commit before the end of its batch. Thus, a microbatch
gate can append only what the previous batch committed. [RAMA DOCS:
12-microbatch-topologies.md:118–130 + RAN PROBE: Q4 + DERIVED]

#### 3.3.10 Promotion

A promotion is two acts: the request in the source layer, and the landing in the
target layer. [RULED]

The source gate does these steps: [RULED + RIG: promote_flow.clj:109 +
promote.clj:183–184]

1. It reads the value out, through the lock of the value.
2. It writes the crossing fact. The crossing fact holds the landing exactly as
   the gate will send it.
3. It commits.
4. It appends the landing to the acts depot of the target.

The order of steps 3 and 4 is necessary. If the gate appends before it commits,
and the attempt then fails, the landing can leave. The record then still says
"not yet read out". [RAN CHECK + RAN PROBE]

Then a forget can refuse a promotion whose copy is already in the target. That
breaks the ruled rule: "A forget ordered before the read-out refuses the
promotion". [RULED + DERIVED]

After a failure, Rama can do the steps again. The landing then comes again, with
the same content. The target finds the name, and it gives the same answer again.
[RIG: promote_flow.clj:23–25]

The rulings call the state before the landing "pending". It has two parts:
[RULED]

- **Not yet read out.** The person can still forget the value.
- **Read out and not landed.** The value crossed. A forget after the read-out
  does not remove the copy from the target.

"Crossed" does not mean "done". The target can still refuse the landing. [RULED]

The copy is about the people that the grammar and the tool of the target name.
It is not about its earlier owner. [RULED]

From a microbatch layer, the rig does not do promotion. The microbatch gate
refuses a promotion request. [RIG: RIG.md:1446–1448] This is open item ?10.

#### 3.3.11 Re-class

A re-class changes the class of a layer. [RULED: "a hot layer can be
re-classed" + SID]

In the rig, a re-class has these steps: [RIG: micro_client.clj:542–571 +
IMPLICIT_SPEC.md:601–617]

1. The operator sends one act with the new class to the stream gate. The stream
   gate decides it on the home task.
2. The stream era stops. No data moves.
3. The microbatch gate reads the stream era as settled history.
4. Each read of the layer merges the two eras.
5. The gate refuses an act for the old class. The reason is "class mismatch".

The costs: [RIG + DERIVED]

- Each read of the layer merges two eras, for the life of the layer.
- Each act still goes to the old home task, for the settings, the permissions,
  and the current versions of the stream era.
- The order of one owner stops at the re-class. [RULED]

No rig test has acts in flight across a re-class. [RIG] This is open item ?9.

A re-class for scale gives a batch of delay to each act of the members. [DERIVED]

#### 3.3.12 Locks and forget

- Each value has its own small lock. The gate wraps that lock under the locks of
  the subjects of the value. [RULED: ruling 7]
- For a personal layer and a hand session, each lock has its own row. For the
  other layers, the lock is in the record. A mark on a value overrides this.
  [RULED]
- The gate opens values by default. [RULED: ruling 6]
- Thus, the lock of each person is on each task. [RIG: locks.clj:1083–1084 +
  DERIVED from rulings 6 and 7] This is not a rule of Rama.
- To forget a person, the store destroys the lock of that person on each task.
  The answer comes after all tasks destroyed their copy. [RIG: module.clj:151–157]
- The bytes of a destroyed lock stay on the disk until compaction, on each task
  and on each replica. [RIG: RIG.md:1577–1581]
- Sid ruled two deletes: retract and forget. [RULED]
- Excision is the last method of the operator, for a value that has no mark and
  whose lock is in the record. [RULED] A module update can change or excise
  depot records and PState records. [RAMA DOCS: 14-depots.md:288–312]
- The operator is trusted at the start. Locks are for forget, not to hide data
  from the operator. [RULED]

#### 3.3.13 Clocks and stamps

- Each task has a clock. [RULED: ruling 4 + RAN 25 SEP]
- The clock never goes back on its task. A stamp is never earlier than any fact
  that the act stood on. [RULED]
- When an attempt fails, Rama removes its clock tick. The next act can then get
  the same stamp. [RAN PROBE: Q2]
- Thus, a stamp that left the record can belong to another act. Only a stamp
  that the runtime reads from the record, by name, is correct. [DERIVED]
- In the rig, a stamp is the wall time in milliseconds, times 65536, plus a
  counter. [RIG: clock.clj:9–12] This is a default. It is not ruled. That is
  open item ?7.

#### 3.3.14 Resolve

Resolve is the one exit for reads. [PROPOSAL]

Resolve does these jobs: [PROPOSAL + RULED]

- It shows each reader only what that reader can see.
- When two layers have a fact for the same entity and key, the nearer layer
  wins. This is the same for both classes.
- It gives a status with each read: "value", "absent", "pending", or "failed".
- It gives the moment of each layer that it read. That is open item ?6.
- For a layer that had a re-class, it merges the two eras. [RIG]

Resolve is a set of query topologies on private PStates. [DERIVED + OPEN: ?11]
This is a choice. Rama does not make it necessary. But with this choice, Rama
itself keeps the one exit. [RAMA DOCS + RAN CHECK]

As built, there is one query for each layer. The query examines who can see what,
in the module. The runtime (Inland) finds the nearer layer. [RIG: reads.clj:1619–1635 +
gesture.clj:35–71 + facts.clj:88–99]

One query for a full context is a design. Nobody built it. That is open item
?16.

#### 3.3.15 The maintain step

The maintain step runs a tool again when a fact that the tool read changes.
[PROPOSAL]

- For the screen, the bells do this. [RIG]
- A landed fact must find the facts that stood on it. Those facts can be in
  other layers, on other tasks. The architecture has no method for this yet.
  That is open item ?14. [PROPOSAL + DERIVED]
- As built, Inland looks for the facts that stood on a fact only in the hand
  layer and the personal layer. [RIG: reader.clj:24–26]
- Where match, tools, and the maintain step run is open item ?2. [DRAWING]

## 4. The act

```
{:name        <random; tells its layer and its class>      the door makes it      [RULED]
 :layer       <the id of the layer>                         the stream acts depot hashes this [RULED]
 :who         <the actor>                                                          [RULED]
 :permission  <a permission in that layer>                                         [RULED]
 :based-on    [<each read, at its version>]                 one time, on the act   [RULED]
 :subjects    #{<the people that it is about>}                                     [RULED]
 :session  :because-of  :claimed-when  :expected  :version  on the act in the rig  [RULED; RIG; OPEN ?7]
 :facts       [[entity key value replaces] ...]                                    [RULED]
}
The gate adds its stamp. The gate records its answer under the name.
```

The ruled core on each fact also holds the act id and the index. In the act,
these are the name and the position of the fact. [RULED]

In the rig, the expected versions are the `:stood-on` of the act. The field
`:replaces` is on each fact. The version marker is 1. [RIG:
envelope.clj:28–29 + PLAN-stream-store.md:808–809]

The microbatch acts depot hashes one entity. In the rig, that entity is the lease
of the first lock that a fact names. If there is no lease, it is the entity of
the first fact. [RIG: micro.clj:319–321 + OPEN: ?7]

Three tests tell where each part of an act must be: [DERIVED]

1. **Where it goes.** The partitioner of a depot sees only the act and N. Thus,
   all that selects the task must be in the act. [RAMA DOCS: 14-depots.md:42–58]
2. **What the gate examines.** A stream gate decides each act alone, in one
   event, on one task. Thus, all that it examines must be in the act or on that
   task. A microbatch act goes to many tasks in one batch. [RIG:
   micro.clj:1549–1555, 1744–1749]
3. **What nobody can find later.** Sid's card says: "an event individually and
   the log collectively should have all the information that it needs to collect
   at the time it is invoked". [SID]

## 5. The rules that all parts obey

1. Only a gate writes the record. [PROPOSAL]
2. An act writes one layer. [RULED]
3. An act names its permission. [RULED]
4. The door names an act before the gate. A name goes to one gate only. [RULED]
5. The runtime finds an answer by name. The ack is not the answer. [RULED + RAN
   25 SEP + RAN PROBE]
6. A gate reads its record by name before it decides. [RIG + RAN PROBE]
7. A refusal is data. A gate never throws an exception. [RAN 25 SEP]
8. All that leaves a decision is a signal. The truth is in the record. Read it
   again by name. [DERIVED from RAN PROBE and RAN CHECK]
9. A gate commits first, and appends after. [RAN CHECK + RIG]
10. All reads go through resolve. The record is private. [PROPOSAL + DERIVED]
11. A bell carries no value and no lock. [DERIVED]
12. Time comes in only as the stamp of a gate or as an act from outside.
    [PROPOSAL]
13. Order between two layers exists only through "stood on". [RULED, for the two
    stores + DERIVED, for each pair of layers]
14. The screen shows an act only after the gate accepts it. [RULED + VISION]

## 6. What each class costs

### 6.1 A stream layer

What it gives:

- The answer comes in milliseconds. [RAN 25 SEP + RIG]
- The layer has one order. [RAMA DOCS]

What it costs:

- A stream layer uses one thread only. [RAMA DOCS]
- A task has one thread for its stream layers, for its part of each batch, and
  for reads. [RAMA DOCS + RAN 25 SEP]
- Thus, a busy stream layer slows the other stream layers on its task. [DERIVED]
- A busy stream layer also slows all microbatch layers, because each batch waits
  for its slowest task. [DERIVED + RAN PROBE: Q5]
- A stream layer cannot move to another task, except with a re-class. [RAMA
  DOCS + DERIVED]
- Rama can do an event two or more times. Thus, the gate must read its answer by
  name first. [RAN 25 SEP + RAN PROBE]

The rig measured one layer on one task. The conditions: in-process cluster, 4
tasks, replication factor 1, AMD Ryzen 9 9900X. [RIG]

| Store | Writers | Acts each second | Slowest 1 in 100 |
|---|---|---|---|
| Stream store of 25 September (4 index writes for each act) | 1 / 16 / 32 / 128 | 300 / 2,228 / 3,362 / 5,192 | 4.7 ms / … / 16.5 ms / 43.9 ms |
| Finished store, closed loop | 1 / 16 / 32 / 128 | 226 / 842 / 1,062 / 1,198 | 15.5 ms (1 writer) to 236 ms (128 writers) |
| Finished store, open arrival | 100 acts each second | all accepted | 46 ms |
| Finished store, open arrival | 1,000 acts each second | all accepted | 254 ms |

Sources: `BENCH_NOTES-stream.md` lines 180–196, and `BUILD_NOTES-numbers.md`
lines 292–317. [RIG]

The task thread stayed at about 60% of a core or less. Thus, the runs did not
find the limit of one task. [RIG]

At 100 acts each second, the slowest 1 in 100 is 2.3 times the assumed limit of
20 ms. [RIG]

### 6.2 A microbatch layer

What it gives:

- The data and the work on each value use all tasks. [RAMA DOCS + RIG]
- The PState writes occur exactly one time. [RAMA DOCS + RAN 25 SEP]

What it costs:

- The answer comes one batch later. [RAMA DOCS]
- The screen shows no act before the gate accepts it. Thus, a person who types
  in a microbatch layer gets "typing lag". [RULED + DERIVED]
- `carry-on.md` calls typing lag a "direct failure" of the experience. [VISION:
  carry-on.md:300] Sid wrote: "I DON'T LIKE OPTIMISIC UPDATES". [VISION:
  LOG.md:1217]
- All microbatch layers wait for the slowest task in each batch. [RAN PROBE: Q5]
- In the rig, all decisions use one thread, on task 0. [RIG]
- Depot appends from a batch do not occur exactly one time. [RAMA DOCS + RAN
  PROBE: Q4]

### 6.3 What follows

- A person must type in stream layers. [DERIVED]
- A re-class for scale gives a batch of delay to the members of the layer.
  [DERIVED]
- Task 0 has two loads: all microbatch decisions, and the stream layers whose
  home task is task 0. A busy stream layer on task 0 slows each microbatch
  decision. [DERIVED] Nobody examined if the rig keeps stream layers away from
  task 0. That is open item ?15.

## 7. Evidence

### 7.1 The Rama check of 25 September 2026

Rama 1.6.0, in-process cluster, 4 tasks. [RAN 25 SEP]

- A stream event is atomic on its one task.
- If an event fails between two tasks, the write on the first task stays, and
  the second write does not occur.
- Rama does a stream record again, at least one time.
- An ordinary exception stops the worker. Rama starts the worker again, and it
  does again all records after the last checkpoint, also records that had an
  ack.
- A client can get an error for an append whose act landed.
- A slow event can run two times at the same time.
- A microbatch is decided before it is visible. Rama does a failed batch again
  as a full batch.
- During the commit of a batch, one task can show the batch before another task.
- A clock on a task never goes back.

### 7.2 The depot-append probe of 2 October 2026 (recheck session 2)

Rama 1.6.0, revision c45b08f3. In-process cluster, 4 tasks, 4 threads, 1
worker. Three full runs gave the same counts. A raw read of each depot partition
gave the ground truth. [RAN PROBE]

| Case | What the gate did | Records in the depot for the act | PState |
|---|---|---|---|
| Q1 | Stream, one task. It appended, then failed with Rama's failure class. | 2, with the same stamp | the second attempt only |
| Q1 | The same, with an ordinary exception, and an ack level on the append. | 2 | the second attempt only |
| Q1 | The same, with an ordinary exception, and no ack level on the append. | 1: the append of the first attempt did not land (3 runs of 3) | the second attempt only |
| Q1-none | No second attempt. | 1, in three variants of four | nothing |
| Q2a | A worker restart. The gate did not read its answer by name first. | 2 for an act that already had an ack, with stamps 1 and 2 | 2 rows |
| Q2b | The same, but the gate read its answer by name first. | 1 for the act that had an ack | 1 row |
| Q3 | It appended on task A, then failed on task B. | 2 or 3 | not applicable |
| Q4 | Microbatch. It appended, then failed. | 2, with the same batch id | one time only |
| Q5 | Two records with no relation, in one batch. Task B was held. | not applicable | The record on task A was not visible for 3 seconds. |

Also:

- Each stream case that failed gave the client an error. This occurred also when
  the gate accepted the act on the second attempt.
- After a failed attempt, the next act got the same stamp. In Q2a, stamp 2 is
  on the failed act, and on another act that Rama did again.
- A stream topology that read the depot saw some records two times after a
  restart.

The probe did not put a commit before the append. The question of the drawing
was about an append in the same event as the failure.

### 7.3 The append check of 2 October 2026 (the other recheck session)

Rama 1.6.0, in-process cluster, 4 tasks, 1 worker, one copy of the data. [RAN
CHECK]

| What the gate does | Attempts that the PState kept | Attempts that the depot kept |
|---|---|---|
| No failure | 1 | 1 |
| Fails before it writes | 2 | 2 |
| Appends and waits, then fails | 2 | 1, 2 |
| Appends without a wait, then fails in the same event (three runs) | 2 | 1, 2 |
| Commits its PState write, then appends, then fails | 1, 2 | 1, 2 |
| Appends and waits, then fails with an ordinary exception | 2 | 1, 2 |
| Microbatch gate: appends, then fails | 2 | 1, 2 |

In the fifth row, the PState shows 1 and 2 because the test gate did not read
its answer by name first. A real gate does. [RAN CHECK]

A private PState: a query topology in the same module read it. A client read
failed with an error. [RAN CHECK]

The two runs agree where they examined the same cases. [DERIVED]

### 7.4 The rig and Inland

A gatherer read these facts. Recheck session 2 read the lines with a mark (✓).

- The record is in PStates. No gate appends decisions to a depot. [RIG]
- The one topology append is the landing of a promotion, after a commit.
  [RIG: promote_flow.clj:109–134]
- The stream gate reads its answer by name first. [RIG: gate_event.clj:123–128]
- The re-class makes two eras. [RIG: IMPLICIT_SPEC.md:601–617]
- The microbatch gate decides in one fold on task 0. (✓) [RIG: micro.clj:15–35,
  1735–1760]
- Each microbatch act goes to the task of its layer. (✓) [RIG:
  micro.clj:1540–1560]
- The bells: a stamp for each layer, a frontier, and the full entry of each
  person. (✓) [RIG: bell.clj:1–75]
- The entry of a person holds the lock. (✓) [RIG: locks.clj:1080–1095]
- Each accepted act moves the bell of its layer. (✓) [RIG: gate_event.clj:80–95]
- The door reads the class for each act. (✓) [RIG: micro_client.clj:455–470]
- The reader shows rows only after the gate accepts the read entry. (✓) [RIG:
  reader.clj:1–30]

### 7.5 Where the raw files are

The files of the two runs are in temporary folders. A restart removes them.

- The probe: `/tmp/claude-1000/-mnt-data-projects-Softland/bb8d4e88-3d77-4ccd-9ce7-bc9ff57ca53d/scratchpad/depot-append-probe/`
- The check: `/tmp/claude-1000/-mnt-data-projects-Softland/bec14570-35d3-4ccf-9fab-eb6bda859ede/scratchpad/append-check/`

Sections 7.2 and 7.3 keep the results.

## 8. The corrections to the drawing of 2 October

| # | The drawing said | The correction | Source |
|---|---|---|---|
| 1 | ?1: the record is a depot of decided acts, if a failed event drops its append. | A failed event keeps its append. The record is in PStates (D2). | [RAN PROBE + RAN CHECK] |
| 2 | Promotion: read out, write the crossing fact, send the landing. | Commit the crossing fact first, then append the landing. If not, a forget can refuse a promotion whose copy already left. | [RAN CHECK + RULED + RIG] |
| 3 | Promotion runs between any two kinds of layer. | From a microbatch layer, it is open (?10). The rig does not do it. | [RAMA DOCS + RAN PROBE + RIG] |
| 4 | Owner count picked the gate in "earlier drawings". | Owner count was in rulings 1 and 2, "Layer kinds", and "Two consequences". Switch 3 changes them (D1). | [RULED] |
| 5 | Switch 3: "set by its members". | "Its members, by its policy." The table that Sid agreed to says this. | [SID] |
| 6 | The answer comes on the ack. | The answer is found by name. The ack can be an error for an act that landed. | [RAN 25 SEP + RAN PROBE + RAN CHECK] |
| 7 | Rama lets any client read any PState. | Rama can make a PState private. Rama cannot make a depot private. | [RAMA DOCS + RAN CHECK] |
| 8 | ?3 is necessary if ?1 is a depot. | The acts depots keep each act's values in all cases. ?3 does not wait on ?1. | [RULED + RAMA DOCS] |
| 9 | Rama never rewrites a depot. | A module update can change or excise depot records. | [RAMA DOCS] |
| 10 | The record cannot be touched (recheck item 5). | Removal is permitted when the removal is a fact. Excision is the last method of the operator. ?4 loses its reason. | [RULED] |
| 11 | A stream layer costs only scale. | It also costs its neighbors on its task, and all microbatch layers. | [RAMA DOCS + RAN PROBE + DERIVED] |
| 12 | Proxies push changes. | A bell only rings. The runtime reads again through resolve. A bell carries no value and no lock. | [PROPOSAL + RIG + DERIVED] |
| 13 | The primitives include the clock and random numbers, for all act converters. | No leaf of a tool reads a clock or a random number. These primitives are for outside tools only. | [PROPOSAL] |
| 14 | ?8: the escape clause of "Nothing in a record is a program" applies. | The clause has a test that nobody met. A parser can be data as a grammar. | [SETTLED + PROPOSAL + DERIVED] |
| 15 | Sid's position: "capabilities first, then proofs". | That was the session's statement. Sid named reviews and proofs, with Lean or Bend. | [SID] |
| 16 | "Every person's lock on every task" comes from Rama. | It comes from rulings 6 and 7. Its cost: a forget destroys a copy on each task. | [DERIVED + RIG] |
| 17 | "Grammars live in the layer" comes from Rama. | It is a choice. The other road is a recorded read from another task. | [RULED + DERIVED] |
| 18 | "Resolve is query topologies" comes from Rama. | It is a choice. With private PStates, it is the choice that lets Rama keep the one exit. | [RAMA DOCS + RAN CHECK + DERIVED] |
| 19 | "An act writes one layer" is derived. | It is ruled. | [RULED] |
| 20 | "The runtime" is the client only, and is "the one tool that is not a fact". | The fixed part holds the runtime and the code of the module. | [PROPOSAL] |
| 21 | Re-class: not drawn. | The rig makes two eras. Section 3.3.11 gives the costs. | [RIG] |
| 22 | Every read leaves a basis entry (in resolve). | The runtime appends a read entry as an act. It shows nothing before the gate accepts it. | [RULED + RIG] |
| 23 | Microbatch layers are spread over all tasks. | In the rig, decisions use one fold on task 0. Each act goes to the task of its layer. | [RIG] |
| 24 | The door reads the class of the layer. | The rig reads it again for each act, and does not keep it. | [RIG] |
| 25 | Maintain: not drawn. | The bells serve the screen. A method across layers and tasks is open (?14). | [PROPOSAL + RIG] |
| 26 | Placement: "indexes". | Placement rules are facts. The gate must apply them in its event (?13). | [PROPOSAL] |
| 27 | A quote: "every crossing is owned by the fixed part and governed by facts". | The proposal says: "Every crossing is owned by the floor and governed by material." | [PROPOSAL: PICTURE.md:34] |
| 28 | The words "whole" and "spread" are retired. | The drawing still used them. This document says "on one task" and "divided by entity". | [DRAWING] |

The recheck list of the drawing:

| # | Claim | Result |
|---|---|---|
| 1 | Rama drops the append of a failed event. | It does not. [RAN PROBE + RAN CHECK] |
| 2 | Each microbatch layer waits for the slowest task. | Yes. [RAN PROBE: Q5] |
| 3 | The throughput numbers. | They agree with the bench notes of 25 September. The finished store is slower. Section 6.1. [RIG] |
| 4 | Resolve reads a full context in one query topology. | Nobody built it (?16). [RIG] |
| 5 | Ruling 7 reads as if the record cannot be touched. | Too strong. Correction 10. [RULED] |
| 6 | Where the parts of the server go. | It is not in the drawing. It is in the drawing session's chat at 07:42. Nobody examined it against the server. [SID: the record] |

## 9. Decisions and open items

### 9.1 Decisions for Sid now

**D1. Switch 3 against rulings 1 and 2.** Sid agreed to the three switches on 2
October. Does that agreement replace these four lines of 24 September? [RULED +
SID]

- ruling 1,
- ruling 2,
- "Layer kinds",
- the first of "Two consequences".

The two answers:

- If yes, the class is a fact that the members set by policy, for all kinds of
  layer.
- If no, Sid tells which part of the switches to keep.

**D2. The record in the PStates of the gates.** Does Sid take the record in the
PStates? Then the code of the gate, not Rama, keeps the rule "never
rewritten". [RAN PROBE + RAN CHECK] Section 3.3.8 gives the reason and the
costs. (This was ?1.)

### 9.2 Open items

| # | Open item | State now | Source |
|---|---|---|---|
| ?2 | Where match, tools, and the maintain step run: in the module, in the runtime, or on a budget. | It does not wait for D2 now. All three read the answer by name after a signal. A feed is necessary only for the module. | [DRAWING + DERIVED] |
| ?3 | Does the door seal values before an acts depot keeps them? | It is now only about the acts depots. It is the same question as Sid's: must forget reach the copy in the depot? A third road: excision by module update. | [RULED + RAMA DOCS] |
| ?4 | What identifies a person in the store. | Opaque ids with one link that the store can erase make forget one deletion. Without them, forget needs a module update. This is Sid's item 41. | [RULED + RAMA DOCS + DERIVED] |
| ?5 | The class of each layer at the start. | The drawing: all layers start as stream layers. The rig: the base is a stream layer until the first group. 24 September: promotions into the base go to the microbatch gate. | [DRAWING + RIG + RULED] |
| ?6 | Does one read line up the moments of the layers that it read? | Sid's open item (one moment for each store). It applies to each pair of layers. The rig records one moment for each layer that it reads. | [RULED + RIG] |
| ?7 | The open parts of the act. | They are: where the five parts go, the stamp, the fingerprints, the entity for the microbatch acts depot, and the ids from ingest. The rig took defaults (sections 3.3.13 and 4). New condition: data that leaves a decision must be the same on each attempt, or the runtime must read it again by name. | [RIG + RAN PROBE + DERIVED] |
| ?8 | A designed total language, so that act converters and base functions can be data. | The escape clause needs a proof: "a record provably cannot say a needed tool twice over". Nobody made that proof. A parser can be data as a grammar that one primitive reads. The repo converter is the test. | [SETTLED + PROPOSAL + DERIVED] |
| ?9 | A re-class with acts in flight. | No test. | [RIG] |
| ?10 | Promotion out of a microbatch layer. | Not built. It can append only what the previous batch committed. With the rig's stamp, a second attempt makes a different landing with the same name. | [RAMA DOCS + RAN PROBE + RIG + DERIVED] |
| ?11 | Private record PStates, and bell PStates that are not private. | A design. Nobody built it. The door and Inland read the PStates directly now. | [RAMA DOCS + RAN CHECK + DERIVED] |
| ?12 | The cost of a read entry for each read. Does a reader ring itself? | Each read waits for one decision of the stream gate. The second question: nobody examined it. | [RIG + DERIVED] |
| ?13 | Grammars and placement rules for a stream gate. | A copy in each layer, a change in each layer, or a recorded read from another task. | [PROPOSAL + RULED + DERIVED] |
| ?14 | The maintain step across layers and tasks. | No method. Inland looks only in the hand layer and the personal layer. | [PROPOSAL + RIG] |
| ?15 | Task 0: all microbatch decisions, and some stream layers. | Nobody examined if the rig keeps stream layers away from task 0. | [RIG + DERIVED] |
| ?16 | One query for a full context. | A design. Nobody built it. | [RIG] |

### 9.3 Items that Sid's rulings already list as Sid's own

From "25 September, evening", in the rulings: [RULED]

- Must forget reach the copy of a value in a depot? (This is ?3.)
- The form of the stamp. (This is in ?7.)
- What the fingerprint of a read entry covers. (This is in ?7.)
- Can two nodes run here?
- The three thresholds.
- The size of the values of a hand session.

### 9.4 One position for later

[DERIVED] For ?5, recheck session 2 has this position. It is not a ruling.

- The base starts as a microbatch layer. All other layers start as stream layers.
- The reasons: the base gets the promotions of all people. Thus, its load is
  known. Promotions already have a ruled state for the wait. People do not type
  in the base. A re-class leaves a permanent cost.
- The cost: promotions into the base wait one batch. The base shares task 0 and
  the slowest task with all microbatch layers.

## 10. Sources

- Sid's rulings: `git show main:src/proposal/frame-2026-09-15/PROGRESS.md`, the
  "Now" section.
- Sid's proposal: `src/proposal/frame-2026-09-15/PICTURE.md` and `PARTS-4-8.md`.
- The settled ground: `docs/decisions.md`.
- The vision: `docs/carry-on.md` and `vision/LOG.md`.
- The Rama check: `git show main:src/proposal/rama-check-2026-09-25/RESULTS.md`.
- The Rama documents: `reference/rama/docs/`, mostly `11-stream-topologies.md`,
  `12-microbatch-topologies.md`, `13-query-topologies.md`, `14-depots.md`,
  `15-pstates.md`, `19-operating-rama.md`, and `23-acid-semantics.md`.
- The rig: `src/proposal/rig-2026-09-25/` (code under `src/rig/store/`, and the
  notes).
- Inland: `src-inland/softland/inland/`.
- The handover of the stretch: `/mnt/data/projects/research/softland/handover-2026-09-29/WHERE-SID-IS.md`.
- The first drawing: `ARCHITECTURE.md` in this folder.
- The record of the session of 2 October 2026 that drew the architecture.
