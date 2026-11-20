# Server verification

[Server source entry](../src-server/README.md).

This tree owns the checks of the server's code and what they print, not accepted
behavior.

| Immediate child | Responsibility |
|---|---|
| [softland/server/](softland/server/README.md) | The JVM checks: the server's pure parts and what its source may hold, with no cluster; the seam, and the face over HTTP, each on Rama's in-process cluster. |

Run the checks from the repository root:

```sh
clojure -M:server -m softland.server.pure-proof
clojure -M:server -m softland.server.seam-proof
clojure -M:server -m softland.server.http-proof
```

The first needs no cluster. The second and the third each start Rama's
in-process cluster inside their own JVM, with four tasks and two threads, launch
the store's module and the server's in it, and close it at the end. The third
also starts two Jetty servers on free ports of the loopback address. Each
connects to nothing outside its process: Google is stood in for by a key pair
of the proof's own.

Each prints the sentence of each check after `PASSED` or `FAILED`, with what it
saw for a failure, then the counts, and exits with a status that is not zero
when a check fails.
