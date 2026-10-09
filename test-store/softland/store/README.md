# Store proofs

[Parent: JVM checks](../README.md) · [Verification map](../../README.md).

| File | What it checks | State that it owns or changes |
|---|---|---|
| [act_proof.clj](act_proof.clj) | [The act](../../../src-store/softland/store/act.clj): its declared shape and rules, and the check that reads them. It needs no cluster. | One atom: the lines that it showed. |
| [pure_proof.clj](pure_proof.clj) | The gates' pure namespaces, beside the act: the one text and the digest, the door's declaration, the stamp, the seal, the first act of a layer, the record's rows, the checks of the decision, and the pure code of a batch of the microbatch gate. And [the door's](../../../src-store/softland/store/door/README.md) three pure namespaces: its words, what it makes of one act, and its stock of locks. It needs no cluster. | One atom: the lines that it showed. It takes random bytes from the JVM, and orders of arrival from a random source with a fixed seed. |
| [module_proof.clj](module_proof.clj) | [The store's module](../../../src-store/softland/store/README.md) on Rama's in-process cluster: the two gates, their records and the three reads. It plays the door by hand. | It starts one cluster in its own JVM and closes it. Eight atoms: the lines shown, the things sent to each depot, the first answers of each gate, the locks in hand, the bytes of each lock read, and what later checks come back to. |
| [door_proof.clj](door_proof.clj) | [The door](../../../src-store/softland/store/door.clj) on Rama's in-process cluster: acts into both depots through the door's six calls, and their answers back. The proof is the door's one maker. | It starts one cluster in its own JVM and closes it. It opens three doors on the module. Four atoms: the lines shown, each result that a door gave, what later checks come back to, and the locks that the proof itself left outside every stock. |

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
   words, read from them; that an act-id, a depot, a seal, a lock id, a layer
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
- the use of the act by a gate: the pure proof and the module's proof, below,
  show it.

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
2. The declaration. The door's five shapes and twenty-one rules, each written out
   a second time as literals; `act.clj`'s shapes inside the door's map as
   `act.clj` declares them; the limits, the store's namespace and the store keys,
   the words of a stray. The test for a stray, one thing for each word. The one
   form on arrival. A fact under each of the three store keys passes. A tool's
   own fact with a sealed value passes as a fact under a plain key when its key
   is a bare word that the store's keys were (`:class`, `:permission`,
   `:lock-request`), the bare word `:depot`, one of the store's words in another
   namespace, or in a namespace that only begins with `softland.store`.
3. One refused thing for each fault, with its word; a key in the store's
   namespace that the store does not know, with a sealed value, a plain value
   and none; a long id in each place where an act holds an id; each limit at its
   edge. The proof reads the reason words from the declarations, and each word
   must come from a thing that earns it.
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
9. The first act: the depot that a layer starts with; acts that have the form of
   a making act, and one act for each way of not having it.
10. The record's rows: the three keys and their order; each field of each row of
    its declared class; a retract's row; a plain value's row; what each row says;
    the heads of the three reads.
11. The decision: the standing of an act in each state of its layer; each word of
    the checks of the places and of the seal, each on rows built as the gate
    writes them; that a tool's own fact under the bare word `:class` is no
    setting, and one under `:permission` no permission fact; the stamp of a
    decision; the rows that a refusal, an accept, a request for locks and a
    making act write.
12. The batch, function by function. What the microbatch depot derives from a
    thing, never throwing. The turn, the base and the stamp of a turn. The
    standing that step 1 can tell, and the questions of an act. The replies of a
    home task about an answer, a place and the locks of a request. Who goes to
    the meeting task: one case for each of the four rules, and the acts that
    stay, an act with a tool's own facts under the bare words among them. The
    home of each row of a decision: a used lock as two rows, the bytes in one; a
    tool's own fact under a bare word on its entity's task alone. The rows that
    the meeting task reads from its own copy. What a task writes at once and
    what it sends. The routing id, the heads of the two reads by routing id, and
    a row of a closed batch.
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
14. The door's words. The table of the door's own fourteen words, each with
    whether nothing was appended for the act or the door does not know, written
    out a second time. No word is a reason word of the act or of the declaration,
    or a word of a stray. A word as a maker gets it. The four answers from what a
    read by act-id gave: accepted and refused with the stamp, nothing yet, not
    mine for another digest and for no digest. The two acks.
15. What the door makes of one act. Things that are no act, one kind after the
    other, from a thing that is no map to an act of 257 facts: each ends in one
    of the door's words, or goes in, and the declaration's check then has the
    gate's word for it. The limit of sealed bytes at
    its edge, by the length that the seal gives. An act-id given back: of the
    layer, of another layer, not of the shape, with the other depot word. The
    kinds of fact: a store-key fact plain, a key in the store's namespace that is
    no store key sealed, a retract, a nil value. The order of the steps, for
    things with two faults. The thing that is built: `:facts` as a vector for a
    vector, a list and a lazy sequence; a sealed fact's parts; the sealed bytes
    open at their place to the plain bytes; the declaration's check takes it; it
    is no stray; the maker's parts are the maker's own objects; its digest. The
    request for locks: its parts as given, its one fact, and the check takes it at
    1 and at 256 locks. Nothing throws for facts that throw when they are read.
    No lock's bytes are in the thing. The count of a series of act-ids.
16. The door's stock. A stock that holds nothing. A take from one run, two takes
    that share no lock, a take across the border of two runs, and a take that is
    short, which changes nothing. The size rule, case by case, and the door's
    five numbers. Over 200 acts of 1 to 5 values, drawn with a fixed seed, the
    stock holds after each request fewer unused locks than that request asked
    for. What goes into a run, and that a read of locks that came back ends its
    request. A lock that was taken is in the stock no more. The open request: in
    doubt with its moment, sent, forgotten, and how long one in doubt is kept.
    The count of what was given out, and the door's count, which prints no byte.

### What the pure proof does not reach

- a PState, a depot or a query: each row is a value of this process, and the
  walk of one event, which reads rows between the checks, is the gate's;
- Rama in the run of a batch in memory: the proof gathers and delivers the
  messages itself. That the microbatch gate's dataflow calls the pure functions
  in that way is the module's proof's to show;
- a thing after Rama serialised it;
- a clock: each case gives the time as a number;
- how long a check takes;
- of the door: anything that needs a cluster, a clock or a second thread. The
  seat of a stock, the waits, the count of the calls that use a stock, and what
  holds when two takes race are the door's proof's to show, as far as it does.

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
   layer is made by the system for that person, with the depot streaming. A
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
   a store key's place; the ending of a permission; a depot after the making.
   Whole or nothing: a give with a stale fact gives nothing. A tool's own facts
   under the bare words `:class` and `:permission`, sealed, on the layer's
   entity and on a holder's: accepted, each at its own place, and the layer's
   depot and the holder's permission are as they were. A key in the store's
   namespace that the store does not know: refused, with the refusal recorded.
6. The path of a lock. A second value under a used lock. The lock of a refused act
   still free. Sealed bytes copied to another place. A lock of another layer. A
   refused request makes no lock. A request sent twice gives the same bytes. A
   read of a part of a request's locks.
7. Strays and limits. One stray for each word, kept once however often it comes,
   with no answer. A thing at and over each limit. A carried stamp too far ahead,
   and one 5 seconds ahead.
8. The reads take anything: what is no id, no act-id, another layer's act-id or
   the other depot gets nothing.
9. After the module is started again. Rama's module update is run with the
   unchanged module: the memory of the old instance is gone, and the depot and
   the PStates stay. The answer, the depot, the strays and the locks are as they
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
    in the two orders: one is accepted. The depot is microbatch; the depot row
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
    key in the store's namespace that the store does not know, refused. A tool's
    own fact under the bare word `:permission`, on a holder's entity: accepted,
    its row on one task, beside the store's permission on every task. A carried
    stamp too far ahead, also under a permission that does not stand.
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
20. The base after the module is started again: the answer, the depot, the
    gate's last stamp, the strays, a permission's rows and the locks are as they
    were; and, last of all, the stamps of the microbatch gate's answers go up in
    the order decided, each answer that a read gave was of a closed batch at the
    read, the microbatch depot holds each thing sent, and no lock's bytes are on
    it. For the third, the hand selects `:closed-through` on the answer's task
    right after each read that gave an answer; `:closed-through` never goes back,
    so a stamp above it then is a row that was read before its batch was closed.

Each line that the contract asks for is on a `SHOWN` line in the contract's
words, with "depot" where the contract says "class".

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
- a layer's depot and a refusal's word read between a batch and its closing: the
  proof catches an answer and a lock there, and the depot read has the same test;
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

## The door's proof

Run it from the repository root:

```sh
clojure -M:store-module -m softland.store.door-proof
```

The proof is the door's one maker: each act, each request for locks and each
answer goes through the door's six calls. The checks run in one order on one
cluster. Each who of the proof has a layer of its own, made by the system through
the door, so that a check knows what its who's stock holds. The proof opens three
doors on the module: the door of most checks; a second whose wait for locks is
400 ms; and a third near the end, which stands for the door of a process that
started again.

A pass shows the door in one process, with a maker that the proof plays. No
other maker uses the door yet.

What it checks, group by group:

1. The door is opened. A name under which no module runs gives the door's word,
   by a real throw of Rama's client. A door prints as the word "door" and the
   module's name, whatever prints it, and a new door holds no lock.
2. An act with no value. An act-id for a layer that is not made carries the depot
   that the layer starts with. The making act through the door, with the
   streaming gate's ack; its answer, accepted, with a stamp. The record in the log
   is the maker's act with `:act-id` and `:depot` and nothing else changed, and
   its digest is the digest that the maker got. Twenty act-ids of one layer with
   one query.
3. Round trips are counted: a send that meets a request for locks, a send with
   its act-id given and its locks in the stock, a send that makes its act-id, an
   answer with no wait, a wait of 600 ms that finds nothing, a bound that is not
   above zero, which is one read, and an answer for a thing that is no act-id,
   which makes no query.
4. Locks and the seal. A person's first act with a value: one request in the log,
   for 4 locks, under her who, permission, session and version; the fact sealed;
   the lock, read round the door, opens the sealed bytes at their place to the
   plain bytes. As she writes 129 values the requests ask for 4, 4, 8, 16, 32, 64
   and 64. An act with a store-key fact, a retract and a value takes one lock.
5. The door and the hand send the same thing for one act with a value, a retract
   and a store-key fact, but for the random parts; both are accepted.
6. The digest that the door gives is the gate's, for facts given as a vector, a
   list and a lazy sequence, and `:facts` arrives as a vector.
7. Things that are no act, one kind after the other. Each that ends in one of
   the door's words: the word, no record more in either log, no stray more.
   Each that goes in: the gate's recorded refusal, read through the door.
8. The answer back: the four answers; nothing yet for a thing that is no act-id,
   with no query; not mine for a second, different act under a used act-id and
   for a maker's own second send of one act with a value; a wait whose thread is
   interrupted; and the stamp of an answer used in `:based-on`.
9. A refused request. A who with no permission, and a layer that is not made: the
   door's word with the request's act-id and the gate's reason, the request and
   its refusal in the record, nothing under the act's own act-id. And the other
   outcome of the same fault with locks in the stock: the act goes in and is
   refused under its own act-id.
10. A request in doubt. One whose append threw before it left the process, by a
    real throw of Rama's client: after longer than the door's wait, the next send
    reads it once, forgets it, asks anew and goes in. One whose append threw and
    went through: the next send reads its answer and uses its locks, with no
    second request.
11. Reads that throw: the read of a layer's depot, of an answer, of a request's
    answer and of a request's locks, each with its word. After the third, a send
    of 10 values reads the earlier request of 4 locks, asks for its own in the
    same call and names the locks of both. After the fourth, the next send reads
    the answer again and then the locks. And a read that gives locks that cannot
    seal: the word, the door holds none of them, and the send after asks anew.
    And a refused request that an earlier call left open: the next send hands
    that refusal to nobody and is handed the refusal of its own request.
12. An append that threw: before the thing left the process, by a real throw; and
    one that went through, on a thread whose interrupt mark the proof set, where
    the log holds the record, its digest is the word's, and the answer is
    accepted.
13. A lock never crosses a who: a second holder of a layer, and one who in two
    layers, each get a request of their own while the first stock holds locks.
14. Many makers at once: 16 threads for one who, 25 acts each of 1 to 3 values,
    each accepted, no lock named twice, each act naming one request or two, each
    request held to the size rule; then 8 whos, each on 2 threads.
15. The base through the door: its making act, with the microbatch depot's ack
    and a wait; a value act, whose request is sent and answered inside the call,
    with the calls of Rama's client in their order and the first read of the
    request's answer one interval after its append; 4 acts of one who from 4
    threads while the gate is paused, with one request in the log; 8 acts on 8
    entities in one batch.
16. A wait for locks that passes, for a request that its depot acked while the
    gate was paused, with a second maker that waits for the same stock; when the
    gate goes on, the next send uses the same request.
17. A request in doubt in the base, met at once by the next send: its first read
    finds nothing, since the request's batch is not closed; the door keeps the
    request, reads again each interval, and uses its locks, with no second
    request.
18. An act-id given back with the other depot word: the thing goes to the depot
    that it names and that gate refuses it; with a value, the door's request is
    refused there, and no lock of the layer's own depot is taken.
19. The module is started again under the opened door, by Rama's module update
    with the unchanged module: an answer from before is the same, and a lock that
    the door held before seals a value after.
20. The door's memory starts again: a new door holds nothing and asks anew. Then
    the rows that seal nothing are counted in both records, and the count is held
    against its sources: the unused locks in the three doors' stocks, the locks
    that the proof itself left outside every stock, and the locks that were taken
    for acts that were not accepted.
21. Over the whole run: every lock's bytes, read round the door, are in no record
    of either log; no result that a door gave holds a run of bytes; a lock never
    crosses a who, over every record of both logs; the door made no stray; and
    each of the door's fourteen words was given.

### Where the proof stands in for a call of Rama's client

Clojure lets a proof put a function of its own in the place of Rama's client
calls `foreign-append!` and `foreign-invoke-query` for a while. The proof does so
for four things, and each `SHOWN` line that rests on it says so:

- to count the round trips of a call of the door, or to note the moment of each
  (groups 2, 3, 8, 10, 11, 15 and 17): the function counts or notes and then
  calls Rama;
- to make a read throw (group 11): no real throw of a query was found that the
  in-process cluster gives without a fault of the cluster, because the door tests
  each thing that it passes to a read before it calls;
- to give the locks of one read back with a way of sealing that the code does not
  know (group 11): the function calls Rama and changes what it gave;
- to set the interrupt mark of its own thread at the append of one request
  (groups 10 and 17): the function sets the mark and then calls Rama, and the
  throw is Rama's client's.

The other throws are real: a module name under which no module runs; a thing
that holds a plain Java object, which Rama's client cannot turn into bytes; and
a call on a thread whose interrupt mark the proof set. The proof never destroys
the module to make a call throw, because the close of the cluster then throws.

### What it changes outside the door

It appends to both depots of its own cluster through the door, and so writes
both records. Round the door it appends two things with a handle of its own: the
hand's request for locks and the hand's act, for group 5. It pauses the
microbatch gate with Rama's test tool three times and lets it go on, and it
starts the module again once. It sets the interrupt mark of three threads of its
own, and interrupts a fourth while it waits.

### The door as the module's one client, and two proofs

In the runtime the door is the module's only client. Two proofs are not held to
that. The module's proof does not use the door at all: its hand appends to the
depots and reads the queries itself, which is what keeps the store proven without
the door. The door's proof sends through the door, and beside that it holds
handles of its own: it reads the two depots' logs, the answer of a request and the
locks of a request by the module's queries, each lock's row by a select of the
gates' PStates, and the strays; and it appends the two things of group 5. A maker
has none of these ways.

### What the door's proof does not reach

- a throw of a query that Rama's client makes by itself;
- an append that threw for a fault of the cluster and went through all the same:
  the one that the proof reaches is an append on a thread that it interrupted
  itself;
- a request in doubt whose append truly failed, met by a call before the door's
  wait had passed since the throw: that call would wait for nothing until then.
  The proof meets such a request only after that time; the request in doubt
  that it meets at once is one that went through;
- a worker of another process, and a thing of a type that it does not have;
- a real cluster, more than one machine, replication, and a module that is
  started again there. That one set of handles serves many threads is shown for
  one process, by one run each time, and Rama's documentation does not say it;
- how fast the door is: the in-process cluster's timings say nothing about it;
- a wait that outlasts its bound by the time limit of a query, and a call in a
  stock's seat that outlasts the door's wait by a call of Rama's client;
- a gate that is so slow that a request stays open over many calls;
- two takes of one lock at the same instant, forced: 16 threads take from one
  stock and no lock is named twice, in one run each time;
- that a stock which holds nothing leaves the door's memory, and a call that
  leaves such a stock while another enters it: no call of the door shows its map
  of stocks. The pure proof holds the test of such a stock, and the count of
  group 20 would be short if a stock with locks were lost;
- an act with a value whose append threw: the locks that it took are then named
  by no record, and the count of group 20 has no such act;
- facts with no end that hold fewer than 257 values, for which a send does not
  come back.
