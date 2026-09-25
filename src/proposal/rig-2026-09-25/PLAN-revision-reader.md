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
