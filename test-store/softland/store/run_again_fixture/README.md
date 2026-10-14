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

This fixture was made on 14 October 2026, by the build "run again": the build
in which both depots came to place an act by its layer, and the base's gate
came to decide each act on the layer's task in the depot's order. It is that
day's second making. The first followed the build's part B. The second
replaced it at the build's end, for two rules that were changed on purpose
after the build's review:

- the stamp of a refusal is its layer's last stamp plus one, or 0, and holds
  nothing of the time on the thing: a refused thing moves no layer's time;
- the time on an act is below 2^45 milliseconds, where it was 2^46.

Before the first making was replaced, the store with the two rules was held
against it: 14 of its 475 rows had another value, with 4 tasks and with 8. They
were the stamps of 12 refused answers and the last stamps of 2 layers whose last
decision is a refusal. No row of a fact and no row of the facts by value
differed. The build's receipt says the same
(`src/proposal/backend-on-rama-2026-10-02/run-again/RECEIPT.md`).

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
other times, and so other stamps and digests, than the one before: the two files
of two makings are not the same text.

A run of the proof never writes here.

## When it may be made again

Sid, 13 October 2026:

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
