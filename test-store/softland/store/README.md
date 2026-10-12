# Store proofs

[Parent: JVM checks](../README.md) · [Verification map](../../README.md).

| File | What it checks | State that it owns or changes |
|---|---|---|
| [act_proof.clj](act_proof.clj) | [The act](../../../src-store/softland/store/act.clj): its declared shape and rules, and the check that reads them. It needs no cluster. | One atom: the lines that it showed. |
| [pure_proof.clj](pure_proof.clj) | The gates' pure namespaces, beside the act: the one text and the digest, the door's declaration, the stamp, the first act of a layer, the record's rows, the checks of the decision, and the pure code of a batch of the microbatch gate. And [the door's](../../../src-store/softland/store/door/README.md) three pure namespaces: its words, what it makes of one act, and the numbers of its wait. And the source of the store's tree, read as data. It needs no cluster. | One atom: the lines that it showed. It takes orders of arrival from a random source with a fixed seed. It reads the source files of the store's tree and writes none. |
| [module_proof.clj](module_proof.clj) | [The store's module](../../../src-store/softland/store/README.md) on Rama's in-process cluster: the two gates, their records and the two reads. It plays the door by hand, and, where it reads a fact's row or a run of the facts by value, a reader of the tables. | It starts two clusters in its own JVM, one after the other, and closes each. For the second alone it puts three functions of its own into the vars of three functions of the gate's pure code, and puts the store's back when that cluster is closed. Its atoms: the lines shown, the things sent to each depot, the first answers of each gate, what later checks come back to, and, for the second cluster, the note of each attempt of a batch, the wish for a throw and each throw made. |
| [door_proof.clj](door_proof.clj) | [The door](../../../src-store/softland/store/door.clj) on Rama's in-process cluster: acts into both depots through the door's five calls, and their answers back. The proof is the door's one maker. | It starts one cluster in its own JVM and closes it. It opens more than one door on the module. Its atoms: the lines shown, each result that a door gave, and what later checks come back to. |
| [read_path_pure_proof.clj](read_path_pure_proof.clj) | [The read path's](../../../src-store/softland/store/read_path/README.md) five pure namespaces: the form of a query and its check, the layers, the sources, the answer and the steps of a live read, the words; and the six functions of the record's rows that only a read needs. It needs no cluster. | One atom: the lines that it showed. Its row makers remember each row that they made. |
| [read_path_proof.clj](read_path_proof.clj) | [The read path](../../../src-store/softland/store/read_path.clj) on Rama's in-process cluster: facts into both depots through the door, and back out through the read path's five calls, once and kept live. The proof is the read path's one asker. | It starts two clusters in its own JVM, one after the other, and closes each. For the second alone it puts three functions of its own into the vars of three functions of the gate's pure code, and puts the store's back. It opens one door and four read paths on each; and on the first, two read paths more, each with one small number, for the group of the read path's own bound. Eight atoms: what it holds of the cluster, the lines shown, each permission that it made, each thing that the door sent, each live read that it opened, and for the second cluster the rows of each attempt of a batch, whether a throw is asked, and how many were made. |
| [read_measures.clj](read_measures.clj) | Nothing: it is a run that is no proof. It measures the gates under acts, with and without live reads, the opening of a canvas, and how late a push is, and holds nothing to a number. | It starts one cluster in its own JVM and closes it. It opens one door and two read paths. It reads the clock and the heap. |

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
   and a mark are no parts; the version and the four limits, and that the shape's
   number for a permission is the value's own. And that the check reads
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
   carried stamp, and the depth of a permission: one of depth 32 passes and one
   of depth 33 is refused as malformed, each with the slot and without it, since
   the slot does not count toward the depth. And the three forms of a
   permission, each under an act that the check takes.
7. Ids and values: an id that is a UUID passes, and one that is a string fails;
   a key that is a UUID fails; a retract is a fact with no `:v`; what is a
   value, and its one normal form; the shapes of an act-id, a fact id and a
   permission id, the last in its three forms, and at depth 5 and 32, which have
   the shape, and at depth 33, which has not.

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
   the digest of `#unreadable`. One act built twice has one digest. A thing that
   holds a run of bytes, which is no value, has a text and a digest all the
   same.
2. The declaration. The door's five shapes and its rules, each written out
   a second time as literals; `act.clj`'s shapes inside the door's map as
   `act.clj` declares them; the seven limits, the bound on one value and the
   bound on one act among them; the store's namespace and the five
   store keys, with the shape of the value of each, which is a setting and which
   takes a second write; the words of a stray. The test for a stray, one thing
   for each word. The one form on arrival, each fact with the text of its
   value. A fact under each of the five store
   keys passes, with a right plain value; so do the values that the three keys of
   the read build take, an empty list and an empty set among them, a fact with
   no value under each, and a permission fact with the slot. A tool's
   own fact passes with its plain value, as a fact under a plain key, when its
   key is a bare word that the store's keys were (`:class`, `:permission`,
   `:lock-request`), the bare word `:depot`, one of the store's words in another
   namespace, or in a namespace that only begins with `softland.store`.
3. One refused thing for each fault, with its word; a key in the store's
   namespace that the store does not know, with a value and with none, and
   `:softland.store/lock-request` as one such key; a fact that carries `:sealed`,
   and one that carries `:lock-id`, each refused as a part that the form does
   not name; each wrong value under the three keys of the read build, 65 layers,
   33 marked keys and a mark of a key of the store's own namespace; a long id in
   each place where an act holds an id, in a
   list of layers, in a set of marked keys and above a permission with the slot;
   each limit at its edge. Among the edges: a value whose text is 65,536 bytes
   passes and one of 65,537 is refused, counted in bytes with a string of
   three-byte characters; an act at the bound on one act passes and one a byte
   over is refused; a permission fact of depth 5 passes and one of depth 33 is
   refused as malformed. The proof reads the reason words from the
   declarations, and each word must come from a thing that earns it.
4. The maker's check inside the door's: for each fault of the maker's form, the
   door's check gives the word that `act.clj`'s own parse gives.
5. The order of the reasons: a thing with two faults gets the word of the part
   that is declared first, of the shape before a rule, of the earlier rule. The
   step that makes the texts is the last: a value over its bound beside a place
   named twice gets the word of the place, and a value over its bound beside an
   act over its bound gets the word of the value.
6. Nothing throws: things that are no acts, facts that throw when they are read.
7. The stamp: the packing; the next stamp when the clock is ahead, stands still,
   goes back, and when one tick has more decisions than the counter holds; a
   carried stamp; the 10-second rule at its edge; the year in which the packing
   reaches 2^62.
8. The first act: the depot that a layer starts with; what a layer starts with
   where no fact says who may see it, what it builds on or which keys it marks;
   acts that have the form of a making act, and one act for each way of not
   having it, a permission with the slot among them.
9. The record's rows: the keys and their order; each field of each row of
   its declared class. A fact's row holds the text of its value as a String, as
   the declaration's check made it, and gives the UTF-8 bytes of that String;
   who wrote it and the permission that the act named. The row's value is held
   against a text that the proof writes out, and not against what the store's
   own function makes of the value. A retract's
   row holds no value; the row of the marked keys holds the stamps that the
   gate derived. What each row says: the fact that wrote it, whether it holds a
   value, its first form, who and the permission read back, and for a reader
   one of four: no fact, nothing here, the value, and a String that is the text
   of no value, on a row that the proof writes by hand. The facts by value: the
   value digest, a key, the two ends of a
   run, that no run holds a key of another value, layer, key or stamp for six
   kinds of value, the two texts of a UUID, and a key back to its parts. The
   heads of the two reads.
10. The decision: the standing of an act in each state of its layer, and what its
    permission allows: a holder of see who names her permission as the row has
    it, without the slot, and with see and act; each word of
    the checks of the places, each on rows built as the gate
    writes them; for each of the three keys of the read build a first write, a
    second, one that names the fact there, a stale one, a retract and another
    entity; who may write them; that a tool's own fact under the bare word
    `:class` is no
    setting, and one under `:permission` no permission fact; the stamp of a
    decision; the rows that a refusal, an accept, a
    making act and a write of the marked keys write, where an accept of one
    value writes three rows, the fact, the answer and the last stamp. And the
    order by value,
    each act decided as steps 6 and 7 of the streaming gate decide it against a
    record in memory, with the text of each value as the declaration's check
    gives it: a mark, a fact, the mark taken off, the fact replaced, the
    mark put on again, the fact replaced twice; a fact replaced with `:replaces`
    and without, written again with the same value, retracted; each kind of
    value; a mark and a fact under it in one act; which acts read the marked
    keys.
11. The batch, function by function. What the microbatch depot derives from a
    thing, never throwing. The turn, the base and the stamp of a turn. The
    clock of a batch: the record that the gate appends; the clock that a kept
    row gives, only to an attempt of the row's own batch; and the rule by which
    the row is kept, first write wins, for a record handed over twice, for two
    records of one batch with two clocks in either order, and for a record of
    a closed batch that comes late. Each of the three is also given things that
    are no row and no record, and none throws. The
    standing that step 1 can tell, a permission that says see among them, and
    the questions of an act, which hold the layer's marked-keys place and the
    fact's key; a fact under one of the five keys whose row every task holds
    asks nothing. The marked-keys places that a task reads, the reads of a task
    with and without a marked key, and the last stamp that a batch can hold. The
    replies of a
    home task about an answer and a place: under a
    marked key a reply holds the key's stamp, the row goes to each asker that
    does not have the place as its own, and the place is shared whenever two
    acts name it. Who goes to
    the meeting task: one case for each of the four rules, and the acts that
    stay, an act with a tool's own facts under the bare words among them. The
    home of each row of a decision: a
    fact under each of the five keys on every task; a row of the facts by value
    on the task of its value digest; a
    tool's own fact under a bare word on its entity's task alone. The rows that
    the meeting task reads from its own copy. What a task writes at once and
    what it sends; that a pass sends one row for a key of the facts by value,
    the last, which can be a removal. The routing id, the head of the read
    by routing id, and
    a row of a closed batch.
12. A batch is decided in one order. The proof runs whole batches in memory: the
    gate's four steps over the pure functions, each task's rows in its own map,
    each message gathered on the task of its route. It runs each batch in 200
    orders of arrival, drawn with a fixed seed, and compares the record after
    each with the record of deciding the same things one at a time in the order
    of their turns, by the checks of `softland.store.gate.decision` in their order. One
    batch is into a base that is made: two acts that replace one fact from two
    tasks, two that write one place, a give and an act under it in both orders,
    a repeat, one
    act-id under two things, acts for a layer of the other gate, strays, acts
    with no standing, and an act on three tasks accepted and one refused whole.
    The other is into a base that is not
    made, with two making acts. What is compared: each answer, each fact row on
    one task or, under one of the five keys, on every task, and each row of the
    facts by value on the task of its value digest.
13. A batch under marked keys, run in memory in the same 200 orders. The model
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
14. A batch run as two attempts. The batch of group 12 with one act more, at
    the edge of the 10 seconds, is run twice in memory from the same record
    before it. With one clock no row differs between the two attempts, in any
    field; the count of the rows that were compared is held beside it, so that
    an empty batch does not pass.
    With a clock one second later each answer and each fact that the batch
    wrote has another stamp, and the act at the edge is accepted where it was
    refused; each other answer keeps its word. That is what a kept clock gives,
    as a run of the pure code.
15. The door's words. The table of the door's own seven words, each with
    whether nothing was appended for the act or the door does not know, written
    out a second time. No word is a reason word of the act or of the declaration,
    or a word of a stray. A word as a maker gets it. The four answers from what a
    read by act-id gave: accepted and refused with the stamp, nothing yet, not
    mine for another digest and for no digest. The two acks.
16. What the door makes of one act. Things that end in one of the door's words:
    a thing that is no plain map; a sorted map whose keys cannot be compared
    with a keyword; no layer, a layer that is no id, a layer of 257 characters,
    an act-id of another layer; `:act-id` or `:depot` already there. Every other
    thing goes in, and the declaration's check then has the gate's word for it:
    a value that is no value, a fact that is a record with a value, a fact that
    holds `:sealed` or `:lock-id`, 257 facts with values. An act-id given back:
    of the layer, of another layer, not of the shape, with the other depot
    word. The thing that is built: `:facts` is the very object that the maker
    gave, for a vector, a list and a lazy sequence, and the building does not
    walk the lazy sequence; each fact is the maker's own object, a fact with a
    value too; the declaration's check takes it; it is no stray. The reading
    comes back for a thing whose facts have no end, for a who that throws when
    it is hashed and for a permission that throws when it is read: the door
    reads none of them. The digest of the thing with its facts as a list is the
    digest with the same facts as a lazy sequence, and is not the digest with
    them as a vector. The count of a series of act-ids.
17. The door's wait. The door's one number with its default, a number that an
    opener gives, and what is no such number, which gives the default; a wait
    that the opener gives is cut to the longest that the door counts.
18. No code of the store names a lock. The proof reads each source file of the
    store's tree as data, and finds no symbol and no keyword whose name holds
    "lock", "seal" or "stock", but Clojure's `locking`.

### What the pure proof does not reach

- a PState, a depot or a query: each row is a value of this process, and the
  walk of one event, which reads rows between the checks, is the gate's. The
  cases of the order by value walk steps 6 and 7 of the streaming gate with a
  function of the proof's own, on a record in memory; that the gate's dataflow
  makes those reads and writes is the module's proof's to show;
- a collision of the value digest: two values with one SHA-256;
- Rama in the run of a batch in memory: the proof gathers and delivers the
  messages itself. That the microbatch gate's dataflow calls the pure functions
  in that way is the module's proof's to show;
- a thing after Rama serialised it;
- a clock: each case gives the time as a number. That the microbatch gate
  keeps a batch's clock, and gives a second attempt the first attempt's, is the
  module's proof's to show;
- how long a check takes;
- of the door: anything that needs a cluster, a clock or a second thread. The
  waits are the door's proof's to show, as far as it does;
- a docstring, a comment or a string that names a lock: the search of group 18
  holds the symbols and the keywords of the code, and it reads no page of
  explanation.

## The module's proof

Run it from the repository root:

```sh
clojure -M:store-module -m softland.store.module-proof
```

The proof starts two clusters, one after the other in one JVM. Every group but
the last runs on the first, on the functions as the store defines them: nothing
is replaced in the JVM while that cluster runs. The last group, the batch that
runs a second time, runs on a cluster of its own, after the first is closed,
with three functions of [batch.clj](../../../src-store/softland/store/gate/batch.clj)
replaced by ones that give what the store's give and note it; the store's own
are put back when that cluster is closed. The count line is one: the counts of
the two runs, added.

On the first cluster the checks run in one order, and later checks build on the layers,
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
2. Layers are made. An act with a value into a layer that is not made is
   refused with the word of a layer that is not made, and writes no row. A
   person's
   layer is made by the system for that person, with the depot streaming. A
   group's layer is made by the person who creates it. A making act for a layer
   that is made is refused, and so is a second act of the system in a personal
   layer.
3. An act lands. One act with a value, read back by act-id with the digest of
   what was sent. Its row holds the value as a String, the one text of the
   value in its normal form, held against a text that the proof writes out; who
   wrote it and the permission that the act named. The record in the depot's
   log holds the value plain, as the hand sent it.
4. Refusals and repeats. A refusal recorded with its reason. A repeat answered
   from the record with the first stamp, and the task's last stamp unchanged. A
   second, different act under a used act-id, which reads an answer that is not of
   its digest. A stale `replaces`, and the current fact named.
   A fact that names nothing is never stale. A retract is a row with no value,
   which holds who retracted and under which permission, and a later fact
   replaces it. A place named twice.
5. Settings and permissions. A setting key on another layer. A permission for
   another layer. A permission that does not stand. A permission given beneath
   another by its holder, and an act under it. A give by one who does not hold the
   one above. A permission deeper than four: a give at each depth from 4 to 33,
   on each depot, of which the one at depth 33 is refused as malformed; an act
   under a permission of depth 5, and one under depth 32, whose row holds the
   whole permission; an act that names an own permission deeper than any that
   was given, and one whose own permission has depth 33, each refused; the slot
   at depth 32. `granted-in`. One write for
   a permission's place; the ending of a permission; a depot after the making.
   Whole or nothing: a give with a stale fact gives nothing. A tool's own facts
   under the bare words `:class` and `:permission`, on the layer's
   entity and on a holder's: accepted, each at its own place with the tool's
   value, and the layer's
   depot and the holder's permission are as they were. A key in the store's
   namespace that the store does not know: refused, with the refusal recorded.
6. Strays and limits. One stray for each word, kept once however often it comes,
   with no answer. A thing at and over each limit. A value whose text is 65,536
   bytes is accepted, and its row holds those bytes; one of 65,537 is refused
   with the word of a value that is too large. An act over the bound on one act
   is refused with its word. Each of the two refused things is in its depot's
   log whole, and each bound is held through each depot. A carried stamp too
   far ahead,
   and one 5 seconds ahead.
7. The reads take anything: what is no id, no act-id, another layer's act-id or
   the other depot gets nothing.
8. What a fact keeps. In a layer that the proof makes for these checks, an act
   with a value of each kind: a keyword id, a UUID, a string with spaces, a
   number, a map. Each row holds the one text of its value, held against a text
   that the proof writes out for each kind, and reads
   back as the value; the act in the depot's log is as the hand sent it, each
   fact with its value plain. The edges of a value: `false`, an empty string
   and an empty map are values and no retract, each with its text in its row; a
   value inside 32 collections is kept, and one inside 33 is refused as
   malformed. An act whose second fact names a stale fact to replace leaves no
   row: whole or nothing. Who wrote each row and under which permission:
   the maker of the
   layer; a second holder; a retract; a give; and the system's making of a
   personal layer, where the who is the system and the permission the person's.
9. The keys that take a second write. For who may see a layer, what it builds
   on and its marked keys, each: no row and what the layer starts with; a first
   write, a second, one that names the fact there; then a stale `replaces`, a
   retract and another entity, each refused with its word. The
   stamps of the marked keys, which the gate derives: the stamp of the marking
   act, none for the empty set, a new one when the key is marked again. A wrong
   value under each key. 64 layers and 32 keys. A list that names the layer
   itself, a layer twice and a layer that nobody made. A permission a second
   time and a depot after the making, still refused. A second holder of the
   layer writes who may see it. A setting into a layer that is not made, and a
   making act with a setting as a third fact, are refused, and nothing is
   written.
10. What a permission allows. A permission given with the slot that says see.
    Its holder acts, naming it as the row has it: refused, with a word of its
    own. She names it without the slot, and with see and act: it does not stand.
    She cannot give, and her act with a value is refused under its own act-id
    with the word of her permission, and writes no row. With a second
    fault she gets the word of her permission. A second give to her of see and
    act is refused, and so is one by another giver. A holder of see and act
    acts and gives. A making act whose
    permission has the slot is refused, and the layer is then made by a right
    act.
11. What points at a thing, in a streaming layer. A fact before the mark; the
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
12. After the module is started again. Rama's module update is run with the
    unchanged module: the memory of the old instance is gone, and the depot and
    the PStates stay. The answer, the depot and the strays are as they
    were, and so are a value kept plain, the layer's marked keys with their
    stamps and the run of a value; a repeat is still answered from the record;
    the next stamp is later than
    the last. During this step Rama prints some lines of its own log that are
    marked ERROR, while the new instance opens the depot's log and the client finds
    the new instance. They are not failed checks.
13. The stamps and the depot. The stamps of the answers increase strictly on each
    task, in the order sent. The depot holds each thing sent, as it was sent,
    each value plain. The layers are on
    more than one task.

Then the base and the microbatch gate. Groups 14 to 29 run before group 12,
group 30 after it, and group 31 last, on the second cluster:

14. The base is made. An act into the base before it is made is refused, and so
    is one sent to the streaming depot. Two making acts in one batch, their facts
    in the two orders: one is accepted. The depot is microbatch; the depot row
    and the system's permission row are on every task.
15. An act lands in the base. Eight acts of one batch on eight entities, whose
    rows are on more than one task. One act with a value, read back by act-id;
    its row holds the value as a String, the one text of the value in its normal
    form, who wrote
    it and the permission; and the record in the depot's log holds the value
    plain.
16. Closing. The proof watches the answers table itself for the row of a
    maker's act. At the first sight of the row, the read by act-id gives
    nothing; after the next run of the gate it gives the answer, with the stamp
    of the row that was in the table.
17. Refusals and repeats in the base. A refusal with its reason. A stale
    `replaces`. A repeat answered from the record. A different thing under a used
    act-id. One act-id under two things in one batch, on two tasks: one answer.
18. One batch, one order. Two acts of one batch that replace the same fact,
    arriving on two tasks: one winner, with the earlier stamp, and the loser
    refused whole. Two that write one place. A give and an act under it, in both
    orders.
19. A person acts in the base under a permission that the system gave, whose row
    is on every task. Acts with no standing. One write for a permission's place
    and for the depot. A
    key in the store's namespace that the store does not know, refused. A tool's
    own fact under the bare word `:permission`, on a holder's entity: accepted,
    its row on one task with the tool's value, beside the store's permission on
    every task. A carried
    stamp too far ahead, also under a permission that does not stand.
20. A long batch: 400 acts on 20 entities in one batch, each accepted, and on
    each entity the stamps go up in the order sent.
21. An act on several tasks. Refused whole: no row of it on any task. Accepted
    whole: each fact on its entity's task, one stamp on
    all of it, later than each row written before
    on those tasks; the gate's one last stamp. And an act that is decided where
    it arrived replaces the current fact of a place on another task: the row
    comes to it in a reply. Accepted; the same `replaces` again is stale.
22. Each gate tells the other gate's layers: an act for the base on the streaming
    depot, an act for a streaming layer on the microbatch depot, and one for a
    layer that is not made. One act-id on both depots has one answer. Strays of
    the microbatch gate.
23. What a fact keeps in the base: as group 8, on one entity of the base. A
    refused act leaves no row on any task. A person's rows
    hold her and the permission that the system gave her.
24. The keys that take a second write, in the base. The three first writes in
    one batch, the three second ones in one, the three that name the fact there
    in one: each accepted, and each row on every task, the same on each. Then
    one batch of things, each refused with its word: a stale
    `replaces`, a retract, another entity and each wrong value.
    The stamps of the marked keys on every task: the last stamp of the batch
    that marked the key, which is not below any stamp of that batch. A person
    with a permission in the base writes who may see it. A setting for the base
    sent to the streaming depot is refused as an act into a layer of the other
    gate.
25. What a permission allows, in the base. A give with the slot that says see,
    and an act of its holder in the same batch, which is decided on the meeting
    task against the row that the give wrote. In a later batch her act is
    refused where it arrives. Naming it without the slot, with see and act, a
    give of hers, her act with a value, and a second give to her.
26. What points at a thing, in the base: the walk of group 11, one act in each
    batch, on two entities that are on two tasks. Two places on two tasks that
    hold one value have their two rows on one task, which the hand finds with
    the value's digest as the partitioning key. Where an entry goes out, no task
    holds a row under its key. An act that arrived on another task than the
    place's replaces a fact under a marked key: the row before came in a reply.
27. One batch under a marked key. Two acts of one batch on one place, each with
    the place as its own: both accepted, the place holds the later turn's value,
    and the first act's value has no row on any task. A mark and a fact under
    its key in one batch: the fact is in no run, and its stamp is not later than
    the key's; a fact of a later batch is in the run. Two writes of who may see
    the base in one batch, from two tasks: every task holds the later turn's
    row.
28. The module keeps no lock. The cluster gives no table of locks and no query
    of a request's locks. Through each depot, a fact under
    `:softland.store/lock-request` is refused as a key of the store's namespace
    that the store does not know, and a fact that carries `:sealed` or
    `:lock-id` is refused as a part that the form does not name.
29. A place of the base never gives nothing. A fact with a value is written to
    a place, and an act that names it replaces it. From before that append until
    the new row shows, the hand selects the place every 2 ms, straight from the
    facts table on the task of its entity: each select gave a row, the fact
    before or the fact after, and never nothing; and the row at its first sight
    is the row that the place holds after the batch is closed. The line also
    says what the door's read by act-id gave at that first sight, which is not
    checked: it is nothing when the batch was not closed yet.
30. The base after the module is started again: the answer, the depot, the
    gate's last stamp, the strays, a permission's rows, the marked
    keys on every task, a run of the facts by value, a value kept plain and the
    row of the kept clock of the newest batch are
    as they were. A batch with things after the start keeps its clock under a
    later id. The gate's one throw is called by hand: given a kept clock it
    gives it, and given none it throws and names the batch. And, last of all, the stamps of the microbatch gate's answers go up in
    the order decided, each answer that a read gave was of a closed batch at the
    read, and the microbatch depot holds each thing sent, as it was sent, each
    value plain. The depot of batch clocks is read too: each record is a map of exactly
    a batch's id and a clock, two integers, all on one partition; and the kept
    row is the first record, in the depot's order,
    of the batch with the greatest id. For the third, the hand selects `:closed-through` on the answer's task
    right after each read that gave an answer; `:closed-through` never goes back,
    so a stamp above it then is a row that was read before its batch was closed.
31. A batch that runs a second time, on the second cluster. The group first
    makes the base there, marks a key, and writes a fact to
    replace, a value under the marked key and a person's permission, each in a
    batch that runs once. Then it sends its things in one batch, every one an
    act into the base, with a throw asked for: the first attempt fails in step
    4, after the one call that gives the rows of the acts that meet, when every
    row of the attempt is noted and nothing is committed; Rama starts the
    worker again and runs the batch a second time. The batch holds: facts with
    no value and with values on several entities, one act on two
    entities, a person's fact, a fact that names what it replaces and a second
    act on its place, a give and a give with the slot that says see, an act
    whose permission no act gave, a thing that the
    declaration refuses, a fact under the marked key over a value that goes
    out, two acts on one place under that key, a write of the marks, of who may
    see the base and of what it builds on, and, last, an act that carries a
    stamp at the edge of the 10 seconds. What it holds, in this order. One: the
    notes are two attempts of one batch: two notes, one throw between them in a
    call that gave rows, the same last stamp and the same numbers given to
    both, and another last stamp given to the batch before. Two: both attempts
    were given one clock, the clock of the kept row and of the batch's one
    record on the depot of batch clocks. Three: the second attempt began later
    than that clock, and made the same base. Four: the rows of the two attempts
    side by side give no key: the same keys, and at each key the same row in
    every field, the stamp too; a run of bytes, which an answer's
    row holds, is compared by its content. Five:
    each row that the tables hold after the batch, selected on every task, is
    that row, and no task holds a row under a key that went out. Six: each
    answer as `answer-by-act-id` gives it has the verdict, the word and the
    stamp of that row. Then the rows that the build of what the gates keep
    added: who wrote a person's fact and under which permission, the row of the
    marked keys with its derived stamps on every task, the give with its slot,
    and the runs of the facts by value. The check is shown to fail: the first
    attempt's note beside itself with one stamp moved, with one fact left out
    and with a row that goes out left out gives that key each time. Last, the
    depot of batch clocks: as many records as batches with things since that
    cluster's launch, which is the attempts noted less one; a quiet time of
    1.5 s adds none; a later batch adds one, under a later id; and each record
    is a batch's id and a clock and nothing else. During this group Rama prints
    lines of its own log that are marked ERROR, for the throw that the proof
    makes and for the worker that it starts again. They are not failed checks.

Each line that the contract asks for is on a `SHOWN` line in the contract's
words, with "depot" where the contract says "class".

Where the proof shows a row of the base, it selects it from the gate's PState
with the row's own route, or once on each task to see which tasks hold it. Such
a select goes round the two reads and is not held to `:closed-through`; the
proof makes it after the read by act-id gave the answer, which is after the
batch is closed, but for group 16, which uses it to look before.

The hand also plays a reader of the tables, since no reader is built yet: it
selects a fact's row, the row of a layer's marked keys and a run of the facts by
value, with the keys and the partitioning keys that
[rows.clj](../../../src-store/softland/store/record/rows.clj) gives. It opens no
push. For the base those selects go round `:closed-through` too.

### What in the module's proof rests on a time

Group 31 has three numbers of the proof. None decides whether it passes but the
second.

- The edge act carries a stamp 10,700 ms after the hand's reading of the clock,
  which the hand takes right before it appends that act, last. It is at the
  edge when the first attempt is given a clock less than 700 ms after that
  reading and the second attempt begins more than 700 ms after it. Then a
  clock read anew would have accepted the act where the kept clock refuses it.
  The check holds the act's word to what the 10-second rule gives against the
  first attempt's clock, whichever that is; its line says whether the act was
  at the edge in the run. On a machine where it is not, the group still passes
  and that one line shows nothing about the verdict.
- The hand reads for the answers of the batch that runs twice for 60 s at
  most. A second attempt came 1 to 2 s after the first; an attempt that Rama
  ends by its phase timeout takes about 31 s.
- A quiet time of 1.5 s, in which no record may come to the depot of batch
  clocks.

### What the module's proof does not reach

- an event that Rama runs twice, a batch of streaming events that fails, and a
  later act of a layer decided before an earlier one that failed. Group 11
  appends one accepted act a second time, which ends at the gate's read of its
  answer; that is a second thing under a used act-id, not Rama running an event
  again;
- that a streaming layer's fact and its rows in the facts by value have no
  moment between them: it rests on one event with no partitioner, read in the
  code;
- a push: the hand selects, once. That an open push on a run is sent the
  removal of a place when a key that holds no row is taken out was run on a
  stand-in table by the check of this part's plan, not on the store's module;
- a run of very many places, what the one read more costs a gate, and a value
  whose digest another value shares;
- a reader that asks through layers, and whether an asker may see a layer: no
  read path is built in this part;
- a commit that fails after one task showed its part, and the moment between
  two tasks' commits: a probe ran both on a module of its own
  (`src/proposal/backend-on-rama-2026-10-02/reads/batch-probe/`), not on the
  store's. The batch of group 31 fails while it is processed, so no row of its
  first attempt was ever in a table, and no reader could have been given one:
  what the group sets side by side is what each attempt made, as the noting
  functions were handed it, and what the tables hold after the second;
- that what the notes hold of the first attempt outlasts a worker: they are
  the memory of the proof's process, and they last through the restart only
  because the in-process cluster is one JVM;
- closing against a second attempt: it is shown for a batch that ran once, that
  a read gives nothing of it before the next run of the gate, and not that the
  other of two words for an act into a layer of the other gate stays unread;
- a failure of a batch before its clock is kept, a record that the topology
  which keeps the clock fails, and that topology not answering: a probe of the
  plan ran them on a stand-in walk, not on the store's module. The gate's one
  throw was never reached in the gate; group 30 calls its function by hand;
- a module update between two attempts of one batch: the second attempt then
  runs the new code, and can make other rows with the same clock;
- a second forced failure in one cluster: the group forces one, because each
  one takes Rama longer than the last;
- a layer's depot and a refusal's word read between a batch and its closing: the
  proof catches an answer there, and the depot read has the same test;
- a read that gave a row of a batch that closed in the moment between that read
  and the hand's select of `:closed-through`: the last check of the base takes
  it for a row of a closed batch;
- a pass long enough to be known to give the task back: 400 acts in one batch is
  the longest;
- a rebuild of a record from its depot;
- a worker whose process is killed, a worker that stops in the middle of an
  event, a move of a task's leader, more than one machine, replication, and so
  more than one copy of the kept clock's row and what the ack of its append
  waits for there;
- an append whose ack fails for an act that the gate accepted;
- a thing that a topology appends to another partition;
- the two aims of speed: the in-process cluster's timings say
  nothing about them. The measures of the read build ("The measures", below)
  print in-process numbers for a gate, and prove nothing;
- an update of the module on a cluster whose record another form of the module
  made: each cluster of the proof is launched with the module as it is, and the
  one update is of the module to itself;
- a thing nested so deep that a client writes it and a task cannot read it: the
  client and the tasks of the in-process cluster have one stack size.

## The door's proof

Run it from the repository root:

```sh
clojure -M:store-module -m softland.store.door-proof
```

The proof is the door's one maker: each act and each
answer goes through the door's five calls. The checks run in one order on one
cluster. The proof opens more than one door on the module: the door of most
checks; one that is opened with the opener's one number given; and one near the
end, which stands for the door of another process and reads the answer of an
act that the first door sent.

A pass shows the door in one process, with a maker that the proof plays. No
other maker uses the door yet.

What it checks, group by group:

1. The door is opened. A name under which no module runs gives the door's word,
   by a real throw of Rama's client. A door prints as the word "door" and the
   module's name, whatever prints it. A second door is opened with the opener's
   one number.
2. An act with no value. An act-id for a layer that is not made carries the depot
   that the layer starts with. The making act through the door, with the
   streaming gate's ack; its answer, accepted, with a stamp. The record in the log
   is the maker's act with `:act-id` and `:depot` and nothing else changed, and
   its digest is the digest that the maker got. Twenty act-ids of one layer with
   one query.
3. Round trips are counted: a first send of a value with no act-id, which is one
   append and one query; a send with its act-id given, which is one append; an
   answer with no wait, a wait of 600 ms that finds nothing, a bound that is not
   above zero, which is one read, a bound greater than any wait that the door
   counts, which throws nothing, and an answer for a thing that is no act-id,
   which makes no query. So every send is one append and at most one query.
4. A value is in the log as given. The record on the depot holds a value as its
   maker gave it, and not its normal form; the fact's row holds the text of the
   normal form; the digest of the record is the door's. An act with a store-key
   fact, a retract and a value is accepted.
5. The door and the hand send the same thing for one act with a value, a retract
   and a store-key fact: the door's record is equal to the hand's but for the
   uuid of the act-id, and the two act-ids differ, so the equality is of two
   records; both are accepted.
6. The digest that the door gives is the gate's, for facts given as a vector, a
   list and a lazy sequence: each is accepted under the door's digest, and the
   log holds a vector, a list and a sequence.
7. Things that are no act, one kind after the other. Each that ends in one of
   the door's words: the word, no record more in either log, no stray more.
   Those are a thing that is no map, a thing with no layer, a sorted map that
   cannot be read, a record, a layer that is no id, a layer that is too long,
   `:act-id` or `:depot` already there, and an act-id of another layer.
   Each that goes in: the gate's recorded refusal, read through the door. Those
   are a fact that is a record with a value, a fact with a value that holds
   `:sealed`, a value that is a ratio, a list or NaN, and 300 facts with
   values. A value of 65,507 characters is accepted: the edge of a value is a
   gate's, at 65,536 bytes of its text. The sends of a who that throws when it
   is hashed or compared are cases of this group.
8. The answer back: the four answers; nothing yet for a thing that is no act-id,
   with no query; not mine for a second, different act under a used act-id; a
   maker's own second send of one act with a value, which has the same digest,
   so that both sends read the answer as their own; a wait whose thread is
   interrupted; and the stamp of an answer used in `:based-on`.
9. A maker who may not act. A value act under a permission that was never
   given, and one into a layer that is not made: each is appended, is refused
   under its own act-id with the gate's word, and leaves no row; the log holds
   each value plain.
   And a holder of a permission that says see, in a check of its own: Alice
   gives one through the door; its holder sends an act with a value and an act
   with none; each goes in and is refused under its own act-id with the gate's
   word for a permission that does not allow acting.
10. Reads that throw: the read of a layer's depot and the read of an answer,
    each with its word.
11. The opener's number: with a read each 100 ms, a wait of 250 ms makes 4
    queries.
12. An append that threw: before the thing left the process, by a real throw; and
    one that went through, on a thread whose interrupt mark the proof set, where
    the log holds the record, its digest is the word's, and the answer is
    accepted.
13. Many makers at once: 400 acts on 16 threads, each accepted.
14. The base through the door: its making act, with the microbatch depot's ack
    and a wait; a value act, which is one record more in the base's log, by one
    query and one append; 4 acts of one who from 4
    threads while the gate is paused; 8 acts on 8
    entities in one batch.
15. A value of each kind through each depot. A value of each kind that the form
    takes goes through the door and each depot: it is accepted under the door's
    digest, and it reads back from its row as the value in its normal form.
16. An act-id given back with the other depot word: the thing goes to the depot
    that it names and that gate refuses it under its own act-id, an act with a
    value as an act with none.
17. The module is started again under the opened door, by Rama's module update
    with the unchanged module: an answer from before is the same, and a value
    act after is accepted.
18. The door holds nothing. A second door, opened on the module, reads the
    answer of an act that the first door sent, by the act-id and the digest
    alone.
19. Over the whole run: the door made no stray, and each of the door's seven
    words was given.

### Where the proof stands in for a call of Rama's client

Clojure lets a proof put a function of its own in the place of Rama's client
calls `foreign-append!` and `foreign-invoke-query` for a while. The proof does so
for two things, and each `SHOWN` line that rests on it says so:

- to count the round trips of a call of the door, or to note the moment of
  each: the function counts or notes and
  then calls Rama;
- to make a read throw (group 10): no real throw of a query was found
  that the in-process cluster gives without a fault of the cluster, because the
  door tests each thing that it passes to a read before it calls.

The other throws are real: a module name under which no module runs; a thing
that holds a plain Java object, which Rama's client cannot turn into bytes; and
a call on a thread whose interrupt mark the proof set. The proof never destroys
the module to make a call throw, because the close of the cluster then throws.

One part of a maker's act is an object of the proof's own making (group 7): a
`:who` that throws when it is hashed.

### What it changes outside the door

It appends to both depots of its own cluster through the door, and so writes
both records. Round the door it appends one thing with a handle of its own: the
hand's act, for group 5. It pauses the
microbatch gate with Rama's test tool and lets it go on, and it
starts the module again once. It sets the interrupt mark of a thread of its
own, and interrupts another while it waits.

### The module's two clients, and three proofs

In the runtime the module has two clients: the door, for the way in, and the read
path, for the way out. Three proofs are not held to that. The module's proof uses
neither: its hand appends to the depots, reads the queries and selects the tables
itself, which is what keeps the store proven without the door and without the
read path. The read path's proof sends through the door and reads through the
read path, and beside that its own hand selects one row
("The read path's proof", below). The door's proof sends through the door, and beside that it holds
handles of its own: it reads the two depots' logs, rows of the gates' PStates by
a select, and the strays; and it appends the one thing of group 5. A maker
has none of these ways.

### What the door's proof does not reach

- a throw of a query that Rama's client makes by itself;
- an append that threw for a fault of the cluster and went through all the same:
  the one that the proof reaches is an append on a thread that it interrupted
  itself;
- a worker of another process, and a thing of a type that it does not have;
- a door in another process: the door of group 18 is a second door in the
  proof's own JVM;
- a real cluster, more than one machine, replication, and a module that is
  started again there. That one set of handles serves many threads is shown for
  one process, by one run each time, and Rama's documentation does not say it;
- how fast the door is: the in-process cluster's timings say nothing about it;
- a wait that outlasts its bound by the time limit of a query;
- a thing that has no end in any part, facts with no end among them, for which
  a send does not come back: the proof sends none, since that check would not
  end;
- a thing nested so deep that a client writes it and a task cannot read it;
- a count of act-ids that the memory of the process does not hold.

### The checks that rest on a time

A pass of each of these holds for calls of Rama's client that take a few
milliseconds, as they do on the in-process cluster. If one fails on a slow
machine, read its numbers before the door.

- A wait of 600 ms with a read each 200 ms makes 4 queries (group 3), and a wait
  of 250 ms with a read each 100 ms makes 4 (group 11): each read must take less
  than the rest of its interval.

## The read path's pure proof

Run it from the repository root:

```sh
clojure -M:store -m softland.store.read-path-pure-proof
```

It runs on the alias that holds Clojure alone, so a pass also holds that the read
path's five pure namespaces load with no Rama. It takes a few seconds.

The tables are a value in this proof: a map from a gate and a table to a sorted
map from a row's key to the row. A small function of the proof's own stands for a
gate: it puts the rows of one accepted act in those maps, and keeps the order by
value of a marked key as a gate keeps it. No task is played: every part of a run
across the tasks of a microbatch layer holds the whole run.

What it checks, group by group:

1. The form of a query. Each of the four words, with each kind of end of a run,
   is taken, and comes out in its one form. Each fault is turned away with its
   word and the part at which the check stopped, and every word that the
   declaration holds is given once. The check never throws: on a thing that
   throws when it is read, on a list of layers with no end, on things that are no
   map. The read path's numbers take a default for each that is left out, and
   the seven defaults are written out a second time.
2. A word of a query is declared in three maps or in none: what it is, what it
   reads in one layer, how its rows are put together.
3. The layers. The order of a list, and of the list that a start layer gives;
   a start layer with no fact, not known yet, not made, not seen. The seven lines
   of the table of an asker's standing. What no fact reads as. What a layer's own
   rows mean for a query: a setting written again with the same word, and a list
   or a mark written in a layer that she may not see, mean the same; a setting
   that goes to "members" means another thing also when her standing stays as it
   was.
4. The sources of each word in a streaming layer and in the base: the table, the
   routing id and the path, line by line as the record's page gives them. Two
   askers of one thing who ask for different parts have equal sources.
5. The loop ends. It is played on the tables' maps: what is wanted, fetch what is
   missing, again. The rounds and the calls of each round are held for a place,
   an entity, a run and what points at a thing, asked once and kept live. A read
   asked once wants the fact with the rights; a live read does not; with the one
   predicate of that choice giving false, the read takes the rights first and
   gives the same answer.
6. The checks of nearer layers for what points at a thing: one in each layer
   nearer than the nearest order that holds the place, and in no other; none with
   the one function put aside; none past the build's number, which is one for a
   read asked once and another, far smaller, for a read kept live. The nearest row
   wins, and the place is given from it exactly when it holds the thing.
7. What a read gives: the four things that a place can hold, the fourth, a
   String that is the text of no value, on a row that the proof writes by
   hand, since no write of a gate gives one; the nearest layer
   wins, also with "nothing here"; a layer that is left out is named with why; a
   place-answer holds no writer and no permission unless asked; the store's own
   facts are in no answer that did not ask; a row that several parts of a run hold
   is given once.
8. Which changes reach an asker, and which places a change names for a query.
9. A step that joins or leaves a source that is no check owes the whole answer.
10. Every order of the callbacks. For a live read the proof holds a world: the
    tables, the writes that are to come, and for each push the value that it
    holds, the changes that it has not taken and the callbacks that are not
    handled. One move changes one thing: a write; a push takes its next value; a
    callback is handled; a push that the read asked for is served; its arrival is
    handled. A callback and an arrival are handled by the functions that the read
    path calls for them. The proof plays every order of the moves and holds, at
    each end, the asker's screen against what a read asked once gives from the
    tables as they are then. It plays: thirteen scenarios of what points at a
    thing through two, three and four layers; every list of up to three writes on
    one place in two layers, 672 lists; every list of up to three writes of a
    setting, a permission and two facts for a place through two layers, 620
    lists, and of up to two for an entity; and a start layer whose list drops a
    layer and takes it back while facts change. And it shows that the player can
    fail: with the read's one mark dropped, and with a step that counts a check
    which it has just left, the scenarios that need each end wrong.
11. The size rule of a run; the order of a table's keys, by code point; the two
    ends of a run; when the first read of a run can be made; the keys of a run
    held to its two ends; a lookup in a run whose map misses a key; two rows that
    are the same; which keys a change names.
12. The words: the read path's eight, what each carries, why a live read ends, why
    a layer gave nothing; the word of the read path's own bound on its pushes is
    given by a read kept live and by no read asked once; none is a gate's reason or a word of a stray, and one is
    spelled as a word of the door.

A pass shows the read path's rules on values of one process.

### What the read path's pure proof does not reach

- Anything that needs Rama: a select, a push, a callback, a thread;
- that Rama's client delivers the changes of two pushes in the ways that the
  player plays, and in no other: the probes of the read build ran that,
  in-process, on modules of their own
  (`src/proposal/backend-on-rama-2026-10-02/reads/plan-probes-read-path/` and
  `plan-check-probes-read-path/`);
- two live reads that share a push: the player plays one read;
- the tasks of a microbatch layer;
- more than four writes in one played world.

## The read path's proof

Run it from the repository root:

```sh
clojure -M:store-module -m softland.store.read-path-proof
```

It takes about a minute and a half. The proof is the read path's one asker, played by
hand: no view, no screen, no tool and no agent asks. Each act goes in through the
door, and each read goes through the read path's five calls. The checks run in
one order on one cluster, and later checks build on the layers that earlier ones
made. One check has a second cluster, after the first is closed.

A pass shows the read path in one process, with an asker that the proof plays.
No inhabitant uses the read path yet.

What it checks, group by group:

1. The read path is opened. A name under which no module runs gives its word, by
   a real throw of Rama's client. A read path prints as its name and nothing
   else, and a new one holds no live read and no push.
2. A thing that is no query gives the word `:read-the-query` from `read-once` and
   from `read-live`, with the check's word, and no call to Rama.
3. Each word of a query, asked once, for each gate: a place, an entity of five
   keys, a run across twelve entities, and what points at a thing. The answer
   holds the value that an accepted act wrote, the layer, the fact id that the
   door's act-id and the fact's index make, and the stamp of the door's answer.
4. The three things that a gate's write leaves at a place: no fact, a retract,
   and a value as its maker gave it. The fourth thing that a reader can be
   told, a String that is the text of no value, is the write of no gate; the
   read path's pure proof holds it, on a row written by hand.
5. A part of a row is handed over only when the query asks: neither, the writer,
   both. For the system's making of a personal layer the two differ.
6. The store's own facts are in no answer that did not ask for them. A place
   under a store key, asked without them, is turned away with no call.
7. Through layers: the nearest layer that holds a fact wins, also with "nothing
   here"; a run that three layers hold in parts; from a layer, with the newest
   list; a list that names a layer twice.
8. A layer that is left out: one that the asker may not see, one that is not
   made, a start layer that she may not see.
9. Who asks: a person who is not signed in; the system; no asker, both, an asker
   that is over the limit. And what the working answer for the store's own facts
   gives a person who is not signed in: a holder's permission in the base, which
   is everyone, and nothing in a members layer.
10. What points at a thing through layers, asked once: a nearer layer moves the
    thing, takes it away, moves it back; a nearer layer that does not mark the key
    writes another thing, then the same thing; a nearer fact from before its
    layer's mark; the farther layer moves away; the mark off and on.
11. The calls of a read asked once, counted: a place through three layers, from
    a layer, an entity, a run across the tasks of the base, what points at a
    thing through three layers. The same place while a live read holds a push
    open on each of its sources: the same calls and the same answer, since a
    read asked once reads the tables and takes no value of a push. And a call of
    300 paths, whose values come in the paths' order.
12. A very large run, on a read path that was opened with small numbers: a run of
    3, of 8 and of 12 places by its calls; a layer that she may not see; a whole
    layer; a read that needs more checks than one read may; a run of 8 places
    that is kept live and then grows past the number, which ends the read with
    its word and why; a source of a kind that the read path builds no path for.
13. A run across entities of the base, once and kept live: one push for each task;
    one write gives one change; a row that every task holds is given once.
14. Each word kept live, for each gate: the answer now; an act inside gives one
    change; an act outside gives none; an act in a farther layer under a nearer
    layer's fact gives none; twenty acts in quick turn end at the last value.
15. What shapes a read changes under it: a permission given; a setting to
    "everyone" and back; the start layer's list written again; a key marked, a
    fact under it, the mark off and on; a list and a mark in a layer that she may
    not see; a layer that joins over farther orders; the base to "members" and
    back.
16. One push for one thing: two askers of one place share its pushes, each is
    handed the parts that she asked for, and a push is closed when its last reader
    has left. 50 times two threads open the same thing at once; 50 times one joins
    while the last one leaves, and a write follows. The fields of what the read
    path holds for a live read and for a push.
17. The read path's own bound on its pushes, on a read path that may hold 3 on
    one table under one routing id. One live read of a place in a members layer
    holds 3: the layer's own rows, her permission, the place. A read of another
    place there is turned away with the read path's word, and no push is opened
    or closed for it; a read asked once of that place still answers; a second
    reader of the open thing joins and is not counted again; a read through
    another layer opens; when the readers have left, the read that was turned
    away opens. 20 times, eight threads each open a read of another place of
    the one layer at once: one opens, seven are turned away. The two tables of
    one layer are counted apart. An open read that comes to need one push more
    ends with the word and why. In the base a layer's own rows are counted
    under the layer and a place under its entity. At each of these moments the
    read path's counts are the counts of its map's entries. And where one read
    kept live may need 1 check, a read that needs 2 is turned away before a
    check is opened, and asked once it is given whole; and an open read that
    comes to need 2 ends with its word and why.
18. Nothing is shown that is not in the tables: a refused act at a watched place
    gives the asker nothing; each value that she was given is of an accepted act.
    When every asker has left, the read path's three maps are empty.
19. Reads that fail: a select that throws, and one that does not come back; under
    an open read, an opening of a push that throws, and one that does not come
    back; Rama ending a push; a function that throws, and a thing that is no
    function; `leave` of a thing that is no handle of this read path, and of
    the handle of another read path's read. A value that Rama sends
    anew after the first callback of a push: with the rows as they are the asker
    is told nothing, and with one row that differs she is told that one place.
    And 40 times, Rama's ending
    of a push while a read opens: either the read was not opened yet, `read-live`
    gives a word and the function is not called at all, or it was opened and the
    function is given the whole answer and then `{:ended}`; never `{:ended}`
    first or alone.
20. The function and its threads, under 200 acts from 4 threads: the first call on
    the caller's thread, the others on a thread of Rama's client, one at a time.
    And straight after the 200 acks a read asked once gives every place, through
    the read path that holds a push open on that run and through one that holds
    none.
21. One reader in the source, by a search of the store's source as text.
22. The order of the table's keys: entities with letters outside ASCII; an entity
    and a layer whose text is the start of another's; letters from U+E000 on and
    outside the first plane.
23. A function that leaves its own read, or another, inside its call; two reads
    that leave each other, 50 times; a function that asks inside its call.
24. The opening: 50 times a maker writes one place 20 times in quick turn while a
    live read opens on it. And a read of another asker while one asker's function
    stays for 2 seconds.
25. The first thing that a function is given: of every live read that the proof
    opened, the first call was the whole answer, on the caller's thread.
26. The module is started again under an open read.
27. On the second cluster: a batch of the base runs a second time under an open
    read of a place that it writes. The function is called once for that place,
    with the row that the first attempt made.

### How it shows that nothing came

A check that holds "the asker is given nothing" cannot hold it by looking at
once: a change that is still on its way would pass as none. So after the act that
must give nothing, the proof sends a second act that changes the same push, and
waits until a witness has that second change: a second live read of the proof's
that holds the same push and is told every change of it. The changes of one push
come in order, and one callback is handled for every read that holds the push
before the next callback begins. So when the witness has the second change, the
first one has been handled for the asker too. Groups 14, 15, 18 and 27 hold
"nothing" in this way.

### Where it stands in for a call of Rama's client

The read path's two reading calls are macros of Rama's that end in two functions.
The proof puts a function of its own in the place of those two, and of Rama's
close, for a while: to count the calls of a read (groups 2, 6, 11, 12, 16, 17, 23);
to keep each push that a read opened, and its callback (groups 16, 19); to make a
select throw, and to make one never come back (group 19); to make the opening of
a push throw, and to make one never come back (group 19). In group 19 the proof
makes two diffs of Rama's own classes, the one with which Rama ends a push and
the one with which it sends a value anew, and hands each to a push's callback:
Rama itself ended no push in a run, and since a fact's row holds a String it
sends a change as a diff of its own, so no check meets a value sent anew by
Rama after the first callback. It also puts a
function in the place of the read path's own function that makes the calls of one
round, to count the rounds (group 11), and calls two private functions of the
read path (group 12).

### What it changes outside the read path

It appends to both depots through the door, and nothing by its own hand. It
selects one row with a handle of
its own in group 22, to find the task of a layer. It writes the base's setting to
"members" and back in group 15. It starts the module again once, in group 26. It
opens two more read paths in group 17, each with one small number. For
its second cluster it replaces three functions of the gate's pure code, as the
module's proof does, and puts them back.

### What the read path's proof does not reach

- A cluster of several machines, a network between the runtime and Rama, a move
  of a task's leader, a killed process, a restart of the runtime;
- Rama ending a push by itself, and Rama leaving a reader out of a change and
  sending the value anew: group 19 hands the read path the diff that Rama's
  pages name for each. The value that it hands in with the second is the
  proof's own, and the push's value stays what Rama holds;
- the order in which two pushes call back: no check can force it on Rama. The
  pure proof plays every order on the pure functions; groups 14, 16 and 24 only
  make a crossing likely;
- a number of tasks that changes under an open read;
- many askers and many pushes at once: groups 16, 17 and 20 run a few threads.
  No check opens as many pushes as the read path's own bound at its default,
  900, or as Rama allows on one table on one task, 1,000 by default: group 17
  holds the bound with the number 3, the measures ask for 1,000 reads on one
  layer, and a probe of the build counted Rama's;
- that the read path's count stands for Rama's: the read path counts by routing
  id and one process, Rama by task and every client. Two routing ids on one
  task, and a second runtime, are not played;
- a run between two ends that hold a letter from U+E000 on, or one outside the
  first plane: the read path turns it away, because a range of Rama's between
  such ends gave, in runs of this proof, fewer places than the table holds;
- a failure of a batch while it commits, with one task seconds before another;
- a tool, a view, an inhabitant;
- how fast anything is.

### The checks that rest on a time

A check that waits for a change waits up to 15 seconds, and up to 30 for a change
of the base. A check that holds that nothing came waits a quarter of a second
after its witness has the second change. Group 19 holds that a select which does
not come back, under a wait of 300 ms, returns after 250 ms and before 3 seconds.
If one fails on a slow machine, read its numbers before the read path.

Group 19's 40 turns hand the ending in after 0 to 9 ms, so that some turns meet a
read that is not opened yet and some a read that is. The check holds each turn
to the one of the two right outcomes that it met, and its line prints how many
of each came. On a machine where every turn meets an opened read, the check
still passes and that line says 0 for the other: it then did not reach a read
that was not opened. With the function that ends a read put back as it was
before it took the read's turn first, the check failed in the turns that met a
read that was not opened: run once, by a scratch file of the build that changed
no source.

## The measures

Run them from the repository root:

```sh
clojure -M:store-module -m softland.store.read-measures
```

A run that is no proof. It prints each number on a line that starts with
`MEASURED`, with what it is a number of, holds nothing to a number, and exits 0
unless it cannot run. It takes about ten minutes; with the argument `small` it
runs each shape with a small load, in a minute and a half. A set of times is printed
as its count, its middle, its slowest 1 in 100 and its slowest.

What it measures: a gate under 100 acts a second, the streaming gate with a key
not marked and marked, and the base; the same with 1, 100 and 1,000 live reads
asked for on the layer, of which the read path's own bound on its pushes lets
898 open; the opening of a canvas of 5,000 facts, as a run and as what points at
the canvas, once and kept live, through one layer and through three, where the
last, kept live, is turned away by the bound on the checks of one live read;
what points at a canvas of 100 facts kept live through three layers, which
opens; and how late a push is, in a streaming layer and in the base, for a place
of the facts table and for a place of the facts by value.

What a number of it is: the cluster runs in the process of the measure, on one
machine, with every row in memory and no network. A time is a floor for a round
trip and says little of a seek on a disk. A real cluster, the base on one, a
network and a second node are not measured.
