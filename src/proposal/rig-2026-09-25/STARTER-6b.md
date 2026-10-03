# Starter: step 6b, the micro gate's grammar, for Codex

Written 26 September 2026, about 08:40 IST, by builder C of the overnight
relay, at Sid's word ("yes"). Sid runs this step in Codex. The relay's
builders (A, then B, then C, then D) build everything else on the rig and
merge your branch when you are done.

---

## Where the rig stands

The rig is a Rama build of Softland's store: branch `rig-2026-09-25`,
folder `src/proposal/rig-2026-09-25/`. It is a candidate for the store
core, so build in the form it should be. Its records are not kept, and
nothing is kept until edition one, so nothing here is irreversible.

Built and landed on the branch: phases 0 to 5; phase 6's stream side
(grammar facts checked at the stream gate, tool facts, the minimal runner,
the revision reader); phase 8's replays, all 14 fixed histories playing as
the formal model says; and step R, the review's fixes (last code landing
`5bf802fc`). Phase 7's harnesses are being written. Left after you: the
relay merges your branch, then runs phase 7's minimum set, the three
numbers, which waits for your step because it measures the micro fold you
change.

## Your step

Sid's brief for phase 6: "grammar facts checked at the gate, tool facts, a
minimal runner that finds tools by matching ... Report the compiled steps
it needed in three classes: fixed-side steps the frame already promised,
fixed-side steps nobody anticipated, and capabilities. Call it the
machinery count. It is not the thesis count."

The stream side is built. Step 6b is the micro side: the micro gate's
grammar (the plan's a4 and b4, and F4's in-batch rule), then the machinery
count's micro side. Today the micro store keeps the compiled constant
(`grammar/grammars`, `reads/seed-hints`) and refuses grammar facts as data:
`micro/control-keys` lacks `:grammar`.

What 6b must build, and the question block 2b needs answered first, are in
`BUILD_NOTES-tools-and-grammars.md`, section "Phase 6b". In short: the
built micro store opens each sealed value twice on the arrival task (block
1's `arrival-open` and block 2b's `row-wraps`), while the grammar rows live
on `hash(L)`; how does block 2b get the pre-batch grammar, or the subjects
block 1 computed under it? The section gives three roads. Its author leaned
to road 1, carry the subjects and not the grammar, with the plan's M1
reorder, as a lean to test, not a decision. Choose a road, or another, and
say why in your notes.

Also yours:
- The places still reading the constant, as the wave 2 merge found them:
  RIG.md, For Sid 65. They include phase 5's shared-layer index writes,
  which take `seed-hints` and need the rows' hints at `hash(L)`, and phase
  4's landing, whose copy's subjects are "the target's grammar".
- W2-3 (`BUILD_NOTES-wave2.md`) already made the re-classed layer's stream
  era read `$$layers`, the "rows from `$$layers` on a miss" half of 6b's
  rule. Build the micro-rows-first half on it.
- Step R added one clause to `gate/refusal`: a setting key (`:kind :owner
  :class :lock-grain`) about another entity than its layer is refused
  `:malformed-control` at both gates, and both doors throw first
  (`client/refuse-misplaced!`). Keep it, and check that `:grammar` joining
  the micro gate's control keys fits it (`BUILD_NOTES-review-fixes.md`).
- Phase 8's guard P3-5 (`world-refusal` in `test/rig/replay_test.clj`):
  once `:grammar` is among the micro gate's control keys, the cases that
  write a `:mention` into the group (A2 and A4 to A7) fail on purpose, so
  6b cannot change them silently. Update the replay's seed so those cases
  again play as the model says, the way W2-2 made the seed write the
  stream side's grammar as facts (`BUILD_NOTES-replays.md`, "Pass 3, after
  wave 2").
- The machinery count's micro side, in the three classes, and the total.
  The stream side is 26 (10 promised, 6 unanticipated, 10 capabilities),
  plus 3 units the merge changed, listed apart (RIG.md, "The machinery
  count so far", which also gives the unit: one named code unit added to
  the fixed side or the vocabulary, or one existing unit whose behaviour
  changes).

## Read first

- The rama skill, `/mnt/data/projects/Softland/.claude/skills/rama/`
  (start with `SKILL.md`). Every phase goes through its steps: plan,
  validate the plan, build, validate the build, write the tests, validate
  the tests, notes, commit. Validation is by reading; see the test rule
  below for when anything runs.
- In the rig folder: `SPEC.md` (phase 6); RIG.md's "Overnight state" top
  block, "The machinery count so far" and For Sid 65;
  `BUILD_NOTES-tools-and-grammars.md` ("Phase 6b" and "The machinery
  count"); `BUILD_NOTES-wave2.md`; `BUILD_NOTES-review-fixes.md`; and, by
  section, the large `PLAN-micro-store.md` and `PLAN-tools-and-grammars.md`
  for a4, b4, F4, M1 and T-FR3.

## How to work

- Your own worktree, and only there:
  `git -C /mnt/data/projects/Softland-rig-2026-09-25 worktree add -b rig-build-grammar-micro /mnt/data/projects/Softland-rig-build-grammar-micro rig-2026-09-25`.
  The worktree `/mnt/data/projects/Softland-rig-2026-09-25` belongs to the
  relay's builder; don't work in it.
- **Sid's test rule** (26 September, about 08:20 IST: "We want to optimize
  for throughput of the system doing testing only when it is needed not
  after every ... step"). No test runs while you build: write the tests and
  validate them by reading. A load check that compiles your changed
  namespaces without starting a cluster is fine. When 6b is built, merge
  `rig-2026-09-25` into your branch once more, to take whatever landed
  meanwhile, and run the night's one full suite, once:
  `runs/wave2-suite-cmd.sh`'s namespaces plus `rig.store.review-fixes-test`
  and your new ones. Save its output to `runs/grammar-micro-suite.txt`.
  Fix what fails and rerun only the namespaces a fix touches. The relay
  then merges your branch without rerunning, because the merged code is
  your tested code.
- **The cluster lock.** Every in-process cluster run uses port 2002, so
  only one can run on this machine at a time. Prefix every run with
  `flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock`; a run
  without it breaks whichever run holds the port.
- Where no ruling settles something a kept record would carry (its bytes,
  a stamp, an id, what an entry holds), use the simplest placeholder, mark
  it first-record in your notes, and write the question for Sid. Anything
  else: the simplest thing that can change later without touching a
  record. Sid is running you, so a choice that is his can be asked.
- Use plain words in code and test names (erased, unreadable, restart).
  Write long files in parts and commit as you go.

## Rules (Sid's)

- Never read `src/app/server/env.clj`; nothing under `src/app` is read or
  touched. `test/rig/store/gate_test.clj` is Sid's: never open it.
- Commit on your branch only; never push; no Co-Authored-By or session
  lines. Stage with `git add <named files>`, never `-A` or `.`.
- Don't delete anything; list it in your notes instead. Avoid git reset,
  git checkout or restore of files, git stash, rm, and moving tracked
  files.
- Don't edit RIG.md; the relay folds your notes into it.

## When you are done

- `BUILD_NOTES-grammar-micro.md` in the rig folder: the receipt (commits,
  the suite's counts), the road you chose and why, what was built, the
  machinery count's micro side and the total, "Shared-file changes", and
  "For RIG.md" (rig choices, questions for Sid, first-record placeholders).
  Commit it.
- Append one line to `/mnt/data/projects/rig-relay-2026-09-26/log`:
  `<date and time> IST Codex: step 6b done at <commit> on rig-build-grammar-micro; suite <tests>/<assertions>, <failures> failures`.
  The relay's builder watches the log for it.
