# The command-line program — verification

[Source entry](../src-cli/README.md).

This tree holds what helps to run the command-line program against a server
with no person. It holds no proof: nothing here says `PASSED` or `FAILED`, and
no command of the program is held by a check that a later build can run.

| File | What it does | State that it changes |
|---|---|---|
| [sign-in-with-stand-in.sh](sign-in-with-stand-in.sh) | Signs the program in against a server that runs a stand-in for Google: it starts `softland sign-in`, reads the hash out of the link that the program shows, signs a browser in through the stand-in with `curl`, and says yes, or no, for it. It prints what the program printed and exits with its status. | The credentials file that `$SOFTLAND_CREDENTIALS` names, and a session on the server. It writes its own files into the work folder that it is given. |

Run it from the repository root, while a server runs with `--stand-in-google`,
after the program is built:

```sh
SOFTLAND_CREDENTIALS=/some/folder/credentials.edn sh test-cli/sign-in-with-stand-in.sh /some/work-folder yes
```

Give it a credentials file of its own: without `SOFTLAND_CREDENTIALS` it writes
the one in `~/.config/softland/`.

## What was run by hand, and what no check reaches

On 20 November 2026 the built program was run against the server on Rama's
in-process cluster: a sign-in with her yes and with her no; `who`; a `write`
and its `read`; an integer of 30 digits read back digit for digit; a `watch`
that showed a change as it was written; an act that names a who, and a token
over the server's bound, each refused; the MCP round (`initialize`,
`tools/list`, `write`, `read`, `watch`); and `sign-out` followed by a
visitor's `who`. The commands and what they printed are in the commit that
added the tree.

None of that is a check. Where the next ones come from:

- every command and every MCP tool, as a script that says what must hold and
  exits with a status;
- a harness of an agent, Claude Code or Codex, that starts `softland mcp`;
- the ten minutes after which a sign-in gives up, and a server that goes away
  while the program waits;
- `send-all` through `say`; a line of a stream, or a body, that is no EDN.
