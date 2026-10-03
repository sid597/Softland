# Build notes: the first tool, a citation that stays true (27 September 2026)

The citation session (Opus 5.5, max effort), on the MacBook Air, in the
clone `~/projects/Softland-citation`, branch `citation-2026-09-27` cut from
`rig-2026-09-25` (`1e63217e`). Committed there, never pushed. The records
the rig wrote are thrown away. The plans: `PLAN-dependents.md` (the store's
lookup) and `PLAN-citation.md` (the tool); their validation,
`PLAN_VALIDATION-citation.md` (a fresh Opus session: major-fail on the
lookup's first version, minor-fail on the tool); the review of the built
code, `REVIEW-citation.md`. The report over this project:
`runs/citation-report.md`.

## What was built, in plain words

- **Material** (`rig.material`, beside `rig.revision`, which is
  unchanged). A file at a git revision becomes facts: a reading of the file
  (its units, their lines, which unit holds each line), its text by line,
  and one fact per named top-level Clojure form on that form's own thing.
  The next reading writes new facts on the same things, each replacing the
  last; for a form that replace is the reading tool's claim that the same
  name in the same file is the same function. Names and kinds as the
  server's `clojure_adapter.clj` gives them, plus Rama's `deframaop` and
  `deframafn` and any other `def…` head.
- **The lookup** (`rig.store.dependents`, `rig.store.dependents-query`).
  When the gate admits an act that stood on facts, the same event writes one
  entry per fact it stood on, ids only, in the dependent's layer. A read
  `[:dependents x]` through the one exit shows the facts of every act that
  stood on x (a fact) or on any fact of x (an act), with `:on`. Rebuilt from
  the log by the existing put pages.
- **The tool, as four facts** (`tools/citation.edn`): a reader (a file at a
  revision into material), a finder (a document's `path:line` citations,
  when each line was written, which file each names), a binder (each
  citation bound to the named form holding its lines at the revision it
  was written, standing on that form's fact, or not bound yet with the
  reason), and a marker (when a file's next reading lands, it asks the
  store what stood on the previous reading and marks each citation true,
  moved or stale against the form as it was when bound).
- **The vocabulary the tool needed** (`rig.store.recipe`), **the runner's
  changes** (`rig.store.runner`), and **the driver and report**
  (`rig.cite.run`, `clojure -M:cite`).

## The count, as the rig's machinery count takes it

Unit: one named code unit added to the fixed side or the vocabulary, or
one existing unit whose behaviour changes (the rig's assumed unit;
IMPLICIT_SPEC O21 leaves it open, RIG.md For Sid 79). Classes: (a)
fixed-side steps the frame promised, (b) fixed-side steps nobody
anticipated, (c) capabilities.

**22 as built: 4 promised, 7 unanticipated, 11 capabilities.**

*(a) Promised, 4.* The frame's first promise, "When something you built on
changes, you find out. Always.", and its "guaranteed walk"; nothing built
it, and no plan's count predicted it.
- a1 `:ix-dep`, written in the admitting event (`dependents/layer-fields`,
  `dependents/index-writes`; `gate/decide*` appends them; `module` merges
  the field).
- a2 `[:dependents x]` at the one exit: its parse and range
  (`dependents/parse-pattern`, `dependents/bounds`; `reads/parse-pattern`
  and `reads/bounds` delegate; `reads/read-pattern` hands it over).
- a3 the query `read-dependents` (`dependents-query/dependents-read>`,
  declared in `module`): each dependent act read once, its rows opened.
- a4 the lookup rebuilt from the log (`reads`' put page reads each act's
  stood-on; `put-act`, `put-page-writes`; `index-op`'s drop takes the
  field, its sweep refuses it).

*(b) Unanticipated, 7.*
- b1 the lookup refused where it is not built (`shared-reads/micro-pp`,
  `read-delta`, `delta>`): the shared path parses with the exit's own
  parser, and a standing read opens through `read-pattern`.
- b2 the runner looks up answered runs first and runs and offers match by
  match (`runner/run-tool`): once a recipe reads the store, recomputing
  every match every pass writes a read entry per match per pass, and a
  file's two requests in one pass would be refused stale.
- b3 a recipe's read function (`runner/recipe-read`).
- b4 the run's own act name in its row (`:run`), so a reading's value can
  name its own facts.
- b5 a run's act stands on what its recipe names (`runner/output-act`'s
  `also`; T-FR5 widened).
- b6 a run with no facts is not offered (from the validation's F16; this
  tool never has one, since each of its runs writes at least a record).
- b7 the driver's reading plan and rounds (`rig.cite.run`): the order in
  which revisions are read is operator code (P-C16).

*(c) Capabilities, 11.* `:read` (with the nil rule), `:emit-all`,
`:stand-on`, `[:vec]`, `[:in :replaces]`, `[:in :run]`,
`:material/reading` (its rule `:same`), `:cite/find`, `:cite/bind` (its
rule `:target`), `:cite/check` (its rule `:show`), and the report, the
view.

*The zero.* After these, the four tools were added as facts only: nothing
under `src/` names `:material-reader`, `:cite-finder`, `:cite-binder` or
`:cite-marker` (`rig.cite-tools-test`, "the machinery count's zero"), and
the tools' rules are literal arguments in their facts.

*What the count says.* The recipe vocabulary as the rig built it could run
a tool that reads git and emits a fixed number of facts about its match.
This tool needed the store read (c1), an unbounded emit (c2), a named
based-on (c3), and a runner that does not recompute (b2); every domain
computation became a capability (c7 to c10), as integration.md allows ("a
parser, a domain computation or a representation capability is growth").
The four (a) units are the store's own missing piece.

## Findings for Sid

1. **No tool runs in the base on the rig.** `locks/lease-under`: a writer
   who is no person leases under the layer's person owner, and the base's
   owner is the root actor, so a tool's lease is refused and its own read
   entry with it (`:no-such-lock`). Tested (`rig.cite-tools-test`, the
   base). The pointer session puts material in the base, so it offers the
   same reading step as operator code, which works (tested).
2. **"Reads on the act once" and "a rerun is a retry" pull against each
   other** for a tool's runs (validation F13, declined). A run's act does
   not name the read entries of its reads; naming them would make a run's
   content depend on the pass and the runner.
3. **What `:on` shows** (validation F19). The lookup shows the facts an act
   stood on to anyone who can read the act's layer; a promotion's landing
   stands on the crossing, whose name carries the source layer.
4. **One runner per layer** (P-C18, validation F14).
5. **The runner reads one page of matches, with no cursor** (b7 of the
   rig's own count, deferred). The driver stops on a partial match read.
6. **The existing rig**: the standing read's opening goes through
   `read-pattern`, and the shared path parses with the exit's parser, so
   any new pattern kind must be refused in both.


7. **A forgotten reading stops its file's reader** (review finding 15): the
   next reading cannot read the forgotten one's heads. What a reading after
   a forgotten one does is open.
8. **The role of a read whose run failed** (review finding 17, with
   P-C12): it is recorded as `:stood-on`, though nothing stood on it.
9. **A walk of more than about 500 citations of one file** stops, loudly
   (`:too-many-facts` or a partial lookup, then `:previous-walk-missing`):
   splitting a walk over several acts is not built.

## Placeholders

The lookup, `PLAN-dependents.md` section 10: P-D1 the dependent's side, a
lookup reads one layer, the promise holds within one layer; P-D2 act
grain; P-D3 the stream store only; P-D4 no standing read; P-D5 rebuilt by
put pages, stale entries skipped at read; P-D6 (first-record) the store
writes nothing on finding a dependent but the read entry. The pattern's
form and the rows' `:on` become first-record at the first kept record.

The tool, `PLAN-citation.md` section 9: P-C1 the digest (keyed); P-C2 the
kind table's additions; P-C3 thing ids derived (keyed); P-C4 the
same-function rule; P-C5 every reading restates every form; P-C6 the
citation forms; P-C7 written-at is the line's blame commit; P-C8 path
resolution across lines of work; P-C9 a late binding is marked at once;
P-C10 (first-record) what a stale citation shows; P-C11 what a new tool
owes history; P-C12 (first-record) a recipe read's role; P-C13 a nil in a
pattern reads nothing; P-C14 a run's act stands on what its recipe names;
P-C15 requests are the person's facts; P-C16 the reading plan is operator
code; P-C17 what a citation binds to; P-C18 one runner per layer; P-C19
(first-record) the walk is the tool's read of the store's lookup.

## Runs

All on the MacBook Air (8 GB), one in-process cluster at a time, turns
agreed with the pointer session.

- Pure tests: `rig.material-test`, `rig.cite-test`,
  `rig.store.recipe-citation-test`, 15 tests, 0 failures.
- `rig.store.dependents-test`: the first run failed 1 of 36 (a standing
  read of the lookup opened: its opening goes through `read-pattern`, so
  the refusal moved to the top of `read-delta`); then 36 of 36 on fact
  grain; after the validation, act grain, with the tool's test,
  `rig.cite-tools-test`, 62 of 62 together (about 50 s).
- `rig.cite-tools-test`, before that: the report code shadowed `when` with a
  local (fixed); the base case found that no tool runs in the base
  (finding 1).
- The tool over this project, first (a preview, before the commits):
  29 documents, 342 citations, 100 readings of 50 files in 3 rounds, 2 min
  24 s, no run refused at the end, no miss of the walk. 133 true, 4 stale,
  205 not bound yet (178 citing Markdown, 19 not inside one form, 7 inside
  an unnamed form, 1 past the file's end). Checked by hand against git:
  `micro.clj:1197-1240` in RIG.md was `fact-rows`, exactly, at the commit
  that wrote it, and at HEAD is at 1365 with its docstring changed (stale,
  right); `placements.cljc:95–154` in FACTS.md cites a file of 152 lines at
  its commit (past the end, right); `code_import.clj:251-322` in HANDOFF.md
  is `commit-lineage` (251-321) and a blank line, which the binding rule
  then refused. Setting aside the lines no unit holds at a range's ends
  (P-C17) binds it and two others; the other 16 of the 19 span several
  forms or sit on a banner comment, and stay not bound yet, as the thin
  slice says. The handoff's own drift example, `relation_kernel.clj:441-448`,
  already straddled two forms at the commit that quoted it.
- The review of the built code (`REVIEW-citation.md`, a fresh Opus session,
  read-only, no cluster): fail, 2 high, 5 medium, 11 low; the answers are in
  `PLAN-citation.md` section 11 and `PLAN-dependents.md` section 5. After
  the fixes: the pure tests, 16 tests, 169 assertions, 0 failures;
  `rig.store.dependents-test` and `rig.cite-tools-test` (now three readings
  of the scratch file, so a walk passes through the previous walk's
  marks), 66 of 66, 1 min 15 s.
- **The tool over this project, at the committed HEAD** (`runs/citation-
  report.md`; `main` 89c8e1a9, `rig` 5aef8471, this branch after its
  commits): 29 documents, 342 citations, 105 readings of 50 files in 3
  rounds, 2 min 20 s, no run refused at the end, no miss of the walk. **131
  true, 4 moved, 5 stale, 202 not bound yet** (178 citing Markdown, 16 not
  inside one form, 7 inside an unnamed form, 1 past the file's end).
  - The 4 moved and 1 of the stale are this session's own commits,
    caught: HANDOFF.md's `reads.clj:218-229` (`layer-fields`),
    `gate.clj:252-254` and `gate.clj:138` each one line down (a `require`
    added above them), `reads.clj:578-590` (`visible?`) seven down, and
    `recipe.clj:118-123` (`capabilities`) stale because this build added
    the new steps to that map. Checked against git: `gate.clj` 252-254 at
    `1e63217e` and 253-255 at `5aef8471` are the same three lines;
    `visible?` is at 578 then and 585 now, its text identical.
  - The full suite, once (`runs/citation-suite-cmd.sh`, summary
    `runs/citation-suite.txt`): the rig's 26 namespaces of step 6b's suite,
    the spec fixes' tests and the citation's 5, at `5aef8471`, 22:51 to
    23:34 IST, 42 minutes: 190 tests, 8,440 assertions, **5 failures, 0
    errors**. The model's 14 fixed histories replay as the model says
    (`runs/citation-suite-replays.txt`). The five:
    - 3 in `rig.store.recipe-test` were this build's: `recipe/run` always
      returned `:stood-on {}`, where that test expects exactly `{:facts
      [...]}`. Fixed: `:stood-on` only when a recipe names pairs, so a
      recipe that names none returns what it always did. Rerun:
      `rig.store.recipe-test` and `rig.store.recipe-citation-test` 82 of
      82; `rig.store.tools-test` and `rig.cite-tools-test` 182 of 182.
    - 1 in `rig.revision-test` (t18) is this machine: the test runs `ls`
      in git's place and expects GNU ls's exit 2 on `--verify`; macOS's BSD
      ls exits 1 (checked). Neither `rig.revision` nor its test changed on
      this branch; it passed on the Linux machine.
    - 1 in `rig.store.micro-test` (M15's "ran for at least 300 batches")
      is this machine's speed: 252 batches here. The rig at its original
      tip (`1e63217e`, a plain copy with no citation code), run alone on
      this Mac, fails the same check with 168 (checked).
  - The pointer session's merged tree runs the one full suite for both
    branches (agreed with it; its `rig.store.screen-test` added).
  - The other 4 stale were already stale before this session: RIG.md's
    `micro_test.clj:414-416`, `micro.clj:1197-1240` (`fact-rows`, changed
    and moved to 1365) and `micro_prepare_test.clj:245`, and
    `gate.clj:186-193` (`stamp-for`) cited in a main document at a rig
    commit of 26 September.
