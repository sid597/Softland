# The command-line program — source entry

This tree holds `softland`, the program by which a person or an agent's
harness speaks the words to Softland's server over HTTP (the words are
declared in `src-server/softland/server/words.clj`; the face that takes them
is `src-server/softland/server/http.clj`). Run as `softland mcp`, the same
program is an MCP server on stdio for Claude Code or Codex. It is
ClojureScript that shadow-cljs compiles into one script for Node; no JVM runs
it, and it uses no npm package, only Node's built-ins and ClojureScript's EDN
reader.

Build it from the repository root (alias `:cli` of `deps.edn`, build
`:softland-cli` of `shadow-cljs.edn`; the classpath is `src-cli` alone):

    clojure -M:cli -m shadow.cljs.devtools.cli release softland-cli

The script lands in `target/softland-cli/softland.js`; `bin/softland` runs it
from any folder. `bin/softland help` lists the commands. The server is
`--server <url>`, else `$SOFTLAND_SERVER`, else `http://localhost:8400`. The
credential that a sign-in saves is in `$SOFTLAND_CREDENTIALS`, else
`~/.config/softland/credentials.edn`, one entry for each server.

To use it from an agent's harness, register `bin/softland mcp` (with
`--server` when needed) as a stdio MCP server, after `bin/softland sign-in`.

`test-cli/sign-in-with-stand-in.sh` signs the program in with no person,
against a server that runs a stand-in for Google.

Start with the question you are carrying:

| Question | Enter here |
|---|---|
| What is in this tree, and what state does each part own? | [softland/cli/](softland/cli/README.md) |
| How does a command line become a word, and what does an exit status mean? | The namespace docstring of `softland/cli/pure.cljs` |
| How does a program sign in without a browser, and where is the credential kept? | The namespace docstring of `softland/cli/commands.cljs`, then `sign-in` in it; the server's side is the namespace docstring of `src-server/softland/server/http.clj` |
| Why does a wide number reach the server whole? | The namespace docstring of `softland/cli/commands.cljs`, "Data passes through as text"; `verbatim` in `pure.cljs` |
| How is a word posted, and a watch read line by line? | The namespace docstring of `softland/cli/node.cljs` |
| What tools does an agent see, and what do they give? | The namespace docstring of `softland/cli/mcp.cljs`, then `tools` in it |
