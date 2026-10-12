# Rebuild — what a rebuild needs of Rama

[Parent: store](../README.md) · [Store source entry](../../../README.md).

The rebuild is [rebuild.clj](../rebuild.clj), one folder up: pure code that makes
every row of every table again from the logs alone, and decides nothing. It takes
the logs as plain data and requires no Rama. This folder holds the two things
that a rebuild of a running module needs from a cluster, and both require Rama.

| File | Responsibility | State and lifetime |
|---|---|---|
| [reader.clj](reader.clj) | Takes the logs off a running module, in the form that the rebuild takes: the logs of acts as a list, each with its gate; each gate's log of answers; the log of the clocks of batches. Partition by partition, in ranges of a bounded number of records | It reads the cluster and changes nothing. It keeps nothing after a call; each log is whole in memory when it returns |
| [placing.clj](placing.clj) | What gives the task of a route: a module of one query, in which Rama's hash partitioner does the placing, and the client's call of it | The module declares no depot and no PState. It is launched beside the store, with the store's number of tasks |

## How the files relate

Neither file requires the other, and neither requires [rebuild.clj](../rebuild.clj).
A caller reads the logs with [reader.clj](reader.clj), asks
[rebuild.clj](../rebuild.clj) which routes it needs the task of, gets those tasks
from [placing.clj](placing.clj), and hands the rebuild the logs with them.

## What it borrows

The depots of the store's module, by their names: they are declared and owned in
[module/](../module/README.md). The reader appends to none. Rama's hash
partitioner, through a module of its own; [placing.clj](placing.clj) says what
that rests on and what is not decided.

## What is not here

A rebuild on a running store, and a rebuild of a store too large for memory:
[rebuild.clj](../rebuild.clj) says what that would need. Neither is built. The
comparison of what a rebuild makes with what the gates wrote is a proof, under
`test-store/`.

No gate, no door and no read calls the rebuild. In this build its proofs are its
only callers.

## Decisions this build stands on

Sid's decisions are in [the store source entry](../../../README.md), with his
reasons. The rebuild stands on the section of 12 October 2026, "before real data
is kept", by entry:

| Entry | Decision | Where it shows in the rebuild |
|---|---|---|
| 1 | Every table can be made again from the logs. A gate decides once, and what it decided also goes into a log, so that a rebuild applies the answers and decides nothing again | [rebuild.clj](../rebuild.clj) makes every row of every table from the logs of acts, the logs of answers and the log of the clocks of batches. It runs no check of a gate and no test for a stray, and reads no clock: the answer that the log holds is the one applied, also where the gate of today would give another |
| 2 | Every value is plain in the log, and the store keeps no locks | The rebuild takes each value from the thing in a log of acts, and no table is outside the logs |
| 3 | The log is kept whole | The rebuild reads each log from its start, and makes up nothing for a record that is not there: an entry with no thing gives no row. [reader.clj](reader.clj) reads a depot that was trimmed from where it starts |
| 7 | An act from a maker who may not act is written to the log, and then refused | A refused thing is in a log of acts and its refusal in a log of answers: the rebuild makes its answer's row and no row of a fact |

The rest by pointer. The two depots of acts and the gates' PStates as the record
that is read (7 October), the stamp, which gives the order in which the acts are
applied (8 October), the store's keys and their namespace (8 and 9 October), and
what the gates keep for a read, the facts by value and who wrote a fact (11 and
12 October), are read through the functions of the gates that the rebuild
calls.

## Engineering choices

Each is the builder's, with its reason. None is a decision of Sid's, and each is
provisional. The reasons in full are in the docstrings that the lines name.

**The core is pure, and Rama is beside it.** [rebuild.clj](../rebuild.clj) takes
the logs as plain data and loads with Clojure alone. So the road from the logs
to the rows is held with no cluster, and a proof can hand it logs that no gate
wrote. The two files here are the two things that a cluster must give.

**It decides nothing, and the text of a row is the gate's.** Giving a thing its
one form is not deciding; refusing it is. An accepted thing gets its one form
from the declaration's check, and its rows from the function of the decision
that gives a gate its rows, handed what a gate hands it. Where the check of
today refuses a thing that the log says was accepted, the answer's row is made
as it was logged, no row of a fact is made, and the rebuild names the act.

**One entry is taken where several are equal.** A log of answers holds equal
copies of an entry, so what is made does not depend on how many there are.
Entries under one act-id that differ are named, and no row is made of them.

**An entry is tied to its thing by two equalities:** the act-id, and the digest
of the thing. A digest is taken only of a thing under an act-id that has an
entry. An entry with no thing is named, and no row is made from an answer
alone.

**The acts are applied in the order of their stamps, never of a depot.** The
order of a partition's offsets is the order of the appends, and a copy can
stand behind a later act's entry.

**Which kind a decision is comes from its record, never from a gate's word.** A
record with no `:batch` holds a decision that was made in an event of its own:
its rows are on the task of the partition where its entry is, and a key that it
newly marks gets its own stamp. A record with `:batch` holds a decision of a
batch: its rows are placed by route, it is decided with the marks as they were
when its batch began, and a key that it newly marks gets the batch's last
stamp. So a later depot of acts needs no new branch.

**What each part of each row is made from.**

| Table, part | Made from |
|---|---|
| An answer: its key | The entry's act-id: its layer and its uuid |
| An answer: the word of a refusal, the stamp, the digest | The entry, as it holds them |
| A fact: its key, its value, the fact that wrote it, who, the permission | The thing, in its one form, only when the entry holds no refusal |
| A fact: its stamp | The entry's stamp |
| The row of the marked keys: since which stamp each key is level | For a key that stays marked, the row before, which the rebuild has itself made. For a key that is newly marked: the stamp of the marking act's entry, or, for a decision of a batch, the last stamp of its batch, from the batch's first record |
| A row of the facts by value: its key, and that it is put or taken out | The thing; the key's stamp, as above; the row before at the place, which the rebuild has itself made |
| What a gate keeps for a task: the last stamp | For decisions made in events of their own, on each task the greatest stamp of the entries on its partition. For decisions of batches, on the gate's task alone, the last stamp of the batch with the greatest id that holds one |
| The same: the strays | Each stray's entry with that task: its word, and its digest into the set of that word. A stray that arrived twice is one member |
| The same: the stamp through which a task's rows are of closed batches | That last stamp of the newest batch, on every task: what a store at rest holds |
| The kept clock of a batch | The id and the clock of the batch with the greatest id that holds a clock, from the gate's log of answers. The log of the clocks of batches is the check |
| The task of a row | A decision made in an event of its own: the partition of its entry. A decision of a batch: the task of the row's route, as the caller hands it; a row under one of the five keys that every task holds, on every task |

**What is not made again, each with its reason.** Three parts of what the
microbatch gate keeps for a task. `:turns`: the working state of one run of the
gate; no log holds it, and nothing reads it once its batch is committed.
`:to-keep`: the entries that a batch has still to append; on a store at rest it
holds no key. `:to-close`: what the next run has to close; on a store at rest
it is absent. A comparison of what a rebuild made with the tables leaves these
three out.

**What no log holds is handed in.** The module's number of tasks, and the task
of a route, on which the placing of each row of a batch rests. The rebuild has
no hash of its own to keep equal to Rama's: [placing.clj](placing.clj) asks
Rama where it does the placing itself, in a topology, and says what that rests
on.

**What the rebuild names.** A thing that it cannot make a row of is named, with
why, and is not passed over in silence: an entry with no thing, entries that
differ, an accepted thing whose form cannot be given, an accepted act of a batch
with no last stamp, records of one batch that differ, and a kept clock that the
log of clocks does not hold.

**The order of the reader's reads:** the logs of answers, then the logs of acts,
then the log of clocks. A gate appends an entry after the thing that it answers
is in its log of acts, and a batch's entries after its clock is in the log of
clocks. So each entry that was read has its thing and its clock among what was
read, also on a store that is not at rest.

**Every log is whole in memory.** The reader gives each log whole, and the core
holds every entry, two indexes and every row that it makes. The namespace
docstring of [rebuild.clj](../rebuild.clj) says what a store too large for that
would need. None of it is built.

## Not decided, kept for now

- **The use of a rebuild on a store.** Nothing puts the rows that a rebuild
  makes into a table. The exchange of the tables of a store for tables that were
  made again, what its makers and its askers are told meanwhile, and the
  downtime are not built and not decided. Kept: the rebuild gives rows as data,
  and its proofs compare them with what the gates wrote.
- **A rebuild at another number of tasks.** The rebuild is handed the number of
  tasks and the placing. Kept: both are those of the module that decided the
  acts; no check holds a rebuild at another number.
- **A store that is not at rest.** Kept: what falls out. A thing with no entry
  gives no row, and the stamp through which a task's rows are closed is made as
  a store at rest holds it. No check holds it equal to the tables of a store
  whose gates are deciding.
- **Where the query for the task of a route lives.** Kept: in a module of its
  own beside the store, because this build adds no topology to the store. The
  other answer is one query more in the store's own module.
  [placing.clj](placing.clj) is the one place.
- **What follows when the rebuild names a thing.** Kept: the thing is named, the
  rows that can be made are made, and no row is guessed. Who is told, and
  whether a rebuild with a named thing may replace a table, is not decided.

## Words a session made, Sid's to change

Each says what the thing is. None comes from the rig's code. Carried and not
made here: "entry", "copy", "depot of answers", "keeping task" and the parts of
a record are the words of the build of the logs of answers, on
[the store's page](../README.md); "route" and "home" are the words of a batch.

| Word | What it is |
|---|---|
| rebuild | The making of every row of every table again from the logs, with no decision made again |
| `softland.store.rebuild`, `softland.store.rebuild.reader`, `softland.store.rebuild.placing` | The three namespaces: the pure core, the reader of the logs, and what gives the task of a route |
| `from-logs`, `routes-asked` | In [rebuild.clj](../rebuild.clj): the one call that makes the rows, and the call that says which routes a caller must ask the task of |
| `:tasks`, `:task-of-route`, `:acts`, `:answers`, `:batch-clocks` | What `from-logs` takes: the number of tasks, the placing, and the logs |
| `:gate`, `:log` | In one log of acts as it is handed over: the word of the gate that decided it, and the log |
| `:tables`, `:batch-clock`, `:named`, `:left-out` | What `from-logs` gives: the rows by gate, table and task; the kept clock; what it names; and the parts that it does not make |
| a decision made in an event of its own; a decision of a batch | The two kinds of decision, told by whether the record holds `:batch` |
| to take an entry | To use one of several equal entries under one act-id |
| to tie an entry to its thing | To find the thing with the entry's act-id and digest |
| to name; `:why` | To say of a thing that no row, or not every row, could be made of it, with the reason word |
| `:entry-with-no-act`, `:entries-differ`, `:form-cannot-be-given`, `:no-last-stamp-of-its-batch`, `:records-of-a-batch-differ`, `:clock-not-in-the-log-of-clocks` | The six reason words of a named thing. The docstring of `from-logs` says what is made in each case |
| left out, `left-out` | The parts that no log gives and that the rebuild does not make: `:turns`, `:to-keep`, `:to-close` |
| `bytes-as-hex` | In [rebuild.clj](../rebuild.clj): a value in a form that `=` compares, for a row that holds a run of bytes |
| `logs`, `depots` | In [reader.clj](reader.clj): the call that reads the logs off a module, and the depots that it reads, as data |
| `Placing`, `tasks-of-routes` | In [placing.clj](placing.clj): the module of one query, and the client's call that gives the task of each route |
