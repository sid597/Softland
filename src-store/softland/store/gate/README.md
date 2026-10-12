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
| [decision.clj](decision.clj) | The checks of one act against rows of the record, and the rows that a decision writes, as data, those of the facts by value among them. | None. It reads no PState. |
| [first_act.clj](first_act.clj) | The depot that a layer starts with, the form of a making act, and what a layer starts with when no fact says who may see it, what it builds on or which keys it marks. | None. |
| [stamp.clj](stamp.clj) | The stamp: clock time and a counter in one integer, the next stamp, the 10-second rule. | None. It reads no clock. |
| [batch.clj](batch.clj) | The pure code of one batch of the microbatch gate: what the depot derives from a thing, the turn of a thing, the pure code of a batch's clock (which clock is kept for a batch, and the rule by which the first one that is kept stays), what an act asks of other tasks and what they reply, since which stamp a place's key is marked, which acts meet, the decision of an act with its rows in hand, the home of each row, that of a row of the facts by value too. And the pure parts of the keeping: the keeping task of a batch, what each step hands on to keep, and the records that the run that closes a batch appends to the gate's depot of answers. | None. It reads no PState and appends nothing. |

## How the files relate

[decision.clj](decision.clj) holds each check. It asks
[first_act.clj](first_act.clj) whether an act has the form of a making act,
[the act](../act.clj) what the act's permission allows, and
[stamp.clj](stamp.clj) for the stamp of a decision. [batch.clj](batch.clj) adds
no check: it puts the things of a batch in one order and calls
[decision.clj](decision.clj) on one act with all its rows in hand.

## What it borrows

The form of what arrived is [door/](../door/README.md)'s: its check gives a gate
the act in its one form on arrival, each fact with the one text of its value,
which the fact's row takes as it is. Each row that a check reads or a decision
writes is made by [record/](../record/README.md).
