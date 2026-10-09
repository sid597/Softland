# Gate — the decision, as pure code

[Parent: store](../README.md) · [Store source entry](../../../README.md).

This folder holds what a gate decides with: the checks of one act against rows
of the record, the form of a layer's first act, the stamp, and the steps of one
batch of the microbatch gate. It is pure code and loads with Clojure alone.

No function here reads a PState, a clock or a random source. A gate reads the
rows, calls these functions, and writes what they give. So the same checks serve
both gates, each gives the same answer when Rama runs the work again, and each is
proven without a cluster.

The two gates themselves, the topologies that read and write the record, are in
[module/](../module/README.md).

| File | Responsibility | State and lifetime |
|---|---|---|
| [decision.clj](decision.clj) | The checks of one act against rows of the record, and the rows that a decision writes, as data. | None. It reads no PState. |
| [first_act.clj](first_act.clj) | The depot that a layer starts with, and the form of a making act. | None. |
| [stamp.clj](stamp.clj) | The stamp: clock time and a counter in one integer, the next stamp, the 10-second rule. | None. It reads no clock. |
| [batch.clj](batch.clj) | The pure code of one batch of the microbatch gate: what the depot derives from a thing, the turn of a thing, what an act asks of other tasks and what they reply, which acts meet, the decision of an act with its rows in hand, the home of each row. | None. It reads no PState. |

## How the files relate

[decision.clj](decision.clj) holds each check. It asks
[first_act.clj](first_act.clj) whether an act has the form of a making act, and
[stamp.clj](stamp.clj) for the stamp of a decision. [batch.clj](batch.clj) adds
no check: it puts the things of a batch in one order and calls
[decision.clj](decision.clj) on one act with all its rows in hand.

## What it borrows

The form of what arrived, and the opening of a sealed value, are
[door/](../door/README.md)'s. Each row that a check reads or a decision writes is
made by [record/](../record/README.md).
