# Keep explanations with the store code

These rules are for the store tree: the source under `src-store/` and its checks
under `test-store/`. They are the rules that the server and the client keep
([server](../src/app/server/CLAUDE.md), [client](../src/app/client/CLAUDE.md)),
said for this tree. `AGENTS.md` points to this file, so Claude and Codex use the
same rules.

## Read by level

Enter through the [store source entry](README.md). Then read the README of the
folder, the docstring of the namespace, and the docstring of the function. A
parent map tells the role of a part. The child tells how the part does its work.
Read only the branch that the question needs. Follow a caller or a dependency
when the relation crosses a folder or leaves the tree.

## Each level has one home

- The `README.md` of the tree: what the tree is for, where to enter by question,
  and its classpath.
- The `README.md` of each folder: the responsibility of the folder, its
  boundaries, the state that it owns or borrows, and how its immediate files and
  folders relate. Each folder has one, with a link to its parent.
- The docstring of each namespace: the role of the file, what it takes, what it
  gives or changes, the state that it owns or borrows, and its approach.
- The docstring of each function, a private function too: what it takes, what it
  gives or changes, what must hold before the call, and its limits. A reason that
  is not obvious stays with its function. A small function needs a small
  docstring.

Keep a map at its level. A README holds no list of functions, no copy of source,
and no contract of a file two levels down. It links to the child, and the child
says it. There is no second tree of documents beside the code. An outside caller
or dependency goes at the boundary where it explains this code. An overview of
another build, and a part that is not built, stay out of these maps.

## Change the explanation with the code

An explanation changes in the same commit as the code that it explains.

- When the contract of a function or its reason changes, change its docstring. A
  change inside a function does not always change the responsibility of its
  folder.
- When a responsibility, a flow of data or an ownership changes, change that
  scope and its parent map. Go further up only when the larger responsibility
  changes too.
- A decision at a higher level shows in the contracts and the code below it.
  Examine both directions. Does the code do what its explanation says? Does the
  explanation still explain the code?
- When what is intended and what the code does are different, say both. Do not
  write the intended contract again around a bug. Do not present a proposal as
  built. An explanation does not permit a code change that nobody asked for.
- Add, move and remove an explanation with its code. Repair the links to and from
  its parent. Replace old wording; do not add a history of corrections.

## Say what was traced and what was run

Trace the real callers and the real lifetime before you describe a path as in
use. Keep these apart:

- durable state, the memory of one process, and a handle that is borrowed;
- a request, and an outcome that a gate accepted;
- the taking of a thing, and its release;
- what a source file says, what an existing check says, what a new run printed,
  and a claim about speed.

When you claim a retry, an ack or a transaction, say how far the claim reaches.

A source description is not a run. What a check observed belongs to the
checks, under `test-store/`, and not to a page of the source.

## The checks explain themselves

The levels above, a README for each folder and a docstring for each namespace
and function, are for the source. A check is not explained a second time on a
page: its sentence, in the test file, says what it checks, and a run prints
that sentence after `PASSED` or `FAILED`. A page that retells each check must
change with every check, and when it does not, it says that a thing is checked
that is not.

The page of a folder of checks says, for each test file: what it checks, in a
line or two; how to run it; what a pass shows; what state it changes outside
the code that it checks; and what it does not reach, with ideas for checks that
are not written yet. What no check reaches is the one thing that a test file
cannot say of itself, and it is where the next check comes from.

## Words

"Act" is the word of this tree, and it is Sid's. The rig's code says "offer" for
the same thing. A name that this tree makes uses Sid's word.

"Key" is the key of a fact. "Lock" is an encryption key. Never one word for both.

## The rig, Rama, and environment files

The rig (`src/proposal/rig-2026-09-25/`) is an example build. This tree takes no
form, no name and no call shape from its code. Where a finding of the rig on Rama
forced a choice here, such as refusals as data, the seal before the depot, or the
stamp, cite the finding, never the code. No namespace of this tree requires the
rig.

Load the repository's Rama skill before you read or explain Rama code. Never read
`env.clj` or a `.env` file. Refer to an environment symbol by its name only.

## What lasts

Prefer a constraint and its reason to a dated inventory, a count, or a guess of
what a run gives. When you write an explanation again, keep its useful detail and
move it to its proper home.
