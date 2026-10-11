# Store verification

[Store source entry](../src-store/README.md).

This tree owns checks of the store's code and what they print, not accepted
behavior.

| Immediate child | Responsibility |
|---|---|
| [softland/](softland/README.md) | The JVM checks: the act, the gates' pure namespaces and the door's, with no cluster; and the store's module and the door, each on Rama's in-process cluster. |

Run the checks from the repository root:

```sh
clojure -M:store -m softland.store.act-proof
clojure -M:store -m softland.store.pure-proof
clojure -M:store-module -m softland.store.module-proof
clojure -M:store-module -m softland.store.door-proof
```

The first two need local Clojure and nothing else: the alias `:store` puts
Clojure alone on the classpath. They start no browser, connect to no cluster and
call no provider.

The third and the fourth need Rama, which the alias `:store-module` adds. Each
starts Rama's in-process cluster inside its own JVM, with four tasks and two
workers, launches the module in it, and closes it at the end. It connects to
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
hand: no reader of facts is built in this tree yet. A pass of the fourth shows the door in one
process: acts that a maker gave it, which went through each depot and its gate,
and whose answers the door read back. The proof is the door's one maker; no
other maker uses the door yet. Neither shows a cluster of several machines, a
failure in the middle of an event or of a batch, or how fast the module or the
door is.

The [proofs' own map](softland/store/README.md) says what each check covers and
what it does not reach.
