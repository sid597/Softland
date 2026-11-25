# softland/cli — the files of the program

| File | Responsibility | State |
|---|---|---|
| `main.cljs` | The entry: reads the command line into a plan and hands it to the MCP server or to the commands; sets the exit status. | None. |
| `pure.cljs` | Everything that touches nothing: the plan of a command line, EDN read and printed, the person's text carried as it is, the token's text, the short code and the link of a sign-in, the act of `write`, and what a result means for the exit status. | None. |
| `node.cljs` | The touch of Node, kept thin: a word posted with `fetch`, the stream of `:watch` read line by line, the credentials file written with modes 0700 and 0600, standard input and output, random bytes and SHA-256. | None of its own; its effects are on the network, the file system and the process. |
| `commands.cljs` | The commands, and the steps that the MCP tools share with them: say a word with the saved credential, an act sent under an act-id that is told first, a fact written into her own lane, a watch collected for a while, the sign-in and the sign-out. | None of its own; the credential is read from the credentials file at each step. |
| `mcp.cljs` | The MCP server on stdio: JSON-RPC 2.0, one object per line, the tools and their descriptions, each tool a step of `commands.cljs`. | The context, fixed at the start: the server's URL and the credentials file's path. |
