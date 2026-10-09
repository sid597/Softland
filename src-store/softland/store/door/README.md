# Door — what arrives on a depot

[Parent: store](../README.md) · [Store source entry](../../../README.md).

This folder holds what the door and the gates must agree on: the form of a thing
as it arrives on a depot, how a value is sealed to its place, and how a thing is
given one text and one digest. The door itself is not built; the
[module's proof](../../../../test-store/softland/store/README.md) plays it by
hand. A gate checks each thing that arrives against this folder's declaration,
and the gate is the authority: anything can append to a depot without the door.

All of it is pure code and loads with Clojure alone.

| File | Responsibility | State and lifetime |
|---|---|---|
| [declaration.clj](declaration.clj) | The door's declaration: what arrives on a depot, as shapes and rules in `act.clj`'s language; the store's namespace and the store keys; the limits; the test for a stray; the check. | None. |
| [seal.clj](seal.clj) | Locks and the seal: a lock's bytes, sealing a value to its place, opening it. | One random source for each process. |
| [text.clj](text.clj) | The one text of a thing: one function from anything to a string, the same in every process. | None. |
| [digest.clj](digest.clj) | The digest of a thing, SHA-256 over its one text. The door and the gates share it. | None. |

## How the files relate

[text.clj](text.clj) is at the bottom. It requires only [the act](../act.clj),
for what a value is. [digest.clj](digest.clj) takes the digest of a thing over
that text, and [seal.clj](seal.clj) binds a sealed value to the text of its
place. [declaration.clj](declaration.clj) declares what arrives, and reads its
shapes with the engine of [act.clj](../act.clj); it requires none of the other
three.

## Boundaries

Nothing here reads the record. Whether a permission stands, whether a replaced
fact is current and whether a lock opens a sealed value need rows of the record:
those checks are [gate/](../gate/README.md)'s. Nothing here writes a lock
anywhere: a gate writes a lock's row, in [module/](../module/README.md).
