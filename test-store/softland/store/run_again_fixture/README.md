# The run-again fixture — kept acts, and the tables that they gave

[Parent: store proofs](../README.md).

This folder holds one fixture: the records of both depots of acts of a store, and
the tables that the store's gates made of them. A later build must make the same
tables from the same records, also on a store with another number of tasks.

The folder holds data and no code. One proof reads it,
[`softland.store.run-again-proof`](../run_again_proof.clj), as resources of the
classpath. Its namespace docstring holds the form of each file, how a run of
bytes is told from a string, and what each check does; each file's head says the
same in short.

| File | What it holds | Who writes it |
|---|---|---|
| [acts.edn](acts.edn) | The records of the two depots of acts as they stood on a store of four tasks: for each depot, for each partition in order, its records in the partition's order, each as it was appended, with its act-id, its depot word and the time on it. | `make-fixture`, below. Nothing else. |
| [tables.edn](tables.edn) | The tables that those records gave, in a form that names no task: for each gate and each of its four tables, the rows as a sorted vector of `[key row]`. | The same run of `make-fixture`: the two files are one making, and neither is changed alone. |

## What the proof does with it, and what a pass shows

Two checks of the proof read the fixture. Each launches the store's module on a
fresh in-process cluster, with 4 tasks and then with 8, appends the records of
`acts.edn` to the two depots by hand, each as it is and with its own time, each
partition's records in their order, and holds the store's tables against
`tables.edn`, row by row. A pass shows that this build's gates give, for the
kept acts, the rows that the store gave which the fixture was made of: on the
number of tasks of the making, and on another. It does not show that those rows
are right. They are what the store of the making gave.

When a file is absent, both checks fail with "the fixture is not made: run
make-fixture".

## How it was made, and how it is made

This fixture was made on 14 October 2026. It is that day's third making. The
first two were made by the build "run again", in which both depots came to
place an act by its layer and the base's gate came to decide each act on the
layer's task in the depot's order: the first after the build's part B, the
second at the build's end, for two rules changed after the build's review
(`src/proposal/backend-on-rama-2026-10-02/run-again/RECEIPT.md`). The third
replaced the second for the rules that Sid's decisions of 14 October 2026
changed on purpose: order and time are kept apart, and order never uses the
time.

- the stamp of a decision, an accept or a refusal, is its layer's count of
  decisions: the layer's last stamp plus one, or 0. It holds no time, and an
  act's stamp is not raised above the stamps that the act carries;
- the time on a thing is a label beside the stamp: the answer holds it when
  the thing held a usable one, a refused thing too, and each row of a fact and
  of the facts by value holds the time of the act that wrote it;
- no rule reads the time or a stamp that an act carries: the rule that refused
  an act carrying a stamp more than 10 seconds ahead of its time is gone, with
  its word `:carried-stamp-too-far-ahead`;
- the time on an act and a carried stamp have no upper bound: each is any
  whole number not below zero.

The hand changed with the rules. Its things whose time is chosen are now an
act with no time, an act with a time an hour behind, an act that carries the
greatest long as a stamp, an act with a time in the year 3084, and two honest
acts behind it, the second based on the fact of the first.

No run held the store with these rules against the second making. What
follows is derived from the rules and the second making's `tables.edn`, not
run. Fed the second making's records, the store would give another value in
463 of its 475 rows: every answer (its stamp, or a `:time` that it did not
hold), every row of a fact and of the facts by value (`:time`, and the stamps
in it), and the last stamp of each layer but two. A key of the facts by value
holds the stamp of its key's marking, so none of those keys would be found.
Twelve rows would keep their value: the ten rows of strays, whose digests do
not hold a stamp, and the last stamps of the two layers whose only decisions
are two refusals (`:base` in the streaming gate, `:alice` in the microbatch
gate), which a refusal already counted 0 and 1. The second making's act that
carried a stamp an hour ahead of its time was refused; it would be accepted,
and write a fact's row in each gate and a row of the facts by value in the
streaming gate.

The third making ran on 14 October 2026 at 14:48 IST. It wrote `acts.edn`,
104,788 bytes, 337 records (the streaming depot's four partitions 132, 0, 17
and 5; the microbatch depot's 7, 0, 176 and 0), and `tables.edn`, 90,418
bytes, 488 rows: in the streaming gate 142 answers, 61 facts, 25 facts by
value and 14 rows of the task's table; in the microbatch gate 169, 64, 6 and
7. The run of all seven proofs after it passed, the two checks of this
fixture among them. The receipt of the change says that the fixture was made
again, and why (`src/proposal/backend-on-rama-2026-10-02/order-and-time/RECEIPT.md`).

From the repository root, behind the lock of this machine's cluster runs:

```sh
clojure -M:store-module -m softland.store.run-again-proof make-fixture
```

It launches the store with four tasks, plays the proof's hand with the fixture's
numbers, waits for rest, reads each depot partition's records and every table,
and writes the two files here. It writes nothing when the store does not hold
what the fixture is for, or when a file's text does not read back as its value.
A folder named behind `make-fixture` gets the files in place of this one; that
is how a second making is held against the first. A making gives other uuids and
other times, and so other digests and other labels of time, than the one before:
the two files of two makings are not the same text.

A run of the proof never writes here.

## When it may be made again

The rule is a session's text, which Sid pasted into his prompt of 13 October
2026 as his decision 6. The record shows that he did not type it:

> A change in what a gate accepts is a new version of the act. The old version's
> rules stay in the code. This starts with the first real record. Until then the
> fixture is made again only when a rule is changed on purpose, and the receipt
> says so. In this build it means the rule on the page and the fixture; the code
> still knows one version.

So the fixture is not made again to make a failing proof pass.

## When the proof fails on the fixture

The store changed what it gives for kept acts. The proof's lines that begin
`DIFFERENT` name the first rows, each with its gate, its table and its key. It
is one of two things:

- a fault: the build changed an answer, a row or a stamp and did not mean to.
  The build is repaired, and the fixture stays as it is;
- a rule that was changed on purpose. Then the receipt of the build that
  changed it says which rule, and that the fixture was made again for it.
