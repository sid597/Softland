# Implementation validation — the revision reader

Phase 4 of the rama skill's build, inside the build session (Claude Opus
5.5, `claude-opus-5-5`, max effort, 26 September 2026 IST), following
`references/phase-4-impl-validate.md` and the template
`references/artifact-impl-validation.md`. Reviewed: `src/rig/revision.clj`
against `PLAN-revision-reader.md` (with F1 to F14), the brief it answers
(`STARTER-next.md` line 22, the ask the plan quotes in section 2), and
`PLAN_VALIDATION-revision-reader.md`. Line numbers ("L n") cite
`src/rig/revision.clj` as committed with this artifact.

Marks: **traced** (read through the code for the case named), **ran** (run
tonight in this worktree against git 2.43.0 and Clojure 1.12.4, from probes
in the session's scratchpad; the tests in phase 5 repeat the load-bearing
ones), **derived** (follows from those).

Default verdict at the start: major-fail. One finding, a literal divergence
from the plan, was found and fixed in place; every check below is traced
against the fixed code.

## Finding, and its fix

- **IV1. `cut-for` had no catch.** Plan section 5: "Each public function,
  the pure cuts included, also wraps its body in a catch of `Exception`
  that gives `:internal`". `cut-for` was written total by construction
  (`string?` guard, `.toLowerCase` with `Locale/ROOT`, `.endsWith`) and left
  bare, which is a divergence without a correctness reason, and phase 4
  admits none other. Fixed: its body is wrapped (L 720 to 733); the catch
  cannot be reached, and its docstring says so. Localized.

Not a finding, recorded because the code changed during this review:
`git-env` now takes the environment as its argument (L 568 to 574) and
`run-git` passes `(System/getenv)` (L 584), so test 18's neighbour can check
the F5 filter on a literal map. Behaviour unchanged.

## Skipped checks

The template's Rama checks (redundant dataflow conditionals, consecutive
keypath, select-compute-transform, `nil->val`, `:allow-yield?`,
non-subindexed collections, stream topology idempotency, partial failure in
stream topologies, single depot append per client operation) do not apply:
the namespace has no Rama in it (traced: no `com.rpl` anywhere in the file;
its requires are `clojure.java.shell` and `clojure.string`, L 28 to 33).

## Application-state caches survive restart

No cache, no atom, no dynamic var, no task global (traced: the only global
reads are `System/getenv` at L 584 and `System/getProperty` at L 564, each
per call; `clojure.java.shell`'s dynamic defaults `*sh-dir*` and `*sh-env*`
are overridden by the explicit `:dir nil` and `:env`, L 584 to 585). A
restart loses nothing; the plan's section 9 says the same. **Pass.**

## No reimplementation of built-in operations

Rama's `ops` do not apply. On the JVM side: decoding is the JDK's own
strict decoder (L 681 to 684); binary search is `Arrays/binarySearch`
(L 86); trimming and blank tests are `clojure.string` (L 132, L 184). The
two hand-written parts are the plan's by design: the form scanner (section
7, "Why a scanner of its own, and no library": Clojure's reader needs
aliases, folds `\r\n` and gives no positions for vectors, maps or atoms;
`tools.reader` and `rewrite-clj` are declined there) and the line index,
whose convention (a `\r` before `\n` is part of the terminator, a lone
`\r` is content) no JDK method implements. **Pass.**

## Plan conformance

Each part of the plan, the check stated, the code, the trace.

**Section 3, a unit is exactly content and position.** `unit` (L 39 to 44)
builds `{:content (subs text start end) :position {:lines [a b] :chars
[s e]}}`, longs throughout; no other constructor of a successful unit
exists (traced: `blocks` L 280 to 283, `forms` L 481 to 483, `span` L 507 all call it).
Ran: the README's first block is `{:content "# Inland — run and use the
build" :position {:lines [1 1] :chars [0 32]}}`, the plan's example.
**Pass.**

**Section 3, results.** `read-units` returns `{:rev :commit :path :cut
:units}` (L 763), `read-span` `{:rev :commit :path :unit}` (L 790),
`read-text` `{:rev :commit :path :text}` (L 716), `blocks` and `forms`
`{:units}`, `span` `{:unit}`. `:commit` is only what `resolve-commit`
matched against `[0-9a-f]{40}|[0-9a-f]{64}` (L 596, L 611). **Pass.**

**Section 3, errors carry the inputs.** Every read error is merged with
`{:repo :rev :path}`: step 0 (L 698, L 755, L 783), step 1 (L 701), steps 2
to 6 with `:commit` (`at`, L 703; L 707, L 709, L 714, L 715), the cut's
`:unreadable` (L 762), the span past the end (L 789), `:internal` (L 764,
L 791). Pure functions return their own keys only (L 273, L 473 to 480,
L 500 to 510). Ran: every case in the table below printed its inputs, and
`:commit` exactly from step 2 on. **Pass.**

**Section 3's error table, row by row** (all ran):

| Row | Code | Ran |
|---|---|---|
| `:bad-argument` | `input-error` L 545 to 563; `:cut` L 753; span lines L 780; pure text L 273, L 473, L 500; past the end L 510 | rev `-p` and `:HEAD`, path `a//b`, opts `[]`, `:limit 0`, `:cut :nope`, span `[5 4]` (no `:commit`), span `[1 97]` (`:line-count 96`, `:commit`), `(blocks nil)`, `(forms 42)`, span `[nil 1]`, `[1.5 2]` |
| `:not-a-repository` | L 615 to 620 | `/nonexistent-rig-reader`, `/proc`, each with git's stderr |
| `:unknown-revision` | L 610 to 614, L 618 | `no-such-rev-2026`, `^HEAD`, the range, `e53bc6d0^{tree}` without `:detail`; `HEAD@{99999}` with `:detail ""` (git prints nothing under `--quiet`) |
| `:missing-path` | L 635 to 636 | `RIG.md` at `ce6ebaeb`, `RIG.md/x` at `7a7403bd`, `:(top)AGENTS.md` |
| `:not-a-file` | L 639 to 641 | `src` (`:directory`), `AGENTS.md` (`:symlink`) |
| `:too-large` | L 708 to 709 | `:limit 1000` gives `:size 26033`; `:limit 26033` reads |
| `:binary` | L 675 to 679 | the PNG fixture; three Markdown files in the sweep below, each also `- -` in git's numstat |
| `:not-utf-8` | L 685 | tested in phase 5 (test 16) |
| `:unreadable` | `scan` L 400 to 454, `forms` L 477 to 480 | README read as `:forms`: `:bad-dispatch` at line 1, char 0; the thirteen strings of test 17 |
| `:git-failed` | `run-git` L 586, `git-failed` L 588 to 594 | `read-text` with `/nonexistent/git` |
| `:internal` | L 285, L 484, L 511, L 733, L 764, L 791 | appears in no run |

**Section 4, operations.** Arities as planned (L 748 to 749, L 775 to 776).
`:cut` defaults to `(cut-for path)` (L 756) and is checked before git runs
(L 753); `:limit` defaults to 1,048,576 (L 515 to 517). The executable is
not an option (F4): both entries pass `"git"` (L 757, L 784); only the
private `read-text` takes it (L 687). Ran: `{:git "/nonexistent/git"}` in
opts reads the README normally. `read-text` is private (`defn-`, L 687);
`read-units` is `read-text` then the cut (L 757 to 763), `read-span` is
`read-text` then `span` (L 784 to 790). **Pass.**

**Section 5, the read path, step by step.**

- Step 0 (traced L 545 to 563, ran as above). The order of checks is
  repo, rev, path, encoding, opts, limit; the plan gives none.
- Step 1: exactly `git -C <repo> rev-parse --verify --quiet
  --end-of-options <rev>^{commit}` (L 605 to 607). Exit 0 with one id is
  the commit, exit 0 otherwise and exit 1 are `:unknown-revision`, exit 128
  runs `rev-parse --git-dir` (F1, F2), anything else is `:git-failed`
  (L 608 to 621). Ran for every revision in test 11.
- Step 2: exactly `git --literal-pathspecs -C <repo> ls-tree -z
  --full-tree -l <commit> -- <path>` (L 652 to 653), the commit id, never
  `rev`. The record parser takes the record whose path equals the request
  exactly (L 630 to 634) and maps modes as the plan does (L 639 to 644).
  Ran: `repo` given as the worktree's `src` subdirectory still reads
  `docs/builds/inland/README.md` (`--full-tree`).
- Step 3: `(> size limit)` before any byte is read (L 708 to 709); the
  budget is inclusive (ran).
- Step 4: `git -C <repo> cat-file blob <oid>`, stdout as bytes (L 663).
- Step 5: a NUL among the first `(min 8000 (alength bs))` bytes (L 675 to
  678).
- Step 6: a decoder set to `REPORT` for malformed and unmappable input
  (L 681 to 683); `CharacterCodingException` gives `:not-utf-8` (L 685); a
  byte order mark decodes to U+FEFF and stays (the JDK decoder does not
  strip it; tested in phase 5).
- How commands run: `clojure.java.shell/sh` with an argument vector, no
  shell (L 584); `IOException` on start is `:git-failed` (L 586); a
  non-zero exit from step 2 or 4 is `:git-failed` (L 655, L 664).
- Environment (F5): every `GIT_` variable dropped (L 568 to 574). Ran, with
  the JVM started under `GIT_DIR=/mnt/data/projects/Softland/.git` and
  `GIT_WORK_TREE=/mnt/data/projects/Softland`: git itself, given that
  environment, reports `/proc` as inside the main repository; the reader
  reports `:not-a-repository` for `/proc` and reads this worktree's own
  `HEAD` (`619b7b47`), not main's.
- Encoding (F14): a non-ASCII `rev` or `path` is refused unless
  `sun.jnu.encoding` names UTF-8, compared as a charset, not a string
  (L 519 to 524, L 555 to 558). Ran: `(input-error repo rev "é.md" {}
  "ANSI_X3.4-1968")` gives `{:error :bad-argument :argument :path :value
  "é.md" :encoding "ANSI_X3.4-1968"}`; this JVM's encoding is UTF-8.

**Pass.**

**Section 6, the block cut.** Traced rule by rule in `block-step` (L 193
to 249): rule 3's fence first, where no other rule applies (L 204 to 211);
rule 1's blank line, which ends a paragraph and only marks an item's
`:gap` (L 213 to 218); rule 2's heading ending anything, an item included
(L 221); inside an item, after a gap the item continues only when the line
is indented to `:col`, a fence indented to `:col` stays and a shallower one
ends it, and an item line no deeper than the item's first line starts a new
block (L 223 to 237); in a paragraph a fence ends it and an item line no
deeper than its first line interrupts only as a bullet or the number 1
(L 239 to 249). The heading and item tests are L 105 to 113 and L 139 to
173, the tab rule L 91 to 103. Ran: the golden cut, all 24 ranges and the
six char positions, first time; the twenty literal cases later in test 21
(fence with a blank and a `# ` line, unclosed fence, indented and
unindented continuation, `2026.` and `1.` under a paragraph, `7b.`, nested
item, heading under a paragraph, CRLF, tab continuation, byte order mark,
fences inside and under items, `* * *`, `***`, a heading in an item, `1)`
against `3)`, a deeper bullet in a paragraph, seven hashes and four
spaces). **Pass.**

**Section 7, the form cut.** Traced: the terminating set is the plan's
(L 289 to 295); a token ends at `Character/isWhitespace`, a comma or a
terminating char (L 297 to 302, F9); strings and regexes let a backslash
take the next char (L 311 to 321); `;` and `#!` are comments (L 323 to
337); the dispatch table is F9's, with `<` refused as Clojure's reserved
`#<` (L 339 to 369); the driver keeps the closer stack and the depth-0
count exactly as the plan's bullets say: an atom or a collection closing to
depth 0 completes a datum (L 427 to 443), a one-datum prefix opens a unit
needing one or keeps the count (L 445 to 449), metadata takes two (L 361,
L 396), `#_` opens a skipped region or adds one (L 451 to 454), errors at
the positions the plan names (L 411, L 421, L 428, L 429). Iterative, no
recursion anywhere in the file (traced; ran: 10,000 nested lists give one
unit `[0 20000]`). Ran: section 7's table exactly (23, 28, 51, 11, 1 forms,
first and last lines); every case listed for tests 17 and 22. **Pass.**

**Section 8, the span.** `span` (L 488 to 511): line count from
`line-index` (L 60 to 81), `1 <= first <= last <= n` or `:bad-argument` with
`:line-count`. Ran: a blank line gives `""` at `[4 4]`; `[1 3]` of `"a\nb"`
gives `:line-count 2`; the empty text has none; CRLF keeps the inner
`"\r\n"`; a whitespace line is untrimmed. **Pass.**

**Section 9, no state, no writes.** Traced above. The only side effects
are the three (or four) git processes a read. **Pass.**

**Section 10, where it lives.** `src/rig/revision.clj`, namespace
`rig.revision`; first line the rig's reminder (L 1); a namespace docstring
saying what it is, that it gives no identity, that it never throws and
where its plan is (L 3 to 27); `*warn-on-reflection*` on (L 35) and quiet
(ran: loading prints no reflection or boxing warning). No `rig.store`
dependency; `deps.edn` unchanged. Imports beyond the plan's list are JDK
classes the code names (`Charset` and `CharacterCodingException` for steps
6 and F14, `Arrays`, `Locale`): no library. **Pass.**

**The fixes F1 to F14, as they land in code.** F1 L 615 to 621; F2 L 611;
F3 nothing to code (the docstring of `read-units` states git 2.30; this
machine has 2.43.0); F4 L 687, L 757, L 784; F5 L 568 to 585; F6 L 552 to
563, L 273, L 473, L 500, L 506; F7 test side; F8 behaviour (ran: a nested
item stays, `* * *` reads as an item); F9 L 297 to 302, L 339 to 369; F10
nothing to code (every command after step 1 names the commit or the oid,
L 704, L 711); F11 documentation; F12 every catch is `Exception` (the six
lines in the table); F13 the namespace docstring (L 15 to 19); F14 L 519 to
558. **Pass.**

**Readings the plan left open.** Each is taken from the plan's own words;
none moves a golden number, and each is logged in the build notes.
"Whitespace" in the block rules is `Character/isWhitespace`, the one
definition the plan gives (F9), while indentation counts spaces and tabs as
section 6 says. Rule 4 compares an item line's indentation with "the open
block's first line" for a paragraph too, so a bullet indented deeper than
the paragraph's first line stays in the paragraph (ran: `"p\n  - x"` is one
block). "The number 1" is the marker's integer value. An unclosed regex's
`:at` is "its opening quote", the char after `#`. `##` with its token chars
is an atom, as section 7 lists `##Inf`. These are readings, not
divergences: the plan says nothing that another reading would follow more
closely.

**Plan conformance: pass, after IV1.**

## Spec conformance, beyond the template

- **The brief: passages and functions, content and position, no store
  identity.** Traced above; ran over the whole tracked tree outside
  `src/app` at this branch's head (a probe, not a committed test): 1,580
  Markdown files cut into 126,747 blocks with every section 6 invariant
  holding (order, cover, edges on non-blank lines, blank lines only in
  fences and items, content and positions against an independent line
  reading); the six files that did not read are refusals, not failures
  (two symlinks, one 6.3 MB transcript over the budget, three with a NUL
  inside the window, which git's numstat also calls binary). 345 Clojure
  files (`.clj`, `.cljc`, `.cljs`, `.edn`) cut into 3,327 forms, each file's
  count, first lines and last lines equal to Clojure's reader with the
  plan's bindings, and the reader read every file. **Pass.**
- **Errors as data, never an exception.** Every public function's body is
  inside a catch of `Exception` (six, listed above); no path throws on a
  wrong-typed input (ran, F6's cases); no recursion, so no
  `StackOverflowError` from depth; `OutOfMemoryError` is the runner's bound
  (F12). **Pass.**
- **No Rama, no store namespace, no new library.** Traced. **Pass.**

## Self-consistency check

Re-read. The one failure found (IV1) is marked a failure and its fix named;
no check is passed while naming a gap. The readings are stated as readings
and argued to follow the plan's text. The duplication between the
`:close` and `:atom` completions in `scan` (L 431 to 443) is two branches
of one `case` doing the same completion for different tokens; `recur`
cannot leave the loop for a helper without allocating a tuple per datum,
so it stays, and it is no divergence from the plan.

## Verdict

**minor-fail**: one localized divergence (IV1, `cut-for` without the
plan's catch), fixed in place at L 720 to 733; everything else traced,
and the load-bearing cases ran.

PHASE_VALIDATION:minor-fail
