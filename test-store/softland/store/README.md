# Store proofs

[Parent: JVM checks](../README.md) · [Verification map](../../README.md).

| File | What it checks | State that it owns or changes |
|---|---|---|
| [act_proof.clj](act_proof.clj) | [The act](../../../src-store/softland/store/act.clj): its declared shape and rules, and the check that reads them. It needs no cluster. | One atom: the lines that it showed. |
| [pure_proof.clj](pure_proof.clj) | The gates' pure namespaces, beside the act: the one text and the digest, the door's declaration, the time on the act, the stamp, the first act of a layer, the record's rows, and the checks of the decision, each alone and as the walk of one thing calls them, one thing after the other. And [the door's](../../../src-store/softland/store/door/README.md) three pure namespaces: its words, what it makes of one act, and the numbers of its wait. And the source of the store's tree, read as data. It needs no cluster. | One atom: the lines that it showed. It reads the clock for nothing and draws no random number. It reads the source files of the store's tree and of the proofs, and writes none. |
| [module_proof.clj](module_proof.clj) | [The store's module](../../../src-store/softland/store/README.md) on Rama's in-process cluster: the two gates, their records and the two reads. It plays the door by hand, and, where it reads a fact's row or a run of the facts by value, a reader of the tables. | It starts two clusters in its own JVM, one after the other, and closes each. For the second alone it puts two functions of its own into the vars of two pure functions of the store that the walk of one thing calls, and puts the store's back when that cluster is closed. Its atoms: the lines shown, the things sent to each depot, the first answers of the streaming gate, what later checks come back to, and, for the second cluster, the batch that is to fail and the events of the streaming gate that it watches, each with the calls that were noted for it. It reads the clock for the time that the hand writes on a thing, and to end a loop that watches a table. |
| [door_proof.clj](door_proof.clj) | [The door](../../../src-store/softland/store/door.clj) on Rama's in-process cluster: acts into both depots through the door's five calls, and their answers back. The proof is the door's one maker. | It starts one cluster in its own JVM and closes it. It opens seven doors on the module. It pauses the microbatch gate with Rama's test tool, and lets it go on each time. Three atoms: the lines shown, each result that a door gave, and what later checks come back to. It reads two clocks of its JVM and sets neither. |
| [read_path_pure_proof.clj](read_path_pure_proof.clj) | [The read path's](../../../src-store/softland/store/read_path/README.md) five pure namespaces: the form of a query and its check, the layers, the sources, the answer and the steps of a live read, the words; and the six functions of the record's rows that only a read needs. It needs no cluster. | One atom: the lines that it showed. Its row makers remember each row that they made. |
| [read_path_proof.clj](read_path_proof.clj) | [The read path](../../../src-store/softland/store/read_path.clj) on Rama's in-process cluster: facts into both depots through the door, and back out through the read path's five calls, once and kept live. The proof is the read path's one asker. | It starts two clusters in its own JVM, one after the other, and closes each. For the second alone it puts one function of its own into the var of one pure function that the walk of both gates calls, and puts the store's back. It opens one door and four read paths on each; and on the first, three read paths more: two, each with one small number, for the group of the read path's own bound, and one for the counts of pushes of the group of the base that is read where the asker's layer lives. Eight atoms: what it holds of the cluster, the lines shown, each permission that it made, each thing that the door sent, each live read that it opened, and for the second cluster the rows of facts of each decision, the place at whose decision a throw is asked, and how many throws were made. |
| [run_again_proof.clj](run_again_proof.clj) | That the store comes out the same when it is run again from its depots of acts. A rule that names the acts decided out of their layer's line, held on lines made by hand. Then, on Rama's in-process cluster: a second module that mirrors the two depots and runs the store's own gates over them from their beginning, every row of every table compared; the kept acts of [the fixture](run_again_fixture/README.md) fed to a fresh store with 4 tasks and to one with 8, held against the kept tables; and the run again after failures that the proof makes. | It starts four clusters in its own JVM, one after the other, and closes each. The first and the fourth also hold a module of its own, `StoreAgain`. For the fourth alone it puts three functions of its own into the vars of three pure functions that the walk calls, and puts the store's back. It opens a door, appends by its own hand, and selects every row of every table. Two atoms: the lines shown, and what the check after a failure plans and notes. A run of the proof writes no file; its mode `make-fixture` writes the two files of the fixture. |
| [read_measures.clj](read_measures.clj) | Nothing: it is a run that is no proof. It measures the gates under acts, with and without live reads, the opening of a canvas, and how late a push is, and holds nothing to a number. | It starts one cluster in its own JVM and closes it. It opens one door and two read paths. It reads the clock and the heap. |
| [gate_measures.clj](gate_measures.clj) | Nothing: it is a run that is no proof. It measures an act through the door and each gate, with no live read open, and holds nothing to a number. | It starts two clusters in its own JVM, one after the other, and closes each: the first with the module `Store`, the second with a module of its own, `StoreAtOne`, which is `Store` with one setting of Rama at 1. It opens doors and no read path, and no depot and no table itself. It reads the clock, and asks `git` which tree it is on. |

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
   words, read from them; that an act-id, a depot, a time of the door, a seal,
   a lock id, a layer and a mark are no parts; the version and the three limits,
   of the subjects, of the collections around a value and of a carried stamp,
   and that the shape's number for a permission is the value's own.
2. The check reads the declarations: for each part of an act and of a fact, a
   thing without the part, and a thing with a thing of no shape in it, get what
   the declaration gives.
3. Acts that pass, each shown in its one form: from the least act (version, who,
   layer, permission, session and one fact), through a retract, plain values
   under any key, ids that are UUIDs, a permission that a parent granted, a
   given permission that says what it allows, see and see and act, and an
   act that replaces a fact and says what it was based on, to an act with each
   optional part, given with subvectors, narrow integers and a list of facts. Its
   one form has only plain vectors and longs, and each one form parses again to
   itself.
4. One refused act for each fault, with its reason. Each differs from the least
   act in one thing. Among them, for the slot of a permission that says what it
   allows: a word in the place of the permission above, a third word in the
   slot, a permission in the slot, six items, and a slot after a first
   permission. The proof reads the reason words from the declarations, and
   each word must come from an act that earns it.
5. The order of the checks, which is the order of the declarations: an act with
   two faults gets the reason of the earlier part, and of the shape before a rule.
6. Nothing throws: the check, and the test of each declared shape, are given
   things that are not acts, a thing that throws when it is read among them, and
   each gives back data.
7. The limits at their edges: the subjects, the collections around a value, a
   carried stamp, and the depth of a permission: one of depth 32 passes and one
   of depth 33 is refused as malformed, each with the slot and without it, since
   the slot does not count toward the depth. And it prints the three forms of a
   permission, each under an act that the check takes; group 3 holds each as a
   case.
8. Ids and values: an id that is a UUID passes, and one that is a string fails;
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
content.

A thing on a depot holds the time on the act, and the declaration refuses a
thing with no usable one. So each thing that the proof's hand builds holds one
time, under the name that the door's declaration holds for that part; a thing
without it, or with another time, is built so on purpose.

Both gates decide a thing with one walk. Where a group decides things one after
the other, a function of the proof's own makes the reads, the calls and the
writes of that walk in its order, against a record in memory: what one gate
holds on one task, each table a map. What that shows is what the pure functions
give when they are called in that order.

What it checks, group by group:

1. The one text and the digest. One thing of each kind with its text. Pairs that
   no check can tell apart, and that a serialiser may change, have one text and
   one digest: an Integer and a Long, minus zero and zero, a subvector and a
   vector, a map built in two orders. Pairs that a check tells apart have two.
   The text of a value reads back as the value. The digest is the SHA-256 of the
   one text, with bytes fed as they are; a thing that throws when it is read has
   the digest of `#unreadable`. One act built twice has one digest. A thing that
   holds a run of bytes, which is no value, has a text and a digest all the
   same. The digest leaves the time on the act out, at the top level of a plain
   map and nowhere else: a thing with its time, with another time and with none
   has one digest; two times inside a value give two digests; a sorted map whose
   keys cannot be compared with a keyword, and a record with a field of the
   part's name, each keep a digest of their own.
2. The declaration. The door's five shapes and its rules, each written out
   a second time as literals; `act.clj`'s shapes inside the door's map as
   `act.clj` declares them; the name of the part that holds the time on the act,
   and the bound that a time must be below; the seven limits, the bound on one
   value and the bound on one act among them; the store's namespace and the five
   store keys, with the shape of the value of each, which is a setting and which
   takes a second write; the words of a stray. The test for a stray, one thing
   for each word. The one form on arrival, each fact with the text of its
   value. A fact under each of the five store
   keys passes, with a right plain value; so do the values that the three keys of
   the read build take, an empty list and an empty set among them, a fact with
   no value under each, and a permission fact with the slot. A tool's
   own fact passes with its plain value, as a fact under a plain key, when its
   key is one of the bare words `:class`, `:permission`, `:lock-request` and
   `:depot`, one of the store's words in another
   namespace, or in a namespace that only begins with `softland.store`.
3. One refused thing for each fault, with its word; a key in the store's
   namespace that the store does not know, with a value and with none, and
   `:softland.store/lock-request` as one such key; a fact that carries `:sealed`,
   and one that carries `:lock-id`, each refused as a part that the form does
   not name; each wrong value under the three keys of the read build, 65 layers,
   33 marked keys and a mark of a key of the store's own namespace; a long id in
   each place where an act holds an id, in a
   list of layers, in a set of marked keys and above a permission with the slot;
   each limit at its edge. A permission fact of depth 5 passes, and one of depth
   33 is refused as malformed. The least thing with each kind of time that the
   shape does not take, each refused with the word of a thing with no time of
   the door: no time, nil, a string, a double, a big integer, a time below zero,
   and a time at the bound or above it. The proof reads the reason words from
   the declarations, and each word must come from a thing that earns it.
4. The maker's check inside the door's: for each fault of the maker's form, the
   door's check gives the word that `act.clj`'s own parse gives for the maker's
   part, which is the thing without the three parts that the door adds. Each of
   the three is no part of the maker's act: `act.clj`'s own parse refuses the
   maker's part with any one of them put back.
5. The order of the reasons: a thing with two faults gets the word of the part
   that is declared first, of the shape before a rule, of the earlier rule. The
   step that makes the texts is the last: a value over its bound beside a place
   named twice gets the word of the place.
6. The time on the act. What the declaration takes as a usable time: a
   fixed-width integer of 0 or more and below the bound, given as a long
   whatever fixed-width class the thing held; at the edges of that range, the
   usable time and the check's word. A thing that is no plain map has no usable
   time, and neither has a thing that throws when the part is read; nothing
   throws. Where the time's word stands among the others: after the word of a
   part that the form does not know and of a depot that is absent or no depot
   word, and before the word of each of the maker's own parts and of each rule.
   The test for a stray reads no time.
7. The bounds: the two bounds of the check's last step, at their edges. A value
   under a plain key whose one text is 65,536 bytes passes, and one of 65,537 is
   refused with the word of the bound on one value; the bytes are counted in
   UTF-8, with a string of three-byte characters, and by a count of the proof's
   own. An act at the bound on one act passes, and one over it is refused with
   its word. A value over its bound in an act over its bound gets the word of
   the value.
8. Nothing throws: things that are no acts, facts that throw when they are read.
9. The stamp: the packing; the next stamp when the time on the act is ahead of
   the last stamp, is the same, is earlier, and when one millisecond has more
   decisions than the counter holds; a carried stamp; the 10-second rule at its
   edge, held against the time on the act; the year in which the packing
   reaches 2^62, the bound of the time on an act at half of that, and the
   stamp of an act with the greatest time and the greatest stamp that it may
   carry, which is 655,360,000 above 2^61. With no time the next
   stamp is the last plus one, the greatest carried plus one, or 0 with
   neither, and no carried stamp is too far ahead.
10. The first act: the depot that a layer starts with; what a layer starts with
    where no fact says who may see it, what it builds on or which keys it marks;
    acts that have the form of a making act, and one act for each way of not
    having it, a permission with the slot among them. Whether a layer starts
    with the other gate's depot: at the streaming gate's depot the base and no
    other layer, at the microbatch gate's depot each layer but the base.
11. The record's rows: the keys and their order; each field of each row of
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
    heads of the two reads. The routing id of an act-id, which is its layer,
    for an act-id of each depot; the head of the read by act-id with that
    routing id, and nothing with another; and an act-id's layer and uuid back
    from the text of its answer's key, for a layer that is a keyword and one
    that is a UUID.
12. The decision: the time that a gate decides with, which is the time on the
    thing, and which is in the stamp of an accept alone: a refused thing,
    whatever time and whatever stamp it carries, gets its layer's last stamp
    plus one, or 0 in a layer with no stamp. The standing of
    an act in each state of its layer, at the streaming gate's depot and, for
    an act into the base, at the microbatch gate's. A layer of the other gate,
    told from the gate's depot and the act alone, with no row of the layer: the
    base at the streaming gate's depot, and any other layer at the microbatch
    gate's, also when the act has the form of a making act of the other gate.
    What a
    permission allows: a holder of see who names her permission as the row has
    it, without the slot, and with see and act; each word of
    the checks of the places, each on rows built as the gate
    writes them; for each of the three keys of the read build a first write, a
    second, one that names a stale fact, a retract and another
    entity, and for who may see the layer one that names the fact there; who
    may write them; that a tool's own fact under the bare word
    `:class` is no
    setting, and one under `:permission` no permission fact; the stamp of a
    decision; the rows that a refusal, an accept, a
    making act and a write of the marked keys write, where an accept of one
    value writes three rows, the fact, the answer and the last stamp. And the
    order by value,
    each act decided as steps 6 and 7 of the walk decide it against a
    record in memory, with the text of each value as the declaration's check
    gives it: a mark, a fact, the mark taken off, the fact replaced, the
    mark put on again, the fact replaced twice; a fact replaced with `:replaces`
    and without, written again with the same value, retracted; each kind of
    value; a mark and a fact under it in one act; which acts read the marked
    keys.
13. The walk of one thing, in memory. What a gate may copy to other tasks: of
    the writes of a decision, each row of a fact and of the facts by value, in
    their order, a key that is taken out among them, and never the answer or
    the last stamp; nothing of a refusal. The store's function for it is in a
    namespace that requires Rama, so the proof reads the source file of the
    walk as data, makes that one function with Clojure's own names, and holds
    it against a filter of the proof's own, also over the writes of each thing
    of two lines that the proof decides in memory. A second run of one
    decision, at each gate: from the same record it gives the same writes in
    every field; on the record that the first run left it finds the answer
    under the act-id, writes nothing and leaves every table as it was. The
    stamp in a line: acts of one
    layer whose times rise, stand still and go back get stamps that each are
    later than the one before, in a layer of each gate; a stamp holds its act's
    time when that is later than the last stamp, and is the last stamp plus one
    when it is not. A mark holds from the next act, in a layer of each gate:
    the key gets the stamp of the act that marks it; a fact under the key in
    that act is in no run; a fact under it in the next act has its entry in the
    facts by value under that stamp.
14. A line is decided in the depot's order. The proof decides a line of things
    into a base that is made, in memory, one thing after the other in the order
    of the depot, each against the rows that the things before it wrote. In the
    line: two acts that replace one fact, where the first is accepted and the
    second is stale; two that write one place and name nothing, both accepted,
    the place holding the later one's row; a give and then an act under it,
    both accepted; an act and then the give that it needs, the act refused and
    the give accepted; a repeat; one act-id under two things, which has the
    answer of the first; two acts for layers of the other gate, each with the
    word of such a layer; strays, which have no answer; acts with no standing;
    an act with three values that is accepted whole, and one that is refused
    whole; an act that writes a place and names nothing, and then one that
    names the fact from before the line, which is stale; and two things that
    the declaration refuses. No refusal writes a fact. Each decision has a
    stamp later than the one before it in the order of the depot, a refusal's
    too. The last stamp is kept for a layer, so a decision in one layer does
    not raise the stamps of another. The time on the act decides at this gate
    too: an act that carries a stamp past the 10 seconds is refused with one
    time on it, and accepted with a time one second later. The line decided a
    second time from the same record gives the same record, with no row that
    differs in any field; that the rows compared are rows that the line wrote
    is held beside it, so that an empty line does not pass, and so is that the
    comparison tells a row whose value alone differs. Decided again on the
    record that it left, the line writes nothing. And three lines more: into a
    base that is not made, with an act before the making, two making acts and
    acts after; of strays alone, which writes no answer, no fact and no stamp;
    and of repeats alone, which writes nothing and makes no stamp.
15. A line under marked keys, decided in memory in the same way, into a base
    that marks two keys. In the line: two acts on one place under a marked key;
    the same value written again; a retract of a fact from before the mark and
    of one since it; a place that comes to hold the value of another place; a
    fact that names what it replaces; a write of the marks that takes one key
    off and marks a new one, then a fact under the new key and a fact under the
    key that was taken off, and a second write of the marks; a fact under a
    marked key that names a fact which is not current, refused whole; two
    writes of who may see the layer; two writes of what the layer builds on
    that name the same fact to replace; a holder of see who acts, who names her
    permission without the slot, who gives, and who sends a value under a
    marked key; a retract of the marks; marks on another entity. A key from
    before the line keeps its stamp, and each key that an act of the line
    marked has the stamp of that act: a fact under such a key in the act after
    is in the order by value.
16. The door's words. The table of the door's own seven words, each with
    whether nothing was appended for the act or the door does not know, written
    out a second time. No word is a reason word of the act or of the declaration,
    or a word of a stray. A word as a maker gets it. The four answers from what a
    read by act-id gave: accepted and refused with the stamp, nothing yet, not
    mine for another digest and for no digest. The two acks.
17. What the door makes of one act. Things that end in one of the door's words:
    a thing that is no plain map; a sorted map whose keys cannot be compared
    with a keyword; no layer, a layer that is no id, a layer of 257 characters,
    an act-id of another layer; `:act-id`, `:depot` or the part of the time
    already there, the last also with nil. Every other
    thing goes in, and the declaration's check then has the gate's word for it:
    a value that is no value, a fact that is a record with a value, a fact that
    holds `:sealed` or `:lock-id`, 257 facts with values. An act-id given back:
    of the layer, of another layer, not of the shape, with the other depot
    word. The thing that is built: the maker's act with `:act-id`, `:depot` and
    the time that the door hands to the building, which adds that time and
    tests nothing of it, each other part as given; `:facts` is the very object
    that the maker
    gave, for a vector, a list and a lazy sequence, and the building does not
    walk the lazy sequence; each fact is the maker's own object, a fact with a
    value too; the declaration's check takes it; it is no stray. The reading
    comes back for a thing whose facts have no end, for a who that throws when
    it is hashed and for a permission that throws when it is read: the door
    reads none of them. The digest of the thing with its facts as a list is the
    digest with the same facts as a lazy sequence, and is not the digest with
    them as a vector. The count of a series of act-ids.
18. The door's wait. The door's one number with its default, a number that an
    opener gives, and what is no such number, which gives the default; a wait
    that the opener gives is cut to the longest that the door counts.
19. No code of the store names a lock. The proof reads each source file of the
    store's tree as data, and finds no symbol and no keyword in which a word of
    the name, or of its namespace, begins with "lock", "seal" or "stock", or
    with "unlock" or "unseal"; Clojure's `locking` alone is let through. A name
    is cut into words at each character that is no letter, and before a capital
    that follows a small letter, so `clock` and `block` name no lock. A string
    and a comment are not code. The reading is held to have read the files: it counts the
    files and the names that it read, and the namespace of the act is among
    them. The proofs' own code is read the same way, against a written set of
    the names that are meant where they stand, such as a seal's part that a
    check sends to be refused: each name found is in that set, and each file
    was read.

### What the pure proof does not reach

- a PState, a depot or a query: each row is a value of this process, and the
  walk of one thing, which reads rows between the checks, is the gates'. The
  proof walks a thing with functions of its own, on a record in memory: the
  whole walk in groups 13 to 15, and its steps 6 and 7 in the cases of the
  order by value. That the walk on a cluster makes those reads, calls and
  writes in that order is the module's proof's to show;
- a collision of the value digest: two values with one SHA-256;
- the depot's order, and a second task: a line in memory is a vector that the
  proof writes, decided on one record. That a gate hands the walk the things
  of a partition in the depot's order, and that the microbatch gate copies the
  rows of an accept to every task, are the module's proof's to show;
- the var of the walk that a cluster loads: the check of what a gate may copy
  reaches the text of that function in its source file;
- a thing after Rama serialised it;
- a clock: each case gives the time as a number. That a door writes its
  server's clock on an act, and that no gate reads a clock, need a cluster. The
  door's proof holds the time that the door wrote, and the time in the stamp
  of an accepted act against it; the run-again proof holds that the gates give
  the same stamps when they are run again over the depots;
- how long a check takes;
- of the door: anything that needs a cluster, a clock or a second thread. The
  waits are the door's proof's to show, as far as it does;
- a docstring, a comment or a string that names a lock, a name that code makes
  while it runs, and a file that is no `.clj` file: the search of group 19
  holds the symbols and the keywords of the code under `src-store/` and under
  `test-store/`, and it reads no page of explanation.

## The module's proof

Run it from the repository root:

```sh
clojure -M:store-module -m softland.store.module-proof
```

The proof starts two clusters, one after the other in one JVM. The groups of
the first run on the functions as the store defines them: nothing
is replaced in the JVM while that cluster runs. The two groups that make a gate
fail run on a cluster of their own, after the first is closed,
with two functions of the store's pure code that the walk of one thing calls
replaced by ones that give what the store's give, but for what a group asked:
one of [decision.clj](../../../src-store/softland/store/gate/decision.clj),
which gives the rows that a decision writes, and
one of [declaration.clj](../../../src-store/softland/store/door/declaration.clj),
the test for a stray. The first throws once for one act of each of the two
groups, and hands the walk rows that no act writes where group 39 asks for
them; both note their calls. The store's own
are put back when that cluster is closed. The count line is one: the counts of
the two runs, added.

On the first cluster the checks run in one order, and later checks build on the layers,
the facts and the permissions that earlier ones made. The hand writes a time on
each thing, as the door does: a reading of the proof's own clock, or a time
that a check chose. A check of a stamp chooses the time, in both gates, so that
the stamp is a number that the proof makes itself from that time. The hand
reads each answer back through the module's own queries, by act-id, with the
act-id's layer as the routing id, for both depots. For a streaming layer it
reads after an append that waited for the gate. For the base no ack carries an
answer: the hand waits with Rama's test tool until the gate has processed each
thing that it appended, and reads the answer once. It sends things of the base
in three ways: one alone; several in a row, with no wait for the gate between
two of them, as a maker sends her acts; and several while it pauses the
microbatch topology with Rama's test tool, so that one batch takes them all.

The checks of the streaming gate come first. The base is made after
them, because one of them sends the base's making act to the streaming gate and
holds the word that the streaming gate gives an act for the base while the base
is not made.

The streaming gate's groups are the measure of the base's: what the proof shows
of a streaming layer it shows of the base, group for group. Two things are held
once, for the streaming depot: the kinds of value that Rama carries on a depot,
and what the two reads give for a thing that is no id. Beside them stand the
checks of what only the base has: the line, the copies on every task, the stamp
by the proof's own arithmetic for each thing that the gate decided, a mark and
the facts after it in one batch, and an attempt of a batch that fails and runs
again.

What it checks, group by group:

1. What Rama carries. A thing that holds each kind of value is appended; the gate
   records the same digest that the hand took before the append; the record read
   back from the depot's log is another object, with the same digest. The thing
   holds no time, and it is the first thing that the gate decides in its layer:
   the stamp of its refusal is 0, and that is the layer's last stamp.
2. Layers are made. An act with a value into a layer that is not made is
   refused with the word of a layer that is not made, and writes no row. A
   person's
   layer is made by the system for that person, with the depot streaming. A
   group's layer is made by the person who creates it. A making act for a layer
   that is made is refused, and so is a second act of the system in a personal
   layer. A making act of the base on the streaming depot, while the base is not
   made, is refused as an act for a layer of the other gate: the gate tells it
   from the layer id.
3. An act lands. One act with a value, read back by act-id with the digest of
   what was sent. Its row holds the value as a String, the one text of the
   value in its normal form, held against a text that the proof writes out; who
   wrote it and the permission that the act named. The record in the depot's
   log holds the value plain, as the hand sent it, and the time that the hand
   wrote on the act; the stamp of the answer is not earlier than that time with a
   counter of 0.
4. Refusals and repeats. A refusal recorded with its reason. A repeat answered
   from the record with the first stamp, and the layer's last stamp unchanged. Two
   things under one act-id that differ only in the time on them have one digest:
   the second reads the one answer, that of the first, and is a repeat. A
   second, different act under a used act-id, which reads an answer that is not of
   its digest. A stale `replaces`, and the current fact named.
   A fact that names nothing is never stale. A retract is a row with no value,
   which holds who retracted and under which permission, and a later fact
   replaces it. A place named twice.
5. Settings and permissions. A setting key on another layer. A permission for
   another layer. A permission that does not stand. A permission given beneath
   another by its holder, and an act under it. A give by one who does not hold the
   one above. `granted-in`. One write for
   a permission's place; the ending of a permission; a depot after the making.
   Whole or nothing: a give with a stale fact gives nothing. A tool's own facts
   under the bare words `:class` and `:permission`, on the layer's
   entity and on a holder's: accepted, each at its own place with the tool's
   value, and the layer's
   depot and the holder's permission are as they were. A key in the store's
   namespace that the store does not know: refused, with the refusal recorded.
6. A permission deeper than four. In a layer of its own, each holder gives a
   permission beneath her own, one at each depth from 2 to 32, and each give is
   accepted; the permissions of depth 4, 5, 31 and 32 sit on their holders as
   they were given. The give at depth 33 is refused as malformed: the refusal is
   recorded under its own act-id, and nothing sits on that holder. An act under
   the permission of depth 5, and one under the permission of depth 32, each with
   a value, are accepted, and the row of each holds the whole permission that the
   act named, with no part of it cut for its depth.
7. The time on the act, in layers that the group makes, with times that the hand
   chose. Five layers are made with one time on each making act: the stamp of
   each is that time with a counter of 0, so five layers hold one stamp. A second
   act into one of them, with the same time on it, gets the counter 1, and a
   third the counter 2. Two of the five layers are on one task, and each counts
   its own: the decisions in the one do not raise the other, and the task keeps a
   last stamp for each. An act with a later time on it gets that time with a
   counter of 0. An act whose time is earlier than its layer's last stamp, and
   one with the time 0 on it, each get the last stamp plus one: the clock went
   back, and the stamps of the layer did not. A thing with no time, with a nil
   time, with a time at the bound, with a time below zero and with a time that is
   no integer is each refused with the word of the time, under its own act-id,
   with a stamp of the layer's last plus one; none writes a row. A refused thing
   moves no layer's time: two things with a time one hour ahead that the
   declaration refuses for another fault, and an act of a stranger with such a
   time that the gate refuses for its standing, each get the layer's last stamp
   plus one, and the layer's last stamp at the end is not of that hour. The
   greatest time that the declaration takes is taken, in a layer of its own.
8. Strays and limits. One stray for each word, kept once however often it comes,
   with no answer and no stamp: the layer's last stamp is as it was before them.
   A thing at and over each limit. A value whose text is 65,536
   bytes is accepted, and its row holds those bytes; one of 65,537 is refused
   with the word of a value that is too large. An act over the bound on one act
   is refused with its word. The 10-second rule, held against the time on the
   act, at its edge: an act that carries a stamp 10,001 ms after the time on it is
   refused, and its answer holds the layer's last stamp plus one; one that carries a stamp
   exactly 10,000 ms after it is accepted, with the carried stamp plus one. A
   `claimed-when` far ahead is a claim: the act is accepted, and its stamp does
   not follow it.
9. The reads take anything: what is no id, no act-id, another layer's act-id or
   the other depot gets nothing.
10. What a fact keeps. In a layer that the proof makes for these checks, an act
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
11. The keys that take a second write. For who may see a layer, what it builds
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
12. What a permission allows. A permission given with the slot that says see.
    Its holder acts, naming it as the row has it: refused, with a word of its
    own. She names it without the slot, and with see and act: it does not stand.
    She cannot give, and her act with a value is refused under its own act-id
    with the word of her permission, and writes no row. With a second
    fault she gets the word of her permission. A second give to her of see and
    act is refused, and so is one by another giver. A holder of see and act
    acts and gives. A making act whose
    permission has the slot is refused, and the layer is then made by a right
    act.
13. What points at a thing, in a streaming layer. A fact before the mark; the
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
14. The base is made. Before its making, the depot of the base is nothing. An
    act for the base on the streaming depot is refused as an act for a layer of
    the other gate: the streaming gate tells it from the layer id, and reads no
    table of the other gate for it. An act into the base before it is made is
    refused; it was appended alone, and one read by its act-id gave its answer
    when the gate had processed it, so no answer of the base waits for a later
    run of the gate. A making act by a who that is not the system, and one that
    names a person as the base's holder, are each refused. The line: the base's
    making act, the give of a permission to a person, and her first act under
    the given permission, appended one after the other with no wait for the gate
    between them, are each accepted, with stamps that go up in the order of the
    appends. A second making act, its two facts in the other order, is refused.
    The depot is microbatch; the depot row, the system's permission row, the row
    of the permission that was given and the fact of the first act under it are
    on every task, each with the stamp of its act's answer.
15. An act lands in the base. Eight acts of one batch on eight entities are each
    accepted. The eight things are on one partition of the microbatch depot: the
    depot places a thing by its layer. The fact of each is on every task, the
    same row in every field. One act with a value, read back by act-id;
    its row holds the value as a String, the one text of the value in its normal
    form, who wrote
    it and the permission, and every task holds it; a select of the place gives
    that row whatever the partitioning key is. The answer's row is on one task,
    the task of the base's depot partition, and that task alone keeps a last
    stamp of the base. The record in the depot's log holds the value
    plain, and the time that the hand wrote on the act; the stamp of the answer is
    not earlier than that time with a counter of 0.
16. An answer is readable when its batch is committed. The proof watches the
    answers table on the base's task for the row of an act's answer. At the first
    sight of the row, one call of the read by act-id, with the act-id's layer as
    its routing id, gives the answer, with the stamp of that row: the read waits
    for nothing behind the commit that showed the row. The same query, called
    with the uuid of the act-id or with another layer as its routing id, gives
    nothing: its head finds that the routing id is not the act-id's layer, and
    makes no read.
17. Refusals and repeats in the base. A refusal with its reason, which writes
    nothing at the place, on any task. A stale
    `replaces`. A repeat answered from the record, with the stamp of the first
    decision, and the base's last stamp unchanged. Two things under one act-id
    that differ only in the time on them have one digest: the second reads the
    answer of the first, and is a repeat. A different thing under a used
    act-id, of which nothing is written. One act-id under two things in one
    batch: one answer, of the thing that the depot holds first. And a row of
    things appended with no wait for the gate, each decided against the rows that
    the things before it wrote: a thing that is no act, and a good act under its
    act-id, which reads that refusal; a fact; a stale `replaces`; the current fact
    named and replaced; a fact that names nothing, which writes over; a retract,
    which is a row with no value on every task, and a fact that names it; a value
    onto a retract; a place named twice; and a fact that replaces a fact of its
    own act.
18. One line, one order. Two acts of one batch that replace the same fact: the
    one that the depot holds first is accepted, and the second is refused whole
    as stale, with a later stamp. Two acts that write one place, appended one
    after the other with no wait for the gate: both are accepted, and the second
    names the first one's fact as the one that it replaces, so it was decided
    against the row that the first wrote. Two acts of one batch that write one
    place and name nothing: both accepted, and the place holds the fact of the
    second. In one batch, a give and then an act under it are both accepted; an
    act and then the give, the act is refused.
19. A chain in one batch. A chain of 100 acts is appended while the gate is
    paused, each naming the fact of the act before it as the one that it
    replaces. None has an answer while the gate is paused. Every one is accepted:
    an act that was decided before the act before it would have been refused as
    stale. The depot holds them in the order in which they were appended, and
    their stamps increase strictly in that order. On every task the place holds
    the fact of the last act of the chain.
20. A person acts in the base under a permission that the system gave, whose row
    is on every task. Acts with no standing. One write for a permission's place
    and for the depot. A
    key in the store's namespace that the store does not know, refused. A tool's
    own fact under the bare word `:permission`, on a holder's entity: accepted,
    its row on every task, as any fact's, with the tool's value, beside the
    store's permission of that holder. A carried
    stamp too far ahead of the time on the act, also under a permission that does
    not stand.
21. A permission deeper than four, in the base. Each holder gives a permission
    beneath her own, one at each depth from 2 to 32, appended one after the other
    with no wait for the gate, and each give is accepted, so each was decided
    against the row that the give before it wrote; the permissions of depth 4, 5,
    31 and 32 sit on
    their holders as they were given, on every task. The give at depth 33 is
    refused as malformed, and no task holds a row on that holder. An act under the
    permission of depth 5, and one under depth 32, each with a value, are
    accepted, and the row of each holds the whole permission. An act that names
    as its own a permission of depth 33, deeper than any that was given, is
    refused as malformed and writes nothing on any task.
22. The bounds in the base. Through the microbatch gate, a value whose one text
    is 65,537 bytes is refused with the word of the bound on one value: the
    refusal is recorded under its own act-id, and no task holds a row at its
    place. A value whose one text is 65,536 bytes is accepted in the same batch,
    and its row holds those bytes, on every task. An act whose rows would hold
    more than the
    bound on one act is refused with its word. And the limits of the facts of one
    act, of the entries of `:based-on` and of the text of an id, each at its edge.
23. A long batch: 400 acts on 20 entities in one batch, each accepted, each with
    a stamp of its own. The base is one line: the stamps go up in the order in
    which the acts were appended, whatever the entity of each.
24. The copies of the base. After the line, the chain and the long batch, the
    proof reads the four tables of the microbatch gate whole, on every task, and
    holds them against the depot's log. The facts are the same map on every task:
    no key has a row that is not the same on all of them, in every field. So are
    the facts by value. An answer is only on the task of its layer. A layer's
    last stamp is only on the task of that layer, and the digest of a stray only
    on the task of the partition where the stray arrived. Each layer that a thing
    of the microbatch depot names is on one partition of the depot. The proof
    makes the same five checks at three more moments: after the marks, in group
    32; at the end of the run, in group 38; and after the failed attempt and its
    retry, in group 39.
25. The time on the act, in the base: things with times that the hand chose,
    appended while the gate is paused, so that one batch takes them. As in group
    7: acts with one time on them get the counters 0, 1 and 2; a later time gets a
    counter of 0; an earlier time, and the time 0, get the base's last stamp plus
    one; a thing with no usable time is refused with the word of the time, and
    writes no row on any task; a thing with a time one hour ahead that the
    declaration refuses for another fault gets the base's last stamp plus one.
    The 10-second rule at its
    edge, as in group 8. And two layers of one task count apart in the microbatch
    gate too: a layer that starts streaming, and whose id hashes to the base's
    task, gets for its acts on the microbatch depot refusals with the stamps 0
    and 1, its own count, far below the base's last stamp on that task. The base's task
    keeps a last stamp for each of the two layers, and no other task keeps one
    for that layer.
26. An act of several facts in the base. Refused whole: an act of three values
    on three entities, with its third fact stale, leaves no row on any task.
    Accepted whole: each fact's row is on every task, with the index of the fact,
    the one stamp of the answer and the text of its value. The refusal took a
    stamp of the line, and the act's stamp is later. And three acts appended in a
    row with no wait: a retract; an act of two facts whose second names that
    retract as the fact that it replaces; and the same `replaces` again under a
    new act-id, which is stale.
27. Each gate tells the other gate's layers. An act for the base on the streaming
    depot, after the base is made, gets the word of an act into a layer of the
    other gate: the word that such an act got before the base was made. An act
    for a streaming layer that is made, on the microbatch depot, gets the same
    word. So does an act for a layer that starts streaming and that no gate has
    made, and a making act of that layer: the microbatch gate tells the layer
    from its id, and does not make it. The streaming gate then makes that layer,
    and an act for it on the microbatch depot gets the word that it got before.
    None wrote a fact, in either record. The refusal of an act for a streaming
    layer is on one task of the microbatch gate, the partition of that layer on
    the microbatch depot, and that task keeps its stamp as the layer's last stamp
    there. One act-id on both depots has one answer, the streaming gate's: the
    microbatch gate keeps the thing as a stray. Strays of the microbatch gate:
    one for each word, kept once however often it comes, with no answer, no fact
    and no stamp, the digest of each on the task of the depot partition where the
    stray arrived. And a search of the store's source as code, strings and
    comments left out: the microbatch gate's file and the walk's name no table of
    the streaming gate, and hold no reading of the clock and no append to a depot;
    the walk's file holds no partitioner and no yield; the microbatch gate's file
    holds one partitioner, to every task, behind the call of the walk. The search
    is held to find each word where it is.
28. What a fact keeps in the base: as group 10, on one entity of the base, each
    row on every task, the same in every field. A
    refused act leaves no row on any task. A person's rows
    hold her and the permission that the system gave her.
29. The keys that take a second write, in the base. The three first writes in
    one batch, the three second ones in one, the three that name the fact there
    in one: each accepted, and each row on every task, the same on each. Then
    one batch of things, each refused with its word: a stale
    `replaces`, a retract, another entity and each wrong value.
    The stamps of the marked keys on every task: the stamp of the act that marked
    the key, none when the set is empty, and the stamp of the later act when the
    key is marked again. A person
    with a permission in the base writes who may see it. A setting for the base
    sent to the streaming depot is refused as an act into a layer of the other
    gate.
30. What a permission allows, in the base. A give with the slot that says see,
    and an act of its holder in the same batch, after it, which is decided
    against the row that the give wrote: it gets the word of a permission that
    stands and says see. In a later batch her act is
    refused with the same word. Naming it without the slot, with see and act, a
    give of hers, her act with a value, and a second give to her. A permission
    that says see and act, given in the same batch: its holder acts in a later
    batch.
31. What points at a thing, in the base: the walk of group 13, on two entities of
    the base. The mark puts the key in the row of the marked keys, on every task,
    with the stamp of the act that marked it. Two places that hold one value are
    both in the run of that value, and each row of the run is on every task.
    Where an entry goes out, no task holds a row under its key. An act that took
    an entry out is appended a second time, after a later act put the entry back:
    the gate answers it from the record and copies nothing, so on every task the
    entry is still the later act's.
32. One batch under a marked key. Two acts of one batch on one place, each with
    the place as its own: both accepted, the place holds the second act's value,
    and the first act's value has no row on any task. A mark and the facts after
    it, appended while the gate is paused: the act that marks a key, the next act
    with two facts under it, a third fact, a replace of one and a retract of
    another, each accepted. The key is marked on every task with the stamp of the
    act that marked it. The act is decided with the marks as they were before it:
    the fact that the marking act itself holds under the key is in no run. The
    mark holds from the next act: on every task the order by value holds, under
    that key, the entries that the acts after the mark left. A fact of a later
    batch is in the order too. Two writes of who may see
    the base in one batch: every task holds the second one's
    row. Then the copies are held, as in group 24.
33. The module keeps no lock. Asked of the cluster by name, the two tables of
    facts and the module's two queries are there, and a table of locks and a
    query of a request's locks are not, as a table and a query that the module
    never had are not. Through each gate, a fact under
    `:softland.store/lock-request` is refused as a key of the store's namespace
    that the store does not know, and a fact that carries `:sealed` or
    `:lock-id` is refused as a part that the form does not name.
34. A place of the base never gives nothing. A fact with a value is written to
    a place, and an act that names it replaces it. From before that append until
    the new row shows on every task, the hand selects the place on each of the
    four tasks, straight from the facts table: each select gave a row, the fact
    before or the fact after, and never nothing. On each task the place went from
    the fact before to the fact after once, and never back. The rows that the
    last round of selects gave are the rows that the four tasks hold when the gate
    has processed the act, in every field.
35. After the module is started again. Rama's module update is run with the
    unchanged module: the memory of the old instance is gone, and the depot and
    the PStates stay. The answer, the depot and the strays are as they
    were, and so are a value kept plain, the layer's marked keys with their
    stamps and the run of a value; a repeat is still answered from the record;
    the last stamps of two layers that are on one task are as they were, each its
    own; and the first stamp after is the layer's last stamp before plus one. The
    hand wrote the time in that last stamp on the act, so the gate counted on
    from the number that its table kept through the start. During this step Rama
    prints some lines of its own log that are
    marked ERROR, while the new instance opens the depot's log and the client finds
    the new instance. They are not failed checks.
36. The base after the module is started again: the answer, the depot, the
    base's last stamp, the strays, a permission's rows, the marked
    keys, a run of the facts by value on each task and a value kept plain are
    as they were. The copies are still on every task: each of the gate's four
    tables holds on each task the rows that it held before the start, in every
    key and every field. A repeat is answered from the record. The first stamp
    after is the base's last stamp before plus one, and the next act, with a
    later time on it, gets its own time with a counter of 0.
37. The stamps and the depot. The stamps of the answers increase strictly in
    each layer, in the order sent. The order is the layer's and not the task's:
    on a task that holds more than one layer the stamps do not all go up in the
    order sent. Each stamp is the number that the gate's rule gives, by the
    proof's own arithmetic: for an accepted act the greatest of the time that
    the hand wrote on the thing with a counter of 0, the stamp before it in its
    layer plus one, and the greatest stamp that the thing carries plus one; for
    a refused thing the stamp before it in its layer plus one, or 0 with none
    before it. That check is shown to fail,
    on the same answers with one stamp moved by one. The last stamp that the gate
    keeps for each layer is the stamp of the last answer of that layer. The depot
    holds each thing sent, as it was sent,
    each value plain, and under each act-id the time that the hand wrote. The
    layers are on
    more than one task. The module has two depots: asked of the cluster by name,
    the two depots of acts are there, and a depot of batch clocks, a depot of
    answers of either gate and a depot that the module never had are not. And a
    search of the store's source as code: no file names a depot of answers or a
    clock of a batch; the store declares two depots, both in the module's file;
    and the streaming gate's file names no table of the microbatch gate, and
    holds no append to a depot, no reading of the clock and no partitioner.
38. The stamps and the depot of the base. Each thing of the microbatch depot
    that is no stray has an answer under its act-id. The stamps of the answers
    increase strictly in each layer of the microbatch depot, in the depot's
    order, and each is the number that the gate's rule gives, by the proof's own
    arithmetic, for each thing that the gate decided; the check is shown to fail
    as in group 37. The last stamp that the gate keeps for each layer, on the
    layer's task, is the stamp of the last answer of that layer. The microbatch
    depot holds each thing sent, as it was sent, each value plain, with the time
    that the hand wrote. The things of the base are on one partition of the
    depot, in the order in which the hand appended them, whatever the entities of
    their facts. Both depots put a thing on the partition of its layer: a layer
    that has things on both depots is on the partition of one number in both.
    Then the copies are held, as in group 24.

The two groups of the second cluster run last:

39. An attempt of a batch fails and runs again. The group makes a streaming
    layer and the base, and marks a key of the base. Then it appends a batch of
    40 acts while the gate is paused, with a throw asked for at the 21st. Before
    the throw the proof's function hands the walk, for each of the 20 acts before
    it, one row more at a key that no act writes. The failed attempt walked the
    acts in the depot's order up to the throw and no further; Rama starts the
    worker again and runs the batch a second time. The second attempt made each
    row as the first made it: for each act that both attempts decided, both
    calls were given the same stamp and no word of a refusal, and the store's own
    function gave the same writes, row for row. After the retry all 40 are
    accepted, each with the stamp that the rule gives. No task holds one of the
    rows that only the failed attempt was handed. The order by value holds, on
    every task, the last entry of each place and no entry of an earlier act of
    the batch, and each place holds the fact of the last act that wrote it. The
    gate goes on behind the retry. The check of the rows of the failed attempt
    is shown to be able to fail: one more act, decided with the proof's function
    handing it such a row and no throw, is accepted, and that row is on every
    task. So a row that the walk is handed this way is written and copied, and
    the 20 rows went with the attempt that failed. Then the copies are held, as
    in group 24. During this group Rama prints
    lines of its own log that are marked ERROR, for the throw that the proof
    makes and for the worker that it starts again. They are not failed checks.
40. An event fails and runs again. Five acts are sent to the streaming gate
    right before the throw, with their events watched. The first run of one
    more act's event throws behind its reads and before its first write, and
    Rama runs the record again. The run that threw left no answer and no row: the
    second run reached the decision too, and found the place as the first run had
    found it. Each run was given the same stamp, the layer's last stamp from
    before plus one: no clock was read between the two. Each made the same three
    writes, row for row: a fact, an answer and the layer's last stamp. The second
    run is the one that counts: its writes are what the tables hold. Rama ran the
    records before it again too: the event of each of the five acts began at
    least a second time, after its rows were committed and its append acked, and
    none was decided a second time, since a run that finds an answer under the
    act-id decides nothing. Appended a second time by hand, the act reads the
    same answer and is not decided again. Rama prints lines marked ERROR here
    too, which are not failed checks.

Each line that the contract asks for is on a `SHOWN` line in the contract's
words, with "depot" where the contract says "class".

Where the proof shows a row of the base, it selects it from the gate's PState
with a partitioning key: on the base's task; on each of the four tasks, with a
key that hashes to the task; and, at chosen moments of the run, in the whole
table on every task, which it holds against itself task by task. Such
a select goes round the two reads. The row of a fact and a row of the facts by
value are on every task. An answer, a layer's last stamp and the digest of a
stray are on one task. Which task is a layer's is told from the depot's log:
the partition that holds the layer's things.

The hand also plays a reader of the tables, so that the gates stay proven
without the read path: it
selects a fact's row, the row of a layer's marked keys and a run of the facts by
value, with the keys that
[rows.clj](../../../src-store/softland/store/record/rows.clj) gives. It opens no
push.

### What in the module's proof rests on a time or a wait

No check holds a number of milliseconds.

- The hand's wait for the microbatch gate is Rama's test tool, which counts
  the records that the gate processed and throws at its own time limit.
- On the first cluster two loops watch a table until a row shows, each for 30
  seconds at most: for the first sight of an answer's row, in group 16, and for
  the new row of a place that is replaced, on every task, in group 34. A call
  that asks the cluster for a name is given ten seconds.
- On the second cluster the answers of the batch that fails, of the act
  behind it and of the event that fails are read in a loop for 60 seconds at
  most, and the proof's function sleeps 300 ms before the throw in the batch.
- The proof reads the clock for the time that the hand writes on a thing when
  no check chose one, and to end a loop that watches a table. Where a check
  holds a stamp to a number, the hand chose the time.

### What the module's proof does not reach

- a later act of a layer decided before an earlier one that failed. Group 13
  appends one accepted act a second time, which ends at the gate's read of its
  answer; that is a repeat. Group 40 makes
  an event of the streaming gate fail by a throw of the proof's own, and Rama
  runs its record again; the run-again proof, below, makes failures with acts
  behind them. A failure of the machine or of Rama in the middle of
  an event is not made;
- that a streaming layer's fact and its rows in the facts by value have no
  moment between them: it rests on one event with no partitioner, read in the
  code;
- a push: the hand selects, once. That an open push on a run is sent the
  removal of a place when a key that holds no row is taken out was run on a
  stand-in table by the check of this part's plan, not on the store's module;
- a run of very many places, what the one read more costs a gate, and a value
  whose digest another value shares;
- a reader that asks through layers, and whether an asker may see a layer: the
  read path's two proofs, below, show them;
- a failure while a batch commits. The forced failure of group 39 is in the
  walk, before any task commits. Of the time in which one task has committed a
  batch and another has not, the proof holds nothing: the rounds of selects of
  group 34 count how often they saw it, and no check rests on that number. A
  probe ran a commit that fails after one task showed its part, and the moment
  between two tasks' commits, on a module of its own
  (`src/proposal/backend-on-rama-2026-10-02/reads/batch-probe/`), not on the
  store's;
- that what the notes hold of the first attempt outlasts a worker: they are
  the memory of the proof's process, and they last through the restart only
  because the in-process cluster is one JVM;
- a yield in the walk: nothing looks for one at run time. The proof holds the
  depot's order by the chain of group 19, each act of which names the fact of
  the act before it, so that an act decided out of that order would be refused
  as stale; and group 27 finds no yield in the source of the walk;
- a batch from inside. That the things appended while the topology is paused
  fall into one batch is what Rama's test docs say. The proof sees that none
  of them had an answer while it was paused; and in group 39 the notes show
  that the failed attempt decided the acts up to the throw and the retry all
  of them;
- a module update between two attempts of one batch: the second attempt then
  runs the new code, and can make other rows from the same things;
- a second failure of one batch or of one event: each of the two groups of
  the second cluster forces one failure, of its own kind;
- the greatest time that the declaration takes, in the base: it would set the
  base's last stamp to the end of the range for the rest of the run. Group 7
  holds it in a streaming layer of its own, and both gates have one
  declaration and one rule for the stamp;
- a microbatch layer beside the base: no layer but the base starts with the
  microbatch depot. So the second layer of group 25 is a layer of the other
  gate, whose acts this gate refuses: it has stamps and answers there, and no
  fact;
- a module with another number of tasks: both clusters have four;
- a second module over the same two depots: the run-again proof, below, shows
  it;
- a worker whose process is killed, a worker that stops in the middle of an
  event, a move of a task's leader, more than one machine, and replication:
  the throw that the proof makes restarts a worker inside one JVM, and no row
  has to come back from a disk;
- an append whose ack fails for an act that the gate accepted;
- the two aims of speed: the in-process cluster's timings say
  nothing about them. The two runs of measures ("The measures", below)
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

The proof plays the door's maker: each act and each
answer goes through the door's five calls. The checks run in one order on one
cluster. The proof opens seven doors on the module: the door of most checks; a
second door, opened with a read each 100 ms; one for the count of the handles
that an opening fetches; two in the check of the opener's number; one that
reads each 5 ms, to time an act of the base; and one near
the end, which stands for the door of another process.

A pass shows the door in one process, with a maker that the proof plays. No
other maker uses the door yet.

The door adds three parts to a maker's act: the act-id, the depot, and the time
of the clock of the server that runs the door. The first two are known before
the send. The time is not, so the proof holds it in two steps: a record of the
log without its time is the maker's act with the act-id and the depot; and the
time is a fixed-width integer between two readings of the clock that the proof
makes, one just before the call of the door and one just after. The proof and
its doors run in one process, so the proof reads the clock that the door reads.

An act of the base goes to the microbatch depot, whose ack says that the depot
holds the thing and nothing of the decision. An answer of the base shows when
its batch is committed on the base's task, and the copy of a fact on another
task can show a moment later. So before it reads a row of the base round the
door, the proof waits, with Rama's test tool, until the microbatch gate has
processed each record of its depot. A place of a streaming layer has its row on
the layer's task. A place of the base has the same row on every task, and the
proof reads each task.

What it checks, group by group:

1. The door is opened. A name under which no module runs gives the door's word,
   by a real throw of Rama's client, and no door. The right name gives a door,
   and so does the same name with the door's one number given. The opening of
   a door fetches four handles of Rama's client and no more: the two depots of
   acts and the two queries. A door prints as the word "door" and the
   module's name, whatever prints it.
2. An act with no value. An act-id for a layer that is not made carries the depot
   that the layer starts with. The making act through the door, with the
   streaming gate's ack; its answer, accepted, with a stamp. The record in the log
   is the maker's act with the door's three additions and nothing else changed:
   without its time it is the act with `:act-id` and `:depot`, and its time is
   between the proof's two readings of the clock round the send. The digest
   that the maker got is the digest of the record in the log, of that record
   with its time taken out and with another time, and the gate's digest in the
   answer: the door and the gate both leave the time out. The stamp of the
   answer holds the time that the door wrote, with a counter of zero, since the
   making act is the first decision of its layer. Twenty act-ids of one layer
   with one query.
3. Round trips are counted: a first send of a value with no act-id, which is one
   append and one query; a send with its act-id given, which is one append; an
   answer with no wait, a wait of 600 ms that finds nothing, a bound that is not
   above zero, which is one read, a bound greater than any wait that the door
   counts, which throws nothing, and an answer for a thing that is no act-id,
   which makes no query. So every send is one append and at most one query.
4. A value is in the log as given. The record on the depot holds a value as its
   maker gave it, and not its normal form; the fact's row holds the text of the
   normal form; the digest of the record is the door's. The time in the stamp
   of the answer is the time that the door wrote on the act, which was sent
   after the layer's making act. An act with a store-key fact, a retract and a
   value is accepted.
5. The door and the hand send the same thing for one act with a value, a retract
   and a store-key fact: the door's record is equal to the hand's but for the
   uuid of the act-id and the time, and the two act-ids differ, so the equality
   is of two records; both are accepted. The hand writes the time on its thing
   as the door does, a reading of the clock, and the time in the stamp of its
   answer is that time.
6. The digest that the door gives is the gate's, for facts given as a vector, a
   list and a lazy sequence: each is accepted under the door's digest, and in
   the log `:facts` is equal to what the maker gave, a vector only where she
   gave one and a sequence in each case. One act under one act-id, sent with
   its facts as a vector and then as a list, has two digests, and the second
   send reads not mine; sent as a list and then as a lazy sequence, it has one
   digest, and both sends read the one answer as their own.
7. Things that are no act, one kind after the other. Each that ends in one of
   the door's words: the word, no record more in either log, no stray more.
   Those are a thing that is no map, a thing with no layer, a sorted map that
   cannot be read, a record, a layer that is no id, a layer that is too long,
   `:act-id` or `:depot` already there, the part of the door's time already
   there, with a number and with nil, an act-id of another layer, and a
   `:session` that throws when it is read, which the door meets in the walk
   for its digest. A value act whose `:who` throws when it is hashed ends in a
   word too, by a real throw of Rama's client, and the log holds nothing under
   its act-id.
   Each that goes in: the gate's recorded refusal, read through the door. Among
   them are a fact that is a record with a value, a fact that holds `:sealed`,
   with a value and with `:lock-id` and none, a fact under
   `:softland.store/lock-request`, a value that is a ratio, a list or NaN, and
   300 facts with values. No fact of a refused thing is in the tables. The edge
   of a value is a gate's, at 65,536 bytes of its text: a string whose text is
   65,536 bytes is accepted and its row holds those bytes; one character more
   goes in, the log holds the value whole, and a gate refuses it with the word
   of the bound on one value.
8. The answer back: the four answers; nothing yet for a thing that is no act-id,
   with no query; not mine for a second, different act under a used act-id; a
   maker's own second send of one act with a value under its act-id: the
   depot holds two records, each with the time of its own send, and the two
   have one digest; a gate answered the act-id once, with the time of the
   first record in its stamp, and both sends read the one answer as their own.
   A wait whose thread is interrupted; and the stamp of an answer used in
   `:based-on`.
9. A maker who may not act. A value act under a permission that was never
   given, and one into a layer that is not made: each is appended, is refused
   under its own act-id with the gate's word, and leaves no row; the log holds
   each value plain.
10. A holder of see. Alice gives, through the door, a permission that says see.
    Its holder sends a value: the thing goes in, the log holds her act as she
    gave it, with its value plain, and a gate refuses it under its own act-id
    with the word for a permission that does not allow acting; no gate holds a
    row at its place, on any task. An act of hers with no value is refused the
    same way.
11. Reads that throw: the read of a layer's depot and the read of an answer,
    each with its word.
12. The opener's number. With a read each 100 ms, a wait of 250 ms that finds
    nothing makes 4 queries. A door opened with a number that is no integer of
    1 or more, and with a name that is none of the door's, reads each 200 ms,
    the door's own number. A door opened with a read each `Long/MAX_VALUE` ms,
    more than any wait that the door counts, is a door, and no call throws.
13. An append that threw: before the thing left the process, by a real throw; and
    one that went through, on a thread whose interrupt mark the proof set, where
    the log holds the record, its digest is the word's, and the answer is
    accepted.
14. Many makers at once: 16 threads through one door, for one who in one layer,
    25 acts each of 1 to 3 values. Every act is accepted, the log holds one
    record more for each act and no other, and each value reads back from the
    row of its own place. Each record holds a time, the stamps of the one layer
    are all different, and no stamp is of a time before the time on its act:
    two threads can read the clock in one order and reach the depot in the
    other, so a stamp can be of a later time than the time on its act. Then 8
    whos at once, each on 2 threads.
15. The base through the door. An act-id for the base before it is made names
    the microbatch depot, by one read of the layer's depot that gave nothing.
    An act for the base under an act-id with the streaming depot word, sent
    before the base is made, goes to the streaming depot, and that gate's
    recorded refusal comes back with the word of a layer of the other gate, and
    not the word of a layer that is not made. The base's making act, with the
    microbatch depot's ack and a wait; its stamp holds the time that the door
    wrote, with a counter of zero. A value act, which is one record more in the
    base's log, the act itself with the door's three additions, by the read of
    the layer's depot and the act's append, in that order; the microbatch gate
    holds the row of its place on each task, and the streaming gate holds none.
    A read of its answer with no wait is one query, asked with the act-id's
    layer and the act-id.
16. The depot of a layer. The door reads a layer's depot when it makes an
    act-id: one read, asked with the layer alone. For the base, which is made,
    the read gives the microbatch depot; for a streaming layer that is made,
    the streaming depot; for a layer that is not made, nothing, and the act-id
    names the depot that the layer starts with. A series of act-ids in the base
    is one read. The module's query of a layer's depot, called with the proof's
    own handle for the same three layers, gives the same.
17. The ack of the base is no answer. While the microbatch gate is paused, 4
    acts of one who from 4 threads, and then an act that the gate will accept
    and one that it will refuse, each come back with the depot's one ack and
    read nothing yet; a wait of 600 ms comes back after its bound with nothing
    yet. When the gate goes on, each reads its answer, accepted or the gate's
    recorded refusal with its word, and each later read gives that answer
    again, its stamp too. The accepted value is one row on each task, and the
    refused one has no row on any task.
18. Two sends into the base are decided in their order. A give and then the
    first act under it, and an act and then an act that replaces its fact:
    each pair is sent one after the other under act-ids asked for ahead, which
    is two appends and no read, and both are accepted, with stamps that rise in
    the order of the sends and in each the time that the door wrote. Then 8
    acts sent one after the other while the gate is paused: a give, the first
    act under it, and six that each replace the fact of the act before. Each is
    accepted, so each was decided with the rows that the act before it wrote;
    the stamps rise in the order of the sends; and the place holds the value of
    the last, one row on each task.
19. How long an act of the base takes: 5 acts through a door that reads each
    5 ms, each sent when the one before is answered. The line prints the time
    from the return of the send to the answer, as an order of size, and the
    check holds it to nothing but the bound of a wait.
20. A value of each kind through each depot. A value of each kind that the form
    takes goes through the door and each depot: it is accepted under the door's
    digest, and it reads back from its row as the value in its normal form. No
    place of those values of the base has a row in the streaming gate's table.
21. An act-id given back with the other depot word: the thing goes to the depot
    that it names and that gate refuses it under its own act-id, an act with a
    value as an act with none. It is sent to each depot, for a layer that is
    made and for one that is not: each time the word is that of a layer of the
    other gate, and never that of a layer that is not made. The streaming gate
    gives the same word for an act of the base before the base is made and
    after.
22. The module is started again under the opened door, by Rama's module update
    with the unchanged module: an answer from before is the same, of a
    streaming layer and of the base, its stamp too, and a value act after is
    accepted, in each.
23. The door holds nothing. A second door, opened on the module, reads the
    answer of an act that the first door sent, by the act-id and the digest
    alone; it reads an act of the base that the first door sent too.
24. Each word and no stray, over the whole run: the door made no stray, over
    each word of a stray and each task of both gates; and each of the door's
    seven words was given, and no other.

### Where the proof stands in for a call of Rama's client

Clojure lets a proof put a function of its own in the place of Rama's client
calls `foreign-append!`, `foreign-invoke-query`, `foreign-depot` and
`foreign-query` for a while. The proof does so
for four things, and each `SHOWN` line that rests on it says so:

- to count the handles that the opening of a door fetches (group 1);
- to count the round trips of a call of the door: the function counts and
  then calls Rama;
- to note the calls of the door in their order, and of a read what it was
  asked and what Rama gave (groups 15, 16 and 18);
- to make a read throw (group 11): no real throw of a query was found
  that the in-process cluster gives without a fault of the cluster, because the
  door tests each thing that it passes to a read before it calls.

The other throws are real: a module name under which no module runs; a thing
that holds a plain Java object, which Rama's client cannot turn into bytes; a
thing whose `:who` is an object of the proof's own; and
a call on a thread whose interrupt mark the proof set. The proof never destroys
the module to make a call throw, because the close of the cluster then throws.

Two parts of a maker's act are objects of the proof's own making: a `:session`
that throws when it is read, and a `:who` that throws when it is hashed.

### What it changes outside the door

It appends to both depots of its own cluster through the door, and so writes
both records. Round the door it appends one thing with a handle of its own: the
hand's act, for group 5. It pauses the
microbatch gate with Rama's test tool and lets it go on each time, and it
starts the module again once. It sets the interrupt mark of a thread of its
own, and interrupts another while it waits.

### The module's two clients, and four proofs

In the runtime the module has two clients: the door, for the way in, and the read
path, for the way out. Four proofs are not held to that. The module's proof uses
neither: its hand appends to the depots, reads the queries and selects the tables
itself, which is what keeps the store proven without the door and without the
read path. The read path's proof sends through the door and reads through the
read path, and beside that its own hand selects a layer's last stamp
("The read path's proof", below). The door's proof sends through the door, and beside that it holds
handles of its own: it reads the two depots' logs, the answer under an act-id
by the module's query, rows of the gates' tables of facts by a select, and the
strays; and it appends the one thing of group 5. The run-again proof sends
through the door and by its own hand, reads an answer under an act-id by the
module's query, reads the records of each partition of both depots, and
selects every row of every table itself. A maker
has none of these ways.

### What the door's proof does not reach

- a throw of a query that Rama's client makes by itself;
- an append that threw for a fault of the cluster and went through all the same:
  the one that the proof reaches is an append on a thread that it interrupted
  itself;
- a worker of another process, and a thing of a type that it does not have;
- a door in another process: the door of group 23 is one more door in the
  proof's own JVM;
- a door on another server, with another clock: the proof and its doors run in
  one process and read one clock;
- where a batch is cut: the proof cannot see it, and nothing of a batch is in
  an answer. That the batch after a pause holds every act that was sent
  meanwhile is what Rama's test docs say;
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
  of 250 ms with a read each 100 ms makes 4 (group 12): each read must take less
  than the rest of its interval.
- The wait of 600 ms is held to return after 600 ms or more and before 5
  seconds, and a call with a thing that is no act-id to return within 300 ms
  (group 3).

The time on an act is held between two readings of the clock round the call of
the door, which holds only because the proof and the door read one clock.
Where a check holds that the time in a stamp is the time on its act, it says
why: the act is the first of its layer, or it was sent after each earlier act
of its layer. Of acts that 16 threads send into one layer at once the proof
holds only what holds in every order (group 14). The time of group 19 is
printed and held to nothing but the bound of a wait.

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
value of a marked key as a gate keeps it. No task is played: a source's routing
id is not looked at when it is read, so a source of the base gives the same
rows under every home, as the store's tables do when every task holds the
base's rows. The home of a read is the layer that it starts from.

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
   a start layer with no fact, not known yet, not made, not seen. The home of a
   read: the first layer of a list, or the start layer of a query that takes
   its list from a layer, read from the query alone, whatever is asked, whoever
   asks and whatever the tables hold of that layer. The seven lines
   of the table of an asker's standing. What no fact reads as. What a layer's own
   rows mean for a query: a setting written again with the same word, and a list
   or a mark written in a layer that she may not see, mean the same; a setting
   that goes to "members" means another thing also when her standing stays as it
   was.
4. The sources of each word in a streaming layer and in the base: the table, the
   routing id and the path, line by line as the record's page gives them. The
   routing id of a source of a streaming layer is the layer, under every home.
   The routing id of a source of the base is the home of the read, for every
   kind of source, and never the entity or the value's digest; a run across
   entities and a whole layer are each one source there. Two reads with one
   home have equal sources for the base; two reads with two homes share no
   source of the base, though each reads the same rows of it. Two
   askers of one thing who ask for different parts have equal sources.
5. The loop ends. It is played on the tables' maps: what is wanted, fetch what is
   missing, again. The rounds and the calls of each round are held for a place,
   an entity, a run and what points at a thing, asked once and kept live. Asked
   once from the base itself, the base is one call, with the rights and the
   fact in it, also for a run across its entities and for the whole layer;
   through her own layer it is one call more, under her layer's routing id.
   Each call holds sources of one table under one routing id. A read
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
   facts are in no answer that did not ask; a run across entities of the base,
   read through her own layer, gives each place of the base once, and the
   nearer layer's place where it holds one; the answer of a read of the base
   does not differ by its home.
8. Which changes reach an asker, and which places a change names for a query,
   also for a change of the base that an asker reads through her own layer.
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
    layer and takes it back while facts change. It plays the base under the
    routing id of the asker's own layer too: the scenarios of what points at a
    thing with the base as the nearest, the second and the farthest layer; and
    the lists of writes for a place, an entity and the whole of two layers,
    through her own layer and the base. And it shows that the player can
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
- that every task holds the base's rows, and a read on a task: no task is
  played. The read path's proof, below, reads the base on the task of an
  asker's own layer;
- more than four writes in one played world.

## The read path's proof

Run it from the repository root:

```sh
clojure -M:store-module -m softland.store.read-path-proof
```

The proof is the read path's one asker, played by
hand: no view, no screen, no tool and no agent asks. Each act goes in through the
door, and each read goes through the read path's five calls. The checks run in
one order on one cluster, and later checks build on the layers that earlier ones
made. One check has a second cluster, after the first is closed.

The home of a read is the layer that it starts from. A streaming layer is read
on the layer's task. The base is read under the routing id of the read's home,
so on the task where the asker's own layer lives, which holds a copy of the
base's rows; a read that starts from the base reads it on the base's own task.

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
    a layer, an entity, a run across entities of the base, from the base
    itself and through her own layer, what points at a
    thing through three layers. The same place while a live read holds a push
    open on each of its sources: the same calls and the same answer, since a
    read asked once reads the tables and takes no value of a push. And a call of
    300 paths, whose values come in the paths' order.
12. A very large run, on a read path that was opened with small numbers: a run of
    3, of 8 and of 12 places by its calls; a layer that she may not see; a whole
    layer; a read that needs more checks than one read may; a run of 8 places
    that is kept live and then grows past the number, which ends the read with
    its word and why; a source of a kind that the read path builds no path for.
13. A run across entities of the base, from the base itself, once and kept
    live. Asked once, the places come whole from one task, in one call under
    the base's routing id. Kept live, the read holds two pushes, the run and the
    base's own rows, both under the base's routing id, and one write in the run
    gives one change. A run that reaches the base's own entity, asked with the
    store's own facts, holds the base's depot row; asked without, it holds no
    row of the store's.
14. The base is read where the asker's layer lives. The proof finds, by selects
    of its own, a layer of Alice's on each task of the cluster; and a layer of
    Alice's, a layer of Bob's and the base on three different tasks. Asked once
    through a layer on each task, a place of the base is given as it is from
    the base itself: by one call on the base's facts under the routing id of
    the layer that the read starts from, and none under the base's routing id.
    A run across entities of the base, and what points at a thing in it, come
    back whole from the one task of each asker's layer: the places that the
    base's own task gives. Kept live by two askers whose layers are on two
    tasks, and by one who reads from the base itself: each has the value now,
    the base's row has one push under each read's home, and a new fact of the
    base is told to each of the three, one change for each act. A run and what
    points at a thing, kept live through two layers on two tasks: one act of
    the base with a fact in each tells each asker the one new place, once.
    Acts of the base, each followed at once by a read asked once through a
    layer on another task: each is given within the proof's wait; how many are
    given at once is shown and not held. And the pushes on the base are
    counted by the read's home: two askers with one home share its pushes on
    the base, an asker with another home holds pushes of his own, one write
    calls each function once, and the pushes of a home stay until its last
    asker has left.
15. Each word kept live, for each gate: the answer now; an act inside gives one
    change; an act outside gives none; an act in a farther layer under a nearer
    layer's fact gives none; twenty acts in quick turn end at the last value.
16. What shapes a read changes under it: a permission given; a setting to
    "everyone" and back; the start layer's list written again; a key marked, a
    fact under it, the mark off and on; a list and a mark in a layer that she may
    not see; a layer that joins over farther orders; the base to "members" and
    back.
17. One push for one thing: two askers of one place share its pushes, each is
    handed the parts that she asked for, and a push is closed when its last reader
    has left. 50 times two threads open the same thing at once; 50 times one joins
    while the last one leaves, and a write follows. The fields of what the read
    path holds for a live read and for a push.
18. The read path's own bound on its pushes, on a read path that may hold 3 on
    one table under one routing id. One live read of a place in a members layer
    holds 3: the layer's own rows, her permission, the place. A read of another
    place there is turned away with the read path's word, and no push is opened
    or closed for it; a read asked once of that place still answers; a second
    reader of the open thing joins and is not counted again; a read through
    another layer opens; when the readers have left, the read that was turned
    away opens. 20 times, eight threads each open a read of another place of
    the one layer at once: one opens, seven are turned away. The two tables of
    one layer are counted apart. An open read that comes to need one push more
    ends with the word and why. A read that starts from the base holds the
    base's own rows and a place, both counted under the base's routing id. In
    the base the count is of the pushes of one home: two reads of two places
    through one home fill its count on the base's facts; a third place through
    that home is turned away with the word, and no push is opened or closed for
    it; the same place opens through another home, and from the base itself.
    At each of these moments the
    read path's counts are the counts of its map's entries. And where one read
    kept live may need 1 check, a read that needs 2 is turned away before a
    check is opened, and asked once it is given whole; and an open read that
    comes to need 2 ends with its word and why.
19. Nothing is shown that is not in the tables: a refused act at a watched place
    gives the asker nothing; each value that she was given is of an accepted act.
    When every asker has left, the read path's three maps are empty.
20. Reads that fail: a select that throws, and one that does not come back; under
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
21. The function and its threads, under 200 acts from 4 threads: the first call on
    the caller's thread, the others on a thread of Rama's client, one at a time.
    And straight after the 200 acks a read asked once gives every place, through
    the read path that holds a push open on that run and through one that holds
    none.
22. One reader in the source, by a search of the store's source as text.
23. The order of the table's keys: entities with letters outside ASCII; an entity
    and a layer whose text is the start of another's; letters from U+E000 on and
    outside the first plane.
24. A function that leaves its own read, or another, inside its call; two reads
    that leave each other, 50 times; a function that asks inside its call.
25. The opening: 50 times a maker writes one place 20 times in quick turn while a
    live read opens on it. And a read of another asker while one asker's function
    stays for 2 seconds.
26. The first thing that a function is given: of every live read that the proof
    opened, the first call was the whole answer, on the caller's thread.
27. The module is started again under an open read.
28. On the second cluster: a batch of the base runs a second time under two open
    reads of a place that it writes, one from the base itself and one through a
    layer on another task than the base's. The proof makes one throw, in the
    decision of an act that comes after in the batch, so the act that wrote the
    place is decided twice. The row that the second attempt made for the place
    is the first attempt's row in every field, the stamp too. Under each open
    read the function is called once for that place, with the row that the
    first attempt made.

### How it shows that nothing came

A check that holds "the asker is given nothing" cannot hold it by looking at
once: a change that is still on its way would pass as none. So after the act that
must give nothing, the proof sends a second act that changes the same push, and
waits until a witness has that second change: a second live read of the proof's
that holds the same push and is told every change of it. The changes of one push
come in order, and one callback is handled for every read that holds the push
before the next callback begins. So when the witness has the second change, the
first one has been handled for the asker too. Groups 15, 16, 19 and 28 hold
"nothing" in this way.

### Where it stands in for a call of Rama's client

The read path's two reading calls are macros of Rama's that end in two functions.
The proof puts a function of its own in the place of those two, and of Rama's
close, for a while: to count the calls of a read (groups 2, 6, 11, 12, 17, 18, 24);
to note the table and the routing id of each call (groups 13, 14);
to keep each push that a read opened, and its callback (groups 17, 20); to make a
select throw, and to make one never come back (group 20); to make the opening of
a push throw, and to make one never come back (group 20). In group 20 the proof
makes two diffs of Rama's own classes, the one with which Rama ends a push and
the one with which it sends a value anew, and hands each to a push's callback:
Rama itself ended no push in a run, and since a fact's row holds a String it
sends a change as a diff of its own, so no check meets a value sent anew by
Rama after the first callback. It also puts a
function in the place of the read path's own function that makes the calls of one
round, to count the rounds (group 11), and calls two private functions of the
read path (group 12).

### What it changes outside the read path

It appends to both depots through the door, and nothing by its own hand. To
find the task of a layer it selects, with two handles of its own, the layer's
last stamp in the table that the layer's gate keeps for a task, in groups 14,
23 and 28: only the task that decides the layer's acts holds one. It reads no
table of facts round the read path. It reads the read path's own memory, its
three maps, from the field of its type, for what no call shows. It writes the
base's setting to
"members" and back in group 16. It starts the module again once, in group 27. It
opens two more read paths in group 18, each with one small number, and one more
in group 14, for its counts of pushes. For
its second cluster it puts one function of its own into the var of one pure
function that the walk of both gates calls, before the cluster is launched, and
puts the store's own back after; there it pauses the microbatch gate once with
Rama's test tool, and lets it go on.

### What the read path's proof does not reach

- A cluster of several machines, a network between the runtime and Rama, a move
  of a task's leader, a killed process, a restart of the runtime;
- Rama ending a push by itself, and Rama leaving a reader out of a change and
  sending the value anew: group 20 hands the read path the diff that Rama's
  pages name for each. The value that it hands in with the second is the
  proof's own, and the push's value stays what Rama holds;
- the order in which two pushes call back: no check can force it on Rama. The
  pure proof plays every order on the pure functions; groups 15, 17 and 25 only
  make a crossing likely;
- a number of tasks that changes under an open read;
- many askers and many pushes at once: groups 17, 18 and 21 run a few threads.
  No check opens as many pushes as the read path's own bound at its default,
  900, or as Rama allows on one table on one task, 1,000 by default: group 18
  holds the bound with the number 3, the measures ask for 1,000 reads on one
  layer, and a probe of the build counted Rama's;
- that the read path's count stands for Rama's: the read path counts by routing
  id and one process, Rama by task and every client. Two routing ids on one
  task, and a second runtime, are not played. The base is read under the
  routing id of each read's home, so the homes that live on one task are
  counted apart by the read path and together by Rama;
- a run between two ends that hold a letter from U+E000 on, or one outside the
  first plane: the read path turns it away, because a range of Rama's between
  such ends gave, in runs of this proof, fewer places than the table holds;
- a failure of a batch while it commits, with one task seconds before another;
- how long after its maker's answer a fact of the base shows on another task
  than the base's: group 14 prints how many reads gave it at once, and holds
  only the proof's wait;
- a tool, a view, an inhabitant;
- how fast anything is.

### The checks that rest on a time

A check that waits for a change waits up to 15 seconds, and up to 30 for a change
of the base. A check that holds that nothing came waits a quarter of a second
after its witness has the second change. Group 20 holds that a select which does
not come back, under a wait of 300 ms, returns after 250 ms and before 3 seconds.
If one fails on a slow machine, read its numbers before the read path.

Group 20's 40 turns hand the ending in after 0 to 9 ms, so that some turns meet a
read that is not opened yet and some a read that is. The check holds each turn
to the one of the two right outcomes that it met, and its line prints how many
of each came. On a machine where every turn meets an opened read, the check
still passes and that line says 0 for the other: it then did not reach a read
that was not opened. With the function that ends a read put back as it was
before it took the read's turn first, the check failed in the turns that met a
read that was not opened: run once, by a scratch file of the build that changed
no source.

## The run-again proof

Run it from the repository root:

```sh
clojure -M:store-module -m softland.store.run-again-proof
```

The proof holds that the store comes out the same when it is run again from its
depots of acts. It holds one rule on lines made by hand, and then runs four
checks, each on an in-process cluster of its own, one after the other. Each row
that differs is printed on a line that starts with `DIFFERENT`, a few to a
table, with its gate, table, part and key, what differs, and both rows. Each
act that the rule names is printed on a line that begins `OUT OF ITS LINE`,
with its gate, layer, act-id, place and the part of the rule.

The second module. `StoreAgain` is a module of the proof's own. It declares a
mirror of each of the two depots of acts, under the depot's own name, and then
calls the one function that `Store` calls behind its own two depots
([module.clj](../../../src-store/softland/store/module.clj)). So it cannot be
another gate than the store's. Its tables are its own. Each gate declares its
source from the beginning of the depot, so a module that is launched when the
depots are full reads them whole.

The time on a thing. A gate reads no clock: the stamp of a decision is made of
the time on the thing, of the layer's last stamp, and of the stamps that the
act carries. The door writes the time on what goes through it. On a thing that
it appends with no door the hand writes its own reading of the clock at the
send, as a door does. A few things are appended as the proof built them, with a
chosen time or with none.

How it reaches one task's part of a table: by a select with the key that Rama's
test tool gives for that task. The select goes round the door's reads, so it
sees every row that is there.

What it checks, group by group:

1. The naming rule names the acts that are out of their line, on lines made by
   hand, with no cluster. A gate decides the acts of a layer in the order of
   the layer's depot partition, and gives each a stamp later than the one
   before. So a client can read, with no gate, whether a store kept that order:
   it reads a partition's records in their order and each act's answer. Within
   one layer an act is out of its line when the depot holds it and it has no
   answer, or when an act of its layer stands before it with a higher stamp, or
   behind it with a lower one. A stray is no act, and of two records under one
   act-id the first is the act. The group holds the rule on: acts of one layer
   with stamps in the order of their places, where it names none; two stamps
   swapped; a first act with the highest stamp of all; an act with no answer
   between decided ones; repeats and strays, where it names none; and two
   layers on one partition, each in its own order, where it names none. And it
   holds which rows that differ a named act accounts for: its own, those of an
   act behind it in its layer, and the layer's last stamp; and not those of an
   act before it, of another layer, or the strays.
2. The gates, run again, decide the same. This is check 1. It launches `Store`
   with four tasks. A hand sends things of every kind to both depots, some
   through the door and some with no door, and the first module decides them as
   they arrive: acts before a layer is made, and acts for a layer of the other
   gate on each depot, before that layer is made and after; the base's making
   act, a give and the first act under it; in each gate a thing of each kind
   and shapes whose answer depends on the order, things whose time is chosen,
   an act sent twice with two times, a chain of acts that each replace the one
   before, an act with each optional part, a stray of each word, and acts
   refused for several reasons; more streaming layers, with their acts sent
   round the layers one after the other, so that the records of two layers on
   one partition stand between each other there; a load into the base with no
   wait for the gate, and into one streaming layer; and two acts that write
   one place. When that module is at rest the
   check launches `StoreAgain`, which decides both depots from their
   beginning. Every row of every table of both gates is then held, task by
   task and key by key, against the same row of the other module. A pass
   shows: no row differs; every task of each module holds one and the same
   map of the microbatch gate's facts and of its facts by value; the first
   module's tables are the same before the second module was launched and
   after it came to rest; and the naming rule, applied to each partition of
   both depots against each module's answers, names no act. Beside the check,
   and held to nothing, it prints what each module holds of the hand's cases.
3. The fixture's records give its tables, with 4 tasks. This is check 2. It
   launches `Store` on a fresh cluster and appends the kept records of
   [the fixture](run_again_fixture/README.md) to the two depots by hand, as
   they are: the time on a record is its own, and the check writes no other.
   Each partition's records go in their order, which keeps each layer's. When
   the store is at rest the check reads every table on every task and compares
   three times. One: the tables, brought into a form that names no task,
   against the kept tables, row by row. Two: each task's map of the microbatch
   gate's facts, and of its facts by value, against the fixture's one map of
   each. Three: placement, with no task number: for each layer of the fixture,
   a select with that layer as the partitioning key reads one task, and that
   task must hold the layer's answers and its last stamp, and for the
   streaming gate its facts and its facts by value, each equal to the
   fixture's rows of that layer. A pass shows that no row differs in any of
   the three, and that the naming rule names no act of this store.
4. The fixture's records give its tables, with 8 tasks. This is check 3: the
   check of group 3 on a store with another number of tasks than the store
   that the fixture was made of. The order between two partitions must not
   matter: with 4 tasks the check appends the streaming depot before the
   microbatch depot and each depot's partitions from the first to the last;
   with 8 tasks all of it reversed. A store whose tables hung on the order of
   two partitions, or of the two depots, would not give the fixture's tables
   twice.
5. A failure leaves each layer's acts in their line. This is check 4, on the
   store's own module and gates, with no option of Rama's set. The proof makes
   three kinds of failure, each once, in the first attempt of one act, each
   with acts behind it whose answer depends on the order. A throw in the base:
   in the decision of an act in the middle of a batch, with an act behind it
   in the batch and one appended after the throw. A stall with the thread
   held: an event of a streaming layer sleeps on its task's thread for longer
   than Rama's stream timeout, with one act in the depot before the event
   began and one appended into the sleep. A throw in an event of a streaming
   layer, in two layers: with the next act appended right behind it, and with
   the next act in the depot before its event began. For each it prints what
   each maker was told, which acts the depot holds, the attempts as the walk
   began them, and the order of the decisions by stamp in both modules. Then
   it puts the store's functions back, launches the second module, which
   decides both depots from their beginning with no failure, and compares as
   group 2 does. A pass shows: each planned failure was seen to happen; the
   store's own functions are back; no row differs; and the rule names no act
   in either module. Should rows differ, the check is another: each row that
   differs must be of an act that the rule names or that stands behind a named
   act in its layer, and the line of the comparison says that rows differ. An
   append that a failure swallowed is no act of the depot: its maker was told
   that the append may not have gone through, the check says which were lost,
   and they are no difference.

Group 5 runs what Sid asked for on 13 October 2026: "Nobody has shown whether a
failure in a fast layer can make acts that were in flight come out in another
order when the store is run again. Run it and find out." The store's page says
the rule, and what the runs found:
[The order of a layer's acts after a failure](../../../src-store/softland/store/README.md#the-order-of-a-layers-acts-after-a-failure).

### The form with no task

A store with another number of tasks puts a layer on another task, so groups 3
and 4 compare a row by its key alone. Of the microbatch gate's facts and its
facts by value every task holds a whole copy: the form holds that one map.
Every other table holds a row on the task of the row's layer alone: the form
holds the union of the tasks' rows. What a gate keeps for a task is in the form
as rows too: each layer's last stamp, and the strays of each word as the sorted
digests of all tasks.

### The fixture

Groups 3 and 4 read one fixture: the records of both depots of acts as they
stood on a store of four tasks, and the tables that those records gave.
[Its own map](run_again_fixture/README.md) says what the two files hold, who
writes them, when the fixture may be made again, and what a failure of the
proof on it means. When a file of the fixture is absent, each of the two
groups fails with the line "the fixture is not made: run make-fixture" and
launches nothing.

A run of the proof never writes the fixture. One mode of the proof makes it,
and runs no check:

```sh
clojure -M:store-module -m softland.store.run-again-proof make-fixture
```

It is run only when a rule is changed on purpose. A fixture that was made
again to make a failing check pass shows nothing.

### Where the proof stands in for a function of the store

For group 5 alone the proof puts functions of its own into the vars of three
pure functions that the walk calls: the declaration's test for a stray, the
declaration's check, and the decision's writes. Each gives what the store's
gives; through them the proof notes what the walk did with a watched act, and
makes each planned failure once. The walk reads each var at each call, so a
module that runs calls the proof's functions from the next thing on, and the
store's own again once they are back. The store's own are put back when the
group ends, however it ends. Every other group runs on the functions as the
store defines them.

### What it changes outside the code that it checks

It appends to both depots of acts of each of its clusters: through the door
and by its own hand in groups 2 and 5, and by its own hand alone in groups 3
and 4. In groups 2 and 5 it launches `StoreAgain` beside the store. It pauses
the microbatch gate with Rama's test tool, and lets it go on. In group 5 it
makes each planned failure once: a throw, or a sleep on a task's thread. The
clusters' files are temporary and are Rama's. The checks write no file; `make-fixture`
writes the two files of the fixture.

### What in the run-again proof rests on a time or a wait

How it knows that a store is at rest. Rama's test tool waits until the
microbatch gate has processed as many things as its depot holds. Each append
to the streaming depot waits for the gate's event. The answer of each act that
went through the door is there. And across a quiet time neither depot of acts
grew and nothing changed of what the proof polls on each task: for each gate
the number of its answers, of its facts, of its facts by value and of its
strays, and each layer's last stamp.

An append to a mirrored depot does not wait for the second module's stream
topology. So for the second module the proof polls until that module's
streaming tables hold as many answers and strays on each task as the first's,
and then waits for the same quiet time.

In group 5 an append that a failure swallowed is in no depot, so the hand's
count of what it appended does not say what the gate has to process. There
the first module is at rest when every act that the depots hold has its
answer and the quiet time has passed. The stall of that group rests on a sleep
that is longer than Rama's stream timeout. Whether the depot holds the next
act before the worker is started again, in the first throw of a streaming
layer, is a race: the check says how it went.

### What the run-again proof does not reach

- a real cluster, a process that is killed, a leader that moves;
- a run again while the first module still takes acts: the second module is
  launched when the first is at rest;
- a second module that takes the place of the first: the proof holds the
  second module's tables against the first's, and opens no door and no read
  path on it;
- a second module that mirrors the depots with another number of tasks than
  the first: groups 3 and 4 feed a fresh store;
- a number of tasks other than 4 and 8;
- a failure other than the three kinds of group 5, each run once: above all a
  batch of a streaming layer that Rama forces to fail with no restart, its
  rows dropped, which the in-process cluster did not make
  (`src/proposal/backend-on-rama-2026-10-02/run-again/probes-order-after-a-failure/RESULTS.md`,
  "Not settled");
- a store larger than memory;
- that the rows are right: group 2 shows that the second module's rows are the
  first's, and groups 3 and 4 that a store gives the rows which the store gave
  that the fixture was made of.

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

What it measures. A gate under 100 acts a second: the streaming gate with a key
not marked and marked, timed to the return of the door's send, and the base,
timed to the moment at which the door's wait gives "accepted". The streaming
gate with the key not marked once more, with 1, 100 and 1,000 live reads
asked for on the layer: the read path's own bound on its pushes lets some open,
the others are turned away, and the line says how many opened. The opening of a
canvas of 5,000 facts, as a run and as what points at
the canvas, once and kept live, through one layer and through three, with a
streaming layer as the farthest and with the base as the farthest. Through one
layer the base is read from the base itself, on the base's own task; through
three it is read on the task of the nearest layer, the read's home. The last
shape, kept live, is turned away by the bound on the checks of one live read;
what points at a canvas of 100 facts kept live through three layers, which
opens, gives the cost of those checks. And how late a push is: for a place of
the facts table in a streaming layer; in the base, read from the base itself;
in the base, read through a layer of the asker's own, on that layer's task,
which holds a copy of the base's rows; and for a place of the facts by value
in a streaming layer.

What a number of it is: the cluster runs in the process of the measure, on one
machine, with every row in memory and no network. A time is a floor for a round
trip and says little of a seek on a disk. A real cluster, the base on one, a
network and a second node are not measured. The base's rows are on each of the
four tasks, and the microbatch gate writes each accepted row of a fact on each
of them: what that copy costs the gate is the gates' own measure, not this
one.

One more run measures the gates alone, with no live read open:

```sh
clojure -M:store-module -m softland.store.gate-measures
```

It is a run that is no proof too, and prints each number on a line that starts
with `MEASURED`. Its first lines say which tree is measured: the commit, and how
many paths under `src-store` differ from it. A set of times is printed as its
count, its least, its middle, the
time that 9 of 10 are not above, and its most. With the argument `small` it runs
each shape with a small load. It is run while no proof and no other measure
runs on the machine.

What it measures. An act through the door and each gate, from one maker at 100
acts a second: the streaming gate, timed to the return of the door's send, with
the key not marked and marked; and the base, timed to the door's "accepted",
once through a door whose wait reads at the door's own number and once through
a door whose wait reads far more often. The same act sent a second time under
its act-id, into a streaming layer, where the gate finds the answer that it
gave the first time and decides nothing. And Rama's setting
`topology.stream.max.executing.per.task`, at Rama's default and at 1, the
second in a cluster of its own with the module `StoreAtOne`, which is `Store`
with that setting: an act at 100 a second, and the most acts a second that one
maker holds, as the rates at which the last act was answered within a second of
its due moment and as one maker that sends each act when the one before is
answered.

Where an answer is, and when. Each gate keeps what it decided in its own
tables, and the module's two depots hold what clients appended and nothing
else. The append of a streaming act returns when the streaming gate has run,
so a time of the streaming gate is to the return of the door's send. The
append of an act of the base returns when the depot holds the thing, and its
answer is there when the batch that decided it is committed on the base's
task. So a time of the base is to the read of the door's wait that gave the
answer: it can be one sleep of that wait and one read later than the commit.

It holds nothing to a number, and what a number of it is holds as above. One
run is an order of size.
