# Store verification

[Store source entry](../src-store/README.md).

This tree owns checks of the store's code and what they print, not accepted
behavior.

| Immediate child | Responsibility |
|---|---|
| [softland/](softland/README.md) | The JVM checks: the act, the gates' pure namespaces and the door's, and the read path's pure namespaces, with no cluster; the store's module, the door, the read path, and the store's gates run again over its depots, each on Rama's in-process cluster; and the measures of the read build and of the gates, two runs that are no check. |

Run the checks from the repository root:

```sh
clojure -M:store -m softland.store.act-proof
clojure -M:store -m softland.store.pure-proof
clojure -M:store-module -m softland.store.module-proof
clojure -M:store-module -m softland.store.door-proof
clojure -M:store -m softland.store.read-path-pure-proof
clojure -M:store-module -m softland.store.read-path-proof
clojure -M:store-module -m softland.store.run-again-proof
```

The first two and the fifth need local Clojure and nothing else: the alias
`:store` puts Clojure alone on the classpath. They start no browser, connect to
no cluster and call no provider.

The third, the fourth, the sixth and the seventh need Rama, which the alias
`:store-module`
adds. Each starts Rama's in-process cluster inside its own JVM, with four tasks
and two workers, launches the module in it, and closes it at the end. The third
and the sixth each start two such clusters, one after the other: the
second is
for the groups that make a gate fail and run again. The seventh starts four,
one for each of its checks, one of them with eight tasks where the others have
four; on two of them it launches a second module of its own beside the
store's. Where a
proof makes a gate fail, it puts functions of its own into the vars of pure
functions of the store that the walk of one thing calls, each of which gives
what the store's gives but for what the proof asks, and puts the store's back.
Each connects to
nothing outside its process. The cluster keeps its files in a temporary
directory of Rama's.

Each prints what it observed on lines that start with `SHOWN`, prints those lines
again together at the end, and exits with a status that is not zero when a check
fails.

The seventh reads a fixture, kept acts and the tables that they gave, which
its own mode makes:

```sh
clojure -M:store-module -m softland.store.run-again-proof make-fixture
```

That mode runs no check. It is run only when a rule is changed on
purpose: [the fixture's map](softland/store/run_again_fixture/README.md) says
when.

A pass of the first two shows the pure code on values of one process. A pass of
the third shows the module in one process: acts that went through each depot and
its gate, and were read back by act-id; and what each gate keeps for a read,
selected straight from its tables. The third plays the door by hand, so the
store stays proven without the door. It also plays a reader of the tables by
hand, so the gates stay proven without the read path. A pass of the fourth shows the door in one
process: acts that a maker gave it, which went through each depot and its gate,
and whose answers the door read back. The proof plays the door's maker; beside
the proofs and the measures of this tree, no maker uses the door yet. A pass of the fifth shows the read path's pure
code on values of one process, the steps of a live read in every order of the
callbacks among them. A pass of the sixth shows the read path in one process:
facts that went in through the door and came back out through the read path,
once and kept live, to an asker that the proof plays. No other asker uses the
read path yet: no view, no screen, no tool and no agent. A pass of the seventh
shows the store run again in one process. A second module, which mirrors the
store's two depots and runs the store's own gates over them from their
beginning, holds on every task every row of every table that the first module
holds. And the kept acts of a fixture, fed to a fresh store with 4 tasks and to
one with 8, give the kept tables. It does not show that those rows are right:
they are what the store gave that the fixture was made of. The seventh also
holds a rule that names each act which was decided out of the order of its
layer's line, and applies it to every partition of both depots of each store
that it reads.

The
third shows failures that the proof makes: of an attempt of a batch while it
is processed, where the second attempt makes each row as the first
made it; and of an event of
the streaming gate, before its first write. The sixth shows such an attempt of
a batch under open reads. The seventh makes a throw in a batch of the base, a
stall of an event of a streaming layer past Rama's stream timeout, and a throw
in such an event, each with acts behind it, and holds that the store run again
after them gives the same rows. None shows a cluster of
several machines, a killed process, a failure of the machine or of Rama in the
middle of an event, a failure while a batch commits, a second module that takes
the place of the first, a run again while the first module still decides, a
second module with another number of tasks over the same depots, a number of
tasks other than 4 and 8, or how fast the
module, the door or the read path is.

Two more runs are no check:

```sh
clojure -M:store-module -m softland.store.read-measures
clojure -M:store-module -m softland.store.gate-measures
```

The first measures, on the same in-process cluster, a gate under 100 acts a
second,
the streaming gate again with live reads open, the opening of a canvas of 5,000 facts, and how
late a push is. The second measures an act through the door and each gate with
no live read open, on whichever tree it is run. Each prints each number on a
line that starts with `MEASURED`,
holds nothing to a number, and takes some minutes; with the argument
`small` it runs each shape with a small load. An in-process number is a floor
for a round trip and says little of a seek on a disk.

The [proofs' own map](softland/store/README.md) says what each check covers and
what it does not reach.
