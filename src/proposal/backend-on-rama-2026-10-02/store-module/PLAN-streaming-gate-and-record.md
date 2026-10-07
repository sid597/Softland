# Plan

<!-- Phase 1 Step 5. Fill in after completing Steps 1-4. After completing this, fill in PLAN_VALIDATION.md. -->

Part: `streaming-gate-and-record`, the first of the two parts of the store's module
(`DECOMPOSITION.json`, first entry). Stage: Phase 1 of the Rama skill, "Plan". This paper is a design.
It holds no topology code.
Written by: a fresh session (Opus 5.5, effort max), 8 October 2026, for the build session "softland-11".
Revised the same day, by the same session, for two rulings of the build session on this plan. (1) The
engine of `act.clj` may take the map of shapes and the nesting limits, so the door's declaration is
data that the same engine reads. (2) How a value was sealed is said by a word in its lock's row, not by
a mark in the value. Each is marked "ruled by the build session" where it applies.

## How to read this paper

**Sources.** The same four as the implied spec, cited the same way: `C` is the contract
(`BUILD-store-module.md`; `C item 3(d)`, `C rule 6`), `R` is the README (`src-store/README.md`;
`R 7-Oct 7`), `act.clj` is the one form, and `IS` is the implied spec (`IS C3`, `IS X29`, `IS O8`).
Where the contract and the README differ, the README wins; the section "Where the contract and the
README differ" says what this stage found. Rama's behaviour is cited from the skill's references
(`skill stream.md`) and from the local copy of Rama's docs (`docs 11:158` is
`reference/rama/docs/11-stream-topologies.md`, line 158). No code of the rig, the server or Inland was
read. The four findings that the contract names are used as findings.

**Four kinds of statement.**

- *given*: a source says it. The citation follows.
- *derived*: it follows from the sources. The reason follows.
- **the plan's choice**: the sources are silent and the design needs an answer. The choice, its reason,
  and what the other answer would change. All of them are listed again near the end.
- *not verified*: a behaviour of Rama or of the JVM that this stage read in a document and did not run.
  This stage ran no cluster and no code.

**Words of this paper.** A *place* is an entity and a key in one layer (C rule 12). A *store key* is
`:class`, `:permission` or `:lock-request` (C item 2); a *plain key* is any other key. A *thing* is
whatever arrives on a depot. A *stray* is a thing that gets no answer (C rule 10). *The layer's task* is
the task that Rama's hash of the layer id gives. A *read* is one RocksDB point read, or the first seek of
a range read; the skill costs it at 0.3 to 0.5 ms from a cold disk. An *iteration* is one more row of a
range read, about 5 µs (SKILL.md, "Performance costs").

**What this part is.** The decision of one act against the record of its layer, as the streaming gate
makes it, and the record that the decision reads and writes. It works only with layers of the class
streaming. The base, the microbatch depot and the microbatch gate are the next part's. This part's record
and pure functions are what the next part builds on; the section "For the second part" says exactly what
it gets and what is left to it.

## The design in short

| Kind | Name | What it is | Where it lives |
|---|---|---|---|
| Depot | `*streaming-depot` | Every thing sent for a streaming layer, as it was sent. Never trimmed. | Partitioned by the hash of the thing's `:layer` |
| Topology | `streaming-gate` (stream) | One event for each thing, with no hop to another task. It decides the act and writes the record. | Runs on the depot partition's task, which is the layer's task |
| PState | `$$streaming-answers` | The answer under each act-id, with the digest of its act | A flat map, key `"<layer> <act uuid>"`, on the layer's task |
| PState | `$$streaming-facts` | The current fact of each place: values, retracts, the class, permissions, requests for locks | A flat map, key `"<layer> <entity> <key>"`, on the layer's task |
| PState | `$$streaming-locks` | Each lock: its bytes, and the fact that it seals | A flat map, key `"<layer> <request uuid> <index>"`, on the layer's task |
| PState | `$$streaming-task` | What the gate keeps for a task as a whole: its last stamp, and the strays it saw | One value for each task |
| Query topology | `answer-by-act-id` | act-id → the recorded answer with its digest, or nothing | The layer's task; 1 read |
| Query topology | `locks-of-request` | a request's act-id and a range of indexes → those locks: the bytes and the way of each | The layer's task; 1 read and one iteration for each lock |
| Query topology | `class-of-layer` | layer id → its class, or nothing | The layer's task; 1 read |

Beside these, plain Clojure namespaces that need no cluster hold everything that is not Rama: the door's
declaration, the one text of a thing and its digest, the stamp, the seal, the form of a first act, and
the checks of the decision. The gate's dataflow only reads rows, calls these functions, and writes rows.
The reasons are this part's own: a function that reads no PState, no clock and no random source gives
the same answer on a retry and can be proven without a cluster, and each of them turns a throw into a
refusal in one place (IS C1). It also makes "the same checks" (C item 4) one set of functions.

One accepted act with one sealed value costs **4 reads and 4 row writes, in one event, on one task**:
the answer (is the act-id used), the permission, the lock, the task's last stamp. The working is in
"The checks of one act".

## Where the code lives, and the classpath

The skill's default paths do not apply. The papers of the stages stay in
`src/proposal/backend-on-rama-2026-10-02/store-module/` (C "How the build runs").

**Source**, all in `src-store/softland/store/`, beside `act.clj`, each namespace with its docstring:

| Namespace | Role | Needs Rama |
|---|---|---|
| `softland.store.act` | The one form. Built and proven. One small change, ruled by the build session on 8 October: its engine takes the map of shapes and the nesting limits. Said exactly below the table. | no |
| `softland.store.text` | The one text of a thing: one function from anything to a string, the same in every process | no |
| `softland.store.digest` | The digest of a thing: SHA-256 over its one text. One function that the door and both gates share (C rule 11) | no |
| `softland.store.declaration` | The door's declaration: what arrives on a depot, its limits, the store keys, the test for a stray, and the check | no |
| `softland.store.stamp` | The stamp: the packing of clock time and counter, the next stamp, the 10-second rule | no |
| `softland.store.seal` | Locks and the seal: making a lock's bytes, sealing a value to its place, opening it | no |
| `softland.store.first-act` | The first act of a layer: the class a layer starts with (F4 C), and the form of a making act | no |
| `softland.store.decision` | The checks (c) to (g) and the plan-placed ones, as functions of an act and the rows it names; and the rows that an accept writes, as data | no |
| `softland.store.record` | The record's rows: the text of each key, the making and the reading of each row | no |
| `softland.store.record-pstates` | The record as PStates: the four schemas, written once, which each gate declares under its own names (C item 4, "same record shape") | yes |
| `softland.store.streaming-gate` | The stream topology: its four PStates, and the dataflow of one event | yes |
| `softland.store.door-reads` | The three query topologies | yes |
| `softland.store.module` | The Rama module `Store`: the depot, and the calls that add the gate and the reads | yes |

**What changes in `act.clj`, exactly** (the build session's ruling of 8 October; the build makes the
change):

- The engine, which is `conform` and its two helpers `conform-all` and `conform-parts`, takes the map
  of shapes and the nesting limits as an argument. Today it reads only the namespace's own `shapes`
  and `nesting`.
- One new public function gives that engine to another namespace. It takes a map of shapes, the
  nesting limits, the name of a shape and a thing. It gives what the engine gives: the thing in its
  one form, or the reason of the first part that is wrong. Its docstring says what it takes and gives.
- `parse` and `has-shape?` call the engine with `act.clj`'s own `shapes` and `nesting`. Their
  behaviour and their results stay exactly as they are.
- Nothing else changes: not the shapes, the rules, the reasons, the limits or the leaves, and not what
  a docstring claims, beyond the engine's own docstrings saying what they now take.
- The check of the change: `clojure -M:store -m softland.store.act-proof` still passes its 150 checks,
  with no check changed. This stage ran it on the tree as it stands: 150 passes, none failed.

The leaves stay `act.clj`'s own: the ruling gives the engine the shapes and the nesting limits, not
the five shapes that code checks. That matters for one part of what arrives, `:sealed`; see "The
door's declaration".

One thing here is the build session's to say, and the plan does not decide it. `act.clj`'s namespace
docstring says "Callers. Only the proof of this namespace requires it" and "Neither the door nor a
gate is built". The first stops being true when the declaration requires `act.clj`, the second when
the gate is built. The ruling keeps what the docstrings claim; the tree's rules ask that an
explanation change with the code (`src-store/CLAUDE.md`, "Change the explanation with the code").

The folder's `README.md` is the module's README (C "Explanations"). It gets a row for each file, and
the three sections that the contract names: "Decisions this module stands on", "Engineering choices"
(each of the plan's choices listed near the end of this paper, with its reason), and "Words a session
made, Sid's to change". It also says what rule 14 asks it to say: until sign-in is built, any sender
can make any layer that is not made, for any holder. A part that is not built stays out of it. The
map of `test-store/` gets the proof: how to run it, what a pass shows, and what it does not reach. The
namespaces marked "no" load under the alias `:store`.

**Proof**: `test-store/softland/store/module_proof.clj`, namespace `softland.store.module-proof`, on
Rama's in-process cluster, in the style of `act_proof.clj`: a program that prints `SHOWN` lines, prints
them again together at the end, and exits with a status that is not zero when a check fails.

**Classpath: a second alias, `:store-module`. Rama does not go into `:store`.** The plan's choice, which
the contract leaves open (C "Classpath") and the README decides in effect. Reason: `:store` is the guard
that the pure code needs "local Clojure and nothing else" (`test-store/README.md`) and that "a require
of any of them from here fails at load" (R "This tree has its own classpath"). Both sentences stay true
only while `:store` holds Clojure alone. With Rama in `:store`, the act's proof would pass even if
`act.clj` or one of the pure namespaces above started to require Rama. So:

- `:store` stays as it is. `clojure -M:store -m softland.store.act-proof` still runs with Clojure alone.
- `:store-module` is a whole alias, not an addition: `:replace-paths ["src-store" "test-store"]`,
  `:replace-deps` with `org.clojure/clojure 1.12.4` and `com.rpl/rama 1.6.0`, and the same `:jvm-opts`
  as `:store` (`-Xss16m`, which the root `deps.edn` says Rama's module compilation needs, and `-Xmx4g`).
  The root's `:mvn/repos` entry for Red Planet Labs' repository already applies to every alias.
- The proof runs as `clojure -M:store-module -m softland.store.module-proof`.
- `com.rpl/rama-helpers 0.10.0` is left out: this design uses none of its helpers. If the build finds
  that Rama's in-process cluster wants a logging binding on the classpath, that is a finding of the
  build, added to this alias with its reason.

`src-store/README.md` gets one paragraph that says which alias holds what and why (C "Classpath").

## Reads
<!-- For each read operation: access method, path expression, partition -->

Three reads are open to a caller (IS "What the door reads from the module"). Each is one roundtrip from
the caller, lands on one task, and costs one read there.

**R1. The answer by act-id (IS O8; C item 8; C rule 11).**
- Access method: the query topology `answer-by-act-id`. *Given*: the contract names a query topology
  for it. By the skill's first question it could be one foreign select in this part (one PState, one
  key, one partition). It is a query topology for two reasons that the spec states: the contract says
  so, and the next part must complete it for an act-id of the other class without changing a caller
  (`DECOMPOSITION.json`, this part, point 10).
- Path: `$$streaming-answers` at the key `"<layer> <uuid>"`, the whole row.
- Partition: the layer's task, by the layer slot of the act-id.
- It gives `{:verdict :accepted | :refused, :reason word or nil, :stamp integer, :digest hex text}`, or
  nil for "nothing yet". Nil also for a thing that is not an act-id (IS X110).
- Latency: single-digit milliseconds; it is on the path of every act (IS O8). Throughput: one read at
  least for each act.

**R2. The bytes of a request's locks (IS O9; C item 7).**
- Access method: the query topology `locks-of-request`. *Given*: "the door reads a lock's bytes by a
  query". One roundtrip gives all the locks of one request (IS O9, "without one roundtrip for each
  lock").
- Path: `$$streaming-locks`, the range of keys from `"<layer> <uuid> <from>"` to
  `"<layer> <uuid> <to>"`, the `:bytes` and the `:way` of each row.
- Partition: the layer's task, by the layer slot of the request's act-id.
- It gives a map from index to a lock, `{:bytes … :way …}`, with an entry for each lock in the range
  that the store made, in each state of the lock (IS O9). The door needs the way with the bytes: it
  seals in the way that the lock is for. An empty map when the request made no lock, is not yet
  visible, or the arguments are not what they should be (IS X97, X98).
- Latency: tens of milliseconds are acceptable; locks are read ahead of need. Throughput: one read of
  this kind for each request, so the number of values divided by the locks in one request.

**R3. The class of a layer (IS O10; C "The proof", "reads the class"; C rule 7).**
- Access method: the query topology `class-of-layer`. The plan's, since the contract "does not say how
  the module serves it" (IS O10). A query topology for the same reason as R1: the next part completes
  it for a layer of the other class.
- Path: `$$streaming-facts` at the key `"<layer> <layer> :class"`, the `:value` of the row. The class is
  a fact on the layer's own entity (F1 A; C item 2), so its place is the layer as entity and `:class` as
  key.
- Partition: the layer's task.
- It gives `:streaming`, or nil for a layer that the streaming gate has not made (IS X116).
- Latency: single-digit milliseconds. Throughput: at most one for each act; a door may keep what it
  read, because no act of this build changes a class (IS O10).

**R4. The strays of a task (C rule 10).** Not an operation of the implied spec; rule 10 says the gate
keeps a sign of each stray, and a sign that nobody can read is no sign. One foreign select on
`$$streaming-task`, path `[:strays (keypath reason) (view count)]`, with the layer of the stray thing
as the partitioning key. One PState, one partition, so no query topology (the skill's first question).
An operator's read; it is on no act's path.

The reads that the gate itself makes for one act are in "The checks of one act".

## Writes
<!-- For each write operation: depot, event type -->

**W1. A thing for a streaming layer (IS O1, and O3 to O7 as far as they go through it).**
- Depot: `*streaming-depot`, appended by the door (in this build, by the proof's hand) with the ack
  level `:ack`.
- Event type: one. The record is the thing as it arrives: the maker's act, plus `:act-id` and `:class`,
  with each value fact's `:v` replaced by `:sealed` and `:lock-id` (C item 2). There is no dispatch by
  type. Making a layer, writing values and retracts, giving a permission and asking for locks are what
  the facts of an act do, and one act can do several of them (IS "The operations, in short"). A repeat
  (IS O7) is the same record a second time.
- Latency: the decision and its facts are visible within single-digit milliseconds at normal load
  (IS O1). After the append's ack, R1 gives the answer.

No other write enters the module in this part. A request for locks is an act (C item 7). The microbatch
depot is the next part's.

## PState Design
<!-- For each PState:
If obvious: why this is the only reasonable design
If not obvious:
  Option A: schema — query cost: N seeks, M iterations because why
  Option B: schema — query cost: N seeks, M iterations because why
  Chosen: A or B because why -->

### What the record must serve

The dominant work is not a caller's read. It is the gate's own reads for one act, once for every act
(IS O1). For one accepted act the gate must learn four things from the record, and each is reached by a
different key:

1. whether the act-id is used: by the act's uuid (check (a));
2. whether the act's permission stands: by the act's `:who` (check (d));
3. the lock of each sealed value, with its bytes: by the lock's id (C item 7; C rule 16);
4. the current fact of each place that a fact replaces: by entity and key (check (e)).

And one thing from the task: its last stamp (C item 6). Everything of one streaming layer is on one task
(*given*: C items 1 and 3), so the only question is how many reads each of these costs there.

### The first question: one PState keyed by the layer, or flat keys

The skill's rule is to collect data that shares a key and a partitioner into one PState. Every row of a
layer shares the layer and the layer's task. So the first design that comes to mind is Option A. It was
costed against two others. The cost model is the skill's: "each path navigation through a top-level key
or subindexed structure is a RocksDB read" (SKILL.md, goal 7), and "`keypath` + further navigation does
read" on a write (skill paths.md, "No-Read Optimizations").

**Option A. One PState, key = the layer; its value a fixed-keys record of three subindexed maps**
(answers by uuid, facts by place, locks by lock id).
- Each read of a row costs 2: the layer's row (it holds the handles of the subindexed maps), then the
  element. Each write must load the layer's row before it can reach into a subindexed map: 1 more.
- One accepted act with one sealed value, with the reads in stages as the checks need them: answer 2,
  permission 2, lock 2, last stamp 1, the write's navigation 1 = **8 reads**.
- With every select of the act merged into one path, so that the layer's row is loaded once for the
  reads: 1 + 3, last stamp 1, the write 1 = **6 reads**. But a merged select reads the permission row
  and the lock rows before check (a) has answered a repeat, and reads up to 256 lock rows of an act
  whose permission does not stand. The stages are lost.
- PState partitions on each task: 2 (this one and the task's own).

**Option B. Three flat PStates: the top-level key holds the layer and the rest of the key.**
Answers under `"<layer> <uuid>"`, facts under `"<layer> <entity> <key>"`, locks under
`"<layer> <uuid> <index>"`.
- Each read of a row costs 1. Each write is a key and a whole row, which is the no-read case
  (`keypath` + `termval`).
- The same act, in stages: answer 1, permission 1, lock 1, last stamp 1, writes 0 = **4 reads**.
- PState partitions on each task: 4.

**Option B1. One flat PState for all three kinds of row**, the kind as a part of the key.
- Reads as B: **4**. PState partitions on each task: 2.
- One position then holds three kinds of row. The skill's schema rule for that is an interface and a
  record for each kind (skill pstate-schema.md, "Storing Polymorphic Data"). The schema of each kind
  would no longer be declared to Rama.

| | Reads for one accepted act, one sealed value | Latency of those reads, cold (0.3–0.5 ms each) | PState partitions per task |
|---|---|---|---|
| A, staged | 8 | 2.4–4.0 ms | 2 |
| A, merged select | 6 | 1.8–3.0 ms | 2 |
| B | 4 | 1.2–2.0 ms | 4 |
| B1 | 4 | 1.2–2.0 ms | 2 |

Throughput is the same arithmetic times the acts each second on the task: A spends 2 to 4 more reads
on every act than B, for ever.

**Chosen: B.** It was not a close call once costed: A costs 50% to 100% more reads on the one operation
that runs for every act, and the first two limits of F9 (C rule 21) are about exactly that operation.
"Never trade I/O efficiency for code simplicity" (SKILL.md). What B costs is two more PState partitions
on each task. B1 would give that memory back and keep the 4 reads; it is not taken, because it trades
the typed schema of each kind of row for memory only, and nothing in the spec bounds memory. If the
build measures four partitions a task as a problem, B1 is the fallback: it changes no key, no read
count and no function of the record's rows.

**Why the three are not one PState** (the validation's question for PStates that share a key type and a
partitioner): they share no key. An answer is found by an act's uuid, a fact by its place, a lock by its
id. There is no key at which an answer, a fact and a lock would be three fields. The one thing they
share, the layer, is the partition; making it the top-level key is Option A, costed above.

### Why the keys are strings

A key here is a pair or a triple, and one of its parts, an id, is "a readable keyword or a UUID"
(R 6-Oct 9): two classes. The schema needs one concrete class, and never `Object`.

- A string made of the parts' texts: one class; equal keys are equal strings; and Rama's docs give the
  order of string keys ("Rama implements this property as well for built-in serialization for relevant
  types like numbers and strings", docs 17:169). The range read of a request's locks rests on that order.
- A vector `[layer uuid index]`: one class, equal vectors serialise the same (docs 17:167), but the docs
  do not give the order of vectors as keys. The lock range would rest on something not documented.
- Nested maps, layer → uuid → index, each of a simple class: documented order at each level, and 1 more
  read for each level (Option A's cost).

**Chosen: strings.** The text of an id is its printed form: `:alice`, `:tool/class`,
`0b5f1c9e-…` (36 characters). A readable keyword and a UUID never print alike, and neither holds a
space (`act.clj`, `token-pattern`), so one space parts the parts and no two different keys give one
string. A lock's index is three digits, `000` to `255`, so that the order of the strings is the order
of the indexes. Only ids that passed the declaration, or the test at the head of a query, ever reach a
key. Cost: a UUID is 36 bytes in a key where 16 would do. RocksDB stores a key as its difference from
the key before it in a block, so the shared front of neighbouring keys (the layer; for locks the
request's uuid too) is paid once in a block, not once in a key (*not verified*, not measured).

### `$$streaming-answers`: the answer under each act-id

```
{ String                               "<layer> <act uuid>"
  -> fixed-keys { :refused Keyword     the reason word; absent when the act was accepted
                  :stamp   Long
                  :digest  bytes } }   32 bytes: SHA-256 of the thing's one text
```

A point lookup by key, so the map is the obvious shape. Three things were weighed inside the row.

- The class slot of the act-id is not in the key: every row of this PState is of the class streaming,
  because the gate answers only under an act-id of its own class (C rule 10). The layer is in the key,
  although the uuid alone is random: two layers can share a task, and a forger can send a uuid that is
  used in another layer; without the layer in the key the two would meet on one row.
- `:accepted` is not stored as a word. A row with no `:refused` is an accepted act. The contract's
  shape, "`:accepted | :refused reason, stamp`", is what R1 gives back; the row keeps the smaller form,
  because there is one row for every act for ever (IS C2).
- The digest is kept whole, 32 raw bytes, and given to callers as hex text. A cut to 16 bytes would
  still leave no practical way to find a second thing with the digest of a given one; it is not taken,
  because "a short hash is not enough" (IS C6) is easier to hold with the whole of a standard digest
  than with an argument about a cut. Said again under "Minimization".

The row is written once and never changed or deleted (IS C2). It holds nothing of the act beyond its
digest: `:based-on`, `:because-of`, `:subjects`, `:session` and `:claimed-when` stay in the depot
(IS O4, "given by silence").

### `$$streaming-facts`: the current fact of each place

```
{ String                                 "<layer> <entity> <key>"
  -> fixed-keys { :value      bytes      the value's bytes: sealed when :lock-act is there, the one
                                         text of a plain value when it is not; absent for a retract
                  :lock-act   UUID       the lock that seals :value is [[layer :streaming lock-act] lock-index]
                  :lock-index Long
                  :by-act     UUID       the fact that wrote this row is [[layer :streaming by-act] by-index]
                  :by-index   Long
                  :stamp      Long } }
```

This is the contract's "`entity → key → {value or sealed, the fact-id that wrote it, the stamp}`"
(C item 5), with entity and key flattened into one sorted key so that a place costs 1 read, not 2 (a
map of entities whose values are maps of keys, both subindexed, costs one read for each level). The
keys of one entity stay next to each other in the order, and the entities of one layer too.

**Every accepted fact is a row here**, under a store key or a plain key. So "the layer's settings as
facts on its own entity" and "permissions as facts" (C item 5) are rows of this same map: the class at
`"<layer> <layer> :class"`, a permission at `"<layer> <holder> :permission"`, a request for locks at
`"<layer> <act uuid> :lock-request"`. Their `:value` is the one text of the plain value, as bytes:
`:streaming`, `[:agent-1 :alice :alice [:alice :alice :alice]]`, `16`. A fact under a store key keeps
a plain value (C item 2), and the row says so by having no lock.

**One shape for every row.** A row is "the current fact of a place": which fact wrote it, when, the
bytes of its value if it has a value, and the lock that seals those bytes if a lock does. A retract
has no value; a plain value has no lock. Those are nullable fields on one shape, which the skill
allows. The alternative was an interface with three records (sealed, retract, plain). It was weighed
and not taken: the three would hold the same fields under three class names, and the plain value's
field would be of no fixed class (a keyword, a vector or an integer), which is what a typed schema is
there to avoid. Holding a plain value as the bytes of its one text gives the field one class and makes
"is this the permission that the act names" a comparison of bytes with no decoding.

**Which entity a permission fact sits on** (C rule 13, last sentence: "the plan's"). Three ways were
costed:

1. *On its holder*: the place is the holder as entity and `:permission` as key. Check (d) is then 1
   read of this map by the act's `:who`, and there is no second structure. Consequence: a holder has
   one permission in a layer. A second one for the same holder lands on a place that holds one, and
   rule 13 refuses it.
2. *On any entity the giver picks, with a second map from permission id to its fact*: any number of
   permissions for a holder. Check (d) is still 1 read, of the second map. A give costs 2 reads and 2
   writes instead of 1 and 1, and there is one more PState.
3. *On an entity whose id is a UUID made from the digest of the permission id*: any number for a holder,
   1 read, one structure. But the entity of a permission is then an id that no person can read, and the
   door must compute it.

**The plan's choice: 1.** It is the form in which "permissions as facts" is literally the structure
that the gate reads; it costs least; and it reads plainly: `{:e :agent-1 :k :permission :v
[:agent-1 :alice :alice [:alice :alice :alice]]}` says "agent-1's permission in this layer is this".
The README's line for E4 A is in the singular ("each actor acts under its own permission", R 8-Oct 3,
a session's reading that Sid let stand); the plan does not take that as a decision. **Sid should know: in this build a holder has one permission in a
layer.** One case that the implied spec had as accepted now falls under rule 13: a holder that gives
itself a second permission beneath its own (IS X85) writes to a place that holds one, and is refused.
If Sid wants more than one, the change is the declaration's rule for the entity of a permission fact
and the key that check (d) reads (way 2 or 3); the rows of permissions already given would move. No
other part of the design changes.

**Which entity a request for locks sits on.** A store key takes one write for a place (C rule 13), so
two requests cannot share a place. **The plan's choice:** the fact of a request sits on the entity that
is the uuid of its own act. Then the place of a request is new by construction, so the gate needs no
read to hold rule 13 for it; an act holds one request at most (C rule 12: a place once); and the id of
each lock follows from the request's act-id (see `$$streaming-locks`).

### `$$streaming-locks`: each lock, and the fact that it seals

```
{ String                                  "<layer> <request uuid> <index, three digits>"
  -> fixed-keys { :bytes       bytes      32 bytes; written once when the request is accepted
                  :way         Keyword    the word of the way of sealing that the lock is for
                  :seals-act   UUID       absent until the lock seals an accepted value; then the fact
                  :seals-index Long } }   that it seals is [[layer :streaming seals-act] seals-index]
```

**The way of sealing is said by the lock, not by a mark in each value** (ruled by the build session,
8 October). A sealed value carries no version byte. The lock's row holds one word, `:way`, for the way
of sealing that the lock is for, written with the bytes when the lock is made. This build knows one
way and makes every lock for it: `:aes-256-gcm`, which is AES-256-GCM with a random part of 12 bytes
and the place as associated data ("Locks and the seal"). The seal and the gate read the word from the
row. So the way can change later without touching a stored value: a new way is new locks with a new
word, and each value that is already sealed keeps its lock and its lock's word. What the word costs is
bytes in every lock row; the figure is under "Resource usage analysis".

**A lock's id is `[request's act-id, index]`.** The door finds the locks of its request from the
request's act-id alone (IS O6, from R 7-Oct 7): it needs no read to know their ids, and a retry of the
request cannot make locks under other ids. That is the skill's own rule for a stream topology: an id
made inside one comes out different on a retry, so ids come with the record (skill unique-ids.md). The
gate makes a lock's bytes, never its id. The id carries its layer, so "a lock that the store did not
make for this layer" is first a comparison and only then a read.

Options for the rows:

- **One row for each lock, keyed so that the locks of one request are neighbours** (chosen). The seal
  check of one value is 1 read. The locks of one request are 1 read and one iteration each, for R2 and
  for an act that carries several values. Marking a lock as sealing a fact is one small write.
- *One row for each request, holding all its locks.* The seal check is 1 read of up to 256 locks to
  use one, and each use writes the whole row again: about 8 to 20 KB written for each value at 256
  locks. Not taken.
- *The lock's bytes moved into the fact's row when the lock is used.* The lock would then sit in the
  very row of its value. But R2 gives a lock's bytes "in each state of the lock" by its id (IS O9), and
  a fact that is replaced leaves the facts map while its lock must stay (IS O4). So a table by lock id
  is needed in any case, and a second copy of the bytes adds nothing.

"Co-located with their values in the record" (C item 7) holds in the sense that matters for a forget:
the row of a value names its lock, the row of the lock names its fact, and both are on the layer's
task, so destroying a lock is one local delete of one row. The step itself is not built.

Room that this row leaves (C item 7, "not in this build, room left"): a lock wrapped under the locks
of the people the value is about is one more field on the row; a destroyed lock is the row's absence;
the other grains of H1 (one lock for each act, or one for each session and layer; R "Open") keep the
key and the bytes and change `:seals-act`/`:seals-index` and the one check "a second value under a
lock that already seals one".

### `$$streaming-task`: what the gate keeps for a task as a whole

```
fixed-keys { :last-stamp Long
             :strays     { Keyword -> set of String, subindexed } }   reason word -> the digests (hex)
                                                                       of the strays seen on this task
```

Two things that belong to a task and to no layer, in one PState with a fixed-keys top level, so that
they cost one PState partition and not two.

**`:last-stamp`.** "Strictly increasing on its task" (C item 6) is about the task, not the layer: two
layers on one task must not get the same stamp or a smaller one after a clock that stepped back. So
the state is one integer for each task. It is read (1 read) and written at each decision. It is a
PState because it must survive a restart and a move of the task's leader (IS C7, C8).
- *The alternative: keep the last stamp in the memory of the task and read the PState only after a
  restart.* It would save 1 read of the 4. It is a cache of durable state, and the project's rule on
  caching is to stop and examine in a new session before reaching for one (`CLAUDE.md`, "Systemic
  repair and caching"). So it is not taken here. The stamp function takes the last stamp as an argument,
  so where it comes from is one line, should that examination find for it.

**`:strays`.** Rule 10 asks that the gate count a stray by a reason word, on its task, and that a stray
write a bounded amount. Three ways were built on paper:

1. *A counter for each reason, added to in the stream event.* 8 bytes for each reason. But a stream
   topology processes a record again after some failures (docs 11:156, 11:166), and with the retry
   mode this gate uses, every record after a failed one is processed again (docs 11:162). Each time,
   a stray would be counted again. The skill does not allow that: "duplicate side effects from retried
   processing are bugs, not acceptable tradeoffs" (SKILL.md).
2. *A counter kept by a microbatch topology that reads the streaming depot a second time.* Exact, also
   for the same stray sent twice. Cost: every record of the streaming depot is read and deserialised
   twice for ever, to move one rare write; and the stream gate must still test each thing for a stray
   itself, because it cannot look up an answer without a usable act-id. In numbers, for the example
   task of "Resource usage analysis" (100 acts a second, about 580 bytes each): 8.64 million more
   record reads and about 5 GB of depot read again each day, on that one task. If 1 thing in 10,000
   is a stray, way 3 does 864 small writes a day there instead.
3. *A set for each reason of the digests of the strays seen, added to in the stream event* (chosen).
   Adding to a set is the same write however often it runs, so a retry changes nothing. A non-stray
   costs nothing. A stray costs its digest, 1 or 2 reads and one small write. One stray writes about 70
   bytes, once; the same stray thing sent a thousand times writes once.

**The plan's choice: 3.** What it means in plain words: the sign is the set of the different stray
things seen, by reason; the count of rule 10 is the size of the set (size tracking stays on, so the
size is one read). The same stray thing sent twice counts once. If an exact tally of arrivals is
wanted, way 2 gives it at the cost named. The outer map is not subindexed: its keys are the four
reason words of a stray, a closed set declared in `softland.store.declaration`. Each inner set is
subindexed: it grows with the strays.

## Depots
<!-- - *name: (hash-by :key), event types [...] -->

- `*streaming-depot`: `(hash-by :layer)`. One kind of record: a thing, as it was sent (W1).

*Given*: two depots, one for each gate, called the streaming depot and the microbatch depot (F6 A,
Q7 a); the streaming depot partitions by the act's `:layer` (C item 1). This part declares the first.

- **One depot for every act of a streaming layer.** The acts of one layer are order-dependent on each
  other: a give and the first act under it, a request and the first use of its lock, two acts that
  replace one fact. One depot and one partition for a layer give them the depot's order (skill
  depot-design.md, "Same depot when events require local ordering").
- **Colocation.** The depot's partition for a layer is the task on which every row of that layer
  lives, so the gate reaches the record with no hop.
- **Never trimmed** (C rule 4). The module sets no trimming option, and "by default depots store all
  data permanently" (skill depot-reference.md, "Depot Trimming").
- **Nothing secret goes on it** (C rule 5; IS C9). A request for locks carries a count. A lock's bytes
  are made in the gate's event and written to a PState; no code path of this design appends them.
- **Any thing can arrive** (IS C1, C10). The partitioner applies the keyword `:layer` to the thing. On
  a thing that is no map a keyword gives nil (or, on a set, the keyword itself), and it does not
  throw. It runs on the appending client, where a throw goes to the caller of the append and not to
  the gate (skill depot-design.md). So no thing can stop a partition through the partitioner.
- **Ack level.** The door appends with `:ack`: the append returns when the stream topology has
  processed the record and its PState writes are replicated and visible (docs 14:206). An ack can
  fail for an act that the gate accepted: "if it fails the first time the stream topology tries to
  process it but succeeds when the topology retries it, the client append call will get an exception"
  (docs 14:214). That is Q22 A's reason; the door then reads by act-id.

Not guarded: a thing that sits on a partition other than the one its `:layer` hashes to. Rama's client
computes the partition from the thing with the declared partitioner, and this stage found no call that
lets an appender pick the partition of such a depot (*not verified*).

There is no internal depot.

## Topologies and PStates
<!-- Each topology owns zero or more PStates. Only the owning topology can write to a PState.
A module can have multiple topologies of different types. (The template's rules on topology type and
count were read and are answered below.) -->

### `streaming-gate`: stream

**Why stream.** Both stream reasons apply, and the contract names the type (C item 3).
(a) Latency: a streaming layer is "work a person engages with directly", which "must be interactive"
(R 7-Oct 4); the decision and its facts are needed within single-digit milliseconds (IS O1). A
microbatch has at least 300 ms. (b) Ack coordination: the door reads the answer by act-id right after
the ack (R 7-Oct 6; IS X111), so the append must block until the record's change is visible.

**It is the only topology of this part.** The next part adds the microbatch gate as a second topology.
Rama allows one topology to write a PState, so each gate owns its own PStates; "the record is in the
gates' PStates" (F2 A).

**The source.** `*streaming-depot`, with two options, both the plan's choice:
- start from the beginning: no thing that is in the depot is ever skipped, whenever the topology first
  runs on a partition;
- retry mode "all after": when a record fails, that record and every record after it on the same
  partition are processed again; it is the mode that the docs give for when "the order of processing
  is critical" (docs 11:162). The gate is order-sensitive, and a record that was already decided
  costs 1 read when it comes again (check (a)). The default mode retries only
  the failed records and "can't guarantee" their order (docs 11:158). What this does and does not give
  is under "What the plan found out", X29.

**The event has no partitioner.** It starts on the depot partition's task, which is the layer's task,
and ends there. So all its PState writes are one atomic group: "all writes to PStates from all events
during one of these batches are made visible at the same time" (docs 11:140), and when a batch fails
"all PState writes during that batch of execution will be discarded" (docs 11:170). That one fact
carries IS C3 (whole or nothing, visible together) and makes every retry safe.

**The event does not yield.** While one act is being decided, no other event of the task may run
between its reads and its writes; a yield would let one in, and reads on the two sides of a yield "are
NOT atomic" (skill dataflow.md, "Yielding"). The event uses none of the things that suspend an event
(a partitioner, a depot append, a future). So its length is the work of one act, and rule 20's limits
bound that work; the figures are under "Resource usage analysis".

**The walk of one event.**

0. *Stray?* (C rule 10). `declaration/stray-reason` is given the thing and this gate's class. If it
   gives a word, the gate takes the thing's digest, adds it to `:strays` under that word in
   `$$streaming-task`, and ends. No answer, no stamp.
1. *Check (a).* Read `$$streaming-answers` at the act-id's key. If a row is there, the event ends and
   writes nothing, whether the thing is the same act again or a different one (C rules 1 and 11).
2. *Digest, and check (b).* `digest/digest` of the thing as it arrived; then `declaration/check`. A
   refusal goes to step 7 with its word.
3. *The clock, once.* The gate reads the wall clock one time for this decision. The stamps that the act
   carries in `:based-on` are held to the 10-second rule (C rule 17).
4. *Standing: checks (c), (d), and the first act of a layer (C item 9).* Read the place
   `"<layer> <who> :permission"` in `$$streaming-facts`. If it holds the act's permission, both checks
   pass and nothing more is read. If not, read `"<layer> <layer> :class"`: there, the layer is made
   and (d) refuses; not there, the layer is not made by this gate, and the act is accepted only if it
   is a right making act.
5. *Places: checks (e), (f), (g), and what a store key does not take in this build.* Read, in one
   select, the row of each place that a fact names in `:replaces`, and of each place of a fact under
   `:permission`. Then the checks, in order.
6. *The seal.* For each request whose locks the act names, one range read of `$$streaming-locks`. Then,
   for each sealed fact in the act's order: the lock is there, it seals nothing, and it opens the
   bytes at this fact's place (C item 7; C rule 16).
7. *The stamp, and the writes.* Read `:last-stamp` in `$$streaming-task`; `stamp/next` gives the stamp.
   A refusal writes the answer row and the last stamp, and nothing else (C rule 15). An accept writes
   the row of each fact, the row of each lock that the act uses (its bytes as read, and the fact it now
   seals), the rows of the locks that a request asks for (fresh bytes), the answer row, and the last
   stamp.

The stages are in the contract's order, and a later stage's rows are read only when the earlier checks
passed. So an act whose permission does not stand costs 4 reads whatever it carries (the answer, the
permission, the class, the last stamp for its refusal), and cannot make the gate read 256 places or
open 16 MB.

**The processing concerns, and whether each needs a stream topology.**
- *The decision of an act and the writing of its facts and answer.* Yes: reasons (a) and (b) above.
- *Making locks on a request.* Yes: the request is an act, and its locks must be visible together with
  its answer (IS C3), so they are written in the same event.
- *The sign of a stray.* The test for a stray is the first step of every event and cannot leave it:
  the gate cannot look up an answer before it knows that the thing has an act-id of this gate. The
  write of the sign could be moved to a microbatch topology, and was costed (`$$streaming-task`, way
  2): it would make a second topology read and deserialise every record of the streaming depot, for
  ever, to move one write that almost no record causes. The write stays in the event that found the
  stray.

**Each write, and what a retry does to it.** A stream topology can process a record again after all
its writes were committed (skill stream.md, "Stream retries and exactly-once").

| Write | Kind | A retry after the first run was committed | A retry after the first run was discarded |
|---|---|---|---|
| Answer row | whole row at a fixed key | check (a) finds the row; the event ends; nothing is written | written for the first time |
| Fact rows | whole row at a fixed key | not reached | written for the first time |
| Lock rows made by a request | whole row at a fixed key, fresh random bytes | not reached, so the bytes that a reader may have seen stay (C rule 5) | written for the first time; the bytes of the first run were never visible and may differ (IS X94) |
| Lock row of a used lock | whole row at a fixed key | not reached | written for the first time |
| `:last-stamp` | one value | not reached | written; the stamp of the first run was never visible and may differ (IS C7) |
| A stray's digest | add to a set | the same element again: no change | added for the first time |

No write appends to a list or adds to a counter. Every write of a decision is behind check (a), and the
answer row commits in the same batch as the rest. The one write that is not behind check (a), the
stray's, is idempotent by its kind.

**Many senders, a sender that restarts, a worker that restarts.**
- *Two senders into one layer.* The depot's partition gives the order, and each act sees the record as
  the act before it left it (IS O1). Acts of two layers do not meet: no row belongs to two layers. Two
  layers on one task share only the task's last stamp, which one event reads and writes with nothing
  in between.
- *A sender that restarts, or that got no ack.* It reads by act-id. With no answer it sends the same
  thing again under the same act-id; if both copies arrive, the second is a repeat (IS O7, X26). The
  module keeps nothing that depends on a sender's memory.
- *A worker that stops in the middle of an event, or a task whose leader changes.* The batch's writes
  were discarded, or were committed whole. The record is processed again and the act is decided once
  (IS X27, X28). Nothing that a decision needs is in the memory of a process.
- *Scale.* Every collection is a top-level map on disk or a subindexed set. Nothing grows in memory.
  A very large layer is one task's load, the limit that the decision itself sets (IS O1).

**Nothing is given back in the ack.** The plan's choice. The ack is a signal, and "read it by act-id
before acting on a signal" is the rule for all code (F7 A). A value in the ack would be read by no one
who follows that rule. Adding one later is one line of the gate.

**PStates** (the schemas are under "PState Design"; no `Object` anywhere):

- `$$streaming-answers`: `{String -> fixed-keys{:refused Keyword, :stamp Long, :digest bytes}}`
- `$$streaming-facts`: `{String -> fixed-keys{:value bytes, :lock-act UUID, :lock-index Long, :by-act UUID, :by-index Long, :stamp Long}}`
- `$$streaming-locks`: `{String -> fixed-keys{:bytes bytes, :way Keyword, :seals-act UUID, :seals-index Long}}`
- `$$streaming-task`: `fixed-keys{:last-stamp Long, :strays {Keyword -> set of String, subindexed}}`

"bytes" is the JVM's byte array class. A byte array as the class of a schema position is *not
verified*; the build checks it first. If Rama refuses it, the build picks the nearest form and reports
it; no key and no read count depends on it.

A schema mismatch throws in the topology ("ValueSchemaMismatchException … schema Long, value Integer",
skill troubleshooting.md), and a throw there stops the partition (IS C1). So rows are made only by the
functions of `softland.store.record`, which give exactly the declared classes.

## Query Topologies
<!-- - name:
  Input example 1: describe input → N total reads, M meaningful (non-empty) reads
  Input example 2: different input → N total reads, M meaningful (non-empty) reads
  Fixed or variable: fixed if M is same for all inputs, variable if M differs
  If variable, dynamic approach: ops/explode + aggregator | loop<- | etc. -->

Each takes the layer as its first argument and begins with the hash partitioner on it. A leading
partitioner whose inputs are the topology's own arguments is evaluated by the client, which then sends
the query straight to the right task (skill query-topologies.md, "Leading partitioner"). With the
act-id as the only argument, the layer would have to be taken out of it first, and the query would
start on a random task and hop. The plan's choice: one hop less on a read that follows every act. The
caller still needs the act-id and nothing else; the door's hand passes its first slot. Each query
checks that the layer it was given is the layer slot of the id, and gives nothing when it is not. None
of them aggregates: each gives one value.

- **`answer-by-act-id`** `[layer act-id] → answer or nil`
  - Example 1: `:alice`, `[:alice :streaming u1]`, an act that was decided → 1 read, 1 meaningful.
  - Example 2: the same shape, an act that is not decided or never arrived → 1 read. Its result is the
    output: without the read, "nothing yet" cannot be told from an answer. 1 meaningful.
  - Example 3: a thing that is not an act-id, or a layer that is not the id's layer slot → 0 reads,
    nil (IS X110).
  - Example 4, in this part: an act-id of the class `:microbatch` → 0 reads, nil. The next part gives
    this branch its read.
  - Fixed: 1 read for each act-id of this gate. The inputs that cost 0 are stopped by a test before
    the read; no read is issued and left empty.

- **`locks-of-request`** `[layer request-act-id from to] → {index {:bytes … :way …}}`
  - Example 1: a request that made 64 locks, range 0 to 63 → 1 read and 64 iterations, all meaningful.
  - Example 2: the same request, range 10 to 10 → 1 read and 1 iteration.
  - Example 3: an act-id that made no lock (a refused request, an act that is no request) → 1 read,
    0 iterations; the empty map is the output.
  - Example 4: arguments of the wrong shape, or the class `:microbatch` in this part → 0 reads, an
    empty map.
  - Variable in the number of rows, fixed in the number of seeks. The variable part is one range
    navigator over neighbouring keys, not a read for each index. The range is cut to 0–255 (C rule
    20), so one call iterates 256 rows at most, about 1.3 ms, and needs no yield.

- **`class-of-layer`** `[layer] → class or nil`
  - Example 1: a layer that the streaming gate made → 1 read, 1 meaningful.
  - Example 2: a layer that is not made → 1 read; nil is the output (IS X116).
  - Example 3: a thing that is not an id → 0 reads, nil (IS X119).
  - Fixed: 1 read. In this part a layer of the other class reads as "not made here"; the next part
    adds its record's branch.

A lock's bytes leave the module through `locks-of-request`, to any caller, as rule 16 says. The locks
PState is not declared private: the two pages of Rama's docs differ on whether a query topology of the
module may still read a private PState (docs 15:222, "anything other than the owning topology";
docs 28:263, "only … from topology code within the module"). So a foreign select of
`$$streaming-locks` is a second way out in this build. Rule 16 already holds that any caller can read
a lock.

## Partitioning efficiency

**The placement that the reads want.** For every read of this part the key names a layer, and the
answer is one row or one run of neighbouring rows of that layer. The gate's own reads for one act are
all of one layer too. So the placement that costs least is `f(layer) = one task`, the same task for
every row of the layer and for the depot's partition of the layer; then each read is 1 seek on 1 task
and the gate never hops. *Given* as well: "one event per act, on the layer's task" (C item 3); "the
streaming depot partitions by the act's `:layer`" (C item 1). The partitioner that implements `f` is
the hash of the layer id: `hash-by :layer` on the depot, the hash partitioner on the layer at the head
of each query. No row of this part is written after a partitioner inside a topology.

A row's key is longer than the layer, and its placement is by the layer alone. That is sound in Rama:
"a write lands on whatever task the topology routes to before the `local-transform>` — there is no
default placement" (skill pstate-schema.md, "Partitioning control"). The cost is that no caller may
use a select that routes by the whole key, since the hash of the whole key names another task. None
does: the three queries route by the layer, and the operator's select of strays passes the layer as
its partitioning key. No key partitioner is declared on the PStates.

**Balance.** Many layers share a task (one for each person, one for each group; IS O3), so the hash
spreads them evenly. One layer cannot be spread: "a very large layer is one task's load. That follows
from the decision; it is a known limit, not a fault to design round" (IS O1, from C items 1 and 3).

**The dominant read: the answer by act-id (R1), once for each act.** Seeks are totals across all
tasks. The proportions are this stage's assumption about a door that reads once after each ack; they
are not measured.

### N = 1 task (single-task baseline)
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| An act-id whose answer is recorded | 0.97 | 1 | 0 |
| An act-id with no answer yet (not arrived, not decided, or a stray) | 0.02 | 1 | 0 |
| Not an act-id of this gate (wrong shape, wrong layer argument) | 0.01 | 0 | 0 |

Weighted seeks = 0.99   |   Weighted iterator reads = 0

### N = 16 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| An act-id whose answer is recorded | 0.97 | 1 | 0 |
| An act-id with no answer yet (not arrived, not decided, or a stray) | 0.02 | 1 | 0 |
| Not an act-id of this gate (wrong shape, wrong layer argument) | 0.01 | 0 | 0 |

Weighted seeks = 0.99   |   Weighted iterator reads = 0

### N = 128 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| An act-id whose answer is recorded | 0.97 | 1 | 0 |
| An act-id with no answer yet (not arrived, not decided, or a stray) | 0.02 | 1 | 0 |
| Not an act-id of this gate (wrong shape, wrong layer argument) | 0.01 | 0 | 0 |

Weighted seeks = 0.99   |   Weighted iterator reads = 0

Flat from 1 task to 128: the read goes to the one task that the layer hashes to, whatever the number
of tasks. The same holds for R2 (1 seek and one iteration for each lock, on one task), for R3 (1 seek
on one task), and for the gate's reads for one act (4 for the common act, all on the layer's task; the
table is in "The checks of one act"). No read of this part fans out.

## Design Decisions
<!-- - Subindexing: which PState collections are subindexed and why
- Colocation: how depot partitioning aligns with PState keys -->

### Subindexing

- The three keyed PStates are top-level maps. Rama keeps each key of a top-level map as its own entry
  on disk, sorted, so they need no subindexing and can grow without limit.
- No row holds a collection. Each field of a row is one value: a keyword, an integer, a UUID or a run
  of bytes. The longest is a sealed value, at most 65,536 bytes (C rule 20).
- `$$streaming-task` `:strays`: the outer map, reason word → set, is not subindexed. Its size is
  enforced by code: its keys are the four words that `declaration/stray-reason` can give, and nothing
  else writes to it. Each inner set is subindexed, with size tracking on: it grows by one for each
  different stray, and its size is the count that rule 10 asks for.

### Colocation

The depot's partition for a layer, every row of the layer, and each query about the layer are on one
task: the hash of the layer id. The gate's event never leaves that task, and no query hops.

### The door's declaration: `softland.store.declaration`

*Given* (C item 2; R 6-Oct 2, 3): what arrives on a depot is the maker's act plus `:act-id` and
`:class`, with each value fact's `:v` replaced by `:sealed` bytes and a `:lock-id`; facts under a store
key keep a plain value; the door's additions are "the door's own small declaration, which contains the
maker's" (R 6-Oct 2). *Ruled by the build session, 8 October*: the engine of `act.clj` takes the map of
shapes and the nesting limits. So the declaration is data in the language of `act.clj`'s `shapes`, and
the same engine reads it. The next part's gate checks what arrives against this same declaration.

The namespace is made as `act.clj` is made, of the same three things in the same order: the shapes, as
data; the rules, each a name and a reason word, as data, with the code of each rule in one place; and
the check, which reads the two and holds no knowledge of the form itself. So the order of the
declarations is the order of the reasons, as it is for the maker's act.

**The shapes.** The door's map of shapes is `act.clj`'s map, `act/shapes`, with five entries added.
The maker's shapes are in it as `act.clj` declares them, and are not written a second time. That is
how the door's declaration contains the maker's.

| Name | Shape | What it is |
|---|---|---|
| `:arrived-act` | `[:parts [:act-id :act-id] [:class :class-word {:reason :class-not-the-act-ids-class}] …]`. The `…` is the ten parts of the maker's `:act`, read from `act/shapes`, in its order and with its options, with one changed: `:facts` holds `[:seq-of :arrived-fact]` | An act as it arrives on a depot |
| `:arrived-fact` | `[:parts [:e :id] [:k :key] [:v :value {:optional true}] [:replaces :fact-id {:optional true}] [:lock-id :lock-id {:optional true :reason :lock-id-malformed}]]` | A fact as it arrives. Its `:sealed` is not in the shape; see below |
| `:class-word` | `[:enum :streaming :microbatch]` | One of the two class words |
| `:lock-id` | `[:tuple :act-id [:int {:min 0 :below 256}]]` | The id of a lock: the act-id of its request, and its index |
| `:lock-count` | `[:int {:min 1}]` | How many locks a request asks for |

The nesting limits that the declaration gives the engine are the maker's one: a permission id inside
itself, at most `act/max-permission-depth` times. No shape of the door's is inside itself.

*Which reason each part gives*, when the engine reads a thing as `:arrived-act`. A part that the
declaration does not name gives `:unknown-part`. `:class`, when it is absent or no class word, gives
`:class-not-the-act-ids-class`. Each of the maker's parts gives the word that `act.clj` declares for
it (`:unknown-version`, `:no-permission`, `:no-session`, `:empty-act`), or `:malformed` where it
declares none. A fact's `:lock-id` gives `:lock-id-malformed`; each other part of a fact gives
`:malformed`. `:act-id` always fits: a thing whose act-id does not is a stray and never reaches the
check.

**The one part that the engine does not read: `:sealed`.** The engine as ruled takes the shapes and
the nesting limits. The leaves, the five shapes that code checks, stay `act.clj`'s own. None of them
fits a run of bytes, and no other shape of the language does. So `:sealed` cannot be given a shape.
The check sets each fact's `:sealed` aside before the engine reads the thing, and puts it back on the
one form that the engine gives; two rules, in code, then hold it. This is the one thing kept in code
that the ruling does not list. If the new function of `act.clj` also took leaves that a caller adds,
`:sealed` would be a part like the others, `[:sealed :bytes {:optional true :reason
:sealed-not-bytes}]` with one added leaf, and nothing would be set aside. That is one more argument
of the same function, and it is the build session's to rule.

**The store keys**, three (K1 A), declared in one place as data: each with its reason, the shape
that its value must have, the entity that its fact sits on, and whether it is a setting.

| Key | Why it is a store key | Shape of its value | Its fact sits on | Setting |
|---|---|---|---|---|
| `:class` | The gate must read which gate a layer belongs to (F1 A, F4 C) | `:class-word` | the layer (F8) | yes |
| `:permission` | The gate must read whether an act's permission stands (R 6-Oct 8; E4 A) | `:permission-id`, the maker's | the holder that the permission names (the plan's choice) | no |
| `:lock-request` | In Rama a query cannot write, so the door asks for locks by an act (C item 7) | `:lock-count`, and at most 256 | the uuid of its own act (the plan's choice) | no |

**The limits** (C rule 20; "kept, not decided"), four numbers declared beside the shapes: 256 facts
in one act, 1,024 entries of `:based-on`, 65,536 bytes in one sealed value, 256 locks in one request.
The shape language can say one of them in part, and does: the index of a lock is below 256. It cannot
say the rest: `:seq-of` and `:map-of` take no greatest count, and no shape is a run of bytes. So four
rules hold them. A thing over a limit is read by the engine before its rule refuses it; that is one
more pass over the thing, which the digest has walked once already.

**The rules.** As `act.clj`'s `rules`: a list of pairs, the name of a rule and its reason word, in
the order in which they are examined, and one map that holds the code of each. A rule relates two
places of what arrived, or holds a limit that no shape can say. The first is the maker's own, taken
from `act/rules`. Each rule is given an act that has the shape, with `:sealed` back on its facts. A
rule about a fact is kept when each fact of the act keeps it.

| | The rule | What must hold | The reason when it does not |
|---|---|---|---|
| 1 | `:permission-is-in-the-acts-layer` (the maker's) | the layer of the act's permission is the act's layer | `:permission-not-in-layer` |
| 2 | `:class-is-the-act-ids-class` | `:class` is the class slot of `:act-id` (IS X5) | `:class-not-the-act-ids-class` |
| 3 | `:facts-within-the-limit` | the act has at most 256 facts | `:too-many-facts` |
| 4 | `:based-on-within-the-limit` | `:based-on` has at most 1,024 entries | `:too-many-based-on` |
| 5 | `:store-key-fact-has-no-seal` | a fact under a store key carries no `:sealed` and no `:lock-id` (IS X10) | `:store-key-sealed` |
| 6 | `:class-fact-holds-a-class-word` | the value of a `:class` fact, when it has one, has the shape `:class-word` (IS X58) | `:class-fact-not-a-class-word` |
| 7 | `:permission-fact-holds-a-permission` | the value of a `:permission` fact, when it has one, has the shape `:permission-id` (IS X80) | `:permission-fact-not-a-permission` |
| 8 | `:permission-fact-sits-on-its-holder` | the entity of a `:permission` fact is the holder that its permission names | `:permission-fact-not-on-its-holder` |
| 9 | `:lock-request-holds-a-count` | the value of a `:lock-request` fact, when it has one, has the shape `:lock-count` (IS X90) | `:lock-request-not-a-count` |
| 10 | `:lock-request-within-the-limit` | that count is at most 256 | `:too-many-locks-requested` |
| 11 | `:lock-request-sits-on-its-act` | the entity of a `:lock-request` fact is the uuid of the act's act-id | `:lock-request-not-on-its-act` |
| 12 | `:plain-value-is-sealed` | no fact under a plain key has a plain `:v` and no `:sealed` (IS X8) | `:not-sealed` |
| 13 | `:sealed-fact-has-no-plain-value` | no fact has both `:sealed` and a plain `:v` (IS X9) | `:sealed-with-a-plain-value` |
| 14 | `:seal-is-whole` | `:sealed` and `:lock-id` come together (IS X9) | `:seal-incomplete` |
| 15 | `:sealed-is-bytes` | `:sealed` is a run of bytes that holds at least one (IS X9) | `:sealed-not-bytes` |
| 16 | `:sealed-within-the-limit` | `:sealed` holds at most 65,536 bytes | `:sealed-value-too-large` |
| 17 | `:place-named-once` | no two facts are on one entity and key (C rule 12) | `:place-named-twice` |
| 18 | `:no-fact-replaces-a-fact-of-its-act` | no fact names a fact of its own act in `:replaces` (C rule 12) | `:replaces-own-fact` |
| 19 | `:lock-named-once` | no two facts are under one lock (IS X70) | `:lock-named-twice` |

Rules 6, 7 and 9 test a value with the engine, against the shape that the store key's entry names.
A fact under a store key that has no value keeps them; the gate refuses it by its own step. A fact
under a plain key with no value and no seal is a retract (IS X11).

**What the engine is asked for.** The declaration calls the new public function of `act.clj` (see
"Where the code lives") with its own map of shapes, its nesting limits, the name of a shape, and a
thing. It uses no other way into the engine: not `act/parse`, which reads the maker's act and would
refuse the door's parts, and not `act/has-shape?`. Every refusal of a maker's part still comes out
with `act.clj`'s own word (IS X6), because the maker's parts in `:arrived-act` are `act.clj`'s own
entries, and the maker's rule is `act.clj`'s own.

**The test for a stray** (C rule 10), `stray-reason`, given the gate's class and a thing. It runs
before check (a). In order: the thing is no plain map, or holds no `:act-id` → `:no-act-id`; its
`:act-id` does not have the shape `:act-id`, `[layer class uuid]` → `:act-id-malformed`; the class
slot is not the gate's class → `:act-id-of-the-other-gate`; the layer slot is not the thing's `:layer`
→ `:act-id-of-another-layer`. The last one covers a thing with no `:layer` at all: such a thing is on
the partition that the hash of nil gives, where no reader by its act-id would look. A thing that
passes is on the task of its act-id's layer, which is where `answer-by-act-id` looks.

**The check**, `check`, given a thing that is no stray. It gives the act in its one form on arrival,
or one reason word. It never throws: as in `act/parse`, each throw becomes `:malformed`. In order,
which is `act.clj`'s order, first the shape and then the rules:

1. Each fact's `:sealed` is set aside.
2. The shape. The engine reads the thing as `:arrived-act`. The first part that is wrong, in the
   declared order, gives its reason.
3. Each `:sealed` is put back on its fact, in the one form that the engine gave.
4. The rules, in their order. The first that the act breaks gives its reason.

The one form on arrival holds every part of `:arrived-act`, as the engine builds a one form: plain
vectors and maps, each fixed-width integer a long, nil or the declared default for a part that was
left out. Each fact holds `:e`, `:k`, `:v`, `:replaces`, `:lock-id` and `:sealed`.

The words are in the table of "The decision", below, in the order in which the check can give them.

### The one text of a thing, and the digest: `softland.store.text`, `softland.store.digest`

*Given*: one digest function that the door and the gates share; an answer keeps the digest of its act
(C rules 1 and 11). *Derived* (IS C6): the digest is of the thing as it arrived, with its sealed bytes;
it is the same in every process; it is not short; it is defined for any thing that has a usable
act-id, also for one that is no act.

**The one text.** One function from anything to a string, which never throws. It writes each thing
in the form below. For a value that a fact can hold (`act/value?`) the text is EDN, and
`clojure.edn` reads it back.

| The thing | Its text |
|---|---|
| nil, true, false | `nil`, `true`, `false` |
| a string | in double quotes, escaped as Clojure prints it |
| a keyword that reads back as itself | its printed form, `:tool/class` |
| a UUID | `#uuid "…"` |
| a fixed-width integer (what `int?` takes) | its digits |
| a big integer (`BigInt`, `BigInteger`) | its digits and `N` |
| a double | `Double/toString`, with -0.0 as 0.0; `#double "NaN"` and the like for what is not finite |
| a decimal | its text without trailing zeros, and `M` |
| a run of bytes | `#bytes`, the count, the bytes (as hex in a string; as they are when fed to the hash) |
| a vector (what `vector?` takes) | `[` its parts `]` |
| any other sequence | `(` its parts `)` |
| a set | `#{` the texts of its parts, sorted `}` |
| a plain map | `{` its entries, sorted by the text of the key `}` |
| any other keyword | `#keyword` and its namespace and name as two strings |
| anything else: a record, a symbol, a ratio, a float, a Java collection, any object | `#other` and the name of its class |
| a part inside more than 64 collections | `#too-deep` |
| a thing that throws while it is read | the whole text is `#unreadable` |

Two properties, and the reason for each line:

- *The same thing has the same text everywhere.* Nothing in the text depends on the order of a hash
  map, on the identity of an object, or on the process.
- *Two things that a check can tell apart have two texts.* The checks of `act.clj` and of the
  declaration tell things apart by exactly the tests on the left: a plain map or not, `vector?`,
  `sequential?`, a set, a readable keyword, a UUID, a string, `int?`, a big integer, a finite double,
  a decimal, a run of bytes. Where no check can tell two things apart, the text may be the same: an
  `Integer` and a `Long` of one value; -0.0 and 0.0; two objects of a class that the form does not
  know, which every check refuses in the same way wherever they sit. That is on purpose. A serialiser
  may change exactly those (see X16), and a text that kept them apart would give the door and the gate
  two digests for one thing. It is safe for rule 1: two things with one text get the same decision
  from every check, so an answer read back for the one is the right answer for the other.

**The digest.** SHA-256 over the UTF-8 of the one text, fed to the hash piece by piece as the thing
is walked, so that no whole text of a large thing is built. One difference from the text as a string,
for speed: a run of bytes is fed as `#bytes`, its count, and the bytes themselves, not as hex. The
same walk feeds both, so the two cannot drift apart. 32 bytes; hex text for a caller. The plan's
choice of function; it is in the JVM, and the full width needs no argument about what "not short"
means. The door's comparison is one small function beside it: the digest that an answer holds,
and the digest of the thing the door sent, are the same or not.

Cost: linear in the size of the thing, once for each first decision and once for each stray. A repeat
is not digested: check (a) ends the event first.

### The stamp: `softland.store.stamp`

*Given*: an integer the gate assigns; strictly increasing on its task; later than each stamp that the
act carries; below 2^62; it holds the clock time and a counter (C item 6; H2 A); how the two are
packed is the builder's. The finding: plain milliseconds ran ahead under load.

**The packing (the plan's choice).** `stamp = milliseconds since 1970 × 65,536 + counter`: the low
16 bits are the counter, the bits above them the clock time.

- One clock tick, a millisecond, holds 65,536 stamps for a task. A task decides a few acts in a
  millisecond at most, so the counter does not run out under any load a task can carry, and the time
  in a stamp does not run ahead of the clock because of load. That is what the finding asks.
- The packing reaches 2^62 when the clock reaches 2^46 ms: in the year 4199.
- 20 bits of counter were weighed: a million stamps a tick, and the end in the year 2109. Not taken:
  the counter is already far more than a task can use, and 2109 is near.

**The next stamp.** One pure function of three numbers: the clock now; the task's last stamp, or
none before the task's first decision; the greatest stamp that the act carries, or none. It gives the
greatest of: the clock now with a counter of zero; the last stamp plus one; the greatest carried stamp
plus one.

- More than 65,536 decisions in one tick (IS X17): "last plus one" carries into the time bits. The
  stamp still increases; its time is one tick ahead until the clock passes it.
- The clock goes back, or the task's leader moves to a node whose clock is behind (IS X18): "last plus
  one" again.
- A restart (IS X19): the last stamp is read from `$$streaming-task` at every decision; nothing about
  it is in memory.
- A retry of work that was never visible may get another stamp (IS C7): the last stamp and the answer
  of the first run were discarded together.

**The 10-second rule** (C rule 17). One pure function: a carried stamp is too far ahead when its time
is more than 10,000 ms past the clock now. An act that carries one is refused
`:carried-stamp-too-far-ahead`, right after check (b), before any row of the record is read for it.
Its answer holds an ordinary stamp, which is not later than the stamp that made it be refused; that
is the one exception to "later than each stamp that the act carries", and it is what the rule is for.
So no act can move a task's stamps more than 10 seconds ahead of its clock, and the case "no stamp
below 2^62 is later" (IS X21) cannot be reached by a carried stamp. `:claimed-when` is a claim; the
stamp does not follow it (IS X24).

A consequence to know: a task's stamps can stand up to 10 seconds ahead of its node's clock after an
act that carries such a stamp. An honest act that carries one of those to a gate whose clock is
behind, or after the clock stepped back, can be refused by this rule. The bound is rule 17's number.

The next part needs "later than the last stamp of each task on which it writes" (C rule 18). The
function takes the last stamp as a number, so the greatest of several tasks' last stamps is the same
call.

### Locks and the seal: `softland.store.seal`

*Given*: AES-GCM; a lock for each value (H1's working answer); the gate opens each sealed value once
with its lock, and keeps nothing of the plain value; the seal binds a value to its place; the door
takes a fresh random part for each seal (C item 7; C rules 15 and 16).

- **The way of sealing is a word in the lock's row** (ruled by the build session, 8 October), and
  the functions of this namespace take a lock as its row gives it: its bytes and its way. They know
  one way, `:aes-256-gcm`, and the next three points say what that way is. A sealed value carries no
  mark of its way. A lock whose way the code does not know seals nothing and opens nothing; the gate
  then refuses the act with `:lock-way-not-known`. Every lock of this build is made for the one way,
  so no act of this build can meet that word. It is there so that a later way is new locks with a new
  word, and no stored value is touched.
- **A lock** of this way is 32 random bytes, a key for AES-256, from the JVM's `SecureRandom`, made in
  the gate's event when a request is accepted, and written with the way's word. The plan's choice of
  size: the sealed bytes stay in the depot for ever (C rule 4), and the only forget is the destruction
  of the lock; a key for that should be the longer one. It costs 16 bytes more for each value than
  AES-128, and counts against F9's third limit.
- **Sealing**: AES-GCM under the lock, with a random part of 12 bytes and a tag of 16. The sealed bytes
  are the random part, then the cipher's output with its tag: 28 bytes more than the plain bytes.
- **The place is bound in** as the cipher's associated data: the UTF-8 of the one text of
  `[layer entity key]`. Sealed bytes that are copied to another entity, key or layer do not open
  (C rule 16).
- **Opening** gives the plain bytes, or nothing when the tag does not hold or the lock's way is not
  known. It never throws.
- **The gate does not read what a lock opens.** Rule 16 has the gate check "that the lock opens it"
  and keep nothing. The plan takes that as written: the gate checks the tag and drops the plain bytes.
  It does not check that they are a value of the form. So the one part of the maker's act that no gate
  holds to `act.clj` is the `:v` of a sealed fact: a sender with a real lock can record bytes that
  open and are no value. If the build session wants `act/value?` held there, this step is its place
  ("the grammar check has its place", rule 16): the plain bytes would be read as the one text of a
  value, and one reason word added. That needs the plain form to be fixed here; in this build it is
  the door's. The proof's hand seals the UTF-8 of the one text of the value in its normal form.

The door plays no part in this namespace beyond calling it: the proof's hand uses the same sealing
function that the gate's opening undoes.

### The first act of a layer: `softland.store.first-act`

*Given* (C item 9; K2; F4 C; C rule 14): a layer is made by its first act; the form is the builder's.

- `class-at-making`: the class that a layer starts with: `:microbatch` for `:base`, `:streaming` for
  any other layer. The one place where F4 C is code.
- `making-act?`, given the gate's class and an act in its one form on arrival. True when all of these
  hold:
  1. the gate's class is the class that the act's layer starts with;
  2. the act's `:permission` is `[holder layer layer]`: three slots, nothing above it, granted in the
     layer itself (C rule 14);
  3. `:who` is the holder (a group's layer, made by the person who creates it), or `:system` (a
     personal layer, made for its person); for `:base` the holder is `:system`;
  4. the act has exactly two facts, in either order: `{:e layer :k :class :v the class it starts
     with}` and `{:e holder :k :permission :v the act's own permission}`.

The form says nothing about `:replaces`; a making act that names something to replace is stale at
check (e), as IS X49 reasons. The gate checks nothing more about who the holder is (C rule 14): any
sender can make any layer that is not made, for any holder, until sign-in is built. The module's
README says so.

### The decision: `softland.store.decision`

The checks after (b), as functions of an act in its one form and of rows. None reads a PState, a
clock or a random source. Three stages, in the order of the walk: *standing* (given the row at the
permission's place, the row at the class's place when it was read, and whether the other gate's record
holds the layer); *places* (given the rows of the places read); *seals* (given the lock rows read; it
opens the values). Each gives nothing, or the word of the first check that fails. Two more functions
say which rows a stage needs, and one gives the rows that an accept writes, as data. The gate's
dataflow is the reads and the writes between them.

**The order of the checks is the order of the reasons** (IS C4). The contract places (a) to (g). The
plan places the rest and says where: the 10-second rule after (b), because it needs the act's parts
and nothing of the record; what a store key does not take, after (g), where the contract's own text
has it ("Not decided, so refused in this build"); the seal last, because it is the most costly and
needs the lock's bytes.

| Step | The check | Words, in the order tested | Source |
|---|---|---|---|
| before (a) | a stray gets no answer | `:no-act-id`, `:act-id-malformed`, `:act-id-of-the-other-gate`, `:act-id-of-another-layer` (kept in `:strays`, never in an answer) | C rule 10 |
| (a) | the act-id has an answer: end | none; nothing is written | F7 A; C rules 1, 11 |
| (b), the shape | the thing has the shape `:arrived-act`, part by part in the declared order | `:unknown-part`; `:class-not-the-act-ids-class` (a `:class` that is absent or no class word); then `act.clj`'s words for the maker's parts: `:unknown-version`, `:no-permission`, `:no-session`, `:empty-act`, and `:malformed` where a part names no word; `:lock-id-malformed` for a fact's lock id | C item 2; IS X5 to X7, X12 |
| (b), the rules: the act as a whole | rules 1 to 4 of the declaration | `:permission-not-in-layer`; `:class-not-the-act-ids-class`; `:too-many-facts`, `:too-many-based-on` | C item 2; IS X5; C rule 20 |
| (b), the rules: a fact under a store key | rules 5 to 11 | `:store-key-sealed`; `:class-fact-not-a-class-word`; `:permission-fact-not-a-permission`, `:permission-fact-not-on-its-holder`; `:lock-request-not-a-count`, `:too-many-locks-requested`, `:lock-request-not-on-its-act` | C item 2; IS X10, X58, X80, X90; C rules 13, 20 |
| (b), the rules: a fact under a plain key | rules 12 to 16 | `:not-sealed`; `:sealed-with-a-plain-value`; `:seal-incomplete`; `:sealed-not-bytes`; `:sealed-value-too-large` | C item 7; IS X8, X9; C rule 20 |
| (b), the rules: between facts | rules 17 to 19 | `:place-named-twice`, `:replaces-own-fact`, `:lock-named-twice` | C rule 12; IS X70 |
| after (b) | the 10-second rule | `:carried-stamp-too-far-ahead` | C rule 17 |
| (c) | the layer's class is this gate's | `:layer-belongs-to-the-other-gate` | C item 3(c); C rule 7 |
| item 9 | the layer is not made: a right making act, or refused | `:layer-not-made` | C item 9 |
| (d) | the act's permission stands, for this `:who` | `:who-not-the-permissions-holder` (the holder slot of the act's permission is not `:who`), then `:permission-does-not-stand` | C item 3(d); E4 A |
| (e) | each `:replaces` is the current fact of its place | `:replaced-fact-not-current` | C item 3(e); C rules 2, 3 |
| (f) | no setting key on an entity other than the layer | `:setting-not-on-the-layer` | F8 C |
| (g) | a permission fact writes a permission beneath the act's own, in the act's layer | `:permission-fact-for-another-layer`, `:permission-fact-granted-in-another-layer`, `:permission-fact-not-beneath-the-acts-own` | C item 3(g); E2 A; C rule 14 |
| after (g) | what a store key does not take in this build | `:ending-a-permission-not-decided`; `:store-key-retract-not-built`; `:changing-a-class-not-decided`; `:store-key-takes-one-write` | C item 3, last sentence; C rule 13 |
| last | the seal | `:lock-not-made-for-this-layer`, `:lock-already-seals-a-value`, `:lock-way-not-known`, `:lock-does-not-open-the-value` | C item 7; C rule 16; the ruling on the way |

How each record check is made:

- **(c), for a layer that is made here, costs nothing of its own.** A gate's record holds rows only
  for layers of its class: a row is written only by an accept; an accept happens in a layer that is
  made here, or makes it; a making act is right only for the gate of the class that the layer starts
  with; and no act of this build changes a class. So when the act's permission is found in this
  gate's record, the layer is this gate's. When it is not found, the gate reads the class fact at
  `"<layer> <layer> :class"`. There: the layer is made here, and (d) refuses. Not there: the gate asks
  whether the other gate's record holds the layer. In this part nothing can make that true, and the
  step is one function that gives "no"; the next part gives it its read, and then the word is
  `:layer-belongs-to-the-other-gate` (IS X31, which this part leaves open). Otherwise the layer is
  not made, and item 9 decides.
  Rama itself keeps the invariant in the future: only the owning topology can write a PState, so a
  layer whose class changes must move to the other gate's PStates before that gate can write a fact of
  it. Re-class is not built; this says only that the room is there.
- **(d) is 1 read.** The place is `"<layer> <who> :permission"`. The row's `:value` is compared, as
  bytes, with the one text of the act's `:permission`. Equal: the record holds the permission, and its
  holder is the act's `:who`, because a permission fact sits on its holder. "Each permission above it
  in its chain stands" then holds without a read: a permission row is written only by a right making
  act (nothing above it) or by a give that passed (d) and (g), whose own permission stood and is
  exactly the one above; and nothing in this build ends a permission. A permission id holds its whole
  chain (`act.clj`, `:permission-id`), so equal bytes are an equal chain. **The plan's choice**, said
  plainly: the gate does not read the permissions above. Reading them would cost up to 3 more reads on
  every act of every kind, to find what this build cannot make false. The room for a permission that
  has ended is the place itself: an end would be a later fact on it, and the day "who may end one" is
  decided, that decision also says whether the gate then reads each place above or the end is written
  beneath.
- **(e) is 1 read for each fact that names something to replace**, of that fact's place. The fact
  named must be this layer's, of the class streaming, and its uuid and index must be the row's
  `:by-act` and `:by-index`. An empty place, another fact, a fact of another layer: stale (IS X62). A
  fact that names nothing reads nothing and is never stale (C rule 2); it becomes the current fact by
  a write that needs no read. A retract is written as the row of its place, with no value, so a later
  fact can replace it (C rule 3).
- **(g) and the store-key step read the place of each `:permission` fact**: 1 read each, shared with
  (e) when that fact also names something to replace. A place that holds a row is
  `:store-key-takes-one-write`, whether it holds this same permission ("is not given again") or
  another (C rule 13). A `:class` fact in a layer that is made needs no read: the layer's class place
  holds a row because the layer is made; the class word that is the layer's own is
  `:store-key-takes-one-write`, the other one is `:changing-a-class-not-decided`. A `:lock-request`
  fact needs no read: its place is new by its form.
- **The seal step** is under "The path of a lock".

## State primitive selection
<!-- For every piece of state in the design, name the storage class and justify -->

Every piece of state is a PState. There is no TaskGlobal and no state in the memory of a process.

- `$$streaming-answers` (PState): the source of truth for a decision (F2 A, F7 A). One row for each
  decided act: O(1) for an event. Durable, never deleted (IS C2, C8).
- `$$streaming-facts` (PState): the source of truth for the current facts, the class and the
  permissions. One row for each fact of an accepted act: at most 256 for an event (C rule 20).
  Durable.
- `$$streaming-locks` (PState): the only home of a lock's bytes, which are in no depot (C rule 5); a
  lock that is lost is a value lost (IS O6). At most 256 new rows for an event (a request), and at
  most 256 rows written again (the locks that an act uses). Durable.
- `$$streaming-task` (PState): the last stamp, one value written at each decision; and one set element
  for each different stray. Durable: a stamp must increase through a restart (IS C7).
- Not a TaskGlobal: the last stamp. It could be held in memory and found again from the PState after
  a restart; that is a cache, and the project's rule sends a cache to its own examination first. See
  `$$streaming-task` under "PState Design".
- Not state: the random source for a lock's bytes. One `SecureRandom` for each worker process. It
  holds nothing of the record, and losing it loses nothing.
- External: none.

Every write of an event is bounded by the limits of rule 20, which the declaration holds before any
row is written. A stray writes one set element at most, about 70 bytes.

## Resource usage analysis

All figures here are estimates from the sizes of the parts. Nothing was measured: this stage ran no
cluster, and the in-process cluster's timings "are not a proxy for production latency or throughput"
(skill testing.md).

### Disk usage (PStates)

Sizes for a layer whose id is a short keyword; a UUID as layer id adds 30 bytes to each key. Field
names and type marks are counted at about 10 bytes a field.

| Where | One entry | Grows by | Example: a task that takes 100 acts a second, each with one sealed value of 100 plain bytes |
|---|---|---|---|
| `*streaming-depot` | the thing as sent: about 450 bytes of act around its facts, plus the sealed bytes (plain + 28) | one for each thing sent, for ever (C rule 4) | about 580 bytes × 8.64 million a day ≈ 5 GB a day |
| `$$streaming-answers` | key about 43; row about 70 accepted (32 digest, 8 stamp, the rest names), about 100 refused | one for each decided act, for ever | about 115 bytes × 8.64 million ≈ 1 GB a day |
| `$$streaming-facts` | key 25 to 110; row about 112 plus the value's bytes | one for each place that is new; a fact that replaces another writes over its row | half the acts on new places: about 280 bytes × 4.32 million ≈ 1.2 GB a day |
| `$$streaming-locks` | key about 47; row about 61 before it seals a fact, about 104 after (32 of lock, about 18 for the word of its way with its field name, 17 of the fact's uuid, the rest names and marks) | one for each lock asked for, for ever in this build | about 113 to 151 bytes × 8.64 million ≈ 1.0 to 1.3 GB a day |
| `$$streaming-task` | 8 bytes of stamp; about 70 for each different stray | strays only | nothing to speak of |

The depot is the largest by far, and it is never trimmed by decision. The answers and the locks grow
with the acts and are never deleted in this build (IS C2; IS O6).

**The third limit of F9** (C rule 21): "the locks take no more than 2 times the bytes of the values
(measured: 169 bytes of lock for each value)". This design: one lock row for each value, about 151
bytes as written (47 of key, 104 of row), about 113 where RocksDB pays the shared front of
neighbouring keys once in a block (*not verified*). Of those, about 18 are the word of the lock's way
of sealing and its field name, which the build session's ruling of 8 October put in each lock's row;
before it the figures were 133 and 95, and the aim was 100. **The plan aims at 120 bytes of lock or
fewer for each value on disk.** Against "2 times": that holds when the sealed values average 60 bytes
or more. For shorter values it does not hold with a lock for each value at 32 bytes of lock; what
moves it then is the grain, which is the open row H1 (one lock for an act, or for a session and
layer, divides the lock's bytes among the values under it), not the row.

### Memory usage (TaskGlobals)

None. The design holds no state in memory. During one event the gate holds the thing, the rows it
read, and, for one sealed value at a time, the plain bytes that the lock opened; they are dropped
before the next value is opened (C rule 16, "keeps nothing of the plain value").

### The length of one event

- *A common act* (a few hundred bytes, one sealed value): the digest of a small text, one pass of
  the declaration's check, one AES-GCM opening, 4 reads, 4 row writes. About 0.05 to 0.15 ms of computing beside
  the reads.
- *The largest act that the limits allow*: 256 facts, each replacing a fact, each with a sealed value
  of 65,536 bytes, 16.8 MB in all. 256 place reads (77 to 128 ms from a cold disk), 1 to 256 lock
  range reads, the digest of 16.8 MB (40 to 110 ms), the opening of 16.8 MB (10 to 20 ms), 514 row
  writes. Some 0.15 to 0.5 s in which the task serves nothing else. The event cannot yield (see
  "Topologies"). The limits are the lever, and they are "kept, not decided" (C rule 20); whether Rama
  itself takes a depot record of that size was not found in its docs.
- *A request for 256 locks*: 8 KB of random bytes and 259 row writes; under 1 ms.

### Minimization

- **Answers.** The digest could be 16 bytes instead of 32 with no practical loss (see "PState
  Design"): 16 bytes less for every act for ever. Kept whole in this build. The row keeps no
  `:accepted` word.
- **Facts.** The row names its lock and its fact by a uuid and an index each, 34 bytes of ids. The
  layer and the class are in neither: the key and the PState say them. Nothing of the act is copied
  into the row.
- **Locks.** The row could be one run of bytes (the lock, then the fact it seals), about 33 bytes less
  for each lock. Not taken: a row of named fields is what Rama's schema can check, and the skill asks
  for a fixed-keys schema for a record-like row. If the build's measure of lock bytes misses the aim,
  this is the next step, with AES-128 after it (16 bytes). The word of the way is the same in every
  row of this build, about 18 bytes each. A row with no word, read as the first way, would save
  them; the ruling asks for a word in each row, so it is written. A block compressor would remove a
  word that repeats in every row; whether Rama's RocksDB compresses its blocks was not found in its
  docs.
- **No data is held twice**, with one exception that is a link, not a copy: a fact's row names its
  lock, and the lock's row names its fact. Both directions are read: the first by a forget (not
  built) and by whoever opens a value; the second by the seal check, and by a rebuild that must see
  "this lock seals this same fact" (C rule 19).
- **Keys.** A UUID in a key is 36 characters of text where 16 bytes would do. Kept as text for the
  documented order of string keys. Each UUID in a key costs 20 bytes before RocksDB shares fronts.

## The checks of one act: what each reads, on which task, and what it costs

Every read below is on the layer's task, in the one event of the act. "0" means the check uses only
the thing itself, or rows that an earlier check already read.

| Check (C item 3; C item 9) | State it reads | Reads |
|---|---|---|
| A stray, before (a) (C rule 10) | nothing. (A stray then writes: the `:strays` field and one set element, 2 reads with the set's size tracking.) | 0 |
| (a) the record holds an answer under this act-id | `$$streaming-answers`, the key of the act-id | 1 |
| (b) the door's declaration, with `act.clj`'s check | nothing | 0 |
| the 10-second rule (C rule 17) | nothing: the wall clock | 0 |
| (c) the layer's class is this gate's | when (d)'s row is the act's permission: nothing more. When it is not: `$$streaming-facts`, the layer's class place. In the next part, when that is empty too: the other gate's record | 0, or 1 (the next part: or 2) |
| (d) the act's permission stands | `$$streaming-facts`, the place of `:who`'s permission | 1 |
| The first act of a layer (C item 9) | the two rows of (d) and (c), both empty. The form of the act is tested on the act alone | 0 more (2 with (c) and (d)) |
| (e) each `:replaces` is the current fact of its place | `$$streaming-facts`, the place of each fact that names something to replace | 1 for each such fact; 0 for a fact that names nothing |
| (f) no setting key on an entity other than the layer | nothing | 0 |
| (g) a permission fact writes a permission beneath the act's own | the check itself: nothing. With it, rule 13's "a place that holds one": `$$streaming-facts`, the place of each fact under `:permission` | 1 for each fact under `:permission` |
| What a store key does not take (C item 3, end; C rule 13) | rows already read | 0 |
| The seal (C item 7; C rule 16) | `$$streaming-locks`, one range for each request whose locks the act names | 1 for each request, and one iteration for each index in the range |
| The stamp (C item 6) | `$$streaming-task`, `:last-stamp` | 1, for each first decision, accepted or refused |

**One accepted act with one sealed value that replaces nothing: 4 reads.**

| | Read | Row |
|---|---|---|
| 1 | (a) | the answer: not there |
| 2 | (d), and with it (c) | the permission of `:who`: there, and it is the act's |
| 3 | the seal | the lock: there, sealing nothing; its bytes open the value at its place |
| 4 | the stamp | the task's last stamp |

Then 4 row writes, none of which reads: the fact, the lock (its bytes as read, and the fact it now
seals), the answer, the last stamp. If the value replaces a fact: 5 reads. One event, one task, no
hop.

Other acts, for the same accounting: a repeat, or a different act under a used act-id: 1 read and no
write. A request for n locks: 3 reads (answer, permission, last stamp) and n + 3 writes. A making act:
4 reads (answer, permission, class, last stamp) and 4 writes. A give: 4 reads (answer, the giver's
permission, the place of the new one, last stamp) and 3 writes. An act refused by the declaration: 2
reads (answer, last stamp) and 2 writes. An act whose permission does not stand: 4 reads and 2 writes.

**Set against the second limit of F9** (C rule 21): "at least 100 acts each second for one person,
with the slowest 1 in 100 at 20 ms (measured: 46 ms)". The plan takes the 20 ms from the door's append
to the answer in the door's hand, which is the stricter reading.

| On the path of one act | What it costs | Whose |
|---|---|---|
| The door's append reaches the task's leader | 1 network hop | Rama |
| The depot append is replicated before the gate sees it | 1 replication round | Rama |
| The gate's event | 4 reads: 1.2 to 2.0 ms at the cold cost, far less when the rows are in memory; about 0.1 ms of computing; 4 row writes into buffers | this design |
| The batch's PState writes are replicated and made visible | 1 replication round | Rama |
| The ack returns to the door | 1 network hop | Rama |
| The door reads the answer by act-id | 1 roundtrip straight to the task, 1 read: 0.3 to 0.5 ms at the cold cost | this design |

What this design adds to Rama's own two replication rounds and two roundtrips is 5 reads, 2.5 ms at
the worst cold cost, with no hop between tasks and no wait on another task. At 100 acts a second for
one person an act mostly finds its task idle and is in a batch of its own (docs 11:138). **The plan
aims at the 20 ms.** Whether the slowest 1 in 100 meets it depends on Rama's replication on real
nodes, which this stage cannot say and the in-process cluster cannot show.

## The path of a lock, from "asked for ahead" to "sitting beside its value"

*Given*: C item 7. The path is for a streaming layer; a lock never leaves the layer's task.

1. **Asked for ahead.** The door sends an act with one fact `{:e u :k :lock-request :v n}`, where `u`
   is the uuid of that act's own act-id and `n` is 1 to 256. It is an act like any other: check (a),
   the declaration, the layer, the permission (IS O6). The ids of the locks that it asks for are
   known before it is sent: `[act-id 0]` to `[act-id n-1]`.
2. **Made.** When the gate accepts the request, the same event writes n rows to `$$streaming-locks`,
   `"<layer> <u> 000"` onward, each with 32 fresh random bytes, the word of its way of sealing, and
   no fact; the request's own fact as the row of its place; the answer; the last stamp. All of them
   become visible together (IS C3). A refused request writes its answer and no lock (IS X90 to X92).
3. **Read.** The door reads the request's answer by act-id, then its locks, the bytes and the way of
   each, with one call of `locks-of-request`. Before the request's batch is visible the call gives an
   empty map (IS X97). This is the one planned way that a lock's bytes leave the module.
4. **Used by the door.** For each value, the door takes one of its locks, seals the plain bytes to
   the fact's place in the lock's way, with a fresh random part, and sends the fact as
   `{:e :k :sealed :lock-id}`.
5. **Checked by the gate**, last of its checks, for each sealed fact in the act's order:
   - the lock id's layer is the act's layer and its class is this gate's; else
     `:lock-not-made-for-this-layer`, with no read;
   - the row is there (one range read for each request named, covering the indexes that the act
     uses); else `:lock-not-made-for-this-layer` (IS X68);
   - the row names no fact; else `:lock-already-seals-a-value` (IS X69);
   - the row's way is one that the code knows; else `:lock-way-not-known`. No lock of this build can
     fail this; it is there for the day the way changes;
   - the row's bytes open the sealed bytes at this fact's place, in the row's way; else
     `:lock-does-not-open-the-value` (IS X72). The plain bytes are dropped at once.
6. **Sitting beside its value.** On accept, the same event writes the fact's row, which names the
   lock (`:lock-act`, `:lock-index`), and writes the lock's row again with the bytes and the way as
   read and the fact it now seals (`:seals-act`, `:seals-index`). From then on the value's row and the lock's row
   name each other, on one task, and no other value can come under that lock.
7. **Later.** A fact that replaces the value writes over the value's row. The lock's row stays and
   still names the fact that it sealed (IS E4, "seals a value that is no longer current"). A forget
   would delete the lock's row; that is one local delete of one key, and it is not built.

A refused act leaves every lock that it named as it was: the refusal writes the answer and the last
stamp, and no lock row (C rule 15). "A lock seals one value" counts accepted values.

**Rule 5 on a retry of the stream event** (Rama can process a record more than once).

- *The request is processed again after its first run was committed.* Check (a) finds the request's
  answer and ends the event. No lock row is written. The bytes that a door may have read are the bytes
  that stay.
- *The request is processed again after its first run was discarded.* The lock rows of the first run
  were discarded with its answer, in one batch, and no reader ever saw them (docs 11:170). The retry
  makes new bytes under the same ids. That is the case that IS X94 allows.
- *An act that uses a lock is processed again after it was committed.* Check (a) ends the event before
  the seal step. The lock is not "used twice", and nothing is made (IS X69, IS C5).
- There is no state between these: the answer row and the lock rows commit in one batch on one task,
  or not at all.
- *Never on a depot.* The request holds a count. The event appends to no depot.
- *A replay* (a rebuild from the depots) is not built (C rule 19). What it would need is in "What the
  plan found out".

**Rule 16 on the layer's task.** The gate opens each sealed value once in the decision of its act,
with the row it read on the layer's task, in the event that decides the act. The lock's bytes and the
sealed bytes meet only there. No hop carries a lock's bytes or a plain value, because the event has
no hop. A committed act is never opened again by the gate: its retry ends at check (a). A first run
that was discarded opened the value too and kept nothing, as every run does.

## The builder's rules, one line each

How the design meets each of the contract's rules 1 to 21. Three are the next part's; for those the
line says what this part leaves for it.

1. Each answer row keeps the SHA-256 of its thing's one text. A second, different act under a used
   act-id ends at check (a) and writes nothing; its sender reads an answer whose digest is not the
   digest of what it sent, and so does not take it as its own.
2. A fact that names nothing to replace reads no row and is written as the row of its place. No check
   can call it stale.
3. A retract is written as the row of its place, with no value, with its fact's id and the stamp. A
   later fact that names it passes (e).
4. The module sets no trimming option on `*streaming-depot`; Rama's default keeps every record.
5. A lock's bytes are made in the gate's event and written only to `$$streaming-locks`. A request
   holds a count, and the event appends to no depot. A retry makes no lock again for a value that is
   sealed: "The path of a lock", rule 5.
6. *The next part's.* This part does not shut it out: the checks are functions of an act and of rows,
   with no order and no task built in; every key names its layer and its own id, so a row can live on
   any task; no shape needs all acts of a layer on one task.
7. A streaming layer's class is a fact at a place found from the layer id, which any topology can
   read. The streaming gate's check (c) has the step that asks the other gate's record. A streaming
   act's answer is found from its act-id alone (R1). The microbatch half is the next part's; each of
   the three reads has its branch.
8. Every word that this design makes is under "Words a session made, Sid's to change", each with what
   it is. None is "offer", "by-layer", "lease", or "name" for an act's id. This stage read no code of
   the rig.
9. *The next part's.* The like question for this gate is IS X29; the answer is under "What the plan
   found out".
10. `stray-reason` runs before check (a). A stray gets no answer; its digest is added to the set of its
    reason word in `$$streaming-task`, on the task where it arrived: one set element, once. The gate
    writes an answer only under an act-id of its class whose layer is the thing's layer.
11. The answer row keeps the digest and `answer-by-act-id` gives it. `softland.store.digest` is the one
    function, called by the gate and by the door's hand. The gate writes nothing at check (a).
12. Two rules of the declaration, each a name and a reason word as data and its test in code, since
    each relates two places: `:place-named-once`, with the word `:place-named-twice`, and
    `:no-fact-replaces-a-fact-of-its-act`, with the word `:replaces-own-fact`.
13. The store-key step refuses a second write on a store key's place, each retract under a store key,
    and a `:class` fact after the making, with four words that say which and that it is not decided
    or not built. A request holds a count of 1 to 256. The rule's last sentence, answered: a
    permission fact sits on its holder.
14. `granted-in` must be the act's layer: in a first permission it is part of the form of a making
    act; in a permission given, `:permission-fact-granted-in-another-layer`. The gate checks nothing
    more about a holder or about `:who`; the module's README says that any sender can make any layer
    that is not made, for any holder, until sign-in is built.
15. A refusal writes the answer row and the task's last stamp, and no lock row. A lock's row names a
    fact only after an accept. The sealing function takes 12 fresh random bytes at each call.
16. The seal step opens each sealed value with its lock's bytes, in the way that the lock's row names,
    and drops the plain bytes. The place is the cipher's associated data. `locks-of-request` gives a
    lock's bytes and its way to any caller.
17. `:carried-stamp-too-far-ahead`, tested right after check (b) against one reading of the wall
    clock, before any stamp is made.
18. *The next part's.* The stamp function takes the last stamp as an argument, so "the greatest last
    stamp of the tasks it writes on" is the same call.
19. Nothing is built for a rebuild. What this stage found is under "What the plan found out".
20. The four limits are numbers declared in the declaration. The shape language cannot say them (no
    greatest count for a sequence or a map, no shape for a run of bytes), so four rules hold them,
    with the words `:too-many-facts`, `:too-many-based-on`, `:sealed-value-too-large` and
    `:too-many-locks-requested`.
21. The three aims are under "What the plan found out": 1,000 acts a second on one task; 20 ms from
    append to answer; 120 bytes of lock or fewer for each value.

## What the plan found out

The points that `DECOMPOSITION.json` gives this part to find out or to say (point 12), and rule 19.

### IS X16: which values Rama carries unchanged

*Found in the docs, not run.* Rama has "built-in support for using basic types (int, long, float,
double, string, etc.), commonly used types from java.util, Clojure data structures, and types defined
with defrecord", and "under the hood, Rama uses Nippy for serialization" (docs 30). The docs that this
stage read do not speak of byte arrays, of what becomes of -0.0 or of an `Integer`, or of whether a
list comes back as a list.

*What the design does about it.*

- The digest is taken of the one text, which is blind to the differences that a serialiser may make
  and that no check can see (the width of a small integer, -0.0, trailing zeros of a decimal). So
  those cannot make the door's digest and the gate's differ.
- The design does rest on these arriving as they were sent: a vector as a vector and a plain map as a
  plain map (`act.clj` tests both); a keyword with its namespace; a UUID; a big integer as a big
  integer; a run of bytes as the same bytes (every sealed value, and three row fields); and the order
  of a sequence, which gives a fact its index (IS X13).
- **The proof shows it before anything rests on it.** It appends one thing that holds each of these
  kinds, reads the stored record back from the depot's log, and shows that the digest of what came
  back is the digest that the door took before the append. Reading the stored record is what makes
  this a check: in the in-process cluster "serialization only crosses workers" (skill testing.md), so
  a thing can reach the gate without having been serialised. The proof also launches more than one
  worker. If a kind does not come back as sent, the build stops and reports it; the design is not
  bent to hide it.

### IS X29: is an act of a layer ever decided after a later act of that layer?

"The plan shows this from Rama's guarantees; it does not assume it" (IS X29). What Rama's docs
guarantee, and what they do not:

- *Without a failure, the depot's order holds.* The acts of a layer are on one depot partition; "after
  a depot partition appends a new record, it pushes that record to all subscribed stream topologies"
  (docs 11:115); the gate's event runs on that one task, start to end, and a task runs one event at a
  time. An act sees the record as the act before it left it, also inside one batch: the owning
  topology reads its own uncommitted writes (skill pstate-schema.md, "Read visibility").
- *When an event fails, its whole batch fails, and its writes are discarded* (docs 11:168, 11:170).
  Nothing of an act is half-written.
- *Among records that failed, the order of the retries.* In the default mode "Rama will try its best
  to retry records in the order in which they were appended, but … can't guarantee this" (docs
  11:158). In the mode "all after", "a failed record will cause all records after it on the same
  depot partition to also be retried (even if they already succeeded)" (docs 11:162). So this gate
  uses "all after": what failed together is decided again from the first failed record onward.
- *What no mode gives.* The words "even if they already succeeded" say that a later record can have
  been processed, and committed, before the earlier failed one is retried. Then the later act was
  decided first. Its retry is answered at check (a) and changes nothing.

**So the sentence of IS X29 holds as long as no streaming batch fails, and can fail to hold in the
window between a failed batch and its retry.** The plan cannot close the window: the gate would have
to know each record's position in its partition, and the skill's references name no operation that
gives it to a stream topology.

How wide the window is, *derived from the docs and not verified*. This gate's event has no hop, so
two kinds of failure differ. (1) A batch's replication does not finish. A task's changes are applied
in the order of its replication log, up to an "apply offset" (docs 21:45), so a later batch cannot
become visible before an earlier one, and the order holds. (2) Something throws while a batch runs.
That batch is discarded with no entry in the log, and a later batch can commit before the retry. The
gate's own code never throws on a thing (IS C1), so (2) needs a fault of the machine or of Rama inside
the batch, or a fault of this module that the proof did not find.

What this does not touch: one answer for an act-id, whole or nothing, and each act decided against the
record as the act before it, in the order of the decisions, left it. What it does touch is F2 A's
reason, "an act proposed at a version is refused the same way every time" (R 7-Oct 2). That is true
for one order. After such a window the order of the decisions is not the depot's order for the acts
involved, and a rebuild that replays the depot in its order can give two acts that meet (two that
replace one fact) the opposite answers. The order in which the gate did decide is in the record: on a
task, it is the order of the answers' stamps. This goes to Sid in the receipt, with rule 19.

### IS C7, last point: how many stamps a tick holds, and the year

65,536 stamps in one millisecond for a task. The packing reaches 2^62 in the year 4199. The working is
under "The stamp".

### C rule 19: what a rebuild of the record from the depots would meet

Nothing is built or tested for a rebuild. Found for this gate:

- *Locks.* A lock's bytes are in no depot. A rebuild must carry `$$streaming-locks` over as it is. A
  request whose locks are there makes none again. A value whose lock already names this same fact
  (`:seals-act`, `:seals-index`) is the same act, not a second use; the row holds what that test
  needs.
- *Stamps.* They would be new, because a stamp holds the gate's clock at the decision (H2 A). An old
  act's `:based-on` then names stamps that the rebuilt record does not hold. No check of this build
  reads `:based-on` against the record, and old stamps are in the past, so the 10-second rule refuses
  none of them.
- *The order.* IS X29, above: the depot's order is the order of the first decisions except across a
  failed batch.
- *Strays.* The same set comes out again: it is a set of digests.
- Rule 9's own question, two microbatch acts on different entities, is the next part's.

### C rule 21: the three limits of F9, and what the plan aims at

The rig's measures are quoted from the contract. This stage read nothing of the rig, so it cannot say
how each was measured.

1. **At least 1,000 acts each second on one task, at the rate of an agent** (measured: all accepted,
   the slowest 1 in 100 at 254 ms). *Aim: 1,000 acts a second on one task, every act decided, the
   slowest 1 in 100 well under 254 ms.* The gate's time for a common act is 4 reads and about 0.1 ms.
   At the skill's cold cost, 0.3 to 0.5 ms a read, that is 1.3 to 2.1 ms an act: 480 to 770 acts a
   second. **By that arithmetic the aim is not met.** It is met only when the reads are answered from
   memory, and for an agent's run in one layer there is a reason for each of the four: the answer's
   key was never written; the permission row is the same row at every act; the lock was written by a
   request a short time before; the last stamp was written by the act before. That is an argument,
   not a measure. Three of the four reads cannot go: F7 A asks for the first, check (d) for the
   second, rule 16 for the third. The fourth, the last stamp, can go only by keeping it in memory,
   which the project's caching rule holds back.
2. **At least 100 acts each second for one person, the slowest 1 in 100 at 20 ms** (measured: 46 ms).
   *Aim: 20 ms from the door's append to the answer in the door's hand.* The count and the path are
   under "The checks of one act".
3. **The locks take no more than 2 times the bytes of the values** (measured: 169 bytes of lock for
   each value). *Aim: 120 bytes of lock or fewer for each value.* The working, and the size of value
   below which "2 times" cannot hold with a lock for each value, are under "Resource usage analysis".

## Where the contract and the README differ

The README wins. The implied spec listed seven differences; the contract was amended on 8 October, and
this stage checked each of the seven against the contract as it stands: each is corrected. This stage
found no place where the two still differ in a way that changes this part. Three things to know:

1. **The classpath.** The contract leaves open whether Rama goes into `:store` (C "Classpath"). The
   README says of `:store` that it holds "Clojure alone as `:replace-deps`", and
   `test-store/README.md` that the act's proof "needs local Clojure and nothing else". Those sentences
   stay true only with the second alias. The plan follows the README: `:store-module`.
2. **The name of the module.** The contract says "one Rama module, `softland.store`" in one place and
   "the module `softland.store.module`" in another. The README does not name it. The plan uses the
   namespace `softland.store.module` and calls the module `Store`.
3. **F1 A beside F4 C.** "The members of a layer set its class, as a fact on the layer" (R 7-Oct 3),
   and "every other layer starts streaming" (R 7-Oct 4). Both files hold both. In this design the
   class is a fact on the layer, written by the layer's first act with the class that F4 C gives, and
   every later `:class` fact is refused. So in this build no member sets a class after the making. The
   implied spec named this tension; the design does not resolve it and must not.

In the contract and not in the README, used here as the contract gives them: the keywords `:system`
and `:base`, the first permission of the base, and the word `:not-sealed`.

## Words a session made, Sid's to change

Every word that this design makes. Each says what the thing is. None comes from the rig's code, which
this stage did not read. The module's README carries this list (C rule 8).

Not made here, carried from the contract and the README: `:act-id`, `:class`, `:sealed`, `:lock-id`,
`:permission`, `:lock-request`, `:not-sealed`, `:system`, `:base`, `:streaming`, `:microbatch`,
`:accepted`, `:refused`; "stray" (C rule 10), "place" (C rule 12), "holder", "making act" (C item 9).
"Class" and "act-id" are Sid's to change (R 7-Oct 10); each word below that holds one of them follows
the word that he gives.

**Names in Rama.**

| Word | What it is |
|---|---|
| `Store` | The Rama module, in the namespace `softland.store.module` |
| `:store-module` | The alias of the root `deps.edn` that holds the store's paths, Clojure and Rama |
| `*streaming-depot` | The streaming depot: every thing sent for a streaming layer |
| `streaming-gate` | The stream topology that decides each thing of the streaming depot |
| `$$streaming-answers` | The answer under each act-id of a streaming layer |
| `$$streaming-facts` | The current fact of each place of each streaming layer |
| `$$streaming-locks` | Each lock of each streaming layer, and the fact that it seals |
| `$$streaming-task` | What the streaming gate keeps for one task: its last stamp, its strays |
| `answer-by-act-id` | The query: act-id → the recorded answer, or nothing |
| `locks-of-request` | The query: a request's act-id and a range of indexes → those locks, the bytes and the way of each |
| `class-of-layer` | The query: layer → its class, or nothing |

**Namespaces**, each `softland.store.` and the word.

| Word | What it is |
|---|---|
| `text` | The one text of a thing |
| `digest` | The digest of a thing, over its one text |
| `declaration` | The door's declaration: what arrives on a depot |
| `stamp` | The stamp: clock time and counter in one integer |
| `seal` | Locks and the seal |
| `first-act` | The first act of a layer |
| `decision` | The checks of the decision, as functions of an act and rows |
| `record` | The record's rows: their keys, their making, their reading |
| `record-pstates` | The record as PStates: the four schemas |
| `streaming-gate` | The stream topology |
| `door-reads` | The three queries that the door reads the module by |

**Fields of a row, and of an answer as a caller gets it.**

| Word | What it is |
|---|---|
| `:refused` | In an answer row: the reason word. Absent when the act was accepted |
| `:stamp` | In an answer row and a fact row: the stamp of the decision |
| `:digest` | In an answer row: the digest of the thing that was decided |
| `:value` | In a fact row: the bytes of the fact's value, sealed or plain. Absent for a retract |
| `:lock-act`, `:lock-index` | In a fact row: the lock that seals `:value`, as the uuid of the act that asked for it and its index |
| `:by-act`, `:by-index` | In a fact row: the fact that wrote the row, as the uuid of its act and its index |
| `:bytes` | In a lock row: the lock's 32 bytes |
| `:way` | In a lock row, and in a lock as `locks-of-request` gives it: the word of the way of sealing that the lock is for |
| `:aes-256-gcm` | The one way of sealing that this build knows, as the value of `:way`: AES-256-GCM, a random part of 12 bytes in front, a tag of 16 bytes, the place as associated data |
| `:seals-act`, `:seals-index` | In a lock row: the fact that the lock seals, once it seals one |
| `:last-stamp` | In the task's value: the last stamp that the gate gave on this task |
| `:strays` | In the task's value: by reason word, the digests of the strays seen |
| `:verdict`, `:reason` | In an answer as `answer-by-act-id` gives it: `:accepted` or `:refused`, and the reason word |

**The words of a stray** (kept in `:strays`, never in an answer).

| Word | What it is |
|---|---|
| `:no-act-id` | The thing is no plain map, or holds no `:act-id` |
| `:act-id-malformed` | Its `:act-id` is not `[layer class uuid]` |
| `:act-id-of-the-other-gate` | The class slot of its act-id is not the class of the gate where it arrived |
| `:act-id-of-another-layer` | The layer slot of its act-id is not its own `:layer` |

**The reason words of the declaration**, beside `act.clj`'s seven.

| Word | What it is |
|---|---|
| `:class-not-the-act-ids-class` | `:class` is absent, or is not the class slot of the act-id |
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
| `:lock-id-malformed` | `:lock-id` is not `[act-id index]` with an index of 0 to 255 |
| `:place-named-twice` | Two facts of the act are on one entity and key |
| `:replaces-own-fact` | A fact names a fact of its own act in `:replaces` |
| `:lock-named-twice` | Two facts of the act are under one lock |

**The names in the declaration**: its shapes, and its rules. Each rule's reason word is in the table
above; the rules and what each holds are in the table under "The door's declaration".

| Word | What it is |
|---|---|
| `:arrived-act` | The shape of an act as it arrives on a depot |
| `:arrived-fact` | The shape of a fact as it arrives |
| `:class-word` | The shape of a class word: `:streaming` or `:microbatch` |
| `:lock-id` (as a shape) | The shape of a lock's id; the same word as the part of a fact, which is the contract's |
| `:lock-count` | The shape of the count that a request for locks holds |
| `:class-is-the-act-ids-class` | The rule: `:class` is the class slot of the act-id |
| `:facts-within-the-limit`, `:based-on-within-the-limit` | The rules: the two counts of an act are within their limits |
| `:store-key-fact-has-no-seal` | The rule: a fact under a store key carries no seal |
| `:class-fact-holds-a-class-word`, `:permission-fact-holds-a-permission`, `:lock-request-holds-a-count` | The rules: the value of a fact under a store key has the shape that the key names |
| `:permission-fact-sits-on-its-holder`, `:lock-request-sits-on-its-act` | The rules: the entity that such a fact sits on |
| `:lock-request-within-the-limit` | The rule: a request asks for 256 locks at most |
| `:plain-value-is-sealed`, `:sealed-fact-has-no-plain-value`, `:seal-is-whole` | The rules: a value under a plain key comes sealed, with no plain value beside it, and with its lock id |
| `:sealed-is-bytes`, `:sealed-within-the-limit` | The rules: `:sealed` is a run of bytes, and within its limit |
| `:place-named-once`, `:no-fact-replaces-a-fact-of-its-act`, `:lock-named-once` | The rules between the facts of one act |

**The reason words of the gate.**

| Word | What it is |
|---|---|
| `:carried-stamp-too-far-ahead` | The act carries a stamp more than 10 seconds ahead of the gate's clock |
| `:layer-belongs-to-the-other-gate` | The layer is made, and its class is the other gate's |
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
| `:lock-way-not-known` | The way of sealing that the lock's row names is not one that the code knows, so the lock opens nothing |
| `:lock-does-not-open-the-value` | The lock's bytes do not open the sealed bytes at this fact's place |

**Marks in the one text**: `#bytes`, `#double`, `#keyword`, `#other`, `#too-deep`, `#unreadable`. Each
is in the table under "The one text".

## For the second part

What the microbatch gate gets from this part, by role; the one thing the streaming gate needs from
it; and what this plan leaves to it on purpose. This plan designs none of its mechanisms.

**What it gets.**

1. *The checks of the decision, as functions that read nothing* (`softland.store.decision`,
   `softland.store.first-act`). Each takes the gate's class, an act in its one form, and rows; each
   gives nothing or a reason word, in the contract's order. "The same checks" (C item 4) is then
   literal: the microbatch gate calls them. What it must bring is the rows, each as every earlier act
   in its one order left it. In the stream gate the single task gives that for free; in a batch, its
   one order must. The form of a first act already holds the base: class microbatch, the first
   permission `[:system :base :base]`, `:who` `:system`.
2. *The writes of an accept, as data.* The function that gives the rows of an accept names each row by
   its key and its content. Where each row goes is the caller's. Locks that a request asks for are
   named as "make n"; the bytes are made by the caller, at the write.
3. *The door's declaration* (`softland.store.declaration`): the one check of what arrives, the limits,
   the store keys, and the test for a stray, which takes the gate's class.
4. *The one text, the digest, the stamp, the seal.* The next stamp is a function of the clock, a last
   stamp and the carried stamps; it does not know where the last stamp came from, so the greatest of
   several tasks' last stamps is the same call (C rule 18).
5. *The record's shape, written once* (`softland.store.record`, `softland.store.record-pstates`): the
   four schemas and the text of each key. "Same record shape" (C item 4) means the microbatch topology
   declares the same four schemas under its own names. It cannot write this part's PStates: in Rama
   only the owning topology writes a PState.
6. *The three reads, each with a branch by class* (`softland.store.door-reads`). In this part the
   branch for the class microbatch gives nothing. Each takes the layer as its first argument for
   direct routing; if a microbatch answer does not live on the layer's task, that branch routes on
   from there, or the first argument changes.
7. *Where a streaming layer's class is*, for IS X41: the fact at the place `"<layer> <layer> :class"`
   in `$$streaming-facts`, on the task that the hash of the layer id gives. Any topology of the module
   can read it there.
8. *The proof's hand* and its `SHOWN` style, to extend for the base and the microbatch depot.

**What the streaming gate needs from it: one read.** Check (c) has a step that asks "does the other
gate's record hold this layer?". It is asked inside the streaming gate's event, on the task that the
hash of the layer id gives, and that event must not hop (IS O1; IS C3). So the answer must be one read
on that task. That closes IS X31, and it is the only place where this part's code changes.

**What the shared forms already fix for it.** They are forms of the act on arrival, so both gates
hold them: a permission fact sits on its holder, and a holder has one permission in a layer; a request
for locks sits on the uuid of its own act, and a lock's id is `[request's act-id, index]`; an act
names a place once; the limits; the words. One consequence it may use: a lock's id holds its request's
uuid, and that uuid is the entity of the request's one fact. So whatever the microbatch depot derives
from a request's facts can be derived from a lock's id as well.

**What this plan leaves to it** (`DECOMPOSITION.json`, second entry):

- the microbatch depot and what it derives from an act;
- the one order in which the acts of a batch are decided, and the same winner on every attempt
  (C rule 6);
- an act that touches several tasks, accepted whole or refused whole, and the decisions of a batch
  visible together;
- where the rows that belong to a layer as a whole live for a microbatch layer (its class, its
  permissions, its locks, its answers), how the gate on each task reaches them, and how a lock that
  was asked for ahead comes to sit beside its value there;
- the one stamp of a microbatch act (C rule 18), and its task's last stamp;
- its own strays;
- the base's making; one act through the microbatch gate; IS X31 and X41;
- the answers to C rules 9 and 19 for its gate, and the reads that one microbatch act costs.

Nothing in this part's shapes needs a layer's rows to be on one task, a batch to be decided on one
task, or an answer to live where a fact lives. Every key names its layer and its own id; no row
points at a task.

## The proof's hand, and what the proof shows for this part

**The hand.** The door is not built; the proof plays it with a few functions of its own namespace (C
"The proof"). In the order of one act:

1. It reads the class of the layer with `class-of-layer`. For a layer that is not made it takes the
   class that the layer starts with (`first-act/class-at-making`).
2. It makes the act-id: `[layer class a random uuid]`. Two sources differ here, and the plan follows
   Sid's: the README says "the last a random UUID" (R 6-Oct 5); the Rama skill says to make every id
   for a stream topology as a version 7 UUID, which holds its time of making in its first 48 bits
   (skill unique-ids.md). The module takes either: `act.clj` takes a UUID of any version. The hand
   makes a random one. What version 7 would give is that the answers and the locks of a layer are
   written in the order of their keys. Whether "random" rules that out is Sid's word; the change is
   one line of the hand.
3. It asks for locks: an act with one `:lock-request` fact on that act's own uuid; appended with
   `:ack`; its answer read by act-id; its locks, the bytes and the way of each, read with one call
   of `locks-of-request`.
4. It seals each value: the UTF-8 of the one text of the value in its normal form, sealed to the
   fact's place under one lock that it has not used, in that lock's way.
5. It appends the thing to `*streaming-depot` with `:ack`. An error from the append is not an answer
   (Q22 A).
6. It reads the answer with `answer-by-act-id`, and compares the answer's digest with the digest of
   what it sent.

It keeps each thing that it sent, so that a repeat is the same thing byte for byte (IS X106).

**The cluster.** In-process, with more than one task, so that two layers can be on two tasks, and
more than one worker.

**The contract's lines that this part shows**, each on a `SHOWN` line: a person's layer made by the
system for that person, with the class streaming; a group's layer made by the person who creates it;
an act into a layer that is not made refused; one act landing through the streaming gate and read back
by act-id; a refusal recorded with its reason; a repeat answered from the record, not decided twice
(the same stamp on both reads); a second, different act under a used act-id told so (the digests
differ); an unsealed value refused; a stale `replaces` refused; a setting key on another layer
refused; an act with a permission for another layer refused; an act under a permission that does not
stand refused; a permission given beneath another by its holder, and an act under it accepted; a
permission given by one who does not hold the one above refused.

**The two that it cannot show**: the base made by the system, with the class microbatch; one act
through the microbatch gate. Both need the next part.

**Beyond the contract's lines, for the rules that this part carries**: each kind of value comes back
from the depot's log with the digest it was sent with (IS X16); a stray is kept under its word and
gets no answer (C rule 10); the path of a lock: asked for, read, used, and then a second value under
it refused, the lock of a refused act still free, sealed bytes copied to another place not opening (C
rules 5, 15, 16); a fact that names nothing is never stale, and a retract can be replaced (C rules 2,
3); a place named twice (C rule 12); a second write and a retract under a store key (C rule 13); a
`granted-in` that is not the act's layer (C rule 14); a carried stamp too far ahead (C rule 17); a
thing over each limit (C rule 20); the stamps of one layer's answers increasing.

**What this proof does not reach**: a retry by Rama, a failed batch and the window of IS X29, a
restart, a move of a task's leader; the three limits of F9, since the in-process cluster's timings
say nothing about them; IS X31.

## The plan's choices, and what could not be met as written

**The plan's choices**, each with its reason where it is made. None is a decision of Sid's.

1. Three flat PStates with string keys that hold the layer, not one PState keyed by the layer ("PState
   Design").
2. A permission fact sits on its holder; so a holder has one permission in a layer. **Sid should
   know.** The other answer is a change of one rule of the declaration and of the key that check (d)
   reads.
3. A request for locks sits on the uuid of its own act; a lock's id is `[request's act-id, index]`.
4. Check (d) is one read, and "each permission above stands" rests on how a permission row can come
   to be; check (c), for a layer that is made here, is read off the same row.
5. The sign of a stray is a set of digests for each reason word; the count is the number of different
   strays. The other answer, an exact tally, needs a second reader of the depot.
6. The gate does not read what a lock opens (rule 16 as written). The other answer, holding the opened
   bytes to `act/value?`, has its place in the seal step.
7. No value in the ack.
8. The source: retry mode "all after"; start from the beginning.
9. The cipher's parts: AES-256, a random part of 12 bytes, the place as associated data. By the build
   session's ruling of 8 October a value carries no version byte, and the way of sealing is one word
   in the lock's row. The word itself, `:aes-256-gcm`, and the field's name, `:way`, are the plan's.
10. The stamp's packing: milliseconds × 65,536 + counter.
11. SHA-256 over the one text, kept whole; the one text's table.
12. The last stamp is read from the record at each decision, not kept in memory (the project's caching
    rule).
13. The form of a making act: exactly two facts, in either order.
14. The placing of the checks that the contract does not place: the 10-second rule after (b); the
    store-key step after (g); the seal last. Two words inside (d).
15. Four words for "not decided" and "not built" (C item 3; C rule 13), where the rule says "the
    word".
16. Each query takes the layer as its first argument.
17. The second alias `:store-module`; `rama-helpers` left out.
18. The words: 40 reason and stray words, the names of the declaration's shapes and rules, the word of
    the way of sealing, the names in Rama, the fields, the namespaces.
19. The proof's hand makes a random UUID for an act-id, as the README says, and not the version 7 UUID
    that the Rama skill asks for ("The proof's hand", step 2).
20. The door's map of shapes holds `act.clj`'s own entries, and the maker's parts of an arrived act
    are read from them, not written a second time. Inside check (b) the order of the reasons is
    `act.clj`'s own: the shape first, then the rules in the order of their list.
21. `:sealed` is set aside from the engine's pass and held by two rules in code, because no leaf of
    `act.clj` fits a run of bytes and the engine as ruled takes no leaf from a caller. The other
    answer is one more argument of `act.clj`'s new function, and it is the build session's to rule.

**What could not be met as written, and why.**

1. *"Reuse its engine by requiring `softland.store.act`"* (C item 2). **Met, by the build session's
   ruling of 8 October.** The first version of this plan could not meet it with `act.clj` unchanged.
   Now the engine takes the map of shapes and the nesting limits, the declaration is data in the
   language of `act.clj`'s `shapes`, and the same engine reads it; nothing leans on `has-shape?`. One
   part of what arrives stays outside the engine: `:sealed`, which no leaf fits ("The door's
   declaration").
2. *IS X29*: "an act of a layer that is not yet decided is not decided after a later act of that
   layer". Rama's stream topology does not guarantee it across a failed batch. The design narrows the
   window and cannot close it.
3. *F9's first limit*, by the skill's cold cost for a read: 480 to 770 acts a second on one task, not
   1,000. It is a target; the argument that the reads are served from memory is not a measure.
4. *F9's third limit* for values that average under 60 bytes: not met with a lock of 32 bytes for
   each value. The grain (H1) is what moves it.
5. *"Each permission above it in its chain stands"* (C item 3(d)) is not read from the record; it is
   held by how a permission row can come to be. The gate's answer is the same for every record that
   this build can reach.
6. *"The gate counts it"* (C rule 10): the gate keeps the different strays, and the count is their
   number. A stray sent twice counts once.
7. *IS X16* is said from the docs, not found by a run. The proof's first check shows it.
8. *IS X31* stays open in this part, as `DECOMPOSITION.json` says.

**For the build to check first.** Before anything else, once the engine of `act.clj` takes its
arguments: `clojure -M:store -m softland.store.act-proof` still passes its 150 checks, with no check
changed (this stage ran it on the tree as it stands: 150 passes, none failed). Then what this stage
did not verify: a byte array as the class of a schema position; that a record read back from the
depot's log has been through serialisation; that no call of Rama's client lets an appender pick the
partition of a `hash-by` depot; RocksDB's sharing of key fronts, which the lock's byte count leans
on.

## Design difficulty log
<!-- An honest, first-person record of where this design was hard to settle. -->

What was not hard: the depot, the topology's type and the partitioning. The contract gives all three,
and each is also what the reads want.

1. **One PState keyed by the layer, or flat keys.** I began with one PState keyed by the layer,
   because the skill's rule is to collect what shares a key and a partitioner, and every row here
   shares the layer. It stopped being a contest when I counted: each step through a top-level key or
   a subindexed map is a read, and a write that reaches into a subindexed map loads its parent
   first. That made the layer-keyed design 6 to 8 reads for an act against 4. What I am least sure
   of is the model, not the arithmetic: if Rama keeps a layer's row in memory between the steps of
   one event, the nested design costs less than I counted. I costed by the skill's model, as the
   stage asks.
2. **Rows with no `Object`, when an id is of two classes and a fact's value is of three kinds.** This
   took longest. Keys became strings because that is the one composite whose order the docs give.
   For the fact row I weighed an interface with three records against one shape with two fields that
   can be absent. I chose one shape and hold a plain value as the bytes of its text. A reviewer may
   read that as three variants written with optional fields, which the skill forbids. My reason is
   that every row means one thing, the current fact of a place, and each field has one class.
3. **Which entity a permission sits on.** A real contest between "on its holder" and "on an entity
   made from the permission's digest". The second keeps any number of permissions for a holder at the
   same cost. I chose the first for plain reading and one structure. It makes a rule about people:
   one permission for a holder in a layer. I do not know that Sid wants that rule, so it is flagged
   and the way back is named.
4. **Check (d) without reading the permissions above.** The contract's words name them. I costed
   reading them, up to 3 more reads on every act, against holding them by how a row can come to be.
   Nothing in this build can make the reads find something. I am confident of the reasoning and less
   confident that a reviewer will accept reasoning in place of a read.
5. **Check (c) read off the same row.** One more read of the class at every act was the plain way. I
   took the invariant instead, and what settled it was noticing that Rama itself will keep it: a
   layer whose class changes must move to the other gate's PStates before that gate can write to it.
6. **The sign of a stray.** A counter came first. It fails under a retry, and with the retry mode
   that the order needs it fails often. A second reader of the depot gives an exact tally and costs
   every act a second reading. I chose a set of digests. I am not sure that "the number of different
   strays" is what rule 10 meant by "counts it".
7. **The last stamp.** Keeping it in memory would take a quarter of the reads off every act, and the
   first limit is where it shows. The project's rule on caches is plain, so I left it out. If one
   thing of this plan is opened again after a measure, it is this.
8. **IS X29.** I looked for a way to make the gate hold the depot's order under a failure and found
   none without the record's position in its partition. "All after" narrows the window. I could not
   show the sentence from Rama's guarantees, and the plan says so in place of a mechanism that would
   only look like one.
9. **What the gate does with the bytes that a lock opens.** I went back and forth. Holding them to
   `act/value?` closes the one place where the maker's form is not held by any gate. Rule 16's words
   stop at "the lock opens it". I left it as written.
10. **Reusing `act.clj`'s engine.** As first written, with `act.clj` unchanged, the sentence could not
    be met in full, and the plan leaned on what `has-shape?`'s code does beyond its docstring. The
    build session ruled on 8 October that the engine may take the map of shapes, and the declaration
    is now data that the same engine reads. What stayed hard is one part. No leaf fits a run of
    bytes, and the engine as ruled takes no leaf from a caller, so `:sealed` is set aside and held by
    code. I did not widen the ruling to add a leaf; I say what it would take.
11. **The words for what is not decided and not built.** Rule 13 says "the word", one. Item 3 asks for
    a word that says "not decided" for two of the cases. I made four. One word for all of them may be
    what was meant.
12. **The retry mode.** "All after" makes a record that fails every time cost more: every record behind
    it is processed again at each try. Nothing in the gate may throw (IS C1), and that is the guard. A
    record that did throw every time would stop its partition in either mode.
13. **The longest event.** An act at every limit holds its task for some tenths of a second. I found
    no way to give the task back in the middle without letting another act of the layer in between
    this act's reads and its writes. It stays bounded by the limits, which are kept, not decided.

