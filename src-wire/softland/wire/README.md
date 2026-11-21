# Wire — the forms that cross

[Parent: softland](../README.md) · [Wire source entry](../../README.md).

Everything here is pure, owns no state, and is required from both sides of the
line: by the runtime ([src-runtime](../../../src-runtime/README.md)) and by the
command line ([src-command](../../../src-command/README.md)).

| File | Responsibility |
|---|---|
| [words.clj](words.clj) | The words that a system can say over the line, declared as data, with their check; the bounds on a frame's bytes; and the runtime's own words for what it could not do. |
| [credential.clj](credential.clj) | A credential's text and its three parts, a secret and its hash, a code for a program's sign-in that is under way, and how a credential is read from a header and from a cookie. |
| [frame.clj](frame.clj) | One frame: one EDN form, read with a bound on its longest token, and written on one line. |
| [watching.clj](watching.clj) | What waits for a reader who is slower than the changes, merged by place; and an answer after a change. |

`words.clj` requires `credential.clj`, for the form of a hash, and the store's
`softland.store.act`, whose engine reads its shapes. `frame.clj` requires one
number of the store's declaration. Nothing here requires the runtime, the
command line, Auth or Rama.

## Words a session made

"The line", "a word", "a frame", "a carrier", "a code", "an ask". `:could-not`
for what the runtime says in place of a result is carried from the server of
20 November. Each is Sid's to change.
