# Test validation — the revision reader

Phase 6 of the rama skill's build, inside the build session (Claude Opus
5.5, `claude-opus-5-5`, max effort, 26 September 2026 IST), following
`references/phase-6-test-validate.md` and the template
`references/artifact-test-validation.md`. The contract the tests are held
to: `PLAN-revision-reader.md` section 11 (tests 1 to 25) and its error
table (section 3), the public docstrings of `src/rig/revision.clj`, and the
two implicit-spec entries the plan cites, `IMPLICIT_SPEC.md` OP17 and OP18
(read tonight, lines 890 to 915). Test source:
`test/rig/revision_test.clj`, 910 lines, one deftest or defspec per plan
test, named `t01` to `t25`; line numbers ("L n") cite it as committed with
this artifact. Written before the suite's first run, as phase 6 requires.

Default verdict at the start: major-fail. Three findings, each a missing
case inside an existing deftest, were found; their fixes are applied in the
test source (phase 7's pre-loop, done here) and cited below.

## Findings, and their fixes

- **TV1. F5 was tested at the filter, not at the process.** `t18` checked
  `git-env` on a literal map, but nothing checked that `run-git` hands the
  child the filtered copy. Added to `t18` (L 625 to 631): `env` is run
  through the private `run-git`, and no `GIT_` name may appear in the
  child's environment while `PATH` does. This session's environment holds
  `GIT_EDITOR`, so the case is not vacuous here; phase 7 also runs the
  suite under a hook-like `GIT_DIR` and `GIT_WORK_TREE`.
- **TV2. Three `:git-failed` branches had no test.** The error table's
  `:git-failed` row covers "exited in a way the step does not expect", and
  only the executable that cannot start was tested. Added to `t18` (L 614
  to 621): step 2 on a commit the store lacks and step 4 on a blob it lacks
  (git exits 128 on each), and step 1 when the program exits 2 (`ls` given
  `rev-parse`'s arguments refuses `--verify`).
- **TV3. F14 was tested at the check, not at the entries.** `t09` called
  `input-error` with an encoding name passed in; nothing checked that the
  entries pass the JVM's own `sun.jnu.encoding`. Added to `t09` (L 508 to
  512): a read of the non-ASCII path `é.md` is `:missing-path` when the
  JVM's encoding is UTF-8 (the path reached git) and `:bad-argument` with
  that `:encoding` otherwise; phase 7 runs the suite under `LC_ALL=C` too,
  so both branches run.

## Minimize IPC launches

Not applicable: the suite starts no cluster and loads no Rama namespace.
Traced: its requires are `clojure.java.io`, `clojure.java.shell`,
`clojure.string`, `clojure.test`, test.check and `rig.revision` (L 14 to
21), and `rig.revision` requires only `clojure.java.shell` and
`clojure.string`. `rig.test-runner` requires the namespaces named on its
command line and nothing else, so the untracked partial test in the main
rig worktree is never loaded; no test names its path. **Pass.**

## Synchronization

Not applicable: the reader writes nothing, so no read waits on a write.
The only moving input is `HEAD`, which `t08` resolves once with `git
rev-parse` and compares against the read by that id, in one deftest
(L 431 to 453). **Pass.**

## Test namespaces compile

Loaded with `*warn-on-reflection*` on and without running (an inline alias
with the `:test` alias's test path and test.check): 25 test vars, no
reflection warning; the one warning printed is test.check 1.1.0's own
`abs` under Clojure 1.12. No private namespace is required; private
**vars** of `rig.revision` are reached by `#'` exactly where the plan says
tests reach them (`read-text`, the ls-tree record parser, the decode step)
and for the three fixes above (`input-error`, `git-env`, `run-git`,
`find-entry`, `read-blob`, `resolve-commit`), plus `internal` in `t19`.
**Pass.**

## Implicit spec coverage

The plan's tests, each walked against the deftest that carries it. "Plan"
quotes section 11's test.

**A. Faithful to git.**

- **1.** "the private `read-text` gives git show's bytes decoded, `:commit`
  equal to rev-parse's, and a line count equal to git's newline count", for
  every text fixture: `t01` (L 270 to 280) over all eight, `git show
  --no-textconv` against `read-text` by its var; rev-parse without
  `--verify`, a different invocation from the reader's; the line count
  taken from the public `span`'s `:line-count` against the count of `0x0A`
  bytes, after asserting each fixture ends in a newline. **Pass.**
- **2.** "(subs text start end) is :content; for units of whole lines,
  :content is git show's lines first to last joined by "\n", and start is
  the sum of the earlier lines' lengths ... plus one per newline"; the
  README's [3 6] at char 34 and byte 36, [56 60] at 2597 and 2601: `t02`
  (L 286 to 326) over every unit of the eight fixtures as blocks and the
  five Clojure fixtures as forms, each unit's first line against this
  namespace's own line reading, forms' last lines likewise, four spans per
  fixture, the two char-and-byte pairs, and one `read-span` through git.
  **Pass.**
- **3.** Exactly 24 blocks with the six positions: `t03` (L 334 to 345),
  the whole golden list by equality and the six `:chars`. **Pass.**
- **4.** The block invariants over every text fixture: `t04` (L 347 to
  351) with `block-invariant-failures` (L 171 to 206), an independent
  statement of section 6's invariants over `line-spans` (L 142 to 160),
  which splits on `"\n"` and is not the reader's index. **Pass.**

**B. Faithful to Clojure's reader.**

- **5.** Form count, first lines against `:line` where the reader gives
  one, last lines against the reader's line number after the read, equal to
  both the reader's run and section 7's table; each unit reads alone as one
  datum then the end; the text outside the units reads as nothing: `t05`
  (L 368 to 395) over the five fixtures, with the plan's bindings
  (L 100 to 116). A unit that wrongly took the comment above it would still
  read as one datum, so the start is pinned twice more: by `:line` for
  every top-level list, and for `deps.edn`'s one map by the line of its
  first brace. **Pass.**

**C. Two revisions.**

- **6.** `RIG.md` at `e53bc6d0` and `7a7403bd`: `t06` (L 397 to 412):
  `:commit` differs, `:path` agrees, the heading is a one-line block at
  line 29 and at line 209, found by content, both reads keep the
  invariants, and the only keys are the contract's. **Pass.**
- **7.** `gate.clj`'s `perm-entity` the same at [17 20] and [21 24],
  `refusal` different at [101 114] and [125 141], `intake` only in the
  second: `t07` (L 414 to 429), each by exact line pairs and a count of one.
  **Pass.**
- **8.** `"HEAD"` gives rev-parse's commit and equals the read by that id
  but for `:rev`; two reads of one commit and path are equal: `t08`
  (L 431 to 453), for `read-units` and `read-span`, plus a short id and a
  `repo` that is a directory inside the repository. This is OP18's replay
  condition for the reader: a read by a commit is a function of it.
  **Pass.**

**D. Every error path** (the error table's rows, with their keys).

- **9.** `t09` (L 455 to 512): every value the plan lists (rev `""`, `nil`,
  `"-p"`, `"--output=x"`, `"a\nb"`; path `""`, `"/etc/hosts"`, `"../x"`,
  `"a/./b"`, `"a//b"`, the rig folder's `"src/"`; repo `""`; `:limit` 0 and
  -1; `:cut :nope`; spans [0 1] and [5 4]; [1 97] on the 96-line README
  with `:line-count 96` and `:commit`; rev `:HEAD`, path `42`, repo `nil`,
  opts `[]`, span lines `[nil 1]` and `[1.5 2]`, `(blocks nil)`), each
  asserting `:argument` and `:value`, the inputs as given, and no
  `:commit` for step 0's refusals; plus `\r` and NUL, blank repos,
  `nil` opts and `:limit`s of the wrong type, and F14 (TV3 above).
  `--output=x` shows the refusal is step 0's: past `--end-of-options` git
  would have answered `:unknown-revision`. **Pass.**
- **10.** `t10` (L 514 to 519): the missing directory and `/proc`, each
  `:not-a-repository` with git's stderr, and afterwards the missing
  directory still does not exist. **Pass.**
- **11.** `t11` (L 521 to 531): the three the plan names, `^HEAD` and the
  range, none with `:detail` or `:commit`; `HEAD@{99999}` with `:detail`.
  **Pass.**
- **12.** `t12` (L 533 to 540): `RIG.md` at `ce6ebaeb` with `:commit`,
  `RIG.md/x` below a file, and `:(top)AGENTS.md`. **Pass.**
- **13.** `t13` (L 542 to 554): `:directory` and `:symlink` through git;
  `:submodule` through the record parser with the plan's record; the parser
  on two regular files and on an exact-match miss. **Pass.**
- **14.** `t14` (L 556 to 561): `:limit 1000` gives `:size 26033 :limit
  1000`; 26,032 is too large; 26,033 reads. **Pass.**
- **15.** `t15` (L 563 to 566): the PNG is `:binary`, and git's numstat
  says `- -` for it and `96 0` for the README. **Pass.**
- **16.** `t16` (L 568 to 581): Latin-1 `café` is `:not-utf-8`, not
  `:binary`; also an overlong encoding, a NUL at byte 7,999 (binary) and at
  byte 8,000 (text), a byte order mark kept, a three-byte char. **Pass.**
- **17.** `t17` (L 583 to 605): the eleven strings the plan lists with the
  exact `{:error :unreadable :reason :at}`, four more, and one through a
  read (the README cut as forms), with the read's keys. **Pass.**
- **18.** `t18` (L 607 to 632): `read-text` with `/nonexistent/git`;
  `{:git ...}` in opts reads normally (F4); TV1 and TV2 above. **Pass.**
- **19.** `through` (L 243 to 250), which every result in the suite passes
  through, asserts no `:internal`; the properties assert no `:error` at
  all; `t19` (L 633 to 649) adds inputs chosen to strain the code
  (100,000 unclosed openers, 20,000 nested vectors, 2,000 items with
  fences, stray `\r`s, a `:limit` beyond a long), checks `internal` itself,
  and `cut-for`'s cases. **Pass.**

**E. No identity.**

- **20.** `identity-free?` (L 230 to 241) walks every key at any depth,
  requires exactly `:content` and `:position` in a unit and exactly `:lines`
  and `:chars` in a position, two non-negative integers each, `:commit`
  only at the top; `through` applies it to every successful result. `t20`
  (L 651 to 687) asserts the exact top-level keys of each of the seven
  result kinds and a 40-hex `:commit`, and that the walk refuses fourteen
  results with an id, hash, sha, index, name, kind, extra map, nested
  `:commit`, a three-entry `:lines`, a negative or non-integer char, or a
  missing `:content`. **Pass.**

**F. Rules on literal strings.**

- **21.** `t21` (L 689 to 733). Every rule the plan lists: a fence with
  blank and `# ` lines; an unclosed fence; the indented and unindented
  continuations; `2026.` in a paragraph and `1.` under one; `7b.` after
  item 7; a nested item; a heading under a paragraph; CRLF (blank lines end
  blocks, the inner `\r\n` kept, no unit ends in `\r`, exact chars); a tab
  continuation; the byte order mark. Also: tilde fences, `1)` against `3)`,
  any number under an item, a deeper bullet in a paragraph, a heading in an
  item, every heading edge (three spaces, four, seven `#`, `#tag`, a bare
  `#`, a tab), a lazy continuation, fences inside and under items, a fence
  ending a paragraph, `* * *` and `***`, empty and blank texts. Each case
  also runs the invariants. **Pass.**
- **22.** `t22` (L 735 to 781). Every construct the plan lists, by exact
  contents: two forms on a line (and touching), a comment holding `)` and
  `"` outside and inside a form, all eight character literals inside a
  form and at the top, the string and the regex, `#{}` `#()` `##Inf`, both
  tags (and `#js[1]`), the four metadata forms (and metadata on the line
  above), the seven quote-kin prefixes, `#?` and `#?@` at the top, `#:a{}`
  and `#::{}`, the keywords, both skipped discards (and `#_ x y`), discards
  inside forms and prefixed forms, `#!`, the comment above a form with a
  docstring holding `(`, commas, `'` `#` `%` in symbols, terminating
  chars, an unknown tag, a string across lines; positions across lines;
  10,000-deep nesting; the astral char in a symbol and a string, two chars
  each. **Pass.**
- **23.** `t23` (L 783 to 810): the first line, the last, a blank one, the
  whole file; a text without a final newline; the empty text for three
  spans; untrimmed whitespace, CRLF, two blank lines. **Pass.**

**G. Properties.**

- **24.** `t24` (L 820 to 824), 200 trials: texts of up to 40 lines drawn
  from 39 kinds covering every kind the plan names (L 804 to 812), joined
  by `"\n"` or `"\r\n"`, with or without a final terminator; no error and
  no invariant failing. **Pass.**
- **25.** `t25` (L 900 to 910), 200 trials: up to six top-level data built
  from test 22's atoms (31, L 826 to 831) and every prefix it lists,
  nested by `recursive-gen`, with discards inside forms and after prefixes,
  separated at the top by whitespace, commas, comments holding delimiters
  and quotes, `#!` lines and top-level discards (L 871 to 878); the
  generator records where it placed each datum (`place`, L 880 to 891),
  and the scanner must return exactly those chars, those contents, and the
  lines of their first and last chars. **Pass.**

**The error table, row by row:** `:bad-argument` `t09`; `:not-a-repository`
`t10`; `:unknown-revision` `t11`; `:missing-path` `t12`; `:not-a-file`
`t13`; `:too-large` `t14`; `:binary` `t15`, `t16`; `:not-utf-8` `t16`;
`:unreadable` `t17`; `:git-failed` `t18`; `:internal` `t19` and every
result. **Pass.**

**OP17 and OP18.** OP17 is the tool's side, facts only, and nothing the
reader does; OP18's replay condition is `t08`. **Pass.**

**Implicit spec coverage: pass, after TV1 to TV3.**

## Self-consistency check

Re-read. The three findings are marked failures and their fixes cited;
every other check names the deftest that carries it and what it asserts.
No check is passed while naming a missing case: the `:git-failed` rows
that looked untestable without a broken repository are reached through the
private steps with ids the store lacks (TV2), and the two environment
facts that a test cannot set from inside a JVM are covered at the process
by `env` (TV1) and by both branches of the encoding (TV3), with phase 7's
runs under `GIT_DIR` and `LC_ALL=C` as receipts of the same code paths
end to end.

## After the first run (phase 7), recorded here

The first run failed twice, both in `t07`, on a golden number this
validation passed under test 7 above without checking it against an
oracle. Plan test 7 gives the form holding `(defn refusal` as `[101 114]`
and `[125 141]`. Those are the blocks holding that line (a blank line
follows the first `cond` clause), and the forms end at 148 and 182, where
Clojure's reader ends them (`t05` had passed on every form of both files).
The test was wrong, not the code. `t07` now asserts `[101 148]` and
`[125 182]`, checks each end against the reader, and keeps the plan's
numbers as the block extents they are (`BUILD_NOTES-revision-reader.md`,
03:33). That adds nine lines inside `t07`, so every line number above from
L 424 on sits nine lines later in the test source as it now stands.

## Verdict

**minor-fail**: three missing cases, each fixed by lines added inside an
existing deftest (`t09`, `t18`); no namespace added, nothing restructured.

PHASE_VALIDATION:minor-fail
