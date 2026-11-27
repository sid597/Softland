# Gate — the decision, as pure code

[Parent: store](../README.md) · [Store source entry](../../../README.md).

This folder holds what a gate decides with: the walk of one thing as a pure
function of the thing and of the rows that it asks for, the checks of one act
against rows of the record, the chain of a permission, the form of a lane's
first act, and the stamp. It is pure code and loads with Clojure alone.

No function here reads a PState, a clock or a random source. Whoever drives the
walk reads the rows that it asks for and writes the rows that it gives. So the
same checks serve both gates, each gives the same answer when Rama runs the work
again, and each is proven without a cluster.

The time that a decision is made with is handed in, the same for both gates: the
time on the act, which the door wrote, and the last stamp of the act's lane. A
stamp is counted in its lane, and nothing of a batch is in it.

The two gates themselves, the code that reads and writes the record, are in
[module/](../module/README.md). On 27 November 2026 that code is still in the
form before the permission fix: it does not drive [walk.clj](walk.clj) yet, and
does not load against this folder.

| File | Responsibility | State and lifetime |
|---|---|---|
| [walk.clj](walk.clj) | The walk of one thing, in steps: what it wants read now, what it does with what the reads gave, and, when it is done, what happened and which rows to write. The order of a decision is here and nowhere else: the stray, the answer under the act-id, the door's declaration, the standing, the places, the lane's marked keys, the stamp, the writes. | None. It reads nothing and writes nothing. |
| [decision.clj](decision.clj) | The checks of one act against rows of the record: whether it may be heard at all, under the permission that it names; what each of its facts needs, a give, a change and an end of a permission among them; and its result held as a whole. Each step says which rows it wants next. And the rows that a decision writes, as data: those of the facts, of the facts by value, of the permissions by holder and of a lane's owners, the use of a permission for one exact act, and the lane's line, with the two bounds on what a gate keeps of a thing. | None. It reads no PState. |
| [chain.clj](chain.clj) | The chain of a permission: the permissions that it is lent from, found one row at a time; whether it stands; what it allows at this moment, which is no more than each permission above it allows; and who holds it, everyone's among them. | None. |
| [first_act.clj](first_act.clj) | The depot that a lane starts with, whether that is the other gate's depot, the form of a making act, the ids that a maker gives a lane's first permissions, and what a lane starts with when no fact says who may read its acts whole, what it builds on or which keys it marks. | None. |
| [stamp.clj](stamp.clj) | The stamp: the position of a decision in its lane, the lane's last stamp plus one. It holds no time and nothing that the act carries. | None. It reads no clock. |

## How the files relate

[walk.clj](walk.clj) holds the order of a decision and asks
[decision.clj](decision.clj) for each check and for the rows to write.
[decision.clj](decision.clj) asks [first_act.clj](first_act.clj) whether an act
has the form of a making act and whether the act's lane starts with the other
gate's depot, and [chain.clj](chain.clj) whether a permission stands, what it
allows at this moment, and whether one permission is lent from another. For an
act under a permission for one exact act it asks
[the digest](../door/digest.clj) for the digest of the act as its maker made
it, the function by which the maker made it, and holds it against what the
permission says; the accept writes the permission's use at its place, so it
covers its act once. The read path asks [chain.clj](chain.clj) the same
questions of the same rows, so a gate and a read never differ on whether a
permission stands or on what it allows. The walk asks [stamp.clj](stamp.clj)
for the stamp of a decision; no check reads a stamp that an act carries, or the
time on the act. Whether a lane is the other gate's is asked of the lane id
alone, in both gates: no check reads the other gate's record.

## What it borrows

The form of what arrived is [door/](../door/README.md)'s: its check gives a gate
the act in its one form on arrival, with the time on the act, and each fact
with the one text of its value, which the fact's row takes as it is. Each row
that a check reads or a decision writes is made by
[record/](../record/README.md).
