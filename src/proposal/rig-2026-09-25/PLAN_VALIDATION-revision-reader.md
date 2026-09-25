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
not a claim (L 164 to 171, L 743 to 746).

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
