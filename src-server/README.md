# The server — source entry

This tree holds the server: the one program of ours that stands between every
system that connects and Rama. It does what the store cannot do. It turns a
credential into a who, and it is where a sender would be paced before she
reaches a log that is never trimmed. An act goes on to the store's door and a
read to the store's read path. Nothing true is kept in the server: what must
last is in Rama.

Nothing in this tree is a decision of Sid's. It was built on 20 November 2026
from [the proposal](../src/proposal/server-2026-11-20/PROPOSAL.md), at his word
to build it while he was away. Each fork that the build took, and what stays
his call, is on [the build's page](../src/proposal/server-2026-11-20/BUILD.md).
What sign-in writes into the store is on
[its own page](../src/proposal/server-2026-11-20/SIGN-IN-WRITES.md), for his
redline. "The server" is the proposal's word for this program; his decisions
of 11 and 12 October say "the runtime" for it, and which word stays is his.

The implementation is beneath [softland/](softland/README.md).

Start with the question you are carrying:

| Question | Enter here |
|---|---|
| What is in this tree, and what state does each part own? | [softland/server/](softland/server/README.md) |
| How does a credential become a who, and what ends a session on every face? | The namespace docstring of [seam.clj](softland/server/seam.clj) |
| What can a system outside our build say to the server? | The namespace docstring of [words.clj](softland/server/words.clj), then its declarations |
| What does a request look like on the wire, and what is a stream? | The namespace docstring of [http.clj](softland/server/http.clj) |
| What does the server keep in Rama, and why is it not in the store? | The namespace docstring of [module.clj](softland/server/module.clj), then [session/](softland/server/session/README.md) |
| What does sign-in write into the store? | The namespace docstring of [sign_in.clj](softland/server/sign_in.clj) |
| How is Google's token checked? | The namespace docstring of [google.clj](softland/server/google.clj) |
| How do I run it? | The namespace docstring of [main.clj](softland/server/main.clj) |
| Which choices are the builder's, and which words did a session make? | [softland/server/](softland/server/README.md): "Engineering choices", "Words a session made" |
| Where can I challenge a behavior with an existing check? | [Verification map](../test-server/README.md) |

Two other trees stand on this one, and neither is required by it:
[the command-line program](../src-cli/README.md), which speaks the words over
HTTP and is also the MCP adapter, and
[the browser's face](../src-server-browser/README.md), an Electric program
whose server half calls the seam.

This tree has its own classpath, the alias `:server` of the root `deps.edn`. It
holds `src-store`, for the door and the read path, `src-server` and
`test-server` as `:replace-paths`; and Clojure, Rama, Missionary, Ring's core
with its Jetty adapter, and `data.json` as `:replace-deps`. So the root `src`,
the rig, the old server and Inland are not on it, and neither is Electric: a
require of any of them from here fails at load. That is the guard that the
seam and the words are called by every face, and the pure proof also reads the
source for it.

Run the program from the repository root:

```sh
clojure -M:server -m softland.server.main --stand-in-google
```

It starts Rama's in-process cluster with the store's module and the server's,
on a store that is thrown away when the program ends, and listens on
`http://localhost:8400`.

This tree keeps the store's rules for explanations
([src-store/CLAUDE.md](../src-store/CLAUDE.md)): a README for each folder, a
docstring for each namespace and each function, and an explanation that
changes in the same commit as its code. It has no `CLAUDE.md` of its own: a
page that binds later sessions is Sid's to add.
