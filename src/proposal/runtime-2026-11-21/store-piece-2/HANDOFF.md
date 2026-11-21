# The store's line: carry-over to a new session

[The runtime's page](../PROPOSAL.md), "The store's line, to start from".

**21 November 2026, 23:10 IST.** Written by the Claude session
`build-proposal-store-line`, which built piece 1 and most of piece 2, when Sid
asked for a new session because this one had grown too long. It says what is
committed, what is in the tree and was never run, and what comes next. It is a
session's page: where it and the code differ, the code is right.

## Where the three pieces stand

| Piece | State |
|---|---|
| 1. A permission ends, is given again; root-only settings | **Built, proven, committed** (`2882b55b`). Its page: [BUILD-store-piece-1.md](../BUILD-store-piece-1.md). |
| 2. A permission for one run and for one act | **Code written and committed as work in progress. Its pure proofs pass. Its cluster proofs have never run.** The rest of this page. |
| 3. A layer's acts and refusals, read by stamp | **Specified, not built.** [SPEC.md](../store-piece-3/SPEC.md) and Phase 0's [IMPLICIT_SPEC.md](../store-piece-3/IMPLICIT_SPEC.md) are committed. Its plan starts when piece 2 has landed. |

## Piece 2: what exists

The form and the statements are in [SPEC.md](SPEC.md), written before the
code: a sixth slot of the permission id, `[:run session]` or
`[:act uuid digest]`; two more store keys; R1 to R10. Read it first.

The code, in the commit that holds this page:

- `src-store/softland/store/act.clj`: the sixth slot, `covers`, `allows` for
  six slots, the leaf `:digest-hex`.
- `door/declaration.clj`: the keys `:softland.store/run-permission` and
  `:softland.store/act-permission`, `permission-keys`, two rules.
- `door/digest.clj`: `hex-for-its-permission`.
- `gate/chain.clj`: `place` by what a permission covers.
- `gate/decision.clj`: `standing` takes six arguments and holds an act to what
  its permission covers; `permission-place` is the place of the permission
  that the act names; a permission for a run or an act gives nothing; a run
  ends only its own.
- `module/walk.clj`: one more argument to `standing`.
- `read_path/query.clj`, `layers.clj`, `sources.clj`, `answer.clj`: a query
  can name its `:session`, and "may see" reads that run's permission too.

## What was run, and what was not

Run on the tree of this commit, 21 November, 23:05 IST:

| Proof | Result |
|---|---|
| `clojure -M:store -m softland.store.act-proof` | 166 checks, none failed |
| `clojure -M:store -m softland.store.pure-proof` | 724, none failed |
| `clojure -M:store -m softland.store.read-path-pure-proof` | 223, none failed |

**Never run on piece 2's code:** `module-proof`, `door-proof`,
`read-path-proof`, `run-again-proof`. A sequence of them was started and the
session ended before it reported; its logs were under `/tmp` and are gone.
So these edits of the proofs have never been compiled, and small faults in
them are likely:

- `test-store/softland/store/module_proof.clj`: the groups
  `a-permission-for-a-run-and-for-an-act` and
  `a-permission-for-a-run-and-for-an-act-in-the-base`.
- `test-store/softland/store/read_path_proof.clj`: the group
  `a-read-asked-in-a-run`.
- `test-store/softland/store/run_again_proof.clj`: `a-permission-covers!` in
  the hand, `words-of-what-a-permission-covers`, and the sentence in
  `what-the-fixture-must-hold`.

The store's pages (`src-store/MODEL.md`, `softland/store/README.md`,
`read_path/README.md`, and entries 4 to 6 of "Decisions of 20 and 21
November" in `src-store/README.md`) already say that piece 2 is built. That
is ahead of the cluster proofs. If a cluster proof shows a rule to be wrong,
those pages change with the code.

## What is left of piece 2, in order

1. Run the four cluster proofs, each behind the machine's lock, and repair
   what they show:
   `flock src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock clojure -M:store-module -m softland.store.module-proof`
   and the same for `door-proof`, `read-path-proof`, `run-again-proof`. The
   run-again proof should pass on the fixture as it is: the kept acts hold no
   permission for a run or an act.
2. Docstrings that still say the old thing or leave piece 2 out:
   - `module/walk.clj`, "What one call costs": one SHA-256 more for an act
     under a permission for one act.
   - `gate/decision.clj`, the namespace docstring's list of checks: what a
     permission covers, and that one for a run or an act gives nothing.
   - `read_path.clj`, "What it follows" and "What it costs": the run's
     permission and the rows above it.
   - `record/rows.clj`, the namespace docstring, where it names the key of a
     permission's place.
   - `door/declaration.clj`, the docstring of `store-keys`: "the last three
     are of the read build" now stands before five keys.
   - `gate/README.md`, `door/README.md`: one line each for `place` by form
     and for `hex-for-its-permission`.
3. Make the fixture again, on purpose, for the new forms, and add the fifth
   making to `test-store/softland/store/run_again_fixture/README.md`, as the
   fourth is written there:
   `flock <the lock> clojure -M:store-module -m softland.store.run-again-proof make-fixture`
4. Run all seven proofs on the final code, one after the other.
5. Tell the session `softland-1f` that `gate/decision.clj` and
   `gate/chain.clj` have settled, and take its result. It keeps generated
   histories of the gate's rules against statements in plain code
   ([gate-histories/](../gate-histories/README.md)); it found R10 before any
   code was written. Its uncommitted edits in `gate-histories/` are its own:
   do not stage them.
6. Write `BUILD-store-piece-2.md` beside piece 1's page, in its form: what
   was built, a session's choices for Sid to turn, what it costs, what was
   run, what no run reached, what is beyond the ask. It must say that the
   fixture was made again, and why.
7. Commit by concern, your own paths by name.

## Then piece 3

By the Rama skill's phases, each in a fresh context
(`.claude/skills/rama/references/phases.md`): decompose, Phase 1 (the plan),
Phase 2 (its validation), build, full-spec review. Phase 0 is done. Its open
questions Q2, Q3 and Q6 are marked Sid's in `IMPLICIT_SPEC.md`; Q1 is
resolved in the spec. Piece 3 adds a store key: after piece 2 it is the
eighth, not the sixth as its spec counts.

## What Sid said in this session, in order

- "what? more questions for me? why is the work not in shape of
  implementation" (to a sheet of six calls). So: take the default, build it,
  mark it as a session's, and tell him so that he can turn it. Bring him a
  question only when a thing cannot be built as the pages say.
- A holder can have more than one permission in a layer: "bio-agent is not a
  single thing we can run 10k instances of the same thing unloke a human so
  each instance can have different permisiion" (‹95754ddf›).
- "3.yes" (‹95754ddf›): his decisions of 20 and 21 November may be written
  into `src-store/README.md`. Done (`84d3bd81`).
- Of act-id or exact act: "This is basically permissions question on product
  side why are you asking me this" (‹4eaf571e›). So the forms are a
  session's.
- "this session is too long we should start a new session".

Nothing is waiting on him. For him to turn when he wants: the choices listed
in [BUILD-store-piece-1.md](../BUILD-store-piece-1.md), the forms in
[SPEC.md](SPEC.md), and the reading in piece 3's spec that a taken act whole
is held to the same rule as a refused one.

## What this session learned the hard way

- **Nothing under `/tmp` survives a restart.** The logs of the last run were
  lost with it. Keep run logs outside the repo and outside `/tmp`, for
  example under `/mnt/data/projects/research/softland/store-line-2026-11-21/`.
- **One cluster at a time, behind the lock** named above. Another session
  builds the runtime in this tree and runs clusters too.
- **No edit of a `.clj` file while a sequence of proofs still has one to
  start**: each JVM loads the source when it starts. Pages can be edited.
- **The shell is zsh.** `${p%% *}` works; an unquoted variable is not split.
- **Other sessions write in this tree**: `deps.edn`, `src-runtime/`,
  `src-wire/`, `src-command/`, `test-runtime/`, `gate-histories/`, the
  runtime's page. Read `git log --oneline -3` and `git status --short`
  before each commit, and stage your own paths by name.
- **The proofs keep a second writing of each declaration**, as literals. A
  change of a shape, a rule, a word or a store key fails `act-proof` or
  `pure-proof` until the literal is changed too. That is on purpose.
- **A new gate rule's statements go to `softland-1f` before the rule is
  built.** It caught a hole that the builder's own proof would have passed.
