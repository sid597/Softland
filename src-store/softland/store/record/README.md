# Record — what the gates keep

[Parent: store](../README.md) · [Store source entry](../../../README.md).

This folder says what the record is made of: its rows, the keys of its two
orders of facts, and the schemas of the PStates that hold them. The first order
is by place: layer, entity, key. The second, the facts by value, is kept only
for the keys that a layer marks: layer, key, the stamp since which the key is
marked, the digest of the value, entity. It holds no record and it declares
nothing. Each gate declares its own PStates with these schemas, because in Rama
only the topology that owns a PState can write it. Those PStates are durable
state of the cluster, and they are owned in [module/](../module/README.md).

The rows and the schemas are the same for both gates. Where a row lives is not:
every row of a streaming layer is on the layer's task; of a microbatch layer the
facts and the facts by value are on every task, and the answers on the layer's
task. [rows.clj](rows.clj) says it row by row.

| File | Responsibility | State and lifetime |
|---|---|---|
| [rows.clj](rows.clj) | The record's rows: the text of each key, the making and the reading of each row, the text that a fact's row holds for its value, the run of one value in the facts by value and the digest that its keys hold, the routing id of an act-id, which is its layer, and the test of what a caller gave a read. And what only a reader of the tables needs: the two ends of a run of places, the order in which a table keeps its keys, and two tests of one row against another row or a value. Pure code; it loads with Clojure alone. | None. It holds no record. |
| [tables.clj](tables.clj) | The record as PStates: the four schemas, written once for both gates. The answers, the facts, the facts by value, and what a gate keeps for a task: the last stamp of each layer, since a stamp is counted in its layer, and the strays. It requires Rama. | None. It declares nothing. |

## How the files relate

[rows.clj](rows.clj) makes each row and reads each row. [tables.clj](tables.clj)
gives the schemas, which take exactly the classes that those rows hold. A row of
another class throws at the write, and a throw in a topology stops its
partition; so a gate writes no row that [rows.clj](rows.clj) did not make.
Neither file requires the other.

## What it borrows

A row's key is made of the printed texts of its ids. Every value is kept plain,
as its one text: the text of the value in its normal form, made once by the
last step of the declaration's check, for a fact under a plain key and for a
fact under a store key alike. A fact's row holds that text as a String, so that
no field of a table that a reader is kept live on is a byte array;
[tables.clj](tables.clj) says why. Who wrote a fact and the permission that its
act named are kept as their one text too. The printed text of an id, the one
text and the store keys are [door/](../door/README.md)'s. Whether a thing is a
value, its normal form and the shape of a permission are
[the act](../act.clj)'s.
