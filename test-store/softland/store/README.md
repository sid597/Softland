# Store proofs

[Parent: JVM checks](../README.md) · [Verification map](../../README.md).

| File | What it checks | State that it owns or changes |
|---|---|---|
| [act_proof.clj](act_proof.clj) | [The act](../../../src-store/softland/store/act.clj): its declared shape and rules, and the check that reads them. It needs no cluster. | One atom: the lines that it showed. |
| [pure_proof.clj](pure_proof.clj) | The gates' pure namespaces, beside the act: the one text and the digest, the door's declaration, the stamp, the seal, the first act of a layer, the record's rows, the checks of the decision, and the pure code of a batch of the microbatch gate. It needs no cluster. | One atom: the lines that it showed. It takes random bytes from the JVM, and orders of arrival from a random source with a fixed seed. |
| [module_proof.clj](module_proof.clj) | [The store's module](../../../src-store/softland/store/README.md) on Rama's in-process cluster: the two gates, their records and the three reads. It plays the door by hand. | It starts one cluster in its own JVM and closes it. Eight atoms: the lines shown, the things sent to each depot, the first answers of each gate, the locks in hand, the bytes of each lock read, and what later checks come back to. |

## The act's proof

Run it from the repository root:

```sh
clojure -M:store -m softland.store.act-proof
```

Each check is a case: what it is about, a function that calls the act's code, and
what that function must give.

What it checks, group by group:

1. The parts. The two declarations, the shape and the rules, each written out a
   second time as literals; the parts of an act and of a fact, and the reason
   words, read from them; that an act-id, a class, a seal, a lock id, a layer
   and a mark are no parts; the version and the limits. And that the check reads
   the declarations: for each part of an act and of a fact, a thing without the
   part, and a thing with a thing of no shape in it, get what the declaration
   gives.
2. Acts that pass, each shown in its one form: from the least act (version, who,
   layer, permission, session and one fact), through a retract, plain values
   under any key, ids that are UUIDs, a permission that a parent granted, and an
   act that replaces a fact and says what it was based on, to an act with each
   optional part, given with subvectors, narrow integers and a list of facts. Its
   one form has only plain vectors and longs, and each one form parses again to
   itself.
3. One refused act for each fault, with its reason. Each differs from the least
   act in one thing. The proof reads the reason words from the declarations, and
   each word must come from an act that earns it.
4. The order of the checks, which is the order of the declarations: an act with
   two faults gets the reason of the earlier part, and of the shape before a rule.
5. Nothing throws: the check, and the test of each declared shape, are given
   things that are not acts, a thing that throws when it is read among them, and
   each gives back data.
6. The limits at their edges: the subjects, the collections around a value, a
   carried stamp, and the depth of a permission.
7. Ids and values: an id that is a UUID passes, and one that is a string fails;
   a key that is a UUID fails; a retract is a fact with no `:v`; what is a
   value, and its one normal form; the shapes of an act-id, a fact id and a
   permission id.

### What the act's proof does not reach

- an act after Rama wrote it to a depot and read it back: each case is a value
  of this process;
- facts that are a sequence with no end, for which the check does not come back;
- an act of many facts, and how long the check takes;
- the use of the act by a gate: the two proofs below show it.

## The pure proof

Run it from the repository root:

```sh
clojure -M:store -m softland.store.pure-proof
```

Each check is a case, as in the act's proof. A run of bytes is compared by its
content. What it checks, group by group:

1. The one text and the digest. One thing of each kind with its text. Pairs that
   no check can tell apart, and that a serialiser may change, have one text and
   one digest: an Integer and a Long, minus zero and zero, a subvector and a
   vector, a map built in two orders. Pairs that a check tells apart have two.
   The text of a value reads back as the value. The digest is the SHA-256 of the
   one text, with bytes fed as they are; a thing that throws when it is read has
   the digest of `#unreadable`.
2. The declaration. The door's five shapes and twenty rules, each written out a
   second time as literals; `act.clj`'s shapes inside the door's map as `act.clj`
   declares them; the limits, the store keys, the words of a stray. The test for a
   stray, one thing for each word. The one form on arrival.
3. One refused thing for each fault, with its word; a long id in each place where
   an act holds an id; each limit at its edge. The proof reads the reason words
   from the declarations, and each word must come from a thing that earns it.
4. The maker's check inside the door's: for each fault of the maker's form, the
   door's check gives the word that `act.clj`'s own parse gives.
5. The order of the reasons: a thing with two faults gets the word of the part
   that is declared first, of the shape before a rule, of the earlier rule.
6. Nothing throws: things that are no acts, facts that throw when they are read.
7. The stamp: the packing; the next stamp when the clock is ahead, stands still,
   goes back, and when one tick has more decisions than the counter holds; a
   carried stamp; the 10-second rule at its edge; the year in which the packing
   reaches 2^62.
8. The seal: a lock; sealed bytes open at their place only; another lock, a
   changed bit, another entity, key or layer do not open; a way that is not known;
   nothing throws.
9. The first act: the class that a layer starts with; acts that have the form of
   a making act, and one act for each way of not having it.
10. The record's rows: the three keys and their order; each field of each row of
    its declared class; a retract's row; a plain value's row; what each row says;
    the heads of the three reads.
11. The decision: the standing of an act in each state of its layer; each word of
    the checks of the places and of the seal, each on rows built as the gate
    writes them; the stamp of a decision; the rows that a refusal, an accept, a
    request for locks and a making act write.
12. The batch, function by function. What the microbatch depot derives from a
    thing, never throwing. The turn, the base and the stamp of a turn. The
    standing that step 1 can tell, and the questions of an act. The replies of a
    home task about an answer, a place and the locks of a request. Who goes to
    the meeting task: one case for each of the four rules, and the acts that
    stay. The home of each row of a decision: a used lock as two rows, the bytes
    in one. What a task writes at once and what it sends. The routing id, the
    heads of the two reads by routing id, and a row of a closed batch.
13. A batch is decided in one order. The proof runs whole batches in memory: the
    gate's four steps over the pure functions, each task's rows in its own map,
    each message gathered on the task of its route. It runs each batch in 200
    orders of arrival, drawn with a fixed seed, and compares the record after
    each with the record of deciding the same things one at a time in the order
    of their turns, by the checks of `softland.store.gate.decision` in their order. One
    batch is into a base that is made: two acts that replace one fact from two
    tasks, two that write one place, a give and an act under it in both orders,
    two acts under one lock, a request and a use of its lock, a repeat, one
    act-id under two things, acts for a layer of the other gate, strays, acts
    with no standing, an act on three tasks accepted and one refused whole, and a
    value whose lock's home is its own task. The other is into a base that is not
    made, with two making acts.

### What the pure proof does not reach

- a PState, a depot or a query: each row is a value of this process, and the
  walk of one event, which reads rows between the checks, is the gate's;
- Rama in the run of a batch in memory: the proof gathers and delivers the
  messages itself. That the microbatch gate's dataflow calls the pure functions
  in that way is the module's proof's to show;
- a thing after Rama serialised it;
- a clock: each case gives the time as a number;
- how long a check takes.

## The module's proof

Run it from the repository root:

```sh
clojure -M:store-module -m softland.store.module-proof
```

The checks run in one order on one cluster, and later checks build on the layers,
the facts and the permissions that earlier ones made. The hand reads each answer
back through the module's own queries. For a streaming layer it reads after an
append that waited for the gate. For the base no append waits: it reads in a loop
with a time limit, as a door would, and an answer is there about a second after
the append. To put several things into one batch of the microbatch gate it pauses
that topology with Rama's test tool, appends, and lets it go on.

The checks of the streaming gate come first. The base is made after
them, because one of them sends the base's making act to the streaming gate and
holds the word of a layer that is not made.

What it checks, group by group:

1. What Rama carries. A thing that holds each kind of value is appended; the gate
   records the same digest that the hand took before the append; the record read
   back from the depot's log is another object, with the same digest.
2. Layers are made. An act into a layer that is not made is refused. A person's
   layer is made by the system for that person, with the class streaming. A
   group's layer is made by the person who creates it. A making act for a layer
   that is made is refused, and so is a second act of the system in a personal
   layer.
3. An act lands. A request for locks, read with one query. One act with a sealed
   value, read back by act-id with the digest of what was sent. Its row, and its
   lock's row beside it, naming each other.
4. Refusals and repeats. A refusal recorded with its reason. A repeat answered
   from the record with the first stamp, and the task's last stamp unchanged. A
   second, different act under a used act-id, which reads an answer that is not of
   its digest. An unsealed value. A stale `replaces`, and the current fact named.
   A fact that names nothing is never stale. A retract is a row, and a later fact
   replaces it. A place named twice.
5. Settings and permissions. A setting key on another layer. A permission for
   another layer. A permission that does not stand. A permission given beneath
   another by its holder, and an act under it. A give by one who does not hold the
   one above. A chain four deep, and a fifth refused. `granted-in`. One write for
   a store key's place; the ending of a permission; a class after the making.
   Whole or nothing: a give with a stale fact gives nothing.
6. The path of a lock. A second value under a used lock. The lock of a refused act
   still free. Sealed bytes copied to another place. A lock of another layer. A
   refused request makes no lock. A request sent twice gives the same bytes. A
   read of a part of a request's locks.
7. Strays and limits. One stray for each word, kept once however often it comes,
   with no answer. A thing at and over each limit. A carried stamp too far ahead,
   and one 5 seconds ahead.
8. The reads take anything: what is no id, no act-id, another layer's act-id or
   the other class gets nothing.
9. After the module is started again. Rama's module update is run with the
   unchanged module: the memory of the old instance is gone, and the depot and
   the PStates stay. The answer, the class, the strays and the locks are as they
   were; a repeat is still answered from the record; the next stamp is later than
   the last. During this step Rama prints some lines of its own log that are
   marked ERROR, while the new instance opens the depot's log and the client finds
   the new instance. They are not failed checks.
10. The stamps and the depot. The stamps of the answers increase strictly on each
    task, in the order sent. The depot holds each thing sent. The layers are on
    more than one task. No lock's bytes are on the depot.

Then the base and the microbatch gate. Groups 11 to 19 run before group 9, and
group 20 after it:

11. The base is made. An act into the base before it is made is refused, and so
    is one sent to the streaming depot. Two making acts in one batch, their facts
    in the two orders: one is accepted. The class is microbatch; the class row
    and the system's permission row are on every task.
12. An act lands in the base. Eight acts of one batch on eight entities, whose
    rows are on more than one task. A request for locks, on one task with its
    answer and its locks. One act with a sealed value, read back by act-id. One
    copy of its lock's bytes: the whole row beside the value, the home's row
    without bytes, and the bytes open the value.
13. Closing. The proof watches the answers table itself for the row of a request
    for a lock. At the first sight of the row, the read by act-id gives nothing
    and the read of locks gives no lock; after the next run of the gate both
    give, with the stamp and the bytes of the rows that were in the table.
14. Refusals and repeats in the base. A refusal with its reason. A stale
    `replaces`. A repeat answered from the record. A different thing under a used
    act-id. One act-id under two things in one batch, on two tasks: one answer.
15. One batch, one order. Two acts of one batch that replace the same fact,
    arriving on two tasks: one winner, with the earlier stamp, and the loser
    refused whole. Two that write one place. A give and an act under it, in both
    orders. Two acts under one lock.
16. A person acts in the base under a permission that the system gave, whose row
    is on every task. Acts with no standing. One write for a store key's place. A
    carried stamp too far ahead, also under a permission that does not stand.
17. A long batch: 400 acts on 20 entities in one batch, each accepted, and on
    each entity the stamps go up in the order sent.
18. An act on several tasks. Refused whole: no row of it on any task, its three
    locks free. Accepted whole: each fact on its entity's task, each lock's bytes
    beside its value, one stamp on all of it, later than each row written before
    on those tasks; the gate's one last stamp. And an act that is decided where
    it arrived replaces the current fact of a place on another task: the row
    comes to it in a reply. Accepted; the same `replaces` again is stale.
19. Each gate tells the other gate's layers: an act for the base on the streaming
    depot, an act for a streaming layer on the microbatch depot, and one for a
    layer that is not made. One act-id on both depots has one answer. Strays of
    the microbatch gate. Then the path of a lock in the base: a second value
    under a used lock; a request sent twice; a value whose lock's home is its own
    task, with one row that holds the bytes; a use of a lock in the batch that
    asks for it; a refused request, which makes no lock.
20. The base after the module is started again: the answer, the class, the
    gate's last stamp, the strays, a permission's rows and the locks are as they
    were; and, last of all, the stamps of the microbatch gate's answers go up in
    the order decided, each answer that a read gave was of a closed batch at the
    read, the microbatch depot holds each thing sent, and no lock's bytes are on
    it. For the third, the hand selects `:closed-through` on the answer's task
    right after each read that gave an answer; `:closed-through` never goes back,
    so a stamp above it then is a row that was read before its batch was closed.

Each line that the contract asks for is on a `SHOWN` line in the contract's
words.

Where the proof shows a row of the base, it selects it from the gate's PState
with the row's own route, or once on each task to see which tasks hold it. Such
a select goes round the three reads and is not held to `:closed-through`; the
proof makes it after the read by act-id gave the answer, which is after the
batch is closed, but for group 13, which uses it to look before.

### What the module's proof does not reach

- an event that Rama runs twice, a batch of streaming events that fails, and a
  later act of a layer decided before an earlier one that failed;
- a second attempt of a batch of the microbatch gate; a commit that fails after
  one task showed its part; the moment between two tasks' commits. So closing is
  shown for a batch that ran once: that a read gives nothing of it before the
  next run of the gate, and not that a second attempt's other stamp or other lock
  bytes stay unread;
- a layer's class and a refusal's word read between a batch and its closing: the
  proof catches an answer and a lock there, and the class read has the same test;
- a read that gave a row of a batch that closed in the moment between that read
  and the hand's select of `:closed-through`: the last check of the base takes
  it for a row of a closed batch;
- that the read of locks gives a lock without its bytes before the batch of the
  act that uses it is closed: read in the code, not shown;
- a pass long enough to be known to give the task back: 400 acts in one batch is
  the longest;
- a rebuild of a record from its depot;
- a worker that stops in the middle of an event, a move of a task's leader, more
  than one machine, replication;
- an append whose ack fails for an act that the gate accepted;
- a thing that a topology appends to another partition;
- the three targets of speed and size: the in-process cluster's timings say
  nothing about them;
- a lock whose way of sealing the code does not know: no act can make one. The
  pure proof shows the check on a row built by hand.
