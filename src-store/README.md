# The store — source entry

This tree holds the code of the store that is built to stay. The implementation
lives beneath [softland/](softland/README.md). It has two parts. The act is the
one form of what a maker gives to the store, and the check of that form. The door
is the part of the runtime that puts each act in its envelope and appends the act
to an acts depot. The door does not use this tree's act yet: it takes the act from
the rig, as the gates do.

The tree owns no durable state. The record is in a Rama module, and that module,
with its gates, depots and reads, is the rig's (`rig.store.module/Store`, under
`src/proposal/rig-2026-09-25/`). The door reaches it as a client, by the module's
name. The [store folder's map](softland/store/README.md) says where that boundary
is.

Start with the question you are carrying:

| Question | Enter here |
|---|---|
| What is in this tree, and what state does each part own? | [softland/](softland/README.md), then [store/](softland/store/README.md) |
| What is an act, and why is a thing refused as one? | The namespace docstring of [act.clj](softland/store/act.clj), then the docstring of its check |
| What does one call of the door take and give, and what does it not handle? | The namespace docstring of [door.clj](softland/store/door.clj), then that function's docstring |
| Where can I challenge a behavior with an existing check? | [Verification map](../test-store/README.md) |

"Act" is the word in this tree. The rig's code says "offer" for the same thing.

This tree has its own classpath. The alias `:store` in the root `deps.edn` holds
`src-store`, `test-store` and the rig's source as `:replace-paths`, so the root
`src` and `resources` are not on it and a require of app code from here fails at
load.

Follow folder map → namespace docstring → function docstring, and read only the
branch that the question needs. A folder map explains its immediate children.
What a function takes, gives and does not handle is in its docstring. The
[rules for explanations](CLAUDE.md) of this tree govern this hierarchy: update a
function's docstring and the affected parent map in the same change as the code.
A source description is not a run; what a check observed belongs to the
verification tree.
