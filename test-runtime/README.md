# Runtime verification

[Runtime source entry](../src-runtime/README.md).

This tree owns the checks of the runtime's code and what they print, not
accepted behavior.

| Immediate child | Responsibility |
|---|---|
| [softland/](softland/README.md) | The JVM checks of the runtime: its pure part, with no cluster and no network; the service, and its way in over HTTP, each on an in-process cluster |

Run the checks from the repository root:

```sh
clojure -M:runtime -m softland.runtime.pure-proof
flock src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock \
  clojure -M:runtime-module -m softland.runtime.service-proof
flock src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock \
  clojure -M:runtime-module -m softland.runtime.http-proof
```

The pure proof needs local Clojure and the Nimbus library, which the alias
`:runtime` puts on the classpath with the store's and the runtime's source and
nothing else of the root. It starts no browser, connects to no cluster, calls
no provider and fetches no key from Google: the proof makes its own key pair
and signs its own tokens.

The service proof needs Rama too (the alias `:runtime-module`). It starts Rama's
in-process cluster under the lock that the store's cluster proofs share, and a
stand-in for Google of its own; it opens no port and calls no network.

The HTTP proof needs Rama too (the alias `:runtime-module`). It starts Rama's
in-process cluster under the lock that the store's cluster proofs share, a
server on a free port of 127.0.0.1, and a stand-in for Google of its own; it
calls no network.
