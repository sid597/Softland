# Gate — the decision, as pure code

[Parent: store](../README.md) · [Store source entry](../../../README.md).

This folder holds what a gate decides with: the checks of one act against rows
of the record, the form of a layer's first act, and the stamp. It is pure code
and loads with Clojure alone.

No function here reads a PState, a clock or a random source. The walk of one
thing, which both gates call, reads the rows, calls these functions, and writes
what they give. So the same checks serve both gates, each gives the same answer
when Rama runs the work again, and each is proven without a cluster.

The time that a decision is made with is handed in, the same for both gates: the
time on the act, which the door wrote, and the last stamp of the act's layer. A
stamp is counted in its layer, and nothing of a batch is in it.

The two gates themselves and the walk, the code that reads and writes the
record, are in [module/](../module/README.md).

| File | Responsibility | State and lifetime |
|---|---|---|
| [decision.clj](decision.clj) | The checks of one act against rows of the record, and the rows that a decision writes, as data, those of the facts by value among them. | None. It reads no PState. |
| [first_act.clj](first_act.clj) | The depot that a layer starts with, whether that is the other gate's depot, the form of a making act, and what a layer starts with when no fact says who may see it, what it builds on or which keys it marks. | None. |
| [stamp.clj](stamp.clj) | The stamp: a time and a counter in one integer; the next stamp, of the time on the act, the layer's last stamp and the stamps that the act carries; the 10-second rule. | None. It reads no clock. |

## How the files relate

[decision.clj](decision.clj) holds each check. It asks
[first_act.clj](first_act.clj) whether an act has the form of a making act and
whether the act's layer starts with the other gate's depot,
[the act](../act.clj) what the act's permission allows, and
[stamp.clj](stamp.clj) for the stamp of a decision. Whether a layer is the other
gate's is asked of the layer id alone, in both gates: no check reads the other
gate's record.

## What it borrows

The form of what arrived is [door/](../door/README.md)'s: its check gives a gate
the act in its one form on arrival, with the time on the act, and each fact
with the one text of its value, which the fact's row takes as it is. Each row
that a check reads or a decision writes is made by
[record/](../record/README.md).
