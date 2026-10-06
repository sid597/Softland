# Store — the act

[Parent: namespace root](../README.md) · [Store source entry](../../README.md).

This folder holds the act: the one form of what a maker gives to the store, and
the check of that form. It is pure code and owns no state. It holds no door and
no gate.

| File | Responsibility | State and lifetime |
|---|---|---|
| [act.clj](act.clj) | The declared shape of an act, the rules that a shape cannot say, the check that reads both, the names, the stand-in sealed form with which a maker examines an act, and the digest. Pure code. | One constant: the secret of the digest, which is a throwaway. No handle and no memory. |

## The act

The form of an act is declared, not implied. [act.clj](act.clj) holds two
declarations as plain data, and they are the place to start:

- the shape: the parts of an act, what each part holds, which parts must be
  there, and the reason word when a part is wrong; and the same for a fact, a
  name, a fact id and a permission id;
- the rules: what must hold between two places of an act, which a shape cannot
  say.

The check holds no knowledge of the form. It reads the two declarations: first the
shape, part by part in the order declared, then the rules in their order. So the
order of the declarations is the order of the reasons.

Code is in three places only, and each is small: the leaves, which are the few
shapes that a declaration cannot say in data (an id, a value, bytes); the engine
that reads a shape; and one function for each rule.

The shape is compiled with the store. It is not a fact in the store, because each
shape that a tool gives enters the store as a fact inside an act; the act's own
shape must hold when each fact is wrong.
