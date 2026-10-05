# Store verification

[Store source entry](../src-store/README.md).

This tree owns checks of the store's code and what they print, not accepted
behavior.

| Immediate child | Responsibility |
|---|---|
| [softland/](softland/README.md) | JVM checks of the door against a fresh in-process Rama cluster with the rig's module. |

Run the existing check from the repository root:

```sh
clojure -M:store -m softland.store.door-proof
```

It needs local Clojure and the project's Rama dependency. It starts no browser,
connects to no running cluster and calls no provider. It prints each thing that
it observed on a line that starts with `SHOWN`, prints those lines again together
at the end, and exits with a status that is not zero when a check fails.

A pass shows the door's behavior against the rig's gates on Rama's in-process
cluster, in one JVM. It does not show a deployed cluster, other machines or a
real failure of a worker. The [suite's own map](softland/store/README.md) says
what each scene checks and what the suite does not reach.
