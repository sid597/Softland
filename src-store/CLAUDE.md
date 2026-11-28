# What is written beside the store's code

These lines are for the store tree: the source under `src-store/` and its checks
under `test-store/`. `AGENTS.md` points to this file, so Claude and Codex read the
same page.

They follow Sid's word of 25 November 2026. Code is the truth of what happens. A
check is the truth of what must hold. Prose beside the code stays only where it
is the one home of something; a second telling of the code is a copy that someone
has to keep true by hand.

## Where each kind of statement lives

- **What happens: the code.** Names and declared data say it. The act's form in
  `softland/store/act.clj` is the model: its shape is data, and its reasons are
  words inside it. A docstring does not retell a body, list a namespace's
  functions, or say who calls a thing.
- **What must hold: a check, with its sentence.** A run prints the sentence after
  `PASSED` or `FAILED`. A claim that no check holds is not written as if one did.
- **A why, a trap, a limit, what a run found: a short note at its one site.** What
  the code relies on and does not check goes there too. A namespace keeps a
  sentence or two on what the file is for.
- **What Sid decided: the "Decisions of …" sections of `README.md`.** His quoted
  lines stay as typed.

No map of the tree is kept beside the code. To find the way in, read the
namespaces, or ask a session to read them.

## When the code changes

- The check and its sentence change in the same commit.
- A note that the change makes false is changed or removed in the same commit. A
  note is not added to tell a body again.
- When what is intended and what the code does differ, say both. Do not present a
  proposal as built.

## Say what was traced and what was run

Keep these apart: durable state, the memory of one process, and a borrowed
handle; a request, and an outcome that a gate accepted; what a source file says,
what an existing check says, what a new run printed, and a claim about speed.
When you claim a retry, an ack or a transaction, say how far the claim reaches.
What a check observed belongs to the checks, under `test-store/`.

## Words

"Act" is the word of this tree, and it is Sid's. "Lane" is the store's unit: the
acts that one gate decides one after another. "Layer" is
Sid's word for a group of acts at any size; this tree does not use it for its
unit, and defines no meaning for it. "Key" is the key of a fact. "Lock" is an
encryption key. Never one word for both.

## The rig, Rama, and environment files

The rig (`src/proposal/rig-2026-09-25/`) is an example build. This tree takes no
form, no name and no call shape from its code; a finding of the rig is cited as a
finding. Load the repository's Rama skill before you read or explain Rama code.
Never read `env.clj` or a `.env` file. Refer to an environment symbol by its name
only.
