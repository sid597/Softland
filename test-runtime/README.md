# Runtime verification

[Runtime source entry](../src-runtime/README.md).

This tree owns the checks of the runtime's code and what they print, not
accepted behavior.

| Immediate child | Responsibility |
|---|---|
| [softland/runtime/](softland/runtime/README.md) | The runtime's pure part and what its source may hold, with no cluster; the runtime as a system outside it meets it, on Rama's in-process cluster; and the same again on a cluster that stays up, with processes killed. |

Run them from the repository root:

```sh
clojure -M:runtime -m softland.runtime.pure-proof
clojure -M:runtime -m softland.runtime.proof
clojure -M:runtime -m softland.runtime.command-proof
clojure -M:runtime -m softland.runtime.cluster-proof --conductor localhost --conductor-port 1983
```

The first needs no cluster. The second starts Rama's in-process cluster inside
its own JVM, with the store's module and Auth, and three copies of the runtime
on free ports of the loopback address. The third needs a Rama cluster that is
up, with the two modules launched, and that holds nothing that is kept; how
the proof cluster of 21 November was set up is on
[the spine's page](../src/proposal/runtime-2026-11-21/spine/BUILD.md).

Each prints the sentence of each check after `PASSED` or `FAILED`, with what it
saw for a failure, then the counts, and exits with a status that is not zero
when a check fails.
