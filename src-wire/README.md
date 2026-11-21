# The wire — source entry

This tree holds the forms that the runtime and whatever connects to it share:
the words, one frame, a credential's text, and what waits for a slow reader.
It is pure code. The runtime requires it to read what arrives, and the command
line requires the same files to write it, so the two cannot drift apart.

It was built on 21 November 2026 as part of the spine of
[the runtime's page](../src/proposal/runtime-2026-11-21/PROPOSAL.md). That the
whole act goes on the wire is Sid's decision (entry 7 of that page). The words
themselves, the form of a credential and each number here are a session's, and
his to turn. Two of its files, `frame.clj` and `watching.clj`, came over from
the server of 20 November with their names changed and nothing else.

The implementation is beneath [softland/](softland/README.md).

| Question | Enter here |
|---|---|
| What can a system say to the runtime, and what does the runtime say when it could not do a thing? | The namespace docstring of [words.clj](softland/wire/words.clj), then its declarations |
| What is a credential, and how does the runtime find what it stands for? | The namespace docstring of [credential.clj](softland/wire/credential.clj) |
| What bounds one frame before it is read? | The namespace docstring of [frame.clj](softland/wire/frame.clj) |
| What is a slow reader handed? | The namespace docstring of [watching.clj](softland/wire/watching.clj) |
| Where can I challenge a behavior with an existing check? | [Verification map](../test-wire/README.md) |

Its classpath is the alias `:wire` of the root `deps.edn`: `src-store`, for the
form of an act that the words' check is written in, `src-wire` and `test-wire`,
with Clojure alone. Rama is not on it, so nothing here can open a cluster; the
runtime's pure proof also reads the source for that.

This tree keeps the store's rules for explanations
([src-store/CLAUDE.md](../src-store/CLAUDE.md)). It has no `CLAUDE.md` of its
own: a page that binds later sessions is Sid's to add.
