# The command line — source entry

This tree holds the command line: Softland from a terminal, signed in as an
agent of a person (no program is a person), and the library by which any
Clojure program speaks to the
runtime. It is Clojure on the JVM. It shares the wire's forms with the runtime
as the same files, and it keeps a number of any width as itself.

It was built on 21 November 2026 as part of the spine of
[the runtime's page](../src/proposal/runtime-2026-11-21/PROPOSAL.md). That the
command line is Clojure is entry 10 of that page. It takes the place of the
Node program of 20 November ([src-cli](../src-cli/README.md)), which stays
where it is until Sid names where it goes.

The implementation is beneath [softland/](softland/README.md).

| Question | Enter here |
|---|---|
| Which commands are there, what does it keep, and how does it end? | The namespace docstring of [main.clj](softland/command/main.clj) |
| How does a program sign in, open the line, say a word and hold a watch? | The namespace docstring of [client.clj](softland/command/client.clj) |
| What can it say? | [The wire](../src-wire/README.md), `words.clj` |
| Where can I challenge a behavior with an existing check? | [The runtime's checks](../test-runtime/softland/runtime/README.md): `command_proof.clj` runs the commands as processes against a runtime, and `checks.clj` uses the library for everything that it says over the line |

Its classpath is the alias `:command` of the root `deps.edn`: `src-store`, for
the form of an act that the wire's check is written in, `src-wire`,
and `src-command`, with Clojure alone. Its checks need a runtime to speak to,
so they are with the runtime's. No Rama and no server is on
it: it speaks to the runtime and to nothing else.

Run it from the repository root, against a runtime that listens:

```sh
clojure -M:command -m softland.command.main sign-in
clojure -M:command -m softland.command.main write :chair-7 :colour :green
clojure -M:command -m softland.command.main read :chair-7 :colour
```

This tree keeps the store's rules for explanations
([src-store/CLAUDE.md](../src-store/CLAUDE.md)). It has no `CLAUDE.md` of its
own: a page that binds later sessions is Sid's to add.
