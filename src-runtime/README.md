# The runtime — source entry

This tree holds the runtime: the one program of ours that stands between
everything that connects and Rama. It does what the store cannot do: it turns a
credential into a who, and it holds each act against the one who sends it. An
act goes on to the store's door, a read to the store's read path, and what
sign-in keeps to Auth. Nothing true is kept in the runtime: many copies of it
run at once and share nothing but Rama.

It was built on 21 November 2026, at Sid's go, as the spine of
[the runtime's page](../src/proposal/runtime-2026-11-21/PROPOSAL.md) ("The
spine, to start from"). His decisions that it stands on are entries 1, 2, 5, 7,
8 and 12 of that page, and his go to the sheet for sign-in. What was built,
each fork that the build took, and what was run are on
[the spine's page](../src/proposal/runtime-2026-11-21/spine/BUILD.md). It takes
the place of the server of 20 November ([src-server](../src-server/README.md)),
which stays where it is until Sid names where it goes.

The implementation is beneath [softland/](softland/README.md).

| Question | Enter here |
|---|---|
| What is in this tree, and what state does each part own? | [softland/runtime/](softland/runtime/README.md) |
| How does a credential become a caller, and how is an act held against its sender? | The namespace docstring of [seam.clj](softland/runtime/seam.clj) |
| What ends a connection when a sign-in ends at another copy? | [seam.clj](softland/runtime/seam.clj), `standing`; then [standing.clj](softland/runtime/standing.clj) |
| What crosses one connection, and in which order are words done? | The namespace docstring of [line.clj](softland/runtime/line.clj) |
| What can a system say? | [The wire](../src-wire/README.md), `words.clj` |
| How does a person sign in, and a program? | The namespace docstring of [http.clj](softland/runtime/http.clj) |
| What does a first sign-in write, where, and what holds when it breaks between Auth and the store? | The namespace docstring of [sign_in.clj](softland/runtime/sign_in.clj) |
| How is a deployment born? | [sign_in.clj](softland/runtime/sign_in.clj), `setup!`; then [main.clj](softland/runtime/main.clj) |
| What does Auth keep, and why is it not in the store? | [Auth](../src-auth/README.md) |
| How do I run it? | The namespace docstring of [main.clj](softland/runtime/main.clj) |
| Where can I challenge a behavior with an existing check? | [Verification map](../test-runtime/README.md) |

Its classpath is the alias `:runtime` of the root `deps.edn`: `src-store`, for
the door and the read path; `src-auth`, for Auth's module and its client;
`src-wire`; `src-runtime`; and the checks. Clojure, Rama, Missionary, Ring's
core with its Jetty adapter, and `data.json` are its dependencies. The root
`src`, the rig, the old server, the server of 20 November and Inland are not on
it, and neither is Electric.

Run it from the repository root, on a store that is thrown away:

```sh
clojure -M:runtime -m softland.runtime.main serve --stand-in-google --first-person-email you@example.com
```

It starts Rama's in-process cluster with the store's module and Auth, runs the
setup for that first person, and listens on `http://localhost:8500`. With the
stand-in for Google, the sign-in form takes any account; the one that is
invited is the first person's email.

This tree keeps the store's rules for explanations
([src-store/CLAUDE.md](../src-store/CLAUDE.md)). It has no `CLAUDE.md` of its
own: a page that binds later sessions is Sid's to add.
