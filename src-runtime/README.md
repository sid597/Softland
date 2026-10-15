# The runtime — source entry

This tree holds the runtime: the code that stands in front of the store's door
and read path and makes the names on acts and reads true. Today the door takes
`:who`, `:session` and `:permission` as a maker gives them, and the read path
asks as the caller says (`src-store/softland/store/door.clj`,
`src-store/softland/store/read_path.clj`). Sign-in is the way into the runtime
for every actor: a person at a browser, an agent, a program, and the system.
The implementation lives beneath [softland/](softland/README.md).

What is here is the runtime's pure part (BUILD.md of
`src/proposal/sign-in-2026-10-15/`, Part 1, "who is acting"): the ids, the
records it writes into the store, tokens, the check of Google's ID token, the
binding of an actor to her acts and reads, and the runtime's start and sign-in
as steps that are safe to run again, with the runtime's other work as the
system (a name changed, an agent made, a token's session, a run). None of it
opens a cluster, reads a clock or takes a random source: a caller hands in
fresh ids, random bytes and the time.

The runtime on a cluster (Part 2) is the service: it opens the store's door and
read path on the module `Store`, drives those steps with them, binds each
actor's act and read, holds them to the system's blocks and limits, and keeps
in its process's memory only what the namespace docstring of
[service.clj](softland/runtime/service.clj) lists.

Beside them is the way in (Parts 2 and 3): the HTTP server on 127.0.0.1, the
one page with Google's button, Sid's command line and its admin token, and the
main entry that starts it all on Rama's in-process cluster, on a store that is
thrown away when the process ends. Run it from the repository root:

```sh
clojure -M:runtime-module -m softland.runtime.main --google-client-id <id>
clojure -M:runtime-module -m softland.runtime.command invite --base see-and-act
```

The rig (`src/proposal/rig-2026-09-25/`), the old server (`src/app/server/`) and
Inland (`src-inland/`) are examples, not sources: no namespace of this tree
requires them, and no form, name or call shape comes from their code.

Start with the question you are carrying:

| Question | Enter here |
|---|---|
| What is in this tree, and what state does each part own? | [softland/](softland/README.md), then [runtime/](softland/runtime/README.md) |
| What does the runtime write into the store, in which layer, under which keys? | The namespace docstring of [records.clj](softland/runtime/records.clj), then `records`; the system's layers are named in [ids.clj](softland/runtime/ids.clj) |
| How does a first sign-in make a person, and why does a crash not make two? | The namespace docstring of [sign_in.clj](softland/runtime/sign_in.clj), then of [steps.clj](softland/runtime/steps.clj) for what "safe to run again" rests on |
| What does the runtime make at its start? | The namespace docstring of [start.clj](softland/runtime/start.clj) |
| What calls does the runtime offer, and what does each give? What does it keep in memory? | The namespace docstring of [service.clj](softland/runtime/service.clj) |
| How are a name changed, an agent made, a token's session, a run and a blocked attempt written? | The namespace docstring of [system_steps.clj](softland/runtime/system_steps.clj) |
| How is Google's ID token checked? | The namespace docstring of [google.clj](softland/runtime/google.clj) |
| Why is an actor's act or read refused before the door? How do blocks and limits read? | The namespace docstring of [binding.clj](softland/runtime/binding.clj); how they are counted and recorded, of [service.clj](softland/runtime/service.clj) |
| What is a token, and what does the store keep of it? | The namespace docstring of [token.clj](softland/runtime/token.clj) |
| How do I run the runtime, and what does it start and throw away? | The namespace docstring of [main.clj](softland/runtime/main.clj) |
| Which HTTP routes are there, who may call each, and how is a browser's POST checked? | The namespace docstring of [http.clj](softland/runtime/http.clj) |
| What does the page show, and how does Google's button post its credential? | The namespace docstring of [page.clj](softland/runtime/page.clj) |
| How does Sid invite, link, block, unblock and set a limit? Where is the admin token? | The namespace docstrings of [command.clj](softland/runtime/command.clj) and [admin.clj](softland/runtime/admin.clj) |
| Where can I challenge a behavior with a check? | [Verification map](../test-runtime/README.md) |

The rules for keeping these explanations are in [CLAUDE.md](CLAUDE.md).

## Classpath

The tree has its own classpath, in two aliases of the root `deps.edn`. Both
hold `src-store`, `src-runtime` and `test-runtime` as `:replace-paths`, so the
root `src` and `resources`, the rig, Inland and the root's other dependencies
are on neither, and a require of any of them from here fails at load.

- `:runtime` holds Clojure and Nimbus (`com.nimbusds/nimbus-jose-jwt` 10.10),
  for the check of Google's ID token. The pure code and its proof run on it. It
  is the guard that the pure code needs no Rama: a namespace that came to
  require Rama would fail to load here.
- `:runtime-module` holds Clojure, Rama (`com.rpl/rama` 1.6.0) and Nimbus, for
  the runtime on a cluster.

Both carry the store's JVM options. The runtime requires the store's pure
namespaces for the forms it makes and reads: the act
(`softland.store.act`), and the store's ids and the depot a layer starts with
(`softland.store.gate.first-act`); and, for a blocked attempt kept whole or as
a digest, the store's bound on a value, its text and its digest
(`softland.store.door.declaration`, `.text`, `.digest`). The service requires
the door (`softland.store.door`) and the read path
(`softland.store.read-path`), and so loads only on `:runtime-module`.
