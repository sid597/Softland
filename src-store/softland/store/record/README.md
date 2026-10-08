# Record — what the gates keep

[Parent: store](../README.md) · [Store source entry](../../../README.md).

This folder says what the record is made of: its rows, and the schemas of the
PStates that hold them. It holds no record and it declares nothing. Each gate
declares its own PStates with these schemas, because in Rama only the topology
that owns a PState can write it. Those PStates are durable state of the cluster,
and they are owned in [module/](../module/README.md).

| File | Responsibility | State and lifetime |
|---|---|---|
| [rows.clj](rows.clj) | The record's rows: the text of each key, the making and the reading of each row, the routing id of an act-id, and the test of what a caller gave a read. Pure code; it loads with Clojure alone. | None. It holds no record. |
| [tables.clj](tables.clj) | The record as PStates: the schemas, written once, and what each gate keeps for a task. It requires Rama. | None. It declares nothing. |

## How the files relate

[rows.clj](rows.clj) makes each row and reads each row. [tables.clj](tables.clj)
gives the schemas, which take exactly the classes that those rows hold. A row of
another class throws at the write, and a throw in a topology stops its
partition; so a gate writes no row that [rows.clj](rows.clj) did not make.
Neither file requires the other.

## What it borrows

A row's key is made of the printed texts of its ids, and a plain value is kept
as the bytes of its one text. Both are [door/](../door/README.md)'s.
