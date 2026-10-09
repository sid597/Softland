# Plan

<!-- Phase 1 Step 5. Fill in after completing Steps 1-4. After completing this, fill in PLAN_VALIDATION.md. -->

Part: `door`, the one part of the door's build (`DECOMPOSITION.json`). Stage: Phase 1 of the Rama
skill, "Plan". This paper is a design. It holds no code of the door: it holds what each call takes and
gives, the steps of each call in their order, and each choice with what the other answers cost.
Written by: one fresh session (Opus 5.5), 9 October 2026, for the build session of the door to judge.
It wrote this one file and five probe files outside the repository, and changed nothing else.

## How the skill's template is adapted

The template is written for a Rama module: PStates, depots, topologies, partitioners. The door
declares none of these. It is client code over a module that exists: two depots and three queries
(`src-store/softland/store/module.clj`, `module/door_reads.clj`). No section of the template is left
out. Each one is answered for the door, or says in a line why it does not apply.

| The template's section | What it holds here |
|---|---|
| Reads | The door's reads of the cluster: the three queries, when each is called, and what a maker reads through the door. |
| Writes | The door's appends: an act, and a request for locks. Which depot, which ack level. |
| PState Design | The door declares no PState. The section holds the one state that the door has, the stock of locks, with each choice costed against another answer. |
| Depots | The door declares no depot. The section says which of the two depots each append goes to, and where the thing lands. |
| Topologies and PStates | The door declares no topology. "Topology choice" becomes the ack level of each append. The section also says on whose thread the door's code runs. |
| Query Topologies | The door declares none. For each of the three that exist: examples of what the door passes, and how many reads each costs a task. |
| Partitioning efficiency | What each call of the door costs the cluster: round trips, seeks and rows read, for each kind of act, at 1, 16 and 128 tasks, and held against the gates' aims. |
| Design Decisions | Subindexing and colocation do not apply (one line each). The choices that are the door's own are in their own sections and are listed here. |
| State primitive selection | The stock: the memory of one process. Why it is not durable, and what is never remembered. |
| Resource usage analysis | The memory of the door's process, and what the door's calls leave in the cluster for good. |
| Design difficulty log | Where the design was hard to settle. |

Sections that the template does not have, and the door needs: the calls of the door; the way of one
act in; the answer back and the wait; the door's own words; what holds when something fails; the
door's own proof; the explanations; what is not built; what goes to the receipt; the words that a
session made; the points for the build session; and where each of the twelve things of the split and
each ruling is met.

## How to read this paper

**Sources.** `C` is the contract, `src/proposal/backend-on-rama-2026-10-02/BUILD-door.md`: `C part 4`
is a numbered part of its "What to build", `C D3` one of its builder's rules. `IS` is the implied
spec, `door/IMPLICIT_SPEC.md`: `IS G12`, `IS O4.14`, `IS matrix D`. `P7` is the build session's ruling
on that point, at the end of the implied spec; "rule A" and "rule B" are the two rules there. `R` is
`src-store/README.md`, Sid's decisions. "The store's map" is `src-store/softland/store/README.md`.
`docs 14:212` is `reference/rama/docs/14-depots.md`, line 212. That copy of Rama's documentation
records its extraction on 22 March 2026 and no version (`reference/README.md`); the module runs on
`com.rpl/rama` 1.6.0 (`deps.edn`, alias `:store-module`). `skill foreign-client.md:243` is a file
under `.claude/skills/rama/references/`.

**Five kinds of statement.** The paper keeps them apart.

- *given*: a source file says it. The citation follows.
- *Rama's documentation*: the local copy says it. File and line follow.
- *ran*: this session ran it, and the line that it printed follows. The probes are under
  `/mnt/data/projects/research/softland/door-build-2026-10-09/plan-probes/`; "What the plan found
  out" lists each with its command.
- *derived*: it follows from the sources. The reason follows.
- **the plan's choice**: the sources leave it to the builder. The choice, its reason, and what the
  other answer costs.

A timing from the in-process cluster says nothing about production (skill `testing.md:38`: "IPC
timings are not a proxy for production latency or throughput. Only order-of-magnitude results mean
anything"). Each one here is marked as such.

**Words of this paper.** "Act", "maker", "door", "gate", "depot", "act-id" as the contract defines
them. "Key" is a fact's key. "Lock" is an encryption key; no other thing is called a lock here. A
*thing* is what is appended to a depot. A *value fact* is a fact that carries a value and whose key is
not one of the three store keys (IS O4.2). A *round trip* is one call of Rama's client that goes to
the cluster and comes back: one append, or one query. A *read* on a task is one RocksDB point read or
the first seek of a range read; an *iteration* is one more row of a range read (SKILL.md, "Performance
costs"). The words that this plan makes are under "Words a session made, Sid's to change", near the
end; four of them are used throughout: a *stock*, a *run*, an *open request* and a *seat*, each
defined where the stock is designed.

## The design in short

| Thing | What it is | Where it lives |
|---|---|---|
| `softland.store.door` | The door. Opening; act-ids; seal and send; the answer back. The only namespace of the door that requires Rama, and only Rama's client calls. | `src-store/softland/store/door.clj`, beside `act.clj` and `module.clj` |
| `softland.store.door.additions` | Pure. What the door makes of one act: the tests of rule A and rule B, the walk over the facts, the thing to append, the request for locks. | `src-store/softland/store/door/additions.clj` |
| `softland.store.door.stock` | Pure. The stock as a value: runs of locks, taking the locks of one act, how many a request asks for. | `src-store/softland/store/door/stock.clj` |
| `softland.store.door.words` | Pure. Everything that the door says to a maker, declared as data: the four answers, the two acks, and the door's own fourteen words. | `src-store/softland/store/door/words.clj` |
| The stock | The door's one memory. One stock for each layer, depot and who that asked for locks. It holds unused locks, and at most one request whose locks are not read yet. | The memory of the door's process. Empty at each opening. |
| The door's proof | A fourth proof, through the door, on Rama's in-process cluster. | `test-store/softland/store/door_proof.clj` |

The stock in six lines:

1. A stock is for one layer, one depot and one who. A lock goes only to an act of that layer and
   that who (C part 3, the cutter's rule).
2. The door asks for locks only inside the call of a maker whose act needs more locks than its stock
   holds (P16). It then asks for as many as that stock has given out so far, at least 4, at most 64
   in a streaming layer and 256 in a microbatch layer, and never fewer than the act needs.
3. One call at a time asks for one stock. Another maker that needs locks of that stock waits for
   that call, inside the door's own bound (10 seconds), and then finds the locks or asks itself.
4. A maker is handed the refusal of a request only when its own call sent that request.
5. The locks of one act are taken in one step, in the order of the stock: neighbours of one
   request, or of two where the act falls on the border between two requests.
6. A lock that was taken never comes back (P7). A request whose answer or whose locks the door did
   not read is remembered and read at the next need of its stock (P8).

The wait: `answer-of` reads once; with a bound it reads again each 200 ms until an answer is there or
the bound has passed. The door's own words are fourteen; each says what the door could not do, and
each says whether nothing was appended or the door does not know.

## Where the code sits, and what it may require

*given*: C "What to build": one namespace, `softland.store.door`, in
`src-store/softland/store/door.clj`, "at the top of the tree beside `act.clj` and `module.clj`"; the
placement is "the cutter's engineering choice, the builder may vary with its reason"; "the pure parts
of the door [...] may be split into a pure namespace so that the pure proof can hold them; builder's
choice". P6: the door "opens no PState, makes no key of a row and no row"; it may require the pure
functions that each hold one rule that it needs.

**The plan's choice: the contract's placement, and the pure parts split into three namespaces under
`door/`.** The tree then reads `door.clj` above `door/` as it reads `module.clj` above `module/`.

Two answers were weighed.

- **(a) One file.** `door.clj` holds everything and requires Rama. The pure proof stays at 449. Every
  rule of the door is then proven only on a cluster: each row of the table "Things given to the door
  that are not an act" (IS O4), the bounds of P9, the taking of locks from a stock. A run of the
  cluster proof takes minutes, and it cannot hold a case such as "200 acts of mixed sizes never leave
  more unused locks than the last request asked for" as a value.
- **(b) The split.** Nearly all of the door is pure: it is tests, a walk over facts, the building of
  two maps, arithmetic on a queue, and a table of words. What is not pure is small: five client
  handles, one append, three queries, a clock, a sleep, and what holds between two makers at once.
  With the split the pure proof holds each rule as a case, with Clojure alone, and the alias `:store`
  stays the guard that this code needs nothing else (`src-store/README.md:33-38`, IS O6.5). The
  cluster proof then shows what only a cluster can show.

The plan takes (b). Its cost: three more files and their maps, and the pure proof grows by 89 cases
(counted under "The door's own proof").

**What each namespace requires.**

| Namespace | Requires | Why each |
|---|---|---|
| `softland.store.door` | `com.rpl.rama` (the client calls `foreign-depot`, `foreign-query`, `foreign-append!`, `foreign-invoke-query`, and nothing else of it); `softland.store.door.additions`, `.stock`, `.words`; `softland.store.door.digest`; `softland.store.gate.first-act`; `softland.store.record.rows` | The client calls are the module's surface. `digest/hex-of` is the digest that a gate keeps (C part 4). `first-act/depot-at-making` is the depot that a layer starts with (C part 2 names the file). From `record.rows`: `routing-id`, the first argument of two queries, and the three tests of what a caller passes to a read, `depot-read`, `answer-read-by-routing-id`, `lock-range-read-by-routing-id`, which `module/door_reads.clj:69-71` tells a caller to use (P6). |
| `softland.store.door.additions` | `softland.store.act` (`value?`, `normal-value`); `softland.store.door.declaration` (`usable-id?`, `act-id-in-layer?`, `store-key?`, `max-facts`, `max-sealed-bytes`, `max-locks-in-request`); `softland.store.door.seal`; `softland.store.door.text`; `softland.store.door.words` | `value?` is "the one function that says what a value is" (rule B). The two tests of an id and of an act-id are the ones that `stray-reason` is made of (rule A, P2). `store-key?` tells a fact that stays plain (C K4 B). The three limits are read from the declaration in one place (P9). |
| `softland.store.door.stock` | `softland.store.door.declaration` (`max-locks-in-request`, `max-facts`); `softland.store.door.seal` (`known-way?`) | The ceiling of one request (C part 3). A lock whose way the code does not know goes into no stock (IS O3, edge cases). |
| `softland.store.door.words` | Nothing outside Clojure | It is a table and two small functions. |

The door requires nothing of `softland.store.module`, of the two gates' namespaces, of
`record/tables.clj` or of `gate/decision.clj`. It names the five things that it opens by their names
in Rama, as text: `*streaming-depot`, `*microbatch-depot`, `answer-by-act-id`, `locks-of-request`,
`depot-of-layer` (`module.clj:9-15`). The door's proof opens the door on the real module, so a name
that drifts fails there. No rule is written a second time (P6), with one line that ruling 10 names:
the test of a plain map, which the tree writes in five places already and the door writes in a
sixth.

`deps.edn` does not change: both aliases hold `src-store` and `test-store` already.

## The calls of the door

Six calls. Two names are the contract's (`act-id-for`, `answer-of`); the other four are this plan's.
Each call but `open` takes the door first. No call throws for what a maker gave (IS O4.11): a call
gives its result, or one of the door's own words. A *word* is a map that holds `:could-not`; no other
result of the door holds that key, and no word holds `:answer`.

| Call | Takes | Gives |
|---|---|---|
| `open` | a cluster handle; the module's name; and, when the opener wants, a map of the door's five numbers | a door, or the word `:find-the-module` |
| `act-id-for` | a layer | an act-id `[layer depot uuid]`, or a word |
| `act-ids-for` | a layer and a count | a vector of that many act-ids of the layer, or a word |
| `send-act` | a maker's act; and, when the maker asked for one ahead, its act-id | `{:act-id :digest :ack}`, or a word |
| `answer-of` | an act-id; the digest that `send-act` gave; and, when the maker wants to wait, a bound in milliseconds | one of four answers, or the word `:read-the-answer` |
| `stock-count` | nothing more | three numbers: the unused locks that the door holds, the stocks that hold them, and the requests whose locks it has not read |

**The door's five numbers**, each with its default and its reason where it is designed:
`:request-wait-ms` 10,000; `:read-every-ms` 200; `:fewest-at-once` 4; `:most-at-once-streaming` 64;
`:most-at-once-microbatch` 256. P8 says "the plan may let the opener of the door give the number";
the plan lets the opener give each of the five, because each is a number of the build and none is
measured. A number that is missing, or is no integer of 1 or more, takes its default. A number over
the declaration's ceiling is cut to it.

**The steps of `act-id-for`.** (1) It tests the layer with `record/depot-read`; a layer that is no id
within the limit ends in the word `:make-an-act-id`, with no round trip. (2) It reads
`depot-of-layer`. A depot word is the layer's depot. Nil says that the layer is not made, and the
depot is then the one that the layer starts with, `first-act/depot-at-making` (C part 2; IS O2.2). A
read that threw is the word `:read-the-layers-depot`, and no id (P21). (3) It gives
`[layer depot uuid]`, the uuid a random one of the JVM's, made from nothing of the act, the maker,
the person or the time (IS O2.3). The door keeps no id that it gave (C D2). In this build the read
and the starting depot always agree (IS G16); the read is made all the same, and the docstring says
that it is what leaves room for a layer whose depot can change (the row ?9, not decided).
`act-ids-for` is the same with one read and as many uuids as the count; a count that is no integer of
1 or more ends in `:make-an-act-id`.

**Why `act-ids-for` exists.** *given*: a maker "may ask for the ids of a series before it sends the
first" (C part 2); the door "remembers no layer's depot between two calls" (IS O2.6). *derived*: with
`act-id-for` alone a series of 20 ids costs 20 reads of one row on the layer's task, which has one
thread that the streaming gate also needs (IS G26). Inside one call the door reads once, so one call
for a series costs one read. The contract "does not say whether a series is one call" (IS O2, edge
cases); this is an engineering choice.

**Why `stock-count` exists.** P22 leaves it to the plan: "The door may say how many locks it holds,
as a count and never as bytes". The plan takes it. The door's proof holds the count against the rows
that it counts in the record round the door, at the restart of the door's memory; and an operator of
the runtime can see what a restart would lose. It gives numbers only.

**What each call costs in round trips.**

| Call | Round trips | Each one |
|---|---|---|
| `open` | 5, once for a process | One fetch of a client for each of the two depots and the three queries. On no act's way. *Ran*, probe 2: 3 ms for the five on the in-process cluster |
| `act-id-for` | 1; or 0 | The read of the layer's depot (C part 2). None for a layer that is no id: the word |
| `act-ids-for` | 1; or 0 | The same one read, for the whole series |
| `send-act` | 1 to 2; 3 more when it asks for locks | "The way of one act in" has the table |
| `answer-of` | 1; or 0; with a wait, one more for each 200 ms that passes without an answer | The read by act-id (C D4). None for a thing that is no act-id |
| `stock-count` | 0 | It reads the door's memory |

**The door as a value.** An opened door is one object of a type of its own. It prints as the word
"door" and the module's name, and nothing else: no handle, no stock, no lock (P18). The type has its
own print method, so that `pr`, `println` and a pretty-printer all give that text. This is care and
no guard: code of the same process can reach a field of any object, and any client of the cluster can
read a lock (IS G24).

## Reads

For each read: who calls it, the access method, and the task that it reaches. The door has no
`foreign-select`: it holds no PState handle (IS O1.1).

| Read | Called by | Access | What it reaches |
|---|---|---|---|
| The depot of a layer | `act-id-for`, `act-ids-for`; `send-act` when the maker gave no act-id | `foreign-invoke-query` of `depot-of-layer` with `[layer]` | One task, the layer's. Streaming layer: 1 read. The base: 1 read, and a second, of `:closed-through`, when the row is there (`door_reads.clj:149-170`). |
| The answer under an act-id | `answer-of`; `send-act` for the stock's open request | `foreign-invoke-query` of `answer-by-act-id` with `[routing-id act-id]` | One task: the layer's for a streaming act-id, the task of the uuid for a microbatch one. 1 read; in a microbatch layer a second when the row is there (`door_reads.clj:87-111`). |
| The locks of a request | `send-act`, for a request that this door sent and whose answer it read as accepted | `foreign-invoke-query` of `locks-of-request` with `[routing-id request-act-id 0 (n - 1)]`, n the count that the request asked for | The same task as the request's answer. Streaming: 1 seek and n iterations. Microbatch: the answer's row, `:closed-through`, then 1 seek and n iterations (`door_reads.clj:113-147`). |

*given*: each query begins with the hash partitioner on its first argument, and "Rama's client
evaluates a leading partitioner on a query's own arguments, so the query goes straight to one task
and never hops" (`door_reads.clj:54-58`; skill `query-topologies.md:120-126`). So each read of the
door is one round trip to one task, whatever the number of tasks.

**Before each read the door tests what it passes**, with the function of `record/rows.clj` that the
head of that query uses (P6; `door_reads.clj:64-71`). When the test gives nil, the door makes no call:
the query would give nil or an empty map for it, and a first argument that Rama's client cannot hash
is reached by no proof (IS G6). So:

- `depot-read` nil (the layer is no id within the limit): `act-id-for` ends in the word
  `:make-an-act-id`. No round trip.
- `answer-read-by-routing-id` nil (the thing is no act-id, or its layer is over the limit):
  `answer-of` gives "nothing yet", which is what the read gives for it (IS O5.4). No round trip.
- `lock-range-read-by-routing-id` is never nil for a request that the door made itself.

**The range of the read of locks is exactly what the request asked for**: indexes 0 to n - 1. The
hand asks for 0 to 255 (`module_proof.clj:209`); the range is cut by the rows that exist, so both
cost one seek and n iterations. The door asks for its own range, because it knows n and then knows
when the read gave fewer locks than the request made.

**What a maker reads through the door**: the depot slot of an act-id, and the answer under an act-id
with its stamp. Nothing else. The door reads no fact of the record for a maker, and reads locks only
of requests that it made itself in this process (C D6; IS O3.8, O5.10).

## Writes

Each write of the door is one append of one thing to one of the module's two depots.

| Write | The thing | Depot | Ack level |
|---|---|---|---|
| A maker's act | The maker's act with the door's three additions: `:act-id`, `:depot`, and for each value fact `:sealed` and `:lock-id` in place of `:v`. Its facts are a vector. | The depot that the act-id's second slot names | `:ack` to the streaming depot; `:append-ack` to the microbatch depot (P17) |
| A request for locks | An act of the door's own making: the `:version`, `:who`, `:permission` and `:session` of the maker's act as given, its `:layer`, `:act-id`, `:depot`, and one fact `{:e <the uuid of the request's act-id> :k :softland.store/lock-request :v n}` | The same depot as the maker's act: the request's act-id carries that act-id's depot | The same level as an act to that depot |

The door appends nothing else. It writes no PState and no row, and a query writes nothing
(`door_reads.clj:7`).

*derived*: the request needs no read of the layer's depot of its own. C part 3 says that its
`:layer` "is the act's", and IS O3.3 that its act-id is "in the depot of the act's layer". Inside the
call the door holds the act's act-id, made in this call from the module's read or given by the maker,
and takes the depot slot from it. A request under an act-id whose depot slot is not the layer's depot
goes to the depot that the slot names, and a gate refuses it there, as it would refuse the act
(P2: "goes to the depot that it names, and a gate answers it").

## PState Design

The door declares no PState: it owns no durable state (C part 1; IS O1.2). The question that this
section asks of a module, "which state serves the reads, and at what cost", has one analogue for the
door: the stock of locks. It is the door's one memory (C part 1), the one thing that makers share at
once, and the one place where the contract leaves the builder a design (C part 3: "is engineering,
with its reason in the docstring").

### What an opened door holds, and what it never holds

An opened door holds:

1. Five handles of Rama's client, borrowed: the two depots and the three queries (C part 1). The
   opener owns the cluster handle; nothing closes the door (P22).
2. Its five numbers, fixed at the opening.
3. The stocks: a map from `[layer depot who]` to one stock. Empty at the opening, whatever the
   record holds from a process before (IS O1.4; C D3: "a restart asks again").

A stock holds:

- its **runs**. A *run* is the unused locks of one request, in the order of their indexes: the
  request's act-id, the next index, and the locks that are left, each as the read gave it (its 32
  bytes and the number of its way). The runs of a stock are a queue, oldest first;
- **how many locks it has given out** since the door was opened. The size of the next request is
  read from it;
- at most one **open request**: a request that this door sent and whose locks it has not read. Of it
  the stock keeps the act-id, the digest of the thing that was sent, the count that it asked for,
  and its state, one of two: *sent* (its append returned) or *in doubt* (its append threw, or has
  not returned yet). Of a request in doubt it also keeps the moment from which the door's bound for
  it is counted, by the clock that the waits read, which never goes back. The stock keeps no answer
  that the door read (ruling 13);
- one **seat**: the call that sits in it is the one call that asks for this stock, or reads what an
  earlier call asked for. A seat is a thing of the JVM on which one thread at a time gets in, in the
  order of arrival, and a thread can stop waiting for it at a time limit.

The door never holds: an act-id that it gave (C D2); a thing that it sent, or its digest, once the
call returns (C part 5; IS O4.12), but for the digest of an open request, which is part of the stock
(IS O1.3); a layer's depot between two calls (IS O2.6); a lock that it has given to a fact; a handle
of a PState; any thing that a maker sent before. `act-id-for`, `act-ids-for` and `answer-of` neither
read nor change the stocks (IS O2.6, O5.9, matrix E1).

**A process opens one door** (P22). Two opened doors are two sets of stocks; each asks for itself and
neither uses the other's locks. No row is wrong for it; rows are wasted (IS O1, Concurrency). The
docstring of `open` says so.

### The stock's key: layer, depot and who

*given*: "a lock is used only for values of acts that carry the same `:who` and `:layer` as the
request that made it" (C part 3; C D3). The contract lists "per layer and who" among the things that
are engineering. A gate holds a lock to its layer and to its depot, and not to who asked for it (IS
G12; `gate/decision.clj:266-272`).

Three keys were weighed.

- **(a) `[layer who]`.** The contract's own words. An act-id that a maker gives back can carry a
  depot slot that is not the layer's depot (P2). Under this key such an act takes locks of the right
  depot and seals with them; the gate of the wrong depot then refuses it, and the locks are lost to
  the stock (P7).
- **(b) `[layer depot who]`.** The depot is the second slot of the act's act-id. An act under a
  wrong depot slot finds its own, empty stock; its request goes to the same wrong depot and is
  refused; no lock is taken. It costs nothing. It also leaves the room that C part 2 leaves for a
  layer whose depot can change (the row ?9, not decided): locks that were made in one depot's record
  are never offered to an act that goes to the other.
- **(c) `[layer depot who permission]`.** No maker then ever waits for a request under another
  permission. But the implied spec says of an act whose who "holds locks in the stock that were
  asked for under a permission that the act does not name: the locks are used" (IS O4, edge cases).
  A holder has one permission in a layer (the store's map, "A permission fact sits on its holder"),
  so a second stock for the same who could only fill from a request under a permission that does not
  stand, and such a request is refused. It would hold nothing.

**The plan's choice: (b).** It keeps the cutter's rule, adds the one thing that a gate holds a lock
to, and costs no round trip and no memory beside one more slot in a key.

The key is made of the act's `:layer`, the depot slot of its act-id and its `:who`, each as given
(P4: the door reads `:who` to choose the stock, and checks nothing about it). `:who` may be anything;
a who that is no id gets a stock that never fills, because a gate refuses its request. `:system` is a
who like any other (C K3 A; C part 6).

### How many locks a request asks for, and how far ahead

*given*: "how far ahead it asks, how many at once (the declaration's limit is the ceiling)" is
engineering (C part 3). The ceiling is 256 (`declaration.clj:76-79`). "Each request is sent inside the
call of a maker whose act needs locks, and may ask for more than that act needs. The door has no
thread of its own" (P16). "The plan says how small a stock keeps that cost" of a restart (C part 3).

*derived*, from P16: **the door asks only at need.** A request is sent only by a call whose act needs
more locks than its stock holds. So "how far ahead" has one meaning: how many locks more than the
act's need one request asks for.

**What each side of the choice costs.** By arithmetic from the shapes of the rows
(`record/rows.clj:7-27`), not by a measure:

- A request is an act. It leaves, for good: one record in the depot's log (the eight parts of the
  thing, about 300 bytes), one fact row (about 140 bytes) and one answer row (about 100 bytes):
  about 550 bytes. It costs the layer's task one event of the gate, and it costs the maker that
  meets it three round trips in a streaming layer (the append, the answer, the locks). In the base
  it costs that maker the time until the request's batch is closed.
- A lock that is never used is one row for good: its key and its 32 bytes, about 100 bytes. A stock
  that a restart loses leaves one such row for each lock that it held (C part 3).

So one request costs the cluster about as much as five unused locks.

Three rules were weighed. `m` is the number of locks that the act needs; `n` is what the request asks
for.

| Rule | A who that writes 1 value and stops | A who that writes 20 values | A who that writes 2,000 values |
|---|---|---|---|
| **(a) A fixed batch**, `n = max(m, 64)` | 1 request; 63 rows left at a restart | 1 request; 44 left | 32 requests; 48 left |
| **(a') A small fixed batch**, `n = max(m, 8)` | 1 request; 7 left | 3 requests; 4 left | 250 requests: one event of the gate more for each 8 values, 12.5% |
| **(b) As many as the stock has given out so far**, between 4 and the most for the depot | 1 request; 3 left | 4 requests (4, 4, 8, 16); 12 left | 36 requests; 48 left |

The acts of the table hold one value each, and the layer is a streaming layer.

- (a) is good for a who that writes much and poor for one that writes little: a who that wrote one
  value leaves 63 rows. Whether most whos write little between two restarts of the runtime is not
  known; a personal layer of a person who writes a note now and then is the case that (a) serves
  worst.
- (a') turns that round. At the streaming gate's aim of 1,000 acts a second on one task (the
  store's map, rule 21), the task would decide 125 requests beside each 1,000 acts.
- (b) follows the who. **The stock holds fewer unused locks than its last request asked for, and
  that request asked for no more than the stock had given out before it** (or 4, or what one act
  needed). So what a restart loses for a who is fewer than 4 locks while that who has written up to 4
  values, less than what it wrote through this door after that, and less than the most for its depot
  when no act needs more. One state is outside the sentence in bold: a call read an earlier call's
  request, its stock was still short, and the call ended before it sent its own (its wait had
  passed, or its own append threw). The stock then holds what it held and that request's locks,
  which is fewer than the act that came short needed, so 255 at most (the check of the plan, F3;
  ruling 11). The next call's request brings the sentence back. The requests of a who that goes on writing are few: sizes 4, 4,
  8, 16, 32, 64 and then 64 each, so 6 requests for its first 128 values and one for each 64 after
  that, 1.6% of its acts.

A fourth rule was built and dropped: to ask for more only when the stock's last request was recent,
by the clock. It needs a second number (how recent), it reads a clock, and it gives no bound that can
be said in one sentence. Rule (b) reads no clock.

**The plan's choice: (b).** In full:

    n = the smallest of:  the ceiling of one request (256)
                          the greatest of:  m
                                            the smallest of:  the most at once for the depot
                                                              the greatest of:  the fewest at once (4)
                                                                                the locks that this stock has given out so far

**The fewest at once is 4**, because a request costs about as much as five unused locks: to ask for
fewer than about that many saves no bytes even when the who never comes back, and a who that comes
back three times has saved three requests.

**The most at once is 64 in a streaming layer.** Past 64 the gain is small: one request for each 64
values is 1.6% more events for the gate, one for each 256 is 0.4%. What grows with it is not small:
the rows that a restart leaves for a who that wrote much (63, or 255), the memory of its stock, and
the length of the gate's one event for the request, which writes one lock row for each lock while it
holds the layer's task (IS G26).

**The most at once is 256 in a microbatch layer**, the ceiling. There a request costs its maker the
time until the request's batch is closed: 752 to 979 ms on the in-process cluster (*ran*, probe 2,
five requests, and 1,034 ms for the base's making act; the module's build saw 873 to 893 ms, IS G17). All that time the stock gives nothing.
So the largest request that the declaration allows is the one that stops a who least often. What
this cannot lift is the first point under "Points for the build session".

**One act never takes more locks than 256** (P9, the first bound): the smaller of the declaration's
`max-facts` and `max-locks-in-request`, both 256 today. A lock past that number cannot seal an
accepted value, and one request can then always cover one act. An act with more value facts ends in
the word `:seal-that-many-values` before any lock is taken. The bound is the door's own, of its
stock, and the docstring says so: if the declaration's `max-locks-in-request` were made less than
its `max-facts`, the door would stop an act that a gate would take with the locks of two requests
(ruling 17; for the receipt).

### What holds between makers who need locks of one stock at once

*given*: "no lock goes to both" (IS O3.6). "Whether the second waits on the first's request or sends
its own is the plan's; either way a maker never waits without a bound on a request that is not its
own, and O3.11 holds" (IS O3, Concurrency). IS O3.11: "A maker is handed the refusal only of a request
that carried its own act's `:who`, `:permission` and `:layer`."

Three answers were weighed.

- **(a) Each maker that finds the stock short sends its own request.** Nothing is shared but the
  queue. In a streaming layer a request is answered in a few milliseconds, so few makers meet. In
  the base a request is not answered for about a second. A who that sends 100 value acts a second
  into the base would send about 100 requests in that second, each for up to 256 locks: 100 acts
  more for the gate, and up to 25,600 locks for a need of 100. The stock has no bound under (a).
- **(b) One request at a time for one stock, and the others wait for it.** A maker that finds the
  stock short waits for the seat. The call in the seat asks, reads the locks and takes its own; the
  next call in the seat finds locks, or asks. The stock keeps its bound. A maker waits for a request
  that is not its own, but never past the door's bound ("How far the door's bound reaches", below).
- **(c) One mutual exclusion around every use of the stock.** Simple, and wrong for the common
  path: a call that asks holds it across three round trips, and a maker whose act the stock could
  serve at once would wait behind it.

**The plan's choice: (b), with two steps kept apart.**

1. **Taking** is one compare-and-set on the stock's value: the locks of one act leave the queue
   together, or none does. It never waits for the seat. Two takes at once cannot give one lock twice
   (IS O3.6; C H1 A).
2. **Asking** needs the seat. A call sits in it only when a take came back short. The seat is fair:
   makers get in by the order of their arrival.

**Whose refusal a maker is handed.** Only the refusal of a request that its own call sent. That
request carries its own act's `:version`, `:who`, `:permission`, `:session` and `:layer` by the way
that it is made, so IS O3.11 holds with nothing to compare. When a call in the seat reads a refusal
of an open request that an earlier call left, it forgets that request, hands the refusal to nobody,
and sends its own. The earlier call was told at its own time what the door then knew.

**How long a maker waits.** One time limit for all of a call's getting of locks: the door's bound,
`:request-wait-ms`, counted from the moment when the call's first take came back short. Waiting for
the seat, reading an earlier call's open request, and waiting for its own request's answer all end at
that one moment. When it passes, the call ends in the word `:get-locks-within-its-wait`, and nothing
is appended for its act (P8).

**How far the door's bound reaches** (the check of the plan, F1). The bound says when the door stops
waiting for the seat and stops starting a step. A call of Rama's client that is on its way when the
bound passes takes its own time, and only the call in the seat can be in one: the append of its
request, for as long as Rama's client holds an append (`foreign.depot.operation.timeout.millis`,
10,000 ms by default, *ran*, probe 3); a read of an answer or of locks, for as long as a query can
take (70,002 ms in one run with Rama's defaults, *ran*, probe 2). So a call that waits for the seat
ends at its bound, and the call in the seat can outlast its bound by one append and by one query. The
docstring of `send-act` says so, as the docstring of `answer-of` does for the maker's bound.

**The door's bound is 10,000 ms** (P8: "a number with its reason"). Two reasons. It is about ten
times the time in which a request's answer was readable in the base on the in-process cluster (752
to 979 ms for five requests, 1,034 ms for the making act, *ran*, probe 2), and five times the microbatch gate's aim of an answer within two runs
of the gate, each inside a second (the store's map, rule 21). So a maker of the base does not meet
the word in ordinary work. And it is the time for which Rama's own client holds a caller in one
append by default: `foreign.depot.operation.timeout.millis` is 10,000 (*ran*, probe 3, read from the
jar's own table of defaults; *ran*, probe 2: an append to a module that was destroyed threw after
10,000 ms). So the door's own bound is no longer than the time for which one append can hold a maker
anyway.

### The life of one request

The states are those of IS matrix D. "The call" is the call in the seat. T is the moment when the
call's wait ends.

| State | What the call does | What comes of it |
|---|---|---|
| **D0** not sent | Makes the request's act-id, in the depot of the act's act-id. Makes the thing and takes its digest. Writes the open request into the stock as *in doubt*, with the moment. Appends. A call does this once at most. | The append returned: the state is *sent*. The append threw: D5. |
| **D1** open, no answer read in this call | Reads the answer by act-id: at once in a streaming layer and for a request that an earlier call left; after one `:read-every-ms` in a microbatch layer when this call sent the request itself; then again each `:read-every-ms`, until T. | Accepted, with the request's digest: D2. Refused, or an answer with another digest: D4. Nothing yet, the request *sent*: at T the word `:get-locks-within-its-wait`; the request stays. Nothing yet, the request *in doubt*: D5, second row. A read threw: the word `:read-its-requests-answer`; the request stays as it was. |
| **D2** answer read as accepted, locks not read | Reads the locks, indexes 0 to n - 1, with one query. | The read came back: D3. The read threw: the word `:read-its-requests-locks`; the request stays open as it was, and the next call in the seat reads its answer again and then its locks, two round trips (ruling 13). |
| **D3** the read of locks came back | In one step: the locks that can seal become a run at the end of the queue, the open request is forgotten, and this call's own locks are taken when the stock now holds them. | The stock holds them: the call leaves the seat and goes on to seal. The stock is still short, and the request was an earlier call's: this call goes to D0 with its own, inside its own wait (ruling 11). The stock is still short, and the request was this call's own, so that locks of it cannot seal: the word `:read-its-requests-locks`. |
| **D4** refused | Forgets the open request. | A request that this call sent: the word `:have-its-request-accepted`, with the request's act-id and the request's answer as `answer-of` gives one. A request that an earlier call left: nobody is handed it, and this call goes to D0 with its own. |
| **D5** the append threw | Leaves the open request *in doubt*, and sets its moment to the moment of the throw. | The word `:append-its-request-cleanly`. |
| **D5**, met by a later call | Reads the answer as in D1. An answer is used as any. | Nothing yet, at a read that began the door's bound or more after the request's moment: the request is forgotten, and this call goes to D0 with its own. Nothing yet, and this call's own T comes before that: the word `:get-locks-within-its-wait`; the request stays *in doubt* (ruling 12). |

**One call sends one request at most, and reads one earlier request at most.** A call in the seat
first deals with the request that an earlier call left, when there is one; then, when its stock is
still short, it sends its own. So the four ways in which an earlier request ends without giving this
act its locks (refused, another digest, too few locks, in doubt for the door's bound with no answer)
all go on to D0, and the door's words about a request are said only by the call that sent it, or by
a call whose wait passed. A step is started only while the call's own T has not passed.

Four things in that table are choices.

**In a microbatch layer the first read of the door's own request comes after one interval.** The
door has just appended the request; its answer is readable only when its batch is closed, one short
run of the gate after its own (IS G17). A read at once is a round trip that can find nothing.

**A request is written into the stock before its append.** If the call ends in any way between the
append and the answer, the stock still knows the request, as *in doubt*.

**A request whose append threw is kept for the door's bound, counted from the throw.** *Rama's
documentation*: "An exception doesn't mean the append did not go through" (docs 14:212). With `:ack`,
a first attempt that fails gives the client an exception also when a retry succeeds (docs 14:214).
When a task's limit of pending records is hit, an `:ack` append throws and "the appends will have
gone through" (docs 11:297). *Ran* by the check of the plan: an append on a thread whose interrupt
mark was set threw at once and went through all the same. Three answers were weighed.

- *(a) Forget it at once.* If it went in, its locks are rows that seal nothing, up to 256. And under
  the third case, a task that is over its limit, each maker that comes back sends one more request
  into a gate that is already behind, and each of them goes through: the door would add load at the
  moment when the gate has too much.
- *(b) Keep it for the wait of the next call.* The plan as first written. The next call that needs
  locks of that stock reads its answer until its own T, and then forgets it. That wait is what is
  left of that call's bound: one read when the call stood long for the seat (the check of the plan,
  F2), and the whole bound, for nothing, when the call comes an hour after an append that truly
  failed.
- *(c) Keep it by its age.* A call forgets a request in doubt only at a read that finds nothing and
  that began the door's bound or more after the moment of the throw. A call that comes later than
  that pays one read and asks at once. A call that comes sooner reads each `:read-every-ms` until
  that moment, or until its own T when that is first.

The plan takes (c), by ruling 12. Each request in doubt gets the whole of the door's bound, and no
call waits for one that is older than that. Its cost is one moment more in the open request. "The
door retries nothing on its own" (C part 3) holds: the door sends no thing twice, and it sends the
next request only inside a maker's call.

**A request that was acked is never forgotten before it is answered.** The depot holds it, so a gate
will decide it. While it is open the stock sends no other request. Under a gate that is slow, calls
end in `:get-locks-within-its-wait` and come back to the same request.

*derived*: a request of the door's own is never a stray. The door builds it right (rule A): a plain
map, the act's layer, which the door has tested, and an act-id of that layer. So "nothing yet, for
ever" (IS matrix A0) is not a state of a request that was acked.

### How the locks of one act are taken

*given*: "The locks that one act names are neighbours: indexes next to each other, of as few requests
as the stock allows. The streaming gate reads one range of lock rows for each request that an act
names, from the least index to the greatest" (IS O4.14; `gate/decision.clj:274-290`).

A take of m locks gives the next m locks of the queue: the oldest run first, in the order of its
indexes, and then the run after it. The door gives them to the act's value facts in the order of the
facts. All m leave the stock in one step, or none does.

Two orders were weighed.

- **(a) Oldest first, across the border of two runs.** Between two requests the stock is one run,
  and an act names one request. The act that falls on a border takes what is left of the old run and
  the rest from the new one: it names two requests, and the gate reads two ranges for it, one seek
  more. That is one act for each request. Each range holds exactly the locks that the act names,
  because the locks that an act takes from one run are next to each other. Nothing is ever left
  behind in an old run.
- **(b) Always from one run when one run can serve the act, and leave a smaller rest behind.** An
  act then names one request more often. But the rests add up: with acts of 3 values and requests of
  64, each run leaves 1, and when three rests of 1 are all that the stock holds, the next act names
  three requests. Over three runs (a) costs the gate 2 seeks more, one at each of the two borders
  that an act falls on (the third border, at 192, falls between two acts), and (b) costs it 2 as
  well, both in the one act that names three requests (recounted by the check of the plan, F4). And
  a rest that no later act fits is a row that seals nothing.

**The plan's choice: (a).** An act names one request, or two at a border. It names more than two
only when the door had fewer locks left than the act needs and read the locks of an earlier, open
request first; that act names one request for each run that it crosses.

What IS O4.14 says with "of as few requests as the stock allows" is met in this sense: the stock is a
queue, and it allows the front. The plan says it here plainly, because under (b) the letter would be
met more often and the gate would not read less.

**What goes into a run.** A lock that the read gave with its bytes and with a way that the code
knows (`seal/known-way?`). A lock with nil for its bytes, or with a way that this code does not know,
is left out: it can seal nothing (IS O3, edge cases; IS G18). No lock of this build is such a lock.
A read of locks that came back ends its request, whatever it gave: the locks that can seal become a
run, and the open request is forgotten. A second read of the same request can give no more: a gate
writes all the locks of a request in the event or the batch that accepts it. So no stock is held at
one request for good (ruling 8). When the stock then still holds fewer locks than the act needs,
what the call does turns on whose request it was (ruling 11). An earlier call's: this call has sent
nothing yet, so it asks for its own, inside its own wait. Its own: it asked for as many as the act
needs, so locks of it cannot seal; the call ends in the word `:read-its-requests-locks` and sends
no second request, and the next call takes what is there, or asks anew.

**A run gives up a lock when it is taken.** The stock holds no reference to a lock that it gave: the
bytes are held by the one call that seals with them, and by nothing after that call returns. The door
makes no copy of a lock's bytes.

### What the stock never does

- It never gives a lock to an act of another who, another layer or another depot (C D3; the key).
- It never takes a lock back (P7): not when a gate refuses the act, not when the act arrives second
  under its act-id, not when the append threw. After a thrown append the door does not know whether
  the lock seals a value; a second act under it could be refused `:lock-already-seals-a-value` for
  no fault of its maker (IS matrix C3).
- It never asks ahead of need, and never between two calls (P16).
- It never reads the locks of a request that it did not send in this process (C D6).
- It never destroys a lock (C part 3; IS O3.10).

## Depots

The door declares no depot. The module declares two (`module.clj:73-74`), and the door appends to
both.

| Depot | Which things the door appends to it | Where the thing lands |
|---|---|---|
| `*streaming-depot` | A thing whose act-id's depot slot is `:streaming` | The task of the hash of its `:layer`, where every row of that layer is (`module.clj:39-46`) |
| `*microbatch-depot` | A thing whose act-id's depot slot is `:microbatch` | The task of the hash of the entity of its first fact (`gate/batch.clj:85-97`). For a request that is the request's own uuid, so it arrives on the task where its answer and its locks will be (`module.clj:48-52`) |

*given*: both partitioners run on the appending client (`module.clj:57-58`). The streaming depot's
applies the keyword `:layer` to the thing (`module.clj:44-46`); for a map whose keys cannot be
compared with a keyword, a sorted map with string keys, that throws on the client
(`store-module/FULL_SPEC_REVIEW.md:252-254`). The microbatch depot's never throws
(`gate/batch.clj:85-97`). *derived*: under rule A the door has read `:layer` of the thing itself in
step 1 of a send, and a throw there is the word `:read-the-act`. So the partitioner meets nothing
that the door has not read before it.

**The order of two sends.** When a call returns, its append is over: it was acked or it threw (IS
O4.13). The door starts no append that goes on after its call, so two sends that one maker makes one
after the other reach the depot in that order. In a streaming layer that is the order of the
decisions, without a failure. In the base two acts on two first entities have no order in the depot
(IS G21): a maker that needs the first decided first reads its answer before it sends the second.
The docstring of `send-act` says so.

## Topologies and PStates

The door declares no topology and owns no PState. What the template asks here, "stream or
microbatch, and why", is already decided for the module (R, 7 October, F6 A and F4 C). The door's
analogue is the ack level of each append.

| Append | Ack level | What the return says | Source |
|---|---|---|---|
| To the streaming depot | `:ack` | The gate has processed the thing and its rows are visible. A signal: the answer is read by act-id. | C part 4; `module.clj:60-63` |
| To the microbatch depot | `:append-ack` | The depot holds the thing. Nothing about the decision. | P17; `module.clj:63-65` |

*Rama's documentation*: `AckLevel.ACK` "waits for [...] all colocated stream topologies to process the
data"; with no colocated stream topology it "is equivalent to `AckLevel.APPEND_ACK`" (docs 14:206).
The streaming gate is the one stream topology, and it reads the streaming depot only. So on the
microbatch depot `:ack` would wait for nothing more than `:append-ack`; the plan names the level that
says what is meant (P17).

Two other levels were weighed for the streaming depot.

- `:append-ack` and then reads of the answer in a loop. The send returns sooner. The maker's first
  read then often finds nothing, so each act costs more reads, on the layer's own task. The contract
  names `:ack` (C part 4).
- No ack. An act could be lost without a word. Not weighed further.

**The ack is given to the maker as one of two words**, `:gate-ran` for the streaming depot and
`:depot-holds` for the microbatch depot, "since the two mean different things to a maker" (IS O4.6).
It is never given as the answer, and no answer is made from it (C D4; R, 7 October, F7 A).

**On whose thread the door runs.** The door has no thread of its own (P16). Each call runs on the
thread of the maker that called it, and holds that thread until it returns: Rama's blocking client
calls, the sleep of a wait, the wait for a seat. The threads that send and receive for Rama's client
are Rama's.

**One set of handles, many threads.** *Rama's documentation*: the local copy says nothing on whether
one depot client or one query client may be used by many threads at once. A search for "thread-safe"
and "multiple threads" finds two passages, on serialisers (docs 17:160) and on clients of other
systems (docs 25:22). The skill says to make each handle once and use it again (skill
`foreign-client.md:243`). *Ran*, probe 2: 16 threads, each with one request for 25 locks and 25 acts
of one sealed value, on one depot handle and two query handles of the in-process cluster: "400 results
in 1044 ms; request verdicts {:accepted 400} [...] act verdicts {:accepted 400} mine {true 400}
distinct lock bytes 400 errors 0". That is one run of a probe, not a guarantee of Rama's. The door's
proof sends from many threads at once (the build session's line after P22), so each run of it shows
the same again.

## Query Topologies

The door declares none. It calls the three that exist. For each: examples of what the door passes,
and the reads on the task, total and meaningful (a read that finds a row).

- **`answer-by-act-id`**
  - A streaming act-id after its acked append: 1 read, 1 meaningful.
  - A microbatch act-id whose batch is closed: 2 reads (the row, then `:closed-through`), 2
    meaningful.
  - A microbatch act-id that is on its depot and not decided: 1 read, 0 meaningful. This is each
    read of a wait but the last.
  - A microbatch act-id that is decided in a batch that is not closed yet: 2 reads (the row, then
    `:closed-through`), and nil (`door_reads.clj:97-107`). The state lasts one short run of the
    gate, so a read of a wait seldom falls in it; where one does, the 9 seeks of kind D below are
    10 (added by the check of the plan, F5).
  - An act-id that fails the head's test: 0 reads; and through the door, no call at all.
  - Fixed or variable: fixed by the depot slot, 1 or 2. The module is built; the door changes
    nothing of it.
- **`locks-of-request`**
  - A streaming request for 64 locks, accepted: 1 seek and 64 iterations, all meaningful.
  - A microbatch request for 256, accepted and closed: 2 reads and then 1 seek and 256 iterations.
  - The door calls it only after it read "accepted" for the request, so it makes no read of locks
    that finds none.
- **`depot-of-layer`**
  - A streaming layer that is made: 1 read, 1 meaningful.
  - A layer that is not made, for its making act: 1 read, 0 meaningful. The read is made all the
    same (IS O2.2): it is what tells "not made" from "made".
  - The base, made: 2 reads.

**The reads that find nothing are the reads of a wait.** They cannot be taken away by the door: C D5
allows no subscription, and the three queries are the whole surface. What the plan can do is make
them few; "The answer back, and the wait" gives the number.

## The way of one act in

`send-act` takes a maker's act and, when the maker asked for one ahead, its act-id. The steps, in
their order. A step that ends in a word ends the call. The order is chosen so that **every test that
needs no cluster comes before the first round trip, and every step that can fail without a lock comes
before the taking of locks.** A lock that is taken never comes back (P7), so nothing that the door
can know beforehand may cost one.

| # | Step | Round trips | Ends in a word when |
|---|---|---|---|
| 1 | Reads the thing: it is a plain map. Reads its `:layer`, `:facts`, `:who`, `:permission`, `:version`, `:session` (P4). | 0 | The thing is no plain map, or it throws while it is read: `:read-the-act` |
| 2 | Its own parts are free: the thing carries no `:act-id` and no `:depot`. | 0 | It carries one: `:add-its-parts` |
| 3 | The layer can be in an act-id: `declaration/usable-id?`. An act-id that the maker gave is an act-id of that layer: `declaration/act-id-in-layer?`. | 0 | No `:layer`, a layer that is no id or is over 256 characters, or a given act-id that is not `[that layer, a depot word, a uuid]`: `:make-an-act-id` |
| 4 | Walks the facts, in order (below). Counts the value facts, m, and makes the plain bytes of each value. | 0 | A fact that the door cannot add its parts to: `:add-its-parts`. A value that it cannot seal: `:seal-a-value`. A 257th value fact: `:seal-that-many-values`. A fact that throws while it is read: `:read-the-act` |
| 5 | The act-id. Given: used as it is. Not given: reads the depot of the layer; nil means "not made", and the depot is then the one that the layer starts with (`first-act/depot-at-making`); a random UUID. | 0, or 1 | The read threw: `:read-the-layers-depot` (P21) |
| 6 | The locks, only when m is 1 or more: takes m from the stock of `[layer, the act-id's depot, who]`, and asks when it holds fewer (the stock, above). | 0; or 3 and more when it asks | The five words about a request |
| 7 | Seals each value with its lock, to the place `[layer entity key]`. Builds the thing. Takes its digest. | 0 | A seal gave nothing, or gave more bytes than the limit: `:seal-a-value` |
| 8 | Appends the thing to the depot that the act-id names, with that depot's ack level. | 1 | The append threw: `:append-cleanly`, with the act-id and the digest (P1) |
| 9 | Gives `{:act-id :digest :ack}`. | 0 | |

**What step 3 and rule A hold.** These tests are what P2 allows and no more: "the thing is a plain
map; its `:layer` is an id that an act-id can hold; an act-id given back has the shape of one and is
of that layer". `declaration/usable-id?` and `declaration/act-id-in-layer?` are made of the tests
that `declaration/stray-reason` makes: the shape of an act-id, the limit of an id's text, the layer.
The test of a plain map is private in that file (`declaration.clj:295-298`), so the table of what
each namespace requires names no home for it (the check of the plan, F10). By ruling 10 the door
writes it once, as a private function of `softland.store.door.additions`, and the pure proof holds
it against `stray-reason` for the thing. So a thing
that passes them is no stray on the depot that its act-id names, and a gate answers it; the pure
proof holds that as a case (for each thing that passes, `stray-reason` gives nil). An act-id given
back whose depot slot is a depot word and not the layer's depot is used as given: the thing goes to
the depot that it names and comes back as a gate's refusal (P2; IS O4, edge cases).

**The walk of the facts (step 4).** The door finds a value "in a fact that is a map, in `:facts`
that is a sequence" (rule B).

- `:facts` is absent, nil, or no sequence (one fact map, a set, a string): the door finds no fact.
  The thing goes as given, and a gate refuses it `:malformed`. A value inside goes plain into the
  depot (P19).
- `:facts` is a sequence. Each fact, at its position:

| The fact | What the door does | Source |
|---|---|---|
| No map: a string, a number, a vector | As given | P19; IS table, row 10 |
| A map that is no plain map (a record), with a value under `:v` | The word `:seal-a-value`, at that fact | Rule B; P3 |
| A map that is no plain map, with no value | As given | Rule B finds no value |
| A plain map with no `:v`, or a nil one: a retract, or a fact that already carries `:sealed` or `:lock-id` | As given; the gate judges it | C part 4; P3, last sentence |
| A plain map with a value, under one of the three store keys | As given, the value plain. Also when it holds `:sealed` or `:lock-id`: the door adds no part to a fact under a store key, so it writes over nothing, and a gate refuses the fact `:store-key-sealed` (ruling 9). This row is tested before the next | C part 4; C K4 B; rule A |
| A plain map with a value, under any other key, that also holds the part `:sealed` or `:lock-id` | The word `:add-its-parts`, at that part: the door would write over what the maker gave | Rule A; P3 |
| A plain map with a value, under any other key: a value fact | It is counted. The value must be a value of the form (`act/value?`); else the word `:seal-a-value`. Its plain bytes are the UTF-8 of the one text of its normal form | Rule B; IS O4.3 |

A key in the namespace `softland.store` that is no store key is "any other key": its value is sealed,
and a gate refuses the fact either way (IS O4.2). The door tells a store key by its being one of the
three (`declaration/store-key?`), as the gates do; it decides nothing of what a key is (K5, K15).

**The second bound of P9, before any lock.** A sealed value is longer than its plain bytes by a
fixed number of bytes, 28 in the one way of sealing that this build knows (`seal.clj:32-33`). The
number is `seal.clj`'s, and it is private there. The door does not write it a second time, and it
does not measure it: by ruling 5 of the build session on this plan, `seal.clj` gains one public
function, `sealed-length`, which takes the number of a way and a count of plain bytes and gives the
count of bytes that `seal` gives for them, or nil for a way that the code does not know. It is made
of the two private numbers of that file, so the rule stays in its one home. No gate calls it, and
no other code of `seal.clj` changes. In step 4 the door asks it for the way that new locks are made
for (`seal/way-of-new-locks`): a value whose sealed length would be more than
`declaration/max-sealed-bytes` ends in `:seal-a-value`, with the two numbers, and no lock is taken.
In step 7 the door holds the real sealed bytes to the same limit, so that it "appends no `:sealed`
that is over the declaration's limit" (P9) whatever way a lock is of. Three other answers were
weighed: to test only after the seal, which loses m locks for each such act; to write 28 into the
door, which is a second home for one number; and to measure the length at the door's first use, by
sealing an empty run with a lock made for nothing else, which is the same rule found a second time
by a run (the plan as first written; see ruling 5).

**The first bound of P9, and facts with no end.** The walk takes one fact at a time and stops at the
257th value fact. P9 asks: "the plan says whether the first bound ends that walk". It ends the walk
of a sequence in which facts with a value to seal keep coming. It does not end the walk of a sequence
with no end that holds 256 such facts or fewer, for example retracts with no end: that call does not
come back, as `act/parse` does not for the same thing (`act.clj:505-506`). The door has no bound on
the count of facts as such: that count is a gate's to judge (`:too-many-facts`), and P9 names two
bounds and no third. The docstring of `send-act` says it. The same holds of any other part of the
thing that has no end, an entity or a part that the form does not know: the seal's place and the
digest walk what they are given (`door/text.clj`, "Limit"), and that call does not come back
either. And `act-ids-for` has no bound on its count but the memory of the process (F12).

**The thing that is built (step 7).** The maker's map, with `:act-id` and `:depot` added, the depot
being the act-id's second slot (IS O4.4), and `:facts` in place: a vector, each fact at the position
that the maker gave it (P20; IS O2.5). A value fact is the maker's fact without `:v` and with
`:sealed` and `:lock-id`; each other part of that fact stays. Each other fact, and each other part of
the act, is the maker's own object, untouched: the door fills no missing part, names no permission,
and carries `:who`, `:session` and `:permission` as given (IS O4.1; C part 6). When `:facts` was no
sequence it stays as it was.

**The digest** is `digest/hex-of` the very thing that is appended (IS O4.7): 64 hex characters, the
form in which `answer-by-act-id` gives a digest back. The door keeps the thing and the digest until
the append returns, and then nothing (C part 5).

**What `send-act` costs in round trips.**

| The act | Round trips | Each one |
|---|---|---|
| The maker gave the act-id; no value, or the stock holds its locks | 1 | The append |
| The maker gave no act-id | 2 | The read of the layer's depot; the append |
| The stock is short, streaming layer | 3 more | The request's append; its answer; its locks |
| The stock is short, the base | 3 more, and the reads of a wait | The request's append; about 5 reads of its answer at 200 ms; its locks |

With the maker's own `answer-of`, one act in a streaming layer is 2 or 3 round trips: what the hand
makes (`module_proof.clj:155-197`).

**One faulty act has two outcomes, by the state of the stock** (P5). An act with a value under a
permission that does not stand: when its stock holds locks, the act goes in and its refusal is
recorded under its act-id; when the stock is empty, the request is refused and nothing is ever
recorded under the act's act-id. The docstring says it.

**The sender.** The door takes `:who`, `:session` and `:permission` as the maker gives them. It
cannot check that the sender is the who that the act names, and does not; neither can a gate (C part
6). The docstring of the namespace says so in those words. A sign-in built later stands in front of
the door.

## The answer back, and the wait

`answer-of` takes an act-id, the digest that `send-act` gave, and when the maker wants to wait, a
bound in milliseconds. It gives one of four answers, or the word `:read-the-answer`.

| What the read gave | What the door gives |
|---|---|
| nil | `{:answer :nothing-yet}` |
| An answer whose digest is the digest given, accepted | `{:answer :accepted :stamp <the stamp>}` |
| An answer whose digest is the digest given, refused | `{:answer :refused :reason <the gate's word> :stamp <the stamp>}` |
| An answer with another digest | `{:answer :not-mine}`, and nothing more (P10; IS O5.3) |
| The read threw | The word `:read-the-answer`: none of the four is true (P10) |

The door compares two hex texts. It holds no thing to take a digest of (IS O5.2). A digest that is
nil or is no digest reads "not mine" when an answer is there, and "nothing yet" when none is (IS O5,
edge cases). The call needs nothing that the door remembers (IS O5.11), reads the stock not at all,
and writes nothing (IS O5.9).

**The stamp** is carried by "accepted" and "refused": it is part of the record's answer and no fact
(P10; C D6). A maker needs it for `:based-on`.

**"Nothing yet" does not mean "nothing is on a depot"** (IS O5.4). It is the answer in five states
that the door cannot tell apart: nothing was ever sent under the act-id; a thing is on its depot and
not decided; a microbatch act is decided and its batch is not closed; the thing is a stray, which the
door itself never sends; the thing given is no act-id.

**A maker's own second send reads "not mine"** (P11). The door keeps nothing, so a second send of one
act with a value is another thing: other locks, another random part. A maker that sends again under
one act-id holds each digest that it was given; at most one of them reads an answer that is its own.
C D2 means that the door never makes one id twice; it does not refuse a second send. The docstring
says it.

### The wait

*given*: the wait is "one question about one act-id", "in a bound the maker gives" (C part 5; C D5).
It is the same for any act-id, streaming or microbatch (P10). "The door starts no read after the
bound has passed" (IS O5.7). "A bound of zero, or none given: one read. A bound below zero: one read"
(IS O5, edge cases).

The wait reads once at its start. While the read gives nil and the bound has not passed, it sleeps
for `:read-every-ms`, or for what is left of the bound when that is less, and reads again. So the
last read starts at the bound and none starts after it. It ends at the first read that gives an
answer, or that throws; a read that throws ends it in the word `:read-the-answer`. A thread that is
interrupted in the sleep ends the wait as if the bound had passed, with the thread's mark set again.

**How often it reads.** Four schedules were weighed. The case: the microbatch gate's aim, 1,000 acts
a second arriving on one depot partition, and about a second to an answer, so about 1,000 waits at
once (IS O5, Throughput).

| Schedule | Reads for one answer at 0.9 s | Queries each second at 1,000 acts a second: the reads for one answer, times 1,000 (recounted by the check of the plan, F4) | How late the maker learns, at the mean |
|---|---|---|---|
| Every 20 ms, as the hand reads (`module_proof.clj:330`) | 46 | 46,000 | 10 ms |
| Every 200 ms | 6 | 6,000 | 100 ms |
| Every 500 ms | 3 | 3,000 | 250 ms |
| 50 ms, then twice as long each time | 6 (at 0, 50, 150, 350, 750, 1,550) | 6,000, from 1,550 waits at once | 650 ms in this case: the answer falls just after a read |

*Ran*, probe 2: the hand's schedule made 37 to 49 reads for each of six answers in the base. Each read
of a wait but the last finds nothing, and each is one round trip and one read on a task whose one
thread the microbatch gate also needs (IS G26). The waits of many acts spread over the tasks, because
an answer is on the task of its act-id's uuid.

**The plan's choice: every 200 ms.** A microbatch layer "is not for what a person does directly" (the
store's map, rule 21), so a tenth of a second on a second is a small price, and a tenth of the hand's
reads is a large gain. The doubling schedule reads no less in the common case and can be late by its
whole last interval. The number is the door's, `:read-every-ms`, and the opener may give another; the
maker gives the bound and nothing else.

**How far the maker's bound reaches** (IS O5.7; the build session's line after P22). The bound says
when the door stops starting reads. A read that is on its way then takes its own time, and Rama's
client decides how long that is. *Rama's documentation* names the setting and gives no number:
"`topology.query.timeout.millis`: This is the timeout used for a query topology's execution" (docs
13:342), a dynamic option of the module, set for a topology. *Ran*, probe 2: on the in-process
cluster with Rama's defaults, a query that slept 120 seconds made the client's call throw
`QueryTopologyInvokeFailed` after 70,002 ms. So with the defaults a wait can outlast its bound by
about 70 seconds, when its last read is one that the cluster does not answer. The door sets none of
Rama's settings. The docstring of `answer-of` says how far the bound reaches, and names the setting.

One other answer was weighed: Rama's non-blocking call with a time limit of the door's own on the
result, so that the call comes back at the bound. The read that was left would go on in the cluster,
and the door would give "nothing yet" for a read that has not said so. IS O5.7 already holds the
plain answer as the requirement.

## The door's own words

*given*: "it tells the maker in the door's own words, which are not a gate's refusal and are kept in
no record, and it appends nothing for that act" (C part 4). "The door's own words name only what the
door could not do, never a judgment of the act" (C D1). "Each word says which of two things is true:
nothing was appended for this act; or the door does not know whether the depot holds the thing"
(IS O6.1; P1).

A word is a map. `:could-not` holds the word. `:appended` holds `:nothing` or `:not-known`, and it is
said of the maker's act. The words are declared once, as data, in `softland.store.door.words`: each
with what it says and with its `:appended`. A result of the door that holds `:could-not` holds no
`:answer` and no `:reason` of its own, so it can never be read as a gate's refusal; the one gate's
word that a word carries is inside `:request-answer`, and it is the request's.

Each word reads after "the door could not".

| Word | From | What it says | What it carries | `:appended` |
|---|---|---|---|---|
| `:find-the-module` | `open` | The cluster gave no depot or no query of the module under that name. No door is opened (IS O1.5). | `:module`, `:threw`, `:said` | `:nothing` |
| `:make-an-act-id` | `act-id-for`, `act-ids-for`, `send-act` | No act-id can be made for this layer, or the act-id given is not one of this layer. Also: the count of a series is no integer of 1 or more. The case of P14. | `:at`: `:layer`, `:act-id` or `:count` | `:nothing` |
| `:read-the-layers-depot` | `act-id-for`, `act-ids-for`, `send-act` | The read of the layer's depot threw. The door does not take "the read failed" for "not made" (P21). | `:layer`, `:threw`, `:said` | `:nothing` |
| `:read-the-act` | `send-act` | The thing is no plain map, or it threw while the door read it. | `:at`, the part that was being read, when one was | `:nothing` |
| `:add-its-parts` | `send-act` | The thing already holds a part that the door adds: `:act-id` or `:depot` on the act, `:sealed` or `:lock-id` on a fact with a value. The door writes over nothing (rule A). | `:at`: `:act-id`, `:depot`, or `[:facts i :sealed]`, `[:facts i :lock-id]` | `:nothing` |
| `:seal-a-value` | `send-act` | A value cannot be sealed so that it reads back: it is no value of the form, it is in a fact that is a map and no plain map, or its sealed bytes would be over the limit (rule B; P9). | `:at`: `[:facts i :v]` or `[:facts i]`; for the limit, `:sealed-bytes` and `:most` | `:nothing` |
| `:seal-that-many-values` | `send-act` | The act holds more values to seal than one act can hold facts (P9). No lock is taken. | `:most` | `:nothing` |
| `:have-its-request-accepted` | `send-act` | The door asked for locks under the act's who and permission, and the record's answer to that request is no accept. Nothing is recorded under the act's own act-id (P5). | `:request`, its act-id; `:request-answer`, the request's answer as `answer-of` gives one, with the gate's reason word | `:nothing`. The request is on its depot and its refusal is in the record. |
| `:append-its-request-cleanly` | `send-act` | The append of the door's request threw. The door does not know whether the depot holds the request (P1). | `:request`, `:threw`, `:said` | `:nothing` of the act. Of the request: not known; the door remembers it as in doubt. |
| `:get-locks-within-its-wait` | `send-act` | The door's wait for locks passed: its request, or one that an earlier call left, has no answer yet, or another call was still getting locks for this stock (P8). | `:waited-ms`; `:request` when this call read or sent one | `:nothing`. A request that was acked stays open and is read at the next need. |
| `:read-its-requests-answer` | `send-act` | The read of the request's answer threw. | `:request`, `:threw`, `:said` | `:nothing`. The request stays open. |
| `:read-its-requests-locks` | `send-act` | The request was accepted, and the read of its locks threw, or gave fewer locks that can seal than the act needs. | `:request`; `:threw` when the read threw | `:nothing`. After a read that threw, the request stays open, and the next call reads its answer again and then its locks. After a read of the call's own request that came back with too few locks that can seal, the request is ended: what it gave is in the stock, and the next call asks anew (rulings 8 and 11). |
| `:append-cleanly` | `send-act` | The append of the act threw. The door does not know whether the depot holds the thing; the maker reads the record (P1; C D4). | `:act-id`, `:digest`, `:threw`, `:said` | `:not-known` |
| `:read-the-answer` | `answer-of` | The read by act-id threw. None of the four answers is true (P10). | `:act-id`, `:threw`, `:said` | `:nothing`: the call appends nothing |

**What is carried.** `:at` names a place in the thing that the maker gave, by the thing's own part
names; it is no new word. `:threw` is the name of the class of what Rama's client threw, and `:said`
is its message. The word about the read of locks carries the class alone. No word carries a lock, a
run of bytes, a thing that was sent, or anything that the door read of the record but the one answer
of a request (P18).

**How the contract's four are met** (C part 4). "No `:layer`": `:make-an-act-id` at `:layer`, and
`:read-the-act` for a thing that is no map. "A layer not made and no depot to start with": no such
case can come about, because `depot-at-making` gives a depot for whatever it is handed; the word has
the case of a layer of which no act-id can be made, `:make-an-act-id` (P14; for the receipt). "A stock
empty and the request refused": `:have-its-request-accepted`. "An append that threw":
`:append-cleanly`, the one word of which "it appends nothing for that act" cannot be said (P1; for the
receipt).

**Why fourteen, and not fewer.** Two cuts were weighed. One word for every fault of a request, with
a second word inside for what became of the request: the maker would then read two words to learn one
thing, and "a word about a request says what became of the request" (IS O6.1) asks for the fate in
the word. One word for every fault of the thing: a maker could not tell "no act-id can be made" from
"this value cannot be sealed" without a second word either. The plan keeps one word for one thing
that the door could not do, and gives the place as data.

**No word is a gate's reason word, or a word of a stray.** The pure proof holds the fourteen against
`act/reasons`, `declaration/reasons` and `declaration/stray-reasons`: no word in common.

**Each word has a case that the in-process cluster reaches** (P13; IS O6.1). They are listed under
"The door's own proof", with the three that need the proof to make a read throw.

## Partitioning efficiency

The door places no data and declares no partitioner, so there is no placement to derive. The
question behind this section is "does one operation cost more as the cluster grows". For the door:
what one act through the door costs the cluster, for each kind of act, at 1, 16 and 128 tasks.

*derived*: every read of the door goes to exactly one task, by the leading hash partitioner of its
query ("Reads", above), and every append goes to one depot partition. So no number below changes with
the number of tasks. The gate's own work for one act is the module's, counted in its plans; the rows
below count what the door adds, and name the gate's cost beside it.

**The kinds of act.** One act is `send-act` and then `answer-of`. "Seeks" are the reads that the
door's queries make on a task, summed over all tasks. "Iterations" are the rows of the range read of
locks. The proportions are **assumed**, to give the sums a weight; no source gives a mix of acts.
The 0.0125 and 0.0005 are one act in 64 and one in 256 of their layers' value acts, the rate of
requests at the most at once.

| Kind of act | Proportion | The door's round trips | Seeks | Iterations |
|---|---|---|---|---|
| A. Streaming, with values, the stock holds the locks; the door made the act-id | 0.7875 | 3: the depot's read, the append, the answer | 2 | 0 |
| B. Streaming, with values, and it meets a request | 0.0125 | 6: A's, and the request's append, answer and locks | 4 | 64 |
| C. Streaming, no value (a making act, a retract, a give) | 0.0600 | 3 | 2 | 0 |
| D. The base, the stock holds the locks; the maker waits for the answer | 0.1295 | 8: the depot's read, the append, 6 reads of the wait | 2 + 5 + 2 = 9 | 0 |
| E. The base, and it meets a request | 0.0005 | 15: D's, and the request's append, 5 reads of its answer, its locks | 9 + 4 + 2 + 3 = 18 | 256 |
| F. A thing that ends in a word before the first round trip | 0.0100 | 0 | 0 | 0 |

An act whose act-id the maker gave back, or took from `act-ids-for`, costs one round trip fewer in
each row, and one seek fewer (two in the base): the table takes the dearer case for every act, so
its sums are the most that the mix costs (F15).

How D's 9 are counted: the base's depot is 2 reads; a wait at 200 ms for an answer at 0.9 s is 5 reads
that find nothing and a sixth that reads the row and `:closed-through`. E adds the request's 4 reads
that find nothing and its fifth of 2, and the read of locks: the request's row, `:closed-through`,
and one seek for the range.

### N = 1 task (single-task baseline)
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| A | 0.7875 | 2 | 0 |
| B | 0.0125 | 4 | 64 |
| C | 0.0600 | 2 | 0 |
| D | 0.1295 | 9 | 0 |
| E | 0.0005 | 18 | 256 |
| F | 0.0100 | 0 | 0 |

Weighted seeks = 2.92   |   Weighted iterator reads = 0.93

### N = 16 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| A | 0.7875 | 2 | 0 |
| B | 0.0125 | 4 | 64 |
| C | 0.0600 | 2 | 0 |
| D | 0.1295 | 9 | 0 |
| E | 0.0005 | 18 | 256 |
| F | 0.0100 | 0 | 0 |

Weighted seeks = 2.92   |   Weighted iterator reads = 0.93

### N = 128 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| A | 0.7875 | 2 | 0 |
| B | 0.0125 | 4 | 64 |
| C | 0.0600 | 2 | 0 |
| D | 0.1295 | 9 | 0 |
| E | 0.0005 | 18 | 256 |
| F | 0.0100 | 0 | 0 |

Weighted seeks = 2.92   |   Weighted iterator reads = 0.93

The sums are flat from 1 task to 128, as they must be when every read goes to one task. What grows
with the cluster is nothing of the door's.

**Held against the streaming gate's aim: 1,000 acts a second on one task** (the store's map, rule
21). One accepted act with one sealed value costs the gate 4 reads and 4 row writes in one event (IS
G22). Through the door the same act costs the layer's task 2 reads more, in two queries: the layer's
depot and the answer. They are on the same one thread (IS G26). So an act of kind A is 6 reads on the
layer's task, and a request adds its own event and 2 reads for each 64 values, about 0.03 read an act
beside the gate's own event for the request (F4).
By the cost of a cold read, 0.3 to 0.5 ms (SKILL.md), 4 reads give 500 to 830 acts a second on one
task (the store's map says 480 to 770) and 6 reads give 330 to 550. The store's map already says of the gate's 4 reads that "the first
aim is met only when the rows are read from memory"; the same holds with 6, and more so. No number
here is measured.

What the door can do about the 2 reads, inside the contract:

- The answer's read is the contract's (C D4; R, 7 October, Q22 A). It stays.
- The read of the layer's depot is one for each act that the door makes an act-id for. A maker that
  asks for the ids of a series with `act-ids-for` pays one read for the series. A maker that gives
  the act-id back pays none in `send-act`.
- To remember a layer's depot between two calls would take the read away. That is a cache of what
  the cluster holds, and it is not designed in. It is the second point under "Points for the build
  session".

**Held against "20 ms from the door's append to the answer in the door's hand"** (the store's map,
rule 21). That span is the act's append with `:ack` and one read of its answer: two round trips. On
the in-process cluster, as an order of magnitude only: the append took 7.1 ms at the median and the
read 1.1 ms (*ran*, probe 2, 40 acts of one sealed value). Before the append, a send that makes its
own act-id adds one read (0.6 ms at the median there), and a send that meets a request adds three
round trips (14.9, 1.9 and 12.8 ms for the first request of 64 there). So a send that finds its locks
in the stock makes no request on its way (IS O3, Latency), and one send in 64 of a who that goes on
writing does. For a who that has just begun, the requests come sooner: 6 in its first 128 values.

**Held against the microbatch gate's aim: 1,000 acts a second arriving on one depot partition.** The
waits cost 6,000 queries a second over all tasks ("The wait", above). A stock in the base gives at
most 256 locks for each time that a request takes to be answered; that is the first point for the
build session.

## Design Decisions

- **Subindexing.** Does not apply: the door declares no PState. The one long collection that it
  reads is the range of a request's locks, which the module holds and bounds at 256
  (`record/rows.clj:284-301`).
- **Colocation.** Does not apply to the door's own data: it places none. What it rests on is the
  module's: a streaming act, its answer and its locks are on the layer's task; a request for the
  base arrives on the task where its answer and its locks will be (`module.clj:48-52`). So the
  three round trips of a request go to one task.

The door's own choices are each in their section, with what the other answers cost. In one list:

| Choice | The pick | Where |
|---|---|---|
| Placement | `door.clj` at the top, as the contract places it | "Where the code sits" |
| The pure parts | Split into three namespaces under `door/`; the pure proof takes them | "Where the code sits" |
| A call for a series of ids | `act-ids-for`: one read for the series | "The calls of the door" |
| A count of the stock | `stock-count`: three numbers, no bytes (P22) | "The calls of the door" |
| The stock's key | `[layer depot who]` | "The stock's key" |
| How many locks at once | As many as the stock has given out; 4 at least; 64 or 256 at most | "How many locks a request asks for" |
| Makers at once | One call asks for one stock; the others wait for it inside the door's bound | "What holds between makers" |
| Whose refusal | Only of a request that the maker's own call sent | the same |
| The door's bound (P8) | 10,000 ms; the opener may give another | the same |
| A request whose append threw | Kept as in doubt for the door's bound, counted from the throw (ruling 12) | "The life of one request" |
| The first read of an own request in the base | After one interval | the same |
| The order of taking | Oldest run first, across a border | "How the locks of one act are taken" |
| The order of the steps of a send | Every test before the first round trip; the locks last before the seal | "The way of one act in" |
| The limit of sealed bytes | Tested on the plain bytes before a lock is taken, and on the sealed bytes after | the same |
| The ack as a word | `:gate-ran`, `:depot-holds` | "Topologies and PStates" |
| The wait | A read each 200 ms; the last read at the bound | "The wait" |
| The words | Fourteen; the place as data under `:at` | "The door's own words" |
| The id's uuid | The JVM's random UUID | "Design difficulty log" |

## State primitive selection

The template asks, for each piece of state: a PState, a task global, or an outside system. The door
is a client and can declare neither of the first two. Its state is one thing.

- **The stocks (the memory of one process).** Not durable, by the contract: "It owns no durable
  state. Its one memory is a stock of locks (part 3), the memory of one process" (C part 1); "a
  restart asks again" (C D3). What is lost with it cannot be read again: the door reads locks only of
  requests that it made itself in this process (C D6), and it does not know the requests of a
  process before. So the state is not "rebuilt from durable state"; the contract tolerates its loss
  and names the cost: "a row that seals nothing, for good". "Resource usage analysis" bounds that
  cost.
- **Nothing else.** No act-id, no digest and no thing after a call (C D2; C part 5). No layer's depot
  between two calls: that would be a remembered copy of what the cluster holds (IS O2.6; `CLAUDE.md`,
  "Systemic repair and caching"). The five handles are borrowed objects of Rama's client, not state
  of the door's.

**Could the stock be smaller by reading at need?** One answer was built: the stock keeps of each run
only the request's act-id and the next index, and the door reads the bytes of an act's locks from
the cluster when it seals, with one call of `locks-of-request` for the range. The memory of a stock
falls to a few words. The cost: one round trip and one seek more for each act with a value, on the
layer's own task, and a value then waits on the store before it is sealed. The reason for the stock
is that "sealing a value does not wait on the store" (`BUILD-store-module.md`, item 7; IS O3,
Latency). Dropped on that cost. What a restart loses is the same under both.

## Resource usage analysis

### The memory of the door's process

The template's "memory of task globals" becomes the memory of the stocks. By arithmetic on the JVM's
object sizes, not by a measure.

- **One stock with no lock**: its key (a vector of three ids that the act already holds), its seat,
  its value with two counts, and its entry in the map: about 300 bytes.
- **One unused lock**: the array of 32 bytes as the read gave it, 48 bytes with its header, and its
  place in the run's queue: about 60 bytes. The way of a run's locks is kept once for the run when
  they are all of one way.
- **One stock at its most**: fewer than 64 unused locks in a streaming layer, about 4 KB; fewer than
  256 in a microbatch layer, about 16 KB.
- **One open request**: an act-id, 64 characters of digest, a count and a state: about 250 bytes.

**What bounds the whole.** The number of stocks is the number of triples of a layer, a depot and a
who for which a maker sent a value act through this process. The contract gives it no bound (IS O1,
Data growth). The plan's answer:

- A stock that holds nothing at all is taken out of the map when the call that used it leaves: no
  run, no open request, and nothing given out yet. That is the stock of a who whose every request
  was refused. So a maker that sends value acts under whos that do not stand grows no memory in the
  door. (Each such try still leaves a refused request in the depot and in the record; that cost is
  P9's, and the docstring says it.) A call counts itself into its stock when it gets it and out when
  it leaves, each in one atomic step of the map, and a stock goes only when no call is counted in:
  so no call ever holds a stock that the map has let go. Whether the stock holds nothing is read
  inside that same step, after the count has come to none, and never before it: a call that read
  "nothing" first and counted itself out later could take away a stock that another call had filled
  in between, and its locks would be lost with no word and no count (the check of the plan, F13).
  A stock that holds an open request is not such a stock: it stays until a later call of the same
  layer, depot and who reads that request or forgets it. So a who whose request was left open and
  who never comes back leaves about 550 bytes for the life of the process, and a `:who` that is
  equal to no other thing, a Java object, leaves that for each call whose request's append threw
  (F11).
- A stock that has given out a lock stays for the life of the process: its count of what it gave
  out is what the next request is sized by. So the memory grows with the whos that wrote a value
  through this door, at about 300 bytes each and 60 bytes for each unused lock: 100,000 such whos
  with 20 unused locks each are about 150 MB.
- Nothing takes a stock with locks away but the end of the process. To take one away sooner would
  free memory and turn its locks into rows that seal nothing at once; the door "never destroys one"
  (C part 3), and to drop one from memory early is the same loss by another name. Not built.

**Garbage.** A send makes, for each value, its normal form, its one text, its plain bytes and its
sealed bytes, and for the thing one walk of the digest (`digest.clj:40-44`). All of it is dead when
the call returns. The door keeps no copy of a lock's bytes and makes none.

### What the door's calls leave in the cluster, for good

Neither depot is ever trimmed (`module.clj:33-37`), and no code of this build deletes a row. So each
of these is for good. The sizes are by arithmetic from the rows' shapes, not measured.

| What | Left by | Size | What bounds it |
|---|---|---|---|
| One depot record for each thing sent | Each `send-act` that reaches its append: an accepted act, a refused one, a repeat | The thing | Nothing in the door. A refused act that a maker sends in a loop leaves one record for each try (P9; the docstring says it) |
| The rows of an accept, and one answer row for each act-id that is decided | The gate | The module's (IS G22, G23) | The module's |
| One request: a depot record, a fact row, an answer row | Each request of the door | About 550 bytes | The size rule: for a who that goes on writing, one request for each 64 values in a streaming layer and for each 256 in the base; 6 requests for its first 128 values |
| One lock row for each lock that a request made | The gate, at the write of an accepted request | About 100 bytes unused; about 140 in a streaming layer and 245 in the base once it seals (IS G23) | n for each request, 256 at most |

**The rows that seal nothing**, by their source (IS O3, Data growth), with what bounds each:

1. **A stock that a restart loses** (C part 3 names it). A stock holds fewer unused locks than its
   last request asked for: fewer than 4 for a who that wrote up to 4 values, fewer than it wrote
   after that, and at most 63 in a streaming layer and 255 in a microbatch layer when no act needs
   more. In the one state that ruling 11 leaves outside that sentence, it holds fewer than the act that
   came short needed, 255 at most in either depot (F3). The sum over all stocks is what one restart leaves. `stock-count` gives that sum before the
   restart, and the door's proof counts the same rows in the record after it.
2. **A request that was accepted and whose locks the door never read.** One stock has at most one
   open request, so a restart loses at most one request's locks for each stock, 256 at most. While
   the process lives, an open request is read at the next need of its stock (P8), and the only one
   that is given up is a request whose append threw and that had no answer at a read that began the
   door's bound or more after the throw (ruling 12; the check of the plan, F2).
3. **A lock that was taken for an act that is not accepted**: the gate refused the act, the thing
   arrived second under its act-id, or its append threw (P7). One row for each value of each such
   act. Nothing in the door bounds it: a maker that sends a refused value act in a loop loses one
   lock for each value of each try. It is the third row that the build proposes for the queue, and it
   goes to the receipt with what would change it: a door that remembers which locks an act took until
   its answer is read.
4. **A request that a maker sends as an act of its own**, with a fact under
   `:softland.store/lock-request`. The door checks nothing (C D1): the fact is under a store key and
   goes plain; a gate that accepts it makes its locks; no door reads them (C D6). Nothing bounds it.

**Who counts** (P22). The door's proof, from the record, round the door: at its end it counts the
lock rows of the streaming record and of the base that seal nothing, and prints the count with its
sources. That printed line goes to the receipt (C part 3: "the receipt counts the rows it left").

### Minimization

- *Can the stock's data be fetched at need instead of held?* Built and dropped, under "State
  primitive selection": one round trip more for each act with a value.
- *Can a lock be held in fewer bytes?* The 32 bytes are the secret itself. One array for a whole
  run would save the 16 bytes of each array's header and would cost a copy of each lock's bytes at
  each seal, a second home for a secret until the collector runs. The plan keeps the arrays that the
  read gave and copies nothing.
- *Does the door hold anything twice?* No. A lock is in one run, or with the one call that seals
  with it.

## What holds when something fails

The template's "retry and fault tolerance" asks of a module what holds when an event runs twice. For
the door: what holds when an append or a query throws, when the process restarts, and when many
makers call at once. Each line says how far the claim reaches.

| What happens | What holds | What does not |
|---|---|---|
| **The act's append throws** | The maker gets `:append-cleanly` with the act-id and the digest, and reads the record (P1). The door sends nothing again. The locks of the act are out of the stock. | The door does not know whether the depot holds the thing. A maker that sends the act again under the same act-id has sent a second, different thing when the act has a value; at most one of the two is decided (IS G8), and the other digest reads "not mine". Under a new act-id both can be accepted: the act is then twice in the record. |
| **The request's append throws** | Nothing is appended for the act. The stock keeps the request as in doubt, with the moment of the throw; a later call that needs locks of that stock reads its answer first, and sends its own request only when that answer is no accept, or when a read finds nothing the door's bound or more after the throw (ruling 12). | If the request never went in, a call that comes within the door's bound of the throw waits for nothing until that bound has passed. If the request goes in after it was forgotten, its locks are rows that seal nothing. |
| **A query throws** | The call ends in a word that says which read it was. No state changes, but that an open request stays open. The door does not take "the read failed" for "nothing yet" or for "not made" (P10, P21). | The door reads nothing again on its own inside that call. |
| **A gate runs an event or a batch again** | The module's: check (a) answers a repeat from the record, and a request sent once makes its locks once (IS G8, G14). In the base a read gives a row only of a closed batch, so the door never reads lock bytes that a second attempt would change (IS G17). | Not reached by any proof on the in-process cluster (`test-store/softland/store/README.md:285-291`). |
| **The door's process ends** | Every act that is on a depot is decided all the same. A maker that kept its act-id and its digest reads the answer through a door opened later (IS O5.11). | The stocks are gone, and each open request with them. The rows that this leaves are bounded as above. The door promises nothing across a restart (IS O6.4). |
| **The module is started again under an opened door** | On the in-process cluster the same handles read the same answer and the same lock bytes after Rama's module update, and a lock asked for before seals after (IS G25). The door's proof shows it through the door. | A real cluster: not shown anywhere. A finding of the rig's notes is that a module update drains and is no failover (IS, "What could not be settled", 7). |
| **Many makers at once** | No lock goes to two facts or to two acts: a take is one compare-and-set. One request at a time for one stock. A maker that waits for the seat never waits past the door's bound on a request that is not its own; the call in the seat can outlast its bound by one call of Rama's client that is on its way ("How far the door's bound reaches"). A maker is handed the refusal only of its own request. `act-id-for` and `answer-of` share nothing and never wait on another call. | Two sends in flight at once have no order that the door promises (IS O4, Concurrency). |
| **A thread is interrupted while it waits** | The wait ends as if its time had passed, and the thread's mark is set again. A seat is always given up when a call leaves it, whatever ended the call. A thread that is interrupted before or inside a call of Rama's client: the call throws `InterruptedException` at once and the thread's mark is cleared (*ran* by the check of the plan, F8: `out-interrupt-2.txt`). The door takes it as any throw of that call, so the word names the class, and it sets the thread's mark again, as it does after a wait. | An append on an interrupted thread threw and went through all the same: the log held the record and its answer was readable later (the same run). So `:append-cleanly` and a request in doubt are the right words for it. |
| **A thing that Rama's client cannot turn into bytes** | *Ran*, probe 2: a thing that holds a plain Java object in a part that the form does not know made `foreign-append!` throw `CallbackException` after 14 ms, and no answer was readable under its act-id; the same handle took the next append. Through the door that is `:append-cleanly`. A thing that holds a function or a ratio there was appended, and refused `:unknown-part`. | What a worker of another process does with bytes of a Java type that it does not have cannot be shown on the in-process cluster (the build session's line after P22). For the receipt. |

**What the door can cost when it checks nothing** (P9; the docstring says each): a refused act sent
in a loop leaves one depot record for each try, and with an empty stock a refused request too; an act
of 256 values of 65,508 bytes each is sealed and appended whole; facts with no end and fewer than 257
values to seal do not come back. The door as built is no guard of a gate.

## What the plan found out

Four probes, run from the repository root with the probe directory on the classpath as an alias of
the command line. Their files and their whole outputs are under
`/mnt/data/projects/research/softland/door-build-2026-10-09/plan-probes/` (`src/probe/`, `out-*.txt`).
Nothing of them is in the repository.

    clojure -Sdeps '{:aliases {:probe {:extra-paths ["<the probe directory>/src"]}}}' -M:store-module:probe -m probe.api
    (the same with probe.cluster, probe.defaults and probe.dyn)

**Probe 1, `probe.api`: what Rama's Clojure client is made of.** Each client call that the door uses
is a function and no macro: "VAR #'com.rpl.rama/foreign-append! fn? true macro? false arglists
([depot data] [depot data ack-level])", and the like for `foreign-depot`, `foreign-query` and
`foreign-invoke-query`. So a proof can stand in for one of them. And: "OPS-UUID ()": the namespace
`com.rpl.rama.ops` of Rama 1.6.0 holds no var with "uuid" in its name.

**Probe 2, `probe.cluster`: the module `Store` on the in-process cluster, four tasks and two
workers.** One run. The lines that the design rests on:

- "open a depot on a wrong module name: 2 ms :threw org.apache.thrift.TException: Module not alive:
  no.such/Module". The word `:find-the-module` has a real case, at once.
- "open the five handles: 3 ms :ok".
- "append of the making act, :ack: 66.6 ms :ok returned {} answer :accepted". The ack carries
  nothing, as the gate's docstring says.
- "depot-of-layer, 30 calls, median ms: 0.57"; "request for 64 locks: append :ack 14.9 ms, answer
  read 1.9 ms :accepted , locks read 12.8 ms, 64 locks"; "40 acts of one sealed value: append :ack
  median 7.09 ms, answer read median 1.07 ms, verdicts {:accepted 40} mine {true 40}". Orders of
  magnitude only.
- The line on 16 threads, quoted under "Topologies and PStates".
- "append of a thing that holds a plain Java object : 14 ms :threw
  rpl.rama.distributed.exceptions.CallbackException: Callback failure [...] | answer after: nil", and
  the same for an atom in 4 ms. "append of a thing that holds a function : 10 ms :ok {} | answer
  after: {:verdict :refused, :reason :unknown-part}", and the same for a ratio. "an append on the
  same handle after those: 4.8 ms :ok {:verdict :accepted, :reason nil}". So **a real throw of Rama's
  client can be made on the in-process cluster with no fault of the cluster** (IS, "What could not be
  settled", 2 and 3): by a thing that holds a plain Java object. P13 wants one for the word of P1.
- "with-redefs on foreign-invoke-query, a call of four arguments made to throw:
  [\"clojure.lang.ExceptionInfo: made to throw\" nil]": the call with four arguments threw, and the
  call with two went through to the cluster in the same form.
- "base: request 0 : append 5.6 ms; answer readable after 752 ms in 37 reads: :accepted ; locks read
  6.0 ms, 8 locks", and four more: 914, 979, 911 and 929 ms, in 45 to 48 reads at 20 ms. The base's
  making act: 1,034 ms in 49 reads.
- "slow query, 120 s: returned after 70002 ms :threw
  rpl.rama.distributed.exceptions.QueryTopologyInvokeFailed". A query of a probe module of this
  session's own, which sleeps; Rama's defaults.
- After `destroy-module!`: "answer-by-act-id : 2 ms :threw [...] QueryTopologyDoesNotExist"; "append
  to the streaming depot, :ack : 10000 ms :threw [...] CallbackException", and the same for the
  microbatch depot. And then the close of the cluster itself threw, and the probe ended with a
  status that is not zero. So **the door's proof does not destroy the module** to make a call throw.

**Probe 3, `probe.defaults`: Rama's own defaults, read from the jar.** In the table `foreign-spec`:
"foreign.depot.operation.timeout.millis" with ":default 10000"; "foreign.pstate.operation.timeout.millis"
with ":default 10000"; "topology.query.sync.timeout.frequency.millis" with ":default 60000". The first
agrees with the 10,000 ms of probe 2.

**Probe 4, `probe.dyn`: the default of `topology.query.timeout.millis`.** Not found: the table of
dynamic options in the jar is no plain map, and the probe read no default from it. The number that
the plan has is the one run of probe 2, 70,002 ms.

**What reading settled, and no probe.** The ack level `:append-ack` for the microbatch depot (docs
14:206). That an append that throws may have gone through (docs 14:212, 14:214, 11:297). That a query
with a leading partitioner goes to one task (skill `query-topologies.md:120-126`).

**What is still not settled.** Whether one handle may be used by many threads, by Rama's own word:
the local documentation is silent, and the plan has one run of a probe. What a worker does with bytes
of a type that it does not have. Whether a client's handles stay good when a module is started again
in a real cluster. What a query's time limit is by Rama's own table. Each goes to the receipt.

## The door's own proof

*given*: C part 7; P13; P20; P22; the build session's line after P22 on many threads; IS O6.3.

`test-store/softland/store/door_proof.clj`, namespace `softland.store.door-proof`. One command from
the repository root: `clojure -M:store-module -m softland.store.door-proof`. It starts Rama's
in-process cluster with four tasks and two workers, as the module's proof does, launches `Store`,
opens one door on it, and closes the cluster at the end. It prints each thing that it showed on a
line that starts with `SHOWN`, prints those lines again at the end, and exits with a status that is
not zero when a check fails.

**It sends only through the door.** Each act, each request and each answer of this proof goes
through `open`, `act-id-for`, `act-ids-for`, `send-act` and `answer-of`. The proof makes no act-id,
asks for no lock, seals nothing and appends nothing itself, with one exception that the contract
asks for: for the check "what the door sends for an act is what the hand sends for the same act", it
builds the hand's thing for one act as a value, with the hand's steps written out a second time, and
appends that thing itself.

**What it reads round the door** (P13; the proofs' README names it as a second exception to C D8).
The door gives a maker no thing, no lock id and no lock. So, as the module's proof does:

- the two depots' logs, each record (`foreign-depot-read`): what the door appended;
- the locks of a request, with its own handle of `locks-of-request`: to open a sealed value, and to
  hold every lock's bytes against every record of both logs;
- the lock rows of both records, by a select of the gates' PStates on each task: to count the rows
  that seal nothing;
- the strays of both gates: to show that the door made none.

**Its cases.** Each group says what it shows.

1. **Opening.** A wrong module name gives `:find-the-module`, by a real throw of Rama's client. The
   right name gives a door. The printed text of the door, before and after it holds locks, holds no
   run of a lock's bytes as hex and no handle.
2. **An act with no value, in a streaming layer.** `act-id-for` a layer that is not made gives
   `[layer :streaming uuid]`. The making act through the door gives `{:act-id :digest :ack :gate-ran}`.
   `answer-of` gives accepted, with a stamp. The record in the log is the maker's act with `:act-id`
   and `:depot` and nothing else changed; its digest is the digest that the maker got. After the
   making, `act-id-for` gives the same depot from the module's read. `act-ids-for` 20 gives 20
   different ids and makes one query.
3. **Round trips are counted.** For this group the proof stands in for Rama's two client calls with
   functions that count and then call them. A send with a given act-id and locks in the stock: 1
   append, 0 queries. A send that makes its act-id: 1 query, 1 append. A send that makes its act-id
   and meets a request in a streaming layer: 2 appends, 3 queries. An `answer-of` with no wait: 1 query. A wait of 600 ms
   on an act-id that was never sent: 4 queries, and it comes back after the bound.
4. **Locks and the seal.** A person's first act with a value: one request in the log, for 4 locks,
   under her who, her permission, her session and her version; the act's fact holds `:sealed` and
   `:lock-id` and no `:v`; the lock, read round the door, opens the sealed bytes at
   `[layer entity key]` to the UTF-8 of the one text of the value in its normal form; the answer is
   accepted. As she writes more, the requests in the log ask for 4, 4, 8, 16, 32, 64 and then 64. An
   act with a store-key fact, a retract and a value takes one lock. A fact under a store key goes
   plain.
5. **The door and the hand send the same thing, but for the random parts.** One act with a value, a
   retract and a store-key fact, through the door and by the hand. The two records in the log are
   equal when the uuid of each act-id, the request's uuid in each lock id, and the sealed bytes are
   set aside; each sealed value opens to the same plain bytes; both are accepted.
6. **The digest is the gate's**, for facts given as a vector, as a list and as a lazy sequence
   (P20): the digest that `send-act` gave is the digest in the answer, and `:facts` in the log is a
   vector with each fact at its position.
7. **Things that are no act.** Each row of the table of the implied spec (IS O4). A row that ends in
   a word: the word, and no record more in either log, and no stray more. A row that goes in (7, 8,
   10, 12, 16, 17, 18 with no value over the limit): the gate's recorded refusal, read by
   `answer-of` with the reason word of the table. Row 9, facts with no end that each hold a value:
   the call comes back with `:seal-that-many-values`.
8. **Each of the door's own words**, fourteen cases, each with its `:appended` and with what the
   logs hold after it:
   - `:find-the-module`, `:make-an-act-id`, `:read-the-act`, `:add-its-parts`, `:seal-a-value`,
     `:seal-that-many-values`: from groups 1 and 7.
   - `:have-its-request-accepted`: a who with no permission in a made layer sends a value act. The
     word carries the request's act-id and the gate's `:permission-does-not-stand`. Round the door:
     the request and its refusal are in the record; nothing is under the act's own act-id. Then the
     two outcomes of one faulty act (P5): a who with locks in her stock sends a value act under a
     permission that does not stand; that act goes in, and its refusal is under its own act-id.
   - `:append-its-request-cleanly`: a value act whose `:session` is a plain Java object, with an
     empty stock. The request carries that session as given, and Rama's client throws: a real throw.
     Nothing is appended for the act. The proof then waits for longer than the door's bound, and
     the next send of that who makes one read of the request in doubt, forgets it, asks anew and
     goes in, inside a time that is less than the bound (ruling 12). For this the proof opens a
     second door with a bound of 400 ms. And the other
     branch, which ruling 6 is for (added by the check of the plan, F7): the proof stands in for
     `foreign-append!` with a function that sets the interrupt mark of its own thread and then
     calls Rama, for the request's append alone. Rama's client throws and the request goes through
     all the same. The word; then the next send of that who reads the request's answer and uses
     its locks: one request in the log, and no lock of it is left unread. The line says that the
     proof interrupted its own thread.
   - `:get-locks-within-its-wait`, for an acked request: the microbatch topology is paused with
     Rama's test tool; a value act into the base, through the door with the short bound; the word.
     The topology goes on; the next send of that who uses the same request: the log holds one
     request, not two.
   - `:read-its-requests-answer`, `:read-its-requests-locks`, `:read-the-layers-depot`,
     `:read-the-answer`: **the proof makes the read throw itself, and each line says so.** It stands
     in for `foreign-invoke-query` with a function that throws for a call with that query's count of
     arguments (one for the depot, two for the answer, four for the locks) and calls Rama for each
     other. No real throw of a query was found that the in-process cluster gives without a fault of
     the cluster: the door tests each thing that it passes to a read before it calls. After
     `:read-its-requests-locks`, the next send reads the answer of the same request again and then
     its locks (ruling 13): one request in the log. The short read of an earlier call's request (F7; ruling 11): a send of one value
     whose read of the request's answer is made to throw leaves a request for 4 open; a send of 10
     values of that who then reads it, asks for its own in the same call and goes in. The log holds
     two requests of that who, for 4 and for 10 or more, and the act names both. The word's other
     branch, a read of the call's own request that gives locks that cannot seal: the proof stands in
     for `foreign-invoke-query` with a function that calls Rama and gives each lock back with a way
     that the code does not know, and the line says so; the word; `stock-count` counts none of
     them; the send after it asks anew and goes in.
   - `:append-cleanly`: an act with a part that the form does not know, which holds a plain Java
     object. A real throw of Rama's client. The word carries the act-id and the digest and
     `:appended :not-known`; `answer-of` with that pair gives "nothing yet"; the proof reads the log
     and says whether the record is there. A second case, where the thing is on the depot (F7): an
     act with no value and a given act-id, sent on a thread whose interrupt mark the proof has set.
     Rama's client throws, and the append goes through all the same. The word carries the act-id
     and the digest; round the door, the log holds one record under that act-id, and its digest is
     the word's; with the mark cleared, `answer-of` with the word's pair gives accepted. Without
     this case a door that gave a wrong digest in the word would pass.
9. **The answer back.** The four answers. Accepted and refused carry the stamp, and "not mine"
   carries nothing more. "Nothing yet" for an act-id that was never sent, and for a thing that is no
   act-id with no query made. "Not mine": a second, different act through the door under a used
   act-id; and a maker's own second send of one act with a value, whose second digest is another
   (P11). The stamp is usable: an act that replaces a fact and names the stamp in `:based-on` is
   accepted.
10. **A lock never crosses a who.** Two holders of one layer, and one who in two layers, each send
    value acts through the door. Then, over every record that the door appended in the whole run, in
    both logs: each lock id that an act names is of a request whose `:who` and `:layer` are that
    act's, and whose depot is the act's depot.
11. **Many makers at once.** 16 threads through the one door, for one who in one layer, 25 acts
    each of 1 to 3 values. Every act is accepted. No lock id is named twice over all of them. Each
    act names one request, or two. The requests that the log holds for that who are held to the
    size rule, each by its own count: 4 at least, 64 at most, and no more than the counts of the
    requests before it together, or 4; and they are fewer than the threads' acts by far, not one
    for each thread. Their number is not one fixed number: a request's count follows what the
    stock held when it was sent (F7). Then 8 whos at once, each
    on 2 threads.
12. **The base, through the door.** The making act by the system: `:depot-holds`, and a wait that
    gives accepted. A value act: the request is sent inside the call, and the call comes back after
    the request's batch is closed. Several acts of one who sent from several threads while the
    topology is paused, then it goes on: one request in the log; each answer by a wait. The base's
    batches: 8 acts on 8 entities in one batch, each answer read with a bounded wait.
13. **No lock's bytes are ever appended.** Every lock of every request in both logs is read round
    the door; no record of either log holds a run of bytes that is one of them. For a lock of the
    base that seals a value, `locks-of-request` gives nil for the bytes (`door_reads.clj:20-26`),
    so the proof takes those bytes from the lock's whole row, by its select of
    `$$microbatch-locks` on each task (F7). And no result that
    the door gave the proof in the whole run holds a run of bytes at all.
14. **A restart of the door's memory.** `stock-count` of the first door says how many unused locks
    it holds. A new door is opened and the first is let go. Its first value act for a who of the
    first door sends a new request and names no lock of an old one. Round the door: the lock rows
    that seal nothing are counted in both records, and the count is held against its sources: what
    the first door held, the locks of acts that were not accepted, and the locks of requests that
    were given up. The line with the count is the receipt's.
15. **The module is started again under an opened door**, by Rama's module update with the
    unchanged module. A lock that the door held before seals a value after, and `answer-of` gives
    the same answer for an act-id and a digest from before.

**What the door's proof does not reach** (its README says so): a throw of a query that Rama's client
makes by itself; an append that threw for a fault of the cluster and went through all the same (the
one that the proof reaches is an append on a thread that it interrupted itself); a worker of another process and
a type that it does not have; a real cluster, more than one machine, replication; how fast the door
is; a wait that outlasts its bound by the time limit of a query; a gate that is so slow that an acked
request stays open over many calls.

### The three other proofs, after

| Proof | Today | After | Why |
|---|---|---|---|
| The act's proof | `{:test 8, :pass 150}` | `{:test 8, :pass 150}` | Nothing of `act.clj` changes but two sentences of its docstring. |
| The module's proof | `{:test 22, :pass 227}` | `{:test 22, :pass 227}` | Its docstring changes; no check does (P15). It does not use the door, so the store stays proven without it. |
| The pure proof | `{:test 13, :pass 449}` | `{:test 16, :pass 538}` | It takes the door's three pure namespaces: three groups more, 89 cases, one pass each. |

**The pure proof's three groups**, as cases in the form that the proof has: what the case is about, a
function that calls the code, and what it must give. The count of each line is in brackets.

Group 14, the door's words (11): the table written out a second time as literals, fourteen words
each with its `:appended` (1); no word is a reason word of the act or of the declaration or a word of
a stray (1); a word as the table's function makes it holds `:could-not`, its `:appended` and what was
carried, and no `:answer` (1); a word that the table does not hold is not made (1); the four answers
from what a read gave: accepted with its stamp, refused with its reason and stamp, nil, another
digest, a nil digest with an answer, a nil digest with none (6); the two acks by depot word (1).

Group 15, the door's additions (55): the eighteen rows of the table of things that are no act, 23
cases (rows 11, 13 and 14 are two each, row 18 is three, each other row one); the limit of sealed
bytes at its edge, and `seal/sealed-length` held against what `seal` gives for three sizes (3); an act-id given back, of the
layer, of another layer, not of the shape, and with the other depot word (4); the kinds of fact: each
store key plain, a key in the store's namespace that is no store key sealed, a retract, a nil `:v`,
and one act with all kinds (5); the order of the steps, three things with two faults each (3); the
thing that is built: for a vector, a list and a lazy sequence (3), a sealed fact's parts (1), the
sealed bytes open to the plain bytes at the place (1), `declaration/check` takes it (1),
`stray-reason` gives nil for each thing that passes (1), the maker's parts are untouched (1), the
digest (1); the request: its parts as given and an absent part absent (1), its one fact, and
`declaration/check` takes it at 1 and at 256 (2); nothing throws for facts that throw when they are
read (2); no lock's bytes are in the thing (1); the count of a series (2).

Group 16, the stock (23): an empty stock (2); a take from one run (1); two takes share no lock (1); a
take across a border (1); a take that is short changes nothing (1); how many a request asks for,
eight cases of the rule (8); over 200 acts of 1 to 5 values the unused locks are fewer than the last
request asked for (1); what goes into a run (2); a lock that was taken is in the stock no more (1);
the open request: written in doubt with its moment, made *sent*, forgotten (3); the count of what was given out (1); `stock-count`'s numbers
print no bytes (1).

If the build's list differs from these 89, its receipt says by which cases.

## The explanations

*given*: `src-store/CLAUDE.md` binds every file. "Each sentence that the door makes untrue is changed
in the commit that makes it untrue, in `src-store/` and in `test-store/`" (P15). "Replace old wording;
do not add a history of corrections" (`src-store/CLAUDE.md`). No code of the gates, the record or the
declaration changes: each change below in one of their files is a sentence of a docstring. One file
of the folder `door/` gains code: `seal.clj` gains the function `sealed-length` (ruling 5 on this
plan). It is none of the three that the contract closes, no gate calls it, and no line of that file
that a gate runs changes.

### The new explanations

- **The namespace docstring of `softland.store.door`**: its role; the six calls with what each takes
  and gives; the state that it owns (the stocks, the memory of one process) and borrows (the cluster
  handle and the five handles); that a process opens one door; the two things that it requires
  outside `door/` and why (P6); that it cannot check that the sender is the who that the act names,
  and does not (C part 6); what goes plain into a depot through it (P19); what it can cost when it
  checks nothing (P9); the room that it leaves, each by the row of the queue: a layer whose depot
  changes (?9), the switch of a layer to one lock for each act and forget (K11, H10, H11), sign-in in
  front of it (K7 to K10, Q16), who may act as the system (K9), where a read's steps run (W3), what
  tells a screen that a fact changed (W4).
- **The docstring of each function**, private ones too. Of `send-act`: the steps and their order;
  the two outcomes of one faulty act (P5); that two sends one after the other reach the depot in
  that order, and what a maker of the base does when it needs the order; facts with no end. Of
  `answer-of`: the four answers; what "nothing yet" does not mean; how a maker's own second send
  reads (P11); how far the bound reaches, with the setting's name. Of the stock's size rule, of the
  door's bound and of the interval of the wait: each number with its reason (C part 3; P8).
- **The namespace docstrings of `additions`, `stock` and `words`**, as the tree writes them: inputs
  and results by call, state and effects, callers, what is not here.
- **`door/README.md`**, the door's README (C D7). It says where the door sits and what the folder
  gives it, holds a row for each of its seven files, and holds the three sections that the contract
  names: "Decisions this build stands on" (the four of 9 October by row, the rest by pointer to
  `src-store/README.md`); "Engineering choices" (the placement, the stock, the wait, the words, how
  each of D1 to D8 is met); "Words a session made, Sid's to change". It lists no function.
- **The proofs' README**: a row and a section for the door's proof: how to run it, what a pass
  shows, what state it changes, what it does not reach. Under it, the two exceptions to C D8: the
  module's proof, whose hand appends without the door; and the door's proof, which reads the logs
  and the record round the door (P13), and appends one request and one act by hand, for the one
  check that holds the door against the hand (F6).

### The sentences that the door makes untrue

| # | Where | What it says today | What it must say |
|---|---|---|---|
| 1 | `src-store/softland/store/act.clj:59` | "The door is not built; the gate is in its own namespaces beside this one." | The door is `softland.store.door`; the gate is in its own namespaces beside this one. |
| 2 | `act.clj:41-46`, "Callers" | The proof's hand, which plays the door, asks `normal-value`. | The same, and: the door's additions ask `value?` and `normal-value`. |
| 3 | `src-store/softland/store/README.md:17-18` | "The door is not built. The module's proof plays it by hand." | The door is `door.clj`: it takes a maker's act into a depot and reads its answer back. The module's proof still plays it by hand, so that the store stays proven without the door. |
| 4 | the store's map, `:20-34`, "The parts" | The row of `door/` ends "The door itself is not built." There is no row for the door. | A row for `door.clj`: its role, and its state, the stocks, the memory of one process. The row of `door/`: what arrives on a depot, and the door's own pure parts. The sentence on what loads with Clojure alone names `door.clj` among what requires Rama. |
| 5 | the store's map, `:162-164`, "Who reads a lock" | "Any caller of the module can read a lock's bytes. The door proper is the guard, later." | Any client of the cluster can read a lock's bytes. In the runtime the door is the one client; it reads only the locks of its own requests and gives a maker no lock. A guard of the record is not built (?11 is open). (P12) |
| 6 | the store's map, `:192-196`, an act with no permission and the microbatch gate | "The door proper is the guard, later." | The door as built is no guard of it: it checks nothing of an act. As it falls out, a value act of such a who stops at its request when its stock is empty; an act with no value goes through. (P9) |
| 7 | the store's map, `:197-207`, how long one act holds a task | "Nothing is built against it [...] The door proper is the guard, later." | Nothing is built against it. The door's own acts name the locks of one request, or of two, in runs of neighbours; what is appended without the door is not held by it. (P9) |
| 8 | the store's map, `:552-561`, "Folders and namespaces" | Four namespaces under `door`; "Two are at the top with no folder: `act` and `module`"; "The two proofs are `pure-proof` and `module-proof`." | Seven under `door`; three at the top, `act`, `module` and `door`; the proofs are `act-proof`, `pure-proof`, `module-proof` and `door-proof`. |
| 9 | `src-store/README.md:3-9` and its table of questions | The tree holds the act and the store's module. | And the door. A row: "How does a maker's act get into the store, and how is its answer read?", to `door.clj`. The state: the door's stocks are the memory of one process. The line on `:store-module` names the door and its proof. |
| 10 | `src-store/softland/README.md:5-6` | "the act, and the store's Rama module with its two gates and their records" | And the door. |
| 11 | `src-store/softland/store/door/README.md:5-9` | "The door itself is not built; the module's proof plays it by hand." | Rewritten as the door's README, as above. |
| 12 | `door/digest.clj:5-10` and `:62-68` | "The sender compares it with the digest of what it sent"; "The door's comparison. Takes [...] the thing that the door sent." | The door gives its maker the digest as hex text when it sends, and compares that text with the answer's: it keeps no thing. `is-digest-of?` is the comparison of a sender that still holds its thing, as the module's proof's hand does. |
| 13 | `door/seal.clj:51-52` | "Which plain bytes stand for a value: the door's. The proof's hand seals the UTF-8 of the one text of the value." | The door's: `softland.store.door.additions` seals the UTF-8 of the one text of the value in its normal form, and the proof's hand does the same. And the docstring's list of calls gains `sealed-length`, with what it takes and gives, and that the door asks it before it takes a lock (ruling 5 on this plan). |
| 14 | `door/declaration.clj:50-52`, "Callers" | The decision, the rows and the two gates. | And the door's additions and its stock, for the tests of an id and of an act-id, the store keys and the limits. |
| 15 | `module/door_reads.clj:13-15` | "The sender compares the digest with the digest of what it sent (`is-digest-of?`)" | The door gave its maker the digest of what it sent, and compares the two hex texts. |
| 16 | `module/door_reads.clj:73-76` | "Any caller of the module can read a lock's bytes in this build; the door proper is the guard, later." | As row 5. (P12) |
| 17 | `record/rows.clj:83-85`, "Callers" | "The proof's hand calls `routing-id` and the `-read` functions [...]" | The door and the proof's hand call them. |
| 18 | `test-store/README.md:10`, `:12-18`, `:20-27`, `:33-37` | Three commands; "The third needs Rama"; "No door is built; the third proof plays it by hand." | Four commands. The third and the fourth need Rama. The third plays the door by hand; the fourth goes through the door. |
| 19 | `test-store/softland/README.md:5-7` | Three proofs. | And the door's proof. |
| 20 | `test-store/softland/store/README.md:5-9` and the pure proof's section | The pure proof's row names the gates' pure namespaces; its section has 13 groups. | The row names the door's three pure namespaces too; groups 14 to 16; what they do not reach. The module's proof's row stays as it is (C "Explanations"). |
| 21 | `test-store/softland/store/module_proof.clj:12` | "The door is not built. The proof plays it by hand [...]" | The proof plays the door by hand and does not use `softland.store.door`: the store stays proven without the door, and the hand stays the proof that anything can append to a depot without it. No check changes. |
| 22 | `test-store/softland/store/pure_proof.clj:2-5`, `:33-34` | "The proof of the gates' pure namespaces: [...]" | And the door's pure namespaces. |
| 23 | the store's map, `:1` and `:5-9` (added by the check of the plan, F6, as are rows 24 and 25) | The title "Store — the act, and the store's module"; "This folder holds two things. The act [...] And the store's Rama module [...]" | Three things: the act, the store's module, and the door. |
| 24 | `test-store/softland/store/README.md:59`, under "What the act's proof does not reach" | "the use of the act by a gate: the two proofs below show it." | The pure proof and the module's proof show it. With the door's proof there are three proofs below. |
| 25 | `test-store/softland/store/pure_proof.clj:290` and `:308` | "the door's comparison" in the docstring of the digest's cases, and in the name of one case, of `is-digest-of?` | The comparison of a sender that still holds its thing, as row 12 has it. The case's name is a text; no check changes and the count stays. |

Three sentences stay true and are not changed: "It plays the door by hand" in the row of the
module's proof; "reads the answer by act-id in a loop with a time limit, as a door would"
(`module_proof.clj:24-26`); "a caller that may hold a thing that is no id tests it with the same
functions before it calls" (`door_reads.clj:69-71`), which the door now does.

C "Explanations" names `module/README.md` for the line on who reads a lock. That file holds no such
line; the line is in rows 5 and 16 (P12; for the receipt).

## What this part does not build and does not decide

Each stays open. Where the door's code leaves room for one, its docstring says so.

- A first maker: no converter, no screen, no agent. The proof is the only maker.
- Who may act as the system (K9): `:system` is a who like any other.
- Sign-in: persons and their ids, the links to their accounts, sessions (K7, K8, K10, Q16, E5, E6,
  Q19). The door copies `:session` into its request as given, and makes no id of its own. One thing
  follows that those rows may touch: a lock asked for in one session can seal a value of the same
  who in another (P4).
- Reads of facts for a screen, resolve, context (group 6, W1 to W8); what tells a screen that a fact
  changed (W4); where a read's steps run (W3). The wait is one question about one act-id.
- The switch of a layer to one lock for each act; forget, and what the store hands to the one who
  forgets (K11, H10, H11). The stock's key keeps a lock to one who in one layer, which is the room
  that the cutter's rule keeps.
- A network edge. The door is in-process Clojure.
- Re-class (?9), promotion (?10), what a key is (K5, K15), a record that only the gates can read
  (?11).
- Any change to the gates, the record or the declaration. This plan found no place where the
  module's surface is not enough for the door. It found one cost of the surface, the read of a
  layer's depot for each act-id, which is the second point below.
- The four rows that the build proposes for the queue (IS, last section): who holds the form of a
  value once it is sealed; whether the door runs the maker's check before it seals; whether a lock
  of an act that was not accepted comes back; what guards a gate from an act that costs much. This
  plan builds to the rulings and decides none.

## What the build hands to the receipt

From C "Commits and the receipt", from each line of the rulings that is marked "For the receipt", and
from this plan.

0. The commits (C "Commits and the receipt"; added by the check of the plan, F9): on `main`, grouped
   by concern, each message saying what and why; no push; no `Co-Authored-By`, no `Claude-Session`
   and no other attribution line. The contract names three groups, "the door; its proof; the
   maps", and P15 has each sentence that the door makes untrue changed "in the commit that makes it
   untrue". By ruling 14: two commits of the tree, the door with each sentence that it makes untrue,
   and its proof with the proofs' maps; then the build's papers. The receipt says that the
   contract's third group, the maps, went into the first two. The build stage commits nothing; the
   build session makes the commits.
1. The line "proven by a hand-played maker, used by no inhabitant yet"; the commit hashes; the four
   proofs' outputs in full; the count lines before (act 150, pure 449, module 227) and after (act
   150, pure 538 by this plan, module 227, and the door's proof's own line); `git status --short`.
2. Where the contract is not met as written: "it appends nothing for that act", of `:append-cleanly`
   (P1); "a malformed act goes in and comes back as the gate's refusal", of a thing that would be a
   stray and of a fault that the door's additions would take away (P2, P3); "a layer not made and no
   depot to start with" (P14); "reads `:layer` and `:facts` and no more" (P4); the line on who reads
   a lock, and the three sentences that end "the door proper is the guard, later" (P9, P12); the
   second exception to C D8 (P13); the docstring of the module's proof (P15); what goes plain into a
   depot through the door (P19).
3. Each provisional choice: the placement and the split; the two calls that the contract does not
   name, `act-ids-for` and `stock-count`; the stock's key, its size rule and its three numbers; one
   request at a time for one stock; the door's bound; the wait's interval; the fourteen words and
   the words of the results; a request whose append threw is kept for the door's bound, counted
   from the throw (ruling 12); that a call sends one request of its own at most (ruling 11); the one
   function that `seal.clj` gained (ruling 5).
3a. What the stock costs the base (ruling 1): one who gets at most 256 locks for each time that a
   request's batch takes to close, and each send that finds its stock short waits that time; with
   what would lift it. And that the door's memory has no bound but the life of its process.
3b. The read of a layer's depot for each act-id (ruling 2): its cost, that no copy is built, and
   that it goes to the examination of its own that the project's rule on caching asks for.
3c. The most at once of a streaming layer, 64, against the aim for one person (ruling 16): 1 send
   in 64 of a who that goes on writing one value at a time asks for locks, three round trips more,
   so counted from the maker's call the slowest 1 in 100 sends is a send that asks; at 128 it is
   not. The store's map counts the aim from the door's append, after the request. Not measured.
   With it, a row for the queue, decided by nobody: from where the aim for one person is counted
   once a door is built, the maker's call or the door's append.
3d. The door's first bound of P9 is the smaller of two limits of the declaration, which are the
   same number today (ruling 17). The test of a plain map is written in six places of the tree
   (ruling 10). The word "stock" (ruling 15).
4. The rows that seal nothing: the count that the door's proof prints, with its sources (P7, P22),
   and what would change the third source.
5. What was not settled: one handle and many threads, by Rama's own word; a worker and a type that
   it does not have; a module started again in a real cluster; the time limit of a query.
6. The rows for the queue, decided by nobody: the four of the implied spec, and what the build
   session makes of the points below.

## Words a session made, Sid's to change

Each says what the thing is. None comes from the rig's code. Carried and not made here: `:act-id`,
`:depot`, `:sealed`, `:lock-id`, `:digest`, `:stamp`, `:reason`, `:accepted`, `:refused`; "stock" (C
part 1); `act-id-for` and `answer-of` (C parts 2 and 5); "nothing yet" and "not mine" (C part 5).

**Namespaces and files.**

| Word | What it is |
|---|---|
| `softland.store.door.additions` | The pure code of what the door adds to one act: the act-id's tests, the walk over the facts, the seal of each value, the thing to append, the request for locks. "Additions" is the contract's word for the three things that the door adds. |
| `softland.store.door.stock` | The pure code of the stock as a value. |
| `softland.store.door.words` | Everything that the door says to a maker, declared as data. |
| `softland.store.door-proof` | The door's own proof. |

**Calls.**

| Word | What it is |
|---|---|
| `open` | Opens the door on a cluster handle and the module's name. |
| `act-ids-for` | The act-ids of a series in one layer, with one read. |
| `send-act` | Takes a maker's act into its depot. |
| `stock-count` | How many unused locks the door holds, in how many stocks, and how many requests it has not read. |

**The door's five numbers.**

| Word | What it is |
|---|---|
| `:request-wait-ms` | How long one call waits for locks, in all, before it ends in a word. 10,000. |
| `:read-every-ms` | How long a wait sleeps between two reads of an answer. 200. |
| `:fewest-at-once` | The fewest locks that the size rule asks for in one request. 4. An act that needs more gets more. |
| `:most-at-once-streaming`, `:most-at-once-microbatch` | The most that the size rule asks for in one request, by the depot. 64 and 256. |

**What a call gives.**

| Word | What it is |
|---|---|
| `:ack`, with `:gate-ran` and `:depot-holds` | The signal of the append. The streaming gate has processed the thing; or the microbatch depot holds it. Never the answer. |
| `:answer`, with `:accepted`, `:refused`, `:nothing-yet`, `:not-mine` | What `answer-of` gives. The last two are the contract's phrases as keywords. |
| `:could-not` | The key of one of the door's own words. |
| `:appended`, with `:nothing` and `:not-known` | Whether nothing was appended for the act, or the door does not know whether the depot holds it. |
| `:at` | The place in the maker's thing where the door stopped, by the thing's own part names. |
| `:most`, `:sealed-bytes`, `:waited-ms`, `:module` | Numbers and a name that a word carries. |
| `sealed-length`, in `door/seal.clj` | How many bytes `seal` gives for a count of plain bytes, by the way of sealing (ruling 5; listed by the check of the plan, F14). |
| `:request`, `:request-answer` | The act-id of the door's request, and its answer as `answer-of` gives one. |
| `:threw`, `:said` | The class of what Rama's client threw, and its message. |

**The door's fourteen words.** Each reads after "the door could not": `:find-the-module`,
`:make-an-act-id`, `:read-the-layers-depot`, `:read-the-act`, `:add-its-parts`, `:seal-a-value`,
`:seal-that-many-values`, `:have-its-request-accepted`, `:append-its-request-cleanly`,
`:get-locks-within-its-wait`, `:read-its-requests-answer`, `:read-its-requests-locks`,
`:append-cleanly`, `:read-the-answer`. The table under "The door's own words" says what each is.

**Words of the stock**, in the explanations and in the names of the code.

| Word | What it is |
|---|---|
| a stock | The unused locks that the door holds for one who in one layer and that layer's depot. The door's memory is its stocks. The word is the contract's and the door's sheet's. It is a session's: no line that Sid typed has "stock of locks", and a session wrote it first on 8 October (the record of his lines, searched on 10 October). The rig's code has a function `stock!` (`rig/store/client.clj:358`, found by a search for the word; the file was not read). Whether the session took the word from there is not known. C D7: "No word comes from the rig's code" (ruling 15). |
| a run | The unused locks of one request, in the order of their indexes. |
| open request | A request that the door sent and whose locks it has not read. Its states: sent, in doubt. |
| in doubt | Of a request: its append threw, so the door does not know whether the depot holds it. |
| seat | What lets one call at a time ask for a stock. Never called a lock. |
| given out | Of a stock: how many locks it has handed to acts since the door was opened. |

## Points for the build session

Four need a ruling. Each has its passage, what meets there, the choices, and the plan's pick where
the pick is engineering.

**1. P16 and the base: one stock gives at most 256 locks for each time that a request takes.**
P16: "Each request is sent inside the call of a maker whose act needs locks". IS G17: a request's
answer in the base is readable only when its batch is closed, 0.75 to 0.98 s on the in-process
cluster (1.03 s for the making act). The declaration's ceiling is 256 locks for one request. With one request at a time for one
stock, a who that writes values into the base faster than 256 for each such time waits for locks:
each of its makers stands in `send-act` until the request's batch is closed. The microbatch gate's
aim is 1,000 acts a second arriving on one partition; that aim is over all whos, and many whos have
many stocks, so it is not missed by this. One who that brings a large import into the base is the
case that meets it. The choices:
(a) keep, and say it in the docstring and the receipt;
(b) let a call whose act the stock served also send a request when the stock falls under a mark, so
that the next request is in flight before the stock is empty. That is a request that no act waits
on: P16's other answer, with its cost, that a refusal of it has nobody to be handed to;
(c) more than one request in flight for one stock, each sent by a maker whose act needs locks. The
stock's bound is then times that number.
The plan is built to (a): it is the ruling. The pick, as engineering: (a) for this build. The case
is the bulk layer for which the switch to one lock for each act is kept (R, 9 October, entry 2), and
that switch is not built and not this build's.

**2. The read of a layer's depot for each act-id, and the rule on caching.** C part 2 names the read.
IS O2.6: the door remembers no layer's depot between two calls. What made a remembered copy look
needed: the read is one round trip and one read on the layer's own task for each act that the door
makes an act-id for, on the one thread that the streaming gate needs; and the row that it reads is
written once, since no act of this build changes a depot (IS G16). With the answer's read, an act
costs the layer's task 6 reads where the gate alone costs 4. The plan does not design the copy in.
What it does inside the rule: `act-ids-for` reads once for a series, and a maker that gives its
act-id back pays no read in `send-act`. Whether the door may ever hold a layer's depot is for the
examination that `CLAUDE.md` asks for, in a session of its own, and not for this build.

**3. C part 3's order, "its answer by act-id and then its locks", costs one round trip for each
request.** A request's locks exist only when the request was accepted (IS G14), and in a microbatch
layer the read gives them only when its batch is closed (IS G15). So a read of the locks that gives
locks says "accepted" by itself, and the read of the answer would be needed only when the locks' read
gives none. That is 2 round trips for a request in a streaming layer where the contract's order
makes 3. The saving is one round trip for each 64 values of a who that goes on writing. Against it:
C part 3 and IS O3.4 give the order, and C D4 has the door read the record by act-id and compare the
digests, which the other order would skip for an accepted request. The plan keeps the contract's
order. The pick, as engineering: keep.

**4. IS O4.14's "of as few requests as the stock allows", and the oldest run first.** Under "How the
locks of one act are taken" the plan takes across the border of two runs, so that no rest is left
behind. An act at a border then names two requests where the stock, read to the letter, would allow
one. The other order was built: it costs the gate no fewer seeks over time, and it leaves rests. The
pick, as engineering: oldest first.

Three things for the build session to know, which need no ruling:

- **Four of the fourteen words have no real throw on the in-process cluster**: the three reads and
  the read of a request's answer. The door's proof makes those reads throw itself, by standing in
  for Rama's client call, and says so on each line. P13 asks for a real throw for the word of P1;
  that one, and the request's, have real throws (probe 2).
- **The door's memory has no bound but the life of its process**: about 300 bytes for each who that
  wrote a value through it, and its unused locks. To drop a stock sooner is to lose its locks sooner.
- **The implied spec's edge case on a who's locks under another permission** (IS O4, edge cases)
  holds as written: the stock's key has no permission in it, and the gate judges the act's
  permission.

## Rulings of the build session on the plan

By the build session that took the door's build over (Opus 5.5, effort xhigh), late on 9 October
2026. It read this paper's design whole: the calls, the stock, the way of one act in, the answer and
the wait, the words, what holds when something fails, the proof and the explanations. It checked in
the source the facts that a ruling rests on, and none was false: Rama's words on an append that
throws, on `:ack` after a first attempt that failed, and on the limit of pending records
(`reference/rama/docs/14-depots.md:206`, `212-214`; `11-stream-topologies.md:297`); the one setting
that Rama names for a query's time limit, with no number (`13-query-topologies.md:342`); that a gate
holds a lock to its layer and its depot and reads one range of lock rows for each request that an
act names, from the least index that the act uses to the greatest (`gate/decision.clj:266-290`); and
the lines of the probes that the design rests on, in their outputs under the scratch path (16
threads on one set of handles with no error; the base's requests answered after 752 to 979 ms; a
thing with a plain Java object, thrown by Rama's client after 14 ms; a query that slept, thrown
after 70,002 ms; an append after the module was destroyed, thrown after 10,000 ms). Rama's local
documentation says nothing on the use of one handle by many threads; that was searched for and not
found.

Each ruling is the build session's, a requirement for the check of the plan, the build and the
review, beside the contract and the rulings on P1 to P22. None is a decision of Sid's, and each is
his to strike. Rulings 5 and 8 changed this paper; the passages that they changed name them.

1. **Point 1: (a).** The door asks only at need, one request at a time for one stock. In the base
   one who then gets at most 256 locks for each time that a request's batch takes to close, and each
   send that finds its stock short waits that time. The reasons: the aim of the microbatch gate is
   over all whos, and each who has a stock of its own; a base act's answer takes a batch of the gate
   in any case; and the one who that this holds back, a large import into the base, is the case for
   which Sid kept the switch of a layer to one lock for each act (entry 2 of 9 October), which is
   not built. Choice (b) lifts no limit: it takes away the wait at each later request and not at the
   first, and it adds requests that no act waits on. Choice (c) lifts the limit and gives the stock
   a bound that grows with the makers that wait. Neither is built. For the receipt, with the
   arithmetic and with what would lift it.
2. **Point 2: no copy of a layer's depot is built, and nothing is designed round the read.** The
   cost goes to the receipt as a finding: one round trip and one read on the layer's task for each
   act-id that the door makes, for a row that no act of this build changes. `act-ids-for` and an
   act-id given back are inside the rule, since each reads once inside one call. Whether the door
   may ever hold a layer's depot goes to the examination in a session of its own that the project's
   rule on caching asks for. One thing for that examination, which is the build session's
   observation and no design: a gate already answers a thing that arrives on the depot that is not
   its layer's with a recorded word, `:layer-belongs-to-the-other-gate`
   (`gate/decision.clj:125`, `147`), so the record already tells a sender that the depot that it
   used was wrong.
3. **Point 3: the contract's order, the answer and then the locks.** D4 has the door read the record
   by act-id and compare the digests, and the other order would skip that for an accepted request.
   One round trip for each request is its price.
4. **Point 4: the oldest run first, across the border of two runs.** "Of as few requests as the
   stock allows" (IS O4.14) is read of a stock that leaves no lock behind: the stock is a queue, and
   it allows its front. The reasons: a rest that no later act fits is a row that seals nothing, and
   rests in the door's memory have no bound for a who whose acts all hold the same number of values;
   and the price is one seek more for one act of each request, with each range holding exactly the
   locks that the act names.
5. **The length of a seal is asked of `seal.clj`, by a function that it gains: `sealed-length`.**
   The plan as first written measured the length at the door's first use, with a lock made for
   nothing else. It did so because the brief of the plan stage, which this session wrote, closed
   `seal.clj`, `text.clj` and `digest.clj` to any change of code. That was wider than the contract,
   which closes "the gates, the record or the declaration". The ruling goes back to the contract: a
   rule that the door needs is asked of the file that holds it (the ruling on P6: "No rule is
   written a second time"), and a rule found again by a run is a rule written a second time. The
   function changes nothing that a gate runs, and the module's proof keeps its count. The entry of
   the difficulty log on this stays as the record of the first answer. For the receipt, as a
   provisional choice.
6. **A request whose append threw is kept as in doubt and waited for once more**, as the plan has
   it. (How long it is kept is ruled again in ruling 12, after the check of the plan.) Rama's own words make "it went in" the common case of a thrown `:ack` append, and a door that
   forgets would send more requests into a gate that is behind. The price is one call that waits the
   door's bound for nothing after an append that truly failed. For the receipt, as a provisional
   choice; the plan's writer was least sure of this one.
7. **`door/README.md` keeps two groups of files apart**: the four that the door and the gates agree
   on, which a gate requires; and the three that are the door's own, which no gate requires. The
   folder was "what the door and the gates must agree on", and with the split it holds more than
   that.
8. **A read of a request's locks that came back ends the request.** The plan as first written left
   the request open, as accepted, when the read gave fewer locks that can seal than the act needs. A
   second read can give no more, so a who whose acts all need more than that request gave would get
   the word at each call for the life of the process, and the stock would never ask again. The
   locks that can seal go into the stock, the request is forgotten, and the call ends in the word;
   the next call takes or asks anew. Only a read that threw leaves the request open. (Ruling 11,
   after the check of the plan, lets the same call ask for its own when the request was an earlier
   call's.)

Accepted as the plan has them, with no change: the three pure namespaces under `door/`; the stock's
key with the depot in it; the size rule and its three numbers; the seat; the door's bound of
10,000 ms; the wait's interval of 200 ms; the five numbers as the opener's to give; `act-ids-for`
and `stock-count`; the fourteen words; the door as a value that prints no stock; that a stock which
gave out a lock stays for the life of the process. Each is a provisional choice for the receipt.

For the check of the plan to trace, because this session read them and did not trace them: what
holds between a take, a call that counts itself into a stock and out of it, and the taking of an
empty stock out of the map; and that a seat is given up on every way out of a call.

### Rulings on the check of the plan

By the same build session, 10 October 2026. The check (`PLAN_VALIDATION.md`) gave the verdict
minor-fail: fifteen fails, each fixed in this paper by the check and marked F1 to F15, and ten
points. The build session read every line that the check changed (the diff of this paper against
its commit `088ff5f6`) and takes each of the fifteen fixes as written. The two things that it had
asked the check to trace gave F13 (where "holds nothing" is read) and F8 (a thread interrupted
inside a call of Rama's client); both fixes stand. It checked in the source, for the rulings below:
that a gate refuses a fact under a store key that holds `:sealed` or `:lock-id`, by the rule
`:store-key-fact-has-no-seal` (`declaration.clj:377-380`); that the test of a plain map is private
in `declaration.clj:295-298` and in `gate/batch.clj:80-83`, and written in line three times in
`act.clj` (`:265`, `:332`, `:413`); and in the record of Sid's lines, that "stock of locks" is in
no line that he typed.

9. **Point 1: (a). A fact under a store key goes as given, also when it carries a seal.** Rule A is
   the rule, and the ruling on P3 is drawn from it: the door stops a thing when it would write over
   what the maker gave. To a fact under a store key the door adds no part, so it writes over
   nothing, and a gate sees the fact as it was given and refuses it. The row of a store key is
   tested first.
10. **Point 2: (a). The door writes the test of a plain map once, as a private function of its
    additions.** "No rule is written a second time" (P6) is read of the store's own rules: a limit,
    a key, a way to a task, a number such as the length of a seal (ruling 5). This test is two of
    Clojure's own predicates, and the tree writes it in five places already. One home for it is a
    change to `act.clj`, the declaration and the gates' batch, and the last two are closed to this
    build; a public function in `act.clj` alone would change the code of the one form for a line
    and leave four copies. For the receipt, as a finding.
11. **Point 3: (b), by one rule: a call sends one request of its own at most.** A call that read an
    earlier call's request, and whose stock is still short, has sent nothing yet; it asks for its
    own inside its own wait, as it does after an earlier call's refused request. The word
    `:read-its-requests-locks` stays for a read that threw, and for a call's own request whose
    locks cannot seal. D1 holds the door's words to what the door could not do, and here it could.
    This amends ruling 8, which stays for what it was for: a read that came back ends its request.
12. **Point 4: a request in doubt is kept by its age.** A call forgets it only at a read that finds
    nothing and that began the door's bound or more after the throw. The first answer gave such a
    request what was left of the next call's bound: one read for a call that had stood long for the
    seat, and a whole bound of waiting, for nothing, for a call that came an hour later. By its
    age each such request gets the whole bound and no call waits for one that is older. The clock
    is the one that the waits read. This amends ruling 6, on which the plan's writer was least
    sure; the check's probe gives the ruling its case, an append that threw and went through.
13. **Point 5: (b). The stock keeps no answer that the door read; an open request has two states.**
    After a read of locks that threw, the next call reads the request's answer again and then its
    locks. That is one round trip more on a way that follows a thrown read, and every call then
    reads in the contract's order and compares the digest (D4). The state "accepted" could not go
    stale, and the check passed it; it was still the one thing in the stock that was a read's
    result and no lock, and the project's rule on caching is kept clear of it at that price.
14. **Point 6: (a). Two commits of the tree, and one of the papers.** The door, with each sentence
    that it makes untrue; its proof, with the proofs' maps; then the build's papers.
    `src-store/CLAUDE.md`, which the contract makes binding, and P15 say the same thing against the
    contract's third group, "the maps". For the receipt, as a place where the contract is not met
    as written.
15. **Point 7: "stock" is listed for Sid with what was found.** It is a session's word and not his,
    and the rig's code has a function of that name. The build keeps the contract's word. He may
    give another.
16. **Point 8: the most at once of a streaming layer stays 64, and the point goes to the receipt.**
    No number of the three is measured, and the opener may give another. At 64 the gate's one
    event for a request stayed under the 20 ms of the aim in the plan's probe (14.9 ms for the
    append with `:ack`), and that event holds the layer's task for every act behind it. From where
    the aim for one person is counted, the maker's call or the door's append, is not the build's to
    say: a row for the queue, decided by nobody.
17. **Point 9: the door's first bound of P9 stays the smaller of the two limits.** One request must
    cover one act, because a call sends one request at most (ruling 11). The docstring says that
    the bound is the door's own. For the receipt.
18. **Point 10: the door sets the thread's mark again** after a throw of Rama's client that is an
    `InterruptedException`, as it does after a wait. The door turns the throw into a word, so
    without the mark the maker's thread would not know that it was interrupted.

Checked least well, by the check's own word, and carried to the build and the review: the seat, the
compare-and-set and the count are traced on paper only, since no code exists; one handle on many
threads rests on one probe run; what the microbatch gate does with an act that names the locks of
two requests was read in `gate/batch.clj`'s pure code and not run. The door's proof has a case for
each of the three.

## Where each thing of the split, and each ruling, is met

**The twelve things of `DECOMPOSITION.json`.**

| # | The thing | Where |
|---|---|---|
| 1 | The door's memory of one process | "What an opened door holds, and what it never holds"; "State primitive selection" |
| 2 | The stock, with its three questions | "PState Design", whole: (a) "What holds between makers", (b) "The life of one request", (c) "How the locks of one act are taken" |
| 3 | What the door's calls leave in the cluster | "Resource usage analysis", second half |
| 4 | The way of one act in | "The way of one act in"; "Writes"; "Depots" |
| 5 | The answer back, and the wait | "The answer back, and the wait" |
| 6 | The door's own words | "The door's own words" |
| 7 | What the plan must find out or say | "Partitioning efficiency"; "What the plan found out" |
| 8 | Where the code sits, what it may require, the pure parts | "Where the code sits, and what it may require" |
| 9 | The door's own proof | "The door's own proof" |
| 10 | The explanations | "The explanations" |
| 11 | What is not built and not decided | "What this part does not build and does not decide" |
| 12 | What goes to the receipt | "What the build hands to the receipt" |

**The rulings.**

| Ruling | How the plan meets it | Where |
|---|---|---|
| Rule A | The tests of steps 1 to 4 and 7: a plain map, its own parts free, a layer that an act-id can hold, a given act-id of that layer, a sealed value within the limit | "The way of one act in" |
| Rule B | `act/value?` on each value that the door finds; a value in a map that is no plain map | The walk of the facts |
| P1 | `:append-cleanly` carries the act-id and the digest and says `:not-known`; `:append-its-request-cleanly` says it of the request | "The door's own words"; "The life of one request", D5 |
| P2 | Step 3; a given act-id with the other depot word goes as given | "The way of one act in" |
| P3 | `:add-its-parts`, `:seal-a-value`; a fact with `:sealed` and no value goes as given | The walk of the facts |
| P4 | Step 1 reads the six parts; the request copies four of them as given | "Writes"; step 1 |
| P5 | `:have-its-request-accepted` with the request's act-id and answer; the two outcomes | "The door's own words"; "The way of one act in" |
| P6 | The table of what each namespace requires | "Where the code sits" |
| P7 | A take is for good | "What the stock never does" |
| P8 | The door's bound, 10,000 ms, the opener's to give; the open request is read at the next need | "What holds between makers"; "The life of one request" |
| P9 | The two bounds; what ends the walk; the cost said in the docstring; rows 6 and 7 of the explanations | "The way of one act in"; "What holds when something fails"; "The explanations" |
| P10 | `:read-the-answer`; the stamp; one wait for any act-id | "The answer back, and the wait" |
| P11 | The docstring of `answer-of`; group 9 of the proof | "The answer back, and the wait" |
| P12 | Rows 5 and 16 of the explanations | "The explanations" |
| P13 | What the proof reads round the door; a case for each word; real throws for both appends | "The door's own proof" |
| P14 | `:make-an-act-id` has the case | "The door's own words" |
| P15 | The 25 rows; the module's proof stays at 227 | "The explanations"; "The three other proofs, after" |
| P16 | The door asks only at need, inside a maker's call; no thread | "How many locks a request asks for"; point 1 |
| P17 | `:append-ack` | "Topologies and PStates" |
| P18 | The door's printed text; what a word carries; no copy of a lock's bytes | "The calls of the door"; "The door's own words"; group 13 of the proof |
| P19 | Literal; said in the docstring | The walk of the facts; "The explanations" |
| P20 | The facts are sent as a vector; group 6 of the proof | "The way of one act in" |
| P21 | `:read-the-layers-depot` | Step 5 |
| P22 | No closing; one door for a process; the proof counts the rows; `stock-count` | "What an opened door holds"; "Resource usage analysis"; group 14 of the proof |
| On threads | The documentation is silent; one run of a probe; the proof sends from many threads | "Topologies and PStates"; group 11 |
| On what Rama's client cannot turn into bytes | Probed: it throws, and the handle stays good | "What the plan found out" |
| On a query's time limit | The setting is named; one run gave 70,002 ms | "The wait" |

## Design difficulty log

**How many locks a request asks for.** This was the hardest call, and it changed twice. I began with
a fixed batch of 64, which is what the door's sheet pictures ("asks for more before the stock runs
out"). Counting what a restart leaves for a who that wrote one value, 63 rows, made it look wrong for
the common who. I then built a rule by the clock: ask for more only when the last request was recent.
It worked on paper and gave no bound that I could say in a sentence. What settled it was the
arithmetic of one request against one unused lock, about 550 bytes against about 100: asking for as
many as the stock has given out makes the rows that are left fewer than the values written, with no
clock. The fixed batch stayed competitive only for a who that writes much; there the two are the
same.

**Whether makers wait for each other.** For a streaming layer alone I would have let each maker ask
for itself: it is simpler, and a request is answered in milliseconds. The base forced the other
answer: a second there is long enough for a hundred makers to send a hundred requests. One design
for both depots won over two designs.

**The request whose append threw.** The simple answer, forget it, was my first. Rama's own page on
the limit of pending records changed it: under that limit an `:ack` append throws and has gone
through, and a door that forgets would send more requests into a gate that is already behind. The
cost of keeping it, one call that waits for nothing after a true failure, is real, and I am less
sure of this choice than of the others.

**P16 in the base.** The design wants to ask before the stock is empty there, and the ruling says
no. I built to the ruling and wrote the cost as the first point. I did not design round it.

**The read of the layer's depot.** The cheapest design remembers it. The project's rule and IS O2.6
say no, and I did not reach for it; the cost is written as the second point.

**The limit of sealed bytes without the number.** `seal.clj` keeps its 28 private, and the door may
not change that file's code. Measuring the length once, with a lock made for nothing else, was the
only answer that neither writes the number twice nor loses locks; it is an odd thing to do, and the
pure proof holds it against the seal for three sizes.

**The id's uuid.** The skill says to make ids with `ops/random-uuid7`, which orders by time. Act 5
says "a random UUID", and IS O2.3 that it is made from nothing of the time. Probe 1 then showed that
Rama 1.6.0 has no such function. Forced: the JVM's random UUID, as the hand makes it. No read of this
build ranges over act uuids, so nothing is lost.

**The words.** How many, and whether a word may hold a second word. I settled on one word for one
thing that the door could not do, with the place as data. Fourteen is more than I wanted; each of the
cuts that made fewer made a maker read two words to learn one thing.

Not contested, forced by the sources once read: the ack levels (C part 4, P17); that the door holds
no PState handle; that a lock never comes back (P7); the four answers and the stamp (C part 5, P10);
the vector of facts (P20).

## What was read, and what was run

Read whole, in the order of the brief: the skill's `SKILL.md`, `phase-1-plan.md`, `artifact-plan.md`,
`artifact-plan-validation.md`, `foreign-client.md`, `depot-reference.md`, `testing.md`,
`unique-ids.md`; the contract; `src-store/CLAUDE.md`; `src-store/README.md`;
`decisions/groups/g-door.txt`; `act.clj`; `door/README.md`, `declaration.clj`, `seal.clj`,
`digest.clj`; `module.clj` and `module/door_reads.clj`; the hand, `module_proof.clj:1-503`; the
implied spec, whole, with its rulings; `DECOMPOSITION.json`; `door/PROGRESS.md`; the store's map,
`src-store/softland/store/README.md`; `gate/first_act.clj`; `module/README.md`; the proofs' README;
`test-store/README.md`; the two small namespace-root READMEs.

Read in part, for the lines that a claim needed: `door/text.clj:1-70`; `record/rows.clj:1-110` and
`225-344`; `gate/decision.clj:262-336`; `gate/batch.clj:85-97`; the skill's `core-concepts.md` and
`query-topologies.md`, by search; `store-module/PROGRESS.md:110-130`, `165-182`;
`store-module/FULL_SPEC_REVIEW.md:118-135`, `245-258`; the first 70 lines and the headings of
`store-module/PLAN-streaming-gate-and-record.md`, for register; `pure_proof.clj:1-60` and its list of
checks; six pages of the local copy of Rama's documentation, by search (11, 13, 14, 17, 25, 26);
`reference/README.md`; `deps.edn`, the two aliases.

Not read: `decisions/OPEN-QUESTIONS.md` beyond a search that found nothing (its rows are cited here
by the ids that the contract and the implied spec give); `store-module/PROGRESS.md` and
`FULL_SPEC_REVIEW.md` beyond the lines above; the two gates' dataflow; `gate/stamp.clj`;
`record/tables.clj`; the checks of the module's proof below line 503; the act's proof; the rig's
notes, the server and Inland, of which nothing is in this paper. No `env.clj` and no `.env` file.

Run: the four probes under "What the plan found out". The three existing proofs were not run by this
session; their counts are the contract's, which the build session confirmed by a run before the first
stage.

