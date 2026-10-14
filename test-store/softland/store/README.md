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

What it checks. Each check states itself: its sentence is in the test file, as
the first argument of `check`, or the first part of a case. A run prints each
sentence after `PASSED` or `FAILED`, beside the `SHOWN` lines that say what a
group observed. This page does not retell the checks.

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

What it checks. Each check states itself: its sentence is in the test file, as
the first argument of `check`, or the first part of a case. A run prints each
sentence after `PASSED` or `FAILED`, beside the `SHOWN` lines that say what a
group observed. This page does not retell the checks.

### What the pure proof does not reach

- a PState, a depot or a query: each row is a value of this process, and the
  walk of one thing, which reads rows between the checks, is the gates'. The
  proof walks a thing with functions of its own, on a record in memory: the
  whole walk in the tests `the-walk-of-one-thing-in-memory`, `a-line-is-decided-in-the-depots-order` and `a-line-under-marked-keys`, and its steps 6 and 7 in the cases of the
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
  while it runs, and a file that is no `.clj` file: the search of the test `no-code-of-the-store-names-a-lock`
  holds the symbols and the keywords of the code under `src-store/` and under
  `test-store/`, and it reads no page of explanation.
- the gates' dataflow: its three readings of the gates' source
  (`what-no-run-in-process-can-see`) hold how two settings and one guard are
  written, and run none of them.

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
groups, and hands the walk rows that no act writes where a check of a second
run asks for them; both note their calls. The store's own
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

What it checks. Each check states itself: its sentence is in the test file, as
the first argument of `check`, or the first part of a case. A run prints each
sentence after `PASSED` or `FAILED`, beside the `SHOWN` lines that say what a
group observed. This page does not retell the checks.

### What in the module's proof rests on a time or a wait

No check holds a number of milliseconds.

- The hand's wait for the microbatch gate is Rama's test tool, which counts
  the records that the gate processed and throws at its own time limit.
- On the first cluster two loops watch a table until a row shows, each for 30
  seconds at most: for the first sight of an answer's row, in the test `an-answer-is-readable-when-its-batch-is-committed`, and for
  the new row of a place that is replaced, on every task, in the test `a-place-of-the-base-never-gives-nothing`. A call
  that asks the cluster for a name is given ten seconds.
- On the second cluster the answers of the batch that fails, of the act
  behind it and of the event that fails are read in a loop for 60 seconds at
  most, and the proof's function sleeps 300 ms before the throw in the batch.
- The proof reads the clock for the time that the hand writes on a thing when
  no check chose one, and to end a loop that watches a table. Where a check
  holds a stamp to a number, the hand chose the time.

### What the module's proof does not reach

- a later act of a layer decided before an earlier one that failed. The test `what-points-at-a-thing`
  appends one accepted act a second time, which ends at the gate's read of its
  answer; that is a repeat. The test `an-event-fails-and-runs-again` makes
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
- a failure while a batch commits. The forced failure of the test `an-attempt-of-a-batch-fails-and-runs-again` is in the
  walk, before any task commits. Of the time in which one task has committed a
  batch and another has not, the proof holds nothing: the rounds of selects of
  the test `a-place-of-the-base-never-gives-nothing` count how often they saw it, and no check rests on that number. A
  probe ran a commit that fails after one task showed its part, and the moment
  between two tasks' commits, on a module of its own
  (`src/proposal/backend-on-rama-2026-10-02/reads/batch-probe/`), not on the
  store's;
- that what the notes hold of the first attempt outlasts a worker: they are
  the memory of the proof's process, and they last through the restart only
  because the in-process cluster is one JVM;
- a yield in the walk: nothing looks for one at run time. The proof holds the
  depot's order by the chain of the test `a-chain-in-one-batch`, each act of which names the fact of
  the act before it, so that an act decided out of that order would be refused
  as stale; and the test `each-gate-tells-the-other-gates-layers` finds no yield in the source of the walk;
- a batch from inside. That the things appended while the topology is paused
  fall into one batch is what Rama's test docs say. The proof sees that none
  of them had an answer while it was paused; and in the test `an-attempt-of-a-batch-fails-and-runs-again` the notes show
  that the failed attempt decided the acts up to the throw and the retry all
  of them;
- a module update between two attempts of one batch: the second attempt then
  runs the new code, and can make other rows from the same things;
- a second failure of one batch or of one event: each of the two groups of
  the second cluster forces one failure, of its own kind;
- the greatest time that the declaration takes, in the base: it would set the
  base's last stamp to the end of the range for the rest of the run. The test `the-time-on-the-act`
  holds it in a streaming layer of its own, and both gates have one
  declaration and one rule for the stamp;
- a microbatch layer beside the base: no layer but the base starts with the
  microbatch depot. So the second layer of the test `the-time-on-the-act-in-the-base` is a layer of the other
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
- three things that leave the tables as they are, so that no run in process
  sees them: that the microbatch gate's copy passes over the deciding task,
  that a refusal, a repeat or a stray is copied to no task, and that the
  streaming gate's retry mode is "all after". The pure proof holds each by a
  reading of the source (`what-no-run-in-process-can-see`). A reading shows
  how the code is written, not what a cluster does with it: a row that a copy
  to itself would set back, or the cost of a copy that writes nothing, is
  measured by no check.

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

What it checks. Each check states itself: its sentence is in the test file, as
the first argument of `check`, or the first part of a case. A run prints each
sentence after `PASSED` or `FAILED`, beside the `SHOWN` lines that say what a
group observed. This page does not retell the checks.

### Where the proof stands in for a call of Rama's client

Clojure lets a proof put a function of its own in the place of Rama's client
calls `foreign-append!`, `foreign-invoke-query`, `foreign-depot` and
`foreign-query` for a while. The proof does so
for four things, and each `SHOWN` line that rests on it says so:

- to count the handles that the opening of a door fetches (the test `the-door-is-opened`);
- to count the round trips of a call of the door: the function counts and
  then calls Rama;
- to note the calls of the door in their order, and of a read what it was
  asked and what Rama gave (the tests `the-base-through-the-door`, `the-depot-of-a-layer` and `two-sends-into-the-base-are-decided-in-their-order`);
- to make a read throw (the test `reads-that-throw`): no real throw of a query was found
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
hand's act, for the test `the-door-and-the-hand-send-the-same-thing`. It pauses the
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
strays; and it appends the one thing of the test `the-door-and-the-hand-send-the-same-thing`. The run-again proof sends
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
- a door in another process: the door of the test `the-door-holds-nothing` is one more door in the
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

- A wait of 600 ms with a read each 200 ms makes 4 queries (the test `round-trips-are-counted`), and a wait
  of 250 ms with a read each 100 ms makes 4 (the test `the-openers-numbers`): each read must take less
  than the rest of its interval.
- The wait of 600 ms is held to return after 600 ms or more and before 5
  seconds, and a call with a thing that is no act-id to return within 300 ms
  (the test `round-trips-are-counted`).

The time on an act is held between two readings of the clock round the call of
the door, which holds only because the proof and the door read one clock.
Where a check holds that the time in a stamp is the time on its act, it says
why: the act is the first of its layer, or it was sent after each earlier act
of its layer. Of acts that 16 threads send into one layer at once the proof
holds only what holds in every order (the test `many-makers-at-once`). The time of the test `how-long-an-act-of-the-base-takes` is
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

What it checks. Each check states itself: its sentence is in the test file, as
the first argument of `check`, or the first part of a case. A run prints each
sentence after `PASSED` or `FAILED`, beside the `SHOWN` lines that say what a
group observed. This page does not retell the checks.

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

What it checks. Each check states itself: its sentence is in the test file, as
the first argument of `check`, or the first part of a case. A run prints each
sentence after `PASSED` or `FAILED`, beside the `SHOWN` lines that say what a
group observed. This page does not retell the checks.

### How it shows that nothing came

A check that holds "the asker is given nothing" cannot hold it by looking at
once: a change that is still on its way would pass as none. So after the act that
must give nothing, the proof sends a second act that changes the same push, and
waits until a witness has that second change: a second live read of the proof's
that holds the same push and is told every change of it. The changes of one push
come in order, and one callback is handled for every read that holds the push
before the next callback begins. So when the witness has the second change, the
first one has been handled for the asker too. The tests `each-word-kept-live`, `what-shapes-a-read-changes-under-it`, `nothing-is-shown-that-is-not-in-the-tables` and `a-batch-that-runs-a-second-time` hold
"nothing" in this way.

### Where it stands in for a call of Rama's client

The read path's two reading calls are macros of Rama's that end in two functions.
The proof puts a function of its own in the place of those two, and of Rama's
close, for a while: to count the calls of a read (the tests `a-thing-that-is-no-query`, `the-stores-own-facts`, `the-calls-of-a-read-asked-once`, `a-very-large-run`, `one-push-for-one-thing`, `the-read-paths-own-bound-on-its-pushes` and `a-function-that-leaves-or-asks-inside-its-call`);
to note the table and the routing id of each call (the tests `a-run-across-entities-of-the-base` and `the-base-is-read-where-the-askers-layer-lives`);
to keep each push that a read opened, and its callback (the tests `one-push-for-one-thing` and `reads-that-fail`); to make a
select throw, and to make one never come back (the test `reads-that-fail`); to make the opening of
a push throw, and to make one never come back (the test `reads-that-fail`). In the test `reads-that-fail` the proof
makes two diffs of Rama's own classes, the one with which Rama ends a push and
the one with which it sends a value anew, and hands each to a push's callback:
Rama itself ended no push in a run, and since a fact's row holds a String it
sends a change as a diff of its own, so no check meets a value sent anew by
Rama after the first callback. It also puts a
function in the place of the read path's own function that makes the calls of one
round, to count the rounds (the test `the-calls-of-a-read-asked-once`), and calls two private functions of the
read path (the test `a-very-large-run`).

### What it changes outside the read path

It appends to both depots through the door, and nothing by its own hand. To
find the task of a layer it selects, with two handles of its own, the layer's
last stamp in the table that the layer's gate keeps for a task, in the tests `the-base-is-read-where-the-askers-layer-lives`, `the-order-of-the-tables-keys` and `a-batch-that-runs-a-second-time`: only the task that decides the layer's acts holds one. It reads no
table of facts round the read path. It reads the read path's own memory, its
three maps, from the field of its type, for what no call shows. It writes the
base's setting to
"members" and back in the test `what-shapes-a-read-changes-under-it`. It starts the module again once, in the test `the-module-is-started-again`. It
opens two more read paths in the test `the-read-paths-own-bound-on-its-pushes`, each with one small number, and one more
in the test `the-base-is-read-where-the-askers-layer-lives`, for its counts of pushes. For
its second cluster it puts one function of its own into the var of one pure
function that the walk of both gates calls, before the cluster is launched, and
puts the store's own back after; there it pauses the microbatch gate once with
Rama's test tool, and lets it go on.

### What the read path's proof does not reach

- A cluster of several machines, a network between the runtime and Rama, a move
  of a task's leader, a killed process, a restart of the runtime;
- Rama ending a push by itself, and Rama leaving a reader out of a change and
  sending the value anew: the test `reads-that-fail` hands the read path the diff that Rama's
  pages name for each. The value that it hands in with the second is the
  proof's own, and the push's value stays what Rama holds;
- the order in which two pushes call back: no check can force it on Rama. The
  pure proof plays every order on the pure functions; the tests `each-word-kept-live`, `one-push-for-one-thing` and `the-opening` only
  make a crossing likely;
- a number of tasks that changes under an open read;
- many askers and many pushes at once: the tests `one-push-for-one-thing`, `the-read-paths-own-bound-on-its-pushes` and `the-function-and-its-threads` run a few threads.
  No check opens as many pushes as the read path's own bound at its default,
  900, or as Rama allows on one table on one task, 1,000 by default: the test `the-read-paths-own-bound-on-its-pushes`
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
  than the base's: the test `the-base-is-read-where-the-askers-layer-lives` prints how many reads gave it at once, and holds
  only the proof's wait;
- a tool, a view, an inhabitant;
- how fast anything is.

### The checks that rest on a time

A check that waits for a change waits up to 15 seconds, and up to 30 for a change
of the base. A check that holds that nothing came waits a quarter of a second
after its witness has the second change. The test `reads-that-fail` holds that a select which does
not come back, under a wait of 300 ms, returns after 250 ms and before 3 seconds.
If one fails on a slow machine, read its numbers before the read path.

The test `reads-that-fail`'s 40 turns hand the ending in after 0 to 9 ms, so that some turns meet a
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

What it checks. Its five checks, and how each is made, are in the namespace
docstring of [run_again_proof.clj](run_again_proof.clj). Its assertions carry no
sentences of their own: a run prints a `SHOWN` line for what each check
compared, with the number of rows that differ. This page does not retell the
checks.

### The form with no task

A store with another number of tasks puts a layer on another task, so the tests `the-fixtures-records-give-its-tables-with-4-tasks` and `the-fixtures-records-give-its-tables-with-8-tasks` compare a row by its key alone. Of the microbatch gate's facts and its
facts by value every task holds a whole copy: the form holds that one map.
Every other table holds a row on the task of the row's layer alone: the form
holds the union of the tasks' rows. What a gate keeps for a task is in the form
as rows too: each layer's last stamp, and the strays of each word as the sorted
digests of all tasks.

### The fixture

The tests `the-fixtures-records-give-its-tables-with-4-tasks` and `the-fixtures-records-give-its-tables-with-8-tasks` read one fixture: the records of both depots of acts as they
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

For the test `a-failure-leaves-each-layers-acts-in-their-line` alone the proof puts functions of its own into the vars of three
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
and by its own hand in the tests `the-gates-run-again-decide-the-same` and `a-failure-leaves-each-layers-acts-in-their-line`, and by its own hand alone in the tests `the-fixtures-records-give-its-tables-with-4-tasks` and `the-fixtures-records-give-its-tables-with-8-tasks`. In the tests `the-gates-run-again-decide-the-same` and `a-failure-leaves-each-layers-acts-in-their-line` it launches `StoreAgain` beside the store. It pauses
the microbatch gate with Rama's test tool, and lets it go on. In the test `a-failure-leaves-each-layers-acts-in-their-line` it
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

In the test `a-failure-leaves-each-layers-acts-in-their-line` an append that a failure swallowed is in no depot, so the hand's
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
  the first: the tests `the-fixtures-records-give-its-tables-with-4-tasks` and `the-fixtures-records-give-its-tables-with-8-tasks` feed a fresh store;
- a number of tasks other than 4 and 8;
- a failure other than the three kinds of the test `a-failure-leaves-each-layers-acts-in-their-line`, each run once: above all a
  batch of a streaming layer that Rama forces to fail with no restart, its
  rows dropped, which the in-process cluster did not make
  (`src/proposal/backend-on-rama-2026-10-02/run-again/probes-order-after-a-failure/RESULTS.md`,
  "Not settled");
- a store larger than memory;
- that the rows are right: the test `the-gates-run-again-decide-the-same` shows that the second module's rows are the
  first's, and the tests `the-fixtures-records-give-its-tables-with-4-tasks` and `the-fixtures-records-give-its-tables-with-8-tasks` that a store gives the rows which the store gave
  that the fixture was made of.
- two acts of one layer with one stamp: the naming rule names an act whose
  stamp is higher than one behind it or lower than one before it, and an equal
  stamp is neither. The module's proof holds that the stamps rise strictly in
  each layer.

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
