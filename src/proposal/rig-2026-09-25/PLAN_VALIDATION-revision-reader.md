# Plan validation — the revision reader

Written 26 September 2026 (IST), 02:30 to 03:10, by a fresh session:
Claude Opus 5.5 (`claude-opus-5-5`), max effort, following the shape of the
rama skill's `references/phase-2-plan-validate.md` (default FAIL, each
requirement quoted verbatim, a concrete case traced for each, the plan's
lines cited, a self-consistency pass). Branch `rig-reader`, worktree
`/mnt/data/projects/Softland-rig-reader`.

The plan validated is `PLAN-revision-reader.md` at `117b6cf9`. The fixes
were applied in place and committed as `183f381a`; **line numbers below
("L n") are the fixed plan's** at `183f381a`, and each fix is marked
**[Fn]** where it lands in the plan.

Marks: **checked** (run or read in this session; the command or line is
named), **derived** (follows from checked facts), **assumed** (not checked).

## Verdict

**minor-fail.** The plan's shape holds: plain Clojure, git plumbing through
an argument vector, two cuts over text plus a span, exact content and
position, errors as data, no identity, no new library, tests that need no
cluster. Every golden number the plan calls checked was re-checked here and
is right. Fourteen failures were found; each is a localized edit, and all
fourteen are applied (F1 to F14, list at the end). Two are real bugs in the
read path's error mapping (F1, F2), one is a door at the tool boundary
(F4), one a robustness hole in the environment (F5); the rest are
statements the plan made wrongly or left out. None needs a redesign.

## Skipped checks

The template's Rama checks (query topologies, PState schemas, partitioning,
topologies, production readiness of topologies, internal depots, stream
commit boundaries, task-global state) do not apply: the plan has no Rama in
it, no state and no writes (L 618 to 646, checked by reading the whole
plan).

## Inputs read

The whole plan (1,015 lines at `117b6cf9`); the rama skill's
`phase-2-plan-validate.md` and the top of `artifact-plan-validation.md`;
the rig folder's `STARTER-next.md` (whole), `deps.edn`,
`test/rig/test_runner.clj`, `SPEC.md` phase 6 (lines 104 to 110),
`IMPLICIT_SPEC.md` OP17 and OP18 (lines 890 to 912); on main,
`PROGRESS.md` lines 25 to 41, 63 to 66, 205 to 225,
`docs/builds/inland/integration.md` lines 33 to 55, `docs/decisions.md`
lines 162 to 182, `PICTURE.md` line 82, `STARTER-round-3.md` lines 26 to 33.
Not opened: anything under `src/app`, and `test/rig/store/gate_test.clj`.

## The requirements, verbatim, each traced

### R1. Sid's overnight brief

> "the code that reads a file at a git revision into passages and
> functions, with content and position and no store identity"

**Trace.** `read-units "<toplevel>" "ce6ebaeb" "docs/builds/inland/README.md"`
(L 196 to 214). Step 1 resolves `ce6ebaeb` to
`ce6ebaeb01bbc8ea33d9d6ef25c47f9d641a1ee7` (L 251 to 270); step 2 finds
one record, mode `100644`, size 4701 (L 271 to 285; checked here:
`git ls-tree -l ce6ebaeb -- docs/builds/inland/README.md` prints
`100644 blob c0759106… 4701`); step 3 passes the 1 MiB budget; step 4 reads
4,701 bytes; step 5 finds no NUL; step 6 decodes 4,697 UTF-16 units; step 7
cuts by extension into blocks (L 196 to 214, `cut-for`). The first unit is
`{:content "# Inland — run and use the build" :position {:lines [1 1]
:chars [0 32]}}` (L 110 to 118). Checked here with Python over
`git show ce6ebaeb:docs/builds/inland/README.md`: 4,701 bytes, 4,697 UTF-16
units, 96 newlines and 96 lines; `[1 1]` is chars `[0 32]`, `[3 6]` is
`[34 328]`, `[16 18]` is `[624 647]`, `[44 45]` is `[1732 1878]`, `[56 60]`
is `[2597 2973]`, `[94 96]` is `[4426 4696]`; line 3 starts at char 34 and
byte 36. All equal the plan's numbers (L 118 to 128, L 718 to 727). The 24
line ranges of the golden cut (L 424 to 437) were checked here by reading
the file line by line against the six rules: every range is right and the
count is 24.

A function: `read-units … "45627e45" "src/proposal/rig-2026-09-25/src/rig/store/gate.clj"`
cuts by extension into top-level forms (L 475 to 605). Checked here by
running Clojure 1.12.4's reader with the plan's bindings (L 564 to 569)
over `git show`: 23 forms, first `[2 10] [12 15] [17 20] [24 24]`, last
`[229 237]`; `gate.clj` at `ea52c424` 28 forms, `[2 14] [16 19] [21 24]
[26 30]`, last `[298 307]`; `envelope.clj` at `ea52c424` 51 forms, first
`[2 18]`, last `[377 382]`; `geometry.cljc` at `d9e619b5` 11 forms, first
`[1 7]`, last `[39 53]`; rig `deps.edn` at `7a7403bd` one form ending at
line 19. All equal the plan's table (L 571 to 578). None of the four
Clojure fixtures has a top-level prefix at the start of a line or a `\r`
(checked with grep), so the oracle's `:line`, which sits on the `(`, cannot
disagree with a unit that starts at a `^` on an earlier line.

**Content and position:** L 110 to 152. **No store identity:** see
"Identity" below. **Handled: pass**, after F13's statement that `:commit`
and `:path` are position.

### R2. STARTER-next.md, line 22

> "Give them no store identity: the design session is deciding how a
> passage at one revision is known to be the same at the next, and will
> send what its tool needs from the rig"

**Trace.** `RIG.md` at `e53bc6d0` and at `7a7403bd` (test 6, L 739 to 747).
Checked here: 96 lines and 6,891 bytes, then 405 lines and 26,033 bytes;
the heading "## Phase 0: the two Rama claims, and the cross-module read"
is at line 29, then at line 209. The two results differ in `:commit` and
agree in `:path`; each heading is `{:content "## Phase 0: …" :position
{:lines [29 29] …}}` and `{… :lines [209 209] …}`; nothing in either says
the one is the other. The plan also declines to give names and kinds of
forms (L 475 to 486, G14 at L 921), since naming is where identity begins,
and leaves continuity to round three (L 1085 to 1088). **Pass**, with one
wording failure fixed: the plan said the two outputs "carry nothing that
links one to the other" (117b6cf9 L 676), but equal `:path` is a shared
coordinate a reader could take as a link; F13 restates it as position,
not a claim (L 163 to 171, L 743 to 746).

### R3. Why it exists: PROGRESS.md "Next" and integration.md

PROGRESS.md on main, lines 211 to 218, checked: "The model's third round
designs the seed against the reference tool of 13 September, a reference
to a passage or a function at a revision that shows the result or shows
that it is stale … The rig builds the read exit the count needs on the
stream store, then the code that reads a file at a git revision into
passages and functions with no store identity". integration.md lines 42 to
44, checked: "It refers to the actual things being worked on and keeps that
correspondence as they change; when the material changes, the tool shows
the result or shows that it is stale."

**Trace.** A citation "PROGRESS.md line 101" (ruling 7b), made at commit A.
The tool reads PROGRESS.md at A as blocks, finds the block holding line
101 (rule 4's lettered marker makes `7b.` its own block, L 378 to 399),
keeps its content; at a later commit B it reads again and compares
content. Same content at another line: moved; different content: stale;
no block holds it: gone. Every one of these is the tool's comparison over
content and position; the reader gives exactly what the comparison needs
and claims none of the answers. **Pass.**

### R4. "The piece is a capability in phase 6's count (a built-in a tool calls)"

**Trace.** SPEC.md phase 6 (rig folder, lines 104 to 110, checked): "count
how many new compiled steps were needed. Zero is the target." PROGRESS.md
line 222 to 223 reads the count "in three classes: … and capabilities."
The plan puts the reader in the third class (L 838 to 871). **Failed, fixed
(F11):** the plan went on to say that adding it "does not count against
'zero new compiled steps'" (117b6cf9 L 784 to 785). That scores the count
before its definition exists: `IMPLICIT_SPEC.md` OP18, checked: "What
counts as a compiled step is fixed by the plan and written down with the
count (open how it is defined, O21)." The fixed text records one
class-three entry and leaves the scoring to the count (L 856 to 862).

## The checks the orchestrator named

### Identity: no ids, no names used as keys, no hashes offered as identity

**Trace.** Walk a `read-units` result for `gate.clj` at `ea52c424`: keys
`:rev :commit :path :cut :units`; each unit `:content :position`; each
position `:lines :chars`, each two integers (L 110 to 171; test 20 at
L 793 to 800 walks every successful result and admits only those eleven
keys). A unit carries no id, no name (the `defn perm-entity` is inside
`:content`, never a key), no hash, no kind, no index. The vector's order is
order (L 146 to 147). The reader offers no blob id: step 2 reads the oid
and uses it only as the argument of step 4 (L 271 to 297). **Pass.**

**`:commit`: position.** It is a hash, and it is git's identity for a
commit object; the reader offers it as the revision coordinate of the
read, "what 'at a revision' means once `HEAD` has moved" (L 163 to 166),
which decisions.md line 164 asks every result to keep ("every result keeps
the subject it was built from (its record, its revision, its snapshot)",
checked). It names no passage and no function, and two reads at different
commits carry nothing that says their units are the same. Without it the
ask's "at a git revision" has no content once a ref moves.

**`:path`: position.** It is the place in the snapshot the read was asked
at, echoed as given; it is a value, not a key; it makes no claim that the
file at one commit is the file at another (a path can change content, and
content can change path). A unit's full position is `:commit` and `:path`
with its own `:lines` and `:chars`. The plan said this for `:commit` only;
F13 states both (L 163 to 171).

**`:rev` and `:cut`:** the input as given and the name of the rules that
cut (G15 at L 926); neither names a unit.

### The cut: simple, stated whole, changeable by round three

**Blocks (L 357 to 474).** Six rules and a tab rule, invariants that can
be tested, a golden cut. Traced on the README fixture (all 24 ranges right,
above) and on the two lines that begin with a number under a paragraph
line: rig `README.md` line 354 at `7a7403bd` ("64. One offerer at a
time"), checked here, sits under a paragraph line, so rule 4's
paragraph exception keeps it in its paragraph, as the plan says.

**Failed, fixed (F8), "stated whole":** two behaviours the rules produce
were not stated. (a) A nested item stays in its parent's block (rule 4,
L 378 to 386), and the corpus holds 240 indented item lines outside fences
in the 36 Markdown files at `7a7403bd` (checked with a Python count), so
"a passage inside a list" below the top level is not a unit; the plan
argues for the finer grain one level up (items over lists) and did not say
where it stops. Stated now in "What it gives up" (L 451 to 459), G2
(L 884 to 886) and section 17 item 6. (b) Rule 6 lists thematic breaks as
paragraph text, but `* * *` and `- - -` match rule 4's item syntax first;
none occurs in the corpus (checked); stated at L 408 to 413.

**Forms (L 475 to 605).** The scanner is stated as rules a builder can
follow, and its ordering of prefixes, metadata and discards was traced
here: `^:a ^:b x` (count 2, 1, 2, 1, 0, one unit), `#_ #_ a b` (a skipped
region needing 1, then 2, 1, 0, no unit), `^:m #_ x y` (2, 1, 2, 1, 0, one
unit whose datum is `y`, as the reader has it). The terminating set is
Clojure's (`LispReader`: macros other than `#`, `'` and `%`; recalled from
the source, not re-read tonight). **Failed, fixed (F9), "stated whole":**
"whitespace" was not defined (Clojure's is `Character/isWhitespace` or a
comma), and the `:bad-dispatch` parenthetical listed bad dispatches where
it read as a list of tag starts (L 540 to 554).

**Changeable by round three, without losing what it may need.** Both cuts
are pure functions over text, chosen by `:cut` (L 196 to 238); the reader
keeps no records, so a new cut is a new entry (L 838 to 871), and G15
(L 926 to 930) names the one moment this stops being free, when a kept
fact records a cut's name. Traced for the three the orchestrator named:

- *A passage inside a list:* a top-level item is a block; a nested item is
  reachable now by `read-span` over its lines, and a cut making it a block
  is additive (F8).
- *A heading's section:* the heading's block carries its `#` count in its
  content, so a section is the run of blocks from one heading block to the
  next of its level or higher, a coarsening of the blocks output; the plan
  plans it as a twenty-line entry (L 468 to 473).
- *A function's name:* inside `:content` exactly, readable by Clojure's
  reader or `rewrite-clj` (L 582 to 593); PICTURE.md line 82, checked:
  "Ingest mints ids for named forms because reference by name is the
  demand it serves", which is round three's act, not the reader's.

Nothing is lost because content is the exact substring and positions are
exact (L 130 to 147). **Pass after F8 and F9.**

### Character offsets: counted in UTF-16 code units; a sound default

The plan counts `:chars` in UTF-16 code units of the text decoded from
UTF-8, start inclusive, end exclusive, so `(subs text start end)` is the
content (L 135 to 139, G7 at L 897 to 901). Traced: the README's line 3
starts at char 34 and byte 36 (checked), so these are not bytes; an astral
char counts two (test 22). **Sound as a default**, for a reason the plan
gives and one it does not: every consumer on this stack indexes strings in
UTF-16 (the JVM's Clojure and Rama, ClojureScript and the browser for
Electric), and it is also the default position encoding of the Language
Server Protocol, which editors speak. Its costs are named: it differs from
code points only outside the Basic Multilingual Plane, and from git's bytes
whenever a char is not ASCII. The key's name `:chars` invites a code-point
reading, which the contract's definition answers; the plan already makes
the unit first-record (G7) and offers lines alone as the way round (L 1080
to 1082). **Pass.**

### The read path's safety

- **Option injection.** A `rev` starting with `-` is refused in step 0 and
  `--end-of-options` follows (L 244 to 270); a `path` comes after `--`
  (L 271); `-C` takes the next argument as a directory, so a `repo`
  beginning with `-` is a directory name; the commit and oid passed on are
  git's own hex. Checked here: `--output=x` after `--end-of-options` exits
  1. **Failed, fixed (F3):** the plan said git 2.24 suffices (117b6cf9
  L 830); `rev-parse` learned `--end-of-options` in 2.30
  (`/usr/share/doc/git/RelNotes/2.30.0.txt` line 74, checked; 2.24's line
  18 is the other commands' option parser). **Failed, fixed (F4):** the
  public option `:git` let each call name the executable (117b6cf9 L 192
  to 197). A tool's arguments come from facts; with `:git "sh"` the call
  becomes `sh -C <repo> rev-parse …`, and `sh` runs the file named by
  `repo` as a script (derived from `sh`'s argument rules, not run). The
  executable is now an argument of the private `read-text` only (L 207 to
  211, test 18 at L 788).
- **Odd revisions.** Checked here with `git rev-parse --verify --quiet
  --end-of-options <rev>^{commit}` over 30 revision strings in this
  repository. **Failed, fixed (F1):** `HEAD@{99999}` exits 128 inside a
  good repository, which the plan mapped to `:not-a-repository`; exit 128
  is now settled by `rev-parse --git-dir` (L 251 to 270, error table
  L 185 to 186). **Failed, fixed (F2):** `^HEAD` exits 0 and prints
  `^117b6cf9…`, a negated revision, which the plan would have taken as the
  commit id and carried to step 2; the id's form is now checked. Ranges
  (`main..rig-2026-09-25`, `main...HEAD`) exit 1 with lines on stdout, so
  the exit code decides; `HEAD:AGENTS.md`, `:/…`, `@{upstream}`,
  `HEAD~99999`, `{`, `*`, a tree id and the empty tree all exit 1. Tests
  gain `HEAD@{99999}`, `^HEAD` and a range (L 765 to 768).
- **Pathspec magic.** `--literal-pathspecs` and the exact match on the
  returned path (L 271 to 285): trusted as the plan checked it
  (`:(top)AGENTS.md`), not re-run.
- **Symlinks.** Mode 120000 is refused (checked here: `AGENTS.md` at
  `ce6ebaeb` is `120000`); a path through a symlinked directory finds no
  record in the tree, so the reader never follows a link out of the
  repository (derived from ls-tree reading the tree, not the disk).
- **Submodules.** Mode 160000 refused; a path inside one finds no record
  (derived). Tested through the record parser (test 13), since none is
  tracked.
- **Binary and huge files.** The size is read before the bytes (L 286 to
  291); the NUL test is git's (L 298 to 305; `FIRST_FEW_BYTES` 8000 is
  recalled from git's source by both sessions, not re-read). No
  `.gitattributes`, replace refs or partial-clone promisor exist in this
  repository (checked), so `cat-file` and the `git show` oracle read the
  true bytes and no read can trigger a fetch. **Failed, fixed (F12):** "never
  throws" holds for `Exception` only; a caller-set `:limit` that the heap
  cannot hold several times over ends in `OutOfMemoryError`, which the
  catch does not take; stated, with the runner owning the bound (L 318 to
  324). No cap number was invented.
- **Non-UTF-8.** A strict decoder, `:not-utf-8` (L 306 to 309), and the
  NUL test first, so NUL-free Latin-1 is `:not-utf-8` (test 16). **Failed,
  fixed (F14):** the JVM passes arguments to git in `sun.jnu.encoding`,
  checked here as UTF-8 under `LANG=en_US.UTF-8` and `ANSI_X3.4-1968` under
  `LC_ALL=C`; there, a non-ASCII path would reach git as `?` and read as
  `:missing-path`, a false answer. Step 0 now refuses a non-ASCII `rev` or
  `path` unless the encoding is UTF-8 (L 340 to 347, error table L 184).
  No tracked path at the rig branch's head is non-ASCII (checked).
- **The environment. Failed, fixed (F5):** the plan left inherited
  `GIT_DIR` to the runner (117b6cf9 L 301 to 303). A git hook or `git
  rebase --exec` sets it; git then reads that repository instead of the
  one named, and `/proc` (test 10) is suddenly inside one. Every `GIT_`
  variable is now dropped from the command's environment, the oracles run
  the same way, and `git diff` runs with `--no-ext-diff --no-textconv`
  (L 330 to 338).

### Every error as data

Every step returns data (L 175 to 195, L 239 to 356). **Failed, fixed
(F6):** a value of the wrong type (a keyword `rev`, a number `path`, a
`nil` span line, non-string text for a pure cut) would reach
`clojure.string/blank?` or `<=` and throw `ClassCastException` or
`NullPointerException`, caught as `:internal`: data, but mislabelled, and
the suite's "no `:internal` anywhere" (test 19) never tried one. Type
checks are now in the `:bad-argument` row and in test 9 (L 184, L 756 to
762). **Failed, fixed (F10):** "a retry returns the same result" and "a
replayed tool step gets the same units" hold for a commit id, not for
`"HEAD"` or a branch, which resolve again on each read; stated, and the
replay rule left to the runner (L 92 to 98, L 626 to 629, section 16).

### The oracles, and no cluster

Oracles are git's output (`git show`, `rev-parse`, `diff --numstat`) and
Clojure's reader, never the code under test (L 704 to 710). The block
cut's golden list is hand-derived, not an independent parser; it was
re-derived here and agrees. **No cluster:** `clojure -M:test
rig.revision-test` runs `rig.test-runner`, which requires only the
namespaces named on its command line, runs them, calls `shutdown-agents`
and exits (rig folder `test/rig/test_runner.clj`, read here); the new
namespace requires no Rama namespace (L 647 to 666), so no cluster starts
and no lock is taken, and `test/rig/store/gate_test.clj` is never loaded
because nothing names it. **Failed, fixed (F7):** the plan never said which
`repo` a good read passes; it is now the toplevel of the worktree the tests
run in, from `git rev-parse --show-toplevel` (L 683 to 690). **Pass after
F5 and F7.**

### Fixtures: every commit exists and is reachable

Checked here in `/mnt/data/projects/Softland`'s object store with `git
cat-file -t` and `git merge-base --is-ancestor`:

| Commit | Type | Ancestor of `rig-2026-09-25` | of `rig-reader` | of `main` |
|---|---|---|---|---|
| `ce6ebaeb01bbc8ea33d9d6ef25c47f9d641a1ee7` | commit | yes | yes | yes |
| `d9e619b575fb00140d56ad196f3da2520fb815f1` | commit | yes | yes | yes |
| `e53bc6d01a44cfb8a4967703cf2dc82446f88c76` | commit | yes | yes | no |
| `7a7403bd7850539e58c37da0fd0936bf46214f9b` | commit | yes | yes | no |
| `45627e45d412b2c4c1b1224594823adc47e59d99` | commit | yes | yes | no |
| `ea52c424515ae06b5d283da82cef8296a320f825` | commit | yes | yes | no |

`4b825dc642cb6eb9a060e54bf8d69288fbee4904`, the oracle's empty tree, is a
tree git knows without storing it (`cat-file -t` gives `tree`); the repo is
SHA-1 (checked). Paths checked at their commits: the README (4,701 bytes),
`AGENTS.md` (120000), the PNG (878 bytes), the rig `RIG.md` absent at
`ce6ebaeb`, `src` a tree and `deps.edn` a blob at `7a7403bd`, `(defn
perm-entity` at lines 17 and 21, `(defn refusal` at 101 and 125, `(defn
intake` only at `ea52c424` line 213, and reader conditionals on
`geometry.cljc` lines 44 and 45. **Pass.**

### No library without need

`deps.edn` unchanged; `clojure.java.shell`, `clojure.string` and
`java.nio` only; `test.check` already in `:test` (rig `deps.edn`, read
here); `tools.reader` and `rewrite-clj` declined with reasons (L 582 to
593). **Pass.**

### Minimality

The simplest design that meets the ask is the plan's own sketch (L 1003 to
1007): resolve, read, cut by extension, content and position, errors as
data. Every addition was attacked: `:cut` is one key and the read-time pick
PROGRESS.md lines 37 to 40 describe; `read-span` is outside the literal ask
but is PICTURE.md line 82's "pointing at a range" and can be cut alone; the
public pure cuts cost nothing. One addition failed and was removed: the
public `:git` (F4). **Pass after F4.**

## Self-consistency

Every check above that found a failure says so and names its fix; none is
passed while describing a gap. The two places this artifact leans on
recall rather than a read tonight, Clojure's terminating characters and
git's 8,000-byte window, are marked as such; both are also tested by the
build (the reader oracle, the property test, numstat), so neither is
passed on recall alone.

## Fixes applied in the plan

- **F1.** Exit 128 in step 1 is disambiguated by `rev-parse --git-dir`;
  `HEAD@{99999}` is `:unknown-revision` (L 251 to 270, L 185 to 186, test 11).
- **F2.** Step 1's stdout must be one commit id; `^HEAD` is
  `:unknown-revision` (L 251 to 270, L 186, test 11).
- **F3.** Git 2.30 or later, not 2.24 (G9).
- **F4.** The executable leaves the public options; the private
  `read-text` takes it (section 4, test 18, section 15).
- **F5.** Git runs without inherited `GIT_` variables; oracles likewise,
  and `diff` without external diff or textconv (section 5).
- **F6.** Wrong-typed inputs are `:bad-argument` (error table, section 4,
  test 9).
- **F7.** The tests' `repo` is named, and the fixture reachability recorded
  (section 11).
- **F8.** Nested items and spaced thematic breaks stated as behaviour and
  as things given up (rule 6, section 6, G2, section 17).
- **F9.** Clojure's whitespace and the dispatch rule stated exactly
  (section 7).
- **F10.** Replays and retries reproduce a read by its commit, not by a
  moving revision string (sections 2, 9, 16).
- **F11.** The count's scoring of a capability left to the count's
  definition (section 12).
- **F12.** "Never throws" bounded by memory, stated (section 5).
- **F13.** `:commit` and `:path` stated as position; test 6's wording
  (section 3, test 6).
- **F14.** A non-ASCII `rev` or `path` refused under a non-UTF-8 JVM
  encoding (section 5, error table).

## For Sid

- **Nested items (F8).** The default passage stops at top-level list
  items; 240 nested item lines in the Markdown corpus are inside their
  parents' blocks. That is right for "The nine", which has none, and may be
  too coarse where sub-points are cited on their own. It is round three's
  grain question; the reader can add the finer cut without touching a
  record, and `read-span` reaches any nested item now.
- **The count (F11).** Whether a capability such as this one counts
  against "zero new compiled steps" is open (O21); the plan no longer
  answers it.
- G7 (the unit of `:chars`) and G15 (a cut's name frozen once recorded)
  stay as the plan queued them for RIG.md "For Sid".

PHASE_VALIDATION:minor-fail
