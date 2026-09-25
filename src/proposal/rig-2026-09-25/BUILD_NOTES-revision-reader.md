# Build notes — the revision reader (rig phase 6, a capability)

Running log of the build of `PLAN-revision-reader.md` (validated minor-fail,
fixes F1 to F14 in place; `PLAN_VALIDATION-revision-reader.md`). Built
26 September 2026 (IST) by a fresh session, Claude Opus 5.5
(`claude-opus-5-5`), max effort, following the shape of the rama skill's
`references/phase-build.md`. Branch `rig-build-reader`, worktree
`/mnt/data/projects/Softland-rig-build-reader`, cut at `d0eea904`.

The Rama-specific parts of the build phase (module compile, PState and
topology checks, IPC launches, depot synchronization) are skipped: the
reader is plain Clojure with no Rama in it (plan section 9).

## Log

Times are IST: the first is the clock at the start, the rest are the times
of the commits that carry each step's work (`git log`), corrected after the
fact because the first draft of this log carried guessed times.

- 03:11. Read the plan whole, its validation whole, and the build phase's
  steps (`phase-3` to `phase-7`, both validation templates). Probed the git
  behaviours the read path maps, git 2.43.0 in this worktree: `HEAD@{99999}`
  exits 128 with an empty stderr (so its `:detail` is an empty string);
  `^HEAD` exits 0 printing `^` and an id; the range exits 1 with two lines;
  a missing directory and `/proc` exit 128 on both commands; ls-tree
  records as the plan gives them. `git show --no-textconv` is accepted, so
  the show oracle takes it too (insurance; no `.gitattributes` exists).
- 03:14. Implemented `src/rig/revision.clj` (namespace `rig.revision`) in the
  plan's order: the line index, the block cut, the form scanner (a lexer and
  an iterative driver), `span`, step 0, the git steps, the entries. Loads
  with `*warn-on-reflection*` on and no reflection or boxing warning. Lint:
  clj-kondo is not installed; `clojure-lsp diagnostics` (which embeds it) on
  a copy in this session's scratchpad reports only "unused public var" for
  the two entries, which nothing calls tonight. First smoke run reproduces
  every golden number in the plan: the README's 24 blocks and their six
  char positions, and section 7's table (23, 28, 51, 11 and 1 forms, first
  and last lines). 786 lines against the plan's assumed 300: the docstrings
  on every function; the code is about half.
- Readings the plan left open, taken in its words (none changes a golden
  number): "whitespace" in the block rules is `Character/isWhitespace`, the
  definition F9 gave the scanner, while indentation counts only spaces and
  tabs; the paragraph rule compares an item's indentation with the
  paragraph's first line, as rule 4's first sentence says of any open block,
  so a bullet indented deeper than the paragraph stays in it; "the number
  1" is the marker's integer value; an unclosed regex's `:at` is its opening
  quote, one char after the `#`; `##` is an atom with the token chars after
  it; `cut-for` returns `:blocks` for a non-string and has no catch, being
  total by construction (every other public function wraps its body).
- 03:22. Phase 4, `IMPLEMENTATION_VALIDATION-revision-reader.md`:
  **minor-fail**, one finding, fixed in place. IV1: `cut-for` lacked the
  catch the plan puts on every public function (section 5); the last
  reading above is withdrawn, and its body is wrapped now. Also, not a
  finding: `git-env` takes the environment as an argument, so the F5 filter
  can be tested on a literal map. Evidence gathered for it: every error row
  ran against real git; F5 ran end to end (a JVM started under
  `GIT_DIR`/`GIT_WORK_TREE` pointing at main: `/proc` is still
  `:not-a-repository`, `HEAD` is still this worktree's); a sweep of both
  cuts over every tracked file outside `src/app` at this branch's head:
  1,580 Markdown files, 126,747 blocks, no invariant failing; 345 Clojure
  files, 3,327 forms, every count and line equal to Clojure's reader; the
  refusals were two symlinks, the 6.3 MB transcript, and three Markdown
  files git's numstat also calls binary.
- 03:27. Phase 5, `test/rig/revision_test.clj` (namespace
  `rig.revision-test`): the plan's tests 1 to 25, one deftest or defspec
  each, named `t01` to `t25`; the fixture map first, then the oracles, then
  `through`, the helper every result passes (no `:internal`, test 20's
  walk). Hand-checking expected values against the rules before any run
  caught three of my own mistakes: a unit's chars written `[5 12]` for
  `[5 11]`; a headings case separated by blank lines, which could not tell
  a heading from paragraph text (now each sits under a paragraph line); and
  a case running `sh` as git, removed as shell-dependent. Loads clean, 25
  test vars.
- 03:31. Phase 6, `TEST_VALIDATION-revision-reader.md`: **minor-fail**,
  three missing cases, fixed inside `t09` and `t18` as phase 7's pre-loop:
  TV1, F5 checked at the process (`env` run through `run-git`); TV2, the
  `:git-failed` exits of steps 1, 2 and 4; TV3, F14 checked at the entries
  (a non-ASCII path under the JVM's own encoding).
- 03:33. Phase 7, first run: 25 tests, 2,829 assertions, **2 failures**,
  0 errors, both in `t07`: the form holding `(defn refusal` is `[101 148]`
  at `45627e45` and `[125 182]` at `ea52c424`, where plan test 7 says
  `[101 114]` and `[125 141]`. **The plan's numbers are wrong, not the
  code:** they are the extents of the *blocks* holding that line (a blank
  line follows the first `cond` clause, at 115 and at 142), not of the
  forms; read with `:cut :blocks` the reader gives exactly `[101 114]` and
  `[125 141]`. Clojure's reader, the plan's own oracle, ends the forms at
  148 and 182, and `t05` had already passed on every form of both
  files (23 and 28). The plan validation checked only the start lines (101 and 125),
  and `perm-entity` has no blank line, so its block and form agree and
  the slip never showed. Fixed in the test, not the code: `t07` asserts
  the form extents, checks them against the reader, and asserts the plan's
  numbers as the block extents they are. The plan is left as it stands
  (the orchestrator's to correct, if wanted).
- 03:33. Second run: 25 tests, 2,837 assertions, 0 failures, 0 errors,
  1.4 s wall. Two receipts beyond the plan's run, same result each: the
  suite under `GIT_DIR=/mnt/data/projects/Softland/.git` and
  `GIT_WORK_TREE=/mnt/data/projects/Softland` (F5 end to end: the reader
  and the oracles still read this worktree; `t18`'s child sees none of the
  three `GIT_` variables the JVM has), and under `LC_ALL=C`, where the JVM's
  `sun.jnu.encoding` is `ANSI_X3.4-1968` and `t09` takes F14's refusal
  branch through the entries.
- 03:33. The receipt run, `runs/revision-reader-tests.txt`: the plan's
  command from the rig folder, 25 tests, 2,837 assertions, 0 failures,
  0 errors, exit 0, about 1.4 s, at `87438510` (no code has changed since).

## For the orchestrator

- **Built.** `rig.revision` (`src/rig/revision.clj`) and its tests
  `rig.revision-test` (`test/rig/revision_test.clj`). Nothing else in the
  rig requires either; `deps.edn` is unchanged.
- **The count (plan section 12).** One capability, class three, anticipated
  on 13 September (integration.md's forced first step): two built-in steps
  a tool can call, `read-units` (its `:cut` a pick) and `read-span`. It
  touches no envelope part, gate rule, key grammar, store namespace or Rama
  code, so it is no compiled step of the line; whether a class-three entry
  counts against "zero new compiled steps" is left to the count's own
  definition (O21, F11).
- **For Sid, first-record once a kept fact carries them.** G7, the unit
  positions are counted in: in use, lines 1-based and inclusive, chars
  0-based and end-exclusive in UTF-16 code units of the text decoded from
  UTF-8. G15, a cut's name as the name of its rules: in use, `:blocks` and
  `:forms` mean tonight's rules and may change freely until a kept fact
  records a cut's name; from then a change is a new name.
- **A plan number to correct, if wanted.** Plan test 7 (section 11) gives
  the refusal form's extents as `[101 114]` and `[125 141]`; they are the
  block extents; the forms are `[101 148]` and `[125 182]`. The test
  asserts both, each as what it is; the plan is untouched.
- **Nothing to delete.** Every scratch file (probes, the lint copy and its
  caches, the sweep) is in this session's scratchpad, outside the worktree.
