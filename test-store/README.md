# Store verification

[Store source entry](../src-store/README.md).

This tree owns checks of the store's code and what they print, not accepted
behavior.

| Immediate child | Responsibility |
|---|---|
| [softland/](softland/README.md) | JVM checks of the act, with no cluster, and of the door, against a fresh in-process Rama cluster with the rig's module. |

Run the existing checks from the repository root:

```sh
clojure -M:store -m softland.store.act-proof
clojure -M:store -m softland.store.door-proof
```

Each needs local Clojure. The door's check also needs the project's Rama
dependency. Neither starts a browser, connects to a running cluster or calls a
provider. Each prints what it observed on lines that start with `SHOWN`, prints
those lines again together at the end, and exits with a status that is not zero
when a check fails.

A pass of the act's check shows the act's own code on values of one process. It
does not show an act that went through a depot, and it does not show the door or
a gate, because neither uses that code. A second command gives the same cases to
the rig's envelope and prints where the two differ; it asserts nothing.

A pass of the door's check shows the door's behavior against the rig's gates on
Rama's in-process cluster, in one JVM. It does not show a deployed cluster, other
machines or a real failure of a worker.

The [proofs' own map](softland/store/README.md) says what each check covers, how
to run the second command, and what the checks do not reach.
