# Command — the library and the program

[Parent: softland](../README.md) · [Command line source entry](../../README.md).

| File | Responsibility | State and lifetime |
|---|---|---|
| [client.clj](client.clj) | The library: a program's sign-in, the line, a word and its result, a watch. It requires the JDK's HTTP client and the wire's forms. | A connection, in the memory of the process: the socket, the results that are waited for, the watches. |
| [main.clj](main.clj) | The program: the commands, the one file that it keeps, and its exit status. | The file `sign-in.edn`; for `send`, a file of the acts that were sent and their UUIDs, beside the file that is sent. |

`main.clj` requires `client.clj`. Neither requires Rama, the runtime or Auth.

## Engineering choices

Each is a session's, kept and not decided.

- **One sitting is one session.** The commands of one sign-in share a session
  until `new-session`.
- **It keeps the credential and the session's id, and nothing else.** Who it is
  and where the person's own lane is come from the runtime's hello each time.
  Beside the file it keeps an empty `sign-in.edn.lock`, which a command holds
  while it changes the file, so that two commands at once change it one after
  the other.
- **A refusal ends with status 3,** a gate's of an act or her no to a
  sign-in, so that a script can tell it from a command that could not be
  done, which ends with 1.
- **With no `--permission`, an act names the permission that her yes lent
  this sign-in,** whose id the program makes from its root and its sign-in
  (`softland.wire.credential/lent-at-yes`): the command line is an agent of
  hers, and that lend is what it holds in her own lane. A sign-in that her
  yes lent nothing, an agent of hers signed in again with no box ticked or
  one that `agent new` made, names what it holds with `--permission`.
- **`give` to a person is refused by a gate today.** Passing a permission to
  a person needs `:give`; what her yes lends holds none, and no page lends
  it. A `give` to an agent or a run lends beneath what the program holds.
- **`send` keeps the UUIDs of a file's acts before it sends any,** so that a
  run that was cut off is sent again as the same acts.
- **The JVM stops at its first compiler** (`-XX:TieredStopAtLevel=1`, on the
  alias): a command is a short run.
