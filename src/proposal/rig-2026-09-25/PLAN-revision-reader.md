# Plan — the revision reader (rig phase 6, a capability)

Written 26 September 2026 (IST) by the plan step of the rama skill's
process, in a fresh session. Cut by Claude Opus 5.5 (`claude-opus-5-5`) at
max effort. **Not yet validated:** a fresh session validates this plan
(`PLAN_VALIDATION-revision-reader.md`), and another session builds it.
Branch `rig-reader`, worktree `/mnt/data/projects/Softland-rig-reader`, cut
from `rig-2026-09-25` at `7a7403bd`. Paths are relative to the worktree
root unless they say otherwise; "the rig folder" is
`src/proposal/rig-2026-09-25/`; "on main" means
`/mnt/data/projects/Softland` at `main`.

Marks on load-bearing claims: **checked** (run or read tonight, and the
command or line is named), **derived** (follows from checked facts),
**assumed** (not checked; the build or the tests check it).

Loaded for this plan: the rig folder's `README.md` (skimmed), `RIG.md` (top
sections and "What is next"), `SPEC.md` (phase 6), `STARTER-next.md`,
`IMPLICIT_SPEC.md` (OP17, OP18); on main, `PROGRESS.md` "Now" and "Next",
`docs/builds/inland/integration.md` lines 1 to 80, `docs/decisions.md`
"Tools are records over a vocabulary", `PICTURE.md` line 82,
`formal-model-2026-09-24/STARTER-round-3.md`; the rama skill's
`phase-1-plan.md`, `artifact-plan.md` and `artifact-plan-validation.md`.

## 1. What this is, in one paragraph

The revision reader is plain Clojure. Given a repository path, a revision
(any git revision string) and a file path, it returns the file at that
revision cut into units, and each unit is its content and its position,
nothing else. Prose files (Markdown, and any file that is not Clojure) are
cut into passages, which are blocks (section 6). Clojure files (`.clj`,
`.cljc`, `.cljs`, `.edn`) are cut into top-level forms, which is how this
plan reads "functions" (section 7). A third entry reads the lines a person
points at as one unit (section 8). Every failure comes back as data. It has
no Rama in it, no dependency on the store's namespaces, no new library, and
no identity of any kind: no ids, no names used as keys, no hashes offered as
identity. It is a capability in the count's third class, a built-in a tool
calls (section 12). It is not the reference tool, not ingest into the store,
and not a rule for when a passage at one revision is the same as one at the
next; round three designs those.

## 2. Why it exists, and what binds it

- **The ask.** Rig folder `STARTER-next.md`, line 22: "Then the code that
  reads a file at a git revision into its passages and functions, with
  content and position. Give them no store identity: the design session is
  deciding how a passage at one revision is known to be the same at the
  next, and will send what its tool needs from the rig; build that next."
  PROGRESS.md "Next" (on main, `src/proposal/frame-2026-09-15/PROGRESS.md`,
  lines 211 to 218) puts it after the read exit and before the blind count.
- **The tool it serves.** The same lines: round three "designs the seed
  against the reference tool of 13 September, a reference to a passage or a
  function at a revision that shows the result or shows that it is stale".
  `docs/builds/inland/integration.md` (on main), lines 34 to 39, "The forced
  first step": "Nothing can be by-reference until repo material can be
  referenced as facts: a passage at a revision, a function at a revision, a
  decision's clause, a check and its result." The tool's first material,
  `src/proposal/formal-model-2026-09-24/STARTER-round-3.md` (on main), line
  28: "this project's own citations: the file:line references in handoffs,
  plans and PROGRESS.md, which go wrong silently whenever those files
  change." Line numbers are therefore the position that matters first, and
  they must count lines the way a file:line citation does.
- **Grain.** integration.md lines 52 to 54: "Its references are at the
  grain of meaningful work. A reference at file grain repeats the July
  failure". PROGRESS.md "Now", line 27: grain "can be coarsened later, never
  refined". So the default cut is the finer one (sections 6 and 13).
- **No identity.** `src/proposal/frame-2026-09-15/PICTURE.md` (on main),
  line 82: "Identity is irreducible and grain is not upfront. ... A person
  mints an id for a passage by pointing at a range. Minting is a claim with
  an actor, and continuity between minted things is a claim too". Minting
  and continuity are acts with actors; a reader that minted ids or linked
  units across revisions would make those claims silently, with no actor.
  PROGRESS.md "Now", line 65: "Keys are ids; word and grammar are facts about
  them." STARTER-round-3.md line 32 leaves the identity rule for repo
  material to round three (its open items 17 and 60).
- **What kind of code it is.** `docs/decisions.md` (on main), lines 169 to
  170: "A capability is code below the waist, added once, with receipts."
  Line 164: "every result keeps the subject it was built from (its record,
  its revision, its snapshot)", which is why a result carries the commit it
  was read at. Line 181: "a record is total with a work budget per read",
  which is why the reader never throws and reads at most a byte budget
  (sections 5 and 13).
- **Menu, default, pick.** PROGRESS.md "Now", lines 37 to 40: "the menu is
  baked in code, the default lives in the first facts, the pick is a fact on
  a layer, key, tool or value. A pick that decides where or how something is
  written sticks; a pick applied at read can change." The two cuts are the
  menu, the file's extension gives the default until first facts carry one,
  and `:cut` is the pick (section 4). A cut is applied at read, so it can
  change, as long as no kept record depends on a cut's rules (G15).
- **Replays.** Rig folder `IMPLICIT_SPEC.md`, OP18: "A runner re-run on a
  replay must not double its outputs". A read is a function of the commit,
  the path, the cut and the reader's own code, so a replayed tool step gets
  the same units (derived, section 9).

Not done here: the reference tool, its facts or its seed; minting; any
identity or continuity rule; the runner (phase 6 proper); anything under
`src/app`; any Rama cluster.

## 3. The shapes

Written so it can stand as the reader's part of round three's one-page
contract, which the blind count's fresh session will write a tool from
(PROGRESS.md "Next").

### A unit

```clojure
{:content  "# Inland — run and use the build"
 :position {:lines [1 1]      ; first and last line: 1-based, inclusive
            :chars [0 32]}}   ; start and end: 0-based, end exclusive
```

That unit and these are real (checked: computed with python over the bytes
of `git show ce6ebaeb:docs/builds/inland/README.md`):

```clojure
{:content  "1. Point at Orb and Ring. Each initially selects itself. ..."
 :position {:lines [44 45] :chars [1732 1878]}}   ; an item with its continuation
{:content  "```sh\nbin/inland up\n```"
 :position {:lines [16 18] :chars [624 647]}}     ; a fence
```

Conventions:

- **Lines.** A line ends at `"\n"`; a `"\r"` directly before it is part of
  its terminator. Lines are numbered from 1, as editors and file:line
  citations count them. For a file that ends in a newline, git's count of
  newlines is the reader's line count (checked for the Markdown fixture: 96
  newlines, 96 lines).
- **Chars.** Indices into the file's text decoded from UTF-8, counted in
  UTF-16 code units (Java and JavaScript string indices), start inclusive,
  end exclusive, so `(subs text start end)` is exactly `:content`. They are
  not byte offsets: in that fixture line 3 starts at char 34 and byte 36,
  because the em dash on line 1 is one char and three bytes (checked).
- **Content is the exact substring.** Nothing is normalized: indentation,
  trailing spaces, tabs, `"\r\n"` inside a unit, a byte order mark.
- **Extent.** A unit of whole lines (a block, a span) starts at the first
  char of its first line and ends just before its last line's terminator. A
  form starts at its first char, including a prefix such as `^`, `'` or
  `#?`, and ends just after its last char.
- **Order.** Units come in file order. A unit's place in the vector is
  order, not identity.

A unit has exactly the keys `:content` and `:position`, and `:position`
exactly `:lines` and `:chars`. No other key appears anywhere in a
successful output (tested, section 11, test 20).

### A result

```clojure
{:rev    "HEAD"                                      ; the revision as given
 :commit "7a7403bd7850539e58c37da0fd0936bf46214f9b"  ; the commit it named when read
 :path   "src/proposal/rig-2026-09-25/RIG.md"        ; repository-relative, as given
 :cut    :blocks                                     ; the cut that made :units
 :units  [unit ...]}
```

`:commit` is 40 hex digits, or 64 in a SHA-256 repository. It identifies the
snapshot, not any passage: it is what "at a revision" means once `HEAD` has
moved, and decisions.md line 164 asks every result to keep it. The reader
offers no blob id and no hash of any unit. The other two entries return
`{:rev :commit :path :text}` (the whole file) and `{:rev :commit :path
:unit}` (a span).

### Errors

An error is a map with `:error`; a success never has it. Every error carries
the inputs as given (`:repo`, `:rev`, `:path`) and `:commit` once it is
known, plus the keys below.

| `:error` | When | Also carries |
|---|---|---|
| `:bad-argument` | An input is malformed, checked before git runs: `repo` blank; `rev` blank, starting with `-`, or holding NUL, `\n` or `\r`; `path` blank, starting with `/`, ending with `/`, holding NUL, or with an empty, `.` or `..` segment; `:limit` not a positive integer; `:git` blank; `:cut` not `:blocks` or `:forms`; span lines not `1 <= first <= last`; after the read, span `last` past the file's last line | `:argument` (which input), `:value`; for a span past the end, `:line-count` |
| `:not-a-repository` | git exits 128 while resolving the revision: the directory is missing or not in a repository | `:detail` (git's stderr) |
| `:unknown-revision` | the revision names no commit (git exits 1), a tree or blob id included | |
| `:missing-path` | no entry at exactly that path in the commit's tree | `:commit` |
| `:not-a-file` | the entry is a directory, a symlink or a submodule | `:entry` (`:directory`, `:symlink`, `:submodule`) |
| `:too-large` | the blob is larger than the byte budget | `:size`, `:limit` |
| `:binary` | a NUL byte among the first 8,000 bytes | |
| `:not-utf-8` | the bytes are not valid UTF-8 | |
| `:unreadable` | the form cut cannot find where a form ends | `:at {:line :char}`, `:reason` (`:unclosed`, `:unclosed-string`, `:mismatched-close`, `:unexpected-close`, `:bad-dispatch`) |
| `:git-failed` | git could not be started, or exited in a way the step does not expect | `:detail`, and `:exit` when there is one |
| `:internal` | an exception nothing above foresaw, caught at the entry | `:detail`; the tests assert it never appears |

## 4. Operations

These are the plan template's "Reads". The reader has no writes.

Public, in `rig.revision`:

- `(read-units repo rev path)` and `(read-units repo rev path opts)`: a
  result or an error. `opts`: `:cut` (`:blocks` or `:forms`; default from
  `cut-for`), `:limit` (bytes; default 1,048,576), `:git` (the git
  executable; default `"git"`).
- `(read-span repo rev path first-line last-line)` and the same with
  `opts` (`:limit`, `:git`): `{:rev :commit :path :unit}` or an error.
- `(read-text repo rev path)` and with `opts`: `{:rev :commit :path :text}`
  or an error.
- `(blocks text)`: `{:units [...]}`; it cannot fail.
- `(forms text)`: `{:units [...]}`, or `{:error :unreadable :at ... :reason
  ...}`.
- `(span text first-line last-line)`: `{:unit ...}`, or `{:error
  :bad-argument :argument :lines :value [first last] :line-count n}`.
- `(cut-for path)`: `:forms` when the path, lowercased, ends in `.clj`,
  `.cljc`, `.cljs` or `.edn`; `:blocks` for every other path.

The four pure functions take text the caller already holds, so a runner can
cut text that did not come from git. `read-units` is `read-text` then the
cut; `read-span` is `read-text` then `span`. Calls share nothing: no atom,
no dynamic var, no cache.

One entry with a `:cut`, rather than one for passages and one for
functions, because a Clojure file read as blocks is a useful second reading
(a comment sitting directly above a form comes in the form's block; a
one-map EDN file comes in blank-line chunks), and because a cut named by a
parameter is the pick PROGRESS.md lines 37 to 40 describe.

## 5. The read path

Git 2.43.0 on this machine (checked). Every step below uses exit codes and
fixed output formats; no message text is parsed.

**Step 0, the inputs (pure).** The checks in the `:bad-argument` row. A
revision that starts with `-` could reach git as an option (git's reading
commands accept options such as `--output`, which writes a file); refusing
it, and `--end-of-options` in step 1, each close that door. Refusing
absolute paths and `.` or `..` segments loses nothing: paths in a git tree
are always relative and normalized.

**Step 1, the commit.** `git -C <repo> rev-parse --verify --quiet
--end-of-options <rev>^{commit}`. Exit 0: stdout, trimmed, is the commit
id. Exit 1: `:unknown-revision`. Exit 128: `:not-a-repository`. Anything
else, `:git-failed`. Checked tonight: `no-such-rev-2026` and forty zeros
exit 1; a tree id exits 1 (with a line on stderr despite `--quiet`); a
missing directory and a directory outside any repository both exit 128;
`--output=x` placed after `--end-of-options` is taken as a revision and
exits 1. From here on every command names the commit id, never the
revision string, so a ref that moves during the read (a commit landing on
`HEAD`) cannot split one read across two snapshots (derived).

**Step 2, the entry.** `git --literal-pathspecs -C <repo> ls-tree -z
--full-tree -l <commit> -- <path>`. The output is NUL-terminated records,
each `<mode> <type> <oid> <size>\t<path>`, the size right-aligned with
spaces and `-` for a tree. Take the record whose path equals the requested
path exactly; if there is none, `:missing-path`. Mode `040000`:
`:not-a-file :directory`; `120000`: `:symlink`; `160000` (type `commit`):
`:submodule`; a blob with mode `100644` or `100755` goes on with its oid and
size. Checked tonight: a file gives one record; a directory named without a
trailing slash gives its own tree record, and with one lists its children
(hence both the refusal of a trailing `/` and the exact match); a missing
path gives nothing, exit 0; `--full-tree` makes paths repository-relative
whatever the working directory; and `--literal-pathspecs` matters: without
it the path `:(top)AGENTS.md` matched `AGENTS.md` through pathspec magic,
with it, nothing.

**Step 3, the budget.** A size over `:limit` gives `:too-large` before any
byte is read. The default, 1 MiB, holds every tracked text file outside
`src/app` but one, a 6.3 MB transcript under `history/` (checked with
`ls-tree -r -l` over this branch's head); `vision/LOG.md` is 167 KB, the
largest rig plan 86 KB.

**Step 4, the bytes.** `git -C <repo> cat-file blob <oid>`, stdout read as
bytes. Plumbing gives the object's bytes exactly: no pager, no colour, no
textconv, no filters, no line-ending conversion. The tests read the same
files with `git show <commit>:<path>`, a different command, as the
independent oracle (section 11).

**Step 5, text or not.** A NUL among the first 8,000 bytes gives `:binary`.
That is git's own test for binary content (the 8,000-byte window is
assumed from memory of git's source, `FIRST_FEW_BYTES` in
`xdiff-interface.c`; the tests check the classification against git's own
output, `git diff --numstat` from the empty tree, which prints `-` for a
binary file: checked tonight, `-` for the fixture PNG and `96 0` for the
fixture Markdown). `.gitattributes` is not consulted.

**Step 6, decode.** Strict UTF-8: a decoder set to report malformed and
unmappable input, never to replace it; failure gives `:not-utf-8`. A byte
order mark, if present, stays as the text's first char.

**Step 7, the cut,** on the decoded text (sections 6 to 8).

**How the commands run.** `clojure.java.shell/sh` with an argument vector
and no shell, so no argument is ever interpreted by a shell; stdout as
bytes for step 4 and as UTF-8 text otherwise; stderr kept as `:detail`. An
exception from starting the process (git missing) gives `:git-failed`.
Each public entry also wraps its body in a catch of `Exception` that gives
`:internal`, so a runner can call it from anywhere, a Rama task included,
and never see an exception (an exception in topology code kills the
worker: RIG.md, phase 0). Three processes per read (by construction; their
cost, a few milliseconds each, is assumed). No timeout, because
`clojure.java.shell` has none and local plumbing reads of a bounded blob
wait on nothing (assumed). A caller whose environment sets `GIT_DIR` reads
that repository instead of the one named (git's rule); the runner must not
set it.

**What can be read.** Only what a commit holds: the working copy,
uncommitted edits and untracked files are invisible (derived: every command
names a commit). So the partial, untracked `test/rig/store/gate_test.clj`
exists at no revision and this code cannot reach it; no test names its
path. `src/app/server/env.clj` is gitignored (`.gitignore` line 16; checked
with `git check-ignore`, which reads no file). The reader has no path
policy of its own: which paths a tool may read is the permission it runs
under, which round three and the runner decide.

## 6. Passages: the block cut

A passage is a block. The file's non-blank lines are cut into blocks, each a
run of whole lines, by six rules.

1. **Blank lines end blocks.** A blank line (whitespace only, `\r`
   included) ends the open block, except inside a fence (rule 3) and inside
   a list item whose next non-blank line is indented as its continuation
   (rule 5).
2. **A heading is a block by itself.** A heading line is up to three
   spaces, one to six `#`, then a space, a tab or the end of the line. It
   ends whatever block is open and is a one-line block.
3. **A fence is kept whole.** A line whose first non-blank characters are
   three or more backticks, or three or more tildes, opens a fence. The
   fence runs, blank lines included, to the first later line holding only
   the same character repeated at least as many times, and whitespace. An
   unclosed fence runs to the last non-blank line of the file. Inside a
   fence no other rule applies. A fence opened outside a list item is its
   own block and ends an open paragraph; a fence inside a list item,
   indented at least to the column where the item's text begins, stays in
   the item.
4. **A list item is a block.** A list item line starts, after its
   indentation, with `-`, `*` or `+`, or with one to nine digits, optionally
   one lowercase letter, then `.` or `)`; and then a space, a tab or the end
   of the line. It starts a new block when its indentation is no more than
   that of the open block's first line; indented deeper, it is a nested
   item and stays in the block it is in. In a paragraph (a block that did
   not start as an item), a bullet or the number 1 interrupts the paragraph
   and any other number does not, because a wrapped line can begin with a
   number and a full stop. That is CommonMark's rule, and it is measured
   here: in the 36 Markdown files of `docs/`, `src/proposal/` and `vision/`
   on this branch, ten lines begin with a number marker directly under a
   paragraph line; the eight that are `1.` start real lists (PROGRESS.md's
   "**The nine ...**" label directly above "1. Gate kind", for one), and
   the two that are not, `LEDGER.md` line 128 ("560. Line numbers below are
   in that file") and the rig's `README.md` line 354 ("64. One offerer at
   a time"), are wrapped prose (checked with awk tonight). The letter is
   this project's own convention, not CommonMark's: three lines in those
   files use it, each meant as its own item, ruling "7b." at PROGRESS.md
   line 101 and steps "2a." and "2b." at `PLAN-micro-store.md` lines 488
   and 495 (checked). Without it, ruling 7b would read as part of ruling
   7's block, as it renders.
5. **A list item keeps its continuation.** An item runs over every later
   line until it ends: nested items, indented fences, and paragraphs after
   blank lines when the next non-blank line is indented at least to the
   column where the item's text begins (after the marker and the spaces
   after it). After a blank line, a line indented less than that ends the
   item. A non-blank line directly below, with no blank line between,
   continues it (a lazy continuation), unless rule 2, 3 or 4 starts a new
   block there.
6. **Everything else is paragraph text:** tables, block quotes, HTML,
   thematic breaks, setext underlines, indented code. A paragraph runs until
   a blank line, or until rule 2, 3 or 4 starts a new block.

Indentation counts a tab as advancing to the next multiple of four columns,
as CommonMark does; the content keeps the tab.

**Invariants** (tested, section 11): blocks are in file order and do not
overlap; every non-blank line of the file is in exactly one block; every
block begins and ends on a non-blank line; a blank line is inside a block
only within a fence or a list item's continuation; `(subs text start end)`
is the content.

**The golden cut.** `docs/builds/inland/README.md` at `ce6ebaeb` (on main;
96 lines, 4,701 bytes, 4,697 chars, checked) cuts into these 24 blocks, by
line range. The list is derived by reading git's output of the file line by
line against the six rules, not by running any code; the build confirms it.

    [1 1] heading       [3 6]              [8 10]
    [12 12] heading     [14 14]            [16 18] fence
    [20 23]             [25 29]            [31 34] fence
    [36 40]             [42 42] heading
    [44 45] [46 48] [49 51] [52 55] [56 60]   the five items of an ordered
                                              list, continuations indented 3
    [62 63]             [65 65] heading    [67 68]
    [70 74] fence       [76 79]            [81 85]
    [87 92]             [94 96]

**Why blocks, and items rather than whole lists.** The caller's example
offered "a heading's section, or a block between blank lines, with headings,
lists and fenced code kept whole". Sections are the coarser reading, and
grain "can be coarsened later, never refined" (PROGRESS.md line 27), so the
finer one is the default. Whole lists are too coarse for this project's
first material: its rulings are written as list items. PROGRESS.md's "The
nine" (lines 67 to 107) is one tight list; as one block, a citation of
ruling 3 would resolve to all nine, and a change to any ruling would touch
all nine. Items are the grain the rulings are cited at. Fences are kept
whole because a blank line or a `# ` inside a code block is not prose.

**What it gives up.** A heading's section is not a unit: the heading's
block is its one line. A list as a whole is not a unit. An item's later
paragraph stays with the item only when indented. Indented code, HTML
comments and block quotes that hold blank lines are split at them. A setext
heading reads as a paragraph with its underline. A heading inside a list
item ends the item. Non-Markdown text files (shell, YAML, Python) get the
same rules, so a `# comment` line there reads as a heading. A byte order
mark before `#` on line 1 turns that heading into paragraph text. And `7b.`
starts an item here although Markdown renders it as text.

**The first coarsening round three may ask for** is the heading's section:
a heading and every line to the next heading of its level or higher. It
sits on the same line classification and is about twenty lines. It is not
built tonight, because STARTER-next.md says round three "will send what its
tool needs from the rig"; when it comes, it is another entry in the same
capability (section 12).

## 7. Functions: the form cut

A function at a revision is read as the top-level form that holds it, and
every top-level form is a unit: `ns`, `def`, `defn`, `defn-`, `defmacro`,
`defmulti`, each `defmethod`, `defprotocol`, `defrecord`, `deftype`,
`extend-protocol`, Rama's `defmodule` and `deframaop`, `declare`, a
`(comment ...)` block, a top-level expression, even a top-level keyword or
string. The reader does not decide which forms are functions. Deciding
would take a list of defining heads that grows with every macro (the rig's
own code defines with Rama's), and selecting a form by its head and its
name is where naming, and so identity, begins.

How each thing the brief names is handled:

- **A docstring** is inside its form, so it is part of the form's content.
- **A comment above a form**, like a banner comment between forms, belongs
  to no unit: comments and whitespace between top-level forms are outside
  every unit. `read-span` reads any lines, comments included, and `:cut
  :blocks` on the same file returns a comment sitting directly above a
  form in the form's block.
- **Reader conditionals** (`#?(...)`, `#?@(...)`) are part of the form they
  sit in. A top-level reader conditional is one unit holding every branch;
  nothing is chosen per platform.
- **Namespaced keywords** (`:a/b`, `::b`, `::alias/b`) and namespaced maps
  (`#:a{...}`, `#::{...}`) are tokens to the scanner. No alias is resolved,
  so a file is cut without its namespace or its requires being loaded.
  Clojure's own reader needs the alias map to read `::alias/b`, or a
  resolver bound in its place (checked tonight: with a permissive
  `*reader-resolver*` bound, `::str/x` reads).
- **Discards.** A top-level `#_` and the datum it discards are skipped like
  a comment, since Clojure's reader yields nothing for them; `#_ #_ a b`
  skips both, as Clojure does. Inside a form, a `#_` is part of the form.
  So commented-out code is never glued to the form after it.
- **Metadata** (`^:private`, `^{...}`, `#^`) belongs to the form it
  precedes, and the unit starts at the `^`.
- **Other prefixes** (quote, syntax-quote, unquote, unquote-splicing,
  deref, var-quote, `#=`, a tag such as `#inst`) belong to the datum after
  them.
- **Strings, character literals and regexes** are scanned so a delimiter,
  `;` or `"` inside them counts for nothing: `\(`, `\)`, `\;`, `\"`,
  `"(;\""`, `#"[)\"]"`. The rig's `envelope.clj` at `ea52c424` has escaped
  quotes in its namespace docstring (line 10) and a regex holding `[`, `]`,
  `'` and `#` (line 73) (checked), so a real fixture carries them.
- A comma is whitespace, as in Clojure. `;` and `#!` start a comment that
  runs to the end of the line.

**The scanner.** Lexical, iterative (no recursion, so no depth limit and no
stack overflow on deep nesting), one pass over the text's chars. It keeps a
stack of the closing delimiters it expects, and at depth 0 a count of the
data the open top-level unit still needs.

- An atom (a token, a string, a character, a regex, a symbolic value such
  as `##Inf`), or a collection whose closer brings the depth back to 0,
  completes a datum. If nothing is open, the atom is a unit by itself;
  otherwise the count drops by one, and the unit (or skipped region) ends
  when it reaches 0.
- A prefix that yields a datum and takes one (`'`, `` ` ``, `~`, `~@`,
  `@`, `#'`, `#=`, `#?`, `#?@`, `#:` with its namespace, a tag): if nothing
  is open it opens a unit needing one; otherwise it fills the slot it sits
  in and opens one, so the count stays. Metadata (`^`, `#^`) takes two: a
  unit needing two, or the count plus one.
- `#_` takes one and yields nothing: the count plus one. With nothing open
  at depth 0 it opens a skipped region needing one, which emits no unit.
- Openers are `(`, `[`, `{`, `#{`, `#(`. A closer must match the top of the
  stack.
- A token ends at whitespace, a comma, or one of `"` `;` `@` `^` `` ` ``
  `~` `(` `)` `[` `]` `{` `}` `\`, which are Clojure's terminating
  characters; `'`, `#` and `%` do not end one (`foo'` and `foo#` are
  symbols). This set is from Clojure's reader source, `LispReader`,
  assumed from memory; the reader oracle and the property test check it.
- A character literal is `\`, then one char taken whatever it is, then any
  token characters after it (`\newline`, `é`, `\(`).
- In a string or a regex, `\` takes the next char, and the first unescaped
  `"` ends it.
- After `#`, any char other than `{ ( " ' _ = ? : ^ # !` or the start of a
  tag symbol (`#<`, `#` then whitespace, `#` at the end) is `:bad-dispatch`.
- Errors, with `:at` the line and char: a closer with an empty stack,
  `:unexpected-close` at the closer; a closer that does not match,
  `:mismatched-close` at the closer; the end of the text inside a
  collection, or with a count above 0, `:unclosed` at the start of the unit
  or skipped region; the end inside a string or regex, `:unclosed-string`
  at its opening quote.

A unit's `:chars` run from its first char to just after its last, and its
`:lines` are the lines of those two chars.

**Checked against Clojure's own reader tonight** (Clojure 1.12.4 from the
local Maven cache, `*read-eval*` false, a permissive `*reader-resolver*`,
`*default-data-reader-fn*` bound to `tagged-literal`, `:read-cond
:preserve`; start lines from the reader's `:line` metadata, which only
lists carry, and end lines from the reader's line number after each read):

| Fixture | Top-level forms | First forms, by lines | Last |
|---|---|---|---|
| `gate.clj` at `45627e45` | 23 | [2 10] [12 15] [17 20] [24 24] | [229 237] |
| `gate.clj` at `ea52c424` | 28 | [2 14] [16 19] [21 24] [26 30] | [298 307] |
| `envelope.clj` at `ea52c424` | 51 | [2 18] | [377 382] |
| `geometry.cljc` at `d9e619b5` | 11 | [1 7] | [39 53] |
| rig `deps.edn` at `7a7403bd` | 1 | ends at line 19 | |

(`gate.clj` and `envelope.clj` are under the rig folder's `src/rig/store/`;
`geometry.cljc` is `src-inland/softland/inland/geometry.cljc`.) The form cut
must return exactly these counts and lines.

**Why a scanner of its own, and no library.** Clojure's reader interprets
as it reads: it needs aliases for `::alias/b`, refuses `#=` and record
literals unless evaluation is on, needs readers for tags, and stops at the
first error; it records `:line` for lists only, and folds `\r\n` into one
char, so it gives neither exact char offsets nor positions for vectors, maps
or atoms. `tools.reader` gives end positions but interprets the same way
and is a new library. `rewrite-clj` is the right tool for a full syntax
tree, names and docstrings as nodes, and is the first addition to make if
round three asks for names or inner forms; for top-level boundaries it is
not needed. The scanner is about a hundred lines, and Clojure's reader stays
in the tests as its oracle.

**What it gives up.** Which forms are functions. Inner units: arities, a
`let`'s definitions, the forms inside `(comment ...)`, the branches of
`#?(...)`, a module's topologies. Comments and commented-out code. A file
with one broken form is `:unreadable` as a whole at that revision (`:cut
:blocks` still reads it). An EDN file of one map is one unit, since its top
map is its one form; the finer reading, the entries of the top map, is the
addition to make if round three's tool records are EDN files
(integration.md line 50: "Today that means editing an EDN record").
Semantic errors pass: a top-level `#?@`, an unknown tag, or a token Clojure
would refuse still form units.

## 8. The span

`(span text first last)` returns lines `first` through `last` as one unit:
whole lines, blank lines included, nothing trimmed. It serves the lines a
person points at (PICTURE.md line 82, "pointing at a range") and the
file:line citations that are round three's first material (STARTER-round-3
line 28). A file's line count is the number of `"\n"`, plus one when the
text does not end with one; an empty file has no lines. A range outside
`1 <= first <= last <= line count` is `:bad-argument` with `:line-count`.
The span is beyond the brief's literal ask (G13), and cutting it touches
nothing else.

## 9. What does not apply, and what little there is to size

The plan template's Rama sections (PState design, depots, topologies, query
topologies, partitioning) do not apply: this is plain Clojure with no Rama
in it.

**Writes:** none. **State:** none. No PState, no task global, no atom, no
dynamic var, no cache; git's object store is the only storage, and this code
only reads it. A result is a function of the commit, the path, the cut and
the reader's code, so a restart loses nothing and a retry returns the same
result (derived). No cache is designed, and none should be added without the
examination CLAUDE.md requires; if many reads a second ever matter, the
first thing to examine is one long-lived `git cat-file --batch` process
instead of three processes a read, which keeps no results.

**Resources per read:** three processes; the blob's bytes (at most the
budget, 1 MiB by default), the decoded string (two bytes a char), and the
units' contents, which copy their substrings, so about two to three times
the text at worst; one pass over the text per cut. For the largest text
fixture, RIG.md at `7a7403bd` (26,033 bytes, checked), that is well under a
megabyte (derived).

**Concurrency:** calls share nothing, and reads of git's object store may
run at once (assumed from git's design; the tests do not run reads
concurrently). Called on a Rama task thread, a read would block that task
for its processes' time; where the runner calls it from is phase 6's
decision (section 17).

## 10. Where it lives

- `src/rig/revision.clj` in the rig folder, namespace `rig.revision`. Its
  first line, as the rig's other sources have: `;; IMPORTANT: Before
  modifying this file, re-read PLAN-revision-reader.md.` A namespace
  docstring saying what it is, that it gives no identity, that it never
  throws, and where its plan is. It requires `clojure.java.shell` and
  `clojure.string`, and imports from `java.nio.charset`
  (`StandardCharsets`, `CodingErrorAction`) and `java.nio` (`ByteBuffer`).
  `*warn-on-reflection*` on, and hinted until it is quiet.
- No dependency on `rig.store.*` or any other rig namespace, and nothing in
  the rig depends on it tonight.
- The rig's `deps.edn` is unchanged. Clojure 1.12.4 has
  `clojure.java.shell`, and `test.check`, which the property tests use, is
  already in the `:test` alias. No new library (section 7 says why
  `rewrite-clj` is not needed yet).
- `test/rig/revision_test.clj` in the rig folder, namespace
  `rig.revision-test`.
- About 300 lines for the reader and 450 for the tests (assumed).
