# Store verification

[Store source entry](../src-store/README.md).

This tree owns checks of the store's code and what they print, not accepted
behavior.

| Immediate child | Responsibility |
|---|---|
| [softland/](softland/README.md) | The JVM checks: the act, the gates' pure namespaces and the door's, and the read path's pure namespaces, with no cluster; the store's module, the door and the read path, each on Rama's in-process cluster; and the measures of the read build, a run that is no check. |

Run the checks from the repository root:

```sh
clojure -M:store -m softland.store.act-proof
clojure -M:store -m softland.store.pure-proof
clojure -M:store-module -m softland.store.module-proof
clojure -M:store-module -m softland.store.door-proof
clojure -M:store -m softland.store.read-path-pure-proof
clojure -M:store-module -m softland.store.read-path-proof
```

The first two and the fifth need local Clojure and nothing else: the alias
`:store` puts Clojure alone on the classpath. They start no browser, connect to
no cluster and call no provider.

The third, the fourth and the sixth need Rama, which the alias `:store-module`
adds. Each starts Rama's in-process cluster inside its own JVM, with four tasks
and two workers, launches the module in it, and closes it at the end. The third
and the sixth each start two such clusters, one after the other: the second is
for the one group that makes a batch of the microbatch gate run a second time,
and for it alone the proof replaces three functions of the gate's pure code by
ones that give what the store's give. It connects to
nothing outside its process. The cluster keeps its files in a temporary
directory of Rama's.

Each prints what it observed on lines that start with `SHOWN`, prints those lines
again together at the end, and exits with a status that is not zero when a check
fails.

A pass of the first two shows the pure code on values of one process. A pass of
the third shows the module in one process: acts that went through each depot and
its gate, and were read back by act-id; and what each gate keeps for a read,
selected straight from its tables. The third plays the door by hand, so the
store stays proven without the door. It also plays a reader of the tables by
hand, so the gates stay proven without the read path. A pass of the fourth shows the door in one
process: acts that a maker gave it, which went through each depot and its gate,
and whose answers the door read back. The proof is the door's one maker; no
other maker uses the door yet. A pass of the fifth shows the read path's pure
code on values of one process, the steps of a live read in every order of the
callbacks among them. A pass of the sixth shows the read path in one process:
facts that went in through the door and came back out through the read path,
once and kept live, to an asker that the proof plays. No other asker uses the
read path yet: no view, no screen, no tool and no agent. The third shows one failure of a batch while it
is processed, made by the proof: the second attempt makes each row as the first
made it. None shows a cluster of several machines, a failure in the middle
of an event, a failure while a batch commits, or how fast the module, the
door or the read path is.

One more run is no check:

```sh
clojure -M:store-module -m softland.store.read-measures
```

It measures, on the same in-process cluster, a gate under 100 acts a second,
the same with live reads open, the opening of a canvas of 5,000 facts, and how
late a push is. It prints each number on a line that starts with `MEASURED`,
holds nothing to a number, and takes about ten minutes; with the argument
`small` it runs each shape with a small load. An in-process number is a floor
for a round trip and says little of a seek on a disk.

The [proofs' own map](softland/store/README.md) says what each check covers and
what it does not reach.
