# Gate — the decision, as pure code

[Parent: store](../README.md) · [Store source entry](../../../README.md).

This folder holds what a gate decides with: the checks of one act against rows
of the record, the rule of a chain of rights, the form of a layer's first act,
and the stamp. It is pure code and loads with Clojure alone.

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
| [decision.clj](decision.clj) | The checks of one act against rows of the record: its standing, its places, the end of a right, the keys that its right allows, and the claims of a place under a key that the layer marks as taking one write; and the rows that a decision writes, as data, those of the facts by value among them. | None. It reads no PState. |
| [chain.clj](chain.clj) | The chain of a permission: its levels, the place of each, whether every level still stands after the ends that the record holds, and whether one permission is above another. The one rule of a chain, for the gates and for the read path. | None. |
| [first_act.clj](first_act.clj) | The depot that a layer starts with, whether that is the other gate's depot, the form of a making act, and what a layer starts with when no fact says who may see it, what it builds on or which keys it marks. | None. |
| [stamp.clj](stamp.clj) | The stamp: the position of a decision in its layer, the layer's last stamp plus one. It holds no time and nothing that the act carries. | None. It reads no clock. |

## How the files relate

[decision.clj](decision.clj) holds each check. It asks
[first_act.clj](first_act.clj) whether an act has the form of a making act,
whether the act's layer starts with the other gate's depot, and what a layer's
one-write keys are while no fact says them; [chain.clj](chain.clj) which places
the levels above the act's permission sit on, whether they all stand, and
whether the act's permission is above a right that it ends; and
[the act](../act.clj) what the act's permission allows, whether that allows a
fact's key, and whether a right that the act gives allows no more. The read
path asks [chain.clj](chain.clj) the same rule of a chain for who may see a
layer. The walk asks
[stamp.clj](stamp.clj) for the stamp of a decision; no check reads a stamp
that an act carries, or the time on the act. Whether a layer is the other
gate's is asked of the layer id alone, in both gates: no check reads the other
gate's record.

## What it borrows

The form of what arrived is [door/](../door/README.md)'s: its check gives a gate
the act in its one form on arrival, with the time on the act, and each fact
with the one text of its value, which the fact's row takes as it is. Each row
that a check reads or a decision writes is made by
[record/](../record/README.md).
