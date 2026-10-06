# Store verification

[Store source entry](../src-store/README.md).

This tree owns checks of the store's code and what they print, not accepted
behavior.

| Immediate child | Responsibility |
|---|---|
| [softland/](softland/README.md) | The JVM check of the act, with no cluster. |

Run the check from the repository root:

```sh
clojure -M:store -m softland.store.act-proof
```

It needs local Clojure and nothing else: the alias puts Clojure alone on the
classpath. It does not start a browser, connect to a cluster or call a provider.
It prints what it observed on lines that start with `SHOWN`, prints those lines
again together at the end, and exits with a status that is not zero when a check
fails.

A pass shows the act's own code on values of one process. It does not show an
act that went through a depot, a door or a gate: none of them is built.

The [proof's own map](softland/store/README.md) says what the check covers and
what it does not reach.
