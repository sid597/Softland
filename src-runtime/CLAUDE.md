# Keep explanations with the runtime code

These rules are for the runtime tree: the source under `src-runtime/` and its
checks under `test-runtime/`. They are the store tree's rules
([src-store/CLAUDE.md](../src-store/CLAUDE.md)), said for this tree; where this
page is silent, that page holds. `AGENTS.md` points to this file, so Claude and
Codex use the same rules.

## Read by level

Enter through the [runtime source entry](README.md). Then read the README of
the folder, the docstring of the namespace, and the docstring of the function.
Read only the branch that the question needs. Follow a caller or a dependency
when it crosses a folder or leaves the tree: the store's forms are explained
in `src-store/`, at the namespace that holds them.

## Each level has one home

- The `README.md` of the tree: what the tree is for, where to enter by
  question, and its classpath.
- The `README.md` of each folder: its responsibility, its boundaries, the state
  it owns or borrows, and how its files relate, with a link to its parent.
- The docstring of each namespace: its role, what it takes and gives, the state
  it owns or borrows, and its approach.
- The docstring of each function, a private one too: what it takes, what it
  gives, what must hold before the call, and its limits.

A README holds no list of functions and no copy of source. There is no second
tree of documents beside the code. A part that is not built stays out of these
maps.

## Change the explanation with the code

An explanation changes in the same commit as the code it explains. When what is
intended and what the code does differ, say both. A record, a key, a word or a
step that the plan of a build named and the code does differently is said at the
code, with why.

## What is decided, and by whom

The forms here were chosen by sessions, from Sid's decisions and the defaults
he did not strike (`src/proposal/sign-in-2026-10-15/BUILD.md`). Say which is
which at the code: cite his decision where one holds, and call a session's
choice a session's. A word that this tree makes is his to change.

## Pure, and what is handed in

The runtime's pure namespaces read no clock and take no random source: a caller
hands in fresh ids, random bytes and the time. A function that needs one of
them takes it as an argument and says so. A namespace that calls a cluster, the
network or a clock is not pure and says so in its docstring.

## The checks explain themselves

A check's sentence is in its test file, and a run prints it after `PASSED` or
`FAILED`. The page of a folder of checks says, for each test file, what it
checks, how to run it, what a pass shows, what state it changes outside the
code, and what it does not reach.

## Words, examples, environment files

"Act" is Sid's word. "Key" is the key of a fact; "lock" is an encryption key. A
token's secret is a secret, never a key.

The rig, the old server and Inland are examples: take no form, name, call shape
or default from their code, and require nothing of them. Never read `env.clj`
or a `.env` file. Load the repository's Rama skill before you read or explain
Rama code.
