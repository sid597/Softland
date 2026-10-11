# Store proofs

[Parent: JVM checks](../README.md) · [Verification map](../../README.md).

| File | What it checks | State that it owns or changes |
|---|---|---|
| [act_proof.clj](act_proof.clj) | [The act](../../../src-store/softland/store/act.clj): its declared shape and rules, and the check that reads them. It needs no cluster. | One atom: the lines that it showed. |
| [pure_proof.clj](pure_proof.clj) | The gates' pure namespaces, beside the act: the one text and the digest, the door's declaration, the stamp, the seal, the first act of a layer, the record's rows, the checks of the decision, and the pure code of a batch of the microbatch gate. And [the door's](../../../src-store/softland/store/door/README.md) three pure namespaces: its words, what it makes of one act, and its stock of locks. It needs no cluster. | One atom: the lines that it showed. It takes random bytes from the JVM, and orders of arrival from a random source with a fixed seed. |
| [module_proof.clj](module_proof.clj) | [The store's module](../../../src-store/softland/store/README.md) on Rama's in-process cluster: the two gates, their records and the three reads. It plays the door by hand, and, where it reads a fact's row or a run of the facts by value, a reader of the tables. | It starts one cluster in its own JVM and closes it. Eight atoms: the lines shown, the things sent to each depot, the first answers of each gate, the locks in hand, the bytes of each lock read, and what later checks come back to. |
| [door_proof.clj](door_proof.clj) | [The door](../../../src-store/softland/store/door.clj) on Rama's in-process cluster: acts into both depots through the door's six calls, and their answers back. The proof is the door's one maker. | It starts one cluster in its own JVM and closes it. It opens five doors on the module. Four atoms: the lines shown, each result that a door gave, what later checks come back to, and the locks that the proof itself left outside every stock. |

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
   under any key, ids that are UUIDs, a permission that a parent granted, a
   given permission that says what it allows, see and see and act, and an
   act that replaces a fact and says what it was based on, to an act with each
   optional part, given with subvectors, narrow integers and a list of facts. Its
   one form has only plain vectors and longs, and each one form parses again to
   itself.
3. One refused act for each fault, with its reason. Each differs from the least
   act in one thing. Among them, for the slot of a permission that says what it
   allows: a word in the place of the permission above, a third word in the
   slot, a permission in the slot, six items, and a slot after a first
   permission. The proof reads the reason words from the declarations, and
   each word must come from an act that earns it.
4. The order of the checks, which is the order of the declarations: an act with
   two faults gets the reason of the earlier part, and of the shape before a rule.
5. Nothing throws: the check, and the test of each declared shape, are given
   things that are not acts, a thing that throws when it is read among them, and
   each gives back data.
6. The limits at their edges: the subjects, the collections around a value, a
   carried stamp, and the depth of a permission, which the slot does not count
   toward. And the three forms of a permission, each under an act that the
   check takes.
7. Ids and values: an id that is a UUID passes, and one that is a string fails;
   a key that is a UUID fails; a retract is a fact with no `:v`; what is a
   value, and its one normal form; the shapes of an act-id, a fact id and a
   permission id, the last in its three forms.

### What the act's proof does not reach

- an act after Rama wrote it to a depot and read it back: each case is a value
  of this process;
- facts that are a sequence with no end, for which the check does not come back;
- an act of many facts, and how long the check takes;
- the use of the act by a gate, and what a gate does with a permission that
  says see: the pure proof and the module's proof, below, show it.

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
2. The declaration. The door's eight shapes and twenty-seven rules, each written out
   a second time as literals; `act.clj`'s shapes inside the door's map as
   `act.clj` declares them; the seven limits, the store's namespace and the six
   store keys, with the shape of the value of each, which is a setting and which
   takes a second write; the words of a stray. The test for a stray, one thing
   for each word. The one form on arrival. A fact under each of the six store
   keys passes, with a right plain value; so do the values that the three keys of
   the read build take, an empty list and an empty set among them, a fact with
   no value under each, and a permission fact with the slot. A tool's
   own fact with a sealed value passes as a fact under a plain key when its key
   is a bare word that the store's keys were (`:class`, `:permission`,
   `:lock-request`), the bare word `:depot`, one of the store's words in another
   namespace, or in a namespace that only begins with `softland.store`.
3. One refused thing for each fault, with its word; a key in the store's
   namespace that the store does not know, with a sealed value, a plain value
   and none; each wrong value under the three keys of the read build, 65 layers,
   33 marked keys, a mark of a key of the store's own namespace, and each of the
   three arriving sealed; a long id in each place where an act holds an id, in a
   list of layers, in a set of marked keys and above a permission with the slot;
   each limit at its edge. The proof reads the reason words from the declarations, and each word
   must come from a thing that earns it.
4. The maker's check inside the door's: for each fault of the maker's form, the
   door's check gives the word that `act.clj`'s own parse gives.
5. The order of the reasons: a thing with two faults gets the word of the part
   that is declared first, of the shape before a rule, of the earlier rule. The
   six rules of the three keys stand after the rules of a request for locks and
   before the seal's, in their own order.
6. Nothing throws: things that are no acts, facts that throw when they are read.
7. The stamp: the packing; the next stamp when the clock is ahead, stands still,
   goes back, and when one tick has more decisions than the counter holds; a
   carried stamp; the 10-second rule at its edge; the year in which the packing
   reaches 2^62.
8. The seal: a lock; sealed bytes open at their place only; another lock, a
   changed bit, another entity, key or layer do not open; a way that is not known;
   nothing throws.
9. The first act: the depot that a layer starts with; what a layer starts with
   where no fact says who may see it, what it builds on or which keys it marks;
   acts that have the form of a making act, and one act for each way of not
   having it, a permission with the slot among them.
10. The record's rows: the keys and their order; each field of each row of
    its declared class. A fact's row holds the plain bytes that its lock opened,
    who wrote it and the permission that the act named, and no lock; a retract's
    row holds no value; the row of the marked keys holds the stamps that the
    gate derived. What each row says: the fact that wrote it, whether it holds a
    value, its first form, who and the permission read back, and for a reader
    one of four: no fact, nothing here, the value, bytes that are no value of
    the form. The facts by value: the value digest, a key, the two ends of a
    run, that no run holds a key of another value, layer, key or stamp for six
    kinds of value, the two texts of a UUID, and a key back to its parts. The
    heads of the three reads.
11. The decision: the standing of an act in each state of its layer, and what its
    permission allows: a holder of see who names her permission as the row has
    it, without the slot, and with see and act; each word of
    the checks of the places and of the seal, each on rows built as the gate
    writes them; for each of the three keys of the read build a first write, a
    second, one that names the fact there, a stale one, a retract and another
    entity; who may write them; that a tool's own fact under the bare word
    `:class` is no
    setting, and one under `:permission` no permission fact; that the check of
    the seals gives the plain bytes that each lock opened, and none when a later
    value of the act fails; the stamp of a
    decision; the rows that a refusal, an accept, a request for locks, a
    making act and a write of the marked keys write. And the order by value,
    each act decided as steps 6b and 7 of the streaming gate decide it against a
    record in memory: a mark, a fact, the mark taken off, the fact replaced, the
    mark put on again, the fact replaced twice; a fact replaced with `:replaces`
    and without, written again with the same value, retracted; each kind of
    value; a mark and a fact under it in one act; which acts read the marked
    keys.
12. The batch, function by function. What the microbatch depot derives from a
    thing, never throwing. The turn, the base and the stamp of a turn. The
    standing that step 1 can tell, a permission that says see among them, and
    the questions of an act, which hold the layer's marked-keys place and the
    fact's key; a fact under one of the five keys whose row every task holds
    asks nothing. The marked-keys places that a task reads, the reads of a task
    with and without a marked key, and the last stamp that a batch can hold. The
    replies of a
    home task about an answer, a place and the locks of a request: under a
    marked key a reply holds the key's stamp, the row goes to each asker that
    does not have the place as its own, and the place is shared whenever two
    acts name it. Who goes to
    the meeting task: one case for each of the four rules, and the acts that
    stay, an act with a tool's own facts under the bare words among them. The
    home of each row of a decision: a used lock as two rows, the bytes in one; a
    fact under each of the five keys on every task; a row of the facts by value
    on the task of its value digest; a
    tool's own fact under a bare word on its entity's task alone. The rows that
    the meeting task reads from its own copy. What a task writes at once and
    what it sends; that a pass sends one row for a key of the facts by value,
    the last, which can be a removal. The routing id, the heads of the two reads
    by routing id, and
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
    made, with two making acts. What is compared: each answer, each fact row on
    one task or, under one of the five keys, on every task, each row of the
    facts by value on the task of its value digest, and each lock.
14. A batch under marked keys, run in memory in the same 200 orders. The model
    that it is held against decides the acts one at a time in the order of their
    turns, with one thing that is not in that order: each act is decided with
    the layer's marked keys as the record held them before the batch, and a
    newly marked key gets the last stamp that the batch can hold. In the batch:
    two acts on one place under a marked key, each with the place as its own;
    the same value written again; a retract of a fact from before the mark and
    of one since it; an act whose marked place is on another task and holds a
    value since the mark; a fact that
    names what it replaces, and one that names a fact which is not there; a
    write of the marks, a fact under the new key, a
    fact under a key whose mark is taken off, and a second write of the marks;
    two writes of who may see the layer from two tasks; two writes of the list
    that name one fact to replace; a holder of see who acts, who names her
    permission without the slot, and who gives; a retract of the marks; marks on
    another entity.
15. The door's words. The table of the door's own fourteen words, each with
    whether nothing was appended for the act or the door does not know, written
    out a second time. No word is a reason word of the act or of the declaration,
    or a word of a stray. A word as a maker gets it. The four answers from what a
    read by act-id gave: accepted and refused with the stamp, nothing yet, not
    mine for another digest and for no digest. The two acks.
16. What the door makes of one act. Things that are no act, one kind after the
    other, from a thing that is no map to an act of 257 facts: each ends in one
    of the door's words, or goes in, and the declaration's check then has the
    gate's word for it. The limit of sealed bytes at
    its edge, by the length that the seal gives. An act-id given back: of the
    layer, of another layer, not of the shape, with the other depot word. The
    kinds of fact: a fact under each of the six store keys plain, a key in the store's namespace that is
    no store key sealed, a retract, a nil value. The order of the steps, for
    things with two faults. The thing that is built: `:facts` as a vector for a
    vector, a list and a lazy sequence; a sealed fact's parts; the sealed bytes
    open at their place to the plain bytes; the declaration's check takes it; it
    is no stray; the maker's parts are the maker's own objects; its digest. The
    request for locks: its parts as given, its one fact, that it is made of the
    four parts that the reading holds and reads nothing of the maker's thing
    again, and the check takes it at
    1 and at 256 locks. Nothing throws for facts that throw when they are read.
    No lock's bytes are in the thing. The count of a series of act-ids.
17. The door's stock. A stock that holds nothing. A take from one run, two takes
    that share no lock, a take across the border of two runs, and a take that is
    short, which changes nothing. The size rule, case by case, and the door's
    five numbers; a wait that the opener gives is cut to the longest that the
    door counts. Over 200 acts of 1 to 5 values, drawn with a fixed seed, the
    stock holds after each request fewer unused locks than that request asked
    for. What goes into a run, and that a read of locks that came back ends its
    request. A lock that was taken is in the stock no more. The open request: in
    doubt with its moment, sent, forgotten, and how long one in doubt is kept.
    The count of what was given out, and the door's count, which prints no byte.

### What the pure proof does not reach

- a PState, a depot or a query: each row is a value of this process, and the
  walk of one event, which reads rows between the checks, is the gate's. The
  cases of the order by value walk steps 6b and 7 of the streaming gate with a
  function of the proof's own, on a record in memory; that the gate's dataflow
  makes those reads and writes is the module's proof's to show;
- a seal in those cases: the plain bytes are handed over as the check of the
  seals would give them;
- a collision of the value digest: two values with one SHA-256;
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
   value, read back by act-id with the digest of what was sent. Its row holds
   the plain bytes of the value, who wrote it and the permission that the act
   named, and names no lock. The lock's row names the fact, and opens the
   sealed bytes that the hand sent to the row's bytes.
4. Refusals and repeats. A refusal recorded with its reason. A repeat answered
   from the record with the first stamp, and the task's last stamp unchanged. A
   second, different act under a used act-id, which reads an answer that is not of
   its digest. An unsealed value. A stale `replaces`, and the current fact named.
   A fact that names nothing is never stale. A retract is a row with no value,
   which holds who retracted and under which permission, and a later fact
   replaces it. A place named twice.
5. Settings and permissions. A setting key on another layer. A permission for
   another layer. A permission that does not stand. A permission given beneath
   another by its holder, and an act under it. A give by one who does not hold the
   one above. A chain four deep, and a fifth refused. `granted-in`. One write for
   a permission's place; the ending of a permission; a depot after the making.
   Whole or nothing: a give with a stale fact gives nothing. A tool's own facts
   under the bare words `:class` and `:permission`, on the layer's
   entity and on a holder's: accepted, each at its own place with the tool's
   value, and the layer's
   depot and the holder's permission are as they were. A key in the store's
   namespace that the store does not know: refused, with the refusal recorded.
6. The path of a lock. A second value under a used lock. The lock of a refused act
   still free. Sealed bytes copied to another place. A lock of another layer. A
   refused request makes no lock. A request sent twice gives the same bytes. A
   read of a part of a request's locks.
7. Strays and limits. One stray for each word, kept once however often it comes,
   with no answer. A thing at and over each limit; a sealed value of 65,536
   bytes is kept as its 65,508 plain bytes, which here are no value of the
   form, and a reader is told so. A carried stamp too far ahead,
   and one 5 seconds ahead.
8. The reads take anything: what is no id, no act-id, another layer's act-id or
   the other depot gets nothing.
9. What a fact keeps. In a layer that the proof makes for these checks, an act
   with a value of each kind: a keyword id, a UUID, a string with spaces, a
   number, a map. Each row holds the plain bytes that the hand sealed and reads
   back as the value; the act in the depot's log still holds the sealed bytes
   and the lock ids, and no plain value; each lock's row names its fact and
   opens the log's bytes to the row's. An act whose first value opens and whose
   second does not leaves no row and no used lock. An unsealed value is still
   refused. Who wrote each row and under which permission: the maker of the
   layer; a second holder; a retract; a give; and the system's making of a
   personal layer, where the who is the system and the permission the person's.
10. The keys that take a second write. For who may see a layer, what it builds
    on and its marked keys, each: no row and what the layer starts with; a first
    write, a second, one that names the fact there; then a stale `replaces`, a
    retract, another entity and a sealed value, each refused with its word. The
    stamps of the marked keys, which the gate derives: the stamp of the marking
    act, none for the empty set, a new one when the key is marked again. A wrong
    value under each key. 64 layers and 32 keys. A list that names the layer
    itself, a layer twice and a layer that nobody made. A permission a second
    time and a depot after the making, still refused. A second holder of the
    layer writes who may see it. A setting into a layer that is not made, and a
    making act with a setting as a third fact, are refused, and nothing is
    written.
11. What a permission allows. A permission given with the slot that says see.
    Its holder acts, naming it as the row has it: refused, with a word of its
    own. She names it without the slot, and with see and act: it does not stand.
    She cannot give, and her request for locks makes no lock. With a second
    fault she gets the word of her permission. A second give to her of see and
    act is refused, and so is one by another giver. A holder of see and act
    acts and gives. A making act whose
    permission has the slot is refused, and the layer is then made by a right
    act.
12. What points at a thing, in a streaming layer. A fact before the mark; the
    mark; nothing filled from before. A fact whose value is a keyword id, then
    one whose value is a UUID; replaced naming what it replaces; written over;
    the same value again; a string with spaces; a stale `replaces`, refused,
    which leaves each run as it was; a retract, and a fact onto it; the mark
    taken off; a
    fact replaced meanwhile; the key marked again, with a new stamp; a fact
    written and replaced under the new stamp. After each act the hand selects
    the run of each value, as one range of the facts by value, and the key of
    each entry that went out. Once the same act is appended a second time,
    after a later act put back the entry that it had taken out: the run stays
    as the later act left it. A mark and a fact under it in one act.
13. After the module is started again. Rama's module update is run with the
    unchanged module: the memory of the old instance is gone, and the depot and
    the PStates stay. The answer, the depot, the strays and the locks are as they
    were, and so are a value kept plain, the layer's marked keys with their
    stamps and the run of a value; a repeat is still answered from the record;
    the next stamp is later than
    the last. During this step Rama prints some lines of its own log that are
    marked ERROR, while the new instance opens the depot's log and the client finds
    the new instance. They are not failed checks.
14. The stamps and the depot. The stamps of the answers increase strictly on each
    task, in the order sent. The depot holds each thing sent. The layers are on
    more than one task. No lock's bytes are on the depot.

Then the base and the microbatch gate. Groups 15 to 29 run before group 13, and
group 30 after it:

15. The base is made. An act into the base before it is made is refused, and so
    is one sent to the streaming depot. Two making acts in one batch, their facts
    in the two orders: one is accepted. The depot is microbatch; the depot row
    and the system's permission row are on every task.
16. An act lands in the base. Eight acts of one batch on eight entities, whose
    rows are on more than one task. A request for locks, on one task with its
    answer and its locks. One act with a sealed value, read back by act-id; its
    row holds the plain bytes, who wrote it and the permission, and no lock. One
    copy of its lock's bytes: the whole row on the value's task, the home's row
    without bytes, and the bytes open the sealed bytes that were sent to the
    row's value.
17. Closing. The proof watches the answers table itself for the row of a request
    for a lock. At the first sight of the row, the read by act-id gives nothing
    and the read of locks gives no lock; after the next run of the gate both
    give, with the stamp and the bytes of the rows that were in the table.
18. Refusals and repeats in the base. A refusal with its reason. A stale
    `replaces`. A repeat answered from the record. A different thing under a used
    act-id. One act-id under two things in one batch, on two tasks: one answer.
19. One batch, one order. Two acts of one batch that replace the same fact,
    arriving on two tasks: one winner, with the earlier stamp, and the loser
    refused whole. Two that write one place. A give and an act under it, in both
    orders. Two acts under one lock.
20. A person acts in the base under a permission that the system gave, whose row
    is on every task. Acts with no standing. One write for a permission's place
    and for the depot. A
    key in the store's namespace that the store does not know, refused. A tool's
    own fact under the bare word `:permission`, on a holder's entity: accepted,
    its row on one task with the tool's value, beside the store's permission on
    every task. A carried
    stamp too far ahead, also under a permission that does not stand.
21. A long batch: 400 acts on 20 entities in one batch, each accepted, and on
    each entity the stamps go up in the order sent.
22. An act on several tasks. Refused whole: no row of it on any task, its three
    locks free. Accepted whole: each fact on its entity's task, each lock's bytes
    on its value's task, opening what was sent to the row's value, one stamp on
    all of it, later than each row written before
    on those tasks; the gate's one last stamp. And an act that is decided where
    it arrived replaces the current fact of a place on another task: the row
    comes to it in a reply. Accepted; the same `replaces` again is stale.
23. Each gate tells the other gate's layers: an act for the base on the streaming
    depot, an act for a streaming layer on the microbatch depot, and one for a
    layer that is not made. One act-id on both depots has one answer. Strays of
    the microbatch gate.
24. What a fact keeps in the base: as group 9, on one entity of the base. The
    whole row of each lock, on the value's task, opens the log's sealed bytes to
    the row's bytes. A refused act leaves no row on any task. A person's rows
    hold her and the permission that the system gave her.
25. The keys that take a second write, in the base. The three first writes in
    one batch, the three second ones in one, the three that name the fact there
    in one: each accepted, and each row on every task, the same on each. Then
    eighteen things in one batch, each refused with its word: a stale
    `replaces`, a retract, another entity, a sealed value and each wrong value.
    The stamps of the marked keys on every task: the last stamp of the batch
    that marked the key, which is not below any stamp of that batch. A person
    with a permission in the base writes who may see it. A setting for the base
    sent to the streaming depot is refused as an act into a layer of the other
    gate.
26. What a permission allows, in the base. A give with the slot that says see,
    and an act of its holder in the same batch, which is decided on the meeting
    task against the row that the give wrote. In a later batch her act is
    refused where it arrives. Naming it without the slot, with see and act, a
    give of hers, her request for locks, and a second give to her.
27. What points at a thing, in the base: the walk of group 12, one act in each
    batch, on two entities that are on two tasks. Two places on two tasks that
    hold one value have their two rows on one task, which the hand finds with
    the value's digest as the partitioning key. Where an entry goes out, no task
    holds a row under its key. An act that arrived on another task than the
    place's replaces a fact under a marked key: the row before came in a reply.
28. One batch under a marked key. Two acts of one batch on one place, each with
    the place as its own: both accepted, the place holds the later turn's value,
    and the first act's value has no row on any task. A mark and a fact under
    its key in one batch: the fact is in no run, and its stamp is not later than
    the key's; a fact of a later batch is in the run. Two writes of who may see
    the base in one batch, from two tasks: every task holds the later turn's
    row.
29. The path of a lock in the base: a second value
    under a used lock; a request sent twice; a value whose lock's home is its own
    task, with one row that holds the bytes; a use of a lock in the batch that
    asks for it; a refused request, which makes no lock.
30. The base after the module is started again: the answer, the depot, the
    gate's last stamp, the strays, a permission's rows, the locks, the marked
    keys on every task, a run of the facts by value and a value kept plain are
    as they were; and, last of all, the stamps of the microbatch gate's answers go up in
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
batch is closed, but for group 17, which uses it to look before.

The hand also plays a reader of the tables, since no reader is built yet: it
selects a fact's row, the row of a layer's marked keys and a run of the facts by
value, with the keys and the partitioning keys that
[rows.clj](../../../src-store/softland/store/record/rows.clj) gives. It opens no
push. For the base those selects go round `:closed-through` too.

### What the module's proof does not reach

- an event that Rama runs twice, a batch of streaming events that fails, and a
  later act of a layer decided before an earlier one that failed. Group 12
  appends one accepted act a second time, which ends at the gate's read of its
  answer; that is a second thing under a used act-id, not Rama running an event
  again;
- that a streaming layer's fact and its rows in the facts by value have no
  moment between them: it rests on one event with no partitioner, read in the
  code;
- that each row which the gates now keep, a row of the facts by value and the
  stamps of the marked keys among them, comes out the same on a second attempt
  of a batch: the build of the base's kept clock shows it, and this proof makes
  no batch run twice;
- a push: the hand selects, once. That an open push on a run is sent the
  removal of a place when a key that holds no row is taken out was run on a
  stand-in table by the check of this part's plan, not on the store's module;
- a run of very many places, what the one read more costs a gate, and a value
  whose digest another value shares;
- a reader that asks through layers, and whether an asker may see a layer: no
  read path is built in this part;
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
the door, so that a check knows what its who's stock holds. The proof opens five
doors on the module: the door of most checks; a second whose wait for locks is
400 ms; one with four of its numbers given; one for whos that cannot be the key
of a stock; and one near the end, which stands for the door of a process that
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
   above zero, which is one read, a bound greater than any wait that the door
   counts, which throws nothing, and an answer for a thing that is no act-id,
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
   refused under its own act-id. The stock of a who whose one request was
   refused has left the door's memory; the stock of a who that was given locks
   stays.
   And a holder of a permission that says see, in a check of its own: Alice
   gives one through the door; its holder sends a value; the door asks for locks
   under her who and her permission, a gate refuses that request with its word
   for a permission that does not allow acting, and the door gives its own word
   with that answer and appends nothing for her act. Her act with no value needs
   no lock, goes in, and is refused under its own act-id with the same word.
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
    that refusal to nobody and is handed the refusal of its own request. A stock
    that holds an open request stays in the door's memory.
12. The seat and the wait, on the door whose wait is 400 ms. A value act whose
    `:session` does not come back when it is read holds no seat: another maker of
    that who and layer gets its locks meanwhile. The append of a request that
    outlasts the wait is followed by no read: the word names the request, which
    stays open, and the next send uses it. A second maker that waits for that
    stock's seat meanwhile comes back at its own wait, before that append
    returns. A wait that ends after the call read
    an earlier call's refused request to its end: the word names no request, and
    the send after it is handed the refusal of its own. And a request in doubt
    whose append truly failed, met 200 ms after the throw: the send reads it
    until the door's wait has passed since the throw, forgets it, asks anew
    inside its own wait and goes in.
13. A request's answer with another digest: the door does not take it for its
    own request's, whatever its verdict, reads no lock, and gives the word with
    "not mine"; the send after it asks anew.
14. An act over three runs: the stock holds 1 lock, the door reads the 4 of a
    request that an earlier call left open and asks for 10 of its own; the act
    names the locks of three requests, each a run of neighbours.
15. The opener's numbers: with the fewest at once 2, the most at once 3 and a
    read each 100 ms, the requests of one who ask for 2, 2 and 3 and a wait of
    250 ms makes 4 queries; a wait for locks greater than any that the door
    counts opens a door that works.
16. A who that cannot be the key of a stock: one that throws when it is hashed,
    and one that throws when it is compared with a who that the door holds a
    stock for. Each ends in the door's word, at `:who`, and no call throws.
17. An append that threw: before the thing left the process, by a real throw; and
    one that went through, on a thread whose interrupt mark the proof set, where
    the log holds the record, its digest is the word's, and the answer is
    accepted.
18. A lock never crosses a who: a second holder of a layer, and one who in two
    layers, each get a request of their own while the first stock holds locks.
19. Many makers at once: 16 threads for one who, 25 acts each of 1 to 3 values,
    each accepted, no lock named twice, each act naming one request or two, each
    request held to the size rule; then 8 whos, each on 2 threads.
20. The base through the door: its making act, with the microbatch depot's ack
    and a wait; a value act, whose request is sent and answered inside the call,
    with the calls of Rama's client in their order and the first read of the
    request's answer one interval after its append; 4 acts of one who from 4
    threads while the gate is paused, with one request in the log; 8 acts on 8
    entities in one batch.
21. A wait for locks that passes, for a request that its depot acked while the
    gate was paused, with a second maker that waits for the same stock; when the
    gate goes on, the next send uses the same request.
22. A request in doubt in the base, met at once by the next send: its first read
    finds nothing, since the request's batch is not closed; the door keeps the
    request, reads again each interval, and uses its locks, with no second
    request.
23. An act-id given back with the other depot word: the thing goes to the depot
    that it names and that gate refuses it; with a value, the door's request is
    refused there, and no lock of the layer's own depot is taken.
24. The module is started again under the opened door, by Rama's module update
    with the unchanged module: an answer from before is the same, and a lock that
    the door held before seals a value after.
25. The door's memory starts again: a new door holds nothing and asks anew. Then
    the rows that seal nothing are counted in both records, and the count is held
    against its sources: the unused locks in the stocks of the four doors that
    hold locks, the locks that the proof itself left outside every stock, and the
    locks that were taken for acts that were not accepted.
26. Over the whole run: every lock's bytes, read round the door, are in no record
    of either log; no result that a door gave holds a run of bytes; a lock never
    crosses a who, over every record of both logs; over every stock in the doors'
    memory no call is counted in, no seat is taken and no request is open; the
    door made no stray; and each of the door's fourteen words was given.

### Where the proof stands in for a call of Rama's client

Clojure lets a proof put a function of its own in the place of Rama's client
calls `foreign-append!` and `foreign-invoke-query` for a while. The proof does so
for six things, and each `SHOWN` line that rests on it says so:

- to count the round trips of a call of the door, or to note the moment of each
  (groups 2, 3, 8, 10, 11, 12, 15, 20 and 22): the function counts or notes and
  then calls Rama;
- to make a read throw (groups 11, 12 and 14): no real throw of a query was found
  that the in-process cluster gives without a fault of the cluster, because the
  door tests each thing that it passes to a read before it calls;
- to give the locks of one read back with a way of sealing that the code does not
  know (group 11): the function calls Rama and changes what it gave;
- to set the interrupt mark of its own thread at the append of one request
  (groups 10 and 22): the function sets the mark and then calls Rama, and the
  throw is Rama's client's;
- to make one call slow, so that it outlasts the door's wait (group 12): the
  append of a request, and a read of an answer; the function sleeps and then
  calls Rama;
- to give the answer of one request back with another digest (group 13): the
  function calls Rama and changes what it gave.

The other throws are real: a module name under which no module runs; a thing
that holds a plain Java object, which Rama's client cannot turn into bytes; and
a call on a thread whose interrupt mark the proof set. The proof never destroys
the module to make a call throw, because the close of the cluster then throws.

Two parts of a maker's act are objects of the proof's own making (groups 12 and
16): a `:session` whose read blocks until the proof lets it end, which stands
for a part that does not come back; and a `:who` that throws when it is hashed.

### What it changes outside the door

It appends to both depots of its own cluster through the door, and so writes
both records. Round the door it appends two things with a handle of its own: the
hand's request for locks and the hand's act, for group 5. It pauses the
microbatch gate with Rama's test tool three times and lets it go on, and it
starts the module again once. It sets the interrupt mark of three threads of its
own, and interrupts a fourth while it waits. It holds one thread of its own in
the read of a maker's act for about 200 ms and then lets it go.

### The door as the module's one client, and two proofs

In the runtime the door is the module's only client. Two proofs are not held to
that. The module's proof does not use the door at all: its hand appends to the
depots and reads the queries itself, which is what keeps the store proven without
the door. The door's proof sends through the door, and beside that it holds
handles of its own: it reads the two depots' logs, the answer of a request and the
locks of a request by the module's queries, each lock's row by a select of the
gates' PStates, and the strays; and it appends the two things of group 5. A maker
has none of these ways. It also reads the door's own map of stocks from the
field of the door's type, for what no call of the door shows: that a stock has
left the door's memory, that a call was counted out, and that a seat is free.

### What the door's proof does not reach

- a throw of a query that Rama's client makes by itself;
- an append that threw for a fault of the cluster and went through all the same:
  the one that the proof reaches is an append on a thread that it interrupted
  itself;
- a request in doubt whose append truly failed, met by a call at once after the
  throw: that call reads until the door's wait has passed since the throw, which
  is the moment when its own wait ends, so it ends in the word and the call
  after it asks anew. The proof meets such a request 200 ms after the throw and
  after the whole wait; the request in doubt that it meets at once is one that
  went through;
- a worker of another process, and a thing of a type that it does not have;
- a real cluster, more than one machine, replication, and a module that is
  started again there. That one set of handles serves many threads is shown for
  one process, by one run each time, and Rama's documentation does not say it;
- how fast the door is: the in-process cluster's timings say nothing about it;
- a wait that outlasts its bound by the time limit of a query, and a call in a
  stock's seat that outlasts the door's wait by a call that Rama's client itself
  holds: the one that the proof reaches is an append that it made slow itself;
- a gate that is so slow that a request stays open over many calls;
- two takes of one lock at the same instant, forced: 16 threads take from one
  stock and no lock is named twice, in one run each time;
- a call that leaves a stock that holds nothing at the instant when another call
  enters it, forced: the proof reads the door's map of stocks between calls. The
  count of group 25 would be short if a stock with locks were lost;
- a who whose call cannot be counted out of its stock: one that throws only when
  it is compared with a who that came into the door's memory after it;
- an act with a value whose append threw: the locks that it took are then named
  by no record, and the count of group 25 has no such act;
- facts with no end that hold fewer than 257 values, and any other part with no
  end, for which a send does not come back: the proof's part of that kind is one
  whose read blocks until the proof lets it end;
- a count of act-ids that the memory of the process does not hold.

### The checks that rest on a time

A pass of each of these holds for calls of Rama's client that take a few
milliseconds, as they do on the in-process cluster. If one fails on a slow
machine, read its numbers before the door.

- A wait of 600 ms with a read each 200 ms makes 4 queries (group 3), and a wait
  of 250 ms with a read each 100 ms makes 4 (group 15): each read must take less
  than the rest of its interval.
- After longer than the door's wait, a send of 2 appends and 3 queries comes back
  in less than 400 ms (group 10).
- A request in doubt met 200 ms after the throw, on the door whose wait is
  400 ms (group 12): the send must ask anew and get its locks in the 200 ms that
  are then left of its own wait.
- An append and a read that the proof makes take 600 ms outlast a wait of 400 ms
  (group 12); and a second maker, which starts 50 ms after the first, is back
  from its wait of 400 ms inside 540 ms, before that append returns.
- The first read of a request in the base comes 195 ms or more after its append
  (group 20): the door sleeps 200 ms, and the JVM's sleep can end a little
  early.
- A request in doubt in the base, met at once, has no answer at the first read
  (group 22): the request's batch takes most of a second to close.
