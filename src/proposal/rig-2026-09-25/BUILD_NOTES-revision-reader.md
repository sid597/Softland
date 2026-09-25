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

- 03:11. Read the plan whole, its validation whole, and the build phase's
  steps (`phase-3` to `phase-7`, both validation templates). Probed the git
  behaviours the read path maps, git 2.43.0 in this worktree: `HEAD@{99999}`
  exits 128 with an empty stderr (so its `:detail` is an empty string);
  `^HEAD` exits 0 printing `^` and an id; the range exits 1 with two lines;
  a missing directory and `/proc` exit 128 on both commands; ls-tree
  records as the plan gives them. `git show --no-textconv` is accepted, so
  the show oracle takes it too (insurance; no `.gitattributes` exists).
- 03:25. Implemented `src/rig/revision.clj` (namespace `rig.revision`) in the
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
