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
plan reads "functions" (section 7). A span, the lines a person points at,
is the third kind of unit (section 8), read through a second entry. Every
failure comes back as data. It has no Rama in it, no dependency on the
store's namespaces, no new library, and no identity of any kind: no ids, no
names used as keys, no hashes offered as identity. It is a capability in
the count's third class, a built-in a tool calls (section 12). It is not the
reference tool, not ingest into the store, and not a rule for when a passage
at one revision is the same as one at the next; round three designs those.

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
offers no blob id and no hash of any unit. A span read returns `{:rev
:commit :path :unit}`; the private whole-file read under both entries
returns `{:rev :commit :path :text}` (section 4).

### Errors

An error is a map with `:error`; a success never has it. An error from a
read (`read-units`, `read-span`) carries the inputs as given (`:repo`,
`:rev`, `:path`) and `:commit` once it is known, plus the keys below; an
error from a pure function carries only `:error` and its own keys.

| `:error` | When | Also carries |
|---|---|---|
| `:bad-argument` | An input is malformed, checked before git runs: `repo` blank; `rev` blank, starting with `-`, or holding NUL, `\n` or `\r`; `path` blank, starting with `/`, ending with `/`, holding NUL, or with an empty, `.` or `..` segment; `:limit` not a positive integer; `:git` blank; `:cut` not `:blocks` or `:forms`; span lines not `1 <= first <= last`; after the read, span `last` past the file's last line | `:argument` (which input), `:value`; for a span past the end, `:line-count` |
| `:not-a-repository` | git exits 128 while resolving the revision: the directory is missing, is not in a repository, or git refuses it (an unsafe owner, for one); its stderr says which | `:detail` (git's stderr) |
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
- `(blocks text)`: `{:units [...]}`; it has no error of its own (only
  `:internal`, on a bug).
- `(forms text)`: `{:units [...]}`, or `{:error :unreadable :at ... :reason
  ...}`.
- `(span text first-line last-line)`: `{:unit ...}`, or `{:error
  :bad-argument :argument :lines :value [first last] :line-count n}`.
- `(cut-for path)`: `:forms` when the path, lowercased, ends in `.clj`,
  `.cljc`, `.cljs` or `.edn`; `:blocks` for every other path.

Private: `(read-text repo rev path opts)`, steps 0 to 6 of section 5,
giving `{:rev :commit :path :text}` or an error. `read-units` is
`read-text` then the cut; `read-span` is `read-text` then `span`. It is
private because a whole file offered as a step a tool can call invites the
file-grain reference integration.md lines 52 to 54 warn against; a tool
asks for units or a span. The tests call it through its var.

The four pure functions take text the caller already holds, so a runner can
cut text that did not come from git, and the tests can reach every rule
without git. Calls share nothing: no atom, no dynamic var, no cache.

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
A non-zero exit from step 2 or step 4 gives `:git-failed` too (it can only
happen if the repository changes under the read, for example a pruned
object). Each public function, the pure cuts included, also wraps its body
in a catch of `Exception` that gives `:internal`, so a runner can call it
from anywhere, a Rama task included, and never see an exception (an
exception in topology code kills the worker: RIG.md, phase 0). Three
processes per read (by construction; their cost, a few milliseconds each, is
assumed). No timeout, because
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
   at `7a7403bd`, ten lines begin with a number marker directly under a
   paragraph line; the eight that are `1.` all start real lists, a label or
   a sentence directly above each (checked one by one; PROGRESS.md's "**The
   nine ...**" label directly above "1. Gate kind" is one), and the two
   that are not, `LEDGER.md` line 128 ("560. Line numbers below are
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
  token characters after it (`\newline`, `\u00e9`, `\(`).
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
char (checked tonight: its line-numbering reader reads `"a\r\nb"` as three
chars), so it gives neither exact char offsets nor positions for vectors,
maps or atoms. `tools.reader` gives end positions but interprets the same way
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

## 11. Tests

Run from the rig folder: `clojure -M:test rig.revision-test`. It starts no
Rama cluster and loads no Rama namespace (the `:test` alias puts Rama on the
classpath, and nothing requires it), so it does not wait on the cluster
lock. Name the namespace; never glob test namespaces, so the untracked
`test/rig/store/gate_test.clj` never loads, and never name that path, not
even as a missing-path case.

**Fixtures,** full ids, kept in one map at the top of the test namespace.
All are reachable from this branch; "main" marks the two commits that are
ancestors of `main` (checked with `git merge-base --is-ancestor`). The rig
commits stay reachable while the rig branch, or a merge of it that keeps its
history, does; if it is ever squash-merged and deleted, re-point them in
that one map.

| Commit | Where | Path | What it exercises |
|---|---|---|---|
| `ce6ebaeb01bbc8ea33d9d6ef25c47f9d641a1ee7` | main | `docs/builds/inland/README.md` | the golden block cut (section 6); an em dash on line 1 and an ellipsis on line 53, so chars and bytes differ |
| same | main | `AGENTS.md` | `:not-a-file :symlink` (mode 120000, checked) |
| same | main | `history/docs/below-the-waist/path-kind/production/receipts/png/gpu-path-production-empty-clip.png` | `:binary` (878 bytes, checked) |
| same | main | the rig folder's `RIG.md` | `:missing-path`: the commit before the rig existed |
| `d9e619b575fb00140d56ad196f3da2520fb815f1` | main | `src-inland/softland/inland/geometry.cljc` | a `.cljc` with reader conditionals inside forms (lines 44 and 45) |
| `e53bc6d01a44cfb8a4967703cf2dc82446f88c76` | rig | the rig folder's `RIG.md` | the first of two revisions (96 lines, 6,891 bytes) |
| `7a7403bd7850539e58c37da0fd0936bf46214f9b` | rig | the rig folder's `RIG.md`, `src` and `deps.edn` | the second revision (405 lines, 26,033 bytes); `:directory`; EDN as one form |
| `45627e45d412b2c4c1b1224594823adc47e59d99` | rig | the rig folder's `src/rig/store/gate.clj` | the first of two revisions |
| `ea52c424515ae06b5d283da82cef8296a320f825` | rig | the same `gate.clj`, and `src/rig/store/envelope.clj` | the second revision; escaped quotes and a bracket-holding regex |

**Oracles.** Git's own output and Clojure's own reader, run inside the
tests, never the code under test: `git show <commit>:<path>` (a different
command from the reader's `cat-file`), `git rev-parse <rev>^{commit}`, `git
diff --numstat 4b825dc642cb6eb9a060e54bf8d69288fbee4904 <commit> -- <path>`
(the empty tree; `-` marks a binary file), and the read loop with the
bindings given in section 7.

**A. Faithful to git.**
1. For every text fixture, the private `read-text` gives git show's bytes
   decoded, `:commit` equal to rev-parse's, and a line count equal to git's
   newline count.
2. For every unit of every text fixture, `(subs text start end)` is
   `:content`; for units of whole lines, `:content` is git show's lines
   `first` to `last` joined by `"\n"`, and `start` is the sum of the
   earlier lines' lengths in chars plus one per newline. In the README
   fixture, the block at [3 6] starts at char 34 where git's bytes put
   byte 36, and the block at [56 60] at char 2597 against byte 2601
   (checked tonight).
3. The README fixture cuts into exactly the 24 blocks of section 6, with
   these positions (checked): [1 1] chars [0 32]; [3 6] [34 328]; [16 18]
   [624 647]; [44 45] [1732 1878]; [56 60] [2597 2973]; [94 96]
   [4426 4696].
4. The block invariants of section 6 hold over every text fixture read as
   blocks.

**B. Faithful to Clojure's reader.**
5. For both `gate.clj` revisions, `envelope.clj`, `geometry.cljc` and
   `deps.edn`: the form count, each form's first line against the
   reader's `:line` where it has one, and each last line against the
   reader's line number after the read, equal both the reader's run in the
   test and the numbers in section 7's table; each unit's content, read
   alone, is one datum and then the end; the text outside the units reads
   as nothing.

**C. Two revisions.**
6. `RIG.md` at `e53bc6d0` and at `7a7403bd`: both reads pass A; `:commit`
   differs and `:path` does not; the heading "## Phase 0: the two Rama
   claims, and the cross-module read" is a one-line block at line 29 in the
   first and at line 209 in the second (checked with grep over git show),
   and the test finds it in each by its content. The outputs carry nothing
   that links one to the other; the test's own content match is the only
   link, which is the point.
7. `gate.clj` at `45627e45` and `ea52c424`: the form holding `(defn
   perm-entity` has the same content at [17 20] and at [21 24] (checked:
   the same four lines); the form holding `(defn refusal` differs, [101
   114] against [125 141]; `(defn intake` exists only in the second.
8. A read by `"HEAD"` has the `:commit` that rev-parse gives at test time
   and equals the read by that commit id apart from `:rev`; two reads of one
   commit and path are equal.

**D. Every error path,** each asserting the kind and its extra keys.
9. `:bad-argument`: rev `""`, `nil`, `"-p"`, `"--output=x"`, `"a\nb"`; path
   `""`, `"/etc/hosts"`, `"../x"`, `"a/./b"`, `"a//b"`, the rig folder's
   `"src/"`; repo `""`; `:limit` 0 and -1; `:git ""`; `:cut :nope`; spans
   [0 1] and [5 4]; and, after a read, [1 N+1] on a file of N lines, with
   `:line-count` N.
10. `:not-a-repository`: repo `"/nonexistent-rig-reader"` (missing) and
    `"/proc"` (not in a repository). Neither writes anything.
11. `:unknown-revision`: `"no-such-rev-2026"`, forty zeros, and
    `"e53bc6d0^{tree}"`.
12. `:missing-path`: `RIG.md` at `ce6ebaeb`; a path below a file, the rig
    folder's `"RIG.md/x"` at `7a7403bd`.
13. `:not-a-file`: the rig folder's `src` at `7a7403bd` (`:directory`);
    `AGENTS.md` at `ce6ebaeb` (`:symlink`); `:submodule` through the
    private ls-tree record parser, given the record
    `"160000 commit <40 hex>       -\tvendor/x"`, since no tracked path
    outside `src/app` is a submodule (checked).
14. `:too-large`: `RIG.md` at `7a7403bd` with `:limit` 1000; with `:limit`
    26033, its exact size, the read succeeds (the budget is inclusive).
15. `:binary`: the PNG fixture; git's numstat gives `-` for it and `96 0`
    for the README fixture.
16. `:not-utf-8`: the private decode step on the bytes of `caf`, 0xE9 and
    a newline (Latin-1); and that NUL-free invalid UTF-8 is `:not-utf-8`,
    not `:binary`.
17. `:unreadable`, through `forms`: `"(defn f [x]"` (`:unclosed` at line 1,
    char 0), `"(a]"` (`:mismatched-close`), `")"` (`:unexpected-close`),
    `"\"abc"` and `"#\"abc"` (`:unclosed-string`), `"'"`, `"^:m"` and `"#_"`
    alone (`:unclosed`), a lone backslash (`:unclosed`), `"# x"` and `"#<x>"`
    (`:bad-dispatch`).
18. `:git-failed`: a good read with `:git "/nonexistent/git"`.
19. `:internal` appears in no result of any test (checked by the helper
    every test reads through).

**E. No identity.**
20. Walking every successful result of the suite: every map key anywhere is
    one of `:rev :commit :path :cut :units :unit :text :content :position
    :lines :chars`; every unit has exactly `:content` and `:position`, and
    every position exactly `:lines` and `:chars`, each two non-negative
    integers; `:commit` appears only at the top. So no id, name, hash, sha,
    kind or index key can be present.

**F. Rules on literal strings.**
21. Blocks: a fence holding blank lines and a `# ` line is one block; an
    unclosed fence runs to the last non-blank line; an item with a blank
    line and an indented second paragraph is one block, and one whose next
    line is not indented ends; `"2026. Then"` wrapped in a paragraph does
    not split it, and `"1. First"` directly under a paragraph line does;
    `"7b."` after item 7 starts a block; a nested item stays in its parent;
    a heading directly under a paragraph line splits it; CRLF text: blank
    lines still end blocks, content keeps an inner `"\r\n"`, and no unit
    ends in `"\r"`; a tab-indented continuation; a byte order mark before
    `# Title` makes that line paragraph text (G8, recorded as behaviour).
22. Forms: two forms on one line; a comment holding `)` and `"`; the
    characters `\(`, `\)`, `\;`, `\"`, `\\`, `\newline`, `\space`,
    `\u00e9`; a string holding `(;` and an escaped quote; the regex
    `#"[)\"]"`; `#{}`, `#()`, `##Inf`; `#inst "..."` and `#foo/bar {}`;
    `^:private`, `^{:a 1}`, `#^` and `^:a ^:b x`; `'`, `` ` ``, `~`, `~@`,
    `@`, `#'`, `#=`; `#?(...)` and `#?@(...)` at the top, one unit each;
    `#:a{}` and `#::{}`; `::alias/k` and `:a/b`; a top-level `#_ x` and
    `#_ #_ a b`, skipped, no unit; `#_` inside a form, kept; `#!` on line 1;
    nesting 10,000 deep, no stack overflow; an astral char in a symbol and
    in a string, counted as two chars.
23. Span: the first line, the last line, a blank line, the whole file; a
    text without a final newline, whose last line counts; an empty text,
    where every span is `:bad-argument`.

**G. Properties** (`test.check`, already in the `:test` alias).
24. Blocks: random texts of blank, heading, text, bullet, ordered, lettered,
    indented and fence lines, joined by `"\n"` or `"\r\n"`, satisfy section
    6's invariants; 200 trials.
25. Forms: random top-level data built from 22's atoms and prefixes,
    nested to random depth and joined by random whitespace, commas and
    comments that hold delimiters and quotes, give exactly the spans the
    generator placed; 200 trials.

Time: about 40 git reads at three processes each, plus the pure and
property tests; well under a minute (assumed).

## 12. What it gives the rig's phase 6 count

**The third class: a capability.** PROGRESS.md "Next" (lines 221 to 223)
reads the running count in three classes: "fixed-side steps the frame
already promised, fixed-side steps nobody anticipated, and capabilities."
This reader is in the third. decisions.md lines 169 to 170: "A capability
is code below the waist, added once, with receipts." It is a built-in a tool
calls: round three's seed lists "the built-in steps it calls"
(STARTER-round-3.md line 32), and a tool that refers to a passage or a
function at a revision calls it to get that material. It was anticipated:
integration.md's forced first step (lines 34 to 39, 13 September) names
this material.

**What the count records:** one capability, `rig.revision`, with two
built-in steps a tool can call, `read-units` (its cut a pick) and
`read-span`. Its receipts are its test namespace, run green, and the git
version it ran against (2.43.0 tonight). It touches nothing on the line: no
envelope part ("Tools add facts, never envelope parts", PROGRESS.md line
52), no gate rule, no key grammar, no store namespace, no Rama code. So it
is not a compiled step of the line, and adding it once does not count
against "zero new compiled steps".

**What is counted elsewhere:** whatever the runner needs in order to call
a capability at all (resolving a tool's step to a function, taking its
arguments from facts, turning its plain-data result into offers) belongs to
the runner, phase 6 proper, and the count classes it there: in the first
class if the frame promised it, in the second if nobody did. The reader
hides none of that. A cut or a field round three asks for later (sections,
names, EDN entries) is another entry in this capability, class three again,
unless it changes the line.

## 13. Picks, and what each gives up

Each is a plain rig choice that can change without touching a record,
because the reader writes none. Two of them become first-record the moment
a kept fact carries what they decide, G7 and G15; the build step writes
both under RIG.md "For Sid" (section 14).

- **G1. Two cuts, the default by extension, the pick by `:cut`.** `.clj`,
  `.cljc`, `.cljs`, `.edn` read as forms; everything else as blocks.
  Gives up: other languages get Markdown-shaped blocks; a one-map EDN file
  is one unit by default.
- **G2. A passage is a block** (section 6's six rules). Gives up: sections,
  whole lists, and blocks split where indented code or HTML holds blank
  lines; setext headings.
- **G3. The lettered marker** (`7b.`), this project's convention. Gives up:
  agreement with how Markdown renders such a line.
- **G4. A function is a top-level form, every one.** Gives up: telling a
  `defn` from a `def` or a `(comment ...)`; inner units.
- **G5. Comments, whitespace and top-level discards belong to no form;
  docstrings belong to theirs.** Gives up: a citation of a comment above a
  function lands in no form (`read-span` reads it; `:cut :blocks` groups a
  tight comment with its form).
- **G6. A lexical scanner, not a reader.** Gives up: semantic checks; a
  top-level `#?@` or an unknown tag still forms a unit.
- **G7. Positions:** lines 1-based inclusive; chars 0-based, end exclusive,
  in UTF-16 code units of the decoded text. Gives up: byte offsets (git's
  unit) and code points (the two differ only for chars outside the Basic
  Multilingual Plane). **First-record** when a kept fact carries a
  position.
- **G8. Content is the exact substring, nothing normalized.** Gives up: a
  byte order mark hides a heading on line 1; CRLF files keep `"\r"` inside
  content.
- **G9. Git's CLI plumbing** through `clojure.java.shell`, the revision
  resolved once to a commit id. Gives up: a timeout; three processes a read;
  needs git 2.24 or later on `PATH`, or `:git`.
- **G10. Errors as data, never an exception,** with `:internal` as the last
  resort. Gives up: stack traces, which survive only as `:detail` text.
- **G11. Text only.** Strict UTF-8; git's NUL test for binary; directories,
  symlinks and submodules refused. Gives up: Latin-1 and other encodings;
  `.gitattributes`; symlink targets.
- **G12. A byte budget,** 1 MiB by default, a parameter. Gives up: one
  tracked text file outside `src/app` (6.3 MB, under `history/`) needs a
  larger `:limit`.
- **G13. `read-span`,** beyond the literal ask, for the lines a person
  points at and the file:line citations. Gives up: nothing; it can be cut.
- **G14. No identity:** units are exactly `:content` and `:position`;
  results carry `:rev`, `:commit`, `:path`, `:cut`; the whole-file read is
  private. Gives up: a tool cannot find "the function named f" without
  looking through content; names and kinds come when round three sends what
  its tool needs.
- **G15. A cut's name is the name of its rules.** Tonight the rules can
  change freely. **First-record** once a kept fact records a cut's name:
  from then on `:blocks` and `:forms` mean exactly their rules, and a
  change is a new name, as the envelope's version marker works.

## 14. The build step, in order

1. In a worktree of its own off `rig-2026-09-25` (builder A names it), read
   this plan and `PLAN_VALIDATION-revision-reader.md`.
2. Write `src/rig/revision.clj`: the line index (line starts, the line of a
   char, where a line's content ends), the block cut, the form scanner,
   `span`, the git steps, then the entries. A docstring on every public
   function naming its section here.
3. Write `test/rig/revision_test.clj`: the fixture map first, then the
   oracles as test helpers (git show, rev-parse, numstat, the reader loop),
   then tests 1 to 25.
4. From the rig folder, `clojure -M:test rig.revision-test`, to green. No
   lock: no cluster starts. Other suites are not needed, since nothing is
   shared; any that start a cluster wait on the lock.
5. The rama skill's review steps, each in a fresh session, as builder A
   runs them: implementation validation, tests, test validation.
6. Upkeep with the code: README.md "Where to find it" gains rows for the
   reader, its test and its command; RIG.md's overnight state names it;
   RIG.md's rig choices gain one entry pointing at G1 to G15 here; RIG.md
   "For Sid" gains the two first-record questions, G7 and G15, each with
   the placeholder in use. SPEC.md is unchanged.
7. Commit by explicit paths, in parts if the files are long, each message
   saying what and why, with no attribution line; never push.

## 15. Design difficulty log

- **Whole lists or items.** The brief's example kept lists whole. It was
  not close once I read how this project writes its rulings: PROGRESS.md's
  "The nine" is one tight list, and a whole list would make one passage of
  nine rulings. The grain line settled it.
- **Sections.** Offering them would have cost twenty lines, and the first
  material cites sections by name. What settled it was PROGRESS.md line 27
  (coarsen later, never refine) and STARTER-next.md's "will send what its
  tool needs from the rig". Close, and easy to reverse.
- **The comment above a form.** Genuinely close. Attaching a directly
  preceding comment matches how people read code, and a citation of that
  comment would land in the function. Keeping comments out keeps the cut to
  syntax with a clean oracle (every unit reads as one datum), and `:cut
  :blocks` already gives the other grouping. I chose to keep them out; a
  validator could fairly choose the other way; the change is local and
  touches no record.
- **Top-level `#_`.** My first rule treated it as a prefix, which glued
  commented-out code to the next form, faithful to the reader's count and
  wrong for citations. Then as a unit of its own, which broke agreement with
  the reader's count. Skipping it like a comment keeps both.
- **The scanner or a reader.** Not close once I had checked the alias
  problem and the `\r\n` folding.
- **The unit of chars.** No ruling decides it. UTF-16 code units are what
  Clojure, ClojureScript and the browser index by; bytes are git's unit.
  Picked for the stack, and flagged first-record for when a fact carries
  one.
- **`read-text` public or private.** I first had it public. Made private
  because a whole-file step invites file-grain references.
- **`read-span`.** Beyond the ask. Kept because the first material is
  citations of line ranges, and it is the part a validator may cut.
- **No names, no kinds.** The costliest omission: a tool cannot find "the
  function f" in the output without looking through content. The brief is
  explicit, and round three will say what its tool needs.
- **An unreadable Clojure file.** An error for the whole file, rather than
  the units before the break, because partial units would hide the broken
  region at that revision.
- **The `:git` option.** Close. `with-redefs` on `clojure.java.shell/sh`
  would test `:git-failed` without it, but replaces a core var for the
  whole JVM while it runs; the option is one key and also serves a host
  whose git is not on `PATH`.

## 16. Self-validation

Against the parts of `artifact-plan-validation.md` that apply to plain code.

**Minimality.** The simplest design that meets the brief: one function that
resolves a revision, reads the blob, and cuts it by extension into blocks or
top-level forms returned as content and position, with errors as data. The
plan differs from that sketch by:

- *The `:cut` pick.* Delete it, and a Clojure file can no longer be read as
  blocks, and the pick is no longer a parameter a tool can carry, which is
  PROGRESS.md's standing stance (lines 35 to 40). One key. Kept.
- *`read-span`.* Delete it, and a cited line range spanning several blocks
  has no unit. Not in the brief; kept as G13, and it can go without touching
  anything else.
- *The pure cuts as public functions.* Private, the tests still reach them
  through vars, and a runner could not cut text from elsewhere. Nothing in
  the brief needs that. Public costs nothing and keeps the cut definitions
  where round three will look for them. Kept, a close call.
- *Input validation, `--end-of-options` and `--literal-pathspecs` with the
  exact match.* Delete them, and `--output=x` could reach git as an option
  and `:(top)AGENTS.md` matches another path (checked). Kept.
- *The byte budget, the NUL test, the strict decode.* Each is an error the
  brief names ("a file too large", "binary file", "the encoding"). The NUL
  test does not merge into the decode: NUL is valid UTF-8, so a binary made
  of ASCII and NULs would decode. Kept.
- *The commit resolved first.* Delete it, and a result cannot keep its
  snapshot (decisions.md line 164) and a moving ref can split a read. Kept.
- *The `:internal` catch.* Delete it, and a bug throws inside whatever
  called the reader, a Rama task included. Kept.
- *The iterative scanner.* A recursive one throws `StackOverflowError`, an
  error the catch does not take, on deep nesting within the budget. Kept.
- *Rule 5, list continuations, and the lettered marker.* Delete rule 5, and
  an item with an indented second paragraph splits; delete the letter, and
  ruling 7b joins ruling 7 (checked, PROGRESS.md line 101). Kept.

**Coverage of the brief, traced.**

- "a repository path, a revision (any git revision string), and a file
  path": section 4's signatures; step 1 passes the string to rev-parse, so
  anything rev-parse takes works. Trace: `"e53bc6d0"`, a short id, resolves
  to `e53bc6d01a44cfb8a4967703cf2dc82446f88c76` (checked).
- "passages ... or functions ..., each with its content and its position
  (start and end line, and character offsets within the file at that
  revision)": section 3. Trace: the README fixture's first item is lines 44
  to 45, chars 1732 to 1878, 146 chars (checked).
- "Decide what a passage is ... say what it gives up": section 6, G2, G3.
- "what a function is ... which forms count, and how a docstring, a comment
  block above a form, reader conditionals and namespaced keywords are
  handled": section 7, G4 to G6.
- "How the file is read at a revision ..., how errors come back as data
  (unknown revision, missing path, binary file, a file too large), and the
  encoding": section 5 and the error table; each has a test in D.
- "its own namespace ... no dependency on the store's namespaces and no new
  library unless the plan shows it is needed": section 10.
- "Tests ... at fixed commits ..., against git's own output, a file read at
  two revisions, every error path, and that no output carries an identity.
  Tests must not need a Rama cluster": section 11, groups A to E, and its
  run line.
- "no ids, no names used as keys, no hashes offered as identity ... content
  and position only": section 3 and test 20; `:commit` is the snapshot's,
  and is justified there.
- "What it gives the rig's phase 6 count": section 12.
- "the plan's Rama sections ... don't apply; say so in one line": section 9,
  first paragraph.

**Faults and races.** A restart: there is no state to lose. A retry: the
same inputs give the same result (derived from section 9). A partial
failure: there are no writes. Two callers at once: they share nothing. A
ref that moves mid-read: pinned by step 1. An object pruned mid-read, which
is possible only for an unreachable commit: step 2 or 4 exits non-zero and
the result is `:git-failed`, never a mixed read.

**Consistency.** Nothing above is marked a gap and then passed. The open
items are in section 17, and none of them is a flaw in this plan's reading
of its brief; they are round three's, or Sid's.

## 17. What this plan could not settle

1. **The unit of positions** (G7) becomes first-record when a kept fact
   carries a position. Round three can rule it at that step, or store lines
   only, which carry no unit question.
2. **A cut's name as a frozen meaning** (G15), if facts record which cut
   made them.
3. **Identity and continuity** stay round three's (its open items 17 and
   60). The reader gives content and position at each revision; whatever
   says two of them are the same is a claim with an actor (PICTURE.md line
   82).
4. **Uncommitted text** cannot be read: the reader sees commits only. If
   Sid's first use cites files while they are being edited, the tool can
   refer only to their committed text.
5. **Where and under what permission the runner calls it.** It blocks for
   its processes, so not on a Rama task thread without a reason. It has no
   path policy, and a secret ever committed is readable at that revision.
6. **Additions round three may ask for,** each a new entry in the same
   capability: sections; names or kinds of forms, which is where identity
   starts, so they wait for its rule; the entries of an EDN file's top map;
   inner Clojure units, for which `rewrite-clj` would be the library to
   name; and whether non-Markdown text should drop the Markdown rules.
