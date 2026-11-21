# The store's line, piece 3: carry-over to a new session

[The runtime's page](../PROPOSAL.md), entry 9 and "The store's line, to start
from". [Piece 2's carry-over](../store-piece-2/HANDOFF.md) is the page before
this one.

**22 November 2026, 00:30 IST.** Written by the Claude session that finished
piece 2 and started piece 3 (`softland-18`), at its own position that a build
of three stages should not be judged from a context this long. It says what
is committed, what is planned and not checked, and what comes next. It is a
session's page: where it and the code differ, the code is right.

## Where the three pieces stand

| Piece | State |
|---|---|
| 1. A permission ends, is given again; root-only settings | Built, proven, committed (`2882b55b`). [BUILD-store-piece-1.md](../BUILD-store-piece-1.md). |
| 2. A permission for one run and for one act | **Built, proven, committed** (`34fdab0e`, `3544d026`, `f7202571`). [BUILD-store-piece-2.md](../BUILD-store-piece-2.md). Not reviewed by a fresh session. |
| 3. A layer's acts and refusals, read by stamp | **Phase 0, the decomposition and the first stage's plan are written. The plan is not checked. No code.** The rest of this page. |

## Piece 3: what exists

By the Rama skill's phases (`.claude/skills/rama/references/phases.md`), each
in a fresh context:

| Artifact | What it is | State |
|---|---|---|
| [SPEC.md](SPEC.md) | The spec. Its last section, "After piece 2, and the defaults taken", was added before any plan | Committed (`73e16b61`) |
| [IMPLICIT_SPEC.md](IMPLICIT_SPEC.md) | Phase 0. Where it follows the earlier reading of an asker's own things, SPEC.md governs | Committed |
| [DECOMPOSITION.json](DECOMPOSITION.json) | Three stages: `gates-keep-the-line`, `read-the-line-by-stamp`, `read-the-line-live` | Committed (`73e16b61`) |
| [PLAN-gates-keep-the-line.md](PLAN-gates-keep-the-line.md) | Phase 1 for the first stage, written on Fable in a fresh context | In the commit that holds this page. **Phase 2 has not run** |

**The plan in five lines.** For each decision a gate writes three rows more,
on the layer's task, with the answer, with no read: a small line row under
the layer and the stamp; the thing again, as the object that was decided, in
a table of its own, when it weighs 32 MiB or less; and a row by who. The
answers' table, the door's reads and the depots are not touched. The base's
three tables are on the base's task alone. The setting is the eighth store
key, a setting like the other three.

## What the session that asked for the plan made of it

It read the plan against the spec and against `walk.clj`, `tables.clj`,
`decision.clj` and `chain.clj`. This is that session's view, for the checker
to break and not to lean on.

- **It takes the plan's main choice.** Nothing kept twice was the road wanted
  (the gate keeps where the thing sits in its depot). Rama gives a topology no
  position of its record, a stream gate cannot count one across a retry, and a
  position differs between 4 tasks and 8. Two probes that did not know of each
  other agree that a PState value and a query's answer of 20 to 64 MiB work in
  process (`probes/sizes-output.txt`, from a stopped run;
  `probes-second/keeps_a_thing.out`, the plan's own). So the thing is kept
  again, and the store's bytes are held a second time, in RocksDB. That cost
  is real and is not measured.
- **What Phase 2 should press on, in its order of weight:**
  1. One thing of up to 32 MiB is serialised and written inside the walk, on
     the thread of a task that many layers share. A sender with no permission
     in the layer can send such things. Today her refused thing costs a
     digest's walk and two small writes. Whether a thing that a gate's check
     never reached should be kept again at that size, or under a smaller
     bound than a taken act, is not weighed in the plan.
  2. The rows by who cost one write for each decision, for ever, in every
     layer. The plan rests them on SPEC's "from a stamp on". A second ground
     that it does not give: they also serve Sid's second use of 12 October,
     "to see what a sender in bad faith does", as one run. The other road is
     that an asker outside a layer names what she sent.
  3. The plan says that IMPLICIT S4 cannot be met as written, by any place of
     the new rule. That is to be traced, not taken.
  4. The weight is a new measure of the store, beside the one text and the
     digest. Whether the one text's own count can serve, and what is then
     not kept, is the simpler road to hold it against.
- **A10 has no owner in reach.** The statements of the new rule go into the
  reference of the generated histories before the rule is built. That folder's
  session (`softland-1f`) could not be reached from this one, and its three
  files are still uncommitted in the working tree.
  [see_check.clj](../store-piece-2/see_check.clj) shows the way that changes
  none of its files.

## What is left, in order

1. **Phase 2 for the first stage**, in a fresh context, adversarial:
   `phase-2-plan-validate.md`, giving `PLAN_VALIDATION-gates-keep-the-line.md`.
   On a major fail, Phase 1 runs again with that page as input.
2. **The build of the first stage**, one long session: the walk, the three
   tables, the rows, the setting's rule, their proofs, the sixth making of the
   fixture, the pages of the tree. It changes `src-store` and `test-store`.
3. **The second stage**: its plan, its check, its build. The read itself, who
   is given what, the bounds in count and bytes.
4. **The third stage**: its plan says whether the read is kept live at all.
5. **The full-spec review**, always, in a fresh context.
6. `BUILD-store-piece-3.md` beside the other two, in their form, and commits
   by concern.

Also open, and not piece 3's: **a fresh session's review of piece 2 from its
diff** (`34fdab0e`, `3544d026`), as `build.md` asks of every build.

## The defaults taken for piece 3, each Sid's to turn

In [SPEC.md](SPEC.md), last section: who is inside a layer for this read (her
permission for the layer, or the run that she asks in; never a permission for
one act); the read gives the line of the layer's own gate alone; a refused
thing in a layer that is never made is read by the who that it names;
`:everyone` shows acts whole to whoever may see the layer, also one who is
not signed in when the layer is open to everyone; what an asker outside a
layer is given must cost what is hers. In the plan, "A session's choices":
nine more, the 32 MiB bound and the rows by who first among them.

## What Sid said in this session, in order

- The ask: "Finish piece 2 first, then build piece 3. I have answered what
  was waiting on me. Don't bring me more questions: where something is open,
  take the default, build it, mark it as yours, and tell me so I can turn it.
  Only come to me when a thing cannot be built as the pages say."
- "should you be doing the implementation or opus-max why or why not?"
- "is the subagent building or is it a different task?"
- "plan for what?"
- "which model-effort are you using there? and is that enough to write the
  plan why?"
- "so you are saying the piece 2 is all done from code pov as well? so why
  are we doieng the piece 3 work here?"

He gave no ruling on the model or on where piece 3 is built. The session's
answers to him: the plan of a stored record is a judgment and goes to Fable;
and piece 3's check, builds and review belong in a fresh session. Both are
that session's positions, and his to turn.

## What this session learned the hard way

- **A subagent here runs on Opus 5.5 unless a model is named.** Sid's user
  settings set `CLAUDE_CODE_SUBAGENT_MODEL=opus`. Name `fable` for a phase
  that is a judgment.
- **Two Opus plan agents were stopped by a safeguard**, category
  `reasoning_extraction`, each while it was only reading files (the spec the
  first time, `walk.clj` the second), on 21 and 22 November. The second ran
  on a brief with every word that asks for thinking taken out, so the brief's
  wording was not shown to be the cause. The Fable agent ran the same brief
  clean. After a stop the harness says not to do the agent's work in the
  asking session, and to pass the message to Sid.
- **Run logs and probes are kept** under
  `/mnt/data/projects/research/softland/store-line-2026-11-21/`: `runs/` for
  the proofs of piece 2, `probes/` and `probes-second/` for piece 3.
- **All seven proofs, one after the other, take about four minutes** behind
  the lock; the fixture's making takes 25 seconds.
  `flock src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock clojure -M:store-module -m softland.store.<proof>`,
  and `-M:store` for the three pure ones. Each in `bash -c`: the shell is zsh.
- **The generated histories turn a statement off without failing** when the
  code under them changes its arguments: read the line that a run prints
  before `HOLDS`, and count the mutants that were not caught.
- What piece 2's carry-over learned still holds: one cluster at a time, no
  edit of a `.clj` file while a sequence of proofs has one to start, other
  sessions write in this tree, and the proofs keep a second writing of each
  declaration as literals.
