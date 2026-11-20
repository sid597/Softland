# The browser's face — source entry

This tree holds the browser's face of the server: one small Electric program
whose server half calls the seam (`softland.server.seam`) directly, and the
host that serves it in front of the face over HTTP. It is a first slice, the
proof that a browser tab can sign in, keep a live read open, and send an act,
through the same seam that the face over HTTP stands on. It is plain DOM, with
no styling effort: a working slice, not a product screen.

The implementation is beneath [softland/browser/](softland/browser/README.md).

Start with the question you are carrying:

| Question | Enter here |
|---|---|
| What is in this tree, and what state does each part own? | [softland/browser/](softland/browser/README.md) |
| Where is the actor kept from the wire, and how does an act from the browser stay one act when the socket opens again? | The namespace docstring of [app.cljc](softland/browser/app.cljc) |
| Which socket is refused, and what is served beside the program? | The namespace docstring of [host.clj](softland/browser/host.clj) |
| How do I run it? | The namespace docstring of [main.clj](softland/browser/main.clj), and below |
| What does the seam give, and what ends a session on every face? | [The server](../src-server/README.md), then the namespace docstring of `seam.clj` there |
| How is it checked with a real browser? | [test-server-browser/](../test-server-browser/README.md) |

## Classpath

This tree has its own classpath, the alias `:server-browser` of the root
`deps.edn`: `src-store`, `src-server`, `src-server-browser` and `test-server` as
`:replace-paths`; Clojure, ClojureScript, Rama, Missionary, Ring's core and its
Jetty adapter, `data.json`, Electric `v3-alpha-20260519.115706-45` and
shadow-cljs as `:replace-deps`. It is a whole alias and not an addition to
`:server`, so that `:server` stays the guard that the seam loads with no
Electric. The browser's build is `:softland-browser` in `shadow-cljs.edn`.

## Build and run

From the repository root, build the client's half, then start the server:

```sh
clojure -M:server-browser -m shadow.cljs.devtools.cli release softland-browser
clojure -M:server-browser -m softland.browser.main --stand-in-google --port 8401
```

The first build of Electric `-45` on a machine stops and asks for a login: it
prints "Please login with any email to activate" with a link, and waits. What
the login gives is kept under `.shadow-cljs/builds/softland-browser/`, which
git ignores, and later builds do not ask. On 20 November 2026 this build was
run with the file that the build `:inland` of this machine already held there,
copied and not read. Electric's terms are in
[THIRD-PARTY.md](../THIRD-PARTY.md).

The build writes `target/softland-browser/public/js/main.js`. The server starts
Rama's in-process cluster, with a store that is thrown away when it ends, and
listens on `http://localhost:8401`: sign in on `/`, then open `/app`. The
first command must be run again after a change to `app.cljc` or `boot.cljs`,
and the server started again after a change to a `.clj` or `.cljc` file:
both peers must run the same program.

This tree keeps the store's rules for explanations
([src-store/CLAUDE.md](../src-store/CLAUDE.md)): a README for each folder, a
docstring for each namespace and each function, and an explanation that
changes in the same commit as its code.
