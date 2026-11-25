# Gate — the decision, as pure code

[Parent: store](../README.md) · [Store source entry](../../../README.md).

This folder holds what a gate decides with: the checks of one act against rows
of the record, the form of a lane's first act, and the stamp. It is pure code
and loads with Clojure alone.

No function here reads a PState, a clock or a random source. The walk of one
thing, which both gates call, reads the rows, calls these functions, and writes
what they give. So the same checks serve both gates, each gives the same answer
when Rama runs the work again, and each is proven without a cluster.

The time that a decision is made with is handed in, the same for both gates: the
time on the act, which the door wrote, and the last stamp of the act's lane. A
stamp is counted in its lane, and nothing of a batch is in it.

The two gates themselves and the walk, the code that reads and writes the
record, are in [module/](../module/README.md).

| File | Responsibility | State and lifetime |
|---|---|---|
| [decision.clj](decision.clj) | The checks of one act against rows of the record, and the rows that a decision writes, as data, those of the facts by value among them, and the use of a yes for one act by the act that it was taken under. And the rows that a decision writes in the lane's line, with the two bounds on what a gate keeps of a thing: 32 MiB of weight from a sender whose permission stands, 256 KiB from one with none or for a thing that the declaration refused. | None. It reads no PState. |
| [chain.clj](chain.clj) | The chain of a permission: the permissions above it, the places of their rows, and when a permission stands. The place of a permission is read from what it covers: its holder for the lane, the session for one run, a yes's own id for a yes. It holds how a permission ends, how what was given beneath it ends with it, and how a permission that is given again is told from the one that ended. | None. |
| [first_act.clj](first_act.clj) | The depot that a lane starts with, whether that is the other gate's depot, the form of a making act, and what a lane starts with when no fact says who may see it, who may read its acts whole, what it builds on or which keys it marks. | None. |
| [stamp.clj](stamp.clj) | The stamp: the position of a decision in its lane, the lane's last stamp plus one. It holds no time and nothing that the act carries. | None. It reads no clock. |

## How the files relate

[decision.clj](decision.clj) holds each check. It asks
[first_act.clj](first_act.clj) whether an act has the form of a making act and
whether the act's lane starts with the other gate's depot,
[chain.clj](chain.clj) whether a permission stands, by the rows of the
permissions above it, and whether one permission is above another,
and [the act](../act.clj) what the act's permission allows and what it
covers. For an act under a yes for one act it asks
[the digest](../door/digest.clj) for the digest of the act as its maker made
it, the function by which the maker made it, and holds it against the yes;
the accept writes the yes's use, an end at its place, so the yes covers its
act once. The read path asks
[chain.clj](chain.clj) the same question of the same rows, so a gate and a read
never differ on whether a permission stands. The walk asks
[stamp.clj](stamp.clj) for the stamp of a decision; no check reads a stamp
that an act carries, or the time on the act. Whether a lane is the other
gate's is asked of the lane id alone, in both gates: no check reads the other
gate's record.

## What it borrows

The form of what arrived is [door/](../door/README.md)'s: its check gives a gate
the act in its one form on arrival, with the time on the act, and each fact
with the one text of its value, which the fact's row takes as it is. Each row
that a check reads or a decision writes is made by
[record/](../record/README.md).
